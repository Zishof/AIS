package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoBiodataMahasiswa;
import ais.database.model.file.FotoMahasiswa;

/**
 * Servlet yang menyajikan foto profil mahasiswa berdasarkan NIM yang dikirim
 * lewat parameter request {@code nim}.
 * <p>
 * Alur kerja {@link #process(HttpServletRequest, HttpServletResponse)}:
 * mencari {@link Mahasiswa} aktif (atau yang kolom {@code aktif}-nya null,
 * diperlakukan sebagai aktif) dengan NIM tersebut, lalu mencari baris
 * {@link BiodataMahasiswa} terbarunya. Foto diprioritaskan dari
 * {@link FotoBiodataMahasiswa} yang ditandai {@code fotoUtama = true} milik
 * biodata itu; bila tidak ada, jatuh ke foto terbaru pada
 * {@link FotoMahasiswa} milik mahasiswa yang sama. Bila mahasiswa tidak
 * ditemukan, tidak aktif, atau tidak ada foto yang berkasnya bisa dibaca,
 * servlet menyajikan salah satu gambar default lewat
 * {@link #kirimFotoDefaultAtau404(ServletContext, HttpServletResponse)}.
 * </p>
 * <p>
 * <b>Catatan keamanan:</b> tidak ada gerbang otentikasi/otorisasi apa pun;
 * siapa pun yang mengetahui/menebak NIM mahasiswa dapat mengunduh foto profil
 * mahasiswa tersebut tanpa login. Pola yang sama dengan servlet
 * {@code AmbilFotoDosen} dan servlet {@code Ambil*} lain di paket ini.
 * </p>
 */
