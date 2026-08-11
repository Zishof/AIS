package ais.database.model.tenant;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * <h3>Katalog Jenis Usaha/Instansi tenant (control-plane, schema public).</h3>
 *
 * <p>Sumber pilihan multi-select pada wizard {@code Common.ROOT + "/pendaftaran"} --
 * form TIDAK meng-hard-code daftar jenis usaha; seed awal 14 jenis diisi idempoten oleh
 * {@code ais.service.registration.JenisUsahaTenantSeedService} dan selebihnya dikelola
 * Platform Admin. {@link #getCode()} IMMUTABLE (kunci logika/mapping), {@link #getNama()}
 * bebas diubah utk tampilan.</p>
 *
 * <p>CATATAN NAMA: kelas ini TIDAK ada hubungannya dgn {@code inventory.SetoranTenant}
 * ("tenant" = penyewa stan/kios bagi-hasil) -- "tenant" di paket ini = workspace
 * multi-tenant SaaS (lihat docs/pendaftaran-tenant/01-source-audit.md §8).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jenis_usaha_tenant")
public class JenisUsahaTenant extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String code;
	private String nama;
	private String deskripsi;
	private String icon;
	private Integer displayOrder;
	private Boolean aktif;
	private Boolean requiresManualReview;
	private String defaultModuleBundleCode;
	private Date createdAt;
	private Integer version;

	private String oleh;
	private String olehId;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	public JenisUsahaTenant() {
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

	/** Kode stabil huruf besar (mis. {@code APOTEK}, {@code INVENTORY_SALES}) -- IMMUTABLE setelah seed. */
	@Column(name = "code", unique = true, nullable = false, length = 64)
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return nama == null ? null : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "deskripsi", columnDefinition = "text")
	public String getDeskripsi() {
		return deskripsi;
	}

	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	@Column(name = "icon", length = 100)
	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	@Column(name = "display_order")
	public Integer getDisplayOrder() {
		return displayOrder == null ? Integer.valueOf(0) : displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** True = permohonan yang memilih jenis ini masuk REVIEW_PENDING (perlu persetujuan manual admin). */
	@Column(name = "requires_manual_review")
	public Boolean getRequiresManualReview() {
		return requiresManualReview == null ? Boolean.FALSE : requiresManualReview;
	}

	public void setRequiresManualReview(Boolean requiresManualReview) {
		this.requiresManualReview = requiresManualReview;
	}

	@Column(name = "default_module_bundle_code", length = 64)
	public String getDefaultModuleBundleCode() {
		return defaultModuleBundleCode;
	}

	public void setDefaultModuleBundleCode(String defaultModuleBundleCode) {
		this.defaultModuleBundleCode = defaultModuleBundleCode;
	}

	@Column(name = "created_at")
	@Temporal(TemporalType.TIMESTAMP)
	public Date getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Version
	@Column(name = "version")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
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
