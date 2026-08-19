package ais.action.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.FileSizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.json.JSONException;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;

import ais.action.master.helper.generic.AmbilDataPertemuanFileContent;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.servlet.api.ApiUtil;
import ais.common.Common;
import ais.common.ObjectHelper;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GDriveCode;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.action.ws.util.ConstantUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BuktiPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.LampiranLainBiodataCalonMahasiswa;
import ais.ui.util.WaktuUtil;

public class DoUpload extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// Pending files untuk GDrive auth handoff
	public static Map<String, File> filesPending = new ConcurrentHashMap<String, File>();

	// === RETRY QUEUE: file diterima tapi DB gagal → coba ulang 10 menit kemudian ===
	// Thread DAEMON + bernama: versi lama memakai factory default (non-daemon, tanpa
	// shutdown) sehingga thread scheduler menahan classloader webapp saat redeploy.
	private static final ScheduledExecutorService RETRY_SCHEDULER =
		Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "doupload-retry");
				t.setDaemon(true);
				return t;
			}
		});

	/**
	 * Menghentikan scheduler retry upload saat webapp stop/redeploy. Dipanggil dari
	 * {@code AppStartupListener.contextDestroyed}. Antrian retry yang belum tereksekusi
	 * memang hangus — file fisiknya sudah tersimpan dan aman diproses ulang manual.
	 */
	public static void hentikanRetryScheduler() {
		try {
			RETRY_SCHEDULER.shutdownNow();
		} catch (Throwable abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "DoUpload.hentikanRetryScheduler");
		}
	}

	private static final ConcurrentHashMap<String, PendingUpload> RETRY_QUEUE =
		new ConcurrentHashMap<String, PendingUpload>();

	@SuppressWarnings("rawtypes")
	private static class PendingUpload {
		final File file;
		final Class clazz;
		final Serializable ref;
		final String jenis;
		final String subdata;
		final String nama;

		PendingUpload(File file, Class clazz, Serializable ref, String jenis, String subdata, String nama) {
			this.file = file;
			this.clazz = clazz;
			this.ref = ref;
			this.jenis = jenis;
			this.subdata = subdata;
			this.nama = nama;
		}
	}

	@SuppressWarnings("rawtypes")
	private static void masukkanAntriRetry(final File file, final Class clazz,
			final Serializable ref, final String jenis, final String subdata, final String nama) {
		final String key = "retry_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 100000);
		final PendingUpload pending = new PendingUpload(file, clazz, ref, jenis, subdata, nama);
		RETRY_QUEUE.put(key, pending);
		RETRY_SCHEDULER.schedule(new Runnable() {
			public void run() {
				cobaUlangSimpan(key, pending);
			}
		}, 10L, TimeUnit.MINUTES);
		System.err.println("[DoUpload] Antri retry: " + nama + " ref=" + ref + " key=" + key);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void cobaUlangSimpan(String key, PendingUpload pending) {
		Session s = null;
		try {
			s = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
			FileFotoLain.createFileFotoLain(null, s, pending.clazz, false,
				pending.ref, pending.jenis, null, pending.file, pending.nama);
			RETRY_QUEUE.remove(key);
			System.err.println("[DoUpload] Retry BERHASIL: " + pending.nama + " key=" + key);
		} catch (Exception eRetry) {
			RETRY_QUEUE.remove(key);
			System.err.println("[DoUpload] Retry GAGAL FINAL: " + pending.nama
				+ " key=" + key + " err=" + eRetry.getMessage());
			if (pending.file != null && pending.file.exists()) {
				try { pending.file.delete(); } catch (Exception eDel) { ais.common.ErrorAuditUtil.record(eDel, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:126"); /* abaikan */ }
			}
		} finally {
			if (s != null && s.isOpen()) {
				try { s.clear(); s.close(); } catch (Exception eClose) { ais.common.ErrorAuditUtil.record(eClose, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:130"); /* abaikan */ }
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	@SuppressWarnings("unchecked")
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("status", "OK");
		} catch (JSONException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:153");
		}

		Map<String, String> formFields = new HashMap<String, String>();
		InputStream fileInputStream = null;
		File tempFile = null;

		long maxFileSize = 1024 * 1024L;
		try {
			String ukuranFileStr = Common.getKonfigurasi("ukuran_maksimal_file_diupload", "1024").getNilai();
			if (ukuranFileStr != null && !ukuranFileStr.trim().isEmpty()) {
				maxFileSize = Long.parseLong(ukuranFileStr.trim()) * 1024L;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:166");
		}

		long maxRequestSize = maxFileSize + (2 * 1024 * 1024L);

		try {
			DiskFileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setFileSizeMax(maxFileSize);
			upload.setSizeMax(maxRequestSize);

			List<FileItem> items = upload.parseRequest(request);
			for (FileItem item : items) {
				if (item.isFormField())
					formFields.put(item.getFieldName(), item.getString());
				else if (item.getInputStream() != null)
					fileInputStream = item.getInputStream();
			}

			String namaFile = formFields.get("nama");
			if (fileInputStream != null && namaFile != null) {
				tempFile = saveFileToDisk(namaFile, fileInputStream);
			}

			String pertemuanFileContent = formFields.get("pertemuanFileContent");
			if (pertemuanFileContent != null && !pertemuanFileContent.trim().isEmpty()) {
				handlePertemuanContent(jsonObject, pertemuanFileContent, tempFile);
			} else {
				handleUserProfileUpload(request, jsonObject, formFields, tempFile);
			}

		} catch (FileSizeLimitExceededException e) {
			try {
				jsonObject.put("status", "Gagal");
				jsonObject.put("keterangan", "Ukuran file melebihi batas " + (maxFileSize / 1024L) + " KB.");
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:201");
			}
		} catch (SizeLimitExceededException e) {
			try {
				jsonObject.put("status", "Gagal");
				jsonObject.put("keterangan", "Total data request melebihi batas sistem.");
			} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:207");
			}
		} catch (Exception e) {
			handleException(request, jsonObject, e);
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}

		sendJsonResponse(response, jsonObject);
	}

	private File saveFileToDisk(String fileName, InputStream inputStream) throws IOException {
		File outputFile = new File(Common.ambilREAL_PATH_REPORT() + "/" + fileName);
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(outputFile);
			IOUtils.copy(inputStream, outputStream);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
		return outputFile;
	}

	private void handlePertemuanContent(JSONObject jsonObject, String pertemuanContentId, File file)
			throws JSONException {
		if (file != null && file.exists()) {
			AmbilDataPertemuanFileContent.mapFileUpload.put(pertemuanContentId, file);
			jsonObject.put("status", "Sukses");
		} else {
			jsonObject.put("status", "Gagal");
		}
	}

	private void handleUserProfileUpload(HttpServletRequest request, JSONObject jsonObject, Map<String, String> fields,
			File file) throws Exception {
		for (Map.Entry<String, String> entry : fields.entrySet())
			jsonObject.put(entry.getKey(), entry.getValue());
		String token = fields.get("token");
		String tanpaLogin = fields.get("tanpaLogin");
		Tbmuser tbmuser = (token != null) ? ApiUtil.currentUser(token) : Common.getCurrentUser(request);

		if (tbmuser == null && (tanpaLogin == null || !tanpaLogin.equalsIgnoreCase("true"))) {
			jsonObject.put("status", "Gagal");
			return;
		}
		if (file == null || !file.exists()) {
			jsonObject.put("status", "Gagal");
			return;
		}
		processHibernateTransaction(request, tbmuser, fields, file, jsonObject);
	}

	@SuppressWarnings("rawtypes")
	private void processHibernateTransaction(HttpServletRequest request, final Tbmuser tbmuser, Map<String, String> fields, final File file, JSONObject jsonObject) throws Exception {
		Session session = null;
		Transaction tx = null;
		try {
			session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
			String fotoProfile = fields.get("fotoProfile");
			String clazz = fields.get("clazz");
			String id = fields.get("id");
			String jenis = fields.get("jenis");
			String subdata = fields.get("subdata");

			if (tbmuser != null && "true".equalsIgnoreCase(fotoProfile)) {
				tx = session.beginTransaction();
				jsonObject.put("url", processProfilePhoto(session, tbmuser, fields.get("nama"), jenis, file, jsonObject));
				tx.commit();
			} else {
				Serializable ref = (id != null && Common.isNumber(id.trim())) ? Long.parseLong(id.trim()) : 0L;
				Class c = Class.forName(clazz);
				
				boolean isDrive = "true".equalsIgnoreCase(fields.get("toDrive"));
				boolean isConnected = false;

				// =========================================================================================
				// SIMPAN LOKAL TERLEBIH DAHULU; jika DB gagal → antri retry 10 menit
				// =========================================================================================
				FileFotoLain fileFotoLain;
				try {
					fileFotoLain = FileFotoLain.createFileFotoLain(tbmuser, session, c, false, ref, jenis, null, isDrive ? null : file, file.getName());
				} catch (Exception eDb) {
					if (!isDrive) {
						// File sudah tersimpan di disk — antri retry, beritahu user "Menunggu"
						masukkanAntriRetry(file, c, ref, jenis, subdata, file.getName());
						jsonObject.put("status", "Menunggu");
						jsonObject.put("keterangan", "File diterima. Sistem akan menyimpan ulang dalam 10 menit.");
						return;
					}
					throw eDb;
				}
				final Long fotoId = fileFotoLain.getId();
				// Bila upload adalah bukti bayar PMB dari JSP, buat juga BuktiPembayaran + LampiranLain
				// agar bagian keuangan dapat melihat dan men-download dari DaftarUlangMahasiswaBaruAction.
				if (LampiranLainBiodataCalonMahasiswa.class.equals(c)
						&& ref instanceof Long && ((Long) ref) > 0L
						&& (LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN.equals(jenis)
								|| LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_DAFTAR_ULANG.equals(jenis))) {
					// Sub-langkah OPSIONAL: buat BuktiPembayaran + LampiranLain PMB agar bagian keuangan
					// bisa melihat/mengunduh. Bila gagal (mis. entitas LampiranLain belum terdaftar di
					// SessionFactory streaming karena hibernate.streaming.cfg.xml belum ter-deploy), JANGAN
					// menggagalkan upload utama yang SUDAH tersimpan & ter-commit di atas — cukup catat log.
					try {
						buatBuktiPembayaranDariLampiran(session, (Long) ref, jenis, file);
					} catch (Exception eBukti) {
						try {
							Common.tampilErrorJikaAdmin(eBukti);
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:314");
						}
					}
				}
				final Class finalC = c;
				final Tbmuser finalUser = tbmuser;
				final PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi(request);
				final File finalFileKirim = file; 

				// =========================================================================================
				// ATTEMPT DIRECT UPLOAD (LOGIKA BARU ANDA)
				// =========================================================================================
				if (isDrive && tbmuser != null) {
					Label labelMock = new Label("");
					GDriveUtilPerPengguna driveUtil = new GDriveUtilPerPengguna(tbmuser);

					try {
						driveUtil.kirimBackupLangsung(labelMock,
								finalFileKirim, pt, finalC.getSimpleName(), new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0.getData();
										if (fileUpload != null && fileUpload.getId() != null) {
											Session s3 = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
											try {
												FileFotoLain fRefresh = (FileFotoLain) s3.get(finalC, fotoId);
												s3.refresh(fRefresh);
												fRefresh.setFoto(null); // Pastikan blob aman (bersih)
												fRefresh.setGdrive(fileUpload.getId());
												fRefresh.setGdriveUsername(finalUser.getUserId());
												s3.getTransaction().begin();
												s3.update(fRefresh);
												s3.getTransaction().commit();

												// Hapus File Lokal Temp
												if (finalFileKirim != null && finalFileKirim.exists()) {
													finalFileKirim.delete();
												}
											} finally {
												if (s3.isOpen()) {
													s3.clear();
													s3.close();
												}
											}
										}
									}
								});

						// Cek apakah ada error dari proses upload
						isConnected = !labelMock.getValue().equals("Error");
					} catch (Exception e) {
						System.err.println("Gagal Direct Upload Drive: " + e.getMessage());
						isConnected = false;
					}
				}

				// =========================================================================================
				// EVALUASI KONEKSI (JIKA GAGAL -> MINTA LOGIN, JIKA SUKSES -> SELESAI)
				// =========================================================================================
				if (isDrive && !isConnected) {
					// Bersihkan cache token yang rusak
					ais.common.GoogleCommon.codes.remove(tbmuser.getUserId());
					Session sDel = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
					try {
						GDriveCode gcDel = (GDriveCode) sDel.createCriteria(GDriveCode.class).add(Restrictions.eq("nama", tbmuser.getUserId())).setMaxResults(1).uniqueResult();
						if (gcDel != null) { sDel.getTransaction().begin(); sDel.delete(gcDel); sDel.getTransaction().commit(); }
					} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:380");} finally { if(sDel.isOpen()) { sDel.clear(); sDel.close(); } }
					
					// Simpan file ke map sementara untuk diproses setelah popup login selesai
					filesPending.put(fotoId.toString(), file);

					String stateParam = URLEncoder.encode(Common.getRequestHostWithProtocol(request) + "/accept.jsp?u=" + tbmuser.getUserId(), "UTF-8");
					String authUrl = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id="
							+ ais.common.GoogleCommon.getGoogle_drive_client_id() + "&redirect_uri="
							+ ais.common.GoogleCommon.getRedirect_url_drive()
							+ "&response_type=code&scope=https://www.googleapis.com/auth/drive.file&state="
							+ stateParam;

					jsonObject.put("status", "NeedAuth");
					jsonObject.put("authUrl", authUrl);
					jsonObject.put("id", fotoId);
					jsonObject.put("clazz", clazz);
				} else {
					// Jika upload drive langsung sukses ATAU ini adalah upload lokal biasa
					JSONObject jsonObjectdata = Common.validJsonObject(subdata);
					if (jsonObjectdata != null) {
						jsonObjectdata.put("fileMimeType", fileFotoLain.getKeterangan());
						ClassMetadata classMetadata = StreamingHibernateUtil.getInstance().getClassMetadata(c);
						session.refresh(fileFotoLain);
						ObjectHelper.setObjectValues(classMetadata, fileFotoLain, jsonObjectdata);
						Transaction txSub = session.getTransaction();
						txSub.begin(); session.update(fileFotoLain); txSub.commit();
					}

					jsonObject.put("status", "Sukses");
					jsonObject.put("id", fileFotoLain.getId());
					jsonObject.put("url", fileFotoLain.createLinkUri(false));
				}
			}
		} catch (Exception e) {
			if (tx != null && tx.isActive()) tx.rollback();
			throw e;
		} finally {
			if (session != null && session.isOpen()) { session.clear(); session.close(); }
		}
	}

	private String processProfilePhoto(Session session, Tbmuser user, String nama, String jenis, File file,
			JSONObject jsonObject) throws Exception {
		if (user.getMahasiswa() != null)
			return saveFotoEntity(session, FotoMahasiswa.class, "mahasiswa", user.getMahasiswa().getId(), nama, jenis,
					file, jsonObject);
		else if (user.getSiswa() != null)
			return saveFotoEntity(session, FotoSiswa.class, "siswa", user.getSiswa().getId(), nama, jenis, file,
					jsonObject);
		else if (user.getDosen() != null)
			return saveFotoEntity(session, FotoDosen.class, "dosen", user.getDosen().getId(), nama, jenis, file,
					jsonObject);
		else if (user.getGuru() != null)
			return saveFotoEntity(session, FotoGuru.class, "guru", user.getGuru().getId(), nama, jenis, file,
					jsonObject);
		else if (user.getPegawai() != null)
			return saveFotoEntity(session, FotoPegawai.class, "pegawai", user.getPegawai().getId(), nama, jenis, file,
					jsonObject);
		else
			return saveFotoEntity(session, FotoAdmin.class, "tbmuser", user.getUserId(), nama, jenis, file, jsonObject);
	}

	@SuppressWarnings("deprecation")
	private String saveFotoEntity(Session session, Class<? extends FileFotoLain> entityClass, String fkField,
			Serializable fkId, String nama, String jenis, File file, JSONObject jsonObject) throws Exception {
		Object oldEntity = session.createCriteria(entityClass).add(Restrictions.eq(fkField, fkId)).setMaxResults(1)
				.uniqueResult();
		if (oldEntity != null) {
			session.delete(oldEntity);
			session.flush();
		}

		FileFotoLain newEntity = entityClass.newInstance();
		entityClass.getMethod("setNama", String.class).invoke(newEntity, nama);
		entityClass.getMethod("setKeterangan", String.class).invoke(newEntity, jenis);
		entityClass.getMethod("set" + Character.toUpperCase(fkField.charAt(0)) + fkField.substring(1), Long.class)
				.invoke(newEntity, fkId);

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			entityClass.getMethod("setFoto", java.sql.Blob.class).invoke(newEntity, new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fis)));
		} finally {
			IOUtils.closeQuietly(fis);
		}

		session.save(newEntity);
		jsonObject.put("id", newEntity.getId());
		return (String) entityClass.getMethod("createLinkUri", boolean.class).invoke(newEntity, false);
	}

	/**
	 * Setelah DoUpload menyimpan LampiranLainBiodataCalonMahasiswa untuk bukti bayar PMB,
	 * buat juga BuktiPembayaran + LampiranLain agar DaftarUlangMahasiswaBaruAction
	 * (bagian keuangan) dapat melihat dan men-download bukti tersebut.
	 * Semua error ditelan agar upload utama tetap berhasil.
	 */
	@SuppressWarnings("deprecation")
	private void buatBuktiPembayaranDariLampiran(Session session, Long biodataId, String jenis, File file) {
		// Entitas PMB (BiodataCalonMahasiswa/JenisKegiatan/BuktiPembayaran) dipetakan di SessionFactory
		// UTAMA (HibernateUtil). Sedangkan LampiranLain (blob Large Object / oid) HANYA dipetakan di
		// SessionFactory STREAMING. Bila keduanya dicampur pada satu session, akan muncul
		// "Unknown entity: ais.database.model.file.LampiranLain". Karena itu masing-masing disimpan
		// lewat session dari factory yang benar. Semua error ditelan agar upload utama tetap berhasil.
		Session mainSession = null;
		Session lampiranSession = null;
		java.sql.Connection lampiranConn = null;
		try {
			mainSession = ais.database.hibernate.HibernateUtil.getSessionFactory().openSession();
			BiodataCalonMahasiswa cama = (BiodataCalonMahasiswa) mainSession.get(BiodataCalonMahasiswa.class, biodataId);
			if (cama == null) {
				return;
			}

			String namaJenisKeg = LampiranLainBiodataCalonMahasiswa.BUKTI_BAYAR_PENDAFTARAN.equals(jenis)
					? ConstantUtil.PENDAFTARAN_CALON_MAHASISWA
					: ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU;

			JenisKegiatan jenisKeg = (JenisKegiatan) mainSession.createCriteria(JenisKegiatan.class)
					.add(Restrictions.eq("namaKegiatan", namaJenisKeg)).setMaxResults(1).uniqueResult();
			if (jenisKeg == null) {
				return;
			}

			// LampiranLain berisi blob Large Object (oid) yang WAJIB ditulis pada koneksi NON-autocommit.
			// c3p0 melepas koneksi antar-statement sehingga beginTransaction() saja tidak cukup — ambil
			// SATU koneksi langsung dari ConnectionProvider, kendalikan autocommit sendiri, lalu buka
			// session di ATAS koneksi itu agar tidak dilepas antar-statement (pola sama dgn FileFotoLain).
			org.hibernate.engine.SessionFactoryImplementor sfStreaming = (org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil
					.getInstance().getSessionFactory();
			org.hibernate.connection.ConnectionProvider cp = sfStreaming.getConnectionProvider();
			lampiranConn = cp.getConnection();
			lampiranConn.setAutoCommit(false);
			lampiranSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession(lampiranConn);

			LampiranLain lainMahasiswa = new LampiranLain();
			lainMahasiswa.setRef(Common.refSementara());
			lainMahasiswa.setNama(file.getName());
			lainMahasiswa.setKeterangan("Bukti Bayar PMB");
			lainMahasiswa.setJenis(BuktiPembayaran.class.getName());

			FileInputStream fisBlob = null;
			try {
				fisBlob = new FileInputStream(file);
				java.sql.Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(fisBlob));
				lainMahasiswa.setFoto(blob);
			} finally {
				IOUtils.closeQuietly(fisBlob);
			}

			lampiranSession.save(lainMahasiswa);
			lampiranSession.flush();
			lampiranConn.commit();
			Long idLampiran = lainMahasiswa.getId();

			// Buat BuktiPembayaran tanpa cicilanPembayaran; staff keuangan akan melengkapi nilai dan
			// mengaitkan ke cicilan via DaftarUlangMahasiswaBaruAction. Disimpan lewat session UTAMA.
			BuktiPembayaran buktiPembayaran = new BuktiPembayaran();
			buktiPembayaran.setNama(file.getName());
			buktiPembayaran.setTanggal(WaktuUtil.getDate());
			buktiPembayaran.setJenisKegiatan(jenisKeg);
			buktiPembayaran.setBiodataCalonMahasiswa(cama);
			buktiPembayaran.setIdLampiran(idLampiran);
			Transaction txBukti = mainSession.beginTransaction();
			mainSession.save(buktiPembayaran);
			txBukti.commit();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			// Tutup session streaming + kembalikan koneksi LO ke pool (autocommit dipulihkan).
			if (lampiranSession != null) {
				try {
					lampiranSession.clear();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:553");
				}
				try {
					lampiranSession.close();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:557");
				}
			}
			if (lampiranConn != null) {
				try {
					lampiranConn.setAutoCommit(true);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:563");
				}
				try {
					((org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil.getInstance()
							.getSessionFactory()).getConnectionProvider().closeConnection(lampiranConn);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:568");
				}
			}
			if (mainSession != null) {
				try {
					mainSession.clear();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:574");
				}
				try {
					mainSession.disconnect();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:578");
				}
				try {
					mainSession.close();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:582");
				}
			}
		}
	}

	private void handleException(HttpServletRequest request, JSONObject jsonObject, Exception e) {
		try {
			jsonObject.put("status", "Gagal");
			jsonObject.put("keterangan", "Error sistem: " + e.getMessage());
		} catch (JSONException ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/DoUpload.java:592");
		}
	}

	private void sendJsonResponse(HttpServletResponse response, JSONObject jsonObject) throws IOException {
		String body = jsonObject.toString();
		response.setHeader("Content-Length", String.valueOf(body.length()));
		response.setContentType("application/json");
		response.setHeader("Access-Control-Allow-Origin", "*");
		PrintWriter writer = response.getWriter();
		writer.write(body);
		writer.flush();
	}
}