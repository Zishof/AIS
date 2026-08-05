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
import ais.database.model.sirs.Gudang;

/**
 * Ambang stok minimum sebuah {@link Produk} (biasanya bahan baku) PER {@link Gudang} -- fitur
 * "Purchase: notifikasi stok minimum otomatis 2 tingkat" (gap analisis PDF klien 2026-07-26).
 *
 * <p><b>Kenapa per-Gudang, bukan pakai {@link Produk#getStokMinimum()} yang sudah ada.</b>
 * {@code Produk.stokMinimum} adalah SATU angka datar dipakai murni sbg label peringatan "stok
 * menipis" di layar kasir/dashboard (tidak memicu apa pun secara otomatis) -- tidak bisa mewakili
 * kebutuhan PDF klien: ambang batas BERBEDA di tiap gudang untuk BAHAN BAKU yang SAMA (contoh
 * literal PDF: Tepung Terigu 10kg di gudang cabang vs 50kg di gudang pusat). Baris di sini SENGAJA
 * terpisah dari {@code Produk.stokMinimum} (yang tetap dipakai apa adanya utk peringatan kasir)
 * supaya tidak menimpa makna field lama itu.</p>
 *
 * <p>Dicek berkala oleh {@link ais.common.StokThresholdScheduler} -- lihat javadoc kelas itu untuk
 * alur lengkap (bagaimana ambang ini memicu {@link PengajuanPembelianGudang} otomatis).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "ambang_stok_gudang")
public class AmbangStokGudang extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		produk = getProduk();
		gudang = getGudang();
		return id + "-" + produk + "@" + gudang;
	}

	private Produk produk;
	private Gudang gudang;
	private Double ambangMinimum;
	private Boolean aktif;
	private String keterangan;

	public AmbangStokGudang() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
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
	@JoinColumn(name = "gudang", nullable = false)
	public Gudang getGudang() {
		gudang = check(gudang);
		return gudang;
	}

	public void setGudang(Gudang gudang) {
		this.gudang = gudang;
	}

	/** Ambang batas (satuan sama dgn stok Produk) -- di bawah/sama dgn nilai ini memicu pengajuan otomatis. */
	@Column(name = "ambang_minimum", nullable = false)
	public Double getAmbangMinimum() {
		return ambangMinimum == null ? 0.0 : ambangMinimum;
	}

	public void setAmbangMinimum(Double ambangMinimum) {
		this.ambangMinimum = ambangMinimum;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
