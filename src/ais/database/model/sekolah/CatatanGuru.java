package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;
import org.zkoss.zul.Row;

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.GeneralValueObject;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.WaktuUtil;

/**
 * Baris <b>catatan guru</b> &mdash; satu catatan kepegawaian/pembinaan yang ditulis pihak
 * berwenang (kepala sekolah, pengawas, admin sekolah) mengenai seorang {@link Guru}.
 *
 * <p>Tabel: {@code sekolah.catatan_guru}. Satu baris = satu catatan untuk SATU guru pada satu
 * tahun ajaran + semester, dikategorikan oleh {@link JenisCatatanGuru} dan dapat dilengkapi
 * sejumlah <i>field kustom</i> (&quot;parameter tambahan&quot;) yang bentuknya ditentukan oleh
 * jenis catatan tersebut. Entity ini adalah <b>padanan GURU</b> dari
 * {@code ais.database.model.sekolah.CatatanSiswa}: struktur kolom, pola serialisasi parameter
 * tambahan, dan bahkan nilai {@code serialVersionUID}-nya identik &mdash; yang berbeda adalah
 * subjeknya (guru, bukan siswa) dan, sebagaimana dirinci di bawah, hasil audit kontrol aksesnya.
 *
 * <h2>Domain (TERVERIFIKASI dari kode dan panduan aplikasi)</h2>
 * <p>Panduan resmi halaman ini ({@code webapp/WEB-INF/bantuan/catatan_guru.html}) menyatakan
 * halaman Catatan Guru dipakai untuk mendokumentasikan &quot;jurnal pelaksanaan mengajar,
 * observasi kelas, kendala, atau pencapaian&quot; dan secara eksplisit menyebut kaitannya dengan
 * <b>pembinaan</b> dan <b>penilaian kinerja</b> guru &mdash; termasuk peringatan bahwa isi catatan
 * &quot;memengaruhi keadilan pembinaan dan penilaian guru&quot;. Javadoc {@link JenisCatatanGuru}
 * menyebut contoh kategori yang lazim: &quot;Pembinaan&quot;, &quot;Surat Peringatan&quot;,
 * &quot;Penghargaan&quot;, &quot;Penilaian Kinerja&quot;. Dengan kata lain isi
 * {@link #getKeterangan()} beserta parameter tambahannya adalah <b>data kepegawaian bersifat
 * sensitif</b> (teguran, evaluasi kinerja, catatan pembinaan seorang pegawai), bukan sekadar
 * catatan administratif. Panduan yang sama menyatakan pengelolaannya &quot;umumnya dilakukan oleh
 * kepala sekolah, pengawas, atau pihak yang berwenang&quot; dan bahwa pembatasan hak akses
 * &quot;menjaga kerahasiaan catatan&quot; &mdash; ekspektasi kerahasiaan itu penting untuk menilai
 * temuan kontrol akses yang dirangkum di bagian akhir.</p>
 * <p>Tidak ada daftar jenis bawaan: {@link JenisCatatanGuru} tidak punya auto-seed, sehingga
 * seluruh nama kategori diketik sendiri oleh admin tiap sekolah. Entity ini karena itu serbaguna
 * &mdash; satu instalasi memakainya untuk jurnal mengajar, instalasi lain untuk surat peringatan.</p>
 *
 * <h2>Layar &amp; jalur pemakai (TERVERIFIKASI)</h2>
 * <ul>
 *   <li><b>Layar utama:</b> {@code ais.action.master.sekolah.CatatanGuruAction} +
 *       {@code /pages/master/sekolah/catatan_guru.zul}. Punya 5 tab: Dasbor, daftar/CRUD Catatan
 *       Guru, Jenis Catatan Guru, Manajemen Parameter, dan Laporan.</li>
 *   <li><b>Form isian dinamis:</b>
 *       {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener} &mdash;
 *       membangun baris input parameter tambahan, memvalidasi field wajib/lampiran wajib, lalu
 *       memanggil {@link #populateParameterTambahan(List)} saat simpan.</li>
 *   <li><b>Dasbor:</b> {@code ais.action.master.catatan.DasbordCatatan} dengan
 *       {@code Lingkup.GURU} (dimuat otomatis oleh {@code CatatanGuruAction.onDasbor()}).</li>
 *   <li><b>Laporan/cetak:</b> {@code ais.action.report.format1.sekolah.LaporanCatatanGuru}
 *       &mdash; cetak rekap per rentang tanggal, cetak per-catatan lewat tombol printer di grid,
 *       dan cetak otomatis setiap kali {@code onSave()} berhasil. Layout JasperReports-nya
 *       diunggah per {@link JenisCatatanGuru} sebagai {@link LampiranLain}.</li>
 *   <li><b>Dasbor agregat:</b> {@code ais.action.master.dashboard.admin.DasboardGuru} &mdash;
 *       kartu &quot;total catatan&quot;, &quot;top jenis catatan&quot;, &quot;top guru
 *       bercatatan&quot;, dan tren bulanan dihitung dari tabel ini.</li>
 *   <li><b>REST/mobile:</b> hanya SATU rute, yaitu {@code catatan_guru} pada
 *       {@code ais.action.servlet.api.ApiRouteRegistry} &rarr;
 *       {@code ais.action.servlet.api.LaporanApi.catatan_guru()} (cetak laporan PDF). TIDAK ADA
 *       rute CRUD mobile untuk entity ini &mdash; lihat catatan verifikasi di bawah.</li>
 *   <li><b>Modul JSP &quot;baru&quot;:</b> {@code webapp/WEB-INF/baru/modul/
 *       pagesmastersekolahcatatanguruzul/index.jsp} memanggil
 *       {@code ais.common.DynamicJspCrudGenerator.generate(CatatanGuru.class)} &mdash; CRUD
 *       generik berbasis metadata Hibernate, jalur akses KEDUA yang sepenuhnya terpisah dari
 *       layar ZK.</li>
 * </ul>
 *
 * <h2>Warisan {@link GeneralValueObject}</h2>
 * <p>{@link GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass}
 * &mdash; ia POJO abstrak biasa sehingga Hibernate <b>tidak memetakan properti induknya sama
 * sekali</b>. Karena itu {@link #getId()}, {@link #getOleh()}, {@link #getOlehId()}, dan
 * {@link #getTanggal_dirubah()} <b>harus</b> dideklarasikan ulang di kelas ini, begitu pula field
 * {@code nama} dan {@code keterangan} yang menutupi (<i>shadow</i>) field senama milik induk.
 * Duplikasi tersebut adalah KEHARUSAN TEKNIS, bukan bug &mdash; jangan &quot;dirapikan&quot;
 * dengan menghapusnya.</p>
 * <p>Kelas ini juga <b>tidak</b> meng-{@code override} {@link GeneralValueObject#compareTo}.
 * Implementasi induk memanggil <i>getter</i> (bukan field) sehingga tetap bekerja secara virtual;
 * kunci {@code nomorUrut} dan {@code nim} milik induk selalu {@code null} pada instance ini,
 * sehingga pengurutan alami praktis selalu jatuh ke kunci ketiga, yaitu {@link #getNama()} &mdash;
 * yang (lihat kuirk di bawah) berisi <b>nama guru</b>, bukan judul catatan.</p>
 * <p>Getter relasi memakai {@code check(...)} milik induk untuk meresolusi proxy lazy sebelum
 * mengembalikan nilai; bila keempat tahap resolusi gagal, {@code check()} mengembalikan argumen
 * apa adanya (tidak melempar).</p>
 *
 * <h2>Model data &amp; relasi</h2>
 * <ul>
 *   <li>{@link #getGuru()} &rarr; {@link Guru} (FK {@code guru}) &mdash; SUBJEK catatan (guru yang
 *       dicatat), sekaligus sumber turunan untuk {@link #getKode()}, {@link #getNama()},
 *       {@link #getSekolah()}, dan tidak langsung {@link #getYayasan()}. Perhatikan perbedaan
 *       penting dari {@code CatatanSiswa}: di sana {@code guru} adalah PENULIS catatan, di sini
 *       {@code guru} adalah OBJEK catatan. Entity ini <b>tidak menyimpan relasi ke penulis</b>
 *       sama sekali &mdash; identitas penulis hanya terekam sebagai teks pada
 *       {@link #getOleh()}/{@link #getOlehId()} dan pada riwayat Envers.</li>
 *   <li>{@link #getJenisCatatanGuru()} &rarr; {@link JenisCatatanGuru} (FK
 *       {@code jenis_catatan_guru}) &mdash; kategori catatan sekaligus penentu formulir parameter
 *       tambahan yang muncul dan template cetaknya.</li>
 *   <li>{@link #getSekolah()}/{@link #getYayasan()} (FK {@code sekolah_id}/{@code yayasan_id})
 *       &mdash; kolom cakupan multi-tenant.</li>
 *   <li>Lampiran berkas TIDAK punya relasi langsung: berkas disimpan sebagai
 *       {@link LampiranLain} dan ditemukan lewat pasangan
 *       {@code (ref = id catatan, jenis = "{idKelompok}-&gt;{idParameter}")} &mdash; lihat
 *       {@link #populateParameterTambahan(List)}.</li>
 * </ul>
 *
 * <h2>Parameter tambahan (field kustom) &mdash; dua kolom teks terdenormalisasi</h2>
 * <p>Nilai field kustom tidak disimpan dalam tabel terpisah, melainkan diserialkan ke DUA kolom
 * teks pada baris ini:</p>
 * <ul>
 *   <li>{@link #getParameterTambahan()} &mdash; versi <b>siap-baca manusia</b>; per baris:
 *       <code>namaKelompok-&gt;labelInputan&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok</code>.</li>
 *   <li>{@link #getParameterTambahanInds()} &mdash; versi <b>berkunci id</b> (dipakai untuk memuat
 *       ulang nilai ke form dan untuk merender kolom parameter pada grid); per baris:
 *       <code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;urlLampiran</code>.</li>
 * </ul>
 * <p>Antarbaris dipisah {@code "\n"}, antar-ruas {@code "<=>"}. Keduanya ditulis sekaligus oleh
 * {@link #populateParameterTambahan(List)}. Konsekuensi bentuk denormalisasi ini: mengubah label
 * atau nomor urut sebuah {@link ParameterTambahan} <b>tidak</b> memperbarui catatan lama &mdash;
 * catatan lama tetap menyimpan salinan label pada saat disimpan.</p>
 *
 * <h2>Kelompok method</h2>
 * <ul>
 *   <li><b>Jejak audit (deklarasi ulang wajib):</b> {@link #getOleh()}, {@link #setOleh(String)},
 *       {@link #getOlehId()}, {@link #setOlehId(String)}, {@link #onUpdate()},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}.</li>
 *   <li><b>Identitas &amp; representasi:</b> {@link #CatatanGuru()}, {@link #getId()},
 *       {@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Identitas subjek terdenormalisasi:</b> {@link #getKode()}, {@link #setKode(String)},
 *       {@link #getNama()}, {@link #setNama(String)}.</li>
 *   <li><b>Isi catatan:</b> {@link #getKeterangan()}, {@link #setKeterangan(String)},
 *       {@link #getWaktu()}, {@link #setWaktu(Date)}.</li>
 *   <li><b>Periode akademik:</b> {@link #getTahunAjaran()}, {@link #setTahunAjaran(String)},
 *       {@link #getSemester()}, {@link #setSemester(Integer)}.</li>
 *   <li><b>Relasi subjek &amp; kategori:</b> {@link #getGuru()}, {@link #setGuru(Guru)},
 *       {@link #getJenisCatatanGuru()}, {@link #setJenisCatatanGuru(JenisCatatanGuru)}.</li>
 *   <li><b>Cakupan multi-tenant:</b> {@link #getSekolah()}, {@link #setSekolah(Sekolah)},
 *       {@link #getYayasan()}, {@link #setYayasan(Yayasan)}.</li>
 *   <li><b>Parameter tambahan:</b> {@link #getParameterTambahan()},
 *       {@link #setParameterTambahan(String)}, {@link #getParameterTambahanInds()},
 *       {@link #setParameterTambahanInds(String)}, {@link #populateParameterTambahan(List)},
 *       {@link #ambilDataParameterTambahan()}.</li>
 * </ul>
 *
 * <h2>Kuirk &amp; catatan penting (semua TERVERIFIKASI dari kode)</h2>
 * <ol>
 *   <li><b>{@code nama} BUKAN judul catatan.</b> {@link #getNama()} menimpa dirinya dengan
 *       {@code getGuru().getNama()} setiap kali dibaca &mdash; kolom {@code nama} pada praktiknya
 *       adalah <i>nama guru</i> yang didenormalisasi. Isi catatan yang sebenarnya ada di
 *       {@link #getKeterangan()}. Hal serupa berlaku untuk {@link #getKode()} (kode guru).</li>
 *   <li><b>Enam getter melakukan write-back (menulis balik ke field).</b> {@link #getKode()},
 *       {@link #getNama()}, {@link #getSekolah()}, {@link #getYayasan()}, {@link #getSemester()},
 *       dan {@link #getTahunAjaran()} bukan getter murni: sekadar MEMBACA baris (mis. merender
 *       grid atau menyusun laporan) sudah mengubah state entity, dan karena entity dalam keadaan
 *       <i>managed</i> perubahan itu ikut ter-<i>flush</i> ke DB tanpa ada tombol Simpan yang
 *       ditekan. Dua di antaranya bersifat mengisi-diam-diam pada baris lama:
 *       {@link #getSemester()} dan {@link #getTahunAjaran()} menstempelkan periode
 *       <b>saat ini</b> ke baris warisan yang kolomnya masih {@code null} &mdash; catatan lama
 *       jadi seolah-olah dibuat pada tahun ajaran berjalan.</li>
 *   <li><b>{@link #getSekolah()}/{@link #getYayasan()} menulis ulang kolom tenant.</b> Sekolah
 *       catatan selalu diambil ulang dari {@code getGuru().getSekolah()} (dan yayasan mengekor
 *       sekolah). Memindahkan seorang guru ke sekolah lain dalam satu yayasan akan
 *       <b>secara retroaktif memindahkan seluruh catatan pembinaan lamanya</b> ke sekolah baru.
 *       Efek sampingnya di layar: nilai Sekolah/Yayasan yang dipilih operator pada form Tambah/Ubah
 *       praktis <b>diabaikan</b> selama guru terisi &mdash; {@code CatatanGuruAction.onSave()}
 *       memanggil {@code setSekolah(...)} dari combobox, tetapi pembacaan berikutnya menimpanya
 *       kembali dari guru.</li>
 *   <li><b>{@link #getWaktu()} adalah pengecualian yang justru menimbulkan masalah lain:</b> ia
 *       mengembalikan waktu SEKARANG bila kolomnya {@code null} tetapi <b>tidak</b> menulis balik.
 *       Akibatnya (a) dua pembacaan berturut-turut atas baris yang sama bisa memberi nilai
 *       berbeda, dan (b) baris dengan {@code waktu} {@code null} tetap {@code null} di DB sehingga
 *       <b>tidak pernah lolos</b> filter rentang tanggal
 *       {@code date(waktu) between ...} pada {@code LaporanCatatanGuru.generateParameter()} &mdash;
 *       catatannya tampil di grid tetapi hilang dari laporan.</li>
 *   <li><b>{@link #setOleh(String)}/{@link #setOlehId(String)} menolak nilai kosong secara
 *       senyap</b> (pola warisan {@link GeneralValueObject}) &mdash; jejak audit tidak bisa
 *       dikosongkan lagi setelah terisi, dan pemanggil tidak diberi tahu bahwa set-nya diabaikan.</li>
 *   <li><b>{@link #setSekolah(Sekolah)}/{@link #setYayasan(Yayasan)} membuang instance yang belum
 *       tersimpan</b> ({@code getId() == null} &rarr; disimpan sebagai {@code null}), sehingga
 *       merangkai objek baru sebelum menyimpannya menghasilkan kolom tenant kosong tanpa pesan.</li>
 *   <li><b>{@link #ambilDataParameterTambahan()} adalah kode mati</b> untuk kelas ini &mdash;
 *       tidak ada satu pun pemanggil di repositori (yang dipanggil adalah method senama milik
 *       entity lain seperti {@code KegiatanSiswa}/{@code IsiAngketParameterUmum}). Ia juga tidak
 *       pernah mengembalikan daftar kosong; lihat Javadoc method tersebut.</li>
 *   <li><b>{@link #toString()} membaca field {@code nama} langsung, bukan getter</b>, sehingga
 *       untuk entity yang baru dimuat ia bisa mencetak {@code null} padahal {@link #getNama()}
 *       akan mengembalikan nama guru.</li>
 *   <li><b>{@code serialVersionUID} identik dengan milik {@code CatatanSiswa}</b>
 *       ({@code 2463821577548439808L}) &mdash; sisa salin-tempel saat entity ini diturunkan dari
 *       saudaranya. Tidak berbahaya (serialisasi Java memakai nama kelas juga), tetapi menyesatkan
 *       saat menelusuri riwayat.</li>
 *   <li><b>Kolom {@code nama} dideklarasikan {@code nullable = false}</b> padahal
 *       {@link #getNama()} bisa mengembalikan {@code null} saat {@code guru} belum diisi &mdash;
 *       menyimpan baris tanpa guru gagal di level DB, bukan dengan pesan validasi yang ramah.</li>
 * </ol>
 *
 * <h2>Kontrol akses pada jalur pemakai (TERVERIFIKASI)</h2>
 * <p>Bagian ini merangkum hasil audit ulang untuk MENGUJI apakah pola temuan pada
 * {@code CatatanSiswa} terulang di sini. Dua di antaranya <b>TIDAK</b> terulang (verifikasi
 * negatif), sisanya terulang atau muncul dalam bentuk lain.</p>
 * <ul>
 *   <li><b>Seeder hak berlebihan: TIDAK ADA (verifikasi NEGATIF).</b>
 *       {@code ais.common.MenuInitializer.ensureSiswaRoleAndPrivileges()} memang memberi role
 *       {@code Tbmrole.SISWA} hak {@code create/read/update/delete = 1}, tetapi HANYA untuk tiga
 *       menu: {@code 431898} (Kuesioner Siswa), {@code 127616} (Rapor Siswa), dan {@code 48916}
 *       (Catatan Siswa). Menu Catatan Guru ({@code 18907} dan {@code 812131} pada
 *       {@code ais.common.MenuSnapshotData}) TIDAK termasuk, dan tidak muncul pada satu pun
 *       pemanggilan {@code addPrivilegeToRoles(...)} di kelas itu. Tidak ada role yang di-seed
 *       otomatis dengan hak atas modul ini.</li>
 *   <li><b>IDOR REST seperti {@code CatatanApi}: TIDAK ADA (verifikasi NEGATIF).</b>
 *       {@code ais.action.servlet.api.CatatanApi} melayani <b>khusus</b> {@code CatatanSiswa};
 *       Javadoc kelasnya menyebut {@code CatatanGuruAction} hanya sebagai saudara yang
 *       ekstensinya &quot;bersifat mekanis&quot;, dan ekstensi itu <b>belum dibuat</b>. Satu-satunya
 *       rute API yang menyentuh entity ini, {@code LaporanApi.catatan_guru()}, justru
 *       <b>fail-closed</b>: laporan disusun untuk {@code tbmuser.ambilGuru()} milik pemegang token
 *       sendiri, dan {@code LaporanCatatanGuru.generateParameter()} mengembalikan {@code null}
 *       (bukan seluruh tabel) bila guru itu {@code null}. Akun non-guru tidak memperoleh data.</li>
 *   <li><b>Gerbang layar utama: ADA dan BENAR (contoh POSITIF).</b> Berbeda dari sejumlah layar
 *       yang diaudit batch-batch sebelumnya, {@code CatatanGuruAction.doBeforeCompose()} memanggil
 *       {@code Common.doCheckSecurity()} &rarr; {@code CommonPrivilages.doCheckPrevilagesRead()},
 *       dan tombol Tambah/Ubah/Hapus masing-masing digerbangi
 *       {@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE)}.</li>
 *   <li><b>TAPI tidak ada pembatas kepemilikan sama sekali.</b>
 *       {@code CatatanGuruAction.initCriteria()} menyaring berdasarkan jenis, guru (dari bandbox
 *       pencarian), semester, tahun ajaran, sekolah, yayasan, dan kata kunci nama/kode/NUPTK
 *       &mdash; tidak ada pembatas &quot;guru = guru milik akun&quot;.
 *       {@code doAfterCompose()} hanya menyembunyikan dua tab master bagi akun guru, tanpa
 *       menyentuh data. Konsekuensinya: setiap akun yang punya hak BACA menu ini &mdash; termasuk
 *       akun guru biasa &mdash; dapat <b>membaca, mengubah, dan menghapus catatan pembinaan
 *       SELURUH guru</b> pada instalasi, termasuk menghapus surat peringatan atas dirinya
 *       sendiri.</li>
 *   <li><b>Fail-open cakupan tenant.</b> Seluruh filter pada {@code initCriteria()} memakai pola
 *       {@code Restrictions.sqlRestriction("1=1")} ketika combobox tidak dipilih, sedangkan
 *       combobox Yayasan/Sekolah diisi {@code Common.initYayasanDanSekolahDanSemua(...)} yang
 *       menyediakan opsi &quot;Semua&quot;. Keadaan bawaan layar karenanya adalah
 *       <b>tanpa pembatas tenant</b> &mdash; daftar mencakup seluruh yayasan dan sekolah dalam
 *       satu instalasi.</li>
 *   <li><b>Fail-open pada dasbor.</b> {@code DasbordCatatan.muatCatatanGuru()} memblokir akun
 *       siswa ({@code sis != null} &rarr; {@code return}) dan membatasi akun guru ke dirinya
 *       sendiri ({@code Restrictions.eq("guru", gur)}); tetapi untuk akun yang bukan keduanya
 *       (pegawai, staf, wali murid, admin) <b>TIDAK ADA filter apa pun</b> &mdash; hingga
 *       {@code MAX_ROWS = 600} catatan pembinaan lintas guru dan lintas sekolah dirender apa
 *       adanya, lengkap dengan {@code keterangan} dan parameter tambahannya.</li>
 *   <li><b>Ekspor massal tanpa gerbang tambahan.</b> {@code doAfterCompose()} memasang tombol
 *       unduh {@code Common.cetakData(CatatanGuru.class, this, contents)} ke toolbar
 *       <b>tanpa pemeriksaan hak apa pun</b> (berbeda dari tombol Tambah di sebelahnya). Tombol
 *       itu mengekspor hasil {@code initCriteria()} &mdash; yaitu, dengan fail-open di atas,
 *       seluruh catatan pembinaan guru satu instalasi &mdash; ke Excel, cukup berbekal hak BACA.</li>
 *   <li><b>Pewarisan hak lewat menu induk (dua tingkat).</b> Tab &quot;Jenis Catatan Guru&quot;
 *       dan &quot;Manajemen Parameter&quot; me-{@code MyInclude} halaman master
 *       {@code jenis_catatan_guru.zul} dan {@code parameter_tambahan_catatan_guru.zul} ke dalam
 *       jendela ini; layar kedua itu sendiri kemudian menyisipkan
 *       {@code kelompok_parameter_tambahan_catatan_guru.zul}. Karena
 *       {@code CommonPrivilages.checkPrevilages()} memutuskan berdasarkan
 *       {@code Common.getCurrentMenu()} (menu terakhir yang diklik), hak atas menu
 *       &quot;Catatan Guru&quot; efektif menjadi hak CRUD atas <b>tiga</b> master konfigurasi
 *       tersebut. Menu yang sama juga terdaftar ganda pada dua induk berbeda
 *       ({@code 18907} di bawah {@code 431} dan {@code 812131} di bawah {@code 5701} pada
 *       {@code MenuSnapshotData}), sehingga tingkat hak yang berlaku bergantung pada dari mana
 *       halaman dibuka.</li>
 *   <li><b>Jalur JSP generik tanpa gerbang BACA.</b> {@code DynamicJspCrudGenerator} (dipakai
 *       modul {@code /WEB-INF/baru/modul/pagesmastersekolahcatatanguruzul}) menyediakan
 *       {@code canCreate/canEdit/canDelete} &mdash; semuanya bersandar pada
 *       {@code Common.getCurrentMenu()} yang sama &mdash; tetapi <b>tidak punya padanan
 *       {@code canRead}</b>: daftar barisnya dirender untuk sesi mana pun yang sudah login. Ini
 *       jalur akses kedua atas tabel yang sama, dengan gerbang yang lebih longgar daripada layar
 *       ZK-nya.</li>
 *   <li><b>Contoh POSITIF (fail-closed) sebagai pembanding:</b>
 *       {@code LaporanCatatanGuru} mengunci bandbox pilihan guru ke akun sendiri bila
 *       {@code Common.getCurrentUser().getGuru() != null}, dan
 *       {@code generateParameter(...)} mengembalikan {@code null} saat guru kosong &mdash;
 *       kontras dengan {@code LaporanCatatanSiswa} yang membiarkan akun wali murid memilih siswa
 *       mana pun.</li>
 * </ul>
 *
 * <h2>Bug fungsional pada pemanggil (TERVERIFIKASI, dicatat apa adanya)</h2>
 * <ul>
 *   <li>{@code CatatanGuruAction.doAfterCompose()} menguji
 *       {@code tbmuser.ambilGuru() != null || tbmuser.ambilGuru() != null} &mdash; kondisi yang
 *       sama diulang dua kali (salin-tempel); cabang kedua tidak pernah menambah apa pun.</li>
 *   <li>{@code CatatanGuruAction.initCriteria()} membangun
 *       {@code Restrictions.or(eq("guru", x), eq("guru", x))} &mdash; juga kondisi kembar identik,
 *       kemungkinan besar sisa dari layar {@code CatatanSiswa} yang meng-OR-kan dua kolom berbeda
 *       ({@code siswa} dan {@code guru}).</li>
 *   <li>{@code CatatanGuruAction.CatatanGuruRenderer.render()} memanggil {@code guru.getKode()}
 *       dan {@code session.refresh(j)} tanpa uji {@code null}. Kolom {@code guru} dan
 *       {@code jenis_catatan_guru} tidak {@code NOT NULL} di sisi entity, dan jalur JSP generik di
 *       atas bisa membuat baris tanpa keduanya &mdash; satu baris cacat cukup untuk menggagalkan
 *       render SELURUH grid.</li>
 * </ul>
 *
 * @see JenisCatatanGuru
 * @see KelompokParameterTambahanCatatanGuru
 * @see ParameterTambahanCatatanGuru
 * @see Guru
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true, 
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "catatan_guru")
public class CatatanGuru extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Dipertahankan tetap agar sesi ZK/objek ter-serialisasi dari rilis
	 * sebelumnya tetap dapat dibaca; JANGAN diubah saat menambah field. Nilainya kebetulan
	 * identik dengan milik {@code CatatanSiswa} (sisa salin-tempel) &mdash; bukan masalah, karena
	 * serialisasi Java juga mencocokkan nama kelas.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama; dideklarasikan ulang karena {@link GeneralValueObject} tidak dipetakan Hibernate. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit, deklarasi ulang wajib). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini (jejak audit, deklarasi ulang wajib). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} atau string yang hanya berisi spasi
	 * <b>diabaikan secara senyap</b> (method langsung {@code return} tanpa mengubah apa pun dan
	 * tanpa memberi tahu pemanggil). Konsekuensinya jejak audit tidak dapat dikosongkan kembali
	 * setelah pernah terisi.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong
	 * diabaikan secara senyap.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor} pada alur simpan biasa; jadi bagi
	 * catatan pembinaan, inilah satu-satunya jejak &quot;siapa yang menulis&quot; pada baris itu
	 * sendiri &mdash; entity ini tidak punya relasi ke penulis.</p>
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait {@code @PreUpdate} JPA plus deklarasi field {@code tanggal_dirubah} (keduanya sengaja
	 * berada pada satu baris fisik, mengikuti bentuk yang dipakai seluruh entity keluarga ini).
	 *
	 * <p>{@code onUpdate()} dipanggil Hibernate tepat sebelum {@code UPDATE} dieksekusi dan
	 * mendelegasikan ke {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} yang
	 * mengisi {@link #setOleh(String)}/{@link #setOlehId(String)} serta
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna aktif. Field
	 * {@code tanggal_dirubah} sendiri diinisialisasi ke waktu pembuatan objek
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru tidak pernah punya stempel kosong.</p>
	 *
	 * <p><b>Perhatian:</b> kait ini hanya berjalan pada alur ORM. Operasi massal berbasis HQL/SQL
	 * native melewatinya, sehingga stempel audit tidak diperbarui untuk perubahan semacam itu.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya tidak dipanggil langsung oleh kode layar; diisi {@link #onUpdate()} lewat
	 * {@code AuditTimestampInterceptor}. {@code LaporanCatatanGuru} memakai nilai ini sebagai
	 * tanggal cetak pada mode cetak per-catatan.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang dibuat
	 *         lewat konstruktor karena field-nya diinisialisasi saat instansiasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas berbentuk {@code "{id}-{nama}"}.
	 *
	 * <p><b>Kuirk:</b> membaca <b>field</b> {@code nama} secara langsung, bukan
	 * {@link #getNama()}. Untuk entity yang baru dimuat dan belum pernah dibaca lewat getter,
	 * hasilnya bisa memuat {@code null} padahal {@link #getNama()} akan mengembalikan nama guru.</p>
	 *
	 * @return string {@code "{id}-{nama}"}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode guru yang didenormalisasi; selalu ditimpa dari {@code guru} saat dibaca. */
	private String kode;
	/** SUBJEK catatan: guru yang dicatat (bukan penulis catatan). */
	private Guru guru;
	/** Nama guru yang didenormalisasi; selalu ditimpa dari {@code guru} saat dibaca. */
	private String nama;
	/** Isi catatan sesungguhnya (teks bebas, kolom {@code text}). */
	private String keterangan;
	/** Tanggal/waktu kejadian yang dicatat; boleh {@code null} di DB. */
	private Date waktu;
	/** Kategori catatan sekaligus penentu bentuk formulir parameter tambahan. */
	private JenisCatatanGuru jenisCatatanGuru;
	/** Kolom cakupan tenant tingkat sekolah; diturunkan dari {@code guru}. */
	private Sekolah sekolah;
	/** Kolom cakupan tenant tingkat yayasan; diturunkan dari {@code sekolah}. */
	private Yayasan yayasan;
	/** Nilai field kustom versi siap-baca manusia (serialisasi teks). */
	private String parameterTambahan;
	/** Nilai field kustom versi berkunci id (serialisasi teks). */
	private String parameterTambahanInds;
	/** Tahun ajaran berlakunya catatan, format {@code "yyyy/yyyy"}. */
	private String tahunAjaran;
	/** Semester berlakunya catatan: {@code 1} = ganjil, {@code 2} = genap. */
	private Integer semester;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Dipakai juga oleh {@code CatatanGuruAction.onAdd()} untuk menyiapkan baris kosong pada
	 * form Tambah. Seluruh field dibiarkan {@code null}; nilai bawaan periode akademik baru
	 * terbentuk saat {@link #getTahunAjaran()}/{@link #getSemester()} dibaca pertama kali.</p>
	 */
	public CatatanGuru() {
	}

	/**
	 * Mengembalikan kunci utama baris.
	 *
	 * <p>Dipakai juga sebagai {@code ref} saat mencari/menautkan {@link LampiranLain} milik
	 * parameter tambahan (lihat {@link #populateParameterTambahan(List)}).</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris.
	 *
	 * @param id kunci utama; {@code null} untuk baris baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode guru yang didenormalisasi pada baris ini.
	 *
	 * <p><b>Bukan getter murni.</b> Bila {@link #getGuru()} tidak {@code null}, field {@code kode}
	 * <b>ditimpa</b> dengan {@code getGuru().getKode()} lebih dulu; pada entity yang sedang
	 * <i>managed</i> penimpaan itu ikut ter-<i>flush</i> ke DB meski tidak ada tombol Simpan yang
	 * ditekan. Efeknya: kolom {@code kode} selalu mengikuti kode guru saat ini, bukan kode pada
	 * saat catatan dibuat.</p>
	 * <p>Berbeda dari {@link #getNama()}, method ini menormalkan hasil menjadi string kosong
	 * (bukan {@code null}) supaya aman dipakai langsung pada label ZK.</p>
	 *
	 * @return kode guru tanpa spasi tepi; string kosong bila belum ada nilai
	 */
	public String getKode() {
		if (getGuru() != null) {
			kode = getGuru().getKode();
		}
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Menetapkan kode yang didenormalisasi.
	 *
	 * <p>Praktis tidak berpengaruh jangka panjang: nilai apa pun akan ditimpa kembali oleh
	 * {@link #getKode()} selama {@code guru} terisi.</p>
	 *
	 * @param kode kode guru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama guru yang didenormalisasi pada baris ini.
	 *
	 * <p><b>Bukan getter murni, dan BUKAN judul catatan.</b> Bila {@link #getGuru()} tidak
	 * {@code null}, field {@code nama} <b>ditimpa</b> dengan {@code getGuru().getNama()}; pada
	 * entity <i>managed</i> penimpaan itu ikut ter-<i>flush</i> ke DB. Kolom {@code nama}
	 * karenanya berfungsi sebagai salinan nama guru, bukan judul catatan &mdash; isi catatan yang
	 * sesungguhnya ada di {@link #getKeterangan()}.</p>
	 * <p>Nama ini juga menjadi kunci pengurutan alami efektif entity ini, karena
	 * {@link GeneralValueObject#compareTo} milik induk jatuh ke kunci ketiga
	 * ({@code nama}) saat {@code nomorUrut} dan {@code nim} bernilai {@code null}.</p>
	 * <p><b>Perhatian:</b> kolom dipetakan {@code nullable = false}, tetapi method ini dapat
	 * mengembalikan {@code null} saat {@code guru} belum diisi &mdash; menyimpan baris seperti itu
	 * gagal di level basis data.</p>
	 *
	 * @return nama guru tanpa spasi tepi, atau {@code null} bila belum ada nilai sama sekali
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getGuru() != null) {
			nama = getGuru().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama yang didenormalisasi.
	 *
	 * <p>Sama seperti {@link #setKode(String)}, nilai apa pun akan ditimpa kembali oleh
	 * {@link #getNama()} selama {@code guru} terisi.</p>
	 *
	 * @param nama nama guru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan isi catatan (teks bebas).
	 *
	 * <p>Inilah muatan sesungguhnya entity ini: uraian pembinaan, hasil observasi kelas, kendala,
	 * atau isi teguran. Diisi lewat {@code Textbox} tiga baris pada form Tambah/Ubah, dirender
	 * apa adanya pada grid, dasbor, dan laporan cetak. <b>Perlakukan sebagai data kepegawaian
	 * sensitif.</b></p>
	 *
	 * @return isi catatan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan isi catatan.
	 *
	 * @param keterangan isi catatan (teks bebas, boleh {@code null})
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan semester berlakunya catatan.
	 *
	 * <p><b>Bukan getter murni.</b> Bila kolomnya masih {@code null}, method ini
	 * <b>menulis balik</b> semester berjalan hasil {@code Common.isNowSemensterGanjil()}
	 * ({@code 1} = ganjil, {@code 2} = genap). Pada entity <i>managed</i>, sekadar membaca baris
	 * warisan yang kolom semesternya kosong sudah menstempelkan semester <b>saat ini</b> ke baris
	 * tersebut secara permanen &mdash; catatan lama jadi seolah-olah dibuat pada semester
	 * berjalan.</p>
	 *
	 * @return {@code 1} untuk semester ganjil atau {@code 2} untuk genap; tidak pernah
	 *         {@code null} setelah pemanggilan pertama
	 */
	@Column(name = "semester", nullable = true)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * Menetapkan semester berlakunya catatan.
	 *
	 * @param semester {@code 1} (ganjil) atau {@code 2} (genap)
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * Mengembalikan tahun ajaran berlakunya catatan.
	 *
	 * <p><b>Bukan getter murni.</b> Persis seperti {@link #getSemester()}, bila kolomnya masih
	 * {@code null} method ini <b>menulis balik</b> tahun akademik berjalan hasil
	 * {@code Common.getCurrentTahunAkademik()} &mdash; sehingga membaca baris warisan sudah
	 * mengubahnya.</p>
	 *
	 * @return tahun ajaran, format {@code "yyyy/yyyy"}; tidak pernah {@code null} setelah
	 *         pemanggilan pertama
	 */
	@Column(name = "tahun_ajaran", nullable = true, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * Menetapkan tahun ajaran berlakunya catatan.
	 *
	 * @param tahunAjaran tahun ajaran, format {@code "yyyy/yyyy"}
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}



	/**
	 * Mengembalikan tanggal/waktu kejadian yang dicatat.
	 *
	 * <p><b>Kuirk penting &mdash; nilai bawaan yang TIDAK ditulis balik.</b> Bila kolom
	 * {@code waktu} bernilai {@code null}, method ini mengembalikan
	 * {@code WaktuUtil.getDate()} (waktu <i>sekarang</i>) <b>tanpa</b> menyimpannya ke field.
	 * Dua akibatnya:</p>
	 * <ul>
	 *   <li>dua pembacaan berturut-turut atas baris yang sama dapat memberi nilai berbeda; dan</li>
	 *   <li>kolomnya tetap {@code null} di basis data, sehingga baris tersebut <b>tidak pernah
	 *       lolos</b> filter rentang {@code date(waktu) between ...} pada
	 *       {@code LaporanCatatanGuru.generateParameter()} maupun filter tanggal API
	 *       &mdash; catatannya tampil di grid tetapi hilang dari laporan.</li>
	 * </ul>
	 *
	 * @return waktu kejadian; waktu saat ini bila kolomnya belum diisi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan tanggal/waktu kejadian yang dicatat.
	 *
	 * <p>Diisi dari {@code MyDatebox} pada form Tambah/Ubah ({@code CatatanGuruAction.onSave()}).</p>
	 *
	 * @param waktu waktu kejadian; {@code null} akan memicu perilaku bawaan {@link #getWaktu()}
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Mengembalikan kategori catatan.
	 *
	 * <p>Selain menamai catatan, {@link JenisCatatanGuru} menentukan <b>kelompok parameter
	 * tambahan mana</b> yang muncul pada formulir (lihat
	 * {@link #populateParameterTambahan(List)}) dan <b>template JasperReports</b> mana yang
	 * dipakai saat mencetak.</p>
	 * <p>Memanggil {@code check(...)} milik {@link GeneralValueObject} lebih dulu untuk
	 * meresolusi proxy lazy; bila resolusi gagal, proxy dikembalikan apa adanya (tidak melempar).</p>
	 *
	 * @return kategori catatan, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_catatan_guru")
	public JenisCatatanGuru getJenisCatatanGuru() {
		jenisCatatanGuru = check(jenisCatatanGuru);
		return jenisCatatanGuru;
	}

	/**
	 * Menetapkan kategori catatan.
	 *
	 * <p>Mengubah nilai ini setelah catatan tersimpan <b>tidak</b> memigrasikan nilai parameter
	 * tambahan yang sudah terserialisasi: kunci {@code "{idKelompok}-&gt;{idParameter}"} milik
	 * jenis lama tetap tersimpan dan tidak akan cocok dengan formulir jenis yang baru.</p>
	 *
	 * @param jenisCatatanGuru kategori catatan
	 */
	public void setJenisCatatanGuru(JenisCatatanGuru jenisCatatanGuru) {
		this.jenisCatatanGuru = jenisCatatanGuru;
	}

	/**
	 * Mengembalikan guru yang menjadi SUBJEK catatan ini.
	 *
	 * <p><b>Perhatikan perbedaan dari {@code CatatanSiswa}:</b> di sana kolom {@code guru} berisi
	 * guru <i>penulis/pembina</i>; di sini ia berisi guru yang <i>dicatat</i>. Entity ini tidak
	 * punya relasi ke penulis sama sekali &mdash; jejak penulis hanya berupa
	 * {@link #getOleh()}/{@link #getOlehId()} dan riwayat Envers.</p>
	 * <p>Relasi ini adalah sumber turunan bagi {@link #getKode()}, {@link #getNama()},
	 * {@link #getSekolah()}, dan (lewat sekolah) {@link #getYayasan()}. Memanggil
	 * {@code check(...)} lebih dulu untuk meresolusi proxy lazy.</p>
	 *
	 * @return guru subjek catatan, atau {@code null} bila belum dipilih
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru yang menjadi subjek catatan.
	 *
	 * <p>Diwajibkan oleh validasi {@code CatatanGuruAction.onSave()}. Mengubah nilai ini juga
	 * mengubah kode, nama, sekolah, dan yayasan yang tampil pada baris (semuanya turunan) pada
	 * pembacaan berikutnya.</p>
	 *
	 * @param guru guru subjek catatan
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan sekolah pemilik catatan (kolom cakupan multi-tenant).
	 *
	 * <p><b>Bukan getter murni, dan menimpa pilihan operator.</b> Bila {@link #getGuru()} tidak
	 * {@code null}, field {@code sekolah} selalu <b>diganti</b> dengan
	 * {@code getGuru().getSekolah()}; hanya bila guru kosong nilai tersimpan dipertahankan (lewat
	 * {@code check(...)}). Konsekuensi yang terverifikasi:</p>
	 * <ul>
	 *   <li>Sekolah yang dipilih operator pada form Tambah/Ubah praktis diabaikan &mdash;
	 *       {@code CatatanGuruAction.onSave()} memanggil {@link #setSekolah(Sekolah)} dari
	 *       combobox, tetapi pembacaan berikutnya menimpanya kembali dari guru.</li>
	 *   <li>Memindahkan seorang guru ke sekolah lain <b>secara retroaktif memindahkan seluruh
	 *       catatan pembinaan lamanya</b> ke sekolah baru; filter tenant pada daftar, dasbor, dan
	 *       laporan karenanya tidak mencerminkan keadaan historis.</li>
	 * </ul>
	 *
	 * @return sekolah pemilik catatan, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		if (getGuru() != null) {
			sekolah = getGuru().getSekolah();
		} else {
			sekolah = check(sekolah);
		}
		return this.sekolah;
	}

	/**
	 * Menetapkan sekolah pemilik catatan.
	 *
	 * <p><b>Perhatian:</b> instance yang belum tersimpan ({@code getId() == null}) maupun
	 * {@code null} sama-sama disimpan sebagai {@code null} &mdash; merangkai objek {@link Sekolah}
	 * baru lalu menyetelnya di sini menghasilkan kolom tenant kosong tanpa peringatan.</p>
	 * <p>Lihat pula peringatan penimpaan pada {@link #getSekolah()}.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau instance tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah==null||sekolah.getId()==null?null:sekolah;
	}

	/**
	 * Mengembalikan yayasan pemilik catatan (kolom cakupan multi-tenant tingkat atas).
	 *
	 * <p><b>Bukan getter murni.</b> Method ini memanggil {@link #getSekolah()} lebih dulu
	 * (sehingga seluruh efek samping getter tersebut ikut terjadi), lalu bila sekolahnya ada
	 * <b>mengganti</b> field {@code yayasan} dengan {@code sekolah.getYayasan()}. Yayasan
	 * karenanya selalu mengekor sekolah, dan ikut berpindah secara retroaktif ketika guru
	 * dipindahkan antaryayasan.</p>
	 *
	 * @return yayasan pemilik catatan, atau {@code null} bila tidak dapat ditentukan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		sekolah = getSekolah();
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menetapkan yayasan pemilik catatan.
	 *
	 * <p><b>Perhatian:</b> sama seperti {@link #setSekolah(Sekolah)}, instance tanpa id disimpan
	 * sebagai {@code null}.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau instance tanpa id akan disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan==null||yayasan.getId()==null?null:yayasan;
	}

	/**
	 * Mengembalikan nilai parameter tambahan versi <b>berkunci id</b>.
	 *
	 * <p>Format per baris: <code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;urlLampiran</code>,
	 * antarbaris dipisah {@code "\n"}. Versi inilah yang dibaca ulang saat memuat form
	 * ({@code ParameterTambahanCatatanGuruListener}) dan saat merender kolom parameter pada grid
	 * ({@code CatatanGuruAction.CatatanGuruRenderer}), karena kuncinya stabil terhadap perubahan
	 * label.</p>
	 * <p><b>Bukan getter murni:</b> bila field masih {@code null}, ia menuliskan string kosong ke
	 * field tersebut. Efeknya kecil (hanya mengganti {@code null} dengan {@code ""}), tetapi tetap
	 * berarti membaca baris dapat menandainya kotor bagi Hibernate.</p>
	 *
	 * @return string terserialisasi berkunci id; string kosong bila belum ada nilai, tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Menetapkan nilai parameter tambahan versi berkunci id.
	 *
	 * <p>Dipanggil {@link #populateParameterTambahan(List)}; jangan menyusun string ini secara
	 * manual di kode layar agar formatnya tetap satu sumber.</p>
	 *
	 * @param parameterTambahanInds string terserialisasi berkunci id
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} (versi siap-baca manusia) menjadi daftar
	 * {@link CommonVO} yang sudah terurut menurut nomor urut parameter.
	 *
	 * <p>Pemetaan tiap baris <code>lbl&lt;=&gt;nilai&lt;=&gt;url&lt;=&gt;nomorUrut&lt;=&gt;idParameter</code>
	 * ke {@link CommonVO} adalah: {@code id} &larr; ruas ke-5 (id parameter),
	 * {@code name} &larr; label lengkap {@code "namaKelompok->labelInputan"},
	 * {@code name1} &larr; nilai, {@code name2} &larr; URL lampiran,
	 * {@code name5} &larr; ruas pertama label sebelum {@code "->"} (nama kelompok), dan
	 * {@code nomorUrut} &larr; ruas ke-4. Pengurutan akhir memakai
	 * {@link Collections#sort(List)} atas {@link CommonVO}.</p>
	 *
	 * <p><b>Ketahanan:</b> ruas yang tidak ada diperlakukan sebagai string kosong, dan kegagalan
	 * mengurai {@code nomorUrut}/{@code id} ditangkap lalu dicatat
	 * {@code ais.common.ErrorAuditUtil} dengan nilai bawaan {@code 1}/{@code 1L} &mdash; jadi
	 * method ini tidak melempar untuk data yang cacat sebagian.</p>
	 *
	 * <p><b>Kuirk:</b> method ini <b>tidak pernah</b> mengembalikan daftar kosong. Untuk catatan
	 * tanpa parameter tambahan, {@code "".split("\n")} menghasilkan array berisi satu string
	 * kosong, sehingga hasilnya selalu memuat minimal satu {@link CommonVO} dengan seluruh ruas
	 * kosong. Pemanggil yang memakai {@code isEmpty()} sebagai penanda &quot;tidak ada
	 * parameter&quot; akan keliru.</p>
	 *
	 * <p><b>Status pemakaian:</b> tidak ada pemanggil di repositori untuk kelas ini &mdash;
	 * <b>kode mati</b>. Method senama pada entity lain ({@code KegiatanSiswa},
	 * {@code IsiAngketParameterUmum}) memang dipakai dasbor rekap, tetapi versi Catatan Guru
	 * belum pernah disambungkan ke dasbor mana pun. Layar dan laporan Catatan Guru mengurai
	 * sendiri {@link #getParameterTambahanInds()} secara inline.</p>
	 *
	 * @return daftar {@link CommonVO} hasil penguraian, terurut menurut {@code nomorUrut};
	 *         tidak pernah kosong (lihat kuirk di atas)
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanGuru.java:259");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/CatatanGuru.java:265");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " + val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Mengumpulkan nilai seluruh baris field kustom yang sedang ditampilkan di formulir, lalu
	 * menuliskannya ke DUA kolom terserialisasi milik baris ini.
	 *
	 * <p><b>Kapan dipanggil.</b> Hanya dari
	 * {@code ais.action.master.sekolah.helper.ParameterTambahanCatatanGuruListener#onSave(CatatanGuru)},
	 * yang dipanggil {@code CatatanGuruAction.onSave()} tepat sebelum
	 * {@code Common.refreshSaveOrUpdate(...)}. Validasi field wajib dan lampiran wajib sudah
	 * dilakukan lebih dulu oleh {@code validate()} pada listener yang sama.</p>
	 *
	 * <p><b>Cara kerja.</b> Setiap {@link Row} yang dikirim listener membawa dua atribut ZK:
	 * {@code "parameterTambahan"} ({@link ParameterTambahan}, definisi field) dan
	 * {@code "kelompokParameterTambahanCatatanGuru"}
	 * ({@link KelompokParameterTambahanCatatanGuru}, kelompok/heading). Untuk tiap baris yang
	 * kedua atributnya lengkap, method:</p>
	 * <ol>
	 *   <li>menyusun kunci {@code jenis = "{idKelompok}-&gt;{idParameter}"};</li>
	 *   <li>mengambil nilai isian lewat {@code ParameterTambahan.ambilVal(row, parameterTambahan)}
	 *       &mdash; satu titik yang menangani semua tipe komponen input;</li>
	 *   <li>bila parameter tersebut mewajibkan lampiran
	 *       ({@code getHarusMenyertakanLampiran()}), mencari {@link LampiranLain} lewat
	 *       {@code LampiranLain.ambil(getId(), jenis)} dan menyusun URL unduhnya dengan
	 *       {@code createLinkUri()};</li>
	 *   <li>menambahkan satu baris ke versi siap-baca manusia
	 *       (<code>namaKelompok-&gt;label&lt;=&gt;nilai&lt;=&gt;url&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok</code>)
	 *       dan satu baris ke versi berkunci id
	 *       (<code>idKelompok-&gt;idParameter&lt;=&gt;nilai&lt;=&gt;url</code>).</li>
	 * </ol>
	 *
	 * <p><b>Efek samping.</b> Memanggil {@link #setParameterTambahanInds(String)} dan
	 * {@link #setParameterTambahan(String)}; keduanya <b>menimpa total</b>, bukan menggabung.
	 * Memanggil method ini dengan daftar baris yang tidak lengkap akan menghapus nilai field
	 * kustom yang tidak terwakili di daftar tersebut. Tidak menyentuh basis data secara langsung
	 * &mdash; penyimpanan dilakukan pemanggil.</p>
	 *
	 * <p><b>Ketahanan &amp; urutan.</b> Daftar {@code null} atau kosong menyebabkan method langsung
	 * {@code return} tanpa mengubah apa pun (nilai lama dipertahankan). Kegagalan per baris
	 * ditangkap dan ditampilkan lewat {@code Common.tampilErrorJikaAdmin(e)}, sehingga satu baris
	 * bermasalah tidak membatalkan baris lain &mdash; namun nilainya diam-diam hilang dari hasil.
	 * Urutan baris mengikuti urutan {@code parameterRows} apa adanya (tidak diurutkan ulang).</p>
	 *
	 * <p><b>Catatan lampiran.</b> Pencarian lampiran memakai {@link #getId()}; pada catatan yang
	 * BARU (id masih {@code null}) URL lampiran otomatis kosong. Karena itu
	 * {@code CatatanGuruAction.onSave()} menautkan ulang {@code LampiranLain.setRef(id)} setelah
	 * entity tersimpan &mdash; kolom URL baru terisi pada penyuntingan berikutnya.</p>
	 *
	 * @param parameterRows daftar baris komponen formulir yang dibangun
	 *                      {@code ParameterTambahanCatatanGuruListener}; {@code null}/kosong
	 *                      berarti tidak melakukan apa-apa
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanCatatanGuru kelompokParameterTambahanCatatanGuru = (KelompokParameterTambahanCatatanGuru) row
						.getAttribute("kelompokParameterTambahanCatatanGuru");
				if (parameterTambahan != null && kelompokParameterTambahanCatatanGuru != null) {
					String jenis = kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId();

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);

					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanCatatanGuru.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanCatatanGuru.getId();

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanCatatanGuru.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url;
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Mengembalikan nilai parameter tambahan versi <b>siap-baca manusia</b>.
	 *
	 * <p>Format per baris:
	 * <code>namaKelompok-&gt;labelInputan&lt;=&gt;nilai&lt;=&gt;urlLampiran&lt;=&gt;nomorUrut&lt;=&gt;idParameter&lt;=&gt;idKelompok</code>,
	 * antarbaris dipisah {@code "\n"}. Karena label dan nama kelompok disalin apa adanya saat
	 * simpan, mengganti label sebuah {@link ParameterTambahan} <b>tidak</b> memperbarui catatan
	 * lama &mdash; nilai historis inilah yang dipakai dasbor dan laporan.</p>
	 * <p><b>Bukan getter murni:</b> bila field masih {@code null}, ia menuliskan string kosong ke
	 * field tersebut (perilaku sama dengan {@link #getParameterTambahanInds()}).</p>
	 *
	 * @return string terserialisasi siap-baca; string kosong bila belum ada nilai, tidak pernah
	 *         {@code null}
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Menetapkan nilai parameter tambahan versi siap-baca manusia.
	 *
	 * <p>Dipanggil {@link #populateParameterTambahan(List)}; jangan menyusun string ini secara
	 * manual di kode layar agar formatnya tetap satu sumber.</p>
	 *
	 * @param parameterTambahan string terserialisasi siap-baca
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	
}
