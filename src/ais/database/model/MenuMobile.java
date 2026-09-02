package ais.database.model;

// Generated 06 Jan 09 21:45:56 by Hibernate Tools 3.2.4.CR1

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.envers.Audited;

/**
 * Entity <b>pohon menu/navigasi versi mobile</b> (tabel {@code public.menu_mobile}) — sebuah
 * <b>salinan sederhana</b> dari {@link Menu} yang dulu dirancang sebagai daftar menu terpisah
 * khusus kanal mobile, dengan kolom yang persis subset {@code Menu}: hierarki
 * {@code root}/{@code child}, {@code label}, {@code url}, sepasang ikon
 * ({@code icon}/{@code big_icon}), sakelar {@code aktif}, {@code nomorUrut}, plus kolom jejak
 * audit standar. Dibangkitkan {@code hbm2java} pada 2009 bersamaan dengan {@code Menu}.
 *
 * <h3>PERINGATAN UTAMA: entity ini YATIM — tidak dipakai kode mana pun</h3>
 * <p>Penelusuran menyeluruh atas seluruh <i>source tree</i> (Java, ZUL, JSP, XML, JSON, SQL)
 * menemukan bahwa <b>tidak ada satu baris kode pun yang membaca atau menulis entity ini</b>.
 * Rujukan yang ada hanya tiga, dan tidak satu pun berupa pemakaian nyata:</p>
 * <ol>
 * <li>baris pemetaan {@code <mapping class="ais.database.model.MenuMobile" />} di
 * {@code hibernate.cfg.xml} — sehingga tabelnya tetap dikenal/dipelihara skema;</li>
 * <li>satu entri di manifes {@code WEB-INF/generic-crud/manifests/general_value_object_inventory}
 * berstatus {@code ELIGIBLE_METADATA_FIRST} (kandidat CRUD generik, <b>masih disabled</b>);</li>
 * <li>rujukan silang Javadoc dari {@link Menu} (justru untuk memperingatkan hal yang sama).</li>
 * </ol>
 * <p>Tidak ada {@code Action}, {@code Helper}, API, maupun layar ZUL yang menyentuhnya. Praktis
 * class ini adalah <b>sisa desain awal yang tak pernah selesai</b> (atau ditinggalkan setelah
 * arah kanal mobile berubah), yang tetap ikut ter-<i>deploy</i> karena masih terdaftar di
 * konfigurasi Hibernate.</p>
 *
 * <h3>DUA JEBAKAN PENAMAAN — apa yang SEBENARNYA melayani menu mobile</h3>
 * <p>Nama {@code MenuMobile}/{@code menu_mobile} bertabrakan dengan <b>dua</b> mekanisme lain
 * yang benar-benar berjalan, dan keduanya <b>tidak</b> memakai entity ini:</p>
 * <ol>
 * <li><b>Shell mobile ZK</b> — {@code ais.action.maintenance.MobileAction} memuat baris
 * {@link Menu} biasa milik peran pengguna, lalu {@code prepareMobileMenu(...)} menyalinnya
 * menjadi objek <b>{@code Menu} BARU</b> yang URL-nya dialihkan ke varian mobile sebelum
 * diserahkan ke {@code Common.launchMenu(...)}. Objek hasil salinan itu bertipe {@code Menu},
 * <b>bukan</b> {@code MenuMobile}. Kesalahpahaman ini mudah terjadi justru karena entity
 * {@code MenuMobile} ada dan namanya cocok.</li>
 * <li><b>Aplikasi Flutter (eCampus/eSchool)</b> — menu navigasinya diambil dari <b>baris
 * konfigurasi</b> berkunci {@code menu_mobile} di tabel {@code konfigurasi} (nilainya sebuah
 * string JSON berisi daftar item, ikon, label, dan peran yang berhak), dibaca klien lewat
 * {@code ApiCall.ambilMenuMobile()} dengan {@code action=konfigurasi&nama=menu_mobile}; lihat
 * dokumentasi kunci tersebut di {@code ais.action.master.KonfigurasiNewAction}. Kesamaan nama
 * {@code menu_mobile} dengan <b>tabel</b> {@code menu_mobile} di sini murni kebetulan —
 * sumbernya tabel {@code konfigurasi}, bukan entity ini.</li>
 * </ol>
 *
 * <h3>Perbedaan penting dengan {@link Menu}</h3>
 * <ul>
 * <li><b>Bukan unit hak akses.</b> Ini perbedaan paling berkonsekuensi. {@link Menu} adalah
 * unit granular hak akses — {@link Tbmrole} punya relasi {@code ManyToMany} ke {@code Menu}
 * lewat {@code job_has_menu}, dan {@link RolePrivilage} menautkan {@code Menu} ke peran dengan
 * flag {@code read}/{@code create}/{@code update}/{@code delete}/{@code approve}/{@code reject}.
 * {@code MenuMobile} <b>tidak punya relasi apa pun</b> ke {@code Tbmrole} maupun
 * {@code RolePrivilage}. Artinya, bila suatu saat entity ini dihidupkan sebagai sumber
 * navigasi, butir menunya <b>tidak akan punya gerbang hak akses sama sekali</b> kecuali
 * mekanisme baru dibangun lebih dulu.</li>
 * <li><b>Tanpa penyaring varian produk.</b> Tidak ada padanan {@code tampilDiPt}/
 * {@code tampilDiSekolah} maupun {@code bukaHalamanBaru}.</li>
 * <li><b>Tanpa kait invalidasi cache.</b> {@code Menu} punya kait {@code @PostPersist}/
 * {@code @PostUpdate}/{@code @PostRemove} yang menyegarkan cache menu UI Baru; di sini hanya ada
 * {@code @PreUpdate}. Konsisten dengan status yatimnya — tidak ada cache yang perlu disegarkan.</li>
 * <li><b>Urutan berbeda.</b> {@link #compareTo(GeneralValueObject)} di sini mengurutkan
 * <b>hanya berdasarkan {@code label}</b>, sedangkan {@code Menu} memakai kunci gabungan
 * {@code nomorUrut}+{@code root}+{@code child}. Akibatnya {@link #getNomorUrut()} di sini
 * praktis tidak berpengaruh pada pengurutan.</li>
 * <li><b>{@link #toString()} berbeda.</b> {@code Menu} mengembalikan {@code id + "-" + label};
 * di sini hanya {@code label}, dan boleh {@code null} (lihat catatan di method-nya).</li>
 * </ul>
 *
 * <h3>Relasi dengan {@code GeneralValueObject}</h3>
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}, yang
 * <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} melainkan POJO abstrak biasa —
 * Hibernate <b>tidak</b> memetakan properti induknya. Karena itu deklarasi ULANG {@link #id},
 * {@link #oleh}, {@link #olehId} dan {@link #tanggal_dirubah} di sini <b>bukan bug atau
 * duplikasi ceroboh</b>, melainkan keharusan teknis supaya keempat kolom itu ikut terpetakan.
 * Konsekuensinya field-field tersebut <b>membayangi (shadow)</b> field senama milik induk;
 * begitu pula {@link #nomorUrut} beserta {@link #getNomorUrut()} yang meng-<i>override</i>
 * versi {@code GeneralValueObject} (tipe sama, {@link Integer}). Yang terbaca dari luar selalu
 * versi milik {@code MenuMobile} ini.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; hierarki:</b> {@link #getId()} (PK; <b>tidak ada layar pengisi</b>,
 * sehingga fallback acak di getter-nya efektif jadi satu-satunya sumber ID — lihat catatan di
 * method-nya), {@link #getRoot()}, {@link #getChild()},
 * {@link #compareTo(GeneralValueObject)}, {@link #toString()}, plus empat konstruktor.</li>
 * <li><b>Isi/penyajian butir menu:</b> {@link #getLabel()}, {@link #getUrl()},
 * {@link #getIcon()} (ikon kecil), {@link #getBigIcon()} (ikon besar/ubin),
 * {@link #getNomorUrut()}.</li>
 * <li><b>Sakelar tampil:</b> {@link #getAktif()}.</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * </ol>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>Hierarki memakai {@code root}/{@code child}, BUKAN {@code id}/{@code parentId}</b> —
 * konvensi sama persis dengan {@link Menu}: {@code child} adalah kode node milik baris itu
 * sendiri, {@code root} adalah kode node induknya ({@code 0} untuk level atas). Tidak ada
 * <i>foreign key</i> formal yang menjaganya. Karena tidak ada layar pengelola, tidak ada pula
 * kode pembangkit {@code child} seperti {@code MenuAction} pada {@code Menu} — pengisian
 * harus dilakukan manual/lewat SQL.</li>
 * <li><b>Dua getter melakukan <i>lazy initialization</i> dan menulis balik ke field:</b>
 * {@link #getId()} dan {@link #getAktif()}. Sekadar <i>membaca</i> keduanya sudah mengubah
 * keadaan objek; pada objek terkelola (managed) Hibernate, perubahan itu bisa ikut ter-
 * <i>flush</i> ke database oleh <i>dirty checking</i>.</li>
 * <li><b>Tetapi {@link #getNomorUrut()} TIDAK menulis balik</b> — ia mengembalikan {@code 1}
 * bila field {@code null} tanpa menyimpannya, sehingga kolomnya tetap {@code NULL} di
 * database. Ketidakkonsistenan ini nyata (bandingkan dengan {@link #getAktif()}) dan mudah
 * menyesatkan saat men-<i>debug</i> beda antara nilai di layar dan nilai di tabel.</li>
 * <li><b>Tidak ada getter yang destruktif</b> (tidak ada yang menghapus/menimpa nilai bisnis)
 * dan <b>tidak ada getter yang membuka atau menutup sesi Hibernate</b> — semua getter di sini
 * murni membaca field, tanpa query dan tanpa {@code GeneralValueObject.check(...)}.</li>
 * <li><b>Tidak ada kait {@code @PrePersist}.</b> {@link #onUpdate()} hanya {@code @PreUpdate},
 * jadi pada {@code INSERT} pertama {@link #oleh}/{@link #olehId} <b>tidak</b> terisi otomatis;
 * hanya {@link #tanggal_dirubah} yang sudah punya nilai karena diinisialisasi saat objek
 * dibuat. Sama seperti {@link Menu}.</li>
 * <li><b>{@link #getAktif()}, {@link #getNomorUrut()} dan {@link #getTanggal_dirubah()} tidak
 * beranotasi {@code @Column}</b>, jadi nama kolomnya mengikuti nama properti apa adanya
 * ({@code aktif}, {@code nomorUrut}, {@code tanggal_dirubah}). Karena entity memakai
 * <i>property access</i> (anotasi {@code @Id} ada di getter), setiap getter publik ikut
 * terpetakan kecuali ditandai {@code @Transient}.</li>
 * <li><b>{@code @Audited}</b>: setiap perubahan direkam Hibernate Envers ke tabel bayangan
 * {@code menu_mobile_AUD}. {@code dynamicInsert}/{@code dynamicUpdate} membuat Hibernate hanya
 * menyertakan kolom yang benar-benar berubah pada SQL yang dihasilkan.</li>
 * <li><b>Risiko laten (bukan celah aktif).</b> Karena entity ini termasuk kelas yang terpetakan
 * Hibernate, ia ikut terjangkau endpoint CRUD reflektif generik {@code /Data} yang tidak punya
 * otorisasi per-kelas (temuan batch 22). Dampaknya <b>saat ini nihil</b> justru karena tidak ada
 * pembaca — menulis baris ke tabel ini tidak mengubah perilaku aplikasi mana pun. Namun bila
 * entity ini kelak dihidupkan sebagai sumber navigasi tanpa menutup celah tersebut lebih dulu,
 * ia berubah menjadi vektor penyisipan butir menu (termasuk {@link #url} sembarang, yang
 * <b>tidak divalidasi setter mana pun</b>).</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Menu
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "menu_mobile")
public class MenuMobile extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, diwarisi dari {@link java.io.Serializable} milik
	 * {@code GeneralValueObject}. Nilainya dibangkitkan {@code hbm2java} dan tidak perlu
	 * diubah; mengubahnya akan memutus kompatibilitas objek yang sudah ter-serialisasi.
	 */
	private static final long serialVersionUID = 7836173482067483952L;

	/**
	 * Primary key baris menu mobile. Dideklarasikan ulang dari {@code GeneralValueObject}
	 * karena keharusan pemetaan Hibernate (lihat Javadoc class). Perhatikan bahwa nilainya
	 * bisa terisi sendiri secara acak saat {@link #getId()} dipanggil — lihat catatan di sana.
	 */
	private Long id;

	/**
	 * Nama pengguna terakhir yang mengubah baris ini (jejak audit). Dideklarasikan ulang dari
	 * {@code GeneralValueObject} karena keharusan pemetaan Hibernate, bukan duplikasi.
	 */
	private String oleh;

	/**
	 * ID pengguna terakhir yang mengubah baris ini (jejak audit). Dideklarasikan ulang dari
	 * {@code GeneralValueObject} karena keharusan pemetaan Hibernate, bukan duplikasi.
	 */
	private String olehId;

	/**
	 * Mengembalikan ID pengguna terakhir yang mengubah baris menu mobile ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. <b>Menolak nilai kosong secara diam-diam</b>:
	 * bila {@code olehId} {@code null} atau hanya berisi spasi, method langsung {@code return}
	 * sehingga nilai lama <b>dipertahankan</b> — jejak audit tidak bisa dihapus dengan
	 * menimpanya memakai string kosong. Normalnya dipanggil
	 * {@code AuditTimestampInterceptor.ubah(...)} lewat kait {@link #onUpdate()}.
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
	 * Menyimpan nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)},
	 * nilai {@code null}/kosong <b>diabaikan diam-diam</b> agar jejak audit yang sudah ada
	 * tidak terhapus.
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
	 * Mengembalikan nama pengguna terakhir yang mengubah baris menu mobile ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil container persistence <b>tepat sebelum</b>
	 * {@code UPDATE} baris ini dikirim ke database. Mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} yang menyetel {@link #tanggal_dirubah} ke
	 * waktu server serta mengisi {@link #oleh}/{@link #olehId} dari pengguna yang sedang login
	 * (interceptor tersebut melewatkan pembaruan bila {@code AuditTrailHelper} menilai tidak
	 * ada perubahan bisnis).
	 * <p><b>Efek samping:</b> memutasi state objek ini. <b>Tidak berjalan pada {@code INSERT}</b>
	 * karena tidak ada kait {@code @PrePersist}. Jangan dipanggil manual dari kode aplikasi —
	 * hanya untuk dipicu oleh JPA. Method ini sekaligus implementasi wajib dari
	 * {@code protected abstract void onUpdate()} milik {@code GeneralValueObject}.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir baris ini. <b>Diinisialisasi ke waktu server saat objek
	 * dibuat</b> ({@code WaktuUtil.getDate()}), bukan {@code null}, sehingga baris baru selalu
	 * punya stempel waktu walau kait {@link #onUpdate()} belum pernah berjalan. Dideklarasikan
	 * ulang dari {@code GeneralValueObject} karena keharusan pemetaan Hibernate.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris ini. Menerima {@code null} apa adanya (tidak ada
	 * penjagaan seperti pada {@link #setOleh(String)}).
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris ini. Tidak beranotasi {@code @Column},
	 * jadi terpetakan ke kolom bernama sama dengan propertinya ({@code tanggal_dirubah}).
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di JVM ini, tetapi bisa {@code null} bila kolomnya kosong di database
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kode node <b>INDUK</b> di dalam pohon menu mobile, yaitu nilai {@link #child} milik baris
	 * di atasnya; {@code 0} untuk butir level atas. Lihat Javadoc class soal konvensi
	 * {@code root}/{@code child}.
	 */
	private Long root;

	/**
	 * Kode node <b>milik baris ini sendiri</b> di dalam pohon menu mobile — semacam nomor
	 * jalur, <b>bukan</b> "anak" meski namanya begitu. "Anak dari baris X" adalah semua baris
	 * yang {@code root}-nya sama dengan {@code child} milik X.
	 */
	private Long child;

	/** Teks butir menu yang dilihat pengguna; juga satu-satunya kunci pengurutan class ini. */
	private String label;

	/** Path/URL halaman tujuan butir menu ini. Tidak divalidasi setter mana pun. */
	private String url;

	/** Nama/berkas ikon kecil untuk tampilan daftar atau pohon menu. */
	private String icon;

	/** Nama/berkas ikon besar untuk tampilan ubin (tile) di layar mobile. */
	private String bigIcon;

	/** Sakelar tampil butir menu. Lihat {@link #getAktif()} soal default {@code true}. */
	private Boolean aktif;

	/**
	 * Nomor urut tampil. Membayangi field senama milik {@code GeneralValueObject}. Perhatikan
	 * bahwa {@link #compareTo(GeneralValueObject)} class ini <b>mengabaikannya</b>.
	 */
	private Integer nomorUrut;

	/**
	 * Mengembalikan representasi teks butir menu, yaitu {@link #label} apa adanya (dibaca
	 * langsung dari field, bukan lewat getter).
	 * <p><b>Perhatian:</b> berbeda dari {@code Menu.toString()} yang mengembalikan
	 * {@code id + "-" + label}, method ini <b>dapat mengembalikan {@code null}</b> bila label
	 * belum diisi — hal yang melanggar kebiasaan umum {@code toString()} dan berpotensi
	 * menimbulkan {@code NullPointerException} pada pemanggil yang langsung merantai method
	 * {@code String}. Tidak memanggil {@link #getId()}, jadi tidak memicu pembangkitan ID acak.</p>
	 *
	 * @return teks label butir menu, bisa {@code null}
	 */
	public String toString() {
		return label;
	}

	/**
	 * Mengembalikan nama/berkas ikon kecil butir menu ini.
	 *
	 * @return nama ikon kecil, atau {@code null} bila tidak diisi
	 */
	@Column(name = "icon")
	public String getIcon() {
		return icon;
	}

	/**
	 * Menyetel nama/berkas ikon kecil butir menu ini.
	 *
	 * @param icon nama ikon kecil; boleh {@code null}
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Constructor default tanpa argumen. WAJIB ada karena Hibernate membutuhkannya untuk
	 * membuat instance saat hidrasi entity dari hasil query.
	 */
	public MenuMobile() {
	}

	/**
	 * Constructor pintas yang mengisi posisi baris di dalam pohon menu.
	 * <p>Tidak mengisi {@link #id}; ID baru terbentuk saat {@link #getId()} dipanggil pertama
	 * kali (lihat catatan di sana).</p>
	 *
	 * @param root  kode node induk ({@code 0} untuk level atas)
	 * @param child kode node milik baris ini sendiri
	 */
	public MenuMobile(Long root, Long child) {
		this.root = root;
		this.child = child;
	}

	/**
	 * Constructor pintas yang mengisi posisi baris di dalam pohon menu beserta teks tampilnya.
	 *
	 * @param root  kode node induk ({@code 0} untuk level atas)
	 * @param child kode node milik baris ini sendiri
	 * @param label teks butir menu yang dilihat pengguna
	 */
	public MenuMobile(Long root, Long child, String label) {
		this.root = root;
		this.child = child;
		this.label = label;
	}

	/**
	 * Constructor pintas terlengkap: posisi di pohon, teks tampil, dan halaman tujuan.
	 *
	 * @param root  kode node induk ({@code 0} untuk level atas)
	 * @param child kode node milik baris ini sendiri
	 * @param label teks butir menu yang dilihat pengguna
	 * @param url   path/URL halaman tujuan
	 */
	public MenuMobile(Long root, Long child, String label, String url) {
		this.root = root;
		this.child = child;
		this.label = label;
		this.url = url;
	}

	/**
	 * Mengembalikan primary key baris ini, <b>dengan membangkitkan ID acak bila masih
	 * {@code null}</b>.
	 * <p><b>Efek samping (penting):</b> saat {@link #id} masih {@code null}, method ini
	 * membentuk nilai baru dari {@code Long.parseLong(RandomStringUtils.randomNumeric(6))} dan
	 * <b>menyimpannya</b> ke field. Jadi sekadar <i>membaca</i> ID sudah mengubah keadaan
	 * objek, dan pemeriksaan bergaya {@code menuMobile.getId() == null} <b>tidak akan pernah
	 * bernilai benar</b>. Pada objek terkelola Hibernate, mutasi ini bisa ikut ter-<i>flush</i>.</p>
	 * <p><b>Risiko yang lebih besar dibanding {@link Menu}:</b> pada {@code Menu} ID diisi
	 * manual oleh operator di layar menu dan pembangkitan acak hanya jadi jaring pengaman,
	 * sedangkan di sini <b>tidak ada layar apa pun</b> yang mengisinya sehingga fallback acak
	 * efektif menjadi satu-satunya sumber ID. Ruangnya hanya 6 digit (dan
	 * {@code randomNumeric(6)} dapat menghasilkan angka berawalan nol sehingga nilainya bisa
	 * jauh lebih kecil dari 100000), <b>tanpa pengecekan keunikan</b> — tabrakan primary key
	 * sepenuhnya mungkin dan akan muncul sebagai kegagalan {@code INSERT}. Entity ini tidak
	 * memakai {@code @GeneratedValue}; ID selalu ditentukan aplikasi.</p>
	 *
	 * @return primary key baris ini; tidak pernah {@code null} setelah pemanggilan pertama
	 */
	@Id
	@Column(name = "id", insertable = true, unique = true, nullable = false)
	public Long getId() {
		if (id == null) {
			id = Long.parseLong(RandomStringUtils.randomNumeric(6));
		}
		return this.id;
	}

	/**
	 * Menyetel primary key baris ini secara eksplisit.
	 *
	 * @param id primary key; {@code null} berarti "belum punya ID" — nilai acak akan dibentuk
	 *           pada pemanggilan {@link #getId()} berikutnya
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode node <b>INDUK</b> baris ini di dalam pohon menu mobile. Berbeda dengan
	 * {@code Menu.getRoot()}, method ini <b>tidak</b> memaksakan nilai default apa pun.
	 *
	 * @return kode node induk ({@code 0} untuk butir level atas), atau {@code null} bila kolomnya
	 *         kosong
	 */
	@Column(name = "root", nullable = true)
	public Long getRoot() {
		return this.root;
	}

	/**
	 * Menyetel kode node induk baris ini.
	 *
	 * @param root kode node induk; {@code 0} untuk butir level atas
	 */
	public void setRoot(Long root) {
		this.root = root;
	}

	/**
	 * Mengembalikan kode node <b>milik baris ini sendiri</b> di dalam pohon menu mobile —
	 * nilai inilah yang dirujuk oleh {@link #getRoot()} baris-baris anaknya. Berbeda dengan
	 * {@code Menu.getChild()}, method ini <b>tidak</b> memaksakan nilai default apa pun.
	 *
	 * @return kode node baris ini, atau {@code null} bila kolomnya kosong
	 */
	@Column(name = "child", nullable = true)
	public Long getChild() {
		return this.child;
	}

	/**
	 * Menyetel kode node milik baris ini. Tidak ada pembangkit otomatis seperti
	 * {@code MenuAction} pada {@code Menu}; nilai harus disiapkan pemanggil.
	 *
	 * @param child kode node baris ini
	 */
	public void setChild(Long child) {
		this.child = child;
	}

	/**
	 * Mengembalikan teks butir menu yang dilihat pengguna. Nilai ini juga menjadi
	 * satu-satunya kunci pengurutan {@link #compareTo(GeneralValueObject)} dan isi
	 * {@link #toString()}.
	 *
	 * @return teks label, atau {@code null} bila tidak diisi
	 */
	@Column(name = "label", nullable = true, length = 100)
	public String getLabel() {
		return this.label;
	}

	/**
	 * Menyetel teks butir menu. Tidak ada validasi panjang di sisi Java — batas 100 karakter
	 * hanya berlaku di tingkat kolom database.
	 *
	 * @param label teks butir menu; boleh {@code null}
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Mengembalikan path/URL halaman tujuan butir menu ini. Berbeda dengan
	 * {@code Menu.getUrl()} yang menggantikan {@code null} dengan nilai default, method ini
	 * mengembalikan isi field apa adanya.
	 *
	 * @return path/URL tujuan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "url", length = 100)
	public String getUrl() {
		return this.url;
	}

	/**
	 * Menyetel path/URL halaman tujuan butir menu ini.
	 * <p><b>Tidak ada validasi sama sekali</b> — nilai apa pun diterima apa adanya, termasuk
	 * URL absolut ke host luar. Saat ini tidak berbahaya karena tidak ada kode yang membaca
	 * entity ini, tetapi wajib diperhatikan bila entity ini kelak dihidupkan.</p>
	 *
	 * @param url path/URL tujuan; boleh {@code null}
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Menyetel nama/berkas ikon besar (tampilan ubin) butir menu ini.
	 *
	 * @param bigIcon nama ikon besar; boleh {@code null}
	 */
	public void setBigIcon(String bigIcon) {
		this.bigIcon = bigIcon;
	}

	/**
	 * Mengembalikan nama/berkas ikon besar (tampilan ubin) butir menu ini.
	 *
	 * @return nama ikon besar, atau {@code null} bila tidak diisi
	 */
	@Column(name = "big_icon", length = 255)
	public String getBigIcon() {
		return bigIcon;
	}

	/**
	 * Membandingkan dua butir menu mobile <b>hanya berdasarkan {@link #label}</b> secara
	 * leksikografis. Meng-override {@code GeneralValueObject.compareTo(...)} yang memakai kunci
	 * gabungan {@code nomorUrut}/{@code nim}/{@code nama}/{@code keterangan}, dan berbeda pula
	 * dari {@code Menu.compareTo(...)} yang memakai {@code nomorUrut}+{@code root}+{@code child}.
	 * Akibatnya {@link #getNomorUrut()} <b>tidak berpengaruh</b> pada urutan tampil class ini.
	 * <p><b>Dua jebakan:</b> (1) argumen di-<i>cast</i> ke {@code MenuMobile} <b>tanpa</b>
	 * pemeriksaan {@code instanceof}, sehingga membandingkan objek ini dengan
	 * {@code GeneralValueObject} jenis lain melempar {@code ClassCastException} — perhatikan
	 * bahwa {@code cast} terjadi <b>sebelum</b> pemeriksaan {@code null} pada baris berikutnya;
	 * (2) bila salah satu label {@code null} hasilnya {@code 0}, yang oleh {@code TreeSet}/
	 * {@code TreeMap} dibaca sebagai "sama" sehingga butir-butir tersebut saling menyingkirkan.</p>
	 *
	 * @param object butir menu mobile pembanding; harus bertipe {@code MenuMobile}
	 * @return hasil {@code String.compareTo} antar label, atau {@code 0} bila salah satu label
	 *         (atau argumennya) {@code null}
	 * @throws ClassCastException bila {@code object} bukan {@code MenuMobile}
	 */
	@Override
	public int compareTo(GeneralValueObject object) {
		MenuMobile arg0 = (MenuMobile) object;
		if (arg0 == null || arg0.getLabel() == null || this.label == null) {
			return 0;
		} else {
			return this.label.compareTo(arg0.getLabel());
		}
	}

	/**
	 * Menyetel sakelar tampil butir menu ini.
	 *
	 * @param aktif {@code true} bila butir ditampilkan; {@code null} akan dibaca sebagai
	 *              {@code true} (lihat {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sakelar tampil butir menu ini, dengan default {@code true}.
	 * <p><b>Efek samping:</b> bila field masih {@code null}, method <b>menulis
	 * {@code true} ke field</b> sebelum mengembalikannya — jadi membaca sakelar ini mengubah
	 * keadaan objek dan, pada objek terkelola Hibernate, dapat ikut ter-<i>flush</i> menjadi
	 * {@code UPDATE}. Karena itu pula method ini <b>tidak pernah</b> mengembalikan {@code null},
	 * sehingga pemeriksaan bergaya {@code x.getAktif() == null || x.getAktif()} akan selalu
	 * jatuh ke cabang kedua. Tidak beranotasi {@code @Column}; kolomnya bernama {@code aktif}
	 * mengikuti nama properti.</p>
	 *
	 * @return {@code true} bila butir menu ditampilkan; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil butir menu ini, dengan default {@code 1} bila belum
	 * diisi. Meng-override versi {@code GeneralValueObject} dan membaca field milik class ini.
	 * <p><b>Berbeda dari {@link #getAktif()}, method ini TIDAK menulis balik nilai default ke
	 * field</b>: kolomnya tetap {@code NULL} di database sementara kode Java melihat
	 * {@code 1}. Ketidakkonsistenan ini nyata dan mudah menyesatkan saat men-<i>debug</i>.
	 * Perlu dicatat juga bahwa nilainya <b>tidak dipakai</b> oleh
	 * {@link #compareTo(GeneralValueObject)} class ini. Tidak beranotasi {@code @Column};
	 * kolomnya bernama {@code nomorUrut} mengikuti nama properti.</p>
	 *
	 * @return nomor urut tampil; {@code 1} bila field masih {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil butir menu ini.
	 *
	 * @param nomorUrut nomor urut; {@code null} akan dibaca sebagai {@code 1} oleh
	 *                  {@link #getNomorUrut()}
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}
}