public class AmbilFotoMahasiswa extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilFotoMahasiswa() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}. Exception apa pun
	 * yang dilempar oleh {@code process} ditangkap di sini dan diteruskan ke
	 * {@link Common#tampilErrorJikaAdmin(Exception)} sehingga rincian error hanya
	 * ditampilkan bila pengguna yang sedang login adalah administrator.
	 *
	 * @param request permintaan HTTP; parameter {@code nim} berisi NIM mahasiswa yang fotonya diminta
	 * @param response respons HTTP; isi foto (atau gambar default) ditulis ke output stream-nya
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
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
	 * Menangani permintaan HTTP POST dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, dengan perilaku
	 * dan penanganan error yang identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; parameter {@code nim} berisi NIM mahasiswa yang fotonya diminta
	 * @param response respons HTTP; isi foto (atau gambar default) ditulis ke output stream-nya
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
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
	 * Mencari foto profil mahasiswa berdasarkan NIM dan menuliskannya ke response.
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Membaca parameter {@code nim}; bila kosong, membalas status 500 dan berhenti.</li>
	 *   <li>Mencari {@link Mahasiswa} dengan NIM tersebut yang berstatus aktif (atau
	 *       kolom {@code aktif} bernilai null, yang diperlakukan sama seperti aktif).
	 *       Bila tidak ditemukan, jatuh ke {@link #kirimFotoDefaultAtau404(ServletContext, HttpServletResponse)}.</li>
	 *   <li>Mencari {@link BiodataMahasiswa} terbaru (id terbesar) milik mahasiswa tersebut.</li>
	 *   <li>Mencari {@link FotoBiodataMahasiswa} milik biodata itu yang ditandai
	 *       {@code fotoUtama = true}; bila tidak ada, jatuh ke foto terbaru pada
	 *       {@link FotoMahasiswa} milik mahasiswa yang sama.</li>
	 *   <li>Membuka berkas fisik foto lewat {@link FileFoto#ambilFile()}; bila gagal
	 *       atau berkas tidak ada di disk, jatuh ke gambar default.</li>
	 *   <li>Menuliskan isi berkas ke response sebagai {@code image/png}, dengan
	 *       header {@code Content-Disposition} berisi nama asli berkas.</li>
	 * </ol>
	 * Dua sesi Hibernate dibuka (satu biasa untuk {@link Mahasiswa}/{@link BiodataMahasiswa},
	 * satu {@link StreamingHibernateUtil} untuk entitas foto) dan keduanya selalu
	 * ditutup di blok {@code finally}.
	 * </p>
	 *
	 * @param request permintaan HTTP; parameter {@code nim} wajib berisi NIM mahasiswa
	 * @param resp respons HTTP tujuan penulisan isi foto/gambar default
	 * @throws Exception diteruskan ke pemanggil ({@link #doGet}/{@link #doPost}) yang menanganinya lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) throws Exception {

		String nim = request.getParameter("nim");
		ServletContext sc = getServletContext();
		if (nim == null || nim.trim().equals("")) {
			sc.log("nim harus diisi !");
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		Session session = null;
		Session streamingSession = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Mahasiswa mahasiswa = (Mahasiswa) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("nim", nim == null ? null : nim.trim())).setMaxResults(1).uniqueResult();
			if (mahasiswa == null || mahasiswa.getId() == null) {
				kirimFotoDefaultAtau404(sc, resp);
				return;
			}

			streamingSession = StreamingHibernateUtil.getInstance().openSession();

			BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
					.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1)
					.uniqueResult();

			FileFoto fileFoto = (FotoBiodataMahasiswa) streamingSession.createCriteria(FotoBiodataMahasiswa.class)
					.add(Restrictions.eq("biodataMahasiswa",
							biodataMahasiswa == null ? null : biodataMahasiswa.getId()))
					.add(Restrictions.eq("fotoUtama", true)).setMaxResults(1).uniqueResult();

			// Blob blob = fotoBiodataMahasiswa == null ? null
			// : fotoBiodataMahasiswa.getFoto();

			if (fileFoto == null) {
				fileFoto = (FotoMahasiswa) streamingSession.createCriteria(FotoMahasiswa.class)
						.addOrder(Order.desc("id")).add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
						.setMaxResults(1).uniqueResult();

			}

			File file = null;
			if (fileFoto != null) {
				try {
					file = fileFoto.ambilFile();
				} catch (Exception eFile) {
					ais.common.ErrorAuditUtil.record(eFile,
							"AmbilFotoMahasiswa: file foto mahasiswa tidak dapat diambil, nim=" + nim);
				}
			}
			if (file == null || !file.exists() || !file.isFile()) {
				kirimFotoDefaultAtau404(sc, resp);
				return;
			}

			if (fileFoto != null) {
				String headerKey = "Content-Disposition";
				String headerValue = String.format("attachment; filename=\"%s\"", fileFoto.getNama());
				resp.setHeader(headerKey, headerValue);
			}

			resp.setContentType("image/png");

			ServletOutputStream out = resp.getOutputStream();
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				int length;
				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];
	
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
			} finally {
				if (in != null) {
					try { in.close(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "AmbilFotoMahasiswa.closeInput"); }
				}
			}
			out.flush();

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (streamingSession != null) {
				try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:131");}
				try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:132");}
				try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:133");}
			}
			if (session != null) {
				try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:136");}
				try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:137");}
				try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilFotoMahasiswa.java:138");}
			}
		}

	}

	/**
	 * Menyajikan gambar avatar default (lewat {@link #fileDefault(ServletContext)})
	 * sebagai fallback ketika mahasiswa/foto yang diminta tidak ditemukan atau tidak
	 * bisa dibaca. Bila tidak satu pun kandidat berkas default tersedia di disk,
	 * membalas status {@link HttpServletResponse#SC_NOT_FOUND 404}.
	 *
	 * @param sc konteks servlet, dipakai untuk resolusi path fisik kandidat gambar default
	 * @param resp respons HTTP tujuan penulisan gambar default atau status 404
	 * @throws IOException bila penulisan ke output stream response gagal
	 */
	private void kirimFotoDefaultAtau404(ServletContext sc, HttpServletResponse resp) throws IOException {
		File file = fileDefault(sc);
		if (file == null || !file.exists() || !file.isFile()) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		resp.setContentType("image/png");
		ServletOutputStream out = resp.getOutputStream();
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			int length;
			byte[] buffer = new byte[1024];
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
		} finally {
			if (in != null) {
				try { in.close(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "AmbilFotoMahasiswa.closeDefault"); }
			}
		}
		out.flush();
	}

	/**
	 * Mencari kandidat berkas gambar avatar default pertama yang benar-benar ada
	 * di disk, dengan urutan prioritas: {@code /img/user_default.png},
	 * {@code /img/user_male.png}, {@code /img/USER.png}, lalu
	 * {@code /component/adminlte/assets/img/avatar.png}.
	 *
	 * @param sc konteks servlet, dipakai untuk resolusi path fisik lewat {@link ServletContext#getRealPath(String)}
	 * @return berkas kandidat pertama yang ditemukan di disk, atau {@code null} bila tidak satu pun ada
	 */
	private File fileDefault(ServletContext sc) {
		String[] daftar = new String[] { "/img/user_default.png", "/img/user_male.png", "/img/USER.png",
				"/component/adminlte/assets/img/avatar.png" };
		for (int i = 0; i < daftar.length; i++) {
			String realPath = sc.getRealPath(daftar[i]);
			if (realPath == null) {
				continue;
			}
			File file = new File(realPath);
			if (file.exists() && file.isFile()) {
				return file;
			}
		}
		return null;
	}

}
