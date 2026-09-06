package ais.action.master.helper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
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
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.MatakuliahPrasyaratAction;
import ais.action.master.PerkuliahanAction;
import ais.action.master.helper.generic.AmbilDataTugasKelompok;
import ais.common.AIGenerator;
import ais.common.Common;
import ais.common.CommonEmail;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.Html2Text;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.DspaceInformation;
import ais.database.model.FormatNilai;
import ais.database.model.Kurikulum;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NamaTugasKelompok;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;
import ais.database.model.SyaratUjian;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.VOPembelajaran;
import ais.database.model.file.LampiranLain;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DetailGrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.DetailGrupPenilaian;
import ais.database.model.sekolah.DetailJenisPenilaian;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.JenisPenilaian;
import ais.database.model.sekolah.KategoriItemPenilaianSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Matapelajaran;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyCkEditor;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelAgakKecilBold;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldConfig;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
// MyToolbarbuttonConfig sudah tidak dipakai — semua tombol memakai MyToolbarbutton dengan FA icon.
import ais.ui.util.MyWindow;
import ais.ui.util.SmartDateTimeUtil;
import ais.ui.util.WaktuUtil;

/**
 * <h2>TugasKelompokHelper &mdash; Pengelola Tampilan &amp; Aksi Tugas Kelompok</h2>
 *
 * <p><b>Untuk apa (bahasa sederhana):</b> menampilkan dan mengelola seluruh tugas yang dikerjakan
 * secara berkelompok &mdash; baik di lingkup perkuliahan (mahasiswa), sekolah (siswa), maupun
 * kegiatan KKN/PKL. Dosen/guru dapat membuat tugas, menetapkan batas waktu, melihat anggota
 * kelompok beserta berkas yang dikumpulkan, dan memberi nilai; sedangkan mahasiswa/siswa dapat
 * melihat tugas, mengunggah berkas, dan memantau status pengumpulan kelompoknya.</p>
 *
 * <h3>Peran &amp; cara kerja</h3>
 * <p>Kelas ini adalah <i>helper</i> ZK (ZKoss 5.5) yang membangun komponen UI secara terprogram,
 * bukan halaman ZUL statis. Ia mengimplementasikan {@link ais.ui.util.DataLoader} sehingga dapat
 * dimuat ulang (paging/pencarian) lewat {@link #loadData(Object)}. Tampilan dirakit melalui
 * beberapa varian {@code display(...)} yang seluruhnya bermuara ke
 * {@link #display(ais.database.model.Perkuliahan, ais.database.model.KelompokKkn,
 * ais.database.model.KelompokPkl, ais.database.model.JadwalPelajaran, org.zkoss.zk.ui.Component)}:</p>
 * <ul>
 *   <li><b>Mode tab</b> (induk berupa {@code Tabpanel}): menampilkan header modul modern (kotak ikon
 *       gradien + judul + penjelasan singkat via {@link ais.ui.util.DashboardUiKit#headerModul}),
 *       toolbar (Tambah Tugas, Ambil Tugas, Cari, Refresh), grid daftar tugas, dan paging.</li>
 *   <li><b>Mode sisip</b> (induk komponen lain): tampilan ringkas berupa toolbar + grid yang
 *       ditanam di dalam halaman detail pembelajaran.</li>
 *   <li>{@link #tampilanTugas} menampilkan satu tugas terpilih secara rinci.</li>
 * </ul>
 *
 * <h3>Pengambilan data &amp; sesi Hibernate</h3>
 * <p>Kriteria daftar dibangun di {@link #initCriteria(boolean)} dengan filter pencarian (judul/nama/
 * keterangan) serta penyaring konteks (perkuliahan, KKN, PKL, jadwal pelajaran, atau SQL tambahan).
 * Seluruh akses basis data memakai {@code HibernateUtil.currentSession()} yang terikat pada thread
 * request ZK; sesi ini <b>tidak ditutup manual</b> karena dikelola dan ditutup otomatis oleh
 * kerangka kerja. Tidak ada {@code openSession()}/{@code currentNativeSession()} yang dibuka di
 * kelas ini, sehingga tidak diperlukan penutupan eksplisit di blok {@code finally}. Demi hemat
 * memori, daftar tugas dimuat per halaman ({@code setMaxResults}/{@code setFirstResult}) sehingga
 * hanya sebagian baris yang diambil ke memori, bukan seluruh tabel.</p>
 *
 * <h3>Tampilan modern &amp; pemakaian ulang</h3>
 * <p>Agar konsisten dengan halaman e-learning JSP modern (mis. {@code tugas_kelompok.jsp}), header
 * modul memakai komponen reusable {@link ais.ui.util.DashboardUiKit#headerModul} (HTML/CSS murni,
 * ikon SVG inline, warna mengikuti tema aktif) sehingga seragam tanpa menggandakan markup. Deskripsi
 * panel ditulis ringkas dan tanpa istilah teknis agar mudah dipahami pengguna non-IT.</p>
 *
 * <h3>Threading &amp; kompatibilitas</h3>
 * <p>Semua event listener berjalan pada thread ZK Desktop (single-thread per event). Kode dijaga
 * kompatibel dengan Java 1.7 (tanpa lambda/stream/diamond pada bagian inti) dan gaya {@code try/catch}
 * versi 1.6. Seluruh fungsi lama dipertahankan; penyempurnaan difokuskan pada kerapian tampilan dan
 * pencegahan kesalahan tanpa menghilangkan logika apa pun.</p>
 *
 * @author eCampus
 * @see ais.ui.util.DashboardUiKit
 * @see ais.ui.util.DataLoader
 */
public class TugasKelompokHelper implements DataLoader {
	/**
	 * <h3>Pembersih rujukan Format Nilai "yatim" pada satu Tugas Kelompok</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> memperbaiki satu jenis kerusakan data yang membuat
	 * halaman tugas kelompok gagal dibuka atau gagal disimpan. Setiap tugas kelompok boleh menunjuk ke
	 * satu komponen penilaian ("Format Nilai", misalnya <i>Tugas 30%</i>). Bila komponen penilaian itu
	 * kemudian dihapus dari daftar format nilai perkuliahan, kolom penunjuk di baris tugas kelompok
	 * <b>tidak ikut dikosongkan</b>, sehingga tersisa penunjuk ke baris yang sudah tidak ada lagi &mdash;
	 * inilah yang disebut rujukan <i>yatim</i> (istilah teknis: <i>orphan foreign key</i>). Ketika
	 * Hibernate mencoba memuat objek yang ditunjuk, ia melempar galat dan seluruh tampilan ikut gagal.
	 * Metode ini mengembalikan penunjuk tersebut menjadi kosong sehingga tugas kelompok dapat dibuka,
	 * disimpan, dan dipetakan ulang ke komponen penilaian yang benar.</p>
	 *
	 * <h4>Kapan dipanggil</h4>
	 * <p>Dipanggil tepat SEBELUM pengguna mengganti pilihan Format Nilai pada kartu tugas kelompok
	 * (listener {@code onChange} combobox Format Nilai di dalam {@code DetailPerkuliahanRenderer.render}).
	 * Urutannya sengaja demikian: bersihkan dulu rujukan rusak yang mungkin sudah ada, baru tulis pilihan
	 * baru. Tanpa langkah ini, pemanggilan {@code Common.refreshUpdate} pada baris yang masih memuat
	 * rujukan yatim akan gagal sebelum sempat menuliskan nilai penggantinya, dan pengguna terjebak: tidak
	 * bisa memperbaiki karena perbaikannya sendiri diblokir oleh kerusakan yang hendak diperbaiki.</p>
	 *
	 * <h4>Cara kerja &amp; alasan memakai SQL langsung</h4>
	 * <p>Perbaikan memakai satu perintah SQL bersyarat:</p>
	 * <pre>update tugas_kelompok t set format_nilai=null
	 * where t.id=:id and t.format_nilai is not null
	 *   and not exists (select 1 from formatnilai f where f.id=t.format_nilai)</pre>
	 * <p>Klausa {@code not exists} membuat perintah ini <b>hanya</b> menyentuh baris yang rujukannya memang
	 * sudah menggantung; tugas kelompok yang rujukannya masih sah tidak diubah sama sekali (0 baris
	 * terpengaruh). Klausa {@code t.id=:id} membatasi dampak pada SATU tugas kelompok yang sedang dibuka,
	 * bukan seluruh tabel &mdash; penting agar operasi pemeliharaan ini tidak pernah berubah menjadi
	 * pembersihan massal yang tidak disengaja. Parameter id diikat lewat {@code setLong} (bukan disambung
	 * sebagai teks), sehingga tidak ada celah penyisipan perintah SQL. SQL langsung dipilih karena lapisan
	 * pemetaan objek justru tidak sanggup memuat baris yang rujukannya rusak; perbaikan harus dikerjakan
	 * pada tingkat tabel.</p>
	 *
	 * <h4>Sesi &amp; transaksi terpisah (disengaja)</h4>
	 * <p>Metode ini sengaja TIDAK memakai {@code HibernateUtil.currentSession()} milik permintaan ZK,
	 * melainkan membuka sesi sendiri lewat {@code HibernateUtil.openSession()} beserta transaksi
	 * tersendiri. Alasannya: sesi permintaan bisa jadi sudah menampung objek {@code TugasKelompok} dalam
	 * keadaan rusak, dan menjalankan perbaikan di sesi terpisah menjamin perintah tetap berjalan serta
	 * hasilnya langsung permanen (commit) tanpa terpengaruh isi cache sesi utama. Sesi SELALU ditutup di
	 * blok {@code finally} lewat {@code HibernateUtil.closeSessionQuietly} sehingga tidak ada koneksi yang
	 * bocor, dan transaksi di-<i>rollback</i> lebih dulu bila terjadi kegagalan.</p>
	 *
	 * <p><b>Konsekuensi pada cache sesi utama.</b> Karena perbaikan terjadi di sesi lain, objek
	 * {@code TugasKelompok} yang masih dipegang sesi ZK <b>belum</b> mengetahui perubahan tersebut &mdash;
	 * di memori kolom penunjuknya masih berisi nilai lama. Itulah sebabnya pemanggil ({@code onChange})
	 * langsung menimpa nilai penunjuk dengan pilihan baru ({@code setFormatNilai}) lalu menyimpannya,
	 * sehingga keadaan di memori dan di basis data kembali seiring. Bila metode ini dipakai ulang di
	 * tempat lain, pemanggil WAJIB melakukan {@code session.refresh(...)} atau menimpa nilainya sendiri;
	 * kalau tidak, penyimpanan berikutnya akan menuliskan kembali rujukan yatim yang baru saja dihapus.</p>
	 *
	 * <h4>Perilaku kegagalan: berisik, bukan diam</h4>
	 * <p>Berbeda dari sebagian besar penjagaan kosmetik di kelas ini yang sengaja diam bila gagal (kartu
	 * ringkas, analitik, rincian Sub-CPMK), metode ini <b>melempar</b> {@link IllegalStateException} bila
	 * perbaikan tidak berhasil. Ini disengaja: bila pembersihan gagal, penulisan Format Nilai berikutnya
	 * hampir pasti juga gagal, sehingga lebih baik pengguna melihat pesan galat yang jelas (memuat id tugas
	 * kelompok yang bermasalah) daripada mengira pilihannya sudah tersimpan padahal tidak.</p>
	 *
	 * <p>Bila {@code tugasKelompok} bernilai {@code null} atau belum memiliki id (belum pernah disimpan),
	 * metode langsung keluar tanpa melakukan apa pun &mdash; tidak ada baris basis data yang dapat
	 * diperbaiki untuk data yang memang belum tersimpan.</p>
	 *
	 * @param tugasKelompok tugas kelompok yang rujukan Format Nilai-nya hendak diperiksa dan dibersihkan;
	 *                      boleh {@code null} atau belum ber-id (metode tidak melakukan apa pun)
	 * @throws IllegalStateException bila perintah perbaikan gagal dijalankan atau gagal di-commit; pesannya
	 *                               menyertakan id tugas kelompok yang bermasalah
	 * @see ais.database.model.TugasKelompok#getFormatNilai()
	 * @see ais.database.model.FormatNilai
	 */
	private static void bersihkanFormatNilaiYatim(TugasKelompok tugasKelompok) {
		if (tugasKelompok == null || tugasKelompok.getId() == null) {
			return;
		}
		Session sesiPerbaikan = null;
		Transaction transaksi = null;
		try {
			sesiPerbaikan = HibernateUtil.openSession();
			transaksi = sesiPerbaikan.beginTransaction();
			sesiPerbaikan.createSQLQuery("update tugas_kelompok t set format_nilai=null "
					+ "where t.id=:id and t.format_nilai is not null "
					+ "and not exists (select 1 from formatnilai f where f.id=t.format_nilai)")
					.setLong("id", tugasKelompok.getId().longValue()).executeUpdate();
			transaksi.commit();
		} catch (Exception e) {
			if (transaksi != null && transaksi.isActive()) {
				try { transaksi.rollback(); } catch (Exception abaikan) { }
			}
			throw new IllegalStateException("Gagal memperbaiki format nilai tugas kelompok "
					+ tugasKelompok.getId(), e);
		} finally {
			HibernateUtil.closeSessionQuietly(sesiPerbaikan);
		}
	}

	/**
	 * <h4>State helper: satu instance = satu layar Tugas Kelompok</h4>
	 *
	 * <p>Seluruh field di bawah ini adalah state instance, BUKAN state bersama. Satu
	 * {@code TugasKelompokHelper} dibuat untuk satu layar/tab, dipakai oleh satu desktop ZK, dan tidak
	 * boleh disimpan ke cache statis atau dibagikan lintas sesi &mdash; sebagian field memuat identitas
	 * pelajar yang sedang login, sehingga membagikannya berarti membocorkan data antarpengguna.</p>
	 *
	 * <p>State terbagi dalam empat kelompok yang saling bebas:</p>
	 * <ol>
	 *   <li><b>Identitas peserta</b> ({@code mahasiswa}, {@code biodataCalonMahasiswa}, {@code siswa},
	 *   {@code calonSiswa}) &mdash; diisi lewat konstruktor, menentukan apakah layar dibuka "sebagai
	 *   pelajar"; dibaca oleh {@link #konteksPelajar()} dan {@link #bolehKelola(Tbmuser)}.</li>
	 *   <li><b>Cakupan data</b> ({@code perkuliahan}, {@code kelompokKkn}, {@code kelompokPkl},
	 *   {@code jadwalPelajaran}, ketiga varian jamaknya, dan {@code sqlTambahan}) &mdash; menentukan
	 *   tugas kelompok MANA yang boleh muncul di daftar; dibaca oleh {@link #initCriteria(boolean)}.</li>
	 *   <li><b>Komponen UI daftar</b> ({@code grid}, {@code paging}, {@code cari}) &mdash; dipasang oleh
	 *   {@code display(...)} dan dipakai ulang oleh {@link #loadData(Object)} setiap kali data dimuat
	 *   ulang.</li>
	 *   <li><b>Komponen UI formulir</b> ({@code addWindow}, {@code judul}, {@code isi},
	 *   {@code mulaiWaktuMengumpulkanTugas}, {@code batasWaktuMengumpulkanTugas},
	 *   {@code banboxPerkuliahan}, {@code syaratMengumpulkanTugas}, {@code lampiran},
	 *   {@code tugasKelompok}) &mdash; dibangun oleh {@link #init(TugasKelompok)} dan dibaca kembali
	 *   oleh {@link #onSave(Event)}.</li>
	 * </ol>
	 *
	 * <p><b>Catatan penting soal daur hidup.</b> Kelompok (4) ditimpa ulang setiap kali formulir dibuka.
	 * Karena {@link #onSave(Event)} membaca field-field tersebut secara langsung (bukan menerima
	 * parameter), membuka dua formulir sekaligus dari satu instance helper akan membuat penyimpanan
	 * memakai komponen milik formulir yang terakhir dibuka. Alur pemakaian normal (formulir modal,
	 * satu per satu) mencegah hal ini terjadi.</p>
	 */

	/**
	 * Jendela modal formulir "Instruksi Tugas Kelompok" (tambah/ubah). Dibuat ulang setiap kali
	 * {@link #onAdd(Event, TugasKelompok)} atau {@link #onAddExternal} dipanggil, diisi oleh
	 * {@link #init(TugasKelompok)}, dan dilepas ({@code detach}) oleh tombol Batal maupun setelah
	 * {@link #onSave(Event)} berhasil. Bernilai {@code null} selama formulir belum pernah dibuka.
	 */
	private MyWindow addWindow;
	/**
	 * Grid daftar tugas kelompok. Dibuat oleh salah satu varian {@code display(...)} atau oleh
	 * {@link #tampilanTugas}, lalu diisi berulang kali oleh {@link #loadData(Object)} melalui
	 * {@code setModelCheckMobile}. Baris digambar oleh {@link DetailPerkuliahanRenderer}. Selalu
	 * dipasang sebelum {@code loadData} dipanggil pertama kali.
	 */
	private MyGrid grid;
	/**
	 * Cakupan perkuliahan (mata kuliah + kelas + tahun ajaran) yang sedang dibuka. Bila terisi,
	 * {@link #initCriteria(boolean)} membatasi daftar hanya pada tugas kelompok milik perkuliahan ini,
	 * dan {@link #init(TugasKelompok)} menyembunyikan pemilih perkuliahan karena sudah ditentukan.
	 * {@code null} berarti layar tidak dibatasi oleh satu perkuliahan tertentu.
	 */
	private Perkuliahan perkuliahan = null;

	/**
	 * Pengatur halaman daftar. Jumlah total baris dihitung ulang oleh {@link #loadData(Object)} lewat
	 * {@code Common.initPaging1}, dan halaman aktif ({@code getActivePage()}) dipakai untuk menghitung
	 * {@code setFirstResult} sehingga hanya satu halaman baris yang dimuat ke memori, bukan seluruh
	 * tabel. Berpindah halaman memicu {@code loadData} lagi lewat listener yang dipasang di
	 * {@code display(...)}.
	 */
	private Paging paging;

	/**
	 * Tugas kelompok yang sedang diedit di formulir, ATAU satu-satunya tugas yang ditampilkan pada mode
	 * rinci {@link #tampilanTugas}. Diisi oleh {@link #init(TugasKelompok)} dan dibaca kembali oleh
	 * {@link #onSave(Event)}; pada penyimpanan, objek ini ditukar dengan salinan terkelola hasil
	 * {@code session.load} bila sudah memiliki id, agar pembaruan aman terhadap data basi.
	 */
	private TugasKelompok tugasKelompok;
	/**
	 * Kotak isian judul tugas kelompok pada formulir (maksimal 255 karakter, 2 baris). Wajib diisi:
	 * {@link #onSave(Event)} menolak menyimpan dan menampilkan peringatan bila isinya kosong setelah
	 * dipangkas spasi.
	 */
	private Textbox judul;
	/**
	 * Penyunting teks kaya (CKEditor) untuk instruksi/langkah pengerjaan tugas kelompok. Isinya disimpan
	 * ke kolom {@code nama} milik {@link TugasKelompok} oleh {@link #onSave(Event)}. Komponen ini juga
	 * menjadi sasaran tulis fitur "Generate Tugas Kelompok" berbasis AI di {@link #init(TugasKelompok)}.
	 */
	private MyCkEditor isi;
	/**
	 * Berkas lampiran instruksi yang baru saja diunggah pada formulir, ditangkap dari callback
	 * {@code LampiranLain.createDownloadUploadFileLain}. Sengaja {@code protected} agar dapat diakses
	 * dari listener anonim di dalam kelas ini. Bernilai {@code null} bila pengguna tidak mengunggah apa
	 * pun; bila terisi, {@link #onSave(Event)} menautkannya ke tugas kelompok yang baru tersimpan dengan
	 * menyetel {@code ref} ke id tugas (lewat sesi streaming terpisah).
	 */
	protected LampiranLain lampiran;
	/**
	 * Cakupan kelompok KKN yang sedang dibuka. Analog dengan {@code perkuliahan}, tetapi untuk modul
	 * Kuliah Kerja Nyata: bila terisi, daftar dibatasi pada tugas kelompok milik kelompok KKN ini dan
	 * {@link #onSave(Event)} menautkan tugas baru ke kelompok tersebut.
	 */
	private KelompokKkn kelompokKkn = null;
	/**
	 * Cakupan kelompok PKL (Praktik Kerja Lapangan) yang sedang dibuka. Perannya sama persis dengan
	 * {@code kelompokKkn}, hanya berbeda modul asal.
	 */
	private KelompokPkl kelompokPkl = null;
	/**
	 * Pemilih tanggal &amp; jam saat tugas mulai dibuka. Disimpan ke kolom {@code mulai}. Selama waktu
	 * sekarang masih sebelum nilai ini, {@link DetailPerkuliahanRenderer#render} menampilkan pesan
	 * "Tugas belum mulai" dan menyembunyikan seluruh kontrol pengumpulan.
	 */
	private MyDatebox mulaiWaktuMengumpulkanTugas;
	/**
	 * Pemilih tanggal &amp; jam batas akhir pengumpulan. Disimpan ke kolom {@code selesai}. Sengaja boleh
	 * dikosongkan &mdash; kosong berarti tugas tidak memiliki batas waktu, dan pengecekan "sudah
	 * ditutup" pada kartu maupun {@code render} dilewati.
	 */
	private MyDatebox batasWaktuMengumpulkanTugas;
	/**
	 * Cakupan JAMAK: daftar perkuliahan yang tugas kelompoknya digabungkan dalam satu layar (mis. semua
	 * kelas yang diampu seorang dosen). Bila tidak kosong, {@link #initCriteria(boolean)} memakai
	 * {@code Restrictions.in} atas daftar ini. Bersifat menambah, bukan menggantikan, penyaring
	 * {@code perkuliahan} tunggal &mdash; bila keduanya terisi, keduanya diterapkan sekaligus.
	 */
	private List<Perkuliahan> perkuliahans;
	/**
	 * Cakupan JAMAK untuk kelompok KKN; berperilaku sama seperti {@code perkuliahans}.
	 */
	private List<KelompokKkn> kelompokKkns;
	/**
	 * Cakupan JAMAK untuk kelompok PKL; berperilaku sama seperti {@code perkuliahans}.
	 */
	private List<KelompokPkl> kelompokPkls;
	/**
	 * Penyaring SQL mentah tambahan yang ditempelkan apa adanya ke kriteria lewat
	 * {@code Restrictions.sqlRestriction} di {@link #initCriteria(boolean)}.
	 *
	 * <p><b>Peringatan bagi pengembang.</b> Isi field ini TIDAK diparameterkan dan TIDAK divalidasi;
	 * apa pun yang masuk ke sini menjadi bagian klausa WHERE. Karena itu field ini hanya boleh diisi
	 * dengan potongan SQL yang ditulis tetap di dalam kode, TIDAK PERNAH dengan nilai yang berasal dari
	 * masukan pengguna, parameter URL, atau isi basis data. Satu-satunya jalur pengisiannya adalah
	 * {@link #display(String, Component)}, yang pada revisi ini tidak dipanggil dari mana pun di dalam
	 * kode sumber &mdash; jadi jalur ini praktis tidak aktif. Bila kelak dipakai, penyaring ini juga
	 * berperan sebagai SATU-SATUNYA pembatas cakupan: bila seluruh field cakupan lain kosong, kriteria
	 * akan mencakup seluruh tabel tugas kelompok.
	 */
	private String sqlTambahan;
	/**
	 * Mahasiswa pemilik layar bila helper dibuka "sebagai mahasiswa". Diisi lewat
	 * {@link #TugasKelompokHelper(Mahasiswa, BiodataCalonMahasiswa)}. Terisinya field ini membuat
	 * {@link #konteksPelajar()} bernilai benar sehingga seluruh tombol kelola disembunyikan, dan
	 * diteruskan ke {@code NamaTugasKelompokHelper} agar mahasiswa dapat bergabung ke kelompok.
	 */
	private Mahasiswa mahasiswa;
	/**
	 * Calon mahasiswa (biodata PMB) pemilik layar, pasangan dari {@code mahasiswa} pada konstruktor yang
	 * sama. Diperlakukan sebagai pelajar persis seperti mahasiswa penuh.
	 */
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	/**
	 * Cakupan jadwal pelajaran (jalur SEKOLAH) yang sedang dibuka. Selain menyaring daftar, field ini
	 * mengalihkan seluruh blok penilaian di {@link DetailPerkuliahanRenderer#render} dari format nilai
	 * perkuliahan ke rantai penilaian sekolah ({@code JenisPenilaian} &rarr; {@code GrupPenilaian} &rarr;
	 * {@code GrupKategoriItemPenilaianSiswa} &rarr; {@code JenisItemPenilaianSiswa}).
	 */
	private JadwalPelajaran jadwalPelajaran = null;
	/**
	 * Siswa pemilik layar bila helper dibuka "sebagai siswa" lewat
	 * {@link #TugasKelompokHelper(Siswa, CalonSiswa)}. Sama seperti {@code mahasiswa}, terisinya field
	 * ini menandai konteks pelajar sehingga tombol kelola tidak dibuat.
	 */
	private Siswa siswa;
	/**
	 * Calon siswa (PSB) pemilik layar, pasangan dari {@code siswa} pada konstruktor yang sama.
	 * Diperlakukan sebagai pelajar penuh &mdash; peran inilah yang dahulu terlewat pada pemeriksaan
	 * tersebar dan menjadi alasan dibuatnya {@link #bolehKelola(Tbmuser)}.
	 */
	private CalonSiswa calonSiswa;
	/**
	 * Kotak pencarian daftar. Isinya dibaca langsung oleh {@link #initCriteria(boolean)} untuk menyusun
	 * penyaring {@code ilike} atas judul, nama, dan keterangan.
	 *
	 * <p>Field ini merangkap penanda "layar sudah siap": {@link #loadData(Object)} dan
	 * {@link #tempelRingkasanTugas(Component)} langsung keluar bila masih {@code null}, karena keduanya
	 * memanggil {@code initCriteria} yang membacanya tanpa penjagaan {@code null}.</p>
	 */
	private Textbox cari;
	/**
	 * Pemilih perkuliahan pada formulir, hanya terlihat ketika layar dibuka tanpa cakupan apa pun
	 * (perkuliahan, KKN, PKL, dan jadwal pelajaran semuanya {@code null}). Perkuliahan terpilih disimpan
	 * sebagai atribut komponen bernama {@code "perkuliahan"}, bukan sebagai nilai teks; itulah yang
	 * dibaca {@link #onSave(Event)} untuk validasi wajib-isi maupun untuk penyimpanan.
	 */
	private AmbilDataPerkuliahanBandbox banboxPerkuliahan;
	/**
	 * Pemilih {@link SyaratUjian} yang harus dipenuhi peserta sebelum boleh mengumpulkan tugas (mis.
	 * lunas administrasi). Daftar hanya memuat syarat aktif. Bila syarat terpilih ditandai "hanya boleh
	 * diubah oleh admin", combobox dinonaktifkan bagi dosen maupun mahasiswa dan sebuah keterangan
	 * penjelas dimunculkan.
	 */
	private Combobox syaratMengumpulkanTugas;
	/**
	 * Callback milik layar pemanggil, dijalankan setiap kali data berubah sehingga halaman induk (mis.
	 * daftar pertemuan) dapat menyegarkan tampilannya sendiri. Dipicu setelah penyimpanan berhasil,
	 * setelah penghapusan tugas, setelah pemindahan tugas ke pertemuan lain, dan saat formulir
	 * dibatalkan. Boleh {@code null} bila pemanggil tidak memerlukan pemberitahuan.
	 */
	private EventListener eventListener = null;

	/**
	 * Pemilih mode tampilan. {@code true} (bawaan) = mode DAFTAR: baris menampilkan informasi dosen
	 * pengampu, prasyarat mata kuliah, serta jadwal hari/jam/ruangan, dan penyimpanan diikuti
	 * {@link #loadData(Object)}. {@code false} = mode RINCI satu tugas (disetel oleh
	 * {@link #tampilanTugas}): blok informasi perkuliahan tambahan dilewati agar tidak mengulang
	 * informasi yang sudah tampil di halaman induk, dan penyimpanan menggambar ulang layar lewat
	 * {@code tampilanTugas} alih-alih memuat ulang daftar.
	 */
	private boolean tampilRinci = true;
	/**
	 * Komponen induk yang menampung layar pada mode RINCI, disimpan oleh {@link #tampilanTugas} agar
	 * dapat dibersihkan ({@code Common.clear}) dan digambar ulang setelah penyimpanan berhasil. Tidak
	 * dipakai pada mode daftar.
	 */
	private Component component = null;

