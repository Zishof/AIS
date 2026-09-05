package ais.database.model.tenant;

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
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Mapping jenis usaha → module code (database-driven, BUKAN switch besar di servlet/JSP).</h3>
 *
 * <p>Satu baris = satu module yang direkomendasikan/diwajibkan utk satu {@link JenisUsahaTenant}.
 * Dipakai (1) wizard utk menampilkan "module bundle yang akan diaktifkan" per kartu jenis usaha,
 * (2) provisioning utk membentuk union {@code TenantModuleEntitlement} (source BUSINESS_TYPE).
 * Entitlement ≠ permission: baris di sini TIDAK memberi izin tindakan pengguna apa pun.</p>
 *
 * <h4>Pendaftar TIDAK dapat memilih modulnya sendiri</h4>
 *
 * <p>Ini titik yang paling penting untuk dipahami dari sisi keamanan pada modul pendaftaran
 * mandiri. Daftar modul yang akhirnya aktif pada tenant baru dibentuk seluruhnya di server oleh
 * {@code ais.service.tenant.TenantEntitlementService.terapkanDariJenisUsaha(...)}, dengan sumber
 * data murni dari database:</p>
 * <ol>
 * <li>ambil pilihan jenis usaha permohonan dari {@link PendaftaranTenantJenisUsaha} (bukan dari
 * request);</li>
 * <li>untuk tiap jenis usaha, ambil baris tabel ini yang ber-{@link #getDefaultEnabled()}
 * {@code true};</li>
 * <li>bentuk union kode modul, lalu buat {@code TenantModuleEntitlement} bersumber
 * {@code SOURCE_BUSINESS_TYPE} -- idempoten, baris yang sudah ada dilewati sehingga retry
 * provisioning tidak menggandakan entitlement.</li>
 * </ol>
 *
 * <p>Tidak ada parameter request bernama modul/entitlement di mana pun jalur pendaftaran; formulir
 * hanya mengirim {@code jenisUsahaIds} yang divalidasi harus ada dan aktif. Karena itu memilih
 * "kartu" jenis usaha tertentu hanya memberi modul yang memang sudah ditetapkan Platform Admin
 * untuk kartu itu -- pendaftar tidak dapat menambahkan modul premium dengan memanipulasi payload.
 * Kode paket ({@code planCode}) yang dikirim klien pun tidak memengaruhi daftar modul sama sekali;
 * ia hanya tersalin sebagai label {@code planVersion} pada baris entitlement (lihat
 * {@link PendaftaranTenant#getSelectedPlanVersion()}).</p>
 *
 * <p>Penegasan ulang atas kalimat "entitlement ≠ permission" di atas: baris entitlement hanya
 * menyatakan modul mana yang dibeli/berlaku bagi tenant, sedangkan izin tindakan pengguna tetap
 * ditentukan mekanisme hak akses AIS ({@code Tbmrole} dan kawan-kawan). Menambah baris di sini
 * TIDAK menaikkan hak siapa pun di dalam tenant.</p>
 *
 * @see JenisUsahaTenant
 * @see PendaftaranTenantJenisUsaha
 * @see TenantModuleEntitlement
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_usaha_tenant_module",
		uniqueConstraints = @UniqueConstraint(columnNames = { "jenis_usaha_tenant_id", "module_code" }))
public class JenisUsahaTenantModule extends GeneralValueObject {

	/** Versi serialisasi Java standar entity AIS. */
	private static final long serialVersionUID = 1L;

	/** Primary key surrogate (identity, di-generate database). */
	private Long id;
	/** Jenis usaha pemilik pemetaan modul ini. */
	private JenisUsahaTenant jenisUsahaTenant;
	/** Kode modul yang dipetakan. */
	private String moduleCode;
	/** Penanda modul ikut aktif otomatis saat provisioning. */
	private Boolean defaultEnabled;
	/** Penanda modul wajib (belum dibaca kode mana pun). */
	private Boolean required;
	/** Urutan tampil modul pada kartu wizard. */
	private Integer displayOrder;

	/** Nama pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris (shadow audit AIS). */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir + hook {@code @PreUpdate} lewat
	 * {@code AuditTimestampInterceptor}. Trio {@code oleh}/{@code olehId}/{@code tanggal_dirubah}
	 * adalah keharusan teknis pola entity AIS (dipakai layar audit generik), berdampingan dengan
	 * riwayat Envers dan bukan penggantinya. Perhatikan bahwa entity ini TIDAK punya kolom
	 * {@code created_at} maupun {@code @Version} seperti saudara-saudaranya di paket ini --
	 * riwayat perubahannya bergantung sepenuhnya pada Envers.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default wajib JavaBean/Hibernate. */
	public JenisUsahaTenantModule() {
	}

	/**
	 * Primary key baris pemetaan (identity database). Tidak dipakai sebagai kunci bisnis --
	 * identitas logis baris ini adalah pasangan (jenis usaha, {@link #getModuleCode()}) yang
	 * dijaga unique constraint tabel.
	 *
	 * @return id baris, {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key -- dipanggil Hibernate. Jangan diisi manual dari kode aplikasi.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Jenis usaha pemilik pemetaan ini (wajib). Bersama {@link #getModuleCode()} membentuk
	 * unique constraint tabel, sehingga satu modul tidak dapat terdaftar dua kali pada jenis usaha
	 * yang sama -- perlindungan di tingkat database, bukan sekadar pemeriksaan aplikasi.
	 *
	 * <p>Getter memakai {@code check(...)} milik {@link GeneralValueObject} untuk menetralkan
	 * proxy Hibernate yang tidak dapat diinisialisasi menjadi {@code null} alih-alih melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return jenis usaha pemilik, atau {@code null} bila proxy tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = false)
	public JenisUsahaTenant getJenisUsahaTenant() {
		jenisUsahaTenant = check(jenisUsahaTenant);
		return jenisUsahaTenant;
	}

	/**
	 * Tautkan pemetaan ke jenis usaha pemiliknya.
	 *
	 * @param jenisUsahaTenant baris master jenis usaha (wajib saat disimpan)
	 */
	public void setJenisUsahaTenant(JenisUsahaTenant jenisUsahaTenant) {
		this.jenisUsahaTenant = jenisUsahaTenant;
	}

	/** Kode modul stabil huruf besar (mis. {@code POS}, {@code PERSEDIAAN}, {@code KAS_JURNAL}).
	 *
	 *  <p>Nilainya disalin apa adanya menjadi {@code TenantModuleEntitlement.moduleCode} saat
	 *  provisioning, dan di sanalah kode itu dicocokkan dengan daftar modul yang sudah benar-benar
	 *  operasional ({@code TenantEntitlementService.modulOperasional(...)}, perbandingan tidak peka
	 *  huruf besar-kecil): yang operasional dicatat berstatus aktif, sisanya berstatus terencana.
	 *  Karena pencocokan itu berbasis teks, salah ketik kode di sini tidak akan menimbulkan error --
	 *  modulnya hanya diam-diam berakhir sebagai entitlement "terencana" yang tidak berfungsi.</p> */
	@Column(name = "module_code", nullable = false, length = 64)
	public String getModuleCode() {
		return moduleCode;
	}

	/**
	 * Tetapkan kode modul.
	 *
	 * @param moduleCode kode modul huruf besar
	 */
	public void setModuleCode(String moduleCode) {
		this.moduleCode = moduleCode;
	}

	/**
	 * Penanda modul ikut diaktifkan otomatis saat provisioning tenant. Getter mengembalikan
	 * {@code TRUE} bila kolom kosong -- default dibaca-saja (tidak menulis balik ke field) yang
	 * bersifat PERMISIF: baris pemetaan baru yang lupa mengisi kolom ini akan langsung ikut
	 * terbawa ke entitlement tenant.
	 *
	 * <p>Inilah SATU-SATUNYA flag yang benar-benar dipakai untuk menyaring modul: kueri
	 * {@code TenantEntitlementService.terapkanDariJenisUsaha(...)} membatasi hasil pada
	 * {@code defaultEnabled = true}. Karena nilai default permisif, cara yang benar untuk
	 * menampilkan sebuah modul di kartu wizard TANPA mengaktifkannya bagi tenant adalah mengisi
	 * kolom ini {@code false} secara eksplisit -- mengosongkannya tidak cukup.</p>
	 *
	 * @return {@code TRUE} bila modul ikut aktif otomatis (termasuk saat kolom kosong)
	 */
	@Column(name = "default_enabled")
	public Boolean getDefaultEnabled() {
		return defaultEnabled == null ? Boolean.TRUE : defaultEnabled;
	}

	/**
	 * Tetapkan apakah modul ikut aktif otomatis saat provisioning.
	 *
	 * @param defaultEnabled {@code FALSE} agar modul tidak dibawa ke entitlement tenant
	 */
	public void setDefaultEnabled(Boolean defaultEnabled) {
		this.defaultEnabled = defaultEnabled;
	}

	/**
	 * Penanda modul bersifat wajib bagi jenis usaha ini (tidak boleh dilepas tenant).
	 *
	 * <p><strong>FIELD TIDUR.</strong> Audit pemakai menunjukkan tidak ada satu pun kode yang
	 * membaca nilai ini: penyaringan modul saat provisioning hanya memandang
	 * {@link #getDefaultEnabled()}. Konsekuensi yang mudah menjebak: baris dengan
	 * {@code required = true} tetapi {@code defaultEnabled = false} TIDAK akan pernah diprovisikan
	 * -- "wajib" di kolom ini tidak mengalahkan penyaring yang sesungguhnya. Selama belum ada
	 * mekanisme yang membacanya, perlakukan kolom ini sebagai keterangan saja, dan pastikan modul
	 * yang benar-benar wajib juga ber-{@code defaultEnabled = true}.</p>
	 *
	 * <p>Getter mengembalikan {@code FALSE} bila kolom kosong (default dibaca-saja, arah aman).</p>
	 *
	 * @return {@code TRUE} bila ditandai wajib, {@code FALSE} bila tidak/belum diisi
	 */
	@Column(name = "required")
	public Boolean getRequired() {
		return required == null ? Boolean.FALSE : required;
	}

	/**
	 * Tandai modul sebagai wajib bagi jenis usaha ini (saat ini belum berefek -- lihat
	 * {@link #getRequired()}).
	 *
	 * @param required {@code TRUE} untuk menandai wajib
	 */
	public void setRequired(Boolean required) {
		this.required = required;
	}

	/**
	 * Urutan tampil modul pada kartu jenis usaha di wizard (menaik); dipakai {@code katalog()} saat
	 * menyusun daftar kode modul per kartu. Getter mengembalikan {@code 0} bila kolom kosong --
	 * default dibaca-saja sehingga baris tanpa urutan berkumpul di awal daftar alih-alih membuat
	 * pengurutan gagal. Murni kosmetik: urutan tidak memengaruhi entitlement yang terbentuk.
	 *
	 * @return urutan tampil, {@code 0} bila belum diisi
	 */
	@Column(name = "display_order")
	public Integer getDisplayOrder() {
		return displayOrder == null ? Integer.valueOf(0) : displayOrder;
	}

	/**
	 * Tetapkan urutan tampil modul.
	 *
	 * @param displayOrder nomor urut menaik
	 */
	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	/**
	 * Nama pengguna yang terakhir menyentuh baris pemetaan (shadow audit AIS).
	 *
	 * @return penanda pengubah terakhir
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Isi penanda pengubah terakhir. Nilai null/kosong sengaja diabaikan (pola shadow audit AIS)
	 * agar jejak lama tidak terhapus oleh pemanggil yang lupa mengisinya.
	 *
	 * @param oleh nama pengubah; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna yang terakhir menyentuh baris pemetaan (pendamping {@link #getOleh()}).
	 *
	 * @return id pengubah terakhir
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Isi id pengubah terakhir; nilai null/kosong diabaikan, sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id pengubah; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu perubahan terakhir (shadow audit AIS), disegarkan hook {@code @PreUpdate}
	 * lewat {@code AuditTimestampInterceptor} dan sudah terisi sejak objek dibentuk. Karena entity
	 * ini tidak memiliki kolom waktu pembuatan, nilai awal stempel inilah satu-satunya petunjuk
	 * kapan baris pemetaan mulai ada di luar riwayat Envers.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Tetapkan stempel waktu perubahan terakhir (umumnya hanya dipanggil interceptor audit).
	 *
	 * @param tanggal_dirubah waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
