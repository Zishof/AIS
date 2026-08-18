package ais.action.master.helper.generic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
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
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import com.google.common.io.Files;

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.dropbox.UploadDropboxUtil;
import ais.common.gdrive.GDriveUtilPerPengguna;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GrupPertemuan;
import ais.database.model.Konfigurasi;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.KurikulumPunyaMatakuliahDetail;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.PertemuanFileContent;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataPertemuanFileContent extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;
	private MyTextbox oleh;

	private Tbmuser tbmuser;
	private Pertemuan pertemuan;
	private GrupPertemuan grupPertemuan;
	private KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah;
	private KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail;
	private Paging paging;

	public AmbilDataPertemuanFileContent(final Pertemuan pertemuan, final GrupPertemuan grupPertemuan,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliahTemp,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail) {
		super();
		this.pertemuan = pertemuan;
		this.grupPertemuan = grupPertemuan;
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliahTemp;
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;

		tbmuser = Common.getCurrentUser();
		display();
		onSearchDefault(null);
	}

	public static void displayRow(final PertemuanFileContent pertemuanFileContent, Row arg0,
			final EventListener eventListener) {

		String n = pertemuanFileContent.getNama() != null
				&& pertemuanFileContent.getNama().trim().equalsIgnoreCase("link") ? pertemuanFileContent.getLink()
						: pertemuanFileContent.getNama();
		if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
			n = pertemuanFileContent.getNama();
		}

		if (n == null || n.trim().isEmpty()) {
			arg0.setVisible(false);
			return;
		}

		Hbox hbox = new Hbox();
		hbox.setParent(arg0);
		final Radio checkbox = new Radio();
		checkbox.setParent(hbox);
		arg0.setAttribute("checkbox", checkbox);
		checkbox.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null) {
					Event myEvent = new Event("myEvent", event.getTarget(), pertemuanFileContent);
					eventListener.onEvent(myEvent);
				}
			}
		});

		Vbox vbox = new Vbox();
		vbox.setParent(hbox);

		Toolbarbutton downloadButton = new MyToolbarbuttonConfig(n,
				!pertemuanFileContent.getGoogleBook().isEmpty() ? "/img/Apps-Google-Play-Books-icon.png"
						: pertemuanFileContent.getLokasiFisik() != null ? "/img/svg/desktop-light.svg"
								: FileFoto.icon(pertemuanFileContent.getNama()));
		downloadButton.setTooltiptext("Download \"" + pertemuanFileContent.getNama() + "\"");
		downloadButton.setAttribute("janganDisabled", true);
		vbox.appendChild(downloadButton);
		downloadButton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!pertemuanFileContent.getGoogleBook().isEmpty()) {
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(pertemuanFileContent.getLink(), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + pertemuanFileContent.getLink()
								+ "', title: 'Book', w: 1200, h: 600});");
					}
				} else if (pertemuanFileContent.getGdrive() != null) {
					pertemuanFileContent.tampilGDrive(null);
				} else {

					String link = pertemuanFileContent == null ? null
							: (pertemuanFileContent.getLink() == null || pertemuanFileContent.getLink().isEmpty() ? null
									: pertemuanFileContent.getLink());

					if (pertemuanFileContent != null
							&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
						link = pertemuanFileContent.createLinkUri();
						if (link != null) {
							// link = link.replaceAll("download=false", "download=true");
						}
					}

					if (pertemuanFileContent != null && link != null && !link.trim().isEmpty()) {

						if (pertemuanFileContent.bisaPreview()) {
							Common.displayWindow(pertemuanFileContent.merupakanGambar(), link, true, "95%", "95%", true,
									pertemuanFileContent);
						} else {
							if (Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
							} else {
								Clients.evalJavaScript(
										"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
							}
						}
					} else {
						MyMessageboxConfig.show(
								"Mohon maaf, berkas yang Anda akses tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) pastikan berkas belum dihapus atau dipindahkan; (3) hubungi pengajar atau administrator apabila berkas seharusnya tersedia.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				}
			}
		});

		if (pertemuanFileContent.getPertemuan() != null) {
			Session session = HibernateUtil.currentSession();
			Pertemuan pertemuan = (Pertemuan) session.createCriteria(Pertemuan.class)
					.add(Restrictions.idEq(pertemuanFileContent.getPertemuan())).uniqueResult();
			if (pertemuan != null) {

				try {
					RevisiHelper.createNewRevisi(Pertemuan.class, pertemuan,
							pertemuan.info() + " pertemuan ke " + pertemuan.getPertemuanKe() + " "
									+ pertemuan.getTopik() + " " + Common.dateFormat4.get().format(pertemuan.getTanggal()))
							.setParent(vbox);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:237");
					// TODO: handle exception
				}

			}
			pertemuan = null;
		}

		new Label(pertemuanFileContent.getTanggal_dirubah() == null ? ""
				: Common.dateFormat5.get().format(pertemuanFileContent.getTanggal_dirubah())).setParent(arg0);

		String olehId = pertemuanFileContent.getOlehId();
		String oleh = pertemuanFileContent.getOleh();

		Common.infoDiuploadOleh(olehId, oleh, arg0);
	}

	class PertemuanFileContentRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			final PertemuanFileContent pertemuanFileContent = (PertemuanFileContent) arg1;
			arg0.setAttribute("pertemuanFileContent", pertemuanFileContent);

			AmbilDataPertemuanFileContent.displayRow(pertemuanFileContent, arg0, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (eventListener != null) {
						eventListener.onEvent(arg0);
					}
					AmbilDataPertemuanFileContent.this.detach();
				}
			});

		}

	}

	public MyToolbarbuttonConfig tampilkanTombolUpload(String tambahan) {
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig(
				"Upload " + tambahan + " " + Common.ukuranLabelFileUpload(), "/img/new.gif");
		mybutton.setUpload(Common.ukuranFileUpload());
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				try {

					Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
					Blob blob = Common.getBlobFromMedia(media, session);
					pertemuanFileContent.setFoto(blob);
					pertemuanFileContent.setNama(media.getName());
					pertemuanFileContent.setFileMimeType(media.getContentType());
					pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
							kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
									: kurikulumPunyaMatakuliahDetail.getId());
					pertemuanFileContent
							.setGrupPertemuan(grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
					pertemuanFileContent.setKurikulumPunyaMatakuliah(
							kurikulumPunyaMatakuliah == null ? -Common.randLong() : kurikulumPunyaMatakuliah.getId());
					pertemuanFileContent.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
					pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

					Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
					Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
					Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
					String olehId = Common.generateOlehId(tbmuser);
					pertemuanFileContent.setOlehId(olehId);
					pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
							: mahasiswa != null ? mahasiswa.getNama()
									: dosen != null ? dosen.getNama()
											: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

					session.getTransaction().begin();
					session.save(pertemuanFileContent);
					session.getTransaction().commit();

					eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

					StreamingHibernateUtil.getInstance().closeSession();

				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}
			}
		});

		return mybutton;
	}

	public static MyToolbarbuttonConfig tampilkanTombolUploadGDrive(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {

		int maxDrive = 300;
		try {
			maxDrive = Integer.parseInt(Common.getKonfigurasi("max_upload_via_drive_baru", "300").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:343");
			// TODO: handle exception
		}

		final List<PertemuanFileContent> pertemuanFileContents = new ArrayList<PertemuanFileContent>();
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload materi ke Drive (maks " + maxDrive + " Mb)",
				"/img/Google-Drive-icon.png");
		mybutton.setUpload("true,maxsize=" + (1024 * maxDrive));
		mybutton.setVisible(tbmuser != null && tbmuser.getUserId() != null);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

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

				GDriveUtilPerPengguna driveUtilPerPengguna = new GDriveUtilPerPengguna(tbmuser);

				VOPembelajaran voPembelajaran = pertemuan.ambilVOPembelajaran();
				String tugasName = "Materi pertemuan ke " + pertemuan.getPertemuanKe() + " " + pertemuan.info();

				driveUtilPerPengguna.prosesBackup(f, voPembelajaran.infoSimple(), tugasName, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						try {

							com.google.api.services.drive.model.File fileUpload = (com.google.api.services.drive.model.File) arg0
									.getData();

							if (fileUpload != null && fileUpload.getId() != null) {

								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setGdrive(fileUpload.getId());
								pertemuanFileContent.setGdriveUsername(
										tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
								pertemuanFileContent.setNama(media.getName());
								pertemuanFileContent.setFileMimeType(media.getContentType());
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								pertemuanFileContents.add(pertemuanFileContent);
							}

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						StreamingHibernateUtil.getInstance().closeSession();
					}
				});

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!pertemuanFileContents.isEmpty()) {
							eventListener.onEvent(new Event("baru", null, pertemuanFileContents.get(0)));
							timer.stop();
							timer.detach();
						}
					}
				});
				timer.start();
			}
		});

		return mybutton;
	}

	public static MyToolbarbuttonConfig tampilkanTombolTambahLink(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Link Materi", FileFoto.icon(""));
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final Window myWindow = new Window("Tambah link materi", "none", true);
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

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new MyLabelBold("Link Materi"));

				row = new MyFormRow();
				row.setParent(rows);
				final Textbox isia;
				row.appendChild(isia = new Textbox());
				isia.setValue("");
				isia.setWidth("90%");
				isia.setRows(3);
				isia.select();

				row = new MyFormRow();
				row.setParent(rows);
				Common.initKeteranganSatuKolom(rows, "* Jika link lebih dari satu, pisahkan dengan spasi");

				row = new MyFormRow();
				row.setParent(rows);

				row.appendChild(new Image(FileFoto.icon("drive.google")));

				Common.initKeteranganSatuKolom(rows,
						"Contoh link materi jika menggunakan google drive : https://drive.google.com/file/d/1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV/view?usp=sharing");
				Common.initKeteranganSatuKolom(rows,
						"atau contoh di google drive : atau juga bisa https://drive.google.com/open?id=1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV");

				Common.initKeteranganSatuKolom(rows,
						"Contoh link di dalam folder drive : https://drive.google.com/drive/folders/0B1iqp0kGPjWsNDg5NWFlZjEtN2IwZC00NmZiLWE3MjktYTE2ZjZjNTZiMDY2");

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

						List<String> urls = Common.getUrls(isia.getValue().trim());
						if (urls.isEmpty()) {
							MyMessageboxConfig.show(
									"Masukkan link secara valid, perhatikan beberapa contoh link di bawah ini.",
									"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							return;
						}

						for (String u : urls) {

							String type = "";
							try {
								URL url = new URL(u);
								HttpURLConnection huc = (HttpURLConnection) url.openConnection();
								type = huc.getHeaderField("Content-Type");
//								System.out.println("type => " + type);
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:608");
							}

							try {
								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setKeterangan("link");
								pertemuanFileContent.setType("link");
								pertemuanFileContent.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								pertemuanFileContent.setOleh(Common.getCurrentUser().getUserId());
								pertemuanFileContent.setNama("link");
								pertemuanFileContent.setLink(u);
								pertemuanFileContent.setFileMimeType(type);

								if (pertemuan != null) {
									pertemuanFileContent.setPertemuan(pertemuan.getId());
								}
								if (kurikulumPunyaMatakuliah != null) {
									pertemuanFileContent.setKurikulumPunyaMatakuliah(kurikulumPunyaMatakuliah.getId());
								}

								if (kurikulumPunyaMatakuliahDetail != null) {
									pertemuanFileContent
											.setKurikulumPunyaMatakuliahDetail(kurikulumPunyaMatakuliahDetail.getId());
								}

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});
		return button;
	}

	public static MyToolbarbuttonConfig tampilkanTombolUploadDropbox(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {
		final List<PertemuanFileContent> pertemuanFileContents = new ArrayList<PertemuanFileContent>();
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig mybutton = new MyToolbarbuttonConfig("Upload materi ke Dropbox (maks 500 Mb)",
				FileFoto.icon("dropbox"));
		mybutton.setUpload("true,maxsize=" + (1024 * 500));
		mybutton.setVisible(false);
		mybutton.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				final Media media = ((UploadEvent) event).getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

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
						Session session = StreamingHibernateUtil.getInstance().currentSession();
						try {

							String urlLink = (String) arg0.getData();

							if (urlLink != null && !urlLink.trim().isEmpty()) {

								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();

								pertemuanFileContent.setNama(media.getName());
								pertemuanFileContent.setFileMimeType(media.getContentType());
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());
								pertemuanFileContent.setLink(urlLink);

								Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								pertemuanFileContents.add(pertemuanFileContent);
							}

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						StreamingHibernateUtil.getInstance().closeSession();
					}
				});

				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (!pertemuanFileContents.isEmpty()) {
							eventListener.onEvent(new Event("baru", null, pertemuanFileContents.get(0)));
							timer.stop();
							timer.detach();
						}
					}
				});
				timer.start();
			}
		});

		return mybutton;
	}

	public static MyToolbarbuttonConfig createScanFoto(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Gunakan Kamera", "/img/camera-icon.png");
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		toolbarbutton.setVisible(Common.isSecure(request));
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {

							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");

								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setFoto(a.getFoto());
								pertemuanFileContent.setNama(a.getNama());
								pertemuanFileContent.setFileMimeType(a.getKeterangan());
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

								timer.detach();

							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
									pertemuanFileContent.setGdrive(d);
									pertemuanFileContent.setGdriveUsername(
											tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
									pertemuanFileContent.setNama(file_name);
									pertemuanFileContent.setFileMimeType("image/jpg");
									pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
											kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
													: kurikulumPunyaMatakuliahDetail.getId());
									pertemuanFileContent.setGrupPertemuan(
											grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
									pertemuanFileContent.setKurikulumPunyaMatakuliah(
											kurikulumPunyaMatakuliah == null ? -Common.randLong()
													: kurikulumPunyaMatakuliah.getId());
									pertemuanFileContent
											.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
									pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

									Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									pertemuanFileContent.setOlehId(olehId);
									pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									Session session = StreamingHibernateUtil.getInstance().currentSession();
									session.getTransaction().begin();
									session.save(pertemuanFileContent);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();

									eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + PertemuanFileContent.class.getName();

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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:969");
				}
			}
		});

		return toolbarbutton;
	}

	public static MyToolbarbuttonConfig createScanLayar(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Rekam Layar", "/img/Monitor-3-icon.png");
		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		toolbarbutton.setVisible(Common.isSecure(request));
		toolbarbutton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event a) throws Exception {
				final Long rand = Common.randLong();
				final Timer timer = new Timer(1000);
				timer.setRepeats(true);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Map<String, Object> da = AmbilDataLampiranFileLain.fotoDrive.get(rand);
						if (da != null) {

							if (da.get("fileFotoLain") != null) {

								FileFotoLain a = (FileFotoLain) da.get("fileFotoLain");

								PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
								pertemuanFileContent.setFoto(a.getFoto());
								pertemuanFileContent.setNama(a.getNama());
								pertemuanFileContent.setFileMimeType(a.getKeterangan());
								pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
										kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
												: kurikulumPunyaMatakuliahDetail.getId());
								pertemuanFileContent.setGrupPertemuan(
										grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
								pertemuanFileContent.setKurikulumPunyaMatakuliah(
										kurikulumPunyaMatakuliah == null ? -Common.randLong()
												: kurikulumPunyaMatakuliah.getId());
								pertemuanFileContent
										.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
								pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

								Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
								Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
								Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
								String olehId = Common.generateOlehId(tbmuser);
								pertemuanFileContent.setOlehId(olehId);
								pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
										: mahasiswa != null ? mahasiswa.getNama()
												: dosen != null ? dosen.getNama()
														: pegawai != null ? pegawai.getNama()
																: (tbmuser.getUserNama()));

								Session session = StreamingHibernateUtil.getInstance().currentSession();
								session.getTransaction().begin();
								session.save(pertemuanFileContent);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

								timer.detach();

							} else if (da.get("drive") != null) {

								String d = (String) da.get("drive");
								String file_name = (String) da.get("file_name");
								if (d != null && !d.trim().isEmpty()) {

									PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
									pertemuanFileContent.setGdrive(d);
									pertemuanFileContent.setGdriveUsername(
											tbmuser == null ? Common.getCurrentSessionId() : tbmuser.getUserId());
									pertemuanFileContent.setNama(file_name);
									pertemuanFileContent.setFileMimeType("image/jpg");
									pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
											kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
													: kurikulumPunyaMatakuliahDetail.getId());
									pertemuanFileContent.setGrupPertemuan(
											grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
									pertemuanFileContent.setKurikulumPunyaMatakuliah(
											kurikulumPunyaMatakuliah == null ? -Common.randLong()
													: kurikulumPunyaMatakuliah.getId());
									pertemuanFileContent
											.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
									pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

									Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									pertemuanFileContent.setOlehId(olehId);
									pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									Session session = StreamingHibernateUtil.getInstance().currentSession();
									session.getTransaction().begin();
									session.save(pertemuanFileContent);
									session.getTransaction().commit();

									StreamingHibernateUtil.getInstance().closeSession();

									eventListener.onEvent(new Event("baru", null, pertemuanFileContent));

									timer.detach();
								}
							}
						}
					}
				});
				timer.start();

				AmbilDataLampiranFileLain.fotoDrive.put(rand, null);
				String q = "&rand=" + rand + "&clazz=" + PertemuanFileContent.class.getName();

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

					Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
							+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:1141");
				}
			}
		});

		return toolbarbutton;
	}

	@SuppressWarnings("deprecation")
	public static void uploadScorm(File fileAsli, Pertemuan pertemuan,
			KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, GrupPertemuan grupPertemuan,
			KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah, Tbmuser tbmuser, EventListener eventListener) {
		try {
			String rootPath = Common.getKonfigurasi("root_path_scorm", "/opt").getNilai();
			if (fileAsli != null && fileAsli.getName().toLowerCase().trim().endsWith(".zip")) {

				String nama = Common.getGeneratedBarCode() + "_" + fileAsli.getName().replaceAll(" ", "_");
				File fileLokasi = new File(rootPath + Common.ROOT + "_scorm/" + nama);
				fileLokasi.getParentFile().mkdirs();
				Files.copy(fileAsli, fileLokasi);

				String lokasi = "/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/scorm/"
						+ fileLokasi.getName().replace(".zip", "").replaceAll(" ", "_");
				String path1 = Common.REAL_PATH + lokasi;

				File dir = new File(path1);
				try {
					FileUtils.deleteDirectory(dir);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:1169");

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

					Session session = StreamingHibernateUtil.getInstance().currentSession();
					PertemuanFileContent pertemuanFileContent = new PertemuanFileContent();
					pertemuanFileContent.setKeterangan(
							(title.isEmpty() ? "Materi pembelajaran " + (pertemuan == null ? "" : pertemuan.info())
									: title) + (description.isEmpty() ? "" : ", " + description));
					pertemuanFileContent.setLokasiFisik(fileLokasi.getAbsolutePath());

					long fileSizeInBytes = fileLokasi.length();
					// Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
					long fileSizeInKB = fileSizeInBytes / 1024;
					// Convert the KB to MegaBytes (1 MB = 1024 KBytes)
					long fileSizeInMB = fileSizeInKB / 1024;

					if (fileSizeInMB <= 3L) {
						try {
							pertemuanFileContent.setFoto(new javax.sql.rowset.serial.SerialBlob(
									IOUtils.toByteArray(new FileInputStream(fileLokasi.getAbsolutePath()))));
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/generic/AmbilDataPertemuanFileContent.java:1243");
						}
					}

					pertemuanFileContent.setLink(Common.getRequestHostWithProtocol() + relativeLocation);
					pertemuanFileContent.setNama(fileAsli.getName());
					pertemuanFileContent.setFileMimeType("application/zip");
					pertemuanFileContent.setKurikulumPunyaMatakuliahDetail(
							kurikulumPunyaMatakuliahDetail == null ? -Common.randLong()
									: kurikulumPunyaMatakuliahDetail.getId());
					pertemuanFileContent
							.setGrupPertemuan(grupPertemuan == null ? -Common.randLong() : grupPertemuan.getId());
					pertemuanFileContent.setKurikulumPunyaMatakuliah(
							kurikulumPunyaMatakuliah == null ? -Common.randLong() : kurikulumPunyaMatakuliah.getId());
					pertemuanFileContent.setPertemuan(pertemuan == null ? -Common.randLong() : pertemuan.getId());
					pertemuanFileContent.setUploadDate(ais.ui.util.WaktuUtil.getDate());

					Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
					Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
					Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
					String olehId = Common.generateOlehId(tbmuser);
					pertemuanFileContent.setOlehId(olehId);
					pertemuanFileContent.setOleh(tbmuser == null ? "external_update"
							: mahasiswa != null ? mahasiswa.getNama()
									: dosen != null ? dosen.getNama()
											: pegawai != null ? pegawai.getNama() : (tbmuser.getUserNama()));

					session.getTransaction().begin();
					session.save(pertemuanFileContent);
					session.getTransaction().commit();

					eventListener.onEvent(new Event("baru", null, pertemuanFileContent));
					StreamingHibernateUtil.getInstance().closeSession();

				} else {
					MyMessageboxConfig.show(
							"Mohon maaf, launcher tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas launcher telah tersedia; (2) unggah ulang berkas yang sesuai; (3) hubungi administrator apabila kendala berlanjut.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}

			} else {
				MyMessageboxConfig.show(
						"Mohon maaf, berkas yang Anda unggah harus berupa berkas berformat ZIP. Langkah yang dapat dilakukan: (1) kompres berkas ke dalam format .zip terlebih dahulu; (2) pastikan ekstensi berkas benar; (3) unggah kembali berkas tersebut.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public static String cariLaunch(File dir) {
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
				relativeLocation = org.apache.commons.lang3.StringUtils.replace(file.getAbsolutePath(), Common.REAL_PATH, "");
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

	public static Map<String, File> mapFileUpload = new HashMap<String, File>();

	public static MyToolbarbuttonConfig createScorm(final Pertemuan pertemuan,
			final KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah,
			final KurikulumPunyaMatakuliahDetail kurikulumPunyaMatakuliahDetail, final GrupPertemuan grupPertemuan,
			final EventListener eventListener) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Upload Media Pembelajaran / scorm (*.zip)",
				"/img/svg/desktop-light.svg");
		button.addEventListener("onClick", new EventListener() {

			private Window myWindow;

			@Override
			public void onEvent(Event event) throws Exception {
				myWindow = new Window("Media Pembelajaran / Shareable Content Object Reference Model (SCORM)", "none",
						true);
				myWindow.setHeight("450px");
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

				MyFormRow row = new MyFormRow();row.setValign("top");
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

				final String randLong = Common.getGeneratedBarCode();
				String src = Common.getRequestHostWithProtocol() + "/doUpload.jsp?pertemuanFileContent=" + randLong
						+ "&accept=" + URLEncoder.encode("accept=\".zip\"", "UTF-8");

				String tinggi = "130px";
				row = new MyFormRow();
				row.setHeight("130px");
				row.setParent(rows);
				Html html = new ais.ui.util.MyHtml("<iframe src=\"" + src + "\" style=\"width:100%;height:" + tinggi
						+ ";border:0px;\" allow=\"camera;microphone;geolocation\"></iframe>");
				html.setHeight(tinggi);
				row.appendChild(html);

				final Timer timer = new Timer(1000);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.setRepeats(true);
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (mapFileUpload.containsKey(randLong)) {
							File file = mapFileUpload.get(randLong);

							uploadScorm(file, pertemuan, kurikulumPunyaMatakuliahDetail, grupPertemuan,
									kurikulumPunyaMatakuliah, tbmuser, eventListener);

							timer.detach();
							myWindow.detach();
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
						myWindow.detach();
					}
				});
				cancel.setParent(toolbar);

				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});
		return button;

	}

	public void display() {

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);
		radiogroup.setHeight("100%");
		radiogroup.setWidth("100%");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myBorderlayout1 = new ais.ui.util.MyBorderlayout();
		myBorderlayout1.setParent(center);

		Center myCenter1 = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter1, true);
		myCenter1.setParent(myBorderlayout1);

		South mySouth = new South();
		mySouth.setParent(myBorderlayout1);

		paging.setParent(mySouth);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Columns columns = new Columns();

		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama File"));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		oleh = new MyTextbox("");

		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		boolean hanyaBolehMelihatlampirannyaSendiri = Common.bolehKonfigurasi("hanya_boleh_melihat_lampirannya_sendiri", Konfigurasi.TIDAK_AKTIF);

		if (!hanyaBolehMelihatlampirannyaSendiri && dosen == null && mahasiswa == null
				&& biodataCalonMahasiswa != null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Upload Oleh"));
			row.appendChild(oleh);
			oleh.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});
		}

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight(Common.isMobile() ? "50px" : "25px");
		toolbar.setParent(div);

		toolbar.appendChild(createScanFoto(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}));

		if (!Common.isMobile())
			toolbar.appendChild(createScanLayar(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
					grupPertemuan, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							eventListener.onEvent(arg0);
						}
					}));

		toolbar.appendChild(createScorm(pertemuan, kurikulumPunyaMatakuliah, kurikulumPunyaMatakuliahDetail,
				grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}));

		if (Common.bolehKonfigurasi("boleh_upload_file_langsung")) {
			toolbar.appendChild(tampilkanTombolUpload("materi baru "));
		}

		toolbar.appendChild(tampilkanTombolUploadGDrive(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}));

		toolbar.appendChild(tampilkanTombolUploadDropbox(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}));

		toolbar.appendChild(tampilkanTombolTambahLink(pertemuan, kurikulumPunyaMatakuliah,
				kurikulumPunyaMatakuliahDetail, grupPertemuan, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						eventListener.onEvent(arg0);
					}
				}));

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		center.setTitle("Pilih daftar materi yang sebelumnya pernah di-upload, jika file materi lebih besar dari "
				+ Common.ukuranLabelFileUpload()
				+ ", maka materi harus di-upload di tempat lain dan klik tambahkan link di atas, kemudian masukkan link dari file yang baru saja di-upload.");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setParent(myCenter1);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Materi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu Upload");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Diupload oleh");
		column.setWidth("15%");

		if (!Common.isMobile()) {
			South south = new South();
			ais.ui.util.ZkCompat.setFlex(south, true);
			south.setParent(borderlayout);

			toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(south);
		}

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataPertemuanFileContent.this.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	public Criteria initCriteria(boolean order, Session session) {

		boolean hanyaBolehMelihatlampirannyaSendiri = Common.bolehKonfigurasi("hanya_boleh_melihat_lampirannya_sendiri", Konfigurasi.TIDAK_AKTIF);

		BiodataCalonMahasiswa biodataCalonMahasiswa = tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa();
		Dosen dosen = tbmuser == null ? null : tbmuser.getDosen();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
		String olehId = tbmuser == null ? "external_update;"
				: biodataCalonMahasiswa != null
						? biodataCalonMahasiswa.getNoRegistrasi() + ";" + BiodataCalonMahasiswa.class.getName()
						: mahasiswa != null ? mahasiswa.getNim() + ";" + Mahasiswa.class.getName()
								: dosen != null ? tbmuser.getUserId() + ";" + Dosen.class.getName()
										: pegawai != null ? tbmuser.getUserId() + ";" + Pegawai.class.getName()
												: (tbmuser.getUserId() + ";" + Tbmuser.class.getName());

		Criteria criteria = session.createCriteria(PertemuanFileContent.class)
				// .add(Restrictions.or(Restrictions.eq("googleBook", ""),
				// Restrictions.isNull("googleBook")))
				.add(Restrictions.isNull("copyDari"))
				.add(oleh.getValue().trim().isEmpty()
						? (hanyaBolehMelihatlampirannyaSendiri || dosen != null || mahasiswa != null
								|| biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null
										? Restrictions.ilike("olehId", olehId, MatchMode.START)
										: Restrictions.sqlRestriction("true"))
						: Restrictions.or(Restrictions.ilike("olehId", oleh.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("oleh", oleh.getValue().trim(), MatchMode.ANYWHERE)))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE));
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = StreamingHibernateUtil.getInstance().currentSession();
		Common.initPaging(initCriteria(false, session), paging);

		List<PertemuanFileContent> myPertemuanFileContent = initCriteria(true, session)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(myPertemuanFileContent);
		grid.setRowRenderer(new PertemuanFileContentRenderer());
		grid.setModelCheckMobile(strset);
		StreamingHibernateUtil.getInstance().closeSession();
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
