package ais.action.master.helper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.dao.BeasiswaPunyaItemBiayaTambahanDao;
import ais.database.dao.DaoFactory;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.BeasiswaPunyaItemBiayaTambahan;
import ais.database.model.ItemBiaya;

/**
 * Helper "pilih dari daftar" untuk menautkan {@link ItemBiaya} tambahan ke satu {@link Beasiswa},
 * lewat relasi {@link BeasiswaPunyaItemBiayaTambahan}. Menampilkan jendela modal pencarian
 * ({@code nama}/{@code deskripsi}, memakai pola paging server-side
 * {@link ais.ui.util.AmbilDataPagingHelper}) dengan satu checkbox per baris item biaya — dicentang
 * bila item tersebut sudah tertaut ke {@link Beasiswa} yang bersangkutan. Checkbox pada header
 * kolom berfungsi centang/lepas-centang semua baris sekaligus.
 *
 * <p>
 * Perubahan status checkbox tidak langsung disimpan; baru ditulis ke database saat tombol
 * "Simpan" ditekan, lewat {@link #save()} yang menyinkronkan seluruh baris yang saat ini
 * ditampilkan grid (checked → buat/pertahankan relasi, unchecked → hapus relasi) dan tambahan
 * relasi pada {@link #deletedItemBiayas} — meskipun secara semantik penamaan set ini agak
 * menyesatkan: berisi item yang di-uncheck lewat listener {@code onCheck} pada baris yang di luar
 * halaman grid saat ini, sehingga tetap dihapus eksplisit di {@link #save()}.
 * </p>
 */
public class AmbilDataItemBiayaHelper {
	private MyGrid grid;

	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final ais.ui.util.AmbilDataPagingHelper pagingHelper = new ais.ui.util.AmbilDataPagingHelper();
	private Beasiswa beasiswa;
	private Textbox nama;
	private Set<BeasiswaPunyaItemBiayaTambahan> deletedItemBiayas = new HashSet<BeasiswaPunyaItemBiayaTambahan>();

	public AmbilDataItemBiayaHelper() {
	}

	/** Perender baris grid: label kode/nama/deskripsi item biaya, plus checkbox status tertaut yang menyimpan/menghapus relasi ke {@link #deletedItemBiayas} saat diklik. */
	class ItemBiayaRenderer extends ais.ui.util.MyRowRenderer {
		private BeasiswaPunyaItemBiayaTambahanDao beasiswaPunyaItemBiayaTambahanDao = DaoFactory.getInstance()
				.getBeasiswaPunyaItemBiayaTambahanDao();
		private Session session = beasiswaPunyaItemBiayaTambahanDao.getCurrentSession();

		/**
		 * Merender satu baris {@link ItemBiaya}: checkbox status tertaut (dicentang bila sudah ada
		 * {@link BeasiswaPunyaItemBiayaTambahan} untuk kombinasi beasiswa+item ini), lalu label
		 * kode, nama, dan deskripsi item biaya.
		 */
		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final ItemBiaya itemBiaya = (ItemBiaya) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("itemBiaya", itemBiaya);
			// checkbox.setId("" + itemBiaya.getId());

			Integer jml = ((Number) session.createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
					.setProjection(Projections.rowCount()).add(Restrictions.eq("beasiswa", beasiswa))
					.add(Restrictions.eq("itemBiaya", itemBiaya)).uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan = (BeasiswaPunyaItemBiayaTambahan) HibernateUtil
							.currentSession().createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
							.add(Restrictions.eq("beasiswa", beasiswa)).add(Restrictions.eq("itemBiaya", itemBiaya))
							.uniqueResult();
					if (beasiswaPunyaItemBiayaTambahan != null) {
						if (!checkbox.isChecked()) {
							deletedItemBiayas.remove(beasiswaPunyaItemBiayaTambahan);
						} else {
							deletedItemBiayas.add(beasiswaPunyaItemBiayaTambahan);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));

			new Label(itemBiaya.getKode()).setParent(arg0);
			new Label(itemBiaya.getNama()).setParent(arg0);
			new Label(itemBiaya.getDeskripsi()).setParent(arg0);
		}
	}

