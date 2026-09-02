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

/**
 * Entity <b>pohon wilayah administratif bergaya Feeder/PDDIKTI</b> (tabel
 * {@code public.wilayah}): satu tabel tunggal yang menyimpan provinsi, kota/kabupaten dan
 * kecamatan sekaligus, dibedakan hanya oleh kolom {@link #getLevel() level} dan dirangkai
 * menjadi pohon lewat relasi ke dirinya sendiri ({@link #getWilayahInduk() wilayahInduk}).
 *
 * <p>Tingkat yang dipakai di seluruh codebase:</p>
 *
 * <table border="1" summary="Arti nilai kolom level">
 *   <tr><th>{@code level}</th><th>Arti</th><th>Contoh pemakai</th></tr>
 *   <tr><td>{@code "1"}</td><td>Provinsi</td>
 *       <td>{@code AmbilDataPropinsiBanbox}, {@link Propinsi#simpanWilayah()}</td></tr>
 *   <tr><td>{@code "2"}</td><td>Kota/Kabupaten</td>
 *       <td>{@code AmbilDataKotaKabupatenBanbox}, {@link Kota#simpanWilayah()}</td></tr>
 *   <tr><td>{@code "3"}</td><td>Kecamatan (default getter)</td>
 *       <td>{@code AmbilDataKecamatanBanbox}, {@code WilayahKecamatanAction}</td></tr>
 * </table>
 *
 * <h3>Dua hierarki wilayah yang berjalan berdampingan</h3>
 *
 * <p>AIS menyimpan wilayah administratif <b>dua kali</b>, dan kelas ini adalah salah satu
 * dari dua hierarki itu (lihat pula {@link Propinsi} yang mendokumentasikan sisi
 * seberangnya):</p>
 *
 * <ol>
 *   <li><b>Hierarki klasik (tiga tabel terpisah)</b>: {@link Negara} &rarr; {@link Propinsi}
 *   &rarr; {@link Kota}. Hierarki ini <b>berhenti di kota/kabupaten</b> &mdash; tidak ada
 *   entity kecamatan untuk jalur umum ({@code ais.database.model.sirs.Kecamatan} khusus
 *   modul rumah sakit).</li>
 *   <li><b>Hierarki {@code Wilayah} (kelas ini &mdash; satu tabel self-reference)</b>:
 *   {@code level} membedakan jenjang, {@link #getInduk() induk} menyimpan <i>kode Feeder</i>
 *   induk sebagai teks, dan {@link #getWilayahInduk() wilayahInduk} adalah relasi
 *   {@code @ManyToOne} ke baris induk pada tabel yang sama. <b>Hanya di hierarki inilah
 *   kecamatan tersedia</b>, sehingga seluruh field "kecamatan" pada biodata
 *   ({@link BiodataMahasiswa#getKecamatan()}, {@link BiodataDosen#getKecamatan()},
 *   {@link OrangTua#getKecamatan()}, {@code BiodataCalonMahasiswa.kecamatanCalon/
 *   kecamatanSekolah/kecamatanOrtu}, {@code CalonPegawai}, {@code PenyediaAsset})
 *   menunjuk ke kelas ini, bukan ke {@link Kota}.</li>
 * </ol>
 *
 * <p><b>Jembatan arah maju (klasik &rarr; {@code Wilayah})</b> dikerjakan secara
 * <i>malas dan otomatis</i> oleh {@link Propinsi#simpanWilayah()} dan
 * {@link Kota#simpanWilayah()}: keduanya mencari baris {@code Wilayah} pasangan
 * berdasarkan <b>nama</b> ({@code ilike}), dan bila tidak ketemu <b>membuat baris baru di
 * tabel ini</b> lalu menyimpan referensinya di {@code Propinsi.wilayah}/{@code Kota.wilayah}.
 * Karena {@code PropinsiAction}/{@code KotaAction} memanggil method itu dari dalam
 * <i>row renderer</i>-nya, sekadar membuka atau membalik halaman layar master Provinsi/Kota
 * dapat <b>menyisipkan baris ke tabel {@code wilayah}</b> &mdash; operasi "baca" di sana
 * tidak benar-benar read-only.</p>
 *
 * <p><b>Jembatan arah balik ({@code Wilayah} &rarr; klasik)</b> ada di
 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)}: begitu pengguna
 * memilih kecamatan pada sebuah form biodata, listener menaiki dua tingkat
 * {@link #getWilayahInduk()} untuk mendapatkan nama provinsi, lalu mencocokkannya ke tabel
 * {@link Propinsi} dengan <b>jarak Levenshtein &lt; 2</b>. Bila tidak ada yang cukup mirip,
 * listener <b>membuat baris {@code Propinsi} baru</b> (dan, dengan pencocokan
 * {@code ilike EXACT}, bisa pula membuat baris {@link Kota} baru) langsung dari nama pada
 * pohon ini &mdash; tanpa ada seorang pun membuka layar master Provinsi/Kota. Jadi kedua
 * tabel dapat saling menumbuhkan isi satu sama lain sebagai efek samping penggunaan form
 * biasa. Karena pencocokan berbasis nama (bukan kode), penulisan yang berbeda tipis
 * (mis. {@code "Prop. Jawa Timur"} vs {@code "Jawa Timur"}, sudah ditangani sebagian dengan
 * membuang literal {@code "Prop."}) mudah melahirkan baris kembar di sisi klasik.</p>
 *
 * <h3>Dari mana isi tabel ini datang</h3>
 *
 * <ul>
 *   <li><b>Impor Feeder/Neo Feeder</b> &mdash; sumber utama. Jalur XML lama
 *   ({@code FeederImporter.wilayah()} + {@code FeederConverter.wilayah(Node)}) memetakan
 *   {@code id_wil}&rarr;{@link #getFeeder() feeder}, {@code nm_wil}&rarr;{@link #getNama()},
 *   {@code id_induk_wilayah}&rarr;{@link #getInduk()}, {@code id_level_wil}&rarr;
 *   {@link #getLevel()}, {@code id_negara}&rarr;{@link #getNegara()}; setelah semua baris
 *   masuk, ada lintasan kedua yang mengisi {@link #getWilayahInduk()} dengan mencari baris
 *   ber-{@code feeder} sama dengan {@code induk}. Jalur JSON baru
 *   ({@code FeederJSONImport.wilayah(JSONObject)}, dipicu tombol "Syn. Feeder" pada
 *   {@code WilayahKecamatanAction}) melakukan hal serupa &mdash; lihat catatan bug di
 *   {@link #getInduk()}.</li>
 *   <li><b>Sinkronisasi malas dari hierarki klasik</b> &mdash; {@link Propinsi#simpanWilayah()}
 *   (level {@code "1"}, {@code induk = "000000"}) dan {@link Kota#simpanWilayah()}
 *   (level {@code "2"}).</li>
 *   <li><b>Layar master kecamatan</b> {@code /pages/master/wilayah_kecamatan.zul}
 *   ({@code WilayahKecamatanAction}) &mdash; satu-satunya layar CRUD untuk entity ini;
 *   {@code level} di sana <b>dikunci ke {@code "3"}</b>.</li>
 *   <li><b>Tombol "Tambah Kecamatan Baru"</b> pada popup {@code AmbilDataKecamatanBanbox}
 *   &mdash; membuat baris level {@code "3"} inline sambil lebih dulu memanggil
 *   {@code simpanWilayah()} pada provinsi dan kota terpilih (sehingga satu klik bisa
 *   menyisipkan sampai tiga baris {@code Wilayah} sekaligus).</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang perlu diketahui sebelum menyentuh kelas ini</h3>
 *
 * <ol>
 *   <li><b>Pemetaan memakai <i>property access</i></b> (anotasi {@code @Id} menempel pada
 *   {@link #getId()}), sehingga Hibernate membaca <b>nilai yang dikembalikan getter</b> saat
 *   dirty-check/flush &mdash; bukan isi field. Semua getter yang mengganti nilai kosong
 *   dengan nilai bawaan di kelas ini karenanya <b>ikut mempersistensikan</b> nilai penggantinya
 *   pada update berikutnya. Lihat {@link #getFeeder()} ({@code "000000"}),
 *   {@link #getLevel()} ({@code "3"}), {@link #getNegara()} ({@code "ID"}),
 *   {@link #getKode()} ({@code ""}), {@link #getNama()} (ter-<i>trim</i>) dan
 *   {@link #getAktif()} ({@code true}).</li>
 *   <li><b>Hanya empat properti yang punya {@code @Column} eksplisit</b>
 *   ({@code id}, {@code nama}, {@code keterangan}, {@code tanggal_dirubah}); sisanya
 *   ({@code kode}, {@code level}, {@code induk}, {@code feeder}, {@code negara},
 *   {@code aktif}, {@code oleh}, {@code olehId}) mengandalkan penamaan default JPA.
 *   Perhatikan bahwa kolom {@code level} adalah kata kunci pada beberapa RDBMS (mis. Oracle);
 *   di PostgreSQL &mdash; skema yang dipakai AIS &mdash; ini aman.</li>
 *   <li><b>Deklarasi ulang {@code id}/{@code oleh}/{@code olehId}/{@code tanggal_dirubah}
 *   BUKAN duplikasi yang bisa dihapus.</b> {@link GeneralValueObject} adalah POJO abstrak
 *   biasa &mdash; bukan {@code @Entity} maupun {@code @MappedSuperclass} &mdash; sehingga
 *   Hibernate sama sekali tidak memetakan properti kelas induk. Setiap entity turunan wajib
 *   mendeklarasikan sendiri kolom-kolom itu agar terpetakan.</li>
 *   <li><b>Tidak ada jejak pembuat.</b> Ada {@code @PreUpdate} ({@link #onUpdate()}) tetapi
 *   tidak ada {@code @PrePersist}, sehingga {@code oleh}/{@code olehId} hanya terisi saat
 *   baris di-<i>update</i>, bukan saat pertama dibuat. Riwayat penuh tetap tersedia lewat
 *   {@code @Audited} (Hibernate Envers), dan layar master menampilkan tombol revisi
 *   ({@code RevisiHelper.createNewRevisi}).</li>
 *   <li><b>Seluruh tabel di-<i>preload</i> ke memori saat startup, berapa pun jumlah
 *   barisnya.</b> {@code Wilayah.class} terdaftar di {@code InitData} <i>dan</i> di
 *   {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN}; {@code InitDataHelper} memuat penuh kelas
 *   yang ditandai "jangan dibersihkan" tanpa memeriksa ambang
 *   {@code preload_maks_baris_kecil}. Untuk instalasi yang mengimpor seluruh wilayah
 *   Indonesia (puluhan ribu baris) ini berarti seluruh pohon menetap di cache in-memory.</li>
 *   <li><b>{@code kode} dan {@code feeder} adalah dua kode yang berbeda.</b> {@code kode}
 *   diisi dari kode provinsi/kota hierarki klasik saat disinkronkan
 *   ({@code Propinsi.getKode()}/{@code Kota.getKode()}), sedangkan {@code feeder} adalah
 *   {@code id_wilayah} milik PDDIKTI dan menjadi kunci rekonsiliasi impor. Layar master
 *   mencari pada <b>keduanya</b> dengan satu kotak pencarian, dan kolom "Kode Feeder Wilayah"
 *   hanya tampil bila {@code Common.getApakahAdminBolehAksesFeeder()} bernilai benar.</li>
 *   <li><b>Komentar generator "Bank generated by hbm2java" salah nama</b> &mdash; sisa
 *   salin-tempel template hbm2java dari {@link Bank} (sumber asli yang dibajak puluhan entity
 *   lain di codebase ini); tidak ada hubungannya dengan perbankan.</li>
 * </ol>
 *
 * <h3>Pengelompokan anggota kelas</h3>
 *
 * <ul>
 *   <li><b>Jejak audit</b>: {@link #getOleh()}/{@link #setOleh(String)},
 *   {@link #getOlehId()}/{@link #setOlehId(String)},
 *   {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas</b>: {@link #getId()}/{@link #setId(Long)}, {@link #toString()},
 *   konstruktor {@link #Wilayah()}.</li>
 *   <li><b>Atribut deskriptif</b>: {@link #getKode()}/{@link #setKode(String)},
 *   {@link #getFeeder()}/{@link #setFeeder(String)}, {@link #getNama()}/{@link #setNama(String)},
 *   {@link #getKeterangan()}/{@link #setKeterangan(String)},
 *   {@link #getAktif()}/{@link #setAktif(Boolean)}.</li>
 *   <li><b>Struktur pohon</b>: {@link #getLevel()}/{@link #setLevel(String)} (jenjang),
 *   {@link #getInduk()}/{@link #setInduk(String)} (kode Feeder induk, teks) dan
 *   {@link #getWilayahInduk()}/{@link #setWilayahInduk(Wilayah)} (relasi objek ke induk),
 *   serta {@link #getNegara()}/{@link #setNegara(String)} (akar pohon, kode negara sebagai
 *   teks &mdash; bukan relasi ke {@link Negara}).</li>
 * </ul>
 *
 * <p>Kelas ini <b>tidak memiliki method bisnis maupun method query statis</b>: seluruh
 * pencarian, penyaringan tingkat, dan penulisan dilakukan pemanggil lewat {@code Criteria}
 * masing-masing. Satu-satunya method yang bukan accessor murni adalah {@link #onUpdate()}
 * (callback JPA) dan {@link #toString()}. Berbeda dari {@link Propinsi} dan {@link Kota},
 * kelas ini tidak pernah menulis sendiri ke database.</p>
 *
 * @see Propinsi
 * @see Kota
 * @see Negara
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "wilayah")
public class Wilayah extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Wajib ada karena {@link GeneralValueObject} mengimplementasikan
	 * {@code Serializable} dan instance entity ini ikut diserialisasi ke cache in-memory/MapDB
	 * ({@code ConstantValues}, {@code MemoryCacheUtil}) serta ke state desktop ZK. Jangan diubah
	 * tanpa alasan: nilai baru membuat objek yang sudah tersimpan di cache lama tidak dapat
	 * dibaca kembali.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key ({@code IDENTITY}); lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini (kolom {@code olehid}, penamaan default JPA).
	 *
	 * <p>Diisi otomatis oleh {@code AuditTimestampInterceptor.ubah(...)} lewat {@link #onUpdate()};
	 * karena tidak ada {@code @PrePersist}, nilainya masih {@code null} pada baris yang belum
	 * pernah di-update sejak dibuat &mdash; termasuk baris hasil impor Feeder.</p>
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila baris belum pernah di-update
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir.
	 *
	 * <p><b>Perhatian:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menyentuh field), sehingga jejak audit yang sudah ada
	 * tidak dapat dikosongkan lewat setter ini. Ini pola seragam di seluruh entity AIS.</p>
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}, nilai {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (kolom {@code oleh}, penamaan default JPA).
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila baris belum pernah di-update
	 * @see #getOlehId()
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mencatat siapa dan kapan baris ini diubah.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)} dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang aktif.</p>
	 *
	 * <p><b>Dipanggil oleh Hibernate</b>, tidak pernah oleh kode aplikasi, dan <b>hanya pada
	 * UPDATE</b> &mdash; tidak ada {@code @PrePersist} di kelas ini sehingga baris yang baru
	 * disisipkan (mis. oleh {@link Propinsi#simpanWilayah()} atau impor Feeder) tidak membawa
	 * jejak pembuat.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir; diinisialisasi ke waktu server saat objek dibuat
	 * ({@code WaktuUtil.getDate()}) sehingga baris baru tidak pernah punya kolom kosong.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir.
	 *
	 * <p>Normalnya hanya dipanggil {@code AuditTimestampInterceptor} lewat {@link #onUpdate()};
	 * tidak ada validasi, sehingga pemanggil bebas menulis waktu apa pun (termasuk {@code null}).</p>
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Waktu perubahan terakhir (kolom {@code tanggal_dirubah}, presisi {@code TIMESTAMP}).
	 *
	 * @return waktu perubahan terakhir; untuk objek yang baru dibuat berisi waktu instansiasi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks untuk log/debug berformat {@code "id-kode-nama"}.
	 *
	 * <p><b>Berbeda dari {@link GeneralValueObject#toString()}</b> (yang berformat
	 * {@code "kode - nama"}) dan sengaja menampilkan {@code id} agar jejak impor Feeder
	 * mudah ditelusuri &mdash; {@code FeederImporter.wilayah()} dan
	 * {@code FeederJSONImport.wilayah(...)} mencetak objek ini apa adanya ke {@code stdout}.</p>
	 *
	 * <p><b>Kuirk:</b> method ini membaca <b>field mentah</b> {@code kode}/{@code nama}, bukan
	 * getter-nya, sehingga tidak ikut memangkas spasi dan menampilkan {@code null} apa adanya
	 * &mdash; hasilnya bisa berbeda dari nilai yang benar-benar tersimpan (lihat
	 * {@link #getKode()} dan {@link #getNama()}).</p>
	 *
	 * @return string {@code "<id>-<kode>-<nama>"} dari nilai field apa adanya
	 */
	public String toString() {
		return id + "-" + kode + "-" + nama;
	}

	/** Kode wilayah versi hierarki klasik; lihat {@link #getKode()}. */
	private String kode;
	/** Nama wilayah (provinsi/kota/kecamatan sesuai {@code level}); lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Jenjang pohon: {@code "1"} provinsi, {@code "2"} kota/kabupaten, {@code "3"} kecamatan. */
	private String level;
	/** Kode Feeder milik <i>induk</i>, disimpan sebagai teks; lihat {@link #getInduk()}. */
	private String induk;
	/** Kode wilayah PDDIKTI/Feeder ({@code id_wilayah}); lihat {@link #getFeeder()}. */
	private String feeder;
	/** Kode negara sebagai teks (akar pohon, mis. {@code "ID"}); lihat {@link #getNegara()}. */
	private String negara;

	/** Relasi {@code @ManyToOne} ke baris induk pada tabel yang sama; lihat {@link #getWilayahInduk()}. */
	private Wilayah wilayahInduk;
	/** Penanda aktif/nonaktif (tri-state di DB, dua-state di getter); lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate; seluruh field diisi lewat setter.
	 *
	 * <p>Dipakai langsung oleh {@code FeederConverter.wilayah(Node)},
	 * {@code FeederJSONImport.wilayah(JSONObject)}, {@link Propinsi#simpanWilayah()},
	 * {@link Kota#simpanWilayah()}, {@code WilayahKecamatanAction.onAdd(...)} dan
	 * {@code AmbilDataKecamatanBanbox}.</p>
	 */
	public Wilayah() {
	}

	/**
	 * Primary key baris ini (kolom {@code id}).
	 *
	 * <p>Dibangkitkan database ({@code IDENTITY}) sehingga berurutan dan mudah ditebak; kolom
	 * ditandai {@code insertable = false} karena nilainya diisi sepenuhnya oleh sequence.
	 * Anotasi {@code @Id} berada di getter ini &mdash; inilah yang membuat seluruh entity
	 * memakai <i>property access</i> (lihat catatan pada javadoc kelas).</p>
	 *
	 * @return primary key, atau {@code null} bila objek belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Normalnya hanya dilakukan Hibernate; mengubahnya pada objek yang
	 * sudah <i>persistent</i> tidak memindahkan baris, melainkan merusak identitas objek.
	 *
	 * @param id primary key
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama wilayah (kolom {@code nama}, {@code NOT NULL}, panjang 255) &mdash; provinsi,
	 * kota/kabupaten, atau kecamatan sesuai {@link #getLevel()}.
	 *
	 * <p><b>Nama adalah kunci pencocokan de facto antara kedua hierarki wilayah.</b>
	 * {@link Propinsi#simpanWilayah()} dan {@link Kota#simpanWilayah()} mencari pasangannya di
	 * sini dengan {@code Restrictions.ilike("nama", ...)}, dan
	 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)} memakai nilai ini untuk
	 * pencocokan Levenshtein arah sebaliknya. Konsekuensinya, mengubah nama sebuah baris dapat
	 * memutus pasangan yang sudah terbentuk dan memicu pembuatan baris kembar.</p>
	 *
	 * <p><b>Kuirk:</b> getter memangkas spasi tetapi <b>tidak menuliskannya balik</b> ke field;
	 * karena pemetaan memakai <i>property access</i>, versi ter-<i>trim</i> itulah yang
	 * dipersistensikan &mdash; nilai berspasi ekor akan "membersihkan diri" pada update
	 * berikutnya, sementara {@link #toString()} masih memakai field mentah.</p>
	 *
	 * @return nama wilayah tanpa spasi di ujung, atau {@code null} bila kolom kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama wilayah. Disimpan apa adanya (tanpa {@code trim}); pemangkasan baru terjadi
	 * saat dibaca lewat {@link #getNama()}.
	 *
	 * <p>Validasi "wajib diisi" hanya ada di layar master ({@code WilayahKecamatanAction.onSave});
	 * jalur impor Feeder dan {@code simpanWilayah()} tidak memvalidasi apa pun.</p>
	 *
	 * @param nama nama wilayah
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Catatan bebas untuk baris ini (kolom {@code keterangan}, boleh {@code null}).
	 *
	 * <p>Hanya diisi/ditampilkan lewat layar master kecamatan; tidak pernah diisi jalur impor
	 * Feeder maupun {@code simpanWilayah()}. Berbeda dari beberapa entity lain, properti ini
	 * <b>dideklarasikan ulang dan terpetakan</b> di sini sehingga isinya benar-benar tersimpan
	 * (bandingkan dengan {@code StatusAwalMahasiswa} yang kolom keterangannya tidak pernah
	 * tersimpan karena hanya mewarisi properti tak terpetakan dari {@link GeneralValueObject}).</p>
	 *
	 * @return catatan bebas, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas.
	 *
	 * @param keterangan catatan bebas; boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Kode wilayah versi PDDIKTI/Feeder ({@code id_wilayah}) &mdash; <b>kunci rekonsiliasi impor</b>.
	 *
	 * <p>Diisi oleh {@code FeederConverter.wilayah(Node)} (dari elemen {@code id_wil}) dan
	 * {@code FeederJSONImport.wilayah(JSONObject)} (dari properti {@code id_wilayah}); dipakai
	 * {@code FeederImporter.wilayah()} untuk menemukan baris lama sebelum menimpanya, dan untuk
	 * merangkai pohon pada lintasan kedua (mencocokkan {@code feeder} sebuah baris dengan
	 * {@link #getInduk() induk} baris anaknya). Layar master mencari pada kolom ini sekaligus
	 * pada {@link #getKode()} dengan satu kotak pencarian.</p>
	 *
	 * <p><b>Getter ini MENULIS BALIK ke field</b> (pola getter write-back): bila {@code feeder}
	 * masih {@code null}, field langsung diisi {@code "000000"} &mdash; sentinel Feeder untuk
	 * "tidak punya induk", nilai yang sama yang dipakai {@link Propinsi#simpanWilayah()} sebagai
	 * {@code induk} baris level 1. Karena pemetaan memakai <i>property access</i>, nilai
	 * pengganti ini <b>ikut ter-flush ke database</b> pada update berikutnya. Efeknya: baris apa
	 * pun yang belum pernah menerima kode Feeder (mis. hasil {@code simpanWilayah()} atau input
	 * manual tanpa kode) akan mengklaim kode Feeder {@code "000000"} yang sama, sehingga
	 * pencarian impor berbasis {@code feeder} ({@code setMaxResults(1)}) bisa mengenai baris
	 * sembarang dan menimpa nama/level/negara/induk-nya.</p>
	 *
	 * <p><b>Kuirk kecil:</b> ekspresi {@code feeder == null ? null : ...} pada baris
	 * {@code return} sudah tidak mungkin terpenuhi karena {@code feeder} baru saja dipastikan
	 * tidak {@code null}; cabang itu praktis kode mati. Nilai berisi spasi saja tetap
	 * mengembalikan {@code null} (dan, lewat property access, mengosongkan kolom) &mdash; baru
	 * pada pembacaan berikutnya ia menjadi {@code "000000"}.</p>
	 *
	 * @return kode Feeder ter-<i>trim</i>, atau {@code null} bila kolom hanya berisi spasi
	 */
	public String getFeeder() {
		if (feeder == null) {
			feeder = "000000";
		}
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/**
	 * Mengisi kode Feeder wilayah. Disimpan apa adanya; normalisasi terjadi di
	 * {@link #getFeeder()}.
	 *
	 * @param feeder kode wilayah PDDIKTI ({@code id_wilayah})
	 */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * Jenjang wilayah pada pohon: {@code "1"} provinsi, {@code "2"} kota/kabupaten,
	 * {@code "3"} kecamatan.
	 *
	 * <p>Nilai ini adalah <b>satu-satunya pembeda jenis baris</b> di tabel tunggal ini; seluruh
	 * popup pemilih menyaring dengan {@code Restrictions.eq("level", ...)}
	 * ({@code AmbilDataPropinsiBanbox} = {@code "1"}, {@code AmbilDataKotaKabupatenBanbox} =
	 * {@code "2"}, {@code AmbilDataKecamatanBanbox} = {@code "3"} secara default) dan layar
	 * master kecamatan mengunci {@code level} ke {@code "3"} baik saat mencari maupun menyimpan.</p>
	 *
	 * <p><b>Getter ini MENULIS BALIK ke field</b>: nilai {@code null} atau kosong langsung
	 * diganti {@code "3"} (kecamatan) dan &mdash; lewat <i>property access</i> &mdash; ikut
	 * tersimpan ke database pada update berikutnya. Artinya baris hasil impor yang kehilangan
	 * {@code id_level_wil} akan <b>diam-diam menjadi kecamatan</b>, muncul di popup kecamatan,
	 * dan hilang dari popup provinsi/kota.</p>
	 *
	 * @return jenjang wilayah sebagai teks; tidak pernah {@code null} (default {@code "3"})
	 */
	public String getLevel() {
		if (level == null || level.trim().isEmpty()) {
			level = "3";
		}
		return level;
	}

	/**
	 * Mengisi jenjang wilayah.
	 *
	 * <p>Tidak ada validasi terhadap himpunan nilai yang dikenal ({@code "1"}/{@code "2"}/
	 * {@code "3"}): nilai apa pun dari Feeder ({@code id_level_wil}) diterima apa adanya, dan
	 * baris ber-{@code level} di luar ketiganya menjadi tak terlihat oleh seluruh popup pemilih.</p>
	 *
	 * @param level jenjang wilayah sebagai teks
	 */
	public void setLevel(String level) {
		this.level = level;
	}

	/**
	 * Kode negara tempat wilayah ini berada, disimpan sebagai <b>teks</b> (mis. {@code "ID"}).
	 *
	 * <p>Perhatikan: ini <b>bukan</b> relasi ke entity {@link Negara} &mdash; nilainya disalin
	 * dari {@code Negara.getKode()} oleh {@link Propinsi#simpanWilayah()}/
	 * {@link Kota#simpanWilayah()}, atau dari elemen {@code id_negara} pada impor Feeder.
	 * Dipakai sebagai penyaring saat mencari pasangan provinsi di {@code simpanWilayah()}.</p>
	 *
	 * <p>Berbeda dari {@link #getFeeder()} dan {@link #getLevel()}, getter ini <b>tidak</b>
	 * menulis balik ke field: nilai bawaan {@code "ID"} hanya dikembalikan ke pemanggil.
	 * Namun karena pemetaan memakai <i>property access</i>, nilai bawaan itu tetap yang dibaca
	 * Hibernate saat flush, sehingga baris ber-{@code negara} kosong akan tersimpan sebagai
	 * {@code "ID"} pada update berikutnya &mdash; hasil akhirnya sama, hanya jalannya berbeda.</p>
	 *
	 * @return kode negara ter-<i>trim</i>; tidak pernah {@code null} (default {@code "ID"})
	 */
	public String getNegara() {
		return negara == null || negara.trim().isEmpty() ? "ID" : negara.trim();
	}

	/**
	 * Mengisi kode negara sebagai teks.
	 *
	 * @param negara kode negara (mis. {@code "ID"}); boleh {@code null} &mdash; pembacaan
	 *               berikutnya akan mengembalikan {@code "ID"}
	 */
	public void setNegara(String negara) {
		this.negara = negara;
	}

	/**
	 * Kode Feeder milik <b>baris induk</b>, disimpan sebagai teks (bukan foreign key).
	 *
	 * <p>Kolom ini adalah versi "lepas" dari {@link #getWilayahInduk()}: impor Feeder mengisi
	 * {@code induk} lebih dulu untuk semua baris, lalu pada lintasan kedua
	 * ({@code FeederImporter.wilayah()}) mencari baris ber-{@link #getFeeder() feeder} yang sama
	 * dengan nilai ini untuk mengisi relasi objeknya. Nilai {@code "000000"} berarti "tidak
	 * punya induk" (dipakai {@link Propinsi#simpanWilayah()} untuk baris level 1).</p>
	 *
	 * <p><b>Bug yang terlihat pada pemanggil (dicatat, tidak diperbaiki di sini):</b> jalur impor
	 * JSON {@code FeederJSONImport.wilayah(JSONObject)} &mdash; jalur yang dipakai tombol
	 * "Syn. Feeder" pada layar master kecamatan &mdash; mengisi {@code induk} dari properti
	 * {@code id_wilayah} (id baris itu sendiri), bukan {@code id_induk_wilayah}. Akibatnya
	 * setiap baris yang masuk lewat jalur JSON menunjuk dirinya sendiri sebagai induk, dan
	 * lintasan perangkaian pohon berbasis {@code induk} akan menghasilkan self-reference
	 * alih-alih hierarki. Jalur XML lama ({@code FeederConverter.wilayah(Node)}) memetakan
	 * {@code id_induk_wilayah} dengan benar.</p>
	 *
	 * <p>Getter ini murni &mdash; tidak ada normalisasi maupun nilai bawaan, sehingga bisa
	 * mengembalikan {@code null}.</p>
	 *
	 * @return kode Feeder induk, atau {@code null} bila belum diisi
	 */
	public String getInduk() {
		return induk;
	}

	/**
	 * Mengisi kode Feeder induk.
	 *
	 * <p>Tidak ada pemeriksaan konsistensi terhadap {@link #setWilayahInduk(Wilayah)}: kedua
	 * penunjuk induk dapat berbeda satu sama lain, dan hanya {@code wilayahInduk} yang benar-benar
	 * dipakai untuk menaiki pohon di runtime.</p>
	 *
	 * @param induk kode Feeder baris induk; {@code "000000"} berarti tanpa induk
	 */
	public void setInduk(String induk) {
		this.induk = induk;
	}

	/**
	 * Baris induk pada pohon yang sama (relasi self-reference, kolom {@code wilayah_induk}).
	 *
	 * <p>Inilah penunjuk induk yang benar-benar dipakai aplikasi: kecamatan (level {@code "3"})
	 * menunjuk kota/kabupaten (level {@code "2"}), yang menunjuk provinsi (level {@code "1"});
	 * baris provinsi biasanya {@code null}. Banyak pemanggil menaiki dua tingkat sekaligus
	 * &mdash; mis. {@code kecamatan.getWilayahInduk().getWilayahInduk().getNama()} di
	 * {@code BiodataPegawaiAction}, {@code AmbilDataKecamatanBanbox.WilayahRenderer} dan
	 * {@code Common.createKotaPropinsiListenerBerdasarkanKecamatan(...)} &mdash; sehingga baris
	 * yang relasi induknya belum terangkai membuat kolom "Kota/Kabupaten" dan "Propinsi" tampil
	 * kosong pada form biodata.</p>
	 *
	 * <p><b>Getter ini menulis balik ke field</b> karena memanggil
	 * {@link GeneralValueObject#check(Object)}: relasi dipetakan {@code FetchType.LAZY} sementara
	 * objek entity di AIS sering hidup lebih lama daripada {@code Session} yang memuatnya, jadi
	 * {@code check(...)} menyelesaikan proxy lewat {@code EntityIdentityMap}/cache
	 * {@code ConstantValues} dan hasilnya disimpan kembali ke field. Ini <b>bukan</b> getter
	 * destruktif dan tidak menutup sesi Hibernate &mdash; hanya penggantian referensi proxy
	 * dengan instance yang sudah termuat.</p>
	 *
	 * <p><b>Perhatian pada {@code cascade}:</b> relasi ini memakai
	 * {@code CascadeType.PERSIST, CascadeType.MERGE}, sehingga menyimpan sebuah kecamatan ikut
	 * menyimpan/menggabungkan baris induknya. Tidak ada pengaman siklus: bila
	 * {@code wilayahInduk} sampai menunjuk baris itu sendiri (lihat bug jalur JSON pada
	 * {@link #getInduk()}), penelusuran ke atas oleh pemanggil bisa berputar tanpa henti.</p>
	 *
	 * @return baris induk yang sudah diselesaikan dari proxy, atau {@code null} bila baris ini
	 *         adalah akar (biasanya provinsi) atau relasinya belum terangkai
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "wilayah_induk", nullable = true)
	public Wilayah getWilayahInduk() {
		wilayahInduk = check(wilayahInduk);
		return wilayahInduk;
	}

	/**
	 * Mengisi baris induk pada pohon.
	 *
	 * <p>Tidak ada validasi jenjang maupun siklus: pemanggil bebas memasang induk ber-{@code level}
	 * apa pun, termasuk baris itu sendiri. Perlu diketahui juga bahwa
	 * {@code WilayahKecamatanAction.onSave()} tidak hanya memanggil setter ini untuk kecamatan
	 * yang sedang disunting, tetapi <b>juga menulis ulang {@code wilayahInduk} milik
	 * kota/kabupaten induknya</b> dari isian "Propinsi" pada form &mdash; sehingga menyunting satu
	 * kecamatan dapat memindahkan seluruh kota/kabupaten (beserta semua kecamatan lain di
	 * bawahnya) ke provinsi yang berbeda.</p>
	 *
	 * @param wilayahInduk baris induk; {@code null} untuk akar pohon
	 */
	public void setWilayahInduk(Wilayah wilayahInduk) {
		this.wilayahInduk = wilayahInduk;
	}

	/**
	 * Kode wilayah versi hierarki klasik (kolom {@code kode}, penamaan default JPA).
	 *
	 * <p>Diisi dengan menyalin {@code Propinsi.getKode()}/{@code Kota.getKode()} saat baris
	 * dibentuk oleh {@link Propinsi#simpanWilayah()}/{@link Kota#simpanWilayah()}, atau diketik
	 * manual pada layar master kecamatan. <b>Bukan</b> kode Feeder &mdash; untuk itu ada
	 * {@link #getFeeder()}. Nilai ini juga dipakai {@link Kota#simpanWilayah()} dan
	 * {@code AmbilDataKecamatanBanbox} sebagai sumber isian {@link #setInduk(String)} baris anak,
	 * yang berarti pada baris hasil sinkronisasi kolom {@code induk} berisi kode klasik, bukan
	 * kode Feeder seperti pada baris hasil impor &mdash; dua semantik berbeda dalam satu kolom.</p>
	 *
	 * <p>Getter mengganti {@code null} dengan string kosong dan memangkas spasi tanpa menulis
	 * balik ke field; lewat <i>property access</i>, string kosong itulah yang tersimpan pada
	 * update berikutnya, sehingga kolom ini praktis tidak pernah {@code null} setelah baris
	 * pernah di-update.</p>
	 *
	 * @return kode wilayah ter-<i>trim</i>; tidak pernah {@code null} (string kosong bila belum diisi)
	 */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/**
	 * Mengisi kode wilayah versi hierarki klasik.
	 *
	 * @param kode kode wilayah; boleh {@code null}
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Penanda apakah baris ini masih dipakai.
	 *
	 * <p>Kolomnya tri-state di database ({@code true}/{@code false}/{@code null}) tetapi getter
	 * memetakan {@code null} menjadi {@code true} &mdash; <b>default berpihak "aktif"</b>,
	 * sehingga baris hasil impor Feeder maupun hasil {@code simpanWilayah()} (yang tidak pernah
	 * mengisi kolom ini) langsung dianggap aktif. Lewat <i>property access</i>, nilai pengganti
	 * ini ikut tersimpan pada update berikutnya.</p>
	 *
	 * <p><b>Catatan penting:</b> penyaringan berdasarkan nilai ini <b>tidak konsisten</b>.
	 * {@code AmbilDataKecamatanBanbox} menambahkan
	 * {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} pada pencarian tingkat 3,
	 * sedangkan {@code AmbilDataKotaKabupatenBanbox}, {@code AmbilDataPropinsiBanbox} dan layar
	 * master kecamatan sendiri tidak menyaring sama sekali &mdash; wilayah yang sudah dinonaktifkan
	 * tetap dapat dipilih sebagai kota/kabupaten atau provinsi.</p>
	 *
	 * @return {@code true} bila aktif atau kolom belum diisi; {@code false} hanya bila
	 *         dinonaktifkan secara eksplisit
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi penanda aktif/nonaktif.
	 *
	 * <p>Satu-satunya jalur UI adalah checkbox "Aktif" pada grid layar master kecamatan, yang
	 * dijaga dengan benar ({@code checkbox.setDisabled(!edit)} mengikuti
	 * {@code CommonPrivilages.UPDATE}) lalu langsung memanggil
	 * {@code Common.refreshSaveOrUpdate(...)} &mdash; perubahan tersimpan seketika tanpa tombol
	 * simpan.</p>
	 *
	 * @param aktif {@code true}/{@code false}; {@code null} akan dibaca sebagai {@code true}
	 *              oleh {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
