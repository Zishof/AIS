package ais.database.model.kursus;

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
 * Kode kupon diskon untuk checkout ProdukKursus. produkKursus null = berlaku untuk semua kursus.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kupon_kursus")
public class KuponKursus extends GeneralValueObject {

	public final static String PERSEN = "Persen";
	public final static String NOMINAL = "Nominal";

	private static final long serialVersionUID = 2463821577548439808L;
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
		return id + "-" + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private String tipeDiskon;
	private Double nilai;
	private Date berlakuMulai;
	private Date berlakuSampai;
	private Integer batasPemakaian;
	private Integer jumlahDipakai;
	private ProdukKursus produkKursus;
	private Boolean aktif;

	public KuponKursus() {
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

	@Column(name = "kode", nullable = false, unique = true, length = 100)
	public String getKode() {
		return kode == null ? "" : kode.trim().toUpperCase();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "tipe_diskon", nullable = true, length = 50)
	public String getTipeDiskon() {
		return tipeDiskon == null || tipeDiskon.isEmpty() ? PERSEN : tipeDiskon;
	}

	public void setTipeDiskon(String tipeDiskon) {
		this.tipeDiskon = tipeDiskon;
	}

	public Double getNilai() {
		return nilai == null ? 0.0 : nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	@Temporal(TemporalType.DATE)
	public Date getBerlakuMulai() {
		return berlakuMulai;
	}

	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	@Temporal(TemporalType.DATE)
	public Date getBerlakuSampai() {
		return berlakuSampai;
	}

	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

	public Integer getBatasPemakaian() {
		return batasPemakaian;
	}

	public void setBatasPemakaian(Integer batasPemakaian) {
		this.batasPemakaian = batasPemakaian;
	}

	public Integer getJumlahDipakai() {
		return jumlahDipakai == null ? 0 : jumlahDipakai;
	}

	public void setJumlahDipakai(Integer jumlahDipakai) {
		this.jumlahDipakai = jumlahDipakai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = true)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public boolean berlakuUntuk(Date sekarang) {
		if (sekarang == null) {
			sekarang = new Date();
		}
		if (berlakuMulai != null && sekarang.before(berlakuMulai)) {
			return false;
		}
		if (berlakuSampai != null && sekarang.after(berlakuSampai)) {
			return false;
		}
		if (batasPemakaian != null && getJumlahDipakai() >= batasPemakaian) {
			return false;
		}
		return getAktif();
	}

}
