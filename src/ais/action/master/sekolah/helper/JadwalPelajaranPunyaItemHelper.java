package ais.action.master.sekolah.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
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
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import com.google.api.services.books.model.Volume;

import ais.action.master.helper.RevisiHelper;
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
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoImagePerHalamanItem;
import ais.database.model.library.Item;
import ais.database.model.library.SaldoAwalDetail;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPelajaranPunyaItem;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class JadwalPelajaranPunyaItemHelper implements DataLoader {

	private MyGrid grid;
	private JadwalPelajaran jadwalPelajaran;
	private Component component;

	private Tbmuser tbmuser;
	private String sqltambahan = "false";

	public JadwalPelajaranPunyaItemHelper() {
		tbmuser = Common.getCurrentUser();
	}

	class DetailJadwalPelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final JadwalPelajaranPunyaItem jadwalPelajaranPunyaItem = (JadwalPelajaranPunyaItem) data;
			final Item item = jadwalPelajaranPunyaItem.getItem();
			Image image = LibraryUtil.generateImage(jadwalPelajaranPunyaItem.getItem());
			image.setWidth("100%");
			image.setParent(row);

			new Label(item.getIsbn() + " " + item.getIssn()).setParent(row);

			RevisiHelper.createNewRevisi(SaldoAwalDetail.class, jadwalPelajaranPunyaItem,
					jadwalPelajaranPunyaItem.getItem() == null ? "" : jadwalPelajaranPunyaItem.getItem().getNama())
					.setParent(row);

			new Label(item.getPengarangs()).setParent(row);

			new Label(item.getPenerbit() == null ? "" : item.getPenerbit().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			JadwalPelajaran jadwalPelajaran = jadwalPelajaranPunyaItem.getJadwalPelajaran();
			if (jadwalPelajaran != null) {
				Common.displayGuruJadwalPelajaran(vbox, jadwalPelajaran, true);

				Vbox a = RevisiHelper.createNewRevisi(JadwalPelajaran.class, jadwalPelajaran,
						jadwalPelajaran.getMatapelajaran().getKode() + "-"
								+ jadwalPelajaran.getMatapelajaran().getNama());
				a.setParent(vbox);

				Common.displayHariJamRuanganJadwalPelajaranUmum(vbox, jadwalPelajaran);
			}

			if (tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null) {
				final Textbox keterangan = new Textbox(jadwalPelajaranPunyaItem.getKeterangan());
				keterangan.setRows(2);
				keterangan.setWidth("97%");
				keterangan.setParent(vbox);
				keterangan.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						jadwalPelajaranPunyaItem.setKeterangan(keterangan.getValue().trim());
						Common.refreshUpdate(jadwalPelajaranPunyaItem);

					}
				});
			} else {
				new Label(jadwalPelajaranPunyaItem.getKeterangan()).setParent(vbox);
			}

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
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/helper/JadwalPelajaranPunyaItemHelper.java:167");

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

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setVisible(Common.getCurrentUser().getMahasiswa() == null);
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
											Common.refreshDelete(jadwalPelajaranPunyaItem);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
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

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		Session session = HibernateUtil.currentSession();
		List<JadwalPelajaranPunyaItem> jadwalPelajaranPunyaItem = session.createCriteria(JadwalPelajaranPunyaItem.class)
				.addOrder(Order.asc("id")).add(jadwalPelajaran == null ? Restrictions.sqlRestriction(sqltambahan)
						: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
				.list();

		if (component instanceof Tabpanel) {
			((Tabpanel) component).getLinkedTab().setLabel("Buku Referensi "
					+ (jadwalPelajaranPunyaItem.size() == 0 ? "" : "(" + jadwalPelajaranPunyaItem.size() + ")"));
		}

		ListModel strset = new SimpleListModel(jadwalPelajaranPunyaItem);
		grid.setRowRenderer(new DetailJadwalPelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display(final String sqltambahan, final Component component) {
		this.sqltambahan = sqltambahan;
		display(jadwalPelajaran, component);
	}

	public void display(final JadwalPelajaran jadwalPelajaran, final Component component) {
		this.jadwalPelajaran = jadwalPelajaran;
		Common.clear(component);
		this.component = component;
		tbmuser = Common.getCurrentUser();
		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(new MyCaptionStyled("Daftar Referensi Perpustakaan"));

			Toolbar toolbar = new Toolbar();
			toolbar.setVisible(tbmuser != null && jadwalPelajaran != null && tbmuser.getSiswa() == null);
			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Referensi", "/img/new.gif");
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					List<Item> items = HibernateUtil.currentSession().createCriteria(JadwalPelajaranPunyaItem.class)
							.add(Restrictions.eq("jadwalPelajaran", jadwalPelajaran))
							.setProjection(Projections.property("item")).list();
					AmbilDataItemBanyak ambilDataItemBanyak = new AmbilDataItemBanyak(items);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataItemBanyak);
					ambilDataItemBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Item> items = (List<Item>) arg0.getData();
							for (Item item : items) {
								JadwalPelajaranPunyaItem jadwalPelajaranPunyaItem = new JadwalPelajaranPunyaItem();
								jadwalPelajaranPunyaItem.setItem(item);
								jadwalPelajaranPunyaItem.setKeterangan("");
								jadwalPelajaranPunyaItem.setJadwalPelajaran(jadwalPelajaran);
								Common.refreshSaveOrUpdate(jadwalPelajaranPunyaItem);
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
			toolbar.setVisible(tbmuser != null && jadwalPelajaran != null && tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataDariGoogleBookBanyak ambilDataDariGoogleBookBanyak = new AmbilDataDariGoogleBookBanyak(
							jadwalPelajaran != null && jadwalPelajaran.getMatapelajaran() != null
									? jadwalPelajaran.getMatapelajaran().getNama()
									: "");
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
								JadwalPelajaranPunyaItem jadwalPelajaranPunyaItem = new JadwalPelajaranPunyaItem();
								jadwalPelajaranPunyaItem.setItem(item);
								jadwalPelajaranPunyaItem.setKeterangan("");
								jadwalPelajaranPunyaItem.setJadwalPelajaran(jadwalPelajaran);
								Common.refreshSaveOrUpdate(jadwalPelajaranPunyaItem);
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

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

		} else {
			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(10);grid.getPagingChild().setMold("os");
			grid.setParent(component);
		}

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
		column.setWidth(tbmuser == null ? "0%" : "15%");

		loadData(null);

	}

}
