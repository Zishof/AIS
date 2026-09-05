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
 * <h3>Bukti penyerahan kredensial generated (TANPA menyimpan plaintext apa pun).</h3>
 *
 * <p>Mencatat KAPAN dan lewat KANAL apa kredensial generated diserahkan/diakui pemiliknya
 * (invariant #12 ERD: hanya ditampilkan sekali; tidak pernah bisa dilihat ulang). Baris di
 * sini adalah bukti audit, bukan tempat penyimpanan kredensial.</p>
 *
 * <h3>Verifikasi keamanan: entity ini TIDAK menyimpan password dalam bentuk apa pun</h3>
 *
 * <p>Karena namanya ("credential delivery") entity ini adalah kandidat kuat untuk pola
 * kerentanan "password plaintext" yang berulang kali ditemukan di modul-modul lama AIS
 * (mis. helper reset password guru/siswa, action penyedia asset, action guru/mahasiswa) di
 * mana password mentah disimpan di kolom database dan/atau dikirim apa adanya lewat badan
 * email HTML. Pemeriksaan menyeluruh atas kelas ini menyimpulkan bahwa pola tersebut
 * <b>TIDAK</b> terulang di sini. Seluruh field yang dideklarasikan hanyalah: {@link #getId()},
 * {@link #getPendaftaranTenant()}, {@link #getDeliveryChannel()}, {@link #getDeliveredAt()},
 * {@link #getAcknowledgedAt()}, {@link #getCredentialVersion()}, {@link #getCreatedAt()},
 * ditambah trio audit shadow {@code oleh}/{@code olehId}/{@code tanggal_dirubah}. Tidak ada
 * field bernama {@code password}, {@code plainPassword}, {@code temporaryPassword},
 * {@code generatedPassword}, {@code initialPassword}, bahkan tidak ada {@code passwordHash}
 * ataupun {@code tokenHash}. Yang disimpan semata-mata METADATA penyerahan: kanal apa, kapan
 * diserahkan, kapan diakui penerima, dan generasi keberapa. Rahasianya sendiri tidak pernah
 * menyentuh tabel ini.</p>
 *
 * <p>Desain ini adalah penerapan langsung invariant #12 ERD: kredensial generated bersifat
 * <i>show-once</i>. Nilai mentahnya hidup hanya selama satu respons HTTP (kanal
 * {@link #CHANNEL_SCREEN_ONCE}) atau selama satu pesan email keluar (kanal
 * {@link #CHANNEL_EMAIL}), lalu dibuang dari memori tanpa pernah dipersistensikan. Ketika
 * pemilik kehilangan kredensialnya, sistem tidak punya jalan untuk "menampilkan ulang" —
 * satu-satunya jalur pemulihan adalah menerbitkan kredensial BARU, yang menaikkan
 * {@link #getCredentialVersion()} dan membuat baris bukti baru. Sifat "tidak bisa dibaca
 * ulang" itulah yang membedakan modul ini dari modul-modul lama yang bermasalah: pada modul
 * lama, siapa pun yang bisa membaca satu baris tabel (DBA, operator backup, pemegang dump,
 * penyerang yang berhasil SQL injection di tempat lain) otomatis memegang password aktif
 * milik pengguna; di sini, baris tabel yang bocor hanya membocorkan fakta bahwa penyerahan
 * pernah terjadi pada waktu tertentu.</p>
 *
 * <p>Konsekuensi penting kedua adalah pada anotasi {@link Audited} (Hibernate Envers) di
 * kelas ini. Envers menyalin setiap perubahan baris ke tabel bayangan {@code _aud}, sehingga
 * kolom apa pun yang ditaruh di entity ber-{@code @Audited} otomatis tereplikasi dan
 * tersimpan selamanya dalam riwayat — retensi yang jauh lebih panjang daripada baris
 * aslinya. Seandainya entity ini memuat kolom password (walau "sementara"), Envers akan
 * mengabadikannya, dan penghapusan baris utama pun tidak akan menghapus jejaknya. Karena
 * entity ini hanya memuat metadata, {@code @Audited} justru menjadi nilai tambah: riwayat
 * penyerahan kredensial tidak bisa dihapus diam-diam oleh siapa pun yang punya akses tulis
 * ke tabel utama.</p>
 *
 * <h3>Status pemakaian: entity TIDUR (belum punya pemanggil produksi)</h3>
 *
 * <p>Penelusuran seluruh pohon sumber ({@code ais/**}) menemukan bahwa nama kelas
 * {@code RegistrationCredentialDelivery} <b>tidak dirujuk dari satu pun kelas lain</b> —
 * tidak ada {@code new RegistrationCredentialDelivery()}, tidak ada {@code createCriteria}
 * atas kelas ini, dan tidak ada service yang menuliskannya. Rujukan yang ada hanyalah
 * pendaftaran mapping di {@code src/hibernate.cfg.xml}. Dengan kata lain ini adalah "entity
 * tidur": tabel dan mapping-nya sudah disiapkan lengkap mengikuti ERD, tetapi alur yang
 * mengisinya belum diaktifkan. Ini pola yang sudah dikenal di paket-paket modern AIS —
 * skema didefinisikan lebih dulu (mengikuti dokumen master), implementasi menyusul.</p>
 *
 * <p>Perlu ditegaskan bagaimana alur pendaftaran mandiri BEKERJA SAAT INI, tanpa entity ini.
 * Pendaftar memilih sendiri password-nya pada form pendaftaran publik (field {@code password}
 * dan {@code konfirmasiPassword} pada payload submit); password itu langsung di-hash oleh
 * {@code PasswordHashService.hash(String)} menjadi pasangan hash+salt PBKDF2-HMAC-SHA256
 * (120.000 iterasi, salt 32 byte dari {@code SecureRandom}) dan yang disimpan ke
 * {@code Pendaftar} hanyalah {@code passwordHash} dan {@code passwordSalt}. Metadata
 * algoritmanya (nama algoritma, versi, jumlah iterasi) disimpan terpisah di
 * {@code PendaftarTenantProfile}. Artinya sistem <b>tidak pernah men-generate password untuk
 * pendaftar dan tidak pernah mengirim password lewat email</b>; email yang dikirim pada alur
 * ini hanyalah email verifikasi berisi tautan bertoken (lihat
 * {@link PendaftaranEmailVerification}), bukan kredensial. Langkah provisioning
 * {@code VERIFY_LOGIN} hanya MEMERIKSA bahwa {@code passwordHash} milik pemilik sudah
 * terisi, bukan membuat atau mengirimkan password.</p>
 *
 * <p>Kesimpulan audit: modul ini setara baiknya dengan pola positif yang sebelumnya
 * dikonfirmasi pada paket {@code sosial} (mis. redaksi rahasia di {@code SocialCallbackService}),
 * dan merupakan <i>counter-example</i> eksplisit terhadap pola password plaintext yang
 * mendominasi modul-modul lama. Tidak ada temuan kerentanan pada kelas ini. Satu-satunya
 * catatan untuk pengembang berikutnya bersifat preventif: bila kelak alur penyerahan
 * kredensial benar-benar diimplementasikan, entity ini harus tetap dipertahankan sebagai
 * penyimpan METADATA saja — menambahkan kolom untuk menampung nilai kredensial (walau
 * dienkripsi reversibel) akan meruntuhkan invariant #12 sekaligus mengaktifkan kembali
 * masalah replikasi Envers yang dijelaskan di atas.</p>
 *
 * @see PendaftaranTenant
 * @see PendaftaranEmailVerification
 * @see PendaftaranAuditEvent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "registration_credential_delivery")
public class RegistrationCredentialDelivery extends GeneralValueObject {

	/** Versi serialisasi Java standar untuk seluruh entity model AIS. */
	private static final long serialVersionUID = 1L;

	/**
	 * Kanal penyerahan "tampil sekali di layar": kredensial dirender satu kali pada respons
	 * HTTP penyelesaian pendaftaran dan tidak pernah bisa dibuka ulang. Kanal yang paling
	 * aman dari dua konstanta di kelas ini karena rahasianya tidak pernah meninggalkan
	 * sesi TLS pendaftar dan tidak singgah di kotak surat mana pun.
	 */
	public static final String CHANNEL_SCREEN_ONCE = "SCREEN_ONCE";

	/**
	 * Kanal penyerahan lewat email. Nilai konstanta ini menandai bahwa penyerahan dilakukan
	 * lewat surat elektronik; ia TIDAK menyiratkan bahwa isi kredensial ditulis di badan
	 * email — implementasi yang benar mengirim tautan aktivasi/reset bertoken sekali pakai,
	 * bukan password mentah (bandingkan {@link PendaftaranEmailVerification} yang memakai
	 * token 32 byte {@code SecureRandom} dan hanya menyimpan hash-nya).
	 */
	public static final String CHANNEL_EMAIL = "EMAIL";

	/** Primary key surrogate, IDENTITY dari sequence PostgreSQL. */
	private Long id;
	/** Permohonan pendaftaran tenant yang kredensialnya diserahkan (wajib). */
	private PendaftaranTenant pendaftaranTenant;
	/** Kanal penyerahan: {@link #CHANNEL_SCREEN_ONCE} atau {@link #CHANNEL_EMAIL}. */
	private String deliveryChannel;
	/** Waktu kredensial diserahkan oleh sistem. */
	private Date deliveredAt;
	/** Waktu penerima mengonfirmasi telah menerima/menyimpan kredensial. */
	private Date acknowledgedAt;
	/** Generasi kredensial (1 = penerbitan pertama, naik pada setiap penerbitan ulang). */
	private Integer credentialVersion;
	/** Waktu baris bukti ini dibuat. */
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
	public RegistrationCredentialDelivery() {
	}

	/**
	 * Primary key baris bukti penyerahan. Dibangkitkan database (IDENTITY) saat insert,
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
	 * Permohonan pendaftaran tenant yang menjadi induk bukti penyerahan ini (kolom
	 * {@code pendaftaran_tenant_id}, {@code NOT NULL}). Relasi {@code LAZY}, sehingga
	 * pemanggilan di luar sesi Hibernate yang masih terbuka berpotensi
	 * {@code LazyInitializationException} — karena itu getter melewatkan nilainya ke
	 * {@code check(...)} milik {@code GeneralValueObject}, helper standar AIS yang
	 * meng-unwrap/menyegarkan proxy Hibernate dan mengembalikan {@code null} secara aman
	 * bila proxy sudah tidak dapat diinisialisasi. Perhatikan bahwa helper ini melakukan
	 * penulisan balik ke field ({@code pendaftaranTenant = check(pendaftaranTenant)}) —
	 * pola "getter destruktif" yang lazim di model AIS: getter tidak sepenuhnya bebas efek
	 * samping, ia dapat mengganti isi field dengan objek hasil unwrap.
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
	 * Kanal yang dipakai menyerahkan kredensial: {@link #CHANNEL_SCREEN_ONCE} (ditampilkan
	 * sekali di layar) atau {@link #CHANNEL_EMAIL} (dikirim lewat surat elektronik). Kolom
	 * {@code NOT NULL} sepanjang 40 karakter, disimpan sebagai teks bebas tanpa constraint
	 * enum di sisi database, sehingga penulisnya kelak wajib memakai konstanta kelas ini
	 * agar nilai tetap konsisten.
	 *
	 * <p>Nilai kanal adalah satu-satunya petunjuk teknis mengenai seberapa besar paparan
	 * rahasia pada satu penerbitan, dan karenanya menjadi field paling relevan-keamanan di
	 * kelas ini. Pada {@link #CHANNEL_SCREEN_ONCE} rahasia hanya melintasi kanal TLS antara
	 * server dan peramban pendaftar, tidak diarsipkan di mana pun, dan hilang begitu halaman
	 * ditutup; jejak yang tersisa hanyalah baris metadata ini. Pada {@link #CHANNEL_EMAIL}
	 * paparannya secara inheren lebih luas: pesan melewati MTA pengirim, mungkin relay
	 * perantara, lalu mengendap di kotak surat penerima yang bisa dibuka ulang kapan saja,
	 * disinkronkan ke banyak perangkat, dan ikut terbawa cadangan. Itulah sebabnya praktik
	 * yang benar untuk kanal email adalah mengirim TAUTAN bertoken sekali pakai dengan masa
	 * berlaku pendek — bukan password mentah — persis seperti yang sudah dilakukan alur
	 * verifikasi email pada paket ini, yang mengirim tautan berisi token 32 byte hasil
	 * {@code SecureRandom} sementara database hanya menyimpan SHA-256 hex dari token
	 * tersebut.</p>
	 *
	 * <p>Pembedaan ini penting karena inisiatif audit yang berjalan berulang kali menemukan
	 * modul lama AIS yang menempuh jalan sebaliknya: password mentah dirakit ke dalam string
	 * HTML dan dikirim lewat email, sekaligus disimpan di kolom database agar "bisa dilihat
	 * admin". Dua kesalahan itu saling memperkuat — pengguna tidak pernah dipaksa mengganti
	 * password, dan salinan permanennya tersedia di dua tempat sekaligus. Modul pendaftaran
	 * tenant ini tidak mengulanginya: tidak ada kolom penampung rahasia di sini, dan alur
	 * yang aktif saat ini bahkan tidak pernah membangkitkan password untuk pendaftar sama
	 * sekali (pendaftar menetapkan password-nya sendiri di form, yang langsung di-hash
	 * PBKDF2 sebelum menyentuh database).</p>
	 *
	 * <p>Catatan implementasi untuk pengembang yang kelak mengaktifkan entity tidur ini:
	 * getter ini sengaja TIDAK memberi nilai default (berbeda dari {@link #getCredentialVersion()}
	 * yang mem-default ke 1). Artinya baris yang ditulis tanpa mengisi kanal akan gagal pada
	 * constraint {@code NOT NULL} di database, bukan diam-diam tercatat dengan kanal yang
	 * salah. Perilaku gagal-keras itu tepat untuk field audit keamanan: lebih baik penulisan
	 * bukti ditolak daripada menghasilkan bukti yang menyesatkan tentang bagaimana sebuah
	 * rahasia dipaparkan.</p>
	 *
	 * @return kode kanal penyerahan, atau {@code null} bila belum diisi
	 */
	@Column(name = "delivery_channel", nullable = false, length = 40)
	public String getDeliveryChannel() {
		return deliveryChannel;
	}

	/**
	 * Menetapkan kanal penyerahan. Isi dengan salah satu konstanta
	 * {@link #CHANNEL_SCREEN_ONCE} atau {@link #CHANNEL_EMAIL}; tidak ada validasi nilai di
	 * sini maupun di database.
	 *
	 * @param deliveryChannel kode kanal penyerahan
	 */
	public void setDeliveryChannel(String deliveryChannel) {
		this.deliveryChannel = deliveryChannel;
	}

	/**
	 * Waktu kredensial diserahkan sistem kepada pemiliknya (halaman dirender, atau email
	 * dikirim). Bersama {@link #getAcknowledgedAt()} membentuk pasangan "dikirim vs diakui"
	 * yang memungkinkan dukungan pelanggan membedakan kasus "kredensial belum pernah
	 * dikirim" dari "sudah dikirim tetapi tidak pernah dibuka".
	 *
	 * @return waktu penyerahan, atau {@code null} bila belum diserahkan
	 */
	@Column(name = "delivered_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDeliveredAt() {
		return deliveredAt;
	}

	/**
	 * Menetapkan waktu penyerahan kredensial.
	 *
	 * @param deliveredAt waktu penyerahan
	 */
	public void setDeliveredAt(Date deliveredAt) {
		this.deliveredAt = deliveredAt;
	}

	/**
	 * Waktu penerima secara eksplisit mengonfirmasi sudah menerima dan menyimpan kredensial
	 * (mis. menekan tombol "saya sudah menyalin" pada layar tampil-sekali). Selama masih
	 * {@code null}, kredensial dianggap diserahkan tetapi belum diakui — kondisi yang wajar
	 * dipakai untuk mengingatkan pemilik atau untuk menahan penutupan permohonan.
	 *
	 * @return waktu pengakuan penerimaan, atau {@code null} bila belum diakui
	 */
	@Column(name = "acknowledged_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getAcknowledgedAt() {
		return acknowledgedAt;
	}

	/**
	 * Menetapkan waktu pengakuan penerimaan oleh pemilik kredensial.
	 *
	 * @param acknowledgedAt waktu pengakuan
	 */
	public void setAcknowledgedAt(Date acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	/**
	 * Generasi kredensial yang diserahkan pada baris ini: {@code 1} untuk penerbitan pertama,
	 * lalu naik satu setiap kali kredensial diterbitkan ulang (mis. pemilik kehilangan
	 * kredensial awal). Getter mengembalikan {@code 1} bila field masih {@code null},
	 * sehingga baris lama yang ditulis sebelum kolom ini terisi tetap terbaca konsisten
	 * sebagai penerbitan pertama; perhatikan bahwa default ini hanya berlaku di lapisan
	 * Java — kolom di database tetap menyimpan {@code NULL} sampai ada penulisan eksplisit.
	 *
	 * <p>Nomor generasi adalah mekanisme yang membuat sifat show-once (invariant #12 ERD)
	 * dapat ditegakkan tanpa perlu menyimpan rahasianya. Karena sistem tidak menyimpan
	 * kredensial, permintaan "tolong kirim ulang password saya" secara teknis mustahil
	 * dilayani sebagai pengiriman ulang; yang bisa dilakukan hanyalah menerbitkan kredensial
	 * baru, yang otomatis membatalkan kredensial lama dan menghasilkan baris bukti baru
	 * dengan {@code credentialVersion} lebih tinggi. Deret nomor generasi pada satu
	 * permohonan karena itu berfungsi sebagai indikator penyalahgunaan yang berguna:
	 * lonjakan penerbitan ulang dalam waktu singkat adalah sinyal klasik upaya pengambilalihan
	 * akun (penyerang yang menguasai kotak surat pendaftar memicu penerbitan berulang), dan
	 * karena kelas ini ber-{@code @Audited}, deret tersebut tidak dapat dirapikan diam-diam
	 * oleh pihak yang punya akses tulis ke tabel utama.</p>
	 *
	 * <p>Perlu dicatat bahwa penegakan urutan generasi tidak dilakukan di kelas ini: tidak ada
	 * unique constraint atas pasangan ({@code pendaftaran_tenant_id}, {@code credential_version})
	 * dan tidak ada kolom {@code @Version} optimistic locking pada entity ini (berbeda dari
	 * {@link TenantMembership} yang memilikinya). Bila alur penerbitan kelak diaktifkan dan
	 * dijalankan konkuren, dua permintaan penerbitan ulang yang bersamaan dapat menghasilkan
	 * dua baris dengan nomor generasi identik. Untuk bukti audit hal itu tidak merusak
	 * keamanan kredensial itu sendiri, tetapi membuat rekonstruksi urutan kejadian menjadi
	 * ambigu; menambahkan unique constraint pada pasangan kolom tersebut adalah pengerasan
	 * yang murah dan disarankan saat implementasi menyusul.</p>
	 *
	 * @return nomor generasi kredensial, minimal {@code 1}
	 */
	@Column(name = "credential_version")
	public Integer getCredentialVersion() {
		return credentialVersion == null ? Integer.valueOf(1) : credentialVersion;
	}

	/**
	 * Menetapkan nomor generasi kredensial. Nilai {@code null} akan dibaca kembali sebagai
	 * {@code 1} oleh {@link #getCredentialVersion()}.
	 *
	 * @param credentialVersion nomor generasi kredensial
	 */
	public void setCredentialVersion(Integer credentialVersion) {
		this.credentialVersion = credentialVersion;
	}

	/**
	 * Waktu baris bukti ini dibuat. Berbeda dari {@link #getDeliveredAt()}: baris dapat
	 * dibuat lebih dahulu (mis. saat kredensial dibangkitkan) dan baru kemudian ditandai
	 * terserahkan.
	 *
	 * @return waktu pembuatan baris, atau {@code null} bila belum diisi
	 */
	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	/**
	 * Menetapkan waktu pembuatan baris bukti.
	 *
	 * @param createdAt waktu pembuatan
	 */
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	/**
	 * Nama pengguna yang membuat/mengubah baris (field audit shadow standar AIS).
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pengguna pembuat/pengubah. Setter sengaja MENGABAIKAN nilai
	 * {@code null} maupun string kosong/spasi — pola baku audit shadow AIS yang mencegah
	 * jejak pelaku yang sudah terisi tertimpa nilai kosong oleh proses batch atau binding
	 * form yang tidak menyertakan field ini.
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
