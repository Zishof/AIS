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

import ais.common.BarcodeCommon;
import ais.database.model.GeneralValueObject;

/**
 * Review/rating (1-5) yang diberikan seorang peserta untuk sebuah ProdukKursus.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "ulasan_kursus")
public class UlasanKursus extends GeneralValueObject {

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
		return kode + " - " + nama;
	}

	private String kode;
	private String nama;
	private String keterangan;
	private ProdukKursus produkKursus;
	private PesertaKursus pesertaKursus;
	private Integer rating;
	private String komentar;
	private Date tanggal;
	private Boolean aktif;

	public UlasanKursus() {
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

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(unique = true)
	public String getKode() {
		if (kode == null) {
			kode = BarcodeCommon.generateCode();
		}
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	public String getNama() {
		if (getProdukKursus() != null && getPesertaKursus() != null) {
			nama = pesertaKursus.getNama() + " - " + produkKursus.getNama();
		}
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "produk_kursus", nullable = false)
	public ProdukKursus getProdukKursus() {
		produkKursus = check(produkKursus);
		return produkKursus;
	}

	public void setProdukKursus(ProdukKursus produkKursus) {
		this.produkKursus = produkKursus;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_kursus", nullable = false)
	public PesertaKursus getPesertaKursus() {
		pesertaKursus = check(pesertaKursus);
		return pesertaKursus;
	}

	public void setPesertaKursus(PesertaKursus pesertaKursus) {
		this.pesertaKursus = pesertaKursus;
	}

	public Integer getRating() {
		return rating == null ? 5 : rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	@Column(name = "komentar", nullable = true, columnDefinition = "text")
	public String getKomentar() {
		return komentar;
	}

	public void setKomentar(String komentar) {
		this.komentar = komentar;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? new Date() : tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
