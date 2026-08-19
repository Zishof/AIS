package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
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

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GrupPertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.streaming.AudioPertemuan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyPanel;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AudioGrupPertemuanHelper implements DataLoader {

	private MyGrid grid;
	private GrupPertemuan grupPertemuan;
	private Boolean delete = false;
	private Tab tab;

	public AudioGrupPertemuanHelper(Boolean delete) {
		this.delete = delete;
	}

	class DetailGrupPertemuanRenderer extends ais.ui.util.MyRowRenderer {

		HttpServletRequest request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
		Session session = HibernateUtil.currentSession();
		Tbmuser user = Common.getCurrentUser();

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final AudioPertemuan audioPertemuan = (AudioPertemuan) data;
			GrupPertemuan grupPertemuan = (GrupPertemuan) (audioPertemuan.getGrupPertemuan() == null ? null
					: session.createCriteria(GrupPertemuan.class)
							.add(Restrictions.idEq(audioPertemuan.getGrupPertemuan())).uniqueResult());

			String link = audioPertemuan.getLink();
			if (audioPertemuan != null
					&& (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
				link = audioPertemuan.createLinkUri();
				if (link != null) {
					// link = link.replaceAll("download=false", "download=true");
				}
			}

			String htmlContent = "<object type=\"application/x-shockwave-flash\" data=\"" + request.getContextPath()
					+ "/component/audioplayer/player.swf\" id=\"audioplayer1\" width=\"100%\" height=\"24\"> <param name=\"movie\" value=\"swf/player.swf\"> "
					+ "  <param name=\"FlashVars\" value=\"playerID=1&amp;bg=000000&amp;leftbg=efefef&amp;lefticon=000000&amp;rightbg=efefef&amp;rightbghover=0x999999&amp;righticon=000000&amp;righticonhover=0xffffff&amp;text=FFFFFF&amp;slider=000000&amp;track=000000&amp;border=000000&amp;loader=dad702&amp;loop=no&amp;autostart=no&amp;soundFile="
					+ link + "\""
					+ "<param name=\"quality\" value=\"high\"><param name=\"menu\" value=\"false\"><param name=\"wmode\" value=\"transparent\"></object>";

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.appendChild(new ais.ui.util.MyHtml(htmlContent));
			detail.setHeight("40px");
			detail.setOpen(true);

			if (audioPertemuan.getLink() == null) {
				Vbox vbox = new Vbox();
				vbox.setParent(row);
				new Label(audioPertemuan.getNama()).setParent(vbox);
				new Label(audioPertemuan.getKeterangan()).setParent(vbox);
				new Label(audioPertemuan.getType()).setParent(vbox);

				new Label(audioPertemuan.getTanggal_dirubah() == null ? ""
						: Common.dateFormat3.get().format(audioPertemuan.getTanggal_dirubah())).setParent(vbox);
			} else {
				A a;
				(a = new A(audioPertemuan.getLink())).setParent(row);
				a.setTarget("_blank");
				a.setHref(audioPertemuan.getLink());
			}

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			new Label(grupPertemuan == null ? "" : grupPertemuan.getNama()).setParent(vbox);

			if (user != null && user.getMahasiswa() != null) {
				new MyLabelBold(audioPertemuan.getKeteranganTambahan()).setParent(vbox);
			} else {
				final Textbox keteranganTambahan = new Textbox(audioPertemuan.getKeteranganTambahan());
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
							audioPertemuan.setKeteranganTambahan(keteranganTambahan.getValue());
							session.getTransaction().begin();
							Common.refreshUpdate(session, (audioPertemuan));
							session.getTransaction().commit();
							StreamingHibernateUtil.getInstance().closeSession();

						} catch (Exception e) {
							StreamingHibernateUtil.getInstance().rollbackTransaction();
							Common.tampilErrorJikaAdmin(e);
						}

					}
				});
			}

			if (audioPertemuan.getLink() == null) {
				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();

				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Download",
						audioPertemuan.iconDonwload());

				toolbarbutton.setOrient("vertical");
				aksiButtons.add(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {

						Filedownload.save(audioPertemuan.ambilFile(), audioPertemuan.getType());
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
												Common.refreshDelete((audioPertemuan));
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}

				});
				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

			} else {
				final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
						new java.util.ArrayList<org.zkoss.zk.ui.Component>();

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
												Common.refreshDelete((audioPertemuan));
												session.getTransaction().commit();

												StreamingHibernateUtil.getInstance().closeSession();
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												StreamingHibernateUtil.getInstance().rollbackTransaction();
												PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
											}

										}

									}
								});

					}

				});
				aksiButtons.add(button);

				ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		try {
			Session session = StreamingHibernateUtil.getInstance().currentSession();

			List<AudioPertemuan> audioPertemuans = session.createCriteria(AudioPertemuan.class)
					.addOrder(Order.asc("id")).add(Restrictions.eq("grupPertemuan", grupPertemuan.getId())).list();

			if (tab != null) {
				tab.setLabel("Audio (" + audioPertemuans.size() + " audio)");
			}

			ListModel strset = new SimpleListModel(audioPertemuans);
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
		panel.setTitle("Daftar Audio");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(panel);
		toolbar.setVisible(grupPertemuan != null);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Audio (Maks 10 mb)", "/img/new.gif");
		button.setUpload("true,maxsize=" + (1024 * 10));
		button.setVisible(delete);
		button.addEventListener("onUpload", new EventListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onEvent(Event event) throws Exception {
				UploadEvent uploadEvent = (UploadEvent) event;

				Media media = uploadEvent.getMedia();if(!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))return;

				if (!media.getName().toLowerCase().endsWith(".mp3")) {
					MyMessageboxConfig.show("Mohon maaf, format file audio tidak sesuai. Langkah yang dapat dilakukan: (1) pastikan file audio berformat MP3; (2) konversi file ke format MP3 jika perlu; (3) ulangi proses upload. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

				try {
					Session session = StreamingHibernateUtil.getInstance().currentSession();
					AudioPertemuan audioPertemuan = new AudioPertemuan();
					audioPertemuan.setFoto(new javax.sql.rowset.serial.SerialBlob(media.getByteData()));
					
					
					try {
						audioPertemuan.setNama(media.getName());
						audioPertemuan.setKeterangan(media.getFormat());
						audioPertemuan.setType(media.getContentType());
					}catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/AudioGrupPertemuanHelper.java:338");
					}
					
					audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
					audioPertemuan.setOleh(Common.getCurrentUser().getUserId());
			

					if (grupPertemuan != null) {
						audioPertemuan.setGrupPertemuan(grupPertemuan.getId());
					}

					session.getTransaction().begin();
					session.save(audioPertemuan);
					session.getTransaction().commit();

					StreamingHibernateUtil.getInstance().closeSession();

					loadData(null);
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
					Common.tampilErrorJikaAdmin(e);
				}

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Tambah Link Audio", "/img/new.gif");
		button.setVisible(delete);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				final MyWindow myWindow = new MyWindow("Tambah link Audio", "none", true);
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
				row.appendChild(new Label(ais.common.Common.getBahasaConfig("Link Audio")));
				final Textbox isi;
				row.appendChild(isi = new Textbox());
				isi.setValue("http://");
				isi.setWidth("90%");
				isi.setRows(3);
				isi.select();

				Common.initKeterangan(rows,
						"Link harus berupa MP3, contoh link : https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3");

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
							AudioPertemuan audioPertemuan = new AudioPertemuan();
							audioPertemuan.setKeterangan("link");
							audioPertemuan.setType("link");
							audioPertemuan.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
							audioPertemuan.setOleh(Common.getCurrentUser().getUserId());
							audioPertemuan.setNama("link");
							audioPertemuan.setLink(isi.getValue().trim());

							if (grupPertemuan != null) {
								audioPertemuan.setGrupPertemuan(grupPertemuan.getId());
							}

							Session session = StreamingHibernateUtil.getInstance().currentSession();
							session.getTransaction().begin();
							session.save(audioPertemuan);
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

		grid = new MyGrid();
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("0px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Audio");
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
