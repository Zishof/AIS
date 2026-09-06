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
import ais.database.model.ipaymu.IpaymuRequest;
import ais.database.model.ipaymu.IpaymuRequestDetail;
import ais.database.model.ipaymu.IpaymuResponse;

/**
 * Servlet penerima notifikasi pembayaran <b>iPaymu</b>, terpasang di {@code web.xml} pada URL
 * {@code /IPayMuResponse}.
 *
 * <p>Bentuk pesan berupa pasangan {@code nama=nilai} yang dipisah {@code &} pada badan permintaan
 * (bukan JSON seperti {@link Finpay} atau {@link FinPayResponse}); lihat {@link #process}. Alur
 * kerjanya dua tahap: {@link #prosesTransaksi} menyimpan seluruh medan mentah sebagai satu baris
 * {@link IpaymuResponse}, lalu {@link #prosesResponse} mencocokkannya ke {@link IpaymuRequest} yang
 * sudah dibuat lebih dulu (saat mahasiswa/calon mahasiswa memulai pembayaran) dan membukukan
 * tagihan bila statusnya berhasil.</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; tidak ada verifikasi tanda tangan sama sekali</h4>
 * <ul>
 *   <li>Tidak ada satu pun medan pada payload iPaymu (termasuk {@code sid}) yang diperiksa
 *       terhadap tanda tangan atau kredensial rahasia sebelum dipercaya. {@link #prosesResponse}
 *       mencari {@link IpaymuRequest} semata dari medan {@code sid} (disimpan sebagai
 *       {@code nama}) dan langsung membukukan pembayaran bila {@code status} bernilai
 *       {@link IpaymuResponse#BERHASIL}.</li>
 *   <li>Ini pola yang sama dengan {@link Finpay} (lihat peringatan pada javadoc kelas tersebut):
 *       siapa pun yang mengetahui atau menebak nilai {@code sid} sebuah permintaan pembayaran yang
 *       masih berjalan dapat mengirim POST palsu ke {@code /IPayMuResponse} dengan
 *       {@code status=Berhasil} dan membukukannya sebagai lunas, tanpa pembayaran nyata terjadi di
 *       sisi iPaymu.</li>
 *   <li>Tidak ada pemetaan alamat IP pemanggil ke {@code BankHost} atau mekanisme setara yang
 *       membatasi siapa yang boleh memanggil endpoint ini; rute jatuh ke aturan penampung
 *       {@code IS_AUTHENTICATED_ANONYMOUSLY} pada {@code applicationContext-security.xml}.</li>
 * </ul>
 *
 * <h4>Catatan idempotensi</h4>
 * <p>Penautan {@link IpaymuResponse} ke {@link IpaymuRequest} hanya dilakukan sekali (dijaga
 * {@code ipaymuRequest.getIpaymuResponse() == null}), tetapi blok pembukuan di bawahnya
 * <b>tidak</b> ikut dijaga kondisi yang sama &mdash; ia berjalan setiap kali status
 * {@code BERHASIL} diterima untuk {@code sid} yang sama. Perlindungan terhadap pembukuan ganda
 * bertumpu sepenuhnya pada kunci {@code ref} per baris {@link CicilanPembayaran} (pola
 * <i>upsert</i> yang sama dipakai di seluruh gerbang pembayaran AIS), bukan pada pemeriksaan
 * eksplisit di method ini.
 *
 * @see FinPayResponse
 * @see Finpay
 */
