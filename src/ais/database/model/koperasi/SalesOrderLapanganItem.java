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
 * Rincian {@link SalesOrderLapangan} (ERD &sect;3.6). Snapshot nama/harga/HPP dibekukan saat
 * item disimpan -- harga katalog boleh berubah kapan pun tanpa mengubah order berjalan, dan
 * {@code hppSnapshot} dipakai laporan Laba Kotor (layar 46, P6) tanpa rekonstruksi historis.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "sales_order_lapangan_item")
public class SalesOrderLapanganItem extends GeneralValueObject {

	/** ID versi serialisasi Java untuk kompatibilitas {@link java.io.Serializable}. */
	private static final long serialVersionUID = 1L;

	/** ID baris (primary key), diisi otomatis DB; lihat {@link #getId()}. */
	private Long id;
	/** Order induk yang memiliki baris ini; lihat {@link #getSalesOrder()}. */
	private SalesOrderLapangan salesOrder;
	/** Produk master yang dipesan; lihat {@link #getProduk()}. */
	private Produk produk;
	/** Snapshot nama produk saat item disimpan; lihat {@link #getNamaProduk()}. */
	private String namaProduk;
	/** Snapshot harga jual per unit saat item disimpan; lihat {@link #getHargaSatuan()}. */
	private BigDecimal hargaSatuan;
	/** Kuantitas dipesan (dalam satuan dasar); lihat {@link #getJumlah()}. */
	private BigDecimal jumlah;
	/** Subtotal baris (hargaSatuan &times; jumlah); lihat {@link #getSubtotal()}. */
	private BigDecimal subtotal;
	/** Snapshot HPP per unit saat item disimpan, dibekukan untuk laporan Laba Kotor; lihat
	 *  {@link #getHppSnapshot()}. */
	private BigDecimal hppSnapshot;
	// Fase B: snapshot satuan jual -- lihat catatan di Pembelian.
	/** ID satuan jual yang dipilih user saat input (mis. "dus"/"pcs"), Fase B; lihat
	 *  {@link #getSatuanJual()}. */
	private Long satuanJual;
	/** Kuantitas input mentah dalam satuan jual yang dipilih (sebelum dikonversi ke satuan
	 *  dasar), Fase B; lihat {@link #getQtyInput()}. */
	private BigDecimal qtyInput;
	/** Faktor konversi dari satuan jual ke satuan dasar, Fase B; lihat {@link #getFaktorKeDasar()}. */
	private BigDecimal faktorKeDasar;

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis Hibernate sebelum baris ini
	 * di-{@code UPDATE}, mendelegasikan pencatatan stempel waktu perubahan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(GeneralValueObject)} yang
	 * memutakhirkan {@link #tanggal_dirubah}. Tidak dipanggil pada {@code INSERT} pertama --
	 * field diinisialisasi saat konstruksi lewat {@link ais.ui.util.WaktuUtil#getDate()}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor kosong wajib Hibernate/JavaBean; field diisi belakangan lewat setter atau reflection. */
	public SalesOrderLapanganItem() {
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

	/** Nama properti sengaja {@code salesOrder} (bukan {@code order}) -- "order" kata kunci
	 *  HQL, rawan patah di query non-native.
	 *
	 * @return order induk baris ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order", nullable = false)
	public SalesOrderLapangan getSalesOrder() {
		salesOrder = check(salesOrder);
		return salesOrder;
	}

	/**
	 * Menyimpan order induk.
	 *
	 * @param salesOrder order baru
	 */
	public void setSalesOrder(SalesOrderLapangan salesOrder) {
		this.salesOrder = salesOrder;
	}

	/**
	 * Produk master yang dipesan pada baris ini, kolom wajib ({@code nullable = false}), relasi
	 * lazy.
	 *
	 * @return produk master
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
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
	 * Snapshot nama produk saat item disimpan -- tetap terbaca walau nama produk master berubah
	 * belakangan (lihat javadoc kelas).
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
	 * Snapshot harga jual per unit saat item disimpan -- dibekukan agar harga katalog yang
	 * berubah belakangan tidak mengubah order berjalan (lihat javadoc kelas).
	 *
	 * @return harga satuan snapshot, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "harga_satuan", precision = 19, scale = 2)
	public BigDecimal getHargaSatuan() {
		return hargaSatuan == null ? BigDecimal.ZERO : hargaSatuan;
	}

	/**
	 * Menyimpan harga satuan snapshot.
	 *
	 * @param hargaSatuan harga satuan baru
	 */
	public void setHargaSatuan(BigDecimal hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	/**
	 * Kuantitas dipesan dalam satuan dasar (setelah konversi lewat {@link #getFaktorKeDasar()}
	 * bila input memakai satuan jual selain dasar; lihat Fase B pada field {@link #satuanJual}).
	 *
	 * @return jumlah dalam satuan dasar, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "jumlah", precision = 19, scale = 2)
	public BigDecimal getJumlah() {
		return jumlah == null ? BigDecimal.ZERO : jumlah;
	}

	/**
	 * Menyimpan jumlah dalam satuan dasar.
	 *
	 * @param jumlah jumlah baru
	 */
	public void setJumlah(BigDecimal jumlah) {
		this.jumlah = jumlah;
	}

	/**
	 * Subtotal baris, hasil kali {@code hargaSatuan} &times; {@code jumlah} -- denormal, dihitung
	 * pemanggil saat baris disimpan (entity ini sendiri tidak menghitungnya ulang).
	 *
	 * @return subtotal baris, tidak pernah {@code null} (nol bila belum diisi)
	 */
	@Column(name = "subtotal", precision = 19, scale = 2)
	public BigDecimal getSubtotal() {
		return subtotal == null ? BigDecimal.ZERO : subtotal;
	}

	/**
	 * Menyimpan subtotal baris.
	 *
	 * @param subtotal subtotal baru
	 */
	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}

	/**
	 * ID satuan jual yang dipilih user saat input baris ini (mis. "dus"/"pcs"), Fase B --
	 * dipasangkan dengan {@link #getQtyInput()} dan {@link #getFaktorKeDasar()} untuk merekam
	 * input mentah user sebelum dikonversi ke {@link #getJumlah()} dalam satuan dasar. Catatan
	 * pola yang sama dipakai entity {@code Pembelian} (lihat komentar kode di atas field).
	 *
	 * @return ID satuan jual, bisa {@code null} (baris lama sebelum Fase B)
	 */
	@Column(name = "satuan_jual", nullable = true)
	public Long getSatuanJual() { return satuanJual; }
	/**
	 * Menyimpan ID satuan jual.
	 *
	 * @param v ID satuan jual baru
	 */
	public void setSatuanJual(Long v) { satuanJual = v; }

	/**
	 * Kuantitas input mentah dalam satuan jual yang dipilih user ({@link #getSatuanJual()}),
	 * SEBELUM dikonversi ke satuan dasar -- nilai yang benar-benar dipakai dalam kalkulasi
	 * qty/subtotal tetap {@link #getJumlah()} (satuan dasar); field ini murni rekaman input asli
	 * untuk ditampilkan kembali ke user (mis. "3 dus" bukan "36 pcs").
	 *
	 * @return qty input mentah, bisa {@code null} (baris lama sebelum Fase B)
	 */
	@Column(name = "qty_input", precision = 19, scale = 4)
	public BigDecimal getQtyInput() { return qtyInput; }
	/**
	 * Menyimpan qty input mentah.
	 *
	 * @param v qty input baru
	 */
	public void setQtyInput(BigDecimal v) { qtyInput = v; }

	/**
	 * Faktor konversi dari satuan jual yang dipilih user ke satuan dasar (mis. {@code 12} bila
	 * satuan jual "dus" berisi 12 "pcs") -- dipakai menghitung {@link #getJumlah()} dari
	 * {@link #getQtyInput()} saat baris disimpan.
	 *
	 * @return faktor konversi ke satuan dasar, bisa {@code null} (baris lama sebelum Fase B)
	 */
	@Column(name = "faktor_ke_dasar", precision = 19, scale = 6)
	public BigDecimal getFaktorKeDasar() { return faktorKeDasar; }
	/**
	 * Menyimpan faktor konversi ke satuan dasar.
	 *
	 * @param v faktor baru
	 */
	public void setFaktorKeDasar(BigDecimal v) { faktorKeDasar = v; }

	/**
	 * HPP per unit saat order dibuat (dari {@code Produk.hargaBeli} / rata-rata kulakan) --
	 * snapshot immutable utk laba kotor; TIDAK ikut berubah bila master berubah.
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
