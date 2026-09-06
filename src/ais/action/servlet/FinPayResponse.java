package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.action.report.CommonReportHelper;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.database.model.finpay.FinpayResponse;

/**
 * Servlet penerima notifikasi pembayaran <b>FinPay</b> bentuk kedua, terpasang di {@code web.xml}
 * pada URL {@code /FinPayResponse}.
 *
 * <p>Berbeda dari {@link Finpay} (dipetakan ke {@code /Finpay}) yang menangani payload JSON
 * bersarang dan bekerja di atas {@link ais.database.model.VirtualAccountBank}, kelas ini menangani
 * bentuk notifikasi FinPay yang lebih lama: pasangan {@code nama=nilai} yang dipisah {@code &} pada
 * badan permintaan (identik gaya urai dengan {@link IPayMuResponse}), dan bekerja di atas pasangan
 * entity {@link ais.database.model.finpay.FinpayRequest}/{@link ais.database.model.finpay.FinpayResponse}
 * yang dibuat lebih dulu oleh {@code FinpayRequestAction} saat mahasiswa/calon mahasiswa memulai
 * pembayaran.</p>
 *
 * <h4>Alur kerja</h4>
 * <p>{@link #process} mengurai badan permintaan menjadi peta nama-nilai, lalu
 * {@link #prosesTransaksi} menyimpannya sebagai satu baris {@link ais.database.model.finpay.FinpayResponse}
 * tanpa syarat, dan {@link #prosesResponse} mencocokkannya ke {@link ais.database.model.finpay.FinpayRequest}
 * lewat pasangan {@code paymentCode}+{@code invoice} lalu membukukan tagihan.</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tidak ada verifikasi tanda tangan/status sama sekali</h4>
 * <ul>
 *   <li>Sama seperti {@link Finpay}: tidak ada satu pun medan permintaan yang diperiksa terhadap
 *       tanda tangan atau kredensial rahasia sebelum dipercaya, dan tidak ada pemetaan IP pemanggil
 *       ke {@code BankHost} sama sekali di kelas ini (berbeda dari {@link Finpay}/{@link Bjb}/
 *       {@link Jaring} yang setidaknya memetakan IP meski tidak menjadikannya gerbang). Medan
 *       {@code mer_signature} (dipetakan ke {@link ais.database.model.finpay.FinpayResponse#getNama()},
 *       lihat catatan keamanan pada Javadoc kelas tersebut) disimpan apa adanya, tidak pernah
 *       diverifikasi.</li>
 *   <li><b>{@link #prosesResponse} membukukan pembayaran tanpa memeriksa {@code resultCode}/
 *       {@code resultDesc} sama sekali</b> &mdash; begitu {@link ais.database.model.finpay.FinpayRequest}
 *       ditemukan lewat {@code paymentCode}+{@code invoice}, seluruh cicilan langsung dibukukan
 *       tanpa cabang "berhasil vs gagal" seperti pada {@link Finpay#doProcess} (status VA) atau
 *       {@link IPayMuResponse#prosesResponse} (status {@code BERHASIL}). Siapa pun yang mengetahui
 *       atau menebak pasangan {@code payment_code}+{@code invoice} sebuah permintaan yang masih
 *       berjalan dapat mem-POST notifikasi palsu ke {@code /FinPayResponse} dan membukukannya sebagai
 *       lunas, tanpa pembayaran nyata terjadi di sisi FinPay dan tanpa perlu memalsukan status apa
 *       pun. Ini pola yang sama (bahkan lebih longgar) dengan yang sudah dipetakan sebagai temuan
 *       H2H berulang di seluruh AIS &mdash; lihat peringatan pada Javadoc {@link Finpay}.</li>
 *   <li>Rute {@code /FinPayResponse} tidak muncul di {@code applicationContext-security.xml} dan
 *       jatuh ke aturan penampung {@code /**} yang bernilai {@code IS_AUTHENTICATED_ANONYMOUSLY}.</li>
 * </ul>
 *
 * @see Finpay
 * @see IPayMuResponse
 * @see ais.database.model.finpay.FinpayRequest
 * @see ais.database.model.finpay.FinpayResponse
 */
