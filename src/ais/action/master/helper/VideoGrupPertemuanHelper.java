package ais.action.master.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.FlowPlayerWindow;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.VideoConverter;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GrupPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class VideoGrupPertemuanHelper implements DataLoader {

	private MyGrid grid;
	private GrupPertemuan grupPertemuan;
	private Boolean delete = false;
	private Tab tab;

	public VideoGrupPertemuanHelper(Boolean delete) {
		this.delete = delete;
	}

	class DetailGrupPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		Session session = HibernateUtil.currentSession();
		Tbmuser user = Common.getCurrentUser();

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final VideoPertemuan videoPertemuan = (VideoPertemuan) data;
			GrupPertemuan grupPertemuan = (GrupPertemuan) (videoPertemuan.getGrupPertemuan() == null ? null
					: session.createCriteria(GrupPertemuan.class)
							.add(Restrictions.idEq(videoPertemuan.getGrupPertemuan())).uniqueResult());
			if (videoPertemuan.getLink() == null) {

				String url = videoPertemuan.createLinkUri();

				String str = "<video style=\"display:block;width:560px;height:315px\" controls>" + "<source src=\""
						+ url + "#t=00:00:03\" type=\"video/mp4\">" + "Your browser does not support the video tag."
						+ "</video>";

				new ais.ui.util.MyHtml(str).setParent(row);

			} else {
				if (videoPertemuan.getLink().toLowerCase().contains("youtu")) {
					String idVideo = null;
					try {
						String[] s = StringUtils.split(videoPertemuan.getLink(), "=");
						idVideo = s[s.length - 1];
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/VideoGrupPertemuanHelper.java:103");

					}
					if (idVideo == null) {
						try {
							String[] s = StringUtils.split(videoPertemuan.getLink(), "/");
							idVideo = s[s.length - 1];
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/VideoGrupPertemuanHelper.java:110");

						}
					}
					System.out.println("idVideo => " + idVideo);
					if (idVideo != null) {
						String contentVideo = "<iframe width=\"560\" "
								+ (Common.isMobile() ? "style=\"height:600px\"" : "height=\"460\"")
								+ " src=\"https://www.youtube.com/embed/" + idVideo
								+ "\" frameborder=\"0\" allowfullscreen></iframe>";
						new ais.ui.util.MyHtml(contentVideo).setParent(row);
					} else {
						A a;
						(a = new A(videoPertemuan.getLink())).setParent(row);
						a.setTarget("_blank");
						a.setHref(videoPertemuan.getLink());
					}
				} else {
					A a;
					(a = new A(videoPertemuan.getLink())).setParent(row);
					a.setTarget("_blank");
					a.setHref(videoPertemuan.getLink());
				}
			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new Label(grupPertemuan == null ? "" : grupPertemuan.getNama()).setParent(vbox);

			if (user != null && user.getMahasiswa() != null) {
				new MyLabelBold(videoPertemuan.getKeteranganTambahan()).setParent(vbox);
			} else {
				final Textbox keteranganTambahan = new Textbox(videoPertemuan.getKeteranganTambahan());
				keteranganTambahan.setReadonly(!delete);
				keteranganTambahan.setRows(3);
				keteranganTambahan.setParent(vbox);
				keteranganTambahan.setWidth("90%");
				keteranganTambahan.setHeight("95%");
				keteranganTambahan.setStyle("min-height: 95%;");
				keteranganTambahan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						try {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							videoPertemuan.setKeteranganTambahan(keteranganTambahan.getValue());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (videoPertemuan));
							session.getTransaction().commit();
							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			}

			if (videoPertemuan.getLink() == null) {

				Hbox toolbar = new Hbox();
				toolbar.setParent(row);
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("", "/img/flash.png");

				toolbarbutton.setOrient("vertical");
				toolbarbutton.setParent(toolbar);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						String url = videoPertemuan.createLinkUri();

						FlowPlayerWindow flowPlayerWindow = new FlowPlayerWindow(url);
						flowPlayerWindow.setHeight("95%");
						flowPlayerWindow.setWidth("750px");
						flowPlayerWindow.onModal();

					}

				});

				toolbarbutton = new MyToolbarbuttonConfig("Download", videoPertemuan.iconDonwload());

				toolbarbutton.setOrient("vertical");
				toolbarbutton.setParent(toolbar);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Session session = StreamingHibernateUtil.getInstance().currentSession();

						VideoPertemuan myvideoPertemuan = (VideoPertemuan) session.createCriteria(VideoPertemuan.class)
								.add(Restrictions.idEq(videoPertemuan.getId())).uniqueResult();

						Filedownload.save(myvideoPertemuan.ambilFile(), myvideoPertemuan.getType());

						StreamingHibernateUtil.getInstance().closeSession();

					}

				});

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setVisible(delete);
				button.setOrient("vertical");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												Common.refreshDelete((videoPertemuan));
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												PesanFormalHelper.tampilkanGagalException(
														"menghapus video pertemuan ini",
														e,
														new String[] {
																"Periksa apakah data video ini masih berelasi dengan data lain sehingga tidak dapat dihapus.",
																"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
																"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
			} else {
				Hbox toolbar = new Hbox();
				toolbar.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setVisible(delete);
				button.setOrient("vertical");
				button.setTooltiptext("Hapus Data");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Session session = StreamingHibernateUtil.getInstance().currentSession();

												session.getTransaction().begin();
												Common.refreshDelete((videoPertemuan));
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												PesanFormalHelper.tampilkanGagalException(
														"menghapus video pertemuan ini",
														e,
														new String[] {
																"Periksa apakah data video ini masih berelasi dengan data lain sehingga tidak dapat dihapus.",
																"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
																"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
											}

										}

									}
								});

					}

				});
				button.setParent(toolbar);
			}

		}

	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<VideoPertemuan> videoPertemuans = session.createCriteria(VideoPertemuan.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("grupPertemuan", grupPertemuan.getId())).list();

			if (tab != null) {
				tab.setLabel("Video (" + videoPertemuans.size() + " video)");
			}

			ListModel strset = new SimpleListModel(videoPertemuans);
			grid.setRowRenderer(new DetailGrupPertemuanRenderer());
			grid.setModelCheckMobile(strset);

			StreamingHibernateUtil.getInstance().closeSession();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

	}

	public void display(final GrupPertemuan grupPertemuan, final Component component) {

		this.grupPertemuan = grupPertemuan;
		if (component instanceof Tabpanel) {
			tab = ((Tabpanel) component).getLinkedTab();
			tab.getLabel();
		}

		Common.clear(component);

		MyPanel panel = new MyPanel();
		panel.setParent(component);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Video");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);
		toolbar.setVisible(grupPertemuan != null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Video (Maks 50 mb)", "/img/new.gif");
		button.setVisible(delete);
		button.setUpload("true,maxsize=" + (1024 * 50));
		button.addEventListener("onUpload", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;

				final Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;
				String url = media.getName();
				if (url.trim().toLowerCase().endsWith("mp4") || url.trim().toLowerCase().endsWith("avi")
						|| url.trim().toLowerCase().endsWith("mov") || url.trim().toLowerCase().endsWith("wmv")
						|| url.trim().toLowerCase().endsWith("flv") || url.trim().toLowerCase().endsWith("3gp")) {
					final File file = new File(
							Sessions.getCurrent().getWebApp().getRealPath("/temp/" + media.getName()));
					final Tbmuser tbmuser = Common.getCurrentUser();
					final Label label = Common.displayLoadBar(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							MyMessageboxConfig.show("Upload video berhasil dilakukan", "Informasi",
									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											file.delete();
											loadData(null);
										}
									});

						}
					});

					new Thread(new Runnable() {

						@SuppressWarnings("deprecation")
						@Override
						public void run() {

							try {

								InputStream inputStream = media.getStreamData();
								// System.out.println("media = " + media);

								// System.out.println("file = " +
								// file.getAbsolutePath());
								file.getParentFile().mkdirs();
								FileOutputStream fileOutputStream = new FileOutputStream(file);
								int c;
								while ((c = inputStream.read()) != -1) {
									fileOutputStream.write(c);
								}
								fileOutputStream.close();
								inputStream.close();

								File folder = CommonMedia.getMediaDirectory();
								final File newFile = new File(folder.getAbsolutePath() + "/"
										+ ais.ui.util.WaktuUtil.getCalendar().getTimeInMillis() + ".mp4");

								System.out.println("newFile = " + newFile.getAbsolutePath());
								Session session = StreamingHibernateUtil.getInstance().currentSession();
								VideoPertemuan videoPertemuan = new VideoPertemuan();
								try {
									Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(newFile)));

									VideoConverter videoConverter = new VideoConverter("ffmpeg");
									videoConverter.convert(file.getAbsolutePath(), newFile.getAbsolutePath(), label);
									videoPertemuan.setFoto(blob);
									videoPertemuan.setNama(newFile.getName());
								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(file)));

									videoPertemuan.setFoto(blob);
									videoPertemuan.setNama(file.getName());
								}

								try {
									videoPertemuan.setNama(media.getName());
									videoPertemuan.setKeterangan(media.getFormat());
									videoPertemuan.setType(media.getContentType());
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/VideoGrupPertemuanHelper.java:439");
								}

								videoPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
								videoPertemuan.setOleh(tbmuser.getUserId());

								if (grupPertemuan != null) {
									videoPertemuan.setGrupPertemuan(grupPertemuan.getId());
								}

								session.getTransaction().begin();
								session.save(videoPertemuan);
								session.getTransaction().commit();

								StreamingHibernateUtil.getInstance().closeSession();

								newFile.delete();

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);
							}

							label.setValue("");

						}
					}).start();

				} else {
					MyMessageboxConfig.show("File video harus berformat MP4, AVI, MOV, WMV, FLV, dan 3GP", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				}

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Tambah Link Video", "/img/new.gif");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow myWindow = new MyWindow("Tambah link Video", "none", true);
				myWindow.setHeight("300px");
				myWindow.setWidth("550px");
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

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("30%");

				column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth("70%");

				Rows rows = new Rows();
				rows.setParent(grid);

				MyFormRow row = new MyFormRow();row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Link Video")));
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setValue("http://");
				isi.setWidth("90%");
				isi.setRows(3);
				isi.select();

				Common.initKeterangan(rows,
						"Contoh link video : https://drive.google.com/file/d/1jqqlH3bqCE9IcShsooF_RqOYpLehKiRV/view?usp=sharing");

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

						try {
							VideoPertemuan videoPertemuan = new VideoPertemuan();
							videoPertemuan.setKeterangan("link");
							videoPertemuan.setType("link");
							videoPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							videoPertemuan.setOleh(Common.getCurrentUser().getUserId());
							videoPertemuan.setNama("link");
							videoPertemuan.setLink(isi.getValue().trim());

							if (grupPertemuan != null) {
								videoPertemuan.setGrupPertemuan(grupPertemuan.getId());
							}

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(videoPertemuan);
							session.getTransaction().commit();

							StreamingHibernateUtil.getInstance().closeSession();

							loadData(null);
						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}
						myWindow.detach();
					}
				});
				save.setParent(toolbar);
				borderlayout.setParent(myWindow);
				myWindow.onModal();
			}

		});
		button.setParent(toolbar);

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Video");
		column.setWidth("60%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		loadData(null);

	}

}
