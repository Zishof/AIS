package ais.action.servlet;

import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.penelitiandanpengabdian.FilePengajuanTahapanPelaporanPenelitianDanPengabdian;

/**
 * Servlet penyaji berkas lampiran tahapan pelaporan penelitian dan pengabdian
 * ({@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}) berdasarkan
 * <i>primary key</i>-nya sendiri.
 *
 * <p>Terdaftar di {@code web.xml} dengan alamat
 * {@code /FilePengajuanTahapanPelaporanPenelitianDanPengabdian}. Susunan kelas ini
 * identik baris-demi-baris dengan
 * {@code AmbilFilePengajuanPengajuanPenelitianDanPengabdian}, hanya kelas entitas
 * sasarannya yang berbeda &mdash; lampiran laporan tahapan, bukan lampiran pengajuan
 * awal. Sama seperti saudaranya, kelas ini <b>tidak memakai</b> {@code LampiranLain}
 * maupun {@code FileFotoLain.ambil(...)}: {@link #process} langsung mengueri
 * {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian} lewat
 * {@code Restrictions.idEq(id)} pada parameter {@code id} apa adanya, sehingga
 * parameter {@code usingId} milik temuan {@code task_b82b25d2} tidak relevan di sini.</p>
 *
 * <h4>PERINGATAN KEAMANAN &mdash; pengambilan berkas anonim berdasarkan primary key tebakan</h4>
 * <p>Alamat servlet ini tidak punya aturan {@code intercept-url} sendiri di
 * {@code applicationContext-security.xml} sehingga jatuh ke aturan payung
 * {@code /** = IS_AUTHENTICATED_ANONYMOUSLY}: siapa pun tanpa perlu masuk dapat memanggil
 * {@code ?id=1}, {@code ?id=2}, dst. secara berurutan tanpa pernah diminta membuktikan
 * kepemilikan atas laporan tahapan yang bersangkutan. Pola ini sama persis dengan enam
 * kelas {@code Ambil*}/{@code AmbilFile*} bertetangga di paket ini (lih. Javadoc
 * {@code AmbilFilePengajuanPengajuanPenelitianDanPengabdian} untuk daftar lengkapnya).</p>
 *
 * @see FilePengajuanTahapanPelaporanPenelitianDanPengabdian
 */
public class AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian extends HttpServlet {
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
	public AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan ke {@link #process} dan menelan
	 * setiap pengecualiannya lewat {@link Common#tampilErrorJikaAdmin(Exception)}.
	 *
	 * @param request  permintaan masuk, memuat parameter {@code id}
	 * @param response balasan yang akan diisi bita berkas lampiran bila ditemukan
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
	 * @param response balasan yang akan diisi bita berkas lampiran bila ditemukan
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
	 * Mencari baris {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}
	 * ber-{@code id} yang diminta dan menyalin isi berkasnya ke {@code resp} sebagai
	 * unduhan.
	 *
	 * <p>Bila {@code id} kosong, balasan diisi {@code HTTP 500} dan dicatat lewat
	 * {@code ServletContext#log}; bila {@code id} bukan angka, {@code Long.parseLong}
	 * melempar {@code NumberFormatException} yang merambat ke pemanggil ({@link #doGet}/
	 * {@link #doPost}), yang menelannya. Tipe MIME ditentukan dari ekstensi nama berkas
	 * ({@code getNama()}) lewat {@code ServletContext#getMimeType}, dengan cadangan
	 * png/jpg/gif berbasis akhiran nama dan {@code image/jpg} sebagai nilai baku terakhir
	 * bila semuanya gagal dikenali. Sesi Hibernate dibersihkan (clear/disconnect/close)
	 * di blok {@code finally} tanpa bergantung pada hasil query.</p>
	 *
	 * <p>Tidak ada pemeriksaan kepemilikan maupun otentikasi tambahan di sini di luar
	 * yang sudah diberlakukan Spring Security pada tingkat URL (lih. Javadoc kelas).</p>
	 *
	 * @param request permintaan masuk; parameter {@code id} dibaca sebagai primary key
	 *                {@link FilePengajuanTahapanPelaporanPenelitianDanPengabdian}
	 * @param resp    balasan; diisi status 500 bila {@code id} kosong, atau header
	 *                {@code Content-Type}/{@code Content-Disposition} dan bita berkas bila
	 *                baris ditemukan (tidak diubah sama sekali bila baris tidak ditemukan)
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

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			FilePengajuanTahapanPelaporanPenelitianDanPengabdian fileFoto = (FilePengajuanTahapanPelaporanPenelitianDanPengabdian) session
					.createCriteria(FilePengajuanTahapanPelaporanPenelitianDanPengabdian.class)
					.add(Restrictions.idEq(Long.parseLong(id))).setMaxResults(1).uniqueResult();

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

				ServletOutputStream out = resp.getOutputStream();
				FileInputStream in = new FileInputStream(fileFoto.getPath());
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
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian.java:122");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian.java:123");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFilePengajuanTahapanPelaporanPenelitianDanPengabdian.java:124");}
			}
		}

	}

}
