package ais.action.master.payroll;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
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

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.asset.util.AssetUtil;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.payroll.ItemGaji;
import ais.database.model.payroll.KelompokItemGaji;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class KelompokItemGajiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private AmbilDataAkunBanbox searchakun;

	private Textbox nama;
	private Combobox induk;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokItemGaji kelompokItemGaji;
	private MyToolbarbuttonConfig add;
	private JSONArray akun;
	private JSONArray akunDebet;
	private Textbox kode;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}
		searchakun.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
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

		String[] contents = new String[] { "id", "kode", "nama", "induk", "akun", "akunDebet", "keterangan", };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokItemGaji.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class KelompokItemGajiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			KelompokItemGaji kelompokItemGaji = (KelompokItemGaji) arg1;
			new Label(kelompokItemGaji.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(KelompokItemGaji.class, kelompokItemGaji, kelompokItemGaji.getNama())
					.setParent(arg0);

			new Label(kelompokItemGaji.getInduk() == null ? "" : kelompokItemGaji.getInduk().getNama()).setParent(arg0);
			new Label(kelompokItemGaji.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokItemGaji, new DataInitDefault() {
				
				@Override
				public void onSearchDefault(Event event) {
					Common.createDefaultTimer(new EventListener() {
						
						@Override
						public void onEvent(Event arg0) throws Exception {
							ItemGaji.reloadKelompokItemGaji();
						}
					});
					
					KelompokItemGajiAction.this.onSearchDefault(event); 
				}
				
				@Override
				public void init(GeneralValueObject obj) throws Exception {
					KelompokItemGajiAction.this.init(obj); 
				}
			}).setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokItemGaji());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokItemGaji kelompokItemGaji) throws Exception {
		this.kelompokItemGaji = kelompokItemGaji;
		addWindow.setTitle(kelompokItemGaji.getId() == null ? "Tambah Kelompok Item Gaji" : "Ubah Kelompok Item Gaji");
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

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Kelompok Item Gaji *"));
		row.appendChild(kode = new Textbox(kelompokItemGaji.getKode() == null ? "" : kelompokItemGaji.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok Item Gaji *"));
		row.appendChild(nama = new Textbox(kelompokItemGaji.getNama() == null ? "" : kelompokItemGaji.getNama()));
		nama.setWidth("90%");

		MyFormRow rowInduk = new MyFormRow();
		rowInduk.setParent(rows);
		rowInduk.appendChild(new ais.ui.util.MyLabelConfig("Kelompok ini meng-induk pada"));
		rowInduk.appendChild(induk = new Combobox());
		Common.insertComboDanSemua(induk, new String[] { "nomorUrut", "nama" }, "keterangan", KelompokItemGaji.class,
				"== Merupakan Induk Utama ==");
		Common.selectComboItem(true, induk, kelompokItemGaji.getInduk());
		induk.setWidth("90%");
		induk.setReadonly(true);

		final MyFormRow row1 = new MyFormRow();
		row1.setParent(rows);
		row1.appendChild(new ais.ui.util.MyLabelConfig("Akun Kredit"));
		akun = new JSONArray(kelompokItemGaji.getAkun());
		Row rowFormula = Common.tampilanScroll1(row1);
		AssetUtil.reloadFormula(rowFormula, akun, edit);

		final MyFormRow row2 = new MyFormRow();
		row2.setParent(rows);
		row2.appendChild(new ais.ui.util.MyLabelConfig("Akun Debet"));
		akunDebet = new JSONArray(kelompokItemGaji.getAkunDebet());
		rowFormula = Common.tampilanScroll1(row2);
		AssetUtil.reloadFormula(rowFormula, akunDebet, edit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				kelompokItemGaji.getKeterangan() == null ? "" : kelompokItemGaji.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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
			MyMessageboxConfig.show("Mohon maaf, kolom Kode Kelompok belum diisi. Langkah yang dapat dilakukan: (1) isikan Kode Kelompok pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, kolom Nama Kelompok belum diisi. Langkah yang dapat dilakukan: (1) isikan Nama Kelompok pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini. Jika masih mengalami kendala, hubungi Administrator.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		boolean i = checkKodeKelompokItemGaji();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Kode Kelompok yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Kode Kelompok yang berbeda; (2) periksa kembali daftar kelompok item gaji yang telah ada; (3) simpan kembali data ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		i = checkNamaKelompokItemGaji();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, Nama Kelompok yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Nama Kelompok yang berbeda; (2) periksa kembali daftar kelompok item gaji yang telah ada; (3) simpan kembali data ini.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokItemGaji.getId() != null) {
			kelompokItemGaji = (KelompokItemGaji) session.load(KelompokItemGaji.class, kelompokItemGaji.getId());

		}

		kelompokItemGaji.setKode(kode.getValue().trim());
		kelompokItemGaji.setNama(nama.getValue().trim());
		kelompokItemGaji.setKeterangan(keterangan.getValue());
		kelompokItemGaji.setInduk(
				(KelompokItemGaji) (induk.getSelectedItem() == null ? null : induk.getSelectedItem().getValue()));

		kelompokItemGaji.setAkun(akun.toString());
		kelompokItemGaji.setAkunDebet(akunDebet.toString());

		Common.refreshSaveOrUpdate(session, kelompokItemGaji);
		
		
		Common.createDefaultTimer(new EventListener() {
			
			@Override
			public void onEvent(Event arg0) throws Exception {
				ItemGaji.reloadKelompokItemGaji();
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {

		Akun akun = (Akun) searchakun.getAttribute("akun");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KelompokItemGaji.class);

		if (akun != null && akun.getId() != null) {
			criteria.add(Restrictions.or(Restrictions.ilike("akun", ":" + akun.getId() + "}", MatchMode.ANYWHERE),
					Restrictions.ilike("akunDebet", ":" + akun.getId() + "}", MatchMode.ANYWHERE))

			);
		}

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KelompokItemGaji> kelompokItemGaji = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokItemGaji);
		grid.setRowRenderer(new KelompokItemGajiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaKelompokItemGaji() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokItemGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.kelompokItemGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokItemGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkKodeKelompokItemGaji() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(KelompokItemGaji.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim()))
				.add(this.kelompokItemGaji.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.kelompokItemGaji.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokItemGaji = (KelompokItemGaji) obj;
		init(kelompokItemGaji);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

}
