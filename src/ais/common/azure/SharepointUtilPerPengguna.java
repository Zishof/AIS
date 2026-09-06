package ais.common.azure;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.common.PesanFormalHelper;

import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Create;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.SharepointCode;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Adapter layanan eksternal/per-pengguna untuk sharepoint util per pengguna. Tipe ini membungkus
 * autentikasi, client API, dan mapping data layanan tersebut agar detail integrasi tidak disalin
 * ke action pemanggil.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String DRIVE_PATH}, {@code String
 * APPLICATION_NAME}, {@code FileDataStoreFactory dataStoreFactory}, {@code HttpTransport httpTransport}, {@code
 * JsonFactory JSON_FACTORY}, {@code String username}, {@code Drive drive}, {@code java.io.File file}; mutasi
 * data ({@code prosesBackup()}, {@code prosesBackup()}); operasi domain lain ({@code kirimBackupLangsung()},
 * {@code kirimBackupLangsung()}, {@code displayLink()}); konfigurasi constructor: {@code dataStoreFactory},
 * {@code file}, {@code httpTransport}, {@code lokasi}, {@code username}. Bagian lain dari kontrak tetap
 * mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> operasi dapat membaca kredensial per pengguna, melakukan I/O jaringan, menyegarkan
 * token, atau memetakan data remote. Jangan membagikan client/token antar pengguna; gunakan adapter ini sebagai
 * satu batas integrasi dan tangani kegagalan layanan luar.</p>
 */
public class SharepointUtilPerPengguna {
	
	
	 static String DRIVE_PATH = "https://api.onedrive.com/v1.0/drive/root:/";

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
	// java.io.File("/opt/sharepoint_temp");

	private String username;

	private Drive drive = null;

	private java.io.File file;

	public SharepointUtilPerPengguna(Tbmuser tbmuser) {
		try {
			username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();
			String clientId = Common.getKonfigurasi("id_aplikasi_azure", "id_aplikasi_azure").getNilai();
			
			String lokasi = ConstantValues.lokasiFileTemproraryTemp;

			if (lokasi.endsWith("/")) {
				lokasi += username + "_" + clientId;
			} else {
				lokasi += "/" + username + "_" + clientId;
			}

			System.out.println("Simpan ke lokasi -> " + lokasi);

			file = new java.io.File(lokasi);
			file.mkdirs();

			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			dataStoreFactory = new FileDataStoreFactory(file);

		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace(); ais.common.ErrorAuditUtil.record(t, "auto-audit src/ais/common/azure/SharepointUtilPerPengguna.java:117");
		}
	}


	private File fileFolder = null;
	private Map<String, File> fileFolder2 = new HashMap<String, File>();
	private Map<String, File> fileFolder3 = new HashMap<String, File>();

	public File kirimBackupLangsung(final Label label, final java.io.File file, PerguruanTinggi perguruanTinggi,
			String folderName, final EventListener eventListener) {
		return kirimBackupLangsung(label, file, perguruanTinggi, folderName, null, eventListener);
	}

