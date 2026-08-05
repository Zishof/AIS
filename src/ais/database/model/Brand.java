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

	public Brand() {
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

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pendaftar", nullable = false)
	public Pendaftar getPendaftar() {
		pendaftar = check(pendaftar);
		return pendaftar;
	}

	public void setPendaftar(Pendaftar pendaftar) {
		this.pendaftar = pendaftar;
	}

	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "dibuat_pada")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	public String toString() {
		return id + "-" + nama;
	}
}
