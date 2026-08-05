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

import ais.database.model.GeneralValueObject;

/**
 * <h2>SurveyKepuasanPos — Rating Kepuasan Pembeli dari Layar Kedua POS.</h2>
 *
 * <p>
 * Entity BARU untuk menampung rating kepuasan (1-5) yang diisi PEMBELI sendiri lewat "Layar
 * Pelanggan" (layar kedua POS dual-monitor) setelah transaksi selesai. Dengan pendaftaran di
 * {@code hibernate.cfg.xml}, tabel {@code koperasi.survey_kepuasan_pos} DAN tabel audit Envers
 * {@code new_audit.survey_kepuasan_pos__audit} otomatis dibuat (hbm2ddl=update) saat RESTART --
 * entity BARU (bukan kolom baru pada entity lama), jadi tak butuh ALTER manual di InitIndex.java.
 * </p>
 *
 * <p>
 * Penamaan kolom mengikuti aturan proyek: field tanpa @Column ter-<i>fold</i> menjadi huruf kecil
 * tanpa underscore (mis. {@code olehId}→{@code olehid}). Kompatibel Java 1.7 / Hibernate 3.
 * </p>
 *
 * @author AIS e-Kantin (modul POS layar kedua)
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@org.hibernate.envers.Audited
@Table(schema = "koperasi", name = "survey_kepuasan_pos")
public class SurveyKepuasanPos extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private Toko toko;
	private Integer rating;
	private String catatan;
	private Date waktu;
	private String oleh;
	private String olehId;

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public SurveyKepuasanPos() {
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
	@JoinColumn(name = "toko")
	public Toko getToko() {
		toko = check(toko);
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	public Integer getRating() {
		return rating;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	@Column(columnDefinition = "text")
	public String getCatatan() {
		return catatan;
	}

	public void setCatatan(String catatan) {
		this.catatan = catatan;
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

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
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
