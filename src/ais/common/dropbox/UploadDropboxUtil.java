package ais.common.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxStandardSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.json.JsonReader;
import com.dropbox.core.util.IOUtil.ProgressListener;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxPathV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.GoogleCommon;
import ais.common.RequestContext;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * An example command-line application that runs through the web-based OAuth
 * flow (using {@link DbxWebAuth}).
 */
public class UploadDropboxUtil {
	// Adjust the chunk size based on your network speed and reliability. Larger
	// chunk sizes will
	// result in fewer network requests, which will be faster. But if an error
	// occurs, the entire
	// chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for
	// your chunk size.
	private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
	private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

	/**
	 * Uploads a file in a single request. This approach is preferred for small
	 * files since it eliminates unnecessary round-trips to the servers.
	 *
	 * @param dbxClient
	 *            Dropbox user authenticated client
	 * @param localFIle
	 *            local file to upload
	 * @param dropboxPath
	 *            Where to upload the file to within Dropbox
	 * @throws Exception
	 */
	private static void uploadFile(DbxClientV2 dbxClient, final File localFile, String dropboxPath, final Label label,
			EventListener eventListener) throws Exception {
		try {
			InputStream in = new FileInputStream(localFile);
			final long size = localFile.length();
			ProgressListener progressListener = new ProgressListener() {

				@Override
				public void onProgress(long arg0) {
					printProgress(arg0, size, label);

				}
			};
			FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath).withMode(WriteMode.ADD)
					.withClientModified(new Date(localFile.lastModified())).uploadAndFinish(in, progressListener);

			System.out.println(metadata.toStringMultiline());

			SharedLinkMetadata sharedLinkMetadata = dbxClient.sharing().createSharedLinkWithSettings(dropboxPath);
			String url = sharedLinkMetadata.getUrl();

			System.out.println(url);

			eventListener.onEvent(new Event("", null, url));
			label.setValue("");

		} catch (UploadErrorException ex) {
			label.setValue("Error");
			System.err.println("Error uploading to Dropbox: " + ex.getMessage());

		} catch (DbxException ex) {
			label.setValue("Error");
			System.err.println("Error uploading to Dropbox: " + ex.getMessage());

		} catch (IOException ex) {
			label.setValue("Error");
			System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());

		}
	}

	/**
	 * Uploads a file in chunks using multiple requests. This approach is
	 * preferred for larger files since it allows for more efficient processing
	 * of the file contents on the server side and also allows partial uploads
	 * to be retried (e.g. network connection problem will not cause you to
	 * re-upload all the bytes).
	 *
	 * @param dbxClient
	 *            Dropbox user authenticated client
	 * @param localFIle
	 *            local file to upload
	 * @param dropboxPath
	 *            Where to upload the file to within Dropbox
	 * @throws Exception
	 */
	private static void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath, final Label label,
			EventListener eventListener) throws Exception {
		final long size = localFile.length();

		// assert our file is at least the chunk upload size. We make this
		// assumption in the code
		// below to simplify the logic.
		if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
			System.err.println("File too small, use upload() instead.");

			return;
		}

		long uploaded = 0L;
		DbxException thrown = null;

		ProgressListener progressListener = new ProgressListener() {
			long uploadedBytes = 0;

			@Override
			public void onProgress(long l) {
				printProgress(l + uploadedBytes, size, label);
				if (l == CHUNKED_UPLOAD_CHUNK_SIZE)
					uploadedBytes += CHUNKED_UPLOAD_CHUNK_SIZE;
			}
		};

		// Chunked uploads have 3 phases, each of which can accept uploaded
		// bytes:
		//
		// (1) Start: initiate the upload and get an upload session ID
		// (2) Append: upload chunks of the file to append to our session
		// (3) Finish: commit the upload and close the session
		//
		// We track how many bytes we uploaded to determine which phase we
		// should be in.
		String sessionId = null;
		for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
			if (i > 0) {
				System.out.printf("Retrying chunked upload (%d / %d attempts)\n", i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS);
			}

			try {
				InputStream in = new FileInputStream(localFile);
				in.skip(uploaded);

				// (1) Start
				if (sessionId == null) {
					sessionId = dbxClient.files().uploadSessionStart()
							.uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE, progressListener).getSessionId();
					uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
					printProgress(uploaded, size, label);
				}

				UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

				// (2) Append
				while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
					dbxClient.files().uploadSessionAppendV2(cursor).uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE,
							progressListener);
					uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
					printProgress(uploaded, size, label);
					cursor = new UploadSessionCursor(sessionId, uploaded);
				}

				// (3) Finish
				long remaining = size - uploaded;
				CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath).withMode(WriteMode.ADD)
						.withClientModified(new Date(localFile.lastModified())).build();
				FileMetadata metadata = dbxClient.files().uploadSessionFinish(cursor, commitInfo).uploadAndFinish(in,
						remaining, progressListener);

				SharedLinkMetadata sharedLinkMetadata = dbxClient.sharing().createSharedLinkWithSettings(dropboxPath);
				String url = sharedLinkMetadata.getUrl();

				System.out.println(url);

				eventListener.onEvent(new Event("", null, url));
				label.setValue("");
				System.out.println(metadata.toStringMultiline());
				return;
			} catch (RetryException ex) {
				label.setValue("Error");
				thrown = ex;
				// RetryExceptions are never automatically retried by the client
				// for uploads. Must
				// catch this exception even if DbxRequestConfig.getMaxRetries()
				// > 0.
				sleepQuietly(ex.getBackoffMillis());
				continue;
			} catch (NetworkIOException ex) {
				label.setValue("Error");
				thrown = ex;
				// network issue with Dropbox (maybe a timeout?) try again
				continue;
			} catch (UploadSessionLookupErrorException ex) {
				label.setValue("Error");
				if (ex.errorValue.isIncorrectOffset()) {
					thrown = ex;
					// server offset into the stream doesn't match our offset
					// (uploaded). Seek to
					// the expected offset according to the server and try
					// again.
					uploaded = ex.errorValue.getIncorrectOffsetValue().getCorrectOffset();
					continue;
				} else {
					// Some other error occurred, give up.
					System.err.println("Error uploading to Dropbox: " + ex.getMessage());

					return;
				}
			} catch (UploadSessionFinishErrorException ex) {
				label.setValue("Error");
				if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
					thrown = ex;
					// server offset into the stream doesn't match our offset
					// (uploaded). Seek to
					// the expected offset according to the server and try
					// again.
					uploaded = ex.errorValue.getLookupFailedValue().getIncorrectOffsetValue().getCorrectOffset();
					continue;
				} else {
					// some other error occurred, give up.
					System.err.println("Error uploading to Dropbox: " + ex.getMessage());

					return;
				}
			} catch (DbxException ex) {
				label.setValue("Error");
				System.err.println("Error uploading to Dropbox: " + ex.getMessage());

				return;
			} catch (IOException ex) {
				label.setValue("Error");
				System.err.println("Error reading from file \"" + localFile + "\": " + ex.getMessage());

				return;
			}
		}

		// if we made it here, then we must have run out of attempts
		System.err.println("Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());

	}

	private static void printProgress(long uploaded, long size, Label label) {
		label.setValue("Upload " + Common.numberFormat.get().format(100 * (uploaded / (double) size)) + "%");
		System.out.printf("Uploaded %12d / %12d bytes (%5.2f%%)\n", uploaded, size, 100 * (uploaded / (double) size));
	}

	private static void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			// just exit
			System.err.println("Error uploading to Dropbox: interrupted during backoff.");

		}
	}

	public static void prosesBackup(final java.io.File file, final EventListener eventListener) {
		final Label label = new Label(ais.common.Common.getBahasaConfig("Proses kirim file .."));
		final Tbmuser tbmuser = Common.getCurrentUser();
		Clients.showBusy(label.getValue());
		final Timer timer = new Timer(200);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.setRepeats(true);
		timer.addEventListener("onTimer", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Clients.showBusy(label.getValue());
				if (label.getValue().isEmpty()) {
					System.out.println(
							"Kirim file " + file.getAbsolutePath() + " telah berhasil, hapus -> " + file.delete());
					Clients.clearBusy();
					timer.detach();
				} else if (label.getValue().equals("Error")) {
					displayLink(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							try {
								upload(tbmuser, file, label, eventListener);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/dropbox/UploadDropboxUtil.java:332");
							}
						}
					}, tbmuser);
					Clients.clearBusy();
					timer.detach();
				}

			}
		});
		timer.start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					upload(tbmuser, file, label, eventListener);
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/dropbox/UploadDropboxUtil.java:351");
				}
			}
		}).start();
	}

	public static void displayLink(final EventListener eventListener, final Tbmuser tbmuser) throws Exception {
		final String username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();
		GoogleCommon.codesDropbox.remove(username);

		// Run through Dropbox API authorization process
		File fileDropBoxAuth = new File("/opt/dropbox_auth.txt");
		if (!fileDropBoxAuth.exists()) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("key", "sp9ufnk7b0flta9");
			jsonObject.put("secret", "5fpw2a7kxf9x5ex");
			ais.common.BacaTulisUtil.tulis(fileDropBoxAuth, jsonObject.toString());
		}
		String argAppInfoFile = fileDropBoxAuth.getAbsolutePath();
		final DbxAppInfo appInfo;
		try {
			appInfo = DbxAppInfo.Reader.readFromFile(argAppInfoFile);
		} catch (JsonReader.FileLoadException ex) {
			System.err.println("Error reading <app-info-file>: " + ex.getMessage());
			return;
		}
		DbxRequestConfig requestConfig = new DbxRequestConfig("examples-authorize");
		final DbxWebAuth webAuth = new DbxWebAuth(requestConfig, appInfo);
		final String redirectUri = "https://ecampus.id/";
		System.out.println("redirectUri -> " + redirectUri);

		final MyWindow window = new MyWindow("Dropbox", "none", true);
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
				"Anda belum terhubung ke dropbox, untuk menghubungkan, klik tombol berikut :");

		MyButtonConfig a = new MyButtonConfig("Hubungkan ke Dropbox sekarang", FileFoto.icon("dropbox"));
		a.setStyle("font-size:14px;font-weight: bolder;");
		a.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				GoogleCommon.codes.remove(username);

				HttpServletRequest request = null;
				if (ExecutionsCtrl.getCurrent() != null) {
					request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				}

				if (request == null) {
					request = RequestContext.get();
				}

				HttpSession session = request.getSession(true);
				String sessionKey = "dropbox-auth-csrf-token";
				DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);

				DbxWebAuth.Request webAuthRequest = DbxWebAuth.newRequestBuilder()
						.withState(URLEncoder.encode(
								Common.getRequestHostWithProtocol() + "/accept_dropbox.jsp?u=" + username, "UTF-8"))
						.withRedirectUri(redirectUri, csrfTokenStore).build();
				String authorizeUrl = webAuth.authorize(webAuthRequest);
				System.out.println("authorizeUrl -> " + authorizeUrl);
				Clients.evalJavaScript(
						"popupCenter({url: '" + authorizeUrl + "', title: 'Hubungkan ke Dropbox', w: 500, h: 500});");
			}
		});

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(a);

		final Timer timer = new Timer(500);
		timer.setRepeats(true);
		timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		timer.addEventListener("onTimer", new EventListener() {

			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void onEvent(Event event) throws Exception {
				if (GoogleCommon.codesDropbox.containsKey(username)) {
					Map kode = GoogleCommon.codesDropbox.get(username);
					System.out.println("username -> " + username + " kode -> " + kode);
					try {
						HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent()
								.getNativeRequest();
						HttpSession session = request.getSession(true);
						String sessionKey = "dropbox-auth-csrf-token";
						DbxSessionStore csrfTokenStore = new DbxStandardSessionStore(session, sessionKey);
						DbxAuthFinish authFinish;
						try {
							authFinish = webAuth.finishFromRedirect(redirectUri, csrfTokenStore, kode);
						} catch (DbxException ex) {
							System.err.println("Error in DbxWebAuth.authorize: " + ex.getMessage());
							return;
						}

						System.out.println("Authorization complete.");
						System.out.println("- User ID: " + authFinish.getUserId());
						System.out.println("- Account ID: " + authFinish.getAccountId());
						System.out.println("- Access Token: " + authFinish.getAccessToken());

						// Save auth information to output file.
						DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.getAccessToken(), appInfo.getHost());

						String username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();

						String lokasi = ConstantValues.lokasiFileTemproraryTemp;

						if (lokasi.endsWith("/")) {
							lokasi += username + "_dropbox_auth_output.txt";
						} else {
							lokasi += "/" + username + "_dropbox_auth_output.txt";
						}

						File output = new File(lokasi);
						DbxAuthInfo.Writer.writeToFile(authInfo, output);
						System.out.println("Saved authorization information to \"" + output.getCanonicalPath() + "\".");

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/dropbox/UploadDropboxUtil.java:488");
					}
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

	public static void upload(Tbmuser tbmuser, File file, Label label, EventListener eventListener) throws Exception {
		// Only display important log messages.
		Logger.getLogger("").setLevel(Level.WARNING);

		final String username = tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId();

		String lokasi = ConstantValues.lokasiFileTemproraryTemp;

		if (lokasi.endsWith("/")) {
			lokasi += username + "_dropbox_auth_output.txt";
		} else {
			lokasi += "/" + username + "_dropbox_auth_output.txt";
		}

		File fileConf = new File(lokasi);
		if (!fileConf.exists()) {
			label.setValue("Error");
			throw new Exception("File konfigurasi belum ada");
		}

		String argAuthFile = fileConf.getAbsolutePath();
		String localPath = file.getAbsolutePath();
		String dropboxPath = "/" + file.getName();

		// Read auth info file.
		DbxAuthInfo authInfo;
		try {
			authInfo = DbxAuthInfo.Reader.readFromFile(argAuthFile);
		} catch (JsonReader.FileLoadException ex) {
			System.err.println("Error loading <auth-file>: " + ex.getMessage());

			return;
		}

		String pathError = DbxPathV2.findError(dropboxPath);
		if (pathError != null) {
			System.err.println("Invalid <dropbox-path>: " + pathError);

			return;
		}

		File localFile = new File(localPath);
		if (!localFile.exists()) {
			System.err.println("Invalid <local-path>: file does not exist.");

			return;
		}

		if (!localFile.isFile()) {
			System.err.println("Invalid <local-path>: not a file.");
			return;
		}

		// Create a DbxClientV2, which is what you use to make API calls.
		DbxRequestConfig requestConfig = new DbxRequestConfig("examples-upload-file");
		DbxClientV2 dbxClient = new DbxClientV2(requestConfig, authInfo.getAccessToken(), authInfo.getHost());

		// upload the file with simple upload API if it is small enough,
		// otherwise use chunked
		// upload API for better performance. Arbitrarily chose 2 times our
		// chunk size as the
		// deciding factor. This should really depend on your network.
		if (localFile.length() <= (2 * CHUNKED_UPLOAD_CHUNK_SIZE)) {
			uploadFile(dbxClient, localFile, dropboxPath, label, eventListener);
		} else {
			chunkedUploadFile(dbxClient, localFile, dropboxPath, label, eventListener);
		}

	}
}
