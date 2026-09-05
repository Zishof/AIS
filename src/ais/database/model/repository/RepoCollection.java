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

/**
 * Entitas Hibernate untuk satu koleksi dalam sistem repositori institusional AIS (modul
 * {@code repository}, bergaya DSpace) — dipetakan ke tabel {@code public.repo_collection}. Koleksi
 * adalah unit pengelompokan item repositori (mis. per program studi/jenis karya) dan dapat disusun
 * hierarkis lewat {@link #parentId} (self-reference by id, BUKAN relasi Hibernate {@code @ManyToOne}
 * — navigasi parent/child harus lewat query eksplisit oleh pemanggil, bukan lazy-load otomatis).
 * Membawa tiga blob konfigurasi berformat JSON mentah ({@link #metadataProfileJson},
 * {@link #workflowProfileJson}, {@link #accessPolicyJson}) yang menentukan profil metadata, alur
 * kerja submisi, dan kebijakan akses koleksi ini — tidak divalidasi/diparse skema di level entitas.
 * Field {@link #dspaceUuid}/{@link #dspaceHandle} dan {@link #sourceSystem} mengindikasikan
 * dukungan interoperasi/migrasi dengan instalasi DSpace eksternal.
 *
 * <h2>Catatan {@link #getDepositEnabled()}</h2>
 * <p>
 * Kolom database nullable meskipun makna bisnisnya defaultnya "diaktifkan" (lihat komentar di atas
 * deklarasi {@code @Column} pada {@link #getDepositEnabled()}) — ini disengaja agar migrasi skema
 * (ADD COLUMN tanpa DEFAULT di PostgreSQL lewat Hibernate 3) tidak gagal pada instalasi lama yang
 * sudah punya baris; default {@code true} diterapkan di level getter, bukan constraint DB.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "repo_collection")
public class RepoCollection extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    private Long id;
    /** Kunci penyewa (tenant) pemilik koleksi ini pada instalasi AIS multi-tenant. */
    private String tenantKey;
    private String kode;
    private String nama;
    private String deskripsi;
    /** Id {@link RepoCollection} induk untuk menyusun hierarki koleksi; BUKAN relasi Hibernate — hanya nilai id mentah, {@code null} berarti koleksi tingkat atas (root). */
    private Long parentId;
    /** Tipe koleksi ini, mis. {@code "COLLECTION"} (default); bisa dipakai membedakan koleksi vs komunitas/sub-tipe lain dalam hierarki repositori. */
    private String tipe;
    /** Sistem sumber data koleksi ini; default {@code "AIS"}, bisa berbeda bila koleksi diimpor dari instalasi DSpace eksternal. */
    private String sourceSystem;
    /** UUID koleksi yang berkorespondensi pada instalasi DSpace eksternal, bila koleksi ini disinkronkan/diimpor dari sana. */
    private String dspaceUuid;
    /** Handle (identifier permanen bergaya DSpace, mis. {@code 123456789/1}) koleksi ini pada instalasi DSpace eksternal. */
    private String dspaceHandle;
    private Integer sortOrder;
    /** Profil metadata koleksi ini sebagai JSON mentah (skema field metadata yang berlaku untuk item di koleksi ini); default {@code "{}"}, tidak divalidasi di level entitas. */
    private String metadataProfileJson;
    /** Profil alur kerja submisi/review koleksi ini sebagai JSON mentah; default {@code "{}"}, tidak divalidasi di level entitas. */
    private String workflowProfileJson;
    /** Kebijakan akses (siapa boleh lihat/unduh) koleksi ini sebagai JSON mentah; default {@code "{}"}, tidak divalidasi di level entitas. */
    private String accessPolicyJson;
    /** URI lisensi default yang diterapkan ke item baru di koleksi ini bila item tidak menentukan lisensinya sendiri. */
    private String defaultLicenseUri;
    /** Apakah submisi/deposit item baru diizinkan ke koleksi ini; kolom DB nullable disengaja — lihat "Catatan {@link #getDepositEnabled()}" pada Javadoc kelas. */
    private Boolean depositEnabled;
    
    // Audit & Default Fields
    private String oleh;
    private String olehId;
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    private Boolean aktif;

    /** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
    public RepoCollection() {}

    /**
     * Mengembalikan kunci tenant pemilik koleksi ini pada instalasi AIS multi-tenant.
     * @return kunci tenant, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name="tenant_key",length=120)
    public String getTenantKey(){return tenantKey==null?"":tenantKey.trim();}
    /** Menyetel kunci tenant pemilik koleksi. @param tenantKey kunci tenant baru */
    public void setTenantKey(String tenantKey){this.tenantKey=tenantKey;}

    /**
     * Mengembalikan primary key baris ini. {@code insertable = false} karena nilai dihasilkan
     * database ({@code IDENTITY}).
     * @return primary key, {@code null} untuk entity yang belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return this.id; }
    /** Menyetel primary key. Tanpa validasi. @param id primary key baru */
    public void setId(Long id) { this.id = id; }

    /**
     * Kode ringkas koleksi ini (mis. singkatan program studi).
     * @return kode, dipangkas spasi; {@code null} bila belum diisi (berbeda dari kebanyakan
     *         field {@code String} lain di kelas ini yang memakai string kosong sebagai default)
     */
    @Column(name = "kode", length = 50)
    public String getKode() { return kode == null ? null : kode.trim(); }
    /** Menyetel kode koleksi. @param kode kode baru */
    public void setKode(String kode) { this.kode = kode; }

    /**
     * Nama koleksi, wajib diisi ({@code nullable = false}).
     * @return nama koleksi, dipangkas spasi; {@code null} hanya bila field belum pernah disetel
     *         sama sekali (baris tersimpan seharusnya selalu punya nilai)
     */
    @Column(name = "nama", nullable = false, length = 255)
    public String getNama() { return this.nama == null ? null : this.nama.trim(); }
    /** Menyetel nama koleksi. @param nama nama baru */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Deskripsi/keterangan koleksi ini.
     * @return teks deskripsi apa adanya (TIDAK dipangkas spasi, berbeda dari kebanyakan getter
     *         {@code String} lain di kelas ini), boleh {@code null}
     */
    @Column(name = "deskripsi", columnDefinition = "TEXT")
    public String getDeskripsi() { return this.deskripsi; }
    /** Menyetel deskripsi koleksi. @param deskripsi deskripsi baru */
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    /**
     * Id {@link RepoCollection} induk untuk menyusun hierarki koleksi (mis. fakultas &rarr;
     * program studi). Id polos, bukan relasi Hibernate {@code @ManyToOne} — navigasi ke induk
     * atau daftar anak harus lewat query eksplisit oleh pemanggil (tidak ada lazy-load otomatis
     * dan tidak ada pengecekan siklus/cycle di level entitas ini).
     * @return id koleksi induk, atau {@code null} bila koleksi ini tingkat atas (root)
     */
    @Column(name = "parent_id")
    public Long getParentId() { return parentId; }
    /** Menyetel id koleksi induk. @param parentId id induk baru */
    public void setParentId(Long parentId) { this.parentId = parentId; }

    /**
     * Tipe koleksi ini (mis. {@code "COLLECTION"} default, bisa dibedakan dari komunitas/sub-tipe
     * lain dalam hierarki repositori bergaya DSpace).
     * @return tipe koleksi, default {@code "COLLECTION"} bila kolom {@code null}
     */
    @Column(name = "tipe", length = 50)
    public String getTipe() { return tipe == null ? "COLLECTION" : tipe.trim(); }
    /** Menyetel tipe koleksi. @param tipe tipe baru */
    public void setTipe(String tipe) { this.tipe = tipe; }

    /**
     * Sistem sumber data koleksi ini; default {@code "AIS"}, bisa berbeda (mis. {@code "DSPACE"})
     * bila koleksi diimpor/disinkronkan dari instalasi DSpace eksternal.
     * @return sistem sumber, default {@code "AIS"} bila kolom {@code null}
     */
    @Column(name = "source_system", length = 50)
    public String getSourceSystem() { return sourceSystem == null ? "AIS" : sourceSystem.trim(); }
    /** Menyetel sistem sumber. @param sourceSystem sistem sumber baru */
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    /**
     * UUID koleksi yang berkorespondensi pada instalasi DSpace eksternal.
     * @return UUID DSpace, tidak pernah {@code null} (string kosong bila tidak ada)
     */
    @Column(name = "dspace_uuid", length = 80)
    public String getDspaceUuid() { return dspaceUuid == null ? "" : dspaceUuid.trim(); }
    /** Menyetel UUID DSpace padanan. @param dspaceUuid UUID baru */
    public void setDspaceUuid(String dspaceUuid) { this.dspaceUuid = dspaceUuid; }

    /**
     * Handle (identifier permanen bergaya DSpace, mis. {@code 123456789/1}) koleksi ini pada
     * instalasi DSpace eksternal.
     * @return handle DSpace, tidak pernah {@code null} (string kosong bila tidak ada)
     */
    @Column(name = "dspace_handle", length = 255)
    public String getDspaceHandle() { return dspaceHandle == null ? "" : dspaceHandle.trim(); }
    /** Menyetel handle DSpace padanan. @param dspaceHandle handle baru */
    public void setDspaceHandle(String dspaceHandle) { this.dspaceHandle = dspaceHandle; }

    /**
     * Urutan tampil koleksi ini relatif terhadap koleksi sekelasnya (mis. pada listing anak
     * dari {@link #getParentId()} yang sama).
     * @return urutan tampil, default {@code 0} bila kolom {@code null}
     */
    @Column(name = "sort_order")
    public Integer getSortOrder() { return sortOrder == null ? Integer.valueOf(0) : sortOrder; }
    /** Menyetel urutan tampil. @param sortOrder urutan baru */
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    /**
     * Profil metadata koleksi ini sebagai JSON mentah (skema field metadata yang berlaku untuk
     * item di koleksi ini) — tidak divalidasi/diparse skema di level entitas, sepenuhnya
     * tanggung jawab pemanggil untuk mem-parse dan memvalidasi isinya.
     * @return JSON profil metadata, default {@code "{}"} bila kolom {@code null}
     */
    @Column(name = "metadata_profile_json", columnDefinition = "TEXT")
    public String getMetadataProfileJson() { return metadataProfileJson == null ? "{}" : metadataProfileJson; }
    /** Menyetel JSON profil metadata. @param metadataProfileJson JSON baru */
    public void setMetadataProfileJson(String metadataProfileJson) { this.metadataProfileJson = metadataProfileJson; }

    /**
     * Profil alur kerja submisi/review koleksi ini sebagai JSON mentah — tidak divalidasi/
     * diparse skema di level entitas.
     * @return JSON profil alur kerja, default {@code "{}"} bila kolom {@code null}
     */
    @Column(name = "workflow_profile_json", columnDefinition = "TEXT")
    public String getWorkflowProfileJson() { return workflowProfileJson == null ? "{}" : workflowProfileJson; }
    /** Menyetel JSON profil alur kerja. @param workflowProfileJson JSON baru */
    public void setWorkflowProfileJson(String workflowProfileJson) { this.workflowProfileJson = workflowProfileJson; }

    /**
     * Kebijakan akses (siapa boleh lihat/unduh) koleksi ini sebagai JSON mentah — tidak
     * divalidasi/diparse skema di level entitas. Berbeda dari {@link RepoItem#getAccessPolicy()}
     * yang berupa string status tunggal, kebijakan pada level koleksi ini berbentuk JSON bebas
     * sehingga bisa memuat aturan lebih kaya (mis. daftar peran yang diizinkan); item individual
     * di koleksi ini tetap punya {@code accessPolicy}-nya sendiri yang tidak otomatis diwarisi
     * dari JSON di sini.
     * @return JSON kebijakan akses, default {@code "{}"} bila kolom {@code null}
     */
    @Column(name = "access_policy_json", columnDefinition = "TEXT")
    public String getAccessPolicyJson() { return accessPolicyJson == null ? "{}" : accessPolicyJson; }
    /** Menyetel JSON kebijakan akses. @param accessPolicyJson JSON baru */
    public void setAccessPolicyJson(String accessPolicyJson) { this.accessPolicyJson = accessPolicyJson; }

    /**
     * URI lisensi default yang diterapkan ke item baru di koleksi ini bila item tidak
     * menentukan lisensinya sendiri lewat {@link RepoItem#getLicenseUri()} — penerapan default
     * ini adalah tanggung jawab layer service saat item dibuat, bukan otomatis di level entitas.
     * @return URI lisensi default, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "default_license_uri", length = 500)
    public String getDefaultLicenseUri() { return defaultLicenseUri == null ? "" : defaultLicenseUri.trim(); }
    /** Menyetel URI lisensi default. @param defaultLicenseUri URI baru */
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
    /**
     * Mengembalikan id pengguna yang terakhir mengubah koleksi ini. Field ini didefinisikan
     * ulang sebagai field privat khusus kelas ini (bukan mewarisi field {@code olehId} milik
     * {@link GeneralValueObject}) karena kolom {@code oleh_id} yang dipetakan lewat anotasi
     * {@code @Column} harus dideklarasikan pada kelas yang memiliki getter fisiknya.
     * @return id pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOlehId() { return olehId; }
    /**
     * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
     * atau kosong/spasi diabaikan diam-diam agar jejak audit yang sudah terisi tidak terhapus
     * oleh jalur simpan yang kebetulan tidak membawa informasi pengguna.
     * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    /**
     * Mengembalikan nama pengguna yang terakhir mengubah koleksi ini.
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
     * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence tepat
     * sebelum setiap {@code UPDATE}, memperbarui {@link #getTanggal_dirubah()} lewat
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
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
     * Menandakan koleksi ini aktif (belum soft-delete). Default {@code true} bila kolom
     * {@code null}. Perhatikan bahwa entitas ini tidak memaksakan konsistensi hierarki: menonaktifkan
     * koleksi induk TIDAK otomatis menonaktifkan koleksi anak ({@link #getParentId()}) — bila
     * perilaku itu diinginkan, penegakannya menjadi tanggung jawab layer service pemanggil.
     * @return {@code true} bila koleksi aktif
     */
    public Boolean getAktif() { return aktif == null ? true : aktif; }
    /** Menyetel penanda aktif. @param aktif nilai aktif baru */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Representasi teks ringkas koleksi ini untuk debugging/log: {@code "<id>-<nama>"}.
     * Meng-override {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"}.
     * @return string berformat {@code "<id>-<nama>"}
     */
    public String toString() {
        return id + "-" + nama;
    }
}
