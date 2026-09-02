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
 * <b>AkunManajemen</b> -- akun pengguna modul Manajemen (SDM/Payroll/Logistik/Surat
 * Menyurat/Workflow/Akunting/Finance/Aset &amp; Inventaris/Produksi/Ekspedisi/Pelacakan
 * Kendaraan/Antar Jemput/Audit &amp; Pengawasan Internal, dst) milik satu {@link Pendaftar}.
 * {@code jabatan} sekadar label bebas tahap ini (mis. "HRD", "Finance") -- pemetaan ke
 * modul/hak-akses spesifik menyusul saat konten tiap modul Manajemen dibangun.
 *
 * <h3>PENTING: ini BUKAN entity sistem informasi akademik</h3>
 * <p>Meski tinggal di paket {@code ais.database.model} bersama {@code Mahasiswa}/{@code Dosen}/
 * {@code Krs}, entity ini tidak ada hubungannya dgn perkuliahan. Ia bagian dari lini produk
 * <b>ebisnis.id</b> (platform SaaS multi-tenant POS/ERP) yang menumpang codebase, {@code
 * hibernate.cfg.xml}, dan SessionFactory yang sama dgn AIS. Trio entity ebisnis.id yang
 * secara struktural KEMBAR dan selalu berubah bersama:</p>
 * <ul>
 *   <li>{@link Brand} -- merek dagang milik Pendaftar (tanpa kredensial);</li>
 *   <li>{@link Investor} -- pemodal, + {@code userid}/{@code pass} + {@code kepemilikanJson};</li>
 *   <li><b>{@code AkunManajemen}</b> (kelas ini) -- staf modul Manajemen, + {@code userid}/
 *       {@code pass} + {@code jabatan}.</li>
 * </ul>
 * <p>Ketiganya ber-{@code pendaftar} NOT NULL dan didaftarkan berdampingan di
 * {@code src/hibernate.cfg.xml} (blok {@code <mapping class=...>} baris 410-412). Tabel fisiknya
 * dibuat otomatis oleh {@code hbm2ddl.auto=update}; tidak ada skrip DDL manual untuk
 * {@code public.akun_manajemen} di repo, dan tidak ada satu pun SQL native yang menyebut nama
 * tabel itu (verifikasi: {@code grep -rn "akun_manajemen"} atas seluruh {@code src}/{@code webapp}
 * hanya mengenai anotasi {@code @Table} di kelas ini sendiri).</p>
 *
 * <h3>Siapa yang memakai entity ini (terverifikasi, bukan dugaan)</h3>
 * <p>Hanya TIGA berkas Java di seluruh codebase menyebut nama kelas ini: kelas ini sendiri,
 * {@link Investor} (rujukan JavaDoc silang), dan
 * {@code ais.action.servlet.api.PendaftarDashboardHelper} -- satu-satunya pemakai nyata.
 * Helper itu dipanggil dari {@code ais.action.servlet.EbisnisPublicServlet}
 * ({@code prosesDashboard}, parameter {@code s=<subAksi>}) dan dirender
 * {@code webapp/WEB-INF/baru/dashboard_ebisnis.jsp} tab "Manajemen":</p>
 * <ul>
 *   <li>{@code s=ringkasan} -- {@code ringkasan()} menghitung {@code jumlahManajemen}
 *       (rowCount ber-filter {@code pendaftar.id}) untuk kartu statistik dashboard;</li>
 *   <li>{@code s=manajemen_list} -- {@code manajemenList()} mengirim
 *       id/nama/jabatan/userid/aktif; kolom {@code pass} <b>TIDAK</b> ikut dikirim;</li>
 *   <li>{@code s=manajemen_tambah} -- {@code manajemenTambah()}, SATU-SATUNYA jalur penciptaan
 *       baris (lihat "Siklus hidup" di bawah);</li>
 *   <li>{@code s=manajemen_nonaktif} -- {@code manajemenNonaktif()} membalik {@link #getAktif()}.
 *       Rute ini ADA di servlet ({@code EbisnisPublicServlet} ~baris 214) tapi tabel Manajemen di
 *       {@code dashboard_ebisnis.jsp} hanya menggambar badge Aktif/Nonaktif TANPA tombol pemicu
 *       (baris ~480) -- dalam praktik aksi ini cuma tercapai lewat POST manual. Persis kuirk yang
 *       sama pada {@link Investor};</li>
 *   <li>{@code PendaftarDashboardHelper.useridDipakai()} -- tabel ini ikut disapu saat memeriksa
 *       tabrakan userid lintas {@code inventory.Pedagang}/{@link Investor}/{@code AkunManajemen}.</li>
 * </ul>
 * <p><b>Tidak ada layar ZK</b> (tidak ada {@code .zul}, tidak ada {@code Action} ZK) yang
 * menyentuh entity ini. Konsekuensinya seluruh mekanisme hak akses ZK
 * ({@code CommonPrivilages.doCheckSecurity}, whitelist {@code MUST_CHECKED}, menu {@code Tbmrole})
 * TIDAK berlaku di sini sama sekali; otorisasinya murni "sesi Pendaftar ebisnis.id".</p>
 *
 * <h3>Model otorisasi -- CONTOH POSITIF (sama spt {@link Investor})</h3>
 * <p>Jalur ebisnis.id ini IDOR-safe secara desain, berbeda dari banyak Action ZK di AIS:
 * {@code EbisnisPublicServlet.prosesDashboard()} meresolusi {@link Pendaftar} dari
 * {@code HttpSession} (atribut {@code SESSION_PENDAFTAR}), <b>tidak pernah</b> dari parameter
 * klien, dan langsung mengembalikan status {@code 91} "Sesi Anda telah berakhir" bila atribut itu
 * kosong. Setiap query di helper lalu difilter ulang {@code pendaftar.id = <sesi>} di sisi server;
 * {@code manajemenNonaktif()} bahkan memverifikasi ulang kepemilikan baris hasil
 * {@code session.get(AkunManajemen.class, id)} sebelum menyentuhnya ("bukan milik Anda"). Menebak
 * atau mengubah {@code id} di payload tidak membuka data Pendaftar lain.</p>
 * <p>Gerbang kedua: karena sub-aksi berakhiran {@code _tambah}/{@code _nonaktif}, mutasi ditolak
 * bila tenant belum READY/ACTIVE ({@code TenantOnboardingService.alasanTidakBolehMutasi}, kode
 * {@code TENANT_NOT_READY}). Catat gerbang <i>entitlement modul</i> hanya diminta untuk sub-aksi
 * {@code toko_*}/{@code mesin_pos_*} ({@code modulPerlu = "POS"}); {@code manajemen_*} lewat dgn
 * {@code modulPerlu = null} -- cukup status tenant, tanpa modul berbayar apa pun.</p>
 * <p><b>Siapa yang boleh membuat/mengubah akun ini:</b> hanya pemilik bisnis (Pendaftar) yang
 * sedang login di dashboard ebisnis.id, dan hanya untuk barisnya sendiri. Tidak ada peran
 * "admin platform", tidak ada jalur staf/operator, tidak ada jalur ZK. Tidak ada pula
 * pembatasan jumlah akun yang boleh dibuat (tidak ada kuota per tenant/paket).</p>
 *
 * <h3>Siklus hidup baris (create-only)</h3>
 * <p>Baris dibuat sekali oleh {@code PendaftarDashboardHelper.manajemenTambah()}: {@code nama}
 * wajib non-kosong, {@code jabatan} diambil apa adanya (boleh kosong, tidak divalidasi terhadap
 * daftar modul mana pun), {@code userid} dibangkitkan {@code buatUseridUnik(session, "mgr-" + nama)}
 * (slug huruf-kecil + 4 digit acak {@link java.security.SecureRandom}), {@code pass} 8 karakter
 * acak dari alfabet tanpa karakter rancu, {@code aktif = true},
 * {@code dibuatPada = WaktuUtil.getDate()}. Respons JSON mengembalikan {@code userid},
 * {@code password}, dan {@code qrData = userid + ":" + password} -- ditampilkan SEKALI di kotak
 * kredensial dashboard, tidak pernah bisa ditampilkan ulang lewat {@code manajemen_list}.</p>
 * <p><b>TIDAK ADA jalur ubah.</b> Tidak ada sub-aksi {@code manajemen_ubah} di servlet maupun
 * method {@code manajemenUbah} di helper (bandingkan: {@code brand_ubah} dan {@code toko_ubah}
 * ADA). Setelah tersimpan, {@code nama}/{@code jabatan}/{@code userid}/{@code pass} tidak dapat
 * dikoreksi lewat UI mana pun; satu-satunya properti yang masih bisa berubah adalah {@code aktif}
 * (itu pun tanpa tombol, lihat di atas). Salah ketik nama/jabatan, atau kebutuhan reset password,
 * hanya bisa diselesaikan lewat SQL langsung -- atau dgn membuat akun baru dan menonaktifkan yang
 * lama lewat POST manual. Gap yang sudah dicatat sbg <b>G-06</b>/§4 di
 * {@code docs/pendaftaran-tenant/01-source-audit.md} ("HANYA list+tambah").</p>
 * <p><b>Tidak ada jalur hapus</b> pula: baris hanya bisa dinonaktifkan, tidak pernah dibuang.</p>
 *
 * <h3>Kredensial yang belum punya pintu masuk</h3>
 * <p>Fakta yang perlu diketahui sebelum menilai risikonya: <b>belum ada satu pun kode yang
 * MEM-VERIFIKASI kredensial ini</b>. Tidak ada servlet/filter/helper login yang membaca
 * {@code akun_manajemen} atau memanggil {@link #getPass()} untuk mencocokkan password (verifikasi:
 * hanya {@code PendaftarDashboardHelper} yang menyentuh kelas ini, dan di sana {@code getPass()}
 * tidak pernah dipanggil sama sekali -- hanya {@code setPass()}). Jadi tahap ini entity berfungsi
 * sbg <i>penampung kredensial yang dicetak lebih dulu</i>: akun sudah dibuatkan userid+password
 * (dan QR-nya), tapi modul Manajemen yang akan menerimanya belum ada. Ini konsisten dgn kalimat
 * pembuka "pemetaan ke modul/hak-akses spesifik menyusul".</p>
 *
 * <h3>Pemetaan Hibernate &amp; kejutan yang perlu diketahui</h3>
 * <ul>
 *   <li><b>Access type PROPERTY</b> -- {@code @Id} ditempel di {@link #getId()}, jadi Hibernate
 *   membaca/menulis SEMUA properti lewat getter, bukan field. Akibat konkretnya: normalisasi yang
 *   dilakukan DI DALAM getter ikut TERSIMPAN ke DB pada flush berikutnya. Lihat {@link #getNama()}
 *   (trim) dan {@link #getAktif()} ({@code null} menjadi {@code true}).</li>
 *   <li><b>{@link GeneralValueObject} bukan {@code @Entity}/{@code @MappedSuperclass}</b> -- ia POJO
 *   abstrak biasa, sehingga properti induknya ({@code oleh}, {@code olehId}, {@code id},
 *   {@code tanggal_dirubah}, dst) TIDAK dipetakan Hibernate. Karena itu {@code id} dan
 *   {@code tanggal_dirubah} sengaja dideklarasikan ULANG di kelas ini: itu KEHARUSAN TEKNIS, bukan
 *   duplikasi ceroboh. Perhatikan entity ini <i>tidak</i> mendeklarasikan ulang {@code oleh}/
 *   {@code olehId} spt kebanyakan entity AIS lama, jadi tabel {@code public.akun_manajemen} tidak
 *   punya kolom "diubah oleh siapa" -- jejak pelaku hanya ada di revisi Envers.</li>
 *   <li><b>{@code @Audited} (Envers)</b> -- setiap insert/update/delete menyalin SELURUH properti
 *   terpetakan ke tabel audit {@code new_audit.akun_manajemen__audit} (suffix {@code __audit},
 *   {@code default_schema=new_audit} di {@code hibernate.cfg.xml}); {@code
 *   org.hibernate.envers.store_data_at_delete=true} membuat salinan itu TETAP ADA setelah baris
 *   aslinya dihapus. <b>Kolom {@code pass} plaintext ikut tersalin di setiap revisi</b> -- artinya
 *   password yang bocor tidak cuma satu salinan di {@code public}, tapi satu salinan per revisi di
 *   skema audit yang tidak pernah dibersihkan. Konsekuensi operasional lain: bila kolom baru
 *   ditambahkan ke kelas ini tanpa menyinkronkan tabel {@code *__audit}, INSERT audit gagal dan
 *   flush ikut rollback (lihat CATATAN ENVERS di {@code hibernate.cfg.xml} baris 40-60).</li>
 *   <li><b>{@code dynamicInsert}/{@code dynamicUpdate}</b> -- SQL dibangun hanya untuk kolom yang
 *   benar-benar terisi/berubah.</li>
 *   <li><b>Tidak ikut cutover skema tenant</b> -- {@code TenantDataPlaneService} hanya punya
 *   {@code mirrorBrand}/{@code mirrorToko}/{@code mirrorPedagang}. Data akun manajemen tetap
 *   tinggal di skema {@code public} bersama seluruh tenant, tidak disalin ke skema per-tenant.
 *   Karena itu {@code manajemenList()} juga tidak punya cabang {@code sumberData =
 *   "tenant-schema"} spt {@code brandList()}.</li>
 *   <li><b>Pola getter berulang</b> (diverifikasi dari kode kelas ini, bukan diasumsikan dari file
 *   lain): SATU getter menulis balik ke field-nya sendiri, yaitu {@link #getPendaftar()}
 *   ({@code pendaftar = check(pendaftar)}) -- resolusi proxy lazy, bukan mutasi data bisnis, dan
 *   tidak menyentuh DB kecuali tahap terakhir {@code check()} membuka session sendiri lalu
 *   menutupnya di {@code finally}. TIDAK ADA getter destruktif (tidak ada yang menghapus/
 *   mengosongkan baris), dan TIDAK ADA getter yang menutup session Hibernate milik pemanggil di
 *   kelas ini. {@link #getNama()} dan {@link #getAktif()} menormalisasi nilai kembalian TANPA
 *   menyentuh field (efek penyimpanannya datang dari access type PROPERTY, bukan dari write-back
 *   eksplisit).</li>
 * </ul>
 *
 * <h3>Catatan keamanan -- KONFIRMASI ULANG {@code pass} PLAINTEXT (dicatat apa adanya)</h3>
 * <p>Klaim "password plaintext" pada entity ini <b>diverifikasi ulang secara independen dari
 * kode</b>, bukan diambil dari laporan sesi sebelumnya. Rantai buktinya:</p>
 * <ol>
 *   <li>{@code PendaftarDashboardHelper.manajemenTambah()} membangkitkan
 *   {@code String password = buatPasswordAcak(8)} -- sebuah {@code StringBuilder} berisi 8 karakter
 *   mentah dari {@code KARAKTER_PASSWORD}, tanpa hash/salt/KDF apa pun;</li>
 *   <li>nilai {@code String} yang SAMA langsung diteruskan {@code a.setPass(password)} lalu
 *   {@code session.save(a)} -- tidak ada transformasi di antaranya, dan {@link #setPass(String)}
 *   di kelas ini murni penugasan field;</li>
 *   <li>kolomnya dipetakan {@code @Column(name = "pass", nullable = false, length = 100)} -- lebar
 *   100 char, tidak ada {@code columnDefinition} khusus; bandingkan panjang hash PBKDF2 yang
 *   dipakai {@link Pendaftar} (jalur pendaftaran mandiri publik, lihat
 *   {@code PendaftarPublicHelper#hashPassword}) -- di sini tidak ada padanannya;</li>
 *   <li>respons JSON {@code manajemen_tambah} mengirim balik {@code hasil.put("password",
 *   password)} dan {@code qrData = userid + ":" + password}, yang hanya masuk akal bila nilai yang
 *   tersimpan memang nilai yang sama (kalau di-hash, QR tidak akan pernah cocok);</li>
 *   <li>tidak ada kode mana pun yang mem-verifikasi password ini, jadi juga tidak ada tempat lain
 *   yang bisa diam-diam melakukan hashing.</li>
 * </ol>
 * <p><b>Kesimpulan: benar, {@code public.akun_manajemen.pass} berisi password plaintext yang bisa
 * langsung dipakai, bukan ciphertext/hash.</b> Ini keputusan sadar yang konsisten dgn
 * {@code inventory.Pedagang} dan {@link Investor} (agar QR-login cukup meng-encode
 * {@code userid:password}), tapi konsekuensinya: pembacaan tabel APA PUN membocorkan kredensial
 * siap pakai, bukan sekadar hash yang masih harus dipecahkan.</p>
 * <p><b>Jalur kebocoran yang terkonfirmasi</b> (bukan di kelas ini, tapi menjangkau kelas ini):
 * endpoint generik {@code /Api} aksi {@code dataRinci}
 * ({@code ais.action.servlet.api.ElearningApiUtil#dataRinci}, terdaftar di
 * {@code ApiRouteRegistry} baris 152) melakukan {@code Class.forName(<nama kelas dari klien>)}
 * TANPA allow-list, {@code session.createCriteria(clazz).add(Restrictions.idEq(id))} dgn
 * {@code id} sembarang dari klien, lalu {@code Common.insertProperty(...)} menyalin SEMUA properti
 * ke JSON. Rantai {@code Common.insertProperty} -&gt; {@code ais.common.ManajemenProperty} tidak
 * punya satu pun daftar-hitam nama properti sensitif (verifikasi: {@code grep -n
 * "pass|password|sandi"} pada {@code ManajemenProperty.java} nihil; parameter varargs
 * {@code pengecualian} memang ada, tapi {@code dataRinci} memanggil overload TANPA mengisinya).
 * Kelas ini terpetakan Hibernate, jadi terjangkau dari sana -- gerbangnya cuma "token login APA
 * SAJA" ({@code ApiUtil.currentUser}, cukup {@code Tbmuser} mahasiswa/siswa/penduduk mana pun),
 * padahal data yang keluar adalah kredensial tenant ebisnis.id yang sama sekali tidak berhubungan
 * dgn si pemegang token. Ini instance yang memperkuat task keamanan {@code /Api dataRinci} yang
 * sudah ada. Jalur dashboard sendiri sudah benar tidak pernah mengirim {@code pass}; kebocoran ada
 * di endpoint generiknya.</p>
 * <p>Catatan pelengkap, dicatat apa adanya:</p>
 * <ul>
 *   <li>Aksi dashboard ebisnis.id belum memakai token CSRF (dicatat di
 *   {@code docs/pendaftaran-tenant/01-source-audit.md} §"dashboard_ebisnis.jsp"). Karena
 *   {@code manajemen_tambah} membuat akun login, CSRF di sini berarti "penyerang bisa memaksa
 *   tenant mencetak akun manajemen baru" -- meski ia tidak melihat responsnya.</li>
 *   <li>Keunikan {@code userid} dijaga dua lapis yang tidak setara: constraint {@code unique} hanya
 *   per-tabel, sedangkan pengecekan lintas-tabel {@code useridDipakai()} bersifat best-effort
 *   (cek-lalu-simpan, bukan atomik) dan hanya menyapu {@code koperasi.pedagang} +
 *   {@link Investor} + tabel ini.</li>
 *   <li>{@code jabatan} adalah teks bebas yang BELUM mengendalikan apa pun. Jangan sekali-kali
 *   menjadikannya dasar otorisasi tanpa lebih dulu menormalisasinya ke referensi peran yang
 *   tervalidasi -- saat ini nilainya sepenuhnya masukan klien.</li>
 *   <li>Tidak ada kuota jumlah akun manajemen per tenant; {@code manajemen_tambah} bisa dipanggil
 *   berulang tanpa batas selama tenant READY/ACTIVE.</li>
 * </ul>
 *
 * @see Pendaftar
 * @see Investor
 * @see Brand
 * @see ais.database.model.GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "akun_manajemen")
public class AkunManajemen extends GeneralValueObject {

	/** Versi serialisasi Java; entity ini ikut diserialkan lewat {@link GeneralValueObject}. */
	private static final long serialVersionUID = 1L;

	/** Primary key {@code public.akun_manajemen.id}, IDENTITY (di-generate DB). */
	private Long id;

	/**
	 * Nama pemegang akun manajemen (orang). Wajib non-kosong (divalidasi di
	 * {@code PendaftarDashboardHelper.manajemenTambah()}); nilai yang tersimpan selalu ter-trim,
	 * lihat {@link #getNama()}. Juga menjadi basis slug {@code userid} ({@code "mgr-" + nama}).
	 */
	private String nama;

	/**
	 * Label jabatan/modul bebas (mis. "HRD", "Finance", "Logistik"). Opsional, maksimal 100 char,
	 * TIDAK divalidasi terhadap daftar modul mana pun dan BELUM mengendalikan hak akses apa pun --
	 * murni teks tampilan tahap ini.
	 */
	private String jabatan;

	/**
	 * Pemilik bisnis (tenant ebisnis.id) yang memiliki baris ini. Wajib ({@code nullable = false});
	 * dasar seluruh penyaringan IDOR-safe di {@code PendaftarDashboardHelper}.
	 */
	private Pendaftar pendaftar;

	/**
	 * Userid login, di-generate {@code mgr-<slug nama>-<4 digit acak>} oleh
	 * {@code PendaftarDashboardHelper.buatUseridUnik()}. Unik per tabel ({@code unique = true});
	 * keunikan lintas tabel {@code Pedagang}/{@link Investor} hanya best-effort.
	 */
	private String userid;

	/**
	 * Password login <b>plaintext</b> 8 karakter acak (bukan hash, bukan ciphertext). Lihat blok
	 * "KONFIRMASI ULANG {@code pass} PLAINTEXT" di JavaDoc kelas untuk rantai buktinya dan jalur
	 * kebocorannya lewat {@code /Api dataRinci}.
	 */
	private String pass;

	/**
	 * Flag aktif/nonaktif akun. {@code null} diperlakukan -- dan pada flush berikutnya ikut
	 * tersimpan -- sbg {@code true}; lihat {@link #getAktif()}.
	 */
	private Boolean aktif;

	/**
	 * Waktu pembuatan baris, diisi sekali oleh
	 * {@code PendaftarDashboardHelper.manajemenTambah()} dan tidak pernah diperbarui sesudahnya.
	 */
	private Date dibuatPada;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JavaBeans. Seluruh properti diisi lewat
	 * setter oleh {@code PendaftarDashboardHelper.manajemenTambah()}; tidak ada nilai default yang
	 * dipasang di sini kecuali {@link #tanggal_dirubah} (lihat deklarasi field-nya di bawah).
	 */
	public AkunManajemen() {
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil Hibernate tepat sebelum UPDATE baris ini
	 * di-flush, dan meneruskan ke {@code AuditTimestampInterceptor.ubah(this)} yang memperbarui
	 * cap waktu/pelaku audit standar AIS.
	 *
	 * <p>Tidak pernah dipanggil manual dari kode aplikasi. Karena satu-satunya jalur UPDATE untuk
	 * entity ini adalah {@code manajemenNonaktif()}, callback ini praktis hanya aktif saat flag
	 * {@link #getAktif()} dibalik.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Cap waktu perubahan terakhir. Dideklarasikan ULANG di sini (bukan diwarisi) karena
	 * {@link GeneralValueObject} bukan {@code @MappedSuperclass} sehingga properti induknya tidak
	 * dipetakan Hibernate -- keharusan teknis, bukan duplikasi. Diinisialisasi ke waktu sekarang
	 * saat object dibuat, lalu diperbarui {@link #onUpdate()} pada tiap UPDATE.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan cap waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return cap waktu perubahan terakhir ({@code TIMESTAMP})
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return primary key baris ({@code null} bila belum tersimpan)
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key. Normalnya hanya diisi Hibernate setelah INSERT.
	 *
	 * @param id primary key baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama pemegang akun dalam bentuk sudah ter-trim.
	 *
	 * <p><b>Efek non-obvious:</b> karena access type entity ini PROPERTY ({@code @Id} di getter),
	 * Hibernate membaca nilai lewat method ini saat dirty-check/flush -- jadi hasil {@code trim()}
	 * inilah yang benar-benar TERSIMPAN ke kolom {@code nama}, meski field {@link #nama} sendiri
	 * tidak diubah oleh getter ini (bukan write-back ke field).</p>
	 *
	 * @return nama ter-trim, atau {@code null} bila field belum diisi
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menetapkan nama pemegang akun (disimpan apa adanya; trim terjadi di {@link #getNama()}).
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return label jabatan/modul bebas, atau {@code null}/kosong bila tidak diisi. Ingat: nilai
	 *         ini belum mengendalikan hak akses apa pun dan berasal langsung dari masukan klien.
	 */
	@Column(name = "jabatan", nullable = true, length = 100)
	public String getJabatan() {
		return jabatan;
	}

	/**
	 * Menetapkan label jabatan/modul.
	 *
	 * @param jabatan teks bebas, maksimal 100 karakter
	 */
	public void setJabatan(String jabatan) {
		this.jabatan = jabatan;
	}

	/**
	 * Mengembalikan pemilik bisnis (tenant) yang memiliki akun ini.
	 *
	 * <p><b>Menulis balik ke field:</b> memanggil {@code check(pendaftar)} milik
	 * {@link GeneralValueObject} dan menyimpan hasilnya kembali ke {@link #pendaftar}. Ini
	 * resolusi proxy lazy ({@code @ManyToOne(fetch = LAZY)}) supaya pemanggil tidak terkena
	 * {@code LazyInitializationException} saat object sudah detached -- bukan mutasi data bisnis.
	 * Pada kasus umum murni in-memory (identity map/cache); hanya pada tahap terakhir
	 * {@code check()} membuka {@code SessionFactory.openSession()} sendiri lalu menutupnya di
	 * {@code finally} -- session milik pemanggil TIDAK ikut ditutup.</p>
	 *
	 * <p>Dipanggil antara lain dari {@code PendaftarDashboardHelper.manajemenNonaktif()} untuk
	 * memverifikasi kepemilikan baris sebelum mengubahnya.</p>
	 *
	 * @return entity {@link Pendaftar} pemilik (tidak pernah {@code null} untuk baris tersimpan,
	 *         karena kolomnya {@code nullable = false})
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * Menetapkan pemilik bisnis. {@code manajemenTambah()} mengisi ini dgn
	 * {@code session.load(Pendaftar.class, <id dari sesi server>)}, bukan dgn id dari klien.
	 *
	 * @param pendaftar tenant pemilik baris
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * @return userid login akun ini (dikirim ke dashboard lewat {@code manajemen_list})
	 */
	@Column(name = "userid", unique = true, nullable = false, length = 100)
	public String getUserid() {
		return userid;
	}

	/**
	 * Menetapkan userid login. Normalnya hanya diisi hasil
	 * {@code PendaftarDashboardHelper.buatUseridUnik()}; tidak ada jalur UI yang mengubahnya
	 * setelah baris tercipta.
	 *
	 * @param userid userid baru
	 */
	public void setUserid(String userid) {
		this.userid = userid;
	}

	/**
	 * Mengembalikan password login <b>dalam bentuk plaintext</b> (lihat blok konfirmasi di JavaDoc
	 * kelas).
	 *
	 * <p><b>Peringatan:</b> method ini tidak pernah dipanggil satu pun kode aplikasi saat ini
	 * (belum ada mekanisme login yang memverifikasi akun manajemen). Yang justru memanggilnya
	 * adalah serialisasi refleksi generik: {@code Common.insertProperty} /
	 * {@code ais.common.ManajemenProperty} menyapu seluruh properti terpetakan tanpa daftar-hitam,
	 * sehingga endpoint {@code /Api} aksi {@code dataRinci} mengeluarkan nilai ini apa adanya ke
	 * JSON bagi pemegang token login apa pun. Jangan menambahkan pemanggil baru tanpa lebih dulu
	 * mempertimbangkan hal ini.</p>
	 *
	 * @return password plaintext, atau {@code null} bila belum diisi
	 */
	@Column(name = "pass", nullable = false, length = 100)
	public String getPass() {
		return pass;
	}

	/**
	 * Menetapkan password login. Nilai disimpan <b>apa adanya, tanpa hashing/enkripsi</b>;
	 * satu-satunya pemanggil, {@code PendaftarDashboardHelper.manajemenTambah()}, meneruskan
	 * langsung hasil {@code buatPasswordAcak(8)}.
	 *
	 * @param pass password plaintext
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/**
	 * Mengembalikan status aktif akun, dgn {@code null} dinormalisasi menjadi {@code true}.
	 *
	 * <p><b>Dua hal non-obvious.</b> (1) Default-nya <i>fail-open</i>: baris yang kolom
	 * {@code aktif}-nya NULL (mis. disisipkan lewat SQL langsung) dianggap AKTIF, bukan nonaktif.
	 * (2) Karena access type PROPERTY, nilai {@code true} hasil normalisasi ini ikut TERSIMPAN ke
	 * DB pada flush berikutnya -- membaca baris ber-NULL lalu menyimpannya diam-diam mengubah NULL
	 * menjadi {@code true}.</p>
	 *
	 * <p>Perhatikan properti ini SATU-SATUNYA di kelas ini yang tidak diberi anotasi
	 * {@code @Column} eksplisit; Hibernate memakai nama kolom default hasil turunan nama properti
	 * ({@code aktif}), jadi tetap terpetakan -- inkonsistensi gaya, bukan bug. Sama persis dgn
	 * {@link Investor#getAktif()}.</p>
	 *
	 * @return {@code true} bila akun aktif (termasuk saat nilainya {@code null})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan status aktif akun. Dipanggil {@code manajemenTambah()} ({@code true}) dan
	 * {@code manajemenNonaktif()} (hasil {@code "true".equals(request.optString("aktif","false"))},
	 * jadi nilai selain string {@code "true"} berarti nonaktif).
	 *
	 * @param aktif status baru; {@code null} akan dibaca kembali sbg {@code true} oleh
	 *              {@link #getAktif()}
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return waktu pembuatan baris, atau {@code null} untuk baris yang disisipkan di luar
	 *         {@code manajemenTambah()}
	 */
	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	/**
	 * Menetapkan waktu pembuatan baris. Hanya diisi sekali saat akun dibuat
	 * ({@code WaktuUtil.getDate()}); tidak ada jalur yang memperbaruinya.
	 *
	 * @param dibuatPada waktu pembuatan
	 */
	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	/**
	 * Representasi teks ringkas {@code "<id>-<nama>"}.
	 *
	 * <p>Membaca field {@link #nama} LANGSUNG (bukan lewat {@link #getNama()}), jadi hasilnya
	 * tidak ter-trim -- perbedaan halus yang tampak bila nama tersimpan berspasi di ujung. Aman
	 * dari sisi kebocoran: {@code pass} tidak ikut dicetak.</p>
	 *
	 * @return {@code "<id>-<nama>"}; kedua bagian bisa berbunyi {@code null} bila belum diisi
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
