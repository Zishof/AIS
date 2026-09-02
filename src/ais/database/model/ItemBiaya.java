package ais.database.model;

// Generated Apr 18, 2010 9:34:04 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.TreeMap;

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

import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;

import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.akunting.Akun;

/**
 * Entity master <b>item biaya</b> &mdash; satu baris di tabel {@code public.item_biaya} adalah satu
 * <i>komponen tagihan</i> yang bisa dipakai berulang kali oleh seluruh modul keuangan: SPP, uang
 * gedung, uang praktikum, uang wisuda, denda perpustakaan, potongan/diskon, dan seterusnya.
 *
 * <p>Kelas ini adalah <b>katalog/master</b>, bukan data transaksi. Karena itu perlu ditegaskan sejak
 * awal: <b>{@code ItemBiaya} TIDAK menyimpan nominal sama sekali.</b> Tidak ada field rupiah di sini.
 * Yang disimpan hanyalah <i>identitas</i> item (kode, nama, deskripsi) dan <i>aturan perlakuan</i>
 * item tersebut (cara menghitung pengalinya, boleh dicicil atau tidak, kena denda atau tidak, muncul
 * di surat tagihan atau tidak, akun akunting mana yang dipakai). Nominalnya baru muncul di entity
 * anak.</p>
 *
 * <h2>Posisi dalam rantai penagihan</h2>
 *
 * <p>Urutan hulu ke hilir dari satu komponen tagihan di AIS kira-kira begini:</p>
 *
 * <pre>
 *   ItemBiaya                (master: "apa" yang ditagih + aturannya  &mdash; kelas ini)
 *        &darr;
 *   {@link SettingBiaya} / {@link DetailSettingBiaya}
 *                            (template per tahun akademik; DetailSettingBiaya.defaultBiaya
 *                             barulah nominal defaultnya)
 *        &darr;
 *   {@link DetailBiaya}      (baris tagihan yang sudah dipersempit ke prodi/angkatan/semester/
 *                             gelombang; nilaiBiaya = nominal dasar,
 *                             nilaiBiayaBaru = nominal setelah pengali/penghitungan)
 *        &darr;
 *   {@link DetailKegiatan}   (tagihan nyata milik satu {@link Kegiatan} milik satu mahasiswa)
 *        &darr;
 *   {@link CicilanPembayaran} / {@link BuktiPembayaran} / {@link DendaPembayaran}
 *                            (realisasi pembayaran, angsuran, denda)
 * </pre>
 *
 * <p>Karena itu hampir semua pemakai membaca {@code ItemBiaya} <b>lewat</b> anaknya, dengan pola
 * {@code detailBiaya.getItemBiaya().getXxx()} atau
 * {@code cicilanPembayaran.getItemBiaya().getXxx()}. Nyaris tidak ada kode yang memuat
 * {@code ItemBiaya} sendirian selain layar master {@code ais.action.master.ItemBiayaAction}.</p>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 *
 * <ol>
 *   <li><b>Katalog mode penghitungan</b> &mdash; puluhan konstanta {@code DIKALI_*} /
 *   {@code HITUNG_*} / {@link #TIDAK_ADA_PENGHITUNGAN} plus {@link #PENGHITUNGAN_MAP}. Lihat bagian
 *   "Mode penghitungan" di bawah.</li>
 *   <li><b>Jejak audit</b> &mdash; {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, dan {@link #onUpdate()}.
 *   Pola ini identik di semua entity AIS; lihat {@link GeneralValueObject}.</li>
 *   <li><b>Identitas item</b> &mdash; {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *   {@link #getDeskripsi()}, {@link #toString()}, {@link #getAktif()}.</li>
 *   <li><b>Aturan penghitungan nominal</b> &mdash; {@link #getPenghitungan()},
 *   {@link #getNamaMatakuliah()}, {@link #getNilaiBisaDiubah()},
 *   {@link #getTerhubungKeNilaiTambahan()}/{@link #getParameterTambahan()}.</li>
 *   <li><b>Aturan denda keterlambatan</b> &mdash; {@link #getDendaJikaTerlambat()},
 *   {@link #getDefaultProsentaseDenda()}, {@link #getNilaiDendaDalamPersen()},
 *   {@link #getDendaAkanBerlipatTerlambaHari()}, {@link #getMaksimalBerlipatTerlambaHari()}.</li>
 *   <li><b>Aturan cicilan/angsuran &amp; tampilan</b> &mdash;
 *   {@link #getMahasiswaBolehMencicilkan()}, {@link #getAdminBolehMencicilkan()},
 *   {@link #getMenggunakanIstilahBayarAngsuran()}, {@link #getDitampilkanDiSuratTagihan()}.</li>
 *   <li><b>Batas berlakunya tagihan</b> &mdash; {@link #getMinSmt()}, {@link #getMaxSmt()},
 *   {@link #getTidakDitagihDiSmtGanjil()}, {@link #getTidakDitagihDiSmtGenap()},
 *   {@link #getTanggalTagihanMengikutiRencanaTahunAkademik()}.</li>
 *   <li><b>Resolusi akun akunting</b> &mdash; delapan method {@code ambil*}: {@link #ambilAkun},
 *   {@link #ambilPiutang}, {@link #ambilDibayarDimuka}, {@link #ambilPendapatanDenda}, masing-masing
 *   punya dua bentuk (dari {@link Kegiatan}, dan dari fakultas/jurusan/program/angkatan). Ini
 *   satu-satunya kelompok method di file ini yang menyentuh database.</li>
 * </ol>
 *
 * <h2>Mode penghitungan ({@code penghitungan})</h2>
 *
 * <p>Field {@link #getPenghitungan()} menentukan <b>pengali</b> nominal dasar. Nilainya bukan kode
 * pendek, melainkan <b>kalimat berbahasa Indonesia apa adanya</b> (mis. {@code "Dikali jumlah SKS
 * matakuliah yang diambil"}) yang disimpan mentah di kolom {@code penghitungan}. Konstanta di kelas
 * ini adalah satu-satunya sumber kebenaran teks tersebut.</p>
 *
 * <p><b>Konsekuensi penting: teks konstanta ini load-bearing.</b> Mengubah satu huruf pun pada
 * literalnya akan memutus pencocokan {@code equals(...)} terhadap seluruh baris {@code item_biaya}
 * yang sudah tersimpan di produksi, dan item itu diam-diam berhenti dikalikan (nominal jatuh ke
 * nilai dasar tanpa error). Termasuk salah ejanya &mdash; lihat catatan pada
 * {@link #DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU}.</p>
 *
 * <p>Yang benar-benar menafsirkan nilai ini adalah
 * {@code ais.action.master.helper.PembayaranNominalModifikasiHelper}: sebuah rantai
 * {@code else if} raksasa yang, untuk mode yang cocok, menghitung jumlah SKS/matakuliah/pertemuan
 * milik mahasiswa pada semester bersangkutan, lalu mengisi {@code detailBiaya.nilaiBiayaBaru} dan
 * {@code detailBiaya.keterangan}. Rantai itu ada dua kali: satu untuk tagihan
 * {@link DetailBiaya} biasa, satu lagi untuk tagihan bulanan
 * ({@link PengaturanPembayaranBulanan}).</p>
 *
 * <p>{@link #PENGHITUNGAN_MAP} hanyalah daftar pilihan untuk combobox di layar master. Kuncinya
 * ({@code "505"}, {@code "5051"}, &hellip;) <b>tidak pernah disimpan ke database</b> dan tidak sama
 * dengan {@link #getKode()} item biaya &mdash; kunci itu semata-mata penentu <i>urutan tampil</i>
 * karena {@code TreeMap} mengurutkan kuncinya secara leksikografis. Yang disimpan sebagai
 * {@code value} combobox adalah teks mode itu sendiri.</p>
 *
 * <h2>Resolusi akun akunting</h2>
 *
 * <p>Saat pembayaran diposting menjadi jurnal, mesin posting perlu tahu akun mana yang didebet/
 * dikredit untuk item biaya ini. Pemetaannya tidak disimpan sebagai kolom, melainkan sebagai
 * <b>tabel jembatan</b>, satu tabel per peran akun:</p>
 *
 * <ul>
 *   <li>{@link ItemBiayaPunyaAkun} &rarr; akun <b>pendapatan</b> ({@link #ambilAkun}),</li>
 *   <li>{@link ItemBiayaPunyaPiutang} &rarr; akun <b>piutang</b> ({@link #ambilPiutang}),</li>
 *   <li>{@link ItemBiayaPunyaDibayarDimuka} &rarr; akun <b>pendapatan diterima dimuka</b>
 *   ({@link #ambilDibayarDimuka}),</li>
 *   <li>{@link ItemBiayaPunyaPendapatanDenda} &rarr; akun <b>pendapatan denda</b>
 *   ({@link #ambilPendapatanDenda}),</li>
 *   <li>{@link ItemBiayaPunyaDiskon} &rarr; akun <b>diskon</b> &mdash; <b>tidak ada method
 *   {@code ambilDiskon()} di kelas ini</b>; pemetaannya hanya dikelola dari layar UI dan dibaca
 *   langsung oleh pemanggil lain.</li>
 * </ul>
 *
 * <p>Setiap baris jembatan boleh dipersempit per {@link Fakultas}, {@link Jurusan}, {@code program},
 * dan {@code angkatan}. Keempat method {@code ambil*} memakai <b>algoritma pencarian berjenjang
 * delapan tahap yang identik</b> (hanya kelas jembatannya yang berbeda), dari yang paling spesifik
 * ke yang paling umum. Rinciannya didokumentasikan sekali secara lengkap di
 * {@link #ambilAkun(Fakultas, Jurusan, String, String)}; tiga method lain merujuk ke sana.</p>
 *
 * <h2>Interaksi dengan {@link JenisKegiatan} pada perhitungan denda</h2>
 *
 * <p>Lima properti denda di kelas ini punya kembaran bernama sama persis di {@link JenisKegiatan}.
 * Yang memutuskan mana yang dipakai adalah {@link DetailBiaya#checkDenda}, dan aturannya
 * <b>bukan</b> "item biaya menang": bila {@code jenisKegiatan.getDendaJikaTerlambat()} bernilai
 * {@code true}, maka {@code kelipatan}, {@code maksimal}, {@code dalamPersen}, dan nilai dendanya
 * <b>semuanya diambil dari {@link JenisKegiatan}</b> dan pengaturan denda di item biaya ini
 * diabaikan. Pengaturan di sini baru berlaku bila jenis kegiatannya sendiri tidak mengaktifkan
 * denda.</p>
 *
 * <h2>Pola "getter yang menulis" (field audit shadow)</h2>
 *
 * <p>Sama seperti entity AIS lain, sebagian getter di sini <b>menulis balik ke field</b> saat
 * menemukan nilai {@code null}, alih-alih sekadar mengembalikan nilai default. Karena kelas ini
 * dipetakan {@code dynamicUpdate = true} dengan akses properti, penulisan itu mengotori
 * (<i>dirty</i>) entity dan <b>ikut tersimpan ke database pada flush berikutnya</b>, walaupun
 * pemanggilnya hanya bermaksud membaca. Getter yang berperilaku begitu:</p>
 *
 * <ul>
 *   <li>{@link #getDeskripsi()} &mdash; <b>yang paling berdampak</b>: bila {@code deskripsi} masih
 *   {@code null}, ia mengisinya dengan {@link #getNama()}. Sekali sebuah baris pernah dibaca lewat
 *   getter ini dalam session yang di-flush, kolom {@code deskripsi} permanen berisi salinan nama,
 *   dan "deskripsi kosong" tidak bisa lagi dibedakan dari "deskripsi memang sama dengan nama".</li>
 *   <li>{@link #getAktif()} &rarr; menulis {@code true};</li>
 *   <li>{@link #getPenghitungan()} &rarr; menulis {@link #TIDAK_ADA_PENGHITUNGAN};</li>
 *   <li>{@link #getMaxSmt()} &rarr; menulis {@code 30};</li>
 *   <li>{@link #getAutoCreate()}, {@link #getNilaiBisaDiubah()},
 *   {@link #getTerhubungKeNilaiTambahan()} &rarr; menulis {@code false};</li>
 *   <li>{@link #getParameterTambahan()} dan {@link #getJenisPembayaran()} &rarr; menulis hasil
 *   {@link GeneralValueObject#check(Object)} (resolusi proxy lazy &mdash; ini pola standar dan
 *   memang disengaja, lihat {@link GeneralValueObject}).</li>
 * </ul>
 *
 * <p>Sisanya memakai bentuk aman {@code return x == null ? default : x} tanpa menulis apa pun.
 * Percampuran dua gaya ini di satu file adalah sumber kebingungan yang nyata: dua getter bertetangga
 * dengan bentuk penulisan berbeda punya efek samping berbeda pula. Bandingkan
 * {@link #getMinSmt()} (aman) dengan {@link #getMaxSmt()} (menulis).</p>
 *
 * <p><b>Tidak ada</b> method di file ini yang membuka session Hibernate sendiri atau memanggil
 * {@code HibernateUtil.closeSession()}. Keempat method {@code ambil*} memakai
 * {@link HibernateUtil#currentSession()} (session milik request yang sedang berjalan) dan
 * membiarkannya terbuka &mdash; itu benar dan memang pola yang diharapkan di sini.</p>
 *
 * <h2>Kuirk &amp; jebakan yang sudah terverifikasi</h2>
 *
 * <ul>
 *   <li><b>{@link #getTidakDitagihDiSmtGanjil()} dan {@link #getTidakDitagihDiSmtGenap()} tidak
 *   pernah ditegakkan.</b> Kedua flag itu hanya dibaca dan ditulis oleh
 *   {@code ItemBiayaAction} (mengisi dan menyimpan checkbox); penelusuran seluruh pohon sumber tidak
 *   menemukan satu pun tempat yang memakainya untuk menyaring pembuatan tagihan. Operator bisa
 *   mencentangnya dan mengira item tidak akan ditagih di semester genap, padahal tetap ditagih.</li>
 *   <li><b>{@link #getMinSmt()}/{@link #getMaxSmt()} hanya jadi peringatan, bukan penyaring.</b>
 *   Di luar layar master, keduanya cuma dipakai {@code DaftarUlangMahasiswaBaruAction} dan
 *   {@code DaftarUlangMahasiswaLamaAction} untuk menampilkan teks peringatan "di luar rentang
 *   tagihan". Tagihannya sendiri tidak dibatalkan.</li>
 *   <li><b>{@link #getAutoCreate()} tidak pernah di-set {@code true} oleh kode Java mana pun.</b>
 *   {@link #setAutoCreate(Boolean)} tidak punya pemanggil selain layar master. Efeknya (mengunci
 *   {@code kode} dan {@code penghitungan} di form) baru muncul kalau kolomnya diisi lewat SQL/
 *   migrasi langsung.</li>
 *   <li><b>Tiga entri combobox tanpa penangan.</b> {@link #DIAMBIL_DARI_DENDA_PERPUSTAKAAN}
 *   ({@code "456"}) terdaftar di {@link #PENGHITUNGAN_MAP} sehingga bisa dipilih operator, tetapi
 *   tidak dirujuk di mana pun di luar file ini &mdash; memilihnya sama saja dengan tidak memilih
 *   apa-apa. {@link #DIKALI_JUMLAH_SKS_UAS_REMDIAL} ({@code "5592"}) punya penangan HANYA di jalur
 *   tagihan bulanan; di jalur {@link DetailBiaya} biasa cabangnya hilang &mdash; blok yang
 *   seharusnya menanganinya justru menguji {@link #DIKALI_JUMLAH_SKS_UTS_REMEDIAL} untuk kedua
 *   kalinya (blok kembar persis, cabang kedua tak terjangkau). Jadi item "dikali jumlah SKS
 *   matakuliah remedial yang ada uas-nya" tidak dikalikan pada tagihan biasa.</li>
 *   <li><b>Tujuh konstanta blok "BARU" belum tersambung.</b> {@link #DIKALI_JUMLAH_PERTEMUAN} dan
 *   enam saudaranya tidak dimasukkan ke {@link #PENGHITUNGAN_MAP} dan tidak dirujuk di mana pun
 *   &mdash; jadi tidak bisa dipilih operator dan tidak punya penangan. Rangka untuk fitur yang
 *   belum jadi.</li>
 *   <li><b>{@link #ambilAkun(Fakultas, Jurusan, String, String)} tidak dibungkus {@code try/catch},
 *   tiga saudaranya dibungkus.</b> Bila query gagal, {@code ambilAkun} melempar ke pemanggil
 *   sedangkan {@code ambilPiutang}/{@code ambilDibayarDimuka}/{@code ambilPendapatanDenda}
 *   mengembalikan {@code null} diam-diam. Perilaku posting jurnal jadi berbeda tergantung akun mana
 *   yang bermasalah.</li>
 *   <li><b>Salah eja yang sudah telanjur jadi nama kolom.</b> {@code dendaAkanBerlipatTerlambaHari}
 *   kehilangan huruf {@code t} ("Terlamba"). Karena {@code MyNamingStrategy} adalah turunan
 *   {@code DefaultNamingStrategy} (nama kolom = nama properti apa adanya, tanpa konversi
 *   {@code snake_case}) dan properti ini tidak dianotasi {@code @Column}, nama kolom di PostgreSQL
 *   pun ikut salah eja. Tidak bisa diperbaiki tanpa migrasi skema.</li>
 *   <li><b>Field {@code akunKredit} beserta getter/setter-nya dikomentari</b> (sisa desain lama:
 *   satu akun kredit langsung di item biaya). Digantikan sepenuhnya oleh tabel jembatan
 *   {@link ItemBiayaPunyaAkun}. Jangan dihidupkan kembali tanpa memeriksa mesin posting.</li>
 *   <li><b>{@link #setOleh(String)} dan {@link #setOlehId(String)} tidak bisa mengosongkan
 *   nilai</b> &mdash; keduanya langsung {@code return} bila argumennya {@code null}/kosong.</li>
 *   <li><b>Hanya empat properti yang dianotasi {@code @Column}</b> ({@code id}, {@code nama},
 *   {@code deskripsi}, {@code kode}). Selebihnya jatuh ke penamaan default, sehingga nama kolomnya
 *   {@code camelCase} apa adanya &mdash; termasuk {@code tidakDitagihDiSmtGanjil},
 *   {@code tanggalTagihanMengikutiRencanaTahunAkademik}, dan seterusnya. Hanya
 *   {@code parameter_tambahan} dan {@code jenis_pembayaran} yang {@code snake_case} karena
 *   dianotasi {@code @JoinColumn} eksplisit.</li>
 *   <li><b>Baris {@code kode} di database unik</b> ({@code @Column(unique = true)}) tetapi
 *   {@link #getKode()} mengembalikan {@code ""} untuk {@code null}, sehingga kode kosong dan kode
 *   {@code null} tampak sama di lapisan Java padahal berbeda di indeks unik.</li>
 * </ul>
 *
 * @see GeneralValueObject
 * @see DetailBiaya
 * @see DetailSettingBiaya
 * @see DetailKegiatan
 * @see JenisKegiatan
 * @see ItemBiayaPunyaAkun
 * @see ItemBiayaPunyaPiutang
 * @see ItemBiayaPunyaDibayarDimuka
 * @see ItemBiayaPunyaPendapatanDenda
 * @see ais.action.master.ItemBiayaAction
 * @see ais.action.master.helper.PembayaranNominalModifikasiHelper
 * @see ais.action.master.helper.PembayaranUtilHelper
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "item_biaya")
public class ItemBiaya extends GeneralValueObject {

	/**
	 * Mode: nominal dikali total SKS matakuliah <b>yang diulang</b> pada semester bersangkutan.
	 * Kunci combobox {@code "5051"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG = "Dikali jumlah SKS matakuliah mengulang";
	/**
	 * Mode: nominal dikali total SKS matakuliah yang <b>bukan</b> pengulangan. Pasangan komplementer
	 * {@link #DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG}. Kunci combobox {@code "5052"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG = "Dikali jumlah SKS matakuliah tidak mengulang";
	/**
	 * Mode paling umum dipakai: nominal dikali total SKS seluruh matakuliah yang diambil mahasiswa
	 * pada semester bersangkutan (SPP variabel per SKS). Kunci combobox {@code "505"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MAHASISWA = "Dikali jumlah SKS matakuliah yang diambil";
	/**
	 * Mode: nominal dikali total SKS matakuliah berjenis praktek. Kunci combobox {@code "506"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MK_PRAKTEK = "Dikali jumlah SKS matakuliah praktek";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_SKS_MK_PRAKTEK} tetapi khusus matakuliah semester pendek.
	 * Kunci combobox {@code "5061"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP = "Dikali jumlah SKS matakuliah praktek SP";
	/**
	 * Mode: nominal dikali total SKS matakuliah hasil konversi (mahasiswa pindahan/alih jenjang).
	 * Kunci combobox {@code "514"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_MK_KONVERSI = "Dikali jumlah SKS matakuliah konversi";
	/**
	 * Mode biner: pengali {@code 1} bila mahasiswa mengambil minimal satu matakuliah konversi,
	 * {@code 0} bila tidak (biaya sekali bayar, bukan per SKS). Kunci combobox {@code "5142"}.
	 */
	public static final String DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI = "Dikali satu jika mengambil matakuliah konversi";
	/**
	 * Mode biner: pengali {@code 1} bila mahasiswa mengambil matakuliah tertentu yang namanya/kodenya
	 * didaftar di {@link #getNamaMatakuliah()} (dipisah titik koma). Kunci combobox {@code "51422"}.
	 *
	 * <p><b>Perhatian: teks konstanta ini mengandung salah eja yang load-bearing</b> &mdash;
	 * {@code "suatau"}, bukan {@code "suatu"}. Nilai itulah yang tersimpan di kolom
	 * {@code penghitungan} pada baris-baris produksi, jadi memperbaiki ejaannya justru akan memutus
	 * pencocokan dan mematikan mode ini pada data lama. Bandingkan dengan
	 * {@link #DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA} yang ejaannya benar.</p>
	 */
	public static final String DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU = "Dikali satu jika mengambil suatau matakuliah tertentu";

	/**
	 * Mode biner: pengali {@code 1} bila mahasiswa dinyatakan lulus pada semester yang sama dengan
	 * semester tagihan (mis. biaya wisuda yang hanya ditagih di semester kelulusan). Kunci combobox
	 * {@code "51424"}.
	 */
	public static final String DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA = "Dikali satu jika lulus di semester yang sama";

	/**
	 * Mode biner: seperti {@link #DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU}, tetapi pengambilan matakuliah
	 * yang diperiksa adalah pada <b>semester sebelumnya</b>. Daftar matakuliahnya juga dibaca dari
	 * {@link #getNamaMatakuliah()}. Kunci combobox {@code "51423"}.
	 */
	public static final String DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA = "Dikali satu jika mengambil suatu matakuliah tertentu dan semester sebelumnya";

	/**
	 * Mode: nominal dikali <b>cacah</b> matakuliah konversi (bukan cacah SKS-nya). Pasangan
	 * {@link #DIKALI_JUMLAH_SKS_MK_KONVERSI}. Kunci combobox {@code "5141"}.
	 */
	public static final String DIKALI_JUMLAH_MK_KONVERSI = "Dikali jumlah matakuliah konversi";
	/**
	 * Mode khusus: nominal tagihan diambil dari sisa tunggakan mahasiswa pada semester sebelumnya,
	 * bukan dari pengali apa pun. Kunci combobox {@code "515"}.
	 */
	public static final String HITUNG_TUNGGAKAN_SMT_LALU = "Hitung tunggakan semester Lalu";
	/**
	 * Mode: nominal dikali total SKS matakuliah berjenis teori/diskusi. Kunci combobox {@code "507"}.
	 */
	public static final String DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI = "Dikali jumlah SKS matakuliah teori / diskusi";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI} tetapi khusus semester pendek.
	 * Kunci combobox {@code "5071"}.
	 */
	public static final String DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP = "Dikali jumlah SKS matakuliah teori / diskusi SP";
	/**
	 * Mode: nominal dikali total SKS matakuliah berjenis simulasi. Kunci combobox {@code "707"}
	 * &mdash; satu-satunya kunci berawalan {@code 7}, sehingga entri ini muncul paling belakang di
	 * combobox.
	 */
	public static final String DIKALI_JUMLAH_MK_SKS_SIMULASI = "Dikali jumlah SKS matakuliah simulasi";

	/**
	 * Mode: nominal dikali <b>cacah</b> matakuliah yang punya komponen UTS (biaya per lembar/per
	 * mata ujian). Kunci combobox {@code "508"}.
	 */
	public static final String DIKALI_JUMLAH_MK_UTS = "Dikali jumlah matakuliah yang ada uts-nya";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_MK_UTS} tetapi khusus matakuliah semester pendek. Kunci
	 * combobox {@code "5081"}.
	 */
	public static final String DIKALI_JUMLAH_MK_UTS_SP = "Dikali jumlah matakuliah semester pendek yang ada uts-nya";

	/**
	 * Mode: nominal dikali <b>cacah</b> matakuliah yang punya komponen UAS. Kunci combobox
	 * {@code "509"}.
	 */
	public static final String DIKALI_JUMLAH_MK_UAS = "Dikali jumlah matakuliah yang ada uas-nya";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_MK_UAS} tetapi khusus matakuliah semester pendek. Kunci
	 * combobox {@code "5091"}.
	 */
	public static final String DIKALI_JUMLAH_MK_UAS_SP = "Dikali jumlah matakuliah semester pendek yang ada uas-nya";

	/**
	 * Mode: nominal dikali <b>cacah</b> matakuliah praktek (bukan cacah SKS-nya). Pasangan
	 * {@link #DIKALI_JUMLAH_SKS_MK_PRAKTEK}. Kunci combobox {@code "510"}.
	 */
	public static final String DIKALI_JUMLAH_MK_PRAKTEK = "Dikali jumlah matakuliah praktek";

	/**
	 * Mode: nominal dikali <b>cacah</b> seluruh matakuliah yang diambil. Pasangan
	 * {@link #DIKALI_JUMLAH_SKS_MAHASISWA}. Kunci combobox {@code "511"}.
	 */
	public static final String DIKALI_JUMLAH_MK = "Dikali jumlah matakuliah yang diambil";
	/**
	 * Mode: nominal dikali cacah matakuliah semester pendek yang diambil. Kunci combobox
	 * {@code "513"}.
	 *
	 * <p>Catatan sejarah: baris pendaftaran untuk kunci {@code "5111"} dikomentari di blok
	 * inisialisasi statis dengan keterangan "belum", sehingga konstanta ini akhirnya didaftarkan
	 * dengan kunci {@code "513"}.</p>
	 */
	public static final String DIKALI_JUMLAH_MK_SP = "Dikali jumlah matakuliah semester pendek yang diambil";
	/**
	 * Mode biner: pengali {@code 1} bila mahasiswa mengambil minimal satu matakuliah semester pendek
	 * (biaya pendaftaran SP sekali bayar). Kunci combobox {@code "5112"}.
	 */
	public static final String DIKALI_SATU_JIKA_AMBIL_MK_SP = "Dikali satu jika mengambil matakuliah SP";

	/**
	 * Mode: nominal dikali cacah matakuliah teori/diskusi. Pasangan cacah dari
	 * {@link #DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI}. Kunci combobox {@code "512"}.
	 */
	public static final String DIKALI_JUMLAH_MK_DISKUSI_TEORI = "Dikali jumlah matakuliah teori / diskusi";

	/**
	 * Penanda bahwa item biaya ini adalah <b>komponen pengurang</b> (diskon/potongan/beasiswa),
	 * bukan penambah. Kunci combobox {@code "000"}, sehingga selalu muncul paling atas di daftar
	 * pilihan.
	 *
	 * <p>Berbeda dengan konstanta {@code DIKALI_*} lain, mode ini tidak punya cabang perhitungan di
	 * {@code PembayaranNominalModifikasiHelper}. Perannya adalah <b>penanda tanda (sign)</b> yang
	 * diperiksa langsung oleh puluhan pemanggil di seluruh modul keuangan (lebih dari empat puluh
	 * berkas), yang memperlakukan nominalnya sebagai pengurang total tagihan.</p>
	 */
	public static final String DIKALI_NILAI_MINUS = "Dikali nilai minus / pengurangan / diskon";

	/**
	 * Mode: nominal dikali total <b>SKS</b> matakuliah yang punya komponen UTS (berbeda dari
	 * {@link #DIKALI_JUMLAH_MK_UTS} yang memakai cacah matakuliah). Kunci combobox {@code "558"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_UTS = "Dikali jumlah SKS matakuliah yang ada uts-nya";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_SKS_UTS} tetapi khusus semester pendek. Kunci combobox
	 * {@code "5581"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_UTS_SP = "Dikali jumlah SKS matakuliah semester pendek yang ada uts-nya";

	/**
	 * Mode: nominal dikali total SKS matakuliah <b>remedial</b> yang punya komponen UTS. Kunci
	 * combobox {@code "5582"}.
	 *
	 * <p>Mode inilah yang tidak sengaja diuji <b>dua kali berturut-turut</b> di
	 * {@code PembayaranNominalModifikasiHelper} (dua blok kembar persis); cabang keduanya tidak akan
	 * pernah tercapai dan tampaknya seharusnya menguji
	 * {@link #DIKALI_JUMLAH_SKS_UAS_REMDIAL}.</p>
	 */
	public static final String DIKALI_JUMLAH_SKS_UTS_REMEDIAL = "Dikali jumlah SKS matakuliah remedial yang ada uts-nya";

	/**
	 * Mode: nominal dikali total SKS matakuliah yang punya komponen UAS. Kunci combobox
	 * {@code "559"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_UAS = "Dikali jumlah SKS matakuliah yang ada uas-nya";
	/**
	 * Mode: seperti {@link #DIKALI_JUMLAH_SKS_UAS} tetapi khusus semester pendek. Kunci combobox
	 * {@code "5591"}.
	 */
	public static final String DIKALI_JUMLAH_SKS_UAS_SP = "Dikali jumlah SKS matakuliah semester pendek yang ada uas-nya";

	/**
	 * Mode: nominal dikali total SKS matakuliah <b>remedial</b> yang punya komponen UAS. Kunci
	 * combobox {@code "5592"}.
	 *
	 * <p><b>Cacat terverifikasi:</b> mode ini hanya punya cabang perhitungan pada jalur tagihan
	 * bulanan ({@link PengaturanPembayaranBulanan}) di
	 * {@code PembayaranNominalModifikasiHelper}. Pada jalur {@link DetailBiaya} biasa cabangnya
	 * tidak ada &mdash; tempatnya terisi blok kembar
	 * {@link #DIKALI_JUMLAH_SKS_UTS_REMEDIAL}. Akibatnya item biaya yang dikonfigurasi dengan mode
	 * ini tidak dikalikan apa pun pada tagihan biasa dan nominalnya jatuh ke nilai dasar tanpa pesan
	 * kesalahan.</p>
	 *
	 * <p>Nama konstantanya sendiri salah eja ({@code REMDIAL}, bukan {@code REMEDIAL}), tetapi
	 * <i>teks nilainya</i> benar dan itulah yang tersimpan di database &mdash; jadi mengganti nama
	 * konstanta aman, mengganti teksnya tidak.</p>
	 */
	public static final String DIKALI_JUMLAH_SKS_UAS_REMDIAL = "Dikali jumlah SKS matakuliah remedial yang ada uas-nya";

	/**
	 * Mode: nominal tagihan diambil dari akumulasi denda perpustakaan mahasiswa. Kunci combobox
	 * {@code "456"}.
	 *
	 * <p><b>Tanpa penangan.</b> Konstanta ini terdaftar di {@link #PENGHITUNGAN_MAP} sehingga bisa
	 * dipilih operator di layar master, tetapi tidak dirujuk di berkas mana pun di luar kelas ini.
	 * Memilihnya berperilaku sama persis dengan {@link #TIDAK_ADA_PENGHITUNGAN}.</p>
	 */
	public static final String DIAMBIL_DARI_DENDA_PERPUSTAKAAN = "Diambil dari denda perpustakaan";

	/**
	 * Mode: nominal dikali cacah matakuliah remedial berbobot tepat 1 SKS. Kunci combobox
	 * {@code "4564"}. Bagian dari kuartet tarif remedial berjenjang menurut bobot SKS.
	 */
	public static final String DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS = "Dikali jumlah matakuliah remedial 1 SKS";
	/**
	 * Mode: nominal dikali cacah matakuliah remedial berbobot tepat 2 SKS. Kunci combobox
	 * {@code "4565"}.
	 */
	public static final String DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS = "Dikali jumlah matakuliah remedial 2 SKS";
	/**
	 * Mode: nominal dikali cacah matakuliah remedial berbobot tepat 3 SKS. Kunci combobox
	 * {@code "4566"}.
	 */
	public static final String DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS = "Dikali jumlah matakuliah remedial 3 SKS";
	/**
	 * Mode: nominal dikali cacah matakuliah remedial berbobot tepat 4 SKS. Kunci combobox
	 * {@code "4567"}.
	 */
	public static final String DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS = "Dikali jumlah matakuliah remedial 4 SKS";

	/**
	 * Mode: nominal dikali cacah seluruh matakuliah remedial tanpa memandang bobot SKS-nya. Kunci
	 * combobox {@code "4563"}. Alternatif "tarif rata" dari kuartet
	 * {@link #DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS} dan kawan-kawan.
	 */
	public static final String DIKALI_JUMLAH_MATAKULIAH_REMEDIAL = "Dikali jumlah matakuliah remedial";

	/**
	 * Nilai default/sentinel: item biaya ditagih apa adanya, tanpa pengali apa pun.
	 *
	 * <p>Satu-satunya konstanta mode yang <b>tidak</b> didaftarkan di {@link #PENGHITUNGAN_MAP};
	 * {@code ItemBiayaAction} menambahkannya ke combobox secara manual sesudah seluruh isi map,
	 * sehingga ia selalu jadi entri terakhir. Juga nilai yang ditulis balik oleh
	 * {@link #getPenghitungan()} bila kolomnya masih kosong.</p>
	 */
	public static final String TIDAK_ADA_PENGHITUNGAN = "Tidak ada penghitungan";

	// -----------------------------BARU-------------------------
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah pertemuan perkuliahan.
	 *
	 * <p><b>Belum tersambung.</b> Konstanta ini dan enam saudaranya di blok yang sama tidak
	 * didaftarkan di {@link #PENGHITUNGAN_MAP} dan tidak dirujuk di berkas mana pun &mdash; tidak
	 * bisa dipilih operator dan tidak punya cabang perhitungan. Rangka untuk skema tagihan berbasis
	 * pertemuan yang belum diselesaikan.</p>
	 */
	public static final String DIKALI_JUMLAH_PERTEMUAN = "Dikali jumlah pertemuan";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah pertemuan tatap muka. Belum tersambung; lihat
	 * {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_PERTEMUAN_TATAP_MUKA = "Dikali jumlah pertemuan tatap muka";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah pertemuan daring. Belum tersambung; lihat
	 * {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_PERTEMUAN_DARING = "Dikali jumlah pertemuan daring";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah tugas pertemuan. Belum tersambung; lihat
	 * {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_TUGAS_PERTEMUAN = "Dikali jumlah tugas pertemuan";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah ujian pertemuan. Belum tersambung; lihat
	 * {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_UJIAN_PERTEMUAN = "Dikali jumlah ujian pertemuan";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah materi audio pertemuan. Belum tersambung;
	 * lihat {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_AUDIO_PERTEMUAN = "Dikali jumlah audio pertemuan";
	/**
	 * Mode rencana (blok "BARU"): nominal dikali cacah materi video pertemuan. Belum tersambung;
	 * lihat {@link #DIKALI_JUMLAH_PERTEMUAN}.
	 */
	public static final String DIKALI_JUMLAH_VIDEO_PERTEMUAN = "Dikali jumlah video pertemuan";
	// -----------------------------BARU-------------------------

	/**
	 * Daftar pilihan mode penghitungan untuk combobox "Penghitungan" di layar master item biaya.
	 *
	 * <p>Peta ini <b>bukan</b> pemetaan {@code kode item biaya} &rarr; {@code mode}, meskipun
	 * kuncinya berupa string angka dan sempat dipakai begitu (lihat kode yang dikomentari di
	 * {@link #getPenghitungan()}). Peranannya sekarang murni untuk UI:</p>
	 *
	 * <ul>
	 *   <li><b>Kunci</b> ({@code "000"}, {@code "456"}, {@code "505"}, &hellip;) hanya menentukan
	 *   <i>urutan tampil</i>, karena {@link TreeMap} mengurutkan kuncinya secara leksikografis
	 *   sebagai {@code String} &mdash; bukan numerik. Karena itu {@code "5051"} muncul sebelum
	 *   {@code "506"}, dan {@code "51422"} sebelum {@code "5142"}&hellip; tidak, justru sesudahnya:
	 *   perbandingan huruf demi huruf membuat {@code "5142"} lebih dulu daripada {@code "51422"}.
	 *   Kunci tidak pernah disimpan di database.</li>
	 *   <li><b>Nilai</b> adalah teks mode itu sendiri, dan teks itulah yang dipasang sebagai label
	 *   sekaligus {@code value} setiap {@code Comboitem}, lalu disimpan mentah ke kolom
	 *   {@code penghitungan}.</li>
	 * </ul>
	 *
	 * <p><b>Peringatan mutabilitas:</b> peta ini {@code public static final} tetapi isinya
	 * <b>bisa diubah</b> siapa saja ({@code TreeMap} yang tidak dibungkus
	 * {@code Collections.unmodifiableMap}) dan dibagi ke seluruh aplikasi. Jangan pernah
	 * memodifikasinya saat runtime.</p>
	 *
	 * <p>{@link #TIDAK_ADA_PENGHITUNGAN} sengaja tidak ada di sini; {@code ItemBiayaAction}
	 * menambahkannya sendiri di akhir daftar. Tujuh konstanta blok "BARU" juga tidak ada di sini
	 * &mdash; itulah sebabnya belum bisa dipilih operator.</p>
	 *
	 * @see ais.action.master.ItemBiayaAction
	 */
	public static final TreeMap<String, String> PENGHITUNGAN_MAP = new TreeMap<String, String>();

	/*
	 * Blok inisialisasi statis: mengisi PENGHITUNGAN_MAP satu kali saat kelas dimuat. Urutan baris di
	 * sini tidak berpengaruh pada urutan tampil combobox (TreeMap mengurutkan sendiri menurut kunci);
	 * pengelompokannya semata-mata agar mudah dibaca. Baris "5111" sengaja dikomentari sejak awal
	 * dengan keterangan "belum".
	 */
	static {
		PENGHITUNGAN_MAP.put("505", DIKALI_JUMLAH_SKS_MAHASISWA);
		PENGHITUNGAN_MAP.put("5051", DIKALI_JUMLAH_SKS_MATAKULIAH_MENGULANG);
		PENGHITUNGAN_MAP.put("5052", DIKALI_JUMLAH_SKS_MATAKULIAH_TIDAK_MENGULANG);
		PENGHITUNGAN_MAP.put("506", DIKALI_JUMLAH_SKS_MK_PRAKTEK);
		PENGHITUNGAN_MAP.put("5061", DIKALI_JUMLAH_SKS_MK_PRAKTEK_SP);
		PENGHITUNGAN_MAP.put("514", DIKALI_JUMLAH_SKS_MK_KONVERSI);

		PENGHITUNGAN_MAP.put("5141", DIKALI_JUMLAH_MK_KONVERSI);
		PENGHITUNGAN_MAP.put("5142", DIKALI_SATU_JIKA_AMBIL_MK_KONVERSI);
		PENGHITUNGAN_MAP.put("51422", DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU);

		PENGHITUNGAN_MAP.put("51423", DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA);
		PENGHITUNGAN_MAP.put("51424", DIKALI_SATU_JIKA_LULUS_DISEMESTER_YANG_SAMA);

		PENGHITUNGAN_MAP.put("515", HITUNG_TUNGGAKAN_SMT_LALU);
		PENGHITUNGAN_MAP.put("507", DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI);
		PENGHITUNGAN_MAP.put("5071", DIKALI_JUMLAH_MK_SKS_DISKUSI_TEORI_SP);
		PENGHITUNGAN_MAP.put("707", DIKALI_JUMLAH_MK_SKS_SIMULASI);

		PENGHITUNGAN_MAP.put("508", DIKALI_JUMLAH_MK_UTS);

		PENGHITUNGAN_MAP.put("5081", DIKALI_JUMLAH_MK_UTS_SP);

		PENGHITUNGAN_MAP.put("509", DIKALI_JUMLAH_MK_UAS);

		PENGHITUNGAN_MAP.put("5091", DIKALI_JUMLAH_MK_UAS_SP);

		PENGHITUNGAN_MAP.put("510", DIKALI_JUMLAH_MK_PRAKTEK);

		PENGHITUNGAN_MAP.put("511", DIKALI_JUMLAH_MK);
		// PENGHITUNGAN_MAP.put("5111", DIKALI_JUMLAH_MK_SP); // belum
		PENGHITUNGAN_MAP.put("5112", DIKALI_SATU_JIKA_AMBIL_MK_SP);

		PENGHITUNGAN_MAP.put("512", DIKALI_JUMLAH_MK_DISKUSI_TEORI);

		PENGHITUNGAN_MAP.put("513", DIKALI_JUMLAH_MK_SP);

		PENGHITUNGAN_MAP.put("000", DIKALI_NILAI_MINUS);

		PENGHITUNGAN_MAP.put("558", DIKALI_JUMLAH_SKS_UTS);
		PENGHITUNGAN_MAP.put("5581", DIKALI_JUMLAH_SKS_UTS_SP);
		PENGHITUNGAN_MAP.put("5582", DIKALI_JUMLAH_SKS_UTS_REMEDIAL);
		PENGHITUNGAN_MAP.put("559", DIKALI_JUMLAH_SKS_UAS);
		PENGHITUNGAN_MAP.put("5591", DIKALI_JUMLAH_SKS_UAS_SP);
		PENGHITUNGAN_MAP.put("5592", DIKALI_JUMLAH_SKS_UAS_REMDIAL);

		PENGHITUNGAN_MAP.put("456", DIAMBIL_DARI_DENDA_PERPUSTAKAAN);

		PENGHITUNGAN_MAP.put("4563", DIKALI_JUMLAH_MATAKULIAH_REMEDIAL);

		PENGHITUNGAN_MAP.put("4564", DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_1_SKS);
		PENGHITUNGAN_MAP.put("4565", DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_2_SKS);
		PENGHITUNGAN_MAP.put("4566", DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_3_SKS);
		PENGHITUNGAN_MAP.put("4567", DIKALI_JUMLAH_MATAKULIAH_REMEDIAL_4_SKS);
	}

	/**
	 * Versi serialisasi Java. Entity ini ikut diserialkan saat disimpan di session HTTP/ZK atau cache
	 * berkas, jadi nilainya jangan diubah tanpa alasan.
	 */
	private static final long serialVersionUID = -4671225667012312808L;
	/** Kunci primer baris {@code item_biaya}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir (jejak audit ringan); lihat {@link #getOleh()}. */
	private String oleh;
	/** ID pengguna pengubah terakhir (jejak audit ringan); lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris item biaya ini (jejak audit ringan).
	 * Membayangi properti bernama sama di {@link GeneralValueObject}.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi ID pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param olehId ID pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. <b>Nilai {@code null} atau string kosong diabaikan</b>
	 * (method langsung keluar), sehingga jejak audit tidak bisa dikosongkan lewat setter ini.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris item biaya ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum baris ini di-{@code UPDATE},
	 * dan mendelegasikan pengisian jejak audit ({@code oleh}, {@code olehId},
	 * {@code tanggal_dirubah}) ke {@code AuditTimestampInterceptor.ubah(this)}. Ini implementasi
	 * satu-satunya method {@code abstract} milik {@link GeneralValueObject}; jangan dipanggil manual.
	 *
	 * <p>Perhatikan tidak ada padanan {@code @PrePersist}: pada {@code INSERT} pertama,
	 * {@code tanggal_dirubah} terisi dari inisialisasi field (waktu object dibuat) sedangkan
	 * {@code oleh}/{@code olehId} tetap {@code null} kecuali diisi pemanggil.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir. Diinisialisasi ke waktu pembuatan object sehingga tidak pernah
	 * {@code null} pada entity baru; lihat {@link #getTanggal_dirubah()}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir baris item biaya ini. Normalnya diisi otomatis oleh
	 * {@link #onUpdate()}; pemanggilan manual hanya dipakai saat migrasi/impor data.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris item biaya ini. Nilai awalnya diisi waktu
	 * pembuatan object ({@code WaktuUtil.getDate()}), bukan {@code null}.
	 *
	 * @return timestamp perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks item biaya ini dalam bentuk {@code id-kode-nama}, mis.
	 * {@code "17-SPP-SPP Reguler"}.
	 *
	 * <p>Dipakai combobox/banbox pemilih item biaya dan pesan log. Perhatikan method ini membaca
	 * <i>field</i> {@code kode} dan {@code nama} secara langsung, bukan lewat {@link #getKode()}/
	 * {@link #getNama()} &mdash; jadi nilainya <b>tidak di-{@code trim}</b> dan bisa memuat
	 * {@code "null"} bila field-nya kosong. Aman terhadap proxy lazy karena tidak menyentuh relasi
	 * apa pun.</p>
	 *
	 * @return string {@code id-kode-nama}
	 */
	public String toString() {
		return id + "-" + kode + "-" + nama;
	}

	/** Kode unik item biaya (mis. {@code "SPP"}); lihat {@link #getKode()}. */
	private String kode;
	/** Nama item biaya yang tampil di tagihan; lihat {@link #getNama()}. */
	private String nama;
	/** Deskripsi panjang; diisi otomatis dari {@code nama} oleh {@link #getDeskripsi()} bila kosong. */
	private String deskripsi;
	/** Mode pengali nominal, salah satu konstanta {@code DIKALI_*}; lihat {@link #getPenghitungan()}. */
	private String penghitungan;
	/**
	 * Daftar kode/nama matakuliah pemicu, dipisah titik koma; hanya relevan untuk dua mode
	 * "matakuliah tertentu". Lihat {@link #getNamaMatakuliah()}.
	 */
	private String namaMatakuliah;

	/** Penanda item biaya bawaan sistem; lihat {@link #getAutoCreate()}. */
	private Boolean autoCreate;
	/** Apakah item ini dikenakan denda keterlambatan; lihat {@link #getDendaJikaTerlambat()}. */
	private Boolean dendaJikaTerlambat;
	/** Apakah nilai denda berupa persen (bukan rupiah); lihat {@link #getNilaiDendaDalamPersen()}. */
	private Boolean nilaiDendaDalamPersen;
	/** Panjang periode kelipatan denda dalam hari; lihat {@link #getDendaAkanBerlipatTerlambaHari()}. */
	private Integer dendaAkanBerlipatTerlambaHari;
	/** Batas atas jumlah kelipatan denda; lihat {@link #getMaksimalBerlipatTerlambaHari()}. */
	private Integer maksimalBerlipatTerlambaHari;
	/** Besaran denda default (persen atau rupiah); lihat {@link #getDefaultProsentaseDenda()}. */
	private Double defaultProsentaseDenda;
	/** Pemilih istilah "Angsuran" pada tagihan bulanan; lihat {@link #getMenggunakanIstilahBayarAngsuran()}. */
	private Boolean menggunakanIstilahBayarAngsuran;
	// private Akun akunKredit;
	/** Apakah nominal boleh diubah kasir saat pembayaran; lihat {@link #getNilaiBisaDiubah()}. */
	private Boolean nilaiBisaDiubah;
	/** Apakah item muncul di surat tagihan; lihat {@link #getDitampilkanDiSuratTagihan()}. */
	private Boolean ditampilkanDiSuratTagihan;

	/** Apakah item terhubung ke parameter tambahan; lihat {@link #getTerhubungKeNilaiTambahan()}. */
	private Boolean terhubungKeNilaiTambahan;
	/** Definisi isian tambahan yang menentukan nominal; lihat {@link #getParameterTambahan()}. */
	private ParameterTambahan parameterTambahan;
	/** Penanda aktif/arsip; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Izin mencicil dari sisi mahasiswa; lihat {@link #getMahasiswaBolehMencicilkan()}. */
	private Boolean mahasiswaBolehMencicilkan;
	/** Izin mencicil dari sisi admin/keuangan; lihat {@link #getAdminBolehMencicilkan()}. */
	private Boolean adminBolehMencicilkan;

	/** Kanal/jenis pembayaran default; lihat {@link #getJenisPembayaran()}. */
	private JenisPembayaran jenisPembayaran;

	/** Batas bawah semester berlakunya item; lihat {@link #getMinSmt()}. */
	private Integer minSmt = 0;
	/** Batas atas semester berlakunya item; lihat {@link #getMaxSmt()}. */
	private Integer maxSmt = 30;

	/** Flag "jangan tagih di semester ganjil" &mdash; tidak ditegakkan; lihat {@link #getTidakDitagihDiSmtGanjil()}. */
	private Boolean tidakDitagihDiSmtGanjil;
	/** Flag "jangan tagih di semester genap" &mdash; tidak ditegakkan; lihat {@link #getTidakDitagihDiSmtGenap()}. */
	private Boolean tidakDitagihDiSmtGenap;

	/** Sumber tanggal tagihan; lihat {@link #getTanggalTagihanMengikutiRencanaTahunAkademik()}. */
	private Boolean tanggalTagihanMengikutiRencanaTahunAkademik;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA untuk instansiasi entity, dan dipakai
	 * layar master saat membuat item biaya baru. Seluruh properti dibiarkan pada nilai awalnya
	 * ({@code minSmt = 0}, {@code maxSmt = 30}, {@code tanggal_dirubah} = waktu sekarang, sisanya
	 * {@code null} &mdash; getter masing-masing yang menentukan nilai default efektifnya).
	 */
	public ItemBiaya() {
	}

	/**
	 * Konstruktor peringkas untuk membuat item biaya yang hanya diketahui namanya. Tidak dipakai
	 * jalur UI mana pun; disediakan generator hbm2java dan berguna untuk uji/impor data.
	 *
	 * @param nama nama item biaya; disimpan apa adanya tanpa {@code trim}
	 */
	public ItemBiaya(String nama) {
		this.nama = nama;
	}

	/**
	 * Kunci primer baris {@code item_biaya}, dibangkitkan database ({@code IDENTITY}/sequence).
	 * Kontrak umumnya &mdash; termasuk perannya di {@code equals}/{@code hashCode}/{@code compareTo}
	 * &mdash; dijelaskan di {@link GeneralValueObject}.
	 *
	 * @return ID baris, atau {@code null} bila entity belum pernah disimpan
	 * @see GeneralValueObject
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci primer. Normalnya hanya dipanggil Hibernate; jangan diubah manual pada entity
	 * yang sudah tersimpan.
	 *
	 * @param id ID baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama item biaya seperti yang tampil di tagihan, kuitansi, dan surat tagihan (mis.
	 * {@code "SPP Semester"}, {@code "Uang Gedung"}).
	 *
	 * <p>Nilainya di-{@code trim} sebelum dikembalikan, tetapi {@code null} tetap dikembalikan
	 * sebagai {@code null} (berbeda dengan {@link #getKode()} yang memulangkan {@code ""}). Kolomnya
	 * {@code NOT NULL} di skema, jadi {@code null} hanya mungkin pada entity yang belum disimpan.</p>
	 *
	 * @return nama item biaya sudah ter-{@code trim}, atau {@code null}
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama item biaya. Nilai disimpan apa adanya (tanpa {@code trim}); pemangkasan baru
	 * terjadi saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama item biaya
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Deskripsi panjang item biaya (keterangan bebas yang diisi operator di layar master).
	 *
	 * <p><b>Getter ini menulis.</b> Bila {@code deskripsi} masih {@code null}, method mengisi
	 * field-nya dengan {@link #getNama()} lebih dulu, lalu mengembalikannya. Karena entity ini
	 * dipetakan {@code dynamicUpdate = true} dengan akses properti, penulisan itu membuat entity
	 * kotor dan <b>ikut tersimpan ke kolom {@code deskripsi} pada flush berikutnya</b> walaupun
	 * pemanggilnya hanya membaca. Efeknya permanen: sesudah itu tidak ada lagi cara membedakan
	 * "deskripsi belum diisi" dari "deskripsi memang disamakan dengan nama".</p>
	 *
	 * <p>Perhatikan juga nilai yang ditulis berasal dari {@link #getNama()} yang sudah di-{@code
	 * trim}; bila {@code nama} juga {@code null}, {@code deskripsi} tetap {@code null} dan penulisan
	 * terulang setiap kali getter dipanggil.</p>
	 *
	 * @return deskripsi item biaya; jatuh ke nama item bila deskripsi belum diisi
	 */
	@Column(name = "deskripsi")
	public String getDeskripsi() {
		if (deskripsi == null) {
			deskripsi = getNama();
		}
		return this.deskripsi;
	}

	/**
	 * Mengisi deskripsi item biaya.
	 *
	 * @param deskripsi deskripsi bebas; {@code null} akan memicu perilaku isi-otomatis di
	 *                  {@link #getDeskripsi()}
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengisi kode item biaya. Nilai disimpan apa adanya (tanpa {@code trim}); kolomnya
	 * ber-{@code UNIQUE} sehingga kode kembar baru ketahuan saat {@code INSERT}/{@code UPDATE}
	 * gagal di database, bukan di sini.
	 *
	 * @param kode kode item biaya
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Kode singkat item biaya yang dipakai sebagai label pendek di banbox pemilih, laporan, dan
	 * pesan bank (mis. {@code "SPP"}, {@code "GEDUNG"}).
	 *
	 * <p>Mengembalikan {@code ""} (bukan {@code null}) bila kolomnya kosong, dan hasilnya
	 * di-{@code trim}. Perlu diingat: karena kolomnya {@code UNIQUE}, {@code null} dan {@code ""}
	 * berbeda di database (beberapa baris boleh {@code NULL}, tetapi hanya satu yang boleh
	 * {@code ""}) padahal di lapisan Java keduanya tampak sama lewat getter ini.</p>
	 *
	 * <p>Kode ini <b>tidak</b> ada hubungannya dengan kunci {@link #PENGHITUNGAN_MAP}, meskipun kode
	 * yang dikomentari di {@link #getPenghitungan()} menunjukkan keduanya pernah dikaitkan.</p>
	 *
	 * @return kode item biaya sudah ter-{@code trim}, atau {@code ""} bila belum diisi
	 */
	@Column(name = "kode", unique = true)
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	// @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	// @Fetch(FetchMode.SELECT)
	// @JoinColumn(name = "akun_kredit", nullable = true)
	// public Akun getAkunKredit() {
	// return akunKredit;
	// }
	//
	// public void setAkunKredit(Akun akunKredit) {
	// this.akunKredit = akunKredit;
	// }

	/**
	 * Apakah kasir boleh mengubah nominal item ini pada saat pembayaran ("Pada saat pembayaran nilai
	 * bisa di-ubah" di layar master).
	 *
	 * <p>Dipakai luas di alur daftar ulang dan bukti pembayaran untuk menentukan apakah kotak isian
	 * nominal dibiarkan terbuka atau dikunci ke nilai tagihan &mdash; termasuk memutuskan apakah
	 * baris tagihan boleh dilepas dari {@link DetailKegiatan} asalnya.</p>
	 *
	 * <p><b>Getter ini menulis:</b> bila field masih {@code null}, ia menuliskan {@code false} ke
	 * field sebelum mengembalikannya (bisa ikut tersimpan pada flush berikutnya).</p>
	 *
	 * <p>Blok yang dikomentari di dalamnya adalah sisa aturan lama: dulu item bermode
	 * {@link #DIKALI_NILAI_MINUS} (diskon) otomatis dianggap "nilai bisa diubah". Aturan itu sudah
	 * dimatikan &mdash; item diskon kini harus dicentang manual bila nominalnya perlu bisa
	 * diubah.</p>
	 *
	 * @return {@code true} bila nominal boleh diubah saat pembayaran; {@code false} bila terkunci
	 */
	public Boolean getNilaiBisaDiubah() {
		if (nilaiBisaDiubah == null) {
			nilaiBisaDiubah = false;
		}
//		if (getPenghitungan().equals(DIKALI_NILAI_MINUS)) {
//			nilaiBisaDiubah = true;
//		}
		return nilaiBisaDiubah;
	}

	/**
	 * Mengisi izin ubah nominal saat pembayaran.
	 *
	 * @param nilaiBisaDiubah {@code true} bila kasir boleh mengubah nominal
	 */
	public void setNilaiBisaDiubah(Boolean nilaiBisaDiubah) {
		this.nilaiBisaDiubah = nilaiBisaDiubah;
	}

	/**
	 * Penanda bahwa item biaya ini dianggap bawaan sistem. Bila {@code true}, layar master mengunci
	 * kotak {@code Kode} dan combobox {@code Penghitungan} agar tidak bisa diubah operator, dan
	 * kolom "Auto Create" di daftar menampilkan "Ya".
	 *
	 * <p><b>Catatan lapangan:</b> {@link #setAutoCreate(Boolean)} tidak pernah dipanggil dengan
	 * {@code true} oleh kode Java mana pun di pohon sumber ini. Jadi penguncian itu hanya aktif bila
	 * kolomnya diisi lewat SQL atau skrip migrasi langsung.</p>
	 *
	 * <p><b>Getter ini menulis:</b> bila field masih {@code null}, ia menuliskan {@code false} ke
	 * field sebelum mengembalikannya.</p>
	 *
	 * @return {@code true} bila item dianggap bawaan sistem; default {@code false}
	 */
	public Boolean getAutoCreate() {
		if (autoCreate == null) {
			autoCreate = false;
		}
		return autoCreate;
	}

	/**
	 * Mengisi penanda item bawaan sistem.
	 *
	 * @param autoCreate {@code true} bila item dianggap bawaan sistem
	 */
	public void setAutoCreate(Boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	/**
	 * Apakah nominal item ini ditentukan oleh isian tambahan yang diisi mahasiswa/admin, alih-alih
	 * oleh tarif tetap.
	 *
	 * <p>Bila {@code true}, {@link #getParameterTambahan()} wajib terisi dan
	 * {@code PembayaranNominalModifikasiHelper} akan mengambil nilai dari isian tambahan itu untuk
	 * menghitung nominal. Di layar master, mencentangnya memunculkan banbox pemilih
	 * {@link ParameterTambahan}.</p>
	 *
	 * <p><b>Getter ini menulis:</b> bila field masih {@code null}, ia menuliskan {@code false} ke
	 * field sebelum mengembalikannya.</p>
	 *
	 * @return {@code true} bila nominal bergantung pada parameter tambahan; default {@code false}
	 */
	public Boolean getTerhubungKeNilaiTambahan() {
		if (terhubungKeNilaiTambahan == null) {
			terhubungKeNilaiTambahan = false;
		}
		return terhubungKeNilaiTambahan;
	}

	/**
	 * Mengisi penanda keterhubungan ke parameter tambahan. Mematikannya tidak mengosongkan
	 * {@link #getParameterTambahan()} &mdash; relasinya tetap tersimpan, hanya tidak lagi
	 * dipakai/ditampilkan.
	 *
	 * @param terhubungKeNilaiTambahan {@code true} bila nominal bergantung pada parameter tambahan
	 */
	public void setTerhubungKeNilaiTambahan(Boolean terhubungKeNilaiTambahan) {
		this.terhubungKeNilaiTambahan = terhubungKeNilaiTambahan;
	}

	/**
	 * Definisi isian tambahan ({@link ParameterTambahan}) yang menentukan nominal item ini, relevan
	 * hanya bila {@link #getTerhubungKeNilaiTambahan()} bernilai {@code true}.
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code parameter_tambahan}. Seperti seluruh getter
	 * relasi di AIS, hasil {@link GeneralValueObject#check(Object)} <b>ditugaskan kembali ke
	 * field</b> supaya proxy lazy yang sudah lepas dari session tergantikan object hidup &mdash;
	 * kontrak lengkapnya di {@link GeneralValueObject}.</p>
	 *
	 * @return parameter tambahan penentu nominal, atau {@code null} bila tidak dipakai
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan")
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * Mengisi parameter tambahan penentu nominal.
	 *
	 * @param parameterTambahan definisi isian tambahan, boleh {@code null}
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Apakah item biaya ini masih boleh dipakai untuk tagihan baru. Item yang tidak lagi dipakai
	 * di-nonaktifkan (arsip), bukan dihapus, agar tagihan lama tetap punya rujukan yang sah.
	 *
	 * <p><b>Default {@code true}</b> &mdash; berbeda dari flag lain di kelas ini yang default
	 * {@code false}. Konsekuensinya: baris warisan yang kolom {@code aktif}-nya masih {@code NULL}
	 * otomatis dianggap aktif.</p>
	 *
	 * <p><b>Getter ini menulis:</b> bila field masih {@code null}, ia menuliskan {@code true} ke
	 * field sebelum mengembalikannya. Di layar master, mengubah centang "Aktif" langsung memanggil
	 * {@code Common.refreshSaveOrUpdate(itemBiaya)} sehingga tersimpan seketika tanpa tombol
	 * simpan.</p>
	 *
	 * @return {@code true} bila item masih aktif; default {@code true}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengisi status aktif item biaya.
	 *
	 * @param aktif {@code true} bila item boleh dipakai untuk tagihan baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mode pengali nominal item ini &mdash; salah satu konstanta {@code DIKALI_*}/{@code HITUNG_*}
	 * di kelas ini, atau {@link #TIDAK_ADA_PENGHITUNGAN}.
	 *
	 * <p>Nilainya adalah <b>kalimat berbahasa Indonesia utuh</b>, bukan kode. Seluruh pemanggil
	 * membandingkannya dengan {@code equals(...)} terhadap konstanta di kelas ini; penafsir
	 * utamanya {@code PembayaranNominalModifikasiHelper}. Lihat bagian "Mode penghitungan" pada
	 * Javadoc kelas untuk gambaran lengkapnya, termasuk mode yang tidak punya penangan.</p>
	 *
	 * <p><b>Getter ini menulis:</b> bila field {@code null} atau berisi string kosong, ia menuliskan
	 * {@link #TIDAK_ADA_PENGHITUNGAN} ke field sebelum mengembalikannya. Karena itu getter ini tidak
	 * pernah mengembalikan {@code null} &mdash; itulah sebabnya pemanggil di seluruh repo aman
	 * menulis {@code itemBiaya.getPenghitungan().equals(...)} tanpa penjagaan null.</p>
	 *
	 * <p>Blok yang dikomentari di awal method adalah desain lama: mode dulu diturunkan dari
	 * {@link #getKode()} lewat {@link #PENGHITUNGAN_MAP} (jadi kode item biaya harus berupa angka
	 * {@code "505"}, {@code "506"}, &hellip;). Aturan itu sudah dilepas; mode kini murni disimpan di
	 * kolomnya sendiri dan kunci map tinggal jadi penentu urutan combobox.</p>
	 *
	 * @return teks mode penghitungan; tidak pernah {@code null}
	 * @see #PENGHITUNGAN_MAP
	 * @see ais.action.master.helper.PembayaranNominalModifikasiHelper
	 */
	public String getPenghitungan() {
//		if (kode != null && !kode.trim().isEmpty() && PENGHITUNGAN_MAP.containsKey(kode.trim())) {
//			penghitungan = PENGHITUNGAN_MAP.get(kode);
//		} else

		if (penghitungan == null || penghitungan.trim().isEmpty()) {
			penghitungan = TIDAK_ADA_PENGHITUNGAN;
		}
		return penghitungan;
	}

	/**
	 * Mengisi mode penghitungan. <b>Wajib</b> memakai salah satu konstanta di kelas ini; string bebas
	 * akan tersimpan tanpa keluhan tetapi tidak pernah cocok dengan cabang mana pun sehingga item
	 * berperilaku seperti {@link #TIDAK_ADA_PENGHITUNGAN}.
	 *
	 * @param penghitungan teks mode penghitungan
	 */
	public void setPenghitungan(String penghitungan) {
		this.penghitungan = penghitungan;
	}

	/**
	 * Mencari akun <b>pendapatan</b> untuk item biaya ini berdasarkan konteks satu {@link Kegiatan}
	 * (satu kegiatan akademik/keuangan milik seorang mahasiswa atau calon mahasiswa).
	 *
	 * <p>Method ini hanyalah adaptor: ia menyimpulkan fakultas, jurusan, program, dan angkatan dari
	 * kegiatan, lalu mendelegasikan ke
	 * {@link #ambilAkun(Fakultas, Jurusan, String, String)}. Sumber konteksnya dipilih menurut
	 * urutan berikut, yang pertama cocok dipakai:</p>
	 *
	 * <ol>
	 *   <li>{@code kegiatan.getMahasiswa()} terisi &rarr; pakai jurusan mahasiswa (beserta
	 *   fakultasnya), program, dan tahun angkatan mahasiswa;</li>
	 *   <li>{@code kegiatan.getCalonMahasiswa().getProdiLulus()} terisi &rarr; pakai prodi kelulusan
	 *   seleksi calon mahasiswa;</li>
	 *   <li>{@code kegiatan.getCalonMahasiswa().getProdi1()} terisi &rarr; pakai pilihan prodi
	 *   pertama.</li>
	 * </ol>
	 *
	 * <p><b>Risiko {@code NullPointerException}:</b> cabang pertama memanggil
	 * {@code getJurusan().getFakultas()} dan {@code getTahunangkatan().toString()} tanpa penjagaan
	 * null. Mahasiswa tanpa jurusan atau tanpa tahun angkatan (data impor yang belum lengkap) akan
	 * membuat method ini melempar, dan &mdash; berbeda dari tiga saudaranya &mdash; tidak ada
	 * {@code try/catch} yang menahannya.</p>
	 *
	 * <p>Dipanggil dari mesin posting jurnal ({@code PostingDetailKegiatanAction},
	 * {@code PostingCicilanMahasiswaAction},
	 * {@code PostingCicilanDibayarDimukaMahasiswaAction}) dan dari
	 * {@code AnalisisPemetaanAkunHelper} yang memeriksa kelengkapan pemetaan akun sebelum
	 * posting.</p>
	 *
	 * @param kegiatan kegiatan sumber konteks; {@code null} atau kegiatan tanpa mahasiswa maupun
	 *                 calon mahasiswa yang berprodi menghasilkan {@code null}
	 * @return akun pendapatan yang cocok, atau {@code null} bila tidak ada pemetaan yang cocok
	 * @see #ambilAkun(Fakultas, Jurusan, String, String)
	 */
	public Akun ambilAkun(Kegiatan kegiatan) {
		if (kegiatan != null && kegiatan.getMahasiswa() != null) {
			return ambilAkun(kegiatan.getMahasiswa().getJurusan().getFakultas(), kegiatan.getMahasiswa().getJurusan(),
					kegiatan.getMahasiswa().getProgram(), kegiatan.getMahasiswa().getTahunangkatan().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
			return ambilAkun(kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdiLulus(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdi1() != null) {
			return ambilAkun(kegiatan.getCalonMahasiswa().getProdi1().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdi1(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		}

		return null;
	}

	/**
	 * Mencari akun <b>pendapatan</b> untuk item biaya ini di tabel jembatan
	 * {@link ItemBiayaPunyaAkun}, dengan pencocokan berjenjang dari konteks paling spesifik ke
	 * paling umum. <b>Method ini adalah acuan lengkap untuk keempat resolver akun di kelas ini</b>
	 * ({@link #ambilPiutang}, {@link #ambilDibayarDimuka}, {@link #ambilPendapatanDenda} memakai
	 * algoritma identik, hanya berbeda tabel jembatannya).
	 *
	 * <h3>Penyaring angkatan (selalu ikut di semua tahap)</h3>
	 *
	 * <p>Setiap query menambahkan syarat: kolom {@code angkatan} pada baris pemetaan harus
	 * <i>mengandung</i> teks angkatan yang diminta ({@code ILIKE '%angkatan%'}, tidak peka besar
	 * kecil huruf), <b>atau</b> {@code NULL}, <b>atau</b> string kosong. Artinya satu baris pemetaan
	 * bisa melayani banyak angkatan sekaligus dengan menuliskannya berderet (mis.
	 * {@code "2021,2022,2023"}), dan baris tanpa angkatan berlaku untuk semua angkatan.</p>
	 *
	 * <p><b>Jebakan {@code ANYWHERE}:</b> karena pencocokannya substring, angkatan {@code "202"}
	 * akan cocok dengan baris {@code "2021"}, dan angkatan {@code "21"} cocok dengan
	 * {@code "2021"}. Selama angkatan selalu empat digit hal ini tidak terasa, tetapi jangan pakai
	 * penulisan angkatan dua digit.</p>
	 *
	 * <h3>Urutan pencarian (delapan tahap, berhenti pada hasil pertama yang tidak {@code null})</h3>
	 *
	 * <ol>
	 *   <li>jurusan = {@code jurusan} <b>dan</b> program = {@code program} &mdash; dilewati bila
	 *   {@code jurusan} {@code null};</li>
	 *   <li>fakultas = {@code fakultas} dan program = {@code program} &mdash; hanya bila
	 *   {@code fakultas} tidak {@code null};</li>
	 *   <li>jurusan = {@code jurusan} dan program {@code IS NULL};</li>
	 *   <li>fakultas = {@code fakultas} dan program {@code IS NULL};</li>
	 *   <li>jurusan {@code IS NULL} dan program = {@code program};</li>
	 *   <li>fakultas {@code IS NULL} dan program = {@code program};</li>
	 *   <li>jurusan {@code IS NULL} dan program {@code IS NULL};</li>
	 *   <li>fakultas {@code IS NULL} dan program {@code IS NULL} &mdash; pemetaan paling umum.</li>
	 * </ol>
	 *
	 * <p>Semua tahap juga menyaring {@code itemBiaya = this}, memakai
	 * {@code Projections.property("akun.id")} dan {@code setMaxResults(1)} sehingga yang diambil
	 * dari database hanyalah satu ID akun (bukan seluruh baris jembatan). ID itu kemudian
	 * dimaterialkan menjadi {@link Akun} oleh
	 * {@code ConstantValues.simpleObject(criteria, Akun.class, false)}, yang mengambilnya dari cache
	 * entity bila ada. Argumen {@code false} berarti "projection sudah dipasang pemanggil, jangan
	 * pasang lagi".</p>
	 *
	 * <p><b>Kelonggaran yang perlu disadari pada tahap 5&ndash;8:</b> tahap yang membatasi
	 * {@code jurusan IS NULL} <i>tidak</i> ikut membatasi {@code fakultas}, dan sebaliknya. Jadi
	 * tahap 5 bisa saja memungut baris pemetaan yang fakultasnya terisi fakultas <b>lain</b>. Selama
	 * pemetaan diisi lewat layar UI (yang mengisi salah satu saja) hal ini tidak muncul, tetapi
	 * pemetaan hasil impor yang mengisi keduanya bisa memberi akun yang tidak diharapkan.</p>
	 *
	 * <h3>Session dan penanganan galat</h3>
	 *
	 * <p>Memakai {@link HibernateUtil#currentSession()} &mdash; session milik request yang sedang
	 * berjalan &mdash; dan <b>tidak menutupnya</b>; itu benar, penutupan session bukan urusan
	 * entity. Berbeda dari tiga saudaranya, method ini <b>tidak dibungkus {@code try/catch}</b>,
	 * sehingga kegagalan query merambat ke pemanggil alih-alih menjadi {@code null} diam-diam.</p>
	 *
	 * @param fakultas fakultas konteks; boleh {@code null} (tahap 2 dan 4 dilewati)
	 * @param jurusan  jurusan/program studi konteks; boleh {@code null} (tahap 1 dan 3 dilewati)
	 * @param program  nama program (mis. reguler/karyawan); boleh {@code null}
	 * @param angkatan tahun angkatan sebagai teks, dicocokkan sebagai substring
	 * @return akun pendapatan yang paling spesifik cocok, atau {@code null} bila tidak ada pemetaan
	 *         sama sekali
	 * @see ItemBiayaPunyaAkun
	 */
	public Akun ambilAkun(Fakultas fakultas, Jurusan jurusan, String program, String angkatan) {
		Session session = HibernateUtil.currentSession();
		Akun akun = (Akun) (jurusan == null ? null
				: ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaAkun.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false));

		if (akun == null && fakultas != null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
							.add(Restrictions.eq("program", program)).setMaxResults(1),
					Akun.class, false);

		}

		if (akun == null && jurusan != null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("jurusan", jurusan))
							.add(Restrictions.isNull("program")).setMaxResults(1),
					Akun.class, false);
		}

		if (akun == null && fakultas != null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
							.add(Restrictions.isNull("program")).setMaxResults(1),
					Akun.class, false);
		}

		if (akun == null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.eq("program", program)).setMaxResults(1),
					Akun.class, false);
		}

		if (akun == null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
							.add(Restrictions.eq("program", program)).setMaxResults(1),
					Akun.class, false);
		}

		if (akun == null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
							.add(Restrictions.isNull("program")).setMaxResults(1),
					Akun.class, false);
		}

		if (akun == null) {
			akun = (Akun) ConstantValues.simpleObject(
					session.createCriteria(ItemBiayaPunyaAkun.class).setProjection(Projections.property("akun.id"))
							.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
									Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
							.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
							.add(Restrictions.isNull("program")).setMaxResults(1),
					Akun.class, false);
		}

		return akun;
	}

	/**
	 * Mencari akun <b>piutang</b> untuk item biaya ini berdasarkan konteks satu {@link Kegiatan}.
	 * Bentuk dan urutan penyimpulan konteksnya persis sama dengan {@link #ambilAkun(Kegiatan)};
	 * hanya tabel jembatan tujuannya yang berbeda ({@link ItemBiayaPunyaPiutang}).
	 *
	 * <p>Akun piutang dipakai mesin posting sebagai lawan jurnal saat tagihan diakui sebelum
	 * dibayar; pada beberapa alur posting ia juga menjadi <b>pengganti</b> akun pendapatan &mdash;
	 * pola {@code akunPiutang != null ? akunPiutang : itemBiaya.ambilAkun(kegiatan)} muncul di
	 * banyak tempat.</p>
	 *
	 * @param kegiatan kegiatan sumber konteks
	 * @return akun piutang yang cocok, atau {@code null}
	 * @see #ambilAkun(Kegiatan)
	 * @see #ambilPiutang(Fakultas, Jurusan, String, String)
	 */
	public Akun ambilPiutang(Kegiatan kegiatan) {
		if (kegiatan != null && kegiatan.getMahasiswa() != null) {
			return ambilPiutang(kegiatan.getMahasiswa().getJurusan().getFakultas(),
					kegiatan.getMahasiswa().getJurusan(), kegiatan.getMahasiswa().getProgram(),
					kegiatan.getMahasiswa().getTahunangkatan().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
			return ambilPiutang(kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdiLulus(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdi1() != null) {
			return ambilPiutang(kegiatan.getCalonMahasiswa().getProdi1().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdi1(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		}

		return null;
	}

	/**
	 * Mencari akun <b>piutang</b> untuk item biaya ini di tabel jembatan
	 * {@link ItemBiayaPunyaPiutang}, memakai algoritma berjenjang delapan tahap yang <b>identik</b>
	 * dengan {@link #ambilAkun(Fakultas, Jurusan, String, String)} &mdash; termasuk penyaring
	 * angkatan {@code ILIKE '%angkatan%' OR NULL OR ''} dan kelonggaran pada tahap 5&ndash;8.
	 * Penjelasan lengkap urutannya ada di sana dan sengaja tidak diulang di sini.
	 *
	 * <p><b>Perbedaan satu-satunya selain tabel:</b> seluruh badan method dibungkus
	 * {@code try/catch(Exception)}. Bila query gagal, method mengembalikan {@code null} &mdash;
	 * pemanggil tidak bisa membedakan "tidak ada pemetaan" dari "query gagal". Blok
	 * {@code catch}-nya sudah dilengkapi pencatatan
	 * {@code ais.common.ErrorAuditUtil.record(...)} oleh audit blok-catch-kosong (inisiatif
	 * terpisah), jadi kejadiannya tetap terekam meski tidak dilemparkan.</p>
	 *
	 * @param fakultas fakultas konteks; boleh {@code null}
	 * @param jurusan  jurusan/program studi konteks; boleh {@code null}
	 * @param program  nama program; boleh {@code null}
	 * @param angkatan tahun angkatan sebagai teks, dicocokkan sebagai substring
	 * @return akun piutang yang paling spesifik cocok; {@code null} bila tidak ada pemetaan
	 *         <b>atau</b> bila query melempar exception
	 * @see #ambilAkun(Fakultas, Jurusan, String, String)
	 * @see ItemBiayaPunyaPiutang
	 */
	public Akun ambilPiutang(Fakultas fakultas, Jurusan jurusan, String program, String angkatan) {
		Session session = HibernateUtil.currentSession();
		try {
			Akun akun = (Akun) (jurusan == null ? null
					: ConstantValues
							.simpleObject(
									session.createCriteria(ItemBiayaPunyaPiutang.class)
											.setProjection(Projections.property("akun.id"))
											.add(Restrictions.or(
													Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.isNull("angkatan"),
															Restrictions.eq("angkatan", ""))))
											.add(Restrictions.eq("itemBiaya", this))
											.add(Restrictions.eq("jurusan", jurusan))
											.add(Restrictions.eq("program", program)).setMaxResults(1),
									Akun.class, false));

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);

			}

			if (akun == null && jurusan != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPiutang.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}
			return akun;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ItemBiaya.java:586");
			// TODO: handle exception
		}

		return null;
	}

	/**
	 * Mencari akun <b>pendapatan diterima dimuka</b> untuk item biaya ini berdasarkan konteks satu
	 * {@link Kegiatan}. Bentuk dan urutan penyimpulan konteksnya persis sama dengan
	 * {@link #ambilAkun(Kegiatan)}; tabel jembatannya {@link ItemBiayaPunyaDibayarDimuka}.
	 *
	 * <p>Akun ini dipakai jalur posting cicilan yang uangnya sudah diterima tetapi tagihannya belum
	 * jatuh tempo &mdash; lihat {@code PostingCicilanDibayarDimukaMahasiswaAction}, yang menjadikan
	 * akun ini sebagai sisi debet dan akun piutang/pendapatan sebagai sisi kreditnya.</p>
	 *
	 * @param kegiatan kegiatan sumber konteks
	 * @return akun pendapatan diterima dimuka yang cocok, atau {@code null}
	 * @see #ambilAkun(Kegiatan)
	 * @see #ambilDibayarDimuka(Fakultas, Jurusan, String, String)
	 */
	public Akun ambilDibayarDimuka(Kegiatan kegiatan) {
		if (kegiatan != null && kegiatan.getMahasiswa() != null) {
			return ambilDibayarDimuka(kegiatan.getMahasiswa().getJurusan().getFakultas(),
					kegiatan.getMahasiswa().getJurusan(), kegiatan.getMahasiswa().getProgram(),
					kegiatan.getMahasiswa().getTahunangkatan().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
			return ambilDibayarDimuka(kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdiLulus(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdi1() != null) {
			return ambilDibayarDimuka(kegiatan.getCalonMahasiswa().getProdi1().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdi1(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		}

		return null;
	}

	/**
	 * Mencari akun <b>pendapatan diterima dimuka</b> untuk item biaya ini di tabel jembatan
	 * {@link ItemBiayaPunyaDibayarDimuka}, memakai algoritma berjenjang delapan tahap yang
	 * <b>identik</b> dengan {@link #ambilAkun(Fakultas, Jurusan, String, String)}; penjelasan
	 * lengkap urutan dan penyaring angkatannya ada di sana.
	 *
	 * <p>Sama seperti {@link #ambilPiutang(Fakultas, Jurusan, String, String)}, badan method
	 * dibungkus {@code try/catch(Exception)} sehingga kegagalan query menjadi {@code null}, bukan
	 * exception.</p>
	 *
	 * @param fakultas fakultas konteks; boleh {@code null}
	 * @param jurusan  jurusan/program studi konteks; boleh {@code null}
	 * @param program  nama program; boleh {@code null}
	 * @param angkatan tahun angkatan sebagai teks, dicocokkan sebagai substring
	 * @return akun pendapatan diterima dimuka yang paling spesifik cocok; {@code null} bila tidak ada
	 *         pemetaan atau bila query melempar exception
	 * @see #ambilAkun(Fakultas, Jurusan, String, String)
	 * @see ItemBiayaPunyaDibayarDimuka
	 */
	public Akun ambilDibayarDimuka(Fakultas fakultas, Jurusan jurusan, String program, String angkatan) {
		Session session = HibernateUtil.currentSession();
		try {
			Akun akun = (Akun) (jurusan == null ? null
					: ConstantValues
							.simpleObject(
									session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
											.setProjection(Projections.property("akun.id"))
											.add(Restrictions.or(
													Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.isNull("angkatan"),
															Restrictions.eq("angkatan", ""))))
											.add(Restrictions.eq("itemBiaya", this))
											.add(Restrictions.eq("jurusan", jurusan))
											.add(Restrictions.eq("program", program)).setMaxResults(1),
									Akun.class, false));

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);

			}

			if (akun == null && jurusan != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaDibayarDimuka.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			return akun;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ItemBiaya.java:695");
			// TODO: handle exception
		}

		return null;
	}

	/**
	 * Mencari akun <b>pendapatan denda</b> untuk item biaya ini berdasarkan konteks satu
	 * {@link Kegiatan}. Bentuk dan urutan penyimpulan konteksnya persis sama dengan
	 * {@link #ambilAkun(Kegiatan)}; tabel jembatannya {@link ItemBiayaPunyaPendapatanDenda}.
	 *
	 * <p>Dipanggil mesin posting hanya ketika transaksi memang mengandung denda keterlambatan
	 * &mdash; hasil {@link DetailBiaya#checkDenda} yang bukan nol. Pemetaannya dikelola dari layar
	 * akunting lewat {@code ais.action.master.akunting.helper.ItemBiayaPunyaDenda} (perhatikan:
	 * itu kelas <i>helper UI</i>, bukan entity; entity-nya
	 * {@link ItemBiayaPunyaPendapatanDenda}).</p>
	 *
	 * @param kegiatan kegiatan sumber konteks
	 * @return akun pendapatan denda yang cocok, atau {@code null}
	 * @see #ambilAkun(Kegiatan)
	 * @see #ambilPendapatanDenda(Fakultas, Jurusan, String, String)
	 */
	public Akun ambilPendapatanDenda(Kegiatan kegiatan) {
		if (kegiatan != null && kegiatan.getMahasiswa() != null) {
			return ambilPendapatanDenda(kegiatan.getMahasiswa().getJurusan().getFakultas(),
					kegiatan.getMahasiswa().getJurusan(), kegiatan.getMahasiswa().getProgram(),
					kegiatan.getMahasiswa().getTahunangkatan().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdiLulus() != null) {
			return ambilPendapatanDenda(kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdiLulus(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		} else if (kegiatan != null && kegiatan.getCalonMahasiswa() != null
				&& kegiatan.getCalonMahasiswa().getProdi1() != null) {
			return ambilPendapatanDenda(kegiatan.getCalonMahasiswa().getProdi1().getFakultas(),
					kegiatan.getCalonMahasiswa().getProdi1(), kegiatan.getCalonMahasiswa().getProgram(),
					kegiatan.getCalonMahasiswa().getTahun().toString());
		}

		return null;
	}

	/**
	 * Mencari akun <b>pendapatan denda</b> untuk item biaya ini di tabel jembatan
	 * {@link ItemBiayaPunyaPendapatanDenda}, memakai algoritma berjenjang delapan tahap yang
	 * <b>identik</b> dengan {@link #ambilAkun(Fakultas, Jurusan, String, String)}; penjelasan
	 * lengkap urutan dan penyaring angkatannya ada di sana.
	 *
	 * <p>Sama seperti {@link #ambilPiutang(Fakultas, Jurusan, String, String)} dan
	 * {@link #ambilDibayarDimuka(Fakultas, Jurusan, String, String)}, badan method dibungkus
	 * {@code try/catch(Exception)} sehingga kegagalan query menjadi {@code null}, bukan
	 * exception.</p>
	 *
	 * @param fakultas fakultas konteks; boleh {@code null}
	 * @param jurusan  jurusan/program studi konteks; boleh {@code null}
	 * @param program  nama program; boleh {@code null}
	 * @param angkatan tahun angkatan sebagai teks, dicocokkan sebagai substring
	 * @return akun pendapatan denda yang paling spesifik cocok; {@code null} bila tidak ada pemetaan
	 *         atau bila query melempar exception
	 * @see #ambilAkun(Fakultas, Jurusan, String, String)
	 * @see ItemBiayaPunyaPendapatanDenda
	 */
	public Akun ambilPendapatanDenda(Fakultas fakultas, Jurusan jurusan, String program, String angkatan) {
		Session session = HibernateUtil.currentSession();
		try {
			Akun akun = (Akun) (jurusan == null ? null
					: ConstantValues
							.simpleObject(
									session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
											.setProjection(Projections.property("akun.id"))
											.add(Restrictions.or(
													Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
													Restrictions.or(Restrictions.isNull("angkatan"),
															Restrictions.eq("angkatan", ""))))
											.add(Restrictions.eq("itemBiaya", this))
											.add(Restrictions.eq("jurusan", jurusan))
											.add(Restrictions.eq("program", program)).setMaxResults(1),
									Akun.class, false));

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);

			}

			if (akun == null && jurusan != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null && fakultas != null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.eq("fakultas", fakultas))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.eq("program", program)).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("jurusan"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			if (akun == null) {
				akun = (Akun) ConstantValues.simpleObject(session.createCriteria(ItemBiayaPunyaPendapatanDenda.class)
						.setProjection(Projections.property("akun.id"))
						.add(Restrictions.or(Restrictions.ilike("angkatan", angkatan, MatchMode.ANYWHERE),
								Restrictions.or(Restrictions.isNull("angkatan"), Restrictions.eq("angkatan", ""))))
						.add(Restrictions.eq("itemBiaya", this)).add(Restrictions.isNull("fakultas"))
						.add(Restrictions.isNull("program")).setMaxResults(1), Akun.class, false);
			}

			return akun;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/ItemBiaya.java:804");
			// TODO: handle exception
		}

		return null;
	}

	/**
	 * Apakah item ini dikenakan denda bila dibayar melewati tenggat ("Dikenakan denda jika terlambat
	 * membayar"). Mengaktifkannya memunculkan empat isian denda lain di layar master.
	 *
	 * <p><b>Ingat aturan pengutamaannya:</b> {@link DetailBiaya#checkDenda} mengenakan denda bila
	 * flag ini <i>atau</i> flag bernama sama di {@link JenisKegiatan} bernilai {@code true}, tetapi
	 * bila {@link JenisKegiatan} yang mengaktifkannya maka seluruh parameter dendanya diambil dari
	 * {@link JenisKegiatan} dan pengaturan di sini diabaikan.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code false} bila field {@code null} <b>tanpa</b> menulis balik
	 * ke field.</p>
	 *
	 * @return {@code true} bila item dikenakan denda keterlambatan; default {@code false}
	 */
	public Boolean getDendaJikaTerlambat() {
		return dendaJikaTerlambat == null ? false : dendaJikaTerlambat;
	}

	/**
	 * Mengisi penanda pengenaan denda keterlambatan.
	 *
	 * @param dendaJikaTerlambat {@code true} bila item dikenakan denda
	 */
	public void setDendaJikaTerlambat(Boolean dendaJikaTerlambat) {
		this.dendaJikaTerlambat = dendaJikaTerlambat;
	}

	/**
	 * Besaran denda dasar untuk satu kali kelipatan keterlambatan. Satuannya ditentukan
	 * {@link #getNilaiDendaDalamPersen()}: <b>persen dari nominal tagihan</b> bila {@code true},
	 * <b>rupiah tetap</b> bila {@code false}.
	 *
	 * <p>Meski namanya "prosentase", nilainya juga dipakai sebagai nominal rupiah pada mode nilai
	 * tetap &mdash; penamaannya sisa desain lama. Denda hanya dikenakan bila nilainya lebih besar
	 * dari nol.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code 0.0} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return besaran denda dasar; {@code 0.0} bila belum diisi
	 * @see DetailBiaya#checkDenda
	 */
	public Double getDefaultProsentaseDenda() {
		return defaultProsentaseDenda == null ? 0.0 : defaultProsentaseDenda;
	}

	/**
	 * Mengisi besaran denda dasar.
	 *
	 * @param defaultProsentaseDenda besaran denda (persen atau rupiah, lihat
	 *                               {@link #getNilaiDendaDalamPersen()})
	 */
	public void setDefaultProsentaseDenda(Double defaultProsentaseDenda) {
		this.defaultProsentaseDenda = defaultProsentaseDenda;
	}

	/**
	 * Daftar kode atau nama matakuliah pemicu, ditulis dalam satu kotak isian dan <b>dipisah tanda
	 * titik koma ({@code ;})</b> bila lebih dari satu &mdash; sesuai keterangan yang ditampilkan di
	 * layar master.
	 *
	 * <p>Hanya bermakna untuk dua mode penghitungan:
	 * {@link #DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU} dan
	 * {@link #DIKALI_SATU_JIKA_AMBIL_MK_TERTENTU_DAN_SEMESTER_SEBELUMNYA}. Di luar kedua mode itu,
	 * layar master menyembunyikan barisnya dan tidak ada kode yang membacanya. Penafsirnya
	 * {@code PembayaranNominalModifikasiHelper}, yang memeriksa apakah mahasiswa mengambil salah
	 * satu matakuliah dalam daftar ini.</p>
	 *
	 * <p>Mengembalikan {@code ""} (bukan {@code null}) bila kosong, dan hasilnya di-{@code trim}
	 * &mdash; sehingga pemanggil bisa langsung menulis {@code getNamaMatakuliah().trim().isEmpty()}
	 * tanpa penjagaan null.</p>
	 *
	 * @return daftar kode/nama matakuliah dipisah titik koma; {@code ""} bila belum diisi
	 */
	public String getNamaMatakuliah() {
		return namaMatakuliah == null ? "" : namaMatakuliah.trim();
	}

	/**
	 * Mengisi daftar kode/nama matakuliah pemicu. Nilai disimpan apa adanya (tanpa {@code trim});
	 * pemisahnya harus titik koma.
	 *
	 * @param namaMatakuliah daftar kode/nama matakuliah dipisah titik koma
	 */
	public void setNamaMatakuliah(String namaMatakuliah) {
		this.namaMatakuliah = namaMatakuliah;
	}

	/**
	 * Jenis/kanal pembayaran default untuk item ini ({@link JenisPembayaran}), yang membawa serta
	 * akun kas/bank penampungnya.
	 *
	 * <p><b>Praktis tidak terpakai:</b> baris "Jenis Pembayaran" di layar master dibuat lalu
	 * langsung di-{@code setVisible(false)}, jadi operator tidak pernah bisa mengisinya lewat UI.
	 * Relasinya tetap dipetakan agar data warisan tidak hilang.</p>
	 *
	 * <p>Relasi {@code @ManyToOne} lazy ke kolom {@code jenis_pembayaran}; hasil
	 * {@link GeneralValueObject#check(Object)} ditugaskan kembali ke field mengikuti pola standar
	 * getter relasi AIS.</p>
	 *
	 * @return jenis pembayaran default, atau {@code null}
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_pembayaran")
	public JenisPembayaran getJenisPembayaran() {
		jenisPembayaran = check(jenisPembayaran);
		return jenisPembayaran;
	}

	/**
	 * Mengisi jenis/kanal pembayaran default.
	 *
	 * @param jenisPembayaran jenis pembayaran, boleh {@code null}
	 */
	public void setJenisPembayaran(JenisPembayaran jenisPembayaran) {
		this.jenisPembayaran = jenisPembayaran;
	}

	/**
	 * Apakah item ini dicetak sebagai baris tersendiri di surat tagihan mahasiswa.
	 *
	 * <p><b>Default {@code true}</b> &mdash; item lama yang kolomnya masih {@code NULL} tetap
	 * tercetak. Dipakai {@code ais.action.report.CommonReportHelper} saat menyusun surat tagihan,
	 * baik untuk baris {@link DetailBiaya} maupun baris angsuran
	 * {@link PengaturanPembayaranBulanan}. Mematikannya tidak membatalkan tagihannya &mdash; item
	 * tetap ditagih, hanya tidak ditampilkan di surat.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code true} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return {@code true} bila item dicetak di surat tagihan; default {@code true}
	 */
	public Boolean getDitampilkanDiSuratTagihan() {
		return ditampilkanDiSuratTagihan == null ? true : ditampilkanDiSuratTagihan;
	}

	/**
	 * Mengisi penanda tampil di surat tagihan.
	 *
	 * @param ditampilkanDiSuratTagihan {@code true} bila item dicetak di surat tagihan
	 */
	public void setDitampilkanDiSuratTagihan(Boolean ditampilkanDiSuratTagihan) {
		this.ditampilkanDiSuratTagihan = ditampilkanDiSuratTagihan;
	}

	/**
	 * Apakah tagihan bulanan item ini diberi label "Angsuran 1", "Angsuran 2", &hellip; alih-alih
	 * label bulan.
	 *
	 * <p>Murni kosmetik: dibaca {@link PengaturanPembayaranBulanan} saat menyusun teks keterangan
	 * baris angsuran. Tidak mengubah jadwal, nominal, maupun jumlah angsurannya.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code false} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return {@code true} bila memakai istilah "Angsuran"; default {@code false}
	 */
	public Boolean getMenggunakanIstilahBayarAngsuran() {
		return menggunakanIstilahBayarAngsuran == null ? false : menggunakanIstilahBayarAngsuran;
	}

	/**
	 * Mengisi pemilih istilah angsuran pada tagihan bulanan.
	 *
	 * @param menggunakanIstilahBayarAngsuran {@code true} bila memakai istilah "Angsuran"
	 */
	public void setMenggunakanIstilahBayarAngsuran(Boolean menggunakanIstilahBayarAngsuran) {
		this.menggunakanIstilahBayarAngsuran = menggunakanIstilahBayarAngsuran;
	}

	/**
	 * Apakah <b>mahasiswa</b> boleh mencicil item ini sendiri lewat portal ("Boleh diangsur oleh
	 * mahasiswa").
	 *
	 * <p>Dipakai layar daftar ulang untuk mengunci tombol/kotak cicilan. Perhatikan pola
	 * pemanggilannya yang khas: kondisinya hampir selalu dipakai dalam bentuk negatif
	 * ({@code && !itemBiaya.getMahasiswaBolehMencicilkan()}) untuk <i>menonaktifkan</i> kendali UI,
	 * berpasangan dengan {@link #getAdminBolehMencicilkan()} untuk peran petugas.</p>
	 *
	 * <p><b>Default {@code true}</b> &mdash; item lama otomatis boleh dicicil. Bentuk aman: tidak
	 * menulis balik ke field.</p>
	 *
	 * @return {@code true} bila mahasiswa boleh mencicil; default {@code true}
	 */
	public Boolean getMahasiswaBolehMencicilkan() {
		return mahasiswaBolehMencicilkan == null ? true : mahasiswaBolehMencicilkan;
	}

	/**
	 * Mengisi izin mencicil dari sisi mahasiswa.
	 *
	 * @param mahasiswaBolehMencicilkan {@code true} bila mahasiswa boleh mencicil
	 */
	public void setMahasiswaBolehMencicilkan(Boolean mahasiswaBolehMencicilkan) {
		this.mahasiswaBolehMencicilkan = mahasiswaBolehMencicilkan;
	}

	/**
	 * Apakah <b>petugas admin/keuangan</b> boleh mencatat cicilan untuk item ini ("Boleh diangsur
	 * oleh admin / keuangan"). Pasangan {@link #getMahasiswaBolehMencicilkan()} untuk sisi petugas;
	 * keduanya diperiksa terpisah sehingga item bisa saja tidak boleh dicicil mahasiswa tetapi tetap
	 * boleh dicicilkan petugas di loket.
	 *
	 * <p><b>Default {@code true}</b>. Bentuk aman: tidak menulis balik ke field.</p>
	 *
	 * @return {@code true} bila admin/keuangan boleh mencicilkan; default {@code true}
	 */
	public Boolean getAdminBolehMencicilkan() {
		return adminBolehMencicilkan == null ? true : adminBolehMencicilkan;
	}

	/**
	 * Mengisi izin mencicil dari sisi admin/keuangan.
	 *
	 * @param adminBolehMencicilkan {@code true} bila admin/keuangan boleh mencicilkan
	 */
	public void setAdminBolehMencicilkan(Boolean adminBolehMencicilkan) {
		this.adminBolehMencicilkan = adminBolehMencicilkan;
	}

	/**
	 * Semester terendah tempat item ini seharusnya berlaku ("Minimal Semester"), berpasangan dengan
	 * {@link #getMaxSmt()}.
	 *
	 * <p><b>Bukan penyaring.</b> Di luar layar master, rentang ini hanya dipakai
	 * {@code DaftarUlangMahasiswaBaruAction}/{@code DaftarUlangMahasiswaLamaAction} untuk memunculkan
	 * teks peringatan bahwa tagihan berada di luar rentang; tagihannya sendiri tetap dibuat dan tetap
	 * harus dibayar. Jangan mengandalkannya sebagai kontrol.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code 0} bila field {@code null} <b>tanpa</b> menulis balik
	 * &mdash; berbeda dari {@link #getMaxSmt()} yang menulis. Field-nya sendiri sudah diinisialisasi
	 * {@code 0}.</p>
	 *
	 * @return semester minimal; {@code 0} bila belum diisi
	 */
	public Integer getMinSmt() {
		return minSmt == null ? 0 : minSmt;
	}

	/**
	 * Mengisi semester terendah berlakunya item.
	 *
	 * @param minSmt semester minimal
	 */
	public void setMinSmt(Integer minSmt) {
		this.minSmt = minSmt;
	}

	/**
	 * Semester tertinggi tempat item ini seharusnya berlaku ("Maksimal Semester"), berpasangan
	 * dengan {@link #getMinSmt()}. Sama seperti pasangannya, <b>hanya dipakai untuk peringatan</b>,
	 * bukan untuk menyaring pembuatan tagihan.
	 *
	 * <p><b>Getter ini menulis:</b> bila field {@code null}, ia menuliskan {@code 30} ke field
	 * sebelum mengembalikannya &mdash; tidak konsisten dengan {@link #getMinSmt()} yang memakai
	 * bentuk aman. Field-nya sendiri sudah diinisialisasi {@code 30}, jadi penulisan ini hanya
	 * terjadi pada baris warisan yang kolomnya {@code NULL}.</p>
	 *
	 * @return semester maksimal; {@code 30} bila belum diisi
	 */
	public Integer getMaxSmt() {
		if (maxSmt == null) {
			maxSmt = 30;
		}
		return maxSmt;
	}

	/**
	 * Mengisi semester tertinggi berlakunya item.
	 *
	 * @param maxSmt semester maksimal
	 */
	public void setMaxSmt(Integer maxSmt) {
		this.maxSmt = maxSmt;
	}

	/**
	 * Satuan besaran denda: {@code true} berarti {@link #getDefaultProsentaseDenda()} dibaca sebagai
	 * <b>persen dari nominal tagihan</b>, {@code false} berarti dibaca sebagai <b>rupiah tetap</b>
	 * ("Denda dalam persen (jika tidak dipilih dalam nilai fix)").
	 *
	 * <p><b>Default {@code true}</b>. Ingat pengutamaan {@link JenisKegiatan}: bila jenis kegiatan
	 * mengaktifkan dendanya sendiri, satuan yang dipakai adalah milik jenis kegiatan, bukan yang
	 * ini. Bentuk aman: tidak menulis balik ke field.</p>
	 *
	 * @return {@code true} bila denda dalam persen; default {@code true}
	 * @see DetailBiaya#checkDenda
	 */
	public Boolean getNilaiDendaDalamPersen() {
		return nilaiDendaDalamPersen == null ? true : nilaiDendaDalamPersen;
	}

	/**
	 * Mengisi satuan besaran denda.
	 *
	 * @param nilaiDendaDalamPersen {@code true} bila denda dalam persen, {@code false} bila rupiah
	 */
	public void setNilaiDendaDalamPersen(Boolean nilaiDendaDalamPersen) {
		this.nilaiDendaDalamPersen = nilaiDendaDalamPersen;
	}

	/**
	 * Panjang satu periode kelipatan denda dalam hari ("Denda akan berlipat jika terlambat dalam
	 * hari").
	 *
	 * <p>Perhitungannya di {@link DetailBiaya#checkDenda}:
	 * {@code jumlahKali = (jumlahHariTerlambat - 1) / kelipatan}, lalu denda dasar dikalikan
	 * {@code jumlahKali} dan dibatasi {@link #getMaksimalBerlipatTerlambaHari()}. Nilai {@code 0}
	 * berarti tidak ada pelipatan &mdash; denda dikenakan sekali saja, sebesar nilai dasarnya.</p>
	 *
	 * <p>Perhatikan konsekuensi pembagian bulat itu: dengan kelipatan {@code 30}, keterlambatan
	 * 1&ndash;30 hari menghasilkan {@code jumlahKali = 0}, artinya <b>dendanya nol</b>. Denda baru
	 * muncul pada hari ke-31.</p>
	 *
	 * <p><b>Salah eja yang tidak bisa diperbaiki:</b> nama properti kehilangan huruf {@code t}
	 * ("Terlamba"), dan karena properti ini tidak dianotasi {@code @Column} sementara naming
	 * strategy memakai nama properti apa adanya, nama kolom di database ikut salah eja.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code 0} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return panjang periode kelipatan denda dalam hari; {@code 0} bila tidak ada pelipatan
	 * @see DetailBiaya#checkDenda
	 */
	public Integer getDendaAkanBerlipatTerlambaHari() {
		return dendaAkanBerlipatTerlambaHari == null ? 0 : dendaAkanBerlipatTerlambaHari;
	}

	/**
	 * Mengisi panjang periode kelipatan denda dalam hari.
	 *
	 * @param dendaAkanBerlipatTerlambaHari panjang periode dalam hari; {@code 0} untuk tanpa
	 *                                      pelipatan
	 */
	public void setDendaAkanBerlipatTerlambaHari(Integer dendaAkanBerlipatTerlambaHari) {
		this.dendaAkanBerlipatTerlambaHari = dendaAkanBerlipatTerlambaHari;
	}

	/**
	 * Batas atas jumlah kelipatan denda ("Maksimal jumlah kelipatan"), supaya denda tidak tumbuh tak
	 * terbatas pada tunggakan yang sangat lama.
	 *
	 * <p>Di {@link DetailBiaya#checkDenda}, batas ini hanya berlaku bila nilainya lebih besar dari
	 * nol; nilai {@code 0} berarti <b>tanpa batas</b>. Batas juga hanya relevan bila
	 * {@link #getDendaAkanBerlipatTerlambaHari()} bukan nol.</p>
	 *
	 * <p>Ikut menanggung salah eja "Terlamba" seperti pasangannya.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code 0} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return batas jumlah kelipatan; {@code 0} berarti tanpa batas
	 * @see DetailBiaya#checkDenda
	 */
	public Integer getMaksimalBerlipatTerlambaHari() {
		return maksimalBerlipatTerlambaHari == null ? 0 : maksimalBerlipatTerlambaHari;
	}

	/**
	 * Mengisi batas atas jumlah kelipatan denda.
	 *
	 * @param maksimalBerlipatTerlambaHari batas jumlah kelipatan; {@code 0} untuk tanpa batas
	 */
	public void setMaksimalBerlipatTerlambaHari(Integer maksimalBerlipatTerlambaHari) {
		this.maksimalBerlipatTerlambaHari = maksimalBerlipatTerlambaHari;
	}

	/**
	 * Niat konfigurasi "item ini tidak ditagihkan pada semester ganjil" ("Tidak ditagih di semester
	 * ganjil").
	 *
	 * <p><b>Peringatan: flag ini tidak ditegakkan di mana pun.</b> Penelusuran seluruh pohon sumber
	 * hanya menemukan dua pemakai, keduanya di {@code ItemBiayaAction}: satu mengisi centang di form,
	 * satu menyimpannya kembali. Tidak ada satu pun kode pembuatan tagihan yang membacanya. Operator
	 * yang mencentangnya akan mengira item tidak ditagih di semester ganjil, padahal tetap ditagih.
	 * Jangan dijadikan andalan sebelum penegakannya benar-benar ditambahkan (dan penambahan itu
	 * berpotensi mengubah tagihan berjalan &mdash; perlu keputusan bisnis, bukan sekadar
	 * perbaikan teknis).</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code false} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return {@code true} bila dikonfigurasi tidak ditagih di semester ganjil; default {@code false}
	 */
	public Boolean getTidakDitagihDiSmtGanjil() {
		return tidakDitagihDiSmtGanjil == null ? false : tidakDitagihDiSmtGanjil;
	}

	/**
	 * Mengisi niat konfigurasi "tidak ditagih di semester ganjil". Lihat peringatan di
	 * {@link #getTidakDitagihDiSmtGanjil()}: nilainya tersimpan tetapi belum berpengaruh apa pun.
	 *
	 * @param tidakDitagihDiSmtGanjil {@code true} bila item tidak boleh ditagih di semester ganjil
	 */
	public void setTidakDitagihDiSmtGanjil(Boolean tidakDitagihDiSmtGanjil) {
		this.tidakDitagihDiSmtGanjil = tidakDitagihDiSmtGanjil;
	}

	/**
	 * Niat konfigurasi "item ini tidak ditagihkan pada semester genap" ("Tidak ditagih di semester
	 * genap"). Kembaran {@link #getTidakDitagihDiSmtGanjil()}, dan <b>sama-sama tidak ditegakkan di
	 * mana pun</b> &mdash; lihat peringatan lengkapnya di sana.
	 *
	 * <p>Bentuk aman: mengembalikan {@code false} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return {@code true} bila dikonfigurasi tidak ditagih di semester genap; default {@code false}
	 */
	public Boolean getTidakDitagihDiSmtGenap() {
		return tidakDitagihDiSmtGenap == null ? false : tidakDitagihDiSmtGenap;
	}

	/**
	 * Mengisi niat konfigurasi "tidak ditagih di semester genap". Lihat peringatan di
	 * {@link #getTidakDitagihDiSmtGanjil()}.
	 *
	 * @param tidakDitagihDiSmtGenap {@code true} bila item tidak boleh ditagih di semester genap
	 */
	public void setTidakDitagihDiSmtGenap(Boolean tidakDitagihDiSmtGenap) {
		this.tidakDitagihDiSmtGenap = tidakDitagihDiSmtGenap;
	}

	/**
	 * Sumber tanggal tagihan item ini ("Tanggal Tagihan Mengikuti Rencana Tahun Akademik atau
	 * Gelombang Pendaftaran").
	 *
	 * <p>Bila {@code true}, {@link DetailKegiatan} mengambil tanggal tagihan dari rencana tahun
	 * akademik / gelombang pendaftaran alih-alih dari tanggal default yang tertulis di setting
	 * biaya. Ini satu-satunya flag "rentang/tanggal" di kelas ini yang <b>benar-benar dipakai</b> di
	 * luar layar master &mdash; berbeda dari
	 * {@link #getTidakDitagihDiSmtGanjil()}/{@link #getTidakDitagihDiSmtGenap()} dan
	 * {@link #getMinSmt()}/{@link #getMaxSmt()}.</p>
	 *
	 * <p>Bentuk aman: mengembalikan {@code false} bila field {@code null} tanpa menulis balik.</p>
	 *
	 * @return {@code true} bila tanggal tagihan mengikuti rencana tahun akademik/gelombang;
	 *         default {@code false}
	 * @see DetailKegiatan
	 */
	public Boolean getTanggalTagihanMengikutiRencanaTahunAkademik() {
		return tanggalTagihanMengikutiRencanaTahunAkademik == null ? false
				: tanggalTagihanMengikutiRencanaTahunAkademik;
	}

	/**
	 * Mengisi sumber tanggal tagihan.
	 *
	 * @param tanggalTagihanMengikutiRencanaTahunAkademik {@code true} bila tanggal tagihan mengikuti
	 *                                                    rencana tahun akademik/gelombang
	 *                                                    pendaftaran
	 */
	public void setTanggalTagihanMengikutiRencanaTahunAkademik(Boolean tanggalTagihanMengikutiRencanaTahunAkademik) {
		this.tanggalTagihanMengikutiRencanaTahunAkademik = tanggalTagihanMengikutiRencanaTahunAkademik;
	}

}
