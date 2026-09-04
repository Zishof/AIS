package ais.database.model.inventory;

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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Kulakan per-Faktur (gap-closure permintaan user 2026-08-11) -- header SATU faktur/nota supplier,
 * diisi SEKALI (nomor faktur, tanggal, supplier), diikuti banyak baris {@link PengadaanProduk}
 * (lewat FK baru {@code PengadaanProduk.fakturPengadaan}) di bawahnya -- sebelumnya setiap baris
 * produk sepenuhnya independen, mengetik ulang nomor faktur/supplier sendiri-sendiri tanpa validasi
 * konsistensi apa pun.
 *
 * <p>Supplier menunjuk {@link Penyedia} (bukan {@link ais.database.model.asset.PenyediaAsset} yang
 * SEBELUMNYA dipakai {@code PengadaanProduk.supplier} utk form JSP lama) -- permintaan eksplisit
 * user ("ambil dari class Penyedia"); {@code PengadaanProduk.supplier} (PenyediaAsset) TIDAK diubah,
 * TETAP ada apa adanya utk data lama/jalur lama, header baru ini adalah sumber kebenaran BARU utk
 * entri lewat alur Faktur.</p>
 *
 * <p>{@code totalFakturManual} SENGAJA nullable -- {@code null} berarti kasir/admin tidak
 * mengisinya, total dianggap = jumlah baris ({@code totalHitungSaatSimpan}), {@code diskon = 0}.
 * Diisi HANYA bila nilai di nota fisik LEBIH KECIL dari jumlah hitungan baris (kelebihan/potongan
 * dari supplier) -- {@code diskon = totalHitungSaatSimpan - totalFakturManual} (selalu &gt;= 0,
 * server yang menghitung &amp; menyimpan, bukan klien -- lihat {@code KantinHelper.kulakanFakturSimpan}).
 * Diskon dicatat di level HEADER (bukan didistribusikan ke tiap baris produk) -- pilihan desain
 * sengaja demi kesederhanaan, cukup utk kebutuhan pencatatan "potongan faktur" tanpa perlu
 * menghitung ulang harga beli per satuan tiap baris.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_faktur")
