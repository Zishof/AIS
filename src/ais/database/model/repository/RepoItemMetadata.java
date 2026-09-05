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
 * Entitas Hibernate yang memetakan tabel {@code public.repo_item_metadata} pada
 * modul repositori institusional (mirip DSpace — lihat juga {@link RepoItem},
 * {@link RepoBitstream}) untuk skripsi/tesis/jurnal. Merepresentasikan satu
 * pasangan bidang-nilai metadata Dublin Core (mis. {@code dc.title},
 * {@code dc.contributor.author}) milik satu {@link RepoItem}
 * ({@code itemId}, referensi tanpa relasi JPA eksplisit) — pola ini
 * bersesuaian dengan tabel {@code metadatavalue} pada DSpace asli: satu item
 * dapat memiliki banyak baris metadata dengan {@code metadataField} yang
 * berbeda-beda maupun berulang (mis. banyak penulis), diurutkan lewat
 * {@code place}.
 *
 * <p>
 * {@code authority} dan {@code confidence} mengikuti mekanisme authority
 * control ala DSpace: {@code authority} menyimpan kunci/ID otoritatif rujukan
 * nilai (mis. ID pada pengendali nama/istilah terkendali), sedangkan
 * {@code confidence} menyimpan tingkat keyakinan pencocokan authority
 * tersebut (default {@code -1} bila belum diisi/tidak diketahui).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "repo_item_metadata")
public class RepoItemMetadata extends GeneralValueObject {

    private static final long serialVersionUID = 1L;
    /** Primary key baris ini. Lihat {@link #getId()}. */
    private Long id;
    /** Id {@link RepoItem} pemilik pasangan metadata ini. Lihat {@link #getItemId()}. */
    private Long itemId;
    /** Nama bidang metadata Dublin Core (mis. {@code dc.title}). Lihat {@link #getMetadataField()}. */
    private String metadataField;
    /** Nilai metadata untuk bidang ini. Lihat {@link #getMetadataValue()}. */
    private String metadataValue;
    /** Kode bahasa nilai metadata ini. Lihat {@link #getLanguage()}. */
    private String language;
    /** Urutan tampil nilai ini di antara nilai lain pada bidang yang sama. Lihat {@link #getPlace()}. */
    private Integer place;
    /** Kunci/id authority control rujukan nilai ini. Lihat {@link #getAuthority()}. */
    private String authority;
    /** Tingkat keyakinan pencocokan authority. Lihat {@link #getConfidence()}. */
    private Integer confidence;

    // Audit & Default Fields
    /** Nama pengguna terakhir yang mengubah baris ini. Lihat {@link #getOleh()}. */
    private String oleh;
    /** Id pengguna terakhir yang mengubah baris ini. Lihat {@link #getOlehId()}. */
    private String olehId;
    /** Stempel waktu perubahan terakhir; diinisialisasi ke waktu pembuatan object. Lihat {@link #getTanggal_dirubah()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    /** Penanda aktif/nonaktif (soft-delete) baris ini. Lihat {@link #getAktif()}. */
    private Boolean aktif;

