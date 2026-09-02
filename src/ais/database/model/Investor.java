package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * <b>Investor</b> -- pemilik modal pada satu atau lebih toko/brand milik satu {@link Pendaftar}
 * ebisnis.id. Satu Pendaftar boleh punya banyak Investor; satu Investor bisa memegang persentase
 * kepemilikan di lebih dari satu toko/brand -- disimpan sbg {@code kepemilikanJson} (array
 * {@code [{tokoId, persentase}]}) drpd tabel relasi terpisah, supaya iterasi pertama ini tetap
 * ringkas (lihat catatan di {@code PendaftarDashboardHelper}; normalisasi ke tabel sendiri bisa
 * menyusul kalau kebutuhan laporan bagi-hasil sudah lebih jelas).
 *
 * <p>Login memakai kredensial plaintext ({@code userid}/{@code pass}) -- KONSISTEN dgn skema
 * {@code ais.database.model.inventory.Pedagang} yang sudah dipakai seluruh ekosistem POS (akun
 * dibuat oleh pemilik bisnis/Pendaftar, bukan pendaftaran mandiri publik spt {@link Pendaftar}
 * sendiri yang pakai hash PBKDF2 -- beda tingkat kepercayaan, lihat JavaDoc
 * {@code PendaftarPublicHelper}). Dipilih supaya QR-login bisa langsung meng-encode
 * {@code userid:pass} tanpa mekanisme token terpisah.</p>
 *
 * <h3>PENTING: ini BUKAN entity sistem informasi akademik</h3>
 * <p>Meski berada di paket {@code ais.database.model} bersama {@code Mahasiswa}/{@code Dosen}/
 * {@code Krs}, entity ini sama sekali tidak berhubungan dgn perkuliahan. Ia bagian dari lini
 * produk <b>ebisnis.id</b> (platform SaaS multi-tenant POS/ERP) yang menumpang codebase &amp;
 * SessionFactory yang sama. Trio entity ebisnis.id yang secara struktural KEMBAR dan selalu
 * berubah bersama:</p>
 * <ul>
 *   <li>{@link Brand} -- merek dagang milik Pendaftar (tanpa kredensial);</li>
 *   <li><b>{@code Investor}</b> (kelas ini) -- pemodal, + {@code userid}/{@code pass} +
 *       {@code kepemilikanJson};</li>
 *   <li>{@link AkunManajemen} -- staf modul Manajemen, + {@code userid}/{@code pass} +
 *       {@code jabatan}.</li>
 * </ul>
 * <p>Ketiganya ber-{@code pendaftar} NOT NULL dan didaftarkan berdampingan di
 * {@code src/hibernate.cfg.xml} (blok {@code <mapping class=...>} ~baris 409-412). Tabel fisiknya
 * dibuat otomatis oleh {@code hbm2ddl.auto=update} -- tidak ada skrip DDL manual untuk
 * {@code public.investor} di repo.</p>
 *
 * <h3>Siapa yang memakai entity ini (terverifikasi, bukan dugaan)</h3>
 * <p>Seluruh pemakaian nyata terkonsentrasi di SATU kelas,
 * {@code ais.action.servlet.api.PendaftarDashboardHelper}, dipanggil lewat
 * {@code ais.action.servlet.EbisnisPublicServlet} (parameter {@code s=<subAksi>}), dan
 * dirender oleh {@code webapp/WEB-INF/baru/dashboard_ebisnis.jsp} tab "Investor":</p>
 * <ul>
 *   <li>{@code s=ringkasan} -- {@code PendaftarDashboardHelper.ringkasan()} menghitung
 *       {@code jumlahInvestor} (rowCount ber-filter {@code pendaftar.id}) untuk kartu statistik
 *       dashboard;</li>
 *   <li>{@code s=investor_list} -- {@code investorList()} menampilkan
 *       id/nama/email/userid/kepemilikanJson/aktif (kolom {@code pass} TIDAK ikut dikirim);</li>
 *   <li>{@code s=investor_tambah} -- {@code investorTambah()} SATU-SATUNYA jalur penciptaan
 *       baris (lihat "Siklus hidup" di bawah);</li>
 *   <li>{@code s=investor_nonaktif} -- {@code investorNonaktif()} membalik flag {@link #getAktif()}.
 *       Rute ini ADA di servlet ({@code EbisnisPublicServlet} ~baris 211) tapi tabel investor di
 *       {@code dashboard_ebisnis.jsp} hanya menggambar badge Aktif/Nonaktif TANPA tombol pemicu --
 *       jadi dalam praktik aksi ini cuma bisa dicapai lewat POST manual;</li>
 *   <li>{@code PendaftarDashboardHelper.useridDipakai()} -- entity ini ikut disapu saat memeriksa
 *       tabrakan userid lintas {@code Pedagang}/{@code Investor}/{@code AkunManajemen}.</li>
 * </ul>
 * <p><b>Tidak ada layar ZK</b> (tidak ada {@code .zul}, tidak ada {@code Action} ZK) yang
 * menyentuh entity ini -- berbeda dari mayoritas entity AIS. Konsekuensinya: seluruh mekanisme
 * hak akses ZK ({@code CommonPrivilages.doCheckSecurity}, whitelist {@code MUST_CHECKED}, menu
 * {@code Tbmrole}) TIDAK berlaku sama sekali di sini; otorisasinya murni "sesi Pendaftar
 * ebisnis.id" (lihat di bawah).</p>
 *
 * <h3>Model otorisasi -- CONTOH POSITIF</h3>
 * <p>Berbeda dgn banyak Action ZK di AIS, jalur ebisnis.id ini IDOR-safe secara desain:
 * {@code EbisnisPublicServlet.prosesDashboard()} meresolusi {@link Pendaftar} dari
 * {@code HttpSession} (atribut {@code pendaftarEbisnisEntity}), <b>tidak pernah</b> dari parameter
 * klien, lalu setiap query di helper difilter ulang {@code pendaftar.id = <sesi>} di sisi server.
 * {@code investorNonaktif()} bahkan memverifikasi ulang kepemilikan baris hasil
 * {@code session.get(Investor.class, id)} sebelum menyentuhnya. Menebak/mengubah {@code id} di
 * payload tidak membuka data Pendaftar lain.</p>
 * <p>Di atasnya ada gerbang kedua: karena sub-aksi berakhiran {@code _tambah}/{@code _nonaktif},
 * mutasi Investor ditolak bila tenant belum READY/ACTIVE
 * ({@code TenantOnboardingService.alasanTidakBolehMutasi}, kode {@code TENANT_NOT_READY}). Catat
 * bahwa gerbang <i>entitlement modul</i> hanya diminta untuk sub-aksi {@code toko_*}/
 * {@code mesin_pos_*} ({@code modulPerlu = "POS"}), sedangkan Investor lewat dgn
 * {@code modulPerlu = null} -- artinya cukup status tenant, tanpa modul berbayar apa pun.</p>
 *
 * <h3>Siklus hidup baris (create-only)</h3>
 * <p>Baris dibuat sekali oleh {@code investorTambah()}: {@code nama} wajib non-kosong,
 * {@code email}/{@code telp}/{@code kepemilikanJson} diambil apa adanya dari request
 * ({@code kepemilikanJson} default {@code "[]"}), {@code userid} dibangkitkan
 * {@code buatUseridUnik(session, "inv-" + nama)} (slug huruf-kecil + 4 digit acak
 * {@code SecureRandom}), {@code pass} 8 karakter acak dari alfabet tanpa karakter rancu,
 * {@code aktif = true}, {@code dibuatPada = WaktuUtil.getDate()}. Respons JSON mengembalikan
 * {@code userid}, {@code password}, dan {@code qrData = userid + ":" + password} -- ditampilkan
 * SEKALI di kotak kredensial dashboard, tidak pernah bisa ditampilkan ulang lewat
 * {@code investor_list}.</p>
 * <p><b>TIDAK ADA jalur ubah.</b> Tidak ada sub-aksi {@code investor_ubah} di servlet maupun
 * method {@code investorUbah} di helper. Setelah tersimpan, {@code nama}/{@code email}/
 * {@code telp}/{@code kepemilikanJson}/{@code pass} tidak dapat dikoreksi lewat UI mana pun --
 * satu-satunya properti yang masih bisa berubah adalah {@code aktif} (itu pun tanpa tombol, lihat
 * di atas). Salah ketik persentase kepemilikan atau email hanya bisa diperbaiki lewat SQL
 * langsung. Ini gap yang sudah dicatat sbg <b>G-06</b>/§4 di
 * {@code docs/pendaftaran-tenant/01-source-audit.md} ("HANYA list+tambah").</p>
 *
 * <h3>{@code kepemilikanJson} belum punya pembaca</h3>
 * <p>Kolom ini ditulis mentah dari request dan dikirim balik apa adanya ke tabel dashboard, tapi
 * <b>tidak ada satu pun kode di codebase yang mem-parse-nya</b> -- tidak ada perhitungan bagi
 * hasil, tidak ada validasi bahwa total persentase &le; 100, tidak ada pengecekan bahwa
 * {@code tokoId} yang dirujuk benar-benar milik Pendaftar yang sama, bahkan tidak ada validasi
 * bahwa isinya JSON yang sah. Fitur "Investor &amp; Bagi Hasil" yang dijanjikan di landing page
 * {@code ebisnis.jsp} masih berupa penampung data, belum mesin hitung. Perlakukan isi kolom ini
 * sbg masukan klien yang belum tervalidasi.</p>
 *
 * <h3>Pemetaan Hibernate &amp; kejutan yang perlu diketahui</h3>
 * <ul>
 *   <li><b>Access type PROPERTY</b> -- {@code @Id} ditempel di {@link #getId()}, jadi Hibernate
 *   membaca/menulis SEMUA properti lewat getter, bukan field. Akibat konkretnya: normalisasi yang
 *   dilakukan di dalam getter ikut TERSIMPAN ke DB. Lihat {@link #getNama()} (trim) dan
 *   {@link #getAktif()} ({@code null} menjadi {@code true}).</li>
 *   <li><b>{@link GeneralValueObject} bukan {@code @MappedSuperclass}</b> -- properti induk
 *   ({@code oleh}, {@code olehId}, dst) TIDAK dipetakan Hibernate. Karena itu {@code id} dan
 *   {@code tanggal_dirubah} sengaja dideklarasikan ULANG di kelas ini; itu KEHARUSAN TEKNIS, bukan
 *   duplikasi ceroboh. Perhatikan entity ini <i>tidak</i> mendeklarasikan ulang {@code oleh}/
 *   {@code olehId} spt kebanyakan entity AIS lama, jadi tabel {@code public.investor} tidak punya
 *   kolom "diubah oleh siapa" -- jejak pelaku hanya ada di revisi Envers.</li>
 *   <li><b>{@code @Audited} (Envers)</b> -- setiap insert/update/delete menyalin SELURUH properti
 *   terpetakan ke tabel audit {@code new_audit.investor__audit} (suffix {@code __audit},
 *   {@code default_schema=new_audit} di {@code hibernate.cfg.xml}). Karena
 *   {@code org.hibernate.envers.store_data_at_delete=true}, salinan itu TETAP ADA setelah baris
 *   aslinya dihapus. Kolom {@code pass} plaintext ikut tersalin di setiap revisi.</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate}</b> -- SQL dibangun hanya untuk kolom yang
 *   benar-benar terisi/berubah, wajar untuk tabel dgn banyak kolom nullable.</li>
 *   <li><b>Tidak ikut cutover skema tenant</b> -- {@code TenantDataPlaneService} hanya punya
 *   {@code mirrorBrand}/{@code mirrorToko}/{@code mirrorPedagang}. Data Investor tetap tinggal di
 *   skema {@code public} bersama seluruh tenant, tidak dipindah/disalin ke skema per-tenant.
 *   Konsekuensinya {@code investorList()} juga tidak punya cabang {@code sumberData =
 *   "tenant-schema"} spt {@code brandList()}.</li>
 * </ul>
 *
 * <h3>Catatan keamanan (dicatat apa adanya, bukan diperbaiki di sini)</h3>
 * <ul>
 *   <li>{@code pass} disimpan <b>plaintext</b>. Ini keputusan sadar (lihat paragraf pembuka), tapi
 *   berarti pembacaan tabel apa pun -- termasuk lewat jalur generik -- membocorkan password yang
 *   bisa langsung dipakai, bukan sekadar hash.</li>
 *   <li>Endpoint generik {@code /Api} aksi {@code dataRinci}
 *   ({@code ais.action.servlet.api.ElearningApiUtil#dataRinci}) melakukan
 *   {@code Class.forName(<nama kelas dari klien>)} tanpa allow-list, lalu
 *   {@code Common.insertProperty(...)} menyalin SEMUA properti tanpa daftar-hitam. Kelas ini
 *   terpetakan Hibernate, jadi ia terjangkau dari sana dgn token login APA SAJA -- dan yang
 *   terbawa adalah {@code pass} plaintext. Jalur dashboard sendiri sudah benar tidak pernah
 *   mengirim {@code pass}; kebocoran ada di endpoint generiknya, bukan di sini.</li>
 *   <li>Aksi dashboard ebisnis.id belum memakai token CSRF (dicatat di
 *   {@code docs/pendaftaran-tenant/01-source-audit.md} §"dashboard_ebisnis.jsp").</li>
 *   <li>Keunikan {@code userid} dijaga dua lapis yang tidak setara: constraint {@code unique}
 *   hanya per-tabel, sedangkan pengecekan lintas-tabel {@code useridDipakai()} bersifat
 *   best-effort (cek-lalu-simpan, bukan atomik).</li>
 * </ul>
 *
 * @see Pendaftar
 * @see AkunManajemen
 * @see Brand
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "investor")
public class Investor extends GeneralValueObject {

	/** Versi serialisasi Java; entity ini ikut diserialkan lewat {@link GeneralValueObject}. */
	private static final long serialVersionUID = 1L;

	/** Primary key {@code public.investor.id}, IDENTITY (di-generate DB). */
	private Long id;

	/** Nama investor (orang/badan). Wajib; nilai yang tersimpan selalu ter-trim, lihat {@link #getNama()}. */
	private String nama;

	/** Alamat surel investor. Opsional, tidak divalidasi formatnya, tidak dipakai untuk apa pun selain ditampilkan. */
	private String email;

	/** Nomor telepon investor. Opsional, teks bebas, tidak dinormalisasi. */
	private String telp;

	/** Pemilik bisnis (tenant ebisnis.id) yang memiliki baris ini. Wajib; dasar seluruh penyaringan IDOR-safe. */
	private Pendaftar pendaftar;

	/** Userid login, di-generate {@code inv-<slug nama>-<4 digit>}; unik per tabel. */
	private String userid;

	/** Password login <b>plaintext</b> 8 karakter acak. Lihat catatan keamanan di JavaDoc kelas. */
	private String pass;

	/** Persentase kepemilikan lintas toko/brand sbg teks JSON mentah. Belum pernah di-parse siapa pun. */
	private String kepemilikanJson;

	/** Flag aktif/nonaktif. {@code null} diperlakukan (dan akhirnya tersimpan) sbg {@code true}. */
	private Boolean aktif;

	/** Waktu pembuatan baris, diisi sekali oleh {@code PendaftarDashboardHelper.investorTambah()}. */
	private Date dibuatPada;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans. Seluruh properti diisi lewat
	 * setter oleh {@code PendaftarDashboardHelper.investorTambah()}; tidak ada nilai default yang
	 * dipasang di sini kecuali {@link #tanggal_dirubah} (lihat deklarasi field-nya).
	 */
	public Investor() {
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai metadata perubahan lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum Hibernate mengirim UPDATE.
	 *
	 * <p>Dipanggil oleh Hibernate, bukan oleh kode aplikasi. Perhatikan callback ini hanya berjalan
	 * pada UPDATE -- pada INSERT (baris investor baru dari dashboard) ia tidak dieksekusi, sehingga
	 * nilai {@link #tanggal_dirubah} untuk baris baru berasal dari inisialisasi field.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu perubahan terakhir. Dideklarasikan ULANG di sini (bukan diwarisi) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} sehingga properti induk tidak
	 * dipetakan Hibernate. Diinisialisasi ke waktu pembuatan object di JVM -- artinya untuk baris
	 * yang BARU dibuat nilainya praktis sama dgn {@link #dibuatPada}.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Setter stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu perubahan; biasanya diisi
	 *                        {@code AuditTimestampInterceptor}, bukan kode pemanggil
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Getter stempel waktu perubahan terakhir (kolom {@code tanggal_dirubah}, TIMESTAMP).
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk object yang dibuat di JVM
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Primary key baris. Kolom {@code id} ber-{@code insertable = false} karena nilainya
	 * di-generate database (IDENTITY / sequence PostgreSQL); baru terisi setelah
	 * {@code session.save()} dieksekusi.
	 *
	 * <p>Anotasi {@code @Id} di getter inilah yang menetapkan access type PROPERTY untuk SELURUH
	 * entity -- konsekuensinya dibahas di JavaDoc kelas.</p>
	 *
	 * @return id baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter primary key. Praktis tidak pernah dipanggil kode aplikasi (nilai datang dari DB);
	 * disediakan untuk Hibernate.
	 *
	 * @param id id baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama investor, dikembalikan dalam bentuk sudah di-{@code trim()}.
	 *
	 * <p><b>Efek samping tidak langsung:</b> karena access type entity ini PROPERTY, Hibernate
	 * membaca nilai lewat getter ini saat INSERT dan saat dirty-check. Jadi spasi di awal/akhir
	 * TIDAK PERNAH sampai ke database -- normalisasi di getter ini efektif menjadi normalisasi
	 * penyimpanan. Field mentahnya sendiri tetap menyimpan versi belum-trim sampai object dimuat
	 * ulang dari DB (terlihat mis. di {@link #toString()} yang membaca field langsung).</p>
	 *
	 * @return nama investor yang sudah di-trim, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Setter nama investor. Nilai disimpan apa adanya; trim terjadi belakangan di
	 * {@link #getNama()}. Pemanggil ({@code investorTambah()}) sudah menolak nama kosong lebih dulu.
	 *
	 * @param nama nama investor (orang/badan)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Alamat surel investor (kolom {@code email}, opsional).
	 *
	 * <p>Hanya ditampilkan di tabel dashboard; tidak ada pengiriman surel, tidak ada validasi
	 * format, dan tidak dipakai sbg identitas login (login memakai {@link #getUserid()}).</p>
	 *
	 * @return alamat surel, boleh {@code null} atau string kosong
	 */
	@Column(name = "email", nullable = true, length = 255)
	public String getEmail() {
		return email;
	}

	/**
	 * Setter alamat surel investor.
	 *
	 * @param email alamat surel; {@code investorTambah()} mengisinya dgn string kosong bila
	 *              parameter request tidak dikirim
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Nomor telepon investor (kolom {@code telp}, opsional, teks bebas).
	 *
	 * <p>Disimpan tapi tidak pernah dibaca kembali oleh kode mana pun -- {@code investorList()}
	 * bahkan tidak mengirimkannya ke dashboard, sehingga nilainya hanya terlihat lewat query DB
	 * langsung.</p>
	 *
	 * @return nomor telepon apa adanya, boleh {@code null}
	 */
	@Column(name = "telp", nullable = true, length = 50)
	public String getTelp() {
		return telp;
	}

	/**
	 * Setter nomor telepon investor.
	 *
	 * @param telp nomor telepon dalam format bebas; tidak dinormalisasi
	 */
	public void setTelp(String telp) {
		this.telp = telp;
	}

	/**
	 * Pemilik bisnis (tenant) yang memiliki baris investor ini -- relasi {@code @ManyToOne} LAZY ke
	 * kolom FK {@code pendaftar} (NOT NULL).
	 *
	 * <p><b>Pola write-back {@code check()} -- terverifikasi ada di file ini.</b> Getter memanggil
	 * {@code GeneralValueObject.check(pendaftar)} lalu <b>menugaskan hasilnya kembali ke field</b>
	 * sebelum mengembalikannya. Itu wajib karena {@code check()} bisa mengembalikan instance LAIN
	 * (kanonik dari {@code EntityIdentityMap}, hasil cache, atau hasil reload) dan bukan proxy
	 * semula. Tujuannya mencegah {@code LazyInitializationException} saat object sudah detached --
	 * kondisi normal di sini, karena {@code PendaftarDashboardHelper} menutup {@link
	 * org.hibernate.Session}-nya di {@code finally} sementara object hasil query masih dipakai
	 * untuk menyusun JSON.</p>
	 *
	 * <p><b>Efek samping yang mungkin:</b> pada tahap terakhir resolusi, {@code check()} dapat
	 * MEMBUKA session Hibernate baru sendiri untuk me-reload object detached lalu menutupnya
	 * kembali. Jadi getter ini tidak selalu murni -- pada kasus terburuk ia memicu satu query
	 * tambahan. Ia tidak pernah melempar exception dan tidak pernah mengembalikan {@code null}
	 * untuk field non-null; bila seluruh tahap gagal, proxy semula dikembalikan apa adanya
	 * (kegagalan senyap).</p>
	 *
	 * @return Pendaftar pemilik baris ini, sudah teresolusi bila memungkinkan
	 * @see ais.database.model.GeneralValueObject#check(Object)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Setter pemilik bisnis. {@code investorTambah()} mengisinya dgn {@code session.load(...)}
	 * (proxy tanpa query) berdasarkan id Pendaftar dari SESI -- tidak pernah dari parameter klien.
	 *
	 * @param pendaftar tenant pemilik baris; tidak boleh {@code null} saat disimpan (kolom NOT NULL)
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * Userid login investor (kolom {@code userid}, {@code unique}, NOT NULL).
	 *
	 * <p>Selalu hasil generate {@code PendaftarDashboardHelper.buatUseridUnik()} dgn basis
	 * {@code "inv-" + nama}: slug huruf kecil (maks 30 karakter) + tanda hubung + 4 digit acak,
	 * mis. {@code inv-budi-santoso-4271}. Klien tidak pernah bisa memilih userid sendiri.
	 * Keunikannya dicek best-effort lintas tabel ({@code koperasi.pedagang} lewat SQL native,
	 * plus {@code Investor} dan {@code AkunManajemen} lewat Criteria) sebelum simpan; constraint DB
	 * sendiri hanya menjamin keunikan di dalam tabel {@code investor}.</p>
	 *
	 * @return userid login, atau {@code null} bila baris belum tersimpan
	 */
	@Column(name = "userid", unique = true, nullable = false, length = 100)
	public String getUserid() {
		return userid;
	}

	/**
	 * Setter userid login.
	 *
	 * @param userid userid hasil {@code buatUseridUnik()}; jangan diisi dari masukan klien
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Password login investor -- disimpan dan dikembalikan sbg <b>plaintext</b> (kolom {@code pass},
	 * NOT NULL).
	 *
	 * <p>Nilainya 8 karakter acak {@code SecureRandom} dari alfabet tanpa karakter rancu (tanpa
	 * {@code i}, {@code l}, {@code o}, {@code 0}, {@code 1}), dibangkitkan
	 * {@code PendaftarDashboardHelper.buatPasswordAcak(8)}, ditampilkan ke pemilik bisnis SEKALI
	 * saat pembuatan (bersama {@code qrData = userid + ":" + pass}) dan tidak pernah bisa dilihat
	 * ulang lewat jalur dashboard -- {@code investorList()} sengaja tidak mengirim properti ini.</p>
	 *
	 * <p><b>Perhatian:</b> nilai ini juga tersalin ke tabel audit Envers
	 * {@code new_audit.investor__audit} pada setiap revisi, dan tetap tersimpan di sana setelah
	 * baris aslinya dihapus ({@code store_data_at_delete=true}). Getter ini juga ikut terbaca oleh
	 * serialisasi properti generik ({@code Common.insertProperty}) yang dipakai endpoint
	 * {@code /Api} aksi {@code dataRinci} -- lihat catatan keamanan di JavaDoc kelas.</p>
	 *
	 * @return password plaintext, atau {@code null} bila baris belum tersimpan
	 */
	@Column(name = "pass", nullable = false, length = 100)
	public String getPass() {
		return pass;
	}

	/**
	 * Setter password login.
	 *
	 * @param pass password plaintext hasil {@code buatPasswordAcak()}; tidak di-hash di mana pun
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Komposisi kepemilikan investor lintas toko/brand sbg teks JSON mentah.
	 *
	 * <p>Bentuk yang diharapkan: {@code [{"tokoId":1,"persentase":30.0}, ...]} -- lihat JavaDoc
	 * kelas. Nilai ini ditulis langsung dari parameter request (default {@code "[]"}) dan dikirim
	 * balik apa adanya ke dashboard; <b>tidak ada kode di codebase yang mem-parse, memvalidasi,
	 * atau menghitung apa pun darinya</b> (tidak ada pemeriksaan total &le; 100%, tidak ada
	 * pemeriksaan bahwa {@code tokoId} milik Pendaftar yang sama, tidak ada pemeriksaan JSON sah).
	 * Perlakukan sbg masukan klien belum tervalidasi.</p>
	 *
	 * @return teks JSON kepemilikan apa adanya, boleh {@code null} untuk baris lama
	 */
	@Column(name = "kepemilikan_json", nullable = true, columnDefinition = "text")
	public String getKepemilikanJson() {
		return kepemilikanJson;
	}

	/**
	 * Setter komposisi kepemilikan.
	 *
	 * @param kepemilikanJson teks JSON array kepemilikan; disimpan tanpa validasi apa pun
	 */
	public void setKepemilikanJson(String kepemilikanJson) {
		this.kepemilikanJson = kepemilikanJson;
	}

	/**
	 * Status aktif investor, dgn default berpihak "aktif": {@code null} dilaporkan sbg
	 * {@code true}.
	 *
	 * <p><b>Efek samping tidak langsung (fail-open + write-back):</b> karena access type PROPERTY,
	 * Hibernate membaca nilai lewat getter ini saat INSERT/UPDATE. Jadi baris yang field-nya
	 * {@code null} akan TERSIMPAN sbg {@code true} pada penyimpanan berikutnya -- normalisasi
	 * getter menjadi normalisasi database. Tidak ada jalan mengekspresikan "belum ditentukan" di
	 * kolom ini setelah baris pernah tersimpan lewat entity ini.</p>
	 *
	 * <p>Perhatikan properti ini SATU-SATUNYA di kelas ini yang tidak diberi {@code @Column}
	 * eksplisit; Hibernate memakai nama properti sbg nama kolom ({@code aktif}). Perilakunya sama,
	 * hanya tidak konsisten dgn properti tetangganya. Konsumennya:
	 * {@code investorList()} (badge Aktif/Nonaktif) dan {@code investorNonaktif()} (satu-satunya
	 * penulis, dgn semantik {@code "true".equals(request.optString("aktif","false"))} -- artinya
	 * nilai apa pun selain persis {@code "true"} berarti dinonaktifkan).</p>
	 *
	 * @return {@code true} bila aktif atau belum ditentukan; {@code false} hanya bila eksplisit
	 *         dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Setter status aktif.
	 *
	 * @param aktif status baru; {@code null} akan terbaca kembali sbg {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Waktu baris investor dibuat (kolom {@code dibuat_pada}, TIMESTAMP).
	 *
	 * <p>Diisi eksplisit sekali oleh {@code investorTambah()} dgn {@code WaktuUtil.getDate()};
	 * tidak ada nilai default di level entity maupun DDL. Baris yang lahir dari jalur lain (mis.
	 * penyisipan SQL manual) akan memiliki {@code null} di sini.</p>
	 *
	 * @return waktu pembuatan, atau {@code null} bila tidak pernah diisi
	 */
	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	/**
	 * Setter waktu pembuatan baris.
	 *
	 * @param dibuatPada waktu pembuatan; hanya diisi saat baris pertama kali dibuat
	 */
	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	/**
	 * Representasi teks singkat berformat {@code <id>-<nama>}, dipakai untuk log/debug.
	 *
	 * <p>Membaca FIELD langsung, bukan getter -- jadi {@code nama} yang ditampilkan adalah versi
	 * belum di-trim, dan untuk object yang belum tersimpan hasilnya berawalan {@code "null-"}.</p>
	 *
	 * @return gabungan id dan nama dipisah tanda hubung
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
