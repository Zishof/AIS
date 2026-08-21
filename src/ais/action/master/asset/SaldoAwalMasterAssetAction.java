package ais.action.master.asset;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.zkoss.zul.Hbox;
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

import ais.action.master.asset.helper.AmbilDataPemesananPengadaanAsetBanbox;
import ais.action.master.asset.helper.AmbilDataPenerimaanPengadaanAsetBanbox;
import ais.action.master.asset.helper.AmbilDataPenyediaAssetBanbox;
import ais.action.master.asset.helper.BreakdownTagihanVendorHelper;
import ais.action.master.asset.helper.RevisiSaldoAwalMasterAssetHelper;
import ais.action.master.asset.helper.SaldoAwalMasterAssetDetailAction;
import ais.action.master.asset.helper.SaldoAwalPunyaMasterAssetHelper;
import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.report.format1.library.LaporanSaldoAwal;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Ruang;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.asset.JenisPajakBarang;
import ais.database.model.asset.Lokasi;
import ais.database.model.asset.NomorSuratAlurPengadaan;
import ais.database.model.asset.PembayaranPengadaanMasterAssetDetail;
import ais.database.model.asset.PemesananPengadaanMasterAsset;
import ais.database.model.asset.PemesananPengadaanMasterAssetDetail;
import ais.database.model.asset.PemilikAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAsset;
import ais.database.model.asset.PenerimaanPengadaanMasterAssetDetail;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.asset.SaldoAwalMasterAsset;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.database.model.surat.NomorSurat;
import ais.ui.util.DataCriteria;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;

/**
 * <h3>SaldoAwalMasterAssetAction — Pengelola Tagihan Vendor &amp; Saldo Awal Aset</h3>
 *
 * <p><b>Untuk apa</b><br>
 * Kelas ini adalah controller ZKoss (GenericAutowireComposer) untuk halaman
 * <em>Tagihan Vendor / Saldo Awal Aset</em> pada modul Asset Management.
 * Fungsi utamanya mencakup:</p>
 * <ul>
 *   <li>Menampilkan daftar tagihan vendor (saldo awal) dalam grid dengan paging,
 *       filter kode, keterangan, penyedia, tanggal, status lunas, dan status persetujuan.</li>
 *   <li>Membuat tagihan baru dari data Penerimaan Pengadaan (BAST) maupun tanpa BAST,
 *       dengan dukungan pembayaran bertahap berdasarkan termin (cicilan/milestone).</li>
 *   <li>Menyimpan rincian baris tagihan ({@code SaldoAwalMasterAssetDetail}) beserta
 *       perhitungan pajak PPh dan PPN per item aset.</li>
 *   <li>Mengelola alur persetujuan: approve (setujui) dan reject (batalkan persetujuan)
 *       langsung dari baris grid dengan update real-time pada label nama/tanggal.</li>
 *   <li>Mencetak laporan tagihan vendor dalam format PDF melalui JasperReports
 *       ({@code LaporanSaldoAwal}).</li>
 *   <li>Menghasilkan kode tagihan otomatis melalui {@code NomorSuratAlurPengadaan}
 *       dan memastikan keunikannya dengan {@code KodeUnikUtil.pastikanUnik()}.</li>
 *   <li>Menampilkan lampiran dokumen (faktur pajak, kwitansi, dll.) terkait Penerimaan
 *       Pengadaan langsung di baris grid.</li>
 * </ul>
 *
 * <p><b>Cara kerja</b><br>
 * Siklus hidup halaman dikelola oleh dua metode ZKoss lifecycle:</p>
 * <ol>
 *   <li>{@code doBeforeCompose} — memeriksa keamanan akses pengguna.</li>
 *   <li>{@code doAfterCompose} — inisialisasi komponen UI (grid, paging, filter,
 *       toolbar), menyetel rentang tanggal default 6 bulan ke belakang, mendaftarkan
 *       listener paging dan timer, serta menambah tombol "Cetak" dan "History" ke toolbar.</li>
 * </ol>
 * <p>Grid data dirender oleh inner class {@code SaldoAwalMasterAssetRenderer} yang
 * mengiterasi setiap {@link ais.database.model.asset.SaldoAwalMasterAsset} dan membangun
 * sel-sel: detail item, revisi, lampiran, penyedia, nilai, status lunas, pembuat,
 * persetujuan, dan tombol aksi (Cetak / Setujui / Batalkan / Ubah / Hapus).</p>
 * <p>Form tambah/ubah dibangun secara programatik oleh {@code init()} yang membuat
 * {@code MyWindow} berisi {@code Borderlayout}. Konten form (grid dua kolom label-input)
 * dirakit oleh {@code reloadData()} yang mendukung reload saat pengguna mengganti
 * Penerimaan Pengadaan atau termin. Penyimpanan dilakukan oleh {@code onSave()}.</p>
 *
 * <p><b>Threading</b><br>
 * Seluruh operasi database menggunakan {@code HibernateUtil.currentSession()} yang
 * terikat pada thread ZKoss event-thread (single-threaded per sesi ZK). Tindakan
 * ringan seperti auto-simpan {@code DaftarPengajuanTransfer} dan mencetak PDF setelah
 * simpan dilakukan melalui {@code Common.createDefaultTimer()} agar tidak memblokir
 * respons UI. Kelas ini <em>tidak</em> thread-safe bila dipanggil dari luar event-thread ZK.</p>
 *
 * <p><b>Pemeliharaan</b><br>
 * Untuk menambah kolom filter baru, tambahkan field komponen ZUL (di-wire otomatis oleh
 * {@code GenericAutowireComposer}) dan sertakan restriksi Hibernate yang sesuai di
 * {@code initCriteria()}. Format kode tagihan dikonfigurasi melalui
 * {@code NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA}. Jika template JasperReports
 * diubah, sesuaikan pula kunci parameter pada metode {@code parameter()}. Pastikan
 * kode lama tetap terbaca oleh {@code generateCode()} setelah perubahan format.</p>
 *
 * @author  Tim Pengembang AIS
 * @version 2026-06-18
 * @see     ais.database.model.asset.SaldoAwalMasterAsset
 * @see     ais.database.model.asset.SaldoAwalMasterAssetDetail
 * @see     ais.action.master.asset.helper.SaldoAwalPunyaMasterAssetHelper
 * @see     ais.action.master.asset.helper.SaldoAwalMasterAssetDetailAction
 */
public class SaldoAwalMasterAssetAction extends GenericAutowireComposer implements FormSop, DataCriteria {

	/** ID serialisasi untuk kompatibilitas versi kelas yang diserialisasi. */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchkode;
	private Textbox searchketerangan;
	private Textbox searchpenyedia;
	private Combobox searchLunas;

	private MyDatebox start;
	private MyDatebox end;

	private boolean tampilkanRuanganDamPemilikAset = Common.bolehKonfigurasi("tampilkanRuanganDamPemilikAset", Konfigurasi.TIDAK_AKTIF);

	private Label kode;
	private Combobox pemilikAsset;
	private Combobox lokasi;
	private AmbilDataRuangBanbox ruang;
	private MyTextbox keterangan;
	private MyDatebox tanggalPembuatan;

	private boolean edit = false;
	private boolean delete = false;
	private boolean approve = false;
	private boolean reject = false;

	private Textbox kodeTagihan;
	private MyDatebox tanggalTagihan;

	private SaldoAwalMasterAsset saldoAwalMasterAsset;
	private MyToolbarbuttonConfig add;
	private MyGrid gridMasterAsset;
	private AmbilDataPenyediaAssetBanbox penyedia;

	private MyCheckboxConfig lunasSaja;
	private MyCheckboxConfig blmLunasSaja;

	private MyCheckboxConfig blmDisetujui;
	private MyCheckboxConfig disetujui;
	private AmbilDataPenerimaanPengadaanAsetBanbox penerimaanPengadaanMasterAsset;
	private MyCheckboxConfig tanpaPenerimaan;
	private boolean persetujuan = false;
	private DisposisiSop disposisiSop = null;
	private SaldoAwalPunyaMasterAssetHelper saldoAwalPunyaMasterAssetHelper = null;
	private AmbilDataPemesananPengadaanAsetBanbox pemesananPengadaanMasterAsset;
	private PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAssetData = null;
	private PemesananPengadaanMasterAsset dataPemesanan = null;
	private Row rowTermin;
	private Combobox kodeTermin;
	private Row rowTerminProgres;
	private Label progresTermin;
	private Row rowTerminDokumen;
	private Vbox dokumenTermin;
	private MyDatebox tanggalPersetujuanManual;
	private Row rowData;

