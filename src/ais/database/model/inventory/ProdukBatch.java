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
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Stok POS per batch/lot. Nilai {@code stok} adalah saldo batch yang masih dapat dipakai;
 * stok agregat produk tetap dihitung oleh {@link ais.action.master.inventory.StokKantinUtil}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "produk_batch", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "produk", "toko", "nomor_batch", "tanggal_expired" }) })
public class ProdukBatch extends GeneralValueObject {
	private static final long serialVersionUID = 1L;

	public static final String STATUS_AKTIF = "AKTIF";
	public static final String STATUS_KARANTINA = "KARANTINA";
	public static final String STATUS_DIMUSNAHKAN = "DIMUSNAHKAN";

	private Long id;
	private Produk produk;
	private Toko toko;
	private String nomorBatch;
	private Date tanggalProduksi;
	private Date tanggalExpired;
	private Double stok;
	private Double hargaModal;
	private String status;
	private String keterangan;
	private String oleh;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() { produk = check(produk); return produk; }
	public void setProduk(Produk produk) { this.produk = produk; }

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() { toko = check(toko); return toko; }
	public void setToko(Toko toko) { this.toko = toko; }

	@Column(name = "nomor_batch", nullable = false, length = 100)
	public String getNomorBatch() { return nomorBatch; }
	public void setNomorBatch(String nomorBatch) { this.nomorBatch = nomorBatch; }

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_produksi")
	public Date getTanggalProduksi() { return tanggalProduksi; }
	public void setTanggalProduksi(Date tanggalProduksi) { this.tanggalProduksi = tanggalProduksi; }

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_expired", nullable = false)
	public Date getTanggalExpired() { return tanggalExpired; }
	public void setTanggalExpired(Date tanggalExpired) { this.tanggalExpired = tanggalExpired; }

	@Column(nullable = false)
	public Double getStok() { return stok == null ? 0.0 : stok; }
	public void setStok(Double stok) { this.stok = stok; }

	@Column(name = "harga_modal")
	public Double getHargaModal() { return hargaModal == null ? 0.0 : hargaModal; }
	public void setHargaModal(Double hargaModal) { this.hargaModal = hargaModal; }

	@Column(nullable = false, length = 20)
	public String getStatus() { return status == null ? STATUS_AKTIF : status; }
	public void setStatus(String status) { this.status = status; }

	@Column(columnDefinition = "text")
	public String getKeterangan() { return keterangan; }
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	public String getOleh() { return oleh; }
	public void setOleh(String oleh) { this.oleh = oleh; }

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() { return tanggal_dirubah; }
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

	@javax.persistence.PreUpdate
	protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
}
