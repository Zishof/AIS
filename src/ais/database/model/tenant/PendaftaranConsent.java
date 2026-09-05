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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Bukti persetujuan (consent) per dokumen per permohonan tenant.</h3>
 *
 * <p>Checkbox saja tidak cukup (§14.5 dokumen master): disimpan versi dokumen yang disetujui,
 * waktu, IP sumber, user-agent, locale, dan channel. Submit ditolak bila versi dokumen yang
 * dicentang ≠ versi terpublikasi saat submit.</p>
 *
 * <h3>Cara penegakan versi dokumen</h3>
 *
 * <p>Penolakan yang disebut di atas benar-benar dijalankan dan bersifat gagal-tertutup.
 * Formulir pendaftaran menerima versi dokumen yang dilihat pengguna sebagai bagian payload
 * ({@code termsVersion} dan {@code privacyVersion}), dan pada saat submit nilai tersebut
 * dibandingkan dengan versi terpublikasi yang dibaca dari Konfigurasi
 * ({@code pendaftaran_terms_version} dan {@code pendaftaran_privacy_version}, keduanya
 * berdefault {@code "2026-01"}). Bila salah satunya tidak cocok — atau kotak persetujuannya
 * tidak dicentang — submit ditolak dengan galat per-field dan pengguna diminta memuat ulang
 * halaman. Konstruksi ini menutup celah yang halus tetapi nyata: tanpa pemeriksaan versi,
 * pengguna yang membuka formulir sebelum dokumen berubah dan menekan kirim sesudahnya akan
 * tercatat menyetujui dokumen yang belum pernah ia baca. Karena versi yang tersimpan di
 * kolom {@link #getDocumentVersion()} adalah versi yang sudah lolos pencocokan itu, baris di
 * tabel ini merekam persetujuan atas naskah yang benar-benar ditampilkan.</p>
 *
 * <p>Persetujuan disimpan sebagai satu baris per dokumen, bukan satu kolom boolean per
 * dokumen pada tabel permohonan. Bentuk ini yang membuat riwayat persetujuan dapat tumbuh
 * (versi dokumen baru menghasilkan baris baru, bukan menimpa yang lama) dan membuat setiap
 * persetujuan membawa konteksnya sendiri: waktu, alamat IP, peramban, bahasa, dan kanal.
 * Ditambah anotasi {@link Audited}, baris persetujuan yang sudah tertulis tidak dapat diubah
 * atau dihapus tanpa meninggalkan jejak — properti yang justru menjadi inti nilai sebuah
 * bukti persetujuan.</p>
 *
 * <p>Dua hal perlu diketahui pembaca data. Pertama, persetujuan pemasaran bersifat opt-in
 * murni: barisnya hanya ditulis bila pengguna mencentangnya, sehingga TIDAK ADA baris
 * {@link #TYPE_MARKETING} berisi {@code accepted=false} untuk pengguna yang menolak.
 * Ketiadaan baris karena itu harus dibaca sebagai "tidak menyetujui", bukan sebagai data
 * yang hilang. Kedua, seluruh baris yang ditulis alur ini selalu bernilai
 * {@code accepted=true} dan berkanal {@code "WEB"}; kolom {@link #getAccepted()} tetap
 * disediakan agar pencabutan persetujuan kelak dapat direkam sebagai baris baru tanpa
 * mengubah skema.</p>
 *
 * @see PendaftaranTenant
 * @see PendaftaranAuditEvent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_consent")
public class PendaftaranConsent extends GeneralValueObject {

	/** Versi serialisasi Java standar untuk seluruh entity model AIS. */
	private static final long serialVersionUID = 1L;

	/** Dokumen Syarat &amp; Ketentuan; wajib disetujui, versinya dicocokkan saat submit. */
	public static final String TYPE_TERMS = "TERMS";
	/** Dokumen Kebijakan Privasi; wajib disetujui, versinya dicocokkan saat submit. */
	public static final String TYPE_PRIVACY = "PRIVACY";
	/**
	 * Persetujuan komunikasi pemasaran; bersifat opsional dan opt-in murni — barisnya hanya
	 * ditulis bila pengguna mencentangnya, sehingga ketiadaan baris berarti tidak setuju.
	 */
	public static final String TYPE_MARKETING = "MARKETING";

	/** Primary key surrogate, IDENTITY dari sequence PostgreSQL. */
	private Long id;
	/** Permohonan pendaftaran tenant yang persetujuannya direkam (wajib). */
	private PendaftaranTenant pendaftaranTenant;
	/** Jenis dokumen yang disetujui; salah satu konstanta {@code TYPE_*}. */
	private String consentType;
	/** Versi dokumen yang disetujui, sudah dicocokkan dengan versi terpublikasi. */
	private String documentVersion;
	/** Status persetujuan; praktis selalu {@code true} pada data saat ini. */
	private Boolean accepted;
	/** Waktu persetujuan diberikan. */
	private Date acceptedAt;
	/** Alamat IP sumber, disimpan MENTAH (dipotong 64 karakter) sebagai bukti. */
	private String sourceIp;
	/** String {@code User-Agent} peramban, dipotong 500 karakter. */
	private String userAgent;
	/** Kode bahasa antarmuka saat persetujuan diberikan, default {@code "id"}. */
	private String locale;
	/** Kanal pemberian persetujuan; alur saat ini selalu {@code "WEB"}. */
	private String channel;

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
	public PendaftaranConsent() {
	}

	/**
	 * Primary key baris persetujuan. Dibangkitkan database (IDENTITY) saat insert, sehingga
	 * bernilai {@code null} selama objek masih transient.
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
	 * Permohonan pendaftaran tenant yang menjadi induk persetujuan ini (kolom
	 * {@code pendaftaran_tenant_id}, {@code NOT NULL}). Relasi {@code LAZY}; getter
	 * melewatkan nilainya ke {@code check(...)} milik {@code GeneralValueObject} yang
	 * meng-unwrap proxy Hibernate dan mengembalikan {@code null} secara aman bila proxy sudah
	 * tidak dapat diinisialisasi. Perhatikan pola "getter destruktif" khas model AIS: hasil
	 * {@code check(...)} ditulis balik ke field, sehingga getter ini tidak bebas efek samping.
	 *
	 * @return permohonan induk, atau {@code null} bila proxy tak dapat diinisialisasi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = false)
	public PendaftaranTenant getPendaftaranTenant() {
		pendaftaranTenant = check(pendaftaranTenant);
		return pendaftaranTenant;
	}

	/**
	 * Menetapkan permohonan pendaftaran induk. Wajib diisi sebelum {@code save} karena kolom
	 * FK bersifat {@code NOT NULL}.
	 *
	 * @param pendaftaranTenant permohonan pendaftaran tenant terkait
	 */
	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	/**
	 * Jenis dokumen yang disetujui: {@link #TYPE_TERMS}, {@link #TYPE_PRIVACY}, atau
	 * {@link #TYPE_MARKETING}. Kolom {@code NOT NULL} tanpa constraint enum di database, dan
	 * tanpa unique constraint atas pasangan permohonan+jenis — dengan sengaja, karena satu
	 * permohonan dapat memiliki lebih dari satu baris untuk jenis yang sama ketika versi
	 * dokumen berubah atau persetujuan kelak dicabut dan diberikan lagi. Pembacaan data
	 * karena itu harus mengambil baris TERBARU per jenis, bukan berasumsi hanya ada satu.
	 *
	 * @return kode jenis dokumen, atau {@code null} bila belum diisi
	 */
	@Column(name = "consent_type", nullable = false, length = 40)
	public String getConsentType() {
		return consentType;
	}

	/**
	 * Menetapkan jenis dokumen yang disetujui. Gunakan konstanta {@code TYPE_*} kelas ini.
	 *
	 * @param consentType kode jenis dokumen
	 */
	public void setConsentType(String consentType) {
		this.consentType = consentType;
	}

	/**
	 * Versi dokumen yang disetujui, mis. {@code "2026-01"}. Kolom {@code NOT NULL}.
	 *
	 * <p>Field inilah yang mengubah persetujuan dari sekadar "pengguna pernah mencentang
	 * kotak" menjadi bukti yang dapat dipertanggungjawabkan, karena ia menjawab pertanyaan
	 * yang sebenarnya penting: naskah yang mana yang disetujui. Nilai yang tersimpan di sini
	 * bukan nilai yang dikirim klien begitu saja — ia sudah melewati pencocokan gagal-tertutup
	 * terhadap versi terpublikasi yang dibaca dari Konfigurasi
	 * ({@code pendaftaran_terms_version} / {@code pendaftaran_privacy_version}) pada saat
	 * submit, dan submit ditolak bila keduanya berbeda. Dengan demikian tidak mungkin ada
	 * baris yang merekam persetujuan atas versi yang tidak sedang terpublikasi saat itu.</p>
	 *
	 * <p>Konsekuensi operasional yang perlu diketahui administrator: karena versi dokumen
	 * dibaca dari Konfigurasi, menaikkan versi berarti seluruh formulir yang sedang terbuka
	 * di peramban pengguna menjadi basi dan submit-nya akan ditolak sampai halaman dimuat
	 * ulang. Perilaku itu memang disengaja dan merupakan sisi baik dari desain gagal-tertutup,
	 * tetapi sebaiknya diperhitungkan saat memilih waktu pembaruan dokumen. Perlu diingat
	 * pula karakter umum mekanisme Konfigurasi di AIS, yaitu bahwa pembacaan sebuah kunci
	 * yang belum ada akan menuliskan nilai defaultnya ke database — sehingga kunci versi
	 * dokumen sebaiknya diubah lewat jalur konfigurasi resmi, bukan diandalkan tetap kosong.</p>
	 *
	 * <p>Untuk {@link #TYPE_MARKETING} alur saat ini menuliskan versi {@code "1"} sebagai
	 * penanda sederhana, karena persetujuan pemasaran tidak terikat pada naskah dokumen
	 * bernomor sebagaimana Syarat &amp; Ketentuan maupun Kebijakan Privasi.</p>
	 *
	 * @return versi dokumen yang disetujui
	 */
	@Column(name = "document_version", nullable = false, length = 40)
	public String getDocumentVersion() {
		return documentVersion;
	}

	/**
	 * Menetapkan versi dokumen yang disetujui. Isi dengan versi yang SUDAH dicocokkan dengan
	 * versi terpublikasi, bukan nilai mentah dari klien.
	 *
	 * @param documentVersion versi dokumen
	 */
	public void setDocumentVersion(String documentVersion) {
		this.documentVersion = documentVersion;
	}

	/**
	 * Status persetujuan. Getter mem-default ke {@link Boolean#FALSE} bila field {@code null},
	 * yaitu arah default yang aman untuk sebuah bukti persetujuan: baris yang datanya tidak
	 * lengkap dibaca sebagai TIDAK menyetujui, bukan sebaliknya. Default ini hanya berlaku di
	 * lapisan Java; kolom database tetap menyimpan {@code NULL} sampai ada penulisan eksplisit.
	 *
	 * <p>Pada data yang dihasilkan alur saat ini, nilainya selalu {@code true} — persetujuan
	 * yang ditolak tidak menghasilkan baris sama sekali (untuk pemasaran) atau menggagalkan
	 * submit (untuk Syarat &amp; Ketentuan dan Kebijakan Privasi). Kolom ini tetap disediakan
	 * agar pencabutan persetujuan kelak dapat direkam sebagai baris baru bernilai
	 * {@code false} tanpa perlu mengubah skema maupun menimpa bukti lama.</p>
	 *
	 * @return {@code true} bila menyetujui, {@code false} bila tidak/belum diisi
	 */
	@Column(name = "accepted")
	public Boolean getAccepted() {
		return accepted == null ? Boolean.FALSE : accepted;
	}

	/**
	 * Menetapkan status persetujuan.
	 *
	 * @param accepted {@code true} bila menyetujui
	 */
	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}

	/**
	 * Waktu persetujuan diberikan. Diisi dengan stempel waktu submit yang sama untuk seluruh
	 * baris persetujuan pada satu permohonan, sehingga ketiga jenis dokumen dapat dikenali
	 * berasal dari satu tindakan pengiriman formulir yang sama.
	 *
	 * @return waktu persetujuan, atau {@code null} bila belum diisi
	 */
	@Column(name = "accepted_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getAcceptedAt() {
		return acceptedAt;
	}

	/**
	 * Menetapkan waktu persetujuan.
	 *
	 * @param acceptedAt waktu persetujuan
	 */
	public void setAcceptedAt(Date acceptedAt) {
		this.acceptedAt = acceptedAt;
	}

	/**
	 * Alamat IP sumber saat persetujuan diberikan, disimpan MENTAH (dirapikan dan dipotong ke
	 * 64 karakter agar muat IPv6).
	 *
	 * <p>Perhatikan asimetri yang disengaja terhadap {@link PendaftaranAuditEvent}, yang
	 * justru menyimpan alamat IP dalam bentuk SHA-256 dan bukan aslinya. Perbedaan itu
	 * mengikuti perbedaan tujuan kedua tabel. Audit event hanya perlu MENCOCOKKAN apakah
	 * beberapa peristiwa berasal dari sumber yang sama, sehingga hash sudah memadai dan
	 * paparannya lebih kecil. Bukti persetujuan sebaliknya perlu dapat DIBACA KEMBALI dan
	 * disajikan sebagai keterangan pendukung bila keabsahan persetujuan dipersoalkan — bentuk
	 * hash tidak berguna untuk keperluan itu. Karena kolom ini memuat data pribadi yang dapat
	 * dibaca langsung, aksesnya patut dibatasi setara dengan data pendaftar lainnya, dan
	 * nilainya wajib di-escape saat ditampilkan karena berasal dari payload permintaan.</p>
	 *
	 * @return alamat IP sumber, atau {@code null}/kosong bila tidak diketahui
	 */
	@Column(name = "source_ip", length = 64)
	public String getSourceIp() {
		return sourceIp;
	}

	/**
	 * Menetapkan alamat IP sumber. Pemanggil bertanggung jawab merapikan dan memotongnya ke
	 * batas 64 karakter.
	 *
	 * @param sourceIp alamat IP sumber
	 */
	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}

	/**
	 * String {@code User-Agent} peramban saat persetujuan diberikan, dirapikan dan dipotong ke
	 * 500 karakter oleh penulis. Melengkapi {@link #getSourceIp()} sebagai konteks teknis
	 * bukti persetujuan. Isinya sepenuhnya dikendalikan klien sehingga harus diperlakukan
	 * sebagai data tidak terpercaya dan wajib di-escape saat ditampilkan.
	 *
	 * @return string User-Agent, atau {@code null}/kosong bila tidak disertakan
	 */
	@Column(name = "user_agent", length = 500)
	public String getUserAgent() {
		return userAgent;
	}

	/**
	 * Menetapkan string {@code User-Agent}.
	 *
	 * @param userAgent string User-Agent
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	/**
	 * Kode bahasa antarmuka saat persetujuan diberikan (default {@code "id"}, dipotong 20
	 * karakter). Dicatat karena naskah dokumen dapat tersedia dalam beberapa bahasa: bersama
	 * {@link #getDocumentVersion()}, field ini menentukan naskah persis mana yang ditampilkan
	 * kepada pengguna.
	 *
	 * @return kode locale, atau {@code null} bila tidak diisi
	 */
	@Column(name = "locale", length = 20)
	public String getLocale() {
		return locale;
	}

	/**
	 * Menetapkan kode bahasa antarmuka.
	 *
	 * @param locale kode locale, mis. {@code "id"}
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * Kanal pemberian persetujuan. Alur pendaftaran mandiri saat ini selalu menuliskan
	 * {@code "WEB"}; kolom disediakan agar kanal lain (mis. aplikasi bergerak atau proses
	 * berbantuan petugas) dapat dibedakan tanpa mengubah skema. Berbeda dari
	 * {@link #getConsentType()}, tidak ada konstanta untuk nilai kanal di kelas ini.
	 *
	 * @return kode kanal, atau {@code null} bila tidak diisi
	 */
	@Column(name = "channel", length = 40)
	public String getChannel() {
		return channel;
	}

	/**
	 * Menetapkan kanal pemberian persetujuan.
	 *
	 * @param channel kode kanal, mis. {@code "WEB"}
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/**
	 * Nama pengguna yang membuat/mengubah baris (field audit shadow standar AIS). Untuk
	 * persetujuan yang lahir dari alur publik, nilainya adalah penanda sistem
	 * {@code "pendaftaran"} karena pada tahap itu belum ada pengguna terautentikasi.
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
	 * {@code AuditTimestampInterceptor.ubah} lewat callback {@code @PreUpdate}. Baris
	 * persetujuan pada pemakaian normal tidak pernah diubah setelah disisipkan, sehingga
	 * selisih antara nilai ini dan {@link #getAcceptedAt()} adalah sinyal bahwa bukti
	 * persetujuan pernah disentuh dan layak ditelusuri lewat riwayat Envers.
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
