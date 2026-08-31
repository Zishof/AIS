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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
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
import ais.database.dao.DaoFactory;
import ais.database.dao.DetailSettingBiayaDao;
import ais.database.dao.ItemBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.SettingBiaya;

/**
 * Helper ZK berbentuk dialog pencarian-dan-pilih untuk menetapkan (dan melepaskan) item biaya
 * ({@link ItemBiaya}) yang termasuk dalam satu {@link SettingBiaya}, lewat relasi {@link
 * DetailSettingBiaya}. Berbeda dari dialog "Ambil Data" lain di modul ini yang umumnya bersifat
 * aditif saja, dialog ini dua arah: checkbox tercentang menyimpan/menciptakan relasi, checkbox
 * yang dilepas menghapus relasi yang ada.
 *
 * <p>
 * <b>Catatan:</b> penghapusan relasi ditangani lewat dua jalur yang tumpang tindih — di dalam
 * {@link #save()} sendiri (baris tidak tercentang memicu {@code session.delete(...)} langsung),
 * dan lewat set {@link #deleteDetailSettingBiaya} yang diisi event {@code onCheck} pada {@link
 * ItemBiayaRenderer} lalu dihapus ulang di akhir {@link #save()}. Bila kedua jalur mengenai
 * relasi yang sama, panggilan {@code delete} kedua berpotensi gagal (objek sudah terhapus) —
 * risiko ini tidak terlihat ke pengguna karena error di dalam loop utama ditangkap diam-diam,
 * tetapi baris kedua di luar loop (di atas set {@link #deleteDetailSettingBiaya}) tidak
 * dibungkus try/catch.
 * </p>
 */
public class AmbilDetailSettingBiayaHelper {

	private SettingBiaya settingBiaya;
	private MyGrid grid;
	private Textbox namaItemBiaya;

	private Set<DetailSettingBiaya> deleteDetailSettingBiaya = new HashSet<DetailSettingBiaya>();

	/**
	 * Renderer baris item biaya: checkbox (tercentang bila relasi ke {@link #settingBiaya} sudah
	 * ada), kode, nama, dan status "nilai bisa diubah". Setiap klik checkbox menambah/menghapus
	 * relasi existing dari {@link #deleteDetailSettingBiaya} (lihat catatan kelas mengenai
	 * potensi tumpang tindih dengan {@link #save()}).
	 */
	class ItemBiayaRenderer extends ais.ui.util.MyRowRenderer {

		private ItemBiayaDao itemBiayaDao = DaoFactory.getInstance().getItemBiayaDao();

		private Session session = itemBiayaDao.getCurrentSession();

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final ItemBiaya itemBiaya = (ItemBiaya) arg1;
			final MyCheckboxConfig checkbox = new MyCheckboxConfig();
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("myValue", itemBiaya);
			Integer jml = 0;

