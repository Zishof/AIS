package ais.database.model.repository;

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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "repo_collection")
public class RepoCollection extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String tenantKey;
    private String kode;
    private String nama;
    private String deskripsi;
    private Long parentId;
    private String tipe;
    private String sourceSystem;
    private String dspaceUuid;
    private String dspaceHandle;
    private Integer sortOrder;
    private String metadataProfileJson;
    private String workflowProfileJson;
    private String accessPolicyJson;
    private String defaultLicenseUri;
    private Boolean depositEnabled;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    public RepoCollection() {}

    @Column(name="tenant_key",length=120)
    public String getTenantKey(){return tenantKey==null?"":tenantKey.trim();}
    public void setTenantKey(String tenantKey){this.tenantKey=tenantKey;}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "kode", length = 50)
    public String getKode() { return kode == null ? null : kode.trim(); }
    public void setKode(String kode) { this.kode = kode; }

    @Column(name = "nama", nullable = false, length = 255)
    public String getNama() { return this.nama == null ? null : this.nama.trim(); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "deskripsi", columnDefinition = "TEXT")
    public String getDeskripsi() { return this.deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    @Column(name = "parent_id")
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    @Column(name = "tipe", length = 50)
    public String getTipe() { return tipe == null ? "COLLECTION" : tipe.trim(); }
    public void setTipe(String tipe) { this.tipe = tipe; }

    @Column(name = "source_system", length = 50)
    public String getSourceSystem() { return sourceSystem == null ? "AIS" : sourceSystem.trim(); }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    @Column(name = "dspace_handle", length = 255)
    public String getDspaceHandle() { return dspaceHandle == null ? "" : dspaceHandle.trim(); }
    public void setDspaceHandle(String dspaceHandle) { this.dspaceHandle = dspaceHandle; }

    @Column(name = "sort_order")
    public Integer getSortOrder() { return sortOrder == null ? Integer.valueOf(0) : sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    @Column(name = "metadata_profile_json", columnDefinition = "TEXT")
    public String getMetadataProfileJson() { return metadataProfileJson == null ? "{}" : metadataProfileJson; }
    public void setMetadataProfileJson(String metadataProfileJson) { this.metadataProfileJson = metadataProfileJson; }

    @Column(name = "workflow_profile_json", columnDefinition = "TEXT")
    public String getWorkflowProfileJson() { return workflowProfileJson == null ? "{}" : workflowProfileJson; }
    public void setWorkflowProfileJson(String workflowProfileJson) { this.workflowProfileJson = workflowProfileJson; }

    @Column(name = "access_policy_json", columnDefinition = "TEXT")
    public String getAccessPolicyJson() { return accessPolicyJson == null ? "{}" : accessPolicyJson; }
    public void setAccessPolicyJson(String accessPolicyJson) { this.accessPolicyJson = accessPolicyJson; }

    @Column(name = "default_license_uri", length = 500)
    public String getDefaultLicenseUri() { return defaultLicenseUri == null ? "" : defaultLicenseUri.trim(); }
    public void setDefaultLicenseUri(String defaultLicenseUri) { this.defaultLicenseUri = defaultLicenseUri; }

    /*
     * Harus nullable pada DDL: instalasi lama sudah memiliki baris. Hibernate 3
     * menambahkan kolom baru tanpa DEFAULT; nullable=false membuat PostgreSQL
     * menolak ADD COLUMN sehingga aplikasi kemudian gagal saat SELECT.
     * Default bisnis tetap TRUE melalui getter dan data lama dibackfill oleh
     * migrasi post-Hibernate.
     */
    @Column(name = "deposit_enabled")
    public Boolean getDepositEnabled() { return depositEnabled == null ? Boolean.TRUE : depositEnabled; }
    public void setDepositEnabled(Boolean depositEnabled) { this.depositEnabled = depositEnabled; }

    // --- Audit Methods ---
    public String getOlehId() { return olehId; }
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    public String getOleh() { return oleh; }
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    public Boolean getAktif() { return aktif == null ? true : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    public String toString() {
        return id + "-" + nama;
    }
}
