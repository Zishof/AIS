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
 * Entity <b>MASTER menu/navigasi aplikasi</b> (tabel {@code public.menu}) — satu baris mewakili
 * satu butir menu yang dapat muncul di navigasi AIS, mulai dari grup level-atas ("Akademik",
 * "Keuangan", …) sampai daun yang benar-benar membuka satu halaman ZUL. Baris {@code Menu}
 * dipakai serentak oleh <b>tiga hal berbeda</b>: (1) sumber data pohon navigasi di semua shell UI
 * (UI lama, "UI Baru", mobile, dan desktop native), (2) <b>unit granular hak akses</b> —
 * {@link Tbmrole} punya relasi {@code ManyToMany} ke {@code Menu} lewat tabel gabung
 * {@code job_has_menu}, dan {@link RolePrivilage} menautkan satu {@code Menu} ke satu peran dengan
 * flag {@code read}/{@code create}/{@code update}/{@code delete}/{@code approve}/{@code reject},
 * dan (3) <b>kunci audit jejak pemakaian</b> — {@code DisplayMenu} menyimpan menu yang sedang
 * dibuka ke atribut session {@code currentMenu} dan mencatat {@code DetailLogLogin}.
 *
 * <p>Class ini turunan langsung {@link ais.database.model.GeneralValueObject}. Ingat bahwa
 * {@code GeneralValueObject} <b>bukan</b> {@code @Entity} maupun {@code @MappedSuperclass} —
 * ia POJO abstrak biasa dan Hibernate <b>tidak</b> memetakan properti induknya. Karena itu
 * deklarasi ULANG {@link #id}, {@link #oleh}, {@link #olehId} dan {@link #tanggal_dirubah} di
 * sini <b>bukan bug/duplikasi ceroboh</b>, melainkan keharusan teknis supaya keempat kolom
 * tersebut ikut terpetakan. Konsekuensinya field-field itu <b>membayangi (shadow)</b> field
 * senama milik induk — begitu pula {@link #nomorUrut} beserta {@link #getNomorUrut()} yang
 * meng-<i>override</i> versi {@code GeneralValueObject} (tipe sama, {@link Integer}); yang dibaca
 * oleh kode di luar selalu versi milik {@code Menu} ini.</p>
 *
 * <h3>Struktur hierarki: {@code root}/{@code child}, BUKAN {@code id}/{@code parentId}</h3>
 * <p>Ini bagian paling tidak intuitif dari entity ini. Kolom {@link #id} <b>tidak</b> berperan
 * sebagai kunci hierarki sama sekali. Hierarki dibentuk oleh sepasang kolom:</p>
 * <ul>
 * <li>{@link #getChild() child} = <b>kode node milik menu ini sendiri</b> di dalam pohon
 * (semacam "nomor jalur"/materialized code, bukan "anak"; namanya menyesatkan);</li>
 * <li>{@link #getRoot() root} = <b>kode node INDUK</b>, yaitu nilai {@code child} milik menu di
 * atasnya. Menu level-atas memakai {@code root == 0}.</li>
 * </ul>
 * <p>Jadi "anak dari menu X" = semua baris {@code Menu} yang {@code root}-nya sama dengan
 * {@code child} milik X — persis pola yang dipakai {@code MenuTreeModel},
 * {@code MainMenuHelper.createRootSubMenu()}, {@code MainTreeMenuHelper},
 * {@code MainBaruMenuHelper}, {@code TbmroleAction.createRootSubMenu()}, dan
 * {@code MainHelper.hasChild()}. Kode {@code child} untuk anak baru dibangkitkan
 * {@code MenuAction} dengan menyambung dua digit: {@code Long.parseLong("" + induk.getChild() +
 * "00")} untuk anak pertama, lalu {@code max(child sibling) + 1} untuk berikutnya — sehingga
 * kedalaman pohon "memakan" 2 digit per level. <b>Tidak ada foreign key formal</b> yang menjaga
 * konsistensi pasangan ini; keutuhan pohon sepenuhnya bergantung pada disiplin pengisian data
 * oleh operator menu.</p>
 *
 * <h3>Pengelompokan anggota</h3>
 * <ol>
 * <li><b>Identitas &amp; hierarki:</b> {@link #getId()} (PK, <b>diisi manual</b> oleh operator di
 * layar menu; punya fallback acak — lihat catatan di method-nya), {@link #getRoot()},
 * {@link #getChild()}, {@link #compareTo(GeneralValueObject)} (urutan tampil pohon),
 * {@link #toString()}.</li>
 * <li><b>Isi/penyajian butir menu:</b> {@link #getLabel()} (teks yang dilihat pengguna;
 * dipakai juga sebagai kunci keunikan di {@code MenuAction.checkKodeMenu()}),
 * {@link #getUrl()} (path ZUL/URL tujuan), {@link #getIcon()} dan {@link #getBigIcon()}
 * (ikon kecil untuk pohon/daftar, ikon besar untuk halaman ubin {@code BlankAction} dan
 * {@code AmbilDataMenuBanyak}), {@link #getNomorUrut()}.</li>
 * <li><b>Sakelar tampil/perilaku:</b> {@link #getAktif()}, {@link #getTampilDiPt()} dan
 * {@link #getTampilDiSekolah()} (penyaring varian produk: instalasi Perguruan Tinggi vs
 * Yayasan/Sekolah), {@link #getBukaHalamanBaru()} (buka di tab browser baru lewat redirect,
 * bukan di dalam {@code Tabbox} aplikasi).</li>
 * <li><b>Jejak audit (deklarasi ulang milik induk):</b> {@link #getOleh()},
 * {@link #getOlehId()}, {@link #getTanggal_dirubah()}, kait {@link #onUpdate()}.</li>
 * <li><b>Kait daur hidup cache:</b> {@link #invalidateNewUiMenuCache()} — satu-satunya method
 * di class ini yang menyentuh keadaan global JVM.</li>
 * </ol>
 *
 * <h3>Pola pemakaian umum (± 78 file merujuk class ini)</h3>
 * <ul>
 * <li><b>Membangun navigasi:</b> {@code MainAction}/{@code MainHelper} +
 * {@code MainMenuHelper}/{@code MainTreeMenuHelper}/{@code MainBaruMenuHelper} memuat seluruh
 * {@code Menu} milik role aktif ({@code Tbmrole.getMenus()}), lalu menyaring
 * {@link #getAktif()} dan {@link #getTampilDiPt()}/{@link #getTampilDiSekolah()} sebelum
 * merender pohon secara rekursif berdasarkan {@code root}/{@code child}.</li>
 * <li><b>Membuka halaman:</b> {@code Common.launchMenu(...)} membaca
 * {@link #getBukaHalamanBaru()} dan {@link #getUrl()}.</li>
 * <li><b>Pengelolaan data menu:</b> {@code ais.action.maintenance.MenuAction} dan
 * {@code ais.action.master.MenuAction} (dua layar CRUD yang hampir kembar) +
 * {@code MenuTreeModel}.</li>
 * <li><b>Hak akses:</b> {@code TbmroleAction} (pohon centang hak akses per peran),
 * {@code RolePrivilage}, {@code GenericCrudRoutePrivilegeResolver},
 * {@code JurnalAuthorizationService}, {@code HakAksesApi}/{@code GrupPenggunaAksesApi}.</li>
 * <li><b>Kanal non-web-desktop:</b> {@code MobileAction} (menyalin {@code Menu} menjadi objek
 * {@code Menu} baru yang URL-nya dialihkan ke varian mobile — <b>bukan</b> ke entity
 * {@link MenuMobile}), {@code DesktopMenuBootstrap}/{@code DesktopNativeApi},
 * {@code NewUiHybridMenuAccessService}.</li>
 * <li><b>Rekonsiliasi otomatis:</b> {@code JurnalMenuReconciler} membuat/menyelaraskan baris
 * {@code Menu} modul Jurnal secara idempoten saat startup dengan konvensi
 * {@code id = 2000000000L + child}.</li>
 * </ul>
 *
 * <h3>Hal non-obvious yang WAJIB diketahui pemelihara</h3>
 * <ul>
 * <li><b>Banyak getter memaksakan nilai default.</b> {@link #getRoot()}, {@link #getChild()},
 * {@link #getUrl()}, {@link #getNomorUrut()}, {@link #getTampilDiPt()},
 * {@link #getTampilDiSekolah()} dan {@link #getBukaHalamanBaru()} mengganti {@code null}
 * dengan nilai default <b>tanpa</b> menulis balik ke field. Karena Hibernate di entity ini
 * memakai <b>akses properti</b> (anotasi ada di getter), nilai default itulah yang benar-benar
 * <b>ditulis ke database</b> saat flush — kolom {@code root}/{@code child} tidak pernah tersimpan
 * {@code null} melainkan {@code 0}, dan {@code url} tersimpan string kosong yang sudah
 * di-{@code trim()}. Efeknya asimetris: objek di memori tetap {@code null}, baris di DB tidak.</li>
 * <li><b>Dua getter menulis balik ke field</b> (bukan sekadar mengembalikan default):
 * {@link #getId()} dan {@link #getAktif()}. Keduanya melakukan <i>lazy initialization</i>, jadi
 * membaca properti tersebut <b>mengubah keadaan objek</b>. Tidak ada getter yang menutup sesi
 * Hibernate, tidak ada getter destruktif, dan tidak ada getter yang menjalankan query
 * (berbeda dari beberapa entity lain di paket ini) — {@code Menu} adalah POJO murni.</li>
 * <li><b>Akibat {@link #getAktif()} tidak pernah mengembalikan {@code null}</b>, seluruh
 * pengecekan bergaya {@code menu.getAktif() == null || menu.getAktif()} yang tersebar di
 * {@code MainAction}, {@code MobileAction}, {@code DesktopMenuBootstrap},
 * {@code TampilanELearningActionMobile} dan kedua {@code MenuAction} secara efektif adalah
 * <b>kode mati</b> — cabang {@code == null} tidak pernah tercapai.</li>
 * <li><b>Tidak ada satu pun setter yang memvalidasi</b> {@link #url}: nilai apa pun yang
 * diketik operator akan dipakai apa adanya oleh {@code Common.launchMenu(...)}, termasuk URL
 * absolut {@code http…} ke host luar.</li>
 * </ul>
 *
 * @see ais.database.model.GeneralValueObject
 * @see Tbmrole
 * @see RolePrivilage
 * @see MenuMobile
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "menu")
public class Menu extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Instance {@code Menu} ikut ter-serialisasi karena disimpan di
	 * atribut HTTP session (mis. {@code currentMenu} oleh {@code DisplayMenu}, dan
	 * {@code current_menu}/{@code current_menus} oleh cache menu UI Baru), sehingga container
	 * dapat memindahkannya antar node/restart. Jangan diubah tanpa alasan kuat.
	 */
	private static final long serialVersionUID = 7836173482067483952L;
	/**
	 * Primary key tabel {@code menu}. <b>Bukan</b> kunci hierarki (lihat {@link #root}/
	 * {@link #child}) dan <b>tidak</b> dibangkitkan database — diisi manual oleh operator di layar
	 * menu. Dideklarasikan ulang di sini karena {@code GeneralValueObject} tidak dipetakan
	 * Hibernate.
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
	 * Mengembalikan ID pengguna terakhir yang mengubah baris menu ini.
	 *
	 * @return ID pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan ID pengguna pengubah terakhir. <b>Menolak nilai kosong secara diam-diam</b>:
	 * bila {@code olehId} {@code null} atau hanya berisi spasi, method langsung
	 * {@code return} sehingga nilai lama <b>dipertahankan</b> — jejak audit tidak bisa dihapus
	 * dengan menimpanya memakai string kosong. Dipanggil terutama oleh
	 * {@code AuditTimestampInterceptor} lewat kait {@link #onUpdate()}.
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
	 * Mengembalikan nama pengguna terakhir yang mengubah baris menu ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: dipanggil container persistence <b>tepat sebelum</b>
	 * {@code UPDATE} baris menu dikirim ke database. Mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} yang mengisi {@link #oleh}/{@link #olehId}
	 * dari pengguna yang sedang login dan menyetel {@link #tanggal_dirubah} ke waktu server.
	 * <p><b>Efek samping:</b> memutasi state objek ini. Jangan dipanggil manual dari kode
	 * aplikasi — hanya untuk dipicu oleh JPA.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Kait JPA {@code @PostPersist}/{@code @PostUpdate}/{@code @PostRemove}: dipanggil setiap kali
	 * satu baris menu <b>selesai</b> disimpan, diubah, atau dihapus. Menaikkan versi global
	 * menu-permission lewat {@code NewUiCacheInvalidator.invalidateAllMenuVersions()} sehingga
	 * <b>seluruh session yang sedang aktif</b> akan memuat ulang pohon menu/hak aksesnya pada
	 * request berikutnya — tanpa ini, perubahan menu baru terlihat setelah pengguna login ulang.
	 * <p><b>Efek samping:</b> memutasi counter global tingkat JVM (bukan hanya objek ini), jadi
	 * biayanya menyentuh semua pengguna. Satu-satunya anggota class ini yang tidak murni POJO.
	 * Jangan dipanggil manual — hanya untuk dipicu oleh JPA.</p>
	 */
	@javax.persistence.PostPersist
	@javax.persistence.PostUpdate
	@javax.persistence.PostRemove
	protected void invalidateNewUiMenuCache() {
		ais.common.newui.NewUiCacheInvalidator.invalidateAllMenuVersions();
	}

	/**
	 * Waktu perubahan terakhir baris menu ini. <b>Diinisialisasi ke waktu server saat objek
	 * dibuat</b> ({@code WaktuUtil.getDate()}), bukan {@code null}, sehingga baris baru selalu
	 * punya stempel waktu walau kait {@link #onUpdate()} belum pernah jalan (kait tersebut
	 * {@code @PreUpdate}, jadi tidak berjalan pada {@code INSERT} pertama). Dideklarasikan ulang
	 * dari {@code GeneralValueObject} karena keharusan pemetaan Hibernate.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel waktu perubahan terakhir baris menu ini. Menerima {@code null} apa adanya
	 * (tidak ada guard seperti pada {@link #setOleh(String)}).
	 *
	 * @param tanggal_dirubah stempel waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan waktu perubahan terakhir baris menu ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek yang baru
	 *         dibuat di memori, namun bisa {@code null} bila kolomnya kosong di database
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Kode node <b>INDUK</b> menu ini di dalam pohon — berisi nilai {@code child} milik menu di
	 * atasnya, atau {@code 0} untuk menu level-atas. Lihat penjelasan hierarki di dokumentasi
	 * class.
	 */
	private Long root;
	/**
	 * Kode node <b>menu ini sendiri</b> di dalam pohon (bukan "anak"! namanya menyesatkan).
	 * Menjadi nilai {@code root} bagi menu-menu turunannya. Lihat penjelasan hierarki di
	 * dokumentasi class.
	 */
	private Long child;
	/** Teks butir menu yang dilihat pengguna (kolom {@code label}, maksimum 100 karakter). */
	private String label;
	/**
	 * Alamat tujuan butir menu (kolom {@code url}, maksimum 100 karakter): umumnya path ZUL
	 * relatif seperti {@code /pages/master/…zul}, namun boleh pula URL absolut {@code http…}
	 * yang ditangani khusus oleh {@code Common.launchMenu(...)}. Tidak divalidasi di mana pun.
	 */
	private String url;
	/** Ikon kecil butir menu (dipakai pohon/daftar navigasi, UI Baru, mobile, dan API hak akses). */
	private String icon;
	/**
	 * Ikon besar butir menu (kolom {@code big_icon}, maksimum 255 karakter), dipakai tampilan
	 * ubin/dashboard seperti {@code BlankAction} dan {@code AmbilDataMenuBanyak}. Butir tanpa
	 * {@code url} atau tanpa {@code bigIcon} dilewati oleh {@code BlankAction}.
	 */
	private String bigIcon;
	/** Sakelar tampil/tidaknya butir menu ini. {@code null} diperlakukan sebagai aktif. */
	private Boolean aktif;
	/** Nomor urut tampil di antara menu sesaudara; kunci urut PERTAMA {@link #compareTo(GeneralValueObject)}. */
	private Integer nomorUrut;
	/** Sakelar: butir menu ini ikut ditampilkan pada instalasi varian Yayasan/Sekolah. Default anggap {@code true}. */
	private Boolean tampilDiSekolah;
	/** Sakelar: butir menu ini ikut ditampilkan pada instalasi varian Perguruan Tinggi. Default anggap {@code true}. */
	private Boolean tampilDiPt;
	/**
	 * Sakelar: butir menu dibuka sebagai <b>redirect ke tab browser baru</b> alih-alih tab
	 * internal {@code Tabbox} aplikasi. Lihat {@link #getBukaHalamanBaru()} untuk catatan
	 * perilaku {@code Common.launchMenu(...)}.
	 */
	private Boolean bukaHalamanBaru;

	/**
	 * Representasi teks singkat berformat {@code "<id>-<label>"} untuk keperluan debug/log.
	 * <p>Perhatikan: membaca field {@link #id} secara <b>langsung</b>, bukan lewat
	 * {@link #getId()}, sehingga <b>tidak</b> memicu pembangkitan ID acak dan bisa menghasilkan
	 * awalan {@code "null-"} untuk objek transient. {@link #label} juga dapat {@code null}.</p>
	 *
	 * @return string {@code "<id>-<label>"}
	 */
	public String toString() {
		return id + "-" + label;
	}

	/**
	 * Mengembalikan nama/berkas ikon kecil butir menu ini.
	 *
	 * @return ikon kecil, atau {@code null} bila tidak diisi
	 */
	@Column(name = "icon")
	public String getIcon() {
		return icon;
	}

	/**
	 * Menyetel nama/berkas ikon kecil butir menu ini.
	 *
	 * @param icon ikon kecil; boleh {@code null}
	 */
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Konstruktor kosong yang diwajibkan Hibernate/JPA; dipakai juga oleh layar CRUD menu dan
	 * {@code JurnalMenuReconciler} saat membuat baris menu baru.
	 */
	public Menu() {
	}

	/**
	 * Konstruktor pintasan untuk membuat objek acuan berdasarkan primary key saja — banyak
	 * dipakai self-test modul Jurnal untuk merakit himpunan {@code Tbmrole.getMenus()} tanpa
	 * menyentuh database.
	 *
	 * @param id primary key baris menu
	 */
	public Menu(Long id) {
		this.id = id;
	}

	/**
	 * Konstruktor pintasan yang hanya mengisi pasangan kunci hierarki. Pemakai utamanya adalah
	 * {@code MenuTreeModel} yang membuat <b>node sentinel</b> {@code new Menu(-1L, 0L)} sebagai
	 * akar semu pohon menu (root {@code -1} tidak pernah dipakai baris data nyata; child {@code 0}
	 * berarti "kumpulan menu level-atas"). Perhatikan {@link #id} dibiarkan {@code null}.
	 *
	 * @param root kode node induk
	 * @param child kode node menu ini sendiri
	 */
	public Menu(Long root, Long child) {
		this.root = root;
		this.child = child;
	}

	/**
	 * Konstruktor pintasan: kunci hierarki + teks butir menu.
	 *
	 * @param root kode node induk
	 * @param child kode node menu ini sendiri
	 * @param label teks butir menu
	 */
	public Menu(Long root, Long child, String label) {
		this.root = root;
		this.child = child;
		this.label = label;
	}

	/**
	 * Konstruktor pintasan terlengkap: kunci hierarki + teks + alamat tujuan.
	 *
	 * @param root kode node induk
	 * @param child kode node menu ini sendiri
	 * @param label teks butir menu
	 * @param url alamat tujuan (path ZUL atau URL absolut)
	 */
	public Menu(Long root, Long child, String label, String url) {
		this.root = root;
		this.child = child;
		this.label = label;
		this.url = url;
	}

	/**
	 * Mengembalikan primary key baris menu ini.
	 *
	 * <p><b>PERHATIAN — getter ini MENULIS BALIK ke field.</b> Bila {@link #id} masih
	 * {@code null}, method membangkitkan nilai acak
	 * {@code Long.parseLong(RandomStringUtils.randomNumeric(6))} dan <b>menyimpannya</b> ke field
	 * sebelum mengembalikannya. Jadi sekadar <i>membaca</i> properti ini pada objek transient
	 * sudah mengubah keadaan objek, dan pemeriksaan bergaya {@code menu.getId() == null} yang
	 * dipakai luas di {@code MenuAction} ("Tambah" vs "Ubah") <b>hanya benar bila dipanggil
	 * sekali</b> — pemanggilan berikutnya selalu melihat ID yang sudah terisi.</p>
	 *
	 * <p><b>Catatan risiko yang sengaja dicatat apa adanya (tidak diperbaiki di sini):</b>
	 * nilai acak diambil dari rentang sempit 6 digit ({@code 0}–{@code 999999}; hasil berawalan
	 * nol seperti {@code "000123"} menjadi {@code 123}), <b>tanpa</b> pengecekan tabrakan ke
	 * database. Untuk basis data dengan ratusan menu, peluang tabrakan tidak dapat diabaikan dan
	 * akan muncul sebagai pelanggaran unique constraint saat {@code INSERT}. Dalam praktiknya
	 * jalur normal tidak bergantung pada fallback ini: kedua layar {@code MenuAction} meminta
	 * operator <b>mengetik sendiri</b> ID di {@code Longbox} dan memvalidasinya lewat
	 * {@code checkId()}, sedangkan {@code JurnalMenuReconciler} memakai konvensi deterministik
	 * {@code 2000000000L + child}.</p>
	 *
	 * @return primary key baris menu; tidak pernah {@code null} — dibangkitkan acak bila kosong
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
	 * Menyetel primary key baris menu ini. Boleh {@code null} — pola {@code setId(null)} dipakai
	 * {@code MenuAction} pada aksi "Tambah Data"/"Copy Data" untuk mengubah hasil
	 * {@code clone()} menjadi baris baru.
	 *
	 * @param id primary key; {@code null} berarti "belum punya ID" (lihat {@link #getId()})
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode node <b>INDUK</b> menu ini (lihat penjelasan hierarki di dokumentasi
	 * class). Nilai {@code 0} menandakan menu level-atas.
	 *
	 * <p>{@code null} <b>dikoalesikan</b> menjadi {@code 0L}, namun field tidak ditulis balik.
	 * Karena Hibernate memetakan entity ini lewat akses properti, nilai {@code 0} inilah yang
	 * ikut ter-{@code INSERT}/{@code UPDATE} ke kolom {@code root} — kolom tersebut tidak pernah
	 * benar-benar tersimpan {@code null} lewat jalur ini. Berkat koalesi ini pula, pemanggilan
	 * bergaya {@code menu.getRoot().equals(0L)} di {@code TbmroleAction} aman dari
	 * {@code NullPointerException}.</p>
	 *
	 * @return kode node induk; tidak pernah {@code null}
	 */
	@Column(name = "root", nullable = true)
	public Long getRoot() {
		return this.root == null ? 0L : this.root;
	}

	/**
	 * Menyetel kode node induk menu ini (nilai {@code child} milik menu di atasnya, atau
	 * {@code 0} untuk level-atas).
	 *
	 * @param root kode node induk; boleh {@code null} (akan dibaca sebagai {@code 0})
	 */
	public void setRoot(Long root) {
		this.root = root;
	}

	/**
	 * Mengembalikan kode node <b>menu ini sendiri</b> di dalam pohon — nilai yang dipakai
	 * menu-menu turunannya pada kolom {@code root} mereka (lihat dokumentasi class).
	 *
	 * <p>Sama seperti {@link #getRoot()}, {@code null} dikoalesikan menjadi {@code 0L} tanpa
	 * menulis balik ke field, dan nilai koalesi itulah yang tersimpan ke database.</p>
	 *
	 * @return kode node menu ini; tidak pernah {@code null}
	 */
	@Column(name = "child", nullable = true)
	public Long getChild() {
		return this.child == null ? 0L : child;
	}

	/**
	 * Menyetel kode node menu ini sendiri. Nilai baru dibangkitkan {@code MenuAction} dengan
	 * pola sambung dua digit ({@code induk.getChild() + "00"}) atau {@code max(sibling) + 1}.
	 *
	 * @param child kode node menu ini; boleh {@code null} (akan dibaca sebagai {@code 0})
	 */
	public void setChild(Long child) {
		this.child = child;
	}

	/**
	 * Mengembalikan teks butir menu apa adanya (tanpa koalesi/trim).
	 *
	 * @return teks butir menu, atau {@code null} bila belum diisi
	 */
	@Column(name = "label", nullable = true, length = 100)
	public String getLabel() {
		return this.label;
	}

	/**
	 * Menyetel teks butir menu. Tidak divalidasi di sini; keharusan tidak-kosong dan keunikan
	 * ditegakkan di layar CRUD ({@code MenuAction.checkKodeMenu()}), bukan di entity.
	 *
	 * @param label teks butir menu (maksimum 100 karakter sesuai definisi kolom)
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Mengembalikan alamat tujuan butir menu, sudah di-{@code trim()}.
	 *
	 * <p><b>{@code null} dikoalesikan menjadi string kosong</b> {@code ""} (bukan {@code null})
	 * tanpa menulis balik ke field — sehingga (a) pemanggil tidak perlu memeriksa {@code null}
	 * dan pola {@code getUrl().trim().equals("")} yang tersebar di kode aman, tetapi (b) karena
	 * Hibernate membaca lewat properti, kolom {@code url} tersimpan sebagai string kosong
	 * ter-{@code trim}, bukan {@code NULL}. Waspadai pemeriksaan bergaya
	 * {@code menu.getUrl() == null} (mis. di {@code BlankAction} dan {@code MobileAction}) —
	 * pemeriksaan itu <b>tidak pernah bernilai benar</b> dan hanya cabang {@code equals("")}-nya
	 * yang efektif.</p>
	 *
	 * @return alamat tujuan yang sudah di-trim; tidak pernah {@code null}
	 */
	@Column(name = "url", length = 100)
	public String getUrl() {
		return this.url == null ? "" : this.url.trim();
	}

	/**
	 * Menyetel alamat tujuan butir menu. <b>Tidak ada validasi/pembersihan sama sekali</b>:
	 * nilai apa pun yang diketik operator akan dipakai {@code Common.launchMenu(...)}, termasuk
	 * URL absolut ke host di luar aplikasi.
	 *
	 * @param url path ZUL relatif atau URL absolut (maksimum 100 karakter sesuai definisi kolom)
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Menyetel nama/berkas ikon besar butir menu.
	 *
	 * @param bigIcon ikon besar; boleh {@code null}
	 */
	public void setBigIcon(String bigIcon) {
		this.bigIcon = bigIcon;
	}

	/**
	 * Mengembalikan nama/berkas ikon besar butir menu apa adanya (tanpa koalesi/trim, berbeda
	 * dari {@link #getUrl()}).
	 *
	 * @return ikon besar, atau {@code null} bila tidak diisi
	 */
	@Column(name = "big_icon", length = 255)
	public String getBigIcon() {
		return bigIcon;
	}

	/**
	 * Membandingkan dua butir menu untuk keperluan <b>pengurutan tampil</b>, meng-override
	 * {@code GeneralValueObject.compareTo(GeneralValueObject)} yang berbasis
	 * {@code nomorUrut}/{@code nim}/{@code nama}/{@code keterangan}.
	 *
	 * <p><b>Cara kerja:</b> ketiga kunci ({@link #getNomorUrut()}, {@link #getRoot()},
	 * {@link #getChild()}) diubah menjadi string yang <b>dipad kiri dengan nol sampai 10
	 * karakter</b> (disambung ke konstanta 24 nol lalu diambil 10 karakter terakhir), digabung
	 * menjadi {@code "<nomorUrut>_<root>_<child>"}, lalu dibandingkan secara leksikografis.
	 * Padding ini yang membuat perbandingan string berperilaku seperti perbandingan numerik.
	 * Urutan prioritas kunci adalah <b>nomorUrut lebih dulu, baru root, baru child</b> —
	 * bukan urutan pohon; ini bekerja untuk mengurutkan menu <i>sesaudara</i> (yang {@code root}-nya
	 * sama), tetapi pada daftar datar lintas induk butir dengan {@code nomorUrut} sama akan
	 * berkelompok terlebih dahulu.</p>
	 *
	 * <p><b>Kuirk yang dicatat apa adanya (tidak diperbaiki):</b></p>
	 * <ul>
	 * <li>Cast {@code (Menu) object} berada <b>di luar</b> blok {@code try}, sehingga
	 * membandingkan {@code Menu} dengan {@code GeneralValueObject} jenis lain melempar
	 * {@link ClassCastException} yang tidak tertangkap. Pemeriksaan {@code arg0 == null}
	 * dilakukan <b>setelah</b> cast (aman, karena cast atas {@code null} selalu berhasil).</li>
	 * <li>Padding 10 karakter memakai <b>10 digit terakhir</b>. Karena kode {@code child}
	 * bertambah 2 digit tiap level kedalaman, pohon yang lebih dalam dari ±5 level menghasilkan
	 * nilai lebih dari 10 digit dan digit terdepannya terpotong — urutan menjadi salah.
	 * Nilai negatif (mis. node sentinel {@code root = -1}) juga menghasilkan string yang
	 * mengandung tanda minus dan berurut aneh.</li>
	 * <li>Seluruh badan dibungkus {@code catch (Exception)} yang mengembalikan {@code 0};
	 * kegagalan apa pun diam-diam menjadi "dianggap sama".</li>
	 * <li>Mengembalikan {@code 0} <b>tidak</b> berarti {@code equals()} — konsisten dengan
	 * catatan yang sama pada {@code GeneralValueObject}.</li>
	 * </ul>
	 *
	 * @param object butir menu pembanding; harus berupa {@code Menu} (atau {@code null})
	 * @return bilangan negatif/nol/positif sesuai urutan tampil {@code this} terhadap
	 *         {@code object}; {@code 0} juga dikembalikan bila {@code object} {@code null} atau
	 *         terjadi exception apa pun
	 */
	@Override
	public int compareTo(GeneralValueObject object) {
		Menu arg0 = (Menu) object;
		if (arg0 == null) {
			return 0;
		} else {

			try {
				String nourut1 = "000000000000000000000000" + this.getNomorUrut();
				String nourut2 = "000000000000000000000000" + arg0.getNomorUrut();

				nourut1 = nourut1.substring(nourut1.length() - 10);
				nourut2 = nourut2.substring(nourut2.length() - 10);

				String root1 = "000000000000000000000000" + this.getRoot();
				String root2 = "000000000000000000000000" + arg0.getRoot();

				root1 = root1.substring(root1.length() - 10);
				root2 = root2.substring(root2.length() - 10);

				String child1 = "000000000000000000000000" + this.getChild();
				String child2 = "000000000000000000000000" + arg0.getChild();

				child1 = child1.substring(child1.length() - 10);
				child2 = child2.substring(child2.length() - 10);

				String total1 = nourut1 + "_" + root1 + "_" + child1;
				String total2 = nourut2 + "_" + root2 + "_" + child2;

				return total1.compareTo(total2);
			} catch (Exception e) {
				return 0;
			}
		}
	}

	/**
	 * Menyetel sakelar aktif/tidaknya butir menu ini. Dipanggil antara lain oleh checkbox pada
	 * pohon menu {@code MenuAction} yang langsung menyimpan perubahan lewat
	 * {@code Common.refreshUpdate(...)}.
	 *
	 * @param aktif {@code true} tampil, {@code false} disembunyikan; {@code null} akan dibaca
	 *              sebagai {@code true} (lihat {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan sakelar aktif/tidaknya butir menu ini.
	 *
	 * <p><b>PERHATIAN — getter ini MENULIS BALIK ke field</b> (berbeda dari
	 * {@link #getRoot()}/{@link #getUrl()} yang hanya mengoalesikan nilai kembalian): bila
	 * {@link #aktif} {@code null}, field <b>diisi</b> {@code true} lalu dikembalikan. Akibatnya
	 * (a) membaca properti ini mengubah keadaan objek, dan (b) method ini <b>tidak pernah</b>
	 * mengembalikan {@code null} — sehingga cabang {@code menu.getAktif() == null} pada belasan
	 * pemanggil ({@code MainAction}, {@code MobileAction}, {@code DesktopMenuBootstrap},
	 * {@code TampilanELearningActionMobile}, kedua {@code MenuAction}) adalah kode mati.</p>
	 *
	 * <p><b>Cakupan penegakan:</b> flag ini disaring saat <b>membangun tampilan</b> menu
	 * ({@code MainMenuHelper}, {@code MainTreeMenuHelper}, {@code MainBaruMenuHelper},
	 * {@code BlankAction}, {@code HakAksesApi}, {@code GrupPenggunaAksesApi}). Ia
	 * <b>bukan</b> pemeriksaan otorisasi — lihat catatan pada {@link #getUrl()} dan dokumentasi
	 * class.</p>
	 *
	 * @return {@code true} bila butir menu aktif; tidak pernah {@code null}
	 */
	public Boolean getAktif() {
		if (aktif == null) {
			aktif = true;
		}
		return aktif;
	}

	/**
	 * Mengembalikan nomor urut tampil butir menu ini, meng-override
	 * {@code GeneralValueObject.getNomorUrut()} (yang membaca field induk yang tidak dipetakan
	 * Hibernate). {@code null} dikoalesikan menjadi {@code 0} tanpa menulis balik ke field, dan
	 * nilai koalesi itulah yang tersimpan ke database.
	 *
	 * @return nomor urut tampil; tidak pernah {@code null}
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/**
	 * Menyetel nomor urut tampil butir menu ini di antara menu sesaudara.
	 *
	 * @param nomorUrut nomor urut tampil; boleh {@code null} (akan dibaca sebagai {@code 0})
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Mengembalikan sakelar "tampil pada instalasi varian Yayasan/Sekolah". {@code null}
	 * dikoalesikan menjadi {@code true} (default: tampil) tanpa menulis balik ke field.
	 *
	 * <p>Dipakai berpasangan dengan {@link #getTampilDiPt()} oleh {@code MainMenuHelper} dan
	 * {@code MainTreeMenuHelper} dengan pola {@code (!pt && !ya) || (pt && getTampilDiPt()) ||
	 * (ya && getTampilDiSekolah())} — bila instalasi bukan PT maupun Yayasan, kedua sakelar
	 * diabaikan dan semua menu ditampilkan.</p>
	 *
	 * @return {@code true} bila butir menu ikut tampil di varian Yayasan/Sekolah; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTampilDiSekolah() {
		return tampilDiSekolah == null ? true : tampilDiSekolah;
	}

	/**
	 * Menyetel sakelar "tampil pada instalasi varian Yayasan/Sekolah".
	 *
	 * @param tampilDiSekolah {@code false} untuk menyembunyikan; {@code null} dibaca sebagai
	 *                        {@code true}
	 */
	public void setTampilDiSekolah(Boolean tampilDiSekolah) {
		this.tampilDiSekolah = tampilDiSekolah;
	}

	/**
	 * Mengembalikan sakelar "tampil pada instalasi varian Perguruan Tinggi". {@code null}
	 * dikoalesikan menjadi {@code true} (default: tampil) tanpa menulis balik ke field. Lihat
	 * {@link #getTampilDiSekolah()} untuk pola penyaringannya.
	 *
	 * @return {@code true} bila butir menu ikut tampil di varian Perguruan Tinggi; tidak pernah
	 *         {@code null}
	 */
	public Boolean getTampilDiPt() {

		return tampilDiPt == null ? true : tampilDiPt;
	}

	/**
	 * Menyetel sakelar "tampil pada instalasi varian Perguruan Tinggi".
	 *
	 * @param tampilDiPt {@code false} untuk menyembunyikan; {@code null} dibaca sebagai
	 *                   {@code true}
	 */
	public void setTampilDiPt(Boolean tampilDiPt) {
		this.tampilDiPt = tampilDiPt;
	}

	/**
	 * Mengembalikan sakelar "buka di halaman/tab browser baru". {@code null} dikoalesikan
	 * menjadi {@code false} (default: buka di dalam aplikasi) tanpa menulis balik ke field.
	 *
	 * <p><b>Perilaku yang dipicu:</b> bila {@code true}, {@code Common.launchMenu(...)} tidak
	 * membuka tab internal melainkan melakukan {@code sendRedirect(..., "_blank")} ke
	 * {@link #getUrl()} — dipakai apa adanya bila diawali {@code "http"}, atau ditempeli host
	 * aplikasi bila relatif — <b>dengan menambahkan parameter query {@code ?uid=} berisi ID
	 * pengguna yang dienkripsi DES</b> sebagai mekanisme single-sign-on ke aplikasi tujuan.
	 * Konsekuensi yang perlu disadari: identitas pengguna (terenkripsi) ikut melintas di URL,
	 * sehingga berpotensi tercatat di access log server tujuan dan header {@code Referer};
	 * dan karena {@link #setUrl(String)} tidak memvalidasi apa pun, operator yang bisa menyunting
	 * master menu dapat mengarahkan token tersebut ke host mana pun. Dicatat apa adanya, tidak
	 * diperbaiki di sini.</p>
	 *
	 * @return {@code true} bila butir menu harus dibuka di tab browser baru; tidak pernah
	 *         {@code null}
	 */
	public Boolean getBukaHalamanBaru() {
		return bukaHalamanBaru == null ? false : bukaHalamanBaru;
	}

	/**
	 * Menyetel sakelar "buka di halaman/tab browser baru".
	 *
	 * @param bukaHalamanBaru {@code true} untuk membuka lewat redirect ke tab baru;
	 *                        {@code null} dibaca sebagai {@code false}
	 */
	public void setBukaHalamanBaru(Boolean bukaHalamanBaru) {
		this.bukaHalamanBaru = bukaHalamanBaru;
	}

}
