package ais.database.model.sekolah;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Perkuliahan;

/**
 * Entity <b>transaksi prestasi guru</b> — tabel <code>sekolah.prestasi_guru</code>.
 *
 * <p><b>Domain TERVERIFIKASI dari kode, bukan dugaan.</b> Satu baris kelas ini adalah satu catatan
 * pencapaian/kejuaraan milik seorang guru: nama prestasi, penyelenggara, tempat, rentang tanggal,
 * juara &amp; peringkat, jumlah peserta, capaian, nomor sertifikat, tautan bukti, plus status alur
 * persetujuan. Layar yang memakainya berjudul "Manajemen Prestasi Guru" dengan sub-judul "Kelola
 * data pencapaian dan prestasi guru pendidik"
 * (<code>/WEB-INF/baru/modul/prestasi/_prestasi_guru.jsp</code>), dan dasbor pendampingnya berjudul
 * "Statistik &amp; Rekapitulasi Prestasi Guru"
 * (<code>_dashboard_prestasi_guru.jsp</code>). Jadi ini murni data personalia guru, bukan data
 * siswa dan bukan katalog master.</p>
 *
 * <p><b>Posisi dalam keluarga entity.</b> Kelas ini adalah sisi TRANSAKSI; dua entity master
 * mendampinginya lewat dua FK terpisah yang menjawab dua pertanyaan berbeda:</p>
 * <ul>
 * <li>{@link ais.database.model.sekolah.KategoriPrestasiGuru} (kolom
 *     <code>kategori_prestasi_guru</code>) — <i>seberapa tinggi tingkat kejuaraannya</i>:
 *     Internasional / Nasional / Regional / Kab-Kota / Kecamatan / Kampus-Sekolah / Lain-Lain.</li>
 * <li>{@link ais.database.model.sekolah.CabangPrestasiGuru} (kolom
 *     <code>cabang_prestasi_guru</code>) — <i>di bidang apa</i>: Seni, Olah Raga, Kejuaraan Ilmiah,
 *     dan seterusnya.</li>
 * </ul>
 * <p>Keduanya <b>opsional</b> ({@code nullable = true}) dan tidak punya koleksi balik ke kelas ini
 * — relasinya satu arah dari sisi transaksi. Perlu diingat (didokumentasikan pada kedua kelas
 * master itu): tidak ada satu pun kode Java di repositori yang menyemai atau menyunting isi kedua
 * tabel master tersebut, sehingga pada instalasi baru kombo "Pilih Kategori"/"Pilih Cabang" pada
 * formulir prestasi guru selalu kosong dan kedua FK di sini praktis selalu {@code null}.</p>
 *
 * <p><b>Dimensi lingkup: TIGA FK sekaligus.</b> Selain {@link #getGuru()} (wajib,
 * {@code nullable = false}), baris ini juga membawa {@link #getYayasan()} dan
 * {@link #getSekolah()} yang keduanya opsional dan <b>diisi dari pilihan pengguna di formulir</b>,
 * bukan diturunkan otomatis dari guru pemiliknya. Tidak ada kode yang menyinkronkan
 * {@code sekolah}/{@code yayasan} di sini dengan sekolah/yayasan sesungguhnya milik
 * {@link ais.database.model.sekolah.Guru} yang bersangkutan; ketiganya dapat saling bertentangan
 * tanpa validasi apa pun.</p>
 *
 * <p><b>Alur status.</b> Empat konstanta {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
 * {@link #DISETUJUI}, {@link #DITOLAK} adalah <i>seluruh</i> kosakata kolom <code>status</code>.
 * Nilainya disimpan sebagai teks apa adanya (bukan enum, bukan FK), dibaca oleh
 * {@code ais.action.master.dashboard.admin.DasboardGuru} lewat konstanta ini, dan oleh kedua JSP
 * lewat literal string yang diketik ulang. Status {@code Disetujui} punya arti operasional: pada
 * layar JSP formulir dikunci menjadi hanya-baca dan tombol hapus disembunyikan untuk pemilik
 * data.</p>
 *
 * <p><b>Pengelompokan anggota.</b></p>
 * <ul>
 * <li><i>Konstanta status</i> — {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES},
 *     {@link #DISETUJUI}, {@link #DITOLAK}.</li>
 * <li><i>Identitas &amp; kunci</i> — {@link #getId()}/{@link #setId(Long)},
 *     {@link #serialVersionUID}, konstruktor {@link #PrestasiGuru()}, {@link #toString()}.</li>
 * <li><i>Jejak audit warisan</i> — {@link #getOleh()}/{@link #setOleh(String)},
 *     {@link #getOlehId()}/{@link #setOlehId(String)},
 *     {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, callback
 *     {@link #onUpdate()}.</li>
 * <li><i>Identitas prestasi</i> — {@link #getNama()}/{@link #setNama(String)},
 *     {@link #getNamaEn()}/{@link #setNamaEn(String)},
 *     {@link #getKeterangan()}/{@link #setKeterangan(String)}.</li>
 * <li><i>Penyelenggaraan</i> — {@link #getTempat()}/{@link #setTempat(String)},
 *     {@link #getPenyelenggara()}/{@link #setPenyelenggara(String)},
 *     {@link #getJumlahPeserta()}/{@link #setJumlahPeserta(String)},
 *     {@link #getPrestasiLuarKampus()}/{@link #setPrestasiLuarKampus(Boolean)}.</li>
 * <li><i>Hasil yang diraih</i> — {@link #getJuara()}/{@link #setJuara(String)},
 *     {@link #getPeringkat()}/{@link #setPeringkat(Integer)},
 *     {@link #getCapaian()}/{@link #setCapaian(String)}.</li>
 * <li><i>Waktu</i> — {@link #getTanggal()}/{@link #setTanggal(Date)},
 *     {@link #getTanggalSelesai()}/{@link #setTanggalSelesai(Date)},
 *     {@link #getTahun()}/{@link #setTahun(Integer)},
 *     {@link #getTahunAkademik()}/{@link #setTahunAkademik(String)},
 *     {@link #getJenisSemester()}/{@link #setJenisSemester(String)}.</li>
 * <li><i>Bukti &amp; alur persetujuan</i> —
 *     {@link #getNomorSertifikat()}/{@link #setNomorSertifikat(String)},
 *     {@link #getUrl()}/{@link #setUrl(String)}, {@link #getStatus()}/{@link #setStatus(String)}.</li>
 * <li><i>Relasi</i> — {@link #getGuru()}/{@link #setGuru(Guru)},
 *     {@link #getYayasan()}/{@link #setYayasan(Yayasan)},
 *     {@link #getSekolah()}/{@link #setSekolah(Sekolah)},
 *     {@link #getKategoriPrestasiGuru()}/{@link #setKategoriPrestasiGuru(KategoriPrestasiGuru)},
 *     {@link #getCabangPrestasiGuru()}/{@link #setCabangPrestasiGuru(CabangPrestasiGuru)}.</li>
 * </ul>
 *
 * <p><b>Hal non-obvious yang WAJIB diketahui sebelum menyunting kelas ini.</b></p>
 * <ol>
 * <li><b>TIGA getter DESTRUKTIF (menulis balik ke field).</b> Pemetaan kelas ini berbasis
 *     <i>property access</i> (anotasi {@code @Id} ada di getter), sehingga Hibernate memanggil
 *     getter untuk membentuk <i>snapshot</i> maupun untuk memeriksa perubahan saat flush. Akibatnya
 *     nilai yang ditimpa di dalam getter ikut TERTULIS ke basis data:
 *     <ul>
 *     <li>{@link #getTahun()} menghitung ulang {@code tahun} dari {@code tahunAkademik} — dapat
 *         menghapus tahun yang diketik pengguna secara manual;</li>
 *     <li>{@link #getTahunAkademik()} mengisi tahun ajaran BERJALAN bila kolomnya kosong — dapat
 *         "memindahkan" prestasi lama ke tahun ajaran sekarang;</li>
 *     <li>{@link #getJenisSemester()} mengisi semester BERJALAN bila kolomnya kosong.</li>
 *     </ul>
 *     Rinciannya, beserta rantai efeknya, ada pada Javadoc masing-masing method.</li>
 * <li><b>Getter yang menormalkan tanpa menulis balik</b> — {@link #getStatus()},
 *     {@link #getPeringkat()}, {@link #getPrestasiLuarKampus()}, {@link #getJumlahPeserta()},
 *     {@link #getCapaian()}, {@link #getUrl()}, {@link #getNama()}. Semuanya mengganti
 *     {@code null} dengan nilai bawaan <i>hanya pada nilai yang dikembalikan</i>. Ini terlihat
 *     tidak berbahaya, tetapi menimbulkan <b>selisih angka yang nyata</b> antara tampilan Java dan
 *     rekap SQL — lihat butir 3.</li>
 * <li><b>Java dan SQL tidak sepakat soal nilai bawaan.</b> Dua contoh terverifikasi:
 *     <ul>
 *     <li>{@link #getStatus()} menampilkan baris ber-{@code status} NULL sebagai "Belum diproses",
 *         tetapi kartu ringkasan dasbor menghitungnya dengan
 *         {@code SUM(CASE WHEN status = 'Belum diproses' ...)} sehingga baris NULL masuk ke
 *         {@code COUNT(*)} tetapi tidak masuk ke keempat kartu status — jumlah kartu tidak pernah
 *         sama dengan totalnya.</li>
 *     <li>{@link #getPrestasiLuarKampus()} menganggap NULL sebagai {@code true} (kotak centang
 *         tampil tercentang), sedangkan
 *         {@code DasboardGuru.countEntityByGuru(..., "prestasiLuarKampus", Boolean.TRUE)}
 *         menyaring dengan {@code = true} yang TIDAK cocok dengan NULL — angka "prestasi luar
 *         kampus" pada dasbor selalu lebih kecil daripada yang terlihat di daftar.</li>
 *     </ul></li>
 * <li><b>Warisan {@link ais.database.model.GeneralValueObject} bukan {@code @MappedSuperclass}.</b>
 *     Kelas induk adalah POJO abstrak biasa sehingga Hibernate TIDAK memetakan propertinya. Karena
 *     itu {@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} sengaja
 *     DIDEKLARASIKAN ULANG di sini — keharusan teknis pemetaan, bukan duplikasi yang bisa
 *     dibersihkan. Jangan menghapusnya.</li>
 * <li><b>{@code @Audited} (Hibernate Envers) aktif.</b> Setiap penulisan — termasuk penulisan tak
 *     sengaja akibat butir 1 — menghasilkan satu revisi baru di tabel audit.</li>
 * <li><b>Komentar generator "Bank generated by hbm2java" pada versi lama adalah salah salin.</b>
 *     Kelas ini tidak ada hubungannya dengan entity Bank; string yang sama tersalin ke belasan
 *     berkas lain di repositori. Komentar itu digantikan Javadoc ini.</li>
 * <li><b>Dua field yang dikirim formulir TIDAK ADA di kelas ini.</b> Formulir
 *     <code>_prestasi_guru.jsp</code> menyediakan isian "Guru Pembina 2" dan "Guru Pembina 3" dan
 *     mengirimkan {@code guruPembina2}/{@code guruPembina3} pada muatan simpan, dengan komentar
 *     jujur di JSP-nya sendiri: <i>"hanya tersimpan jika di model java telah ditambahkan field
 *     guruPembina2/guruPembina3"</i>. Field itu tidak pernah ditambahkan, sehingga kedua isian itu
 *     selalu hilang diam-diam setelah disimpan dan selalu tampil kosong ketika baris dibuka
 *     kembali.</li>
 * </ol>
 *
 * <p><b>Siapa yang membaca/menulis baris ini.</b></p>
 * <ul>
 * <li><code>/WEB-INF/baru/modul/prestasi/_prestasi_guru.jsp</code> — layar CRUD utama. Seluruh
 *     lalu lintasnya lewat endpoint reflektif generik <code>/Data</code>
 *     ({@code action=daftar} untuk daftar, {@code action=load} untuk membuka satu baris,
 *     {@code action=simpanDataRinci} untuk simpan, {@code action=hapusDataRinci} untuk hapus),
 *     ditambah <code>/DoUpload</code> untuk berkas bukti. Tidak ada Action ZK maupun servlet khusus
 *     untuk entity ini.</li>
 * <li><code>_dashboard_prestasi_guru.jsp</code> — delapan rekap yang seluruhnya berupa string SQL
 *     yang DISUSUN DI PERAMBAN lalu dikirim ke <code>/Data</code> {@code action=sql}.</li>
 * <li><code>_tab_guru.jsp</code> menggabungkan keduanya menjadi dua tab, dan
 *     <code>prestasi.jsp</code> menyisipkan tab itu — untuk akun admin sebagai salah satu dari lima
 *     tab (Mahasiswa/Siswa/Dosen/Guru/Pegawai), dan untuk akun guru sebagai satu-satunya isi
 *     halaman.</li>
 * <li>{@code ais.action.master.prestasi.DasbordPrestasi.muatGuru(...)} — dasbor prestasi terpadu
 *     berbasis ZK; membaca maksimal 800 baris terbaru.</li>
 * <li>{@code ais.action.master.dashboard.admin.DasboardGuru} — enam pencacah, satu tabel "top
 *     status", satu tabel "top kategori", satu "top guru", dan satu tren tahunan.</li>
 * <li>{@code ais.action.master.helper.profile.ProfileUiHelper} — satu kartu angka "Prestasi Guru"
 *     pada ringkasan profil sekolah.</li>
 * </ul>
 *
 * <p><b>PERINGATAN KEAMANAN — fail-open cakupan data personalia guru (TERKONFIRMASI ULANG dari
 * sisi entity ini).</b> Tabel inilah yang dibaca dan ditulis oleh kedua JSP yang lingkup datanya
 * dimatikan. Verifikasi ulang atas kode saat ini:</p>
 * <ol>
 * <li>Pada <code>_prestasi_guru.jsp</code> dan <code>_dashboard_prestasi_guru.jsp</code>, ketiga
 *     variabel penentu lingkup di-hardcode {@code null} dengan panggilan aslinya DIKOMENTARI:
 *     {@code Yayasan loginSebagaiYayasan = null; // tbmuser.getYayasan();},
 *     {@code Sekolah loginSebagaiSekolah = null; // tbmuser.getSekolah();},
 *     {@code Guru loginSebagaiGuru = null; // tbmuser.getGuru();}. Akibatnya
 *     {@code idGuruLogin}/{@code idSekolahLogin}/{@code idYayasanLogin} selalu bernilai
 *     {@code null} dan seluruh cabang penyaring tidak pernah dieksekusi.</li>
 * <li><b>Bukan keterbatasan API.</b> Ketiga method itu ADA dan berfungsi:
 *     {@code Tbmuser.getYayasan()}, {@code Tbmuser.getSekolah()}, dan {@code Tbmuser.getGuru()}
 *     seluruhnya terdefinisi di {@code ais.database.model.Tbmuser}. Lebih jauh,
 *     {@code DasbordPrestasi} (ZK) yang membaca entity yang SAMA memanggil
 *     {@code user.ambilGuru()} dengan sukses dan menyaring
 *     {@code Restrictions.eq("guru", gur)} dengan benar untuk akun guru. Jadi regresi ini khusus
 *     pada kedua berkas JSP, bukan pada model data.</li>
 * <li><b>Akun guru biasa memang diarahkan ke layar ini.</b> Berkas induk
 *     <code>prestasi.jsp</code> memanggil {@code tbmuser.ambilGuru()} dan, bila hasilnya bukan
 *     {@code null}, menyisipkan <code>_tab_guru.jsp</code> sebagai satu-satunya isi halaman. Jadi
 *     dampaknya bukan teoretis: seorang guru melihat daftar dan dasbor prestasi SELURUH guru di
 *     semua sekolah/yayasan dalam satu instalasi, dapat mengunduhnya sebagai berkas Excel
 *     ("Data_Prestasi_Guru_*.xlsx"), dan — karena {@code idGuruLogin} {@code null} — memperoleh
 *     pula pemilih Yayasan/Sekolah pada formulir serta dropdown "Status Persetujuan" yang
 *     seharusnya hanya milik pengelola, sehingga dapat MENYETUJUI prestasinya sendiri. Pembatas
 *     "tidak boleh menghapus prestasi berstatus Disetujui" juga ikut mati karena berada di cabang
 *     {@code idGuruLogin != null} yang tak pernah tercapai.</li>
 * <li><b>Membuka komentar saja tidak cukup.</b> Penyaring di kedua berkas menyebut kolom
 *     {@code guru_id}/{@code sekolah_id}/{@code yayasan_id}, padahal kolom fisik entity ini
 *     bernama {@code guru}/{@code sekolah}/{@code yayasan} (lihat {@code @JoinColumn} pada
 *     {@link #getGuru()}, {@link #getSekolah()}, {@link #getYayasan()}). Ironisnya berkas dasbor
 *     yang sama memakai nama yang BENAR pada klausa {@code JOIN}-nya
 *     ({@code JOIN sekolah.guru d ON p.guru = d.id}). Menghidupkan kembali ketiga baris itu apa
 *     adanya hanya akan menghasilkan galat SQL "column does not exist"; perbaikan harus menyentuh
 *     nama kolom sekaligus, dan idealnya dipindah ke sisi server. Cacat serupa berlaku pada
 *     penyaring kategori/cabang yang menyebut {@code cabang_prestasi_guru_id}/{@code
 *     kategori_prestasi_guru_id}.</li>
 * </ol>
 *
 * <p><b>PERINGATAN KEAMANAN — IDOR baca/tulis/hapus lewat endpoint generik.</b> Modul prestasi
 * guru ini tidak memakai parameter URL sama sekali (tidak ada satu pun {@code request.getParameter}
 * pada kedua JSP), sehingga pola {@code ?guru=}/{@code ?prestasi=} ala
 * {@link ais.database.model.sekolah.PrestasiSiswa} <b>tidak berlaku secara harfiah</b> di sini.
 * Efeknya tetap setara — bahkan lebih luas — karena dipindahkan ke badan JSON endpoint
 * <code>/Data</code>:</p>
 * <ul>
 * <li>{@code ais.action.servlet.Data} hanya memeriksa <i>apakah pengguna sudah masuk</i>. Tidak ada
 *     pemeriksaan hak per-menu, tidak ada pemeriksaan kepemilikan baris, dan tidak ada filter
 *     tenant sisi server untuk {@code action=load}, {@code daftar}, {@code simpanDataRinci},
 *     maupun {@code hapusDataRinci}. Gerbang CRUD pada
 *     {@code ElearningApiUtil.prosesSimpan(...)} hanya berlaku bagi dua kelas master e-Kantin —
 *     entity ini tidak termasuk.</li>
 * <li>Konsekuensinya: pengguna terautentikasi mana pun (termasuk akun siswa dan orang tua) dapat
 *     mengirim {@code {"action":"load","class":"ais.database.model.sekolah.PrestasiGuru","id":N}}
 *     untuk membaca baris mana pun, {@code action=simpanDataRinci} untuk menyunting atau membuat
 *     baris atas nama guru mana pun (nilai {@code guru} diambil apa adanya dari klien) termasuk
 *     menyetel {@code status} menjadi {@code Disetujui}, dan {@code action=hapusDataRinci} untuk
 *     menghapusnya.</li>
 * <li>Penyaring pada layar bukan kontrol keamanan: klausa {@code where1} disusun di JavaScript
 *     peramban lalu diteruskan apa adanya ke {@code Restrictions.sqlRestriction(...)} oleh
 *     {@code ais.action.servlet.api.DaftarDataService}.</li>
 * <li>Dasbor memperbesar dampaknya lagi: {@code action=sql} menerima string SQL utuh dari peramban,
 *     dan {@code ais.common.SqlSecurityGuard} bawaannya bermode mati.</li>
 * <li>Unggahan bukti pada layar ini mengirim {@code tanpaLogin=true} ke <code>/DoUpload</code>
 *     ({@code formData.append('tanpaLogin', 'true')}). Pada {@code DoUpload}, penanda itu
 *     melewatkan pemeriksaan pengguna, sehingga lampiran dapat ditempelkan ke id
 *     {@code PrestasiGuru} mana pun tanpa sesi sama sekali.</li>
 * <li>{@code DasbordPrestasi} tidak memanggil {@code CommonPrivilages.checkPrevilages} sama sekali
 *     dan tidak menyaring sekolah/yayasan; untuk akun yang bukan guru/siswa/mahasiswa (mis. pegawai
 *     atau orang tua) ia menampilkan sampai 800 baris prestasi guru lintas sekolah.</li>
 * </ul>
 *
 * <p><b>Pewarisan hak lewat menu induk.</b> Gerbang RBAC pada <code>_prestasi_guru.jsp</code>
 * memang ADA ({@code CommonPrivilages.checkPrevilages(CREATE/UPDATE/DELETE, tbmuser)}) — berbeda
 * dari sejumlah layar lain yang gerbangnya dikomentari — tetapi varian tanpa argumen {@code Menu}
 * itu mengambil {@code Common.getCurrentMenu()}, yaitu menu yang TERAKHIR dibuka pengguna pada
 * sesi. Hak Tambah/Ubah/Hapus atas prestasi guru karena itu diwarisi dari menu apa pun yang
 * kebetulan sedang aktif, bukan dari menu Prestasi. Ditambah lagi tidak ada gerbang BACA sama
 * sekali (kedua JSP hanya memeriksa sesi login), dan <code>prestasi.jsp</code> menyatukan lima
 * modul berbeda (Mahasiswa, Siswa, Dosen, Guru, Pegawai) di bawah satu menu — hak baca menu
 * "Prestasi" karena itu sekaligus membuka data personalia guru.</p>
 *
 * @see ais.database.model.GeneralValueObject
 * @see ais.database.model.sekolah.KategoriPrestasiGuru
 * @see ais.database.model.sekolah.CabangPrestasiGuru
 * @see ais.database.model.sekolah.Guru
 * @see ais.database.model.sekolah.PrestasiSiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sekolah", name = "prestasi_guru")
public class PrestasiGuru extends GeneralValueObject {

	/**
	 * Status awal: prestasi sudah dicatat tetapi belum ditinjau siapa pun.
	 *
	 * <p>Juga dipakai {@link #getStatus()} sebagai nilai bawaan ketika kolom <code>status</code>
	 * kosong, dan oleh {@code DasboardGuru} sebagai penyaring pencacah "prestasi belum diproses".
	 * <b>Perhatikan:</b> penyamaan bawaan itu hanya terjadi di sisi Java — rekap SQL dasbor JSP
	 * mencocokkan literal {@code 'Belum diproses'} langsung ke kolom, sehingga baris ber-status
	 * NULL tidak ikut terhitung di sana.</p>
	 */
	public static final String BELUM_DIPROSES = "Belum diproses";
	/**
	 * Status antara: berkas prestasi sedang ditinjau pengelola.
	 *
	 * <p>Hanya dapat dipilih dari dropdown "Status Persetujuan" pada formulir, yang menurut kode
	 * JSP semestinya tampil untuk pengelola saja.</p>
	 */
	public static final String SEDANG_DIPROSES = "Sedang diproses";
	/**
	 * Status akhir positif: prestasi diakui.
	 *
	 * <p>Satu-satunya status yang punya efek perilaku pada layar: bila bernilai ini, formulir
	 * dikunci menjadi hanya-baca, tombol unggah/ganti/hapus bukti disembunyikan, dan tombol hapus
	 * baris ditiadakan bagi pemilik data. Rekap "disetujui" pada seluruh dasbor juga memakai nilai
	 * ini.</p>
	 */
	public static final String DISETUJUI = "Disetujui";
	/**
	 * Status akhir negatif: prestasi ditolak pengelola.
	 *
	 * <p>Tidak mengunci apa pun — baris berstatus ini masih dapat disunting dan dihapus.</p>
	 */
	public static final String DITOLAK = "Ditolak";

	/**
	 * Penanda versi serialisasi Java.
	 *
	 * <p>Nilainya sengaja dipertahankan apa adanya. Nilai yang sama dipakai pula oleh
	 * {@link ais.database.model.sekolah.KategoriPrestasiGuru} dan
	 * {@link ais.database.model.sekolah.KategoriPrestasiSiswa} karena berkas-berkas itu lahir dari
	 * salinan generator yang sama — bukan indikasi hubungan warisan apa pun. Instance entity ikut
	 * terserialisasi saat ZK menyimpan state komponen ke dalam session, sehingga mengubah nilai ini
	 * dapat mematahkan session lama yang masih hidup.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/**
	 * Kunci utama tabel <code>sekolah.prestasi_guru</code>.
	 *
	 * <p>Dideklarasikan ulang karena {@link ais.database.model.GeneralValueObject} bukan
	 * {@code @MappedSuperclass}. Dipetakan lewat {@link #getId()}.</p>
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 *
	 * <p>Diisi otomatis oleh lapisan penyimpanan bersama, bukan oleh formulir pengguna.</p>
	 */
	private String oleh;
	/**
	 * Identitas login (user id) pengguna yang terakhir menyimpan baris ini (jejak audit warisan).
	 */
	private String olehId;

	/**
	 * Mengembalikan identitas login pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return user id penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identitas login pengguna penyimpan terakhir.
	 *
	 * <p><b>Perhatikan penjaga di awal method:</b> nilai {@code null} atau string kosong/spasi
	 * DIABAIKAN diam-diam — nilai lama dipertahankan. Ini kontrak keluarga
	 * {@link ais.database.model.GeneralValueObject}: jejak audit tidak boleh terhapus oleh pemanggil
	 * yang kebetulan tidak punya konteks pengguna (misalnya proses batch atau penyemaian data).</p>
	 *
	 * @param olehId user id penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama tampilan pengguna penyimpan terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan agar jejak
	 * audit yang sudah ada tidak tertimpa nilai hampa.</p>
	 *
	 * @param oleh nama pengguna penyimpan; diabaikan bila {@code null} atau kosong setelah di-trim.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama tampilan pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama penyimpan terakhir, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menyegarkan jejak audit sesaat sebelum baris di-<i>update</i>.
	 *
	 * <p>Mendelegasikan seluruh pekerjaan ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setTanggal_dirubah(Date)} beserta {@link #setOleh(String)}/{@link #setOlehId(String)}
	 * dari konteks pengguna yang sedang aktif. Dipanggil oleh penyedia persistensi, bukan oleh kode
	 * aplikasi.</p>
	 *
	 * <p><b>Efek samping penting:</b> callback ini ikut berjalan pada <i>update</i> yang tidak
	 * disengaja — misalnya update yang dipicu ketiga getter destruktif {@link #getTahun()},
	 * {@link #getTahunAkademik()}, dan {@link #getJenisSemester()} — sehingga stempel waktu dan nama
	 * penyimpan dapat berubah tanpa ada yang benar-benar menyunting baris. Ditambah {@code @Audited},
	 * setiap kejadian seperti itu juga melahirkan satu revisi Envers baru.</p>
	 *
	 * <p>Deklarasi field {@code tanggal_dirubah} sengaja ditulis pada baris yang sama seperti pada
	 * berkas aslinya; posisinya dipertahankan agar diff terhadap berkas kembarannya tetap bersih.
	 * Nilai awalnya diambil dari {@code ais.ui.util.WaktuUtil.getDate()} sehingga instance baru
	 * sudah berstempel waktu bahkan sebelum disimpan.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya dipanggil oleh {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}, bukan
	 * oleh kode layar. Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, method ini
	 * TIDAK menolak {@code null} — memanggilnya dengan {@code null} benar-benar mengosongkan jejak
	 * waktu.</p>
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir; boleh {@code null}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} (tanggal + jam), berbeda dari {@link #getTanggal()} dan
	 * {@link #getTanggalSelesai()} yang hanya menyimpan tanggal.</p>
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} pada instance baru karena
	 *         field-nya diinisialisasi saat deklarasi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris ini dalam bentuk <code>&lt;id&gt;-&lt;nama&gt;</code>.
	 *
	 * <p>Dipakai komponen ZK sebagai label bawaan bila tidak ada <i>renderer</i> khusus, dan muncul
	 * di log. <b>Catatan:</b> method ini membaca FIELD {@code nama} langsung, bukan lewat
	 * {@link #getNama()}, sehingga spasi di awal/akhir TIDAK dipangkas di sini padahal dipangkas
	 * pada nilai yang disimpan ke basis data. Pada baris yang belum tersimpan, {@code id} masih
	 * {@code null} sehingga hasilnya berawalan "null-".</p>
	 *
	 * @return gabungan id dan nama prestasi yang dipisah tanda hubung.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Nama/judul prestasi sebagaimana tertulis pada sertifikat. Wajib diisi
	 * ({@code nullable = false}), bertipe {@code text}. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Terjemahan bahasa Inggris nama prestasi (kolom <code>namaen</code>), opsional. Tidak dibaca
	 * satu pun rekap; hanya disimpan dan ditampilkan kembali di formulir.
	 */
	private String namaEn;
	/**
	 * Lokasi penyelenggaraan lomba/kegiatan, teks bebas.
	 */
	private String tempat;
	/**
	 * Pihak penyelenggara lomba/kegiatan, teks bebas.
	 */
	private String penyelenggara;
	/**
	 * Predikat juara yang diraih, teks bebas (mis. "Juara 1", "Harapan 2", "Medali Perak").
	 *
	 * <p>Terpisah dari {@link #peringkat} yang menyimpan angkanya; tidak ada kode yang menjaga
	 * keduanya tetap konsisten.</p>
	 */
	private String juara;
	/**
	 * Peringkat numerik yang diraih. Lihat {@link #getPeringkat()} untuk perlakuan {@code null}.
	 */
	private Integer peringkat;
	/**
	 * Tanggal mulai/pelaksanaan prestasi, dipetakan sebagai {@code DATE} (tanpa jam).
	 */
	private Date tanggal;
	/**
	 * Tanggal selesai pelaksanaan, dipetakan sebagai {@code DATE}. Tidak ada validasi bahwa nilainya
	 * berada setelah {@link #tanggal}.
	 */
	private Date tanggalSelesai;
	/**
	 * Nomor sertifikat/piagam, teks bebas. Tidak diperiksa keunikannya.
	 */
	private String nomorSertifikat;
	/**
	 * Status alur persetujuan; salah satu dari keempat konstanta status kelas ini. Disimpan sebagai
	 * teks bebas sehingga nilai di luar keempatnya secara teknis tetap dapat masuk lewat
	 * <code>/Data</code>.
	 */
	private String status;
	/**
	 * Catatan bebas, bertipe {@code text}.
	 */
	private String keterangan;
	/**
	 * Guru pemilik prestasi — satu-satunya relasi WAJIB ({@code nullable = false}).
	 */
	private Guru guru;

	/**
	 * Penanda apakah prestasi diraih di luar lingkungan sekolah/kampus.
	 *
	 * <p>Perhatikan ketidaksepakatan nilai bawaan antara {@link #getPrestasiLuarKampus()} (NULL
	 * dianggap {@code true}) dan penyaring SQL dasbor ({@code = true}, yang tidak cocok dengan
	 * NULL).</p>
	 */
	private Boolean prestasiLuarKampus;

	/**
	 * Yayasan yang diklaim sebagai lingkup prestasi ini. Opsional, diisi dari pilihan pengguna dan
	 * tidak disinkronkan dengan yayasan sesungguhnya milik {@link #guru}.
	 */
	private Yayasan yayasan;
	/**
	 * Sekolah yang diklaim sebagai lingkup prestasi ini. Opsional, diisi dari pilihan pengguna dan
	 * tidak disinkronkan dengan sekolah sesungguhnya milik {@link #guru}.
	 */
	private Sekolah sekolah;

	/**
	 * Bidang/cabang prestasi — menjawab "di bidang apa". Opsional; lihat
	 * {@link #getCabangPrestasiGuru()}.
	 */
	private CabangPrestasiGuru cabangPrestasiGuru;
	/**
	 * Tingkat/cakupan kejuaraan — menjawab "seberapa tinggi". Opsional; lihat
	 * {@link #getKategoriPrestasiGuru()}.
	 */
	private KategoriPrestasiGuru kategoriPrestasiGuru;
	/**
	 * Jumlah peserta lomba. Disimpan sebagai <b>String</b>, bukan angka, sehingga isian seperti
	 * "±200" atau "50 tim" dapat masuk dan tidak dapat dijumlahkan.
	 */
	private String jumlahPeserta;
	/**
	 * Uraian capaian/prestasi yang diraih, bertipe {@code text}.
	 */
	private String capaian;
	/**
	 * Tautan bukti daring (berita, unggahan, sertifikat digital), bertipe {@code text}. Tidak
	 * divalidasi formatnya dan ditampilkan apa adanya oleh layar.
	 */
	private String url;
	/**
	 * Tahun kejadian prestasi. <b>Rawan tertimpa</b> oleh {@link #getTahun()}.
	 */
	private Integer tahun;

	/**
	 * Tahun ajaran pencatatan dalam format <code>YYYY/YYYY</code>. <b>Diisi otomatis</b> dengan
	 * tahun ajaran berjalan oleh {@link #getTahunAkademik()} bila kosong.
	 */
	private String tahunAkademik;
	/**
	 * Semester pencatatan; bernilai {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}.
	 * <b>Diisi otomatis</b> dengan semester berjalan oleh {@link #getJenisSemester()} bila kosong.
	 */
	private String jenisSemester;

	/**
	 * Konstruktor tanpa argumen.
	 *
	 * <p>Wajib ada untuk Hibernate (instansiasi lewat refleksi saat memuat baris) dan untuk
	 * endpoint reflektif {@code /Data}, yang membuat instance kosong lalu mengisinya dari muatan
	 * JSON. Tidak melakukan inisialisasi apa pun; nilai bawaan yang ada berasal dari inisialisasi
	 * field ({@code tanggal_dirubah}) dan dari getter yang menormalkan {@code null}.</p>
	 */
	public PrestasiGuru() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Anotasi {@code @Id} berada di getter, sehingga strategi akses seluruh kelas adalah
	 * <i>property access</i> — Hibernate memanggil getter (bukan membaca field) untuk memuat,
	 * membuat snapshot, dan memeriksa perubahan. Inilah sebab getter destruktif di kelas ini bisa
	 * berdampak sampai ke basis data.</p>
	 *
	 * <p>Kolom dipetakan {@code insertable = false} karena nilainya dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY}, sequence PostgreSQL).</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama baris ini.
	 *
	 * <p>Tidak dipanggil kode layar dalam alur normal — id diisi basis data. Namun endpoint
	 * {@code /Data action=simpanDataRinci} memakai id dari muatan klien untuk memilih baris yang
	 * akan disunting, sehingga nilai ini pada praktiknya dapat ditentukan pemanggil.</p>
	 *
	 * @param id kunci utama yang akan dipasang.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/judul prestasi, sudah dipangkas spasi awal-akhir.
	 *
	 * <p>Karena pemetaan berbasis <i>property access</i>, nilai yang dipangkas inilah yang dibaca
	 * Hibernate saat menyimpan — jadi spasi berlebih tidak pernah sampai ke kolom, meskipun field
	 * di memori (dan karenanya {@link #toString()}) masih memuatnya. Pemangkasan ini bukan getter
	 * destruktif: field tidak ditimpa.</p>
	 *
	 * @return nama prestasi tanpa spasi pinggir, atau {@code null} bila memang belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama/judul prestasi.
	 *
	 * <p>Kolomnya {@code nullable = false}; menyimpan baris tanpa nama akan gagal di tingkat basis
	 * data, bukan divalidasi di sini. Layar JSP menandainya sebagai isian wajib, tetapi endpoint
	 * {@code /Data} tidak memaksakannya.</p>
	 *
	 * @param nama judul prestasi sebagaimana pada sertifikat.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas untuk baris ini apa adanya (tanpa normalisasi).
	 *
	 * @return keterangan, atau {@code null} bila belum diisi.
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan catatan bebas untuk baris ini.
	 *
	 * @param keterangan teks catatan; boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan lokasi penyelenggaraan prestasi.
	 *
	 * @return nama tempat, atau {@code null} bila belum diisi.
	 */
	public String getTempat() {
		return tempat;
	}

	/**
	 * Menetapkan lokasi penyelenggaraan prestasi.
	 *
	 * @param tempat nama tempat/kota penyelenggaraan; boleh {@code null}.
	 */
	public void setTempat(String tempat) {
		this.tempat = tempat;
	}

	/**
	 * Mengembalikan nama pihak penyelenggara lomba/kegiatan.
	 *
	 * @return nama penyelenggara, atau {@code null} bila belum diisi.
	 */
	public String getPenyelenggara() {
		return penyelenggara;
	}

	/**
	 * Menetapkan nama pihak penyelenggara lomba/kegiatan.
	 *
	 * @param penyelenggara nama lembaga/panitia penyelenggara; boleh {@code null}.
	 */
	public void setPenyelenggara(String penyelenggara) {
		this.penyelenggara = penyelenggara;
	}

	/**
	 * Mengembalikan predikat juara yang diraih.
	 *
	 * <p>Nilainya teks bebas dan tidak terhubung dengan {@link #getPeringkat()}; keduanya diisi
	 * terpisah pada formulir dan bisa saling bertentangan.</p>
	 *
	 * @return predikat juara, atau {@code null} bila belum diisi.
	 */
	public String getJuara() {
		return juara;
	}

	/**
	 * Menetapkan predikat juara yang diraih.
	 *
	 * @param juara predikat juara sebagaimana tertulis pada sertifikat; boleh {@code null}.
	 */
	public void setJuara(String juara) {
		this.juara = juara;
	}

	/**
	 * Mengembalikan tanggal mulai/pelaksanaan prestasi.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE} sehingga komponen jam tidak disimpan. Kolom ini yang
	 * ditampilkan sebagai kolom "Tanggal" pada daftar prestasi.</p>
	 *
	 * @return tanggal pelaksanaan, atau {@code null} bila belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	/**
	 * Menetapkan tanggal mulai/pelaksanaan prestasi.
	 *
	 * @param tanggal tanggal pelaksanaan; boleh {@code null}.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * Mengembalikan nomor sertifikat/piagam.
	 *
	 * @return nomor sertifikat, atau {@code null} bila belum diisi.
	 */
	public String getNomorSertifikat() {
		return nomorSertifikat;
	}

	/**
	 * Menetapkan nomor sertifikat/piagam.
	 *
	 * <p>Tidak ada pemeriksaan keunikan maupun format, sehingga nomor yang sama dapat dipakai
	 * berkali-kali oleh guru yang berbeda.</p>
	 *
	 * @param nomorSertifikat nomor sertifikat; boleh {@code null}.
	 */
	public void setNomorSertifikat(String nomorSertifikat) {
		this.nomorSertifikat = nomorSertifikat;
	}

	/**
	 * Mengembalikan status alur persetujuan, dengan {@link #BELUM_DIPROSES} sebagai nilai bawaan.
	 *
	 * <p>Bila kolom <code>status</code> {@code null} atau berisi spasi saja, method mengembalikan
	 * {@link #BELUM_DIPROSES}. Normalisasi ini <b>hanya pada nilai kembalian</b>: field tidak
	 * ditimpa, sehingga ini BUKAN getter destruktif dan tidak memicu penulisan.</p>
	 *
	 * <p><b>Konsekuensi yang mudah luput.</b> Karena normalisasi hanya ada di Java, rekap SQL pada
	 * <code>_dashboard_prestasi_guru.jsp</code> — yang mencocokkan
	 * {@code status = 'Belum diproses'} langsung ke kolom — tidak menghitung baris ber-status NULL,
	 * padahal baris yang sama dicacah oleh {@code COUNT(*)} pada kartu "total" dan ditampilkan
	 * sebagai "Belum diproses" pada daftar. Karena itu keempat kartu status pada dasbor tidak
	 * pernah berjumlah sama dengan kartu total selama masih ada baris ber-status kosong.</p>
	 *
	 * @return salah satu dari keempat konstanta status; tidak pernah {@code null}.
	 */
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? BELUM_DIPROSES : status;
	}

	/**
	 * Menetapkan status alur persetujuan.
	 *
	 * <p>Tidak memvalidasi bahwa nilainya termasuk keempat konstanta status. Pada layar JSP nilai
	 * ini berasal dari dropdown "Status Persetujuan" yang menurut kodenya hanya ditampilkan bagi
	 * pengelola — tetapi lihat peringatan keamanan pada Javadoc kelas: cabang penentu itu
	 * bergantung pada {@code idGuruLogin} yang selalu {@code null}, dan endpoint
	 * {@code /Data action=simpanDataRinci} menerima nilai {@code status} dari klien tanpa gerbang
	 * hak sama sekali.</p>
	 *
	 * @param status status baru; idealnya salah satu dari keempat konstanta kelas ini.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Mengembalikan guru pemilik prestasi, dengan resolusi proxy lazy terlebih dahulu.
	 *
	 * <p>Memanggil {@code check(guru)} milik {@link ais.database.model.GeneralValueObject} dan
	 * MENIMPA field dengan hasilnya. Ini pola standar seluruh entity repo — bukan penulisan nilai
	 * baru, melainkan penggantian proxy lazy dengan instance yang benar-benar terinisialisasi
	 * (dari cache, session aktif, atau session baru). Bila keempat sumber gagal, {@code check()}
	 * mengembalikan argumennya apa adanya sehingga field tidak berubah.</p>
	 *
	 * <p>Relasi ini WAJIB ({@code @JoinColumn(name = "guru", nullable = false)}). <b>Perhatikan
	 * nama kolomnya:</b> {@code guru}, bukan {@code guru_id} — inilah sebab penyaring lingkup pada
	 * kedua JSP (yang menyebut {@code guru_id}) tidak akan berfungsi meskipun baris komentarnya
	 * dibuka kembali.</p>
	 *
	 * @return guru pemilik prestasi; secara teori tidak pernah {@code null} pada baris tersimpan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru", nullable = false)
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/**
	 * Menetapkan guru pemilik prestasi.
	 *
	 * <p><b>Titik rawan keamanan.</b> Nilai ini datang langsung dari muatan klien
	 * ({@code dataObj.guru = parseInt(guruVal)} pada <code>_prestasi_guru.jsp</code>) dan tidak
	 * pernah dicocokkan dengan guru yang sedang masuk. Karena {@code /Data} hanya memeriksa status
	 * login, pengguna terautentikasi mana pun dapat mencatatkan atau memindahkan prestasi atas nama
	 * guru mana pun.</p>
	 *
	 * <p>{@code CascadeType.PERSIST}/{@code MERGE} berarti guru yang belum tersimpan ikut
	 * tersimpan; alur normal selalu memasok guru yang sudah ada.</p>
	 *
	 * @param guru guru pemilik prestasi.
	 */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/**
	 * Mengembalikan yayasan lingkup prestasi ini, dengan resolusi proxy lazy terlebih dahulu.
	 *
	 * <p>Sama seperti {@link #getGuru()}, field ditimpa hasil {@code check(...)} untuk mengganti
	 * proxy lazy dengan instance terinisialisasi.</p>
	 *
	 * <p>Nilainya berasal dari pilihan pengguna pada formulir, bukan diturunkan dari
	 * {@link #getGuru()}, dan tidak pernah divalidasi terhadap sekolah/yayasan guru tersebut. Nama
	 * kolomnya {@code yayasan} (bukan {@code yayasan_id}).</p>
	 *
	 * @return yayasan lingkup, atau {@code null} bila tidak dipilih — kondisi yang lazim karena
	 *         pemilihnya hanya tampil untuk pengguna non-guru.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan", nullable = true)
	public Yayasan getYayasan() {
		yayasan = check(yayasan);
		return yayasan;
	}

	/**
	 * Menetapkan yayasan lingkup prestasi ini.
	 *
	 * <p>Diisi dari kombo "Yayasan" pada formulir. Karena kombo itu memuat isinya lewat
	 * {@code /Data action=daftar} atas seluruh {@link ais.database.model.sekolah.Yayasan} aktif —
	 * tanpa penyempitan lingkup, sebab {@code idYayasanLogin} selalu {@code null} — pengguna dapat
	 * memilih yayasan mana pun pada instalasi.</p>
	 *
	 * @param yayasan yayasan lingkup; boleh {@code null}.
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan;
	}

	/**
	 * Mengembalikan sekolah lingkup prestasi ini, dengan resolusi proxy lazy terlebih dahulu.
	 *
	 * <p>Perilaku {@code check(...)} identik dengan {@link #getGuru()} dan {@link #getYayasan()}.
	 * Nama kolomnya {@code sekolah} (bukan {@code sekolah_id}) — lihat catatan pada
	 * {@link #getGuru()} mengenai penyaring lingkup yang salah menyebut nama kolom.</p>
	 *
	 * @return sekolah lingkup, atau {@code null} bila tidak dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah", nullable = true)
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return sekolah;
	}

	/**
	 * Menetapkan sekolah lingkup prestasi ini.
	 *
	 * <p>Diisi dari kombo "Sekolah" pada formulir; sama seperti kombo yayasan, isinya tidak
	 * dipersempit oleh sekolah pengguna karena {@code idSekolahLogin} selalu {@code null}.</p>
	 *
	 * @param sekolah sekolah lingkup; boleh {@code null}.
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah;
	}

	/**
	 * Mengembalikan penanda "prestasi diraih di luar sekolah/kampus", dengan {@code true} sebagai
	 * nilai bawaan.
	 *
	 * <p>Field tidak ditimpa — normalisasi hanya pada nilai kembalian, jadi ini bukan getter
	 * destruktif. Efeknya di layar: kotak centang "Prestasi Luar Kampus" tampil TERCENTANG untuk
	 * baris lama yang kolomnya masih NULL.</p>
	 *
	 * <p><b>Selisih angka yang nyata:</b> {@code DasboardGuru} mencacah dengan
	 * {@code countEntityByGuru(PrestasiGuru.class, ..., "prestasiLuarKampus", Boolean.TRUE)} yang
	 * diterjemahkan menjadi {@code = true} di SQL dan TIDAK mencocokkan NULL. Jadi angka "prestasi
	 * luar kampus" pada dasbor selalu lebih kecil daripada jumlah baris yang tampil tercentang di
	 * layar.</p>
	 *
	 * @return {@code true} bila kolomnya kosong atau bernilai true; {@code false} hanya bila
	 *         eksplisit disetel false.
	 */
	public Boolean getPrestasiLuarKampus() {
		return prestasiLuarKampus == null ? true : prestasiLuarKampus;
	}

	/**
	 * Menetapkan penanda "prestasi diraih di luar sekolah/kampus".
	 *
	 * <p>Formulir selalu mengirim nilai boolean tegas (hasil {@code checkbox.checked}), sehingga
	 * baris yang dibuat lewat layar tidak pernah menyimpan NULL — NULL hanya ada pada baris lama
	 * atau baris yang dibuat langsung lewat SQL/endpoint generik.</p>
	 *
	 * @param prestasiLuarKampus penanda baru; boleh {@code null} (diperlakukan {@code true} saat
	 *        dibaca).
	 */
	public void setPrestasiLuarKampus(Boolean prestasiLuarKampus) {
		this.prestasiLuarKampus = prestasiLuarKampus;
	}

	/**
	 * Mengembalikan tingkat/cakupan kejuaraan prestasi ini.
	 *
	 * <p>Berbeda dari {@link #getGuru()}/{@link #getYayasan()}/{@link #getSekolah()}, method ini
	 * TIDAK memanggil {@code check(...)}; nilainya dikembalikan apa adanya. Karena relasinya
	 * dipetakan {@code @Fetch(FetchMode.SELECT)} tanpa {@code fetch = LAZY} eksplisit — dan
	 * bawaan {@code @ManyToOne} adalah {@code EAGER} — Hibernate sudah memuatnya lewat query
	 * terpisah saat baris dibaca, sehingga resolusi proxy tidak diperlukan.</p>
	 *
	 * <p>Menjawab pertanyaan "seberapa tinggi tingkat kejuaraannya"
	 * ({@link ais.database.model.sekolah.KategoriPrestasiGuru}: Internasional/Nasional/…). Kolomnya
	 * {@code kategori_prestasi_guru} — tanpa akhiran {@code _id}, berbeda dari yang diasumsikan
	 * penyaring di JSP. Pada praktiknya hampir selalu {@code null} karena tabel masternya tidak
	 * pernah disemai kode mana pun.</p>
	 *
	 * @return kategori/tingkat prestasi, atau {@code null} bila tidak dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kategori_prestasi_guru", nullable = true)
	public KategoriPrestasiGuru getKategoriPrestasiGuru() {
		return kategoriPrestasiGuru;
	}

	/**
	 * Menetapkan tingkat/cakupan kejuaraan prestasi ini.
	 *
	 * <p>Karena {@code CascadeType.PERSIST}/{@code MERGE} aktif, memasok instance
	 * {@link ais.database.model.sekolah.KategoriPrestasiGuru} yang belum tersimpan akan MEMBUAT
	 * baris master baru secara diam-diam. Alur layar selalu memasok id yang sudah ada.</p>
	 *
	 * @param kategoriPrestasiGuru kategori/tingkat prestasi; boleh {@code null}.
	 */
	public void setKategoriPrestasiGuru(KategoriPrestasiGuru kategoriPrestasiGuru) {
		this.kategoriPrestasiGuru = kategoriPrestasiGuru;
	}

	/**
	 * Mengembalikan jumlah peserta lomba sebagai teks, dengan string kosong sebagai nilai bawaan.
	 *
	 * <p>Field tidak ditimpa (bukan getter destruktif). Perlu diingat tipenya {@code String}, bukan
	 * angka, sehingga nilai seperti "±200" sah tersimpan dan kolom ini tidak dapat diagregasi.</p>
	 *
	 * @return jumlah peserta sebagai teks; tidak pernah {@code null}, minimal string kosong.
	 */
	public String getJumlahPeserta() {
		return jumlahPeserta == null ? "" : jumlahPeserta;
	}

	/**
	 * Menetapkan jumlah peserta lomba.
	 *
	 * @param jumlahPeserta jumlah peserta sebagai teks bebas; boleh {@code null}.
	 */
	public void setJumlahPeserta(String jumlahPeserta) {
		this.jumlahPeserta = jumlahPeserta;
	}

	/**
	 * Mengembalikan uraian capaian, dengan string kosong sebagai nilai bawaan.
	 *
	 * <p>Field tidak ditimpa. Dibaca antara lain oleh
	 * {@code ais.action.master.prestasi.DasbordPrestasi.muatGuru(...)} untuk kolom "Capaian" pada
	 * dasbor prestasi terpadu.</p>
	 *
	 * @return uraian capaian; tidak pernah {@code null}, minimal string kosong.
	 */
	@Column(columnDefinition = "text")
	public String getCapaian() {
		return capaian == null ? "" : capaian;
	}

	/**
	 * Menetapkan uraian capaian yang diraih.
	 *
	 * @param capaian teks capaian; boleh {@code null}.
	 */
	public void setCapaian(String capaian) {
		this.capaian = capaian;
	}

	/**
	 * Mengembalikan bidang/cabang prestasi ini.
	 *
	 * <p>Seperti {@link #getKategoriPrestasiGuru()}, method ini tidak memanggil {@code check(...)}
	 * karena relasinya dimuat {@code EAGER} lewat {@code @Fetch(FetchMode.SELECT)}.</p>
	 *
	 * <p>Menjawab pertanyaan "di bidang apa"
	 * ({@link ais.database.model.sekolah.CabangPrestasiGuru}: Seni/Olah Raga/Kejuaraan Ilmiah/…).
	 * Kolomnya {@code cabang_prestasi_guru} tanpa akhiran {@code _id}, dan seperti kategori,
	 * praktis selalu {@code null} karena tabel masternya tidak pernah disemai.</p>
	 *
	 * @return cabang prestasi, atau {@code null} bila tidak dipilih.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "cabang_prestasi_guru", nullable = true)
	public CabangPrestasiGuru getCabangPrestasiGuru() {
		return cabangPrestasiGuru;
	}

	/**
	 * Menetapkan bidang/cabang prestasi ini.
	 *
	 * <p>Sama seperti {@link #setKategoriPrestasiGuru(KategoriPrestasiGuru)}, cascade
	 * {@code PERSIST}/{@code MERGE} dapat menciptakan baris master baru bila diberi instance yang
	 * belum tersimpan.</p>
	 *
	 * @param cabangPrestasiGuru cabang prestasi; boleh {@code null}.
	 */
	public void setCabangPrestasiGuru(CabangPrestasiGuru cabangPrestasiGuru) {
		this.cabangPrestasiGuru = cabangPrestasiGuru;
	}

	/**
	 * Mengembalikan tautan bukti daring, dengan string kosong sebagai nilai bawaan.
	 *
	 * <p>Field tidak ditimpa. Nilainya tidak divalidasi sebagai URL dan tidak disaring skemanya;
	 * layar menampilkannya apa adanya. Berbeda dari berkas bukti yang diunggah — berkas itu
	 * disimpan sebagai {@code ais.database.model.file.LampiranLain} yang menunjuk balik ke id
	 * baris ini, bukan di kolom ini.</p>
	 *
	 * @return tautan bukti; tidak pernah {@code null}, minimal string kosong.
	 */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url == null ? "" : url;
	}

	/**
	 * Menetapkan tautan bukti daring.
	 *
	 * @param url alamat bukti daring; boleh {@code null}.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Mengembalikan tanggal selesai pelaksanaan prestasi.
	 *
	 * <p>Dipetakan {@code TemporalType.DATE}. Tidak ada satu pun rekap yang membacanya — kolom
	 * "Tanggal" pada daftar dan seluruh tren dasbor memakai {@link #getTanggal()} atau
	 * {@link #getTahunAkademik()}.</p>
	 *
	 * @return tanggal selesai, atau {@code null} bila kegiatan berlangsung satu hari atau tidak
	 *         diisi.
	 */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSelesai() {
		return tanggalSelesai;
	}

	/**
	 * Menetapkan tanggal selesai pelaksanaan prestasi.
	 *
	 * <p>Tidak ada validasi bahwa nilainya berada setelah {@link #getTanggal()}; rentang terbalik
	 * dapat tersimpan tanpa peringatan.</p>
	 *
	 * @param tanggalSelesai tanggal selesai; boleh {@code null}.
	 */
	public void setTanggalSelesai(Date tanggalSelesai) {
		this.tanggalSelesai = tanggalSelesai;
	}

	/**
	 * Mengembalikan tahun kejadian prestasi — <b>GETTER DESTRUKTIF, membaca sekaligus MENULIS
	 * field.</b>
	 *
	 * <p><b>Apa yang dilakukan.</b> Bila field {@code tahunAkademik} tidak {@code null}, method ini
	 * memecahnya pada karakter <code>/</code> dan MENIMPA field {@code tahun} dengan potongan
	 * pertama yang di-parse sebagai bilangan (mis. {@code "2026/2027"} menjadi {@code 2026}). Baru
	 * setelah itu nilai {@code tahun} dikembalikan.</p>
	 *
	 * <p><b>Mengapa berbahaya.</b> Pemetaan kelas ini berbasis <i>property access</i> dan method ini
	 * tidak diberi {@code @Transient}, sehingga Hibernate memanggilnya saat memeriksa perubahan
	 * pada flush. Nilai hasil timpaan itulah yang dibandingkan dengan snapshot dan, bila berbeda,
	 * DITULIS ke kolom <code>tahun</code>. Formulir prestasi menyediakan isian "Tahun" dan "Tahun
	 * Pelajaran" secara terpisah dan mengirim keduanya; begitu keduanya tidak sejalan — misalnya
	 * lomba tahun 2024 yang dicatat pada tahun pelajaran 2026/2027 — angka yang diketik pengguna
	 * akan tergantikan diam-diam oleh tahun pertama tahun pelajaran.</p>
	 *
	 * <p><b>Rantai efek dengan getter tetangganya.</b> Method ini membaca FIELD {@code tahunAkademik}
	 * langsung, bukan lewat {@link #getTahunAkademik()}. Namun {@link #getTahunAkademik()} sendiri
	 * mengisi field itu dengan tahun ajaran BERJALAN bila kosong. Jadi pada baris lama yang kedua
	 * kolomnya kosong, urutan pemanggilan menentukan hasil: bila {@code getTahunAkademik()}
	 * kebetulan dipanggil lebih dulu (mis. oleh serialisasi JSON {@code Common.insertProperty} atau
	 * oleh renderer grid), pemanggilan {@code getTahun()} berikutnya akan menstempel baris itu
	 * dengan tahun sekarang.</p>
	 *
	 * <p><b>Kegagalan parse ditelan.</b> Bila {@code tahunAkademik} tidak berformat
	 * <code>YYYY/…</code>, {@link NumberFormatException} ditangkap dan hanya dicatat ke
	 * {@code ErrorAuditUtil}; nilai {@code tahun} lama dipertahankan dan tidak ada pesan ke
	 * pengguna.</p>
	 *
	 * @return tahun kejadian prestasi — hasil turunan dari tahun pelajaran bila tahun pelajaran
	 *         terisi, atau nilai kolom apa adanya bila tidak; dapat {@code null}.
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sekolah/PrestasiGuru.java:296");

			}
		}
		return tahun;
	}

	/**
	 * Menetapkan tahun kejadian prestasi.
	 *
	 * <p>Nilai yang dipasang di sini <b>tidak bertahan</b> begitu {@code tahunAkademik} terisi —
	 * {@link #getTahun()} akan menghitung ulang dan menimpanya. Lihat peringatan pada getter
	 * tersebut.</p>
	 *
	 * @param tahun tahun kejadian; boleh {@code null}.
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengembalikan tahun pelajaran pencatatan — <b>GETTER DESTRUKTIF, membaca sekaligus MENULIS
	 * field.</b>
	 *
	 * <p>Bila field {@code tahunAkademik} masih {@code null}, method ini MENGISINYA dengan
	 * {@code Common.getCurrentTahunAkademik()} — tahun pelajaran yang sedang berjalan menurut
	 * konfigurasi instalasi — lalu mengembalikannya. Karena pemetaan berbasis <i>property
	 * access</i>, isian itu ikut tertulis ke kolom pada flush berikutnya.</p>
	 *
	 * <p><b>Dampak nyata.</b> Baris prestasi lama yang kolom tahun pelajarannya kosong akan
	 * "berpindah" ke tahun pelajaran sekarang begitu ia dirender di grid, diserialisasi ke JSON,
	 * atau dibaca dasbor mana pun. Rekap tren pada <code>_dashboard_prestasi_guru.jsp</code>
	 * (dikelompokkan menurut {@code tahunakademik}) serta pencacah
	 * {@code DasboardGuru.countEntityByGuru(..., "tahunAkademik", ...)} karena itu dapat menunjukkan
	 * lonjakan palsu pada tahun berjalan. Efek ini juga memicu {@link #onUpdate()} sehingga jejak
	 * audit dan revisi Envers ikut bertambah tanpa ada penyuntingan sungguhan.</p>
	 *
	 * <p>Perhatikan pula rantainya dengan {@link #getTahun()} yang menurunkan {@code tahun} dari
	 * field yang baru saja diisi method ini.</p>
	 *
	 * @return tahun pelajaran dalam format <code>YYYY/YYYY</code>; tidak pernah {@code null} setelah
	 *         pemanggilan pertama.
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/**
	 * Menetapkan tahun pelajaran pencatatan.
	 *
	 * <p>Menyetel {@code null} di sini tidak akan bertahan: pembacaan berikutnya lewat
	 * {@link #getTahunAkademik()} langsung mengisinya kembali dengan tahun pelajaran berjalan.</p>
	 *
	 * @param tahunAkademik tahun pelajaran, format <code>YYYY/YYYY</code>.
	 */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * Mengembalikan semester pencatatan — <b>GETTER DESTRUKTIF, membaca sekaligus MENULIS
	 * field.</b>
	 *
	 * <p>Bila field {@code jenisSemester} masih {@code null}, method ini MENGISINYA dengan
	 * {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP} sesuai
	 * {@code Common.isNowSemensterGanjil()} — yaitu semester yang sedang berjalan saat baris dibaca,
	 * bukan semester saat prestasi diraih. Seperti dua getter destruktif lainnya, isian itu ikut
	 * tertulis ke kolom pada flush berikutnya.</p>
	 *
	 * <p>Kolom inilah yang dikelompokkan rekap tren dasbor ({@code jenissemester}) dan dipakai
	 * {@code DasboardGuru} sebagai dimensi semester, sehingga penstempelan otomatis ini langsung
	 * memengaruhi angka laporan.</p>
	 *
	 * @return {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}; tidak pernah {@code null}
	 *         setelah pemanggilan pertama.
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/**
	 * Menetapkan semester pencatatan.
	 *
	 * <p>Menyetel {@code null} tidak bertahan — {@link #getJenisSemester()} akan mengisinya kembali
	 * dengan semester berjalan.</p>
	 *
	 * @param jenisSemester {@code Perkuliahan.GANJIL} atau {@code Perkuliahan.GENAP}.
	 */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/**
	 * Mengembalikan peringkat numerik yang diraih, dengan {@code 0} sebagai nilai bawaan.
	 *
	 * <p>Field tidak ditimpa — normalisasi hanya pada nilai kembalian, jadi bukan getter
	 * destruktif. Akibatnya "belum diisi" dan "peringkat 0" tidak dapat dibedakan oleh pemanggil
	 * Java, sementara di basis data keduanya tetap berbeda (NULL vs 0). Dibaca
	 * {@code DasbordPrestasi.muatGuru(...)} untuk kolom peringkat pada dasbor terpadu.</p>
	 *
	 * @return peringkat yang diraih; tidak pernah {@code null}, minimal {@code 0}.
	 */
	public Integer getPeringkat() {
		return peringkat == null ? 0 : peringkat;
	}

	/**
	 * Menetapkan peringkat numerik yang diraih.
	 *
	 * <p>Tidak dijaga konsistensinya dengan {@link #setJuara(String)} yang menyimpan predikat
	 * juara dalam bentuk teks.</p>
	 *
	 * @param peringkat peringkat yang diraih; boleh {@code null}.
	 */
	public void setPeringkat(Integer peringkat) {
		this.peringkat = peringkat;
	}

	/**
	 * Mengembalikan terjemahan bahasa Inggris nama prestasi.
	 *
	 * <p>Dipetakan ke kolom <code>namaen</code> (semua huruf kecil, tanpa garis bawah) — perhatikan
	 * bila menulis SQL manual. Tidak dinormalkan dan tidak dipangkas, berbeda dari
	 * {@link #getNama()}. Tidak dibaca satu pun rekap; hanya disimpan dan ditampilkan kembali di
	 * formulir.</p>
	 *
	 * @return nama prestasi dalam bahasa Inggris, atau {@code null} bila belum diisi.
	 */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/**
	 * Menetapkan terjemahan bahasa Inggris nama prestasi.
	 *
	 * @param namaEn nama prestasi dalam bahasa Inggris; boleh {@code null}.
	 */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}

}
