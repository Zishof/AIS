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

/**
 * Mutasi Stok Antar Outlet (Fase 3 roadmap F&amp;B, klien item #5) -- ledger bertanda (signed) yang
 * menyambung LANGSUNG ke {@link ais.action.master.inventory.StokKantinUtil#formulaStokSql} (pola
 * SAMA PERSIS {@code pemakaian_bahan_baku}), BUKAN migrasi ke mesin {@code MutasiLokasi}/Gudang yang
 * sudah ada -- mesin itu terputus dari {@link Produk#getStok()} yang dipakai POS/Kasir.
 *
 * <p>Karena tiap outlet punya baris {@link Produk} TERPISAH utk "barang yang sama" (skema toko-per-baris
 * yang sudah berlaku di seluruh sistem ini), satu transfer dicatat sbg SATU baris menunjuk KEDUA sisi
 * sekaligus: {@code produkAsal} (baris Produk milik toko asal, stoknya BERKURANG) dan
 * {@code produkTujuan} (baris Produk milik toko tujuan, stoknya BERTAMBAH) -- bukan dua baris terpisah
 * spt {@code Deposit}/{@code Pembelian}. {@code tokoAsal}/{@code tokoTujuan} didenormalisasi (ikut
 * disimpan) murni utk mempercepat filter riwayat per-toko tanpa join ke {@code koperasi.produk}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "mutasi_stok_toko")
public class MutasiStokToko extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Produk produkAsal;
	private Produk produkTujuan;
	private Toko tokoAsal;
	private Toko tokoTujuan;

	private Double qty;
	private Date waktu;
	private String keterangan;
	private String oleh;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public MutasiStokToko() {
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
	@JoinColumn(name = "produk_asal", nullable = false)
	public Produk getProdukAsal() {
		return produkAsal;
	}

	public void setProdukAsal(Produk produkAsal) {
		this.produkAsal = produkAsal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_tujuan", nullable = false)
	public Produk getProdukTujuan() {
		return produkTujuan;
	}

	public void setProdukTujuan(Produk produkTujuan) {
		this.produkTujuan = produkTujuan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko_asal", nullable = false)
	public Toko getTokoAsal() {
		return tokoAsal;
	}

	public void setTokoAsal(Toko tokoAsal) {
		this.tokoAsal = tokoAsal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "toko_tujuan", nullable = false)
	public Toko getTokoTujuan() {
		return tokoTujuan;
	}

	public void setTokoTujuan(Toko tokoTujuan) {
		this.tokoTujuan = tokoTujuan;
	}

	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
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
}
