package ais.database.model;

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

import ais.database.model.akunting.Akun;

/**
 * Tabel jembatan pemetaan akuntansi: menghubungkan satu {@link ItemBiaya} (item tagihan/billing)
 * dengan satu {@link Akun} berperan <b>PENDAPATAN DENDA</b>, berlaku untuk cakupan
 * fakultas/jurusan/program/angkatan tertentu. Satu baris menjawab pertanyaan: <i>"kalau mahasiswa
 * berkonteks (fakultas F, jurusan J, program P, angkatan A) terlambat membayar item biaya X dan
 * dikenakan denda, akun pendapatan mana yang dikredit untuk denda itu?"</i>
 *
 * <p>Tabel database: {@code public.item_biaya_punya_pendapatan_denda}. Kelas ini murni data &mdash;
 * tidak ada satu pun method bisnis/query statis di sini; seluruh logika pencarian baris berada di
 * {@link ItemBiaya}, dan seluruh logika penyimpanan di helper UI-nya
 * ({@code ais.action.master.akunting.helper.ItemBiayaPunyaDenda} &mdash; perhatikan namanya
 * <b>tidak</b> mengandung kata "Pendapatan", lihat catatan kuirk di bawah).</p>
 *
 * <h3>Keluarga lima tabel jembatan yang identik</h3>
 *
 * <p>Entity ini adalah salah satu dari lima tabel jembatan ber-struktur <b>identik</b> yang
 * menggantung pada {@link ItemBiaya}, masing-masing untuk satu peran akun:</p>
 *
 * <ul>
 *   <li>{@link ItemBiayaPunyaAkun} &rarr; akun <b>pendapatan</b>, dibaca
 *   {@link ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaPiutang} &rarr; akun <b>piutang</b>, dibaca
 *   {@link ItemBiaya#ambilPiutang(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaDibayarDimuka} &rarr; akun <b>pendapatan diterima di muka</b>, dibaca
 *   {@link ItemBiaya#ambilDibayarDimuka(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@code ItemBiayaPunyaPendapatanDenda} (kelas ini) &rarr; akun <b>pendapatan denda</b>,
 *   dibaca {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaDiskon} &rarr; akun <b>diskon/potongan</b> &mdash; satu-satunya
 *   anggota keluarga yang <b>tidak punya resolver</b> di {@link ItemBiaya}, sehingga datanya
 *   yatim/write-only.</li>
 * </ul>
 *
 * <p><b>Hasil verifikasi perbandingan kode:</b> berkas ini dibandingkan baris-per-baris dengan
 * versi pristine {@link ItemBiayaPunyaPiutang} dan {@link ItemBiayaPunyaDiskon} (setelah nama
 * kelas dan nama tabel dinormalkan). Ketiganya <b>identik</b>, dengan satu-satunya perbedaan
 * bersifat kosmetik murni: di kelas ini deklarasi field {@code fakultas} diletakkan tepat setelah
 * {@code akun}, sedangkan di dua kerabatnya diletakkan paling akhir. Nol perbedaan semantik
 * &mdash; termasuk {@code serialVersionUID} yang sama persis ({@code 2463821577548439808L}) di
 * ketiganya, sisa salin-tempel generator. Konsekuensi praktis: setiap temuan/perbaikan pada salah
 * satu kelas hampir selalu berlaku untuk keempat lainnya.</p>
 *
 * <h3>Entity ini HIDUP: resolvernya ada dan dipakai mesin posting</h3>
 *
 * <p><b>Hasil verifikasi:</b> berbeda dari {@link ItemBiayaPunyaDiskon} yang ternyata yatim,
 * pemetaan denda ini benar-benar dibaca. {@link ItemBiaya} menyediakan pasangan resolver lengkap
 * &mdash; {@link ItemBiaya#ambilPendapatanDenda(Kegiatan)} (menyimpulkan konteks dari mahasiswa /
 * calon mahasiswa) yang mendelegasikan ke
 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)} (algoritma berjenjang
 * delapan tahap) &mdash; dan pemanggil nyatanya:</p>
 *
 * <ul>
 *   <li>{@code ais.action.master.PostingCicilanMahasiswaAction} &mdash; empat titik panggil
 *   (pratinjau jurnal, posting satu cicilan, posting massal, dan pembatalan/posting ulang);</li>
 *   <li>{@code ais.action.master.PostingCicilanDibayarDimukaMahasiswaAction} &mdash; tiga titik
 *   panggil dengan pola yang sama untuk jalur pendapatan diterima di muka;</li>
 *   <li>{@link ais.database.model.akunting.GrupTransaksi} &mdash; lewat
 *   {@code ambilAkunPendapatanDenda(ItemBiaya, Kegiatan, JurnalPembayaranAkunCache)}, yang
 *   membungkus panggilan dalam {@code try/catch} dan meng-cache hasilnya per proses posting
 *   dengan kunci peran {@code "DENDA"} (bersebelahan dengan kunci {@code "PIUTANG"} dan
 *   {@code "PENDAPATAN"}).</li>
 * </ul>
 *
 * <h3>Peran baris ini dalam jurnal pembayaran</h3>
 *
 * <p>Denda hanya muncul bila pembayaran memang terlambat. Besarannya dihitung
 * {@link DetailBiaya#checkDenda} dari parameter denda yang tersimpan di {@link ItemBiaya} sendiri
 * ({@code dendaJikaTerlambat}, {@code nilaiDendaDalamPersen}, {@code defaultProsentaseDenda},
 * {@code dendaAkanBerlipatTerlambaHari}, {@code maksimalBerlipatTerlambaHari}); baris di tabel
 * inilah yang menentukan <b>ke akun mana</b> angka tersebut dibukukan.</p>
 *
 * <p>Dalam jurnal, akun hasil pemetaan ini menjadi <b>baris kredit tambahan</b>: total debet tetap
 * sebesar nilai pembayaran, sedangkan sisi kreditnya dipecah menjadi {@code nilai - denda} ke akun
 * pokok (piutang/pendapatan diterima di muka) plus {@code denda} ke akun pendapatan denda ini.</p>
 *
 * <p><b>Ini adalah gerbang keras, bukan sekadar pelengkap.</b> Di seluruh action posting cicilan
 * berlaku pola yang sama: bila {@code denda > 0.1} tetapi
 * {@link ItemBiaya#ambilPendapatanDenda(Kegiatan)} mengembalikan {@code null}, jurnal
 * <b>tidak dibentuk sama sekali</b> dan layar hanya menampilkan pesan <i>"Transaksi tidak valid.
 * Ada denda &lt;nilai&gt;, namun Akun denda tidak ditemukan"</i>. Jadi pemetaan yang hilang di
 * tabel ini menghentikan posting pembayaran yang mengandung denda &mdash; persis sekelas dampaknya
 * dengan pemetaan piutang yang hilang.</p>
 *
 * <h3>Cara baris ini dipilih saat posting (pencocokan berjenjang)</h3>
 *
 * <p>Baris tidak dicari dengan kunci tunggal, melainkan lewat <b>delapan tahap</b> di
 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)}, dari konteks paling
 * spesifik (jurusan + program) sampai paling umum (jurusan {@code IS NULL} + program
 * {@code IS NULL}), berhenti pada hasil pertama yang ditemukan. Konsekuensi praktis untuk pengisi
 * data:</p>
 *
 * <ul>
 *   <li>Baris dengan {@link #getJurusan() jurusan}, {@link #getFakultas() fakultas} dan
 *   {@link #getProgram() program} <b>kosong semua</b> berfungsi sebagai <b>default/fallback</b>
 *   untuk seluruh kombinasi yang tidak punya baris khusus. Karena tanpa pemetaan posting akan
 *   gagal total, satu baris default seperti ini praktis wajib ada di setiap item biaya yang
 *   mengaktifkan denda keterlambatan.</li>
 *   <li>{@link #getAngkatan() angkatan} ikut disaring di <b>semua</b> tahap dengan pencocokan
 *   {@code ILIKE '%...%'} (substring, tidak peka besar-kecil huruf) <b>atau</b> {@code NULL}
 *   <b>atau</b> string kosong &mdash; sehingga satu baris bisa melayani banyak angkatan sekaligus
 *   bila ditulis berderet (mis. {@code "2021,2022,2023"}), dan baris tanpa angkatan berlaku untuk
 *   semua angkatan. Karena substring, angkatan dua digit ({@code "21"}) akan ikut cocok dengan
 *   baris {@code "2021"}; selalu tulis empat digit.</li>
 *   <li>Seluruh badan resolver dibungkus {@code try/catch(Exception)}. Bila query gagal, hasilnya
 *   {@code null} dan pemanggil tidak bisa membedakan "belum dipetakan" dari "query error"
 *   &mdash; gejalanya sama-sama pesan "Akun denda tidak ditemukan".</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getFakultas()} adalah getter yang MENULIS.</b> Bila {@link #getJurusan()}
 *   terisi, getter ini menimpa field {@code fakultas} dengan fakultas milik jurusan tersebut.
 *   Karena pemetaan Hibernate kelas ini memakai <i>property access</i> (anotasi {@code @Id} dan
 *   seluruh anotasi kolom menempel pada <b>getter</b>, bukan field), nilai hasil timpaan itulah
 *   yang dibaca Hibernate saat <i>dirty check</i>; digabung {@code dynamicUpdate = true} di
 *   tingkat kelas, hasilnya adalah
 *   {@code UPDATE item_biaya_punya_pendapatan_denda SET fakultas = ?} yang benar-benar
 *   tereksekusi &mdash; plus satu revisi Envers, karena kelas ini {@code @Audited}. Perilaku ini
 *   <b>terkonfirmasi identik</b> dengan {@link ItemBiayaPunyaPiutang} dan
 *   {@link ItemBiayaPunyaDiskon}; rincian jalur pemicunya ada di javadoc method itu.</li>
 *   <li><b>Getter relasi memanggil {@code check(...)}</b>
 *   ({@link GeneralValueObject#check(Object)}) untuk meresolusi proxy lazy dan menetapkan
 *   hasilnya kembali ke field. Ini menulis balik ke field (bukan ke database dengan sendirinya)
 *   dan tidak pernah melempar exception; kegagalan resolusi bersifat senyap.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa
 *   &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate
 *   sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib mendeklarasikan
 *   sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi tidak
 *   ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat baris
 *   di-<i>update</i>, bukan saat pertama dibuat. Riwayat lengkapnya tetap ada lewat
 *   {@code @Audited} (Hibernate Envers), yang mencatat setiap revisi baris ke tabel audit dan
 *   ditampilkan di grid lewat {@code RevisiHelper.createNewRevisi(...)}.</li>
 *   <li><b>{@link #toString()} degeneratif</b> &mdash; hasilnya selalu {@code "<id>-"}, sama
 *   persis kata per kata dengan keempat kerabatnya; lihat javadoc method itu. Jangan diandalkan
 *   untuk label UI.</li>
 *   <li><b>Tidak ada kolom {@code nama} maupun {@code keterangan} di entity ini.</b> Baris
 *   pemetaan sengaja tidak punya label sendiri; identitasnya di layar dibentuk dari kode+nama
 *   {@link #getAkun() akun} ditambah kolom cakupannya. Karena tidak ada {@code getKeterangan()},
 *   kelas ini juga tidak ikut dalam pola "getter keterangan membalik kontrak kelas induk" yang
 *   tercatat di entity lain.</li>
 *   <li><b>Nama entity dan nama helper UI-nya tidak sinkron.</b> Entity ini
 *   {@code ItemBiayaPunyaPendapatanDenda}, tetapi kelas helper ZK yang mengelolanya bernama
 *   {@code ais.action.master.akunting.helper.ItemBiayaPunyaDenda} (tanpa "Pendapatan"). Nama
 *   pendek itu <b>bukan</b> entity dan tidak terdaftar di {@code hibernate.cfg.xml}; lihat catatan
 *   bug di bawah untuk akibat nyatanya.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   generator Apr 2010); tidak ada hubungannya dengan entity {@code Bank}.</li>
 * </ol>
 *
 * <h3>Catatan bug terverifikasi di layar daftar Item Biaya (bukan di kelas ini)</h3>
 *
 * <p>Kolom ringkasan pada layar daftar Item Biaya seharusnya menampilkan baris
 * <i>"Akun denda : &lt;kode-kode akun&gt;"</i>. Baris itu <b>tidak pernah terisi</b>. Penyebabnya
 * tabrakan nama: {@code ItemBiayaAction} meng-import helper UI
 * {@code ais.action.master.akunting.helper.ItemBiayaPunyaDenda} lalu keliru memakainya sebagai
 * kelas entity pada {@code session.createCriteria(ItemBiayaPunyaDenda.class)} &mdash; padahal
 * kelas itu bukan entity terpetakan. Query melempar {@code MappingException}, yang kemudian
 * ditelan dua lapis {@code catch} di {@code ConstantValues.simpleList(...)} sehingga
 * mengembalikan daftar kosong tanpa jejak error. Baris tetangganya ("Akun pendapatan" dan "Akun
 * piutang") memakai kelas entity yang benar dan tampil normal. Perbaikannya satu kata (ganti ke
 * {@code ItemBiayaPunyaPendapatanDenda.class}), tetapi berada di luar cakupan berkas ini.</p>
 *
 * <p>Asimetri terkait: layar diagnostik {@code AnalisisPemetaanAkunHelper} hanya membedah pemetaan
 * <b>pendapatan</b> dan <b>piutang</b>; tidak ada padanan untuk pemetaan denda, sehingga
 * penelusuran "kenapa akun denda tidak ketemu" harus dilakukan manual lewat tab
 * "Akun Pendapatan Denda".</p>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #ItemBiayaPunyaPendapatanDenda()}.</li>
 *   <li><b>Pasangan yang dipetakan</b>: {@link #getItemBiaya()} dan {@link #getAkun()}.</li>
 *   <li><b>Cakupan berlakunya pemetaan</b>: {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getProgram()}, {@link #getAngkatan()}.</li>
 * </ul>
 *
 * @see ItemBiaya
 * @see ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)
 * @see ItemBiaya#ambilPendapatanDenda(Kegiatan)
 * @see ItemBiayaPunyaPiutang
 * @see ItemBiayaPunyaAkun
 * @see DetailBiaya#checkDenda
 * @see Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "item_biaya_punya_pendapatan_denda")
public class ItemBiayaPunyaPendapatanDenda extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris (kolom {@code id}, IDENTITY/serial PostgreSQL). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code @PreUpdate}, bukan saat insert. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; diisi {@code @PreUpdate}, bukan saat insert. */
	private String olehId;

	/** @return id pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pengubah. Nilai {@code null} atau berisi spasi saja
	 * <b>diabaikan</b> (nilai lama dipertahankan) &mdash; guard seragam di seluruh entity
	 * turunan {@link GeneralValueObject}, supaya jejak audit tidak terhapus oleh pemanggil
	 * yang tidak punya konteks pengguna.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong diabaikan sehingga nilai lama tidak tertimpa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mengisi {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * dari konteks pengguna aktif lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)}. Tidak ada pasangan
	 * {@code @PrePersist}, jadi baris yang baru dibuat tidak punya jejak pembuat &mdash; hanya
	 * Envers yang merekam revisi pertamanya.
	 *
	 * <p>Method ini tidak boleh dipanggil manual; Hibernate memanggilnya sendiri sebelum setiap
	 * UPDATE &mdash; termasuk UPDATE "hantu" yang dipicu {@link #getFakultas()}, sehingga baris
	 * bisa tercatat "diubah oleh" pengguna yang sebenarnya hanya membuka tab pemetaan.</p>
	 *
	 * <p><b>Catatan pembaca kode:</b> deklarasi ini satu baris dengan field
	 * {@code tanggal_dirubah} (gaya penyisipan otomatis di seluruh model). Field tersebut
	 * diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}), sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir baris ini */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi TIMESTAMP) */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks bawaan hasil generator hbm2java yang <b>tidak pernah dilengkapi</b>:
	 * selalu berbentuk {@code "<id>-"} (bagian setelah tanda hubung adalah string kosong
	 * hardcoded {@code ""}), mis. {@code "412-"}.
	 *
	 * <p><b>Hasil verifikasi:</b> ini <i>bukan</i> field {@code nama} yang lupa dideklarasikan.
	 * Tabel {@code item_biaya_punya_pendapatan_denda} memang tidak punya kolom nama/keterangan,
	 * dan keempat kerabatnya ({@link ItemBiayaPunyaAkun}, {@link ItemBiayaPunyaPiutang},
	 * {@link ItemBiayaPunyaDibayarDimuka}, {@link ItemBiayaPunyaDiskon}) punya {@code toString()}
	 * yang persis sama, kata per kata. Yang terjadi adalah template hbm2java tahun 2010 merangkai
	 * {@code id + "-" + <properti label>} dan slot label itu dibiarkan kosong karena entity
	 * jembatan tidak punya kolom label &mdash; disengaja secara struktural, bukan bug
	 * penulisan.</p>
	 *
	 * <p>Praktisnya method ini tidak dipakai siapa pun untuk menampilkan data: helper grid
	 * merender {@code getAkun().getKode()} dan {@code getAkun().getNama()} secara eksplisit.
	 * Jangan mengandalkannya sebagai label UI, dan jangan pula "membetulkannya" tanpa memeriksa
	 * apakah ada log/pesan yang sudah terlanjur mengandalkan formatnya.</p>
	 *
	 * @return {@code id} diikuti tanda hubung
	 */
	public String toString() {
		return id + "-" + "";
	}

	/** Item biaya yang akun pendapatan denda-nya dipetakan oleh baris ini (kolom {@code item_biaya}). */
	private ItemBiaya itemBiaya;
	/** Akun pendapatan denda hasil pemetaan (kolom {@code akun}, entity {@link Akun} di modul akunting). */
	private Akun akun;
	/** Cakupan fakultas; lihat kuirk penurunan otomatis di {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Cakupan jurusan/program studi; ikut menentukan {@link #getFakultas()} secara otomatis. */
	private Jurusan jurusan;
	/** Cakupan nama program (mis. Reguler/Karyawan) sebagai teks bebas; {@code null} = semua program. */
	private String program;
	/** Cakupan angkatan sebagai teks bebas, dicocokkan secara substring; kosong = semua angkatan. */
	private String angkatan;

	/**
	 * Konstruktor tanpa argumen (wajib untuk Hibernate). Juga dipakai langsung oleh
	 * {@code ais.action.master.akunting.helper.ItemBiayaPunyaDenda} saat tombol "Tambah Akun"
	 * pada tab "Akun Pendapatan Denda" menambahkan satu baris baru ke grid untuk setiap akun yang
	 * dipilih di dialog {@code AmbilDataBanyakAkun}; baris tersebut baru benar-benar disimpan
	 * ketika jendela Item Biaya di-<i>save</i>.
	 */
	public ItemBiayaPunyaPendapatanDenda() {
	}

	/** @return kunci utama baris, atau {@code null} bila belum pernah disimpan */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama; normalnya hanya diisi Hibernate (kolom IDENTITY, {@code insertable = false}) */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Item biaya pemilik baris pemetaan ini (kolom {@code item_biaya}, boleh {@code null} di
	 * tingkat database). Relasi LAZY; getter memanggil
	 * {@link GeneralValueObject#check(Object)} lebih dulu untuk meresolusi proxy dan menetapkan
	 * hasilnya kembali ke field.
	 *
	 * <p>Meski kolomnya {@code nullable = true}, baris tanpa item biaya tidak akan pernah
	 * terpungut oleh pencarian mana pun: setiap tahap query di
	 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)} maupun pemuatan
	 * grid di helper UI selalu menyaring {@code itemBiaya = ?}. Baris yatim seperti itu praktis
	 * tak terlihat.</p>
	 *
	 * @return item biaya pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya pemilik. Diisi dua kali oleh alur UI: sekali saat baris dibuat
	 *                  di grid oleh helper "Tambah Akun", sekali lagi tepat sebelum
	 *                  {@code session.saveOrUpdate(...)} di {@code ItemBiayaAction} &mdash; supaya
	 *                  baris yang dibuat sebelum item biayanya sendiri tersimpan tetap menunjuk ke
	 *                  entity yang benar.
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Akun pendapatan denda yang menjadi tujuan pemetaan (kolom {@code akun}). Relasi LAZY; getter
	 * memanggil {@link GeneralValueObject#check(Object)} lebih dulu.
	 *
	 * <p>Kolomnya {@code nullable = true} di tingkat database, tetapi jendela Item Biaya
	 * <b>menolak menyimpan</b> bila ada baris denda yang akunnya kosong (validasi eksplisit di
	 * {@code ItemBiayaAction} dengan pesan "Kolom Pendapatan Denda belum Bapak/Ibu isi ..."). Jadi
	 * baris ber-akun {@code null} hanya bisa muncul dari jalur non-UI.</p>
	 *
	 * <p>Inilah nilai yang akhirnya dikembalikan
	 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)} &mdash; resolver
	 * memproyeksikan {@code akun.id} lalu memuat ulang entity {@link Akun}-nya, bukan mengambil
	 * baris pemetaan secara utuh.</p>
	 *
	 * @return akun pendapatan denda, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/** @param akun akun pendapatan denda tujuan pemetaan (wajib diisi sebelum jendela Item Biaya bisa disimpan) */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Jurusan/program studi yang menjadi cakupan baris ini (kolom {@code jurusan}); {@code null}
	 * berarti pemetaan tidak dibatasi per jurusan. Relasi LAZY; getter memanggil
	 * {@link GeneralValueObject#check(Object)} lebih dulu.
	 *
	 * <p><b>Perhatikan efek sampingnya:</b> mengisi jurusan otomatis menentukan
	 * {@link #getFakultas()} &mdash; getter fakultas menurunkan nilainya dari jurusan ini dan
	 * menimpa apa pun yang pernah di-set manual.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila berlaku untuk semua jurusan
	 * @see #getFakultas()
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan jurusan cakupan; mengisinya membuat {@link #getFakultas()} menimpa field fakultas */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Nama program (mis. "Reguler", "Karyawan") sebagai cakupan tambahan; kolom {@code program}
	 * berisi teks, bukan relasi. Nilai {@code null} berarti "berlaku untuk semua program" dan
	 * dicocokkan lewat tahap-tahap {@code program IS NULL} di
	 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)}.
	 *
	 * <p>Berbeda dari {@link #getAngkatan()}, getter ini <b>tidak</b> menormalkan {@code null}
	 * menjadi string kosong &mdash; asimetri bawaan generator yang perlu diingat saat
	 * membandingkan nilai kedua kolom itu.</p>
	 *
	 * @return nama program cakupan, atau {@code null}
	 */
	public String getProgram() {
		return program;
	}

	/** @param program nama program cakupan; {@code null} berarti berlaku untuk semua program */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Angkatan yang menjadi cakupan baris ini (kolom {@code angkatan}), disimpan sebagai teks
	 * bebas sehingga bisa memuat beberapa angkatan sekaligus (mis. {@code "2021,2022"}).
	 *
	 * <p>Getter menormalkan hasilnya: {@code null} dikembalikan sebagai string kosong, dan spasi
	 * di ujung dipangkas. Normalisasi ini <b>tidak</b> ditulis balik ke field (berbeda dari
	 * {@link #getFakultas()}), jadi tidak berdampak ke database &mdash; murni kenyamanan
	 * pemanggil, terutama textbox angkatan di helper UI yang menerima hasilnya langsung.</p>
	 *
	 * <p>Pencocokannya di
	 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)} memakai
	 * {@code ILIKE '%angkatan%'} <b>atau</b> {@code IS NULL} <b>atau</b> {@code = ''}, sehingga
	 * baris tanpa angkatan berlaku untuk semua angkatan, dan pencocokan substring membuat nilai
	 * dua digit bisa "menabrak" angkatan empat digit.</p>
	 *
	 * @return angkatan cakupan yang sudah dipangkas, atau string kosong (tidak pernah {@code null})
	 */
	public String getAngkatan() {
		return angkatan == null ? "" : angkatan.trim();
	}

	/** @param angkatan angkatan cakupan; disimpan apa adanya (tanpa trim), kosong berarti semua angkatan */
	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * Fakultas yang menjadi cakupan baris ini (kolom {@code fakultas}) &mdash; <b>getter dengan
	 * efek samping, bukan pembaca murni.</b> Method ini melakukan dua hal sebelum mengembalikan
	 * nilai:
	 *
	 * <ol>
	 *   <li>meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan
	 *   menetapkannya kembali ke field (sama seperti getter relasi lain di kelas ini);</li>
	 *   <li><b>bila {@link #getJurusan()} terisi dan jurusan itu punya fakultas, field
	 *   {@code fakultas} DITIMPA dengan fakultas milik jurusan tersebut</b> &mdash; nilai yang
	 *   pernah di-set lewat {@link #setFakultas(Fakultas)} hilang tanpa peringatan.</li>
	 * </ol>
	 *
	 * <h3>Verifikasi: apakah timpaan ini benar-benar tertulis ke database?</h3>
	 *
	 * <p><b>Ya &mdash; sama persis seperti pada {@link ItemBiayaPunyaPiutang}.</b> Tiga syaratnya
	 * semuanya terpenuhi di kelas ini:</p>
	 *
	 * <ul>
	 *   <li><i>Property access</i> &mdash; {@code @Id} menempel pada {@link #getId()}, sehingga
	 *   Hibernate membaca <b>seluruh</b> state entity lewat getter, termasuk method ini. Nilai
	 *   yang dilihat mesin <i>dirty check</i> adalah nilai hasil timpaan, bukan nilai yang dimuat
	 *   dari database.</li>
	 *   <li><i>Entity dalam keadaan managed</i> &mdash; jalur pemicunya nyata dan rutin:
	 *   {@code ItemBiayaPunyaDenda.loadDataDetail(...)} memuat baris lewat
	 *   {@code HibernateUtil.currentSession().createCriteria(...)} (jadi managed), lalu
	 *   {@code initRow(...)} memanggil {@code getFakultas()} <b>dua kali</b> &mdash; sekali untuk
	 *   memilih item combobox fakultas, sekali lagi untuk membangun {@code Restrictions.eq(
	 *   "fakultas", ...)} yang menyaring daftar jurusan. Artinya <b>cukup dengan membuka tab
	 *   "Akun Pendapatan Denda"</b> pada jendela Item Biaya &mdash; tanpa menyunting apa pun,
	 *   tanpa menekan Simpan &mdash; kolom {@code fakultas} setiap baris berjurusan sudah ikut
	 *   ter-UPDATE saat session di-flush.</li>
	 *   <li><i>Perubahan ikut ter-flush</i> &mdash; {@code dynamicUpdate = true} membuat
	 *   pernyataan yang dihasilkan hanya menyentuh kolom yang berubah
	 *   ({@code UPDATE item_biaya_punya_pendapatan_denda SET fakultas = ? WHERE id = ?}), dan
	 *   karena kelas ini {@code @Audited}, setiap timpaan juga melahirkan <b>satu revisi
	 *   Envers</b> atas nama siapa pun yang kebetulan membuka layar itu.</li>
	 * </ul>
	 *
	 * <p>Sisi baiknya: timpaan ini bersifat <i>konvergen</i>. Setelah kolom fakultas sekali
	 * disamakan dengan fakultas jurusannya, pembacaan berikutnya menghasilkan nilai yang sama
	 * sehingga tidak ada UPDATE berulang &mdash; berbeda dari beberapa getter destruktif lain di
	 * model ini yang menulis {@code null} dan tidak pernah pulih. Yang tetap perlu disadari:
	 * fakultas yang sengaja diisi berbeda dari fakultas jurusannya <b>tidak bisa
	 * dipertahankan</b>, dan jejak Envers akan memuat revisi yang tidak pernah diniatkan
	 * pengguna.</p>
	 *
	 * <h3>Konsekuensi pada pencarian akun pendapatan denda</h3>
	 *
	 * <p>Karena baris ber-jurusan selalu ikut ber-fakultas, tahap pencarian yang hanya menyaring
	 * {@code fakultas = <fakultas>} (tanpa menyaring jurusan) di
	 * {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)} bisa memungut
	 * baris yang sebenarnya dibuat khusus untuk <b>jurusan lain</b> dalam fakultas yang sama.
	 * Dengan kata lain, pemetaan yang diniatkan spesifik per jurusan otomatis berperan juga
	 * sebagai fallback se-fakultas. Ini konsisten dengan urutan tahap (spesifik dulu, umum
	 * belakangan) sehingga jurusan yang punya barisnya sendiri tetap benar; yang perlu disadari
	 * adalah jurusan <i>tanpa</i> baris sendiri akan mewarisi akun denda milik jurusan tetangga
	 * alih-alih jatuh ke baris default. Untuk peran denda, akibat "salah akun" ini justru
	 * <b>lebih ringan</b> daripada akibat "tidak ada akun sama sekali", karena yang terakhir
	 * membatalkan seluruh posting pembayaran. Jangan "memperbaiki" perilaku ini tanpa memeriksa
	 * data pemetaan produksi lebih dulu &mdash; banyak instalasi kemungkinan sudah bergantung
	 * padanya.</p>
	 *
	 * @return fakultas cakupan (diturunkan dari jurusan bila jurusan terisi), atau {@code null}
	 * @see #getJurusan()
	 * @see ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		if (getJurusan() != null && getJurusan().getFakultas() != null) {
			fakultas = getJurusan().getFakultas();
		}
		return fakultas;
	}

	/**
	 * @param fakultas fakultas cakupan; <b>akan ditimpa</b> oleh {@link #getFakultas()} bila
	 *                 {@link #getJurusan()} terisi, jadi setter ini hanya benar-benar menentukan
	 *                 nilai akhir untuk baris yang jurusannya {@code null}
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

}
