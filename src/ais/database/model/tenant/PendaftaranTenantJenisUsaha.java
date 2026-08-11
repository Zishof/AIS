package ais.database.model.tenant;

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
import javax.persistence.UniqueConstraint;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Pilihan jenis usaha satu permohonan tenant (join table = SUMBER KEBENARAN multi-select).</h3>
 *
 * <p>{@code Pendaftar.jenisBisnis} existing HANYA diisi sebagai compatibility snapshot (kode
 * primary, atau daftar kode dipisah koma) -- SEMUA logika membaca pilihan jenis usaha WAJIB
 * dari tabel ini, bukan dari kolom snapshot itu (invariant #5 ERD). Minimal satu baris per
 * permohonan; unique (permohonan, jenis usaha) menolak duplikat.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pendaftaran_tenant_jenis_usaha",
		uniqueConstraints = @UniqueConstraint(columnNames = { "pendaftaran_tenant_id", "jenis_usaha_tenant_id" }))
public class PendaftaranTenantJenisUsaha extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private PendaftaranTenant pendaftaranTenant;
	private JenisUsahaTenant jenisUsahaTenant;
	private Boolean primaryChoice;
	private String otherDescription;
	private Date createdAt;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public PendaftaranTenantJenisUsaha() {
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
	@JoinColumn(name = "pendaftaran_tenant_id", nullable = false)
	public PendaftaranTenant getPendaftaranTenant() {
		return pendaftaranTenant;
	}

	public void setPendaftaranTenant(PendaftaranTenant pendaftaranTenant) {
		this.pendaftaranTenant = pendaftaranTenant;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_usaha_tenant_id", nullable = false)
	public JenisUsahaTenant getJenisUsahaTenant() {
		return jenisUsahaTenant;
	}

	public void setJenisUsahaTenant(JenisUsahaTenant jenisUsahaTenant) {
		this.jenisUsahaTenant = jenisUsahaTenant;
	}

	/** True pada tepat satu baris per permohonan -- jenis usaha utama (dipakai snapshot jenisBisnis). */
	@Column(name = "primary_choice")
	public Boolean getPrimaryChoice() {
		return primaryChoice == null ? Boolean.FALSE : primaryChoice;
	}

	public void setPrimaryChoice(Boolean primaryChoice) {
		this.primaryChoice = primaryChoice;
	}

	/** Wajib diisi bila jenis usaha = LAINNYA (validasi server, bukan sekadar UI). */
	@Column(name = "other_description", length = 500)
	public String getOtherDescription() {
		return otherDescription;
	}

	public void setOtherDescription(String otherDescription) {
		this.otherDescription = otherDescription;
	}

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
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
