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
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.inventory.JenisProduk;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisProdukAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox nama;
	private MyDoublebox maksimalHarian;
	private Textbox keterangan;
	private ais.action.master.akunting.helper.AmbilDataAkunBanbox akunPendapatan;
	private ais.action.master.akunting.helper.AmbilDataAkunBanbox akunPpnKeluaran;
	private ais.action.master.akunting.helper.AmbilDataAkunBanbox akunHpp;

	private boolean edit = false;
	private boolean delete = false;

	private JenisProduk jenisProduk;
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

		tampilkanIntroDashboardInventoryV1(comp, "Dashboard Jenis Produk", "Mengelompokkan produk agar kasir dan pengelola toko lebih mudah melakukan pencarian, pelaporan, dan pembatasan aturan penjualan harian. Data kategori yang rapi membantu produk tidak tercampur dan memudahkan evaluasi penjualan.");

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

		String[] contents = new String[] { "id", "nama", "maksimalHarian", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisProduk.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisProdukRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisProduk jenisProduk = (JenisProduk) arg1;

			RevisiHelper.createNewRevisi(JenisProduk.class, jenisProduk, jenisProduk.getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisProduk.getMaksimalHarian())).setParent(arg0);
			new Label(jenisProduk.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisProduk, JenisProdukAction.this).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisProduk());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisProduk = (JenisProduk) obj;
		init(jenisProduk);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisProduk jenisProduk) {
		this.jenisProduk = jenisProduk;
		addWindow.setTitle(jenisProduk.getId() == null ? "Tambah Jenis Produk" : "Ubah Jenis Produk");
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jenis Produk"));
		row.appendChild(nama = new Textbox(jenisProduk.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Belanja maksimal Harian"));
		row.appendChild(maksimalHarian = new MyDoublebox(jenisProduk.getMaksimalHarian()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisProduk.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		// Akun untuk Posting Penjualan Kantin (sisi KREDIT) per jenis produk.
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Pendapatan Penjualan"));
		row.appendChild(akunPendapatan = new ais.action.master.akunting.helper.AmbilDataAkunBanbox(false));
		akunPendapatan.setWidth("90%");
		if (jenisProduk.getAkunPendapatan() != null) {
			akunPendapatan.setAttribute("akun", jenisProduk.getAkunPendapatan());
			akunPendapatan.setValue(jenisProduk.getAkunPendapatan().toString());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun PPN Keluaran"));
		row.appendChild(akunPpnKeluaran = new ais.action.master.akunting.helper.AmbilDataAkunBanbox(false));
		akunPpnKeluaran.setWidth("90%");
		if (jenisProduk.getAkunPpnKeluaran() != null) {
			akunPpnKeluaran.setAttribute("akun", jenisProduk.getAkunPpnKeluaran());
			akunPpnKeluaran.setValue(jenisProduk.getAkunPpnKeluaran().toString());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun HPP (Beban Pokok Penjualan)"));
		row.appendChild(akunHpp = new ais.action.master.akunting.helper.AmbilDataAkunBanbox(false));
		akunHpp.setWidth("90%");
		if (jenisProduk.getAkunHpp() != null) {
			akunHpp.setAttribute("akun", jenisProduk.getAkunHpp());
			akunHpp.setValue(jenisProduk.getAkunHpp().toString());
		}

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
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nama Jenis Produk belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Jenis Produk; (2) pastikan nama tidak kosong atau hanya spasi; (3) ulangi kembali proses penyimpanan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkNamaJenisProduk();
		if (i) {
			MyMessageboxConfig.show("Nama Jenis Produk sudah ada di database", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisProduk.getId() != null) {
			jenisProduk = (JenisProduk) session.load(JenisProduk.class, jenisProduk.getId());

		}

		jenisProduk.setNama(nama.getValue());
		jenisProduk.setMaksimalHarian(maksimalHarian.getValue());
		jenisProduk.setKeterangan(keterangan.getValue());
		jenisProduk.setAkunPendapatan(
				(ais.database.model.akunting.Akun) (akunPendapatan == null ? null : akunPendapatan.getAttribute("akun")));
		jenisProduk.setAkunPpnKeluaran(
				(ais.database.model.akunting.Akun) (akunPpnKeluaran == null ? null : akunPpnKeluaran.getAttribute("akun")));
		jenisProduk.setAkunHpp(
				(ais.database.model.akunting.Akun) (akunHpp == null ? null : akunHpp.getAttribute("akun")));

		Common.refreshSaveOrUpdate(session, jenisProduk);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisProduk.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisProduk> jenisProduk = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisProduk);
		grid.setRowRenderer(new JenisProdukRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisProduk() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisProduk.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.jenisProduk.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.jenisProduk.getId()))
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
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/inventory/JenisProdukAction.java:301");
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
