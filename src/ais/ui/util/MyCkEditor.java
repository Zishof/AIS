package ais.ui.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.zkforge.ckez.CKeditor;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import com.google.common.io.Files;

import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.MD5;
import ais.common.RequestContext;
import ais.common.YouTubeHelper;
import ais.common.dropbox.UploadDropboxUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.LampiranLain;

/**
 * Komponen/konfigurasi ZK khusus AIS untuk my ck editor. Tipe ini membakukan default dan perilaku
 * tampilan di atas komponen induk supaya layar tidak mengulang konfigurasi widget yang sama.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Borderlayout}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code CKeditor cKeditor}, {@code
 * EventListener my}, {@code com.google.api.services.drive.model.File fileUpload}, {@code String urlLink}, {@code
 * MyHboxToolbar hbox}; pembacaan/pencarian ({@code getValue()}, {@code addEventListener()}); mutasi data ({@code
 * setHeight()}, {@code setWidth()}, {@code setValue()}, {@code setStyle()}); konfigurasi constructor: {@code
 * contentVideo}, {@code description}, {@code fileUpload}, {@code fileupload}, {@code fileupload2}, {@code
 * hasil}, {@code hbox}, {@code isi}, {@code maxDrive}, {@code relativeLocation}, {@code request}, {@code row},
 * {@code text}, {@code title}, {@code urlLink}. Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> setter dan helper mengubah state komponen ZK yang sedang terpasang pada desktop.
 * Gunakan pada event thread UI dan jangan membagikan instance antar session; aturan bisnis dan transaksi
 * persistence tetap harus didelegasikan ke action atau service pemanggil.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Borderlayout
 */
public class MyCkEditor extends Borderlayout {

	public CKeditor cKeditor = new CKeditor();

	private EventListener my = null;

	/**
	 * 
	 */
	private static final long serialVersionUID = 122222222222L;

	private com.google.api.services.drive.model.File fileUpload = null;
	private String urlLink = null;

	public MyHboxToolbar hbox;

