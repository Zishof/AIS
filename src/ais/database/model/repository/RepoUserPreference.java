package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_user_preference}
 * — preferensi pengguna terautentikasi pada modul repositori institusional,
 * paling umum berupa saved search/alert langganan (mirip DSpace subscription
 * ke koleksi/pencarian) atau bookmark item tertentu.
 *
 * <p>
 * Baris jenis saved search/alert diproses berkala oleh penjadwal
 * ({@code RepositoryAlertScheduler} &rarr; {@code RepositoryAlertService})
 * yang mencocokkan {@link #getQueryValue()} terhadap item baru; setiap
 * kecocokan menghasilkan baris {@link RepoNotification} baru dan
 * memperbarui {@link #getLastCheckedAt()}/{@link #getLastMatchedAt()}/
 * {@link #getLastNotifiedItemId()}. Kegagalan pemrosesan (mis. query pola
 * tidak valid) diakumulasi ke {@link #getFailureCount()}/
 * {@link #getLastError()} tanpa menghentikan preferensi pengguna lain.
 * </p>
 *
 * <p>
 * {@link #getAktif()} default {@code true} bila {@code null} — pola flag
 * aktif fail-open by-default yang konsisten dengan entitas Repo* lain di
 * paket ini (bandingkan {@link RepoAuthorAuthority#getAktif()}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_user_preference")
public class RepoUserPreference implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId,lastNotifiedItemId; private String tenantKey,userId,preferenceType,label,queryValue,lastError; private Date createdAt,lastCheckedAt,lastMatchedAt; private Boolean aktif; private Integer failureCount;
    /** Id baris preferensi ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    /** Id pemilik preferensi ini — hanya pengguna ini (dan proses sistem) yang berhak melihat/mengubah baris ini. */
    @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    /** Kunci penyewa (tenant) pemilik baris preferensi ini — dasar isolasi data antar-institusi pada instalasi multi-tenant. */
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    /** Jenis preferensi, mis. {@code "SAVED_SEARCH"} (alert query berkala) atau {@code "BOOKMARK"} (tandai satu item); menentukan makna {@link #getQueryValue()} vs {@link #getItemId()}. */
    @Column(name="preference_type",nullable=false,length=30) public String getPreferenceType(){return preferenceType;} public void setPreferenceType(String v){preferenceType=v;}
    /** Id {@link RepoItem} yang ditandai, untuk preferensi jenis bookmark; tidak dipakai untuk saved search. */
    @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    /** Label tampilan yang diberikan pengguna untuk preferensi ini, mis. nama saved search. */
    @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
    /** Ekspresi kueri pencarian yang dicocokkan berkala terhadap item baru, untuk preferensi jenis saved search/alert. */
    @Column(name="query_value",length=2000) public String getQueryValue(){return queryValue;} public void setQueryValue(String v){queryValue=v;}
    /** Waktu preferensi ini dibuat. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    /** Waktu terakhir penjadwal alert memproses/mengecek preferensi ini, terlepas dari ada tidaknya kecocokan baru. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_checked_at") public Date getLastCheckedAt(){return lastCheckedAt;} public void setLastCheckedAt(Date v){lastCheckedAt=v;}
    /** Waktu terakhir kali query ini benar-benar menemukan kecocokan item baru. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_matched_at") public Date getLastMatchedAt(){return lastMatchedAt;} public void setLastMatchedAt(Date v){lastMatchedAt=v;}
    /** Id item terakhir yang sudah dikirimi notifikasi untuk preferensi ini — dipakai mencegah notifikasi duplikat pada item yang sama. */
    @Column(name="last_notified_item_id") public Long getLastNotifiedItemId(){return lastNotifiedItemId;} public void setLastNotifiedItemId(Long v){lastNotifiedItemId=v;}
    /** Jumlah kegagalan berturut-turut penjadwal memproses preferensi ini; {@code null} pada baris lama ditafsirkan sebagai {@code 0} (belum pernah gagal). */
    @Column(name="failure_count") public Integer getFailureCount(){return failureCount==null?Integer.valueOf(0):failureCount;} public void setFailureCount(Integer v){failureCount=v;}
    /** Pesan kesalahan terakhir dari penjadwal alert saat memproses preferensi ini, untuk diagnosis; {@code null} bila belum pernah gagal atau sudah pulih. */
    @Column(name="last_error",length=1000) public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    /** Status aktif preferensi ini; {@code null} ditafsirkan sebagai {@code true} (default aktif) — preferensi tidak aktif dilewati oleh penjadwal alert. */
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
}
