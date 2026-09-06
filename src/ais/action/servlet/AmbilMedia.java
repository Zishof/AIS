package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.sql.Blob;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;

/**
 * Servlet generik "satu untuk semua" bagi sebagian besar kebutuhan penyajian
 * media/lampiran di AIS. Klien mengirim nama kelas entitas ({@code clazz}),
 * nama properti kolom yang dipakai untuk mencari baris ({@code property}) dan
 * nilainya ({@code id}), serta nama properti kolom yang berisi data biner
 * ({@code foto}) dan properti yang berisi nama tampilan ({@code name}).
 * Parameter boleh dikirim sebagai parameter request biasa, ATAU digabung
 * dalam satu JSON terenkripsi lewat parameter {@code d} (didekripsi lewat
 * {@link Common#desEncrypter}); nilai dari JSON diprioritaskan bila ada.
 * Parameter {@code file} (di dalam JSON) bahkan memungkinkan servlet
 * menyajikan path berkas fisik APA ADANYA langsung dari disk tanpa lewat
 * database sama sekali, asal berkas itu ada.
 * <p>
 * Bila {@code clazz} adalah entitas yang mengimplementasikan
 * {@link FileFotoLain} (mis. {@link ais.database.model.file.FotoAdmin}) dan
 * properti pencarian bertipe {@code Long}, pengambilan didelegasikan ke
 * {@link FileFotoLain#ambil(boolean, Object, String, Class)} dengan
 * {@code usingId} yang NILAINYA DIAMBIL LANGSUNG DARI PARAMETER REQUEST
 * {@code usingId} milik klien (default {@code false}) -- bila klien mengirim
 * {@code usingId=true}, berlaku kerentanan yang sama seperti yang telah
 * dikonfirmasi pada servlet lampiran lain di paket ini (filter kolom
 * {@code jenis} diabaikan sepenuhnya). Bila entitas TIDAK termasuk kategori
 * ini (jalur generik, dipakai untuk hampir semua jenis media lain), baris
 * dicari lewat proyeksi Hibernate {@code SELECT id, <name>, gdrive FROM
 * <clazz> WHERE <property> = <id>} (dengan filter tambahan opsional
 * {@code fotoId}/{@code fotoUtama}), lalu kolom {@code foto} dibaca lewat
 * query terpisah {@code SELECT <foto> FROM <clazz> WHERE id = <id-baris>}.
 * Hasil bisa berupa redirect ke GDrive/Dropbox, atau berkas lokal (dengan
 * opsi resize lewat {@code height}/{@code width}).
 * </p>
 * <p>
 * <b>Catatan keamanan (permukaan lebih luas dari servlet {@code Ambil*} lain):</b>
 * pada jalur generik, {@code clazz}/{@code property}/{@code foto}/{@code name}
 * adalah nama KELAS dan NAMA KOLOM Hibernate apa pun yang dikirim mentah oleh
 * klien, dibatasi hanya sejauh kelas tersebut memiliki
 * {@link org.hibernate.metadata.ClassMetadata} dan nama properti yang diminta
 * valid pada kelas itu -- servlet ini pada dasarnya berfungsi sebagai ORAKEL
 * PEMBACAAN KOLOM BINER GENERIK: siapa pun yang mengetahui/menebak kombinasi
 * {@code clazz}+{@code property}+{@code foto}+{@code id} yang valid dapat
 * membaca kolom biner ENTITAS Hibernate APA PUN, TIDAK terbatas pada foto
 * profil seperti servlet {@code Ambil*} lain, dan TIDAK memerlukan entitas
 * tersebut berhubungan dengan {@link FileFotoLain}/{@code LampiranLain} sama
 * sekali. Tidak ada gerbang otentikasi/otorisasi apa pun pada servlet ini.
 * </p>
 */
public class AmbilMedia extends HttpServlet {
	/** ID versi serialisasi tetap untuk kontrak {@link java.io.Serializable} milik {@link HttpServlet}. */
	private static final long serialVersionUID = 1L;