	/**
	 * <h3>Konstruktor jalur PERGURUAN TINGGI (perkuliahan / KKN / PKL)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuat layar Tugas Kelompok dan sekaligus memberi tahu
	 * layar itu "siapa yang sedang melihat". Bila salah satu argumen diisi, layar dibuka <i>atas nama
	 * seorang pelajar</i> sehingga hanya menampilkan hal yang boleh dilihat dan dikerjakan peserta:
	 * membaca instruksi, melihat kelompoknya, bergabung ke kelompok, dan mengunggah berkas. Bila KEDUA
	 * argumen dibiarkan {@code null}, layar dibuka atas nama pengelola (dosen/admin) sehingga tombol
	 * kelola, penilaian, dan dashboard analitik ikut dibangun.</p>
	 *
	 * <p><b>Kedua argumen saling melengkapi, bukan saling menggantikan.</b> Satu orang hanya mengisi
	 * salah satu: mahasiswa aktif memakai {@code mahasiswa}, sedangkan calon mahasiswa yang datanya
	 * masih berupa biodata pendaftaran (PMB) memakai {@code biodataCalonMahasiswa}. Keduanya
	 * diperlakukan setara sebagai pelajar oleh {@link #konteksPelajar()}.</p>
	 *
	 * <h4>Pola pemakaian yang berlaku di seluruh kode</h4>
	 * <p>Semua pemanggil meneruskan identitas pengguna yang sedang login apa adanya, dengan bentuk
	 * {@code new TugasKelompokHelper(tbmuser.getMahasiswa(), tbmuser.getBiodataCalonMahasiswa())}.
	 * Konsekuensinya, kombinasi {@code (null, null)} berarti dua hal sekaligus: pengguna yang login
	 * bukan mahasiswa/calon mahasiswa, DAN layar dibuka dalam mode pengelola. Itulah sebabnya
	 * {@link #bolehKelola(Tbmuser)} tidak cukup memeriksa field ini saja &mdash; ia juga memeriksa
	 * objek pengguna yang login lewat {@link #loginPelajar(Tbmuser)}, agar peran pelajar yang tidak
	 * tercakup konstruktor ini (siswa, calon siswa, peserta kursus) tetap tidak dianggap pengelola.</p>
	 *
	 * <p><b>Tidak ada konstruktor tanpa argumen.</b> Ini disengaja: setiap pembuatan layar dipaksa
	 * menyatakan konteks penggunanya secara eksplisit, sehingga mustahil membuat helper "tanpa
	 * identitas" secara tidak sengaja. Kelas ini juga tidak menyalin data dari kedua objek yang
	 * diterima; keduanya disimpan sebagai rujukan dan diteruskan ke {@code NamaTugasKelompokHelper}.</p>
	 *
	 * @param mahasiswa             mahasiswa aktif pemilik layar, atau {@code null} bila bukan mahasiswa
	 * @param biodataCalonMahasiswa calon mahasiswa (biodata PMB) pemilik layar, atau {@code null}
	 * @see #TugasKelompokHelper(Siswa, CalonSiswa)
	 * @see #bolehKelola(Tbmuser)
	 */
	public TugasKelompokHelper(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.mahasiswa = mahasiswa;
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * <h3>Konstruktor jalur SEKOLAH (jadwal pelajaran)</h3>
	 *
	 * <p>Kembaran dari {@link #TugasKelompokHelper(Mahasiswa, BiodataCalonMahasiswa)} untuk modul
	 * sekolah: {@code siswa} untuk siswa terdaftar dan {@code calonSiswa} untuk calon siswa (PSB).
	 * Aturannya sama persis &mdash; salah satu argumen terisi berarti layar dibuka atas nama pelajar,
	 * keduanya {@code null} berarti mode pengelola (guru/admin).</p>
	 *
	 * <p><b>Catatan penting: kedua field jalur perguruan tinggi tetap kosong.</b> Helper yang dibuat
	 * lewat konstruktor ini selalu memiliki {@code mahasiswa} dan {@code biodataCalonMahasiswa}
	 * bernilai {@code null}. Sejumlah pemeriksaan lama di dalam kelas ini hanya menguji kedua field
	 * tersebut untuk menentukan "apakah pengelola"; pemeriksaan semacam itu akan salah menilai
	 * pemakaian jalur sekolah. Pemeriksaan yang benar adalah {@link #bolehKelola(Tbmuser)}, yang
	 * menggabungkan {@link #konteksPelajar()} (mencakup {@code siswa}/{@code calonSiswa}) dengan
	 * {@link #loginPelajar(Tbmuser)} (memeriksa objek pengguna yang login).</p>
	 *
	 * <p>Selain memengaruhi hak akses, jalur sekolah juga mengubah seluruh blok penilaian: nilai tugas
	 * kelompok dipetakan ke rantai penilaian sekolah ({@code JenisItemPenilaianSiswa} beserta grup dan
	 * kategorinya) alih-alih ke {@code FormatNilai} milik perkuliahan &mdash; lihat cabang
	 * {@code jadwalPelajaran != null} pada {@link DetailPerkuliahanRenderer#render}.</p>
	 *
	 * @param siswa      siswa pemilik layar, atau {@code null} bila bukan siswa
	 * @param calonSiswa calon siswa (PSB) pemilik layar, atau {@code null}
	 * @see #TugasKelompokHelper(Mahasiswa, BiodataCalonMahasiswa)
	 * @see #bolehKelola(Tbmuser)
	 */
	public TugasKelompokHelper(Siswa siswa, CalonSiswa calonSiswa) {
		this.siswa = siswa;
		this.calonSiswa = calonSiswa;
	}

	/**
	 * <h3>Penentu hak akses tampilan Tugas Kelompok (pengelola vs pelajar) &mdash; terpusat</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> tiga metode kecil ini menjadi SATU tempat untuk
	 * memutuskan apakah pengguna yang sedang melihat tugas kelompok berhak <i>mengelola</i>
	 * (dosen/guru/admin: menambah tugas, mengubah instruksi, menghapus, mengelola nilai &amp;
	 * kelompok, melihat dashboard analitik) ATAU hanya sebagai <i>pelajar/peserta</i> yang boleh
	 * melihat dan mengumpulkan tugasnya saja. Sebelumnya keputusan ini tersebar dan sebagian hanya
	 * memeriksa "bukan mahasiswa dan bukan siswa" sehingga <b>calon siswa</b>, <b>calon mahasiswa
	 * (biodata)</b>, dan <b>peserta kursus</b> keliru dianggap pengelola dan ikut melihat tombol
	 * kelola. Dengan memusatkan aturan di sini, seluruh cabang memakai logika yang sama sehingga
	 * konsisten, mudah dirawat, dan bebas dari celah tersebut.</p>
	 *
	 * <p><b>Cara kerja.</b> {@link #konteksPelajar()} menandai bahwa tampilan ini memang dibuka
	 * <i>dalam konteks pelajar</i> (helper dibangun lewat konstruktor mahasiswa/biodata calon
	 * mahasiswa atau siswa/calon siswa). {@link #loginPelajar(Tbmuser)} menandai bahwa pengguna yang
	 * login memang seorang pelajar/peserta (mahasiswa, siswa, calon siswa, calon mahasiswa, atau
	 * peserta kursus) &mdash; juga menganggap sesi tanpa pengguna ({@code null}) sebagai bukan
	 * pengelola demi keamanan. {@link #bolehKelola(Tbmuser)} bernilai benar HANYA jika kedua
	 * pemeriksaan di atas menyatakan "bukan pelajar", yaitu benar-benar pengelola. Semua tombol
	 * kelola (Tambah/Instruksi/Hapus/Kelola Nilai) dan dashboard analitik memakai {@code bolehKelola}
	 * sehingga aturan seragam; sedangkan fitur khusus pelajar (mis. bergabung kelompok, mengumpulkan
	 * berkas) tetap dijaga oleh pemeriksaan pelajar yang sesuai di tempatnya.</p>
	 *
	 * @return {@code true} bila tampilan dibuka dalam konteks pelajar (salah satu field
	 *         mahasiswa/biodataCalonMahasiswa/siswa/calonSiswa terisi)
	 */
	private boolean konteksPelajar() {
		return mahasiswa != null || biodataCalonMahasiswa != null || siswa != null || calonSiswa != null;
	}

	/**
	 * Menandai pengguna yang sedang login sebagai pelajar/peserta (bukan pengelola). Sesi tanpa
	 * pengguna ({@code null}) sengaja dianggap "pelajar" agar tombol kelola tidak pernah muncul saat
	 * identitas belum jelas. Mencakup mahasiswa, siswa, calon siswa, calon mahasiswa (biodata), dan
	 * peserta kursus &mdash; melengkapi pemeriksaan lama yang hanya menguji mahasiswa &amp; siswa.
	 *
	 * @param u pengguna login ({@code Common.getCurrentUser()})
	 * @return {@code true} bila {@code u} adalah pelajar/peserta atau {@code null}
	 */
	private boolean loginPelajar(Tbmuser u) {
		return u == null || u.getMahasiswa() != null || u.getSiswa() != null || u.getCalonSiswa() != null
				|| u.getBiodataCalonMahasiswa() != null || u.getPesertaKursus() != null;
	}

	/**
	 * Benar HANYA bila pengguna berhak mengelola tugas kelompok (dosen/guru/admin): bukan konteks
	 * pelajar dan bukan login pelajar/peserta. Dipakai seragam untuk visibilitas seluruh tombol
	 * kelola serta dashboard analitik.
	 *
	 * @param u pengguna login ({@code Common.getCurrentUser()})
	 * @return {@code true} bila boleh mengelola
	 */
	private boolean bolehKelola(Tbmuser u) {
		return !konteksPelajar() && !loginPelajar(u);
	}

	/**
	 * <h3>Kartu ringkas Tugas Kelompok (gaya kartu modern, responsif)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuat tampilan ringkas satu tugas kelompok dalam
	 * bentuk kartu yang enak dilihat dan mudah dipahami &mdash; menampilkan judul tugas, mata
	 * kuliah/kelas, kapan tugas mulai dan batas akhirnya, daftar kelompok beserta anggotanya (bisa
	 * dibuka-tutup), para dosen pengampu, serta jumlah peserta kelas. Kartu ini diletakkan di bagian
	 * ATAS tampilan tugas kelompok, sementara seluruh kontrol lama (unggah/unduh berkas tugas, syarat
	 * pengumpulan, nilai, dan aksi lainnya) tetap tampil apa adanya di bawah kartu ini &mdash;
	 * sehingga tidak ada fungsi yang hilang, hanya ditambah ringkasan yang lebih rapi di atas.
	 *
	 * <p><b>Efisiensi memori &amp; database.</b> Daftar anggota semua kelompok diambil dalam SATU
	 * kueri gabungan (menggunakan {@code Restrictions.in} atas seluruh kelompok) alih-alih satu kueri
	 * per kelompok (menghindari pola N+1 yang boros memori dan koneksi). Session dibuka melalui
	 * {@code HibernateUtil.openSession()} dan SELALU ditutup di blok {@code finally}
	 * ({@code disconnect()} + {@code ElearningSessionUtil.closeQuietly()}), sehingga tidak ada
	 * kebocoran koneksi. Semua string dibangun sekali lewat {@link StringBuffer}, dan URL foto
	 * dihitung saat session masih aktif sehingga aman dipakai setelah session ditutup.
	 *
	 * <p><b>Keamanan tampilan.</b> Seluruh teks dari data (judul, nama, NIM, dsb.) disaring lewat
	 * {@code DashboardUiKit.esc(...)} agar aman dari karakter HTML. Semua pemanggilan data dibungkus
	 * penjagaan {@code null} dan {@code try/catch} agar kegagalan memuat satu bagian (mis. daftar
	 * dosen atau jumlah peserta) tidak menggagalkan seluruh kartu; bagian yang gagal cukup
	 * dikosongkan. Metode ini tidak pernah melempar; bila terjadi masalah, dikembalikan potongan
	 * seaman mungkin (atau string kosong) sehingga tampilan lama tetap berjalan.
	 *
	 * <p><b>Tanpa ketergantungan JavaScript eksternal.</b> Bagian "buka-tutup" daftar anggota memakai
	 * elemen HTML native {@code <details>/<summary>} (accordion bawaan browser), bukan pustaka
	 * JavaScript pihak ketiga, sehingga ringan, tidak bentrok, dan tetap berfungsi di dalam ZK.
	 * Tata letak memakai CSS Grid/Flex modern: dua kolom di layar lebar (kiri: info &amp; kelompok,
	 * kanan: dosen &amp; tombol) dan satu kolom (menumpuk) di layar HP.
	 *
	 * @param tugas objek {@link TugasKelompok} yang akan diringkas menjadi kartu.
	 * @return potongan HTML (berikut gaya CSS-nya) siap disisipkan ke dalam komponen
	 *         {@link ais.ui.util.MyHtml}; string kosong bila {@code tugas} null.
	 */
	private String[] buatKartuRingkasTugasKelompok(TugasKelompok tugas) {
		if (tugas == null) {
			return new String[] { "", "", "" };
		}
		Perkuliahan perkuliahan = tugas.getPerkuliahan();

		// === Kelompok + anggota: 2 kueri (bukan N+1) ===
		List<NamaTugasKelompok> kelompoks = new ArrayList<NamaTugasKelompok>();
		Map<Long, StringBuffer> anggotaHtml = new java.util.HashMap<Long, StringBuffer>();
		Map<Long, Integer> jumlahAnggota = new java.util.HashMap<Long, Integer>();

		// Aturan tampil NILAI anggota (read-only): peserta (mahasiswa/siswa/calon) HANYA boleh
		// melihat nilainya SENDIRI (nilai peserta lain dikosongkan); dosen/admin boleh melihat SEMUA.
		Tbmuser userLogin = Common.getCurrentUser();
		boolean pesertaLogin = userLogin != null && (userLogin.getMahasiswa() != null
				|| userLogin.getBiodataCalonMahasiswa() != null || userLogin.getSiswa() != null
				|| userLogin.getCalonSiswa() != null);
		Long idPesertaLogin = null;
		if (userLogin != null) {
			if (userLogin.getMahasiswa() != null) {
				idPesertaLogin = userLogin.getMahasiswa().getId();
			} else if (userLogin.getBiodataCalonMahasiswa() != null) {
				idPesertaLogin = userLogin.getBiodataCalonMahasiswa().getId();
			} else if (userLogin.getSiswa() != null) {
				idPesertaLogin = userLogin.getSiswa().getId();
			} else if (userLogin.getCalonSiswa() != null) {
				idPesertaLogin = userLogin.getCalonSiswa().getId();
			}
		}

		// Untuk kurikulum OBE, nilai anggota disimpan PER Sub-CPMK di keteranganNilai (bukan di kolom
		// nilai tunggal). Badge menampilkan nilai gabungan (rata-rata berbobot) agar tetap informatif.
		boolean obeKartu = false;
		JSONObject bobotSubCpmk = null;
		JSONObject keteranganNilaiKartu = null;
		java.util.List<String[]> subCpmkKartu = null;
		try {
			if (perkuliahan != null && perkuliahan.getKurikulum() != null
					&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())
					&& tugas.getFormatNilais() != null && tugas.getFormatNilais().trim().length() > 0) {
				bobotSubCpmk = new JSONObject(tugas.getFormatNilais());
				if (bobotSubCpmk.length() > 0) {
					obeKartu = true;
					String ketK = tugas.getKeteranganNilai();
					keteranganNilaiKartu = new JSONObject(
							ketK == null || ketK.trim().length() == 0 ? "{}" : ketK.replace('\u0000', ' '));
				}
			}
		} catch (Exception eObeKartu) {
			obeKartu = false;
		}
		// Daftar Sub-CPMK yang dinilai tugas ini (id + label) untuk RINCIAN nilai anggota per Sub-CPMK
		// (permintaan kurikulum OBE). Dimuat SEKALI di sini, dipakai berulang untuk tiap anggota.
		if (obeKartu && bobotSubCpmk != null) {
			subCpmkKartu = daftarSubCpmkTugas(perkuliahan, bobotSubCpmk);
		}

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			kelompoks = session.createCriteria(NamaTugasKelompok.class).add(Restrictions.eq("tugasKelompok", tugas))
					.addOrder(Order.asc("nama")).list();
			if (kelompoks != null && !kelompoks.isEmpty()) {
				List<NamaTugasKelompokPunyaMahasiswa> rels = session
						.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
						.add(Restrictions.in("namaTugasKelompok", kelompoks)).list();
				if (rels != null) {
					for (NamaTugasKelompokPunyaMahasiswa r : rels) {
						if (r == null || r.getNamaTugasKelompok() == null
								|| r.getNamaTugasKelompok().getId() == null) {
							continue;
						}
						String foto = null;
						String nama = null;
						String ident = null;
						Long memberId = null;
						if (r.getMahasiswa() != null) {
							foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(r.getMahasiswa()));
							nama = r.getMahasiswa().getNama();
							ident = r.getMahasiswa().getNim();
							memberId = r.getMahasiswa().getId();
						} else if (r.getSiswa() != null) {
							foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(r.getSiswa()));
							nama = r.getSiswa().getNama();
							ident = r.getSiswa().getNomorInduk();
							memberId = r.getSiswa().getId();
						} else {
							continue;
						}
						// NILAI read-only: peserta hanya lihat nilainya sendiri; dosen/admin lihat semua.
						boolean bolehLihatNilai = !pesertaLogin
								|| (idPesertaLogin != null && idPesertaLogin.equals(memberId));
						double nilaiVal;
						String breakdownHtml = "";
						if (obeKartu) {
							// OBE: nilai gabungan (rata-rata berbobot) dari nilai per Sub-CPMK di keteranganNilai.
							String memberKey = memberId + (r.getMahasiswa() != null ? "_mhs" : "_siswa");
							nilaiVal = hitungNilaiObeMember(bobotSubCpmk, keteranganNilaiKartu, memberKey);
							// RINCIAN nilai PER Sub-CPMK (permintaan: kurikulum OBE tampilkan nilai per Sub-CPMK).
							if (bolehLihatNilai && nilaiVal > 0) {
								breakdownHtml = rincianNilaiObeMemberHtml(subCpmkKartu, keteranganNilaiKartu, memberKey);
							}
						} else {
							nilaiVal = r.getNilai() == null ? 0.0 : r.getNilai().doubleValue();
						}
						String nilaiTeks = "";
						if (bolehLihatNilai && nilaiVal > 0) {
							nilaiTeks = Common.numberFormat.get().format(nilaiVal);
						}
						Long gid = r.getNamaTugasKelompok().getId();
						StringBuffer b = anggotaHtml.get(gid);
						if (b == null) {
							b = new StringBuffer();
							anggotaHtml.put(gid, b);
						}
						b.append(anggotaItemHtml(foto, nama, ident, nilaiTeks, breakdownHtml));
						Integer c = jumlahAnggota.get(gid);
						jumlahAnggota.put(gid, (c == null ? 0 : c.intValue()) + 1);
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasKelompokHelper.java:430");
				}
				ais.common.ElearningSessionUtil.closeQuietly(session);
			}
		}

		// === Dosen pengampu + jumlah peserta ===
		StringBuffer dosenHtml = new StringBuffer();
		int pesertaCount = 0;
		if (perkuliahan != null) {
			try {
				List<Dosen> dosens = perkuliahan.populateDosenBuNama();
				if (dosens != null) {
					for (Dosen d : dosens) {
						if (d == null) {
							continue;
						}
						String foto = CommonMedia.getUrlFotoPengguna(new Tbmuser(d));
						dosenHtml.append("<div class='tk-dosen-item'><img class='tk-foto' src='")
								.append(foto == null ? "" : foto).append("'/><span>")
								.append(ais.ui.util.DashboardUiKit.esc(d.getNama())).append("</span></div>");
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			try {
				List<Mahasiswa> ms = perkuliahan.ambilMahasiswa();
				pesertaCount = ms == null ? 0 : ms.size();
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}

		// === Info mata kuliah & jadwal ===
		Matakuliah mk = perkuliahan == null ? null : perkuliahan.getMatakuliah();
		String mkNama = mk != null && mk.getNama() != null ? mk.getNama() : ais.ui.util.DashboardUiKit.esc(tugas.getJudul());
		String mkMeta = "";
		if (perkuliahan != null) {
			StringBuffer m = new StringBuffer();
			if (mk != null && mk.getKode() != null) {
				m.append("<span>").append(ais.ui.util.DashboardUiKit.esc(mk.getKode())).append("</span>");
			}
			if (perkuliahan.getKelas() != null) {
				m.append(m.length() > 0 ? "<span>&bull;</span>" : "").append("<span>Kelas ")
						.append(ais.ui.util.DashboardUiKit.esc(perkuliahan.getKelas())).append("</span>");
			}
			String ta = perkuliahan.getTahunAjaran() == null ? "" : perkuliahan.getTahunAjaran();
			String gg = perkuliahan.getGanjilGenap() == null ? "" : perkuliahan.getGanjilGenap();
			if (ta.length() > 0 || gg.length() > 0) {
				m.append(m.length() > 0 ? "<span>&bull;</span>" : "").append("<span>")
						.append(ais.ui.util.DashboardUiKit.esc(ta)).append(gg.length() > 0 ? " (" + ais.ui.util.DashboardUiKit.esc(gg) + ")" : "")
						.append("</span>");
			}
			mkMeta = m.toString();
		}

		String tglMulai = tugas.getMulai() == null ? "-"
				: SmartDateTimeUtil.getDayString(tugas.getMulai(), null) + " " + Common.dateFormat6.get().format(tugas.getMulai());
		String jamMulai = tugas.getMulai() == null ? "" : Common.timeFormat.get().format(tugas.getMulai());
		String tglSelesai = tugas.getSelesai() == null ? "-"
				: SmartDateTimeUtil.getDayString(tugas.getSelesai(), null) + " " + Common.dateFormat6.get().format(tugas.getSelesai());
		String jamSelesai = tugas.getSelesai() == null ? "" : Common.timeFormat.get().format(tugas.getSelesai());

		// Status pelaksanaan tugas (jadwal vs waktu sekarang) — info berguna bagi pengguna.
		String statusTk;
		java.util.Date kiniTk = ais.ui.util.WaktuUtil.getCalendar().getTime();
		if (tugas.getMulai() != null && kiniTk.before(tugas.getMulai())) {
			statusTk = "Belum dibuka";
		} else if (tugas.getSelesai() != null && kiniTk.after(tugas.getSelesai())) {
			statusTk = "Sudah ditutup";
		} else {
			statusTk = "Sedang berlangsung";
		}

		// === Rakit HTML jadi 3 bagian ===
		// [0] gaya CSS + header kartu (lebar penuh)
		// [1] isi kolom KIRI (mata kuliah, jadwal, daftar kelompok+anggota)
		// [2] isi bagian ATAS kolom KANAN (dosen pengampu + tombol peserta);
		//     kontrol tugas (unggah/nilai/dll) disisipkan di bawahnya sebagai komponen ZK oleh render().
		StringBuffer head = new StringBuffer();
		head.append(GAYA_KARTU_TUGAS);
		head.append("<div class='tk-head'><div class='tk-head-l'><div class='tk-head-ic'>&#128101;</div><div style='min-width:0;'>");
		head.append("<div class='tk-judul'>").append(ais.ui.util.DashboardUiKit.esc(tugas.getJudul())).append("</div>");
		head.append("<div class='tk-sub'>&#128218; ").append(Common.getBahasaConfig("Total Kelompok")).append(" : <b>")
				.append(kelompoks == null ? 0 : kelompoks.size()).append("</b></div>");
		head.append("<div class='tk-sub'>&#128100; ").append(Common.getBahasaConfig("Total Peserta")).append(" : <b>")
				.append(pesertaCount).append(" mhs</b></div>");
		head.append("<div class='tk-sub'>&#9201; ").append(Common.getBahasaConfig("Status")).append(" : <b>")
				.append(ais.ui.util.DashboardUiKit.esc(statusTk)).append("</b></div>");
		head.append("</div></div>");
		if (tugas.getSyaratMengumpulkanTugas() != null && tugas.getSyaratMengumpulkanTugas().getNama() != null) {
			head.append("<span class='tk-syarat'>&#9888; ")
					.append(ais.ui.util.DashboardUiKit.esc(tugas.getSyaratMengumpulkanTugas().getNama())).append("</span>");
		}
		head.append("</div>");

		StringBuffer kiri = new StringBuffer();
		kiri.append("<div class='tk-main'>");
		kiri.append("<div class='tk-mk'>").append(ais.ui.util.DashboardUiKit.esc(mkNama)).append("</div>");
		if (mkMeta.length() > 0) {
			kiri.append("<div class='tk-mk-meta'>").append(mkMeta).append("</div>");
		}
		kiri.append("<div class='tk-dates'>");
		kiri.append("<div class='tk-date'><b>").append(Common.getBahasaConfig("Mulai")).append("</b><div class='tk-date-v'>")
				.append(ais.ui.util.DashboardUiKit.esc(tglMulai)).append("</div><div class='tk-date-j'>")
				.append(ais.ui.util.DashboardUiKit.esc(jamMulai)).append("</div></div>");
		if (tugas.getSelesai() != null) {
			kiri.append("<div class='tk-date tk-date-end'><b>").append(Common.getBahasaConfig("Batas Akhir"))
					.append("</b><div class='tk-date-v'>").append(ais.ui.util.DashboardUiKit.esc(tglSelesai))
					.append("</div><div class='tk-date-j'>").append(ais.ui.util.DashboardUiKit.esc(jamSelesai))
					.append(" WIB</div></div>");
		}
		kiri.append("</div>");
		kiri.append("<div class='tk-sec-judul'>").append(Common.getBahasaConfig("Daftar Kelompok & Anggota")).append("</div>");
		if (kelompoks != null && !kelompoks.isEmpty()) {
			for (NamaTugasKelompok ntk : kelompoks) {
				if (ntk == null) {
					continue;
				}
				Integer jml = jumlahAnggota.get(ntk.getId());
				int jmlA = jml == null ? 0 : jml.intValue();
				kiri.append("<details class='tk-grp'><summary><span>&#128101; ")
						.append(ais.ui.util.DashboardUiKit.esc(ntk.getNama())).append("</span><span class='tk-badge'>")
						.append(jmlA).append(" ").append(Common.getBahasaConfig("Anggota")).append("</span></summary>");
				kiri.append("<div class='tk-grp-body'>");
				StringBuffer isi = anggotaHtml.get(ntk.getId());
				if (jmlA > 0 && isi != null) {
					kiri.append(isi);
				} else {
					kiri.append("<div class='tk-empty'>").append(Common.getBahasaConfig("Belum ada anggota di kelompok ini"))
							.append("</div>");
				}
				kiri.append("</div></details>");
			}
		} else {
			kiri.append("<div class='tk-alert'>&#8505; ").append(Common.getBahasaConfig("Belum ada pembagian kelompok."))
					.append("</div>");
		}
		kiri.append("</div>"); // tk-main

		StringBuffer kanan = new StringBuffer();
		if (dosenHtml.length() > 0) {
			kanan.append("<div class='tk-panel'><div class='tk-sec-judul' style='margin:0 0 4px;'>")
					.append(Common.getBahasaConfig("Dosen Pengampu")).append("</div>").append(dosenHtml).append("</div>");
		}
		if (perkuliahan != null && perkuliahan.getId() != null) {
			String linkPeserta = Common.ROOT + "/baru?hanya_tampil_jsp=true&p=elearning&s=daftar_peserta&clazz="
					+ Perkuliahan.class.getName() + "&id=" + perkuliahan.getId();
			kanan.append("<a class='tk-btn tk-btn-peserta' target='_blank' href='").append(linkPeserta).append("'>&#128101; ")
					.append(pesertaCount).append(" ").append(Common.getBahasaConfig("Peserta Kelas")).append("</a>");
		}

		return new String[] { head.toString(), kiri.toString(), kanan.toString() };
	}

	/**
	 * Satu baris anggota kelompok (foto bulat + nama + NIM/No.Induk) beserta NILAI read-only.
	 * Nilai ditampilkan sebagai badge di sisi kanan bila {@code nilaiTeks} tidak kosong; bila
	 * kosong (peserta lain yang tak boleh dilihat, atau belum dinilai) badge tidak ditampilkan.
	 */
	private String anggotaItemHtml(String foto, String nama, String ident, String nilaiTeks, String breakdownHtml) {
		String badge = (nilaiTeks == null || nilaiTeks.trim().length() == 0) ? ""
				: "<span class='tk-nilai' title='Nilai gabungan (rata-rata berbobot per Sub-CPMK)'>"
						+ ais.ui.util.DashboardUiKit.esc(nilaiTeks) + "</span>";
		String rincian = (breakdownHtml == null || breakdownHtml.trim().length() == 0) ? ""
				: "<div class='tk-cpmk-breakdown'>" + breakdownHtml + "</div>";
		return "<div class='tk-anggota'><img class='tk-foto' src='" + (foto == null ? "" : foto)
				+ "'/><div style='min-width:0;flex:1;'><div class='tk-anggota-nama'>"
				+ ais.ui.util.DashboardUiKit.esc(nama) + "</div><div class='tk-anggota-id'>"
				+ (ident == null || ident.trim().length() == 0 ? "-" : ais.ui.util.DashboardUiKit.esc(ident))
				+ "</div>" + rincian + "</div>" + badge + "</div>";
	}

	/**
	 * Daftar Sub-CPMK yang DINILAI oleh tugas ini (yang tercentang di {@code formatNilais}), terurut,
	 * sebagai pasangan {@code [idFormatNilai, label]}. Dipakai untuk memecah nilai gabungan anggota
	 * menjadi rincian per Sub-CPMK di kartu OBE. Memuat daftar komponen dari {@code Common.getFormatNilais}
	 * (dengan fallback yang sama seperti {@code bangunGridSubCpmk}), hanya mengambil baris ber-Sub-CPMK
	 * ({@code statusPertemuan != null}) yang ada di {@code bobotSubCpmk}. Mengembalikan list kosong bila gagal.
	 *
	 * @param perkuliahan  perkuliahan (OBE) sumber daftar komponen
	 * @param bobotSubCpmk peta bobot Sub-CPMK tugas ini (dari {@code tugas.getFormatNilais()})
	 * @return daftar {@code [id, label]} Sub-CPMK yang dinilai; kosong bila tidak ada / gagal
	 */
	private java.util.List<String[]> daftarSubCpmkTugas(Perkuliahan perkuliahan, JSONObject bobotSubCpmk) {
		java.util.List<String[]> hasil = new java.util.ArrayList<String[]>();
		if (perkuliahan == null || bobotSubCpmk == null) {
			return hasil;
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
			boolean sudahadasubCpmk = false;
			if (formatNilais != null) {
				for (FormatNilai nilai : formatNilais) {
					if (nilai.getNama() != null && nilai.getNama().toLowerCase().contains("cpmk")) {
						sudahadasubCpmk = true;
						break;
					}
				}
			}
			if (!sudahadasubCpmk) {
				formatNilais = Common.getFormatNilais(perkuliahan, true);
			}
			if (formatNilais != null) {
				for (FormatNilai nilai : formatNilais) {
					if (nilai.getStatusPertemuan() == null || nilai.getId() == null) {
						continue;
					}
					String id = nilai.getId().toString();
					if (bobotSubCpmk.isNull(id)) {
						continue; // hanya Sub-CPMK yang dinilai tugas ini
					}
					hasil.add(new String[] { id, nilai.getNama() == null ? ("Sub-CPMK " + id) : nilai.getNama() });
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasKelompokHelper.java:632");
			// abaikan — rincian per Sub-CPMK bersifat opsional, jangan gagalkan kartu
		}
		return hasil;
	}

	/**
	 * Membangun HTML rincian nilai seorang anggota PER Sub-CPMK berupa chip kecil ({@code label + nilai})
	 * untuk kartu OBE. Nilai tiap Sub-CPMK dibaca dari kunci {@code <memberKey>_nilai_<idSubCpmk>} di
	 * {@code keteranganNilai}; Sub-CPMK yang belum dinilai ditampilkan "&ndash;". Aman terhadap nilai
	 * kosong/bukan angka ({@code optDouble}) sehingga tidak pernah melempar.
	 *
	 * @param subCpmk        daftar {@code [id, label]} Sub-CPMK dari {@link #daftarSubCpmkTugas}
	 * @param keteranganNilai peta nilai per anggota-per-komponen (dari {@code tugas.getKeteranganNilai()})
	 * @param memberKey      kunci anggota: {@code <id>_mhs} atau {@code <id>_siswa}
	 * @return HTML chip per Sub-CPMK; string kosong bila tidak ada komponen
	 */
	private String rincianNilaiObeMemberHtml(java.util.List<String[]> subCpmk, JSONObject keteranganNilai,
			String memberKey) {
		if (subCpmk == null || subCpmk.isEmpty() || keteranganNilai == null || memberKey == null) {
			return "";
		}
		StringBuilder bd = new StringBuilder();
		for (String[] sc : subCpmk) {
			if (sc == null || sc.length < 2) {
				continue;
			}
			String kunci = memberKey + "_nilai_" + sc[0];
			String nTeks = keteranganNilai.has(kunci)
					? Common.numberFormat.get().format(keteranganNilai.optDouble(kunci, 0.0)) : "–";
			bd.append("<span class='tk-cpmk-chip'><b>").append(ais.ui.util.DashboardUiKit.esc(sc[1])).append("</b> ")
					.append(ais.ui.util.DashboardUiKit.esc(nTeks)).append("</span>");
		}
		return bd.toString();
	}

	/**
	 * Menghitung nilai gabungan (rata-rata berbobot) seorang anggota pada tugas kelompok OBE, dari
	 * nilai per Sub-CPMK yang tersimpan di {@code TugasKelompok.keteranganNilai}. Bobot tiap Sub-CPMK
	 * diambil dari peta {@code formatNilais} (id Sub-CPMK &rarr; bobot). Untuk setiap Sub-CPMK: nilai
	 * anggota dibaca dari kunci {@code <memberKey>_nilai_<idSubCpmk>}, lalu dijumlahkan dengan bobot
	 * sebagai penimbang. Hasil = &Sigma;(nilai&times;bobot) / &Sigma;(bobot); mengembalikan 0 bila
	 * belum ada bobot/nilai. Semua pembacaan memakai {@code optDouble} sehingga aman terhadap nilai
	 * yang kosong atau bukan angka (tidak pernah melempar).
	 *
	 * @param formatNilais  peta bobot Sub-CPMK (dari {@code tugas.getFormatNilais()})
	 * @param keteranganNilai peta nilai per anggota-per-komponen (dari {@code tugas.getKeteranganNilai()})
	 * @param memberKey     kunci anggota: {@code <id>_mhs} atau {@code <id>_siswa}
	 * @return nilai gabungan 0..100 (0 bila belum dinilai)
	 */
	private double hitungNilaiObeMember(JSONObject formatNilais, JSONObject keteranganNilai, String memberKey) {
		if (formatNilais == null || keteranganNilai == null || memberKey == null) {
			return 0.0;
		}
		double totalBerbobot = 0.0;
		double totalBobot = 0.0;
		java.util.Iterator<String> keys = formatNilais.keys();
		while (keys.hasNext()) {
			String fnId = keys.next();
			try {
				double bobot = formatNilais.optDouble(fnId, 0.0);
				if (bobot <= 0) {
					continue;
				}
				double nilaiKomp = keteranganNilai.optDouble(memberKey + "_nilai_" + fnId, 0.0);
				totalBerbobot += nilaiKomp * bobot;
				totalBobot += bobot;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/TugasKelompokHelper.java:699");
				// abaikan komponen yang tak dapat dibaca
			}
		}
		return totalBobot > 0 ? totalBerbobot / totalBobot : 0.0;
	}

	/**
	 * <h3>Grid pemetaan Sub-CPMK &amp; Bobot &mdash; dua mode: baca-saja / dapat diedit</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menampilkan daftar komponen penilaian OBE (Sub-CPMK)
	 * beserta bobotnya untuk satu tugas kelompok. Metode ini dipakai di DUA tempat dengan perilaku
	 * berbeda melalui parameter {@code editable}: (1) di dalam <i>kartu ringkas</i> tugas kelompok,
	 * dipanggil dengan {@code editable = false} sehingga tampil sebagai <b>label baca-saja</b> yang
	 * rapi (tanda centang + nilai bobot, tanpa kotak isian) &mdash; pengguna hanya melihat pemetaan
	 * yang sedang berlaku; (2) di dalam <i>Window "Instruksi Tugas Kelompok"</i>, dipanggil dengan
	 * {@code editable = true} sehingga tampil sebagai kotak centang + kotak angka bobot yang bisa
	 * diubah dan disimpan otomatis. Dengan pemisahan ini, kartu tetap bersih dan bebas dari kendali
	 * yang tidak sengaja tergeser, sementara penyuntingan tetap tersedia namun terpusat di satu
	 * tempat (form Instruksi) sesuai instruksi kerapian tampilan.</p>
	 *
	 * <p><b>Sumber data &amp; efisiensi.</b> Daftar {@link FormatNilai} diambil sekali melalui
	 * {@link ais.common.Common#getFormatNilais(org.hibernate.Session, ais.database.model.Perkuliahan)}
	 * pada {@code HibernateUtil.currentSession()} (sesi thread-request yang dikelola kerangka kerja
	 * dan TIDAK ditutup manual di sini). Bila belum ada komponen ber-nama mengandung "cpmk", daftar
	 * dilengkapi via {@code getFormatNilais(perkuliahan, true)} agar Sub-CPMK tetap muncul. Nilai
	 * bobot terpilih dibaca dari kolom JSON {@code tugasKelompok.getFormatNilais()} (peta id
	 * komponen &rarr; bobot). Hanya baris dengan {@code getStatusPertemuan() != null} yang
	 * ditampilkan, meniru logika lama agar konsisten.</p>
	 *
	 * <p><b>Penyuntingan (mode editable).</b> Saat kotak centang di-klik atau bobot diubah, perubahan
	 * langsung ditulis ke peta JSON lalu disimpan lewat {@code Common.refreshUpdate}. Sebelum menyimpan,
	 * entitas di-{@code refresh} bila sudah memiliki id agar tidak menimpa data basi. Kotak bobot
	 * dinonaktifkan otomatis ketika komponennya tidak dicentang. Ketika perkuliahan sudah dikunci
	 * ({@code getDikunci() != null}), kotak centang dinonaktifkan sehingga pemetaan tidak dapat diubah.
	 * Seluruh perilaku ini menyalin persis kode lama yang sebelumnya berada di dalam kartu, hanya
	 * dipindah ke satu tempat agar dapat dipakai ulang tanpa menggandakan logika.</p>
	 *
	 * <p><b>Ketahanan.</b> Seluruh proses dibungkus {@code try/catch}; bila terjadi kegagalan memuat
	 * (mis. data komponen tidak lengkap), kesalahan hanya dicatat untuk admin dan grid dilewati tanpa
	 * menggagalkan seluruh tampilan. Pada mode baca-saja, jika tidak ada satu pun baris yang layak
	 * tampil, grid dilepas ({@code detach}) agar tidak menyisakan kotak kosong.</p>
	 *
	 * @param parent    komponen induk tempat grid ditanam (mis. Hbox kartu atau Div di form Instruksi)
	 * @param tugasKelompok tugas kelompok yang pemetaan Sub-CPMK-nya ditampilkan/disunting
	 * @param perkuliahan   perkuliahan (OBE) sumber daftar komponen penilaian
	 * @param editable  {@code true} untuk kotak isian yang dapat diubah; {@code false} untuk label baca-saja
	 */
	private void bangunGridSubCpmk(Component parent, final TugasKelompok tugasKelompok, final Perkuliahan perkuliahan,
			final boolean editable) {
		if (parent == null || tugasKelompok == null || perkuliahan == null) {
			return;
		}
		try {
			Session session = HibernateUtil.currentSession();
			List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
			boolean sudahadasubCpmk = false;
			if (formatNilais != null && !formatNilais.isEmpty()) {
				for (FormatNilai nilai : formatNilais) {
					if (nilai.getNama() != null && nilai.getNama().toLowerCase().contains("cpmk")) {
						sudahadasubCpmk = true;
						break;
					}
				}
			}
			if (!sudahadasubCpmk) {
				formatNilais = Common.getFormatNilais(perkuliahan, true);
			}
			if (formatNilais == null) {
				return;
			}
			final JSONObject jsonObject = new JSONObject(
					tugasKelompok.getFormatNilais() == null ? "{}" : tugasKelompok.getFormatNilais());

			MyGrid gridPilih = new MyGrid();
			gridPilih.setParent(parent);
			gridPilih.setWidth("100%");
			gridPilih.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(gridPilih);

			MyColumnConfig column = new MyColumnConfig("Sub-CPMK");
			column.setParent(columns);
			column.setWidth("80%");

			column = new MyColumnConfig("Bobot");
			column.setParent(columns);

			Rows rowsPilih = new Rows();
			rowsPilih.setParent(gridPilih);

			boolean adaBaris = false;
			for (FormatNilai nilai : formatNilais) {
				if (nilai.getStatusPertemuan() == null) {
					continue;
				}
				adaBaris = true;
				final boolean checked = !jsonObject.isNull(nilai.getId().toString());
				final double bobot = checked ? jsonObject.getDouble(nilai.getId().toString()) : 100.0;
				String labelSub = nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)";

				if (editable) {
					final Checkbox radio = new Checkbox(labelSub);
					radio.setAttribute("value", nilai);
					radio.setWidth("95%");
					MyFormRow rowPilih = new MyFormRow();
					rowPilih.setValign("top");
					rowPilih.setParent(rowsPilih);
					rowPilih.appendChild(radio);

					final MyDoublebox doubleboxBobot = new MyDoublebox(bobot);
					doubleboxBobot.setWidth("90%");
					rowPilih.appendChild(doubleboxBobot);

					radio.setChecked(checked);
					doubleboxBobot.setDisabled(!checked);
					radio.setDisabled(perkuliahan.getDikunci() != null);

					EventListener eventListenerD = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Session session = HibernateUtil.currentSession();
							if (tugasKelompok.getId() != null) {
								session.refresh(tugasKelompok);
							}
							FormatNilai fn = (FormatNilai) radio.getAttribute("value");
							if (radio.isChecked()) {
								jsonObject.put(fn.getId().toString(),
										doubleboxBobot.getValue() == null ? 100.0 : doubleboxBobot.getValue());
							} else {
								jsonObject.remove(fn.getId().toString());
							}
							tugasKelompok.setFormatNilais(jsonObject.toString());
							Common.refreshUpdate(session, (tugasKelompok));
							doubleboxBobot.setDisabled(!radio.isChecked());
						}
					};
					radio.addEventListener("onClick", eventListenerD);
					doubleboxBobot.addEventListener("onChange", eventListenerD);
				} else {
					// Mode BACA-SAJA: tampil sebagai label (tanda centang + bobot), bukan kotak isian.
					MyFormRow rowPilih = new MyFormRow();
					rowPilih.setValign("top");
					rowPilih.setParent(rowsPilih);
					Label lblSub = new Label((checked ? "☑ " : "☐ ") + labelSub);
					lblSub.setStyle(checked ? "font-weight:600;color:#0f172a;" : "color:#94a3b8;");
					rowPilih.appendChild(lblSub);
					Label lblBobot = new Label(checked ? Common.numberFormat.get().format(bobot) : "-");
					lblBobot.setStyle(checked ? "font-weight:700;color:#166534;" : "color:#94a3b8;");
					rowPilih.appendChild(lblBobot);
				}
			}
			if (!adaBaris && !editable) {
				gridPilih.detach();
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>Window "Kelola Nilai" &mdash; entri nilai per anggota kelompok</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuka jendela khusus untuk mengisi nilai setiap
	 * anggota di seluruh kelompok pada satu tugas kelompok, dalam satu daftar rata yang mudah dan
	 * cepat &mdash; tanpa perlu membuka-tutup tiap kelompok. Setiap baris menampilkan nama kelompok,
	 * identitas anggota (NIM/No. Induk &amp; nama), kotak <b>Nilai</b>, dan kotak <b>Keterangan</b>.
	 * Perubahan pada kotak Nilai atau Keterangan langsung tersimpan begitu berpindah kolom (event
	 * {@code onChange}), sehingga dosen/guru dapat menilai secara beruntun dengan lancar.</p>
	 *
	 * <p><b>Responsif.</b> Ukuran jendela mengikuti perangkat: di layar HP dibuat 100% (penuh),
	 * sedangkan di desktop 95% &times; 95% agar lega namun tetap menyisakan bingkai. Tata letak
	 * memakai {@link org.zkoss.zul.Borderlayout} (isi di tengah + tombol Tutup di bawah) sehingga
	 * daftar panjang dapat digulir dengan nyaman.</p>
	 *
	 * <p><b>Efisiensi memori &amp; database.</b> Daftar anggota semua kelompok diambil dalam DUA
	 * kueri saja (satu untuk daftar kelompok, satu untuk seluruh relasi anggota memakai
	 * {@code Restrictions.in}) &mdash; bukan satu kueri per kelompok (menghindari pola N+1). Sesi
	 * memakai {@code HibernateUtil.currentSession()} yang dikelola kerangka kerja dan tidak ditutup
	 * manual di sini. Saat menyimpan satu baris, entitas dimuat ulang lewat {@code session.load}
	 * (bila sudah ber-id) agar pembaruan aman terhadap data basi, lalu disimpan dengan
	 * {@code Common.refreshUpdate}.</p>
	 *
	 * <p><b>Ketahanan.</b> Bila belum ada anggota sama sekali, ditampilkan pesan yang menuntun
	 * pengguna membuat kelompok lewat tombol "Kelola Kelompok" terlebih dulu. Jendela ditutup lewat
	 * tombol Tutup ({@code detach}).</p>
	 *
	 * @param tugas tugas kelompok yang nilainya akan dikelola
	 * @throws Exception bila terjadi kegagalan saat memodalkan jendela
	 */
	private void bukaKelolaNilai(final TugasKelompok tugas) throws Exception {
		final MyWindow win = new MyWindow();
		win.setTitle("Kelola Nilai Anggota — " + (tugas.getJudul() == null ? "" : tugas.getJudul()));
		if (Common.isMobile()) {
			win.setWidth("100%");
			win.setHeight("100%");
		} else {
			win.setWidth("95%");
			win.setHeight("95%");
		}
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid g = new MyGrid();
		g.setSclass("fgrid");
		g.setWidth("100%");
		g.setHeight("100%");
		g.setParent(center);

		// === Deteksi OBE: bila perkuliahan OBE & ada Sub-CPMK terpilih, nilai diisi PER KOMPONEN ===
		// (disimpan di TugasKelompok.keteranganNilai JSON, key: <idAnggota>_<mhs|siswa>_nilai_<idSubCpmk>),
		// meniru mekanisme lama sebelum tombol nilai dipindah ke popup. Non-OBE: satu kolom "Nilai".
		final java.util.List<FormatNilai> obeFormatNilais = new java.util.ArrayList<FormatNilai>();
		Perkuliahan perkuliahanNilai = tugas.getPerkuliahan();
		boolean obeTmp = false;
		try {
			if (perkuliahanNilai != null && perkuliahanNilai.getKurikulum() != null
					&& perkuliahanNilai.getKurikulum().apakahObe(perkuliahanNilai.getTahunAjaran(),
							perkuliahanNilai.getGanjilGenap())
					&& tugas.getFormatNilais() != null && tugas.getFormatNilais().trim().length() > 0) {
				JSONObject jfn = new JSONObject(tugas.getFormatNilais());
				List<FormatNilai> formatNilaisAll = Common.getFormatNilais(HibernateUtil.currentSession(), perkuliahanNilai);
				if (formatNilaisAll != null) {
					for (FormatNilai fn : formatNilaisAll) {
						if (fn.getStatusPertemuan() != null && fn.getId() != null && !jfn.isNull(fn.getId().toString())) {
							obeFormatNilais.add(fn);
						}
					}
				}
				obeTmp = !obeFormatNilais.isEmpty();
			}
		} catch (Exception eObe) {
			Common.tampilErrorJikaAdmin(eObe);
		}
		final boolean obe = obeTmp;
		String ketAwal = tugas.getKeteranganNilai();
		final JSONObject jsonKet = new JSONObject(
				ketAwal == null || ketAwal.trim().length() == 0 ? "{}" : ketAwal.replace('\u0000', ' '));

		Columns cols = new Columns();
		cols.setParent(g);
		MyColumnConfig cKel = new MyColumnConfig("Kelompok");
		cKel.setParent(cols);
		cKel.setWidth(obe ? "13%" : "22%");
		MyColumnConfig cAng = new MyColumnConfig("Anggota");
		cAng.setParent(cols);
		// Kolom tunggal "Nilai & Keterangan" \u2014 menggantikan kolom CPMK per-kolom.
		MyColumnConfig cValKet = new MyColumnConfig("Nilai & Keterangan");
		cValKet.setParent(cols);
		cValKet.setWidth("50%");

		Rows rows = new Rows();
		rows.setParent(g);

		Session session = HibernateUtil.currentSession();
		List<NamaTugasKelompok> kelompoks = session.createCriteria(NamaTugasKelompok.class)
				.add(Restrictions.eq("tugasKelompok", tugas)).addOrder(Order.asc("nama")).list();

		int jml = 0;
		if (kelompoks != null && !kelompoks.isEmpty()) {
			List<NamaTugasKelompokPunyaMahasiswa> rels = session
					.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
					.add(Restrictions.in("namaTugasKelompok", kelompoks)).list();
			if (rels != null) {
				for (final NamaTugasKelompokPunyaMahasiswa r : rels) {
					if (r == null || r.getNamaTugasKelompok() == null) {
						continue;
					}
					String nmKelompok = r.getNamaTugasKelompok().getNama();
					String nama = null;
					String ident = null;
					String memberKeyTmp = null;
					if (r.getMahasiswa() != null) {
						nama = r.getMahasiswa().getNama();
						ident = r.getMahasiswa().getNim();
						memberKeyTmp = r.getMahasiswa().getId() + "_mhs";
					} else if (r.getSiswa() != null) {
						nama = r.getSiswa().getNama();
						ident = r.getSiswa().getNomorInduk();
						memberKeyTmp = r.getSiswa().getId() + "_siswa";
					} else {
						continue;
					}
					final String memberKey = memberKeyTmp;
					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setParent(rows);
					row.appendChild(new Label(nmKelompok == null ? "-" : nmKelompok));
					row.appendChild(new Label((ident == null ? "" : ident + " — ") + (nama == null ? "" : nama)));

					// Kolom tunggal: ringkasan nilai + tombol Edit popup
					final String namaFinal = nama;
					final String identFinal = ident;
					final org.zkoss.zul.Vbox kcCell = new org.zkoss.zul.Vbox();
					kcCell.setStyle("gap:2px;width:100%;");
					row.appendChild(kcCell);

					final org.zkoss.zul.Vbox kSummary = new org.zkoss.zul.Vbox();
					kSummary.setStyle("gap:1px;");
					kSummary.setParent(kcCell);

					String ketCurr = r.getKeterangan();
					if (ketCurr != null && !ketCurr.trim().isEmpty()) {
						Label lKet = new Label("Ket: " + ketCurr);
						lKet.setStyle("color:#555;font-size:11px;white-space:normal;");
						lKet.setParent(kSummary);
					}
					if (obe) {
						for (FormatNilai fnSumm : obeFormatNilais) {
							double nSumm = jsonKet.optDouble(memberKey + "_nilai_" + fnSumm.getId(), 0.0);
							String fnNamaSumm = fnSumm.getNama() != null ? fnSumm.getNama() : ("CPMK " + fnSumm.getId());
							Label lN = new Label(fnNamaSumm + ": " + (nSumm > 0.0 ? Common.numberFormat.get().format(nSumm) : "-"));
							lN.setStyle("font-size:11px;");
							lN.setParent(kSummary);
						}
					} else {
						double nSumm = r.getNilai() != null ? r.getNilai() : 0.0;
						Label lN = new Label("Nilai: " + (nSumm > 0.0 ? Common.numberFormat.get().format(nSumm) : "-"));
						lN.setStyle("font-size:11px;");
						lN.setParent(kSummary);
					}

					MyToolbarbutton btnEditNilai = new MyToolbarbutton("fa-pencil", "Edit Nilai");
					btnEditNilai.setStyle("margin-top:2px;");
					btnEditNilai.setParent(kcCell);
					btnEditNilai.addEventListener("onClick", new EventListener() {
						@Override
						public void onEvent(Event evEdit) throws Exception {
							final org.zkoss.zul.Window popupW = new org.zkoss.zul.Window();
							popupW.setTitle("Entry Nilai \u2014 " + (identFinal == null ? "" : identFinal + " \u2014 ") + (namaFinal == null ? "" : namaFinal));
							popupW.setWidth("480px");
							popupW.setClosable(true);
							popupW.setBorder("normal");
							popupW.setPage(evEdit.getTarget().getPage());

							org.zkoss.zul.Vbox pContent = new org.zkoss.zul.Vbox();
							pContent.setStyle("padding:8px;gap:6px;width:100%;");
							pContent.setParent(popupW);

							Label lblKetT = new Label("Keterangan:");
							lblKetT.setStyle("font-weight:bold;");
							lblKetT.setParent(pContent);
							final Textbox pKet = new Textbox(r.getKeterangan());
							pKet.setRows(2);
							pKet.setWidth("100%");
							pKet.setMaxlength(255);
							pKet.setParent(pContent);

							Label lblNilaiT = new Label("Nilai:");
							lblNilaiT.setStyle("font-weight:bold;margin-top:4px;");
							lblNilaiT.setParent(pContent);

							final java.util.ArrayList<MyDoublebox> pNilaiBoxes = new java.util.ArrayList<MyDoublebox>();
							if (obe) {
								for (FormatNilai fnPop : obeFormatNilais) {
									Hbox rowFn = new Hbox();
									rowFn.setStyle("align-items:center;gap:6px;");
									rowFn.setParent(pContent);
									String fnNP = fnPop.getNama() != null ? fnPop.getNama() : ("CPMK " + fnPop.getId());
									Label lblFn = new Label(fnNP + ":");
									lblFn.setStyle("min-width:120px;font-size:12px;");
									lblFn.setParent(rowFn);
									double nPop = jsonKet.optDouble(memberKey + "_nilai_" + fnPop.getId(), 0.0);
									MyDoublebox nbP = new MyDoublebox(nPop);
									ais.ui.util.UIUtil.gayaInputNilai(nbP);
									nbP.setParent(rowFn);
									pNilaiBoxes.add(nbP);
								}
							} else {
								double nPop = r.getNilai() != null ? r.getNilai() : 0.0;
								MyDoublebox nbP = new MyDoublebox(nPop);
								ais.ui.util.UIUtil.gayaInputNilai(nbP);
								nbP.setParent(pContent);
								pNilaiBoxes.add(nbP);
							}

							Hbox btnRowP = new Hbox();
							btnRowP.setStyle("margin-top:8px;justify-content:flex-end;gap:8px;");
							btnRowP.setParent(pContent);
							final org.zkoss.zul.Label lblStatus = new org.zkoss.zul.Label("");
							lblStatus.setParent(btnRowP);
							org.zkoss.zul.Button btnSimpanP = new org.zkoss.zul.Button("Simpan");
							btnSimpanP.setSclass("btn btn-primary btn-sm");
							btnSimpanP.setParent(btnRowP);
							btnSimpanP.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event evSave) throws Exception {
									try {
										Session sSave = HibernateUtil.currentSession();
										NamaTugasKelompokPunyaMahasiswa xSave = (NamaTugasKelompokPunyaMahasiswa)
												sSave.load(NamaTugasKelompokPunyaMahasiswa.class, r.getId());
										xSave.setKeterangan(pKet.getValue());
										if (!obe && !pNilaiBoxes.isEmpty()) {
											xSave.setNilai(pNilaiBoxes.get(0).getValue());
										}
										Common.refreshUpdate(sSave, xSave);
										if (obe) {
											if (tugas.getId() != null) sSave.refresh(tugas);
											String ketDb = tugas.getKeteranganNilai();
											JSONObject dbKet = new JSONObject(ketDb == null || ketDb.trim().length() == 0
													? "{}" : ketDb.replace('\u0000', ' '));
											for (int idxP = 0; idxP < obeFormatNilais.size(); idxP++) {
												FormatNilai fnSv = obeFormatNilais.get(idxP);
												Double nvSv = pNilaiBoxes.get(idxP).getValue();
												String kSv = memberKey + "_nilai_" + fnSv.getId();
												dbKet.put(kSv, nvSv);
												jsonKet.put(kSv, nvSv);
											}
											tugas.belum("tugas_file_content_" + tugas.getClass().getName());
											tugas.setKeteranganNilai(dbKet.toString());
											Common.refreshUpdate(sSave, tugas);
										}
										while (kSummary.getFirstChild() != null) {
											kSummary.getFirstChild().detach();
										}
										String ketNew = pKet.getValue();
										if (ketNew != null && !ketNew.trim().isEmpty()) {
											Label lKetU = new Label("Ket: " + ketNew);
											lKetU.setStyle("color:#555;font-size:11px;white-space:normal;");
											lKetU.setParent(kSummary);
										}
										if (obe) {
											for (int idxU = 0; idxU < obeFormatNilais.size(); idxU++) {
												FormatNilai fnU = obeFormatNilais.get(idxU);
												Double nvU = pNilaiBoxes.get(idxU).getValue();
												String fnNamaU = fnU.getNama() != null ? fnU.getNama() : ("CPMK " + fnU.getId());
												Label lNU = new Label(fnNamaU + ": " + (nvU != null && nvU > 0.0 ? Common.numberFormat.get().format(nvU) : "-"));
												lNU.setStyle("font-size:11px;");
												lNU.setParent(kSummary);
											}
										} else if (!pNilaiBoxes.isEmpty()) {
											Double nvU = pNilaiBoxes.get(0).getValue();
											Label lNU = new Label("Nilai: " + (nvU != null && nvU > 0.0 ? Common.numberFormat.get().format(nvU) : "-"));
											lNU.setStyle("font-size:11px;");
											lNU.setParent(kSummary);
										}
										popupW.detach();
									} catch (Exception eSave) {
										lblStatus.setValue("Gagal: " + eSave.getMessage());
										lblStatus.setStyle("color:red;font-size:11px;");
										ais.common.ErrorAuditUtil.record(eSave, "TugasKelompokHelper popup Simpan");
									}
								}
							});
							popupW.doModal();
						}
					});
					jml++;
				}
			}
		}
		if (jml == 0) {
			MyFormRow row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new Label(
					"Belum ada anggota kelompok. Tambahkan anggota lewat tombol \"Kelola Kelompok\" terlebih dahulu."));
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);
		Toolbar tb = new Toolbar();
		tb.setParent(south);

		MyToolbarbutton btnBatal = new MyToolbarbutton("fa-undo", "Batal");
		btnBatal.setTooltiptext("Batal: tutup tanpa menyimpan");
		btnBatal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				win.detach();
			}
		});
		btnBatal.setParent(tb);

		MyToolbarbutton tutup = new MyToolbarbutton("fa-times", "Tutup");
		tutup.setTooltiptext("Tutup");
		tutup.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event ev) throws Exception {
				win.detach();
			}
		});
		tutup.setParent(tb);
		borderlayout.setParent(win);

