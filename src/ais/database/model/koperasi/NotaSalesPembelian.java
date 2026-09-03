package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.math.BigDecimal;
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
import ais.database.model.inventory.PengadaanFaktur;
import ais.database.model.library.Penyedia;

/**
 * Pembelian/kulakan dalam sesi sales lapangan (ERD &sect;3.9) -- LINK ke faktur Kulakan
 * existing ({@link PengadaanFaktur}), bukan duplikasi. Hanya {@code dibayarSesi} (kas/DP
 * aktual saat sesi) yang mengurangi hasil bersih sesi; bagian kredit dilaporkan terpisah
 * sebagai hutang baru (sisaHutang) -- rumus ERD &sect;4.1.
 *
 * <p><b>Tidak dijurnal langsung dari sini.</b> Dok 61 semula menduga modul pembelian sesi
 * sales memakai buku terpisah dari akunting; penelusuran ulang ({@code PostingBiayaSalesUtil},
 * lihat javadoc kelasnya) menunjukkan itu keliru: pembelian sesi ini sekadar TAUTAN opsional
 * ({@link #pengadaanFaktur}, boleh {@code null}) ke {@link PengadaanFaktur} yang SUDAH punya
 * jalur posting jurnalnya sendiri di modul Inventory. Class ini sendiri tidak membawa kolom
 * {@code posting_history}, berbeda dari {@link NotaSalesBiaya} yang mendapatkannya lewat
 * migrasi terpisah (r78666) karena biaya sesi memang belum pernah tersentuh buku besar sama
 * sekali sebelum itu.</p>
 *
 * <p><b>Efek sampingnya ke ledger kas.</b> Saat {@link #dibayarSesi} &gt; 0,
 * {@code SalesInventoryTripHelper.tripPurchaseLink} menulis SATU baris
 * {@link NotaSalesKas#JENIS_PURCHASE_PAYMENT} bernilai negatif (nominal
 * {@code dibayarSesi.negate()}) ke ledger kas sesi -- pembayaran ini murni catatan operasional
 * laci kas, bukan trigger posting jurnal terpisah.</p>
 *
 * <p><b>Idempotensi.</b> {@link #kodeUnik} adalah kunci idempoten (unik di DB) yang dipakai
 * pemanggil API untuk retry aman pada koneksi lapangan yang tidak stabil (pola P7 yang sama
 * dipakai {@link NotaSalesBiaya#kodeUnik}) -- request dengan {@code kode_unik} yang sudah ada
 * akan dikembalikan sebagai {@code idempotentReplay} tanpa membuat baris baru maupun menulis
 * baris kas kedua.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "nota_sales_pembelian")
public class NotaSalesPembelian extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = 1L;

	/** Barang hasil pembelian sesi masuk ke stok mobil sales (default). */
	public static final String TUJUAN_MOBIL_SALES = "MOBIL_SALES";
	/** Barang hasil pembelian sesi masuk ke stok gudang. */
	public static final String TUJUAN_GUDANG = "GUDANG";

	/** Primary key baris {@code nota_sales_pembelian}, di-generate DB (identity). */
	private Long id;

	/** Pengait wajib ke {@link NotaSalesSession} pemilik dokumen pembelian ini. */
	private NotaSalesSession sesi;

	/**
	 * Pengait opsional ke {@link PengadaanFaktur} (faktur kulakan) yang sudah punya jalur
	 * posting jurnalnya sendiri di modul Inventory -- dokumen ini hanya TAUTAN, bukan
	 * duplikasi data faktur.
	 */
	private PengadaanFaktur pengadaanFaktur;

	/** Pemasok/supplier sumber pembelian, opsional. */
	private Penyedia supplier;

	/** Nilai total faktur pembelian. */
	private BigDecimal totalFaktur;

	/**
	 * Kas/transfer/DP yang BENAR-BENAR dibayar pada sesi -- satu-satunya komponen pembelian
	 * yang mengurangi hasil bersih sesi (lihat javadoc {@link #getDibayarSesi()}).
	 */
	private BigDecimal dibayarSesi;

	/** Bagian kredit/hutang yang belum dibayar pada sesi ini, dilaporkan terpisah sebagai hutang baru. */
	private BigDecimal sisaHutang;

	/** Tujuan stok hasil pembelian: {@link #TUJUAN_MOBIL_SALES} (default) atau {@link #TUJUAN_GUDANG}. */
	private String tujuanStok;

	/** Keterangan bebas dokumen pembelian ini. */
	private String keterangan;

	/**
	 * Kunci idempoten unik untuk retry aman pada koneksi lapangan yang tidak stabil (pola
	 * P7). Lihat javadoc kelas bagian "Idempotensi".
	 */
	private String kodeUnik;

	/**
	 * Hook Hibernate {@code @PreUpdate}: dipanggil otomatis sebelum setiap UPDATE terhadap
	 * baris ini, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk mengisi
	 * ulang {@link #tanggal_dirubah}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (dibutuhkan Hibernate/JPA), tidak menginisialisasi field apa pun. */
	public NotaSalesPembelian() {
	}

	/**
	 * Mengambil primary key baris {@code NotaSalesPembelian} ini.
	 *
	 * @return {@link #id}, {@code null} bila baris belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key secara manual.
	 *
	 * @param id primary key yang ingin diset pada objek in-memory
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil {@link NotaSalesSession} pemilik dokumen pembelian ini.
	 *
	 * @return {@link #sesi}, seharusnya tidak pernah {@code null} pada baris yang sudah
	 *         tersimpan ({@code nullable = false} pada kolom {@code sesi})
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sesi", nullable = false)
	public NotaSalesSession getSesi() {
		return sesi;
	}

	/**
	 * Mengisi pengait sesi pemilik dokumen pembelian ini.
	 *
	 * @param sesi sesi baru
	 */
	public void setSesi(NotaSalesSession sesi) {
		this.sesi = sesi;
	}

	/**
	 * Mengambil {@link PengadaanFaktur} (faktur kulakan) yang ditautkan dokumen ini.
	 *
	 * @return {@link #pengadaanFaktur}, boleh {@code null} bila belum/tidak ditautkan ke
	 *         faktur kulakan mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur")
	public PengadaanFaktur getPengadaanFaktur() {
		return pengadaanFaktur;
	}

	/**
	 * Mengisi pengait faktur kulakan yang ditautkan dokumen pembelian sesi ini.
	 *
	 * @param pengadaanFaktur faktur kulakan baru, boleh {@code null}
	 */
	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
	}

	/**
	 * Mengambil pemasok/supplier sumber pembelian ini.
	 *
	 * @return {@link #supplier}, boleh {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier")
	public Penyedia getSupplier() {
		return supplier;
	}

	/**
	 * Mengisi pemasok/supplier sumber pembelian ini.
	 *
	 * @param supplier supplier baru, boleh {@code null}
	 */
	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	/**
	 * Mengambil nilai total faktur pembelian, dengan default null-safe.
	 *
	 * @return {@link #totalFaktur}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "total_faktur", precision = 19, scale = 2)
	public BigDecimal getTotalFaktur() {
		return totalFaktur == null ? BigDecimal.ZERO : totalFaktur;
	}

	/**
	 * Mengisi nilai total faktur pembelian.
	 *
	 * @param totalFaktur nilai baru
	 */
	public void setTotalFaktur(BigDecimal totalFaktur) {
		this.totalFaktur = totalFaktur;
	}

	/**
	 * Mengambil nilai kas/transfer/DP yang BENAR-BENAR dibayar pada sesi, dengan default
	 * null-safe. Ini SATU-SATUNYA komponen pembelian yang mengurangi hasil bersih sesi
	 * ({@code HASIL_BERSIH = TOTAL_PIUTANG_DIBAYAR - TOTAL_BIAYA -
	 * TOTAL_PEMBAYARAN_AKTUAL_PEMBELIAN}, lihat javadoc {@link NotaSalesSession}) -- nilai
	 * {@link #totalFaktur} dan {@link #sisaHutang} TIDAK ikut mengurangi hasil bersih,
	 * karena bagian yang belum dibayar tunai sesungguhnya adalah hutang baru, bukan
	 * pengeluaran kas sesi.
	 *
	 * @return {@link #dibayarSesi}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "dibayar_sesi", precision = 19, scale = 2)
	public BigDecimal getDibayarSesi() {
		return dibayarSesi == null ? BigDecimal.ZERO : dibayarSesi;
	}

	/**
	 * Mengisi nilai yang benar-benar dibayar pada sesi. Kode pemanggil
	 * ({@code SalesInventoryTripHelper.tripPurchaseLink}) memvalidasi nilai ini berada di
	 * rentang {@code 0..totalFaktur} sebelum disimpan -- validasi tersebut TIDAK diulang di
	 * setter ini, sehingga pemanggilan langsung tanpa lewat jalur API bisa menyimpan nilai
	 * di luar rentang tersebut.
	 *
	 * @param dibayarSesi nilai baru
	 */
	public void setDibayarSesi(BigDecimal dibayarSesi) {
		this.dibayarSesi = dibayarSesi;
	}

	/**
	 * Mengambil bagian kredit/hutang yang belum dibayar pada sesi ini, dengan default
	 * null-safe.
	 *
	 * @return {@link #sisaHutang}, atau {@link BigDecimal#ZERO} bila belum diisi
	 */
	@Column(name = "sisa_hutang", precision = 19, scale = 2)
	public BigDecimal getSisaHutang() {
		return sisaHutang == null ? BigDecimal.ZERO : sisaHutang;
	}

	/**
	 * Mengisi sisa hutang dokumen ini. Umumnya diisi kode pemanggil sebagai
	 * {@code totalFaktur.subtract(dibayarSesi)}, tetapi setter ini sendiri TIDAK memaksa
	 * konsistensi tersebut -- kedua field bisa disimpan tidak sinkron bila dipanggil di
	 * luar jalur normal.
	 *
	 * @param sisaHutang nilai baru
	 */
	public void setSisaHutang(BigDecimal sisaHutang) {
		this.sisaHutang = sisaHutang;
	}

	/**
	 * Mengambil tujuan stok hasil pembelian, dengan default null-safe.
	 *
	 * @return {@link #tujuanStok} apa adanya bila sudah diisi dan tidak blank; {@link
	 *         #TUJUAN_MOBIL_SALES} bila {@code null} atau kosong/spasi saja
	 */
	@Column(name = "tujuan_stok", length = 30)
	public String getTujuanStok() {
		return tujuanStok == null || tujuanStok.trim().isEmpty() ? TUJUAN_MOBIL_SALES : tujuanStok;
	}

	/**
	 * Mengisi tujuan stok hasil pembelian secara langsung, tanpa validasi terhadap
	 * {@link #TUJUAN_MOBIL_SALES}/{@link #TUJUAN_GUDANG}.
	 *
	 * @param tujuanStok nilai baru
	 */
	public void setTujuanStok(String tujuanStok) {
		this.tujuanStok = tujuanStok;
	}

	/**
	 * Mengambil keterangan bebas dokumen pembelian ini.
	 *
	 * @return {@link #keterangan} apa adanya, bisa {@code null}
	 */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * Mengisi keterangan bebas dokumen pembelian ini.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Mengambil kunci idempoten unik dokumen ini.
	 *
	 * @return {@link #kodeUnik} apa adanya, bisa {@code null} pada baris yang dibuat sebelum
	 *         pola idempotensi ini diterapkan
	 */
	@Column(name = "kode_unik", length = 80, unique = true)
	public String getKodeUnik() {
		return kodeUnik;
	}

	/**
	 * Mengisi kunci idempoten unik dokumen ini. Kolom {@code unique = true} pada DB --
	 * penyimpanan dengan {@code kodeUnik} yang sudah dipakai baris lain akan gagal dengan
	 * {@code ConstraintViolationException}, yang oleh kode pemanggil ditangkap dan
	 * diperlakukan sebagai sinyal replay idempoten (lihat javadoc kelas).
	 *
	 * @param kodeUnik kunci idempoten baru
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini.
	 *
	 * @return {@link #tanggal_dirubah}, tidak pernah {@code null} setelah objek dikonstruksi
	 *         (inisialisasi eager pada deklarasi field, diperbarui otomatis oleh
	 *         {@link #onUpdate()} pada setiap UPDATE)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Mengisi timestamp perubahan terakhir secara manual. Umumnya tidak perlu dipanggil
	 * langsung karena {@link #onUpdate()} sudah mengisinya otomatis setiap UPDATE.
	 *
	 * @param tanggal_dirubah timestamp baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
