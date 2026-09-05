package ais.action.master;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.util.media.Media;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.A;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
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
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataAfiliasiCalonMahasiswaBanbox;
import ais.action.master.helper.DetailSettingBiayaAction;
import ais.action.master.helper.PengecualianTagihanList;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.SettingBiayaMahasiswaSelector;
import ais.action.master.helper.SetingBiayaHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.SettingBiayaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailSettingBiaya;
import ais.database.model.GelombangPendaftaran;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.JenisSeleksi;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Paket;
import ais.database.model.Perkuliahan;
import ais.database.model.SettingBiaya;
import ais.database.model.SettingBiayaDetail;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Layar CRUD sekaligus MESIN INTI penentuan tagihan biaya kuliah/sekolah ({@link SettingBiaya}
 * beserta rincian {@link DetailSettingBiaya}) — salah satu komponen paling kritis pada modul
 * keuangan AIS. Satu baris {@link SettingBiaya} mendefinisikan sekumpulan ATURAN pencocokan
 * (jenis kegiatan, angkatan, jenjang, jurusan, program, status awal/status mahasiswa, kelamin,
 * jenis seleksi, gelombang pendaftaran, paket, afiliasi, rentang semester, tahun akademik) beserta
 * DAFTAR ITEM BIAYA yang berlaku bila aturan tersebut cocok dengan data mahasiswa/calon mahasiswa
 * yang sedang ditagih. Nilai {@code null} pada field pencocokan (kecuali {@code jenisKegiatan})
 * berarti "berlaku untuk SEMUA" (wildcard) — baik saat pencarian (layar ini) maupun saat penerapan
 * (method statis {@code get*}), konsisten memakai pola {@code isNull OR eq} agar setting umum tetap
 * ikut cocok saat difilter nilai spesifik. Saat beberapa baris {@link SettingBiaya} sama-sama cocok,
 * pemilihan satu baris "paling spesifik/prioritas" didelegasikan ke
 * {@link ais.action.master.helper.SettingBiayaMahasiswaSelector}.
 *
 * <p>
 * Selain layar CRUD (pencarian, form tambah/ubah dengan grid pemilihan item biaya per
 * {@code bayarKe}, unggah massal, validasi duplikasi lewat {@link #checkSettingBiaya()}), kelas ini
 * menyediakan kumpulan method STATIS yang dipanggil dari banyak tempat lain di aplikasi (proses
 * pembuatan tagihan mahasiswa/calon mahasiswa) untuk MENERAPKAN aturan setting biaya:
 * </p>
 * <ul>
 * <li>{@link #getItemBiaya} — daftar {@link ItemBiaya} yang berlaku untuk satu konteks mahasiswa
 * berdasarkan setting biaya UMUM (bukan {@code khususBuatMahasiswaTertentu}, bukan
 * {@code gunakanBiayaDefault}) yang paling cocok/prioritas.</li>
 * <li>{@link #getDetailBiayaDefault} (empat overload: berbasis {@link BiodataCalonMahasiswa},
 * {@link Mahasiswa}, atau kombinasi kriteria mentah) — mencari {@link SettingBiayaDetail} KHUSUS
 * satu individu ({@code khususBuatMahasiswaTertentu=true}) yang cocok rentang semester, lalu
 * mendelegasikan penyusunan detail tagihan ke {@link #getDefaultSettingBiaya}. Mahasiswa yang masuk
 * daftar pengecualian ({@code isMahasiswaDikecualikan}) menerima tagihan kosong
 * ({@link PengecualianTagihanList#kosong()}), bukan {@code null}.</li>
 * <li>{@link #getDefaultSettingBiaya} (tiga overload) — menyusun daftar {@link DetailBiaya} aktual
 * dari {@link DetailSettingBiaya} suatu {@link SettingBiaya}/{@link SettingBiayaDetail}, memakai
 * baris {@link DetailBiaya} yang sudah ada bila cocok persis, atau menyalin dari template terdekat
 * bila belum ada.</li>
 * <li>{@link #getDetailBiayaBukanDefaultBiaya} — varian untuk setting biaya yang BUKAN default
 * (mis. biaya tambahan/opsional di luar paket biaya utama).</li>
 * </ul>
 */
public class SetingBiayaAction extends GenericAutowireComposer {
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchJenisKegiatan;
	private Combobox searchAngkatan;
	private Combobox searchjurusan;
	private Combobox jenisKegiatan;
	private Combobox searchjenjang;
	private Combobox searchstatusAwalMahasiswa;
	protected Combobox searchprogram;
	private Combobox jenjang;
	private Combobox angkatan;
	private Combobox program;
	private MyCheckboxConfig gunakanBiayaDefault;
	private SettingBiaya settingBiaya;
	private MyToolbarbuttonConfig add;
	private boolean edit;
	private boolean delete;
	private MyComboitemConfig comboitem;
	private Map<Integer, Map<Long, DetailSettingBiaya>> selectedItemBiayaIndex;
	private Combobox statusAwalMahasiswa;
	private AmbilDataAfiliasiCalonMahasiswaBanbox afiliasiCalonMahasiswa;
	private Combobox jenisSeleksi;
	private Combobox jurusan;
	private Intbox maxSmt;
	private Intbox minSmt;
	private MyCheckboxConfig smtIkutiSettinganDisini;
	private Combobox gelombangPendaftaran;
	private Combobox paket;
	private MyCheckboxConfig khususBuatMahasiswaTertentu;
	private MyCheckboxConfig batasiMahasiswaTertentu;
	private Intbox jumlahPembayaran;
	private MyCheckboxConfig tampilkanPerProdi;

	private Textbox searchGelombang;
	private Textbox searchSeleksi;
	private EventListener eventListener = null;
	private Combobox statusMahasiswa;
	private Combobox kelamin;
	private Combobox tahunAkademik;
	private Combobox semester;
	private MyCheckboxConfig terdapatPengecualianMahasiswa;
	private Textbox pengecualianMahasiswa;
	private Intbox prioritas;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/** Menginisialisasi layar: cek sesi/hak baca (logoff bila tidak valid), mengisi seluruh combobox filter pencarian (jenis kegiatan, angkatan 2000-2030, jenjang, jurusan, status awal, program), hak akses ubah/hapus, pencarian awal, dan paging. */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		Common.initPrograms(searchprogram);

		// FIX NPE: Jurusan TIDAK punya properti Hibernate-mapped "keterangan".
		Common.insertComboDanSemua(searchjurusan, new String[] { "nama" }, "", Jurusan.class,
				Restrictions.eq("aktif", true));

		Common.insertComboDanSemua(searchstatusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(searchJenisKegiatan, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua Jenis Kegiatan"); }
		if (comboitem != null) { comboitem.setValue(new JenisKegiatan()); }
		searchJenisKegiatan.appendChild(comboitem);
		if (searchJenisKegiatan != null) { searchJenisKegiatan.setSelectedIndex(searchJenisKegiatan.getItemCount() - 1); }

		if (searchJenisKegiatan != null) { searchJenisKegiatan.setReadonly(true); }

		comboitem = new MyComboitemConfig();
		if (comboitem != null) { comboitem.setLabel("Semua Angkatan"); }
		if (comboitem != null) { comboitem.setValue(0); }
		searchAngkatan.appendChild(comboitem);
		for (int i = 2000; i <= 2030; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			searchAngkatan.appendChild(comboitem);
		}
		if (searchAngkatan != null) { searchAngkatan.setSelectedIndex(0); }

		if (searchAngkatan != null) { searchAngkatan.setReadonly(true); }

		Common.insertComboDanSemua(searchjenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		if (searchjenjang != null) { searchjenjang.setReadonly(true); }

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Mengambil id entity sebagai nilai parameter filter layar Billing. Nilai {@code -1}
	 * adalah kontrak layar {@code detail_biaya_excel.zul} untuk "Semua/tidak dibatasi".
	 */
	private static String idFilterBilling(Object nilai) {
		if (nilai instanceof ais.database.model.GeneralValueObject) {
			Long id = ((ais.database.model.GeneralValueObject) nilai).getId();
			return id == null ? "-1" : id.toString();
		}
		return "-1";
	}

	/**
	 * Membangun deep-link editor Billing untuk tepat satu {@link SettingBiaya}. Semua kriteria
	 * penting dari baris sumber diteruskan sebagai filter agar operator tidak perlu memilih ulang
	 * semester, periode, angkatan, program, jenjang, prodi, status, jenis kegiatan, paket, seleksi,
	 * dan gelombang. Parameter {@code settingBiayaBulanan} mengikat template Billing yang dibuat
	 * oleh layar tujuan ke SettingBiaya ini; {@code autoBukaRencanaAngsuran=1} langsung membuka
	 * editor pembuatan Billing setelah data prodi ditemukan.
	 *
	 * <p>Method ini hanya digunakan untuk baris dengan "Tagihan Default = Tidak". Pada mode itu
	 * nominal tagihan tidak boleh dianggap otomatis berasal dari nilai default
	 * {@link DetailSettingBiaya}; operator memang harus membuat/mengatur Billing. Bila tahun
	 * akademik, semester masuk, program, atau angkatan tidak dibatasi oleh setting, nilai periode
	 * berjalan dipakai sebagai titik awal yang tetap dapat disesuaikan pada layar Billing.</p>
	 */
	private static String urlBuatBilling(SettingBiaya settingBiaya) throws Exception {
		if (settingBiaya == null || settingBiaya.getId() == null) {
			throw new IllegalArgumentException("Setting Biaya harus sudah disimpan sebelum Billing dibuat.");
		}
		Integer semester = settingBiaya.getMinSmt() == null ? Integer.valueOf(1) : settingBiaya.getMinSmt();
		String tahunAjaran = settingBiaya.getTahunAkademik();
		if (tahunAjaran == null || tahunAjaran.trim().length() == 0) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		if (tahunAjaran == null) {
			tahunAjaran = "";
		}
		String semesterMulai = settingBiaya.getSemester();
		if (semesterMulai == null || semesterMulai.trim().length() == 0) {
			semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		String program = settingBiaya.getProgram() == null || settingBiaya.getProgram().trim().length() == 0
				? "Reguler" : settingBiaya.getProgram();
		Integer angkatan = settingBiaya.getAngkatan() == null
				? Integer.valueOf(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR))
				: settingBiaya.getAngkatan();

		return "/pages/master/detail_biaya_excel.zul?settingBiayaBulanan=" + settingBiaya.getId()
				+ "&searchSemester=" + semester
				+ "&searchTahunAjaran=" + URLEncoder.encode(tahunAjaran, "UTF-8")
				+ "&labelAngkatan=" + angkatan
				+ "&searchMulaiBelajarDiSemester=" + URLEncoder.encode(semesterMulai, "UTF-8")
				+ "&searchProgram=" + URLEncoder.encode(program, "UTF-8")
				+ "&searchWargaNegara=WNI"
				+ "&searchJenjang=" + idFilterBilling(settingBiaya.getJenjang())
				+ "&searchJurusan=" + idFilterBilling(settingBiaya.getJurusan())
				+ "&searchStatusMahasiswa=" + idFilterBilling(settingBiaya.getStatusMahasiswa())
				+ "&searchStatusAwalMahasiswa=" + idFilterBilling(settingBiaya.getStatusAwalMahasiswa())
				+ "&searchJenisKegiatan=" + idFilterBilling(settingBiaya.getJenisKegiatan())
				+ "&searchPaket=" + idFilterBilling(settingBiaya.getPaket())
				+ "&searchJenisSeleksi=" + idFilterBilling(settingBiaya.getJenisSeleksi())
				+ "&searchGelombangPendaftaran=" + idFilterBilling(settingBiaya.getGelombangPendaftaran())
				+ "&autoBukaRencanaAngsuran=1";
	}

	/** Membuka editor Billing yang sama dari aksi baris maupun dari form Setting Biaya. */
	private void bukaEditorBilling(SettingBiaya settingBiaya) {
		try {
			Common.displayWindow(urlBuatBilling(settingBiaya), true, "95%", "98%",
					new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							onSearchDefault(null);
						}
					}, "Buat Billing - Setting Biaya");
		} catch (Exception e) {
			PesanFormalHelper.tampilkanGagalException("Membuka editor Billing", e,
					new String[] { "Simpan Setting Biaya terlebih dahulu sebelum membuka Billing.",
							"Periksa kembali periode, semester, prodi, program, dan item biaya pada Setting Biaya." });
		}
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link SetingBiayaAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link SetingBiayaAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see SetingBiayaAction
	 */
	class SettingBiayaRenderer extends ais.ui.util.MyRowRenderer {

		
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			arg0.setValign("top");
			final SettingBiaya settingBiaya = (SettingBiaya) arg1;

			if (!edit) {
				new Label().setParent(arg0);
			} else {
				DetailSettingBiayaAction detail = new DetailSettingBiayaAction(settingBiaya);
				detail.setParent(arg0);
			}

			RevisiHelper.createNewRevisi(SettingBiaya.class, settingBiaya,
					settingBiaya.getJenisKegiatan() == null ? "" : settingBiaya.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);

			new Label(settingBiaya.getJenjang() == null ? "Semua" : settingBiaya.getJenjang().getNama())
					.setParent(arg0);
			new Label(settingBiaya.getAngkatan() == null ? "Semua" : settingBiaya.getAngkatan().toString())
					.setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(settingBiaya.getStatusAwalMahasiswa() == null ? ""
					: settingBiaya.getStatusAwalMahasiswa().getNama()).setParent(vbox);
			new Label(settingBiaya.getStatusMahasiswa() == null ? "" : settingBiaya.getStatusMahasiswa().getNama())
					.setParent(vbox);
			new Label(settingBiaya.getAfiliasiCalonMahasiswa() == null ? ""
					: settingBiaya.getAfiliasiCalonMahasiswa().getNama()).setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(arg0);
			new Label(settingBiaya.getJenisSeleksi() == null ? "Semua" : settingBiaya.getJenisSeleksi().getNama())
					.setParent(vbox);
			new Label(settingBiaya.getGelombangPendaftaran() == null ? ""
					: settingBiaya.getGelombangPendaftaran().getNama()).setParent(vbox);
			new Label(settingBiaya.getPaket() == null ? "" : settingBiaya.getPaket().getNama()).setParent(vbox);
			new Label(settingBiaya.getKelamin() == null ? "" : settingBiaya.getKelamin()).setParent(vbox);

			new Label(settingBiaya.getJurusan() == null ? "Semua" : settingBiaya.getJurusan().getNama())
					.setParent(arg0);
			new Label(settingBiaya.getProgram() == null ? "Semua" : settingBiaya.getProgram()).setParent(arg0);

			new Label(settingBiaya.getGunakanBiayaDefault() ? "Ya" : "Tidak").setParent(arg0);

			new Label(settingBiaya.getMinSmt() + " sd " + settingBiaya.getMaxSmt()).setParent(arg0);

			Label pengecualian = new Label(settingBiaya.getPengecualianMahasiswa());
			pengecualian.setMultiline(true);
			pengecualian.setTooltiptext(settingBiaya.getPengecualianMahasiswa().length() == 0
					? "Tidak ada mahasiswa yang dikecualikan"
					: "NIM berikut tidak akan memperoleh tagihan dari setting ini");
			pengecualian.setParent(arg0);

			Label labelPrioritas = new Label(settingBiaya.getPrioritas().toString());
			labelPrioritas.setTooltiptext("Angka lebih kecil didahulukan sebelum pembobotan kecocokan setting biaya");
			labelPrioritas.setParent(arg0);

			Session session = HibernateUtil.currentSession();
			List<DetailSettingBiaya> selectedItemBiaya = ConstantValues
					.simpleList(session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya")
							.addOrder(Order.asc("itemBiaya.nama")).addOrder(Order.asc("bayarKe"))
							.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
									Restrictions.eq("itemBiaya.aktif", true)))
							.add(Restrictions.eq("settingBiaya", settingBiaya)), DetailSettingBiaya.class);
			vbox = new Vbox();
			vbox.setParent(arg0);
			// Tombol "Ubah" di atas daftar Item Biaya — kolom ini ikut terlihat di layar HP/sempit,
			// sedangkan toolbar aksi di kolom paling kanan sering berada di luar layar. Toolbar kanan
			// (edit/copy/hapus) tetap dipertahankan utuh untuk tampilan desktop.
			if (edit) {
				MyToolbarbuttonConfig editItem = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
				editItem.setTooltiptext("Ubah Data");
				editItem.setStyle("color:#0056b3; font-weight:bold;");
				editItem.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						init(settingBiaya);
						addWindow.setVisible(true);
						addWindow.onModal();
					}
				});
				editItem.setParent(vbox);
			}
			Groupbox daftarItemBiayaBox = new Groupbox();
			daftarItemBiayaBox.setClosable(true);
			daftarItemBiayaBox.setOpen(false);
			daftarItemBiayaBox.setWidth("260px");
			daftarItemBiayaBox.setStyle("margin-top:4px;");
			daftarItemBiayaBox.appendChild(new Caption("Item Biaya (" + selectedItemBiaya.size() + ")"));
			Vbox daftarItemBiaya = new Vbox();
			daftarItemBiaya.setParent(daftarItemBiayaBox);
			daftarItemBiayaBox.setParent(vbox);
			int i = 1;
			for (DetailSettingBiaya itemBiaya : selectedItemBiaya) {
				RevisiHelper.createNewRevisi(DetailSettingBiaya.class, itemBiaya, i + ". "
						+ itemBiaya.getItemBiaya().getNama()
						+ (settingBiaya.getJumlahPembayaran() > 1 ? " ke-" + itemBiaya.getBayarKe() + " " : "")
						+ (itemBiaya.getDefaultBiaya() > 0.1
								? " Nominal : " + Common.numberFormat.get().format(itemBiaya.getDefaultBiaya()) + ""
								: "")
						+ (itemBiaya.getDefaultTanggalTagihan() != null
								? " Tagihan : " + Common.dateFormat.get().format(itemBiaya.getDefaultTanggalTagihan()) + ""
								: ""))
						.setParent(daftarItemBiaya);
				i++;
			}
			selectedItemBiaya = null;

			new Label(settingBiaya.getTa().equals(0) ? "" : settingBiaya.getTa().toString()).setParent(arg0);

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dibungkus kebab popup (⋯)
			// via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten antar layar.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			if (!Boolean.TRUE.equals(settingBiaya.getGunakanBiayaDefault())) {
				MyToolbarbuttonConfig billing = new MyToolbarbuttonConfig("", "/img/svg/money-bills.svg");
				billing.setTooltiptext("Buat Billing");
				billing.setVisible(edit);
				billing.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						bukaEditorBilling(settingBiaya);
					}
				});
				aksiButtons.add(billing);
			}

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(settingBiaya);
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/edit-copy.svg");
			button.setTooltiptext("Copy Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					SettingBiaya copyObjt = (SettingBiaya) settingBiaya.clone();

					copyObjt.setId(null);
					copyObjt.setCopyDari(settingBiaya);
					init(copyObjt);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											int detail_biaya = session.createSQLQuery(
													"update detail_biaya set setting_biaya=null,detail_setting_biaya=null,setting_biaya_detail=null where setting_biaya="
															+ settingBiaya.getId() + ";")
													.executeUpdate();

											System.out.println("detail_biaya -> " + detail_biaya);

											/*
											 * Hapus dulu baris anak di setting_biaya_detail yang menunjuk
											 * setting_biaya ini. Tanpa langkah ini, DELETE induk melanggar
											 * FK fkab1d273d23361fda (setting_biaya_detail.setting_biaya) →
											 * flush gagal, transaksi di-rollback, lalu onSearchDefault
											 * berikutnya ikut gagal ("createCriteria is not valid without
											 * active transaction"). Referensi dari detail_biaya sudah
											 * di-null-kan di atas sehingga baris detail ini aman dihapus.
											 */
											int setting_biaya_detail = session.createSQLQuery(
													"delete from setting_biaya_detail where setting_biaya="
															+ settingBiaya.getId() + ";")
													.executeUpdate();

											System.out.println("setting_biaya_detail -> " + setting_biaya_detail);

											int detail_setting_biaya = session.createSQLQuery(
													"delete from detail_setting_biaya where setting_biaya="
															+ settingBiaya.getId() + ";")
													.executeUpdate();
											System.out.println("detail_setting_biaya -> " + detail_setting_biaya);

											Common.refreshDelete(settingBiaya);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show("Data ini tidak dapat dihapus");
										}
									}

								}
							});

				}
			});
			aksiButtons.add(button);
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	/**
	 * Membuka dialog tambah/ubah setting biaya dari LUAR layar ini (dipanggil oleh layar lain yang
	 * ingin menyisipkan pembuatan setting biaya baru): membuat instance {@code SetingBiayaAction}
	 * sementara, memasang jendela modal berdiri sendiri, dan memanggil {@code eventListener} setelah
	 * data disimpan.
	 *
	 * @param eventListener callback yang dipanggil setelah setting biaya berhasil disimpan
	 * @param settingBiaya  entitas setting biaya yang diedit (entitas baru untuk tambah data)
	 * @throws Exception diteruskan apa adanya dari kegagalan pembangunan form
	 */
	public static void onAddExternal(EventListener eventListener, SettingBiaya settingBiaya) throws Exception {
		SetingBiayaAction setingBiayaAction = new SetingBiayaAction();
		setingBiayaAction.eventListener = eventListener;
		setingBiayaAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(setingBiayaAction.addWindow);
		setingBiayaAction.addWindow.setHeight("95%");
		setingBiayaAction.addWindow.setWidth("90%");

		setingBiayaAction.init(settingBiaya);

		setingBiayaAction.addWindow.setVisible(true);
		setingBiayaAction.addWindow.setClosable(true);
		setingBiayaAction.addWindow.onModal();

	}

	/** Membuka form tambah setting biaya baru. */
	public void onAdd(Event event) throws Exception {
		init(new SettingBiaya());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings({ "deprecation" })
	private void init(final SettingBiaya settingBiaya) throws Exception {
		selectedItemBiayaIndex = new HashMap<Integer, Map<Long, DetailSettingBiaya>>();
		this.settingBiaya = settingBiaya;
		addWindow.setHeight("95%");
		addWindow.setWidth("700px");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		jenisKegiatan = new Combobox();
		Common.insertCombo(jenisKegiatan, "namaKegiatan", JenisKegiatan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisKegiatan, settingBiaya.getJenisKegiatan());
		row.appendChild(jenisKegiatan);
		jenisKegiatan.setWidth("90%");

		jenisKegiatan.setReadonly(true);
		if (settingBiaya.getId() == null && searchJenisKegiatan != null
				&& searchJenisKegiatan.getSelectedItem() != null) {
			Common.selectComboItem(jenisKegiatan, searchJenisKegiatan.getSelectedItem().getValue());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		Common.insertComboDanSemua(jenjang = new Combobox(), "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenjang, settingBiaya.getJenjang());
		row.appendChild(jenjang);
		jenjang.setReadonly(true);
		jenjang.setWidth("90%");

		if (settingBiaya.getId() == null && searchjenjang != null && searchjenjang.getSelectedItem() != null) {
			Common.selectComboItem(jenjang, searchjenjang.getSelectedItem().getValue());
		} else if (settingBiaya.getId() == null) {
			jenjang.setSelectedItem(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		Common.insertComboDanSemua(statusAwalMahasiswa = new Combobox(), "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, settingBiaya.getStatusAwalMahasiswa());
		row.appendChild(statusAwalMahasiswa);
		statusAwalMahasiswa.setReadonly(true);
		statusAwalMahasiswa.setWidth("90%");

		if (settingBiaya.getId() == null && searchstatusAwalMahasiswa != null
				&& searchstatusAwalMahasiswa.getSelectedItem() != null
				&& searchstatusAwalMahasiswa.getSelectedItem().getValue() != null) {
			Common.selectComboItem(statusAwalMahasiswa, searchstatusAwalMahasiswa.getSelectedItem().getValue());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Afiliasi"));
		row.appendChild(afiliasiCalonMahasiswa = new AmbilDataAfiliasiCalonMahasiswaBanbox());
		afiliasiCalonMahasiswa.setAttribute("afiliasiCalonMahasiswa", settingBiaya.getAfiliasiCalonMahasiswa());
		afiliasiCalonMahasiswa.setValue(settingBiaya.getAfiliasiCalonMahasiswa() == null ? ""
				: settingBiaya.getAfiliasiCalonMahasiswa().getNama());
		afiliasiCalonMahasiswa.setReadonly(true);
		afiliasiCalonMahasiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Mahasiswa"));
		Common.insertComboDanSemua(statusMahasiswa = new Combobox(), "nama", StatusMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusMahasiswa, settingBiaya.getStatusMahasiswa());
		row.appendChild(statusMahasiswa);
		statusMahasiswa.setReadonly(true);
		statusMahasiswa.setWidth("90%");

		program = Common.initPrograms(program);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		program.setReadonly(true);
		program.setWidth("90%");
		Common.selectComboItem(program, settingBiaya.getProgram());

		if (settingBiaya.getId() == null && searchprogram != null && searchprogram.getSelectedItem() != null
				&& searchprogram.getSelectedItem().getValue() != null) {
			Common.selectComboItem(program, searchprogram.getSelectedItem().getValue());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Angkatan"));
		angkatan = new Combobox();

		Integer tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);

		for (int i = tahun - 20; i <= tahun + 20; i++) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			angkatan.appendChild(comboitem);
		}

		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		angkatan.appendChild(comboitem);
		Common.selectComboItem(angkatan, settingBiaya.getAngkatan());
		row.appendChild(angkatan);
		angkatan.setReadonly(true);
		if (settingBiaya.getId() == null && searchAngkatan != null && searchAngkatan.getSelectedItem() != null) {
			Common.selectComboItem(angkatan, searchAngkatan.getSelectedItem().getValue());
		} else if (settingBiaya.getId() == null) {
			angkatan.setSelectedItem(comboitem);
		}

		angkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Seleksi"));
		Common.insertComboDanSemua(jenisSeleksi = new Combobox(), "nama", "deskripsi", JenisSeleksi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenisSeleksi, settingBiaya.getJenisSeleksi());
		row.appendChild(jenisSeleksi);
		jenisSeleksi.setReadonly(true);
		jenisSeleksi.setWidth("90%");

		gelombangPendaftaran = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Gelombang Pendaftaran"));
		row.appendChild(gelombangPendaftaran);
		gelombangPendaftaran.setReadonly(true);
		gelombangPendaftaran.setWidth("90%");

		EventListener eventListenerGel = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));

				Integer thn = angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue() == null ? null
						: Integer.valueOf(angkatan.getSelectedItem().getValue().toString());

				if (thn != null) {
					criterion = Restrictions.and(criterion,
							Restrictions.ilike("tahunAkademik", thn + "", MatchMode.START));
				}

				JenisSeleksi js = (JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
						: jenisSeleksi.getSelectedItem().getValue());

				if (js != null) {
					criterion = Restrictions.and(criterion, Restrictions.eq("jenisSeleksi", js));
				}

				Common.insertComboDanSemua(gelombangPendaftaran, new String[] { "nama", "tahunAkademik" }, "keterangan",
						GelombangPendaftaran.class, criterion);
				Common.selectComboItem(true, gelombangPendaftaran, settingBiaya.getGelombangPendaftaran());
			}
		};

		jenisSeleksi.addEventListener("onChange", eventListenerGel);
		angkatan.addEventListener("onChange", eventListenerGel);
		eventListenerGel.onEvent(null);

		paket = new Combobox();
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket"));
		row.appendChild(paket);
		paket.setReadonly(true);
		paket.setWidth("90%");
		Common.insertComboDanSemua(paket, new String[] { "nama" }, "keterangan", Paket.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(true, paket, settingBiaya.getPaket());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jurusan"));
		Common.insertComboDanSemua(jurusan = new Combobox(), new String[] { "nama", "jenjang" }, "kodeEpsbed",
				Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.pilihJurusan(jurusan, settingBiaya.getJurusan());
		row.appendChild(jurusan);
		jurusan.setReadonly(true);
		jurusan.setWidth("90%");

		kelamin = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		kelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		row.appendChild(kelamin);
		Common.selectComboItem(kelamin, settingBiaya.getKelamin());
		kelamin.setReadonly(true);
		kelamin.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(khususBuatMahasiswaTertentu = new MyCheckboxConfig(
				"Khusus buat mahasiswa tertentu dengan nilai yg berbeda-beda"));
		khususBuatMahasiswaTertentu.setChecked(settingBiaya.getKhususBuatMahasiswaTertentu());
		khususBuatMahasiswaTertentu.setTooltiptext(
				"Mode nilai khusus/insidentil per mahasiswa. Jangan gunakan pilihan ini bila tagihan tetap mengikuti billing bulanan.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(batasiMahasiswaTertentu = new MyCheckboxConfig(
				"Batasi hanya untuk mahasiswa yang dipilih (tetap tagihan bulanan)"));
		batasiMahasiswaTertentu.setChecked(settingBiaya.getBatasiMahasiswaTertentu());
		batasiMahasiswaTertentu.setTooltiptext(
				"Menampilkan fitur Ambil Mahasiswa, tetapi nominal dan periode tetap berasal dari Pengaturan Tagihan Bulanan. Mahasiswa yang tidak dipilih tidak memakai setting ini.");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(terdapatPengecualianMahasiswa = new MyCheckboxConfig("Terdapat pengecualian NIM"));
		terdapatPengecualianMahasiswa.setChecked(settingBiaya.getPengecualianMahasiswa().length() > 0);
		terdapatPengecualianMahasiswa.setTooltiptext(
				"Centang bila terdapat mahasiswa yang tidak boleh memakai setting biaya ini.");

		final MyFormRow rowPengecualianMahasiswa = new MyFormRow();
		rowPengecualianMahasiswa.setValign("top");
		rowPengecualianMahasiswa.setParent(rows);
		rowPengecualianMahasiswa.appendChild(new ais.ui.util.MyLabelConfig("Pengecualian Mahasiswa (NIM)"));
		Vbox panelPengecualian = new Vbox();
		panelPengecualian.setWidth("100%");
		pengecualianMahasiswa = new Textbox(settingBiaya.getPengecualianMahasiswa());
		pengecualianMahasiswa.setRows(4);
		pengecualianMahasiswa.setMultiline(true);
		pengecualianMahasiswa.setWidth("100%");
		pengecualianMahasiswa.setTooltiptext(
				"Format lama: NIM,NIM (semua semester). Format rentang: NIM:SMT_MULAI:SMT_SAMPAI;NIM:SMT_MULAI:SMT_SAMPAI.");
		pengecualianMahasiswa.setParent(panelPengecualian);
		Label petunjukPengecualian = new Label(
				"Format: NIM,NIM untuk semua semester, atau NIM:SMT_MULAI:SMT_SAMPAI;NIM:SMT_MULAI:SMT_SAMPAI untuk rentang semester tertentu.");
		petunjukPengecualian.setMultiline(true);
		petunjukPengecualian.setStyle("color:#64748b;font-size:11px;white-space:normal;");
		petunjukPengecualian.setParent(panelPengecualian);
		rowPengecualianMahasiswa.appendChild(panelPengecualian);
		rowPengecualianMahasiswa.setVisible(terdapatPengecualianMahasiswa.isChecked());
		terdapatPengecualianMahasiswa.addEventListener("onCheck", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				rowPengecualianMahasiswa.setVisible(terdapatPengecualianMahasiswa.isChecked());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prioritas"));
		prioritas = new Intbox(settingBiaya.getPrioritas());
		prioritas.setTooltiptext(
				"Semakin kecil nilainya, semakin dahulu setting ini dipertimbangkan. Jika sama, sistem memakai pembobotan kecocokan seperti sebelumnya. Nilai bawaan: 10.");
		row.appendChild(prioritas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Minimal Semester"));
		row.appendChild(minSmt = new Intbox(settingBiaya.getMinSmt()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Maksimal Semester"));
		row.appendChild(maxSmt = new Intbox(settingBiaya.getMaxSmt()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(smtIkutiSettinganDisini = new MyCheckboxConfig(
				"Smt minimal dan maksimal diatur di sini (bila dicentang, rentang semester pada Jenis Pembayaran tidak berlaku)"));
		smtIkutiSettinganDisini.setChecked(settingBiaya.getSmtIkutiSettinganDisini());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(gunakanBiayaDefault = new MyCheckboxConfig(
				"Gunakan Nilai Tagihan Default, jika dipilih, maka tagihan ini tidak perlu diinputkan di menu billing/angsuran"));
		gunakanBiayaDefault.setChecked(settingBiaya.getGunakanBiayaDefault());

		final MyFormRow rowBukaBilling = new MyFormRow();
		rowBukaBilling.setParent(rows);
		rowBukaBilling.appendChild(new ais.ui.util.MyLabelConfig());
		final A bukaBilling = new A("Atur Billing / Angsuran");
		bukaBilling.setTooltiptext("Buka pengaturan Billing untuk Setting Biaya ini");
		bukaBilling.setStyle("font-weight:700;text-decoration:underline;cursor:pointer;");
		bukaBilling.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaEditorBilling(SetingBiayaAction.this.settingBiaya);
			}
		});
		rowBukaBilling.appendChild(bukaBilling);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah Pembayaran"));
		row.appendChild(jumlahPembayaran = new Intbox(settingBiaya.getJumlahPembayaran()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		row.appendChild(tampilkanPerProdi = new MyCheckboxConfig("Tampilkan pilihan tagihan per prodi"));
		tampilkanPerProdi.setChecked(settingBiaya.getTampilkanPerProdi());

		final EventListener eventListenerKhusus = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (khususBuatMahasiswaTertentu.isChecked()) {
					batasiMahasiswaTertentu.setChecked(false);
				}
				jenjang.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				statusAwalMahasiswa.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				afiliasiCalonMahasiswa.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				program.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
//				angkatan.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				jenisSeleksi.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				gelombangPendaftaran.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				paket.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				jurusan.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				gunakanBiayaDefault.getParent().setVisible(!khususBuatMahasiswaTertentu.isChecked());
				rowBukaBilling.setVisible(!khususBuatMahasiswaTertentu.isChecked()
						&& !gunakanBiayaDefault.isChecked());
			}
		};

		khususBuatMahasiswaTertentu.addEventListener("onClick", eventListenerKhusus);
		batasiMahasiswaTertentu.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (batasiMahasiswaTertentu.isChecked()) {
					khususBuatMahasiswaTertentu.setChecked(false);
					eventListenerKhusus.onEvent(null);
				}
			}
		});
		eventListenerKhusus.onEvent(null);

		EventListener eventListenerDefault = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jumlahPembayaran.getParent().setVisible(gunakanBiayaDefault.isChecked());
				tampilkanPerProdi.getParent().setVisible(gunakanBiayaDefault.isChecked());
				rowBukaBilling.setVisible(!khususBuatMahasiswaTertentu.isChecked()
						&& !gunakanBiayaDefault.isChecked());
			}
		};

		gunakanBiayaDefault.addEventListener("onClick", eventListenerDefault);
		eventListenerDefault.onEvent(null);

		tahunAkademik = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		tahunAkademik.appendChild(comboitem);
		tahunAkademik = Common.generateTahunAjaranDanSemua(tahunAkademik);

		semester = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Semua");
		comboitem.setValue(null);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semester.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Tahun Akademik"));
		row.appendChild(tahunAkademik);
		tahunAkademik.setWidth("90%");
		Common.selectComboItem(tahunAkademik, settingBiaya.getTahunAkademik());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Berlaku Mulai Semester"));
		row.appendChild(semester);
		semester.setWidth("90%");
		Common.selectComboItem(semester, settingBiaya.getSemester());
		semester.setReadonly(true);

		// PERMINTAAN: pencarian nama item + paging 10/halaman, KEDUANYA terpisah PER tombol
		// "Ke-N"/per angsuran (bukan satu pencarian+paging global yang berlaku ke semua tab)
		// + urut nama untuk daftar checklist "Item Biaya" di bawah (dulu satu daftar panjang
		// tanpa pencarian/paging, harus discroll manual sampai puluhan baris). Kotak
		// pencarian & widget Paging-nya masing-masing dibangun SATU PER TAB di dalam loop
		// "Ke-N" di bawah (lihat kataCariItemBiayaPerTab/halamanItemBiayaPerTab), ditaruh
		// persis di atas grid milik tab tsb -- BUKAN satu widget global di sini.
		final Map<Integer, String> kataCariItemBiayaPerTab = new HashMap<Integer, String>();
		final Map<Integer, Integer> halamanItemBiayaPerTab = new HashMap<Integer, Integer>();
		// Checkbox "Tampilkan hanya yg dipilih" PER TAB -- saat dicentang, daftar item biaya
		// disaring hanya menampilkan yang SUDAH TERCENTANG (selectedItemBiayaIndex), agar mudah
		// memverifikasi item apa saja yang sudah dipilih tanpa perlu klik halaman satu-satu.
		final Map<Integer, Boolean> hanyaTampilkanTerpilihPerTab = new HashMap<Integer, Boolean>();

		final MyFormRow rowJumlah = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowJumlah, "2");
		rowJumlah.setParent(rows);

		Session session = HibernateUtil.currentSession();
		final List<ItemBiaya> itemBiayaSemua = ConstantValues.simpleList(
				session.createCriteria(ItemBiaya.class).addOrder(Order.asc("nama"))
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
				ItemBiaya.class);

		// BUG "tab Ke-N jadi blank setelah checkbox/filter lain diubah": eventListenerJumlah
		// membangun ULANG seluruh Tabbox dari nol setiap kali dipicu (Jumlah Pembayaran, Tampilkan
		// per prodi, Gunakan Biaya Default, Khusus buat mahasiswa tertentu, Jenjang, Jurusan — semua
		// dipasangi listener yang sama). Tabbox BARU selalu memuat-eager tab Ke-1 SAJA (hardcode),
		// padahal user bisa saja sedang berada di tab lain (mis. Ke-2) saat salah satu field itu
		// diubah -> tab yang tadinya aktif ikut dibongkar & dibangun ulang, tapi kontennya TIDAK
		// pernah di-parent lagi (hanya Ke-1 yang eager) sehingga tab tsb tampak kosong walau masih
		// terlihat aktif/terpilih. Simpan nomor tab TERAKHIR aktif di sini (di luar onEvent, agar
		// bertahan lintas pemanggilan ulang) supaya pembangunan ulang tetap eager-load & memilih
		// tab yang SAMA seperti sebelum perubahan, bukan selalu kembali ke Ke-1.
		final int[] tabAktifTerakhir = new int[] { 1 };

		final EventListener eventListenerJumlah = new EventListener() {

			EventListener getThis() {
				return this;
			}

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowJumlah);

				int jml = jumlahPembayaran.getValue() == null || jumlahPembayaran.getValue() < 1 ? 1
						: jumlahPembayaran.getValue();

				final boolean perProdi = tampilkanPerProdi.isChecked() && gunakanBiayaDefault.isChecked();
				Session session = HibernateUtil.currentSession();
				final List<Jurusan> jurusans = perProdi ? ConstantValues.simpleList(
						session.createCriteria(Jurusan.class)

								.add(jenjang.getSelectedItem() == null || jenjang.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jenjang", jenjang.getSelectedItem().getValue()))

								.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("id",
												((Jurusan) jurusan.getSelectedItem().getValue()).getId()))

								.createAlias("fakultas", "fakultas")
								.add(Restrictions.eq("fakultas.perguruanTinggi",
										PerguruanTinggiUtil.getPerguruanTinggi()))
								.addOrder(Order.asc("fakultas.nama")).addOrder(Order.asc("nama"))
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))),
						Jurusan.class) : null;

				// Saring & paging kini sepenuhnya PER TAB (lihat loop "Ke-N" di bawah) --
				// setiap tombol "Ke-N" punya kotak pencarian & halaman aktifnya sendiri-sendiri.

				// GANTI TAB -> BUTTON GROUP (permintaan user): "Ke-1".."Ke-N" kini berupa
				// tombol horizontal (bukan Tab/Tabpanel bawaan ZK, dan bukan pula Toolbar --
				// lihat javadoc MyButtonTabbox soal kenapa Toolbar bikin tombol tersusun
				// vertikal) lewat kelas reusable ais.ui.util.MyButtonTabbox, supaya pola ini
				// gampang dipakai ulang di layar lain yang selama ini pakai Tabbox dan
				// bermasalah dengan scroll/blank. Konten semua panel tetap dibangun EAGER
				// (lihat loop di bawah), panel bertinggi DEFINITE supaya tidak pernah kolaps
				// ke 0px -- tinggi dihitung dari MAKSIMAL 10 baris (page size paging per tab),
				// bukan dari jumlah total item biaya lagi (tiap tab kini dipotong per halaman).
				final int tinggiPanelItemBiayaPx = Math.min(Math.max(10 * 38 + 150, 320), 4000);
				final ais.ui.util.MyButtonTabbox tabboxKe = jml > 1
						? ais.ui.util.MyButtonTabbox.buat(rowJumlah, (tinggiPanelItemBiayaPx + 44) + "px",
								tabAktifTerakhir)
						: null;


				for (int i = 1; i <= jml; i++) {
					final int index = i;

					if (!selectedItemBiayaIndex.containsKey(index)) {

						if (SetingBiayaAction.this.settingBiaya.getId() != null) {
							session.refresh(SetingBiayaAction.this.settingBiaya);
						}

						final Map<Long, DetailSettingBiaya> selectedItemBiaya;

						if (SetingBiayaAction.this.settingBiaya.getId() != null) {

							List<DetailSettingBiaya> detailSettingBiayas = ConstantValues.simpleList(
									session.createCriteria(DetailSettingBiaya.class)
											.add(Restrictions.or(Restrictions.isNull("bayarKe"),
													Restrictions.eq("bayarKe", index)))
											.createAlias("itemBiaya", "itemBiaya").addOrder(Order.asc("itemBiaya.nama"))
											.addOrder(Order.asc("bayarKe"))
											.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
													Restrictions.eq("itemBiaya.aktif", true)))
											.add(Restrictions.eq("settingBiaya", SetingBiayaAction.this.settingBiaya)),
									DetailSettingBiaya.class);

							selectedItemBiaya = new HashMap<Long, DetailSettingBiaya>();
							for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {
								if (!selectedItemBiaya.containsKey(detailSettingBiaya.getItemBiaya().getId())) {
									selectedItemBiaya.put(detailSettingBiaya.getItemBiaya().getId(),
											detailSettingBiaya);
								}
							}
							detailSettingBiayas = null;
						} else {
							selectedItemBiaya = new HashMap<Long, DetailSettingBiaya>();
						}

						selectedItemBiayaIndex.put(index, selectedItemBiaya);
					}

					// PERMINTAAN: pencarian & paging terpisah PER tombol "Ke-N" -- kata pencarian
					// & halaman aktif tab ini diambil/disimpan sendiri di
					// kataCariItemBiayaPerTab/halamanItemBiayaPerTab (key=index), lalu
					// itemBiayaSemua disaring+dipotong 10/halaman KHUSUS untuk tab ini saja.
					String kataCariTab = kataCariItemBiayaPerTab.containsKey(index)
							? kataCariItemBiayaPerTab.get(index)
							: "";
					final boolean hanyaTerpilihTab = hanyaTampilkanTerpilihPerTab.containsKey(index)
							&& hanyaTampilkanTerpilihPerTab.get(index);
					final List<ItemBiaya> itemBiayaCocok = new ArrayList<ItemBiaya>();
					for (ItemBiaya ib : itemBiayaSemua) {
						if ((kataCariTab.isEmpty()
								|| (ib.getNama() != null && ib.getNama().toLowerCase().contains(kataCariTab)))
								&& (!hanyaTerpilihTab || selectedItemBiayaIndex.get(index).containsKey(ib.getId()))) {
							itemBiayaCocok.add(ib);
						}
					}

					int halamanTab = halamanItemBiayaPerTab.containsKey(index) ? halamanItemBiayaPerTab.get(index) : 0;
					int totalHalamanTab = Math.max(1, (int) Math.ceil(itemBiayaCocok.size() / 10.0));
					if (halamanTab >= totalHalamanTab) {
						halamanTab = totalHalamanTab - 1;
					}
					if (halamanTab < 0) {
						halamanTab = 0;
					}
					halamanItemBiayaPerTab.put(index, halamanTab);
					int mulaiTab = halamanTab * 10;
					int akhirTab = Math.min(mulaiTab + 10, itemBiayaCocok.size());
					final List<ItemBiaya> itemBiayaTab = mulaiTab >= akhirTab ? new ArrayList<ItemBiaya>()
							: itemBiayaCocok.subList(mulaiTab, akhirTab);

					final org.zkoss.zul.Hbox hboxCariPagingTab = new org.zkoss.zul.Hbox();
					hboxCariPagingTab.setWidth("100%");
					hboxCariPagingTab.setSclass("ais-item-biaya-searchbar");

					final org.zkoss.zul.Label labelCariTab = new org.zkoss.zul.Label("Cari:");
					labelCariTab.setParent(hboxCariPagingTab);

					final org.zkoss.zul.Textbox cariTab = new org.zkoss.zul.Textbox(
							kataCariItemBiayaPerTab.containsKey(index) ? kataCariItemBiayaPerTab.get(index) : "");
					cariTab.setWidth("200px");
					cariTab.setParent(hboxCariPagingTab);
					final EventListener jalankanPencarianItemBiaya = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							kataCariItemBiayaPerTab.put(index,
									cariTab.getValue() == null ? "" : cariTab.getValue().trim().toLowerCase());
							// Kata pencarian baru -> kembali ke halaman 1 KHUSUS tab ini saja.
							halamanItemBiayaPerTab.put(index, 0);
							getThis().onEvent(arg0);
						}
					};
					cariTab.addEventListener("onChange", jalankanPencarianItemBiaya);
					cariTab.addEventListener("onOK", jalankanPencarianItemBiaya);

					MyToolbarbuttonConfig tombolCariItemBiaya = new MyToolbarbuttonConfig("Cari",
							"/img/svg/search.svg");
					tombolCariItemBiaya.setTooltiptext("Cari item biaya pada angsuran ke-" + index);
					tombolCariItemBiaya.addEventListener("onClick", jalankanPencarianItemBiaya);
					tombolCariItemBiaya.setParent(hboxCariPagingTab);

					final org.zkoss.zul.Checkbox hanyaTerpilihCheckbox = new org.zkoss.zul.Checkbox(
							"Tampilkan hanya yg dipilih");
					hanyaTerpilihCheckbox.setChecked(hanyaTerpilihTab);
					hanyaTerpilihCheckbox.setParent(hboxCariPagingTab);
					hanyaTerpilihCheckbox.addEventListener("onCheck", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							hanyaTampilkanTerpilihPerTab.put(index, hanyaTerpilihCheckbox.isChecked());
							// Filter baru -> kembali ke halaman 1 KHUSUS tab ini saja.
							halamanItemBiayaPerTab.put(index, 0);
							getThis().onEvent(arg0);
						}
					});

					final org.zkoss.zul.Paging pagingTab = new org.zkoss.zul.Paging();
					pagingTab.setPageSize(10);
					pagingTab.setTotalSize(itemBiayaCocok.size());
					pagingTab.setActivePage(halamanTab);
					pagingTab.setParent(hboxCariPagingTab);
					pagingTab.addEventListener("onPaging", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							halamanItemBiayaPerTab.put(index, pagingTab.getActivePage());
							getThis().onEvent(arg0);
						}
					});

					MyGrid vboxSkala = new MyGrid();
					vboxSkala.setWidth("100%");
					if (jml == 1) {
						// Satu kali bayar: tampilkan langsung tanpa pembungkus tab -- TAPI rowJumlah
						// adalah baris ber-setSpans("2") (1-sel-penuh, lihat deklarasinya di atas),
						// yang HANYA boleh diisi SATU anak. Menaruh hboxCariPagingTab & vboxSkala
						// sebagai DUA anak terpisah persis bug yang sama dgn "Cari Item Biaya" yang
						// sebelumnya tak tampil -- jadi keduanya dibungkus SATU Div dulu di sini.
						org.zkoss.zul.Div wrapperSatuKali = new org.zkoss.zul.Div();
						wrapperSatuKali.setWidth("100%");
						wrapperSatuKali.setParent(rowJumlah);
						hboxCariPagingTab.setParent(wrapperSatuKali);
						vboxSkala.setParent(wrapperSatuKali);
					} else {
						// Banyak cicilan: tombol "Ke-N" (button group, bukan Tab bawaan ZK). Konten
						// (grid item biaya) dibangun EAGER di loop ini untuk SEMUA panel, sama seperti
						// sebelumnya -- hanya kerangka tombol+panelnya kini didelegasikan ke kelas
						// reusable MyButtonTabbox.
						org.zkoss.zul.Div panelKe = tabboxKe.tambahTab(index, "Ke-" + index);
						hboxCariPagingTab.setParent(panelKe);

						// Tinggi DEFINITE (px) pada grid -> body grid SELALU ter-render walau panelnya
						// sedang disembunyikan (setVisible(false)) atau rantai flex/height:100% tak
						// resolve. Dihitung dari MAKSIMAL 10 baris (page size); bila konten lebih
						// tinggi, grid ber-scroll sendiri (anti-BLANK, tak bergantung pengukuran parent).
						vboxSkala.setHeight((tinggiPanelItemBiayaPx - 45) + "px");
						// EAGER: grid di-parent LANGSUNG ke panel-nya untuk SEMUA "Ke-N" saat itu juga --
						// panel yang sedang tidak aktif tetap AMAN karena tinggi grid sudah DEFINITE (px),
						// bukan bergantung pengukuran panel yang sedang disembunyikan (setVisible(false)
						// pada Div TIDAK mengukur 0 seperti tabpanel tersembunyi, karena kita sendiri yang
						// mengendalikan tampil/sembunyi -- bukan lazy-mount seperti Tabbox bawaan ZK).
						vboxSkala.setParent(panelKe);
					}

					Columns columns = new Columns();
					columns.setParent(vboxSkala);

					MyColumnConfig column = new MyColumnConfig("Item Biaya");
					column.setParent(columns);

					if (gunakanBiayaDefault.isChecked() || khususBuatMahasiswaTertentu.isChecked()) {
						column.setWidth("35%");
						column = new MyColumnConfig("Nominal Tagihan");
						column.setParent(columns);

						column = new MyColumnConfig("Tgl Tagihan");
						column.setParent(columns);

						column = new MyColumnConfig("Tgl Deadline");
						column.setParent(columns);

						column = new MyColumnConfig("Keterangan Tagihan");
						column.setParent(columns);
					}

					Rows rowsSkala = new Rows();
					rowsSkala.setParent(vboxSkala);

					for (final ItemBiaya itemBiaya : itemBiayaTab) {

						MyFormRow rowSkala = new MyFormRow();
						rowSkala.setStyle("border:0px;background: transparent;");
						rowSkala.setParent(rowsSkala);

						DetailSettingBiaya detailSettingBiayaTemp = selectedItemBiayaIndex.get(index)
								.get(itemBiaya.getId());
						if (detailSettingBiayaTemp == null) {
							detailSettingBiayaTemp = new DetailSettingBiaya();
						}
						detailSettingBiayaTemp.setBayarKe(index);
						detailSettingBiayaTemp.setItemBiaya(itemBiaya);
						final DetailSettingBiaya detailSettingBiaya = detailSettingBiayaTemp;

						final MyDoublebox defaultTagihan = new MyDoublebox(detailSettingBiaya.getDefaultBiaya());
						final MyDatebox defaultTanggalTagihan = new MyDatebox(
								detailSettingBiaya.getDefaultTanggalTagihan());
						defaultTanggalTagihan.setFormat(Common.dateFormat.get().toPattern());

						final MyDatebox defaultTanggalDeadline = new MyDatebox(
								detailSettingBiaya.getDefaultTanggalDeadline());
						defaultTanggalDeadline.setFormat(Common.dateFormat1.get().toPattern());

						final MyTextbox defaultKeterangan = new MyTextbox(detailSettingBiaya.getDefaultKeterangan());

						final Checkbox checkbox = new Checkbox(itemBiaya.getNama());
						checkbox.setAttribute("detailSettingBiaya", detailSettingBiaya);
						checkbox.setParent(rowSkala);
						checkbox.setChecked(selectedItemBiayaIndex.get(index).containsKey(itemBiaya.getId()));
						checkbox.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								defaultTagihan.setDisabled(!checkbox.isChecked());
								defaultTanggalTagihan.setDisabled(!checkbox.isChecked());
								defaultTanggalDeadline.setDisabled(!checkbox.isChecked());
								defaultKeterangan.setDisabled(!checkbox.isChecked());
								if (checkbox.isChecked()) {
									selectedItemBiayaIndex.get(index).put(itemBiaya.getId(), detailSettingBiaya);
								} else {
									selectedItemBiayaIndex.get(index).remove(itemBiaya.getId());
								}

								if (perProdi) {
									getThis().onEvent(arg0);
								}
							}
						});

						if (!perProdi) {

							gunakanBiayaDefault.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									defaultTagihan.setDisabled(!checkbox.isChecked());
									defaultTanggalTagihan.setDisabled(!checkbox.isChecked());
									defaultTanggalDeadline.setDisabled(!checkbox.isChecked());
								}
							});

							if (gunakanBiayaDefault.isChecked() || khususBuatMahasiswaTertentu.isChecked()) {
								defaultTagihan.setParent(rowSkala);
								defaultTagihan.setWidth("90%");
								defaultTagihan.setDisabled(!checkbox.isChecked());
								defaultTagihan.addEventListener("onChange", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										if (checkbox.isChecked()) {
											detailSettingBiaya.setDefaultBiaya(defaultTagihan.getValue());
											selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
													detailSettingBiaya);
										}
									}
								});

								defaultTanggalTagihan.setParent(rowSkala);
								defaultTanggalTagihan.setCols(3);
								defaultTanggalTagihan.setDisabled(!checkbox.isChecked());
								defaultTanggalTagihan.addEventListener("onChange", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										if (checkbox.isChecked()) {
											detailSettingBiaya
													.setDefaultTanggalTagihan(defaultTanggalTagihan.getValue());
											selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
													detailSettingBiaya);
										}
									}
								});

								defaultTanggalDeadline.setParent(rowSkala);
								defaultTanggalDeadline.setCols(3);
								defaultTanggalDeadline.setDisabled(!checkbox.isChecked());
								defaultTanggalDeadline.addEventListener("onChange", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										if (checkbox.isChecked()) {
											detailSettingBiaya
													.setDefaultTanggalDeadline(defaultTanggalDeadline.getValue());
											selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
													detailSettingBiaya);
										}
									}
								});

								defaultKeterangan.setParent(rowSkala);
								defaultKeterangan.setWidth("90%");
								defaultKeterangan.setDisabled(!checkbox.isChecked());
								defaultKeterangan.addEventListener("onChange", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										if (checkbox.isChecked()) {
											detailSettingBiaya.setDefaultKeterangan(defaultKeterangan.getValue());
											selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
													detailSettingBiaya);
										}
									}
								});
							}

						} else if (checkbox.isChecked()) {

							MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Download",
									"/img/svg/download.svg");
							search.addEventListener("onClick", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {

									XSSFWorkbook workbook = new XSSFWorkbook();

									XSSFSheet sheet = workbook.createSheet("Nilai");
									sheet.setDefaultColumnWidth(18);

									XSSFRow rowhead = sheet.createRow((short) 0);

									rowhead.createCell(0).setCellValue("ID Prodi");
									rowhead.createCell(1).setCellValue("Nama Prodi");
									rowhead.createCell(2).setCellValue("Nilai");
									rowhead.createCell(3).setCellValue("Default Tanggal Tagihan");
									rowhead.createCell(4).setCellValue("Default Tanggal Deadline");
									rowhead.createCell(5).setCellValue("Default Keterangan Tagihan");

									JSONObject jsonObject = new JSONObject(detailSettingBiaya.getBiayaPerProdi());

									int rowIndex = 1;
									for (Jurusan jurusan : jurusans) {
										XSSFRow row = sheet.createRow(rowIndex);

										XSSFCell cell = row.createCell(0);
										cell.setCellValue(jurusan.getId());

										cell = row.createCell(1);
										cell.setCellValue(jurusan.getNama());

										Double biaya = 0.0;
										Date tgl = null;
										Date deadline = null;
										String ket = "";

										try {
											biaya = jsonObject.isNull("b_" + jurusan.getId()) ? 0.0
													: jsonObject.getDouble("b_" + jurusan.getId());
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1130");
											// TODO: handle exception
										}

										try {
											tgl = jsonObject.isNull("t_" + jurusan.getId())
													|| jsonObject.get("t_" + jurusan.getId()).equals("") ? null
															: Common.dateFormat.get()
																	.parse(jsonObject.get("t_" + jurusan.getId()) + "");
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1139");
											// TODO: handle exception
										}

										try {
											deadline = jsonObject.isNull("d_" + jurusan.getId())
													|| jsonObject.get("d_" + jurusan.getId()).equals("") ? null
															: Common.dateFormat.get()
																	.parse(jsonObject.get("d_" + jurusan.getId()) + "");
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1148");
											// TODO: handle exception
										}

										try {
											ket = jsonObject.isNull("ket_" + jurusan.getId()) ? ""
													: jsonObject.get("ket_" + jurusan.getId()) + "";
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1155");
											// TODO: handle exception
										}

										cell = row.createCell(2);
										cell.setCellValue(biaya);

										cell = row.createCell(3);
										cell.setCellValue(tgl == null ? "" : Common.databaseDateFormat.get().format(tgl));

										cell = row.createCell(4);
										cell.setCellValue(
												deadline == null ? "" : Common.databaseDateFormat.get().format(deadline));

										cell = row.createCell(5);
										cell.setCellValue(ket);

										rowIndex++;
									}

									Common.setStyled(sheet);
									String filename = Sessions.getCurrent().getWebApp()
											.getRealPath("/tmp/tagihan_per_prodi_"
													+ URLEncoder.encode(Common.datetimeFormat2s
															.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8")
													+ ".xlsx");
									try {
										FileOutputStream fileOut = new FileOutputStream(filename);
										workbook.write(fileOut);
										fileOut.close();
									} catch (IOException e) {
										// TODO Auto-generated catch block
										Common.tampilErrorJikaAdmin(e);
									}

									Filedownload.save(new FileInputStream(filename),
											"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
											"tagihan_per_prodi.xlsx");

								}
							});
							search.setParent(rowSkala);

							final MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(
									"Upload " + Common.ukuranLabelFileUpload(), "/img/upload.png");
							button.setParent(rowSkala);
							button.setUpload(Common.ukuranFileUpload());
							button.addEventListener("onUpload", new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									// TODO Auto-generated method stub

									UploadEvent uploadEvent = (UploadEvent) event;
									Media media = uploadEvent.getMedia();
									if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media))
										return;
									if (media.getName().toLowerCase().endsWith("xlsx")) {
										InputStream inputStream = media.getStreamData();
										File file = new File(Sessions.getCurrent().getWebApp()
												.getRealPath("/temp/" + media.getName()));
										file.getParentFile().mkdirs();
										FileOutputStream fileOutputStream = new FileOutputStream(file);
										int c;
										while ((c = inputStream.read()) != -1) {
											fileOutputStream.write(c);
										}
										fileOutputStream.close();
										inputStream.close();

										final ais.common.UploadReportHelper report = new ais.common.UploadReportHelper("Upload Biaya Per Prodi");
										XSSFWorkbook workbookUpload;
										try {
											workbookUpload = new XSSFWorkbook(file.getAbsolutePath());

											XSSFSheet sheetUpload = workbookUpload.getSheetAt(0);
											int size = sheetUpload.getLastRowNum() + 1;
											MyJSONObject jsonObject = new MyJSONObject(
													detailSettingBiaya.getBiayaPerProdi());
											for (int i = 1; i < size; i++) {
												try {
													Jurusan jurusan = (Jurusan) Common
															.getSheetContentAsObject(sheetUpload, 0, i, Jurusan.class);
													if (jurusan == null) {
														report.gagal(i, "Baris " + i, "Prodi tidak ditemukan.", "Pastikan kode mahasiswa/siswa dan komponen biaya valid.");
														continue;
													}

													Double nilai = Common.getSheetContentAsDouble(sheetUpload, 2, i);
													Date mulai = Common.getSheetContentAsDateDatabase(sheetUpload, 3,
															i);
													Date deadline = Common.getSheetContentAsDateDatabase(sheetUpload, 4,
															i);
													String ket = Common.getSheetContentAsString(sheetUpload, 5, i);

													jsonObject.put("b_" + jurusan.getId(), nilai);
													jsonObject.put("t_" + jurusan.getId(),
															mulai == null ? "" : Common.dateFormat.get().format(mulai));
													jsonObject.put("d_" + jurusan.getId(),
															deadline == null ? "" : Common.dateFormat.get().format(deadline));

													jsonObject.put("ket_" + jurusan.getId(), ket);
													report.sukses(i, jurusan.getNama() + " - " + itemBiaya.getNama(), "Nilai: " + nilai);

												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/SetingBiayaAction.java:1257");
													report.gagal(i, "Baris " + i, e, "Pastikan kode mahasiswa/siswa dan komponen biaya valid.");
												}
											}
											detailSettingBiaya.setBiayaPerProdi(jsonObject.toString());
											selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
													detailSettingBiaya);
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/SetingBiayaAction.java:1264");
										}
										try { Filedownload.save(report.simpanLaporan(), "text/plain"); } catch (Exception eR) { ais.common.ErrorAuditUtil.record(eR, "auto-audit(empty-catch) SetingBiayaAction laporan"); }
										getThis().onEvent(event);
									} else {
										MyMessageboxConfig.show(
												"File yang anda upload harus ber-format Excel Open XML Spreadsheet (xlsx). Jika masih menggunakan format lain, buka file excel tersebut, kemudian Save As Excel Open XML Spreadsheet (xlsx). "
														+ media,
												"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR);
									}
								}
							});

							final MyJSONObject jsonObject = new MyJSONObject(detailSettingBiaya.getBiayaPerProdi());
							for (final Jurusan jurusan : jurusans) {
								rowSkala = new MyFormRow();
								rowSkala.setStyle("border:0px;background: transparent;");
								rowSkala.setParent(rowsSkala);

								new Label("    " + jurusan.getNama()).setParent(rowSkala);

								Double biaya = 0.0;
								Date tgl = null;
								Date deadline = null;
								String ket = "";

								try {
									biaya = jsonObject.isNull("b_" + jurusan.getId()) ? 0.0
											: jsonObject.getDouble("b_" + jurusan.getId());
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1292");
									// TODO: handle exception
								}

								try {
									tgl = jsonObject.isNull("t_" + jurusan.getId())
											|| jsonObject.get("t_" + jurusan.getId()).equals("") ? null
													: Common.dateFormat.get()
															.parse(jsonObject.get("t_" + jurusan.getId()) + "");
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1301");
									// TODO: handle exception
								}

								try {
									deadline = jsonObject.isNull("d_" + jurusan.getId())
											|| jsonObject.get("d_" + jurusan.getId()).equals("") ? null
													: Common.dateFormat.get()
															.parse(jsonObject.get("d_" + jurusan.getId()) + "");
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1310");
									// TODO: handle exception
								}

								try {
									ket = jsonObject.isNull("ket_" + jurusan.getId()) ? ""
											: jsonObject.get("ket_" + jurusan.getId()) + "";
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1317");
									// TODO: handle exception
								}

								final MyDoublebox defaultTagihanJurusan = new MyDoublebox(biaya);
								final MyDatebox defaultTanggalJurusan = new MyDatebox(tgl);
								defaultTanggalJurusan.setFormat(Common.dateFormat.get().toPattern());
								final MyDatebox tanggalDeadline = new MyDatebox(deadline);
								tanggalDeadline.setFormat(Common.dateFormat1.get().toPattern());
								final MyTextbox defaultKeteranganJurusan = new MyTextbox(ket);

								if (gunakanBiayaDefault.isChecked() || khususBuatMahasiswaTertentu.isChecked()) {
									defaultTagihanJurusan.setParent(rowSkala);
									defaultTagihanJurusan.setWidth("90%");
									defaultTagihanJurusan.setDisabled(!checkbox.isChecked());
									defaultTagihanJurusan.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											if (checkbox.isChecked()) {
												jsonObject.put("b_" + jurusan.getId(),
														defaultTagihanJurusan.getValue());
												detailSettingBiaya.setBiayaPerProdi(jsonObject.toString());
												selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
														detailSettingBiaya);
											}
										}
									});

									defaultTanggalJurusan.setParent(rowSkala);
									defaultTanggalJurusan.setCols(3);
									defaultTanggalJurusan.setDisabled(!checkbox.isChecked());
									defaultTanggalJurusan.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (checkbox.isChecked()) {
												jsonObject.put("t_" + jurusan.getId(),
														defaultTanggalJurusan.getValue() == null ? ""
																: Common.dateFormat
																		.get().format(defaultTanggalJurusan.getValue()));
												detailSettingBiaya.setBiayaPerProdi(jsonObject.toString());
												selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
														detailSettingBiaya);
											}
										}
									});

									tanggalDeadline.setParent(rowSkala);
									tanggalDeadline.setCols(3);
									tanggalDeadline.setDisabled(!checkbox.isChecked());
									tanggalDeadline.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (checkbox.isChecked()) {
												jsonObject.put("d_" + jurusan.getId(),
														tanggalDeadline.getValue() == null ? ""
																: Common.dateFormat.get().format(tanggalDeadline.getValue()));
												detailSettingBiaya.setBiayaPerProdi(jsonObject.toString());
												selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
														detailSettingBiaya);
											}
										}
									});

									defaultKeteranganJurusan.setParent(rowSkala);
									defaultKeteranganJurusan.setWidth("90%");
									defaultKeteranganJurusan.setDisabled(!checkbox.isChecked());
									defaultKeteranganJurusan.addEventListener("onChange", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {

											if (checkbox.isChecked()) {
												jsonObject.put("ket_" + jurusan.getId(),
														defaultKeteranganJurusan.getValue());
												detailSettingBiaya.setBiayaPerProdi(jsonObject.toString());
												selectedItemBiayaIndex.get(index).put(itemBiaya.getId(),
														detailSettingBiaya);
											}
										}
									});
								}
							}

						}
					}
				}

				// Konten SEMUA tab sudah eager-parented di loop atas -- di sini tinggal memulihkan
				// tab mana yang tadinya aktif secara VISUAL (biar tidak "melompat" balik ke Ke-1
				// setiap kali user mengubah checkbox/field lain di luar Jumlah Pembayaran), dan
				// terus mencatatnya tiap kali user berpindah tab agar siap dipulihkan lagi nanti.
				if (jml > 1) {
					tabboxKe.pulihkanSeleksi(jml);
				} else {
					tabAktifTerakhir[0] = 1;
				}
			}
		};

		jumlahPembayaran.addEventListener("onChange", eventListenerJumlah);
		jumlahPembayaran.addEventListener("onOK", eventListenerJumlah);
		tampilkanPerProdi.addEventListener("onClick", eventListenerJumlah);
		gunakanBiayaDefault.addEventListener("onClick", eventListenerJumlah);
		khususBuatMahasiswaTertentu.addEventListener("onClick", eventListenerJumlah);
		jenjang.addEventListener("onChange", eventListenerJumlah);
		jurusan.addEventListener("onChange", eventListenerJumlah);

		// Pencarian & paging item biaya kini sepenuhnya per tab "Ke-N" -- kotak pencarian
		// dan widget Paging masing-masing tab sudah dipasangi listener-nya sendiri di dalam
		// loop "Ke-N" (lihat komentar "PERMINTAAN: pencarian & paging terpisah PER tombol
		// Ke-N"), tidak ada lagi listener global untuk itu di sini.
		eventListenerJumlah.onEvent(null);

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
				try {
					if (onSave(event)) {
						onSearchDefault(null);
						addWindow.setVisible(false);
					}
				} catch (Exception e) {
					// PERMINTAAN: setiap error saat proses Simpan WAJIB diinformasikan ke pengguna
					// secara jelas (bukan dibiarkan lolos jadi halaman error ZK generik yang
					// membingungkan), lengkap dengan saran langkah dan eskalasi ke admin/pengembang
					// (wajib lampirkan screenshot) -- lihat PesanFormalHelper.eskalasi().
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit src/ais/action/master/SetingBiayaAction.java:onClick-Simpan");
					PesanFormalHelper.tampilkanGagalException("penyimpanan data Setting Biaya", e,
							new String[] {
									"Periksa kembali seluruh isian pada form ini (Jenis Kegiatan, Jumlah Pembayaran, item biaya yang dicentang, dsb).",
									"Pastikan koneksi jaringan Bapak/Ibu stabil, lalu ulangi proses Simpan.",
									"Bila data sudah benar namun kesalahan tetap terjadi, kemungkinan ada kendala pada sistem/basis data."
							});
				}
			}
		});
		save.setParent(toolbar);

		borderlayout.setParent(addWindow);

	}

	/**
	 * Memvalidasi lalu menyimpan data setting biaya dari form: menolak bila jenis kegiatan belum
	 * dipilih, atau kombinasi kriteria yang sama sudah terdaftar ({@link #checkSettingBiaya()}); jika
	 * lolos menyimpan/memperbarui entitas {@link SettingBiaya} dengan seluruh field kriteria, lalu
	 * menyinkronkan daftar {@link DetailSettingBiaya} per {@code bayarKe} sesuai pilihan pada grid
	 * item biaya — item lama yang tidak lagi dipilih dihapus KECUALI masih dipakai oleh
	 * {@link DetailBiaya} (tagihan mahasiswa nyata) yang sudah ada, dalam hal ini item tersebut
	 * dipertahankan dan pengguna diberi tahu lewat pesan peringatan (mencegah pelanggaran foreign
	 * key ke tagihan yang sudah terbentuk).
	 *
	 * @param event event ZK pemicu penyimpanan (tombol simpan)
	 * @return {@code true} bila berhasil disimpan, {@code false} bila validasi gagal
	 * @throws Exception diteruskan apa adanya dari kegagalan Hibernate saat menyimpan
	 */
	public boolean onSave(Event event) throws Exception {
		if (jenisKegiatan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Kegiatan",
					"Kolom Jenis Kegiatan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Kegiatan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		String nilaiPengecualian = pengecualianMahasiswa == null || pengecualianMahasiswa.getValue() == null
				? "" : pengecualianMahasiswa.getValue().trim();
		String daftarPengecualian = terdapatPengecualianMahasiswa != null
				&& terdapatPengecualianMahasiswa.isChecked() ? nilaiPengecualian : "";
		try {
			SettingBiaya.validasiFormatPengecualianMahasiswa(daftarPengecualian);
		} catch (IllegalArgumentException e) {
			PesanFormalHelper.tampilkanGagal("penyimpanan pengecualian NIM", e.getMessage(),
					new String[] { "Gunakan NIM,NIM untuk semua semester.",
							"Gunakan NIM:SMT_MULAI:SMT_SAMPAI;NIM:SMT_MULAI:SMT_SAMPAI untuk rentang semester." });
			return false;
		}
		if (daftarPengecualian.length() == 0 && terdapatPengecualianMahasiswa != null) {
			terdapatPengecualianMahasiswa.setChecked(false);
		}

		boolean i = checkSettingBiaya();
		if (i) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Setting Biaya",
					"Setting Biaya sudah terdaftar sebelumnya di database, sehingga tidak dapat disimpan kembali untuk menghindari duplikasi data.",
					new String[] {
							"Gunakan Setting Biaya yang berbeda dari data yang sudah ada.",
							"Periksa kembali daftar data yang sudah tersimpan apabila Bapak/Ibu ragu."
					});
			return false;
		}

		SettingBiayaDao settingBiayaDao = DaoFactory.getInstance().getSettingBiayaDao();
		if (settingBiaya.getId() != null) {
			settingBiaya = settingBiayaDao.load(settingBiaya.getId());
		}
		settingBiaya.setAfiliasiCalonMahasiswa(
				(AfiliasiCalonMahasiswa) afiliasiCalonMahasiswa.getAttribute("afiliasiCalonMahasiswa"));
		settingBiaya
				.setAngkatan(angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue() == null ? null
						: Integer.valueOf(angkatan.getSelectedItem().getValue().toString()));
		settingBiaya.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));
		settingBiaya.setJenisKegiatan((JenisKegiatan) (jenisKegiatan.getSelectedItem() == null ? null
				: jenisKegiatan.getSelectedItem().getValue()));
		settingBiaya.setGunakanBiayaDefault(gunakanBiayaDefault.isChecked());
		settingBiaya.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));
		settingBiaya.setProgram(
				(String) (program.getSelectedItem() == null ? null : program.getSelectedItem().getValue()));
		settingBiaya.setJenisSeleksi((JenisSeleksi) (jenisSeleksi.getSelectedItem() == null ? null
				: jenisSeleksi.getSelectedItem().getValue()));

		settingBiaya
				.setGelombangPendaftaran((GelombangPendaftaran) (gelombangPendaftaran.getSelectedItem() == null ? null
						: gelombangPendaftaran.getSelectedItem().getValue()));
		settingBiaya.setPaket((Paket) (paket.getSelectedItem() == null ? null : paket.getSelectedItem().getValue()));

		settingBiaya.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null ? null : jurusan.getSelectedItem().getValue()));

		settingBiaya.setMinSmt(minSmt.getValue());
		settingBiaya.setMaxSmt(maxSmt.getValue());
		settingBiaya.setSmtIkutiSettinganDisini(smtIkutiSettinganDisini.isChecked());
		settingBiaya.setKhususBuatMahasiswaTertentu(khususBuatMahasiswaTertentu.isChecked());
		settingBiaya.setBatasiMahasiswaTertentu(batasiMahasiswaTertentu.isChecked());
		settingBiaya.setPengecualianMahasiswa(daftarPengecualian);
		settingBiaya.setPrioritas(prioritas.getValue());
		settingBiaya.setJumlahPembayaran(jumlahPembayaran.getValue());
		settingBiaya.setTampilkanPerProdi(tampilkanPerProdi.isChecked());
		settingBiaya.setStatusMahasiswa((StatusMahasiswa) (statusMahasiswa.getSelectedItem() == null ? null
				: statusMahasiswa.getSelectedItem().getValue()));
		settingBiaya
				.setKelamin((String) (kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue()));

		settingBiaya.setTahunAkademik(
				(String) (tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? null
						: tahunAkademik.getSelectedItem().getValue()));
		settingBiaya.setSemester(
				(String) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(settingBiaya);

		Session session = HibernateUtil.currentSession();

		for (Integer index : selectedItemBiayaIndex.keySet()) {
			List<Long> ids = new ArrayList<Long>();
			for (DetailSettingBiaya detailSettingBiaya : selectedItemBiayaIndex.get(index).values()) {
				if (detailSettingBiaya.getId() != null) {
					ids.add(detailSettingBiaya.getId());
				}
			}

			List<DetailSettingBiaya> detailSettingBiayas = ConstantValues
					.simpleList(session.createCriteria(DetailSettingBiaya.class).add(Restrictions.eq("bayarKe", index))
							.add(ids.isEmpty() ? Restrictions.sqlRestriction("true")
									: Restrictions.not(Restrictions.in("id", ids)))
							.add(Restrictions.eq("settingBiaya", settingBiaya)), DetailSettingBiaya.class);
			for (DetailSettingBiaya d : detailSettingBiayas) {
				/*
				 * DetailBiaya adalah histori/tagihan mahasiswa yang harus dipertahankan,
				 * tetapi FK-nya ke pilihan Setting Biaya yang telah dilepas tidak boleh
				 * membuat item itu tetap dianggap aktif. Putuskan relasi nullable tersebut
				 * sebelum menghapus DetailSettingBiaya. Nilai dan histori pembayaran tetap ada.
				 */
				List<DetailBiaya> detailBiayaTerkait = ConstantValues.simpleList(
						session.createCriteria(DetailBiaya.class).add(Restrictions.eq("detailSettingBiaya", d)),
						DetailBiaya.class);
				for (DetailBiaya detailBiayaTerkaitItem : detailBiayaTerkait) {
					detailBiayaTerkaitItem.setDetailSettingBiaya(null);
				}
				session.flush();
				session.delete(d);
				session.flush();
			}

			for (DetailSettingBiaya detailSettingBiaya : selectedItemBiayaIndex.get(index).values()) {
				detailSettingBiaya.setSettingBiaya(settingBiaya);
				session.saveOrUpdate(detailSettingBiaya);
				session.flush();
			}
		}

		if (eventListener != null) {
			eventListener.onEvent(new Event("", add, settingBiaya));
		}

		return true;
	}

	/**
	 * Menyusun kriteria pencarian {@link SettingBiaya} sesuai filter layar (jenis kegiatan ketat;
	 * jenjang/jurusan/status awal/program/angkatan longgar — nilai kosong pada baris data tetap ikut
	 * cocok, konsisten dengan cara penerapan biaya di method statis {@code get*}), ditambah
	 * pencarian teks bebas pada gelombang pendaftaran dan paket/jenis seleksi (via {@code LEFT_JOIN}
	 * agar baris tanpa gelombang/paket tetap tampil), diurutkan berdasarkan prioritas lalu id
	 * terbaru bila diminta.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SettingBiaya.class);
		if (order)
			criteria.addOrder(Order.asc("prioritas")).addOrder(Order.desc("id"));
		// CATATAN (perbaikan "tagihan sudah dibuat tapi tak muncul di setup"):
		// Untuk field pengelompokan (jenjang/jurusan/statusAwal/program/angkatan/gelombang),
		// NULL berarti "berlaku untuk SEMUA". Filter memakai "isNull OR eq" (bukan eq ketat) agar
		// setting UMUM (mis. Pendaftaran Calon prodi Ekonomi Syariah tanpa gelombang tertentu) tetap
		// muncul saat pengguna menyaring nilai spesifik. Ini KONSISTEN dengan query penerapan biaya
		// (getDefaultSettingBiaya) yang sudah memakai "isNull OR eq". Perubahan hanya MEMPERLUAS hasil
		// (tak pernah menyembunyikan baris yang sebelumnya tampil). jenisKegiatan tetap ketat (itu
		// jenis pembayaran, wajib ada).
		criteria.add(searchJenisKegiatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
				: (((JenisKegiatan) (searchJenisKegiatan.getSelectedItem().getValue())).getId() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("jenisKegiatan",
								(JenisKegiatan) searchJenisKegiatan.getSelectedItem().getValue())))

				.add(searchjenjang.getSelectedItem() == null || searchjenjang.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: (((Jenjang) (searchjenjang.getSelectedItem().getValue())).getId() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jenjang"),
										Restrictions.eq("jenjang", (Jenjang) searchjenjang.getSelectedItem().getValue()))))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: (((Jurusan) (searchjurusan.getSelectedItem().getValue())).getId() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("jurusan"),
										Restrictions.eq("jurusan", (Jurusan) searchjurusan.getSelectedItem().getValue()))))

				.add(searchstatusAwalMahasiswa.getSelectedItem() == null
						|| searchstatusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: (((StatusAwalMahasiswa) (searchstatusAwalMahasiswa.getSelectedItem().getValue()))
										.getId() == null
												? Restrictions.sqlRestriction("1=1")
												: Restrictions.or(Restrictions.isNull("statusAwalMahasiswa"),
														Restrictions.eq("statusAwalMahasiswa",
																(StatusAwalMahasiswa) searchstatusAwalMahasiswa
																		.getSelectedItem().getValue()))))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.isNull("program"),
								Restrictions.eq("program", (String) searchprogram.getSelectedItem().getValue())))

				.add(searchAngkatan.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: (Integer.parseInt(searchAngkatan.getSelectedItem().getValue().toString()) == 0
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(Restrictions.isNull("angkatan"),
										Restrictions.eq("angkatan", searchAngkatan.getSelectedItem().getValue()))));

		if (!searchGelombang.getValue().trim().isEmpty()) {
			// LEFT_JOIN + ilike pada tabel gelombang akan MEMBUANG baris ber-gelombang NULL (berlaku
			// semua gelombang). Sertakan isNull agar setting umum tetap muncul saat mencari gelombang.
			criteria.createAlias("gelombangPendaftaran", "gelombangPendaftaran", Criteria.LEFT_JOIN)
					.add(Restrictions.or(Restrictions.isNull("gelombangPendaftaran.id"),
							Restrictions.or(
									Restrictions.ilike("gelombangPendaftaran.nama", searchGelombang.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("gelombangPendaftaran.kode", searchGelombang.getValue().trim(),
											MatchMode.ANYWHERE))));
		}

		if (!searchSeleksi.getValue().trim().isEmpty()) {
			// Perbaikan: sebelumnya block ini keliru menyaring memakai nilai searchGelombang (salin-tempel).
			// Kini memakai searchSeleksi. Sertakan juga isNull agar setting umum (tanpa paket/jenis seleksi
			// tertentu) tetap muncul saat mencari paket/jenis seleksi.
			String cariSeleksi = searchSeleksi.getValue().trim();
			criteria.createAlias("jenisSeleksi", "jenisSeleksi", Criteria.LEFT_JOIN)
					.createAlias("paket", "paket", Criteria.LEFT_JOIN)
					.add(Restrictions.or(
							Restrictions.and(Restrictions.isNull("jenisSeleksi.id"), Restrictions.isNull("paket.id")),
							Restrictions.or(
									Restrictions.ilike("paket.nama", cariSeleksi, MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("jenisSeleksi.nama", cariSeleksi, MatchMode.ANYWHERE),
											Restrictions.ilike("jenisSeleksi.kode", cariSeleksi, MatchMode.ANYWHERE)))));
		}

		return criteria;
	}

	
	/** Menjalankan pencarian setting biaya sesuai kriteria dan halaman paging aktif, lalu merender hasilnya ke grid; no-op bila komponen pencarian belum siap. */
	public void onSearchDefault(Event event) {
		if (searchJenisKegiatan == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);

		@SuppressWarnings("unchecked")
		List<SettingBiaya> settingBiaya = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(settingBiaya);
		grid.setRowRenderer(new SettingBiayaRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Memeriksa apakah kombinasi kriteria pada form (angkatan, mode nilai khusus mahasiswa,
	 * pembatasan mahasiswa untuk tagihan bulanan,
	 * tahun-semester {@code ta} yang disusun dari tahun akademik + semester terpilih, rentang
	 * semester min/maks, dan opsi ikut-settingan-di-sini) sudah dipakai oleh baris
	 * {@link SettingBiaya} lain, untuk mencegah duplikasi aturan tagihan yang saling tumpang tindih.
	 *
	 * @return {@code true} bila kombinasi kriteria sudah terdaftar pada baris lain, {@code false} bila belum
	 */
	public Boolean checkSettingBiaya() {

		String tahunAkademik = (String) (this.tahunAkademik.getSelectedItem() == null
				|| this.tahunAkademik.getSelectedItem().getValue() == null ? ""
						: this.tahunAkademik.getSelectedItem().getValue());
		String semester = (String) (this.semester.getSelectedItem() == null
				|| this.semester.getSelectedItem().getValue() == null ? ""
						: this.semester.getSelectedItem().getValue());

		String id_smt = (tahunAkademik == null || tahunAkademik.trim().isEmpty() ? "0" : tahunAkademik.split("/")[0])
				+ (semester == null || semester.trim().isEmpty() ? "0"
						: semester.equals(Perkuliahan.GENAP) ? "2" : "1");
		Integer ta = 0;
		try {
			ta = Integer.parseInt(id_smt.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:1669");

		}

		boolean modeNilaiKhususMahasiswa = khususBuatMahasiswaTertentu.isChecked();
		boolean modeBatasiMahasiswaBulanan = batasiMahasiswaTertentu.isChecked();

		Integer settingBiayaCount;
		Session session = HibernateUtil.currentSession();
		settingBiayaCount = ((Number) session.createCriteria(SettingBiaya.class).setProjection(Projections.rowCount())
				.add(angkatan.getSelectedItem() == null || angkatan.getSelectedItem().getValue() == null
						? Restrictions.isNull("angkatan")
						: Restrictions.eq("angkatan",
								Integer.valueOf(angkatan.getSelectedItem().getValue().toString())))

				// Kedua kolom adalah mode yang berbeda dan wajib ikut menentukan duplikasi.
				// NULL pada data lama setara dengan false agar perbandingan tetap konsisten.
				.add(kriteriaBooleanNullSebagaiFalse("khususBuatMahasiswaTertentu", modeNilaiKhususMahasiswa))
				.add(kriteriaBooleanNullSebagaiFalse("batasiMahasiswaTertentu", modeBatasiMahasiswaBulanan))

				.add(Restrictions.ge("ta", ta))

				.add(minSmt.getValue() == null ? Restrictions.eq("minSmt", 0)
						: Restrictions.eq("minSmt", minSmt.getValue()))

				.add(maxSmt.getValue() == null ? Restrictions.eq("maxSmt", 30)
						: Restrictions.eq("maxSmt", maxSmt.getValue()))

				.add(smtIkutiSettinganDisini.isChecked() ? Restrictions.eq("smtIkutiSettinganDisini", Boolean.TRUE)
						: Restrictions.or(Restrictions.isNull("smtIkutiSettinganDisini"),
								Restrictions.eq("smtIkutiSettinganDisini", Boolean.FALSE)))

				.add(Restrictions.eq("jenisKegiatan", (JenisKegiatan) jenisKegiatan.getSelectedItem().getValue()))

				.add(afiliasiCalonMahasiswa.getAttribute("afiliasiCalonMahasiswa") == null
						? Restrictions.isNull("afiliasiCalonMahasiswa")
						: Restrictions.eq("afiliasiCalonMahasiswa",
								afiliasiCalonMahasiswa.getAttribute("afiliasiCalonMahasiswa")))

				.add(jenjang.getSelectedItem() == null || jenjang.getSelectedItem().getValue() == null
						? Restrictions.isNull("jenjang")
						: Restrictions.eq("jenjang", jenjang.getSelectedItem().getValue()))

				.add(statusAwalMahasiswa.getSelectedItem() == null
						|| statusAwalMahasiswa.getSelectedItem().getValue() == null
								? Restrictions.isNull("statusAwalMahasiswa")
								: Restrictions.eq("statusAwalMahasiswa",
										statusAwalMahasiswa.getSelectedItem().getValue()))

				.add(statusMahasiswa.getSelectedItem() == null || statusMahasiswa.getSelectedItem().getValue() == null
						? Restrictions.isNull("statusMahasiswa")
						: Restrictions.eq("statusMahasiswa", statusMahasiswa.getSelectedItem().getValue()))

				.add(program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? Restrictions.isNull("program")
						: Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(kelamin.getSelectedItem() == null || kelamin.getSelectedItem().getValue() == null
						? Restrictions.isNull("kelamin")
						: Restrictions.eq("kelamin", kelamin.getSelectedItem().getValue()))

				.add(jenisSeleksi.getSelectedItem() == null || jenisSeleksi.getSelectedItem().getValue() == null
						? Restrictions.isNull("jenisSeleksi")
						: Restrictions.eq("jenisSeleksi", jenisSeleksi.getSelectedItem().getValue()))

				.add(gelombangPendaftaran.getSelectedItem() == null
						|| gelombangPendaftaran.getSelectedItem().getValue() == null
								? Restrictions.isNull("gelombangPendaftaran")
								: Restrictions.eq("gelombangPendaftaran",
										gelombangPendaftaran.getSelectedItem().getValue()))

				.add(paket.getSelectedItem() == null || paket.getSelectedItem().getValue() == null
						? Restrictions.isNull("paket")
						: Restrictions.eq("paket", paket.getSelectedItem().getValue()))

				.add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? Restrictions.isNull("jurusan")
						: Restrictions.eq("jurusan", jurusan.getSelectedItem().getValue()))

				.add(this.settingBiaya.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.settingBiaya.getId()))
				.uniqueResult()).intValue();

		return !settingBiayaCount.equals(0);
	}

	/** Membandingkan kolom boolean dengan kompatibilitas data lama: {@code null} dianggap {@code false}. */
	private static Criterion kriteriaBooleanNullSebagaiFalse(String namaProperti, boolean nilai) {
		return nilai ? Restrictions.eq(namaProperti, Boolean.TRUE)
				: Restrictions.or(Restrictions.isNull(namaProperti),
						Restrictions.eq(namaProperti, Boolean.FALSE));
	}

	
	private static org.hibernate.criterion.Criterion kriteriaSemesterIkutSetting(JenisKegiatan jenisKegiatan,
			Integer semester) {
		int sem = (semester == null ? 0 : semester.intValue());
		int jkMin = (jenisKegiatan == null || jenisKegiatan.getMinSmt() == null) ? 0 : jenisKegiatan.getMinSmt().intValue();
		int jkMax = (jenisKegiatan == null || jenisKegiatan.getMaxSmt() == null) ? 30 : jenisKegiatan.getMaxSmt().intValue();
		boolean dalamRentangJenisKegiatan = (sem >= jkMin && sem <= jkMax);
		org.hibernate.criterion.Criterion rentangSettingIni = Restrictions.sqlRestriction(sem + " between minsmt and maxsmt");
		// flag true  -> rentang DIATUR DI SINI (rentang JenisKegiatan tidak berlaku);
		// flag false -> IKUT JenisKegiatan (semester wajib di dalam rentang JenisKegiatan).
		return Restrictions.or(
				Restrictions.and(Restrictions.eq("smtIkutiSettinganDisini", Boolean.TRUE), rentangSettingIni),
				Restrictions.and(
						Restrictions.or(Restrictions.isNull("smtIkutiSettinganDisini"),
								Restrictions.eq("smtIkutiSettinganDisini", Boolean.FALSE)),
						Restrictions.and(rentangSettingIni, dalamRentangJenisKegiatan
								? Restrictions.sqlRestriction("1=1") : Restrictions.sqlRestriction("1=0"))));
	}

	/**
	 * Satu pintu pemilihan induk SettingBiaya. Seluruh jalur item, detail reguler,
	 * detail default, dan pembayaran bulanan wajib memakai hasil yang sama agar
	 * setting prioritas rendah tidak tercampur kembali hanya karena ItemBiayanya sama.
	 */
	public static SettingBiaya getSettingBiayaTerpilih(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi,
			GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan, String program,
			String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta,
			String nimMahasiswa, boolean gunakanBiayaDefault) {
		Criteria criteria = session.createCriteria(SettingBiaya.class)
				.add(Restrictions.le("ta", ta))
				.add(Restrictions.or(Restrictions.isNull("khususBuatMahasiswaTertentu"),
						Restrictions.eq("khususBuatMahasiswaTertentu", false)))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan))
				.add(kriteriaSemesterIkutSetting(jenisKegiatan, semester));
		if (gunakanBiayaDefault) {
			criteria.add(Restrictions.eq("gunakanBiayaDefault", true));
		} else {
			criteria.add(Restrictions.or(Restrictions.isNull("gunakanBiayaDefault"),
					Restrictions.eq("gunakanBiayaDefault", false)));
		}
		criteria.addOrder(Order.desc("ta")).addOrder(Order.desc("id"));

		List<SettingBiaya> kandidat = ConstantValues.simpleList(criteria, SettingBiaya.class);
		kandidat = SettingBiayaMahasiswaSelector.saringDanPrioritaskan(session, kandidat, nimMahasiswa);

		String[] properties = new String[] { "statusMahasiswa", "kelamin", "afiliasiCalonMahasiswa", "program",
				"angkatan", "jenjang", "statusAwalMahasiswa", "jenisSeleksi", "gelombangPendaftaran", "paket",
				"jurusan" };
		Object[] datas = new Object[] { statusMahasiswa, kelamin, afiliasiCalonMahasiswa, program, angkatan, jenjang,
				statusAwalMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan };
		return SettingBiayaMahasiswaSelector.pilihSatuDenganPrioritas(kandidat, properties, datas);
	}

	/** Seperti {@link #getItemBiaya(Session, Integer, Jenjang, Integer, JenisKegiatan, StatusAwalMahasiswa, StatusMahasiswa, JenisSeleksi, GelombangPendaftaran, Paket, Jurusan, String, String, AfiliasiCalonMahasiswa, Integer, String)}, tanpa NIM mahasiswa spesifik (tidak menerapkan penyaringan/prioritas per-mahasiswa individual). */
	public static List<ItemBiaya> getItemBiaya(Session session, Integer angkatan, Jenjang jenjang, Integer semester,
			JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa, StatusMahasiswa statusMahasiswa,
			JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan,
			String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta) {
		return getItemBiaya(session, angkatan, jenjang, semester, jenisKegiatan, statusAwalMahasiswa,
				statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program, kelamin,
				afiliasiCalonMahasiswa, ta, null);
	}

	/**
	 * Menentukan daftar {@link ItemBiaya} yang berlaku untuk satu konteks mahasiswa/calon mahasiswa:
	 * mencari seluruh {@link SettingBiaya} UMUM (bukan {@code khususBuatMahasiswaTertentu}, bukan
	 * {@code gunakanBiayaDefault}) dengan {@code ta} tidak melebihi tahun-semester yang diminta dan
	 * jenis kegiatan cocok, menyaring+memprioritaskan lewat
	 * {@link ais.action.master.helper.SettingBiayaMahasiswaSelector} (termasuk pengecekan daftar
	 * pengecualian per NIM bila {@code nimMahasiswa} diberikan), lalu memilih SATU baris paling
	 * spesifik berdasarkan seluruh kriteria yang diberikan, dan akhirnya mengambil item biayanya
	 * yang aktif dan berlaku pada semester yang diminta (memperhatikan flag tidak-ditagih-semester-
	 * ganjil/genap dan rentang min/maks semester per item).
	 *
	 * @param session                  sesi Hibernate aktif
	 * @param angkatan                 tahun angkatan mahasiswa/calon mahasiswa
	 * @param jenjang                  jenjang pendidikan
	 * @param semester                 semester berjalan (memengaruhi item yang tidak ditagih ganjil/genap dan rentang min/maks smt)
	 * @param jenisKegiatan            jenis kegiatan penagihan (kriteria wajib cocok persis)
	 * @param statusAwalMahasiswa      status awal mahasiswa, boleh {@code null}
	 * @param statusMahasiswa          status mahasiswa saat ini, boleh {@code null}
	 * @param jenisSeleksi             jenis seleksi PMB, boleh {@code null}
	 * @param gelombangPendaftaran     gelombang pendaftaran, boleh {@code null}
	 * @param paket                    paket PMB, boleh {@code null}
	 * @param jurusan                  program studi
	 * @param program                  kode program (mis. reguler/karyawan)
	 * @param kelamin                  jenis kelamin, boleh {@code null}
	 * @param afiliasiCalonMahasiswa   afiliasi calon mahasiswa, boleh {@code null}
	 * @param ta                       kode tahun-semester (format {@code YYYYS}) batas atas setting biaya yang dipertimbangkan
	 * @param nimMahasiswa             NIM untuk penyaringan/pengecualian per-individu, boleh {@code null}
	 * @return daftar item biaya yang berlaku, atau {@code null} bila mahasiswa termasuk daftar pengecualian setting yang cocok
	 */
	public static List<ItemBiaya> getItemBiaya(Session session, Integer angkatan, Jenjang jenjang, Integer semester,
			JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa, StatusMahasiswa statusMahasiswa,
			JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran, Paket paket, Jurusan jurusan,
			String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa, Integer ta,
			String nimMahasiswa) {

		SettingBiaya settingBiaya = getSettingBiayaTerpilih(session, angkatan, jenjang, semester, jenisKegiatan,
				statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program,
				kelamin, afiliasiCalonMahasiswa, ta, nimMahasiswa, false);

		System.out.println("getItemBiaya final settingBiaya -> " + settingBiaya);
		if (settingBiaya == null) {
			return new ArrayList<ItemBiaya>();
		}
		if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa, semester)) {
			return null;
		}

		List<ItemBiaya> itemBiayas = ConstantValues
				.simpleList(session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya")

						.add(semester != null && semester % 2 == 0
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester != null && semester % 2 == 1
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"),
										Restrictions.le("itemBiaya.minSmt", semester)))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"),
										Restrictions.ge("itemBiaya.maxSmt", semester)))
						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
								Restrictions.eq("itemBiaya.aktif", true)))
						.setProjection(Projections.groupProperty("itemBiaya.id"))
						.add(Restrictions.eq("settingBiaya", settingBiaya)), ItemBiaya.class, false);

		System.out.println("3 itemBiayas -> " + itemBiayas);
		return itemBiayas;
	}

	/**
	 * Menyusun daftar {@link DetailBiaya} default (tagihan siap pakai) untuk satu
	 * {@link BiodataCalonMahasiswa} berdasarkan {@link SettingBiayaDetail} KHUSUS individu tersebut
	 * ({@code khususBuatMahasiswaTertentu=true}) yang rentang semesternya mencakup {@code semester}
	 * yang diminta dan {@code ta} setting tidak melebihi batas yang diminta (baris berprioritas dan
	 * ta terbaru dipilih). Mendelegasikan penyusunan detail ke
	 * {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, BiodataCalonMahasiswa)}.
	 *
	 * @param session                sesi Hibernate aktif
	 * @param biodataCalonMahasiswa  calon mahasiswa yang tagihannya disusun
	 * @param jenisKegiatan          jenis kegiatan penagihan
	 * @param semester               semester tagihan yang diminta
	 * @param ta                     kode tahun-semester batas atas setting yang dipertimbangkan
	 * @return daftar detail biaya, {@code null} bila tidak ada setting khusus yang cocok, atau daftar kosong bila calon mahasiswa termasuk daftar pengecualian
	 */
	public static List<DetailBiaya> getDetailBiayaDefault(Session session, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, Integer semester, Integer ta) {
		SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues.simpleObject(
				session.createCriteria(SettingBiayaDetail.class)
						.add(Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa))
						.createAlias("settingBiaya", "settingBiaya").add(Restrictions.le("settingBiaya.ta", ta))
						.add(Restrictions.eq("settingBiaya.khususBuatMahasiswaTertentu", true))
						.add(Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
						.add(Restrictions
								.sqlRestriction((semester == null ? 0 : semester) + " between minsmt and maxsmt"))
						.add(Restrictions.sqlRestriction(
								(semester == null ? 0 : semester) + " between min_smt_detail and max_smt_detail"))
						.addOrder(Order.asc("settingBiaya.prioritas"))
						.addOrder(Order.desc("settingBiaya.ta")).addOrder(Order.desc("id")).setMaxResults(1),
				SettingBiayaDetail.class);

		if (settingBiayaDetail != null) {
			if (settingBiayaDetail.getSettingBiaya() != null
					&& settingBiayaDetail.getSettingBiaya().isMahasiswaDikecualikan(
							biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getNim(), semester)) {
				return PengecualianTagihanList.kosong();
			}
			return getDefaultSettingBiaya(session, settingBiayaDetail, semester, biodataCalonMahasiswa);
		} else {
			return null;
		}
	}

	/** Seperti {@link #getDetailBiayaDefault(Session, BiodataCalonMahasiswa, JenisKegiatan, Integer, Integer)}, untuk mahasiswa aktif ({@link Mahasiswa}) alih-alih calon mahasiswa. */
	public static List<DetailBiaya> getDetailBiayaDefault(Session session, Mahasiswa mahasiswa,
			JenisKegiatan jenisKegiatan, Integer semester, Integer ta) {
		SettingBiayaDetail settingBiayaDetail = (SettingBiayaDetail) ConstantValues.simpleObject(
				session.createCriteria(SettingBiayaDetail.class).add(Restrictions.eq("mahasiswa", mahasiswa))
						.createAlias("settingBiaya", "settingBiaya").add(Restrictions.le("settingBiaya.ta", ta))
						.add(Restrictions.eq("settingBiaya.khususBuatMahasiswaTertentu", true))
						.add(Restrictions.eq("settingBiaya.jenisKegiatan", jenisKegiatan))
						.add(Restrictions
								.sqlRestriction((semester == null ? 0 : semester) + " between minsmt and maxsmt"))
						.add(Restrictions.sqlRestriction(
								(semester == null ? 0 : semester) + " between min_smt_detail and max_smt_detail"))
						.addOrder(Order.asc("settingBiaya.prioritas"))
						.addOrder(Order.desc("settingBiaya.ta")).addOrder(Order.desc("id")).setMaxResults(1),
				SettingBiayaDetail.class);

		if (settingBiayaDetail != null) {
			if (settingBiayaDetail.getSettingBiaya() != null
					&& settingBiayaDetail.getSettingBiaya().isMahasiswaDikecualikan(mahasiswa == null ? null : mahasiswa.getNim(), semester)) {
				return PengecualianTagihanList.kosong();
			}
			return getDefaultSettingBiaya(session, settingBiayaDetail, semester, mahasiswa);
		} else {
			return null;
		}
	}

	
	/** Seperti varian dengan {@code nimMahasiswa}, tanpa penyaringan/pengecualian per-individu. */
	public static List<DetailBiaya> getDetailBiayaDefault(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Integer ta) {
		return getDetailBiayaDefault(session, angkatan, jenjang, semester, jenisKegiatan, statusAwalMahasiswa,
				statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program, kelamin,
				afiliasiCalonMahasiswa, ta, null);
	}

	/**
	 * Varian {@code getDetailBiayaDefault} berbasis kriteria mentah (bukan entitas mahasiswa/calon
	 * mahasiswa langsung): menentukan {@link ItemBiaya} yang berlaku lewat {@link #getItemBiaya},
	 * lalu menyusun {@link DetailBiaya} default per item dari setting biaya UMUM yang paling cocok
	 * (bukan setting khusus individu — bandingkan dengan overload berbasis
	 * {@link BiodataCalonMahasiswa}/{@link Mahasiswa} yang mencari {@link SettingBiayaDetail} khusus).
	 *
	 * @param nimMahasiswa NIM untuk penyaringan/pengecualian per-individu, boleh {@code null}
	 * @return daftar detail biaya default yang berlaku sesuai kriteria
	 */
	public static List<DetailBiaya> getDetailBiayaDefault(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Integer ta, String nimMahasiswa) {

		SettingBiaya settingBiaya = getSettingBiayaTerpilih(session, angkatan, jenjang, semester, jenisKegiatan,
				statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program,
				kelamin, afiliasiCalonMahasiswa, ta, nimMahasiswa, true);

//		System.out.println(
//				"getDetailBiayaDefault final settingBiaya -> " + settingBiaya + " dari " + settingBiayas.size());
		if (settingBiaya == null) {
			// Tidak menemukan setting default bukan berarti mahasiswa dikecualikan.
			// Kembalikan list kosong biasa agar PembayaranUtil tetap melanjutkan ke
			// jalur tagihan normal/bulanan seperti perilaku sebelum fitur pengecualian NIM.
			return new ArrayList<DetailBiaya>();
		}
		if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa, semester)) {
			System.out.println("[TAGIHAN-DEBUG] SettingBiaya id=" + settingBiaya.getId()
					+ " tidak berlaku untuk NIM " + nimMahasiswa + " (daftar pengecualian).");
			// Hanya kondisi pengecualian eksplisit yang memakai list penanda agar
			// pemanggil berhenti dan tidak jatuh kembali ke tagihan normal.
			return PengecualianTagihanList.kosong();
		}
		List<DetailBiaya> detailBiayas = getDefaultSettingBiaya(session, settingBiaya, angkatan, jenjang, semester,
				jenisKegiatan, statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan,
				program, kelamin, afiliasiCalonMahasiswa);