	public File kirimBackupLangsung(final Label label, final java.io.File file, PerguruanTinggi perguruanTinggi,
			String folderName, String folderNameLagi, final EventListener eventListener) {
		if (folderName == null || folderName.trim().isEmpty()) {
			folderName = "File Pembelajaran";
		} else {
			folderName = folderName.replaceAll("[^a-zA-Z0-9\\s]", "");
		}
		File fileUpload = null;
		try {



			if (fileFolder == null) {
				String namaPt = perguruanTinggi.getNama();
				namaPt = namaPt.replaceAll("[^a-zA-Z0-9\\s]", "");
				Files.List request = drive.files().list().setQ(
						"mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + namaPt + "'");
				FileList files = request.execute();
				List<File> folders = files.getFiles();

				if (folders.isEmpty()) {
					File fileMetadata = new File();
					fileMetadata.setName(namaPt);
					fileMetadata.setMimeType("application/vnd.google-apps.folder");

					fileFolder = drive.files().create(fileMetadata).setFields("id").execute();
				} else {
					fileFolder = folders.get(0);
				}
			}
			File folder = null;
			File folder2 = null;
			if (!fileFolder2.keySet().contains(folderName)) {
				Files.List request = drive.files().list()
						.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and name='" + folderName
								+ "' and '" + fileFolder.getId() + "' in parents");
				FileList files = request.execute();
				List<File> folders = files.getFiles();

				if (folders.isEmpty()) {
					File fileMetadata = new File();
					fileMetadata.setName(folderName);
					fileMetadata.setParents(Collections.singletonList(fileFolder.getId()));
					fileMetadata.setMimeType("application/vnd.google-apps.folder");

					folder = drive.files().create(fileMetadata).setFields("id").execute();
					fileFolder2.put(folderName, folder);
				} else {
					folder = folders.get(0);
					fileFolder2.put(folderName, folder);
				}
			} else {
				folder = fileFolder2.get(folderName);
			}

			if (folderNameLagi != null && !folderNameLagi.trim().isEmpty()) {
				folderNameLagi = folderNameLagi.replaceAll("[^a-zA-Z0-9\\s]", "");
				if (!fileFolder3.keySet().contains(folderNameLagi)) {
					Files.List request = drive.files().list()
							.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and name='"
									+ folderNameLagi + "' and '" + folder.getId() + "' in parents");
					FileList files = request.execute();
					List<File> folders = files.getFiles();

					if (folders.isEmpty()) {
						File fileMetadata = new File();
						fileMetadata.setName(folderNameLagi);
						fileMetadata.setParents(Collections.singletonList(folder.getId()));
						fileMetadata.setMimeType("application/vnd.google-apps.folder");

						folder2 = drive.files().create(fileMetadata).setFields("id").execute();
						fileFolder3.put(folderNameLagi, folder2);
					} else {
						folder2 = folders.get(0);
						fileFolder3.put(folderNameLagi, folder2);
					}
				} else {
					folder2 = fileFolder3.get(folderNameLagi);
				}
			}

			com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();

			fileMetadata.setName(file.getName());

			String mime = CommonMedia.getMime(file);

			InputStreamContent mediaContent = new InputStreamContent(mime,
					new BufferedInputStream(new FileInputStream(file)));
			mediaContent.setLength(file.length());
			fileMetadata.setParents(folder2 == null ? Collections.singletonList(folder.getId())
					: Collections.singletonList(folder2.getId()));

			Create insert = drive.files().create(fileMetadata, mediaContent);
			System.out.println("insert => " + insert);
			MediaHttpUploader uploader = insert.getMediaHttpUploader();
			uploader.setDirectUploadEnabled(true);
			uploader.setProgressListener(new MediaHttpUploaderProgressListener() {

				@SuppressWarnings("incomplete-switch")
				@Override
				public void progressChanged(MediaHttpUploader uploader) throws IOException {
					// TODO Auto-generated method stub

					switch (uploader.getUploadState()) {
					case INITIATION_STARTED:
						System.out.println("Upload Initiation has started.");
						if (label != null)
							label.setValue("Upload Initiation has started.");
						break;
					case INITIATION_COMPLETE:
						System.out.println("Upload Initiation is Complete.");
						if (label != null)
							label.setValue("Upload Initiation is Complete.");
						break;
					case MEDIA_IN_PROGRESS:
						System.out.println(
								"Upload is In Progress: " + Common.numberFormat.get().format(uploader.getProgress()));
						if (label != null)
							label.setValue("Sedang mengupload ke google drive.. harap tunggu..");
						break;
					case MEDIA_COMPLETE:
						System.out.println("Upload is Complete!");
						if (label != null)
							label.setValue("");

						file.delete();
						// System.out.println("hapus file " + file.getAbsolutePath() + " status " +
						// hapus);
						break;
					}

				}
			});

			try {
				fileUpload = insert.execute();
			} catch (Exception e) {

				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				String err = errors.toString();

				if (label != null && (err.toLowerCase().contains("limit")) || err.toLowerCase().contains("usagelimits"))
					label.setValue("error_kirim");
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException("upload berkas ke penyimpanan awan (SharePoint/Drive)", e,
						new String[] {
								"Kemungkinan kuota penyimpanan awan yang digunakan telah mencapai batas (limit/usage limits).",
								"Hubungi Administrator untuk memeriksa/menambah kuota penyimpanan, lalu ulangi proses upload." });
			}

		

			if (eventListener != null)
				eventListener.onEvent(new Event(fileUpload.getId(), null, fileUpload));

		} catch (Exception e) {
			if (label != null)
				label.setValue("Error");
			// TODO Auto-generated catch block
//			Common.tampilErrorJikaAdmin(e);
		}
		return fileUpload;
	}

	public void displayLink(final EventListener eventListener) throws Exception {
		GoogleCommon.codesAzure.remove(username);
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
				"Anda belum terhubung ke sharepoint, untuk menghubungkan, klik tombol berikut :");

		MyButtonConfig a = new MyButtonConfig("Hubungkan ke Sharepoint sekarang");
		a.setStyle("font-size:14px;font-weight: bolder;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				GoogleCommon.codesAzure.remove(username);

				String clientId = Common.getKonfigurasi("id_aplikasi_azure", "id_aplikasi_azure").getNilai();
				String clientSecret = Common
						.getKonfigurasi("kunci_rahasia_aplikasi_azure", "kunci_rahasia_aplikasi_azure").getNilai();
				OAuth20Service service = new ServiceBuilder(clientId).apiSecret(clientSecret)
						.defaultScope("openid User.Read")
						.callback(Common.getRequestHostWithProtocol() + "/azure.jsp?u=" + username)
						.build(MicrosoftAzureActiveDirectory20Api.instance());

				Map<String, String> additionalParams = new HashMap<String, String>();
				additionalParams.put("approval_prompt", "auto");
				additionalParams.put("access_type", "online");
				String authorizationUrl = service.getAuthorizationUrl(additionalParams);
				System.out.println("Got the Authorization URL!");
				System.out.println("Now go and authorize ScribeJava here:");
				System.out.println(authorizationUrl);

				Clients.evalJavaScript("popupCenter({url: '" + authorizationUrl
						+ "', title: 'Hubungkan ke Sharepoint', w: 500, h: 500});");
			}
		});

		final Timer timer = new Timer(500);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				if (GoogleCommon.codesAzure.containsKey(username)) {
					String kode = GoogleCommon.codesAzure.get(username);
					SharepointCode sharepointCode;
					try {

						Session session = HibernateUtil.currentNativeSession();
						sharepointCode = (SharepointCode) session.createCriteria(SharepointCode.class)
								.add(Restrictions.eq("nama", username)).setMaxResults(1).uniqueResult();
						if (sharepointCode == null) {
							sharepointCode = new SharepointCode();
						}
						sharepointCode.setNama(username);
						// Keamanan: authorization code OAuth Azure AD TIDAK disimpan mentah.
						// Field ini tidak pernah dibaca kembali di manapun untuk ditukar
						// menjadi access/refresh token (fitur yatim), jadi menyimpannya
						// hanya menambah risiko kebocoran tanpa manfaat fungsional.
						// Dikosongkan eksplisit agar nilai lama pun ikut terhapus saat
						// baris ini disentuh ulang.
						sharepointCode.setKeterangan(null);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session, (sharepointCode));
						session.getTransaction().commit();

						HibernateUtil.closeSession();
					} catch (Exception e) {
						HibernateUtil.rollbackTransaction();
					}
					GoogleCommon.codesAzure.remove(username);
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

		new Thread(new Runnable() {

			@Override
			public void run() {
				kirimBackupLangsung(label, file, perguruanTinggi, folderName, folderNameLagi, eventListener);
			}
		}).start();
	}

}
