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

	private static final long serialVersionUID = 1L;

	private Long id;
	private SalesOrderLapangan salesOrder;
	private Produk produk;
	private String namaProduk;
	private BigDecimal hargaSatuan;
	private BigDecimal jumlah;
	private BigDecimal subtotal;
	private BigDecimal hppSnapshot;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SalesOrderLapanganItem() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Nama properti sengaja {@code salesOrder} (bukan {@code order}) -- "order" kata kunci
	 *  HQL, rawan patah di query non-native. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sales_order", nullable = false)
	public SalesOrderLapangan getSalesOrder() {
		return salesOrder;
	}

	public void setSalesOrder(SalesOrderLapangan salesOrder) {
		this.salesOrder = salesOrder;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	@Column(name = "nama_produk")
	public String getNamaProduk() {
		return namaProduk;
	}

	public void setNamaProduk(String namaProduk) {
		this.namaProduk = namaProduk;
	}

	@Column(name = "harga_satuan", precision = 19, scale = 2)
	public BigDecimal getHargaSatuan() {
		return hargaSatuan == null ? BigDecimal.ZERO : hargaSatuan;
	}

	public void setHargaSatuan(BigDecimal hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	@Column(name = "jumlah", precision = 19, scale = 2)
	public BigDecimal getJumlah() {
		return jumlah == null ? BigDecimal.ZERO : jumlah;
	}

	public void setJumlah(BigDecimal jumlah) {
		this.jumlah = jumlah;
	}

	@Column(name = "subtotal", precision = 19, scale = 2)
	public BigDecimal getSubtotal() {
		return subtotal == null ? BigDecimal.ZERO : subtotal;
	}

	public void setSubtotal(BigDecimal subtotal) {
		this.subtotal = subtotal;
	}

	/** HPP per unit saat order dibuat (dari {@code Produk.hargaBeli} / rata-rata kulakan) --
	 *  snapshot immutable utk laba kotor; TIDAK ikut berubah bila master berubah. */
	@Column(name = "hpp_snapshot", precision = 19, scale = 2)
	public BigDecimal getHppSnapshot() {
		return hppSnapshot == null ? BigDecimal.ZERO : hppSnapshot;
	}

	public void setHppSnapshot(BigDecimal hppSnapshot) {
		this.hppSnapshot = hppSnapshot;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
