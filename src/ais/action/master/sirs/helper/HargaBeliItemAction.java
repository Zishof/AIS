package ais.action.master.sirs.helper;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.library.Penyedia;
import ais.database.model.sirs.HargaBeliItem;
import ais.database.model.sirs.ItemMedis;
import ais.database.model.sirs.JenisItemMedis;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

/**
 * Controller/action ZK untuk harga beli item. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * Window}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code List vendors}, {@code boolean edit},
 * {@code MyTextbox kodeItemMedisan}, {@code MyTextbox nama}, {@code Combobox jenisItemMedis}, {@code Grid grid};
 * pembacaan/pencarian ({@code loadData()}); operasi domain lain ({@code display()}); konfigurasi constructor:
 * {@code edit}, {@code vendors}. Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang
 * disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see Window
 */
public class HargaBeliItemAction extends Window {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private List<Penyedia> vendors;
	private boolean edit = false;
	private MyTextbox kodeItemMedisan;
	private MyTextbox nama;
	private Combobox jenisItemMedis;

	private Grid grid;

	@SuppressWarnings("unchecked")
	public HargaBeliItemAction() {
		super();
		vendors = ConstantValues.simpleList(HibernateUtil.currentSession().createCriteria(Penyedia.class),
				Penyedia.class);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		display();
	}

	@SuppressWarnings("unchecked")
	public HargaBeliItemAction(String title, String border, boolean closable) {
		super(title, border, closable);
		vendors = ConstantValues.simpleList(HibernateUtil.currentSession().createCriteria(Penyedia.class),
				Penyedia.class);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		display();
	}

	class ItemMedisRenderer extends ais.ui.util.MyRowRenderer {

		private Session session = HibernateUtil.currentSession();

		public ItemMedisRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final ItemMedis item = (ItemMedis) data;
			new Label(item.getKode()).setParent(row);
			new Label(item.getNama()).setParent(row);

			for (final Penyedia vendor : vendors) {
				final HargaBeliItem hargaBeliItemMedis = (HargaBeliItem) session.createCriteria(HargaBeliItem.class)
						.add(Restrictions.eq("item", item)).add(Restrictions.eq("vendor", vendor)).setMaxResults(1)
						.uniqueResult();

				final MyDoublebox doublebox = new MyDoublebox(
						hargaBeliItemMedis == null || hargaBeliItemMedis.getHargaBeli() == null ? 0.0
								: hargaBeliItemMedis.getHargaBeli());

				doublebox.setParent(row);
				doublebox.setDisabled(!edit);
				doublebox.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Session session = HibernateUtil.currentSession();
						HargaBeliItem myHargaBeliItem = hargaBeliItemMedis;
						if (myHargaBeliItem == null) {
							myHargaBeliItem = new HargaBeliItem();
							myHargaBeliItem.setItem(item);
							myHargaBeliItem.setPenyedia(vendor);
							myHargaBeliItem.setKeterangan(
									"Harga beli " + item.getNama() + " untuk supplier " + vendor.getNama());
						}
						myHargaBeliItem.setHargaBeli(doublebox.getValue() == null ? 0.0 : doublebox.getValue());
						Common.refreshUpdate(session, (myHargaBeliItem));

						ItemMedis item = hargaBeliItemMedis.getItem();
						item.setDefaultHargaBeli(myHargaBeliItem.getHargaBeli());
						session.saveOrUpdate(session.merge(item));
					}
				});
			}

		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {
		JenisItemMedis jenisItemMedis = (JenisItemMedis) (this.jenisItemMedis.getSelectedItem() == null ? null
				: this.jenisItemMedis.getSelectedItem().getValue());
		Session session = HibernateUtil.currentSession();
		List<ItemMedis> items = ConstantValues
				.simpleList(session.createCriteria(ItemMedis.class).addOrder(Order.asc("nama"))
						.add(jenisItemMedis == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("jenisItem", jenisItemMedis))
						.add(Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))
						.add(Restrictions.ilike("kode", kodeItemMedisan.getValue().trim(), MatchMode.ANYWHERE))
						.setMaxResults(Common.MAX_RESULT_50), ItemMedis.class);

		ListModel strset = new SimpleListModel(items);
		grid.setRowRenderer(new ItemMedisRenderer());
		grid.setModel(strset);
		grid.renderAll();

	}

	private void display() {

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		Grid searchgrid = new Grid();
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Kode Barang Medis")));
		row.appendChild(kodeItemMedisan = new MyTextbox());
		kodeItemMedisan.setWidth("90%");
		kodeItemMedisan.addEventListener("onOK", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Barang Medis")));
		row.appendChild(nama = new MyTextbox());
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Barang Medis")));
		row.appendChild(jenisItemMedis = new Combobox());
		Common.insertCombo(jenisItemMedis, "nama", JenisItemMedis.class);
		jenisItemMedis.setWidth("90%");
		jenisItemMedis.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("25px");
		toolbar.setParent(div);

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Cari", "/img/search.gif");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				loadData(null);
			}
		});
		button.setParent(toolbar);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new Grid();
		grid.setParent(center);

		Columns columns = new Columns();
		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("Kode Barang Medis");
		column.setWidth("100px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Barang Medis");
		column.setWidth("200px");

		for (Penyedia vendor : vendors) {
			column = new Column();
			column.setParent(columns);
			column.setLabel(vendor.getNama());
		}

		loadData(null);
	}

}
