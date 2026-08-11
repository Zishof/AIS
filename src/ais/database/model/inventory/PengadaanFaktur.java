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
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;
import ais.database.model.library.Penyedia;

/**
 * Kulakan per-Faktur (gap-closure permintaan user 2026-08-11) -- header SATU faktur/nota supplier,
 * diisi SEKALI (nomor faktur, tanggal, supplier), diikuti banyak baris {@link PengadaanProduk}
 * (lewat FK baru {@code PengadaanProduk.fakturPengadaan}) di bawahnya -- sebelumnya setiap baris
 * produk sepenuhnya independen, mengetik ulang nomor faktur/supplier sendiri-sendiri tanpa validasi
 * konsistensi apa pun.
 *
 * <p>Supplier menunjuk {@link Penyedia} (bukan {@link ais.database.model.asset.PenyediaAsset} yang
 * SEBELUMNYA dipakai {@code PengadaanProduk.supplier} utk form JSP lama) -- permintaan eksplisit
 * user ("ambil dari class Penyedia"); {@code PengadaanProduk.supplier} (PenyediaAsset) TIDAK diubah,
 * TETAP ada apa adanya utk data lama/jalur lama, header baru ini adalah sumber kebenaran BARU utk
 * entri lewat alur Faktur.</p>
 *
 * <p>{@code totalFakturManual} SENGAJA nullable -- {@code null} berarti kasir/admin tidak
 * mengisinya, total dianggap = jumlah baris ({@code totalHitungSaatSimpan}), {@code diskon = 0}.
 * Diisi HANYA bila nilai di nota fisik LEBIH KECIL dari jumlah hitungan baris (kelebihan/potongan
 * dari supplier) -- {@code diskon = totalHitungSaatSimpan - totalFakturManual} (selalu &gt;= 0,
 * server yang menghitung &amp; menyimpan, bukan klien -- lihat {@code KantinHelper.kulakanFakturSimpan}).
 * Diskon dicatat di level HEADER (bukan didistribusikan ke tiap baris produk) -- pilihan desain
 * sengaja demi kesederhanaan, cukup utk kebutuhan pencatatan "potongan faktur" tanpa perlu
 * menghitung ulang harga beli per satuan tiap baris.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_faktur")
public class PengadaanFaktur extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private Toko toko;
	private Penyedia supplier;

	private String nomorFaktur;
	private Date tanggalFaktur;
	private Double totalFakturManual;
	private Double totalHitungSaatSimpan;
	private Double diskon;
	private String keterangan;

	private String oleh;
	private Date waktu;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PengadaanFaktur() {
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
	@JoinColumn(name = "toko", nullable = false)
	public Toko getToko() {
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "supplier", nullable = true)
	public Penyedia getSupplier() {
		return supplier;
	}

	public void setSupplier(Penyedia supplier) {
		this.supplier = supplier;
	}

	@Column(name = "nomor_faktur")
	public String getNomorFaktur() {
		return nomorFaktur;
	}

	public void setNomorFaktur(String nomorFaktur) {
		this.nomorFaktur = nomorFaktur;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_faktur")
	public Date getTanggalFaktur() {
		return tanggalFaktur == null ? ais.ui.util.WaktuUtil.getDate() : tanggalFaktur;
	}

	public void setTanggalFaktur(Date tanggalFaktur) {
		this.tanggalFaktur = tanggalFaktur;
	}

	@Column(name = "total_faktur_manual")
	public Double getTotalFakturManual() {
		return totalFakturManual;
	}

	public void setTotalFakturManual(Double totalFakturManual) {
		this.totalFakturManual = totalFakturManual;
	}

	@Column(name = "total_hitung_saat_simpan")
	public Double getTotalHitungSaatSimpan() {
		return totalHitungSaatSimpan == null ? 0.0 : totalHitungSaatSimpan;
	}

	public void setTotalHitungSaatSimpan(Double totalHitungSaatSimpan) {
		this.totalHitungSaatSimpan = totalHitungSaatSimpan;
	}

	@Column(name = "diskon")
	public Double getDiskon() {
		return diskon == null ? 0.0 : diskon;
	}

	public void setDiskon(Double diskon) {
		this.diskon = diskon;
	}

	/**
	 * Total faktur final -- manual bila diisi, jika tidak = jumlah hitungan baris. {@code @Transient}
	 * WAJIB: tanpa ini Hibernate menganggapnya kolom persisten (implicit-naming ->
	 * {@code totalfakturfinal}) yang tak pernah ada di skema {@code koperasi.pengadaan_faktur},
	 * bikin hbm2ddl gagal validasi/rebuild setiap start (root-cause bug identik pernah terjadi di
	 * {@code PembelianAnggotaKoperasi.getNominalBayar1()}).
	 */
	@Transient
	public Double getTotalFakturFinal() {
		return totalFakturManual == null ? getTotalHitungSaatSimpan() : totalFakturManual;
	}

	public void setTotalFakturFinal(Double totalFakturFinal) {
		// Nilai final dihitung implisit dari totalFakturManual/totalHitungSaatSimpan.
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
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
