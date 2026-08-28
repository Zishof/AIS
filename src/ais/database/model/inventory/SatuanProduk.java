package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Satuan (unit-of-measure) katalog Produk POS (Pcs, Liter, Kg, dst) -- master ringan khusus fitur
 * impor/ekspor Excel katalog barang di layar Produk Kasir Desktop/Android, lihat JavaDoc
 * {@code KantinHelper.produkImporExcel}. Tidak ada master satuan bersama sebelumnya di modul
 * inventory ({@code rab.Satuan}/{@code asset.SatuanMasterAsset} milik modul lain, tidak dipakai
 * ulang di sini supaya tidak menaut lintas-modul yang tak berkaitan).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "satuan_produk")
public class SatuanProduk extends GeneralValueObject {

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

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return nama == null ? "" : nama;
	}

	private String nama;
	private Boolean aktif;
	private String kategori;
	private String tipeKonversi;
	private Double rasio;
	private Double presisiPembulatan;

	public SatuanProduk() {
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

	@Column(name = "nama", nullable = false, length = 100)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "kategori", nullable = true, length = 50)
	public String getKategori() {
		return kategori == null || kategori.trim().isEmpty() ? "UNIT" : kategori.trim().toUpperCase();
	}

	public void setKategori(String kategori) {
		this.kategori = kategori;
	}

	@Column(name = "tipe_konversi", nullable = true, length = 20)
	public String getTipeKonversi() {
		return tipeKonversi == null || tipeKonversi.trim().isEmpty() ? "REFERENCE" : tipeKonversi.trim().toUpperCase();
	}

	public void setTipeKonversi(String tipeKonversi) {
		this.tipeKonversi = tipeKonversi;
	}

	@Column(name = "rasio", nullable = true)
	public Double getRasio() {
		return rasio == null || rasio.doubleValue() <= 0.0 ? Double.valueOf(1.0) : rasio;
	}

	public void setRasio(Double rasio) {
		this.rasio = rasio;
	}

	@Column(name = "presisi_pembulatan", nullable = true)
	public Double getPresisiPembulatan() {
		return presisiPembulatan == null || presisiPembulatan.doubleValue() <= 0.0
				? Double.valueOf(0.01) : presisiPembulatan;
	}

	public void setPresisiPembulatan(Double presisiPembulatan) {
		this.presisiPembulatan = presisiPembulatan;
	}

}
