package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisPembayaran;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.LogHostToHost;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.database.model.jatelindo.JatelindoResponse;

/**
 * Servlet <i>host-to-host</i> untuk kanal pembayaran <b>Jatelindo</b> (switching biller).
 *
 * <p>Pesan dipertukarkan dalam bentuk JSON yang meniru struktur medan ISO 8583, sehingga
 * kuncinya berupa nama bit, bukan nama yang deskriptif:</p>
 * <table border="1">
 *   <caption>Medan yang dipakai kelas ini</caption>
 *   <tr><th>Kunci</th><th>Arti</th></tr>
 *   <tr><td>{@code bit3}</td><td>kode proses: {@code 380000} inquiry, {@code 170000} payment</td></tr>
 *   <tr><td>{@code bit4}</td><td>nilai transaksi</td></tr>
 *   <tr><td>{@code bit39}</td><td>kode hasil pada balasan</td></tr>
 *   <tr><td>{@code bit48}</td><td>data tambahan; memuat nomor transaksi dan rincian penagih</td></tr>
 *   <tr><td>{@code bit62}</td><td>teks siap tampil di mesin/aplikasi mitra</td></tr>
 *   <tr><td>{@code mti}</td><td>selalu diisi {@code 0210} pada balasan</td></tr>
 * </table>
 *
 * <p>Nomor transaksi diambil dari {@code bit48} dengan cara yang berbeda per kode proses:
 * pada inquiry dipotong sampai spasi pertama, pada payment diambil dari posisi 10 sampai 26.</p>
 *
 * <h4>Kode hasil yang dikirim balik</h4>
 * <ul>
 *   <li>{@code 00} &mdash; berhasil;</li>
 *   <li>{@code 14} &mdash; nomor transaksi tidak dikenal, atau host pemanggil tidak dikenali;</li>
 *   <li>{@code 34} &mdash; tagihan sudah pernah dibayar;</li>
 *   <li>{@code 51} &mdash; nilai transaksi tidak sama dengan tagihan ditambah biaya administrasi.</li>
 * </ul>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tidak ada verifikasi tanda tangan atau MAC</h4>
 * <p>Berbeda dengan lazimnya pesan ISO 8583, kelas ini <b>tidak memeriksa MAC, tanda tangan,
 * token, maupun kredensial apa pun</b> atas pesan yang masuk &mdash; tidak pada cabang inquiry
 * dan tidak pada cabang payment. Satu-satunya penjagaan adalah {@code bankHost != null}, hasil
 * pemetaan alamat IP oleh {@code PembayaranUtil.getBankHost(String, String)}. Penjagaan itu
 * lemah karena pemetaan tersebut punya dua jalur pelonggaran: konfigurasi
 * {@code apabila_bank_host_tidak_ditemukan_buat_data_bank_otomatis} yang membuat baris
 * {@link BankHost} baru untuk IP pemanggil apa pun, dan baris cadangan ber-IP {@code 0.0.0.0}
 * yang menampung sisanya. Pada {@code applicationContext-security.xml},
 * {@code /JatelindoCallback} jatuh ke aturan penampung {@code /**} yang bernilai
 * {@code IS_AUTHENTICATED_ANONYMOUSLY}.</p>
 * <p>Yang membatasi kerugian di sini hanyalah bahwa nomor transaksi harus cocok dengan
 * {@link JatelindoRequest} yang memang sudah diterbitkan, dan nilai transaksi harus sama
 * persis; nilai yang benar sendiri dapat dibaca lebih dahulu lewat cabang inquiry.</p>
 *
 * <h4>Catatan arsitektur</h4>
 * <p>Setiap permintaan wajib menghasilkan satu baris {@link LogHostToHost} yang ditulis di blok
 * {@code finally}, lengkap dengan jejak <i>stack trace</i> bila terjadi galat. Ini pola
 * <i>audit shadow</i> yang berlaku di seluruh gerbang pembayaran AIS dan merupakan fakta
 * arsitektur yang disengaja, bukan cacat.</p>
 *
 * @see ais.database.model.jatelindo.JatelindoRequest
 * @see ais.database.model.jatelindo.JatelindoResponse
 */