    /** Constructor default tanpa argumen, dibutuhkan Hibernate untuk hidrasi entity. */
    public RepoItemMetadata() {}

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
     * Id {@link RepoItem} pemilik pasangan bidang-nilai metadata ini. Relasi disimpan sebagai
     * id polos ({@code Long}), BUKAN {@code @ManyToOne} — untuk memuat objek item, pemanggil
     * harus melakukan query eksplisit sendiri. Wajib diisi ({@code nullable = false}).
     * @return id item pemilik, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return this.itemId; }
    /** Menyetel id item pemilik. @param itemId id {@link RepoItem} baru */
    public void setItemId(Long itemId) { this.itemId = itemId; }

    /**
     * Nama bidang metadata Dublin Core ala DSpace (mis. {@code dc.title},
     * {@code dc.contributor.author}, {@code dc.subject}). Wajib diisi ({@code nullable = false}).
     * Nilai bebas teks — tidak ada enum/lookup skema yang membatasi bidang mana yang valid di
     * level entitas ini; validasi terhadap {@link RepoCollection#getMetadataProfileJson()} (bila
     * ada) menjadi tanggung jawab layer service.
     * @return nama bidang metadata apa adanya
     */
    @Column(name = "metadata_field", nullable = false, length = 100)
    public String getMetadataField() { return this.metadataField; }
    /** Menyetel nama bidang metadata. @param metadataField nama bidang baru */
    public void setMetadataField(String metadataField) { this.metadataField = metadataField; }

    /**
     * Nilai metadata untuk bidang ini ({@link #getMetadataField()}). Wajib diisi
     * ({@code nullable = false}), dipetakan sebagai kolom {@code TEXT} agar dapat menampung nilai
     * panjang (mis. abstrak lengkap bila dimodelkan sebagai {@code dc.description.abstract}).
     * @return nilai metadata apa adanya
     */
    @Column(name = "metadata_value", columnDefinition = "TEXT", nullable = false)
    public String getMetadataValue() { return this.metadataValue; }
    /** Menyetel nilai metadata. @param metadataValue nilai baru */
    public void setMetadataValue(String metadataValue) { this.metadataValue = metadataValue; }

    /**
     * Kode bahasa nilai metadata ini (mis. {@code "id"}/{@code "en"}) — memungkinkan satu bidang
     * yang sama (mis. {@code dc.title}) memiliki nilai berbeda per bahasa (judul asli vs judul
     * terjemahan), pola authority/i18n standar DSpace.
     * @return kode bahasa, boleh {@code null} bila tidak spesifik bahasa
     */
    @Column(name = "language", length = 10)
    public String getLanguage() { return this.language; }
    /** Menyetel kode bahasa nilai metadata. @param language kode bahasa baru */
    public void setLanguage(String language) { this.language = language; }

    /**
     * Urutan tampil nilai ini di antara nilai-nilai lain pada bidang metadata yang sama milik
     * item yang sama (mis. urutan penulis kedua/ketiga saat {@code dc.contributor.author}
     * memiliki banyak baris untuk satu item) — pola {@code place} standar tabel
     * {@code metadatavalue} DSpace. Default {@code 0} bila kolom {@code null}.
     * @return urutan tampil, default {@code 0}
     */
    @Column(name = "place")
    public Integer getPlace() { return place == null ? Integer.valueOf(0) : place; }
    /** Menyetel urutan tampil. @param place urutan baru */
    public void setPlace(Integer place) { this.place = place; }

    /**
     * Kunci/id authority control yang menjadi rujukan nilai ini (mis. id pada pengendali nama/
     * istilah terkendali seperti {@link RepoAuthorAuthority} untuk nama penulis, atau otoritas
     * subjek eksternal) — mekanisme authority control standar DSpace untuk menyatukan variasi
     * penulisan nilai yang sama.
     * @return kunci authority, dipangkas spasi, tidak pernah {@code null}
     */
    @Column(name = "authority", length = 255)
    public String getAuthority() { return authority == null ? "" : authority.trim(); }
    /** Menyetel kunci authority. @param authority kunci baru */
    public void setAuthority(String authority) { this.authority = authority; }

    /**
     * Tingkat keyakinan pencocokan authority ({@link #getAuthority()}) — makin tinggi nilainya
     * makin yakin pencocokan otomatisnya (skala dan makna persis nilainya ditentukan konvensi
     * DSpace/pemanggil, tidak divalidasi di level entitas). Default {@code -1} bila kolom
     * {@code null}, menandakan "belum diisi/tidak diketahui" — bukan "keyakinan rendah" (0)
     * maupun "keyakinan tinggi" (nilai positif besar).
     * @return tingkat keyakinan authority, default {@code -1}
     */
    @Column(name = "confidence")
    public Integer getConfidence() { return confidence == null ? Integer.valueOf(-1) : confidence; }
    /** Menyetel tingkat keyakinan authority. @param confidence nilai baru */
    public void setConfidence(Integer confidence) { this.confidence = confidence; }

    // --- Audit Methods ---
    /**
     * Mengembalikan id pengguna yang terakhir mengubah baris ini. Field ini didefinisikan ulang
     * sebagai field privat khusus kelas ini (bukan mewarisi field {@code olehId} milik
     * {@link GeneralValueObject}) karena kolom {@code oleh_id} yang dipetakan lewat anotasi
     * {@code @Column} harus dideklarasikan pada kelas yang memiliki getter fisiknya.
     * @return id pengguna pengubah terakhir, boleh {@code null}
     */
    public String getOlehId() { return olehId; }
    /**
     * Menyetel id pengguna pengubah terakhir, dengan validasi non-trivial: nilai {@code null}
     * atau kosong/spasi diabaikan diam-diam agar jejak audit yang sudah terisi tidak terhapus.
     * @param olehId id pengguna pengubah; diabaikan bila {@code null}/kosong
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) return;
        this.olehId = olehId;
    }

    /**
     * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
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
     * Menandakan baris ini aktif (belum soft-delete).
     * @return {@code true} bila baris aktif, default {@code true} bila kolom {@code null}
     */
    public Boolean getAktif() { return aktif == null ? true : aktif; }
    /** Menyetel penanda aktif. @param aktif nilai aktif baru */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Representasi teks ringkas baris metadata ini untuk debugging/log:
     * {@code "<id>-<metadataField>:<metadataValue>"}. Meng-override
     * {@link GeneralValueObject#toString()} yang berformat {@code "kode - nama"}.
     * @return string berformat {@code "<id>-<metadataField>:<metadataValue>"}
     */
    public String toString() {
        return id + "-" + metadataField + ":" + metadataValue;
    }
}
