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
import ais.common.DokuCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.LogPembayaran;
import ais.database.model.Mahasiswa;
import ais.database.model.doku.DokuRequest;
import ais.database.model.doku.DokuRequestDetail;
import ais.database.model.doku.DokuResponse;

/**
 * Servlet notifikasi pembayaran untuk gateway Doku: menerima callback {@code RESULT}/
 * {@code TRANSIDMERCHANT}/{@code AMOUNT}/{@code WORDS} dari Doku dan, bila valid, memfinalisasi
 * pembayaran/pendaftaran terkait lewat {@link #prosesResponse(DokuResponse)}.
 *
 * <p><b>Riwayat keamanan (DIPERBAIKI 2026-09-07):</b> {@link #prosesTransaksi(Map)} sebelumnya
 * langsung mempercayai {@code TRANSIDMERCHANT} dan {@code RESULT} mentah dari request masuk TANPA
 * memverifikasi checksum {@code WORDS} apa pun — siapa pun yang mengetahui/menebak
 * {@code TRANSIDMERCHANT} suatu transaksi Doku yang masih pending dapat memanggil endpoint ini
 * langsung dengan {@code RESULT=Success} untuk memalsukan pelunasan tanpa pembayaran nyata.
 * {@link #prosesTransaksi(Map)} kini mencari {@link DokuRequest} berdasarkan {@code TRANSIDMERCHANT}
 * lalu WAJIB lulus {@link ais.common.DokuCommon#verifikasiChecksum(DokuRequest, String, String)}
 * (checksum {@code WORDS} DAN {@code AMOUNT} harus cocok dengan baris tersimpan) sebelum
 * {@link DokuResponse} disimpan dan {@link #prosesResponse(DokuResponse)} dipanggil; bila tidak
 * lulus, method langsung membalas {@code "Stop"} tanpa efek samping apa pun. Lihat javadoc
 * {@link ais.common.DokuCommon#verifikasiChecksum(DokuRequest, String, String)} untuk formula
 * checksum Doku Basic Store yang diterapkan, dan javadoc {@code ais.action.servlet.DokuVerifyServlet}
 * untuk perbaikan serupa pada endpoint pre-check.</p>
 */
