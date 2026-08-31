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

/**
 * Model data untuk pengadaan produk. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code Produk produk}, {@code
 * Toko toko}, {@code String nomorFaktur}, {@code String namaSupplier}, {@code PenyediaAsset supplier}, {@code
 * PengadaanFaktur fakturPengadaan}, {@code Double qty}; pemetaan persistence: tabel {@code
 * koperasi.pengadaan_produk}; pembacaan/pencarian ({@code getId()}, {@code getProduk()}, {@code getToko()},
 * {@code getNomorFaktur()}, {@code getNamaSupplier()}, {@code getQty()}); mutasi data ({@code onUpdate()},
 * {@code setId()}, {@code setProduk()}, {@code setToko()}, {@code setNomorFaktur()}, {@code setNamaSupplier()}).
 * Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
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
	// Snapshot UOM pada saat dokumen disimpan. qty/hargaBeliSatuan tetap dalam
	// satuan stok/dasar agar seluruh rumus stok dan HPP lama tetap kompatibel.
	private SatuanProduk satuanInput;
	private Double qtyInput;
	private Double faktorKonversi;
	private Double hargaBeliSatuanInput;

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

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_input", nullable = true)
	public SatuanProduk getSatuanInput() {
		satuanInput = check(satuanInput);
		return satuanInput;
	}

	public void setSatuanInput(SatuanProduk satuanInput) {
		this.satuanInput = satuanInput;
	}

	@Column(name = "qty_input")
	public Double getQtyInput() {
		return qtyInput == null ? getQty() : qtyInput;
	}

	public void setQtyInput(Double qtyInput) {
		this.qtyInput = qtyInput;
	}

	@Column(name = "faktor_konversi")
	public Double getFaktorKonversi() {
		return faktorKonversi == null || faktorKonversi.doubleValue() <= 0.0 ? Double.valueOf(1.0) : faktorKonversi;
	}

	public void setFaktorKonversi(Double faktorKonversi) {
		this.faktorKonversi = faktorKonversi;
	}

	@Column(name = "harga_beli_satuan_input")
	public Double getHargaBeliSatuanInput() {
		return hargaBeliSatuanInput == null ? getHargaBeliSatuan() : hargaBeliSatuanInput;
	}

	public void setHargaBeliSatuanInput(Double hargaBeliSatuanInput) {
		this.hargaBeliSatuanInput = hargaBeliSatuanInput;
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


	/**
	 * Penanda jurnal. Jurnal kulakan: debet Persediaan, kredit Utang Supplier/Kas. Diisi saat baris ini diposting ke buku besar; dipakai
	 * sebagai kunci anti-posting-ganda dan jejak balik dari jurnal ke dokumen sumbernya.
	 * Kolomnya dibuat otomatis oleh Hibernate.
	 */
	private ais.database.model.akunting.PostingHistory postingHistory;

	@javax.persistence.ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@javax.persistence.JoinColumn(name = "posting_pembelian", nullable = true)
	public ais.database.model.akunting.PostingHistory getPostingHistory() {
		return postingHistory;
	}

	public void setPostingHistory(ais.database.model.akunting.PostingHistory postingHistory) {
		this.postingHistory = postingHistory;
	}

}
