package ais.action.master.library.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.books.model.Volume;
import com.google.api.services.books.model.Volumes;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.util.BooksSample;
import ais.action.master.library.util.LibraryUtil;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.library.ItemTemporary;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataDariGoogleBookBanyak extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	public JsonFactory jsonFactory = new JacksonFactory();

	private Set<Volume> ids = new HashSet<Volume>();
	private Set<String> isbn13s = new HashSet<String>();
	private List<ItemTemporary> itemTemporaryselected = new ArrayList<ItemTemporary>();
	private String katakunci = "";

	private boolean terintegrasi_dengan_google_book_baru = Common.bolehKonfigurasi("terintegrasi_dengan_google_book_baru", Konfigurasi.TIDAK_AKTIF);

	public AmbilDataDariGoogleBookBanyak(String katakunci) {
		super();
		this.katakunci = katakunci;
		try {
			display();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:89");
		}
	}

	private MyTextbox author;
	private MyTextbox isbn;
	private MyTextbox title;
	private MyTextbox publisher;
	private MyTextbox subject;

	private Paging paging;
	private MyGrid gridSebelumnya;
	private MyTabConfig tab1;
	private MyTabConfig tab1AsistenMahasiswa;

	class ItemTemporaryRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			arg0.setValign("top");
			try {
				final Volume volume = (Volume) arg1;

				if (!title.getValue().trim().isEmpty() || (katakunci != null && !katakunci.trim().isEmpty())) {
					new Thread(new Runnable() {

						@Override
						public void run() {
							CheckISBN.simpanVolume(volume, new ItemTemporary(),
									title.getValue().trim().isEmpty() ? katakunci.trim() : title.getValue().trim());
						}
					}).start();
				}

				final Volume.VolumeInfo volumeInfo = volume.getVolumeInfo();

				String isbn10 = "";
				try {

					isbn10 = volumeInfo.getIndustryIdentifiers().get(0).getIdentifier();

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:132");
					// Common.tampilErrorJikaAdmin(e);
				}

				String isbn13 = "";
				try {
					isbn13 = volumeInfo.getIndustryIdentifiers().get(1).getIdentifier();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:139");
					// Common.tampilErrorJikaAdmin(e);
				}

				String lain = "";
				try {
					lain = volumeInfo.getIndustryIdentifiers().get(2).getIdentifier();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:146");

				}

				arg0.setAttribute("volume", volume);

				final MyCheckboxConfig checkbox = new MyCheckboxConfig();
				checkbox.setParent(arg0);
				arg0.setAttribute("checkbox", checkbox);

				checkbox.setChecked(ids.contains(volume));

				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						String isbn13 = "";
						try {
							isbn13 = volumeInfo.getIndustryIdentifiers().get(1).getIdentifier();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:162");
						}
						if (checkbox.isChecked()) {
							ids.add(volume);
							isbn13s.add(isbn13);
						} else {
							ids.remove(volume);
							isbn13s.remove(isbn13);
						}
					}
				});

				String imageUrl = "";
				try {
					imageUrl = volumeInfo.getImageLinks().getThumbnail();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:176");
					// TODO: handle exception
				}

				String title = "";
				try {
					title = volumeInfo.getTitle();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:183");
					// TODO: handle exception
				}

				String description = "";
				try {
					description = volumeInfo.getDescription();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:190");
					// TODO: handle exception
				}

				if (description == null || description.equals("")) {
					try {
						description = volume.getSearchInfo().getTextSnippet();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:197");
						// TODO: handle exception
					}
				}

				String publisher = "";
				try {
					publisher = volumeInfo.getPublisher();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:205");
					// TODO: handle exception
				}

				String autors = "";
				try {
					autors = volumeInfo.getAuthors() != null
							? volumeInfo.getAuthors().toString().replaceAll("\\[", "").replaceAll("\\]", "")
							: "";
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:212");
					// TODO: handle exception
				}

				Image image = new Image(imageUrl);
				image.setWidth("100%");
				image.setParent(arg0);

				Vbox vbox = new Vbox();
				vbox.setParent(arg0);
				new Label(isbn10 + " " + isbn13 + " " + lain).setParent(vbox);
				MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Isi Buku",
						"/img/Apps-Google-Play-Books-icon.png");
				myButtonConfig.setParent(vbox);
				myButtonConfig.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (Common.isMobile()) {
							ExecutionsCtrl.getCurrent().sendRedirect(volumeInfo.getPreviewLink(), "_blank");
						} else {
							Clients.evalJavaScript("popupCenter({url: '" + volumeInfo.getPreviewLink()
									+ "', title: 'Book', w: 1200, h: 600});");
						}
					}
				});

				new Label(title).setParent(arg0);
				new Label(publisher).setParent(arg0);
				new Label(autors).setParent(arg0);
				new ais.ui.util.MyHtml(
						"<div style='font-size:8px'>" + (description == null ? "" : description) + "</div>")
						.setParent(arg0);
			} catch (Exception e) {
				arg0.setVisible(false);
				Common.tampilErrorJikaAdmin(e);
			}
		}

	}

	@SuppressWarnings("deprecation")
	public void display() throws Exception {

		// Rapikan tampilan dialog: beri judul + bingkai + tombol tutup (X) + posisikan di tengah.
		setTitle("Ambil Buku dari Google Book");
		setBorder("normal");
		setClosable(true);
		setSclass("ais-googlebook-dialog");
		try {
			setPosition("center");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/library/helper/AmbilDataDariGoogleBookBanyak.java:263");
			// abaikan bila mold tidak mendukung penempatan
		}
		addEventListener(org.zkoss.zk.ui.event.Events.ON_CLOSE, new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				AmbilDataDariGoogleBookBanyak.this.detach();
			}
		});

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		Grid searchgrid = new Grid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		if (!Common.isMobile()) {
			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "10");
			row.appendChild(new MyLabelBoldAja(
					"Untuk mengambil buku dari google book, masukkan salah satu atau beberapa kata kunci pencarian berikut :"));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode/ISBN"));
			row.appendChild(isbn = new MyTextbox());
			isbn.setWidth("90%");
			isbn.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
			row.appendChild(title = new MyTextbox());
			title.setWidth("90%");
			title.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Pengarang"));
			row.appendChild(author = new MyTextbox());
			author.setWidth("90%");
			author.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
			row.appendChild(publisher = new MyTextbox());
			publisher.setWidth("90%");
			publisher.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Subjek"));
			row.appendChild(subject = new MyTextbox());
			subject.setWidth("90%");
			subject.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "10");
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(row);

		} else {

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("30%");

			column = new MyColumnConfig();
			column.setParent(columns);

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.appendChild(new MyLabelBoldAja(
					"Untuk mengambil buku dari google book, masukkan beberapa kata kunci pencarian berikut :"));

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Kode/ISBN"));
			row.appendChild(isbn = new MyTextbox());
			isbn.setWidth("90%");
			isbn.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Judul"));
			row.appendChild(title = new MyTextbox());
			title.setWidth("90%");
			title.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Pengarang"));
			row.appendChild(author = new MyTextbox());
			author.setWidth("90%");
			author.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit"));
			row.appendChild(publisher = new MyTextbox());
			publisher.setWidth("90%");
			publisher.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Subjek"));
			row.appendChild(subject = new MyTextbox());
			subject.setWidth("90%");
			subject.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onSearchDefault(null);
				}
			});
			button.setParent(row);
		}

		Borderlayout borderlayoutsub = new ais.ui.util.MyBorderlayout();
		borderlayoutsub.setParent(center);
		Center centersub = new Center();
		centersub.setParent(borderlayoutsub);
		ais.ui.util.ZkCompat.setFlex(centersub, true);

		South southsub = new South();
		southsub.setParent(borderlayoutsub);
		paging = new Paging();
		paging.setMold("os");
		southsub.appendChild(paging);
		paging.setPageSize(5);
		paging.setVisible(false);
		paging.addEventListener("onPaging", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (!terintegrasi_dengan_google_book_baru) {
			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(centersub);

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Gambar");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Isbn");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Judul");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penerbit");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengarang");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Deskripsi");
			column.setWidth("35%");
		} else {

			Tabbox tabbox = new Tabbox();
			tabbox.setParent(centersub);

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			tab1AsistenMahasiswa = new MyTabConfig();
			tab1AsistenMahasiswa.setSelected(true);
			tab1AsistenMahasiswa.setParent(tabs);
			tab1AsistenMahasiswa.setLabel("Daftar buku yang sebelumnya sudah diambil");

			tab1 = new MyTabConfig();
			tab1.setParent(tabs);
			tab1.setLabel("Cari buku lain di Google Book");
			tab1.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					paging.setVisible(false);
					if ((katakunci != null && !katakunci.trim().isEmpty()) || !title.getValue().trim().isEmpty()) {
						onSearchDefault(arg0);
					}
				}
			});

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel managemenPenilaian = new ais.ui.util.MyTabpanel();
			managemenPenilaian.setParent(tabpanels);

			MyBorderlayout borderlayoutsubsub = new ais.ui.util.MyBorderlayout();
			borderlayoutsubsub.setParent(managemenPenilaian);
			Center centersubsub = new Center();
			centersubsub.setParent(borderlayoutsubsub);
			ais.ui.util.ZkCompat.setFlex(centersubsub, true);

			gridSebelumnya = new MyGrid();
			gridSebelumnya.setWidth("100%");
			gridSebelumnya.setMold("paging");
			gridSebelumnya.setPageSize(1000);
			gridSebelumnya.setParent(centersubsub);

			Columns columns = new Columns();
			columns.setParent(gridSebelumnya);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Gambar");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Isbn");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Judul");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penerbit");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengarang");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Deskripsi");
			column.setWidth("35%");

			managemenPenilaian = new ais.ui.util.MyTabpanel();
			managemenPenilaian.setParent(tabpanels);

			borderlayoutsubsub = new ais.ui.util.MyBorderlayout();
			borderlayoutsubsub.setParent(managemenPenilaian);
			centersubsub = new Center();
			centersubsub.setParent(borderlayoutsubsub);
			ais.ui.util.ZkCompat.setFlex(centersubsub, true);

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(centersubsub);

			columns = new Columns();
			columns.setParent(grid);

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("");
			column.setWidth("40px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Gambar");
			column.setWidth("70px");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Isbn");
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Judul");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Penerbit");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Pengarang");
			column.setWidth("10%");

			column = new MyColumnConfig();
			column.setParent(columns);
			column.setLabel("Deskripsi");
			column.setWidth("35%");
		}

		South south = new South();
		south.setHeight("54px");
		south.setStyle("border-top:1px solid var(--theme-border,#e2e8f0);background:var(--theme-light,#f8fafc);");
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("padding:8px 12px;");
		// Saat tertutup, North hanya menampilkan baris instruksi → pakai tinggi RAPAT ("64px")
		// agar TIDAK ada ruang kosong besar antara instruksi dan daftar buku (sesuai keluhan).
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar, "64px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.setStyle("margin-left:8px;");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataDariGoogleBookBanyak.this.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Pilih Buku", "/img/save.gif");
		button.setTooltiptext("Simpan buku yang dicentang ke materi");
		button.setStyle("margin-left:8px;font-weight:600;");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(final Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (eventListener != null) {
							List<Object> volumes = new ArrayList<Object>();
							if (grid != null && grid.getRows() != null) {
								List<Row> rows = grid.getRows().getChildren();
								for (Row row : rows) {
									if (row.getAttribute("checkbox") != null) {
										MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

										if (checkbox.isChecked() && !checkbox.isDisabled()) {
											Volume myItemTemporary = (Volume) row.getAttribute("volume");
											volumes.add(myItemTemporary);
										}
									}
								}
							}

							if (gridSebelumnya != null && gridSebelumnya.getRows() != null) {
								List<Row> rows = gridSebelumnya.getRows().getChildren();
								for (Row row : rows) {
									if (row.getAttribute("checkbox") != null) {
										MyCheckboxConfig checkbox = (MyCheckboxConfig) row.getAttribute("checkbox");

										if (checkbox.isChecked() && !checkbox.isDisabled()) {
											ItemTemporary itemTemporary = (ItemTemporary) row
													.getAttribute("itemTemporary");
											volumes.add(itemTemporary);
										}
									}
								}
							}

							System.out.println("volumes => " + volumes);

							Event myEvent = new Event("myEvent", event.getTarget(), volumes);
							eventListener.onEvent(myEvent);
						}
						AmbilDataDariGoogleBookBanyak.this.detach();
					}
				});
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);
	}

	private void prosesGogleBook() throws Exception {
		final List<Volume> volumes = new ArrayList<Volume>();
		volumes.addAll(ids);

		String query = "";

		if (!isbn.getValue().trim().equals("")) {
			String prefix = null;
			prefix = "isbn:";
			query += query.equals("") ? (prefix + isbn.getValue().trim()) : ", " + (prefix + isbn.getValue().trim());
		} else {

			if (!author.getValue().trim().equals("")) {
				String prefix = null;
				prefix = "inauthor:";
				query += query.equals("") ? (prefix + author.getValue().trim())
						: ", " + (prefix + author.getValue().trim());
			}

			if (!title.getValue().trim().equals("")) {
				String prefix = null;
				prefix = "intitle:";
				query += query.equals("") ? (prefix + title.getValue().trim())
						: ", " + (prefix + title.getValue().trim());
			} else if (katakunci != null && !katakunci.trim().isEmpty()) {
				String prefix = null;
				prefix = "intitle:";
				query += query.equals("") ? (prefix + katakunci.trim()) : ", " + (prefix + katakunci.trim());
			}

			if (!publisher.getValue().trim().equals("")) {
				String prefix = null;
				prefix = "inpublisher:";
				query += query.equals("") ? (prefix + publisher.getValue().trim())
						: ", " + (prefix + publisher.getValue().trim());
			}

			if (!subject.getValue().trim().equals("")) {
				String prefix = null;
				prefix = "subject:";
				query += query.equals("") ? (prefix + subject.getValue().trim())
						: ", " + (prefix + subject.getValue().trim());
			}
		}

		if (!query.trim().isEmpty()) {

			int sekaliAmbil = 35;
			Volumes myvolumes = BooksSample.queryGoogleBooks(jsonFactory, query,
					sekaliAmbil * (paging == null ? 0 : paging.getActivePage()), sekaliAmbil);
			int total = myvolumes.getTotalItems();
			paging.setVisible(total > sekaliAmbil);
			paging.setPageSize(sekaliAmbil);
			paging.setTotalSize(total);
			if (paging.isVisible()) {
				((South) paging.getParent()).setHeight("25px");
			}
			if (myvolumes != null && myvolumes.getItems() != null) {
				for (final Volume volume : myvolumes.getItems()) {
					if (!ids.contains(volume)) {
						volumes.add(volume);
					}
				}
			}

		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				ListModel strset = new SimpleListModel(volumes);
				grid.setRowRenderer(new ItemTemporaryRenderer());
				grid.setModelCheckMobile(strset);
				grid.renderAll();
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) throws Exception {

		if (!terintegrasi_dengan_google_book_baru) {
			prosesGogleBook();
		} else {
			if (tab1.isSelected()) {
				prosesGogleBook();
			} else if (tab1AsistenMahasiswa.isSelected()) {
				// Pemuatan daftar "buku yang sebelumnya sudah diambil" DITUNDA via timer agar berjalan
				// SETELAH window ter-attach ke desktop. display() dipanggil di konstruktor (sebelum
				// appendChild), sehingga bila render dilakukan langsung, grid masih DETACHED dan data
				// TIDAK muncul. Pola sama persis dengan prosesGogleBook(). renderAll() memaksa baris
				// terender pada grid ber-mold paging.
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event ev) throws Exception {
						Common.initPaging(initCriteria(false), paging);
						List<ItemTemporary> itemTemporary = new ArrayList<ItemTemporary>();
						itemTemporary.addAll(itemTemporaryselected);

						List<ItemTemporary> myItemTemporary = initCriteria(true)
								.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
								.setFirstResult(
										Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
								.list();

						itemTemporary.addAll(myItemTemporary);

						ListModel strset = new SimpleListModel(itemTemporary);
						gridSebelumnya.setRowRenderer(new ItemTemporaryRendererBaru());
						gridSebelumnya.setModelCheckMobile(strset);
						gridSebelumnya.renderAll();
					}
				});
			}
		}
	}

	class ItemTemporaryRendererBaru extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemTemporary itemTemporary = (ItemTemporary) arg1;
			arg0.setAttribute("itemTemporary", itemTemporary);
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			for (ItemTemporary myItemTemporary : itemTemporaryselected) {
				if (myItemTemporary.getId().equals(itemTemporary.getId())) {
					checkbox.setChecked(true);
					checkbox.setDisabled(true);
					break;
				}
			}

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (checkbox.isChecked()) {
						itemTemporaryselected.add(itemTemporary);
					} else {
						itemTemporaryselected.remove(itemTemporary);
					}
				}
			});

			Image image = LibraryUtil.generateImage(itemTemporary);
			image.setWidth("100%");
			image.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(itemTemporary.getIsbn()).setParent(vbox);
			MyButtonConfig myButtonConfig = new MyButtonConfig("Lihat Isi Buku",
					"/img/Apps-Google-Play-Books-icon.png");
			myButtonConfig.setParent(vbox);
			myButtonConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					JSONObject jsonObject = new JSONObject(itemTemporary.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + jsonObject.getString("previewLink")
								+ "', title: 'Book', w: 1200, h: 600});");
					}
				}
			});

			RevisiHelper.createNewRevisi(ItemTemporary.class, itemTemporary, itemTemporary.getNama()).setParent(arg0);

			new Label(itemTemporary.getPenerbit() == null ? "" : itemTemporary.getPenerbit().getNama()).setParent(arg0);
			new Label(itemTemporary.getPengarangs()).setParent(arg0);
			new ais.ui.util.MyHtml("<div style='font-size:8px'>" + itemTemporary.getAbstrak() + "</div>")
					.setParent(arg0);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();

		List<Long> ids = new ArrayList<Long>();
		for (ItemTemporary itemTemporary : itemTemporaryselected) {
			ids.add(itemTemporary.getId());
		}

		Criteria criteria = session.createCriteria(ItemTemporary.class)
				.add(ids.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.not(Restrictions.in("id", ids)))

				.add(Restrictions.isNotNull("googleBookId")).add(Restrictions.ne("googleBookId", ""))

				.createAlias("penerbit", "penerbit", Criteria.LEFT_JOIN)

				.add(publisher.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("penerbit.nama", publisher.getValue().trim(), MatchMode.ANYWHERE))

				.add(subject.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kategories", subject.getValue().trim(), MatchMode.ANYWHERE))

				.add(author.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("pengarangs", author.getValue().trim(), MatchMode.ANYWHERE))

				.add(title.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", title.getValue().trim(), MatchMode.ANYWHERE))

				// Daftar "buku yang sebelumnya sudah diambil" TIDAK lagi disaring otomatis oleh
				// katakunci (nama matakuliah pembuka dialog). Sebelumnya filter kewords ILIKE katakunci
				// membuat daftar KOSONG untuk matakuliah yang belum punya buku ber-kewords cocok —
				// sehingga "data tidak tampil". Penyaringan tetap tersedia lewat kolom pencarian
				// eksplisit (Judul/Pengarang/Penerbit/ISBN/Subjek) yang diisi pengguna.
				.add(Restrictions.sqlRestriction("true"))

				.add(isbn.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("isbn", isbn.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("isbn10", isbn.getValue().trim(), MatchMode.ANYWHERE)));
		if (order) {
			criteria.addOrder(Order.asc("nama"));
		}

		return criteria;
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