	/**
	 * <b>Tujuan</b><br>
	 * Konstruktor default tanpa argumen untuk instansiasi standar oleh ZKoss melalui
	 * refleksi. Digunakan saat controller dikaitkan dengan file ZUL biasa (bukan mode
	 * persetujuan). Semua field diinisialisasi dengan nilai default-nya: {@code persetujuan=false},
	 * semua referensi komponen UI {@code null} (akan di-wire oleh {@code doAfterCompose}).
	 *
	 * <p><b>Cara kerja</b><br>
	 * Hanya memanggil {@code super()} dari {@code GenericAutowireComposer} yang
	 * menyiapkan infrastruktur wiring komponen ZKoss. Tidak ada logika bisnis di sini.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Jangan hapus konstruktor ini — ZKoss memerlukannya untuk membuat instance controller
	 * via refleksi ketika halaman ZUL dimuat.</p>
	 */
	public SaldoAwalMasterAssetAction() {
		super();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Konstruktor dengan parameter mode persetujuan. Digunakan saat controller diinstansiasi
	 * secara programatik (bukan dari ZUL) untuk menampilkan form dalam mode persetujuan,
	 * di mana tombol Tambah, Ubah, dan Hapus disembunyikan dan hanya tindakan
	 * Setujui/Batalkan yang ditampilkan kepada pengguna.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Memanggil {@code super()} lalu menyetel field {@code persetujuan} sesuai argumen.
	 * Nilai {@code persetujuan=true} menyebabkan {@code doAfterCompose()} menyembunyikan
	 * tombol Tambah dan menonaktifkan hak edit/delete, serta {@code reloadData()} menampilkan
	 * field form sebagai {@code Label} (read-only) bukan input.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Lihat juga {@link #onAddExternal(EventListener, SaldoAwalMasterAsset)} yang
	 * menggunakan konstruktor ini secara tidak langsung dengan menyetel field {@code persetujuan}
	 * setelah konstruksi default.</p>
	 *
	 * @param persetujuan {@code true} untuk mode persetujuan (read-only form + approve/reject),
	 *                    {@code false} untuk mode pengelolaan penuh (tambah/ubah/hapus)
	 */
	public SaldoAwalMasterAssetAction(boolean persetujuan) {
		super();
		this.persetujuan = persetujuan;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode lifecycle ZKoss yang dipanggil sebelum komponen halaman dibuat. Berfungsi
	 * sebagai gerbang keamanan: jika pengguna tidak memiliki hak akses, proses pembuatan
	 * halaman dihentikan sejak dini sebelum komponen UI manapun dirender ke klien.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Memanggil {@code Common.doCheckSecurity()} yang memeriksa sesi dan hak akses
	 * pengguna yang sedang login. Jika pemeriksaan gagal, metode tersebut melempar
	 * exception atau melakukan redirect ke halaman login. Setelah pemeriksaan lulus,
	 * metode ini mendelegasikan ke implementasi superclass untuk melanjutkan siklus
	 * pembuatan komponen secara normal.</p>
	 *
	 * <p><b>Penanganan error</b><br>
	 * {@code Common.doCheckSecurity()} menangani pengalihan ke halaman login secara
	 * internal. Metode ini tidak perlu menangkap exception secara eksplisit.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Jangan hapus atau ubah pemeriksaan keamanan di sini. Ini adalah lapis pertahanan
	 * pertama terhadap akses tidak sah ke halaman tagihan vendor.</p>
	 *
	 * @param page     halaman ZKoss yang sedang dibuat
	 * @param parent   komponen induk dalam hierarki komponen
	 * @param compInfo metadata komponen dari file ZUL
	 * @return {@code ComponentInfo} yang diteruskan dari superclass untuk melanjutkan
	 *         proses pembuatan komponen
	 */
	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode lifecycle ZKoss yang dipanggil setelah seluruh komponen halaman selesai
	 * dibuat dan di-wire. Merupakan titik inisialisasi utama untuk seluruh logika UI:
	 * konfigurasi grid, filter, paging, toolbar, rentang tanggal default, hak akses
	 * tombol, dan registrasi event listener.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah inisialisasi secara berurutan:</p>
	 * <ol>
	 *   <li>Validasi sesi pengguna; jika tidak valid, hapus atribut sesi dan redirect ke
	 *       halaman logoff via {@code Common.goLogoff()}.</li>
	 *   <li>Terapkan style pada grid data melalui {@code styleDataGrid()}.</li>
	 *   <li>Set field tanggal {@code start} dan {@code end} sebagai readonly (input tanggal
	 *       hanya bisa diubah via datepicker).</li>
	 *   <li>Isi nilai default: {@code start} = 6 bulan lalu, {@code end} = besok.</li>
	 *   <li>Isi combo box status lunas dengan pilihan: Semua / Lunas / Belum Lunas.</li>
	 *   <li>Setel visibilitas tombol Tambah berdasarkan hak CREATE.</li>
	 *   <li>Baca hak edit/delete/approve/reject dari {@code CommonPrivilages}.</li>
	 *   <li>Jika mode persetujuan: sembunyikan Tambah, nonaktifkan edit dan delete.</li>
	 *   <li>Inisialisasi paging dengan listener yang memanggil {@code onSearchDefault}.</li>
	 *   <li>Daftarkan timer default untuk memuat data awal.</li>
	 *   <li>Tambah tombol "Cetak" (ekspor Excel) ke toolbar via {@code Common.cetakData()}.</li>
	 *   <li>Tambah tombol "History" ke toolbar untuk membuka {@code RevisiSaldoAwalMasterAssetHelper}.</li>
	 * </ol>
	 *
	 * <p><b>Threading</b><br>
	 * Dijalankan pada ZKoss event-thread. Timer default yang didaftarkan di sini
	 * menyebabkan {@code onSearchDefault} dipanggil sesaat setelah halaman selesai render.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Untuk menambah filter baru, deklarasikan field komponen ZK dan tambahkan logika
	 * inisialisasi di sini (misalnya mengisi combobox). Pastikan field memiliki id yang
	 * sama dengan nama field agar di-wire otomatis oleh {@code GenericAutowireComposer}.</p>
	 *
	 * @param comp komponen root halaman ZKoss yang telah selesai dibuat
	 * @throws Exception jika terjadi kesalahan saat inisialisasi komponen UI
	 */
	public void doAfterCompose(Component comp) throws Exception {
				super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (grid != null) {
			styleDataGrid(grid);
		}
		if (start != null) {
			if (start != null) start.setReadonly(true);
		}
		if (end != null) {
			if (end != null) end.setReadonly(true);
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) {
			if (start != null) start.setValue(calendar.getTime());
		}
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) {
			if (end != null) end.setValue(calendar.getTime());
		}

		if (searchLunas != null) {
			Comboitem comboitem = new Comboitem("Semua");
			searchLunas.appendChild(comboitem);
			comboitem = new Comboitem("Lunas");
			comboitem.setValue(1);
			searchLunas.appendChild(comboitem);
			comboitem = new Comboitem("Belum Lunas");
			comboitem.setValue(2);
			searchLunas.appendChild(comboitem);
			searchLunas.setReadonly(false);
			searchLunas.setAutodrop(true);
			searchLunas.setSelectedIndex(0);
		}

		if (add != null) {
			add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
			add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);
		reject = CommonPrivilages.checkPrevilages(CommonPrivilages.REJECT);

		if (persetujuan) {
			edit = false;
			delete = false;
			if (add != null) {
				add.setVisible(false);
			}
		}

		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		String[] contents = new String[] { "kode", "penyedia", "tanggalPembuatan", "tanggalPersetujuan", "dibuatOleh",
				"disetujuiOleh", "nilai", "dibayar", "penerimaanPengadaanMasterAsset", "satuanKerja", "tahun", "bulan",
				"disposisiSop", "kodeTermin", "keteranganTermin", "jsonTermin", "daftarPengajuanTransfer",
				"kodeTagihan", "tanggalTagihan", "keterangan", "aktif" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(SaldoAwalMasterAsset.class, this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				RevisiSaldoAwalMasterAssetHelper revisiHelper = new RevisiSaldoAwalMasterAssetHelper(
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										onSearchDefault(arg0);
									}
								});
							}
						});
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
				revisiHelper.setVisible(true);
				revisiHelper.onModal();

			}

		});
		Common.appendKeToolbar(button, add, comp);
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menangani klik tombol "Cetak" di toolbar utama halaman. Membuka popup modal yang
	 * menampilkan laporan saldo awal dalam format yang dapat dicetak menggunakan komponen
	 * {@code LaporanSaldoAwal} berbasis JasperReports.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Membuat instance baru {@code LaporanSaldoAwal}, mengatur properti tampilan (judul,
	 * tinggi 95%, lebar 90%, bisa ditutup), lalu menampilkannya sebagai modal overlay
	 * di atas halaman menggunakan {@code onModal()}. Pengguna dapat mencetak atau
	 * menyimpan laporan dari dalam popup tersebut.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Metode ini hanya membuka popup laporan generik. Untuk mencetak laporan spesifik
	 * per baris data, gunakan metode {@code cetak(SaldoAwalMasterAsset)} yang dipanggil
	 * dari tombol Cetak di setiap baris grid melalui {@code SaldoAwalMasterAssetRenderer}.</p>
	 *
	 * @param event event klik dari ZKoss (tidak digunakan secara langsung)
	 * @throws Exception jika terjadi kesalahan saat membuka popup laporan
	 */
	public void onCetak(Event event) throws Exception {
		LaporanSaldoAwal laporan = new LaporanSaldoAwal();
		laporan.setTitle("Cetak Laporan");
		page.getFirstRoot().appendChild(laporan);
		laporan.setHeight("95%");
		laporan.setWidth("90%");
		laporan.setClosable(true);
		laporan.onModal();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Membangun {@code Map} parameter yang diperlukan oleh template JasperReports untuk
	 * mencetak laporan tagihan vendor ({@code SaldoAwalMasterAsset}). Parameter mencakup
	 * data header tagihan, data Penerimaan Pengadaan, daftar rincian item aset, dan
	 * total nilai keseluruhan.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah yang dilakukan:</p>
	 * <ol>
	 *   <li>Membuat map kosong dengan ID acak via {@code HashMapGenerator.getRand()}.</li>
	 *   <li>Menyisipkan properti objek {@code SaldoAwalMasterAsset} ke dalam map dengan
	 *       prefix {@code "data"} menggunakan refleksi {@code Common.insertProperty()}.</li>
	 *   <li>Jika ada Penerimaan Pengadaan terkait, menyisipkan propertinya dengan prefix
	 *       {@code "terima"}.</li>
	 *   <li>Mengambil semua {@code SaldoAwalMasterAssetDetail} dari database dan
	 *       membangun daftar sub-map per item yang berisi: nama aset, kode, harga beli,
	 *       harga total, PPh, PPN, diskon, jumlah, penyedia, status persetujuan, dan
	 *       tanggal persetujuan.</li>
	 *   <li>Menghindari duplikasi baris dengan melacak ID aset yang sudah diproses
	 *       menggunakan {@code Set<Long>}.</li>
	 *   <li>Jika total nilai rincian berbeda dari nilai header, secara otomatis memperbarui
	 *       nilai header di database ({@code Common.refreshUpdate}).</li>
	 *   <li>Menempatkan daftar sub-map dan total keseluruhan ke dalam map parameter.</li>
	 * </ol>
	 *
	 * <p><b>Threading</b><br>
	 * Menggunakan {@code HibernateUtil.currentSession()} — harus dipanggil dari
	 * ZKoss event-thread yang memiliki sesi aktif.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Jika template JasperReports diubah dan membutuhkan parameter baru, tambahkan
	 * {@code parameters.put("kunci_baru", nilai)} di sini. Pastikan kunci yang
	 * digunakan konsisten dengan nama variabel dalam file JRXML.</p>
	 *
	 * @param saldoAwalMasterAsset data tagihan vendor yang akan dicetak
	 * @return {@code Map} berisi semua parameter untuk template JasperReports
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	// Akses dilebarkan (2026-08-21) agar modul Pengadaan POS memakai pembangun
	// parameter YANG SAMA, sehingga dokumen cetaknya identik dengan versi ZKoss.
	// Isi metodenya tidak diubah dan tidak menyentuh keadaan instance.
	public static Map parameter(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("id", saldoAwalMasterAsset.getId());

		Common.insertProperty(SaldoAwalMasterAsset.class, saldoAwalMasterAsset, parameters, "data");

		if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
			Common.insertProperty(PenerimaanPengadaanMasterAsset.class,
					saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(), parameters, "terima");
		}

		Session session = HibernateUtil.currentSession();

		List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
				.createCriteria(SaldoAwalMasterAssetDetail.class)
				.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();
		List<Map> maps = new ArrayList<Map>();
		Double totalSemua = 0.0;
		Set<Long> longs = new HashSet<Long>();

		for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
			if (saldoAwalMasterAssetDetail.getMasterAsset() != null
					&& !longs.contains(saldoAwalMasterAssetDetail.getMasterAsset().getId())) {
				Map map = new HashMap();
				Common.insertProperty(SaldoAwalMasterAssetDetail.class, saldoAwalMasterAssetDetail, map, "data");
				totalSemua += saldoAwalMasterAssetDetail.getHargaTotal();
				map.put("hargatotal", saldoAwalMasterAssetDetail.getHargaTotal());
				map.put("pph", saldoAwalMasterAssetDetail.getPersenPph());
				map.put("ppn", saldoAwalMasterAssetDetail.getPersenPpn());

				map.put("hargapotongan", saldoAwalMasterAssetDetail.getHargaPotongan());
				map.put("hargabeli", saldoAwalMasterAssetDetail.getHarga());
				map.put("jumlah", saldoAwalMasterAssetDetail.getJumlah());
				map.put("nama", saldoAwalMasterAssetDetail.getMasterAsset().getNama());
				map.put("kode", saldoAwalMasterAssetDetail.getSaldoAwal().getKode());
				map.put("isbn", saldoAwalMasterAssetDetail.getMasterAsset().getKode());

				map.put("penyedia", saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia() == null ? ""
						: saldoAwalMasterAssetDetail.getSaldoAwal().getPenyedia().getNama());

				String status = "";
				if (saldoAwalMasterAssetDetail.getSaldoAwal().getDisetujuiOleh() == null) {
					status = "Belum disetujui";
				} else {
					status = "Disetujui oleh "
							+ saldoAwalMasterAssetDetail.getSaldoAwal().getDisetujuiOleh().getUserNama() + " pada "
							+ (saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPersetujuan() == null ? ""
									: Common.dateFormat51.get()
											.format(saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPersetujuan()));
				}

				map.put("status_persetujuan", status);

				map.put("perpustakaan", saldoAwalMasterAssetDetail.getSaldoAwal().getKeterangan());
				map.put("tanggal_persetujuan", saldoAwalMasterAssetDetail.getSaldoAwal().getTanggalPersetujuan());
				map.put("disetujui_oleh", saldoAwalMasterAssetDetail.getSaldoAwal().getDisetujuiOleh() == null ? ""
						: saldoAwalMasterAssetDetail.getSaldoAwal().getDisetujuiOleh().getUserNama());

				maps.add(map);
			}
		}

		if (Double.valueOf(doubleValue(saldoAwalMasterAsset.getNilai())).intValue() != totalSemua.intValue()) {
			saldoAwalMasterAsset.setNilai(totalSemua);
			Common.refreshUpdate(saldoAwalMasterAsset);
		}

		parameters.put("maps", maps);
		parameters.put("totalSemua", totalSemua);
		return parameters;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Implementasi metode {@code cetakData} dari interface {@code DataCriteria}. Menghasilkan
	 * file PDF laporan tagihan vendor untuk satu entitas {@code SaldoAwalMasterAsset}
	 * yang dipilih dari grid. Metode ini dipanggil oleh mekanisme ekspor generik
	 * {@code Common.cetakData()} ketika pengguna mengklik tombol cetak per baris.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Melakukan cast {@code GeneralValueObject} ke {@code SaldoAwalMasterAsset}, lalu
	 * memanggil {@code Report.generateFileReport()} dengan parameter dari {@code parameter()},
	 * template report {@code "asset/saldo_awal"}, tanggal pembuatan sebagai nama file,
	 * dan locale pengguna saat ini. Hasilnya adalah {@code File} PDF yang dapat diunduh.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Template laporan berada di direktori resources JasperReports dengan path
	 * {@code asset/saldo_awal.jrxml} (atau format yang dikonfigurasi). Pastikan template
	 * tersebut ada dan konsisten dengan parameter yang dihasilkan oleh {@code parameter()}.</p>
	 *
	 * @param generalValueObject entitas tagihan vendor yang akan dicetak; di-cast ke
	 *                           {@code SaldoAwalMasterAsset}
	 * @return {@code File} PDF yang berisi laporan tagihan vendor
	 * @throws Exception jika terjadi kesalahan saat membuat laporan PDF
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		SaldoAwalMasterAsset saldoAwalMasterAsset = (SaldoAwalMasterAsset) generalValueObject;
		List maps = null;
		File file = Report.generateFileReport(Report.PDF, parameter(saldoAwalMasterAsset), "asset/saldo_awal",
				saldoAwalMasterAsset.getTanggalPembuatan(), maps, Common.locale);
		return file;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode statis untuk menghasilkan dan menampilkan laporan PDF tagihan vendor secara
	 * langsung kepada pengguna sebagai respons download browser. Dipanggil setelah
	 * penyimpanan tagihan berhasil (dari {@code onSave()} melalui timer) dan dari
	 * tombol Cetak di setiap baris grid {@code SaldoAwalMasterAssetRenderer}.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Memanggil {@code Report.generatePDFReport()} yang membuat file PDF dari template
	 * JasperReports {@code "asset/saldo_awal"} menggunakan parameter dari {@code parameter()},
	 * lalu mengirimkan hasilnya langsung ke browser pengguna sebagai download. Metode ini
	 * bersifat statis sehingga dapat dipanggil tanpa instance controller.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Metode ini dipanggil via timer (delay 3 detik) setelah simpan agar proses PDF
	 * tidak memblokir respons simpan. Jika template laporan berubah, perubahan otomatis
	 * tercermin karena parameter diambil dari {@code parameter()} yang sama.</p>
	 *
	 * @param saldoAwalMasterAsset entitas tagihan vendor yang akan dicetak sebagai PDF
	 * @throws Exception jika terjadi kesalahan saat membuat atau mengirim laporan PDF
	 */
	@SuppressWarnings({})
	public static void cetak(SaldoAwalMasterAsset saldoAwalMasterAsset) throws Exception {

		Report.generatePDFReport(Report.PDF, parameter(saldoAwalMasterAsset), "asset/saldo_awal",
				saldoAwalMasterAsset.getTanggalPembuatan());
	}


	/**
	 * <b>Tujuan</b><br>
	 * Metode utilitas statis untuk memeriksa apakah sebuah string kosong, {@code null},
	 * atau hanya berisi spasi. Digunakan secara luas di seluruh kelas ini untuk validasi
	 * input form sebelum penyimpanan dan pemfilteran nilai string.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengembalikan {@code true} jika nilai adalah {@code null} atau setelah di-trim
	 * panjangnya nol karakter. Aman dipanggil dengan argumen {@code null}.</p>
	 *
	 * @param value string yang akan diperiksa
	 * @return {@code true} jika string kosong atau {@code null}, {@code false} jika ada isi
	 */
	private static boolean isEmpty(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode utilitas statis untuk mengkonversi {@code Double} nullable menjadi nilai
	 * primitif {@code double} yang aman. Menghindari {@code NullPointerException} saat
	 * melakukan operasi aritmatika pada nilai harga/nilai aset yang bisa {@code null}.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengembalikan {@code 0.0} jika argumen {@code null}, atau memanggil {@code doubleValue()}
	 * pada objek {@code Double} jika tidak {@code null}.</p>
	 *
	 * @param value nilai {@code Double} yang mungkin {@code null}
	 * @return nilai primitif double, atau {@code 0.0} jika input {@code null}
	 */
	private static double doubleValue(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode utilitas statis untuk mengkonversi string yang mungkin {@code null} menjadi
	 * string yang sudah di-trim dan tidak pernah {@code null}. Digunakan untuk membersihkan
	 * input dari komponen ZK sebelum dimasukkan ke kriteria Hibernate.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengembalikan string kosong {@code ""} jika argumen {@code null}, atau memanggil
	 * {@code trim()} pada string jika tidak {@code null}. Hasil selalu berupa string
	 * yang tidak {@code null} dan bebas spasi terdepan/belakang.</p>
	 *
	 * @param value string input yang mungkin {@code null}
	 * @return string yang sudah di-trim, atau string kosong jika input {@code null}
	 */
	private static String safeText(String value) {
		return value == null ? "" : value.trim();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode utilitas statis untuk memformat nilai numerik {@code Double} menjadi string
	 * representasi angka yang sesuai dengan format lokal Indonesia (misalnya pemisah
	 * ribuan dengan titik). Digunakan untuk menampilkan nilai harga dan total di grid.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Menggunakan {@code Common.numberFormat.get()} yang merupakan {@code ThreadLocal<NumberFormat>}
	 * untuk keamanan thread. Nilai {@code null} dikonversi ke {@code 0.0} terlebih dahulu
	 * melalui {@code doubleValue()}.</p>
	 *
	 * @param value nilai {@code Double} yang akan diformat
	 * @return string representasi angka dalam format lokal, misalnya {@code "1.250.000"}
	 */
	private static String formatNumber(Double value) {
		return Common.numberFormat.get().format(doubleValue(value));
	}

	/**
	 * <b>Tujuan</b><br>
	 * Metode utilitas statis yang memeriksa status centang dari komponen {@code Checkbox}
	 * ZKoss secara null-safe. Digunakan untuk membaca nilai filter checkbox di halaman
	 * tanpa risiko {@code NullPointerException}.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengembalikan {@code false} jika checkbox {@code null} (belum di-wire), atau
	 * memanggil {@code isChecked()} pada komponen jika tidak {@code null}.</p>
	 *
	 * @param checkbox komponen {@code Checkbox} ZKoss yang mungkin {@code null}
	 * @return {@code true} jika checkbox tidak {@code null} dan dalam keadaan tercentang
	 */
	private static boolean isChecked(Checkbox checkbox) {
		return checkbox != null && checkbox.isChecked();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Overload dari {@code isChecked(Checkbox)} untuk komponen {@code MyCheckboxConfig},
	 * wrapper kustom checkbox yang digunakan di seluruh codebase AIS. Memeriksa status
	 * centang secara null-safe untuk filter Lunas, Belum Lunas, Belum Disetujui, dll.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Identik dengan {@code isChecked(Checkbox)}: mengembalikan {@code false} jika
	 * komponen {@code null}, atau hasil {@code isChecked()} jika tidak {@code null}.</p>
	 *
	 * @param checkbox komponen {@code MyCheckboxConfig} yang mungkin {@code null}
	 * @return {@code true} jika checkbox tidak {@code null} dan dalam keadaan tercentang
	 */
	private static boolean isChecked(MyCheckboxConfig checkbox) {
		return checkbox != null && checkbox.isChecked();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Memeriksa apakah Pemesanan Pengadaan dikonfigurasi dengan sistem pembayaran bertahap
	 * (by termin/cicilan). Digunakan untuk memutuskan apakah UI perlu menampilkan
	 * baris pemilihan termin dan dokumen termin terkait.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengembalikan {@code true} hanya jika referensi data tidak {@code null} dan
	 * field {@code byTermin} bernilai {@code Boolean.TRUE}. Penggunaan
	 * {@code Boolean.TRUE.equals()} aman terhadap nilai {@code null} dari field boolean
	 * yang di-wrap dalam {@code Boolean}.</p>
	 *
	 * @param data entitas {@code PemesananPengadaanMasterAsset} yang mungkin {@code null}
	 * @return {@code true} jika pemesanan menggunakan sistem termin, {@code false} jika tidak
	 */
	private static boolean isByTermin(PemesananPengadaanMasterAsset data) {
		return data != null && Boolean.TRUE.equals(data.getByTermin());
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menghasilkan objek {@code Date} yang merupakan hari berikutnya dari tanggal yang
	 * diberikan. Digunakan dalam {@code initCriteria()} untuk membuat filter tanggal
	 * eksklusif pada batas atas (kurang dari hari berikutnya = sampai dengan hari ini).
	 *
	 * <p><b>Cara kerja</b><br>
	 * Jika input {@code null}, mengembalikan {@code null}. Jika tidak, membuat {@code Calendar}
	 * baru, menyetel ke tanggal input, menambah 1 hari, lalu mengembalikan hasilnya sebagai
	 * {@code Date}. Menggunakan {@code WaktuUtil.getCalendar()} untuk konsistensi timezone.</p>
	 *
	 * @param date tanggal awal yang akan ditambah 1 hari; boleh {@code null}
	 * @return tanggal keesokan harinya, atau {@code null} jika input {@code null}
	 */
	private static Date nextDay(Date date) {
		if (date == null) {
			return null;
		}
		Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
		cal.setTime(date);
		cal.add(Calendar.DATE, 1);
		return cal.getTime();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menerapkan style tampilan standar pada grid data utama halaman. Memastikan grid
	 * memiliki lebar penuh, border bersih, latar putih, dan tinggi minimum yang cukup
	 * agar tidak kolaps saat data kosong.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Jika parameter {@code grid} adalah {@code null} (belum di-wire), metode langsung
	 * kembali tanpa melakukan apa-apa. Jika tidak {@code null}, menyetel lebar ke
	 * {@code "100%"}, style CSS dengan border nol, latar putih, dan min-height 360px.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Untuk mengubah tampilan grid, ubah nilai style di sini. Style ini bersifat inline
	 * dan akan menimpa CSS eksternal. Gunakan class CSS jika perlu fleksibilitas lebih.</p>
	 *
	 * @param grid komponen {@code MyGrid} yang akan diberi style; boleh {@code null}
	 */
	private static void styleDataGrid(MyGrid grid) {
		if (grid == null) {
			return;
		}
		grid.setWidth("100%");
		grid.setStyle("border:0; background:#ffffff; min-height:360px;");
	}

	/**
	 * <h3>SaldoAwalMasterAssetRenderer — Renderer Baris Grid Tagihan Vendor</h3>
	 *
	 * <p><b>Untuk apa</b><br>
	 * Inner class renderer ZKoss yang bertanggung jawab membangun satu baris ({@code Row})
	 * dalam grid daftar tagihan vendor. Setiap instance dipanggil oleh ZKoss untuk setiap
	 * elemen {@code SaldoAwalMasterAsset} dalam model data grid. Renderer ini menghasilkan
	 * tampilan lengkap baris yang mencakup: detail item aset, informasi revisi, lampiran
	 * dokumen, penyedia, nilai tagihan, status lunas, data pembuat, status persetujuan,
	 * keterangan SOP, status DaftarPengajuanTransfer, checkbox aktif, dan toolbar aksi
	 * (Cetak, Setujui, Batalkan, Ubah, Hapus).</p>
	 *
	 * <p><b>Cara kerja</b><br>
	 * Metode {@code render(Row, Object)} dipanggil satu kali per baris data. Setiap sel
	 * dibangun dengan menambahkan komponen ZKoss (Label, Vbox, Hbox, Checkbox, Toolbarbutton,
	 * dll.) sebagai anak dari {@code Row}. Urutan penambahan komponen harus sesuai dengan
	 * definisi kolom ({@code <columns>}) di file ZUL. Listener event (onClick untuk tombol
	 * Setujui/Batalkan/Ubah/Hapus) ditautkan secara anonymous inner class, menangkap
	 * referensi {@code saldoAwalMasterAsset} lewat closure.</p>
	 *
	 * <p><b>Threading</b><br>
	 * Dieksekusi pada ZKoss event-thread saat model data diperbarui. Operasi database
	 * di dalam listener onClick menggunakan {@code HibernateUtil.currentSession()}
	 * yang terikat pada thread tersebut. Tindakan berat (auto-simpan DaftarPengajuanTransfer)
	 * dilakukan melalui {@code Common.createDefaultTimer()} agar tidak memblokir render.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Urutan sel harus sinkron dengan urutan {@code <column>} di file ZUL. Jika kolom
	 * ditambah/dihapus di ZUL, refleksikan perubahan yang sama di metode {@code render()}.
	 * Lampiran dokumen dibaca langsung dari database per baris — pertimbangkan batch
	 * loading jika performa menjadi masalah pada dataset besar.</p>
	 */
	class SaldoAwalMasterAssetRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * <b>Tujuan</b><br>
		 * Membangun satu baris ({@code Row}) dalam grid daftar tagihan vendor dengan
		 * semua sel data dan tombol aksi yang diperlukan. Metode ini adalah inti dari
		 * proses render dan mengkonversi satu objek {@code SaldoAwalMasterAsset} menjadi
		 * representasi visual lengkap dalam grid.
		 *
		 * <p><b>Cara kerja</b><br>
		 * Secara berurutan membangun sel-sel berikut:</p>
		 * <ol>
		 *   <li>Sel detail item: {@code SaldoAwalMasterAssetDetailAction} (jika bukan termin)
		 *       atau Label kosong (jika termin).</li>
		 *   <li>Sel revisi: panel revisi dari {@code RevisiHelper} + keterangan termin
		 *       + link Penerimaan Pengadaan.</li>
		 *   <li>Sel lampiran: kode tagihan, tanggal tagihan, dan daftar link lampiran
		 *       (faktur pajak, kwitansi, dokumen lain I/II/III) dari Penerimaan Pengadaan
		 *       atau dari saldo sendiri.</li>
		 *   <li>Sel penyedia: nama penyedia dan link ke pembayaran terkait.</li>
		 *   <li>Sel pemilik aset.</li>
		 *   <li>Sel lokasi.</li>
		 *   <li>Sel ruang.</li>
		 *   <li>Sel nilai total (dihitung ulang jika 0).</li>
		 *   <li>Sel status lunas (Ya/Tidak).</li>
		 *   <li>Sel pembuat + tanggal pembuatan.</li>
		 *   <li>Sel persetujuan: nama + tanggal penyetuju (update real-time saat disetujui).</li>
		 *   <li>Sel keterangan + SOP link + status DaftarPengajuanTransfer.</li>
		 *   <li>Sel aktif: checkbox (jika bisa diedit) atau label Ya/Tidak.</li>
		 *   <li>Sel toolbar aksi: Cetak, Setujui, Batalkan, Ubah, Hapus.</li>
		 * </ol>
		 *
		 * <p><b>Pemeliharaan</b><br>
		 * Jika pengguna melaporkan kolom data bergeser, periksa apakah urutan
		 * {@code appendChild()} di sini masih sinkron dengan definisi {@code <column>}
		 * di file ZUL halaman ini.</p>
		 *
		 * @param arg0 baris ZKoss yang akan diisi dengan komponen sel
		 * @param arg1 objek data dari model, di-cast ke {@code SaldoAwalMasterAsset}
		 * @throws Exception jika terjadi kesalahan saat mengambil data terkait dari database
		 *                   atau saat membangun komponen UI
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
						final SaldoAwalMasterAsset saldoAwalMasterAsset = (SaldoAwalMasterAsset) arg1;

			if (saldoAwalMasterAsset.getDaftarPengajuanTransfer() == null
					&& saldoAwalMasterAsset.getDisetujuiOleh() != null) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(saldoAwalMasterAsset);
					}
				});

			}

			// Mode BREAKDOWN: pastikan baris PPh "Bukti Potong" ikut TERBENTUK di Daftar Transfer
			// (DPC) saat tagihan sudah disetujui. Idempoten (sinkron memeriksa baris yang ada) ->
			// memperbaiki kasus PPh breakdown yang belum tampil, termasuk tagihan lama (vendor DPT
			// sudah ada sehingga blok auto-save di atas dilewati).
			if (Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif())
					&& saldoAwalMasterAsset.getDisetujuiOleh() != null
					&& saldoAwalMasterAsset.getBreakdownBuktiPotong() != null
					&& saldoAwalMasterAsset.getBreakdownBuktiPotong() > 0) {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ais.action.master.asset.helper.BreakdownTagihanVendorHelper
								.sinkronPajakBreakdown(saldoAwalMasterAsset);
					}
				});
			}
			final SaldoAwalMasterAssetDetailAction detail;
			if (isEmpty(saldoAwalMasterAsset.getKodeTermin())) {
				(detail = new SaldoAwalMasterAssetDetailAction(saldoAwalMasterAsset, persetujuan)).setParent(arg0);
				// Pada langkah disposisi SOP ("Pembayaran Tagihan Vendor"), tampilan tabel mengikuti
				// mode Breakdown (permintaan perbaikan tampilan):
				//  - Breakdown = YA   → ringkasan breakdown (panel "Daftar Barang/Jasa Tagihan Vendor")
				//    sudah menampilkan rincian; grid detail pajak (TABEL PO) ini duplikasi → DISEMBUNYIKAN.
				//  - Breakdown = TIDAK → tidak ada ringkasan breakdown, maka grid detail pajak
				//    (TABEL PO) inilah yang DITAMPILKAN.
				// Di daftar utama (non-disposisi, disposisiSop == null) tetap ditampilkan seperti semula.
				if (disposisiSop != null && Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif())) {
					detail.setVisible(false);
				}
			} else {
				detail = null;
				new Label().setParent(arg0);
			}
			Vbox aa;
			(aa = RevisiHelper.createNewRevisi(SaldoAwalMasterAsset.class, saldoAwalMasterAsset,
					saldoAwalMasterAsset.getKode(), "font-size:10px;")).setParent(arg0);

			if (saldoAwalMasterAsset.getKeteranganTermin() != null
					&& !saldoAwalMasterAsset.getKeteranganTermin().trim().isEmpty()) {
				new MyLabelKecil(saldoAwalMasterAsset.getKeteranganTermin()).setParent(aa);
			}

			if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
				RevisiHelper
						.createNewRevisi(PenerimaanPengadaanMasterAsset.class,
								saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(),
								saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getKode(), "font-size:10px;")
						.setParent(aa);
			}

			Vbox toolbar = new Vbox();
			toolbar.setParent(arg0);

			new MyLabelKecil(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null
					? saldoAwalMasterAsset.getKodeTagihan()
					: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getKodeTagihan()).setParent(toolbar);

			new MyLabelKecil(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null
					|| saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getTanggalTagihan() == null
							? (saldoAwalMasterAsset.getTanggalTagihan() == null ? ""
									: Common.dateFormat4.get().format(saldoAwalMasterAsset.getTanggalTagihan()))
							: Common.dateFormat4.get().format(
									saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getTanggalTagihan()))
					.setParent(toolbar);

			if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
				PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset = saldoAwalMasterAsset
						.getPenerimaanPengadaanMasterAsset();
				LampiranLain lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName());
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Faktur_Pajak");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Kode_Pajak");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Kwitansi");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_I");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_II");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(penerimaanPengadaanMasterAsset.getId(),
						PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_III");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}
			} else {

				LampiranLain lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName());
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Faktur_Pajak");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Kode_Pajak");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Kwitansi");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_I");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_II");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

				lampiranLain = LampiranLain.ambil(saldoAwalMasterAsset.getIdTemp(),
						"Saldo_" + PenerimaanPengadaanMasterAsset.class.getName() + "_Dokumen_Lain_III");
				if (lampiranLain != null) {
					A a = new A(lampiranLain.getNama());
					a.setAttribute("lampiranLain", lampiranLain);
					a.setStyle("font-size:8px");
					toolbar.appendChild(a);

					a.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							LampiranLain lampiranLain = (LampiranLain) arg0.getTarget().getAttribute("lampiranLain");
							Common.display(lampiranLain);
						}
					});
				}

			}

			toolbar = new Vbox();
			toolbar.setParent(arg0);

			new MyLabelKecil(
					saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama())
					.setParent(toolbar);

			List<PembayaranPengadaanMasterAssetDetail> pembayaranPengadaanMasterAssetDetails = HibernateUtil
					.currentSession().createCriteria(PembayaranPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("saldoAwalMasterAsset", saldoAwalMasterAsset)).list();
			for (final PembayaranPengadaanMasterAssetDetail pembayaranPengadaanMasterAssetDetail : pembayaranPengadaanMasterAssetDetails) {
				A a = new A(pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset().getKode());

				a.setStyle("font-size:8px");
				toolbar.appendChild(a);

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset()
								.getDisposisiSop() != null) {
							TampilanAlurSopAction.prosess(pembayaranPengadaanMasterAssetDetail
									.getPembayaranPengadaanMasterAsset().getDisposisiSop().getId(), null, null, true,
									arg0.getTarget());
						} else {
							PembayaranPengadaanMasterAssetAction
									.cetak(pembayaranPengadaanMasterAssetDetail.getPembayaranPengadaanMasterAsset());
						}
					}
				});

			}
			pembayaranPengadaanMasterAssetDetails.clear();
			pembayaranPengadaanMasterAssetDetails = null;

			new MyLabelKecil(saldoAwalMasterAsset.getPemilikAsset() == null ? ""
					: saldoAwalMasterAsset.getPemilikAsset().getNama()).setParent(arg0);

			new MyLabelKecil(saldoAwalMasterAsset.getLokasi() == null ? ""
					: (saldoAwalMasterAsset.getLokasi().getNama() + " (" + saldoAwalMasterAsset.getLokasi().getAlamat()
							+ ")"))
					.setParent(arg0);

			new MyLabelKecil(saldoAwalMasterAsset.getRuang() == null ? "" : saldoAwalMasterAsset.getRuang().getNama())
					.setParent(arg0);

			if (Double.valueOf(doubleValue(saldoAwalMasterAsset.getNilai())).intValue() == 0) {
				Number s = (Number) HibernateUtil.currentSession().createCriteria(SaldoAwalMasterAssetDetail.class)
						.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset))
						.setProjection(Projections.sum("hargaTotal")).uniqueResult();
				if (s != null && s.doubleValue() > 0.1) {
					saldoAwalMasterAsset.setNilai(s.doubleValue());
					Common.refreshUpdate(saldoAwalMasterAsset);
				}
			}

			new MyLabelAgakKecil(formatNumber(saldoAwalMasterAsset.getNilai())).setParent(arg0);

			new MyLabelAgakKecil(saldoAwalMasterAsset.getLunas() ? "Ya" : "Tidak").setParent(arg0);

			Vbox a = new Vbox();
			a.setParent(arg0);
			new MyLabelAgakKecil(saldoAwalMasterAsset.getDibuatOleh() == null ? ""
					: saldoAwalMasterAsset.getDibuatOleh().getUserNama()).setParent(a);
			new MyLabelAgakKecil(saldoAwalMasterAsset.getTanggalPembuatan() == null ? ""
					: Common.dateFormat3.get().format(saldoAwalMasterAsset.getTanggalPembuatan())).setParent(a);

			a = new Vbox();
			a.setParent(arg0);
			final MyLabelAgakKecil disetujuiOleh;
			(disetujuiOleh = new MyLabelAgakKecil(saldoAwalMasterAsset.getDisetujuiOleh() == null ? ""
					: saldoAwalMasterAsset.getDisetujuiOleh().getUserNama())).setParent(a);
			final MyLabelAgakKecil disetujuiTanggal;
			(disetujuiTanggal = new MyLabelAgakKecil(saldoAwalMasterAsset.getTanggalPersetujuan() == null ? ""
					: Common.dateFormat3.get().format(saldoAwalMasterAsset.getTanggalPersetujuan()))).setParent(a);

			Vbox vbox1 = new Vbox();
			vbox1.setParent(arg0);
			new MyLabelKecil(saldoAwalMasterAsset.getKeterangan()).setParent(vbox1);

			if (saldoAwalMasterAsset.getDisposisiSop() != null) {
				A aaa;
				(aaa = new A()).setParent(vbox1);
				aaa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aaa, "SOP " + saldoAwalMasterAsset.getDisposisiSop().getKeterangan() + " ("
						+ saldoAwalMasterAsset.getDisposisiSop().getSop().getNama() + ")");
				aaa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(saldoAwalMasterAsset.getDisposisiSop().getId(), null, null, true,
								arg0.getTarget());
					}
				});
			}

			DaftarPengajuanTransfer.tampilStatus(saldoAwalMasterAsset.getDaftarPengajuanTransfer(), vbox1);

			if (saldoAwalMasterAsset.getDisposisiSop() != null && !saldoAwalMasterAsset.getDisposisiSop().getAktif()) {
				new Label(ais.common.Common.getBahasaConfig("Tidak aktif")).setParent(arg0);
			} else if (saldoAwalMasterAsset.getDisetujuiOleh() == null && edit) {
				final MyCheckboxConfig aktif = new MyCheckboxConfig("Aktif");
				aktif.setChecked(saldoAwalMasterAsset.getAktif());
				aktif.setParent(arg0);
				aktif.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						saldoAwalMasterAsset.setAktif(aktif.isChecked());
						Common.refreshSaveOrUpdate(saldoAwalMasterAsset);
					}
				});
			} else {
				new Label(saldoAwalMasterAsset.getAktif() ? "Ya" : "Tidak").setParent(arg0);
			}

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({})
				@Override
				public void onEvent(Event event) throws Exception {
					cetak(saldoAwalMasterAsset);
				}
			});
			aksiButtons.add(button);

			MyToolbarbuttonConfig btnBreakdown = new MyToolbarbuttonConfig("", "/img/svg/list-box-line.svg");
			btnBreakdown.setTooltiptext("Breakdown Detail Item");
			btnBreakdown.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BreakdownTagihanVendorHelper.tampilkanPopup(saldoAwalMasterAsset, event.getTarget());
				}
			});
			aksiButtons.add(btnBreakdown);

			final MyToolbarbuttonConfig disetujui = new MyToolbarbuttonConfig("", "/img/svg/check2.svg");
			disetujui.setSclass("ais-row-btn-success");

			final MyToolbarbuttonConfig dibatalkan = new MyToolbarbuttonConfig("", "/img/svg/warning-outline.svg");
			dibatalkan.setSclass("ais-row-btn-warning");
			final MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			hapus.setSclass("ais-row-btn-danger");
			final MyToolbarbuttonConfig rubah = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");

			disetujui.setVisible(approve && saldoAwalMasterAsset.getDisetujuiOleh() == null);
			dibatalkan.setVisible(reject && saldoAwalMasterAsset.getDisetujuiOleh() != null);

			disetujui.setTooltiptext("Persetujuan");

			disetujui.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin mensetujui Saldo Awal ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@SuppressWarnings({})
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										saldoAwalMasterAsset.setDisetujuiOleh(Common.getCurrentUser());
										saldoAwalMasterAsset.setTanggalPersetujuan(ais.ui.util.WaktuUtil.getDate());
										Common.refreshUpdate(session, (saldoAwalMasterAsset));

										// SINKRON LANGSUNG ke DPC saat disetujui — tanpa harus buka ulang menu.
										// Buat DPT vendor + baris PPh breakdown (bila breakdown aktif), SAMA
										// seperti yang dilakukan renderer saat data dibuka. Idempoten
										// (simpanSaldoAwalMasterAsset skip bila DPT sudah ada / belum disetujui).
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(saldoAwalMasterAsset);
												if (Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif())
														&& saldoAwalMasterAsset.getDisetujuiOleh() != null
														&& saldoAwalMasterAsset.getBreakdownBuktiPotong() != null
														&& saldoAwalMasterAsset.getBreakdownBuktiPotong() > 0) {
													ais.action.master.asset.helper.BreakdownTagihanVendorHelper
															.sinkronPajakBreakdown(saldoAwalMasterAsset);
												}
											}
										});

										disetujuiTanggal
												.setValue(saldoAwalMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(saldoAwalMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(saldoAwalMasterAsset.getDisetujuiOleh() == null ? ""
												: saldoAwalMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui
												.setVisible(approve && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && saldoAwalMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}

									}
								}
							});
				}

			});
			aksiButtons.add(disetujui);

			dibatalkan.setTooltiptext("Dibatalkan");
			dibatalkan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah yakin ingin membatalkan Saldo Awal ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Session session = HibernateUtil.currentSession();

										saldoAwalMasterAsset.setDisetujuiOleh(null);
										saldoAwalMasterAsset.setTanggalPersetujuan(null);
										Common.refreshUpdate(session, (saldoAwalMasterAsset));


										disetujuiTanggal
												.setValue(saldoAwalMasterAsset.getTanggalPersetujuan() == null ? ""
														: Common.dateFormat3.get()
																.format(saldoAwalMasterAsset.getTanggalPersetujuan()));
										disetujuiOleh.setValue(saldoAwalMasterAsset.getDisetujuiOleh() == null ? ""
												: saldoAwalMasterAsset.getDisetujuiOleh().getUserNama());
										disetujui
												.setVisible(approve && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										dibatalkan
												.setVisible(reject && saldoAwalMasterAsset.getDisetujuiOleh() != null);
										rubah.setVisible(edit && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										hapus.setVisible(delete && saldoAwalMasterAsset.getDisetujuiOleh() == null);
										if (detail != null) {
											Common.clear(detail);
											detail.display();
										}
									}
								}
							});
				}

			});
			aksiButtons.add(dibatalkan);

			rubah.setTooltiptext("Ubah Data");
			rubah.setVisible(edit && saldoAwalMasterAsset.getDisetujuiOleh() == null);
			rubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(saldoAwalMasterAsset);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(rubah);

			hapus.setTooltiptext("Hapus Data");
			hapus.setVisible(delete && saldoAwalMasterAsset.getDisetujuiOleh() == null);
			hapus.addEventListener("onClick", new EventListener() {
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

											Session session = HibernateUtil.currentSession();
											List<SaldoAwalMasterAssetDetail> saldoAwalMasterAssetDetails = session
													.createCriteria(SaldoAwalMasterAssetDetail.class)
													.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset)).list();
											for (SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail : saldoAwalMasterAssetDetails) {
												session.delete(saldoAwalMasterAssetDetail);
											}

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													Session session = HibernateUtil.currentSession();
													Common.refreshDelete(session, saldoAwalMasterAsset);

													onSearchDefault(arg0);
												}
											});
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException(
												"penghapusan data ini",
												"Data yang Bapak/Ibu coba hapus kemungkinan besar masih digunakan/direferensikan oleh data transaksi Asset lain di sistem (mis. dokumen pengadaan, penerimaan, pembayaran, peminjaman, atau riwayat terkait), sehingga database menolak penghapusan demi menjaga integritas data.",
												e,
												new String[] {
														"Periksa apakah data ini masih digunakan/dirujuk oleh transaksi atau data lain yang berelasi.",
														"Hapus atau ubah terlebih dahulu data yang masih berelasi tersebut, baru ulangi penghapusan data ini.",
														"Nonaktifkan saja data ini (bukan menghapus) apabila data ini memang masih perlu dirujuk oleh data lain." });

										}

									}

								}
							});

				}
			});
			aksiButtons.add(hapus);

			// Susun semua tombol: max 3 per baris, rata tengah
			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menangani klik tombol "Tambah" di toolbar utama halaman. Membuka form tambah
	 * tagihan vendor baru dalam popup modal dengan entitas {@code SaldoAwalMasterAsset}
	 * kosong yang siap diisi oleh pengguna.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Memanggil {@code init(new SaldoAwalMasterAsset())} untuk membangun form dengan
	 * data kosong, kemudian menampilkan {@code addWindow} sebagai dialog modal.
	 * Tombol Simpan di form akan memanggil {@code onSave()} dan tombol Batal akan
	 * menyembunyikan window.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Tombol ini hanya terlihat jika pengguna memiliki hak CREATE (diset di
	 * {@code doAfterCompose()}). Untuk menyetel nilai default pada form baru,
	 * ubah logika di {@code reloadData()} bukan di sini.</p>
	 *
	 * @param event event klik dari ZKoss (tidak digunakan secara langsung)
	 * @throws Exception jika terjadi kesalahan saat membangun form
	 */
	public void onAdd(Event event) throws Exception {
		init(new SaldoAwalMasterAsset());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <b>Tujuan</b><br>
	 * Memproses pemilihan Penerimaan Pengadaan (BAST) oleh pengguna di form tagihan.
	 * Jika tagihan untuk Penerimaan Pengadaan ini sudah ada di database, data tersebut
	 * dimuat dan ditampilkan. Jika belum ada, tagihan baru dibuat otomatis dan disimpan.
	 * Setelah proses selesai, {@code eventListener} dipanggil dengan data tagihan sebagai
	 * payload untuk melanjutkan (biasanya reload form).
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah:</p>
	 * <ol>
	 *   <li>Jika {@code data} {@code null}, langsung keluar tanpa melakukan apa-apa.</li>
	 *   <li>Query database untuk mencari {@code SaldoAwalMasterAsset} yang sudah terkait
	 *       dengan {@code PenerimaanPengadaanMasterAsset} ini.</li>
	 *   <li>Jika sudah ada: jika pemesanan bukan by-termin, muat ulang detail aset via
	 *       {@code saldoAwalPunyaMasterAssetHelper.loadDataDetail()}. Kemudian panggil
	 *       {@code eventListener} dengan aset yang ditemukan.</li>
	 *   <li>Jika belum ada: buat {@code SaldoAwalMasterAsset} baru, salin data dari
	 *       penerimaan (satkerja, penyedia, ruang, pemilik aset), set kode termin jika ada,
	 *       hasilkan kode unik, simpan ke database, muat detail aset, lalu panggil listener.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error</b><br>
	 * Jika penyimpanan atau flush gagal, exception akan merambat ke pemanggil. Tidak ada
	 * penanganan error eksplisit di metode ini — penanganan dilakukan oleh pemanggil.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Metode ini dipanggil dari dua tempat: listener pemilihan Penerimaan Pengadaan di
	 * form, dan listener pemilihan Pemesanan Pengadaan. Pastikan perubahan logika di
	 * sini konsisten untuk kedua use case tersebut.</p>
	 *
	 * @param data          entitas Penerimaan Pengadaan yang dipilih pengguna; boleh {@code null}
	 * @param eventListener listener yang akan dipanggil setelah proses selesai, dengan
	 *                      {@code SaldoAwalMasterAsset} (baru atau yang ditemukan) sebagai
	 *                      data event
	 * @throws Exception jika terjadi kesalahan database atau saat membangun komponen
	 */
	private void prosesTerima(PenerimaanPengadaanMasterAsset data, EventListener eventListener) throws Exception {
		if (data != null) {

			Session session = HibernateUtil.currentSession();
			SaldoAwalMasterAsset asset = (SaldoAwalMasterAsset) session.createCriteria(SaldoAwalMasterAsset.class)
					.add(Restrictions.eq("penerimaanPengadaanMasterAsset", data)).setMaxResults(1).uniqueResult();
			if (asset != null) {

				if (data.getPemesananPengadaanMasterAsset() != null
						&& !isByTermin(data.getPemesananPengadaanMasterAsset())) {
					saldoAwalPunyaMasterAssetHelper.loadDataDetail(asset, data, persetujuan);
				}
				eventListener.onEvent(new Event("", null, asset));
			} else {
				SaldoAwalMasterAsset saldoAwalMasterAsset = new SaldoAwalMasterAsset();
				if (disposisiSop != null && disposisiSop.getId() != null) {
					saldoAwalMasterAsset.setDisposisiSop(disposisiSop);
				}
				saldoAwalMasterAsset.setPenerimaanPengadaanMasterAsset(data);
				saldoAwalMasterAsset.setSatuanKerja(data.getSatuanKerja());
				saldoAwalMasterAsset.setPenyedia(data.getPenyedia());
				saldoAwalMasterAsset.setRuang(data.getRuang());
				saldoAwalMasterAsset.setPemilikAsset(data.getPemilikAsset());
				String kodeTermin = (String) (SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null
						? null
						: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getValue());
				saldoAwalMasterAsset.setKodeTermin(kodeTermin);
				saldoAwalMasterAsset.setKeteranganTermin(
						(String) (SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null ? null
								: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getLabel() + " "
										+ SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem()
												.getDescription()));

				saldoAwalMasterAsset.setJsonTermin((SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null
						|| SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getAttribute("value") == null
								? null
								: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getAttribute("value")
										+ ""));

				String noAgenda = generateCode(
						tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue(), true);
				saldoAwalMasterAsset.setKode(noAgenda);

				session.save(saldoAwalMasterAsset);
				session.flush();

				if (data.getPemesananPengadaanMasterAsset() != null
						&& !isByTermin(data.getPemesananPengadaanMasterAsset())) {
					saldoAwalPunyaMasterAssetHelper.loadDataDetail(saldoAwalMasterAsset, data, persetujuan);
				}
				eventListener.onEvent(new Event("", null, saldoAwalMasterAsset));
			}

		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menampilkan informasi detail satu termin pembayaran dalam UI form: progres pekerjaan
	 * dalam persen dan daftar dokumen pendukung termin (lampiran file atau link URL).
	 * Dipanggil saat pengguna memilih termin dari combobox atau saat form dimuat ulang
	 * dengan data tagihan by-termin.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Mengambil nilai {@code pekerjaan} (persentase) dari JSON dan menampilkannya di
	 * {@code progresTermin}. Kemudian iterasi array {@code dokumens} dalam JSON:
	 * untuk setiap dokumen, coba ambil {@code LampiranLain} dari database berdasarkan
	 * {@code id_file} atau {@code keyDok}. Jika ditemukan, tambahkan link {@code <A>}
	 * ke {@code dokumenTermin} Vbox yang saat diklik menampilkan file via
	 * {@code Common.display()}. Jika tidak ditemukan tapi ada {@code nama_file} dan
	 * {@code link}, tambahkan link yang membuka URL di popup browser via JavaScript.
	 * {@code Common.clear(dokumenTermin)} dipanggil terlebih dahulu untuk membersihkan
	 * tampilan sebelumnya.</p>
	 *
	 * <p><b>Penanganan error</b><br>
	 * Entri dokumen tanpa {@code keyDok} dilewati. Konversi {@code id_file} dan
	 * {@code keyDok} dari JSON string ke Long dilakukan tanpa try-catch — pastikan
	 * format JSON valid. Error parsing ditangani di pemanggil.</p>
	 *
	 * @param jsonObject objek JSON yang merepresentasikan satu termin dari array formula
	 *                   PemesananPengadaan; harus memiliki key {@code pekerjaan} dan
	 *                   opsional {@code dokumens}
	 * @throws Exception jika terjadi kesalahan saat mengambil lampiran dari database
	 *                   atau saat membangun komponen UI
	 */
	private void tampilDataTermin(JSONObject jsonObject) throws Exception {
		Double pekerjaan = 0.0;
		if (!jsonObject.isNull("pekerjaan")) {
			pekerjaan = jsonObject.getDouble("pekerjaan");
		}

		progresTermin.setValue(Common.numberFormat.get().format(pekerjaan) + "%");

		final JSONArray dokumens;
		if (!jsonObject.isNull("dokumens")) {
			dokumens = jsonObject.getJSONArray("dokumens");
		} else {
			dokumens = new JSONArray();
			jsonObject.put("dokumens", dokumens);
		}

		Common.clear(dokumenTermin);

		for (int i = 0; i < dokumens.length(); i++) {

			final JSONObject jsonDokumen = dokumens.getJSONObject(i);

			if (jsonDokumen.isNull("keyDok")) {
				continue;
			}

			Long keyDok = Long.parseLong(jsonDokumen.get("keyDok") + "");

			Long id_file = null;

			if (!jsonDokumen.isNull("id_file")) {
				id_file = Long.parseLong(jsonDokumen.get("id_file") + "");
			}

			String nama_file = "";

			if (!jsonDokumen.isNull("nama_file")) {
				nama_file = jsonDokumen.get("nama_file") + "";
			}

			String link = "";

			if (!jsonDokumen.isNull("link")) {
				link = jsonDokumen.get("link") + "";
			}

			final LampiranLain lampiranLain = id_file != null ? LampiranLain.ambil(true, id_file, "id")
					: LampiranLain.ambil(keyDok, "Dokumen Termin PO");

			if (lampiranLain != null) {

				A a = new A(lampiranLain.getNama());
				a.setParent(dokumenTermin);
				a.setWidth("95%");

				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.display(lampiranLain);
					}
				});

			} else if (!nama_file.isEmpty() && !link.isEmpty()) {

				A a = new A(nama_file);
				a.setParent(dokumenTermin);
				a.setWidth("95%");
				final String url = link;
				a.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Clients.evalJavaScript("popupCenter({url: '" + url + "', title: 'Data', w: 1200, h: 600});");
					}
				});

			}
		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Membangun atau membangun ulang seluruh konten form tagihan vendor dalam grid dua kolom
	 * (label di kiri, input di kanan). Dipanggil pertama kali dari {@code form()} dan
	 * dipanggil ulang setiap kali pengguna mengganti Penerimaan Pengadaan atau Pemesanan
	 * Pengadaan agar form disesuaikan dengan data terpilih.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Metode ini merakit baris-baris form secara berurutan:</p>
	 * <ol>
	 *   <li>Reset referensi {@code saldoAwalMasterAsset} dan data penerimaan.</li>
	 *   <li>Baris Pemesanan Pengadaan: Label jika read-only/terisi, atau
	 *       {@code AmbilDataPemesananPengadaanAsetBanbox} jika bisa dipilih.</li>
	 *   <li>Baris Termin (awalnya tersembunyi jika tidak by-termin): combobox termin atau
	 *       label jika sudah terpilih, dengan progres dan dokumen termin.</li>
	 *   <li>Baris Penerimaan Barang/Jasa: Label atau {@code AmbilDataPenerimaanPengadaanAsetBanbox}
	 *       dengan event listener yang memanggil {@code prosesTerima()} dan reload form.</li>
	 *   <li>Checkbox "Tanpa penerimaan": toggle visibilitas baris penerimaan/pemesanan.</li>
	 *   <li>Baris Kode Pengadaan: Label kode otomatis.</li>
	 *   <li>Baris Tanggal Pengadaan: DateBox atau Label.</li>
	 *   <li>Baris Pemilik Aset (jika dikonfigurasi): Combobox atau Label.</li>
	 *   <li>Baris Lokasi: Combobox dengan kunci lokasi dari {@code LokasiAction.kunciLokasi()}.</li>
	 *   <li>Baris Ruang (jika dikonfigurasi): {@code AmbilDataRuangBanbox} atau Label.</li>
	 *   <li>Baris Penyedia: {@code AmbilDataPenyediaAssetBanbox} atau Label.</li>
	 *   <li>Baris Tanggal Persetujuan Manual (hanya jika sudah disetujui): DateBox dengan
	 *       onChange yang langsung menyimpan perubahan.</li>
	 *   <li>Baris Keterangan: MyTextbox atau Label.</li>
	 *   <li>Baris Tagihan: menampilkan rincian tagihan via {@code PenerimaanPengadaanMasterAsset.terimaTagihan()}.</li>
	 *   <li>Baris Detail Aset: grid detail via {@code SaldoAwalPunyaMasterAssetHelper}.</li>
	 * </ol>
	 *
	 * <p><b>Threading</b><br>
	 * Dieksekusi pada ZKoss event-thread. Harus dipanggil setelah rows dibersihkan
	 * ({@code Common.clear(rows)}) untuk menghindari komponen duplikat.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Saat menambah field baru ke form, tambahkan baris {@code MyFormRow} baru di sini
	 * dan pastikan field tersebut disimpan di {@code onSave()}. Urutan baris sangat
	 * penting untuk keterbacaan form.</p>
	 *
	 * @param grid             grid form yang memuat baris-baris input
	 * @param rows             container baris form yang akan diisi komponen
	 * @param generalValueObject entitas tagihan (di-cast ke {@code SaldoAwalMasterAsset})
	 * @param disposisiSop     alur SOP yang terkait, boleh {@code null}
	 * @param save             tombol Simpan yang akan diaktifkan/dinonaktifkan
	 * @param setujui          listener untuk event persetujuan, boleh {@code null}
	 * @throws Exception jika terjadi kesalahan saat query database atau membangun komponen
	 */
	@SuppressWarnings("deprecation")
	private void reloadData(final MyGrid grid, final Rows rows, GeneralValueObject generalValueObject,
			final DisposisiSop disposisiSop, final MyToolbarbuttonConfig save, final EventListener setujui)
			throws Exception {

		saldoAwalMasterAsset = (SaldoAwalMasterAsset) generalValueObject;
		reinitDataDetail(saldoAwalMasterAsset);

		penerimaanPengadaanMasterAssetData = saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset();
		if (penerimaanPengadaanMasterAssetData == null) {
			penerimaanPengadaanMasterAssetData = new PenerimaanPengadaanMasterAsset();
		}

		final MyFormRow rowPemesanan = new MyFormRow();
		rowPemesanan.setParent(rows);
		rowPemesanan.appendChild(new ais.ui.util.MyLabelConfig("Pemesanan Pengadaan"));

		pemesananPengadaanMasterAsset = new AmbilDataPemesananPengadaanAsetBanbox(true);

		rowTermin = new MyFormRow();
		rowTermin.setVisible(!saldoAwalMasterAsset.getKodeTermin().isEmpty());
		rowTermin.setParent(rows);
		rowTermin.appendChild(new ais.ui.util.MyLabelConfig("Termin ke *"));

		kodeTermin = new Combobox();
		if (isEmpty(saldoAwalMasterAsset.getKodeTermin())) {
			rowTermin.appendChild(kodeTermin);
		} else {
			rowTermin.appendChild(new Label(saldoAwalMasterAsset.getKeteranganTermin()));
		}
		kodeTermin.setReadonly(true);
		kodeTermin.setWidth("90%");

		rowTerminProgres = new MyFormRow();
		rowTerminProgres.setVisible(!saldoAwalMasterAsset.getKodeTermin().isEmpty());
		rowTerminProgres.setParent(rows);
		rowTerminProgres.appendChild(new ais.ui.util.MyLabelConfig("Progres"));

		progresTermin = new Label();
		rowTerminProgres.appendChild(progresTermin);

		rowTerminDokumen = new MyFormRow();
		rowTerminDokumen.setVisible(!saldoAwalMasterAsset.getKodeTermin().isEmpty());
		rowTerminDokumen.setParent(rows);
		rowTerminDokumen.appendChild(new ais.ui.util.MyLabelConfig("Dokumen Termin"));

		dokumenTermin = new Vbox();
		rowTerminDokumen.appendChild(dokumenTermin);

		if (!saldoAwalMasterAsset.getKodeTermin().isEmpty()) {

			try {
				JSONObject jsonObject = new JSONObject(saldoAwalMasterAsset.getJsonTermin());
				tampilDataTermin(jsonObject);

				generateDetail(
						saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset(),
						saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(), saldoAwalMasterAsset.getKodeTermin(),
						jsonObject);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		if (persetujuan) {
			rowPemesanan.appendChild(
					new Label(penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset() == null ? ""
							: penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset().getKode()));
		} else if (penerimaanPengadaanMasterAssetData != null
				&& penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset() != null) {
			rowPemesanan.appendChild(
					new Label(penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset().getKode()));
		} else {
			rowPemesanan.appendChild(pemesananPengadaanMasterAsset);

			final EventListener eventListenerTermin = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					rowTermin.setVisible(false);
					rowTerminProgres.setVisible(false);
					rowTerminDokumen.setVisible(false);

					if (penerimaanPengadaanMasterAssetData != null
							&& penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset() != null) {
						dataPemesanan = penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset();
					}

					if (isByTermin(dataPemesanan)) {

						penyedia.setValue(dataPemesanan == null || dataPemesanan.getPenyedia() == null ? ""
								: (dataPemesanan).getPenyedia().getNama());
						Common.clear(kodeTermin);
						if (dataPemesanan != null) {
							rowTermin.setVisible(isByTermin(dataPemesanan));
							rowTerminProgres.setVisible(isByTermin(dataPemesanan));
							rowTerminDokumen.setVisible(isByTermin(dataPemesanan));

							String p = "";
							try {

								JSONArray array = new JSONArray(dataPemesanan.getFormula());
								for (int i = 0; i < array.length(); i++) {

									JSONObject jsonObject = array.getJSONObject(i);

									if (jsonObject.isNull("key")) {
										continue;
									}

									String nama = "";

									if (!jsonObject.isNull("nama")) {
										nama = jsonObject.get("nama") + "";
									}

									String nomor = "";

									if (!jsonObject.isNull("nomor")) {
										nomor = jsonObject.get("nomor") + "";
									}

									Double pekerjaan = 0.0;
									if (!jsonObject.isNull("pekerjaan")) {
										pekerjaan = jsonObject.getDouble("pekerjaan");
									}

									Double penagihan = 0.0;
									if (!jsonObject.isNull("penagihan")) {
										penagihan = jsonObject.getDouble("penagihan");
									}

									JenisPajakBarang jenisPajakBarang;
									if (!jsonObject.isNull("pajak")) {
										jenisPajakBarang = (JenisPajakBarang) ConstantValues.ambil(
												JenisPajakBarang.class.getName(),
												Long.parseLong(jsonObject.get("pajak") + ""));
									} else {
										jenisPajakBarang = null;
									}

									Comboitem comboitem = new Comboitem(nomor + " " + nama);
									comboitem.setDescription("Progress " + Common.numberFormat.get().format(pekerjaan)
											+ "% nilai " + Common.numberFormat.get().format(penagihan) + ", Pajak "
											+ (jenisPajakBarang == null ? "\"Tanpa Pajak\""
													: jenisPajakBarang.getNama()));
									comboitem.setValue(jsonObject.get("key") + "");
									comboitem.setAttribute("value", jsonObject);
									kodeTermin.appendChild(comboitem);

									if (saldoAwalMasterAsset.getKodeTermin().equals(jsonObject.get("key") + "")) {
										p = Common.numberFormat.get().format(pekerjaan) + "%";

										final JSONArray dokumens;
										if (!jsonObject.isNull("dokumens")) {
											dokumens = jsonObject.getJSONArray("dokumens");
										} else {
											dokumens = new JSONArray();
											jsonObject.put("dokumens", dokumens);
										}

										Common.clear(dokumenTermin);

										for (int ii = 0; ii < dokumens.length(); ii++) {

											final JSONObject jsonDokumen = dokumens.getJSONObject(ii);

											if (jsonDokumen.isNull("keyDok")) {
												continue;
											}

											Long keyDok = Long.parseLong(jsonDokumen.get("keyDok") + "");

											Long id_file = null;

											if (!jsonDokumen.isNull("id_file")) {
												id_file = Long.parseLong(jsonDokumen.get("id_file") + "");
											}

											String nama_file = "";

											if (!jsonDokumen.isNull("nama_file")) {
												nama_file = jsonDokumen.get("nama_file") + "";
											}

											String link = "";

											if (!jsonDokumen.isNull("link")) {
												link = jsonDokumen.get("link") + "";
											}

											final LampiranLain lampiranLain = id_file != null
													? LampiranLain.ambil(true, id_file, "id")
													: LampiranLain.ambil(keyDok, "Dokumen Termin PO");

											if (lampiranLain != null) {

												A a = new A(lampiranLain.getNama());
												a.setParent(dokumenTermin);
												a.setWidth("95%");

												a.addEventListener("onClick", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Common.display(lampiranLain);
													}
												});

											} else if (!nama_file.isEmpty() && !link.isEmpty()) {

												A a = new A(nama_file);
												a.setParent(dokumenTermin);
												a.setWidth("95%");
												final String url = link;
												a.addEventListener("onClick", new EventListener() {

													@Override
													public void onEvent(Event arg0) throws Exception {
														Clients.evalJavaScript("popupCenter({url: '" + url
																+ "', title: 'Data', w: 1200, h: 600});");
													}
												});

											}
										}
									}
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							progresTermin.setValue(p);
						}

						Common.selectComboItem(true, kodeTermin, saldoAwalMasterAsset.getKodeTermin());

						generateDetail(dataPemesanan, penerimaanPengadaanMasterAssetData,
								saldoAwalMasterAsset.getKodeTermin(), null);
					}
				}
			};

			EventListener pemesananEventListener = new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					rowTermin.setVisible(false);
					rowTerminProgres.setVisible(false);
					rowTerminDokumen.setVisible(false);
					dataPemesanan = (PemesananPengadaanMasterAsset) pemesananPengadaanMasterAsset
							.getAttribute("pemesananPengadaanMasterAsset");

					if (dataPemesanan != null) {

						String kodeTermin = (String) (SaldoAwalMasterAssetAction.this.kodeTermin
								.getSelectedItem() == null ? null
										: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getValue());
						Session session = HibernateUtil.currentSession();
						if (!isByTermin(dataPemesanan)) {
							int assetCount = ((Number) session.createCriteria(SaldoAwalMasterAsset.class)
									.add(Restrictions.isNotNull("disetujuiOleh")).setProjection(Projections.rowCount())
									.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")
									.add(Restrictions.eq("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset",
											dataPemesanan))
									.uniqueResult()).intValue();
							if (assetCount > 0) {
								MyMessageboxConfig.show("Tagihan sudah pernah dibuat", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}
						} else if (kodeTermin != null) {
							int assetCount = ((Number) session.createCriteria(SaldoAwalMasterAsset.class)
									.add(Restrictions.isNotNull("disetujuiOleh")).setProjection(Projections.rowCount())
									.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset")

									.add(kodeTermin == null || kodeTermin.trim().isEmpty()
											|| !isByTermin(dataPemesanan) ? Restrictions.sqlRestriction("true")
													: Restrictions.eq("kodeTermin", kodeTermin))

									.add(Restrictions.eq("penerimaanPengadaanMasterAsset.pemesananPengadaanMasterAsset",
											dataPemesanan))
									.uniqueResult()).intValue();
							if (assetCount > 0) {
								MyMessageboxConfig.show("Tagihan sudah pernah dibuat", "Peringatan",
										MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
								return;
							}
						}
						rowTermin.setVisible(isByTermin(dataPemesanan));
						rowTerminProgres.setVisible(isByTermin(dataPemesanan));
						rowTerminDokumen.setVisible(isByTermin(dataPemesanan));

						if (isByTermin(dataPemesanan)) {

							if (kodeTermin == null || kodeTermin.trim().isEmpty()) {
								eventListenerTermin.onEvent(arg0);
								return;
							} else {
								SaldoAwalMasterAssetAction.this.kodeTermin.setDisabled(true);
							}
						}

						PenerimaanPengadaanMasterAsset aa = (PenerimaanPengadaanMasterAsset) session
								.createCriteria(PenerimaanPengadaanMasterAsset.class)

								.add(kodeTermin == null || kodeTermin.trim().isEmpty() || !isByTermin(dataPemesanan)
										? Restrictions.sqlRestriction("true")
										: Restrictions.eq("kodeTermin", kodeTermin))

								.add(Restrictions.eq("pemesananPengadaanMasterAsset", dataPemesanan)).setMaxResults(1)
								.uniqueResult();

						if (aa == null) {

							aa = new PenerimaanPengadaanMasterAsset();
							aa.setKodeTermin(kodeTermin);
							aa.setKeteranganTermin(
									(String) (SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null
											? null
											: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getLabel()
													+ " " + SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem()
															.getDescription()));

							aa.setJsonTermin((SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null
									|| SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem()
											.getAttribute("value") == null ? null
													: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem()
															.getAttribute("value") + ""));
							aa.setPemesananPengadaanMasterAsset(dataPemesanan);

							String noAgenda = PenerimaanPengadaanMasterAssetAction.generateCode(true,
									WaktuUtil.getDate());
							aa.setKode(noAgenda);
							aa.setDibuatOleh(Common.getCurrentUser());
							aa.setTanggalPembuatan(WaktuUtil.getDate());
							session.save(aa);
							session.flush();

							JSONObject jsonObject = null;
							if (isByTermin(dataPemesanan)) {
								kodeTermin = aa.getKodeTermin();
								try {
									jsonObject = new JSONObject(aa.getJsonTermin());
								} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
							}

							if (jsonObject != null) {
								tampilDataTermin(jsonObject);
							}

							List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
									.createCriteria(PemesananPengadaanMasterAssetDetail.class)
									.add(Restrictions.eq("pemesananPengadaanMasterAsset", dataPemesanan)).list();
							for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {

								PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) session
										.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
										.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset",
												Criteria.LEFT_JOIN)

										.add(kodeTermin == null || kodeTermin.trim().isEmpty()
												|| !isByTermin(dataPemesanan)
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("penerimaanPengadaanMasterAsset.kodeTermin",
																kodeTermin))

										.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail",
												pemesananPengadaanMasterAssetDetail))
										.setMaxResults(1).uniqueResult();
								if (penerimaanPengadaanMasterAssetDetail == null) {
									penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
									penerimaanPengadaanMasterAssetDetail
											.setMasterAsset(pemesananPengadaanMasterAssetDetail.getMasterAsset());
									penerimaanPengadaanMasterAssetDetail
											.setJumlah(pemesananPengadaanMasterAssetDetail.getJumlah());
									penerimaanPengadaanMasterAssetDetail
											.setDiterima(pemesananPengadaanMasterAssetDetail.getJumlah());
									penerimaanPengadaanMasterAssetDetail
											.setKeterangan(pemesananPengadaanMasterAssetDetail.getKeterangan());

									if (jsonObject != null && !jsonObject.isNull("penagihan")) {
										try {
											Double penagihan = jsonObject.getDouble("penagihan");
											penerimaanPengadaanMasterAssetDetail.setHargaBeli(penagihan);
											penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(penagihan);
										} catch (Exception e) {
																						Common.tampilErrorJikaAdmin(e);
										}

									} else {
										penerimaanPengadaanMasterAssetDetail
												.setHargaBeli(pemesananPengadaanMasterAssetDetail.getHargaBeli());
									}

									penerimaanPengadaanMasterAssetDetail.setPemesananPengadaanMasterAssetDetail(
											pemesananPengadaanMasterAssetDetail);

									if (aa.getId() != null) {
										penerimaanPengadaanMasterAssetDetail.setPenerimaanPengadaanMasterAsset(aa);
										session.save(penerimaanPengadaanMasterAssetDetail);
										session.flush();
									}
								}
							}

						}
						if (aa != null) {
							penerimaanPengadaanMasterAssetData = aa;
						}
						penerimaanPengadaanMasterAsset.setAttribute("penerimaanPengadaanMasterAsset", aa);
						penerimaanPengadaanMasterAsset.setValue(aa == null ? "" : aa.getKode());
						penerimaanPengadaanMasterAsset.setDisabled(true);

						prosesTerima(aa, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.clear(rows);
								reloadData(grid, rows, (GeneralValueObject) arg0.getData(), disposisiSop, save,
										setujui);
							}
						});

					}

				}
			};

			kodeTermin.addEventListener("onChange", pemesananEventListener);

			pemesananPengadaanMasterAsset.setEventListener(pemesananEventListener);
		}

		pemesananPengadaanMasterAsset.setWidth("90%");
		pemesananPengadaanMasterAsset.setAttribute("pemesananPengadaanMasterAsset",
				penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset());
		pemesananPengadaanMasterAsset
				.setValue(penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset() == null ? ""
						: penerimaanPengadaanMasterAssetData.getPemesananPengadaanMasterAsset().toString());
		pemesananPengadaanMasterAsset.setReadonly(true);

		final MyFormRow rowPenerimaan = new MyFormRow();
		rowPenerimaan.setParent(rows);
		rowPenerimaan.appendChild(new ais.ui.util.MyLabelConfig("Penerimaan Barang / Jasa"));
		penerimaanPengadaanMasterAsset = null;

		if (persetujuan) {
			rowPenerimaan.appendChild(new Label(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null ? ""
					: saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getKode()));
		} else if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {
			rowPenerimaan.appendChild(new Label(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getKode()));
		} else {
			penerimaanPengadaanMasterAsset = new AmbilDataPenerimaanPengadaanAsetBanbox();
			rowPenerimaan.appendChild(penerimaanPengadaanMasterAsset);
			penerimaanPengadaanMasterAsset.setWidth("90%");

			penerimaanPengadaanMasterAsset.setEventListener(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					try {
						PenerimaanPengadaanMasterAsset data = (PenerimaanPengadaanMasterAsset) penerimaanPengadaanMasterAsset
								.getAttribute("penerimaanPengadaanMasterAsset");
						if (data != null) {
							penerimaanPengadaanMasterAssetData = data;
						}
						prosesTerima(data, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.clear(rows);
								reloadData(grid, rows, (GeneralValueObject) arg0.getData(), disposisiSop, save,
										setujui);
							}
						});
					} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

				}
			});
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		tanpaPenerimaan = new MyCheckboxConfig("Tanpa penerimaan barang / jasa");

		if (persetujuan) {
			row.appendChild(new Label("Tanpa penerimaan barang / jasa -> "
					+ (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null ? "Ya" : "Tidak")));
		} else {
			row.appendChild(tanpaPenerimaan);
		}

		if (saldoAwalMasterAsset.getId() != null)
			tanpaPenerimaan.setChecked(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() == null);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				rowPenerimaan.setVisible(!isChecked(tanpaPenerimaan));
				rowPemesanan.setVisible(!isChecked(tanpaPenerimaan));
			}
		};
		tanpaPenerimaan.addEventListener("onClick", eventListener);
		eventListener.onEvent(null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode Pengadaan"));
		tanggalPembuatan = new MyDatebox(
				saldoAwalMasterAsset.getTanggalPembuatan() == null ? ais.ui.util.WaktuUtil.getDate()
						: saldoAwalMasterAsset.getTanggalPembuatan());
		if (saldoAwalMasterAsset.getKode() == null) {
			String noAgenda = generateCode(tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue(),
					false);
			saldoAwalMasterAsset.setKode(noAgenda);
		}

		kode = new Label(saldoAwalMasterAsset.getKode());
		row.appendChild(kode);
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Pengadaan"));
		tanggalPembuatan = new MyDatebox(saldoAwalMasterAsset.getTanggalPembuatan());

		if (persetujuan) {
			row.appendChild(new Label(Common.dateFormat3.get().format(saldoAwalMasterAsset.getTanggalPembuatan())));
		} else {
			row.appendChild(tanggalPembuatan);
		}

		tanggalPembuatan.setFormat(Common.dateFormat.get().toPattern());
		tanggalPembuatan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pemilik"));

		pemilikAsset = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(saldoAwalMasterAsset.getPemilikAsset() == null ? ""
					: saldoAwalMasterAsset.getPemilikAsset().getNama()));
		} else {
			row.appendChild(pemilikAsset);
		}

		Common.insertComboDanSemua(pemilikAsset, new String[] { "nama", "id" }, "keterangan", PemilikAsset.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(pemilikAsset, saldoAwalMasterAsset.getPemilikAsset());
		pemilikAsset.setWidth("90%");
		pemilikAsset.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi"));
		lokasi = new Combobox();

		if (persetujuan) {
			row.appendChild(new Label(
					saldoAwalMasterAsset.getLokasi() == null ? "" : saldoAwalMasterAsset.getLokasi().getNama()));
		} else {
			row.appendChild(lokasi);
		}

		Common.insertComboDanSemua(lokasi, new String[] { "nama" }, "alamat", Lokasi.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(lokasi, saldoAwalMasterAsset.getLokasi());
		lokasi.setWidth("90%");
		lokasi.setReadonly(true);

		LokasiAction.kunciLokasi(lokasi);

		row = new MyFormRow();
		row.setVisible(tampilkanRuanganDamPemilikAset);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang"));
		ruang = new AmbilDataRuangBanbox();

		if (persetujuan) {
			row.appendChild(new Label(
					saldoAwalMasterAsset.getRuang() == null ? "" : saldoAwalMasterAsset.getRuang().getNama()));
		} else {
			row.appendChild(ruang);
		}

		ruang.setValue(
				saldoAwalMasterAsset.getRuang() == null ? "" : (saldoAwalMasterAsset.getRuang().getKodeRuangan()));
		ruang.setAttribute("ruang", saldoAwalMasterAsset.getRuang());
		ruang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Penyedia *"));
		penyedia = new AmbilDataPenyediaAssetBanbox();
		if (persetujuan) {
			row.appendChild(new Label(
					saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama()));
		} else {
			row.appendChild(penyedia);
		}
		penyedia.setAttribute("penyediaAsset", saldoAwalMasterAsset.getPenyedia());
		penyedia.setValue(
				saldoAwalMasterAsset.getPenyedia() == null ? "" : saldoAwalMasterAsset.getPenyedia().getNama());
		penyedia.setWidth("90%");
		penyedia.setReadonly(true);

		row = new MyFormRow();
		row.setVisible(saldoAwalMasterAsset.getDisetujuiOleh() != null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Persetujuan"));
		tanggalPersetujuanManual = new MyDatebox(saldoAwalMasterAsset.getTanggalPersetujuanManual());
		if (saldoAwalMasterAsset.getPostingHistory() == null) {
			row.appendChild(tanggalPersetujuanManual);
		} else {
			row.appendChild(new Label(Common.dateFormat1.get()
					.format(saldoAwalMasterAsset.getTanggalPersetujuanManual() == null ? WaktuUtil.getDate()
							: saldoAwalMasterAsset.getTanggalPersetujuanManual())));
		}
		tanggalPersetujuanManual.setReadonly(true);
		tanggalPersetujuanManual.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null) {
					saldoAwalMasterAsset.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());
					Common.refreshUpdate(saldoAwalMasterAsset);
				}
			}
		});

		grid.setAttribute("eventListenerSetuju", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (arg0 != null && arg0.getTarget() instanceof Checkbox) {
					Checkbox checkbox = (Checkbox) arg0.getTarget();
					Boolean selesai = (Boolean) checkbox.getAttribute("selesai");
					if (tanggalPersetujuanManual != null && tanggalPersetujuanManual.getParent() != null) {
						if (tanggalPersetujuanManual.getValue() == null) {
							tanggalPersetujuanManual.setValue(WaktuUtil.getDate());
						}
						tanggalPersetujuanManual.getParent().setVisible(selesai != null && selesai);
					}
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		keterangan = new MyTextbox(
				saldoAwalMasterAsset.getKeterangan() == null ? "" : saldoAwalMasterAsset.getKeterangan());

		if (persetujuan) {
			row.appendChild(new Label(saldoAwalMasterAsset.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}
		keterangan.setWidth("90%");
		keterangan.setRows(4);

		kodeTagihan = new Textbox(saldoAwalMasterAsset.getKodeTagihan());
		tanggalTagihan = new MyDatebox(saldoAwalMasterAsset.getTanggalTagihan());

		if (saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null) {

			MyFormRow rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowData, "2");

			PenerimaanPengadaanMasterAsset.terimaTagihan(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset(),
					rowData, saldoAwalMasterAsset.getDisetujuiOleh() == null);
		} else {
			MyFormRow rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowData, "2");

			PenerimaanPengadaanMasterAsset.terimaTagihan(saldoAwalMasterAsset, kodeTagihan, tanggalTagihan, rowData,
					saldoAwalMasterAsset.getDisetujuiOleh() == null);
		}

		// Tampilan detail "Daftar Barang / Jasa" mengikuti mode Breakdown (perbaikan tampilan
		// Pengajuan Pembayaran Vendor):
		//  - Breakdown = YA   -> yang DITAMPILKAN adalah PANEL RINGKASAN breakdown (read-only).
		//    Grid detail per-item dibangun (untuk proses simpan) tetapi tidak ditampilkan agar
		//    tidak duplikat dengan ringkasan.
		//  - Breakdown = TIDAK -> yang DITAMPILKAN adalah grid detail per-item seperti semula
		//    (dibungkus MyFormRow span 2).
		if (Boolean.TRUE.equals(saldoAwalMasterAsset.getBreakdownAktif())) {
			// Breakdown = YA: grid detail per-item duplikat dengan ringkasan, jadi TIDAK ditampilkan.
			// Helper & gridMasterAsset TETAP dibangun agar proses simpan (yang membaca gridMasterAsset)
			// tidak berubah; nilai balik initDetail (groupbox) hanya disimpan untuk cadangan.
			org.zkoss.zul.Groupbox detailGrid =
					(saldoAwalPunyaMasterAssetHelper = new SaldoAwalPunyaMasterAssetHelper(gridMasterAsset = new MyGrid()))
							.initDetail(saldoAwalMasterAsset, persetujuan, tanpaPenerimaan);

			rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowData, "2");

			// Panel untuk menampilkan RINGKASAN breakdown (read-only) ditempel di sini: daftar item +
			// Subtotal Barang/Jasa, PPN, Jumlah Total, Jenis PPh, Bukti Potong, Total Transfer.
			// Layoutnya sama dengan popup pengelolaan (tombol "Breakdown Detail Item") dan hasil cetak.
			// Pengubahan item tetap lewat tombol toolbar tersebut; panel ini murni menampilkan.
			try {
				String htmlBreakdown = BreakdownTagihanVendorHelper.buildHtmlRingkasanEmbed(saldoAwalMasterAsset);
				if (htmlBreakdown != null && htmlBreakdown.trim().length() > 0) {
					ais.ui.util.MyHtml panelBreakdown = new ais.ui.util.MyHtml();
					panelBreakdown.setContent(htmlBreakdown);
					panelBreakdown.setStyle("width:100%;display:block;");
					rowData.appendChild(panelBreakdown);
				} else {
					// Ringkasan belum bisa dibangun -> pakai detail grid helper sebagai cadangan.
					rowData.appendChild(detailGrid);
				}
			} catch (Throwable eBreakdown) {
				// Gagal membangun panel ringkasan -> jatuh ke detail grid helper agar form tetap utuh.
				rowData.appendChild(detailGrid);
			}
		} else {
			rowData = new MyFormRow();
			rowData.setValign("top");
			rowData.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowData, "2");
			rowData.appendChild(
					(saldoAwalPunyaMasterAssetHelper = new SaldoAwalPunyaMasterAssetHelper(gridMasterAsset = new MyGrid()))
							.initDetail(saldoAwalMasterAsset, persetujuan, tanpaPenerimaan));
		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Memastikan bahwa semua detail baris aset (dari Pemesanan Pengadaan) sudah terwakili
	 * sebagai {@code PenerimaanPengadaanMasterAssetDetail} dan {@code SaldoAwalMasterAssetDetail}
	 * di database, kemudian memuat ulang tampilan detail aset di form. Dipanggil setelah
	 * pengguna memilih termin atau saat form dimuat untuk tagihan by-termin yang sudah ada.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah:</p>
	 * <ol>
	 *   <li>Jika pemesanan by-termin tetapi kodeTermin kosong, langsung keluar (belum dipilih).</li>
	 *   <li>Query semua {@code PemesananPengadaanMasterAssetDetail} dari pemesanan.</li>
	 *   <li>Untuk setiap detail pemesanan, cari {@code PenerimaanPengadaanMasterAssetDetail}
	 *       yang sesuai (filter by kodeTermin jika by-termin). Jika tidak ada, buat baru
	 *       dengan data dari pemesanan, termasuk harga dari JSON termin jika tersedia.</li>
	 *   <li>Cari {@code SaldoAwalMasterAssetDetail} yang sesuai. Jika tidak ada, buat baru
	 *       dengan data aset, jumlah, harga, PPh, PPN, diskon dari detail pemesanan.</li>
	 *   <li>Setelah semua detail terpastikan ada, muat ulang tampilan via
	 *       {@code saldoAwalPunyaMasterAssetHelper.loadDataDetail()}.</li>
	 *   <li>Sembunyikan baris data jika tagihan adalah by-termin (tidak perlu grid detail terpisah).</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error</b><br>
	 * Kesalahan parsing JSON (harga termin) dan kesalahan saat memuat detail ditangkap
	 * dan ditampilkan via {@code Common.tampilErrorJikaAdmin()} tanpa menghentikan proses.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Metode ini hanya membuat entitas baru jika belum ada ({@code == null}), sehingga
	 * aman dipanggil berulang kali tanpa menyebabkan duplikasi data.</p>
	 *
	 * @param mypemesananPengadaanMasterAsset pemesanan pengadaan yang menjadi sumber detail;
	 *                                        boleh {@code null} (langsung keluar)
	 * @param penerimaanPengadaanMasterAsset  penerimaan pengadaan yang terkait dengan tagihan
	 * @param kodeTermin                      kode termin yang dipilih; {@code null} atau kosong
	 *                                        jika bukan by-termin
	 * @param jsonObject                      data JSON termin dari formula pemesanan, berisi
	 *                                        harga penagihan; boleh {@code null}
	 */
	@SuppressWarnings("unchecked")
	public void generateDetail(PemesananPengadaanMasterAsset mypemesananPengadaanMasterAsset,
			PenerimaanPengadaanMasterAsset penerimaanPengadaanMasterAsset, String kodeTermin, JSONObject jsonObject) {

		if (isByTermin(mypemesananPengadaanMasterAsset)
				&& (kodeTermin == null || kodeTermin.trim().isEmpty())) {
			return;
		}

		if (mypemesananPengadaanMasterAsset != null && mypemesananPengadaanMasterAsset.getId() != null) {
			Session session = HibernateUtil.currentSession();
			List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", mypemesananPengadaanMasterAsset)).list();

			for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {

				PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) session
						.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset",
								Criteria.LEFT_JOIN)

						.add(kodeTermin == null || kodeTermin.trim().isEmpty()
								|| !isByTermin(mypemesananPengadaanMasterAsset) ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("penerimaanPengadaanMasterAsset.kodeTermin", kodeTermin))

						.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail",
								pemesananPengadaanMasterAssetDetail))
						.setMaxResults(1).uniqueResult();
				if (penerimaanPengadaanMasterAssetDetail == null) {
					penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
					penerimaanPengadaanMasterAssetDetail
							.setMasterAsset(pemesananPengadaanMasterAssetDetail.getMasterAsset());
					penerimaanPengadaanMasterAssetDetail.setJumlah(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail.setDiterima(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail
							.setKeterangan(pemesananPengadaanMasterAssetDetail.getKeterangan());

					if (jsonObject != null && !jsonObject.isNull("penagihan")) {
						try {
							Double penagihan = jsonObject.getDouble("penagihan");
							penerimaanPengadaanMasterAssetDetail.setHargaBeli(penagihan);
							penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(penagihan);
						} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
						}

					} else {
						penerimaanPengadaanMasterAssetDetail
								.setHargaBeli(pemesananPengadaanMasterAssetDetail.getHargaBeli());
					}

					penerimaanPengadaanMasterAssetDetail
							.setPemesananPengadaanMasterAssetDetail(pemesananPengadaanMasterAssetDetail);

					if (penerimaanPengadaanMasterAsset.getId() != null) {
						penerimaanPengadaanMasterAssetDetail
								.setPenerimaanPengadaanMasterAsset(penerimaanPengadaanMasterAsset);
						session.save(penerimaanPengadaanMasterAssetDetail);
						session.flush();
					}
				}

			}

			try {
				if (saldoAwalPunyaMasterAssetHelper == null) {
					saldoAwalPunyaMasterAssetHelper = new SaldoAwalPunyaMasterAssetHelper(
							gridMasterAsset = new MyGrid());
				}
				saldoAwalPunyaMasterAssetHelper.loadDataDetail(saldoAwalMasterAsset, penerimaanPengadaanMasterAsset,
						persetujuan);

				if (rowData != null) {
					rowData.setVisible(saldoAwalMasterAsset.getJsonTermin() == null);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Implementasi metode {@code form()} dari interface {@code FormSop}. Membuat dan
	 * mengembalikan komponen {@code MyGrid} yang berisi seluruh form isian tagihan vendor,
	 * siap untuk ditempatkan dalam container (biasanya {@code Center} dari {@code Borderlayout}
	 * di dalam {@code MyWindow}).
	 *
	 * <p><b>Cara kerja</b><br>
	 * Membuat {@code MyGrid} baru dengan lebar 100%, tinggi 100%, style putih bersih
	 * dengan min-height 520px. Menambah dua kolom ({@code Columns}): kolom kiri 30% untuk
	 * label, kolom kanan untuk input. Membuat {@code Rows} container, lalu mendelegasikan
	 * pengisian baris ke {@code reloadData()}. Mengembalikan grid yang sudah terisi.</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Metode ini dipanggil dari {@code init()} saat membuka form tambah/ubah. Jika ingin
	 * mengubah jumlah kolom form, ubah di sini. Konten baris dikelola sepenuhnya oleh
	 * {@code reloadData()} — jangan duplikasi logika di sini.</p>
	 *
	 * @param generalValueObject entitas yang akan ditampilkan di form; {@code SaldoAwalMasterAsset}
	 *                           baru (kosong) untuk tambah, atau existing untuk ubah
	 * @param disposisiSop       alur SOP yang sedang aktif, boleh {@code null}
	 * @param save               tombol Simpan dari toolbar yang akan dipasang listener-nya
	 * @param setujui            listener persetujuan dari alur SOP, boleh {@code null}
	 * @return {@code MyGrid} yang berisi seluruh form isian tagihan vendor
	 * @throws Exception jika terjadi kesalahan saat membangun komponen form
	 */
	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop, MyToolbarbuttonConfig save,
			EventListener setujui) throws Exception {

		this.disposisiSop = disposisiSop;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0; background:#ffffff; min-height:520px;");

		Columns columns = new Columns();
		columns.setParent(grid);
		columns.setSizable(true);
		columns.setStyle("background:#f8fafc; border-bottom:1px solid #e5e7eb; font-weight:bold;");

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		reloadData(grid, rows, generalValueObject, disposisiSop, save, setujui);

		return grid;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menginisialisasi dan menampilkan popup modal form tagihan vendor, baik untuk menambah
	 * tagihan baru maupun mengubah tagihan yang sudah ada. Metode ini membangun keseluruhan
	 * struktur UI popup: Borderlayout dengan form di tengah dan toolbar (Simpan/Batal) di bawah.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah:</p>
	 * <ol>
	 *   <li>Simpan referensi {@code saldoAwalMasterAsset} ke field instance.</li>
	 *   <li>Jika {@code addWindow} sudah ada (re-use), setel judul, ukuran, dan bersihkan
	 *       konten lama via {@code Common.clear()}.</li>
	 *   <li>Buat {@code MyBorderlayout} baru sebagai layout utama popup.</li>
	 *   <li>Tambah {@code Center} dengan flex, isi dengan hasil {@code form()} (grid form).</li>
	 *   <li>Tambah {@code South} dengan toolbar berisi tombol Batal dan Simpan.</li>
	 *   <li>Tombol Batal: menyembunyikan window.</li>
	 *   <li>Tombol Simpan: memanggil {@code onSave()}, jika berhasil memanggil
	 *       {@code onSearchDefault()} dan menyembunyikan window.</li>
	 *   <li>Pasang Borderlayout sebagai anak dari {@code addWindow}.</li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Ukuran popup dikonfigurasi di sini: 96% x 94% untuk desktop, 100% x 100% untuk
	 * mobile. Jika perlu mengubah ukuran, ubah nilai di sini. {@code disposisiSop}
	 * di-reset ke {@code null} karena form ini digunakan tanpa alur SOP langsung
	 * (SOP ditangani melalui {@code form()} dari interface {@code FormSop}).</p>
	 *
	 * @param saldoAwalMasterAsset entitas tagihan yang akan dikelola; {@code new SaldoAwalMasterAsset()}
	 *                             untuk tambah baru, atau entitas existing untuk ubah
	 * @throws Exception jika terjadi kesalahan saat membangun form atau komponen popup
	 */
	private void init(final SaldoAwalMasterAsset saldoAwalMasterAsset) throws Exception {
		this.saldoAwalMasterAsset = saldoAwalMasterAsset;

		if (addWindow != null) {
			addWindow.setTitle("Pembayaran / Terima Tagihan Vendor");
			addWindow.setWidth(Common.isMobile() ? "100%" : "96%");
			addWindow.setHeight(Common.isMobile() ? "100%" : "94%");
			Common.clear(addWindow);
		}

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");

		disposisiSop = null;
		center.appendChild(form(saldoAwalMasterAsset, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (addWindow != null) {
					addWindow.setVisible(false);
				}
			}
		});
		cancel.setParent(toolbar);

		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);

					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {

							onSearchDefault(null);
						}
					});
					if (addWindow != null) {
						addWindow.setVisible(false);
					}
				}
			}
		});
		save.setParent(toolbar);
		if (addWindow != null) {
			borderlayout.setParent(addWindow);
		}
	}

	/**
	 * <b>Tujuan</b><br>
	 * Menyimpan data tagihan vendor baru atau memperbarui tagihan yang sudah ada ke
	 * database. Melakukan validasi input, menyimpan header tagihan, menyimpan semua
	 * baris detail aset, menghitung total nilai, dan menjalankan proses pasca-simpan
	 * (auto-simpan DaftarPengajuanTransfer dan cetak PDF via timer).
	 *
	 * <p><b>Cara kerja</b><br>
	 * Langkah-langkah secara berurutan:</p>
	 * <ol>
	 *   <li><b>Validasi</b>: Pastikan kode tagihan tidak kosong; penyedia dipilih; setiap
	 *       baris detail aset memiliki master aset yang dipilih.</li>
	 *   <li><b>Ambil entitas</b>: Jika ID sudah ada, load dari database. Jika gagal (entitas
	 *       terhapus), buat baru.</li>
	 *   <li><b>Setel field header</b>: Penerimaan Pengadaan, lokasi, pemilik aset, ruang, kode,
	 *       keterangan, tanggal, SOP, penyedia, kode termin, keterangan termin, JSON termin,
	 *       tanggal persetujuan manual, kode tagihan, tanggal tagihan.</li>
	 *   <li><b>Simpan/Update header</b>: {@code session.update()} untuk existing,
	 *       {@code session.save()} untuk baru dengan data pembuat dan kode otomatis.</li>
	 *   <li><b>Simpan detail</b>: Untuk setiap baris di {@code gridMasterAsset}, set FK
	 *       ke header lalu {@code session.saveOrUpdate()}. Akumulasi total nilai.</li>
	 *   <li><b>Update nilai header</b>: Simpan total ke header dan flush.</li>
	 *   <li><b>Reinit detail</b>: Sinkronkan detail dari Pemesanan ke Saldo via
	 *       {@code reinitDataDetail()}.</li>
	 *   <li><b>Timer pasca-simpan</b>: Delay 3 detik, auto-simpan DaftarPengajuanTransfer
	 *       dan cetak PDF.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error</b><br>
	 * Validasi menampilkan pesan via {@code MyMessageboxConfig.show()} dan mengembalikan
	 * {@code false}. Kesalahan database di blok simpan header dan detail ditangkap dan
	 * ditampilkan via {@code Common.tampilErrorJikaAdmin()} tanpa menghentikan proses
	 * (nilai masih diakumulasi dari baris yang berhasil).</p>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Saat menambah field baru ke form, pastikan field tersebut di-set ke entitas di
	 * blok "Setel field header" (langkah 3). Urutan penyimpanan header sebelum detail
	 * sangat penting karena detail membutuhkan FK ke header.</p>
	 *
	 * @param event event yang memicu penyimpanan (biasanya onClick dari tombol Simpan);
	 *              tidak digunakan secara langsung
	 * @return {@code true} jika penyimpanan berhasil (form boleh ditutup),
	 *         {@code false} jika validasi gagal atau terjadi kesalahan
	 * @throws Exception jika terjadi kesalahan kritis yang tidak tertangkap
	 */
	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (isEmpty(kode == null ? null : kode.getValue())) {
			MyMessageboxConfig.show("Mohon maaf, Kode Saldo Awal belum diisi. Langkah yang dapat dilakukan: (1) Isi field Kode atau gunakan tombol generate kode; (2) Pastikan kode bersifat unik dan belum digunakan sebelumnya; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (penyedia == null || penyedia.getAttribute("penyediaAsset") == null) {
			MyMessageboxConfig.show("Mohon maaf, Penyedia belum dipilih. Langkah yang dapat dilakukan: (1) Klik tombol pilih Penyedia dan cari penyedia dari daftar; (2) Jika penyedia belum terdaftar, daftarkan terlebih dahulu melalui menu Data Penyedia; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		List<Row> rowsMasterAsset = gridMasterAsset == null || gridMasterAsset.getRows() == null ? new ArrayList<Row>() : (java.util.List) gridMasterAsset.getRows().getChildren();
		for (Row row : rowsMasterAsset) {
			SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) row
					.getAttribute("saldoAwalMasterAssetDetail");
			if (saldoAwalMasterAssetDetail.getMasterAsset() == null) {
				MyMessageboxConfig.show("Mohon maaf, Data Barang pada daftar saldo awal belum lengkap. Langkah yang dapat dilakukan: (1) Klik tombol pilih barang pada baris yang masih kosong; (2) Cari dan pilih barang/aset dari daftar master; (3) ulangi proses simpan ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
		}

		Session session = HibernateUtil.currentSession();
		try {
			if (saldoAwalMasterAsset.getId() != null) {
				saldoAwalMasterAsset = (SaldoAwalMasterAsset) session.load(SaldoAwalMasterAsset.class,
						saldoAwalMasterAsset.getId());
			}
		} catch (Exception e) {
			saldoAwalMasterAsset = new SaldoAwalMasterAsset();
		}

		if (penerimaanPengadaanMasterAsset != null
				&& penerimaanPengadaanMasterAsset.getAttribute("penerimaanPengadaanMasterAsset") != null) {
			saldoAwalMasterAsset
					.setPenerimaanPengadaanMasterAsset((PenerimaanPengadaanMasterAsset) penerimaanPengadaanMasterAsset
							.getAttribute("penerimaanPengadaanMasterAsset"));
		}

		try {
			if (isChecked(tanpaPenerimaan)) {
				saldoAwalMasterAsset.setPenerimaanPengadaanMasterAsset(null);
			}

			saldoAwalMasterAsset.setLokasi(
					(Lokasi) (lokasi.getSelectedItem() == null || lokasi.getSelectedItem().getValue() == null ? null
							: lokasi.getSelectedItem().getValue()));
			saldoAwalMasterAsset.setPemilikAsset((PemilikAsset) (pemilikAsset.getSelectedItem() == null
					|| pemilikAsset.getSelectedItem().getValue() == null ? null
							: pemilikAsset.getSelectedItem().getValue()));
			saldoAwalMasterAsset.setRuang((Ruang) ruang.getAttribute("ruang"));
			saldoAwalMasterAsset.setKode(kode.getValue());
			saldoAwalMasterAsset.setKeterangan(keterangan.getValue());
			saldoAwalMasterAsset.setTanggalPembuatan(tanggalPembuatan.getValue());
			saldoAwalMasterAsset.setDisposisiSop(disposisiSop);
			saldoAwalMasterAsset.setPenyedia((PenyediaAsset) penyedia.getAttribute("penyediaAsset"));

			String kodeTermin = (String) (SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null ? null
					: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getValue());
			saldoAwalMasterAsset.setKodeTermin(kodeTermin);
			saldoAwalMasterAsset.setKeteranganTermin(
					(String) (SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null ? null
							: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getLabel() + " "
									+ SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getDescription()));

			saldoAwalMasterAsset.setJsonTermin((SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem() == null
					|| SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getAttribute("value") == null ? null
							: SaldoAwalMasterAssetAction.this.kodeTermin.getSelectedItem().getAttribute("value") + ""));

			saldoAwalMasterAsset.setTanggalPersetujuanManual(tanggalPersetujuanManual.getValue());

			saldoAwalMasterAsset.setKodeTagihan(kodeTagihan.getValue());
			saldoAwalMasterAsset.setTanggalTagihan(tanggalTagihan.getValue());

			if (saldoAwalMasterAsset.getId() != null) {
				session.update(saldoAwalMasterAsset);
			} else {
				saldoAwalMasterAsset.setDibuatOleh(Common.getCurrentUser());
				String noAgenda = generateCode(
						tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan.getValue(), true);
				kode.setValue(noAgenda);
				saldoAwalMasterAsset.setKode(kode.getValue());
				session.save(saldoAwalMasterAsset);
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		try {
			Double nilai = 0.0;
			for (Row row : rowsMasterAsset) {
				SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) row
						.getAttribute("saldoAwalMasterAssetDetail");
				saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);
				session.saveOrUpdate(saldoAwalMasterAssetDetail);

				Double total = saldoAwalMasterAssetDetail.getHargaTotal();

				nilai += total;
			}

			saldoAwalMasterAsset.setNilai(nilai);
			Common.refreshUpdate(session, saldoAwalMasterAsset);

			session.flush();

			reinitDataDetail(saldoAwalMasterAsset);

			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {

					DaftarPengajuanTransfer.simpanSaldoAwalMasterAsset(saldoAwalMasterAsset);

					cetak(saldoAwalMasterAsset);

				}
			}, "", false, 3000);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		return true;
	}

	/**
	 * <b>Tujuan</b><br>
	 * Memastikan sinkronisasi antara detail Pemesanan Pengadaan, detail Penerimaan
	 * Pengadaan, dan detail Saldo Awal Aset di database. Jika ada detail dari Pemesanan
	 * yang belum memiliki entri di Penerimaan atau Saldo Awal, entri tersebut dibuat otomatis.
	 * Dipanggil setelah {@code onSave()} berhasil untuk menjamin konsistensi data.
	 *
	 * <p><b>Cara kerja</b><br>
	 * Hanya berjalan jika tagihan memiliki ID, memiliki Penerimaan Pengadaan, dan Penerimaan
	 * tersebut memiliki Pemesanan Pengadaan. Kemudian:</p>
	 * <ol>
	 *   <li>Ambil kode termin dan parse JSON termin dari tagihan.</li>
	 *   <li>Query semua {@code PemesananPengadaanMasterAssetDetail}.</li>
	 *   <li>Untuk setiap detail pemesanan:
	 *     <ul>
	 *       <li>Cari {@code PenerimaanPengadaanMasterAssetDetail} yang sesuai (filter
	 *           kodeTermin jika by-termin). Jika tidak ada, buat baru dengan data dari
	 *           pemesanan (harga dari JSON termin jika tersedia), simpan ke database.</li>
	 *       <li>Cari {@code SaldoAwalMasterAssetDetail} yang sesuai. Jika tidak ada, buat
	 *           baru dengan data dari pemesanan (harga, PPh, PPN, diskon), simpan ke database.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan</b><br>
	 * Logika ini mirip dengan {@code generateDetail()} namun khusus untuk konteks pasca-simpan
	 * dengan referensi langsung ke {@code saldoAwalMasterAsset} yang sudah memiliki ID.
	 * Pastikan kedua metode tetap konsisten jika ada perubahan model data.</p>
	 *
	 * @param saldoAwalMasterAsset tagihan vendor yang baru disimpan; harus memiliki ID
	 *                             dan referensi Penerimaan Pengadaan dengan Pemesanan
	 *                             agar proses reinit berjalan
	 */
	@SuppressWarnings("unchecked")
	private void reinitDataDetail(SaldoAwalMasterAsset saldoAwalMasterAsset) {
		if (saldoAwalMasterAsset != null && saldoAwalMasterAsset.getId() != null
				&& saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset() != null && saldoAwalMasterAsset
						.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset() != null) {

			String kodeTermin = saldoAwalMasterAsset.getKodeTermin();
			JSONObject jsonObject = null;
			if (saldoAwalMasterAsset.getJsonTermin() != null) {
				try {
					jsonObject = new JSONObject(saldoAwalMasterAsset.getJsonTermin());
				} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
			}
			Session session = HibernateUtil.currentSession();
			List<PemesananPengadaanMasterAssetDetail> pemesananPengadaanMasterAssetDetails = session
					.createCriteria(PemesananPengadaanMasterAssetDetail.class)
					.add(Restrictions.eq("pemesananPengadaanMasterAsset", saldoAwalMasterAsset
							.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset()))
					.list();
			for (PemesananPengadaanMasterAssetDetail pemesananPengadaanMasterAssetDetail : pemesananPengadaanMasterAssetDetails) {

				PenerimaanPengadaanMasterAssetDetail penerimaanPengadaanMasterAssetDetail = (PenerimaanPengadaanMasterAssetDetail) session
						.createCriteria(PenerimaanPengadaanMasterAssetDetail.class)
						.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset",
								Criteria.LEFT_JOIN)

						.add(kodeTermin == null || kodeTermin.trim().isEmpty()
								|| !isByTermin(saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset().getPemesananPengadaanMasterAsset())
												? Restrictions.sqlRestriction("true")
												: Restrictions.eq("penerimaanPengadaanMasterAsset.kodeTermin",
														kodeTermin))

						.add(Restrictions.eq("pemesananPengadaanMasterAssetDetail",
								pemesananPengadaanMasterAssetDetail))
						.setMaxResults(1).uniqueResult();
				if (penerimaanPengadaanMasterAssetDetail == null) {
					penerimaanPengadaanMasterAssetDetail = new PenerimaanPengadaanMasterAssetDetail();
					penerimaanPengadaanMasterAssetDetail
							.setMasterAsset(pemesananPengadaanMasterAssetDetail.getMasterAsset());
					penerimaanPengadaanMasterAssetDetail.setJumlah(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail.setDiterima(pemesananPengadaanMasterAssetDetail.getJumlah());
					penerimaanPengadaanMasterAssetDetail
							.setKeterangan(pemesananPengadaanMasterAssetDetail.getKeterangan());

					if (jsonObject != null && !jsonObject.isNull("penagihan")) {
						try {
							Double penagihan = jsonObject.getDouble("penagihan");
							penerimaanPengadaanMasterAssetDetail.setHargaBeli(penagihan);
							penerimaanPengadaanMasterAssetDetail.setHargaBeliDiEntry(penagihan);
						} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
						}

					} else {
						penerimaanPengadaanMasterAssetDetail
								.setHargaBeli(pemesananPengadaanMasterAssetDetail.getHargaBeli());
					}

					penerimaanPengadaanMasterAssetDetail
							.setPemesananPengadaanMasterAssetDetail(pemesananPengadaanMasterAssetDetail);

					penerimaanPengadaanMasterAssetDetail.setPenerimaanPengadaanMasterAsset(
							saldoAwalMasterAsset.getPenerimaanPengadaanMasterAsset());
					session.save(penerimaanPengadaanMasterAssetDetail);
					session.flush();

				}

				SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail = (SaldoAwalMasterAssetDetail) session
						.createCriteria(SaldoAwalMasterAssetDetail.class)
						.add(Restrictions.eq("saldoAwal", saldoAwalMasterAsset))
						.add(Restrictions.eq("masterAsset", pemesananPengadaanMasterAssetDetail.getMasterAsset()))
						.setMaxResults(1).uniqueResult();
				if (saldoAwalMasterAssetDetail == null) {
					saldoAwalMasterAssetDetail = new SaldoAwalMasterAssetDetail();
					saldoAwalMasterAssetDetail.setMasterAsset(pemesananPengadaanMasterAssetDetail.getMasterAsset());
					saldoAwalMasterAssetDetail.setJumlah(pemesananPengadaanMasterAssetDetail.getJumlah());
					saldoAwalMasterAssetDetail.setSaldoAwal(saldoAwalMasterAsset);

					if (jsonObject != null && !jsonObject.isNull("penagihan")) {
						try {
							Double penagihan = jsonObject.getDouble("penagihan");
							saldoAwalMasterAssetDetail.setHarga(penagihan);
						} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
						}

					} else {
						saldoAwalMasterAssetDetail.setHarga(pemesananPengadaanMasterAssetDetail.getHargaBeli());
					}

					saldoAwalMasterAssetDetail.setHargaPotongan(pemesananPengadaanMasterAssetDetail.getHargaPotongan());
					saldoAwalMasterAssetDetail
							.setPenerimaanPengadaanMasterAssetDetail(penerimaanPengadaanMasterAssetDetail);
					saldoAwalMasterAssetDetail
							.setJenisPajakBarang(penerimaanPengadaanMasterAssetDetail.getJenisPajakBarang());
					saldoAwalMasterAssetDetail
							.setJenisPajakPpn(penerimaanPengadaanMasterAssetDetail.getJenisPajakPpn());
					saldoAwalMasterAssetDetail.setDiskonDalamBentukPersen(
							penerimaanPengadaanMasterAssetDetail.getDiskonDalamBentukPersen());
					saldoAwalMasterAssetDetail
							.setHargaPotongan(penerimaanPengadaanMasterAssetDetail.getHargaPotongan());
					session.save(saldoAwalMasterAssetDetail);
					session.flush();
				}
			}
		}
	}

	private Checkbox searchaktif;

	public Criteria initCriteria(boolean order) {

		Integer lns = (searchLunas == null || searchLunas.getSelectedItem() == null ? null
				: (Integer) searchLunas.getSelectedItem().getValue());

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(SaldoAwalMasterAsset.class)

				.add(isChecked(searchaktif)
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"))

				.add(start == null || start.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.ge("tanggalPembuatan", start.getValue()))
				.add(end == null || end.getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.lt("tanggalPembuatan", nextDay(end.getValue())))

				// Filter persetujuan. Bila KEDUA checkbox tidak tercentang (default, dan checkbox
				// disetujui/blmDisetujui memang tidak ada di ZUL → selalu null/false), TAMPILKAN SEMUA
				// ("true"). Dulu di sini "false" sehingga grid SELALU kosong.
				.add(isChecked(blmDisetujui) && isChecked(disetujui) ? Restrictions.sqlRestriction("true")
						: !isChecked(blmDisetujui) && !isChecked(disetujui) ? Restrictions.sqlRestriction("true")
								: isChecked(blmDisetujui) ? Restrictions.isNull("disetujuiOleh")
										: Restrictions.isNotNull("disetujuiOleh"))

				.createAlias("penyedia", "penyedia", Criteria.LEFT_JOIN)

				.createAlias("penerimaanPengadaanMasterAsset", "penerimaanPengadaanMasterAsset", Criteria.LEFT_JOIN)

				.add(safeText(searchpenyedia.getValue()).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("penyedia.nama", safeText(searchpenyedia.getValue()),
										MatchMode.ANYWHERE),
								Restrictions.ilike("penyedia.kode", safeText(searchpenyedia.getValue()),
										MatchMode.ANYWHERE)))

				.add(isChecked(lunasSaja) ? Restrictions.eq("lunas", true)
						: Restrictions.sqlRestriction("1=1"))

				.add(isChecked(blmLunasSaja) ? Restrictions.eq("lunas", false)
						: Restrictions.sqlRestriction("1=1"))

				.add(lns == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("lunas", lns.equals(1)))

				.add(safeText(searchketerangan.getValue()).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("keterangan", safeText(searchketerangan.getValue()), MatchMode.ANYWHERE))

				.add(safeText(searchkode.getValue()).isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kode", safeText(searchkode.getValue()), MatchMode.ANYWHERE));

		if (persetujuan) {
			criteria.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset.saldoAwalMasterAsset"))
					.add(Restrictions.isNotNull("penerimaanPengadaanMasterAsset.disetujuiOleh"));
		}

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<SaldoAwalMasterAsset> saldoAwalMasterAsset = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(saldoAwalMasterAsset);
		grid.setRowRenderer(new SaldoAwalMasterAssetRenderer());
		grid.setModelCheckMobile(strset);

	}

	public static String generateCode(Date tanggal, boolean tambah) {
		if (NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA == null
				|| NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat() == null) {
			return Common.getGeneratedBarCode();
		}

		Long index = NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat().getGunakanIndexUrut()
				? NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat().getNomorIndex()
				: getindex(NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat());
		if (tambah) {
			NomorSurat.tambahIndexNomorSurat(NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat());
		}
		String noAgenda = NomorSuratAlurPengadaan.PENERIMAAN_TAGIHAN_DATA.getNomorSurat().format(index, tanggal);
		return ais.action.master.KodeUnikUtil.pastikanUnik(SaldoAwalMasterAsset.class, noAgenda);
	}

	public static Long getindex(NomorSurat nomorSurat) {
		if (nomorSurat == null) {
			return 0L;
		}
		Session session = HibernateUtil.currentSession();
		int tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		int bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		Date sekarang = WaktuUtil.getDate();
		Number indexO = (Number) session.createCriteria(SaldoAwalMasterAsset.class)
				.createAlias("nomorSuratAlurPengadaan", "nomorSuratAlurPengadaan", Criteria.LEFT_JOIN)
				.createAlias("nomorSuratAlurPengadaan.nomorSurat", "nomorSurat", Criteria.LEFT_JOIN)

				.add(nomorSurat.getUrutBerdasarkanNomor()
						? Restrictions.eq("nomorSuratAlurPengadaan.nomorSurat", nomorSurat)

						: (nomorSurat.getUrutBerdasarkanKelompok() && nomorSurat.getKelompokNomorSurat() != null
								? Restrictions.eq("nomorSurat.kelompokNomorSurat", nomorSurat.getKelompokNomorSurat())
								: Restrictions.sqlRestriction("true")))

				.add(nomorSurat.getResetUrutanTiapTahun() ? Restrictions.eq("tahun", tahun)
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetUrutanTiapBulan()
						? Restrictions.and(Restrictions.eq("tahun", tahun), Restrictions.eq("bulan", bulan))
						: Restrictions.sqlRestriction("true"))

				.add(nomorSurat.getResetTiap() != null && (Common.dateFormat8.get().format(nomorSurat.getResetTiap())
						.equals(Common.dateFormat8.get().format(sekarang))
						|| nomorSurat.getResetTiap().before(sekarang))
								? Restrictions.ge("tanggalPembuatan", nomorSurat.getResetTiap())
								: Restrictions.sqlRestriction("true"))

				.setProjection(Projections.rowCount()).uniqueResult();

		Long index = indexO == null ? null : indexO.longValue();
		if (index == null) {
			index = 0L;
		}
		return ++index;
	}

	@Override
	public String istilah() throws Exception {
		return "Terima Tagihan Barang / Jasa";
	}

	@Override
	public DataSop ambil() throws Exception {
				return saldoAwalMasterAsset;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
				return SaldoAwalMasterAsset.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	public static void onAddExternal(EventListener eventListener, SaldoAwalMasterAsset saldoAwal) throws Exception {
		SaldoAwalMasterAssetAction prosesTransferAction = new SaldoAwalMasterAssetAction();
		prosesTransferAction.persetujuan = true;
		prosesTransferAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(prosesTransferAction.addWindow);
		prosesTransferAction.addWindow.setHeight("95%");
		prosesTransferAction.addWindow.setWidth("90%");

		prosesTransferAction.init(saldoAwal);

		prosesTransferAction.addWindow.setVisible(true);
		prosesTransferAction.addWindow.setClosable(true);
		prosesTransferAction.addWindow.onModal();

	}

}
