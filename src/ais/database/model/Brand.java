package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

/**
 * <b>Brand</b> -- sub-merek milik satu {@link Pendaftar} ebisnis.id. Satu
 * Pendaftar boleh punya banyak Brand; satu Brand menaungi banyak
 * {@code ais.database.model.inventory.Toko} (lihat kolom {@code brand}
 * baru pada {@code Toko}).
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "brand")
public class Brand extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String nama;
	private Pendaftar pendaftar;
	private Boolean aktif;
	private Date dibuatPada;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public Brand() {
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah}
	 * setiap kali baris ini di-update. Field ini adalah kebutuhan teknis
	 * (dipakai {@code AuditTimestampInterceptor}), bukan celah/bug.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return id unik brand (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik brand.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama brand, di-trim (tanpa spasi awal/akhir) saat dibaca.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama nama brand yang akan disimpan.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return {@link Pendaftar} ebisnis.id pemilik brand ini — batas kepemilikan/tenant
	 *         utama entitas ini; relasi lazy, di-"check" (dilewatkan proxy-safe helper)
	 *         sebelum dikembalikan.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	/**
	 * @param pendaftar {@link Pendaftar} pemilik brand ini.
	 */
	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	/**
	 * @return status aktif brand; default {@code true} bila belum pernah diisi
	 *         (flag aktif satu-arah — baris lama tanpa nilai eksplisit dianggap aktif).
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * @param aktif status aktif/nonaktif brand.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return waktu brand ini dibuat.
	 */
	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	/**
	 * @param dibuatPada waktu pembuatan brand.
	 */
	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}
}
