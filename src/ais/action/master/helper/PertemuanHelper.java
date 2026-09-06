package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.North;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.dashboard.admin.DashboardTimelinePertemuan;
import ais.action.master.sekolah.helper.AbsensiSiswaHelper;
import ais.action.master.sekolah.helper.PertemuanPunyaUjianSiswaHelper;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.ui.util.HtmlChartHelper;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK yang membangun window detail satu {@link Pertemuan} (satu sesi kelas/pertemuan
 * perkuliahan, jadwal pelajaran sekolah, kegiatan KKN, atau kegiatan PKL) dan menampilkannya
 * sebagai modal beranak-tab. Dipanggil dari banyak titik akademik (dasbor dosen/guru, riwayat
 * perkuliahan, KRS, dsb.) setiap kali pengguna perlu membuka rincian satu pertemuan.
 *
 * <p><b>Tab yang dibangun {@link #tampilanDesktop(org.zkoss.zk.ui.Component)} (lazy-load lewat
 * {@code MyButtonTabbox}):</b></p>
 * <ol>
 * <li><b>Dasbor</b> (indeks 9, didaftarkan pertama agar tampil paling kiri) — ringkasan KPI
 * (total peserta, hadir, belum absen, jumlah tugas/ujian/konten), grafik batang bertumpuk
 * pengumpulan tugas &amp; partisipasi ujian, radar kelengkapan pertemuan, dan baris navigasi
 * yang tiap barisnya bisa diklik untuk lompat ke tab terkait. Dibangun oleh
 * {@link #initDasbor(Div)}.</li>
 * <li><b>Kehadiran</b> (0) — didelegasikan ke {@link AbsensiSiswaHelper} bila pertemuan berasal
 * dari {@code JadwalPelajaran}/{@code JadwalUjianPSB}/formulir kegiatan sekolah, atau ke
 * {@link AbsensiHelper} untuk kasus akademik biasa (perkuliahan, KKN, PKL).</li>
 * <li><b>Pembelajaran</b> (1) — form catatan/RPS pertemuan ({@code catatan}, {@code indikator},
 * {@code metodePembelajaran}, {@code pengalamanBelajar}, {@code waktupembelajaran},
 * {@code tugasDanPenilaian}) dibangun oleh {@link #initCatatan(boolean)}; setiap field punya
 * tombol "Generate ..." yang memanggil {@link AIGenerator#generateApa} untuk mengisi otomatis
 * berdasarkan konteks pertemuan (dosen, mahasiswa, matakuliah/matapelajaran, jadwal).</li>
 * <li><b>Materi</b> (2) — file konten pertemuan lewat {@code filePerkuliahanHelper}
 * ({@link FilePerkuliahanHelper}).</li>
 * <li><b>Tugas</b> (3) — daftar {@link Tugas} (individu {@link TugasPertemuan} lewat
 * {@code tugasMandiriHelper}, kelompok {@link TugasKelompok} lewat {@code tugasKelompokHelper})
 * ditampilkan sebagai sub-tab, dibangun oleh {@link #initTugas(Component, boolean, boolean)}.</li>
 * <li><b>Audio</b> (4) / <b>Video</b> (5) — lewat {@code audioPertemuanHelper}/
 * {@code videoPertemuanHelper}.</li>
 * <li><b>Ujian</b> (6) — didelegasikan ke {@code pertemuanPunyaUjianSiswaHelper} (jadwal sekolah)
 * atau {@code pertemuanPunyaUjianHelper} (akademik) — pola percabangan yang sama seperti tab
 * Kehadiran.</li>
 * <li><b>Diskusi</b> (7) — lewat {@code pertemuanPunyaDiskusiHelper}.</li>
 * <li><b>Hasil, Evaluasi, Kusioner</b> (8) — lewat {@code pertemuanPunyaHasilHelper}.</li>
 * </ol>
 *
 * <p><b>Mode tampilan:</b> {@link #init()} memilih antara {@link #tampilanDesktop} (tab button
 * penuh) dan {@link #tampilanMobile(Center)} (satu tab aktif berdasarkan {@code index}, tanpa
 * dasbor) berdasarkan {@code Common.isMobile()}.</p>
 *
 * <p><b>Konstruksi &amp; identitas pengguna:</b> keempat constructor menentukan "siapa yang
 * melihat" (mahasiswa, calon mahasiswa/biodataCalonMahasiswa, siswa/calon siswa lewat
 * {@link Tbmuser}, atau admin/dosen/guru bila semua null) dan meneruskannya ke sub-helper
 * masing-masing tab supaya query dan hak edit dibatasi sesuai identitas tersebut. Perhatikan
 * dua kuirk: (1) {@link #PertemuanHelper(Mahasiswa, BiodataCalonMahasiswa)} MENIMPA parameter
 * {@code mahasiswa}/{@code biodataCalonMahasiswa} yang diteruskan pemanggil dengan milik
 * {@code Common.getCurrentUser()} bila user sesi punya mahasiswa/biodataCalonMahasiswa sendiri —
 * jadi parameter itu hanya dipakai sebagai fallback ketika sesi login tidak mewakili siapa pun;
 * (2) {@link #PertemuanHelper(Mahasiswa, BiodataCalonMahasiswa, boolean)} memanggil constructor
 * 2-parameter TERLEBIH DAHULU (yang berpotensi menimpa mahasiswa/biodataCalonMahasiswa seperti
 * di atas) baru kemudian menimpa {@code tampilSelesai}.</p>
 *
 * <p><b>Efek samping:</b> perubahan pada field catatan/indikator/metode pembelajaran/pengalaman
 * belajar/waktu pembelajaran/tugas dan penilaian langsung melakukan
 * {@code Common.refreshSaveOrUpdate(pertemuan)} pada {@code onChange} (autosave per field, tanpa
 * tombol simpan terpisah). Membuat tugas individu/kelompok baru ({@code initTugas}) melakukan
 * {@code session.save()+flush()} langsung lalu memuat ulang koleksi tugas pertemuan. Window
 * ditutup lewat tombol "Selesai" yang memanggil {@code dataLoader.loadData(null)} (memberi tahu
 * pemanggil untuk refresh) sebelum {@code window.detach()}.</p>
 *
 * <p><b>Titik masuk:</b> lima overload {@link #display} — parameter tambahan (tugas/tugas
 * kelompok terpilih, file/audio/video terpilih) hanya dipakai untuk mem-preselect item saat
 * window dibuka (mis. dari notifikasi atau tautan langsung ke satu tugas/file tertentu). Semua
 * overload berujung ke overload penuh yang membungkus pembukaan window dalam
 * {@code Common.createDefaultTimer(...)} agar berjalan di siklus event ZK yang benar.</p>
 */
public class PertemuanHelper {

	/**
	 * Callback yang dipanggil dengan argumen {@code null} saat window ditutup lewat tombol
	 * "Selesai" ({@link #init()}), sebagai sinyal bagi pemanggil untuk memuat ulang datanya
	 * (mis. grid daftar pertemuan). Diisi di dalam timer {@link #display(Pertemuan, DataLoader,
	 * int, TugasPertemuan, TugasKelompok, PertemuanFileContent, AudioPertemuan, VideoPertemuan)},
	 * jadi bernilai {@code null} sampai {@code display} benar-benar dijalankan.
	 */
	private DataLoader dataLoader;
	/**
	 * Modal ZK pembungkus seluruh tab pertemuan. Dibuat sekali (99% x 99%, ditempel ke root
	 * page saat itu) pada pemanggilan {@code display} pertama, lalu DIPAKAI ULANG pada
	 * pemanggilan berikutnya — isinya dibersihkan {@code Common.clear(window)} bukan
	 * di-{@code detach}. Publik agar pemanggil bisa mengatur ukuran/judul sendiri sebelum
	 * atau sesudah {@code display}.
	 */
	public MyWindow window = null;
	/**
	 * Menentukan apakah baris South berisi tombol "Selesai" ditampilkan
	 * ({@code south.setVisible(tampilSelesai)} di {@link #init()}). Disetel {@code false} oleh
	 * pemanggil yang menyematkan helper ini di dalam layar lain sehingga tombol tutup sendiri
	 * tidak relevan. Lihat {@link #PertemuanHelper(Mahasiswa, BiodataCalonMahasiswa, boolean)}.
	 */
	public boolean tampilSelesai = true;
	/**
	 * Entity pertemuan yang sedang ditampilkan — sumber data tunggal seluruh tab. Diisi di
	 * dalam timer {@code display}, jadi masih {@code null} sesudah constructor. Semua sub-helper
	 * tab menerima objek yang SAMA ini, sehingga perubahan field catatan/RPS oleh tab
	 * Pembelajaran langsung terlihat oleh tab lain tanpa reload.
	 */
	private Pertemuan pertemuan;

	/**
	 * Sub-helper tab Video (indeks 5): merender daftar {@link VideoPertemuan} milik pertemuan
	 * ini. Dibangun di constructor dengan {@code tbmuser} null-safe.
	 */
	private VideoPertemuanHelper videoPertemuanHelper;
	/**
	 * Sub-helper tab Audio (indeks 4): merender daftar {@link AudioPertemuan} milik pertemuan
	 * ini. Dibangun di constructor dengan {@code tbmuser} null-safe.
	 */
	private AudioPertemuanHelper audioPertemuanHelper;
	/**
	 * Sub-helper tab Materi (indeks 2): merender daftar {@link PertemuanFileContent} (file
	 * materi/konten) milik pertemuan ini.
	 */
	private FilePerkuliahanHelper filePerkuliahanHelper;

	/**
	 * Sub-helper tab Ujian (indeks 6) jalur AKADEMIK ({@link PertemuanPunyaUjian} pada
	 * perkuliahan/KKN/PKL). Dipilih ketika {@code pertemuan.getJadwalPelajaran()} DAN
	 * {@code getJadwalUjianPSB()} keduanya {@code null}.
	 */
	private PertemuanPunyaUjianHelper pertemuanPunyaUjianHelper;
	/**
	 * Sub-helper tab Ujian (indeks 6) jalur SEKOLAH. Dipilih ketika pertemuan berasal dari
	 * {@code JadwalPelajaran} atau {@code JadwalUjianPSB}. Dibangun di constructor dengan
	 * {@code tbmuser.getSiswa()} — TIDAK null-safe terhadap {@code tbmuser == null}.
	 */
	private PertemuanPunyaUjianSiswaHelper pertemuanPunyaUjianSiswaHelper;
	/**
	 * Sub-helper tab Diskusi (indeks 7): forum diskusi pertemuan. Menerima
	 * {@link #selectedDiskusi} untuk langsung membuka satu utas tertentu (mis. dari notifikasi).
	 */
	private PertemuanPunyaDiskusiHelper pertemuanPunyaDiskusiHelper;
	/**
	 * Sub-helper tab "Hasil, Evaluasi, Kusioner" (indeks 8). Tidak punya padanan di mode mobile.
	 */
	private PertemuanPunyaHasilHelper pertemuanPunyaHasilHelper;
	/**
	 * Sub-helper tugas INDIVIDU ({@link TugasPertemuan}) untuk sub-tab tugas utama pertemuan.
	 * Perhatikan bahwa {@link #initTugas(Component, boolean, boolean)} juga membuat instance
	 * {@code TugasMandiriHelper} LOKAL baru untuk tiap sub-tab tugas lainnya; field ini hanya
	 * dipakai untuk tugas bawaan pertemuan ({@code pertemuan.getJudultugas()}).
	 */
	private TugasMandiriHelper tugasMandiriHelper;
	/**
	 * Sub-helper tab Kehadiran (indeks 0) jalur AKADEMIK (perkuliahan, KKN, PKL). Dipilih
	 * ketika pertemuan bukan berasal dari jadwal pelajaran/ujian PSB/formulir kegiatan sekolah.
	 */
	private AbsensiHelper absensiHelper;
	/**
	 * Sub-helper tab Kehadiran (indeks 0) jalur SEKOLAH. Dipilih ketika
	 * {@code pertemuan.getJadwalPelajaran() != null}, {@code getJadwalUjianPSB() != null}, atau
	 * {@code getFormulirKegiatan().getSekolah() != null}. Dibangun di constructor dengan
	 * {@code tbmuser.getSiswa()} — TIDAK null-safe terhadap {@code tbmuser == null}.
	 */
	private AbsensiSiswaHelper absensiSiswaHelper;

	/**
	 * Mahasiswa "pemilik sudut pandang" bila window dibuka dari sisi mahasiswa. Bernilai
	 * {@code null} untuk admin/dosen/guru. Dipakai sebagai gerbang tampilan di banyak tempat:
	 * tab "Tugas Individu Baru"/"Tugas Kelompok Baru" hanya di-{@code setVisible} bila field ini
	 * (dan {@link #biodataCalonMahasiswa}, {@code tbmuser.getPesertaKursus()},
	 * {@code tbmuser.getSiswa()}) semuanya {@code null}; {@link #initCatatan(boolean)} memakai
	 * kondisi yang sama untuk memilih tata letak editable vs read-only.
	 *
	 * <p><b>Catatan arsitektur:</b> pada constructor 2-argumen nilai parameter dapat DITIMPA oleh
	 * {@code Common.getCurrentUser().getMahasiswa()} — jadi nilai yang diteruskan pemanggil hanya
	 * berlaku sebagai fallback ketika sesi login tidak mewakili seorang mahasiswa.</p>
	 */
	private Mahasiswa mahasiswa;

	/**
	 * Referensi ke {@link Textbox} catatan pertemuan pada tab Pembelajaran, disimpan sebagai
	 * field karena dibutuhkan oleh listener autosave {@code onChange} dan oleh callback hasil
	 * {@link AIGenerator#generateApa} ("Generate Catatan") yang menuliskan hasil AI ke dalamnya.
	 * Bernilai {@code null} bila tab Pembelajaran belum pernah dibangun, atau bila tampilan
	 * read-only ({@code MyHtmlIframe}) yang dipakai untuk calon mahasiswa.
	 */
	private Textbox catatan;
	/**
	 * Indeks tab yang aktif: 0=Kehadiran, 1=Pembelajaran, 2=Materi, 3=Tugas, 4=Audio, 5=Video,
	 * 6=Ujian, 7=Diskusi, 8=Hasil/Evaluasi/Kusioner, 9=Dasbor. Diisi dari parameter
	 * {@code index} milik {@code display} DI DALAM timer, lalu dipakai
	 * {@link #tampilanDesktop(Component)} ({@code btnTab.pilih(index)}) atau
	 * {@link #tampilanMobile(Center)} (memilih satu-satunya konten yang dirender).
	 */
	private int index = 0;
	/**
	 * Calon mahasiswa "pemilik sudut pandang" bila window dibuka dari portal PMB. Bernilai
	 * {@code null} untuk peran lain. Selain menjadi gerbang tampilan bersama
	 * {@link #mahasiswa}, field ini secara khusus membuat catatan pertemuan dirender READ-ONLY
	 * lewat {@code MyHtmlIframe} alih-alih {@link Textbox} yang bisa diedit
	 * ({@link #initCatatan(boolean)}). Bisa DITIMPA oleh sesi login — lihat {@link #mahasiswa}.
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/**
	 * Tabbox tombol lazy-load milik mode desktop. Disimpan sebagai field karena seluruh elemen
	 * navigasi Dasbor (kartu KPI, groupbox, baris tabel) memanggil {@code btnTab.pilih(tabIdx)}
	 * dari dalam listener {@code onClick}-nya. Tetap {@code null} pada mode mobile — jadi jangan
	 * dipakai di jalur {@link #tampilanMobile(Center)}.
	 */
	private ais.ui.util.MyButtonTabbox btnTab;
	/**
	 * Tugas individu yang harus otomatis terpilih saat tab Tugas dibangun (mis. saat window
	 * dibuka langsung dari notifikasi tugas). Juga dipakai sebagai state internal: listener
	 * "Tugas Individu Baru" menyetel field ini ke tugas yang baru dibuat lalu memanggil ulang
	 * {@link #initTugas(Component, boolean)} agar tab tugas baru langsung aktif.
	 */
	private TugasPertemuan selectedTugasPertemuan = null;
	/**
	 * Padanan {@link #selectedTugasPertemuan} untuk tugas KELOMPOK. Kedua field ini saling
	 * meniadakan: menyetel salah satu selalu disertai me-{@code null}-kan yang lain.
	 */
	private TugasKelompok selectedTugasKelompok = null;

	/**
	 * ID utas diskusi yang otomatis dibuka di tab Diskusi. Publik dan disetel LANGSUNG oleh
	 * pemanggil (mis. {@code MobileNotifHelper}) sebelum memanggil {@code display}, karena tidak
	 * ada overload {@code display} yang menerimanya sebagai parameter.
	 */
	public Long selectedDiskusi = null;
	/**
	 * Materi yang otomatis disorot di tab Materi; diisi lewat overload
	 * {@link #display(Pertemuan, DataLoader, int, PertemuanFileContent)}.
	 */
	private PertemuanFileContent selectedPertemuanFileContent = null;
	/**
	 * Audio yang otomatis disorot di tab Audio; diisi lewat overload
	 * {@link #display(Pertemuan, DataLoader, int, AudioPertemuan)}.
	 */
	private AudioPertemuan selectedAudioPertemuan = null;
	/**
	 * Video yang otomatis disorot di tab Video; diisi lewat overload
	 * {@link #display(Pertemuan, DataLoader, int, VideoPertemuan)}.
	 */
	private VideoPertemuan selectedVideoPertemuan = null;
	/**
	 * User yang sedang membuka window — sumber kebenaran identitas bagi seluruh gerbang
	 * tampilan di kelas ini ({@code getMahasiswa()}, {@code getSiswa()}, {@code getCalonSiswa()},
	 * {@code getBiodataCalonMahasiswa()}, {@code getPesertaKursus()}). Diisi di constructor dan
	 * tidak pernah berubah setelahnya.
	 *
	 * <p><b>Perhatian:</b> {@link #initTugas(Component, boolean, boolean)} dan
	 * {@link #initCatatan(boolean)} men-dereference {@code tbmuser} secara langsung
	 * ({@code tbmuser.getPesertaKursus()}), sehingga jalur yang meneruskan {@code tbmuser}
	 * bernilai {@code null} akan melempar {@code NullPointerException} di sana.</p>
	 */
	private Tbmuser tbmuser;

	/** Constructor tanpa argumen: pakai user login saat ini ({@code Common.getCurrentUser()}). */
	public PertemuanHelper() {
		this(Common.getCurrentUser());
	}

	/**
	 * Membangun helper untuk identitas {@code tbmuser} tertentu (mahasiswa/calon
	 * mahasiswa/siswa/calon siswa/dosen/admin — ditentukan dari field mana yang terisi di
	 * {@link Tbmuser}). Menyiapkan seluruh sub-helper tab (tugas, materi, audio, video,
	 * absensi, ujian, diskusi, hasil) dengan konteks identitas ini sehingga query dan hak
	 * edit tiap tab konsisten dengan siapa yang sedang melihat.
	 *
	 * @param tbmuser user yang membuka pertemuan; boleh {@code null} (ditangani null-safe untuk
	 *                {@code videoPertemuanHelper}/{@code audioPertemuanHelper}, tapi TIDAK
	 *                null-safe untuk {@code pertemuanPunyaUjianSiswaHelper}/
	 *                {@code absensiSiswaHelper} yang langsung memanggil {@code tbmuser.getSiswa()}
	 *                tanpa cek null — constructor ini mengasumsikan pemanggil sudah memastikan
	 *                {@code tbmuser} tidak null kecuali lewat jalur {@link #PertemuanHelper()}
	 *                yang parameternya berasal dari {@code Common.getCurrentUser()}).
	 */
	public PertemuanHelper(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
		if (tbmuser != null) {
			mahasiswa = tbmuser.getMahasiswa();
			biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
		}
		tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
		filePerkuliahanHelper = new FilePerkuliahanHelper(mahasiswa, biodataCalonMahasiswa);
		// FIX NPE: tbmuser bisa null di konstruktor ini (parameter tbmuser diteruskan apa adanya,
		// tidak selalu berasal dari Common.getCurrentUser() yang sudah dicek pemanggilnya).
		videoPertemuanHelper = new VideoPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		audioPertemuanHelper = new AudioPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		absensiHelper = new AbsensiHelper(mahasiswa, biodataCalonMahasiswa);
		pertemuanPunyaUjianHelper = new PertemuanPunyaUjianHelper(mahasiswa, biodataCalonMahasiswa);

		pertemuanPunyaUjianSiswaHelper = new PertemuanPunyaUjianSiswaHelper(tbmuser.getSiswa(),
				tbmuser.getCalonSiswa());

		absensiSiswaHelper = new AbsensiSiswaHelper(tbmuser.getSiswa(), tbmuser.getCalonSiswa());

		pertemuanPunyaDiskusiHelper = new PertemuanPunyaDiskusiHelper(mahasiswa,
				tbmuser == null ? null : tbmuser.ambilDosen(), biodataCalonMahasiswa,
				tbmuser == null ? null : tbmuser.getSiswa(), tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaHasilHelper = new PertemuanPunyaHasilHelper();
	}

	/**
	 * Sama seperti {@link #PertemuanHelper(Mahasiswa, BiodataCalonMahasiswa)} tapi dengan kendali
	 * eksplisit atas tombol "Selesai" (South toolbar) di {@link #init()}.
	 *
	 * @param mahasiswa              lihat kuirk penimpaan di {@link #PertemuanHelper(Mahasiswa, BiodataCalonMahasiswa)}
	 *                               yang dipanggil terlebih dahulu.
	 * @param biodataCalonMahasiswa  idem.
	 * @param tampilSelesai          {@code true} untuk menampilkan toolbar "Selesai" di bawah
	 *                               window (nilai default {@code true} bila konstruktor lain dipakai).
	 */
	public PertemuanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, boolean tampilSelesai) {
		this(mahasiswa, biodataCalonMahasiswa);
		this.tampilSelesai = tampilSelesai;
	}

	/**
	 * Membangun helper untuk konteks mahasiswa/calon mahasiswa yang diberikan eksplisit.
	 *
	 * <p><b>Kuirk penting:</b> method ini membaca ulang {@code Common.getCurrentUser()} dan, bila
	 * user sesi login punya {@code mahasiswa}/{@code biodataCalonMahasiswa} sendiri, MENIMPA
	 * parameter yang diteruskan pemanggil dengan milik user sesi tersebut. Akibatnya parameter
	 * {@code mahasiswa}/{@code biodataCalonMahasiswa} di sini hanya benar-benar dipakai ketika
	 * user sesi login tidak mewakili siapa pun (mis. dipanggil dari konteks admin/dosen yang
	 * membuka pertemuan atas nama mahasiswa lain) — bukan sekadar nilai default yang bisa dipaksa.
	 * {@code tbmuser} boleh {@code null}; seluruh pemakaian berikutnya di constructor ini sudah
	 * null-safe (beda dengan {@link #PertemuanHelper(Tbmuser)}).</p>
	 *
	 * @param mahasiswa             kandidat mahasiswa pemilik konteks (lihat kuirk di atas).
	 * @param biodataCalonMahasiswa kandidat calon mahasiswa pemilik konteks (lihat kuirk di atas).
	 */
	public PertemuanHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		Tbmuser tbmuser = Common.getCurrentUser();
		this.tbmuser = tbmuser;
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			mahasiswa = tbmuser.getMahasiswa();
		}
		if (tbmuser != null && tbmuser.getBiodataCalonMahasiswa() != null) {
			biodataCalonMahasiswa = tbmuser.getBiodataCalonMahasiswa();
		}
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
		tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
		filePerkuliahanHelper = new FilePerkuliahanHelper(mahasiswa, biodataCalonMahasiswa);
		// FIX NPE: tbmuser bisa null (mis. akses anonim/edge-case sesi) -- konstruktor lain di
		// bawah (absensiSiswaHelper dst.) sudah menjaga tbmuser null, tapi 2 baris ini belum.
		videoPertemuanHelper = new VideoPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		audioPertemuanHelper = new AudioPertemuanHelper(mahasiswa == null && biodataCalonMahasiswa == null
				&& (tbmuser == null || (tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null)), false);
		absensiHelper = new AbsensiHelper(mahasiswa, biodataCalonMahasiswa);
		absensiSiswaHelper = new AbsensiSiswaHelper(tbmuser == null ? null : tbmuser.getSiswa(),
				tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaUjianHelper = new PertemuanPunyaUjianHelper(mahasiswa, biodataCalonMahasiswa);
		pertemuanPunyaUjianSiswaHelper = new PertemuanPunyaUjianSiswaHelper(tbmuser == null ? null : tbmuser.getSiswa(),
				tbmuser == null ? null : tbmuser.getCalonSiswa());

		pertemuanPunyaDiskusiHelper = new PertemuanPunyaDiskusiHelper(mahasiswa,
				tbmuser == null ? null : tbmuser.ambilDosen(), biodataCalonMahasiswa,
				tbmuser == null ? null : tbmuser.getSiswa(), tbmuser == null ? null : tbmuser.getCalonSiswa());
		pertemuanPunyaHasilHelper = new PertemuanPunyaHasilHelper();
	}

	/** Overload singkat: memuat tab Tugas tanpa memaksa memilih tab tugas terakhir. */
	private void initTugas(final Component tabpanelFileTugasPertemuan, boolean tampilInfo) throws Exception {
		initTugas(tabpanelFileTugasPertemuan, false, tampilInfo);
	}

	/**
	 * Membangun isi tab Tugas: sebuah {@link Tabbox} bersarang yang sub-tab-nya adalah tugas
	 * utama pertemuan ({@code pertemuan.getJudultugas()}, bila diisi) diikuti seluruh
	 * {@link Tugas} lain (individu {@link TugasPertemuan} atau kelompok {@link TugasKelompok})
	 * dari {@code pertemuan.ambilTugasTotalSemua()}. Isi sub-tab dimuat LAZY: hanya dibangun saat
	 * tab benar-benar diklik (event listener {@code onClick} yang cek
	 * {@code tabpanelUtama.getChildren().isEmpty()}), kecuali tugas yang cocok dengan
	 * {@code selectedTugasPertemuan}/{@code selectedTugasKelompok} (di-preselect dari
	 * {@link #display}) yang langsung dibangun dan tab-nya diaktifkan.
	 *
	 * <p>Di akhir method ditambahkan tab "Tugas Individu Baru" dan (bila pertemuan berasal dari
	 * perkuliahan/jadwal pelajaran/KKN/PKL) "Tugas Kelompok Baru" — keduanya hanya tampak untuk
	 * admin/dosen/guru (disembunyikan bila {@code mahasiswa}/{@code biodataCalonMahasiswa} terisi
	 * atau {@code tbmuser} adalah peserta kursus/siswa). Mengklik salah satunya membuat entity
	 * {@link TugasPertemuan}/{@link TugasKelompok} baru dengan atribut penilaian disalin dari
	 * {@code pertemuan} (format nilai, syarat mengumpulkan, prosentase), langsung
	 * {@code session.save()+flush()}, lalu memanggil ulang {@code pertemuan.reInitTugasPertemuan}/
	 * {@code reInitTugasKelompok} dan memuat ulang tab ini dengan tugas baru terpilih.</p>
	 *
	 * @param tabpanelFileTugasPertemuan wadah tab (dikosongkan dulu lewat {@code Common.clear}
	 *                                   bila tidak {@code null}) tempat sub-tabbox dipasang.
	 * @param selectTerakhir             bila {@code true} dan belum ada tugas terpilih, tab tugas
	 *                                   TERAKHIR dalam iterasi otomatis dipilih &amp; dimuat (dipakai
	 *                                   {@link #tampilanMobile} setelah membuat tugas baru).
	 * @param tampilInfo                 diteruskan ke {@code tugasMandiriHelper.createTugas} untuk
	 *                                   menampilkan/menyembunyikan info pertemuan di dalam sub-tab.
	 */
	private void initTugas(final Component tabpanelFileTugasPertemuan, final boolean selectTerakhir,
			final boolean tampilInfo) throws Exception {
		if (tabpanelFileTugasPertemuan != null) {
			Common.clear(tabpanelFileTugasPertemuan);
		}
		Borderlayout borderlayout1 = new Borderlayout();
		borderlayout1.setParent(tabpanelFileTugasPertemuan);

		Center center1 = new Center();
		center1.setParent(borderlayout1);
		ais.ui.util.ZkCompat.setFlex(center1, true);

		Tabbox tabbox1 = new Tabbox();
		tabbox1.setParent(center1);

		final Tabs tabs1 = new Tabs();
		tabs1.setParent(tabbox1);

		Collection<Tugas> tugases = pertemuan.ambilTugasTotalSemua().values();

		final Tabpanels tabpanels1 = new Tabpanels();
		tabpanels1.setParent(tabbox1);

		boolean buka = false;
		if (!tugases.isEmpty() && pertemuan.getJudultugas().trim().isEmpty()) {
			buka = true;
//			System.out.println("Tugas utama tidak ditampilkan karena sudah ada sub tugas");
		} else {
			final Tab tabTugas1 = new Tab(pertemuan.getJudultugas().trim().isEmpty() ? "(Tidak ada tugas)"
					: pertemuan.getJudultugas().length() > 30 ? pertemuan.getJudultugas().substring(0, 30) + "..."
							: pertemuan.getJudultugas(),
					"/img/Status-mail-task-icon.png");

			tabTugas1.setParent(tabs1);

			Tabpanel tabpanelUtama1 = new ais.ui.util.MyTabpanel();
			tabpanelUtama1.setParent(tabpanels1);

			tugasMandiriHelper.createTugas(pertemuan, tabpanelUtama1, new EventListener() {

				/**
				 * Callback "tugas berubah" untuk sub-tab tugas BAWAAN pertemuan (yang judulnya diambil dari
				 * {@code pertemuan.getJudultugas()}). Dipanggil oleh {@link TugasMandiriHelper} setiap kali
				 * tugas disimpan/dihapus, dan membangun ulang seluruh daftar sub-tab lewat
				 * {@link #initTugas(Component, boolean)} agar judul dan jumlah tab ikut ter-refresh.
				 *
				 * @param arg0 event dari sub-helper; isinya tidak dipakai.
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
				}
			}, tampilInfo);
		}

		Tab tabTugas = null;
		Tabpanel tabpanelUtamaU = null;
		Tugas tugasPertemuanU = null;
		for (final Tugas tugasPertemuan : tugases) {
			if (tugasPertemuan != null && tugasPertemuan.getId() != null) {
				tugasPertemuanU = tugasPertemuan;

				if (tugasPertemuan instanceof TugasPertemuan) {
					((TugasPertemuan) tugasPertemuan).setPertemuan(pertemuan.getId());
				} else if (tugasPertemuan instanceof TugasKelompok) {
					((TugasKelompok) tugasPertemuan).setPertemuan(pertemuan.getId());
				}

				String icon = "Status-mail-task-icon.png";
				if (tugasPertemuan instanceof TugasKelompok) {
					icon = "Healthcare-Groups-icon.png";
				}

				tabTugas = new Tab(tugasPertemuan.getJudultugas().trim().isEmpty() ? "(Tidak ada tugas)"
						: tugasPertemuan.getJudultugas().length() > 30
								? tugasPertemuan.getJudultugas().substring(0, 30) + "..."
								: tugasPertemuan.getJudultugas(),
						"/img/" + icon);
				tabTugas.setVisible(!tugasPertemuan.getJudultugas().trim().isEmpty());
				tabTugas.setParent(tabs1);
				final Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
				tabpanelUtama.setParent(tabpanels1);
				tabpanelUtamaU = tabpanelUtama;
				if (selectedTugasPertemuan != null && selectedTugasPertemuan.getId() != null
						&& selectedTugasPertemuan.getId().equals(tugasPertemuan.getId())) {

					TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
					tugasMandiriHelper.createTugas(tugasPertemuan, tabpanelUtama, new EventListener() {

						/**
						 * Callback "tugas berubah" untuk sub-tab tugas INDIVIDU yang sedang di-preselect lewat
						 * {@link #selectedTugasPertemuan}. Sama seperti callback tugas bawaan: membangun ulang
						 * seluruh daftar sub-tab lewat {@link #initTugas(Component, boolean)}.
						 *
						 * @param arg0 event dari sub-helper; isinya tidak dipakai.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
						}
					}, tampilInfo);
					tabTugas.setSelected(true);
				} else if (selectedTugasKelompok != null && selectedTugasKelompok.getId() != null
						&& selectedTugasKelompok.getId().equals(tugasPertemuan.getId())) {
					TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
					tugasKelompokHelper.tampilanTugas(selectedTugasKelompok, tabpanelUtama, new EventListener() {

						/**
						 * Callback "tugas berubah" untuk sub-tab tugas KELOMPOK yang sedang di-preselect lewat
						 * {@link #selectedTugasKelompok}; membangun ulang daftar sub-tab.
						 *
						 * @param arg0 event dari sub-helper; isinya tidak dipakai.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
						}
					});
					tabTugas.setSelected(true);
				} else {
					EventListener eventListener = new EventListener() {

						/**
						 * Listener {@code onClick} pada tab tugas yang TIDAK di-preselect — inilah mekanisme
						 * lazy-load per sub-tab: konten baru dibangun saat tab benar-benar diklik, dan hanya bila
						 * panelnya masih kosong ({@code tabpanelUtama.getChildren().isEmpty()}) sehingga klik
						 * berulang tidak menumpuk komponen. Memilih {@link TugasMandiriHelper} untuk
						 * {@link TugasPertemuan} atau {@link TugasKelompokHelper} untuk {@link TugasKelompok}.
						 *
						 * <p>Listener yang sama juga dipakai sebagai isi {@code Common.createDefaultTimer(...)} ketika
						 * {@code buka} bernilai {@code true} (yaitu ketika pertemuan tidak punya tugas bawaan
						 * sehingga sub-tab pertama perlu dibuka otomatis).</p>
						 *
						 * @param arg0 event {@code onClick} dari tab; isinya tidak dipakai.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (tabpanelUtama.getChildren().isEmpty()) {

								if (tugasPertemuan instanceof TugasPertemuan) {
									TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa,
											biodataCalonMahasiswa);
									tugasMandiriHelper.createTugas(tugasPertemuan, tabpanelUtama, new EventListener() {

										/**
										 * Callback "tugas berubah" untuk tugas individu yang dimuat malas oleh listener klik tab di
										 * atas; membangun ulang daftar sub-tab lewat {@link #initTugas(Component, boolean)}.
										 *
										 * @param arg0 event dari sub-helper; isinya tidak dipakai.
										 */
										@Override
										public void onEvent(Event arg0) throws Exception {
											initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
										}
									}, tampilInfo);
								} else if (tugasPertemuan instanceof TugasKelompok) {
									TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa,
											biodataCalonMahasiswa);
									tugasKelompokHelper.tampilanTugas((TugasKelompok) tugasPertemuan, tabpanelUtama,
											new EventListener() {

												/**
												 * Callback "tugas berubah" untuk tugas kelompok yang dimuat malas oleh listener klik tab di
												 * atas; membangun ulang daftar sub-tab lewat {@link #initTugas(Component, boolean)}.
												 *
												 * @param arg0 event dari sub-helper; isinya tidak dipakai.
												 */
												@Override
												public void onEvent(Event arg0) throws Exception {
													initTugas(tabpanelFileTugasPertemuan, selectTerakhir);
												}
											});
								}
							}
						}
					};

					tabTugas.addEventListener("onClick", eventListener);

					if (buka) {
						buka = false;
						Common.createDefaultTimer(eventListener);
					}
				}
			}
		}
		tugases = null;

		if (selectedTugasPertemuan == null) {
			if (tabTugas != null && selectTerakhir) {
				tabTugas.setSelected(true);
				TugasMandiriHelper tugasMandiriHelper = new TugasMandiriHelper(mahasiswa, biodataCalonMahasiswa);
				tugasMandiriHelper.createTugas(tugasPertemuanU, tabpanelUtamaU, new EventListener() {

					/**
					 * Callback "tugas berubah" untuk sub-tab tugas individu TERAKHIR yang dibuka otomatis ketika
					 * {@code selectTerakhir} bernilai {@code true} dan tidak ada preselect. Berbeda dari callback
					 * lain: memanggil {@code initTugas(..., true)} sehingga setelah refresh tab terakhir kembali
					 * dipilih otomatis.
					 *
					 * @param arg0 event dari sub-helper; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						initTugas(tabpanelFileTugasPertemuan, true);
					}
				}, tampilInfo);
			}
		}
		if (selectedTugasKelompok == null) {
			if (tabTugas != null && selectTerakhir) {
				tabTugas.setSelected(true);
				TugasKelompokHelper tugasKelompokHelper = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
				tugasKelompokHelper.tampilanTugas((TugasKelompok) tugasPertemuanU, tabpanelUtamaU, new EventListener() {

					/**
					 * Padanan callback di atas untuk tugas KELOMPOK terakhir; juga memanggil
					 * {@code initTugas(..., true)} agar tab terakhir tetap terpilih setelah refresh.
					 *
					 * @param arg0 event dari sub-helper; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						initTugas(tabpanelFileTugasPertemuan, true);
					}
				});
			}
		}

		final Tab tabTugasBaru = new Tab("Tugas Individu Baru", "/img/add_item.png");
		tabTugasBaru.setVisible(mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null);
		tabTugasBaru.setParent(tabs1);
		tabTugasBaru.addEventListener("onClick", new EventListener() {

			/**
			 * Aksi tab "Tugas Individu Baru": MEMBUAT dan LANGSUNG MENYIMPAN satu {@link TugasPertemuan}
			 * kosong ({@code session.save()} + {@code flush()}) yang mewarisi format nilai, syarat
			 * mengumpulkan, dan prosentase dari {@code pertemuan}, dengan judul otomatis "Tugas individu
			 * pertemuan ke N". Tidak ada dialog konfirmasi: satu klik = satu baris tugas baru di basis
			 * data. Penyegaran UI ditunda ke timer terpisah agar {@code flush} sudah selesai sebelum
			 * {@code pertemuan.reInitTugasPertemuan(session)} membaca ulang koleksinya.
			 *
			 * <p><b>Catatan otorisasi (perluasan pola terdokumentasi "gerbang UI-only"):</b> satu-satunya
			 * pembatas siapa yang boleh membuat tugas adalah {@code tabTugasBaru.setVisible(...)} pada
			 * komponen tab — sebuah gerbang TAMPILAN. Listener ini sendiri tidak mengulang pemeriksaan
			 * {@code mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() ==
			 * null && tbmuser.getSiswa() == null}, dan juga tidak memeriksa apakah pengguna berhak
			 * mengajar pada {@code pertemuan} ini. Pola yang sama berlaku pada tab "Tugas Kelompok
			 * Baru".</p>
			 *
			 * @param arg0 event {@code onClick} dari tab; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				final TugasPertemuan tugasPertemuan = new TugasPertemuan();
				tugasPertemuan.setPertemuan(pertemuan.getId());
				tugasPertemuan.setFormatNilai(pertemuan.getFormatNilai());
				tugasPertemuan.setSyaratMengumpulkanTugas(pertemuan.getSyaratMengumpulkanTugas());
				tugasPertemuan.setProsentase(pertemuan.getProsentase());
				tugasPertemuan.setJudultugas("Tugas individu pertemuan ke " + pertemuan.getPertemuanKe());
				Session session = HibernateUtil.currentSession();
				session.save(tugasPertemuan);
				session.flush();

				Common.createDefaultTimer(new EventListener() {

					/**
					 * Fase kedua pembuatan tugas individu, dijalankan pada siklus event ZK berikutnya: membaca
					 * ulang koleksi tugas pertemuan ({@code reInitTugasPertemuan}), menyetel
					 * {@link #selectedTugasPertemuan} ke tugas yang baru dibuat (dan me-{@code null}-kan
					 * {@link #selectedTugasKelompok}), lalu membangun ulang sub-tab dengan
					 * {@code selectTerakhir=false} sehingga tugas barulah yang terpilih, bukan tugas terakhir.
					 *
					 * @param arg0 event timer; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						Session session = HibernateUtil.currentSession();
						pertemuan.reInitTugasPertemuan(session);

						selectedTugasKelompok = null;
						selectedTugasPertemuan = tugasPertemuan;
						initTugas(tabpanelFileTugasPertemuan, false);
					}
				});
			}
		});

		if (pertemuan != null && (pertemuan.getPerkuliahan() != null || pertemuan.getJadwalPelajaran() != null
				|| pertemuan.getKelompokKkn() != null || pertemuan.getKelompokPkl() != null)) {
			final Tab tabTugasKelompokBaru = new Tab("Tugas Kelompok Baru", "/img/add_item.png");
			tabTugasKelompokBaru.setVisible(mahasiswa == null && biodataCalonMahasiswa == null
					&& tbmuser.getPesertaKursus() == null && tbmuser.getSiswa() == null);
			tabTugasKelompokBaru.setParent(tabs1);
			tabTugasKelompokBaru.addEventListener("onClick", new EventListener() {

				/**
				 * Aksi tab "Tugas Kelompok Baru": padanan tugas individu untuk {@link TugasKelompok} —
				 * membuat dan langsung menyimpan satu tugas kelompok kosong berjudul "Tugas kelompok
				 * pertemuan ke N" yang mewarisi format nilai/syarat/prosentase dari pertemuan. Tab ini hanya
				 * dibuat bila pertemuan berasal dari perkuliahan, jadwal pelajaran, kelompok KKN, atau
				 * kelompok PKL (konteks yang punya rombongan peserta). Berbagi catatan otorisasi yang sama
				 * dengan tab "Tugas Individu Baru": gerbangnya hanya {@code setVisible} di sisi tampilan.
				 *
				 * @param arg0 event {@code onClick} dari tab; isinya tidak dipakai.
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {

					final TugasKelompok tugasKelompok = new TugasKelompok();
					tugasKelompok.setPertemuan(pertemuan.getId());
					tugasKelompok.setFormatNilai(pertemuan.getFormatNilai());
					tugasKelompok.setSyaratMengumpulkanTugas(pertemuan.getSyaratMengumpulkanTugas());
					tugasKelompok.setProsentase(pertemuan.getProsentase());
					tugasKelompok.setJudultugas("Tugas kelompok pertemuan ke " + pertemuan.getPertemuanKe());
					Session session = HibernateUtil.currentSession();
					session.save(tugasKelompok);
					session.flush();

					Common.createDefaultTimer(new EventListener() {

						/**
						 * Fase kedua pembuatan tugas kelompok: {@code reInitTugasKelompok}, set
						 * {@link #selectedTugasKelompok} ke tugas baru (dan {@link #selectedTugasPertemuan} ke
						 * {@code null}), lalu bangun ulang sub-tab agar tugas kelompok baru langsung terpilih.
						 *
						 * @param arg0 event timer; isinya tidak dipakai.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = HibernateUtil.currentSession();
							pertemuan.reInitTugasKelompok(session);

							selectedTugasPertemuan = null;
							selectedTugasKelompok = tugasKelompok;
							initTugas(tabpanelFileTugasPertemuan, false);
						}
					});
				}
			});
		}
	}

	/**
	 * Membangun tampilan desktop: memasang {@code MyButtonTabbox} berisi 9 tab lazy-load (lihat
	 * daftar lengkap di Javadoc kelas) ke dalam {@code container}, dengan tab Dasbor didaftarkan
	 * pertama agar tampil di posisi paling kiri walau secara logis merupakan tab "ke-9". Tinggi
	 * tabbox dipatok {@code calc(100vh - 70px)} untuk menyisakan ruang toolbar South. Di akhir,
	 * {@code btnTab.pilih(index)} memaksa tab yang sesuai state {@code index} (dari
	 * {@link #display}) menjadi aktif — dipanggil setelah semua tab terdaftar karena
	 * {@code buatTombolDanPanel()} pada {@code MyButtonTabbox} akan meng-override pilihan ke tab
	 * pertama jika dipanggil lebih awal.
	 *
	 * @param container komponen ZK tempat tabbox dipasang (Center dari {@link MyBorderlayout}).
	 */
	private void tampilanDesktop(org.zkoss.zk.ui.Component container) throws Exception {
		TreeMap<Long, TugasPertemuan> tugasPertemuansa = pertemuan.ambilTugasPertemuanTotal();
		final int jumlahTugas = (pertemuan.getJudultugas().isEmpty() ? 0 : 1) + tugasPertemuansa.size();
		tugasPertemuansa = null;

		// Tinggi: 100vh dikurangi South + window chrome (~70px).
		// Info pertemuan hanya ditampilkan pada tab Dasbor; tab lain langsung menampilkan
		// kontennya agar panel ringkasan tidak berulang di setiap tab.
		btnTab = ais.ui.util.MyButtonTabbox.buat(container, "calc(100vh - 70px)",
				new int[] { index });

		// Tab 9: Dasbor — didaftarkan PERTAMA agar tampil paling kiri.
		// Berisi info VoPembelajaran + ringkasan data semua tab.
		btnTab.tambahTabLazy(9, "Dasbor", "/img/svg/dashboard-chart.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Dasbor</b> (indeks 9): mendelegasikan seluruh pembangunan ke
					 * {@link #initDasbor(Div)}.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						initDasbor(panel);
					}
				});

		// Tab 0: Kehadiran
		btnTab.tambahTabLazy(0, "Kehadiran", "/img/svg/person-check.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Kehadiran</b> (indeks 0). Di sinilah percabangan SEKOLAH vs AKADEMIK
					 * dijalankan: {@link AbsensiSiswaHelper} bila pertemuan berasal dari {@code JadwalPelajaran},
					 * {@code JadwalUjianPSB}, atau formulir kegiatan bersekolah; selain itu
					 * {@link AbsensiHelper}. Argumen terakhir {@code false} menandakan mode desktop (bukan
					 * mobile).
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null
								|| (pertemuan.getFormulirKegiatan() != null
										&& pertemuan.getFormulirKegiatan().getSekolah() != null)) {
							absensiSiswaHelper.mainInit(pertemuan, panel, false);
						} else {
							absensiHelper.mainInit(pertemuan, panel, false);
						}
					}
				});

		// Tab 1: Pembelajaran (catatan/materi belajar)
		btnTab.tambahTabLazy(1, "Pembelajaran", "/img/svg/book.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Pembelajaran</b> (indeks 1): menempelkan hasil
					 * {@link #initCatatan(boolean)} dengan {@code tampilInfo=false} — panel info pertemuan tidak
					 * diulang di sini karena sudah ada di tab Dasbor.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						panel.appendChild(initCatatan(false));
					}
				});

		// Tab 2: Materi (file konten pertemuan)
		btnTab.tambahTabLazy(2,
				"Materi (" + pertemuan.ambilJumlahPertemuanFileContent() + ")",
				"/img/svg/folder-open-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Materi</b> (indeks 2): daftar file konten pertemuan lewat
					 * {@code filePerkuliahanHelper}, dengan {@link #selectedPertemuanFileContent} sebagai item
					 * yang otomatis disorot bila window dibuka lewat tautan ke satu file tertentu.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						filePerkuliahanHelper.createFile(pertemuan, null, null, null, panel,
								selectedPertemuanFileContent);
					}
				});

		// Tab 3: Tugas
		btnTab.tambahTabLazy(3, "Tugas (" + jumlahTugas + ")", "/img/svg/card-checklist.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Tugas</b> (indeks 3): memanggil
					 * {@link #initTugas(Component, boolean)} dengan {@code selectTerakhir=false} sehingga sub-tab
					 * yang terpilih ditentukan oleh state preselect, bukan otomatis ke tugas terakhir.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						initTugas(panel, false);
					}
				});

		// Tab 4: Audio
		btnTab.tambahTabLazy(4,
				"Audio (" + pertemuan.ambilJumlahAudioPertemuan() + ")",
				"/img/svg/file-audio-thin.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Audio</b> (indeks 4): daftar {@link AudioPertemuan} lewat
					 * {@code audioPertemuanHelper}, dengan {@link #selectedAudioPertemuan} sebagai item yang
					 * otomatis disorot.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						audioPertemuanHelper.display(pertemuan, null, null, panel, selectedAudioPertemuan);
					}
				});

		// Tab 5: Video
		btnTab.tambahTabLazy(5,
				"Video (" + pertemuan.ambilJumlahVideoPertemuan() + ")",
				"/img/svg/camera-video.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Video</b> (indeks 5): daftar {@link VideoPertemuan} lewat
					 * {@code videoPertemuanHelper}, dengan {@link #selectedVideoPertemuan} sebagai item yang
					 * otomatis disorot.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						videoPertemuanHelper.display(pertemuan, null, null, panel, selectedVideoPertemuan);
					}
				});

		// Tab 6: Ujian
		btnTab.tambahTabLazy(6,
				"Ujian (" + pertemuan.ambilJumlahPertemuanPunyaUjian() + ")",
				"/img/svg/pencil-square.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Ujian</b> (indeks 6). Percabangan SEKOLAH vs AKADEMIK yang sejajar
					 * dengan tab Kehadiran, tetapi dengan syarat yang LEBIH SEMPIT: di sini hanya
					 * {@code getJadwalPelajaran()}/{@code getJadwalUjianPSB()} yang diperiksa, sedangkan tab
					 * Kehadiran juga memperhitungkan {@code getFormulirKegiatan().getSekolah()}. Artinya sebuah
					 * pertemuan dari formulir kegiatan sekolah memakai helper SISWA untuk kehadiran tetapi helper
					 * AKADEMIK untuk ujian.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null) {
							pertemuanPunyaUjianSiswaHelper.display(pertemuan, panel);
						} else {
							pertemuanPunyaUjianHelper.display(pertemuan, panel);
						}
					}
				});

		// Tab 7: Diskusi
		btnTab.tambahTabLazy(7,
				"Diskusi (" + pertemuan.ambilJumlahPertemuanPunyaDiskusi() + ")",
				"/img/svg/comment-2-text-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Diskusi</b> (indeks 7): forum diskusi pertemuan; argumen {@code false}
					 * menandakan mode desktop, dan {@link #selectedDiskusi} membuka satu utas tertentu bila
					 * disetel pemanggil.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						pertemuanPunyaDiskusiHelper.display(pertemuan, panel, false, selectedDiskusi);
					}
				});

		// Tab 8: Hasil, Evaluasi, Kusioner
		btnTab.tambahTabLazy(8, "Hasil, Evaluasi, Kusioner", "/img/svg/chart-line.svg",
				new ais.ui.util.MyButtonTabbox.PemuatTab() {
					/**
					 * Pemuat malas tab <b>Hasil, Evaluasi, Kusioner</b> (indeks 8). Tab ini tidak punya padanan
					 * pada {@link #tampilanMobile(Center)}.
					 *
					 * @param panel wadah tab yang disediakan {@code MyButtonTabbox}.
					 */
					@Override
					public void muat(Div panel) throws Exception {
						pertemuanPunyaHasilHelper.display(pertemuan, panel);
					}
				});

		// Paksa tab yang sesuai terpilih SETELAH semua tab terdaftar.
		// MyButtonTabbox.buat() menerima tabAktif[] sebagai hint, tapi
		// buatTombolDanPanel() meng-override-nya ke tab pertama jika target
		// belum ada di panelMap saat tab pertama ditambahkan.
		btnTab.pilih(index);
	}

	/**
	 * Menyisipkan panel info ringkas pertemuan (delegasi ke
	 * {@link DashboardTimelinePertemuan#displayInfoPertemuan(Pertemuan)}) di bagian atas
	 * {@code panel}, dibungkus {@code Div} dengan garis bawah tipis sebagai pemisah visual.
	 * Dipakai hanya di tab Dasbor.
	 *
	 * @param panel wadah tab Dasbor yang sedang dibangun.
	 */
	private void addInfoPertemuan(Div panel) throws Exception {
		Div infoWrap = new Div();
		infoWrap.setStyle("border-bottom:1px solid #dee2e6;padding:4px 8px 8px;margin-bottom:8px;");
		infoWrap.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));
		infoWrap.setParent(panel);
	}

	/**
	 * Membangun tab Dasbor: mengumpulkan statistik pertemuan dari method {@link Pertemuan} yang
	 * relevan (jumlah peserta, status kehadiran, tugas, ujian, materi/audio/video/diskusi — tiap
	 * pengumpulan data dibungkus {@code try/catch} individual dan diabaikan senyap bila gagal,
	 * sehingga satu sumber data yang error tidak menggagalkan seluruh dasbor) lalu merender:
	 * kartu KPI klik-navigasi ({@link #addKpiCard}), tabel kehadiran per jenis peserta
	 * (mahasiswa/dosen/siswa/guru, lewat {@link #ringkasStatus}), grafik batang bertumpuk
	 * pengumpulan tugas &amp; partisipasi ujian per item (lewat {@link HtmlChartHelper#stackedBar}),
	 * radar kelengkapan 7 dimensi (lewat {@link HtmlChartHelper#radar}, skala 0–10 hasil
	 * normalisasi kasar dari jumlah mentah), bar horizontal konten (materi/audio/video), info
	 * {@link ais.database.model.VOPembelajaran} bila ada, dan tabel navigasi tab. Setiap groupbox
	 * dibuat lewat {@link #buatGbClickable(String, int)} sehingga mengklik judulnya langsung
	 * berpindah ke tab terkait via {@code btnTab.pilih(tabIdx)}.
	 *
	 * <p>Kehadiran mahasiswa/siswa dihitung dari parsing string {@code pertemuan.getAbsensi()}
	 * (lihat {@link #hitungStatusDenganSuffix(String)}), sedangkan kehadiran dosen/guru memakai
	 * method khusus {@code pertemuan.hitungStatusDosen()}/{@code hitungStatusGuru()} yang totalnya
	 * dijumlahkan balik dari peta status bila {@code ambilDosen()}/{@code ambilGuru()} gagal atau
	 * kosong (fallback ganda untuk menghindari total 0 palsu).</p>
	 *
	 * @param panel wadah tab Dasbor.
	 */
	private void initDasbor(Div panel) throws Exception {
		addInfoPertemuan(panel);

		// === Kumpulkan data ===

		// Total peserta kelas
		int totalMahasiswa = 0;
		int totalSiswa = 0;
		int totalDosen = 0;
		int totalGuru = 0;
		try { totalMahasiswa = pertemuan.ambilMahasiswa().size(); } catch (Exception ignored) {}
		try {
			if (pertemuan.getJadwalPelajaran() != null)
				totalSiswa = pertemuan.ambilSiswa().size();
		} catch (Exception ignored) {}
		try { totalDosen = pertemuan.ambilDosen().size(); } catch (Exception ignored) {}
		try { totalGuru = pertemuan.ambilGuru().size(); } catch (Exception ignored) {}
		int totalPeserta = totalMahasiswa + totalSiswa;

		java.util.Map<String,Integer> statusMahasiswa = null;
		java.util.Map<String,Integer> statusSiswa = null;
		java.util.Map<String,Integer> statusMahasiswaSiswa = null;
		java.util.Map<String,Integer> statusDosen = null;
		java.util.Map<String,Integer> statusGuru = null;
		try { statusMahasiswa = hitungStatusDenganSuffix("mahasiswa"); } catch (Exception ignored) {}
		try { statusSiswa = hitungStatusDenganSuffix("siswa"); } catch (Exception ignored) {}
		statusMahasiswaSiswa = gabungStatus(statusMahasiswa, statusSiswa);
		try { statusDosen = pertemuan.hitungStatusDosen(); } catch (Exception ignored) {}
		try { statusGuru = pertemuan.hitungStatusGuru(); } catch (Exception ignored) {}

		if (totalDosen == 0 && statusDosen != null) {
			for (Integer v : statusDosen.values()) {
				totalDosen += v == null ? 0 : v.intValue();
			}
		}
		if (totalGuru == 0 && statusGuru != null) {
			for (Integer v : statusGuru.values()) {
				totalGuru += v == null ? 0 : v.intValue();
			}
		}

		// Kehadiran: M=Masuk, S=Sakit, I=Izin, A=Alpa
		int cMasuk = 0, cSakit = 0, cIzin = 0, cAlpa = 0;
		try {
			java.util.Map<String,Integer> sm = statusMahasiswaSiswa;
			if (sm != null) {
				for (java.util.Map.Entry<String,Integer> en : sm.entrySet()) {
					int v = en.getValue() == null ? 0 : en.getValue();
					if ("M".equals(en.getKey()))      cMasuk  += v;
					else if ("S".equals(en.getKey())) cSakit  += v;
					else if ("I".equals(en.getKey())) cIzin   += v;
					else if ("A".equals(en.getKey())) cAlpa   += v;
				}
			}
		} catch (Exception ignored) {}
		int cBelumAbsen = Math.max(0, totalPeserta - cMasuk - cSakit - cIzin - cAlpa);

		// Tugas: per-TugasPertemuan kumpulkan nama + jumlah terkumpul
		TreeMap<Long,TugasPertemuan> tugasMap = null;
		try { tugasMap = pertemuan.ambilTugasPertemuanTotal(); } catch (Exception ignored) {}
		List<String>  tugasNama      = new ArrayList<String>();
		List<Integer> tugasTerkumpul = new ArrayList<Integer>();
		List<Integer> tugasBelum     = new ArrayList<Integer>();
		if (tugasMap != null) {
			int idx = 0;
			for (java.util.Map.Entry<Long,TugasPertemuan> en : tugasMap.entrySet()) {
				idx++;
				TugasPertemuan tp = en.getValue();
				String judul = null;
				try { judul = tp.getJudultugas(); } catch (Exception ignored) {}
				if (judul == null || judul.trim().isEmpty()) judul = "Tugas " + idx;
				if (judul.length() > 30) judul = judul.substring(0, 27) + "...";
				int kumpul = 0;
				try { kumpul = tp.ambilJumlahTugasFileContent(); } catch (Exception ignored) {}
				tugasNama.add(judul);
				tugasTerkumpul.add(kumpul);
				tugasBelum.add(Math.max(0, totalPeserta - kumpul));
			}
		}
		int totalTerkumpul = 0;
		for (int v : tugasTerkumpul) totalTerkumpul += v;
		int totalBelumTerkumpul = 0;
		for (int v : tugasBelum) totalBelumTerkumpul += v;
		int jTugas = (pertemuan.getJudultugas() != null && !pertemuan.getJudultugas().isEmpty() ? 1 : 0)
				   + (tugasMap != null ? tugasMap.size() : 0);

		// Ujian: per-PertemuanPunyaUjian ikut vs belum
		List<String>  ujianNama = new ArrayList<String>();
		List<Integer> ujianIkut = new ArrayList<Integer>();
		List<Integer> ujianBelum = new ArrayList<Integer>();
		try {
			TreeMap<Long,PertemuanPunyaUjian> ujianMap =
					pertemuan.ambilPertemuanPunyaUjianTotal(null);
			if (ujianMap != null) {
				int idx = 0;
				for (java.util.Map.Entry<Long,PertemuanPunyaUjian> en : ujianMap.entrySet()) {
					idx++;
					PertemuanPunyaUjian ppu = en.getValue();
					String nama = null;
					try { nama = ppu.getNama(); } catch (Exception ignored) {}
					if (nama == null || nama.trim().isEmpty()) nama = "Ujian " + idx;
					if (nama.length() > 30) nama = nama.substring(0, 27) + "...";
					int sudah = 0;
					try { sudah = ppu.ambilJumlahHasilUjianMahasiswaTelahIkut(false); } catch (Exception ignored) {}
					ujianNama.add(nama);
					ujianIkut.add(sudah);
					ujianBelum.add(Math.max(0, totalPeserta - sudah));
				}
			}
		} catch (Exception ignored) {}
		int jUjian = ujianNama.size();
		int totalMengerjakanUjian = 0;
		for (int v : ujianIkut) totalMengerjakanUjian += v;
		int totalBelumMengerjakanUjian = 0;
		for (int v : ujianBelum) totalBelumMengerjakanUjian += v;

		// Konten
		int cMateri = 0, cAudio = 0, cVideo = 0, cDiskusi = 0;
		try { cMateri  = pertemuan.ambilJumlahPertemuanFileContent(); }   catch (Exception ignored) {}
		try { cAudio   = pertemuan.ambilJumlahAudioPertemuan(); }         catch (Exception ignored) {}
		try { cVideo   = pertemuan.ambilJumlahVideoPertemuan(); }         catch (Exception ignored) {}
		try {
			Number nd = pertemuan.ambilJumlahPertemuanPunyaDiskusi();
			if (nd != null) cDiskusi = nd.intValue();
		} catch (Exception ignored) {}

		// === KPI Cards — individual clickable per kategori ===
		org.zkoss.zul.Groupbox gbKpi = new org.zkoss.zul.Groupbox();
		gbKpi.setMold("3d");
		new org.zkoss.zul.Caption("Ringkasan Cepat").setParent(gbKpi);
		gbKpi.setStyle("margin:4px 8px 8px;");
		Div kpiRow = new Div();
		kpiRow.setStyle("display:flex;flex-wrap:wrap;");
		kpiRow.setParent(gbKpi);
		addKpiCard(kpiRow, "Total Peserta", String.valueOf(totalPeserta), "terdaftar",        "#1877f2", 0);
		addKpiCard(kpiRow, "Hadir",         String.valueOf(cMasuk),       "dari " + totalPeserta, "#42b72a", 0);
		addKpiCard(kpiRow, "Belum Absen",   String.valueOf(cBelumAbsen),  "perlu absen",       "#f7b928", 0);
		addKpiCard(kpiRow, "Tugas",         String.valueOf(jTugas),       "tugas aktif",       "#e4496b", 3);
		addKpiCard(kpiRow, "Ujian",         String.valueOf(jUjian),       "kuis/ujian",        "#8b5cf6", 6);
		addKpiCard(kpiRow, "Konten",        String.valueOf(cMateri + cAudio + cVideo), "materi+audio+video", "#06b6d4", 2);
		gbKpi.setParent(panel);

		// === Kehadiran — akses cepat sesuai jenis peserta/pengajar ===
		org.zkoss.zul.Groupbox gbHadir = buatGbClickable("Kehadiran ↗", 0);
		if (totalPeserta > 0) {
			Grid gHadir = new Grid();
			gHadir.setWidth("100%");
			gHadir.setSclass("ais-form-grid");
			Rows rHadir = new Rows();
			rHadir.setParent(gHadir);
			if (totalMahasiswa > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Mahasiswa",
						ringkasStatus(statusMahasiswa, totalMahasiswa), 0);
			}
			if (totalDosen > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Dosen",
						ringkasStatus(statusDosen, totalDosen), 0);
			}
			if (totalSiswa > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Siswa",
						ringkasStatus(statusSiswa, totalSiswa), 0);
			}
			if (totalGuru > 0) {
				tambahBarisClickable(rHadir, "Kehadiran Guru",
						ringkasStatus(statusGuru, totalGuru), 0);
			}
			gHadir.setParent(gbHadir);
		} else {
			new Label("Belum ada data peserta.").setParent(gbHadir);
		}
		gbHadir.setParent(panel);

		// === Tugas — stacked bar, klik → tab Tugas ===
		if (!tugasNama.isEmpty()) {
			org.zkoss.zul.Groupbox gbTugas = buatGbClickable("Pengumpulan Tugas ↗", 3);
			String[] catsTugas = tugasNama.toArray(new String[0]);
			double[][] valsTugas = new double[catsTugas.length][2];
			for (int i = 0; i < catsTugas.length; i++) {
				valsTugas[i][0] = tugasTerkumpul.get(i);
				valsTugas[i][1] = tugasBelum.get(i);
			}
			new org.zkoss.zul.Html(HtmlChartHelper.stackedBar(
				null, null, catsTugas,
				new String[]{"Sudah Kumpul","Belum Kumpul"},
				valsTugas, new String[]{"#42b72a","#e4e6eb"}
			)).setParent(gbTugas);
			gbTugas.setParent(panel);
		}

		// === Ujian — stacked bar, klik → tab Ujian ===
		if (!ujianNama.isEmpty()) {
			org.zkoss.zul.Groupbox gbUjian = buatGbClickable("Partisipasi Ujian / Kuis ↗", 6);
			String[] catsUjian = ujianNama.toArray(new String[0]);
			double[][] valsUjian = new double[catsUjian.length][2];
			for (int i = 0; i < catsUjian.length; i++) {
				valsUjian[i][0] = ujianIkut.get(i);
				valsUjian[i][1] = ujianBelum.get(i);
			}
			new org.zkoss.zul.Html(HtmlChartHelper.stackedBar(
				null, null, catsUjian,
				new String[]{"Sudah Ikut","Belum Ikut"},
				valsUjian, new String[]{"#8b5cf6","#e4e6eb"}
			)).setParent(gbUjian);
			gbUjian.setParent(panel);
		}

		// === Radar — kelengkapan, klik → tab Kehadiran sebagai starting point ===
		{
			org.zkoss.zul.Groupbox gbRadar = buatGbClickable("Kelengkapan Pertemuan ↗", 0);
			double rKehadiran = totalPeserta > 0 ? Math.min(10.0, (cMasuk * 10.0) / totalPeserta) : 0;
			new org.zkoss.zul.Html(HtmlChartHelper.radar(
				null,
				"Skor kelengkapan setiap dimensi (skala 0–10). Klik untuk navigasi.",
				new String[]{"Kehadiran","Materi","Tugas","Audio","Video","Ujian","Diskusi"},
				new String[]{"Pertemuan ini"},
				new double[][]{{
					rKehadiran,
					Math.min(10.0, cMateri  * 2.0),
					Math.min(10.0, jTugas   * 3.0),
					Math.min(10.0, cAudio   * 2.0),
					Math.min(10.0, cVideo   * 2.0),
					Math.min(10.0, jUjian   * 3.0),
					Math.min(10.0, cDiskusi * 3.0)
				}},
				new String[]{"#1877f2"}, 10.0
			)).setParent(gbRadar);
			gbRadar.setParent(panel);
		}

		// === Konten — bar horizontal, klik → tab Materi ===
		if (cMateri + cAudio + cVideo > 0) {
			org.zkoss.zul.Groupbox gbKonten = buatGbClickable("Konten Pertemuan ↗", 2);
			new org.zkoss.zul.Html(HtmlChartHelper.barHorizontal(
				null, null,
				new String[]{"Materi (berkas)","Audio","Video","Diskusi"},
				new double[]{cMateri, cAudio, cVideo, cDiskusi},
				"#06b6d4"
			)).setParent(gbKonten);
			gbKonten.setParent(panel);
		}

		// === Informasi Pembelajaran, klik → tab Pembelajaran ===
		ais.database.model.VOPembelajaran vo = pertemuan.ambilVOPembelajaran();
		if (vo != null) {
			org.zkoss.zul.Groupbox gbVo = buatGbClickable("Informasi Pembelajaran ↗", 1);
			gbVo.setStyle("margin:4px 8px 0;cursor:pointer;");
			Grid gVo = new Grid();
			gVo.setWidth("100%");
			gVo.setSclass("ais-form-grid");
			Rows rVo = new Rows();
			rVo.setParent(gVo);
			try {
				String info = vo.infoSimple();
				if (info != null && !info.isEmpty()) tambahBarisLabel(rVo, "Kelas / Mata Kuliah", info);
			} catch (Exception ignored) {}
			String kode = vo.getCourse();
			if (kode != null && !kode.isEmpty()) tambahBarisLabel(rVo, "Kode", kode);
			tambahBarisLabel(rVo, "Total Pertemuan Kelas", vo.ambilJumlahPertemuan() + " pertemuan");
			gVo.setParent(gbVo);
			gbVo.setParent(panel);
		}

		// === Navigasi Tab — semua tab sebagai baris clickable ===
		org.zkoss.zul.Groupbox gbNav = new org.zkoss.zul.Groupbox();
		gbNav.setMold("3d");
		new org.zkoss.zul.Caption("Navigasi Tab").setParent(gbNav);
		gbNav.setStyle("margin:8px;");
		Grid gNav = new Grid();
		gNav.setWidth("100%");
		gNav.setSclass("ais-form-grid");
		Rows rNav = new Rows();
		rNav.setParent(gNav);
		tambahBarisClickable(rNav, "Kehadiran",
				cMasuk + " hadir · " + cSakit + " sakit · " + cIzin + " izin · " + cAlpa + " alpa · " + cBelumAbsen + " belum absen", 0);
		tambahBarisClickable(rNav, "Pembelajaran", "Bahan kajian & catatan", 1);
		tambahBarisClickable(rNav, "Materi",   cMateri + " berkas tersedia", 2);
		tambahBarisClickable(rNav, "Tugas",    jTugas  + " tugas · " + totalTerkumpul
				+ " mengumpulkan · " + totalBelumTerkumpul + " belum mengumpulkan", 3);
		tambahBarisClickable(rNav, "Audio",    cAudio  + " audio", 4);
		tambahBarisClickable(rNav, "Video",    cVideo  + " video", 5);
		tambahBarisClickable(rNav, "Ujian",    jUjian  + " ujian · " + totalMengerjakanUjian
				+ " mengerjakan · " + totalBelumMengerjakanUjian + " belum mengerjakan", 6);
		tambahBarisClickable(rNav, "Diskusi",  cDiskusi + " diskusi", 7);
		tambahBarisClickable(rNav, "Hasil, Evaluasi, Kuesioner", "lihat evaluasi & kuesioner", 8);
		gNav.setParent(gbNav);
		gbNav.setParent(panel);
	}

	/**
	 * Menghitung distribusi kode kehadiran (M/S/I/A) untuk satu jenis peserta dari string mentah
	 * {@code pertemuan.getAbsensi()}. Format string: entri dipisah {@code ";"}, tiap entri
	 * dipisah {@code ","} dengan kode status di indeks ke-2 (indeks 0/1 diasumsikan id/nama
	 * peserta yang tidak dipakai di sini); hanya entri yang KUNCI-nya (nama peserta, case
	 * -insensitive) berakhiran {@code suffix} yang dihitung — inilah cara sederhana membedakan
	 * baris kehadiran mahasiswa dari baris kehadiran siswa dalam satu string absensi gabungan.
	 * Baris yang gagal di-parse (format tak sesuai) diabaikan senyap per-baris agar satu baris
	 * rusak tidak menggagalkan seluruh rekap.
	 *
	 * @param suffix akhiran kunci yang dicari, mis. {@code "mahasiswa"} atau {@code "siswa"}.
	 * @return peta kode status ({@code "M"/"S"/"I"/"A"}) ke jumlah kemunculannya; peta kosong bila
	 *         {@code pertemuan}/{@code suffix} {@code null} atau {@code absensi} kosong.
	 */
	private java.util.Map<String,Integer> hitungStatusDenganSuffix(String suffix) {
		java.util.Map<String,Integer> jumlah = new java.util.HashMap<String,Integer>();
		if (pertemuan == null || suffix == null) {
			return jumlah;
		}
		String absensi = pertemuan.getAbsensi();
		if (absensi == null || absensi.trim().isEmpty()) {
			return jumlah;
		}
		String suffixLower = suffix.toLowerCase();
		String[] nilais = absensi.split(";");
		for (String nn : nilais) {
			try {
				if (nn != null && nn.toLowerCase().endsWith(suffixLower)) {
					String[] s = nn.split(",");
					if (s.length > 2 && s[2] != null && !s[2].equalsIgnoreCase("-")) {
						Integer lama = jumlah.get(s[2]);
						jumlah.put(s[2], Integer.valueOf(lama == null ? 1 : lama.intValue() + 1));
					}
				}
			} catch (Exception ignored) {}
		}
		return jumlah;
	}

	/**
	 * Menggabungkan dua peta status kehadiran (mis. mahasiswa + siswa) menjadi satu peta
	 * terjumlah per kode status, dipakai untuk rekap "belum absen" gabungan di kartu KPI.
	 *
	 * @param a peta pertama (boleh {@code null}).
	 * @param b peta kedua (boleh {@code null}).
	 * @return peta baru hasil penjumlahan {@code a} dan {@code b} per kunci.
	 */
	private java.util.Map<String,Integer> gabungStatus(java.util.Map<String,Integer> a, java.util.Map<String,Integer> b) {
		java.util.Map<String,Integer> hasil = new java.util.HashMap<String,Integer>();
		tambahStatus(hasil, a);
		tambahStatus(hasil, b);
		return hasil;
	}

	/** Menjumlahkan nilai {@code sumber} ke dalam {@code hasil} per kunci in-place; no-op bila salah satu {@code null}. */
	private void tambahStatus(java.util.Map<String,Integer> hasil, java.util.Map<String,Integer> sumber) {
		if (hasil == null || sumber == null) {
			return;
		}
		for (java.util.Map.Entry<String,Integer> en : sumber.entrySet()) {
			if (en.getKey() != null) {
				Integer lama = hasil.get(en.getKey());
				int nilai = en.getValue() == null ? 0 : en.getValue().intValue();
				hasil.put(en.getKey(), Integer.valueOf(lama == null ? nilai : lama.intValue() + nilai));
			}
		}
	}

	/**
	 * Merangkai teks ringkasan kehadiran satu jenis peserta, mis. {@code "20 hadir · 1 sakit ·
	 * 0 izin · 2 alpa · 3 belum absen"}. "Belum absen" dihitung sebagai sisa
	 * {@code total - hadir - sakit - izin - alpa}, dijamin tidak negatif.
	 *
	 * @param statusMap peta kode status ke jumlah, hasil {@link #hitungStatusDenganSuffix} atau
	 *                  {@code pertemuan.hitungStatusDosen()}/{@code hitungStatusGuru()}.
	 * @param total     jumlah total peserta jenis ini (penyebut untuk menghitung "belum absen").
	 * @return baris teks ringkasan siap tampil.
	 */
	private String ringkasStatus(java.util.Map<String,Integer> statusMap, int total) {
		int hadir = nilaiStatus(statusMap, "M");
		int sakit = nilaiStatus(statusMap, "S");
		int izin = nilaiStatus(statusMap, "I");
		int alpa = nilaiStatus(statusMap, "A");
		int belum = Math.max(0, total - hadir - sakit - izin - alpa);
		return hadir + " hadir · " + sakit + " sakit · " + izin + " izin · " + alpa + " alpa · "
				+ belum + " belum absen";
	}

	/** Mengambil jumlah untuk satu {@code kode} status dari {@code statusMap}, {@code 0} bila tidak ada/null. */
	private int nilaiStatus(java.util.Map<String,Integer> statusMap, String kode) {
		if (statusMap == null || kode == null) {
			return 0;
		}
		Integer nilai = statusMap.get(kode);
		return nilai == null ? 0 : nilai.intValue();
	}

	/**
	 * Membuat {@link org.zkoss.zul.Groupbox} mold "3d" dengan {@code judul} sebagai caption,
	 * kursor pointer, dan {@code onClick} yang memanggil {@code btnTab.pilih(tabIdx)} — dipakai
	 * di seluruh tab Dasbor sebagai pola "kartu ringkasan yang bisa diklik untuk navigasi".
	 *
	 * @param judul  teks caption groupbox.
	 * @param tabIdx indeks tab tujuan navigasi saat groupbox diklik.
	 * @return groupbox baru (belum di-{@code setParent}, dipasang oleh pemanggil).
	 */
	private org.zkoss.zul.Groupbox buatGbClickable(String judul, final int tabIdx) {
		org.zkoss.zul.Groupbox gb = new org.zkoss.zul.Groupbox();
		gb.setMold("3d");
		new org.zkoss.zul.Caption(judul).setParent(gb);
		gb.setStyle("margin:8px;cursor:pointer;");
		gb.setTooltiptext("Klik untuk membuka tab");
		gb.addEventListener("onClick", new EventListener() {
			/**
			 * Navigasi kartu Dasbor: memindahkan tab aktif ke {@code tabIdx} lewat
			 * {@code btnTab.pilih(...)}. Aman dipakai hanya di mode desktop karena {@link #btnTab} bernilai
			 * {@code null} pada mode mobile — dan mode mobile memang tidak pernah membangun Dasbor.
			 *
			 * @param e event {@code onClick} pada groupbox; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIdx);
			}
		});
		return gb;
	}

	/**
	 * Menyisipkan satu kartu KPI klik-navigasi (label, nilai besar, subjudul, warna aksen) ke
	 * {@code parent} lewat {@link HtmlChartHelper#kpiCards}, dibungkus {@code Div} yang saat
	 * diklik memanggil {@code btnTab.pilih(tabIdx)}.
	 *
	 * @param parent   wadah baris kartu KPI.
	 * @param label    judul kartu, mis. {@code "Total Peserta"}.
	 * @param nilai    angka besar yang ditonjolkan (sudah berupa {@code String}).
	 * @param subtitle keterangan kecil di bawah nilai.
	 * @param warna    kode warna hex aksen kartu.
	 * @param tabIdx   indeks tab tujuan saat kartu diklik.
	 */
	private void addKpiCard(Div parent, String label, String nilai, String subtitle,
			String warna, final int tabIdx) {
		Div wrap = new Div();
		wrap.setStyle("cursor:pointer;flex:1 1 0;min-width:110px;");
		wrap.setTooltiptext("→ Tab " + label);
		wrap.addEventListener("onClick", new EventListener() {
			/**
			 * Navigasi kartu KPI Dasbor: memindahkan tab aktif ke {@code tabIdx} lewat
			 * {@code btnTab.pilih(...)} saat kartu diklik.
			 *
			 * @param e event {@code onClick} pada pembungkus kartu; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIdx);
			}
		});
		new org.zkoss.zul.Html(HtmlChartHelper.kpiCards(
			new String[]{label}, new String[]{nilai},
			new String[]{subtitle}, null, null, new String[]{warna}
		)).setParent(wrap);
		wrap.setParent(parent);
	}

	/** Menambah satu baris label:nilai statis (tanpa interaksi) ke {@code rows}, dipakai di panel info Pembelajaran pada Dasbor. */
	private void tambahBarisLabel(Rows rows, String label, String nilai) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		Label lbl = new Label(label);
		lbl.setStyle("font-weight:bold;color:#555;white-space:nowrap;padding-right:8px;");
		lbl.setParent(row);
		new Label(nilai).setParent(row);
	}

	/**
	 * Menambah satu baris label:nilai ke {@code rows} yang seluruh barisnya bisa diklik (kursor
	 * pointer, label bergaris bawah putus-putus) untuk pindah ke {@code tabIndex} — dipakai di
	 * tabel navigasi tab paling bawah Dasbor.
	 *
	 * @param rows     wadah baris grid.
	 * @param label    teks label (kolom kiri, biru bergaris bawah).
	 * @param nilai    teks nilai/ringkasan (kolom kanan).
	 * @param tabIndex indeks tab tujuan saat baris diklik.
	 */
	private void tambahBarisClickable(Rows rows, String label, String nilai, final int tabIndex) {
		MyFormRow row = new MyFormRow();
		row.setStyle("cursor:pointer;");
		row.addEventListener("onClick", new EventListener() {
			/**
			 * Navigasi baris tabel Dasbor: memindahkan tab aktif ke {@code tabIndex} saat baris diklik.
			 *
			 * @param e event {@code onClick} pada baris grid; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event e) throws Exception {
				btnTab.pilih(tabIndex);
			}
		});
		row.setParent(rows);
		Label lbl = new Label(label);
		lbl.setStyle("font-weight:bold;color:#0070c0;white-space:nowrap;padding-right:8px;text-decoration:underline dotted;");
		lbl.setParent(row);
		new Label(nilai).setParent(row);
	}

	/**
	 * Membangun tampilan mobile: TIDAK memakai tab button seperti desktop — hanya me-render
	 * konten satu tab yang sesuai {@code index} langsung ke {@code div} (indeks 0=Kehadiran,
	 * 1=Pembelajaran, 2=Materi, 3=Tugas, 4=Audio, 5=Video, 6=Ujian, 7=Diskusi; tidak ada mode
	 * mobile untuk Dasbor/8=Hasil-Evaluasi). Percabangan Kehadiran/Ujian mengikuti pola yang sama
	 * dengan {@link #tampilanDesktop}: pilih helper siswa vs akademik berdasarkan asal pertemuan.
	 * Berisi {@code System.out.println} debug (```index = ...```) yang tersisa dari
	 * pengembangan — bukan bagian dari kontrak, tidak dihapus di sini agar tidak mengubah
	 * perilaku existing.
	 *
	 * @param div Center tempat konten tab tunggal dirender.
	 */
	private void tampilanMobile(Center div) throws Exception {
		System.out.println("index = " + index);
		if (index == 0) {
			if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null
					|| (pertemuan.getFormulirKegiatan() != null
							&& pertemuan.getFormulirKegiatan().getSekolah() != null)) {
				absensiSiswaHelper.mainInit(pertemuan, div, true);
			} else {
				absensiHelper.mainInit(pertemuan, div, true);
			}
		} else if (index == 1) {
			div.appendChild(initCatatan(true));
		} else if (index == 2) {
			filePerkuliahanHelper.createFile(pertemuan, null, null, null, Common.tampilanScroll1(div),
					selectedPertemuanFileContent);
		} else if (index == 3) {
			initTugas(div, true);
		} else if (index == 4) {
			audioPertemuanHelper.display(pertemuan, null, null, Common.tampilanScroll1(div), selectedAudioPertemuan);
		} else if (index == 5) {
			videoPertemuanHelper.display(pertemuan, null, null, Common.tampilanScroll1(div), selectedVideoPertemuan);
		} else if (index == 6) {

			if (pertemuan.getJadwalPelajaran() != null || pertemuan.getJadwalUjianPSB() != null) {
				pertemuanPunyaUjianSiswaHelper.display(pertemuan, Common.tampilanScroll1(div));
			} else {
				pertemuanPunyaUjianHelper.display(pertemuan, Common.tampilanScroll1(div));
			}

		} else if (index == 7) {
			pertemuanPunyaDiskusiHelper.display(pertemuan, div, true, selectedDiskusi);
		}
	}

	/**
	 * Menyusun kerangka layout window: {@link MyBorderlayout} dengan Center berisi tab (desktop
	 * atau mobile, dipilih via {@code Common.isMobile()}) dan South berisi toolbar tombol
	 * "Selesai" (visibilitasnya mengikuti {@code tampilSelesai}). Tombol "Selesai" memanggil
	 * {@code dataLoader.loadData(null)} (memberi tahu pemanggil untuk refresh data) lalu
	 * {@code window.detach()} untuk menutup modal.
	 *
	 * @throws Exception diteruskan dari pembangunan tab (query DB, render komponen ZK, dsb.).
	 */
	public void init() throws Exception {
		Borderlayout borderlayout = new MyBorderlayout(true);
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setBorder("none");

		if (Common.isMobile()) {
			tampilanMobile(center);
		} else {
			// Tab buttons di paling atas; info pertemuan menyusul di bawah
			// tab buttons (via tambahHeaderKonten) sehingga navigasi antar-tab
			// selalu mudah dijangkau tanpa diblokir info card.
			tampilanDesktop(center);
		}

		South south = new South();
		south.setVisible(tampilSelesai);
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Selesai", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Aksi tombol "Selesai": memberi tahu pemanggil untuk memuat ulang datanya
			 * ({@code dataLoader.loadData(null)}) lalu menutup modal dengan {@code window.detach()}.
			 *
			 * <p><b>Perhatian:</b> {@link #dataLoader} di-dereference tanpa cek {@code null}, sehingga
			 * pemanggil yang menampilkan tombol ini WAJIB meneruskan {@code dataLoader} bukan
			 * {@code null} pada {@code display}. Karena window di-{@code detach}, pemanggilan
			 * {@code display} berikutnya pada instance helper yang sama akan menemui {@link #window}
			 * yang sudah lepas dari page — jalur pakai-ulang window hanya benar selama window belum
			 * pernah ditutup lewat tombol ini.</p>
			 *
			 * @param event event {@code onClick} tombol; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				dataLoader.loadData(null);
				window.detach();
			}
		});
		cancel.setParent(toolbar);

	}

	/**
	 * Membangun tab Pembelajaran (catatan pertemuan + field RPS terkait). Mencatat kunjungan
	 * lewat {@code pertemuan.masukkanData("melihat_catatan")} sebagai side effect pertama.
	 *
	 * <p>Dua tata letak BERBEDA tergantung siapa yang melihat:</p>
	 * <ul>
	 * <li><b>Peserta sendiri</b> (mahasiswa/biodataCalonMahasiswa/pesertaKursus/siswa null semua,
	 * yaitu admin/dosen/guru): grid satu kolom lebar, {@code catatan} berupa {@link Textbox}
	 * yang bisa langsung diedit (autosave {@code onChange}). Bila {@code pertemuan.getPerkuliahan()
	 * != null}, ditambah field RPS penuh: Indikator Capaian, Metode Pembelajaran, Pengalaman
	 * Belajar, Waktu Pembelajaran, Tugas dan Penilaian — masing-masing {@link Textbox} beserta
	 * tombol "Generate ..." (untuk admin/dosen/guru saja) yang memanggil
	 * {@link AIGenerator#generateApa} dengan prompt berisi info pertemuan, dosen, dan mahasiswa,
	 * hasilnya diformat lewat {@code Wa.ubahKeBold(...)} lalu disimpan via
	 * {@code Common.refreshSaveOrUpdate(pertemuan)}.</li>
	 * <li><b>Bukan peserta sendiri</b> (dilihat mahasiswa/siswa/calon): grid dua kolom
	 * (label 15% + isi), field RPS ditampilkan READ-ONLY sebagai {@link Label}, dan
	 * {@code catatan} hanya bisa diedit bila {@code biodataCalonMahasiswa == null} (calon
	 * mahasiswa melihat versi read-only lewat {@code MyHtmlIframe}); URL di dalam catatan
	 * dikonversi jadi tautan {@code <a target="_blank">} lewat {@code Common.getUrls(...)}.</li>
	 * </ul>
	 *
	 * <p>Kedua cabang diakhiri dengan bar unduh/unggah lampiran (Catatan Perkuliahan, Laporan
	 * Hasil Pembelajaran) via {@link LampiranLain#createDownloadUploadFileLain}, dirapikan rata
	 * kiri oleh {@link #rataKiriToolbarLampiran(org.zkoss.zk.ui.Component)}.</p>
	 *
	 * @param tampilInfo bila {@code true}, sisipkan panel info pertemuan
	 *                   ({@link DashboardTimelinePertemuan#displayInfoPertemuan}) di baris
	 *                   pertama grid.
	 * @return {@link Borderlayout} siap di-{@code appendChild} ke panel tab Pembelajaran.
	 */
	/**
	 * Predikat peran: benar hanya untuk admin/dosen/guru yang login sebagai {@link #tbmuser}
	 * (bukan mahasiswa, siswa, peserta kursus, calon siswa, maupun calon mahasiswa). Dipakai
	 * untuk menggerbangi seluruh tombol AI "Generate ..." pada tab Pembelajaran
	 * ({@link #initCatatan(boolean)}) secara konsisten satu sama lain — sebelumnya tiap gerbang
	 * menyalin-tempel syarat ini sendiri-sendiri, salah satunya menulis {@code getSiswa()} tiga
	 * kali sekaligus lupa memeriksa {@code getPesertaKursus()}, sehingga peserta kursus tetap
	 * bisa menekan tombol Generate.
	 *
	 * @return {@code true} bila {@link #tbmuser} berperan admin/dosen/guru.
	 */
	private boolean bolehGenerateCatatanAi() {
		return tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
				&& tbmuser.getPesertaKursus() == null && tbmuser.getCalonSiswa() == null
				&& tbmuser.getBiodataCalonMahasiswa() == null;
	}

	@SuppressWarnings("deprecation")
	private Borderlayout initCatatan(boolean tampilInfo) throws Exception {
		if (pertemuan != null) {
			pertemuan.masukkanData("melihat_catatan");
		}
		Borderlayout myBorderlayout = new ais.ui.util.MyBorderlayout();

		Center myCenter = new Center();
		ais.ui.util.ZkCompat.setFlex(myCenter, true);
		myCenter.setParent(myBorderlayout);

		Grid grid = new Grid();
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setParent(myCenter);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();

		rows.setParent(grid);

		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null) {

			if (tampilInfo) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);

			new MyLabelBoldConfig("Catatan : ").setParent(row);

			row = new MyFormRow();
			row.setParent(rows);

			catatan = new Textbox();
			catatan.setValue(pertemuan.getCatatan());
			catatan.setRows(pertemuan.getPerkuliahan() == null ? 15 : 5);
			catatan.setWidth("90%");
			catatan.setParent(row);

			catatan.addEventListener("onChange", new EventListener() {

				/**
				 * Autosave field <b>Catatan</b> pada tampilan ADMIN/DOSEN/GURU: menyegarkan entity dari basis
				 * data ({@code Common.refresh}), menyalin isi textbox ke {@code pertemuan.setCatatan(...)},
				 * lalu menyimpan seketika ({@code Common.refreshSaveOrUpdate}). Tidak ada tombol simpan
				 * terpisah — setiap {@code onChange} (blur/enter) langsung menulis ke basis data.
				 *
				 * <p>{@code Common.refresh} dipanggil LEBIH DULU agar perubahan bidang lain oleh sesi lain
				 * tidak tertimpa oleh salinan entity yang basi; namun karena tidak ada penguncian, dua
				 * pengguna yang mengedit catatan pertemuan yang sama tetap saling menimpa (last-write-wins).</p>
				 *
				 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai (nilai diambil
				 *             langsung dari {@link #catatan}).
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.refresh(pertemuan);
					pertemuan.setCatatan(catatan.getValue());
					Common.refreshSaveOrUpdate(pertemuan);
				}
			});

			row = new MyFormRow();
			row.setParent(rows);

			new MyLabelAgakKecil(
					"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu")
					.setParent(row);

			row = new MyFormRow();
			row.setParent(rows);

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN,
					"Catatan", false, new EventListener() {

						/**
						 * Listener KOSONG yang diwajibkan tanda tangan
						 * {@link LampiranLain#createDownloadUploadFileLain} untuk bar lampiran "Catatan
						 * Perkuliahan" pada tampilan admin/dosen/guru. Tidak ada aksi lanjutan setelah unggah/unduh
						 * karena isi lampiran tidak memengaruhi komponen lain di layar ini.
						 *
						 * @param arg0 event dari utilitas lampiran; sengaja diabaikan.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, false);

			hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.LHP,
					pertemuan.getPerkuliahan() == null ? "Laporan Hasil" : LampiranLain.LHP, false,
					new EventListener() {

						/**
						 * Listener KOSONG untuk bar lampiran "Laporan Hasil Pembelajaran" (LHP) pada tampilan
						 * admin/dosen/guru — lihat catatan pada listener lampiran Catatan di atas.
						 *
						 * @param arg0 event dari utilitas lampiran; sengaja diabaikan.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true, null, false, false);

			if (bolehGenerateCatatanAi()) {
				MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Catatan",
						"/img/svg/gear.svg");
				toolbarbutton.setParent(hbox);

				String d = "";
				for (Dosen dosen : pertemuan.ambilDosen()) {
					d += d.isEmpty() ? dosen.getNama() : " dan " + dosen.getNama();
				}

				String p = "";
				for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa()) {
					p += p.isEmpty() ? mahasiswa.getNama() : " dan " + mahasiswa.getNama();
				}

				String tanya = "Buatkan catatan dan berita acara untuk " + pertemuan.info() + ", hari dan tanggal : "
						+ Common.dateFormat6.get().format(pertemuan.getTanggal()) + ", jam waktu mulai "
						+ pertemuan.getWaktuMulai() + " sampai " + pertemuan.getWaktuSelesai() + ", Ruangan : "
						+ (pertemuan.getRuang() == null ? "Belum ditentukan" : pertemuan.getRuang().getNama())
						+ ", Gedung : "
						+ (pertemuan.getRuang() == null || pertemuan.getRuang().getGedung() == null ? ""
								: pertemuan.getRuang().getGedung().getNama())
						+ ", Pengajar : " + d + ", Topik : " + pertemuan.getTopik() + ", Peserta : " + p;

				String tanyaAkhiran = "";
				String tanyaMengajar = " apa saja";
				if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
					tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
					tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama() + "\"";
				} else if (pertemuan.getJadwalPelajaran() != null
						&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
					tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
					tanyaAkhiran = " pada matapelajaran \""
							+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
				}
				toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Catatan",
						"Informasikan tentang pertemuan kali ini", tanya, false, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
								.getNilai().trim(),
						null, new EventListener() {

							/**
							 * Callback HASIL AKHIR tombol "Generate Catatan" ({@link AIGenerator#generateApa}): mengambil
							 * teks jawaban model dari {@code arg0.getData()}, mengubah penanda tebal gaya WhatsApp menjadi
							 * HTML ({@code Wa.ubahKeBold}), mengganti newline dengan {@code <br>}, menaruhnya di textbox
							 * {@link #catatan}, lalu MENYIMPANNYA ke {@code pertemuan}. Inilah callback yang menulis ke
							 * basis data; pasangannya di bawah hanya memperbarui tampilan.
							 *
							 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
							 */
							@Override
							public void onEvent(Event arg0) throws Exception {

								catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));

								Common.refresh(pertemuan);
								pertemuan.setCatatan(catatan.getValue());
								Common.refreshSaveOrUpdate(pertemuan);

							}
						}, tanyaMengajar, new EventListener() {

							/**
							 * Callback PRATINJAU tombol "Generate Catatan": memformat dan menaruh teks hasil ke textbox
							 * {@link #catatan} TANPA menyimpan ke basis data — dipakai untuk menampilkan hasil sementara
							 * selagi pengguna masih bisa membatalkan/mengulang generasi.
							 *
							 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
							 */
							@Override
							public void onEvent(Event arg0) throws Exception {
								catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
										.replaceAll("\n", "<br>"));
							}
						}));

			}

			// Tombol Catatan (Ubah / Upload Catatan / Generate Catatan) dibuat RATA KIRI.
			rataKiriToolbarLampiran(hbox);

			if (pertemuan.getPerkuliahan() != null) {

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Indikator Capaian : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox indikator = new Textbox(pertemuan.getIndikator());
				indikator.setRows(3);
				indikator.setWidth("90%");
				indikator.setParent(row);

				indikator.addEventListener("onChange", new EventListener() {

					/**
					 * Autosave field RPS <b>Indikator Capaian</b> ({@code pertemuan.setIndikator}). Field ini
					 * hanya dibangun bila {@code pertemuan.getPerkuliahan() != null}, yaitu untuk pertemuan
					 * perkuliahan yang punya Rencana Pembelajaran Semester. Pola sama dengan autosave Catatan:
					 * {@code refresh} lalu {@code refreshSaveOrUpdate}, tanpa tombol simpan.
					 *
					 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setIndikator(indikator.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: Mahasiswa mampu menjelaskan dan mendiskusikan ....").setParent(row);

				if (bolehGenerateCatatanAi()) {

					row = new MyFormRow();
					row.setParent(rows);

					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Indikator Capaian",
							"/img/svg/gear.svg");
					toolbarbutton.setParent(row);

					String tanya = "Buatkan Indikator Capaian untuk \"" + pertemuan.info() + "\"";

					String tanyaAkhiran = "";
					String tanyaMengajar = " apa saja";
					if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
						tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
						tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
								+ "\"";
					} else if (pertemuan.getJadwalPelajaran() != null
							&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
						tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
						tanyaAkhiran = " pada matapelajaran \""
								+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
					}
					toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Indikator",
							"Indikator tentang apa ?", tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								/**
								 * Callback HASIL AKHIR tombol "Generate Indikator Capaian": memformat teks hasil model dan
								 * MENYIMPANNYA ke {@code pertemuan.setIndikator}.
								 *
								 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

									indikator.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));

									Common.refresh(pertemuan);
									pertemuan.setIndikator(indikator.getValue());
									Common.refreshSaveOrUpdate(pertemuan);

								}
							}, tanyaMengajar, new EventListener() {

								/**
								 * Callback PRATINJAU tombol "Generate Indikator Capaian": hanya memperbarui textbox, tidak
								 * menyimpan.
								 *
								 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									indikator.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));

				}

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Metode Pembelajaran : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox metodePembelajaran = new Textbox(pertemuan.getMetodePembelajaran());
				metodePembelajaran.setRows(3);
				metodePembelajaran.setWidth("90%");
				metodePembelajaran.setParent(row);

				metodePembelajaran.addEventListener("onChange", new EventListener() {

					/**
					 * Autosave field RPS <b>Metode Pembelajaran</b> ({@code pertemuan.setMetodePembelajaran}).
					 *
					 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
						&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null
						&& tbmuser.getBiodataCalonMahasiswa() == null) {

					row = new MyFormRow();
					row.setParent(rows);

					MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Metode Pembelajaran",
							"/img/svg/gear.svg");
					toolbarbutton.setParent(row);

					String tanya = "Buatkan Metode Pembelajaran untuk \"" + pertemuan.info() + "\"";

					String tanyaAkhiran = "";
					String tanyaMengajar = " apa saja";
					if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
						tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
						tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
								+ "\"";
					} else if (pertemuan.getJadwalPelajaran() != null
							&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
						tanyaMengajar = " matapelajaran " + pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
						tanyaAkhiran = " pada matapelajaran \""
								+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
					}
					toolbarbutton.addEventListener("onClick", AIGenerator.generateApa("Generate Metode Pembelajaran",
							"Metode Pembelajaran tentang apa ?", tanya, true, tanyaAkhiran,
							Common.getKonfigurasi("llama_system_catatan", "Kamu adalah Pengajar atau Dosen atau Guru ")
									.getNilai().trim(),
							null, new EventListener() {

								/**
								 * Callback HASIL AKHIR tombol "Generate Metode Pembelajaran": memformat teks hasil model dan
								 * MENYIMPANNYA ke {@code pertemuan.setMetodePembelajaran}.
								 *
								 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

									metodePembelajaran.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));

									Common.refresh(pertemuan);
									pertemuan.setMetodePembelajaran(metodePembelajaran.getValue());
									Common.refreshSaveOrUpdate(pertemuan);

								}
							}, tanyaMengajar, new EventListener() {

								/**
								 * Callback PRATINJAU tombol "Generate Metode Pembelajaran": hanya memperbarui textbox, tidak
								 * menyimpan.
								 *
								 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									metodePembelajaran.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
											.replaceAll("\n", "<br>"));
								}
							}));

				}

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Pengalaman Belajar : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox pengalamanBelajar = new Textbox(pertemuan.getPengalamanBelajar());
				pengalamanBelajar.setRows(3);
				pengalamanBelajar.setWidth("90%");
				pengalamanBelajar.setParent(row);

				pengalamanBelajar.addEventListener("onChange", new EventListener() {

					/**
					 * Autosave field RPS <b>Pengalaman Belajar</b> ({@code pertemuan.setPengalamanBelajar}).
					 * Berbeda dari Catatan/Indikator/Metode Pembelajaran, field ini TIDAK punya tombol
					 * "Generate ..." berbantuan AI.
					 *
					 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setPengalamanBelajar(pengalamanBelajar.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: Menyimak, Mengamati, Mendiskusikan, dan Menjawab soal").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Waktu Pembelajaran : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox waktupembelajaran = new Textbox(pertemuan.getWaktupembelajaran());
				waktupembelajaran.setRows(1);
				waktupembelajaran.setWidth("90%");
				waktupembelajaran.setParent(row);

				waktupembelajaran.addEventListener("onChange", new EventListener() {

					/**
					 * Autosave field RPS <b>Waktu Pembelajaran</b> ({@code pertemuan.setWaktupembelajaran}) —
					 * teks bebas satu baris, mis. "2 x 50 menit". Tidak punya tombol "Generate ...".
					 *
					 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setWaktupembelajaran(waktupembelajaran.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil("*) Contoh: 2 x 50 menit").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Tugas dan Penilaian : ").setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				final Textbox tugasDanPenilaian = new Textbox(pertemuan.getTugasDanPenilaian());
				tugasDanPenilaian.setRows(3);
				tugasDanPenilaian.setWidth("90%");
				tugasDanPenilaian.setParent(row);

				tugasDanPenilaian.addEventListener("onChange", new EventListener() {

					/**
					 * Autosave field RPS <b>Tugas dan Penilaian</b> ({@code pertemuan.setTugasDanPenilaian}) —
					 * field RPS terakhir; tidak punya tombol "Generate ...".
					 *
					 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.refresh(pertemuan);
						pertemuan.setTugasDanPenilaian(tugasDanPenilaian.getValue());
						Common.refreshSaveOrUpdate(pertemuan);
					}
				});

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelAgakKecil(
						"*) Contoh: Ketepatan menjelaskan...., Ketepatan menyebutkan..., dan lain sebagainya")
						.setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

			}

		} else {

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("15%");

			column = new MyColumnConfig();
			column.setParent(columns);

			if (tampilInfo) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setValign("top");
				row.setParent(rows);
				ais.ui.util.ZkCompat.setSpans(row, "2");
				row.appendChild(DashboardTimelinePertemuan.displayInfoPertemuan(pertemuan));

			}

			List<String> urls = Common.getUrls(pertemuan.getCatatan());
			String catat = pertemuan.getCatatan();
			catat = catat.replaceAll("\n", "<br>");
			for (String url : urls) {
				catat = org.apache.commons.lang3.StringUtils.replace(catat, url,
						"<a href='" + url + "' target='_blank'>" + url + "</a>");
			}

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);

			new MyLabelBoldConfig("Catatan").setParent(row);
			if (pertemuan.getPerkuliahan() != null) {
				new ais.ui.util.MyHtmlIframe(catat).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Kemampuan akhir pembelajaran").setParent(row);
				new Label(pertemuan.getTopik()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Kriteria,Indikator&Bobot penilaian").setParent(row);
				new Label(pertemuan.getIndikator()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Metode Pembelajaran").setParent(row);
				new Label(pertemuan.getMetodePembelajaran()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Pengalaman Belajar").setParent(row);
				new Label(pertemuan.getPengalamanBelajar()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Waktu Pembelajaran").setParent(row);
				new Label(pertemuan.getWaktupembelajaran()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

				new MyLabelBoldConfig("Tugas dan Penilaian").setParent(row);
				new Label(pertemuan.getTugasDanPenilaian()).setParent(row);

				row = new MyFormRow();
				row.setParent(rows);

			} else {
				if (biodataCalonMahasiswa != null) {
					new ais.ui.util.MyHtmlIframe(catat).setParent(row);
				} else {
					catatan = new Textbox();
					catatan.setValue(pertemuan.getCatatan());
					catatan.setRows(15);
					catatan.setWidth("90%");
					catatan.setParent(row);

					catatan.addEventListener("onChange", new EventListener() {

						/**
						 * Autosave field <b>Catatan</b> pada tampilan PESERTA (cabang {@code else} dari pemeriksaan
						 * peran di awal {@link #initCatatan(boolean)}). Sama persis dengan autosave versi
						 * admin/dosen/guru: menyegarkan entity, menyalin isi textbox ke
						 * {@code pertemuan.setCatatan(...)}, lalu menyimpan seketika ke basis data.
						 *
						 * <p><b>Cakupan peran yang perlu diperhatikan.</b> Textbox yang memasang listener ini hanya
						 * dibangun ketika DUA syarat terpenuhi: (a) {@code pertemuan.getPerkuliahan() == null} —
						 * yakni pertemuan berasal dari jadwal pelajaran sekolah, KKN, PKL, atau formulir kegiatan,
						 * bukan perkuliahan; dan (b) {@code biodataCalonMahasiswa == null}. Untuk pertemuan
						 * perkuliahan seluruh isi RPS dan catatan dirender READ-ONLY ({@code MyHtmlIframe} +
						 * {@link Label}), dan calon mahasiswa selalu read-only. Di luar dua kasus itu — mahasiswa,
						 * siswa, dan peserta kursus pada pertemuan non-perkuliahan — catatan/berita acara pertemuan
						 * dapat disunting dan langsung tersimpan oleh peserta, tanpa jejak audit tersendiri di kelas
						 * ini. Perilaku ini didokumentasikan apa adanya; penilaian apakah ini disengaja (catatan
						 * bersama) atau kelalaian gerbang peran dilacak terpisah dan tidak diubah di sini.</p>
						 *
						 * @param arg0 event {@code onChange} dari textbox; isinya tidak dipakai.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.refresh(pertemuan);
							pertemuan.setCatatan(catatan.getValue());
							Common.refreshSaveOrUpdate(pertemuan);
						}
					});

					row = new MyFormRow();
					row.setParent(rows);

					new MyLabelAgakKecil(
							"*) Catatan juga bisa berisi link atau URL yang mengarah ke website, audio, video, atau file tertentu")
							.setParent(row);

					if (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getSiswa() == null && tbmuser.getSiswa() == null
							&& tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null) {

						row = new MyFormRow();
						row.setParent(rows);

						MyToolbarbuttonConfig toolbarbutton = new MyToolbarbuttonConfig("Generate Catatan",
								"/img/svg/gear.svg");
						toolbarbutton.setParent(row);

						String d = "";
						for (Dosen dosen : pertemuan.ambilDosen()) {
							d += d.isEmpty() ? dosen.getNama() : " dan " + dosen.getNama();
						}

						String p = "";
						for (Mahasiswa mahasiswa : pertemuan.ambilMahasiswa()) {
							p += p.isEmpty() ? mahasiswa.getNama() : " dan " + mahasiswa.getNama();
						}

						String tanya = "Buatkan catatan dan berita acara untuk " + pertemuan.info()
								+ ", hari dan tanggal : " + Common.dateFormat6.get().format(pertemuan.getTanggal())
								+ ", jam waktu mulai " + pertemuan.getWaktuMulai() + " sampai "
								+ pertemuan.getWaktuSelesai() + ", Ruangan : "
								+ (pertemuan.getRuang() == null ? "Belum ditentukan" : pertemuan.getRuang().getNama())
								+ ", Gedung : "
								+ (pertemuan.getRuang() == null || pertemuan.getRuang().getGedung() == null ? ""
										: pertemuan.getRuang().getGedung().getNama())
								+ ", Pengajar : " + d + ", Topik : " + pertemuan.getTopik() + ", Peserta : " + p;

						String tanyaAkhiran = "";
						String tanyaMengajar = " apa saja";
						if (pertemuan.getPerkuliahan() != null && pertemuan.getPerkuliahan().getMatakuliah() != null) {
							tanyaMengajar = " matakuliah " + pertemuan.getPerkuliahan().getMatakuliah().getNama();
							tanyaAkhiran = " pada matakuliah \"" + pertemuan.getPerkuliahan().getMatakuliah().getNama()
									+ "\"";
						} else if (pertemuan.getJadwalPelajaran() != null
								&& pertemuan.getJadwalPelajaran().getMatapelajaran() != null) {
							tanyaMengajar = " matapelajaran "
									+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama();
							tanyaAkhiran = " pada matapelajaran \""
									+ pertemuan.getJadwalPelajaran().getMatapelajaran().getNama() + "\"";
						}
						toolbarbutton.addEventListener("onClick",
								AIGenerator.generateApa("Generate Catatan", "Informasikan tentang pertemuan kali ini",
										tanya, false, tanyaAkhiran,
										Common.getKonfigurasi("llama_system_catatan",
												"Kamu adalah Pengajar atau Dosen atau Guru ").getNilai().trim(),
										null, new EventListener() {

											/**
											 * Callback HASIL AKHIR tombol "Generate Catatan" pada cabang PESERTA: memformat teks hasil
											 * model dan MENYIMPANNYA ke {@code pertemuan.setCatatan}.
											 *
											 * <p>Tombol pemicunya dibangun di balik syarat {@code tbmuser.getMahasiswa() == null &&
											 * tbmuser.getSiswa() == null && tbmuser.getSiswa() == null && tbmuser.getSiswa() == null &&
											 * tbmuser.getCalonSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null}. Perhatikan
											 * {@code getSiswa()} tertulis TIGA KALI sementara {@code getPesertaKursus()} — yang ikut
											 * diperiksa pada percabangan peran utama di awal method — tidak diperiksa sama sekali; pola
											 * salin-tempel yang sama muncul pada seluruh gerbang tombol "Generate ..." di kelas ini.
											 * Dicatat apa adanya, tidak diubah di sini.</p>
											 *
											 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
											 */
											@Override
											public void onEvent(Event arg0) throws Exception {

												catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
														.replaceAll("\n", "<br>"));

												Common.refresh(pertemuan);
												pertemuan.setCatatan(catatan.getValue());
												Common.refreshSaveOrUpdate(pertemuan);

											}
										}, tanyaMengajar, new EventListener() {

											/**
											 * Callback PRATINJAU tombol "Generate Catatan" pada cabang PESERTA: hanya memperbarui textbox
											 * {@link #catatan}, tidak menyimpan ke basis data.
											 *
											 * @param arg0 event yang membawa teks hasil generasi pada {@code getData()}.
											 */
											@Override
											public void onEvent(Event arg0) throws Exception {
												catatan.setValue(ais.action.servlet.Wa.ubahKeBold((arg0.getData() + ""))
														.replaceAll("\n", "<br>"));
											}
										}));

					}
				}

			}

			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.CATATAN_PERKULIAHAN,
					"Catatan", false, new EventListener() {

						/**
						 * Listener KOSONG untuk bar lampiran "Catatan Perkuliahan" pada cabang PESERTA. Berbeda dari
						 * versi admin/dosen/guru, {@code createDownloadUploadFileLain} di sini dipanggil dengan
						 * argumen izin unggah bernilai {@code false} sehingga peserta hanya dapat mengunduh.
						 *
						 * @param arg0 event dari utilitas lampiran; sengaja diabaikan.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, false);

			hbox1 = new Hbox();
			hbox1.setParent(hbox);
			LampiranLain.createDownloadUploadFileLain(hbox1, pertemuan.getId(), LampiranLain.LHP,
					pertemuan.getPerkuliahan() == null ? "Laporan Hasil" : LampiranLain.LHP, false,
					new EventListener() {

						/**
						 * Listener KOSONG untuk bar lampiran "Laporan Hasil Pembelajaran" (LHP) pada cabang PESERTA;
						 * juga hanya-unduh — lihat catatan pada listener lampiran Catatan di atas.
						 *
						 * @param arg0 event dari utilitas lampiran; sengaja diabaikan.
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, false, null, false, false);

			// Tombol Catatan dibuat RATA KIRI (samakan dengan tampilan dosen/admin).
			rataKiriToolbarLampiran(hbox);

		}

		return myBorderlayout;
	}

	/**
	 * Membuat deretan tombol lampiran (hasil {@code createDownloadUploadFileLain}) menjadi
	 * <b>rata kiri</b>. Utilitas bersama {@code FileFotoLain.createDownloadUpload} membungkus
	 * tombol dalam Vbox/Hbox ber-{@code hflex="1"} (melebar penuh); akibatnya, bila tombolnya
	 * lebih dari satu — mis. bar Catatan: Ubah, Upload Catatan, Generate Catatan — ZK menyebar
	 * tombol ke sisi kiri, tengah, dan kanan (tampak "rata kanan-kiri"). Metode ini menelusuri
	 * seluruh komponen di bawah {@code akar} lalu MENGHAPUS {@code hflex} sehingga tiap wadah
	 * kembali selebar isinya dan tombol menempel rapat di kiri.
	 *
	 * <p>Perubahan hanya menyentuh properti tata letak ({@code hflex}); tidak mengubah data,
	 * event, maupun perilaku komponen berbagi-pakai lain — jadi aman dan bersifat lokal untuk
	 * halaman ini (tidak ikut mengubah halaman lain yang memakai createDownloadUpload).
	 *
	 * @param akar wadah terluar bar tombol (mis. {@code hbox} pada bagian Catatan).
	 */
	private static void rataKiriToolbarLampiran(org.zkoss.zk.ui.Component akar) {
		if (akar == null) {
			return;
		}
		try {
			if (akar instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
				((org.zkoss.zk.ui.HtmlBasedComponent) akar).setHflex(null);
			}
			java.util.List<?> anak = akar.getChildren();
			for (int i = 0; i < anak.size(); i++) {
				Object c = anak.get(i);
				if (c instanceof org.zkoss.zk.ui.Component) {
					rataKiriToolbarLampiran((org.zkoss.zk.ui.Component) c);
				}
			}
		} catch (Exception abaikan) { ais.common.ErrorAuditUtil.record(abaikan, "auto-audit(empty-catch) src/ais/action/master/helper/PertemuanHelper.java:1416");
		}
	}

	/**
	 * Titik masuk utama: membuka window pertemuan dengan tab {@code index} aktif, tanpa
	 * preselect item apa pun. Lihat overload penuh untuk detail perilaku.
	 *
	 * @param pertemuan  entity pertemuan yang akan ditampilkan.
	 * @param dataLoader dipanggil dengan {@code null} saat window ditutup lewat "Selesai", sinyal
	 *                   bagi pemanggil untuk memuat ulang data (mis. grid daftar pertemuan).
	 * @param index      indeks tab yang aktif saat window dibuka (lihat daftar tab di Javadoc kelas).
	 */
	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, null, null);
	}

	/** Sama seperti {@link #display(Pertemuan, DataLoader, int)}, plus preselect satu {@link PertemuanFileContent} di tab Materi. */
	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			PertemuanFileContent pertemuanFileContent) throws Exception {
		display(pertemuan, dataLoader, index, null, null, pertemuanFileContent, null, null);
	}

	/** Sama seperti {@link #display(Pertemuan, DataLoader, int)}, plus preselect satu {@link AudioPertemuan} di tab Audio. */
	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			AudioPertemuan audioPertemuan) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, audioPertemuan, null);
	}

	/** Sama seperti {@link #display(Pertemuan, DataLoader, int)}, plus preselect satu {@link VideoPertemuan} di tab Video. */
	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			VideoPertemuan videoPertemuan) throws Exception {
		display(pertemuan, dataLoader, index, null, null, null, null, videoPertemuan);
	}

	/**
	 * Overload penuh: menyimpan seluruh state preselect ke field instance, lalu membungkus
	 * pembukaan window dalam {@code Common.createDefaultTimer(...)} (memastikan eksekusi berjalan
	 * pada siklus event ZK yang tepat, bukan langsung inline). Di dalam timer: pindahkan
	 * {@code index} ke field instance, kosongkan window lama bila sedang dipakai ulang
	 * ({@code Common.clear(window)}), buat {@link MyWindow} baru bila belum ada (ukuran 99% x
	 * 99%, ditempel ke root page saat ini via {@code ExecutionsCtrl}), set field
	 * {@code dataLoader}/{@code pertemuan}, panggil {@link #init()} untuk membangun isi window,
	 * lalu tampilkan sebagai modal ({@code setVisible(true)} + {@code window.onModal()}).
	 *
	 * @param pertemuan               entity pertemuan yang akan ditampilkan.
	 * @param dataLoader              callback refresh saat window ditutup.
	 * @param index                   indeks tab aktif awal.
	 * @param selectedTugasPertemuan  tugas individu yang otomatis dibuka di tab Tugas, atau {@code null}.
	 * @param selectedTugasKelompok   tugas kelompok yang otomatis dibuka di tab Tugas, atau {@code null}.
	 * @param pertemuanFileContent    materi yang otomatis disorot di tab Materi, atau {@code null}.
	 * @param audioPertemuan          audio yang otomatis disorot di tab Audio, atau {@code null}.
	 * @param videoPertemuan          video yang otomatis disorot di tab Video, atau {@code null}.
	 */
	public void display(final Pertemuan pertemuan, final DataLoader dataLoader, final int index,
			TugasPertemuan selectedTugasPertemuan, TugasKelompok selectedTugasKelompok,
			PertemuanFileContent pertemuanFileContent, AudioPertemuan audioPertemuan, VideoPertemuan videoPertemuan)
			throws Exception {
		this.selectedTugasPertemuan = selectedTugasPertemuan;
		this.selectedTugasKelompok = selectedTugasKelompok;
		this.selectedPertemuanFileContent = pertemuanFileContent;
		this.selectedAudioPertemuan = audioPertemuan;
		this.selectedVideoPertemuan = videoPertemuan;

		Common.createDefaultTimer(new EventListener() {

			/**
			 * Badan timer pembuka window (dijalankan {@code Common.createDefaultTimer} pada siklus event
			 * ZK berikutnya, bukan inline). Urutannya: salin {@code index} ke field instance; bersihkan
			 * isi {@link #window} bila sedang dipakai ulang; buat {@link MyWindow} baru 99% x 99% dan
			 * tempelkan ke root page saat ini bila belum ada; isi field {@link #dataLoader} dan
			 * {@link #pertemuan}; panggil {@link #init()} untuk membangun seluruh tab; terakhir tampilkan
			 * sebagai modal ({@code setVisible(true)} + {@code onModal()}).
			 *
			 * <p>Penundaan lewat timer inilah yang menyebabkan seluruh field state instance
			 * ({@code pertemuan}, {@code dataLoader}, {@code index}) masih bernilai lama tepat setelah
			 * {@code display} kembali — pemanggil tidak boleh mengandalkan isinya secara sinkron.</p>
			 *
			 * @param arg0 event timer; isinya tidak dipakai.
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				PertemuanHelper.this.index = index;

				if (window != null) {

					Common.clear(window);

				}

				if (window == null) {
					window = new MyWindow();
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
					window.setWidth("99%");
					window.setHeight("99%");
				}

				PertemuanHelper.this.dataLoader = dataLoader;

				PertemuanHelper.this.pertemuan = pertemuan;
				init();
				PertemuanHelper.this.window.setVisible(true);

				PertemuanHelper.this.window.onModal();
			}
		});
	}

}
