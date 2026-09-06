package ais.action.servlet;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranPenelitianDanPengabdian;

/**
 * Servlet penyaji berkas lampiran penelitian dan pengabdian
 * ({@link LampiranPenelitianDanPengabdian}) berdasarkan <i>primary key</i>-nya sendiri.
 *
 * <p>Terdaftar di {@code web.xml} dengan nama servlet {@code PenelitianDanPengabdian} pada
 * alamat {@code /AmbilPenelitianDanPengabdian}. Kerangkanya identik dengan
 * {@code AmbilPengumumanAkademis} dan saudara-saudaranya, dengan satu perbedaan: bila
 * berkas fisik ternyata tidak ada di disk, kelas ini membalas {@code HTTP 404} secara
 * eksplisit lewat {@code resp.sendError(...)} (lih. {@link #process}), bukan membiarkan
 * {@code FileNotFoundException} menembus ke penelan generik seperti kelas-kelas
 * bertetangga. Kelas ini <b>tidak memakai</b> {@code LampiranLain} maupun
 * {@code FileFotoLain.ambil(...)}: kueri langsung ke {@link LampiranPenelitianDanPengabdian}
 * lewat {@code Restrictions.idEq(id)}, sehingga parameter {@code usingId} milik temuan
 * {@code task_b82b25d2} tidak relevan di sini.</p>
 *
 * <h4>PERINGATAN KEAMANAN (DITAMBAL) &mdash; sebelumnya pengambilan berkas anonim berdasarkan
 * primary key tebakan</h4>
 * <p>Alamat servlet ini tidak punya aturan {@code intercept-url} sendiri di
 * {@code applicationContext-security.xml} sehingga TADINYA jatuh ke aturan payung
 * {@code /** = IS_AUTHENTICATED_ANONYMOUSLY}: siapa pun tanpa perlu masuk bisa memanggil
 * {@code ?id=1}, {@code ?id=2}, dst. secara berurutan. DITAMBAL: {@code
 * applicationContext-security.xml} kini punya aturan eksplisit
 * {@code /PenelitianDanPengabdian = IS_AUTHENTICATED_REMEMBERED}, DAN {@link #process} kini
 * mensyaratkan sesi login ({@code HttpSession.getAttribute("mytbmuser")}) sebelum menguery
 * baris. Entitas {@link LampiranPenelitianDanPengabdian} melekat pada
 * {@code PenelitianDanPengabdian} (baris "skema" penelitian/pengabdian institusional, TIDAK
 * punya field pengaju/pemilik perorangan &mdash; lih.
 * {@code ais.database.model.penelitiandanpengabdian.PenelitianDanPengabdian}), sehingga gerbang
 * berhenti di syarat login saja tanpa pemeriksaan kepemilikan per-baris (berbeda dari
 * {@code AmbilFilePengajuanPengajuanPenelitianDanPengabdian}/
 * {@code AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian} yang lampirannya memang
 * milik pengaju perorangan). Investigasi juga tidak menemukan pemanggil URL ini di UI mana
 * pun (kode mati/legacy) &mdash; lih. Javadoc
 * {@code AmbilFilePengajuanPengajuanPenelitianDanPengabdian} untuk daftar lengkap kelas
 * bertetangga dengan pola serupa (SEMUA sudah ditambal).</p>
 *
 * @see LampiranPenelitianDanPengabdian
 */
