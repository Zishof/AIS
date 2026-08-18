package ais.action.servlet;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyJSONObject;

/**
 * Servlet implementation class AmbilFotoPengumumanPerkuliahan
 */
public class AmbilLampiran extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public AmbilLampiran() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
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
	 * Whitelist direktori yang boleh disajikan lewat parameter "file" (token
	 * terenkripsi "d"). Tanpa ini, path absolut apa pun yang berhasil dienkode
	 * ke token akan disajikan mentah-mentah (arbitrary file read) -- lihat
	 * FileFotoLain.ambilLinkLampiranLain(File) yang jadi satu-satunya pembuat
	 * token dengan key "file", selalu berasal dari direktori media/webapp.
	 */
	private static boolean isDalamDirektoriDiizinkan(File file) {
		try {
			String canonical = file.getCanonicalPath();
			java.util.List<File> diizinkan = new java.util.ArrayList<File>();
			try {
				diizinkan.add(CommonMedia.getMediaDirectory());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:isDalamDirektoriDiizinkan"); }
			if (Common.REAL_PATH != null && !Common.REAL_PATH.trim().isEmpty()) {
				diizinkan.add(new File(Common.REAL_PATH));
			}
			for (File dir : diizinkan) {
				if (dir == null) {
					continue;
				}
				String dirCanonical = dir.getCanonicalPath();
				if (canonical.equals(dirCanonical) || canonical.startsWith(dirCanonical + File.separator)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:isDalamDirektoriDiizinkan");
			return false;
		}
	}

	/**
	 * Bangun nilai header Content-Disposition yang aman utk AJP: konektor AJP Tomcat hanya
	 * bisa mengirim header sebagai byte ISO-8859-1 (0-255), jadi nama berkas yang mengandung
	 * karakter di luar itu (emoji, dsb -- lihat KE-11 pengguna "wanto,400": nama berkas
	 * mengandung karakter unicode) membuat IllegalArgumentException saat Tomcat menyiapkan
	 * response, PADA SAAT byte pertama ditulis ke output stream. Solusi: kirim fallback ASCII
	 * lewat parameter "filename" (karakter non-ASCII diganti "_") DAN nama asli lewat parameter
	 * "filename*" berenkode RFC 5987 (UTF-8 persen-encode -- hasilnya selalu ASCII murni,
	 * sehingga aman utk AJP) supaya nama berkas asli tetap tampil di browser modern.
	 */
	private static String contentDispositionHeader(String disposition, String namaBerkas) {
		String nama = namaBerkas == null ? "" : namaBerkas;
		StringBuilder asciiFallback = new StringBuilder();
		for (int i = 0; i < nama.length(); i++) {
			char c = nama.charAt(i);
			asciiFallback.append(c <= 255 && c != '"' ? c : '_');
		}
		String header = disposition + ";filename=\"" + asciiFallback.toString() + "\"";
		try {
			header += ";filename*=UTF-8''" + java.net.URLEncoder.encode(nama, "UTF-8").replace("+", "%20");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:contentDispositionHeader");
		}
		return header;
	}

	public static void doDownload(HttpServletRequest request1, HttpServletResponse resp, File file) throws Exception {
		String mimeType = CommonMedia.getMime(file);
		resp.setContentType(mimeType);
		resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:87");

		}

		// in.close();
		IOUtils.closeQuietly(in);
		out.flush();
	}

	@SuppressWarnings("rawtypes")
	private void process(HttpServletRequest request1, HttpServletResponse resp) throws Exception {

		MyJSONObject jsonObject = null;

		try {
			String q = request1.getParameter("d");
			if (q != null && !q.trim().isEmpty()) {
				jsonObject = new MyJSONObject(Common.desEncrypter.get().decrypt(q));
			}

//			System.out.println("jsonObject -> " + jsonObject);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:108");
//			e.printStackTrace();
		}

		try {
			String filePath = jsonObject != null && !jsonObject.isNull("file") ? jsonObject.getString("file") : null;
//			System.out.println("filePath -> " + filePath);
			if (filePath != null && !filePath.trim().isEmpty()) {
				File file = new File(filePath);
				// Penjagaan arbitrary-file-read: token "d" berisi path absolut apa pun yang
				// dienkode pemanggil (mis. LampiranLain.ambilLinkLampiranLain(File)). Tanpa
				// batasi ke direktori yang memang dipakai utk menyimpan lampiran/aset publik,
				// path apa pun di server bisa diminta lewat sini. Gunakan canonical path
				// (menormalkan "..") supaya tidak bisa keluar dari direktori yang diizinkan.
				if (file.exists() && isDalamDirektoriDiizinkan(file)) {

					AmbilLampiran.doDownload(request1, resp, file);
					return;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:123");
//			e.printStackTrace();
		}

//		System.out.println("jsonObject -> " + jsonObject);

		String download = jsonObject != null && !jsonObject.isNull("download") ? jsonObject.getString("download")
				: request1.getParameter("download");

		String id = jsonObject != null && !jsonObject.isNull("ref") ? jsonObject.get("ref").toString()
				: request1.getParameter("ref");
		Class clazz = LampiranLain.class;
		try {

			try {
				String cc = jsonObject != null && !jsonObject.isNull("clazz") ? jsonObject.getString("clazz")
						: request1.getParameter("clazz");
				if (cc != null) {
					clazz = Class.forName(cc);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:143");
				// TODO: handle exception
			}

			Serializable ref = null;

			if (clazz.getName().equals(FotoAdmin.class.getName())) {
				ref = id;
			} else if (id == null || id.trim().isEmpty()) {
				// parameter "ref"/"d" tidak dikirim atau tidak valid -> jangan lanjut
				// proses (mis. Long.parseLong(id.trim()) akan NPE), balas error
				// yang jelas ke client drpd NPE mentah.
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter ref/id lampiran tidak valid");
				return;
			} else {
				try {
					ref = Long.parseLong(id.trim());
				} catch (Exception e) {
					try {
						ref = new BigDecimal(id.trim()).longValue();
					} catch (Exception we) { ais.common.ErrorAuditUtil.record(we, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:157");

					}
				}
			}

//			System.out.println("id -> " + id + " ref -> " + ref);

			InputStream in = null;

			if (ref != null && ref.equals(LampiranLain.ID_SKIN)) {
				String content_path = request1.getContextPath().replace("/", "");
				File file = new File("/opt/" + content_path + ".zip");
				if (file != null && file.exists()) {
					File fileTujuan = new File(
							CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + file.getName());
					if (!fileTujuan.exists()) {
						FileUtils.copyFile(file, fileTujuan);
					}
					resp.setContentType("application/zip");
					in = new FileInputStream(file);
					resp.setContentLength((int) file.length());
				}
			} else {

				String jurusan = jsonObject != null && !jsonObject.isNull("jurusan") ? jsonObject.getString("jurusan")
						: request1.getParameter("jurusan");
				String jenis = jsonObject != null && !jsonObject.isNull("jenis") ? jsonObject.getString("jenis")
						: request1.getParameter("jenis");
				String iframe = jsonObject != null && !jsonObject.isNull("iframe") ? jsonObject.getString("iframe")
						: request1.getParameter("iframe");
				Boolean usingId = Boolean.parseBoolean(
						jsonObject != null && !jsonObject.isNull("usingId") ? jsonObject.get("usingId") + ""
								: request1.getParameter("usingId"));

				Boolean rezise = Boolean.parseBoolean(
						jsonObject != null && !jsonObject.isNull("rezise") ? jsonObject.get("rezise") + "" : "false");

				if (clazz.getName().equals(FotoAdmin.class.getName())) {
					usingId = true;
				}

				FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis + (jurusan == null ? "" : jurusan),
						clazz, false);

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis + (jurusan == null ? "" : jurusan), clazz,
							true);
				}

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(ref, jenis + (jurusan == null ? "" : jurusan), clazz, false);
				}

				if (fileFotoLain == null) {
					fileFotoLain = FileFotoLain.ambil(ref, jenis + (jurusan == null ? "" : jurusan), clazz, true);
				}

				if (iframe != null && fileFotoLain != null && fileFotoLain.getGdrive() != null) {
					String url = "https://drive.google.com/file/d/" + fileFotoLain.getGdrive() + "/preview";
					resp.sendRedirect(url);
					return;
				} else if (fileFotoLain != null && fileFotoLain.getGdrive() != null) {
					resp.sendRedirect(fileFotoLain.forwardGDriveUrl());
					return;
				}
				if (fileFotoLain != null && fileFotoLain.getLink() != null && !fileFotoLain.getLink().isEmpty()) {
					try {
						in = new URL(fileFotoLain.getLink()).openStream();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/AmbilLampiran.java:227");
						resp.sendRedirect(fileFotoLain.getLink());
						return;
					}
				} else {

					File file = fileFotoLain == null ? null : fileFotoLain.ambilFile();

//					System.out.println("rezise -> " + rezise + " file " + file);

					if (file != null && file.exists() && file.length() > 0L) {

						String namaFile = fileFotoLain.getNama();
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, " ", "_");
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "%", "_");
						namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "#", "_");

						String fileDiMedia = CommonMedia.getMediaDirectory().getAbsolutePath() + "/"
								+ fileFotoLain.getId() + "/" + namaFile;
						File fileTujuan = new File(fileDiMedia);
						if (!fileTujuan.getParentFile().exists()) {
							fileTujuan.getParentFile().mkdirs();
						}
						boolean exist = fileTujuan.exists();

						if (!exist) {
							FileUtils.copyFile(file, fileTujuan);
						}
					}

					// Berkas fisik harus benar-benar ada di disk sebelum dibuka via FileInputStream;
					// bila tidak (mis. file terhapus manual/migrasi server), jatuh ke cabang di bawah
					// yang menyajikan langsung dari BLOB atau ikon placeholder, alih-alih melempar
					// FileNotFoundException/NPE mentah.
					if (fileFotoLain != null && file != null && file.exists() && file.length() > 0L) {

						String mimeType = CommonMedia.getMime(file);
						resp.setContentType(mimeType);
						if (file.getName().toLowerCase().endsWith("pdf") || file.getName().toLowerCase().endsWith("jpg")
								|| file.getName().toLowerCase().endsWith("jpeg")
								|| file.getName().toLowerCase().endsWith("png")
								|| file.getName().toLowerCase().endsWith("gif")) {

							if (rezise) {
								if (file.getName().toLowerCase().endsWith("jpg")
										|| file.getName().toLowerCase().endsWith("jpeg")
										|| file.getName().toLowerCase().endsWith("png")
										|| file.getName().toLowerCase().endsWith("gif")) {

									String extenstion = Common.getFileExtension(file);

									String fileDiMediathumbnail = file.getParentFile().getAbsolutePath() + "/thumbnail_"
											+ file.getName();
									File fileKecil = new File(fileDiMediathumbnail);
									if (!fileKecil.exists()) {
										BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
										if (originalImage != null) {
											BufferedImage thumbnail = Scalr.resize(originalImage, 128);
											ImageIO.write(thumbnail, extenstion, fileKecil);
											file = fileKecil;
										}
									} else {
										file = fileKecil;
									}
								}
							}
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						} else if ((download != null && !download.equalsIgnoreCase("false"))
								|| file.getName().trim().toLowerCase().endsWith(".xml")
								|| file.getName().trim().toLowerCase().endsWith(".jrxml")) {
							resp.setHeader("Content-Disposition", contentDispositionHeader("attachment", file.getName()));
							resp.setContentLength((int) file.length());
						} else {
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						}
						in = new FileInputStream(file);

					} else if (fileFotoLain != null && file != null) {

						String mimeType = CommonMedia.getMime(file);

						resp.setContentType(mimeType);
						if (file.getName().toLowerCase().endsWith("pdf") || file.getName().toLowerCase().endsWith("jpg")
								|| file.getName().toLowerCase().endsWith("jpeg")
								|| file.getName().toLowerCase().endsWith("png")
								|| file.getName().toLowerCase().endsWith("gif")) {
							if (rezise) {
								if (file.getName().toLowerCase().endsWith("jpg")
										|| file.getName().toLowerCase().endsWith("jpeg")
										|| file.getName().toLowerCase().endsWith("png")
										|| file.getName().toLowerCase().endsWith("gif")) {

									String fileDiMediathumbnail = file.getParentFile().getAbsolutePath() + "/thumbnail_"
											+ file.getName();
									File fileKecil = new File(fileDiMediathumbnail);
									if (!fileKecil.exists()) {

										String extenstion = Common.getFileExtension(file);
//										System.out.println("extenstion " + extenstion);

										BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(file);
										if (originalImage != null) {
											BufferedImage thumbnail = Scalr.resize(originalImage, 128);
											ImageIO.write(thumbnail, extenstion, fileKecil);
											file = fileKecil;
										}
									} else {
										file = fileKecil;
									}
								}
							}
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", file.getName()));
							resp.setContentLength((int) file.length());
						} else if ((download != null && !download.equalsIgnoreCase("false"))
								|| file.getName().trim().toLowerCase().endsWith(".xml")
								|| file.getName().trim().toLowerCase().endsWith(".jrxml")) {
							resp.setHeader("Content-Disposition", contentDispositionHeader("attachment", fileFotoLain.getNama()));
							resp.setContentLength((int) file.length());
						} else {
							resp.setHeader("Content-Disposition", contentDispositionHeader("inline", fileFotoLain.getNama()));
							resp.setContentLength((int) file.length());
						}
						in = (fileFotoLain.getCopyDari() != null ? fileFotoLain.getCopyDari().getFoto()
								: fileFotoLain.getFoto()).getBinaryStream();
					} else if (fileFotoLain != null) {
						// fileFotoLain ditemukan di DB tapi berkas fisiknya (file) tidak dapat
						// ditentukan (mis. entity ter-detach dari session Hibernate saat ambilFile()
						// dipanggil). Jangan lempar NPE -> sajikan ikon "berkas tidak ada", sama
						// seperti kasus fileFotoLain == null di cabang paling bawah.
						file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
						in = new FileInputStream(file);
						resp.setContentType("image/png");
						resp.setContentLength((int) file.length());
					} else if (jenis != null && jenis.toLowerCase().contains("kop")) {
						file = new File(Common.ambilREAL_PATH_REPORT() + "/sekolah/kop_surat.jpg");
						if (file == null || !file.exists()) {
							file = new File(Common.ambilREAL_PATH_REPORT() + "/wood.jpg");
							resp.setContentLength((int) file.length());
						}
						in = new FileInputStream(file);
						resp.setContentType("image/jpg");
					} else {
						file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
						in = new FileInputStream(file);
						resp.setContentType("image/png");
						resp.setContentLength((int) file.length());
					}

				}
			}

			// NPE guard: cabang ID_SKIN di atas hanya mengisi `in` bila berkas .zip ada di
			// disk; kalau tidak ada, `in` tetap null dan in.available() di bawah melempar
			// NPE mentah. Sajikan ikon "berkas tidak ada" seperti cabang fallback lain di
			// method ini, alih-alih membiarkan NPE lolos ke caller.
			if (in == null) {
				File file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
				in = new FileInputStream(file);
				resp.setContentType("image/png");
				resp.setContentLength((int) file.length());
			}

			ServletOutputStream out = resp.getOutputStream();

			int length = (int) in.available();

			int bufferSize = 1024;
			byte[] buffer = new byte[bufferSize];

			try {
				while ((length = in.read(buffer)) != -1) {
					out.write(buffer, 0, length);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:382");

			}

			// in.close();
			IOUtils.closeQuietly(in);
			out.flush();

		} catch (Exception e) {
//			e.printStackTrace();
			try {
				ServletOutputStream out = resp.getOutputStream();

				File file = new File(Common.REAL_PATH + "/img/" + FileFotoLain.iconNggakAda(clazz));
				resp.setContentLength((int) file.length());
				FileInputStream in = new FileInputStream(file);
				resp.setContentType("image/png");
				int length = (int) in.available();

				int bufferSize = 1024;
				byte[] buffer = new byte[bufferSize];

				try {
					while ((length = in.read(buffer)) != -1) {
						out.write(buffer, 0, length);
					}
				} catch (Exception se) { ais.common.ErrorAuditUtil.record(se, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:408");

				}

				in.close();
				out.flush();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/action/servlet/AmbilLampiran.java:414");
				// TODO: handle exception
			}
		}

	}

}
