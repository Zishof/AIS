package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entitas Hibernate untuk relasi terarah antar-item repositori institusional AIS (modul
 * {@code repository}, bergaya DSpace) — dipetakan ke tabel {@code public.repo_item_relation}.
 * Menyatakan {@link #itemId} berelasi dengan {@link #relatedItemId} lewat jenis relasi bebas teks
 * {@link #relationType} (mis. "cites", "isVersionOf", "isSupplementTo" — tidak dibatasi enum di
 * level entitas). Kedua id item TIDAK dipetakan sebagai relasi Hibernate {@code @ManyToOne} ke
 * entitas item repositori — hanya id mentah (lebih longgar/decoupled, navigasi ke item sesungguhnya
 * harus lewat query eksplisit oleh pemanggil). {@link #actorId} mencatat siapa yang membuat relasi
 * ini (untuk audit). Tidak extends {@link ais.database.model.GeneralValueObject} (berbeda dari
 * mayoritas entitas AIS lain) — hanya {@link Serializable} polos, dan TIDAK di-{@code @Audited}
 * lewat Envers.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "repo_item_relation")
public class RepoItemRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Id item sumber relasi ({@link #itemId}), id item tujuan relasi ({@link #relatedItemId}) — keduanya id mentah, bukan relasi Hibernate ke entitas item. */
    private Long id, itemId, relatedItemId;
    /** Jenis relasi (bebas teks, mis. "cites"/"isVersionOf") dan id aktor/pengguna yang membuat relasi ini. */
    private String relationType, actorId;
    /** Urutan tampil relasi ini di antara relasi lain dengan {@link #itemId} dan {@link #relationType} yang sama. Lihat {@link #getSortOrder()}. */
    private Integer sortOrder;
    /** Waktu relasi ini dibuat. Lihat {@link #getCreatedAt()}. */
    private Date createdAt;
    /** Penanda aktif/nonaktif (soft-delete) baris ini. Lihat {@link #getAktif()}. */
    private Boolean aktif;

    /**
     * Mengembalikan primary key baris ini. {@code insertable = false} karena nilai dihasilkan
     * database ({@code IDENTITY}).
     * @return primary key, {@code null} untuk entity yang belum tersimpan
     */
    @Id @GeneratedValue(strategy = IDENTITY) @Column(name="id", insertable=false, nullable=false)
    public Long getId(){return id;}
    /** Menyetel primary key. Tanpa validasi. @param v primary key baru */
    public void setId(Long v){id=v;}
    /**
     * Id item sumber relasi ini (sisi "subjek" dari pernyataan {@code itemId relationType
     * relatedItemId}). Id polos, bukan relasi Hibernate — navigasi ke entitas
     * {@link RepoItem} harus lewat query eksplisit oleh pemanggil. Wajib diisi
     * ({@code nullable = false}).
     * @return id item sumber, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;}
    /** Menyetel id item sumber. @param v id baru */
    public void setItemId(Long v){itemId=v;}
    /**
     * Id item tujuan relasi ini (sisi "objek" dari pernyataan {@code itemId relationType
     * relatedItemId}). Id polos, bukan relasi Hibernate. Wajib diisi ({@code nullable = false}).
     * @return id item tujuan, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name="related_item_id",nullable=false) public Long getRelatedItemId(){return relatedItemId;}
    /** Menyetel id item tujuan. @param v id baru */
    public void setRelatedItemId(Long v){relatedItemId=v;}
    /**
     * Jenis relasi antara {@link #getItemId()} dan {@link #getRelatedItemId()} — nilai bebas
     * teks, tidak dibatasi enum di level entitas, mengikuti konvensi relasi DSpace/Dublin Core
     * (mis. {@code "cites"}, {@code "isVersionOf"}, {@code "isSupplementTo"},
     * {@code "isPartOf"}). Wajib diisi ({@code nullable = false}). Arah relasi bersifat searah
     * ({@code itemId} &rarr; {@code relatedItemId}); bila relasi timbal balik diinginkan (mis.
     * "cites" dan "isCitedBy"), pemanggil harus membuat dua baris terpisah — entitas ini tidak
     * membuat baris kebalikannya secara otomatis.
     * @return jenis relasi apa adanya
     */
    @Column(name="relation_type",nullable=false,length=60) public String getRelationType(){return relationType;}
    /** Menyetel jenis relasi. @param v jenis relasi baru */
    public void setRelationType(String v){relationType=v;}
    /**
     * Id aktor/pengguna yang membuat relasi ini, dicatat untuk keperluan audit (siapa yang
     * menyatakan hubungan antar-item ini, berguna terutama untuk relasi yang bisa disengketakan
     * seperti klaim sitasi).
     * @return id aktor pembuat relasi, boleh {@code null}
     */
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;}
    /** Menyetel id aktor pembuat relasi. @param v id aktor baru */
    public void setActorId(String v){actorId=v;}
    /**
     * Urutan tampil relasi ini di antara relasi lain milik {@link #getItemId()} dengan
     * {@link #getRelationType()} yang sama (mis. urutan tampil daftar versi/sitasi). Wajib diisi
     * ({@code nullable = false} pada kolom, meski getter tetap punya default). Default {@code 0}
     * bila field {@code null}.
     * @return urutan tampil, default {@code 0}
     */
    @Column(name="sort_order",nullable=false) public Integer getSortOrder(){return sortOrder==null?Integer.valueOf(0):sortOrder;}
    /** Menyetel urutan tampil. @param v urutan baru */
    public void setSortOrder(Integer v){sortOrder=v;}
    /**
     * Waktu relasi ini dibuat. Wajib diisi ({@code nullable = false}) — berbeda dari kebanyakan
     * entitas AIS lain, kelas ini tidak memiliki {@code tanggal_dirubah}/callback
     * {@code @PreUpdate} (lihat catatan pada Javadoc kelas: tidak extends
     * {@link ais.database.model.GeneralValueObject} dan tidak {@code @Audited}), sehingga
     * {@code createdAt} adalah satu-satunya jejak waktu pada baris ini dan TIDAK diperbarui
     * otomatis bila baris diubah setelah dibuat.
     * @return waktu pembuatan relasi
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;}
    /** Menyetel waktu pembuatan relasi. @param v waktu baru */
    public void setCreatedAt(Date v){createdAt=v;}
    /**
     * Menandakan baris ini aktif (belum soft-delete). Wajib diisi ({@code nullable = false} pada
     * kolom), default {@code TRUE} bila field {@code null}.
     * @return {@code true} bila relasi aktif, default {@code true}
     */
    @Column(name="aktif",nullable=false) public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;}
    /** Menyetel penanda aktif. @param v nilai aktif baru */
    public void setAktif(Boolean v){aktif=v;}
}
