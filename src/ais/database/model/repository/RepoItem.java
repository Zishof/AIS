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
@Table(schema = "public", name = "repo_item")
public class RepoItem extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    private Long id;
    private Long collectionId; 
    private String oaiIdentifier; 
    private Boolean isWithdrawn; 
    private String sourceClass;
    private Long sourceId;
    private String sourceLabel;
    private String dspaceUuid;
    private String dspaceHandle;
    private String title;
    private String abstractText;
    private String authors;
    private String subjects;
    private String publisher;
    private String language;
    private String documentType;
    private String accessPolicy;
    private String syncStatus;
    private String syncMessage;
    private Date lastSyncAt;
    private Boolean turnitinIndexed;
    private Date turnitinIndexedAt;
    private Date submittedAt;
    private Date issuedAt;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    public RepoItem() {}

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "collection_id", nullable = false)
    public Long getCollectionId() { return this.collectionId; }
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }

    @Column(name = "oai_identifier", unique = true, length = 255)
    public String getOaiIdentifier() { return this.oaiIdentifier; }
    public void setOaiIdentifier(String oaiIdentifier) { this.oaiIdentifier = oaiIdentifier; }

    @Column(name = "is_withdrawn")
    public Boolean getIsWithdrawn() { return isWithdrawn == null ? false : isWithdrawn; }
    public void setIsWithdrawn(Boolean isWithdrawn) { this.isWithdrawn = isWithdrawn; }

    @Column(name = "source_class", length = 255)
    public String getSourceClass() { return sourceClass == null ? null : sourceClass.trim(); }
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    @Column(name = "source_id")
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    @Column(name = "source_label", length = 255)
    public String getSourceLabel() { return sourceLabel; }
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }

    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    @Column(name = "dspace_handle", length = 255)
    public String getDspaceHandle() { return dspaceHandle == null ? "" : dspaceHandle.trim(); }
    public void setDspaceHandle(String dspaceHandle) { this.dspaceHandle = dspaceHandle; }

    @Column(name = "title", columnDefinition = "TEXT")
    public String getTitle() { return title == null ? "" : title.trim(); }
    public void setTitle(String title) { this.title = title; }

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    public String getAbstractText() { return abstractText == null ? "" : abstractText.trim(); }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    @Column(name = "authors", columnDefinition = "TEXT")
    public String getAuthors() { return authors == null ? "" : authors.trim(); }
    public void setAuthors(String authors) { this.authors = authors; }

    @Column(name = "subjects", columnDefinition = "TEXT")
    public String getSubjects() { return subjects == null ? "" : subjects.trim(); }
    public void setSubjects(String subjects) { this.subjects = subjects; }

    @Column(name = "publisher", length = 255)
    public String getPublisher() { return publisher == null ? "" : publisher.trim(); }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    @Column(name = "language", length = 30)
    public String getLanguage() { return language == null ? "id" : language.trim(); }
    public void setLanguage(String language) { this.language = language; }

    @Column(name = "document_type", length = 80)
    public String getDocumentType() { return documentType == null ? "Other" : documentType.trim(); }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    @Column(name = "access_policy", length = 40)
    public String getAccessPolicy() { return accessPolicy == null ? "OPEN_ACCESS" : accessPolicy.trim(); }
    public void setAccessPolicy(String accessPolicy) { this.accessPolicy = accessPolicy; }

    @Column(name = "sync_status", length = 40)
    public String getSyncStatus() { return syncStatus == null ? "DRAFT" : syncStatus.trim(); }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    @Column(name = "sync_message", columnDefinition = "TEXT")
    public String getSyncMessage() { return syncMessage == null ? "" : syncMessage.trim(); }
    public void setSyncMessage(String syncMessage) { this.syncMessage = syncMessage; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_sync_at")
    public Date getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Date lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    @Column(name = "turnitin_indexed")
    public Boolean getTurnitinIndexed() { return turnitinIndexed == null ? false : turnitinIndexed; }
    public void setTurnitinIndexed(Boolean turnitinIndexed) { this.turnitinIndexed = turnitinIndexed; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "turnitin_indexed_at")
    public Date getTurnitinIndexedAt() { return turnitinIndexedAt; }
    public void setTurnitinIndexedAt(Date turnitinIndexedAt) { this.turnitinIndexedAt = turnitinIndexedAt; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submitted_at")
    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "issued_at")
    public Date getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }

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
        return id + "-" + oaiIdentifier;
    }
}
