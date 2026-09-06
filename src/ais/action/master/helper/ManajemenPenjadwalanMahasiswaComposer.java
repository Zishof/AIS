package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.calendar.Calendars;
import org.zkoss.calendar.api.CalendarEvent;
import org.zkoss.calendar.event.CalendarsEvent;
import org.zkoss.calendar.impl.SimpleCalendarEvent;
import org.zkoss.calendar.impl.SimpleCalendarModel;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.metainfo.ComponentInfo;
import org.zkoss.zk.ui.util.GenericForwardComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Center;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PenjadwalanUtil;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.common.CommonPMB;
import ais.common.CommonPenjadwalan;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.OnSearchDefaultListener;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Fakultas;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Kelas;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.PenjadwalanMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.CustomSimpleDateFormatter;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Composer ZK ({@link GenericForwardComposer}) untuk layar <b>"Manajemen Penjadwalan Mahasiswa"</b>
 * — satu halaman dengan DUA region yang saling terhubung lewat filter yang sama (tahun ajaran,
 * fakultas, jurusan, program, semester, kelas):
 *
 * <ol>
 * <li><b>Kalender jadwal perkuliahan (kiri/tengah).</b> Komponen {@link Calendars} (widget ZK
 * calendar/scheduler) menampilkan setiap {@link Perkuliahan} yang cocok filter sebagai satu event
 * kalender (dibangun oleh {@code CalendarPerkuliahanMahasiswa.initModel}, lihat
 * {@link #initCalendarModel()}). Admin dapat men-<i>drag</i> membuat slot baru
 * ({@link #onEventCreate$calendars}) atau membuka slot yang ada ({@link #onEventEdit$calendars})
 * untuk mengedit hari/jam/dosen/ruang — keduanya memvalidasi filter lengkap dan status aktif
 * penjadwalan ({@link CommonPenjadwalan#apakahPenjadwalanTidakAktif}) sebelum mendelegasikan ke
 * {@link #init(Perkuliahan)}, yang membuka dialog detail lewat {@link PenjadwalanUtil} (dengan
 * field filter dikunci/disabled karena konteksnya sudah ditentukan dari layar ini). Navigasi
 * kalender (hari/minggu/bulan, maju/mundur, "hari ini", switch timezone) ditangani method
 * {@code onMoveDate}/{@code onToday}/{@code onUpdateView}/{@code onSwitchTimeZone}/
 * {@code onUpdateFirstDayOfWeek} yang murni delegasi ke API {@link Calendars}.</li>
 * <li><b>Panel roster mahasiswa (kanan, {@link #initDataMahasiswa()}).</b> Dibangun murni via kode
 * Java (bukan dari ZUL), menampilkan {@link PenjadwalanMahasiswa} (paket "mahasiswa terjadwal ke
 * kelas ini") dengan empat aksi: <i>Ambil data Mahasiswa</i> (menambah lewat picker), <i>
 * Singkronisasikan</i> (bentuk {@link Detailperkuliahan}/KRS otomatis untuk SETIAP mahasiswa
 * &times; SETIAP {@link Perkuliahan} yang match filter kelas — divalidasi lagi per mahasiswa
 * lewat batas SKS ({@link Common#checkPembatasanSKSBerdasarkanIP}), status pembayaran
 * ({@link Common#checkStatusPembayaranMahasiswa}), dan bentrok jadwal matakuliah yang sama
 * ({@link #checkMahasiswaBentrok}), dengan ringkasan BERHASIL/GAGAL/DILEWATI per mahasiswa),
 * <i>Batalkan Singkronisasi</i> (hapus {@link Detailperkuliahan} yang belum dinilai,
 * {@code totalNilai <= 0.1}, beserta baris {@code nilai} terkait lewat SQL langsung), dan
 * <i>Bersihkan Daftar</i> (hapus seluruh baris {@code penjadwalan_mahasiswa} kelas+semester ini via
 * SQL langsung). Keempat aksi berbagi satu validasi konteks terpusat
 * ({@link #ambilKonteksKelasTervalidasi()}) yang menggantikan blok validasi yang sebelumnya
 * disalin-tempel di empat tempat.</li>
 * </ol>
 *
 * <p><b>Field ZK yang di-wire</b> (injeksi otomatis {@link GenericForwardComposer} dari ZUL, nama
 * variabel harus cocok {@code id} komponen): {@code calendars} (kalender utama), {@code tahunAjaran}/
 * {@code semester}/{@code fakultas}/{@code jurusan}/{@code program}/{@code kelas} (filter bersama
 * kalender &amp; roster), {@code panelDaftarMahasiswa} (region {@link Center} tempat
 * {@link #initDataMahasiswa()} menyuntikkan UI-nya secara terprogram), serta satu set field terkait
 * DIALOG DETAIL slot perkuliahan ({@code dosen1}..{@code dosen10} untuk tim pengajar hingga 10 orang,
 * {@code ruang}, {@code jamPerkuliahan}, {@code hari}, {@code minggu1}..{@code minggu5}, dsb.) yang
 * dipakai/disiapkan oleh {@link PenjadwalanUtil} yang dipanggil dari {@link #init(Perkuliahan)}.</p>
 *
 * <p><b>Debounce refresh:</b> {@link #onRefresh(Event)} membangun "kunci" dari kombinasi nilai
 * filter terpilih ({@link #bangunKunciRefresh()}) dan mengabaikan panggilan berturut-turut dengan
 * kunci identik dalam jendela 500ms — mencegah kalender di-render ulang berkali-kali saat beberapa
 * event {@code onChange} filter terpicu nyaris bersamaan (mis. saat {@code FakultasEventListener}
 * mengosongkan &amp; mengisi ulang combobox jurusan). Flag {@code sedangSinkronFilter} menahan
 * refresh sepenuhnya selama sinkronisasi combobox fakultas&rarr;jurusan berlangsung.</p>
 *
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK (satu instance per halaman per
 * desktop) dan menyimpan state layar (filter terpilih, model kalender, cache debounce) sebagai field
 * instance — jangan dipakai sebagai singleton atau dibagikan antar desktop/session. Event handler
 * harus tetap memakai konteks pengguna serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericForwardComposer
 * @see PenjadwalanUtil
 * @see Perkuliahan
 * @see PenjadwalanMahasiswa
 */
public class ManajemenPenjadwalanMahasiswaComposer extends GenericForwardComposer implements OnSearchDefaultListener {

	/**
	 * Versi serialisasi warisan {@link GenericForwardComposer}. Nilainya berpola tanggal
	 * ({@code 2010-11-24 09:04}) mengikuti konvensi contoh komponen kalender ZK asal kode ini.
	 */
	protected static final long serialVersionUID = 201011240904L;
	/**
	 * Model data kalender: kumpulan {@code SimpleCalendarEvent} hasil query {@link Perkuliahan}
	 * yang cocok dengan filter aktif. Dibangun ulang seluruhnya oleh {@code initCalendarModel()}
	 * setiap kali {@link #onRefresh(Event)} lolos debounce.
	 */
	protected SimpleCalendarModel cm;
	/**
	 * Komponen kalender ZK utama (di-wire dari ZUL lewat id {@code calendars}). Jam mulai/selesai
	 * dan zona waktunya dikonfigurasi di {@link #doAfterCompose(Component)} dari
	 * {@link Konfigurasi} {@code penjadwalan_jam_mulai}/{@code penjadwalan_jam_selesai}/
	 * {@code penjadwalan_timezone}.
	 */
	protected Calendars calendars;
	/**
	 * 288 slot waktu berformat {@code HH:mm} berjarak 5 menit (00:00..23:55), diisi
	 * {@link #initTimeDropdown(Page)} dan dipakai sebagai sumber pilihan waktu pada dialog detail
	 * jadwal.
	 */
	protected List<String> dateTime = new LinkedList<String>();

	/**
	 * Filter tahun akademik, diisi {@code Common.generateTahunAjaran(...)}. Bagian dari kunci
	 * debounce {@link #bangunKunciRefresh()} dan dari {@code KonteksKelas} yang divalidasi
	 * {@code ambilKonteksKelasTervalidasi()}.
	 */
	protected Combobox tahunAjaran;
	/**
	 * Filter semester, diisi angka 1..23 di {@link #doAfterCompose(Component)} dengan default 1
	 * (ganjil berjalan) atau 2 (genap berjalan) menurut {@code Common.isNowSemensterGanjil()}.
	 */
	protected Combobox semester;
	/**
	 * Filter nama kelas (mis. {@code "A"}), disetel ke {@code "A"} sebagai nilai awal. Berupa teks
	 * bebas: {@code ambilKonteksKelasTervalidasi()} menerjemahkannya menjadi entity {@code Kelas}
	 * lewat pencarian berdasarkan nama, dan menolak konteks bila tidak ada kelas yang cocok.
	 */
	protected AmbilDataKelasBanbox kelas;
	/**
	 * Filter fakultas. Di-{@code setDisabled(true)} bila pengguna hanya berwenang pada satu
	 * fakultas ({@code tbmuser.ambilFakultas() != null}), sekaligus dipaksa terpilih ke fakultas
	 * tersebut. Perubahannya memicu {@code FakultasEventListener} yang mengisi ulang
	 * {@link #jurusan}.
	 */
	protected Combobox fakultas;
	/**
	 * Filter program studi. Isinya bergantung pada {@link #fakultas} yang terpilih, dan
	 * di-{@code setDisabled(true)} bila pengguna hanya berwenang pada satu jurusan
	 * ({@code tbmuser.ambilJurusan() != null}).
	 */
	protected Combobox jurusan;
	/**
	 * Filter program (Reguler, Ekstensi, dan seterusnya), diisi {@code Common.initPrograms(...)}.
	 */
	protected Combobox program;
	/**
	 * Menandai apakah layar sedang mengelola jadwal REMEDIAL. Diteruskan ke
	 * {@code PenjadwalanUtil.init(...)} dari {@link #init(Perkuliahan)} sehingga dialog detail
	 * menyesuaikan aturannya. Bernilai {@code false} secara bawaan dan tidak pernah diubah dari
	 * dalam kelas ini — disediakan untuk sub-kelas yang menimpanya.
	 */
	protected Boolean merupakanRemedial = false;
	/**
	 * Penanda bahwa combobox {@link #jurusan} sedang diisi ulang akibat perubahan
	 * {@link #fakultas}. Selama bernilai {@code true}, {@link #onRefresh(Event)} menolak bekerja
	 * sehingga pengosongan-lalu-pengisian ulang combobox tidak memicu render kalender di tengah
	 * keadaan filter yang belum konsisten. Selalu dikembalikan ke {@code false} lewat blok
	 * {@code finally}.
	 */
	private boolean sedangSinkronFilter;
	/**
	 * Kunci filter dari {@link #onRefresh(Event)} yang terakhir benar-benar dieksekusi (lihat
	 * {@link #bangunKunciRefresh()}). Dipakai bersama {@link #waktuRefreshTerakhir} untuk menolak
	 * panggilan berturut-turut yang kombinasi filternya identik.
	 */
	private String kunciRefreshTerakhir;
	/**
	 * Cap waktu (milidetik) refresh terakhir yang dieksekusi. Panggilan dengan kunci sama dalam
	 * jendela 500 ms diabaikan — inilah debounce yang mencegah kalender di-render berkali-kali saat
	 * beberapa event {@code onChange} filter terpicu nyaris bersamaan.
	 */
	private long waktuRefreshTerakhir;

	/**
	 * Tanggal mulai pada dialog detail slot jadwal. Diinisialisasi langsung (bukan lewat wiring
	 * ZUL) agar tidak {@code null} pada jalur yang membacanya sebelum dialog dibangun.
	 */
	protected MyDatebox ppbegin = new MyDatebox();
	/**
	 * Jam mulai pada dialog detail slot jadwal.
	 */
	protected MyTimebox waktuMulai;
	/**
	 * Tanggal selesai pada dialog detail slot jadwal; sejajar dengan {@link #ppbegin}.
	 */
	protected MyDatebox ppend = new MyDatebox();
	/**
	 * Jam selesai pada dialog detail slot jadwal.
	 */
	protected MyTimebox waktuSelesai;
	/**
	 * Penanda "sepanjang hari" pada dialog detail slot jadwal; bila dicentang, jam mulai/selesai
	 * tidak dipakai.
	 */
	protected MyCheckboxConfig ppallDay;
	/**
	 * Pilihan warna tampilan slot pada kalender.
	 */
	protected Combobox ppcolor;
	/**
	 * Isi/keterangan slot jadwal yang ditampilkan sebagai teks event pada kalender.
	 */
	protected Textbox ppcnt;
	/**
	 * Penanda slot terkunci — slot yang terkunci tidak dapat digeser lewat drag pada kalender.
	 */
	protected MyCheckboxConfig pplocked;
	/**
	 * Penanda bahwa perkuliahan ini merupakan KELAS PARALEL dari perkuliahan lain; bila dicentang,
	 * {@link #perkuliahan_paralel} menentukan perkuliahan induknya.
	 */
	protected MyCheckboxConfig merupakan_paralel;
	/**
	 * Perkuliahan induk yang diikuti bila {@link #merupakan_paralel} dicentang.
	 */
	protected Combobox perkuliahan_paralel;

	/**
	 * Pilihan hari (Senin..Minggu) pada dialog detail jadwal, diisi dari {@code Common.haris}.
	 * Dibuat ulang secara terprogram di {@link #doAfterCompose(Component)}, menimpa instance hasil
	 * wiring ZUL bila ada.
	 */
	protected Combobox hari;

	/**
	 * Penanda perkuliahan berlangsung pada minggu ke-1 dalam bulan. Kelima penanda minggu hanya
	 * ditampilkan bila konfigurasi {@link #tampilkanMingguPerkuliahan} aktif.
	 */
	protected MyCheckboxConfig minggu1;
	/**
	 * Penanda perkuliahan berlangsung pada minggu ke-2 dalam bulan.
	 */
	protected MyCheckboxConfig minggu2;
	/**
	 * Penanda perkuliahan berlangsung pada minggu ke-3 dalam bulan.
	 */
	protected MyCheckboxConfig minggu3;
	/**
	 * Penanda perkuliahan berlangsung pada minggu ke-4 dalam bulan.
	 */
	protected MyCheckboxConfig minggu4;
	/**
	 * Penanda perkuliahan berlangsung pada minggu ke-5 dalam bulan.
	 */
	protected MyCheckboxConfig minggu5;

	/**
	 * Matakuliah yang diampu pada slot jadwal yang sedang dibuat/diedit.
	 */
	protected Combobox matakuliah;
	/**
	 * Dosen pengampu ke-1. Kelas ini menyediakan sepuluh slot dosen
	 * ({@code dosen1}..{@code dosen10}) untuk tim pengajar; jumlah baris yang tampak dikendalikan
	 * {@link #jumlahDosen} lewat {@code rowdosen1}..{@code rowdosen10}.
	 */
	protected AmbilDataDosenBanbox dosen1;
	/**
	 * Dosen pengampu ke-2; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen2;

	/**
	 * Filter waktu perkuliahan: PAGI, SIANG, SORE, atau MALAM. Dibuat secara terprogram di
	 * {@link #doAfterCompose(Component)}.
	 */
	protected Combobox waktu;
	// protected Textbox kelas;
	/**
	 * Kurikulum acuan matakuliah pada slot jadwal.
	 */
	protected Combobox kurikulum;

	/**
	 * Perkuliahan yang sedang dibuka pada dialog detail — kosong berisi hari/jam hasil drag
	 * kalender ketika membuat baru, atau entity lengkap ketika mengedit slot yang sudah ada.
	 */
	protected Perkuliahan perkuliahan;
	/**
	 * Id seluruh {@link Perkuliahan} yang cocok dengan filter kelas aktif, diisi saat kalender
	 * di-render ulang. Menjadi sumber iterasi aksi massal roster: "Singkronisasikan" membentuk KRS
	 * untuk setiap mahasiswa &times; setiap id di sini, dan "Batalkan Singkronisasi" menghapusnya
	 * kembali.
	 *
	 * <p>Karena diisi oleh alur render kalender, aksi massal bergantung pada kalender yang sudah
	 * ter-refresh lebih dulu; nilai basi di sini berarti aksi massal bekerja pada himpunan
	 * perkuliahan yang tidak lagi sesuai layar.</p>
	 */
	protected List<Long> perkuliahans;

	/**
	 * Grid baris dosen pada dialog detail jadwal, wadah {@code rowdosen1}..{@code rowdosen10}.
	 */
	protected MyGrid gridDosen;

	/**
	 * Pengguna yang sedang login, diambil sekali saat instance composer dibuat.
	 *
	 * <p>Perhatikan bahwa {@link #doAfterCompose(Component)} dan beberapa listener memanggil
	 * {@code Common.getCurrentUser()} LAGI secara lokal alih-alih memakai field ini — keduanya
	 * menunjuk pengguna yang sama, jadi perbedaannya hanya gaya penulisan.</p>
	 */
	protected Tbmuser tbmuser = Common.getCurrentUser();

	/**
	 * Format jam {@code HH.mm} untuk label slot pada kalender.
	 *
	 * <p>{@link SimpleDateFormat} TIDAK aman-thread, tetapi instance ini milik satu composer yang
	 * hidup pada satu desktop ZK dan hanya disentuh dari event thread, sehingga aman dalam pemakaian
	 * di kelas ini.</p>
	 */
	protected SimpleDateFormat dateFormat = new SimpleDateFormat("HH.mm");
	/**
	 * Filter/pemilih ruang. Dibuat ulang secara terprogram di {@link #doAfterCompose(Component)}
	 * beserta listener yang memicu {@link #onRefresh(Event)} setiap nilainya berubah.
	 */
	protected AmbilDataRuangBanbox ruang;
	/**
	 * Kapasitas peserta kelas pada dialog detail jadwal.
	 */
	protected Decimalbox kapasitasKelas;
	/**
	 * Pemilih slot jam perkuliahan baku (mis. sesi ke-1, ke-2) pada dialog detail jadwal.
	 */
	protected AmbilDataJamPerkuliahanBanbox jamPerkuliahan;

	/**
	 * Penanda periode Semester Pendek. Bernilai {@code null} untuk semester reguler dan diteruskan
	 * apa adanya ke {@code PenjadwalanUtil.init(...)} serta ke pemeriksaan batas SKS pada aksi
	 * "Singkronisasikan". Tidak pernah diubah dari dalam kelas ini — disediakan untuk sub-kelas.
	 */
	protected Integer semesterPendek = null;

	/**
	 * Jumlah dosen pengampu yang aktif (1..10); menentukan baris {@code rowdosen*} mana yang
	 * ditampilkan pada dialog detail jadwal.
	 */
	protected Combobox jumlahDosen;
	/**
	 * Baris grid untuk {@link #dosen1} pada dialog detail jadwal.
	 */
	protected Row rowdosen1;
	/**
	 * Penanda perkuliahan tanpa dosen pengampu; bila dicentang, seluruh baris dosen disembunyikan.
	 */
	protected MyCheckboxConfig merupakan_tanpa_dosen;
	/**
	 * Baris grid untuk {@link #dosen2}.
	 */
	protected Row rowdosen2;
	/**
	 * Baris grid untuk {@link #dosen3}.
	 */
	protected Row rowdosen3;
	/**
	 * Dosen pengampu ke-3; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen3;
	/**
	 * Baris grid untuk {@link #dosen4}.
	 */
	protected Row rowdosen4;
	/**
	 * Dosen pengampu ke-4; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen4;
	/**
	 * Baris grid untuk {@link #dosen5}.
	 */
	protected Row rowdosen5;
	/**
	 * Dosen pengampu ke-5; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen5;
	/**
	 * Baris grid untuk {@link #dosen6}.
	 */
	protected Row rowdosen6;
	/**
	 * Dosen pengampu ke-6; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen6;
	/**
	 * Baris grid untuk {@link #dosen7}.
	 */
	protected Row rowdosen7;
	/**
	 * Dosen pengampu ke-7; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen7;
	/**
	 * Baris grid untuk {@link #dosen8}.
	 */
	protected Row rowdosen8;
	/**
	 * Dosen pengampu ke-8; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen8;
	/**
	 * Baris grid untuk {@link #dosen9}.
	 */
	protected Row rowdosen9;
	/**
	 * Dosen pengampu ke-9; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen9;
	/**
	 * Baris grid untuk {@link #dosen10}.
	 */
	protected Row rowdosen10;
	/**
	 * Dosen pengampu ke-10 — slot terakhir yang didukung; lihat {@link #dosen1}.
	 */
	protected AmbilDataDosenBanbox dosen10;

	/**
	 * Tanggal awal rentang berlakunya jadwal perkuliahan pada dialog detail.
	 */
	protected MyDatebox perkuliahanDimulai;
	/**
	 * Tanggal akhir rentang berlakunya jadwal perkuliahan pada dialog detail.
	 */
	protected MyDatebox perkuliahanSampai;

	/**
	 * Region {@link Center} pada ZUL tempat {@code initDataMahasiswa()} menyuntikkan seluruh UI
	 * roster mahasiswa (penjelasan, toolbar aksi, toolbar pencarian, dan grid) secara terprogram —
	 * panel ini sengaja dibiarkan kosong di ZUL.
	 */
	protected Center panelDaftarMahasiswa;
	/**
	 * Komponen paging roster mahasiswa. Dibuat secara terprogram di
	 * {@link #doAfterCompose(Component)} dan dihubungkan ke {@code loadDataMahasiswa(null)} lewat
	 * {@code Common.initPaging(...)}.
	 */
	protected Paging paging;

	/**
	 * Membuka dialog detail satu slot jadwal perkuliahan (buat baru atau edit) lewat
	 * {@link PenjadwalanUtil}, dengan field filter konteks (kelas/program/semester/tahun ajaran/
	 * fakultas/jurusan) DIKUNCI (disabled) karena nilainya sudah ditentukan dari layar kalender ini,
	 * serta opsi "tanpa jadwal perkuliahan" disembunyikan (tidak relevan di alur ini). Refresh
	 * kalender otomatis dipicu ({@link #onRefresh}) lewat callback {@link OnSearchDefaultListener}
	 * saat dialog ditutup dengan perubahan.
	 *
	 * @param perkuliahan data awal dialog: {@link Perkuliahan} kosong berisi hari/jam/kelas terisi
	 *            dari drag kalender ({@link #onEventCreate$calendars}), atau entity lengkap hasil
	 *            query saat mengedit slot yang sudah ada ({@link #onEventEdit$calendars})
	 */
	@SuppressWarnings({})
	protected void init(final Perkuliahan perkuliahan) throws Exception {

		PenjadwalanUtil penjadwalanUtil;
		(penjadwalanUtil = new PenjadwalanUtil(new OnSearchDefaultListener() {

			/**
			 * Callback yang dipasang ke {@link PenjadwalanUtil}: dipanggil saat dialog detail slot jadwal
			 * ditutup dengan perubahan, dan memicu render ulang kalender lewat {@code onRefresh(null)}.
			 *
			 * <p>Argumen {@code null} disengaja — {@link #onRefresh(Event)} tidak membaca event-nya, dan
			 * {@code null} membuat refresh berjalan lewat jalur yang sama dengan render awal.</p>
			 *
			 * @param event event dari dialog; isinya tidak dipakai.
			 */
			@Override
			public void onSearchDefault(Event event) {
				onRefresh(null);
			}
		})).init(perkuliahan, semesterPendek, null, merupakanRemedial);
		penjadwalanUtil.kelas.setDisabled(true);
		penjadwalanUtil.program.setDisabled(true);
		penjadwalanUtil.semester.setDisabled(true);
		penjadwalanUtil.tahunAjaran.setDisabled(true);
		penjadwalanUtil.fakultas.setDisabled(true);
		penjadwalanUtil.jurusan.setDisabled(true);
		penjadwalanUtil.merupakan_tanpa_jadwal_perkuliahan.setVisible(false);

	}

	/**
	 * Mengisi combobox {@code perkuliahan_paralel} dengan kandidat {@link Perkuliahan} yang bisa
	 * dijadikan "kelas paralel" (matakuliah/program/jurusan/tahun ajaran/semester sama, belum
	 * berstatus paralel sendiri, bukan slot ini sendiri) — dipakai saat sebuah kelas ingin dibuat
	 * sebagai paralel dari kelas lain (berbagi kapasitas/mahasiswa). Memvalidasi lebih dulu bahwa
	 * tahun ajaran, program, jurusan, semester, dan matakuliah sudah dipilih di dialog; menampilkan
	 * pesan peringatan &amp; berhenti bila ada yang kosong. Label combobox menampilkan nama dosen +
	 * matakuliah + id; deskripsi (tooltip) menampilkan ringkasan dosen/semester/kelas/ruang/hari/jam.
	 */
	@SuppressWarnings("unchecked")
	protected void generatePerkulihaanParalel() throws Exception {
		Common.clear(perkuliahan_paralel);

		if (tahunAjaran.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, tahun akademik belum dipilih. Langkah yang dapat dilakukan: (1) pilih tahun akademik dari daftar yang tersedia; (2) pastikan data tahun akademik sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, program studi belum dipilih. Langkah yang dapat dilakukan: (1) pilih program dari daftar yang tersedia; (2) pastikan data program sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, semester belum dipilih. Langkah yang dapat dilakukan: (1) pilih semester dari daftar yang tersedia; (2) pastikan data semester sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		if (matakuliah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, mata kuliah belum dipilih. Langkah yang dapat dilakukan: (1) pilih mata kuliah dari daftar yang tersedia; (2) pastikan data mata kuliah sudah ada di sistem; (3) ulangi proses ini. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		// if (dosen1.getAttribute("myValue") == null) {
		// MyMessageboxConfig.show("Dosen 1 harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return;
		// }

		List<Perkuliahan> perkuliahan = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.addOrder(Order.desc("id"))
				.add(this.perkuliahan.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.perkuliahan.getId()))
				.add(Restrictions.or(Restrictions.eq("merupakan_paralel", false),
						Restrictions.isNull("merupakan_paralel")))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.add(Restrictions.eq("program", program.getSelectedItem().getValue()))

				.add(Restrictions.eq("matakuliah", matakuliah.getSelectedItem().getValue()))

				.add(Restrictions.eq("tahunAjaran", tahunAjaran.getSelectedItem().getValue()))
				.add(Restrictions.eq("semester", semester.getSelectedItem().getValue()))

				.add(Restrictions.isNull("statusSemesterPendek")).add(Restrictions.isNull("ganjilGenap"))

				.createCriteria("jurusan", Criteria.LEFT_JOIN)
				.add(CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false)).list();
		for (Perkuliahan o : perkuliahan) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel((o.getDosen1() == null ? "" : o.getDosen1().getNama()) + " - "
					+ o.getMatakuliah().getNama() + " (" + o.getId() + ")");
			comboitem.setValue(o);

			String deskripsi = "Dosen: " + (o.getDosen1() == null ? "" : o.getDosen1().getNama()) + ",Smt: "
					+ (o.getSemester() + (o.getKelas() == null || o.getKelas().equals("") ? "" : " " + o.getKelas()))
					+ ", Ruang: " + (o.getRuang() == null ? "" : o.getRuang().getKodeRuangan()) + ", Hari: "
					+ o.getHari() + ", Waktu: " + o.getWaktuMulai() + "-" + o.getWaktuSelesai();

			comboitem.setDescription(deskripsi);
			perkuliahan_paralel.appendChild(comboitem);
		}
	}

	/**
	 * Titik refresh bersama kalender &amp; roster mahasiswa, dipanggil dari seluruh listener
	 * perubahan filter (fakultas/jurusan/dst.) maupun {@link #onSearchDefault}. Di-debounce via
	 * {@link #bangunKunciRefresh()}: dilewati bila flag {@code sedangSinkronFilter} aktif (combobox
	 * fakultas&rarr;jurusan sedang disinkronkan) ATAU bila kombinasi filter identik dengan panggilan
	 * terakhir dalam jendela 500ms (mencegah render kalender berulang saat beberapa event
	 * {@code onChange} terpicu nyaris bersamaan). Efek: membangun ulang model kalender
	 * ({@link #initCalendarModel()}), memaksa {@link Calendars#invalidate()}, dan memuat ulang tabel
	 * roster mahasiswa ({@link #loadDataMahasiswa(Object)}).
	 *
	 * @param event event pemicu (tidak dipakai isinya, hanya penanda ada perubahan)
	 */
	public void onRefresh(Event event) {
		if (sedangSinkronFilter) {
			return;
		}
		String kunciRefresh = bangunKunciRefresh();
		long sekarang = System.currentTimeMillis();
		if (kunciRefresh.equals(kunciRefreshTerakhir) && sekarang - waktuRefreshTerakhir < 500) {
			return;
		}
		kunciRefreshTerakhir = kunciRefresh;
		waktuRefreshTerakhir = sekarang;
		initCalendarModel();
		calendars.invalidate();
		loadDataMahasiswa(null);
	}

	/** Menggabungkan nilai semua combobox filter + kelas menjadi satu kunci string untuk deteksi "tidak ada perubahan" di {@link #onRefresh}. */
	private String bangunKunciRefresh() {
		return nilaiTerpilih(tahunAjaran) + "|" + nilaiTerpilih(fakultas) + "|" + nilaiTerpilih(jurusan) + "|"
				+ nilaiTerpilih(program) + "|" + nilaiTerpilih(semester) + "|"
				+ (kelas == null || kelas.getValue() == null ? "" : kelas.getValue().trim());
	}

	/** Nilai combobox terpilih sebagai string stabil untuk perbandingan kunci (ID untuk {@link Fakultas}/{@link Jurusan}, {@code String.valueOf} untuk tipe lain, {@code ""} bila tidak ada pilihan). */
	private String nilaiTerpilih(Combobox combo) {
		if (combo == null || combo.getSelectedItem() == null || combo.getSelectedItem().getValue() == null) {
			return "";
		}
		Object value = combo.getSelectedItem().getValue();
		if (value instanceof Fakultas) {
			return "fakultas:" + ((Fakultas) value).getId();
		}
		if (value instanceof Jurusan) {
			return "jurusan:" + ((Jurusan) value).getId();
		}
		return String.valueOf(value);
	}

	/** Pengaman keamanan halaman ({@link Common#doCheckSecurity()}) dan penyiapan slot dropdown waktu ({@link #initTimeDropdown(Page)}) sebelum ZUL di-compose. */
	@Override
	public ComponentInfo doBeforeCompose(Page page, Component parent, ComponentInfo compInfo) {
		Common.doCheckSecurity();
		initTimeDropdown(page);
		return super.doBeforeCompose(page, parent, compInfo);
	}

	/**
	 * Konfigurasi {@code tampilkan_minggu_perkuliahan} (default AKTIF) yang menentukan apakah
	 * penanda {@code minggu1}..{@code minggu5} ditampilkan pada dialog detail jadwal. Dibaca sekali
	 * di {@link #doAfterCompose(Component)}.
	 *
	 * <p>Karena dibaca sekali per pembukaan halaman, perubahan konfigurasi ini baru berlaku setelah
	 * halaman dimuat ulang.</p>
	 */
	protected Konfigurasi tampilkanMingguPerkuliahan;
	/**
	 * Grid roster mahasiswa terjadwal (mold {@code paging}, 10 baris per halaman). Dibuat
	 * terprogram oleh {@code initDataMahasiswa()} dan diisi ulang oleh
	 * {@code loadDataMahasiswa(...)} dengan {@link DetailKelasRenderer} sebagai penggambar barisnya.
	 */
	private MyGrid grid;
	/**
	 * Kotak pencarian NIM pada toolbar roster; menekan Enter ({@code Events.ON_OK}) memicu
	 * pemuatan ulang daftar.
	 */
	private Textbox nim;
	/**
	 * Kotak pencarian nama mahasiswa pada toolbar roster; menekan Enter memicu pemuatan ulang.
	 */
	private Textbox nama;
	/**
	 * Kotak pencarian tahun angkatan pada toolbar roster; menekan Enter memicu pemuatan ulang.
	 */
	private Intbox angkatan;

	/**
	 * Inisialisasi lengkap layar setelah ZUL selesai di-compose: pengecekan sesi/hak akses baca
	 * ({@link CommonPrivilages#checkPrevilages}, logoff paksa bila gagal), baca konfigurasi tampilan
	 * minggu perkuliahan, wiring listener refresh untuk {@code kelas}/{@code ruang}, pengisian
	 * combobox {@code semester} (1..23) dengan default sesuai ganjil/genap berjalan, pengisian
	 * {@code tahunAjaran}, konfigurasi jam/timezone kalender dari {@link Konfigurasi}
	 * ({@code penjadwalan_jam_mulai}/{@code penjadwalan_jam_selesai}/{@code penjadwalan_timezone}),
	 * pengisian combobox {@code hari}/{@code waktu}/{@code program}, penguncian
	 * {@code fakultas}/{@code jurusan} bila user hanya berwenang pada fakultas/jurusan tertentu
	 * (scoping otorisasi), inisialisasi paging &amp; panel roster ({@link #initDataMahasiswa()}),
	 * dan render awal ({@link #onRefresh(Event)}).
	 */
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		//
		// FDOW.setVisible("month".equals(calendars.getMold())
		// || calendars.getDays() == 7);

		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tampilkanMingguPerkuliahan = Common.getKonfigurasi("tampilkan_minggu_perkuliahan", Konfigurasi.AKTIF);

		kelas.setEventListener(new EventListener() {

			/**
			 * Listener perubahan nilai filter {@link #kelas}: meneruskan langsung ke
			 * {@link #onRefresh(Event)} sehingga kalender dan roster mengikuti kelas yang dipilih.
			 *
			 * @param arg0 event perubahan dari banbox kelas.
			 * @throws Exception diteruskan dari proses refresh.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});
		kelas.setValue("A");

		ruang = new AmbilDataRuangBanbox();
		ruang.setEventListener(new EventListener() {

			/**
			 * Listener perubahan nilai filter {@link #ruang}: meneruskan langsung ke
			 * {@link #onRefresh(Event)}.
			 *
			 * @param arg0 event perubahan dari banbox ruang.
			 * @throws Exception diteruskan dari proses refresh.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				onRefresh(arg0);
			}
		});

		for (int i = 1; i <= 23; i++) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(i + "");
			comboitem.setValue(i);
			semester.appendChild(comboitem);
		}
		Boolean ganjilSekarang = Common.isNowSemensterGanjil();
		Common.selectComboItem(semester,
				ganjilSekarang == null || ganjilSekarang.booleanValue() ? Integer.valueOf(1) : Integer.valueOf(2));

		Common.generateTahunAjaran(tahunAjaran);

		calendars.setDateFormatter(new CustomSimpleDateFormatter());
		calendars.setTimeslots(4);
		Konfigurasi penjadwalanjamMulai = Common.getKonfigurasi("penjadwalan_jam_mulai", Konfigurasi.AKTIF, "7", "",
				"");
		Konfigurasi penjadwalanjamSelesai = Common.getKonfigurasi("penjadwalan_jam_selesai", Konfigurasi.AKTIF, "23",
				"", "");
		Konfigurasi penjadwalanTimezone = Common.getKonfigurasi("penjadwalan_timezone", Konfigurasi.AKTIF,
				"Jakarta=GMT+7", "", "");

		if (penjadwalanTimezone.getNilai().equals(Konfigurasi.AKTIF)) {
			calendars.setTimeZone(penjadwalanTimezone.getInfo1());
		}

		if (penjadwalanjamMulai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer mulai = 7;
			try {
				mulai = Integer.parseInt(penjadwalanjamMulai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:335");
			}
			calendars.setBeginTime(mulai);
		}
		if (penjadwalanjamSelesai.getNilai().equals(Konfigurasi.AKTIF)) {
			Integer sampai = 23;
			try {
				sampai = Integer.parseInt(penjadwalanjamSelesai.getInfo1().trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:343");
			}
			calendars.setEndTime(sampai);
		}

		hari = new Combobox();
		for (String h : Common.haris) {
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(h);
			comboitem.setValue(h);
			hari.appendChild(comboitem);

		}

		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Common.insertCombo(fakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		/**
		 * Event listener lokal milik {@link ManajemenPenjadwalanMahasiswaComposer}. Kelas ini menangani event untuk
		 * komponen induk dan meneruskan pekerjaan domain ke method/service yang sudah tersedia.
		 *
		 * <p><b>Scope:</b> setiap instance terikat pada instance {@link ManajemenPenjadwalanMahasiswaComposer} dan
		 * dapat mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
		 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code onEvent}(). Aturan bisnis bersama
		 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
		 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
		 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
		 * renderer/listener ini.</p>
		 *
		 * @see ManajemenPenjadwalanMahasiswaComposer
		 */
		class FakultasEventListener implements EventListener {

			/**
			 * Menyinkronkan isi combobox {@link #jurusan} mengikuti {@link #fakultas} yang baru dipilih,
			 * lalu memicu {@link #onRefresh(Event)}.
			 *
			 * <p>Selama pengisian ulang berlangsung, {@link #sedangSinkronFilter} ditahan bernilai
			 * {@code true} (dipulihkan lewat {@code finally}) agar pengosongan-lalu-pengisian combobox tidak
			 * memicu refresh di tengah keadaan filter yang belum konsisten. Bila tidak ada fakultas
			 * terpilih, seluruh jurusan aktif dimuat; bila ada, jurusan disaring ke fakultas tersebut.</p>
			 *
			 * @param event event {@code onChange} dari combobox fakultas.
			 * @throws Exception diteruskan dari pengisian combobox atau proses refresh.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				sedangSinkronFilter = true;
				try {
					Common.clear(jurusan);
					jurusan.setSelectedItem(null);
					if (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null) {
						Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
					} else {
						Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
								CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
					}
				} finally {
					sedangSinkronFilter = false;
				}
				onRefresh(event);
			}

		}

		fakultas.addEventListener("onChange", new FakultasEventListener());

		waktu = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("PAGI");
		comboitem.setValue("PAGI");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SIANG");
		comboitem.setValue("SIANG");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("SORE");
		comboitem.setValue("SORE");
		waktu.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("MALAM");
		comboitem.setValue("MALAM");
		waktu.appendChild(comboitem);

		Common.initPrograms(program);

		// Apabila user berwenang hanya di fakultas tertentu, maka user hanya
		// boleh mengakses data fakultas atau jurusan tertentu

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(fakultas, tbmuser.ambilFakultas());
			Common.clear(jurusan);
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					Restrictions.eq("fakultas", tbmuser.ambilFakultas()));
			fakultas.setDisabled(true);
		} else {
			fakultas.setDisabled(false);
		}

		if (tbmuser.ambilJurusan() != null) {
			Common.pilihJurusan(jurusan, tbmuser.ambilJurusan());
			jurusan.setDisabled(true);
		} else {
			jurusan.setDisabled(false);
		}

		calendars.addEventListener(Events.ON_CHANGE, new EventListener() {

			/**
			 * Listener {@code onChange} pada komponen kalender. Saat ini hanya mencetak penanda ke
			 * {@code System.out} — sisa jejak pengembangan yang tidak melakukan pekerjaan apa pun.
			 * Dipertahankan apa adanya agar perilaku existing tidak berubah.
			 *
			 * @param arg0 event perubahan dari kalender; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				System.out.println(
						"======================================= on Chnage ==========================================");
			}
		});

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			/**
			 * Listener paging roster: memuat ulang halaman daftar mahasiswa lewat
			 * {@code loadDataMahasiswa(null)} setiap kali pengguna berpindah halaman.
			 *
			 * @param arg0 event paging; isinya tidak dipakai.
			 * @throws Exception diteruskan dari pemuatan data.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataMahasiswa(null);
			}
		});
		initDataMahasiswa();
		onRefresh(null);

	}

	/** Mengisi {@link #dateTime} dengan 288 slot waktu berformat {@code HH:mm} berjarak 5 menit (00:00..23:55), sumber pilihan waktu untuk dialog detail jadwal. */
	protected void initTimeDropdown(Page page) {

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);

		for (int i = 0; i < 288; i++) {
			dateTime.add(sdf.format(calendar.getTime()));
			calendar.add(Calendar.MINUTE, 5);
		}
	}

	/**
	 * Membangun ulang model kalender dari filter terpilih: query id {@link Perkuliahan} aktif,
	 * bukan bagian dari kelas paralel lain, sesuai semester (pendek/reguler),
	 * {@code kelas}/{@code tahunAjaran}/{@code semester} persis, disaring lagi oleh
	 * jurusan (bila dipilih) atau fakultas (fallback bila jurusan kosong), dan program (bila
	 * dipilih). Tidak melakukan apa pun (return dini) bila tahun ajaran/semester belum dipilih atau
	 * nama kelas kosong. Hasil id disimpan ke {@link #perkuliahans} (dipakai ulang oleh proses
	 * sinkronisasi roster) lalu diterjemahkan ke {@link SimpleCalendarModel} oleh
	 * {@code CalendarPerkuliahanMahasiswa.initModel} dan dipasang ke {@link #calendars}.
	 */
	@SuppressWarnings("unchecked")
	protected void initCalendarModel() {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || kelas.equals("")) {

			return;
		}
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Perkuliahan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.setProjection(Projections.property("id"))
				.add(Restrictions.isNull("perkuliahan_paralel"))
				.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
						: Restrictions.eq("statusSemesterPendek", semesterPendek))
				.add(Restrictions.ilike("kelas", kelas, MatchMode.EXACT))
				.add(Restrictions.eq("tahunAjaran", tahunAkademik))
				.add(Restrictions.eq("semester", semester));
		if (jurusan != null) {
			criteria.add(Restrictions.eq("jurusan", jurusan));
		} else if (fakultas != null) {
			criteria.createCriteria("jurusan", Criteria.LEFT_JOIN).add(Restrictions.eq("fakultas", fakultas));
		}
		if (program != null) {
			criteria.add(Restrictions.eq("program", program));
		}
		perkuliahans = criteria.list();
		System.out.println("perkuliahan = " + perkuliahans.size());
		// fill the events' data
		SimpleCalendarModel cm = new SimpleCalendarModel();

		CalendarPerkuliahanMahasiswa.initModel(cm, perkuliahans);

		calendars.setModel(cm);
		calendars.onInitRender();
	}

	/**
	 * Handler ZK untuk drag-membuat slot baru pada {@link #calendars} (naming convention
	 * {@code on<Event>$<componentId>}). Memvalidasi filter lengkap (fakultas/prodi/jurusan/tahun
	 * ajaran/semester/kelas) dan status aktif penjadwalan periode terkait
	 * ({@link CommonPenjadwalan#apakahPenjadwalanTidakAktif}); bila lolos, membentuk
	 * {@link Perkuliahan} baru berisi hari (dari tanggal drag) dan jam mulai/selesai (dari rentang
	 * drag) lalu membuka dialog detail ({@link #init(Perkuliahan)}). {@code evt.stopClearGhost()}
	 * mencegah widget kalender menghapus "bayangan" slot yang baru digambar sebelum dialog selesai.
	 *
	 * @param event event ZK asli dari widget calendar ({@link CalendarsEvent} via
	 *            {@code event.getOrigin()})
	 */
	public void onEventCreate$calendars(ForwardEvent event) throws Exception {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || fakultas == null || jurusan == null || program == null
				|| kelas.equals("")) {
			MyMessageboxConfig.show(
					"Fakultas" + ", Program Studi, Program, Tahun Akademik, Semester, dan Kelas harus dipilih",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		String ganjilGenap = perkuliahan == null ? null : perkuliahan.getGanjilGenap();
		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(tahunAkademik, ganjilGenap,
				semesterPendek, fakultas, jurusan, program)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + tahunAkademik + "\" semester \""
							+ ganjilGenap + "\" tidak diaktifkan",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		Calendar begin = ais.ui.util.WaktuUtil.getCalendar();
		begin.setTime(evt.getBeginDate());

		Perkuliahan perkuliahan = new Perkuliahan();
		perkuliahan.setWaktuMulai(dateFormat.format(evt.getBeginDate()));
		perkuliahan.setWaktuSelesai(dateFormat.format(evt.getEndDate()));
		perkuliahan.setHari(Common.haris[begin.get(Calendar.DAY_OF_WEEK) - 1]);
		perkuliahan.setKelas(kelas);
		perkuliahan.setProgram(program);
		perkuliahan.setJurusan(jurusan);
		perkuliahan.setTahunAjaran(tahunAkademik);
		perkuliahan.setSemester(semester);
		init(perkuliahan);

		evt.stopClearGhost();
	}

	/**
	 * Handler ZK untuk membuka slot {@link Perkuliahan} yang sudah ada (klik pada event kalender).
	 * Memvalidasi filter lengkap, memuat ulang {@link Perkuliahan} by id (id disimpan sebagai
	 * {@code ce.getTitle()} pada event kalender), lalu menegakkan SCOPING OTORISASI: pengguna yang
	 * dibatasi ke fakultas/jurusan tertentu TIDAK BOLEH mengubah jadwal milik
	 * fakultas/jurusan lain (ditolak dengan pesan spesifik menyebut nama fakultas/jurusannya) —
	 * pengecekan ini terpisah dari (dan lebih ketat dari) filter combobox layar. Setelah lolos
	 * otorisasi dan status penjadwalan aktif ({@link CommonPenjadwalan#apakahPenjadwalanTidakAktif}),
	 * membuka dialog detail ({@link #init(Perkuliahan)}) untuk diedit.
	 *
	 * @param event event ZK asli dari widget calendar; {@code getCalendarEvent().getTitle()} berisi
	 *            id {@link Perkuliahan} sebagai string
	 */
	public void onEventEdit$calendars(ForwardEvent event) throws Exception {

		String tahunAkademik = tahunAjaran.getSelectedItem() == null ? null
				: tahunAjaran.getSelectedItem().getValue().toString();
		Integer semester = (Integer) (this.semester.getSelectedItem() == null ? null
				: this.semester.getSelectedItem().getValue());
		String kelas = this.kelas.getValue().trim();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null
				|| this.fakultas.getSelectedItem().getValue() == null ? null
						: this.fakultas.getSelectedItem().getValue());
		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null
				|| this.jurusan.getSelectedItem().getValue() == null ? null
						: this.jurusan.getSelectedItem().getValue());
		String program = (String) (this.program.getSelectedItem() == null
				|| this.program.getSelectedItem().getValue() == null ? null
						: this.program.getSelectedItem().getValue());
		if (tahunAkademik == null || semester == null || fakultas == null || jurusan == null || program == null
				|| kelas.equals("")) {
			MyMessageboxConfig.show(
					"Fakultas" + ", Program Studi, Program, Tahun Akademik, Semester, dan Kelas harus dipilih",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();

		CalendarEvent ce = evt.getCalendarEvent();

		Perkuliahan perkuliahan = (Perkuliahan) HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.idEq(Long.parseLong(ce.getTitle()))).setMaxResults(1).uniqueResult();

		Fakultas userFakultas = tbmuser.ambilFakultas();
		jurusan = tbmuser.ambilJurusan();
		if (userFakultas != null && !userFakultas.getId().equals(perkuliahan.getJurusan().getFakultas().getId())) {
			MyMessageboxConfig.show(
					"Anda tidak boleh mengubah jadwal perkuliahan dari Fakultas "
							+ perkuliahan.getJurusan().getFakultas().getNama(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (jurusan != null && !jurusan.getId().equals(perkuliahan.getJurusan().getId())) {
			MyMessageboxConfig.show(
					"Anda tidak boleh mengubah jadwal perkuliahan dari Prodi " + perkuliahan.getJurusan().getNama(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return;
		}

		String ta = perkuliahan.getTahunAjaran();
		String sem = perkuliahan.getGanjilGenap();
		if (CommonPenjadwalan.apakahPenjadwalanTidakAktif(ta, sem, semesterPendek, perkuliahan)) {
			MyMessageboxConfig.show(
					"Penjadwalan tahun akademik \"" + ta + "\" semester \"" + sem + "\" tidak diaktifkan", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		init(perkuliahan);

	}

	/**
	 * Handler ZK saat pengguna men-drag/resize event kalender yang sudah ada (perubahan
	 * visual/interaktif SAJA, bukan simpan permanen — tidak menyentuh database): menyalin tanggal
	 * mulai/selesai baru dari event drag ke {@link SimpleCalendarEvent} lalu memperbarui
	 * {@link SimpleCalendarModel} agar tampilan kalender konsisten dengan posisi hasil drag.
	 */
	public void onEventUpdate$calendars(ForwardEvent event) {
		CalendarsEvent evt = (CalendarsEvent) event.getOrigin();
		// SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/d");
		// sdf1.setTimeZone(TimeZone.getDefault());
		// StringBuffer sb = new StringBuffer("Update... from ");
		// sb.append(sdf1.get().format(evt.getCalendarEvent().getBeginDate()));
		// sb.append(" to ");
		// sb.append(sdf1.get().format(evt.getBeginDate()));
		// popupLabel.setValue(sb.toString());
		// int left = evt.getX();
		// int top = evt.getY();
		// if (top + 100 > evt.getDesktopHeight())
		// top = evt.getDesktopHeight() - 100;
		// if (left + 330 > evt.getDesktopWidth())
		// left = evt.getDesktopWidth() - 330;
		// updateMsg.open(left, top);
		// timer.start();
		org.zkoss.calendar.Calendars cal = (org.zkoss.calendar.Calendars) evt.getTarget();
		SimpleCalendarModel m = (SimpleCalendarModel) cal.getModel();
		SimpleCalendarEvent sce = (SimpleCalendarEvent) evt.getCalendarEvent();
		sce.setBeginDate(evt.getBeginDate());
		sce.setEndDate(evt.getEndDate());
		m.update(sce);
	}

	/** Navigasi kalender mundur/maju satu halaman periode (hari/minggu/bulan sesuai mold aktif); {@code event.getData()=="arrow-left"} berarti mundur, selain itu maju. */
	public void onMoveDate(ForwardEvent event) {
		if ("arrow-left".equals(event.getData()))
			calendars.previousPage();
		else
			calendars.nextPage();

	}

	/** Melompat kalender ke tanggal hari ini (timezone default JVM). */
	public void onToday(ForwardEvent event) {
		calendars.setCurrentDate(Calendar.getInstance(TimeZone.getDefault()).getTime());

	}

	/** Menukar timezone kalender aktif ke timezone berikutnya dalam {@link Calendars#getTimeZones()} (siklus sederhana: hapus timezone pertama, tambahkan kembali dengan label yang sama). */
	@SuppressWarnings("rawtypes")
	public void onSwitchTimeZone(ForwardEvent event) {
		Map<?, ?> zone = calendars.getTimeZones();
		if (!zone.isEmpty()) {
			Map.Entry me = (Map.Entry) zone.entrySet().iterator().next();
			calendars.removeTimeZone((TimeZone) me.getKey());
			calendars.addTimeZone((String) me.getValue(), (TimeZone) me.getKey());
		}

	}

	/** Mengatur hari pertama minggu tampilan kalender dari label item {@link Listbox} yang dipilih pengguna. */
	public void onUpdateFirstDayOfWeek(ForwardEvent event) {
		Listbox listbox = (Listbox) event.getOrigin().getTarget();
		calendars.setFirstDayOfWeek(listbox.getSelectedItem().getLabel());

	}

	/**
	 * Mengganti mode tampilan kalender berdasarkan {@code event.getData()}: {@code "Day"} (1 hari),
	 * {@code "5 Days"}, {@code "Week"} (7 hari) memakai mold {@code "default"} dengan jumlah hari
	 * sesuai; nilai lain (mis. {@code "Month"}) memakai mold {@code "month"}.
	 */
	public void onUpdateView(ForwardEvent event) {
		String text = String.valueOf(event.getData());
		int days = "Day".equals(text) ? 1 : "5 Days".equals(text) ? 5 : "Week".equals(text) ? 7 : 0;

		if (days > 0) {
			calendars.setMold("default");
			calendars.setDays(days);
		} else
			calendars.setMold("month");

		// FDOW.setVisible("month".equals(calendars.getMold())
		// || calendars.getDays() == 7);
	}

	/**
	 * Implementasi {@link OnSearchDefaultListener}: dipanggil sebagai callback saat dialog detail
	 * jadwal yang dibuka {@link #init(Perkuliahan)} (via {@link PenjadwalanUtil}) ditutup dengan
	 * perubahan — cukup memicu {@link #onRefresh(Event)} agar kalender &amp; roster menampilkan data
	 * terbaru.
	 */
	@Override
	public void onSearchDefault(Event event) {
		onRefresh(event);
	}

	// =====================================================================================
	// PANEL "DAFTAR MAHASISWA YANG MENGIKUTI PERKULIAHAN" (sisi kanan layar penjadwalan)
	// =====================================================================================

	/**
	 * Penampung ringan konteks kelas yang sudah tervalidasi (tahun ajaran, program, prodi,
	 * semester, dan entitas kelas). Dipakai bersama oleh keempat tombol aksi panel agar tidak ada
	 * lagi penggandaan blok validasi yang identik.
	 */
	private static final class KonteksKelas {
		final String tahunAjaran;
		final String program;
		final Jurusan jurusan;
		final Integer semester;
		final Kelas kelas;

		KonteksKelas(String tahunAjaran, String program, Jurusan jurusan, Integer semester, Kelas kelas) {
			this.tahunAjaran = tahunAjaran;
			this.program = program;
			this.jurusan = jurusan;
			this.semester = semester;
			this.kelas = kelas;
		}
	}

	/**
	 * Membaca pilihan filter di bagian atas layar (tahun ajaran, program, prodi, semester, kelas),
	 * memvalidasi kelengkapannya, dan mengembalikan {@link KonteksKelas} yang siap pakai.
	 *
	 * <p>
	 * Bila ada isian yang belum dipilih, metode ini langsung menampilkan pesan yang jelas kepada
	 * pengguna dan mengembalikan {@code null} &mdash; pemanggil cukup berhenti bila hasilnya
	 * {@code null}. Sebelumnya blok validasi ini disalin-tempel di empat tempat berbeda; kini
	 * terpusat sehingga mudah dipelihara dan konsisten. Memakai
	 * {@link HibernateUtil#currentSession()} (ditutup otomatis).
	 * </p>
	 *
	 * @return konteks kelas yang lengkap, atau {@code null} bila ada isian yang belum dipilih.
	 */
	private KonteksKelas ambilKonteksKelasTervalidasi() {
		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		String prg = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
				: program.getSelectedItem().getValue());
		Jurusan jrs = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
				: jurusan.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());
		String namaKelas = kelas.getValue() == null ? "" : kelas.getValue().trim();
		Kelas kls = namaKelas.isEmpty() ? null
				: (Kelas) HibernateUtil.currentSession().createCriteria(Kelas.class)
						.add(Restrictions.eq("nama", namaKelas)).setMaxResults(1).uniqueResult();

		if (ta == null) {
			pesanWajibDiisi("Tahun Akademik");
			return null;
		}
		if (prg == null) {
			pesanWajibDiisi("Program");
			return null;
		}
		if (jrs == null) {
			pesanWajibDiisi(Common.getBahasaConfig("Jurusan"));
			return null;
		}
		if (smt == null) {
			pesanWajibDiisi("Semester");
			return null;
		}
		if (kls == null) {
			pesanWajibDiisi("Kelas");
			return null;
		}
		return new KonteksKelas(ta, prg, jrs, smt, kls);
	}

	/** Menampilkan pesan &ldquo;&lt;nama&gt; harus diisi&rdquo; secara seragam. */
	private void pesanWajibDiisi(String namaField) {
		try {
			MyMessageboxConfig.show(namaField + " harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Memeriksa apakah seorang mahasiswa sudah terlanjur mengambil {@code matakuliah} yang sama pada
	 * tahun ajaran &amp; semester berjalan di perkuliahan lain (yang sudah disetujui). Bila ada,
	 * pesan bentrok yang rinci ditambahkan ke {@code warnings} dan metode mengembalikan {@code false}
	 * sehingga sinkronisasi untuk mahasiswa tersebut dibatalkan.
	 *
	 * <p>
	 * Memakai {@link HibernateUtil#currentNativeSession()} untuk kueri baca lintas relasi; sesi
	 * <b>dijamin ditutup di blok {@code finally}</b> agar tidak bocor walau terjadi galat.
	 * </p>
	 *
	 * @param mahasiswa  mahasiswa yang diperiksa.
	 * @param matakuliah mata kuliah yang hendak ditambahkan.
	 * @param warnings   daftar pesan; diisi bila ditemukan bentrok.
	 * @return {@code true} bila aman ditambahkan, {@code false} bila bentrok.
	 */
	private boolean checkMahasiswaBentrok(Mahasiswa mahasiswa, Matakuliah matakuliah, List<String> warnings)
			throws Exception {
		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());

		Session session = HibernateUtil.currentNativeSession();
		try {
			Detailperkuliahan perkuliahanLain = (Detailperkuliahan) session.createCriteria(Detailperkuliahan.class)
					.add(Restrictions.isNull("ikutiPerkuliahan"))
					.add(Restrictions.eq("persetujuan", Detailperkuliahan.DISETUJUI))
					.add(Restrictions.eq("mahasiswa", mahasiswa)).createCriteria("perkuliahan", Criteria.LEFT_JOIN)
					.add(semesterPendek == null ? Restrictions.isNull("statusSemesterPendek")
							: Restrictions.isNotNull("statusSemesterPendek"))
					.add(Restrictions.eq("tahunAjaran", ta)).add(Restrictions.eq("semester", smt))
					.add(Restrictions.eq("matakuliah", matakuliah)).setMaxResults(1).uniqueResult();
			if (perkuliahanLain != null) {
				warnings.add("GAGAL : Mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama " + mahasiswa.getNama()
						+ " tidak bisa dimasukkan ke jadwal perkuliahan matakuliah \"" + matakuliah.toString()
						+ "\", karena mahasiswa tersebut sudah mengambil matakuliah "
						+ perkuliahanLain.getPerkuliahan().getMatakuliah().getNama() + ", tahun akademik "
						+ perkuliahanLain.getPerkuliahan().getTahunAjaran() + ", semester "
						+ perkuliahanLain.getSemester() + ", kelas " + perkuliahanLain.getPerkuliahan().getKelas()
						+ ", dosen "
						+ (perkuliahanLain.getPerkuliahan().getDosen1() == null ? ""
								: perkuliahanLain.getPerkuliahan().getDosen1().getNama())
						+ ", hari " + perkuliahanLain.getPerkuliahan().getHari() + ", jam "
						+ perkuliahanLain.getPerkuliahan().getWaktuMulai() + " s.d "
						+ perkuliahanLain.getPerkuliahan().getWaktuSelesai() + ".");
				return false;
			}
			return true;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	/**
	 * Membangun panel kanan &ldquo;Daftar mahasiswa yang mengikuti perkuliahan&rdquo;.
	 *
	 * <p>
	 * <b>Untuk apa panel ini.</b> Di sinilah petugas mengelola daftar mahasiswa sebuah kelas lalu
	 * membentuk KRS mereka secara massal: tambahkan mahasiswa lewat <i>Ambil data Mahasiswa</i>,
	 * lalu tekan <i>Singkronisasikan</i> agar setiap mahasiswa otomatis terdaftar pada seluruh
	 * perkuliahan kelas ini &mdash; mahasiswa tidak perlu mengisi KRS sendiri.
	 * </p>
	 *
	 * <p>
	 * Susunan: sebuah penjelasan singkat, toolbar aksi (Ambil / Singkronisasi / Batalkan / Bersihkan),
	 * toolbar pencarian (NIM / Nama / Angkatan), dan tabel mahasiswa beserta status pembayaran.
	 * Seluruh tombol memakai satu jalur validasi konteks ({@link #ambilKonteksKelasTervalidasi()}).
	 * </p>
	 */
	public void initDataMahasiswa() {

		// Panel kanan dibungkus MyPortallayout -> MyPortalchildren -> Panel (permintaan user) agar
		// menjadi KARTU SOLID yang mengisi penuh area sampai bawah. Tanpa ini, wadah lama (MyDiv)
		// tampil transparan sehingga kalender di belakangnya menembus/tumpang-tindih dengan isi panel.
		// Region East diberi latar & autoscroll supaya buram (opaque) dan dapat digulir bila konten
		// lebih tinggi dari layar.
		try {
			panelDaftarMahasiswa.setStyle("background:#f1f5f9;box-sizing:border-box;");
			panelDaftarMahasiswa.setAutoscroll(true);
		} catch (Exception eStyle) {
			ais.common.ErrorAuditUtil.record(eStyle,
					"auto-audit(empty-catch) src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:initDataMahasiswa-eastStyle");
		}

		ais.ui.util.MyPortallayout portal = new ais.ui.util.MyPortallayout();
		portal.setStyle("width:100%;box-sizing:border-box;padding:6px;background:#f1f5f9;min-height:100%;");
		portal.setParent(panelDaftarMahasiswa);

		ais.ui.util.MyPortalchildren kolomPortal = new ais.ui.util.MyPortalchildren();
		kolomPortal.setWidth("100%");
		kolomPortal.setParent(portal);

		org.zkoss.zul.Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(kolomPortal);
		panel.setTitle("Daftar Mahasiswa Kelas Ini");
		panel.setBorder("none");
		panel.setStyle("border:1px solid #e6edf5;border-radius:14px;background:#ffffff;"
				+ "box-shadow:0 8px 22px rgba(15,23,42,0.06);overflow:hidden;");

		org.zkoss.zul.Panelchildren panelchildren = new org.zkoss.zul.Panelchildren();
		panelchildren.setParent(panel);
		panelchildren.setStyle("padding:10px;background:#ffffff;box-sizing:border-box;");

		// Wadah isi (banner + toolbar + grid) tetap bernama 'groupbox' agar seluruh setParent di bawah
		// tidak perlu diubah — kini bersarang rapi di dalam kartu Panel yang opaque.
		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height:800px;box-sizing:border-box;background:#ffffff;");
		groupbox.setParent(panelchildren);

		// Penjelasan singkat dengan bahasa awam (bukan istilah teknis).
		ais.ui.util.MyHtml penjelasan = new ais.ui.util.MyHtml(
				"<div style='font-size:12px;color:#334155;line-height:1.5;padding:4px 2px 8px;'>"
						+ "<span style='font-weight:800;color:#0f172a;'>Daftar mahasiswa kelas ini.</span> "
						+ "Tambahkan mahasiswa lewat <b>Ambil data Mahasiswa</b>, lalu tekan "
						+ "<b>Singkronisasikan</b> agar KRS tiap mahasiswa terbentuk otomatis untuk seluruh "
						+ "perkuliahan di kelas ini &mdash; mahasiswa tidak perlu mengambil sendiri.</div>");
		penjelasan.setParent(groupbox);

		// ---- Toolbar aksi (responsif: melipat di layar sempit) ----
		Toolbar toolbar = new Toolbar();
		toolbar.setStyle("display:flex;flex-wrap:wrap;gap:6px;padding:4px 2px;background:transparent;border:0;");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil data Mahasiswa", "/img/new.gif");
		button.setTooltiptext("Pilih mahasiswa yang akan dimasukkan ke kelas ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper dataMahasiswaHelper = new AmbilDataMahasiswaForManajemenPenjadwalanMahasiswaHelper(
						ctx.tahunAjaran, ctx.program, ctx.jurusan, ctx.semester, ctx.kelas);
				dataMahasiswaHelper.display(new DataLoader() {
					@Override
					public void loadData(Object value) {
						loadDataMahasiswa(value);
					}
				});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Singkronisasikan", "/img/process-accept-icon-kecil.png");
		button.setTooltiptext("Bentuk KRS otomatis: daftarkan semua mahasiswa di daftar ini ke perkuliahan kelas ini");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				final Tbmuser tbmuser = Common.getCurrentUser();
				MyMessageboxConfig.show(
						"Apakah yakin ingin meng-singkronisasikan data mahasiswa dengan jadwal perkuliahan ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								Common.createDefaultTimer(new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										try {
											List<PenjadwalanMahasiswa> mahasiswas = initCriteria(false).list();
											StringBuilder warnings = new StringBuilder();

											for (PenjadwalanMahasiswa penjadwalanMahasiswa : mahasiswas) {
												Integer smt = penjadwalanMahasiswa.getSemester();
												Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();

												Integer jumlah = KrsUtilHelper.hitungSksYangTelahDiambil(null, mahasiswa,
														null, smt, semesterPendek);

												if (Common.checkPembatasanSKSBerdasarkanIP(mahasiswa, smt, jumlah,
														semesterPendek)) {
													continue;
												}

												if (!Common.checkStatusPembayaranMahasiswa(smt, 0, mahasiswa, false,
														false)) {
													warnings.append("GAGAL : NIM ").append(mahasiswa.getNim())
															.append(" dan nama ").append(mahasiswa.getNama())
															.append(" belum melakukan pembayaran di smt ").append(smt)
															.append("\n\n");
													continue;
												}

												for (Long perkuliahanid : perkuliahans) {
													Perkuliahan perkuliahan = (Perkuliahan) ConstantValues
															.ambil(Perkuliahan.class.getName(), perkuliahanid);
													if (perkuliahan == null) {
														continue;
													}
													List<String> myWarining = new ArrayList<String>();
													if (checkMahasiswaBentrok(mahasiswa, perkuliahan.getMatakuliah(),
															myWarining)) {
														Session session = HibernateUtil.currentNativeSession();
														try {
															Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
																	.createCriteria(Detailperkuliahan.class)
																	.add(Restrictions.eq("mahasiswa", mahasiswa))
																	.add(Restrictions.eq("perkuliahan", perkuliahan))
																	.add(Restrictions.eq("semester",
																			penjadwalanMahasiswa.getSemester()))
																	.setMaxResults(1).uniqueResult();

															boolean dataBaru = detailperkuliahan == null;
															if (dataBaru) {
																detailperkuliahan = new Detailperkuliahan(tbmuser,
																		ManajemenPenjadwalanMahasiswaComposer.class);
															}

															detailperkuliahan.setPerkuliahan(perkuliahan);
															detailperkuliahan.setMahasiswa(mahasiswa);
															detailperkuliahan
																	.setSemester(penjadwalanMahasiswa.getSemester());
															detailperkuliahan.setPersetujuan(Detailperkuliahan.DISETUJUI);

															session.getTransaction().begin();
															if (dataBaru) {
																if (!KrsUtilHelper.simpanKrsJikaBelumAda(session, detailperkuliahan)) {
																	session.getTransaction().commit();
																	warnings.append("DILEWATI : NIM ").append(mahasiswa.getNim())
																			.append(" matakuliah ").append(perkuliahan.getMatakuliah())
																			.append(" sudah ada di KRS dan tidak ditambahkan lagi\n\n");
																	continue;
																}
															} else {
																session.update(detailperkuliahan);
															}
															session.getTransaction().commit();

															warnings.append("BERHASIL : NIM ").append(mahasiswa.getNim())
																	.append(" dan nama ").append(mahasiswa.getNama())
																	.append(" matakuliah ")
																	.append(perkuliahan.getMatakuliah()).append("\n\n");
														} catch (Exception e) {
															try {
																session.getTransaction().rollback();
															} catch (Exception er) {
																ais.common.ErrorAuditUtil.record(er,
																		"rollback-gagal src/ais/action/master/helper/ManajemenPenjadwalanMahasiswaComposer.java:singkron");
															}
															warnings.append("GAGAL : NIM ").append(mahasiswa.getNim())
																	.append(" dan nama ").append(mahasiswa.getNama())
																	.append(" matakuliah ")
																	.append(perkuliahan.getMatakuliah())
																	.append(". Error : ").append(e.getMessage())
																	.append("\n\n");
															Common.tampilErrorJikaAdmin(e);
														} finally {
															HibernateUtil.closeSession();
														}
													}

													if (!myWarining.isEmpty()) {
														for (String string : myWarining) {
															warnings.append(string).append("\n\n");
														}
													}
												}
											}

											if (warnings.length() == 0) {
												MyMessageboxConfig.show(
														"Singkronisasi mahasiswa dengan jadwal perkuliahan berhasil dilakukan",
														"Pemberitahuan", MyMessageboxConfig.OK,
														MyMessageboxConfig.INFORMATION);
											} else {
												MyMessageboxConfig.show(warnings.toString(), "Pemberitahuan",
														MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
											}

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("sinkronisasi mahasiswa dengan jadwal perkuliahan",
													e,
													new String[] {
															"Periksa kembali apakah data jadwal perkuliahan (Kelas, Matakuliah, Dosen) yang disinkronkan sudah lengkap.",
															"Pastikan tidak ada mahasiswa dengan data KRS yang sedang diubah bersamaan oleh pengguna lain saat proses ini berjalan.",
															"Coba ulangi proses sinkronisasi beberapa saat lagi.",
															"Bila kegagalan berulang, laporkan ke Administrator/pengembang disertai tangkapan layar (screenshot) pesan ini."
													});
										}
									}
								}, "Sedang melakukan singkronisasi.. harap menunggu..");
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Batalkan Singkronisasi", "/img/svg/trash.svg");
		button.setTooltiptext("Hapus kembali KRS yang belum dinilai untuk mahasiswa di daftar ini");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				MyMessageboxConfig.show(
						"Apakah yakin ingin menghapus kembali singkronisasi jadwal mahasiswa yang ada di daftar ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								try {
									List<PenjadwalanMahasiswa> mahasiswas = initCriteria(false).list();
									Session session = HibernateUtil.currentSession();
									for (Long perkuliahanid : perkuliahans) {
										Perkuliahan perkuliahan = (Perkuliahan) ConstantValues
												.ambil(Perkuliahan.class.getName(), perkuliahanid);
										if (perkuliahan == null) {
											continue;
										}
										for (PenjadwalanMahasiswa penjadwalanMahasiswa : mahasiswas) {
											Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();
											Detailperkuliahan detailperkuliahan = (Detailperkuliahan) session
													.createCriteria(Detailperkuliahan.class)
													.add(Restrictions.le("totalNilai", 0.1))
													.add(Restrictions.eq("mahasiswa", mahasiswa))
													.add(Restrictions.eq("perkuliahan", perkuliahan))
													.add(Restrictions.eq("semester", penjadwalanMahasiswa.getSemester()))
													.uniqueResult();
											if (detailperkuliahan != null) {
												session.createSQLQuery("delete from nilai where detailperkuliahan = "
														+ detailperkuliahan.getId() + ";").executeUpdate();
												session.delete(detailperkuliahan);
											}
										}
									}

									MyMessageboxConfig.show(
											"Singkronisasi mahasiswa dengan jadwal perkuliahan berhasil di-hapus",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan Daftar", "/img/svg/trash.svg");
		button.setTooltiptext("Kosongkan seluruh daftar mahasiswa pada kelas & semester ini");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final KonteksKelas ctx = ambilKonteksKelasTervalidasi();
				if (ctx == null) {
					return;
				}
				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data mahasiswa yang ada di daftar ini ?",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i != MyMessageboxConfig.OK) {
									return;
								}
								try {
									Session session = HibernateUtil.currentSession();
									session.createSQLQuery("delete from penjadwalan_mahasiswa where kelas = "
											+ ctx.kelas.getId() + " and tahunajaran = '" + ctx.tahunAjaran
											+ "' and semester = " + ctx.semester).executeUpdate();

									MyMessageboxConfig.show(
											"Data mahasiswa di paket jadwal perkuliahan ini berhasil di-hapus",
											"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									loadDataMahasiswa(null);

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
									PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
								}
							}
						});
			}
		});
		button.setParent(toolbar);

		// ---- Toolbar pencarian ----
		Toolbar toolbarCari = new Toolbar();
		toolbarCari.setStyle("display:flex;flex-wrap:wrap;gap:6px;align-items:center;padding:2px;background:transparent;border:0;");
		toolbarCari.setParent(groupbox);

		final EventListener pemicuCari = new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				loadDataMahasiswa(null);
			}
		};

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("NIM : ")));
		toolbarCari.appendChild(nim = new Textbox());
		nim.setCols(4);
		nim.addEventListener(Events.ON_OK, pemicuCari);

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbarCari.appendChild(nama = new Textbox());
		nama.setCols(4);
		nama.addEventListener(Events.ON_OK, pemicuCari);

		toolbarCari.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbarCari.appendChild(angkatan = new Intbox());
		angkatan.setCols(2);
		angkatan.addEventListener(Events.ON_OK, pemicuCari);

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		cari.setTooltiptext("Cari mahasiswa di daftar ini");
		cari.addEventListener("onClick", pemicuCari);
		cari.setParent(toolbarCari);

		// ---- Tabel mahasiswa ----
		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Fakultas");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pembayaran");
		column.setWidth("18%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		paging.setParent(groupbox);
	}

	/**
	 * Menyusun kriteria pencarian daftar mahasiswa terjadwal pada kelas berjalan (dipakai bersama
	 * oleh pencarian, penomoran halaman, dan proses sinkronisasi/pembatalan). Memakai
	 * {@link HibernateUtil#currentSession()} (ditutup otomatis).
	 *
	 * @param order true untuk mengurutkan berdasarkan NIM menaik.
	 */
	public Criteria initCriteria(boolean order) {

		String ta = (String) (tahunAjaran.getSelectedItem() == null ? null : tahunAjaran.getSelectedItem().getValue());
		String prg = (String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null ? null
				: program.getSelectedItem().getValue());
		Jurusan jrs = (Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
				: jurusan.getSelectedItem().getValue());
		Integer smt = (Integer) (semester.getSelectedItem() == null ? null : semester.getSelectedItem().getValue());
		String namaKelas = kelas.getValue() == null ? "" : kelas.getValue().trim();
		Kelas kls = (Kelas) HibernateUtil.currentSession().createCriteria(Kelas.class)
				.add(Restrictions.eq("nama", namaKelas)).setMaxResults(1).uniqueResult();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PenjadwalanMahasiswa.class).createAlias("mahasiswa", "mahasiswa")
				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))
				.add(Restrictions.eq("tahunAjaran", ta))
				.add(Restrictions.eq("semester", smt))
				.add(Restrictions.ilike("mahasiswa.nim", teksAman(nim), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nama", teksAman(nama), MatchMode.ANYWHERE))
				.add(Restrictions.eq("kelas", kls));
		if (jrs != null) {
			criteria.add(Restrictions.eq("mahasiswa.jurusan", jrs));
		}
		if (prg != null) {
			criteria.add(Restrictions.eq("mahasiswa.program", prg));
		}

		if (order) {
			criteria.addOrder(Order.asc("mahasiswa.nim"));
		}

		return criteria;
	}

	/** Nilai teks kotak isian yang aman dari null dan sudah di-trim. */
	private static String teksAman(Textbox t) {
		return t == null || t.getValue() == null ? "" : t.getValue().trim();
	}

	/**
	 * Memuat ulang tabel mahasiswa terjadwal untuk halaman berjalan. Sebelumnya metode ini
	 * menjalankan satu kueri {@code list()} yang hasilnya dibuang percuma sebelum kueri sebenarnya;
	 * kueri sia-sia itu kini dihapus demi efisiensi. Memakai {@link HibernateUtil#currentSession()}
	 * (ditutup otomatis).
	 */
	@SuppressWarnings("unchecked")
	public void loadDataMahasiswa(Object value) {
		if (!filterMinimumDaftarMahasiswaTerisi()) {
			grid.setRowRenderer(new DetailKelasRenderer());
			grid.setModelCheckMobile(new SimpleListModel(new ArrayList<PenjadwalanMahasiswa>()));
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<PenjadwalanMahasiswa> mahasiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(mahasiswas);
		grid.setRowRenderer(new DetailKelasRenderer());
		grid.setModelCheckMobile(strset);
	}

	/**
	 * Pengecekan cepat sebelum {@link #loadDataMahasiswa(Object)} melakukan query berat: tahun
	 * ajaran dan semester harus terpilih, nama kelas tidak boleh kosong, dan nama kelas tersebut
	 * harus benar-benar ada sebagai baris {@link Kelas} di database.
	 *
	 * @return {@code true} bila semua syarat minimum terpenuhi
	 */
	private boolean filterMinimumDaftarMahasiswaTerisi() {
		if (tahunAjaran.getSelectedItem() == null || tahunAjaran.getSelectedItem().getValue() == null) {
			return false;
		}
		if (semester.getSelectedItem() == null || semester.getSelectedItem().getValue() == null) {
			return false;
		}
		String namaKelas = kelas == null || kelas.getValue() == null ? "" : kelas.getValue().trim();
		if (namaKelas.length() == 0) {
			return false;
		}
		return HibernateUtil.currentSession().createCriteria(Kelas.class).add(Restrictions.eq("nama", namaKelas))
				.setMaxResults(1).uniqueResult() != null;
	}

	/**
	 * Penggambar baris tabel mahasiswa terjadwal: NIM (via tombol revisi), Nama, Angkatan, Fakultas,
	 * Prodi, Program, status Pembayaran, dan tombol Hapus (bila pengguna berhak menghapus). Seluruh
	 * pembacaan relasi dijaga terhadap {@code null} agar satu data tak lengkap tidak menggagalkan
	 * seluruh tabel.
	 */
	class DetailKelasRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;

		public DetailKelasRenderer() {
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {
			row.setValign("top");
			final PenjadwalanMahasiswa penjadwalanMahasiswa = (PenjadwalanMahasiswa) data;
			Mahasiswa mahasiswa = penjadwalanMahasiswa.getMahasiswa();

			RevisiHelper.createNewRevisi(PenjadwalanMahasiswa.class, penjadwalanMahasiswa, mahasiswa.getNim())
					.setParent(row);

			new Label(mahasiswa.getNama()).setParent(row);
			new Label(String.valueOf(mahasiswa.getTahunangkatan())).setParent(row);

			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(row);

			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(row);

			new Label(mahasiswa.getProgram() == null ? "" : mahasiswa.getProgram()).setParent(row);

			JenisKegiatan jenisKegiatan = CommonPMB.pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			Kegiatan kegiatan = mahasiswa.ambilKegiatansRefresh(penjadwalanMahasiswa.getSemester(), jenisKegiatan);
			new Label(kegiatan == null ? "Belum bayar" : kegiatan.toString()).setParent(row);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setVisible(delete);
			button.setTooltiptext("Hapus Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus mahasiswa penjadwalan ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i != MyMessageboxConfig.OK) {
										return;
									}
									try {
										Common.refreshDelete(penjadwalanMahasiswa);
										loadDataMahasiswa(null);
									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
									}
								}
							});
				}
			});
			button.setParent(toolbar);
			toolbar.setParent(row);
		}
	}

}
