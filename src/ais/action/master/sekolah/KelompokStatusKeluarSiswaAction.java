package ais.action.master.sekolah;

import java.util.ArrayList;
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
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.sekolah.helper.KelompokStatusKeluarSiswaDetailAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;
import ais.database.model.sekolah.KelompokStatusKeluarSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.StatusKeluarSiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * <h3>Manajemen Kelompok Status Keluar Siswa</h3>
 *
 * <p>Padanan {@code KelompokStatusKeluarMahasiswaAction} untuk Siswa. Halaman ini menampilkan
 * daftar kelompok status keluar (Nama, Status Keluar, Tahun Akademik, Semester, Tanggal
 * Lulus/Keluar, Keterangan) dengan operasi CRUD standar. Setiap baris memiliki panel detail
 * ({@link KelompokStatusKeluarSiswaDetailAction}) untuk mengelola daftar siswa anggota kelompok.
 * Logika dibuat sama dengan versi Mahasiswa, namun memakai tabel/entitas Siswa
 * ({@link KelompokStatusKeluarSiswa}, {@link StatusKeluarSiswa}, {@link Siswa}).</p>
 */
public class KelompokStatusKeluarSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	private static final long serialVersionUID = -5779730267402400329L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchSiswa;

	private Textbox nama;
	private Combobox statusKeluar;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private KelompokStatusKeluarSiswa kelompokStatusKeluarSiswa;
	private MyToolbarbuttonConfig add;
	private MyDatebox tanggalLulus;
	private Combobox tahunAkademik;
	private Combobox semester;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

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

		String[] contents = new String[] { "id", "nama", "statusKeluar", "tahunAkademik", "semester", "tanggalLulus",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KelompokStatusKeluarSiswa.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KelompokStatusKeluarSiswa.class, contents);
		if (upload != null) {
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
		}
		Common.appendKeToolbar(upload, add, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link KelompokStatusKeluarSiswaAction}. Kelas ini menerjemahkan satu
	 * item data menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KelompokStatusKeluarSiswaAction} dan dapat
	 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see KelompokStatusKeluarSiswaAction
	 */
	class KelompokStatusKeluarSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			KelompokStatusKeluarSiswa kelompokStatusKeluarSiswa = (KelompokStatusKeluarSiswa) arg1;

			(new KelompokStatusKeluarSiswaDetailAction(kelompokStatusKeluarSiswa)).setParent(arg0);

			new Label(kelompokStatusKeluarSiswa.getNama()).setParent(arg0);
			new Label(kelompokStatusKeluarSiswa.getStatusKeluar() == null ? ""
					: kelompokStatusKeluarSiswa.getStatusKeluar().getNama()).setParent(arg0);

			new Label(kelompokStatusKeluarSiswa.getTahunAkademik()).setParent(arg0);
			new Label(kelompokStatusKeluarSiswa.getSemester()).setParent(arg0);

			new Label(kelompokStatusKeluarSiswa.getTanggalLulus() == null ? "Tidak Ditentukan"
					: Common.dateFormat4.get().format(kelompokStatusKeluarSiswa.getTanggalLulus())).setParent(arg0);

			new Label(kelompokStatusKeluarSiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, kelompokStatusKeluarSiswa, KelompokStatusKeluarSiswaAction.this)
					.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new KelompokStatusKeluarSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kelompokStatusKeluarSiswa = (KelompokStatusKeluarSiswa) obj;
		init(kelompokStatusKeluarSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(KelompokStatusKeluarSiswa kelompokStatusKeluarSiswa) {
		this.kelompokStatusKeluarSiswa = kelompokStatusKeluarSiswa;
		addWindow.setTitle(
				kelompokStatusKeluarSiswa.getId() == null ? "Tambah Status Keluar Siswa" : "Ubah Status Keluar Siswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Kelompok *"));
		row.appendChild(nama = new Textbox(kelompokStatusKeluarSiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Keluar *"));
		statusKeluar = new Combobox();
		Common.insertCombo(statusKeluar, "nama", StatusKeluarSiswa.class);
		Common.selectComboItem(statusKeluar, kelompokStatusKeluarSiswa.getStatusKeluar());
		row.appendChild(statusKeluar);
		statusKeluar.setWidth("90%");
		statusKeluar.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik *"));
		tahunAkademik = new Combobox();
		Common.generateTahunAjaran(tahunAkademik);
		Common.selectComboItem(tahunAkademik, kelompokStatusKeluarSiswa.getTahunAkademik());
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester *"));
		semester = new Combobox();
		Comboitem comboitem = new Comboitem(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);
		comboitem = new Comboitem(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		Common.selectComboItem(semester, kelompokStatusKeluarSiswa.getSemester());
		row.appendChild(semester);
		semester.setWidth("90%");
		semester.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Status Keluar"));
		row.appendChild(tanggalLulus = new MyDatebox(kelompokStatusKeluarSiswa.getTanggalLulus()));
		tanggalLulus.setDisabled(false);
		tanggalLulus.setReadonly(false);

		Common.initKeterangan(rows,
				"Kosongkan tanggal lulus jika siswa dalam kelompok ini mempunyai tanggal lulus yang berbeda");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(kelompokStatusKeluarSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

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

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Kelompok Status Keluar Siswa harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (statusKeluar.getSelectedItem() == null || statusKeluar.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Status Keluar harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (kelompokStatusKeluarSiswa.getId() != null) {
			kelompokStatusKeluarSiswa = (KelompokStatusKeluarSiswa) session.load(KelompokStatusKeluarSiswa.class,
					kelompokStatusKeluarSiswa.getId());
		}
		kelompokStatusKeluarSiswa.setNama(nama.getValue());

		kelompokStatusKeluarSiswa.setStatusKeluar((StatusKeluarSiswa) (statusKeluar.getSelectedItem() == null ? null
				: statusKeluar.getSelectedItem().getValue()));
		kelompokStatusKeluarSiswa.setTanggalLulus(tanggalLulus.getValue());
		kelompokStatusKeluarSiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		kelompokStatusKeluarSiswa.setSemester((String) semester.getSelectedItem().getValue());
		kelompokStatusKeluarSiswa.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, kelompokStatusKeluarSiswa);

		return true;
	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		List<Long> ids = new ArrayList<Long>();

		Session session = HibernateUtil.currentSession();

		if (!searchSiswa.getValue().trim().isEmpty()) {
			ids = session.createCriteria(Siswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.or(
							Restrictions.ilike("nomorInduk", searchSiswa.getValue().trim(), MatchMode.ANYWHERE),
							Restrictions.ilike("nama", searchSiswa.getValue().trim(), MatchMode.ANYWHERE)))
					.add(Restrictions.isNotNull("kelompokStatusKeluarSiswa"))
					.setProjection(Projections.groupProperty("kelompokStatusKeluarSiswa.id")).list();
		}

		Criteria criteria = session.createCriteria(KelompokStatusKeluarSiswa.class);

		if (!ids.isEmpty()) {
			criteria.add(Restrictions.in("id", ids));
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

		List<KelompokStatusKeluarSiswa> kelompokStatusKeluarSiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(kelompokStatusKeluarSiswa);
		grid.setRowRenderer(new KelompokStatusKeluarSiswaRenderer());
		grid.setModelCheckMobile(strset);
	}

}
