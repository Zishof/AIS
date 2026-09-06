package ais.action.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.doku.DokuRequest;

/**
 * Servlet verifikasi (pre-check) untuk alur pembayaran Doku, dipetakan pada {@code web.xml}
 * sebagai endpoint yang dipanggil oleh gateway Doku SEBELUM transaksi diselesaikan, untuk
 * memastikan merchant (aplikasi AIS ini) mengenali transaksi yang sedang diproses.
 *
 * <p>Doku mengirim body berformat {@code key=value} dipisah {@code &} (bukan JSON maupun
 * form-encoded standar) berisi antara lain {@code AMOUNT} (nominal transaksi) dan
 * {@code WORDS} (pada protokol Doku umumnya berupa checksum/hash transaksi, BUKAN nilai
 * transaksi itu sendiri). {@link #process} mem-parsing body tersebut secara manual lalu
 * mencocokkan {@code WORDS} langsung sebagai nilai kolom {@code trxId} pada
 * {@link DokuRequest} — TIDAK ada verifikasi checksum/tanda tangan kriptografis atas
 * {@code WORDS} terhadap {@code AMOUNT} dan data transaksi lain. Jika ditemukan
 * {@link DokuRequest} dengan {@code trxId} yang cocok persis dan nilai {@code amount}-nya
 * sama dengan {@code AMOUNT} yang dikirim, servlet membalas {@code "Continue"}; selain itu
 * membalas {@code "Stop"}.</p>
 *
 * <p><b>Keamanan:</b> karena tidak ada validasi asal request (mis. IP allowlist milik Doku)
 * maupun verifikasi checksum {@code WORDS}, endpoint ini dapat dipanggil oleh pihak mana pun
 * yang mengetahui atau menebak nilai {@code trxId} transaksi yang sudah ada beserta
 * amount-nya. Karena servlet ini HANYA melakukan query baca (tidak menulis status pembayaran
 * apa pun), dampaknya terbatas pada tahap pre-check; keputusan akhir tetap ditentukan oleh
 * {@code DokuResponseServlet} pada notifikasi berikutnya. Namun {@code DokuResponseServlet}
 * juga TIDAK memverifikasi checksum Doku — lihat javadoc di sana untuk rincian celah yang
 * lebih berdampak (finalisasi pembayaran/pendaftaran mahasiswa).</p>
 */
public class DokuVerifyServlet extends HttpServlet {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini. */
	private static final long serialVersionUID = 1L;

	// private static PembayaranUtil pembayaranUtil =
	// PembayaranUtil.getInstance();

	/**
	 * Konstruktor default tanpa argumen, hanya meneruskan ke {@link HttpServlet#HttpServlet()}.
	 * Tidak ada state khusus yang diinisialisasi di sini.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public DokuVerifyServlet() {
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
	 * Mem-parsing body request sebagai pasangan {@code key=value} dipisah {@code &} (bukan
	 * lewat {@link HttpServletRequest#getParameter}, karena Doku tidak selalu mengirim
	 * content-type form-encoded standar), lalu memverifikasi keberadaan transaksi Doku yang
	 * cocok di database sebelum membalas status ke pemanggil.
	 *
	 * <p>Alur: (1) baca seluruh body sebagai teks mentah; (2) pecah per {@code &} lalu per
	 * {@code =} menjadi peta parameter, melewati diam-diam pasangan yang tidak berbentuk
	 * {@code key=value} (mis. tanpa tanda sama dengan) lewat catch kosong yang tetap dicatat
	 * ke {@link ais.common.ErrorAuditUtil}; (3) ambil {@code AMOUNT} dan parse sebagai angka;
	 * (4) buka sesi Hibernate baru dan cari {@link DokuRequest} yang kolom {@code trxId}-nya
	 * cocok PERSIS ({@link MatchMode#EXACT}) dengan parameter {@code WORDS}; (5) bila
	 * ditemukan dan {@code amount} tersimpan sama dengan {@code AMOUNT} yang dikirim, hasil
	 * diisi {@code "Continue"}, selain itu tetap {@code "Stop"}; (6) sesi selalu dibersihkan
	 * (clear/disconnect/close) di blok {@code finally} sebelum method kembali; (7) hasil
	 * ditulis sebagai {@code text/plain} ke {@code response}.</p>
	 *
	 * <p>Lihat catatan keamanan pada Javadoc kelas mengenai tidak adanya verifikasi checksum
	 * kriptografis atas {@code WORDS}.</p>
	 *
	 * @param request  request HTTP masuk; body-nya dibaca utuh dan di-parsing manual sebagai
	 *                 parameter {@code AMOUNT}/{@code WORDS}
	 * @param response response HTTP keluar; diisi header {@code Content-Type: text/plain} dan
	 *                 badan berupa {@code "Continue"} atau {@code "Stop"}
	 * @throws Exception bila parsing {@code AMOUNT} gagal (bukan angka atau tidak ada), bila
	 *                    query Hibernate gagal, atau bila penulisan respons gagal
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
		System.out.println("==> DokuVerifyServlet data => " + data);

		String[] splt = StringUtils.split(data, "&");
		Map<String, String> param = new HashMap<String, String>();
		for (String s : splt) {
			try {
				String[] v = StringUtils.split(s, "=");
				param.put(v[0], v[1]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:87");

			}
		}

		System.out.println("==> param => " + param + ", " + request.getQueryString());

		int AMOUNT = (int) Double.parseDouble(param.get("AMOUNT").trim());

		String hasil = "Stop";
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Double amount = (Double) session.createCriteria(DokuRequest.class).setProjection(Projections.property("amount"))
					.add(Restrictions.ilike("trxId", param.get("WORDS"), MatchMode.EXACT)).setMaxResults(1).uniqueResult();
			if (amount != null && AMOUNT == amount.intValue()) {
				hasil = "Continue";
			}
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:107");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:108");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/DokuVerifyServlet.java:109");}
			}
		}

		System.out.println("==> hasil => " + hasil);

		response.setHeader("Content-Type", "text/plain");

		PrintWriter writer = response.getWriter();
		writer.write(hasil);
	}

}