	public MyCkEditor() {
		super();

		super.setStyle("min-height: 450px;");

		Center center = new Center();
		center.setBorder("none");
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(this);
		center.appendChild(cKeditor);

		North north = new North();
		north.setHeight("25px");
		north.setBorder("none");
		north.setParent(this);
		hbox = new MyHboxToolbar();
		hbox.setParent(north);

		Toolbarbutton fileupload2 = new MyToolbarbuttonConfig("Gambar", FileFoto.icon(".jpg"));
		fileupload2.setUpload(Common.ukuranFileUpload(1024 * 2));
		hbox.appendChild(fileupload2);
		fileupload2.addEventListener("onUpload", new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				if (uploadEvent != null) {
					String type = uploadEvent.getMedia().getContentType();
					if (type.equalsIgnoreCase("image/jpeg") || type.equalsIgnoreCase("image/jpg")
							|| type.equalsIgnoreCase("image/gif") || type.equalsIgnoreCase("image/png")) {

						Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

						File folder = CommonMedia.getMediaDirectory();
						if (!folder.exists()) {
							folder.mkdirs();
						}

						File f = new File(folder.getAbsolutePath() + "/"
								+ URLEncoder.encode(ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_"
										+ uploadEvent.getMedia().getName(), "UTF-8"));

						f.createNewFile();
						FileOutputStream fileOutputStream = new FileOutputStream(f);
						try {
							IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
						} catch (Exception e) {
							try {
								IOUtils.write(media.getStringData(), fileOutputStream);
							} catch (Exception ee) {
								IOUtils.write(media.getByteData(), fileOutputStream);
							}
						}

						fileOutputStream.close();

						long ref = Common.randLong();
						Tbmuser tbmuser = Common.getCurrentUser();
						Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
						Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
						Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
						String olehId = Common.generateOlehId(tbmuser);

						LampiranLain lainMahasiswa = new LampiranLain();
						lainMahasiswa.setRef((Long) ref);
						lainMahasiswa.setNama(media.getName());
						lainMahasiswa.setLink(null);
						lainMahasiswa.setKeterangan(media.getContentType());
						lainMahasiswa.setJenis("Gambar");
						lainMahasiswa.setOlehId(olehId);
						lainMahasiswa.setOleh(tbmuser == null ? "external_update"
								: mahasiswa != null ? mahasiswa.getNama()
										: dosen != null ? dosen.getNama()
												: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

						try {

							lainMahasiswa.setFoto(Hibernate
									.createBlob(IOUtils.toByteArray(new FileInputStream(f.getAbsolutePath()))));

							Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
							streamingSession.getTransaction().begin();
							streamingSession.save(lainMahasiswa);
							streamingSession.getTransaction().commit();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

						StreamingHibernateUtil.getInstance().closeSession();

						String d = lainMahasiswa.createLinkUri(false);

						String text = cKeditor.getValue();
						text += "<img alt='' style=\"width: 80%;\" src=\"" + d + "\"></img>";
						cKeditor.setValue(text);
						if (my != null) {
							Common.createDefaultTimer(my);
						}
					} else {
						MyMessageboxConfig.show(
								"File yang diuplaod harus berupa gambar, format yang mendukung adalah : JPG, JPEG, PNG, dan GIF",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					}
				}
			}
		});

		MyToolbarbutton fileupload = new MyToolbarbutton("fa-wpforms", "Media Pembelajaran (SCORM)");
		hbox.appendChild(fileupload);
		fileupload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final Window myWindow = new Window(
						"Media Pembelajaran / Shareable Content Object Reference Model (SCORM)", "none", true);
				myWindow.setHeight("350px");
				myWindow.setWidth("850px");
				myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBold(
						"Apa itu SCORM? Shareable Content Object Reference Model (SCORM) = Model Referensi Objek Konten yang Dapat Dibagikan. Merupakan standar internasional untuk online course yang hampir kompatibel untuk semua LMS."));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Html(
						"Contoh tools yang dapat digunakan untuk membuat media pembelajaran SCORM adalah <a target='_blank' href='http://www.courselab.com'>http://www.courselab.com</a>"));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Html(
						"Cara membuat  membuat media pembelajaran SCORM bisa dilihat di <a target='_blank' href='https://docs.moodle.org/404/en/Creating_SCORM_Content'>https://docs.moodle.org/404/en/Creating_SCORM_Content</a>"));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Html(
						"Tools atau software yang berguna untuk menkonversi file PPTX menjadi SCORM <a target='_blank' href='https://www.ispringsolutions.com/ispring-free/download?ref=ispring-free'>iSpring</a>"));

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
						myWindow.detach();
					}
				});
				cancel.setParent(toolbar);

				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig(
						"Lanjut upload file media pembelajaran / scorm (*.zip)", "/img/svg/upload.svg");
				toolbarbutton.setUpload("true,maxsize=" + (1024 * 300));
				toolbarbutton.setParent(toolbar);
				toolbarbutton.addEventListener("onUpload", new EventListener() {

					private String cariLaunch(File dir) {
						String relativeLocation = "";

						File[] files = dir.listFiles(new FilenameFilter() {
							public boolean accept(File dir, String name) {
								return name.toLowerCase().endsWith(".html") || dir.isDirectory();
							}
						});

						for (File file : files) {

							if (file.getName().endsWith("start.html") || file.getName().endsWith("launchpage.html")
									|| file.getName().endsWith("index.html")) {
								System.out.println("start.html -> " + file.getAbsolutePath());
								relativeLocation = org.apache.commons.lang3.StringUtils.replace(file.getAbsolutePath(),
										Common.REAL_PATH, "");
								System.out.println("relativeLocation -> " + relativeLocation);
								break;
							} else if (file.isDirectory()) {
								String s = cariLaunch(file);
								if (!s.isEmpty()) {
									relativeLocation = s;
								}
							}
						}

						return relativeLocation;
					}

					@SuppressWarnings("deprecation")
					@Override
					public void onEvent(Event event) throws Exception {

						try {

							Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

							if (media.getName().toLowerCase().trim().endsWith(".zip")) {

								String nama = Common.getGeneratedBarCode() + "_" + media.getName().replaceAll(" ", "_");
								File fileLokasi = new File("/opt" + Common.ROOT + "_scorm/" + nama);
								fileLokasi.getParentFile().mkdirs();
								Files.write(media.getByteData(), fileLokasi);

								String lokasi = "/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/scorm/"
										+ fileLokasi.getName().replace(".zip", "").replaceAll(" ", "_");
								String path1 = Common.REAL_PATH + lokasi;

								File dir = new File(path1);
								try {
									FileUtils.deleteDirectory(dir);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyCkEditor.java:316");

								}
								dir.mkdirs();

								Path source = Paths.get(fileLokasi.getAbsolutePath());
								Path target = Paths.get(dir.getAbsolutePath());

								Common.unzipFolder(source, target);

								File[] files = dir.listFiles(new FilenameFilter() {
									public boolean accept(File dir, String name) {
										return name.toLowerCase().endsWith(".xml");
									}
								});

								String relativeLocation = "";
								String title = "";
								String description = "";

								if (files.length > 0) {
									File xmlManifest = files[0];
									FileInputStream fis = new FileInputStream(xmlManifest);
									Document doc = Jsoup.parse(fis, null, "", Parser.xmlParser());

									for (Element e : doc.select("title")) {
										title = e.html();
									}

									for (Element e : doc.select("description")) {
										description = e.html();
									}

									for (Element e : doc.select("resource")) {
										relativeLocation = e.attr("href");
									}

									if (relativeLocation.trim().isEmpty()) {
										for (Element e : doc.select("location")) {

											relativeLocation = e.html();
											relativeLocation = relativeLocation.replace("<!--// <![CDATA[", "");
											relativeLocation = relativeLocation.replace("// ]]> -->", "");

										}
									}
									relativeLocation = lokasi + "/" + relativeLocation;
								} else {
									relativeLocation = "/" + cariLaunch(dir);
								}

								if (!relativeLocation.isEmpty()) {

									System.out.println("title -> " + title + ", description -> " + description
											+ ", relativeLocation -> " + relativeLocation);

									String d = Common.getRequestHostWithProtocol() + relativeLocation;

									long ref = Common.randLong();
									Tbmuser tbmuser = Common.getCurrentUser();
									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);

									LampiranLain lainMahasiswa = new LampiranLain();
									lainMahasiswa.setRef((Long) ref);
									lainMahasiswa.setNama(media.getName());
									lainMahasiswa.setLink(null);
									lainMahasiswa.setKeterangan(media.getContentType());
									lainMahasiswa.setLokasiFisik(fileLokasi.getAbsolutePath());
									lainMahasiswa.setJenis("link");
									lainMahasiswa.setOlehId(olehId);
									lainMahasiswa.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									lainMahasiswa.setLink(d);

									try {

										long fileSizeInBytes = fileLokasi.length();
										// Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
										long fileSizeInKB = fileSizeInBytes / 1024;
										// Convert the KB to MegaBytes (1 MB = 1024 KBytes)
										long fileSizeInMB = fileSizeInKB / 1024;

										if (fileSizeInMB <= 3L) {
											try {
												lainMahasiswa.setFoto(Hibernate.createBlob(IOUtils.toByteArray(
														new FileInputStream(fileLokasi.getAbsolutePath()))));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:410");
											}
										}

										Session streamingSession = StreamingHibernateUtil.getInstance()
												.currentSession();
										streamingSession.getTransaction().begin();
										streamingSession.save(lainMahasiswa);
										streamingSession.getTransaction().commit();

									} catch (Exception e) {
										StreamingHibernateUtil.getInstance().rollbackTransaction();
										Common.tampilErrorJikaAdmin(e);
									}

									StreamingHibernateUtil.getInstance().closeSession();

									String text = cKeditor.getValue();
									text += "<iframe src=\"" + d + "\" " + Common.getStyleContent1() + "></iframe>";
									cKeditor.setValue(text);
									if (my != null) {
										Common.createDefaultTimer(my);
									}

									myWindow.detach();
								} else {
									MyMessageboxConfig.show("Maaf, launcher tidak ditemukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
								}

							} else {
								MyMessageboxConfig.show("Maaf, file yang diupload harus berupa file zip",
										"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							}

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
					}
				});

				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});

		fileupload = new MyToolbarbutton("fa-camera", "Kamera");
		HttpServletRequest request = null;
		if (ExecutionsCtrl.getCurrent() != null) {
			request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		}

		if (request == null) {
			request = RequestContext.get();
		}
		fileupload.setVisible(Common.isSecure(request));
		hbox.appendChild(fileupload);
		fileupload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {

							if (da.get("urlFoto") != null) {
								String text = cKeditor.getValue();
								text += "<img alt='' style=\"width: 80%;\" src=\"" + da.get("urlFoto") + "\"></img>";
								cKeditor.setValue(text);
								if (my != null) {
									Common.createDefaultTimer(my);
								}
								timer.detach();
							} else if (da.get("drive") != null) {
								String d = (String) da.get("drive");
								if (d != null && !d.trim().isEmpty()) {

									String text = cKeditor.getValue();
									text += "<iframe src=\"https://drive.google.com/file/d/" + d + "/preview\" "
											+ Common.getStyleContent() + "></iframe>";
									cKeditor.setValue(text);
									if (my != null) {
										Common.createDefaultTimer(my);
									}

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String q = "&rand=" + rand;

				try {

					final MyWindow window = new MyWindow("Ambil Foto / Video", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src = Common.getRequestHostWithProtocol() + "/capture.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;
					String src1 = Common.getRequestHostWithProtocol() + "/capture_video.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					Tabbox tabbox = new Tabbox();
					tabbox.setHeight("100%");
					tabbox.setWidth("100%");
					tabbox.setParent(center);
					Tabs myTabs = new Tabs();
					myTabs.setParent(tabbox);

					Tabpanels mytabpanels = new Tabpanels();
					mytabpanels.setParent(tabbox);

					Tab tabUtama = new Tab("Foto");
					myTabs.appendChild(tabUtama);

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
					tabpanelUtama.setHeight(tinggi);
					tabpanelUtama.setWidth("100%");
					tabpanelUtama.setParent(mytabpanels);

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama).appendChild(html);

					Tab tabUtama1 = new Tab("Video");
					myTabs.appendChild(tabUtama1);

					Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
					tabpanelUtama1.setHeight(tinggi);
					tabpanelUtama1.setWidth("100%");
					tabpanelUtama1.setParent(mytabpanels);

					Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1 + "\" style=\"width:100%;height:"
							+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html1.setHeight(tinggi);
					Common.tampilanScroll(tabpanelUtama1).appendChild(html1);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:592");
				}
			}
		});

		if (!Common.isMobile()) {
			fileupload = new MyToolbarbutton("fa-desktop", "Layar");
			fileupload.setVisible(Common.isSecure(request));
			hbox.appendChild(fileupload);
			fileupload.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					final Long rand = Common.randLong();
					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
							if (da != null) {

								if (da.get("urlFoto") != null) {
									String text = cKeditor.getValue();
									text += "<img alt='' style=\"width: 80%;\" src=\"" + da.get("urlFoto")
											+ "\"></img>";
									cKeditor.setValue(text);
									if (my != null) {
										Common.createDefaultTimer(my);
									}
									timer.detach();
								} else if (da.get("drive") != null) {

									String d = (String) da.get("drive");
									if (d != null && !d.trim().isEmpty()) {

										String text = cKeditor.getValue();
										text += "<iframe src=\"https://drive.google.com/file/d/" + d + "/preview\" "
												+ Common.getStyleContent() + "></iframe>";
										cKeditor.setValue(text);
										if (my != null) {
											Common.createDefaultTimer(my);
										}

										timer.detach();
									}
								}
							}
						}
					});
					timer.start();

					AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
					String q = "&rand=" + rand;

					try {

						final MyWindow window = new MyWindow("Rekam Layar", "none", true);
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

						Borderlayout borderlayout = new Borderlayout();
						borderlayout.setParent(window);

						Center center = new Center();
						center.setBorder("none");
						center.setParent(borderlayout);
						ais.ui.util.ZkCompat.setFlex(center, true);
						String src = Common.getRequestHostWithProtocol() + "/capture_screen.jsp?lokasi=false&mobile="
								+ Common.isMobile() + q;

						boolean mobile = Common.isMobile();
						String tinggi = mobile ? "850px" : "550px";

						Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:"
								+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
						html.setHeight(tinggi);
						Common.tampilanScroll(center).appendChild(html);

						South south = new South();
						ais.ui.util.ZkCompat.setFlex(south, true);
						south.setParent(borderlayout);

						Toolbar toolbar = new Toolbar();
						toolbar.setParent(south);
						MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
						cancel.setTooltiptext("Tutup");
						cancel.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								window.detach();
							}
						});
						cancel.setParent(toolbar);
						window.setVisible(true);
						window.setHeight("97%");
						window.setWidth(mobile ? "97%" : "550px");
						window.onModal();

					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:693");
					}
				}
			});
		}

		fileupload = new MyToolbarbutton("fa-volume-up", "Audio");
		fileupload.setVisible(Common.isSecure(request));
		hbox.appendChild(fileupload);
		fileupload.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {
							if (da.get("urlFoto") != null) {
								String text = cKeditor.getValue();
								text += "<img alt='' style=\"width: 80%;\" src=\"" + da.get("urlFoto") + "\"></img>";
								cKeditor.setValue(text);
								if (my != null) {
									Common.createDefaultTimer(my);
								}
								timer.detach();
							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								if (d != null && !d.trim().isEmpty()) {
									String s = "style=\"min-height: 80px;min-width: 260px;width:100%;height:95%;\"";
									String text = cKeditor.getValue();
									text += "<iframe src=\"https://drive.google.com/file/d/" + d + "/preview\" " + s
											+ "></iframe>";
									cKeditor.setValue(text);
									if (my != null) {
										Common.createDefaultTimer(my);
									}

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String q = "&rand=" + rand;

				try {

					final MyWindow window = new MyWindow("Rekam Audio", "none", true);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(window);

					Center center = new Center();
					center.setBorder("none");
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					String src1 = Common.getRequestHostWithProtocol() + "/capture_audio.jsp?lokasi=false&mobile="
							+ Common.isMobile() + q;

					boolean mobile = Common.isMobile();
					String tinggi = mobile ? "850px" : "550px";

					Html html1 = new ais.ui.util.MyHtml("<iframe src=\"" + src1 + "\" style=\"width:100%;height:"
							+ tinggi + ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
					html1.setHeight(tinggi);
					Common.tampilanScroll(center).appendChild(html1);

					South south = new South();
					ais.ui.util.ZkCompat.setFlex(south, true);
					south.setParent(borderlayout);

					Toolbar toolbar = new Toolbar();
					toolbar.setParent(south);
					MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
					cancel.setTooltiptext("Tutup");
					cancel.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							window.detach();
						}
					});
					cancel.setParent(toolbar);
					window.setVisible(true);
					window.setHeight("97%");
					window.setWidth(mobile ? "97%" : "550px");
					window.onModal();

				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:792");
				}
			}
		});

		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyCkEditor.java:800");
			// TODO: handle exception
		}

		fileupload2 = new MyToolbarbuttonConfig("Drive", "/img/Google-Drive-icon.png");
		fileupload2.setUpload("true,maxsize=" + (1024 * maxDrive));
		hbox.appendChild(fileupload2);
		fileupload2.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				if (uploadEvent != null) {
					fileUpload = null;
					Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					File folder = CommonMedia.getMediaDirectory();
					if (!folder.exists()) {
						folder.mkdirs();
					}
					File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
							ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(), "UTF-8"));

					f.createNewFile();
					FileOutputStream fileOutputStream = new FileOutputStream(f);
					try {
						IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
					} catch (Exception e) {
						try {
							IOUtils.write(media.getStringData(), fileOutputStream);
						} catch (Exception ee) {
							IOUtils.write(media.getByteData(), fileOutputStream);
						}
					}

					fileOutputStream.close();
					Tbmuser tbmuser = Common.getCurrentUser();
					GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);
					driveUtilPerPengguna.prosesBackup(f, "Data Untuk Editor", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							fileUpload = (com.google.api.services.drive.model.File) arg0.getData();
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (fileUpload != null && fileUpload.getId() != null) {
								String text = cKeditor.getValue();
								text += "<iframe src=\"https://drive.google.com/file/d/" + fileUpload.getId()
										+ "/preview\" " + Common.getStyleContent() + "></iframe>";
								cKeditor.setValue(text);
								if (my != null) {
									Common.createDefaultTimer(my);
								}
								timer.stop();
								timer.detach();
							}
						}
					});
					timer.start();
				}
			}
		});

		fileupload2 = new MyToolbarbuttonConfig("Tambah ke Dropbox", FileFoto.icon("dropbox"));
		fileupload2.setVisible(false);
		fileupload2.setUpload("true,maxsize=" + (1024 * 500));
		hbox.appendChild(fileupload2);
		fileupload2.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;
				if (uploadEvent != null) {
					urlLink = null;
					Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					File folder = CommonMedia.getMediaDirectory();
					if (!folder.exists()) {
						folder.mkdirs();
					}
					File f = new File(folder.getAbsolutePath() + "/" + URLEncoder.encode(
							ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + "_" + media.getName(), "UTF-8"));

					f.createNewFile();
					FileOutputStream fileOutputStream = new FileOutputStream(f);
					try {
						IOUtils.copyLarge(media.getStreamData(), fileOutputStream);
					} catch (Exception e) {
						try {
							IOUtils.write(media.getStringData(), fileOutputStream);
						} catch (Exception ee) {
							IOUtils.write(media.getByteData(), fileOutputStream);
						}
					}

					fileOutputStream.close();

					UploadDropboxUtil.prosesBackup(f, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							urlLink = (String) arg0.getData();
						}
					});

					final Timer timer = new Timer(1000);
					timer.setRepeats(true);
					timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					timer.addEventListener("onTimer", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							if (urlLink != null && !urlLink.trim().isEmpty()) {
								String text = cKeditor.getValue();
								String hasil = "";
								if (urlLink.toLowerCase().trim().contains(".mp4")) {
									hasil = "<video " + Common.getStyleContentHeighMaxKecil()
											+ " controls><source src=\""
											+ org.apache.commons.lang3.StringUtils
													.replace(org.apache.commons.lang3.StringUtils.replace(urlLink,
															"dl=0", "raw=1"), "dl=1", "raw=1")
											+ "\" type=\"video/mp4\" /></video>";
								} else if (urlLink.toLowerCase().trim().contains(".mp3")) {
									hasil = "<audio controls >" + " <source " + " <source src=\""
											+ org.apache.commons.lang3.StringUtils.replace(
													org.apache.commons.lang3.StringUtils.replace(urlLink, "dl=0",
															"raw=1"),
													"dl=1", "raw=1")
											+ "\" type=\"audio/mpeg\">"
											+ " Your browser does not support the audio element." + "</audio>";
								} else if (urlLink.toLowerCase().trim().contains(".jpg")
										|| urlLink.toLowerCase().trim().contains(".jpeg")
										|| urlLink.toLowerCase().trim().contains(".png")
										|| urlLink.toLowerCase().trim().contains(".gif")) {
									hasil = "<img alt='' src=\"" + StringUtils.replace(
											org.apache.commons.lang3.StringUtils.replace(urlLink, "dl=0", "raw=1"),
											"dl=1", "raw=1") + "\"></img>";
								} else {
									hasil = "<a target='_blank' href=\"" + urlLink + "\">Klik di-sini untuk lihat "
											+ urlLink + "</a>";
								}

								text += hasil;
								cKeditor.setValue(text);
								if (my != null) {
									Common.createDefaultTimer(my);
								}
								timer.stop();
								timer.detach();
							}
						}
					});
					timer.start();
				}
			}
		});

		MyToolbarbutton tambahLink = new MyToolbarbutton("fa-link", "Link");
		hbox.appendChild(tambahLink);
		tambahLink.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow myWindow = new MyWindow("Tambah Link berupa file / video / audio / gambar", "none",
						true);
				myWindow.setHeight("95%");
				myWindow.setWidth("850px");
				myWindow.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());

				Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
				Center center = new Center();
				center.setParent(borderlayout);
				ais.ui.util.ZkCompat.setFlex(center, true);
				MyGrid grid = new MyGrid();
				grid.setWidth("100%");
				grid.setParent(center);
				grid.setWidth("100%");
				grid.setHeight("100%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBold("Link File / Video / Audio / Gambar"));

				row = new MyFormRow();
				row.setParent(rows);
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setValue("");
				isi.setWidth("90%");
				isi.setRows(3);
				isi.select();

				row = new MyFormRow();
				row.setParent(rows);
				Common.initKeteranganSatuKolom(rows, "* Jika link lebih dari satu, pisahkan dengan spasi");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("drive.google")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan google drive : https://drive.google.com/file/d/1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV/view?usp=sharing");
				Common.initKeteranganSatuKolom(rows,
						"atau contoh di google drive : atau juga bisa https://drive.google.com/open?id=1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV");

				Common.initKeteranganSatuKolom(rows,
						"Contoh link di dalam folder drive : https://drive.google.com/drive/folders/0B1iqp0kGPjWsNDg5NWFlZjEtN2IwZC00NmZiLWE3MjktYTE2ZjZjNTZiMDY2");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("google.form")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link form menggunakan google form : https://docs.google.com/forms/d/e/1FAIpQLSccIW6y0dwK3i6z2QlGpsURroanR4HeV6zotSeoVpuZ07xFWw/viewform");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("youtube")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link video jika menggunakan youtube : https://www.youtube.com/watch?v=Ed8Uw9b_jyk");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("instagram")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan Instagram : https://www.instagram.com/p/fA9uwTtkSN");

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new Image(FileFoto.icon("dropbox")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link jika menggunakan dropbox : https://www.dropbox.com/s/fshcbd82hnj0f60/1590735986510_Funny%2BCat%2BFaces%2BCompilation%2B2014%2B%255BNEW%255D.mp4?dl=0");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("mp3")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link MP3 : https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("pdf")));
				Common.initKeteranganSatuKolom(rows,
						"Contoh link pdf : https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("facebook")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh post facebook : https://www.facebook.com/20531316728/posts/10154009990506729/");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("twitter")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh post twitter : https://twitter.com/Interior/status/463440424141459456");

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
						myWindow.detach();
					}
				});
				cancel.setParent(toolbar);
				MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
				save.setTooltiptext("Simpan");
				save.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						List<String> urls = Common.getUrls(isi.getValue().trim());
						if (urls.isEmpty()) {
							MyMessageboxConfig.show(
									"Masukkan link secara valid, perhatikan beberapa contoh link di bawah ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}
						for (String u : urls) {
							try {
								String hasil = "";

								if (u != null && u.toLowerCase().trim().contains("dropbox")) {
									if (u.toLowerCase().trim().contains(".mp4")) {
										hasil = "<video " + Common.getStyleContentHeighMaxKecil()
												+ " controls><source src=\""
												+ org.apache.commons.lang3.StringUtils
														.replace(org.apache.commons.lang3.StringUtils.replace(u, "dl=0",
																"raw=1"), "dl=1", "raw=1")
												+ "\" type=\"video/mp4\" /></video>";
									} else if (u.toLowerCase().trim().contains(".mp3")) {
										hasil = "<audio controls >" + " <source " + " <source src=\""
												+ org.apache.commons.lang3.StringUtils
														.replace(org.apache.commons.lang3.StringUtils.replace(u, "dl=0",
																"raw=1"), "dl=1", "raw=1")
												+ "\" type=\"audio/mpeg\">"
												+ " Your browser does not support the audio element." + "</audio>";
									} else if (u.toLowerCase().trim().contains(".jpg")
											|| u.toLowerCase().trim().contains(".jpeg")
											|| u.toLowerCase().trim().contains(".png")
											|| u.toLowerCase().trim().contains(".gif")) {
										hasil = "<img alt='' src=\"" + org.apache.commons.lang3.StringUtils.replace(
												org.apache.commons.lang3.StringUtils.replace(u, "dl=0", "raw=1"),
												"dl=1", "raw=1") + "\"></img>";
									} else {
										hasil = "<a target='_blank' href=\"" + u + "\">Klik di-sini untuk lihat " + u
												+ "</a>";
									}
								} else if (u != null && u.toLowerCase().trim().contains("instagram")) {
									try {

										File fileFolderLampiran = new File("/opt/ecampus/instagram_"
												+ URLEncoder.encode(MD5.crypt(u), "UTF-8") + ".html");
										if (!fileFolderLampiran.exists()) {
											FileUtils.copyURLToFile(
													new URL("https://api.instagram.com/oembed?url="
															+ URLEncoder.encode(u, "UTF-8") + "&maxwidth=320"),
													fileFolderLampiran);
											String text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
											JSONObject o = new JSONObject(text);
											String html = o.getString("html");
											ais.common.BacaTulisUtil.tulis(fileFolderLampiran, html);
										} else {
											String text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
											if (text.trim().isEmpty()) {
												FileUtils.copyURLToFile(
														new URL("https://api.instagram.com/oembed?url="
																+ URLEncoder.encode(u, "UTF-8") + "&maxwidth=320"),
														fileFolderLampiran);
												text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
												JSONObject o = new JSONObject(text);
												String html = o.getString("html");
												ais.common.BacaTulisUtil.tulis(fileFolderLampiran, html);
											}
										}
										hasil = ais.common.BacaTulisUtil.baca(fileFolderLampiran);

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:1164");
									}
								}

								else if (u != null && u.toLowerCase().trim().contains("twitter")) {
									try {

										File fileFolderLampiran = new File("/opt/ecampus/twitter_"
												+ URLEncoder.encode(MD5.crypt(u), "UTF-8") + ".html");
										if (!fileFolderLampiran.exists()) {
											FileUtils.copyURLToFile(
													new URL("https://publish.twitter.com/oembed?url="
															+ URLEncoder.encode(u, "UTF-8") + "&conversation=none"),
													fileFolderLampiran);
											String text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
											JSONObject o = new JSONObject(text);
											String html = o.getString("html");
											ais.common.BacaTulisUtil.tulis(fileFolderLampiran, html);
										} else {
											String text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
											if (text.trim().isEmpty()) {
												FileUtils.copyURLToFile(
														new URL("https://publish.twitter.com/oembed?url="
																+ URLEncoder.encode(u, "UTF-8") + "&conversation=none"),
														fileFolderLampiran);
												text = ais.common.BacaTulisUtil.baca(fileFolderLampiran);
												JSONObject o = new JSONObject(text);
												String html = o.getString("html");
												ais.common.BacaTulisUtil.tulis(fileFolderLampiran, html);
											}
										}
										hasil = ais.common.BacaTulisUtil.baca(fileFolderLampiran);

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:1198");
									}
								}

								else if (u != null && u.toLowerCase().trim().contains("facebook")) {
									try {

										String c = "<html>" + "<title>My Website</title>" + "<body>"
												+ "<script async defer src='https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v3.2'></script> "
												+ "<div class='fb-post' " + "    data-href='" + u + "'"
												+ "    data-width='520'></div>" + "</body>" + "</html>";

										hasil = "<iframe srcdoc=\"" + c
												+ "\" style='width:100%;min-height:800px;height:100%' frameborder=\"0\" scrolling=\"no\" onload=\"resizeIframe(this)\"  allowtransparency=\"true\"></iframe>";

									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/ui/util/MyCkEditor.java:1214");
									}
								}

								else if (u.toLowerCase().trim().startsWith("https://docs.google.com")) {
									hasil = "<iframe src=\"" + u + "\" " + Common.getStyleContent() + "></iframe>";
								}

								else if (u.toLowerCase().trim().startsWith("https://drive.google.com/drive/folders")) {
									String contentVideo = org.apache.commons.lang3.StringUtils.replace(u,
											"https://drive.google.com/drive/folders", "");
									contentVideo = StringUtils.split(contentVideo, "&")[0];
									contentVideo = StringUtils.split(contentVideo, "/")[0];
									contentVideo = StringUtils.split(contentVideo, "?")[0];
									hasil = "<iframe src=\"https://drive.google.com/embeddedfolderview?id="
											+ contentVideo + "#list\" " + Common.getStyleContent() + "></iframe>";
								}

								else if (u.toLowerCase().trim().startsWith("https://drive.google.com/open?id=")) {
									String contentVideo = org.apache.commons.lang3.StringUtils.replace(u,
											"https://drive.google.com/open?id=", "");
									contentVideo = StringUtils.split(contentVideo, "&")[0];
									contentVideo = StringUtils.split(contentVideo, "/")[0];
									hasil = "<iframe src=\"https://drive.google.com/file/d/" + contentVideo
											+ "/preview\" " + Common.getStyleContent() + "></iframe>";
								} else if (u.toLowerCase().trim().startsWith("https://drive.google.com/file/d")) {
									String contentVideo = org.apache.commons.lang3.StringUtils.replace(u,
											"https://drive.google.com/file/d/", "");
									contentVideo = StringUtils.split(contentVideo, "/")[0];
									hasil = "<iframe src=\"https://drive.google.com/file/d/" + contentVideo
											+ "/preview\" " + Common.getStyleContent() + "></iframe>";
								} else if (u.toLowerCase().trim().startsWith("http")
										&& u.toLowerCase().contains("youtu")) {
									hasil = YouTubeHelper.convertoToEmbed(u);
								} else if (u.toLowerCase().trim().startsWith("http")
										&& u.toLowerCase().contains("facebook")) {
									hasil = "<iframe src=\"https://web.facebook.com/plugins/video.php?href="
											+ URLEncoder.encode(u, "UTF-8") + "&show_text=0&height=315\" "
											+ (Common.isMobile() ? "style='width:100%'" : "width=\"560\"") + " "
											+ (Common.isMobile() ? "style=\"height:600px\"" : "height=\"460\"")
											+ " style=\"border:none;overflow:hidden\" scrolling=\"no\" frameborder=\"0\" allowTransparency=\"true\" allowFullScreen=\"true\"></iframe>";
								} else if (u.toLowerCase().trim().endsWith(".mp3")) {
									hasil = "<audio controls >" + " <source " + " <source src=\"" + u
											+ "\" type=\"audio/mpeg\">"
											+ " Your browser does not support the audio element." + "</audio>";
								} else if (LampiranLain.merupakanDokumen(u)) {
									if (u.toLowerCase().endsWith("pdf")) {
										hasil = "<iframe src=\"" + u + "\" width=\"560\" height=\"400\"></iframe>";
									} else {
										String a = "https://docs.google.com/gview?embedded=true&url="
												+ URLEncoder.encode(u, "UTF-8");
										hasil = "<iframe src=\"" + a + "\" width=\"560\" height=\"400\"></iframe>";
									}
								} else if (LampiranLain.merupakanGambar(u)) {
									hasil = "<img alt='' src=\"" + u + "\"></img>";
								}

								String text = cKeditor.getValue();
								text += hasil;
								cKeditor.setValue(text);

							} catch (Exception e) {

								Common.tampilErrorJikaAdmin(e);
							}
						}
						if (my != null) {
							Common.createDefaultTimer(my);
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}
		});
	}

	@Override
	public void setHeight(String height) {
		// CKEditor (zkforge ckez) memerlukan tinggi dalam PIKSEL. Bila diberi nilai
		// persen seperti "100%", area edit (iframe) kolaps: toolbar tampil tetapi
		// tidak bisa diketik (gejala "tidak bisa input catatan" di Surat Masuk/Keluar).
		// Untuk nilai persen, beri editor tinggi piksel yang wajar; container
		// (Borderlayout) tetap memakai nilai aslinya agar tata letak tab tidak berubah.
		if (height != null && height.trim().endsWith("%")) {
			cKeditor.setHeight("400px");
		} else {
			cKeditor.setHeight(height);
		}
		super.setHeight(height);
	}

	@Override
	public void setWidth(String width) {
		cKeditor.setWidth(width);
		super.setWidth(width);
	}

	public void setValue(String value) {
		cKeditor.setValue(value);
	}

	@Override
	public void setStyle(String style) {
		// TODO Auto-generated method stub
		super.setStyle(style + ";min-height: 450px;");
	}

	public String getValue() {
		return cKeditor.getValue();
	}

	@Override
	public boolean addEventListener(String arg0, EventListener arg1) {
		my = arg1;
		return cKeditor.addEventListener(arg0, arg1);
	}
}
