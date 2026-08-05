package ais.action.master;

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
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.JenisPeredaranBuku;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class JenisPeredaranBukuAction extends GenericAutowireComposer
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
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private JenisPeredaranBuku jenisPeredaranBuku;
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
		
		Session session = HibernateUtil.currentSession();
		int count = ((Number) session.createCriteria(JenisPeredaranBuku.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();
		if (count == 0) { 

			JenisPeredaranBuku jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Hanya berupa modul pengajaran / bukan buku");
			jenisPeredaranBuku.setKeterangan("Hanya berupa modul pengajaran / bukan buku");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan buku yang beredar");
			jenisPeredaranBuku.setKeterangan("Merupakan buku yang beredar");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan buku internasional (berbahasa Internasional yang diakui oleh PBB dan diedarkan secara internasional)");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan buku internasional (berbahasa Internasional yang diakui oleh PBB dan diedarkan secara internasional)");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama(
					"Merupakan terjemahan / saduran buku ilmiah yang diterbitkan dan diedarkan secara nasional");
			jenisPeredaranBuku.setKeterangan(
					"Merupakan terjemahan / saduran buku ilmiah yang diterbitkan dan diedarkan secara nasional");
			session.save(jenisPeredaranBuku);

			jenisPeredaranBuku = new JenisPeredaranBuku();
			jenisPeredaranBuku.setNama("Merupakan editan / suntingan buku ilmiah yang diterbitkan dan diedarkan secara nasional.");
			jenisPeredaranBuku.setKeterangan("Merupakan editan / suntingan buku ilmiah yang diterbitkan dan diedarkan secara nasional.");
			session.save(jenisPeredaranBuku);

		}

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

		String[] contents = new String[] { "id", "nama", "keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData( JenisPeredaranBuku.class,this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, JenisPeredaranBuku.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class JenisPeredaranBukuRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisPeredaranBuku jenisPeredaranBuku = (JenisPeredaranBuku) arg1;

			RevisiHelper.createNewRevisi(JenisPeredaranBuku.class, jenisPeredaranBuku,
					jenisPeredaranBuku.getNama()).setParent(arg0);

			new Label(jenisPeredaranBuku.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, jenisPeredaranBuku, JenisPeredaranBukuAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisPeredaranBuku());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		jenisPeredaranBuku = (JenisPeredaranBuku) obj;
		init(jenisPeredaranBuku);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(JenisPeredaranBuku jenisPeredaranBuku) {
		this.jenisPeredaranBuku = jenisPeredaranBuku;
		addWindow.setTitle(jenisPeredaranBuku.getId() == null ? "Tambah Jenis Peredaran Buku" : "Ubah Jenis Peredaran Buku");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Tahapan"));
		row.appendChild(nama = new Textbox(jenisPeredaranBuku.getNama()));
		nama.setWidth("90%");

		

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(jenisPeredaranBuku.getKeterangan()));
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
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Peredaran Buku",
					"Kolom Nama Jenis Peredaran Buku belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Jenis Peredaran Buku.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaJenisPeredaranBuku();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Peredaran Buku",
					"Nama Jenis Peredaran Buku sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama jenis peredaran buku yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisPeredaranBuku.getId() != null) {
			jenisPeredaranBuku = (JenisPeredaranBuku) session.load(JenisPeredaranBuku.class,
					jenisPeredaranBuku.getId());

		}

		jenisPeredaranBuku.setNama(nama.getValue());
		jenisPeredaranBuku.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, jenisPeredaranBuku);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisPeredaranBuku.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<JenisPeredaranBuku> jenisPeredaranBuku = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisPeredaranBuku);
		grid.setRowRenderer(new JenisPeredaranBukuRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaJenisPeredaranBuku() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisPeredaranBuku.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisPeredaranBuku.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisPeredaranBuku.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