	/**
	 * Membuat instance servlet. Tidak ada inisialisasi khusus di luar konstruktor
	 * bawaan {@link HttpServlet#HttpServlet()}.
	 */
	public AmbilMedia() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * Menangani permintaan HTTP GET dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; lih. parameter generik {@code clazz}/{@code property}/{@code id}/dst. di Javadoc kelas
	 * @param response respons HTTP; isi media (atau redirect/ikon default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doGet}, tidak pernah dilempar keluar method ini
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menangani permintaan HTTP POST dengan mendelegasikan sepenuhnya ke
	 * {@link #process(HttpServletRequest, HttpServletResponse)}, dengan perilaku
	 * yang identik dengan {@link #doGet(HttpServletRequest, HttpServletResponse)}.
	 *
	 * @param request permintaan HTTP; lih. parameter generik {@code clazz}/{@code property}/{@code id}/dst. di Javadoc kelas
	 * @param response respons HTTP; isi media (atau redirect/ikon default) ditulis ke sini
	 * @throws ServletException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 * @throws IOException dideklarasikan oleh kontrak {@link HttpServlet#doPost}, tidak pernah dilempar keluar method ini
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		process(request, response);
	}

	/**
	 * Menentukan berkas yang akan disajikan lewat
	 * {@link #loadFile(HttpServletRequest, HttpServletResponse)}, lalu menyalin
	 * isinya ke response dengan {@code Content-Type} yang dideteksi lewat
	 * {@link CommonMedia#getMime(File)}.
	 * <p>
	 * Bila {@link #loadFile} mengembalikan {@code null} (berarti response sudah
	 * di-redirect, mis. lampiran tersimpan di GDrive/Dropbox, atau parameter
	 * {@code clazz}/{@code id} tidak valid dan sudah dibalas dengan status 400),
	 * method berhenti tanpa menulis apa pun lagi ke response. Bila berkas yang
	 * dikembalikan sudah tidak ada di disk atau kosong, membalas
	 * {@link HttpServletResponse#SC_NOT_FOUND 404} yang ramah. Exception lain
	 * yang terjadi ditelan dan dicatat lewat
	 * {@link ais.common.ErrorAuditUtil#record} tanpa mengubah status response.
	 * </p>
	 *
	 * @param request permintaan HTTP; diteruskan apa adanya ke {@link #loadFile(HttpServletRequest, HttpServletResponse)}
	 * @param resp respons HTTP tujuan penulisan isi berkas
	 */
	private void process(HttpServletRequest request, HttpServletResponse resp) {

		try {
			// Set content size
			File file = loadFile(request, resp);

			// loadFile() mengembalikan null bila response sudah di-redirect (mis. berkas
			// tersimpan di GDrive/Dropbox) -> tidak ada lagi yang perlu ditulis ke stream.
			if (file == null) {
				return;
			}

			// Berkas ditemukan di DB tapi file fisiknya sudah hilang dari disk (terhapus
			// manual/migrasi server) -> balas 404 ramah, jangan lempar FileNotFoundException
			// mentah saat FileInputStream dibuka di bawah. Pola sama seperti Document.java /
			// AmbilPenelitianDanPengabdian.java.
			if (!file.exists() || file.length() <= 0L) {
				try {
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File tidak ditemukan.");
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:notfound"); }
				return;
			}

			String mimeType = CommonMedia.getMime(file);
			resp.setContentType(mimeType);
			resp.setHeader("Content-Disposition", "filename=\"" + file.getName() + "\"");
			resp.setContentLength((int) file.length());
			InputStream in = new FileInputStream(file);
			ServletOutputStream out = resp.getOutputStream();

			int length = (int) in.available();

			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			try {
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:91");

			}

			// in.close();
			IOUtils.closeQuietly(in);
			out.flush();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:98");
//			Common.tampilErrorJikaAdmin(e);
		}

	}

	/**
	 * Menentukan berkas/redirect yang harus disajikan berdasarkan kombinasi
	 * parameter generik {@code clazz}, {@code property}, {@code id}, {@code name},
	 * {@code foto}, dan beberapa parameter opsional lain -- baik dikirim langsung
	 * sebagai parameter request, maupun digabung dalam JSON terenkripsi pada
	 * parameter {@code d}.
	 * <p>
	 * Langkah kerja:
	 * <ol>
	 *   <li>Mendekripsi &amp; mem-parse parameter {@code d} (bila ada) menjadi
	 *       {@link org.json.JSONObject}; setiap parameter lain di bawah lebih
	 *       dulu dicari di JSON ini sebelum jatuh ke parameter request biasa.</li>
	 *   <li>Bila JSON memuat {@code file} dan berkas pada path itu ada di disk,
	 *       LANGSUNG mengembalikan berkas itu tanpa menyentuh database sama
	 *       sekali.</li>
	 *   <li>Menentukan gambar ikon default berdasarkan {@code clazz} (ikon
	 *       "administrator" untuk kelas-kelas foto identitas yang dikenal, ikon
	 *       "book" untuk {@link ais.database.model.file.FotoGambarItem}, atau
	 *       ikon administrator generik untuk kelas lain).</li>
	 *   <li>Memvalidasi {@code clazz} dan {@code id} tidak kosong; bila salah
	 *       satu kosong, membalas {@link HttpServletResponse#SC_BAD_REQUEST 400}
	 *       dan mengembalikan {@code null}.</li>
	 *   <li>Memuat {@code clazz} lewat {@link Class#forName(String)} dan
	 *       menentukan tipe properti {@code property} lewat
	 *       {@link org.hibernate.metadata.ClassMetadata} kelas itu, lalu
	 *       meng-konversi {@code id} ke tipe yang sesuai ({@code Integer},
	 *       {@code Long}, atau {@code Double}; string apa adanya untuk
	 *       {@link ais.database.model.file.FotoAdmin} bila {@code usingId} tidak diminta).</li>
	 *   <li>Bila hasil instansiasi {@code clazz} berupa {@link FileFotoLain} dan
	 *       tipe properti adalah {@code Long}: mendelegasikan ke
	 *       {@link FileFotoLain#ambil(boolean, Object, String, Class)} dengan
	 *       {@code usingId} sesuai parameter request (lih. catatan keamanan pada
	 *       Javadoc kelas). Hasilnya bisa redirect GDrive/Dropbox, atau berkas
	 *       lokal (dengan resize opsional).</li>
	 *   <li>Bila tidak (jalur generik): mengambil {@code id}, {@code name}, dan
	 *       {@code gdrive} baris {@code clazz} yang cocok dengan {@code property}
	 *       (atau {@code id} langsung bila {@code usingId=true}), dengan filter
	 *       tambahan opsional {@code fotoId} (ID baris langsung) dan
	 *       {@code fotoUtama}. Bila kolom {@code gdrive} terisi, redirect ke
	 *       Google Drive. Bila tidak, kolom {@code foto} (blob) baris tersebut
	 *       dibaca lewat query terpisah dan disalin ke berkas cache lokal (lewat
	 *       {@link #writeBlobToFile(Blob, File)}), lalu (opsional) di-resize.</li>
	 * </ol>
	 * Pada setiap titik kegagalan (parameter tidak valid, baris/berkas tidak
	 * ditemukan, dsb.), method jatuh ke ikon default yang disiapkan di langkah
	 * ketiga.
	 * </p>
	 *
	 * @param request1 permintaan HTTP; lih. penjelasan parameter generik di Javadoc kelas dan langkah-langkah di atas
	 * @param resp respons HTTP; dipakai untuk redirect GDrive/Dropbox atau balasan status 400/404 secara langsung
	 * @return berkas yang harus disajikan ke klien, atau {@code null} bila response sudah ditulis/di-redirect langsung oleh method ini
	 * @throws Exception bila {@code clazz} tidak bisa dimuat, metadata/properti tidak valid, atau query database gagal; diteruskan ke pemanggil ({@link #process}) yang menelannya lewat pencatatan error
	 */
	@SuppressWarnings("rawtypes")
	private File loadFile(HttpServletRequest request1, HttpServletResponse resp) throws Exception {

		ServletContext sc = getServletContext();

		String imageNameDefault = "/img/administrator-icon_default.png";

		JSONObject jsonObject = null;

		try {
			String q = request1.getParameter("d");
			if (q != null && !q.trim().isEmpty()) {
				jsonObject = new JSONObject(Common.desEncrypter.get().decrypt(q));
			}
		} catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Tautan media tidak valid atau sudah tidak dapat dibaca. Buka ulang tautan dari aplikasi.");
			return null;
		}

		try {
			String filePath = jsonObject != null && !jsonObject.isNull("file") ? jsonObject.getString("file") : null;
//			System.out.println("filePath -> " + filePath);
			if (filePath != null && !filePath.trim().isEmpty()) {
				File file = new File(filePath);
				if (file.exists()) {
					return file;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AmbilMedia.java:132");
		}

//		System.out.println("jsonObject -> " + jsonObject);

		Boolean usingId = Boolean
				.parseBoolean(jsonObject != null && !jsonObject.isNull("usingId") ? jsonObject.getString("usingId")
						: (request1.getParameter("usingId") == null ? "false" : request1.getParameter("usingId")));

		String fotoId = jsonObject != null && !jsonObject.isNull("foto_id") ? jsonObject.getString("foto_id")
				: request1.getParameter("foto_id");
		String strid = jsonObject != null && !jsonObject.isNull("id") ? jsonObject.getString("id")
				: request1.getParameter("id");
		String name = jsonObject != null && !jsonObject.isNull("name") ? jsonObject.getString("name")
				: request1.getParameter("name");
		String foto = jsonObject != null && !jsonObject.isNull("foto") ? jsonObject.getString("foto")
				: request1.getParameter("foto");
		String clazz = jsonObject != null && !jsonObject.isNull("clazz") ? jsonObject.getString("clazz")
				: request1.getParameter("clazz");
		String property = jsonObject != null && !jsonObject.isNull("property") ? jsonObject.getString("property")
				: request1.getParameter("property");
		String height = jsonObject != null && !jsonObject.isNull("height") ? jsonObject.getString("height")
				: request1.getParameter("height");
		String width = jsonObject != null && !jsonObject.isNull("width") ? jsonObject.getString("width")
				: request1.getParameter("width");
		String fotoUtama = jsonObject != null && !jsonObject.isNull("fotoUtama") ? jsonObject.getString("fotoUtama")
				: request1.getParameter("fotoUtama");

		if (clazz != null && (

		clazz.equalsIgnoreCase("ais.database.model.file.FotoDosen")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoMahasiswa")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoMahasiswaLulus")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoPegawai")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoSiswa")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoGuru")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoAdmin")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoBiodataCalonMahasiswa")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoBiodataMahasiswa")
				|| clazz.equalsIgnoreCase("ais.database.model.file.FotoCalonSiswa")

		)) {
			imageNameDefault = "/img/administrator-icon.png";
		}

		else if (clazz != null && clazz.equalsIgnoreCase("ais.database.model.file.FotoGambarItem")) {
			imageNameDefault = "/img/book.jpg";
		}

		File file = new File(sc.getRealPath(imageNameDefault));

		// clazz berasal dari parameter request "clazz" (langsung atau lewat JSON
		// terenkripsi param "d"). Bila request tidak mengirim parameter ini (mis.
		// link lama/rusak, request tak lengkap, atau hanya mengirim "file" yang
		// ternyata sudah tidak ada di disk sehingga fallback ke jalur ini), clazz
		// akan null dan Class.forName(clazz) melempar NullPointerException mentah.
		// Validasi dulu dan balas 400 yang jelas ke klien alih-alih NPE.
		if (clazz == null || clazz.trim().isEmpty()) {
			try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Parameter 'clazz' tidak ditemukan atau tidak valid pada request AmbilMedia.");
			} catch (Exception ex) {
				ais.common.ErrorAuditUtil.record(ex,
						"auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:clazz-null-guard");
			}
			return null;
		}

