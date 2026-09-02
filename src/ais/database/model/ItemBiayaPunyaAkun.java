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
 * Tabel jembatan <b>pemetaan pos biaya ke akun pendapatan</b>: satu baris menyatakan
 * "untuk {@link ItemBiaya} ini, pada cakupan Fakultas/Jurusan/Program/Angkatan ini, akun
 * pendapatan yang dipakai adalah {@link Akun} ini". Inilah sumber data yang membuat mesin
 * posting jurnal bisa menentukan akun <b>kredit</b> saat pembayaran mahasiswa/calon mahasiswa
 * dibukukan &mdash; tanpa baris di sini, layar Posting (Draft Jurnal) menandai transaksi
 * sebagai <i>"Transaksi tidak valid. Akun kredit tidak ada"</i>.
 *
 * <p>Memetakan tabel {@code public.item_biaya_punya_akun}. Berbeda dari master data biasa,
 * entity ini <b>tidak punya menu sendiri</b>: barisnya dikelola sebagai tab
 * <i>"Akun Pendapatan"</i> di dalam dialog detail menu <i>Item Biaya</i>
 * ({@code ais.action.master.ItemBiayaAction}), lewat
 * {@code ais.action.master.akunting.helper.ItemBiayaPunyaAkunHelper}.</p>
 *
 * <h3>Anggota keluarga pemetaan akun</h3>
 *
 * <p>Entity ini adalah salah satu dari lima tabel jembatan ber-struktur <b>identik</b> yang
 * menggantung pada {@link ItemBiaya}, masing-masing untuk satu peran akun:</p>
 *
 * <ul>
 *   <li>{@code ItemBiayaPunyaAkun} (kelas ini) &rarr; akun <b>pendapatan</b>, dibaca
 *   {@link ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaPiutang} &rarr; akun <b>piutang</b>;</li>
 *   <li>{@link ItemBiayaPunyaDibayarDimuka} &rarr; akun <b>pendapatan diterima di muka</b>;</li>
 *   <li>{@link ItemBiayaPunyaPendapatanDenda} &rarr; akun <b>pendapatan denda</b>;</li>
 *   <li>{@link ItemBiayaPunyaDiskon} &rarr; akun <b>diskon/potongan</b>.</li>
 * </ul>
 *
 * <p>Kelima kelas itu praktis salinan satu sama lain (termasuk kuirk {@link #getFakultas()} di
 * bawah), jadi perbaikan/temuan di satu kelas hampir selalu berlaku untuk keempat lainnya.</p>
 *
 * <h3>Cara baris ini dipilih saat posting (pencocokan berjenjang)</h3>
 *
 * <p>Baris tidak dicari dengan kunci tunggal, melainkan lewat <b>delapan tahap</b> di
 * {@link ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)}, dari konteks paling spesifik
 * (jurusan + program) sampai paling umum (fakultas {@code IS NULL} + program {@code IS NULL}),
 * berhenti pada hasil pertama yang ditemukan. Konsekuensi praktis untuk pengisi data:</p>
 *
 * <ul>
 *   <li>Baris dengan {@link #getJurusan() jurusan}, {@link #getFakultas() fakultas} dan
 *   {@link #getProgram() program} <b>kosong semua</b> berfungsi sebagai <b>default/fallback</b>
 *   untuk seluruh kombinasi yang tidak punya baris khusus.</li>
 *   <li>{@link #getAngkatan() angkatan} ikut disaring di <b>semua</b> tahap dengan pencocokan
 *   {@code ILIKE '%...%'} (substring, tidak peka besar-kecil huruf) <b>atau</b> {@code NULL}
 *   <b>atau</b> string kosong &mdash; sehingga satu baris bisa melayani banyak angkatan
 *   sekaligus bila ditulis berderet (mis. {@code "2021,2022,2023"}), dan baris tanpa angkatan
 *   berlaku untuk semua angkatan. Karena substring, angkatan dua digit ({@code "21"}) akan
 *   ikut cocok dengan baris {@code "2021"}; selalu tulis empat digit.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getFakultas()} adalah getter yang MENULIS.</b> Bila {@link #getJurusan()}
 *   terisi, getter ini menimpa field {@code fakultas} dengan fakultas milik jurusan tersebut.
 *   Karena pemetaan Hibernate kelas ini memakai <i>property access</i> (anotasi menempel pada
 *   getter), nilai hasil timpaan itulah yang ikut ter-flush ke kolom {@code fakultas} di
 *   database. Efeknya: baris yang di UI hanya diisi Jurusan tetap punya kolom Fakultas terisi
 *   di database &mdash; lihat catatan konsekuensinya pada javadoc {@link #getFakultas()}.</li>
 *   <li><b>Getter relasi memanggil {@code check(...)}</b>
 *   ({@link GeneralValueObject#check(Object)}) untuk meresolusi proxy lazy dan menetapkan
 *   hasilnya kembali ke field. Ini menulis balik ke field (bukan ke database dengan sendirinya)
 *   dan tidak pernah melempar exception; kegagalan resolusi bersifat senyap.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak biasa
 *   &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga Hibernate
 *   sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat baris
 *   di-<i>update</i>, bukan saat pertama dibuat. Riwayat lengkapnya tetap ada lewat
 *   {@code @Audited} (Hibernate Envers), yang mencatat setiap revisi baris ke tabel audit.</li>
 *   <li><b>{@link #toString()} degeneratif</b> &mdash; hasilnya selalu {@code "<id>-"} karena
 *   template hbm2java tidak pernah dilengkapi. Jangan diandalkan untuk label UI.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   generator Apr 2010); tidak ada hubungannya dengan entity {@code Bank}.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()}.</li>
 *   <li><b>Pasangan yang dipetakan</b>: {@link #getItemBiaya()} dan {@link #getAkun()}.</li>
 *   <li><b>Cakupan berlakunya pemetaan</b>: {@link #getFakultas()}, {@link #getJurusan()},
 *   {@link #getProgram()}, {@link #getAngkatan()}.</li>
 * </ul>
 *
 * <p>Tidak ada method bisnis/query statis di kelas ini; seluruh logika pencarian akun berada di
 * {@link ItemBiaya}, dan seluruh logika penyimpanan di helper UI-nya.</p>
 *
 * @see ItemBiaya
 * @see ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)
 * @see Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "item_biaya_punya_akun")

public class ItemBiayaPunyaAkun extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama baris pemetaan (kolom {@code id}, IDENTITY). */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String oleh;
	/** ID pengguna terakhir yang mengubah baris ini; diisi oleh {@link #onUpdate()}. */
	private String olehId;

	/** @return ID pengguna terakhir yang mengubah baris ini, atau {@code null} bila belum pernah di-update */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pengguna pengubah. <b>Menolak diam-diam</b> nilai {@code null} maupun string
	 * kosong/spasi: nilai lama dipertahankan alih-alih ditimpa, sehingga jejak audit terakhir
	 * tidak hilang saat interceptor dipanggil tanpa konteks pengguna (mis. proses terjadwal).
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null}/kosong <b>diabaikan</b> dan nilai lama dipertahankan.
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
	 * dari pengguna sesi berjalan lewat
	 * {@code ais.database.hibernate.AuditTimestampInterceptor.ubah(this)} tepat sebelum baris
	 * di-UPDATE. Tidak ada padanan {@code @PrePersist}, jadi pembuat baris tidak tercatat di
	 * kolom-kolom ini (lihat javadoc kelas). Pada baris deklarasi yang sama juga dideklarasikan
	 * field {@code tanggal_dirubah}, yang diinisialisasi ke waktu server saat objek dibuat
	 * ({@code ais.ui.util.WaktuUtil.getDate()}) sehingga baris baru tetap punya stempel waktu
	 * meski belum pernah di-update.
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
	 * hardcoded), mis. {@code "412-"}. Tidak berguna sebagai label UI &mdash; helper grid
	 * menampilkan {@code getAkun().getKode()}/{@code getAkun().getNama()} secara eksplisit,
	 * bukan hasil method ini.
	 *
	 * @return {@code id} diikuti tanda hubung
	 */
	public String toString() {
		return id + "-" + "";
	}

	/** Item biaya yang akunnya dipetakan oleh baris ini (kolom {@code item_biaya}). */
	private ItemBiaya itemBiaya;
	/** Akun pendapatan hasil pemetaan (kolom {@code akun}, skema {@code akunting}). */
	private Akun akun;
	/** Cakupan fakultas; lihat kuirk penurunan otomatis di {@link #getFakultas()}. */
	private Fakultas fakultas;
	/** Cakupan jurusan/program studi; {@code null} berarti berlaku lintas jurusan. */
	private Jurusan jurusan;
	/** Cakupan nama program (mis. Reguler/Karyawan); {@code null} berarti berlaku lintas program. */
	private String program;
	/** Cakupan angkatan sebagai teks bebas, dicocokkan sebagai substring; kosong berarti semua angkatan. */
	private String angkatan;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate; seluruh field diisi lewat setter. */
	public ItemBiayaPunyaAkun() {
	}

	/**
	 * @return kunci utama baris pemetaan, atau {@code null} bila baris belum pernah disimpan
	 *         (dipakai helper UI untuk membedakan baris baru dari baris tersimpan)
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id kunci utama baris; normalnya hanya diisi Hibernate */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Item biaya yang dipetakan (sisi "kiri" jembatan). Relasi lazy, sehingga getter ini
	 * meresolusi proxy lebih dulu lewat {@link GeneralValueObject#check(Object)} dan
	 * <b>menetapkan hasilnya kembali ke field</b> supaya pemanggil berikutnya tidak perlu
	 * meresolusi ulang; resolusi yang gagal bersifat senyap (proxy dikembalikan apa adanya).
	 *
	 * <p>Kolom {@code item_biaya} bersifat {@code nullable}, tetapi baris tanpa item biaya
	 * tidak akan pernah terpungut oleh resolver mana pun karena semua tahap pencarian menyaring
	 * {@code itemBiaya = <item biaya bersangkutan>}.</p>
	 *
	 * @return item biaya pemilik pemetaan ini, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "item_biaya", nullable = true)
	public ItemBiaya getItemBiaya() {
		itemBiaya = check(itemBiaya);
		return itemBiaya;
	}

	/**
	 * @param itemBiaya item biaya pemilik pemetaan; diisi helper UI saat baris dibuat dan
	 *                  di-set ulang oleh {@code ItemBiayaAction.onSave()} untuk item biaya yang
	 *                  baru saja mendapat id
	 */
	public void setItemBiaya(ItemBiaya itemBiaya) {
		this.itemBiaya = itemBiaya;
	}

	/**
	 * Akun akuntansi hasil pemetaan (sisi "kanan" jembatan) &mdash; inilah nilai yang akhirnya
	 * dipakai sebagai akun kredit pada draft jurnal pembayaran. Relasi lazy yang diresolusi
	 * lewat {@link GeneralValueObject#check(Object)} dan ditetapkan kembali ke field.
	 *
	 * <p>Kolom {@code akun} {@code nullable} di level database, tetapi
	 * {@code ItemBiayaAction.onSave()} menolak penyimpanan bila ada baris grid yang akunnya
	 * masih kosong &mdash; jadi baris ber-akun {@code null} secara praktis hanya bisa muncul
	 * dari impor/manipulasi data di luar layar ini.</p>
	 *
	 * @return akun pendapatan yang dipetakan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/** @param akun akun pendapatan yang dipetakan; wajib terisi agar baris lolos validasi simpan */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Jurusan/program studi yang menjadi cakupan pemetaan ini. {@code null} berarti baris
	 * berlaku lintas jurusan (dipungut oleh tahap pencarian {@code jurusan IS NULL}).
	 * Relasi lazy yang diresolusi lewat {@link GeneralValueObject#check(Object)} dan
	 * ditetapkan kembali ke field.
	 *
	 * <p>Perhatikan bahwa mengisi jurusan punya efek samping ke {@link #getFakultas()}:
	 * fakultas baris ini akan ikut terisi otomatis dari fakultas jurusan tersebut.</p>
	 *
	 * @return jurusan cakupan, atau {@code null} bila berlaku untuk semua jurusan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan jurusan cakupan; {@code null} agar baris berlaku lintas jurusan */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * Nama program (mis. Reguler/Karyawan/Kelas Malam) yang menjadi cakupan pemetaan.
	 * Disimpan sebagai teks apa adanya, dicocokkan dengan {@code =} (bukan substring) oleh
	 * resolver. {@code null} berarti baris berlaku lintas program.
	 *
	 * <p>Perhatikan asimetri dengan {@link #getAngkatan()}: getter ini <b>tidak</b> mengubah
	 * {@code null} menjadi string kosong, dan itu memang penting &mdash; empat dari delapan
	 * tahap pencarian menyaring {@code program IS NULL}, sehingga string kosong dan
	 * {@code null} berperilaku berbeda saat pencocokan.</p>
	 *
	 * @return nama program cakupan, atau {@code null}
	 */
	public String getProgram() {
		return program;
	}

	/** @param program nama program cakupan; {@code null} agar baris berlaku lintas program */
	public void setProgram(String program) {
		this.program = program;
	}

	/**
	 * Angkatan yang menjadi cakupan pemetaan, sebagai <b>teks bebas</b>. Boleh berisi beberapa
	 * angkatan sekaligus (mis. {@code "2021,2022,2023"}) karena resolver mencocokkannya sebagai
	 * substring ({@code ILIKE '%...%'}); kosong berarti berlaku untuk semua angkatan.
	 *
	 * <p><b>Normalisasi baca:</b> getter ini mengembalikan string kosong bila field
	 * {@code null}, dan selalu memangkas spasi tepi. Nilai yang dikembalikan karenanya bisa
	 * berbeda dari isi kolom di database &mdash; jangan pakai hasilnya untuk menyimpulkan
	 * apakah kolom {@code angkatan} benar-benar {@code NULL}. Normalisasi ini murni untuk
	 * kenyamanan tampilan ({@code new Textbox(...getAngkatan())} di helper grid), bukan bagian
	 * dari logika pencocokan &mdash; resolver membaca kolomnya langsung lewat SQL.</p>
	 *
	 * @return angkatan cakupan yang sudah di-trim, atau string kosong (tidak pernah {@code null})
	 */
	public String getAngkatan() {
		return angkatan == null ? "" : angkatan.trim();
	}

	/** @param angkatan angkatan cakupan (boleh berderet, mis. {@code "2021,2022"}); kosong berarti semua angkatan */
	public void setAngkatan(String angkatan) {
		this.angkatan = angkatan;
	}

	/**
	 * Fakultas yang menjadi cakupan pemetaan &mdash; <b>getter dengan efek samping, baca dengan
	 * saksama sebelum mengubah apa pun</b>.
	 *
	 * <p>Perilakunya dua lapis:</p>
	 * <ol>
	 *   <li>meresolusi proxy lazy lewat {@link GeneralValueObject#check(Object)} dan
	 *   menetapkannya kembali ke field (sama seperti getter relasi lain di kelas ini);</li>
	 *   <li><b>bila {@link #getJurusan()} terisi dan jurusan itu punya fakultas, field
	 *   {@code fakultas} DITIMPA dengan fakultas milik jurusan tersebut</b> &mdash; nilai yang
	 *   pernah di-set lewat {@link #setFakultas(Fakultas)} hilang tanpa peringatan.</li>
	 * </ol>
	 *
	 * <p>Karena pemetaan Hibernate kelas ini memakai <i>property access</i>, method inilah yang
	 * dibaca Hibernate saat baris di-flush; artinya hasil penurunan otomatis di atas <b>ikut
	 * tertulis ke kolom {@code fakultas} di database</b>, bukan sekadar tampil di layar. Baris
	 * yang di UI hanya diisi Jurusan (Fakultas dibiarkan kosong) tetap tersimpan dengan kolom
	 * Fakultas terisi.</p>
	 *
	 * <p><b>Konsekuensi pada pencarian akun.</b> Karena baris ber-jurusan selalu ikut ber-fakultas,
	 * tahap pencarian yang hanya menyaring {@code fakultas = <fakultas>} (tanpa menyaring jurusan)
	 * di {@link ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)} bisa memungut baris yang
	 * sebenarnya dibuat khusus untuk <b>jurusan lain</b> dalam fakultas yang sama. Dengan kata
	 * lain, pemetaan yang diniatkan spesifik per jurusan otomatis berperan juga sebagai fallback
	 * se-fakultas. Ini konsisten dengan urutan tahap (spesifik dulu, umum belakangan) sehingga
	 * jurusan yang punya barisnya sendiri tetap benar; yang perlu disadari adalah jurusan
	 * <i>tanpa</i> baris sendiri akan mewarisi akun milik jurusan tetangga alih-alih jatuh ke
	 * baris default. Jangan "memperbaiki" hal ini tanpa memeriksa data pemetaan produksi lebih
	 * dulu &mdash; banyak instalasi kemungkinan sudah bergantung pada perilaku ini.</p>
	 *
	 * @return fakultas cakupan (diturunkan dari jurusan bila jurusan terisi), atau {@code null}
	 * @see #getJurusan()
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