public class IPayMuResponse extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/** Singleton pembantu pembayaran, dipakai di {@link #prosesResponse} untuk memutakhirkan tunggakan. */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet.
	 *
	 * <p>Tidak melakukan inisialisasi apa pun; seluruh kebergantungan diambil lewat field statis
	 * {@link #pembayaranUtil}.</p>
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public IPayMuResponse() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan meneruskannya ke {@link #process}.
	 *
	 * <p>iPaymu lazimnya mengirim notifikasi lewat POST, tetapi GET diperlakukan identik untuk
	 * berjaga-jaga terhadap konfigurasi mitra yang berbeda. Kegagalan ditelan
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga pengirim tidak menerima 5xx.</p>
	 *
	 * @param request  permintaan masuk dari iPaymu
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
	 * Menangani permintaan HTTP POST &mdash; metode yang lazim dipakai iPaymu &mdash; dengan
	 * meneruskannya ke {@link #process}.
	 *
	 * @param request  permintaan masuk dari iPaymu
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
	 * Mencocokkan satu {@link IpaymuResponse} ke {@link IpaymuRequest} yang menunggu, lalu
	 * membukukan pembayaran bila statusnya berhasil.
	 *
	 * <p>Pencocokan memakai {@code Restrictions.ilike("nama", ipaymuResponse.getNama(),
	 * MatchMode.EXACT)} &mdash; membandingkan medan {@code sid} pada respons terhadap medan
	 * {@code nama} pada {@link IpaymuRequest} tanpa memandang besar-kecil huruf, tanpa syarat
	 * lain. Bila cocok dan {@link IpaymuRequest} belum punya {@link IpaymuResponse} tertaut,
	 * keduanya ditautkan.</p>
	 *
	 * <p>Bila status bernilai {@link IpaymuResponse#BERHASIL} (tanpa memandang besar-kecil huruf
	 * dan spasi tepi), pembukuan dijalankan:</p>
	 * <ol>
	 *   <li>{@link Kegiatan} pemilik tagihan dicari lewat {@code ambilKegiatans} pada
	 *       {@link Mahasiswa} atau {@link BiodataCalonMahasiswa}; bila tidak ditemukan, satu baris
	 *       baru dibuat dengan {@code validator="iPaymu"} dan nominal dari
	 *       {@code ipaymuRequest.getNilaiBiayaHarusDiBayars()};</li>
	 *   <li>bila {@code ipaymuRequest.getAmount() &gt; 0.1}, satu baris {@link LogPembayaran}
	 *       dicatat;</li>
	 *   <li>setiap {@link IpaymuRequestDetail} (cicilan ke-1 dan seterusnya) diubah menjadi satu
	 *       baris {@link CicilanPembayaran}, di-<i>upsert</i> lewat kunci {@code ref} berbentuk
	 *       {@code "ipaymuRequestDetail-" + id} sehingga notifikasi berulang tidak menggandakan
	 *       cicilan;</li>
	 *   <li>total dan denda dihitung ulang dari seluruh {@link CicilanPembayaran} kegiatan lewat
	 *       {@link PembayaranUtil#getTotalDanDendaFromCicilan}, lalu {@link Kegiatan} disimpan dan
	 *       tunggakannya dimutakhirkan lewat {@link PembayaranUtil#updateTunggakan};</li>
	 *   <li>bukti pembayaran dicetak lewat {@link CommonReportHelper} sesuai jenis pemilik
	 *       ({@link Mahasiswa} atau {@link BiodataCalonMahasiswa}).</li>
	 * </ol>
	 *
	 * <p><b>Keamanan:</b> method ini tidak melakukan verifikasi apa pun terhadap keaslian
	 * {@code ipaymuResponse} &mdash; lihat peringatan pada javadoc kelas.</p>
	 *
	 * <p>Sesi Hibernate dibuka khusus untuk pemanggilan ini dan selalu dibersihkan
	 * (<i>clear</i>/<i>disconnect</i>/<i>close</i>, masing-masing ditelan bila gagal) pada blok
	 * {@code finally}.</p>
	 *
	 * @param ipaymuResponse baris respons iPaymu yang baru saja disimpan oleh
	 *                       {@link #prosesTransaksi}
	 */
	@SuppressWarnings("unchecked")
	public static void prosesResponse(IpaymuResponse ipaymuResponse) {
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		IpaymuRequest ipaymuRequest = (IpaymuRequest) session.createCriteria(IpaymuRequest.class)
				.add(Restrictions.ilike("nama", ipaymuResponse.getNama(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		// System.out.println("ipaymuRequest==>" + ipaymuRequest);
		if (ipaymuRequest != null && ipaymuRequest.getIpaymuResponse() == null) {
			ipaymuRequest.setIpaymuResponse(ipaymuResponse);
			session.getTransaction().begin();
			session.update(ipaymuRequest);
			session.getTransaction().commit();
		}
		if (ipaymuRequest != null && ipaymuResponse.getStatus().trim().equalsIgnoreCase(IpaymuResponse.BERHASIL)) {

			Double nilaiBiayaHarusDiBayars = ipaymuRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = null;

			Mahasiswa mhs = ipaymuRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = ipaymuRequest.getBiodataCalonMahasiswa();

			Integer semester = ipaymuRequest.getSemester();
			JadwalPembayaran jadwalPembayaran = ipaymuRequest.getJadwalPembayaran();
			JenisKegiatan jenisKegiatan = ipaymuRequest.getJenisKegiatan();
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
					+ ipaymuRequest.getPengurangan());

			if (kegiatan == null || kegiatan.getId() == null) {
				kegiatan = new Kegiatan();
				kegiatan.setJenisKegiatan(jenisKegiatan);
				kegiatan.setJadwalPembayaran(jadwalPembayaran);
				kegiatan.setMahasiswa(mhs);
				kegiatan.setCalonMahasiswa(bio);
				kegiatan.setSemster(semester);
				if (mhs != null) {
					kegiatan.setStatusMahasiswa(
							ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mhs, kegiatan.getTahunAkademik(), kegiatan.getSemster())
									.getStatusMahasiswa());
				} else if (bio != null) {
					kegiatan.setStatusMahasiswa(ConstantValues.AKTIF);
				}
				kegiatan.setTahunAkademik(ipaymuRequest.getTahunAkademik());
				kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
				kegiatan.setValidated(1);
				kegiatan.setValidator("iPaymu");
				kegiatan.setPengurangan(ipaymuRequest.getPengurangan());
				kegiatan.setKeterangan(ipaymuRequest.getKeterangan());
				kegiatan.setAmount(nilaiBiayaHarusDiBayars);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			if (ipaymuRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(ipaymuRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<IpaymuRequestDetail> ipaymuRequestDetails = session.createCriteria(IpaymuRequestDetail.class)
					.add(Restrictions.eq("ipaymuRequest", ipaymuRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("ipaymuRequestDetails==>" + ipaymuRequestDetails);
			if (!ipaymuRequestDetails.isEmpty()) {

				for (IpaymuRequestDetail ipaymuRequestDetail : ipaymuRequestDetails) {

					String ref = "ipaymuRequestDetail-" + ipaymuRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(ipaymuRequestDetail.getDetailBiaya());

					}
					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setValidator("iPaymu");
					cicilanPembayaran.setKe(ipaymuRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(ipaymuRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(ipaymuRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(ipaymuRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(ipaymuRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);
					cicilanPembayaran.setDenda(ipaymuRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(ipaymuRequestDetail.getNilaiAsli());
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

				kegiatan.setValidator("iPaymu");

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			pembayaranUtil.updateTunggakan(kegiatan, session);

			if (mhs != null) {
				CommonReportHelper.cetakBuktipembayaranMahasiswa(kegiatan, true);
			} else if (bio != null) {
				CommonReportHelper.cetakBuktipembayaranCalonMahasiswa(kegiatan, true);
			}
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:216");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:217");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:218");}
			}
		}
	}

	/**
	 * Membangun satu {@link IpaymuResponse} dari medan-medan mentah notifikasi iPaymu, menyimpannya
	 * tanpa syarat, lalu meneruskannya ke {@link #prosesResponse}.
	 *
	 * <p>Medan yang diambil: {@code sid} (disimpan sebagai {@code nama}, dipakai sebagai kunci
	 * pencocokan ke {@link IpaymuRequest}), {@code status}, {@code merchant}, {@code trx_id},
	 * {@code product}, {@code buyer}, {@code no_rekening_deposit}, dan {@code comments}. Baris
	 * disimpan apa adanya <b>sebelum</b> statusnya diperiksa &mdash; termasuk notifikasi gagal atau
	 * belum tentu asli &mdash; sehingga tabel {@link IpaymuResponse} berfungsi sebagai log mentah
	 * seluruh percakapan, bukan hanya transaksi yang dibukukan.</p>
	 *
	 * @param param peta nama-nilai hasil urai badan permintaan oleh {@link #process}
	 */
	public static void prosesTransaksi(Map<String, String> param) {
		IpaymuResponse ipaymuResponse = new IpaymuResponse();
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		ipaymuResponse.setNama(param.get("sid"));
		ipaymuResponse.setStatus(param.get("status"));
		ipaymuResponse.setMerchant(param.get("merchant"));
		ipaymuResponse.setTrxId(param.get("trx_id"));
		ipaymuResponse.setProduct(param.get("product"));
		ipaymuResponse.setBuyer(param.get("buyer"));
		ipaymuResponse.setNoRekeningDeposit(param.get("no_rekening_deposit"));
		ipaymuResponse.setComments(param.get("comments"));
		session.getTransaction().begin();
		session.save(ipaymuResponse);
		session.getTransaction().commit();
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:241");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:242");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:243");}
			}
		}

		prosesResponse(ipaymuResponse);
	}

	/**
	 * Membaca notifikasi iPaymu dari badan permintaan, mengurainya, memprosesnya, dan menuliskan
	 * balasan.
	 *
	 * <p>Badan permintaan dibaca baris demi baris menjadi satu string (pemisah baris dibuang),
	 * lalu diurai manual: dipecah dengan pemisah {@code &} menjadi token, dan tiap token dipecah
	 * lagi dengan pemisah {@code =} menjadi pasangan nama-nilai. Kegagalan pengurain satu token
	 * (mis. token tanpa {@code =}, atau nilai yang sendiri mengandung {@code =}) ditelan per token
	 * sehingga token bermasalah cukup dilewati tanpa menggagalkan token lain.</p>
	 *
	 * <p>Peta hasil urai diteruskan ke {@link #prosesTransaksi}, lalu balasan teks biasa
	 * {@code "00"} dituliskan &mdash; format balasan yang diharapkan iPaymu untuk menandakan
	 * notifikasi diterima.</p>
	 *
	 * @param request  permintaan masuk dari iPaymu
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
		System.out.println("==> IPayMuResponse data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/IPayMuResponse.java:269");

			}
		}

		System.out.println("==> param => " + param);

		IPayMuResponse.prosesTransaksi(param);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write("00");
	}

}
