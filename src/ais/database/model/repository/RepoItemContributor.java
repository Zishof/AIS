package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_item_contributor} pada modul
 * repositori institusional (bergaya DSpace) — relasi terstruktur <b>banyak-ke-banyak</b> antara
 * satu {@link RepoItem} ({@link #getItemId()}) dan satu record authority penulis/kontributor
 * ({@link RepoAuthorAuthority}, via {@link #getAuthorityId()}), berperan sebagai kelas
 * penghubung (junction table) dengan atribut tambahan pada relasinya sendiri: peran kontribusi
 * ({@link #getContributorRole()}), urutan tampil ({@link #getSequenceNumber()}), status penulis
 * korespondensi ({@link #getCorresponding()}), serta salinan nama tampil
 * ({@link #getDisplayName()}). Kedua id relasi ({@code itemId}/{@code authorityId}) disimpan
 * sebagai id polos, BUKAN {@code @ManyToOne} — navigasi ke entitas {@link RepoItem}/
 * {@link RepoAuthorAuthority} harus lewat query eksplisit oleh pemanggil.
 * <p>
 * <b>Berbeda dari pola "tanpa urutan" yang berulang di modul lain pada basis kode ini</b> (mis.
 * {@code library.ItemPunyaPengarang}, {@code penelitiandanpengabdian.AnggotaArtikel}), kelas ini
 * SECARA EKSPLISIT menyimpan urutan penulis lewat {@link #getSequenceNumber()} — sehingga urutan
 * tampil daftar penulis pada satu item TIDAK bergantung pada urutan insert/id baris seperti pada
 * entitas junction lain yang tidak punya kolom urutan. Pemanggil yang menampilkan daftar
 * kontributor sebuah item WAJIB mengurutkan hasil query berdasarkan
 * {@link #getSequenceNumber()} (lihat pola {@code addOrder(Order.asc("sequenceNumber"))} pada
 * {@code RepositoryPublicService}), bukan mengasumsikan urutan alami hasil query.
 * </p>
 * <p>
 * Kombinasi ({@code item_id}, {@code authority_id}, {@code contributor_role}) dijaga unik lewat
 * {@code @UniqueConstraint} — satu authority tidak bisa didaftarkan dua kali dengan peran yang
 * sama pada item yang sama, tetapi BISA muncul lebih dari sekali dengan peran berbeda (mis.
 * seorang authority sebagai {@code "AUTHOR"} sekaligus {@code "EDITOR"} pada item yang sama
 * adalah dua baris berbeda yang sah).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_item_contributor",
        uniqueConstraints=@UniqueConstraint(columnNames={"item_id","authority_id","contributor_role"}))
public class RepoItemContributor implements Serializable {
    private static final long serialVersionUID=1L;
    /** Primary key baris ini. Lihat {@link #getId()}. */
    private Long id;
    /** Id {@link RepoItem} yang menerima kontribusi ini. Lihat {@link #getItemId()}. */
    private Long itemId;
    /** Id {@link RepoAuthorAuthority} yang berkontribusi. Lihat {@link #getAuthorityId()}. */
    private Long authorityId;
    /** Peran kontribusi (mis. "AUTHOR"/"EDITOR"/"ADVISOR"). Lihat {@link #getContributorRole()}. */
    private String contributorRole;
    /** Salinan nama tampil kontributor pada saat relasi ini dibuat. Lihat {@link #getDisplayName()}. */
    private String displayName;
    /** Urutan tampil kontributor ini di antara kontributor lain item yang sama. Lihat {@link #getSequenceNumber()}. */
    private Integer sequenceNumber;
    /** Penanda kontributor ini adalah penulis korespondensi (corresponding author). Lihat {@link #getCorresponding()}. */
    private Boolean corresponding;
    /** Penanda aktif/nonaktif (soft-delete) baris ini. Lihat {@link #getAktif()}. */
    private Boolean aktif;
    /** Waktu relasi ini dibuat. Lihat {@link #getCreatedAt()}. */
    private Date createdAt;

    /**
     * Mengembalikan primary key baris ini. {@code insertable = false} karena nilai dihasilkan
     * database ({@code IDENTITY}).
     * @return primary key, {@code null} untuk entity yang belum tersimpan
     */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;}
    /** Menyetel primary key. Tanpa validasi. @param v primary key baru */
    public void setId(Long v){id=v;}
    /**
     * Id {@link RepoItem} yang menerima kontribusi ini. Id polos, bukan relasi Hibernate —
     * navigasi ke entitas item harus lewat query eksplisit. Wajib diisi ({@code nullable = false}).
     * @return id item, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;}
    /** Menyetel id item. @param v id baru */
    public void setItemId(Long v){itemId=v;}
    /**
     * Id {@link RepoAuthorAuthority} (record authority penulis/kontributor tersatukan) yang
     * berkontribusi pada item ini. Id polos, bukan relasi Hibernate. Wajib diisi
     * ({@code nullable = false}).
     * @return id authority kontributor, tidak pernah {@code null} pada baris tersimpan
     */
    @Column(name="authority_id",nullable=false) public Long getAuthorityId(){return authorityId;}
    /** Menyetel id authority kontributor. @param v id baru */
    public void setAuthorityId(Long v){authorityId=v;}
    /**
     * Peran kontribusi kontributor ini atas item (mis. {@code "AUTHOR"}, {@code "EDITOR"},
     * {@code "ADVISOR"}, {@code "TRANSLATOR"}) — nilai bebas teks, tidak dibatasi enum di level
     * entitas. Bagian dari kunci unik komposit bersama {@link #getItemId()}/
     * {@link #getAuthorityId()} (lihat Javadoc kelas), sehingga satu authority dapat memiliki
     * lebih dari satu baris pada item yang sama selama perannya berbeda. Wajib diisi
     * ({@code nullable = false}).
     * @return peran kontribusi apa adanya
     */
    @Column(name="contributor_role",nullable=false,length=60) public String getContributorRole(){return contributorRole;}
    /** Menyetel peran kontribusi. @param v peran baru */
    public void setContributorRole(String v){contributorRole=v;}
    /**
     * Salinan nama tampil kontributor pada saat relasi ini dibuat/terakhir disinkronkan —
     * denormalisasi agar UI tidak perlu memuat {@link RepoAuthorAuthority} hanya untuk
     * menampilkan nama; salinan ini bisa jadi kedaluwarsa bila
     * {@link RepoAuthorAuthority#getCanonicalName()} berubah kemudian tanpa disertai
     * pembaruan baris ini. Wajib diisi ({@code nullable = false}).
     * @return nama tampil kontributor apa adanya
     */
    @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;}
    /** Menyetel nama tampil kontributor. @param v nama baru */
    public void setDisplayName(String v){displayName=v;}
    /**
     * Urutan tampil kontributor ini di antara kontributor lain milik item yang sama (mis. urutan
     * penulis pertama/kedua/ketiga pada daftar penulis karya). Lihat catatan penting pada Javadoc
     * kelas: berbeda dari pola junction table "tanpa urutan" yang berulang di modul lain pada
     * basis kode ini, kolom ini SECARA EKSPLISIT ada di sini — pemanggil wajib mengurutkan
     * berdasarkan kolom ini saat menampilkan daftar kontributor, bukan mengandalkan urutan hasil
     * query alami. Boleh {@code null} bila urutan belum ditentukan (tidak ada default di level
     * getter, berbeda dari kebanyakan kolom {@code Integer} lain pada entitas AIS yang memakai
     * default {@code 0}).
     * @return urutan tampil kontributor, boleh {@code null}
     */
    @Column(name="sequence_number") public Integer getSequenceNumber(){return sequenceNumber;}
    /** Menyetel urutan tampil kontributor. @param v urutan baru */
    public void setSequenceNumber(Integer v){sequenceNumber=v;}
    /**
     * Menandakan kontributor ini adalah penulis korespondensi (corresponding author) — penulis
     * yang menjadi kontak utama terkait publikasi (konvensi umum pada karya ilmiah multi-penulis).
     * Tidak ada pemaksaan "hanya satu corresponding author per item" di level entitas ini; bila
     * aturan itu diperlukan, penegakannya menjadi tanggung jawab layer service pemanggil.
     * @return {@code true} bila kontributor ini corresponding author, default {@code false}
     */
    @Column(name="corresponding") public Boolean getCorresponding(){return corresponding==null?Boolean.FALSE:corresponding;}
    /** Menyetel penanda corresponding author. @param v nilai baru */
    public void setCorresponding(Boolean v){corresponding=v;}
    /**
     * Menandakan baris ini aktif (belum soft-delete).
     * @return {@code true} bila baris aktif, default {@code true} bila kolom {@code null}
     */
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;}
    /** Menyetel penanda aktif. @param v nilai aktif baru */
    public void setAktif(Boolean v){aktif=v;}
    /**
     * Waktu relasi kontribusi ini dibuat. Wajib diisi ({@code nullable = false}) — seperti
     * {@link RepoItemRelation}, kelas ini tidak extends {@link ais.database.model.GeneralValueObject}
     * dan tidak memiliki callback {@code @PreUpdate}/{@code tanggal_dirubah}, sehingga
     * {@code createdAt} adalah satu-satunya jejak waktu pada baris ini dan TIDAK diperbarui
     * otomatis bila baris diubah setelah dibuat (mis. saat {@link #getSequenceNumber()} disusun
     * ulang).
     * @return waktu pembuatan relasi kontribusi
     */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;}
    /** Menyetel waktu pembuatan relasi. @param v waktu baru */
    public void setCreatedAt(Date v){createdAt=v;}
}
