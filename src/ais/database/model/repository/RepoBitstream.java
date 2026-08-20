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
@Table(schema = "public", name = "repo_bitstream")
public class RepoBitstream extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long itemId;
    private String namaFile;
    private String mimeType;     
    private String pathSistem;   
    private Long ukuranByte;
    private String checksum;     
    private String bundleName;
    private String description;
    private String accessPolicy;
    private String sourceClass;
    private Long sourceId;
    private String dspaceUuid;
    private Boolean primaryFile;
    private Boolean turnitinSubmitted;
    private Date turnitinSubmittedAt;
    private String virusScanStatus;
    private Date virusScannedAt;
    private Boolean signatureValid;
    private Long fileVersion;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    public RepoBitstream() {}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return this.itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    @Column(name = "nama_file", nullable = false, length = 255)
    public String getNamaFile() { return this.namaFile; }
    public void setNamaFile(String namaFile) { this.namaFile = namaFile; }

    @Column(name = "mime_type", length = 100)
    public String getMimeType() { return this.mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    @Column(name = "path_sistem", nullable = false, length = 500)
    public String getPathSistem() { return this.pathSistem; }
    public void setPathSistem(String pathSistem) { this.pathSistem = pathSistem; }

    @Column(name = "ukuran_byte")
    public Long getUkuranByte() { return this.ukuranByte; }
    public void setUkuranByte(Long ukuranByte) { this.ukuranByte = ukuranByte; }

    @Column(name = "checksum", length = 64)
    public String getChecksum() { return this.checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    @Column(name = "bundle_name", length = 80)
    public String getBundleName() { return bundleName == null ? "ORIGINAL" : bundleName.trim(); }
    public void setBundleName(String bundleName) { this.bundleName = bundleName; }

    @Column(name = "description", columnDefinition = "TEXT")
    public String getDescription() { return description == null ? "" : description.trim(); }
    public void setDescription(String description) { this.description = description; }

    @Column(name = "access_policy", length = 40)
    public String getAccessPolicy() { return accessPolicy == null ? "OPEN_ACCESS" : accessPolicy.trim(); }
    public void setAccessPolicy(String accessPolicy) { this.accessPolicy = accessPolicy; }

    @Column(name = "source_class", length = 255)
    public String getSourceClass() { return sourceClass == null ? null : sourceClass.trim(); }
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    @Column(name = "source_id")
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    @Column(name = "primary_file")
    public Boolean getPrimaryFile() { return primaryFile == null ? false : primaryFile; }
    public void setPrimaryFile(Boolean primaryFile) { this.primaryFile = primaryFile; }

    @Column(name = "turnitin_submitted")
    public Boolean getTurnitinSubmitted() { return turnitinSubmitted == null ? false : turnitinSubmitted; }
    public void setTurnitinSubmitted(Boolean turnitinSubmitted) { this.turnitinSubmitted = turnitinSubmitted; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "turnitin_submitted_at")
    public Date getTurnitinSubmittedAt() { return turnitinSubmittedAt; }
    public void setTurnitinSubmittedAt(Date turnitinSubmittedAt) { this.turnitinSubmittedAt = turnitinSubmittedAt; }

    /* Nullable pada skema agar hbm2ddl dapat menambah kolom ke tabel lama berisi data. */
    @Column(name = "virus_scan_status", length = 30)
    public String getVirusScanStatus() { return virusScanStatus == null ? "PENDING" : virusScanStatus.trim(); }
    public void setVirusScanStatus(String virusScanStatus) { this.virusScanStatus = virusScanStatus; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "virus_scanned_at")
    public Date getVirusScannedAt() { return virusScannedAt; }
    public void setVirusScannedAt(Date virusScannedAt) { this.virusScannedAt = virusScannedAt; }

    @Column(name = "signature_valid")
    public Boolean getSignatureValid() { return signatureValid == null ? Boolean.FALSE : signatureValid; }
    public void setSignatureValid(Boolean signatureValid) { this.signatureValid = signatureValid; }

    @Column(name = "file_version")
    public Long getFileVersion() { return fileVersion == null ? Long.valueOf(1L) : fileVersion; }
    public void setFileVersion(Long fileVersion) { this.fileVersion = fileVersion; }

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
        return id + "-" + namaFile;
    }
}
