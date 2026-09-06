package ais.database.model.koperasi;

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
import ais.database.model.akunting.Akun;
import ais.database.model.sekolah.KanalPembayaran;

/**
 * Master METODE PEMBAYARAN yang boleh dipakai di modul koperasi/kantin -- mis. "Tunai", "Transfer",
 * "QRIS", "Kasbon Divisi", "Voucher" -- satu baris di sini menjadi satu opsi yang muncul di layar
 * kasir POS Kantin/Koperasi (lihat {@code PosApi.prosesCaraBayarList},
 * {@code KantinHelper.caraBayarListSemua}) maupun di form transaksi lain yang membaca
 * {@link ais.database.model.koperasi.TransaksiKoperasi#getCaraPembayaranKoperasi()}.
 *
 * <p><b>Peran ganda: pilihan UI sekaligus konfigurasi akuntansi.</b> Selain nama yang tampil ke
 * kasir, baris ini membawa {@link #getAkun()} -- akun kas/bank COA yang menjadi lawan jurnal saat
 * mesin posting mencatat penerimaan/pembatalan. Lihat khususnya Javadoc
 * {@link ais.database.model.koperasi.PembatalanTransaksiKantin#getPostingHistory()}: saat "Pembatalan
 * Penjualan Kantin" menjurnal-balik transaksi yang sudah terposting, akun kas lawannya TIDAK dicari
 * lewat relasi FK tersimpan, melainkan lewat QUERY ULANG {@code Restrictions.eq("nama", ...)}
 * terhadap potret teks {@code PembatalanTransaksiKantin.caraPembayaran} (lihat
 * {@code PembatalanTransaksiUtil.postingSemua}). Konsekuensinya: mengubah {@link #getNama()} baris
 * yang sudah pernah dipakai bertransaksi berisiko memutus pencarian itu untuk arsip pembatalan lama
 * (arsip lama tetap dilewati dengan aman -- {@code akunKas == null} membuatnya tetap draf, TIDAK
 * memposting jurnal salah -- tetapi jurnal balik yang seharusnya jalan jadi tidak pernah terbentuk).</p>
 *
 * <p><b>Kenapa pencarian per-nama itu aman:</b> {@code nama} DIPAKSA UNIK secara GLOBAL (lintas
 * SEMUA koperasi/tenant) oleh validasi UI
 * {@code CaraPembayaranKoperasiAction.checkNamaCaraPembayaranKoperasi()} -- query ceknya sendiri
 * tidak memfilter {@code koperasi}, jadi dua koperasi berbeda TIDAK BOLEH memakai nama metode yang
 * sama persis. Ini bukan constraint database (tidak ada {@code UNIQUE} di kolom {@code nama}),
 * murni ditegakkan di lapisan aksi ZK -- baris yang masuk lewat jalur lain (impor data, skrip, API
 * yang melewati action ini) tidak otomatis tunduk pada aturan itu.</p>
 *
 * <p><b>Field {@link #getKoperasi()} bersifat DEKORATIF, bukan filter yang benar-benar
 * ditegakkan.</b> Layar admin ("Berlaku untuk koperasi") mengisinya, tetapi SELURUH titik baca yang
 * ditelusuri (POS checkout {@code PosApi.prosesCaraBayarList}, daftar admin
 * {@code KantinHelper.caraBayarListAdmin}, lookup reversal di atas) hanya menyaring
 * {@code aktif = true} -- tidak satu pun menambahkan {@code Restrictions.eq("koperasi", ...)}. Jadi
 * SEMUA koperasi pada instalasi yang sama berbagi satu daftar metode pembayaran aktif secara efektif;
 * kolom ini baru bermakna bila ada kode pemanggil BARU yang secara eksplisit menyaringnya -- jangan
 * berasumsi mengisi field ini otomatis menyembunyikan baris dari koperasi lain.</p>
 *
 * <p>Sejumlah sifat metode (Tunai vs non-Tunai, wajib verifikasi manual, memotong deposit anggota,
 * tercatat sebagai piutang, wajib pelanggan dipilih) memakai pola DEFAULT-DARI-NAMA/NULL-TRI-STATE
 * yang konsisten di seluruh kelas ini -- lihat Javadoc masing-masing getter
 * ({@link #getOnline()}, {@link #getAdaKembalian()}, {@link #getMemotongDeposit()},
 * {@link #getMasukSebagaiHutang()}, {@link #getWajibPilihMember()}/{@link #wajibPilihMemberEfektif()})
 * untuk rasionalnya masing-masing -- semuanya sengaja dirancang agar baris data LAMA (kolom baru
 * masih {@code null}) tidak pernah mendadak berubah perilaku saat kolom baru ditambahkan.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "cara_pembayaran_koperasi")
public class CaraPembayaranKoperasi extends GeneralValueObject {

	/** Versi serialisasi tetap -- lihat catatan umum {@link GeneralValueObject} soal kompatibilitas. */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang terakhir mengubah baris ini, atau {@code null} bila belum tercatat. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Sengaja MENGABAIKAN input {@code null}/kosong (early-return tanpa mengubah field) --
	 * pola audit shadow yang berulang di seluruh entity AIS: sekali baris punya {@code olehId},
	 * nilai itu tidak boleh tertimpa "lupa diisi" oleh pemanggil yang tidak melewatkan identitas
	 * pengguna. Ini KEHARUSAN TEKNIS, bukan bug -- lihat catatan serupa di entity lain domain
	 * koperasi/akunting.</p>
	 *
	 * @param olehId id pengguna; nilai {@code null}/kosong diabaikan.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menetapkan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * <p>Pola sama dengan {@link #setOlehId(String)}: input {@code null}/kosong diabaikan supaya
	 * potret nama pengubah tidak pernah tertimpa kosong.</p>
	 *
	 * @param oleh nama pengguna; nilai {@code null}/kosong diabaikan.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return nama pengguna (potret teks) yang terakhir mengubah baris ini, atau {@code null} bila belum tercatat. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil OTOMATIS sesaat sebelum UPDATE dieksekusi,
	 * mendelegasikan pembaruan stempel waktu ke {@code AuditTimestampInterceptor.ubah(this)} --
	 * pola seragam yang dipakai seluruh subclass {@link GeneralValueObject} yang memetakan
	 * {@link #getTanggal_dirubah()} sebagai kolom fisik. Tidak dipanggil manual oleh kode aplikasi.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menetapkan stempel waktu terakhir diubah secara eksplisit.
	 *
	 * <p>Biasanya TIDAK perlu dipanggil manual -- nilai defaultnya diisi saat field diinisialisasi
	 * ({@code WaktuUtil.getDate()} pada construction) dan diperbarui otomatis oleh
	 * {@link #onUpdate()} setiap UPDATE. Setter ini tersedia untuk kasus seperti impor data
	 * historis yang perlu mempertahankan stempel waktu asli.</p>
	 *
	 * @param tanggal_dirubah stempel waktu baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu perubahan terakhir baris ini -- diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap UPDATE, dan oleh inisialisasi field pada saat
	 *         object pertama dibuat (baris baru yang belum pernah di-UPDATE tetap punya nilai
	 *         non-null: waktu construction-nya sendiri).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #getNama()} apa adanya -- dipakai a.l. oleh komponen combobox/kombo ZK yang menampilkan label baris ini. */
	public String toString() {
		return nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private Koperasi koperasi;
	private Akun akun;
	private Boolean manual;
	private KanalPembayaran kanalPembayaran;
	private Boolean aktif;
	private Boolean online;
	private Boolean memotongDeposit;
	private Boolean masukSebagaiHutang;
	private Boolean adaKembalian;
	private Boolean wajibPilihMember;

	/** Constructor kosong -- wajib untuk Hibernate (instansiasi via reflection); pemakai aplikasi mengisi field lewat setter. */
	public CaraPembayaranKoperasi() {
	}

	/**
	 * @return id primer baris ini, {@code null} untuk instance yang belum pernah disimpan
	 *         (belum dapat identity dari sequence/auto-increment kolom {@code id}).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan id baris ini secara manual.
	 *
	 * <p>Kolom dipetakan {@code insertable = false} -- nilai yang di-set di sini TIDAK ikut
	 * dikirim pada statement INSERT (id selalu diisi database via strategi {@code IDENTITY}
	 * setelah baris tersimpan). Setter tetap berguna untuk membentuk referensi entity
	 * "hanya berisi id" (mis. {@code new CaraPembayaranKoperasi(); .setId(x)} sebelum
	 * {@code session.load(...)}) tanpa query tambahan.</p>
	 *
	 * @param id id baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode singkat metode pembayaran (opsional, TIDAK diberi anotasi {@code @Column}
	 *         eksplisit -- dipetakan lewat konvensi nama field Hibernate ke kolom {@code kode}).
	 *         Dipakai a.l. oleh {@link #metodeKasbon()} sebagai salah satu sumber pencocokan kata
	 *         "kasbon", di samping {@link #getNama()}.
	 */
	public String getKode() {
		return kode;
	}

	/** @param kode kode singkat metode pembayaran; boleh {@code null}/kosong. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama metode pembayaran yang tampil ke kasir/admin, DI-TRIM setiap dipanggil (nilai
	 *         mentah di field {@code nama} tidak ikut ditrim saat {@code set}) -- lihat catatan
	 *         penting soal keunikan GLOBAL nama ini di Javadoc kelas
	 *         {@link CaraPembayaranKoperasi}: dipakai sebagai kunci pencarian saat mesin posting
	 *         menjurnal-balik pembatalan transaksi kantin.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama metode pembayaran; disimpan apa adanya (trim terjadi di {@link #getNama()}, bukan di sini). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas tentang metode pembayaran ini, atau {@code null} bila tidak diisi. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan catatan bebas; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menandai metode pembayaran ini boleh dipilih kasir/masih berlaku.
	 *
	 * <p>Default {@code true} bila kolom belum diisi (data lama) -- pola umum flag "aktif" di
	 * kelas entity AIS: baris lama otomatis dianggap aktif tanpa perlu migrasi data eksplisit.
	 * Dipakai sebagai satu-satunya filter tenant/keanggotaan di hampir semua titik baca daftar
	 * cara bayar (lihat catatan {@link #getKoperasi()} soal kolom itu TIDAK ikut menyaring).</p>
	 *
	 * @return {@code true} bila aktif/kolom belum diisi, {@code false} bila dinonaktifkan admin.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baru; {@code null} diperlakukan sama seperti {@code true} oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Akun COA (kas/bank/utang, tergantung sifat metode) yang menjadi <b>lawan jurnal</b> saat
	 * mesin posting mencatat transaksi yang memakai metode pembayaran ini -- inilah field yang
	 * menjembatani "metode pembayaran yang dipilih kasir" ke "baris jurnal akuntansi yang
	 * terbentuk", sehingga menyimpan {@code null} membuat mesin posting yang bergantung padanya
	 * tidak dapat menyusun jurnal (baris transaksi/pembatalan terkait dilewati, tetap draf --
	 * lihat mis. {@code PembatalanTransaksiUtil.postingSemua}: {@code akunKas == null} => arsip
	 * dilewati, TIDAK diposting dengan akun yang salah).
	 *
	 * <p><b>Dua cara berbeda field ini ditemukan oleh pemanggil:</b></p>
	 * <ol>
	 *   <li><b>Via relasi FK langsung</b> -- entity yang MENYIMPAN referensi
	 *   {@code CaraPembayaranKoperasi} sendiri (mis. {@link ais.database.model.koperasi.TransaksiKoperasi})
	 *   memanggil {@code getCaraPembayaranKoperasi().getAkun()} secara langsung -- jalur paling
	 *   aman, selalu konsisten dengan baris yang benar-benar dipakai transaksi tersebut.</li>
	 *   <li><b>Via pencarian ulang per-nama</b> -- entity yang hanya menyimpan POTRET TEKS nama
	 *   cara bayar (mis. {@code PembatalanTransaksiKantin.caraPembayaran}, dibentuk saat arsip
	 *   dibuat, TIDAK diperbarui otomatis bila baris {@code CaraPembayaranKoperasi} sumbernya
	 *   berubah nama kemudian) melakukan {@code Restrictions.eq("nama", ...)} baru untuk
	 *   menemukan baris ini kembali, lalu membaca {@code getAkun()}-nya. Jalur ini BERGANTUNG pada
	 *   {@code nama} tidak pernah berubah setelah dipakai bertransaksi -- lihat Javadoc kelas
	 *   {@link CaraPembayaranKoperasi} untuk penjelasan lengkap kenapa itu aman (nama dijaga unik
	 *   global oleh validasi UI) sekaligus risikonya (rename baris lama memutus pencarian
	 *   historis).</li>
	 * </ol>
	 *
	 * <p>Pola {@code akun = check(akun); return akun;} adalah idiom resolusi proxy lazy standar
	 * seluruh entity AIS -- lihat Javadoc {@link GeneralValueObject#check(Object)} untuk mekanisme
	 * empat-tahap lengkapnya (identity map JVM-wide, cache {@code ConstantValues}, inisialisasi
	 * lewat session yang tersedia, lalu reload via session baru sebagai penyelamat terakhir).
	 * Relasi {@code LAZY} + {@code cascade = {PERSIST, MERGE}}: menyimpan
	 * {@code CaraPembayaranKoperasi} baru/berubah otomatis ikut menyimpan {@link Akun} yang
	 * direferensikan bila akun itu sendiri belum tersimpan/berubah, tetapi TIDAK ada
	 * {@code CascadeType.REMOVE} -- menghapus baris cara bayar tidak pernah menghapus akun COA-nya.</p>
	 *
	 * @return akun COA lawan jurnal untuk metode pembayaran ini, atau {@code null} bila belum
	 *         dikonfigurasi (baris tidak dapat dipakai mesin posting sampai diisi).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

	/** @param akun akun COA lawan jurnal baru untuk metode pembayaran ini; boleh {@code null} (belum dikonfigurasi). */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Koperasi yang menjadi konteks "Berlaku untuk koperasi" pada layar admin cara pembayaran.
	 *
	 * <p><b>PENTING -- field ini bersifat informatif, BUKAN filter tenant yang benar-benar
	 * ditegakkan.</b> Ditelusuri seluruh titik baca daftar cara bayar aktif
	 * ({@code PosApi.prosesCaraBayarList}, {@code KantinHelper.caraBayarListSemua},
	 * {@code KantinHelper.caraBayarListAdmin}) maupun pencarian reversal per-nama di
	 * {@code PembatalanTransaksiUtil}: semuanya HANYA menyaring {@link #getAktif()}, tidak satu
	 * pun menambahkan {@code Restrictions.eq("koperasi", ...)}. Efeknya, mengisi field ini di
	 * layar admin TIDAK menyembunyikan baris dari koperasi/instalasi lain manapun -- seluruh
	 * kasir pada instalasi yang sama tetap melihat dan bisa memilih metode pembayaran ini di POS.
	 * Lihat catatan pola berulang "filter tenant lemah/hilang" pada domain finansial ini; jangan
	 * menambah asumsi baru bahwa field ini sudah menyaring sesuatu tanpa memverifikasi ulang titik
	 * baca terkait bila kode pemanggil baru ditambahkan.</p>
	 *
	 * <p>Pola resolusi lazy {@code check(koperasi)} sama seperti {@link #getAkun()} -- lihat
	 * {@link GeneralValueObject#check(Object)}.</p>
	 *
	 * @return koperasi yang tercatat sebagai konteks baris ini, atau {@code null} bila tidak
	 *         dibatasi eksplisit (nilai bawaan pada form "Semua Koperasi").
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "koperasi", nullable = true)
	public Koperasi getKoperasi() {
		koperasi = check(koperasi);
		return koperasi;
	}

	/** @param koperasi koperasi konteks baris ini; boleh {@code null} -- lihat catatan penting di {@link #getKoperasi()}. */
	public void setKoperasi(Koperasi koperasi) {
		this.koperasi = koperasi;
	}

	/**
	 * Kanal pembayaran (klasifikasi umum lintas modul, mis. Tunai/Transfer Bank/QRIS/E-Wallet --
	 * lihat {@link KanalPembayaran}) yang menaungi metode ini. Dipakai untuk pengelompokan/laporan
	 * lintas cara-bayar tanpa harus mencocokkan teks {@code nama} satu per satu.
	 *
	 * @return kanal pembayaran baris ini, atau {@code null} bila belum diklasifikasikan (meski
	 *         form admin mewajibkan pengisian ini sebelum baris dapat disimpan -- lihat
	 *         {@code CaraPembayaranKoperasiAction.onSave}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanal_pembayaran", nullable = true)
	public KanalPembayaran getKanalPembayaran() {
		kanalPembayaran = check(kanalPembayaran);
		return kanalPembayaran;
	}

	/** @param kanalPembayaran kanal pembayaran baru; boleh {@code null} sebagai nilai field mentah (form mewajibkan pengisian sebelum simpan). */
	public void setKanalPembayaran(KanalPembayaran kanalPembayaran) {
		this.kanalPembayaran = kanalPembayaran;
	}

	/**
	 * Menandai metode ini menuntut VERIFIKASI ADMIN/petugas sebelum dianggap sah (mis. transfer
	 * bank yang bukti bayarnya dicek manual), berbeda dari metode yang otomatis lunas begitu
	 * kasir memasukkan nominal (mis. Tunai/QRIS otomatis).
	 *
	 * <p>Default {@code true} bila belum diisi -- KEBALIKAN dari kebanyakan flag boolean lain di
	 * kelas ini yang defaultnya {@code false}: metode pembayaran lama (era sebelum kolom-kolom
	 * turunan seperti {@link #getMemotongDeposit()} ada) memang secara historis SEMUA dianggap
	 * manual, jadi default {@code true} di sini justru yang menjaga perilaku lama tidak berubah.
	 * Lihat Javadoc {@link #getMemotongDeposit()} untuk penjelasan lengkap kenapa flag ini SENDIRI
	 * tidak lagi cukup menentukan "apakah saldo dipotong" sejak {@code memotongDeposit} ada.</p>
	 *
	 * @return {@code true} bila metode ini perlu verifikasi manual (atau belum diisi), {@code
	 *         false} bila otomatis lunas.
	 */
	public Boolean getManual() {
		return manual == null ? true : manual;
	}

	/** @param manual status manual baru; {@code null} diperlakukan sama seperti {@code true} oleh {@link #getManual()}. */
	public void setManual(Boolean manual) {
		this.manual = manual;
	}

	/**
	 * Menandai metode ini adalah kanal pembayaran ONLINE (mis. payment gateway/virtual account),
	 * dipakai a.l. untuk membedakan alur rekonsiliasi/notifikasi otomatis dari pembayaran fisik di
	 * loket/kasir.
	 *
	 * <p>Default TIDAK statis: bila kolom belum diisi eksplisit, DITURUNKAN dari
	 * {@link #getNama()} mengandung kata "online" (case-insensitive) -- pola yang sama dipakai
	 * {@link #getAdaKembalian()} untuk kata "tunai". Ini menghindari migrasi data manual untuk
	 * baris lama yang namanya sudah mengandung "Online" (mis. "Transfer Online"), sekaligus
	 * membiarkan admin meng-override eksplisit lewat form bila deteksi nama keliru (mis. metode
	 * bernama "Kasbon Online Shop" yang sebetulnya bukan kanal online).</p>
	 *
	 * @return {@code true} bila metode ini kanal online (eksplisit atau tersirat dari nama),
	 *         {@code false} sebaliknya.
	 */
	public Boolean getOnline() {
		return online == null ? (getNama() != null && getNama().toLowerCase().contains("online")) : online;
	}

	/** @param online status online baru; {@code null} mengembalikan getter ke deteksi otomatis dari {@link #getNama()}. */
	public void setOnline(Boolean online) {
		this.online = online;
	}

	/**
	 * Menandai bahwa memakai metode pembayaran ini <b>MEMOTONG saldo/Deposit anggota</b>.
	 *
	 * <p><b>Kenapa perlu, padahal sudah ada {@link #getManual()}.</b> Sebelum kolom ini ada, satu-satunya
	 * penentu pemotongan saldo adalah {@code manual == false} (lihat
	 * {@code DepositHelper.hitungDeposit}). Akibatnya metode pembayaran yang perlu SEKALIGUS
	 * diverifikasi admin <i>dan</i> memotong saldo — mis. "Voucher" yang penukarannya dicek petugas —
	 * tidak mungkin dibuat: menyalakan verifikasi manual otomatis membatalkan pemotongan saldo. Kolom
	 * ini memisahkan dua urusan yang memang berbeda: <i>siapa yang memverifikasi</i> ({@code manual})
	 * dan <i>apakah saldo berkurang</i> ({@code memotongDeposit}).</p>
	 *
	 * <p><b>Cara dipakai:</b> syarat pemotongan menjadi
	 * {@code masukSebagaiHutang != true DAN (manual == false ATAU memotongDeposit == true)}. Metode
	 * piutang tidak boleh sekaligus mengurangi saldo member. Jadi kolom ini bersifat MENAMBAH — seluruh
	 * metode pembayaran lama (yang kolom ini masih {@code null}/{@code false}) berperilaku persis
	 * seperti sebelumnya, tidak ada saldo anggota yang bergeser diam-diam saat fitur ini dipasang.</p>
	 *
	 * <p><b>Default sengaja {@code false}</b> (bukan {@code true} seperti {@code getManual()}): data
	 * lama tidak boleh mendadak ikut memotong saldo.</p>
	 */
	@Column(name = "memotong_deposit")
	public Boolean getMemotongDeposit() {
		return memotongDeposit == null ? false : memotongDeposit;
	}

	public void setMemotongDeposit(Boolean memotongDeposit) {
		this.memotongDeposit = memotongDeposit;
	}

	/**
	 * Menandai bahwa transaksi yang memakai metode pembayaran ini dicatat sebagai <b>HUTANG
	 * pelanggan</b> (piutang toko) -- BUKAN transaksi lunas -- lihat JavaDoc
	 * {@code DepositHelper.hitungHutang}/{@code KantinHelper.SplitPembayaran} soal cara nominalnya
	 * dijumlahkan per slot (pola SAMA PERSIS dgn {@link #getMemotongDeposit()}: dihormati per SLOT
	 * pembayaran, bukan per transaksi keseluruhan -- satu transaksi split separuh Tunai + separuh
	 * "Ambil dari Utang" HANYA separuh nominalnya yang tercatat sbg hutang).
	 *
	 * <p>Batas maksimal hutang yang boleh ditumpuk seorang anggota diatur per
	 * {@link TipeAnggotaKoperasi#getMaksimalBolehUtang()} (kategori referensi sivitasnya), BUKAN di
	 * sini -- kolom ini murni menandai METODE pembayarannya, gerbang limitnya ada di sisi member.</p>
	 *
	 * <p>Default sengaja {@code false}: metode pembayaran lama (kolom ini masih null/false) TIDAK
	 * pernah mendadak dianggap hutang.</p>
	 */
	@Column(name = "masuk_sebagai_hutang")
	public Boolean getMasukSebagaiHutang() {
		return masukSebagaiHutang == null ? false : masukSebagaiHutang;
	}

	public void setMasukSebagaiHutang(Boolean masukSebagaiHutang) {
		this.masukSebagaiHutang = masukSebagaiHutang;
	}

	/**
	 * Menandai bahwa metode pembayaran ini MEMUNGKINKAN kembalian (uang diterima kasir boleh LEBIH
	 * dari total, sisanya dikembalikan) -- pola cara bayar fisik spt Tunai. {@code false} berarti
	 * pembayaran WAJIB pas (uang diterima harus SAMA PERSIS dgn total, tanpa kembalian) -- cocok utk
	 * metode non-tunai spt QRIS/Transfer/Voucher/Kasbon yang jumlahnya dipotong persis sesuai total,
	 * bukan "dibayar lebih lalu dikembalikan".
	 *
	 * <p>Default TIDAK statis -- mengikuti pola {@link #getOnline()}: kalau belum diisi eksplisit
	 * (data lama/baru), dihitung dari nama metode mengandung kata "tunai" (case-insensitive). Ini
	 * menghindari migrasi data manual utk baris cara-bayar yang sudah ada -- baris lama bernama
	 * "Tunai" otomatis dapat {@code true}, baris lain otomatis {@code false}, admin tetap bisa
	 * override eksplisit lewat form.</p>
	 */
	@Column(name = "ada_kembalian")
	public Boolean getAdaKembalian() {
		return adaKembalian == null ? (getNama() != null && getNama().toLowerCase().contains("tunai")) : adaKembalian;
	}

	public void setAdaKembalian(Boolean adaKembalian) {
		this.adaKembalian = adaKembalian;
	}

	/**
	 * Metode ini menuntut nama pelanggan/anggota dipilih sebelum transaksi ditulis.
	 *
	 * <p>Nilai MENTAH: {@code null} berarti "ikut aturan bawaan" -- lihat
	 * {@link #wajibPilihMemberEfektif()}. Dibedakan dari {@code FALSE} secara sengaja,
	 * supaya metode yang belum pernah disentuh admin tidak terkunci pada jawaban
	 * yang kebetulan berlaku hari ini.</p>
	 */
	@Column(name = "wajib_pilih_member")
	public Boolean getWajibPilihMember() {
		return wajibPilihMember;
	}

	public void setWajibPilihMember(Boolean wajibPilihMember) {
		this.wajibPilihMember = wajibPilihMember;
	}

	/**
	 * Aturan yang benar-benar ditegakkan saat pembayaran.
	 *
	 * <p>Bila admin belum menentukan, jawabannya diturunkan dari sifat metode itu
	 * sendiri: metode yang MASUK SEBAGAI HUTANG atau MEMOTONG SALDO pada dasarnya
	 * memang tidak bermakna tanpa pemilik -- hutang tanpa pemilik tidak bisa ditagih,
	 * dan saldo tanpa pemilik tidak bisa dipotong dari siapa pun. Penurunan ini
	 * membuat perilaku hari ini tidak berubah sedikit pun ketika kolomnya
	 * ditambahkan.</p>
	 *
	 * <p>Semua metode yang kode/namanya mengandung "Kasbon" wajib mempunyai member.
	 * Kasbon adalah piutang customer; istilah Divisi/Operasional hanya menjelaskan
	 * tujuan tagihannya, sedangkan member menjadi customer/PJ/PIC pada sub-ledger.</p>
	 */
	public boolean wajibPilihMemberEfektif() {
		if (metodeKasbon()) {
			return true;
		}
		if (wajibPilihMember != null) {
			return wajibPilihMember.booleanValue();
		}
		return Boolean.TRUE.equals(getMasukSebagaiHutang()) || Boolean.TRUE.equals(getMemotongDeposit());
	}

	/** True untuk seluruh varian Kasbon, termasuk kode lama tanpa spasi/underscore. */
	public boolean metodeKasbon() {
		String identitas = ((getKode() == null ? "" : getKode()) + " "
				+ (getNama() == null ? "" : getNama())).toLowerCase(java.util.Locale.ENGLISH)
				.replace('_', ' ').replace('-', ' ');
		String ringkas = identitas.replace(" ", "");
		return ringkas.indexOf("kasbon") >= 0;
	}
}
