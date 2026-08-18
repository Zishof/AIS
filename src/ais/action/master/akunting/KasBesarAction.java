package ais.action.master.akunting;

import java.io.File;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.dashboard.akunting.DasboardKasBesar;
import ais.action.master.akunting.helper.MonitorKasBesarDashboard;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.action.master.rab.helper.AmbilDataWorkspaceBanbox;
import ais.action.master.rab.util.SatuanKerjaTreeModel;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.akunting.LaporanKasBesar;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.akunting.DanaTalangan;
import ais.database.model.akunting.JenisKasBesar;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.NomorSuratAlurKeuangan;
import ais.database.model.akunting.UangMuka;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * <h3>KasBesarAction — Pengelolaan Pengeluaran Kas Besar</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini adalah Action ZK yang mengelola seluruh siklus hidup
 * dokumen Pengeluaran Kas Besar (main cash disbursement). Modul ini digunakan oleh staf
 * keuangan untuk mencatat pengeluaran dana dari kas besar institusi — misalnya pembelian
 * peralatan, biaya operasional besar, atau pengisian kembali kas kecil. Setiap pengeluaran
 * dikategorikan melalui {@link JenisKasBesar} yang menentukan akun sumber dan akun penerima
 * dalam jurnal akuntansi.</p>
 *
 * <p><b>Fitur utama:</b>
 * <ul>
 *   <li><b>Detail formula:</b> Rincian biaya disimpan sebagai JSON array di kolom
 *       {@code formula} entitas {@link KasBesar}. Setiap item berisi: workspace (anggaran),
 *       tanggal, keterangan, qty, harga, jumlah, dan referensi lampiran. Method statik
 *       {@code reloadFormula()} dan {@code reloadDataFormula()} merender array ini menjadi
 *       grid yang dapat diedit.</li>
 *   <li><b>Integrasi kas kecil:</b> Opsional, kas besar dapat dikaitkan dengan kas kecil
 *       tertentu via checkbox "Ambil dari Kas Kecil", yang memungkinkan penelusuran aliran
 *       dana dari kas besar ke kas kecil.</li>
 *   <li><b>Alur persetujuan:</b> Pengajuan dibuat oleh staf, lalu disetujui oleh pejabat
 *       (mengubah status menjadi DISETUJU). Setelah disetujui, sistem otomatis membuat
 *       {@link DaftarPengajuanTransfer} untuk proses transfer.</li>
 *   <li><b>Dashboard statistik:</b> Tab "Statistik" (lazy-loaded) menampilkan
 *       {@link DasboardKasBesar} yang merangkum data pengeluaran.</li>
 *   <li><b>Tab Jenis Kas Besar:</b> Tab "Kas Besar" (lazy-loaded) menampilkan master data
 *       jenis kas besar tanpa navigasi ke halaman terpisah.</li>
 * </ul></p>
 *
 * <p><b>Cara kerja:</b> Mengimplementasikan empat antarmuka — {@code DataCriteria},
 * {@code DataSearchDefault}, {@code DataInitDefault}, {@code FormSop} — dengan pola
 * yang sama seperti {@code PenggantianKasKecilAction}. Filter pencarian mendukung:
 * satuan kerja (hierarkis), tanggal, status, kode, nama, dan jenis kas besar.</p>
 *
 * <p><b>Threading:</b> Seluruh operasi UI dan DB berjalan di thread ZK event-dispatcher.
 * Operasi upload lampiran menggunakan sesi streaming terpisah
 * ({@link StreamingHibernateUtil}) untuk menghindari konflik dengan sesi utama.
 * Cetak laporan dan pembuatan DaftarPengajuanTransfer dijadwalkan async via timer.</p>
 *
 * <p><b>Pemeliharaan:</b> Array statik {@code contents} mendefinisikan field yang
 * diekspor ke Excel/PDF. Jika ada field baru di {@link KasBesar}, tambahkan ke array ini.
 * Method statik {@code reloadFormula()} dan {@code reloadDataFormula()} dapat dipanggil
 * dari kelas lain untuk merender formula yang sama di konteks berbeda.</p>
 *
 * @author AIS
 * @see KasBesar
 * @see JenisKasBesar
 * @see DaftarPengajuanTransfer
 */
public class KasBesarAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault, FormSop {

	/**
	 * ID serialisasi versi kelas untuk kompatibilitas {@code Serializable}.
	 */
	private static final long serialVersionUID = 4124140285573733292L;

	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox serachnama;
	private Textbox serachjenis;
	private Textbox serachkode;
	private Combobox searchstatus;
	private Checkbox searchaktif;
	private MyDatebox start;
	private MyDatebox end;
	private Textbox nama;
	private Label kode;
	private Textbox keterangan;

	public KasBesar kasBesar;
	private MyToolbarbuttonConfig add;

	private boolean edit;
	private boolean delete;

	private Combobox jenisKasBesar;

	private Double nilai;

	private MyDatebox tanggal;

	private DisposisiSop disposisiSop;

	private JSONArray array;

	private Row rowFormula;

	private boolean persetujuan = false;

	private Tbmuser tbmuser;

	private SatuanKerjaTreeModel satuanKerjaTreeModel;
	private AmbilDataSatuanKerjaBanbox searchparent;

	private AmbilDataSatuanKerjaBanbox satuanKerja;

	private Radiogroup status;

	private boolean setujui = false;
	protected LampiranLain lainMahasiswa;
	private Tabpanel jenisKasBesarTab;

	private boolean viewOnly = false;

	/**
	 * Handler event ZK untuk tab "Kas Besar" — memuat konten Jenis Kas Besar secara
	 * lazy (hanya saat tab diklik pertama kali) untuk menghindari beban inisialisasi
	 * yang tidak perlu saat halaman pertama kali dimuat.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan lazy loading untuk panel master data Jenis
	 * Kas Besar agar halaman utama lebih cepat dimuat. Konten hanya dibuat satu kali
	 * (guard: {@code getChildren().size() == 0}) sehingga state form tidak direset
	 * setiap kali tab dibuka.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah tab sudah memiliki anak komponen. Jika
	 * belum, membuat {@link MyWindow} dan {@link MyInclude} yang memuat ZUL
	 * {@code "/pages/master/akunting/jenis_kas_besar.zul"} di dalam tab panel.</p>
	 *
	 * @param event event ZK yang memicu handler ini (biasanya onSelect dari Tabbox)
	 *
	 * <p><b>Pemeliharaan:</b> Jika path ZUL Jenis Kas Besar berubah, perbarui string
	 * path di dalam MyInclude. Nama method {@code onKasBesar} bersifat konvensional ZK
	 * — harus sesuai dengan event name di atribut ZUL tabpanel.</p>
	 */
	public void onKasBesar(Event event) {

		if (jenisKasBesarTab.getChildren().size() == 0) {
			MyWindow window = new MyWindow("", "none", false);
			window.setHeight("100%");
			window.setWidth("100%");
			window.setParent(jenisKasBesarTab);
			MyInclude iframe = new MyInclude("/pages/master/akunting/jenis_kas_besar.zul");
			iframe.setParent(window);
		}
	}

	protected Tabpanel statistik;
	/** Tabpanel tab "Monitor" — dashboard pemantauan kas besar & pembayaran DPC (lazy). */
	protected Tabpanel monitor;

	private MyDatebox tanggalPersetujuanManual;

	private MyCheckboxConfig ambilDariKasKecil;

	private Row rowPenerima;

	private Combobox kasKecil;

	private Row rowkasKecil;

	private Row rowDetail;

	/**
	 * Handler event ZK untuk tab "Statistik" — memuat dashboard kas besar secara lazy
	 * saat tab diklik pertama kali.
	 *
	 * <p><b>Tujuan:</b> Mengimplementasikan lazy loading untuk {@link DasboardKasBesar}
	 * yang berisi grafik dan ringkasan statistik pengeluaran kas besar. Dashboard ini
	 * cukup berat sehingga hanya dimuat ketika pengguna benar-benar membuka tab ini.</p>
	 *
	 * <p><b>Cara kerja:</b> Memeriksa apakah {@code statistik} tab panel sudah memiliki
	 * anak. Jika belum, membuat instance {@link DasboardKasBesar} dan menempelkannya
	 * ke panel statistik. Dashboard langsung mengambil data saat diinisialisasi.</p>
	 *
	 * @param event event ZK yang memicu handler ini (biasanya onSelect dari Tabbox)
	 *
	 * <p><b>Pemeliharaan:</b> Jika kelas dashboard berubah nama atau konstruktor-nya
	 * berubah, perbarui di sini. Guard {@code getChildren().size() == 0} memastikan
	 * dashboard hanya dibuat sekali — jangan hapus guard ini.</p>
	 */
	/**
	 * Memuat dashboard "Monitor" kas besar (pemantauan + pembayaran DPC + pertanggungjawaban) secara lazy.
	 */
	public void onMonitor(Event event) {
		if (monitor != null && monitor.getChildren().size() == 0) {
			MonitorKasBesarDashboard dashboard = new MonitorKasBesarDashboard();
			dashboard.setHeight("100%");
			dashboard.setWidth("100%");
			dashboard.setParent(monitor);
		}
	}

	public void onStatistik(Event event) {

		if (statistik.getChildren().size() == 0) {
			DasboardKasBesar include = new DasboardKasBesar();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(statistik);
		}
	}

	public static String[] contents = new String[] { "id", "kode", "nama", "keterangan", "formula", "satuanKerja",
			"jenisKasBesar", "tanggal", "nilai", "status", "daftarPengajuanTransfer.prosesTransfer.kode",
			"daftarPengajuanTransfer.prosesTransfer.nama", "daftarPengajuanTransfer.prosesTransfer.tanggalPembuatan",
			"daftarPengajuanTransfer.prosesTransfer.disetujuiOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalPersetujuan",
			"daftarPengajuanTransfer.prosesTransfer.realisasikanOleh",
			"daftarPengajuanTransfer.prosesTransfer.tanggalRealisasikan", "aktif" };

