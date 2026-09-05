package ais.database.model.tenant;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Audit event BISNIS alur pendaftaran/provisioning tenant (§18 dokumen master).</h3>
 *
 * <p>Berbeda dari {@code public.error_log} (exception) dan dari Envers (riwayat baris) --
 * tabel ini mencatat KEJADIAN alur: {@code REGISTRATION_SUBMITTED}, {@code EMAIL_VERIFIED},
 * {@code TENANT_READY}, dst. FK memakai id polos (bukan relasi) supaya insert audit tidak
 * pernah menggagalkan transaksi utama karena lazy-loading/cascade. Password/OTP/token
 * TIDAK PERNAH masuk ke sini.</p>
 *
 * <h3>Verifikasi isi log: tidak ada data sensitif mentah</h3>
 *
 * <p>Klaim "password/OTP/token tidak pernah masuk ke sini" pada paragraf di atas diperiksa
 * dengan menelusuri SELURUH penulis entity ini, bukan sekadar membaca deklarasi kolomnya.
 * Terdapat lima kelas yang membuat baris {@code PendaftaranAuditEvent}, yaitu
 * {@code PendaftaranTenantService}, {@code PendaftaranTenantAdminService},
 * {@code EmailVerificationService}, {@code TenantOnboardingService}, dan
 * {@code TenantProvisioningService}. Nilai yang benar-benar mereka tuliskan ke kolom
 * {@link #getDetailJson()} hanya berupa: daftar kode jenis usaha yang dipilih pendaftar,
 * pasangan versi dokumen persetujuan (mis. {@code "terms=...;privacy=..."}), penanda
 * {@code "manual-admin"} untuk verifikasi manual oleh administrator, alasan bebas yang
 * diketik administrator pada aksi penolakan/percobaan ulang, atau {@code null}. Tidak satu
 * pun titik penulisan menyertakan password, hash password, token verifikasi, hash token,
 * maupun OTP. Verifikasi ini memperkuat pemeriksaan sisi lain yang menunjukkan bahwa token
 * mentah hanya hidup di memori selama perakitan tautan email dan tidak pernah
 * dipersistensikan ke mana pun.</p>
 *
 * <p>Dua kolom yang berpotensi memuat data pribadi ditangani dengan sadar. Alamat IP sumber
 * TIDAK disimpan mentah melainkan sebagai SHA-256 hex pada {@link #getSourceIpHash()},
 * sehingga korelasi antar peristiwa tetap dimungkinkan tanpa menyimpan pengidentifikasi
 * jaringan yang dapat dibaca langsung. String {@code User-Agent} disimpan apa adanya namun
 * dirapikan dan dipotong ke batas 500 karakter sebelum ditulis, sehingga tidak dapat dipakai
 * menyelundupkan payload besar ke dalam tabel audit. Keduanya sejalan dengan pendekatan yang
 * dipakai di seluruh paket ini, yang membedakan data yang perlu dapat dibaca kembali
 * (mis. alamat email tujuan) dari data yang cukup dapat dicocokkan (mis. alamat IP).</p>
 *
 * <p>Perlu dicatat satu perilaku yang disengaja: penulisan audit dibungkus penangkap
 * exception yang MENELAN kegagalan dan hanya mencatatnya ke {@code error_log}. Artinya
 * kegagalan menulis audit tidak akan pernah menggagalkan transaksi bisnis yang sedang
 * berjalan. Pilihan ini konsisten dengan keputusan memakai id polos alih-alih relasi pada
 * kolom {@code pendaftar_id}/{@code registration_id}/{@code tenant_id}, yang menghilangkan
 * risiko {@code LazyInitializationException} maupun efek cascade tak terduga saat menyisipkan
 * baris audit. Konsekuensinya harus dipahami saat menganalisis data: tabel ini adalah jejak
 * yang bersifat best-effort untuk keperluan penelusuran alur, BUKAN jurnal transaksional
 * yang dijamin lengkap, sehingga ketiadaan sebuah baris tidak dapat dijadikan bukti bahwa
 * peristiwanya tidak pernah terjadi. Untuk pertanyaan yang menuntut kelengkapan mutlak,
 * rujukan yang benar adalah riwayat Envers atas entity yang bersangkutan — dan kelas ini pun
 * ber-{@code @Audited}, sehingga baris audit yang sudah tertulis tidak dapat diubah atau
 * dihapus diam-diam tanpa meninggalkan jejak di tabel bayangannya.</p>
 *
 * @see PendaftaranTenant
 * @see PendaftaranEmailVerification
 * @see PendaftaranConsent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_audit_event")
public class PendaftaranAuditEvent extends GeneralValueObject {

	/** Versi serialisasi Java standar untuk seluruh entity model AIS. */
	private static final long serialVersionUID = 1L;

	/** Formulir pendaftaran publik dibuka pengunjung. */
	public static final String EV_REGISTRATION_FORM_OPENED = "REGISTRATION_FORM_OPENED";
	/** Ketersediaan username diperiksa dari formulir publik. */
	public static final String EV_USERNAME_CHECKED = "USERNAME_CHECKED";
	/** Formulir pendaftaran dikirim dan lolos validasi. */
	public static final String EV_REGISTRATION_SUBMITTED = "REGISTRATION_SUBMITTED";
	/** Baris {@code Pendaftar} baru dibuat untuk pendaftar yang belum punya akun. */
	public static final String EV_PENDAFTAR_CREATED = "PENDAFTAR_CREATED";
	/** Jenis usaha dipilih pendaftar; kode-kodenya dicatat di {@code detailJson}. */
	public static final String EV_BUSINESS_TYPES_SELECTED = "BUSINESS_TYPES_SELECTED";
	/** Syarat &amp; ketentuan disetujui; versi dokumennya dicatat di {@code detailJson}. */
	public static final String EV_CONSENT_ACCEPTED = "CONSENT_ACCEPTED";
	/** Email verifikasi dikirim (hasil SENT/SEND_FAILED ada di {@code result}). */
	public static final String EV_EMAIL_VERIFICATION_SENT = "EMAIL_VERIFICATION_SENT";
	/** Alamat email berhasil diverifikasi, lewat tautan token maupun manual oleh admin. */
	public static final String EV_EMAIL_VERIFIED = "EMAIL_VERIFIED";
	/** Permohonan masuk antrean provisioning tenant. */
	public static final String EV_TENANT_PROVISIONING_QUEUED = "TENANT_PROVISIONING_QUEUED";
	/** Schema database milik tenant berhasil dibuat. */
	public static final String EV_TENANT_SCHEMA_CREATED = "TENANT_SCHEMA_CREATED";
	/** Migrasi struktur diterapkan pada schema tenant. */
	public static final String EV_TENANT_MIGRATION_APPLIED = "TENANT_MIGRATION_APPLIED";
	/** Data awal (seed) diisikan ke schema tenant. */
	public static final String EV_TENANT_SEEDED = "TENANT_SEEDED";
	/** Akun/keanggotaan pemilik tenant dibuat (lihat {@link TenantMembership}). */
	public static final String EV_OWNER_CREATED = "OWNER_CREATED";
	/** Seluruh langkah provisioning selesai; tenant siap dipakai. */
	public static final String EV_TENANT_READY = "TENANT_READY";
	/** Tenant diaktifkan sehingga dapat diakses pemiliknya. */
	public static final String EV_TENANT_ACTIVATED = "TENANT_ACTIVATED";
	/** Provisioning gagal; alasannya dicatat di {@code reason}. */
	public static final String EV_PROVISIONING_FAILED = "PROVISIONING_FAILED";
	/** Provisioning yang gagal dicoba ulang (otomatis maupun oleh admin). */
	public static final String EV_PROVISIONING_RETRIED = "PROVISIONING_RETRIED";
	/** Permohonan ditolak administrator. */
	public static final String EV_REGISTRATION_REJECTED = "REGISTRATION_REJECTED";
	/** Permohonan dibatalkan pendaftar sendiri. */
	public static final String EV_REGISTRATION_CANCELLED = "REGISTRATION_CANCELLED";
	/**
	 * Permohonan ditandai basi oleh penyapu latar ({@code ReservationExpiryScheduler}): masih
	 * {@code STATUS_EMAIL_VERIFICATION_PENDING}/{@code STATUS_SUBMITTED} saat reservasi
	 * username/schema-nya ({@code SchemaNameReservation}) melewati {@code expiresAt} -- ditinggalkan
	 * sebelum verifikasi email, bukan dibatalkan pendaftar.
	 */
	public static final String EV_REGISTRATION_EXPIRED = "REGISTRATION_EXPIRED";
	/** Permintaan diblokir kontrol keamanan (rate limit, honeypot, elapsed-time). */
	public static final String EV_SECURITY_BLOCKED = "SECURITY_BLOCKED";

	/** Aktor: pengunjung anonim pada permukaan pendaftaran publik. */
	public static final String ACTOR_PUBLIC = "PUBLIC";
	/** Aktor: pendaftar yang sudah teridentifikasi. */
	public static final String ACTOR_PENDAFTAR = "PENDAFTAR";
	/** Aktor: administrator platform. */
	public static final String ACTOR_ADMIN = "ADMIN";
	/** Aktor: proses otomatis (mesin provisioning, pengirim email); default {@link #getActorType()}. */
	public static final String ACTOR_SYSTEM = "SYSTEM";

	/** Primary key surrogate, IDENTITY dari sequence PostgreSQL. */
	private Long id;
	/** Kode peristiwa; salah satu konstanta {@code EV_*}. */
	private String eventCode;
	/** Jenis aktor pemicu; salah satu konstanta {@code ACTOR_*}. */
	private String actorType;
	/** Id {@code Pendaftar} terkait — id polos, sengaja bukan relasi. */
	private Long pendaftarId;
	/** Id permohonan pendaftaran terkait — id polos, sengaja bukan relasi. */
	private Long registrationId;
	/** Id tenant terkait — id polos, sengaja bukan relasi. */
	private Long tenantId;
	/** Id request dari klien, untuk mengaitkan peristiwa dengan satu panggilan HTTP. */
	private String requestId;
	/** Id korelasi lintas peristiwa/proses. */
	private String correlationId;
	/** SHA-256 hex alamat IP sumber — IP mentah TIDAK disimpan. */
	private String sourceIpHash;
	/** String {@code User-Agent} peramban, dipotong maksimal 500 karakter. */
	private String userAgent;
	/** Detail non-sensitif dalam JSON kecil. */
	private String detailJson;
	/** Alasan bebas (mis. alasan penolakan admin atau pesan kegagalan). */
	private String reason;
	/** Hasil ringkas peristiwa, mis. {@code "OK"}, {@code "SENT"}, {@code "SEND_FAILED"}. */
	private String result;
	/** Waktu peristiwa terjadi (wajib). */
	private Date waktu;

	/** Nama pengguna pembuat/pengubah baris — field audit shadow wajib pola AIS. */
	private String oleh;
	/** Id pengguna pembuat/pengubah baris — field audit shadow wajib pola AIS. */
	private String olehId;
	/**
	 * Stempel waktu perubahan terakhir. Deklarasi satu baris bersama {@code @PreUpdate}
	 * di bawah ini adalah KEHARUSAN TEKNIS pola AIS (bukan gaya penulisan yang keliru):
	 * interceptor {@code AuditTimestampInterceptor.ubah} dipanggil Hibernate sebelum setiap
	 * update sehingga stempel waktu terisi tanpa campur tangan kode pemanggil.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate untuk instansiasi reflektif. */
	public PendaftaranAuditEvent() {
	}

	/**
	 * Primary key baris audit. Dibangkitkan database (IDENTITY) saat insert, sehingga
	 * bernilai {@code null} selama objek masih transient. Karena baris audit hanya
	 * disisipkan dan tidak pernah diubah pada pemakaian normal, urutan id sekaligus
	 * mencerminkan urutan pencatatan peristiwa.
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Dipakai Hibernate; kode aplikasi normal tidak perlu memanggilnya.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode peristiwa yang dicatat, diisi salah satu konstanta {@code EV_*} kelas ini. Kolom
	 * {@code NOT NULL} sepanjang 64 karakter tanpa constraint enum di database, sehingga
	 * konsistensi nilai sepenuhnya bergantung pada disiplin penulis. Beberapa titik
	 * pemanggilan administratif memang menuliskan kode literal yang belum punya konstanta
	 * (mis. {@code "REGISTRATION_APPROVED"}, {@code "RESERVATION_RELEASED"},
	 * {@code "MANUAL_VERIFICATION"}), jadi analisis data tidak boleh berasumsi bahwa himpunan
	 * nilai yang muncul terbatas pada konstanta yang dideklarasikan di sini.
	 *
	 * @return kode peristiwa, atau {@code null} bila belum diisi
	 */
	@Column(name = "event_code", nullable = false, length = 64)
	public String getEventCode() {
		return eventCode;
	}

	/**
	 * Menetapkan kode peristiwa. Gunakan konstanta {@code EV_*} bila tersedia.
	 *
	 * @param eventCode kode peristiwa
	 */
	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	/**
	 * Jenis aktor yang memicu peristiwa. Getter mem-default ke {@link #ACTOR_SYSTEM} bila
	 * field {@code null}/kosong, sehingga peristiwa yang dicatat proses otomatis tetap
	 * terbaca benar walau penulisnya lupa mengisi; default ini hanya berlaku di lapisan Java,
	 * kolom database tetap menyimpan nilai aslinya.
	 *
	 * @return kode jenis aktor, tidak pernah {@code null}
	 */
	@Column(name = "actor_type", length = 20)
	public String getActorType() {
		return actorType == null || actorType.trim().isEmpty() ? ACTOR_SYSTEM : actorType;
	}

	/**
	 * Menetapkan jenis aktor. Gunakan konstanta {@code ACTOR_*} kelas ini.
	 *
	 * @param actorType kode jenis aktor
	 */
	public void setActorType(String actorType) {
		this.actorType = actorType;
	}

	/**
	 * Id {@code Pendaftar} yang terkait peristiwa, disimpan sebagai id polos dan BUKAN
	 * relasi {@code @ManyToOne}. Pilihan ini disengaja: menyisipkan baris audit tidak boleh
	 * memicu pemuatan lazy, cascade, maupun kegagalan integritas referensial yang dapat
	 * menggagalkan transaksi bisnis yang sedang berjalan. Konsekuensinya, tidak ada foreign
	 * key yang menjamin id ini masih menunjuk baris yang ada — pembacaan data harus
	 * menoleransi id yang menggantung.
	 *
	 * @return id pendaftar, atau {@code null} bila peristiwa belum terkait pendaftar
	 */
	@Column(name = "pendaftar_id")
	public Long getPendaftarId() {
		return pendaftarId;
	}

	/**
	 * Menetapkan id {@code Pendaftar} terkait.
	 *
	 * @param pendaftarId id pendaftar
	 */
	public void setPendaftarId(Long pendaftarId) {
		this.pendaftarId = pendaftarId;
	}

	/**
	 * Id permohonan pendaftaran ({@link PendaftaranTenant}) terkait peristiwa. Sama seperti
	 * {@link #getPendaftarId()}, disimpan sebagai id polos tanpa relasi maupun foreign key.
	 *
	 * @return id permohonan, atau {@code null} bila peristiwa belum terikat permohonan
	 */
	@Column(name = "registration_id")
	public Long getRegistrationId() {
		return registrationId;
	}

	/**
	 * Menetapkan id permohonan pendaftaran terkait.
	 *
	 * @param registrationId id permohonan
	 */
	public void setRegistrationId(Long registrationId) {
		this.registrationId = registrationId;
	}

	/**
	 * Id tenant ({@link TenantRegistry}) terkait peristiwa, terisi pada peristiwa tahap
	 * provisioning ke atas. Disimpan sebagai id polos tanpa relasi maupun foreign key.
	 *
	 * @return id tenant, atau {@code null} bila tenant belum terbentuk
	 */
	@Column(name = "tenant_id")
	public Long getTenantId() {
		return tenantId;
	}

	/**
	 * Menetapkan id tenant terkait.
	 *
	 * @param tenantId id tenant
	 */
	public void setTenantId(Long tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * Id request yang dikirim klien, dipakai mengaitkan beberapa baris audit yang lahir dari
	 * satu panggilan HTTP yang sama. Nilainya berasal dari payload klien sehingga tidak boleh
	 * dipercaya sebagai pengidentifikasi yang unik atau tidak dapat dipalsukan; kegunaannya
	 * murni untuk penelusuran.
	 *
	 * @return id request, atau {@code null} bila tidak disertakan klien
	 */
	@Column(name = "request_id", length = 64)
	public String getRequestId() {
		return requestId;
	}

	/**
	 * Menetapkan id request.
	 *
	 * @param requestId id request dari klien
	 */
	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	/**
	 * Id korelasi untuk merangkai peristiwa lintas proses (mis. submit di web dengan langkah
	 * provisioning asinkron yang menyusul kemudian).
	 *
	 * @return id korelasi, atau {@code null} bila tidak dipakai
	 */
	@Column(name = "correlation_id", length = 64)
	public String getCorrelationId() {
		return correlationId;
	}

	/**
	 * Menetapkan id korelasi lintas proses.
	 *
	 * @param correlationId id korelasi
	 */
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

	/**
	 * SHA-256 heksadesimal dari alamat IP sumber — alamat IP mentah TIDAK pernah disimpan.
	 *
	 * <p>Hashing dilakukan penulis lewat {@code PasswordHashService.sha256Hex(String)}
	 * sebelum nilainya sampai ke sini, dan menghasilkan {@code null} bila IP kosong. Bentuk
	 * hash tetap memungkinkan pekerjaan yang benar-benar dibutuhkan analisis penyalahgunaan,
	 * yaitu mencocokkan apakah beberapa peristiwa berasal dari sumber yang sama, tanpa
	 * menyimpan pengidentifikasi jaringan yang dapat dibaca langsung oleh siapa pun yang
	 * membuka tabel audit. Perlu disadari bahwa ruang alamat IPv4 cukup kecil untuk
	 * dihabiskan secara menyeluruh, sehingga hash tanpa salt seperti ini bukan anonimisasi
	 * yang kuat terhadap pihak yang bersungguh-sungguh membalikkannya; ia adalah pengurangan
	 * paparan yang wajar untuk data operasional, bukan jaminan kerahasiaan.</p>
	 *
	 * @return SHA-256 hex alamat IP sumber, atau {@code null} bila tidak diketahui
	 */
	@Column(name = "source_ip_hash", length = 64)
	public String getSourceIpHash() {
		return sourceIpHash;
	}

	/**
	 * Menetapkan hash alamat IP sumber. Pemanggil WAJIB mengisi hash, bukan IP mentah.
	 *
	 * @param sourceIpHash SHA-256 hex alamat IP sumber
	 */
	public void setSourceIpHash(String sourceIpHash) {
		this.sourceIpHash = sourceIpHash;
	}

	/**
	 * String {@code User-Agent} peramban pendaftar. Berbeda dari alamat IP, nilai ini
	 * disimpan apa adanya karena tidak mengidentifikasi individu secara langsung dan berguna
	 * dibaca kembali saat menelusuri masalah kompatibilitas maupun pola otomatisasi. Penulis
	 * merapikan dan memotongnya ke batas 500 karakter sebelum menyimpan, sehingga string
	 * panjang yang dikirim klien tidak dapat dipakai membanjiri tabel audit. Karena isinya
	 * sepenuhnya dikendalikan klien, nilai ini harus diperlakukan sebagai data tidak
	 * terpercaya dan wajib di-escape saat ditampilkan di antarmuka mana pun.
	 *
	 * @return string User-Agent, atau {@code null} bila tidak disertakan
	 */
	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Menetapkan string {@code User-Agent}. Pemanggil bertanggung jawab merapikan dan
	 * memotongnya ke batas 500 karakter.
	 *
	 * @param userAgent string User-Agent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Before/after non-sensitif dalam JSON kecil (TANPA password/OTP/token/hash).
	 *
	 * <p>Larangan pada kalimat di atas diverifikasi terhadap seluruh titik penulisan, bukan
	 * hanya dinyatakan sebagai niat. Isi yang benar-benar ditulis ke kolom ini terbatas pada
	 * bentuk {@code {"detail": "..."}} dengan muatan berupa daftar kode jenis usaha pilihan
	 * pendaftar, pasangan versi dokumen persetujuan seperti {@code "terms=...;privacy=..."},
	 * penanda {@code "manual-admin"} pada verifikasi manual, alasan bebas yang diketik
	 * administrator, atau tidak diisi sama sekali. Tidak ada titik penulisan yang memasukkan
	 * rahasia dalam bentuk apa pun, termasuk hash-nya. Ini penting karena kolom bertipe
	 * {@code text} tanpa batas panjang seperti ini adalah tempat yang secara historis paling
	 * sering menampung "sekalian saja disimpan semua" pada modul-modul lama, dan karena kelas
	 * ini ber-{@code @Audited} sehingga apa pun yang masuk ke sini akan direplikasi ke tabel
	 * bayangan Envers dan tersimpan jauh lebih lama daripada baris aslinya.</p>
	 *
	 * <p>Aturan bagi penulis berikutnya karena itu sederhana dan tidak boleh dilonggarkan:
	 * kolom ini hanya untuk konteks bisnis yang aman dibaca operator dukungan pelanggan.
	 * Payload permintaan mentah, isi formulir apa adanya, header otentikasi, token, maupun
	 * kredensial dalam bentuk apa pun tidak boleh dituliskan ke sini.</p>
	 *
	 * @return JSON detail peristiwa, atau {@code null} bila tanpa detail
	 */
	@Column(name = "detail_json", columnDefinition = "text")
	public String getDetailJson() {
		return detailJson;
	}

	/**
	 * Menetapkan JSON detail peristiwa. Isi hanya dengan data non-sensitif; lihat larangan
	 * pada {@link #getDetailJson()}.
	 *
	 * @param detailJson JSON detail peristiwa
	 */
	public void setDetailJson(String detailJson) {
		this.detailJson = detailJson;
	}

	/**
	 * Alasan bebas yang menyertai peristiwa, mis. alasan penolakan permohonan yang diketik
	 * administrator atau ringkasan kegagalan provisioning. Karena dapat berisi teks yang
	 * diketik manusia, nilainya wajib di-escape saat ditampilkan.
	 *
	 * @return teks alasan, atau {@code null} bila tidak ada
	 */
	@Column(name = "reason", length = 500)
	public String getReason() {
		return reason;
	}

	/**
	 * Menetapkan teks alasan peristiwa.
	 *
	 * @param reason teks alasan
	 */
	public void setReason(String reason) {
		this.reason = reason;
	}

	/**
	 * Hasil ringkas peristiwa. Alur pendaftaran publik menuliskan {@code "OK"}, sedangkan
	 * pengiriman email verifikasi menuliskan {@code "SENT"} atau {@code "SEND_FAILED"} —
	 * pembedaan yang membuat kegagalan pengiriman tetap terekam meskipun kegagalan itu
	 * sengaja tidak menggagalkan pendaftaran.
	 *
	 * @return kode hasil, atau {@code null} bila tidak diisi
	 */
	@Column(name = "result", length = 40)
	public String getResult() {
		return result;
	}

	/**
	 * Menetapkan kode hasil peristiwa.
	 *
	 * @param result kode hasil
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * Waktu peristiwa terjadi. Kolom {@code NOT NULL} tanpa nilai default di lapisan Java,
	 * sehingga penulis wajib mengisinya; seluruh penulis saat ini mengisi waktu saat
	 * pencatatan. Perhatikan bahwa nilai ini adalah waktu PENCATATAN oleh penulis, yang untuk
	 * peristiwa asinkron dapat sedikit berbeda dari waktu kejadian sesungguhnya.
	 *
	 * @return waktu peristiwa
	 */
	@Column(name = "waktu", nullable = false)
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu;
	}

	/**
	 * Menetapkan waktu peristiwa. Wajib diisi karena kolom bersifat {@code NOT NULL}.
	 *
	 * @param waktu waktu peristiwa
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Nama pengguna yang membuat/mengubah baris (field audit shadow standar AIS). Untuk
	 * peristiwa yang lahir dari alur publik, nilainya adalah penanda sistem
	 * {@code "pendaftaran"} karena pada tahap itu belum ada pengguna terautentikasi; identitas
	 * aktor yang sebenarnya dicatat lewat {@link #getActorType()} beserta kolom id terkait.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna pembuat/pengubah. Setter sengaja MENGABAIKAN nilai
	 * {@code null} maupun string kosong/spasi — pola baku audit shadow AIS yang mencegah
	 * jejak pelaku yang sudah terisi tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Id pengguna yang membuat/mengubah baris (field audit shadow standar AIS).
	 *
	 * @return id pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna pembuat/pengubah. Sama seperti {@link #setOleh(String)}, nilai
	 * {@code null}/kosong diabaikan agar jejak pelaku tidak terhapus.
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel waktu perubahan terakhir, diisi otomatis oleh
	 * {@code AuditTimestampInterceptor.ubah} lewat callback {@code @PreUpdate}. Pada
	 * pemakaian normal baris audit tidak pernah diubah setelah disisipkan, sehingga nilai ini
	 * praktis sama dengan waktu pembuatan; perbedaan di antara keduanya justru merupakan
	 * sinyal bahwa baris audit pernah disentuh dan layak ditelusuri lewat riwayat Envers.
	 *
	 * @return waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel waktu perubahan terakhir. Umumnya tidak dipanggil kode aplikasi
	 * karena sudah ditangani interceptor.
	 *
	 * @param tanggal_dirubah waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
