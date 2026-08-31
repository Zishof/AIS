package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.antarjemput.DetailPenjemputanAntarJemput;
import ais.database.model.antarjemput.JadwalAntarJemput;
import ais.database.model.antarjemput.KartuPenjemputAntarJemput;
import ais.database.model.antarjemput.LogNotifikasiAntarJemput;
import ais.database.model.antarjemput.PesertaJadwalAntarJemput;
import ais.database.model.antarjemput.TransaksiPenjemputanAntarJemput;
import ais.ui.util.WaktuUtil;

/**
 * Servlet kiosk gerbang "Antar Jemput" (penjemputan siswa/mahasiswa oleh orang tua/penjemput
 * terdaftar): memindai kartu/RFID/QR penjemput ({@link KartuPenjemputAntarJemput}), mencocokkannya
 * ke peserta jadwal aktif ({@link PesertaJadwalAntarJemput}) pada jadwal penjemputan terkait
 * ({@link JadwalAntarJemput}), lalu membuat transaksi antrian panggilan
 * ({@link TransaksiPenjemputanAntarJemput} beserta rincian per peserta
 * {@link DetailPenjemputanAntarJemput}) yang akan dipanggil lewat perangkat pengumuman (soundbox,
 * dicatat sebagai {@link LogNotifikasiAntarJemput}). Tiga mode ditentukan lewat parameter
 * {@code action}: {@code verify} (proses pemindaian kartu, respons JSON), {@code card} (halaman
 * kartu), dan default (halaman tampilan gerbang untuk kiosk).
 *
 * <p>
 * <b>Catatan keamanan (DITAMBAL 2026-09-01):</b> aksi {@code verify} (menulis transaksi/antrian
 * baru) dan {@code card} (membaca nama/hubungan penjemput berdasarkan {@code nomor} kartu — vektor
 * enumerasi kartu untuk memanen nama penjemput terdaftar) sebelumnya TIDAK memiliki pemeriksaan
 * otentikasi/otorisasi apa pun. Kelas ini adalah endpoint kiosk gerbang fisik (tidak ada akun
 * staf/siswa yang login), jadi pola {@code username}/{@code password} keluarga {@code *Resource}
 * lain tidak berlaku di sini — dipakai pola kredensial bersama (shared secret) yang sama dengan
 * {@link ais.action.master.resources.PosResource}: kedua aksi kini memvalidasi parameter
 * {@code secret} terhadap konfigurasi {@code antar_jemput_api_secret} (lihat
 * {@link #isValidGateSecret(String)}), TANPA nilai default hardcoded (fail-closed sebelum
 * konfigurasi diisi). Berbeda dari {@code PosResource}, halaman gerbang ({@code action} kosong)
 * TETAP dapat dimuat tanpa {@code secret} (mustahil membuat kiosk tahu rahasia dari halaman yang
 * mensyaratkan rahasia itu sendiri untuk dimuat) — sebagai gantinya, halaman itu menyisipkan nilai
 * {@code secret} yang sedang aktif ke JavaScript/tautan yang dirender, sehingga kiosk yang memuat
 * ulang halaman gerbang otomatis ikut mendapat rahasia terbaru tanpa perlu konfigurasi ulang
 * perangkat satu per satu. Catatan: ini menaikkan ambang terhadap pemindaian/enumerasi otomatis,
 * BUKAN pengganti pembatasan jaringan/reverse-proxy yang sesungguhnya — siapa pun yang dapat memuat
 * halaman gerbang tetap dapat membaca {@code secret} dari sumber halaman.
 * </p>
 */
public class AntarJemput extends HttpServlet {

	private static final long serialVersionUID = 14520260609L;

	/**
	 * Memvalidasi {@code secret} yang dikirim kiosk terhadap konfigurasi
	 * {@code antar_jemput_api_secret} (DITAMBAHKAN 2026-09-01 — lihat catatan keamanan pada javadoc
	 * kelas). TIDAK punya nilai default hardcoded: bila konfigurasi belum diisi, method ini SELALU
	 * mengembalikan {@code false} (fail-closed).
	 *
	 * @param secret nilai yang dikirim lewat parameter request {@code secret}
	 * @return {@code true} bila {@code secret} cocok (case-sensitive) dengan konfigurasi yang
	 *         tersimpan dan konfigurasi tersebut sudah diisi; {@code false} pada semua kondisi lain
	 */
	private boolean isValidGateSecret(String secret) {
		try {
			if (secret == null || secret.trim().length() == 0) {
				return false;
			}
			String configured = Common.getKonfigurasi("antar_jemput_api_secret", "").getNilai();
			return configured != null && configured.trim().length() > 0 && configured.trim().equals(secret.trim());
		} catch (Exception e) {
			try {
				Common.tampilErrorJikaAdmin(e);
			} catch (Exception ignored) {
			}
			return false;
		}
	}