	/**
	 * Konstruktor default — dipakai saat ZK meng-wire kelas ini dari file ZUL.
	 * Menginisialisasi pengguna aktif sebagai pembuat potensial entitas baru.
	 *
	 * @see PenggantianKasKecilAction#PenggantianKasKecilAction()
	 */
	public KasBesarAction() {
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Konstruktor mode persetujuan — dipakai saat action dibuat secara programatis
	 * dari alur SOP atau menu persetujuan keuangan.
	 *
	 * <p><b>Tujuan:</b> Mengaktifkan mode persetujuan sehingga pejabat dapat melihat
	 * pengajuan kas besar, mengubah statusnya, dan mengaktifkan/menonaktifkan item
	 * tanpa bisa membuat pengajuan baru.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan
	 */
	public KasBesarAction(boolean persetujuan) {
		this.persetujuan = persetujuan;
		tbmuser = Common.getCurrentUser();
	}

	/**
	 * Hook ZK pra-composing untuk memeriksa keamanan akses sebelum halaman dirender.
	 *
	 * @param page     halaman ZK yang sedang di-compose
	 * @param parent   komponen induk
	 * @param compInfo metadata komponen
	 * @return {@code ComponentInfo} dari superclass
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Hook ZK pasca-composing yang menginisialisasi seluruh komponen UI halaman kas besar
	 * setelah semua elemen ZUL selesai di-wire ke field Java.
	 *
	 * <p><b>Tujuan:</b> Menyiapkan halaman daftar kas besar sehingga siap digunakan —
	 * termasuk pemeriksaan sesi, filter tanggal, combobox status, hak akses tombol,
	 * paging, ekspor data, dan pemuatan data awal via timer.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code super.doAfterCompose()} dan inisialisasi locale.</li>
	 *   <li>Memeriksa sesi dan hak READ; jika gagal, goLogoff dan return.</li>
	 *   <li>Guard: jika {@code searchparent} null, return. Jika {@code searchstatus}
	 *       atau {@code add} null, return (komponen ZUL belum lengkap).</li>
	 *   <li>Mendaftarkan listener perubahan satuan kerja di {@code searchparent}.</li>
	 *   <li>Inisialisasi filter tanggal (6 bulan lalu s.d. besok, read-only).</li>
	 *   <li>Mengisi combobox status (Semua/Pengajuan/Disetujui/Ditolak).</li>
	 *   <li>Membaca parameter URL {@code persetujuan}.</li>
	 *   <li>Mengatur visibilitas tombol Add (hanya jika CREATE dan bukan mode persetujuan).</li>
	 *   <li>Membaca hak UPDATE dan DELETE.</li>
	 *   <li>Menginisialisasi paging.</li>
	 *   <li>Menambahkan tombol ekspor cetak dan upload.</li>
	 *   <li>Menjadwalkan {@code onSearchDefault()} via timer untuk pengisian data awal.</li>
	 *   <li>Mengaktifkan panel filter lanjutan via {@code FilterLanjutHelper.setup()}.</li>
	 * </ol></p>
	 *
	 * @param comp komponen root ZK hasil composing
	 * @throws Exception jika terjadi kesalahan saat inisialisasi
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchparent == null) return;
		searchparent.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(arg0);
			}
		});
		satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);

		if (searchstatus == null || add == null) return;
		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Comboitem comboitemSemua = new Comboitem("Semua");
		if (comboitemSemua != null) { comboitemSemua.setValue(null); }
		searchstatus.appendChild(comboitemSemua);

		Comboitem comboitem = new Comboitem(KasBesar.PENGAJUAN);
		if (comboitem != null) { comboitem.setValue(KasBesar.PENGAJUAN); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(KasBesar.DISETUJU);
		if (comboitem != null) { comboitem.setValue(KasBesar.DISETUJU); }
		searchstatus.appendChild(comboitem);
		comboitem = new Comboitem(KasBesar.DITOLAK);
		if (comboitem != null) { comboitem.setValue(KasBesar.DITOLAK); }
		searchstatus.appendChild(comboitem);

		if (searchstatus != null) { searchstatus.setSelectedItem(comboitemSemua); }
		if (searchstatus != null) { searchstatus.setReadonly(true); }

		if (execution.getParameter("persetujuan") != null) {
			persetujuan = Boolean.parseBoolean(execution.getParameter("persetujuan"));
		}

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && !persetujuan);
		}

		if (add != null) { add.setTooltiptext("Tambah"); }
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(KasBesar.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KasBesar.class, contents);
		if (upload != null) { upload.setVisible((add != null && add.isVisible()) && edit && delete); }
		Common.appendKeToolbar(upload, add, comp);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	/**
	 * Membangun dan mengembalikan grid ringkasan rincian biaya (formula) dari entitas
	 * {@link KasBesar} dalam tampilan read-only, cocok untuk ditampilkan di popup atau
	 * panel pratinjau.
	 *
	 * <p><b>Tujuan:</b> Menyediakan visualisasi tabel dari array JSON formula kas besar
	 * yang berisi informasi workspace (anggaran), tanggal, keterangan, qty, harga,
	 * dan jumlah per baris biaya, disertai footer total.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Jika {@code kasBesar} null, mengembalikan grid kosong.</li>
	 *   <li>Membuat grid dengan empat kolom: Anggaran, Qty, Harga, Jumlah.</li>
	 *   <li>Mengurai JSON array formula dari {@code kasBesar.getFormula()}.</li>
	 *   <li>Untuk setiap item JSON, mengambil workspace dari {@link ConstantValues},
	 *       nama, qty, harga, jumlah, dan tanggal.</li>
	 *   <li>Membuat baris {@link MyFormRow} dengan label kecil untuk setiap item.</li>
	 *   <li>Menambahkan footer yang menampilkan total nilai dari
	 *       {@code kasBesar.getNilai()}.</li>
	 * </ol></p>
	 *
	 * @param kasBesar entitas kas besar yang rinciannya akan ditampilkan; boleh null
	 * @return {@link Grid} ZK berisi tabel rincian biaya, atau grid kosong jika null
	 * @throws Exception jika terjadi kesalahan parsing JSON atau akses data
	 *
	 * <p><b>Perbedaan dengan {@code reloadDataFormula()}:</b> Method ini menghasilkan
	 * tampilan read-only sederhana tanpa kontrol edit. {@code reloadDataFormula()}
	 * menghasilkan tampilan yang dapat diedit dengan textbox, doublebox, dan datebox.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ada field baru di JSON formula (misalnya nomor nota),
	 * tambahkan kolom baru di grid dan parsing field baru di sini.</p>
	 */
	public static Grid tampilRinci(KasBesar kasBesar) throws Exception {
		if (kasBesar != null) {
			Grid grid = new Grid();
			grid.setSclass("dgrid");
			grid.setWidth("100%");
			grid.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig("Anggaran");
			column.setParent(columns);

			column = new MyColumnConfig("Qty");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("10%");

			column = new MyColumnConfig("Harga");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Jumlah");
			column.setAlign("right");
			column.setParent(columns);
			column.setWidth("20%");

			Rows rows = new Rows();
			rows.setParent(grid);
			JSONArray array = new JSONArray(kasBesar.getFormula());
			for (int i = 0; i < array.length(); i++) {

				JSONObject jsonObject = array.getJSONObject(i);

				Workspace workspace = (Workspace) (jsonObject.isNull("workspace") ? null
						: ConstantValues.ambil(Workspace.class.getName(),
								new BigDecimal(jsonObject.get("workspace") + "").longValue()));

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Date tanggal = WaktuUtil.getDate();
				if (!jsonObject.isNull("tanggal")) {
					tanggal = Common.dateFormat9.get().parse(jsonObject.getString("tanggal"));
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				Vbox vbox = new Vbox();
				row.appendChild(vbox);

				vbox.appendChild(new MyLabelAgakKecil(workspace == null ? "" : workspace.toString()));
				vbox.appendChild(new MyLabelAgakKecil(Common.dateFormat.get().format(tanggal)));
				vbox.appendChild(new MyLabelAgakKecil(nama));

				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(qty)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(harga)));
				row.appendChild(new MyLabelAgakKecil(Common.numberFormat.get().format(jumlah)));
			}
			Foot foot = new Foot();
			foot.setParent(grid);

			Footer footer = new Footer("Total");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			footer = new Footer("");
			foot.appendChild(footer);

			Footer footerTotal = new Footer(Common.numberFormat.get().format(kasBesar.getNilai()));
			foot.appendChild(footerTotal);

			return grid;
		} else {
			return new Grid();
		}
	}

	/**
	 * Renderer baris grid untuk menampilkan setiap entri {@link KasBesar} dalam
	 * tabel daftar utama halaman pengeluaran kas besar.
	 *
	 * <p><b>Untuk apa:</b> Mengubah objek domain menjadi baris visual dengan informasi
	 * kode/transfer, jenis, kas kecil terkait, nilai, tanggal, satuan kerja, pembuat,
	 * status, keterangan, SOP, status aktif, dan tombol aksi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan sel harus sesuai dengan urutan kolom header di ZUL.
	 * Jika ada kolom baru, tambahkan di posisi yang tepat dalam {@code render()}.</p>
	 */
	class KasBesarRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Merender satu baris data {@link KasBesar} ke dalam komponen ZK.
		 *
		 * <p><b>Tujuan:</b> Mengubah objek domain menjadi representasi visual lengkap
		 * termasuk sinkronisasi otomatis relasi kas kecil, pembuatan DaftarPengajuanTransfer
		 * untuk yang sudah disetujui, dan tampilan tombol aksi yang sesuai status.</p>
		 *
		 * <p><b>Cara kerja langkah demi langkah:</b>
		 * <ol>
		 *   <li>Jika kas kecil terkait belum memiliki referensi balik ke kas besar ini,
		 *       memperbarui relasi tersebut di DB secara otomatis.</li>
		 *   <li>Mengisi {@code dibuatOleh} jika masih null.</li>
		 *   <li>Jika status DISETUJU dan belum ada DaftarPengajuanTransfer, membuat entri
		 *       transfer via {@code DaftarPengajuanTransfer.simpanKasBesar()}.</li>
		 *   <li>Menampilkan: revisi kode (dengan link ke prosesTransfer jika ada), jenis
		 *       kas besar dan nama kas kecil, nilai, tanggal dan satuan kerja dan pembuat,
		 *       status dan penyetuju dan tanggal persetujuan, keterangan dan link SOP.</li>
		 *   <li>Menampilkan status aktif: label jika SOP tidak aktif atau sudah disetujui;
		 *       checkbox interaktif jika mode persetujuan dan belum disetujui.</li>
		 *   <li>Menampilkan status DaftarPengajuanTransfer.</li>
		 *   <li>Menampilkan tombol Ubah/Copy/Hapus dan tombol Cetak.</li>
		 * </ol></p>
		 *
		 * @param arg0 baris ZK yang harus diisi komponen anak
		 * @param arg1 objek data yang akan di-cast ke {@link KasBesar}
		 * @throws Exception jika terjadi kesalahan DB atau rendering
		 *
		 * <p><b>Pemeliharaan:</b> Sinkronisasi relasi KasKecil di setiap render dapat
		 * menyebabkan banyak operasi DB jika ada data lama yang tidak konsisten.
		 * Pertimbangkan migrasi data untuk memperbaiki inkonsistensi sekaligus daripada
		 * memperbaikinya satu per satu di render.</p>
		 */
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			final KasBesar kasBesar = (KasBesar) arg1;