//		System.out.println("size detailBiayas -> " + detailBiayas);

		return detailBiayas;

	}

	
	/**
	 * Menyusun daftar {@link DetailBiaya} dari seluruh {@link DetailSettingBiaya} milik satu
	 * {@link SettingBiaya} (setting UMUM, bukan khusus individu) yang aktif dan berlaku pada
	 * semester yang diminta: untuk tiap item, MEMAKAI baris {@link DetailBiaya} yang sudah persis
	 * cocok (setting biaya + detail setting + semester + jurusan + program yang sama) bila ada, atau
	 * MENYALIN dari template {@link DetailBiaya} terdekat (dicocokkan dari kriteria yang lebih umum)
	 * bila belum ada baris persis — sehingga nominal/label biaya konsisten dengan histori sebelumnya
	 * ketimbang dibuat kosong dari nol.
	 *
	 * @param session                sesi Hibernate aktif
	 * @param settingBiaya           setting biaya sumber daftar item
	 * @param angkatan               tahun angkatan, dipakai untuk mencari template terdekat
	 * @param jenjang                jenjang pendidikan
	 * @param semester               semester tagihan yang diminta
	 * @param jenisKegiatan          jenis kegiatan penagihan
	 * @param statusAwalMahasiswa    status awal mahasiswa, boleh {@code null}
	 * @param statusMahasiswa        status mahasiswa saat ini, boleh {@code null}
	 * @param jenisSeleksi           jenis seleksi PMB, boleh {@code null}
	 * @param gelombangPendaftaran   gelombang pendaftaran, boleh {@code null}
	 * @param paket                  paket PMB, boleh {@code null}
	 * @param jurusan                program studi
	 * @param program                kode program
	 * @param kelamin                jenis kelamin, boleh {@code null}
	 * @param afiliasiCalonMahasiswa afiliasi calon mahasiswa, boleh {@code null}
	 * @return daftar detail biaya siap pakai untuk setting biaya yang diberikan
	 */
	public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiaya settingBiaya, Integer angkatan,
			Jenjang jenjang, Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin,
			AfiliasiCalonMahasiswa afiliasiCalonMahasiswa) {
		List<DetailSettingBiaya> detailSettingBiayas = ConstantValues
				.simpleList(session.createCriteria(DetailSettingBiaya.class)

						.createAlias("itemBiaya", "itemBiaya")

						.addOrder(Order.asc("itemBiaya.nama")).addOrder(Order.asc("bayarKe"))

						.add(semester != null && semester % 2 == 0
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester != null && semester % 2 == 1
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"),
										Restrictions.le("itemBiaya.minSmt", semester)))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"),
										Restrictions.ge("itemBiaya.maxSmt", semester)))

						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
								Restrictions.eq("itemBiaya.aktif", true)))
						.add(Restrictions.eq("settingBiaya", settingBiaya)), DetailSettingBiaya.class);

		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {

			DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
					.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
					.add(Restrictions.eq("detailSettingBiaya", detailSettingBiaya))
					.add(Restrictions.eq("settingBiaya", settingBiaya)).add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("program", program)).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();

			System.out.println("size detailSettingBiaya -> " + detailSettingBiaya + " | " + detailBiaya);

			if (detailBiaya == null) {
				detailBiaya = new DetailBiaya();
				detailBiaya.setSemester(semester);
				detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
				detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
				detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
				detailBiaya.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswa);
				detailBiaya.setKelamin(kelamin);
				detailBiaya.setAngkatan(angkatan);
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setJenjang(jenjang);
				detailBiaya.setGelombangPendaftaran(gelombangPendaftaran);
				detailBiaya.setPaket(paket);
				detailBiaya.setJenisSeleksi(jenisSeleksi);
				detailBiaya.setProgram(program);
				detailBiaya.setStatusAwalMahasiswa(statusAwalMahasiswa);
				detailBiaya.setStatusMahasiswa(statusMahasiswa);
				detailBiaya.setJenisKegiatan(jenisKegiatan);
				detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
				detailBiaya.setSettingBiaya(settingBiaya);
				session.getTransaction().begin();
				session.save(detailBiaya);
				session.getTransaction().commit();
			} else if (((detailBiaya.getDetailSettingBiaya() == null && detailSettingBiaya != null)
					|| (detailBiaya.getDetailSettingBiaya() != null && detailSettingBiaya != null
							&& !detailBiaya.getDetailSettingBiaya().getId().equals(detailSettingBiaya.getId())))
					|| (detailBiaya.getSettingBiaya() == null && settingBiaya != null)
					|| (detailBiaya.getSettingBiaya() != null && settingBiaya != null
							&& !detailBiaya.getSettingBiaya().getId().equals(settingBiaya.getId()))) {
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setKelamin(kelamin);
				detailBiaya.setSemester(semester);
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
				detailBiaya.setSettingBiaya(settingBiaya);
				session.getTransaction().begin();
				session.update(detailBiaya);
				session.getTransaction().commit();
			}
			detailBiaya.setSettingBiaya(settingBiaya);
			SetingBiayaHelper.sinkronkanNilaiTemplateDetailBiaya(session, detailBiaya, detailSettingBiaya);
			detailBiayas.add(detailBiaya);
		}

		detailSettingBiayas = null;
		return detailBiayas;
	}

	
	/**
	 * Varian {@link #getDefaultSettingBiaya(Session, SettingBiaya, Integer, Jenjang, Integer,
	 * JenisKegiatan, StatusAwalMahasiswa, StatusMahasiswa, JenisSeleksi, GelombangPendaftaran, Paket,
	 * Jurusan, String, String, AfiliasiCalonMahasiswa)} berbasis {@link SettingBiayaDetail} KHUSUS
	 * satu {@link Mahasiswa}: sebelum menyusun detail, memeriksa apakah mahasiswa sudah lulus/keluar
	 * sebelum semester yang diminta — bila ya dan jenis kegiatan setting TIDAK menandai
	 * {@code tagihanJugaUntukAlumni}, mengembalikan daftar kosong (mahasiswa yang sudah tidak aktif
	 * tidak lagi ditagih kecuali jenis kegiatan tersebut memang berlaku untuk alumni).
	 *
	 * @param session            sesi Hibernate aktif
	 * @param settingBiayaDetail setting biaya detail khusus milik mahasiswa ini
	 * @param semester           semester tagihan yang diminta
	 * @param mahasiswa          mahasiswa target, diperiksa status kelulusan/keluarnya
	 * @return daftar detail biaya, atau daftar kosong bila mahasiswa sudah tidak aktif dan jenis kegiatan bukan untuk alumni
	 */
	public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiayaDetail settingBiayaDetail,
			Integer semester, Mahasiswa mahasiswa) {
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();

		try {
			if (semester != null && mahasiswa.getStatusKeluar() != null
					&& ((mahasiswa.getSemesterLulus() != null && mahasiswa.getSemesterLulus() < semester))) {

				if (settingBiayaDetail != null && settingBiayaDetail.getSettingBiaya() != null
						&& !settingBiayaDetail.getSettingBiaya().getJenisKegiatan().getTagihanJugaUntukAlumni()) {
					return detailBiayas;
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:2070");
			// TODO: handle exception
		}

		List<DetailSettingBiaya> detailSettingBiayas = ConstantValues
				.simpleList(session.createCriteria(DetailSettingBiaya.class)

						.createAlias("itemBiaya", "itemBiaya")

						.addOrder(Order.asc("itemBiaya.nama")).addOrder(Order.asc("bayarKe"))

						.add(semester != null && semester % 2 == 0
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester != null && semester % 2 == 1
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"),
										Restrictions.le("itemBiaya.minSmt", semester)))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"),
										Restrictions.ge("itemBiaya.maxSmt", semester)))

						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
								Restrictions.eq("itemBiaya.aktif", true)))
						.add(Restrictions.eq("settingBiaya", settingBiayaDetail.getSettingBiaya())),
						DetailSettingBiaya.class);

		Jurusan jurusan = mahasiswa.getJurusan();

		for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {

			DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
					.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
					.add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
					.add(Restrictions.eq("detailSettingBiaya", detailSettingBiaya))
					.add(Restrictions.eq("settingBiaya", settingBiayaDetail.getSettingBiaya()))
					.add(Restrictions.eq("settingBiayaDetail", settingBiayaDetail))
					.add(Restrictions.eq("jurusan", jurusan)).setMaxResults(1).addOrder(Order.desc("id"))
					.uniqueResult();

			if (detailBiaya == null) {

				Paket paket = null;
				try {
					BiodataCalonMahasiswa biodataCalonMahasiswa = mahasiswa.getBiodataCalonMahasiswaData();
					paket = biodataCalonMahasiswa == null ? null : biodataCalonMahasiswa.getPaket();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/SetingBiayaAction.java:2122");
					// TODO: handle exception
				}

				detailBiaya = new DetailBiaya();
				detailBiaya.setSemester(semester);
				detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
				detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
				detailBiaya.setPaket(paket);
				detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
				detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
				detailBiaya.setKelamin(detailSettingBiaya.getSettingBiaya().getKelamin());
				detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
				detailBiaya.setAngkatan(mahasiswa.getTahunangkatan());
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setJenjang(mahasiswa.getJenjang());
				detailBiaya.setGelombangPendaftaran(mahasiswa.getGelombangPendaftaranUntukBiaya());
				detailBiaya.setJenisSeleksi(mahasiswa.getJenisSeleksi());
				detailBiaya.setProgram(mahasiswa.getProgram());
				detailBiaya.setStatusAwalMahasiswa(mahasiswa.getStatusAwalMahasiswa());
				detailBiaya.setJenisKegiatan(settingBiayaDetail.getSettingBiaya().getJenisKegiatan());
				detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
				session.getTransaction().begin();
				session.save(detailBiaya);
				session.getTransaction().commit();
			}
			detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
			detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
			detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
			SetingBiayaHelper.sinkronkanNilaiTemplateDetailBiaya(session, detailBiaya, detailSettingBiaya);
			detailBiayas.add(detailBiaya);
		}

		detailSettingBiayas = null;
		return detailBiayas;
	}

	
	/**
	 * Seperti {@link #getDefaultSettingBiaya(Session, SettingBiayaDetail, Integer, Mahasiswa)}, untuk
	 * {@link BiodataCalonMahasiswa} (calon mahasiswa, belum berstatus mahasiswa aktif) — tanpa
	 * pengecekan status kelulusan/keluar yang hanya relevan untuk mahasiswa aktif.
	 */
	public static List<DetailBiaya> getDefaultSettingBiaya(Session session, SettingBiayaDetail settingBiayaDetail,
			Integer semester, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		List<DetailSettingBiaya> detailSettingBiayas = ConstantValues
				.simpleList(session.createCriteria(DetailSettingBiaya.class)

						.createAlias("itemBiaya", "itemBiaya").addOrder(Order.asc("itemBiaya.nama"))
						.addOrder(Order.asc("bayarKe"))

						.add(semester != null && semester % 2 == 0
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester != null && semester % 2 == 1
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"),
										Restrictions.le("itemBiaya.minSmt", semester)))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"),
										Restrictions.ge("itemBiaya.maxSmt", semester)))

						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
								Restrictions.eq("itemBiaya.aktif", true)))
						.add(Restrictions.eq("settingBiaya", settingBiayaDetail.getSettingBiaya())),
						DetailSettingBiaya.class);

		Jurusan jurusan = biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
				: biodataCalonMahasiswa.getProdiLulus();

		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {

			DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
					.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
					.add(Restrictions.eq("bayarKe", detailSettingBiaya.getBayarKe()))
					.add(Restrictions.eq("detailSettingBiaya", detailSettingBiaya))
					.add(Restrictions.eq("settingBiaya", settingBiayaDetail.getSettingBiaya()))
					.add(Restrictions.eq("jurusan", jurusan))
					.add(Restrictions.eq("settingBiayaDetail", settingBiayaDetail)).setMaxResults(1)
					.addOrder(Order.desc("id")).uniqueResult();

			if (detailBiaya == null) {
				detailBiaya = new DetailBiaya();
				detailBiaya.setSemester(semester);
				detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
				detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
				detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
				detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
				detailBiaya.setKelamin(detailSettingBiaya.getSettingBiaya().getKelamin());
				detailBiaya.setAfiliasiCalonMahasiswa(biodataCalonMahasiswa.getAfiliasiCalonMahasiswa());
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
				detailBiaya.setPaket(biodataCalonMahasiswa.getPaket());
				detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
				detailBiaya.setAngkatan(biodataCalonMahasiswa.getTahun());
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setJenjang(biodataCalonMahasiswa.getJenjang());
				detailBiaya.setGelombangPendaftaran(biodataCalonMahasiswa.getGelombangPendaftaran());
				detailBiaya.setJenisSeleksi(biodataCalonMahasiswa.getJenisSeleksi());
				detailBiaya.setProgram(biodataCalonMahasiswa.getProgram());
				detailBiaya.setStatusAwalMahasiswa(biodataCalonMahasiswa.getStatusAwalMahasiswa());
				detailBiaya.setJenisKegiatan(settingBiayaDetail.getSettingBiaya().getJenisKegiatan());
				detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());

				session.getTransaction().begin();
				session.save(detailBiaya);
				session.getTransaction().commit();
			}
			detailBiaya.setSettingBiaya(settingBiayaDetail.getSettingBiaya());
			detailBiaya.setJurusan(biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1()
					: biodataCalonMahasiswa.getProdiLulus());
			detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
			detailBiaya.setSettingBiayaDetail(settingBiayaDetail);
			SetingBiayaHelper.sinkronkanNilaiTemplateDetailBiaya(session, detailBiaya, detailSettingBiaya);
			detailBiayas.add(detailBiaya);
		}

		detailSettingBiayas = null;
		return detailBiayas;
	}

	
	/**
	 * Varian {@link #getItemBiaya}/{@link #getDefaultSettingBiaya} yang HANYA menyertakan item biaya
	 * dengan nominal default eksplisit lebih dari nol ({@code defaultBiaya > 0.1}) — dipakai untuk
	 * konteks yang butuh daftar tagihan bernominal pasti (bukan item "default kosong"/nol yang
	 * biasanya diisi manual belakangan). Sama seperti {@link #getItemBiaya}, mencari
	 * {@link SettingBiaya} UMUM paling cocok/prioritas, lalu menyusun {@link DetailBiaya} per item
	 * (memakai baris tersimpan bila cocok persis, atau template terdekat/entitas baru bila belum ada).
	 *
	 * @return daftar detail biaya bernominal default non-nol yang berlaku sesuai kriteria, daftar
	 *         kosong bila tidak ada setting biaya yang cocok
	 */
	public static List<DetailBiaya> getDetailBiayaBukanDefaultBiaya(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Integer ta) {
		return getDetailBiayaBukanDefaultBiaya(session, angkatan, jenjang, semester, jenisKegiatan,
				statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program,
				kelamin, afiliasiCalonMahasiswa, ta, null);
	}

	public static List<DetailBiaya> getDetailBiayaBukanDefaultBiaya(Session session, Integer angkatan, Jenjang jenjang,
			Integer semester, JenisKegiatan jenisKegiatan, StatusAwalMahasiswa statusAwalMahasiswa,
			StatusMahasiswa statusMahasiswa, JenisSeleksi jenisSeleksi, GelombangPendaftaran gelombangPendaftaran,
			Paket paket, Jurusan jurusan, String program, String kelamin, AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Integer ta, String nimMahasiswa) {

		SettingBiaya settingBiaya = getSettingBiayaTerpilih(session, angkatan, jenjang, semester, jenisKegiatan,
				statusAwalMahasiswa, statusMahasiswa, jenisSeleksi, gelombangPendaftaran, paket, jurusan, program,
				kelamin, afiliasiCalonMahasiswa, ta, nimMahasiswa, false);

		System.out.println("getDetailBiayaBukanDefaultBiaya settingBiaya -> " + settingBiaya);
		if (settingBiaya == null) {
			return new ArrayList<DetailBiaya>();
		}
		if (settingBiaya.isMahasiswaDikecualikan(nimMahasiswa, semester)) {
			return PengecualianTagihanList.kosong();
		}

		List<DetailSettingBiaya> detailSettingBiayas = ConstantValues
				.simpleList(session.createCriteria(DetailSettingBiaya.class).createAlias("itemBiaya", "itemBiaya")

						.addOrder(Order.asc("itemBiaya.nama")).addOrder(Order.asc("bayarKe"))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.minSmt"),
										Restrictions.le("itemBiaya.minSmt", semester)))
						.add(semester == null ? Restrictions.sqlRestriction("true")
								: Restrictions.or(Restrictions.isNull("itemBiaya.maxSmt"),
										Restrictions.ge("itemBiaya.maxSmt", semester)))

						.add(Restrictions.gt("defaultBiaya", 0.1))

						.add(semester != null && semester % 2 == 0
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGenap"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGenap", false))
								: Restrictions.sqlRestriction("true"))

						.add(semester != null && semester % 2 == 1
								? Restrictions.or(Restrictions.isNull("itemBiaya.tidakDitagihDiSmtGanjil"),
										Restrictions.eq("itemBiaya.tidakDitagihDiSmtGanjil", false))
								: Restrictions.sqlRestriction("true"))

						.add(Restrictions.or(Restrictions.isNull("itemBiaya.aktif"),
								Restrictions.eq("itemBiaya.aktif", true)))
						.add(Restrictions.eq("settingBiaya", settingBiaya)), DetailSettingBiaya.class);

		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		for (DetailSettingBiaya detailSettingBiaya : detailSettingBiayas) {
			DetailBiaya detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
					.add(Restrictions.eq("detailSettingBiaya", detailSettingBiaya))
					.add(Restrictions.eq("settingBiaya", settingBiaya)).add(Restrictions.eq("program", program))
					.add(semester == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("semester", semester))
					.add(Restrictions.eq("jurusan", jurusan)).addOrder(Order.desc("id")).setMaxResults(1)
					.uniqueResult();

			if (detailBiaya == null) {
				detailBiaya = new DetailBiaya();
				detailBiaya.setSettingBiaya(settingBiaya);
				detailBiaya.setBayarKe(detailSettingBiaya.getBayarKe());
				detailBiaya.setItemBiaya(detailSettingBiaya.getItemBiaya());
				detailBiaya.setNilaiBiaya(detailSettingBiaya.getDefaultBiaya());
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);
				detailBiaya.setAngkatan(angkatan);
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswa);
				detailBiaya.setKelamin(kelamin);
				detailBiaya.setJenisSeleksi(jenisSeleksi);
				detailBiaya.setGelombangPendaftaran(gelombangPendaftaran);
				detailBiaya.setPaket(paket);
				detailBiaya.setJenjang(jenjang);
				detailBiaya.setProgram(program);
				detailBiaya.setStatusAwalMahasiswa(statusAwalMahasiswa);
				detailBiaya.setStatusMahasiswa(statusMahasiswa);
				detailBiaya.setJenisKegiatan(jenisKegiatan);
				detailBiaya.setNama("Biaya Default " + detailSettingBiaya.getId());
				if (semester != null)
					detailBiaya.setSemester(semester);
				session.getTransaction().begin();
				session.save(detailBiaya);
				session.getTransaction().commit();
			} else if (((detailBiaya.getDetailSettingBiaya() == null && detailSettingBiaya != null)
					|| (detailBiaya.getDetailSettingBiaya() != null && detailSettingBiaya != null
							&& !detailBiaya.getDetailSettingBiaya().getId().equals(detailSettingBiaya.getId())))
					|| (detailBiaya.getSettingBiaya() == null && settingBiaya != null)
					|| (detailBiaya.getSettingBiaya() != null && settingBiaya != null
							&& !detailBiaya.getSettingBiaya().getId().equals(settingBiaya.getId()))) {
				detailBiaya.setJurusan(jurusan);
				detailBiaya.setSettingBiaya(settingBiaya);
				detailBiaya.setDetailSettingBiaya(detailSettingBiaya);

				session.getTransaction().begin();
				session.update(detailBiaya);
				session.getTransaction().commit();
			}
			if (semester != null)
				detailBiaya.setSemester(semester);
			detailBiaya.setSettingBiaya(settingBiaya);
			SetingBiayaHelper.sinkronkanNilaiTemplateDetailBiaya(session, detailBiaya, detailSettingBiaya);
			detailBiayas.add(detailBiaya);
		}

		return detailBiayas;
	}
}
