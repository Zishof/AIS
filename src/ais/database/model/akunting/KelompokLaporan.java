package ais.database.model.akunting;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>KelompokLaporan &mdash; satu BARIS laporan keuangan, sekaligus SIMPUL TENGAH hierarkinya</h3>
 *
 * <p><b>Peran sesungguhnya (TERVERIFIKASI dari isi berkas ini dan dari seluruh pemakainya, bukan
 * asumsi).</b> Entity ini memetakan tabel <code>akunting.kelompok_laporan</code> dan mewakili
 * <b>satu baris cetak</b> pada laporan keuangan &mdash; misalnya baris "Kas dan Setara Kas" di
 * dalam seksi "Aktiva" pada laporan "Neraca". Ia bukan katalog datar seperti tetangganya, dan bukan
 * pula akar sebuah pohon: ia adalah <b>simpul tengah</b> yang menyatukan dua sumbu klasifikasi yang
 * <b>saling ortogonal</b> (bukan berjenjang) lewat dua kolom FK yang keduanya dideklarasikan di
 * berkas ini:</p>
 * <ol>
 *   <li><b>{@link ais.database.model.akunting.JenisLaporan}</b> (kolom <code>jenis_laporan</code>)
 *       &mdash; sumbu "laporan yang mana": Neraca, Rugi Laba, Arus Kas.</li>
 *   <li><b>{@link ais.database.model.akunting.MasterGrupLaporan}</b> (kolom
 *       <code>master_grup_laporan</code>) &mdash; sumbu "seksi/judul blok di dalam laporan itu":
 *       Aktiva, Kewajiban, Pendapatan, Operasional, Investasi.</li>
 * </ol>
 *
 * <h3>Rantai cetak lengkap &mdash; ringkasan untuk pembaca masa depan</h3>
 * <p>Struktur laporan keuangan AIS dirakit dari empat tabel. Urutan bacanya:</p>
 * <pre>
 *   JenisLaporan            (laporan mana: Neraca / Rugi Laba / Arus Kas)
 *     &rarr; MasterGrupLaporan   (judul blok/seksi: Aktiva, Kewajiban, ...)
 *          &rarr; KelompokLaporan       &lt;&lt;&lt; BERKAS INI: satu BARIS laporan
 *               &rarr; KelompokLaporanPunyaAkun  (tabel jembatan)
 *                    &rarr; Akun         (rincian bagan akun / Chart of Accounts)
 * </pre>
 * <p>Berkas ini menyumbang tiga hal ke rantai tersebut: <b>label baris</b> (tiga tingkat teks
 * {@code keterangan}/{@code keterangan1}/{@code keterangan2}), <b>nomor urut cetak</b>
 * ({@code urut}, bertipe {@code Double} sehingga baris dapat disisipkan di antara dua baris lama
 * tanpa menomori ulang seluruhnya), serta dua bendera tampilan ({@code aktif} dan
 * {@code tampilkanAkunRinci}). <b>Tidak ada satu pun kolom nominal, tarif, tanda (+/&minus;),
 * maupun rumus di entity ini</b> &mdash; seluruh angka datang dari agregasi jurnal
 * ({@code akunting.transaksi}) atas akun-akun yang ditempelkan lewat
 * {@code KelompokLaporanPunyaAkun}. Karena itu salah konfigurasi di sini tidak pernah membuat angka
 * jadi salah hitung, tetapi <b>bisa membuat sebuah akun hilang sama sekali dari laporan resmi</b>:
 * akun yang tidak tertempel ke baris manapun tidak ikut terjumlah di Neraca/Laba Rugi/Arus Kas.</p>
 *
 * <h3>Kedua FK bersifat {@code nullable} &mdash; verifikasi dari sisi entity ini</h3>
 * <p>Klaim ini diperiksa ulang langsung dari anotasi di berkas ini dan <b>terbukti benar untuk
 * keduanya</b>: {@code @JoinColumn(name = "jenis_laporan", nullable = true)} dan
 * {@code @JoinColumn(name = "master_grup_laporan", nullable = true)}. Basis data karena itu
 * mengizinkan <b>baris laporan yatim</b>. Yang perlu dipahami adalah bahwa lapisan-lapisan di
 * atasnya berbeda pendapat soal ini:</p>
 * <ul>
 *   <li><b>Kedua layar ZK menolak null.</b> {@code KelompokLaporanAction.onSave} dan
 *       {@code KelompokLaporanDanDetailAction.onSave} sama-sama menampilkan peringatan dan
 *       {@code return false} bila Jenis Laporan atau Master Grup Laporan belum dipilih. Impor Excel
 *       di layar kedua pun hanya memproses baris ketika <em>kedua</em> objek berhasil di-resolve
 *       ({@code if (grupLaporan != null && jenisLaporan != null)}).</li>
 *   <li><b>Jalur REST tidak.</b> {@code ais.action.servlet.api.PemetaanAkunHelper} membuat baris
 *       baru hanya dengan {@code setKeterangan} + {@code setJenisLaporan} + {@code setAktif} +
 *       {@code setUrut} &mdash; {@code masterGrupLaporan} <b>sengaja dibiarkan null</b>. Inilah
 *       satu-satunya penghasil baris yatim yang terverifikasi di repo.</li>
 *   <li><b>Perender menampung yatim, tetapi tidak seragam.</b>
 *       {@code LaporanKeuanganCoaHelper.susun()} mengelompokkan baris ber-{@code masterGrupLaporan}
 *       null ke dalam grup semu ber-id <code>-1</code> berlabel <b>"Lainnya"</b>, sehingga barisnya
 *       tetap tercetak. Sebaliknya {@code LaporanAkuntingSaldoBulanMaster} dan
 *       {@code NewUiLaporanAkuntingSaldoBulanController} memasang
 *       {@code Restrictions.isNotNull("masterGrupLaporan")}, jadi baris yatim tidak pernah muncul
 *       sebagai pilihan di sana.</li>
 * </ul>
 *
 * <p><b>Kuirk terkait yang mudah menjebak.</b> Layar "Kelompok Laporan"
 * ({@code KelompokLaporanAction.onSearchDefault}) membangun kriterianya dengan
 * {@code createAlias("masterGrupLaporan", "masterGrupLaporan")} <b>tanpa menyebut tipe join</b>,
 * dan bawaan Hibernate Criteria untuk itu adalah <b>INNER JOIN</b>. Akibatnya baris yatim buatan
 * jalur REST <b>tidak pernah tampil</b> di layar tersebut &mdash; tidak bisa dilihat, diubah,
 * maupun dihapus dari sana. Layar saudaranya, "Kelompok Laporan dan Detail"
 * ({@code KelompokLaporanDanDetailAction.initCriteria}), memakai
 * {@code Criteria.LEFT_JOIN} secara eksplisit sehingga baris yatim <b>tampil</b> di sana. Jadi jalan
 * pulih tetap ada, tetapi hanya lewat satu layar tertentu; operator yang membuka layar yang salah
 * akan menyimpulkan barisnya "tidak ada" padahal ikut tercetak di laporan HTML.</p>
 *
 * <h3>Tidak ada kolom tenant &mdash; katalog ini GLOBAL</h3>
 * <p>Verifikasi cakupan: entity ini <b>tidak memiliki kolom sekolah, yayasan, maupun satuan kerja
 * sama sekali</b>, dan tidak satu pun pemakainya memasang penyaring tenant. Ini bukan kasus
 * "fail-open bersyarat" (penyaring yang ada tetapi gagal menyala), melainkan <b>ketiadaan kolom</b>
 * &mdash; senapas dengan {@code MasterGrupLaporan}, {@code Closing}, dan
 * {@code ProsesTransferStandingInstruction} pada modul yang sama. Konsekuensinya konkret: satu
 * operator yang mengubah nomor urut, mematikan bendera {@code aktif}, atau mengganti seksi sebuah
 * baris mengubah tata letak laporan keuangan <em>seluruh</em> tenant pada satu instalasi.</p>
 *
 * <h3>Gerbang hak akses: rapi di ZK, TIDAK ADA di dua jalur lain</h3>
 * <p>Perbedaan ini penting dan sudah diverifikasi baris per baris:</p>
 * <ul>
 *   <li><b>Layar ZK &mdash; digerbangi dengan benar.</b> Keduanya memanggil
 *       {@code Common.doCheckSecurity()} di {@code doBeforeCompose}, memeriksa
 *       {@code CommonPrivilages.READ} di {@code doAfterCompose} (gagal &rarr;
 *       {@code Common.goLogoff()}), lalu menurunkan visibilitas tombol Tambah/Ubah/Hapus dari
 *       {@code CREATE}/{@code UPDATE}/{@code DELETE}. Tombol "Upload Akun" bahkan menuntut
 *       ketiganya sekaligus. Perlu dicatat bahwa {@code checkPrevilages()} membaca atribut sesi
 *       {@code currentMenu} yang <b>tidak di-resolve ulang untuk halaman yang di-<em>include</em></b>
 *       &mdash; pola pewarisan hak lewat menu induk yang sudah berulang kali ditemukan di repo ini
 *       tetap berlaku, jadi hak yang diperiksa bisa saja hak menu lain yang kebetulan sedang aktif
 *       di sesi.</li>
 *   <li><b>Jalur REST {@code PosApi} &mdash; NOL gerbang peran.</b> Cabang
 *       {@code action.startsWith("pemetaan_akun_")} memanggil {@code PemetaanAkunHelper.proses}
 *       secara langsung; {@code proses()} menerima {@code Tbmuser} tetapi <b>tidak pernah
 *       memakainya</b>, dan {@code jalankan()} bahkan tidak menerimanya. Kuncinya
 *       (<code>pemetaan_akun</code>) juga <b>tidak terdaftar</b> di
 *       {@code EbisnisMenuKatalog.KUNCI_DEFAULT_NONAKTIF}. Ini <em>berbeda</em> dari pola fail-open
 *       {@code bolehAksi()} yang sudah dikenal di modul akunting: di sana ada gerbang yang salah
 *       arah, di sini tidak ada gerbang sama sekali. Aksi
 *       <code>pemetaan_akun_terapkan</code> MENULIS &mdash; ia membuat baris {@code KelompokLaporan}
 *       baru dan menempelkan akun massal.</li>
 *   <li><b>Halaman diagnosa {@code webapp/WEB-INF/baru/modul/kantin/cek_pemetaan_akun.jsp} &mdash;
 *       juga NOL gerbang peran.</b> Ia hanya menolak pengunjung anonim
 *       ({@code uCP == null || uCP.getUserId() == null}), lalu menerima parameter
 *       <code>?aksi=petakan&amp;akunId=..&amp;kelompokId=..</code> dan menyimpan pemetaan baru.
 *       Komentarnya sendiri sudah menuliskan "MENGUBAH STRUKTUR LAPORAN GLOBAL".</li>
 * </ul>
 *
 * <h3>Pengelompokan method di berkas ini</h3>
 * <ol>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Jejak audit (diulang di hampir semua entity repo ini):</b> {@link #getOleh()},
 *       {@link #setOleh(String)}, {@link #getOlehId()}, {@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}, {@link #setTanggal_dirubah(Date)}, ditambah kait
 *       {@code @PreUpdate} {@code onUpdate()}.</li>
 *   <li><b>Label baris (tiga tingkat):</b> {@link #getKeterangan()}, {@link #getKeterangan1()},
 *       {@link #getKeterangan2()} beserta setter-nya.</li>
 *   <li><b>Penempatan dalam hierarki:</b> {@link #getJenisLaporan()},
 *       {@link #getMasterGrupLaporan()} beserta setter-nya.</li>
 *   <li><b>Tampilan/urutan:</b> {@link #getUrut()}, {@link #getAktif()},
 *       {@link #getTampilkanAkunRinci()} beserta setter-nya.</li>
 * </ol>
 * <p>Tidak ada method bisnis lain: entity ini <b>tidak</b> mempunyai {@code reloadDefault()},
 * {@code compareTo()} sendiri, {@code toString()} sendiri, konstanta nama seksi, maupun query
 * internal. Seluruh perilaku pengurutan diwarisi dari {@link GeneralValueObject#compareTo}, yang
 * karena entity ini tidak mengisi {@code nomorUrut}/{@code nim}/{@code nama} induk akan jatuh ke
 * perbandingan {@code getKeterangan()} &mdash; <b>bukan</b> ke kolom {@code urut} yang justru
 * dipakai laporan. Kode yang butuh urutan cetak harus mengurutkan eksplisit dengan
 * {@code order by urut}, seperti yang memang dilakukan semua pemakainya.</p>
 *
 * <h3>Catatan pewarisan: {@link GeneralValueObject} BUKAN {@code @MappedSuperclass}</h3>
 * <p>Kelas induk hanyalah POJO abstrak biasa (tidak beranotasi {@code @Entity} maupun
 * {@code @MappedSuperclass}), sehingga <b>Hibernate tidak memetakan properti induknya</b>. Field
 * yang dideklarasikan ulang di sini &mdash; {@code id}, {@code keterangan}, {@code oleh},
 * {@code olehId}, {@code tanggal_dirubah} &mdash; <b>bukan bug dan bukan duplikasi ceroboh,
 * melainkan keharusan teknis</b>: tanpa deklarasi ulang, kolomnya tidak akan ada di skema. Efek
 * sampingnya: {@code KelompokLaporan.keterangan} bersifat <em>shadowing</em> atas
 * {@code GeneralValueObject.keterangan}, dan kedua getter berperilaku berbeda (yang di sini
 * mem-{@code trim()}, yang di induk tidak). Selama akses dilakukan lewat getter yang tepat, hal ini
 * tidak menimbulkan masalah &mdash; tetapi kode yang memanggil {@code getKeterangan()} lewat
 * referensi bertipe {@link GeneralValueObject} tetap menjalankan versi kelas ini karena Java
 * memanggil method secara virtual. Lihat {@link ais.database.model.GeneralValueObject} untuk
 * penjelasan lengkap mekanisme {@code check()}/{@code resolveLazy()} yang dipakai kedua getter
 * relasi di bawah.</p>
 *
 * <p><b>Audit revisi.</b> Kelas ini beranotasi {@code @Audited} (Hibernate Envers), sehingga setiap
 * versi baris disalin ke tabel <code>kelompok_laporan_aud</code>. Layar daftar memanfaatkan ini
 * lewat {@code RevisiHelper.createNewRevisi(KelompokLaporan.class, ...)} untuk menampilkan riwayat
 * perubahan. Perlu diingat bahwa gerbang tabel revisi bukan hak menu, melainkan daftar id/role pada
 * konfigurasi <code>boleh_lihat_revisi</code>.</p>
 *
 * <p><b>Penyimpanan &amp; pemuatan.</b> Melalui {@code ais.database.dao.akunting.KelompokLaporanDao}
 * (implementasinya kosong, seluruhnya diwarisi {@code GenericHibernateDao}). Anotasi
 * {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya menulis kolom yang benar-benar
 * berubah &mdash; relevan karena kedua getter relasi di bawah menugaskan ulang field-nya saat
 * dibaca.</p>
 *
 * @see ais.database.model.akunting.JenisLaporan
 * @see ais.database.model.akunting.MasterGrupLaporan
 * @see ais.database.model.akunting.KelompokLaporanPunyaAkun
 * @see ais.database.model.akunting.Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "akunting", name = "kelompok_laporan")

public class KelompokLaporan extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi Java.
	 *
	 * <p>Nilai {@code 2463821577548439808L} ini <b>tidak unik</b>: ia identik dengan
	 * {@code JenisLaporan}, {@code MasterGrupLaporan}, {@code KelompokLaporanPunyaAkun}, dan
	 * sejumlah entity lain di paket ini &mdash; sisa dari pembangkitan hbm2java 2010 yang menyalin
	 * berkas berulang kali. Karena entity ini tidak pernah dikirim antar-JVM (hanya disimpan di
	 * sesi ZK dalam satu proses), kekembaran itu tidak menimbulkan masalah praktis. Jangan diubah
	 * tanpa alasan kuat.</p>
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel <code>akunting.kelompok_laporan</code>; diisi otomatis oleh basis data. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris ini (jejak audit). */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah disentuh mekanisme audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Perilaku non-obvious:</b> nilai {@code null} maupun string kosong/spasi
	 * <b>diabaikan diam-diam</b> ({@code return} lebih awal), sehingga jejak audit yang sudah
	 * terisi <b>tidak dapat dikosongkan</b> lewat setter ini. Pola ini disengaja dan diulang di
	 * seluruh entity repo: interceptor audit boleh memanggilnya tanpa perlu memeriksa dulu apakah
	 * pengguna saat ini diketahui, tanpa risiko menghapus stempel lama.</p>
	 *
	 * @param olehId id pengguna baru; {@code null}/kosong tidak berefek apa pun
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: {@code null} maupun string kosong/spasi
	 * diabaikan diam-diam sehingga stempel audit lama tidak pernah terhapus.</p>
	 *
	 * @param oleh nama pengguna baru; {@code null}/kosong tidak berefek apa pun
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait daur hidup JPA yang berjalan <b>tepat sebelum</b> Hibernate menerbitkan
	 * {@code UPDATE} untuk baris ini, ditambah deklarasi field {@code tanggal_dirubah} yang
	 * menyertainya pada baris yang sama.
	 *
	 * <p><b>Tujuan:</b> mendelegasikan pengisian stempel waktu/pengguna ke
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} sehingga setiap perubahan
	 * baris laporan terekam tanpa perlu diingat oleh kode pemanggil. Method ini <b>tidak pernah
	 * dipanggil manual</b> dari mana pun &mdash; hanya penyedia JPA yang memanggilnya.</p>
	 *
	 * <p><b>Efek samping:</b> menulis ke field {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * milik instance ini. Karena {@link #setOleh(String)} dan {@link #setOlehId(String)} menolak
	 * nilai kosong, stempel lama bertahan bila konteks pengguna tidak tersedia (mis. penulisan dari
	 * proses latar atau dari jalur REST tanpa sesi ZK).</p>
	 *
	 * <p><b>Catatan:</b> kait ini hanya menyala pada {@code UPDATE}, <b>bukan</b> pada
	 * {@code INSERT}; nilai awal {@code tanggal_dirubah} datang dari inisialisasi field
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) yang dijalankan saat objek dibuat. Deklarasi field
	 * sengaja diletakkan menempel di baris yang sama oleh pembangkit kode lama &mdash; jangan
	 * dipisah tanpa menguji ulang, karena beberapa alat di repo ini memindai pola tersebut.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Berbeda dari {@link #setOleh(String)}/{@link #setOlehId(String)}, setter ini
	 * <b>tidak</b> menyaring {@code null} &mdash; memanggilnya dengan {@code null} benar-benar
	 * mengosongkan stempel waktu.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru; {@code null} diterima apa adanya
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP}. Nilai awalnya diisi saat objek Java dibuat, lalu
	 * diperbarui oleh {@code onUpdate()} pada setiap {@code UPDATE}.</p>
	 *
	 * @return stempel waktu perubahan terakhir; praktis tidak pernah {@code null} untuk objek yang
	 *         dibuat lewat konstruktor, tetapi bisa {@code null} bila kolomnya kosong di basis data
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Nomor urut cetak baris ini di dalam seksinya.
	 *
	 * <p>Bertipe {@code Double} (bukan {@code Integer}) supaya baris baru dapat disisipkan di
	 * antara dua baris lama &mdash; mis. 2,5 di antara 2 dan 3 &mdash; tanpa perlu menomori ulang
	 * seluruh laporan. Seluruh pemakainya mengurutkan dengan {@code order by urut} secara eksplisit.</p>
	 */
	private Double urut;

	/**
	 * Label utama baris laporan (tingkat 1). Di layar ZK diberi label <b>"Sub Laporan"</b> pada
	 * {@code KelompokLaporanAction} dan <b>"Sub Grup"</b> pada {@code KelompokLaporanDanDetailAction}.
	 * Inilah teks yang tercetak sebagai nama baris di laporan HTML.
	 *
	 * <p>Menyembunyikan (<em>shadowing</em>) field bernama sama di {@link GeneralValueObject};
	 * lihat catatan pewarisan pada Javadoc kelas.</p>
	 */
	private String keterangan;

	/** Label baris tingkat 2 ("Sub Grup II"). Ikut menjadi kunci pencocokan saat impor Excel. */
	private String keterangan1;

	/** Label baris tingkat 3 ("Sub Grup III"). Hanya dipakai untuk pencarian dan tampilan. */
	private String keterangan2;

	/**
	 * Sumbu pertama: laporan mana baris ini muncul (Neraca / Rugi Laba / Arus Kas).
	 * FK <code>jenis_laporan</code>, {@code nullable}.
	 */
	private JenisLaporan jenisLaporan;

	/**
	 * Sumbu kedua: seksi/judul blok tempat baris ini bernaung (Aktiva / Kewajiban / dst).
	 * FK <code>master_grup_laporan</code>, {@code nullable} &mdash; baris yatim ditampung
	 * {@code LaporanKeuanganCoaHelper} sebagai grup semu "Lainnya" ber-id -1.
	 */
	private MasterGrupLaporan masterGrupLaporan;

	/** Bendera "baris ini ikut dicetak". Baca lewat {@link #getAktif()} yang mem-default ke {@code true}. */
	private Boolean aktif;

	/**
	 * Bendera "tampilkan rincian akun di bawah baris ini". Baca lewat
	 * {@link #getTampilkanAkunRinci()} yang mem-default ke {@code true}.
	 */
	private Boolean tampilkanAkunRinci;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate.
	 *
	 * <p>Seluruh field dibiarkan {@code null} kecuali {@code tanggal_dirubah} yang langsung diisi
	 * waktu saat ini oleh inisialisasi field. Dipakai baik oleh Hibernate saat memuat baris maupun
	 * oleh kode aplikasi yang membuat baris baru
	 * ({@code KelompokLaporanAction.onAdd}, impor Excel, dan {@code PemetaanAkunHelper}).</p>
	 */
	public KelompokLaporan() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Bernilai {@code null} untuk objek yang belum pernah disimpan &mdash; kedua layar ZK
	 * memakai fakta ini untuk membedakan mode "Tambah" dari mode "Ubah" (judul window dan pilihan
	 * antara {@code dao.save} atau {@code dao.update}).</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama. Praktis hanya dipanggil Hibernate; kode aplikasi tidak boleh
	 * menetapkan id sendiri karena kolomnya {@code IDENTITY} dan {@code insertable = false}.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan label utama baris laporan, <b>sudah dirapikan</b>.
	 *
	 * <p><b>Tujuan:</b> menyediakan teks siap tampil tanpa pemanggil perlu menjaga terhadap
	 * {@code null} maupun spasi berlebih. Dipakai perender grid, perender laporan HTML
	 * ({@code LaporanKeuanganCoaHelper.susun()} sebagai {@code Baris.label}), formulir ubah, dan
	 * pesan konfirmasi pemetaan.</p>
	 *
	 * <p><b>Perilaku:</b> mengembalikan string kosong bila field {@code null}, selain itu
	 * mengembalikan hasil {@code trim()}. <b>Bukan getter destruktif</b>: hasil {@code trim()}
	 * hanya dikembalikan, <b>tidak</b> ditugaskan balik ke field &mdash; membaca properti ini tidak
	 * pernah mengubah isi basis data. (Bandingkan dengan kedua getter relasi di bawah, yang memang
	 * menugaskan ulang field-nya.)</p>
	 *
	 * <p><b>Efek samping tak langsung yang perlu diketahui:</b> karena getter merapikan sedangkan
	 * setter tidak, teks dengan spasi di ujung tetap <em>tersimpan</em> apa adanya di basis data.
	 * Query pencocokan yang membandingkan langsung ke kolom (mis. pemeriksaan duplikat
	 * {@code Restrictions.eq("keterangan", ...)} di {@code KelompokLaporanAction.checkNamaKelLaporan}
	 * dan pencocokan impor Excel {@code Restrictions.ilike(..., MatchMode.EXACT)}) dapat meleset
	 * untuk baris lama yang kadung tersimpan dengan spasi berlebih &mdash; layarnya akan mengira
	 * label itu belum dipakai lalu membuat baris kembar.</p>
	 *
	 * @return label baris yang sudah di-{@code trim()}; string kosong bila belum diisi, tidak pernah {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Menyetel label utama baris laporan. Tanpa validasi dan <b>tanpa {@code trim()}</b> &mdash;
	 * nilai disimpan persis seperti yang diberikan (lihat catatan pada {@link #getKeterangan()}).
	 *
	 * @param keterangan label baru; {@code null} diterima dan akan terbaca sebagai {@code ""}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menempatkan baris ini pada sumbu "laporan mana".
	 *
	 * <p>Dipanggil dari {@code KelompokLaporanAction.onSave} dan
	 * {@code KelompokLaporanDanDetailAction.onSave} (nilai dari combobox, keduanya menolak
	 * {@code null} sebelum sampai ke sini), dari impor Excel di layar kedua, serta dari
	 * {@code PemetaanAkunHelper} yang mengisinya dengan proxy hasil
	 * {@code session.load(JenisLaporan.class, ...)}.</p>
	 *
	 * @param jenisLaporan jenis laporan tujuan; secara skema boleh {@code null}, tetapi seluruh
	 *                     jalur tulis yang ada selalu mengisinya
	 */
	public void setJenisLaporan(JenisLaporan jenisLaporan) {
		this.jenisLaporan = jenisLaporan;
	}

	/**
	 * Mengembalikan jenis laporan tempat baris ini muncul (Neraca / Rugi Laba / Arus Kas).
	 *
	 * <p><b>Tujuan:</b> menyediakan objek {@link JenisLaporan} yang <b>sudah terinisialisasi</b>,
	 * aman dipakai walau instance ini sudah lepas dari sesi Hibernate yang memuatnya &mdash;
	 * situasi yang lazim terjadi karena objek entity disimpan di memori sesi ZK antar-permintaan.</p>
	 *
	 * <p><b>Cara kerja:</b> memanggil {@link GeneralValueObject#check(Object)} lalu
	 * <b>menugaskan hasilnya kembali ke field</b>. {@code check()} mencoba empat sumber berurutan
	 * (penanda {@code initData}, cache in-memory, inisialisasi proxy lewat sesi berjalan, lalu
	 * pemuatan ulang lewat sesi baru) dan <b>mengembalikan argumennya apa adanya bila keempatnya
	 * gagal</b>. Karena itu penugasan balik di sini <b>tidak destruktif</b>: ia hanya bisa
	 * mengganti proxy dengan objek nyata, tidak pernah menyulap referensi yang sah menjadi
	 * {@code null}. Ini berbeda tajam dari {@code Transaksi.getAkun()} (yang menimpa akun resmi
	 * dengan {@code akunOver}) maupun {@code ProsesTransitori.getDisetujuiOleh()} (yang dapat
	 * mencabut persetujuan hanya dengan dibaca) &mdash; <b>verifikasi negatif yang menenangkan
	 * untuk entity ini</b>.</p>
	 *
	 * <p><b>Efek samping:</b> tetap ada, meski jinak. (1) Field diganti instance lain, sehingga
	 * pembacaan berikutnya lebih murah. (2) Bila proxy perlu di-resolve, method ini dapat
	 * menerbitkan {@code SELECT} tambahan &mdash; relevan karena relasi ini {@code LAZY} dan
	 * {@code KelompokLaporanRenderer} memanggilnya sekali per baris grid (masalah N+1 klasik pada
	 * daftar panjang). (3) Karena kelas ini {@code dynamicUpdate}, penggantian instance yang
	 * nilainya sama tidak menghasilkan {@code UPDATE} apa pun.</p>
	 *
	 * <p><b>Kapan dipanggil:</b> perender grid kedua layar ZK, formulir ubah
	 * ({@code Common.selectComboItem}), {@code NewUiLaporanKeuanganKoperasiController} lewat HQL
	 * {@code select distinct kl.jenisLaporan ...}, serta seluruh kriteria pencarian yang menyaring
	 * berdasarkan sumbu ini.</p>
	 *
	 * @return jenis laporan baris ini, atau {@code null} bila kolom FK-nya memang kosong
	 *         (kolomnya {@code nullable}; hanya jalur ZK yang menjamin terisi)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_laporan", nullable = true)
	public JenisLaporan getJenisLaporan() {
		jenisLaporan = check(jenisLaporan);
		return jenisLaporan;
	}

	/**
	 * Menempatkan baris ini pada sumbu "seksi/judul blok".
	 *
	 * <p>Dipanggil dari {@code onSave} kedua layar ZK dan dari impor Excel. <b>Tidak pernah
	 * dipanggil</b> oleh {@code PemetaanAkunHelper} &mdash; itulah sebabnya jalur REST menghasilkan
	 * baris yatim yang kemudian tercetak di bawah judul semu "Lainnya".</p>
	 *
	 * @param masterGrupLaporan seksi tujuan; {@code null} diperbolehkan skema dan memang terjadi
	 *                          pada baris buatan jalur REST
	 */
	public void setMasterGrupLaporan(MasterGrupLaporan masterGrupLaporan) {
		this.masterGrupLaporan = masterGrupLaporan;
	}

	/**
	 * Mengembalikan seksi (judul blok) tempat baris ini bernaung.
	 *
	 * <p><b>Tujuan, cara kerja, dan sifat efek sampingnya identik dengan
	 * {@link #getJenisLaporan()}</b>: {@code check()} + penugasan balik, tidak destruktif, berpotensi
	 * menerbitkan {@code SELECT} tambahan karena relasi ini {@code LAZY}.</p>
	 *
	 * <p><b>Yang membedakan adalah konsekuensi {@code null}-nya.</b> Nilai {@code null} di sini
	 * benar-benar terjadi di lapangan (baris buatan {@code PemetaanAkunHelper}), dan setiap
	 * pemakai menanganinya berbeda:</p>
	 * <ul>
	 *   <li>{@code LaporanKeuanganCoaHelper.susun()} &rarr; dimasukkan ke grup semu id -1 berlabel
	 *       "Lainnya", baris <b>tetap tercetak</b>;</li>
	 *   <li>perender grid kedua layar ZK &rarr; menampilkan sel kosong (sudah dijaga
	 *       {@code == null ? "" : ...});</li>
	 *   <li>{@code LaporanAkuntingSaldoBulanMaster} dan
	 *       {@code NewUiLaporanAkuntingSaldoBulanController} &rarr; disaring keluar dengan
	 *       {@code Restrictions.isNotNull("masterGrupLaporan")};</li>
	 *   <li>{@code KelompokLaporanAction.onSearchDefault} &rarr; <b>hilang sama sekali</b>, karena
	 *       {@code createAlias} tanpa tipe join berarti INNER JOIN.</li>
	 * </ul>
	 *
	 * @return seksi laporan baris ini, atau {@code null} untuk baris yatim
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "master_grup_laporan", nullable = true)
	public MasterGrupLaporan getMasterGrupLaporan() {
		masterGrupLaporan = check(masterGrupLaporan);
		return masterGrupLaporan;
	}

	/**
	 * Menyetel nomor urut cetak baris ini.
	 *
	 * <p>Diisi dari {@code MyDoublebox} pada kedua layar ZK (default 1,0 di layar "Kelompok
	 * Laporan" dan 0,0 di layar "Kelompok Laporan dan Detail" untuk baris baru), dan dihitung
	 * otomatis oleh {@code PemetaanAkunHelper.urutDariKode()} yang mengambil digit awal kode akun
	 * induk (mis. 512.000 &rarr; 512, tanpa kode &rarr; 9999) supaya urutan baris otomatis
	 * mengikuti urutan bagan akun.</p>
	 *
	 * <p><b>Tidak ada penjaga keunikan maupun rentang</b>: dua baris boleh memiliki {@code urut}
	 * yang sama, dan urutan relatif keduanya lalu ditentukan basis data (tidak deterministik).</p>
	 *
	 * @param urut nomor urut baru; {@code null} diperbolehkan dan akan terbaca apa adanya
	 */
	public void setUrut(Double urut) {
		this.urut = urut;
	}

	/**
	 * Mengembalikan nomor urut cetak baris ini.
	 *
	 * <p><b>Tidak memberi nilai default</b> (berbeda dari {@link #getAktif()} dan
	 * {@link #getTampilkanAkunRinci()}) &mdash; {@code null} dikembalikan apa adanya, dan setiap
	 * pemanggil wajib menjaganya sendiri. Kedua perender grid memang melakukannya
	 * ({@code getUrut() == null ? "" : Common.numberFormat...}); pengurutan SQL
	 * {@code order by urut} menempatkan baris ber-{@code null} sesuai kebiasaan basis data
	 * (PostgreSQL: paling akhir pada urutan menaik).</p>
	 *
	 * @return nomor urut cetak, atau {@code null} bila belum pernah diisi
	 */
	public Double getUrut() {
		return urut;
	}

	/**
	 * Mengembalikan bendera "baris ini ikut dicetak", dengan <b>default aman {@code true}</b>.
	 *
	 * <p><b>Tujuan default:</b> baris lama yang dibuat sebelum kolom ini ada (nilainya
	 * {@code NULL} di basis data) tetap tampil, bukan menghilang diam-diam dari laporan. Semua
	 * penyaring SQL/HQL di repo menuliskan hal yang sama secara eksplisit &mdash;
	 * {@code (kl.aktif is null or kl.aktif = true)} di {@code LaporanKeuanganCoaHelper.ambilKelompok()}
	 * dan {@code (c.aktif is null or c.aktif)} di {@code cek_pemetaan_akun.jsp} &mdash; sehingga
	 * lapisan Java dan lapisan basis data sepakat.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari:</b> karena getter tidak pernah mengembalikan
	 * {@code null}, kode pemanggil <b>tidak dapat membedakan</b> "sengaja diaktifkan" dari "belum
	 * pernah disetel". Layar "Kelompok Laporan dan Detail" karena itu selalu menampilkan checkbox
	 * Aktif dalam keadaan tercentang untuk baris baru, dan menyimpannya sebagai {@code true} nyata
	 * begitu formulir disimpan.</p>
	 *
	 * @return {@code true} bila baris aktif atau belum pernah disetel; {@code false} hanya bila
	 *         secara eksplisit dinonaktifkan. Tidak pernah {@code null}.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel bendera aktif baris laporan.
	 *
	 * <p>Ditulis dari checkbox "Aktif" pada {@code KelompokLaporanDanDetailAction.onSave} (layar
	 * "Kelompok Laporan" yang lebih sederhana <b>tidak</b> mengekspos bendera ini sama sekali),
	 * dari impor Excel (kolom <code>kelompokLaporan.aktif</code>, dengan aturan permisif
	 * {@code aktif == null || aktif.equalsIgnoreCase("true")} &mdash; teks yang salah eja
	 * menghasilkan {@code false}, sedangkan kolom yang kosong menghasilkan {@code true}), dan dari
	 * {@code PemetaanAkunHelper} yang selalu mengisi {@code Boolean.TRUE}.</p>
	 *
	 * <p><b>Efek:</b> menonaktifkan sebuah baris menghilangkan seluruh akun di bawahnya dari
	 * laporan HTML tanpa menghapus data apa pun &mdash; nilai akun tersebut lenyap dari total tanpa
	 * jejak di laporan itu sendiri. Karena tabel ini tidak bertenant, efeknya berlaku untuk seluruh
	 * tenant satu instalasi.</p>
	 *
	 * @param aktif bendera baru; {@code null} akan terbaca sebagai {@code true} lewat
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan bendera "tampilkan rincian akun di bawah baris ini", dengan default
	 * {@code true}.
	 *
	 * <p><b>Tujuan:</b> menentukan apakah laporan mencetak daftar akun penyusun baris ini satu per
	 * satu, atau hanya angka totalnya. Dibaca {@code LaporanKeuanganCoaHelper.susun()} lewat
	 * {@code Boolean.TRUE.equals(kl.getTampilkanAkunRinci())}; bila aktif, tiap
	 * {@code KelompokLaporanPunyaAkun} di bawah baris ini ditambahkan sebagai objek
	 * {@code Rincian} berisi "kode + nama akun" dan nilainya.</p>
	 *
	 * <p><b>Perhatikan asimetrinya:</b> default {@code true} berarti baris lama yang belum pernah
	 * disetel akan <b>membeberkan seluruh akun penyusunnya</b> di laporan. Ini pilihan yang lebih
	 * berisik &mdash; bukan lebih ringkas &mdash; dan pada bagan akun besar dapat membuat satu
	 * halaman laporan memanjang drastis. Bendera ini murni soal penyajian; ia tidak pernah
	 * memengaruhi angka total baris.</p>
	 *
	 * @return {@code true} bila rincian akun ditampilkan atau bendera belum pernah disetel;
	 *         {@code false} hanya bila dimatikan eksplisit. Tidak pernah {@code null}.
	 */
	public Boolean getTampilkanAkunRinci() {
		return tampilkanAkunRinci == null ? true : tampilkanAkunRinci;
	}

	/**
	 * Menyetel bendera tampilkan-rincian-akun.
	 *
	 * <p>Hanya ada satu penulis di seluruh repo: checkbox "Rinci" pada
	 * {@code KelompokLaporanDanDetailAction.onSave}. Kolom ini ikut diekspor/diimpor lewat berkas
	 * Excel (nama kolom <code>kelompokLaporan.tampilkanAkunRinci</code>) tetapi
	 * <b>tidak diproses</b> oleh listener impor &mdash; nilainya terbawa keluar saat "Download
	 * Akun" namun diabaikan saat "Upload Akun". Menyunting kolom itu di berkas Excel karena itu
	 * tidak berefek apa pun; perubahan harus dilakukan lewat formulir.</p>
	 *
	 * @param tampilkanAkunRinci bendera baru; {@code null} akan terbaca sebagai {@code true}
	 */
	public void setTampilkanAkunRinci(Boolean tampilkanAkunRinci) {
		this.tampilkanAkunRinci = tampilkanAkunRinci;
	}

	/**
	 * Mengembalikan label baris tingkat 2 ("Sub Grup II"), sudah dirapikan.
	 *
	 * <p>Perilakunya sama dengan {@link #getKeterangan()}: {@code null} menjadi {@code ""},
	 * selain itu di-{@code trim()}, dan hasilnya <b>tidak</b> ditugaskan balik ke field (bukan
	 * getter destruktif).</p>
	 *
	 * <p><b>Peran khusus:</b> field ini ikut menjadi <b>kunci pencocokan</b> saat impor Excel di
	 * {@code KelompokLaporanDanDetailAction} &mdash; kombinasi
	 * (keterangan, keterangan1, masterGrupLaporan, jenisLaporan) menentukan apakah baris yang ada
	 * diperbarui atau baris baru dibuat. Perbedaan spasi/ejaan sekecil apa pun pada kolom ini akan
	 * menghasilkan baris laporan kembar, bukan pembaruan.</p>
	 *
	 * @return label tingkat 2 yang sudah di-{@code trim()}; string kosong bila belum diisi, tidak
	 *         pernah {@code null}
	 */
	public String getKeterangan1() {
		return keterangan1 == null ? "" : keterangan1.trim();
	}

	/**
	 * Menyetel label baris tingkat 2. Tanpa validasi dan tanpa {@code trim()}.
	 *
	 * @param keterangan1 label baru; {@code null} diterima dan akan terbaca sebagai {@code ""}
	 */
	public void setKeterangan1(String keterangan1) {
		this.keterangan1 = keterangan1;
	}

	/**
	 * Mengembalikan label baris tingkat 3 ("Sub Grup III").
	 *
	 * <p><b>Tidak konsisten dengan dua saudaranya:</b> getter ini mengembalikan field
	 * <b>apa adanya</b> &mdash; tanpa penggantian {@code null} menjadi {@code ""} dan tanpa
	 * {@code trim()}. Pemanggil karena itu <b>wajib menjaga {@code null} sendiri</b>. Formulir ubah
	 * di {@code KelompokLaporanDanDetailAction} memang menyerahkannya langsung ke konstruktor
	 * {@code Textbox} (yang menerima {@code null}), tetapi kode baru yang meniru pola
	 * {@code getKeterangan()}/{@code getKeterangan1()} akan terkena {@code NullPointerException}.
	 * Ketidakseragaman ini <b>dibiarkan apa adanya</b> di sini karena mengubahnya berarti mengubah
	 * perilaku, bukan dokumentasi.</p>
	 *
	 * @return label tingkat 3 apa adanya; <b>bisa {@code null}</b>
	 */
	public String getKeterangan2() {
		return keterangan2;
	}

	/**
	 * Menyetel label baris tingkat 3. Tanpa validasi dan tanpa {@code trim()}.
	 *
	 * <p>Hanya ditulis dari formulir {@code KelompokLaporanDanDetailAction}; ikut diekspor lewat
	 * "Download Akun" tetapi tidak diproses listener "Upload Akun".</p>
	 *
	 * @param keterangan2 label baru; {@code null} diterima dan akan terbaca kembali sebagai
	 *                    {@code null}
	 */
	public void setKeterangan2(String keterangan2) {
		this.keterangan2 = keterangan2;
	}

}