	/** @return nilai {@code antar_jemput_api_secret} saat ini (string kosong bila belum dikonfigurasi/gagal dibaca), dipakai untuk menyisipkan rahasia aktif ke halaman gerbang yang dirender. */
	private String currentGateSecret() {
		try {
			String configured = Common.getKonfigurasi("antar_jemput_api_secret", "").getNilai();
			return configured == null ? "" : configured.trim();
		} catch (Exception e) {
			return "";
		}
	}

	/** Menangani permintaan {@code GET} — didelegasikan ke {@link #process(HttpServletRequest, HttpServletResponse)} bersama {@code POST}. */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/** Menangani permintaan {@code POST} — didelegasikan ke {@link #process(HttpServletRequest, HttpServletResponse)} bersama {@code GET}. */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		process(request, response);
	}

	/** Merutekan permintaan berdasarkan parameter {@code action}: {@code verify} → {@link #verify}, {@code card} → {@code renderCardPage}, selain itu → {@code renderGatePage} (tampilan kiosk gerbang). */
	private void process(HttpServletRequest request, HttpServletResponse response) throws IOException {
		Common.ROOT = request.getContextPath();
		String action = request.getParameter("action");
		if ("verify".equalsIgnoreCase(action)) {
			verify(request, response);
		} else if ("card".equalsIgnoreCase(action)) {
			renderCardPage(request, response);
		} else {
			renderGatePage(request, response);
		}
	}

	/**
	 * Memproses pemindaian kartu/RFID/QR penjemput: mencari {@link KartuPenjemputAntarJemput} yang
	 * cocok dan aktif, memvalidasi masa berlaku, mencari peserta jadwal aktif yang terkait pemilik
	 * kartu (siswa/mahasiswa/guru/dosen/pegawai), lalu membuat transaksi antrian panggilan beserta
	 * rincian dan log notifikasi per peserta yang cocok. Setiap kegagalan (kartu tidak
	 * ditemukan/nonaktif, masa berlaku habis, tidak ada peserta cocok) tetap mencatat transaksi
	 * dengan status ditolak untuk keperluan audit, dan mengembalikan respons JSON
	 * {@code {"success":..., "message":...}}.
	 *
	 * <p>
	 * <b>DITAMBAL 2026-09-01:</b> parameter {@code secret} kini wajib cocok dengan konfigurasi
	 * {@code antar_jemput_api_secret} — lihat {@link #isValidGateSecret(String)} dan catatan
	 * keamanan pada javadoc kelas.
	 * </p>
	 */
	private void verify(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json;charset=UTF-8");

		Session session = null;
		Transaction tx = null;
		String nomor = firstNotBlank(request.getParameter("kode"), request.getParameter("rfid"),
				request.getParameter("nomor"));
		String pintu = firstNotBlank(request.getParameter("pintu"), "Gerbang Utama");
		try {
			if (!isValidGateSecret(request.getParameter("secret"))) {
				writeJson(response, false, "Akses ditolak", null, 0, null);
				return;
			}
			if (nomor.length() == 0) {
				writeJson(response, false, "Nomor kartu, RFID, atau QR wajib diisi", null, 0, null);
				return;
			}

			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();

			KartuPenjemputAntarJemput kartu = findKartu(session, nomor);
			if (kartu == null || !kartu.getAktif().booleanValue()) {
				createRejectedTransaction(session, nomor, pintu, "Kartu tidak ditemukan atau tidak aktif");
				tx.commit();
				writeJson(response, false, "Kartu tidak ditemukan atau tidak aktif", null, 0, null);
				return;
			}
			if (kartu.getBerlakuSampai() != null && kartu.getBerlakuSampai().before(WaktuUtil.getDate())) {
				createRejectedTransaction(session, nomor, pintu, "Masa berlaku kartu sudah habis");
				tx.commit();
				writeJson(response, false, "Masa berlaku kartu sudah habis", kartu.getNamaPenjemput(), 0, null);
				return;
			}

			JadwalAntarJemput jadwal = loadJadwal(session, request.getParameter("jadwal"));
			TransaksiPenjemputanAntarJemput transaksi = createTransaction(session, nomor, pintu, kartu, jadwal,
					TransaksiPenjemputanAntarJemput.MENUNGGU);
			List peserta = findPeserta(session, kartu, jadwal);
			if (peserta.isEmpty()) {
				transaksi.setStatus(TransaksiPenjemputanAntarJemput.DITOLAK);
				transaksi.setKeterangan("Tidak ada peserta aktif yang cocok dengan kartu dan jadwal");
				session.saveOrUpdate(transaksi);
				tx.commit();
				writeJson(response, false, "Peserta tidak ditemukan pada jadwal aktif", kartu.getNamaPenjemput(), 0,
						transaksi.getNomorAntrian());
				return;
			}

			for (int i = 0; i < peserta.size(); i++) {
				PesertaJadwalAntarJemput p = (PesertaJadwalAntarJemput) peserta.get(i);
				DetailPenjemputanAntarJemput detail = createDetail(session, transaksi, p);
				createLog(session, detail, "ANTRI");
			}
			transaksi.setStatus(TransaksiPenjemputanAntarJemput.DIPANGGIL);
			session.saveOrUpdate(transaksi);
			tx.commit();

			writeJson(response, true, "Berhasil. " + peserta.size() + " peserta masuk antrian panggilan.",
					kartu.getNamaPenjemput(), peserta.size(), transaksi.getNomorAntrian());
		} catch (Exception e) {
			if (tx != null) {
				try { tx.rollback(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:116");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AntarJemput.java:118");
			writeJson(response, false, "Gagal memproses verifikasi: " + e.getMessage(), null, 0, null);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:122");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:123");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:124");}
			}
		}
	}

	private KartuPenjemputAntarJemput findKartu(Session session, String nomor) {
		return (KartuPenjemputAntarJemput) session.createCriteria(KartuPenjemputAntarJemput.class)
				.add(Restrictions.or(Restrictions.eq("nomorKartu", nomor), Restrictions.eq("barcode", nomor)))
				.setMaxResults(1).uniqueResult();
	}

	private JadwalAntarJemput loadJadwal(Session session, String value) {
		Long id = parseLong(value);
		if (id == null) {
			return null;
		}
		return (JadwalAntarJemput) session.get(JadwalAntarJemput.class, id);
	}

	private TransaksiPenjemputanAntarJemput createTransaction(Session session, String nomor, String pintu,
			KartuPenjemputAntarJemput kartu, JadwalAntarJemput jadwal, String status) {
		TransaksiPenjemputanAntarJemput transaksi = new TransaksiPenjemputanAntarJemput();
		transaksi.setKode("AJ-" + WaktuUtil.getDate().getTime());
		transaksi.setNama(kartu == null ? nomor : kartu.getNamaPenjemput());
		transaksi.setWaktuScan(WaktuUtil.getDate());
		transaksi.setTipeScan("KARTU_QR_RFID");
		transaksi.setNomorScan(nomor);
		transaksi.setPintuGerbang(pintu);
		transaksi.setNomorAntrian(generateNomorAntrian(session));
		transaksi.setKartuPenjemputAntarJemput(kartu);
		transaksi.setJadwalAntarJemput(jadwal);
		transaksi.setStatus(status);
		session.saveOrUpdate(transaksi);
		return transaksi;
	}

	private void createRejectedTransaction(Session session, String nomor, String pintu, String message) {
		TransaksiPenjemputanAntarJemput transaksi = createTransaction(session, nomor, pintu, null, null,
				TransaksiPenjemputanAntarJemput.DITOLAK);
		transaksi.setKeterangan(message);
		session.saveOrUpdate(transaksi);
	}

	private DetailPenjemputanAntarJemput createDetail(Session session, TransaksiPenjemputanAntarJemput transaksi,
			PesertaJadwalAntarJemput p) {
		DetailPenjemputanAntarJemput detail = new DetailPenjemputanAntarJemput();
		detail.setTransaksiPenjemputanAntarJemput(transaksi);
		detail.setPesertaJadwalAntarJemput(p);
		detail.setNama(p.getNama());
		detail.setSiswa(p.getSiswa());
		detail.setMahasiswa(p.getMahasiswa());
		detail.setGuru(p.getGuru());
		detail.setDosen(p.getDosen());
		detail.setPegawai(p.getPegawai());
		detail.setKelasSiswa(p.getKelasSiswa());
		detail.setPerangkatTujuan(resolvePerangkatTujuan(p));
		detail.setTeksPanggilan(resolveTeksPanggilan(p));
		detail.setStatusPanggilan(DetailPenjemputanAntarJemput.MENUNGGU_PANGGILAN);
		session.saveOrUpdate(detail);
		return detail;
	}

	private List findPeserta(Session session, KartuPenjemputAntarJemput kartu, JadwalAntarJemput jadwal) {
		Criteria criteria = session.createCriteria(PesertaJadwalAntarJemput.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)));
		if (jadwal != null) {
			criteria.add(Restrictions.eq("jadwalAntarJemput", jadwal));
		}
		List ors = new ArrayList();
		if (kartu.getSiswa() != null) ors.add(Restrictions.eq("siswa", kartu.getSiswa()));
		if (kartu.getMahasiswa() != null) ors.add(Restrictions.eq("mahasiswa", kartu.getMahasiswa()));
		if (kartu.getGuru() != null) ors.add(Restrictions.eq("guru", kartu.getGuru()));
		if (kartu.getDosen() != null) ors.add(Restrictions.eq("dosen", kartu.getDosen()));
		if (kartu.getPegawai() != null) ors.add(Restrictions.eq("pegawai", kartu.getPegawai()));
		if (ors.size() == 0) {
			return new ArrayList();
		}
		org.hibernate.criterion.Criterion criterion = (org.hibernate.criterion.Criterion) ors.get(0);
		for (int i = 1; i < ors.size(); i++) {
			criterion = Restrictions.or(criterion, (org.hibernate.criterion.Criterion) ors.get(i));
		}
		criteria.add(criterion);
		criteria.addOrder(Order.asc("nomorUrut"));
		return criteria.list();
	}

	private void createLog(Session session, DetailPenjemputanAntarJemput detail, String status) {
		LogNotifikasiAntarJemput log = new LogNotifikasiAntarJemput();
		log.setDetailPenjemputanAntarJemput(detail);
		log.setKanal("SOUNDBOX");
		log.setPerangkatTujuan(detail.getPerangkatTujuan());
		log.setPesan(detail.getTeksPanggilan());
		log.setStatus(status);
		log.setPercobaan(Integer.valueOf(1));
		log.setWaktuKirim(WaktuUtil.getDate());
		session.saveOrUpdate(log);
	}

	private String generateNomorAntrian(Session session) {
		Number count = (Number) session.createCriteria(TransaksiPenjemputanAntarJemput.class)
				.setProjection(Projections.rowCount()).uniqueResult();
		int nomor = count == null ? 1 : count.intValue() + 1;
		return "AJ-" + nomor;
	}

	private String resolvePerangkatTujuan(PesertaJadwalAntarJemput p) {
		if (p.getKelasSiswa() != null) {
			return "Kelas " + p.getKelasSiswa().getNama();
		}
		if (p.getGuru() != null) return "Ruang Guru";
		if (p.getDosen() != null) return "Ruang Dosen";
		if (p.getPegawai() != null) return "Ruang Pegawai";
		return "Monitor Antar Jemput";
	}

	private String resolveTeksPanggilan(PesertaJadwalAntarJemput p) {
		String nama = p.getNama() == null ? "" : p.getNama();
		if (p.getSiswa() != null) return "Penjemputan ananda " + nama + " sudah datang.";
		if (p.getMahasiswa() != null) return "Penjemputan mahasiswa " + nama + " sudah datang.";
		if (p.getGuru() != null) return "Penjemputan guru " + nama + " sudah datang.";
		if (p.getDosen() != null) return "Penjemputan dosen " + nama + " sudah datang.";
		return "Penjemputan pegawai " + nama + " sudah datang.";
	}

	private void renderGatePage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		String root = request.getContextPath();
		// Menyisipkan nilai antar_jemput_api_secret yang sedang aktif ke halaman gerbang yang
		// dirender, agar kiosk yang memuat ulang halaman ini otomatis ikut mendapat rahasia terbaru
		// tanpa perlu konfigurasi ulang tiap perangkat — lihat catatan keamanan pada javadoc kelas.
		String secret = currentGateSecret();
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
		html.append("<title>Verifikasi Antar Jemput</title>");
		html.append("<style>");
		html.append("body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#f4f7fb;color:#172033}.wrap{max-width:1060px;margin:0 auto;padding:18px}.top{display:flex;gap:12px;align-items:center;justify-content:space-between;margin-bottom:14px}.brand h1{font-size:24px;margin:0}.brand p{margin:4px 0 0;color:#5d6b82}.grid{display:grid;grid-template-columns:1.2fr .8fr;gap:16px}.panel{background:#fff;border:1px solid #dfe7f2;border-radius:8px;box-shadow:0 8px 24px rgba(20,35,60,.08);padding:16px}.videoBox{background:#101827;border-radius:8px;overflow:hidden;aspect-ratio:16/10;display:flex;align-items:center;justify-content:center}video{width:100%;height:100%;object-fit:cover}.row{display:flex;gap:8px;margin-top:10px}.row input{flex:1;border:1px solid #cbd5e1;border-radius:6px;padding:12px;font-size:16px}button,.btn{border:0;border-radius:6px;background:#0f6ab4;color:white;padding:12px 14px;font-weight:bold;cursor:pointer;text-decoration:none;display:inline-block}button.secondary,.btn.secondary{background:#475569}.status{margin-top:12px;border-radius:8px;padding:14px;background:#edf4ff;border:1px solid #bfdbfe}.status.ok{background:#ecfdf5;border-color:#86efac}.status.err{background:#fff1f2;border-color:#fecdd3}.big{font-size:30px;font-weight:bold}.muted{color:#64748b}.tools{display:flex;gap:8px;flex-wrap:wrap}@media(max-width:780px){.grid{grid-template-columns:1fr}.top{display:block}.tools{margin-top:10px}}");
		html.append("</style></head><body><div class='wrap'>");
		html.append("<div class='top'><div class='brand'><h1>Verifikasi Gerbang Antar Jemput</h1><p>Scan QR kartu, tap RFID, atau input manual saat penjemput tiba di gerbang.</p></div>");
		html.append("<div class='tools'><a class='btn secondary' href='").append(root)
				.append("/antarJemput?action=card&secret=").append(esc(secret)).append("'>Cetak Kartu QR</a></div></div>");
		html.append("<div class='grid'><div class='panel'><div class='videoBox'><video id='preview' autoplay muted playsinline></video></div>");
		html.append("<div class='row'><button onclick='startCamera()'>Buka Kamera HP</button><button class='secondary' onclick='stopCamera()'>Stop</button></div>");
		html.append("<div class='row'><input id='kode' autofocus placeholder='Nomor kartu / RFID / QR'><button onclick='submitManual()'>Verifikasi</button></div>");
		html.append("<div class='row'><input id='jadwal' placeholder='ID jadwal opsional'><input id='pintu' value='Gerbang Utama'></div></div>");
		html.append("<div class='panel'><div class='muted'>Status terakhir</div><div id='status' class='status'><div class='big'>Siap</div><div>Arahkan kamera ke QR atau tap kartu RFID.</div></div>");
		html.append("<p class='muted'>RFID reader model keyboard wedge cukup diarahkan ke input nomor kartu lalu tekan Enter. Kamera memakai BarcodeDetector bawaan browser bila tersedia.</p></div></div></div>");
		html.append("<script>");
		html.append("var root='").append(js(root)).append("',secret='").append(js(secret))
				.append("',stream=null,detector=null,scanning=false,last='';");
		html.append("function setStatus(ok,title,msg){var el=document.getElementById('status');el.className='status '+(ok?'ok':'err');el.innerHTML='<div class=\"big\">'+title+'</div><div>'+msg+'</div>';}");
		html.append("function submitManual(){var v=document.getElementById('kode').value;if(v){verify(v);}}");
		html.append("document.getElementById('kode').addEventListener('keydown',function(e){if(e.keyCode==13){submitManual();}});");
		html.append("async function startCamera(){try{stream=await navigator.mediaDevices.getUserMedia({video:{facingMode:'environment'}});var v=document.getElementById('preview');v.srcObject=stream;scanning=true;if('BarcodeDetector' in window){detector=new BarcodeDetector({formats:['qr_code','code_128','ean_13']});scanLoop();setStatus(true,'Kamera aktif','Silakan arahkan kamera ke QR kartu.');}else{setStatus(false,'Scanner kamera tidak tersedia','Browser ini belum mendukung BarcodeDetector. Gunakan input manual atau RFID.');}}catch(e){setStatus(false,'Kamera gagal',e.message);}}");
		html.append("function stopCamera(){scanning=false;if(stream){stream.getTracks().forEach(function(t){t.stop();});stream=null;}setStatus(true,'Siap','Kamera dihentikan.');}");
		html.append("async function scanLoop(){if(!scanning||!detector){return;}try{var codes=await detector.detect(document.getElementById('preview'));if(codes&&codes.length){var value=codes[0].rawValue;if(value&&value!==last){last=value;verify(value);setTimeout(function(){last='';},3000);}}}catch(e){}requestAnimationFrame(scanLoop);}");
		html.append("function verify(code){var body='action=verify&kode='+encodeURIComponent(code)+'&jadwal='+encodeURIComponent(document.getElementById('jadwal').value)+'&pintu='+encodeURIComponent(document.getElementById('pintu').value)+'&secret='+encodeURIComponent(secret);fetch(root+'/antarJemput',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:body}).then(function(r){return r.json();}).then(function(d){document.getElementById('kode').value='';var t=d.success?'Valid':'Ditolak';var msg=d.message+(d.nomorAntrian?' | Antrian: '+d.nomorAntrian:'')+(d.namaPenjemput?' | '+d.namaPenjemput:'');setStatus(d.success,t,msg);}).catch(function(e){setStatus(false,'Error',e.message);});}");
		html.append("</script></body></html>");
		writeHtml(response, html.toString());
	}

	/**
	 * Merender halaman cetak kartu QR penjemput; bila {@code nomor} diberikan, mencari
	 * {@link KartuPenjemputAntarJemput} yang cocok untuk ditampilkan nama/hubungan/target-nya.
	 *
	 * <p>
	 * <b>DITAMBAL 2026-09-01:</b> pencarian kartu berdasarkan {@code nomor} kini HANYA dijalankan
	 * bila parameter {@code secret} cocok dengan konfigurasi {@code antar_jemput_api_secret} — lihat
	 * {@link #isValidGateSecret(String)} dan catatan keamanan pada javadoc kelas. Tanpa
	 * {@code secret} valid, halaman tetap dirender tapi tanpa data kartu (mencegah enumerasi nomor
	 * kartu untuk memanen nama penjemput terdaftar).
	 * </p>
	 */
	private void renderCardPage(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html;charset=UTF-8");
		String root = request.getContextPath();
		String nomor = firstNotBlank(request.getParameter("nomor"), "");
		String gateSecret = firstNotBlank(request.getParameter("secret"), "");
		boolean secretValid = isValidGateSecret(gateSecret);
		KartuPenjemputAntarJemput kartu = null;
		Session session = null;
		try {
			// Pencarian kartu berdasarkan `nomor` HANYA dijalankan bila `secret` valid — mencegah
			// enumerasi nomor kartu untuk memanen nama/hubungan penjemput terdaftar tanpa kredensial
			// (lihat catatan keamanan pada javadoc kelas). Tanpa secret valid, halaman tetap dirender
			// (kartu=null) seperti kondisi "nomor kosong" yang sudah ada.
			if (nomor.length() > 0 && secretValid) {
				session = HibernateUtil.getSessionFactory().openSession();
				kartu = findKartu(session, nomor);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AntarJemput.java:290");
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:293");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:294");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AntarJemput.java:295");}
			}
		}

		String code = kartu == null ? nomor : firstNotBlank(kartu.getBarcode(), kartu.getNomorKartu());
		String nama = kartu == null ? "" : kartu.getNamaPenjemput();
		String hubungan = kartu == null ? "" : kartu.getHubungan();
		String target = kartu == null ? "" : namaPemilik(kartu);
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'>");
		html.append("<title>Cetak Kartu QR Antar Jemput</title>");
		html.append("<style>body{font-family:Arial,Helvetica,sans-serif;background:#eef3f8;margin:0;color:#172033}.wrap{max-width:860px;margin:0 auto;padding:20px}.toolbar{display:flex;gap:8px;margin-bottom:14px;flex-wrap:wrap}.toolbar input{border:1px solid #cbd5e1;border-radius:6px;padding:11px;font-size:15px}.btn,button{border:0;border-radius:6px;background:#0f6ab4;color:#fff;padding:11px 14px;font-weight:bold;text-decoration:none}.card{width:340px;background:#fff;border:1px solid #cbd5e1;border-radius:8px;padding:16px;box-shadow:0 8px 24px rgba(20,35,60,.12)}.title{font-size:18px;font-weight:bold}.muted{color:#64748b}.qr{width:180px;height:180px;margin:14px auto;display:flex;align-items:center;justify-content:center;border:1px dashed #cbd5e1}.code{text-align:center;font-weight:bold;word-break:break-word}.name{font-size:20px;font-weight:bold;margin-top:10px}.line{border-top:1px solid #e2e8f0;margin:12px 0}@media print{body{background:#fff}.toolbar{display:none}.wrap{padding:0}.card{box-shadow:none}}</style>");
		html.append("<script src='https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js'></script></head><body><div class='wrap'>");
		html.append("<form class='toolbar' method='get' action='").append(root)
				.append("/antarJemput'><input type='hidden' name='action' value='card'><input type='hidden' name='secret' value='")
				.append(esc(gateSecret))
				.append("'><input name='nomor' value='").append(esc(nomor)).append("' placeholder='Nomor kartu / QR'><button>Cari</button><a class='btn' href='").append(root).append("/antarJemput'>Verifikasi Gerbang</a><button type='button' onclick='window.print()'>Cetak</button></form>");
		html.append("<div class='card'><div class='title'>Kartu Penjemput Antar Jemput</div><div class='muted'>AIS eCampus/eSchool</div><div id='qr' class='qr'>QR</div><div class='code' id='code'>").append(esc(code)).append("</div><div class='line'></div>");
		html.append("<div class='name'>").append(esc(nama)).append("</div><div>").append(esc(hubungan)).append("</div><div class='muted'>").append(esc(target)).append("</div>");
		html.append("<div class='line'></div><div class='muted'>Tanggal cetak: ").append(esc(new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()))).append("</div></div></div>");
		html.append("<script>var c='").append(js(code)).append("';if(c&&window.QRCode){document.getElementById('qr').innerHTML='';new QRCode(document.getElementById('qr'),{text:c,width:180,height:180});}</script>");
		html.append("</body></html>");
		writeHtml(response, html.toString());
	}

	private String namaPemilik(KartuPenjemputAntarJemput kartu) {
		if (kartu.getSiswa() != null) return "Siswa: " + kartu.getSiswa().getNama();
		if (kartu.getMahasiswa() != null) return "Mahasiswa: " + kartu.getMahasiswa().getNama();
		if (kartu.getGuru() != null) return "Guru: " + kartu.getGuru().getNama();
		if (kartu.getDosen() != null) return "Dosen: " + kartu.getDosen().getNama();
		if (kartu.getPegawai() != null) return "Pegawai: " + kartu.getPegawai().getNama();
		return "";
	}

	private void writeJson(HttpServletResponse response, boolean success, String message, String namaPenjemput,
			int jumlahPeserta, String nomorAntrian) throws IOException {
		StringBuilder json = new StringBuilder();
		json.append("{\"success\":").append(success ? "true" : "false");
		json.append(",\"message\":\"").append(json(message)).append("\"");
		json.append(",\"namaPenjemput\":\"").append(json(namaPenjemput)).append("\"");
		json.append(",\"jumlahPeserta\":").append(jumlahPeserta);
		json.append(",\"nomorAntrian\":\"").append(json(nomorAntrian)).append("\"}");
		writeHtml(response, json.toString());
	}

	private void writeHtml(HttpServletResponse response, String value) throws IOException {
		PrintWriter writer = response.getWriter();
		writer.print(value);
		writer.flush();
	}

	private String firstNotBlank(String a, String b) {
		if (a != null && a.trim().length() > 0) return a.trim();
		return b == null ? "" : b.trim();
	}

	private String firstNotBlank(String a, String b, String c) {
		String value = firstNotBlank(a, b);
		return value.length() > 0 ? value : firstNotBlank(c, "");
	}

	private Long parseLong(String value) {
		try {
			if (value == null || value.trim().length() == 0) return null;
			return Long.valueOf(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	private String esc(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private String js(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace("'", "\\'").replace("\r", "").replace("\n", "\\n");
	}

	private String json(String value) {
		if (value == null) return "";
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
	}
}
