package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import ais.ui.util.MyCaptionStyled;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.google.api.services.books.model.Volume;

import ais.action.master.library.ItemAction;
import ais.action.master.library.helper.AmbilDataDariGoogleBookBanyak;
import ais.action.master.library.helper.AmbilDataItemBanyak;
import ais.action.master.library.helper.TampilanHasilScanPerHalamanWindow;
import ais.action.master.library.util.LibraryUtil;
import ais.action.servlet.CheckISBN;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.DataPunyaItem;
import ais.database.model.JadwalUjianPMB;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.library.Item;
import ais.database.model.pkl.KelompokPkl;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class DataPunyaItemHelper implements DataLoader {

	private MyGrid grid;
	private Skripsi skripsi;
	private Textbox cari;

	private Paging paging;
	private Tbmuser tbmuser;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private JadwalUjianPMB jadwalUjianPMB;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;

	public DataPunyaItemHelper() {

		tbmuser = Common.getCurrentUser();
	}

	class DetailSkripsiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final DataPunyaItem dataPunyaItem = (DataPunyaItem) data;
			final Item item = dataPunyaItem.getItem();
			Image image = LibraryUtil.generateImage(dataPunyaItem.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(item.getIsbn() + " " + item.getIssn()).setParent(row);

			Vbox a;
			(a = RevisiHelper.createNewRevisi(DataPunyaItem.class, dataPunyaItem,
					dataPunyaItem.getItem() == null ? "" : dataPunyaItem.getItem().getNama())).setParent(row);
			a.appendChild(new Label(item.getSubjects()));

			new Label(item.getPengarangs()).setParent(row);

			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(row);

			final Textbox keterangan = new Textbox(dataPunyaItem.getKeterangan());
			keterangan.setRows(2);
			keterangan.setWidth("97%");
			keterangan.setParent(row);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					dataPunyaItem.setKeterangan(keterangan.getValue().trim());
					Common.refreshUpdate(dataPunyaItem);

				}
			});

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kutipan", "/img/eye-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Kutipan Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					ItemAction.tampilkanKutipan(item);
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Google", "/img/Apps-Google-Play-Books-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Baca Buku via Google");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					JSONObject jsonObject = new JSONObject(item.getInfoLain()).getJSONObject("volumeInfo");
					if (Common.isMobile()) {
						ExecutionsCtrl.getCurrent().sendRedirect(jsonObject.getString("previewLink"), "_blank");
					} else {
						Clients.evalJavaScript("popupCenter({url: '" + jsonObject.getString("previewLink")
								+ "', title: 'Book', w: 1200, h: 600});");
					}

				}

			});
			aksiButtons.add(button);
			button.setVisible(item.getGoogleBookId() != null && !item.getGoogleBookId().trim().isEmpty());

			button = new MyToolbarbuttonConfig("Baca", "/img/Book-icon.png");
			button.setOrient("vertical");
			button.setTooltiptext("Lihat Isi Buku");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					TampilanHasilScanPerHalamanWindow halamanWindow = new TampilanHasilScanPerHalamanWindow("Isi Buku",
							"none", true);

					halamanWindow.init(item);
					try {
						ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(halamanWindow);
						halamanWindow.onModal();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/DataPunyaItemHelper.java:160");

					}
				}

			});
			aksiButtons.add(button);

			Session session = StreamingHibernateUtil.getInstance().currentSession();

			int qty = ((Number) session.createCriteria(FotoImagePerHalamanItem.class)
					.add(Restrictions.eq("item", item.getId())).setProjection(Projections.rowCount()).uniqueResult())
							.intValue();

			StreamingHibernateUtil.getInstance().closeSession();

			button.setVisible(qty > 0);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setOrient("vertical");
			button.setVisible(tbmuser != null);
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
											Common.refreshDelete(dataPunyaItem);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
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

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(DataPunyaItem.class)

				.createAlias("item", "item")
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("item.pengarangs", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("item.nama", cari.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("item.isbn", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
						Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
								Restrictions.or(
										Restrictions.or(
												Restrictions.eq("mahasiswaRequestTugasAkhir",
														mahasiswaRequestTugasAkhir),
												Restrictions.eq("skripsi", skripsi)),
										Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))));

		if (order) {
			crit.addOrder(Order.asc("id"));
		}
		return crit;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Common.initPagingCustom(initCriteria(false), paging, 10);
		List<DataPunyaItem> dataPunyaItem = initCriteria(true).setMaxResults(10)
				.setFirstResult(10 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(dataPunyaItem);
		grid.setRowRenderer(new DetailSkripsiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final Skripsi skripsi, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final JadwalUjianPMB jadwalUjianPMB, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Tabpanel component) {
		this.skripsi = skripsi;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.jadwalUjianPMB = jadwalUjianPMB;
		Common.clear(component);
		paging = new Paging();
		Common.initPagingCustom(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		}, 10);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(new MyCaptionStyled("Daftar Buku Referensi"));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Buku Referensi", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<Item> items = HibernateUtil.currentSession().createCriteria(DataPunyaItem.class)
						.add(Restrictions.or(Restrictions.eq("kelompokPkl", kelompokPkl),
								Restrictions.or(Restrictions.eq("kelompokKkn", kelompokKkn),
										Restrictions.or(
												Restrictions.or(
														Restrictions.eq("mahasiswaRequestTugasAkhir",
																mahasiswaRequestTugasAkhir),
														Restrictions.eq("skripsi", skripsi)),
												Restrictions.eq("jadwalUjianPMB", jadwalUjianPMB)))))
						.setProjection(Projections.property("item")).list();
				AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
				ambilDataItemBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Item> items = (List<Item>) arg0.getData();
						for (Item item : items) {
							DataPunyaItem dataPunyaItem = new DataPunyaItem();
							dataPunyaItem.setItem(item);
							dataPunyaItem.setKeterangan("");
							dataPunyaItem.setJadwalUjianPMB(jadwalUjianPMB);
							dataPunyaItem.setSkripsi(skripsi);
							dataPunyaItem.setKelompokKkn(kelompokKkn);
							dataPunyaItem.setKelompokPkl(kelompokPkl);
							dataPunyaItem.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
							Common.refreshSaveOrUpdate(dataPunyaItem);
						}

						loadData(null);
					}
				});
				ambilDataItemBanyak.setWidth("97%");
				ambilDataItemBanyak.setHeight("97%");
				ambilDataItemBanyak.setVisible(true);
				ambilDataItemBanyak.onModal();

			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ambil Google Book", "/img/Apps-Google-Play-Books-icon.png");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak("");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
						.appendChild(ambilDataDariGoogleBookBanyak);
				ambilDataDariGoogleBookBanyak.setHeight("95%");
				ambilDataDariGoogleBookBanyak.setWidth("90%");

				ambilDataDariGoogleBookBanyak.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						List<Object> objects = (List<Object>) arg0.getData();
						for (Object object : objects) {
							Item item = (object instanceof Item) ? (Item) object
									: (object instanceof ais.database.model.library.ItemTemporary)
										? CheckISBN.itemDariItemTemporary((ais.database.model.library.ItemTemporary) object)
										: CheckISBN.simpanVolume((Volume) object, new Item());
							DataPunyaItem dataPunyaItem = new DataPunyaItem();
							dataPunyaItem.setItem(item);
							dataPunyaItem.setKeterangan("");
							dataPunyaItem.setJadwalUjianPMB(jadwalUjianPMB);
							dataPunyaItem.setSkripsi(skripsi);
							dataPunyaItem.setKelompokKkn(kelompokKkn);
							dataPunyaItem.setKelompokPkl(kelompokPkl);
							dataPunyaItem.setMahasiswaRequestTugasAkhir(mahasiswaRequestTugasAkhir);
							Common.refreshSaveOrUpdate(dataPunyaItem);
						}

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								loadData(null);
							}
						});
					}
				});

				ambilDataDariGoogleBookBanyak.onModal();

			}

		});
		button.setParent(toolbar);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new MyLabelConfig("Cari : "));
		toolbar.appendChild(cari = new Textbox());
		cari.setCols(15);
		cari.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		button = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode/ISBN/ISSN");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama/Judul");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengarang");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Penerbit");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		loadData(null);

		paging.setParent(groupbox);
	}

}
