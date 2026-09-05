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
    /** Primary key baris ini. Lihat {@link #getId()}. */
    private Long id;
    /** Id {@link RepoCollection} yang menaungi item ini. Lihat {@link #getCollectionId()}. */
    private Long collectionId;
    /** Identifier item ini pada protokol harvesting OAI-PMH. Lihat {@link #getOaiIdentifier()}. */
    private String oaiIdentifier;
    /** Penanda soft-withdraw item ini dari publikasi. Lihat {@link #getIsWithdrawn()}. */
    private Boolean isWithdrawn;
    /** Nama kelas entitas AIS sumber item ini. Lihat {@link #getSourceClass()}. */
    private String sourceClass;
    /** Id baris pada entitas sumber ({@link #sourceClass}). Lihat {@link #getSourceId()}. */
    private Long sourceId;
    /** Label tampilan cache untuk entitas sumber. Lihat {@link #getSourceLabel()}. */
    private String sourceLabel;
    /** UUID padanan pada instalasi DSpace eksternal. Lihat {@link #getDspaceUuid()}. */
    private String dspaceUuid;
    /** Handle bergaya DSpace pada instalasi eksternal. Lihat {@link #getDspaceHandle()}. */
    private String dspaceHandle;
    /** Judul item. Lihat {@link #getTitle()}. */
    private String title;
    /** Abstrak/ringkasan item. Lihat {@link #getAbstractText()}. */
    private String abstractText;
    /** Teks hasil ekstraksi berkas untuk pengindeksan pencarian. Lihat {@link #getExtractedText()}. */
    private String extractedText;
    /** Daftar penulis sebagai teks bebas (lihat juga {@link RepoItemContributor} untuk relasi terstruktur). Lihat {@link #getAuthors()}. */
    private String authors;
    /** Daftar subjek/kata kunci sebagai teks bebas. Lihat {@link #getSubjects()}. */
    private String subjects;
    /** Nama penerbit. Lihat {@link #getPublisher()}. */
    private String publisher;
    /** Kode bahasa dokumen. Lihat {@link #getLanguage()}. */
    private String language;
    /** Jenis dokumen (skripsi/tesis/disertasi/jurnal/dll). Lihat {@link #getDocumentType()}. */
    private String documentType;
    /** Kebijakan akses berkas/item ini. Lihat {@link #getAccessPolicy()}. */
    private String accessPolicy;
    /** Status sinkronisasi terakhir dengan layanan eksternal. Lihat {@link #getSyncStatus()}. */
    private String syncStatus;
    /** Pesan/detail hasil sinkronisasi terakhir (mis. pesan error). Lihat {@link #getSyncMessage()}. */
    private String syncMessage;
    /** Waktu sinkronisasi terakhir dijalankan. Lihat {@link #getLastSyncAt()}. */
    private Date lastSyncAt;
    /** Penanda item sudah terindeks di Turnitin. Lihat {@link #getTurnitinIndexed()}. */
    private Boolean turnitinIndexed;
    /** Waktu item terindeks di Turnitin. Lihat {@link #getTurnitinIndexedAt()}. */
    private Date turnitinIndexedAt;
    /** Waktu item disubmit oleh pengunggah. Lihat {@link #getSubmittedAt()}. */
    private Date submittedAt;
    /** Tanggal terbit resmi item (mis. tanggal sidang/lulus). Lihat {@link #getIssuedAt()}. */
    private Date issuedAt;
    /** Kolom {@code @Version} JPA murni untuk optimistic locking. Lihat {@link #getLockVersion()}. */
    private Long lockVersion;
    /** Status alur kerja submisi/review item. Lihat {@link #getWorkflowStatus()}. */
    private String workflowStatus;
    /** Id pengguna pemilik/pengunggah item. Lihat {@link #getOwnerId()}. */
    private String ownerId;
    /** Id pengguna reviewer yang ditugaskan atas item ini. Lihat {@link #getAssignedReviewerId()}. */
    private String assignedReviewerId;
    /** URI lisensi (mis. Creative Commons) yang berlaku atas item ini. Lihat {@link #getLicenseUri()}. */
    private String licenseUri;
    /** Batas waktu embargo akses item ini. Lihat {@link #getEmbargoUntil()}. */
    private Date embargoUntil;
    /** Waktu item dipublikasikan. Lihat {@link #getPublishedAt()}. */
    private Date publishedAt;
    /** Waktu item ditarik (withdraw). Lihat {@link #getWithdrawnAt()}. */
    private Date withdrawnAt;
    /** Alasan penarikan item. Lihat {@link #getWithdrawalReason()}. */
    private String withdrawalReason;
    /** Digital Object Identifier item ini. Lihat {@link #getDoi()}. */
    private String doi;
    /** Slug URL ramah-manusia untuk item ini. Lihat {@link #getSlug()}. */
    private String slug;
    /** Nomor urut versi metadata item ini. Lihat {@link #getVersionNumber()}. */
    private Long versionNumber;
    /** Id versi metadata sebelumnya (rantai revisi). Lihat {@link #getPreviousVersionId()}. */
    private Long previousVersionId;
    /** Penghitung jumlah tampilan halaman item. Lihat {@link #getViewCount()}. */
    private Long viewCount;
    /** Penghitung jumlah unduhan berkas item. Lihat {@link #getDownloadCount()}. */
    private Long downloadCount;
    /** Kunci penyewa (tenant) pemilik item ini pada instalasi AIS multi-tenant. Lihat {@link #getTenantKey()}. */
    private String tenantKey;
    /** Penanda item ini ditonjolkan/disorot pada listing publik. Lihat {@link #getFeatured()}. */
    private Boolean featured;
    /** Waktu item ditandai featured. Lihat {@link #getFeaturedAt()}. */
    private Date featuredAt;
    /** Status pengajuan/registrasi DOI item ini. Lihat {@link #getDoiState()}. */
    private String doiState;
    /** Waktu status DOI terakhir berubah. Lihat {@link #getDoiUpdatedAt()}. */
    private Date doiUpdatedAt;

    // Audit & Default Fields
    /** Nama pengguna terakhir yang mengubah item ini. Lihat {@link #getOleh()}. */
    private String oleh;
    /** Id pengguna terakhir yang mengubah item ini. Lihat {@link #getOlehId()}. */
    private String olehId;
    /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. Lihat {@link #getTanggal_dirubah()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    /** Penanda aktif/nonaktif (soft-delete) baris ini. Lihat {@link #getAktif()}. */
    private Boolean aktif;

    /** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
    public RepoItem() {}

    /**
     * Mengembalikan kunci tenant pemilik item ini pada instalasi AIS multi-tenant, dipangkas spasi.
     * Tidak pernah {@code null} — nilai kosong bila belum diisi (perhatikan: entitas ini
     * TIDAK ikut ditemukan pada {@link ais.action.master.repository.RepositoryTenantScope}, yang
     * hanya memiliki method {@code currentKey()}/{@code ensureSchema()}; pengisian
     * {@code tenantKey} dilakukan eksplisit oleh layer service saat item dibuat/disinkronkan,
     * bukan otomatis oleh entitas).
     * @return kunci tenant, tidak pernah {@code null}
     */
    @Column(name="tenant_key",length=120)
    public String getTenantKey(){return tenantKey==null?"":tenantKey.trim();}
    /** Menyetel kunci tenant pemilik item ini. @param tenantKey kunci tenant baru */
    public void setTenantKey(String tenantKey){this.tenantKey=tenantKey;}

    /**
     * Menandakan item ini ditonjolkan/disorot pada listing publik (mis. halaman depan repositori).
     * Default {@code FALSE} bila kolom {@code null}.
     * @return {@code true} bila item ditandai featured
     */
    @Column(name="featured") public Boolean getFeatured(){return featured==null?Boolean.FALSE:featured;}
    /** Menyetel penanda featured. @param v nilai featured baru */
    public void setFeatured(Boolean v){featured=v;}
    /** Waktu item ditandai featured; {@code null} bila belum pernah/tidak featured. @return waktu penandaan featured */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="featured_at") public Date getFeaturedAt(){return featuredAt;}
    /** Menyetel waktu penandaan featured. @param v waktu baru */
    public void setFeaturedAt(Date v){featuredAt=v;}
    /**
     * Status pengajuan/registrasi DOI item ini (mis. {@code DRAFT}, {@code PENDING}, {@code REGISTERED},
     * {@code FAILED}) — nilai bebas teks, tidak dibatasi enum di level entitas. Default {@code "DRAFT"}
     * bila kolom {@code null}, berbeda dengan {@link #getDoi()} yang defaultnya string kosong.
     * @return status DOI, default {@code "DRAFT"}
     */
    @Column(name="doi_state",length=30) public String getDoiState(){return doiState==null?"DRAFT":doiState;}
    /** Menyetel status DOI. @param v status DOI baru */
    public void setDoiState(String v){doiState=v;}
    /** Waktu status DOI ({@link #getDoiState()}) terakhir berubah. @return waktu perubahan status DOI */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="doi_updated_at") public Date getDoiUpdatedAt(){return doiUpdatedAt;}
    /** Menyetel waktu perubahan status DOI. @param v waktu baru */
    public void setDoiUpdatedAt(Date v){doiUpdatedAt=v;}

    /**
     * Mengembalikan primary key baris ini. {@code insertable = false} karena nilai dihasilkan
     * database ({@code IDENTITY}) — Hibernate tidak menyertakan kolom ini pada {@code INSERT},
     * database yang mengisinya secara otomatis lalu Hibernate membacanya kembali.
     * @return primary key, {@code null} untuk entity yang belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    /** Menyetel primary key. Tanpa validasi. @param id primary key baru */
    public void setId(Long id) { this.id = id; }

    /**
     * Id {@link RepoCollection} yang menaungi item ini. Relasi disimpan sebagai id polos
     * ({@code Long}), BUKAN {@code @ManyToOne} — untuk memuat objek koleksi, pemanggil harus
     * melakukan query eksplisit sendiri (mis. {@code session.get(RepoCollection.class, id)}).
     * Wajib diisi ({@code nullable = false}): setiap item harus tergabung dalam satu koleksi.
     * @return id koleksi induk, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name = "collection_id", nullable = false)
    public Long getCollectionId() { return this.collectionId; }
    /** Menyetel id koleksi induk. @param collectionId id {@link RepoCollection} baru */
    public void setCollectionId(Long collectionId) { this.collectionId = collectionId; }

    /**
     * Identifier item ini pada protokol harvesting OAI-PMH (Open Archives Initiative Protocol
     * for Metadata Harvesting), dipakai agar repositori eksternal/agregator (mis. Garuda,
     * Google Scholar, portal DIKTI) dapat mengambil metadata item ini secara terstandar.
     * Unik per baris ({@code unique = true}).
     * @return identifier OAI-PMH, boleh {@code null} bila item belum pernah dipublikasikan lewat OAI
     */
    @Column(name = "oai_identifier", unique = true, length = 255)
    public String getOaiIdentifier() { return this.oaiIdentifier; }
    /** Menyetel identifier OAI-PMH. @param oaiIdentifier identifier baru */
    public void setOaiIdentifier(String oaiIdentifier) { this.oaiIdentifier = oaiIdentifier; }

    /**
     * Menandakan item ini sudah ditarik dari publikasi (soft-withdraw); pola DSpace standar
     * untuk menyembunyikan sebuah item dari akses publik tanpa menghapus barisnya (mis. karena
     * dugaan plagiarisme, permintaan penulis, atau koreksi administratif) — riwayat penarikan
     * tetap dapat ditelusuri lewat {@link #getWithdrawnAt()}/{@link #getWithdrawalReason()}.
     * <p>
     * <b>Perhatian keamanan bagi pemanggil:</b> flag ini TIDAK otomatis menyaring hasil query.
     * Setiap query yang menampilkan item ke publik (mis.
     * {@link ais.action.master.repository.RepositoryPublicService}) wajib menambahkan sendiri
     * kondisi {@code isWithdrawn = false} (atau {@code null}) di samping {@link #getAktif()} dan
     * {@link #getAccessPolicy()} — lihat method {@code publicVisibilityRestriction()} pada layer
     * service repositori, yang memang menggabungkan ketiganya. Default {@code false} bila kolom
     * {@code null} (item baru dianggap belum ditarik).
     * </p>
     * @return {@code true} bila item sudah ditarik dari publikasi
     */
    @Column(name = "is_withdrawn")
    public Boolean getIsWithdrawn() { return isWithdrawn == null ? false : isWithdrawn; }
    /** Menyetel penanda withdrawn. @param isWithdrawn nilai withdrawn baru */
    public void setIsWithdrawn(Boolean isWithdrawn) { this.isWithdrawn = isWithdrawn; }

    /**
     * Nama kelas entitas AIS asal item ini diproduksi (mis. tugas akhir mahasiswa), dipasangkan
     * dengan {@link #getSourceId()}. Referensi polimorfik longgar (bukan {@code @ManyToOne})
     * sehingga satu modul repositori dapat menerima item dari berbagai jenis entitas sumber
     * (tugas akhir, artikel jurnal, dsb.) tanpa foreign key rigid ke tabel tertentu.
     * @return nama kelas entitas sumber, dipangkas spasi; {@code null} bila tidak ada sumber tercatat
     */
    @Column(name = "source_class", length = 255)
    public String getSourceClass() { return sourceClass == null ? null : sourceClass.trim(); }
    /** Menyetel nama kelas entitas sumber. @param sourceClass nama kelas baru */
    public void setSourceClass(String sourceClass) { this.sourceClass = sourceClass; }

    /**
     * Id baris pada entitas sumber ({@link #getSourceClass()}) yang menjadi asal item ini.
     * @return id baris sumber, atau {@code null} bila tidak ada sumber tercatat
     */
    @Column(name = "source_id")
    public Long getSourceId() { return sourceId; }
    /** Menyetel id baris sumber. @param sourceId id baru */
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    /**
     * Label tampilan untuk entitas sumber, dipakai agar UI tidak perlu memuat ulang entitas asal
     * hanya untuk menampilkan nama (mis. judul tugas akhir atau nama mahasiswa pengunggah) —
     * salinan cache yang bisa jadi kedaluwarsa bila entitas sumber diubah namanya kemudian.
     * @return label tampilan sumber, boleh {@code null}
     */
    @Column(name = "source_label", length = 255)
    public String getSourceLabel() { return sourceLabel; }
    /** Menyetel label tampilan sumber. @param sourceLabel label baru */
    public void setSourceLabel(String sourceLabel) { this.sourceLabel = sourceLabel; }

    /**
     * UUID padanan item ini pada instalasi DSpace eksternal, bila item ini dimigrasikan/
     * disinkronkan dari/ke sana.
     * @return UUID DSpace, tidak pernah {@code null} (string kosong bila tidak ada)
     */
    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    /** Menyetel UUID DSpace padanan. @param dspaceUuid UUID baru */
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    /**
     * Handle bergaya DSpace (identifier permanen, mis. {@code 123456789/1}) untuk item ini
     * pada instalasi DSpace eksternal.
     * @return handle DSpace, tidak pernah {@code null} (string kosong bila tidak ada)
     */
    @Column(name = "dspace_handle", length = 255)
    public String getDspaceHandle() { return dspaceHandle == null ? "" : dspaceHandle.trim(); }
    /** Menyetel handle DSpace padanan. @param dspaceHandle handle baru */
    public void setDspaceHandle(String dspaceHandle) { this.dspaceHandle = dspaceHandle; }

    /**
     * Judul item, dipetakan sebagai kolom {@code TEXT} (tanpa batas panjang praktis) karena judul
     * karya ilmiah bisa sangat panjang.
     * @return judul item, dipangkas spasi, tidak pernah {@code null} (string kosong bila belum diisi)
     */
    @Column(name = "title", columnDefinition = "TEXT")
    public String getTitle() { return title == null ? "" : title.trim(); }
    /** Menyetel judul item. @param title judul baru */
    public void setTitle(String title) { this.title = title; }

    /**
     * Abstrak/ringkasan isi item.
     * @return teks abstrak, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "abstract_text", columnDefinition = "TEXT")
    public String getAbstractText() { return abstractText == null ? "" : abstractText.trim(); }
    /** Menyetel teks abstrak. @param abstractText abstrak baru */
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    /**
     * Teks hasil ekstraksi dari berkas ({@link RepoBitstream}) milik item ini, dipakai sebagai
     * sumber untuk pengindeksan pencarian teks-penuh (full-text search). Berbeda dari kebanyakan
     * field teks lain di kelas ini, getter ini <b>tidak</b> memangkas spasi (
     * {@code extractedText} dikembalikan apa adanya) karena whitespace hasil ekstraksi PDF/DOC
     * kadang bermakna untuk kualitas pengindeksan (mis. pemisah paragraf).
     * @return teks hasil ekstraksi, tidak pernah {@code null}
     */
    @Column(name = "extracted_text", columnDefinition = "TEXT")
    public String getExtractedText() { return extractedText == null ? "" : extractedText; }
    /** Menyetel teks hasil ekstraksi. @param extractedText teks baru */
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    /**
     * Daftar penulis sebagai teks bebas (mis. dipisah titik koma). Untuk relasi penulis
     * terstruktur dengan authority record dan urutan tampil eksplisit, lihat
     * {@link RepoItemContributor}/{@link RepoAuthorAuthority} — field ini adalah salinan
     * ringkas untuk tampilan cepat/pencarian, bukan sumber kebenaran hubungan penulis-item.
     * @return daftar penulis, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "authors", columnDefinition = "TEXT")
    public String getAuthors() { return authors == null ? "" : authors.trim(); }
    /** Menyetel daftar penulis. @param authors teks penulis baru */
    public void setAuthors(String authors) { this.authors = authors; }

    /**
     * Daftar subjek/kata kunci item sebagai teks bebas.
     * @return daftar subjek, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "subjects", columnDefinition = "TEXT")
    public String getSubjects() { return subjects == null ? "" : subjects.trim(); }
    /** Menyetel daftar subjek. @param subjects teks subjek baru */
    public void setSubjects(String subjects) { this.subjects = subjects; }

    /**
     * Nama penerbit item ini (mis. nama fakultas/universitas/jurnal penerbit).
     * @return nama penerbit, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "publisher", length = 255)
    public String getPublisher() { return publisher == null ? "" : publisher.trim(); }
    /** Menyetel nama penerbit. @param publisher nama baru */
    public void setPublisher(String publisher) { this.publisher = publisher; }

    /**
     * Kode bahasa dokumen (mis. {@code "id"}, {@code "en"}). Default {@code "id"} bila kolom
     * {@code null} — asumsi mayoritas karya ilmiah pada instalasi AIS berbahasa Indonesia.
     * @return kode bahasa, default {@code "id"}
     */
    @Column(name = "language", length = 30)
    public String getLanguage() { return language == null ? "id" : language.trim(); }
    /** Menyetel kode bahasa. @param language kode bahasa baru */
    public void setLanguage(String language) { this.language = language; }

    /**
     * Jenis dokumen (mis. {@code "Thesis"}, {@code "Article"}, {@code "Other"}) — nilai bebas
     * teks, tidak dibatasi enum di level entitas. Default {@code "Other"} bila kolom {@code null}.
     * @return jenis dokumen, default {@code "Other"}
     */
    @Column(name = "document_type", length = 80)
    public String getDocumentType() { return documentType == null ? "Other" : documentType.trim(); }
    /** Menyetel jenis dokumen. @param documentType jenis baru */
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    /**
     * Kebijakan akses item ini terhadap metadata dan (secara tidak langsung, lewat kebijakan
     * berkas masing-masing) berkasnya. Nilai bebas teks yang dipakai luas oleh
     * {@link ais.action.master.repository.RepositoryPublicService}, mis. {@code "OPEN_ACCESS"}
     * (metadata dan berkas terbuka publik), {@code "METADATA_ONLY"} (hanya metadata publik,
     * berkas disembunyikan), {@code "RESTRICTED"}/{@code "INSTITUTION_ONLY"}/
     * {@code "AUTHENTICATED"} (perlu otorisasi lebih), dan {@code "EMBARGOED"} (tertunda hingga
     * {@link #getEmbargoUntil()}). Default {@code "OPEN_ACCESS"} bila kolom {@code null} —
     * <b>penting untuk keamanan:</b> ini berarti item yang barunya dibuat lewat jalur yang lupa
     * mengisi kolom ini akan DIANGGAP terbuka publik oleh getter, meski di database nilainya
     * {@code NULL}. Layer publik (lihat {@code RepositoryPublicService.publicVisibilityRestriction()})
     * menangani ini secara konsisten dengan memakai ekspresi SQL {@code coalesce(access_policy,
     * 'METADATA_ONLY')} pada beberapa query dan pengecekan eksplisit {@code "OPEN_ACCESS".equals(...)}
     * pada query lain — perilaku default di level Java ini (OPEN_ACCESS) dan di level SQL pada
     * sebagian query (METADATA_ONLY) TIDAK selalu selaras; pemanggil baru yang menambah query
     * publik harus memverifikasi query SQL-nya sendiri menerapkan default yang aman, bukan
     * mengandalkan default getter Java ini yang lebih longgar.
     * @return kebijakan akses item, default {@code "OPEN_ACCESS"}
     */
    @Column(name = "access_policy", length = 40)
    public String getAccessPolicy() { return accessPolicy == null ? "OPEN_ACCESS" : accessPolicy.trim(); }
    /** Menyetel kebijakan akses. @param accessPolicy kebijakan baru */
    public void setAccessPolicy(String accessPolicy) { this.accessPolicy = accessPolicy; }

    /**
     * Status sinkronisasi terakhir item ini dengan layanan eksternal (mis. OAI-PMH harvester,
     * agregator DIKTI/Garuda). Nilai bebas teks, mis. {@code "DRAFT"}, {@code "SYNCED"},
     * {@code "PUBLISHED"}, {@code "APPROVED"}, {@code "FAILED"} — dipakai bersama
     * {@link #getWorkflowStatus()} oleh {@code isPublicStatus(String)} pada layer service publik
     * untuk menentukan apakah item sudah layak tampil ke publik. Default {@code "DRAFT"} bila
     * kolom {@code null} (item baru dianggap belum siap tampil).
     * @return status sinkronisasi, default {@code "DRAFT"}
     */
    @Column(name = "sync_status", length = 40)
    public String getSyncStatus() { return syncStatus == null ? "DRAFT" : syncStatus.trim(); }
    /** Menyetel status sinkronisasi. @param syncStatus status baru */
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    /**
     * Pesan/detail hasil sinkronisasi terakhir (mis. pesan error dari endpoint eksternal),
     * dipakai untuk diagnostik saat {@link #getSyncStatus()} menunjukkan kegagalan.
     * @return pesan sinkronisasi, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "sync_message", columnDefinition = "TEXT")
    public String getSyncMessage() { return syncMessage == null ? "" : syncMessage.trim(); }
    /** Menyetel pesan sinkronisasi. @param syncMessage pesan baru */
    public void setSyncMessage(String syncMessage) { this.syncMessage = syncMessage; }

    /**
     * Waktu sinkronisasi terakhir dijalankan atas item ini.
     * @return waktu sinkronisasi terakhir, boleh {@code null} bila belum pernah disinkronkan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_sync_at")
    public Date getLastSyncAt() { return lastSyncAt; }
    /** Menyetel waktu sinkronisasi terakhir. @param lastSyncAt waktu baru */
    public void setLastSyncAt(Date lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    /**
     * Menandakan berkas/konten item ini sudah dikirim dan terindeks pada layanan deteksi
     * plagiarisme Turnitin.
     * @return {@code true} bila sudah terindeks di Turnitin, default {@code false}
     */
    @Column(name = "turnitin_indexed")
    public Boolean getTurnitinIndexed() { return turnitinIndexed == null ? false : turnitinIndexed; }
    /** Menyetel penanda terindeks Turnitin. @param turnitinIndexed nilai baru */
    public void setTurnitinIndexed(Boolean turnitinIndexed) { this.turnitinIndexed = turnitinIndexed; }

    /**
     * Waktu item terindeks di Turnitin.
     * @return waktu indeks Turnitin, boleh {@code null} bila belum pernah diindeks
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "turnitin_indexed_at")
    public Date getTurnitinIndexedAt() { return turnitinIndexedAt; }
    /** Menyetel waktu indeks Turnitin. @param turnitinIndexedAt waktu baru */
    public void setTurnitinIndexedAt(Date turnitinIndexedAt) { this.turnitinIndexedAt = turnitinIndexedAt; }

    /**
     * Waktu item disubmit oleh pengunggah (awal alur kerja review), berbeda dari
     * {@link #getIssuedAt()} yang mencatat tanggal terbit resmi karya.
     * @return waktu submisi, boleh {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "submitted_at")
    public Date getSubmittedAt() { return submittedAt; }
    /** Menyetel waktu submisi. @param submittedAt waktu baru */
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }

    /**
     * Tanggal terbit resmi item ini (mis. tanggal sidang/kelulusan untuk tugas akhir, atau
     * tanggal terbit edisi jurnal).
     * @return tanggal terbit, boleh {@code null}
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "issued_at")
    public Date getIssuedAt() { return issuedAt; }
    /** Menyetel tanggal terbit. @param issuedAt tanggal baru */
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }

    /**
     * Kolom {@code @Version} JPA murni untuk optimistic locking Hibernate — Hibernate menaikkan
     * nilainya otomatis pada setiap {@code UPDATE} dan menolak simpanan yang membawa nilai versi
     * usang ({@code StaleObjectStateException}). Terpisah sepenuhnya dari {@link #getVersionNumber()}
     * yang merupakan penomoran versi METADATA bisnis (rantai revisi item) — jangan tertukar
     * keduanya. Default {@code 0L} bila kolom {@code null}.
     * @return nilai lock optimistik saat ini
     */
    @Version
    /* Kolom modern pada tabel berisi data harus dapat ditambahkan Hibernate 3. */
    @Column(name = "lock_version")
    public Long getLockVersion() { return lockVersion == null ? Long.valueOf(0L) : lockVersion; }
    /** Menyetel nilai lock optimistik. Biasanya hanya diisi Hibernate sendiri. @param lockVersion nilai baru */
    public void setLockVersion(Long lockVersion) { this.lockVersion = lockVersion; }

    /**
     * Status alur kerja submisi/review item (mis. {@code "DRAFT"}, {@code "IN_REVIEW"},
     * {@code "APPROVED"}, {@code "PUBLISHED"}, {@code "REJECTED"}) — nilai bebas teks. Dipakai
     * bersama {@link #getSyncStatus()} untuk menentukan visibilitas publik lewat
     * {@code isPublicStatus(String)} pada layer service. Default {@code "DRAFT"} bila kolom
     * {@code null} (item baru dianggap belum melalui alur review apa pun).
     * @return status alur kerja, default {@code "DRAFT"}
     */
    @Column(name = "workflow_status", length = 40)
    public String getWorkflowStatus() { return workflowStatus == null ? "DRAFT" : workflowStatus.trim(); }
    /** Menyetel status alur kerja. @param workflowStatus status baru */
    public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

    /**
     * Id pengguna pemilik/pengunggah item ini — dipakai untuk kepemilikan pada alur kerja
     * submisi (mis. hanya pemilik dan reviewer yang boleh mengedit draft).
     * @return id pengguna pemilik, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "owner_id", length = 255)
    public String getOwnerId() { return ownerId == null ? "" : ownerId.trim(); }
    /** Menyetel id pengguna pemilik. @param ownerId id baru */
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    /**
     * Id pengguna reviewer yang ditugaskan atas item ini pada alur kerja review.
     * @return id reviewer, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "assigned_reviewer_id", length = 255)
    public String getAssignedReviewerId() { return assignedReviewerId == null ? "" : assignedReviewerId.trim(); }
    /** Menyetel id reviewer. @param assignedReviewerId id baru */
    public void setAssignedReviewerId(String assignedReviewerId) { this.assignedReviewerId = assignedReviewerId; }

    /**
     * URI lisensi (mis. tautan Creative Commons) yang berlaku atas item ini; bila kosong,
     * {@link RepoCollection#getDefaultLicenseUri()} pada koleksi induk dapat dipakai sebagai
     * fallback oleh layer service (tidak diterapkan otomatis di level entitas ini).
     * @return URI lisensi, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "license_uri", length = 500)
    public String getLicenseUri() { return licenseUri == null ? "" : licenseUri.trim(); }
    /** Menyetel URI lisensi. @param licenseUri URI baru */
    public void setLicenseUri(String licenseUri) { this.licenseUri = licenseUri; }

    /**
     * Batas waktu embargo akses item ini — selama tanggal ini belum lewat, item dengan
     * {@link #getAccessPolicy()} bernilai {@code "EMBARGOED"} seharusnya belum ditampilkan
     * berkasnya ke publik. Entitas ini <b>tidak</b> memaksakan aturan tersebut sendiri (tidak ada
     * logika perbandingan tanggal di sini) — penegakannya sepenuhnya tanggung jawab layer service
     * pemanggil (mis. {@link ais.action.master.repository.RepositoryPublicService}).
     * @return tanggal berakhirnya embargo, boleh {@code null} bila item tidak diembargo
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "embargo_until")
    public Date getEmbargoUntil() { return embargoUntil; }
    /** Menyetel batas waktu embargo. @param embargoUntil tanggal baru */
    public void setEmbargoUntil(Date embargoUntil) { this.embargoUntil = embargoUntil; }

    /**
     * Waktu item dipublikasikan (lolos seluruh alur kerja dan tampil ke publik).
     * @return waktu publikasi, boleh {@code null} bila belum pernah dipublikasikan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "published_at")
    public Date getPublishedAt() { return publishedAt; }
    /** Menyetel waktu publikasi. @param publishedAt waktu baru */
    public void setPublishedAt(Date publishedAt) { this.publishedAt = publishedAt; }

    /**
     * Waktu item ditarik (withdraw) dari publikasi; diisi berpasangan dengan
     * {@link #getIsWithdrawn()} dan {@link #getWithdrawalReason()}.
     * @return waktu penarikan, boleh {@code null} bila item belum pernah ditarik
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "withdrawn_at")
    public Date getWithdrawnAt() { return withdrawnAt; }
    /** Menyetel waktu penarikan. @param withdrawnAt waktu baru */
    public void setWithdrawnAt(Date withdrawnAt) { this.withdrawnAt = withdrawnAt; }

    /**
     * Alasan penarikan item dari publikasi, ditampilkan lewat teks bebas (mis. untuk keperluan
     * transparansi kepada pengunjung yang mencoba mengakses item yang sudah ditarik).
     * @return alasan penarikan, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "withdrawal_reason", columnDefinition = "TEXT")
    public String getWithdrawalReason() { return withdrawalReason == null ? "" : withdrawalReason.trim(); }
    /** Menyetel alasan penarikan. @param withdrawalReason alasan baru */
    public void setWithdrawalReason(String withdrawalReason) { this.withdrawalReason = withdrawalReason; }

    /**
     * Digital Object Identifier (DOI) item ini, bila sudah didaftarkan; lihat juga
     * {@link #getDoiState()}/{@link #getDoiUpdatedAt()} untuk status pendaftarannya.
     * @return DOI, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "doi", length = 255)
    public String getDoi() { return doi == null ? "" : doi.trim(); }
    /** Menyetel DOI. @param doi DOI baru */
    public void setDoi(String doi) { this.doi = doi; }

    /**
     * Slug URL ramah-manusia untuk item ini (dipakai pada tautan publik alih-alih id numerik).
     * @return slug, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "slug", length = 255)
    public String getSlug() { return slug == null ? "" : slug.trim(); }
    /** Menyetel slug. @param slug slug baru */
    public void setSlug(String slug) { this.slug = slug; }

    /**
     * Nomor urut versi METADATA bisnis item ini (bukan {@link #getLockVersion()} yang murni
     * optimistic-locking JPA) — bertambah setiap kali sebuah revisi metadata baru dibuat sebagai
     * baris terpisah yang menunjuk balik lewat {@link #getPreviousVersionId()}. Default
     * {@code 1L} bila kolom {@code null} (versi pertama).
     * @return nomor versi metadata, default {@code 1L}
     */
    @Column(name = "version_number")
    public Long getVersionNumber() { return versionNumber == null ? Long.valueOf(1L) : versionNumber; }
    /** Menyetel nomor versi metadata. @param versionNumber nomor baru */
    public void setVersionNumber(Long versionNumber) { this.versionNumber = versionNumber; }

    /**
     * Id versi metadata sebelumnya, membentuk rantai revisi item ini (lihat
     * {@link #getVersionNumber()}). Id polos, bukan relasi Hibernate — navigasi ke versi
     * sebelumnya harus lewat query eksplisit ({@code session.get(RepoItem.class, id)}).
     * @return id revisi sebelumnya, atau {@code null} bila ini versi pertama
     */
    @Column(name = "previous_version_id")
    public Long getPreviousVersionId() { return previousVersionId; }
    /** Menyetel id revisi sebelumnya. @param previousVersionId id baru */
    public void setPreviousVersionId(Long previousVersionId) { this.previousVersionId = previousVersionId; }

    /**
     * Penghitung jumlah tampilan halaman detail item ini. Tidak ada mekanisme anti-duplikasi
     * (mis. dedup per-IP/per-sesi) di level entitas — pencegahan hitung ganda, bila ada, menjadi
     * tanggung jawab layer service yang menaikkan angka ini.
     * @return jumlah tampilan, default {@code 0L}
     */
    @Column(name = "view_count")
    public Long getViewCount() { return viewCount == null ? Long.valueOf(0L) : viewCount; }
    /** Menyetel jumlah tampilan. @param viewCount jumlah baru */
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    /**
     * Penghitung jumlah unduhan berkas milik item ini (agregat lintas seluruh
     * {@link RepoBitstream} item ini, bukan per-berkas).
     * @return jumlah unduhan, default {@code 0L}
     */
    @Column(name = "download_count")
    public Long getDownloadCount() { return downloadCount == null ? Long.valueOf(0L) : downloadCount; }
    /** Menyetel jumlah unduhan. @param downloadCount jumlah baru */
    public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }

    // --- Audit Methods ---
    /**
     * Mengembalikan id pengguna yang terakhir mengubah item ini. Field ini didefinisikan ulang
     * sebagai field privat khusus kelas ini (bukan mewarisi field {@code olehId} milik
     * {@link GeneralValueObject}), karena kolom {@code oleh_id}/{@code oleh} yang dipetakan lewat
     * anotasi {@code @Column} harus dideklarasikan pada kelas yang memiliki getter fisiknya —
     * pola ini berulang di seluruh entity turunan {@code GeneralValueObject} pada basis kode AIS.
     * @return id pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOlehId() { return olehId; }
    /**
     * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
     * atau kosong/spasi diabaikan diam-diam agar jejak audit yang sudah terisi tidak terhapus
     * oleh jalur simpan yang kebetulan tidak membawa informasi pengguna (mis. proses batch/
     * penjadwal tanpa sesi login) — sama seperti kontrak {@link GeneralValueObject#setOlehId(String)}.
     * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    /**
     * Mengembalikan nama pengguna yang terakhir mengubah item ini.
     * @return nama pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOleh() { return oleh; }
    /**
     * Menyetel nama pengguna pengubah terakhir, dengan validasi non-trivial yang sama seperti
     * {@link #setOlehId(String)}: nilai {@code null}/kosong diabaikan diam-diam.
     * @param oleh nama pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) return;
        this.oleh = oleh;
    }

    /**
     * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat sebelum
     * setiap {@code UPDATE} dieksekusi, memperbarui {@link #getTanggal_dirubah()} lewat
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} sehingga jejak waktu
     * perubahan selalu konsisten tanpa perlu diisi manual oleh setiap pemanggil.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * Mengembalikan stempel waktu perubahan terakhir baris ini, diperbarui otomatis lewat
     * {@link #onUpdate()} pada setiap update.
     * @return waktu perubahan terakhir
     */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    /** Menyetel stempel waktu perubahan terakhir secara manual. Tanpa validasi. @param tanggal_dirubah waktu baru */
    public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }

    /**
     * Menandakan baris ini aktif (belum soft-delete). Default {@code true} bila kolom
     * {@code null} — konsisten dengan pola AIS umum: baris lama tanpa nilai eksplisit dianggap
     * aktif. Layer service repositori menggabungkan pengecekan ini dengan
     * {@link #getIsWithdrawn()} dan {@link #getAccessPolicy()} untuk menentukan visibilitas
     * publik (lihat catatan keamanan pada {@link #getIsWithdrawn()}).
     * @return {@code true} bila baris aktif
     */
    public Boolean getAktif() { return aktif == null ? true : aktif; }
    /** Menyetel penanda aktif. @param aktif nilai aktif baru */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Representasi teks ringkas item ini untuk debugging/log: {@code "<id>-<oaiIdentifier>"}.
     * Meng-override {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"};
     * entity ini tidak memiliki field {@code kode}/{@code nama} yang dipetakan sehingga format
     * bawaan tidak relevan.
     * @return string berformat {@code "<id>-<oaiIdentifier>"}
     */
    public String toString() {
        return id + "-" + oaiIdentifier;
    }
}
