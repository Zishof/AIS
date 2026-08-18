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
import org.zkoss.zul.Columns;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
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
import ais.action.master.inventory.helper.PedagangAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * TokoAction TIDAK extends GenericCrudAction (komposer manual, mendahului
 * refactor generic CRUD) -- download/upload di sini dipasang langsung lewat
 * {@link Common#appendDownloadUploadButtons} (mekanisme sama yang dipakai
 * GenericCrudAction.getDownloadUploadContents(), lihat contoh AgamaAction),
 * bukan lewat override, karena kelas ini bukan turunan base class tsb.
 */
public class TokoAction extends GenericAutowireComposer implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;

	private Textbox kode;
	private Textbox nama;
	private org.zkoss.zul.Combobox gudangPemasok;

	private Textbox keterangan;
	private MyCheckboxConfig bolehTransaksiStokHabis;
	private MyCheckboxConfig tokoDemo;
	/** kode unit usaha -> checkbox; diisi ulang tiap init() (lihat UnitUsahaKatalog). */
	private java.util.Map<String, MyCheckboxConfig> unitUsahaCek;

	private boolean edit = false;
	private boolean delete = false;

	private Toko toko;
	private MyToolbarbuttonConfig add;

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

		tampilkanIntroDashboardInventoryV1(comp, "Dashboard Toko dan Merchant", "Mengelola daftar toko atau merchant koperasi beserta pedagang yang bertugas di dalamnya. Tampilan ini membantu admin memastikan setiap transaksi POS masuk ke toko yang benar dan hak akses pedagang tidak tertukar.");
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		// Download/upload Excel (pola sama AgamaAction, lihat javadoc kelas ini).
		if (add != null) {
			Common.appendDownloadUploadButtons(add, Toko.class, this, this,
					add.isVisible() && edit && delete,
					"id", "kode", "nama", "keterangan", "aktif",
					"bolehMelihatTokolain", "bolehTransaksiStokHabis", "tokoDemo");
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

	}

	class TokoRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Toko toko = (Toko) arg1;

			new PedagangAction(toko).setParent(arg0);

			new Label(toko.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(Toko.class, toko, toko.getNama()).setParent(arg0);

			new Label(toko.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(toko.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					toko.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(toko);
				}
			});

			final MyCheckboxConfig bolehMelihatTokolain = new MyCheckboxConfig("Antar toko");
			bolehMelihatTokolain.setDisabled(!edit);
			bolehMelihatTokolain.setChecked(toko.getBolehMelihatTokolain());
			bolehMelihatTokolain.setParent(arg0);
			bolehMelihatTokolain.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					toko.setBolehMelihatTokolain(bolehMelihatTokolain.isChecked());
					Common.refreshSaveOrUpdate(toko);
				}
			});

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(toko);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(toko);

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
			toolbar.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new Toko());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(Toko toko) throws Exception {

		this.toko = toko;
		addWindow.setTitle(toko.getId() == null ? "Tambah Toko" : "Ubah Toko");
		Common.clear(addWindow);
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
		column.setWidth("35%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Toko"));
		row.appendChild(kode = new Textbox(toko.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Toko"));
		row.appendChild(nama = new Textbox(toko.getNama() == null ? "" : toko.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(toko.getKeterangan() == null ? "" : toko.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Transaksi saat stok habis"));
		bolehTransaksiStokHabis = new MyCheckboxConfig("Paksa semua produk boleh stok minus");
		bolehTransaksiStokHabis.setChecked(Boolean.TRUE.equals(toko.getBolehTransaksiStokHabis()));
		bolehTransaksiStokHabis.setTooltiptext("OFF: ikuti izin stok minus pada masing-masing produk. ON: seluruh produk toko ini boleh dijual saat stok nol/minus.");
		row.appendChild(bolehTransaksiStokHabis);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Toko Demo / UAT"));
		tokoDemo = new MyCheckboxConfig("Izinkan generator data contoh bervolume besar");
		tokoDemo.setChecked(Boolean.TRUE.equals(toko.getTokoDemo()));
		tokoDemo.setDisabled(!Common.getApakahAdminLain());
		tokoDemo.setTooltiptext("Default OFF. Hanya administrator dapat mengaktifkan toko khusus demo/UAT.");
		row.appendChild(tokoDemo);

		// Unit usaha toko (boleh lebih dari satu) -- katalog terpusat UnitUsahaKatalog;
		// dipakai generator data contoh produk utk memilih katalog sesuai jenis usahanya.
		java.util.Set<String> unitTerpilih = ais.common.UnitUsahaKatalog.urai(toko.getUnitUsahaJson());
		unitUsahaCek = new java.util.LinkedHashMap<String, MyCheckboxConfig>();
		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Usaha (boleh lebih dari satu)"));
		org.zkoss.zul.Vbox unitWadah = new org.zkoss.zul.Vbox();
		String grupTerakhir = null;
		for (ais.common.UnitUsahaKatalog.Entri entri : ais.common.UnitUsahaKatalog.DAFTAR) {
			if (!entri.grup.equals(grupTerakhir)) {
				grupTerakhir = entri.grup;
				org.zkoss.zul.Label judulGrup = new org.zkoss.zul.Label(entri.grup);
				judulGrup.setStyle("font-weight:bold;margin-top:6px;display:block;");
				unitWadah.appendChild(judulGrup);
			}
			MyCheckboxConfig cek = new MyCheckboxConfig(entri.label);
			cek.setChecked(unitTerpilih.contains(entri.kode));
			unitWadah.appendChild(cek);
			unitUsahaCek.put(entri.kode, cek);
		}
		row.appendChild(unitWadah);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gudang Pemasok"));
		gudangPemasok = new org.zkoss.zul.Combobox();
		// Gudang cabang yang bertanggung jawab memasok toko ini -- dipakai StokThresholdScheduler utk
		// menentukan tujuan pengajuan pembelian otomatis saat stok raw material di gudang menipis
		// (lihat javadoc Toko.getGudangPemasok()). Opsional -- toko yg belum ditentukan gudang
		// pemasoknya tidak ikut dicek scheduler tsb.
		Common.insertComboDanSemua(gudangPemasok, new String[] { "nama" }, "kode",
				ais.database.model.sirs.Gudang.class, "== Belum Ditentukan ==",
				Restrictions.eq("aktif", true));
		Common.selectComboItem(true, gudangPemasok, toko.getGudangPemasok());
		row.appendChild(gudangPemasok);
		gudangPemasok.setWidth("90%");
		gudangPemasok.setReadonly(true);

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

	public boolean onSave(Event event) throws Exception {

		if (kode.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Kode Toko belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Kode Toko dengan kode yang unik; (2) pastikan kode tidak kosong atau hanya spasi; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Toko belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Toko; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		// if (satuanKerja.getAttribute("satuanKerja") == null) {
		// MyMessageboxConfig.show("Satuan Kerja harus diisi", "Peringatan",
		// MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return false;
		// }

		boolean i = checkKodeToko();
		if (i) {
			MyMessageboxConfig.show("Kode Toko sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNamaToko();
		if (i) {
			MyMessageboxConfig.show("Nama Toko sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (toko.getId() != null) {
			toko = (Toko) session.load(Toko.class, toko.getId());

		}
		// toko.setMaxPinjam(maxPinjam.getValue());

		toko.setKode(kode.getValue());
		toko.setNama(nama.getValue());
		toko.setKeterangan(keterangan.getValue());
		toko.setBolehTransaksiStokHabis(bolehTransaksiStokHabis.isChecked());
		if (Common.getApakahAdminLain()) toko.setTokoDemo(tokoDemo.isChecked());
		if (unitUsahaCek != null) {
			java.util.Set<String> terpilih = new java.util.LinkedHashSet<String>();
			for (java.util.Map.Entry<String, MyCheckboxConfig> e : unitUsahaCek.entrySet()) {
				if (e.getValue().isChecked()) terpilih.add(e.getKey());
			}
			toko.setUnitUsahaJson(ais.common.UnitUsahaKatalog.keJson(terpilih));
		}
		toko.setGudangPemasok(gudangPemasok.getSelectedIndex() > 0
				&& gudangPemasok.getSelectedItem().getValue() instanceof ais.database.model.sirs.Gudang
						? (ais.database.model.sirs.Gudang) gudangPemasok.getSelectedItem().getValue()
						: null);

		Common.refreshUpdate(session, toko);

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Toko.class);
		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));

		// .add(satuanKerjas.size() == 0 ? Restrictions
		// .sqlRestriction("1=1") : Restrictions.in("satuanKerja",
		// satuanKerjas));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<Toko> toko = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(toko);
		grid.setRowRenderer(new TokoRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeToko() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Toko.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim())).add(this.toko.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.toko.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaToko() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(Toko.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.toko.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.toko.getId()))
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/TokoAction.java:413");
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