public class FinPayResponse extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena instance
	 * servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton pembantu pembayaran, dipakai di {@link #prosesResponse} untuk menghitung ulang total
	 * dan denda cicilan lewat {@link PembayaranUtil#getTotalDanDendaFromCicilan} dan untuk
	 * memutakhirkan tunggakan lewat {@link PembayaranUtil#updateTunggakan}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun; seluruh kebergantungan diambil lewat field statis
	 * {@link #pembayaranUtil}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public FinPayResponse() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>FinPay lazimnya mengirim notifikasi lewat POST, tetapi GET diperlakukan identik untuk
	 * berjaga-jaga terhadap konfigurasi mitra yang berbeda. Kegagalan ditelan
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga pengirim tidak menerima 5xx.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat {@code "00"}
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
	 * Menangani permintaan HTTP POST &mdash; metode yang lazim dipakai FinPay &mdash; dengan
	 * meneruskannya ke {@link #process}.
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat {@code "00"}
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
	 * Mencocokkan satu {@link FinpayResponse} ke {@link FinpayRequest} yang menunggu lewat pasangan
	 * {@code paymentCode}+{@code invoice}, lalu membukukan pembayaran <b>tanpa syarat status apa
	 * pun</b>.
	 *
	 * <p>Pencocokan memakai {@code Restrictions.ilike("paymentCode", ..., MatchMode.EXACT)} dan
	 * {@code Restrictions.ilike("invoice", ..., MatchMode.EXACT)} sekaligus (tanpa memandang
	 * besar-kecil huruf). Begitu {@link FinpayRequest} ditemukan, {@code finpayResponse} langsung
	 * ditautkan dan seluruh langkah pembukuan berikut dijalankan &mdash; <b>tidak ada</b> pemeriksaan
	 * {@code resultCode}/{@code resultDesc} sebelumnya; lihat peringatan pada Javadoc kelas.</p>
	 *
	 * <ol>
	 *   <li>{@link Kegiatan} pemilik tagihan diambil/dibentuk lewat {@link #createKegiatan};</li>
	 *   <li>bila {@code finpayRequest.getAmount() &gt; 0.1}, satu baris {@link LogPembayaran}
	 *       dicatat;</li>
	 *   <li>setiap {@link ais.database.model.finpay.FinpayRequestDetail} (cicilan ke-1 dan seterusnya,
	 *       diurutkan {@code ke} ascending) diubah menjadi satu baris {@link CicilanPembayaran},
	 *       di-<i>upsert</i> lewat kunci {@code ref} berbentuk
	 *       {@code "finpayRequestDetail-" + id} sehingga notifikasi berulang tidak menggandakan
	 *       cicilan;</li>
	 *   <li>total dan denda dihitung ulang dari seluruh {@link CicilanPembayaran} kegiatan lewat
	 *       {@link PembayaranUtil#getTotalDanDendaFromCicilan}, {@link Kegiatan} disimpan, dan
	 *       tunggakannya dimutakhirkan lewat {@link PembayaranUtil#updateTunggakan};</li>
	 *   <li>bukti pembayaran dicetak lewat {@link CommonReportHelper} sesuai jenis pemilik
	 *       ({@link Mahasiswa} atau {@link BiodataCalonMahasiswa}).</li>
	 * </ol>
	 *
	 * <p>Sesi Hibernate dibuka khusus untuk pemanggilan ini dan selalu dibersihkan
	 * (<i>clear</i>/<i>disconnect</i>/<i>close</i>, masing-masing ditelan bila gagal) pada blok
	 * {@code finally}.</p>
	 *
	 * @param finpayResponse baris respons FinPay yang baru saja disimpan oleh {@link #prosesTransaksi}
	 */
	@SuppressWarnings("unchecked")
	public static void prosesResponse(FinpayResponse finpayResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		FinpayRequest finpayRequest = (FinpayRequest) session.createCriteria(FinpayRequest.class)
				.add(Restrictions.ilike("paymentCode", finpayResponse.getPaymentCode(), MatchMode.EXACT))
				.add(Restrictions.ilike("invoice", finpayResponse.getInvoice(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		System.out.println("finpayRequest==>" + finpayRequest);
		if (finpayRequest != null) {
			finpayRequest.setFinpayResponse(finpayResponse);
			session.getTransaction().begin();
			session.update(finpayRequest);
			session.getTransaction().commit();

			Double nilaiBiayaHarusDiBayars = finpayRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = createKegiatan(finpayRequest, finpayResponse, session);

			if (finpayRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(finpayRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<FinpayRequestDetail> finpayRequestDetails = session.createCriteria(FinpayRequestDetail.class)
					.add(Restrictions.eq("finpayRequest", finpayRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("finpayRequestDetails==>" + finpayRequestDetails);
			if (!finpayRequestDetails.isEmpty()) {

				for (FinpayRequestDetail finpayRequestDetail : finpayRequestDetails) {

					String ref = "finpayRequestDetail-" + finpayRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(finpayRequestDetail.getDetailBiaya());

					}

					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setValidator(finpayResponse.getPaymentSource());
					cicilanPembayaran.setKe(finpayRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(finpayRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(finpayRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(finpayRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(finpayRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
					cicilanPembayaran.setDenda(finpayRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(finpayRequestDetail.getNilaiAsli());
					session.getTransaction().begin();
					if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());

				kegiatan.setValidator(finpayResponse.getPaymentSource());

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			Mahasiswa mhs = finpayRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = finpayRequest.getBiodataCalonMahasiswa();
			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:170");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:171");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:172");}
			}
		}
	}

	/**
	 * Membangun satu {@link FinpayResponse} dari medan-medan mentah notifikasi FinPay, menyimpannya
	 * tanpa syarat, lalu meneruskannya ke {@link #prosesResponse}.
	 *
	 * <p>Medan yang diambil: {@code mer_signature} (disimpan sebagai {@code nama}, lihat catatan
	 * penamaan pada {@link FinpayResponse#getNama()}), {@code trax_type}, {@code merchant_id},
	 * {@code invoice}, {@code payment_code} (kunci pencocokan ke {@link FinpayRequest} bersama
	 * {@code invoice}), {@code result_code}, {@code result_desc}, {@code log_no}, dan
	 * {@code payment_source}. Baris disimpan apa adanya <b>sebelum</b> {@code result_code} diperiksa
	 * &mdash; dan sebagaimana dijelaskan pada Javadoc kelas, {@code result_code} tidak pernah
	 * diperiksa sama sekali oleh {@link #prosesResponse}.</p>
	 *
	 * @param param peta nama-nilai hasil urai badan permintaan oleh {@link #process}
	 */
	public static void prosesTransaksi(Map<String, String> param) {
		FinpayResponse finpayResponse = new FinpayResponse();
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		finpayResponse.setNama(param.get("mer_signature"));
		finpayResponse.setTipe(param.get("trax_type"));
		finpayResponse.setMerchant(param.get("merchant_id"));
		finpayResponse.setInvoice(param.get("invoice"));
		finpayResponse.setPaymentCode(param.get("payment_code"));
		finpayResponse.setResultCode(param.get("result_code"));
		finpayResponse.setResultDesc(param.get("result_desc"));
		finpayResponse.setLogNo(param.get("log_no"));
		finpayResponse.setPaymentSource(param.get("payment_source"));
		session.getTransaction().begin();
		session.save(finpayResponse);
		session.getTransaction().commit();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:196");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:197");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/FinPayResponse.java:198");}
			}
		}

		prosesResponse(finpayResponse);
	}

	/**
	 * Membaca notifikasi FinPay dari badan permintaan, mengurainya, memprosesnya, dan menuliskan
	 * balasan.
	 *
	 * <p>Badan permintaan dibaca baris demi baris menjadi satu string (pemisah baris dibuang), lalu
	 * diurai manual: dipecah dengan pemisah {@code &} menjadi token, dan tiap token dipecah lagi
	 * dengan pemisah {@code =} menjadi pasangan nama-nilai. Kegagalan pengurain satu token ditelan
	 * per token sehingga token bermasalah cukup dilewati. Setelahnya, seluruh parameter permintaan
	 * standar ({@code request.getParameterMap()}) ikut ditambahkan/menimpa peta hasil urai badan
	 * &mdash; sehingga notifikasi dapat dikirim lewat query string, badan berenkode form, atau
	 * campuran keduanya.</p>
	 *
	 * <p>Peta hasil diteruskan ke {@link #prosesTransaksi}, lalu balasan teks biasa {@code "00"}
	 * dituliskan dengan header {@code Content-Type: text/plain} &mdash; format balasan yang diharapkan
	 * FinPay untuk menandakan notifikasi diterima.</p>
	 *
	 * @param request  permintaan masuk dari FinPay
	 * @param response balasan berisi teks status singkat {@code "00"}
	 * @throws Exception bila pembacaan permintaan atau penulisan balasan gagal
	 */
	@SuppressWarnings({})
	private void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Read from request
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();
		String line;
		while ((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		String data = buffer.toString();
		System.out.println("==> FinPayResponse data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		for (Object p : request.getParameterMap().keySet()) {
			param.put(p.toString(), request.getParameter(p.toString()));
		}

		System.out.println("==> param => " + param);
		FinPayResponse.prosesTransaksi(param);
		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write("00");
	}

	/**
	 * Mengambil {@link Kegiatan} pemilik tagihan yang sudah ada untuk {@code finpayRequest}, atau
	 * membentuk satu baris baru bila belum ada.
	 *
	 * <p>Pencarian lewat {@code ambilKegiatans(semester, jenisKegiatan)} pada {@link Mahasiswa} bila
	 * {@code finpayRequest} menunjuk mahasiswa, atau pada {@link BiodataCalonMahasiswa} (dengan
	 * jatuh ke overload tanpa {@code semester} untuk semester pertama) bila menunjuk calon
	 * mahasiswa. Bila tidak ditemukan, {@link Kegiatan} baru dibentuk dengan
	 * {@code validated=1}, {@code validator} diisi {@code finpayResponse.getPaymentSource()} (atau
	 * literal {@code "Finpay"} bila {@code finpayResponse} bernilai {@code null}), status mahasiswa
	 * disalin dari status akademik berjalan (atau {@code ConstantValues.AKTIF} untuk calon
	 * mahasiswa), dan nominal awal diisi {@code finpayRequest.getNilaiBiayaHarusDiBayars()}; baris
	 * baru langsung disimpan dalam transaksi sendiri.</p>
	 *
	 * <p>Bila {@link Kegiatan} yang sudah ada ditemukan, method hanya memanggil
	 * {@code session.refresh(kegiatan)} tanpa mengubah data apa pun &mdash; pemutakhiran nilai
	 * dilakukan belakangan oleh {@link #prosesResponse}.</p>
	 *
	 * @param finpayRequest  permintaan pembayaran FinPay yang menjadi sumber data kegiatan
	 * @param finpayResponse balasan FinPay untuk permintaan ini; boleh {@code null}
	 * @param session        sesi Hibernate aktif yang dipakai untuk kueri dan penyimpanan
	 * @return {@link Kegiatan} yang sudah ada (di-<i>refresh</i>) atau baris baru yang baru disimpan
	 */
	public static Kegiatan createKegiatan(FinpayRequest finpayRequest, FinpayResponse finpayResponse, Session session) {
		Mahasiswa mhs = finpayRequest.getMahasiswa();
		BiodataCalonMahasiswa bio = finpayRequest.getBiodataCalonMahasiswa();

		Integer semester = finpayRequest.getSemester();
		JadwalPembayaran jadwalPembayaran = finpayRequest.getJadwalPembayaran();
		JenisKegiatan jenisKegiatan = finpayRequest.getJenisKegiatan();

		Kegiatan kegiatan = null;
		if (mhs != null) {
			kegiatan = mhs.ambilKegiatans(semester, jenisKegiatan);
		} else if (bio != null) {
			kegiatan = bio.ambilKegiatans(semester, jenisKegiatan);
			if (kegiatan == null && semester <= 1) {
				kegiatan = bio.ambilKegiatans(jenisKegiatan);
			}
		}

		Double nilaiBiayaHarusDiBayars = finpayRequest.getNilaiBiayaHarusDiBayars();
		System.out.println("mhs==>" + mhs + ",bio==>" + bio + ", semester==>" + semester + ",jadwalPembayaran==>"
				+ jadwalPembayaran + ",jenisKegiatan==>" + jenisKegiatan + ",kegiatan==>" + kegiatan
				+ ",nilaiBiayaHarusDiBayars==>" + nilaiBiayaHarusDiBayars + ",pengurangan==>"
				+ finpayRequest.getPengurangan());

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
			kegiatan.setTahunAkademik(finpayRequest.getTahunAkademik());
			kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
			kegiatan.setValidated(1);
			kegiatan.setValidator(finpayResponse == null ? "Finpay" : finpayResponse.getPaymentSource());
			kegiatan.setPengurangan(finpayRequest.getPengurangan());
			kegiatan.setKeterangan(finpayRequest.getKeterangan());

			kegiatan.setAmount(nilaiBiayaHarusDiBayars);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, kegiatan);
			session.getTransaction().commit();
		} else {
			session.refresh(kegiatan); 
		}

		return kegiatan;
	}

}