public class DokuResponseServlet extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	/**
	 * Singleton helper penghitung tunggakan/pembayaran yang dipakai {@link #prosesResponse(
	 * DokuResponse)} untuk memutakhirkan tunggakan {@link Kegiatan} setelah pembayaran Doku
	 * dinyatakan berhasil, lewat {@link PembayaranUtil#updateTunggakan(Kegiatan, Session)}.
	 */
	private static PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public DokuResponseServlet() {
		super();

		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani GET dengan mendelegasikan ke {@link #process}; kegagalan apa pun ditelan dan
	 * hanya ditampilkan ke pengguna bila konteks saat ini adalah administrator, lewat
	 * {@link Common#tampilErrorJikaAdmin(Exception)} — pemanggil non-admin (termasuk gateway
	 * Doku) tidak melihat detail error.
	 *
	 * @param request  request HTTP masuk dari gateway Doku
	 * @param response response HTTP keluar; badan diisi {@code "Continue"} atau {@code "Stop"}
	 *                 oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar karena {@link #process} dibungkus
	 *                          try/catch di sini; dipertahankan hanya karena tanda tangan
	 *                          {@link HttpServlet#doGet}
	 * @throws IOException      idem, ditelan oleh blok catch
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Menangani POST dengan perilaku identik seperti {@link #doGet}: mendelegasikan ke
	 * {@link #process} dan menelan kegagalan lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 * Notifikasi Doku pada praktiknya dikirim sebagai POST, tetapi kedua verb didukung.
	 *
	 * @param request  request HTTP masuk dari gateway Doku
	 * @param response response HTTP keluar; badan diisi {@code "Continue"} atau {@code "Stop"}
	 *                 oleh {@link #process}
	 * @throws ServletException tidak pernah dilempar keluar, lihat catatan pada {@link #doGet}
	 * @throws IOException      idem
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
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
	 * Memfinalisasi satu notifikasi Doku yang SUDAH tersimpan sebagai {@link DokuResponse} (lihat
	 * {@link #prosesTransaksi(Map)} untuk gerbang checksum sebelum method ini dipanggil): mencari
	 * {@link DokuRequest} yang cocok, menautkannya ke {@code dokuResponse}, lalu — bila status
	 * {@link DokuResponse#BERHASIL} — membuat/memutakhirkan {@link Kegiatan} pembayaran,
	 * {@link LogPembayaran}, seluruh {@link CicilanPembayaran} dari {@link DokuRequestDetail}
	 * terkait, memutakhirkan tunggakan lewat {@link PembayaranUtil#updateTunggakan(Kegiatan,
	 * Session)}, dan mencetak bukti pembayaran mahasiswa/calon mahasiswa.
	 *
	 * <p>Alur ringkas: (1) balas {@code "Stop"} bila {@code dokuResponse.getNama()} kosong; (2)
	 * cari {@link DokuRequest} dengan {@code nama} yang cocok PERSIS (case-insensitive via
	 * {@link MatchMode#EXACT}) dan, bila ditemukan serta belum tertaut, tautkan ke
	 * {@code dokuResponse}; (3) bila status BUKAN {@link DokuResponse#BERHASIL} (atau
	 * {@link DokuRequest} tidak ditemukan), hasil {@code "Stop"} tanpa efek samping lain; (4) bila
	 * BERHASIL, tentukan/mahasiswa {@link Kegiatan} lewat {@code ambilKegiatans}, buat baru bila
	 * belum ada, catat {@link LogPembayaran} bila {@code amount > 0.1}, salin setiap
	 * {@link DokuRequestDetail} (berurutan {@code ke}, hanya {@code ke > 0}) menjadi
	 * {@link CicilanPembayaran} (insert bila belum ada, update bila sudah, dikunci lewat
	 * {@code ref = "dokuRequestDetail-" + id}), lalu hitung ulang total/denda cicilan dan
	 * mutakhirkan tunggakan serta cetak bukti pembayaran.</p>
	 *
	 * <p>Sesi Hibernate dibuka sendiri oleh method ini (bukan dari pemanggil) dan selalu
	 * dibersihkan (clear/disconnect/close) di blok {@code finally}, dengan setiap kegagalan
	 * penutupan dicatat ke {@link ais.common.ErrorAuditUtil} alih-alih dibiarkan menggagalkan
	 * respons ke gateway Doku.</p>
	 *
	 * @param dokuResponse notifikasi Doku yang sudah tersimpan, memuat {@code nama}
	 *                     ({@code TRANSIDMERCHANT}) dan {@code status} ({@code RESULT})
	 * @return {@code "Continue"} bila status BERHASIL dan seluruh langkah pembukuan pembayaran
	 *         dijalankan; {@code "Stop"} bila {@code nama} kosong, {@link DokuRequest} tidak
	 *         ditemukan/tertaut, atau status bukan BERHASIL
	 */
	@SuppressWarnings("unchecked")
	public static String prosesResponse(DokuResponse dokuResponse) {
		if (dokuResponse.getNama() == null || dokuResponse.getNama().trim().isEmpty()) {
			return "Stop";
		}
		String hasil = "Continue";
		Session session = null;
		try {
		session = HibernateUtil.getSessionFactory().openSession();
		DokuRequest dokuRequest = (DokuRequest) session.createCriteria(DokuRequest.class)
				.add(Restrictions.ilike("nama", dokuResponse.getNama(), MatchMode.EXACT)).setMaxResults(1)
				.uniqueResult();
		System.out.println("dokuRequest==>" + dokuRequest);
		if (dokuRequest != null && dokuRequest.getDokuResponse() == null) {
			dokuRequest.setDokuResponse(dokuResponse);
			session.getTransaction().begin();
			session.update(dokuRequest);
			session.getTransaction().commit();
		}
		if (dokuRequest != null && dokuResponse.getStatus().trim().equalsIgnoreCase(DokuResponse.BERHASIL)) {

			Double nilaiBiayaHarusDiBayars = dokuRequest.getNilaiBiayaHarusDiBayars();

			Kegiatan kegiatan = null;

			Mahasiswa mhs = dokuRequest.getMahasiswa();
			BiodataCalonMahasiswa bio = dokuRequest.getBiodataCalonMahasiswa();

			Integer semester = dokuRequest.getSemester();
			JadwalPembayaran jadwalPembayaran = dokuRequest.getJadwalPembayaran();
			JenisKegiatan jenisKegiatan = dokuRequest.getJenisKegiatan();
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
					+ dokuRequest.getPengurangan());

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
				kegiatan.setTahunAkademik(dokuRequest.getTahunAkademik());
				kegiatan.setTanggal(ais.ui.util.WaktuUtil.getDate());
				kegiatan.setValidated(1);
				kegiatan.setValidator("Doku");
				kegiatan.setPengurangan(dokuRequest.getPengurangan());
				kegiatan.setKeterangan(dokuRequest.getKeterangan());
				kegiatan.setAmount(nilaiBiayaHarusDiBayars);

				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, kegiatan);
				session.getTransaction().commit();
			}

			if (dokuRequest.getAmount() > 0.1) {
				LogPembayaran logPembayaran = new LogPembayaran();
				logPembayaran.setKegiatan(kegiatan);
				logPembayaran.setNominal(dokuRequest.getAmount());
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logPembayaran);
				session.getTransaction().commit();
			}

			List<DokuRequestDetail> dokuRequestDetails = session.createCriteria(DokuRequestDetail.class)
					.add(Restrictions.eq("dokuRequest", dokuRequest)).addOrder(Order.asc("ke"))
					.add(Restrictions.gt("ke", 0)).list();
			System.out.println("dokuRequestDetails==>" + dokuRequestDetails);
			if (!dokuRequestDetails.isEmpty()) {

				for (DokuRequestDetail dokuRequestDetail : dokuRequestDetails) {

					String ref = "dokuRequestDetail-" + dokuRequestDetail.getId();

					CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) session
							.createCriteria(CicilanPembayaran.class).add(Restrictions.eq("ref", ref)).setMaxResults(1)
							.uniqueResult();
					if (cicilanPembayaran == null) {
						cicilanPembayaran = new CicilanPembayaran(dokuRequestDetail.getDetailBiaya());

					}

					cicilanPembayaran.setRef(ref);
					cicilanPembayaran.setCicilanSebelumnya(dokuRequestDetail.getIdCicilan());
					cicilanPembayaran.setValidator("Doku");
					cicilanPembayaran.setKe(dokuRequestDetail.getKe());
					cicilanPembayaran.setKegiatan(kegiatan);
					cicilanPembayaran.setItemBiaya(dokuRequestDetail.getItemBiaya());
					cicilanPembayaran
							.setPengaturanPembayaranBulanan(dokuRequestDetail.getPengaturanPembayaranBulanan());
					cicilanPembayaran.setNilai(dokuRequestDetail.getNilai());
					cicilanPembayaran.setTanggal(dokuRequestDetail.getTanggal());
					cicilanPembayaran.setJenisPembayaran(ConstantValues.TUNAI);

					cicilanPembayaran.setDenda(dokuRequestDetail.getDenda());
					cicilanPembayaran.setNilaiAsli(dokuRequestDetail.getNilaiAsli());

					session.getTransaction().begin();
					if(cicilanPembayaran.getId()==null)session.save(cicilanPembayaran);else Common.refreshUpdate(session, cicilanPembayaran);
					session.getTransaction().commit();

					System.out.println("cicilanPembayaran==>" + cicilanPembayaran);
				}

				Double[] d = PembayaranUtil.getInstance().getTotalDanDendaFromCicilan(session, kegiatan);
				Double jumlah = d[0];
				Double denda = d[1];
				kegiatan.setDenda(denda.doubleValue());
				kegiatan.setAmountTerhutang(nilaiBiayaHarusDiBayars - (jumlah.doubleValue() - denda.doubleValue()));
				kegiatan.setAmount(jumlah.doubleValue());

				kegiatan.setValidator("Doku");

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
		} else {
			hasil = "Stop";
		}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:227");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:228");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:229");}
			}
		}
		return hasil;
	}

	/**
	 * Mencatat satu notifikasi Doku sebagai {@link DokuResponse} dan menindaklanjutinya, HANYA
	 * setelah checksum {@code WORDS} dan {@code AMOUNT} pada notifikasi tersebut lulus verifikasi
	 * terhadap {@link DokuRequest} yang cocok (lihat catatan keamanan pada Javadoc kelas).
	 *
	 * <p>Alur: (1) cari {@link DokuRequest} yang kolom {@code nama}-nya cocok PERSIS dengan
	 * {@code TRANSIDMERCHANT}; (2) validasi lewat {@link ais.common.DokuCommon#verifikasiChecksum(
	 * DokuRequest, String, String)} — bila gagal (termasuk bila {@link DokuRequest} tidak
	 * ditemukan), balas {@code "Stop"} tanpa menyimpan {@link DokuResponse} apa pun atau
	 * memanggil {@link #prosesResponse(DokuResponse)}; (3) bila lulus, simpan {@link DokuResponse}
	 * baru berisi {@code TRANSIDMERCHANT} dan {@code RESULT} seperti semula, lalu serahkan ke
	 * {@link #prosesResponse(DokuResponse)}.</p>
	 *
	 * @param param peta parameter hasil parsing manual body notifikasi Doku, memuat sekurangnya
	 *              {@code TRANSIDMERCHANT}, {@code RESULT}, {@code AMOUNT}, dan {@code WORDS}
	 * @return {@code "Continue"} atau {@code "Stop"} dari {@link #prosesResponse(DokuResponse)}
	 *         bila checksum valid; {@code "Stop"} langsung (tanpa efek samping) bila checksum
	 *         tidak valid
	 */
	public static String prosesTransaksi(Map<String, String> param) {
		String TRANSIDMERCHANT = param.get("TRANSIDMERCHANT");
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			DokuRequest dokuRequest = TRANSIDMERCHANT == null || TRANSIDMERCHANT.trim().isEmpty() ? null
					: (DokuRequest) session.createCriteria(DokuRequest.class)
							.add(Restrictions.eq("nama", TRANSIDMERCHANT.trim())).addOrder(Order.desc("id"))
							.setMaxResults(1).uniqueResult();

			if (!DokuCommon.verifikasiChecksum(dokuRequest, param.get("WORDS"), param.get("AMOUNT"))) {
				System.out.println(
						"==> DokuResponseServlet checksum TIDAK valid, notifikasi ditolak. TRANSIDMERCHANT="
								+ TRANSIDMERCHANT);
				return "Stop";
			}

			DokuResponse dokuResponse = new DokuResponse();
			dokuResponse.setNama(TRANSIDMERCHANT);
			dokuResponse.setStatus(param.get("RESULT"));
			session.getTransaction().begin();
			session.save(dokuResponse);
			session.getTransaction().commit();

			return prosesResponse(dokuResponse);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:250");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:251");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:252");}
			}
		}
	}

	/**
	 * Mem-parsing body request sebagai pasangan {@code key=value} dipisah {@code &} (bukan
	 * lewat {@link HttpServletRequest#getParameter}, karena Doku tidak selalu mengirim
	 * content-type form-encoded standar), lalu menyerahkan peta parameter tersebut ke
	 * {@link #prosesTransaksi(Map)} untuk verifikasi checksum dan pembukuan pembayaran.
	 *
	 * <p>Alur: (1) baca seluruh body sebagai teks mentah; (2) pecah per {@code &} lalu per
	 * {@code =} menjadi peta parameter, melewati diam-diam pasangan yang tidak berbentuk
	 * {@code key=value} lewat catch kosong yang tetap dicatat ke
	 * {@link ais.common.ErrorAuditUtil}; (3) panggil {@link #prosesTransaksi(Map)} dengan peta
	 * tersebut; (4) tulis hasilnya ({@code "Continue"}/{@code "Stop"}) sebagai {@code text/plain}
	 * ke {@code response}.</p>
	 *
	 * @param request  request HTTP masuk dari gateway Doku; body-nya dibaca utuh dan di-parsing
	 *                 manual sebagai parameter {@code TRANSIDMERCHANT}/{@code RESULT}/
	 *                 {@code AMOUNT}/{@code WORDS}
	 * @param response response HTTP keluar; diisi header {@code Content-Type: text/plain} dan
	 *                 badan berupa {@code "Continue"} atau {@code "Stop"} dari
	 *                 {@link #prosesTransaksi(Map)}
	 * @throws Exception bila pembacaan body, query Hibernate, atau penulisan respons gagal
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
		System.out.println("==> DokuResponseServlet data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DokuResponseServlet.java:276");

			}
		}

		System.out.println("==> param => " + param + ", " + request.getQueryString());

		String hasil = DokuResponseServlet.prosesTransaksi(param);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

}
