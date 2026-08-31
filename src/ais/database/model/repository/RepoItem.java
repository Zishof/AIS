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
import javax.persistence.Version;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate untuk tabel {@code public.repo_item}, merepresentasikan satu item
 * (karya ilmiah/dokumen) pada modul repositori institusional AIS — modul bergaya DSpace untuk
 * menyimpan dan mempublikasikan skripsi/tesis/disertasi/jurnal beserta metadatanya. Satu baris
 * mewakili satu item: metadata bibliografis ({@link #getTitle()}, {@link #getAuthors()},
 * {@link #getAbstractText()}, {@link #getSubjects()}, {@link #getPublisher()},
 * {@link #getLanguage()}, {@link #getDocumentType()}), teks hasil ekstraksi berkas untuk
 * pengindeksan/pencarian ({@link #getExtractedText()}), status alur kerja penerbitan
 * ({@link #getWorkflowStatus()}, {@link #getAccessPolicy()}, embargo, penarikan/
 * {@code withdrawn}), serta identitas untuk interoperabilitas dengan sistem repositori lain
 * (OAI-PMH via {@link #getOaiIdentifier()}, DSpace UUID/handle, DOI).
 * <p>
 * Item ini tergabung dalam satu koleksi lewat {@link #getCollectionId()} (id koleksi, relasi
 * disimpan sebagai id polos, bukan {@code @ManyToOne}) dan dapat menunjuk balik ke entitas
 * sumber AIS asal item ini diproduksi (mis. tugas akhir mahasiswa) lewat pasangan
 * {@link #getSourceClass()}/{@link #getSourceId()}/{@link #getSourceLabel()}. Status sinkronisasi
 * dengan layanan eksternal (mis. deteksi plagiarisme Turnitin, endpoint OAI) dilacak lewat
 * {@link #getSyncStatus()}/{@link #getSyncMessage()}/{@link #getLastSyncAt()} dan
 * {@link #getTurnitinIndexed()}/{@link #getTurnitinIndexedAt()}. Versi metadata sebelumnya
 * (revisi) dapat ditelusuri lewat {@link #getPreviousVersionId()}/{@link #getVersionNumber()};
 * kolom {@link #getLockVersion()} adalah kolom {@code @Version} JPA murni untuk optimistic
 * locking, terpisah dari penomoran versi metadata tersebut.
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
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
    private String extractedText;
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
    private Long lockVersion;
    private String workflowStatus;
    private String ownerId;
    private String assignedReviewerId;
    private String licenseUri;
    private Date embargoUntil;
    private Date publishedAt;
    private Date withdrawnAt;
    private String withdrawalReason;
    private String doi;
    private String slug;
    private Long versionNumber;
    private Long previousVersionId;
    private Long viewCount;
    private Long downloadCount;
    private String tenantKey;
    private Boolean featured;
    private Date featuredAt;
    private String doiState;
    private Date doiUpdatedAt;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    public RepoItem() {}

    @Column(name="tenant_key",length=120)
    public String getTenantKey(){return tenantKey==null?"":tenantKey.trim();}
    public void setTenantKey(String tenantKey){this.tenantKey=tenantKey;}

    @Column(name="featured") public Boolean getFeatured(){return featured==null?Boolean.FALSE:featured;} public void setFeatured(Boolean v){featured=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="featured_at") public Date getFeaturedAt(){return featuredAt;} public void setFeaturedAt(Date v){featuredAt=v;}
    @Column(name="doi_state",length=30) public String getDoiState(){return doiState==null?"DRAFT":doiState;} public void setDoiState(String v){doiState=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="doi_updated_at") public Date getDoiUpdatedAt(){return doiUpdatedAt;} public void setDoiUpdatedAt(Date v){doiUpdatedAt=v;}

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

    /** Menandakan item ini sudah ditarik dari publikasi (soft-withdraw); lihat {@link #getWithdrawnAt()}/{@link #getWithdrawalReason()}. */
    @Column(name = "is_withdrawn")
    public Boolean getIsWithdrawn() { return isWithdrawn == null ? false : isWithdrawn; }
    public void setIsWithdrawn(Boolean isWithdrawn) { this.isWithdrawn = isWithdrawn; }

    /** Nama kelas entitas AIS asal item ini diproduksi (mis. tugas akhir mahasiswa), dipasangkan dengan {@link #getSourceId()}. */
    @Column(name = "source_class", length = 255)
    public String getSourceClass() { return sourceClass == null ? null : sourceClass.trim(); }
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    /** Id baris pada entitas sumber ({@link #getSourceClass()}) yang menjadi asal item ini. */
    @Column(name = "source_id")
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    /** Label tampilan untuk entitas sumber, dipakai agar UI tidak perlu memuat ulang entitas asal hanya untuk menampilkan nama. */
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

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    public String getExtractedText() { return extractedText == null ? "" : extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

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

    @Version
    /* Kolom modern pada tabel berisi data harus dapat ditambahkan Hibernate 3. */
    @Column(name = "lock_version")
    public Long getLockVersion() { return lockVersion == null ? Long.valueOf(0L) : lockVersion; }
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }

    @Column(name = "workflow_status", length = 40)
    public String getWorkflowStatus() { return workflowStatus == null ? "DRAFT" : workflowStatus.trim(); }
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

    @Column(name = "owner_id", length = 255)
    public String getOwnerId() { return ownerId == null ? "" : ownerId.trim(); }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    @Column(name = "assigned_reviewer_id", length = 255)
    public String getAssignedReviewerId() { return assignedReviewerId == null ? "" : assignedReviewerId.trim(); }
    public void setAssignedReviewerId(String assignedReviewerId) { this.assignedReviewerId = assignedReviewerId; }

    @Column(name = "license_uri", length = 500)
    public String getLicenseUri() { return licenseUri == null ? "" : licenseUri.trim(); }
    public void setLicenseUri(String licenseUri) { this.licenseUri = licenseUri; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "embargo_until")
    public Date getEmbargoUntil() { return embargoUntil; }
    public void setEmbargoUntil(Date embargoUntil) { this.embargoUntil = embargoUntil; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "published_at")
    public Date getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "withdrawn_at")
    public Date getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Date withdrawnAt) { this.withdrawnAt = withdrawnAt; }

    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    public String getWithdrawalReason() { return withdrawalReason == null ? "" : withdrawalReason.trim(); }
    public void setWithdrawalReason(String withdrawalReason) { this.withdrawalReason = withdrawalReason; }

    @Column(name = "doi", length = 255)
    public String getDoi() { return doi == null ? "" : doi.trim(); }
    public void setDoi(String doi) { this.doi = doi; }

    @Column(name = "slug", length = 255)
    public String getSlug() { return slug == null ? "" : slug.trim(); }
    public void setSlug(String slug) { this.slug = slug; }

    @Column(name = "version_number")
    public Long getVersionNumber() { return versionNumber == null ? Long.valueOf(1L) : versionNumber; }
    public void setVersionNumber(Long versionNumber) { this.versionNumber = versionNumber; }

    @Column(name = "previous_version_id")
    public Long getPreviousVersionId() { return previousVersionId; }
    public void setPreviousVersionId(Long previousVersionId) { this.previousVersionId = previousVersionId; }

    @Column(name = "view_count")
    public Long getViewCount() { return viewCount == null ? Long.valueOf(0L) : viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    @Column(name = "download_count")
    public Long getDownloadCount() { return downloadCount == null ? Long.valueOf(0L) : downloadCount; }
    public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }

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
