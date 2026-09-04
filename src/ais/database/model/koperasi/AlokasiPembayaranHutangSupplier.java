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

/**
 * Alokasi satu {@link PembayaranHutangSupplier} ke satu {@link PengadaanFaktur} (layar legacy
 * 24: satu pembayaran boleh melunasi banyak faktur; satu faktur boleh dibayar bertahap).
 * Invariant (ditegakkan atomik di helper): SUM(alokasi per pembayaran) = nominal pembayaran;
 * tiap alokasi &le; outstanding faktur saat itu.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "alokasi_pembayaran_hutang_supplier")
public class AlokasiPembayaranHutangSupplier extends GeneralValueObject {

	/** Versi serialisasi tetap; dipertahankan hanya krn kontrak {@link GeneralValueObject}/
	 * {@code Serializable}, entity ini tidak dikirim lewat Java serialization jarak jauh. */
	private static final long serialVersionUID = 1L;

	/** PK auto-generated (identity). Lihat {@link #getId()}. */
	private Long id;
	/** Sisi "sumber dana" alokasi -- satu pembayaran boleh dipecah ke banyak baris alokasi (satu
	 * per faktur pengadaan yg dilunasi/dicicil). Lihat {@link #getPembayaran()}. */
	private PembayaranHutangSupplier pembayaran;
	/** Sisi "target utang" alokasi -- faktur pengadaan (hutang ke supplier) yang menerima potongan
	 * outstanding dari baris ini. Lihat {@link #getPengadaanFaktur()}. */
	private PengadaanFaktur pengadaanFaktur;
	/** Nominal yang dialokasikan dari {@link #pembayaran} ke {@link #pengadaanFaktur} pada baris
	 * ini (bukan nominal pembayaran/faktur secara keseluruhan). Lihat {@link #getNominal()}. */
	private BigDecimal nominal;

	/**
	 * Callback JPA {@code @PreUpdate}: menandai kapan baris alokasi ini TERAKHIR diubah, dengan
	 * menuliskan waktu sekarang ke {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Dipanggil otomatis
	 * oleh Hibernate sebelum setiap {@code UPDATE}, tidak pernah dipanggil manual dari kode
	 * aplikasi. Sama pola persis dgn cermin AR-nya, {@link AlokasiPenerimaanPiutangCustomer
	 * #onUpdate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor tanpa argumen wajib JPA/Hibernate; jangan dipakai langsung dari kode aplikasi --
	 * gunakan setter untuk mengisi {@link #pembayaran}, {@link #pengadaanFaktur}, dan
	 * {@link #nominal} sebelum {@code save}, sesuai invariant di helper create (lihat Javadoc
	 * kelas). */
	public AlokasiPembayaranHutangSupplier() {
	}

	/**
	 * PK identity baris alokasi ini. {@code null} sebelum entity di-{@code save}/{@code flush} ke
	 * Hibernate (ID baru dibuat DB saat insert, strategi {@link IDENTITY}).
	 *
	 * @return id baris alokasi, atau {@code null} bila belum tersimpan.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Setter PK -- dipanggil Hibernate saat memuat entity dari DB. Kode aplikasi normal tidak
	 * perlu memanggil ini; id baru dibuat otomatis oleh DB saat insert.
	 *
	 * @param id id baris alokasi.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Pembayaran hutang (sisi kas keluar ke supplier) yang sebagian/seluruh nominalnya
	 * dialokasikan lewat baris ini. {@code nullable = false}. Relasi {@code LAZY}: mengakses field
	 * pada objek yang dikembalikan di luar sesi Hibernate yang masih terbuka akan melempar
	 * {@code LazyInitializationException}.
	 *
	 * @return pembayaran sumber dana alokasi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran", nullable = false)
	public PembayaranHutangSupplier getPembayaran() {
		pembayaran = check(pembayaran);
		return pembayaran;
	}

	/**
	 * Menetapkan pembayaran sumber dana alokasi ini. Dipanggil helper create sebelum simpan; tidak
	 * mengecek ulang bahwa {@link #getNominal()} masih dalam batas sisa pembayaran -- penjagaan
	 * itu tugas pemanggil (query {@code SUM(alokasi)} + validasi di
	 * {@code SalesInventoryPayableHelper}), bukan setter ini.
	 *
	 * @param pembayaran baris {@link PembayaranHutangSupplier} sumber dana.
	 */
	public void setPembayaran(PembayaranHutangSupplier pembayaran) {
		this.pembayaran = pembayaran;
	}

	/**
	 * Faktur pengadaan (hutang ke supplier) yang outstanding-nya dikurangi oleh nominal baris ini.
	 * {@code nullable = false}. Relasi {@code LAZY} -- sama catatan lazy-loading dgn
	 * {@link #getPembayaran()}.
	 *
	 * @return faktur pengadaan target alokasi ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengadaan_faktur", nullable = false)
	public PengadaanFaktur getPengadaanFaktur() {
		pengadaanFaktur = check(pengadaanFaktur);
		return pengadaanFaktur;
	}

	/**
	 * Menetapkan faktur pengadaan target alokasi ini. Sama seperti {@link #setPembayaran}, setter
	 * ini TIDAK memvalidasi bahwa nominal &le; outstanding faktur saat dipanggil -- validasi
	 * "FOR UPDATE per faktur" (mencegah dua pembayaran bersamaan sama-sama lolos) dilakukan
	 * {@code SalesInventoryPayableHelper} di lapisan servlet sebelum entity ini dibangun.
	 *
	 * @param pengadaanFaktur baris {@link PengadaanFaktur} target alokasi.
	 */
	public void setPengadaanFaktur(PengadaanFaktur pengadaanFaktur) {
		this.pengadaanFaktur = pengadaanFaktur;
	}

	/**
	 * Nominal rupiah yang dialokasikan dari {@link #getPembayaran()} ke
	 * {@link #getPengadaanFaktur()} pada baris ini. Getter null-safe: mengembalikan
	 * {@link BigDecimal#ZERO} bila kolom NULL di DB (baris lama/rusak), supaya kalkulasi SUM di
	 * lapisan pemanggil tidak perlu null-check berulang.
	 *
	 * @return nominal alokasi, tidak pernah {@code null}.
	 */
	@Column(name = "nominal", precision = 19, scale = 2)
	public BigDecimal getNominal() {
		return nominal == null ? BigDecimal.ZERO : nominal;
	}

	/**
	 * Menetapkan nominal alokasi baris ini. Tidak melakukan validasi apa pun (boleh negatif/nol
	 * bila dipanggil langsung) -- penjagaan "SUM(alokasi per pembayaran) = nominal pembayaran" dan
	 * "alokasi &le; outstanding faktur" ada di helper create (query {@code FOR UPDATE}), BUKAN di
	 * sini. Memanggil setter ini di luar jalur helper tsb melewati penjagaan tersebut.
	 *
	 * @param nominal nominal alokasi baru, boleh {@code null} (diperlakukan sbg nol oleh getter).
	 */
	public void setNominal(BigDecimal nominal) {
		this.nominal = nominal;
	}

	/**
	 * Timestamp perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()} setiap kali
	 * Hibernate melakukan {@code UPDATE}. Nilai awal (sebelum ada update apa pun) adalah waktu
	 * instansiasi objek Java, BUKAN waktu insert DB sesungguhnya.
	 *
	 * @return waktu perubahan terakhir baris alokasi ini.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Setter manual utk {@link #tanggal_dirubah}. Jarang dipakai langsung -- field ini biasanya
	 * diisi otomatis oleh {@link #onUpdate()}; memanggil setter ini eksplisit dari kode aplikasi
	 * akan ditimpa lagi oleh callback tsb pada {@code UPDATE} berikutnya.
	 *
	 * @param tanggal_dirubah waktu perubahan yang ingin dicatat.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