public class AmbilPenelitianDanPengabdian extends HttpServlet {
	/**
	 * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
	 * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan kontainer servlet. Tidak melakukan
	 * inisialisasi apa pun.
	 *
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilPenelitianDanPengabdian() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process} dan menelan
	 * setiap pengecualiannya lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  permintaan masuk, memuat parameter {@code id}
	 * @param response balasan yang akan diisi bita berkas lampiran, status 404 bila berkas
	 *                 tidak ada di disk, atau status 500 bila {@code id} kosong
	 * @throws ServletException tidak pernah dilempar keluar &mdash; ditelan di dalam
	 * @throws IOException      tidak pernah dilempar keluar, dengan alasan yang sama
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
	 * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
	 *
	 * @param request  permintaan masuk, memuat parameter {@code id}
	 * @param response balasan yang akan diisi bita berkas lampiran, status 404 bila berkas
	 *                 tidak ada di disk, atau status 500 bila {@code id} kosong
	 * @throws ServletException tidak pernah dilempar keluar &mdash; lih. {@link #doGet}
	 * @throws IOException      tidak pernah dilempar keluar, dengan alasan yang sama
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
	 * Mencari baris {@link LampiranPenelitianDanPengabdian} ber-{@code id} yang diminta dan
	 * menyalin isi berkasnya ke {@code resp} sebagai unduhan.
	 *
	 * <p>Bila {@code id} kosong, balasan diisi {@code HTTP 500} dan dicatat lewat
	 * {@code ServletContext#log}; bila {@code id} bukan angka, {@code Long.parseLong}
	 * melempar {@code NumberFormatException} yang merambat ke pemanggil ({@link #doGet}/
	 * {@link #doPost}), yang menelannya. Tipe MIME ditentukan dari ekstensi nama berkas
	 * ({@code getNama()}) lewat {@code ServletContext#getMimeType}, dengan cadangan
	 * png/jpg/gif berbasis akhiran nama dan {@code image/jpg} sebagai nilai baku terakhir
	 * bila semuanya gagal dikenali.</p>
	 *
	 * <p>Berbeda dari kelas-kelas bertetangga: berkas fisik ({@code ambilFile()}) diperiksa
	 * lebih dulu dengan {@code exists()} sebelum dibuka. Bila baris ditemukan di database
	 * tetapi berkasnya sudah tidak ada di disk (mis. terhapus manual, atau baris yatim
	 * peninggalan migrasi), balasan diisi {@code HTTP 404} lewat
	 * {@code resp.sendError(SC_NOT_FOUND, ...)} dan metode kembali lebih awal &mdash;
	 * pengecualian dari upaya {@code sendError} itu sendiri ditelan dan dicatat ke audit
   	 * error, bukan dilempar. Ini mencegah {@code FileNotFoundException} generik menembus
	 * ke penelan {@link Common#tampilErrorJikaAdmin(Exception)} seperti pada kelas-kelas
	 * bertetangga yang tidak melakukan pengecekan ini. Sesi Hibernate dibersihkan
	 * (clear/disconnect/close) di blok {@code finally} tanpa bergantung pada hasil query.</p>
	 *
	 * <p>Tidak ada pemeriksaan kepemilikan maupun otentikasi tambahan di sini di luar
	 * yang sudah diberlakukan Spring Security pada tingkat URL (lih. Javadoc kelas).</p>
	 *
	 * @param request permintaan masuk; parameter {@code id} dibaca sebagai primary key
	 *                {@link LampiranPenelitianDanPengabdian}
	 * @param resp    balasan; diisi status 500 bila {@code id} kosong, status 404 bila
	 *                baris ditemukan tapi berkasnya tidak ada di disk, atau header
	 *                {@code Content-Type}/{@code Content-Disposition} dan bita berkas bila
	 *                keduanya ditemukan (tidak diubah sama sekali bila baris tidak
	 *                ditemukan)
	 * @throws Exception meneruskan apa adanya setiap pengecualian dari penguraian
	 *                    {@code id} atau dari akses Hibernate/berkas
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		String id = request.getParameter("id");
		ServletContext sc = getServletContext();
		if (id == null || id.trim().equals("")) {
			sc.log("id harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		HttpSession httpSession = request.getSession(false);
		Tbmuser tbmuserLogin = httpSession == null ? null : (Tbmuser) httpSession.getAttribute("mytbmuser");
		if (tbmuserLogin == null) {
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Harus login");
			return;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			LampiranPenelitianDanPengabdian fileFoto = (LampiranPenelitianDanPengabdian) session
					.createCriteria(LampiranPenelitianDanPengabdian.class).add(Restrictions.idEq(Long.parseLong(id)))
					.setMaxResults(1).uniqueResult();

			if (fileFoto != null) {
				String mimeType = sc.getMimeType(fileFoto.getNama());
				if (mimeType == null) {
					if (fileFoto.getNama().toLowerCase().endsWith("png")) {
						mimeType = "image/png";
					} else if (fileFoto.getNama().toLowerCase().endsWith("jpg")) {
						mimeType = "image/jpg";
					} else if (fileFoto.getNama().toLowerCase().endsWith("gif")) {
						mimeType = "image/gif";
					} else {
						mimeType = "image/jpg";
					}
					// sc.log("Could not get MIME type of " + filename);
					// resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					// return;
				}

				// Set content type
				resp.setContentType(mimeType);
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", fileFoto.getNama());
				resp.setHeader(headerKey, headerValue);

				java.io.File berkasReal = fileFoto.ambilFile();
				if (berkasReal == null || !berkasReal.exists()) {
					// Berkas lampiran tidak ada di disk → balas 404 ramah, jangan lempar FileNotFoundException.
					try {
						resp.sendError(javax.servlet.http.HttpServletResponse.SC_NOT_FOUND, "Berkas tidak ditemukan");
					} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/AmbilPenelitianDanPengabdian.java:106");
					}
					return;
				}
				ServletOutputStream out = resp.getOutputStream();
				FileInputStream in = new FileInputStream(berkasReal);
				int length = (int) in.available();
				// int length = (int) photo.length();

				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];

				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}

				in.close();
				out.flush();
			}

		} catch (Exception e) {

			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilPenelitianDanPengabdian.java:131");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilPenelitianDanPengabdian.java:132");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilPenelitianDanPengabdian.java:133");}
			}
		}

	}

}
