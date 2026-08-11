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

	private static final long serialVersionUID = 1L;

	public static final String STATUS_PLANNED = "PLANNED";
	public static final String STATUS_LOADED = "LOADED";
	public static final String STATUS_RECONCILED = "RECONCILED";

	private Long id;
	private SuratPerintahSalesJalan spj;
	private Produk produk;
	private String namaProduk;
	private BigDecimal qtyRencana;
	private BigDecimal qtyDimuat;
	private BigDecimal qtyTerjual;
	private BigDecimal qtyKembali;
	private BigDecimal qtyRusak;
	private BigDecimal qtyHilang;
	private BigDecimal hppSnapshot;
	private BigDecimal hargaJualSnapshot;
	private String status;
	private String alasanSelisih;

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SpjSalesBarang() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "spj", nullable = false)
	public SuratPerintahSalesJalan getSpj() {
		return spj;
	}

	public void setSpj(SuratPerintahSalesJalan spj) {
		this.spj = spj;
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

	@Column(name = "qty_rencana", precision = 19, scale = 2)
	public BigDecimal getQtyRencana() {
		return qtyRencana == null ? BigDecimal.ZERO : qtyRencana;
	}

	public void setQtyRencana(BigDecimal qtyRencana) {
		this.qtyRencana = qtyRencana;
	}

	@Column(name = "qty_dimuat", precision = 19, scale = 2)
	public BigDecimal getQtyDimuat() {
		return qtyDimuat == null ? BigDecimal.ZERO : qtyDimuat;
	}

	public void setQtyDimuat(BigDecimal qtyDimuat) {
		this.qtyDimuat = qtyDimuat;
	}

	@Column(name = "qty_terjual", precision = 19, scale = 2)
	public BigDecimal getQtyTerjual() {
		return qtyTerjual == null ? BigDecimal.ZERO : qtyTerjual;
	}

	public void setQtyTerjual(BigDecimal qtyTerjual) {
		this.qtyTerjual = qtyTerjual;
	}

	@Column(name = "qty_kembali", precision = 19, scale = 2)
	public BigDecimal getQtyKembali() {
		return qtyKembali == null ? BigDecimal.ZERO : qtyKembali;
	}

	public void setQtyKembali(BigDecimal qtyKembali) {
		this.qtyKembali = qtyKembali;
	}

	@Column(name = "qty_rusak", precision = 19, scale = 2)
	public BigDecimal getQtyRusak() {
		return qtyRusak == null ? BigDecimal.ZERO : qtyRusak;
	}

	public void setQtyRusak(BigDecimal qtyRusak) {
		this.qtyRusak = qtyRusak;
	}

	@Column(name = "qty_hilang", precision = 19, scale = 2)
	public BigDecimal getQtyHilang() {
		return qtyHilang == null ? BigDecimal.ZERO : qtyHilang;
	}

	public void setQtyHilang(BigDecimal qtyHilang) {
		this.qtyHilang = qtyHilang;
	}

	@Column(name = "hpp_snapshot", precision = 19, scale = 2)
	public BigDecimal getHppSnapshot() {
		return hppSnapshot == null ? BigDecimal.ZERO : hppSnapshot;
	}

	public void setHppSnapshot(BigDecimal hppSnapshot) {
		this.hppSnapshot = hppSnapshot;
	}

	@Column(name = "harga_jual_snapshot", precision = 19, scale = 2)
	public BigDecimal getHargaJualSnapshot() {
		return hargaJualSnapshot == null ? BigDecimal.ZERO : hargaJualSnapshot;
	}

	public void setHargaJualSnapshot(BigDecimal hargaJualSnapshot) {
		this.hargaJualSnapshot = hargaJualSnapshot;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null || status.trim().isEmpty() ? STATUS_PLANNED : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Column(name = "alasan_selisih", columnDefinition = "text")
	public String getAlasanSelisih() {
		return alasanSelisih;
	}

	public void setAlasanSelisih(String alasanSelisih) {
		this.alasanSelisih = alasanSelisih;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