		win.setVisible(true);
		win.onModal();
	}

	/**
	 * <h3>Dashboard analitik &amp; ringkasan nilai Tugas Kelompok (HTML/CSS modern, tanpa JFreeChart)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menyusun satu bagian ringkasan yang berisi angka-angka
	 * penting dan beberapa grafik agar dosen/guru langsung paham kondisi penilaian sebuah tugas
	 * kelompok tanpa harus membaca satu per satu. Yang ditampilkan: berapa jumlah kelompok, berapa
	 * jumlah anggota, berapa yang sudah dinilai, berapa nilai rata-ratanya; grafik lingkaran (donat)
	 * status pengumpulan (kelompok yang sudah vs belum mengunggah berkas), grafik batang perbandingan
	 * nilai rata-rata tiap kelompok, grafik donat sebaran nilai anggota (dikelompokkan per rentang),
	 * dan &mdash; khusus kurikulum OBE &mdash; jaring laba-laba (radar) perbandingan bobot tiap
	 * Sub-CPMK. Ditutup dengan kotak "Sorotan" berisi nilai tertinggi, nilai terendah, kelompok
	 * terbaik, dan jumlah anggota yang belum dinilai.</p>
	 *
	 * <h3>Kenapa memakai HTML/CSS/SVG, bukan JFreeChart</h3>
	 * <p>Seluruh grafik dibangun dari komponen reusable {@link ais.ui.util.DashboardUiKit}
	 * ({@code cards}, {@code donut} dengan <i>conic-gradient</i> CSS, {@code barList} batang HTML,
	 * {@code spider} radar SVG, {@code insight}, serta {@code openGrid}/{@code closeGrid} untuk grid
	 * responsif). Pendekatan ini ringan (tidak menghasilkan gambar di server, tidak menambah memori
	 * untuk buffer citra), tampil tajam di layar HP maupun desktop (SVG/CSS menyesuaikan lebar
	 * kontainer), mengikuti warna tema aktif, dan seragam dengan dashboard lain di aplikasi &mdash;
	 * sehingga perawatan ke depan cukup di satu tempat (maksimalkan pemakaian ulang).</p>
	 *
	 * <h3>Efisiensi memori &amp; database (penting)</h3>
	 * <p>Seluruh data dihitung hanya dari <b>tiga kueri</b> ringkas dan langsung diringkas ke
	 * beberapa akumulator angka, bukan menyimpan seluruh baris di memori: (1) daftar kelompok pada
	 * tugas; (2) satu kueri gabungan status pengumpulan memakai {@code Restrictions.in} atas seluruh
	 * id kelompok (menghindari pola N+1 &mdash; tidak ada kueri per kelompok); (3) satu kueri seluruh
	 * relasi anggota memakai {@code Restrictions.in}. Nilai diakumulasikan dalam loop tunggal
	 * (jumlah, banyak, min, maks, sebaran per rentang, serta jumlah &amp; total per kelompok) sehingga
	 * pemakaian memori hanya sebesar peta kecil ber-kunci id kelompok, bukan sebesar jumlah anggota.
	 * Session dibuka lewat {@code HibernateUtil.openSession()} dan SELALU ditutup di blok
	 * {@code finally} ({@code disconnect()} + {@code ElearningSessionUtil.closeQuietly()}), sehingga
	 * tidak ada kebocoran koneksi meskipun terjadi kesalahan di tengah proses. Bagian jaring Sub-CPMK
	 * yang memerlukan daftar {@link FormatNilai} memakai {@code currentSession()} (dikelola kerangka
	 * kerja, tidak ditutup manual) dan dipanggil SETELAH session khusus di atas ditutup.</p>
	 *
	 * <h3>Ketahanan &amp; keamanan tampilan</h3>
	 * <p>Metode ini tidak pernah melempar keluar: semua akses dibungkus {@code try/catch} gaya 1.6,
	 * dan bila data belum cukup (mis. belum ada kelompok atau belum ada anggota) dikembalikan string
	 * kosong sehingga bagian analitik cukup tidak ditampilkan tanpa mengganggu kartu utama. Bagian
	 * status pengumpulan dan jaring Sub-CPMK masing-masing dijaga {@code try/catch} sendiri sehingga
	 * kegagalan salah satu tidak menggagalkan grafik lain. Seluruh teks dari data (nama kelompok,
	 * nama Sub-CPMK) otomatis disaring lewat {@code DashboardUiKit.esc(...)} di dalam komponen kit,
	 * aman dari karakter HTML. Grafik dibungkus elemen {@code <details>} bawaan browser sehingga bisa
	 * dibuka-tutup tanpa JavaScript tambahan, menjaga kartu tetap ringkas secara bawaan.</p>
	 *
	 * <h3>Kompatibilitas</h3>
	 * <p>Kode dijaga kompatibel Java 1.7 (tanpa lambda/stream), berjalan di thread ZK Desktop, dan
	 * hanya membangun {@link String} sehingga hasilnya dapat disisipkan ke {@link ais.ui.util.MyHtml}
	 * kapan pun. Hanya diperuntukkan bagi pengelola (dosen/admin); pemanggil bertanggung jawab
	 * menyembunyikan bagian ini dari mahasiswa/siswa.</p>
	 *
	 * @param tugas tugas kelompok yang akan dianalisis
	 * @return potongan HTML dashboard siap-tempel; string kosong bila data belum cukup atau terjadi kegagalan
	 */
	private String bangunAnalitikHtml(TugasKelompok tugas) {
		if (tugas == null) {
			return "";
		}
		Perkuliahan perkuliahan = tugas.getPerkuliahan();

		int totalKelompok = 0;
		int totalAnggota = 0;
		int totalDinilai = 0;
		double sumNilai = 0;
		double maxNilai = -1;
		double minNilai = -1;
		String namaTertinggi = "";
		java.util.LinkedHashMap<String, Double> avgPerKelompok = new java.util.LinkedHashMap<String, Double>();
		int b0 = 0, b60 = 0, b70 = 0, b80 = 0, b90 = 0; // <60, 60-69, 70-79, 80-89, 90-100
		int kelompokSudah = 0;

		Session session = null;
		try {
			session = HibernateUtil.openSession();
			List<NamaTugasKelompok> kelompoks = session.createCriteria(NamaTugasKelompok.class)
					.add(Restrictions.eq("tugasKelompok", tugas)).addOrder(Order.asc("nama")).list();
			if (kelompoks == null || kelompoks.isEmpty()) {
				return "";
			}
			totalKelompok = kelompoks.size();

			java.util.List<Long> kelompokIds = new java.util.ArrayList<Long>();
			java.util.Map<Long, String> namaKelompok = new java.util.HashMap<Long, String>();
			for (NamaTugasKelompok k : kelompoks) {
				if (k == null || k.getId() == null) {
					continue;
				}
				kelompokIds.add(k.getId());
				namaKelompok.put(k.getId(), k.getNama());
			}

			// (2) Status pengumpulan: SATU kueri distinct ref LampiranLain (hindari N+1).
			try {
				if (!kelompokIds.isEmpty()) {
					List<?> refs = session.createCriteria(LampiranLain.class)
							.add(Restrictions.eq("jenis", NamaTugasKelompok.class.getName()))
							.add(Restrictions.in("ref", kelompokIds))
							.setProjection(Projections.distinct(Projections.property("ref"))).list();
					if (refs != null) {
						java.util.Set<Long> setSudah = new java.util.HashSet<Long>();
						for (Object o : refs) {
							if (o instanceof Long) {
								setSudah.add((Long) o);
							} else if (o instanceof Number) {
								setSudah.add(((Number) o).longValue());
							}
						}
						kelompokSudah = setSudah.size();
					}
				}
			} catch (Exception eLamp) {
				Common.tampilErrorJikaAdmin(eLamp);
			}

			// (3) Anggota + nilai: SATU kueri gabungan, diakumulasi dalam loop tunggal.
			java.util.Map<Long, double[]> agg = new java.util.HashMap<Long, double[]>(); // id -> {sum, count}
			List<NamaTugasKelompokPunyaMahasiswa> rels = session
					.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
					.add(Restrictions.in("namaTugasKelompok", kelompoks)).list();
			if (rels != null) {
				for (NamaTugasKelompokPunyaMahasiswa r : rels) {
					if (r == null || r.getNamaTugasKelompok() == null || r.getNamaTugasKelompok().getId() == null) {
						continue;
					}
					if (r.getMahasiswa() == null && r.getSiswa() == null) {
						continue;
					}
					totalAnggota++;
					double nilai = r.getNilai() == null ? 0 : r.getNilai().doubleValue();
					if (nilai > 0) {
						totalDinilai++;
						sumNilai += nilai;
						if (maxNilai < 0 || nilai > maxNilai) {
							maxNilai = nilai;
						}
						if (minNilai < 0 || nilai < minNilai) {
							minNilai = nilai;
						}
						if (nilai < 60) {
							b0++;
						} else if (nilai < 70) {
							b60++;
						} else if (nilai < 80) {
							b70++;
						} else if (nilai < 90) {
							b80++;
						} else {
							b90++;
						}
						Long gid = r.getNamaTugasKelompok().getId();
						double[] a = agg.get(gid);
						if (a == null) {
							a = new double[] { 0, 0 };
							agg.put(gid, a);
						}
						a[0] += nilai;
						a[1] += 1;
					}
				}
			}

			double bestAvg = -1;
			for (Long gid : kelompokIds) {
				double[] a = agg.get(gid);
				if (a != null && a[1] > 0) {
					double avg = a[0] / a[1];
					String nm = namaKelompok.get(gid);
					avgPerKelompok.put(nm == null ? "-" : nm, avg);
					if (avg > bestAvg) {
						bestAvg = avg;
						namaTertinggi = nm;
					}
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return "";
		} finally {
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/TugasKelompokHelper.java:1278");
				}
				ais.common.ElearningSessionUtil.closeQuietly(session);
			}
		}

		int kelompokBelum = Math.max(0, totalKelompok - kelompokSudah);
		double rata = totalDinilai > 0 ? sumNilai / totalDinilai : 0;

		java.util.List<ais.ui.util.DashboardUiKit.Stat> stats = new java.util.ArrayList<ais.ui.util.DashboardUiKit.Stat>();
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Total Kelompok", String.valueOf(totalKelompok),
				"Jumlah kelompok pada tugas ini", "#2563eb"));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Total Anggota", String.valueOf(totalAnggota),
				"Seluruh peserta di semua kelompok", "#0891b2"));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Sudah Dinilai", totalDinilai + " / " + totalAnggota,
				"Anggota yang nilainya sudah diisi", "#16a34a"));
		stats.add(new ais.ui.util.DashboardUiKit.Stat("Rata-rata Nilai",
				totalDinilai > 0 ? Common.numberFormat.get().format(rata) : "-",
				"Nilai rata-rata anggota yang dinilai", "#f97316"));

		StringBuilder body = new StringBuilder();
		body.append(ais.ui.util.DashboardUiKit.cards(stats));
		body.append(ais.ui.util.DashboardUiKit.openGrid(300));

		java.util.LinkedHashMap<String, Double> statusMap = new java.util.LinkedHashMap<String, Double>();
		statusMap.put("Sudah mengumpulkan", (double) kelompokSudah);
		statusMap.put("Belum mengumpulkan", (double) kelompokBelum);
		body.append(ais.ui.util.DashboardUiKit.donut("Status Pengumpulan Kelompok",
				"Berapa kelompok yang sudah dan belum mengunggah berkas tugas.", statusMap, false,
				"Belum ada kelompok."));

		body.append(ais.ui.util.DashboardUiKit.barList("Rata-rata Nilai per Kelompok",
				"Perbandingan nilai rata-rata tiap kelompok; batang lebih panjang berarti nilai lebih tinggi.",
				avgPerKelompok, "#22c55e", "", false, "Belum ada nilai yang diisi."));

		java.util.LinkedHashMap<String, Double> distMap = new java.util.LinkedHashMap<String, Double>();
		distMap.put("90-100 (Sangat baik)", (double) b90);
		distMap.put("80-89 (Baik)", (double) b80);
		distMap.put("70-79 (Cukup)", (double) b70);
		distMap.put("60-69 (Kurang)", (double) b60);
		distMap.put("Di bawah 60 (Perlu perhatian)", (double) b0);
		body.append(ais.ui.util.DashboardUiKit.donut("Sebaran Nilai Anggota",
				"Pengelompokan anggota berdasarkan rentang nilai agar terlihat sebaran capaiannya.", distMap, false,
				"Belum ada nilai yang diisi."));

		String spider = bangunSpiderSubCpmk(tugas, perkuliahan);
		if (spider.length() > 0) {
			body.append(spider);
		}

		body.append(ais.ui.util.DashboardUiKit.closeGrid());

		java.util.LinkedHashMap<String, String> insight = new java.util.LinkedHashMap<String, String>();
		insight.put("Nilai Tertinggi", maxNilai < 0 ? "-" : Common.numberFormat.get().format(maxNilai));
		insight.put("Nilai Terendah", minNilai < 0 ? "-" : Common.numberFormat.get().format(minNilai));
		insight.put("Kelompok Terbaik", namaTertinggi == null || namaTertinggi.length() == 0 ? "-" : namaTertinggi);
		insight.put("Belum Dinilai", (totalAnggota - totalDinilai) + " anggota");
		body.append(ais.ui.util.DashboardUiKit.insight("Sorotan",
				"Ringkasan angka penting dari penilaian tugas kelompok ini.", insight));

		StringBuilder out = new StringBuilder();
		out.append("<details class='tk-analitik'><summary>&#128202; Analitik &amp; Ringkasan Nilai &mdash; rata-rata ")
				.append(totalDinilai > 0 ? Common.numberFormat.get().format(rata) : "belum ada")
				.append("</summary><div class='tk-analitik-body'>").append(body).append("</div></details>");
		return out.toString();
	}

	/**
	 * Membangun jaring laba-laba (radar SVG) perbandingan bobot Sub-CPMK untuk tugas kelompok pada
	 * kurikulum OBE. Bobot dibaca dari peta JSON {@code tugasKelompok.getFormatNilais()} lalu
	 * dinormalkan ke skala 0..100 relatif terhadap bobot terbesar sehingga bentuk jaring mudah
	 * dibandingkan. Mengembalikan string kosong bila bukan OBE, belum ada pemetaan, atau kurang dari
	 * tiga sumbu (radar minimal butuh tiga titik). Memakai {@code currentSession()} (dikelola kerangka
	 * kerja) untuk mengambil daftar {@link FormatNilai}; seluruh proses dijaga {@code try/catch} gaya
	 * 1.6 dan tidak pernah melempar.
	 *
	 * @param tugas       tugas kelompok sumber pemetaan bobot Sub-CPMK
	 * @param perkuliahan perkuliahan (untuk cek OBE dan sumber daftar komponen)
	 * @return potongan HTML kartu radar SVG, atau string kosong bila tidak berlaku
	 */
	private String bangunSpiderSubCpmk(TugasKelompok tugas, Perkuliahan perkuliahan) {
		try {
			if (perkuliahan == null || perkuliahan.getKurikulum() == null || !perkuliahan.getKurikulum()
					.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
				return "";
			}
			String json = tugas.getFormatNilais();
			if (json == null || json.trim().length() == 0) {
				return "";
			}
			JSONObject jo = new JSONObject(json);
			if (jo.length() < 3) {
				return "";
			}
			Session session = HibernateUtil.currentSession();
			List<FormatNilai> all = Common.getFormatNilais(session, perkuliahan);
			boolean adaCpmk = false;
			if (all != null) {
				for (FormatNilai f : all) {
					if (f != null && f.getNama() != null && f.getNama().toLowerCase().contains("cpmk")) {
						adaCpmk = true;
						break;
					}
				}
			}
			if (!adaCpmk) {
				all = Common.getFormatNilais(perkuliahan, true);
			}
			java.util.List<String> axes = new java.util.ArrayList<String>();
			java.util.List<Double> bobots = new java.util.ArrayList<Double>();
			double maxB = 0;
			if (all != null) {
				for (FormatNilai f : all) {
					if (f == null || f.getId() == null) {
						continue;
					}
					String key = f.getId().toString();
					if (!jo.isNull(key)) {
						double b = jo.getDouble(key);
						axes.add(f.getNama());
						bobots.add(b);
						if (b > maxB) {
							maxB = b;
						}
					}
				}
			}
			if (axes.size() < 3 || maxB <= 0) {
				return "";
			}
			String[] ax = axes.toArray(new String[axes.size()]);
			int[] vals = new int[bobots.size()];
			for (int i = 0; i < bobots.size(); i++) {
				vals[i] = (int) Math.round(bobots.get(i).doubleValue() / maxB * 100.0);
			}
			return ais.ui.util.DashboardUiKit.spider("Bobot Sub-CPMK",
					"Perbandingan bobot tiap Sub-CPMK yang dinilai pada tugas ini (persen relatif).", ax, vals);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return "";
		}
	}

	/** Gaya CSS untuk kartu ringkas Tugas Kelompok (responsif, aksen hijau). */
	private static final String GAYA_KARTU_TUGAS = "<style>"
			+ ".tk-side-col{min-width:0;width:100%;}"
			+ ".tk-kartu{border:1px solid #e5e7eb;border-radius:16px;overflow:hidden;background:#fff;"
			+ "box-shadow:0 10px 30px rgba(15,23,42,.08);margin-bottom:14px;}"
			+ ".tk-head{padding:15px 20px;background:linear-gradient(120deg,#166534,#22c55e);color:#fff;display:flex;"
			+ "justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;}"
			+ ".tk-head-l{display:flex;align-items:center;gap:12px;min-width:0;}"
			+ ".tk-head-ic{width:44px;height:44px;border-radius:12px;background:rgba(255,255,255,.18);display:flex;"
			+ "align-items:center;justify-content:center;font-size:20px;flex-shrink:0;}"
			+ ".tk-judul{font-size:16px;font-weight:800;line-height:1.25;word-break:break-word;}"
			+ ".tk-sub{font-size:12px;opacity:.92;margin-top:2px;}"
			+ ".tk-syarat{background:#fef9c3;color:#854d0e;font-size:11px;font-weight:700;padding:5px 12px;"
			+ "border-radius:999px;white-space:normal;}"
			+ ".tk-body{padding:18px 20px;display:grid;grid-template-columns:1fr;gap:18px;}"
			+ "@media(min-width:860px){.tk-body{grid-template-columns:1.25fr 1fr;}}"
			+ ".tk-mk{font-size:15px;font-weight:800;color:#166534;word-break:break-word;}"
			+ ".tk-mk-meta{font-size:12px;color:#64748b;margin:4px 0 14px;display:flex;flex-wrap:wrap;gap:6px;}"
			+ ".tk-dates{display:flex;flex-wrap:wrap;gap:12px;margin-bottom:16px;}"
			+ ".tk-date{flex:1 1 175px;border:1px solid #e5e7eb;border-radius:12px;padding:11px 14px;background:#fff;}"
			+ ".tk-date b{display:block;font-size:9.5px;letter-spacing:.04em;text-transform:uppercase;color:#64748b;margin-bottom:4px;}"
			+ ".tk-date-end{background:#fef2f2;border-color:#fecaca;}.tk-date-end b{color:#dc2626;}"
			+ ".tk-date-v{font-size:13px;font-weight:800;color:#0f172a;}.tk-date-end .tk-date-v{color:#dc2626;}"
			+ ".tk-date-j{font-size:11px;color:#64748b;}.tk-date-end .tk-date-j{color:#dc2626;opacity:.8;}"
			+ ".tk-sec-judul{font-size:10.5px;font-weight:800;text-transform:uppercase;letter-spacing:.04em;color:#64748b;margin:6px 0 8px;}"
			+ ".tk-grp{border:1px solid #eef2f7;border-radius:10px;margin-bottom:8px;overflow:hidden;background:#fff;"
			+ "box-shadow:0 2px 6px rgba(15,23,42,.04);}"
			+ ".tk-grp>summary{list-style:none;cursor:pointer;padding:11px 14px;display:flex;align-items:center;"
			+ "gap:10px;font-weight:700;color:#0f172a;font-size:13px;}"
			+ ".tk-grp>summary::-webkit-details-marker{display:none;}"
			+ ".tk-grp>summary:after{content:'\\25be';color:#94a3b8;font-size:12px;margin-left:4px;}"
			+ ".tk-grp[open]>summary:after{content:'\\25b4';}"
			+ ".tk-badge{background:#166534;color:#fff;font-size:11px;font-weight:800;padding:3px 11px;border-radius:999px;margin-left:auto;}"
			+ ".tk-grp-body{padding:6px 10px 10px;background:#f8fafc;}"
			+ ".tk-anggota{display:flex;align-items:center;gap:10px;padding:7px 8px;background:#fff;border-radius:8px;margin-bottom:5px;}"
			+ ".tk-foto{width:34px;height:34px;border-radius:50%;object-fit:cover;border:1px solid #e5e7eb;flex-shrink:0;}"
			+ ".tk-anggota-nama{font-size:12px;font-weight:700;color:#0f172a;}"
			+ ".tk-anggota-id{font-size:10.5px;color:#64748b;}"
			+ ".tk-nilai{background:#166534;color:#fff;font-size:11px;font-weight:800;padding:2px 10px;"
			+ "border-radius:999px;white-space:nowrap;margin-left:8px;flex-shrink:0;}"
			+ ".tk-cpmk-breakdown{margin-top:4px;display:flex;flex-wrap:wrap;gap:4px;}"
			+ ".tk-cpmk-chip{background:#ecfdf5;border:1px solid #a7f3d0;color:#065f46;font-size:9.5px;"
			+ "font-weight:600;padding:1px 6px;border-radius:6px;line-height:1.6;white-space:nowrap;}"
			+ ".tk-cpmk-chip b{color:#047857;font-weight:800;}"
			+ ".tk-empty{text-align:center;color:#94a3b8;font-size:11px;font-style:italic;padding:8px;}"
			+ ".tk-side{display:flex;flex-direction:column;gap:12px;}"
			+ ".tk-panel{border:1px solid #e5e7eb;border-radius:12px;padding:14px;background:#fff;}"
			+ ".tk-dosen-item{display:flex;align-items:center;gap:9px;margin-top:8px;}"
			+ ".tk-dosen-item span{font-size:12.5px;font-weight:700;color:#0f172a;}"
			+ ".tk-btn{display:block;text-align:center;text-decoration:none;font-size:12.5px;font-weight:800;"
			+ "padding:11px;border-radius:999px;}"
			+ ".tk-btn-peserta{border:1px solid #86efac;color:#166534;background:#f0fdf4;}"
			+ ".tk-alert{background:#fffbeb;border:1px solid #fde68a;color:#854d0e;font-size:12px;border-radius:10px;padding:10px 12px;}"
			+ ".tk-analitik{border:1px solid #d1fae5;border-radius:12px;background:#f8fafc;overflow:hidden;margin-top:4px;}"
			+ ".tk-analitik>summary{list-style:none;cursor:pointer;padding:11px 14px;font-weight:800;color:#166534;"
			+ "font-size:13px;background:#ecfdf5;}"
			+ ".tk-analitik>summary::-webkit-details-marker{display:none;}"
			+ ".tk-analitik>summary:after{content:'\\25be';float:right;color:#16a34a;}"
			+ ".tk-analitik[open]>summary:after{content:'\\25b4';}"
			+ ".tk-analitik-body{padding:14px;}"
			+ "</style>";

	/**
	 * Renderer lokal untuk layar/komponen {@link TugasKelompokHelper}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link TugasKelompokHelper} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code NamaTugasKelompokHelper
	 * namaTugasKelompokHelper}, {@code Calendar kemarin}; operasi lokal: {@code bukaDaftarKelompok()}, {@code
	 * pasangAksiPengaturan()}, {@code render}(). Aturan bisnis bersama tetap berada pada kelas induk atau service
	 * yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see TugasKelompokHelper
	 */
	class DetailPerkuliahanRenderer extends ais.ui.util.MyRowRenderer {

		/**
		 * Helper daftar kelompok, dibuat sekali per renderer dengan meneruskan identitas pelajar milik kelas
		 * induk agar hak akses di dalamnya ikut menyesuaikan.
		 *
		 * <p><b>Catatan pemeliharaan:</b> pada revisi ini field tersebut <b>tidak lagi dibaca</b> oleh kode
		 * mana pun. {@link #bukaDaftarKelompok} sengaja membuat instance {@code NamaTugasKelompokHelper}
		 * yang baru setiap kali jendela dibuka, karena helper daftar kelompok menyimpan komponen ZK miliknya
		 * sendiri: memakai ulang satu instance untuk jendela yang berbeda akan membuat komponen jendela lama
		 * yang sudah dilepas ikut terbawa. Field ini tetap dipertahankan sebagai jejak rancangan awal; ia
		 * hanya menyisakan biaya satu objek per baris grid dan tidak memengaruhi perilaku apa pun.</p>
		 */
		private NamaTugasKelompokHelper namaTugasKelompokHelper = new NamaTugasKelompokHelper(mahasiswa,
				biodataCalonMahasiswa);

		/**
		 * Penanda waktu acuan untuk menilai apakah sebuah tugas sudah dibuka atau sudah ditutup, diambil
		 * lewat {@code WaktuUtil.getCalendar()} sehingga mengikuti zona waktu dan penyesuaian waktu server
		 * yang dipakai seluruh aplikasi.
		 *
		 * <p><b>Namanya menyesatkan:</b> meskipun disebut "kemarin", isinya adalah waktu <b>saat ini</b>
		 * ketika renderer dibuat, bukan hari sebelumnya. Di {@link #render(Row, Object)} nilai ini dibandingkan
		 * dengan {@code tugasKelompok.getMulai()} dan {@code getSelesai()} untuk memilih salah satu dari tiga
		 * keadaan: "belum mulai", "telah selesai", atau tampilan kontrol penuh.</p>
		 *
		 * <p>Karena diambil sekali saat renderer dibuat, seluruh baris pada satu kali penggambaran memakai
		 * acuan waktu yang SAMA. Itu justru diinginkan: daftar menjadi konsisten, tidak ada baris yang
		 * dinilai dengan waktu berbeda beberapa milidetik. Konsekuensinya, halaman yang dibiarkan terbuka
		 * lama tidak akan otomatis berpindah dari "sedang berlangsung" ke "telah selesai" sampai daftar
		 * dimuat ulang &mdash; perilaku yang wajar untuk tampilan berbasis muat-ulang seperti ini.</p>
		 */
		private Calendar kemarin = ais.ui.util.WaktuUtil.getCalendar();

		/**
		 * <h3>Jendela "Kelola Kelompok" &mdash; daftar kelompok beserta anggotanya</h3>
		 *
		 * <p><b>Untuk apa (bahasa sederhana):</b> membuka jendela terpisah berisi seluruh kelompok pada satu
		 * tugas kelompok, lengkap dengan anggota tiap kelompok dan berkas yang mereka kumpulkan. Jendela ini
		 * dipakai oleh DUA peran dengan tujuan berbeda: dosen/guru memakainya untuk membuat kelompok,
		 * memindahkan anggota, dan memeriksa pengumpulan; sedangkan mahasiswa/siswa memakainya untuk melihat
		 * kelompok yang ada, bergabung ke salah satunya, dan mengunggah berkas kelompoknya.</p>
		 *
		 * <p><b>Mengapa daftar kelompok dipindah ke jendela, bukan ditanam di kartu.</b> Sebelumnya daftar
		 * kelompok digambar langsung di dalam baris grid, sehingga satu tugas dengan banyak kelompok membuat
		 * baris menjadi sangat panjang dan daftar tugas sulit dibaca. Dengan memindahkannya ke jendela
		 * modal, kartu ringkas tetap pendek dan seluruh ruang jendela dapat dipakai untuk tabel anggota.</p>
		 *
		 * <h4>Dua pintu masuk, satu badan</h4>
		 * <p>Parameter {@code langsungTambah} membedakan dua tombol yang bermuara ke metode yang sama:</p>
		 * <ul>
		 *   <li>{@code false} &mdash; tombol "Kelola Kelompok &amp; Anggota": jendela dibuka apa adanya pada
		 *   daftar kelompok.</li>
		 *   <li>{@code true} &mdash; tombol "Tambah Kelompok": setelah jendela terpasang, formulir tambah
		 *   kelompok langsung dibuka lewat {@code helper.onAdd(null, new NamaTugasKelompok())} sehingga
		 *   pengguna tidak perlu menekan tombol tambah sekali lagi. Setelah formulir itu disimpan, pengguna
		 *   kembali ke daftar kelompok untuk melanjutkan pengaturan anggota.</li>
		 * </ul>
		 * <p>Urutan pemanggilannya penting: {@code win.setVisible(true)} dijalankan LEBIH DULU, baru
		 * {@code onAdd(...)}, dan {@code win.onModal()} paling akhir. Formulir tambah hanya dapat menempel
		 * pada jendela yang sudah berada di pohon komponen, dan pemanggilan modal harus menjadi langkah
		 * terakhir karena ia menahan alur sampai jendela ditutup.</p>
		 *
		 * <h4>Hak akses: sengaja tidak dijaga di sini</h4>
		 * <p>Metode ini TIDAK memeriksa peran pengguna, dan itu disengaja karena jendelanya memang untuk
		 * semua peran. Pembatasan dikerjakan pada dua lapis lain: (1) tombol pemanggilnya &mdash; varian
		 * "Tambah Kelompok" hanya dipasang oleh {@link #pasangAksiPengaturan} yang sudah dijaga
		 * {@link TugasKelompokHelper#bolehKelola(Tbmuser)}; dan (2) di dalam {@code NamaTugasKelompokHelper},
		 * yang menyembunyikan sendiri kontrol tambah/unggah massal bagi pelajar. Identitas pelajar
		 * ({@code mahasiswa}, {@code biodataCalonMahasiswa}) diteruskan ke helper tersebut justru agar
		 * pembatasan lapis kedua itu dapat bekerja.</p>
		 *
		 * <p><b>Instance baru setiap kali dibuka.</b> Helper daftar kelompok dibuat baru di dalam metode ini,
		 * bukan memakai field {@code namaTugasKelompokHelper} milik renderer, karena helper tersebut
		 * menyimpan rujukan ke komponen ZK jendela; memakai ulang instance lama akan menautkan jendela baru
		 * ke komponen jendela lama yang sudah dilepas.</p>
		 *
		 * <p><b>Tampilan responsif.</b> Di ponsel jendela dibuat memenuhi layar (100% &times; 100%), di
		 * desktop 95% &times; 95% agar tetap menyisakan bingkai. Tata letak {@link Borderlayout} memisahkan
		 * isi yang dapat digulir (tengah) dari tombol Tutup (bawah), sehingga daftar panjang tetap nyaman
		 * dibaca. Tombol Tutup hanya melepas jendela ({@code detach}); tidak ada penyimpanan yang tertunda
		 * karena seluruh perubahan di dalamnya sudah disimpan oleh {@code NamaTugasKelompokHelper} sendiri.</p>
		 *
		 * @param tugasKelompok  tugas kelompok yang daftar kelompoknya hendak dikelola
		 * @param syaratAlert    kumpulan pesan syarat pengumpulan yang belum terpenuhi, dikumpulkan lebih dulu
		 *                       oleh {@code Tugas.tampilanSyarat}/{@code tampilanSyaratReadonly} di
		 *                       {@link #render(Row, Object)} dan diteruskan agar peringatan yang sama muncul
		 *                       di dalam jendela &mdash; tidak dihitung ulang supaya konsisten dan hemat kueri
		 * @param langsungTambah {@code true} untuk langsung membuka formulir tambah kelompok setelah jendela
		 *                       tampil; {@code false} untuk berhenti pada daftar kelompok
		 * @throws Exception bila jendela gagal dipasang atau formulir tambah gagal dibangun
		 * @see ais.database.model.NamaTugasKelompok
		 */
		private void bukaDaftarKelompok(final TugasKelompok tugasKelompok, final Set<String> syaratAlert,
				boolean langsungTambah) throws Exception {
			final MyWindow win = new MyWindow();
			win.setTitle("Kelola Kelompok - "
					+ (tugasKelompok.getJudul() == null ? "" : tugasKelompok.getJudul()));
			if (Common.isMobile()) {
				win.setWidth("100%");
				win.setHeight("100%");
			} else {
				win.setWidth("95%");
				win.setHeight("95%");
			}
			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(win);

			Borderlayout bl = new ais.ui.util.MyBorderlayout();
			Center c2 = new Center();
			c2.setParent(bl);
			ais.ui.util.ZkCompat.setFlex(c2, true);
			ais.ui.util.MyGroupboxStyled isiWin = new ais.ui.util.MyGroupboxStyled();
			isiWin.setStyle("padding:8px;width:100%;");
			isiWin.setParent(c2);

			NamaTugasKelompokHelper helper = new NamaTugasKelompokHelper(mahasiswa,
					biodataCalonMahasiswa);
			helper.display(tugasKelompok, isiWin, syaratAlert);

			South south2 = new South();
			ais.ui.util.ZkCompat.setFlex(south2, true);
			south2.setParent(bl);
			Toolbar tb2 = new Toolbar();
			tb2.setParent(south2);
			MyToolbarbutton tutup = new MyToolbarbutton("fa-times", "Tutup");
			tutup.setTooltiptext("Tutup jendela Kelola Kelompok");
			tutup.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event ev) throws Exception {
					win.detach();
				}
			});
			tutup.setParent(tb2);
			bl.setParent(win);
			win.setVisible(true);

			// Untuk tombol cepat, form tambah dibuka langsung. Sesudah disimpan pengguna
			// kembali ke daftar kelompok untuk melanjutkan pengaturan anggota.
			if (langsungTambah) {
				helper.onAdd(null, new NamaTugasKelompok());
			}
			win.onModal();
		}

		/**
		 * Satu jalur konfirmasi untuk seluruh tombol hapus tugas kelompok. Panel
		 * pengaturan kartu dan toolbar lama wajib memakai handler yang sama agar
		 * validasi, pemuatan ulang, serta pesan kegagalannya tidak berbeda.
		 */
		private void konfirmasiHapusTugasKelompok(final TugasKelompok tugasKelompok) throws Exception {
			MyMessageboxConfig.show("Apakah yakin ingin menghapus tugas kelompok \""
					+ tugasKelompok.getJudul() + "\"?", "Hapus Tugas Kelompok",
					MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
					new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							int pilihan = Integer.parseInt(event.getData().toString());
							if (pilihan != MyMessageboxConfig.OK) {
								return;
							}
							try {
								Common.refreshDelete(tugasKelompok);
								loadData(null);
								if (eventListener != null) {
									eventListener.onEvent(event);
								}
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
								PesanFormalHelper.tampilkanGagalException("menghapus data tugas kelompok", e,
										new String[] {
												"Pastikan tidak ada nilai atau data lain yang masih berelasi dengan data ini.",
												"Muat ulang (refresh) halaman ini lalu coba hapus kembali.",
												"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
										});
							}
						}
					});
		}

		/**
		 * <h3>Panel "Pengaturan Tugas Kelompok" &mdash; empat aksi pengelola dalam satu tempat</h3>
		 *
		 * <p><b>Untuk apa (bahasa sederhana):</b> menempelkan sebuah kotak hijau kecil di kolom kanan kartu
		 * tugas, berisi empat tombol yang paling sering dipakai dosen/guru untuk mengurus sebuah tugas
		 * kelompok: mengubah judul &amp; instruksinya, membuat kelompok baru, mengatur kelompok beserta
		 * anggotanya, dan menghapus tugas tersebut. Sebelumnya tombol-tombol ini tersebar di beberapa
		 * tempat; dikumpulkan di sini agar pengguna tidak perlu mencari-cari.</p>
		 *
		 * <h4>Gerbang hak akses: menolak lebih awal, bukan menyembunyikan</h4>
		 * <p>Baris pertama metode ini adalah penjagaan tunggal:</p>
		 * <pre>if (!bolehKelola(Common.getCurrentUser())) { return; }</pre>
		 * <p>Bila pengguna bukan pengelola, metode langsung berhenti dan <b>tidak satu pun komponen dibuat</b>.
		 * Ini lebih kuat daripada membuat tombol lalu menyetel {@code setVisible(false)}: komponen yang
		 * tersembunyi tetap ada di pohon ZK dan tetap memiliki listener yang terdaftar, sehingga masih bisa
		 * menerima event yang dikirim dari sisi klien. Karena tidak pernah dibuat, tombol Ubah, Tambah
		 * Kelompok, Kelola Kelompok, dan Hapus di panel ini tidak dapat dipicu sama sekali oleh pelajar.</p>
		 *
		 * <p>Penjagaan memakai {@link TugasKelompokHelper#bolehKelola(Tbmuser)}, yaitu aturan terpusat yang
		 * menuntut DUA syarat sekaligus: layar tidak dibuka dalam konteks pelajar
		 * ({@link TugasKelompokHelper#konteksPelajar()}) DAN pengguna yang login bukan pelajar/peserta
		 * ({@link TugasKelompokHelper#loginPelajar(Tbmuser)}, yang juga mencakup calon siswa, calon
		 * mahasiswa, dan peserta kursus, serta menganggap sesi tanpa pengguna sebagai bukan pengelola).</p>
		 *
		 * <p><b>Batas yang jujur.</b> Penjagaan ini bersifat <i>per tampilan</i>, bukan per data. Ia
		 * memastikan hanya pengelola yang mendapat tombol, tetapi tidak memeriksa apakah pengelola tersebut
		 * berhak atas tugas kelompok INI &mdash; misalnya apakah ia benar dosen pengampu perkuliahan yang
		 * bersangkutan. Pemeriksaan kepemilikan semacam itu, bila diperlukan, harus ditambahkan di dalam
		 * aksi yang dipanggil ({@link TugasKelompokHelper#onAdd}, {@link #konfirmasiHapusTugasKelompok}),
		 * bukan di sini &mdash; sebab hanya aksi itulah yang benar-benar menyentuh basis data.</p>
		 *
		 * <h4>Keempat tombol dan tujuannya</h4>
		 * <ol>
		 *   <li><b>Ubah Judul &amp; Instruksi</b> &rarr; {@link TugasKelompokHelper#onAdd(Event, TugasKelompok)}
		 *   dengan tugas yang sedang dibuka, sehingga formulir terbuka dalam mode ubah (bukan tambah baru).</li>
		 *   <li><b>Tambah Kelompok</b> &rarr; {@link #bukaDaftarKelompok} dengan {@code langsungTambah=true},
		 *   yaitu jalan pintas yang langsung membuka formulir kelompok baru.</li>
		 *   <li><b>Kelola Kelompok &amp; Anggota</b> &rarr; {@link #bukaDaftarKelompok} dengan
		 *   {@code langsungTambah=false}, berhenti pada daftar kelompok.</li>
		 *   <li><b>Hapus Tugas Kelompok</b> &rarr; {@link #konfirmasiHapusTugasKelompok}, satu-satunya jalur
		 *   penghapusan sehingga konfirmasi, pemuatan ulang, dan pesan kegagalannya selalu seragam.</li>
		 * </ol>
		 *
		 * <p>Seluruh tombol dibuat selebar 100% dengan teks rata kiri agar terbaca sebagai daftar menu
		 * vertikal, dan tombol Hapus diberi warna merah sebagai penanda aksi merusak. Setiap tombol memiliki
		 * {@code tooltiptext} yang menjelaskan akibatnya dalam bahasa awam.</p>
		 *
		 * @param parent        wadah kolom kanan kartu tempat panel ditempelkan; panel disisipkan sebagai
		 *                      anak baru sehingga kontrol lain yang sudah ada di kolom itu tetap utuh
		 * @param tugasKelompok tugas kelompok yang menjadi sasaran keempat aksi
		 * @param syaratAlert   kumpulan peringatan syarat pengumpulan yang sudah dihitung di
		 *                      {@link #render(Row, Object)}, diteruskan apa adanya ke jendela kelola kelompok
		 *                      agar tidak dihitung ulang
		 * @see TugasKelompokHelper#bolehKelola(Tbmuser)
		 */
		private void pasangAksiPengaturan(final Vbox parent, final TugasKelompok tugasKelompok,
				final Set<String> syaratAlert) {
			if (!bolehKelola(Common.getCurrentUser())) {
				return;
			}

			Vbox aksi = new Vbox();
			aksi.setWidth("100%");
			aksi.setSpacing("4px");
			aksi.setStyle("box-sizing:border-box;background:#f0fdf4;border:1px solid #bbf7d0;"
					+ "border-radius:10px;padding:8px;margin:2px 0 8px;");
			aksi.setParent(parent);

			Label judulAksi = new Label("Pengaturan Tugas Kelompok");
			judulAksi.setStyle("font-size:12px;font-weight:bold;color:#166534;margin-bottom:2px;");
			judulAksi.setParent(aksi);

			MyToolbarbutton ubah = new MyToolbarbutton("fa-pencil", "Ubah Judul & Instruksi");
			ubah.setWidth("100%");
			ubah.setStyle("text-align:left;");
			ubah.setTooltiptext("Ubah judul, instruksi, jadwal, dan pengaturan tugas kelompok");
			ubah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(event, tugasKelompok);
				}
			});
			ubah.setParent(aksi);

			MyToolbarbutton tambah = new MyToolbarbutton("fa-plus", "Tambah Kelompok");
			tambah.setWidth("100%");
			tambah.setStyle("text-align:left;");
			tambah.setTooltiptext("Buat kelompok baru lalu atur anggotanya");
			tambah.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaDaftarKelompok(tugasKelompok, syaratAlert, true);
				}
			});
			tambah.setParent(aksi);

			MyToolbarbutton kelola = new MyToolbarbutton("fa-users", "Kelola Kelompok & Anggota");
			kelola.setWidth("100%");
			kelola.setStyle("text-align:left;");
			kelola.setTooltiptext("Lihat kelompok serta tambah atau hapus anggotanya");
			kelola.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaDaftarKelompok(tugasKelompok, syaratAlert, false);
				}
			});
			kelola.setParent(aksi);

			MyToolbarbutton hapus = new MyToolbarbutton("fa-trash", "Hapus Tugas Kelompok");
			hapus.setWidth("100%");
			hapus.setStyle("text-align:left;color:#b91c1c;");
			hapus.setTooltiptext("Hapus tugas kelompok ini secara permanen");
			hapus.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiHapusTugasKelompok(tugasKelompok);
				}
			});
			hapus.setParent(aksi);
		}

		/**
		 * <h3>Penggambar satu baris daftar &mdash; kartu lengkap sebuah Tugas Kelompok</h3>
		 *
		 * <p><b>Untuk apa (bahasa sederhana):</b> mengubah satu data tugas kelompok menjadi satu "kartu"
		 * utuh di layar. Kartu ini bukan sekadar sebaris teks: di dalamnya ada judul dan instruksi tugas,
		 * jadwal mulai dan batas akhir, daftar kelompok beserta anggotanya, dosen pengampu, tempat mengunggah
		 * dan mengunduh berkas, pengaturan bagaimana nilai tugas ini masuk ke nilai akhir, tombol-tombol
		 * pengelolaan, sampai ringkasan grafik penilaian. Metode inilah yang merakit semuanya, sehingga
		 * hampir seluruh isi layar Tugas Kelompok berasal dari sini.</p>
		 *
		 * <p>Karena satu metode merakit begitu banyak hal, urutan bacanya penting. Secara garis besar
		 * badannya berjalan dalam tujuh tahap berikut.</p>
		 *
		 * <h4>Tahap 1 &mdash; Syarat pengumpulan</h4>
		 * <p>Bila tugas menempel pada sebuah {@link Pertemuan}, daftar syarat pengumpulan disiapkan lebih
		 * dulu ke dalam {@code rowsSyarat} dan pesan syarat yang belum terpenuhi dikumpulkan ke
		 * {@code syaratAlert}. Kumpulan pesan itu kemudian diteruskan apa adanya ke
		 * {@link #bukaDaftarKelompok} dan {@link #pasangAksiPengaturan}, sehingga peringatan yang sama tidak
		 * perlu dihitung ulang di tiap jendela. Pengelola mendapat versi yang dapat diubah
		 * ({@code Tugas.tampilanSyarat}), pelajar mendapat versi baca-saja
		 * ({@code Tugas.tampilanSyaratReadonly}) ditambah {@code Tugas.tampilanLain}. Perhatikan bahwa
		 * pemilihan cabang di sini memakai pemeriksaan peran gaya lama yang menguji field helper dan
		 * beberapa peran pada objek pengguna satu per satu, BUKAN
		 * {@link TugasKelompokHelper#bolehKelola(Tbmuser)}; keduanya tidak selalu memberi hasil yang sama
		 * (lihat catatan "Ketidakseragaman gerbang peran" di bawah).</p>
		 *
		 * <h4>Tahap 2 &mdash; Kartu ringkas</h4>
		 * <p>{@link TugasKelompokHelper#buatKartuRingkasTugasKelompok} mengembalikan HTML dalam tiga
		 * potongan: header selebar penuh, kolom kiri (mata kuliah, jadwal, daftar kelompok+anggota), dan
		 * bagian atas kolom kanan (dosen pengampu + tombol peserta). Seluruh kontrol ZK berikutnya
		 * ditempelkan ke dalam {@code vbox} yang menjadi kolom KANAN, tepat di bawah tombol peserta &mdash;
		 * dengan begitu kontrol berada DI DALAM kartu dan mengisi ruang kosong, bukan terlempar jauh ke
		 * bawah. Judul lama ({@code MyCaptionStyled}) disembunyikan karena sudah diwakili header kartu.
		 * Seluruh perakitan kartu dibungkus {@code try/catch}: bila gagal, {@code vbox} tetap ditempelkan
		 * ke {@code Groupbox} induk sebagai jalur cadangan sehingga kontrol lama tidak ikut hilang. Prinsip
		 * ini berlaku di sepanjang metode: hiasan boleh gagal, fungsi tidak boleh ikut gagal.</p>
		 *
		 * <h4>Tahap 3 &mdash; Gerbang waktu: tiga keadaan yang saling meniadakan</h4>
		 * <p>Dengan acuan {@code kemarin} (yang sesungguhnya berisi waktu sekarang), tugas berada di salah
		 * satu dari tiga keadaan: <b>belum mulai</b> (waktu sekarang masih sebelum {@code getMulai()}),
		 * <b>telah selesai</b> (sudah melewati {@code getSelesai()}), atau <b>sedang berjalan</b>. Dua
		 * keadaan pertama hanya menampilkan pesan berikut ikon jam, dan seluruh kontrol pengumpulan maupun
		 * penilaian TIDAK dibangun sama sekali. Ini penting dipahami: batas waktu tugas ditegakkan dengan
		 * cara tidak membuat komponennya, bukan dengan menonaktifkannya.</p>
		 *
		 * <h4>Tahap 4 &mdash; Isi tugas &amp; berkas (hanya saat sedang berjalan)</h4>
		 * <p>Menampilkan rentang waktu, instruksi tugas, syarat pengumpulan bila ada, dan &mdash; pada mode
		 * rinci ({@code tampilRinci}) &mdash; dosen pengampu, riwayat revisi, prasyarat mata kuliah, serta
		 * jadwal hari/jam/ruangan. Kotak unggah/unduh dibangun oleh
		 * {@code LampiranLain.createDownloadUploadFileLain}; argumen terakhirnya yang bertipe {@code boolean}
		 * menentukan apakah pengguna boleh mengubah lampiran instruksi.</p>
		 *
		 * <h4>Tahap 5 &mdash; Penilaian: dua dunia yang terpisah</h4>
		 * <p>Ini bagian terbesar, dan bercabang menurut asal tugas:</p>
		 * <ul>
		 *   <li><b>Perkuliahan</b> ({@code getPerkuliahan() != null}). Pelajar hanya melihat label komponen
		 *   nilai berikut bobotnya. Pengelola mendapat pemilih {@link FormatNilai} + kotak bobot yang
		 *   langsung tersimpan saat diubah, dan bercabang lagi menurut kurikulum:
		 *   <ul>
		 *     <li><i>Kurikulum OBE</i> &mdash; pemilih format nilai tunggal beserta bobot manual
		 *     DISEMBUNYIKAN, karena penilaian OBE memakai pemetaan Sub-CPMK. Yang tampil adalah tombol
		 *     "Masukkan Nilai" (memanggil {@code GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe} di
		 *     dalam timer agar tidak menahan permintaan) dan grid Sub-CPMK BACA-SAJA. Bobot Sub-CPMK
		 *     sengaja hanya dapat diubah dari formulir "Instruksi Tugas Kelompok", yaitu
		 *     {@code bangunGridSubCpmk(..., editable=true)} di {@link TugasKelompokHelper#init}.</li>
		 *     <li><i>Non-OBE</i> &mdash; pemilih format nilai, bobot, tombol "Masukkan Nilai", dan tombol
		 *     "Format Nilai". Saat pilihan diganti, {@code bersihkanFormatNilaiYatim} dijalankan lebih dulu
		 *     untuk membersihkan rujukan menggantung, baru pilihan barunya disimpan.</li>
		 *   </ul>
		 *   Bila perkuliahan sudah dikunci ({@code getDikunci() != null}), pemilih dan kotak bobot
		 *   dinonaktifkan dan keterangan "Penilaian sudah dikunci" ditampilkan.</li>
		 *   <li><b>Sekolah</b> ({@code jadwalPelajaran != null}). Memakai rantai penilaian sekolah:
		 *   {@code JenisPenilaian} &rarr; {@code GrupPenilaian} &rarr; {@code GrupKategoriItemPenilaianSiswa}
		 *   &rarr; {@code JenisItemPenilaianSiswa}. Jenis penilaian diambil dari mata pelajaran, tetapi
		 *   ditimpa oleh kurikulum sekolah bila kurikulum menetapkannya. Daftar disaring per tingkat kelas:
		 *   grup atau kategori yang ditandai "khusus tingkat" dilewati bila tingkatnya tidak cocok. Hanya
		 *   item bertipe ANGKA atau TEXT_ANGKA yang ditawarkan, karena nilai tugas berupa angka. Grup
		 *   kategori dan grup penilaian ikut disimpan sebagai atribut {@code Comboitem} sehingga ketiga
		 *   rujukan tersimpan sekaligus saat pilihan berubah.</li>
		 * </ul>
		 * <p><b>Perangkap yang perlu diketahui:</b> pada cabang sekolah terdapat {@code return} di tengah
		 * loop ketika sebuah grup penilaian tidak memiliki grup kategori. Yang dihentikan bukan hanya loop,
		 * melainkan SELURUH penggambaran baris &mdash; sisa kartu (jumlah kelompok, toolbar, blok
		 * pengecualian peserta) tidak ikut dibangun untuk baris itu.</p>
		 *
		 * <h4>Tahap 6 &mdash; Toolbar aksi</h4>
		 * <p>Berisi lima tombol: <b>Kelola Nilai</b> ({@link TugasKelompokHelper#bukaKelolaNilai}),
		 * <b>Download Nilai</b> dan <b>Upload Nilai</b> (entri massal lewat berkas Excel), <b>Kelola
		 * Kelompok</b>, dan <b>Hapus Tugas Kelompok</b>. Berkas Excel menyesuaikan mode penilaian: kolom
		 * tunggal "Nilai" untuk non-OBE, atau satu kolom per Sub-CPMK untuk OBE, selalu diakhiri kolom
		 * "Keterangan". Kolom "ID" pada berkas adalah id baris keanggotaan
		 * ({@link NamaTugasKelompokPunyaMahasiswa}); saat diunggah, hanya id yang memang milik tugas ini yang
		 * diproses (baris lain diabaikan diam-diam), sehingga berkas hasil unduhan tugas lain tidak dapat
		 * dipakai untuk menulis nilai ke tugas ini. Nilai OBE ditulis ke blob JSON
		 * {@code TugasKelompok.keteranganNilai} sekali di akhir, sedangkan keterangan dan nilai non-OBE
		 * ditulis per baris keanggotaan.</p>
		 * <p>Kedua fitur Excel memuat anggota dengan satu kueri PER KELOMPOK (pola N+1). Untuk jumlah
		 * kelompok yang wajar hal ini tidak terasa, tetapi berbeda dari
		 * {@code buatKartuRingkasTugasKelompok} dan {@code bukaKelolaNilai} yang sengaja memakai
		 * {@code Restrictions.in} agar cukup satu kueri &mdash; perbedaan yang layak diseragamkan bila kelak
		 * ada keluhan lambat.</p>
		 *
		 * <h4>Tahap 7 &mdash; Pengecualian peserta</h4>
		 * <p>Tombol "Tampilkan peserta yang tidak perlu mengumpulkan tugas kelompok" membuka grid berisi
		 * seluruh peserta kelas dengan kotak centang per orang, ditambah satu kotak centang massal di kepala
		 * kolom. Keanggotaan daftar pengecualian disimpan sebagai teks CSV berpagar koma pada
		 * {@code TugasKelompok.mhsYgTidakIkut} dan diubah lewat
		 * {@code GradingHelper.ubahIdPadaCsvBerpagarKoma}, yang membandingkan token secara utuh sehingga id
		 * yang kebetulan menjadi bagian dari id lain tidak ikut terhapus. Kotak centang massal menerapkan
		 * perubahan pada seluruh peserta yang sedang lolos saringan pencarian, lalu menyimpan SEKALI di
		 * akhir. Kotak centang dinonaktifkan untuk baris non-mahasiswa karena daftar ini hanya menampung id
		 * mahasiswa.</p>
		 *
		 * <h4>Ketidakseragaman gerbang peran (fakta yang perlu diketahui pemelihara)</h4>
		 * <p>Metode ini memakai TIGA bentuk pemeriksaan peran yang berbeda, dan ketiganya tidak setara:</p>
		 * <ol>
		 *   <li>{@link TugasKelompokHelper#bolehKelola(Tbmuser)} &mdash; bentuk terpusat dan paling ketat,
		 *   dipakai tombol Kelola Nilai, Download/Upload Nilai, Hapus, dan dashboard analitik.</li>
		 *   <li>Pemeriksaan enam suku gaya lama yang menguji dua field helper ditambah empat peran pada
		 *   objek pengguna &mdash; namun <b>tidak menguji {@code tbmuser.getMahasiswa()}</b>, dan tidak
		 *   menguji field {@code siswa}/{@code calonSiswa}. Dipakai antara lain untuk memilih syarat yang
		 *   dapat diubah versus baca-saja, hak unggah lampiran instruksi, dan pemetaan Sub-CPMK.</li>
		 *   <li>Pemeriksaan empat suku yang HANYA menguji field helper dan sama sekali tidak melihat
		 *   pengguna yang login &mdash; bentuk paling longgar, justru yang menjaga tombol "Anggap Hadir
		 *   (Pengumpul)" dan "Tdk Upload = Alpa" yang menulis absensi seluruh peserta kelas.</li>
		 * </ol>
		 * <p>Karena helper yang dibangun lewat konstruktor sekolah selalu meninggalkan field jalur perguruan
		 * tinggi bernilai {@code null}, bentuk (2) dan (3) dapat menilai seorang pelajar sebagai pengelola
		 * pada jalur tersebut. Seluruh pemeriksaan peran di sini juga bersifat <i>per tampilan</i>, bukan per
		 * data: tidak ada satu pun yang menanyakan apakah pengguna berhak atas perkuliahan/kelas TUGAS INI.
		 * Bentuk (1) adalah yang seharusnya dipakai di semua tempat.</p>
		 *
		 * <p><b>Catatan {@code null}:</b> beberapa cabang memanggil {@code tbmuser.getPesertaKursus()} dan
		 * sejenisnya tanpa penjagaan {@code null} lebih dulu, sehingga sesi tanpa pengguna akan melempar
		 * {@code NullPointerException} alih-alih diperlakukan sebagai bukan pengelola. {@code bolehKelola}
		 * sudah menangani hal ini dengan benar lewat {@code loginPelajar(null)}.</p>
		 *
		 * @param rowUtama baris grid yang harus diisi komponen; disetel rata atas dan menjadi induk seluruh
		 *                 komponen yang dirakit di sini
		 * @param data     objek {@link TugasKelompok} untuk baris ini; di-<i>cast</i> tanpa pemeriksaan tipe
		 *                 karena model grid selalu diisi {@code TugasKelompok} oleh
		 *                 {@link TugasKelompokHelper#loadData(Object)} dan
		 *                 {@link TugasKelompokHelper#tampilanTugas}
		 * @throws Exception bila perakitan komponen atau pembacaan data gagal; ZK akan menampilkannya sebagai
		 *                   galat penggambaran baris
		 * @see TugasKelompokHelper#buatKartuRingkasTugasKelompok(TugasKelompok)
		 * @see TugasKelompokHelper#bukaKelolaNilai(TugasKelompok)
		 * @see #pasangAksiPengaturan(Vbox, TugasKelompok, Set)
		 */
		@SuppressWarnings({ "unchecked" })
		@Override
		public void render(final Row rowUtama, Object data) throws Exception {
			final TugasKelompok tugasKelompok = (TugasKelompok) data;
			rowUtama.setValign("top");
			Pertemuan pertemuan = tugasKelompok.ambilPertemuan();
			final Set<String> syaratAlert = new HashSet<String>();
			Rows rowsSyarat = new Rows();

			if (pertemuan != null) {

				MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

				Tbmuser tbmuser = Common.getCurrentUser();
				if (bolehKelola(tbmuser)) {
					Tugas.tampilanSyarat(pertemuan, tugasKelompok, null, null, null, null, rowsSyarat, syaratAlert,
							button);
				} else {
					Tugas.tampilanSyaratReadonly(pertemuan, tugasKelompok, null, null, null, null, rowsSyarat,
							syaratAlert, button);

					Tugas.tampilanLain(pertemuan, tugasKelompok, null, null, null, null, rowsSyarat, button);
				}
			}

			if (!tugasKelompok.getJudultugas().isEmpty()) {
				tugasKelompok.masukkanData("tugas");
			}

			final Groupbox gb = new ais.ui.util.MyGroupboxStyled();
			gb.setWidth("90%");
			MyCaptionStyled jd;
			gb.appendChild(jd = new MyCaptionStyled(tugasKelompok.getJudul()));
			jd.setStyle(
					"font-size:12px;font-weight: bolder;text-decoration: none;color:black;border: 1px solid black;\r\n"
							+ "  padding: 5px;" + "  background-color: rgba(169,169,169,0.4);"
							+ "  border-radius: 5px 15px;");
			gb.setParent(rowUtama);
			gb.setStyle("min-width: 350px;");

			// KARTU RINGKAS (responsif): header lebar-penuh + body 2 kolom.
			//  - Kolom KIRI  : info mata kuliah, jadwal Mulai/Batas Akhir, daftar kelompok+anggota (HTML).
			//  - Kolom KANAN : dosen pengampu + tombol peserta, LALU seluruh KONTROL tugas (unggah/unduh,
			//    syarat, nilai, tombol) sebagai komponen ZK — sehingga kontrol berada DI DALAM kartu, tepat
			//    di bawah "Peserta Kelas" (mengisi ruang kosong), bukan jauh di bawah. Di HP menumpuk 1 kolom.
			Vbox vbox = new Vbox();
			vbox.setSclass("tk-side-col");
			vbox.setWidth("100%");
			vbox.setStyle("min-width:0;");
			try {
				String[] bagian = buatKartuRingkasTugasKelompok(tugasKelompok);
				org.zkoss.zul.Div kartu = new org.zkoss.zul.Div();
				kartu.setSclass("tk-kartu");
				kartu.setParent(gb);
				new ais.ui.util.MyHtml(bagian[0]).setParent(kartu); // gaya CSS + header

				org.zkoss.zul.Div body = new org.zkoss.zul.Div();
				body.setSclass("tk-body");
				body.setParent(kartu);

				new ais.ui.util.MyHtml(bagian[1]).setParent(body); // kolom kiri

				vbox.setParent(body); // kolom kanan (wadah kontrol)
				if (bagian[2] != null && bagian[2].length() > 0) {
					new ais.ui.util.MyHtml(bagian[2]).setParent(vbox); // dosen + peserta di atas
				}
				pasangAksiPengaturan(vbox, tugasKelompok, syaratAlert);
				jd.setVisible(false); // caption lama redundan dengan header kartu → sembunyikan

				// Dashboard analitik & ringkasan nilai (HTML/CSS/SVG, tanpa JFreeChart) — full-width di
				// bawah body, dapat dibuka-tutup (<details>), HANYA untuk pengelola (dosen/admin).
				Tbmuser uAnalitik = Common.getCurrentUser();
				if (bolehKelola(uAnalitik)) {
					String analitik = bangunAnalitikHtml(tugasKelompok);
					if (analitik != null && analitik.length() > 0) {
						org.zkoss.zul.Div wrapAnalitik = new org.zkoss.zul.Div();
						wrapAnalitik.setStyle("padding:0 20px 18px;");
						wrapAnalitik.setParent(kartu);
						new ais.ui.util.MyHtml(analitik).setParent(wrapAnalitik);
					}
				}
			} catch (Exception eKartu) {
				Common.tampilErrorJikaAdmin(eKartu);
				if (vbox.getParent() == null) {
					vbox.setParent(gb); // fallback: kontrol tetap tampil walau kartu gagal dirakit
				}
			}

			if (!(tugasKelompok.getMulai() == null || tugasKelompok.getMulai().before(kemarin.getTime()))) {
				Html isi = new ais.ui.util.MyHtml();
				isi.setContent("<strong><font style='color:red;font-size: 15px;'>Tugas \"" + tugasKelompok.getJudul()
						+ "\" belum mulai..<br>Tugas akan ditampilkan setelah "
						+ SmartDateTimeUtil.getDayString(tugasKelompok.getMulai(), null)
						+ Common.dateFormat5.get().format(tugasKelompok.getMulai()) + "</font></strong><br><img src=\""
						+ Common.getRequestHostWithProtocol()
						+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");
				isi.setParent(vbox);
			} else if (!(tugasKelompok.getSelesai() == null || tugasKelompok.getSelesai().after(kemarin.getTime()))) {
				Html isi = new ais.ui.util.MyHtml();
				isi.setContent("<strong><font style='color:red;font-size: 15px;'>Tugas \"" + tugasKelompok.getJudul()
						+ "\" telah selesai..<br>Tugas telah ditampilkan sebelum "
						+ SmartDateTimeUtil.getDayString(tugasKelompok.getSelesai(), null)
						+ Common.dateFormat5.get().format(tugasKelompok.getSelesai()) + "</font></strong><br><img src=\""
						+ Common.getRequestHostWithProtocol()
						+ "/img/Apps-preferences-system-time-icon.png\" alt=\"WebP rules.\" />");
				isi.setParent(vbox);
			} else {
				final Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
				if (perkuliahan != null && tampilRinci) {
					ais.action.master.helper.PerkuliahanUIHelper.displayDosenPerkuliahan(vbox, perkuliahan, true);
					Kurikulum kurikulum = perkuliahan.getKurikulum();
					Vbox a = RevisiHelper.createNewRevisi(Perkuliahan.class, perkuliahan,
							perkuliahan.getMatakuliah().getKode() + "-" + perkuliahan.getMatakuliah().getNama() + " "
									+ perkuliahan.getMatakuliah().getSks() + " sks "
									+ (kurikulum == null ? "" : " (Kurikulum:" + kurikulum.getTahun() + ")"));
					a.setParent(vbox);

					MatakuliahPrasyaratAction.tampilPrasyarat(a, perkuliahan.getMatakuliah());
					ais.action.master.helper.PerkuliahanUIHelper.displayHariJamRuanganPerkuliahanUmum(vbox, perkuliahan);

				}

				new MyLabelAgakKecil((tugasKelompok.getMulai() != null
						? "Mulai : " + Common.dateFormat3.get().format(tugasKelompok.getMulai())
						: "")
						+ (tugasKelompok.getSelesai() != null
								? " Sampai : " + Common.dateFormat3.get().format(tugasKelompok.getSelesai())
								: ""))
						.setParent(vbox);
				new ais.ui.util.MyHtml(tugasKelompok.getNama()).setParent(vbox);

				if (tugasKelompok.getSyaratMengumpulkanTugas() != null) {
					new MyLabelBold(
							"Syarat mengumpulkan tugas : " + tugasKelompok.getSyaratMengumpulkanTugas().getNama())
							.setParent(vbox);
				}

				new MyLabelKecil(
						"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload")
						.setParent(vbox);

				Vbox myvbox = new Vbox();
				myvbox.setParent(vbox);

				Hbox hbox = new Hbox();
				hbox.setParent(myvbox);

				Row myvboxBaru = Common.tampilanScroll1(vbox);
				myvboxBaru.getGrid().setWidth("90%");
				Tbmuser tbmuser = Common.getCurrentUser();
				LampiranLain.createDownloadUploadFileLain(hbox, tugasKelompok.getId(),
						LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN, LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN, false,
						new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								lampiran = (LampiranLain) arg0.getData();
							}
						}, null, false, false, false,
						bolehKelola(tbmuser),
						null, false, false, myvboxBaru);
			}

			final Perkuliahan perkuliahan = tugasKelompok.getPerkuliahan();
			if (perkuliahan != null) {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (mahasiswa != null || biodataCalonMahasiswa != null || tbmuser.getPesertaKursus() != null) {
					new Label(tugasKelompok.getFormatNilai() == null
							|| tugasKelompok.getFormatNilai().getStatusPertemuan() == null
									? ""
									: tugasKelompok.getFormatNilai().getNama() + " ("
											+ Common.numberFormat.get().format(tugasKelompok.getProsentase()) + "%)")
							.setParent(vbox);
				} else {

					Hbox hboxP = new Hbox();
					final Combobox formatNilai = new Combobox();

					formatNilai.setWidth("92px");
					MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
					comboitemTidakAda.setValue(null);
					formatNilai.appendChild(comboitemTidakAda);
					Session session = HibernateUtil.currentSession();
					List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
					for (FormatNilai nilai : formatNilais) {
						if (nilai.getStatusPertemuan() != null) {
							org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
							comboitem.setValue(nilai);
							comboitem.setLabel(
									nilai.getNama() + " (" + Common.numberFormat.get().format(nilai.getPersen()) + "%)");
							formatNilai.appendChild(comboitem);
						}
					}
					formatNilai.setParent(hboxP);
					if (tugasKelompok.getFormatNilai() == null) {
						formatNilai.setSelectedItem(comboitemTidakAda);
					} else {
						Common.selectComboItem(formatNilai, tugasKelompok.getFormatNilai());
					}
					formatNilai.setReadonly(true);
					formatNilai.setDisabled(perkuliahan.getDikunci() != null);
					if (perkuliahan.getDikunci() != null) {
						new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
						if (tugasKelompok.getFormatNilai() != null) {
							new MyLabelKecil("Nilai otomatis masuk ke " + tugasKelompok.getFormatNilai().getNama())
									.setParent(vbox);
						}
					}

					hboxP.setParent(vbox);
					final MyDoublebox prosentase = new MyDoublebox(tugasKelompok.getProsentase());
					prosentase.setDisabled(perkuliahan.getDikunci() != null);
					prosentase.setCols(2);
					final Label labelBobot;
					hboxP.appendChild(labelBobot = new Label(ais.common.Common.getBahasaConfig(" bobot ")));
					prosentase.setParent(hboxP);
					hboxP.appendChild(new Label(" "));

					prosentase.addEventListener("onChange", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tugasKelompok.setProsentase(prosentase.getValue());
							Common.refreshUpdate(tugasKelompok);
						}
					});

					Hbox c = new Hbox();
					c.setParent(vbox);

					// Tombol aksi (Masukkan Nilai / Format Nilai / Upload dianggap hadir / Tdk. upload)
					// dikelompokkan dalam SATU button group yang rapi — terpisah dari grid Sub-CPMK.
					final Hbox grupTombol = new Hbox();
					grupTombol.setSpacing("2px");
					grupTombol.setStyle("display:flex;flex-wrap:wrap;gap:2px;align-items:center;"
							+ "background:#f8fafc;border:1px solid #e2e8f0;"
							+ "border-radius:10px;padding:3px 6px;margin:8px 0;");
					grupTombol.setParent(vbox);

					if (tugasKelompok.getFormatNilais() != null && perkuliahan != null
							&& perkuliahan.getKurikulum() != null
							&& perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(),
									perkuliahan.getGanjilGenap())
							&& bolehKelola(tbmuser)) {

						// Kurikulum OBE: sembunyikan pilihan Format Nilai tunggal + bobot manual (hboxP),
						// karena penilaian OBE memakai pemetaan Sub-CPMK di grid di bawah. Meniru
						// TugasMandiriHelper yang hanya menampilkan combo+bobot pada cabang NON-OBE.
						hboxP.setVisible(false);

						final MyToolbarbutton button = new MyToolbarbutton("fa-line-chart", "Masukkan Nilai");
						button.setTooltiptext("Hitung dan masukkan nilai ke format penilaian OBE");
						button.setParent(grupTombol);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								Common.createDefaultTimer(new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilaiObe(tugasKelompok.getPerkuliahan(),
												tugasKelompok.getFormatNilais());
									}
								});
							}
						});

						// Pemetaan Sub-CPMK & Bobot pada KARTU bersifat BACA-SAJA (label). Perubahan bobot
						// HANYA dapat dilakukan lewat tombol "Instruksi Tugas Kelompok" (Window edit) —
						// sesuai instruksi kerapian tampilan. Lihat bangunGridSubCpmk(..., editable=true)
						// yang dipanggil di dalam init() (form Instruksi).
						bangunGridSubCpmk(c, tugasKelompok, perkuliahan, false);

					}

					else {

						final MyToolbarbutton button = new MyToolbarbutton("fa-line-chart", "Masukkan Nilai");
						button.setTooltiptext("Hitung dan masukkan nilai ke format penilaian");
						button.setParent(grupTombol);
						button.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								ais.common.GradingHelper.hitungNilaiBerdasarkanFormatNilai(perkuliahan,
										tugasKelompok.getFormatNilai());
							}
						});

						final MyToolbarbutton buttonFormatNilai = new MyToolbarbutton("fa-sliders", "Format Nilai");
						buttonFormatNilai.setTooltiptext("Ubah format penilaian untuk tugas kelompok ini");

						if (tugasKelompok.getPerkuliahan() != null) {
							if (tugasKelompok.getPerkuliahan() != null
									&& !tugasKelompok.getPerkuliahan().getSembunyikanFormatPenilaian()) {
								buttonFormatNilai.setVisible(bolehKelola(tbmuser));
								buttonFormatNilai.setParent(grupTombol);
								buttonFormatNilai.setVisible(perkuliahan.getDikunci() == null);
								if (perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum()
										.apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap())) {
									buttonFormatNilai.setVisible(false);
								}
								buttonFormatNilai.addEventListener("onClick", new EventListener() {

									FormatPenilaianHelper formatPenilaianHelper = new FormatPenilaianHelper();

									@Override
									public void onEvent(Event event) throws Exception {

										MyWindow addWindow = new MyWindow();
										addWindow.setHeight("95%");
										addWindow.setWidth("700px");
										ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
												.appendChild(addWindow);

										formatPenilaianHelper.display(perkuliahan, addWindow,
												new TampilDetailNilaiInterface() {

													@Override
													public void realoadNilai(final Perkuliahan perkuliahan) {

														Common.realoadNilai(perkuliahan,
																perkuliahan.getSembunyikanNilaiJikaBelumDiverifikasi(),
																new EventListener() {

																	@Override
																	public void onEvent(Event arg0) throws Exception {
																		Common.createDefaultTimer(new EventListener() {

																			@Override
																			public void onEvent(Event arg0)
																					throws Exception {
																				loadData(null);
																			}
																		});

																	}
																}, null);

													}
												});
									}

								});
							}
						}

						prosentase.setVisible(tugasKelompok.getFormatNilai() != null);
						labelBobot.setVisible(tugasKelompok.getFormatNilai() != null);
						button.setVisible(tugasKelompok.getFormatNilai() != null);
						buttonFormatNilai.setVisible(tugasKelompok.getFormatNilai() != null);

						formatNilai.addEventListener("onChange", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								FormatNilai fn = (FormatNilai) (formatNilai.getSelectedItem() == null ? null
										: formatNilai.getSelectedItem().getValue());

								bersihkanFormatNilaiYatim(tugasKelompok);
								Session session = HibernateUtil.currentSession();
								if (fn != null && fn.getId() != null) {
									fn = (FormatNilai) session.get(FormatNilai.class, fn.getId());
								}
								tugasKelompok.setFormatNilai(fn);
								Common.refreshUpdate(session, (tugasKelompok));

								prosentase.setVisible(tugasKelompok.getFormatNilai() != null);
								labelBobot.setVisible(tugasKelompok.getFormatNilai() != null);
								button.setVisible(tugasKelompok.getFormatNilai() != null);
								buttonFormatNilai.setVisible(tugasKelompok.getFormatNilai() != null);
							}

						});
					}

					final Pertemuan p = tugasKelompok.ambilPertemuan();
					if (p != null && bolehKelola(Common.getCurrentUser())) {

						MyToolbarbutton masuk = new MyToolbarbutton("fa-check", "Anggap Hadir (Pengumpul)");
						masuk.setStyle("font-size:9px;");
						masuk.setTooltiptext("Upload Tugas \"" + pertemuan.getJudultugas() + "\" dianggap hadir");
						masuk.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {

								uploadTugasDiangapHadir(tugasKelompok, p, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(p,
												new DataLoader() {

													@Override
													public void loadData(Object value) {
														try {
															TugasKelompokHelper.this.loadData(null);
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasKelompokHelper.java:1896");
														}
													}
												}, 0);
									}
								});

							}
						});
						masuk.setParent(grupTombol);

						masuk = new MyToolbarbutton("fa-times", "Tdk Upload = Alpa");
						masuk.setStyle("font-size:9px;");
						masuk.setTooltiptext("Mahasiswa yang tidak upload tugas dianggap tidak hadir (alpa)");
						masuk.addEventListener("onClick", new EventListener() {
							@Override
							public void onEvent(Event event) throws Exception {
								tidakUploadTugasDiangapTidakHadir(tugasKelompok, p, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										new PertemuanHelper(Common.getCurrentUser().getMahasiswa(), null).display(p,
												new DataLoader() {

													@Override
													public void loadData(Object value) {
														try {
															TugasKelompokHelper.this.loadData(null);
														} catch (Exception e) {
															e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasKelompokHelper.java:1925");
														}
													}
												}, 0);
									}
								});

							}
						});
						masuk.setParent(grupTombol);

					}

				}
			} else if (jadwalPelajaran != null) {

				final Combobox formatNilai = new Combobox();

				formatNilai.setWidth("92px");
				MyComboitemConfig comboitemTidakAda = new MyComboitemConfig("Tidak Ada");
				comboitemTidakAda.setValue(null);
				formatNilai.appendChild(comboitemTidakAda);

				KelasSiswa kelasSiswa = jadwalPelajaran.getKelas();

				JenisPenilaian jenisPenilaian = jadwalPelajaran.getMatapelajaran().getJenisPenilaian();
				if (jadwalPelajaran.getKurikulumPunyaMatapelajaran() != null
						&& jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah() != null
						&& jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah()
								.getJenisPenilaian() != null) {
					jenisPenilaian = jadwalPelajaran.getKurikulumPunyaMatapelajaran().getKurikulumSekolah()
							.getJenisPenilaian();
				}

				Session session = HibernateUtil.currentSession();
				List<GrupPenilaian> grupPenilaians = ConstantValues
						.simpleList(
								session.createCriteria(DetailJenisPenilaian.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))
										.add(Restrictions.eq("jenisPenilaian", jenisPenilaian))
										.setProjection(Projections.groupProperty("grupPenilaian.id")),
								GrupPenilaian.class, false);

				for (GrupPenilaian grupPenilaian : grupPenilaians) {

					if (grupPenilaian != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0
							&& grupPenilaian.getKhususTingkat() != null
							&& !grupPenilaian.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
						continue;
					}

					List<GrupKategoriItemPenilaianSiswa> grupKategoriItemPenilaianSiswas = ConstantValues
							.simpleList(
									session.createCriteria(DetailGrupPenilaian.class)
											.add(Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)))
											.add(Restrictions.isNotNull("grupKategoriItemPenilaianSiswa"))
											.setProjection(
													Projections.groupProperty("grupKategoriItemPenilaianSiswa.id"))
											.add(Restrictions.eq("grupPenilaian", grupPenilaian)),
									GrupKategoriItemPenilaianSiswa.class, false);

					if (grupKategoriItemPenilaianSiswas.isEmpty()) {
						return;
					}

					Collections.sort(grupKategoriItemPenilaianSiswas);
					for (GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa : grupKategoriItemPenilaianSiswas) {

						if (grupKategoriItemPenilaianSiswa != null && kelasSiswa != null && kelasSiswa.getTingkat() > 0
								&& grupKategoriItemPenilaianSiswa.getKhususTingkat() != null
								&& !grupKategoriItemPenilaianSiswa.getKhususTingkat().equals(kelasSiswa.getTingkat())) {
							continue;
						}

						List<KategoriItemPenilaianSiswa> kategoriItemPenilaianSiswasId = ConstantValues.simpleList(
								session.createCriteria(DetailGrupKategoriItemPenilaianSiswa.class)

										.add(Restrictions.eq("grupKategoriItemPenilaianSiswa",
												grupKategoriItemPenilaianSiswa))
										.add(Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))

										.setProjection(Projections.groupProperty("kategoriItemPenilaianSiswa.id")),
								KategoriItemPenilaianSiswa.class, false);

						List<JenisItemPenilaianSiswa> jenisItemPenilaianSiswas = ConstantValues
								.simpleList(
										session.createCriteria(JenisItemPenilaianSiswa.class)
												.createAlias("kategoriItemPenilaianSiswa", "kategoriItemPenilaianSiswa")
												.addOrder(Order.asc("kategoriItemPenilaianSiswa.kode"))
												.addOrder(Order.asc("nomorUrut"))
												.add(Restrictions.in("kategoriItemPenilaianSiswa",
														kategoriItemPenilaianSiswasId))
												.add(Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true))),
										JenisItemPenilaianSiswa.class);
						for (JenisItemPenilaianSiswa jenisItemPenilaianSiswa : jenisItemPenilaianSiswas) {

							if (jenisItemPenilaianSiswa.getTipeDataInputan().equals(JenisItemPenilaianSiswa.ANGKA)
									|| jenisItemPenilaianSiswa.getTipeDataInputan()
											.equals(JenisItemPenilaianSiswa.TEXT_ANGKA)) {

								org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
								comboitem.setValue(jenisItemPenilaianSiswa);
								comboitem.setLabel(jenisItemPenilaianSiswa.getNama() + " ("
										+ jenisItemPenilaianSiswa.getKode() + ")");
								comboitem.setDescription(grupKategoriItemPenilaianSiswa.getNama() + " ("
										+ grupPenilaian.getNama() + ")");

								comboitem.setAttribute("grupKategoriItemPenilaianSiswa",
										grupKategoriItemPenilaianSiswa);

								comboitem.setAttribute("grupPenilaian", grupPenilaian);

								formatNilai.appendChild(comboitem);
							}

						}

					}

				}

				formatNilai.setParent(vbox);
				if (tugasKelompok.getJenisItemPenilaianSiswa() == null) {
					formatNilai.setSelectedItem(comboitemTidakAda);
				} else {
					Common.selectComboItem(formatNilai, tugasKelompok.getJenisItemPenilaianSiswa());
				}
				formatNilai.setReadonly(true);
				formatNilai.setDisabled(jadwalPelajaran.getDikunci() != null);
				if (jadwalPelajaran.getDikunci() != null) {
					new MyLabelKecil("Penilaian sudah dikunci").setParent(vbox);
					if (tugasKelompok.getJenisItemPenilaianSiswa() != null) {
						new MyLabelKecil("Nilai otomatis masuk ke "
								+ tugasKelompok.getJenisItemPenilaianSiswa().getNama() + " "
								+ (tugasKelompok.getJenisItemPenilaianSiswa().getKategoriItemPenilaianSiswa() == null
										? ""
										: " " + tugasKelompok.getJenisItemPenilaianSiswa()
												.getKategoriItemPenilaianSiswa().getNama())

						).setParent(vbox);
					}
				}

				final MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Sinkronkan Nilai");
				button.setTooltiptext("Sinkronkan nilai tugas kelompok ke daftar nilai siswa");
				button.setParent(vbox);
				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						ais.common.GradingHelper.hitungNilaiBerdasarkanJenisItemPenilaianSiswa(jadwalPelajaran,
								tugasKelompok.getGrupKategoriItemPenilaianSiswa(), tugasKelompok.getGrupPenilaian(),
								tugasKelompok.getJenisItemPenilaianSiswa());
					}
				});
				button.setVisible(tugasKelompok.getJenisItemPenilaianSiswa() != null);

				formatNilai.addEventListener("onChange", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						JenisItemPenilaianSiswa fn = (JenisItemPenilaianSiswa) (formatNilai.getSelectedItem() == null
								? null
								: formatNilai.getSelectedItem().getValue());

						GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa = (GrupKategoriItemPenilaianSiswa) (formatNilai
								.getSelectedItem() == null ? null
										: formatNilai.getSelectedItem().getAttribute("grupKategoriItemPenilaianSiswa"));

						GrupPenilaian grupPenilaian = (GrupPenilaian) (formatNilai.getSelectedItem() == null ? null
								: formatNilai.getSelectedItem().getAttribute("grupPenilaian"));

						Session session = HibernateUtil.currentSession();
						tugasKelompok.setJenisItemPenilaianSiswa(fn);
						tugasKelompok.setGrupKategoriItemPenilaianSiswa(grupKategoriItemPenilaianSiswa);
						tugasKelompok.setGrupPenilaian(grupPenilaian);
						Common.refreshUpdate(session, (tugasKelompok));

						button.setVisible(tugasKelompok.getJenisItemPenilaianSiswa() != null);
					}

				});

			}

			RevisiHelper.createNewRevisi(TugasKelompok.class, tugasKelompok,
					"Tgl : " + Common.dateFormat5.get().format(tugasKelompok.getTanggal())).setParent(vbox);

			Session session = HibernateUtil.currentSession();
			Integer count = ((Number) session.createCriteria(NamaTugasKelompok.class)
					.add(Restrictions.eq("tugasKelompok", tugasKelompok)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();

			new Label("Jml kelompok : " + Common.numberFormat.get().format(count)).setParent(vbox);

			if (count.equals(0)) {
				MyLabelAgakKecilBold a;
				vbox.appendChild(a = new MyLabelAgakKecilBold(
						"Catatan : Minimal harus ada satu kelompok yang dibuat agar mahasiswa dapat mengumpulkan tugas."));
				a.setStyle("font-size:9px;font-weight: bolder;color:red");
			}

			Tbmuser tbmuser = Common.getCurrentUser();

			Hbox toolbar = new Hbox();
			toolbar.setSpacing("2px");
			toolbar.setStyle("display:flex;flex-wrap:wrap;gap:2px;align-items:center;"
					+ "background:#f8fafc;border:1px solid #e2e8f0;"
					+ "border-radius:10px;padding:3px 6px;margin:8px 0;");
			MyToolbarbutton button;

			// (C) Kelola Nilai: entri nilai per anggota kelompok dalam satu daftar rata (Window).
			// Hanya untuk pengelola (bukan mahasiswa/siswa).
			button = new MyToolbarbutton("fa-star", "Kelola Nilai");
			button.setVisible(bolehKelola(tbmuser));
			button.setTooltiptext("Isi nilai tiap anggota kelompok");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaKelolaNilai(tugasKelompok);
				}
			});
			button.setParent(toolbar);

			// (D-pre) Download Nilai & Upload Nilai — bulk entry nilai seluruh anggota via Excel
			final MyToolbarbutton downloadNilaiKel = new MyToolbarbutton("fa-download", "Download Nilai");
			downloadNilaiKel.setVisible(bolehKelola(tbmuser));
			downloadNilaiKel.setTooltiptext("Unduh daftar nilai anggota kelompok dalam format Excel");
			downloadNilaiKel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						Session sessDl = HibernateUtil.currentSession();
						final java.util.List<FormatNilai> obeListDl = new java.util.ArrayList<FormatNilai>();
						try {
							Perkuliahan pkDl = tugasKelompok.getPerkuliahan();
							if (pkDl != null && pkDl.getKurikulum() != null
									&& pkDl.getKurikulum().apakahObe(pkDl.getTahunAjaran(), pkDl.getGanjilGenap())
									&& tugasKelompok.getFormatNilais() != null
									&& tugasKelompok.getFormatNilais().trim().length() > 0) {
								JSONObject jfnDl = new JSONObject(tugasKelompok.getFormatNilais());
								List<FormatNilai> fnsAllDl = Common.getFormatNilais(sessDl, pkDl);
								if (fnsAllDl != null) {
									for (FormatNilai fn : fnsAllDl) {
										if (fn.getStatusPertemuan() != null && fn.getId() != null
												&& !jfnDl.isNull(fn.getId().toString()))
											obeListDl.add(fn);
									}
								}
							}
						} catch (Exception eObe) { /* abaikan jika gagal deteksi OBE */ }
						boolean isObeDl = !obeListDl.isEmpty();

						String ketAwalDl = tugasKelompok.getKeteranganNilai();
						JSONObject jsonKetDl = new JSONObject(
								ketAwalDl == null || ketAwalDl.trim().isEmpty() ? "{}" : ketAwalDl.replace(' ', ' '));

						List<NamaTugasKelompok> kelsDl = sessDl.createCriteria(NamaTugasKelompok.class)
								.add(Restrictions.eq("tugasKelompok", tugasKelompok))
								.addOrder(Order.asc("nama")).list();

						org.zkoss.poi.xssf.usermodel.XSSFWorkbook wb = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook();
						org.zkoss.poi.xssf.usermodel.XSSFSheet sheet = wb.createSheet("Nilai");
						org.zkoss.poi.xssf.usermodel.XSSFRow headerRow = sheet.createRow(0);
						headerRow.createCell(0).setCellValue("ID");
						headerRow.createCell(1).setCellValue("NAMATEMP");
						headerRow.createCell(2).setCellValue("Kelompok");
						if (!isObeDl) {
							headerRow.createCell(3).setCellValue("Nilai");
							headerRow.createCell(4).setCellValue("Keterangan");
						} else {
							int col = 3;
							for (FormatNilai fn : obeListDl)
								headerRow.createCell(col++).setCellValue(fn.getNama() != null ? fn.getNama() : ("CPMK_" + fn.getId()));
							headerRow.createCell(col).setCellValue("Keterangan");
						}

						int rowIdx = 1;
						if (kelsDl != null) {
							for (NamaTugasKelompok kel : kelsDl) {
								List<NamaTugasKelompokPunyaMahasiswa> relsDl = sessDl
										.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
										.add(Restrictions.eq("namaTugasKelompok", kel)).list();
								if (relsDl == null) continue;
								for (NamaTugasKelompokPunyaMahasiswa r : relsDl) {
									if (r == null) continue;
									String namaDl = "";
									String memberKeyDl = "";
									if (r.getMahasiswa() != null) {
										namaDl = r.getMahasiswa().getNama() != null ? r.getMahasiswa().getNama() : "";
										memberKeyDl = r.getMahasiswa().getId() + "_mhs";
									} else if (r.getSiswa() != null) {
										namaDl = r.getSiswa().getNama() != null ? r.getSiswa().getNama() : "";
										memberKeyDl = r.getSiswa().getId() + "_siswa";
									}
									org.zkoss.poi.xssf.usermodel.XSSFRow rowDl = sheet.createRow(rowIdx++);
									rowDl.createCell(0).setCellValue(r.getId());
									rowDl.createCell(1).setCellValue(namaDl);
									rowDl.createCell(2).setCellValue(kel.getNama() != null ? kel.getNama() : "");
									String ketDl = r.getKeterangan() != null ? r.getKeterangan() : "";
									if (!isObeDl) {
										rowDl.createCell(3).setCellValue(r.getNilai() != null ? r.getNilai() : 0.0);
										rowDl.createCell(4).setCellValue(ketDl);
									} else {
										int col = 3;
										for (FormatNilai fn : obeListDl) {
											String scoreKey = memberKeyDl + "_nilai_" + fn.getId();
											double score = !memberKeyDl.isEmpty() && !jsonKetDl.isNull(scoreKey)
													? jsonKetDl.optDouble(scoreKey, 0.0) : 0.0;
											rowDl.createCell(col++).setCellValue(score);
										}
										rowDl.createCell(col).setCellValue(ketDl);
									}
								}
							}
						}

						String fname = "Nilai_TugasKelompok_" + tugasKelompok.getId() + ".xlsx";
						java.io.File outFile = new java.io.File(Common.REAL_PATH + "/temp/" + fname);
						outFile.getParentFile().mkdirs();
						java.io.FileOutputStream fos = new java.io.FileOutputStream(outFile);
						wb.write(fos);
						fos.close();
						org.zkoss.zul.Filedownload.save(outFile,
								"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("mengunduh nilai tugas kelompok", e,
								new String[] {
										"Muat ulang halaman lalu coba unduh kembali.",
										"Apabila kendala masih berlanjut, hubungi Admin." });
					}
				}
			});
			downloadNilaiKel.setParent(toolbar);

			final MyToolbarbutton uploadNilaiKel = new MyToolbarbutton("fa-upload", "Upload Nilai");
			uploadNilaiKel.setVisible(bolehKelola(tbmuser));
			uploadNilaiKel.setTooltiptext("Unggah file Excel hasil Download Nilai untuk update nilai massal");
			uploadNilaiKel.setUpload(Common.ukuranFileUpload());
			uploadNilaiKel.addEventListener("onUpload", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					org.zkoss.zk.ui.event.UploadEvent ue = (org.zkoss.zk.ui.event.UploadEvent) event;
					org.zkoss.util.media.Media media = ue.getMedia();
					if (!ais.action.master.helper.generic.AmbilDataTugasFileContent.checkFile(media)) return;
					if (!media.getName().toLowerCase().endsWith("xlsx")) {
						MyMessageboxConfig.showFormat(
								"Berkas harus berformat xlsx. Berkas: {V1}.",
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.ERROR, media);
						return;
					}
					try {
						java.io.InputStream is = media.getStreamData();
						java.io.File f = new java.io.File(org.zkoss.zk.ui.Sessions.getCurrent()
								.getWebApp().getRealPath("/temp/" + media.getName()));
						f.getParentFile().mkdirs();
						java.io.FileOutputStream fosUl = new java.io.FileOutputStream(f);
						int c;
						while ((c = is.read()) != -1) fosUl.write(c);
						fosUl.close();
						is.close();

						Session sessUl = HibernateUtil.currentSession();
						List<NamaTugasKelompok> kelsUl = sessUl.createCriteria(NamaTugasKelompok.class)
								.add(Restrictions.eq("tugasKelompok", tugasKelompok)).list();
						java.util.Map<Long, NamaTugasKelompokPunyaMahasiswa> byId =
								new java.util.HashMap<Long, NamaTugasKelompokPunyaMahasiswa>();
						if (kelsUl != null) {
							for (NamaTugasKelompok kel : kelsUl) {
								List<NamaTugasKelompokPunyaMahasiswa> relsUl = sessUl
										.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
										.add(Restrictions.eq("namaTugasKelompok", kel)).list();
								if (relsUl != null) {
									for (NamaTugasKelompokPunyaMahasiswa r : relsUl) {
										if (r != null && r.getId() != null) byId.put(r.getId(), r);
									}
								}
							}
						}
						final java.util.List<FormatNilai> obeListUl = new java.util.ArrayList<FormatNilai>();
						try {
							Perkuliahan pkUl = tugasKelompok.getPerkuliahan();
							if (pkUl != null && pkUl.getKurikulum() != null
									&& pkUl.getKurikulum().apakahObe(pkUl.getTahunAjaran(), pkUl.getGanjilGenap())
									&& tugasKelompok.getFormatNilais() != null
									&& tugasKelompok.getFormatNilais().trim().length() > 0) {
								JSONObject jfnUl = new JSONObject(tugasKelompok.getFormatNilais());
								List<FormatNilai> fnsAllUl = Common.getFormatNilais(sessUl, pkUl);
								if (fnsAllUl != null) {
									for (FormatNilai fn : fnsAllUl) {
										if (fn.getStatusPertemuan() != null && fn.getId() != null
												&& !jfnUl.isNull(fn.getId().toString()))
											obeListUl.add(fn);
									}
								}
							}
						} catch (Exception eObeUl) { /* skip */ }
						boolean isObeUl = !obeListUl.isEmpty();
						int ketColUl = isObeUl ? 3 + obeListUl.size() : 4;

						String ketAwalUl = tugasKelompok.getKeteranganNilai();
						JSONObject jsonKetUl = new JSONObject(
								ketAwalUl == null || ketAwalUl.trim().isEmpty() ? "{}" : ketAwalUl.replace(' ', ' '));
						boolean jsonDirty = false;

						org.zkoss.poi.xssf.usermodel.XSSFWorkbook wbUl = new org.zkoss.poi.xssf.usermodel.XSSFWorkbook(f.getAbsolutePath());
						org.zkoss.poi.xssf.usermodel.XSSFSheet sheetUl = wbUl.getSheetAt(0);
						int sizeUl = sheetUl.getLastRowNum() + 1;

						for (int i = 1; i < sizeUl; i++) {
							Long id = Common.getSheetContentAsLong(sheetUl, 0, i);
							NamaTugasKelompokPunyaMahasiswa r = byId.get(id);
							if (r == null) continue;
							String keterangan = Common.getSheetContentAsString(sheetUl, ketColUl, i);
							NamaTugasKelompokPunyaMahasiswa xSave = (NamaTugasKelompokPunyaMahasiswa)
									sessUl.load(NamaTugasKelompokPunyaMahasiswa.class, r.getId());
							xSave.setKeterangan(keterangan != null ? keterangan.trim() : "");
							if (!isObeUl) {
								Double nilaiUl = Common.getSheetContentAsDouble(sheetUl, 3, i);
								xSave.setNilai(nilaiUl);
								Common.refreshUpdate(sessUl, xSave);
							} else {
								Common.refreshUpdate(sessUl, xSave);
								String memberKeyUl = "";
								if (r.getMahasiswa() != null) memberKeyUl = r.getMahasiswa().getId() + "_mhs";
								else if (r.getSiswa() != null) memberKeyUl = r.getSiswa().getId() + "_siswa";
								for (int ci = 0; ci < obeListUl.size(); ci++) {
									FormatNilai fn = obeListUl.get(ci);
									Double nilaiCpmk = Common.getSheetContentAsDouble(sheetUl, 3 + ci, i);
									jsonKetUl.put(memberKeyUl + "_nilai_" + fn.getId(),
											nilaiCpmk != null ? nilaiCpmk : 0.0);
								}
								jsonDirty = true;
							}
						}
						if (jsonDirty) {
							sessUl.refresh(tugasKelompok);
							tugasKelompok.belum("tugas_file_content_" + tugasKelompok.getClass().getName());
							tugasKelompok.setKeteranganNilai(jsonKetUl.toString());
							Common.refreshUpdate(sessUl, tugasKelompok);
						}
						MyMessageboxConfig.show("Nilai berhasil diupload.", "Berhasil",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
						PesanFormalHelper.tampilkanGagalException("mengupload nilai tugas kelompok", e,
								new String[] {
										"Pastikan format file sesuai hasil Download Nilai.",
										"Apabila kendala masih berlanjut, hubungi Admin." });
					}
				}
			});
			uploadNilaiKel.setParent(toolbar);

			// (D) Kelola Kelompok: Daftar Kelompok (Tambah/Download/Upload + grid anggota) dipindah
			// ke dalam Popup Window agar kartu tetap ringkas. HANYA terlihat bagi pelajar (mahasiswa/
			// siswa memakainya untuk melihat & bergabung kelompok; kontrol Tambah/Upload otomatis
			// disembunyikan bagi mereka di dalam NamaTugasKelompokHelper). Pengelola tidak butuh tombol
			// ini di sini karena sudah mendapat aksi setara ("Kelola Kelompok & Anggota") di panel
			// "Pengaturan Tugas Kelompok" (lihat pasangAksiPengaturan) yang ditempel di atas kartu.
			button = new MyToolbarbutton("fa-users", "Kelola Kelompok");
			button.setVisible(!bolehKelola(tbmuser));
			button.setTooltiptext("Kelola daftar kelompok & anggota");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					bukaDaftarKelompok(tugasKelompok, syaratAlert, false);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbutton("fa-trash", "Hapus Tugas Kelompok");
			button.setVisible(bolehKelola(tbmuser));
			button.setTooltiptext("Hapus data tugas kelompok ini secara permanen");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					konfirmasiHapusTugasKelompok(tugasKelompok);
				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(vbox);

			if (bolehKelola(tbmuser)) {

				// Daftar Kelompok dipindah ke Popup Window (tombol "Kelola Kelompok" di atas), agar
				// kartu ringkas tetap bersih. Baris inline lama dihapus dari sini.
				final MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-eye",
						"Tampilkan peserta yang tidak perlu mengumpulkan tugas kelompok");
				toolbarbutton.setStyle("font-size:9px;");
				toolbarbutton.setTooltiptext("Tampilkan daftar peserta yang dikecualikan dari tugas kelompok ini");
				gb.appendChild(toolbarbutton);
				toolbarbutton.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						toolbarbutton.detach();

						Groupbox div = new ais.ui.util.MyGroupboxStyled();
						div.appendChild(new MyCaptionStyled("Daftar peserta yang tidak perlu mengumpulkan tugas"));
						div.setParent(gb);

						Hbox hbox = new Hbox();
						hbox.appendChild(new MyLabelConfig("Peserta : "));
						final Textbox cari = new Textbox("");
						cari.setParent(hbox);
						cari.setCols(20);

						final Grid grid = new Grid();
						grid.setSclass("dgrid");
						grid.setParent(div);

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig();
						column.appendChild(hbox);
						column.setParent(columns);
						column.setWidth("70%");

						final MyCheckboxConfig checkboxConfigAll = new MyCheckboxConfig("Tidak perlu ikut");

						column = new MyColumnConfig();
						column.appendChild(checkboxConfigAll);
						column.setParent(columns);

						grid.setHeight("100%");
						grid.setWidth("100%");

						grid.setRowRenderer(new ais.ui.util.MyRowRenderer() {
							@Override
							public void render(Row arg0, Object arg1) throws Exception {
								arg0.setValign("top");
								arg0.setSclass("ais-tugas-upload-row");
								final Mahasiswa mahasiswa = (arg1 instanceof Mahasiswa) ? (Mahasiswa) arg1 : null;
								final BiodataCalonMahasiswa biodataCalonMahasiswa = (arg1 instanceof BiodataCalonMahasiswa)
										? (BiodataCalonMahasiswa) arg1
										: null;
								final Siswa siswa = (arg1 instanceof Siswa) ? (Siswa) arg1 : null;
								final CalonSiswa calonSiswa = (arg1 instanceof CalonSiswa) ? (CalonSiswa) arg1 : null;

								Hbox hbox = new Hbox();
								hbox.setStyle("gap:6px;align-items:center;");
								hbox.setParent(arg0);
								if (mahasiswa != null) {
									CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
								} else if (biodataCalonMahasiswa != null) {
									CommonMedia.tampilkanGambarKecil(biodataCalonMahasiswa).setParent(hbox);
								} else if (siswa != null) {
									CommonMedia.tampilkanGambarKecil(siswa).setParent(hbox);
								} else if (calonSiswa != null) {
									CommonMedia.tampilkanGambarKecil(calonSiswa).setParent(hbox);
								}

								Vbox vb = new Vbox();
								vb.setParent(hbox);
								String nim = mahasiswa != null ? mahasiswa.getNim()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi()
												: siswa != null ? siswa.getNomorInduk()
														: calonSiswa != null ? calonSiswa.getNomorInduk() : "";
								String nama = mahasiswa != null ? mahasiswa.getNama()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama()
												: siswa != null ? siswa.getNama()
														: calonSiswa != null ? calonSiswa.getNama() : "";
								Label namaLbl = new Label(nim + " / " + nama);
								namaLbl.setSclass("ais-tugas-upload-nama");
								vb.appendChild(namaLbl);

								Long id = mahasiswa != null ? mahasiswa.getId()
										: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
												: siswa != null ? siswa.getId()
														: calonSiswa != null ? calonSiswa.getId() : null;

								final MyCheckboxConfig checkboxConfig = new MyCheckboxConfig("Tidak perlu ikut");
								checkboxConfig.setDisabled(mahasiswa == null);
								checkboxConfig.setChecked(tugasKelompok.getMhsYgTidakIkut().contains("," + id + ","));
								checkboxConfig.setParent(arg0);
								checkboxConfig.addEventListener("onClick", new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (tugasKelompok.getId() != null) {
											session.refresh(tugasKelompok);
										}

										Long id = mahasiswa != null ? mahasiswa.getId()
												: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
														: siswa != null ? siswa.getId()
																: calonSiswa != null ? calonSiswa.getId() : null;
										tugasKelompok.setMhsYgTidakIkut(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
												tugasKelompok.getMhsYgTidakIkut(), id, checkboxConfig.isChecked()));

										Common.refreshUpdate(session, tugasKelompok);
									}
								});
							}

						});

						EventListener cariAkun = new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<Mahasiswa> mahasiswasTemorary = tugasKelompok == null
										|| tugasKelompok.getPerkuliahan() == null
										? Collections.<Mahasiswa>emptyList()
										: tugasKelompok.getPerkuliahan().ambilMahasiswa();
								List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
								for (Mahasiswa mahasiswa : mahasiswasTemorary) {
									BiodataCalonMahasiswa biodataCalonMahasiswa = null;
									if (cari.getValue().trim().isEmpty() ||

											(mahasiswa != null &&

													((mahasiswa.getNim() != null && mahasiswa.getNim().toLowerCase()
															.contains(cari.getValue().toLowerCase().trim()))

															||

															(mahasiswa.getNama() != null
																	&& mahasiswa.getNama().toLowerCase().contains(
																			cari.getValue().toLowerCase().trim()))

													)

											)

											||

											(biodataCalonMahasiswa != null &&

													((biodataCalonMahasiswa.getNoRegistrasi() != null
															&& biodataCalonMahasiswa.getNoRegistrasi().toLowerCase()
																	.contains(cari.getValue().toLowerCase().trim()))

															||

															(biodataCalonMahasiswa.getNama() != null
																	&& biodataCalonMahasiswa.getNama().toLowerCase()
																			.contains(cari.getValue().toLowerCase()
																					.trim()))

													)

											)

									) {
										copy.add(mahasiswa);
									}
								}
								ListModel strset = new SimpleListModel(copy);
								grid.setModel(strset);
								mahasiswasTemorary = null;
								copy = null;
							}
						};
						cariAkun.onEvent(null);
						cari.addEventListener("onOK", cariAkun);

						MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-search", "");
						toolbarbutton.setTooltiptext("Cari peserta");
						toolbarbutton.setParent(hbox);
						toolbarbutton.addEventListener("onClick", cariAkun);

						checkboxConfigAll.addEventListener("onClick", new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {

								Session session = HibernateUtil.currentSession();
								if (tugasKelompok.getId() != null) {
									session.refresh(tugasKelompok);
								}

								List<Mahasiswa> mahasiswasTemorary = tugasKelompok == null
										|| tugasKelompok.getPerkuliahan() == null
										? Collections.<Mahasiswa>emptyList()
										: tugasKelompok.getPerkuliahan().ambilMahasiswa();
								List<Mahasiswa> copy = new ArrayList<Mahasiswa>();
								for (Mahasiswa mahasiswa : mahasiswasTemorary) {
									BiodataCalonMahasiswa biodataCalonMahasiswa = null;
									Siswa siswa = null;
									CalonSiswa calonSiswa = null;
									if (cari.getValue().trim().isEmpty() ||

											(mahasiswa != null &&

													((mahasiswa.getNim() != null && mahasiswa.getNim().toLowerCase()
															.contains(cari.getValue().toLowerCase().trim()))

															||

															(mahasiswa.getNama() != null
																	&& mahasiswa.getNama().toLowerCase().contains(
																			cari.getValue().toLowerCase().trim()))

													)

											)

											||

											(biodataCalonMahasiswa != null &&

													((biodataCalonMahasiswa.getNoRegistrasi() != null
															&& biodataCalonMahasiswa.getNoRegistrasi().toLowerCase()
																	.contains(cari.getValue().toLowerCase().trim()))

															||

															(biodataCalonMahasiswa.getNama() != null
																	&& biodataCalonMahasiswa.getNama().toLowerCase()
																			.contains(cari.getValue().toLowerCase()
																					.trim()))

													)

											)

									) {

										Long id = mahasiswa != null ? mahasiswa.getId()
												: biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getId()
														: siswa != null ? siswa.getId()
																: calonSiswa != null ? calonSiswa.getId() : null;
										tugasKelompok.setMhsYgTidakIkut(ais.common.GradingHelper.ubahIdPadaCsvBerpagarKoma(
												tugasKelompok.getMhsYgTidakIkut(), id, checkboxConfigAll.isChecked()));

										copy.add(mahasiswa);
									}
								}

								Common.refreshUpdate(session, tugasKelompok);

								ListModel strset = new SimpleListModel(copy);
								grid.setModel(strset);
								mahasiswasTemorary = null;
								copy = null;

							}
						});

					}
				});

			}
			// Catatan: Daftar Kelompok kini selalu dibuka lewat tombol "Kelola Kelompok" (Popup Window),
			// baik untuk pengelola maupun mahasiswa/siswa — sehingga tidak ada lagi render inline di kartu.

			if (pertemuan != null) {

				Grid grid = new Grid();
				grid.setSclass("dgrid");
				grid.setSclass("fgrid");
				grid.setParent(gb);

				rowsSyarat.setParent(grid);

			}
		}

	}

	/**
	 * Menempelkan strip kartu ringkasan (Total Tugas, Sedang Berjalan, Lewat Batas) di atas daftar.
	 * Memberi gambaran cepat berapa tugas kelompok yang masih dalam masa pengumpulan dan berapa yang
	 * sudah melewati batas, agar dosen/guru langsung tahu kondisi tanpa membaca seluruh daftar.
	 * Jumlah dihitung dengan query agregat (COUNT) memakai {@code currentSession} sehingga hemat
	 * memori (tidak memuat seluruh baris). Bersifat kosmetik &mdash; bila perhitungan gagal, strip
	 * dilewati tanpa menggagalkan tampilan utama.
	 *
	 * @param parent komponen induk tempat strip kartu disisipkan
	 */
	private void tempelRingkasanTugas(Component parent) {
		if (parent == null || cari == null) {
			return;
		}
		try {
			java.util.Date sekarang = new java.util.Date();
			long total = ((Number) initCriteria(false)
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult()).longValue();
			long lewat = ((Number) initCriteria(false).add(Restrictions.lt("selesai", sekarang))
					.setProjection(org.hibernate.criterion.Projections.rowCount()).uniqueResult()).longValue();
			long aktif = Math.max(0, total - lewat);

			List<ais.ui.util.DashboardUiKit.Stat> kartu = new ArrayList<ais.ui.util.DashboardUiKit.Stat>();
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Total Tugas", String.valueOf(total),
					"Semua tugas kelompok pada cakupan ini", "#2563eb"));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Sedang Berjalan", String.valueOf(aktif),
					"Batas pengumpulan belum lewat", "#16a34a"));
			kartu.add(new ais.ui.util.DashboardUiKit.Stat("Lewat Batas", String.valueOf(lewat),
					"Sudah melewati batas pengumpulan", "#dc2626"));
			parent.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.cards(kartu)));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * <h3>Penyusun kriteria daftar Tugas Kelompok</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menyusun "pertanyaan ke basis data" yang menentukan tugas
	 * kelompok mana saja yang boleh muncul di daftar. Semua penyaring yang sedang aktif &mdash; kata
	 * kunci pencarian dan cakupan layar (perkuliahan, KKN, PKL, atau jadwal pelajaran) &mdash;
	 * digabungkan di sini menjadi satu kriteria. Dipakai bersama oleh tiga pemanggil sehingga daftar,
	 * penghitung halaman, dan kartu ringkasan di atas daftar selalu memakai penyaring yang sama persis.</p>
	 *
	 * <h4>Semua penyaring bersifat MENAMBAH (AND), bukan memilih salah satu</h4>
	 * <p>Setiap penyaring ditulis dengan pola "bila kosong, jangan menyaring":</p>
	 * <pre>.add(perkuliahan == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("perkuliahan", perkuliahan))</pre>
	 * <p>{@code Restrictions.sqlRestriction("true")} adalah syarat yang selalu benar, jadi penyaring itu
	 * seolah tidak ada. Karena seluruhnya digabung dengan AND, mengisi dua cakupan sekaligus akan
	 * mempersempit hasil, bukan memperluasnya. Penting untuk pasangan tunggal/jamak: bila
	 * {@code perkuliahan} DAN {@code perkuliahans} sama-sama terisi, hasilnya adalah tugas yang memenuhi
	 * KEDUANYA &mdash; bukan gabungan keduanya. Delapan penyaring diterapkan berurutan: kata kunci,
	 * jadwal pelajaran, perkuliahan, kelompok KKN, kelompok PKL, ketiga varian jamaknya, lalu SQL
	 * tambahan.</p>
	 *
	 * <h4>Pencarian kata kunci</h4>
	 * <p>Bila kotak cari tidak kosong, kata kunci dicocokkan secara tidak peka huruf besar-kecil dan di
	 * mana saja di dalam teks ({@code MatchMode.ANYWHERE}) pada tiga kolom sekaligus: {@code judul},
	 * {@code nama} (isi instruksi), dan {@code keterangan}, digabung dengan OR. Nilai kata kunci
	 * diteruskan sebagai parameter terikat oleh Hibernate, sehingga karakter khusus apa pun yang diketik
	 * pengguna aman dan tidak dapat mengubah struktur kueri.</p>
	 *
	 * <h4>Dua catatan penting bagi pemelihara</h4>
	 * <ol>
	 *   <li><b>Tidak ada pembatas kepemilikan atau unit kerja.</b> Kriteria ini tidak pernah menanyakan
	 *   siapa yang sedang login, dan tidak menyaring berdasarkan dosen pengampu, program studi, maupun
	 *   satuan kerja. Pembatasan sepenuhnya bergantung pada cakupan yang diisi pemanggil: semua
	 *   pemanggil di dalam kode selalu mengisi salah satu dari perkuliahan, kelompok KKN, kelompok PKL,
	 *   atau jadwal pelajaran, sehingga hasilnya selalu terbatas pada satu kelas. Namun bila seluruh
	 *   field cakupan dibiarkan kosong, kriteria ini akan mencakup SELURUH tabel tugas kelompok lintas
	 *   kelas dan lintas program studi. Setiap pemanggil baru wajib memastikan cakupannya terisi.</li>
	 *   <li><b>{@code sqlTambahan} ditempelkan mentah.</b> Berbeda dari penyaring lain, isinya menjadi
	 *   bagian klausa WHERE apa adanya tanpa parameterisasi. Ia hanya boleh diisi potongan SQL yang
	 *   ditulis tetap di dalam kode, TIDAK PERNAH nilai dari masukan pengguna atau parameter URL.
	 *   Satu-satunya jalur pengisinya, {@link #display(String, Component)}, pada revisi ini tidak
	 *   dipanggil dari mana pun sehingga jalur ini praktis tidak aktif.</li>
	 * </ol>
	 *
	 * <p><b>Selalu kriteria baru.</b> Setiap pemanggilan membangun objek {@link Criteria} yang baru di
	 * atas {@code HibernateUtil.currentSession()}. Ini memang diperlukan: objek {@code Criteria}
	 * Hibernate tidak dapat dipakai ulang setelah dieksekusi, dan {@link #loadData(Object)} maupun
	 * {@link #tempelRingkasanTugas(Component)} perlu menjalankan beberapa kueri berbeda (hitung jumlah,
	 * hitung yang lewat batas, ambil satu halaman) di atas penyaring yang sama.</p>
	 *
	 * <p><b>Prasyarat pemanggilan.</b> Field {@code cari} dibaca tanpa penjagaan {@code null}, sehingga
	 * metode ini hanya boleh dipanggil setelah salah satu varian {@code display(...)} membuat kotak
	 * pencarian. Kedua pemanggil internal sudah menjaga hal ini dengan memeriksa {@code cari == null}
	 * lebih dulu.</p>
	 *
	 * @param order {@code true} untuk menambahkan pengurutan tampilan (tanggal mulai terbaru lebih dulu,
	 *              lalu id terbesar sebagai pemecah seri agar urutan stabil antar-halaman);
	 *              {@code false} untuk kriteria tanpa urutan &mdash; dipakai oleh kueri penghitungan
	 *              ({@code COUNT}), yang tidak memerlukan pengurutan dan pada sebagian basis data justru
	 *              menolak kolom urut yang tidak ikut diagregasi
	 * @return kriteria siap pakai; pemanggil masih boleh menambahkan pembatas atau proyeksi sendiri
	 * @see #loadData(Object)
	 * @see #tempelRingkasanTugas(Component)
	 */
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria crit = session.createCriteria(TugasKelompok.class)
				.add(cari.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(Restrictions.ilike("judul", cari.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.ilike("nama", cari.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("keterangan", cari.getValue().trim(), MatchMode.ANYWHERE))))

				.add(jadwalPelajaran == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))

				.add(perkuliahan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("perkuliahan", perkuliahan))
				.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokKkn", kelompokKkn))
				.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kelompokPkl", kelompokPkl))

				.add(perkuliahans == null || perkuliahans.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("perkuliahan", perkuliahans))
				.add(kelompokKkns == null || kelompokKkns.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("kelompokKkn", kelompokKkns))
				.add(kelompokPkls == null || kelompokPkls.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("kelompokPkl", kelompokPkls))

				.add(sqlTambahan == null || sqlTambahan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.sqlRestriction(sqlTambahan));

		if (order) {
			crit.addOrder(Order.desc("mulai")).addOrder(Order.desc("id"));
		}
		return crit;
	}

	/**
	 * <h3>Memuat ulang satu halaman daftar Tugas Kelompok</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> mengambil data terbaru dari basis data lalu menggambar
	 * ulang isi daftar. Inilah yang dijalankan setiap kali pengguna menekan Cari, menekan Refresh,
	 * berpindah halaman, atau setelah sebuah tugas ditambah, diubah, dipindah, maupun dihapus &mdash;
	 * sehingga layar selalu menampilkan keadaan terkini tanpa perlu memuat ulang seluruh halaman.</p>
	 *
	 * <p>Metode ini adalah pemenuhan kontrak {@link ais.common.listener.DataLoader}, yang memungkinkan
	 * layar lain memicu pemuatan ulang tanpa perlu mengetahui isi kelas ini. Parameter {@code value}
	 * merupakan bagian dari kontrak tersebut dan <b>sengaja diabaikan</b>: seluruh penyaring sudah
	 * tersimpan sebagai state helper dan dibaca ulang lewat {@link #initCriteria(boolean)}, jadi seluruh
	 * pemanggil di dalam kelas ini meneruskan {@code null}.</p>
	 *
	 * <h4>Penjagaan "layar belum siap"</h4>
	 * <p>Bila {@code cari} masih {@code null}, metode langsung keluar tanpa melakukan apa pun. Kotak
	 * pencarian baru dibuat oleh {@code display(...)}, sedangkan {@code initCriteria} membacanya tanpa
	 * penjagaan {@code null}; keluar lebih awal mencegah {@code NullPointerException} bila pemuatan
	 * dipicu sebelum layar selesai dirakit &mdash; misalnya oleh callback yang tiba lebih dulu.</p>
	 *
	 * <h4>Hemat memori: hanya satu halaman yang diambil</h4>
	 * <p>Pemuatan berlangsung dua langkah. Pertama {@code Common.initPaging1} menjalankan kueri
	 * penghitungan ({@code COUNT}) di atas kriteria tanpa urutan untuk menentukan jumlah halaman.
	 * Kedua, baris diambil dengan {@code setMaxResults} sebesar satu halaman dan {@code setFirstResult}
	 * sebesar nomor halaman aktif dikali ukuran halaman. Dengan begitu hanya baris yang benar-benar
	 * terlihat yang masuk ke memori &mdash; seluruh tabel TIDAK pernah dimuat, berapa pun banyaknya
	 * tugas kelompok yang ada. Bila {@code paging} belum terpasang, halaman aktif dianggap 0 sehingga
	 * yang tampil adalah halaman pertama.</p>
	 *
	 * <p><b>Renderer selalu baru.</b> Sebuah {@link DetailPerkuliahanRenderer} baru dibuat pada setiap
	 * pemuatan, bukan dipakai ulang. Ini disengaja dan memiliki akibat yang terlihat pengguna: field
	 * {@code kemarin} milik renderer (acuan waktu untuk menilai "belum mulai" / "telah selesai") ikut
	 * disegarkan, sehingga status tugas ikut mutakhir setiap kali daftar dimuat ulang. Sebaliknya,
	 * halaman yang dibiarkan terbuka lama tanpa dimuat ulang akan mempertahankan status lamanya.</p>
	 *
	 * <p>Model dipasang lewat {@code setModelCheckMobile} agar tata letak grid otomatis menyesuaikan
	 * perangkat, dan kelas gaya grid disetel ulang setiap kali karena pemasangan model dapat
	 * menggantikannya.</p>
	 *
	 * @param value tidak dipakai; ada semata-mata untuk memenuhi antarmuka
	 *              {@link ais.common.listener.DataLoader}. Boleh {@code null}.
	 * @see #initCriteria(boolean)
	 * @see DetailPerkuliahanRenderer#render(Row, Object)
	 */
	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		if (cari == null) {
			return;
		}

		Common.initPaging1(initCriteria(false), paging);

		List<TugasKelompok> tugasKelompok = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE_1)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE_1 * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(tugasKelompok);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);
		grid.setSclass("fgrid ais-data-grid");
	}

	/**
	 * <h3>Mode RINCI &mdash; menampilkan SATU tugas kelompok saja</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menampilkan satu tugas kelompok tertentu secara utuh di
	 * dalam halaman lain &mdash; misalnya di halaman detail sebuah pertemuan perkuliahan. Yang muncul
	 * bukan daftar berisi banyak tugas, melainkan satu kartu tugas itu saja, lengkap dengan seluruh
	 * kontrolnya: instruksi, unggah/unduh berkas, kelompok dan anggota, serta penilaian.</p>
	 *
	 * <p>Perbedaannya dengan varian {@code display(...)} bukan pada tampilan kartunya &mdash; keduanya
	 * memakai {@link DetailPerkuliahanRenderer} yang sama persis &mdash; melainkan pada apa yang
	 * mengelilinginya. Mode rinci tidak membangun header modul, toolbar, kotak pencarian, tombol Tambah,
	 * kartu ringkasan, maupun paging, karena semua itu milik halaman induk yang menampungnya.</p>
	 *
	 * <h4>Tiga akibat dari menyetel {@code tampilRinci = false}</h4>
	 * <ol>
	 *   <li><b>Informasi perkuliahan tambahan dilewati.</b> Di dalam {@code render}, blok dosen pengampu,
	 *   riwayat revisi, prasyarat mata kuliah, serta jadwal hari/jam/ruangan hanya dibangun bila
	 *   {@code tampilRinci} bernilai benar. Pada mode ini semuanya dilewati karena halaman induk sudah
	 *   menampilkan informasi tersebut &mdash; menampilkannya lagi hanya akan mengulang.</li>
	 *   <li><b>Perilaku setelah menyimpan berubah.</b> Setelah {@link #onSave(Event)} berhasil,
	 *   {@link #init(TugasKelompok)} tidak memanggil {@link #loadData(Object)} (tidak ada daftar untuk
	 *   dimuat ulang), melainkan membersihkan komponen induk lalu memanggil metode ini kembali sehingga
	 *   kartu tunggal digambar ulang dengan data terbaru.</li>
	 *   <li><b>Daftar tidak pernah dimuat.</b> Model grid diisi langsung dengan sebuah daftar berisi
	 *   satu objek yang sudah ada di tangan, tanpa menjalankan kueri apa pun. Itulah sebabnya
	 *   {@code initCriteria} dan {@code loadData} tidak terlibat sama sekali di jalur ini.</li>
	 * </ol>
	 *
	 * <p><b>Penyalinan cakupan dari objek tugas.</b> Empat field cakupan ({@code perkuliahan},
	 * {@code kelompokKkn}, {@code kelompokPkl}, {@code jadwalPelajaran}) diisi dari tugas yang diterima,
	 * bukan dari parameter. Ini diperlukan agar cabang penilaian di dalam {@code render} memilih dunia
	 * yang benar (format nilai perkuliahan versus rantai penilaian sekolah), dan agar formulir
	 * "Instruksi Tugas Kelompok" yang dibuka dari kartu ini menyembunyikan pemilih perkuliahan karena
	 * cakupannya sudah tertentu.</p>
	 *
	 * <p><b>Tinggi grid di perangkat bergerak.</b> Pada ponsel, baris pembungkus diberi tinggi tetap yang
	 * sangat besar. Ini kompensasi atas keterbatasan tata letak ZK 5 di dalam wadah yang dapat digulir:
	 * tanpa tinggi eksplisit, isi kartu yang panjang akan terpotong. Nilainya sengaja dilebihkan; ruang
	 * berlebih tidak terlihat karena wadah induk hanya menggulir sejauh isinya.</p>
	 *
	 * <p><b>Ukuran halaman grid tidak relevan di sini.</b> Grid tetap dipasang bermold {@code paging}
	 * dengan ukuran halaman 50 demi keseragaman dengan mode daftar, tetapi karena modelnya hanya berisi
	 * satu baris, kendali halaman tidak pernah benar-benar tampil.</p>
	 *
	 * @param tugasKelompok tugas yang akan ditampilkan; menjadi satu-satunya isi model grid dan juga
	 *                      sumber keempat field cakupan
	 * @param component     komponen induk tempat kartu ditanam; disimpan ke field {@code component} agar
	 *                      dapat dibersihkan dan digambar ulang setelah penyimpanan berhasil
	 * @param eventListener callback yang dijalankan setiap kali data berubah, agar halaman induk dapat
	 *                      menyegarkan dirinya sendiri; boleh {@code null}
	 * @see #display(Perkuliahan, KelompokKkn, KelompokPkl, JadwalPelajaran, Component)
	 */
	public void tampilanTugas(TugasKelompok tugasKelompok, Component component, EventListener eventListener) {
		this.tugasKelompok = tugasKelompok;
		this.eventListener = eventListener;
		this.component = component;

		tampilRinci = false;

		Row rowUtama;

		if (Common.isMobile()) {
			rowUtama = Common.tampilanScroll(component);
			rowUtama.setValign("top");
			rowUtama.setHeight("20000px");
		} else {
			rowUtama = Common.tampilanScroll1(component);
		}

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowUtama);

		Columns columns = new Columns();
		columns.setParent(grid);

		this.perkuliahan = tugasKelompok.getPerkuliahan();
		this.kelompokKkn = tugasKelompok.getKelompokKkn();
		this.kelompokPkl = tugasKelompok.getKelompokPkl();
		this.jadwalPelajaran = tugasKelompok.getJadwalPelajaran();

		List<TugasKelompok> tugasKelompoks = new ArrayList<TugasKelompok>();
		tugasKelompoks.add(tugasKelompok);
		ListModel strset = new SimpleListModel(tugasKelompoks);
		grid.setRowRenderer(new DetailPerkuliahanRenderer());
		grid.setModelCheckMobile(strset);
		grid.setSclass("fgrid ais-data-grid");
	}

	/**
	 * <h3>Titik masuk generik &mdash; menerima wadah pembelajaran apa pun</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuka layar Tugas Kelompok ketika pemanggil belum tahu
	 * jenis wadah pembelajaran yang sedang dibuka. Wadah itu bisa berupa perkuliahan, kelompok KKN,
	 * kelompok PKL, atau jadwal pelajaran sekolah; metode ini mengenali jenisnya sendiri lalu mengisi
	 * field cakupan yang sesuai. Dipakai oleh menu generik yang menampilkan tab e-learning tanpa
	 * bercabang menurut modul.</p>
	 *
	 * <p>Pengenalan jenis memakai {@code instanceof} berurutan, dan wadah yang tidak dikenali dibiarkan
	 * tanpa cakupan sama sekali &mdash; tidak ada galat yang dilempar. Perhatikan akibatnya: cakupan
	 * yang kosong TIDAK menghasilkan daftar kosong, melainkan daftar tanpa penyaring (lihat
	 * {@link #initCriteria(boolean)}).</p>
	 *
	 * <p>Metode ini meneruskan pekerjaannya ke
	 * {@link #display(Perkuliahan, KelompokKkn, KelompokPkl, Component)}, yang meneruskan cakupan jadwal
	 * pelajaran yang sudah tersimpan di field ke varian kanonik &mdash; sehingga cabang
	 * {@code JadwalPelajaran} di atas tidak kehilangan cakupannya.</p>
	 *
	 * @param voPembelajaran wadah pembelajaran yang sedang dibuka; boleh {@code null} atau bertipe lain
	 *                       (cakupan tidak diisi)
	 * @param component      komponen induk tempat layar dirakit
	 * @see #display(Perkuliahan, KelompokKkn, KelompokPkl, JadwalPelajaran, Component)
	 */
	public void display(VOPembelajaran voPembelajaran, final Component component) {

		if (voPembelajaran != null && (voPembelajaran instanceof Perkuliahan)) {
			this.perkuliahan = (Perkuliahan) voPembelajaran;
		} else if (voPembelajaran != null && (voPembelajaran instanceof KelompokKkn)) {
			this.kelompokKkn = (KelompokKkn) voPembelajaran;
		} else if (voPembelajaran != null && (voPembelajaran instanceof KelompokPkl)) {
			this.kelompokPkl = (KelompokPkl) voPembelajaran;
		} else if (voPembelajaran != null && (voPembelajaran instanceof JadwalPelajaran)) {
			this.jadwalPelajaran = (JadwalPelajaran) voPembelajaran;
		}

		display(perkuliahan, kelompokKkn, kelompokPkl, component);
	}

	/**
	 * <h3>Titik masuk jalur SEKOLAH &mdash; daftar tugas kelompok satu jadwal pelajaran</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuka daftar tugas kelompok milik satu mata pelajaran di
	 * satu kelas. Selain membatasi daftar, cakupan ini juga menentukan bahwa penilaian memakai rantai
	 * penilaian sekolah, bukan format nilai perkuliahan.</p>
	 *
	 * <p>Metode ini menyimpan jadwal pelajaran ke field, lalu meneruskan pekerjaannya ke
	 * {@link #display(Perkuliahan, KelompokKkn, KelompokPkl, Component)}, yang meneruskan cakupan
	 * jadwal pelajaran yang baru saja disetel ke varian kanonik &mdash; sehingga cakupan tetap terjaga.</p>
	 *
	 * @param jadwalPelajaran jadwal pelajaran (mata pelajaran pada satu kelas) yang menjadi cakupan
	 * @param component       komponen induk tempat layar dirakit
	 * @see #display(Perkuliahan, KelompokKkn, KelompokPkl, JadwalPelajaran, Component)
	 */
	public void display(JadwalPelajaran jadwalPelajaran, final Component component) {
		this.jadwalPelajaran = jadwalPelajaran;
		display(perkuliahan, kelompokKkn, kelompokPkl, component);
	}

	/**
	 * <h3>Titik masuk dengan penyaring SQL bebas &mdash; tidak dipakai pada revisi ini</h3>
	 *
	 * <p>Menyetel {@code sqlTambahan} lalu membuka layar seperti biasa. Potongan SQL itu ditempelkan apa
	 * adanya ke klausa WHERE oleh {@link #initCriteria(boolean)}, sehingga menjadi SATU-SATUNYA pembatas
	 * daftar bila tidak ada cakupan lain yang terisi.</p>
	 *
	 * <p><b>Peringatan keamanan.</b> Isi {@code sqlTambahan} TIDAK diparameterkan dan TIDAK divalidasi.
	 * Metode ini hanya boleh dipanggil dengan potongan SQL yang ditulis tetap di dalam kode &mdash;
	 * TIDAK PERNAH dengan nilai yang berasal dari masukan pengguna, parameter URL, atau isi basis data,
	 * karena hal itu membuka celah penyisipan perintah SQL. Penelusuran seluruh kode sumber pada revisi
	 * ini tidak menemukan satu pun pemanggil, jadi jalur ini praktis tidak aktif; sebelum menghidupkannya
	 * kembali, pertimbangkan mengganti pendekatannya dengan kriteria Hibernate yang terparameter.</p>
	 *
	 * <p>Sama seperti saudara-saudaranya, metode ini meneruskan pekerjaan lewat varian berargumen empat,
	 * yang menjaga agar cakupan jadwal pelajaran yang mungkin sudah terisi tetap dipertahankan.</p>
	 *
	 * @param sqlTambahan potongan SQL literal untuk klausa WHERE; harus berasal dari kode, bukan pengguna
	 * @param component   komponen induk tempat layar dirakit
	 */
	public void display(String sqlTambahan, final Component component) {
		this.sqlTambahan = sqlTambahan;
		display(perkuliahan, kelompokKkn, kelompokPkl, component);
	}

	/**
	 * <h3>Titik masuk GABUNGAN &mdash; banyak kelas sekaligus dalam satu daftar</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menampilkan tugas kelompok dari beberapa kelas sekaligus
	 * dalam satu daftar &mdash; misalnya seluruh kelas yang diampu seorang dosen, atau seluruh kelompok
	 * KKN dan PKL yang dibimbingnya &mdash; sehingga ia tidak perlu membuka kelasnya satu per satu.</p>
	 *
	 * <p>Ketiga daftar bersifat saling melengkapi dan boleh diisi bersamaan; masing-masing menjadi
	 * penyaring {@code IN} tersendiri di {@link #initCriteria(boolean)}. Daftar yang {@code null} atau
	 * kosong tidak menyaring apa pun. Karena seluruh penyaring digabung dengan AND, mengisi dua daftar
	 * sekaligus akan mempersempit hasil, bukan menggabungkan keduanya &mdash; perilaku yang perlu
	 * diperhatikan bila kelak diinginkan tampilan gabungan lintas modul yang sesungguhnya.</p>
	 *
	 * <p>Parameter induknya sengaja bertipe {@link Tabpanel}, bukan {@link Component} umum, karena mode
	 * gabungan memang ditujukan untuk tampilan tab penuh: hanya induk bertipe {@code Tabpanel} yang
	 * memicu perakitan header modul, kartu ringkasan, toolbar, dan paging pada varian kanonik.</p>
	 *
	 * @param perkuliahans daftar perkuliahan yang digabungkan; boleh {@code null} atau kosong
	 * @param kelompokKkns daftar kelompok KKN yang digabungkan; boleh {@code null} atau kosong
	 * @param kelompokPkls daftar kelompok PKL yang digabungkan; boleh {@code null} atau kosong
	 * @param component    panel tab tempat layar dirakit
	 */
	public void display(final List<Perkuliahan> perkuliahans, final List<KelompokKkn> kelompokKkns,
			final List<KelompokPkl> kelompokPkls, final Tabpanel component) {
		this.perkuliahans = perkuliahans;
		this.kelompokKkns = kelompokKkns;
		this.kelompokPkls = kelompokPkls;
		display(perkuliahan, kelompokKkn, kelompokPkl, component);
	}

	/**
	 * <h3>Varian ringkas tanpa jadwal pelajaran (jalur perguruan tinggi)</h3>
	 *
	 * <p>Kemudahan bagi pemanggil jalur perguruan tinggi (perkuliahan, KKN, PKL) yang tidak berurusan
	 * dengan modul sekolah, sehingga tidak perlu menuliskan argumen jadwal pelajaran. Seluruh pekerjaan
	 * diteruskan ke {@link #display(Perkuliahan, KelompokKkn, KelompokPkl, JadwalPelajaran, Component)}.</p>
	 *
	 * <p>Metode ini meneruskan cakupan jadwal pelajaran yang sudah tersimpan di field ({@code
	 * this.jadwalPelajaran}) ke varian kanonik, bukan {@code null}, sehingga keempat varian perantara
	 * yang bermuara ke sini tidak kehilangan cakupan sekolahnya. Pemanggil jalur perguruan tinggi yang
	 * memakai metode ini langsung pada helper yang baru dibangun tidak terpengaruh, karena
	 * {@code jadwalPelajaran} masih {@code null} pada saat itu.</p>
	 *
	 * @param perkuliahan  cakupan perkuliahan, atau {@code null}
	 * @param kelompokKkn  cakupan kelompok KKN, atau {@code null}
	 * @param kelompokPkl  cakupan kelompok PKL, atau {@code null}
	 * @param component    komponen induk tempat layar dirakit
	 */
	public void display(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final Component component) {
		display(perkuliahan, kelompokKkn, kelompokPkl, jadwalPelajaran, component);
	}

	/**
	 * <h3>Tombol "Ambil Tugas Sebelumnya" &mdash; menyalin tugas dari kelas/semester lain</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membuat tombol yang memungkinkan dosen/guru menyalin tugas
	 * kelompok yang pernah ia buat sebelumnya ke kelas yang sedang dibuka, alih-alih mengetik ulang judul,
	 * instruksi, dan lampirannya dari nol. Sangat menghemat waktu untuk mata kuliah yang diampu berulang
	 * setiap semester.</p>
	 *
	 * <p>Metode ini hanya MERAKIT tombolnya dan mengembalikannya; pemanggil yang menentukan di toolbar
	 * mana tombol itu dipasang. Kedua cabang tata letak pada varian kanonik {@code display(...)}
	 * memakainya, sehingga perilakunya seragam pada mode tab maupun mode sisip.</p>
	 *
	 * <h4>Alur penyalinan</h4>
	 * <ol>
	 *   <li>Jendela pemilih {@link ais.action.master.helper.generic.AmbilDataTugasKelompok} dibuka secara
	 *   modal. Daftar sumbernya disaring dengan {@code createCriteriaDosen} sehingga hanya memuat tugas
	 *   milik dosen yang bersangkutan &mdash; termasuk tugas dari semester-semester sebelumnya.</li>
	 *   <li>Setelah sebuah tugas dipilih, objeknya di-<i>clone</i>, {@code id} dikosongkan, field-field
	 *   yang tidak boleh diwarisi dari semester/kelas SUMBER direset (lihat di bawah), keempat field
	 *   cakupan diarahkan ke kelas yang sedang dibuka (dengan jatuh kembali ke cakupan asal bila layar
	 *   ini sendiri tidak punya cakupan), lalu disimpan sebagai baris baru.</li>
	 *   <li>Lampiran instruksi ikut disalin lewat sesi streaming terpisah, dan daftar dimuat ulang di
	 *   dalam timer sehingga jendela pemilih sempat menutup lebih dulu.</li>
	 * </ol>
	 *
	 * <h4>Penyalinan bersifat DANGKAL &mdash; field warisan direset eksplisit</h4>
	 * <p>{@code clone()} yang dipakai diwarisi dari {@code GeneralValueObject} dan merupakan salinan
	 * dangkal: SELURUH field disalin apa adanya oleh {@code super.clone()}. Karena itu, sesudah
	 * {@code clone()} dan {@code setId(null)}, listener ini secara eksplisit mengosongkan field yang
	 * masih melekat pada tugas semester LAMA sebelum keempat field cakupan ditulis ulang:</p>
	 * <ul>
	 *   <li><b>{@code keteranganNilai}</b> dan <b>{@code keteranganNilaiLama}</b> &mdash; blob JSON nilai
	 *   per anggota. Tanpa direset, mahasiswa yang MENGULANG mata kuliah dan hadir di kedua kelas akan
	 *   langsung melihat nilai lamanya tampak sebagai nilai yang sudah terisi pada tugas hasil salinan.
	 *   Mengosongkannya aman: {@link ais.database.model.TugasKelompok#getKeteranganNilai()} jatuh kembali
	 *   ke {@link ais.database.model.TugasKelompok#DEFAULT_FORMULA} ({@code "{}"}) bila kosong.</li>
	 *   <li><b>{@code mhsYgTidakIkut}</b> dan <b>{@code mhsBolehUploadUlang}</b> &mdash; daftar id peserta
	 *   yang dikecualikan/diberi izin unggah ulang. Tanpa direset, id yang kebetulan juga terdaftar di
	 *   kelas baru akan ikut terpengaruh tanpa pernah dicentang siapa pun.</li>
	 *   <li><b>{@code pertemuan}</b> dan <b>{@code pertemuanData}</b> &mdash; rujukan ke pertemuan
	 *   semester LAMA. Tanpa direset, {@code ambilPertemuan()} pada tugas baru masih menunjuk ke sana,
	 *   dan tombol "Anggap Hadir (Pengumpul)" / "Tdk Upload = Alpa" akan menulis absensi ke pertemuan
	 *   semester lama itu.</li>
	 *   <li><b>{@code mulai} dan {@code selesai}</b> &mdash; jadwal semester lama. Dikosongkan sehingga
	 *   dosen/guru wajib menyetel jadwal baru lewat formulir Instruksi, alih-alih diam-diam mewarisi
	 *   jadwal yang umumnya sudah lewat (berstatus "Sudah ditutup").</li>
	 *   <li><b>{@code formatNilai}</b>, <b>{@code jenisItemPenilaianSiswa}</b>, <b>{@code grupPenilaian}</b>,
	 *   dan <b>{@code grupKategoriItemPenilaianSiswa}</b> &mdash; rujukan komponen penilaian milik
	 *   perkuliahan/jadwal pelajaran SUMBER. Dikosongkan karena komponen itu bisa saja tidak berlaku (atau
	 *   tidak ada) pada kelas tujuan; dosen/guru memilih ulang lewat formulir Instruksi.</li>
	 * </ul>
	 *
	 * <h4>Penanganan lampiran</h4>
	 * <p>Sebelum lampiran baru dibuat, dijalankan perintah SQL yang memindahkan lampiran mana pun yang
	 * sudah menempel pada id tugas baru ke penanda khusus {@code -111111111111}. Langkah ini menjaga
	 * agar tugas hasil salinan tidak mewarisi lampiran nyasar bila id yang baru saja diberikan basis data
	 * pernah dipakai tugas lain di masa lalu. Id disambungkan ke teks perintah, tetapi nilainya berasal
	 * dari kunci utama bertipe angka yang baru saja dihasilkan basis data, bukan dari masukan pengguna,
	 * sehingga tidak membuka celah penyisipan perintah. Sesi streaming SELALU ditutup di blok
	 * {@code finally} agar tidak ada koneksi yang bocor walau penyalinan gagal.</p>
	 *
	 * <p>Salinan lampiran mencatat jejak audit sendiri: tanggal diubah, id pelaku, dan nama pelaku yang
	 * dipilih berurutan dari mahasiswa, dosen, pegawai, lalu nama pengguna &mdash; dengan
	 * {@code "external_update"} sebagai penanda bila tidak ada pengguna yang login.</p>
	 *
	 * <h4>Visibilitas tombol</h4>
	 * <p>Tombol disembunyikan bagi pelajar memakai pemeriksaan peran gaya lama, bukan
	 * {@link #bolehKelola(Tbmuser)}. Bentuk itu tidak menguji {@code tbmuser.getMahasiswa()} dan memuat
	 * suku {@code tbmuser.getSiswa() == null} dua kali &mdash; sisa salin-tempel yang semestinya menguji
	 * mahasiswa. Selain itu {@code tbmuser} dipakai tanpa penjagaan {@code null} lebih dulu, sehingga
	 * sesi tanpa pengguna akan melempar {@code NullPointerException}. Perhatikan pula bahwa ini hanya
	 * menyembunyikan tombol: komponennya tetap dibuat dan listenernya tetap terdaftar.</p>
	 *
	 * @return tombol "Ambil Tugas Sebelumnya" yang siap ditempelkan ke toolbar; sudah lengkap dengan
	 *         pengaturan visibilitas dan listenernya
	 * @see ais.action.master.helper.generic.AmbilDataTugasKelompok
	 */
	public MyToolbarbutton createAmbilTugas() {
		MyToolbarbutton ambil = new MyToolbarbutton("fa-history", "Ambil Tugas Sebelumnya");
		ambil.setTooltiptext("Salin tugas dari semester/pertemuan sebelumnya ke sini");
		Tbmuser tbmuser = Common.getCurrentUser();
		ambil.setVisible(bolehKelola(tbmuser));
		ambil.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final AmbilDataTugasKelompok ambilDataLampiranFileLain = new AmbilDataTugasKelompok(perkuliahan);

				ambilDataLampiranFileLain.setHeight("95%");
				ambilDataLampiranFileLain.setWidth("90%");
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataLampiranFileLain);
				ambilDataLampiranFileLain.onModal();
				ambilDataLampiranFileLain.setEventListener(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						TugasKelompok tugasKelompokCopy = (TugasKelompok) arg0.getData();
						if (tugasKelompokCopy != null && !tugasKelompokCopy.getJudul().isEmpty()) {
							TugasKelompok tugasKelompok = (TugasKelompok) tugasKelompokCopy.clone();
							tugasKelompok.setId(null);
							// clone() adalah salinan dangkal: field berikut milik semester/kelas SUMBER dan
							// wajib direset, jika tidak nilai lama, pengecualian mahasiswa, dan absensi
							// pertemuan lama ikut terbawa ke tugas baru secara diam-diam.
							tugasKelompok.setKeteranganNilai(null);
							tugasKelompok.setKeteranganNilaiLama(null);
							tugasKelompok.setMhsYgTidakIkut(null);
							tugasKelompok.setMhsBolehUploadUlang(null);
							tugasKelompok.setPertemuan(null);
							tugasKelompok.setPertemuanData(null);
							tugasKelompok.setMulai(null);
							tugasKelompok.setSelesai(null);
							tugasKelompok.setFormatNilai(null);
							tugasKelompok.setJenisItemPenilaianSiswa(null);
							tugasKelompok.setGrupPenilaian(null);
							tugasKelompok.setGrupKategoriItemPenilaianSiswa(null);
							tugasKelompok.setPerkuliahan(
									perkuliahan == null ? tugasKelompokCopy.getPerkuliahan() : perkuliahan);
							tugasKelompok.setKelompokKkn(
									kelompokKkn == null ? tugasKelompokCopy.getKelompokKkn() : kelompokKkn);
							tugasKelompok.setKelompokPkl(
									kelompokPkl == null ? tugasKelompokCopy.getKelompokPkl() : kelompokPkl);
							tugasKelompok.setJadwalPelajaran(
									jadwalPelajaran == null ? tugasKelompokCopy.getJadwalPelajaran() : jadwalPelajaran);

							Session sessiona = HibernateUtil.currentSession();
							sessiona.save(tugasKelompok);
							sessiona.flush();

							LampiranLain lampiranLain = LampiranLain.ambil(tugasKelompokCopy.getId(),
									LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN);

							Session session = StreamingHibernateUtil.getInstance().currentSession();

							try {

								if (lampiranLain != null) {

									session.getTransaction().begin();

									session.createSQLQuery("update lampiran_lain set ref = -111111111111 where ref = "
											+ tugasKelompok.getId() + " and jenis = '"
											+ LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN + "'").executeUpdate();

									session.getTransaction().commit();

									final LampiranLain copy = new LampiranLain();
									copy.setRef(tugasKelompok.getId());
									copy.setCopyDari(lampiranLain);

									Tbmuser tbmuser = Common.getCurrentUser();
									Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
									Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
									Pegawai pegawai = tbmuser == null ? null : tbmuser.ambilPegawai();
									String olehId = Common.generateOlehId(tbmuser);
									copy.setTanggal_dirubah(ais.ui.util.WaktuUtil.getDate());
									copy.setOlehId(olehId);
									copy.setOleh(tbmuser == null ? "external_update"
											: mahasiswa != null ? mahasiswa.getNama()
													: dosen != null ? dosen.getNama()
															: pegawai != null ? pegawai.getNama()
																	: (tbmuser.getUserNama()));

									session.getTransaction().begin();
									session.save(copy);
									session.getTransaction().commit();
								}

							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/TugasKelompokHelper.java:2785");
							} finally {
								// Sesi streaming WAJIB ditutup di finally agar tidak bocor walau terjadi error.
								StreamingHibernateUtil.getInstance().closeSession();
							}

							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									loadData(null);
								}
							});
						}
						ambilDataLampiranFileLain.detach();

					}
				});
			}
		});
		return ambil;
	}

	/**
	 * <h3>Varian KANONIK &mdash; tempat seluruh layar daftar Tugas Kelompok dirakit</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> membangun layar daftar tugas kelompok secara utuh: judul
	 * modul, tiga kartu ringkasan angka, deretan tombol (Tambah Tugas, Ambil Tugas Sebelumnya, Cari,
	 * Refresh, Recovery), tabel daftar tugas, dan kendali halaman. Seluruh varian {@code display(...)}
	 * lain akhirnya bermuara ke sini, sehingga inilah satu-satunya tempat tata letak layar ditentukan.</p>
	 *
	 * <h4>Menugaskan cakupan: keempat argumen selalu ditulis ke field</h4>
	 * <p>Keempat argumen cakupan ditugaskan ke field tanpa syarat, termasuk bila bernilai {@code null}.
	 * Jadi memanggil metode ini bukan hanya "mengisi" cakupan, melainkan <b>menetapkan ulang</b>
	 * seluruhnya: cakupan lama yang tidak disertakan akan terhapus. Karena itu setiap pemanggil yang
	 * ingin mempertahankan cakupan jadwal pelajaran yang sudah tersimpan di field harus meneruskannya
	 * kembali secara eksplisit &mdash; sebagaimana dilakukan varian berargumen empat
	 * {@link #display(Perkuliahan, KelompokKkn, KelompokPkl, Component)}, yang meneruskan
	 * {@code this.jadwalPelajaran} alih-alih {@code null}.
	 * Ketiga field cakupan JAMAK ({@code perkuliahans}, {@code kelompokKkns}, {@code kelompokPkls})
	 * TIDAK disentuh di sini, sehingga nilai yang disetel varian gabungan tetap bertahan.</p>
	 *
	 * <p>Komponen induk dibersihkan lebih dulu dengan {@code Common.clear(component)} bila ada, sehingga
	 * metode ini aman dipanggil berulang &mdash; itulah yang dilakukan tombol Refresh dan callback
	 * Recovery, yang memanggil ulang dirinya sendiri dengan argumen yang sama persis alih-alih sekadar
	 * memuat ulang data.</p>
	 *
	 * <h4>Dua tata letak, dipilih dari TIPE komponen induk</h4>
	 * <p>Pemilihan tata letak tidak memakai parameter khusus, melainkan memeriksa apakah induknya bertipe
	 * {@link Tabpanel}:</p>
	 * <ul>
	 *   <li><b>Mode TAB</b> (induk {@code Tabpanel}) &mdash; tampilan penuh satu tab: header modul
	 *   "Linimasa Tugas Kelompok" dari {@code DashboardUiKit.headerModul}, strip tiga kartu ringkasan
	 *   lewat {@link #tempelRingkasanTugas(Component)}, toolbar, grid, lalu paging. Semuanya ditanam di
	 *   dalam satu {@code MyDiv} bertinggi minimum agar tab tidak terlihat kosong saat data belum
	 *   dimuat.</li>
	 *   <li><b>Mode SISIP</b> (induk lain) &mdash; tampilan ringkas untuk ditanam di dalam halaman
	 *   detail: hanya toolbar, grid, dan paging, dirakit di dalam wadah bergulir
	 *   {@code Common.tampilanScroll1}. Tanpa header modul dan tanpa kartu ringkasan, karena halaman
	 *   induk sudah memiliki judul dan ringkasannya sendiri.</li>
	 * </ul>
	 * <p>Isi toolbar kedua mode sengaja dibuat identik dan dalam urutan yang sama, sehingga pengguna
	 * menemukan tombol yang sama di posisi yang sama. Konsekuensinya, blok pembuatan toolbar tertulis
	 * dua kali; setiap penambahan tombol baru harus dilakukan di KEDUA cabang agar tidak muncul di satu
	 * mode saja.</p>
	 *
	 * <h4>Urutan pembuatan yang tidak boleh diubah</h4>
	 * <p>{@code paging} dan {@code cari} dibuat SEBELUM kedua cabang tata letak, dan {@code loadData}
	 * dipanggil paling akhir setelah kolom grid terpasang. Urutan ini wajib: {@link #initCriteria(boolean)}
	 * membaca {@code cari} tanpa penjagaan {@code null}, {@link #tempelRingkasanTugas(Component)} sudah
	 * memanggil {@code initCriteria} di tengah perakitan mode tab, dan {@code loadData} memerlukan grid
	 * yang sudah ada beserta kolomnya. Listener perpindahan halaman dipasang pada {@code paging} sejak
	 * awal sehingga setiap perpindahan memicu {@code loadData} kembali.</p>
	 *
	 * <h4>Hak akses tombol</h4>
	 * <p>Tombol "Tambah Tugas" memakai {@link #bolehKelola(Tbmuser)} &mdash; bentuk pemeriksaan yang
	 * benar. Tombol "Recovery" (memulihkan tugas kelompok yang terhapus) dijaga
	 * {@code RecoveryAktivitasPembelajaranHelper.bolehTampil(tbmuser)} dan komponennya tidak dibuat sama
	 * sekali bila tidak berhak. Sebaliknya "Ambil Tugas Sebelumnya" memakai pemeriksaan gaya lama di
	 * dalam {@link #createAmbilTugas()}. Tombol Cari dan Refresh sengaja tersedia untuk semua peran
	 * karena keduanya hanya membaca.</p>
	 *
	 * <p><b>Batas yang jujur:</b> seperti seluruh penjagaan di kelas ini, yang dibatasi adalah tombolnya,
	 * bukan datanya. Tugas kelompok mana yang boleh terlihat sepenuhnya ditentukan cakupan yang
	 * ditugaskan di awal metode ini &mdash; tidak ada penyaring kepemilikan tambahan di
	 * {@code initCriteria}. Memanggil metode ini dengan keempat cakupan {@code null} akan menampilkan
	 * seluruh tabel tugas kelompok.</p>
	 *
	 * @param perkuliahan     cakupan perkuliahan, atau {@code null}
	 * @param kelompokKkn     cakupan kelompok KKN, atau {@code null}
	 * @param kelompokPkl     cakupan kelompok PKL, atau {@code null}
	 * @param jadwalPelajaran cakupan jadwal pelajaran (jalur sekolah), atau {@code null}
	 * @param component       komponen induk; bertipe {@link Tabpanel} untuk mode tab, tipe lain untuk
	 *                        mode sisip. Boleh {@code null}, meskipun layar tidak akan tampak.
	 * @see #initCriteria(boolean)
	 * @see #loadData(Object)
	 * @see #tempelRingkasanTugas(Component)
	 */
	public void display(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn, final KelompokPkl kelompokPkl,
			final JadwalPelajaran jadwalPelajaran, final Component component) {
		this.perkuliahan = perkuliahan;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.jadwalPelajaran = jadwalPelajaran;
		if (component != null) {
			Common.clear(component);
		}
		Tbmuser tbmuser = Common.getCurrentUser();
		paging = new Paging();
		Common.initPaging1(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		cari = new Textbox();
		if (component instanceof Tabpanel) {

			ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
			groupbox.setStyle("min-height: 200px;");
			groupbox.setParent(component);
			groupbox.appendChild(ais.ui.util.DashboardUiKit.html(ais.ui.util.DashboardUiKit.headerModul(
					"Linimasa Tugas Kelompok",
					"Semua tugas yang dikerjakan secara berkelompok. Klik sebuah tugas untuk melihat anggota dan nilainya.")));

			tempelRingkasanTugas(groupbox);

			Toolbar toolbar = new Toolbar();
			// toolbar.setHeight("25px");
			toolbar.setParent(groupbox);
			MyToolbarbutton button = new MyToolbarbutton("fa-plus", "Tambah Tugas");
			button.setTooltiptext("Buat tugas kelompok baru untuk cakupan ini");
			button.setVisible(bolehKelola(tbmuser));
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(event, new TugasKelompok());
				}

			});
			button.setParent(toolbar);
			toolbar.appendChild(createAmbilTugas());

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbutton("fa-search", "");
			button.setTooltiptext("Cari tugas");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbutton("fa-refresh", "Refresh");
			button.setTooltiptext("Muat ulang daftar tugas kelompok");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					display(perkuliahan, kelompokKkn, kelompokPkl, jadwalPelajaran, component);
				}
			});
			button.setParent(toolbar);

			if (RecoveryAktivitasPembelajaranHelper.bolehTampil(tbmuser)) {
				button = new MyToolbarbutton("fa-history", "Recovery");
				button.setTooltiptext("Kembalikan tugas kelompok yang terhapus");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						RecoveryAktivitasPembelajaranHelper.bukaRecoveryTugasKelompok(
								perkuliahan != null ? perkuliahan : jadwalPelajaran, new EventListener() {
									@Override
									public void onEvent(Event callbackEvent) throws Exception {
										display(perkuliahan, kelompokKkn, kelompokPkl, jadwalPelajaran, component);
									}
								});
					}
				});
				button.setParent(toolbar);
			}

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(groupbox);

			paging.setParent(groupbox);

		} else {

			Row rowUtama = Common.tampilanScroll1(component);

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(rowUtama);

			MyFormRow rowUtamaLagi = new MyFormRow();
			rowUtamaLagi.setParent(rowUtama.getParent());

			grid = new MyGrid();// grid.setOddRowSclass("non-odd");
			grid.setWidth("100%");
			grid.setMold("paging");
			grid.setPageSize(50);
			grid.getPagingChild().setMold("os");
			grid.setParent(rowUtamaLagi);

			rowUtamaLagi = new MyFormRow();
			rowUtamaLagi.setParent(rowUtama.getParent());

			paging.setParent(rowUtamaLagi);

			MyToolbarbutton button = new MyToolbarbutton("fa-plus", "Tambah Tugas");
			button.setTooltiptext("Buat tugas kelompok baru untuk cakupan ini");
			button.setVisible(bolehKelola(tbmuser));
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					onAdd(event, new TugasKelompok());
				}

			});
			button.setParent(toolbar);

			toolbar.appendChild(createAmbilTugas());

			toolbar.appendChild(new Space());
			toolbar.appendChild(new MyLabelConfig("Cari : "));
			toolbar.appendChild(cari);
			cari.setCols(15);
			cari.addEventListener("onOK", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});
			button = new MyToolbarbutton("fa-search", "");
			button.setTooltiptext("Cari tugas");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					loadData(null);
				}
			});
			button.setParent(toolbar);

			button = new MyToolbarbutton("fa-refresh", "Refresh");
			button.setTooltiptext("Muat ulang daftar tugas kelompok");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					display(perkuliahan, kelompokKkn, kelompokPkl, jadwalPelajaran, component);
				}
			});
			button.setParent(toolbar);

			if (RecoveryAktivitasPembelajaranHelper.bolehTampil(tbmuser)) {
				button = new MyToolbarbutton("fa-history", "Recovery");
				button.setTooltiptext("Kembalikan tugas kelompok yang terhapus");
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						RecoveryAktivitasPembelajaranHelper.bukaRecoveryTugasKelompok(
								perkuliahan != null ? perkuliahan : jadwalPelajaran, new EventListener() {
									@Override
									public void onEvent(Event callbackEvent) throws Exception {
										display(perkuliahan, kelompokKkn, kelompokPkl, jadwalPelajaran, component);
									}
								});
					}
				});
				button.setParent(toolbar);
			}

		}

		Columns columns = new Columns();
		columns.setParent(grid);

		loadData(null);

	}

	/**
	 * <h3>Membuka formulir Tugas Kelompok dari layar LAIN, tanpa perlu punya helper</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> memungkinkan halaman lain &mdash; yang tidak menampilkan
	 * daftar tugas kelompok sama sekali &mdash; tetap dapat membuka formulir "Instruksi Tugas Kelompok".
	 * Dipakai oleh layar rekapitulasi pertemuan, tempat pengguna memilih membuat tugas kelompok pada
	 * sebuah pertemuan tanpa berpindah halaman. Karena bersifat statis, pemanggil cukup menyediakan data
	 * yang diperlukan; helper dibuat sendiri di dalam metode ini lalu dibuang setelah jendela ditutup.</p>
	 *
	 * <p>Perbedaannya dengan {@link #onAdd(Event, TugasKelompok)} hanya pada kepemilikan helper:
	 * {@code onAdd} memakai helper yang sudah menampilkan daftar, sedangkan metode ini membuat helper
	 * sekali pakai. Jendela dirakit langsung (bukan lewat {@code onAdd}) karena ukurannya berbeda &mdash;
	 * 90% lebar, mengikuti kebutuhan layar pemanggil.</p>
	 *
	 * <h4>PERINGATAN: helper sekali pakai ini TIDAK punya cakupan</h4>
	 * <p>Helper dibuat tanpa memanggil {@code display(...)}, sehingga keempat field cakupannya
	 * ({@code perkuliahan}, {@code kelompokKkn}, {@code kelompokPkl}, {@code jadwalPelajaran}) tetap
	 * {@code null}. Ini berakibat nyata pada penyimpanan, karena {@link #onSave(Event)} menuliskan
	 * keempat field itu ke entity <b>tanpa syarat</b>:</p>
	 * <ul>
	 *   <li>Perkuliahan selamat, karena diambil dari pemilih {@code banboxPerkuliahan} yang diisi
	 *   {@link #init(TugasKelompok)} dari objek tugas &mdash; bukan dari field cakupan.</li>
	 *   <li>Kelompok KKN, kelompok PKL, dan jadwal pelajaran <b>ditimpa dengan {@code null}</b>. Jadi
	 *   bila pemanggil sudah menyetel salah satunya pada objek tugas sebelum memanggil metode ini,
	 *   nilai itu akan hilang saat pengguna menekan Simpan. Pemanggil yang perlu mempertahankan salah
	 *   satu cakupan tersebut sebaiknya memakai instance helper yang cakupannya sudah benar lalu
	 *   memanggil {@link #onAdd(Event, TugasKelompok)}.</li>
	 * </ul>
	 *
	 * <h4>Parameter yang tidak dipakai</h4>
	 * <p>Parameter {@code event} dan {@code eventListener} <b>tidak dibaca sama sekali</b> di dalam badan
	 * metode. Khusus {@code eventListener}, ini perangkap tersembunyi: pemanggil mengira menyerahkan
	 * callback penyegaran layar, padahal callback itu tidak pernah ditugaskan ke field
	 * {@code eventListener} helper sehingga tidak akan pernah dijalankan. Akibatnya, setelah penyimpanan
	 * berhasil tidak ada apa pun yang menyegarkan layar pemanggil &mdash; {@link #loadData(Object)} juga
	 * langsung keluar karena kotak {@code cari} belum pernah dibuat. Satu-satunya pemanggil pada revisi
	 * ini memang menyerahkan callback kosong, jadi tidak ada gejala yang terlihat; tetapi pemanggil baru
	 * yang menyerahkan callback sungguhan akan mendapati callback-nya diam.</p>
	 *
	 * <h4>Hak akses</h4>
	 * <p>Metode ini TIDAK memeriksa wewenang siapa pun. Ia langsung membuka formulir untuk tugas kelompok
	 * mana pun yang diserahkan pemanggil. Karena bersifat {@code public static}, satu-satunya lapis
	 * pengaman adalah pemanggilnya sendiri &mdash; tombol yang memanggilnya di layar rekapitulasi sudah
	 * dijaga di sana. Setiap pemanggil baru wajib memasang penjagaannya sendiri, misalnya dengan
	 * memeriksa peran sebelum memanggil.</p>
	 *
	 * @param event                 tidak dipakai; disediakan agar tanda tangan metode seragam dengan
	 *                              penangan aksi lain
	 * @param eventListener         tidak dipakai (lihat peringatan di atas); callback penyegaran TIDAK
	 *                              akan pernah dijalankan
	 * @param tugasKelompok         tugas yang akan diedit, atau objek baru untuk penambahan
	 * @param mahasiswa             identitas mahasiswa yang login, atau {@code null}
	 * @param biodataCalonMahasiswa identitas calon mahasiswa yang login, atau {@code null}
	 * @throws Exception bila formulir gagal dirakit
	 * @see #onAdd(Event, TugasKelompok)
	 */
	public static void onAddExternal(Event event, EventListener eventListener, TugasKelompok tugasKelompok,
			Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa) throws Exception {
		TugasKelompokHelper tugasKelompokAction = new TugasKelompokHelper(mahasiswa, biodataCalonMahasiswa);
		tugasKelompokAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(tugasKelompokAction.addWindow);
		tugasKelompokAction.addWindow.setHeight("95%");
		tugasKelompokAction.addWindow.setWidth("90%");
		tugasKelompokAction.init(tugasKelompok);

		tugasKelompokAction.addWindow.setVisible(true);
		tugasKelompokAction.addWindow.onModal();
	}

	/**
	 * <h3>Membuka formulir "Instruksi Tugas Kelompok" (tambah maupun ubah)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menampilkan jendela formulir tempat dosen/guru mengisi
	 * judul, instruksi, jadwal mulai dan batas akhir, syarat pengumpulan, lampiran, serta pemetaan
	 * Sub-CPMK sebuah tugas kelompok. Satu metode ini melayani DUA keperluan sekaligus, dan yang
	 * membedakan hanyalah objek yang diserahkan:</p>
	 * <ul>
	 *   <li>objek <b>baru</b> ({@code new TugasKelompok()}) &rarr; formulir kosong, menambah tugas;</li>
	 *   <li>objek yang <b>sudah tersimpan</b> &rarr; formulir terisi, mengubah tugas yang ada.</li>
	 * </ul>
	 * <p>Tidak ada penanda mode terpisah: {@link #onSave(Event)} memutuskan menyisipkan atau memperbarui
	 * berdasarkan ada tidaknya {@code id}, lewat {@code Common.refreshSaveOrUpdate}.</p>
	 *
	 * <p>Metode ini sendiri hanya merakit jendela &mdash; seluruh isinya dibangun
	 * {@link #init(TugasKelompok)}. Lebar jendela menyesuaikan perangkat: memenuhi layar di ponsel, dan
	 * 950 piksel di desktop agar formulir tidak melebar berlebihan pada layar lebar. Jendela ditampilkan
	 * lebih dulu baru dijadikan modal, karena pemanggilan modal menahan alur sampai jendela ditutup.</p>
	 *
	 * <p><b>Cakupan diambil dari helper, bukan dari parameter.</b> Karena metode ini dipanggil pada
	 * instance yang sudah menampilkan daftar (lewat salah satu varian {@code display(...)}) atau satu
	 * tugas (lewat {@link #tampilanTugas}), keempat field cakupannya sudah terisi benar. Itulah yang
	 * membuat formulir menyembunyikan pemilih perkuliahan bila cakupannya sudah tertentu, dan yang
	 * dipakai {@link #onSave(Event)} saat menautkan tugas ke kelas yang tepat. Bandingkan dengan
	 * {@link #onAddExternal}, yang membuat helper tanpa cakupan sehingga penyimpanannya berperilaku
	 * berbeda.</p>
	 *
	 * <p><b>Hak akses:</b> tidak diperiksa di sini. Seluruh tombol yang memanggil metode ini &mdash;
	 * "Tambah Tugas" pada toolbar dan "Ubah Judul &amp; Instruksi" pada panel pengaturan &mdash; sudah
	 * dijaga {@link #bolehKelola(Tbmuser)} di tempatnya masing-masing.</p>
	 *
	 * <p>Parameter {@code event} tidak dipakai; disediakan agar tanda tangan seragam dengan penangan aksi
	 * lain sehingga dapat dipasang langsung sebagai listener.</p>
	 *
	 * @param event         tidak dipakai
	 * @param tugasKelompok tugas yang akan diedit, atau {@code new TugasKelompok()} untuk menambah
	 * @throws Exception bila formulir gagal dirakit
	 * @see #init(TugasKelompok)
	 * @see #onSave(Event)
	 */
	public void onAdd(Event event, TugasKelompok tugasKelompok) throws Exception {
		addWindow = new MyWindow();
		addWindow.setHeight("95%");
		addWindow.setWidth(Common.isMobile() ? "100%" : "950px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(addWindow);
		init(tugasKelompok);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	/**
	 * <h3>Menerbitkan sebuah Tugas Kelompok ke repositori institusi (DSpace)</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> mengirim data sebuah tugas kelompok ke DSpace, yaitu
	 * perangkat lunak repositori karya ilmiah yang banyak dipakai perpustakaan kampus. Dengan begitu
	 * tugas beserta lampirannya tersimpan dan dapat dicari di repositori resmi institusi, bukan hanya di
	 * dalam sistem akademik. Metode ini menyiapkan keterangan (metadata) tugas dalam format baku
	 * repositori, mengirimkannya, lalu mengunggah berkas lampirannya.</p>
	 *
	 * <h4>Metadata yang dikirim (skema Dublin Core)</h4>
	 * <p>Seluruh keterangan disusun sebagai daftar pasangan {@code key}/{@code value} dalam JSON,
	 * memakai nama baku Dublin Core:</p>
	 * <ul>
	 *   <li>{@code dc.contributor.author} &mdash; SATU entri untuk setiap dosen pengampu. Sumbernya
	 *   dipilih menurut asal tugas: dosen perkuliahan, dosen pembimbing kelompok KKN, atau dosen
	 *   pembimbing kelompok PKL. Ketiga cabang bersifat saling meniadakan dan diperiksa berurutan;
	 *   tugas jalur SEKOLAH (jadwal pelajaran) tidak tercakup sehingga terbit tanpa penulis.</li>
	 *   <li>{@code dc.description} &mdash; instruksi tugas, dibersihkan dari markah HTML lebih dulu
	 *   memakai {@link ais.common.Html2Text} agar yang tersimpan berupa teks biasa yang dapat diindeks
	 *   mesin pencari repositori.</li>
	 *   <li>{@code dc.title} &mdash; judul tugas.</li>
	 *   <li>{@code dc.date.copyright} &mdash; pernyataan hak cipta yang menyebut nama institusi, diambil
	 *   dari konfigurasi {@code label_universitas}.</li>
	 *   <li>{@code dc.date.issued} &mdash; tanggal tugas dalam format tanggal basis data; dilewati bila
	 *   tanggalnya kosong.</li>
	 *   <li>{@code dc.identifier.uri} &mdash; tautan berkas lampiran, hanya bila lampirannya ada DAN
	 *   tautannya tidak kosong.</li>
	 * </ul>
	 *
	 * <h4>Menambah atau memperbarui</h4>
	 * <p>Parameter {@code update} menentukan apakah entri di repositori dibuat baru atau diperbarui.
	 * Ketiga pola jalur yang diserahkan ke {@code DspaceInformation.dspaceProcess} &mdash;
	 * {@code "items"}, {@code "collections/<id>/items"}, dan {@code "items/{uuid}/metadata"} &mdash;
	 * diserahkan sekaligus, dan pustaka DSpace-lah yang memilih mana yang dipakai sesuai mode.
	 * Koleksi tujuan ditentukan {@code PerkuliahanAction.getDspace}, yaitu koleksi milik perkuliahan
	 * tugas ini, sehingga tugas terbit di tempat yang benar di dalam hierarki repositori.</p>
	 *
	 * <p>Setelah entri tersimpan dan {@code uuid}-nya diketahui, berkas lampiran diunggah sebagai
	 * <i>bitstream</i> dengan judul "Lampiran Tugas &lt;judul&gt;". Langkah ini dilewati bila tugas tidak
	 * memiliki lampiran.</p>
	 *
	 * <h4>Hal yang perlu diketahui pemelihara</h4>
	 * <ul>
	 *   <li><b>Tidak ada pemeriksaan wewenang.</b> Metode ini {@code public static} dan langsung
	 *   menerbitkan tugas mana pun yang diserahkan. Satu-satunya pemanggil adalah
	 *   {@code DspaceHelper}, yang menjaga aksesnya sendiri; pemanggil baru wajib melakukan hal yang
	 *   sama.</li>
	 *   <li><b>Penerbitan bersifat publik.</b> Instruksi tugas dan berkas lampirannya menjadi dapat
	 *   diakses lewat repositori institusi. Pastikan tugas yang diterbitkan memang layak dibuka untuk
	 *   umum &mdash; tidak ada penyaringan isi di sini.</li>
	 *   <li><b>Tidak ada penjagaan {@code null} pada isi instruksi.</b> {@code parser.parse} menerima
	 *   {@code tugasKelompok.getNama()} apa adanya, sehingga tugas tanpa instruksi berpotensi menggagalkan
	 *   penerbitan. Nilai lain sudah dijaga: tanggal dan lampiran diperiksa lebih dulu.</li>
	 *   <li><b>{@code Common.getKonfigurasi} menuliskan nilai bawaan ke basis data</b> bila kunci
	 *   {@code label_universitas} belum ada. Jadi pemanggilan pertama metode ini dapat menyimpan
	 *   konfigurasi bernilai kosong &mdash; efek samping yang tidak terlihat dari tanda tangan metode.</li>
	 *   <li>Metode ini menembak layanan luar lewat jaringan dan dapat memakan waktu; sebaiknya tidak
	 *   dipanggil langsung dari listener antarmuka tanpa indikator proses.</li>
	 * </ul>
	 *
	 * @param cookie        cookie sesi hasil autentikasi ke DSpace, disediakan pemanggil
	 * @param tugasKelompok tugas yang akan diterbitkan; instruksinya dipakai sebagai deskripsi
	 * @param update        {@code true} untuk memperbarui entri yang sudah ada, {@code false} untuk
	 *                      membuat entri baru
	 * @return keterangan entri DSpace hasil proses, termasuk {@code uuid} yang dipakai saat mengunggah
	 *         lampiran
	 * @throws Exception bila penyusunan metadata, pemanggilan layanan DSpace, atau pengunggahan lampiran
	 *                   gagal
	 * @see ais.database.model.DspaceInformation
	 * @see ais.action.master.PerkuliahanAction#getDspace(String, Perkuliahan)
	 */
	public static DspaceInformation getDspace(String cookie, TugasKelompok tugasKelompok, boolean update)
			throws Exception {

		JSONArray jsonArray = new JSONArray();
		if (tugasKelompok.getPerkuliahan() != null) {
			Map<String, Dosen> map = tugasKelompok.getPerkuliahan().populateDosen();
			for (Dosen dosen : map.values()) {
				String nama = dosen.getNama();

				JSONObject jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.author");
				jsonMetadata.put("value", nama);
				jsonArray.put(jsonMetadata);
			}
		} else if (tugasKelompok.getKelompokKkn() != null) {
			Map<String, Dosen> map = tugasKelompok.getKelompokKkn().populateDosen();
			for (Dosen dosen : map.values()) {
				String nama = dosen.getNama();

				JSONObject jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.author");
				jsonMetadata.put("value", nama);
				jsonArray.put(jsonMetadata);
			}
		} else if (tugasKelompok.getKelompokPkl() != null) {
			Map<String, Dosen> map = tugasKelompok.getKelompokPkl().populateDosen();
			for (Dosen dosen : map.values()) {
				String nama = dosen.getNama();

				JSONObject jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.contributor.author");
				jsonMetadata.put("value", nama);
				jsonArray.put(jsonMetadata);
			}
		}
		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(tugasKelompok.getNama()));

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", tugasKelompok.getJudul());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		if (tugasKelompok.getTanggal() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(tugasKelompok.getTanggal()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(tugasKelompok.getId(), LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN);
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie, tugasKelompok,
				jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/" + PerkuliahanAction.getDspace(cookie, tugasKelompok.getPerkuliahan()) + "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Lampiran Tugas " + tugasKelompok.getJudul());
		}

		return dspaceInformation;

	}

	/**
	 * <h3>Merakit isi formulir "Instruksi Tugas Kelompok"</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> mengisi jendela formulir dengan seluruh kolom isian yang
	 * diperlukan untuk membuat atau mengubah sebuah tugas kelompok: pemindah pertemuan, pemilih
	 * perkuliahan, tanggal mulai dan batas akhir, syarat pengumpulan, judul, instruksi (dengan bantuan
	 * AI bila diinginkan), lampiran, pemetaan Sub-CPMK untuk kurikulum OBE, daftar syarat, serta tombol
	 * Batal dan Simpan. Jendelanya sendiri sudah dibuat pemanggil; metode ini yang mengisinya.</p>
	 *
	 * <p>Beberapa komponen yang dibangun di sini disimpan ke field helper ({@code judul}, {@code isi},
	 * {@code mulaiWaktuMengumpulkanTugas}, {@code batasWaktuMengumpulkanTugas},
	 * {@code syaratMengumpulkanTugas}, {@code banboxPerkuliahan}, {@code lampiran}) karena
	 * {@link #onSave(Event)} membacanya kembali langsung dari field, bukan menerimanya sebagai parameter.
	 * Objek tugas juga disimpan ke field {@code tugasKelompok}. Konsekuensinya: satu instance helper
	 * hanya boleh membuka SATU formulir pada satu waktu &mdash; membuka formulir kedua akan menimpa
	 * field-field ini sehingga penyimpanan memakai komponen milik formulir terakhir. Alur modal yang
	 * dipakai selama ini mencegah hal tersebut.</p>
	 *
	 * <h4>Bagian-bagian formulir</h4>
	 * <ol>
	 *   <li><b>Pindahkan ke pertemuan</b> &mdash; combobox berisi seluruh pertemuan pada wadah
	 *   pembelajaran yang sama, sehingga tugas dapat dipindah "ke pertemuan ke berapa". Bila daftar
	 *   pertemuan dari cache kosong, pemuatan diulang paksa dari basis data agar dropdown tidak kosong.
	 *   Pertemuan yang sedang terpilih selalu dipastikan ada dan tersorot, walau tidak termuat dalam
	 *   daftar. Perpindahan meminta konfirmasi lebih dulu, menyimpan {@code pertemuan} beserta
	 *   {@code pertemuanData}, lalu menutup formulir. Seluruh blok ini tidak dibangun bila yang login
	 *   adalah pelajar (termasuk peserta kursus).</li>
	 *   <li><b>Perkuliahan</b> &mdash; hanya terlihat bila layar tidak punya cakupan sama sekali. Bila
	 *   layar sudah punya cakupan, cakupan itu langsung ditugaskan ke objek tugas dan pemilihnya
	 *   disembunyikan. Perkuliahan terpilih disimpan sebagai ATRIBUT komponen bernama
	 *   {@code "perkuliahan"}, bukan sebagai nilai teks &mdash; itulah yang dibaca {@code onSave}.</li>
	 *   <li><b>Tanggal mulai &amp; selesai</b> &mdash; batas akhir sengaja boleh dikosongkan, artinya
	 *   tugas tanpa batas waktu; keterangan penjelasnya ditampilkan tepat di bawahnya.</li>
	 *   <li><b>Syarat pengumpulan</b> &mdash; daftar {@link SyaratUjian} yang aktif saja. Bila syarat
	 *   terpilih ditandai "hanya boleh diubah oleh admin", pilihan dikunci dan keterangan penjelas
	 *   dimunculkan. Listener yang sama dijalankan sekali di awal agar keadaan kunci sudah benar sejak
	 *   formulir dibuka, bukan baru setelah pengguna mengubah pilihan.</li>
	 *   <li><b>Judul &amp; instruksi</b> &mdash; judul dibatasi 255 karakter; instruksi memakai penyunting
	 *   teks kaya. Tombol "Generate Tugas Kelompok" memanggil {@link ais.common.AIGenerator} untuk
	 *   menyusun langkah pengerjaan secara otomatis; kalimat perintahnya disesuaikan dengan nama mata
	 *   kuliah atau mata pelajaran yang sedang dibuka. Hasilnya langsung ditulis ke objek tugas, dan
	 *   bila judul masih kosong ia diisikan otomatis dari topik yang diketik pengguna.</li>
	 *   <li><b>Pemetaan Sub-CPMK (bobot)</b> &mdash; hanya untuk kurikulum OBE, hanya untuk tugas yang
	 *   SUDAH tersimpan ({@code id != null}), dan hanya untuk pengelola. Inilah SATU-SATUNYA tempat bobot
	 *   Sub-CPMK dapat diubah; pada kartu ringkas grid yang sama ditampilkan baca-saja. Perubahan
	 *   tersimpan otomatis, tidak menunggu tombol Simpan.</li>
	 *   <li><b>Daftar syarat</b> &mdash; versi dapat diubah untuk pengelola, versi baca-saja untuk
	 *   pelajar, sama seperti pada penggambaran baris daftar.</li>
	 *   <li><b>Batal &amp; Simpan</b> &mdash; Batal menjalankan callback pemanggil lalu menutup jendela
	 *   tanpa menyimpan. Simpan memanggil {@link #onSave(Event)}; hanya bila penyimpanan berhasil
	 *   jendela ditutup, sehingga formulir tetap terbuka beserta isiannya saat validasi gagal. Setelah
	 *   berhasil, layar disegarkan sesuai mode: memuat ulang daftar pada mode daftar, atau menggambar
	 *   ulang kartu tunggal pada mode rinci.</li>
	 * </ol>
	 *
	 * <h4>Catatan bagi pemelihara</h4>
	 * <ul>
	 *   <li><b>Penjagaan {@code null} yang terlambat.</b> {@code addWindow.setTitle(...)} dijalankan
	 *   SEBELUM pemeriksaan {@code if (addWindow != null)}, sehingga pemeriksaan itu tidak pernah
	 *   melindungi apa pun &mdash; {@code addWindow} yang {@code null} sudah melempar galat di baris
	 *   sebelumnya. Kedua pemanggil selalu mengisinya lebih dulu, jadi tidak ada gejala.</li>
	 *   <li><b>Pemeriksaan peran tidak seragam.</b> Blok pemindah pertemuan memakai daftar peran yang
	 *   dirakit setempat, sedangkan blok Sub-CPMK dan blok syarat memakai bentuk enam suku gaya lama
	 *   yang tidak menguji {@code tbmuser.getMahasiswa()}. Tidak satu pun memakai
	 *   {@link #bolehKelola(Tbmuser)}, dan {@code tbmuser} sebagian dipakai tanpa penjagaan {@code null}
	 *   lebih dulu.</li>
	 *   <li><b>Penguncian syarat khusus admin.</b> Kunci hanya dipasang bila pengguna berupa dosen atau
	 *   mahasiswa (atau sesi tanpa pengguna). Peran lain &mdash; siswa, calon siswa, peserta kursus
	 *   &mdash; tidak ikut terkunci meskipun bukan admin.</li>
	 *   <li>Perubahan pada blok pemindah pertemuan dan blok Sub-CPMK tersimpan LANGSUNG saat diubah,
	 *   terpisah dari tombol Simpan. Menekan Batal setelah mengubah keduanya tidak membatalkan apa pun.</li>
	 * </ul>
	 *
	 * @param tugasKelompok tugas yang akan diedit, atau objek baru untuk penambahan; disimpan ke field
	 *                      agar dapat dibaca kembali oleh {@link #onSave(Event)}
	 * @throws Exception bila salah satu bagian formulir gagal dirakit
	 * @see #onSave(Event)
	 * @see #bangunGridSubCpmk(Component, TugasKelompok, Perkuliahan, boolean)
	 */
	private void init(final TugasKelompok tugasKelompok) throws Exception {
		this.tugasKelompok = tugasKelompok;

		addWindow.setTitle("Tugas Kelompok");
		if (addWindow != null) {
			Common.clear(addWindow);
		}
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setSclass("fgrid");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("100%");

		Rows rows = new Rows();

		rows.setParent(grid);

		/*
		 * === Pindahkan tugas kelompok ke pertemuan lain ===
		 * Combobox berisi DAFTAR PERTEMUAN yang tersedia pada VOPembelajaran yang SAMA
		 * (ambilVOPembelajaran()), sehingga tugas kelompok dapat dipindah "ke pertemuan ke
		 * berapa" — sama seperti TugasMandiriHelper. TugasKelompok menyimpan FK pertemuan
		 * (setPertemuan(Long)+setPertemuanData), dan berkas pengumpulan (ref=tugas.id) ikut
		 * otomatis. Hanya untuk pengelola (bukan mahasiswa/siswa/calon).
		 */
		{
			final Tbmuser tbmuserPindah = Common.getCurrentUser();
			final Pertemuan pertemuanTugasIni = tugasKelompok.ambilPertemuan();
			final VOPembelajaran pembelajaranPindah = pertemuanTugasIni == null ? null
					: pertemuanTugasIni.ambilVOPembelajaran();
			final boolean bisaPindahPertemuan = pembelajaranPindah != null && tbmuserPindah != null
					&& tbmuserPindah.getMahasiswa() == null && tbmuserPindah.getSiswa() == null
					&& tbmuserPindah.getBiodataCalonMahasiswa() == null && tbmuserPindah.getCalonSiswa() == null;

			// Combo "Pindahkan ke pertemuan" TIDAK ditampilkan bila yang login adalah pelajar
			// (mahasiswa/siswa/calon/peserta kursus) — sebelumnya hanya di-disable, kini disembunyikan.
			final boolean loginPelajar = tbmuserPindah != null && (tbmuserPindah.getMahasiswa() != null
					|| tbmuserPindah.getSiswa() != null || tbmuserPindah.getBiodataCalonMahasiswa() != null
					|| tbmuserPindah.getCalonSiswa() != null || tbmuserPindah.getPesertaKursus() != null);

			if (pembelajaranPindah != null && !loginPelajar) {
				MyFormRow rowKe = new MyFormRow();
				rowKe.setValign("top");
				rowKe.setParent(rows);
				rowKe.appendChild(new MyLabelBoldConfig("Pertemuan"));

				rowKe = new MyFormRow();
				rowKe.setParent(rows);

				final Combobox comboPertemuan = new Combobox();
				comboPertemuan.setReadonly(true);
				comboPertemuan.setWidth("90%");
				rowKe.appendChild(comboPertemuan);

				final Long pertemuanSaatIni = (pertemuanTugasIni == null || pertemuanTugasIni.getId() == null) ? null
						: pertemuanTugasIni.getId();
				try {
					// Bila cache lokasi-pertemuan KOSONG, paksa muat ulang dari DB agar dropdown tidak kosong.
					java.util.TreeMap<String, Long> daftarPertemuan = pembelajaranPindah.ambilPertemuan();
					if (daftarPertemuan == null || daftarPertemuan.isEmpty()) {
						daftarPertemuan = pembelajaranPindah.ambilPertemuan(true);
					}
					boolean adaSaatIni = false;
					if (daftarPertemuan != null) {
						for (Long pid : daftarPertemuan.values()) {
							if (pid == null) {
								continue;
							}
							Pertemuan p = (Pertemuan) ais.database.model.GeneralValueObject.ambilData(Pertemuan.class,
									pid.toString());
							if (p == null) {
								continue;
							}
							String topik = p.getTopik() == null ? "" : p.getTopik().trim();
							if (topik.length() > 40) {
								topik = topik.substring(0, 40) + "...";
							}
							String tgl = p.getTanggal() == null ? ""
									: (" - " + Common.dateFormat.get().format(p.getTanggal()));
							org.zkoss.zul.Comboitem item = new org.zkoss.zul.Comboitem(
									"Pertemuan ke-" + p.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
							item.setValue(pid);
							item.setParent(comboPertemuan);
							if (pertemuanSaatIni != null && pertemuanSaatIni.equals(pid)) {
								comboPertemuan.setSelectedItem(item);
								adaSaatIni = true;
							}
						}
					}
					// Pastikan pertemuan yang SEDANG dipilih selalu ada & terpilih walau tidak termuat.
					if (!adaSaatIni && pertemuanSaatIni != null && pertemuanTugasIni != null) {
						String topik = pertemuanTugasIni.getTopik() == null ? "" : pertemuanTugasIni.getTopik().trim();
						if (topik.length() > 40) {
							topik = topik.substring(0, 40) + "...";
						}
						String tgl = pertemuanTugasIni.getTanggal() == null ? ""
								: (" - " + Common.dateFormat.get().format(pertemuanTugasIni.getTanggal()));
						org.zkoss.zul.Comboitem itemSaatIni = new org.zkoss.zul.Comboitem("Pertemuan ke-"
								+ pertemuanTugasIni.getPertemuanKe() + (topik.isEmpty() ? "" : " : " + topik) + tgl);
						itemSaatIni.setValue(pertemuanSaatIni);
						itemSaatIni.setParent(comboPertemuan);
						comboPertemuan.setSelectedItem(itemSaatIni);
					}
				} catch (Exception eDaftarPertemuan) {
					Common.tampilErrorJikaAdmin(eDaftarPertemuan);
				}

				comboPertemuan.setDisabled(!bisaPindahPertemuan);

				if (bisaPindahPertemuan) {
					comboPertemuan.addEventListener("onChange", new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							if (comboPertemuan.getSelectedItem() == null) {
								return;
							}
							final Long pidBaru = (Long) comboPertemuan.getSelectedItem().getValue();
							if (pidBaru == null || pidBaru.equals(pertemuanSaatIni)) {
								return;
							}
							final String labelTujuan = comboPertemuan.getSelectedItem().getLabel();
							MyMessageboxConfig.show("Pindahkan tugas kelompok ini ke \"" + labelTujuan + "\" ?",
									"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL,
									MyMessageboxConfig.QUESTION, new EventListener() {
										@Override
										public void onEvent(Event ev) throws Exception {
											if (Integer.parseInt(ev.getData().toString()) != MyMessageboxConfig.OK) {
												return;
											}
											Session session = HibernateUtil.currentSession();
											if (tugasKelompok.getId() != null) {
												session.refresh(tugasKelompok);
											}
											Pertemuan pBaru = (Pertemuan) ais.database.model.GeneralValueObject
													.ambilData(Pertemuan.class, pidBaru.toString());
											tugasKelompok.setPertemuan(pidBaru);
											tugasKelompok.setPertemuanData(pBaru);
											Common.refreshUpdate(session, tugasKelompok);

											if (eventListener != null) {
												eventListener.onEvent(ev);
											}
											addWindow.detach();
										}
									});
						}
					});
				}
			}
		}

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(perkuliahan == null && kelompokKkn == null && kelompokPkl == null && jadwalPelajaran == null);
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Perkuliahan (*):"));

		if (perkuliahan != null) {
			tugasKelompok.setPerkuliahan(perkuliahan);
		}

		if (kelompokKkn != null) {
			tugasKelompok.setKelompokKkn(kelompokKkn);
		}
		if (kelompokPkl != null) {
			tugasKelompok.setKelompokPkl(kelompokPkl);
		}

		row = new MyFormRow();
		row.setVisible(perkuliahan == null && kelompokKkn == null && kelompokPkl == null && jadwalPelajaran == null);
		row.setParent(rows);
		row.appendChild(banboxPerkuliahan = new AmbilDataPerkuliahanBandbox());
		banboxPerkuliahan.setReadonly(true);
		banboxPerkuliahan.setAttribute("perkuliahan", tugasKelompok.getPerkuliahan());
		banboxPerkuliahan.setValue(tugasKelompok.getPerkuliahan() == null ? ""
				: Common.getDeskripsiPerkuliahan(tugasKelompok.getPerkuliahan()));
		banboxPerkuliahan.setWidth("90%");
		banboxPerkuliahan.setVisible(
				perkuliahan == null && kelompokKkn == null && kelompokPkl == null && jadwalPelajaran == null);

		mulaiWaktuMengumpulkanTugas = new MyDatebox(tugasKelompok.getMulai());
		mulaiWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());

		batasWaktuMengumpulkanTugas = new MyDatebox(tugasKelompok.getSelesai());
		batasWaktuMengumpulkanTugas.setFormat(Common.dateFormat.get().toPattern());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tanggal dan Waktu Tugas Mulai"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(mulaiWaktuMengumpulkanTugas);
		mulaiWaktuMengumpulkanTugas.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelBoldConfig("Tanggal dan Waktu Tugas Selesai"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(batasWaktuMengumpulkanTugas);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelAgakKecil(
				"*) Kosongkan tanggal tugas selesai jika tugas ini tidak ada batas waktu selesai-nya"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Syarat dapat mengumpulkan tugas"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(syaratMengumpulkanTugas = new Combobox());
		Common.insertComboDanSemua(syaratMengumpulkanTugas, new String[] { "nama" }, "keterangan", SyaratUjian.class,
				"== Tanpa Syarat Mengikuti Ujian ==",
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(syaratMengumpulkanTugas, tugasKelompok.getSyaratMengumpulkanTugas(), true);
		syaratMengumpulkanTugas.setWidth("90%");
		syaratMengumpulkanTugas.setReadonly(true);

		final Row rowSyarat = Common.initKeteranganSatuKolom(rows, "Persyaratan ini hanya boleh diubah oleh admin");

		EventListener listenerSyarat = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();

				SyaratUjian syaratUjian = (SyaratUjian) (syaratMengumpulkanTugas.getSelectedItem() == null ? null
						: syaratMengumpulkanTugas.getSelectedItem().getValue());
				syaratMengumpulkanTugas.setDisabled(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin()
						&& (tbmuser == null || tbmuser.ambilDosen() != null || tbmuser.getMahasiswa() != null));

				rowSyarat.setVisible(syaratUjian != null && syaratUjian.getHanyaBolehDiubahOlehAdmin());
			}
		};
		listenerSyarat.onEvent(null);
		syaratMengumpulkanTugas.addEventListener("onChange", listenerSyarat);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Judul tugas kelompok (*):"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(judul = new Textbox(tugasKelompok.getJudul()));
		judul.setWidth("90%");
		judul.setRows(2);
		judul.setMaxlength(255);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelBoldConfig("Instruksi tugas kelompok :"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(isi = new MyCkEditor());
		isi.setValue(tugasKelompok.getNama());
		isi.setWidth("97%");
		isi.setHeight("70px");

		row = new MyFormRow();
		row.setParent(rows);

		Hbox hbox = new Hbox();
		hbox.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		lampiran = null;
		Tbmuser tbmuser = Common.getCurrentUser();
		LampiranLain.createDownloadUploadFileLain(hbox, tugasKelompok.getId(), LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN,
				LampiranLain.TUGAS_KELOMPOK_PERKULIAHAN, false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lampiran = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false,
				bolehKelola(tbmuser),
				null, false, false, row);

		MyToolbarbutton toolbarbutton = new MyToolbarbutton("fa-cog", "Generate Tugas Kelompok");
		toolbarbutton.setTooltiptext("Buat instruksi tugas kelompok secara otomatis menggunakan AI");
		hbox.appendChild(toolbarbutton);

		Matakuliah mk = null;
		Matapelajaran matpel = null;
		String tanyaAkhiran = "";
		String tanyaMengajar = " apa saja";
		if (perkuliahan != null) {
			mk = (Matakuliah) perkuliahan.getMatakuliah();
			if (mk != null) {
				tanyaMengajar = " matakuliah " + mk.getNama();
				tanyaAkhiran = " pada matakuliah \"" + mk.getNama() + "\"";
			}
		} else if (jadwalPelajaran != null) {
			matpel = (Matapelajaran) jadwalPelajaran.getMatapelajaran();
			if (matpel != null) {
				tanyaMengajar = " matapelajaran " + matpel.getNama();
				tanyaAkhiran = " pada matapelajaran \"" + matpel.getNama() + "\"";
			}
		}

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

			}
		};

		toolbarbutton.addEventListener("onClick",
				AIGenerator.generateApa("Generate Tugas Kelompok", "Tugas Kelompok tentang apa ?",
						"Buatkan tata cara dan langkah-langkah mengerjakan tugas kelompok ", false, tanyaAkhiran,
						Common.getKonfigurasi("llama_system_buat_tugas_kelompok",
								"Kamu adalah Pengajar atau Dosen atau Guru ").getNilai().trim(),
						isi, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								String isiData = (String) arg0.getData();
								Textbox s = (Textbox) arg0.getTarget();

								if (judul.getValue().trim().isEmpty()) {
									judul.setValue("Tugas kelompok \"" + s.getValue().trim() + "\"");
									tugasKelompok.setJudultugas(judul.getValue());
								}
								tugasKelompok.setIsitugas(
										ais.action.servlet.Wa.ubahKeBold(isiData).replaceAll("\n", "<br>"));

								if (tugasKelompok.getId() != null) {
									Common.refreshUpdate(tugasKelompok);
								}

							}
						}, tanyaMengajar, eventListenerData));

		Common.initKeteranganSatuKolom(rows,
				"Jika file yang Anda upload lebih dari satu file, zip / compress / jadikan satu file terlebih dulu, kemudian baru di-upload");

		// === (B) Pemetaan Sub-CPMK & Bobot (DAPAT DIEDIT di sini) ===
		// Sesuai instruksi: bobot Sub-CPMK HANYA dapat diubah di dalam Window "Instruksi Tugas
		// Kelompok" ini; pada kartu ringkas hanya ditampilkan baca-saja (label). Ditampilkan hanya
		// untuk kurikulum OBE, tugas yang SUDAH tersimpan (id != null), dan pengelola (bukan
		// mahasiswa/siswa/calon) — memanfaatkan ulang bangunGridSubCpmk(..., editable=true).
		{
			final Perkuliahan perkuliahanObe = tugasKelompok.getPerkuliahan();
			boolean pengelolaObe = bolehKelola(tbmuser);
			if (pengelolaObe && tugasKelompok.getId() != null && perkuliahanObe != null
					&& perkuliahanObe.getKurikulum() != null && perkuliahanObe.getKurikulum()
							.apakahObe(perkuliahanObe.getTahunAjaran(), perkuliahanObe.getGanjilGenap())) {
				MyFormRow rowObe = new MyFormRow();
				rowObe.setValign("top");
				rowObe.setParent(rows);
				rowObe.appendChild(new MyLabelBoldConfig("Pemetaan Sub-CPMK (Bobot)"));

				rowObe = new MyFormRow();
				rowObe.setValign("top");
				rowObe.setParent(rows);
				org.zkoss.zul.Div wrapObe = new org.zkoss.zul.Div();
				wrapObe.setStyle("width:100%;");
				rowObe.appendChild(wrapObe);
				bangunGridSubCpmk(wrapObe, tugasKelompok, perkuliahanObe, true);

				Common.initKeteranganSatuKolom(rows,
						"Centang Sub-CPMK yang dinilai oleh tugas ini, lalu isi bobotnya. Perubahan tersimpan otomatis.");
			}
		}

		Pertemuan pertemuan = tugasKelompok.ambilPertemuan();
		if (pertemuan != null) {

			MyToolbarbutton button = new MyToolbarbutton("fa-refresh", "Refresh Syarat");

			Set<String> syaratAlert = new HashSet<String>();
			if (bolehKelola(tbmuser)) {
				Tugas.tampilanSyarat(pertemuan, tugasKelompok, null, null, null, null, rows, syaratAlert, button);
			} else {
				Tugas.tampilanSyaratReadonly(pertemuan, tugasKelompok, null, null, null, null, rows, syaratAlert,
						button);

				Tugas.tampilanLain(pertemuan, tugasKelompok, null, null, null, null, rows, button);
			}
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbutton cancel = new MyToolbarbutton("fa-times", "Batal");
		cancel.setTooltiptext("Tutup tanpa menyimpan");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (eventListener != null) {
					eventListener.onEvent(event);
				}

				addWindow.detach();
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbutton save = new MyToolbarbutton("fa-save", "Simpan");
		save.setTooltiptext("Simpan perubahan instruksi tugas kelompok");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					if (tampilRinci) {
						loadData(null);
						if (eventListener != null) {
							eventListener.onEvent(event);
						}
					} else {
						if (TugasKelompokHelper.this.component != null) {
							Common.clear(TugasKelompokHelper.this.component);
						}
						tampilanTugas(TugasKelompokHelper.this.tugasKelompok, TugasKelompokHelper.this.component,
								eventListener);
					}

					addWindow.detach();
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	/**
	 * <h3>Menyimpan isi formulir "Instruksi Tugas Kelompok"</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> memeriksa isian formulir, lalu menyimpannya ke basis data
	 * &mdash; menambah baris baru bila tugas belum pernah disimpan, atau memperbarui baris yang ada bila
	 * sudah. Setelah itu lampiran instruksi ditautkan ke tugas dan pemberitahuan dikirim ke peserta.
	 * Mengembalikan {@code true} bila berhasil, sehingga pemanggil tahu apakah jendela boleh ditutup.</p>
	 *
	 * <h4>Dua pemeriksaan wajib-isi</h4>
	 * <p>Perkuliahan wajib dipilih &mdash; tetapi HANYA bila pemilihnya sedang terlihat. Bila layar sudah
	 * punya cakupan, pemilih itu disembunyikan dan pemeriksaan dilewati karena perkuliahan sudah
	 * ditentukan dari cakupan. Judul wajib diisi dan tidak boleh berupa spasi belaka. Kedua kegagalan
	 * memunculkan peringatan lalu mengembalikan {@code false}; formulir sengaja TIDAK ditutup sehingga
	 * isian pengguna tidak hilang dan tinggal dilengkapi.</p>
	 *
	 * <p>Tidak ada pemeriksaan lain. Secara khusus, tanggal mulai dan batas akhir tidak diperiksa
	 * urutannya, sehingga batas akhir yang lebih awal daripada tanggal mulai akan tersimpan apa adanya
	 * &mdash; menghasilkan tugas yang menurut gerbang waktu di penggambaran baris sudah ditutup sejak
	 * sebelum dibuka.</p>
	 *
	 * <h4>Memuat ulang entity sebelum menulis</h4>
	 * <p>Bila tugas sudah memiliki {@code id}, field {@code tugasKelompok} DITUKAR dengan hasil
	 * {@code session.load} pada sesi berjalan, baru kemudian diisi nilai dari formulir. Tujuannya agar
	 * pembaruan bekerja pada objek yang benar-benar dikelola sesi, bukan pada objek lepas yang mungkin
	 * sudah basi. Perhatikan bahwa penukaran ini juga terlihat dari luar: pemanggil yang masih memegang
	 * objek lama tidak akan melihat perubahan, sedangkan {@link #init(TugasKelompok)} membaca ulang field
	 * ini saat menggambar ulang kartu setelah penyimpanan.</p>
	 *
	 * <h4>PENTING: keempat cakupan ditulis TANPA SYARAT</h4>
	 * <p>Setelah nilai dari formulir disalin, keempat penanda cakupan ditulis langsung dari FIELD helper:</p>
	 * <pre>tugasKelompok.setKelompokKkn(kelompokKkn);
	 * tugasKelompok.setKelompokPkl(kelompokPkl);
	 * tugasKelompok.setJadwalPelajaran(jadwalPelajaran);</pre>
	 * <p>Tidak ada pemeriksaan {@code null} di sini, sehingga field helper yang kosong akan MENGHAPUS
	 * penanda yang sudah tersimpan pada tugas. Selama helper dibuka lewat {@code display(...)} atau
	 * {@link #tampilanTugas} hal ini tidak menimbulkan masalah, karena field cakupannya memang sudah
	 * sesuai dengan tugas yang diedit. Masalah muncul bila helper dibuat tanpa cakupan &mdash; seperti
	 * pada {@link #onAddExternal}: menyimpan lewat jalur itu akan mengosongkan kelompok KKN, kelompok
	 * PKL, dan jadwal pelajaran milik tugas. Perkuliahan tidak ikut terdampak karena diambil dari atribut
	 * pemilih, bukan dari field cakupan.</p>
	 *
	 * <h4>Penautan lampiran: gagal tanpa membatalkan penyimpanan</h4>
	 * <p>Tugas disimpan lebih dulu lewat {@code Common.refreshSaveOrUpdate}, baru lampiran ditautkan
	 * dengan menyetel {@code ref} ke id tugas. Penautan berjalan pada SESI STREAMING terpisah dengan
	 * transaksinya sendiri. Konsekuensinya bersifat sengaja dan perlu diketahui: bila penautan lampiran
	 * gagal, <b>tugas tetap tersimpan</b> &mdash; yang di-<i>rollback</i> hanya transaksi lampiran. Itulah
	 * sebabnya pesan kegagalannya secara eksplisit memberi tahu pengguna bahwa data tugas sudah tersimpan
	 * dan yang perlu diperiksa hanyalah lampirannya. Sesi streaming SELALU ditutup di blok
	 * {@code finally} sehingga tidak ada koneksi yang bocor. Langkah ini dilewati bila pengguna tidak
	 * mengunggah lampiran baru.</p>
	 *
	 * <h4>Pemberitahuan ke peserta</h4>
	 * <p>{@code CommonEmail.infoAdaTugasKelompokPerkuliahan} dipanggil di akhir, TANPA membedakan tambah
	 * dan ubah. Jadi setiap kali tombol Simpan ditekan &mdash; termasuk untuk perbaikan kecil seperti
	 * memperbaiki salah ketik judul &mdash; pemberitahuan dikirim ulang. Pemanggilan ini juga berada di
	 * luar blok {@code try}, sehingga kegagalan pengiriman akan merambat ke pemanggil meskipun data sudah
	 * tersimpan dengan selamat.</p>
	 *
	 * <h4>Hak akses</h4>
	 * <p>Metode ini {@code public} dan TIDAK memeriksa wewenang siapa pun: siapa saja yang berhasil
	 * memanggilnya dapat menulis ke tugas kelompok mana pun yang sedang dipegang field. Penjagaan berada
	 * seluruhnya pada tombol-tombol yang membuka formulir. Bila kelak diperlukan pemeriksaan kepemilikan
	 * per data &mdash; misalnya memastikan pengguna benar dosen pengampu perkuliahan yang bersangkutan
	 * &mdash; di sinilah tempat yang tepat, karena inilah satu-satunya jalur yang benar-benar menulis
	 * entity dari formulir.</p>
	 *
	 * <p>Field {@code banboxPerkuliahan}, {@code judul}, {@code isi}, dan kedua pemilih tanggal dibaca
	 * tanpa penjagaan {@code null}, sehingga metode ini hanya boleh dipanggil setelah
	 * {@link #init(TugasKelompok)} merakit formulirnya. Parameter {@code event} tidak dipakai.</p>
	 *
	 * @param event tidak dipakai; disediakan agar tanda tangan seragam dengan penangan aksi lain
	 * @return {@code true} bila data tersimpan (pemanggil boleh menutup jendela); {@code false} bila
	 *         validasi wajib-isi gagal (jendela harus tetap terbuka)
	 * @throws Exception bila penyimpanan entity atau pengiriman pemberitahuan gagal; kegagalan penautan
	 *                   lampiran TIDAK dilemparkan melainkan dilaporkan ke pengguna
	 * @see #init(TugasKelompok)
	 * @see #onAddExternal(Event, EventListener, TugasKelompok, Mahasiswa, BiodataCalonMahasiswa)
	 */
	public boolean onSave(Event event) throws Exception {
		if (banboxPerkuliahan.isVisible() && banboxPerkuliahan.getAttribute("perkuliahan") == null) {
			MyMessageboxConfig.show("Perkuliahan harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (judul.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Judul tugas harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (tugasKelompok.getId() != null) {
			tugasKelompok = (TugasKelompok) session.load(TugasKelompok.class, tugasKelompok.getId());
		}

		tugasKelompok.setJudul(judul.getValue());
		tugasKelompok.setNama(isi.getValue());
		tugasKelompok.setPerkuliahan((Perkuliahan) banboxPerkuliahan.getAttribute("perkuliahan"));
		tugasKelompok.setKelompokKkn(kelompokKkn);
		tugasKelompok.setKelompokPkl(kelompokPkl);
		tugasKelompok.setJadwalPelajaran(jadwalPelajaran);
		tugasKelompok.setMulai(mulaiWaktuMengumpulkanTugas.getValue());
		tugasKelompok.setSelesai(batasWaktuMengumpulkanTugas.getValue());
		tugasKelompok.setSyaratMengumpulkanTugas((SyaratUjian) (syaratMengumpulkanTugas.getSelectedItem() == null ? null
				: syaratMengumpulkanTugas.getSelectedItem().getValue()));

		Common.refreshSaveOrUpdate(session, tugasKelompok);

		try {
			session = StreamingHibernateUtil.getInstance().currentSession();

			if (lampiran != null && lampiran.getId() != null) {
				session.refresh(lampiran);
				lampiran.setRef(tugasKelompok.getId());

				session.getTransaction().begin();
				session.update(lampiran);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menautkan lampiran ke data tugas kelompok yang baru disimpan",
					e, new String[] {
							"Data tugas kelompok sudah tersimpan; muat ulang (refresh) halaman ini lalu periksa apakah lampiran perlu diunggah ulang.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		} finally {
			// Sesi streaming WAJIB ditutup di finally agar tidak bocor walau terjadi error.
			StreamingHibernateUtil.getInstance().closeSession();
		}

		CommonEmail.infoAdaTugasKelompokPerkuliahan(tugasKelompok);

		return true;
	}

	/**
	 * <h3>Menandai ALPA seluruh anggota kelompok yang tidak mengumpulkan tugas</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> menghemat pekerjaan dosen dengan mengisi absensi secara
	 * borongan. Setelah batas pengumpulan lewat, dosen menekan satu tombol dan sistem menandai semua
	 * mahasiswa yang kelompoknya TIDAK mengumpulkan berkas sebagai alpa (mangkir) pada pertemuan yang
	 * bersangkutan &mdash; tanpa perlu mencentang satu per satu di daftar hadir.</p>
	 *
	 * <h4>Penilaian dilakukan PER KELOMPOK, bukan per orang</h4>
	 * <p>Untuk setiap kelompok pada tugas ini, sistem mencari berkas yang diunggah kelompok tersebut.
	 * Bila kelompok itu TIDAK memiliki berkas sama sekali, SELURUH anggotanya ditandai alpa dengan status
	 * "tidak ada alasan". Sebaliknya bila kelompok sudah mengumpulkan, seluruh anggotanya dilewati.
	 * Konsekuensinya melekat pada sifat tugas kelompok: satu orang yang mengunggah menyelamatkan seluruh
	 * anggota, dan sebaliknya kelompok yang lalai membuat semua anggotanya alpa &mdash; kontribusi
	 * masing-masing anggota tidak dinilai di sini.</p>
	 *
	 * <p>Peserta yang terdaftar pada daftar pengecualian tugas ({@code mhsYgTidakIkut}) DILEWATI, karena
	 * mereka memang tidak diwajibkan mengumpulkan. Keterangan absensi yang dicatat menyebut judul tugas
	 * dan batas waktunya, sehingga alasan penandaan tetap dapat ditelusuri kemudian; bila tugas tidak
	 * punya batas waktu, dipakai tanggal saat tombol ditekan.</p>
	 *
	 * <p>Waktu mulai dan selesai kehadiran diambil dari catatan absensi mahasiswa yang bersangkutan pada
	 * pertemuan itu; bila belum ada, dipakai jam mulai dan jam selesai pertemuan sebagai gantinya.</p>
	 *
	 * <h4>Konfirmasi dan pelaksanaan tertunda</h4>
	 * <p>Karena aksinya mengubah banyak baris sekaligus, pengguna diminta konfirmasi lebih dulu.
	 * Pekerjaan sesungguhnya baru berjalan setelah pengguna menekan OK, dan dibungkus
	 * {@code Common.createDefaultTimer} sehingga dijalankan pada putaran event berikutnya &mdash; kotak
	 * dialog sempat menutup dan antarmuka tidak terlihat membeku selama proses berlangsung. Callback
	 * {@code eventListener} juga dijalankan lewat timer setelah seluruh penandaan selesai, agar layar
	 * pemanggil menyegarkan diri.</p>
	 *
	 * <h4>Catatan bagi pemelihara</h4>
	 * <ul>
	 *   <li><b>Tidak ada pemeriksaan wewenang.</b> Metode ini {@code public static} dan langsung menulis
	 *   absensi begitu pengguna mengonfirmasi. Penjagaan sepenuhnya berada pada tombol pemanggilnya
	 *   &mdash; dan gerbang tombol itu adalah bentuk paling longgar di kelas ini, karena hanya menguji
	 *   field helper tanpa melihat pengguna yang sedang login.</li>
	 *   <li><b>Hanya mahasiswa yang diproses.</b> Kueri anggota menyaring {@code isNotNull("mahasiswa")},
	 *   sehingga anggota kelompok yang berupa siswa (jalur sekolah) tidak pernah ditandai.</li>
	 *   <li><b>Pertemuan sasaran berasal dari pemanggil.</b> Absensi ditulis ke pertemuan yang diserahkan
	 *   sebagai parameter, yang di layar diambil dari {@code tugasKelompok.ambilPertemuan()}. Bila
	 *   rujukan pertemuan pada tugas menunjuk ke semester lain &mdash; misalnya pada tugas hasil salinan
	 *   "Ambil Tugas Sebelumnya", yang tidak mereset rujukan itu &mdash; absensi akan tertulis ke
	 *   pertemuan semester tersebut.</li>
	 *   <li>Pemeriksaan {@code tugas == null} di dalam penyusunan keterangan tidak pernah bernilai benar,
	 *   karena {@code tugas} sudah dipakai di baris-baris sebelumnya.</li>
	 * </ul>
	 *
	 * @param tugas         tugas kelompok yang pengumpulannya diperiksa; judul dan batas waktunya dipakai
	 *                      pada keterangan absensi
	 * @param pa            pertemuan yang absensinya diisi
	 * @param eventListener callback yang dijalankan setelah seluruh penandaan selesai
	 * @throws Exception bila dialog konfirmasi gagal ditampilkan
	 * @see #uploadTugasDiangapHadir(TugasKelompok, Pertemuan, EventListener)
	 */
	public static void tidakUploadTugasDiangapTidakHadir(final TugasKelompok tugas, final Pertemuan pa,
			final EventListener eventListener) throws Exception {
		MyMessageboxConfig.show(
				"Apakah yakin semua mahasiswa yang tidak mengumpulkan \"" + tugas.getJudultugas()
						+ "\" dianggap alpa atau mangkir di kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {
									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									List<NamaTugasKelompok> namaTugasKelompoks = session
											.createCriteria(NamaTugasKelompok.class).addOrder(Order.asc("id"))
											.add(Restrictions.eq("tugasKelompok", tugas)).list();
									for (NamaTugasKelompok namaTugasKelompok : namaTugasKelompoks) {

										LampiranLain tgs = LampiranLain.ambil(namaTugasKelompok.getId(),
												NamaTugasKelompok.class.getName());

										if (tgs == null || tgs.getId() == null) {
											List<Mahasiswa> mahasiswasTemorary = ConstantValues.simpleList(
													session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
															.setProjection(Projections.groupProperty("mahasiswa.id"))
															.add(Restrictions.isNotNull("mahasiswa")).add(Restrictions
																	.eq("namaTugasKelompok", namaTugasKelompok)),
													Mahasiswa.class, false);

											for (Mahasiswa o : mahasiswasTemorary) {

												Long mhs = o.getId();
												if (!tugas.getMhsYgTidakIkut().contains("," + mhs + ",")) {
													Statusabsensi statusabsensi = ConstantValues.TIDAK_ADA_ALASAN;

													String mulai = pa.retreiveAbsensiMulai(mhs);
													String sampai = pa.retreiveAbsensiSampai(mhs);
													if (mulai == null || mulai.trim().isEmpty()) {
														mulai = pa.getWaktuMulai();
													}
													if (sampai == null || sampai.trim().isEmpty()) {
														sampai = pa.getWaktuSelesai();
													}

													pa.populate(mhs, statusabsensi,
															"Tidak Mengumpulkan \"" + tugas.getJudultugas()
																	+ "\" sampai tanggal/waktu "
																	+ Common.dateFormat5.get().format(
																			tugas == null || tugas.getSelesai() == null
																					? WaktuUtil.getDate()
																					: tugas.getSelesai()),
															null, mulai, sampai, "Mahasiswa");
												}

											}
										}
									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				});
	}

	/**
	 * <h3>Menandai HADIR seluruh anggota kelompok yang mengumpulkan tugas</h3>
	 *
	 * <p><b>Untuk apa (bahasa sederhana):</b> kebalikan dari
	 * {@link #tidakUploadTugasDiangapTidakHadir}. Sekali tekan, semua mahasiswa yang kelompoknya SUDAH
	 * mengumpulkan berkas ditandai hadir pada pertemuan yang bersangkutan &mdash; berguna untuk kelas
	 * daring, tempat pengumpulan tugas dipakai sebagai bukti kehadiran.</p>
	 *
	 * <p>Alurnya bercermin persis: konfirmasi lebih dulu, pelaksanaan ditunda lewat timer, penilaian
	 * dilakukan PER KELOMPOK, dan waktu hadir diambil dari catatan absensi yang ada dengan jam pertemuan
	 * sebagai cadangan. Yang membedakan hanyalah syarat dan hasilnya: di sini kelompok yang MEMILIKI
	 * berkas membuat seluruh anggotanya ditandai hadir, dan keterangan absensinya mencatat kapan berkas
	 * itu terakhir diunggah.</p>
	 *
	 * <h4>Dua perbedaan dari kembarannya yang perlu diketahui</h4>
	 * <ol>
	 *   <li><b>Daftar pengecualian TIDAK diperiksa.</b> Berbeda dari {@link #tidakUploadTugasDiangapTidakHadir}
	 *   yang melewati peserta pada {@code mhsYgTidakIkut}, metode ini menandai hadir seluruh anggota
	 *   kelompok tanpa terkecuali &mdash; termasuk peserta yang justru dikecualikan dari kewajiban tugas
	 *   ini. Ketidaksimetrisan ini tampaknya tidak disengaja.</li>
	 *   <li><b>Keterangan absensi salah label.</b> Teks yang dicatat berbunyi "... dengan nama file :
	 *   &lt;nilai&gt;", padahal nilai yang disisipkan adalah {@code o.getNama()}, yaitu NAMA MAHASISWA,
	 *   bukan nama berkas. Sisa salin-tempel; berdampak pada keterbacaan catatan absensi saja, tidak pada
	 *   status kehadiran yang tersimpan.</li>
	 * </ol>
	 *
	 * <p>Catatan pemelihara lain berlaku sama seperti pada kembarannya: tidak ada pemeriksaan wewenang
	 * (metode {@code public static}, dijaga hanya oleh tombol pemanggil yang gerbangnya paling longgar di
	 * kelas ini), hanya anggota bertipe mahasiswa yang diproses sehingga jalur sekolah terlewat, dan
	 * pertemuan sasaran sepenuhnya berasal dari pemanggil sehingga rujukan pertemuan yang salah &mdash;
	 * misalnya pada tugas hasil salinan lintas semester &mdash; akan menulis absensi ke pertemuan yang
	 * keliru.</p>
	 *
	 * @param tugas         tugas kelompok yang pengumpulannya diperiksa; judulnya dipakai pada keterangan
	 *                      absensi
	 * @param pa            pertemuan yang absensinya diisi
	 * @param eventListener callback yang dijalankan setelah seluruh penandaan selesai
	 * @throws Exception bila dialog konfirmasi gagal ditampilkan
	 * @see #tidakUploadTugasDiangapTidakHadir(TugasKelompok, Pertemuan, EventListener)
	 */
	public static void uploadTugasDiangapHadir(final TugasKelompok tugas, final Pertemuan pa,
			final EventListener eventListener) throws Exception {
		MyMessageboxConfig.show(
				"Apakah yakin semua mahasiswa yang mengumpulkan \"" + tugas.getJudultugas()
						+ "\" dianggap hadir kelas ini ?",
				"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
				new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							Common.createDefaultTimer(new EventListener() {

								@SuppressWarnings("unchecked")
								@Override
								public void onEvent(Event arg0) throws Exception {

									Session session = HibernateUtil.currentSession();
									if (pa.getId() != null) {
										session.refresh(pa);
									}

									List<NamaTugasKelompok> namaTugasKelompoks = session
											.createCriteria(NamaTugasKelompok.class).addOrder(Order.asc("id"))
											.add(Restrictions.eq("tugasKelompok", tugas)).list();
									for (NamaTugasKelompok namaTugasKelompok : namaTugasKelompoks) {

										LampiranLain tgs = LampiranLain.ambil(namaTugasKelompok.getId(),
												NamaTugasKelompok.class.getName());

										if (tgs != null && tgs.getId() != null) {
											List<Mahasiswa> mahasiswasTemorary = ConstantValues.simpleList(
													session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
															.setProjection(Projections.groupProperty("mahasiswa.id"))
															.add(Restrictions.isNotNull("mahasiswa")).add(Restrictions
																	.eq("namaTugasKelompok", namaTugasKelompok)),
													Mahasiswa.class, false);

											for (Mahasiswa o : mahasiswasTemorary) {
												Date uploadDate = tgs.getTanggal_dirubah();
												String nama = o.getNama();
												Statusabsensi statusabsensi = ConstantValues.MASUK;

												String mulai = pa.retreiveAbsensiMulai(o.getId());
												String sampai = pa.retreiveAbsensiSampai(o.getId());
												if (mulai == null || mulai.trim().isEmpty()) {
													mulai = pa.getWaktuMulai();
												}
												if (sampai == null || sampai.trim().isEmpty()) {
													sampai = pa.getWaktuSelesai();
												}

												pa.populate(o.getId(), statusabsensi,
														"Mengumpulkan \"" + tugas.getJudultugas() + "\" pada "
																+ Common.dateFormat5.get().format(uploadDate)
																+ " dengan nama file : " + nama,
														null, mulai, sampai, "Mahasiswa");
											}
										}
									}

									Common.refreshUpdate(session, pa);

									Common.createDefaultTimer(eventListener);

								}
							});

						}

					}
				});
	}
}
