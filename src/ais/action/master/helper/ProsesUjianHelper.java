package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.BankSoalAction;
import ais.action.master.SertifikatAction;
import ais.action.master.SyaratUjianAction;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BankSoal;
import ais.database.model.BankSoalDetail;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.HasilUjianMahasiswaDetail;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PenjelasanBankSoal;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.Ujian;
import ais.database.model.UjianPunyaSoal;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyArrayList;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyHashMap;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBoldMerah;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

import org.zkoss.zul.Html;

/**
 * <h3>ProsesUjianHelper — Jendela Interaktif Pelaksanaan Ujian Online (CBT)</h3>
 *
 * <p><b>Untuk apa:</b> Kelas ini adalah komponen utama yang mengelola seluruh alur ujian
 * berbasis komputer (Computer-Based Test / CBT) di platform e-Learning ECAMPUS. Ia menangani
 * tampilan soal satu per satu atau berkelompok, pencatatan jawaban peserta secara real-time,
 * perhitungan skor (pilihan ganda, multi-choice, benar-salah, esai, dan OBE), pembatasan
 * waktu ujian, fitur anti-curang (Mode Pengawasan CBT), serta proses finalisasi hasil ujian
 * ketika peserta menekan tombol "Akhiri Ujian" atau waktu habis.</p>
 *
 * <p>Kelas ini dipakai untuk empat tipe peserta berbeda sekaligus: Mahasiswa reguler,
 * Calon Mahasiswa baru (PMB), Siswa sekolah (PPDB), dan Calon Siswa (PPDB sekolah). Logika
 * percabangan berdasarkan kehadiran objek entitas yang bukan-null ({@code mahasiswa},
 * {@code biodataCalonMahasiswa}, {@code siswa}, {@code calonSiswa}) menentukan jalur yang
 * diambil. Jika semua null, jendela beroperasi dalam mode preview (dosen/admin melihat ujian
 * tanpa menyimpan hasil).</p>
 *
 * <p><b>Cara kerja (alur utama):</b></p>
 * <ol>
 *   <li>Titik masuk eksternal: {@link #ikut} menampilkan tata tertib ujian dan tombol konfirmasi,
 *       atau {@link #tampil} langsung membuka jendela CBT (bila tata tertib tidak diperlukan).</li>
 *   <li>Konstruktor privat {@link #ProsesUjianHelper(PertemuanPunyaUjian,HasilUjianMahasiswa,
 *       Integer,Boolean,boolean,EventListener)} memanggil {@link #init()} yang memeriksa syarat
 *       ujian (presensi, akreditasi, dll.) lalu menampilkan loading-bar sambil mempersiapkan
 *       soal di thread terpisah.</li>
 *   <li>{@link #initSoal(Label)} di thread latar memanggil
 *       {@link #randomPosisiton(List,boolean,Label,Integer)} untuk mengacak / memilih soal,
 *       kemudian {@link #initHasilUjian(Label)} memastikan setiap {@code UjianPunyaSoal} memiliki
 *       baris {@code HasilUjianMahasiswaDetail} di database sebelum soal ditampilkan.</li>
 *   <li>Setelah persiapan selesai, {@link #prosesProsesUjian()} membangun antarmuka CBT
 *       lengkap: panel nomor soal (kiri), area soal (tengah), dan timer countdown (atas).</li>
 *   <li>Setiap perpindahan soal ditangani oleh {@link #doProcessUjian(int,boolean)} yang
 *       me-render ulang konten soal tanpa menutup jendela.</li>
 *   <li>Ketika peserta menekan "Akhiri Ujian" atau waktu habis, {@link #onSelesai()} memanggil
 *       {@link #generateHasilUjian(List,HasilUjianMahasiswa,PertemuanPunyaUjian,Map)} yang
 *       merangkum skor akhir, menyimpan ke database, lalu menutup jendela.</li>
 * </ol>
 *
 * <p><b>Perhitungan skor:</b> Tiga method statik publik membentuk pipeline perhitungan:</p>
 * <ul>
 *   <li>{@link #hitung(HasilUjianMahasiswaDetail,Map)} — skor satu soal (benar/salah/default).</li>
 *   <li>{@link #hitungPilihanGanda(HasilUjianMahasiswa,Map)} — agregasi skor seluruh soal
 *       pilihan ganda dan memanggil {@link #hitungWaktu}.</li>
 *   <li>{@link #hitungObe(HasilUjianMahasiswa,Map)} — pemetaan skor ke format penilaian OBE
 *       (hanya aktif bila kurikulum ujian menggunakan OBE).</li>
 * </ul>
 *
 * <p><b>Anti-curang (CBT Mode Pengawasan):</b> {@code buildCbtAntiCheatScript(sinkUuid)} menghasilkan
 * JavaScript yang diinjeksikan ke browser peserta. Script ini mendeteksi perpindahan tab,
 * Alt+Tab, keluar fullscreen, dan pintasan berbahaya. Setiap pelanggaran dicatat via ZK event
 * {@code onPelanggaran} → {@link #catatPelanggaranUjian(Long,String)} yang menyimpan ke kolom
 * {@code jumlah_pelanggaran} dan {@code log_pelanggaran} di {@code HasilUjianMahasiswa}. Jika
 * batas pelanggaran tercapai, ujian diselesaikan otomatis oleh script browser.</p>
 *
 * <p><b>Threading:</b> {@link #init()} memanfaatkan satu thread non-daemon anonim untuk
 * mempersiapkan soal ({@code initSoal}) agar UI tidak membeku. Thread ini menggunakan
 * {@code HibernateUtil.currentNativeSession()} (native ThreadLocal session) dengan
 * begin/commit eksplisit karena tidak berada dalam konteks request ZK. Class {@link Waktu}
 * (inner class) adalah thread daemon yang menghitung mundur waktu ujian setiap detik.</p>
 *
 * <p><b>Kuota ujian:</b> Field statik {@link #kuotaUjian} (HashSet) menjaga jumlah peserta
 * yang sedang aktif mengerjakan ujian secara bersamaan. Nilai batas kuota dibaca dari
 * konfigurasi {@code kuota_ujian} (default 120). Peserta yang masuk ketika kuota penuh akan
 * mendapat pesan dan jendela ditutup otomatis.</p>
 *
 * <p><b>Pemeliharaan:</b> Kelas ini sangat erat bergantung pada entitas Hibernate
 * {@code HasilUjianMahasiswa}, {@code HasilUjianMahasiswaDetail}, {@code UjianPunyaSoal},
 * dan {@code BankSoal}. Perubahan skema kolom pada entitas-entitas tersebut akan berdampak
 * langsung. Jika ingin menambah jenis soal baru, tambahkan cabang di {@link #hitung} dan
 * sesuaikan {@link #hitungPilihanGanda}. Logika anti-curang dikonfigurasi penuh lewat
 * {@code KonfigurasiNewAction} tab Elearning tanpa recompile.</p>
 *
 * <p><b>Posisi dalam siklus hidup ujian (hasil verifikasi terhadap seluruh pemanggil):</b>
 * kelas ini adalah lapis <i>PELAKSANAAN</i> — mesin yang benar-benar berjalan ketika peserta
 * sedang MENGERJAKAN ujian (render soal, simpan jawaban seketika tiap klik, hitung mundur
 * waktu, pengawasan anti-curang, finalisasi nilai). Ia berbeda dari dua lapis tetangganya:</p>
 * <ul>
 *   <li><b>Lapis SETUP/GERBANG</b> — {@code PertemuanPunyaUjianHelper} (serta padanan sekolah
 *       {@code PertemuanPunyaUjianSiswaHelper}): menjadwalkan ujian, menyiapkan soal, dan
 *       MENEGAKKAN jatah percobaan ({@code jumlahIkut < jumlahBolehIkut}) sebelum memanggil
 *       {@link #ikut} atau {@link #tampil}.</li>
 *   <li><b>Lapis HASIL/KOREKSI</b> — {@code HasilUjianMahasiswaHelper},
 *       {@code KoreksiHasilUjian}, {@code HasilUjianSiswaHelper},
 *       {@code RiwayatJawabanUjianHelper}, {@code RekapitulasiUjianHelper}, dan
 *       {@code HitungUlangNilaiObeHelper}: TIDAK membuka jendela CBT, melainkan memakai ULANG
 *       method statik penilaian milik kelas ini ({@link #hitung}, {@link #hitungPilihanGanda},
 *       {@link #hitungObe}, {@link #hitungWaktu}, {@link #rincianSkorSubCpmk}) untuk menghitung
 *       ulang nilai peserta.</li>
 * </ul>
 *
 * <p><b>PENTING — kelas ini BUKAN penegak jatah percobaan ujian.</b> {@link #ikut} hanya
 * MENAMPILKAN kalimat "Ujian ini hanya bisa dilakukan N kali. Saat ini anda telah mengikuti M
 * kali"; tidak ada pembandingan {@code jumlahIkut} vs {@code jumlahBolehIkut} yang memblokir.
 * {@link #tampil} — yang berstatus {@code public static} dan merupakan titik akhir pembuka
 * jendela ujian untuk SEMUA jalur — juga tidak memeriksa jatah sama sekali. Satu-satunya
 * gerbang jatah berada di {@code PertemuanPunyaUjianHelper} dan
 * {@code PertemuanPunyaUjianSiswaHelper} (percabangan yang memilih {@code ikut} vs
 * {@code tampil}). Konsekuensinya, setiap pemanggil BARU ke {@link #tampil} WAJIB menyalin
 * gerbang jatah tersebut, karena kelas ini tidak akan menolaknya.</p>
 *
 * <p><b>Gerbang yang MEMANG ada di kelas ini</b> (bukan jatah percobaan):</p>
 * <ol>
 *   <li>{@link #tampil} memblokir peserta yang ditandai "tidak perlu mengikuti" oleh dosen
 *       ({@code pertemuanPunyaUjian.mhsYgTidakIkut}) — hanya bila yang mengakses adalah
 *       peserta itu sendiri, bukan dosen/admin yang sedang melakukan pratinjau.</li>
 *   <li>{@link #tampil} menolak pembukaan ujian yang sama di perangkat kedua lewat
 *       {@link #SESI_AKTIF_UJIAN} (aktif hanya bila {@code antiCurangAktif} DAN
 *       {@code antiCurangLarangMultiDevice} bernilai benar).</li>
 *   <li>{@link #init()} memeriksa syarat ujian ({@code SyaratUjian}, presensi, dll.) dan
 *       kuota peserta bersamaan ({@link #kuotaUjian} vs konfigurasi {@code kuota_ujian}).</li>
 * </ol>
 *
 * @see PertemuanPunyaUjian
 * @see HasilUjianMahasiswa
 * @see HasilUjianMahasiswaDetail
 * @see UjianRecomputeUtil
 */
public class ProsesUjianHelper extends MyWindow {

	/** Map: HasilUjianMahasiswaId → ZK sessionId aktif. Digunakan untuk mencegah multi-device. */
	private static final java.util.concurrent.ConcurrentHashMap<Long, String> SESI_AKTIF_UJIAN
	    = new java.util.concurrent.ConcurrentHashMap<Long, String>();

	/**
	 * Halaman paging aktif (berbasis-nol) untuk daftar soal yang telah terjawab.
	 * Dipakai oleh {@link #reloadTelahDikerjakan} untuk menentukan batas offset tampilan.
	 */
	private int pagingTelahTerjawabActivePage = 0;

	/**
	 * Halaman paging aktif (berbasis-nol) untuk daftar soal yang belum dijawab.
	 * Dipakai oleh {@link #reloadBelumDikerjakan} untuk menentukan batas offset tampilan.
	 */
	private int pagingBelumTerjawabActivePage = 0;

	/**
	 * Set global (statik, shared seluruh JVM) yang menyimpan {@code keyhasil} peserta
	 * yang sedang aktif mengerjakan ujian. Dipakai sebagai pembatas kuota ujian bersamaan.
	 * Nilai ditambah saat ujian dimulai dan dihapus setelah {@link #onSelesai()} dipanggil.
	 * <p><b>Perhatian thread-safety:</b> HashSet ini tidak thread-safe; akses
	 * add/remove/size dilakukan di thread ZK event (single-threaded per request) sehingga
	 * pada praktiknya aman, namun perlu dikaji ulang jika diakses dari thread latar.</p>
	 */
	public static Set<String> kuotaUjian = new HashSet<String>();

	/**
	 * Penanda apakah sesi ujian ini adalah peserta BARU (tambah = true) atau peserta yang
	 * melanjutkan sesi yang sudah ada (tambah = false). Bila {@code true}, counter
	 * {@code jumlahIkut} pada {@code HasilUjianMahasiswa} akan dinaikkan satu saat ujian
	 * dimulai. Nilai ini diteruskan dari {@link #ikut} atau {@link #tampil}.
	 */
	private boolean tambah = true;

	/**
	 * Menampilkan jendela konfirmasi tata tertib ujian sebelum peserta masuk ke sesi ujian
	 * sesungguhnya. Jendela ini memuat tata tertib dari {@code PertemuanPunyaUjian} beserta
	 * informasi berapa kali peserta sudah mengikuti ujian ini, dan memeriksa syarat keikutsertaan.
	 *
	 * <p><b>Tujuan:</b> Memberikan peserta kesempatan membaca aturan ujian, memverifikasi bahwa
	 * semua syarat terpenuhi (presensi, nilai minimum, dll.), dan secara eksplisit menekan tombol
	 * "Ikuti Ujian Sekarang" sebagai bukti persetujuan. Jika peserta menekan "Batal", jendela
	 * ditutup tanpa membuka sesi ujian.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Membuat {@code MyWindow} modal berukuran 98% tinggi dan (500px desktop / 78% mobile).</li>
	 *   <li>Menampilkan teks tata tertib ujian (dari {@code Ujian.tatatertibUjian}) dengan ukuran
	 *       font diperbesar (9px → 16px agar lebih mudah dibaca), diikuti pesan berapa kali peserta
	 *       sudah mengikuti ujian dan batas yang diperbolehkan.</li>
	 *   <li>Jika peserta bukan mahasiswa/calon/kursus/siswa (artinya dosen/admin preview),
	 *       syarat ditampilkan bersifat read-write (dapat dicentang); selain itu ditampilkan
	 *       read-only karena sistem sudah menentukan kelayakan.</li>
	 *   <li>Tombol "Batal" menutup jendela (untuk pengguna tamu/calon juga membatalkan konfirmasi
	 *       close browser via {@code Clients.confirmClose(null)}).</li>
	 *   <li>Tombol "Ikuti Ujian Sekarang" memeriksa {@code syaratAlert} — jika ada syarat yang
	 *       belum terpenuhi, pesan peringatan ditampilkan dan ujian TIDAK dimulai. Jika semua
	 *       syarat aman, memanggil {@link #tampil} dan menutup jendela ini.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception yang dilempar diteruskan ke pemanggil karena anotasi
	 * {@code throws Exception}. ZK menangkap ini di level event handler dan menampilkan dialog
	 * error standar.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika ingin menambah informasi yang ditampilkan sebelum ujian dimulai
	 * (misal: kamera webcam, perekaman layar), titik yang tepat adalah setelah baris append tata
	 * tertib di dalam method ini. Jangan ubah alur tombol "Ikuti" karena ia meneruskan ke
	 * {@link #tampil} yang merupakan titik masuk konstruksi penuh CBT.</p>
	 *
	 * <p><b>Batas jatah percobaan TIDAK ditegakkan di sini.</b> Kalimat merah "Ujian ini hanya
	 * bisa dilakukan N kali..." bersifat semata-mata INFORMATIF: nilai
	 * {@code pertemuanPunyaUjian.getJumlahBolehIkut()} hanya dicetak ke layar, tidak pernah
	 * dibandingkan dengan {@code hasilUjianMahasiswa.getJumlahIkut()} untuk menonaktifkan tombol
	 * "Ikuti Ujian Sekarang". Pemeriksaan jatah dilakukan SEBELUM method ini dipanggil, oleh
	 * {@code PertemuanPunyaUjianHelper} (dan {@code PertemuanPunyaUjianSiswaHelper} untuk jalur
	 * sekolah). Jangan berasumsi method ini aman dipanggil langsung tanpa gerbang tersebut.</p>
	 *
	 * @param mahasiswa              entitas Mahasiswa peserta ujian; null untuk tipe peserta lain
	 * @param biodataCalonMahasiswa  entitas calon mahasiswa (PMB); null untuk tipe lain
	 * @param siswa                  entitas Siswa sekolah; null untuk tipe lain
	 * @param calonSiswa             entitas calon siswa (PPDB); null untuk tipe lain
	 * @param pertemuanPunyaUjian    relasi pertemuan-ujian yang menentukan konfigurasi ujian ini
	 * @param hasilUjianMahasiswa    objek hasil ujian yang sudah ada (bisa null bila ujian pertama)
	 * @param tambah                 true jika ini sesi baru (counter jumlahIkut akan dinaikkan)
	 * @param eventListener          callback yang dipanggil setelah ujian selesai (untuk refresh UI)
	 * @throws Exception jika terjadi error ZK atau Hibernate saat membangun jendela
	 */
	public static void ikut(final Mahasiswa mahasiswa, final BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Siswa siswa, final CalonSiswa calonSiswa, final PertemuanPunyaUjian pertemuanPunyaUjian,
			final HasilUjianMahasiswa hasilUjianMahasiswa, final boolean tambah, final EventListener eventListener)
			throws Exception {
		final MyWindow window = new MyWindow(pertemuanPunyaUjian.getNama(), "none", false);
		window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		// Di HP: jendela info/tata tertib ujian dibuat FULL SCREEN (100% x 100%) agar teks lega &
		// terbaca penuh, tidak sempit. Desktop tetap 500px x 98%.
		window.setHeight(Common.isMobile() ? "100%" : "98%");
		window.setWidth(!Common.isMobile() ? "500px" : "100%");

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setParent(borderlayout);

		Row r = Common.tampilanScroll(center);
		ais.ui.util.ZkCompat.setSpans(r, "2");

		r.appendChild(
				new ais.ui.util.MyHtml(pertemuanPunyaUjian.getUjian().getTatatertibUjian().replaceAll("9px", "16px")
						+ "<br><div style='font-size:20px;color:red'>Ujian ini hanya bisa dilakukan "
						+ pertemuanPunyaUjian.getJumlahBolehIkut() + " kali. Saat ini anda telah mengikuti "
						+ (hasilUjianMahasiswa == null ? 0 : hasilUjianMahasiswa.getJumlahIkut())
						+ " kali, kecuali seizin petugas ujian.</div>"));

		Rows rows = (Rows) r.getParent();

		MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

		Tbmuser tbmuser = Common.getCurrentUser();
		final Set<String> syaratAlert = new HashSet<String>();
		if (mahasiswa == null && biodataCalonMahasiswa == null && tbmuser.getPesertaKursus() == null
				&& tbmuser.getSiswa() == null && siswa == null && calonSiswa == null) {
			Tugas.tampilanSyarat(pertemuanPunyaUjian.getPertemuan(), null, pertemuanPunyaUjian.getUjian(), null, null,
					null, rows, syaratAlert, button);
		} else {
			Tugas.tampilanSyaratReadonly(pertemuanPunyaUjian.getPertemuan(), null, pertemuanPunyaUjian.getUjian(), null,
					null, null, rows, syaratAlert, button);

			Tugas.tampilanLain(pertemuanPunyaUjian.getPertemuan(), null, pertemuanPunyaUjian.getUjian(), null, null,
					null, rows, button);
		}

		// Jika ada syarat yang belum terpenuhi, tampilkan banner peringatan mencolok
		if (!syaratAlert.isEmpty()) {
			Row warnRow = new Row();
			warnRow.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(warnRow, "2");
			new ais.ui.util.MyHtml("<div style='background:#fef2f2;border:2px solid #ef4444;border-radius:10px;"
				+ "padding:14px 18px;margin:10px 0;display:flex;align-items:flex-start;gap:12px;'>"
				+ "<span style='font-size:28px;line-height:1;'>&#9888;</span>"
				+ "<div><div style='font-size:15px;font-weight:700;color:#b91c1c;margin-bottom:4px;'>"
				+ "Perhatian! Ada syarat yang belum terpenuhi</div>"
				+ "<div style='font-size:13px;color:#7f1d1d;'>"
				+ "Anda tidak dapat mengikuti ujian ini sebelum memenuhi semua syarat yang tertera di bawah."
				+ " Harap selesaikan terlebih dahulu sebelum melanjutkan.</div></div></div>")
				.setParent(warnRow);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Menutup jendela tata tertib TANPA memulai ujian ketika tombol "Batal" ditekan.
			 *
			 * <p>Untuk pengguna tamu / calon mahasiswa ({@code tbmuser} null atau memiliki
			 * {@code biodataCalonMahasiswa}), konfirmasi bawaan browser "yakin meninggalkan halaman?"
			 * dimatikan lebih dahulu lewat {@code Clients.confirmClose(null)} supaya penutupan tidak
			 * memunculkan dialog ganda. Setelah itu jendela dilepas dengan {@code window.detach()}.</p>
			 *
			 * <p>Tidak ada data yang ditulis ke basis data oleh listener ini — pembatalan pada tahap
			 * tata tertib tidak mengubah {@code jumlahIkut} maupun {@code HasilUjianMahasiswa}.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Batal"
			 * @throws Exception bila operasi ZK gagal (diteruskan ke kerangka kerja ZK)
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
					Clients.confirmClose(null);
				}

				window.detach();
			}
		});
		cancel.setParent(toolbar);

		MyToolbarbuttonConfig ikut = new MyToolbarbuttonConfig("Ikuti Ujian Sekarang", "/img/svg/check2.svg");
		ikut.setTooltiptext("Ikuti Ujian Sekarang");
		ikut.addEventListener("onClick", new EventListener() {
			/**
			 * Memulai sesi ujian ketika tombol "Ikuti Ujian Sekarang" ditekan.
			 *
			 * <p><b>Cara kerja:</b> bila {@code syaratAlert} tidak kosong (masih ada syarat ujian yang
			 * belum terpenuhi), seluruh pesan digabung menjadi satu teks multibaris, ditampilkan sebagai
			 * peringatan, dan proses DIBATALKAN (tidak ada jendela CBT yang dibuka). Bila semua syarat
			 * aman, {@link ProsesUjianHelper#tampil} dipanggil untuk membangun jendela CBT sesungguhnya,
			 * lalu jendela tata tertib ini dilepas.</p>
			 *
			 * <p><b>Catatan gerbang:</b> yang diperiksa di sini HANYA syarat ujian ({@code syaratAlert}),
			 * BUKAN jatah percobaan ujian. Lihat catatan pada {@link ProsesUjianHelper#ikut} dan pada
			 * Javadoc kelas mengenai pembagian tanggung jawab gerbang.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Ikuti Ujian Sekarang"
			 * @throws Exception bila {@code tampil(...)} melempar kesalahan ZK atau Hibernate
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				if (!syaratAlert.isEmpty()) {
					String s = "";
					for (String dd : syaratAlert) {
						s += s.isEmpty() ? dd : "\n\n" + dd;
					}
					MyMessageboxConfig.show(s, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				ProsesUjianHelper.tampil(mahasiswa, biodataCalonMahasiswa, siswa, calonSiswa, pertemuanPunyaUjian,
						tambah, eventListener, false);
				window.detach();
			}
		});
		ikut.setParent(toolbar);

		window.onModal();
	}

	/**
	 * Membuka jendela CBT (Computer-Based Test) penuh tanpa menampilkan halaman konfirmasi
	 * tata tertib. Digunakan ketika peserta telah menyetujui tata tertib melalui {@link #ikut}
	 * atau dipanggil langsung oleh sistem ketika tata tertib tidak diperlukan.
	 *
	 * <p><b>Tujuan:</b> Mempersiapkan {@code HasilUjianMahasiswa} awal (khususnya mengatur
	 * {@code mulaiPada} bila belum terisi), lalu menginstansiasi dan menampilkan
	 * {@link ProsesUjianHelper} sebagai jendela modal penuh layar. Mode preview (dosen/admin
	 * tidak ada hasilUjian) dan mode ujian nyata dibedakan melalui parameter {@code hanyaLihat}.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Memuat {@code HasilUjianMahasiswa} dari database menggunakan
	 *       {@code HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian, mahasiswa, ...)}.
	 *       Jika ditemukan dan {@code mulaiPada} masih null, diisi dengan waktu saat ini
	 *       dan di-flush ke database (menandai peserta "mulai ujian").</li>
	 *   <li>Bila {@code hasilUjianMahasiswa} tidak ditemukan, {@code hanyaLihat} diset false
	 *       (tidak ada yang perlu dilihat — ini mode ujian murni pertama kali).</li>
	 *   <li>Menginstansiasi {@link ProsesUjianHelper} via konstruktor privat, mengatur ukuran
	 *       98%×98% modal, lalu memanggil {@code onModal()}.</li>
	 *   <li>Judul jendela mencerminkan mode: "Preview NamaUjian" (hanyaLihat, hasilNull)
	 *       vs "Proses NamaUjian" (sedang aktif).</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception diserahkan ke pemanggil. Konsistensi data dijaga
	 * oleh Hibernate flush yang terjadi sebelum konstruksi jendela.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Bila jumlah tipe peserta bertambah (misal peserta kursus online
	 * eksternal), parameter baru perlu ditambah di sini dan di konstruktor privat. Pastikan
	 * pula {@code HasilUjianMahasiswa.ambilByKey} mendukung tipe tersebut.</p>
	 *
	 * @param mahasiswa             entitas Mahasiswa; null untuk tipe peserta lain
	 * @param biodataCalonMahasiswa entitas calon mahasiswa PMB; null untuk tipe lain
	 * @param siswa                 entitas Siswa sekolah; null untuk tipe lain
	 * @param calonSiswa            entitas calon siswa PPDB; null untuk tipe lain
	 * @param pertemuanPunyaUjian   konfigurasi ujian dalam pertemuan ini
	 * @param tambah                true jika sesi ini menambah counter keikutsertaan
	 * @param eventListener         callback pasca ujian selesai
	 * @param hanyaLihat            true = mode review (tidak menyimpan jawaban baru);
	 *                              false = mode ujian aktif; null diubah ke false bila hasilNull
	 * @throws Exception jika terjadi kesalahan Hibernate atau ZK saat membangun jendela
	 */
	public static void tampil(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa, Siswa siswa,
			CalonSiswa calonSiswa, PertemuanPunyaUjian pertemuanPunyaUjian, boolean tambah, EventListener eventListener,
			Boolean hanyaLihat) throws Exception {

		// BLOKIR akses ujian bagi PESERTA yang ditandai "Tidak perlu ikut" (Peserta yg tidak perlu
		// ikut → pertemuanPunyaUjian.mhsYgTidakIkut). Ini titik akhir pembuka jendela ujian untuk SEMUA
		// jalur (tombol Ikut Ujian → ikut() → tampil(), lanjut/resume, akses langsung), sehingga tanpa
		// guard di sini peserta yang di-block dosen MASIH bisa mengerjakan ujian (flag tsb sebelumnya
		// hanya dipakai di rekap/hasil). Hanya blokir bila yang MENGAKSES adalah peserta itu sendiri
		// (current user mahasiswa/siswa/calon), BUKAN dosen/admin yang sedang melakukan preview.
		Tbmuser tbmuserCekBlokir = Common.getCurrentUser();
		boolean pengaksesPeserta = tbmuserCekBlokir != null && (tbmuserCekBlokir.getMahasiswa() != null
				|| tbmuserCekBlokir.getSiswa() != null || tbmuserCekBlokir.getCalonSiswa() != null
				|| tbmuserCekBlokir.getBiodataCalonMahasiswa() != null || tbmuserCekBlokir.getPesertaKursus() != null);
		Long idPesertaUjian = mahasiswa != null ? mahasiswa.getId()
				: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
						: siswa != null ? siswa.getId() : calonSiswa != null ? calonSiswa.getId() : null;
		if (pengaksesPeserta && idPesertaUjian != null && pertemuanPunyaUjian != null
				&& pertemuanPunyaUjian.getMhsYgTidakIkut() != null
				&& pertemuanPunyaUjian.getMhsYgTidakIkut().contains("," + idPesertaUjian + ",")) {
			MyMessageboxConfig.show(
				"Mohon maaf, Bapak/Ibu ditandai \"tidak perlu mengikuti\" ujian ini oleh dosen atau pengampu, sehingga tidak dapat mengerjakannya. Apabila Anda merasa hal ini keliru, silakan hubungi dosen atau pengampu untuk dilakukan penyesuaian.",
				"Tidak Dapat Mengikuti Ujian", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return;
		}

		Session session = HibernateUtil.currentSession();
		HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(pertemuanPunyaUjian, mahasiswa,
				biodataCalonMahasiswa, siswa, calonSiswa);
		Tbmuser tbmuser = Common.getCurrentUser();
		if ((mahasiswa != null || biodataCalonMahasiswa != null
				|| (tbmuser != null && tbmuser.getPesertaKursus() != null) || siswa != null || calonSiswa != null)) {
			if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMulaiPada() == null) {
				hasilUjianMahasiswa.setMulaiPada(ais.ui.util.WaktuUtil.getDate());
				session.update(hasilUjianMahasiswa);
				session.flush();
			}
		}

		if (hasilUjianMahasiswa == null) {
			hanyaLihat = false;
		}

		// Cek multi-device: tolak jika ujian sudah aktif di perangkat lain
		if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null
				&& Boolean.TRUE.equals(pertemuanPunyaUjian.getAntiCurangLarangMultiDevice())
				&& Boolean.TRUE.equals(pertemuanPunyaUjian.getAntiCurangAktif())) {
			String sesiSekarang = null;
			try {
				org.zkoss.zk.ui.Session zkSessMD = org.zkoss.zk.ui.Sessions.getCurrent();
				if (zkSessMD != null) {
					Object nativeSessMD = zkSessMD.getNativeSession();
					if (nativeSessMD instanceof javax.servlet.http.HttpSession) {
						sesiSekarang = ((javax.servlet.http.HttpSession) nativeSessMD).getId();
					}
				}
			} catch (Exception eMD) { /* abaikan — gagal dapat ID sesi, lanjut tanpa blokir */ }
			String sesiLama = SESI_AKTIF_UJIAN.get(hasilUjianMahasiswa.getId());
			if (sesiLama != null && sesiSekarang != null && !sesiLama.equals(sesiSekarang)) {
				ais.ui.util.MyMessageboxConfig.show(
					"Ujian ini sedang dikerjakan di perangkat lain. Anda tidak dapat membuka ujian yang sama pada dua perangkat secara bersamaan.",
					"Perangkat Ganda Terdeteksi", ais.ui.util.MyMessageboxConfig.OK, ais.ui.util.MyMessageboxConfig.EXCLAMATION);
				return;
			}
			if (sesiSekarang != null) {
				SESI_AKTIF_UJIAN.put(hasilUjianMahasiswa.getId(), sesiSekarang);
			}
		}

		ProsesUjianHelper prosesUjianHelper = new ProsesUjianHelper(pertemuanPunyaUjian, hasilUjianMahasiswa,
				pertemuanPunyaUjian.getJmlDitampilkan(), hanyaLihat, tambah, eventListener);
		prosesUjianHelper.setClosable(false);
		prosesUjianHelper.setTitle(hasilUjianMahasiswa == null ? "Preview " + pertemuanPunyaUjian.getUjian().getNama()
				: "Proses " + pertemuanPunyaUjian.getUjian().getNama());
		prosesUjianHelper.setHeight("98%");
		prosesUjianHelper.setWidth("98%");
		prosesUjianHelper.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
		prosesUjianHelper.onModal();
		// Ujian selesai/ditutup: bebaskan slot multi-device
		if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null) {
			SESI_AKTIF_UJIAN.remove(hasilUjianMahasiswa.getId());
		}

	}

	/** Serial version UID untuk serialisasi ZK component tree (diperlukan oleh MyWindow → Window). */
	private static final long serialVersionUID = 8876503085998178880L;

	/** Hasil ujian peserta yang sedang dikerjakan; null bila mode preview (tanpa peserta nyata). */
	private HasilUjianMahasiswa hasilUjianMahasiswa;

	/**
	 * Daftar ID {@code UjianPunyaSoal} yang ditampilkan dalam sesi ini, dalam urutan yang sudah
	 * diacak (jika konfigurasi ujian random=true). Diisi oleh {@link #initSoal(Label)}.
	 */
	private MyArrayList<Long> ujianPunyaSoals;

	/** Relasi pertemuan-ujian yang merupakan sumber konfigurasi (waktu, jumlah soal, jenis, dll.). */
	private PertemuanPunyaUjian pertemuanPunyaUjian;

	/** Tombol "Simpan/Akhiri Ujian" di toolbar bawah jendela CBT. */
	private MyToolbarbuttonConfig save;

	/** Tombol "Keluar/Batalkan Ujian" — hanya aktif jika peserta belum menyimpan apapun. */
	private MyToolbarbuttonConfig cancel;

	/** Indeks soal yang sedang ditampilkan saat ini (berbasis-nol terhadap {@link #ujianPunyaSoals}). */
	private int index;

	/** Indeks sementara saat tombol navigasi ditekan; digunakan sebelum konfirmasi. */
	private int indexTemp;

	/** Tombol navigasi "Kembali" (soal sebelumnya). */
	private MyToolbarbuttonConfig back;

	/** Tombol navigasi "Lanjut" (soal berikutnya). */
	private MyToolbarbuttonConfig next;

	/** Timer ZK 1 detik untuk countdown dan autosave berkala. */
	private Timer timer = new Timer(1000);

	/** Panel North yang menampung baris informasi soal dan countdown. */
	private North north;

	/** Label ZK yang menampilkan sisa waktu ujian dalam format jam:menit:detik. */
	private Label waktu;

	/**
	 * Jumlah soal yang ditampilkan dalam satu halaman tampilan.
	 * Default 1 (satu soal per halaman). Bisa >1 jika {@code PertemuanPunyaUjian.jmlDitampilkan > 1}.
	 */
	private Integer jumlahSoalPerHalaman = 1;

	/** Calendar yang menyimpan waktu berjalan (naik) sebagai stopwatch lama pengerjaan. */
	private Calendar lamaTime;

	/**
	 * Jumlah soal yang harus diujikan kepada peserta ini. Bersumber dari
	 * {@code PertemuanPunyaUjian.jmlDitampilkan}; jika 0 atau melampaui total soal tersedia,
	 * disesuaikan ke total soal tersedia.
	 */
	private Integer jumlahDiujikan = 0;

	/** Callback yang dipanggil setelah ujian selesai untuk memperbarui tampilan luar (grid hasil, dll.). */
	private EventListener eventListener;

	/**
	 * Bila true, jendela hanya menampilkan soal dan jawaban yang sudah ada tanpa mengizinkan
	 * perubahan (mode review pasca ujian). Bila false, mode ujian aktif.
	 */
	private Boolean hanyaLihat;

	/** Grid ZK yang menampilkan soal dan opsi jawaban di area tengah jendela CBT. */
	private Grid gridSoal;

	/**
	 * Peta (bankSoalId → Set&lt;HasilUjianMahasiswaDetailId&gt;) yang melacak jawaban peserta
	 * yang sudah tersimpan. Diisi oleh {@link #initHasilUjian(Label)} dari database, lalu
	 * diperbarui setiap kali peserta memilih jawaban baru.
	 */
	private MyHashMap<Long, Set<Long>> hasilUjianMahasiswaDetailsa;

	/** Rows ZK untuk daftar soal yang SUDAH dijawab dalam panel navigasi samping. */
	private Rows rowsYgTelahDikerjakan;

	/** Paging ZK untuk navigasi halaman daftar soal belum dijawab. */
	private Paging pagingBelumTerjawab;

	/** Rows ZK untuk daftar soal yang BELUM dijawab dalam panel navigasi samping. */
	private Rows rowsYgBelumDikerjakan;

	/** Paging ZK untuk navigasi halaman daftar soal sudah dijawab. */
	private Paging pagingTelahTerjawab;

	/** Konfigurasi tab "Soal" di panel tengah CBT. */
	private MyTabConfig tabSoal;

	/** Label teks "Sisa Waktu" yang dikonfigurasi dari bahasa sistem. */
	private String label = Common.getBahasaConfig("Sisa Waktu");

	/** EventListener yang dipasang pada baris soal belum terjawab untuk navigasi klik. */
	private EventListener belumDijawabEventListener;

	/** Konfigurasi tab "Jawaban" (panel rekap jawaban yang sudah diisi). */
	private MyTabConfig tabJawaban;

	/**
	 * Thread countdown waktu ujian. Dijaga referensinya agar bisa dihentikan via
	 * {@code waktuTimer.stop = true} saat ujian selesai atau waktu habis.
	 */
	private Waktu waktuTimer = null;

	/**
	 * Scheduler BERSAMA untuk countdown SEMUA ujian — menggantikan pola lama satu
	 * {@code new Thread()} per ujian (penyebab ledakan jumlah thread saat banyak ujian
	 * berlangsung). Tugas tick 1 detik sangat ringan sehingga pool kecil cukup melayani
	 * ribuan ujian. Daemon agar tak menghalangi shutdown JVM/Tomcat. Perilaku countdown
	 * (kurangi currentTime tiap detik, sisi server, lepas dari ZK Timer/klien) TETAP SAMA.
	 */
	private static final java.util.concurrent.ScheduledExecutorService COUNTDOWN_SCHEDULER =
			java.util.concurrent.Executors.newScheduledThreadPool(2, new java.util.concurrent.ThreadFactory() {
				/**
				 * Pencacah atomik penomoran thread countdown ({@code ujian-countdown-1},
				 * {@code ujian-countdown-2}, dan seterusnya). Tujuannya agar setiap thread di pool memiliki
				 * nama yang unik dan mudah dikenali pada <i>thread dump</i> ketika menelusuri masalah
				 * performa ujian daring.
				 */
				private final java.util.concurrent.atomic.AtomicInteger nomor = new java.util.concurrent.atomic.AtomicInteger(1);

				/**
				 * Membuat satu thread daemon bernama {@code ujian-countdown-N} untuk pool countdown.
				 *
				 * <p>Status daemon dipilih agar thread ini tidak pernah menghalangi proses shutdown
				 * JVM/Tomcat walaupun masih ada ujian berjalan saat server dimatikan.</p>
				 *
				 * @param r tugas tick countdown (satu instance {@link Waktu}) yang akan dijalankan pool
				 * @return thread daemon siap pakai dengan nama unik
				 */
				@Override
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "ujian-countdown-" + nomor.getAndIncrement());
					t.setDaemon(true);
					return t;
				}
			});

	/** Panel West yang menampilkan penjelasan/pembahasan soal (hanya aktif di mode review). */
	private West penjelasan;

	/** Vbox yang menampung pilihan jawaban (checkbox/radio) untuk soal yang sedang aktif. */
	private Vbox vboxJawaban;

	/**
	 * Referensi ke {@code BankSoal} soal yang sedang ditampilkan. Dipakai oleh
	 * {@link #jumlahDibatasi()} untuk memeriksa batas minimal/maksimal jawaban.
	 */
	private BankSoal currentBankSoal;

	/**
	 * Konstruktor privat — hanya dipanggil oleh {@link #tampil(Mahasiswa,BiodataCalonMahasiswa,
	 * Siswa,CalonSiswa,PertemuanPunyaUjian,boolean,EventListener,Boolean)}.
	 *
	 * <p><b>Tujuan:</b> Menginisialisasi seluruh state instance (hasilUjianMahasiswa,
	 * pertemuanPunyaUjian, jumlahDiujikan, hanyaLihat, tambah, eventListener) lalu memanggil
	 * {@link #init()} yang memulai proses asinkron persiapan soal.</p>
	 *
	 * <p><b>Cara kerja:</b> Setelah {@code super()} dipanggil (konstruktor {@code MyWindow}),
	 * field-field instance diisi dari parameter, kemudian {@link #init()} dipanggil. {@code init()}
	 * dapat melakukan pengecekan syarat ujian dan menampilkan loading-bar sebelum soal dimuat.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception dari {@link #init()} diserahkan ke pemanggil
	 * (static method {@link #tampil}) yang pada gilirannya diserahkan ke ZK event framework.</p>
	 *
	 * @param pertemuanPunyaUjian relasi pertemuan-ujian sumber konfigurasi
	 * @param hasilUjianMahasiswa hasil ujian peserta yang sudah ada; null = mode preview
	 * @param jumlahDiujikan      jumlah soal yang harus ditampilkan (dari konfigurasi pertemuan)
	 * @param hanyaLihat          true = mode review saja, false = mode aktif
	 * @param tambah              true = sesi baru (counter jumlahIkut akan naik)
	 * @param eventListener       callback pasca ujian selesai
	 * @throws Exception bila {@link #init()} gagal (syarat tidak terpenuhi atau error ZK)
	 */
	private ProsesUjianHelper(PertemuanPunyaUjian pertemuanPunyaUjian, HasilUjianMahasiswa hasilUjianMahasiswa,
			Integer jumlahDiujikan, Boolean hanyaLihat, boolean tambah, EventListener eventListener) throws Exception {
		super();
		this.hasilUjianMahasiswa = hasilUjianMahasiswa == null ? null : hasilUjianMahasiswa;
		this.pertemuanPunyaUjian = pertemuanPunyaUjian;
		this.eventListener = eventListener;
		this.jumlahDiujikan = jumlahDiujikan;
		this.hanyaLihat = hanyaLihat;
		this.tambah = tambah;
		init();
	}

	/**
	 * Memuat ulang daftar soal yang SUDAH dijawab ke dalam panel navigasi samping.
	 *
	 * <p><b>Tujuan:</b> Menyegarkan tampilan daftar soal terjawab dengan paginasi. Setiap baris
	 * menampilkan nomor soal beserta indikator jawaban (checkbox/radio yang sudah dipilih),
	 * lengkap dengan tombol navigasi langsung ke soal tersebut.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Membersihkan semua baris lama di {@link #rowsYgTelahDikerjakan} menggunakan
	 *       {@code Common.clear()}.</li>
	 *   <li>Mengiterasi seluruh {@link #ujianPunyaSoals} dan memfilter hanya yang ID-nya ada
	 *       di {@code idsa} (soal yang sudah dijawab).</li>
	 *   <li>Menerapkan paginasi: hanya soal pada rentang [{@code mulai}, {@code mulai+banyak})
	 *       yang di-render.</li>
	 *   <li>Untuk setiap soal, mengambil {@code HasilUjianMahasiswaDetail} dari cache
	 *       {@link #hasilUjianMahasiswaDetailsa} dan menampilkan pilihan jawaban yang sudah dipilih
	 *       beserta tombol navigasi "→" ke soal tersebut.</li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini dipanggil setiap kali peserta berpindah soal atau
	 * menyimpan jawaban baru agar daftar tetap sinkron. Jika menambah tipe soal baru, pastikan
	 * rendering jawaban di sini juga diperbarui.</p>
	 *
	 * @param idsa                                set berisi ID {@code BankSoal} yang sudah dijawab
	 * @param jumlahDataDalamSatuHalamanElearning ukuran halaman (jumlah baris per halaman)
	 * @param mulai                               indeks offset awal soal yang akan di-render
	 * @param banyak                              jumlah soal yang akan di-render dari {@code mulai}
	 */
	private void reloadTelahDikerjakan(Set<Long> idsa, int jumlahDataDalamSatuHalamanElearning, int mulai, int banyak) {
		Common.clear(rowsYgTelahDikerjakan);
		int index = 0;
		for (final Long ujianPunyaSoalid : ujianPunyaSoals) {
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					ujianPunyaSoalid.toString());
			if (ujianPunyaSoal != null) {

				if (idsa.contains(ujianPunyaSoal.getBankSoal().getId())) {

					if (index >= mulai && index < (mulai + banyak)) {
						Set<Long> aa = hasilUjianMahasiswa == null ? null
								: hasilUjianMahasiswaDetailsa.get(ujianPunyaSoal.getBankSoal().getId());

						MyFormRow rowSoalYgBelum = new MyFormRow();
						rowSoalYgBelum.setParent(rowsYgTelahDikerjakan);

						Vbox vbox = new Vbox();
						vbox.setParent(rowSoalYgBelum);

						if (pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali()) {
							vbox.appendChild(new Label(
									"Soal no : " + ((ujianPunyaSoals.indexOf(ujianPunyaSoal.getId()) + 1) + " ")));
						} else {
							Radio a = new Radio(
									"Soal no : " + ((ujianPunyaSoals.indexOf(ujianPunyaSoal.getId()) + 1) + " "));
							vbox.appendChild(a);
							a.addEventListener("onClick", new EventListener() {

								/**
								 * Melompat ke soal yang diklik pada daftar "Telah terjawab".
								 *
								 * <p><b>Cara kerja:</b> ketika radio pada baris soal dicentang, daftar
								 * {@link ProsesUjianHelper#ujianPunyaSoals} ditelusuri untuk menemukan posisi (indeks) soal
								 * tersebut; field {@code index} milik instance diperbarui, lalu
								 * {@link ProsesUjianHelper#doProcessUjian(int)} me-render soal itu di panel tengah. Tab
								 * "Soal" kemudian diaktifkan supaya peserta langsung melihat hasilnya tanpa berpindah tab
								 * secara manual.</p>
								 *
								 * <p><b>Catatan:</b> baris yang dapat diklik hanya dibuat bila konfigurasi ujian MENGIZINKAN
								 * tombol kembali ({@code getTidakDiaktifkanTombolKembali() == false}). Pada ujian satu-arah,
								 * baris dirender sebagai {@code Label} biasa sehingga listener ini tidak pernah dipasang.</p>
								 *
								 * @param arg0 event {@code onClick} ZK; targetnya adalah {@code Radio} baris soal
								 * @throws Exception bila render ulang soal gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									Radio a = (Radio) arg0.getTarget();
									if (a.isChecked()) {
										int localindex = 0;
										for (Long sid : ujianPunyaSoals) {
											UjianPunyaSoal s = (UjianPunyaSoal) GeneralValueObject
													.ambilData(UjianPunyaSoal.class, sid.toString());
											if (s != null && s.getId() != null) {
												if (s.getId().equals(ujianPunyaSoalid)) {
													ProsesUjianHelper.this.index = localindex;
													doProcessUjian(ProsesUjianHelper.this.index);
													break;
												}
												localindex++;
											}
										}
										tabSoal.setSelected(true);
									}
								}
							});
						}

						vbox.appendChild(new ais.ui.util.MyHtml(ujianPunyaSoal.getBankSoal().getSoal()));

						if (aa != null) {
							for (Long id : aa) {
								HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
										.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
								MyLabelBoldAja jawab = new MyLabelBoldAja(hasilUjianMahasiswaDetail == null ? ""
										: hasilUjianMahasiswaDetail.getBankSoalDetail() == null
												? (hasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian().getJenis()
														.equals(BankSoal.ESAY)
																? "Jawaban Anda : "
																		+ hasilUjianMahasiswaDetail.getJawaban()
																: "")
												: "Jawaban Anda : " +

														(pertemuanPunyaUjian.getUjian()
																.getTampilanHurufDiPilihanJawaban()
																		? hasilUjianMahasiswaDetail.getBankSoalDetail()
																				.getHuruf() + ". "
																		: "")

														+ hasilUjianMahasiswaDetail.getBankSoalDetail().getJawaban());
								Hbox lampiran = new Hbox();
								BankSoalAction.tampilkanLampiran(hasilUjianMahasiswaDetail, lampiran, false,
										ujianPunyaSoal.getBankSoal().getJumlahLampiran(), null);

								vbox.appendChild(jawab);
								vbox.appendChild(lampiran);
							}

						}

					}
					index++;
				}
			}
		}

		pagingTelahTerjawab.setTotalSize(index);
		pagingTelahTerjawab.setVisible(index > jumlahDataDalamSatuHalamanElearning);
		pagingTelahTerjawab.getParent().setVisible(pagingTelahTerjawab.isVisible());
	}

	/**
	 * Memuat ulang daftar soal yang BELUM dijawab ke dalam panel navigasi samping.
	 *
	 * <p><b>Tujuan:</b> Simetris dengan {@link #reloadTelahDikerjakan}, method ini menampilkan
	 * soal-soal yang belum dijawab peserta agar peserta mudah mendeteksi dan melompat ke soal
	 * yang masih kosong. Panel ini sangat penting saat ujian hampir habis waktu.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Membersihkan semua baris lama di {@link #rowsYgBelumDikerjakan}.</li>
	 *   <li>Mengiterasi seluruh {@link #ujianPunyaSoals} dan memfilter yang ID-nya TIDAK ada
	 *       di {@code idsa} (belum dijawab).</li>
	 *   <li>Menerapkan paginasi: hanya soal pada rentang [{@code mulai}, {@code mulai+banyak})
	 *       yang di-render.</li>
	 *   <li>Setiap baris menampilkan nomor soal dan tombol navigasi ke soal tersebut sehingga
	 *       peserta bisa langsung melompat ke soal yang belum dijawab dengan satu klik.</li>
	 *   <li>Bila konfigurasi ujian tidak mengizinkan tombol kembali
	 *       ({@code getTidakDiaktifkanTombolKembali()} = true), navigasi hanya berupa label
	 *       teks (tidak bisa diklik) untuk mencegah peserta mundur ke soal sebelumnya.</li>
	 * </ol>
	 *
	 * <p><b>Thread-safety:</b> Dipanggil dari ZK event thread (single-threaded per session)
	 * sehingga aman mengakses field instance tanpa sinkronisasi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini dipanggil bersamaan dengan
	 * {@link #reloadTelahDikerjakan} setiap kali status jawaban berubah. Perubahan pada skema
	 * paginasi atau tampilan baris perlu direplikasi di keduanya.</p>
	 *
	 * @param idsa                                set ID {@code BankSoal} yang sudah dijawab
	 *                                            (soal yang TIDAK ada di set ini yang akan ditampilkan)
	 * @param jumlahDataDalamSatuHalamanElearning ukuran halaman (jumlah baris per halaman)
	 * @param mulai                               indeks offset awal soal yang akan di-render
	 * @param banyak                              jumlah soal yang akan di-render dari {@code mulai}
	 */
	private void reloadBelumDikerjakan(Set<Long> idsa, int jumlahDataDalamSatuHalamanElearning, int mulai, int banyak) {
		Common.clear(rowsYgBelumDikerjakan);
		int index = 0;
		for (final Long ujianPunyaSoalid : ujianPunyaSoals) {
			UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					ujianPunyaSoalid.toString());
			if (ujianPunyaSoal != null) {
				if (!idsa.contains(ujianPunyaSoal.getBankSoal().getId())) {
					if (index >= mulai && index < (mulai + banyak)) {

						MyFormRow rowSoalYgBelum = new MyFormRow();
						rowSoalYgBelum.setParent(rowsYgBelumDikerjakan);

						if (pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali()) {
							rowSoalYgBelum.appendChild(new Vbox(new Component[] {
									new Label("Soal no : "
											+ ((ujianPunyaSoals.indexOf(ujianPunyaSoal.getId()) + 1) + "")),
									new ais.ui.util.MyHtml(ujianPunyaSoal.getBankSoal().getSoal()) }));
						} else {
							Radio a = new Radio(
									"Soal no : " + ((ujianPunyaSoals.indexOf(ujianPunyaSoal.getId()) + 1) + ""));
							rowSoalYgBelum.appendChild(new Vbox(new Component[] { a,
									new ais.ui.util.MyHtml(ujianPunyaSoal.getBankSoal().getSoal()) }));

							a.addEventListener("onClick", new EventListener() {

								/**
								 * Melompat ke soal yang diklik pada daftar "Belum terjawab".
								 *
								 * <p>Kembaran dari listener serupa di {@link ProsesUjianHelper#reloadTelahDikerjakan}: posisi
								 * soal dicari di {@link ProsesUjianHelper#ujianPunyaSoals}, {@code index} instance diperbarui,
								 * {@link ProsesUjianHelper#doProcessUjian(int)} me-render soal, lalu tab "Soal" diaktifkan.
								 * Panel ini penting saat waktu ujian hampir habis karena peserta dapat langsung menuju soal
								 * yang masih kosong dengan satu klik.</p>
								 *
								 * <p>Sama seperti kembarannya, listener hanya dipasang bila tombol kembali diizinkan
								 * ({@code getTidakDiaktifkanTombolKembali() == false}).</p>
								 *
								 * @param arg0 event {@code onClick} ZK; targetnya adalah {@code Radio} baris soal
								 * @throws Exception bila render ulang soal gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									Radio a = (Radio) arg0.getTarget();
									if (a.isChecked()) {
										int localindex = 0;
										for (final Long sid : ujianPunyaSoals) {
											UjianPunyaSoal s = (UjianPunyaSoal) GeneralValueObject
													.ambilData(UjianPunyaSoal.class, sid.toString());
											if (s != null && s.getId() != null) {
												if (s.getId().equals(ujianPunyaSoalid)) {
													ProsesUjianHelper.this.index = localindex;
													doProcessUjian(ProsesUjianHelper.this.index);
													break;
												}
												localindex++;
											}
										}
										tabSoal.setSelected(true);
									}
								}
							});
						}
					}
					index++;
				}
			}
		}

		pagingBelumTerjawab.setTotalSize(index);
		pagingBelumTerjawab.setVisible(index > jumlahDataDalamSatuHalamanElearning);
		pagingBelumTerjawab.getParent().setVisible(pagingBelumTerjawab.isVisible());
	}

	/**
	 * Memeriksa apakah soal yang sudah tersimpan di hasil ujian peserta cukup memenuhi kuota
	 * {@code jumlahDiujikan}. Bila kurang, soal tambahan diambil dari master soal ujian dan
	 * ditambahkan ke daftar hasil secara sinkronisasi.
	 *
	 * <p><b>Tujuan:</b> Menangani kasus di mana peserta sebelumnya mengerjakan ujian dengan
	 * jumlah soal yang lebih sedikit dari kuota saat ini (misalnya admin menambah soal ke ujian
	 * setelah peserta mulai). Method ini memastikan peserta mendapat soal sesuai kuota terkini
	 * tanpa mengacak ulang soal yang sudah dikerjakan.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Mencetak info diagnostik: jumlah soal tersedia, jumlah soal di hasil peserta, dan
	 *       kuota yang diinginkan.</li>
	 *   <li>Jika {@code jumlahDiujikan > ujianPunyaSoalsHasil.size()}, mencari soal dari
	 *       {@code ujianPunyaSoals} yang belum ada di {@code ujianPunyaSoalsHasil}.</li>
	 *   <li>Menambahkan soal baru sampai kuota terpenuhi, berhenti ketika
	 *       {@code ujianPunyaSoalsHasil.size() + tambahan >= jumlahDiujikan}.</li>
	 *   <li>Penambahan dilakukan dalam blok {@code synchronized(ujianPunyaSoalsHasil)} untuk
	 *       menghindari race condition bila dipanggil dari thread latar.</li>
	 *   <li>Jika soal sudah cukup, hanya mencetak log dan tidak melakukan perubahan.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception ditangkap dan dicetak ke stderr — operasi ini bersifat
	 * best-effort; kegagalan tidak menghentikan alur ujian.</p>
	 *
	 * <p><b>Thread-safety:</b> Penambahan ke {@code ujianPunyaSoalsHasil} dilakukan dalam blok
	 * {@code synchronized} meskipun di sebagian besar kasus method ini dipanggil dari satu thread
	 * saja. Pertimbangkan menggunakan {@code CopyOnWriteArrayList} bila pemanggilan lintas thread
	 * menjadi lebih sering.</p>
	 *
	 * @param ujianPunyaSoals       daftar semua ID soal yang tersedia untuk ujian ini (master)
	 * @param ujianPunyaSoalsHasil  daftar ID soal yang sudah ada di hasil ujian peserta (dimodifikasi)
	 * @param jumlahDiujikan        target jumlah soal yang harus ada di {@code ujianPunyaSoalsHasil}
	 */
	public static void chekPosisitonJikaKurang(List<Long> ujianPunyaSoals, List<Long> ujianPunyaSoalsHasil,
			Integer jumlahDiujikan) {

		System.out
				.println("jumlah soal yg  jumlahDiujikan" + jumlahDiujikan + "  tersedia -> " + ujianPunyaSoals.size());
		System.out.println("jumlah soal yg sebelumnya diikuti -> " + ujianPunyaSoalsHasil.size());

		try {
			if (jumlahDiujikan > ujianPunyaSoalsHasil.size()) {
				List<Long> ygBelumAda = new ArrayList<Long>();
				for (Long ujianPunyaSoal : ujianPunyaSoals) {
					if (!ujianPunyaSoalsHasil.contains(ujianPunyaSoal)) {
						ygBelumAda.add(ujianPunyaSoal);
						if ((ujianPunyaSoalsHasil.size() + ygBelumAda.size()) >= jumlahDiujikan) {
							break;
						}
					}
				}
				System.out.println("belum ada -> " + ygBelumAda);
				synchronized (ujianPunyaSoalsHasil) {
					ujianPunyaSoalsHasil.addAll(ygBelumAda);
				}
			} else {
				System.out.println("Soal tidak ada yang kurang..");
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:902");
		}
	}

	/**
	 * Memilih soal dari bank soal ujian secara acak atau berurutan sesuai kuota, dan mengembalikan
	 * daftar ID {@code UjianPunyaSoal} yang akan ditampilkan kepada peserta.
	 *
	 * <p><b>Tujuan:</b> Memastikan setiap peserta mendapat himpunan soal yang berbeda (bila
	 * {@code rand=true}) untuk mengurangi kecurangan mencontek, atau mendapat soal berurutan
	 * (bila {@code rand=false}) untuk ujian dengan urutan soal tetap. Method ini adalah implementasi
	 * sampling soal: memilih tepat {@code jumlahDiujikan} soal dari pool yang tersedia.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Menyesuaikan {@code jmlDiujikan}: jika {@code ujianPunyaSoals.size() < jumlahDiujikan}
	 *       atau {@code jumlahDiujikan <= 0}, pakai seluruh soal yang tersedia.</li>
	 *   <li>Membuat {@code MyArrayList<Long>} dengan kapasitas awal {@code jmlDiujikan}.</li>
	 *   <li>Mengisi daftar dalam loop {@code while(true)} yang berhenti ketika ukuran daftar
	 *       mencapai {@code jmlDiujikan}:</li>
	 *   <ul>
	 *     <li>Bila {@code rand=true}: pilih posisi acak via {@code Random.nextInt(jml)}.</li>
	 *     <li>Bila {@code rand=false}: pilih soal berurutan dari indeks 0, 1, 2, ...</li>
	 *   </ul>
	 *   <li>Catatan: implementasi saat ini mengizinkan duplikat soal bila pool lebih kecil dari
	 *       kuota (duplikat dicegah di level {@link #chekPosisitonJikaKurang}). Ini adalah
	 *       perilaku yang diketahui dan diterima pada konfigurasi soal sangat sedikit.</li>
	 *   <li>Update label ZK ("harap tunggu.. Sedang menyiapkan soal") dipanggil setiap iterasi
	 *       agar UI loading-bar tidak membeku.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception per iterasi ditangkap dan dicetak ke stderr; loop
	 * tetap berjalan hingga kuota terpenuhi. Ini menghindari ujian yang tidak bisa dimulai
	 * hanya karena satu soal rusak di cache.</p>
	 *
	 * <p><b>Thread-safety:</b> Dipanggil dari thread latar ({@link #init()} memanggil
	 * {@link #initSoal} di thread baru). {@code label.setValue()} dibungkus dalam try-catch
	 * karena mungkin membutuhkan ZK execution context yang tidak tersedia di thread latar
	 * (meskipun dalam praktiknya ZK Label dapat diperbarui dari thread latar yang didaftarkan
	 * dengan benar melalui {@code Executions.schedule}).</p>
	 *
	 * @param ujianPunyaSoals pool semua ID {@code UjianPunyaSoal} yang tersedia untuk ujian
	 * @param rand            true = pilih acak; false = pilih berurutan dari indeks 0
	 * @param label           ZK Label untuk menampilkan pesan loading (boleh null = tidak diupdate)
	 * @param jumlahDiujikan  target jumlah soal yang harus dipilih
	 * @return {@code MyArrayList<Long>} berisi tepat {@code jmlDiujikan} ID {@code UjianPunyaSoal}
	 *         (atau semua soal bila pool lebih kecil dari kuota)
	 */
	public static MyArrayList<Long> randomPosisiton(List<Long> ujianPunyaSoals, boolean rand, Label label,
			Integer jumlahDiujikan) {

		int jmlDiujikan = jumlahDiujikan;
		if (ujianPunyaSoals.size() < jumlahDiujikan || jumlahDiujikan <= 0) {
			jmlDiujikan = ujianPunyaSoals.size();
		}

		Random random = new Random();
		MyArrayList<Long> ujianPunyaSoalsHasil = new MyArrayList<Long>(jmlDiujikan);
		int jml = ujianPunyaSoals.size();
		int index = 0;
		while (true) {
			try {
				index++;
				if (ujianPunyaSoalsHasil.size() >= jmlDiujikan) {
					break;
				}
				int randomPosition = rand ? random.nextInt(jml) : index - 1;
				Long temp = ujianPunyaSoals.get(randomPosition);
				ujianPunyaSoalsHasil.add(temp);
				if (label != null)
					label.setValue("harap tunggu.. Sedang menyiapkan soal");
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:974");
			}
		}
		return ujianPunyaSoalsHasil;
	}

	/**
	 * Menyiapkan daftar soal yang akan ditampilkan kepada peserta ujian di thread latar.
	 * Ini adalah tahap pertama dari dua tahap inisialisasi (tahap kedua: {@link #initHasilUjian}).
	 *
	 * <p><b>Tujuan:</b> Menentukan himpunan soal yang tepat untuk sesi ujian ini dengan
	 * mempertimbangkan apakah peserta sudah pernah ikut ujian sebelumnya (lanjut) atau ini
	 * sesi pertama (baru). Jika lanjut, soal yang sudah dikerjakan dipertahankan; jika ada
	 * yang kurang, ditambah dari pool master.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Jika {@code hasilUjianMahasiswa} sudah ada (peserta lanjut):
	 *     <ul>
	 *       <li>Memanggil {@code hasilUjianMahasiswa.ambilUjianPunyaSoals(jumlahDiujikan, label, false)}
	 *           untuk memuat soal-soal yang sudah pernah dikerjakan.</li>
	 *       <li>Jika jumlah soal tidak sama dengan {@code jumlahDiujikan} (admin mengubah kuota),
	 *           mengambil soal dari master dan mengacak ulang via
	 *           {@link #randomPosisiton(List,boolean,Label,Integer)}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Jika peserta baru (hasilUjianMahasiswa null):
	 *     <ul>
	 *       <li>Memanggil {@code pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal()} untuk
	 *           mendapat pool soal, lalu mengacak via {@link #randomPosisiton}.</li>
	 *     </ul>
	 *   </li>
	 *   <li>Setelah soal ditentukan, memanggil {@link #chekPosisitonJikaKurang} untuk memastikan
	 *       soal yang dihasilkan benar-benar cukup sesuai kuota.</li>
	 *   <li>Memanggil {@link #initHasilUjian(Label)} untuk melanjutkan ke tahap kedua
	 *       (memastikan baris {@code HasilUjianMahasiswaDetail} ada di database).</li>
	 * </ol>
	 *
	 * <p><b>Thread-safety:</b> Dipanggil dari thread latar yang dibuat di {@link #init()}.
	 * Menggunakan cache {@code GeneralValueObject} yang thread-safe untuk akses entitas.</p>
	 *
	 * <p><b>Penanganan error:</b> Exception ditangkap di dua blok try-catch terpisah:
	 * satu untuk proses pemilihan soal, satu untuk {@link #chekPosisitonJikaKurang}.
	 * Kesalahan ditampilkan via {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * @param label ZK Label untuk menampilkan progress loading ke pengguna
	 */
	private void initSoal(Label label) {

		try {
			if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getId() != null) {
				ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(jumlahDiujikan, label, false);

				boolean sama = ujianPunyaSoals.size() == jumlahDiujikan;
				System.out.println("telah ada soal -> " + ujianPunyaSoals.size() + " jumlahDiujikan " + jumlahDiujikan
						+ " sama " + sama);
				if (!sama) {
					if (pertemuanPunyaUjian != null && pertemuanPunyaUjian.getId() != null) {
						List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian()
								.ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
						System.out.println("ujianPunyaSoalsTemp -> " + ujianPunyaSoalsTemp.size());
						ujianPunyaSoals = randomPosisiton(ujianPunyaSoalsTemp, pertemuanPunyaUjian.getRandom(), label,
								jumlahDiujikan);
						System.out.println("random -> " + ujianPunyaSoals.size());
					}
				}
			} else {
				List<Long> ujianPunyaSoalsTemp = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian,
						false);
				System.out.println("ujianPunyaSoalsTemp -> " + ujianPunyaSoalsTemp.size());
				ujianPunyaSoals = randomPosisiton(ujianPunyaSoalsTemp, pertemuanPunyaUjian.getRandom(), label,
						jumlahDiujikan);
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mempersiapkan soal ujian (CBT) untuk peserta",
					e, new String[] {
							"Muat ulang (refresh) halaman ujian ini lalu coba kembali.",
							"Pastikan koneksi jaringan Anda stabil selama proses persiapan soal.",
							"Apabila kendala masih berlanjut, segera hubungi Admin/Pengawas ujian dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}

		try {
			if (ujianPunyaSoals != null) {
				List<Long> tersedia = pertemuanPunyaUjian.getUjian().ambilUjianPunyaSoal(pertemuanPunyaUjian, false);
				chekPosisitonJikaKurang(tersedia, ujianPunyaSoals, jumlahDiujikan);
				tersedia = null;
			}
		} catch (Exception e) {
			e.addSuppressed(e);
		}

		initHasilUjian(label);

	}

	/**
	 * Memastikan setiap soal dalam {@link #ujianPunyaSoals} memiliki baris
	 * {@code HasilUjianMahasiswaDetail} yang tersimpan di database sebelum antarmuka ujian
	 * ditampilkan. Ini adalah tahap kedua dari inisialisasi sesi ujian.
	 *
	 * <p><b>Tujuan:</b> Menghindari situasi di mana peserta mengisi jawaban tetapi baris detail
	 * hasilnya tidak tersedia di database, yang menyebabkan jawaban hilang. Method ini bertindak
	 * sebagai "gap filler" — memastikan setiap slot soal sudah disiapkan dengan nilai default
	 * sebelum peserta mulai menjawab.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Hanya berjalan bila {@code hasilUjianMahasiswa} tidak null dan {@link #ujianPunyaSoals}
	 *       tidak kosong (jika preview mode, method ini no-op).</li>
	 *   <li>Memuat {@link #hasilUjianMahasiswaDetailsa} dari database via
	 *       {@code hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(...)}.</li>
	 *   <li>Mengiterasi setiap soal di {@link #ujianPunyaSoals} dan memeriksa apakah sudah ada
	 *       entry di {@link #hasilUjianMahasiswaDetailsa} untuk soal tersebut.</li>
	 *   <li>Bila belum ada, membuat {@code HasilUjianMahasiswaDetail} baru dengan nilai default
	 *       dari {@code bankSoal.getSkorDefault()} dan menyimpannya ke database menggunakan
	 *       {@code currentNativeSession()} dengan begin/commit eksplisit.</li>
	 *   <li>Entitas baru ditambahkan ke cache {@code GeneralValueObject} dan ke map
	 *       {@link #hasilUjianMahasiswaDetailsa} agar siap dipakai tanpa query ulang.</li>
	 *   <li>Progress ditampilkan di {@code label} dalam persentase setiap soal diproses.</li>
	 *   <li>Di akhir method, {@code label.setValue("")} membersihkan pesan loading.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception per-soal ditangkap secara lokal (tidak menghentikan
	 * iterasi). Error keseluruhan ditangkap di blok luar dan ditampilkan via
	 * {@code Common.tampilErrorJikaAdmin}. Pola ini memastikan soal yang valid tetap bisa
	 * diakses meskipun satu soal gagal disiapkan.</p>
	 *
	 * <p><b>Session Hibernate:</b> Menggunakan {@code HibernateUtil.currentNativeSession()} dengan
	 * begin/commit/close eksplisit (bukan currentSession dari ZK) karena berjalan di thread latar.
	 * Setelah selesai, {@code HibernateUtil.closeSession()} dipanggil untuk membersihkan
	 * ThreadLocal session.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Jika struktur {@code HasilUjianMahasiswaDetail} berubah (kolom baru),
	 * pastikan nilai defaultnya diisi di sini agar database tidak memiliki baris yang setengah
	 * lengkap. Perhatikan juga bahwa method ini menyimpan satu baris per iterasi — pada ujian
	 * dengan ratusan soal, ini menghasilkan banyak INSERT kecil; bisa dioptimalkan dengan
	 * batch insert jika performa menjadi masalah.</p>
	 *
	 * @param label ZK Label untuk menampilkan persentase persiapan soal kepada pengguna
	 */
	private void initHasilUjian(Label label) {
		try {

			if (hasilUjianMahasiswa != null && ujianPunyaSoals != null && !ujianPunyaSoals.isEmpty()) {

				hasilUjianMahasiswaDetailsa = new MyHashMap<Long, Set<Long>>(jumlahDiujikan);

				if (hasilUjianMahasiswa != null) {
					hasilUjianMahasiswaDetailsa = hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(true,
							jumlahDiujikan, label, ujianPunyaSoals);

				}

				int size = ujianPunyaSoals.size();
				int index = 0;
				for (Long ujianPunyaSoalid : ujianPunyaSoals) {

					UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
							ujianPunyaSoalid.toString());
					if (ujianPunyaSoal != null) {

						index++;

						Set<Long> s = hasilUjianMahasiswaDetailsa.get(ujianPunyaSoal.getBankSoal().getId());

						label.setValue("Harap tunggu.. persiapan menampilkan soal ("
								+ Common.numberFormat.get().format((index * 100.0) / size) + " %)");
						// System.out.println("myHasilUjianMahasiswaDetail -> "
						// +
						// s);
						if (s == null || s.isEmpty()) {
							Session session = HibernateUtil.currentNativeSession();
							try {
								HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
								myHasilUjianMahasiswaDetail.setBankSoal(ujianPunyaSoal.getBankSoal());
								myHasilUjianMahasiswaDetail.setHasilUjianMahasiswa(hasilUjianMahasiswa);
								myHasilUjianMahasiswaDetail.setUjianPunyaSoal(ujianPunyaSoal);
								myHasilUjianMahasiswaDetail.setNilai(ujianPunyaSoal.getBankSoal().getSkorDefault());

								session.getTransaction().begin();
								session.save(myHasilUjianMahasiswaDetail);
								session.getTransaction().commit();

								Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
								hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
								hasilUjianMahasiswaDetailsa.put(myHasilUjianMahasiswaDetail.getBankSoal().getId(),
										hasilUjianMahasiswaDetails);
								GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class,
										myHasilUjianMahasiswaDetail);
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
							HibernateUtil.closeSession();
						}
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"mempersiapkan baris hasil ujian (CBT) untuk peserta",
					e, new String[] {
							"Muat ulang (refresh) halaman ujian ini lalu coba kembali.",
							"Pastikan koneksi jaringan Anda stabil selama proses persiapan soal.",
							"Apabila kendala masih berlanjut, segera hubungi Admin/Pengawas ujian dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
		label.setValue("");
	}

	/**
	 * Menginisialisasi seluruh antarmuka dan logika CBT setelah konstruktor dipanggil.
	 * Ini adalah "pusat kendali" yang mengkoordinasikan pengecekan syarat, loading soal asinkron,
	 * dan pembuatan UI ujian.
	 *
	 * <p><b>Tujuan:</b> Memastikan peserta memenuhi semua syarat sebelum ujian dimulai, mengelola
	 * kuota ujian bersamaan, dan memulai thread latar untuk memuat soal agar UI tidak membeku.</p>
	 *
	 * <p><b>Cara kerja (urutan eksekusi):</b></p>
	 * <ol>
	 *   <li><b>Pemeriksaan syarat per-ujian:</b> Jika peserta adalah mahasiswa dengan syarat ujian
	 *       yang terkonfigurasi di {@code Ujian.syaratUjian}, method
	 *       {@code SyaratUjianAction.checkSyaratSyaratUjian} dipanggil. Jika gagal, jendela
	 *       ditutup otomatis via {@code Common.createDefaultTimer}.</li>
	 *   <li><b>Pemeriksaan syarat global per-status pertemuan:</b> Mengambil daftar {@code SyaratUjian}
	 *       yang berlaku untuk status pertemuan dan program studi mahasiswa. Jika ada yang tidak
	 *       terpenuhi, jendela ditutup. Session Hibernate native dibuka dan ditutup di sini.</li>
	 *   <li><b>Loading bar + thread soal:</b> Menampilkan loading bar ZK
	 *       ({@code Common.displayLoadBar}) dengan callback yang berjalan setelah soal siap.
	 *       Sebuah thread baru dimulai untuk memanggil {@link #initSoal(Label)} di latar.</li>
	 *   <li><b>Di callback loading bar</b> (berjalan di ZK event thread setelah thread latar selesai):
	 *     <ul>
	 *       <li>Memeriksa kuota: jika {@link #kuotaUjian} sudah penuh dan peserta ini belum
	 *           terdaftar, tampilkan pesan dan tutup jendela.</li>
	 *       <li>Jika soal tidak ditemukan, tampilkan pesan informatif dan tutup jendela.</li>
	 *       <li>Jika semua OK: tambahkan peserta ke {@link #kuotaUjian}, naikkan counter
	 *           {@code jumlahIkut} bila {@code tambah=true}, lalu panggil {@link #prosesProsesUjian()}
	 *           untuk membangun UI ujian penuh.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception dari pemeriksaan syarat diserahkan ke pemanggil.
	 * Exception di dalam callback loading bar ditangkap oleh {@code Common.tampilErrorJikaAdmin}.</p>
	 *
	 * <p><b>Thread-safety:</b> Akses ke {@link #kuotaUjian} (HashSet statik) dilakukan dari
	 * ZK event thread sehingga serialized per request. Thread latar ({@code initSoal}) hanya
	 * menulis ke field instance, bukan ke {@link #kuotaUjian}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan pemeriksaan syarat di method ini penting — jangan ubah
	 * urutan tanpa memahami bahwa pemeriksaan ke-2 (global per-status) membutuhkan native session
	 * yang harus ditutup sebelum loading bar dimulai. Jika menambah tipe pengecekan baru,
	 * pastikan session management tetap benar.</p>
	 *
	 * @throws Exception bila konstruksi ZK component gagal atau syarat ujian melempar exception
	 */
	private void init() throws Exception {

		if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
				&& pertemuanPunyaUjian.getPertemuan() != null && pertemuanPunyaUjian.getUjian() != null
				&& pertemuanPunyaUjian.getUjian().getSyaratUjian() != null) {
			Detailperkuliahan detailperkuliahan = hasilUjianMahasiswa == null ? null
					: hasilUjianMahasiswa.getMahasiswa()
							.ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());

			if (hasilUjianMahasiswa != null && detailperkuliahan == null) {
				hasilUjianMahasiswa.getMahasiswa().reInitDetailperkuliahan(HibernateUtil.currentSession());
				detailperkuliahan = hasilUjianMahasiswa.getMahasiswa()
						.ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
			}

			if (detailperkuliahan != null) {

				if (!SyaratUjianAction.checkSyaratSyaratUjian(
						pertemuanPunyaUjian.getUjian() == null ? null : pertemuanPunyaUjian.getUjian().getSyaratUjian(),
						pertemuanPunyaUjian.getPertemuan().ambilVOPembelajaran(), hasilUjianMahasiswa.getMahasiswa(),
						detailperkuliahan.getSemester(),
						pertemuanPunyaUjian.getUjian() == null ? "" : pertemuanPunyaUjian.getUjian().getNama())) {
					Common.createDefaultTimer(new EventListener() {

						/**
						 * Menutup jendela ujian secara otomatis ketika syarat ujian PER-UJIAN tidak terpenuhi.
						 *
						 * <p>Dipanggil lewat {@code Common.createDefaultTimer} — bukan {@code detach()} langsung —
						 * agar pelepasan komponen terjadi pada siklus event ZK berikutnya, sesudah dialog penjelasan
						 * syarat dari {@code SyaratUjianAction.checkSyaratSyaratUjian} sempat terbaca oleh peserta.</p>
						 *
						 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
						 * @throws Exception bila pelepasan komponen ZK gagal
						 */
						@Override
						public void onEvent(Event arg0) throws Exception {
							ProsesUjianHelper.this.detach();
						}
					});
					return;
				}
			}
		}

		if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getMahasiswa() != null
				&& pertemuanPunyaUjian.getPertemuan() != null
				&& pertemuanPunyaUjian.getPertemuan().getStatusPertemuan() != null
				&& pertemuanPunyaUjian.getUjian() != null) {
			Session session = HibernateUtil.currentNativeSession();
			List<SyaratUjian> syaratUjians = ConstantValues.simpleList(
					session.createCriteria(SyaratUjian.class)
							.add(Restrictions.or(Restrictions.isNull("fakultas"),
									Restrictions.eq("fakultas",
											hasilUjianMahasiswa.getMahasiswa().getJurusan().getFakultas())))

							.add(Restrictions.or(Restrictions.isNull("jurusan"),
									Restrictions.eq("jurusan", hasilUjianMahasiswa.getMahasiswa().getJurusan())))

							.add(Restrictions.or(Restrictions.isNull("program"),
									Restrictions.eq("program", hasilUjianMahasiswa.getMahasiswa().getProgram())))

							.add(Restrictions.eq("statusPertemuan",
									pertemuanPunyaUjian.getPertemuan().getStatusPertemuan())),
					SyaratUjian.class);

			List<SyaratUjian> syaratUjiansGlobalUjian = ConstantValues.simpleList(
					session.createCriteria(SyaratUjian.class).add(Restrictions.eq("berlakuUntukSemuaUjian", true)),
					SyaratUjian.class);
			if (!syaratUjiansGlobalUjian.isEmpty()) {
				syaratUjians.addAll(syaratUjiansGlobalUjian);
			}

			// session.disconnect();
			if (session.isOpen()) {
				ais.common.ElearningSessionUtil.closeQuietly(session);
			}
			HibernateUtil.closeSession();

			System.out.println("syaratUjians -> " + syaratUjians.size());
			if (!syaratUjians.isEmpty()) {
				Detailperkuliahan detailperkuliahan = hasilUjianMahasiswa == null ? null
						: hasilUjianMahasiswa.getMahasiswa()
								.ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());

				if (hasilUjianMahasiswa != null && detailperkuliahan == null) {
					hasilUjianMahasiswa.getMahasiswa().reInitDetailperkuliahan(HibernateUtil.currentSession());
					detailperkuliahan = hasilUjianMahasiswa.getMahasiswa()
							.ambilDetailperkuliahan(pertemuanPunyaUjian.getPertemuan().getPerkuliahan());
				}
				if (detailperkuliahan != null) {
					for (SyaratUjian syaratUjian : syaratUjians) {

						if (!SyaratUjianAction.checkSyaratSyaratUjian(syaratUjian,
								pertemuanPunyaUjian.getPertemuan().ambilVOPembelajaran(),
								hasilUjianMahasiswa.getMahasiswa(), detailperkuliahan.getSemester(),
								pertemuanPunyaUjian.getUjian() == null ? ""
										: pertemuanPunyaUjian.getUjian().getNama())) {
							Common.createDefaultTimer(new EventListener() {

								/**
								 * Menutup jendela ujian secara otomatis ketika salah satu {@code SyaratUjian} berlaku-GLOBAL
								 * tidak terpenuhi.
								 *
								 * <p>Kembaran dari listener sebelumnya, tetapi untuk gelombang pemeriksaan kedua: daftar
								 * {@code SyaratUjian} hasil query fakultas/jurusan/program/status pertemuan, ditambah syarat
								 * yang ditandai {@code berlakuUntukSemuaUjian}. Begitu satu syarat gagal, iterasi dihentikan
								 * dan jendela dilepas pada siklus event ZK berikutnya.</p>
								 *
								 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
								 * @throws Exception bila pelepasan komponen ZK gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									ProsesUjianHelper.this.detach();
								}
							});
							syaratUjians = null;
							return;
						}
					}
				}
			}
			syaratUjians = null;
		}

		final Label label = Common.displayLoadBar(new EventListener() {

			/**
			 * Callback loading-bar: berjalan di thread event ZK SETELAH thread latar selesai menyiapkan
			 * soal ({@link ProsesUjianHelper#initSoal(Label)}).
			 *
			 * <p><b>Tiga keputusan yang diambil di sini, berurutan:</b></p>
			 * <ol>
			 *   <li><b>Kuota ujian bersamaan.</b> Batas dibaca dari konfigurasi {@code kuota_ujian}
			 *       (bawaan 120). Konfigurasi lama dapat berisi teks {@code "null"} atau bukan angka,
			 *       karena itu parsing dibungkus try-catch dan nilai bawaan tetap dipakai bila gagal —
			 *       supaya peserta tidak gagal membuka ujian hanya karena konfigurasi rusak. Bila kuota
			 *       penuh DAN peserta ini belum terdaftar di {@link ProsesUjianHelper#kuotaUjian}, pesan
			 *       ditampilkan lalu jendela dilepas.</li>
			 *   <li><b>Ketersediaan soal.</b> Bila {@link ProsesUjianHelper#ujianPunyaSoals} null atau
			 *       kosong, peserta diberi pesan dan jendela dilepas.</li>
			 *   <li><b>Pencatatan keikutsertaan.</b> Bila ini ujian sungguhan ({@code hanyaLihat=false}),
			 *       {@code keyhasil} peserta dimasukkan ke {@link ProsesUjianHelper#kuotaUjian}; dan bila
			 *       {@code tambah=true}, counter {@code jumlahIkut} DINAIKKAN satu lalu disimpan.
			 *       <b>Inilah satu-satunya tempat {@code jumlahIkut} bertambah di seluruh kelas ini.</b>
			 *       Pemeriksaan apakah penambahan tersebut masih berada dalam jatah
			 *       ({@code jumlahBolehIkut}) sudah dilakukan jauh sebelumnya oleh
			 *       {@code PertemuanPunyaUjianHelper} — bukan di sini.</li>
			 * </ol>
			 *
			 * <p>Setelah ketiganya lolos, {@link ProsesUjianHelper#prosesProsesUjian()} dipanggil untuk
			 * membangun antarmuka CBT penuh.</p>
			 *
			 * @param arg0 event ZK dari loading-bar (isinya tidak dipakai)
			 * @throws Exception bila konstruksi antarmuka CBT gagal
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {

				if (hasilUjianMahasiswa != null) {
					int kuota = 120;
					try {
						String nilaiKuota = Common.getKonfigurasi("kuota_ujian", String.valueOf(kuota)).getNilai();
						if (nilaiKuota != null && !nilaiKuota.trim().isEmpty()
								&& !"null".equalsIgnoreCase(nilaiKuota.trim())) {
							int hasilParse = Integer.parseInt(nilaiKuota.trim());
							if (hasilParse > 0) {
								kuota = hasilParse;
							}
						}
					} catch (Exception e) {
						// Konfigurasi lama dapat berisi teks "null"/nonangka. Kuota bawaan
						// tetap dipakai agar peserta tidak gagal membuka ujian.
					}

					if (kuota <= kuotaUjian.size() && !kuotaUjian.contains(hasilUjianMahasiswa.getKeyhasil())) {
						MyMessageboxConfig.show(
				"Mohon maaf, kuota ujian saat ini masih penuh. Langkah yang dapat dilakukan: (1) tunggu beberapa saat hingga terdapat kuota yang tersedia; (2) coba ikuti kembali ujian ini setelah menunggu; (3) apabila kendala masih berlanjut, hubungi dosen atau Admin.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						ProsesUjianHelper.this.detach();
						return;
					}
				}

				if (ujianPunyaSoals == null || ujianPunyaSoals.isEmpty()) {
					MyMessageboxConfig.showFormat(
				"Mohon maaf, untuk ujian \"{V1}\" soal tidak ditemukan. Langkah yang dapat dilakukan: (1) muat ulang halaman lalu coba kembali; (2) hubungi dosen atau Admin untuk informasi lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, pertemuanPunyaUjian.getUjian().getNama());
					ProsesUjianHelper.this.detach();
				} else {

					try {
						if (!hanyaLihat && hasilUjianMahasiswa != null) {
							kuotaUjian.add(hasilUjianMahasiswa.getKeyhasil());
							if (tambah) {
								hasilUjianMahasiswa.setJumlahIkut(hasilUjianMahasiswa.getJumlahIkut() + 1);
								hasilUjianMahasiswa.setLengkapiJawaban(false);
								Common.refreshUpdate(hasilUjianMahasiswa);
							}
						}

					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException(
								"mencatat status keikutsertaan ujian (CBT) peserta",
								e, new String[] {
										"Muat ulang (refresh) halaman ujian ini lalu coba kembali.",
										"Apabila kendala masih berlanjut, segera hubungi Admin/Pengawas ujian dengan menyertakan tangkapan layar (screenshot) pesan ini."
								});
					}
					prosesProsesUjian();
				}
			}
		});

		new Thread(new Runnable() {

			/**
			 * Menyiapkan soal ujian di thread latar agar antarmuka tidak membeku.
			 *
			 * <p>Hanya memanggil {@link ProsesUjianHelper#initSoal(Label)}, yang pada gilirannya memanggil
			 * {@link ProsesUjianHelper#initHasilUjian(Label)}. Karena berjalan di luar konteks request ZK,
			 * kedua method itu memakai {@code HibernateUtil.currentNativeSession()} dengan begin/commit
			 * eksplisit, bukan session milik request.</p>
			 *
			 * <p><b>Catatan:</b> thread ini dibuat langsung dengan {@code new Thread(...)} — bukan daemon
			 * dan bukan dari pool — berbeda dengan thread countdown yang sudah dipindah ke
			 * {@link ProsesUjianHelper#COUNTDOWN_SCHEDULER}. Karena umurnya pendek (sekali jalan lalu
			 * selesai), pola ini masih dipertahankan.
			 */
			@Override
			public void run() {
				initSoal(label);
			}
		}).start();
	}

	/**
	 * <h3>Waktu — Thread Countdown Ujian</h3>
	 *
	 * <p>Inner class yang berjalan sebagai thread daemon untuk menghitung mundur waktu sisa ujian
	 * dan mencatat lama pengerjaan secara bersamaan. Thread ini berdetak setiap 1 detik.</p>
	 *
	 * <p><b>Cara kerja:</b> Loop {@code while(!stop)} tidur 1 detik lalu mengurangi satu detik
	 * dari {@code currentTime} (sisa waktu ujian) dan menambah satu detik ke {@link #lamaTime}
	 * (waktu yang sudah digunakan). Bila {@code currentTime} jadi null (ujian tidak dibatasi
	 * waktu), thread berhenti sendiri ({@code stop=true}).</p>
	 *
	 * <p><b>Thread-safety:</b> Field {@code stop} dideklarasikan sebagai {@code Boolean} (objek)
	 * dan diset via {@link #setStop(Boolean)}. Akses dari luar melalui setter ini. Perubahan
	 * {@code currentTime} dari luar class juga tidak disinkronisasi — ini adalah trade-off yang
	 * diterima karena presisi detik tidak kritis dalam konteks ujian.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk menghentikan thread ini, panggil {@code setStop(true)} atau
	 * set {@code currentTime} ke null. Thread berhenti paling lama dalam 1 detik berikutnya.
	 * Tidak perlu interrupt karena sleep pendek dan flag stop diperiksa setiap iterasi.</p>
	 */
	class Waktu implements Runnable {

		/** Flag penghenti thread. Bila true, loop {@link #run()} akan berhenti setelah sleep berikutnya. */
		private Boolean stop = false;

		/** Waktu sisa ujian sebagai Calendar (detik terus dikurangi). Null = tidak dibatasi waktu. */
		private Calendar currentTime;

		/** Handle task tick di scheduler bersama; dibatalkan saat ujian berhenti. */
		private java.util.concurrent.ScheduledFuture<?> future;

		/**
		 * Mengembalikan objek Calendar yang merepresentasikan sisa waktu ujian saat ini.
		 * Nilai ini terus berkurang satu detik setiap iterasi {@link #run()}.
		 * @return Calendar sisa waktu; null bila ujian tidak dibatasi waktu
		 */
		public Calendar getCurrentTime() {
			return currentTime;
		}

		/**
		 * Membuat instance Waktu dengan waktu awal yang ditentukan.
		 * @param currentTime Calendar yang merepresentasikan total waktu ujian;
		 *                    akan dikurangi satu detik per iterasi thread
		 */
		public Waktu(Calendar currentTime) {
			this.currentTime = currentTime;
		}

		/**
		 * Menghentikan thread countdown. Setelah dipanggil dengan {@code true}, loop run()
		 * akan berhenti setelah sleep 1 detik berikutnya.
		 * @param stop true untuk menghentikan thread, false untuk melanjutkan
		 */
		public void setStop(Boolean stop) {
			this.stop = stop;
		}

		/**
		 * Loop utama thread countdown. Berjalan terus setiap 1 detik hingga {@link #stop} menjadi true.
		 *
		 * <p><b>Cara kerja per iterasi:</b></p>
		 * <ol>
		 *   <li>Tidur 1 detik ({@code Thread.sleep(1000)}).</li>
		 *   <li>Jika {@code currentTime} tidak null: kurangi satu detik dari {@code currentTime}
		 *       (countdown mundur) dan tambah satu detik ke {@link #lamaTime} (stopwatch maju).
		 *       Catatan: {@code lamaTime.set(SECOND, currentTime.get(SECOND) + 1)} terlihat aneh
		 *       (pakai detik dari currentTime, bukan tambah dari lamaTime), namun ini adalah
		 *       implementasi yang sudah ada dan dipakai untuk menghitung lama pengerjaan relatif
		 *       terhadap sisa waktu.</li>
		 *   <li>Jika {@code currentTime} null: set {@code stop=true} untuk keluar dari loop.</li>
		 * </ol>
		 *
		 * <p><b>Interaksi dengan ZK Timer:</b> Thread ini berjalan secara independen dari ZK Timer
		 * (interval 1 detik). ZK Timer yang terpasang di jendela CBT membaca {@code currentTime}
		 * dan memperbarui label countdown di UI. Thread Waktu hanya mengubah nilai Calendar;
		 * rendering ke UI dilakukan oleh ZK event thread melalui Timer.</p>
		 */
		/**
		 * <b>Catatan pemeliharaan:</b> blok komentar panjang tepat DI ATAS blok ini masih menjelaskan
		 * loop {@code while(!stop)} versi lama dari {@link #run()}. Sejak countdown dipindahkan ke
		 * {@link ProsesUjianHelper#COUNTDOWN_SCHEDULER}, blok tersebut menjadi yatim — Javadoc yang
		 * berlaku bagi sebuah method hanyalah blok TERAKHIR sebelum deklarasinya — dan uraiannya
		 * sudah tidak lagi sesuai implementasi. Dokumentasi {@code run()} yang berlaku berada tepat
		 * di atas method {@code run()} itu sendiri.
		 *
		 * Jadwalkan tick countdown 1 detik pada {@link ProsesUjianHelper#COUNTDOWN_SCHEDULER}
		 * (pengganti {@code new Thread(this).start()}). Tick pertama setelah 1 detik — sama
		 * dengan loop lama yang sleep dulu baru mengurangi.
		 */
		public void mulai() {
			try {
				future = COUNTDOWN_SCHEDULER.scheduleAtFixedRate(this, 1, 1, java.util.concurrent.TimeUnit.SECONDS);
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1464");
				// Bila scheduler bermasalah, jangan gagalkan ujian (tampilan tetap jalan via ZK Timer).
			}
		}

		/** Batalkan penjadwalan tick (saat stop/ujian selesai) agar tak ada task menggantung. */
		private void batalkan() {
			try {
				if (future != null) {
					future.cancel(false);
					future = null;
				}
			} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1476");
			}
		}

		/**
		 * SATU tick countdown — dipanggil scheduler bersama tiap 1 detik. Logika kurang-1-detik
		 * IDENTIK dengan loop lama; hanya wadah eksekusinya yang berubah (pool bersama, bukan
		 * satu thread per ujian). Dibungkus try/Throwable agar kegagalan satu tick tidak
		 * menghentikan penjadwalan (scheduleAtFixedRate berhenti diam-diam bila task melempar).
		 */
		public void run() {
			try {
				if (Boolean.TRUE.equals(stop)) {
					batalkan();
					return;
				}
				if (currentTime != null) {
					currentTime.set(Calendar.SECOND, currentTime.get(Calendar.SECOND) - 1);

					lamaTime.set(Calendar.SECOND, currentTime.get(Calendar.SECOND) + 1);
				} else {
					stop = true;
					batalkan();
				}
			} catch (Throwable e) {
				Common.tampilErrorJikaAdmin(e instanceof Exception ? (Exception) e : new Exception(e));
			}
		}
	}

	/**
	 * Membangun panel navigasi "Nomor Soal" yang ditampilkan di antarmuka CBT.
	 *
	 * <p>Panel ini berisi lingkaran bernomor — satu lingkaran per soal — dengan kode warna:</p>
	 * <ul>
	 *   <li><b>Biru</b> — soal yang sedang dikerjakan sekarang.</li>
	 *   <li><b>Hijau</b> — soal yang sudah dijawab.</li>
	 *   <li><b>Abu-abu</b> — soal yang belum dijawab.</li>
	 * </ul>
	 * <p>Klik lingkaran mana pun untuk langsung melompat ke soal tersebut tanpa harus
	 * menekan Kembali/Lanjut berulang kali. Tooltip pada setiap lingkaran menampilkan
	 * keterangan status soal secara eksplisit.</p>
	 *
	 * <p><b>Perbaikan visual (v2):</b></p>
	 * <ul>
	 *   <li>Lingkaran dirender via {@code display:inline-flex; align-items:center;
	 *       justify-content:center} sehingga angka selalu tepat di tengah, tidak terpotong
	 *       atau bergeser seperti pada implementasi {@code Toolbarbutton} sebelumnya.</li>
	 *   <li>Ukuran naik dari 24 px ke 36 px — lebih mudah di-tap di layar sentuh/mobile.</li>
	 *   <li>Container menggunakan {@code flex-wrap:wrap} sehingga otomatis menyesuaikan lebar
	 *       panel tanpa overflow pada ujian dengan banyak soal.</li>
	 *   <li>Legend warna ditampilkan di bawah panel agar peserta memahami arti tiap warna
	 *       tanpa perlu menebak.</li>
	 * </ul>
	 *
	 * <p><b>Perbaikan bug (v2):</b> Logika deteksi {@code telahDijawab} sebelumnya berpotensi
	 * melempar {@code NullPointerException} karena memeriksa {@code getJawaban().isEmpty()}
	 * sebelum memastikan {@code getBankSoalDetail() != null}. Kini diekstrak ke method
	 * {@link #isTelahDijawab(UjianPunyaSoal)} yang null-safe dan lebih mudah diuji.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Dipanggil dari {@link #doProcessUjian(int, boolean)} setiap
	 * kali soal berganti untuk memperbarui status warna. Bila jumlah soal sangat besar (>100),
	 * pertimbangkan update parsial (hanya ubah class lingkaran aktif/sebelumnya) untuk
	 * mengurangi render ulang seluruh panel.</p>
	 *
	 * @param ujianPunyaSoalidParam ID {@link UjianPunyaSoal} yang sedang aktif; null bila tidak ada
	 * @return {@code Groupbox} siap-pakai untuk ditambahkan ke layout CBT
	 */
	private Groupbox tampilNomorSoal(final Long ujianPunyaSoalidParam) {

		Groupbox groupbox = new MyGroupboxStyled();
		groupbox.appendChild(new Caption("Nomor Soal"));

		// ── CSS lingkaran — injeksi satu kali per render panel ───────────────────
		// Menggunakan display:inline-flex agar angka SELALU tepat di tengah lingkaran.
		// !important diperlukan untuk mengalahkan style default z-groupbox ZK 5.5.
		new Html(
			"<style>" +
			".ais-nsb-wrap{display:flex;flex-wrap:wrap;gap:5px;padding:8px 6px 6px;align-items:center;}" +
			".ais-nsb{display:inline-flex!important;align-items:center!important;justify-content:center!important;" +
			  "width:36px;height:36px;min-width:36px;border-radius:50%;font-size:12px;font-weight:800;" +
			  "cursor:pointer;background:#f1f5f9;color:#475569;border:2px solid #cbd5e1;" +
			  "box-shadow:0 1px 3px rgba(0,0,0,.08);transition:transform .12s,box-shadow .12s;" +
			  "text-decoration:none;user-select:none;}" +
			".ais-nsb:hover{transform:scale(1.15);box-shadow:0 3px 10px rgba(0,0,0,.2);}" +
			".ais-nsb-ok{background:#16a34a!important;color:#fff!important;border-color:#15803d!important;}" +
			".ais-nsb-now{background:#1d4ed8!important;color:#fff!important;border-color:#1e40af!important;" +
			  "box-shadow:0 0 0 4px rgba(29,78,216,.25)!important;}" +
			".ais-nsb .z-label{margin:0;padding:0;line-height:1;pointer-events:none;font-size:inherit;" +
			  "font-weight:inherit;color:inherit;}" +
			".ais-nsb-leg{display:flex;flex-wrap:wrap;gap:10px;padding:5px 6px 8px;border-top:1px solid #e2e8f0;margin-top:4px;}" +
			".ais-nsb-li{display:flex;align-items:center;gap:4px;font-size:11px;color:#64748b;}" +
			".ais-nsb-dot{width:11px;height:11px;border-radius:50%;display:inline-block;flex:0 0 auto;}" +
			"</style>"
		).setParent(groupbox);

		// ── Flex container untuk lingkaran-lingkaran ──────────────────────────────
		final org.zkoss.zul.Div wrap = new org.zkoss.zul.Div();
		wrap.setSclass("ais-nsb-wrap");
		wrap.setParent(groupbox);

		if (ujianPunyaSoals != null && !ujianPunyaSoals.isEmpty()) {
			int index = 1;
			for (final Long ujianPunyaSoalid : ujianPunyaSoals) {
				try {
					UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(
							UjianPunyaSoal.class, ujianPunyaSoalid.toString());
					if (ups == null) { index++; continue; }

					boolean aktif    = ujianPunyaSoalid.equals(ujianPunyaSoalidParam);
					boolean terjawab = !aktif && isTelahDijawab(ups);

					String btnClass = "ais-nsb" + (aktif ? " ais-nsb-now" : (terjawab ? " ais-nsb-ok" : ""));
					String tooltip  = "Soal " + index
							+ (aktif    ? " — Sedang dikerjakan"
							: (terjawab ? " — Sudah dijawab"
							             : " — Belum dijawab"));

					final org.zkoss.zul.Div btn = new org.zkoss.zul.Div();
					btn.setSclass(btnClass);
					btn.setTooltiptext(tooltip);
					new Label("" + index).setParent(btn);

					btn.addEventListener("onClick", new EventListener() {
						/**
						 * Melompat ke soal yang lingkarannya diklik pada panel "Nomor Soal".
						 *
						 * <p>Menelusuri {@link ProsesUjianHelper#ujianPunyaSoals} untuk menemukan indeks soal yang
						 * cocok dengan {@code ujianPunyaSoalid} lingkaran ini, memperbarui {@code index} milik
						 * instance, lalu me-render ulang soal lewat {@link ProsesUjianHelper#doProcessUjian(int)}.
						 * Render ulang otomatis membangun kembali panel nomor soal sehingga warna lingkaran (biru =
						 * sedang dikerjakan, hijau = sudah dijawab, abu-abu = belum) ikut diperbarui.</p>
						 *
						 * @param e event {@code onClick} ZK dari div lingkaran nomor soal
						 * @throws Exception bila render ulang soal gagal
						 */
						@Override
						public void onEvent(Event e) throws Exception {
							int idx = 0;
							for (Long sid : ujianPunyaSoals) {
								UjianPunyaSoal s = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, sid.toString());
								if (s != null && s.getId() != null && s.getId().equals(ujianPunyaSoalid)) {
									ProsesUjianHelper.this.index = idx;
									doProcessUjian(idx);
									break;
								}
								idx++;
							}
						}
					});

					btn.setParent(wrap);
					index++;
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1618");
					// abaikan soal yang gagal di-render; lanjut ke soal berikutnya
				}
			}
		}

		// ── Legend warna ──────────────────────────────────────────────────────────
		new Html(
			"<div class='ais-nsb-leg'>" +
			"<span class='ais-nsb-li'><i class='ais-nsb-dot' style='background:#1d4ed8;'></i>Sedang dikerjakan</span>" +
			"<span class='ais-nsb-li'><i class='ais-nsb-dot' style='background:#16a34a;'></i>Sudah dijawab</span>" +
			"<span class='ais-nsb-li'><i class='ais-nsb-dot' style='background:#f1f5f9;border:1.5px solid #cbd5e1;'></i>Belum dijawab</span>" +
			"</div>"
		).setParent(groupbox);

		return groupbox;
	}

	/**
	 * Memeriksa apakah soal tertentu sudah dijawab oleh peserta ujian yang sedang aktif.
	 *
	 * <p>Method ini adalah versi null-safe dari pemeriksaan yang sebelumnya dilakukan secara
	 * inline di dalam {@link #tampilNomorSoal}. Versi lama berpotensi melempar
	 * {@code NullPointerException} karena memanggil {@code getJawaban().isEmpty()} sebelum
	 * memastikan {@code getBankSoalDetail()} tidak null — urutan pengecekan null yang keliru
	 * dalam ekspresi OR bercabang.</p>
	 *
	 * <p><b>Logika deteksi terjawab:</b></p>
	 * <ul>
	 *   <li><b>Pilihan Ganda / Benar-Salah:</b> field {@code jawaban} di {@code BankSoalDetail}
	 *       tidak kosong (peserta sudah memilih opsi).</li>
	 *   <li><b>Esai:</b> field {@code jawaban} tidak kosong (peserta sudah mengetik teks).</li>
	 *   <li><b>Jenis lain:</b> dianggap terjawab apabila {@code jawaban} tidak null dan tidak
	 *       kosong (fallback aman).</li>
	 * </ul>
	 *
	 * <p><b>KOREKSI terhadap daftar di atas — perilaku NYATA implementasi:</b> loop di dalam
	 * method ini melewati ({@code continue}) setiap {@code HasilUjianMahasiswaDetail} yang
	 * {@code getBankSoalDetail()}-nya null. Padahal jawaban ESAI, isian singkat, rumpang,
	 * menjodohkan, dan mengurutkan justru TIDAK memiliki {@code bankSoalDetail} — teks
	 * jawabannya tersimpan di kolom {@code jawaban} milik detail itu sendiri. Akibatnya method
	 * ini hanya pernah mengembalikan {@code true} untuk soal PILIHAN GANDA dan BENAR-SALAH.</p>
	 *
	 * <p><b>Dampaknya terbatas pada tampilan.</b> Satu-satunya pemakai method ini adalah
	 * {@link #tampilNomorSoal(Long)}, yang memakainya untuk mewarnai lingkaran nomor soal. Jadi
	 * pada ujian esai, lingkaran nomor soal tidak pernah berubah hijau meskipun jawaban sudah
	 * tersimpan. Perhitungan nilai, daftar "Telah terjawab"/"Belum terjawab", indikator
	 * "Tuntas n/N", dan penjagaan kelengkapan pada tombol "Selesaikan Ujian" TIDAK memakai
	 * method ini — semuanya memakai {@code hasilUjianMahasiswa.ambilBankSoalIdTerjawab(...)} —
	 * sehingga integritas nilai peserta tidak terpengaruh.</p>
	 *
	 * <p><b>Thread safety:</b> Murni read-only terhadap cache
	 * {@link #hasilUjianMahasiswaDetailsa} dan {@code GeneralValueObject} yang keduanya
	 * berbasis HashMap. Aman dipanggil dari ZK event thread.</p>
	 *
	 * @param ups soal ujian yang akan diperiksa; tidak boleh null
	 * @return {@code true} bila peserta sudah memberikan jawaban untuk soal ini;
	 *         {@code false} bila belum dijawab atau terjadi kesalahan saat membaca data
	 */
	private boolean isTelahDijawab(UjianPunyaSoal ups) {
		if (hasilUjianMahasiswa == null || ups == null || ups.getBankSoal() == null) return false;
		Set<Long> ids = hasilUjianMahasiswaDetailsa.get(ups.getBankSoal().getId());
		if (ids == null || ids.isEmpty()) return false;
		for (Long id : ids) {
			try {
				HasilUjianMahasiswaDetail detail = (HasilUjianMahasiswaDetail)
						GeneralValueObject.ambilData(HasilUjianMahasiswaDetail.class, id.toString());
				if (detail == null || detail.getBankSoalDetail() == null) continue;
				String jawaban = detail.getBankSoalDetail().getJawaban();
				if (jawaban != null && !jawaban.isEmpty()) {
					return true;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1675");
				// abaikan detail yang gagal dibaca; lanjut ke detail berikutnya
			}
		}
		return false;
	}

	/**
	 * Membangun seluruh antarmuka CBT setelah soal selesai disiapkan. Ini adalah method
	 * "terbesar" dalam kelas — ia membangun layout Borderlayout penuh dengan panel soal,
	 * timer, navigasi, dan semua tombol aksi ujian.
	 *
	 * <p><b>Tujuan:</b> Setelah {@link #initSoal} dan {@link #initHasilUjian} berhasil,
	 * method ini membangun tampilan CBT yang sesungguhnya: soal di tengah, nomor soal di
	 * kanan/kiri, countdown di atas, dan toolbar aksi di bawah.</p>
	 *
	 * <p><b>Cara kerja (komponen yang dibangun):</b></p>
	 * <ol>
	 *   <li><b>North panel:</b> Menampilkan informasi ujian (nama, jenis, sisa waktu). Timer ZK
	 *       1-detik dipasang untuk update countdown real-time. Bila ujian dibatasi waktu,
	 *       {@link Waktu} thread dimulai dan timer event ZK menampilkan nilai countdownnya.</li>
	 *   <li><b>West panel (opsional):</b> Panel penjelasan soal untuk mode hanyaLihat=true.
	 *       Disembunyikan bila tidak relevan.</li>
	 *   <li><b>Center panel:</b> Grid soal yang menampilkan soal aktif. Diisi ulang oleh
	 *       {@link #doProcessUjian(int)} setiap navigasi.</li>
	 *   <li><b>East panel (nomor soal):</b> Panel navigasi dari {@link #tampilNomorSoal}.</li>
	 *   <li><b>South panel (toolbar):</b> Tombol Sebelumnya/Berikutnya, Simpan Sementara,
	 *       Akhiri Ujian, dan Rekap Jawaban.</li>
	 *   <li><b>Tab jawaban:</b> Panel yang memuat daftar soal sudah/belum dijawab dengan
	 *       paging. Dirender via {@link #reloadTelahDikerjakan} dan
	 *       {@link #reloadBelumDikerjakan}.</li>
	 *   <li><b>Anti-curang:</b> Script JavaScript CBT diinjeksikan ke browser via
	 *       {@code Clients.evalJavaScript(buildCbtAntiCheatScript(sinkUuid))} bila konfigurasi
	 *       anti-curang aktif.</li>
	 *   <li>Memanggil {@link #doProcessUjian(int, boolean) doProcessUjian(0, false)} di akhir
	 *       untuk menampilkan soal pertama.</li>
	 * </ol>
	 *
	 * <p><b>Thread-safety:</b> Dipanggil dari ZK event thread (callback di dalam
	 * {@code Common.displayLoadBar}), sehingga aman memanipulasi komponen ZK.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini panjang (~500 baris) karena kompleksitas UI CBT.
	 * Bila menambah fitur baru ke antarmuka ujian (misal webcam, chat proktor), tambahkan
	 * komponen baru di sini di posisi yang tepat dalam layout. Hindari memanggil method ini
	 * lebih dari sekali karena akan membuat duplikat komponen di jendela.</p>
	 */
	private void prosesProsesUjian() {

		Clients.confirmClose(Common.getBahasaConfig("Apakah Anda yakin ingin keluar dari ujian ini ?"));

		if (hasilUjianMahasiswa != null && !hanyaLihat) {
			// Komponen tersembunyi penerima event pelanggaran dari JS (anti-curang).
			// Setiap pelanggaran di browser dikirim ke sini lalu DISIMPAN ke DB (jumlah + log),
			// agar bisa di-Rekap oleh dosen/guru. Dibungkus aman (tak mengganggu ujian bila gagal).
			String sinkUuid = null;
			try {
				final org.zkoss.zul.Div pelanggaranSink = new org.zkoss.zul.Div();
				pelanggaranSink.setVisible(false);
				pelanggaranSink.setParent(
						org.zkoss.zk.ui.sys.ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				final Long hasilId = hasilUjianMahasiswa.getId();
				pelanggaranSink.addEventListener("onPelanggaran", new EventListener() {
					/**
					 * Menerima laporan pelanggaran anti-curang yang dikirim dari browser peserta.
					 *
					 * <p>Skrip anti-curang di browser (lihat {@code buildCbtAntiCheatScript}) mengirim event ZK
					 * {@code onPelanggaran} ke komponen tersembunyi {@code pelanggaranSink} setiap kali mendeteksi
					 * perpindahan tab, blur jendela/Alt+Tab, atau keluar dari layar penuh. Listener ini
					 * meneruskannya ke {@link ProsesUjianHelper#catatPelanggaranUjian(Long,String)} yang menaikkan
					 * {@code jumlah_pelanggaran} dan menambah satu baris ke {@code log_pelanggaran} pada
					 * {@code HasilUjianMahasiswa}.</p>
					 *
					 * <p><b>Mengapa memakai {@code hasilId} dan bukan entitasnya:</b> id di-<i>capture</i> sebagai
					 * {@code Long} agar pencatatan dapat memakai session Hibernate terdedikasi walaupun session
					 * ujian sudah ditutup. Bila {@code e.getData()} null, jenis pelanggaran dicatat sebagai teks
					 * umum {@code "Pelanggaran"}.</p>
					 *
					 * <p><b>FAKTA arsitektur (bukan bug):</b> batas jumlah pelanggaran TIDAK ditegakkan di sisi
					 * server. Penghentian otomatis ujian ketika batas tercapai dilakukan oleh skrip di browser
					 * (yang mengklik tombol "Selesaikan Ujian"); server hanya MENCATAT. Peserta yang mematikan
					 * JavaScript karenanya tidak akan tercatat maupun dihentikan — pengawasan ini bersifat
					 * pencegah dan pembukti, bukan penjamin.</p>
					 *
					 * @param e event ZK {@code onPelanggaran}; {@code getData()} berisi teks jenis pelanggaran
					 * @throws Exception bila pencatatan melempar kesalahan yang tidak tertangkap
					 */
					@Override
					public void onEvent(Event e) throws Exception {
						catatPelanggaranUjian(hasilId,
								(e == null || e.getData() == null) ? "Pelanggaran" : e.getData().toString());
					}
				});
				sinkUuid = pelanggaranSink.getUuid();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1744");
				// abaikan: pencatatan pelanggaran tidak boleh menggagalkan ujian
			}

			String cbtScript = buildCbtAntiCheatScript(pertemuanPunyaUjian, sinkUuid, null);
			if (cbtScript != null && !cbtScript.isEmpty()) {
				Clients.evalJavaScript(cbtScript);
			}
		}

		north = new North();
		north.setVisible(hasilUjianMahasiswa != null && pertemuanPunyaUjian != null
				&& pertemuanPunyaUjian.getDibatasiWaktu() != null && pertemuanPunyaUjian.getDibatasiWaktu()
				&& pertemuanPunyaUjian.getLama() != null);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		if (pertemuanPunyaUjian.getLama() != null) {
			calendar.setTime(pertemuanPunyaUjian.getLama());
		}
		lamaTime = ais.ui.util.WaktuUtil.getCalendar();
		lamaTime.set(Calendar.DATE, 1);
		lamaTime.set(Calendar.MONTH, 1);
		lamaTime.set(Calendar.HOUR_OF_DAY, 0);
		lamaTime.set(Calendar.SECOND, 0);
		lamaTime.set(Calendar.MINUTE, 0);

		final Calendar currentTimeTemp = Calendar.getInstance();
		currentTimeTemp.set(Calendar.DATE, 1);
		currentTimeTemp.set(Calendar.MONTH, 1);
		currentTimeTemp.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY));
		currentTimeTemp.set(Calendar.SECOND, 0);
		currentTimeTemp.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE));
		if (pertemuanPunyaUjian.getLama() != null) {
			currentTimeTemp.setTime(pertemuanPunyaUjian.getLama());
		}
		if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getJumlahIkut() > 1
				&& hasilUjianMahasiswa.getSisaWaktuPengerjaan() != null) {
			currentTimeTemp.setTime(hasilUjianMahasiswa.getSisaWaktuPengerjaan());
		}

		if (hasilUjianMahasiswa != null) {
			String yglalu = hasilUjianMahasiswa.retreive();
			if (yglalu != null && !yglalu.trim().isEmpty()) {
				try {
					Date timeLalu = Common.databaseDateFormat1.get().parse(yglalu);
					System.out.println("resume waktu yg lalu -> " + yglalu + ", timeLalu -> " + timeLalu);
					currentTimeTemp.setTime(timeLalu);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:1791");
//					e.printStackTrace();
				}
			}
		}

		System.out.println("hanyaLihat -> " + hanyaLihat);
		waktuTimer = new Waktu(currentTimeTemp);
		if (!hanyaLihat && pertemuanPunyaUjian.getDibatasiWaktu()) {
			waktuTimer.mulai();
			timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			timer.setRepeats(true);
			timer.addEventListener("onTimer", new EventListener() {

				/**
				 * Tick ZK 1 detik: memperbarui tampilan sisa waktu, menyimpan titik-pulih berkala, dan
				 * menghentikan ujian saat waktu habis.
				 *
				 * <p><b>Yang dikerjakan setiap detik:</b></p>
				 * <ol>
				 *   <li>Membaca jam/menit/detik dari {@link Waktu#getCurrentTime()} — nilai yang dikurangi satu
				 *       detik oleh {@link ProsesUjianHelper#COUNTDOWN_SCHEDULER}, sehingga sumber kebenaran
				 *       waktu berada di SISI SERVER, bukan di klien — lalu menuliskannya ke label countdown.</li>
				 *   <li>Pada setiap kelipatan 10 detik, sisa waktu dan indeks soal aktif dititipkan ke cache
				 *       peserta ({@code hasilUjianMahasiswa.put(...)}) sebagai titik pemulihan bila sesi
				 *       terputus — nilai inilah yang dibaca kembali oleh
				 *       {@link ProsesUjianHelper#prosesProsesUjian()} lewat {@code retreive()}.</li>
				 *   <li>Bila ujian sungguhan, {@code lamaPengerjaan} diperbarui dan cache
				 *       {@code hasilUjianMahasiswa} pada entitas peserta (mahasiswa / calon mahasiswa /
				 *       calon siswa / siswa) di-invalidasi agar layar lain melihat data terkini.</li>
				 *   <li><b>Deteksi waktu habis:</b> bila jam &gt; 23 (akibat <i>underflow</i> Calendar ketika
				 *       sisa waktu menembus nol) ATAU jam, menit, dan detik semuanya nol, maka pada ujian
				 *       bermode "tiap soal punya waktu sendiri" tombol Lanjut ditekan otomatis selama masih
				 *       ada soal berikutnya; selain itu peserta diberi pesan dan
				 *       {@link ProsesUjianHelper#onSelesai()} dipanggil sehingga jawaban yang SUDAH tersimpan
				 *       langsung difinalisasi.</li>
				 * </ol>
				 *
				 * <p><b>Penanganan error:</b> seluruh badan listener dibungkus try-catch — kegagalan satu tick
				 * (misalnya {@code waktuTimer.getCurrentTime()} sempat null) hanya dicatat dan tidak
				 * menghentikan timer ZK.</p>
				 *
				 * @param arg0 event {@code onTimer} ZK (isinya tidak dipakai)
				 * @throws Exception tidak pernah lolos keluar; seluruh kesalahan ditangkap di dalam
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						Integer second = waktuTimer.getCurrentTime().get(Calendar.SECOND);
						Integer minute = waktuTimer.getCurrentTime().get(Calendar.MINUTE);
						Integer hour = waktuTimer.getCurrentTime().get(Calendar.HOUR_OF_DAY);

						waktu.setValue(
								label + " : " + Common.timeFormat1.get().format(waktuTimer.getCurrentTime().getTime()));

						if (hasilUjianMahasiswa != null && (second % 10 == 0)) {
							String waktu = Common.databaseDateFormat1.get().format(waktuTimer.getCurrentTime().getTime());
//							System.out.println("Simpan waktu " + waktu);
							hasilUjianMahasiswa.put(waktu);
							hasilUjianMahasiswa.put(ProsesUjianHelper.this.index + "", "index");
						}

						if (!hanyaLihat && pertemuanPunyaUjian != null && hasilUjianMahasiswa != null) {
							hasilUjianMahasiswa.setLamaPengerjaan(waktuTimer.getCurrentTime().getTime());
							if (hasilUjianMahasiswa.getMahasiswa() != null) {
								hasilUjianMahasiswa.getMahasiswa().put(hasilUjianMahasiswa.getId().toString(),
										"hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
								hasilUjianMahasiswa.getBiodataCalonMahasiswa()
										.put(hasilUjianMahasiswa.getId().toString(), "hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getCalonSiswa() != null) {
								hasilUjianMahasiswa.getCalonSiswa().put(hasilUjianMahasiswa.getId().toString(),
										"hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getSiswa() != null) {
								hasilUjianMahasiswa.getSiswa().put(hasilUjianMahasiswa.getId().toString(),
										"hasilUjianMahasiswa");
							}
						}

						if (hour > 23 || (second.equals(0) && minute.equals(0) && hour.equals(0))) {
							if (timer.isRunning()) {
								if (pertemuanPunyaUjian.getTiapSoal()) {
									if (!next.isDisabled()) {
										ProsesUjianHelper.this.index = ProsesUjianHelper.this.index
												+ jumlahSoalPerHalaman;
										doProcessUjian(ProsesUjianHelper.this.index);
									} else {
										MyMessageboxConfig.show(
				"Mohon maaf, waktu pengerjaan ujian telah selesai. Jawaban Anda akan diproses sebagaimana yang telah tersimpan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
										onSelesai();
									}
								} else {

									MyMessageboxConfig.show(
				"Mohon maaf, waktu pengerjaan ujian telah selesai. Jawaban Anda akan diproses sebagaimana yang telah tersimpan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
									onSelesai();
								}
							}

						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:1864");
					}

				}
			});

			timer.start();
		}

		Borderlayout borderlayoutUtama = new Borderlayout();
		borderlayoutUtama.setParent(this);

		Center centerUtama = new Center();
		centerUtama.setParent(borderlayoutUtama);
		ais.ui.util.ZkCompat.setFlex(centerUtama, true);
		// Di HP, scroll ditangani lewat CSS pada css_utama.css (blok "ais-ujian-mobile") agar area
		// soal dapat digulir; struktur Borderlayout dibiarkan seperti desktop (tidak diberi tinggi
		// tetap yang justru memotong konten & menghilangkan scrollbar).
		if (Common.isMobile()) {
			this.setSclass((getSclass() == null || getSclass().trim().isEmpty() ? "" : getSclass() + " ")
					+ "ais-ujian-mobile");
		}

		Borderlayout borderlayout = new Borderlayout();

		if (!pertemuanPunyaUjian.getTiapSoal()) {
			Tabbox tabbox = new Tabbox();
			tabbox.setParent(centerUtama);
			tabbox.setHeight("100%");
			tabbox.setWidth("100%");

			Tabs tabs = new Tabs();
			tabs.setParent(tabbox);

			tabSoal = new MyTabConfig("Soal");
			tabSoal.setParent(tabs);

			tabJawaban = new MyTabConfig("Belum terjawab");
			tabJawaban.setParent(tabs);

			final MyTabConfig tabTelahJawaban = new MyTabConfig("Telah terjawab");
			tabTelahJawaban.setParent(tabs);

			MyTabConfig tabStatistik = new MyTabConfig("Statistik");
			tabStatistik.setParent(tabs);

			Tabpanels tabpanels = new Tabpanels();
			tabpanels.setParent(tabbox);

			Tabpanel tabpanelUtama = new ais.ui.util.MyTabpanel();
			tabpanelUtama.setStyle("min-height: 550px;");
			tabpanelUtama.setParent(tabpanels);

			final Tabpanel tabpanelBelumDijawab = new ais.ui.util.MyTabpanel();
			tabpanelBelumDijawab.setStyle("min-height: 550px;");
			tabpanelBelumDijawab.setParent(tabpanels);
			belumDijawabEventListener = new EventListener() {

				/**
				 * Mengembalikan referensi ke listener ini sendiri, supaya tombol "Refresh" di dalam panel
				 * dapat memanggil ulang {@code onEvent} untuk membangun ulang daftar soal.
				 *
				 * <p>Idiom ini diperlukan karena kelas anonim tidak memiliki nama yang dapat dirujuk dari
				 * kelas anonim bersarang di dalamnya — {@code ProsesUjianHelper.this} menunjuk ke jendela
				 * ujian, bukan ke listener ini.</p>
				 *
				 * @return listener ini sendiri ({@code this})
				 */
				private EventListener get() {
					return this;
				}

				/**
				 * Membangun ulang tab "Belum terjawab": daftar soal yang belum dijawab peserta beserta
				 * paginasinya.
				 *
				 * <p><b>Cara kerja:</b> isi tab dibersihkan lebih dahulu, lalu seluruh perakitan panel
				 * ditunda ke {@code Common.createDefaultTimer} agar tab terasa responsif (ZK sempat mengirim
				 * tampilan kosong ke klien sebelum query berat dijalankan).</p>
				 *
				 * <p><b>Arti parameter {@code arg0}:</b> menentukan apakah daftar id soal terjawab diambil
				 * SEGAR dari basis data atau cukup dari cache. Segar bila event berasal dari klik tab
				 * {@code tabJawaban} itu sendiri, atau bila {@code arg0.getData()} bertipe {@code Boolean} —
				 * jalur yang dipakai tombol Refresh di dalam panel lewat
				 * {@code get().onEvent(new Event("", null, true))}.</p>
				 *
				 * <p>Listener ini juga dipakai ulang sebagai callback pesan "masih ada N soal belum dijawab"
				 * pada tombol "Selesaikan Ujian"; itulah sebabnya ia disimpan ke field
				 * {@link ProsesUjianHelper#belumDijawabEventListener}.</p>
				 *
				 * @param arg0 event ZK pemicu; boleh null (dianggap memakai cache)
				 * @throws Exception bila pembangunan komponen ZK gagal
				 */
				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(final Event arg0) throws Exception {
					Common.clear(tabpanelBelumDijawab);

					Common.createDefaultTimer(new EventListener() {

						/**
						 * Merakit isi panel "Belum terjawab" pada siklus event ZK berikutnya.
						 *
						 * <p>Membangun Borderlayout lengkap: toolbar Refresh di North, grid daftar soal di Center,
						 * dan {@link ProsesUjianHelper#pagingBelumTerjawab} (5 baris per halaman) di bagian bawah.
						 * Pengisian barisnya didelegasikan ke
						 * {@link ProsesUjianHelper#reloadBelumDikerjakan(Set,int,int,int)}.</p>
						 *
						 * <p>Himpunan {@code idsa} berisi id {@code BankSoal} yang SUDAH terjawab; panel ini
						 * menampilkan komplemennya, yaitu soal yang id-nya TIDAK ada di {@code idsa}.</p>
						 *
						 * @param a event timer ZK sekali-jalan (isinya tidak dipakai)
						 * @throws Exception bila query atau pembangunan komponen gagal
						 */
						@Override
						public void onEvent(Event a) throws Exception {

							final Set<Long> idsa = hasilUjianMahasiswa == null ? new HashSet<Long>()
									: hasilUjianMahasiswa.ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoals,
											(arg0 != null && arg0.getTarget() == tabJawaban) ? true
													: (arg0 != null && arg0.getData() instanceof Boolean));

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(tabpanelBelumDijawab);

							North north = new North();
							north.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(north, true);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(north);
							MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh",
									"/img/Button-Refresh-icon.png");
							cari.setParent(toolbar);
							cari.addEventListener("onClick", new EventListener() {

								/**
								 * Membangun ulang daftar "Belum terjawab" dengan data SEGAR dari basis data.
								 *
								 * <p>Memanggil {@code get().onEvent(new Event("", null, true))}; data {@code Boolean.TRUE}
								 * itulah yang memberi tahu listener induk agar mengambil ulang id soal terjawab dari basis
								 * data alih-alih memakai cache.</p>
								 *
								 * @param arg0 event {@code onClick} ZK dari tombol Refresh (isinya tidak dipakai)
								 * @throws Exception bila pembangunan ulang panel gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									get().onEvent(new Event("", null, true));
								}
							});

							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);
							Grid grid = new Grid();
							grid.setSclass("dgrid");
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");
							grid.setSclass("fgrid");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("40%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							final MyFormRow rowBelum = new MyFormRow();
							rowBelum.setStyle("border:0px;background: transparent;");
							rowBelum.setParent(rows);
							ais.ui.util.ZkCompat.setSpans(rowBelum, "2");
							rowBelum.appendChild(new MyLabelBold("Daftar Soal yg belum terjawab :"));

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							ais.ui.util.ZkCompat.setSpans(row, "2");
							row.setParent(rows);

							Radiogroup radiogroup = new Radiogroup();
							row.appendChild(radiogroup);

							Grid gripSoalYgBelumDibayar = new Grid();
							gripSoalYgBelumDibayar.setWidth("100%");
							radiogroup.appendChild(gripSoalYgBelumDibayar);

							rowsYgBelumDikerjakan = new Rows();
							gripSoalYgBelumDibayar.appendChild(rowsYgBelumDikerjakan);

							row = new MyFormRow();
							ais.ui.util.ZkCompat.setSpans(row, "2");
							row.setParent(rows);

							final int jumlahDataDalamSatuHalamanElearning = 5;
							Integer size = ujianPunyaSoals.size();
							pagingBelumTerjawab = new Paging();
							row.appendChild(pagingBelumTerjawab);
							pagingBelumTerjawab.setPageSize(jumlahDataDalamSatuHalamanElearning);
							pagingBelumTerjawab.setMold("os");
							pagingBelumTerjawab.setTotalSize(size);
							pagingBelumTerjawab.setVisible(size > jumlahDataDalamSatuHalamanElearning);
							pagingBelumTerjawab.getParent().setVisible(pagingBelumTerjawab.isVisible());
							pagingBelumTerjawab.setActivePage(
									(size / jumlahDataDalamSatuHalamanElearning) > pagingBelumTerjawabActivePage
											? pagingBelumTerjawabActivePage
											: (size / jumlahDataDalamSatuHalamanElearning));
							Common.initPagingCustom(pagingBelumTerjawab, new EventListener() {

								/**
								 * Menangani perpindahan halaman pada daftar "Belum terjawab".
								 *
								 * <p>Menyimpan halaman aktif ke {@link ProsesUjianHelper#pagingBelumTerjawabActivePage} agar
								 * posisi paginasi tidak hilang ketika panel dibangun ulang, lalu memanggil
								 * {@link ProsesUjianHelper#reloadBelumDikerjakan(Set,int,int,int)} dengan offset
								 * {@code 5 * halamanAktif}.</p>
								 *
								 * @param arg0 event paging ZK (isinya tidak dipakai)
								 * @throws Exception bila render ulang baris gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									pagingBelumTerjawabActivePage = pagingBelumTerjawab.getActivePage();
									reloadBelumDikerjakan(idsa, jumlahDataDalamSatuHalamanElearning,
											jumlahDataDalamSatuHalamanElearning * (pagingBelumTerjawab == null ? 0
													: pagingBelumTerjawab.getActivePage()),
											jumlahDataDalamSatuHalamanElearning);
								}
							}, jumlahDataDalamSatuHalamanElearning);

							reloadBelumDikerjakan(idsa, jumlahDataDalamSatuHalamanElearning, 0,
									jumlahDataDalamSatuHalamanElearning);
						}
					});

				}
			};

			tabJawaban.addEventListener("onClick", belumDijawabEventListener);

			final Tabpanel tabpanelTelahDijawab = new ais.ui.util.MyTabpanel();
			tabpanelTelahDijawab.setStyle("min-height: 550px;");
			tabpanelTelahDijawab.setParent(tabpanels);
			tabTelahJawaban.addEventListener("onClick", new EventListener() {

				/**
				 * Mengembalikan referensi ke listener ini sendiri, supaya tombol "Refresh" di dalam panel
				 * "Telah terjawab" dapat memanggil ulang {@code onEvent}.
				 *
				 * <p>Kembaran dari idiom yang sama pada listener tab "Belum terjawab".</p>
				 *
				 * @return listener ini sendiri ({@code this})
				 */
				private EventListener get() {
					return this;
				}

				/**
				 * Membangun ulang tab "Telah terjawab": daftar soal yang sudah dijawab peserta, lengkap
				 * dengan jawaban yang dipilih dan paginasinya.
				 *
				 * <p>Kembaran simetris dari listener tab "Belum terjawab": isi tab dibersihkan, lalu
				 * perakitan panel ditunda ke {@code Common.createDefaultTimer}. Data diambil SEGAR dari basis
				 * data bila event berasal dari klik tab ini sendiri atau bila {@code arg0.getData()} bertipe
				 * {@code Boolean} (jalur tombol Refresh).</p>
				 *
				 * @param arg0 event ZK pemicu; boleh null (dianggap memakai cache)
				 * @throws Exception bila pembangunan komponen ZK gagal
				 */
				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(final Event arg0) throws Exception {
					Common.clear(tabpanelTelahDijawab);

					Common.createDefaultTimer(new EventListener() {

						/**
						 * Merakit isi panel "Telah terjawab" pada siklus event ZK berikutnya.
						 *
						 * <p>Strukturnya identik dengan panel "Belum terjawab" (toolbar Refresh, grid, paging 5
						 * baris); yang berbeda hanya pengisian baris, yang didelegasikan ke
						 * {@link ProsesUjianHelper#reloadTelahDikerjakan(Set,int,int,int)} — menampilkan soal yang
						 * id {@code BankSoal}-nya ADA di {@code idsa}, beserta teks jawaban yang dipilih peserta dan
						 * lampirannya.</p>
						 *
						 * @param a event timer ZK sekali-jalan (isinya tidak dipakai)
						 * @throws Exception bila query atau pembangunan komponen gagal
						 */
						@Override
						public void onEvent(Event a) throws Exception {
							final Set<Long> idsa = hasilUjianMahasiswa == null ? new HashSet<Long>()
									: hasilUjianMahasiswa.ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoals,
											(arg0 != null && arg0.getTarget() == tabTelahJawaban) ? true
													: (arg0 != null && arg0.getData() instanceof Boolean));

							Borderlayout borderlayout = new Borderlayout();
							borderlayout.setParent(tabpanelTelahDijawab);

							North north = new North();
							north.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(north, true);

							Toolbar toolbar = new Toolbar();
							toolbar.setParent(north);
							MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh",
									"/img/Button-Refresh-icon.png");
							cari.setParent(toolbar);
							cari.addEventListener("onClick", new EventListener() {

								/**
								 * Membangun ulang daftar "Telah terjawab" dengan data SEGAR dari basis data.
								 *
								 * <p>Sama seperti tombol Refresh pada panel "Belum terjawab": memanggil
								 * {@code get().onEvent(new Event("", null, true))} sehingga listener induk mengambil ulang
								 * id soal terjawab dari basis data, bukan dari cache.</p>
								 *
								 * @param arg0 event {@code onClick} ZK dari tombol Refresh (isinya tidak dipakai)
								 * @throws Exception bila pembangunan ulang panel gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									get().onEvent(new Event("", null, true));
								}
							});

							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);
							Grid grid = new Grid();
							grid.setSclass("dgrid");
							grid.setWidth("100%");
							grid.setParent(center);
							grid.setWidth("100%");
							grid.setHeight("100%");
							grid.setSclass("fgrid");

							Columns columns = new Columns();
							columns.setParent(grid);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("40%");

							column = new MyColumnConfig();
							column.setParent(columns);

							Rows rows = new Rows();
							rows.setParent(grid);

							final MyFormRow rowSudah = new MyFormRow();
							rowSudah.setStyle("border:0px;background: transparent;");
							rowSudah.setParent(rows);
							ais.ui.util.ZkCompat.setSpans(rowSudah, "2");
							rowSudah.appendChild(new MyLabelBold("Daftar Soal yg telah terjawab :"));

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							ais.ui.util.ZkCompat.setSpans(row, "2");
							row.setParent(rows);

							Radiogroup radiogroup = new Radiogroup();
							row.appendChild(radiogroup);

							Grid gripSoalYgBelumDibayar = new Grid();
							gripSoalYgBelumDibayar.setWidth("100%");
							radiogroup.appendChild(gripSoalYgBelumDibayar);

							rowsYgTelahDikerjakan = new Rows();
							gripSoalYgBelumDibayar.appendChild(rowsYgTelahDikerjakan);

							row = new MyFormRow();
							ais.ui.util.ZkCompat.setSpans(row, "2");
							row.setParent(rows);

							Integer size = ujianPunyaSoals.size();
							final int jumlahDataDalamSatuHalamanElearning = 5;
							pagingTelahTerjawab = new Paging();
							row.appendChild(pagingTelahTerjawab);
							pagingTelahTerjawab.setPageSize(jumlahDataDalamSatuHalamanElearning);
							pagingTelahTerjawab.setMold("os");
							pagingTelahTerjawab.setTotalSize(size);
							pagingTelahTerjawab.setVisible(size > jumlahDataDalamSatuHalamanElearning);
							pagingTelahTerjawab.getParent().setVisible(pagingTelahTerjawab.isVisible());

							pagingTelahTerjawab.setActivePage(
									(size / jumlahDataDalamSatuHalamanElearning) > pagingTelahTerjawabActivePage
											? pagingTelahTerjawabActivePage
											: (size / jumlahDataDalamSatuHalamanElearning));

							Common.initPagingCustom(pagingTelahTerjawab, new EventListener() {

								/**
								 * Menangani perpindahan halaman pada daftar "Telah terjawab".
								 *
								 * <p>Menyimpan halaman aktif ke {@link ProsesUjianHelper#pagingTelahTerjawabActivePage} lalu
								 * memanggil {@link ProsesUjianHelper#reloadTelahDikerjakan(Set,int,int,int)} dengan offset
								 * {@code 5 * halamanAktif}.</p>
								 *
								 * @param arg0 event paging ZK (isinya tidak dipakai)
								 * @throws Exception bila render ulang baris gagal
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {
									pagingTelahTerjawabActivePage = pagingTelahTerjawab.getActivePage();
									reloadTelahDikerjakan(idsa, jumlahDataDalamSatuHalamanElearning,
											jumlahDataDalamSatuHalamanElearning * (pagingTelahTerjawab == null ? 0
													: pagingTelahTerjawab.getActivePage()),
											jumlahDataDalamSatuHalamanElearning);
								}
							}, jumlahDataDalamSatuHalamanElearning);

							reloadTelahDikerjakan(idsa, jumlahDataDalamSatuHalamanElearning,
									jumlahDataDalamSatuHalamanElearning
											* (pagingTelahTerjawab == null ? 0 : pagingTelahTerjawab.getActivePage()),
									jumlahDataDalamSatuHalamanElearning);
						}
					});

				}
			});

			final Tabpanel tabpanelStatistik = new ais.ui.util.MyTabpanel();
			tabpanelTelahDijawab.setStyle("min-height: 550px;");
			tabpanelStatistik.setParent(tabpanels);
			tabStatistik.addEventListener("onClick", new EventListener() {

				/**
				 * Membangun tab "Statistik": ringkasan progres pengerjaan ujian peserta.
				 *
				 * <p><b>Isi yang ditampilkan:</b> jumlah total soal, jumlah soal yang telah dijawab, jumlah
				 * yang belum dijawab, persentase ketuntasan, serta diagram lingkaran berbasis CSS dari
				 * {@link ProsesUjianHelper#buildElearningHtmlPie(String,String,int,int)}.</p>
				 *
				 * <p>Berbeda dengan dua tab daftar soal, tab ini SELALU mengambil id soal terjawab segar dari
				 * basis data (argumen {@code true} pada {@code ambilBankSoalIdTerjawab}) supaya angka
				 * statistik tidak pernah basi.</p>
				 *
				 * @param arg0 event {@code onClick} ZK dari tab Statistik (isinya tidak dipakai)
				 * @throws Exception bila query atau pembangunan komponen gagal
				 */
				@SuppressWarnings("deprecation")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(tabpanelStatistik);

					Set<Long> idsa = hasilUjianMahasiswa == null ? new HashSet<Long>()
							: hasilUjianMahasiswa.ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoals, true);

					Borderlayout borderlayout = new Borderlayout();
					borderlayout.setParent(tabpanelStatistik);
					Center center = new Center();
					center.setParent(borderlayout);
					ais.ui.util.ZkCompat.setFlex(center, true);
					Grid grid = new Grid();
					grid.setSclass("dgrid");
					grid.setWidth("100%");
					grid.setParent(center);
					grid.setWidth("100%");
					grid.setHeight("100%");
					grid.setSclass("fgrid");

					Columns columns = new Columns();
					columns.setParent(grid);

					MyColumnConfig column = new MyColumnConfig();
					column.setParent(columns);
					column.setWidth("40%");

					column = new MyColumnConfig();
					column.setParent(columns);

					Rows rows = new Rows();
					rows.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Total Soal")));
					row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(ujianPunyaSoals.size())));

					// row = new MyFormRow();
					//					// row.setParent(rows);
					// row.appendChild(new Label(ais.common.Common.getBahasaConfig("Soal ke")));
					// row.appendChild(new
					// MyLabelBoldAja(Common.numberFormat.get().format(index + 1)));

					int terjawab = idsa.size();

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Label(ais.common.Common.getBahasaConfig("Soal yang telah dijawab")));
					row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(terjawab)));

					int belum = ujianPunyaSoals.size() - terjawab;

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Label(ais.common.Common.getBahasaConfig("Soal yang belum dijawab")));
					row.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(belum)));

					Double persen = (100.0 * terjawab) / ujianPunyaSoals.size();
					Double persenBelum = (100.0 * belum) / ujianPunyaSoals.size();

					row = new MyFormRow();
					row.setParent(rows);
					row.appendChild(new Label(ais.common.Common.getBahasaConfig("Prosentase")));
					Vbox vbox = new Vbox();
					vbox.setParent(row);
					vbox.appendChild(new MyLabelBoldAja(Common.numberFormat.get().format(persen) + " %"));

					row = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.setParent(rows);
						row.appendChild(buildElearningHtmlPie("Progres Jawaban", "Memperlihatkan jumlah soal yang sudah dijawab dibandingkan seluruh soal.", terjawab, ujianPunyaSoals.size()));
				}
			});

			borderlayout.setParent(tabpanelUtama);
		} else {
			borderlayout.setParent(centerUtama);
		}

		north.setStyle("background:linear-gradient(90deg,#0f2657 0%,#1d4ed8 100%);padding:6px 16px 2px;");

		Html cbtTopBar = new Html();
		StringBuilder cbtHeaderSb = new StringBuilder();
		String cbtUjianNama = "";
		try {
			if (pertemuanPunyaUjian.getUjian() != null && pertemuanPunyaUjian.getUjian().getNama() != null) {
				cbtUjianNama = pertemuanPunyaUjian.getUjian().getNama();
			}
		} catch (Exception cbtEx) { ais.common.ErrorAuditUtil.record(cbtEx, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:2270"); /* best-effort */ }
		int cbtJmlSoal = ujianPunyaSoals != null ? ujianPunyaSoals.size() : 0;
		cbtHeaderSb.append("<div style='display:flex;align-items:center;color:#fff;font-family:sans-serif;gap:12px;'>");
		cbtHeaderSb.append("<div style='flex:0 0 auto;width:5px;height:34px;background:#fbbf24;border-radius:4px;opacity:.9;'></div>");
		cbtHeaderSb.append("<div style='flex:1;min-width:0;'>");
		cbtHeaderSb.append("<div style='font-size:15px;font-weight:800;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;'>")
			.append(escapeHtmlSimple(cbtUjianNama)).append("</div>");
		cbtHeaderSb.append("<div style='font-size:11px;opacity:.7;margin-top:1px;'>")
			.append(cbtJmlSoal).append(" Soal &bull; Kerjakan dengan jujur &bull; Dilarang membuka tab lain</div>");
		cbtHeaderSb.append("</div>");
		cbtHeaderSb.append("<div style='flex:0 0 auto;text-align:right;'>");
		cbtHeaderSb.append("<div style='font-size:10px;text-transform:uppercase;letter-spacing:.6px;opacity:.7;margin-bottom:2px;'>Sisa Waktu</div>");
		cbtHeaderSb.append("</div>");
		cbtHeaderSb.append("</div>");
		cbtTopBar.setContent(cbtHeaderSb.toString());

		// North hanya boleh 1 child — gunakan Vbox sebagai wrapper
		org.zkoss.zul.Vbox northWrapper = new org.zkoss.zul.Vbox();
		northWrapper.setWidth("100%");
		northWrapper.setParent(north);
		cbtTopBar.setParent(northWrapper);

		// Timer "Sisa Waktu" dibuat tajam & kontras tinggi: pil gelap + ikon jam agar mudah dibaca di layar apa pun.
		org.zkoss.zul.Hbox cbtTimerBox = new org.zkoss.zul.Hbox();
		cbtTimerBox.setWidth("100%");
		cbtTimerBox.setPack("end");
		cbtTimerBox.setAlign("center");
		cbtTimerBox.setParent(northWrapper);
		new Html("<span style='display:inline-flex;align-items:center;justify-content:center;width:30px;height:30px;"
				+ "border-radius:999px;background:rgba(251,191,36,.18);margin-right:8px;'>"
				+ "<svg width='18' height='18' viewBox='0 0 24 24' fill='none' stroke='#fbbf24' stroke-width='2.4' "
				+ "stroke-linecap='round' stroke-linejoin='round'><circle cx='12' cy='12' r='9'/><path d='M12 7v5l3 2'/></svg>"
				+ "</span>").setParent(cbtTimerBox);
		waktu = new Label(label + " : 00:00:00");
		waktu.setStyle("display:inline-block;background:#0b1220;color:#fde047;"
				+ "font-family:'Courier New',Consolas,monospace;font-size:23px;font-weight:900;letter-spacing:1px;"
				+ "padding:5px 16px;border-radius:999px;border:1px solid rgba(251,191,36,.6);"
				+ "box-shadow:0 4px 14px rgba(0,0,0,.35);text-shadow:0 1px 2px rgba(0,0,0,.45);white-space:nowrap;");
		waktu.setParent(cbtTimerBox);

		// Himbauan pengawasan: tampil hanya saat ujian sungguhan & fitur anti-curang diaktifkan admin.
		// Di HP kotak himbauan ini menutup sebagian besar layar soal, jadi TIDAK ditampilkan di HP
		// (pengawasan anti-curang tetap AKTIF lewat skrip — hanya teks himbauannya yang disembunyikan).
		if (!hanyaLihat && isAntiCurangAktif(pertemuanPunyaUjian) && !Common.isMobile()) {
			new Html(buildHimbauanAntiCurangHtml()).setParent(northWrapper);
		}

		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);
		north.setVisible(!hanyaLihat);

		if (!Common.isMobile()) {
			penjelasan = new West();
			penjelasan.setParent(borderlayout);
			penjelasan.setWidth("50%");
			ais.ui.util.ZkCompat.setFlex(penjelasan, true);
			penjelasan.setStyle("background-color:white;");
			penjelasan.setVisible(false);
		} else {
			penjelasan = null;
		}

		Center center = new Center();
		center.setParent(borderlayout);

		ais.ui.util.ZkCompat.setFlex(center, true);
		center.setStyle("background-color:white;");

		gridSoal = new Grid();
		gridSoal.setOddRowSclass("non-odd");
		gridSoal.setWidth("100%");
		gridSoal.setParent(center);
		gridSoal.setWidth("100%");
		gridSoal.setHeight("100%");
		gridSoal.setSclass("fgrid");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayoutUtama);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("65px");
		toolbar.setParent(south);

		cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			/**
			 * Menutup jendela CBT lewat tombol "Tutup".
			 *
			 * <p>Menghentikan timer ZK dan thread countdown ({@code waktuTimer.stop = true}), lalu melepas
			 * jendela. Untuk pengguna tamu / calon mahasiswa, konfirmasi bawaan browser dimatikan lebih
			 * dahulu agar tidak muncul dialog ganda.</p>
			 *
			 * <p><b>Penting:</b> tombol ini TIDAK menyimpan maupun memfinalisasi nilai — jawaban yang
			 * sudah tersimpan tetap ada, tetapi {@code telahIkutUjian} tidak diset dan
			 * {@link ProsesUjianHelper#generateHasilUjian(List,HasilUjianMahasiswa,PertemuanPunyaUjian,Map)}
			 * tidak dipanggil. Karena itu tombol dinonaktifkan ({@code cancel.setDisabled(true)}) begitu
			 * peserta sungguhan mulai mengerjakan; ia hanya tersisa aktif pada mode pratinjau
			 * ({@code hasilUjianMahasiswa} null) dan mode lihat-saja.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Tutup"
			 * @throws Exception bila operasi ZK gagal
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
					Clients.confirmClose(null);
				}

				timer.stop();
				waktuTimer.stop = true;
				ProsesUjianHelper.this.detach();

			}
		});
		cancel.setParent(toolbar);

		save = new MyToolbarbuttonConfig(hanyaLihat ? "Selesai" : "Selesaikan Ujian", "/img/svg/check2-circle.svg");
//		save.setVisible(hasilUjianMahasiswa != null);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			/**
			 * Menangani tombol "Selesaikan Ujian" (berlabel "Selesai" pada mode lihat-saja).
			 *
			 * <p><b>Alur:</b></p>
			 * <ol>
			 *   <li>Pada mode lihat-saja, jendela langsung dilepas tanpa perhitungan apa pun.</li>
			 *   <li>Bila konfigurasi {@code mahasiswa_harus_melengkapi_jawaban_soal} aktif DAN ujian bukan
			 *       bermode "tiap soal" serta tombol kembali tidak dimatikan, seluruh soal diperiksa:
			 *       setiap soal yang id {@code BankSoal}-nya belum ada di daftar terjawab dihitung. Bila
			 *       masih ada yang kosong, tab "Belum terjawab" diaktifkan, peringatan berisi jumlahnya
			 *       ditampilkan, dan penyelesaian DIBATALKAN.</li>
			 *   <li>Bila lolos, muncul dialog konfirmasi. Menekan OK menjadwalkan
			 *       {@link ProsesUjianHelper#onSelesai()} lewat {@code Common.createDefaultTimer}.</li>
			 * </ol>
			 *
			 * <p><b>Catatan integritas:</b> kelengkapan jawaban hanya dipaksakan pada ujian yang
			 * mengizinkan peserta mundur. Pada ujian bermode "tiap soal" atau ujian satu-arah,
			 * kelengkapan sudah dijaga per soal oleh tombol "Lanjut", sehingga pemeriksaan di sini
			 * sengaja dilewati.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Selesaikan Ujian"
			 * @throws Exception bila operasi ZK atau Hibernate gagal
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
					Clients.confirmClose(null);
				}

				if (hanyaLihat) {
					detach();
				} else {
					if (hasilUjianMahasiswa != null
							&& Common.bolehKonfigurasi("mahasiswa_harus_melengkapi_jawaban_soal")) {
						if (!pertemuanPunyaUjian.getTiapSoal()
								&& !pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali()) {
							int soalbelumSelesai = 0;
							Set<Long> idsa = hasilUjianMahasiswa == null ? new HashSet<Long>()
									: hasilUjianMahasiswa.ambilBankSoalIdTerjawab(jumlahDiujikan, ujianPunyaSoals);
							for (Long ujianPunyaSoalid : ujianPunyaSoals) {
								UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
										.ambilData(UjianPunyaSoal.class, ujianPunyaSoalid.toString());
								if (ujianPunyaSoal != null) {
									if (!idsa.contains(ujianPunyaSoal.getBankSoal().getId())) {
										soalbelumSelesai++;
									}
								}
							}
							if (soalbelumSelesai > 0) {
								if (tabJawaban != null) {
									tabJawaban.setSelected(true);
								}
								MyMessageboxConfig.showFormatCb(
				"Terdapat {V1} soal yang belum dijawab. Anda tidak dapat menyelesaikan ujian ini apabila masih terdapat soal yang belum dijawab. Langkah yang dapat dilakukan: (1) periksa kembali seluruh soal; (2) jawab soal yang masih kosong; (3) setelah seluruh soal terjawab, selesaikan ujian.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
				belumDijawabEventListener, soalbelumSelesai);
								return;
							}
						}
					}

					MyMessageboxConfig.show(
				"Apakah Bapak/Ibu yakin ingin menyelesaikan ujian ini? Setelah ujian diselesaikan, Anda tidak dapat lagi mengubah jawaban. Silakan pilih OK untuk menyelesaikan atau Batal untuk kembali mengerjakan.",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

								/**
								 * Menerima pilihan peserta pada dialog konfirmasi "Apakah Bapak/Ibu yakin ingin menyelesaikan
								 * ujian ini?".
								 *
								 * <p>Kode tombol dibaca dari {@code event.getData()}. Hanya bila nilainya
								 * {@code MyMessageboxConfig.OK} penyelesaian dilanjutkan — itu pun tidak langsung, melainkan
								 * dijadwalkan lewat {@code Common.createDefaultTimer} supaya dialog sempat tertutup sebelum
								 * proses perhitungan nilai yang berat dimulai. Pilihan Batal tidak melakukan apa pun sehingga
								 * peserta kembali mengerjakan.</p>
								 *
								 * @param event event ZK dari messagebox; {@code getData()} berisi kode tombol yang ditekan
								 * @throws Exception bila penjadwalan timer ZK gagal
								 */
								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										Common.createDefaultTimer(new EventListener() {

											/**
											 * Menjalankan {@link ProsesUjianHelper#onSelesai()} pada siklus event ZK berikutnya, setelah
											 * peserta menekan OK pada dialog konfirmasi.
											 *
											 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
											 * @throws Exception bila proses finalisasi nilai gagal
											 */
											@Override
											public void onEvent(Event arg0) throws Exception {
												onSelesai();
											}
										});
									}
								}
							});
				}

			}
		});
		save.setParent(toolbar);

		back = new MyToolbarbuttonConfig("Kembali", "/img/left-sign.gif");
		next = new MyToolbarbuttonConfig("Lanjut", "/img/right-sign.gif");

		back.setDisabled(true);
		back.setTooltiptext("Soal Sebelumnya");
		back.addEventListener("onClick", new EventListener() {
			/**
			 * Berpindah ke soal (atau halaman soal) SEBELUMNYA.
			 *
			 * <p>Sebelum berpindah, {@link ProsesUjianHelper#jumlahDibatasi()} dipanggil untuk memastikan
			 * jumlah jawaban pada soal berganda yang sedang tampil sudah memenuhi batas minimal/maksimal;
			 * bila belum, perpindahan dibatalkan dan peserta diberi peringatan.</p>
			 *
			 * <p>Indeks dikurangi {@link ProsesUjianHelper#jumlahSoalPerHalaman}, lalu
			 * {@link ProsesUjianHelper#doProcessUjian(int)} me-render halaman soal sebelumnya. Tombol ini
			 * disembunyikan sepenuhnya pada ujian bermode "tiap soal" maupun ujian yang mematikan tombol
			 * kembali.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Kembali"
			 * @throws Exception bila render ulang soal gagal
			 */
			@Override
			public void onEvent(Event event) throws Exception {
				if (!jumlahDibatasi()) {
					return;
				}

				index = index - jumlahSoalPerHalaman;
				doProcessUjian(index);
			}
		});
		back.setParent(toolbar);

		next.setTooltiptext("Soal Berikutnya");
		next.addEventListener("onClick", new EventListener() {
			/**
			 * Berpindah ke soal (atau halaman soal) BERIKUTNYA, dengan penjagaan kelengkapan jawaban pada
			 * ujian bermode "tiap soal".
			 *
			 * <p><b>Dua penjagaan berurutan:</b></p>
			 * <ol>
			 *   <li>{@link ProsesUjianHelper#jumlahDibatasi()} — batas minimal/maksimal jawaban pada soal
			 *       berganda.</li>
			 *   <li>Khusus ujian {@code tiapSoal} (soal tidak dapat ditinggalkan lalu dikunjungi ulang):
			 *       soal yang sedang tampil WAJIB sudah dijawab. Untuk ujian pilihan ganda, "sudah
			 *       dijawab" berarti ada {@code HasilUjianMahasiswaDetail} dengan {@code bankSoalDetail}
			 *       tidak null; untuk jenis lain berarti kolom {@code jawaban} tidak kosong. Bila belum,
			 *       peringatan ditampilkan dan perpindahan dibatalkan.</li>
			 * </ol>
			 *
			 * <p>Setelah lolos, indeks ditambah {@link ProsesUjianHelper#jumlahSoalPerHalaman} dan
			 * {@link ProsesUjianHelper#doProcessUjian(int)} me-render halaman berikutnya.</p>
			 *
			 * @param event event {@code onClick} ZK dari tombol "Lanjut"
			 * @throws Exception bila render ulang soal gagal
			 */
			@Override
			public void onEvent(Event event) throws Exception {

				if (!jumlahDibatasi()) {
					return;
				}

				if (pertemuanPunyaUjian.getTiapSoal() && hasilUjianMahasiswa != null) {
					try {

						Long aujianPunyaSoalid = null;
						try {
							aujianPunyaSoalid = ujianPunyaSoals.get(index);
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:2475");
						}
						UjianPunyaSoal aujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject
								.ambilData(UjianPunyaSoal.class, aujianPunyaSoalid.toString());
						if (aujianPunyaSoal != null) {
							Set<Long> s = hasilUjianMahasiswaDetailsa.get(aujianPunyaSoal.getBankSoal().getId());
							if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {

								boolean ada = false;
								for (Long myHasilUjianMahasiswaDetailid : s) {
									HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													myHasilUjianMahasiswaDetailid.toString());
									if (myHasilUjianMahasiswaDetail != null
											&& myHasilUjianMahasiswaDetail.getBankSoalDetail() != null) {
										ada = true;
										break;
									}
								}

								if (!ada) {
									MyMessageboxConfig.show(
				"Mohon maaf, Anda belum memilih jawaban. Silakan pilih salah satu jawaban terlebih dahulu sebelum melanjutkan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
							} else {

								boolean ada = false;
								for (Long myHasilUjianMahasiswaDetailid : s) {
									HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													myHasilUjianMahasiswaDetailid.toString());
									if (myHasilUjianMahasiswaDetail != null
											&& !myHasilUjianMahasiswaDetail.getJawaban().trim().isEmpty()) {
										ada = true;
										break;
									}
								}

								if (!ada) {
									MyMessageboxConfig.show(
				"Mohon maaf, Anda belum menuliskan jawaban. Silakan tuliskan jawaban Anda terlebih dahulu sebelum melanjutkan.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									return;
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2524");
					}
				}

				index = index + jumlahSoalPerHalaman;
				doProcessUjian(index);
			}
		});
		next.setParent(toolbar);

		int startIndex = 0;
		try {
			if (hasilUjianMahasiswa != null) {
				String s = hasilUjianMahasiswa.retreive("index");
				if (s != null && !s.trim().isEmpty()) {
					startIndex = Integer.parseInt(s.trim());
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:2542");
		}

		if (ujianPunyaSoals != null) {
			if (startIndex < 0 || startIndex >= ujianPunyaSoals.size()) {
				startIndex = 0;
			}
		} else {
			startIndex = 0;
		}

		if (hanyaLihat) {
			doProcessUjian(startIndex);
//			save.setVisible(false);
			cancel.setDisabled(false);
		} else {
			if (hasilUjianMahasiswa == null) {
				doProcessUjian(startIndex);
			} else {
				cancel.setDisabled(hasilUjianMahasiswa != null);
				doProcessUjian(startIndex);
				timer.start();
			}
		}

		if (pertemuanPunyaUjian.getTiapSoal()) {
			back.setVisible(false);
			if (!hanyaLihat) {
				cancel.setDisabled(true);
			}
		}

		if (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getId() == null) {
			cancel.setDisabled(false);
		}

		if ((ProsesUjianHelper.this.index + jumlahSoalPerHalaman) >= ujianPunyaSoals.size()) {
//			save.setVisible(true);
			next.setDisabled(true);
		}

		if (pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali()) {
			back.setVisible(false);
			back.setDisabled(true);
		}

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			/**
			 * Memuat ulang seluruh soal ujian dari basis data (tombol "Refresh" pada toolbar bawah).
			 *
			 * <p>Menyimpan indeks soal yang sedang dibuka ke {@link ProsesUjianHelper#indexTemp},
			 * menampilkan loading-bar, lalu menjalankan {@link ProsesUjianHelper#initSoal(Label)} di thread
			 * latar. Ketika selesai, callback loading-bar me-render ulang soal yang sama dengan
			 * {@code doProcessUjian(indexTemp, true)} — argumen {@code refresh=true} memaksa
			 * {@code ambilBankSoalDetail} mengambil ulang opsi jawaban dari basis data, bukan dari cache.</p>
			 *
			 * <p>Tombol ini adalah jalan keluar ketika soal atau opsi jawaban gagal tampil karena cache
			 * {@code GeneralValueObject} tidak sinkron dengan basis data.</p>
			 *
			 * @param arg0 event {@code onClick} ZK dari tombol Refresh (isinya tidak dipakai)
			 * @throws Exception bila penyiapan ulang soal gagal
			 */
			@Override
			public void onEvent(Event arg0) throws Exception {
				ProsesUjianHelper.this.indexTemp = ProsesUjianHelper.this.index;
				final Label label = Common.displayLoadBar(new EventListener() {

					/**
					 * Me-render ulang soal yang sedang dibuka setelah thread latar selesai memuat ulang soal.
					 *
					 * <p>Memanggil {@code doProcessUjian(indexTemp, true)} sehingga posisi soal peserta tidak
					 * berubah, tetapi opsi jawaban diambil segar dari basis data.</p>
					 *
					 * @param arg0 event ZK dari loading-bar (isinya tidak dipakai)
					 * @throws Exception bila render ulang soal gagal
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {
						doProcessUjian(ProsesUjianHelper.this.indexTemp, true);
					}
				});

				new Thread(new Runnable() {

					/**
					 * Memuat ulang daftar soal di thread latar agar antarmuka tidak membeku selama proses Refresh.
					 *
					 * <p>Hanya memanggil {@link ProsesUjianHelper#initSoal(Label)}. Sama seperti thread penyiap
					 * soal di {@link ProsesUjianHelper#init()}, ia berjalan di luar konteks request ZK sehingga
					 * memakai session Hibernate native.
					 */
					@Override
					public void run() {
						initSoal(label);
					}
				}).start();
			}
		});

	}

	/**
	 * Menghentikan seluruh <b>mode pengawasan ujian (anti-curang)</b> di sisi browser saat
	 * ujian selesai atau jendela ujian ditutup. Metode ini adalah pasangan penutup dari
	 * skrip yang dipasang {@code buildCbtAntiCheatScript(...)} / {@code buildCbtAntiCheatScriptDefault(...)}
	 * pada awal ujian, sehingga pengawasan hanya aktif SELAMA ujian berlangsung dan benar-benar
	 * berhenti begitu peserta menyelesaikannya.
	 *
	 * <p><b>Mengapa perlu.</b> Pendengar (event listener) anti-curang — deteksi pindah tab
	 * ({@code visibilitychange}), keluar jendela/Alt+Tab ({@code blur}), keluar layar penuh
	 * ({@code fullscreenchange}), blokir klik-kanan ({@code contextmenu}), blokir pintasan
	 * berbahaya ({@code keydown}), dan peringatan meninggalkan halaman ({@code beforeunload})
	 * — dipasang di tingkat {@code window}/{@code document}, BUKAN di komponen ujian. Karena
	 * itu, sekadar melepas (detach) komponen ujian tidak menghentikannya; mode layar penuh dan
	 * overlay "Pelanggaran Terdeteksi" akan tetap muncul walau ujian sudah selesai. Metode ini
	 * mengatasi hal tersebut.
	 *
	 * <p><b>Cara kerja.</b> Skrip anti-curang menyimpan sebuah saklar global
	 * {@code window.__cbtOff} dan fungsi {@code window.__cbtStop()}. Setiap pendengar dan fungsi
	 * efek (perekam pelanggaran {@code rec}, penampil peringatan {@code warn}) memeriksa
	 * {@code window.__cbtOff} di awal dan langsung berhenti bila bernilai {@code true}. Fungsi
	 * {@code window.__cbtStop()} menyetel {@code __cbtOff=true}, menutup overlay peringatan yang
	 * mungkin sedang tampil, lalu keluar dari mode layar penuh (mendukung API standar,
	 * WebKit, dan Mozilla). Metode Java ini cukup memanggil {@code window.__cbtStop()} melalui
	 * {@code Clients.evalJavaScript}; bila fungsi itu belum ada (mis. ujian tanpa pengawasan),
	 * disediakan cadangan yang menyetel {@code window.__cbtOff=true} agar tetap aman (no-op).
	 *
	 * <p><b>Idempoten &amp; aman.</b> Metode boleh dipanggil berkali-kali tanpa efek samping;
	 * dipanggil dari {@link #onSelesai()} (ujian tuntas / auto-selesai karena batas pelanggaran)
	 * dan dari {@link #detach()} (jendela ujian ditutup dengan cara apa pun). Seluruh pemanggilan
	 * dibungkus {@code try/catch} sehingga kegagalan menghentikan pengawasan tidak pernah
	 * menggagalkan proses penyelesaian atau penutupan ujian.
	 */
	private void hentikanPengawasanUjian() {
		try {
			Clients.evalJavaScript("if(window.__cbtStop){window.__cbtStop();}else{window.__cbtOff=true;}");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:2692");
			// abaikan: penghentian pengawasan tidak boleh menggagalkan penyelesaian/penutupan ujian
		}
	}

	/**
	 * Melepas jendela ujian. Sebelum benar-benar menutup, memastikan mode pengawasan
	 * anti-curang dihentikan (keluar fullscreen + nonaktifkan deteksi pelanggaran) via
	 * {@link #hentikanPengawasanUjian()}, sehingga penutupan dengan cara apa pun (tombol
	 * Selesai, tombol X, maupun program) selalu mematikan pengawasan.
	 */
	@Override
	public void detach() {
		hentikanPengawasanUjian();
		super.detach();
	}

	/**
	 * Menyelesaikan sesi ujian: menghitung nilai akhir, menyimpan ke database, menutup jendela CBT,
	 * dan memicu callback pasca-ujian (refresh UI, sertifikat, dll.).
	 *
	 * <p><b>Tujuan:</b> Ini adalah titik akhir dari siklus hidup {@link ProsesUjianHelper}.
	 * Dipanggil ketika peserta menekan "Akhiri Ujian", waktu habis (dipicu ZK Timer), atau
	 * sistem mendeteksi kondisi terminasi (batas pelanggaran anti-curang tercapai).</p>
	 *
	 * <p><b>Cara kerja (urutan eksekusi):</b></p>
	 * <ol>
	 *   <li>Memanggil {@link #generateHasilUjian(List,HasilUjianMahasiswa,PertemuanPunyaUjian,Map)}
	 *       untuk menghitung nilai akhir (skor total, lama pengerjaan, OBE mapping) dan
	 *       menyimpannya ke database. Jika gagal (return false), onSelesai tidak dilanjutkan.</li>
	 *   <li>Setelah berhasil, membersihkan state recompute via
	 *       {@code UjianRecomputeUtil.bersihkan(hasilId)} — menjaga konsistensi dengan
	 *       mekanisme autosave recompute.</li>
	 *   <li>Untuk pengguna tamu/calon mahasiswa, memanggil {@code Clients.confirmClose(null)}
	 *       untuk menonaktifkan dialog "Yakin meninggalkan halaman?".</li>
	 *   <li>Menghentikan timer ZK ({@code timer.stop()}) dan thread countdown
	 *       ({@code waktuTimer.stop = true}).</li>
	 *   <li>Via {@code Common.createDefaultTimer}: membersihkan {@link #kuotaUjian} (mengurangi
	 *       counter peserta aktif) dan invalidasi cache hasil ujian di entitas terkait
	 *       (mahasiswa/calon/siswa via {@code put("", "hasilUjianMahasiswa")}).</li>
	 *   <li>Via timer kedua (setelah cleanup): memuat ulang {@code hasilUjianMahasiswa} dari
	 *       database (refresh), menandai {@code telahIkutUjian=true}, menyimpan kembali,
	 *       memanggil {@code eventListener.onEvent()} agar UI pemanggil memperbarui dirinya,
	 *       lalu menutup jendela via {@code ProsesUjianHelper.this.detach()}.</li>
	 *   <li>Bila peserta lulus dan ujian memiliki sertifikat, memanggil
	 *       {@code SertifikatAction.cetakSertifikat} secara otomatis.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception dari langkah refresh/simpan ditangkap dan dicetak
	 * ke stderr. Jendela tetap ditutup ({@code detach()}) meskipun terjadi error saat menyimpan
	 * status akhir — peserta tidak terblokir di jendela ujian yang sudah selesai.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Urutan dua timer sangat penting — timer pertama membersihkan
	 * resource, timer kedua menyimpan dan menutup. Jangan gabungkan keduanya karena cleanup
	 * harus terjadi sebelum penyimpanan akhir untuk menghindari race condition pada
	 * {@link #kuotaUjian}.</p>
	 *
	 * @throws Exception bila ZK event atau Hibernate melempar exception yang tidak tertangkap
	 */
	public void onSelesai() throws Exception {

		// Hentikan pengawasan anti-curang & keluar layar penuh begitu ujian diselesaikan.
		hentikanPengawasanUjian();

		if (generateHasilUjian(ujianPunyaSoals, hasilUjianMahasiswa, pertemuanPunyaUjian,
				hasilUjianMahasiswaDetailsa)) {

			// Hitung penuh saat "Akhiri Ujian" sudah dilakukan generateHasilUjian di atas;
			// bersihkan state throttle recompute per-jawaban peserta ini.
			if (hasilUjianMahasiswa != null) {
				UjianRecomputeUtil.bersihkan(hasilUjianMahasiswa.getId());
			}

			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser == null || tbmuser.getBiodataCalonMahasiswa() != null) {
				Clients.confirmClose(null);
			}
			timer.stop();
			waktuTimer.stop = true;

			Common.createDefaultTimer(new EventListener() {

				/**
				 * Tahap pembersihan pasca-ujian: melepas slot kuota dan meng-invalidasi cache hasil ujian.
				 *
				 * <p>Menghapus {@code keyhasil} peserta dari {@link ProsesUjianHelper#kuotaUjian} sehingga slot
				 * ujian bersamaan langsung dapat dipakai peserta lain, lalu menulis nilai kosong ke cache
				 * {@code "hasilUjianMahasiswa"} pada entitas peserta (mahasiswa / calon mahasiswa / calon
				 * siswa / siswa) supaya layar daftar ujian memuat ulang datanya.</p>
				 *
				 * <p>Dijadwalkan sebagai timer PERTAMA dari dua timer di {@link ProsesUjianHelper#onSelesai()};
				 * urutan ini disengaja — pembersihan harus terjadi sebelum penyimpanan status akhir.</p>
				 *
				 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
				 * @throws Exception tidak pernah lolos keluar; kesalahan ditangkap dan dicatat di dalam
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					try {

						if (hasilUjianMahasiswa != null) {
							kuotaUjian.remove(hasilUjianMahasiswa.getKeyhasil());
						}

						if (pertemuanPunyaUjian != null && hasilUjianMahasiswa != null) {
							if (hasilUjianMahasiswa.getMahasiswa() != null) {
								hasilUjianMahasiswa.getMahasiswa().put("", "hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
								hasilUjianMahasiswa.getBiodataCalonMahasiswa().put("", "hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getCalonSiswa() != null) {
								hasilUjianMahasiswa.getCalonSiswa().put("", "hasilUjianMahasiswa");
							} else if (hasilUjianMahasiswa.getSiswa() != null) {
								hasilUjianMahasiswa.getSiswa().put("", "hasilUjianMahasiswa");
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2752");
					}
				}
			});

			Common.createDefaultTimer(new EventListener() {

				/**
				 * Tahap penyimpanan akhir dan penutupan jendela ujian.
				 *
				 * <p><b>Urutan:</b> memuat ulang {@code hasilUjianMahasiswa} dari basis data
				 * ({@code session.refresh}), menandai {@code telahIkutUjian = true}, menyimpannya dalam satu
				 * transaksi pada session Hibernate native, menghentikan thread countdown
				 * ({@code waktuTimer.setStop(true)}), memanggil {@link ProsesUjianHelper#eventListener} supaya
				 * layar pemanggil menyegarkan tampilannya, lalu melepas jendela CBT.</p>
				 *
				 * <p><b>Ketahanan:</b> kegagalan penyimpanan hanya dicatat — jendela TETAP dilepas agar peserta
				 * tidak terkurung di jendela ujian yang sudah selesai. Nilai itu sendiri sudah disimpan lebih
				 * dahulu oleh
				 * {@link ProsesUjianHelper#generateHasilUjian(List,HasilUjianMahasiswa,PertemuanPunyaUjian,Map)},
				 * jadi kegagalan di sini paling banter menyisakan {@code telahIkutUjian} yang belum ternyala.</p>
				 *
				 * <p>Bila peserta dinyatakan lulus, pencetakan sertifikat dijadwalkan pada timer berikutnya.</p>
				 *
				 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
				 * @throws Exception bila {@code eventListener} milik layar pemanggil melempar kesalahan
				 */
				@Override
				public void onEvent(Event arg0) throws Exception {
					if (hasilUjianMahasiswa != null) {
						Session session = HibernateUtil.currentNativeSession();
						try {
							session.refresh(hasilUjianMahasiswa);
							hasilUjianMahasiswa.setTelahIkutUjian(true);

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(session, hasilUjianMahasiswa);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {
								ais.common.ElearningSessionUtil.closeQuietly(session);
							}
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2775");
						}
						HibernateUtil.closeSession();

						waktuTimer.setStop(true);
						eventListener.onEvent(new Event("", timer, hasilUjianMahasiswa));
					}

					ProsesUjianHelper.this.detach();

					if (hasilUjianMahasiswa != null && hasilUjianMahasiswa.getLulus()) {
						Common.createDefaultTimer(new EventListener() {

							/**
							 * Mencetak sertifikat secara otomatis untuk peserta yang dinyatakan LULUS.
							 *
							 * <p>Dijadwalkan hanya bila {@code hasilUjianMahasiswa.getLulus()} bernilai benar, dan berjalan
							 * setelah jendela ujian dilepas sehingga dialog sertifikat tampil di atas layar pemanggil.</p>
							 *
							 * @param arg0 event timer ZK sekali-jalan (isinya tidak dipakai)
							 * @throws Exception bila pencetakan sertifikat gagal
							 */
							@Override
							public void onEvent(Event arg0) throws Exception {
								SertifikatAction.cetakSertifikat(hasilUjianMahasiswa);
							}
						});
					}

				}
			});
		}
	}

	/**
	 * Menghitung skor untuk satu soal berdasarkan jawaban yang dipilih peserta.
	 * Mendukung empat jenis perhitungan berbeda tergantung konfigurasi soal.
	 *
	 * <p><b>Tujuan:</b> Ini adalah "mesin penilaian" inti untuk satu soal. Method statik ini
	 * dipanggil oleh {@link #hitungPilihanGanda} untuk setiap soal dalam ujian. Ia menangani
	 * seluruh variasi jenis penilaian yang ada: pilihan ganda biasa, pilihan berganda (checkbox),
	 * benar-salah, dan jawaban default (peserta tidak menjawab).</p>
	 *
	 * <p><b>Empat jenis perhitungan berdasarkan {@code BankSoal.jenisPilihanGanda}:</b></p>
	 *
	 * <p><b>A. MULTIPLE_CHOICE atau BENAR_SALAH:</b> (satu jawaban benar)</p>
	 * <ul>
	 *   <li>Hitung {@code skorDariJawaban} = total skor kunci jawaban benar di bank soal.</li>
	 *   <li>{@code skorBenar} = jumlahPilihanBenar × (skor per jawaban bila berbeda, atau skor soal).</li>
	 *   <li>{@code skorSalah} = jumlahPilihanSalah × {@code bankSoal.getSkorSalah()} (biasanya negatif).</li>
	 *   <li>{@code skorDefault} = jumlahPilihanDefault × {@code bankSoal.getSkorDefault()} (tidak menjawab).</li>
	 *   <li>Return: {skorTotal, skorDariJawaban}.</li>
	 * </ul>
	 *
	 * <p><b>B. MULTIPLE_RESPONSE (checkbox, jawaban berganda):</b></p>
	 * <ul>
	 *   <li>Menghitung {@code totalJawabanBenar} (jumlah kunci yang benar di bank soal).</li>
	 *   <li>Bila {@code jumlahJawabanDibatasi}: skor proporsional berdasarkan
	 *       {@code (pilihanBenar / maksimalJumlahJawaban)}.</li>
	 *   <li>Bila tidak dibatasi: skor = skor × ((benar - salah) / totalBenar) — formula
	 *       partial credit yang mengurangi skor untuk tebakan.</li>
	 *   <li>Bila tidak ada yang benar: tambah {@code skorSalah} dan/atau {@code skorDefault}.</li>
	 *   <li>Nilai detail di database diperbarui bila skor berubah (menggunakan native session).</li>
	 * </ul>
	 *
	 * <p><b>C. Soal tanpa jawaban dipilih (bankSoalDetail == null):</b></p>
	 * <ul>
	 *   <li>Return: {nilai saat ini di database, 100.0} — tidak ada perubahan skor.</li>
	 * </ul>
	 *
	 * <p><b>Penanganan error:</b> Setiap akses detail melalui {@code GeneralValueObject.ambilData}
	 * dibungkus try-catch. Kegagalan satu detail tidak menghentikan perhitungan soal lain.</p>
	 *
	 * <p><b>Thread-safety:</b> Method statik ini dipanggil dari berbagai konteks (ZK event thread,
	 * thread recompute di {@code UjianRecomputeUtil}). Akses ke native session Hibernate dibuka
	 * dan ditutup per call untuk isolasi yang aman.</p>
	 *
	 * @param hasilUjianMahasiswaDetail detail jawaban peserta untuk soal ini (satu baris per opsi dipilih)
	 * @param hasilUjianMahasiswaDetails peta lengkap semua jawaban peserta dalam ujian ini
	 *                                   (bankSoalId → Set&lt;detailId&gt;)
	 * @return array Double[] dengan dua elemen: [0]=skor yang didapat peserta, [1]=skor maksimal soal
	 */
	public static Double[] hitung(HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails) {
		if (hasilUjianMahasiswaDetail == null) {
			return new Double[] { 0.0, 0.0 };
		}
		HasilUjianMahasiswa hasilUjianMahasiswa = hasilUjianMahasiswaDetail.getHasilUjianMahasiswa();
		BankSoal bankSoal = hasilUjianMahasiswaDetail.getBankSoal();
		// PENTING (perbaikan "Nilai per Sub-CPMK tidak tampil"): bankSoal — atau jenis pilihan
		// gandanya — bisa NULL, terutama pada ujian REMEDIAL bila bank soal yang dijawab peserta
		// diganti/dihapus. Tanpa penjagaan ini, akses bankSoal.getJenisPilihanGanda()/getSkor()/dll
		// di bawah melempar NullPointerException yang DITELAN DIAM-DIAM oleh pemanggil hitungObe()
		// (try/catch tanpa log), sehingga nilai per Sub-CPMK GAGAL ditulis ke nilaiObe → kolom
		// Skor/Max & Nilai KOSONG untuk peserta yang MENJAWAB. (Peserta yang TIDAK ikut ujian tetap
		// tampil 0 karena hitung() tak pernah dipanggil untuknya.) Kembalikan skor 0 untuk soal yang
		// tak dapat dinilai agar perhitungan Sub-CPMK peserta tetap berjalan & nilai lain tetap tampil.
		if (bankSoal == null || bankSoal.getJenisPilihanGanda() == null) {
			return new Double[] { 0.0, 0.0 };
		}
		// LANJUTAN perbaikan "Nilai per Sub-CPMK KOSONG untuk peserta yang MENJAWAB": selain bankSoal
		// & jenis pilihan ganda, kolom SKOR bank soal (getSkor/getSkorSalah/getSkorDefault) dan flag
		// (getSkorJawabanBerbeda/getJumlahJawabanDibatasi/getMaksimalJumlahJawaban) juga sering NULL
		// pada ujian REMEDIAL (konfigurasi bank soal tak lengkap). Meng-unbox Double/Boolean null →
		// NPE yang DITELAN oleh hitungObe() → jsonObjectHasil.put() DILEWATI → Sub-CPMK tak tertulis →
		// kolom KOSONG. Baca semua field rawan itu ke variabel LOKAL yang aman (null → 0.0 / false)
		// agar skor tetap terhitung tanpa melempar exception (soal yang tak lengkap otomatis skor 0).
		boolean skorBerbeda = Boolean.TRUE.equals(bankSoal.getSkorJawabanBerbeda());
		boolean jumlahDibatasi = Boolean.TRUE.equals(bankSoal.getJumlahJawabanDibatasi());
		double skorBenarBank = bankSoal.getSkor() == null ? 0.0 : bankSoal.getSkor().doubleValue();
		double skorSalahBank = bankSoal.getSkorSalah() == null ? 0.0 : bankSoal.getSkorSalah().doubleValue();
		double skorDefaultBank = bankSoal.getSkorDefault() == null ? 0.0 : bankSoal.getSkorDefault().doubleValue();
		double maksJawaban = bankSoal.getMaksimalJumlahJawaban() == null ? 0.0
				: bankSoal.getMaksimalJumlahJawaban().doubleValue();
		Double skorDariJawaban = 0.0;
		if (hasilUjianMahasiswaDetail.getBankSoalDetail() != null) {

			double totalJawabanPilihanBenar = 0.0;
			for (Set<Long> aaa : hasilUjianMahasiswaDetails.values()) {
				for (Long detailid : aaa) {
					try {
						HasilUjianMahasiswaDetail detail = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, detailid.toString());
						if (detail != null) {
							if (detail.getBankSoal().getId().equals(bankSoal.getId())
									&& detail.getBankSoalDetail() != null && detail.getBankSoalDetail().getBetul()) {
								totalJawabanPilihanBenar += 1.0;
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2896");
					}
				}
			}

			double totalJawabanPilihanSalah = 0.0;
			for (Set<Long> aaa : hasilUjianMahasiswaDetails.values()) {
				for (Long detailid : aaa) {
					try {
						HasilUjianMahasiswaDetail detail = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, detailid.toString());
						if (detail != null) {
							if (detail.getBankSoal().getId().equals(bankSoal.getId())
									&& detail.getBankSoalDetail() != null && !detail.getBankSoalDetail().getBetul()) {
								totalJawabanPilihanSalah += 1.0;
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2914");
					}
				}
			}

			double totalJawabanPilihanDefault = 0.0;
			for (Set<Long> aaa : hasilUjianMahasiswaDetails.values()) {
				for (Long detailid : aaa) {
					try {
						HasilUjianMahasiswaDetail detail = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, detailid.toString());
						if (detail != null) {
							if (detail.getBankSoal().getId().equals(bankSoal.getId())
									&& detail.getBankSoalDetail() == null) {
								totalJawabanPilihanDefault += 1.0;
							}
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:2932");
					}
				}
			}

			if (bankSoal.getJenisPilihanGanda().equals(BankSoal.MULTIPLE_COICE)
					|| bankSoal.getJenisPilihanGanda().equals(BankSoal.BENAR_SALAH)) {

				List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);

				for (Long bankSoalDetailid : bankSoalDetails) {
					BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							bankSoalDetailid.toString());
					if (bankSoalDetail != null) {
						if (bankSoalDetail != null && Boolean.TRUE.equals(bankSoalDetail.getBetul())) {
							skorDariJawaban += (bankSoalDetail.getSkor() == null ? 0.0 : bankSoalDetail.getSkor());
						}
					}
				}

				// Bank soal lama cukup sering tidak mempunyai skor soal maupun skor pada
				// pilihan benar. Jawabannya tetap dapat dinilai benar/salah, tetapi rumus
				// lama mengalikan jawaban benar dengan 0 sehingga nilai OBE selalu 0/0.
				// Gunakan satu poin sebagai satuan netral bila skor memang belum diatur.
				double skorBenarEfektif = skorBerbeda ? skorDariJawaban.doubleValue() : skorBenarBank;
				if (skorBenarEfektif <= 0.0 && skorDariJawaban.doubleValue() > 0.0) {
					skorBenarEfektif = skorDariJawaban.doubleValue();
				}
				if (skorBenarEfektif <= 0.0) skorBenarEfektif = 1.0;
				Double skorBenar = totalJawabanPilihanBenar * skorBenarEfektif;
				Double skorSalah = totalJawabanPilihanSalah * skorSalahBank;
				Double skorDefault = totalJawabanPilihanDefault * skorDefaultBank;

				Double skorYangDidapat = skorBenar + skorSalah + skorDefault;

//				System.out.println("skorYangDidapat -> " + skorYangDidapat + ", skorBenar -> " + skorBenar
//						+ ", skorSalah -> " + skorSalah + ", skorDefault -> " + skorDefault + ", skorDariJawaban -> "
//						+ skorDariJawaban + ", hasilUjianMahasiswa -> " + hasilUjianMahasiswa);

				return new Double[] { skorYangDidapat, Double.valueOf(skorBenarEfektif) };

			} else {

				double totalJawabanBenar = 0.0;

				List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);

				for (Long bankSoalDetailid : bankSoalDetails) {
					BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
							bankSoalDetailid.toString());
					if (bankSoalDetail != null) {
						if (bankSoalDetail != null && Boolean.TRUE.equals(bankSoalDetail.getBetul())) {
							totalJawabanBenar += 1.0;

							skorDariJawaban += (bankSoalDetail.getSkor() == null ? 0.0 : bankSoalDetail.getSkor());
						}
					}
				}
				bankSoalDetails = null;

				Double skorBenar = 0.0;
				if (totalJawabanBenar > 0.01) {

					if (jumlahDibatasi) {

						skorBenar = ((skorBerbeda ? skorDariJawaban : skorBenarBank)
								* (maksJawaban <= 0.0 ? 0.0 : (totalJawabanPilihanBenar / maksJawaban)));

						System.out.println("totalJawabanPilihanBenar -> " + totalJawabanPilihanBenar + ", maksimal -> "
								+ bankSoal.getMaksimalJumlahJawaban() + ", skorBenar -> " + skorBenar);

					} else {
						skorBenar = ((skorBerbeda ? skorDariJawaban : skorBenarBank)
								* ((totalJawabanPilihanBenar - totalJawabanPilihanSalah) / totalJawabanBenar));
					}
				}

				Double skorSalah = 0.0;
				if (totalJawabanPilihanBenar < 0.01 && !jumlahDibatasi) {
					skorSalah = skorSalahBank;
				}

				Double skorDefault = 0.0;
				if (totalJawabanPilihanBenar < 0.01 && totalJawabanPilihanDefault > 0.01
						&& !jumlahDibatasi) {
					skorDefault = skorDefaultBank;
				}

				Double skorYangDidapat = skorBenar + skorSalah + skorDefault;

				Set<Long> hasil = hasilUjianMahasiswa == null ? null
						: hasilUjianMahasiswa.ambilHasilUjianMahasiswaDetail(hasilUjianMahasiswaDetails, bankSoal);
				for (Long aid : hasil) {
					try {

						HasilUjianMahasiswaDetail a = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, aid.toString());
						if (a != null) {
							if ((a.getNilai() == null ? 0 : a.getNilai().intValue()) != skorYangDidapat.intValue()) {
								a.setNilai(skorYangDidapat);
								Session session = HibernateUtil.currentNativeSession();
								try {
									session.getTransaction().begin();
									Common.refreshUpdate(session, a);
									session.getTransaction().commit();
									// session.disconnect();
									if (session.isOpen()) {
										session.disconnect();
										ais.common.ElearningSessionUtil.closeQuietly(session);
									}
								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3034");
								}

								HibernateUtil.closeSession();
							}
						}
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}

				return new Double[] { skorYangDidapat, skorDariJawaban };

			}
		}

		return new Double[] {
				hasilUjianMahasiswaDetail.getNilai() == null ? 0.0 : hasilUjianMahasiswaDetail.getNilai(), 100.0 };
	}

	/**
	 * Menentukan SKOR MAKSIMAL yang mungkin diraih dari satu {@code BankSoal}.
	 *
	 * <p><b>Untuk apa:</b> menjadi penyebut pada setiap perhitungan persentase — baik pada
	 * agregasi {@link #hitungPilihanGanda(HasilUjianMahasiswa,Map)} maupun pada kolom "Max" per
	 * Sub-CPMK di {@link #hitungObe(HasilUjianMahasiswa,Map)} dan {@link #rincianSkorSubCpmk}.</p>
	 *
	 * <p><b>Urutan penentuan (tiga tingkat, berhenti pada yang pertama valid):</b></p>
	 * <ol>
	 *   <li>Bila soal TIDAK memakai skor per-jawaban ({@code skorJawabanBerbeda} tidak benar) dan
	 *       {@code bankSoal.getSkor()} lebih besar dari nol, nilai itulah yang dipakai.</li>
	 *   <li>Selain itu, skor seluruh {@code BankSoalDetail} yang ditandai BENAR dijumlahkan; bila
	 *       hasilnya lebih besar dari nol, itulah maksimalnya. Bila penjumlahan nol, sistem masih
	 *       mencoba {@code bankSoal.getSkor()} sekali lagi.</li>
	 *   <li><b>Fallback 1.0 (satu poin netral).</b> Bila soal memiliki kunci jawaban benar tetapi
	 *       seluruh kolom skornya kosong, mengembalikan 0 akan membuat penyebut nol sehingga
	 *       persentase Sub-CPMK MUSTAHIL dihitung dan kolom Skor/Max tampil kosong. Karena itu
	 *       nilai 1.0 dikembalikan agar soal tetap dapat dinilai secara relatif.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> kegagalan membaca detail soal ditampilkan lewat
	 * {@code Common.tampilErrorJikaAdmin} dan perhitungan tetap dilanjutkan dengan skor seadanya.
	 * Sengaja tidak melempar exception karena pemanggilnya berada di jalur perhitungan massal
	 * seluruh peserta — satu soal rusak tidak boleh menggagalkan nilai satu kelas.</p>
	 *
	 * @param bankSoal soal yang akan diukur; null menghasilkan {@code 0.0}
	 * @return skor maksimal soal; minimal {@code 1.0} bila soal memiliki kunci benar, atau
	 *         {@code 0.0} bila {@code bankSoal} null
	 */
	private static Double ambilSkorMaksimalBankSoal(BankSoal bankSoal) {
		if (bankSoal == null) {
			return 0.0;
		}

		Double skorDefaultSoal = bankSoal.getSkor() == null ? 0.0 : bankSoal.getSkor();
		if (!Boolean.TRUE.equals(bankSoal.getSkorJawabanBerbeda()) && skorDefaultSoal > 0.0) {
			return skorDefaultSoal;
		}

		Double skorMaksimal = 0.0;
		try {
			List<Long> bankSoalDetails = bankSoal.ambilBankSoalDetail(false);
			for (Long bankSoalDetailid : bankSoalDetails) {
				BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject.ambilData(BankSoalDetail.class,
						bankSoalDetailid.toString());
				if (bankSoalDetail != null && Boolean.TRUE.equals(bankSoalDetail.getBetul())) {
					skorMaksimal += bankSoalDetail.getSkor() == null ? 0.0 : bankSoalDetail.getSkor();
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		if (skorMaksimal > 0.0) return skorMaksimal;
		if (skorDefaultSoal > 0.0) return skorDefaultSoal;
		// Tanpa fallback ini soal dengan kunci benar tetapi skor kosong menghasilkan
		// maksimum 0, sehingga persentase Sub-CPMK tidak pernah dapat dihitung.
		return 1.0;
	}

	/**
	 * Menjumlahkan skor maksimal SELURUH soal yang benar-benar DITAMPILKAN kepada seorang peserta.
	 *
	 * <p><b>Untuk apa:</b> menjadi penyebut {@code jawabanBenarMax} pada
	 * {@link #hitungPilihanGanda(HasilUjianMahasiswa,Map)}. Penyebut ini harus mengikuti soal yang
	 * DITAMPILKAN (hasil pengacakan {@link #randomPosisiton(List,boolean,Label,Integer)}) — bukan
	 * seluruh bank soal — supaya peserta yang mendapat 20 soal acak dari 100 soal tidak dinilai
	 * terhadap 100 soal.</p>
	 *
	 * <p><b>Cara kerja:</b> memuat kembali daftar {@code UjianPunyaSoal} milik peserta lewat
	 * {@code hasilUjianMahasiswa.ambilUjianPunyaSoals(jmlDitampilkan, label, true)} — argumen
	 * {@code true} memaksa pengambilan segar dari basis data — lalu menjumlahkan
	 * {@link #ambilSkorMaksimalBankSoal(BankSoal)} untuk setiap soalnya.</p>
	 *
	 * <p><b>Catatan:</b> {@code new Label()} dilewatkan sekadar sebagai penampung pesan progres
	 * karena method ini dipanggil di luar konteks tampilan; label tersebut memang dibuang.</p>
	 *
	 * <p><b>Perhatian pemakaian:</b> hasil method ini dan penjumlahan per-soal di
	 * {@link #hitungPilihanGanda(HasilUjianMahasiswa,Map)} dapat berada pada SKALA yang berbeda
	 * (misalnya pada soal ber-{@code skorJawabanBerbeda}). Karena itu pemanggilnya memasang
	 * penjaga anti nilai&gt;100: bila penyebut dari method ini lebih kecil daripada skor yang
	 * didapat, penyebut dikembalikan ke penjumlahan per-soal yang sudah pasti seskala.</p>
	 *
	 * @param hasilUjianMahasiswa hasil ujian peserta; null atau tanpa {@code pertemuanPunyaUjian}
	 *                            menghasilkan {@code 0.0}
	 * @return total skor maksimal seluruh soal yang ditampilkan kepada peserta ini
	 */
	private static Double ambilSkorMaksimalSoalDitampilkan(HasilUjianMahasiswa hasilUjianMahasiswa) {
		if (hasilUjianMahasiswa == null || hasilUjianMahasiswa.getPertemuanPunyaUjian() == null) {
			return 0.0;
		}

		Double skorMaksimal = 0.0;
		try {
			MyArrayList<Long> ujianPunyaSoals = hasilUjianMahasiswa.ambilUjianPunyaSoals(
					hasilUjianMahasiswa.getPertemuanPunyaUjian().getJmlDitampilkan(), new Label(), true);
			for (Long ujianPunyaSoalid : ujianPunyaSoals) {
				UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						ujianPunyaSoalid.toString());
				if (ujianPunyaSoal != null) {
					skorMaksimal += ambilSkorMaksimalBankSoal(ujianPunyaSoal.getBankSoal());
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return skorMaksimal;
	}

	/**
	 * Menghitung dan menyimpan pemetaan skor ujian ke format penilaian OBE (Outcome-Based Education)
	 * bila kurikulum yang digunakan adalah kurikulum OBE.
	 *
	 * <p><b>Tujuan:</b> Dalam sistem OBE, setiap soal ujian dapat dipetakan ke Sub-CPMK
	 * (Capaian Pembelajaran Mata Kuliah) tertentu. Method ini mengagregasi skor per Sub-CPMK
	 * berdasarkan jawaban peserta, kemudian menyimpan hasil agregasi sebagai JSON di kolom
	 * {@code hasilJsonObe} pada {@code HasilUjianMahasiswa}. Data ini digunakan untuk laporan
	 * capaian OBE dan peringkat ketercapaian per CPMK.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Memeriksa apakah ujian berada dalam konteks perkuliahan OBE:
	 *       {@code kurikulum.apakahObe(tahunAjaran, ganjilGenap)}. Jika tidak OBE, method
	 *       langsung return tanpa melakukan apapun.</li>
	 *   <li>Mengambil {@code FormatNilai} dari perkuliahan dan membangun peta nomor → FormatNilai
	 *       via {@code pertemuanPunyaUjian.ambilMapNomor(formatNilais)}.</li>
	 *   <li>Mengambil semua soal ujian dengan mapping OBE via
	 *       {@code ujian.ambilUjianPunyaSoal(true, pertemuanPunyaUjian, ...)}.</li>
	 *   <li>Untuk setiap soal, mengambil skor peserta dari {@link #hitung} dan mengelompokkan
	 *       hasilnya per Sub-CPMK yang dipetakan di soal tersebut.</li>
	 *   <li>Membangun {@code JSONObject} hasil OBE dan menyimpannya ke
	 *       {@code hasilUjianMahasiswa.setHasilJsonObe(jsonObjectHasil.toString())}.</li>
	 * </ol>
	 *
	 * <p><b>Kondisi no-op:</b> Bila kurikulum bukan OBE, atau pertemuanPunyaUjian/pertemuan/
	 * perkuliahan/kurikulum null, method langsung return tanpa error. Ini aman untuk
	 * semua tipe peserta (mahasiswa, calon, siswa) — hanya mahasiswa dengan kurikulum OBE
	 * yang akan mengeksekusi logika penuh.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Bila format JSON OBE berubah (penambahan field baru), perbarui
	 * pembangunan {@code jsonObjectHasil} di sini dan pastikan laporan OBE yang membaca JSON
	 * ini juga diperbarui. Gunakan kunci yang konsisten agar tidak memecah laporan yang sudah ada.</p>
	 *
	 * @param hasilUjianMahasiswa    hasil ujian yang akan diperbarui dengan data OBE
	 * @param hasilUjianMahasiswaDetails peta lengkap jawaban peserta (untuk perhitungan skor per soal)
	 */
	/**
	 * <b>Catatan pemeliharaan:</b> blok komentar panjang tepat DI ATAS blok ini menjelaskan
	 * mekanisme perhitungan OBE secara umum, namun secara teknis ia yatim — Javadoc yang berlaku
	 * bagi sebuah method hanyalah blok TERAKHIR sebelum deklarasinya, yaitu blok ini. Uraian
	 * lengkap mekanismenya (kapan aktif, bagaimana Sub-CPMK diagregasi, format JSON hasil) kini
	 * didokumentasikan pada overload dua-argumen {@link #hitungObe(HasilUjianMahasiswa,Map)}.
	 *
	 * Overload hitungObe yang menerima formatNilais yang sudah di-pre-compute.
	 * Gunakan ini dari konteks multi-thread agar setDefaultPembobotan tidak dipanggil
	 * bersamaan dari 50 thread paralel (race condition saling reset persen=0).
	 */
	public static void hitungObe(HasilUjianMahasiswa hasilUjianMahasiswa,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails,
			List<FormatNilai> preComputedFormatNilais) {
		if (preComputedFormatNilais == null) {
			hitungObe(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);
			return;
		}
		JSONObject jsonObjectHasil = new JSONObject();
		if (hasilUjianMahasiswa.getPertemuanPunyaUjian() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum()
						.apakahObe(
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan()
										.getTahunAjaran(),
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan()
										.getGanjilGenap())) {
			try {
				List<FormatNilai> formatNilais = preComputedFormatNilais;
				TreeMap<Integer, FormatNilai> treeMap = hasilUjianMahasiswa.getPertemuanPunyaUjian().ambilMapNomor(formatNilais);

				// refresh=false: jangan reinit file cache soal — 50 thread concurrent menyebabkan
				// race condition (bersihkan+isi soal overlap) → soal partial → sub-CPMK hilang.
				Object[] objects = hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().ambilUjianPunyaSoal(false,
						hasilUjianMahasiswa.getPertemuanPunyaUjian(), "", 0, 1000);
				List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];

				java.util.Set<Long> bankSoalSudahDihitung = new java.util.HashSet<Long>();
				Long mhsIdPeserta = hasilUjianMahasiswa.getMahasiswa() != null
						? hasilUjianMahasiswa.getMahasiswa().getId()
						: null;
				java.util.Set<Long> subCpmkPeserta = hasilUjianMahasiswa.getPertemuanPunyaUjian()
						.ambilSubCpmkPeserta(mhsIdPeserta);
				int soalNo = 1;
				for (Long d : ujianPunyaSoalsData) {
					UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
							d.toString());
					if (ujianPunyaSoal != null) {
						try {
							FormatNilai formatNilai = treeMap.get(soalNo);
							soalNo++;
							if (formatNilai == null) { continue; }
							if (subCpmkPeserta != null && formatNilai.getId() != null
									&& !subCpmkPeserta.contains(formatNilai.getId())) {
								try {
									if (!jsonObjectHasil.has(formatNilai.getId().toString())) {
										jsonObjectHasil.put(formatNilai.getId().toString(), 0);
										jsonObjectHasil.put(formatNilai.getId().toString() + "_max", 0);
									}
								} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3193");}
								continue;
							}
							if (ujianPunyaSoal.getBankSoal() != null
									&& !bankSoalSudahDihitung.add(ujianPunyaSoal.getBankSoal().getId())) { continue; }
							Double nilai = jsonObjectHasil.isNull(formatNilai.getId().toString()) ? 0.0
									: jsonObjectHasil.getDouble(formatNilai.getId().toString());
							Double nilaiMax = jsonObjectHasil.isNull(formatNilai.getId().toString() + "_max") ? 0.0
									: jsonObjectHasil.getDouble(formatNilai.getId().toString() + "_max");
							nilaiMax += ambilSkorMaksimalBankSoal(ujianPunyaSoal.getBankSoal());
							for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
								boolean selesai = false;
								for (Long hasilUjianMahasiswaDetailid : aa) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
									if (hasilUjianMahasiswaDetail != null
											&& hasilUjianMahasiswaDetail.getBankSoal() != null
											&& ujianPunyaSoal.getBankSoal() != null
											&& hasilUjianMahasiswaDetail.getBankSoal().getId()
													.equals(ujianPunyaSoal.getBankSoal().getId())) {
										Double[] skor = ProsesUjianHelper.hitung(hasilUjianMahasiswaDetail, hasilUjianMahasiswaDetails);
										nilai += skor[0];
										selesai = true;
										break;
									}
								}
								if (selesai) { break; }
							}
							jsonObjectHasil.put(formatNilai.getId().toString(), nilai);
							jsonObjectHasil.put(formatNilai.getId().toString() + "_max", nilaiMax);
						} catch (Exception e) {
							System.out.println("[HITUNG-OBE-PRE-ERROR] peserta="
									+ (hasilUjianMahasiswa == null ? "?" : hasilUjianMahasiswa.getId())
									+ " soalNo=" + (soalNo - 1) + " -> " + e);
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3227");
						}
					}
				}
			} catch (Exception e) {
				System.out.println("[HITUNG-OBE-PRE-ERROR-LUAR] peserta="
						+ (hasilUjianMahasiswa == null ? "?" : hasilUjianMahasiswa.getId()) + " -> " + e);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3234");
			}
		}
		hasilUjianMahasiswa.setNilaiObe(jsonObjectHasil.toString());
	}

	/**
	 * Menghitung dan menyimpan pemetaan skor ujian ke format penilaian OBE (Outcome-Based
	 * Education) untuk satu peserta.
	 *
	 * <p><b>Kapan aktif:</b> hanya bila rantai {@code pertemuanPunyaUjian - pertemuan -
	 * perkuliahan - kurikulum} lengkap DAN
	 * {@code kurikulum.apakahObe(tahunAjaran, ganjilGenap)} bernilai benar. Bila tidak, method
	 * tetap menulis {@code nilaiObe} berisi JSON kosong — jadi ia TIDAK pernah meninggalkan kolom
	 * apa adanya.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Memuat {@code FormatNilai} (daftar Sub-CPMK) lewat session Hibernate TERSENDIRI yang
	 *       dibuka dan ditutup di blok {@code finally}, supaya tidak mengganggu session ujian.</li>
	 *   <li>{@code pertemuanPunyaUjian.ambilMapNomor(formatNilais)} memetakan NOMOR URUT soal ke
	 *       Sub-CPMK; nomor urut inilah satu-satunya kunci penghubung soal dengan Sub-CPMK.</li>
	 *   <li>Daftar soal diambil dengan {@code refresh=false} — disengaja: pemanggilan dari
	 *       "Hitung Ulang Semua" berjalan puluhan thread paralel, dan {@code refresh=true} akan
	 *       membuat cache berkas soal dibersihkan-dan-diisi secara tumpang tindih sehingga soal
	 *       terbaca sebagian dan Sub-CPMK hilang.</li>
	 *   <li>Soal dengan {@code BankSoal} yang sudah pernah dihitung DILEWATI
	 *       ({@code bankSoalSudahDihitung}), agar baris soal ganda — akibat ujian ulang yang
	 *       menambah {@code UjianPunyaSoal} untuk Sub-CPMK yang diulang — tidak menggelembungkan
	 *       skor maupun skor maksimalnya.</li>
	 *   <li>Sub-CPMK yang TIDAK dikerjakan peserta ini (kasus remedial sebagian) tetap ditulis ke
	 *       JSON dengan nilai 0 dan {@code _max} 0, supaya kolom laporan tidak kosong melompong.</li>
	 *   <li>Untuk tiap soal: {@code _max} ditambah {@link #ambilSkorMaksimalBankSoal(BankSoal)},
	 *       dan nilainya ditambah hasil {@link #hitung(HasilUjianMahasiswaDetail,Map)} atas jawaban
	 *       peserta pada soal tersebut.</li>
	 *   <li>Hasil akhir ditulis sebagai JSON ke {@code hasilUjianMahasiswa.setNilaiObe(...)} dengan
	 *       kunci berupa id {@code FormatNilai} dan pasangannya berakhiran {@code _max}.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> kesalahan per-soal maupun kesalahan menyeluruh SELALU dicetak
	 * beserta konteksnya ({@code [HITUNG-OBE-ERROR] peserta=... soalNo=... bankSoal=...}). Ini
	 * disengaja: versi lama membisukan {@code printStackTrace} sehingga NPE membuat
	 * {@code jsonObjectHasil.put(...)} terlewat tanpa jejak, dan kolom Skor/Max serta Nilai tampil
	 * KOSONG justru untuk peserta yang MENJAWAB.</p>
	 *
	 * <p><b>Pilih overload yang mana:</b> gunakan {@link #hitungObe(HasilUjianMahasiswa,Map,List)}
	 * bila memanggil dari konteks multi-thread — overload tersebut menerima {@code formatNilais}
	 * yang sudah dihitung sebelumnya sehingga {@code setDefaultPembobotan} tidak dijalankan
	 * bersamaan oleh puluhan thread yang saling mereset persen menjadi 0.</p>
	 *
	 * @param hasilUjianMahasiswa        hasil ujian peserta yang kolom {@code nilaiObe}-nya ditulis
	 * @param hasilUjianMahasiswaDetails peta lengkap jawaban peserta (bankSoalId ke himpunan detailId)
	 */
	@SuppressWarnings("unchecked")
	public static void hitungObe(HasilUjianMahasiswa hasilUjianMahasiswa,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails) {
		JSONObject jsonObjectHasil = new JSONObject();
		if (hasilUjianMahasiswa.getPertemuanPunyaUjian() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum() != null
				&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan().getKurikulum()
						.apakahObe(
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan()
										.getTahunAjaran(),
								hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan().getPerkuliahan()
										.getGanjilGenap())) {
			try {
				Pertemuan pertemuan = hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan();
				Session sessionFmt = HibernateUtil.getSessionFactory().openSession();
				List<FormatNilai> formatNilais;
				try {
					formatNilais = Common.getFormatNilais(sessionFmt, pertemuan.getPerkuliahan());
				} finally {
					if (sessionFmt != null && sessionFmt.isOpen()) try { sessionFmt.close(); } catch (Exception exFmt) { ais.common.ErrorAuditUtil.record(exFmt, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3261");}
				}
				TreeMap<Integer, FormatNilai> treeMap = hasilUjianMahasiswa.getPertemuanPunyaUjian().ambilMapNomor(formatNilais);

				// refresh=false: sama seperti versi preComputed — hindari race condition reinit soal
				Object[] objects = hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().ambilUjianPunyaSoal(false,
						hasilUjianMahasiswa.getPertemuanPunyaUjian(), "", 0, 1000);
				List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];

				// Skor MAX per Sub-CPMK harus berasal dari soal Sub-CPMK ITU SAJA dan dihitung
				// SEKALI per soal. Baris soal yang muncul ganda (mis. akibat "ujian ulang" yang
				// menambah UjianPunyaSoal untuk Sub-CPMK yang diulang) dilewati agar max/skor tidak
				// menggelembung menjadi total seluruh soal.
				java.util.Set<Long> bankSoalSudahDihitung = new java.util.HashSet<Long>();
				// OBE remedial: himpunan Sub-CPMK yang DIKERJAKAN peserta ini (null = semua Sub-CPMK ujian).
				Long mhsIdPeserta = hasilUjianMahasiswa.getMahasiswa() != null
						? hasilUjianMahasiswa.getMahasiswa().getId()
						: null;
				java.util.Set<Long> subCpmkPeserta = hasilUjianMahasiswa.getPertemuanPunyaUjian()
						.ambilSubCpmkPeserta(mhsIdPeserta);
				int soalNo = 1;
				for (Long d : ujianPunyaSoalsData) {

					UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
							d.toString());
					if (ujianPunyaSoal != null) {

						try {
							FormatNilai formatNilai = treeMap.get(soalNo);
							soalNo++;
							if (formatNilai == null) {
								continue;
							}
							// Lewati Sub-CPMK yang TIDAK dikerjakan peserta ini (mis. remedial sebagian Sub-CPMK).
							if (subCpmkPeserta != null && formatNilai.getId() != null
									&& !subCpmkPeserta.contains(formatNilai.getId())) {
								try {
									if (!jsonObjectHasil.has(formatNilai.getId().toString())) {
										jsonObjectHasil.put(formatNilai.getId().toString(), 0);
										jsonObjectHasil.put(formatNilai.getId().toString() + "_max", 0);
									}
								} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3301");}
								continue;
							}

							if (ujianPunyaSoal.getBankSoal() != null
									&& !bankSoalSudahDihitung.add(ujianPunyaSoal.getBankSoal().getId())) {
								continue;
							}

							Double nilai = jsonObjectHasil.isNull(formatNilai.getId().toString()) ? 0.0
									: jsonObjectHasil.getDouble(formatNilai.getId().toString());

							Double nilaiMax = jsonObjectHasil.isNull(formatNilai.getId().toString() + "_max") ? 0.0
									: jsonObjectHasil.getDouble(formatNilai.getId().toString() + "_max");
							nilaiMax += ambilSkorMaksimalBankSoal(ujianPunyaSoal.getBankSoal());

							for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
								boolean selesai = false;
								for (Long hasilUjianMahasiswaDetailid : aa) {
									HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													hasilUjianMahasiswaDetailid.toString());
									if (hasilUjianMahasiswaDetail != null
											&& hasilUjianMahasiswaDetail.getBankSoal() != null
											&& ujianPunyaSoal.getBankSoal() != null
											&& hasilUjianMahasiswaDetail.getBankSoal().getId()
													.equals(ujianPunyaSoal.getBankSoal().getId())) {

										Double[] skor = ProsesUjianHelper.hitung(hasilUjianMahasiswaDetail,
												hasilUjianMahasiswaDetails);
										Double skorYangDidapat = skor[0];

										nilai += skorYangDidapat;
										selesai = true;
										break;
									}
								}
								if (selesai) {
									break;
								}
							}

							jsonObjectHasil.put(formatNilai.getId().toString(), nilai);
							jsonObjectHasil.put(formatNilai.getId().toString() + "_max", nilaiMax);
						} catch (Exception e) {
							// DIAGNOSTIK: sebelumnya printStackTrace di-comment (membisu) → NPE penilaian
							// per Sub-CPMK hilang tanpa jejak & put() dilewati → kolom Skor/Max & Nilai
							// KOSONG. Kini SELALU tampilkan errornya beserta konteks (peserta/soal/bank soal).
							System.out.println("[HITUNG-OBE-ERROR] peserta="
									+ (hasilUjianMahasiswa == null ? "?" : hasilUjianMahasiswa.getId()) + " soalNo="
									+ (soalNo - 1) + " bankSoal="
									+ (ujianPunyaSoal == null || ujianPunyaSoal.getBankSoal() == null ? "null"
											: ujianPunyaSoal.getBankSoal().getId())
									+ " -> " + e);
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3355");
						}
					}
				}

			} catch (Exception e) {
				System.out.println("[HITUNG-OBE-ERROR-LUAR] peserta="
						+ (hasilUjianMahasiswa == null ? "?" : hasilUjianMahasiswa.getId()) + " -> " + e);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3363");
			}
		}

		hasilUjianMahasiswa.setNilaiObe(jsonObjectHasil.toString());
	}

	/**
	 * Mengembalikan rincian per-soal untuk satu Sub-CPMK ({@link FormatNilai}) milik seorang
	 * peserta. Tiap elemen = {@code Object[]{ nomorUrut(Integer), teksSoal(String),
	 * skorDidapat(Double), skorMax(Double) }}. Dipakai popup "Rincian Skor" saat nilai per
	 * Sub-CPMK diklik. Soal duplikat (mis. akibat ujian ulang) dihitung SEKALI — konsisten
	 * dengan {@link #hitungObe}.
	 */
	@SuppressWarnings("unchecked")
	public static List<Object[]> rincianSkorSubCpmk(HasilUjianMahasiswa hasilUjianMahasiswa,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails, Long formatNilaiId) {
		List<Object[]> hasil = new java.util.ArrayList<Object[]>();
		try {
			if (hasilUjianMahasiswa.getPertemuanPunyaUjian() == null
					|| hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan() == null) {
				return hasil;
			}
			// OBE remedial: bila peserta ini TIDAK mengerjakan Sub-CPMK yang diminta, tak ada rincian.
			Long mhsIdPeserta = hasilUjianMahasiswa.getMahasiswa() != null ? hasilUjianMahasiswa.getMahasiswa().getId()
					: null;
			java.util.Set<Long> subCpmkPeserta = hasilUjianMahasiswa.getPertemuanPunyaUjian()
					.ambilSubCpmkPeserta(mhsIdPeserta);
			if (subCpmkPeserta != null && formatNilaiId != null && !subCpmkPeserta.contains(formatNilaiId)) {
				return hasil;
			}
			Pertemuan pertemuan = hasilUjianMahasiswa.getPertemuanPunyaUjian().getPertemuan();
			Session sessionFmt = HibernateUtil.getSessionFactory().openSession();
			List<FormatNilai> formatNilais;
			try {
				formatNilais = Common.getFormatNilais(sessionFmt, pertemuan.getPerkuliahan());
			} finally {
				if (sessionFmt != null && sessionFmt.isOpen()) try { sessionFmt.close(); } catch (Exception exFmt) { ais.common.ErrorAuditUtil.record(exFmt, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3400");}
			}
			TreeMap<Integer, FormatNilai> treeMap = hasilUjianMahasiswa.getPertemuanPunyaUjian()
					.ambilMapNomor(formatNilais);

			Object[] objects = hasilUjianMahasiswa.getPertemuanPunyaUjian().getUjian().ambilUjianPunyaSoal(true,
					hasilUjianMahasiswa.getPertemuanPunyaUjian(), "", 0, 1000);
			List<Long> ujianPunyaSoalsData = (List<Long>) objects[0];

			java.util.Set<Long> sudah = new java.util.HashSet<Long>();
			int soalNo = 1;
			int no = 1;
			for (Long d : ujianPunyaSoalsData) {
				UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						d.toString());
				if (ujianPunyaSoal == null) {
					continue;
				}
				FormatNilai formatNilai = treeMap.get(soalNo);
				soalNo++;
				if (formatNilai == null || !formatNilai.getId().equals(formatNilaiId)) {
					continue;
				}
				if (ujianPunyaSoal.getBankSoal() == null || !sudah.add(ujianPunyaSoal.getBankSoal().getId())) {
					continue;
				}

				Double skorMax = ambilSkorMaksimalBankSoal(ujianPunyaSoal.getBankSoal());
				Double skorDidapat = 0.0;
				for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
					boolean selesai = false;
					for (Long detId : aa) {
						HasilUjianMahasiswaDetail det = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, detId.toString());
						if (det != null && det.getBankSoal() != null
								&& det.getBankSoal().getId().equals(ujianPunyaSoal.getBankSoal().getId())) {
							Double[] skor = hitung(det, hasilUjianMahasiswaDetails);
							skorDidapat = skor[0];
							selesai = true;
							break;
						}
					}
					if (selesai) {
						break;
					}
				}

				hasil.add(new Object[] { Integer.valueOf(no++), ujianPunyaSoal.getBankSoal().getSoal(), skorDidapat,
						skorMax });
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:3451");
		}
		return hasil;
	}

	/**
	 * Mengagregasi total skor seluruh soal pilihan ganda dan menyimpan hasilnya ke
	 * {@code HasilUjianMahasiswa}. Ini adalah method agregasi yang memanggil {@link #hitung}
	 * untuk setiap soal unik dalam daftar jawaban.
	 *
	 * <p><b>Tujuan:</b> Menghitung dua nilai agregat: total skor yang berhasil diraih peserta
	 * ({@code jawabanBenar}) dan total skor maksimal yang mungkin ({@code jawabanBenarMax}).
	 * Keduanya kemudian disimpan ke entitas {@code HasilUjianMahasiswa} untuk ditampilkan
	 * di rekap nilai dan laporan ujian.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Mengiterasi seluruh nilai di {@code hasilUjianMahasiswaDetails} (semua jawaban peserta).</li>
	 *   <li>Menggunakan {@code Set<Long> longs} untuk memastikan setiap {@code BankSoal} hanya
	 *       dihitung sekali (deduplikasi — soal berganda bisa muncul beberapa kali di map).</li>
	 *   <li>Untuk setiap soal unik, memanggil {@link #hitung(HasilUjianMahasiswaDetail,Map)}
	 *       yang mengembalikan {skorYangDidapat, skorDariJawaban}.</li>
	 *   <li>Mengakumulasi {@code benar} (total skor didapat) dan {@code skorMax} (total skor max).</li>
	 *   <li>Menyimpan hasil ke {@code hasilUjianMahasiswa.setJawabanBenar(benar)} dan
	 *       {@code setJawabanBenarMax(skorMax)}.</li>
	 *   <li>Memanggil {@link #hitungWaktu} di akhir untuk mengisi {@code lamaPengerjaan} dan
	 *       {@code sisaWaktuPengerjaan}.</li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan:</b> Bila jenis soal baru ditambahkan yang tidak berbasis pilihan ganda
	 * (misalnya soal numerik), pertimbangkan membuat method {@code hitungJenisLain} terpisah
	 * dan memanggil keduanya dari {@link #generateHasilUjian}.</p>
	 *
	 * @param hasilUjianMahasiswa    entitas yang akan diperbarui dengan skor total
	 * @param hasilUjianMahasiswaDetails peta jawaban peserta (bankSoalId → Set&lt;detailId&gt;)
	 */
	public static void hitungPilihanGanda(HasilUjianMahasiswa hasilUjianMahasiswa,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails) {

		Double benar = 0.0;
		Double skorMax = 0.0;

		Set<Long> longs = new HashSet<Long>();
		for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
			for (Long aaa : aa) {
				HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, aaa.toString());
				if (hasilUjianMahasiswaDetail != null) {
					BankSoal bankSoal = hasilUjianMahasiswaDetail == null ? null
							: hasilUjianMahasiswaDetail.getBankSoal();
					if (bankSoal != null && !longs.contains(bankSoal.getId())) {
						longs.add(bankSoal.getId());
						Double[] skor = ProsesUjianHelper.hitung(hasilUjianMahasiswaDetail, hasilUjianMahasiswaDetails);
						// PENYEBAB UTAMA kolom Skor/Max & Nilai KOSONG untuk peserta yang MENJAWAB: method
						// ini dipanggil SETELAH hitungObe DALAM SATU transaksi (Hitung Ulang Semua). Bila
						// baris skorBenarMax me-unbox getSkorJawabanBerbeda() (Boolean) / getSkor() (Double)
						// yang NULL (umum di REMEDIAL), NPE me-ROLLBACK seluruh transaksi peserta itu →
						// nilaiObe yang sudah disusun hitungObe TIDAK jadi tersimpan → kolom kosong. Peserta
						// yang TIDAK menjawab tak masuk loop ini → transaksinya sukses → nilai tampil (itulah
						// inversinya). Semua nilai dibuat null-safe di sini.
						Double skorYangDidapat = (skor == null || skor.length < 1 || skor[0] == null) ? 0.0 : skor[0];
						Double skorDariJawaban = (skor == null || skor.length < 2 || skor[1] == null) ? 0.0 : skor[1];
						Double skorBenarMax = ambilSkorMaksimalBankSoal(bankSoal);
						// TIPE SOAL BEDA SKALA: Menjodohkan/Mengurutkan menyimpan nilai sbg PERSENTASE (0-100),
						// sedangkan Pilihan Ganda menyimpan POIN (0/skor). Bila dijumlah mentah, 100 (=100% benar pd
						// soal menjodohkan) masuk sbg 100 POIN -> jawabanBenar menggelembung -> nilai > 100 (kasus
						// TATA MAYANG soal "fenitoin"). Maka utk tipe persen: kontribusi poin = (persen/100) * skor
						// maksimal soal; penyebut memakai skor maksimal soal (ambilSkorMaksimalBankSoal) yg konsisten.
						String jenisPgSoal = bankSoal.getJenisPilihanGanda();
						boolean skorPersen = BankSoal.MENGURUTKAN.equals(jenisPgSoal)
								|| BankSoal.MENJODOHKAN.equals(jenisPgSoal);
						double maksSoalIni;
						double didapatSoalIni;
						if (skorPersen) {
							maksSoalIni = ambilSkorMaksimalBankSoal(bankSoal); // poin sebenarnya soal ini
							double persen = skorYangDidapat == null ? 0.0 : skorYangDidapat;
							persen = persen < 0.0 ? 0.0 : (persen > 100.0 ? 100.0 : persen);
							didapatSoalIni = maksSoalIni <= 0.0 ? 0.0 : (persen / 100.0) * maksSoalIni;
						} else {
							maksSoalIni = (skorBenarMax == null || skorBenarMax < 0.0) ? 0.0 : skorBenarMax;
							didapatSoalIni = skorYangDidapat == null ? 0.0 : skorYangDidapat;
							// ANTI-INFLASI data rusak: skor didapat tak boleh melebihi skor maksimal soal.
							if (didapatSoalIni > maksSoalIni) {
								didapatSoalIni = maksSoalIni;
							}
						}
						benar += didapatSoalIni;
						skorMax += maksSoalIni;
					}
				}
			}
		}

		hasilUjianMahasiswa.setJawabanBenar(benar);
		Double skorMaxDariSoalDijawab = skorMax; // penjumlahan per-soal (skala SAMA dengan 'benar')
		Double skorMaxSoalDitampilkan = ambilSkorMaksimalSoalDitampilkan(hasilUjianMahasiswa);
		if (skorMaxSoalDitampilkan > 0.0) {
			skorMax = skorMaxSoalDitampilkan;
		}
		// GUARD anti nilai>100: jawabanBenarMax (penyebut) TIDAK BOLEH lebih kecil dari jawabanBenar
		// (pembilang) - skor yang DIDAPAT mustahil melebihi skor MAKSIMAL. Kejanggalan ini muncul bila
		// 'skorMaxSoalDitampilkan' memakai skala berbeda dari 'benar' (mis. soal skorJawabanBerbeda),
		// sehingga nilai = benar/max*100 melonjak >100 (kasus TATA MAYANG: 122/50*100 = 244). Bila
		// terjadi, pakai skorMax hasil penjumlahan per-soal (skala IDENTIK dgn benar & >= benar) sbg
		// penyebut agar nilai <= 100 dan konsisten dengan perhitungan saat ujian berlangsung.
		if (benar != null && (skorMax == null || skorMax <= 0.0 || benar > skorMax)
				&& skorMaxDariSoalDijawab != null && skorMaxDariSoalDijawab >= benar) {
			skorMax = skorMaxDariSoalDijawab;
		}
		hasilUjianMahasiswa.setJawabanBenarMax(skorMax);

		hitungWaktu(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);

	}

	/**
	 * Menghitung dan menyimpan lama pengerjaan ujian serta sisa waktu berdasarkan
	 * {@code waktuJawab} dari detail jawaban peserta.
	 *
	 * <p><b>Tujuan:</b> Merekam berapa lama peserta mengerjakan ujian (dari jawaban pertama
	 * hingga terakhir) dan berapa sisa waktu yang tidak digunakan. Data ini dipakai untuk
	 * statistik ujian, identifikasi kecurangan (selesai terlalu cepat), dan laporan proktor.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Mengiterasi semua detail jawaban dan mencari tanggal {@code waktuJawab} paling awal
	 *       (= {@code startDate}) dan paling akhir (= {@code endDate}).</li>
	 *   <li>Menghitung durasi: {@code endDate - startDate} dalam milidetik, lalu dikonversi ke
	 *       jam, menit, detik menggunakan operator modulo.</li>
	 *   <li>Menyimpan {@code mulaiPada = startDate}, {@code selesaiPada = endDate}, dan
	 *       {@code lamaPengerjaan = GregorianCalendar(0,0,0, jam, menit, detik)}.</li>
	 *   <li>Bila ujian dibatasi waktu ({@code dibatasiWaktu=true}) dan bukan mode "tiap soal
	 *       punya waktu sendiri", menghitung sisa waktu:
	 *       {@code sisaWaktuPengerjaan = lama ujian - lamaPengerjaan}.</li>
	 *   <li>Bila tidak ada data waktu (semua {@code waktuJawab} null), field waktu tidak diubah.</li>
	 * </ol>
	 *
	 * <p><b>Limitasi:</b> Jika peserta mengerjakan tanpa koneksi internet dan sinkronisasi
	 * gagal, {@code waktuJawab} mungkin tidak akurat. Method ini mempercayai data yang ada
	 * di database tanpa validasi tambahan.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Bila platform menambah fitur jeda ujian (pause/resume),
	 * logika perhitungan durasi di sini perlu dimodifikasi untuk mengurangkan waktu jeda
	 * dari total durasi.</p>
	 *
	 * @param hasilUjianMahasiswa    entitas yang akan diperbarui dengan data waktu
	 * @param hasilUjianMahasiswaDetails peta jawaban peserta untuk mengambil timestamp jawaban
	 */
	public static void hitungWaktu(HasilUjianMahasiswa hasilUjianMahasiswa,
			Map<Long, Set<Long>> hasilUjianMahasiswaDetails) {

		Date startDate = null;
		Date endDate = null;
		for (Set<Long> aa : hasilUjianMahasiswaDetails.values()) {
			for (Long hasilUjianMahasiswaDetailid : aa) {
				HasilUjianMahasiswaDetail hasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
						.ambilData(HasilUjianMahasiswaDetail.class, hasilUjianMahasiswaDetailid.toString());
				if (hasilUjianMahasiswaDetail != null) {
					if (startDate == null || (hasilUjianMahasiswaDetail.getWaktuJawab() != null
							&& hasilUjianMahasiswaDetail.getWaktuJawab().before(startDate))) {
						startDate = hasilUjianMahasiswa == null ? null : hasilUjianMahasiswaDetail.getWaktuJawab();
					}
					if (endDate == null || (hasilUjianMahasiswaDetail.getWaktuJawab() != null
							&& hasilUjianMahasiswaDetail.getWaktuJawab().after(endDate))) {
						endDate = hasilUjianMahasiswa == null ? null : hasilUjianMahasiswaDetail.getWaktuJawab();
					}
				}
			}
		}

		if (startDate != null && endDate != null) {

			hasilUjianMahasiswa.setSelesaiPada(endDate);
			hasilUjianMahasiswa.setMulaiPada(startDate);

			long durationInMillis = endDate.getTime() - startDate.getTime();

			long second = (durationInMillis / 1000) % 60;
			long minute = (durationInMillis / (1000 * 60)) % 60;
			long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

			System.out.println("lama pengerjaan durationInMillis => " + durationInMillis + " second " + second
					+ " minute " + minute + " hour " + hour + " ");

			hasilUjianMahasiswa.setLamaPengerjaan(
					new GregorianCalendar(0, 0, 0, (int) hour, (int) minute, (int) second).getTime());

			if (Boolean.TRUE.equals(hasilUjianMahasiswa.getPertemuanPunyaUjian().getDibatasiWaktu())
					&& hasilUjianMahasiswa.getPertemuanPunyaUjian().getLama() != null
					&& !Boolean.TRUE.equals(hasilUjianMahasiswa.getPertemuanPunyaUjian().getTiapSoal())) {

				Calendar c1 = ais.ui.util.WaktuUtil.getCalendar();
				c1.setTime(hasilUjianMahasiswa.getLamaPengerjaan());
				c1.set(Calendar.YEAR, 0);
				c1.set(Calendar.MONTH, 0);
				c1.set(Calendar.DATE, 1);

				Calendar c2 = ais.ui.util.WaktuUtil.getCalendar();
				c2.setTime(hasilUjianMahasiswa.getPertemuanPunyaUjian().getLama());
				c2.set(Calendar.YEAR, 0);
				c2.set(Calendar.MONTH, 0);
				c2.set(Calendar.DATE, 1);

				durationInMillis = c2.getTimeInMillis() - c1.getTimeInMillis();

				second = (durationInMillis / 1000) % 60;
				minute = (durationInMillis / (1000 * 60)) % 60;
				hour = (durationInMillis / (1000 * 60 * 60)) % 24;

//				System.out.println("sisa waktu durationInMillis => " + durationInMillis + " second " + second
//						+ " minute " + minute + " hour " + hour + " ");

				hasilUjianMahasiswa.setSisaWaktuPengerjaan(
						new GregorianCalendar(0, 0, 0, (int) hour, (int) minute, (int) second).getTime());
			} else {
				hasilUjianMahasiswa.setSisaWaktuPengerjaan(null);
			}

		}
	}

	/**
	 * Delegator ke overload lengkap dengan {@code warnings=null}.
	 * Lihat {@link #generateHasilUjian(List,HasilUjianMahasiswa,PertemuanPunyaUjian,Map,List)}.
	 *
	 * @param ujianPunyaSoals daftar soal yang dikerjakan dalam sesi ini
	 * @param hasilUjianMahasiswa hasil ujian peserta yang akan diperbarui
	 * @param pertemuanPunyaUjian konfigurasi ujian (jenis, lama, dll.)
	 * @param hasilUjianMahasiswaDetails peta jawaban peserta
	 * @return true bila berhasil; false bila hasilUjianMahasiswa null
	 */
	public static boolean generateHasilUjian(List<Long> ujianPunyaSoals, HasilUjianMahasiswa hasilUjianMahasiswa,
			PertemuanPunyaUjian pertemuanPunyaUjian, Map<Long, Set<Long>> hasilUjianMahasiswaDetails) {
		List<String> warnings = null;
		return generateHasilUjian(ujianPunyaSoals, hasilUjianMahasiswa, pertemuanPunyaUjian, hasilUjianMahasiswaDetails,
				warnings);
	}

	/**
	 * Menghasilkan (finalisasi) nilai akhir ujian peserta: menghitung jumlah soal, memanggil
	 * pipeline perhitungan skor, dan menyimpan hasil ke database dalam satu transaksi.
	 *
	 * <p><b>Tujuan:</b> Ini adalah method yang dipanggil saat ujian berakhir (oleh {@link #onSelesai})
	 * maupun saat autosave recompute berkala (oleh {@code UjianRecomputeUtil.jadwalkanHitungUlang}).
	 * Method ini harus idempotent — dapat dipanggil berkali-kali tanpa menyebabkan data inkonsisten,
	 * karena setiap panggilan menimpa nilai lama dengan kalkulasi terbaru.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Guard: bila {@code hasilUjianMahasiswa == null} (mode preview), langsung return true.</li>
	 *   <li>Menghitung total skor soal ({@code jumlahSoal}) dengan menjumlah
	 *       {@code bankSoal.getSkor()} dari semua {@code UjianPunyaSoal} dalam sesi.</li>
	 *   <li>Memanggil {@link #hitungObe} untuk mengisi data OBE bila relevan.</li>
	 *   <li>Berdasarkan jenis ujian ({@code BankSoal.PILIHAN_GANDA} vs lainnya):
	 *     <ul>
	 *       <li>PILIHAN_GANDA: memanggil {@link #hitungPilihanGanda} yang juga memanggil
	 *           {@link #hitungWaktu}.</li>
	 *       <li>Lainnya (esai): set {@code jawabanBenar=0} dan panggil {@link #hitungWaktu}.</li>
	 *     </ul>
	 *   </li>
	 *   <li><b>Critical:</b> Setelah method hitung* selesai, mengambil ULANG native session
	 *       ({@code HibernateUtil.currentNativeSession()}) karena method hitung* mungkin telah
	 *       menutup session lama. Ini mencegah "Session is closed!" exception.</li>
	 *   <li>Menyimpan {@code hasilUjianMahasiswa} ke database dan menutup session.</li>
	 * </ol>
	 *
	 * <p><b>Penanganan error:</b> Exception ditangkap via {@code Common.tampilErrorJikaAdmin},
	 * tetapi session selalu ditutup di blok {@code finally} untuk menghindari kebocoran koneksi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Komentar inline "PENTING" menjelaskan alasan pengambilan ulang
	 * session setelah hitung*. Jangan hapus atau refactor pattern ini tanpa memahami bahwa
	 * {@code currentNativeSession()} dapat tertutup secara tidak terduga oleh method hitung*
	 * ketika mengakses cache GeneralValueObject dengan native session.</p>
	 *
	 * @param ujianPunyaSoals daftar soal yang dikerjakan dalam sesi ini (untuk total jumlahSoal)
	 * @param hasilUjianMahasiswa hasil ujian yang akan diperbarui; null = no-op (return true)
	 * @param pertemuanPunyaUjian konfigurasi ujian (untuk menentukan jenis perhitungan)
	 * @param hasilUjianMahasiswaDetails peta lengkap jawaban peserta
	 * @param warnings list peringatan (saat ini tidak digunakan; dipertahankan untuk extensibility)
	 * @return true bila proses berhasil atau peserta null; false tidak pernah dikembalikan
	 *         (exception langsung ditampilkan)
	 */
	public static boolean generateHasilUjian(List<Long> ujianPunyaSoals, HasilUjianMahasiswa hasilUjianMahasiswa,
			PertemuanPunyaUjian pertemuanPunyaUjian, Map<Long, Set<Long>> hasilUjianMahasiswaDetails,
			List<String> warnings) {
		if (hasilUjianMahasiswa != null) {

			Double jumlahSoal = 0.0;
			for (Long ujianPunyaSoalid : ujianPunyaSoals) {
				UjianPunyaSoal ujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
						ujianPunyaSoalid.toString());
				if (ujianPunyaSoal != null) {

					jumlahSoal += ujianPunyaSoal.getBankSoal().getSkor();

				}
			}

			Session session = HibernateUtil.currentNativeSession();
			try {
				session.refresh(hasilUjianMahasiswa);
				hasilUjianMahasiswa.setJumlahSoal(jumlahSoal);

				hitungObe(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);

				if (pertemuanPunyaUjian.getUjian().getJenis().equals(BankSoal.PILIHAN_GANDA)) {
					hitungPilihanGanda(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);
				} else {
					hasilUjianMahasiswa.setJawabanBenar(0.0);
					hitungWaktu(hasilUjianMahasiswa, hasilUjianMahasiswaDetails);
				}

				// PENTING: method hitung* di atas (lewat GeneralValueObject.ambilData(...,true) dsb.)
				// dapat MENUTUP native ThreadLocal session, sehingga referensi 'session' di atas bisa
				// sudah closed → session.getTransaction() melempar "Session is closed!". Ambil ulang
				// session native yang DIJAMIN open tepat sebelum menulis (currentNativeSession akan
				// membuka ulang bila yang lama sudah tertutup).
				session = HibernateUtil.currentNativeSession();
				org.hibernate.Transaction tx = session.beginTransaction();
				Common.refreshSaveOrUpdate(session, hasilUjianMahasiswa);
				tx.commit();
				// session.disconnect();
				if (session.isOpen()) {
					ais.common.ElearningSessionUtil.closeQuietly(session);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
				PesanFormalHelper.tampilkanGagalException(
						"menghitung dan menyimpan hasil akhir ujian (CBT) peserta",
						e, new String[] {
								"Jangan menutup halaman ini terlebih dahulu; segera hubungi Admin/Pengawas ujian.",
								"Sertakan tangkapan layar (screenshot) pesan ini serta nama/NIM peserta saat melapor.",
								"Admin/Dev dapat menghitung ulang hasil ujian peserta ini secara manual setelah penyebab kendala ditemukan."
						});
			} finally {
				HibernateUtil.closeSession();
			}
		}

		return true;
	}

	/**
	 * Delegator ke {@link #doProcessUjian(int,boolean)} dengan {@code refresh=false}.
	 * Digunakan untuk navigasi biasa (klik soal, tombol Lanjut/Kembali).
	 *
	 * @param a indeks soal yang dituju (0-based dalam {@link #ujianPunyaSoals}); di-clamp ke [0, size-1]
	 */
	private void doProcessUjian(int a) {
		doProcessUjian(a, false);
	}

	/**
	 * Merender tampilan soal pada indeks tertentu ke dalam {@link #gridSoal} dan memperbarui
	 * status tombol navigasi (Kembali/Lanjut) serta panel nomor soal.
	 *
	 * <p><b>Tujuan:</b> Ini adalah "jantung" navigasi CBT. Setiap kali peserta berpindah soal,
	 * method ini dipanggil untuk menghapus konten soal lama dan merender soal baru beserta
	 * semua opsi jawabannya, sambil menjaga konsistensi status UI (tombol aktif/nonaktif,
	 * progress bar, panel nomor soal).</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Meng-clamp indeks {@code a} ke rentang [0, ujianPunyaSoals.size()-1].</li>
	 *   <li>Bila mode "tiap soal punya waktu sendiri" ({@code tiapSoal=true}), mereset timer
	 *       countdown ke lama soal.</li>
	 *   <li>Memperbarui state tombol navigasi: Kembali nonaktif bila di soal pertama, Lanjut
	 *       nonaktif bila di soal terakhir (atau bila {index + jumlahSoalPerHalaman >= total}).</li>
	 *   <li>Menyimpan indeks saat ini ke cache peserta ({@code hasilUjianMahasiswa.put(index, "index")})
	 *       agar bila session habis, peserta kembali ke soal yang sama.</li>
	 *   <li>Menghapus semua baris lama di {@link #gridSoal} dan membuat Rows baru.</li>
	 *   <li>Untuk setiap soal dalam window [index, index+jumlahSoalPerHalaman): merender soal
	 *       lengkap dengan: progress bar (% selesai), konten soal (HTML/teks/gambar), opsi jawaban
	 *       (Radio/Checkbox berdasarkan jenis soal), dan tombol Simpan Jawaban.</li>
	 *   <li>Menampilkan jawaban yang sudah dipilih sebelumnya (dari {@link #hasilUjianMahasiswaDetailsa})
	 *       dalam keadaan tercentang.</li>
	 *   <li>Bila mode anti-curang aktif, menyisipkan komponen monitoring pelanggaran.</li>
	 * </ol>
	 *
	 * <p><b>Thread-safety:</b> Dipanggil dari ZK event thread saja. Semua akses Hibernate melalui
	 * {@code GeneralValueObject.ambilData} (cache in-memory) tanpa membuka session baru.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Method ini adalah yang paling sering dimodifikasi ketika ada
	 * perubahan tampilan soal. Jika menambah tipe soal baru, tambahkan cabang rendering di sini.
	 * Method ini panjang ~300 baris — pertimbangkan ekstrak ke sub-method bila semakin panjang.</p>
	 *
	 * @param a       indeks soal target (0-based); akan di-clamp ke rentang valid
	 * @param refresh true = render ulang soal SAAT INI (bukan pindah ke soal baru);
	 *                false = navigasi normal ke soal pada indeks {@code a}
	 */
	@SuppressWarnings({})
	private void doProcessUjian(int a, boolean refresh) {
		/* Daftar soal dapat kosong ketika relasi soal dihapus dari proses lain atau cache
		 * belum selesai dimuat. Versi lama menghitung index = size - 1 (-1), lalu tetap
		 * membaca list dan mengubah komponen navigasi. Hentikan render secara aman. */
		if (ujianPunyaSoals == null || ujianPunyaSoals.isEmpty()) {
			ProsesUjianHelper.this.index = 0;
			if (back != null) back.setDisabled(true);
			if (next != null) next.setDisabled(true);
			if (gridSoal != null) Common.clear(gridSoal);
			return;
		}
		final int index;
		if (a < 0) {
			index = 0;
		} else if (a >= ujianPunyaSoals.size()) {
			index = ujianPunyaSoals.size() - 1;
		} else {
			index = a;
		}

		if (waktuTimer != null && Boolean.TRUE.equals(pertemuanPunyaUjian.getTiapSoal())
				&& pertemuanPunyaUjian.getLama() != null
				&& waktuTimer.getCurrentTime() != null) {
			waktuTimer.getCurrentTime().setTime(pertemuanPunyaUjian.getLama());
		}
		ProsesUjianHelper.this.index = index;
		if (ProsesUjianHelper.this.index <= 0) {
			back.setDisabled(true);
		} else {
			back.setDisabled(false);
		}

		if ((ProsesUjianHelper.this.index + jumlahSoalPerHalaman) >= ujianPunyaSoals.size()) {
//			save.setVisible(true);
			next.setDisabled(true);
		} else {
//			save.setVisible(false);
			next.setDisabled(false);
		}

		if (hasilUjianMahasiswa != null) {
			cancel.setDisabled(true);
			hasilUjianMahasiswa.put(ProsesUjianHelper.this.index + "", "index");
		} else {
			cancel.setDisabled(false);
		}

		Ujian ujian = pertemuanPunyaUjian.getUjian();

		Common.clear(gridSoal);

		Rows myrows = new Rows();
		myrows.setParent(gridSoal);

		boolean mobile = Common.isMobile();
		final int total = ujianPunyaSoals.size();
		for (int i = ProsesUjianHelper.this.index;
				i < ProsesUjianHelper.this.index + jumlahSoalPerHalaman && i < ujianPunyaSoals.size(); i++) {

			Long aujianPunyaSoalid = null;
			try {
				/* Gunakan indeks loop, bukan indeks halaman pertama. Pemakaian get(index)
				 * membuat soal pertama dirender berulang untuk seluruh slot halaman. */
				aujianPunyaSoalid = ujianPunyaSoals.get(i);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3887");
			}
			if (aujianPunyaSoalid == null) {
				continue;
			}
			UjianPunyaSoal aujianPunyaSoal = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class,
					aujianPunyaSoalid.toString());
			if (aujianPunyaSoal == null) {
				continue;
			}
			final UjianPunyaSoal ujianPunyaSoal = aujianPunyaSoal;
			currentBankSoal = ujianPunyaSoal.getBankSoal();
			if (currentBankSoal == null) {
				continue;
			}

			MyFormRow myrowa = new MyFormRow();
			myrowa.setParent(myrows);
			Box hbxInfo = mobile ? new Vbox() : new Hbox();
			hbxInfo.setParent(myrowa);

//			new ais.ui.util.MyHtml("<b style='color:blue;'>" + nomor + ":</b>").setParent(hbxInfo);

			Progressmeter pm;
			MyLabelAgakKecilBold it;
			try {
				if (hasilUjianMahasiswa != null) {
					Set<Long> ss = hasilUjianMahasiswa.ambilBankSoalIdTerjawab(hasilUjianMahasiswaDetailsa);
					int terjawab = ss.size();
					ss = null;
					double persen = (terjawab * 100.0) / total;
					if (persen > 99.99) {
						persen = 100.0;
					}
					hbxInfo.appendChild(it = new MyLabelAgakKecilBold(
							"Tuntas " + terjawab + "/" + total + " (" + Common.numberFormat.get().format(persen) + "%)"));
					pm = new Progressmeter((int) persen);
					pm.setHeight("5px");
					pm.setWidth("300px");
					hbxInfo.appendChild(pm);
				} else {
					pm = null;
					it = null;
				}
			} catch (Exception e) {
				pm = null;
				it = null;
				Common.tampilErrorJikaAdmin(e);
			}

			final Progressmeter progressmeter = pm;
			final MyLabelAgakKecilBold infoTuntas = it;

			String nomor = "";
			try {
				long nmr = (long) (ujianPunyaSoals.indexOf(ujianPunyaSoal.getId()) + 1);
				nomor = "Soal nomor " + nmr + " dari total " + total + "";
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:3941");
				// TODO: handle exception
			}

			myrowa = new MyFormRow();
			myrowa.setParent(myrows);
			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.appendChild(new Caption(nomor));
			groupboxStyled.setParent(myrowa);

			MyFormRow myrowalampiran = new MyFormRow();
			myrowalampiran.setParent(myrows);

			MyFormRow rowlampiran1Soal = new MyFormRow();
			rowlampiran1Soal.setParent(myrows);

			MyFormRow rowlampiran2Soal = new MyFormRow();
			rowlampiran2Soal.setParent(myrows);

			MyFormRow rowlampiran3Soal = new MyFormRow();
			rowlampiran3Soal.setParent(myrows);

			MyFormRow rowlampiran4Soal = new MyFormRow();
			rowlampiran4Soal.setParent(myrows);

			MyFormRow rowlampiran5Soal = new MyFormRow();
			rowlampiran5Soal.setParent(myrows);

			BankSoalAction.tampilkanLampiran(null, currentBankSoal, myrowalampiran, false, rowlampiran1Soal,
					rowlampiran2Soal, rowlampiran3Soal, rowlampiran4Soal, rowlampiran5Soal);

			Set<Long> detailsTemp = hasilUjianMahasiswaDetailsa == null ? new HashSet<Long>()
					: hasilUjianMahasiswaDetailsa.get(currentBankSoal.getId());
			if (detailsTemp == null) {
				detailsTemp = new HashSet<Long>();
			}
			final Set<Long> details = detailsTemp;
			final Long myHasilUjianMahasiswaDetailid = details == null || details.isEmpty() ? null
					: details.iterator().next();

			if (ujian.getJenisKoreksi().equals(PenjelasanBankSoal.KOREKSI_OTOMATIS)
					&& (BankSoal.JAWABAN_SINGKAT.equals(currentBankSoal.getJenisPilihanGanda())
							|| BankSoal.RUMPANG.equals(currentBankSoal.getJenisPilihanGanda()))) {

				new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
						+ currentBankSoal.getSoal() + "</div>").setParent(groupboxStyled);

				MyFormRow rowjawaban1 = new MyFormRow();
				rowjawaban1.setParent(myrows);

				MyGroupboxStyled vboxSoalUjian = new MyGroupboxStyled();
				vboxSoalUjian.appendChild(new Caption("Isilah bagian yang kosong pada kotak teks di bawah ini:"));
				vboxSoalUjian.setParent(rowjawaban1);

				HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = null;
				if (myHasilUjianMahasiswaDetailid != null) {
					myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
							.ambilData(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetailid.toString());
//					System.out.println("myHasilUjianMahasiswaDetail -> " + myHasilUjianMahasiswaDetail);

					if (currentBankSoal.getJumlahJawabanDibatasi()) {
						int maks = currentBankSoal.getMaksimalJumlahJawaban();
						if (details != null && details.size() < maks) {
							maks = details.size();
						}
						vboxSoalUjian.appendChild(new MyLabelBoldMerah(
								"Anda harus memilih setidaknya " + currentBankSoal.getMinimalJumlahJawaban()
										+ " pilihan jawaban, dan tidak lebih dari " + maks + " pilihan jawaban."));
					}

					if (myHasilUjianMahasiswaDetail == null) {
						myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
						myHasilUjianMahasiswaDetail.setJawaban(new JSONObject().toString());

						myHasilUjianMahasiswaDetail.setBankSoal(currentBankSoal);
						myHasilUjianMahasiswaDetail.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
						myHasilUjianMahasiswaDetail.setHasilUjianMahasiswa(hasilUjianMahasiswa);
						myHasilUjianMahasiswaDetail.setUjianPunyaSoal(ujianPunyaSoal);

						Session session = HibernateUtil.currentSession();
						session.save(myHasilUjianMahasiswaDetail);
						session.flush();

						Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
						hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
						hasilUjianMahasiswaDetailsa.put(myHasilUjianMahasiswaDetail.getBankSoal().getId(),
								hasilUjianMahasiswaDetails);
					}
				}

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setWidth("100%");
				vboxSoalUjian.appendChild(grid);

				Columns columns = new Columns();
				columns.setParent(grid);

				MyColumnConfig column = new MyColumnConfig();
				column.setParent(columns);
				column.setWidth(
						BankSoal.JAWABAN_SINGKAT.equals(currentBankSoal.getJenisPilihanGanda()) ? "0px" : "40px");

				column = new MyColumnConfig();
				column.setParent(columns);

				final Rows rows = new Rows();
				rows.setParent(grid);

				JSONObject tempJawab;
				try {
					tempJawab = new JSONObject(myHasilUjianMahasiswaDetail.getJawaban());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:4054");
					tempJawab = new JSONObject();
				}

				final JSONObject jwb = tempJawab;

				System.out.println("jwb -> " + jwb);

				final EventListener pindahListener = new EventListener() {

					/**
					 * Menyimpan jawaban ISIAN SINGKAT / RUMPANG peserta ke basis data.
					 *
					 * <p>Dipakai oleh soal berjenis {@code JAWABAN_SINGKAT} dan {@code RUMPANG} pada ujian
					 * berkoreksi otomatis. Seluruh isian pada satu soal disimpan sebagai SATU objek JSON
					 * ({@code jwb}) di kolom {@code jawaban} milik {@code HasilUjianMahasiswaDetail}: kunci berupa
					 * huruf opsi berisi teks yang diketik peserta, dan kunci berakhiran {@code _hasil} berisi
					 * {@code "true"}/{@code "false"} hasil pembandingan dengan kunci jawaban.</p>
					 *
					 * <p><b>Yang dikerjakan:</b> memuat kembali detail jawaban dari cache, mengisi ulang seluruh
					 * relasinya ({@code bankSoal}, {@code hasilUjianMahasiswa}, {@code ujianPunyaSoal}), mencatat
					 * {@code waktuJawab}, lalu menyimpan lewat {@code Common.refreshUpdate} dan {@code flush}.
					 * Setelah tersimpan, peta {@link ProsesUjianHelper#hasilUjianMahasiswaDetailsa} disegarkan dan
					 * — bila peserta sudah berada di soal terakhir — indikator "Tuntas n/N (p%)" beserta progress
					 * bar ikut diperbarui.</p>
					 *
					 * <p>Bila detail jawaban tidak ditemukan (data ujian tidak konsisten), peserta diberi pesan
					 * agar menekan tombol Refresh.</p>
					 *
					 * @param arg0 event ZK pemicu; boleh null — listener ini juga dipanggil sekali saat render
					 *              untuk menuliskan nilai awal isian
					 * @throws Exception bila penyimpanan ke basis data gagal
					 */
					@Override
					public void onEvent(Event arg0) throws Exception {

						Long myHasilUjianMahasiswaDetailid = details.isEmpty() ? null : details.iterator().next();
						if (myHasilUjianMahasiswaDetailid != null) {
							HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class,
											myHasilUjianMahasiswaDetailid.toString());

							if (myHasilUjianMahasiswaDetail != null) {

								myHasilUjianMahasiswaDetail.setBankSoal(currentBankSoal);
								myHasilUjianMahasiswaDetail.setJawaban(jwb.toString());
								myHasilUjianMahasiswaDetail.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
								myHasilUjianMahasiswaDetail.setHasilUjianMahasiswa(hasilUjianMahasiswa);
								myHasilUjianMahasiswaDetail.setUjianPunyaSoal(ujianPunyaSoal);

								Session session = HibernateUtil.currentSession();
								Common.refreshUpdate(session, myHasilUjianMahasiswaDetail);
								session.flush();

								Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
								hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
								hasilUjianMahasiswaDetailsa.put(myHasilUjianMahasiswaDetail.getBankSoal().getId(),
										hasilUjianMahasiswaDetails);

								if (next.isDisabled() && hasilUjianMahasiswa != null && infoTuntas != null
										&& progressmeter != null) {
									Set<Long> ss = hasilUjianMahasiswa
											.ambilBankSoalIdTerjawab(hasilUjianMahasiswaDetailsa);
									int terjawab = ss.size();
									ss = null;
									double persen = (terjawab * 100.0) / total;
									infoTuntas.setValue("Tuntas " + terjawab + "/" + total + " ("
											+ Common.numberFormat.get().format(persen) + "%)");
									progressmeter.setValue((int) persen);
								}

							} else if (hasilUjianMahasiswa != null) {
								MyMessageboxConfig.show(
				"Mohon maaf, terjadi kesalahan pada data ujian. Langkah yang dapat dilakukan: (1) klik tombol Refresh di bagian bawah; (2) apabila kesalahan masih berlanjut, hubungi dosen atau Admin untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
							}
						}
					}
				};

				final List<Long> bankSoalDetails = currentBankSoal.ambilBankSoalDetail(refresh);
				for (Long bankSoalDetailid : bankSoalDetails) {

					final BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
							.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
					if (bankSoalDetail != null && !bankSoalDetail.getJawaban().trim().isEmpty()
							&& Common.isNumber(bankSoalDetail.getHuruf())) {

						try {

							String val = jwb.isNull(bankSoalDetail.getHuruf()) ? ""
									: jwb.get(bankSoalDetail.getHuruf()).toString();
							System.out.println("val -> " + val);
							final Textbox jawaban = new Textbox(val);
							jawaban.setRows(1);
							jawaban.setWidth("95%");

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);
							Label lb;
							row.appendChild(lb = new Label(bankSoalDetail.getHuruf()));
							row.appendChild(jawaban);
							lb.setStyle(
									"padding-right: 8px;padding-left: 8px;text-align: center;font-weight: bolder;font-size:16px;width: 24px !important;height: 24px !important;border-radius: 50px;border: 1px solid black;");

							if (!hanyaLihat) {
								jawaban.addEventListener("onChange", new EventListener() {

									/**
									 * Menangani perubahan teks pada SATU kotak isian (soal isian singkat / rumpang).
									 *
									 * <p>Menuliskan dua kunci ke objek JSON jawaban: kunci huruf opsi berisi teks yang diketik
									 * (sudah di-{@code trim}), dan kunci berakhiran {@code _hasil} berisi hasil pembandingan
									 * <b>tanpa membedakan huruf besar-kecil</b> terhadap kunci jawaban di {@code BankSoalDetail}.
									 * Sesudah itu {@code pindahListener} dipanggil untuk mempersistensikan seluruh objek JSON.</p>
									 *
									 * <p><b>Catatan penilaian:</b> pembandingan dilakukan di SISI SERVER pada saat pengetikan, dan
									 * hasilnya ikut tersimpan di kolom jawaban. Perubahan kunci jawaban SETELAH ujian berlangsung
									 * karenanya tidak otomatis mengoreksi penanda benar/salah yang sudah tercatat.</p>
									 *
									 * <p>Listener ini hanya dipasang bila bukan mode lihat-saja.</p>
									 *
									 * @param arg0 event {@code onChange} ZK dari kotak isian
									 * @throws Exception bila penyimpanan jawaban gagal
									 */
									@Override
									public void onEvent(Event arg0) throws Exception {
										jwb.put(bankSoalDetail.getHuruf() + "_hasil", jawaban.getValue().trim()
												.equalsIgnoreCase(bankSoalDetail.getJawaban().trim()) + "");
										jwb.put(bankSoalDetail.getHuruf(), jawaban.getValue().trim());
										pindahListener.onEvent(arg0);
									}
								});

								jwb.put(bankSoalDetail.getHuruf() + "_hasil",
										jawaban.getValue().trim().equalsIgnoreCase(bankSoalDetail.getJawaban().trim())
												+ "");
								jwb.put(bankSoalDetail.getHuruf(), jawaban.getValue().trim());
								pindahListener.onEvent(null);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:4155");
							// TODO: handle exception
						}
					}
				}
			} else {

				new ais.ui.util.MyHtml("<div style=\"font-size: 12px;font-family: Poppins,Helvetica,\"sans-serif\";\">"
						+ currentBankSoal.getSoal() + "</div>").setParent(groupboxStyled);

				MyFormRow rowjawaban1 = new MyFormRow();
				rowjawaban1.setParent(myrows);

				MyGroupboxStyled vboxSoalUjian = new MyGroupboxStyled();
				vboxSoalUjian.appendChild(new Caption(currentBankSoal.getSoalMenjodohkan()
						? "Jodohkan jawaban berikut dengan cara drag item berikut hingga sesuai:"
						: currentBankSoal.getSoalMengurutkan()
								? "Urutkan jawaban berikut dengan cara drag item berikut dari atas ke bawah agar benar:"
								: ujian.getJenis().equals(BankSoal.PILIHAN_GANDA) ? "Pilihlah Jawaban Berikut:"
										: "Tulislah jawaban Anda di sini:"));
				vboxSoalUjian.setParent(rowjawaban1);

				final List<Long> bankSoalDetails = currentBankSoal.ambilBankSoalDetail(refresh);

				if (currentBankSoal.getJumlahJawabanDibatasi()) {
					int maks = currentBankSoal.getMaksimalJumlahJawaban();
					if (bankSoalDetails != null && bankSoalDetails.size() < maks) {
						maks = bankSoalDetails.size();
					}
					vboxSoalUjian.appendChild(new MyLabelBoldMerah(
							"Anda harus memilih setidaknya " + currentBankSoal.getMinimalJumlahJawaban()
									+ " pilihan jawaban, dan tidak lebih dari " + maks + " pilihan jawaban."));
				}

				if (ujian.getJenis().equals(BankSoal.ESAY) || currentBankSoal.getJenis().equals(BankSoal.ESAY)
						|| bankSoalDetails.isEmpty()) {

					currentBankSoal.setJenisKoreksi(PenjelasanBankSoal.KOREKSI_MANUAL);
					currentBankSoal.setJenis(BankSoal.ESAY);

					if (myHasilUjianMahasiswaDetailid != null) {
						HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
								.ambilData(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetailid.toString());

						if (myHasilUjianMahasiswaDetail != null) {

							EventListener pindahListener = null;
							if (!hanyaLihat) {

								final Textbox jawaban = new Textbox();
								jawaban.setParent(vboxSoalUjian);
								jawaban.setValue(myHasilUjianMahasiswaDetail == null ? ""
										: myHasilUjianMahasiswaDetail.getJawaban());
								jawaban.setRows(10);
								jawaban.setWidth("90%");
								jawaban.setStyle("border: 1px solid #9fb8bf;border-radius: 10px;");

								pindahListener = new EventListener() {

									/**
									 * Menyimpan jawaban ESAI peserta beserta lampirannya.
									 *
									 * <p><b>Yang dikerjakan:</b></p>
									 * <ol>
									 *   <li>Bila kotak esai kosong, isinya diganti otomatis menjadi "Jawaban terdapat di file
									 *       terlampir" — supaya soal tetap terhitung sebagai TERJAWAB ketika peserta hanya
									 *       mengunggah berkas.</li>
									 *   <li>Mengisi ulang seluruh relasi detail jawaban, mencatat {@code waktuJawab}, lalu
									 *       menyimpan lewat {@code Common.refreshUpdate}.</li>
									 *   <li>Bila event membawa objek {@code LampiranLain} pada {@code getData()}, berkas tersebut
									 *       dikaitkan ke id detail jawaban ({@code setRef}) memakai session streaming terpisah,
									 *       dengan {@code rollback} bila gagal.</li>
									 *   <li>Menyegarkan {@link ProsesUjianHelper#hasilUjianMahasiswaDetailsa} dan — bila peserta
									 *       berada di soal terakhir — memperbarui indikator "Tuntas n/N (p%)" dan progress bar.</li>
									 * </ol>
									 *
									 * <p><b>Catatan penilaian:</b> jawaban esai TIDAK dinilai otomatis. Soal esai dipaksa menjadi
									 * {@code KOREKSI_MANUAL} dan skornya diisi kemudian oleh dosen lewat
									 * {@code KoreksiHasilUjian}.</p>
									 *
									 * @param arg0 event ZK {@code onChange} kotak esai, atau event unggah lampiran yang membawa
									 *              {@code LampiranLain} pada {@code getData()}
									 * @throws Exception bila penyimpanan jawaban atau pengaitan lampiran gagal
									 */
									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										Long myHasilUjianMahasiswaDetailid = details.isEmpty() ? null
												: details.iterator().next();
										if (myHasilUjianMahasiswaDetailid != null) {
											HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
													.ambilData(HasilUjianMahasiswaDetail.class,
															myHasilUjianMahasiswaDetailid.toString());

											if (myHasilUjianMahasiswaDetail != null) {
												if (jawaban.getValue().trim().isEmpty()) {
													jawaban.setValue("Jawaban terdapat di file terlampir");
												}

												myHasilUjianMahasiswaDetail.setBankSoal(currentBankSoal);
												myHasilUjianMahasiswaDetail.setJawaban(jawaban.getValue().trim());
												myHasilUjianMahasiswaDetail
														.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
												myHasilUjianMahasiswaDetail.setHasilUjianMahasiswa(hasilUjianMahasiswa);
												myHasilUjianMahasiswaDetail.setUjianPunyaSoal(ujianPunyaSoal);

												Common.refreshUpdate(session, myHasilUjianMahasiswaDetail);

												Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
												hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
												hasilUjianMahasiswaDetailsa.put(
														myHasilUjianMahasiswaDetail.getBankSoal().getId(),
														hasilUjianMahasiswaDetails);

												try {
													Session sessionMy = StreamingHibernateUtil.getInstance()
															.currentSession();
													LampiranLain lampiranLain = (LampiranLain) arg0.getData();

													if (lampiranLain != null && lampiranLain.getId() != null) {
														sessionMy.refresh(lampiranLain);
														lampiranLain.setRef(myHasilUjianMahasiswaDetail.getId());

														sessionMy.getTransaction().begin();
														sessionMy.update(lampiranLain);
														sessionMy.getTransaction().commit();
													}

													StreamingHibernateUtil.getInstance().closeSession();
												} catch (Exception e) {
													StreamingHibernateUtil.getInstance().rollbackTransaction();
													Common.tampilErrorJikaAdmin(e);
												}

												if (next.isDisabled() && hasilUjianMahasiswa != null
														&& infoTuntas != null && progressmeter != null) {
													Set<Long> ss = hasilUjianMahasiswa
															.ambilBankSoalIdTerjawab(hasilUjianMahasiswaDetailsa);
													int terjawab = ss.size();
													ss = null;
													double persen = (terjawab * 100.0) / total;
													infoTuntas.setValue("Tuntas " + terjawab + "/" + total + " ("
															+ Common.numberFormat.get().format(persen) + "%)");
													progressmeter.setValue((int) persen);
												}

											} else if (hasilUjianMahasiswa != null) {
												MyMessageboxConfig.show(
				"Mohon maaf, terjadi kesalahan pada data ujian. Langkah yang dapat dilakukan: (1) klik tombol Refresh di bagian bawah; (2) apabila kesalahan masih berlanjut, hubungi dosen atau Admin untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
											}
										}
									}
								};
								jawaban.addEventListener("onChange", pindahListener);
								BankSoalAction.tampilkanLampiran(
										myHasilUjianMahasiswaDetail == null ? new HasilUjianMahasiswaDetail()
												: myHasilUjianMahasiswaDetail,
										vboxSoalUjian, !hanyaLihat, currentBankSoal.getJumlahLampiran(),
										pindahListener);
							} else {
								new ais.ui.util.MyHtml(myHasilUjianMahasiswaDetail == null ? ""
										: (myHasilUjianMahasiswaDetail.getUjianPunyaSoal().getUjian().getJenis()
												.equals(BankSoal.ESAY)
														? "<hr><h3>" + Common.getBahasaConfig("Jawaban Anda") + "</h3>"
																+ myHasilUjianMahasiswaDetail.getJawaban()
														: ""))
										.setParent(vboxSoalUjian);

								BankSoalAction.tampilkanLampiran(
										myHasilUjianMahasiswaDetail == null ? new HasilUjianMahasiswaDetail()
												: myHasilUjianMahasiswaDetail,
										vboxSoalUjian, !hanyaLihat, currentBankSoal.getJumlahLampiran(),
										pindahListener);

								if (pertemuanPunyaUjian.getLihatJawabanSetelahUjian()) {
									BankSoalDetail bankSoalDetail = currentBankSoal
											.ambilSatuBankSoalDetailEssay(refresh);

									BankSoalAction.tampilkanLampiran(
											myHasilUjianMahasiswaDetail == null ? new HasilUjianMahasiswaDetail()
													: myHasilUjianMahasiswaDetail,
											vboxSoalUjian, false, currentBankSoal.getJumlahLampiran(), pindahListener);

									if (bankSoalDetail != null && !bankSoalDetail.getEssay().isEmpty()) {
										new ais.ui.util.MyHtml("<hr><h3>" + Common.getBahasaConfig("Jawaban Benar")
												+ "</h3>" + bankSoalDetail.getEssay()).setParent(vboxSoalUjian);
									}
								}

								if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
									new ais.ui.util.MyHtml("<hr><h3>" + Common.getBahasaConfig("Skor / Nilai") + "</h3>"
											+ Common.numberFormat.get().format(myHasilUjianMahasiswaDetail.getNilai()))
											.setParent(vboxSoalUjian);
								}
							}

							if (myHasilUjianMahasiswaDetail != null) {
								if (hanyaLihat) {
									if (myHasilUjianMahasiswaDetail != null
											&& !myHasilUjianMahasiswaDetail.getKoreksi().trim().isEmpty()) {
										new ais.ui.util.MyHtml(
												"<hr><h3>" + Common.getBahasaConfig("Hasil Koreksi") + "</h3>"
														+ (myHasilUjianMahasiswaDetail == null ? ""
																: myHasilUjianMahasiswaDetail.getKoreksi()))
												.setParent(vboxSoalUjian);
									}
									Hbox hboxA = new Hbox();
									hboxA.setParent(vboxSoalUjian);
									Hbox hbox = new Hbox();
									hbox.setParent(hboxA);
									LampiranLain.createDownloadUploadFileLain(hbox, myHasilUjianMahasiswaDetail.getId(),
											"Lampiran Koreksi Ujian", "Lampiran Koreksi Ujian", false,
											new EventListener() {

												/**
												 * Callback kosong (no-op) untuk komponen unduh "Lampiran Koreksi Ujian" pada tampilan hasil
												 * soal esai dalam mode lihat-saja.
												 *
												 * <p>{@code LampiranLain.createDownloadUploadFileLain} mewajibkan sebuah callback, padahal di
												 * konteks ini komponen dibuat dengan seluruh flag unggah/hapus bernilai salah sehingga tidak
												 * akan pernah ada perubahan yang perlu ditindaklanjuti. Karena itu badan method sengaja
												 * dikosongkan.</p>
												 *
												 * @param arg0 event ZK (tidak dipakai)
												 * @throws Exception tidak pernah dilempar
												 */
												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, false);

									if (myHasilUjianMahasiswaDetail.getNilai() > 0.1) {
										new ais.ui.util.MyHtml(
												"<hr><h3>" + Common.getBahasaConfig("Skor yang diperoleh") + "</h3>"
														+ (myHasilUjianMahasiswaDetail == null ? ""
																: Common.numberFormat.get().format(
																		myHasilUjianMahasiswaDetail.getNilai())))
												.setParent(vboxSoalUjian);
									}

								}

							}

						}
					} else {
						Textbox jawaban = new Textbox();
						jawaban.setParent(vboxSoalUjian);
						jawaban.setRows(10);
						jawaban.setWidth("95%");
						jawaban.setStyle("border: 1px solid #9fb8bf;border-radius: 10px;");
					}
				}

				else if (ujian.getJenisKoreksi().equals(PenjelasanBankSoal.KOREKSI_OTOMATIS)
						&& ujian.getJenis().equals(BankSoal.PILIHAN_GANDA)) {

					if (currentBankSoal.getSoalMenjodohkan()) {

						String jawabanTersimpan = "";
						if (myHasilUjianMahasiswaDetailid != null) {
							HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class,
											myHasilUjianMahasiswaDetailid.toString());

							if (myHasilUjianMahasiswaDetail != null) {
								jawabanTersimpan = myHasilUjianMahasiswaDetail.getJawaban();
							}
						}

						try {

							Map<Integer, String> jawabanSebelumnya = new HashMap<Integer, String>();
							String[] aa = jawabanTersimpan.split(",");
							for (String ss : aa) {
								try {
									String[] aaa = StringUtils.split(ss, ":");
									if (aaa.length == 3) {
										jawabanSebelumnya.put(Integer.parseInt(aaa[0]), aaa[2]);
									}
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:4401");
									// TODO: handle exception
								}
							}
							System.out.println("jawabanTersimpan -> " + jawabanTersimpan);
							System.out.println("jawabanSebelumnya -> " + jawabanSebelumnya);

							JSONArray array = new JSONArray(currentBankSoal.getOpsiSoal());

							Set<String[]> stringsdata = new HashSet<String[]>();
							Map<Integer, String[]> stringsdataTerjawab = new HashMap<Integer, String[]>();

							for (int k = 0; k < array.length(); k++) {
								try {
									JSONObject jsonObject = array.getJSONObject(k);

									if (jsonObject.isNull("key")) {
										continue;
									}

									String nama = "";

									if (!jsonObject.isNull("nama")) {
										nama = jsonObject.get("nama") + "";
									}

									String nomorData = "";

									if (!jsonObject.isNull("nomor")) {
										nomorData = jsonObject.get("nomor") + "";
									}

									if (!jawabanSebelumnya.values().contains(nomorData)) {
										stringsdata.add(new String[] { nama, nomorData });
									} else {

										Integer indexJawab = 0;
										for (Integer key : jawabanSebelumnya.keySet()) {
											String val = jawabanSebelumnya.get(key);
											if (val.equalsIgnoreCase(nomorData)) {
												indexJawab = key;
												break;
											}
										}

										stringsdataTerjawab.put(indexJawab, new String[] { nama, nomorData });
									}

								} catch (Exception e) {
									e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:4450");
								}
							}

							MyGroupboxStyled vboxSoalPasangan = new MyGroupboxStyled();
							vboxSoalPasangan.appendChild(new Caption("Opsi Pasangan"));
							vboxSoalPasangan.setParent(vboxSoalUjian);

							final Listbox listboxPasangan = new Listbox();

							final Listbox listboxJawaban = new Listbox();
							final Listbox listboxPilihan = new Listbox();

							Hbox hbox = new Hbox();
							hbox.setWidth("95%");

							MyFormRow rowjawaban2 = new MyFormRow();
							rowjawaban2.setParent(myrows);

							Grid gridPilihan = new Grid();
							gridPilihan.setParent(rowjawaban2);

							Columns columns = new Columns();
							columns.setParent(gridPilihan);

							MyColumnConfig column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("50%");

							column = new MyColumnConfig();
							column.setParent(columns);
							column.setWidth("50%");

							Rows rows = new Rows();
							rows.setParent(gridPilihan);

							MyFormRow row = new MyFormRow();
							row.setValign("top");
							row.setParent(rows);

							Groupbox groupboxPilihan1 = new Groupbox();
							groupboxPilihan1.appendChild(new Caption("Pertanyaan"));
							groupboxPilihan1.setParent(row);

							groupboxPilihan1.appendChild(listboxJawaban);

							Groupbox groupboxPilihan2 = new Groupbox();
							groupboxPilihan2.appendChild(new Caption("Pasangkan Opsi Diatas"));
							groupboxPilihan2.setParent(row);

							groupboxPilihan2.appendChild(listboxPilihan);

							EventListener eventListener = new EventListener() {

								/**
								 * Mengembalikan referensi ke listener drag-and-drop ini sendiri.
								 *
								 * <p>Diperlukan karena listener harus memasang DIRINYA SENDIRI pada {@code Listitem} kosong
								 * yang dibuat ulang setiap kali sebuah opsi dipindahkan keluar dari kolom "Opsi Pasangan"
								 * ({@code listitem.addEventListener(Events.ON_DROP, getThis())}). Di dalam kelas anonim,
								 * {@code this} pada baris tersebut tidak dapat dirujuk langsung, sehingga dibungkus method
								 * kecil ini.</p>
								 *
								 * @return listener ini sendiri ({@code this})
								 */
								EventListener getThis() {
									return this;
								}

								/**
								 * Menangani drag-and-drop soal MENJODOHKAN sekaligus menilainya seketika.
								 *
								 * <p><b>Dua perilaku drop:</b></p>
								 * <ol>
								 *   <li><b>Menyusun ulang di dalam satu listbox</b> (asal dan tujuan sama): item disisipkan
								 *       sebelum atau sesudah target sesuai arah perpindahan.</li>
								 *   <li><b>Memindahkan antar listbox</b> (dari "Opsi Pasangan" ke kolom jawaban): item
								 *       dipindahkan; bila slot tujuan adalah slot kosong bertanda {@code "temporary"}, slot itu
								 *       dilepas dan satu slot kosong baru dibuat di kolom "Opsi Pasangan" supaya jumlah slot
								 *       tetap sama.</li>
								 * </ol>
								 *
								 * <p><b>Penilaian seketika:</b> sesudah setiap drop, seluruh item di kolom jawaban ditelusuri
								 * berurutan. Satu pasangan dianggap benar bila nomor opsi yang dipasangkan sama dengan posisi
								 * barisnya. Jawaban disimpan sebagai teks berformat
								 * {@code "posisi:benar:nomor,posisi:benar:nomor,..."} dan nilainya dihitung sebagai
								 * {@code (jumlahBenar * 100) / jumlahOpsi}.</p>
								 *
								 * <p><b>PERHATIAN SKALA NILAI (fakta arsitektur, bukan bug):</b> soal menjodohkan menyimpan
								 * {@code nilai} dalam bentuk PERSENTASE (0-100), bukan poin seperti pilihan ganda. Konversi
								 * kembali ke poin dikerjakan di
								 * {@link ProsesUjianHelper#hitungPilihanGanda(HasilUjianMahasiswa,Map)} yang mengalikan
								 * persentase tersebut dengan skor maksimal soal. Tanpa konversi itu, nilai 100 akan terjumlah
								 * sebagai 100 POIN dan menggelembungkan nilai akhir melebihi 100.</p>
								 *
								 * @param arg0 event {@code onDrop} ZK; di-cast menjadi {@code DropEvent}
								 * @throws Exception bila penyimpanan jawaban gagal
								 */
								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {

									DropEvent dropEvent = (DropEvent) arg0;

									Component dragged = dropEvent.getDragged();
									Component self = dropEvent.getTarget();

									System.out.println("dragged -> " + dragged.getClass().getName());
									System.out.println("self -> " + self.getClass().getName());

									if (dragged instanceof Listitem && self instanceof Listitem) {

										if ((dragged.getParent() == listboxPasangan
												&& self.getParent() == listboxPasangan)
												|| (dragged.getParent() == listboxPilihan
														&& self.getParent() == listboxPilihan)) {

											int indexOff1 = ((Listbox) dragged.getParent())
													.getIndexOfItem((Listitem) dragged);
											int indexOff2 = ((Listbox) dragged.getParent())
													.getIndexOfItem((Listitem) self);

											if (indexOff1 > indexOff2) {
												dragged.getParent().insertBefore(dragged, self);
											} else {
												dragged.getParent().insertBefore(self, dragged);
											}
										} else {
											self.getParent().appendChild(dragged);
											self.getParent().insertBefore(dragged, self);

											if (self.getAttribute("temporary") != null) {
												self.detach();

												Listitem listitem = new Listitem();
												listitem.setDroppable("true");
												listitem.addEventListener(Events.ON_DROP, getThis());

												Listcell listcell = new Listcell();
												listcell.setStyle(
														"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
												listcell.setParent(listitem);

												listboxPasangan.appendChild(listitem);

											}
										}
									}

									List<Listitem> listitems = listboxPilihan.getChildren();
									int banyak = bankSoalDetails.size();

									int index = 1;
									int benar = 0;
									String jawaban = "";
									for (Listitem listitem : listitems) {

										if (listitem.getAttribute("dataStrings") != null) {
											String[] dataStrings = (String[]) listitem.getAttribute("dataStrings");
											boolean b = dataStrings != null && dataStrings[1].equals(index + "");
											if (b) {
												benar++;
											}

											String no = dataStrings[1];

											String d = index + ":" + b + ":" + no;
											jawaban += jawaban.isEmpty() ? d : "," + d;

											String nama = dataStrings[0];
											Label label = (Label) listitem.getAttribute("label");
											label.setValue(index + ". " + nama);
										} else if (banyak >= index) {
											boolean b = false;
											String d = index + ":" + b + ":0";
											jawaban += jawaban.isEmpty() ? d : "," + d;
										}

										index++;
									}

									Double nilia = (benar * 100.0) / banyak;

									System.out.println("nilia -> " + nilia + " jawaban " + jawaban);

									if (myHasilUjianMahasiswaDetailid != null) {
										HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
												.ambilData(HasilUjianMahasiswaDetail.class,
														myHasilUjianMahasiswaDetailid.toString());
										System.out.println(
												"myHasilUjianMahasiswaDetail -> " + myHasilUjianMahasiswaDetail);

										if (myHasilUjianMahasiswaDetail != null) {
											myHasilUjianMahasiswaDetail.setJawaban(jawaban);
											myHasilUjianMahasiswaDetail.setNilai(nilia);
											Session session = HibernateUtil.currentSession();
											Common.refreshUpdate(session, myHasilUjianMahasiswaDetail);
											session.flush();

											Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
											hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
											hasilUjianMahasiswaDetailsa.put(
													myHasilUjianMahasiswaDetail.getBankSoal().getId(),
													hasilUjianMahasiswaDetails);
										}
									}

								}
							};

							listboxPasangan.setParent(vboxSoalPasangan);
							int indexdata = 1;
							for (Long bankSoalDetailid : bankSoalDetails) {

								BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
										.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
								if (bankSoalDetail != null) {

									Listitem listitem = new Listitem();

									Listcell listcell = new Listcell(bankSoalDetail.getJawaban());
									listcell.setStyle(
											"border: solid 1px;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
									listcell.setParent(listitem);

									listboxJawaban.appendChild(listitem);

									listitem = new Listitem();
									listitem.setDraggable("true");
									listitem.setDroppable("true");
									listitem.addEventListener(Events.ON_DROP, eventListener);

									if (stringsdataTerjawab.keySet().contains(indexdata)) {

										String[] dataStrings = stringsdataTerjawab.get(indexdata);
										listitem.setAttribute("dataStrings", dataStrings);

										Label label = new Label(indexdata + ". " + dataStrings[0]);
										label.setStyle("border: solid 2px;\r\n" + "    padding-top: 1px;\r\n"
												+ "    border-radius: 15px;\r\n" + "    text-align: justify;\r\n"
												+ "    padding-left: 10px;\r\n" + "    padding-bottom: 1px;\r\n"
												+ "    padding-right: 15px;");
										listitem.setAttribute("label", label);

										listcell = new Listcell();
										listcell.setStyle(
												"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
										listcell.setParent(listitem);
										listcell.appendChild(label);

										listboxPilihan.appendChild(listitem);

									} else {

										Label label = new Label(indexdata + ".....");
										label.setStyle("border: solid 2px;\r\n" + "    padding-top: 1px;\r\n"
												+ "    border-radius: 15px;\r\n" + "    text-align: justify;\r\n"
												+ "    padding-left: 10px;\r\n" + "    padding-bottom: 1px;\r\n"
												+ "    padding-right: 15px;");
										listitem.setAttribute("label", label);
										listitem.setAttribute("temporary", true);

										listcell = new Listcell();
										listcell.setStyle(
												"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
										listcell.setParent(listitem);
										listcell.appendChild(label);

										listboxPilihan.appendChild(listitem);
									}
									indexdata++;
								}
							}

							Integer indexData = 1;
							for (String[] dataStrings : stringsdata) {
								Listitem listitem = new Listitem();
								listitem.setAttribute("dataStrings", dataStrings);
								listitem.setDraggable("true");
								listitem.setDroppable("true");
								listitem.addEventListener(Events.ON_DROP, eventListener);

								Label label = new Label(indexData + ". " + dataStrings[0]);
								label.setStyle("border: solid 2px;\r\n" + "    padding-top: 1px;\r\n"
										+ "    border-radius: 15px;\r\n" + "    text-align: justify;\r\n"
										+ "    padding-left: 10px;\r\n" + "    padding-bottom: 1px;\r\n"
										+ "    padding-right: 15px;");
								listitem.setAttribute("label", label);

								Listcell listcell = new Listcell();
								listcell.setStyle(
										"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
								listcell.setParent(listitem);
								listcell.appendChild(label);

								listboxPasangan.appendChild(listitem);

								indexData++;
							}

						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:4711");
						}

					}

					else if (currentBankSoal.getSoalMengurutkan()) {

						String jawabanTersimpan = "";
						if (myHasilUjianMahasiswaDetailid != null) {
							HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class,
											myHasilUjianMahasiswaDetailid.toString());

							if (myHasilUjianMahasiswaDetail != null) {
								jawabanTersimpan = myHasilUjianMahasiswaDetail.getJawaban();
							}
						}

						System.out.println("jawabanTersimpan -> " + jawabanTersimpan);
						TreeMap<Integer, BankSoalDetail> treeMap = new TreeMap<Integer, BankSoalDetail>();
						if (!jawabanTersimpan.isEmpty()) {
							String[] s = jawabanTersimpan.split(",");
							for (String ss : s) {
								String[] sss = ss.split(":");
								Integer urut = Integer.parseInt(sss[0]);
								BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
										.ambilData(BankSoalDetail.class, sss[1]);
								if (bankSoalDetail != null) {
									treeMap.put(urut, bankSoalDetail);
								}
							}
						} else {

							for (Long bankSoalDetailid : bankSoalDetails) {
								BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
										.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
								if (bankSoalDetail != null) {
									treeMap.put(bankSoalDetail.getUrutanDiujikan(), bankSoalDetail);
								}
							}
						}

						Hlayout vboxJawaban = new Hlayout();
						vboxJawaban.setParent(vboxSoalUjian);

						final Listbox listbox = new Listbox();

						EventListener eventListener = new EventListener() {

							/**
							 * Menangani drag-and-drop soal MENGURUTKAN sekaligus menilainya seketika.
							 *
							 * <p>Item yang diseret disisipkan sebelum atau sesudah item target sesuai arah perpindahan.
							 * Sesudah itu seluruh item ditelusuri berurutan: satu item dianggap benar bila
							 * {@code bankSoalDetail.getUrutanBenar()} sama dengan posisi barisnya. Label setiap item
							 * ditulis ulang dengan nomor urut barunya sehingga peserta selalu melihat urutan terkini.</p>
							 *
							 * <p>Jawaban disimpan sebagai teks berformat {@code "urutan:idDetail:benar,..."} dan nilainya
							 * {@code (jumlahBenar * 100) / jumlahItem}.</p>
							 *
							 * <p><b>PERHATIAN SKALA NILAI (fakta arsitektur, bukan bug):</b> sama seperti soal menjodohkan,
							 * {@code nilai} disimpan sebagai PERSENTASE (0-100) dan dikonversi kembali menjadi poin oleh
							 * {@link ProsesUjianHelper#hitungPilihanGanda(HasilUjianMahasiswa,Map)}.</p>
							 *
							 * @param arg0 event {@code onDrop} ZK; di-cast menjadi {@code DropEvent}
							 * @throws Exception bila penyimpanan jawaban gagal
							 */
							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {

								DropEvent dropEvent = (DropEvent) arg0;

								Component dragged = dropEvent.getDragged();
								Component self = dropEvent.getTarget();
								if (dragged instanceof Listitem && self instanceof Listitem) {

									int indexOff1 = listbox.getIndexOfItem((Listitem) dragged);
									int indexOff2 = listbox.getIndexOfItem((Listitem) self);

									if (indexOff1 > indexOff2) {
										dragged.getParent().insertBefore(dragged, self);
									} else {
										dragged.getParent().insertBefore(self, dragged);
									}

									List<Listitem> listitems = listbox.getChildren();
									int banyak = listitems.size();
									int index = 1;
									int benar = 0;
									String jawaban = "";
									for (Listitem listitem : listitems) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) listitem
												.getAttribute("bankSoalDetail");
										boolean b = bankSoalDetail.getUrutanBenar().equals(index);
										if (b) {
											benar++;
										}

										String d = index + ":" + bankSoalDetail.getId() + ":" + b;

										jawaban += jawaban.isEmpty() ? d : "," + d;

										Label label = (Label) listitem.getAttribute("label");
										label.setValue(index + ". " + bankSoalDetail.getJawaban());

										index++;
									}

									Double nilia = (benar * 100.0) / banyak;

									System.out.println("indexOff1 -> " + indexOff1 + " indexOff2 -> " + indexOff2
											+ " nilia -> " + nilia + " jawaban " + jawaban);

									if (myHasilUjianMahasiswaDetailid != null) {
										HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
												.ambilData(HasilUjianMahasiswaDetail.class,
														myHasilUjianMahasiswaDetailid.toString());
										System.out.println(
												"myHasilUjianMahasiswaDetail -> " + myHasilUjianMahasiswaDetail);

										if (myHasilUjianMahasiswaDetail != null) {
											myHasilUjianMahasiswaDetail.setJawaban(jawaban);
											myHasilUjianMahasiswaDetail.setNilai(nilia);
											Session session = HibernateUtil.currentSession();
											Common.refreshUpdate(session, myHasilUjianMahasiswaDetail);
											session.flush();

											Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
											hasilUjianMahasiswaDetails.add(myHasilUjianMahasiswaDetail.getId());
											hasilUjianMahasiswaDetailsa.put(
													myHasilUjianMahasiswaDetail.getBankSoal().getId(),
													hasilUjianMahasiswaDetails);
										}
									}
								}

							}
						};

						listbox.setParent(vboxJawaban);
						listbox.setDroppable("true");
						listbox.addEventListener(Events.ON_DROP, eventListener);

						Integer indexData = 1;
						for (BankSoalDetail bankSoalDetail : treeMap.values()) {
							Listitem listitem = new Listitem();
							listitem.setAttribute("bankSoalDetail", bankSoalDetail);
							listitem.setDraggable("true");
							listitem.setDroppable("true");
							listitem.addEventListener(Events.ON_DROP, eventListener);

							Label label = new Label(indexData + ". " + bankSoalDetail.getJawaban());
							label.setStyle("border: solid 2px;\r\n" + "    padding-top: 1px;\r\n"
									+ "    border-radius: 15px;\r\n" + "    text-align: justify;\r\n"
									+ "    padding-left: 10px;\r\n" + "    padding-bottom: 1px;\r\n"
									+ "    padding-right: 15px;");

							listitem.setAttribute("label", label);

							Listcell listcell = new Listcell();
							listcell.setStyle(
									"border: none;font-size:14px;padding-top: 10px;padding-bottom: 10px;padding-left: 10px;padding-right: 10px;");
							listcell.setParent(listitem);
							listcell.appendChild(label);

							listbox.appendChild(listitem);

							indexData++;
						}
					}

					else {

						vboxJawaban = new Vbox();

						if (BankSoal.MULTIPLE_COICE.equals(currentBankSoal.getJenisPilihanGanda())
								|| BankSoal.BENAR_SALAH.equals(currentBankSoal.getJenisPilihanGanda())) {
							Radiogroup radiogroup = new Radiogroup();
							radiogroup.setParent(vboxSoalUjian);
							radiogroup.setWidth("100%");
							radiogroup.setHeight("100%");
							vboxJawaban.setParent(radiogroup);
						} else {
							vboxJawaban.setParent(vboxSoalUjian);
						}

						for (Long bankSoalDetailid : bankSoalDetails) {

							final BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
									.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
							if (bankSoalDetail != null) {

								if (hasilUjianMahasiswa != null) {

									EventListener pindahListener = null;

									if (hanyaLihat) {

										if (BankSoal.MULTIPLE_COICE.equals(currentBankSoal.getJenisPilihanGanda())
												|| BankSoal.BENAR_SALAH.equals(currentBankSoal.getJenisPilihanGanda())) {

											if (myHasilUjianMahasiswaDetailid != null) {
												HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
														.ambilData(HasilUjianMahasiswaDetail.class,
																myHasilUjianMahasiswaDetailid.toString());

												String h = (pertemuanPunyaUjian.getUjian()
														.getTampilanHurufDiPilihanJawaban()
																? bankSoalDetail.getHuruf() + ". "
																: "")
														+ bankSoalDetail.getJawaban();

												new Label(h + (myHasilUjianMahasiswaDetail != null
														&& myHasilUjianMahasiswaDetail.getBankSoalDetail() != null
														&& myHasilUjianMahasiswaDetail.getBankSoalDetail().getId()
																.equals(bankSoalDetail.getId())
														&& pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
																? " (pilih / " + (myHasilUjianMahasiswaDetail == null
																		|| !myHasilUjianMahasiswaDetail
																				.getBankSoalDetail().getBetul()
																						? "Salah"
																						: "Benar")
																		+ ")"
																: ""))
														.setParent(vboxJawaban);

												if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
													new ais.ui.util.MyHtml("<hr><h3>"
															+ Common.getBahasaConfig("Skor / Nilai") + "</h3>"
															+ Common.numberFormat.get()
																	.format(myHasilUjianMahasiswaDetail == null ? 0.0
																			: myHasilUjianMahasiswaDetail.getNilai()))
															.setParent(vboxJawaban);
												}

												LampiranLain lampiranLain = LampiranLain.ambil(
														bankSoalDetail.getBankSoal().getId(),
														"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
												if (lampiranLain != null) {
													try {
														vboxJawaban
																.appendChild(new Image(lampiranLain.createLinkUri()));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:4938");
													}
												}
											}

										} else {
											boolean ada = false;
											for (Long aaid : details) {
												HasilUjianMahasiswaDetail aa = (HasilUjianMahasiswaDetail) GeneralValueObject
														.ambilData(HasilUjianMahasiswaDetail.class, aaid.toString());
												if (aa != null && aa.getBankSoalDetail() != null
														&& aa.getBankSoalDetail().getId() != null
														&& aa.getBankSoalDetail().getId().equals(bankSoalDetail.getId())) {

													String h = (pertemuanPunyaUjian.getUjian()
															.getTampilanHurufDiPilihanJawaban()
																	? bankSoalDetail.getHuruf() + ". "
																	: "")
															+ bankSoalDetail.getJawaban();

													new Label(
															h + (pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
																	? " (pilih / "
																			+ (aa == null || !aa.getBankSoalDetail()
																					.getBetul() ? "Salah" : "Benar")
																			+ ")"
																	: ""))
															.setParent(vboxJawaban);

													if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
														new ais.ui.util.MyHtml("<hr><h3>"
																+ Common.getBahasaConfig("Skor / Nilai") + "</h3>"
																+ Common.numberFormat.get()
																		.format(aa == null ? 0.0 : aa.getNilai()))
																.setParent(vboxJawaban);
													}

													LampiranLain lampiranLain = LampiranLain.ambil(
															bankSoalDetail.getBankSoal().getId(),
															"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
													if (lampiranLain != null) {
														try {
															vboxJawaban.appendChild(
																	new Image(lampiranLain.createLinkUri()));
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:4982");
														}
													}

													ada = true;
												}
											}
											if (!ada) {
												String h = (pertemuanPunyaUjian.getUjian()
														.getTampilanHurufDiPilihanJawaban()
																? bankSoalDetail.getHuruf() + ". "
																: "")
														+ bankSoalDetail.getJawaban();

												new Label(h).setParent(vboxJawaban);

												LampiranLain lampiranLain = LampiranLain.ambil(
														bankSoalDetail.getBankSoal().getId(),
														"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
												if (lampiranLain != null) {
													try {
														vboxJawaban
																.appendChild(new Image(lampiranLain.createLinkUri()));
													} catch (Exception e) {
														e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5006");
													}
												}

											}
										}

									} else {

										if (BankSoal.MULTIPLE_COICE.equals(currentBankSoal.getJenisPilihanGanda())
												|| BankSoal.BENAR_SALAH.equals(currentBankSoal.getJenisPilihanGanda())) {

											if (myHasilUjianMahasiswaDetailid != null) {

												HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
														.ambilData(HasilUjianMahasiswaDetail.class,
																myHasilUjianMahasiswaDetailid.toString());
												if (myHasilUjianMahasiswaDetail != null) {
													final Radio jawaban;

													String h = (pertemuanPunyaUjian.getUjian()
															.getTampilanHurufDiPilihanJawaban()
																	? bankSoalDetail.getHuruf() + ". "
																	: "")
															+ bankSoalDetail.getJawaban();

													(jawaban = new Radio(h)).setParent(vboxJawaban);

													LampiranLain lampiranLain = LampiranLain.ambil(
															bankSoalDetail.getBankSoal().getId(),
															"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
													if (lampiranLain != null) {
														try {
															vboxJawaban.appendChild(
																	new Image(lampiranLain.createLinkUri()));
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5043");
														}
													}

													jawaban.setChecked(myHasilUjianMahasiswaDetail != null
															&& myHasilUjianMahasiswaDetail.getBankSoalDetail() != null
															&& myHasilUjianMahasiswaDetail.getBankSoalDetail().getId()
																	.equals(bankSoalDetail.getId()));

													pindahListener = new EventListener() {

														/**
														 * Menyimpan pilihan jawaban peserta pada soal PILIHAN GANDA atau BENAR-SALAH (satu jawaban).
														 *
														 * <p><b>Yang dikerjakan:</b></p>
														 * <ol>
														 *   <li>Menulis {@code bankSoalDetail} opsi yang dipilih ke {@code HasilUjianMahasiswaDetail}
														 *       milik soal ini. Karena hanya satu opsi yang boleh dipilih, baris detail yang SAMA
														 *       ditimpa — bukan ditambah.</li>
														 *   <li>Mencatat {@code waktuJawab} dan menyalin sisa waktu berjalan ke
														 *       {@code hasilUjianMahasiswa.setSisaWaktuPengerjaan(...)}.</li>
														 *   <li>Menyimpan lewat {@code Common.refreshUpdate}, memasukkan hasilnya kembali ke cache
														 *       {@code GeneralValueObject}, lalu menandai jawaban tersimpan pada
														 *       {@code UjianRecomputeUtil} sehingga nilai peserta dihitung ulang secara TERJADWAL,
														 *       bukan pada setiap klik.</li>
														 *   <li>Mengaitkan lampiran ({@code LampiranLain}) bila event membawanya.</li>
														 *   <li><b>Auto-lanjut:</b> pada ujian yang mengizinkan navigasi bebas, memilih jawaban langsung
														 *       memindahkan peserta ke soal berikutnya. Bila sudah berada di soal terakhir, indikator
														 *       "Tuntas n/N (p%)" dan progress bar diperbarui.</li>
														 * </ol>
														 *
														 * @param arg0 event {@code onCheck} ZK dari radio pilihan jawaban; {@code getData()} dapat
														 *              berisi {@code LampiranLain}
														 * @throws Exception bila penyimpanan jawaban gagal
														 */
														@Override
														public void onEvent(Event arg0) throws Exception {
															Long myHasilUjianMahasiswaDetailid = details.isEmpty()
																	? null
																	: details.iterator().next();
															if (myHasilUjianMahasiswaDetailid != null) {
																HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
																		.ambilData(HasilUjianMahasiswaDetail.class,
																				myHasilUjianMahasiswaDetailid
																						.toString());

																if (myHasilUjianMahasiswaDetail != null) {
																	myHasilUjianMahasiswaDetail
																			.setBankSoal(currentBankSoal);
																	myHasilUjianMahasiswaDetail.setHasilUjianMahasiswa(
																			hasilUjianMahasiswa);
																	myHasilUjianMahasiswaDetail
																			.setBankSoalDetail(bankSoalDetail);
																	myHasilUjianMahasiswaDetail.setWaktuJawab(
																			ais.ui.util.WaktuUtil.getDate());
																	myHasilUjianMahasiswaDetail
																			.setUjianPunyaSoal(ujianPunyaSoal);

																	hasilUjianMahasiswa.setSisaWaktuPengerjaan(
																			waktuTimer.getCurrentTime().getTime());

																	Common.refreshUpdate(myHasilUjianMahasiswaDetail);
																		try { GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetail); } catch (Exception eCache) { ais.common.ErrorAuditUtil.record(eCache, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:5081");}
																		if (hasilUjianMahasiswa != null) { UjianRecomputeUtil.tandaiJawabanTersimpan(hasilUjianMahasiswa.getId()); }

																	Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
																	hasilUjianMahasiswaDetails
																			.add(myHasilUjianMahasiswaDetail.getId());
																	hasilUjianMahasiswaDetailsa.put(
																			myHasilUjianMahasiswaDetail.getBankSoal()
																					.getId(),
																			hasilUjianMahasiswaDetails);

																	LampiranLain lampiranLain = (LampiranLain) arg0
																			.getData();

																	if (lampiranLain != null
																			&& lampiranLain.getId() != null) {
																		Session sessionMy = StreamingHibernateUtil
																				.getInstance().currentSession();
																		try {
																			sessionMy.refresh(lampiranLain);
																			lampiranLain
																					.setRef(myHasilUjianMahasiswaDetail
																							.getId());

																			sessionMy.getTransaction().begin();
																			sessionMy.update(lampiranLain);
																			sessionMy.getTransaction().commit();
																		} catch (Exception e) {
																			StreamingHibernateUtil.getInstance()
																					.rollbackTransaction();
																			Common.tampilErrorJikaAdmin(e);
																		}
																		StreamingHibernateUtil.getInstance()
																				.closeSession();
																	}

																	if (!pertemuanPunyaUjian.getTiapSoal()
																			&& !pertemuanPunyaUjian
																					.getTidakDiaktifkanTombolKembali()) {
																		if (!next.isDisabled()) {
																			ProsesUjianHelper.this.index = ProsesUjianHelper.this.index
																					+ jumlahSoalPerHalaman;
																			doProcessUjian(
																					ProsesUjianHelper.this.index);
																		} else if (hasilUjianMahasiswa != null
																				&& infoTuntas != null
																				&& progressmeter != null) {
																			Set<Long> ss = hasilUjianMahasiswa
																					.ambilBankSoalIdTerjawab(
																							hasilUjianMahasiswaDetailsa);
																			int terjawab = ss.size();
																			ss = null;
																			double persen = (terjawab * 100.0) / total;
																			infoTuntas.setValue("Tuntas " + terjawab
																					+ "/" + total + " ("
																					+ Common.numberFormat.get().format(persen)
																					+ "%)");
																			progressmeter.setValue((int) persen);
																		}
																	}
																} else if (hasilUjianMahasiswa != null) {
																	MyMessageboxConfig.show(
				"Mohon maaf, terjadi kesalahan pada data ujian. Langkah yang dapat dilakukan: (1) klik tombol Refresh di bagian bawah; (2) apabila kesalahan masih berlanjut, hubungi dosen atau Admin untuk bantuan lebih lanjut.",
				"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
																}
															}
														}
													};

													jawaban.addEventListener("onCheck", pindahListener);
												}
											}
										} else {

											String h = (pertemuanPunyaUjian.getUjian()
													.getTampilanHurufDiPilihanJawaban()
															? bankSoalDetail.getHuruf() + ". "
															: "")
													+ bankSoalDetail.getJawaban();

											final Checkbox jawaban;
											(jawaban = new Checkbox(h)).setParent(vboxJawaban);
											for (Long aaid : details) {
												HasilUjianMahasiswaDetail aa = (HasilUjianMahasiswaDetail) GeneralValueObject
														.ambilData(HasilUjianMahasiswaDetail.class, aaid.toString());
												if (aa != null && aa.getBankSoalDetail() != null && aa
														.getBankSoalDetail().getId().equals(bankSoalDetail.getId())) {
													jawaban.setChecked(true);
												}
											}

											LampiranLain lampiranLain = LampiranLain.ambil(
													bankSoalDetail.getBankSoal().getId(),
													"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
											if (lampiranLain != null) {
												try {
													vboxJawaban.appendChild(new Image(lampiranLain.createLinkUri()));
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5179");
												}
											}

											pindahListener = new EventListener() {

												/**
												 * Menyimpan atau MENCABUT satu pilihan jawaban pada soal berganda (checkbox / multiple
												 * response).
												 *
												 * <p><b>Alur:</b></p>
												 * <ol>
												 *   <li>{@link ProsesUjianHelper#jumlahDibatasi()} diperiksa lebih dahulu. Bila batas minimal
												 *       atau maksimal jawaban dilanggar, centang dikembalikan ke keadaan tidak tercentang dan
												 *       proses dihentikan.</li>
												 *   <li><b>Dicentang:</b> satu baris {@code HasilUjianMahasiswaDetail} BARU dibuat (atau baris
												 *       lama dipakai ulang) untuk opsi tersebut, disimpan, dimasukkan ke cache, ditandai ke
												 *       {@code UjianRecomputeUtil}, lalu id-nya DITAMBAHKAN ke himpunan di
												 *       {@link ProsesUjianHelper#hasilUjianMahasiswaDetailsa}. Lampiran dikaitkan bila ada.</li>
												 *   <li><b>Dicabut:</b> id baris dihapus dari himpunan dan barisnya DIHAPUS dari basis data
												 *       ({@code Common.refreshDelete} diikuti {@code flush}).</li>
												 * </ol>
												 *
												 * <p><b>Perbedaan penting dengan radio:</b> soal berganda menyimpan SATU BARIS PER OPSI yang
												 * dipilih, sedangkan pilihan ganda menimpa satu baris saja. Itulah sebabnya
												 * {@link ProsesUjianHelper#hitung(HasilUjianMahasiswaDetail,Map)} perlu menghitung jumlah
												 * pilihan benar dan salah dengan menelusuri seluruh baris milik satu {@code BankSoal}.</p>
												 *
												 * <p>Bila peserta sudah berada di soal terakhir, indikator "Tuntas n/N (p%)" dan progress bar
												 * diperbarui. Tidak ada auto-lanjut untuk soal berganda, karena peserta masih boleh menambah
												 * atau mengurangi pilihan.</p>
												 *
												 * @param arg0 event {@code onCheck} ZK dari checkbox pilihan jawaban; {@code getData()} dapat
												 *              berisi {@code LampiranLain}
												 * @throws Exception bila penyimpanan atau penghapusan jawaban gagal
												 */
												@Override
												public void onEvent(Event arg0) throws Exception {

													if (!jumlahDibatasi()) {
														jawaban.setChecked(false);
														return;
													}

													HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = null;
													Set<Long> details = hasilUjianMahasiswaDetailsa
															.get(currentBankSoal.getId());
													for (Long aaid : details) {
														HasilUjianMahasiswaDetail aa = (HasilUjianMahasiswaDetail) GeneralValueObject
																.ambilData(HasilUjianMahasiswaDetail.class,
																		aaid.toString());
														if (aa != null && aa.getBankSoalDetail() != null
																&& aa.getBankSoalDetail().getId()
																		.equals(bankSoalDetail.getId())) {
															myHasilUjianMahasiswaDetail = aa;
														}
													}

													Session session = HibernateUtil.currentSession();

													if (jawaban.isChecked()) {

														if (myHasilUjianMahasiswaDetail == null) {
															myHasilUjianMahasiswaDetail = new HasilUjianMahasiswaDetail();
														}
														myHasilUjianMahasiswaDetail.setBankSoal(currentBankSoal);
														myHasilUjianMahasiswaDetail
																.setHasilUjianMahasiswa(hasilUjianMahasiswa);
														myHasilUjianMahasiswaDetail.setBankSoalDetail(bankSoalDetail);
														myHasilUjianMahasiswaDetail
																.setWaktuJawab(ais.ui.util.WaktuUtil.getDate());
														myHasilUjianMahasiswaDetail.setUjianPunyaSoal(ujianPunyaSoal);

														Common.refreshSaveOrUpdate(session,
																myHasilUjianMahasiswaDetail);

														try { GeneralValueObject.masukkanData(HasilUjianMahasiswaDetail.class, myHasilUjianMahasiswaDetail); } catch (Exception eCache) { ais.common.ErrorAuditUtil.record(eCache, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:5225");}
															if (hasilUjianMahasiswa != null) { UjianRecomputeUtil.tandaiJawabanTersimpan(hasilUjianMahasiswa.getId()); }
															if (hasilUjianMahasiswaDetailsa.containsKey(
																myHasilUjianMahasiswaDetail.getBankSoal().getId())) {
															hasilUjianMahasiswaDetailsa
																	.get(myHasilUjianMahasiswaDetail.getBankSoal()
																			.getId())
																	.add(myHasilUjianMahasiswaDetail.getId());
														} else {
															Set<Long> hasilUjianMahasiswaDetails = new HashSet<Long>();
															hasilUjianMahasiswaDetails
																	.add(myHasilUjianMahasiswaDetail.getId());
															hasilUjianMahasiswaDetailsa.put(
																	myHasilUjianMahasiswaDetail.getBankSoal().getId(),
																	hasilUjianMahasiswaDetails);
														}

														try {
															Session sessionMy = StreamingHibernateUtil.getInstance()
																	.currentSession();
															LampiranLain lampiranLain = (LampiranLain) arg0.getData();

															if (lampiranLain != null && lampiranLain.getId() != null) {
																sessionMy.refresh(lampiranLain);
																lampiranLain
																		.setRef(myHasilUjianMahasiswaDetail.getId());

																sessionMy.getTransaction().begin();
																sessionMy.update(lampiranLain);
																sessionMy.getTransaction().commit();
															}

															StreamingHibernateUtil.getInstance().closeSession();
														} catch (Exception e) {
															StreamingHibernateUtil.getInstance().rollbackTransaction();
															Common.tampilErrorJikaAdmin(e);
														}

													} else if (myHasilUjianMahasiswaDetail != null) {
														Set<Long> d = hasilUjianMahasiswaDetailsa
																.get(currentBankSoal.getId());
														if (d != null) {
															d.remove(myHasilUjianMahasiswaDetail.getId());
															hasilUjianMahasiswaDetailsa.put(currentBankSoal.getId(), d);
														}
														Common.refreshDelete(session, myHasilUjianMahasiswaDetail);
														session.flush();
													}

													if (next.isDisabled() && hasilUjianMahasiswa != null
															&& infoTuntas != null && progressmeter != null) {
														Set<Long> ss = hasilUjianMahasiswa
																.ambilBankSoalIdTerjawab(hasilUjianMahasiswaDetailsa);
														int terjawab = ss.size();
														ss = null;
														double persen = (terjawab * 100.0) / total;
														infoTuntas.setValue("Tuntas " + terjawab + "/" + total + " ("
																+ Common.numberFormat.get().format(persen) + "%)");
														progressmeter.setValue((int) persen);
													}

												}

											};

											jawaban.addEventListener("onCheck", pindahListener);

										}
									}

								} else {
									String h = (pertemuanPunyaUjian.getUjian().getTampilanHurufDiPilihanJawaban()
											? bankSoalDetail.getHuruf() + ". "
											: "") + bankSoalDetail.getJawaban();

									new Label(h).setParent(vboxJawaban);

									LampiranLain lampiranLain = LampiranLain.ambil(bankSoalDetail.getBankSoal().getId(),
											"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
									if (lampiranLain != null) {
										try {
											vboxJawaban.appendChild(new Image(lampiranLain.createLinkUri()));
										} catch (Exception e) {
											e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5308");
										}
									}
								}

							}
						}

						if (hanyaLihat) {
							if (pertemuanPunyaUjian.getLihatJawabanSetelahUjian()
									|| pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
								List<Long> jawabanBenar = currentBankSoal.ambilBankSoalDetail(refresh);
								if (!jawabanBenar.isEmpty()) {

									String ben = "";
									for (Long bankSoalDetailid : jawabanBenar) {
										BankSoalDetail bankSoalDetail = (BankSoalDetail) GeneralValueObject
												.ambilData(BankSoalDetail.class, bankSoalDetailid.toString());
										if (bankSoalDetail != null) {
											String h = (pertemuanPunyaUjian.getUjian()
													.getTampilanHurufDiPilihanJawaban()
															? bankSoalDetail.getHuruf() + ". "
															: "")
													+ bankSoalDetail.getJawaban();
											if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {
												ben += (h + (bankSoalDetail.getBetul() ? " (Benar) " : " (Salah) ") + ""
														+ bankSoalDetail.getJawaban() + "<br>");
											}

											if (pertemuanPunyaUjian.getLihatNilaiSetelahUjian()) {

												ben += "<hr><h3>" + Common.getBahasaConfig("Skor / Nilai") + "</h3>"
														+ Common.numberFormat.get().format(bankSoalDetail == null ? 0.0
																: bankSoalDetail.getSkor());

											}

											LampiranLain lampiranLain = LampiranLain.ambil(
													bankSoalDetail.getBankSoal().getId(),
													"Gambar_Jawaban_" + bankSoalDetail.getHuruf());
											if (lampiranLain != null) {
												try {
													ben = ben + "<br><img src=\"" + lampiranLain.createLinkUri()
															+ "\"></img>";
												} catch (Exception e) {
													e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5353");
												}
											}
										}
									}

									new ais.ui.util.MyHtml(
											"<hr><h3>" + Common.getBahasaConfig("Jawaban Benar") + "</h3>" + ben)
											.setParent(vboxJawaban);
								}
								jawabanBenar = null;
							}

							if (hasilUjianMahasiswa != null) {

								if (myHasilUjianMahasiswaDetailid != null) {

									HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
											.ambilData(HasilUjianMahasiswaDetail.class,
													myHasilUjianMahasiswaDetailid.toString());

									MyLabelBoldAja jawab = new MyLabelBoldAja(myHasilUjianMahasiswaDetail == null ? ""
											: myHasilUjianMahasiswaDetail.getBankSoalDetail() == null ? ""
													: "Jawaban Anda : "
															+ ((pertemuanPunyaUjian.getUjian()
																	.getTampilanHurufDiPilihanJawaban()
																			? myHasilUjianMahasiswaDetail
																					.getBankSoalDetail().getHuruf()
																					+ ". "
																			: ""))
															+ myHasilUjianMahasiswaDetail.getBankSoalDetail()
																	.getJawaban());
									Hbox lampiran = new Hbox();
									// Soal yang TIDAK dijawab (esai kosong / peserta tak memilih) memiliki bankSoalDetail
									// = null. Tanpa penjagaan ini, getBankSoalDetail().getBankSoal() melempar NPE dan
									// MENGGAGALKAN seluruh proses/penampilan hasil ujian sehingga nilai peserta lain ikut
									// tidak terhitung (keluhan: nilai ujian tidak tampil). Lampiran hanya ada bila ada pilihan.
									if (myHasilUjianMahasiswaDetail != null && myHasilUjianMahasiswaDetail.getBankSoalDetail() != null
											&& myHasilUjianMahasiswaDetail.getBankSoalDetail().getBankSoal() != null) {
										BankSoalAction.tampilkanLampiran(myHasilUjianMahasiswaDetail, lampiran, false,
												ujianPunyaSoal.getBankSoal().getJumlahLampiran(), null);

										LampiranLain lampiranLain = LampiranLain.ambil(
												myHasilUjianMahasiswaDetail.getBankSoalDetail().getBankSoal().getId(),
												"Gambar_Jawaban_"
														+ myHasilUjianMahasiswaDetail.getBankSoalDetail().getHuruf());
										if (lampiranLain != null) {
											try {
												lampiran.appendChild(new Image(lampiranLain.createLinkUri()));
											} catch (Exception e) {
												e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5403");
											}
										}
									}

									vboxJawaban.appendChild(new Vbox(new Component[] { jawab, lampiran }));
								}
							}
						}

						if (myHasilUjianMahasiswaDetailid != null) {
							HasilUjianMahasiswaDetail myHasilUjianMahasiswaDetail = (HasilUjianMahasiswaDetail) GeneralValueObject
									.ambilData(HasilUjianMahasiswaDetail.class,
											myHasilUjianMahasiswaDetailid.toString());

							if (myHasilUjianMahasiswaDetail != null) {

								if (hanyaLihat) {
									if (myHasilUjianMahasiswaDetail != null
											&& !myHasilUjianMahasiswaDetail.getKoreksi().trim().isEmpty()) {

										new ais.ui.util.MyHtml(
												"<hr><h3>" + Common.getBahasaConfig("Hasil Koreksi") + "</h3>"
														+ (myHasilUjianMahasiswaDetail == null ? ""
																: myHasilUjianMahasiswaDetail.getKoreksi()))
												.setParent(vboxJawaban);
									}

									Hbox hbox = new Hbox();
									hbox.setParent(vboxJawaban);
									LampiranLain.createDownloadUploadFileLain(hbox, myHasilUjianMahasiswaDetail.getId(),
											"Lampiran Koreksi Ujian", "Lampiran Koreksi Ujian", false,
											new EventListener() {

												/**
												 * Callback kosong (no-op) untuk komponen unduh "Lampiran Koreksi Ujian" pada tampilan hasil
												 * soal pilihan ganda dalam mode lihat-saja.
												 *
												 * <p>Kembaran dari callback kosong pada cabang soal esai: komponen dibuat sebagai unduh-saja
												 * sehingga tidak ada perubahan yang perlu ditindaklanjuti, tetapi
												 * {@code createDownloadUploadFileLain} tetap mewajibkan sebuah listener.</p>
												 *
												 * @param arg0 event ZK (tidak dipakai)
												 * @throws Exception tidak pernah dilempar
												 */
												@Override
												public void onEvent(Event arg0) throws Exception {

												}
											}, null, false, false, false, false);

								}

							}

						}
					}

				}

			}

			if (penjelasan != null)
				Common.clear(penjelasan);
			if (currentBankSoal.getTampilPenjelasanSaatUjian() && currentBankSoal.getPenjelasanBankSoal() != null) {
				if (penjelasan != null) {
					penjelasan.setVisible(true);
					penjelasan.setTitle(currentBankSoal.getPenjelasanBankSoal().getNama());
					Row rowData = Common.tampilanScroll1(penjelasan);
					new ais.ui.util.MyHtml(currentBankSoal.getPenjelasanBankSoal().getKeterangan()).setParent(rowData);
				} else {
					Row rowData = Common.tampilanScroll1(groupboxStyled);
					new ais.ui.util.MyHtml(currentBankSoal.getPenjelasanBankSoal().getKeterangan()).setParent(rowData);
				}

			} else {
				if (penjelasan != null)
					penjelasan.setVisible(false);
			}

			if (ujianPunyaSoals != null && ujianPunyaSoals.size() > 1 && pertemuanPunyaUjian != null
					&& !pertemuanPunyaUjian.getTidakDiaktifkanTombolKembali()) {
				MyFormRow nomorSoal = new MyFormRow();
				nomorSoal.setParent(myrows);
				nomorSoal.appendChild(tampilNomorSoal(ujianPunyaSoal.getId()));
			}
		}

		if (!hanyaLihat) {
			MyFormRow rowFooter = new MyFormRow();
			rowFooter.setParent(myrows);
			rowFooter.appendChild(new ais.ui.util.MyHtml("<hr>" + pertemuanPunyaUjian.getUjian().getTatatertibUjian()));
		}

	}

	/**
	 * Memeriksa apakah jumlah jawaban yang dipilih peserta untuk soal saat ini memenuhi
	 * batas minimal dan tidak melebihi batas maksimal yang dikonfigurasi di {@code BankSoal}.
	 *
	 * <p><b>Tujuan:</b> Untuk soal checkbox berganda (multiple response) dengan pembatasan
	 * jumlah jawaban, peserta tidak boleh memilih terlalu sedikit (di bawah minimum) atau
	 * terlalu banyak (di atas maksimum). Method ini dipanggil sebelum menyimpan jawaban
	 * soal berganda untuk memberikan umpan balik langsung ke peserta.</p>
	 *
	 * <p><b>Cara kerja:</b></p>
	 * <ol>
	 *   <li>Guard: bila {@link #currentBankSoal} null atau soal tidak punya pembatasan
	 *       ({@code getJumlahJawabanDibatasi() = false}), langsung return true.</li>
	 *   <li>Menghitung jumlah checkbox yang tercentang di {@link #vboxJawaban}.</li>
	 *   <li>Bila lebih kecil dari {@code minimalJumlahJawaban}: tampilkan pesan peringatan
	 *       dan return false (simpan dibatalkan).</li>
	 *   <li>Bila lebih besar dari {@code maksimalJumlahJawaban}: tampilkan pesan peringatan
	 *       dan return false.</li>
	 *   <li>Bila dalam batas, return true (simpan boleh dilanjutkan).</li>
	 * </ol>
	 *
	 * <p><b>Pemeliharaan:</b> Dipanggil dari event handler tombol "Simpan Jawaban" sebelum
	 * jawaban disimpan ke database. Jika UX ingin mencegah peserta mencentang lebih dari
	 * maksimum (bukan hanya menolak saat simpan), tambahkan logika di event onChange checkbox.</p>
	 *
	 * @return true bila jumlah jawaban valid atau soal tidak memiliki pembatasan;
	 *         false bila jumlah jawaban di luar batas (dan pesan peringatan sudah ditampilkan)
	 */
	@SuppressWarnings("rawtypes")
	private boolean jumlahDibatasi() {
		try {

			if (currentBankSoal != null && currentBankSoal.getJumlahJawabanDibatasi()) {

				int maks = currentBankSoal.getMaksimalJumlahJawaban();
				int min = currentBankSoal.getMinimalJumlahJawaban();

				int dijawab = 0;
				List components = vboxJawaban.getChildren();
				for (Object oo : components) {
					if (oo != null && oo instanceof Checkbox) {
						Checkbox checkboxData = (Checkbox) oo;
						if (checkboxData.isChecked()) {
							dijawab++;
						}
					}
				}

				System.out.println("ujianPunyaSoal => " + currentBankSoal.getSoal() + " min " + min + " maks " + maks
						+ " dijawab " + dijawab);

				if (dijawab < min) {
					MyMessageboxConfig.show(Common.pesan(
				"Mohon maaf, untuk soal ini jumlah minimal jawaban yang harus dipilih adalah {V1}. Silakan lengkapi pilihan jawaban Anda sesuai ketentuan.",
				min), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								/**
								 * Callback kosong (no-op) untuk pesan peringatan "jumlah MINIMAL jawaban belum terpenuhi".
								 *
								 * <p>Varian {@code MyMessageboxConfig.show} yang dipakai di sini mewajibkan sebuah callback,
								 * sementara peringatan ini murni informatif: penolakan penyimpanan sudah dilakukan lewat nilai
								 * balik {@code false} milik {@link ProsesUjianHelper#jumlahDibatasi()}.</p>
								 *
								 * @param arg0 event ZK dari messagebox (tidak dipakai)
								 * @throws Exception tidak pernah dilempar
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							});

					return false;
				} else if (dijawab > maks) {
					MyMessageboxConfig.show(Common.pesan(
				"Mohon maaf, untuk soal ini jumlah maksimal jawaban yang boleh dipilih adalah {V1}. Silakan kurangi pilihan jawaban Anda sesuai ketentuan.",
				maks), "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								/**
								 * Callback kosong (no-op) untuk pesan peringatan "jumlah MAKSIMAL jawaban terlampaui".
								 *
								 * <p>Kembaran dari callback peringatan batas minimal; penolakan penyimpanan sudah dilakukan
								 * lewat nilai balik {@code false} milik {@link ProsesUjianHelper#jumlahDibatasi()}.</p>
								 *
								 * @param arg0 event ZK dari messagebox (tidak dipakai)
								 * @throws Exception tidak pernah dilempar
								 */
								@Override
								public void onEvent(Event arg0) throws Exception {

								}
							});
					return false;
				}
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/ProsesUjianHelper.java:5566");
		}

		return true;
	}
    /**
     * Membangun komponen HTML berupa bar chart horizontal yang menampilkan distribusi nilai
     * atau statistik ujian tanpa membutuhkan library JavaScript chart eksternal.
     *
     * <p><b>Tujuan:</b> Menyediakan visualisasi data sederhana yang kompatibel dengan
     * ZK 5.5 tanpa mengandalkan ZK Charts (berbayar) atau chart.js. Bar chart ini dibangun
     * sepenuhnya dari HTML/CSS inline menggunakan div dengan lebar proporsional.</p>
     *
     * <p><b>Cara kerja:</b></p>
     * <ol>
     *   <li>Menentukan nilai maksimum dari array {@code values} sebagai acuan lebar 100%.</li>
     *   <li>Untuk setiap label dan value, membuat baris HTML dengan:
     *     <ul>
     *       <li>{@code el-html-chart-label}: teks label (di-escape dari XSS).</li>
     *       <li>{@code el-html-chart-bar}: div berwarna dengan lebar = (value/max)*100%.</li>
     *       <li>{@code el-html-chart-value}: angka formatted (dengan pemisah ribuan ID).</li>
     *     </ul>
     *   </li>
     *   <li>Bar dengan value > 0 tetapi kalkulasi lebar < 3% diberi minimum 3% agar terlihat.</li>
     *   <li>Membungkus semua dalam div {@code el-html-chart} dan mengembalikan sebagai
     *       {@code org.zkoss.zul.Html} component.</li>
     * </ol>
     *
     * <p><b>Keamanan:</b> Semua teks dari parameter di-escape via {@link #escapeHtmlSimple}
     * sebelum dimasukkan ke HTML untuk mencegah XSS.</p>
     *
     * @param title       judul chart yang ditampilkan di atas
     * @param description deskripsi singkat yang ditampilkan di bawah judul
     * @param labels      array teks label untuk setiap bar (harus sama panjang dengan {@code values})
     * @param values      array nilai integer untuk setiap bar; boleh null (chart tidak dirender)
     * @return komponen ZK {@code Html} yang siap ditambahkan ke parent component
     */
    private static Component buildElearningHtmlMetricChart(String title, String description, String[] labels, int[] values) {
        int max = 0;
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] > max) {
                    max = values[i];
                }
            }
        }
        if (max <= 0) {
            max = 1;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='el-html-chart'>");
        sb.append("<div class='el-html-chart-title'>").append(escapeHtmlSimple(title)).append("</div>");
        sb.append("<div class='el-html-chart-desc'>").append(escapeHtmlSimple(description)).append("</div>");
        if (labels != null && values != null) {
            for (int i = 0; i < labels.length && i < values.length; i++) {
                int value = values[i];
                int width = (int) Math.round((value * 100.0) / max);
                if (value > 0 && width < 3) {
                    width = 3;
                }
                sb.append("<div class='el-html-chart-row'>");
                sb.append("<div class='el-html-chart-label'>").append(escapeHtmlSimple(labels[i])).append("</div>");
                sb.append("<div class='el-html-chart-track'><div class='el-html-chart-bar' style='width:").append(width).append("%'></div></div>");
                sb.append("<div class='el-html-chart-value'>").append(Common.numberFormat.get().format(value)).append("</div>");
                sb.append("</div>");
            }
        }
        sb.append("</div>");
        return new Html(sb.toString());
    }

    /**
     * Membangun komponen HTML berupa "pie chart" berbasis CSS (bukan SVG/Canvas) yang
     * menampilkan persentase pencapaian dari satu nilai terhadap total.
     *
     * <p><b>Tujuan:</b> Memberikan visualisasi melingkar untuk data proporsi (misal: persentase
     * soal terjawab, ketercapaian CPMK, dll.) tanpa library eksternal. Implementasi menggunakan
     * CSS custom property {@code --el-percent} yang dirender sebagai lingkaran gradient.</p>
     *
     * <p><b>Cara kerja:</b></p>
     * <ol>
     *   <li>Menghitung persentase: {@code percent = round(value * 100.0 / safeTotal)}, di-clamp ke [0, 100].</li>
     *   <li>Membangun HTML dengan:
     *     <ul>
     *       <li>Div {@code el-pie-css} dengan inline style {@code --el-percent:N%} (dikonsumsi CSS).</li>
     *       <li>Div inner yang menampilkan angka persentase dan label "Terpenuhi".</li>
     *       <li>Legend dua item: "N aktif" dan "M belum".</li>
     *     </ul>
     *   </li>
     *   <li>Rendering lingkaran aktual bergantung pada CSS yang mendefinisikan
     *       {@code el-pie-css} menggunakan {@code conic-gradient}. Pastikan CSS tersebut
     *       didefinisikan di file CSS utama aplikasi.</li>
     * </ol>
     *
     * @param title       judul yang ditampilkan di atas chart
     * @param description deskripsi singkat di bawah judul
     * @param value       nilai yang tercapai / aktif
     * @param total       total maksimum; bila 0, dianggap 1 untuk menghindari pembagian nol
     * @return komponen ZK {@code Html} berisi markup pie chart CSS
     */
    private static Component buildElearningHtmlPie(String title, String description, int value, int total) {
        int safeTotal = total <= 0 ? 1 : total;
        int percent = (int) Math.round((value * 100.0) / safeTotal);
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        StringBuffer sb = new StringBuffer();
        sb.append("<div class='el-html-chart'>");
        sb.append("<div class='el-html-chart-title'>").append(escapeHtmlSimple(title)).append("</div>");
        sb.append("<div class='el-html-chart-desc'>").append(escapeHtmlSimple(description)).append("</div>");
        sb.append("<div class='el-pie-css' style='--el-percent:").append(percent).append("%'>");
        sb.append("<div class='el-pie-css-inner'><div class='el-pie-css-value'>").append(percent).append("%</div><div class='el-pie-css-label'>Terpenuhi</div></div>");
        sb.append("</div>");
        sb.append("<div class='el-chart-legend'><span><i class='el-chart-dot'></i>").append(Common.numberFormat.get().format(value)).append(" aktif</span><span><i class='el-chart-dot el-chart-dot-muted'></i>").append(Common.numberFormat.get().format(Math.max(0, safeTotal - value))).append(" belum</span></div>");
        sb.append("</div>");
        return new Html(sb.toString());
    }

    /**
     * Meng-escape karakter HTML sensitif dalam string untuk mencegah XSS (Cross-Site Scripting).
     * Mengganti {@code &}, {@code <}, {@code >}, {@code "}, dan {@code '} dengan entitas HTML.
     *
     * <p><b>Tujuan:</b> Setiap teks yang dimasukkan ke dalam HTML yang dibangun secara programatik
     * (seperti di {@link #buildElearningHtmlMetricChart} dan {@link #buildElearningHtmlPie})
     * harus di-escape untuk memastikan teks tidak ditafsirkan sebagai markup HTML.</p>
     *
     * <p><b>Cara kerja:</b> Penggantian berurutan menggunakan {@code String.replace()} — bukan
     * regex, sehingga efisien untuk string pendek. Urutan penggantian {@code &} harus pertama
     * untuk menghindari double-escape (&amp; menjadi &amp;amp;).</p>
     *
     * @param value string yang akan di-escape; null menghasilkan string kosong ""
     * @return string yang sudah aman untuk dimasukkan ke konten HTML sebagai teks
     */
    private static String escapeHtmlSimple(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    /**
     * Membangun JavaScript anti-curang CBT berdasarkan konfigurasi sistem.
     * Mengembalikan string kosong jika fitur dinonaktifkan oleh admin.
     *
     * Config keys (semua dapat diubah di KonfigurasiNewAction → Tab Elearning → Anti-Curang):
     *   ujian_anti_curang_aktif                   — master toggle (default: AKTIF)
     *   ujian_anti_curang_batas_pelanggaran        — maks pelanggaran sebelum auto-selesai (default: 3, 0 = tidak ada batas)
     *   ujian_anti_curang_aktifkan_fullscreen      — masuk fullscreen otomatis (default: AKTIF)
     *   ujian_anti_curang_deteksi_pindah_tab       — deteksi tab switching (default: AKTIF)
     *   ujian_anti_curang_deteksi_blur_jendela     — deteksi window blur/Alt+Tab (default: AKTIF)
     *   ujian_anti_curang_cooldown_blur_ms         — cooldown blur dalam ms (default: 5000)
     *   ujian_anti_curang_deteksi_keluar_fullscreen— deteksi keluar fullscreen (default: AKTIF)
     *   ujian_anti_curang_blokir_klik_kanan        — blokir context menu (default: AKTIF)
     *   ujian_anti_curang_blokir_shortcut          — blokir shortcut berbahaya (default: AKTIF)
     *   ujian_anti_curang_peringatan_keluar_halaman— beforeunload warning (default: AKTIF)
     *   ujian_anti_curang_pesan_pindah_tab         — teks peringatan pindah tab
     *   ujian_anti_curang_pesan_blur_jendela       — teks peringatan window blur
     *   ujian_anti_curang_pesan_keluar_fullscreen  — teks peringatan keluar fullscreen
     *   ujian_anti_curang_pesan_keluar_halaman     — teks beforeunload
     */
    /**
     * Memeriksa apakah fitur anti-curang CBT diaktifkan oleh administrator melalui konfigurasi sistem.
     * Digunakan sebagai gerbang sebelum menginject script pengawasan ke browser peserta.
     *
     * <p><b>Tujuan:</b> Memungkinkan admin menonaktifkan seluruh fitur anti-curang dari satu
     * toggle di panel konfigurasi ({@code ujian_anti_curang_aktif}) tanpa harus memodifikasi
     * kode. Berguna untuk ujian dengan pengawas hadir, ujian latihan, atau saat troubleshooting
     * keluhan peserta terkait script anti-curang.</p>
     *
     * <p><b>Cara kerja:</b> Membaca konfigurasi {@code ujian_anti_curang_aktif} via
     * {@code Common.getKonfigurasi}. Default AKTIF jika key tidak ditemukan.
     * Bila exception terjadi (konfigurasi DB tidak tersedia), failsafe ke {@code true}
     * (anti-curang aktif) agar keamanan ujian tidak menurun saat ada masalah DB.</p>
     *
     * @param ppu ujian yang sedang berjalan; pengaturan anti-curang kini PER-UJIAN (kolom {@code ac_aktif}
     *            di {@link PertemuanPunyaUjian}, default AKTIF bila belum diatur).
     * @return true bila fitur anti-curang aktif (default); false bila dinonaktifkan pada ujian ini
     */
    private static boolean isAntiCurangAktif(PertemuanPunyaUjian ppu) {
        try {
            return ppu == null || ppu.getAntiCurangAktif() == null || ppu.getAntiCurangAktif().booleanValue();
        } catch (Exception e) {
            return true; // failsafe: keamanan tetap ON bila terjadi masalah.
        }
    }

    /**
     * Membangun HTML string untuk panel himbauan mode pengawasan yang ditampilkan di atas
     * jendela CBT ketika fitur anti-curang aktif.
     *
     * <p><b>Tujuan:</b> Memberikan peringatan visual yang jelas kepada peserta bahwa ujian ini
     * diawasi secara elektronik, sehingga mereka memahami konsekuensi sebelum melakukan tindakan
     * yang bisa terdeteksi sebagai kecurangan. Himbauan menggunakan bahasa Indonesia yang mudah
     * dipahami oleh semua kalangan.</p>
     *
     * <p><b>Cara kerja:</b> Mengembalikan HTML string hardcoded dengan styling inline (orange/amber
     * theme, ikon polisi &#128737;, border kiri tebal) yang dirender sebagai
     * {@code org.zkoss.zul.Html} di panel North jendela CBT. Teks menjelaskan:
     * mode pengawasan aktif, larangan pindah tab/aplikasi/jendela baru, bahwa pelanggaran
     * terekam otomatis, dan konsekuensi diskualifikasi.</p>
     *
     * <p><b>Pemeliharaan:</b> Bila teks himbauan perlu dikustomisasi per institusi atau bahasa,
     * pertimbangkan memindahkan teks ke konfigurasi database. Saat ini teks bersifat hardcoded
     * Indonesia.</p>
     *
     * @return HTML string siap render untuk panel himbauan mode pengawasan
     */
    private static String buildHimbauanAntiCurangHtml() {
        return "<div style=\"margin-top:9px;background:#fff7ed;border:1px solid #fdba74;border-left:5px solid #ea580c;"
            + "border-radius:12px;padding:10px 13px;color:#7c2d12;font-family:sans-serif;font-size:12.5px;line-height:1.55;"
            + "display:flex;gap:11px;align-items:flex-start;box-shadow:0 4px 14px rgba(0,0,0,.18);\">"
            + "<span style=\"flex:0 0 auto;font-size:20px;line-height:1.1;\">&#128737;</span>"
            + "<div><b style=\"color:#9a3412;\">Mode Pengawasan Ujian Aktif.</b> "
            + "Mohon tetap berada di halaman ujian ini. <b>Jangan</b> berpindah tab atau aplikasi lain, membuka "
            + "jendela baru, atau meninggalkan layar ujian. Setiap pelanggaran terekam otomatis dan dapat menyebabkan "
            + "Anda <b>didiskualifikasi</b> serta ujian <b>dihentikan</b>. Tetap fokus dan kerjakan dengan jujur."
            + "</div></div>";
    }

    /**
     * Menyimpan satu pelanggaran pengawasan ujian ke DB (jumlah + log) untuk hasil ujian
     * tertentu. Dipanggil dari listener event JS anti-curang. Memakai session terdedikasi
     * + commit agar persist walau session ujian (native) sudah ditutup; dibungkus penuh
     * try/catch supaya kegagalan pencatatan TIDAK pernah mengganggu jalannya ujian.
     *
     * <p><b>Cara kerja:</b> membuka session Hibernate baru ({@code HibernateUtil.openSession()}),
     * menaikkan {@code jumlahPelanggaran} sebanyak satu, menambahkan satu baris bertimestamp ke
     * {@code logPelanggaran}, dan memangkas log dari DEPAN bila panjangnya melebihi 8.000
     * karakter agar kolom tidak meluap. Transaksi di-{@code rollback} bila baris hasil ujian
     * tidak ditemukan atau terjadi kesalahan, dan session SELALU dibersihkan lalu ditutup di
     * blok {@code finally}.</p>
     *
     * @param hasilId id {@code HasilUjianMahasiswa} yang dilanggar; null membuat method tidak
     *                melakukan apa pun
     * @param tipe    teks jenis pelanggaran kiriman skrip browser (misalnya "Pindah Tab /
     *                Sembunyikan Halaman"); null dicatat sebagai "Pelanggaran"
     */
    private static void catatPelanggaranUjian(Long hasilId, String tipe) {
        if (hasilId == null) {
            return;
        }
        Session sesi = null;
        try {
            sesi = HibernateUtil.openSession();
            sesi.beginTransaction();
            HasilUjianMahasiswa h = (HasilUjianMahasiswa) sesi.get(HasilUjianMahasiswa.class, hasilId);
            if (h != null) {
                int n = h.getJumlahPelanggaran() == null ? 0 : h.getJumlahPelanggaran().intValue();
                h.setJumlahPelanggaran(n + 1);
                String log = h.getLogPelanggaran() == null ? "" : h.getLogPelanggaran();
                String waktu = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                        .format(ais.ui.util.WaktuUtil.getDate());
                log = log + "[" + waktu + "] " + (tipe == null ? "Pelanggaran" : tipe) + "\n";
                if (log.length() > 8000) {
                    log = log.substring(log.length() - 8000);
                }
                h.setLogPelanggaran(log);
                sesi.update(h);
                sesi.getTransaction().commit();
            } else {
                sesi.getTransaction().rollback();
            }
        } catch (Exception e) {
            if (sesi != null) {
                try {
                    sesi.getTransaction().rollback();
                } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:5819");
                }
            }
        } finally {
            if (sesi != null) {
                try {
                    sesi.clear();
                    sesi.disconnect();
                    sesi.close();
                } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/ProsesUjianHelper.java:5828");
                }
            }
        }
    }

    /**
     * Versi anti-curang untuk halaman UJIAN JSP (<code>ujian.jsp</code>). MEMAKAI ULANG builder yang SAMA
     * seperti versi ZK; bedanya: (a) tanpa sink ZK (pelanggaran tak dilaporkan ke server), dan (b) saat batas
     * pelanggaran tercapai, menjalankan {@code jsFungsiSelesai} milik halaman JSP (mis.
     * {@code "processFinish123()"}). Membaca pengaturan PER-UJIAN dari kolom {@code ac_*} pada {@code ppu}
     * (default NON-AKTIF). Mengembalikan JS siap-tempel di dalam {@code <script>}, atau string kosong bila
     * {@code ppu} null atau anti-curang non-aktif untuk ujian tersebut.
     *
     * @param ppu             ujian.
     * @param jsFungsiSelesai cuplikan JS penyelesai ujian pada halaman JSP (mis. {@code "processFinish123()"}).
     * @return skrip anti-curang untuk JSP, atau "" bila tak berlaku.
     */
    public static String buildCbtAntiCheatScriptJsp(PertemuanPunyaUjian ppu, String jsFungsiSelesai) {
        if (ppu == null) {
            return "";
        }
        return buildCbtAntiCheatScript(ppu, null, jsFungsiSelesai);
    }

    /**
     * Membangun skrip JavaScript pengawasan ujian (anti-curang) sesuai pengaturan PER-UJIAN.
     *
     * <p>Ini adalah builder TUNGGAL yang dipakai ulang oleh dua konteks: jendela CBT berbasis ZK
     * (lewat {@link #prosesProsesUjian()}) dan halaman ujian berbasis JSP (lewat
     * {@link #buildCbtAntiCheatScriptJsp(PertemuanPunyaUjian,String)}).</p>
     *
     * <p><b>Yang dihasilkan skrip:</b> saklar global {@code window.__cbtOff} beserta fungsi
     * {@code window.__cbtStop()} (dipakai {@link #hentikanPengawasanUjian()}), fungsi pelapor
     * pelanggaran {@code rec()}, pembantu masuk layar penuh {@code cbtFS()}, overlay peringatan
     * {@code warn()}, dan sekumpulan pendengar peristiwa yang dipasang HANYA bila fitur
     * terkaitnya diaktifkan: {@code visibilitychange} (pindah tab), {@code blur} (Alt+Tab, dengan
     * cooldown), {@code fullscreenchange}, {@code contextmenu} (blokir klik kanan),
     * {@code keydown} (blokir Ctrl+W/T/N/R, Ctrl+Tab, Alt+F4, F5, F12, dan PrintScreen), serta
     * {@code beforeunload}.</p>
     *
     * <p><b>Sumber pengaturan:</b> seluruhnya dibaca dari kolom {@code ac_*} pada
     * {@link PertemuanPunyaUjian}; getter-nya mengembalikan nilai bawaan ketika kolom masih null,
     * sehingga ujian lama tetap terproteksi seperti sebelum migrasi dari konfigurasi global.
     * Mengembalikan string KOSONG bila {@code antiCurangAktif} bernilai salah, dan jatuh ke
     * {@link #buildCbtAntiCheatScriptDefault(String,String)} bila {@code ppu} null atau pembacaan
     * pengaturan melempar kesalahan (failsafe: pengawasan tetap menyala penuh).</p>
     *
     * <p><b>Batas kepercayaan (fakta arsitektur, bukan bug):</b> seluruh deteksi berjalan di
     * BROWSER peserta. Server hanya menerima laporan lewat {@code onPelanggaran} dan mencatatnya;
     * penghentian otomatis ujian ketika batas tercapai pun dilakukan skrip ini dengan mengklik
     * tombol "Selesaikan Ujian". Peserta yang mematikan JavaScript atau memodifikasi halaman
     * tidak akan tercatat maupun dihentikan. Pengawasan ini bersifat pencegah dan pembukti,
     * bukan penjamin.</p>
     *
     * @param ppu           ujian sumber pengaturan anti-curang PER-UJIAN.
     * @param sinkUuid       uuid komponen ZK penerima pelanggaran ({@code onPelanggaran}); {@code null} =
     *                       tak melaporkan (mis. konteks JSP).
     * @param jsAutoSelesai  cuplikan JS yang dijalankan saat batas pelanggaran tercapai (mis.
     *                       {@code "processFinish123()"} pada JSP). Bila kosong/null → default: klik tombol
     *                       ZK "Selesaikan Ujian". Membuat builder ini DIPAKAI ULANG oleh versi ZK &amp; JSP.
     * @return skrip anti-curang siap-tempel di dalam {@code <script>}; string kosong bila
     *         anti-curang non-aktif untuk ujian ini
     */
    private static String buildCbtAntiCheatScript(PertemuanPunyaUjian ppu, String sinkUuid, String jsAutoSelesai) {
        // ---- baca pengaturan PER-UJIAN (kolom ac_* di PertemuanPunyaUjian; getter mengembalikan default
        //      lama saat null → ujian lama tetap terproteksi seperti sebelum migrasi dari Konfigurasi global) ----
        final boolean aktif;
        final int batasPelanggaran;
        final boolean aktifkanFullscreen;
        final boolean deteksiPindahTab;
        final boolean deteksiBlurJendela;
        final int cooldownBlurMs;
        final boolean deteksiKeluarFullscreen;
        final boolean blokirKlikKanan;
        final boolean blokirShortcut;
        final boolean blokirTangkapLayar;
        final boolean peringatanKeluarHalaman;
        final String pesanPindahTab;
        final String pesanBlurJendela;
        final String pesanKeluarFullscreen;
        final String pesanKeluarHalaman;
        try {
            if (ppu == null) {
                return buildCbtAntiCheatScriptDefault(sinkUuid, jsAutoSelesai);
            }
            aktif = ppu.getAntiCurangAktif() == null || ppu.getAntiCurangAktif().booleanValue();
            if (!aktif) return "";

            batasPelanggaran = ppu.getAntiCurangBatasPelanggaran() == null ? 3
                : ppu.getAntiCurangBatasPelanggaran().intValue();

            aktifkanFullscreen = ppu.getAntiCurangAktifkanFullscreen() == null
                || ppu.getAntiCurangAktifkanFullscreen().booleanValue();
            deteksiPindahTab = ppu.getAntiCurangDeteksiPindahTab() == null
                || ppu.getAntiCurangDeteksiPindahTab().booleanValue();
            deteksiBlurJendela = ppu.getAntiCurangDeteksiBlurJendela() == null
                || ppu.getAntiCurangDeteksiBlurJendela().booleanValue();

            cooldownBlurMs = Math.max(0, ppu.getAntiCurangCooldownBlurMs() == null ? 5000
                : ppu.getAntiCurangCooldownBlurMs().intValue());

            deteksiKeluarFullscreen = ppu.getAntiCurangDeteksiKeluarFullscreen() == null
                || ppu.getAntiCurangDeteksiKeluarFullscreen().booleanValue();
            blokirKlikKanan = ppu.getAntiCurangBlokirKlikKanan() == null
                || ppu.getAntiCurangBlokirKlikKanan().booleanValue();
            blokirShortcut = ppu.getAntiCurangBlokirShortcut() == null
                || ppu.getAntiCurangBlokirShortcut().booleanValue();
            blokirTangkapLayar = ppu.getAntiCurangBlokirTangkapLayar() == null
                || ppu.getAntiCurangBlokirTangkapLayar().booleanValue();
            peringatanKeluarHalaman = ppu.getAntiCurangPeringatanKeluarHalaman() == null
                || ppu.getAntiCurangPeringatanKeluarHalaman().booleanValue();

            pesanPindahTab = ppu.getAntiCurangPesanPindahTab();
            pesanBlurJendela = ppu.getAntiCurangPesanBlurJendela();
            pesanKeluarFullscreen = ppu.getAntiCurangPesanKeluarFullscreen();
            pesanKeluarHalaman = ppu.getAntiCurangPesanKeluarHalaman();
        } catch (Exception e) {
            // Failsafe: bila terjadi masalah baca, pakai perilaku default (aktif penuh).
            return buildCbtAntiCheatScriptDefault(sinkUuid, jsAutoSelesai);
        }

        // ---- bangun script ----
        StringBuilder sb = new StringBuilder();
        sb.append("(function(){'use strict';");
        sb.append("var _v=0,_bl=false,_batas=").append(batasPelanggaran).append(";");
        // Saklar global untuk MENGHENTIKAN pengawasan saat ujian selesai/ditutup. Semua
        // pemicu pelanggaran & pembatas dinonaktifkan saat window.__cbtOff=true, dan
        // window.__cbtStop() keluar dari fullscreen + menutup overlay peringatan.
        sb.append("window.__cbtOff=false;");
        sb.append("window.__cbtStop=function(){try{window.__cbtOff=true;var ov=document.getElementById('cbt-ov');if(ov)ov.remove();var d=document;if(d.exitFullscreen&&d.fullscreenElement)d.exitFullscreen();else if(d.webkitExitFullscreen&&d.webkitFullscreenElement)d.webkitExitFullscreen();else if(d.mozCancelFullScreen&&d.mozFullScreenElement)d.mozCancelFullScreen();}catch(e){}};");
        // Kirim pelanggaran ke server (disimpan utk Rekap). No-op bila sink tak tersedia.
        if (sinkUuid != null) {
            sb.append("function rec(t){if(window.__cbtOff)return;try{if(window.zAu&&window.zk&&zk.Widget){var w=zk.Widget.$('")
              .append(sinkUuid).append("');if(w){zAu.send(new zk.Event(w,'onPelanggaran',t));}}}catch(e){}}");
        } else {
            sb.append("function rec(t){}");
        }

        // helper fullscreen
        if (aktifkanFullscreen || deteksiKeluarFullscreen) {
            sb.append("function cbtFS(){try{var e=document.documentElement;");
            sb.append("if(e.requestFullscreen)e.requestFullscreen();");
            sb.append("else if(e.webkitRequestFullscreen)e.webkitRequestFullscreen();");
            sb.append("else if(e.mozRequestFullScreen)e.mozRequestFullScreen();}catch(x){}}");
        } else {
            sb.append("function cbtFS(){}");
        }

        // warning overlay
        String batasLabel = batasPelanggaran > 0
            ? "' dari '+_batas+'. Jika batas tercapai, ujian akan dianggap selesai.'"
            : "'. Tetap fokus pada ujian.'";
        sb.append("function warn(msg){if(window.__cbtOff)return;");
        sb.append("var ov=document.getElementById('cbt-ov');if(ov)ov.remove();");
        sb.append("ov=document.createElement('div');ov.id='cbt-ov';");
        sb.append("ov.style.cssText='position:fixed;inset:0;background:rgba(10,15,30,.96);z-index:2147483647;display:flex;align-items:center;justify-content:center;font-family:sans-serif;';");
        sb.append("var bx=document.createElement('div');");
        sb.append("bx.style.cssText='background:#fff;border-radius:20px;padding:40px 48px;max-width:480px;text-align:center;box-shadow:0 24px 64px rgba(0,0,0,.55);';");
        sb.append("var ic=document.createElement('div');ic.style.cssText='font-size:56px;margin-bottom:12px;';ic.innerHTML='&#9888;&#65039;';bx.appendChild(ic);");
        sb.append("var h2=document.createElement('h2');h2.style.cssText='color:#b91c1c;font-size:22px;font-weight:900;margin:0 0 12px;';h2.textContent='Pelanggaran Terdeteksi!';bx.appendChild(h2);");
        sb.append("var p1=document.createElement('p');p1.style.cssText='color:#334155;font-size:15px;line-height:1.7;margin:0 0 8px;';p1.textContent=msg;bx.appendChild(p1);");
        sb.append("var p2=document.createElement('p');p2.style.cssText='color:#64748b;font-size:13px;margin:0 0 24px;';");
        sb.append("p2.textContent='Pelanggaran ke-'+_v+").append(batasLabel).append(";bx.appendChild(p2);");
        sb.append("var btn=document.createElement('button');");
        sb.append("btn.style.cssText='background:#1d4ed8;color:#fff;border:0;border-radius:999px;padding:13px 34px;font-size:15px;font-weight:700;cursor:pointer;';");
        sb.append("btn.textContent='Kembali ke Ujian';");
        sb.append("btn.addEventListener('click',function(){ov.remove();cbtFS();});");
        sb.append("bx.appendChild(btn);ov.appendChild(bx);document.body.appendChild(ov);");
        // auto-selesai jika batas tercapai
        if (batasPelanggaran > 0) {
            sb.append("if(_v>=_batas){");
            sb.append("p2.style.color='#b91c1c';p2.style.fontWeight='700';");
            sb.append("p2.textContent='Batas pelanggaran telah tercapai. Ujian akan diselesaikan secara otomatis.';");
            sb.append("btn.style.display='none';");
            sb.append("setTimeout(function(){");
            if (jsAutoSelesai != null && !jsAutoSelesai.trim().isEmpty()) {
                // Konteks non-ZK (mis. JSP): jalankan fungsi selesai milik halaman.
                sb.append("try{").append(jsAutoSelesai).append(";}catch(e){}");
            } else {
                // Konteks ZK: klik tombol "Selesaikan Ujian".
                sb.append("var s=document.querySelector('.z-toolbarbutton[title=\\\"Selesaikan Ujian\\\"]');");
                sb.append("if(s){s.click();}");
                sb.append("else{var btns=document.querySelectorAll('.z-toolbarbutton');");
                sb.append("for(var i=0;i<btns.length;i++){if(btns[i].textContent&&btns[i].textContent.indexOf('Selesaikan')>=0){btns[i].click();break;}}}");
            }
            sb.append("},3000);}");
        }
        sb.append("}");

        // masuk fullscreen
        if (aktifkanFullscreen) {
            sb.append("cbtFS();");
        }

        // deteksi pindah tab
        if (deteksiPindahTab) {
            sb.append("document.addEventListener('visibilitychange',function(){");
            sb.append("if(document.hidden){_v++;rec('Pindah Tab / Sembunyikan Halaman');warn(")
              .append(jsStr(pesanPindahTab)).append(");}");
            sb.append("});");
        }

        // deteksi blur jendela
        if (deteksiBlurJendela) {
            sb.append("window.addEventListener('blur',function(){");
            sb.append("if(!_bl&&!document.hidden){_bl=true;_v++;rec('Blur Jendela / Alt-Tab');");
            sb.append("warn(").append(jsStr(pesanBlurJendela)).append(");");
            sb.append("setTimeout(function(){_bl=false;},").append(cooldownBlurMs).append(");}});");
        }

        // deteksi keluar fullscreen
        if (deteksiKeluarFullscreen) {
            sb.append("document.addEventListener('fullscreenchange',function(){");
            sb.append("if(!document.fullscreenElement){_v++;rec('Keluar Fullscreen');warn(")
              .append(jsStr(pesanKeluarFullscreen)).append(");}});");
            sb.append("document.addEventListener('webkitfullscreenchange',function(){");
            sb.append("if(!document.webkitFullscreenElement){_v++;rec('Keluar Fullscreen');warn(")
              .append(jsStr(pesanKeluarFullscreen)).append(");}});");
        }

        // blokir klik kanan
        if (blokirKlikKanan) {
            sb.append("document.addEventListener('contextmenu',function(e){if(window.__cbtOff)return;e.preventDefault();});");
        }

        // blokir shortcut berbahaya
        if (blokirShortcut) {
            sb.append("document.addEventListener('keydown',function(e){");
            sb.append("if(e.ctrlKey&&'wWtTnNrR'.indexOf(e.key)>=0){e.preventDefault();e.stopPropagation();}");
            sb.append("if(e.ctrlKey&&e.key==='Tab'){e.preventDefault();}");
            sb.append("if(e.altKey&&e.key==='F4'){e.preventDefault();}");
            sb.append("if(e.key==='F5'||e.key==='F12'){e.preventDefault();}");
            sb.append("},true);");
        }

        // blokir tangkap layar (PrintScreen)
        if (blokirTangkapLayar) {
            sb.append("(function(){var s=document.createElement('style');s.textContent='body{-webkit-user-select:none;-moz-user-select:none;user-select:none;}';document.head.appendChild(s);})();");
            sb.append("document.addEventListener('keydown',function(e){");
            sb.append("if(e.key==='PrintScreen'||e.keyCode===44){e.preventDefault();e.stopPropagation();");
            sb.append("document.execCommand('copy');");
            sb.append("}},true);");
            sb.append("document.addEventListener('keyup',function(e){");
            sb.append("if(e.key==='PrintScreen'||e.keyCode===44){navigator.clipboard&&navigator.clipboard.writeText('').catch(function(){});}");
            sb.append("});");
        }

        // peringatan keluar halaman
        if (peringatanKeluarHalaman) {
            sb.append("window.addEventListener('beforeunload',function(e){if(window.__cbtOff)return;");
            sb.append("var m=").append(jsStr(pesanKeluarHalaman)).append(";");
            sb.append("e.returnValue=m;return m;});");
        }

        sb.append("})();");
        return sb.toString();
    }

    /** Fallback saat config tidak tersedia — perilaku default (semua aktif). */
    /**
     * Skrip anti-curang BAWAAN (seluruh fitur menyala) — jaring pengaman ketika pengaturan
     * per-ujian tidak dapat dibaca.
     *
     * <p>Dipanggil dari {@link #buildCbtAntiCheatScript(PertemuanPunyaUjian,String,String)} hanya
     * pada dua keadaan: {@code ppu} bernilai null, atau pembacaan kolom pengaturan {@code ac_*}
     * melempar kesalahan. Pilihan failsafe-nya sengaja "menyala penuh" supaya masalah konfigurasi
     * tidak pernah menurunkan tingkat pengawasan ujian.</p>
     *
     * <p><b>Perilaku bawaan yang dipasang:</b> batas 3 pelanggaran, masuk layar penuh otomatis,
     * deteksi pindah tab, deteksi blur jendela dengan cooldown 5 detik, deteksi keluar layar penuh,
     * blokir klik kanan, blokir Ctrl+W/T/N/R serta Ctrl+Tab, Alt+F4, F5 dan F12, dan peringatan
     * meninggalkan halaman — semuanya dengan teks peringatan berbahasa Indonesia yang tertanam.</p>
     *
     * <p><b>Catatan pemeliharaan:</b> isinya adalah duplikat sadar dari builder utama. Setiap
     * perubahan perilaku pada builder utama perlu direplikasi ke sini agar keduanya tidak
     * menyimpang satu sama lain.</p>
     *
     * @param sinkUuid      uuid komponen ZK penerima event {@code onPelanggaran}; null membuat
     *                      fungsi pelapor {@code rec()} menjadi no-op (misalnya konteks JSP)
     * @param jsAutoSelesai cuplikan JS penyelesai ujian milik halaman; bila kosong atau null,
     *                      skrip mencari lalu mengklik tombol ZK "Selesaikan Ujian"
     * @return skrip JavaScript siap-tempel di dalam {@code <script>}
     */
    private static String buildCbtAntiCheatScriptDefault(String sinkUuid, String jsAutoSelesai) {
        StringBuilder sb = new StringBuilder();
        sb.append("(function(){'use strict';");
        sb.append("var _v=0,_bl=false,_batas=3;");
        sb.append("window.__cbtOff=false;");
        sb.append("window.__cbtStop=function(){try{window.__cbtOff=true;var ov=document.getElementById('cbt-ov');if(ov)ov.remove();var d=document;if(d.exitFullscreen&&d.fullscreenElement)d.exitFullscreen();else if(d.webkitExitFullscreen&&d.webkitFullscreenElement)d.webkitExitFullscreen();else if(d.mozCancelFullScreen&&d.mozFullScreenElement)d.mozCancelFullScreen();}catch(e){}};");
        if (sinkUuid != null) {
            sb.append("function rec(t){if(window.__cbtOff)return;try{if(window.zAu&&window.zk&&zk.Widget){var w=zk.Widget.$('")
              .append(sinkUuid).append("');if(w){zAu.send(new zk.Event(w,'onPelanggaran',t));}}}catch(e){}}");
        } else {
            sb.append("function rec(t){}");
        }
        sb.append("function cbtFS(){try{var e=document.documentElement;");
        sb.append("if(e.requestFullscreen)e.requestFullscreen();");
        sb.append("else if(e.webkitRequestFullscreen)e.webkitRequestFullscreen();");
        sb.append("else if(e.mozRequestFullScreen)e.mozRequestFullScreen();}catch(x){}}");
        sb.append("function warn(msg){if(window.__cbtOff)return;");
        sb.append("var ov=document.getElementById('cbt-ov');if(ov)ov.remove();");
        sb.append("ov=document.createElement('div');ov.id='cbt-ov';");
        sb.append("ov.style.cssText='position:fixed;inset:0;background:rgba(10,15,30,.96);z-index:2147483647;display:flex;align-items:center;justify-content:center;font-family:sans-serif;';");
        sb.append("var bx=document.createElement('div');");
        sb.append("bx.style.cssText='background:#fff;border-radius:20px;padding:40px 48px;max-width:480px;text-align:center;box-shadow:0 24px 64px rgba(0,0,0,.55);';");
        sb.append("var ic=document.createElement('div');ic.style.cssText='font-size:56px;margin-bottom:12px;';ic.innerHTML='&#9888;&#65039;';bx.appendChild(ic);");
        sb.append("var h2=document.createElement('h2');h2.style.cssText='color:#b91c1c;font-size:22px;font-weight:900;margin:0 0 12px;';h2.textContent='Pelanggaran Terdeteksi!';bx.appendChild(h2);");
        sb.append("var p1=document.createElement('p');p1.style.cssText='color:#334155;font-size:15px;line-height:1.7;margin:0 0 8px;';p1.textContent=msg;bx.appendChild(p1);");
        sb.append("var p2=document.createElement('p');p2.style.cssText='color:#64748b;font-size:13px;margin:0 0 24px;';");
        sb.append("p2.textContent='Pelanggaran ke-'+_v+' dari '+_batas+'. Jika batas tercapai, ujian akan dianggap selesai.';bx.appendChild(p2);");
        sb.append("var btn=document.createElement('button');");
        sb.append("btn.style.cssText='background:#1d4ed8;color:#fff;border:0;border-radius:999px;padding:13px 34px;font-size:15px;font-weight:700;cursor:pointer;';");
        sb.append("btn.textContent='Kembali ke Ujian';");
        sb.append("btn.addEventListener('click',function(){ov.remove();cbtFS();});");
        sb.append("bx.appendChild(btn);ov.appendChild(bx);document.body.appendChild(ov);");
        sb.append("if(_v>=_batas){p2.style.color='#b91c1c';p2.style.fontWeight='700';");
        sb.append("p2.textContent='Batas pelanggaran telah tercapai. Ujian akan diselesaikan secara otomatis.';");
        sb.append("btn.style.display='none';");
        sb.append("setTimeout(function(){");
        if (jsAutoSelesai != null && !jsAutoSelesai.trim().isEmpty()) {
            sb.append("try{").append(jsAutoSelesai).append(";}catch(e){}");
        } else {
            sb.append("var s=document.querySelector('.z-toolbarbutton[title=\\\"Selesaikan Ujian\\\"]');");
            sb.append("if(s){s.click();}else{var btns=document.querySelectorAll('.z-toolbarbutton');");
            sb.append("for(var i=0;i<btns.length;i++){if(btns[i].textContent&&btns[i].textContent.indexOf('Selesaikan')>=0){btns[i].click();break;}}}");
        }
        sb.append("},3000);}");
        sb.append("}");
        sb.append("cbtFS();");
        sb.append("document.addEventListener('visibilitychange',function(){if(document.hidden){_v++;rec('Pindah Tab / Sembunyikan Halaman');warn('Anda terdeteksi berpindah tab atau aplikasi lain! Tetaplah di halaman ujian ini.');}});");
        sb.append("window.addEventListener('blur',function(){if(!_bl&&!document.hidden){_bl=true;_v++;rec('Blur Jendela / Alt-Tab');warn('Anda terdeteksi beralih ke jendela lain (contoh: Alt+Tab, Ctrl+Alt+Del). Segera kembali ke ujian!');setTimeout(function(){_bl=false;},5000);}});");
        sb.append("document.addEventListener('fullscreenchange',function(){if(!document.fullscreenElement){_v++;rec('Keluar Fullscreen');warn('Anda keluar dari mode layar penuh! Klik tombol di bawah untuk kembali ke ujian.');}});");
        sb.append("document.addEventListener('webkitfullscreenchange',function(){if(!document.webkitFullscreenElement){_v++;rec('Keluar Fullscreen');warn('Anda keluar dari mode layar penuh! Klik tombol di bawah untuk kembali.');}});");
        sb.append("document.addEventListener('contextmenu',function(e){if(window.__cbtOff)return;e.preventDefault();});");
        sb.append("document.addEventListener('keydown',function(e){if(window.__cbtOff)return;");
        sb.append("if(e.ctrlKey&&'wWtTnNrR'.indexOf(e.key)>=0){e.preventDefault();e.stopPropagation();}");
        sb.append("if(e.ctrlKey&&e.key==='Tab'){e.preventDefault();}");
        sb.append("if(e.altKey&&e.key==='F4'){e.preventDefault();}");
        sb.append("if(e.key==='F5'||e.key==='F12'){e.preventDefault();}");
        sb.append("},true);");
        sb.append("window.addEventListener('beforeunload',function(e){if(window.__cbtOff)return;var m='Jika Anda keluar, ujian akan dianggap selesai!';e.returnValue=m;return m;});");
        sb.append("})();");
        return sb.toString();
    }

    /** Mengubah string Java menjadi literal string JavaScript yang aman (single-quote). */
    /**
     * Mengubah sebuah string Java menjadi literal string JavaScript berkutip tunggal yang aman.
     *
     * <p>Dipakai untuk menyisipkan teks peringatan anti-curang — yang isinya dapat diatur admin
     * per ujian — ke dalam skrip yang dibangun. Tanpa pengawalan ini, tanda kutip atau backslash
     * di dalam teks akan memutus sintaks skrip, bahkan berpotensi menjadi jalan penyuntikan kode.</p>
     *
     * <p><b>Yang dikawal:</b> backslash menjadi backslash ganda, kutip tunggal menjadi kutip
     * tunggal terkawal, newline menjadi {@code \n}, dan carriage return dibuang. Nilai null
     * diperlakukan sebagai string kosong.</p>
     *
     * @param s teks yang akan dijadikan literal; boleh null
     * @return literal string JavaScript lengkap dengan kutip tunggal pembuka dan penutup
     */
    private static String jsStr(String s) {
        if (s == null) s = "";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "") + "'";
    }

}