			KasKecil w = kasBesar.getKasKecil();
			if (w != null && w.getKasBesar() == null) {
				Session session = HibernateUtil.currentSession();
				session.refresh(w);
				w.setKasBesar(kasBesar);
				Common.refreshUpdate(session, w);
			}

			if (kasBesar.getDibuatOleh() == null) {
				kasBesar.setDibuatOleh(tbmuser);
			}

			if (kasBesar.getStatus().equals(UangMuka.DISETUJU) && kasBesar.getDaftarPengajuanTransfer() == null) {
				DaftarPengajuanTransfer.simpanKasBesar(kasBesar);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(KasBesar.class, kasBesar,
					kasBesar.getKode() == null ? "" : kasBesar.getKode().trim().toString())).setParent(arg0);

			if (kasBesar.getDaftarPengajuanTransfer() != null
					&& kasBesar.getDaftarPengajuanTransfer().getProsesTransfer() != null) {

				A aaa = new A(kasBesar.getDaftarPengajuanTransfer().getProsesTransfer().getKode());
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ProsesTransferAction.onAddExternal(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								onSearchDefault(arg0);
							}
						}, kasBesar.getDaftarPengajuanTransfer().getProsesTransfer());

					}
				});
				aaa.setStyle("font-size:12px;");
				aaa.setParent(a);
			}

			Vbox myvbox = new Vbox();
			myvbox.setParent(a);

			Hbox hbox = new Hbox();
			hbox.setParent(myvbox);
			LampiranLain.createDownloadUploadFileLain(hbox, kasBesar.getId(), KasBesar.class.getName(), "Bukti", false,
					null, null, false, false, false, true);

			myvbox = new Vbox();
			myvbox.setParent(arg0);
			new Label(kasBesar.getJenisKasBesar() == null ? ""
					: kasBesar.getJenisKasBesar().getKode() + "-" + kasBesar.getJenisKasBesar().getNama())
					.setParent(myvbox);
			new Label(kasBesar.getKasKecil() == null ? ""
					: kasBesar.getKasKecil().getKode() + "-" + kasBesar.getKasKecil().getNama()).setParent(myvbox);

			new Label(Common.numberFormat.get().format(kasBesar.getNilai())).setParent(arg0);

			a = new Vbox();
			a.setParent(arg0);
			new Label(kasBesar.getTanggal() == null ? "" : Common.dateFormat3.get().format(kasBesar.getTanggal()))
					.setParent(a);
			new Label(kasBesar.getSatuanKerja() == null ? "" : kasBesar.getSatuanKerja().getNama()).setParent(a);

			new MyLabelAgakKecil(kasBesar.getDibuatOleh() == null ? "" : kasBesar.getDibuatOleh().getUserNama())
					.setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			new Label(kasBesar.getStatus()).setParent(a);
			(new MyLabelAgakKecil(kasBesar.getDisetujuiOleh() == null ? "" : kasBesar.getDisetujuiOleh().getUserNama()))
					.setParent(a);
			(new MyLabelAgakKecil(kasBesar.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(kasBesar.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelAgakKecil(Common.simpleString(kasBesar.getKeterangan())).setParent(vbox1);
			if (kasBesar.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox1);
				UIClassHelper.applyReadMore(aa, "SOP " + kasBesar.getDisposisiSop().getKeterangan() + " ("
						+ kasBesar.getDisposisiSop().getSop().getNama() + ")");
				aa.setStyle("font-size:9px;");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(kasBesar.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			if (kasBesar.getDisposisiSop() != null && !kasBesar.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (persetujuan && !kasBesar.getStatus().equals(KasBesar.DISETUJU)) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(kasBesar.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kasBesar.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(kasBesar);
					}
				});
			} else {
				new Label(kasBesar.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			vbox1 = new Vbox();
			vbox1.setParent(arg0);

			DaftarPengajuanTransfer.tampilStatus(kasBesar.getDaftarPengajuanTransfer(), vbox1);

			Hbox hbx;
			(hbx = Common.copyEditDeleteButtons(edit, !persetujuan && !kasBesar.getStatus().equals(KasBesar.DISETUJU),
					delete && !persetujuan && !kasBesar.getStatus().equals(KasBesar.DISETUJU), kasBesar,
					KasBesarAction.this)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(kasBesar);
				}

			});
			button.setParent(hbx);
		}

	}

	/**
	 * Menghasilkan file PDF laporan kas besar untuk ekspor massal dari toolbar grid.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code DataCriteria.cetakData()} untuk menghasilkan
	 * file PDF yang dapat diunduh melalui mekanisme ekspor generik. Menggunakan template
	 * JasperReports {@code "akunting/kasBesar"}.</p>
	 *
	 * @param generalValueObject objek yang akan dicetak; di-cast ke {@link KasBesar}
	 * @return file PDF yang dihasilkan
	 * @throws Exception jika template tidak ditemukan atau terjadi kesalahan I/O
	 */
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		KasBesar kasBesar = (KasBesar) generalValueObject;
		LaporanKasBesar buktiPengeluaranKas = new LaporanKasBesar(kasBesar);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setVisible(false);
		File file = Report.generateFileReport(Report.PDF, buktiPengeluaranKas.generateParameter(), "akunting/kasBesar",
				ais.ui.util.WaktuUtil.getDate(), null, new Toolbar());
		return file;
	}

	/**
	 * Menampilkan laporan cetak kas besar langsung di browser pengguna sebagai window modal.
	 *
	 * <p><b>Tujuan:</b> Memberikan pratinjau cetak kepada pengguna setelah menyimpan
	 * pengajuan baru atau dari tombol Cetak di baris grid, tanpa meninggalkan halaman.</p>
	 *
	 * <p><b>Cara kerja:</b> Sama dengan {@code PenggantianKasKecilAction.cetak()} —
	 * membuat {@link LaporanKasBesar}, mengkonfigurasinya, menempelkan ke root halaman,
	 * dan memanggil {@code onModal()}.</p>
	 *
	 * @param kasBesar entitas yang akan dicetak; tidak boleh null
	 * @throws Exception jika template laporan tidak ditemukan atau terjadi kesalahan rendering
	 */
	public static void cetak(KasBesar kasBesar) throws Exception {
		LaporanKasBesar buktiPengeluaranKas = new LaporanKasBesar(kasBesar);
		buktiPengeluaranKas.setTitle("Laporan");
		buktiPengeluaranKas.setClosable(true);
		buktiPengeluaranKas.setHeight("90%");
		buktiPengeluaranKas.setWidth("900px");
		buktiPengeluaranKas.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		buktiPengeluaranKas.onModal();
	}

	/**
	 * Implementasi {@code DataInitDefault.init()} — membuka form edit/lihat untuk
	 * objek yang diberikan dari luar kelas ini (misalnya dari alur SOP).
	 *
	 * @param obj objek domain; akan di-cast ke {@link KasBesar}
	 * @throws Exception jika cast gagal atau terjadi kesalahan saat membangun form
	 */
	@Override
	public void init(GeneralValueObject obj) throws Exception {
		kasBesar = (KasBesar) obj;
		init(kasBesar);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * Handler event ZK untuk tombol "Tambah" — membuka form kosong untuk membuat
	 * pengeluaran kas besar baru.
	 *
	 * <p><b>Cara kerja:</b> Menyetel {@code viewOnly = false}, membuat entitas
	 * {@link KasBesar} baru, memanggil {@code init()} untuk membangun window,
	 * lalu menampilkannya sebagai modal. Error saat membuka modal dilaporkan ke admin.</p>
	 *
	 * @param event event ZK yang memicu handler ini
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 */
	public void onAdd(Event event) throws Exception {
		viewOnly = false;
		init(new KasBesar());
		addWindow.setVisible(true);
		try {
			addWindow.onModal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan mengembalikan grid formulir pengeluaran kas besar yang dapat
	 * dilekatkan ke berbagai kontainer — window internal maupun form SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code FormSop.form()} yang menghasilkan widget
	 * formulir lengkap untuk entitas {@link KasBesar}, termasuk semua field input,
	 * grid rincian biaya yang dapat diedit, dan kontrol status persetujuan.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li>Menentukan mode ({@code setujui}, {@code viewOnly}) berdasarkan status dan
	 *       kondisi SOP.</li>
	 *   <li>Mengisi {@code satuanKerja} default dari pengguna aktif jika belum ada.</li>
	 *   <li>Merender field-field form: Satuan Kerja (AmbilDataSatuanKerjaBanbox),
	 *       Kode (label), checkbox "Ambil dari Kas Kecil", Judul Pengeluaran (textbox),
	 *       Penerima Kas Besar (combobox JenisKasBesar), Kas Kecil (combobox, hanya
	 *       visible jika ambilDariKasKecil dicentang), Unit, Akun Sumber, Akun Penerima,
	 *       Tanggal Pengajuan, Bukti Pengeluaran (upload/download), Keterangan.</li>
	 *   <li>Mendaftarkan listener pada satuan kerja dan checkbox ambilDariKasKecil yang
	 *       memuat ulang combobox JenisKasBesar dan KasKecil sesuai filter hierarki.</li>
	 *   <li>Mendaftarkan listener pada combobox JenisKasBesar yang memperbarui label
	 *       unit, akun sumber, dan akun penerima secara real-time.</li>
	 *   <li>Merender grid detail formula via {@code reloadFormula()} — dapat diedit
	 *       atau read-only tergantung mode.</li>
	 *   <li>Merender radio status pengajuan (Pengajuan/Disetujui/Ditolak) hanya dalam
	 *       kondisi yang sesuai.</li>
	 *   <li>Mendaftarkan atribut {@code eventListenerSetuju} dan listener perubahan
	 *       radio yang mengubah label tombol simpan dan visibilitas tanggal persetujuan.</li>
	 * </ol></p>
	 *
	 * @param generalValueObject objek data; akan di-cast ke {@link KasBesar}
	 * @param disposisiSop       disposisi SOP aktif atau null
	 * @param save               tombol simpan yang label-nya diubah secara dinamis
	 * @param setujuiData        listener tambahan pada radio status; null jika tidak ada
	 * @return {@link MyGrid} berisi seluruh komponen form
	 * @throws Exception jika terjadi kesalahan saat membangun komponen atau query DB
	 *
	 * <p><b>Pemeliharaan:</b> Method ini cukup panjang. Jika menambah field baru,
	 * pertimbangkan untuk mengekstrak group field ke method helper private. Pastikan
	 * logika read-only ({@code persetujuan || setujui || viewOnly}) konsisten di semua
	 * field.</p>
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, final EventListener setujuiData) throws Exception {
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
		tbmuser = Common.getCurrentUser();
		kasBesar = (KasBesar) generalValueObject;
		if (satuanKerjaTreeModel == null) {
			satuanKerjaTreeModel = new SatuanKerjaTreeModel(false);
		}
		setujui = false;
		if (!persetujuan) {
			if (kasBesar != null && kasBesar.getStatus().equals(KasBesar.DISETUJU)) {
				setujui = true;
			} else {
				setujui = false;
			}
		}

		if (kasBesar != null && kasBesar.getStatus().equals(KasBesar.DISETUJU)) {
			setujui = true;
		}

		if (kasBesar.getDisposisiSop() != null && kasBesar.getDisposisiSop().getDisposisiSetuju() != null
				&& kasBesar.getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null
				&& kasBesar.getDisposisiSop().getDisposisiSetuju().getSelesai()) {
			viewOnly = true;
		}

		try {
			if (kasBesar.getSatuanKerja() == null) {
				kasBesar.setSatuanKerja(Common.getSatuanKerja());
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/KasBesarAction.java:945");
			// TODO: handle exception
		}

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
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
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja *"));
		satuanKerja = new AmbilDataSatuanKerjaBanbox(true);
		satuanKerja.setValue(kasBesar.getSatuanKerja() == null ? "" : kasBesar.getSatuanKerja().getNama());
		satuanKerja.setAttribute("satuanKerja", kasBesar.getSatuanKerja());
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(kasBesar.getSatuanKerja() == null ? "" : kasBesar.getSatuanKerja().getNama()));
		} else {
			row.appendChild(satuanKerja);
		}
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode"));
		if (kasBesar.getKode() == null) {
			String noAgenda = generateCode(false);
			kasBesar.setKode(noAgenda);
		}

		kode = new Label(kasBesar.getKode());
		if (persetujuan) {
			row.appendChild(new Label(kasBesar.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		ambilDariKasKecil = new MyCheckboxConfig("Ambil dari Kas Kecil");
		ambilDariKasKecil.setChecked(kasBesar.getAmbilDariKasKecil());
		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label("Ambil dari Kas Kecil ? " + (kasBesar.getAmbilDariKasKecil() ? "Ya" : "Tidak")));
		} else {
			row.appendChild(ambilDariKasKecil);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Judul Pengeluaran Kas Besar *"));
		nama = new Textbox(kasBesar.getNama());

		if (persetujuan || setujui || viewOnly) {
			row.appendChild(new Label(kasBesar.getNama()));
		} else {
			row.appendChild(nama);
		}

		nama.setWidth("90%");
		nama.setRows(3);

		rowPenerima = new MyFormRow();
		rowPenerima.setParent(rows);
		rowPenerima.appendChild(new ais.ui.util.MyLabelConfig("Penerima Kas Besar *"));
		jenisKasBesar = new Combobox();
		PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();

		SatuanKerja parent = pt == null ? null : pt.getSatuanKerja();
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		jenisKasBesar.setWidth("90%");

		if (jenisKasBesar.getChildren().size() == 1 && kasBesar.getJenisKasBesar() == null) {
			jenisKasBesar.setSelectedIndex(0);
		}

		if (persetujuan || setujui || viewOnly) {
			rowPenerima.appendChild(
					new Label(kasBesar.getJenisKasBesar() == null ? "" : kasBesar.getJenisKasBesar().getNama()));
		} else {
			rowPenerima.appendChild(jenisKasBesar);
		}

		jenisKasBesar.setReadonly(true);

		kasKecil = new Combobox();
		rowkasKecil = new MyFormRow();
		rowkasKecil.setParent(rows);
		rowkasKecil.appendChild(new ais.ui.util.MyLabelConfig("Kas Kecil *"));

		Common.insertCombo(kasKecil, new String[] { "kode", "nama", "jenisKasKecil" }, "keterangan", KasKecil.class,

				Restrictions.and(Restrictions.eq("status", KasKecil.DISETUJU),
						Restrictions.and(
								Restrictions.or(Restrictions.isNull("satuanKerja"),
										Restrictions.or(
												satuanKerjas.isEmpty() ? Restrictions.sqlRestriction("true")
														: Restrictions.or(
																parent == null ? Restrictions.isNull("satuanKerja")
																		: Restrictions.sqlRestriction("false"),
																Restrictions.in("satuanKerja", satuanKerjas)),
												Restrictions.eq("satuanKerja", tbmuser.ambilSatuanKerja()))),

								Restrictions.and(Restrictions.isNull("penggantianKasKecil"),
										Restrictions.eq("aktif", true)))));

		Common.selectComboItem(true, kasKecil, kasBesar.getKasKecil());

		kasKecil.setReadonly(true);
		kasKecil.setWidth("90%");

		if (persetujuan || setujui || viewOnly) {
			rowkasKecil.appendChild(new Label(kasBesar.getKasKecil() == null ? ""
					: kasBesar.getKasKecil().getKode() + "-" + kasBesar.getKasKecil().getNama()));
		} else {
			rowkasKecil.appendChild(kasKecil);
		}

		final EventListener eventListenerKas = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SatuanKerja parent = (SatuanKerja) satuanKerja.getAttribute("satuanKerja");

				if (parent != null) {

					Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
					if (parent != null) {
						satuanKerjas.clear();
						satuanKerjas.add(parent);
						satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
					}

					if (tbmuser != null && tbmuser.ambilSatuanKerja() != null) {
						satuanKerjas.add(tbmuser.ambilSatuanKerja());
					}

					if (ambilDariKasKecil.isChecked()) {
						Common.insertCombo(kasKecil, new String[] { "kode", "nama", "jenisKasKecil" }, "keterangan",
								KasKecil.class,

								Restrictions
										.and(Restrictions.eq("status", KasKecil.DISETUJU),
												Restrictions.and(
														Restrictions.or(Restrictions.isNull("satuanKerja"),
																Restrictions.or(
																		satuanKerjas.isEmpty()
																				? Restrictions.sqlRestriction("true")
																				: Restrictions.in("satuanKerja",
																						satuanKerjas),
																		Restrictions.eq("satuanKerja",
																				tbmuser.ambilSatuanKerja()))),

														Restrictions.and(Restrictions.isNull("penggantianKasKecil"),
																Restrictions.eq("aktif", true)))));

						Common.selectComboItem(true, kasKecil, kasBesar.getKasKecil());
					}

					Common.insertCombo(jenisKasBesar, new String[] { "kode", "nama", "keterangan" }, "akun",
							JenisKasBesar.class,

							Restrictions
									.and(Restrictions.eq("aktif", true),
											Restrictions.or(Restrictions.isNull("satuanKerja"),
													Restrictions.or(
															satuanKerjas.isEmpty()
																	? Restrictions.sqlRestriction("true")
																	: Restrictions.or(
																			parent == null
																					? Restrictions.isNull("satuanKerja")
																					: Restrictions
																							.sqlRestriction("false"),
																			Restrictions.in("satuanKerja",
																					satuanKerjas)),
															Restrictions.eq("satuanKerja",
																	tbmuser.ambilSatuanKerja())))));

					if (kasBesar.getId() == null && parent != null && parent.getId() != null) {
						kasBesar.setSatuanKerja(parent);
					}

					Common.selectComboItem(true, jenisKasBesar, kasBesar.getJenisKasBesar());

					if (jenisKasBesar.getChildren().size() == 1 && kasBesar.getJenisKasBesar() == null) {
						jenisKasBesar.setSelectedIndex(0);
					}

				}

			}
		};

		satuanKerja.setEventListener(eventListenerKas);

		EventListener eventListenerAmbilDari = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowkasKecil.setVisible(ambilDariKasKecil.isChecked());
				eventListenerKas.onEvent(arg0);
			}
		};

		ambilDariKasKecil.addEventListener("onClick", eventListenerAmbilDari);
		Common.createDefaultTimer(eventListenerAmbilDari);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Unit"));
		final Label unit = new Label();
		row.appendChild(unit);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Sumber"));
		final Label akun = new Label();
		row.appendChild(akun);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Penerima"));
		final Label akunPenerima = new Label();
		row.appendChild(akunPenerima);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengajuan Kas Besar *"));
		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		tanggal = new MyDatebox(kasBesar.getTanggal());
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		if (persetujuan || setujui || viewOnly) {
			hbox.appendChild(new Label(Common.dateFormat6.get().format(kasBesar.getTanggal())));
		} else {
			tanggal.setParent(hbox);
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisKasBesar work = (JenisKasBesar) (jenisKasBesar.getSelectedItem() == null ? null
						: jenisKasBesar.getSelectedItem().getValue());

				kasBesar.setJenisKasBesar(work);
				kasBesar.setKode(kode.getValue());
				kasBesar.setNama(nama.getValue());
				kasBesar.setNilai(nilai);
				kasBesar.setKeterangan(keterangan.getValue());
				kasBesar.setTanggal(tanggal.getValue());
				kasBesar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
				kasBesar.setFormula(array.toString());

				String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
				if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
					kasBesar.setDisetujuiOleh(tbmuser);
					kasBesar.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
				} else {
					kasBesar.setDisetujuiOleh(null);
					kasBesar.setTanggalPersetujuan(null);
				}

				kasBesar.setStatus(sts);

				unit.setValue(work == null || work.getSatuanKerja() == null ? "" : work.getSatuanKerja().getNama());

				akun.setValue(work == null || work.getAkun() == null ? ""
						: work.getAkun().getKode() + "-" + work.getAkun().getNama());

				akunPenerima.setValue(work == null || work.getAkunPenerima() == null ? ""
						: work.getAkunPenerima().getKode() + "-" + work.getAkunPenerima().getNama());

			}
		};

		jenisKasBesar.addEventListener("onChange", eventListener);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Bukti Pengeluaran"));
		hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, kasBesar.getId(), KasBesar.class.getName(), "Bukti Pengeluaran",
				false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows, "Jika file bukti pengeluaran lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new Textbox(kasBesar.getKeterangan() == null ? "" : kasBesar.getKeterangan());

		if (setujui) {
			row.appendChild(new Label(kasBesar.getKeterangan() == null ? "" : kasBesar.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(2);

		nilai = 0.0;
		rowDetail = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(rowDetail, "2");
		rowDetail.setParent(rows);

		EventListener eventListenerDetail = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				KasKecil w = (KasKecil) (kasKecil.getSelectedItem() == null ? null
						: kasKecil.getSelectedItem().getValue());
				kasBesar.setAmbilDariKasKecil(ambilDariKasKecil.isChecked());
				kasBesar.setKasKecil(w);

				Common.clear(rowDetail);
				array = new JSONArray(kasBesar.getFormula());
				rowFormula = Common.tampilanScroll1(rowDetail);

				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(tanggal.getValue());

				reloadFormula(rowFormula, array, persetujuan, setujui,
						w != null && ambilDariKasKecil.isChecked() ? true : viewOnly, calendar.get(Calendar.YEAR));
			}
		};

		eventListenerDetail.onEvent(null);
		kasKecil.addEventListener("onChange", eventListenerDetail);

		row = new MyFormRow();
		row.setVisible(persetujuan && !viewOnly && (disposisiSop == null || disposisiSop.getId() == null));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
		status = new Radiogroup();
		Radio comboitem = new Radio(UangMuka.PENGAJUAN);
		comboitem.setAttribute("value", UangMuka.PENGAJUAN);
		comboitem.setValue(UangMuka.PENGAJUAN);
		comboitem.setVisible(false);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DISETUJU);
		comboitem.setAttribute("value", UangMuka.DISETUJU);
		comboitem.setValue(UangMuka.DISETUJU);
		status.appendChild(comboitem);
		comboitem = new Radio(UangMuka.DITOLAK);
		comboitem.setAttribute("value", UangMuka.DITOLAK);
		comboitem.setValue(UangMuka.DITOLAK);
		status.appendChild(comboitem);
		status.setWidth("90%");
		Common.selectRadioItem(status, kasBesar.getStatus());
		row.appendChild(status);

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("checkbox");
					if (selesai != null && selesai) {
						Common.selectRadioItem(status, UangMuka.DISETUJU);
						Common.freeze(status, true);
					} else {
						status.setSelectedItem(null);
						Common.freeze(status, false);
					}
				}
			}
		});

		if (setujuiData != null) {
			status.addEventListener("onClick", setujuiData);

			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					setujuiData.onEvent(new Event("", null, kasBesar.getStatus().equals(UangMuka.DISETUJU)));
				}
			});
		}

		if (setujui) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Status Pengajuan"));
			row.appendChild(new ais.ui.util.MyLabelConfig(kasBesar.getStatus()));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(kasBesar.getTanggalPersetujuanManual());

		if (kasBesar.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(
					Common.dateFormat1.get().format(kasBesar.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: kasBesar.getTanggalPersetujuanManual())));
		}

		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (kasBesar != null && kasBesar.getId() != null) {
					kasBesar.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(kasBesar);
				}
			}
		});

		Common.createDefaultTimer(eventListener);

		EventListener s = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				boolean setujui = status.getSelectedItem() == null ? false
						: status.getSelectedItem().getValue().equals(KasBesar.DISETUJU);

				if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
					if (tanggalPersetujuanManual.getValue() == null) {
						tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
					}
					tanggalPersetujuanManual.getParent().setVisible(setujui);
				}

				if (setujui) {
					save.setLabel("Selesaikan dan Setujui Kas Besar");
				} else {
					save.setLabel(!persetujuan ? "Simpan dan Cetak" : "Ubah Status Persetujuan dan Cetak");
				}
			}
		};

		status.addEventListener("onClick", s);
		Common.createDefaultTimer(s);

		return grid;
	}

	/**
	 * Menginisialisasi area formula dalam form kas besar dengan menambahkan tombol
	 * "Tambah Biaya" dan mengisi grid detail via {@code reloadDataFormula()}.
	 *
	 * <p><b>Tujuan:</b> Mempersiapkan container baris formula dalam form kas besar —
	 * menambahkan tombol tambah item baru dan merender semua item yang sudah ada.
	 * Method ini adalah entry point dari {@code form()} dan dipanggil setelah container
	 * baris siap.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Membuat {@link MyFormRow} baru ({@code rowU}) sebagai container grid detail.</li>
	 *   <li>Membuat tombol "Tambah Biaya" yang saat diklik menambahkan JSONObject baru
	 *       ke array dan memanggil ulang {@code reloadDataFormula()}. Tombol hanya
	 *       visible jika tidak dalam mode persetujuan/setujui/viewOnly.</li>
	 *   <li>Menambahkan {@code rowU} ke parent yang sama dengan {@code rowFormula}
	 *       (sebagai sibling, bukan child).</li>
	 *   <li>Memanggil {@code reloadDataFormula()} untuk mengisi grid dengan data awal.</li>
	 * </ol></p>
	 *
	 * @param rowFormula     baris container tempat tombol tambah dan grid detail dilekatkan
	 * @param array          JSONArray formula yang berisi semua item biaya
	 * @param persetujuan    true jika dalam mode persetujuan (read-only)
	 * @param setujui        true jika data sudah disetujui (read-only)
	 * @param viewOnly       true jika hanya bisa dilihat
	 * @param tahunWorkspace tahun anggaran untuk filter workspace yang relevan
	 * @throws Exception jika terjadi kesalahan saat membangun komponen
	 *
	 * <p><b>Pemeliharaan:</b> Method ini bersifat statik agar dapat dipanggil dari
	 * konteks lain (misalnya PenggantianKasKecilAction) dengan parameter yang sama.
	 * Jika signature berubah, perbarui semua pemanggil.</p>
	 */
	public static void reloadFormula(final Row rowFormula, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final Integer tahunWorkspace) throws Exception {
		final MyFormRow rowU = new MyFormRow();

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Biaya", "/img/svg/addthis.svg");
		button.setTooltiptext("Hapus Data");
		button.setVisible(!persetujuan && !setujui && !viewOnly);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("nama", "");
				jsonObject.put("qty", 0.0);
				jsonObject.put("harga", 0.0);
				jsonObject.put("jumlah", 0.0);
				Long key = Math.abs(Common.randLong());
				jsonObject.put("key", key);

				array.put(jsonObject);

				reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, tahunWorkspace);
			}
		});
		button.setParent(rowFormula);

		rowU.setParent(rowFormula.getParent());

		reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly, tahunWorkspace);

	}

	/**
	 * Merender ulang grid detail formula kas besar dari awal berdasarkan state JSON
	 * array saat ini, mendukung mode edit maupun read-only.
	 *
	 * <p><b>Tujuan:</b> Method inti yang membangun atau merender ulang tabel detail
	 * biaya kas besar. Dipanggil: saat form pertama kali dimuat, setelah tombol
	 * "Tambah Biaya" diklik, dan setelah tombol hapus baris dikonfirmasi. Bersifat
	 * statik agar dapat digunakan dari konteks lain.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li>Membersihkan konten {@code rowU} dengan {@code Common.clear()}.</li>
	 *   <li>Membuat grid dengan 7 kolom: Keterangan Biaya, Anggaran (workspace),
	 *       Tanggal, Qty, Harga, Jumlah, dan kolom aksi (hapus).</li>
	 *   <li>Menambahkan listener {@code hitungTotal} yang menjumlahkan semua item
	 *       di array dan menampilkannya di footer Jumlah.</li>
	 *   <li>Untuk setiap item dalam array yang memiliki {@code key} (bukan item yang
	 *       dihapus):
	 *       <ol>
	 *         <li>Mengambil workspace dari {@link ConstantValues}. Jika workspace null
	 *             tetapi akun ada, mencari workspace dari DB berdasarkan akun dan tahun;
	 *             fallback ke workspace dengan kode yang sama.</li>
	 *         <li>Membaca nama, qty, harga, jumlah, tanggal dari JSON. Membaca
	 *             nama_file dan link untuk lampiran URL.</li>
	 *         <li>Di mode edit: menampilkan {@link AmbilDataWorkspaceBanbox},
	 *             {@link MyDatebox}, {@link MyTextbox}, {@link MyDoublebox} qty dan harga.</li>
	 *         <li>Di mode read-only: menampilkan label-label biasa.</li>
	 *         <li>Menampilkan lampiran: jika ada {@link LampiranLain} dengan key ini,
	 *             menampilkan link; jika ada nama_file+link, menampilkan link popup;
	 *             jika belum ada, menampilkan widget upload.</li>
	 *         <li>Mendaftarkan listener pada setiap widget input yang memperbarui
	 *             JSON object dan menghitung ulang jumlah (qty * harga) secara real-time.</li>
	 *         <li>Tombol hapus (hanya di mode edit) meminta konfirmasi dan jika OK
	 *             mengosongkan item (put JSONObject kosong) dan memanggil ulang
	 *             {@code reloadDataFormula()}.</li>
	 *       </ol>
	 *   </li>
	 * </ol></p>
	 *
	 * @param rowU           container baris ZK tempat grid akan dilekatkan
	 * @param array          JSONArray formula berisi semua item biaya
	 * @param persetujuan    true jika dalam mode persetujuan (field read-only)
	 * @param setujui        true jika data sudah disetujui (field read-only)
	 * @param viewOnly       true jika hanya boleh dilihat
	 * @param tahunWorkspace tahun anggaran untuk lookup workspace dari DB
	 * @throws Exception jika terjadi kesalahan parsing JSON, query DB, atau rendering
	 *
	 * <p><b>Penanganan error:</b> Jika workspace tidak ditemukan di {@link ConstantValues}
	 * atau DB, field workspace ditampilkan kosong tanpa melempar exception. Error ini
	 * hanya tercetak di log via {@code System.out.println()} — pertimbangkan menggunakan
	 * logger yang proper.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini cukup panjang (~300 baris). Jika ada kebutuhan
	 * untuk menambah tipe field baru (misalnya nomor nota), tambahkan field JSON baru,
	 * widget input baru, dan perbarui listener {@code eventListener} untuk menyimpannya
	 * ke JSON. Perhatikan juga logika tampilan lampiran yang memiliki tiga cabang
	 * (LampiranLain ada / link ada / upload baru).</p>
	 */
	public static void reloadDataFormula(final Row rowU, final JSONArray array, final boolean persetujuan,
			final boolean setujui, final boolean viewOnly, final Integer tahunWorkspace) throws Exception {
		Common.clear(rowU);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(rowU);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Keterangan Biaya");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig("Anggaran");
		column.setParent(columns);
		column.setWidth("25%");

		column = new MyColumnConfig("Tanggal");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Qty");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("8%");

		column = new MyColumnConfig("Harga");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig("Jumlah");
		column.setAlign("right");
		column.setParent(columns);
		column.setWidth("12%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Foot foot = new Foot();
		foot.setParent(grid);

		Footer footer = new Footer("Total");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		footer = new Footer("");
		foot.appendChild(footer);

		final Footer footerTotal = new Footer("");
		foot.appendChild(footerTotal);

		footer = new Footer("");
		foot.appendChild(footer);

		Rows rows = new Rows();
		rows.setParent(grid);

		final EventListener hitungTotal = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Double nilai = 0.0;
				for (int i = 0; i < array.length(); i++) {
					Double jumlah = 0.0;
					JSONObject jsonObject = array.getJSONObject(i);
					if (!jsonObject.isNull("jumlah")) {
						jumlah = jsonObject.getDouble("jumlah");
					}
					nilai += jumlah;
				}
				footerTotal.setLabel(Common.numberFormat.get().format(nilai));
			}

		};

		hitungTotal.onEvent(null);
		Session session = HibernateUtil.currentSession();
		for (int i = 0; i < array.length(); i++) {
			final int index = i;
			final JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
			}

			if (key != null) {

				Workspace workspace = (Workspace) (jsonObject.isNull("workspace") ? null
						: ConstantValues.ambil(Workspace.class.getName(),
								new BigDecimal(jsonObject.get("workspace") + "").longValue()));
				System.out.println("workspace -> " + workspace);
				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(),
								ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));
				if (workspace == null && akunBiaya != null) {
					workspace = (Workspace) ConstantValues.simpleObject(
							session.createCriteria(Workspace.class)
									.add(Restrictions.or(Restrictions.eq("carryOver", true),
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true))))
									.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
									.add(Restrictions.eq("akun", akunBiaya)).addOrder(Order.desc("id"))
									.setMaxResults(1),
							Workspace.class);

					if (workspace == null || !workspace.getAktif()) {
						workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
								.add(Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
								.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
								.add(Restrictions.eq("kode", akunBiaya.getKode())).addOrder(Order.desc("id"))
								.setMaxResults(1), Workspace.class);
					}

					System.out.println("workspace -> " + workspace + ", akunBiaya -> " + akunBiaya
							+ ", tahunWorkspace -> " + tahunWorkspace);
				}

				String nama = "";

				if (!jsonObject.isNull("nama")) {
					nama = jsonObject.get("nama") + "";
				}

				Double qty = 0.0;
				if (!jsonObject.isNull("qty")) {
					qty = jsonObject.getDouble("qty");
				}

				Double harga = 0.0;
				if (!jsonObject.isNull("harga")) {
					harga = jsonObject.getDouble("harga");
				}

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				Date tanggal = WaktuUtil.getDate();
				if (!jsonObject.isNull("tanggal")) {
					tanggal = Common.dateFormat9.get().parse(jsonObject.getString("tanggal"));
				}

				String nama_file = "";

				if (!jsonObject.isNull("nama_file")) {
					nama_file = jsonObject.get("nama_file") + "";
				}

				String link = "";

				if (!jsonObject.isNull("link")) {
					link = jsonObject.get("link") + "";
				}

				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);

				final AmbilDataWorkspaceBanbox workspaceBanbox = new AmbilDataWorkspaceBanbox(false);
				workspaceBanbox.setAttribute("workspace", workspace);
				workspaceBanbox.setValue(workspace == null ? "" : workspace.toString());
				workspaceBanbox.setWidth("95%");
				workspaceBanbox.setReadonly(true);

				final Label nilai = new Label(Common.numberFormat.get().format(jumlah));

				final MyTextbox targetText = new MyTextbox(nama);

				final MyDatebox myTanggal = new MyDatebox(tanggal);
				myTanggal.setFormat(Common.dateFormat1.get().toPattern());
				myTanggal.setWidth("95%");

				Vbox myvbox = new Vbox();
				myvbox.setParent(row);
				myvbox.setWidth("95%");

				final MyDoublebox qtyBox = new MyDoublebox(qty);
				final MyDoublebox hargaBox = new MyDoublebox(harga);

				targetText.setWidth("95%");
				qtyBox.setWidth("95%");
				hargaBox.setWidth("95%");

				if (persetujuan || setujui || viewOnly) {
					if (persetujuan) {
						row.appendChild(new Label(workspace == null ? "" : workspace.getNama()));
					} else {
						row.appendChild(workspaceBanbox);
					}
					row.appendChild(new Label(Common.dateFormat3.get().format(tanggal)));
					myvbox.appendChild(new Label(nama));
					row.appendChild(new Label(Common.numberFormat.get().format(qty)));
					row.appendChild(new Label(Common.numberFormat.get().format(harga)));
				} else {
					row.appendChild(workspaceBanbox);
					row.appendChild(myTanggal);
					myvbox.appendChild(targetText);
					row.appendChild(qtyBox);
					row.appendChild(hargaBox);
				}
				final LampiranLain lampiranLain = LampiranLain.ambil(key, "Dokumen Kas Besar");

				if (lampiranLain != null) {

					A a = new A(lampiranLain.getNama());
					a.setParent(myvbox);
					a.setWidth("95%");

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.display(lampiranLain);
						}
					});

				}

				else if (!nama_file.isEmpty() && !link.isEmpty()) {

					A a = new A(nama_file);
					a.setParent(myvbox);
					a.setWidth("95%");
					final String url = link;
					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Clients.evalJavaScript(
									"popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
						}
					});

				} else {

					Hbox hbox = new Hbox();
					hbox.setParent(myvbox);
					LampiranLain.createDownloadUploadFileLain(hbox, key, "Dokumen Kas Besar", "Bukti", false,
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									LampiranLain lampiranLain = (LampiranLain) arg0.getData();
									jsonObject.put("link", lampiranLain.createLinkUri(false));
									jsonObject.put("nama_file", lampiranLain.getNama());
									jsonObject.put("id_file", lampiranLain.getId());
								}
							}, null, false, false, false, !(persetujuan || setujui || viewOnly));
				}

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						Workspace workspace = (Workspace) workspaceBanbox.getAttribute("workspace");
						jsonObject.put("akun",
								workspace == null || workspace.getAkun() == null ? null : workspace.getAkun().getId());

						BigDecimal bigDecimal = workspace == null ? null : new BigDecimal(workspace.getId());

						jsonObject.put("workspace", workspace == null ? null : bigDecimal.toString());
						jsonObject.put("nama", targetText.getValue());
						jsonObject.put("qty", qtyBox.getValue());
						jsonObject.put("harga", hargaBox.getValue());
						jsonObject.put("tanggal", Common.dateFormat9.get()
								.format(myTanggal.getValue() == null ? WaktuUtil.getDate() : myTanggal.getValue()));

						Double jumlah = (qtyBox.getValue() == null ? 0.0 : qtyBox.getValue())
								* (hargaBox.getValue() == null ? 0.0 : hargaBox.getValue());
						jsonObject.put("jumlah", jumlah);
						nilai.setValue(Common.numberFormat.get().format(jumlah));

						hitungTotal.onEvent(null);
					}
				};

				targetText.setRows(2);

				workspaceBanbox.setEventListener(eventListener);

				qtyBox.addEventListener("onChange", eventListener);
				targetText.addEventListener("onChange", eventListener);
				hargaBox.addEventListener("onChange", eventListener);
				myTanggal.addEventListener("onChange", eventListener);
				nilai.setParent(row);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
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
												array.put(index, new JSONObject());

												reloadDataFormula(rowU, array, persetujuan, setujui, viewOnly,
														tahunWorkspace);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												MyMessageboxConfig.show(
														"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
																+ e.getMessage());
											}

										}

									}
								});

					}
				});

				if (persetujuan || setujui || viewOnly) {
					new Label().setParent(row);
				} else {
					button.setParent(row);
				}
			}
		}
	}

	/**
	 * Membangun konten window modal untuk form pengeluaran kas besar dan mengkonfigurasi
	 * tombol aksi (Batal dan Simpan).
	 *
	 * <p><b>Tujuan:</b> Method internal yang digunakan oleh {@code onAdd()} dan
	 * {@code init(GeneralValueObject)} untuk menyiapkan window modal yang berisi form
	 * lengkap pengeluaran kas besar. Tombol simpan dikonfigurasi untuk memanggil
	 * {@code onSave()}, menutup window, dan menjadwalkan refresh grid.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Menyetel judul dan mengisi metadata pembuatan jika belum ada.</li>
	 *   <li>Membuat Borderlayout dengan Center (form) dan South (toolbar).</li>
	 *   <li>Memanggil {@code form()} dan menempelkan hasilnya ke Center.</li>
	 *   <li>Membuat tombol Batal (tutup window) dan Simpan (onSave).</li>
	 *   <li>Jika data sudah disetujui dan bukan mode persetujuan, menyembunyikan
	 *       tombol Simpan dan mengubah label Batal menjadi "Tutup".</li>
	 * </ol></p>
	 *
	 * @param kasBesar objek yang akan diedit atau dibuat baru; jika id null maka entitas baru
	 * @throws Exception jika terjadi kesalahan saat membangun komponen UI
	 */
	private void init(final KasBesar kasBesar) throws Exception {
		addWindow.setTitle("Pengeluaran Kas Besar");

		if (kasBesar.getDibuatOleh() == null) {
			kasBesar.setDibuatOleh(tbmuser);
			kasBesar.setTanggalPembuatan(new Date());
		}

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan dan Cetak", "/img/save.gif");

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(addWindow);
		Center center = new Center();
		center.setParent(borderlayout);
		disposisiSop = null;
		center.appendChild(form(kasBesar, disposisiSop, save, null));
		ais.ui.util.ZkCompat.setFlex(center, true);

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

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					addWindow.setVisible(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		if (!persetujuan && setujui) {
			save.setVisible(false);
			cancel.setLabel("Tutup");
		}

	}

	/**
	 * Memvalidasi input form dan menyimpan entitas {@link KasBesar} ke database,
	 * lalu memicu proses async berupa upload lampiran, pembuatan transfer, dan cetak.
	 *
	 * <p><b>Tujuan:</b> Method inti penyimpanan yang menangani validasi lengkap,
	 * persistensi entitas, pembaruan relasi kas kecil, upload lampiran via sesi
	 * streaming, dan pemicu proses lanjutan secara async.</p>
	 *
	 * <p><b>Cara kerja langkah demi langkah:</b>
	 * <ol>
	 *   <li><b>Validasi satuan kerja:</b> Jika null, tampilkan peringatan dan return false.</li>
	 *   <li><b>Validasi kas kecil:</b> Jika "Ambil dari Kas Kecil" dicentang tetapi tidak
	 *       ada kas kecil dipilih, tampilkan peringatan dan return false.</li>
	 *   <li><b>Validasi formula:</b> Iterasi JSON array untuk memastikan setiap item
	 *       memiliki jumlah > 0. Jika ada yang 0, tampilkan peringatan dan return false.</li>
	 *   <li><b>Kalkulasi nilai total:</b> Jumlahkan semua jumlah dari item valid.</li>
	 *   <li><b>Validasi field wajib:</b> Cek nama, jenis kas besar, dan tanggal. Jika
	 *       ada yang kosong/null, tampilkan peringatan dan return false.</li>
	 *   <li><b>Muat ulang entitas:</b> Jika sudah ada di DB, muat ulang via session.load().</li>
	 *   <li><b>Set semua metadata:</b> ambilDariKasKecil, kasKecil, jenisKasBesar, kode
	 *       (generate baru jika perlu), nama, nilai, keterangan, tanggal, satuan kerja,
	 *       formula, status persetujuan (dengan/tanpa penyetuju).</li>
	 *   <li><b>Simpan ke DB:</b> update atau save tergantung apakah entitas baru.</li>
	 *   <li><b>Update relasi kas kecil:</b> Jika kas kecil dipilih, set {@code kasBesar}
	 *       pada entitas kas kecil.</li>
	 *   <li><b>Upload lampiran:</b> Jika ada lampiran baru, memperbarui referensi
	 *       {@code ref} menggunakan sesi streaming terpisah; rollback jika gagal.</li>
	 *   <li><b>Async post-save:</b> Jika status DISETUJU, membuat DaftarPengajuanTransfer.
	 *       Menjalankan cetak dengan delay 2,5 detik.</li>
	 * </ol></p>
	 *
	 * @param event event ZK pemicu; tidak digunakan langsung
	 * @return {@code true} jika berhasil disimpan; {@code false} jika ada validasi gagal
	 * @throws Exception jika terjadi kesalahan DB yang tidak terduga
	 *
	 * <p><b>Penanganan error:</b> Validasi ditangani dengan MessageBox dan return false.
	 * Error pada upload lampiran ditangani dengan rollback dan pelaporan ke admin,
	 * tanpa membatalkan penyimpanan utama.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Blok validasi harus dipanggil sebelum operasi DB apapun.
	 * Jika menambah field wajib baru, tambahkan validasinya di atas pemanggilan
	 * {@code session.load()}. Perhatikan bahwa lampiran menggunakan sesi streaming
	 * berbeda — jangan mencampur operasi dari sesi utama dan streaming dalam satu
	 * transaksi.</p>
	 */
	public boolean onSave(Event event) throws Exception {

		if (satuanKerja.getAttribute("satuanKerja") == null) {
			MyMessageboxConfig.show("Mohon maaf, Satuan Kerja belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Satuan Kerja dari field pencarian yang tersedia; (2) Pastikan data Satuan Kerja sudah tercatat di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		KasKecil w = (KasKecil) (kasKecil == null || kasKecil.getSelectedItem() == null ? null
				: kasKecil.getSelectedItem().getValue());
		if (ambilDariKasKecil.isChecked()) {
			if (w == null || w.getId() == null) {
				MyMessageboxConfig.show("Mohon maaf, Kas Kecil belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Kas Kecil dari dropdown yang tersedia; (2) Pastikan data kas kecil sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		kasBesar.setFormula(array.toString());
		JSONArray array = new JSONArray(kasBesar.getFormula());
		for (int i = 0; i < array.length(); i++) {

			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
			}

			if (key != null) {

				Double jumlah = 0.0;
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}

				if (jumlah.intValue() == 0) {
					MyMessageboxConfig.show("Mohon maaf, ada nilai biaya pengeluaran yang masih nol. Langkah yang dapat dilakukan: (1) Periksa setiap baris rincian biaya dan isikan nominal yang valid; (2) Pastikan semua nilai biaya lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return false;
				}
			}
		}

		nilai = 0.0;
		for (int i = 0; i < array.length(); i++) {
			Double jumlah = 0.0;
			JSONObject jsonObject = array.getJSONObject(i);
			Long key = null;
			if (!jsonObject.isNull("key")) {
				key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
			}

			if (key != null) {
				if (!jsonObject.isNull("jumlah")) {
					jumlah = jsonObject.getDouble("jumlah");
				}
				nilai += jumlah;
			}
		}

		JenisKasBesar besar = (JenisKasBesar) (jenisKasBesar.getSelectedItem() == null ? null
				: jenisKasBesar.getSelectedItem().getValue());

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Judul Pengeluaran Kas Besar belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Judul Pengeluaran dengan deskripsi singkat yang jelas; (2) Pastikan judul tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (besar == null) {
			MyMessageboxConfig.show("Mohon maaf, Jenis Kas Besar belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Jenis Kas Besar dari dropdown yang tersedia; (2) Pastikan jenis kas besar yang dibutuhkan sudah terdaftar di master data; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Laporan belum diisi. Langkah yang dapat dilakukan: (1) Isikan atau pilih Tanggal Laporan menggunakan date picker; (2) Pastikan tanggal yang dipilih valid sesuai periode berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (kasBesar.getId() != null) {
			kasBesar = (KasBesar) session.load(KasBesar.class, kasBesar.getId());
		}

		if (kasBesar.getDibuatOleh() == null) {
			kasBesar.setDibuatOleh(tbmuser);
			kasBesar.setTanggalPembuatan(new Date());
		}
		if (disposisiSop != null && disposisiSop.getId() != null) {
			kasBesar.setDisposisiSop(disposisiSop);
		}
		kasBesar.setAmbilDariKasKecil(ambilDariKasKecil.isChecked());
		kasBesar.setKasKecil(w);
		kasBesar.setJenisKasBesar(besar);
		kasBesar.setKode(kode.getValue());
		kasBesar.setNama(nama.getValue());
		kasBesar.setNilai(nilai);
		kasBesar.setKeterangan(keterangan.getValue());
		kasBesar.setTanggal(tanggal.getValue());
		kasBesar.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));
		kasBesar.setFormula(array.toString());
		String sts = (String) (status.getSelectedItem() == null ? null : status.getSelectedItem().getValue());
		if (sts != null && sts.equals(DanaTalangan.DISETUJU)) {
			kasBesar.setDisetujuiOleh(tbmuser);
			kasBesar.setTanggalPersetujuan(tanggalPersetujuanManual.getValue());
		} else {
			kasBesar.setDisetujuiOleh(null);
			kasBesar.setTanggalPersetujuan(null);
		}
		kasBesar.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
		kasBesar.setStatus(sts);

		if (kasBesar.getId() != null) {
			session.update(kasBesar);
		} else {
			kasBesar.setDibuatOleh(tbmuser);
			String noAgenda = generateCode(true);
			kode.setValue(noAgenda);
			kasBesar.setKode(kode.getValue());
			session.save(kasBesar);
		}

		try {
			if (w != null && w.getId() != null) {
				session.refresh(w);
				w.setKasBesar(kasBesar);
				Common.refreshUpdate(session, w);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/KasBesarAction.java:2147");
			// TODO: handle exception
		}

		session.flush();

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(kasBesar.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (kasBesar.getStatus().equals(DanaTalangan.DISETUJU)) {
					DaftarPengajuanTransfer.simpanKasBesar(KasBesarAction.this.kasBesar);
				}

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						cetak(KasBesarAction.this.kasBesar);
					}
				}, "Proses cetak", false, 2500);

			}
		});

		return true;
	}

	/**
	 * Membangun {@link Criteria} Hibernate untuk query {@link KasBesar} berdasarkan
	 * semua filter aktif yang dipilih pengguna di panel pencarian.
	 *
	 * <p><b>Tujuan:</b> Menyediakan satu titik pembuatan kriteria query yang digunakan
	 * bersama oleh {@code onSearchDefault()} dan infrastruktur ekspor. Filter yang
	 * didukung: satuan kerja hierarkis, tanggal pembuatan, status aktif, kode, nama,
	 * dan jenis kas besar.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Guard: jika {@code searchparent} null, return null.</li>
	 *   <li>Membangun hierarki satuan kerja dari parent yang dipilih.</li>
	 *   <li>Menyiapkan filter tanggal dengan null-guard: jika start/end null, gunakan
	 *       {@code sqlRestriction("true")} sebagai fallback aman (tidak NPE).</li>
	 *   <li>Membangun Criteria dengan filter: tanggal, satuan kerja (OR null/hierarki),
	 *       aktif, kode ILIKE, nama ILIKE.</li>
	 *   <li>Jika filter jenis diisi, membuat alias join ke {@code jenisKasBesar} dan
	 *       menambahkan filter ILIKE pada nama/kode jenis.</li>
	 *   <li>Menambahkan ORDER BY id DESC jika {@code order} true.</li>
	 * </ol></p>
	 *
	 * @param order {@code true} untuk menambahkan ordering; {@code false} untuk count paging
	 * @return {@link Criteria} siap eksekusi, atau {@code null} jika UI belum siap
	 *
	 * <p><b>Penanganan error:</b> Guard null pada start/end mencegah NPE saat
	 * {@code Common.databaseDateFormat.get().format(null)} dipanggil. Ini berbeda dari
	 * implementasi awal yang tidak memiliki guard dan bisa NPE.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Filter status sengaja tidak ada di sini (berbeda dengan
	 * PenggantianKasKecilAction) meskipun ada combobox searchstatus di ZUL. Jika perlu
	 * menambahkan filter status, tambahkan setelah filter aktif.</p>
	 */
	public Criteria initCriteria(boolean order) {
		if (searchparent == null) return null;

		SatuanKerja parent = (SatuanKerja) searchparent.getAttribute("satuanKerja");
		Set<SatuanKerja> satuanKerjas = ais.action.master.sekolah.util.SekolahUtil.ambilSatuanKerjas();
		if (parent != null) {
			satuanKerjas.clear();
			satuanKerjas.add(parent);
			satuanKerjaTreeModel.getChildsSet(parent, satuanKerjas);
		}

		// Guard: filter tanggal bisa kosong (start/end null) → jangan format(null) yang NPE.
		java.util.Date startVal = start == null ? null : start.getValue();
		java.util.Date endVal = end == null ? null : end.getValue();
		org.hibernate.criterion.Criterion tglCriterion = (startVal != null && endVal != null)
				? Restrictions.sqlRestriction("date(this_.tanggal_pembuatan) between date('"
						+ Common.databaseDateFormat.get().format(startVal) + "') and date('"
						+ Common.databaseDateFormat.get().format(endVal) + "')")
				: Restrictions.sqlRestriction("true");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KasBesar.class)

				.add(tglCriterion)

				.add(Restrictions.or(Restrictions.isNull("satuanKerja"),
						satuanKerjas.size() == 0 ? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										parent == null ? Restrictions.isNull("satuanKerja")
												: Restrictions.sqlRestriction("false"),
										Restrictions.in("satuanKerja", satuanKerjas))))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))
				.add(serachkode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("kode", serachkode.getValue().trim(), MatchMode.ANYWHERE))
				.add(serachnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.ilike("nama", serachnama.getValue().trim(), MatchMode.ANYWHERE));

		if (serachjenis != null && !serachjenis.getValue().trim().isEmpty()) {
			criteria.createAlias("jenisKasBesar", "jenisKasBesar")
					.add(serachjenis.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("jenisKasBesar.nama", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("jenisKasBesar.kode", serachjenis.getValue().trim(),
											MatchMode.ANYWHERE)));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));
		return criteria;
	}

	/**
	 * Memuat ulang data grid kas besar berdasarkan filter aktif dengan paginasi.
	 *
	 * <p><b>Tujuan:</b> Entry point utama untuk refresh tampilan data kas besar.
	 * Dipanggil pada inisialisasi (via timer), setelah simpan, setelah perubahan
	 * filter, dan setelah navigasi halaman paging.</p>
	 *
	 * <p><b>Cara kerja:</b> Menghitung jumlah total record untuk paging, lalu
	 * mengambil halaman aktif dengan setMaxResults dan setFirstResult berdasarkan
	 * nomor halaman aktif dari komponen paging. Mengisi grid menggunakan
	 * {@code KasBesarRenderer} dan {@code setModelCheckMobile()}.</p>
	 *
	 * @param event event ZK pemicu; boleh null jika dipanggil programatis
	 */
	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<KasBesar> kasBesar = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(kasBesar);
		grid.setRowRenderer(new KasBesarRenderer());
		grid.setModelCheckMobile(strset);

	}

	/**
	 * Mengembalikan istilah domain dalam bahasa Indonesia untuk modul ini.
	 *
	 * @return {@code "Pengeluaran Kas Besar"}
	 * @throws Exception tidak akan terjadi
	 */
	public String istilah() throws Exception {
		return "Pengeluaran Kas Besar";
	}

	/**
	 * Mengembalikan entitas {@link KasBesar} aktif sebagai {@link DataSop} untuk
	 * keperluan sistem alur SOP.
	 *
	 * @return entitas yang sedang aktif di form
	 * @throws Exception tidak akan terjadi
	 */
	@Override
	public DataSop ambil() throws Exception {
		return kasBesar;
	}

	/**
	 * Mengembalikan kelas entitas domain yang dikelola kelas ini untuk refleksi generik.
	 *
	 * @return {@code KasBesar.class}
	 * @throws Exception tidak akan terjadi
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		return KasBesar.class;
	}

	/**
	 * Menghasilkan kode unik untuk entitas {@link KasBesar} berdasarkan konfigurasi
	 * nomor surat kas besar yang aktif di {@link NomorSuratAlurKeuangan#KAS_BESAR_DATA}.
	 *
	 * <p><b>Tujuan:</b> Membuat nomor dokumen kas besar yang mengikuti format, urutan,
	 * dan aturan reset yang dikonfigurasi administrator. Fallback ke barcode acak jika
	 * konfigurasi tidak tersedia.</p>
	 *
	 * <p><b>Cara kerja:</b> Sama dengan {@code PenggantianKasKecilAction.generateCode()} —
	 * menggunakan counter tersimpan atau rowCount DB, memformat kode, menaikkan counter
	 * jika {@code tambah = true}, dan memastikan keunikan via {@code KodeUnikUtil}.</p>
	 *
	 * @param tambah {@code true} untuk menaikkan counter nomor surat (saat save entitas baru)
	 * @return string kode unik yang sudah diformat
	 *
	 * <p><b>Pemeliharaan:</b> Konfigurasi ada di {@code NomorSuratAlurKeuangan.KAS_BESAR_DATA}.
	 * Jangan panggil dengan {@code tambah = true} lebih dari sekali untuk entitas yang sama.</p>
	 */
	private String generateCode(boolean tambah) {
		if (NomorSuratAlurKeuangan.KAS_BESAR_DATA == null
				|| NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurKeuangan.KAS_BESAR_DATA.getNomorSurat().format(index, WaktuUtil.getDate());
		return ais.action.master.KodeUnikUtil.pastikanUnik(KasBesar.class, noAgenda);
	}

	/**
	 * Menghitung indeks urutan berikutnya untuk nomor surat kas besar dari jumlah
	 * record yang ada di database, dengan filter kondisional berdasarkan konfigurasi
	 * reset dan kelompok nomor surat.
	 *
	 * <p><b>Tujuan:</b> Alternatif dinamis dari counter tersimpan. Menghitung rowCount
	 * query {@link KasBesar} dengan JOIN ke {@code nomorSuratAlurKeuangan} dan filter
	 * berdasarkan properti {@link NomorSurat}: pengelompokan, reset per tahun/bulan,
	 * dan reset per tanggal. Mengembalikan rowCount + 1 sebagai indeks berikutnya.</p>
	 *
	 * @param nomorSurat konfigurasi nomor surat; jika null return 0
	 * @return indeks urutan berikutnya (jumlah record + 1), minimal 1
	 *
	 * <p><b>Pemeliharaan:</b> Identik pola-nya dengan getindex() di kelas-kelas Action
	 * keuangan lain. Jika ada kebutuhan reset per kondisi baru, tambahkan filter baru
	 * setelah blok reset yang sudah ada.</p>
	 */
	private Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(KasBesar.class)

				.createAlias("nomorSuratAlurKeuangan", "nomorSuratAlurKeuangan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurKeuangan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurKeuangan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang)) || nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	/**
	 * Mengubah mode persetujuan action ini secara programatis dari infrastruktur SOP.
	 *
	 * <p><b>Tujuan:</b> Implementasi {@code FormSop.setPersetujuan()} yang memungkinkan
	 * sistem SOP mengubah perilaku form antara mode pengajuan (pengguna biasa) dan mode
	 * persetujuan (pejabat berwenang) setelah action sudah diinstansiasi.</p>
	 *
	 * @param persetujuan {@code true} untuk mengaktifkan mode persetujuan;
	 *                    {@code false} untuk kembali ke mode pengajuan normal
	 *
	 * <p><b>Pemeliharaan:</b> Perubahan nilai ini akan tercermin pada render form
	 * berikutnya. Jika ada komponen tambahan yang perlu di-refresh saat mode berubah,
	 * tambahkan logika pembaruannya di sini.</p>
	 */
	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}
}
