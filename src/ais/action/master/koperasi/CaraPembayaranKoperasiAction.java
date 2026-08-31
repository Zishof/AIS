package ais.action.master.koperasi;

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
import org.zkoss.zul.Vbox;

import ais.action.master.akunting.helper.AmbilDataAkunBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.koperasi.CaraPembayaranKoperasi;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.sekolah.KanalPembayaran;
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
 * Controller/action ZK untuk cara pembayaran koperasi. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Textbox searchnama}, {@code Textbox searchkode}, {@code Checkbox
 * searchaktif}, {@code Textbox nama}, {@code Textbox keterangan}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code initCriteria()});
 * pembacaan/pencarian ({@code onSearchDefault()}); validasi/perhitungan ({@code
 * checkNamaCaraPembayaranKoperasi()}); mutasi data ({@code onSave()}); operasi domain lain ({@code onAdd()}).
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
public class CaraPembayaranKoperasiAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 *
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchkode;
	private Checkbox searchaktif;

	private Textbox nama;
	private Textbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private CaraPembayaranKoperasi caraPembayaranKoperasi;
	private MyToolbarbuttonConfig add;
	private Textbox kode;
	private AmbilDataAkunBanbox akun;
	private Combobox koperasi;
	private Combobox kanalPembayaran;
	private MyCheckboxConfig memotongDeposit;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		org.zkoss.zul.Div mainContainer = (org.zkoss.zul.Div) comp.getFellow("mainContainer");
		int[] tabAktif = {0};
		ais.ui.util.MyButtonTabbox btabs = ais.ui.util.MyButtonTabbox.buat(mainContainer, "100%", tabAktif);

		org.zkoss.zul.Div panel0 = btabs.tambahTab(0, "Jenis Pembayaran");
		ais.ui.util.MyButtonTabbox.muatZulEager(panel0,
				"/WEB-INF/z/x/y/pages/master/koperasi/cara_pembayaran_koperasi_tab_0.zul");

		super.doAfterCompose(comp);

		btabs.tambahTabLazy(1, "Kanal Pembayaran", new ais.ui.util.MyButtonTabbox.PemuatTab() {
			@Override
			public void muat(org.zkoss.zul.Div panel) throws Exception {
				ais.ui.util.MyButtonTabbox.muatZul(panel,
						"/WEB-INF/z/x/y/pages/master/sekolah/kanal_pembayaran.zul");
			}
		});

		btabs.pulihkanSeleksi(2);

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

		// "memotongDeposit" ikut disertakan supaya Cetak Data & Unggah Data (impor Excel) setara
		// dengan form -- kalau tidak, admin bisa memperbarui massal lewat Excel dan diam-diam
		// mengosongkan flag pemotongan saldo yang sudah diatur.
		String[] contents = new String[] { "id", "kode", "nama", "akun", "koperasi", "kanalPembayaran", "keterangan",
				"memotongDeposit", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(CaraPembayaranKoperasi.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, CaraPembayaranKoperasi.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);
	}

	class CaraPembayaranKoperasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final CaraPembayaranKoperasi caraPembayaranKoperasi = (CaraPembayaranKoperasi) arg1;
			new Label(caraPembayaranKoperasi.getKode()).setParent(arg0);
			Vbox a;
			(a = RevisiHelper.createNewRevisi(CaraPembayaranKoperasi.class, caraPembayaranKoperasi,
					caraPembayaranKoperasi.getNama())).setParent(arg0);
			new Label(caraPembayaranKoperasi.getKanalPembayaran() == null ? ""
					: caraPembayaranKoperasi.getKanalPembayaran().getNama()).setParent(a);

			new Label(caraPembayaranKoperasi.getAkun() == null ? "" : caraPembayaranKoperasi.getAkun().getNama())
					.setParent(arg0);
			new Label(
					caraPembayaranKoperasi.getKoperasi() == null ? "" : caraPembayaranKoperasi.getKoperasi().getNama())
					.setParent(arg0);
			new Label(caraPembayaranKoperasi.getKeterangan()).setParent(arg0);

			// Kolom "Potong Saldo": tampilkan efek NYATA, yaitu manual=false ATAU memotongDeposit=true
			// (syarat yang dipakai DepositHelper) -- bukan sekadar nilai kolom barunya, supaya admin
			// tidak salah mengira metode yang manual-nya mati berarti tidak memotong saldo.
			boolean efektifPotong = !Boolean.TRUE.equals(caraPembayaranKoperasi.getManual())
					|| Boolean.TRUE.equals(caraPembayaranKoperasi.getMemotongDeposit());
			Label lblPotong = new Label(efektifPotong ? "Ya" : "Tidak");
			lblPotong.setStyle(efektifPotong ? "font-weight:800;color:#b45309;" : "color:#64748b;");
			lblPotong.setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(caraPembayaranKoperasi.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					caraPembayaranKoperasi.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(caraPembayaranKoperasi);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, caraPembayaranKoperasi, CaraPembayaranKoperasiAction.this)
					.setParent(arg0);

		}

	}

	public void onAdd(Event event) throws Exception {
		init(new CaraPembayaranKoperasi());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		caraPembayaranKoperasi = (CaraPembayaranKoperasi) obj;
		init(caraPembayaranKoperasi);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void init(CaraPembayaranKoperasi caraPembayaranKoperasi) {
		this.caraPembayaranKoperasi = caraPembayaranKoperasi;
		addWindow.setTitle(caraPembayaranKoperasi.getId() == null ? "Tambah Cara Pembayaran" : "Ubah Cara Pembayaran");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Cara Pembayaran"));
		row.appendChild(kode = new Textbox(caraPembayaranKoperasi.getKode()));
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Cara Pembayaran *"));
		row.appendChild(nama = new Textbox(caraPembayaranKoperasi.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setStyle("border:0px;background: transakun;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Akun ")));
		row.appendChild(akun = new AmbilDataAkunBanbox());
		akun.setValue(caraPembayaranKoperasi.getAkun() == null ? "" : caraPembayaranKoperasi.getAkun().getNama());
		akun.setAttribute("akun", caraPembayaranKoperasi.getAkun());
		akun.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku untuk koperasi"));
		row.appendChild(koperasi = new Combobox());
		Koperasi myKoperasi = Common.getCurrentKoperasi();
		Common.insertComboDanSemua(koperasi, "nama", "keterangan", Koperasi.class, Restrictions.eq("aktif", true));

		if (myKoperasi != null) {
			koperasi.setDisabled(true);
			Common.selectComboItem(true, koperasi, myKoperasi);
		}

		koperasi.setWidth("90%");
		koperasi.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kanal Pembayaran *"));
		row.appendChild(kanalPembayaran = new Combobox());
		kanalPembayaran.setWidth("90%");
		kanalPembayaran.setReadonly(true);

		Common.insertCombo(kanalPembayaran, new String[] { "nama" }, "keterangan", KanalPembayaran.class,
				Restrictions.eq("aktif", true));
		Common.selectComboItem(kanalPembayaran, caraPembayaranKoperasi.getKanalPembayaran());

		// "Memotong Deposit" -- lihat CaraPembayaranKoperasi.getMemotongDeposit(). Syarat pemotongan
		// saldo anggota: manual = false ATAU memotongDeposit = true. Tanpa kotak ini, metode bayar
		// yang perlu SEKALIGUS diverifikasi admin dan memotong saldo (mis. Voucher) tak bisa dibuat
		// dari layar ZK -- hanya dari layar JSP Metode Pembayaran, sehingga kedua layar tidak setara.
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Memotong Deposit"));
		row.appendChild(memotongDeposit = new MyCheckboxConfig("Kurangi saldo/Deposit anggota saat dipakai belanja"));
		memotongDeposit.setChecked(Boolean.TRUE.equals(caraPembayaranKoperasi.getMemotongDeposit()));
		memotongDeposit.setTooltiptext("Bila dicentang, belanja dengan metode ini mengurangi saldo anggota walaupun "
				+ "Verifikasi Manual menyala. Perubahan berlaku surut untuk transaksi yang sudah ada.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(caraPembayaranKoperasi.getKeterangan()));
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
			MyMessageboxConfig.show("Mohon maaf, nama cara pembayaran koperasi belum diisi. Langkah yang dapat dilakukan: (1) isi kolom Nama Cara Pembayaran; (2) gunakan nama yang deskriptif dan belum terpakai; (3) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (kanalPembayaran.getSelectedItem() == null || kanalPembayaran.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, kanal cara pembayaran belum dipilih. Langkah yang dapat dilakukan: (1) pilih kanal pembayaran dari daftar (tunai/transfer/dll.); (2) ulangi penyimpanan.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		boolean i = checkNamaCaraPembayaranKoperasi();
		if (i) {
			MyMessageboxConfig.show("Mohon maaf, nama cara pembayaran koperasi sudah digunakan. Langkah yang dapat dilakukan: (1) gunakan nama lain yang belum terdaftar; (2) cari cara pembayaran dengan nama tersebut di daftar; (3) ulangi penyimpanan.", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (caraPembayaranKoperasi.getId() != null) {
			caraPembayaranKoperasi = (CaraPembayaranKoperasi) session.load(CaraPembayaranKoperasi.class,
					caraPembayaranKoperasi.getId());

		}

		caraPembayaranKoperasi.setKode(kode.getValue());
		caraPembayaranKoperasi.setNama(nama.getValue());
		caraPembayaranKoperasi.setAkun((Akun) akun.getAttribute("akun"));
		caraPembayaranKoperasi.setKeterangan(keterangan.getValue());
		caraPembayaranKoperasi.setKoperasi(
				(Koperasi) (koperasi.getSelectedItem() == null ? null : koperasi.getSelectedItem().getValue()));

		caraPembayaranKoperasi.setKanalPembayaran((KanalPembayaran) (kanalPembayaran.getSelectedItem() == null ? null
				: kanalPembayaran.getSelectedItem().getValue()));

		if (memotongDeposit != null) {
			caraPembayaranKoperasi.setMemotongDeposit(Boolean.valueOf(memotongDeposit.isChecked()));
		}

		Common.refreshSaveOrUpdate(session, caraPembayaranKoperasi);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(CaraPembayaranKoperasi.class)
				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));

		if (order)
			criteria.addOrder(Order.asc("nama"));
		criteria.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE));
		criteria.add(searchkode == null || searchkode.getValue().trim().isEmpty()
		        ? Restrictions.sqlRestriction("true")
		        : Restrictions.ilike("kode", searchkode.getValue().trim(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<CaraPembayaranKoperasi> caraPembayaranKoperasi = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(caraPembayaranKoperasi);
		grid.setRowRenderer(new CaraPembayaranKoperasiRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaCaraPembayaranKoperasi() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(CaraPembayaranKoperasi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.caraPembayaranKoperasi.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.caraPembayaranKoperasi.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