public class JatelindoCallback extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton pembantu pembayaran, dipakai untuk memetakan alamat IP pemanggil menjadi
	 * {@link BankHost} dan untuk mengambil rincian biaya mahasiswa.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun; seluruh kebergantungan diambil lewat field
	 * statis {@link #pembayaranUtil}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public JatelindoCallback() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process(HttpServletRequest,
	 * HttpServletResponse)}.
	 *
	 * <p>Perilakunya sama dengan POST karena payload selalu dibaca dari badan permintaan, bukan
	 * dari <i>query string</i>. Kegagalan ditelan {@link Common#tampilErrorJikaAdmin(Exception)}
	 * sehingga mitra tidak menerima kode status 5xx.</p>
	 *
	 * @param request  permintaan masuk dari switching Jatelindo
	 * @param response balasan yang akan diisi JSON bermedan bit
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menangani permintaan HTTP POST &mdash; metode yang lazim dipakai Jatelindo &mdash; dengan
	 * meneruskannya ke {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request  permintaan masuk dari switching Jatelindo
	 * @param response balasan yang akan diisi JSON bermedan bit
	 * @throws ServletException bila kontainer menandai kegagalan servlet
	 * @throws IOException      bila penulisan balasan gagal
	 * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			process(request, response);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Mencari {@link Kegiatan} yang sesuai dengan sebuah {@link JatelindoRequest}, atau
	 * membuatnya bila belum ada.
	 *
	 * <p>Pencarian memakai {@code ambilKegiatans(semester, jenisKegiatan)} milik
	 * {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}. Khusus calon mahasiswa, bila
	 * pencarian ber-semester gagal dan semester bernilai satu atau kurang, dicoba sekali lagi
	 * tanpa semester &mdash; menampung data pendaftaran yang semesternya belum pasti.</p>
	 *
	 * <p>Kegiatan baru diberi {@code validated = 1} dan {@code validator} berupa nama merchant,
	 * lalu langsung disimpan. Kegiatan yang sudah ada hanya di-{@code refresh} sehingga nilainya
	 * tidak ditimpa.</p>
	 *
	 * @param jatelindoRequest permintaan Jatelindo yang memuat mahasiswa, semester, tahun
	 *                         akademik, jenis kegiatan, dan jadwal pembayaran
	 * @param session          session Hibernate aktif milik pemanggil; transaksi dibuka dan
	 *                         di-<i>commit</i> di dalam method ini
	 * @return kegiatan yang ditemukan atau yang baru dibuat
	 */
	public static Kegiatan createKegiatan(JatelindoRequest jatelindoRequest, Session session) {
		Kegiatan kegiatan = null;
		Double nilaiBiayaHarusDiBayars = jatelindoRequest.getNilaiBiayaHarusDiBayars();

		Mahasiswa mhs = jatelindoRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = jatelindoRequest.getBiodataCalonMahasiswa();

		Integer semester = jatelindoRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = jatelindoRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = jatelindoRequest.getJenisKegiatan();
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
			if (kegiatan == null && semester <= 1) {
				kegiatan = bio.ambilKegiatans(jenisKegiatan);
			}
		}

		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ jatelindoRequest.getPengurangan() + ", hapusCicilanSebelumnya ==> "
				+ jatelindoRequest.getHapusCicilanSebelumnya());

		if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = new Kegiatan();
			kegiatan.setJenisKegiatan(jenisKegiatan);
			kegiatan.setJadwalPembayaran(jadwalPembayaran);
			kegiatan.setMahasiswa(mhs);
			kegiatan.setCalonMahasiswa(bio);
			kegiatan.setSemster(semester);
			if (mhs != null) {
				kegiatan.setStatusMahasiswa(Common
						.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster()).getStatusMahasiswa());
			} else if (bio != null) {
				kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
			}
			kegiatan.setTahunAkademik(jatelindoRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(jatelindoRequest.getMerchant());
			kegiatan.setPengurangan(jatelindoRequest.getPengurangan());
			kegiatan.setKeterangan(jatelindoRequest.getKeterangan());
			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}
		return kegiatan;
	}


	/**
	 * Menguji apakah sebuah {@link JatelindoRequest} sudah pernah diproses, sebagai penjaga
	 * idempotensi terhadap notifikasi ganda.
	 *
	 * <p>Penilaian dilakukan atas koleksi {@link KegiatanTemporary} milik permintaan: dianggap
	 * sudah diproses hanya bila koleksi itu <b>tidak kosong</b> dan <b>seluruh</b> anggotanya
	 * sudah tertaut ke {@link Kegiatan} yang ber-id. Satu anggota yang belum tertaut membuat
	 * seluruh permintaan dianggap belum selesai sehingga pemrosesan diulang.</p>
	 *
	 * <p><b>Perhatikan:</b> permintaan yang sama sekali tidak memakai keranjang
	 * ({@code kegiatanTemporarys} kosong) selalu dinilai {@code false}. Untuk jalur itu
	 * idempotensi ditegakkan di tempat lain, yaitu lewat kolom {@code ref} pada
	 * {@link CicilanPembayaran} dan lewat pemeriksaan {@code kodeStatus} bernilai {@code "2"}
	 * yang menghasilkan kode hasil {@code 34}.</p>
	 *
	 * <p>Bersifat <i>fail-open</i>: kegagalan membaca koleksi dicatat lalu dijawab {@code false},
	 * sehingga pemrosesan tetap dicoba.</p>
	 *
	 * @param jatelindoRequest permintaan yang diperiksa; {@code null} dijawab {@code false}
	 * @return {@code true} bila seluruh kegiatan sementara sudah tertaut ke kegiatan nyata
	 */
	private static boolean isRequestSudahDiproses(JatelindoRequest jatelindoRequest) {
		if (jatelindoRequest == null) {
			return false;
		}
		try {
			Collection temporarys = jatelindoRequest.getKegiatanTemporarys();
			if (temporarys != null && !temporarys.isEmpty()) {
				boolean semuaSudahTerhubungKeKegiatan = true;
				for (Object object : temporarys) {
					KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) object;
					if (kegiatanTemporary == null || kegiatanTemporary.getKegiatan() == null
							|| kegiatanTemporary.getKegiatan().getId() == null) {
						semuaSudahTerhubungKeKegiatan = false;
						break;
					}
				}
				return semuaSudahTerhubungKeKegiatan;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	/**
	 * Menindaklanjuti pembayaran Jatelindo yang berhasil: mengubah tagihan menjadi pembayaran
	 * nyata.
	 *
	 * <p>{@link JatelindoRequest} dicari dari {@code trxId} milik respons. Seluruh pekerjaan
	 * hanya dijalankan bila {@code kodeStatus} bernilai {@code "2"} (lunas), dan
	 * {@link #isRequestSudahDiproses} dipanggil lebih dahulu agar notifikasi berulang tidak
	 * menggandakan pembayaran.</p>
	 *
	 * <h4>Dua bentuk tagihan</h4>
	 * <ul>
	 *   <li><b>Keranjang</b> ({@code kegiatanTemporarys} tidak kosong) &mdash; tiap
	 *       {@link KegiatanTemporary} diproses pada session tersendiri: {@link Kegiatan} nyata
	 *       dicari atau dibuat, seluruh {@link CicilanPembayaran} yang menunjuk kegiatan
	 *       sementara dialihkan ke kegiatan nyata (masing-masing pada session tersendiri pula),
	 *       lalu kegiatan sementara ditandai sudah tertaut. Total dan tunggakan dihitung ulang
	 *       dari jumlah nilai cicilan.</li>
	 *   <li><b>Non-keranjang</b> &mdash; {@link #createKegiatan} dipanggil, lalu tiap
	 *       {@link JatelindoRequestDetail} diubah menjadi {@link CicilanPembayaran}. Setelahnya
	 *       tunggakan diperbarui dan bukti pembayaran dicetak.</li>
	 * </ul>
	 *
	 * <p>Jenis pembayaran ditentukan dari konfigurasi {@code kode_akun_jatelindo}, dan bila
	 * nominal lebih dari 0,1 sebuah {@link LogPembayaran} dibuat atau dimutakhirkan.</p>
	 *
	 * <p><b>Keamanan:</b> method ini mempercayai penuh {@code kodeStatus} pada
	 * {@link JatelindoResponse} yang dibentuk dari pesan masuk tanpa verifikasi MAC atau tanda
	 * tangan; lihat peringatan pada dokumentasi kelas.</p>
	 *
	 * <p>Penyimpanan dilakukan dalam banyak transaksi kecil pada beberapa session yang
	 * di-<i>commit</i> berurutan, sehingga kegagalan di tengah dapat meninggalkan keadaan
	 * setengah jadi.</p>
	 *
	 * @param jatelindoResponse respons yang memuat nomor transaksi dan kode status pembayaran
	 */
	@SuppressWarnings({ "unchecked", "static-access" })
	public static void prosesResponse(JatelindoResponse jatelindoResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		JatelindoRequest jatelindoRequest = (JatelindoRequest) session.createCriteria(JatelindoRequest.class)
				.add(Restrictions.eq("trxId", jatelindoResponse.getTrxId())).setMaxResults(1).uniqueResult();

		if (jatelindoRequest != null && jatelindoResponse.getKodeStatus().toString().trim().equalsIgnoreCase("2")) {

			if (isRequestSudahDiproses(jatelindoRequest)) {
				System.out.println("Callback jatelindoRequest sudah pernah diproses, proses pembayaran dilewati: " + jatelindoRequest);
				return;
			}

			jatelindoRequest.setJatelindoResponse(jatelindoResponse);
			jatelindoRequest.setStatus("Payment Sukses");
			jatelindoRequest.setKodeStatus(jatelindoResponse.getKodeStatus());
			session.getTransaction().begin();
			session.update(jatelindoRequest);
			session.getTransaction().commit();

			String kodeAkun = Common.getKonfigurasi("kode_akun_jatelindo", "").getNilai();
			JenisPembayaran jenisPembayaran = JenisPembayaran.ambilJenisPembayaranBerdasarkanKodeAkun(session,
					kodeAkun);

			if (!jatelindoRequest.getKegiatanTemporarys().isEmpty()) {
				Kegiatan kegiatan = null;
				for (KegiatanTemporary temporary : jatelindoRequest.getKegiatanTemporarys()) {

					Session sessionLocalKeg = null;
					try {
					sessionLocalKeg = HibernateUtil.getSessionFactory().openSession();
						KegiatanTemporary kegiatanTemporary = (KegiatanTemporary) sessionLocalKeg
								.createCriteria(KegiatanTemporary.class).add(Restrictions.idEq(temporary.getId()))
								.uniqueResult();
						if (kegiatanTemporary != null) {

							try {
								Mahasiswa mahasiswa = kegiatanTemporary.getMahasiswa();
								Integer smt = kegiatanTemporary.getSemster();
								JenisKegiatan jenisKegiatan = kegiatanTemporary.getJenisKegiatan();
								kegiatan = mahasiswa.ambilKegiatansRefresh(smt, jenisKegiatan);

								if (kegiatan == null || kegiatan.getId() == null) {
									kegiatan = new Kegiatan();
								} else {
									kegiatan = (Kegiatan) sessionLocalKeg.createCriteria(Kegiatan.class)
											.add(Restrictions.idEq(kegiatan.getId())).uniqueResult();
								}

								kegiatan.setJenisKegiatan(jenisKegiatan);
								kegiatan.setJadwalPembayaran(kegiatanTemporary.getJadwalPembayaran());
								kegiatan.setMahasiswa(mahasiswa);
								kegiatan.setSemster(smt);
								kegiatan.setStatusMahasiswa(kegiatanTemporary.getStatusMahasiswa());
								kegiatan.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
								kegiatan.setTanggal(kegiatanTemporary.getTanggal());
								kegiatan.setValidated(1);
								kegiatan.setJenisKegiatan(jenisKegiatan);
								kegiatan.setValidator(jatelindoRequest.getMerchant());
								kegiatan.setKeterangan(kegiatanTemporary.getKeterangan());

								sessionLocalKeg.getTransaction().begin();
								Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
								sessionLocalKeg.getTransaction().commit();

								List<CicilanPembayaran> cicilanPembayarans = sessionLocalKeg
										.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.eq("kegiatanTemporary.id", temporary.getId())).list();

								System.out.println("cicilanPembayarans==>" + cicilanPembayarans.size());
								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									Session sessionLocal = null;
									try {
										sessionLocal = HibernateUtil.getSessionFactory().openSession();
										try {
											sessionLocal.refresh(cicilanPembayaran);
											cicilanPembayaran.setKegiatan(kegiatan);
											cicilanPembayaran.setValidator(jatelindoRequest.getMerchant());
											cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
											sessionLocal.getTransaction().begin();
											Common.refreshSaveOrUpdate(sessionLocal, cicilanPembayaran);
											sessionLocal.getTransaction().commit();
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:251");
										}
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:254");
									} finally {
										if (sessionLocal != null) {
											try { sessionLocal.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:257");}
											try { sessionLocal.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:258");}
											try { sessionLocal.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:259");}
										}
									}
								}

								try {
									if (kegiatanTemporary.getKegiatan() == null
											|| (kegiatanTemporary.getKegiatan() != null && !kegiatanTemporary
													.getKegiatan().getId().equals(kegiatan.getId()))) {
										sessionLocalKeg.getTransaction().begin();
										kegiatanTemporary.setKegiatan(kegiatan);
										Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatanTemporary);
										sessionLocalKeg.getTransaction().commit();
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:273");
									// TODO: handle exception
								}

								Number jumlah = (Number) sessionLocalKeg.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.isNotNull("itemBiaya"))
										.add(Restrictions.eq("kegiatan", kegiatan))
										.setProjection(Projections.sum("nilai")).uniqueResult();
								Double nilaiBiayaHarusDiBayars = 0.0;
								Double amountTotal = jumlah == null ? 0.0 : jumlah.doubleValue();

								try {

									Map<Long, DetailBiaya> map = new java.util.HashMap<Long, DetailBiaya>();
									Collection<DetailBiaya> mydetailBiayas = pembayaranUtil
											.getDetailBiayaMahasiswa(mahasiswa, smt, jenisKegiatan, false);
									for (Object o : mydetailBiayas) {
										if (o instanceof DetailBiaya) {
											DetailBiaya detailBiaya = (DetailBiaya) o;
											map.put(detailBiaya.getId(), detailBiaya);
										} else if (o instanceof PengaturanPembayaranBulanan) {
											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
											DetailBiaya detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
											map.put(detailBiaya.getId(), detailBiaya);
										}
									}

									for (DetailBiaya detailBiaya : map.values()) {
										nilaiBiayaHarusDiBayars += Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
									}

									kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - amountTotal);
									kegiatan.setAmount(amountTotal);
									sessionLocalKeg.getTransaction().begin();
									Common.refreshSaveOrUpdate(sessionLocalKeg, kegiatan);
									sessionLocalKeg.getTransaction().commit();
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/JatelindoCallback.java:315");
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					} finally {
						if (sessionLocalKeg != null) {
							try { sessionLocalKeg.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:322");}
							try { sessionLocalKeg.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:323");}
							try { sessionLocalKeg.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:324");}
						}
					}
				}

				if (jatelindoRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:339");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setJatelindoRequest(jatelindoRequest);
					logPembayaran.setNominal(jatelindoRequest.getAmount());
					logPembayaran.setKeterangan(jatelindoRequest.getRequest());
					logPembayaran.setKegiatan(kegiatan);
					logPembayaran.setValidator(jatelindoRequest.getMerchant());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

			} else {

				Kegiatan kegiatan = JatelindoCallback.createKegiatan(jatelindoRequest, session);
				Double nilaiBiayaHarusDiBayars = jatelindoRequest.getNilaiBiayaHarusDiBayars();

				List<JatelindoRequestDetail> jatelindoRequestDetails = session
						.createCriteria(JatelindoRequestDetail.class).add(Restrictions.isNull("idCicilan"))
						.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).add(Restrictions.gt("ke", 0))
						.addOrder(Order.asc("ke")).add(Restrictions.gt("ke", 0)).list();
				System.out.println("jatelindoRequestDetails==>" + jatelindoRequestDetails);
				if (!jatelindoRequestDetails.isEmpty()) {

					for (JatelindoRequestDetail jatelindoRequestDetail : jatelindoRequestDetails) {

						String ref = "jatelindoRequestDetail-" + jatelindoRequestDetail.getId();

						CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
								.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref))
								.setMaxResults(1).uniqueResult();
						if (cicilanPembayaran == null) {
							cicilanPembayaran = new CicilanPembayaran(jatelindoRequestDetail.getDetailBiaya());

						}
						cicilanPembayaran.setRef(ref);
						cicilanPembayaran.setValidator(jatelindoRequest.getMerchant());
						cicilanPembayaran.setKe(jatelindoRequestDetail.getKe());
						cicilanPembayaran.setKegiatan(kegiatan);
						cicilanPembayaran.setItemBiaya(jatelindoRequestDetail.getItemBiaya());
						cicilanPembayaran.setPengaturanPembayaranBulanan(
								jatelindoRequestDetail.getPengaturanPembayaranBulanan());
						cicilanPembayaran.setNilai(jatelindoRequestDetail.getNilai());
						cicilanPembayaran.setTanggal(jatelindoRequestDetail.getTanggal());
						cicilanPembayaran.setJenisPembayaran(jenisPembayaran);
						cicilanPembayaran.setDenda(jatelindoRequestDetail.getDenda());
						cicilanPembayaran.setNilaiAsli(jatelindoRequestDetail.getNilaiAsli());
						session.getTransaction().begin();
						if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
						session.getTransaction().commit();

						jatelindoRequestDetail.setCicilanref(cicilanPembayaran.getId());
						session.getTransaction().begin();
						session.update(jatelindoRequestDetail);
						session.getTransaction().commit();
					}

					Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
					Double jumlah = d[0];
					Double denda = d[1];
					kegiatan.setDenda(denda.doubleValue());
					kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
					kegiatan.setAmount(jumlah.doubleValue());

					kegiatan.setValidator(jatelindoRequest.getMerchant());

					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, kegiatan);
					session.getTransaction().commit();
				}

				if (jatelindoRequest.getAmount() > 0.1) {
					LogPembayaran logPembayaran = (LogPembayaran) session.createCriteria(LogPembayaran.class)
							.add(Restrictions.eq("jatelindoRequest", jatelindoRequest)).setMaxResults(1).uniqueResult();
					if (logPembayaran == null) {
						logPembayaran = new LogPembayaran();
					}
					double biayaAdministrasi = 0.0;
					try {
						biayaAdministrasi = Double
								.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:422");

					}
					logPembayaran.setBiayaAdministrasi(biayaAdministrasi);
					logPembayaran.setJatelindoRequest(jatelindoRequest);
					logPembayaran.setKegiatan(kegiatan);
					logPembayaran.setNominal(jatelindoRequest.getAmount());
					logPembayaran.setKeterangan(jatelindoRequest.getRequest());
					logPembayaran.setValidator(jatelindoRequest.getMerchant());
					session.getTransaction().begin();
					Common.refreshSaveOrUpdate(session, logPembayaran);
					session.getTransaction().commit();
				}

				pembayaranUtil.updateTunggakan(kegiatan, session);

				Mahasiswa mhs = jatelindoRequest.getMahasiswa();
				BiodataCalonMahasiswa bio = jatelindoRequest.getBiodataCalonMahasiswa();
				if (mhs != null) {
					CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
				} else if (bio != null) {
					CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
				}
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:449");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:450");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:451");}
			}
		}
	}

	/**
	 * Mencatat satu pesan Jatelindo sebagai baris {@link JatelindoResponse}.
	 *
	 * <p>Nomor transaksi diambil dari potongan {@code bit48} sampai spasi pertama. Baris yang
	 * sudah ada dicari berdasarkan {@code trxId} yang sama, diambil yang id-nya terbesar; bila
	 * tidak ada, baris baru dibuat. Isi pesan disimpan apa adanya pada kolom {@code keterangan},
	 * dan status awal selalu {@link JatelindoResponse#SEDANG_DIPROSES} &mdash; status sebenarnya
	 * baru diisi pemanggil di {@link #process(String, HttpServletRequest, BankHost, boolean)}.</p>
	 *
	 * <p>Session Hibernate dibuka sendiri dan ditutup di blok {@code finally}; objek yang
	 * dikembalikan karena itu bersifat <i>detached</i>.</p>
	 *
	 * @param jatelindo pesan masuk dalam bentuk JSON bermedan bit
	 * @return baris {@link JatelindoResponse} yang tersimpan
	 * @throws Exception bila {@code bit48} tidak ada atau penyimpanan gagal
	 */
	public static JatelindoResponse prosesTransaksi(JSONObject jatelindo) throws Exception {
		String bit48Request = jatelindo.getString("bit48");
		String trx_id = bit48Request.split(" ")[0].trim();
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			JatelindoResponse jatelindoResponse = (JatelindoResponse) session.createCriteria(JatelindoResponse.class)
					.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (jatelindoResponse == null) {
				jatelindoResponse = new JatelindoResponse();
			}
			jatelindoResponse.setKeterangan(jatelindo.toString());
			jatelindoResponse.setNama(trx_id);
			jatelindoResponse.setStatus(JatelindoResponse.SEDANG_DIPROSES);
			jatelindoResponse.setTrxId(trx_id);
			session.getTransaction().begin();
			session.save(jatelindoResponse);
			session.getTransaction().commit();
			return jatelindoResponse;
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:477");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:478");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:479");}
			}
		}
	}

	/**
	 * Membaca badan permintaan sebagai satu string JSON, memprosesnya, lalu menuliskan balasan.
	 *
	 * <p>Alamat IP pemanggil dipetakan lebih dahulu menjadi {@link BankHost} dengan label
	 * {@code "Jatelindo"}, lalu seluruh pekerjaan diserahkan ke
	 * {@link #process(String, HttpServletRequest, BankHost, boolean)} dengan
	 * {@code tetaplanjut} bernilai {@code false}. Balasan selalu dikirim sebagai
	 * {@code application/json}.</p>
	 *
	 * <p>Perhatikan bahwa badan permintaan dibaca baris demi baris dan pemisah barisnya dibuang,
	 * sehingga payload multi-baris digabung rapat.</p>
	 *
	 * @param request  permintaan masuk dari switching Jatelindo
	 * @param response balasan yang akan diisi JSON bermedan bit
	 * @throws Exception bila pembacaan permintaan atau penulisan balasan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
		BankHost bankHost = pembayaranUtil.getBankHost(request.getRemoteAddr(), "Jatelindo");
		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();

		String hasil = JatelindoCallback.process(data, request, bankHost, false);

		response.setHeader("Content-Type", "application/json");
		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

	/**
	 * Inti pemrosesan pesan Jatelindo: menjawab inquiry atau membukukan payment.
	 *
	 * <h4>Urutan kerja</h4>
	 * <ol>
	 *   <li>Payload diurai menjadi JSON; nilai transaksi diambil dari {@code bit4}.</li>
	 *   <li>Nomor transaksi diambil dari {@code bit48}: dipotong sampai spasi pertama bila
	 *       {@code bit3} bernilai {@code 380000}, atau diambil posisi 10&ndash;26 bila
	 *       {@code bit3} bernilai {@code 170000}.</li>
	 *   <li>{@link JatelindoRequest} dicari dari nomor itu. Bila tidak ada, <b>atau</b>
	 *       {@code bankHost} bernilai {@code null}, balasan diberi kode {@code bit39 = 14}.</li>
	 *   <li>Bila tagihan sudah berstatus {@code "2"} dan {@code tetaplanjut} bernilai
	 *       {@code false}, balasan diberi kode {@code 34}.</li>
	 *   <li>Nilai transaksi harus sama persis dengan {@code amount + biayaAdministrasi};
	 *       bila tidak, balasan diberi kode {@code 51}.</li>
	 * </ol>
	 *
	 * <h4>Dua cabang transaksi</h4>
	 * <ul>
	 *   <li><b>Inquiry</b> ({@code bit3 = 380000}) &mdash; status permintaan disetel
	 *       {@code "Sedang diproses"} berkode {@code "1"}, lalu {@code bit48} balasan disusun
	 *       dari nomor rekening alias, nomor VA, nama, jenis transaksi, keterangan, id transaksi,
	 *       nominal, biaya administrasi, nomor telepon, dan surel &mdash; masing-masing dipotong
	 *       atau dipadatkan ke lebar tetap oleh {@code Common.maxPanjangSpace} dan
	 *       {@code Common.maxPanjangNol}. {@code bit62} berisi versi berlabel yang siap
	 *       ditampilkan.</li>
	 *   <li><b>Payment</b> ({@code bit3 = 170000}) &mdash; status disetel {@code "Payment
	 *       Sukses"} berkode {@code "2"}, {@link #prosesResponse} dijalankan untuk membukukan
	 *       pembayaran, dan {@code bit48} balasan ditambahi nomor referensi baru.</li>
	 * </ul>
	 * <p>Kedua cabang mengisi {@code bit39} dengan {@code "00"} bila berhasil, dan blok
	 * {@code finally} selalu menyetel {@code mti} menjadi {@code "0210"}.</p>
	 *
	 * <p><b>Keamanan:</b> tidak ada verifikasi MAC, tanda tangan, atau kredensial pada cabang
	 * mana pun. Penjagaan {@code bankHost != null} bersifat berbasis IP dan lemah; lihat
	 * peringatan pada dokumentasi kelas.</p>
	 *
	 * <p>Blok {@code finally} selalu menulis satu {@link LogHostToHost}. Alamat IP yang dicatat
	 * diambil berurutan dari header {@code Cf-Connecting-Ip}, {@code CF-Connecting-IP},
	 * {@code X-Forwarded-For}, {@code X-Real-IP}, baru {@code getRemoteAddr()} &mdash; sehingga
	 * nilai yang tercatat berasal dari header yang dapat dipalsukan pemanggil, dan hanya layak
	 * dipercaya bila di depan aplikasi memang ada proksi yang menimpanya.</p>
	 *
	 * @param request     permintaan asal, dipakai untuk pencatatan log H2H; boleh {@code null}
	 * @param data        payload JSON mentah bermedan bit
	 * @param bankHost    host bank hasil pemetaan IP; {@code null} membuat balasan berkode 14
	 * @param tetaplanjut {@code true} memaksa pemrosesan diteruskan walau tagihan sudah lunas
	 * @return string JSON balasan yang siap dikirim ke mitra
	 * @throws Exception bila kegagalan terjadi di luar jangkauan penanganan internal
	 */
	@SuppressWarnings({})
	public static String process(String data, HttpServletRequest request, BankHost bankHost, boolean tetaplanjut)
			throws Exception {

		System.out.println("==> JatelindoCallback data => " + data);

		JatelindoRequest jatelindoRequest = null;
		String trx_id = "-0000";
		JSONObject jatelindo = new JSONObject();
		String hasil = "";
		// Jejak stack trace bila pemrosesan callback/pembayaran error; disimpan ke kolom log H2H.
		String h2hStackTrace = null;
		try {
			jatelindo = new JSONObject(data);
			Long nilaiTransaksi = Long.parseLong(jatelindo.getString("bit4").trim());
			Session session = HibernateUtil.getSessionFactory().openSession();
			try {

			String bit48Request = jatelindo.getString("bit48");
			if (jatelindo.getString("bit3").equals("380000")) {
				trx_id = bit48Request.split(" ")[0].trim();
			} else if (jatelindo.getString("bit3").equals("170000")) {
				trx_id = bit48Request.substring(10, 26).trim();
			}

			jatelindoRequest = (JatelindoRequest) session.createCriteria(JatelindoRequest.class)
					.add(Restrictions.eq("trxId", trx_id)).setMaxResults(1).uniqueResult();

			System.out.println(
					"trx_id = " + trx_id + " jatelindoRequest = " + jatelindoRequest + " bankHost " + bankHost);
			if (jatelindoRequest != null && bankHost != null) {

				if (jatelindoRequest.getKodeStatus().equals("2") && !tetaplanjut) {
					jatelindo.put("bit39", "34");
				} else {

					Double amn = jatelindoRequest.getAmount() + jatelindoRequest.getBiayaAdministrasi();
					if (nilaiTransaksi.equals(amn.longValue())) {

						String trx = "000000000000000000000" + jatelindoRequest.getId();
						trx = trx.substring(trx.length() - 8);

						Mahasiswa mahasiswa = jatelindoRequest.getMahasiswa();
						BiodataCalonMahasiswa biodataCalonMahasiswa = jatelindoRequest.getBiodataCalonMahasiswa();

						// String merchant_id =
						// Common.getKonfigurasi("jatelindo_merchant_id",
						// "129").getNilai().trim();
						String no_rek_alias = Common.getKonfigurasi("jatelindo_no_rek_alias", "ecamp").getNilai()
								.trim();

						String label_universitas = Common.getKonfigurasi("label_universitas", "-").getNilai().trim();

						String Norek_alias = Common.maxPanjangSpace(no_rek_alias, 10);
						String No_VA = Common.maxPanjangSpace(trx_id, 16);
						String Nama = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getNama(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getNama(), 30);
						String Jenis_trx = Common.maxPanjangSpace("TOPUP", 30);
						String Keterangan = Common
								.maxPanjangSpace("Bayar " + (jatelindoRequest.getJenisKegiatan() == null ? ""
										: jatelindoRequest.getJenisKegiatan().getNama()), 30);
						String Id_trx = Common.maxPanjangSpace(trx, 30);
						String amount = Common.maxPanjangNol(amn.intValue() + "", 12);
						String admin = Common.maxPanjangNol("0", 12);
						String hp = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getTelp(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getHp(), 30);
						String email = mahasiswa != null ? Common.maxPanjangSpace(mahasiswa.getEmail(), 30)
								: Common.maxPanjangSpace(
										biodataCalonMahasiswa == null ? "" : biodataCalonMahasiswa.getEmail(), 30);

						if (jatelindo.getString("bit3").equals("380000")) {

							JatelindoResponse jatelindoResponse = prosesTransaksi(jatelindo);
							String payment_status_desc = "Sedang diproses";
							String payment_status_code = "1";

							session.refresh(jatelindoResponse);
							jatelindoResponse.setKeterangan(jatelindo.toString());
							jatelindoResponse.setNama(jatelindoRequest.getTrxId());
							jatelindoResponse.setStatus(
									payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
							jatelindoResponse.setTrxId(jatelindoRequest.getTrxId());
							jatelindoResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							jatelindoResponse
									.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoResponse);
							session.getTransaction().commit();

							session.refresh(jatelindoRequest);
							jatelindoRequest
									.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
							jatelindoRequest
									.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoRequest);
							session.getTransaction().commit();

							System.out.println("Request " + jatelindo);

							String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan + Id_trx + amount + admin
									+ hp + email;
							jatelindo.put("bit48", bit48);
							jatelindo.put("bit39", "00");

							String bit62 = Common.maxPanjangSpace("No.VA", 10)
									+ Common.maxPanjangSpace(no_rek_alias + "-" + No_VA, 30)
									+ Common.maxPanjangSpace("Nama", 10) + Nama + Common.maxPanjangSpace("Jenis", 10)
									+ Jenis_trx + Common.maxPanjangSpace("Nominal", 10)
									+ Common.maxPanjangSpace(
											"Rp. " + Common.numberFormat.get().format(Long.parseLong(amount)), 30)
									+ Common.maxPanjangSpace("Instansi", 10)
									+ Common.maxPanjangSpace(label_universitas, 30);
							jatelindo.put("bit62", bit62);

						} else if (jatelindo.getString("bit3").equals("170000")) {

							JatelindoResponse jatelindoResponse = prosesTransaksi(jatelindo);
							String payment_status_desc = "Payment Sukses";
							String payment_status_code = "2";

							session.refresh(jatelindoResponse);
							jatelindoResponse.setKeterangan(jatelindo.toString());
							jatelindoResponse.setNama(jatelindoRequest.getTrxId());
							jatelindoResponse.setStatus(
									payment_status_desc == null ? "Belum diproses" : payment_status_desc.toString());
							jatelindoResponse.setMerchant(
									jatelindo.isNull("merchant_id") ? "" : jatelindo.get("merchant_id").toString());
							jatelindoResponse.setTrxId(jatelindoRequest.getTrxId());
							jatelindoResponse.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							jatelindoResponse
									.setKodeStatus(payment_status_code == null ? "0" : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoResponse);
							session.getTransaction().commit();

							session.refresh(jatelindoRequest);
							jatelindoRequest
									.setStatus(payment_status_desc == null ? null : payment_status_desc.toString());
							jatelindoRequest
									.setKodeStatus(payment_status_code == null ? null : payment_status_code.toString());
							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, jatelindoRequest);
							session.getTransaction().commit();

							JatelindoCallback.prosesResponse(jatelindoResponse);

							System.out.println("Payment " + jatelindo);
							String reffNum = Common.maxPanjangSpace(Common.getGeneratedBarCode(), 30);
							String bit48 = Norek_alias + No_VA + Nama + Jenis_trx + Keterangan + Id_trx + amount + admin
									+ hp + email + reffNum;
							jatelindo.put("bit48", bit48);
							jatelindo.put("bit39", "00");

							String bit62 = Common.maxPanjangSpace("No.VA", 10)
									+ Common.maxPanjangSpace(no_rek_alias + "-" + No_VA, 30)
									+ Common.maxPanjangSpace("Nama", 10) + Nama + Common.maxPanjangSpace("Jenis", 10)
									+ Jenis_trx + Common.maxPanjangSpace("Refnum", 10)
									+ Common.maxPanjangSpace(Id_trx, 30) + Common.maxPanjangSpace("Instansi", 10)
									+ Common.maxPanjangSpace(label_universitas, 30);
							jatelindo.put("bit62", bit62);

						}
					} else {
						jatelindo.put("bit39", "51");
					}
				}
			} else {
				jatelindo.put("bit39", "14");
			}
			} finally {
				if (session != null) {
					try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:677");}
					try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:678");}
					try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:679");}
				}
			}
		} catch (Exception e) {
			h2hStackTrace = ais.action.ws.util.PembayaranGatewayHelper.ambilStackTrace(e);
			// jatelindo.put("bit39", "14");
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// JAMINAN: log H2H SELALU dicatat & tercommit walau terjadi error/exception
			// (helper: session terdedikasi + commit + retry, tak pernah gagal).
			try {
				jatelindo.put("mti", "0210");
			} catch (Exception eMti) { ais.common.ErrorAuditUtil.record(eMti, "auto-audit(empty-catch) src/ais/action/servlet/JatelindoCallback.java:691");
			}
			hasil = jatelindo.toString();
			System.out.println("response " + hasil);
			try {
				LogHostToHost logHostToHost = new LogHostToHost();
				logHostToHost.setKeterangan(data);
				try {
					if (request != null && request.getHeader("Cf-Connecting-Ip") != null) {
						logHostToHost.setIp(request.getHeader("Cf-Connecting-Ip"));
					} else if (request != null && request.getHeader("CF-Connecting-IP") != null) {
						logHostToHost.setIp(request.getHeader("CF-Connecting-IP"));
					} else if (request != null && request.getHeader("X-Forwarded-For") != null) {
						logHostToHost.setIp(request.getHeader("X-Forwarded-For"));
					} else if (request != null && request.getHeader("X-Real-IP") != null) {
						logHostToHost.setIp(request.getHeader("X-Real-IP"));
					} else {
						logHostToHost.setIp(
								request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
					}
				} catch (Exception e) {
					logHostToHost
							.setIp(request != null ? request.getRemoteAddr() : (bankHost != null ? bankHost.getIp() : ""));
				}
				logHostToHost.setBankHost(bankHost);
				logHostToHost.setNim(jatelindoRequest == null ? ""
						: (jatelindoRequest.getMahasiswa() != null ? jatelindoRequest.getMahasiswa().getNim()
								: jatelindoRequest.getBiodataCalonMahasiswa() != null
										? jatelindoRequest.getBiodataCalonMahasiswa().getNoRegistrasi()
										: ""));
				logHostToHost.setKode(trx_id);
				logHostToHost.setNama(jatelindoRequest == null ? ""
						: (jatelindoRequest.getMahasiswa() != null ? jatelindoRequest.getMahasiswa().getNama()
								: jatelindoRequest.getBiodataCalonMahasiswa() != null
										? jatelindoRequest.getBiodataCalonMahasiswa().getNama()
										: ""));
				logHostToHost.setTanggal(ais.ui.util.WaktuUtil.getDate());
				logHostToHost.setResponseDescription(hasil);
				logHostToHost.setNominal(jatelindoRequest == null ? 0.0
						: (jatelindoRequest.getAmount() + jatelindoRequest.getBiayaAdministrasi()));
				logHostToHost.setItem(jatelindo == null ? "" : jatelindo.toString());
				logHostToHost.setStackTrace(h2hStackTrace);
				ais.action.ws.util.PembayaranGatewayHelper.simpanLogHostToHost(logHostToHost);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		return hasil;

	}

}