		if (strid == null || strid.trim().length() == 0 || "null".equalsIgnoreCase(strid.trim())) {
			try {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Parameter 'id' tidak ditemukan atau tidak valid pada request AmbilMedia.");
			} catch (Exception ex) {
				ais.common.ErrorAuditUtil.record(ex,
						"auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:id-null-guard");
			}
			return null;
		}

		Class myClass = Class.forName(clazz);

		ClassMetadata classMetadata = StreamingHibernateUtil.getInstance().getClassMetadata(myClass);

		Class claazz = classMetadata.getPropertyType(property).getReturnedClass();

		Serializable value = strid;

		if (!usingId && clazz.equals(FotoAdmin.class.getName())) {
			value = strid;
		} else if (claazz.getName().equals(Integer.class.getName())) {
			value = Integer.parseInt(strid.trim());
		} else if (claazz.getName().equals(Long.class.getName())) {
			value = Long.parseLong(strid.trim());
		} else if (claazz.getName().equals(Double.class.getName())) {
			value = Double.parseDouble(strid.trim());
		}

		GeneralValueObject o = (GeneralValueObject) myClass.newInstance();
		if (o instanceof FileFotoLain && claazz.getName().equals(Long.class.getName())) {
			String jenis = ((FileFotoLain) o).getJenis();
			if (jenis != null && !jenis.trim().isEmpty()) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, value, jenis, myClass);

