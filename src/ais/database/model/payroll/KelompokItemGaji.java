package ais.database.model.payroll;

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
import ais.database.model.akunting.Pertangungjawaban;

/**
 * Katalog <b>kelompok item gaji</b> (tabel {@code payroll.kelompok_item_gaji}) — master berhierarki
 * yang, di balik namanya yang terdengar seperti sekadar pengelompokan, sesungguhnya berperan sebagai
 * <b>sumber akun jurnal penggajian</b>. Setiap baris menyimpan dua "formula akun" berbentuk JSON
 * ({@link #getAkun()} untuk sisi kredit dan {@link #getAkunDebet()} untuk sisi debet) yang memetakan
 * satuan kerja ke {@code ais.database.model.akunting.Akun}. Komponen gaji
 * ({@link ais.database.model.payroll.ItemGaji}) tidak menyimpan akun jurnalnya sendiri secara
 * definitif; ia meminjam pemetaan dari kelompok inilah, lalu memilih entri yang cocok dengan satuan
 * kerja format gajinya.
 *
 * <h2>Struktur data</h2>
 * <ul>
 *   <li>{@link #getKode()} — kode kelompok bebas-teks. <b>Kolom inilah kunci sesungguhnya</b> dari
 *   seluruh mekanisme (lihat bagian "Pencocokan berbasis kode" di bawah), bukan {@link #getId()}.
 *   Tidak ada anotasi {@code @Column} maupun indeks unik pada kolom ini; keunikannya hanya dijaga
 *   di lapisan aplikasi oleh {@code KelompokItemGajiAction.checkKodeKelompokItemGaji()}.</li>
 *   <li>{@link #getNama()} — nama tampilan, {@code nullable = false}, panjang 255, di-{@code trim}
 *   saat dibaca. Juga dijaga keunikannya di lapisan aplikasi
 *   ({@code checkNamaKelompokItemGaji()}).</li>
 *   <li>{@link #getKeterangan()} — catatan bebas.</li>
 *   <li>{@link #getInduk()} — relasi <b>self-referential</b> ke kelompok induk; membentuk pohon
 *   kelompok. Tidak ada penjaga siklus di mana pun: layar Ubah memuat seluruh kelompok ke combobox
 *   induk termasuk baris yang sedang disunting, sehingga sebuah kelompok bisa dijadikan induk bagi
 *   dirinya sendiri. Dampak praktisnya kecil karena tidak ada satu pun konsumen yang menaiki rantai
 *   induk secara rekursif — renderer daftar hanya membaca satu tingkat.</li>
 *   <li>{@link #getAkun()} / {@link #getAkunDebet()} — dua kolom {@code text} berisi
 *   <b>array JSON</b> pemetaan akun per satuan kerja.</li>
 *   <li>{@link #getAktif()} — bendera aktif. Perhatikan bagian "Bendera {@code aktif}" di bawah:
 *   kolom ini <b>tidak pernah ditulis</b> oleh kode aplikasi mana pun.</li>
 *   <li>{@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} — jejak audit ringan
 *   milik seluruh keluarga {@link GeneralValueObject}.</li>
 * </ul>
 *
 * <p><b>Tidak ada kolom tenant sama sekali</b> — tidak ada {@code yayasan}, {@code sekolah},
 * {@code satuanKerja}, maupun sumbu pemisah lain. Katalog ini memang dirancang <b>global se-instalasi</b>;
 * pemisahan antar-tenant dipindahkan ke <i>dalam</i> JSON pemetaan akun (entri per
 * {@code SatuanKerja}). Konsekuensi keamanannya dibahas di bagian "Cakupan tenant".</p>
 *
 * <h2>Format JSON pemetaan akun</h2>
 * <p>Kedua kolom menyimpan {@code org.json.JSONArray} yang elemennya berbentuk
 * <code>{key, akun, satuanKerja}</code> — {@code key} sekadar pengenal baris editor (acak),
 * {@code akun} berisi id {@code Akun}, dan {@code satuanKerja} berisi id {@code SatuanKerja} atau
 * {@code null}. <b>Entri ber-{@code satuanKerja} {@code null} berfungsi sebagai akun default/fallback.</b>
 * Editornya adalah {@code AssetUtil.reloadFormula(...)} di layar ZK; menghapus sebuah entri tidak
 * memangkas array melainkan <b>mengganti elemen pada indeksnya menjadi {@code JSONObject} kosong</b>,
 * sehingga array bisa berisi banyak lubang. Pembacanya adalah
 * {@code AssetUtil.ambilDataAkun(String, SatuanKerja)}.</p>
 *
 * <h2>Rantai konsumsi (terverifikasi)</h2>
 * <pre>
 * PostingTransaksi*GajiAction / PembayaranItemGajiPegawai.getAkun()
 *   -&gt; ItemGaji.getAkun() / ItemGaji.getAkunDebet()
 *     -&gt; ItemGaji.ambilAkun() / ambilAkunDebet()
 *       -&gt; ItemGaji.getKelompokItemGaji()          // pencocokan kode via cache statis
 *         -&gt; KelompokItemGaji.getAkun() / getAkunDebet()   // JSON pemetaan, kelas INI
 *         -&gt; AssetUtil.ambilDataAkun(json, formatItemGaji.getSatuanKerja())
 * </pre>
 * <p>Karena {@link ais.database.model.payroll.ItemGaji#getAkun()} dan {@code getAkunDebet()} adalah
 * getter yang <b>menulis-balik</b> hasil resolusi ke FK per-item (property access +
 * {@code dynamicUpdate}), pemetaan yang diisi di layar kelompok ini tidak hanya dipakai sekali saat
 * posting — begitu resolusinya berhasil, ia <b>menimpa secara permanen</b> akun yang dipilih
 * operator pada baris Item Gaji.</p>
 *
 * <h2>Pencocokan berbasis kode dan cache statis se-JVM</h2>
 * <p>Perlu ditegaskan lebih dulu: <b>cache statis itu tidak tinggal di kelas ini.</b> Ia adalah
 * field {@code public static List<KelompokItemGaji> ItemGaji.kelompokItemGajis} beserta
 * {@code ItemGaji.reloadKelompokItemGaji()}. Kelas {@code KelompokItemGaji} sendiri sama sekali
 * tidak punya anggota {@code static}, tidak punya hook lifecycle selain {@code @PreUpdate} audit,
 * dan tidak tahu-menahu bahwa dirinya di-cache. Rangkuman mekanismenya, diverifikasi dari kode
 * kedua sisi:</p>
 * <ol>
 *   <li><b>Pemuatan.</b> {@code reloadKelompokItemGaji()} menjalankan
 *   {@code createCriteria(KelompokItemGaji.class).add(Restrictions.eq("aktif", true))} —
 *   <b>tanpa penyaring tenant</b> (wajar, kolomnya memang tidak ada) dan <b>tanpa batas jumlah
 *   baris</b>. Seluruh kegagalan ditelan ke {@code ErrorAuditUtil}; bila gagal, cache tetap
 *   memegang isi lamanya (atau tetap kosong bila gagal saat startup) tanpa indikasi apa pun di UI.</li>
 *   <li><b>Kapan dimuat.</b> (a) saat startup lewat {@code InitData.reloadDefaults()}; (b) dari
 *   {@code KelompokItemGajiAction} setelah Simpan ({@code onSave}) dan setelah Ubah/Hapus dari
 *   grid — keduanya lewat {@code Common.createDefaultTimer(...)}, jadi <b>asinkron</b>, bukan
 *   langsung dalam transaksi yang sama. Jalur tulis lain — impor Excel, SQL langsung, CRUD generik
 *   New UI — <b>tidak</b> menyegarkan cache sampai instans di-restart.</li>
 *   <li><b>Pencocokan.</b> {@code ItemGaji.getKelompokItemGaji()} mengabaikan FK
 *   {@code item_gaji.kelompok_item_gaji} yang tersimpan bila menemukan elemen cache yang
 *   {@code getKode()}-nya sama dengan {@code ItemGaji.getKode()} — perbandingan {@code trim()} +
 *   {@code equalsIgnoreCase()} — lalu <b>menugaskan ulang</b> field FK itu. Pencocokan berhenti
 *   pada kecocokan pertama ({@code break}), sehingga bila ada beberapa kelompok berkode sama yang
 *   pemenangnya ditentukan urutan pemuatan cache, bukan pilihan operator.</li>
 *   <li><b>Risiko tabrakan kode.</b> Kunci pencocokan itu menyeberangi <b>dua tabel yang berbeda</b>:
 *   {@code item_gaji.kode} versus {@code kelompok_item_gaji.kode}. Penjaga keunikan yang ada
 *   ({@code checkKodeKelompokItemGaji()}) hanya membandingkan sesama baris
 *   {@code kelompok_item_gaji} — <b>tidak ada satu pun pemeriksaan</b> terhadap kode komponen gaji
 *   yang sudah dipakai. Jadi menambahkan satu kelompok baru yang kodenya kebetulan sama dengan kode
 *   sebuah komponen gaji akan <b>memindahkan akun jurnal komponen itu</b>, serentak untuk seluruh
 *   tenant dalam JVM tersebut, tanpa satu pun aksi pada layar Item Gaji dan tanpa jejak di layar
 *   mana pun. Penjaga keunikan itu sendiri juga hanya lapisan aplikasi (rentan TOCTOU, tidak
 *   didukung indeks unik basis data) dan berlaku <b>lintas tenant</b> — kode yang sudah dipakai
 *   tenant lain akan ditolak, sehingga sekaligus menjadi orakel keberadaan data tenant lain.</li>
 * </ol>
 *
 * <h2>Bendera {@code aktif} — mekanisme di atas dalam keadaan TIDAK BERSENJATA</h2>
 * <p>Verifikasi menyeluruh atas repositori menemukan bahwa <b>{@link #setAktif(Boolean)} tidak
 * punya satu pun pemanggil</b>. Layar ZK {@code kelompok_item_gaji.zul} tidak menyediakan isian
 * "Aktif", {@code onSave()} hanya menulis {@code kode}, {@code nama}, {@code keterangan},
 * {@code induk}, {@code akun} dan {@code akunDebet}, dan daftar kolom impor Excel
 * ({@code "id","kode","nama","induk","akun","akunDebet","keterangan"}) juga tidak memuatnya.
 * Karena entity ini memakai {@code dynamicInsert = true} dan skema dibangun dengan
 * {@code hbm2ddl.auto=update} (kolom {@code boolean} tanpa {@code DEFAULT}), baris yang dibuat
 * lewat aplikasi menyimpan {@code aktif = NULL}. Sementara itu {@code reloadKelompokItemGaji()}
 * menyaring dengan {@code Restrictions.eq("aktif", true)}, dan dalam SQL {@code NULL = true} tidak
 * pernah benar. Akibatnya <b>cache statis tetap kosong</b> untuk seluruh baris yang lahir dari
 * aplikasi.</p>
 * <p>Ditambah temuan kedua — <b>{@code ItemGaji.setKelompokItemGaji(...)} juga tidak punya
 * pemanggil di luar entity-nya sendiri</b>, sehingga FK per-item pun tidak pernah diisi lewat UI
 * mana pun — konsekuensinya: pada instalasi yang datanya murni dibuat lewat aplikasi, seluruh
 * pemetaan akun yang diisi operator di layar ini <b>tidak pernah dipakai</b>. Penjurnalan gaji
 * jatuh sepenuhnya ke FK {@code akun}/{@code akun_debet} per-item. Ini sekaligus berarti risiko
 * tabrakan kode yang diuraikan di atas bersifat <b>laten</b>: ia menyala begitu ada pihak yang
 * berhasil menyetel {@code aktif = true}. Jalur yang bisa melakukannya: SQL langsung, migrasi/impor
 * data lama, atau <b>form otomatis Generic CRUD/{@code DynamicJspCrudGenerator}</b> yang memang
 * merender setiap properti termasuk {@code aktif} sebagai isian boolean.</p>
 * <p><b>Tiga pembacaan berbeda atas "aktif" hidup berdampingan</b> di kode: getter
 * {@link #getAktif()} menganggap {@code null} berarti aktif; {@code DynamicJspCrudGenerator}
 * menyaring dengan {@code isNull(aktif) OR aktif = true} (senada dengan getter); tetapi
 * {@code reloadKelompokItemGaji()} memakai {@code aktif = true} yang ketat. Layar dan cache karena
 * itu bisa menampilkan/memperlakukan himpunan baris yang berbeda untuk kata yang sama.</p>
 *
 * <h2>Cakupan tenant</h2>
 * <ul>
 *   <li><b>Generic CRUD v2.</b> {@code GenericCrudAutoEntityAdapter.scopeBindings()} hanya memasang
 *   pembatas untuk properti bernama {@code yayasan}, {@code sekolah}, {@code program},
 *   {@code fakultas}, {@code jurusan}, {@code satuanKerja} (plus beberapa properti per-peran).
 *   Entity ini <b>tidak punya satu pun</b> di antaranya, dan {@code addScope()} menelan kondisi
 *   "properti tidak ada" secara diam-diam — jadi daftar maupun tulis lewat jalur New UI berjalan
 *   <b>tanpa restriksi apa pun</b>. Sama seperti koreksi yang sudah dicatat untuk
 *   {@link ais.database.model.payroll.ItemGaji}: menambahkan {@code pegawai} ke whitelist tidak
 *   akan menutup apa pun di sini, sebab masalahnya bukan properti yang terlewat melainkan
 *   ketiadaan sumbu tenant sama sekali.</li>
 *   <li><b>Fail-open di dalam pembaca JSON.</b> {@code AssetUtil.ambilDataAkun(json, satuanKerja)}
 *   punya cabang <code>else if (satuanKerjaData == null || ...)</code>. Bila komponen gaji berasal
 *   dari {@code FormatItemGaji} yang {@code satuanKerja}-nya {@code null}, cabang itu
 *   mengembalikan <b>entri pertama yang punya akun, apa pun satuan kerjanya</b> — bukan hanya entri
 *   default. Artinya satu format gaji tanpa satuan kerja bisa terjurnal ke akun milik tenant lain
 *   yang kebetulan terdaftar lebih dulu di array. Karena katalog ini global, entri milik banyak
 *   tenant memang bercampur dalam satu array.</li>
 * </ul>
 *
 * <h2>Siapa yang bisa mengubah katalog ini</h2>
 * <ul>
 *   <li><b>Layar ZK sendiri</b> ({@code KelompokItemGajiAction}) memeriksa
 *   {@code CommonPrivilages.checkPrevilages(READ/CREATE/UPDATE/DELETE)} — pemeriksaannya ada dan
 *   lengkap untuk tombol Tambah/Ubah/Hapus.</li>
 *   <li><b>Pewarisan hak menu induk.</b> {@code kelompok_item_gaji.zul} juga di-{@code include}
 *   sebagai salah satu tab di {@code format_item_gaji.zul}. Karena
 *   {@code CommonPrivilages.checkPrevilages()} membaca atribut sesi {@code currentMenu} yang tidak
 *   di-resolve ulang untuk halaman ter-include, hak yang dievaluasi adalah hak atas menu
 *   <i>Format Item Gaji</i>, bukan atas menu Kelompok Item Gaji. Pemegang hak tulis pada layar
 *   Format Item Gaji dengan sendirinya memperoleh CRUD atas katalog akun jurnal ini.</li>
 *   <li><b>Impor Excel.</b> {@code Common.uploadData(this, KelompokItemGaji.class, contents)}
 *   dengan {@code contents} yang memuat {@code akun} dan {@code akunDebet} — jadi seluruh peta akun
 *   jurnal bisa ditulis massal dari satu berkas. Tombolnya memang disembunyikan bila pengguna tidak
 *   memegang create+update+delete, tetapi {@code CommonDownloadUpload} sendiri <b>nol pemeriksaan
 *   hak di sisi server</b>: gerbangnya semata-mata visibilitas komponen. Jalur ini juga melewati
 *   {@code checkKodeKelompokItemGaji()}/{@code checkNamaKelompokItemGaji()} dan <b>tidak</b>
 *   memanggil {@code reloadKelompokItemGaji()}.</li>
 *   <li><b>Permukaan New UI ketiga.</b> Tersedia
 *   {@code /WEB-INF/baru/modul/pagesmasterpayrollkelompokitemgajizul/index.jsp} yang merender form
 *   otomatis {@code DynamicJspCrudGenerator} atas kelas ini, plus berkas layanan
 *   {@code new/payroll/services/kelompok_item_gaji_service.jsp}. Form otomatis itulah satu-satunya
 *   jalur aplikasi yang bisa menyetel {@code aktif}.</li>
 *   <li><b>Verifikasi negatif:</b> tidak ada satu pun {@code *ApiHelper}/{@code PosApi} yang
 *   menyentuh kelas ini — seluruh rujukan di repositori hanya
 *   {@code KelompokItemGajiAction}, {@code ItemGaji}, {@code Common.DataUtil},
 *   {@code InitData} dan {@code hibernate.cfg.xml}. Pola fail-open {@code bolehAksi()} yang
 *   tersebar di lapisan REST modul keuangan <b>tidak</b> menjangkau entity ini.</li>
 * </ul>
 *
 * <h2>Pengelompokan method</h2>
 * <ul>
 *   <li><b>Identitas &amp; label:</b> {@link #getId()}, {@link #getKode()}, {@link #getNama()},
 *   {@link #getKeterangan()}, {@link #toString()}.</li>
 *   <li><b>Hierarki:</b> {@link #getInduk()}.</li>
 *   <li><b>Pemetaan akun jurnal:</b> {@link #getAkun()}, {@link #getAkunDebet()}.</li>
 *   <li><b>Status:</b> {@link #getAktif()}.</li>
 *   <li><b>Jejak audit:</b> {@link #getOleh()}, {@link #getOlehId()},
 *   {@link #getTanggal_dirubah()}, {@link #onUpdate()}.</li>
 * </ul>
 *
 * <h2>Hal-hal non-obvious lain</h2>
 * <ul>
 *   <li>{@link #getAkun()}/{@link #getAkunDebet()} <b>tidak pernah mengembalikan {@code null}</b>:
 *   nilai kosong disubstitusi {@code Pertangungjawaban.DEFAULT_FORMULA}, yaitu string
 *   {@code "[]"}. Substitusi ini <b>tidak</b> ditulis balik ke field, jadi — berbeda dari getter
 *   destruktif di {@link ais.database.model.payroll.ItemGaji} — kedua getter ini aman dibaca.
 *   Konstantanya sendiri diimpor dari paket akunting
 *   ({@code ais.database.model.akunting.Pertangungjawaban}), satu-satunya keterikatan lintas modul
 *   pada kelas ini dan murni bersifat "kebetulan formatnya sama".</li>
 *   <li>{@link #getAktif()} juga menyubstitusi ({@code null} → {@code true}) tanpa menulis balik.
 *   Tidak ada satu pun getter destruktif di kelas ini; {@link #getInduk()} memang menugaskan hasil
 *   {@code check(...)} ke field, tetapi itu resolusi proxy lazy yang memang menjadi kontrak
 *   {@link GeneralValueObject#check(Object)}, bukan penggantian nilai bisnis.</li>
 *   <li>Combobox induk di layar Ubah diisi dengan properti label {@code {"nomorUrut","nama"}},
 *   padahal entity ini <b>tidak punya properti {@code nomorUrut}</b>. Helper combobox memverifikasi
 *   metadata Hibernate lebih dulu dan melewati properti yang tidak terpetakan, jadi label jatuh ke
 *   {@code nama} saja tanpa galat — sisa salin-tempel dari layar master lain.</li>
 *   <li>{@link #setOleh(String)} dan {@link #setOlehId(String)} <b>mengabaikan</b> nilai
 *   {@code null}/kosong, sehingga jejak pengubah terakhir tidak bisa dihapus dengan menyimpan nilai
 *   kosong.</li>
 *   <li>Kelas ini terdaftar di {@code DataUtil.CLASS_JANGAN_DIBERSIHKAN} (dikecualikan dari
 *   rutin pembersihan data) dan di daftar {@code InitData.initClasses(...)} (dipanaskan lebih dulu
 *   ke cache {@code ConstantValues} saat startup).</li>
 *   <li>{@code @Audited} (Hibernate Envers) aktif, dan layar memakai
 *   {@code RevisiHelper.createNewRevisi(...)} sehingga riwayat perubahan pemetaan akun dapat
 *   ditelusuri — untuk jalur ZK. Perubahan lewat SQL langsung tentu saja luput.</li>
 * </ul>
 *
 * <p><b>Catatan penamaan:</b> Javadoc lama kelas ini berbunyi "Bank generated by hbm2java" —
 * peninggalan generator Hibernate Tools 2010 yang tidak ada hubungannya dengan entity {@code Bank}.
 * Komentar itu digantikan dokumentasi di atas.</p>
 *
 * @see ais.database.model.payroll.ItemGaji
 * @see ais.database.model.payroll.FormatItemGaji
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "payroll", name = "kelompok_item_gaji")
public class KelompokItemGaji extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci utama {@code IDENTITY}; lihat {@link #getId()}. */
	private Long id;
	/** Nama pengguna pengubah terakhir; lihat {@link #getOleh()}. */
	private String oleh;
	/** Id pengguna pengubah terakhir; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pengubah terakhir. Nilai {@code null} atau kosong <b>diabaikan</b> (bukan
	 * disimpan sebagai kosong) agar jejak audit sebelumnya tidak tertimpa.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir. Sama seperti {@link #setOlehId(String)}, nilai
	 * {@code null} atau kosong diabaikan agar jejak sebelumnya tidak tertimpa.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir menyimpan baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan stempel waktu/pengguna ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris di-{@code UPDATE}. Method ini
	 * memenuhi satu-satunya kontrak {@code abstract} yang diwajibkan {@link GeneralValueObject};
	 * jangan dipanggil manual.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu server saat object dibuat, lihat {@link #getTanggal_dirubah()}. */ private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu perubahan terakhir. Normalnya tidak dipanggil dari kode aplikasi —
	 * nilainya diisi otomatis oleh {@link #onUpdate()} lewat {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris ini.
	 *
	 * @return stempel waktu ({@code TIMESTAMP}); tidak pernah {@code null} pada object baru karena
	 *         field-nya diinisialisasi ke waktu server saat konstruksi
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks singkat berformat {@code "<id>-<nama>"}, dipakai untuk log dan sebagai label
	 * cadangan komponen UI.
	 *
	 * <p>Membaca field {@link #nama} secara langsung (bukan lewat {@link #getNama()}), jadi tanpa
	 * {@code trim}. Pada object yang belum disimpan hasilnya berawalan {@code "null-"}.</p>
	 *
	 * @return gabungan id dan nama kelompok
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Kode kelompok — <b>kunci pencocokan sesungguhnya</b> terhadap {@code ItemGaji.kode}; lihat
	 * {@link #getKode()} dan Javadoc kelas.
	 */
	private String kode;

	/** Nama tampilan kelompok, wajib diisi; lihat {@link #getNama()}. */
	private String nama;
	/** Catatan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Kelompok induk (self-referential, boleh {@code null}); lihat {@link #getInduk()}. */
	private KelompokItemGaji induk;
	/** JSON pemetaan akun sisi <b>kredit</b> per satuan kerja; lihat {@link #getAkun()}. */
	private String akun;
	/** JSON pemetaan akun sisi <b>debet</b> per satuan kerja; lihat {@link #getAkunDebet()}. */
	private String akunDebet;
	/** Bendera aktif — tidak pernah ditulis kode aplikasi; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate. Seluruh field dibiarkan {@code null} kecuali
	 * {@link #getTanggal_dirubah()} yang langsung diisi waktu server.
	 */
	public KelompokItemGaji() {
	}

	/**
	 * Mengembalikan kunci utama baris ini.
	 *
	 * <p>Kolomnya {@code insertable = false} dan dibangkitkan basis data
	 * ({@code GenerationType.IDENTITY}), sehingga bernilai {@code null} sampai baris benar-benar
	 * tersimpan. Perhatikan bahwa <b>bukan id ini</b> yang menentukan komponen gaji mana yang
	 * memakai kelompok ini, melainkan {@link #getKode()} — lihat Javadoc kelas.</p>
	 *
	 * @return id baris, atau {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama. Hanya dipakai Hibernate; kode aplikasi tidak perlu memanggilnya.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan kode kelompok apa adanya (tanpa {@code trim}).
	 *
	 * <p><b>Kolom terpenting di kelas ini.</b> {@code ItemGaji.getKelompokItemGaji()} mencocokkan
	 * nilai ini dengan {@code ItemGaji.getKode()} — perbandingan {@code trim()} +
	 * {@code equalsIgnoreCase()} atas isi cache statis {@code ItemGaji.kelompokItemGajis} — dan bila
	 * cocok, <b>menimpa</b> FK kelompok yang tersimpan pada komponen gaji tersebut. Dengan kata
	 * lain, kolom teks inilah yang secara efektif menentukan akun buku besar mana yang dipakai saat
	 * gaji dijurnal.</p>
	 *
	 * <p>Tidak ada anotasi {@code @Column} (nama kolom mengikuti nama properti) dan
	 * <b>tidak ada indeks unik</b>. Keunikan hanya diperiksa di lapisan aplikasi oleh
	 * {@code KelompokItemGajiAction.checkKodeKelompokItemGaji()} — pemeriksaan itu berlaku
	 * lintas tenant, rentan TOCTOU, dilewati jalur impor Excel, dan <b>sama sekali tidak</b>
	 * membandingkan terhadap kode pada tabel {@code item_gaji}.</p>
	 *
	 * @return kode kelompok, atau {@code null} bila belum diisi
	 */
	public String getKode() {
		return kode;
	}

	/**
	 * Mengisi kode kelompok. Layar ZK selalu memanggilnya dengan nilai yang sudah di-{@code trim}
	 * dan sudah lolos pemeriksaan keunikan; jalur impor Excel menulis nilai sel apa adanya.
	 *
	 * <p><b>Perhatian:</b> mengubah nilai ini mengubah komponen gaji mana saja yang mengambil akun
	 * jurnalnya dari kelompok ini (lihat {@link #getKode()}), berlaku untuk seluruh tenant sekaligus
	 * setelah cache disegarkan.</p>
	 *
	 * @param kode kode kelompok
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan nama tampilan kelompok, sudah di-{@code trim}.
	 *
	 * @return nama kelompok tanpa spasi di ujung, atau {@code null} bila belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama tampilan kelompok. Wajib diisi di layar dan keunikannya diperiksa
	 * {@code checkNamaKelompokItemGaji()} (lapisan aplikasi saja, lintas tenant, tanpa indeks unik).
	 *
	 * @param nama nama kelompok
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan catatan bebas kelompok ini. Murni deskriptif — tidak dibaca logika bisnis mana
	 * pun, hanya ditampilkan pada kolom "Keterangan" di grid.
	 *
	 * @return keterangan, atau {@code null} bila kosong
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi catatan bebas kelompok ini.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengembalikan kelompok induk dari kelompok ini (relasi self-referential, boleh {@code null}
	 * untuk kelompok tingkat teratas).
	 *
	 * <p><b>Efek samping:</b> menugaskan kembali hasil {@link GeneralValueObject#check(Object)} ke
	 * field {@link #induk}. Ini adalah pola resolusi proxy lazy standar seluruh entity di
	 * repositori ini — object yang dikembalikan bisa berasal dari cache atau session lain, tetapi
	 * identitas barisnya sama, jadi getter ini <b>tidak destruktif</b> (berbeda dari getter penulis
	 * -balik di {@link ais.database.model.payroll.ItemGaji}).</p>
	 *
	 * <p><b>Kasus tepi:</b> tidak ada penjaga siklus. Layar Ubah memuat seluruh kelompok ke
	 * combobox induk termasuk baris yang sedang disunting, jadi sebuah kelompok bisa menjadi
	 * induknya sendiri, atau dua kelompok bisa saling menginduk. Tidak ada konsumen yang menaiki
	 * rantai induk secara rekursif, sehingga siklus semacam itu tidak menyebabkan
	 * {@code StackOverflowError} — hanya membuat pohon kelompok tidak bermakna.</p>
	 *
	 * @return kelompok induk, atau {@code null} bila kelompok ini merupakan induk utama
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "induk", nullable = true)
	public KelompokItemGaji getInduk() {
		induk = check(induk);
		return induk;
	}

	/**
	 * Mengisi kelompok induk.
	 *
	 * @param induk kelompok induk, atau {@code null} untuk menjadikan kelompok ini induk utama
	 */
	public void setInduk(KelompokItemGaji induk) {
		this.induk = induk;
	}

	/**
	 * Mengembalikan <b>peta akun jurnal sisi KREDIT</b> kelompok ini dalam bentuk string JSON.
	 *
	 * <p>Isinya {@code JSONArray} dengan elemen berbentuk
	 * <code>{key, akun, satuanKerja}</code>: satu entri per {@code SatuanKerja}, ditambah — bila
	 * diisi operator — satu entri ber-{@code satuanKerja} {@code null} yang bertindak sebagai akun
	 * default. Entri yang "dihapus" di editor diganti menjadi {@code JSONObject} kosong pada
	 * indeksnya, bukan dipangkas, jadi array bisa berlubang; pembacanya memang melewati elemen tanpa
	 * {@code key}.</p>
	 *
	 * <p><b>Substitusi nilai kosong:</b> bila kolomnya {@code null} atau string kosong, getter
	 * mengembalikan {@code Pertangungjawaban.DEFAULT_FORMULA} — yaitu {@code "[]"} — sehingga
	 * pemanggil selalu aman melakukan {@code new JSONArray(...)} tanpa memeriksa {@code null}
	 * lebih dulu. Substitusi ini <b>tidak ditulis balik</b> ke field, jadi membaca getter ini tidak
	 * mengubah data tersimpan.</p>
	 *
	 * <p><b>Dipanggil dari:</b> (1) {@code ItemGaji.ambilAkun()}, yang menyerahkan JSON ini bersama
	 * {@code formatItemGaji.getSatuanKerja()} ke {@code AssetUtil.ambilDataAkun(...)} untuk
	 * memilih akun yang berlaku, dan hasilnya pada akhirnya menentukan akun kredit yang dijurnal
	 * saat posting penggajian; (2) {@code KelompokItemGajiAction.init(...)} yang membangun editor
	 * formula ZK dari string ini.</p>
	 *
	 * <p><b>Kasus tepi penting:</b> bila satuan kerja yang diminta {@code null},
	 * {@code AssetUtil.ambilDataAkun} mengembalikan <b>entri berakun pertama apa pun satuan
	 * kerjanya</b>, bukan hanya entri default — karena katalog ini global lintas tenant, entri milik
	 * tenant lain bisa terpilih. Bila JSON rusak atau akun yang dirujuk sudah dihapus, kegagalan
	 * ditelan menjadi {@code null} di sisi {@code ItemGaji} dan penjurnalan diam-diam jatuh ke FK
	 * per-item.</p>
	 *
	 * @return string JSON pemetaan akun kredit; tidak pernah {@code null} — minimal {@code "[]"}
	 */
	@Column(name = "akun", columnDefinition = "text")
	public String getAkun() {
		return akun == null || akun.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : akun;
	}

	/**
	 * Mengisi peta akun jurnal sisi kredit.
	 *
	 * <p>Satu-satunya penulis di jalur ZK adalah {@code KelompokItemGajiAction.onSave()}, yang
	 * menyerahkan hasil {@code JSONArray.toString()} dari editor formula. Jalur impor Excel dapat
	 * menulis kolom ini secara massal dengan isi sel apa adanya — <b>tanpa validasi bahwa isinya
	 * JSON yang sah</b>; isi yang tidak sah baru ketahuan saat penjurnalan, dan itu pun ditelan
	 * menjadi "tidak ada pemetaan".</p>
	 *
	 * <p><b>Efek samping tidak langsung:</b> mengubah nilai ini mengubah akun buku besar untuk
	 * seluruh komponen gaji yang berkode sama dengan kelompok ini, pada seluruh tenant, terhitung
	 * sejak cache statis disegarkan. Jurnal yang sudah terposting tidak ikut berubah.</p>
	 *
	 * @param akun string JSON pemetaan akun kredit; {@code null}/kosong berarti "tidak ada pemetaan"
	 */
	public void setAkun(String akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan <b>peta akun jurnal sisi DEBET</b> kelompok ini (umumnya akun Beban Gaji/Beban
	 * Tunjangan) dalam bentuk string JSON.
	 *
	 * <p>Format, substitusi {@code "[]"} untuk nilai kosong, penanganan entri terhapus, kasus tepi
	 * satuan kerja {@code null}, dan sifat tidak-menulis-balik seluruhnya <b>identik</b> dengan
	 * {@link #getAkun()}; lihat Javadoc method itu untuk uraian lengkapnya.</p>
	 *
	 * <p><b>Dipanggil dari:</b> {@code ItemGaji.ambilAkunDebet()} — yang pada gilirannya dibaca
	 * {@code PembayaranItemGajiPegawai.getAkunDebet()} dan dikumpulkan
	 * {@code PostingTransaksiPembayaranGajiAction}/{@code PostingTransaksiPenggajianAction} sebagai
	 * sisi debet jurnal gaji — serta dari {@code KelompokItemGajiAction.init(...)} untuk membangun
	 * editor formula "Akun Debet".</p>
	 *
	 * @return string JSON pemetaan akun debet; tidak pernah {@code null} — minimal {@code "[]"}
	 */
	@Column(name = "akun_debet", columnDefinition = "text")
	public String getAkunDebet() {
		return akunDebet == null || akunDebet.isEmpty() ? Pertangungjawaban.DEFAULT_FORMULA : akunDebet;
	}

	/**
	 * Mengisi peta akun jurnal sisi debet. Seluruh catatan pada {@link #setAkun(String)} berlaku
	 * sama persis di sini.
	 *
	 * @param akunDebet string JSON pemetaan akun debet; {@code null}/kosong berarti "tidak ada
	 *        pemetaan"
	 */
	public void setAkunDebet(String akunDebet) {
		this.akunDebet = akunDebet;
	}

	/**
	 * Menyatakan apakah kelompok ini aktif, dengan {@code null} diperlakukan sebagai <b>aktif</b>.
	 *
	 * <p><b>Peringatan — jangan menyimpulkan dari getter ini bahwa kelompok benar-benar dipakai.</b>
	 * Ada tiga pembacaan berbeda atas kata "aktif" di dalam kode, dan hanya yang paling ketat yang
	 * menentukan apakah pemetaan akun kelompok ini benar-benar diterapkan:</p>
	 * <ul>
	 *   <li>getter ini: {@code null} dianggap {@code true};</li>
	 *   <li>{@code DynamicJspCrudGenerator}: menyaring dengan {@code aktif IS NULL OR aktif = true}
	 *   (senada dengan getter ini);</li>
	 *   <li>{@code ItemGaji.reloadKelompokItemGaji()}: menyaring dengan
	 *   {@code Restrictions.eq("aktif", true)} yang <b>ketat</b> — dan dalam SQL, {@code NULL} tidak
	 *   pernah sama dengan {@code true}.</li>
	 * </ul>
	 * <p>Karena {@link #setAktif(Boolean)} tidak punya satu pun pemanggil di seluruh repositori
	 * (layar ZK tidak menyediakan isian "Aktif", {@code onSave()} tidak menulisnya, daftar kolom
	 * impor Excel tidak memuatnya) dan entity ini memakai {@code dynamicInsert = true}, baris yang
	 * dibuat lewat aplikasi tersimpan dengan {@code aktif = NULL}. Barisan tersebut lolos filter
	 * layar tetapi <b>tidak pernah masuk cache statis</b>, sehingga pemetaan akunnya tidak pernah
	 * dipakai saat penjurnalan. Lihat Javadoc kelas, bagian "Bendera {@code aktif}".</p>
	 *
	 * <p><b>Efek samping:</b> tidak ada — substitusi {@code true} dikembalikan langsung tanpa
	 * ditulis ke field, jadi getter ini tidak mengubah data tersimpan.</p>
	 *
	 * @return {@code true} bila kolomnya {@code true} <i>atau</i> {@code null}; {@code false} hanya
	 *         bila kolomnya benar-benar berisi {@code false}
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi bendera aktif kelompok ini.
	 *
	 * <p><b>Tidak ada satu pun pemanggil</b> di seluruh kode aplikasi (per verifikasi menyeluruh
	 * repositori). Satu-satunya jalur yang dapat menyetel kolom ini adalah form otomatis
	 * {@code DynamicJspCrudGenerator}/Generic CRUD — yang memang merender setiap properti termasuk
	 * yang bertipe {@code Boolean} — atau penulisan langsung ke basis data.</p>
	 *
	 * <p><b>Efek samping tidak langsung yang signifikan:</b> menyetel nilai ini menjadi
	 * {@code true} memasukkan kelompok ke cache statis {@code ItemGaji.kelompokItemGajis} pada
	 * penyegaran berikutnya, dan dengan demikian <b>mengaktifkan</b> mekanisme pencocokan berbasis
	 * kode yang menimpa FK akun per-item. Pada instalasi yang seluruh datanya dibuat lewat
	 * aplikasi, inilah satu-satunya langkah yang membuat pemetaan akun kelompok mulai berpengaruh
	 * pada jurnal gaji — sekaligus yang membuat risiko tabrakan kode lintas tabel/lintas tenant
	 * (lihat Javadoc kelas) berubah dari laten menjadi nyata.</p>
	 *
	 * @param aktif {@code true}/{@code false}/{@code null}; {@code null} dibaca sebagai aktif oleh
	 *        {@link #getAktif()} tetapi <i>tidak</i> oleh pemuat cache
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
