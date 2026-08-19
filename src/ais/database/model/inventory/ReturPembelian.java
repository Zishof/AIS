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
import ais.database.model.library.Penyedia;

/**
 * Retur Pembelian (gap-closure roadmap Fase 3, permintaan user 2026-08-11) -- barang yang
 * dikembalikan KE SUPPLIER (rusak/salah kirim/tidak sesuai), kebalikan {@link ReturPenjualan}
 * (barang kembali DARI pelanggan). Menggantikan peran {@link ReturBarang} yang sebelumnya JSP-only,
 * tanpa header/supplier, dan SENGAJA tidak terintegrasi ke rumus stok (lihat JavaDoc di sana) --
 * fitur ini FULL terintegrasi: reachable dari JSP/Electron/Flutter, tertaut opsional ke
 * {@link PengadaanFaktur} asal, DAN mengurangi stok otomatis (lihat suku baru di
 * {@code StokKantinUtil.formulaStokSql}).
 *
 * <p>Pola SAMA PERSIS {@link ReturPenjualan}/{@link PengadaanProduk} -- satu baris per SKU per
 * peristiwa retur, dikelompokkan lewat {@link #getFakturPengadaan()} bila petugas menautkannya ke
 * faktur asal (opsional -- barang bisa saja diretur tanpa jejak faktur pengadaan yang jelas,
 * mis. data lama/barang titipan).</p>
 *
 * <p><b>TIDAK ADA flag "kembalikan ke stok"</b> (beda dari {@link ReturPenjualan}) -- retur
 * pembelian secara fisik SELALU berarti barang meninggalkan toko kembali ke supplier, jadi qty
 * SELALU mengurangi stok tanpa syarat, tidak ada skenario "barang tetap di toko tapi dicatat retur".</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_pembelian")
public class ReturPembelian extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Produk produk;
	private Toko toko;

	private PengadaanFaktur fakturPengadaan;
	private String kodeFakturAsal;
	private Penyedia supplier;

	private Double qty;
	private Double hargaSatuan;
	private Double totalNilai;

	private String alasan;
	private String keterangan;

	private Date waktu;
	private String oleh;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ReturPembelian() {
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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "faktur_pengadaan", nullable = true)
	public PengadaanFaktur getFakturPengadaan() {
		return fakturPengadaan;
	}

	public void setFakturPengadaan(PengadaanFaktur fakturPengadaan) {
		this.fakturPengadaan = fakturPengadaan;
	}

	@Column(name = "kode_faktur_asal")
	public String getKodeFakturAsal() {
		return kodeFakturAsal;
	}

	public void setKodeFakturAsal(String kodeFakturAsal) {
		this.kodeFakturAsal = kodeFakturAsal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public Penyedia getSupplier() {
		return supplier;
	}

	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	public Double getQty() {
		return qty == null ? 0.0 : qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Double getHargaSatuan() {
		return hargaSatuan == null ? 0.0 : hargaSatuan;
	}

	public void setHargaSatuan(Double hargaSatuan) {
		this.hargaSatuan = hargaSatuan;
	}

	public Double getTotalNilai() {
		if (totalNilai == null || totalNilai == 0.0) {
			totalNilai = getQty() * getHargaSatuan();
		}
		return totalNilai;
	}

	public void setTotalNilai(Double totalNilai) {
		this.totalNilai = totalNilai;
	}

	public String getAlasan() {
		return alasan;
	}

	public void setAlasan(String alasan) {
		this.alasan = alasan;
	}

	@Column(columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		this.oleh = oleh;
	}


	/**
	 * Penanda jurnal. Jurnal retur beli: debet Utang Supplier/Kas, kredit Persediaan. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_history", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