				if (fileFotoLain != null) {

					if (fileFotoLain.getGdrive() != null) {
						fileFotoLain.refreshFotoTemporaryGDrive();
						resp.sendRedirect(fileFotoLain.exportGDriveUrl());
						return null;
					} else if (fileFotoLain.getLink() != null
							&& fileFotoLain.getLink().toLowerCase().contains("dropbox")) {
						fileFotoLain.refreshFotoTemporaryGDrive();
						resp.sendRedirect(fileFotoLain.dropboxLinkRaw());
						return null;
					} else {
						File fileDariTemporary = fileFotoLain.ambilFile();
						if (fileDariTemporary.exists()) {
							o = null;

							String mimeType = CommonMedia.getMime(file);

							resp.setContentType(mimeType);

							if (fileDariTemporary.getName().toLowerCase().endsWith("pdf")) {
								resp.setHeader("Content-Disposition",
										"filename=\"" + fileDariTemporary.getName() + "\"");
							} else {
								resp.setHeader("Content-Disposition",
										"attachment; filename=\"" + fileDariTemporary.getName() + "\"");
							}
							if (height != null && width != null) {
								int w = Integer.parseInt(width);
								int h = Integer.parseInt(height);
								File filekecil = CommonMedia.resize(fileDariTemporary, w, h);
								return filekecil.exists() ? filekecil : fileDariTemporary;
							} else {

								return fileDariTemporary;
							}
						}
					}
				}
			}
		}
		o = null;

		ProjectionList projectionList = Projections.projectionList();
		projectionList.add(Projections.id());
		projectionList.add(Projections.property(name));
		projectionList.add(Projections.property("gdrive"));

		Session streamingSession1 = null;
		Object[] fileIds = null;
		try {
			streamingSession1 = StreamingHibernateUtil.getInstance().openSession();
			fileIds = (Object[]) streamingSession1.createCriteria(clazz).addOrder(Order.desc("id"))
					.add(fotoId == null || fotoId.trim().equals("") ? Restrictions.sqlRestriction("1=1")
							: Restrictions.idEq(Long.parseLong(fotoId)))
					.add(usingId ? Restrictions.eq("id", value) : Restrictions.eq(property, value))
					.add(fotoUtama == null ? Restrictions.sqlRestriction("1=1")
							: Restrictions.eq("fotoUtama", new Boolean(fotoUtama)))
					.setProjection(projectionList).setMaxResults(1).uniqueResult();
		} finally {
			if (streamingSession1 != null) {
				try { streamingSession1.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:268");}
				try { streamingSession1.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:269");}
				try { streamingSession1.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:270");}
			}
		}

		Object myId = fileIds != null && fileIds.length > 0 ? fileIds[0] : null;
		Object myName = fileIds != null && fileIds.length > 1 ? fileIds[1] : null;
		Object gdrive = fileIds != null && fileIds.length > 2 ? fileIds[2] : null;

		if (gdrive != null) {
//			resp.sendRedirect("https://drive.google.com/uc?export=view&id=" + gdrive);
			String url = "https://drive.usercontent.google.com/download?id="+gdrive+"&export=view";
			resp.sendRedirect(url);
			return null;
		}

		if (myId != null && myName != null) {

			resp.setHeader("Content-Disposition", "attachment; filename=\"" + myName + "\"");

			File originalFile = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + myId + "_" + clazz
					+ "_" + myName.toString().replaceAll(" ", "_"));

			if (!originalFile.exists()) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().openSession();
					// PostgreSQL Large Object butuh transaksi aktif dan non-aborted.
					// Reset transaksi yang mungkin aborted dari operasi sebelumnya, lalu mulai baru.
					try {
						if (streamingSession.getTransaction() != null
								&& streamingSession.getTransaction().isActive()) {
							streamingSession.getTransaction().rollback();
						}
					} catch (Exception txEx) { ais.common.ErrorAuditUtil.record(txEx, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:302"); /* ignore — tx mungkin tidak aktif */ }
					streamingSession.beginTransaction();
					Blob blob = null;
					try {
						blob = (Blob) streamingSession.createCriteria(clazz).add(Restrictions.idEq(myId))
								.setProjection(Projections.property(foto)).uniqueResult();
					} catch (Exception queryEx) {
						try { streamingSession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:309");}
						return file;
					}
					if (blob == null) {
						try { streamingSession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:313");}
						return file;
					}
					try {
						writeBlobToFile(blob, originalFile);
					} finally {
						try { streamingSession.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:319");}
					}
				} finally {
					if (streamingSession != null) {
						try { streamingSession.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:323");}
						try { streamingSession.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:324");}
						try { streamingSession.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:325");}
					}
				}
			} else if (!Common.isImage(originalFile)) {
				return new File(Common.REAL_PATH + imageNameDefault);
			}

			if (height != null && width != null) {
				int w = Integer.parseInt(width);
				int h = Integer.parseInt(height);
				File filekecil = CommonMedia.resize(originalFile, w, h);
				return filekecil.exists() ? filekecil : originalFile;
			} else {
				return originalFile;
			}

		}

		if (!Common.isImage(file)) {
			return new File(Common.REAL_PATH + imageNameDefault);
		}

		return file;

	}

	/**
	 * Menyalin isi {@code blob} ke {@code file} sekali saja: bila {@code file}
	 * sudah ada di disk, method langsung kembali tanpa melakukan apa pun (blob
	 * tidak dibaca ulang). Bila belum ada, method membuat berkas baru lalu
	 * menyalin seluruh isi {@link Blob#getBinaryStream()} lewat
	 * {@link #fastChannelCopy(ReadableByteChannel, WritableByteChannel)}. Bila
	 * penyalinan gagal di tengah jalan, berkas parsial yang sudah terlanjur
	 * dibuat dihapus agar tidak ter-cache sebagai berkas rusak, dan kegagalan
	 * penutupan kanal (mis. akibat transaksi PostgreSQL yang sudah ter-abort)
	 * ditelan per-kanal agar tidak mengganggu hasil penyalinan yang sudah selesai.
	 *
	 * @param blob sumber data biner dari kolom yang ditunjuk parameter {@code foto}; boleh {@code null} hanya bila {@code file} sudah ada
	 * @param file berkas cache tujuan penulisan
	 */
	private void writeBlobToFile(Blob blob, File file) {

		if (file != null && file.exists()) {
			return;
		}
		try {
			file.createNewFile();
			InputStream inputStream = blob.getBinaryStream();
			FileOutputStream outputStream = new FileOutputStream(file);
			final ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
			final WritableByteChannel outputChannel = Channels.newChannel(outputStream);
			try {
				fastChannelCopy(inputChannel, outputChannel);
			} finally {
				// Penutupan channel large-object dapat melempar IOException bila transaksi
				// PostgreSQL ter-abort; telan per-channel agar tidak mengganggu hasil copy.
				try { inputChannel.close(); } catch (Exception exClose) { ais.common.ErrorAuditUtil.record(exClose, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:367"); /* ignore */ }
				try { outputChannel.close(); } catch (Exception exClose) { ais.common.ErrorAuditUtil.record(exClose, "auto-audit(empty-catch) src/ais/action/servlet/AmbilMedia.java:368"); /* ignore */ }
			}
		} catch (Exception e) {
			// Hapus file parsial agar tidak ter-cache sebagai file rusak
			if (file != null && file.exists()) {
				file.delete();
			}
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menyalin seluruh isi {@code src} ke {@code dest} memakai buffer langsung
	 * (direct {@link ByteBuffer}) berukuran 16 KiB, dengan pola baca-flip-tulis-
	 * compact standar NIO sampai {@code src} habis, lalu mengosongkan sisa buffer
	 * yang belum tertulis.
	 *
	 * @param src kanal sumber data biner yang akan disalin
	 * @param dest kanal tujuan penulisan data biner
	 * @throws IOException bila operasi baca/tulis pada salah satu kanal gagal
	 */
	public void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
		while (src.read(buffer) != -1) {
			buffer.flip();
			dest.write(buffer);
			buffer.compact();
		}
		buffer.flip();
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

}
