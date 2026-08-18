package ais.action.master.employ;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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

import ais.action.master.akunting.helper.AmbilDataPegawaiBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.dao.DaoFactory;
import ais.database.dao.employ.MutasiPindahDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.employ.JabatanStruktural;
import ais.database.model.employ.MutasiPindah;
import ais.database.model.employ.UnitKerja;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class MutasiAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private AmbilDataPegawaiBanbox ambilDataPegawaiBanbox = new AmbilDataPegawaiBanbox();
	private AmbilDataPegawaiBanbox searchpegawai;
	private Combobox jabatanStrukturalAwal;
	private Combobox jabatanStrukturalAkhir;
	private Combobox searchstatus;

	private Combobox unitKerjaAwal;
	private Combobox unitKerjaTujuan;
	private boolean edit = false;
	private boolean delete = false;
	private Pegawai pegawai;

	private MutasiPindah mutasiPindah;
	private MyToolbarbuttonConfig add;
	private MyDatebox tanggalSuratUsul;
	private Textbox noSuratUsul;
	private MyDatebox tmt;

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

		searchpegawai.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.insertCombo(unitKerjaAwal = new Combobox(), "nama", UnitKerja.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(unitKerjaTujuan = new Combobox(), "nama", UnitKerja.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(jabatanStrukturalAwal = new Combobox(), "nama", JabatanStruktural.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.insertCombo(jabatanStrukturalAkhir = new Combobox(), "nama", JabatanStruktural.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (session.getAttribute("pegawai") == null) {
			pegawai = (Pegawai) session.getAttribute("pegawai");
		}

		if (this.pegawai != null) {
			searchpegawai.setAttribute("pegawai", pegawai);
			searchpegawai.setValue(pegawai.toString());
			searchpegawai.setDisabled(true);
		}

		MyComboitemConfig comboitem = new MyComboitemConfig("Disetujui");
		if (comboitem != null) { comboitem.setValue(true); }
		searchstatus.appendChild(comboitem);
		comboitem = new MyComboitemConfig("Belum Disetujui");
		if (comboitem != null) { comboitem.setValue(false); }
		searchstatus.appendChild(comboitem);
		searchstatus.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		mutasiPindah = (MutasiPindah) obj;
		init(mutasiPindah);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	class MutasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final MutasiPindah mutasiPindah = (MutasiPindah) arg1;

			RevisiHelper
					.createNewRevisi(MutasiPindah.class, mutasiPindah, mutasiPindah.getPegawai().getNama().toString())
					.setParent(arg0);
			new Label(mutasiPindah.getUnitKerjaAwal() == null ? "" : mutasiPindah.getUnitKerjaAwal().getNama())
					.setParent(arg0);
			new Label(mutasiPindah.getJabatanStrukturalAwal() == null ? ""
					: mutasiPindah.getJabatanStrukturalAwal().getNama()).setParent(arg0);

			new Label(mutasiPindah.getUnitKerjaTujuan() == null ? "" : mutasiPindah.getUnitKerjaTujuan().getNama())
					.setParent(arg0);

			new Label(mutasiPindah.getJabatanStrukturalAkhir() == null ? ""
					: mutasiPindah.getJabatanStrukturalAkhir().getNama()).setParent(arg0);
			new Label(mutasiPindah.getTanggalSuratUsul() == null ? ""
					: Common.dateFormat2.get().format(mutasiPindah.getTanggalSuratUsul())).setParent(arg0);
			new Label(mutasiPindah.getTmt() == null ? "" : Common.dateFormat2.get().format(mutasiPindah.getTmt()))
					.setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, mutasiPindah, MutasiAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new MutasiPindah());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(MutasiPindah mutasiPindah) {
		this.mutasiPindah = mutasiPindah;
		addWindow.setTitle("Mutasi");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Pegawai *"));
		row.appendChild(ambilDataPegawaiBanbox);
		ambilDataPegawaiBanbox.setValue(mutasiPindah.getPegawai() == null ? ""
				: mutasiPindah.getPegawai().getCode() + " - " + mutasiPindah.getPegawai().getNama());
		ambilDataPegawaiBanbox.setAttribute("pegawai", mutasiPindah.getPegawai());
		ambilDataPegawaiBanbox.setWidth("90%");

		if (this.pegawai != null) {
			ambilDataPegawaiBanbox.setAttribute("pegawai", pegawai);
			ambilDataPegawaiBanbox.setValue(pegawai.toString());
			ambilDataPegawaiBanbox.setDisabled(!Common.getApakahAdmin());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Surat Usul"));
		row.appendChild(tanggalSuratUsul = new MyDatebox(
				mutasiPindah.getTanggalSuratUsul() == null ? ais.ui.util.WaktuUtil.getDate()
						: mutasiPindah.getTanggalSuratUsul()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Surat Usul"));
		row.appendChild(
				noSuratUsul = new Textbox(mutasiPindah.getNoSuratUsul() == null ? "" : mutasiPindah.getNoSuratUsul()));
		noSuratUsul.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Kerja Awal"));
		row.appendChild(unitKerjaAwal);
		Common.selectComboItem(unitKerjaAwal,
				mutasiPindah.getUnitKerjaAwal() == null ? null : mutasiPindah.getUnitKerjaAwal());
		unitKerjaAwal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jabatan Awal"));
		row.appendChild(jabatanStrukturalAwal);
		Common.selectComboItem(jabatanStrukturalAwal,
				mutasiPindah.getJabatanStrukturalAwal() == null ? null : mutasiPindah.getJabatanStrukturalAwal());
		jabatanStrukturalAwal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit Kerja Tujuan"));
		row.appendChild(unitKerjaTujuan);
		Common.selectComboItem(unitKerjaTujuan,
				mutasiPindah.getUnitKerjaTujuan() == null ? null : mutasiPindah.getUnitKerjaTujuan());
		unitKerjaTujuan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mutasi ke Jabatan"));
		row.appendChild(jabatanStrukturalAkhir);
		Common.selectComboItem(jabatanStrukturalAkhir,
				mutasiPindah.getJabatanStrukturalAkhir() == null ? null : mutasiPindah.getJabatanStrukturalAkhir());
		jabatanStrukturalAkhir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("TMT"));
		row.appendChild(tmt = new MyDatebox(
				mutasiPindah.getTmt() == null ? ais.ui.util.WaktuUtil.getDate() : mutasiPindah.getTmt()));

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
		if (ambilDataPegawaiBanbox.getAttribute("pegawai") == null) {
			MyMessageboxConfig.show("Mohon maaf, Pegawai belum dipilih. Langkah yang dapat dilakukan: (1) cari dan pilih Pegawai menggunakan kolom pencarian; (2) pastikan data pegawai sudah terdaftar di sistem; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		MutasiPindahDao mutasiPindahDao = DaoFactory.getInstance().getMutasiPindahDao();
		if (mutasiPindah.getId() != null) {
			mutasiPindah = mutasiPindahDao.load(mutasiPindah.getId());

		}

		mutasiPindah.setPegawai((Pegawai) ambilDataPegawaiBanbox.getAttribute("pegawai"));
		mutasiPindah.setNoSuratUsul(noSuratUsul.getValue());
		mutasiPindah.setTanggalSuratUsul(tanggalSuratUsul.getValue());
		mutasiPindah.setTmt(tmt.getValue());
		mutasiPindah.setUnitKerjaAwal((UnitKerja) (unitKerjaAwal.getSelectedItem() == null ? null
				: unitKerjaAwal.getSelectedItem().getValue()));
		mutasiPindah.setUnitKerjaTujuan((UnitKerja) (unitKerjaTujuan.getSelectedItem() == null ? null
				: unitKerjaTujuan.getSelectedItem().getValue()));
		mutasiPindah
				.setJabatanStrukturalAwal((JabatanStruktural) (jabatanStrukturalAwal.getSelectedItem() == null ? null
						: jabatanStrukturalAwal.getSelectedItem().getValue()));
		mutasiPindah
				.setJabatanStrukturalAkhir((JabatanStruktural) (jabatanStrukturalAkhir.getSelectedItem() == null ? null
						: jabatanStrukturalAkhir.getSelectedItem().getValue()));

		if (mutasiPindah.getId() != null) {
			mutasiPindahDao.update(mutasiPindah);
		} else {
			mutasiPindahDao.save(mutasiPindah);
		}

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(MutasiPindah.class);
		if (order)
			criteria.addOrder(Order.asc("pegawai"));

		criteria.add((searchpegawai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchpegawai.getAttribute("pegawai") == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("pegawai", searchpegawai.getAttribute("pegawai"))))
				.add(searchstatus.getSelectedItem() == null || searchstatus.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("status", searchstatus.getSelectedItem().getValue()));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<MutasiPindah> mutasiPindah = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(mutasiPindah);
		grid.setRowRenderer(new MutasiRenderer());
		grid.setModelCheckMobile(strset);

	}

}
