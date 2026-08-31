package ais.action.master;

import java.io.File;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
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

import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.NamaSekolahAsal;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk nama sekolah asal. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Checkbox searchaktif}, {@code Textbox kode},
 * {@code Textbox nama}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * initdata()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code checkKodeSekolahAsal()}, {@code
 * checkNamaNamaSekolahAsal()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAddExternal()},
 * {@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class NamaSekolahAsalAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Checkbox searchaktif;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private NamaSekolahAsal namaSekolahAsal;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Combobox tingkat;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	@SuppressWarnings({})
	public static void initdata() {
		Session session = HibernateUtil.currentSession();

		int count = ((Number) session.createCriteria(NamaSekolahAsal.class).setProjection(Projections.rowCount())
				.add(Restrictions.isNotNull("kode")).uniqueResult()).intValue();
		if (count == 0) {
			session.createSQLQuery("delete from nama_sekolah_asal where kode is null").executeUpdate();
			File file = new File(Common.REAL_PATH + "/WEB-INF/NPSN.xlsx");

			XSSFWorkbook workbook;
			try {
				workbook = new XSSFWorkbook(file.getAbsolutePath());
				XSSFSheet sheet = workbook.getSheetAt(0);

				int rowCount = (sheet.getLastRowNum() + 1);
				for (int i = 1; i < rowCount; i++) {
					String alamat = Common.getSheetContentAsString(sheet, 3, i);
					String npsn = Common.getSheetContentAsString(sheet, 4, i);
					String nama = Common.getSheetContentAsString(sheet, 5, i);
					String jenis = Common.getSheetContentAsString(sheet, 6, i);
					NamaSekolahAsal namaSekolah = new NamaSekolahAsal();
					namaSekolah.setKode(npsn);
					namaSekolah.setNama(nama);
					namaSekolah.setTingkat(jenis);
					namaSekolah.setKeterangan(alamat);
					session.save(namaSekolah);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			} 	

		}

	}

	@SuppressWarnings({})
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		NamaSekolahAsalAction.initdata();

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

		String[] contents = new String[] { "id", "kode", "nama", "tingkat", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, NamaSekolahAsal.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class NamaSekolahAsalRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final NamaSekolahAsal namaSekolahAsal = (NamaSekolahAsal) arg1;

			new Label(namaSekolahAsal.getKode()).setParent(arg0);

			RevisiHelper.createNewRevisi(NamaSekolahAsal.class, namaSekolahAsal, namaSekolahAsal.getNama())
					.setParent(arg0);
			new Label(namaSekolahAsal.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(namaSekolahAsal.getAktif());
			checkbox.setParent(arg0);arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					namaSekolahAsal.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(namaSekolahAsal);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, namaSekolahAsal, NamaSekolahAsalAction.this).setParent(arg0);

		}

	}

	public static void onAddExternal(Event event, EventListener eventListener, NamaSekolahAsal namaSekolahAsal)
			throws Exception {
		NamaSekolahAsalAction namaSekolahAsalAction = new NamaSekolahAsalAction();
		namaSekolahAsalAction.eventListener = eventListener;
		namaSekolahAsalAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(namaSekolahAsalAction.addWindow);
		namaSekolahAsalAction.addWindow.setHeight("270px");
		namaSekolahAsalAction.addWindow.setWidth("550px");

		namaSekolahAsalAction.init(namaSekolahAsal);

		namaSekolahAsalAction.addWindow.setVisible(true);
		namaSekolahAsalAction.addWindow.onModal();
	}

	public void onAdd(Event event) throws Exception {
		init(new NamaSekolahAsal());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		namaSekolahAsal = (NamaSekolahAsal) obj;
		init(namaSekolahAsal);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(NamaSekolahAsal namaSekolahAsal) {
		this.namaSekolahAsal = namaSekolahAsal;
		addWindow.setTitle(namaSekolahAsal.getId() == null ? "Tambah Nama Institusi Pendidikan" : "Ubah Nama Institusi Pendidikan");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Pokok Sekolah Nasional (NPSN)"));
		row.appendChild(kode = new Textbox(namaSekolahAsal.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Intitusi Pendidikan *"));
		row.appendChild(nama = new Textbox(namaSekolahAsal.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tingkat *"));
		row.appendChild(tingkat = new Combobox());
		tingkat.setWidth("90%");

		MyComboitemConfig comboitemConfig = new MyComboitemConfig(NamaSekolahAsal.SD);
		comboitemConfig.setValue(NamaSekolahAsal.SD);
		tingkat.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(NamaSekolahAsal.SMP);
		comboitemConfig.setValue(NamaSekolahAsal.SMP);
		tingkat.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(NamaSekolahAsal.SMA);
		comboitemConfig.setValue(NamaSekolahAsal.SMA);
		tingkat.appendChild(comboitemConfig);

		comboitemConfig = new MyComboitemConfig(NamaSekolahAsal.PERGURUAN_TINGGI);
		comboitemConfig.setValue(NamaSekolahAsal.PERGURUAN_TINGGI);
		tingkat.appendChild(comboitemConfig);

		Common.selectComboItem(tingkat, namaSekolahAsal.getTingkat());
		tingkat.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(keterangan = new Textbox(namaSekolahAsal.getKeterangan()));
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
		final MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					if (NamaSekolahAsalAction.this.eventListener != null) {
						NamaSekolahAsalAction.this.eventListener
								.onEvent(new Event("", save, NamaSekolahAsalAction.this.namaSekolahAsal));
					}
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Intitusi Pendidikan",
					"Kolom Nama Intitusi Pendidikan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama Intitusi Pendidikan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkKodeSekolahAsal();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nomor Pokok Sekolah Nasional",
					"Nomor Pokok Sekolah Nasional sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Nomor Pokok Sekolah Nasional yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		i = checkNamaNamaSekolahAsal();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Intitusi Pendidikan",
					"Nama Intitusi Pendidikan sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama intitusi pendidikan yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (namaSekolahAsal.getId() != null) {
			namaSekolahAsal = (NamaSekolahAsal) session.load(NamaSekolahAsal.class, namaSekolahAsal.getId());

		}

		namaSekolahAsal.setKode(kode.getValue());
		namaSekolahAsal.setNama(nama.getValue());
		namaSekolahAsal.setTingkat((String) tingkat.getSelectedItem().getValue());
		namaSekolahAsal.setKeterangan(keterangan.getValue());

		Common.refreshSaveOrUpdate(session, namaSekolahAsal);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(NamaSekolahAsal.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		List<NamaSekolahAsal> namaSekolahAsal = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(namaSekolahAsal);
		grid.setRowRenderer(new NamaSekolahAsalRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkKodeSekolahAsal() {

		if (kode.getValue().trim().isEmpty()) {
			return false;
		}

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(NamaSekolahAsal.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("kode", kode.getValue().trim())).add(this.namaSekolahAsal.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.namaSekolahAsal.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	public Boolean checkNamaNamaSekolahAsal() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(NamaSekolahAsal.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim())).add(this.namaSekolahAsal.getId() == null
						? Restrictions.sqlRestriction("1=1") : Restrictions.ne("id", this.namaSekolahAsal.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
