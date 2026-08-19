package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
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
 * Baris barang pada {@link PengadaanPermintaanPos} (PR versi POS).
 *
 * <p>Padanan {@code PermintaanPengadaanMasterAssetDetail} pada versi ZKoss, dengan satu
 * perbedaan pokok: barang ditunjuk lewat {@link Produk} (katalog POS), bukan {@code MasterAsset}.
 * Ini yang memungkinkan hasil penerimaan barang nanti disinkronkan langsung ke Kulakan/stok
 * tanpa pemetaan tambahan.</p>
 *
 * <p>{@code jumlahDatang} dipakai tahap Penerimaan (BAST) untuk menandai realisasi tiap baris --
 * semantik sama dengan versi umum, sehingga PR dapat menunjukkan sisa yang belum datang.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_permintaan_pos_detail")
public class PengadaanPermintaanPosDetail extends GeneralValueObject {

	private static final long serialVersionUID = 4821577548439811002L;

	private Long id;
	private PengadaanPermintaanPos permintaan;
	private Produk produk;
	private Double jumlah;
	private Double hargaBeli;
	private Double hargaTotal;
	private Double jumlahDatang;
	private String keterangan;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	public PengadaanPermintaanPosDetail() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "permintaan", nullable = true)
	public PengadaanPermintaanPos getPermintaan() {
		return permintaan;
	}

	public void setPermintaan(PengadaanPermintaanPos permintaan) {
		this.permintaan = permintaan;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "produk", nullable = true)
	public Produk getProduk() {
		return produk;
	}

	public void setProduk(Produk produk) {
		this.produk = produk;
	}

	@Column(name = "jumlah", nullable = true)
	public Double getJumlah() {
		return jumlah;
	}

	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	@Column(name = "hargabeli", nullable = true)
	public Double getHargaBeli() {
		return hargaBeli;
	}

	public void setHargaBeli(Double hargaBeli) {
		this.hargaBeli = hargaBeli;
	}

	@Column(name = "hargatotal", nullable = true)
	public Double getHargaTotal() {
		return hargaTotal;
	}

	public void setHargaTotal(Double hargaTotal) {
		this.hargaTotal = hargaTotal;
	}

	/** Realisasi datang (diisi tahap BAST); sisa = jumlah - jumlahDatang. */
	@Column(name = "jumlah_datang", nullable = true)
	public Double getJumlahDatang() {
		return jumlahDatang == null ? Double.valueOf(0) : jumlahDatang;
	}

	public void setJumlahDatang(Double jumlahDatang) {
		this.jumlahDatang = jumlahDatang;
	}

	@Column(name = "keterangan", nullable = true)
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
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
