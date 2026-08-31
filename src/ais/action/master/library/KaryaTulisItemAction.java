package ais.action.master.library;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.East;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.AmbilDataTbmuserBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.library.helper.AmbilDataDomainPenelitianBanbox;
import ais.action.master.library.helper.AmbilDataFolderItemBanbox;
import ais.action.master.library.helper.AmbilDataPenerbitBanbox;
import ais.action.master.library.util.LibraryUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.library.ItemDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.FotoGambarItem;
import ais.database.model.file.FotoItem;
import ais.database.model.library.DomainPenelitian;
import ais.database.model.library.Item;
import ais.database.model.library.ItemPunyaBarcode;
import ais.database.model.library.ItemPunyaKategoriItem;
import ais.database.model.library.ItemPunyaPemeriksa;
import ais.database.model.library.ItemPunyaPengarang;
import ais.database.model.library.ItemPunyaTerbit;
import ais.database.model.library.JenisItem;
import ais.database.model.library.Penerbit;
import ais.database.model.library.PenerbitPunyaPemeriksa;
import ais.database.model.library.Perpustakaan;
import ais.database.model.library.StatusTerbitItem;
import ais.database.model.library.TipeItem;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk karya tulis item. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * ItemAction}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan
 * yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Radiogroup
 * statusTerbitItem}, {@code Combobox searchstatus}, {@code AmbilDataSatuanKerjaBanbox satuanKerjaBanbox}, {@code
 * AmbilDataSatuanKerjaBanbox satuanKerja}, {@code AmbilDataDomainPenelitianBanbox domainPenelitian}, {@code
 * boolean aapprove}, {@code boolean areject}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}, {@code initCriteriaPublikasi()});
 * pembacaan/pencarian ({@code onSearchDefault()}); mutasi data ({@code onSave()}); operasi domain lain ({@code
 * onAddExternal()}, {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see ItemAction
 */
public class KaryaTulisItemAction extends ItemAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4462319519095469547L;

	private MyGrid grid;
	private Radiogroup statusTerbitItem;
	private Combobox searchstatus;
	private AmbilDataSatuanKerjaBanbox satuanKerjaBanbox;
	private AmbilDataSatuanKerjaBanbox satuanKerja;
	private AmbilDataDomainPenelitianBanbox domainPenelitian;
	private boolean aapprove = false;
	private boolean areject = false;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataFolderItemBanbox parent;

	private Decimalbox urutan;

	private AmbilDataTbmuserBanbox dibuatOleh;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(
			org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,
			org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		aapprove = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		areject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		LibraryUtil.checkDirectory();
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		final MyComboitemConfig semua = new MyComboitemConfig("Semua");
		if (semua != null) { semua.setAttribute("value", null); }
		searchstatus.appendChild(semua);
		if (searchstatus != null) { searchstatus.setSelectedItem(semua); }

		final MyComboitemConfig draft = new MyComboitemConfig(LibraryUtil.DRAFT.getNama());
		if (draft != null) { draft.setAttribute("value", LibraryUtil.DRAFT); }
		searchstatus.appendChild(draft);

		final MyComboitemConfig approve = new MyComboitemConfig(LibraryUtil.APPROVE.getNama());
		if (approve != null) { approve.setAttribute("value", LibraryUtil.APPROVE); }
		searchstatus.appendChild(approve);

		final MyComboitemConfig reject = new MyComboitemConfig(LibraryUtil.REJECT.getNama());
		if (reject != null) { reject.setAttribute("value", LibraryUtil.REJECT); }
		searchstatus.appendChild(reject);

		final MyComboitemConfig publish = new MyComboitemConfig(LibraryUtil.PUBLISH.getNama());
		if (publish != null) { publish.setAttribute("value", LibraryUtil.PUBLISH); }
		searchstatus.appendChild(publish);

		satuanKerjaBanbox.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		onSearchDefault(null);
	        FilterLanjutHelper.setup(comp);
}

	class ItemRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final Item item = (Item) arg1;

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						MyWindow window = new MyWindow("", "none", false);
						window.setHeight("450px");
						window.setWidth("100%");
						window.setParent(detail);
						initDetail(item, window);
					}
				}
			});

			// RevisiHelper.createNewRevisi(Item.class, item, item.getNama())
			// .setParent(arg0);
			String text = "<font style=\"font-size: x-small;\">"
					+ item.getNama() + "<hr>" + item.getTema() + "</font>";
			new ais.ui.util.MyHtml(text).setParent(arg0);
			new Label(item.getDomainPenelitian() == null ? "" : item
					.getDomainPenelitian().getNama()).setParent(arg0);
			// new Label(item.getDibuatOleh() == null ? "" :
			// item.getDibuatOleh()
			// .toString()).setParent(arg0);

			RevisiHelper.createNewRevisi(
					Item.class,
					item,
					item.getDibuatOleh() == null ? "" : item.getDibuatOleh()
							.toString()).setParent(arg0);

			new Label(item.getStatusTerbitItem() == null ? "" : item
					.getStatusTerbitItem().getNama()).setParent(arg0);

			Session session = HibernateUtil.currentSession();
			List<ItemPunyaPemeriksa> pemeriksas = session
					.createCriteria(ItemPunyaPemeriksa.class)
					.add(Restrictions.isNotNull("pemeriksa"))
					.add(Restrictions.eq("item", item)).list();
			if (pemeriksas.size() != 0) {
				text = "<font style=\"font-size: x-small;\"><ol>";
				for (ItemPunyaPemeriksa itemPunyaPemeriksa : pemeriksas) {
					text += "<li>"
							+ itemPunyaPemeriksa.getPemeriksa().toString()
							+ " - <font style=\"font-size: x-small;color:blue;font-weight: bolder;\">"
							+ itemPunyaPemeriksa.getStatus() + "</font></li>";
				}
				text += "</ol></font>";
				new ais.ui.util.MyHtml(text).setParent(arg0);
			} else {
				new Label().setParent(arg0);
			}

			String directory = "";
			try {

				Item parentItem = item.getParent();
				while (parentItem != null) {
					directory = parentItem.getNama() + "/" + directory;
					parentItem = parentItem.getParent();
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}
			new ais.ui.util.MyHtml("<font style=\"font-size: x-small;\">" + directory
					+ "</font>").setParent(arg0);

			new Label(item.getPengarangs()).setParent(arg0);

			String penerbits = "<font style=\"font-size: x-small;\">";
			penerbits += item.getPenerbit() == null ? "" : item.getPenerbit()
					.getNama() + "<br>";
			penerbits += item.getPenerbit2() == null ? "" : item.getPenerbit2()
					.getNama() + "<br>";
			penerbits += item.getPenerbit3() == null ? "" : item.getPenerbit3()
					.getNama() + "<br>";
			penerbits += item.getPenerbit4() == null ? "" : item.getPenerbit4()
					.getNama() + "<br>";
			penerbits += item.getPenerbit5() == null ? "" : item.getPenerbit5()
					.getNama() + "<br>";
			penerbits += "</font>";

			new ais.ui.util.MyHtml(penerbits).setParent(arg0);
			new Label(item.getTanggal() == null ? ""
					: Common.dateFormat2.get().format(item.getTanggal()))
					.setParent(arg0);
			new Label(
					item.getStatusTerbitItem() != null
							&& item.getStatusTerbitItem().getId()
									.equals(LibraryUtil.PUBLISH.getId()) ? item
							.getTanggalterbit() == null ? ""
							: Common.dateFormat2.get().format(item.getTanggalterbit())
							: "").setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(item);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete
					&& !item.getStatusTerbitItem().getId()
							.equals(LibraryUtil.PUBLISH.getId()));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?",
							"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
							MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event)
										throws Exception {
									int i = new Integer(event.getData()
											.toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(item);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e); 
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}
	}

	public static void onAddExternal(Event event, EventListener eventListener,
			Item item) throws Exception {
		KaryaTulisItemAction karyaTulisItemAction = new KaryaTulisItemAction();
		karyaTulisItemAction.eventListener = eventListener;
		karyaTulisItemAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(karyaTulisItemAction.addWindow);
		karyaTulisItemAction.addWindow.setHeight("97%");
		karyaTulisItemAction.addWindow.setWidth("90%");

		karyaTulisItemAction.init(item);

		karyaTulisItemAction.addWindow.setVisible(true);
		karyaTulisItemAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new Item());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	public class StatusTerbitEventListener implements EventListener {

		private void selectback() {
			StatusTerbitItem statusTerbitItemLama = item.getStatusTerbitItem();
			if (statusTerbitItemLama.getId().equals(LibraryUtil.DRAFT.getId())) {
				statusTerbitItem.setSelectedItem(draft);
			} else if (statusTerbitItemLama.getId().equals(
					LibraryUtil.APPROVE.getId())) {
				statusTerbitItem.setSelectedItem(approve);
			} else if (statusTerbitItemLama.getId().equals(
					LibraryUtil.REJECT.getId())) {
				statusTerbitItem.setSelectedItem(reject);
			} else if (statusTerbitItemLama.getId().equals(
					LibraryUtil.PUBLISH.getId())) {
				statusTerbitItem.setSelectedItem(publish);
			}
		}

		public boolean onCheck() throws Exception {
			StatusTerbitItem statusTerbitItemLama = item.getStatusTerbitItem();
			StatusTerbitItem statusTerbitItemBaru = (StatusTerbitItem) (statusTerbitItem
					.getSelectedItem() == null ? null : statusTerbitItem
					.getSelectedItem().getAttribute("value"));
			if (statusTerbitItemBaru == null) {
				return false;
			}

			if (statusTerbitItemLama.getId().equals(LibraryUtil.DRAFT.getId())
					|| statusTerbitItemLama.getId().equals(
							LibraryUtil.REJECT.getId())) {
				if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.APPROVE.getId())) {
					if (item.getId() != null) {
						Session session = HibernateUtil.currentSession();
						Integer count = ((Number) session
								.createCriteria(ItemPunyaPemeriksa.class)
								.add(Restrictions.eq("item", item))
								.add(Restrictions.ne("status",
										ItemPunyaPemeriksa.DISETUJUI))
								.setProjection(Projections.rowCount())
								.uniqueResult()).intValue();
						if (!count.equals(0)) {
							MyMessageboxConfig
									.show("Terdapat "
											+ count
											+ " pemeriksa yang belum menyetujui karya tulis ini.",
											"Peringatan", MyMessageboxConfig.OK,
											MyMessageboxConfig.EXCLAMATION);
							selectback();
							return false;
						}
					}
				}
			}

			if (statusTerbitItemLama.getId().equals(LibraryUtil.DRAFT.getId())) {
				if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.PUBLISH.getId())) {
					MyMessageboxConfig
							.show("Status " + LibraryUtil.DRAFT.getNama()
									+ " tidak boleh langsung diubah menjadi  "
									+ LibraryUtil.PUBLISH.getNama() + ".",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					selectback();
					return false;
				}

			}
			if (statusTerbitItemLama.getId()
					.equals(LibraryUtil.PUBLISH.getId())) {
				if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.DRAFT.getId())) {
					MyMessageboxConfig.show("Status " + LibraryUtil.PUBLISH.getNama()
							+ " tidak boleh langsung diubah menjadi "
							+ LibraryUtil.DRAFT.getNama() + ".", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					selectback();
					return false;
				}
			}
			if (statusTerbitItemLama.getId().equals(LibraryUtil.REJECT.getId())) {
				if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.PUBLISH.getId())) {
					MyMessageboxConfig
							.show("Status " + LibraryUtil.REJECT.getNama()
									+ " tidak boleh langsung diubah menjadi  "
									+ LibraryUtil.PUBLISH.getNama() + ".",
									"Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.EXCLAMATION);
					selectback();
					return false;
				} else if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.DRAFT.getId())) {
					MyMessageboxConfig.show("Status " + LibraryUtil.REJECT.getNama()
							+ " tidak boleh langsung diubah menjadi  "
							+ LibraryUtil.DRAFT.getNama() + ".", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					selectback();
					return false;
				}
			}
			if (statusTerbitItemLama.getId()
					.equals(LibraryUtil.APPROVE.getId())) {
				if (statusTerbitItemBaru.getId().equals(
						LibraryUtil.DRAFT.getId())) {
					MyMessageboxConfig.show("Status " + LibraryUtil.APPROVE.getNama()
							+ " tidak boleh langsung diubah menjadi  "
							+ LibraryUtil.DRAFT.getNama() + ".", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					selectback();
					return false;
				}

				if (item.getId() != null) {
					Session session = HibernateUtil.currentSession();
					Integer count = ((Number) session
							.createCriteria(ItemPunyaPemeriksa.class)
							.add(Restrictions.eq("item", item))
							.add(Restrictions.ne("status",
									ItemPunyaPemeriksa.DISETUJUI))
							.setProjection(Projections.rowCount())
							.uniqueResult()).intValue();
					if (!count.equals(0)) {
						MyMessageboxConfig
								.show("Terdapat "
										+ count
										+ " pemeriksa yang belum menyetujui karya tulis ini.",
										"Peringatan", MyMessageboxConfig.OK,
										MyMessageboxConfig.EXCLAMATION);
						selectback();
						return false;
					}
				}

			}
			rubahDirectory.onEvent(null);
			return true;
		}

		@Override
		public void onEvent(Event arg0) throws Exception {
			onCheck();
		}
	};

	private MyRadioConfig draft;

	private MyRadioConfig approve;

	private MyRadioConfig reject;

	private MyRadioConfig publish;

	private StatusTerbitEventListener statusTerbitEventListener;

	private Row rowDirektoriTerbit;

	private EventListener rubahDirectory = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {

			parent = null;
			rowDirektoriTerbit.setVisible(false);

			if (publish.isChecked()
					&& satuanKerja.getAttribute("satuanKerja") != null
					&& domainPenelitian.getAttribute("domainPenelitian") != null) {

				DomainPenelitian mydomainPenelitian = (DomainPenelitian) domainPenelitian
						.getAttribute("domainPenelitian");

				Session session = HibernateUtil.currentSession();

				Item myparent = (Item) session
						.createCriteria(Item.class)
						.add(Restrictions.eq("folder", true))
						.add(Restrictions.eq("defaultSatuanKerja",
								satuanKerja.getAttribute("satuanKerja")))
						.add(Restrictions.eq("tipeItem",
								LibraryUtil.KARYA_ILMIAH))
						.add(Restrictions.isNull("parent"))
						.addOrder(Order.asc("id")).setMaxResults(1)
						.uniqueResult();

				Item myitem = (Item) session
						.createCriteria(Item.class)
						.add(Restrictions.eq("folder", true))
						.add(Restrictions.eq("parent", myparent))
						.add(Restrictions.eq("defaultSatuanKerja",
								satuanKerja.getAttribute("satuanKerja")))
						.add(Restrictions.eq("tipeItem",
								LibraryUtil.KARYA_ILMIAH))
						.add(Restrictions.eq("domainPenelitian",
								mydomainPenelitian)).addOrder(Order.asc("id"))
						.setMaxResults(1).uniqueResult();

				Common.clear(rowDirektoriTerbit);
				rowDirektoriTerbit.setVisible(true);
				rowDirektoriTerbit.appendChild(new Label(ais.common.Common.getBahasaConfig("Direktori Terbit")));
				rowDirektoriTerbit
						.appendChild(parent = new AmbilDataFolderItemBanbox(
								true, LibraryUtil.KARYA_ILMIAH,
								(SatuanKerja) satuanKerja
										.getAttribute("satuanKerja"), myitem));
				parent.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						item.setParent((Item) parent.getAttribute("item"));
					}
				});
				parent.setAttribute("item", item.getParent());
				parent.setValue(item.getParent() == null ? "" : item
						.getParent().toString());
				parent.setWidth("90%");
			}
		}
	};

	protected void init(final Item item) throws Exception {
		if (item.getTipeItem() == null) {
			item.setTipeItem(LibraryUtil.KARYA_ILMIAH);
		}
		this.item = item;
		this.statusTerbitEventListener = new StatusTerbitEventListener();
		addWindow.setTitle(item.getId() == null ? "Tambah Item" : "Ubah Item");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("60%");

		initDetail(item, east);

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
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("75%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul (Indonesia)"));
		row.appendChild(nama = new Textbox(item.getNama() == null ? "" : item
				.getNama()));
		nama.setWidth("90%");
		nama.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul (English)"));
		row.appendChild(tema = new Textbox(item.getTema()));
		tema.setWidth("90%");
		tema.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Diajukan oleh"));
		row.appendChild(dibuatOleh = new AmbilDataTbmuserBanbox());
		dibuatOleh.setAttribute("tbmuser", item.getDibuatOleh());
		dibuatOleh.setValue(item.getDibuatOleh() == null ? "" : item
				.getDibuatOleh().toString());
		dibuatOleh.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Urutan ke"));
		row.appendChild(urutan = new Decimalbox(
				new BigDecimal(item.getUrutan())));
		urutan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(item.getDefaultSatuanKerja() == null ? "" : item
				.getDefaultSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja", item.getDefaultSatuanKerja());
		satuanKerja.setWidth("90%");

		if (item.getDefaultSatuanKerja() == null) {
			SatuanKerja satuanKerja = Common.getCurrentUser() == null ? null
					: Common.getCurrentUser().ambilSatuanKerja();
			Perpustakaan currentPerpustakaan = Common.getCurrentPerpustakaan();
			if (satuanKerja == null && currentPerpustakaan != null) {
				satuanKerja = currentPerpustakaan.getSatuanKerja();
			}
			this.satuanKerja.setValue(satuanKerja == null ? "" : satuanKerja
					.toString());
			this.satuanKerja.setAttribute("satuanKerja", satuanKerja);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(statusTerbitItem = new Radiogroup());
		draft = new MyRadioConfig(LibraryUtil.DRAFT.getNama());
		draft.setAttribute("value", LibraryUtil.DRAFT);
		statusTerbitItem.appendChild(draft);
		if (LibraryUtil.DRAFT.getId()
				.equals(item.getStatusTerbitItem().getId())) {
			statusTerbitItem.setSelectedItem(draft);
		}

		approve = new MyRadioConfig(LibraryUtil.APPROVE.getNama());
		approve.setAttribute("value", LibraryUtil.APPROVE);
		approve.setDisabled(!this.aapprove || item.getId() == null);
		statusTerbitItem.appendChild(approve);

		if (LibraryUtil.APPROVE.getId().equals(
				item.getStatusTerbitItem().getId())) {
			statusTerbitItem.setSelectedItem(approve);
		}

		reject = new MyRadioConfig(LibraryUtil.REJECT.getNama());
		reject.setAttribute("value", LibraryUtil.REJECT);
		reject.setDisabled(!this.areject || item.getId() == null);
		statusTerbitItem.appendChild(reject);

		if (LibraryUtil.REJECT.getId().equals(
				item.getStatusTerbitItem().getId())) {
			statusTerbitItem.setSelectedItem(reject);
		}

		publish = new MyRadioConfig(LibraryUtil.PUBLISH.getNama());
		publish.setAttribute("value", LibraryUtil.PUBLISH);
		publish.setDisabled(item.getId() == null);
		statusTerbitItem.appendChild(publish);

		if (LibraryUtil.PUBLISH.getId().equals(
				item.getStatusTerbitItem().getId())) {
			statusTerbitItem.setSelectedItem(publish);
		}

		statusTerbitItem.addEventListener("onClick", statusTerbitEventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahasa"));
		row.appendChild(bahasa = new Textbox(item.getBahasa()));
		bahasa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Edisi"));
		row.appendChild(edisi = new Textbox(item.getEdisi()));
		edisi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penaklikan"));
		row.appendChild(penaklikan = new Textbox(item.getPenaklikan()));
		penaklikan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis"));
		row.appendChild(jenisItem = new Combobox());
		Common.insertCombo(jenisItem, "nama", JenisItem.class);
		Common.selectComboItem(jenisItem, item.getJenisItem());
		jenisItem.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe"));
		row.appendChild(tipeItem = new Combobox());
		Common.insertCombo(tipeItem, "nama", TipeItem.class);
		Common.selectComboItem(tipeItem, LibraryUtil.KARYA_ILMIAH);
		tipeItem.setWidth("90%");
		tipeItem.setDisabled(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit (Instansi)"));
		row.appendChild(penerbit = new AmbilDataPenerbitBanbox());
		if (satuanKerja.getAttribute("satuanKerja") != null) {
			penerbit.setSatuanKerja((SatuanKerja) satuanKerja
					.getAttribute("satuanKerja"));
		}
		penerbit.setAttribute("penerbit", item.getPenerbit());
		penerbit.setValue(item.getPenerbit() == null ? "" : item.getPenerbit()
				.getNama());
		penerbit.setWidth("90%");

		satuanKerja.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				penerbit.setSatuanKerja((SatuanKerja) satuanKerja
						.getAttribute("satuanKerja"));
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Domain Penelitian"));
		row.appendChild(domainPenelitian = new AmbilDataDomainPenelitianBanbox());
		if (penerbit.getAttribute("penerbit") != null) {
			domainPenelitian.setPenerbit((Penerbit) penerbit
					.getAttribute("penerbit"));
		}
		domainPenelitian.setAttribute("domainPenelitian",
				item.getDomainPenelitian());
		domainPenelitian.setValue(item.getDomainPenelitian() == null ? ""
				: item.getDomainPenelitian().getNama());
		domainPenelitian.setWidth("90%");

		penerbit.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Penerbit myPenerbit = (Penerbit) penerbit
						.getAttribute("penerbit");
				domainPenelitian.setPenerbit(myPenerbit);
				rubahDirectory.onEvent(arg0);
			}
		});

		domainPenelitian.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rubahDirectory.onEvent(arg0);
			}
		});

		rowDirektoriTerbit = new MyFormRow();
		rowDirektoriTerbit.setStyle("border:0px;background: transparent;");
		rowDirektoriTerbit.setVisible(true);
		rowDirektoriTerbit.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 1"));
		row.appendChild(penerbit1 = new AmbilDataPenerbitBanbox());
		penerbit1.setAttribute("penerbit", item.getPenerbit2());
		penerbit1.setValue(item.getPenerbit2() == null ? "" : item
				.getPenerbit2().getNama());
		penerbit1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 2"));
		row.appendChild(penerbit2 = new AmbilDataPenerbitBanbox());
		penerbit2.setAttribute("penerbit", item.getPenerbit3());
		penerbit2.setValue(item.getPenerbit3() == null ? "" : item
				.getPenerbit3().getNama());
		penerbit2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 3"));
		row.appendChild(penerbit3 = new AmbilDataPenerbitBanbox());
		penerbit3.setAttribute("penerbit", item.getPenerbit4());
		penerbit3.setValue(item.getPenerbit4() == null ? "" : item
				.getPenerbit4().getNama());
		penerbit3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penerbit Lain 4"));
		row.appendChild(penerbit4 = new AmbilDataPenerbitBanbox());
		penerbit4.setAttribute("penerbit", item.getPenerbit5());
		penerbit4.setValue(item.getPenerbit5() == null ? "" : item
				.getPenerbit5().getNama());
		penerbit4.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Dimasukkan"));
		row.appendChild(tanggal = new MyDatebox(item.getTanggal()));
		tanggal.setFormat(Common.dateFormat1.get().toPattern());
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Terbit"));
		row.appendChild(tanggalTerbit = new MyDatebox(item.getTanggalterbit()));
		tanggalTerbit.setFormat(Common.dateFormat1.get().toPattern());
		tanggalTerbit.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun"));
		row.appendChild(tahun = new Intbox(item.getTahun()));
		tahun.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Halaman"));
		row.appendChild(halaman = new Intbox(item.getHalaman()));
		halaman.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link"));
		row.appendChild(link = new Textbox(item.getLink()));
		link.setWidth("90%");
		link.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link Gambar"));
		row.appendChild(imageUrl = new Textbox(item.getImageUrl()));
		imageUrl.setWidth("90%");
		imageUrl.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Catatan"));
		row.appendChild(catatan = new Textbox(item.getCatatan()));
		catatan.setWidth("90%");
		catatan.setRows(4);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				item.getKeterangan() == null ? "" : item.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(4);

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
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(new Event("", addWindow,
								KaryaTulisItemAction.this.item));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		rubahDirectory.onEvent(null);
	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Judul Item harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (dibuatOleh.getAttribute("tbmuser") == null) {
			MyMessageboxConfig.show("Diajukan Oleh harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (penerbit.getAttribute("penerbit") == null) {
			MyMessageboxConfig.show("Penerbit atau instansi harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (domainPenelitian.getAttribute("domainPenelitian") == null) {
			MyMessageboxConfig.show("Domain Penelitian harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Satuan kerja harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (statusTerbitItem.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (parent != null && parent.getAttribute("item") == null) {
			MyMessageboxConfig.show("Direktori terbit harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (!statusTerbitEventListener.onCheck()) {
			return false;
		}

		List<Row> rowsPengarang = gridPengarang.getRows().getChildren();
		for (Row row : rowsPengarang) {
			ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) row
					.getAttribute("itemPunyaPengarang");
			if (itemPunyaPengarang.getPengarang() == null) {
				MyMessageboxConfig.show("Pengarang harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsKategoriItem = gridKategoriItem.getRows().getChildren();
		for (Row row : rowsKategoriItem) {
			ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) row
					.getAttribute("itemPunyaKategoriItem");
			if (itemPunyaKategoriItem.getKategoriItem() == null) {
				MyMessageboxConfig.show("Kategori Item harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsPemeriksa = gridPemeriksa.getRows().getChildren();
		for (Row row : rowsPemeriksa) {
			ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) row
					.getAttribute("itemPunyaPemeriksa");
			if (itemPunyaPemeriksa.getPemeriksa() == null) {
				MyMessageboxConfig.show("Pemeriksa harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsBarcode = gridBarcode.getRows().getChildren();
		for (Row row : rowsBarcode) {
			ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) row
					.getAttribute("itemPunyaBarcode");
			if (itemPunyaBarcode.getBarcode() == null) {
				MyMessageboxConfig.show("Barcode harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsTerbit = gridTerbit.getRows().getChildren();
		for (Row row : rowsTerbit) {
			ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) row
					.getAttribute("itemPunyaTerbit");
			if (itemPunyaTerbit.getMulai() == null) {
				MyMessageboxConfig.show("Mulai terbit harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			FotoGambarItem fotoGambarItem = (FotoGambarItem) row
					.getAttribute("fotoGambarItem");
			if (fotoGambarItem.getItem() == null) {
				MyMessageboxConfig.show("Gambar harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		List<Row> rowsDocument = gridDocument.getRows().getChildren();
		for (Row row : rowsDocument) {
			FotoItem fotoItem = (FotoItem) row.getAttribute("fotoItem");
			if (fotoItem.getItem() == null) {
				MyMessageboxConfig.show("File harus diisi", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		ItemDao itemDao = DaoFactory.getInstance().getItemDao();
		if (item.getId() != null) {
			item = itemDao.load(item.getId());

		}

		item.setParent((Item) (parent == null ? null : parent
				.getAttribute("item")));
		item.setDomainPenelitian((DomainPenelitian) domainPenelitian
				.getAttribute("domainPenelitian"));
		item.setDibuatOleh((Tbmuser) dibuatOleh.getAttribute("tbmuser"));
		item.setUrutan(urutan.getValue() == null ? null : urutan.getValue()
				.intValue());
		item.setPenerbit2((Penerbit) penerbit1.getAttribute("penerbit"));
		item.setPenerbit3((Penerbit) penerbit2.getAttribute("penerbit"));
		item.setPenerbit4((Penerbit) penerbit3.getAttribute("penerbit"));
		item.setPenerbit5((Penerbit) penerbit4.getAttribute("penerbit"));

		item.setTanggalterbit(tanggalTerbit.getValue());
		item.setTema(tema.getValue());
		item.setDefaultSatuanKerja((SatuanKerja) satuanKerja
				.getAttribute("satuanKerja"));
		item.setStatusTerbitItem((StatusTerbitItem) statusTerbitItem
				.getSelectedItem().getAttribute("value"));
		item.setPengarangs(null);
		item.setNama(nama.getValue());
		item.setKeterangan(keterangan.getValue());
		item.setBahasa(bahasa.getValue());
		item.setCatatan(catatan.getValue());
		item.setEdisi(edisi.getValue());
		item.setHalaman(halaman.getValue());
		item.setJenisItem((JenisItem) (jenisItem.getSelectedItem() == null ? null
				: jenisItem.getSelectedItem().getValue()));
		item.setLink(link.getValue());
		item.setPenaklikan(penaklikan.getValue());
		item.setTanggal(tanggal.getValue());

		item.setPenerbit((Penerbit) penerbit.getAttribute("penerbit"));
		item.setTahun(tahun.getValue());
		item.setTipeItem((TipeItem) (tipeItem.getSelectedItem() == null ? null
				: tipeItem.getSelectedItem().getValue()));
		item.setAbstrak(abstrak.getValue());
		item.setKewords(kewords.getValue());
		item.setAbstrakEn(abstrakEn.getValue());
		item.setKewordsEn(kewordsEn.getValue());
		item.setImageUrl(imageUrl.getValue().trim());
		if (item.getAktif() == null) {
			item.setAktif(true);
		}

		if (item.getId() != null) {
			itemDao.update(item);
		} else {
			itemDao.save(item);
		}

		Session session = HibernateUtil.currentSession();
		for (Row row : rowsPengarang) {
			ItemPunyaPengarang itemPunyaPengarang = (ItemPunyaPengarang) row
					.getAttribute("itemPunyaPengarang");
			itemPunyaPengarang.setItem(item);
			session.saveOrUpdate(itemPunyaPengarang);
		}

		for (Row row : rowsKategoriItem) {
			ItemPunyaKategoriItem itemPunyaKategoriItem = (ItemPunyaKategoriItem) row
					.getAttribute("itemPunyaKategoriItem");
			itemPunyaKategoriItem.setItem(item);
			session.saveOrUpdate(itemPunyaKategoriItem);
		}

		for (Row row : rowsPemeriksa) {
			ItemPunyaPemeriksa itemPunyaPemeriksa = (ItemPunyaPemeriksa) row
					.getAttribute("itemPunyaPemeriksa");
			itemPunyaPemeriksa.setItem(item);
			session.saveOrUpdate(itemPunyaPemeriksa);
		}

		for (Row row : rowsBarcode) {
			ItemPunyaBarcode itemPunyaBarcode = (ItemPunyaBarcode) row
					.getAttribute("itemPunyaBarcode");
			itemPunyaBarcode.setItem(item);
			session.saveOrUpdate(itemPunyaBarcode);
		}

		for (Row row : rowsTerbit) {
			ItemPunyaTerbit itemPunyaTerbit = (ItemPunyaTerbit) row
					.getAttribute("itemPunyaTerbit");
			itemPunyaTerbit.setItem(item);
			session.saveOrUpdate(itemPunyaTerbit);
		}

		Integer jumlahPemeriksa = ((Number) session
				.createCriteria(ItemPunyaPemeriksa.class)
				.add(Restrictions.eq("item", item))
				.setProjection(Projections.rowCount()).uniqueResult())
				.intValue();
		if (jumlahPemeriksa.equals(0)) {
			List<PenerbitPunyaPemeriksa> penerbitPunyaPemeriksas = session
					.createCriteria(PenerbitPunyaPemeriksa.class)
					.add(Restrictions.eq("penerbit", item.getPenerbit()))
					.list();
			for (PenerbitPunyaPemeriksa penerbitPunyaPemeriksa : penerbitPunyaPemeriksas) {
				ItemPunyaPemeriksa itemPunyaPemeriksa = new ItemPunyaPemeriksa();
				itemPunyaPemeriksa.setItem(item);
				itemPunyaPemeriksa.setPemeriksa(penerbitPunyaPemeriksa
						.getPemeriksa());
				session.save(itemPunyaPemeriksa);
			}
		}

		List<String> strings = session.createCriteria(ItemPunyaPengarang.class)
				.createAlias("pengarang", "pengarang")
				.setProjection(Projections.property("pengarang.nama"))
				.add(Restrictions.eq("item", item)).list();
		String pengarangs = strings.toString().replaceAll("\\[", "")
				.replaceAll("\\]", "");
		item.setPengarangs(pengarangs);

		strings = session.createCriteria(ItemPunyaKategoriItem.class)
				.createAlias("kategoriItem", "kategoriItem")
				.setProjection(Projections.property("kategoriItem.nama"))
				.add(Restrictions.eq("item", item)).list();
		String kategories = "";
		for (String s : strings) {
			kategories += kategories.equals("") ? "[" + s + "]" : ", [" + s
					+ "]";
		}
		item.setKategories(kategories);

		Common.refreshUpdate(session, (item));

		Session mysession = StreamingHibernateUtil.getInstance()
				.currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				FotoGambarItem fotoGambarItem = (FotoGambarItem) row
						.getAttribute("fotoGambarItem");
				fotoGambarItem.setItem(item.getId());
				mysession.saveOrUpdate(fotoGambarItem);
			}

			for (Row row : rowsDocument) {
				FotoItem fotoItem = (FotoItem) row.getAttribute("fotoItem");
				fotoItem.setItem(item.getId());
				mysession.saveOrUpdate(fotoItem);
			}
			mysession.getTransaction().commit();
		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e); 
		}

		StreamingHibernateUtil.getInstance().closeSession();

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null || satuanKerjaTreeModel == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<Item> item = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(
						Common.ROWS_COUNT_ON_PAGE
								* (paging == null ? 0 : paging.getActivePage()))
				.list();
		System.out.println("item = " + item.size());
		ListModel strset = new SimpleListModel(item);
		grid.setRowRenderer(new ItemRenderer());
		grid.setModelCheckMobile(strset);

		

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		SatuanKerja parent = (SatuanKerja) satuanKerjaBanbox
				.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear(); satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		List<Long> ids = new ArrayList<Long>();
		for (SatuanKerja kerja : satuanKerjas) {
			ids.add(kerja.getId());
		}

		Criteria criteria = session.createCriteria(Item.class);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.isNotNull("defaultSatuanKerja"))
				.add(Restrictions.eq("folder", false))
				.add(ids.size() == 0 ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("defaultSatuanKerja.id", ids))
				.add(searchtema.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("tema",
						searchtema.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("nama",
						searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchbahasa.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("bahasa",
						searchbahasa.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchedisi.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("edisi",
						searchedisi.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpengarang.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike(
						"pengarangs", searchpengarang.getValue().trim(),
						MatchMode.ANYWHERE))
				.add(searchcatatan.getValue().trim().equals("") ? Restrictions
						.sqlRestriction("1=1") : Restrictions.ilike("catatan",
						searchcatatan.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchpenerbit.getAttribute("penerbit") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("penerbit",
						searchpenerbit.getAttribute("penerbit")))
				.add(searchjenisItem.getSelectedItem() == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq("jenisItem",
						searchjenisItem.getSelectedItem().getValue()))
				.add(Restrictions.eq("tipeItem", LibraryUtil.KARYA_ILMIAH))
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						|| searchstatus.getSelectedItem().getAttribute("value") == null ? Restrictions
						.sqlRestriction("1=1") : Restrictions.eq(
						"statusTerbitItem", searchstatus.getSelectedItem()
								.getAttribute("value")));
		return criteria;
	}


	/**
	 * Kriteria tab Penerbitan KHUSUS KARYA ILMIAH.
	 *
	 * <p>Menimpa versi di {@link ItemAction} yang menyaring koleksi buku
	 * ({@code defaultSatuanKerja} kosong). Karya ilmiah justru sebaliknya: selalu
	 * terikat pada satuan kerja, bukan folder, dan bertipe Karya Ilmiah -- sama
	 * persis dengan lingkup {@link #initCriteria(boolean)} layar ini, sehingga tab
	 * Penerbitan tidak pernah menampilkan data di luar wewenang pengguna.</p>
	 */
	@Override
	protected Criteria initCriteriaPublikasi(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Item.class);
		if (order) {
			criteria.addOrder(Order.desc("id"));
		}

		// Lingkup satuan kerja: ikut pembatasan akses pengguna, seperti daftar utama.
		List<Long> ids = new ArrayList<Long>();
		try {
			SatuanKerja parent = satuanKerjaBanbox == null ? null
					: (SatuanKerja) satuanKerjaBanbox.getAttribute("satuanKerja");
			Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
			if (parent != null) {
				satuanKerjas.clear();
				satuanKerjas.add(parent);
				satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
			}
			for (SatuanKerja kerja : satuanKerjas) {
				if (kerja != null && kerja.getId() != null) {
					ids.add(kerja.getId());
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "KaryaTulisItemAction.initCriteriaPublikasi.satuanKerja");
		}

		criteria.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.isNotNull("defaultSatuanKerja"))
				.add(Restrictions.eq("folder", false))
				.add(ids.isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.in("defaultSatuanKerja.id", ids));

		// Tipe Karya Ilmiah; bila konstanta belum ter-init, cari berdasarkan nama.
		TipeItem tipe = LibraryUtil.KARYA_ILMIAH;
		if (tipe == null) {
			tipe = (TipeItem) session.createCriteria(TipeItem.class)
					.add(Restrictions.ilike("nama", "Karya Ilmiah", MatchMode.EXACT))
					.setMaxResults(1).uniqueResult();
		}
		if (tipe != null) {
			criteria.add(Restrictions.eq("tipeItem", tipe));
		}

		// Filter milik tab Penerbitan (judul, pengarang, status terbit).
		if (searchPublikasiNama != null && !searchPublikasiNama.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("nama", searchPublikasiNama.getValue().trim(),
					MatchMode.ANYWHERE));
		}
		if (searchPublikasiPengarang != null && !searchPublikasiPengarang.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("pengarangs", searchPublikasiPengarang.getValue().trim(),
					MatchMode.ANYWHERE));
		}
		if (searchPublikasiStatus != null && searchPublikasiStatus.getSelectedItem() != null
				&& searchPublikasiStatus.getSelectedItem().getAttribute("value") != null) {
			StatusTerbitItem dipilih = (StatusTerbitItem) searchPublikasiStatus.getSelectedItem()
					.getAttribute("value");
			// Karya lama bisa berstatus NULL; samakan dengan Draft seperti di layar buku.
			if (LibraryUtil.DRAFT != null && dipilih != null && LibraryUtil.DRAFT.getId() != null
					&& LibraryUtil.DRAFT.getId().equals(dipilih.getId())) {
				criteria.add(Restrictions.or(Restrictions.isNull("statusTerbitItem"),
						Restrictions.eq("statusTerbitItem", dipilih)));
			} else {
				criteria.add(Restrictions.eq("statusTerbitItem", dipilih));
			}
		}
		return criteria;
	}
}