	/**
	 * Menyinkronkan status checkbox seluruh baris grid yang saat ini ditampilkan ke tabel relasi
	 * {@link BeasiswaPunyaItemBiayaTambahan}: baris tercentang membuat/mempertahankan relasi,
	 * baris tak tercentang menghapus relasi (bila ada). Setelah itu, seluruh entri pada
	 * {@link #deletedItemBiayas} juga dihapus. Kegagalan per baris ditelan (tidak menghentikan
	 * proses baris lain).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {
		BeasiswaPunyaItemBiayaTambahanDao beasiswaPunyaItemBiayaTambahanDao = DaoFactory.getInstance()
				.getBeasiswaPunyaItemBiayaTambahanDao();
		Session session = beasiswaPunyaItemBiayaTambahanDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);
				if (checkbox.isChecked()) {

					ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
					BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan = (BeasiswaPunyaItemBiayaTambahan) session
							.createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
							.add(Restrictions.eq("beasiswa", this.beasiswa))
							.add(Restrictions.eq("itemBiaya", itemBiaya)).setMaxResults(1).uniqueResult();

					if (beasiswaPunyaItemBiayaTambahan == null) {
						beasiswaPunyaItemBiayaTambahan = new BeasiswaPunyaItemBiayaTambahan();
					}
					beasiswaPunyaItemBiayaTambahan.setBeasiswa(this.beasiswa);
					beasiswaPunyaItemBiayaTambahan.setItemBiaya(itemBiaya);
					session.saveOrUpdate(beasiswaPunyaItemBiayaTambahan);

				} else {
					ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("itemBiaya");
					BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan = (BeasiswaPunyaItemBiayaTambahan) session
							.createCriteria(BeasiswaPunyaItemBiayaTambahan.class)
							.add(Restrictions.eq("beasiswa", this.beasiswa))
							.add(Restrictions.eq("itemBiaya", itemBiaya)).setMaxResults(1).uniqueResult();

					if (beasiswaPunyaItemBiayaTambahan == null) {
						beasiswaPunyaItemBiayaTambahan = new BeasiswaPunyaItemBiayaTambahan();
					}
					session.delete(beasiswaPunyaItemBiayaTambahan);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataItemBiayaHelper.java:143");
				// TODO: handle exception
			}
		}

		if (deletedItemBiayas != null) {

			for (BeasiswaPunyaItemBiayaTambahan beasiswaPunyaItemBiayaTambahan : deletedItemBiayas) {
				session.delete(beasiswaPunyaItemBiayaTambahan);
			}

		}

	}

	/**
	 * Membangun dan menampilkan jendela modal pemilihan item biaya untuk {@code beasiswa} yang
	 * diberikan: kotak pencarian nama, grid ber-paging dengan checkbox per baris, dan tombol
	 * Simpan (memanggil {@link #save()}, menyegarkan {@code dataLoader}, lalu menutup jendela) /
	 * Batal (menutup tanpa menyimpan).
	 *
	 * @param beasiswa   beasiswa yang item biaya tambahannya akan diatur
	 * @param dataLoader callback penyegar tampilan pemanggil setelah simpan
	 */
	public void display(Beasiswa beasiswa, final DataLoader dataLoader) {
		final MyWindow window = new MyWindow();
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		this.beasiswa = beasiswa;
		Common.clear(window);
		window.setTitle("Data Item Biaya");
		window.setWidth("750px");
		window.setHeight("540px");

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(window);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Item Biaya");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(north);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		/* Paging server-side (AmbilDataPagingHelper) menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT. */
		pagingHelper.pasangOnPaging(new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("50px");
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getChildren().get(0);
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataItemBiayaHelper.java:230");

					}
				}
			}
		});

		column = new MyColumnConfig("Kode");
		column.setParent(columns);
		column = new MyColumnConfig("Nama");
		column.setParent(columns);
		column = new MyColumnConfig("Deskripsi");
		column.setParent(columns);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.detach();
			}
		});

		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		button.setParent(toolbar);

		onSearchDefault(null);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Menjalankan pencarian {@link ItemBiaya} berdasarkan teks pada kotak {@code nama} (dicocokkan
	 * ilike terhadap kolom {@code nama} maupun {@code deskripsi}) dan memuat ulang grid dengan hasilnya.
	 *
	 * @param event event pemicu (paging/pencarian), tidak dipakai langsung
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<ItemBiaya> itemBiaya = pagingHelper.cariDenganCriteria(session.createCriteria(ItemBiaya.class)
				.add(Restrictions.or(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("deskripsi", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.addOrder(Order.asc("id")).setMaxResults(Common.MAX_RESULT), ItemBiaya.class);

		ListModel strset = new SimpleListModel(itemBiaya);
		grid.setRowRenderer(new ItemBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
