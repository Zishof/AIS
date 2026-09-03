package ais.database.model.sekolah;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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

import ais.database.model.Bank;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;

/**
 * Master <b>Cara Pembayaran</b> siswa &mdash; satu baris = satu cara/kanal yang boleh dipakai untuk
 * menerima uang siswa pada satu {@link Sekolah}, lengkap dengan pemetaan ke akun buku besar
 * (jurnal) yang akan didebet/dikredit ketika pembayaran diposting.
 *
 * <p>Tabel: {@code sekolah.akun_pembayaran_siswa}. Layar pengelolanya
 * {@code ais.action.master.sekolah.AkunPembayaranSiswaAction} (ZUL
 * {@code /WEB-INF/z/x/y/pages/master/sekolah/akun_pembayaran_siswa.zul}) memberi judul dialog
 * <i>"Tambah Cara Pembayaran"</i>/<i>"Ubah Cara Pembayaran"</i> &mdash; itulah nama bisnis yang
 * dipakai pengguna, sedangkan nama kelas ({@code AkunPembayaranSiswa}) berasal dari sudut pandang
 * akuntansi.</p>
 *
 * <h2>Yang BUKAN entity ini (koreksi dugaan yang sering muncul)</h2>
 * <p>Kelas ini <b>bukan</b> nomor rekening virtual (Virtual Account) milik siswa, dan bukan pula
 * rekening/saldo per siswa. Tidak ada relasi ke {@code Siswa} maupun {@code CalonSiswa} di sini,
 * tidak ada kolom nomor VA, dan tidak ada kolom saldo. Nomor VA per transaksi disimpan di
 * {@link ais.database.model.VirtualAccountBank} (properti {@code kode}), sedangkan saldo/tabungan
 * siswa disimpan di {@link DepositSiswa}. Hubungan keduanya justru terbalik dari dugaan: <b>mereka
 * yang menunjuk ke entity ini</b>, lewat kolom FK {@code akun_pembayaran_siswa_id}. Jumlah baris
 * tabel ini berskala "beberapa per sekolah", bukan "satu per siswa".</p>
 *
 * <h2>Posisi dalam rantai billing siswa</h2>
 * <p>Rantai tagihan-ke-uang di modul sekolah berjalan seperti ini:</p>
 * <ol>
 *   <li>{@link JenisBiayaSekolah} &rarr; {@link ItemBiayaSekolah} menentukan <i>jenis</i> biaya;</li>
 *   <li>{@link ais.database.model.sekolah.NominalBiaya} memateri tarif itu menjadi kewajiban rupiah
 *       per siswa/periode;</li>
 *   <li>{@link ais.database.model.sekolah.Tagihan} adalah baris kewajiban yang harus dilunasi;</li>
 *   <li>{@link PembayaranSiswa} + {@link PembayaranSiswaDetail} mencatat pelunasannya, dan
 *       {@link DepositSiswa} mencatat penambahan saldo/tabungan;</li>
 *   <li><b>entity ini</b> menjawab pertanyaan terakhir: <i>uangnya masuk lewat cara apa, dan
 *       masuk ke akun buku besar yang mana</i>.</li>
 * </ol>
 * <p>Karena itu entity ini tidak pernah menjadi sumber angka rupiah &mdash; nominalnya selalu
 * datang dari {@code Tagihan}/{@code VirtualAccountBank}. Perannya murni sebagai <i>pemetaan</i>:
 * label cara bayar + akun kas/bank + akun deposit + bank penampung + pemilik tenant.</p>
 *
 * <h2>Pemakaian yang terverifikasi</h2>
 * <ul>
 *   <li><b>Jurnal akuntansi.</b> {@code ais.database.model.akunting.GrupTransaksi
 *       .tampilkanJurnalPembayaranSiswa(...)} memakai {@link #getAkun()} sebagai akun
 *       <i>debet</i> (kas/bank bertambah) dan {@link #getAkunDeposit()} sebagai akun
 *       <i>kredit</i> untuk porsi tambahan deposit/tabungan. Bila salah satunya kosong, jurnal
 *       tetap dibuat tetapi disertai peringatan "Akun cara pembayaran siswa belum diatur" /
 *       "Akun deposit/tabungan siswa belum diatur" &mdash; jadi kolom {@code akun_id} yang lupa
 *       diisi berujung pada jurnal timpang, bukan pada penolakan transaksi.</li>
 *   <li><b>Callback bank host-to-host.</b> {@code Bniresponse}, {@code Bsiresponse},
 *       {@code Briresponse} memilih (atau membuat, lihat catatan di bawah) baris entity ini lalu
 *       menempelkannya ke {@code PembayaranSiswa}/{@code DepositSiswa} hasil callback.</li>
 *   <li><b>API pembayaran.</b> {@code ais.action.servlet.api.TagihanSiswa} dan
 *       {@code ais.action.servlet.api.TopupHelper} mencari baris "otomatis" ({@code manual=false},
 *       aktif) milik sekolah siswa yang bersangkutan.</li>
 *   <li><b>Layar pembayaran.</b> {@code PembayaranOnline} dan {@code WizardPembayaranSiswaHelper}
 *       merender satu tombol "Bayar via &lt;nama&gt;" per baris yang lolos filter, sehingga
 *       {@link #getNama()} langsung menjadi teks tombol.</li>
 *   <li><b>Laporan.</b> {@code LaporanRincianPembayaranSiswa} dan kerabatnya memakai
 *       {@link #getNama()} sebagai kolom "cara"/"via".</li>
 * </ul>
 *
 * <h2>Arti tiga flag pemilih (paling penting untuk dipahami)</h2>
 * <p>Tidak ada satu pun kode yang memilih baris berdasarkan id; semuanya memilih berdasarkan
 * kombinasi {@link #getManual()}, {@link #getDariTabungan()}, dan {@link #getAktif()}. Pola yang
 * terverifikasi:</p>
 * <ul>
 *   <li><b>Kanal otomatis (VA/bank/online):</b> {@code manual = false} dan aktif. Dipakai
 *       {@code VirtualAccountBank.bayarSiswa}, {@code TagihanSiswa}, {@code TopupHelper},
 *       {@code Bniresponse} dkk sebagai <i>fallback</i> ketika VA tidak membawa akun sendiri.</li>
 *   <li><b>Potong tabungan/deposit:</b> {@code dariTabungan = true}. Dicari lebih dulu oleh
 *       {@code VirtualAccountBank.bayarSiswaLangsung} sebelum jatuh ke fallback otomatis.</li>
 *   <li><b>Tunai di loket kasir:</b> {@code dariTabungan = true} <b>atau</b> {@code manual = true}
 *       (lihat {@code PembayaranOnline} pada blok tombol "Bayar via ..." untuk pengguna staf).</li>
 * </ul>
 * <p><b>Konsekuensi non-obvious:</b> {@code VirtualAccountBank.getAkunPembayaranSiswa()}
 * <i>mengosongkan</i> relasinya sendiri bila akun yang tertaut ternyata {@code dariTabungan} atau
 * {@code manual}. Artinya: mencentang "Manual" atau "Tabungan" pada baris yang sudah dipakai
 * banyak VA berjalan bukan sekadar mengubah pilihan di layar &mdash; VA-VA itu akan berperilaku
 * seolah tidak punya akun sama sekali dan jatuh ke pencarian fallback.</p>
 *
 * <h2>Kolom, penamaan, dan pemetaan</h2>
 * <p>Access type Hibernate adalah <b>property</b> (anotasi {@code @Id} berada pada getter), jadi
 * seluruh getter publik yang punya pasangan setter ikut dipetakan walau tanpa {@code @Column} —
 * termasuk {@code aktif}, {@code keterangan}, {@code manual}, {@code dariTabungan}, dan
 * {@code defaultPembayaran}. Strategi penamaan instalasi ini adalah
 * {@code ais.database.hibernate.MyNamingStrategy}, turunan {@code DefaultNamingStrategy}, sehingga
 * <b>nama kolom = nama properti apa adanya</b> (tanpa konversi camelCase &rarr; snake_case):
 * kolomnya benar-benar bernama {@code dariTabungan} dan {@code defaultPembayaran}. Satu-satunya
 * pengecualian eksplisit adalah {@code nama} yang dipetakan ke kolom {@code nama_pembayaran}.</p>
 * <p>Karena access type property, nilai yang ditulis ke {@code INSERT}/{@code UPDATE} adalah nilai
 * yang <b>dikembalikan getter</b>, bukan isi field mentah. Untuk tiga getter boolean di bawah yang
 * melakukan coalesce {@code null &rarr; true/false}, itu berarti nilai coalesced-lah yang benar-benar
 * tersimpan ketika baris disimpan lewat Hibernate. Baris yang masuk lewat SQL mentah/migrasi tetap
 * bisa berisi {@code NULL} dan menimbulkan efek yang dijelaskan pada {@link #getAktif()}.</p>
 *
 * <h2>Field yang dideklarasikan ulang dari {@link GeneralValueObject}</h2>
 * <p>{@code id}, {@code oleh}, {@code olehId}, dan {@code tanggal_dirubah} dideklarasikan ulang di
 * kelas ini walau namanya sama dengan milik induk. Ini <b>bukan duplikasi yang keliru</b>:
 * {@link GeneralValueObject} adalah POJO abstrak biasa &mdash; bukan {@code @Entity} maupun
 * {@code @MappedSuperclass} &mdash; sehingga Hibernate tidak memetakan properti apa pun dari sana.
 * Setiap entity yang ingin punya kolom audit tersebut wajib mendeklarasikannya sendiri.</p>
 *
 * <h2>Getter yang menulis balik</h2>
 * <p>Seperti kebanyakan entity AIS, sebagian getter di sini bukan getter murni:</p>
 * <ul>
 *   <li>{@link #getAkun()}, {@link #getAkunDeposit()}, {@link #getBank()}, {@link #getSekolah()}
 *       memanggil {@link GeneralValueObject#check(Object)} lalu <b>menugaskan ulang</b> hasilnya ke
 *       field (resolusi proxy lazy). Efeknya biasanya jinak.</li>
 *   <li>{@link #getYayasan()} lebih jauh dari itu: ia <b>menimpa</b> {@code yayasan} dengan
 *       {@code sekolah.getYayasan()} setiap kali dipanggil selama {@code sekolah} tidak null.
 *       Kolom {@code yayasan_id} praktis menjadi nilai turunan, dan nilai apa pun yang pernah
 *       ditulis manual ke sana akan tergantikan pada flush berikutnya.</li>
 * </ul>
 * <p>Tidak ada getter di kelas ini yang menghapus data finansial (tidak ada kolom rupiah sama
 * sekali di sini) — kontras dengan {@code NominalBiaya.getNominal()} atau
 * {@code VirtualAccountBank.getAkunPembayaranSiswa()}.</p>
 *
 * <h2>Catatan integritas &amp; hal-hal yang perlu diwaspadai</h2>
 * <ul>
 *   <li><b>Pemilihan fallback tidak deterministik.</b> Seluruh query fallback berbentuk
 *       {@code ...setMaxResults(1)} <b>tanpa</b> {@code addOrder(...)}. Bila satu sekolah punya
 *       lebih dari satu baris aktif non-manual, akun buku besar mana yang dipakai untuk mencatat
 *       uang masuk ditentukan oleh urutan baris yang kebetulan dikembalikan database.</li>
 *   <li><b>Callback bank dapat membuat baris master baru.</b> {@code Bniresponse},
 *       {@code Bsiresponse}, dan {@code Briresponse} akan {@code session.save(...)} sebuah baris
 *       baru bernama mis. {@code "bayar via BNI"} bila konfigurasi {@code kode_akun_bni} terisi
 *       tetapi belum ada baris yang cocok. Baris hasil auto-buat itu tidak punya {@code bank},
 *       tidak punya {@code keterangan}, dan seluruh flag-nya memakai nilai coalesced
 *       ({@code aktif=true}, {@code manual=false}) sehingga langsung ikut menjadi kandidat
 *       fallback bagi seluruh kanal lain.</li>
 *   <li><b>{@code defaultPembayaran} adalah flag mati.</b> Checkbox "Default" di grid menulis
 *       kolomnya, tetapi tidak ada satu pun query/kode di repo yang pernah membaca
 *       {@link #getDefaultPembayaran()} milik entity ini. Lihat javadoc getter tersebut.</li>
 *   <li><b>Biaya administrasi dikunci pada id.</b> {@code PembayaranOnline} mengambil biaya admin
 *       per cara bayar lewat kunci konfigurasi {@code "<id>_biaya_administrasi"}. Karena kuncinya
 *       memakai id sekuensial baris ini, menghapus lalu membuat ulang sebuah cara bayar akan
 *       memutus tarif admin yang sudah tersimpan (kuncinya jadi lain).</li>
 *   <li>{@code @Audited}: perubahan baris ini terekam Envers, jadi ada jejak audit untuk perubahan
 *       pemetaan akun. Perlu diingat bahwa operasi SQL mentah di jalur lain (mis. penghapusan
 *       {@code pembayaran_siswa} pada {@code Bniresponse}) tetap tidak terekam.</li>
 * </ul>
 *
 * <h2>Pengelompokan anggota kelas</h2>
 * <ol>
 *   <li><b>Audit warisan:</b> {@link #getOleh()}/{@link #setOleh(String)},
 *       {@link #getOlehId()}/{@link #setOlehId(String)},
 *       {@link #getTanggal_dirubah()}/{@link #setTanggal_dirubah(Date)}, {@link #onUpdate()}.</li>
 *   <li><b>Identitas:</b> {@link #getId()}/{@link #setId(Long)}.</li>
 *   <li><b>Pemetaan akuntansi:</b> {@link #getAkun()}, {@link #getAkunDeposit()},
 *       {@link #getBank()} beserta setter-nya.</li>
 *   <li><b>Cakupan tenant:</b> {@link #getSekolah()}, {@link #getYayasan()} beserta setter-nya.</li>
 *   <li><b>Label:</b> {@link #getNama()}, {@link #getKeterangan()}.</li>
 *   <li><b>Flag perilaku:</b> {@link #getAktif()}, {@link #getManual()},
 *       {@link #getDariTabungan()}, {@link #getDefaultPembayaran()}.</li>
 * </ol>
 *
 * @see GeneralValueObject
 * @see ais.database.model.sekolah.NominalBiaya
 * @see ais.database.model.sekolah.Tagihan
 * @see PembayaranSiswa
 * @see DepositSiswa
 * @see ais.database.model.VirtualAccountBank
 * @see Akun
 * @see Bank
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "akun_pembayaran_siswa", schema = "sekolah")
public class AkunPembayaranSiswa extends GeneralValueObject {

	/**
	 * Versi serialisasi Java. Nilainya hasil generate dan tidak boleh diubah selama struktur
	 * kelas masih kompatibel, karena instance entity ikut diserialisasi ZK ke dalam state
	 * desktop/session.
	 */
	private static final long serialVersionUID = 1536673007848946857L;

	/** Kunci utama; lihat {@link #getId()}. */
	private Long id;

	/** Nama pengguna terakhir yang mengubah baris; lihat {@link #getOleh()}. */
	private String oleh;

	/** Id pengguna terakhir yang mengubah baris; lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Diisi otomatis oleh {@code ais.database.hibernate.AuditTimestampInterceptor} melalui
	 * {@link #onUpdate()}; bukan data yang diisi pengguna lewat form.</p>
	 *
	 * @return id pengguna, atau {@code null} bila baris belum pernah diubah lewat jalur ber-audit
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir.
	 *
	 * <p><b>Non-obvious:</b> nilai {@code null} maupun string kosong/spasi <b>diabaikan diam-diam</b>
	 * (method langsung {@code return} tanpa menulis apa pun). Jejak audit lama karena itu tidak
	 * bisa dihapus dengan menyetel nilai kosong &mdash; sifat ini disengaja agar interceptor yang
	 * gagal menentukan pengguna tidak menghapus jejak sebelumnya.</p>
	 *
	 * @param olehId id pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir.
	 *
	 * <p>Sama seperti {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam
	 * sehingga jejak audit sebelumnya tidak tertimpa nilai kosong.</p>
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Ditampilkan lewat {@code RevisiHelper} pada kolom pertama grid layar Cara Pembayaran,
	 * berdampingan dengan riwayat Envers.</p>
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA yang dijalankan tepat sebelum baris ini di-{@code UPDATE}.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)}, yang mengisi
	 * {@link #setOleh(String)}, {@link #setOlehId(String)}, dan
	 * {@link #setTanggal_dirubah(Date)} dari konteks pengguna yang sedang aktif.</p>
	 *
	 * <p><b>Efek samping:</b> memutasi tiga properti audit pada instance ini. Hanya dipanggil
	 * Hibernate; jangan dipanggil manual dari kode aplikasi. Perhatikan bahwa hook ini adalah
	 * {@code @PreUpdate} saja &mdash; tidak ada {@code @PrePersist}, sehingga baris yang baru
	 * dibuat (termasuk yang dibuat otomatis oleh callback bank) tidak langsung punya jejak
	 * "oleh" sampai pertama kali diubah.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Waktu perubahan terakhir; lihat {@link #getTanggal_dirubah()}. Diinisialisasi ke waktu
	 * server saat instance dibuat sehingga baris baru tidak pernah punya stempel kosong.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * <p>Umumnya diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}; pengisian
	 * manual hanya wajar pada skrip migrasi/perbaikan data.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru (boleh {@code null})
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * <p>Dipetakan sebagai {@code TIMESTAMP} ke kolom {@code tanggal_dirubah} (nama kolom sama
	 * dengan nama properti karena {@code MyNamingStrategy} tidak mengonversi penamaan).</p>
	 *
	 * @return waktu perubahan terakhir; tidak pernah {@code null} untuk instance yang dibuat lewat
	 *         konstruktor karena field diinisialisasi ke waktu server
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Akun kas/bank buku besar yang didebet saat pembayaran diposting; lihat {@link #getAkun()}. */
	private Akun akun;

	/** Akun deposit/tabungan buku besar yang dikredit; lihat {@link #getAkunDeposit()}. */
	private Akun akunDeposit;

	/** Bank penampung (opsional); lihat {@link #getBank()}. */
	private Bank bank;

	/** Sekolah pemilik baris (kunci tenant); lihat {@link #getSekolah()}. */
	private Sekolah sekolah;

	/** Yayasan pemilik baris; nilai turunan dari {@code sekolah}, lihat {@link #getYayasan()}. */
	private Yayasan yayasan;

	/** Label cara pembayaran yang tampil ke pengguna; lihat {@link #getNama()}. */
	private String nama;

	/** Catatan bebas; lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Penanda cara bayar yang dicatat manual oleh petugas; lihat {@link #getManual()}. */
	private Boolean manual;

	/** Penanda cara bayar yang memotong saldo/tabungan siswa; lihat {@link #getDariTabungan()}. */
	private Boolean dariTabungan;

	/** Penanda baris masih boleh dipakai; lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Penanda "default" yang tidak pernah dibaca kode mana pun; lihat {@link #getDefaultPembayaran()}. */
	private Boolean defaultPembayaran;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA.
	 *
	 * <p>Juga dipakai langsung oleh {@code AkunPembayaranSiswaAction.onAdd(...)} untuk menyiapkan
	 * baris baru pada dialog "Tambah Cara Pembayaran", dan oleh callback bank
	 * ({@code Bniresponse}/{@code Bsiresponse}/{@code Briresponse}) ketika mereka membuat baris
	 * "bayar via &lt;bank&gt;" secara otomatis. Seluruh properti dibiarkan {@code null} kecuali
	 * {@link #getTanggal_dirubah()} yang diinisialisasi ke waktu server.</p>
	 */
	public AkunPembayaranSiswa() {
	}

	/**
	 * Kunci utama baris ini.
	 *
	 * <p>Dihasilkan database ({@code IDENTITY}); kolomnya ditandai {@code insertable = false}
	 * sehingga Hibernate tidak pernah mengirim nilai id pada {@code INSERT}.</p>
	 *
	 * <p><b>Non-obvious:</b> id sekuensial ini bocor keluar sebagai bagian dari kunci konfigurasi
	 * biaya administrasi ({@code "<id>_biaya_administrasi"}, dipakai {@code PembayaranOnline}).
	 * Nilainya juga menjadi tie-breaker implisit pada {@code Bniresponse} yang memakai
	 * {@code addOrder(Order.desc("id"))} untuk memilih baris terbaru yang kodenya cocok.</p>
	 *
	 * @return id baris, atau {@code null} untuk instance yang belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci utama.
	 *
	 * <p>Hanya dipakai Hibernate saat memuat/menyimpan baris. Menyetel id secara manual pada
	 * instance yang sudah terkelola session akan membingungkan Hibernate.</p>
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Akun buku besar <b>kas/bank</b> yang akan <b>didebet</b> ketika pembayaran lewat cara ini
	 * diposting.
	 *
	 * <p>Inilah kolom terpenting entity ini secara akuntansi. {@code GrupTransaksi
	 * .tampilkanJurnalPembayaranSiswa(...)} memakainya sebagai sisi debet jurnal; bila kosong,
	 * jurnal tetap dibentuk tetapi diberi peringatan <i>"Akun cara pembayaran siswa belum
	 * diatur"</i> sehingga uang masuk tidak punya lawan akun yang benar.</p>
	 *
	 * <p>Di layar pengelola, kolom ini wajib diisi ({@code onSave} menolak simpan bila picker
	 * {@code AmbilDataAkunBanbox} kosong), tetapi pada tingkat database kolomnya
	 * {@code nullable = true} &mdash; baris yang dibuat lewat jalur lain (callback bank, SQL
	 * mentah, unggah massal) tidak melewati validasi tersebut.</p>
	 *
	 * <p><b>Efek samping:</b> memanggil {@link GeneralValueObject#check(Object)} dan menugaskan
	 * ulang hasilnya ke field, yaitu resolusi proxy lazy. Tidak menghapus data.</p>
	 *
	 * @return akun kas/bank, atau {@code null} bila belum diatur
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_id", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/**
	 * Menyetel akun kas/bank buku besar.
	 *
	 * <p>Dipanggil {@code AkunPembayaranSiswaAction.onSave(...)} dari atribut {@code "akun"} milik
	 * picker {@code AmbilDataAkunBanbox}, dan oleh {@code Bniresponse} dkk ketika membuat baris
	 * otomatis dari akun yang kodenya cocok dengan konfigurasi {@code kode_akun_<bank>}.</p>
	 *
	 * <p>Berbeda dari {@link #setSekolah(Sekolah)}, setter ini <b>tidak</b> menolak object
	 * transient; menyimpan {@code Akun} yang belum punya id akan ikut mem-persist akun tersebut
	 * karena relasi memakai {@code CascadeType.PERSIST}.</p>
	 *
	 * @param akun akun kas/bank tujuan (boleh {@code null})
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Sekolah pemilik baris ini &mdash; kunci pemisah tenant.
	 *
	 * <p>Hampir seluruh pencarian cara pembayaran di codebase menyaring dengan
	 * {@code Restrictions.eq("sekolah", sekolah)}, di mana {@code sekolah} diambil dari siswa /
	 * calon siswa yang sedang bertransaksi. Dengan begitu satu instalasi multi-sekolah tetap
	 * memakai akun kas masing-masing.</p>
	 *
	 * <p><b>Perhatian:</b> kolomnya tidak ditandai {@code nullable = false}. Baris dengan
	 * {@code sekolah_id} kosong tidak akan pernah cocok dengan filter di atas &mdash; jadi baris
	 * seperti itu bukan "berlaku untuk semua sekolah", melainkan praktis tidak terpakai sama
	 * sekali oleh jalur pembayaran, walau tetap tampil di layar daftar.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return sekolah pemilik, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sekolah_id")
	public Sekolah getSekolah() {
		sekolah = check(sekolah);
		return this.sekolah;
	}

	/**
	 * Menyetel sekolah pemilik baris.
	 *
	 * <p><b>Non-obvious:</b> object {@code Sekolah} yang belum punya id (transient, mis. hasil
	 * {@code new Sekolah()} dari combo kosong) <b>dibuang menjadi {@code null}</b>, bukan
	 * disimpan. Penjagaan ini mencegah {@code CascadeType.PERSIST} membuat baris {@code Sekolah}
	 * hantu hanya karena combo tenant tidak terpilih. Konsekuensinya, pemanggil yang mengira
	 * sudah menyetel sekolah bisa mendapati kolom tenant tetap kosong tanpa pesan apa pun.</p>
	 *
	 * @param sekolah sekolah pemilik; {@code null} atau object tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setSekolah(Sekolah sekolah) {
		this.sekolah = sekolah == null || sekolah.getId() == null ? null : sekolah;
	}

	/**
	 * Yayasan pemilik baris ini &mdash; tingkat tenant di atas {@link Sekolah}.
	 *
	 * <p><b>Getter yang menulis balik.</b> Selama {@code sekolah} tidak {@code null}, field
	 * {@code yayasan} <b>selalu ditimpa</b> dengan {@code sekolah.getYayasan()} setiap kali method
	 * ini dipanggil. Kolom {@code yayasan_id} karena itu efektif merupakan nilai turunan: apa pun
	 * yang pernah ditulis ke sana lewat {@link #setYayasan(Yayasan)} atau lewat SQL akan
	 * tergantikan pada pembacaan berikutnya, dan (karena access type property) nilai turunan
	 * itulah yang ikut ter-{@code UPDATE} ke database pada flush.</p>
	 *
	 * <p>Sifat ini juga menjelaskan mengapa combo "Yayasan" pada dialog tambah/ubah dibuat
	 * {@code readonly} &mdash; nilainya toh ditentukan oleh sekolah yang dipilih. Karena
	 * kolomnya tetap disimpan (bukan {@code @Transient}), filter "Yayasan" di layar daftar
	 * tetap bisa bekerja tanpa join tambahan.</p>
	 *
	 * <p>Bila {@code sekolah} {@code null}, nilai field dipertahankan apa adanya dan hanya
	 * di-resolve proxy-nya.</p>
	 *
	 * @return yayasan pemilik (turunan dari sekolah bila ada), atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "yayasan_id")
	public Yayasan getYayasan() {
		if (sekolah != null) {
			yayasan = sekolah.getYayasan();
		}
		yayasan = check(yayasan);
		return this.yayasan;
	}

	/**
	 * Menyetel yayasan pemilik baris.
	 *
	 * <p>Sama seperti {@link #setSekolah(Sekolah)}, object transient (tanpa id) dibuang menjadi
	 * {@code null} agar cascade tidak melahirkan baris {@code Yayasan} hantu.</p>
	 *
	 * <p><b>Efek nyatanya terbatas:</b> nilai yang disetel di sini akan ditimpa lagi oleh
	 * {@link #getYayasan()} pada pembacaan pertama selama {@code sekolah} terisi. Jadi setter ini
	 * hanya benar-benar menentukan hasil untuk baris yang tidak punya sekolah.</p>
	 *
	 * @param yayasan yayasan pemilik; {@code null} atau object tanpa id disimpan sebagai
	 *                {@code null}
	 */
	public void setYayasan(Yayasan yayasan) {
		this.yayasan = yayasan == null || yayasan.getId() == null ? null : yayasan;
	}

	/**
	 * Label cara pembayaran yang dilihat pengguna, mis. {@code "Tunai Loket"},
	 * {@code "Transfer BNI"}, {@code "Potong Tabungan"}.
	 *
	 * <p>Dipetakan ke kolom {@code nama_pembayaran} (satu-satunya properti di kelas ini yang nama
	 * kolomnya berbeda dari nama propertinya) dan ditandai {@code nullable = false}.</p>
	 *
	 * <p>Nilai ini bukan sekadar label internal &mdash; ia dipakai langsung sebagai:</p>
	 * <ul>
	 *   <li>teks tombol pembayaran pada {@code PembayaranOnline}/{@code WizardPembayaranSiswaHelper}
	 *       ({@code "Bayar via " + getNama()});</li>
	 *   <li>kolom "cara"/"via" pada laporan rincian pembayaran siswa;</li>
	 *   <li>field {@code "jenis"} pada respons JSON host-to-host bank
	 *       ({@code ais.action.servlet.Va} dan {@code ais.action.servlet.Mandiri}).</li>
	 * </ul>
	 * <p>Karena itu perubahan nama berdampak langsung ke tampilan siswa dan ke payload yang
	 * dikirim ke bank.</p>
	 *
	 * @return label cara pembayaran
	 */
	@Column(name = "nama_pembayaran", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menyetel label cara pembayaran.
	 *
	 * <p>Divalidasi tidak-boleh-kosong oleh {@code AkunPembayaranSiswaAction.onSave(...)} (pesan
	 * peringatan pada layar berbunyi "Nama Cara Sekolah harus diisi"). Jalur non-UI tidak
	 * memvalidasi apa pun kecuali batasan {@code NOT NULL} database.</p>
	 *
	 * @param nama label cara pembayaran
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Bank penampung untuk cara pembayaran ini (opsional).
	 *
	 * <p>Merujuk katalog {@link Bank} global instalasi. Pada dialog tambah/ubah, combo ini
	 * menyertakan pilihan {@code "== Jenis Pembayaran Bukan Transfer =="} yang bernilai
	 * {@code null} &mdash; itulah cara menyatakan "tunai / bukan transfer".</p>
	 *
	 * <p>Kolom ini bersifat informatif: tidak ada logika pemilihan kanal yang menyaring
	 * berdasarkan {@code bank}. Pemilihan akun untuk callback BNI/BSI/BRI justru dilakukan lewat
	 * pencocokan <b>kode akun</b> ({@code kode_akun_bni} dsb. terhadap {@code akun.kode}), bukan
	 * lewat relasi ini. Akibatnya baris yang dibuat otomatis oleh callback bank selalu punya
	 * {@code bank_id} kosong walau namanya "bayar via BNI".</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return bank penampung, atau {@code null} bila cara pembayaran ini bukan transfer
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "bank_id")
	public Bank getBank() {
		bank = check(bank);
		return bank;
	}

	/**
	 * Menyetel bank penampung.
	 *
	 * @param bank bank penampung, atau {@code null} untuk cara pembayaran bukan transfer
	 */
	public void setBank(Bank bank) {
		this.bank = bank;
	}

	/**
	 * Apakah cara pembayaran ini masih boleh dipakai.
	 *
	 * <p><b>Coalesce {@code null} &rarr; {@code true}:</b> baris yang kolom {@code aktif}-nya
	 * kosong dianggap aktif. Karena access type Hibernate adalah property, nilai coalesced inilah
	 * yang benar-benar ditulis ke database saat baris disimpan lewat Hibernate &mdash; jadi baris
	 * hasil {@code session.save(...)} tidak akan meninggalkan {@code NULL} di kolom ini.</p>
	 *
	 * <p><b>Namun {@code NULL} tetap mungkin</b> untuk baris yang masuk lewat SQL mentah, restore,
	 * atau migrasi. Untuk baris seperti itu ada perbedaan perlakuan yang perlu diketahui:</p>
	 * <ul>
	 *   <li>Seluruh jalur <b>pembayaran</b> menyaring dengan
	 *       {@code Restrictions.or(isNull("aktif"), eq("aktif", true))} &mdash; baris ber-{@code NULL}
	 *       tetap <b>ikut dipakai</b>.</li>
	 *   <li>Layar <b>daftar</b> ({@code AkunPembayaranSiswaAction.initCriteria}) menyaring dengan
	 *       {@code eq("aktif", true)} atau {@code eq("aktif", false)} saja, tanpa cabang
	 *       {@code isNull}. Baris ber-{@code NULL} karena itu <b>tidak muncul pada kedua mode
	 *       filter</b> &mdash; tidak terlihat saat checkbox "Tampilkan hanya yang aktif" dicentang
	 *       maupun saat dilepas.</li>
	 * </ul>
	 * <p>Gabungan keduanya berarti sebuah cara pembayaran bisa aktif melayani uang siswa tanpa
	 * pernah tampil di layar administrasinya. Ini catatan apa adanya dari kode, bukan klaim bahwa
	 * kondisi tersebut pasti terjadi pada instalasi tertentu.</p>
	 *
	 * @return {@code true} bila aktif (termasuk ketika kolomnya {@code NULL})
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel status aktif.
	 *
	 * <p>Ditulis dari checkbox "Aktif" pada grid layar daftar. Checkbox tersebut di-{@code disable}
	 * bila pengguna tidak punya hak {@code UPDATE}, dan setiap centang langsung memicu
	 * {@code Common.refreshSaveOrUpdate(...)} &mdash; perubahan tersimpan seketika tanpa tombol
	 * Simpan.</p>
	 *
	 * @param aktif status aktif baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Penanda "cara pembayaran default".
	 *
	 * <p><b>Flag mati / tidak pernah dibaca.</b> Penelusuran seluruh repo tidak menemukan satu pun
	 * pembaca: tidak ada query yang menyaring {@code AkunPembayaranSiswa} dengan
	 * {@code Restrictions.eq("defaultPembayaran", ...)}, dan tidak ada kode yang memanggil getter
	 * ini selain renderer grid yang menampilkan checkbox-nya sendiri. (Properti bernama sama pada
	 * {@code JenisPembayaran}, {@code CaraPembayaranTransfer}, dan {@code JenisKegiatan}
	 * <i>memang</i> punya pembaca &mdash; jangan tertukar; entity-entity itu milik modul perguruan
	 * tinggi/akuntansi, bukan modul sekolah.)</p>
	 *
	 * <p>Praktisnya: mencentang "Default" pada layar Cara Pembayaran menyimpan nilai ke database
	 * tetapi tidak mengubah perilaku apa pun. Pemilihan cara bayar tetap ditentukan kombinasi
	 * {@link #getManual()}/{@link #getDariTabungan()}/{@link #getAktif()} plus
	 * {@code setMaxResults(1)} tanpa pengurutan. Dicatat apa adanya; jangan diandalkan sebagai
	 * mekanisme untuk "mengunci" akun kas tertentu.</p>
	 *
	 * <p>Coalesce {@code null} &rarr; {@code false}.</p>
	 *
	 * @return {@code true} bila ditandai default; {@code false} bila kolom kosong
	 */
	public Boolean getDefaultPembayaran() {
		return defaultPembayaran == null ? false : defaultPembayaran;
	}

	/**
	 * Menyetel penanda "default".
	 *
	 * <p>Ditulis dari checkbox "Default" pada grid dan langsung disimpan lewat
	 * {@code Common.refreshSaveOrUpdate(...)}. Lihat {@link #getDefaultPembayaran()}: nilainya
	 * tidak dibaca kode mana pun.</p>
	 *
	 * @param defaultPembayaran penanda default baru
	 */
	public void setDefaultPembayaran(Boolean defaultPembayaran) {
		this.defaultPembayaran = defaultPembayaran;
	}

	/**
	 * Catatan bebas untuk cara pembayaran ini.
	 *
	 * <p>Diisi lewat textarea 3 baris pada dialog tambah/ubah dan ditampilkan sebagai satu kolom
	 * di grid. Tidak dipakai logika bisnis apa pun.</p>
	 *
	 * <p>Berbeda dari beberapa entity katalog lain di modul sekolah, properti ini <b>benar-benar
	 * dipetakan</b> ke kolom {@code keterangan} (punya pasangan setter dan tidak
	 * {@code @Transient}), jadi isiannya bertahan antar request.</p>
	 *
	 * @return catatan bebas, atau {@code null}
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menyetel catatan bebas.
	 *
	 * @param keterangan catatan bebas (boleh {@code null}/kosong)
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Akun buku besar <b>deposit/tabungan siswa</b> yang akan <b>dikredit</b> ketika ada setoran
	 * yang menambah saldo, bukan melunasi tagihan.
	 *
	 * <p>Dipakai {@code GrupTransaksi.tampilkanJurnalPembayaranSiswa(...)} khusus untuk porsi
	 * {@code tambahanDeposit}: akun kas ({@link #getAkun()}) didebet sebesar tambahan deposit dan
	 * akun ini dikredit sebesar nilai yang sama, sehingga saldo titipan siswa tercatat sebagai
	 * kewajiban sekolah. Bila kosong, jurnal diberi peringatan <i>"Akun deposit/tabungan siswa
	 * belum diatur"</i>.</p>
	 *
	 * <p>Opsional pada UI maupun database ({@code nullable = true}) &mdash; cara pembayaran yang
	 * tidak pernah menerima setoran saldo boleh membiarkannya kosong.</p>
	 *
	 * <p><b>Efek samping:</b> resolusi proxy lazy lewat {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return akun deposit/tabungan, atau {@code null} bila belum diatur
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_deposit_id", nullable = true)
	public Akun getAkunDeposit() {
		akunDeposit = check(akunDeposit);
		return akunDeposit;
	}

	/**
	 * Menyetel akun deposit/tabungan buku besar.
	 *
	 * <p>Diisi {@code AkunPembayaranSiswaAction.onSave(...)} dari atribut {@code "akun"} picker
	 * {@code AmbilDataAkunBanbox} kedua pada dialog. Tidak divalidasi wajib.</p>
	 *
	 * @param akunDeposit akun deposit/tabungan (boleh {@code null})
	 */
	public void setAkunDeposit(Akun akunDeposit) {
		this.akunDeposit = akunDeposit;
	}

	/**
	 * Apakah cara pembayaran ini dicatat <b>manual</b> oleh petugas (bukan hasil callback
	 * otomatis dari bank).
	 *
	 * <p>Flag ini adalah pembeda utama antara kanal otomatis dan kanal loket:</p>
	 * <ul>
	 *   <li>Seluruh pencarian akun untuk <b>uang masuk otomatis</b> (callback VA BNI/BSI/BRI,
	 *       {@code VirtualAccountBank.bayarSiswa}, API {@code TagihanSiswa}, {@code TopupHelper})
	 *       menambahkan {@code Restrictions.eq("manual", false)} &mdash; baris manual sengaja
	 *       dikecualikan.</li>
	 *   <li>Sebaliknya {@code PembayaranOnline} merender tombol bayar tunai untuk baris yang
	 *       {@code manual = true} <i>atau</i> {@code dariTabungan = true}, dan hanya untuk
	 *       pengguna staf (akun yang tidak terkait siswa/calon siswa/orang tua/mahasiswa).</li>
	 *   <li>{@code VirtualAccountBank.getAkunPembayaranSiswa()} bahkan <b>mengosongkan</b>
	 *       relasinya bila akun yang tertaut ternyata manual &mdash; sebuah VA tidak boleh
	 *       bermuara ke akun loket.</li>
	 * </ul>
	 * <p>Karena filter otomatis memakai {@code eq("manual", false)} yang ketat (bukan
	 * {@code or(isNull, eq(false))} seperti pada {@code aktif}), baris dengan kolom
	 * {@code manual} bernilai {@code NULL} di database tidak akan pernah terpilih sebagai akun
	 * kanal otomatis. Untuk baris yang disimpan lewat Hibernate hal ini tidak terjadi, karena
	 * coalesce di bawah membuat nilai {@code false} yang tertulis ke kolom.</p>
	 *
	 * <p>Coalesce {@code null} &rarr; {@code false}.</p>
	 *
	 * @return {@code true} bila cara pembayaran ini dicatat manual oleh petugas
	 */
	public Boolean getManual() {
		return manual == null ? false : manual;
	}

	/**
	 * Menyetel penanda pencatatan manual.
	 *
	 * <p>Ditulis dari checkbox "Manual" pada grid dan tersimpan seketika lewat
	 * {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * <p><b>Dampak yang mudah terlewat:</b> mengaktifkan flag ini pada baris yang sudah dipakai
	 * VA berjalan membuat VA-VA tersebut kehilangan akunnya (lihat {@link #getManual()}), dan
	 * mengeluarkan baris ini dari daftar kandidat akun kanal otomatis. Jurnal untuk pembayaran
	 * berikutnya akan memakai akun fallback lain milik sekolah yang sama.</p>
	 *
	 * @param manual penanda manual baru
	 */
	public void setManual(Boolean manual) {
		this.manual = manual;
	}

	/**
	 * Apakah cara pembayaran ini <b>memotong saldo/tabungan siswa</b> alih-alih menerima uang
	 * baru.
	 *
	 * <p>Dipakai sebagai prioritas pertama oleh {@code VirtualAccountBank.bayarSiswaLangsung}:
	 * akun dengan {@code dariTabungan = true} milik sekolah yang bersangkutan dicari lebih dulu,
	 * dan hanya bila tidak ada barulah jatuh ke akun otomatis ({@code manual = false}).</p>
	 *
	 * <p>Pada {@code PembayaranOnline}, tombol untuk baris {@code dariTabungan} hanya dirender bila
	 * saldo tabungan siswa memang tersedia ({@code tabungan > 0.1}), dan pembayarannya ditolak
	 * bila total tagihan ditambah biaya administrasi melebihi saldo.</p>
	 *
	 * <p>Sama seperti {@link #getManual()}, flag ini juga membuat
	 * {@code VirtualAccountBank.getAkunPembayaranSiswa()} mengosongkan relasinya &mdash; setoran
	 * VA nyata tidak boleh dicatat sebagai potongan tabungan.</p>
	 *
	 * <p>Kolomnya bernama {@code dariTabungan} apa adanya (bukan {@code dari_tabungan}) karena
	 * {@code MyNamingStrategy} tidak mengonversi camelCase.</p>
	 *
	 * <p>Coalesce {@code null} &rarr; {@code false}.</p>
	 *
	 * @return {@code true} bila cara pembayaran ini memotong tabungan/deposit siswa
	 */
	public Boolean getDariTabungan() {
		return dariTabungan == null ? false : dariTabungan;
	}

	/**
	 * Menyetel penanda "potong tabungan".
	 *
	 * <p>Ditulis dari checkbox "Tabungan" pada grid dan tersimpan seketika lewat
	 * {@code Common.refreshSaveOrUpdate(...)}.</p>
	 *
	 * <p><b>Dampak yang mudah terlewat:</b> sama dengan {@link #setManual(Boolean)} &mdash;
	 * mengaktifkan flag ini memutus baris dari seluruh kanal otomatis dan membuat VA yang sudah
	 * menunjuk ke sini kehilangan akunnya, sekaligus menjadikan baris ini kandidat utama untuk
	 * pembayaran potong-tabungan seluruh sekolah tersebut.</p>
	 *
	 * @param dariTabungan penanda potong-tabungan baru
	 */
	public void setDariTabungan(Boolean dariTabungan) {
		this.dariTabungan = dariTabungan;
	}
}
