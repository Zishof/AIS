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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.asset.PenyediaAsset;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_produk")
public class PengadaanProduk extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Produk produk;
	private Toko toko;

	private String nomorFaktur; // Nomor nota dari supplier
	private String namaSupplier;
	private PenyediaAsset supplier;
	private PengadaanFaktur fakturPengadaan; // Header Kulakan-per-Faktur (gap-closure 2026-08-11) -- nullable, data lama tetap null selamanya
	private Double qty; // Jumlah barang yang masuk
	private Double hargaBeliSatuan;
	private Double totalHarga;

	private Date waktuPengadaan;
	private String keterangan;

	private String oleh;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PengadaanProduk() {
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
	@JoinColumn(name = "produk", nullable = false)
	public Produk getProduk() {
		produk = check(produk);
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	public String getNomorFaktur() {
		return nomorFaktur;
	}

	public void setNomorFaktur(String nomorFaktur) {
		this.nomorFaktur = nomorFaktur;
	}

	public String getNamaSupplier() {
		if (getSupplier() != null) {
			namaSupplier = supplier.getNama();
		}
		return namaSupplier;
	}

	public void setNamaSupplier(String namaSupplier) {
		this.namaSupplier = namaSupplier;
	}

	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Double getHargaBeliSatuan() {
		return hargaBeliSatuan == null ? 0.0 : hargaBeliSatuan;
	}

	public void setHargaBeliSatuan(Double hargaBeliSatuan) {
		this.hargaBeliSatuan = hargaBeliSatuan;
	}

	public Double getTotalHarga() {
		if (totalHarga == null || totalHarga == 0.0) {
			totalHarga = getQty() * getHargaBeliSatuan();
		}
		return totalHarga;
	}

	public void setTotalHarga(Double totalHarga) {
		this.totalHarga = totalHarga;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuPengadaan() {
		return waktuPengadaan == null ? ais.ui.util.WaktuUtil.getDate() : waktuPengadaan;
	}

	public void setWaktuPengadaan(Date waktuPengadaan) {
		this.waktuPengadaan = waktuPengadaan;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public PenyediaAsset getSupplier() {
		supplier = check(supplier);
		return supplier;
	}

	public void setSupplier(PenyediaAsset supplier) {
		this.supplier = supplier;
	}

	/** Header Kulakan-per-Faktur (gap-closure 2026-08-11) -- lihat JavaDoc {@link PengadaanFaktur}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "faktur_pengadaan", nullable = true)
	public PengadaanFaktur getFakturPengadaan() {
		return fakturPengadaan;
	}

	public void setFakturPengadaan(PengadaanFaktur fakturPengadaan) {
		this.fakturPengadaan = fakturPengadaan;
	}

}