			jml = ((Number) session.createCriteria(DetailSettingBiaya.class).setProjection(Projections.rowCount())
					.add(Restrictions.eq("itemBiaya", itemBiaya)).add(Restrictions.eq("settingBiaya", settingBiaya))
					.uniqueResult()).intValue();

			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					DetailSettingBiaya detailSettingBiaya = (DetailSettingBiaya) HibernateUtil.currentSession()
							.createCriteria(DetailSettingBiaya.class).add(Restrictions.eq("itemBiaya", itemBiaya))
							.add(Restrictions.eq("settingBiaya", settingBiaya)).uniqueResult();
					if (detailSettingBiaya != null) {
						if (!checkbox.isChecked()) {
							deleteDetailSettingBiaya.remove(detailSettingBiaya);
						} else {
							deleteDetailSettingBiaya.add(detailSettingBiaya);
						}
					}

				}
			});
			checkbox.setChecked(!jml.equals(0));

			new Label(itemBiaya.getKode()).setParent(arg0);
			new Label(itemBiaya.getNama()).setParent(arg0);
			new Label(itemBiaya.getNilaiBisaDiubah() ? "Ya" : "Tidak").setParent(arg0);
		}

	}

	/**
	 * Menyinkronkan relasi {@link DetailSettingBiaya} sesuai status checkbox tiap baris grid:
	 * tercentang menyimpan/memperbarui relasi, tidak tercentang menghapusnya. Di akhir, seluruh
	 * relasi yang tercatat di {@link #deleteDetailSettingBiaya} juga dihapus (lihat catatan
	 * kelas mengenai potensi tumpang tindih dengan penghapusan di dalam loop utama).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void save() {

		DetailSettingBiayaDao detailSettingBiayaDao = DaoFactory.getInstance().getDetailSettingBiayaDao();
		Session session = detailSettingBiayaDao.getCurrentSession();

		Rows rows = grid.getRows();
		List<Row> list = rows.getChildren();
		for (Row row : list) {
			List data = row.getChildren();
			try {
				MyCheckboxConfig checkbox = (MyCheckboxConfig) data.get(0);

				if (checkbox.isChecked()) {
					ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("myValue");

					DetailSettingBiaya detailSettingBiaya = (DetailSettingBiaya) session
							.createCriteria(DetailSettingBiaya.class)
							.add(Restrictions.eq("settingBiaya", this.settingBiaya))
							.add(Restrictions.eq("itemBiaya", itemBiaya)).setMaxResults(1).uniqueResult();

					if (detailSettingBiaya == null) {
						detailSettingBiaya = new DetailSettingBiaya();
					}

					detailSettingBiaya.setItemBiaya(itemBiaya);
					detailSettingBiaya.setSettingBiaya(settingBiaya);
					session.saveOrUpdate(detailSettingBiaya);

				} else {
					ItemBiaya itemBiaya = (ItemBiaya) checkbox.getAttribute("myValue");
					DetailSettingBiaya detailSettingBiaya = (DetailSettingBiaya) session
							.createCriteria(DetailSettingBiaya.class)
							.add(Restrictions.eq("settingBiaya", this.settingBiaya))
							.add(Restrictions.eq("itemBiaya", itemBiaya)).setMaxResults(1).uniqueResult();

					if (detailSettingBiaya == null) {
						detailSettingBiaya = new DetailSettingBiaya();
					}
					session.delete(detailSettingBiaya);

				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDetailSettingBiayaHelper.java:143");
				// TODO: handle exception
			}
		}

		if (deleteDetailSettingBiaya != null) {

			for (DetailSettingBiaya detailSettingBiayaIdx : deleteDetailSettingBiaya) {
				session.delete(detailSettingBiayaIdx);
			}

		}

	}

	/**
	 * Titik masuk utama: membangun dialog pencarian item biaya (filter nama) dan grid berpaging,
	 * dengan tombol Simpan (memanggil {@link #save()}) / Cari / Batal.
	 *
	 * @param settingBiaya setting biaya tujuan yang item-nya dikelola
	 * @param dataLoader   dipanggil untuk memuat ulang tampilan pemanggil setelah simpan
	 * @param window       window pembungkus dialog (dibersihkan dan diisi ulang)
	 */
	public void display(final SettingBiaya settingBiaya, final DataLoader dataLoader, final MyWindow window) {

		this.settingBiaya = settingBiaya;
		Common.clear(window);
		window.setTitle("Ambil Item Biaya");
		window.setWidth("750px");
		window.setHeight("540px");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Item Biaya"));
		row.appendChild(namaItemBiaya = new Textbox());
		namaItemBiaya.setWidth("90%");

		// row = new MyFormRow();
		//		// row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(center);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig checkbox = new MyCheckboxConfig();
		column.appendChild(checkbox);
		checkbox.addEventListener(Events.ON_CHECK, new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				List<Row> rows = grid.getRows().getChildren();
				for (Row row : rows) {
					try {
						MyCheckboxConfig myCheckbox = (MyCheckboxConfig) row.getAttribute("checkbox");
						myCheckbox.setChecked(!myCheckbox.isDisabled() && checkbox.isChecked());
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDetailSettingBiayaHelper.java:236");

					}
				}
			}
		});
		column.setWidth("50px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Item");
		column.setWidth("20%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Item");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai bisa diubah");
		column.setWidth("20%");

		onSearchDefault(null);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.setParent(toolbar);
		button.setTooltiptext("Simpan");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				save();
				dataLoader.loadData(null);
				window.setVisible(false);
			}
		});

		button = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		button.setTooltiptext("Tutup");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.setVisible(false);
			}
		});
		button.setParent(toolbar);

		window.setVisible(true);
		try {
			window.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/** Memuat ulang grid item biaya aktif (maks {@link Common#MAX_RESULT} baris) sesuai filter nama aktif. */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();
		List<ItemBiaya> itemBiaya = session.createCriteria(ItemBiaya.class).addOrder(Order.asc("nama"))
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.ilike("nama", namaItemBiaya.getText().trim(), MatchMode.ANYWHERE))
				.setMaxResults(Common.MAX_RESULT).list();
		ListModel strset = new SimpleListModel(itemBiaya);
		grid.setRowRenderer(new ItemBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
