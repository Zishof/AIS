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
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Domain/subdomain milik satu tenant ({@code normalized_domain} unique global).</h3>
 *
 * <p>Subdomain bawaan ({@code <slug>.ebisnis.id}) dibuat provisioning; custom domain
 * diverifikasi SETELAH tenant aktif (token hash -- token mentah tidak disimpan).
 * {@code Pendaftar.domain} existing tetap menyimpan slug kompatibilitas tenant PERTAMA;
 * tenant/domain tambahan hidup di tabel ini.</p>
 *
 * <h4>Bagaimana domain dipetakan ke tenant -- dan mengapa itu penting</h4>
 * <p>
 * Pemetaan domain &rarr; tenant adalah titik rawan klasik pada aplikasi multi-tenant: bila
 * pencocokannya longgar (mis. mencari domain yang <i>mengandung</i> potongan nama alih-alih
 * sama persis), penyerang cukup mendaftarkan nama yang memuat nama korban untuk mengarahkan
 * request ke tenant lain. Untuk paket ini, penelusuran seluruh pemakai kelas tersebut memberi
 * dua kesimpulan yang perlu dicatat:
 * </p>
 * <ol>
 * <li><b>Resolusi tenant tidak dilakukan lewat domain sama sekali.</b>
 * {@code ais.service.tenant.TenantContextResolver} -- satu-satunya pembentuk
 * {@code TenantContext} -- menuntut tenant dinyatakan eksplisit lewat id ({@code resolve}) atau
 * {@code code} ({@code resolveByCode}), atau disimpulkan dari keanggotaan aktor yang jumlahnya
 * tepat satu ({@code resolveOtomatis}). Tidak ada pembacaan {@code Host}/{@code getServerName()}
 * yang berujung pada pemilihan tenant. Setelah tenant ditemukan pun, aktor masih harus lolos
 * {@code TenantMembershipResolver}; kepemilikan domain tidak pernah menjadi bukti hak akses.</li>
 * <li><b>Pencocokan yang ada bersifat sama-persis, bukan substring.</b> Kolom
 * {@link #getNormalizedDomain()} bertanda {@code unique} dan diisi bentuk huruf kecil (punycode
 * untuk IDN), sehingga pencarian yang kelak ditambahkan bekerja atas kesetaraan penuh satu
 * kolom, bukan pencocokan sebagian. Ini berbeda dari pola pencocokan substring yang tercatat
 * pada jalur lama {@code SekolahUtil.getYayasanData()}: di sana kunci pencarian dibandingkan
 * sebagai potongan, sehingga nama yang saling memuat dapat tertukar.</li>
 * </ol>
 * <p>
 * Dengan kata lain, saat ini entitas ini bersifat <b>catatan</b>: provisioning menuliskan
 * subdomain bawaan {@code &lt;slug&gt;.&lt;base&gt;} (satu-satunya penulis di kode), dan belum ada
 * pembaca yang menerjemahkan domain menjadi tenant. Bila kelak resolusi berbasis domain
 * ditambahkan, dua aturan berikut wajib dipegang: cocokkan {@link #getNormalizedDomain()} dengan
 * kesetaraan penuh (bukan {@code like}/{@code contains}), dan terima hanya baris berstatus
 * {@link #STATUS_ACTIVE} -- {@link #STATUS_PENDING_VERIFICATION} berarti kepemilikan domain belum
 * dibuktikan, sehingga siapa pun dapat mengklaim domain milik orang lain bila status itu ikut
 * diterima.
 * </p>
 *
 * <h4>Verifikasi kepemilikan domain kustom</h4>
 * <p>
 * Subdomain bawaan langsung {@link #STATUS_ACTIVE} dan {@link #getVerifiedAt()} terisi: ia berada
 * di bawah domain milik penyedia layanan sendiri, jadi tidak ada yang perlu dibuktikan. Domain
 * kustom sebaliknya lahir sebagai {@link #STATUS_PENDING_VERIFICATION} dengan
 * {@link #getVerificationTokenHash()} terisi; pemilik membuktikan penguasaannya dengan memasang
 * token mentah (yang tidak pernah disimpan) pada DNS/berkas di domainnya, dan barulah statusnya
 * dinaikkan. Pola menyimpan hash saja identik dengan
 * {@link SchemaNameReservation#getReservationTokenHash()} dan verifikasi email pendaftaran:
 * bocornya isi tabel tidak memberi siapa pun kemampuan menyelesaikan verifikasi.
 * </p>
 *
 * @see TenantRegistry
 * @see ProvisioningStep#STEP_CREATE_TENANT_REGISTRY
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "tenant_domain")
public class TenantDomain extends GeneralValueObject {

	/** Versi serialisasi entitas; tetap {@code 1L} selama bentuk medan tidak berubah. */
	private static final long serialVersionUID = 1L;

	/**
	 * Subdomain bawaan di bawah domain penyedia ({@code &lt;slug&gt;.&lt;base&gt;}), dibuat
	 * provisioning. Sekaligus nilai bawaan getter tipe untuk kolom kosong -- jenis yang paling
	 * tidak istimewa, sebab domain kustom seharusnya dinyatakan eksplisit.
	 */
	public static final String TYPE_SUBDOMAIN = "SUBDOMAIN";

	/**
	 * Domain milik tenant sendiri. Baru boleh dipakai setelah kepemilikannya dibuktikan lewat
	 * token verifikasi.
	 */
	public static final String TYPE_CUSTOM = "CUSTOM";

	/** Domain aktif dan boleh dipakai. */
	public static final String STATUS_ACTIVE = "ACTIVE";

	/**
	 * Kepemilikan domain belum dibuktikan. Sekaligus nilai bawaan getter status untuk kolom
	 * kosong -- default gagal-tertutup: baris yang datanya tidak lengkap dianggap BELUM
	 * terverifikasi, bukan aktif.
	 */
	public static final String STATUS_PENDING_VERIFICATION = "PENDING_VERIFICATION";

	/** Domain dinonaktifkan (dicabut admin atau dilepas tenant), tetapi barisnya tetap tersimpan. */
	public static final String STATUS_DISABLED = "DISABLED";

	/** Kunci utama teknis (IDENTITY). */
	private Long id;
	/** Tenant pemilik domain ini; wajib. */
	private TenantRegistry tenant;
	/** Domain sebagaimana ditulis/ditampilkan. */
	private String domain;
	/** Bentuk ternormalisasi domain (huruf kecil + punycode); kunci unique global. */
	private String normalizedDomain;
	/** Jenis domain: SUBDOMAIN bawaan atau CUSTOM milik tenant. */
	private String type;
	/** Status domain: ACTIVE/PENDING_VERIFICATION/DISABLED. */
	private String status;
	/** Hash token pembuktian kepemilikan domain kustom. */
	private String verificationTokenHash;
	/** Saat kepemilikan domain terbukti. */
	private Date verifiedAt;
	/** Penanda domain utama tenant. */
	private Boolean primaryDomain;
	/** Saat baris domain dibuat. */
	private Date createdAt;
	/** Penghitung penguncian optimistik Hibernate ({@code @Version}). */
	private Integer version;

	/** Nama pengguna/proses penulis terakhir (audit shadow); diisi {@code "provisioning"}. */
	private String oleh;
	/** Id pengguna/proses penulis terakhir (audit shadow). */
	private String olehId;
	/**
	 * Cap waktu perubahan terakhir. Callback {@code @PreUpdate} dan deklarasi medan pada satu baris
	 * fisik adalah bentuk baku pola audit shadow repo ini (disisipkan alat pembangkit), bukan
	 * kelalaian format.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public TenantDomain() {
	}

	/**
	 * Kunci utama teknis, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return id baris domain, atau {@code null} bila belum disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci utama. Hanya Hibernate yang seharusnya memanggil ini.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Tenant pemilik domain ini (wajib). Satu tenant boleh punya beberapa domain: subdomain bawaan
	 * ditambah domain kustom.
	 *
	 * <p>Getter memakai {@code check(...)} milik {@code GeneralValueObject}, yang mengubah proxy
	 * Hibernate yang tidak dapat lagi di-inisialisasi menjadi {@code null} (hasilnya ditulis balik
	 * ke medan) alih-alih melempar {@code LazyInitializationException}.</p>
	 *
	 * @return tenant pemilik, atau {@code null} bila relasinya tidak dapat dimuat
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tenant_id", nullable = false)
	public TenantRegistry getTenant() {
		tenant = check(tenant);
		return tenant;
	}

	/**
	 * Menetapkan tenant pemilik domain.
	 *
	 * @param tenant registry tenant pemilik
	 */
	public void setTenant(TenantRegistry tenant) {
		this.tenant = tenant;
	}

	/**
	 * Domain sebagaimana ditulis/ditampilkan (belum tentu huruf kecil, dan untuk IDN masih dalam
	 * bentuk Unicode). Kolom ini untuk tampilan; yang dipakai membandingkan adalah
	 * {@link #getNormalizedDomain()}.
	 *
	 * @return domain bentuk tampilan
	 */
	@Column(name = "domain", nullable = false, length = 255)
	public String getDomain() {
		return domain;
	}

	/**
	 * Menetapkan domain bentuk tampilan.
	 *
	 * @param domain domain sebagaimana ditulis pemilik
	 */
	public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
	 * lowercase + punycode (bila custom IDN) -- kunci unique global.
	 *
	 * <p>Keunikan global inilah yang mencegah dua tenant mengklaim domain yang sama: yang kedua
	 * kalah pada constraint, bukan pada pemeriksaan aplikasi yang bisa terlewat. Perbandingan
	 * apa pun terhadap kolom ini harus berupa kesetaraan penuh; pencocokan sebagian
	 * ({@code like '%...%'}) akan membuat domain yang saling memuat tertukar -- lihat uraian
	 * pada Javadoc kelas.</p>
	 *
	 * @return domain ternormalisasi; tidak pernah {@code null} pada baris tersimpan
	 */
	@Column(name = "normalized_domain", unique = true, nullable = false, length = 255)
	public String getNormalizedDomain() {
		return normalizedDomain;
	}

	/**
	 * Menetapkan bentuk ternormalisasi domain. Pemanggil bertanggung jawab menormalisasi
	 * (huruf kecil; punycode untuk IDN) -- entitas ini tidak melakukannya sendiri.
	 *
	 * @param normalizedDomain domain huruf kecil/punycode
	 */
	public void setNormalizedDomain(String normalizedDomain) {
		this.normalizedDomain = normalizedDomain;
	}

	/**
	 * Jenis domain. Kolom kosong dibaca {@link #TYPE_SUBDOMAIN} (tanpa menulis balik ke medan),
	 * yaitu jenis yang tidak menuntut pembuktian kepemilikan pihak luar.
	 *
	 * @return {@link #TYPE_SUBDOMAIN} atau {@link #TYPE_CUSTOM}
	 */
	@Column(name = "type", nullable = false, length = 20)
	public String getType() {
		return type == null || type.trim().isEmpty() ? TYPE_SUBDOMAIN : type;
	}

	/**
	 * Menetapkan jenis domain.
	 *
	 * @param type {@link #TYPE_SUBDOMAIN} atau {@link #TYPE_CUSTOM}
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Status domain. Kolom kosong dibaca {@link #STATUS_PENDING_VERIFICATION} -- default
	 * gagal-tertutup yang penting: baris yang datanya tidak lengkap dianggap belum terbukti
	 * kepemilikannya, bukan aktif. Pembaca yang kelak memetakan domain ke tenant wajib menuntut
	 * {@link #STATUS_ACTIVE}.
	 *
	 * @return salah satu konstanta {@code STATUS_*}; minimal {@link #STATUS_PENDING_VERIFICATION}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING_VERIFICATION : status;
	}

	/**
	 * Menetapkan status domain.
	 *
	 * @param status salah satu konstanta {@code STATUS_*}
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Hash token pembuktian kepemilikan domain kustom; token mentahnya diberikan kepada pemilik
	 * dan tidak pernah disimpan. Untuk subdomain bawaan kolom ini kosong -- tidak ada yang perlu
	 * dibuktikan atas domain milik penyedia sendiri.
	 *
	 * @return hash token verifikasi, atau {@code null}
	 */
	@Column(name = "verification_token_hash", length = 64)
	public String getVerificationTokenHash() {
		return verificationTokenHash;
	}

	/**
	 * Menetapkan hash token verifikasi. Isi dengan hash-nya, jangan pernah token mentahnya.
	 *
	 * @param verificationTokenHash hash token verifikasi
	 */
	public void setVerificationTokenHash(String verificationTokenHash) {
		this.verificationTokenHash = verificationTokenHash;
	}

	/**
	 * Saat kepemilikan domain terbukti. Untuk subdomain bawaan diisi provisioning bersamaan dengan
	 * pembuatan barisnya.
	 *
	 * @return cap waktu verifikasi, atau {@code null} bila belum terverifikasi
	 */
	@Column(name = "verified_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getVerifiedAt() {
		return verifiedAt;
	}

	/**
	 * Menetapkan cap waktu verifikasi kepemilikan.
	 *
	 * @param verifiedAt cap waktu verifikasi
	 */
	public void setVerifiedAt(Date verifiedAt) {
		this.verifiedAt = verifiedAt;
	}

	/**
	 * Penanda domain utama tenant (dipakai saat menyusun tautan yang dikirim ke pengguna).
	 * Getter mengembalikan {@link Boolean#FALSE} untuk kolom kosong sehingga pemanggil tidak perlu
	 * menangani {@code null}; nilai default itu tidak ditulis balik ke medan.
	 *
	 * <p>Perhatikan bahwa keunikan &quot;hanya satu domain utama per tenant&quot; TIDAK ditegakkan
	 * constraint apa pun -- itu kesepakatan yang dijaga lapisan penulis. Provisioning menandai
	 * subdomain bawaan sebagai utama; penulis berikutnya bertanggung jawab menurunkan penanda
	 * pada domain lama bila memindahkannya.</p>
	 *
	 * @return {@code true} bila domain utama; minimal {@link Boolean#FALSE}
	 */
	@Column(name = "primary_domain")
	public Boolean getPrimaryDomain() {
		return primaryDomain == null ? Boolean.FALSE : primaryDomain;
	}

	/**
	 * Menetapkan penanda domain utama.
	 *
	 * @param primaryDomain {@code true} bila domain ini utama
	 */
	public void setPrimaryDomain(Boolean primaryDomain) {
		this.primaryDomain = primaryDomain;
	}

	/**
	 * Saat baris domain dibuat.
	 *
	 * @return cap waktu pembuatan
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan cap waktu pembuatan baris domain.
	 *
	 * @param createdAt cap waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Penghitung penguncian optimistik Hibernate; menjaga perubahan status/penanda utama yang
	 * datang dari transaksi berbeda tidak saling menimpa diam-diam.
	 *
	 * @return nomor versi baris
	 */
	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	/**
	 * Menetapkan nomor versi; dikelola Hibernate, bukan kode aplikasi.
	 *
	 * @param version nomor versi baris
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Nama pengguna/proses penulis terakhir (pola audit shadow); diisi {@code "provisioning"} untuk
	 * subdomain bawaan.
	 *
	 * @return nama penulis terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama penulis terakhir. Nilai {@code null}/kosong sengaja diabaikan supaya jejak
	 * penulis sebelumnya tidak terhapus.
	 *
	 * @param oleh nama penulis; nilai kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna/proses penulis terakhir (audit shadow).
	 *
	 * @return id penulis terakhir, atau {@code null}
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id penulis terakhir; nilai kosong diabaikan, sama seperti {@link #setOleh(String)}.
	 *
	 * @param olehId id penulis; nilai kosong diabaikan
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Cap waktu perubahan terakhir, diperbarui otomatis lewat {@code @PreUpdate}
	 * ({@code AuditTimestampInterceptor.ubah}); nilai awal diisi saat objek dibuat.
	 *
	 * @return cap waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan cap waktu perubahan terakhir; umumnya diurus callback {@code @PreUpdate}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
