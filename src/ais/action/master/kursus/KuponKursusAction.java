package ais.action.master.kursus;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.kursus.helper.AmbilDataProdukKursusBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.kursus.KuponKursus;
import ais.database.model.kursus.ProdukKursus;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Composer ZK CRUD untuk {@link KuponKursus} (kupon diskon kursus): kode, nama, tipe diskon
 * (persen/nominal via {@link KuponKursus#PERSEN}/{@link KuponKursus#NOMINAL}), nilai, masa berlaku,
 * batas pemakaian (kosong = tak terbatas), dan cakupan (satu {@link ProdukKursus} tertentu atau
 * kosong = berlaku untuk semua kursus). Grid menampilkan pemakaian saat ini vs batas
 * ({@code jumlahDipakai / batasPemakaian}) dan checkbox aktif yang langsung tersimpan saat diubah.
 */
public class KuponKursusAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Combobox tipeDiskon;
	private Decimalbox nilai;
	private Datebox berlakuMulai;
	private Datebox berlakuSampai;
	private Intbox batasPemakaian;
	private AmbilDataProdukKursusBanbox produkKursusBanbox;
	private Checkbox aktif;

	private boolean edit = false;
	private boolean delete = false;

	private KuponKursus kuponKursus;
	private MyToolbarbuttonConfig add;

	/** Menjalankan pemeriksaan keamanan standar sebelum komponen ZK di-compose. */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Inisialisasi layar: privilese CREATE/UPDATE/DELETE, pencarian awal, paging, dan tombol cetak data. */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "id", "kode", "nama", "tipeDiskon", "nilai", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KuponKursus.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);
	}

	/** Renderer baris grid {@link KuponKursus}: kode, nama (dengan revisi), tipe+nilai diskon, cakupan produk kursus, pemakaian vs batas, checkbox aktif (autosave), dan tombol ubah/hapus. */
	class KuponKursusRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KuponKursus kuponKursus = (KuponKursus) arg1;
			new Label(kuponKursus.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KuponKursus.class, kuponKursus, kuponKursus.getNama()).setParent(arg0);

			new Label(kuponKursus.getTipeDiskon() + " - "
					+ (KuponKursus.PERSEN.equals(kuponKursus.getTipeDiskon())
							? Common.numberFormat.get().format(kuponKursus.getNilai()) + "%"
							: Common.numberFormat.get().format(kuponKursus.getNilai()))).setParent(arg0);

			new Label(kuponKursus.getProdukKursus() == null ? "Semua Kursus" : kuponKursus.getProdukKursus().getNama())
					.setParent(arg0);

			new Label((kuponKursus.getJumlahDipakai())
					+ (kuponKursus.getBatasPemakaian() == null ? "" : " / " + kuponKursus.getBatasPemakaian()))
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(kuponKursus.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kuponKursus.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(kuponKursus);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, kuponKursus, KuponKursusAction.this).setParent(arg0);
		}

	}

	/** Handler tombol "Tambah": membuka form dengan {@link KuponKursus} kosong baru. */
	public void onAdd(Event event) throws Exception {
		init(new KuponKursus());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/** Implementasi {@link DataInitDefault}: membuka form ubah untuk {@code obj} (dipanggil mis. dari tombol ubah baris {@link Common#copyEditDeleteButtons}). */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kuponKursus = (KuponKursus) obj;
		init(kuponKursus);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/** Membangun form tambah/ubah {@link KuponKursus}: kode, nama, keterangan, tipe+nilai diskon, rentang berlaku, batas pemakaian, cakupan produk kursus, dan status aktif, plus toolbar Batal/Simpan. */
	private void init(KuponKursus kuponKursus) {
		this.kuponKursus = kuponKursus;
		addWindow.setTitle(kuponKursus.getId() == null ? "Tambah Kupon Kursus" : "Ubah Kupon Kursus");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kupon *"));
		row.appendChild(kode = new Textbox(kuponKursus.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(kuponKursus.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kuponKursus.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tipe Diskon"));
		row.appendChild(tipeDiskon = new Combobox());
		tipeDiskon.setReadonly(true);
		tipeDiskon.setWidth("90%");
		for (String s : new String[] { KuponKursus.PERSEN, KuponKursus.NOMINAL }) {
			Comboitem item = tipeDiskon.appendItem(s);
			item.setValue(s);
			if (s.equals(kuponKursus.getTipeDiskon())) {
				tipeDiskon.setSelectedItem(item);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai (% atau Rp) *"));
		row.appendChild(nilai = new Decimalbox());
		nilai.setValue(java.math.BigDecimal.valueOf(kuponKursus.getNilai()));
		nilai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai"));
		row.appendChild(berlakuMulai = new Datebox(kuponKursus.getBerlakuMulai()));
		berlakuMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Sampai"));
		row.appendChild(berlakuSampai = new Datebox(kuponKursus.getBerlakuSampai()));
		berlakuSampai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Batas Pemakaian (kosong = tak terbatas)"));
		row.appendChild(batasPemakaian = new Intbox(kuponKursus.getBatasPemakaian()));
		batasPemakaian.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Khusus Kursus (kosong = semua kursus)"));
		produkKursusBanbox = new AmbilDataProdukKursusBanbox();
		produkKursusBanbox
				.setValue(kuponKursus.getProdukKursus() == null ? "" : kuponKursus.getProdukKursus().getNama());
		produkKursusBanbox.setAttribute("produkKursus", kuponKursus.getProdukKursus());
		row.appendChild(produkKursusBanbox);
		produkKursusBanbox.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aktif"));
		row.appendChild(aktif = new Checkbox());
		aktif.setChecked(kuponKursus.getAktif());

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);
	}

	/** Memvalidasi (kode dan nilai kupon wajib terisi, nilai harus &gt; 0) dan menyimpan {@link KuponKursus} dari nilai form saat ini. @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal. */
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Kode Kupon harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nilai.getValue() == null || nilai.getValue().doubleValue() <= 0) {
			MyMessageboxConfig.show("Nilai kupon harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kuponKursus.getId() != null) {
			kuponKursus = (KuponKursus) session.load(KuponKursus.class, kuponKursus.getId());
		}

		kuponKursus.setKode(kode.getValue());
		kuponKursus.setNama(nama.getValue());
		kuponKursus.setKeterangan(keterangan.getValue());
		kuponKursus.setTipeDiskon(
				tipeDiskon.getSelectedItem() == null ? KuponKursus.PERSEN : (String) tipeDiskon.getSelectedItem().getValue());
		kuponKursus.setNilai(nilai.getValue().doubleValue());
		kuponKursus.setBerlakuMulai(berlakuMulai.getValue());
		kuponKursus.setBerlakuSampai(berlakuSampai.getValue());
		kuponKursus.setBatasPemakaian(batasPemakaian.getValue());
		kuponKursus.setProdukKursus((ProdukKursus) produkKursusBanbox.getAttribute("produkKursus"));
		kuponKursus.setAktif(aktif.isChecked());

		Common.refreshSaveOrUpdate(session, kuponKursus);

		return true;
	}

	/** Implementasi {@link DataCriteria}: membentuk criteria pencarian {@link KuponKursus} berdasarkan filter aktif dan kode/nama (ILIKE), diurut kode bila {@code order} true. */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KuponKursus.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("kode"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.or(Restrictions.ilike("kode", searchnama.getValue().trim(), MatchMode.ANYWHERE),
						Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Implementasi {@link DataSearchDefault}: menjalankan ulang pencarian dan memuat ulang {@link #grid} serta {@link #paging}. */
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KuponKursus> kuponKursus = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kuponKursus);
		grid.setRowRenderer(new KuponKursusRenderer());
		grid.setModelCheckMobile(strset);
	}

}