public class PengadaanFaktur extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}. */
	private static final long serialVersionUID = 1L;
	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Toko pencatat faktur ini -- WAJIB ({@code nullable = false}), penentu kepemilikan/tenant
	 * baris ini dalam skema multi-toko kantin-koperasi. Lihat {@link #getToko()}. */
	private Toko toko;
	/** Supplier/vendor asal faktur ini, menunjuk {@link Penyedia} (BUKAN
	 * {@link ais.database.model.asset.PenyediaAsset} yang dipakai {@link PengadaanProduk#getSupplier()}
	 * pada jalur lama) -- lihat Javadoc kelas. Nullable: faktur boleh dicatat tanpa supplier
	 * teridentifikasi (mis. pembelian tunai dari penjual lepas). Lihat {@link #getSupplier()}. */
	private Penyedia supplier;

	/** Nomor faktur/nota fisik dari supplier, diketik SEKALI di header (lihat Javadoc kelas soal
	 * kenapa header ini dibuat). Lihat {@link #getNomorFaktur()}. */
	private String nomorFaktur;
	/** Tanggal faktur menurut nota fisik supplier. Lihat {@link #getTanggalFaktur()}. */
	private Date tanggalFaktur;
	/** Total faktur SESUAI NOTA FISIK, diisi manual oleh kasir/admin -- {@code nullable}, lihat
	 * Javadoc kelas soal kapan diisi (hanya bila nota fisik LEBIH KECIL dari hasil hitungan baris)
	 * dan bagaimana nilainya dipakai menghitung {@link #getDiskon()}/{@link #getTotalFakturFinal()}.
	 * Lihat {@link #getTotalFakturManual()}. */
	private Double totalFakturManual;
	/** Jumlah hitungan SEMUA baris {@link PengadaanProduk} (qty &times; harga beli satuan) SAAT
	 * faktur ini disimpan, dijumlahkan &amp; ditulis server (lihat
	 * {@code KantinHelper.kulakanFakturSimpan}) -- snapshot beku, TIDAK dihitung ulang otomatis bila
	 * baris produk di bawahnya diubah belakangan. Lihat {@link #getTotalHitungSaatSimpan()}. */
	private Double totalHitungSaatSimpan;
	/** Selisih {@code totalHitungSaatSimpan - totalFakturManual} (server yang menghitung &amp;
	 * menyimpan saat faktur disimpan, klien tidak dapat mengirim nilai ini langsung) -- lihat Javadoc
	 * kelas &amp; {@link #getDiskon()}. */
	private Double diskon;
	/** Catatan bebas ttg faktur ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Nama/id petugas yang mencatat faktur ini. Lihat {@link #getOleh()}. */
	private String oleh;
	/** Waktu faktur ini dicatat/dientri. Lihat {@link #getWaktu()}. */
	private Date waktu;
	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris ini TERAKHIR diubah, dengan menuliskan
	 * waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis oleh
	 * Hibernate sebelum {@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate. */
	public PengadaanFaktur() {
	}

	/**
	 * PK identity faktur ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke Hibernate
	 * (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id faktur, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak perlu
	 * memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id faktur.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Toko pencatat faktur ini -- {@code nullable = false}, kolom penentu kepemilikan/tenant baris
	 * ini pada instalasi multi-toko. Relasi {@code LAZY}: mengakses field pada objek di luar sesi
	 * Hibernate yang masih terbuka akan melempar {@code LazyInitializationException}.
	 *
	 * @return toko pemilik faktur ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	/**
	 * Menetapkan toko pemilik faktur. Dipakai saat membangun header baru (lihat
	 * {@code KantinHelper.kulakanFakturSimpan}) -- toko diresolusi dari sesi/permintaan pemanggil,
	 * BUKAN dikirim bebas oleh klien tanpa verifikasi (gerbang otorisasi ada di helper aksi, bukan
	 * di entity ini).
	 *
	 * @param toko toko pemilik faktur baru.
	 */
	public void setToko(Toko toko) {
		this.toko = toko;
	}

	/**
	 * Supplier/vendor asal faktur ini ({@link Penyedia} -- lihat Javadoc kelas soal bedanya dari
	 * {@code PenyediaAsset} yang dipakai jalur {@link PengadaanProduk} lama). Nullable: faktur boleh
	 * tanpa supplier teridentifikasi. Relasi {@code LAZY}.
	 *
	 * @return supplier faktur ini, atau {@code null} bila tidak diisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public Penyedia getSupplier() {
		supplier = check(supplier);
		return supplier;
	}

	/**
	 * Menetapkan supplier faktur ini. Tidak ada guard di level entity yang memaksa supplier harus
	 * diisi -- validasi kewajiban (bila ada) adalah tanggung jawab helper aksi pemanggil.
	 *
	 * @param supplier supplier baru faktur ini.
	 */
	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	/**
	 * Nomor faktur/nota fisik dari supplier, diketik SEKALI di header ini (lihat Javadoc kelas).
	 * Tidak ada constraint UNIQUE di level entity/kolom -- duplikat nomor faktur (mis. supplier
	 * berbeda memakai penomoran sama, atau kekeliruan input) TIDAK dicegah oleh entity ini.
	 *
	 * @return nomor faktur, atau {@code null}/kosong bila belum diisi (helper aksi pemanggil
	 *         mewajibkannya sebelum simpan).
	 */
	@Column(name = "nomor_faktur")
	public String getNomorFaktur() {
		return nomorFaktur;
	}

	/**
	 * Menetapkan nomor faktur.
	 *
	 * @param nomorFaktur nomor faktur/nota fisik baru.
	 */
	public void setNomorFaktur(String nomorFaktur) {
		this.nomorFaktur = nomorFaktur;
	}

	/**
	 * Tanggal faktur menurut nota fisik supplier. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat) -- pola yang
	 * sama dengan {@link #getWaktu()}.
	 *
	 * @return tanggal faktur, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_faktur")
	public Date getTanggalFaktur() {
		return tanggalFaktur == null ? ais.ui.util.WaktuUtil.getDate() : tanggalFaktur;
	}

	/**
	 * Menetapkan tanggal faktur.
	 *
	 * @param tanggalFaktur tanggal faktur baru menurut nota fisik.
	 */
	public void setTanggalFaktur(Date tanggalFaktur) {
		this.tanggalFaktur = tanggalFaktur;
	}

	/**
	 * Total faktur SESUAI NOTA FISIK, diisi manual -- {@code null} berarti kasir/admin tidak
	 * mengisinya (total dianggap = {@link #getTotalHitungSaatSimpan()}, {@link #getDiskon()} = 0).
	 * Getter TIDAK null-safe (mengembalikan {@code null} apa adanya) -- BEDA dari getter lain di
	 * kelas ini yang memakai fallback 0.0, karena {@code null} DI SINI adalah nilai bermakna
	 * ("tidak diisi manual"), bukan data hilang yang perlu di-default -- lihat Javadoc kelas.
	 * Pembeda "diisi vs tidak" ini juga dipakai {@link #getTotalFakturFinal()}.
	 *
	 * @return total faktur manual, atau {@code null} bila tidak diisi.
	 */
	@Column(name = "total_faktur_manual")
	public Double getTotalFakturManual() {
		return totalFakturManual;
	}

	/**
	 * Menetapkan total faktur manual. Tidak ada validasi di level entity -- perhitungan
	 * {@link #getDiskon()}/{@link #getTotalFakturFinal()} yang konsisten dgn nilai ini adalah
	 * tanggung jawab helper simpan ({@code KantinHelper.kulakanFakturSimpan}), BUKAN turunan
	 * otomatis dari setter ini; mengubah nilai ini langsung lewat entity tanpa lewat helper
	 * TIDAK menghitung ulang {@link #diskon}/{@link #totalHitungSaatSimpan} yang sudah tersimpan.
	 *
	 * @param totalFakturManual total faktur manual baru, atau {@code null} utk menghapusnya.
	 */
	public void setTotalFakturManual(Double totalFakturManual) {
		this.totalFakturManual = totalFakturManual;
	}

	/**
	 * Jumlah hitungan SEMUA baris {@link PengadaanProduk} (qty &times; harga beli satuan) SAAT
	 * faktur ini disimpan -- SNAPSHOT beku ditulis server saat simpan, TIDAK dihitung ulang otomatis
	 * bila baris produk di bawahnya diubah/dihapus belakangan (lihat Javadoc kelas). Getter
	 * null-safe: mengembalikan {@code 0.0} bila kolom NULL di DB.
	 *
	 * @return total hitungan baris saat faktur disimpan, tidak pernah {@code null}.
	 */
	@Column(name = "total_hitung_saat_simpan")
	public Double getTotalHitungSaatSimpan() {
		return totalHitungSaatSimpan == null ? 0.0 : totalHitungSaatSimpan;
	}

	/**
	 * Menetapkan snapshot total hitungan baris. Biasanya diisi SEKALI oleh helper simpan saat
	 * faktur dibuat -- lihat Javadoc {@link #getTotalHitungSaatSimpan()}.
	 *
	 * @param totalHitungSaatSimpan total hitungan baris baru.
	 */
	public void setTotalHitungSaatSimpan(Double totalHitungSaatSimpan) {
		this.totalHitungSaatSimpan = totalHitungSaatSimpan;
	}

	/**
	 * Selisih {@code totalHitungSaatSimpan - totalFakturManual}, dihitung &amp; disimpan SERVER
	 * (klien tidak dapat mengirim nilai ini langsung -- lihat
	 * {@code KantinHelper.kulakanFakturSimpan}), clamp ke {@code 0} minimal: bila manual justru
	 * LEBIH BESAR dari hitungan baris, diskon dianggap {@code 0} (kelebihan pengisian nota diabaikan,
	 * bukan dianggap "diskon negatif") -- lihat Javadoc kelas. Getter null-safe: mengembalikan
	 * {@code 0.0} bila kolom NULL.
	 *
	 * @return nilai diskon/potongan faktur, tidak pernah {@code null} atau negatif.
	 */
	@Column(name = "diskon")
	public Double getDiskon() {
		return diskon == null ? 0.0 : diskon;
	}

	/**
	 * Menetapkan nilai diskon. Biasanya diisi SEKALI oleh helper simpan (hasil hitungan server) --
	 * lihat Javadoc {@link #getDiskon()}; setter ini tidak memvalidasi ulang rumusnya, sehingga
	 * pengisian manual langsung lewat entity dapat menyimpan nilai yang tidak konsisten dengan
	 * {@link #totalHitungSaatSimpan}/{@link #totalFakturManual}.
	 *
	 * @param diskon nilai diskon baru.
	 */
	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Total faktur final -- manual bila diisi, jika tidak = jumlah hitungan baris. {@code @Transient}
	 * WAJIB: tanpa ini Hibernate menganggapnya kolom persisten (implicit-naming ->
	 * {@code totalfakturfinal}) yang tak pernah ada di skema {@code koperasi.pengadaan_faktur},
	 * bikin hbm2ddl gagal validasi/rebuild setiap start (root-cause bug identik pernah terjadi di
	 * {@code PembelianAnggotaKoperasi.getNominalBayar1()}).
	 */
	@Transient
	public Double getTotalFakturFinal() {
		return totalFakturManual == null ? getTotalHitungSaatSimpan() : totalFakturManual;
	}

	/**
	 * Setter kosong SENGAJA (no-op) -- {@link #getTotalFakturFinal()} adalah nilai TURUNAN
	 * ({@code @Transient}), tidak pernah disimpan langsung; dipertahankan (bukan dihapus) hanya
	 * demi kontrak getter/setter berpasangan yang dipakai sebagian framework binding UI (mis.
	 * refleksi ZK/JSTL bila ada) -- memanggil setter ini TIDAK berefek apa pun, nilai final tetap
	 * dihitung ulang dari {@link #totalFakturManual}/{@link #totalHitungSaatSimpan} setiap kali
	 * {@link #getTotalFakturFinal()} dipanggil.
	 *
	 * @param totalFakturFinal diabaikan sepenuhnya.
	 */
	public void setTotalFakturFinal(Double totalFakturFinal) {
		// Nilai final dihitung implisit dari totalFakturManual/totalHitungSaatSimpan.
	}

	/**
	 * Catatan bebas ttg faktur ini.
	 *
	 * @return keterangan, atau {@code null}/kosong bila tidak diisi.
	 */
	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Menetapkan keterangan. Tidak ada guard null/blank -- memanggil dgn string kosong akan menimpa
	 * nilai lama.
	 *
	 * @param keterangan catatan bebas baru.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Nama/id petugas yang mencatat faktur ini. BEDA dari pola
	 * {@link ais.database.model.koperasi.PayableFakturInfo#getOleh()}: getter/setter di sini
	 * TIDAK punya guard null/blank -- setter menerima &amp; menimpa apa adanya termasuk
	 * {@code null}/kosong.
	 *
	 * @return nama/id petugas pencatat, atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan {@link #oleh}. Tanpa guard -- lihat catatan pada Javadoc {@link #getOleh()}.
	 *
	 * @param oleh nama/id petugas pencatat baru.
	 */
	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	/**
	 * Waktu faktur ini dicatat/dientri. Getter null-safe: mengembalikan waktu SEKARANG
	 * ({@link ais.ui.util.WaktuUtil#getDate()}) bila kolom NULL, dihitung ULANG setiap kali getter
	 * dipanggil pada baris yang kolomnya NULL (bukan waktu tetap saat objek dibuat).
	 *
	 * @return waktu pencatatan faktur, atau waktu panggilan getter saat ini bila kolom NULL.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu pencatatan faktur.
	 *
	 * @param waktu waktu faktur ini dicatat/dientri.
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @return waktu perubahan terakhir, atau waktu instansiasi objek bila belum pernah di-update.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
