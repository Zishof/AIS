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
 * <h3>Tantangan verifikasi email/OTP satu permohonan tenant.</h3>
 *
 * <p>HANYA hash token/OTP yang disimpan (SHA-256 hex) -- token mentah dikirim lewat email dan
 * tidak pernah masuk database/log/audit (invariant #8 ERD). Token baru meng-invalidate token
 * lama (status {@link #STATUS_SUPERSEDED}); expiry + attempt count + resend rate-limit
 * ditegakkan {@code EmailVerificationService}.</p>
 *
 * <h3>Verifikasi keamanan mekanisme token</h3>
 *
 * <p>Karena baris tabel ini adalah satu-satunya penjaga antara "seseorang mengklaim memiliki
 * sebuah alamat email" dan "sistem memprovisikan tenant baru atas nama alamat itu", kekuatan
 * tokennya diperiksa secara khusus. Hasilnya positif pada seluruh titik yang biasanya
 * bermasalah:</p>
 *
 * <ul>
 * <li><b>Sumber acak.</b> Token dibangkitkan {@code PasswordHashService.tokenAcakHex(32)}
 * yang memakai {@link java.security.SecureRandom} — CSPRNG, bukan {@code java.util.Random}
 * maupun {@code Math.random()} yang keluarannya dapat direkonstruksi dari beberapa sampel.</li>
 * <li><b>Panjang.</b> 32 byte acak (256 bit) yang dirender menjadi 64 karakter heksadesimal,
 * pas dengan panjang kolom {@code token_hash} sebesar 64. Ini bukan angka pendek berurutan
 * seperti pola OTP 4-6 digit yang lazim dijumpai di modul lama, sehingga penebakan acak
 * maupun enumerasi berurutan tidak dapat dilakukan.</li>
 * <li><b>Penyimpanan.</b> Yang masuk database hanya SHA-256 hex dari token; token mentah
 * hidup di memori hanya selama satu pemanggilan dan dikembalikan ke pemanggil semata-mata
 * untuk dirakit menjadi tautan email. Dump database karena itu tidak memberi penyerang token
 * yang dapat dipakai.</li>
 * <li><b>Kedaluwarsa.</b> {@link #getExpiresAt()} diisi saat pembuatan sebesar sekarang +
 * {@code EmailVerificationService.masaBerlakuJam()} (konfigurasi
 * {@code pendaftaran_verifikasi_jam}, default 48 jam) dan benar-benar ditegakkan saat
 * pencarian: token yang lewat masa berlaku ditandai {@link #STATUS_EXPIRED} lalu ditolak.</li>
 * <li><b>Sekali pakai &amp; supersede.</b> Pencarian hanya menerima status
 * {@link #STATUS_PENDING}; token yang sudah dipakai menjadi {@link #STATUS_CONSUMED}, dan
 * setiap penerbitan token baru lebih dulu menandai seluruh token PENDING milik permohonan
 * yang sama sebagai {@link #STATUS_SUPERSEDED} sehingga tidak pernah ada dua token aktif.</li>
 * <li><b>Pembatasan laju.</b> Ditegakkan di lapisan servlet publik lewat
 * {@code PublicRegistrationRateLimiter}: percobaan verifikasi dibatasi 20 per jam per IP,
 * sedangkan penerbitan ulang dibatasi 5 per jam per IP DAN 3 per jam per kode pendaftaran.
 * Batas per-kode itu penting karena mencegah penyerang membanjiri kotak surat calon korban
 * (email bombing) walau ia berganti-ganti alamat IP.</li>
 * </ul>
 *
 * <p>Dua field pada kelas ini perlu dicatat sebagai <b>tidur</b>, agar pembaca berikutnya
 * tidak keliru menyangka keduanya sudah menjadi kontrol aktif. Pertama, {@link #getOtpHash()}
 * tidak pernah ditulis oleh kode mana pun — kanal OTP disiapkan skemanya tetapi belum
 * diimplementasikan; satu-satunya kanal yang hidup adalah {@link #CHANNEL_EMAIL} berbasis
 * tautan. Kedua, {@link #getAttemptCount()} hanya pernah di-<i>set</i> ke {@code 0} pada saat
 * tantangan dibuat dan tidak pernah dinaikkan di titik verifikasi mana pun, sehingga tidak
 * ada penguncian per-token setelah sekian kali gagal. Untuk desain saat ini konsekuensinya
 * kecil: dengan token 256 bit, brute force tidak layak secara komputasi, dan pembatasan laju
 * per-IP sudah membatasi volume percobaan. Namun bila kanal OTP numerik kelak diaktifkan,
 * penghitung percobaan ini WAJIB dinaikkan dan ditegakkan — OTP pendek tanpa batas percobaan
 * per-token adalah kerentanan nyata, sedangkan token panjang tanpa penghitung tidak.</p>
 *
 * @see #getTokenHash()
 * @see #getAttemptCount()
 * @see PendaftaranTenant
 * @see PendaftaranAuditEvent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_email_verification")
public class PendaftaranEmailVerification extends GeneralValueObject {

	/** Versi serialisasi Java standar untuk seluruh entity model AIS. */
	private static final long serialVersionUID = 1L;

	/**
	 * Kanal verifikasi lewat email berbasis tautan bertoken. Satu-satunya kanal yang
	 * benar-benar diimplementasikan, sekaligus nilai default {@link #getChannel()}.
	 */
	public static final String CHANNEL_EMAIL = "EMAIL";

	/**
	 * Tantangan aktif dan menunggu diverifikasi. Hanya baris berstatus inilah yang dapat
	 * dicocokkan dengan token yang dikirim pendaftar.
	 */
	public static final String STATUS_PENDING = "PENDING";
	/** Tantangan sudah berhasil dipakai; token tidak bisa dipakai kedua kali. */
	public static final String STATUS_CONSUMED = "CONSUMED";
	/** Tantangan lewat {@link #getExpiresAt()}; ditandai saat pencarian menemukannya. */
	public static final String STATUS_EXPIRED = "EXPIRED";
	/**
	 * Tantangan digantikan token yang lebih baru pada permohonan yang sama (penerbitan ulang).
	 * Menjamin hanya ada satu token aktif per permohonan pada satu waktu.
	 */
	public static final String STATUS_SUPERSEDED = "SUPERSEDED";

	/** Primary key surrogate, IDENTITY dari sequence PostgreSQL. */
	private Long id;
	/** Permohonan pendaftaran tenant yang diverifikasi (wajib). */
	private PendaftaranTenant pendaftaranTenant;
	/** Kanal tantangan; praktis selalu {@link #CHANNEL_EMAIL}. */
	private String channel;
	/** Alamat email tujuan ter-normalisasi (bukan rahasia; dipakai audit resend). */
	private String destinationNormalized;
	/** SHA-256 hex dari token mentah — token mentah TIDAK pernah disimpan. */
	private String tokenHash;
	/** SHA-256 hex OTP; field TIDUR, belum pernah ditulis kode mana pun. */
	private String otpHash;
	/** Status siklus hidup tantangan (PENDING/CONSUMED/EXPIRED/SUPERSEDED). */
	private String status;
	/** Penghitung percobaan; field TIDUR, hanya diinisialisasi 0 dan tak pernah dinaikkan. */
	private Integer attemptCount;
	/** Waktu email verifikasi berhasil dikirim. */
	private Date sentAt;
	/** Batas akhir keberlakuan token. */
	private Date expiresAt;
	/** Waktu token berhasil dipakai. */
	private Date consumedAt;
	/** Waktu baris tantangan dibuat. */
	private Date createdAt;

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
	public PendaftaranEmailVerification() {
	}

	/**
	 * Primary key tantangan verifikasi. Dibangkitkan database (IDENTITY) saat insert,
	 * sehingga bernilai {@code null} selama objek masih transient.
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
	 * Permohonan pendaftaran tenant yang sedang diverifikasi (kolom
	 * {@code pendaftaran_tenant_id}, {@code NOT NULL}). Relasi {@code LAZY}; getter
	 * melewatkan nilainya ke {@code check(...)} milik {@code GeneralValueObject} yang
	 * meng-unwrap proxy Hibernate dan mengembalikan {@code null} secara aman bila proxy
	 * sudah tidak dapat diinisialisasi. Perhatikan pola "getter destruktif" khas model AIS:
	 * hasil {@code check(...)} ditulis balik ke field, sehingga getter ini tidak bebas efek
	 * samping.
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
	 * Menetapkan permohonan pendaftaran yang diverifikasi. Wajib diisi sebelum {@code save}
	 * karena kolom FK bersifat {@code NOT NULL}.
	 *
	 * @param pendaftaranTenant permohonan pendaftaran tenant terkait
	 */
	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	/**
	 * Kanal pengiriman tantangan. Getter mem-default ke {@link #CHANNEL_EMAIL} bila field
	 * {@code null} atau berisi spasi saja, sehingga baris lama tetap terbaca konsisten;
	 * default ini hanya berlaku di lapisan Java, kolom database tetap menyimpan nilai
	 * aslinya. Saat ini seluruh penulis mengisi {@link #CHANNEL_EMAIL} secara eksplisit.
	 *
	 * @return kode kanal, tidak pernah {@code null}
	 */
	@Column(name = "channel", nullable = false, length = 20)
	public String getChannel() {
		return channel == null || channel.trim().isEmpty() ? CHANNEL_EMAIL : channel;
	}

	/**
	 * Menetapkan kanal tantangan.
	 *
	 * @param channel kode kanal, umumnya {@link #CHANNEL_EMAIL}
	 */
	public void setChannel(String channel) {
		this.channel = channel;
	}

	/**
	 * Email tujuan ter-normalisasi (utk audit resend/rate-limit; bukan rahasia).
	 *
	 * <p>Disimpan apa adanya (bukan hash) karena memang harus dapat dibaca kembali: nilai
	 * inilah yang dipakai untuk menjawab pertanyaan dukungan pelanggan "tautan verifikasi
	 * dikirim ke alamat mana", serta untuk menelusuri penyalahgunaan penerbitan ulang.
	 * Bandingkan dengan {@code sourceIpHash} pada {@link PendaftaranAuditEvent} yang justru
	 * di-hash — IP tidak perlu dibaca kembali, alamat tujuan perlu.</p>
	 *
	 * @return alamat email tujuan ter-normalisasi
	 */
	@Column(name = "destination_normalized", nullable = false, length = 255)
	public String getDestinationNormalized() {
		return destinationNormalized;
	}

	/**
	 * Menetapkan alamat email tujuan yang sudah dinormalisasi (huruf kecil, tanpa spasi
	 * pinggir). Wajib diisi karena kolom bersifat {@code NOT NULL}.
	 *
	 * @param destinationNormalized alamat email tujuan ter-normalisasi
	 */
	public void setDestinationNormalized(String destinationNormalized) {
		this.destinationNormalized = destinationNormalized;
	}

	/**
	 * Hash SHA-256 heksadesimal dari token verifikasi — <b>bukan</b> tokennya sendiri.
	 *
	 * <p>Field ini adalah inti keamanan seluruh alur pendaftaran mandiri, karena penguasaan
	 * token yang cocok dengannya setara dengan pembuktian bahwa pendaftar benar-benar
	 * mengendalikan alamat email yang didaftarkan, yang pada gilirannya membuka jalan ke
	 * provisioning tenant. Rantai penanganannya dirancang agar rahasia tidak pernah
	 * mengendap: token dibangkitkan sebagai 32 byte dari {@link java.security.SecureRandom},
	 * dirender menjadi 64 karakter heksadesimal, langsung di-hash SHA-256, dan hanya
	 * hash itulah yang disimpan ke kolom ini (panjang kolom 64, tepat sepanjang SHA-256 hex).
	 * Token mentahnya dikembalikan ke pemanggil hanya untuk dirakit menjadi tautan
	 * {@code /pendaftaran?mode=verifikasi&token=...} pada badan email, lalu dilepas — ia
	 * tidak pernah ditulis ke database, ke {@code error_log}, maupun ke
	 * {@link PendaftaranAuditEvent} (invariant #8 ERD).</p>
	 *
	 * <p>Pemilihan 256 bit entropi bukan sekadar formalitas. Karena kolom ini dicari langsung
	 * berdasarkan kecocokan hash tanpa dipersempit lebih dulu oleh id permohonan, token
	 * berperan sebagai <i>bearer credential</i> global: siapa pun yang memegang token yang
	 * benar akan menemukan barisnya, tanpa perlu tahu kode pendaftaran atau identitas
	 * pemiliknya. Desain semacam itu hanya aman bila ruang tokennya cukup besar sehingga
	 * penebakan mustahil, dan 256 bit dari CSPRNG memenuhi syarat tersebut dengan margin
	 * sangat lebar. Perbandingannya menjadi tajam bila diletakkan berdampingan dengan pola
	 * yang berulang kali ditemukan di modul-modul lama AIS, yaitu kode verifikasi numerik
	 * pendek atau nilai berbasis penghitung/waktu yang dapat ditebak maupun dienumerasi;
	 * pada pola lama itu, ketiadaan batas percobaan langsung berubah menjadi kerentanan yang
	 * dapat dieksploitasi, sedangkan di sini tidak.</p>
	 *
	 * <p>Penyimpanan dalam bentuk hash memberi properti kedua yang sama pentingnya: pihak
	 * yang memperoleh isi database — administrator basis data, pemegang berkas cadangan,
	 * atau penyerang yang berhasil membaca tabel lewat celah di modul lain — tetap tidak
	 * dapat menyelesaikan verifikasi milik orang lain, karena yang ia peroleh hanyalah
	 * hash dan membalikkannya berarti membalikkan SHA-256 atas masukan 256 bit acak. Ini
	 * membedakan modul ini dari desain naif yang menyimpan token verifikasi apa adanya
	 * "supaya admin bisa membantu pengguna" — kemudahan yang selalu dibayar dengan
	 * berubahnya seluruh isi tabel menjadi kumpulan kredensial siap pakai.</p>
	 *
	 * <p>Sifat sekali-pakai ditegakkan lewat kombinasi status dan supersede, bukan lewat
	 * penghapusan baris: pencarian hanya menerima {@link #STATUS_PENDING}; token yang lewat
	 * {@link #getExpiresAt()} ditandai {@link #STATUS_EXPIRED} saat itu juga dan ditolak;
	 * token yang berhasil dipakai menjadi {@link #STATUS_CONSUMED}; dan penerbitan token baru
	 * lebih dulu menandai semua token PENDING milik permohonan yang sama sebagai
	 * {@link #STATUS_SUPERSEDED}. Karena baris tidak dihapus dan kelas ini ber-{@code @Audited},
	 * riwayat lengkap penerbitan dan pemakaian token tetap dapat diperiksa tanpa satu pun
	 * token yang dapat dipakai ulang.</p>
	 *
	 * <p>Satu catatan tersisa untuk pengembang berikutnya: nilai hash di sini dihitung dengan
	 * SHA-256 polos tanpa salt maupun peregangan kunci, dan perbandingannya dilakukan sebagai
	 * kecocokan kueri database, bukan perbandingan constant-time seperti pada verifikasi
	 * password. Untuk token acak 256 bit kedua hal itu tepat dan tidak menimbulkan masalah —
	 * salt tidak diperlukan karena masukannya sudah acak penuh dan tidak dapat dikamuskan,
	 * dan kebocoran waktu tidak berguna bagi penyerang yang tetap harus menebak 256 bit.
	 * Namun bila kelak kolom ini dipakai menampung nilai berentropi rendah (mis. OTP numerik
	 * lewat {@link #getOtpHash()}), kedua asumsi tersebut runtuh sekaligus dan mekanismenya
	 * harus dirancang ulang, lengkap dengan penegakan {@link #getAttemptCount()}.</p>
	 *
	 * @return SHA-256 hex token verifikasi, atau {@code null} bila tantangan tanpa token
	 */
	@Column(name = "token_hash", length = 64)
	public String getTokenHash() {
		return tokenHash;
	}

	/**
	 * Menetapkan hash token. Pemanggil WAJIB mengisi hash SHA-256 hex dari token, bukan
	 * token mentahnya — menuliskan token mentah ke sini akan meruntuhkan invariant #8 ERD.
	 *
	 * @param tokenHash SHA-256 hex dari token verifikasi
	 */
	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	/**
	 * Hash SHA-256 heksadesimal dari OTP, untuk kanal verifikasi berbasis kode sekali pakai.
	 *
	 * <p><b>Field TIDUR.</b> Penelusuran seluruh pohon sumber menunjukkan tidak ada satu pun
	 * pemanggil {@code setOtpHash(...)} di luar kelas ini: kanal OTP baru disiapkan
	 * skemanya mengikuti ERD, sedangkan alur yang berjalan sepenuhnya memakai tautan
	 * bertoken lewat {@link #getTokenHash()}. Kolom ini karena itu selalu {@code null} pada
	 * data produksi saat ini.</p>
	 *
	 * <p>Peringatan untuk implementasi berikutnya: OTP umumnya pendek (4-8 digit), yang
	 * berarti ruang tebakannya kecil dan seluruh argumen keamanan yang berlaku bagi token
	 * 256 bit TIDAK berlaku baginya. Bila kanal ini diaktifkan, tiga kontrol menjadi wajib
	 * dan tidak boleh diserahkan pada pembatasan laju per-IP saja: penegakan
	 * {@link #getAttemptCount()} dengan penguncian tantangan setelah sejumlah kecil kegagalan,
	 * masa berlaku yang jauh lebih pendek daripada 48 jam bawaan kanal email, dan perbandingan
	 * yang tidak membocorkan informasi lewat waktu eksekusi.</p>
	 *
	 * @return SHA-256 hex OTP, praktis selalu {@code null} pada implementasi saat ini
	 */
	@Column(name = "otp_hash", length = 64)
	public String getOtpHash() {
		return otpHash;
	}

	/**
	 * Menetapkan hash OTP. Belum dipanggil kode mana pun; lihat catatan pada
	 * {@link #getOtpHash()} sebelum mengaktifkan kanal OTP.
	 *
	 * @param otpHash SHA-256 hex dari OTP
	 */
	public void setOtpHash(String otpHash) {
		this.otpHash = otpHash;
	}

	/**
	 * Status siklus hidup tantangan: {@link #STATUS_PENDING}, {@link #STATUS_CONSUMED},
	 * {@link #STATUS_EXPIRED}, atau {@link #STATUS_SUPERSEDED}. Getter mem-default ke
	 * {@link #STATUS_PENDING} bila field {@code null}/kosong.
	 *
	 * <p>Status inilah yang menegakkan sifat sekali-pakai: pencarian token hanya menerima
	 * baris {@link #STATUS_PENDING}, sehingga transisi ke tiga status lainnya secara efektif
	 * mematikan token tanpa perlu menghapus barisnya. Perhatikan bahwa default PENDING
	 * bersifat "terbuka" — baris yang statusnya gagal terisi akan dianggap aktif, bukan
	 * mati. Untuk alur ini konsekuensinya tidak berbahaya karena baris hanya dapat dicocokkan
	 * bila penyerang sudah memegang token yang benar, tetapi perlu diingat bila logika status
	 * kelak diperluas.</p>
	 *
	 * @return kode status, tidak pernah {@code null}
	 */
	@Column(name = "status", nullable = false, length = 40)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PENDING : status;
	}

	/**
	 * Menetapkan status siklus hidup tantangan. Gunakan konstanta {@code STATUS_*} kelas ini.
	 *
	 * @param status kode status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Jumlah percobaan verifikasi yang tercatat untuk tantangan ini. Getter mem-default ke
	 * {@code 0} bila field {@code null}.
	 *
	 * <p><b>Penghitung TIDUR — tidak menjadi kontrol keamanan aktif.</b> Penelusuran seluruh
	 * pemanggil menunjukkan {@code setAttemptCount(...)} hanya dipanggil satu kali, yaitu
	 * saat tantangan dibuat, dengan nilai {@code 0}; tidak ada satu pun titik verifikasi yang
	 * menaikkannya, dan karenanya tidak ada logika mana pun yang membandingkannya dengan
	 * suatu batas maksimum. Akibatnya tidak ada penguncian per-token setelah sekian kali
	 * percobaan gagal, meskipun kolomnya tersedia.</p>
	 *
	 * <p>Hal ini sengaja dicatat sebagai temuan yang <i>tidak</i> dinaikkan menjadi
	 * kerentanan, dan alasannya perlu dipahami agar tidak salah dinilai di kemudian hari.
	 * Batas percobaan per-token berfungsi melindungi rahasia berentropi rendah — kode
	 * numerik pendek, PIN, jawaban pertanyaan keamanan — yang ruang tebakannya cukup kecil
	 * untuk dihabiskan dengan percobaan berulang. Rahasia yang dijaga di sini bukan jenis itu:
	 * token 256 bit dari CSPRNG berada jauh di luar jangkauan penebakan, sehingga penghitung
	 * percobaan tidak menambah perlindungan berarti. Lapisan yang benar-benar membatasi
	 * volume percobaan pun sudah ada di depannya, yaitu pembatasan laju pada servlet publik
	 * sebesar 20 percobaan verifikasi per jam per IP, ditambah 5 penerbitan ulang per jam per
	 * IP dan 3 per jam per kode pendaftaran.</p>
	 *
	 * <p>Yang tetap perlu diwaspadai adalah perubahan asumsi. Begitu {@link #getOtpHash()}
	 * diaktifkan, rahasia yang diverifikasi berubah menjadi berentropi rendah dan penghitung
	 * ini seketika berubah dari "kolom dekoratif" menjadi kontrol yang wajib ditegakkan —
	 * dinaikkan pada setiap kegagalan, dan dipakai mengunci tantangan setelah sedikit
	 * percobaan. Pembatasan per-IP saja tidak memadai untuk kasus itu, karena penyerang dapat
	 * menyebarkan tebakan lewat banyak alamat IP sementara ruang tebakan OTP tetap kecil.</p>
	 *
	 * @return jumlah percobaan tercatat, praktis selalu {@code 0} pada implementasi saat ini
	 */
	@Column(name = "attempt_count")
	public Integer getAttemptCount() {
		return attemptCount == null ? Integer.valueOf(0) : attemptCount;
	}

	/**
	 * Menetapkan jumlah percobaan verifikasi. Saat ini hanya dipanggil sekali dengan nilai
	 * {@code 0} pada pembuatan tantangan; lihat catatan pada {@link #getAttemptCount()}.
	 *
	 * @param attemptCount jumlah percobaan
	 */
	public void setAttemptCount(Integer attemptCount) {
		this.attemptCount = attemptCount;
	}

	/**
	 * Waktu email verifikasi berhasil dikirim. Diisi dalam transaksi kecil terpisah setelah
	 * pengiriman berhasil; bila pengiriman gagal, field ini tetap {@code null} sementara
	 * barisnya tetap ada — kegagalan kirim sengaja TIDAK menggagalkan pendaftaran, sehingga
	 * token tetap tersimpan dan dapat dikirim ulang atau diverifikasi manual oleh admin.
	 *
	 * @return waktu pengiriman, atau {@code null} bila belum/gagal terkirim
	 */
	@Column(name = "sent_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getSentAt() {
		return sentAt;
	}

	/**
	 * Menetapkan waktu pengiriman email verifikasi.
	 *
	 * @param sentAt waktu pengiriman
	 */
	public void setSentAt(Date sentAt) {
		this.sentAt = sentAt;
	}

	/**
	 * Batas akhir keberlakuan token. Diisi saat pembuatan sebesar waktu sekarang ditambah
	 * {@code EmailVerificationService.masaBerlakuJam()} — konfigurasi
	 * {@code pendaftaran_verifikasi_jam} dengan default 48 jam.
	 *
	 * <p>Batas ini benar-benar ditegakkan, bukan sekadar dicatat: pencarian token memeriksa
	 * nilai ini dan, bila sudah lewat, menandai baris {@link #STATUS_EXPIRED} lalu menolak
	 * verifikasi. Penandaan dilakukan secara malas (saat baris kebetulan diakses), sehingga
	 * baris kedaluwarsa yang tidak pernah dicoba lagi bisa tetap berstatus PENDING di
	 * database — hal ini tidak membuka celah karena pemeriksaan waktu tetap dijalankan pada
	 * setiap pencarian, tetapi perlu diketahui saat membaca data mentah atau menyusun
	 * laporan berdasarkan kolom status saja.</p>
	 *
	 * <p>Perhatikan pula bahwa getter tidak memberi default: token dengan {@code expiresAt}
	 * bernilai {@code null} akan lolos pemeriksaan kedaluwarsa, alias berlaku selamanya.
	 * Seluruh penulis saat ini selalu mengisinya, namun penulis baru wajib mempertahankan
	 * kebiasaan itu.</p>
	 *
	 * @return batas akhir keberlakuan, atau {@code null} bila tanpa batas waktu
	 */
	@Column(name = "expires_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getExpiresAt() {
		return expiresAt;
	}

	/**
	 * Menetapkan batas akhir keberlakuan token. Wajib diisi agar token tidak berlaku selamanya.
	 *
	 * @param expiresAt batas akhir keberlakuan
	 */
	public void setExpiresAt(Date expiresAt) {
		this.expiresAt = expiresAt;
	}

	/**
	 * Waktu token berhasil dipakai untuk verifikasi, mendampingi transisi status ke
	 * {@link #STATUS_CONSUMED}.
	 *
	 * @return waktu pemakaian token, atau {@code null} bila belum pernah dipakai
	 */
	@Column(name = "consumed_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getConsumedAt() {
		return consumedAt;
	}

	/**
	 * Menetapkan waktu pemakaian token.
	 *
	 * @param consumedAt waktu pemakaian
	 */
	public void setConsumedAt(Date consumedAt) {
		this.consumedAt = consumedAt;
	}

	/**
	 * Waktu baris tantangan dibuat. Bersama {@link #getSentAt()} memungkinkan pembedaan
	 * antara "tantangan dibuat tetapi email gagal terkirim" dan "email terkirim".
	 *
	 * @return waktu pembuatan baris, atau {@code null} bila belum diisi
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan waktu pembuatan baris tantangan.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nama pengguna yang membuat/mengubah baris (field audit shadow standar AIS). Untuk
	 * tantangan yang dibuat alur publik, nilainya adalah penanda sistem {@code "pendaftaran"}
	 * karena belum ada pengguna terautentikasi pada tahap tersebut.
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
	 * {@code AuditTimestampInterceptor.ubah} lewat callback {@code @PreUpdate}.
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
