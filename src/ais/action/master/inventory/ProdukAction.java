package ais.action.master.inventory;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
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
import org.zkoss.zul.East;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
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
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.asset.helper.AmbilDataMasterAssetBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.inventory.helper.BahanBakuProdukHelper;
import ais.action.master.inventory.helper.ProdukKomentarHelper;
import ais.action.master.inventory.helper.ProdukPunyaGambarFotoHelper;
import ais.action.master.inventory.helper.ProdukUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.asset.MasterAsset;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.inventory.JenisProduk;
import ais.database.model.inventory.KebijakanRetur;
import ais.database.model.inventory.Produk;
import ais.database.model.inventory.Toko;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk produk. Tipe ini merupakan titik masuk UI yang menghubungkan event
 * layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Combobox
 * searchjenisProduk}, {@code Checkbox searchaktif}, {@code Combobox searchtoko}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code initDetail()}, {@code init()}, {@code
 * initCriteria()}); pembacaan/pencarian ({@code tampilkanStatistikProduk()}, {@code onSearchDefault()}, {@code
 * tampilkanIntroDashboardInventoryV1()}); validasi/perhitungan ({@code hitungStokAset()}, {@code
 * checkNamaProduk()}); mutasi data ({@code onSave()}); operasi domain lain ({@code jadikanLabelHarga()}, {@code
 * resolvePemasokZk()}, {@code resolveSatuanZk()}, {@code onAdd()}, {@code escapeDashboardHtmlInventoryV1()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class ProdukAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Combobox searchjenisProduk;
	private Checkbox searchaktif;
	private Combobox searchtoko;

	private Textbox nama;
	private Textbox barcode;
	private Textbox keterangan;
	/** Pemasok utama & satuan (UOM) -- isian bebas; master dibuat otomatis bila nama belum ada. */
	private Textbox pemasokNama;
	private Textbox satuanNama;

	private boolean edit = false;
	private boolean delete = false;

	private Produk produk;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private Textbox catatan;
	private Combobox jenisProduk;
	private Combobox kebijakanRetur;
	private Combobox toko;
	private MyDoublebox hargaBeli;
	private MyDoublebox hargaJual;
	private Combobox metodeHpp;
	private Combobox izinkanJualMinusStok;
	private Combobox grupProduk;
	private Toko currentToko;
	private Textbox imageUrl;
	private MyGrid gridGambar;
	protected FotoGambarProduk fotoGambarProduk;
	private BahanBakuProdukHelper bahanBakuHelper;
	private AmbilDataMasterAssetBanbox masterAssetBanbox;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tampilkanIntroDashboardInventoryV1(comp, "Dashboard Produk dan Katalog POS", "Menampilkan daftar produk, kode, jenis, harga, toko, gambar, dan status aktif agar pengelola dapat menjaga katalog kasir tetap akurat. Informasi ini membantu memastikan barang yang dijual sudah sesuai harga dan toko yang benar.");

		tampilkanStatistikProduk(comp);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.insertComboDanSemua(searchtoko, "nama", Toko.class);
		currentToko = Common.getCurrentToko();
		if (currentToko != null) {
			Common.selectComboItem(true, searchtoko, currentToko);
			searchtoko.setDisabled(!currentToko.getBolehMelihatTokolain());
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

		String[] contents = new String[] { "id", "kode", "nama", "jenisProduk", "hargaBeli", "hargaJual", "catatan",
				"toko", "imageUrl", "imagePath", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, Produk.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		// Cetak Price Tag / POP (A2/A4/A5) — buka layar terpisah.
		MyToolbarbuttonConfig priceTag = new MyToolbarbuttonConfig("Cetak Price Tag", "/img/print.png");
		priceTag.addEventListener("onClick", new org.zkoss.zk.ui.event.EventListener() {
			public void onEvent(org.zkoss.zk.ui.event.Event e) throws Exception {
				org.zkoss.zk.ui.util.Clients.evalJavaScript(
						"window.open('" + Common.ROOT + "/pages/master/inventory/pricetag_kantin.zul','_blank')");
			}
		});
		Common.appendKeToolbar(priceTag, add, comp);
	}

	/**
	 * Fitur "Dasbor Statistik Produk" -- gap-closure permintaan kartu ringkasan (total/aktif/nonaktif/
	 * stok habis-rendah/nilai stok) + 3 rincian batang (kategori/pemasok/rentang harga) LANGSUNG di
	 * layar CRUD Produk ini, BUKAN cuma lewat tab "Produk & Stok" di Dasbor Kantin terpisah. Query
	 * SQL SENGAJA disalin persis dari {@code KantinHelper.produkStatistik} (aksi server yang SAMA
	 * dipakai Desktop/Android) supaya angkanya taat asas di ketiga tampilan -- BUKAN dipanggil lewat
	 * lapisan JSON API itu (yang bentuknya {@code JSONObject request/hasil}, canggung dipakai di sini)
	 * krn ZK sudah punya sesi Hibernate native sendiri, jadi query SQL langsung lebih pas. Dihitung
	 * SEKALI saat halaman dibuka (scope ke toko login bila terkunci, else SEMUA toko) -- TIDAK ikut
	 * berubah mengikuti filter Kode/Nama/Toko di atas (angka "kondisi katalog saat ini", bukan hasil
	 * pencarian sesaat).
	 */
	private void tampilkanStatistikProduk(Component comp) {
		try {
			Toko tokoScope = Common.getCurrentToko();
			Session session = HibernateUtil.currentSession();
			java.sql.Connection conn = session.connection();
			String kondisiToko = tokoScope != null ? " AND p.toko = ?" : "";

			long totalProduk = 0, totalAktif = 0, totalNonaktif = 0, stokHabis = 0, stokRendah = 0;
			double totalNilaiStok = 0;
			java.sql.PreparedStatement psKpi = conn.prepareStatement(
					"SELECT COUNT(*), COUNT(CASE WHEN COALESCE(p.aktif,true)=true THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.aktif,true)=false THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.stok,0)<=0 THEN 1 END), "
							+ "COUNT(CASE WHEN COALESCE(p.stok,0)>0 AND COALESCE(p.stok,0)<=5 THEN 1 END), "
							+ "COALESCE(SUM(COALESCE(p.stok,0)*COALESCE(p.hargabeli,0)),0) "
							+ "FROM koperasi.produk p WHERE 1=1" + kondisiToko);
			if (tokoScope != null) psKpi.setLong(1, tokoScope.getId().longValue());
			java.sql.ResultSet rsKpi = psKpi.executeQuery();
			if (rsKpi.next()) {
				totalProduk = rsKpi.getLong(1);
				totalAktif = rsKpi.getLong(2);
				totalNonaktif = rsKpi.getLong(3);
				stokHabis = rsKpi.getLong(4);
				stokRendah = rsKpi.getLong(5);
				totalNilaiStok = rsKpi.getDouble(6);
			}
			rsKpi.close(); psKpi.close();

			java.util.LinkedHashMap<String, Double> byKategori = new java.util.LinkedHashMap<String, Double>();
			java.sql.PreparedStatement psKategori = conn.prepareStatement(
					"SELECT COALESCE(j.nama,'Tanpa Kategori') lbl, COUNT(*) cnt FROM koperasi.produk p "
							+ "LEFT JOIN koperasi.jenis_produk j ON p.jenis_produk = j.id WHERE 1=1" + kondisiToko
							+ " GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			if (tokoScope != null) psKategori.setLong(1, tokoScope.getId().longValue());
			java.sql.ResultSet rsKategori = psKategori.executeQuery();
			while (rsKategori.next()) byKategori.put(rsKategori.getString(1), rsKategori.getDouble(2));
			rsKategori.close(); psKategori.close();

			java.util.LinkedHashMap<String, Double> byPemasok = new java.util.LinkedHashMap<String, Double>();
			java.sql.PreparedStatement psPemasok = conn.prepareStatement(
					"SELECT COALESCE(s.nama,'Tanpa Pemasok') lbl, COUNT(*) cnt FROM koperasi.produk p "
							+ "LEFT JOIN koperasi.pemasok_produk s ON p.pemasok = s.id WHERE 1=1" + kondisiToko
							+ " GROUP BY lbl ORDER BY cnt DESC LIMIT 8");
			if (tokoScope != null) psPemasok.setLong(1, tokoScope.getId().longValue());
			java.sql.ResultSet rsPemasok = psPemasok.executeQuery();
			while (rsPemasok.next()) byPemasok.put(rsPemasok.getString(1), rsPemasok.getDouble(2));
			rsPemasok.close(); psPemasok.close();

			java.util.LinkedHashMap<String, Double> byHarga = new java.util.LinkedHashMap<String, Double>();
			java.sql.PreparedStatement psHarga = conn.prepareStatement(
					"SELECT CASE WHEN COALESCE(p.hargajual,0) < 5000 THEN '< Rp 5rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 10000 THEN 'Rp 5rb - 10rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 20000 THEN 'Rp 10rb - 20rb' "
							+ "WHEN COALESCE(p.hargajual,0) < 50000 THEN 'Rp 20rb - 50rb' "
							+ "ELSE '>= Rp 50rb' END lbl, COUNT(*) cnt FROM koperasi.produk p WHERE 1=1" + kondisiToko
							+ " GROUP BY lbl ORDER BY MIN(COALESCE(p.hargajual,0))");
			if (tokoScope != null) psHarga.setLong(1, tokoScope.getId().longValue());
			java.sql.ResultSet rsHarga = psHarga.executeQuery();
			while (rsHarga.next()) byHarga.put(rsHarga.getString(1), rsHarga.getDouble(2));
			rsHarga.close(); psHarga.close();

			java.util.List<ais.ui.util.DashboardUiKit.Stat> kartu = new java.util.ArrayList<ais.ui.util.DashboardUiKit.Stat>();
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Total Produk", ais.ui.util.DashboardUiKit.money(totalProduk), null, ais.ui.util.DashboardUiKit.PRIMARY));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Aktif", ais.ui.util.DashboardUiKit.money(totalAktif), null, ais.ui.util.DashboardUiKit.GOOD));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Non-Aktif", ais.ui.util.DashboardUiKit.money(totalNonaktif), null, ais.ui.util.DashboardUiKit.MUTED));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Stok Habis", ais.ui.util.DashboardUiKit.money(stokHabis), null, ais.ui.util.DashboardUiKit.BAD));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Stok Rendah (<=5)", ais.ui.util.DashboardUiKit.money(stokRendah), null, ais.ui.util.DashboardUiKit.WARN));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Nilai Stok (Modal)", "Rp " + ais.ui.util.DashboardUiKit.money(totalNilaiStok), null, ais.ui.util.DashboardUiKit.PRIMARY));

			StringBuilder sb = new StringBuilder();
			sb.append(ais.ui.util.DashboardUiKit.cards(kartu));
			sb.append(ais.ui.util.DashboardUiKit.openGrid(260));
			sb.append(ais.ui.util.DashboardUiKit.barList("Per Kategori", null, byKategori, ais.ui.util.DashboardUiKit.PRIMARY, "produk", false, "Belum ada produk."));
			sb.append(ais.ui.util.DashboardUiKit.barList("Per Pemasok/Vendor", null, byPemasok, ais.ui.util.DashboardUiKit.ACCENT, "produk", false, "Belum ada produk."));
			sb.append(ais.ui.util.DashboardUiKit.barList("Per Rentang Harga Jual", null, byHarga, ais.ui.util.DashboardUiKit.GOOD, "produk", false, "Belum ada produk."));
			sb.append(ais.ui.util.DashboardUiKit.closeGrid());

			ais.ui.util.DashboardUiKit.html(sb.toString()).setParent(comp);
		} catch (Exception e) {
			// Dasbor statistik gagal dihitung TIDAK BOLEH menggagalkan layar Produk itu sendiri --
			// katalog/CRUD tetap harus bisa dipakai walau statistik tak tampil.
			ais.common.ErrorAuditUtil.record(e, "auto-audit ProdukAction.tampilkanStatistikProduk");
		}
	}

	/**
	 * Sembunyikan {@code box} dan tampilkan nilainya sbg {@link org.zkoss.zul.Label}
	 * di posisi yang sama. Komponen aslinya sengaja TIDAK dibuang supaya
	 * {@code getValue()} saat simpan tetap mengembalikan nilai lama (tidak berubah,
	 * jadi lolos gerbang), bukan null yang akan menghapus harga.
	 */
	private void jadikanLabelHarga(MyDoublebox box) {
		if (box == null) {
			return;
		}
		Double nilai = box.getValue();
		org.zkoss.zul.Label label = new org.zkoss.zul.Label(
				nilai == null ? "-" : new java.text.DecimalFormat("#,##0").format(nilai.doubleValue()));
		label.setStyle("font-weight:600;");
		if (box.getParent() != null) {
			box.getParent().insertBefore(label, box);
		}
		box.setVisible(false);
	}

	/** Cari master pemasok by nama (case-insensitive); buat baru bila belum ada. */
	private static ais.database.model.inventory.PemasokProduk resolvePemasokZk(Session session, String nama) {
		String bersih = nama == null ? "" : nama.trim();
		if (bersih.length() == 0) {
			return null;
		}
		ais.database.model.inventory.PemasokProduk ada = (ais.database.model.inventory.PemasokProduk) session
				.createCriteria(ais.database.model.inventory.PemasokProduk.class)
				.add(Restrictions.ilike("nama", bersih)).setMaxResults(1).uniqueResult();
		if (ada != null) {
			return ada;
		}
		ais.database.model.inventory.PemasokProduk baru = new ais.database.model.inventory.PemasokProduk();
		baru.setNama(bersih);
		session.save(baru);
		return baru;
	}

	/** Cari master satuan by nama (case-insensitive); buat baru bila belum ada. */
	private static ais.database.model.inventory.SatuanProduk resolveSatuanZk(Session session, String nama) {
		String bersih = nama == null ? "" : nama.trim();
		if (bersih.length() == 0) {
			return null;
		}
		ais.database.model.inventory.SatuanProduk ada = (ais.database.model.inventory.SatuanProduk) session
				.createCriteria(ais.database.model.inventory.SatuanProduk.class)
				.add(Restrictions.ilike("nama", bersih)).setMaxResults(1).uniqueResult();
		if (ada != null) {
			return ada;
		}
		ais.database.model.inventory.SatuanProduk baru = new ais.database.model.inventory.SatuanProduk();
		baru.setNama(bersih);
		session.save(baru);
		return baru;
	}

	class ProdukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Produk produk = (Produk) arg1;

			Component image = ProdukUtil.generateImage(produk);
			image.setParent(arg0);

			new Label(produk.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Produk.class, produk, produk.getNama()).setParent(arg0);
			new Label(produk.getJenisProduk() == null ? "" : produk.getJenisProduk().getNama()).setParent(arg0);
			new Label(produk.getHargaBeli() == null ? "" : Common.numberFormat.get().format(produk.getHargaBeli()))
					.setParent(arg0);
			new Label(produk.getHargaJual() == null ? "" : Common.numberFormat.get().format(produk.getHargaJual()))
					.setParent(arg0);
			new Label(produk.getToko() == null ? "Semua" : produk.getToko().getNama()).setParent(arg0);
			new Label(produk.getCatatan()).setParent(arg0);
			new Label(produk.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(produk.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					produk.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(produk);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, produk, ProdukAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Produk());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		produk = (Produk) obj;
		init(produk);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	protected void initDetail(final Produk produk, Component component) throws Exception {
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(component);
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");

		Tabs tabs = new Tabs();
		tabs.setParent(tabbox);

		final MyTabConfig tabGambar = new MyTabConfig("Gambar");
		tabGambar.setParent(tabs);

		final MyTabConfig tabKomentar = new MyTabConfig("Komentar-Komentar");
		tabKomentar.setParent(tabs);
		tabKomentar.setVisible(false);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		final Tabpanel tabpanelGambar = new ais.ui.util.MyTabpanel();
		tabpanelGambar.setParent(tabpanels);

		final Tabpanel tabpanelKomentar = new ais.ui.util.MyTabpanel();
		tabpanelKomentar.setParent(tabpanels);

		tabpanelGambar.appendChild(new ProdukPunyaGambarFotoHelper(gridGambar = new MyGrid()).initDetail(produk));

		tabpanelKomentar.appendChild(new ProdukKomentarHelper(new MyGrid()).initDetail(produk));
	}

	private void init(Produk produk) throws Exception {
		this.produk = produk;
		addWindow.setTitle(produk.getId() == null ? "Tambah Produk" : "Ubah Produk");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		East east = new East();
		east.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(east, true);
		east.setWidth("50%");

		initDetail(produk, east);

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

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Produk *"));
		row.appendChild(kode = new Textbox(produk.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("UPC/Barcode"));
		row.appendChild(barcode = new Textbox(produk.getBarcode()));
		barcode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Produk *"));
		row.appendChild(nama = new Textbox(produk.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Produk *"));
		row.appendChild(jenisProduk = new Combobox());
		Common.insertCombo(jenisProduk, "nama", JenisProduk.class);
		Common.selectComboItem(jenisProduk, produk.getJenisProduk());
		jenisProduk.setWidth("90%");
		jenisProduk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kebijakan Retur"));
		row.appendChild(kebijakanRetur = new Combobox());
		Common.insertCombo(kebijakanRetur, "nama", KebijakanRetur.class, Restrictions.eq("aktif", true));
		KebijakanRetur kebijakanTerpilih = produk.getKebijakanRetur();
		if (kebijakanTerpilih == null) {
			kebijakanTerpilih = (KebijakanRetur) HibernateUtil.currentSession().createCriteria(KebijakanRetur.class)
					.add(Restrictions.ilike("nama", KebijakanRetur.TANPA_KEBIJAKAN)).setMaxResults(1).uniqueResult();
		}
		Common.selectComboItem(kebijakanRetur, kebijakanTerpilih);
		kebijakanRetur.setWidth("90%");
		kebijakanRetur.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko"));
		row.appendChild(toko = new Combobox());
		Common.insertComboDanSemua(toko, "nama", Toko.class, Restrictions.eq("aktif", true));
		Common.selectComboItem(toko, produk.getToko());
		toko.setWidth("90%");
		toko.setReadonly(true);

		if (currentToko != null) {
			Common.selectComboItem(true, toko, currentToko);
			toko.setDisabled(!currentToko.getBolehMelihatTokolain());
		}

		// Grup harga terpusat lintas toko (opsional) -- lihat javadoc GrupProduk. Bila diisi,
		// HPP/harga jual produk ini akan DITIMPA setiap kali grupnya disimpan dari layar
		// Grup Produk; kosong = harga tetap dikelola per toko seperti sebelumnya.
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Grup Produk (Harga Terpusat)"));
		row.appendChild(grupProduk = new Combobox());
		Common.insertComboDanSemua(grupProduk, new String[] { "nama" }, "kode",
				ais.database.model.inventory.GrupProduk.class, "== Tanpa Grup ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, grupProduk, produk.getGrupProduk());
		grupProduk.setWidth("90%");
		grupProduk.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Beli"));
		row.appendChild(hargaBeli = new MyDoublebox(produk.getHargaBeli()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Harga Jual *"));
		row.appendChild(hargaJual = new MyDoublebox(produk.getHargaJual()));

		// Tanpa hak ubah harga: nilainya disajikan sbg LABEL, bukan kolom isian
		// yang di-disable -- kolom disable masih terlihat spt tempat mengetik.
		// Nilai aslinya tetap dipegang komponen tersembunyi supaya simpan tidak
		// mengosongkan harga yang sudah ada.
		Toko tokoFormHarga = produk.getToko() != null ? produk.getToko() : currentToko;
		if (!ais.action.master.inventory.HargaAksesUtil.bolehUbahHarga(
				tokoFormHarga, ais.common.Common.getCurrentUser())) {
			jadikanLabelHarga(hargaBeli);
			jadikanLabelHarga(hargaJual);
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(""));
			org.zkoss.zul.Label catatanHarga = new org.zkoss.zul.Label(
					ais.action.master.inventory.HargaAksesUtil.pesanDitolak());
			catatanHarga.setStyle("color:#B45309;font-size:11px;");
			row.appendChild(catatanHarga);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Metode HPP (Posting)"));
		row.appendChild(metodeHpp = new Combobox());
		metodeHpp.setReadonly(true);
		metodeHpp.setWidth("90%");
		{
			Comboitem ciDefault = new Comboitem("Default (Rata-rata Kulakan, fallback Harga Beli)");
			ciDefault.setValue("");
			ciDefault.setParent(metodeHpp);
			Comboitem ciAvg = new Comboitem("Rata-rata Biaya Kulakan");
			ciAvg.setValue("RATA_RATA_KULAKAN");
			ciAvg.setParent(metodeHpp);
			Comboitem ciLast = new Comboitem("Biaya Kulakan Terakhir");
			ciLast.setValue("KULAKAN_TERAKHIR");
			ciLast.setParent(metodeHpp);
			Comboitem ciProduk = new Comboitem("Harga Beli Produk (Statis)");
			ciProduk.setValue("HARGA_BELI_PRODUK");
			ciProduk.setParent(metodeHpp);
			String m = produk.getMetodeHpp();
			if ("RATA_RATA_KULAKAN".equals(m)) {
				metodeHpp.setSelectedItem(ciAvg);
			} else if ("KULAKAN_TERAKHIR".equals(m)) {
				metodeHpp.setSelectedItem(ciLast);
			} else if ("HARGA_BELI_PRODUK".equals(m)) {
				metodeHpp.setSelectedItem(ciProduk);
			} else {
				metodeHpp.setSelectedItem(ciDefault);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Aturan Jual Saat Stok Kurang"));
		row.appendChild(izinkanJualMinusStok = new Combobox());
		izinkanJualMinusStok.setReadonly(true);
		izinkanJualMinusStok.setWidth("90%");
		{
			Comboitem ciIkut = new Comboitem("Ikut Pengaturan Toko (default)");
			ciIkut.setValue("");
			ciIkut.setParent(izinkanJualMinusStok);
			Comboitem ciBoleh = new Comboitem("Selalu Boleh Dijual Walau Stok Minus");
			ciBoleh.setValue("true");
			ciBoleh.setParent(izinkanJualMinusStok);
			Comboitem ciBlokir = new Comboitem("Wajib Diblokir Jika Stok Tidak Cukup");
			ciBlokir.setValue("false");
			ciBlokir.setParent(izinkanJualMinusStok);
			if (Boolean.TRUE.equals(produk.getIzinkanJualMinusStok())) {
				izinkanJualMinusStok.setSelectedItem(ciBoleh);
			} else if (Boolean.FALSE.equals(produk.getIzinkanJualMinusStok())) {
				izinkanJualMinusStok.setSelectedItem(ciBlokir);
			} else {
				izinkanJualMinusStok.setSelectedItem(ciIkut);
			}
		}

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bahan Baku (Resep) & HPP"));
		Toko scopeBahanBaku = produk.getToko() != null ? produk.getToko() : currentToko;
		bahanBakuHelper = new BahanBakuProdukHelper(produk.getId(), hargaBeli);
		row.appendChild(bahanBakuHelper.build(scopeBahanBaku, produk.getBahanBaku()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Barang Persediaan (Aset)"));
		Vbox boxAset = new Vbox();
		boxAset.setWidth("90%");
		masterAssetBanbox = new AmbilDataMasterAssetBanbox(MasterAsset.TIPE_HABIS_PAKAI);
		masterAssetBanbox.setWidth("100%");
		masterAssetBanbox.setTooltiptext("Tautkan ke barang persediaan modul Aset/Pengadaan (opsional)");
		if (produk.getMasterAsset() != null) {
			masterAssetBanbox.setValue(produk.getMasterAsset().toString());
			masterAssetBanbox.setAttribute("masterAsset", produk.getMasterAsset());
		}
		boxAset.appendChild(masterAssetBanbox);
		if (produk.getId() != null && produk.getMasterAsset() != null) {
			double stokAset = hitungStokAset(produk.getMasterAsset().getId());
			double stokKantin = produk.getStok() == null ? 0 : produk.getStok();
			double selisih = stokKantin - stokAset;
			Label lblRekon = new Label("Stok Aset: " + Common.numberFormat.get().format(stokAset) + "   ·   Stok Kantin: "
					+ Common.numberFormat.get().format(stokKantin) + "   ·   Selisih: "
					+ Common.numberFormat.get().format(selisih));
			lblRekon.setStyle("font-size:11px;font-weight:700;color:" + (Math.abs(selisih) > 0.001 ? "#dc2626" : "#16a34a") + ";");
			boxAset.appendChild(lblRekon);
		}
		row.appendChild(boxAset);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Deskripsi Produk"));
		row.appendChild(catatan = new Textbox(produk.getCatatan()));
		catatan.setWidth("90%");
		catatan.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Link Gambar"));
		row.appendChild(imageUrl = new Textbox(produk.getImageUrl()));
		imageUrl.setWidth("90%");
		imageUrl.setRows(2);

		// Pemasok Utama & Satuan -- SEBELUMNYA hanya bisa terisi lewat impor Excel,
		// sehingga ekspor "Daftar Barang dan Jasa" kosong utk barang yg dibuat dari
		// layar ini. Isian bebas: master dibuat otomatis saat simpan bila belum ada.
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Pemasok Utama"));
		row.appendChild(pemasokNama = new Textbox(produk.getPemasok() == null ? "" : produk.getPemasok().getNama()));
		pemasokNama.setWidth("90%");
		pemasokNama.setTooltiptext("Kosongkan bila tidak ada. Contoh: AB Grosir");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan"));
		row.appendChild(satuanNama = new Textbox(produk.getSatuan() == null ? "" : produk.getSatuan().getNama()));
		satuanNama.setWidth("90%");
		satuanNama.setTooltiptext("Kosongkan bila tidak ada. Contoh: Pcs");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(produk.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gambar Utama Produk"));

		fotoGambarProduk = null;

		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(row);

		Common.createDownloadUploadFoto(vbox, produk, FotoGambarProduk.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				fotoGambarProduk = (FotoGambarProduk) arg0.getData();
			}
		}, true);

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
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Produk belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Produk dengan kode yang unik; (2) pastikan kode tidak kosong atau hanya spasi; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Produk belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Produk dengan nama yang jelas; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		// Gap-closure permintaan user eksplisit: tolak kalau Kode, Barcode, DAN Nama SEKALIGUS kosong
		// (lihat JavaDoc ProdukKunciUnikUtil.adaIdentitasProduk) -- SUDAH tertutup lewat 2 gerbang di
		// atas, dipasang eksplisit di sini sbg lapis kedua yg berdiri sendiri.
		if (!ais.common.ProdukKunciUnikUtil.adaIdentitasProduk(kode.getValue(),
				barcode.getValue(), nama.getValue())) {
			MyMessageboxConfig.show("Mohon maaf, Kode, Barcode, dan Nama Produk tidak boleh kosong semuanya.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenisProduk.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Produk belum dipilih. Langkah yang dapat dilakukan: (1) buka daftar Jenis Produk pada kolom yang tersedia; (2) pilih salah satu Jenis Produk yang sesuai; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (hargaJual.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Harga Jual belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Harga Jual dengan nilai yang sesuai; (2) pastikan nilai harga lebih dari 0; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		List<Row> rowsFotoGambar = gridGambar.getRows().getChildren();
		for (Row row : rowsFotoGambar) {
			FotoGambarProduk fotoGambarProduk = (FotoGambarProduk) row.getAttribute("fotoGambarProduk");
			if (fotoGambarProduk.getProduk() == null) {
				MyMessageboxConfig.show("Mohon maaf, Gambar Produk belum dipilih atau belum diunggah. Langkah yang dapat dilakukan: (1) unggah gambar produk melalui tombol Tambah Gambar; (2) pastikan file gambar sudah terpilih dengan benar; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		boolean i = checkNamaProduk();
		if (i) {
			MyMessageboxConfig.show("Kombinasi Kode + Barcode + Nama produk ini sudah ada di toko yang sama (mengabaikan tanda baca/spasi/huruf besar-kecil). Ubah salah satu kolom, atau buka & ubah produk yang sudah ada.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (produk.getId() != null) {
			produk = (Produk) session.load(Produk.class, produk.getId());

		}

		produk.setKode(kode.getValue());
		String barcodeValue = barcode.getValue() == null ? "" : barcode.getValue().trim();
		produk.setBarcode(barcodeValue.isEmpty() ? null : barcodeValue);
		produk.setJenisProduk((JenisProduk) jenisProduk.getSelectedItem().getValue());
		KebijakanRetur kebijakan = kebijakanRetur == null || kebijakanRetur.getSelectedItem() == null ? null : (KebijakanRetur) kebijakanRetur.getSelectedItem().getValue();
		if (kebijakan == null) {
			kebijakan = (KebijakanRetur) session.createCriteria(KebijakanRetur.class)
					.add(Restrictions.ilike("nama", KebijakanRetur.TANPA_KEBIJAKAN)).setMaxResults(1).uniqueResult();
		}
		produk.setKebijakanRetur(kebijakan);
		produk.setNama(nama.getValue());
		// Kebijakan ubah harga per toko (lihat HargaAksesUtil). Digerbang di sini
		// juga -- bukan cuma di POS Flutter -- supaya aturannya sama lewat ZKoss.
		Toko tokoHarga = (Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue());
		if (tokoHarga == null) {
			tokoHarga = produk.getToko() != null ? produk.getToko() : currentToko;
		}
		double hbBaru = hargaBeli.getValue() == null ? 0d : hargaBeli.getValue().doubleValue();
		double hjBaru = hargaJual.getValue() == null ? 0d : hargaJual.getValue().doubleValue();
		if ((ais.action.master.inventory.HargaAksesUtil.berubah(produk.getHargaBeli(), hbBaru)
				|| ais.action.master.inventory.HargaAksesUtil.berubah(produk.getHargaJual(), hjBaru))
				&& !ais.action.master.inventory.HargaAksesUtil.bolehUbahHarga(
						tokoHarga, ais.common.Common.getCurrentUser())) {
			MyMessageboxConfig.show(ais.action.master.inventory.HargaAksesUtil.pesanDitolak(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		produk.setHargaBeli(hargaBeli.getValue());
		produk.setHargaJual(hargaJual.getValue());
		if (metodeHpp != null && metodeHpp.getSelectedItem() != null) {
			String mVal = (String) metodeHpp.getSelectedItem().getValue();
			produk.setMetodeHpp(mVal == null || mVal.trim().isEmpty() ? null : mVal);
		}
		if (izinkanJualMinusStok.getSelectedItem() == null || izinkanJualMinusStok.getSelectedItem().getValue().equals("")) {
			produk.setIzinkanJualMinusStok(null);
		} else {
			produk.setIzinkanJualMinusStok(Boolean.valueOf((String) izinkanJualMinusStok.getSelectedItem().getValue()));
		}
		produk.setKeterangan(keterangan.getValue());
		produk.setPemasok(resolvePemasokZk(session, pemasokNama.getValue()));
		produk.setSatuan(resolveSatuanZk(session, satuanNama.getValue()));
		produk.setToko((Toko) (toko.getSelectedItem() == null ? null : toko.getSelectedItem().getValue()));
		produk.setGrupProduk(grupProduk.getSelectedIndex() > 0
				&& grupProduk.getSelectedItem().getValue() instanceof ais.database.model.inventory.GrupProduk
						? (ais.database.model.inventory.GrupProduk) grupProduk.getSelectedItem().getValue()
						: null);
		// Diset EKSPLISIT (bukan diandalkan dari @PrePersist/@PreUpdate entity) -- hook JPA lifecycle
		// tidak terjamin terpasang di setup Hibernate 3.6 native project ini (AnnotationConfiguration
		// polos, bukan bootstrap JPA EntityManager) -- lihat JavaDoc gap-closure di
		// KantinHelper.produkSimpan.
		produk.setKunciUnik(ais.common.ProdukKunciUnikUtil.hitung(produk.getKode(), produk.getBarcode(),
				produk.getNama(), produk.getToko() == null ? null : produk.getToko().getId()));
		produk.setImageUrl(imageUrl.getValue());
		if (bahanBakuHelper != null) {
			produk.setBahanBaku(bahanBakuHelper.toJson());
			double hpp = bahanBakuHelper.total();
			if (hpp > 0) {
				produk.setHargaBeli(hpp); // HPP otomatis dari total bahan baku (resep)
			}
		}
		if (masterAssetBanbox != null) {
			Object maSel = masterAssetBanbox.getAttribute("masterAsset");
			String maVal = masterAssetBanbox.getValue();
			if (maVal == null || maVal.trim().isEmpty()) {
				maSel = null; // dikosongkan -> lepas tautan
			}
			produk.setMasterAsset(maSel == null ? null : (MasterAsset) maSel);
		}
		Common.refreshSaveOrUpdate(session, produk);

		Session mysession = StreamingHibernateUtil.getInstance().currentSession();
		try {
			mysession.getTransaction().begin();
			for (Row row : rowsFotoGambar) {
				FotoGambarProduk fotoGambarProduk = (FotoGambarProduk) row.getAttribute("fotoGambarProduk");
				fotoGambarProduk.setProduk(produk.getId());
				mysession.saveOrUpdate(fotoGambarProduk);
			}

			mysession.getTransaction().commit();

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
		}

		StreamingHibernateUtil.getInstance().closeSession();

		if (fotoGambarProduk != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoGambarProduk);
			fotoGambarProduk.setProduk(produk.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoGambarProduk);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Produk.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))

				.add(searchkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("barcode", searchkode.getValue().trim(), MatchMode.ANYWHERE)))

				.add(searchjenisProduk.getSelectedItem() == null
						|| searchjenisProduk.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenisProduk", searchjenisProduk.getSelectedItem().getValue()))

				.add(searchtoko.getSelectedItem() == null || searchtoko.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("true")
						: Restrictions.eq("toko", searchtoko.getSelectedItem().getValue()));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Ukuran halaman KHUSUS layar Produk (bukan {@link Common#ROWS_COUNT_ON_PAGE} global) -- gap-closure permintaan "20 data per halaman" utk katalog produk, tanpa mengubah pagination default layar ZK lain. */
	private static final int UKURAN_HALAMAN_PRODUK = 20;

	public void onSearchDefault(Event event) {
		Common.initPagingCustom(initCriteria(false), paging, UKURAN_HALAMAN_PRODUK);

		List<Produk> produk = initCriteria(true).setMaxResults(UKURAN_HALAMAN_PRODUK)
				.setFirstResult(UKURAN_HALAMAN_PRODUK * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(produk);
		grid.setRowRenderer(new ProdukRenderer());
		grid.setModelCheckMobile(strset);

	}

	/** Stok bersih barang persediaan di modul Aset = Σ((qty+qtybonus) × tanda jenis transaksi). */
	private double hitungStokAset(Long masterAssetId) {
		if (masterAssetId == null) {
			return 0;
		}
		try {
			Object o = HibernateUtil.currentSession().createSQLQuery(
					"SELECT COALESCE(SUM((a.qty+a.qtybonus)*b.jenis),0) FROM asset.detail_transaksi_asset a "
							+ "INNER JOIN library.kode_transaksi b ON a.kode_transaksi = b.id WHERE a.master_asset = "
							+ masterAssetId).uniqueResult();
			return o == null ? 0 : ((Number) o).doubleValue();
		} catch (Exception e) {
			return 0;
		}
	}

	public Boolean checkNamaProduk() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Produk.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("toko", toko.getSelectedItem().getValue()))
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.produk.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.produk.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}


	private void tampilkanIntroDashboardInventoryV1(Component parent, String judul, String deskripsi) {
		if (parent == null) {
			return;
		}
		try {
			org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 10px 0;padding:14px 16px;"
					+ "border-radius:16px;background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:#ffffff;"
					+ "box-shadow:0 12px 24px rgba(15,23,42,.16);\">"
					+ "<div style=\"font-size:17px;font-weight:900;line-height:1.25;\">" + escapeDashboardHtmlInventoryV1(judul) + "</div>"
					+ "<div style=\"font-size:12px;line-height:1.65;margin-top:6px;opacity:.93;\">" + escapeDashboardHtmlInventoryV1(deskripsi) + "</div>"
					+ "<div style=\"display:flex;gap:8px;flex-wrap:wrap;margin-top:10px;\">"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#dbeafe;color:#1e40af;font-size:10.5px;font-weight:900;\">HTML/CSS modern</span>"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#dcfce7;color:#166534;font-size:10.5px;font-weight:900;\">Data operasional POS</span>"
					+ "<span style=\"display:inline-block;padding:5px 9px;border-radius:999px;background:#fef3c7;color:#92400e;font-size:10.5px;font-weight:900;\">Mudah dipahami end user</span>"
					+ "</div></div>");
			if (parent.getChildren() != null && parent.getChildren().size() > 0) {
				parent.insertBefore(html, (org.zkoss.zk.ui.Component) parent.getChildren().get(0));
			} else {
				parent.appendChild(html);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/ProdukAction.java:625");
			/* informasi intro tidak boleh menggagalkan halaman utama */
		}
	}

	private String escapeDashboardHtmlInventoryV1(String value) {
		if (value == null) {
			return "";
		}
		String s = value;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&#39;");
		return s;
	}

}
