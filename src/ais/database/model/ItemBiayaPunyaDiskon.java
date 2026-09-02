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
 * Tabel jembatan <b>pemetaan pos biaya ke akun diskon/potongan</b>: satu baris menyatakan
 * "untuk {@link ItemBiaya} ini, pada cakupan Fakultas/Jurusan/Program/Angkatan ini, akun
 * akuntansi yang mewakili diskon adalah {@link Akun} ini". Memetakan tabel
 * {@code public.item_biaya_punya_diskon}.
 *
 * <p>Seperti saudara-saudaranya, entity ini <b>tidak punya menu sendiri</b>: barisnya dikelola
 * sebagai tab <i>"Diskon"</i> di dalam dialog detail menu <i>Item Biaya</i>
 * ({@code ais.action.master.ItemBiayaAction}), lewat
 * {@code ais.action.master.akunting.helper.ItemBiayaPunyaDiskonHelper}.</p>
 *
 * <h3>Anggota keluarga pemetaan akun</h3>
 *
 * <p>Entity ini adalah salah satu dari lima tabel jembatan ber-struktur <b>identik</b> yang
 * menggantung pada {@link ItemBiaya}, masing-masing untuk satu peran akun:</p>
 *
 * <ul>
 *   <li>{@link ItemBiayaPunyaAkun} &rarr; akun <b>pendapatan</b>, dibaca
 *   {@link ItemBiaya#ambilAkun(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaPiutang} &rarr; akun <b>piutang</b>, dibaca
 *   {@link ItemBiaya#ambilPiutang(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaDibayarDimuka} &rarr; akun <b>pendapatan diterima di muka</b>,
 *   dibaca {@link ItemBiaya#ambilDibayarDimuka(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@link ItemBiayaPunyaPendapatanDenda} &rarr; akun <b>pendapatan denda</b>, dibaca
 *   {@link ItemBiaya#ambilPendapatanDenda(Fakultas, Jurusan, String, String)};</li>
 *   <li>{@code ItemBiayaPunyaDiskon} (kelas ini) &rarr; akun <b>diskon/potongan</b>.</li>
 * </ul>
 *
 * <p>Kelima kelas itu bukan sekadar "mirip": isi berkasnya <b>sama persis kata per kata</b>
 * kecuali nama kelas, nama tabel, dan nama field relasi &mdash; termasuk nilai
 * {@code serialVersionUID} yang sama untuk kelimanya. Setiap temuan/kuirk di satu kelas
 * otomatis berlaku untuk keempat lainnya.</p>
 *
 * <h3>Perbedaan penting: pemetaan ini TIDAK PERNAH DIBACA mesin akuntansi</h3>
 *
 * <p>Keempat saudaranya punya resolver di {@link ItemBiaya} (method {@code ambil*} dengan
 * algoritma pencarian berjenjang delapan tahap). <b>Kelas ini tidak punya</b>: tidak ada
 * {@code ambilDiskon()} di {@link ItemBiaya}, dan penelusuran seluruh berkas sumber terhadap
 * nama kelas ini maupun nama tabelnya hanya menemukan tiga pemakaian, semuanya di sekitar
 * layar Item Biaya sendiri:</p>
 *
 * <ol>
 *   <li>{@code ItemBiayaPunyaDiskonHelper.loadDataDetail(...)} &mdash; memuat baris milik satu
 *   item biaya ke grid tab "Diskon";</li>
 *   <li>{@code ItemBiayaAction} pada daftar item biaya &mdash; menampilkan label ringkas
 *   <i>"Akun diskon : &lt;kode-kode akun&gt;"</i> (proyeksi {@code groupProperty("akun.id")});</li>
 *   <li>{@code ItemBiayaAction.onSave()} &mdash; memvalidasi bahwa kolom akun tiap baris grid
 *   terisi, lalu {@code saveOrUpdate} seluruh barisnya.</li>
 * </ol>
 *
 * <p>Dengan kata lain baris di tabel ini <b>hanya ditulis dan dibaca kembali oleh layarnya
 * sendiri</b>; tidak ada satu pun jalur posting jurnal, laporan, atau ekspor yang memakainya.
 * Diskon yang benar-benar terbukukan berjalan lewat mekanisme lain sama sekali: item biaya
 * bertipe penghitungan {@link ItemBiaya#DIKALI_NILAI_MINUS} (komponen pengurang) diperlakukan
 * sebagai pos biaya bernilai negatif dan akunnya tetap diresolusi lewat
 * {@link ItemBiayaPunyaAkun}. Jadi mengubah/mengosongkan baris di tab "Diskon" tidak mengubah
 * hasil jurnal mana pun. Perlakukan tabel ini sebagai <b>data pemetaan yang belum
 * tersambung</b>, bukan sebagai konfigurasi akuntansi yang aktif; bila suatu saat resolver
 * {@code ambilDiskon()} ditambahkan, cukup mengikuti pola {@code ambilAkun()}.</p>
 *
 * <h3>Cakupan baris (bila kelak dipakai resolver)</h3>
 *
 * <p>Empat kolom cakupan &mdash; {@link #getFakultas() fakultas}, {@link #getJurusan() jurusan},
 * {@link #getProgram() program}, {@link #getAngkatan() angkatan} &mdash; semuanya opsional dan
 * bermakna sama seperti pada saudara-saudaranya: baris yang keempatnya kosong berperan sebagai
 * <b>default</b>, dan pada resolver keluarga ini angkatan dicocokkan sebagai <b>substring</b>
 * ({@code ILIKE '%...%'}) sehingga satu baris bisa ditulis berderet
 * (mis. {@code "2021,2022,2023"}). Karena substring, angkatan dua digit ({@code "21"}) akan ikut
 * cocok dengan {@code "2021"}; biasakan menulis empat digit.</p>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>{@link #getFakultas()} adalah getter yang MENULIS.</b> Bila {@link #getJurusan()}
 *   terisi, getter ini menimpa field {@code fakultas} dengan fakultas milik jurusan tersebut.
 *   Karena pemetaan Hibernate kelas ini memakai <i>property access</i> (anotasi menempel pada
 *   getter) dengan {@code dynamicUpdate = true}, nilai hasil timpaan itulah yang ikut ter-flush
 *   ke kolom {@code fakultas} &mdash; lengkap dengan revisi Envers baru. Lihat javadoc
 *   {@link #getFakultas()}.</li>
 *   <li><b>{@link #getAngkatan()} juga menormalkan nilai baca</b> ({@code null} &rarr; string
 *   kosong, plus {@code trim}). Konsekuensi yang sama seperti di atas berlaku: karena property
 *   access, nilai yang dikembalikan getter inilah yang dilihat Hibernate saat flush, sehingga
 *   kolom yang semula {@code NULL} bisa berubah menjadi string kosong pada UPDATE berikutnya.
 *   Di kelas ini dampaknya tidak terasa karena memang tidak ada resolver yang membedakan
 *   {@code NULL} dari string kosong.</li>
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
 *   {@code @Audited} (Hibernate Envers).</li>
 *   <li><b>{@link #toString()} degeneratif</b> &mdash; hasilnya selalu {@code "<id>-"} karena
 *   template hbm2java tidak pernah dilengkapi. Jangan diandalkan untuk label UI.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> (sisa salin-tempel
 *   generator Apr 2010); tidak ada hubungannya dengan entity {@code Bank}.</li>
 *   <li><b>Penyimpanan baris terjadi seketika, di luar tombol Simpan.</b> Setiap perubahan
 *   kombo/textbox pada baris grid langsung memanggil {@code Common.refreshSaveOrUpdate(...)} di
 *   helper, jadi baris bisa tersimpan sebelum pengguna menekan Simpan pada dialog item biaya.
 *   Validasi "akun wajib diisi" baru dijalankan di {@code ItemBiayaAction.onSave()}.</li>
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
 * <p>Tidak ada method bisnis maupun query statis di kelas ini; seluruh logika pemuatan,
 * penyimpanan, dan penghapusan baris berada di helper UI-nya.</p>
 *
 * @see ItemBiaya
 * @see ItemBiayaPunyaAkun
 * @see Akun
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "item_biaya_punya_diskon")
public class ItemBiayaPunyaDiskon extends GeneralValueObject {

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
	 * hardcoded), mis. {@code "412-"}. Untuk baris yang belum tersimpan hasilnya {@code "null-"}.
	 * Tidak berguna sebagai label UI &mdash; helper grid menampilkan
	 * {@code getAkun().getKode()}/{@code getAkun().getNama()} secara eksplisit, bukan hasil
	 * method ini.
	 *
	 * @return {@code id} diikuti tanda hubung
	 */
	public String toString() {
		return id + "-" + "";
	}

	/** Item biaya yang akun diskonnya dipetakan oleh baris ini (kolom {@code item_biaya}). */
	private ItemBiaya itemBiaya;
	/** Akun diskon/potongan hasil pemetaan (kolom {@code akun}, skema {@code akunting}). */
	private Akun akun;
	/** Cakupan jurusan/program studi; {@code null} berarti berlaku lintas jurusan. */
	private Jurusan jurusan;
	/** Cakupan nama program (mis. Reguler/Karyawan); {@code null} berarti berlaku lintas program. */
	private String program;
	/** Cakupan angkatan sebagai teks bebas; kosong berarti semua angkatan. */
	private String angkatan;
	/** Cakupan fakultas; lihat kuirk penurunan otomatis di {@link #getFakultas()}. */
	private Fakultas fakultas;

	/** Konstruktor tanpa argumen yang diwajibkan Hibernate; seluruh field diisi lewat setter. */
	public ItemBiayaPunyaDiskon() {
	}

	/**
	 * @return kunci utama baris pemetaan, atau {@code null} bila baris belum pernah disimpan
	 *         (dipakai helper UI untuk memutuskan apakah tombol hapus perlu memanggil
	 *         {@code session.delete(...)} atau cukup melepas baris grid)
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
	 * tidak akan pernah muncul di layar mana pun karena satu-satunya pembaca
	 * ({@code ItemBiayaPunyaDiskonHelper}) selalu menyaring
	 * {@code Restrictions.eq("itemBiaya", itemBiaya)}.</p>
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
	 * Akun akuntansi diskon hasil pemetaan (sisi "kanan" jembatan). Relasi lazy yang diresolusi
	 * lewat {@link GeneralValueObject#check(Object)} dan ditetapkan kembali ke field.
	 *
	 * <p>Perlu diingat (lihat javadoc kelas): nilai ini <b>tidak pernah dipakai mesin posting
	 * jurnal</b>. Pembacanya hanya grid tab "Diskon" dan label ringkas <i>"Akun diskon : ..."</i>
	 * pada daftar item biaya.</p>
	 *
	 * <p>Kolom {@code akun} {@code nullable} di level database, tetapi
	 * {@code ItemBiayaAction.onSave()} menolak penyimpanan bila ada baris grid kasat mata yang
	 * akunnya masih kosong. Baris ber-akun {@code null} tetap bisa lahir lebih dulu di database,
	 * karena helper menyimpan tiap perubahan kombo secara langsung sebelum validasi itu
	 * dijalankan.</p>
	 *
	 * @return akun diskon yang dipetakan, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/** @param akun akun diskon yang dipetakan; wajib terisi agar baris lolos validasi simpan */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Jurusan/program studi yang menjadi cakupan pemetaan ini. {@code null} berarti baris
	 * berlaku lintas jurusan. Relasi lazy yang diresolusi lewat
	 * {@link GeneralValueObject#check(Object)} dan ditetapkan kembali ke field.
	 *
	 * <p>Perhatikan bahwa mengisi jurusan punya efek samping ke {@link #getFakultas()}:
	 * fakultas baris ini akan ikut terisi otomatis dari fakultas jurusan tersebut, termasuk di
	 * database.</p>
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
	 * Nama program (mis. Reguler/Karyawan/Kelas Malam) yang menjadi cakupan pemetaan, disimpan
	 * sebagai teks apa adanya dari kombo {@code Common.initPrograms(...)}. {@code null} berarti
	 * baris berlaku lintas program.
	 *
	 * <p>Perhatikan asimetri dengan {@link #getAngkatan()}: getter ini <b>tidak</b> mengubah
	 * {@code null} menjadi string kosong, sehingga isi kolomnya dikembalikan apa adanya.</p>
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
	 * Angkatan yang menjadi cakupan pemetaan, sebagai <b>teks bebas</b> (textbox, bukan kombo).
	 * Boleh berisi beberapa angkatan sekaligus, mengikuti kebiasaan keluarga pemetaan ini yang
	 * mencocokkan angkatan sebagai substring; kosong berarti berlaku untuk semua angkatan.
	 *
	 * <p><b>Normalisasi baca dengan konsekuensi tulis:</b> getter ini mengembalikan string
	 * kosong bila field {@code null} dan selalu memangkas spasi tepi. Karena kelas ini dipetakan
	 * dengan <i>property access</i>, nilai kembalian inilah yang dibaca Hibernate saat flush
	 * &mdash; sehingga baris yang kolom {@code angkatan}-nya {@code NULL} bisa ikut ter-UPDATE
	 * menjadi string kosong (dan memunculkan revisi Envers) begitu baris tersebut kebetulan
	 * disimpan ulang. Jangan memakai hasil getter ini untuk menyimpulkan apakah kolomnya
	 * benar-benar {@code NULL}.</p>
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
	 * tertulis ke kolom {@code fakultas} di database</b> (beserta revisi Envers baru), bukan
	 * sekadar tampil di layar. Baris yang di UI hanya diisi Jurusan (Fakultas dibiarkan kosong)
	 * tetap tersimpan dengan kolom Fakultas terisi &mdash; dan penulisan itu bisa terjadi hanya
	 * karena barisnya kebetulan dibaca dalam sesi yang aktif, tanpa aksi simpan eksplisit dari
	 * pengguna.</p>
	 *
	 * <p>Penulisannya searah dan konsisten (fakultas selalu diturunkan ulang dari jurusan setiap
	 * kali getter dipanggil), jadi tidak ada risiko nilai bolak-balik; yang perlu disadari adalah
	 * kolom {@code fakultas} pada tabel ini <b>tidak bisa dipakai sebagai bukti bahwa operator
	 * memang memilih fakultas tersebut</b>. Nilainya juga langsung ikut tercermin di UI: helper
	 * memanggil {@code Common.selectComboItem(fakultas, itemBiayaPunyaDiskon.getFakultas())},
	 * sehingga kombo Fakultas akan menampilkan fakultas turunan meski operator tak pernah
	 * mengisinya.</p>
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
