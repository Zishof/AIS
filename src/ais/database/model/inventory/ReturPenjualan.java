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
import ais.database.model.koperasi.AnggotaKoperasi;

/**
 * Retur Penjualan -- barang yang dikembalikan PELANGGAN (rusak/salah kirim/tidak sesuai/dll),
 * TERPISAH dari {@link ReturBarang} (barang dikembalikan KE SUPPLIER, bagian alur Kulakan/Pengadaan).
 * Satu baris = satu produk yang diretur dari SATU transaksi penjualan asal ({@link
 * #getPembelianAnggotaKoperasiId()}) -- pola SAMA PERSIS {@link PengadaanProduk} (satu baris per SKU
 * per peristiwa, BUKAN header+item terpisah): retur 3 produk dari satu struk = 3 baris, semua
 * menunjuk {@code pembelianAnggotaKoperasiId} yang sama, tetap bisa dikelompokkan lewat kolom itu
 * utk laporan/tampilan.
 *
 * <p><b>Kenapa {@code pembelianAnggotaKoperasiId} bukan relasi JPA {@code @ManyToOne}</b> -- pola
 * sama {@code PembatalanTransaksiKantin.pembelianAnggotaKoperasiId}: transaksi penjualan LAMA/berdiri
 * sendiri (baris {@code koperasi.pembelian} tanpa header kelompok, lihat
 * {@code PosApi.prosesDetailTransaksi}) tidak selalu punya baris {@code pembelian_anggota_koperasi}
 * yang valid utk di-FK -- disimpan sebagai id polos + {@link #getKodeTransaksiAsal()} (salinan nomor
 * nota, utk tampilan tanpa perlu join) supaya retur tetap bisa dicatat thd transaksi jenis apa pun.</p>
 *
 * <p><b>Stok HANYA bertambah bila {@link #getKembalikanKeStok()} true</b> -- barang kondisi
 * rusak/tak layak jual TIDAK otomatis balik ke stok jual (praktik retail baku: barang rusak masuk
 * kategori write-off, bukan sellable inventory). Lihat {@code StokKantinUtil.recomputeStokProduk}
 * &amp; {@code formulaStokSql} (suku ke-5, ditambahkan bersamaan dgn entity ini) utk formula lengkap.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "retur_penjualan")
public class ReturPenjualan extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Produk produk;
	private Toko toko;

	private Long pembelianAnggotaKoperasiId;
	/** ID baris koperasi.pembelian yang benar-benar dikembalikan. */
	private Long pembelianId;
	private String kodeTransaksiAsal;
	private AnggotaKoperasi anggotaKoperasi;
	private String namaPembeli;

	private Double qty;
	private Double hargaSatuan;
	private Double totalNilai;

	private String alasan;
	private String kondisiBarang;
	private Boolean kembalikanKeStok;
	private String metodePengembalian;
	private String keterangan;

	private Date waktu;
	private String oleh;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public ReturPenjualan() {
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

	@Column(name = "pembelian_anggota_koperasi_id", nullable = true)
	public Long getPembelianAnggotaKoperasiId() {
		return pembelianAnggotaKoperasiId;
	}

	public void setPembelianAnggotaKoperasiId(Long pembelianAnggotaKoperasiId) {
		this.pembelianAnggotaKoperasiId = pembelianAnggotaKoperasiId;
	}

	@Column(name = "pembelian_id", nullable = true)
	public Long getPembelianId() {
		return pembelianId;
	}

	public void setPembelianId(Long pembelianId) {
		this.pembelianId = pembelianId;
	}

	public String getKodeTransaksiAsal() {
		return kodeTransaksiAsal;
	}

	public void setKodeTransaksiAsal(String kodeTransaksiAsal) {
		this.kodeTransaksiAsal = kodeTransaksiAsal;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "anggota_koperasi", nullable = true)
	public AnggotaKoperasi getAnggotaKoperasi() {
		anggotaKoperasi = check(anggotaKoperasi);
		return anggotaKoperasi;
	}

	public void setAnggotaKoperasi(AnggotaKoperasi anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	public String getNamaPembeli() {
		return namaPembeli;
	}

	public void setNamaPembeli(String namaPembeli) {
		this.namaPembeli = namaPembeli;
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

	public String getKondisiBarang() {
		return kondisiBarang;
	}

	public void setKondisiBarang(String kondisiBarang) {
		this.kondisiBarang = kondisiBarang;
	}

	/**
	 * Apakah qty retur ini ditambahkan kembali ke stok jual -- default {@code true} (kebanyakan retur
	 * BUKAN karena barang rusak: salah pilih/ukuran/berubah pikiran, barang masih layak jual). Petugas
	 * WAJIB melepas centang ini secara eksplisit utk retur barang rusak/tak layak jual, supaya barang
	 * tsb tidak diam-diam kembali ke stok yang bisa dijual lagi ke pelanggan lain.
	 */
	@Column(name = "kembalikan_ke_stok", nullable = true)
	public Boolean getKembalikanKeStok() {
		return kembalikanKeStok == null ? true : kembalikanKeStok;
	}

	public void setKembalikanKeStok(Boolean kembalikanKeStok) {
		this.kembalikanKeStok = kembalikanKeStok;
	}

	public String getMetodePengembalian() {
		return metodePengembalian;
	}

	public void setMetodePengembalian(String metodePengembalian) {
		this.metodePengembalian = metodePengembalian;
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
	 * Penanda jurnal. Jurnal retur jual: debet Pendapatan, kredit Kas/Piutang (+ balik HPP bila masuk stok). Diisi saat baris ini diposting ke buku besar; dipakai
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
