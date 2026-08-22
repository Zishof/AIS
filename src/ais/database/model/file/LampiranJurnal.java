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

/** Konten biner jurnal pada database streaming, tanpa relasi ORM lintas SessionFactory. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "lampiran_jurnal", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lampiran_jurnal_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_lampiran_jurnal_bitstream", columnNames = "repo_bitstream_id") })
public class LampiranJurnal implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, repoBitstreamId, declaredSize, actualSize, fileVersion;
    private Blob content;
    private String originalFileName, declaredMimeType, detectedMimeType, checksumSha256, journalStage;
    private String storageState, scanState, quarantineState, idempotencyKey, createdBy, updatedBy;
    private Date createdAt = new Date(), updatedAt = new Date();

    public LampiranJurnal() {}
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name="id",insertable=false,unique=true,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="repo_bitstream_id",nullable=false) public Long getRepoBitstreamId(){return repoBitstreamId;} public void setRepoBitstreamId(Long v){repoBitstreamId=v;}
    @Column(name="file_content",nullable=false) public Blob getContent(){return content;} public void setContent(Blob v){content=v;}
    @Column(name="original_file_name",nullable=false,length=255) public String getOriginalFileName(){return originalFileName;} public void setOriginalFileName(String v){originalFileName=v;}
    @Column(name="declared_mime_type",nullable=false,length=100) public String getDeclaredMimeType(){return declaredMimeType;} public void setDeclaredMimeType(String v){declaredMimeType=v;}
    @Column(name="detected_mime_type",nullable=false,length=100) public String getDetectedMimeType(){return detectedMimeType;} public void setDetectedMimeType(String v){detectedMimeType=v;}
    @Column(name="declared_size",nullable=false) public Long getDeclaredSize(){return declaredSize;} public void setDeclaredSize(Long v){declaredSize=v;}
    @Column(name="actual_size") public Long getActualSize(){return actualSize;} public void setActualSize(Long v){actualSize=v;}
    @Column(name="checksum_sha256",length=64) public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;}
    @Column(name="journal_stage",nullable=false,length=60) public String getJournalStage(){return journalStage;} public void setJournalStage(String v){journalStage=v;}
    @Column(name="file_version",nullable=false) public Long getFileVersion(){return fileVersion;} public void setFileVersion(Long v){fileVersion=v;}
    @Column(name="storage_state",nullable=false,length=40) public String getStorageState(){return storageState;} public void setStorageState(String v){storageState=v;}
    @Column(name="scan_state",nullable=false,length=30) public String getScanState(){return scanState;} public void setScanState(String v){scanState=v;}
    @Column(name="quarantine_state",nullable=false,length=30) public String getQuarantineState(){return quarantineState;} public void setQuarantineState(String v){quarantineState=v;}
    @Column(name="idempotency_key",nullable=false,length=160) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    @Column(name="created_by",nullable=false,length=100) public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    @Column(name="updated_by",nullable=false,length=100) public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false) public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
}
