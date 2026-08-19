package ais.common.gdrive;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GDriveCode;
import ais.database.model.Konfigurasi;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class GDriveUtilPerPengguna {

	/**
	 * Be sure to specify the name of your application. If the application name is
	 * {@code null} or blank, the application will log a warning. Suggested format
	 * is "MyCompany-ProductName/1.0".
	 */
	public final String APPLICATION_NAME = "Siakad";

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to make
	 * it a single globally shared instance across your application.
	 */
	public FileDataStoreFactory dataStoreFactory;

	/** Global instance of the HTTP transport. */
	public HttpTransport httpTransport;

	/** Global instance of the JSON factory. */
	public final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Directory to store user credentials. */
	// public final java.io.File DATA_STORE_DIR = new
	// java.io.File("/opt/gdrive_temp");

	private String username;

	private Drive drive = null;

	private java.io.File file;

	public GDriveUtilPerPengguna(Tbmuser tbmuser) {
		try {
			username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();

			String lokasi = ConstantValues.lokasiFileTemproraryTemp;

			if (lokasi.endsWith("/")) {
				lokasi += username + "_" + GoogleCommon.getGoogle_drive_client_id();
			} else {
				lokasi += "/" + username + "_" + GoogleCommon.getGoogle_drive_client_id();
			}

			System.out.println("Simpan ke lokasi -> " + lokasi);

			file = new java.io.File(lokasi);
			file.mkdirs();

			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(file);

		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/common/gdrive/GDriveUtilPerPengguna.java:123");
		}
	}

	// Public a Google File/Folder.
	public Permission createPublicPermission(Drive driveService, String googleFileId) throws Exception {
		// All values: user - group - domain - anyone
		String permissionType = "anyone";
		// All values: organizer - owner - writer - commenter - reader
		String permissionRole = "reader";

		Permission newPermission = new Permission();
		newPermission.setType(permissionType);
		newPermission.setRole(permissionRole);

		return driveService.permissions().create(googleFileId, newPermission).execute();
	}

	/**
	 * Downloads a file using either resumable or direct media download.
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private java.io.File downloadFile(String fileId, String name) throws Exception {

		if (Common.bolehKonfigurasi("download_google_drive_otomatis", Konfigurasi.TIDAK_AKTIF)) {
			Drive drive = getDrive();

			java.io.File parentDir = new java.io.File(Common.REAL_PATH + "/media/");
			java.io.File file = new java.io.File(
					parentDir.getAbsolutePath() + "/" + URLEncoder.encode(fileId, "UTF-8") + "_" + name);
			if (file == null || !file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
				FileOutputStream outputStream = new FileOutputStream(file);
				drive.files().get(fileId).executeMediaAndDownloadTo(outputStream);
				System.out.println("Download file dari google drive " + file.getAbsolutePath());
			} else {
				System.out.println("file sudah ada, nggak jadi download dari google drive " + file.getAbsolutePath());
			}
			return file;
		} else {
			return null;
		}
	}

	/**
	 * Ambil refresh token Google Drive milik {@code username} dari tabel {@code gdrive_credential}
	 * (dibagi antar node). Mengembalikan {@code null} bila belum ada. openSession() ditutup di finally.
	 */
	public static String ambilRefreshToken(String username) {
		if (username == null) {
			return null;
		}
		Session session = HibernateUtil.openSession();
		try {
			ais.database.model.GDriveCredential cred = (ais.database.model.GDriveCredential) session
					.createCriteria(ais.database.model.GDriveCredential.class).add(Restrictions.eq("nama", username))
					.setMaxResults(1).uniqueResult();
			return cred != null ? cred.getRefreshToken() : null;
		} catch (Exception e) {
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Simpan/perbarui refresh token {@code username} ke tabel {@code gdrive_credential} agar bisa
	 * dipakai SEMUA node. Bila {@code refreshToken} null/kosong, TIDAK berbuat apa-apa (Google hanya
	 * mengirim refresh token saat consent pertama; jangan menimpa yang lama dengan null). openSession()
	 * ditutup di finally.
	 */
	public static void simpanRefreshToken(String username, String refreshToken) {
		if (username == null || refreshToken == null || refreshToken.trim().length() == 0) {
			return;
		}
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			ais.database.model.GDriveCredential cred = (ais.database.model.GDriveCredential) session
					.createCriteria(ais.database.model.GDriveCredential.class).add(Restrictions.eq("nama", username))
					.setMaxResults(1).uniqueResult();
			if (cred == null) {
				cred = new ais.database.model.GDriveCredential();
				cred.setNama(username);
			}
			cred.setRefreshToken(refreshToken.trim());
			// Token baru berhasil disimpan -> kredensial kembali berlaku, hapus tanda "butuh
			// otorisasi ulang" (bila sebelumnya sempat ditandai oleh tandaiCredentialButuhOtorisasiUlang).
			cred.setButuhOtorisasiUlang(Boolean.FALSE);
			session.saveOrUpdate(cred);
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:219");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus refresh token {@code username} dari tabel {@code gdrive_credential}. Dipanggil saat refresh
	 * token tak berlaku (dicabut/kadaluarsa) atau saat Reset, agar tidak dipakai berulang. openSession()
	 * ditutup di finally.
	 */
	public static void hapusRefreshToken(String username) {
		if (username == null) {
			return;
		}
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			java.util.List<?> daftar = session.createCriteria(ais.database.model.GDriveCredential.class)
					.add(Restrictions.eq("nama", username)).list();
			for (int i = 0; i < daftar.size(); i++) {
				session.delete(daftar.get(i));
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:250");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Tandai kredensial GDrive milik {@code username} SEDANG BUTUH OTORISASI ULANG: refresh token
	 * Google ditolak ({@code invalid_grant}) atau panggilan Drive API ditolak 401 Unauthorized.
	 * Refresh token lama DIBUANG (null) supaya TIDAK dicoba lagi apa adanya di setiap run backup
	 * terjadwal berikutnya -- inilah penyebab utama error invalid_grant/401 yang sama muncul
	 * berulang-ulang di log tiap kali scheduler jalan, padahal kredensialnya sudah pasti mati.
	 * Baris di {@code gdrive_credential} SENGAJA DIPERTAHANKAN (bukan dihapus seperti
	 * {@link #hapusRefreshToken(String)}) agar operator bisa query siapa saja user yang butuh klik
	 * "Hubungkan ke Drive" lagi. openSession() ditutup di finally.
	 */
	public static void tandaiCredentialButuhOtorisasiUlang(String username, String alasan) {
		if (username == null) {
			return;
		}
		System.err.println("GDrive: kredensial user '" + username + "' butuh otorisasi ulang (" + alasan
				+ "); backup otomatis akan gagal terus sampai user menghubungkan ulang akun Google Drive-nya.");
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			ais.database.model.GDriveCredential cred = (ais.database.model.GDriveCredential) session
					.createCriteria(ais.database.model.GDriveCredential.class).add(Restrictions.eq("nama", username))
					.setMaxResults(1).uniqueResult();
			if (cred == null) {
				cred = new ais.database.model.GDriveCredential();
				cred.setNama(username);
			}
			cred.setRefreshToken(null);
			cred.setButuhOtorisasiUlang(Boolean.TRUE);
			session.saveOrUpdate(cred);
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:tandaiCredentialButuhOtorisasiUlang");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	private Drive getDrive() throws Exception {
		if (drive == null) {

			GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
					new StringReader(GoogleCommon.getGoogle_drive_key()));

			Credential credential = null;

			// (1) MULTI-NODE: pakai refresh token tersimpan di DB (dibagi antar node) bila ada & masih
			// berlaku. Dengan ini node yang TIDAK pernah menukar code tetap bisa upload tanpa minta
			// otorisasi ulang -> menutup celah FileDataStore yang tersimpan LOKAL per node.
			String refreshTokenTersimpan = ambilRefreshToken(username);
			if (refreshTokenTersimpan != null && refreshTokenTersimpan.trim().length() > 0) {
				com.google.api.client.googleapis.auth.oauth2.GoogleCredential gc = new com.google.api.client.googleapis.auth.oauth2.GoogleCredential.Builder()
						.setTransport(httpTransport).setJsonFactory(JSON_FACTORY)
						.setClientSecrets(clientSecrets.getDetails().getClientId(),
								clientSecrets.getDetails().getClientSecret())
						.build();
				gc.setRefreshToken(refreshTokenTersimpan.trim());
				boolean berhasilRefresh = false;
				try {
					// Ambil access token baru dari refresh token; false/exception = token tak berlaku.
					berhasilRefresh = gc.refreshToken();
				} catch (Exception e) {
					berhasilRefresh = false;
				}
				if (berhasilRefresh) {
					credential = gc;
				} else {
					// Refresh token dicabut/kadaluarsa (invalid_grant) -> tandai user butuh otorisasi ulang
					// & buang token lama, supaya TIDAK dicoba lagi apa adanya di run berikutnya (dulu hanya
					// dihapus via hapusRefreshToken tanpa jejak status, sehingga tidak ada cara bagi
					// operator untuk tahu siapa yang perlu hubungkan ulang tanpa membaca error log).
					tandaiCredentialButuhOtorisasiUlang(username, "refresh token ditolak Google (invalid_grant/expired)");
				}
			}

			// (2) Belum ada / refresh token tak berlaku: jalur otorisasi lama (FileDataStore + code dari
			// gdrive_code). Perilaku lama dipertahankan penuh.
			if (credential == null) {
				try {
					credential = buatCredentialDariCode(clientSecrets);
				} catch (IOException e) {
					if (!merupakanDataStoreKorup(e)) {
						throw e;
					}
					pulihkanDataStoreLokal(e);
					credential = buatCredentialDariCode(clientSecrets);
				}

				// Persist refresh token ke DB agar SEMUA node bisa memakainya tanpa otorisasi ulang.
				simpanRefreshToken(username, credential.getRefreshToken());

				credential.setExpiresInSeconds(100000000L);
			}

			drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME)
					.build();
		}
		return drive;
	}

	private Credential buatCredentialDariCode(final GoogleClientSecrets clientSecrets) throws Exception {
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE)).setDataStoreFactory(dataStoreFactory)
				.setAccessType("offline").build();

		return new AuthorizationCodeInstalledApp(flow, new VerificationCodeReceiver() {

			@Override
			public String waitForCode() throws IOException {

				/* Perbaikan kebocoran session: DULU closeSession() dipanggil lurus setelah query.
				 * Bila query gagal (koneksi c3p0 mati / pool habis saat backup terjadwal berjalan
				 * di Thread background), session hasil currentNativeSession() TIDAK pernah ditutup
				 * dan tetap menempel di ThreadLocal thread backup yang berumur panjang -> koneksi
				 * bocor permanen. Sesuai kontrak HibernateUtil: currentNativeSession() WAJIB ditutup
				 * di finally dengan closeSession() (clear + disconnect + close). */
				GDriveCode gdriveCode = null;
				try {
					Session session = HibernateUtil.currentNativeSession();
					gdriveCode = (GDriveCode) session.createCriteria(GDriveCode.class)
							.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
				} finally {
					HibernateUtil.closeSession();
				}

				System.out.println("gdriveCode -> " + gdriveCode);

				if (gdriveCode == null || gdriveCode.getKeterangan() == null
						|| gdriveCode.getKeterangan().trim().length() == 0) {
					// Tidak ada kode otorisasi BARU tersimpan untuk username ini (belum pernah / belum
					// baru saja klik "Hubungkan ke Drive"). DULU di sini jatuh ke kode contoh yang
					// di-hardcode ("4/N-D27v1qgeomdHvvJdmgcCq6NfugLlRfXhTY3LRf_tc") -> kode single-use
					// itu SELALU ditolak Google dengan 400 invalid_grant (sudah lama kadaluarsa/dipakai),
					// dan karena ini dipanggil dari Thread backup terjadwal (tanpa user interaktif),
					// kegagalan itu terjadi lagi di SETIAP run -> sumber utama error invalid_grant yang
					// berulang di log. Gagal cepat secara LOKAL (tanpa memanggil Google sama sekali) dan
					// tandai user butuh otorisasi ulang, alih-alih memaksa exchange yang pasti gagal.
					tandaiCredentialButuhOtorisasiUlang(username,
							"tidak ada kode otorisasi Google Drive tersimpan (belum/belum ulang menghubungkan akun)");
					throw new IOException("GDrive belum terhubung untuk user '" + username
							+ "': tidak ada kode otorisasi Google yang valid tersimpan. User harus klik "
							+ "'Hubungkan ke Drive' terlebih dahulu sebelum backup otomatis bisa berjalan.");
				}

				return gdriveCode.getKeterangan();
			}

			@Override
			public void stop() throws IOException {

			}

			@Override
			public String getRedirectUri() throws IOException {
				// TODO Auto-generated method stub
				return GoogleCommon.getRedirect_url_drive();
			}
		}).authorize("user");
	}

	private boolean merupakanDataStoreKorup(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof EOFException || t instanceof StreamCorruptedException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private void pulihkanDataStoreLokal(Throwable penyebab) throws IOException {
		System.err.println("GDrive: cache credential lokal user '" + username
				+ "' korup/kosong, cache lokal akan dibuat ulang. Penyebab: " + penyebab.getClass().getName()
				+ " - " + penyebab.getMessage());
		ais.common.ErrorAuditUtil.record(penyebab,
				"auto-audit src/ais/common/gdrive/GDriveUtilPerPengguna.java:getDrive - recovery FileDataStore korup");

		drive = null;
		if (file != null && file.exists()) {
			FileUtils.deleteDirectory(file);
		}
		if (file != null && !file.exists()) {
			file.mkdirs();
		}
		if (file == null || !file.isDirectory()) {
			throw new IOException("Gagal membuat ulang folder cache Google Drive untuk user '" + username + "'");
		}
		dataStoreFactory = new FileDataStoreFactory(file);
	}

	private File fileFolder = null;
	private Map<String, File> fileFolder2 = new HashMap<String, File>();
	private Map<String, File> fileFolder3 = new HashMap<String, File>();

	public File kirimBackupLangsung(Label label, final java.io.File file, PerguruanTinggi perguruanTinggi,
			String folderName, EventListener eventListener) {
		return kirimBackupLangsung(label, file, perguruanTinggi, folderName, null, eventListener);
	}

	public com.google.api.services.drive.model.File kirimBackupLangsung(final Label label, final java.io.File file,
			PerguruanTinggi perguruanTinggi, String folderName, String folderNameLagi,
			final EventListener eventListener) {

		// 1. Sanitasi Nama Folder
		folderName = (folderName == null || folderName.trim().isEmpty()) ? "File Pembelajaran"
				: folderName.replaceAll("[^a-zA-Z0-9\\s]", "");

		com.google.api.services.drive.model.File fileUpload = null;

		try {
			updateStatus(label, "Menghubungkan ke Google Drive...");
			Drive drive = getDrive();

			// 2. Folder Root (Nama Perguruan Tinggi)
			if (fileFolder == null) {
				String namaPt = perguruanTinggi.getNama().replaceAll("[^a-zA-Z0-9\\s]", "");
				updateStatus(label, "Menyiapkan folder utama: " + namaPt);
				fileFolder = getOrCreateDriveFolder(drive, namaPt, null);
			}

			// 3. Folder Level 1
			com.google.api.services.drive.model.File folder = fileFolder2.get(folderName);
			if (folder == null) {
				updateStatus(label, "Menyiapkan direktori: " + folderName);
				folder = getOrCreateDriveFolder(drive, folderName, fileFolder.getId());
				fileFolder2.put(folderName, folder);
			}

			// 4. Folder Level 2 (Sub-folder)
			com.google.api.services.drive.model.File folder2 = null;
			if (folderNameLagi != null && !folderNameLagi.trim().isEmpty()) {
				folderNameLagi = folderNameLagi.replaceAll("[^a-zA-Z0-9\\s]", "");
				folder2 = fileFolder3.get(folderNameLagi);

				if (folder2 == null) {
					updateStatus(label, "Menyiapkan sub-direktori: " + folderNameLagi);
					folder2 = getOrCreateDriveFolder(drive, folderNameLagi, folder.getId());
					fileFolder3.put(folderNameLagi, folder2);
				}
			}

			// Tentukan Parent ID final tempat file akan diletakkan
			String targetParentId = (folder2 == null) ? folder.getId() : folder2.getId();

			// 5. Persiapan Metadata File
			updateStatus(label, "Menyiapkan upload file: " + file.getName());
			com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
			fileMetadata.setName(file.getName());
			fileMetadata.setParents(Collections.singletonList(targetParentId));

			String mime = CommonMedia.getMime(file);

			// 6. Eksekusi Upload. Java 7 style: stream ditutup eksplisit di finally.
			FileInputStream fis = null;
			BufferedInputStream bis = null;
			try {
				fis = new FileInputStream(file);
				bis = new BufferedInputStream(fis);
				InputStreamContent mediaContent = new InputStreamContent(mime, bis);
				mediaContent.setLength(file.length());

				Drive.Files.Create insert = drive.files().create(fileMetadata, mediaContent);
				MediaHttpUploader uploader = insert.getMediaHttpUploader();
				uploader.setDirectUploadEnabled(true);
				uploader.setProgressListener(new MediaHttpUploaderProgressListener() {

					@Override
					public void progressChanged(MediaHttpUploader uploader) throws IOException {
						switch (uploader.getUploadState()) {
						case INITIATION_STARTED:
							updateStatus(label, "Memulai inisiasi upload...");
							break;
						case INITIATION_COMPLETE:
							updateStatus(label, "Inisiasi selesai, bersiap mengunggah...");
							break;
						case MEDIA_IN_PROGRESS:
							// Format progress menjadi persentase bulat (misal: 45%)
							int persen = (int) Math.round(uploader.getProgress() * 100);
							updateStatus(label, "Sedang mengunggah: " + persen + "%");
							break;
						case MEDIA_COMPLETE:
							updateStatus(label, "Upload 100% selesai!");
							break;
						case NOT_STARTED:
							break;
						default:
							break;
						}
					}
				});

				fileUpload = insert.execute();
			} catch (Exception e) {
				// e.printStackTrace();
				System.err.println("Peringatan: - " + e.getMessage());
			} finally {
				closeQuietly(bis);
				closeQuietly(fis);
			}

			// PENTING: bila insert.execute() di atas gagal (exception ditelan hanya sebagai
			// peringatan, lihat komentar di atas), fileUpload TETAP null di sini. Sebelumnya
			// kode di bawah tetap lanjut seolah upload sukses -> mencatat "Berhasil mengirim
			// file", MENGHAPUS file lokal (padahal belum pernah benar-benar ter-upload ke
			// GDrive, jadi backup-nya HILANG baik lokal maupun remote), lalu NPE saat
			// fileUpload.getId() dipanggil. Deteksi kegagalan di sini: hentikan proses untuk
			// file ini SEBELUM langkah 7/8, biarkan file lokal utuh (bisa dicoba lagi di run
			// berikutnya), dan jangan panggil eventListener seolah sukses.
			if (fileUpload == null) {
				System.err.println("Peringatan: Upload GDrive gagal (lihat pesan di atas) untuk file: "
						+ file.getAbsolutePath() + " - file lokal TIDAK dihapus agar bisa dicoba lagi.");
				if (label != null) {
					label.setValue("Error");
				}
				return null;
			}

			// 7. Set Hak Akses Public
			try {
				updateStatus(label, "Menerapkan hak akses publik...");
				createPublicPermission(drive, fileUpload.getId());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/gdrive/GDriveUtilPerPengguna.java:453");
				System.err.println("Peringatan: Gagal set permission public - " + e.getMessage());
				// Lanjut, tidak perlu menghentikan proses jika hanya gagal permission
			}

			// 8. Sukses & Cleanup
			System.out.println("Berhasil mengirim file: " + file.getAbsolutePath());
			if (label != null)
				label.setValue(""); // Kosongkan label sesuai logika awal Anda saat sukses

			if (file.exists()) {
				file.delete();
			}

			if (eventListener != null) {
				eventListener.onEvent(new Event(fileUpload.getId(), null, fileUpload));
			}

		} catch (GoogleJsonResponseException e) {
			// mis. GET drive/v3/files -> 401 Unauthorized: kredensial (biasanya cache lokal per-node di
			// FileDataStore) sudah tak valid lagi, tapi baru ketahuan saat panggilan Drive API sebenarnya
			// (getDrive() sendiri tidak proaktif memvalidasi kredensial lokal). Tandai user butuh otorisasi
			// ulang supaya bukan cuma pesan generik "Error" yang sulit ditindaklanjuti operator, dan supaya
			// scheduled run berikutnya tidak diam-diam mencoba kredensial yang sudah pasti mati lagi.
			if (label != null) label.setValue("Error");
			boolean butuhOtorisasiUlang = e.getStatusCode() == 401;
			if (butuhOtorisasiUlang) {
				tandaiCredentialButuhOtorisasiUlang(username,
						"Drive API menolak 401 Unauthorized (kredensial lokal tersimpan sudah tak valid)");
				// NB: variabel lokal "drive" di method ini menutupi (shadow) field this.drive -> harus
				// eksplisit this.drive supaya cache Drive/kredensial di instance ini benar-benar dibuang,
				// jangan sampai dipakai ulang untuk file berikutnya dalam loop backup yang sama.
				this.drive = null;
			}
			String pesan = "Backup GDrive gagal untuk user '" + username + "' (folder: " + folderName + ", file: "
					+ file.getName() + "): Drive API menolak (HTTP " + e.getStatusCode() + ")"
					+ (butuhOtorisasiUlang ? " - user perlu klik 'Hubungkan ke Drive' lagi." : ".");
			System.err.println(pesan);
			/* 401 setelah token ditandai revoked adalah status akun yang perlu tindakan
			 * pengguna, bukan error aplikasi berulang per file. Pesan dan flag reauth di
			 * atas tetap dipertahankan. */
		} catch (TokenResponseException e) {
			// mis. POST oauth2.googleapis.com/token -> 400 invalid_grant: refresh token/kode otorisasi
			// ditolak Google. Tandai user butuh otorisasi ulang supaya scheduler tidak diam-diam terus
			// mencoba kredensial yang sudah pasti mati di setiap run backup berikutnya.
			if (label != null) label.setValue("Error");
			tandaiCredentialButuhOtorisasiUlang(username, "OAuth token ditolak Google (" + e.getMessage() + ")");
			this.drive = null;
			String pesan = "Backup GDrive gagal untuk user '" + username + "' (folder: " + folderName + ", file: "
					+ file.getName() + "): refresh token/kode otorisasi Google Drive sudah dicabut/kadaluarsa - "
					+ "user perlu klik 'Hubungkan ke Drive' lagi.";
			System.err.println(pesan);
		} catch (Exception e) {
			if (label != null) label.setValue("Error");

			if (e instanceof IOException && e.getMessage() != null
					&& e.getMessage().startsWith("GDrive belum terhubung")) {
				System.err.println(e.getMessage());
				return null;
			}

			if (isTokenStoreCorrupted(e)) {
				// EOFException/StreamCorruptedException/ClassNotFoundException dari
				// IOUtils.deserialize() saat FileDataStoreFactory membaca berkas StoredCredential lokal
				// yang terpotong/rusak (mis. proses ke-stop di tengah write, disk penuh). Berbeda dari
				// GoogleJsonResponseException(401)/TokenResponseException di atas: kegagalan ini
				// PERMANEN pada berkas lokal itu sendiri, bukan pada token-nya -- tanpa dibersihkan,
				// SETIAP run backup berikutnya (dijadwalkan/manual) akan gagal identik selamanya karena
				// dataStoreFactory tetap menunjuk ke berkas yang sama. Sebelumnya jatuh ke cabang ini
				// dan hanya dicatat/di-print, tanpa perbaikan apa pun -- persis pola tombol "Reset"
				// manual di displayLink(), diterapkan otomatis di sini.
				resetTokenStoreLocalQuietly();
				tandaiCredentialButuhOtorisasiUlang(username,
						"berkas kredensial Google Drive lokal rusak/terpotong (" + e.getClass().getSimpleName()
								+ ") - sudah dibersihkan otomatis, user perlu klik 'Hubungkan ke Drive' lagi");
				String pesan = "Backup GDrive gagal untuk user '" + username + "' (folder: " + folderName
						+ ", file: " + file.getName()
						+ "): berkas kredensial Google Drive lokal rusak/terpotong dan sudah dibersihkan otomatis - "
						+ "user perlu klik 'Hubungkan ke Drive' lagi.";
				System.err.println(pesan);
				ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/gdrive/GDriveUtilPerPengguna.java:474 - " + pesan);
				return null;
			}

			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/gdrive/GDriveUtilPerPengguna.java:474");
		}

		return fileUpload;
	}

	/**
	 * Mendeteksi kegagalan deserialize berkas StoredCredential lokal (FileDataStoreFactory) yang
	 * rusak/terpotong -- lihat catatan di kirimBackupLangsung(). Ditelusuri lewat rantai cause karena
	 * Google API client kadang membungkusnya (mis. dalam RuntimeException).
	 */
	private boolean isTokenStoreCorrupted(Throwable e) {
		Throwable t = e;
		while (t != null) {
			if (t instanceof java.io.EOFException || t instanceof java.io.StreamCorruptedException
					|| t instanceof java.io.InvalidClassException || t instanceof ClassNotFoundException
					|| t instanceof java.io.OptionalDataException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Menghapus & membuat ulang folder cache token Drive lokal punya user ini (persis logika tombol
	 * "Reset" manual di displayLink(), tanpa dialog UI) supaya percobaan backup berikutnya tidak
	 * mengulang deserialize berkas yang sama-sama rusak. Best-effort: kegagalan dicatat, tidak
	 * dilempar, supaya alur pemanggil (thread backup terjadwal) tetap bisa lanjut ke file berikutnya.
	 */
	private void resetTokenStoreLocalQuietly() {
		try {
			drive = null;
			if (file != null) {
				if (file.exists()) {
					FileUtils.deleteDirectory(file);
				}
				file.mkdirs();
				if (file.isDirectory()) {
					dataStoreFactory = new FileDataStoreFactory(file);
				}
			}
		} catch (Exception ex) {
			ais.common.ErrorAuditUtil.record(ex,
					"GDriveUtilPerPengguna.resetTokenStoreLocalQuietly: gagal membersihkan folder cache token Drive lokal untuk user="
							+ username);
		}
	}

	/**
	 * Helper Method: Untuk mencegah duplikasi kode pembuatan folder berulang kali.
	 * Silakan tambahkan fungsi ini di dalam class yang sama.
	 */
	private com.google.api.services.drive.model.File getOrCreateDriveFolder(Drive drive, String folderName,
			String parentId) throws IOException {
		String query = "mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName + "'";
		if (parentId != null && !parentId.isEmpty()) {
			query += " and '" + parentId + "' in parents";
		}

		FileList files = drive.files().list().setQ(query).execute();
		List<com.google.api.services.drive.model.File> folders = files.getFiles();

		if (folders.isEmpty()) {
			com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
			fileMetadata.setName(folderName);
			fileMetadata.setMimeType("application/vnd.google-apps.folder");
			if (parentId != null && !parentId.isEmpty()) {
				fileMetadata.setParents(Collections.singletonList(parentId));
			}
			return drive.files().create(fileMetadata).setFields("id").execute();
		} else {
			return folders.get(0);
		}
	}

	/**
	 * Helper Method: Untuk memperbarui Label ZK dan nge-print ke Console sekaligus
	 */
	private void updateStatus(Label label, String message) {
		if (label != null) {
			label.setValue(message);
		}
		System.out.println("GDrive Sync: " + message);
	}

	private void closeQuietly(java.io.Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:closeQuietly");
		}
	}

	/**
	 * Simpan Google authorization code milik {@code username} ke tabel {@code gdrive_code} (dilihat
	 * SEMUA node) sekaligus ke map memori. Dipanggil dari {@code accept.jsp}. Callback OAuth bisa
	 * mendarat di node MANA PUN (load-balanced), sehingga code WAJIB dipersistkan ke DB agar timer di
	 * node tempat halaman pengguna berjalan tetap menemukannya (perbaikan multi-node). Memakai
	 * openSession() dan DITUTUP di finally (rollback/disconnect/close) via closeSessionQuietly.
	 */
	public static void simpanCodeDrive(String username, String code) {
		if (username == null || code == null || code.trim().length() == 0) {
			return;
		}
		// Kompat single-node / node yang sama: isi map memori juga.
		GoogleCommon.codes.put(username, code);
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			GDriveCode gdriveCode = (GDriveCode) session.createCriteria(GDriveCode.class)
					.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
			if (gdriveCode == null) {
				gdriveCode = new GDriveCode();
				gdriveCode.setNama(username);
			}
			gdriveCode.setKeterangan(code.trim());
			session.saveOrUpdate(gdriveCode);
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:547");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Hapus code lama {@code username} dari map memori DAN tabel {@code gdrive_code}. Dipanggil di awal
	 * proses koneksi agar timer hanya bereaksi terhadap code BARU (bukan sisa percobaan sebelumnya).
	 */
	public static void hapusCodeDrive(String username) {
		if (username == null) {
			return;
		}
		GoogleCommon.codes.remove(username);
		Session session = HibernateUtil.openSession();
		org.hibernate.Transaction tx = null;
		try {
			tx = session.beginTransaction();
			java.util.List<?> daftar = session.createCriteria(GDriveCode.class)
					.add(Restrictions.eq("nama", username)).list();
			for (int i = 0; i < daftar.size(); i++) {
				session.delete(daftar.get(i));
			}
			tx.commit();
		} catch (Exception e) {
			if (tx != null) {
				try {
					tx.rollback();
				} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/common/gdrive/GDriveUtilPerPengguna.java:578");
				}
			}
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	/**
	 * Ambil code {@code username} bila sudah tersedia: cek map memori (node yang sama, cepat) lalu tabel
	 * {@code gdrive_code} (node lain). Mengembalikan {@code null} bila belum ada. openSession() ditutup
	 * di finally.
	 */
	private static String ambilCodeDriveJikaAda(String username) {
		if (username == null) {
			return null;
		}
		if (GoogleCommon.codes.containsKey(username)) {
			return GoogleCommon.codes.get(username);
		}
		Session session = HibernateUtil.openSession();
		try {
			GDriveCode gdriveCode = (GDriveCode) session.createCriteria(GDriveCode.class)
					.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
			return gdriveCode != null ? gdriveCode.getKeterangan() : null;
		} catch (Exception e) {
			return null;
		} finally {
			HibernateUtil.closeSessionQuietly(session);
		}
	}

	public void displayLink(final EventListener eventListener) throws Exception {
		// Hapus code lama user (map memori + DB gdrive_code) agar timer hanya bereaksi pada code BARU.
		hapusCodeDrive(username);
		final MyWindow window = new MyWindow("Drive", "none", true);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		window.setHeight("300px");
		window.setWidth("400px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Common.initKeteranganSatuKolom(rows,
				"Anda belum terhubung ke google drive, untuk menghubungkan, klik tombol berikut :");

		MyButtonConfig a = new MyButtonConfig("Hubungkan ke Drive sekarang", FileFoto.icon("drive.google"));
		a.setStyle("font-size:14px;font-weight: bolder;");

		String url = "https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id="
				+ GoogleCommon.getGoogle_drive_client_id() + "&redirect_uri=" + GoogleCommon.getRedirect_url_drive()
				+ "&response_type=code&scope=https://www.googleapis.com/auth/drive.file&state="
				+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/accept.jsp?u=" + username, "UTF-8");

		/* popupCenter hanya ada di /js/common.js milik template JSP; di halaman
		 * ZK fungsi itu tidak terdefinisi sehingga klik tombol terasa mati.
		 * Popup juga dibuka lewat widget listener client-side (bukan
		 * evalJavaScript dari server) agar tidak diblokir popup blocker. */
		a.setWidgetListener("onClick",
				"if(typeof window.popupCenter==='undefined'){window.popupCenter=function(o){var w=o.w||500,h=o.h||500;"
						+ "var l=Math.max(0,((window.innerWidth||screen.width)-w)/2+(window.screenLeft||window.screenX||0));"
						+ "var t=Math.max(0,((window.innerHeight||screen.height)-h)/2+(window.screenTop||window.screenY||0));"
						+ "var win=window.open(o.url,'_blank','scrollbars=yes,resizable=yes,width='+w+',height='+h+',top='+t+',left='+l);"
						+ "if(win){try{win.focus();}catch(e){}}return win;};}"
						+ "popupCenter({url: '" + url.replace("'", "\\'") + "', title: 'Hubungkan ke Drive', w: 500, h: 500});");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// Bersihkan code lama (map memori + DB) tepat sebelum popup otorisasi dibuka.
				hapusCodeDrive(username);
			}
		});

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);

		Hbox hbox = new Hbox();
		row.appendChild(hbox);

		hbox.appendChild(a);

		a = new MyButtonConfig("Reset", "/img/Check-icon.png");
		a.setStyle("font-size:14px;font-weight: bolder;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				try {
					// Reset TOTAL: selain FileDataStore lokal, hapus juga refresh token & code di DB
					// (dibagi antar node) agar benar-benar mulai dari nol dan otorisasi ulang bersih.
					hapusRefreshToken(username);
					hapusCodeDrive(username);
					drive = null;

					if (file.exists()) {
						FileUtils.deleteDirectory(file);
					}

					file.delete();
					file.mkdirs();

					// mkdirs() bisa GAGAL diam-diam bila disk server penuh ("No space left on device")
					// atau folder induk tak bisa ditulis; new FileDataStoreFactory(file) lalu melempar
					// IOException "unable to create directory" yang tampil sebagai UiException ke user.
					// Deteksi lebih awal & beri pesan yang bisa ditindaklanjuti (bukan stack trace).
					if (!file.isDirectory()) {
						MyMessageboxConfig.show(
								"Gagal menyiapkan folder cache Drive di server (kemungkinan penyimpanan/disk server penuh). "
										+ "Hubungi admin untuk mengecek kapasitas disk, lalu coba lagi.",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					dataStoreFactory = new FileDataStoreFactory(file);

					MyMessageboxConfig.show("Data drive telah di reset, coba ulangi upload", "Informasi",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									window.setVisible(false);
									window.detach();
								}
							});
				} catch (Exception e) {
					// Jangan lempar stack trace ke UI (mis. disk penuh). Beri pesan yang jelas.
					MyMessageboxConfig.show(
							"Gagal reset data Drive: " + e.getMessage()
									+ ". Kemungkinan penyimpanan/disk server penuh; hubungi admin.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}

			}
		});
		hbox.appendChild(a);

		final Timer timer = new Timer(500);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				// PERBAIKAN MULTI-NODE: callback OAuth (accept.jsp) bisa mendarat di NODE LAIN sehingga
				// code TIDAK pernah muncul di GoogleCommon.codes (map memori per-JVM) node ini -> dulu
				// timer menunggu selamanya ("login OK tapi tetap belum terhubung"). Kini accept.jsp
				// menyimpan code ke tabel gdrive_code (shared antar node), dan timer memeriksanya di DB
				// (plus map memori untuk node yang sama). displayLink sudah menghapus code lama, jadi
				// baris yang muncul = code BARU dari otorisasi ini.
				String kode = ambilCodeDriveJikaAda(username);
				if (kode != null && kode.trim().length() > 0) {
					GoogleCommon.codes.remove(username);
					eventListener.onEvent(event);

					window.detach();
					timer.stop();
					timer.detach();
				}
			}
		});
		timer.start();

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		cancel.setParent(toolbar);

		window.onModal();
	}

	public void prosesBackup(final java.io.File file, final String folderName, final EventListener eventListener) {
		prosesBackup(file, folderName, null, eventListener);
	}

	public void prosesBackup(final java.io.File file, final String folderName, final String folderNameLagi,
			final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim file .."));
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		final PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println("Kirim file " + file.getAbsolutePath() + " telah berhasil");
					file.delete();
					Clients.clearBusy();
					timer.detach();
				} else if (label.getValue().equals("Error")) {
					displayLink(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							prosesBackup(file, folderName, folderNameLagi, eventListener);
						}
					});
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		/* OPTIMASI FASE 5: dulu memakai thread MENTAH (tak berbatas, tak bernama, tidak
		 * berhenti saat webapp redeploy) dan server push yang dinyalakan pemanggil TIDAK
		 * PERNAH dimatikan -- browser terus polling & menahan thread Tomcat selama tab
		 * terbuka. jalankanDenganPush() memakai pool daemon berbatas milik AsyncTaskManager
		 * dan MELEPAS push di finally begitu unggahan selesai/gagal. Progres tetap dipantau
		 * lewat Timer di atas (polling klien, tidak bergantung pada server push). */
		final org.zkoss.zk.ui.Desktop desktopBackup = org.zkoss.zk.ui.Executions.getCurrent() == null
				? null : org.zkoss.zk.ui.Executions.getCurrent().getDesktop();
		ais.common.AsyncTaskManager.jalankanDenganPush(desktopBackup, new Runnable() {

			@Override
			public void run() {
				kirimBackupLangsung(label, file, perguruanTinggi, folderName, folderNameLagi, eventListener);
			}
		});
	}

}
