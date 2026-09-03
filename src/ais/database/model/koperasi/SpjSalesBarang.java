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
import ais.database.model.inventory.Produk;

/**
 * Barang dibawa per SPJ (layar legacy 39-40, ERD &sect;3.3). Invariant rekonsiliasi:
 * {@code qtyDimuat = qtyTerjual + qtyKembali + qtyRusak + qtyHilang + masihDibawa}
 * (masihDibawa = turunan, tidak disimpan). Snapshot HPP + harga jual dibekukan saat muat.
 *
 * <p>KEPUTUSAN D-14: stok mobil sales dicatat PENUH di ledger ini (rencana/dimuat/terjual/
 * kembali/rusak/hilang + rekonsiliasi tutup sesi); INTEGRASI pemotongan stok toko (inventory
 * movement nyata ke formula stok POS) menunggu keputusan UAT -- mengubah formula stok global
 * menyentuh POS produksi (kebijakan risiko dokumen input).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "spj_sales_barang")
public class SpjSalesBarang extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;

	/** Status awal baris: baru direncanakan (qty rencana diisi), belum dimuat fisik ke kendaraan. */
	public static final String STATUS_PLANNED = "PLANNED";
	/** Barang sudah dimuat fisik ke kendaraan sales ({@code qtyDimuat} terisi); dipakai saat SPJ
	 *  berpindah ke {@code ACTIVE} (lihat {@code SalesInventoryTripHelper}, transisi muat barang). */
	public static final String STATUS_LOADED = "LOADED";
	/** Status akhir/final: baris sudah direkonsiliasi tuntas saat sesi ditutup ({@code
	 *  SalesInventoryTripHelper.tripClose}) -- tidak berubah lagi setelahnya. */
	public static final String STATUS_RECONCILED = "RECONCILED";

	/** ID baris (primary key), diisi otomatis DB; lihat {@link #getId()}. */
	private Long id;
	/** SPJ induk yang membawa barang ini; lihat {@link #getSpj()}. */
	private SuratPerintahSalesJalan spj;
	/** Produk master yang dibawa; lihat {@link #getProduk()}. */
	private Produk produk;
	/** Snapshot nama produk saat dimuat; lihat {@link #getNamaProduk()}. */
	private String namaProduk;
	/** Kuantitas rencana dibawa (sebelum realisasi muat); lihat {@link #getQtyRencana()}. */
	private BigDecimal qtyRencana;
	/** Kuantitas yang benar-benar dimuat ke kendaraan; sisi kiri invariant rekonsiliasi --
	 *  lihat {@link #getQtyDimuat()} dan javadoc kelas. */
	private BigDecimal qtyDimuat;
	/** Kuantitas terjual di lapangan selama trip; salah satu komponen sisi kanan invariant
	 *  rekonsiliasi; lihat {@link #getQtyTerjual()}. */
	private BigDecimal qtyTerjual;
	/** Kuantitas dibawa kembali utuh (tidak terjual); komponen invariant rekonsiliasi;
	 *  lihat {@link #getQtyKembali()}. */
	private BigDecimal qtyKembali;
	/** Kuantitas rusak selama trip (tidak terjual, tidak bisa dikembalikan ke stok baik);
	 *  komponen invariant rekonsiliasi; lihat {@link #getQtyRusak()}. */
	private BigDecimal qtyRusak;
	/** Kuantitas hilang/tidak dapat dipertanggungjawabkan; komponen invariant rekonsiliasi
	 *  paling sensitif kecurangan; lihat {@link #getQtyHilang()}. */
	private BigDecimal qtyHilang;
	/** Snapshot HPP per unit saat dimuat, dibekukan agar laporan laba tidak berubah bila
	 *  harga beli master berubah belakangan; lihat {@link #getHppSnapshot()}. */
	private BigDecimal hppSnapshot;
	/** Snapshot harga jual per unit saat dimuat; lihat {@link #getHargaJualSnapshot()}. */
	private BigDecimal hargaJualSnapshot;
	/** Status siklus hidup baris ({@link #STATUS_PLANNED}/{@link #STATUS_LOADED}/
	 *  {@link #STATUS_RECONCILED}); lihat {@link #getStatus()}. */
	private String status;
	/** Penjelasan manual untuk selisih qty (rusak/hilang) saat rekonsiliasi tidak bulat;
	 *  lihat {@link #getAlasanSelisih()}. */
	private String alasanSelisih;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis Hibernate sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(GeneralValueObject)} yang
	 * memutakhirkan {@link #tanggal_dirubah}. Tidak dipanggil pada {@code INSERT} pertama --
	 * field diinisialisasi saat konstruksi lewat {@link ais.ui.util.WaktuUtil#getDate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate/JavaBean; field diisi belakangan lewat setter atau reflection. */
	public SpjSalesBarang() {
	}

	/**
	 * Primary key baris, dipetakan {@code IDENTITY} (auto-increment DB).
	 *
	 * @return ID baris, atau {@code null} untuk object baru yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyimpan ID baris. Dipanggil Hibernate saat hidrasi dari DB.
	 *
	 * @param id ID baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * SPJ (keberangkatan sales) induk yang membawa baris barang ini, kolom wajib
	 * ({@code nullable = false}), relasi lazy.
	 *
	 * @return SPJ induk
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spj", nullable = false)
	public SuratPerintahSalesJalan getSpj() {
		return spj;
	}

	/**
	 * Menyimpan SPJ induk.
	 *
	 * @param spj SPJ baru
	 */
	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
	}

	/**
	 * Produk master yang dibawa pada baris ini, kolom wajib ({@code nullable = false}), relasi
	 * lazy.
	 *
	 * @return produk master
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	/**
	 * Menyimpan produk master.
	 *
	 * @param produk produk baru
	 */
	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	/**
	 * Snapshot nama produk saat baris dimuat -- tetap terbaca walau nama produk master berubah
	 * atau produk dihapus/nonaktif belakangan.
	 *
	 * @return nama produk snapshot, bisa {@code null}
	 */
	@Column(name = "nama_produk")
	public String getNamaProduk() {
		return namaProduk;
	}

	/**
	 * Menyimpan snapshot nama produk.
	 *
	 * @param namaProduk nama produk baru
	 */
	public void setNamaProduk(String namaProduk) {
		this.namaProduk = namaProduk;
	}

	/**
	 * Kuantitas rencana dibawa, sebelum realisasi muat fisik ke kendaraan.
	 *
	 * @return qty rencana, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_rencana", precision = 19, scale = 2)
	public BigDecimal getQtyRencana() {
		return qtyRencana == null ? BigDecimal.ZERO : qtyRencana;
	}

	/**
	 * Menyimpan qty rencana.
	 *
	 * @param qtyRencana qty rencana baru
	 */
	public void setQtyRencana(BigDecimal qtyRencana) {
		this.qtyRencana = qtyRencana;
	}

	/**
	 * Kuantitas yang benar-benar dimuat fisik ke kendaraan sales -- sisi kiri invariant
	 * rekonsiliasi {@code qtyDimuat = qtyTerjual + qtyKembali + qtyRusak + qtyHilang}. Invariant
	 * ini ditegakkan SEBELUM sesi boleh masuk status {@code RECONCILING}: helper
	 * {@code SalesInventoryTripHelper.ubahStatusSesi(..., cekBarang=true)} menjumlahkan
	 * {@code qtyDimuat - qtyTerjual - qtyKembali - qtyRusak - qtyHilang} tiap baris dan menolak
	 * transisi ({@code tolak(...)}) bila sisanya (toleransi {@code 0.001}) tidak nol -- barang
	 * yang belum "habis dialokasikan" memblokir sesi ditutup.
	 *
	 * @return qty dimuat, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_dimuat", precision = 19, scale = 2)
	public BigDecimal getQtyDimuat() {
		return qtyDimuat == null ? BigDecimal.ZERO : qtyDimuat;
	}

	/**
	 * Menyimpan qty dimuat.
	 *
	 * @param qtyDimuat qty dimuat baru
	 */
	public void setQtyDimuat(BigDecimal qtyDimuat) {
		this.qtyDimuat = qtyDimuat;
	}

	/**
	 * Kuantitas terjual di lapangan selama trip -- komponen sisi kanan invariant rekonsiliasi;
	 * lihat {@link #getQtyDimuat()}.
	 *
	 * @return qty terjual, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_terjual", precision = 19, scale = 2)
	public BigDecimal getQtyTerjual() {
		return qtyTerjual == null ? BigDecimal.ZERO : qtyTerjual;
	}

	/**
	 * Menyimpan qty terjual.
	 *
	 * @param qtyTerjual qty terjual baru
	 */
	public void setQtyTerjual(BigDecimal qtyTerjual) {
		this.qtyTerjual = qtyTerjual;
	}

	/**
	 * Kuantitas dibawa kembali utuh (tidak terjual) -- komponen sisi kanan invariant
	 * rekonsiliasi; lihat {@link #getQtyDimuat()}.
	 *
	 * @return qty kembali, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_kembali", precision = 19, scale = 2)
	public BigDecimal getQtyKembali() {
		return qtyKembali == null ? BigDecimal.ZERO : qtyKembali;
	}

	/**
	 * Menyimpan qty kembali.
	 *
	 * @param qtyKembali qty kembali baru
	 */
	public void setQtyKembali(BigDecimal qtyKembali) {
		this.qtyKembali = qtyKembali;
	}

	/**
	 * Kuantitas rusak selama trip (tidak terjual, tidak kembali sebagai stok baik) -- komponen
	 * sisi kanan invariant rekonsiliasi; lihat {@link #getQtyDimuat()}. Tidak divalidasi silang
	 * terhadap bukti fisik apa pun oleh entity ini -- nilainya murni input sales/petugas
	 * rekonsiliasi, ditopang {@link #getAlasanSelisih()} sebagai penjelasan naratif opsional.
	 *
	 * @return qty rusak, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_rusak", precision = 19, scale = 2)
	public BigDecimal getQtyRusak() {
		return qtyRusak == null ? BigDecimal.ZERO : qtyRusak;
	}

	/**
	 * Menyimpan qty rusak.
	 *
	 * @param qtyRusak qty rusak baru
	 */
	public void setQtyRusak(BigDecimal qtyRusak) {
		this.qtyRusak = qtyRusak;
	}

	/**
	 * Kuantitas hilang/tidak dapat dipertanggungjawabkan -- komponen sisi kanan invariant
	 * rekonsiliasi; lihat {@link #getQtyDimuat()}. Baris paling sensitif kecurangan: bersama
	 * {@link #getQtyRusak()}, field ini menyerap SISA apa pun yang tidak terjual dan tidak
	 * kembali fisik -- entity/invariant hanya memastikan angka-angkanya BERJUMLAH pas dengan qty
	 * dimuat, bukan bahwa qty hilang yang dilaporkan benar-benar hilang (mis. dijual tanpa
	 * dicatat lalu dilaporkan "hilang" untuk menutup selisih kas) -- validasi kejujuran nilai ini
	 * ada di luar entity (approval {@link #getAlasanSelisih()} / proses tutup sesi
	 * {@code Pemilik/Admin}, bukan pemeriksaan otomatis di sini.
	 *
	 * @return qty hilang, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "qty_hilang", precision = 19, scale = 2)
	public BigDecimal getQtyHilang() {
		return qtyHilang == null ? BigDecimal.ZERO : qtyHilang;
	}

	/**
	 * Menyimpan qty hilang.
	 *
	 * @param qtyHilang qty hilang baru
	 */
	public void setQtyHilang(BigDecimal qtyHilang) {
		this.qtyHilang = qtyHilang;
	}

	/**
	 * Snapshot HPP per unit saat baris dimuat, dibekukan agar laporan laba kotor trip tidak
	 * berubah bila harga beli master produk berubah belakangan.
	 *
	 * @return HPP snapshot, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "hpp_snapshot", precision = 19, scale = 2)
	public BigDecimal getHppSnapshot() {
		return hppSnapshot == null ? BigDecimal.ZERO : hppSnapshot;
	}

	/**
	 * Menyimpan HPP snapshot.
	 *
	 * @param hppSnapshot HPP snapshot baru
	 */
	public void setHppSnapshot(BigDecimal hppSnapshot) {
		this.hppSnapshot = hppSnapshot;
	}

	/**
	 * Snapshot harga jual per unit saat baris dimuat, dibekukan agar tidak terpengaruh perubahan
	 * harga katalog di tengah trip.
	 *
	 * @return harga jual snapshot, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "harga_jual_snapshot", precision = 19, scale = 2)
	public BigDecimal getHargaJualSnapshot() {
		return hargaJualSnapshot == null ? BigDecimal.ZERO : hargaJualSnapshot;
	}

	/**
	 * Menyimpan harga jual snapshot.
	 *
	 * @param hargaJualSnapshot harga jual snapshot baru
	 */
	public void setHargaJualSnapshot(BigDecimal hargaJualSnapshot) {
		this.hargaJualSnapshot = hargaJualSnapshot;
	}

	/**
	 * Status siklus hidup baris. Getter memakai default lazy {@link #STATUS_PLANNED} bila field
	 * mentah {@code null}/kosong.
	 *
	 * @return status saat ini, tidak pernah {@code null}/kosong
	 */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PLANNED : status;
	}

	/**
	 * Menyimpan status baris.
	 *
	 * @param status status baru
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Penjelasan manual (bebas teks) untuk selisih qty rusak/hilang -- diisi petugas/sales saat
	 * rekonsiliasi tidak bulat, murni naratif dan tidak divalidasi/diverifikasi otomatis oleh
	 * entity ini.
	 *
	 * @return alasan selisih, bisa {@code null}
	 */
	@Column(name = "alasan_selisih", columnDefinition = "text")
	public String getAlasanSelisih() {
		return alasanSelisih;
	}

	/**
	 * Menyimpan alasan selisih.
	 *
	 * @param alasanSelisih alasan baru
	 */
	public void setAlasanSelisih(String alasanSelisih) {
		this.alasanSelisih = alasanSelisih;
	}

	/**
	 * Mengembalikan stempel waktu terakhir baris ini dirubah, dipetakan sebagai kolom
	 * {@code @Temporal(TIMESTAMP)}. Diinisialisasi ke waktu-sekarang saat object dikonstruksi dan
	 * dimutakhirkan otomatis oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @return stempel waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyimpan stempel waktu terakhir baris ini dirubah.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
