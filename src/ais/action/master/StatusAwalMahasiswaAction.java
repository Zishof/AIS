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
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.StatusAwalMahasiswa;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Controller/action ZK untuk status awal mahasiswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox kode}, {@code Textbox nama}, {@code
 * Textbox keterangan}, {@code Intbox feeder}; inisialisasi/lifecycle ({@code doBeforeCompose()}, {@code
 * doAfterCompose()}, {@code init()}, {@code initCriteria()}, {@code init()}); pembacaan/pencarian ({@code
 * onSearchDefault()}); validasi/perhitungan ({@code checkNamaStatusAwalMahasiswa()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
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
public class StatusAwalMahasiswaAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;

	private Textbox kode;
	private Textbox nama;
	private Textbox keterangan;
	private Intbox feeder;

	private StatusAwalMahasiswa statusAwalMahasiswa;
	private MyToolbarbuttonConfig add;

	private boolean edit = false;
	private boolean delete = false;

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
	}

	class StatusAwalMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final StatusAwalMahasiswa statusAwalMahasiswa = (StatusAwalMahasiswa) arg1;
			new Label(statusAwalMahasiswa.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(StatusAwalMahasiswa.class, statusAwalMahasiswa, statusAwalMahasiswa.getNama())
					.setParent(arg0);
			new Label(statusAwalMahasiswa.getFeeder() == null ? "" : statusAwalMahasiswa.getFeeder().toString())
					.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(statusAwalMahasiswa.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusAwalMahasiswa.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(statusAwalMahasiswa);
				}
			});

			final MyCheckboxConfig pindahan = new MyCheckboxConfig("Pindahan");
			pindahan.setDisabled(!edit);
			pindahan.setChecked(statusAwalMahasiswa.getPindahan());
			pindahan.setParent(arg0);
			pindahan.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusAwalMahasiswa.setPindahan(pindahan.isChecked());
					Common.refreshSaveOrUpdate(statusAwalMahasiswa);
				}
			});

			final MyCheckboxConfig alihProdi = new MyCheckboxConfig("Alih Prodi");
			alihProdi.setDisabled(!edit);
			alihProdi.setChecked(statusAwalMahasiswa.getAlihProdi());
			alihProdi.setParent(arg0);
			alihProdi.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					statusAwalMahasiswa.setAlihProdi(alihProdi.isChecked());
					Common.refreshSaveOrUpdate(statusAwalMahasiswa);
				}
			});

			new Label(statusAwalMahasiswa.getKeterangan()).setParent(arg0);

			Common.copyEditDeleteButtons(edit, delete, statusAwalMahasiswa, StatusAwalMahasiswaAction.this)
					.setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new StatusAwalMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(StatusAwalMahasiswa statusAwalMahasiswa) {
		this.statusAwalMahasiswa = statusAwalMahasiswa;
		addWindow.setTitle(statusAwalMahasiswa.getId() == null ? "Tambah Status Awal Mahasiswa" : "Ubah Status Awal Mahasiswa");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Status Awal Mahasiswa"));
		row.appendChild(kode = new Textbox(statusAwalMahasiswa.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Status Awal Mahasiswa"));
		row.appendChild(nama = new Textbox(statusAwalMahasiswa.getNama() == null ? "" : statusAwalMahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Feeder"));
		row.appendChild(feeder = new Intbox(statusAwalMahasiswa.getFeeder().intValue()));
		feeder.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				statusAwalMahasiswa.getKeterangan() == null ? "" : statusAwalMahasiswa.getKeterangan()));
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
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kode",
					"Kolom Kode belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kode.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (feeder.getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Feeder",
					"Kolom Feeder belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Feeder.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		boolean i = checkNamaStatusAwalMahasiswa();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data StatusAwalMahasiswa",
					"Nama StatusAwalMahasiswa sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan nama statusawalmahasiswa yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (statusAwalMahasiswa.getId() != null) {
			statusAwalMahasiswa = (StatusAwalMahasiswa) session.load(StatusAwalMahasiswa.class,
					statusAwalMahasiswa.getId());

		}

		statusAwalMahasiswa.setKode(kode.getValue().trim());
		statusAwalMahasiswa.setNama(nama.getValue().trim());
		statusAwalMahasiswa.setKeterangan(keterangan.getValue());
		statusAwalMahasiswa.setFeeder(feeder.getValue().longValue());

		Common.refreshSaveOrUpdate(session, statusAwalMahasiswa);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(StatusAwalMahasiswa.class);

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<StatusAwalMahasiswa> statusAwalMahasiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(statusAwalMahasiswa);
		grid.setRowRenderer(new StatusAwalMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaStatusAwalMahasiswa() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(StatusAwalMahasiswa.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.statusAwalMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.statusAwalMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		statusAwalMahasiswa = (StatusAwalMahasiswa) obj;
		init(statusAwalMahasiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}
}
