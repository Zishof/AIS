package ais.database.model.file;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.sql.Blob;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/**
 * Konten biner jurnal pada database streaming. Entitas ini sengaja tidak
 * mewarisi LampiranLain dan tidak mempunyai relasi ORM lintas SessionFactory.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "lampiran_jurnal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lampiran_jurnal_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_lampiran_jurnal_bitstream", columnNames = "repo_bitstream_id") })
public class LampiranJurnal implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long repoBitstreamId;
    private Blob content;
    private String originalFileName;
    private String declaredMimeType;
    private String detectedMimeType;
    private Long declaredSize;
    private Long actualSize;
    private String checksumSha256;
    private String journalStage;
    private Long fileVersion;
    private String storageState;
    private String scanState;
    private String quarantineState;
    private String idempotencyKey;
    private String createdBy;
    private String updatedBy;
    private Date createdAt = new Date();
    private Date updatedAt = new Date();

    public LampiranJurnal() {}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "repo_bitstream_id", nullable = false)
    public Long getRepoBitstreamId() { return repoBitstreamId; }
    public void setRepoBitstreamId(Long repoBitstreamId) { this.repoBitstreamId = repoBitstreamId; }

    @Column(name = "file_content", nullable = false)
    public Blob getContent() { return content; }
    public void setContent(Blob content) { this.content = content; }

    @Column(name = "original_file_name", nullable = false, length = 255)
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    @Column(name = "declared_mime_type", nullable = false, length = 100)
    public String getDeclaredMimeType() { return declaredMimeType; }
    public void setDeclaredMimeType(String declaredMimeType) { this.declaredMimeType = declaredMimeType; }

    @Column(name = "detected_mime_type", nullable = false, length = 100)
    public String getDetectedMimeType() { return detectedMimeType; }
    public void setDetectedMimeType(String detectedMimeType) { this.detectedMimeType = detectedMimeType; }

    @Column(name = "declared_size", nullable = false)
    public Long getDeclaredSize() { return declaredSize; }
    public void setDeclaredSize(Long declaredSize) { this.declaredSize = declaredSize; }

    @Column(name = "actual_size")
    public Long getActualSize() { return actualSize; }
    public void setActualSize(Long actualSize) { this.actualSize = actualSize; }

    @Column(name = "checksum_sha256", length = 64)
    public String getChecksumSha256() { return checksumSha256; }
    public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }

    @Column(name = "journal_stage", nullable = false, length = 60)
    public String getJournalStage() { return journalStage; }
    public void setJournalStage(String journalStage) { this.journalStage = journalStage; }

    @Column(name = "file_version", nullable = false)
    public Long getFileVersion() { return fileVersion; }
    public void setFileVersion(Long fileVersion) { this.fileVersion = fileVersion; }

    @Column(name = "storage_state", nullable = false, length = 40)
    public String getStorageState() { return storageState; }
    public void setStorageState(String storageState) { this.storageState = storageState; }

    @Column(name = "scan_state", nullable = false, length = 30)
    public String getScanState() { return scanState; }
    public void setScanState(String scanState) { this.scanState = scanState; }

    @Column(name = "quarantine_state", nullable = false, length = 30)
    public String getQuarantineState() { return quarantineState; }
    public void setQuarantineState(String quarantineState) { this.quarantineState = quarantineState; }

    @Column(name = "idempotency_key", nullable = false, length = 160)
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    @Column(name = "created_by", nullable = false, length = 100)
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Column(name = "updated_by", nullable = false, length = 100)
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
