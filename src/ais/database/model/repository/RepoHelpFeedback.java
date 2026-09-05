package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_help_feedback} —
 * penilaian ringkas ("membantu"/"tidak membantu" plus komentar opsional) dari
 * pengguna atas satu konten bantuan/FAQ pada modul repositori institusional
 * (lihat {@code RepositoryFaqCatalog}). Dipakai untuk tata kelola kualitas
 * konten bantuan (mengetahui halaman mana yang dianggap kurang membantu),
 * bukan untuk dukungan/tiket personal.
 *
 * <p>
 * Baris ini bersifat append-only ({@code dynamicUpdate = false}) dan sengaja
 * TIDAK menyimpan alamat IP mentah pengunjung — hanya
 * {@link #getVisitorHash()} untuk dedup/analisis kunjungan unik tanpa data
 * pribadi yang bisa diidentifikasi langsung.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false)
@Table(schema="public",name="repo_help_feedback")
public class RepoHelpFeedback implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id;private String tenantKey,contentKey,comment,visitorHash,actorId;private Boolean helpful;private Date createdAt;
    /** Id baris feedback ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    /** Kunci penyewa (tenant) pemilik baris feedback ini — dasar isolasi data antar-institusi pada instalasi multi-tenant. */
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    /** Kunci konten bantuan/FAQ yang dinilai (identitas topik bantuan pada katalog FAQ, bukan id numerik baris). */
    @Column(name="content_key",nullable=false,length=120) public String getContentKey(){return contentKey;} public void setContentKey(String v){contentKey=v;}
    /** Penilaian utama: {@code true} bila pengguna menandai konten membantu, {@code false} bila tidak. */
    @Column(name="helpful",nullable=false) public Boolean getHelpful(){return helpful;} public void setHelpful(Boolean v){helpful=v;}
    /** Komentar bebas opsional yang menyertai penilaian, mis. alasan konten dianggap kurang jelas. */
    @Column(name="comment",length=1000) public String getComment(){return comment;} public void setComment(String v){comment=v;}
    /** Hash pengunjung (bukan alamat IP mentah) untuk dedup/analisis kunjungan unik tanpa menyimpan data pribadi yang bisa diidentifikasi langsung. */
    @Column(name="visitor_hash",length=64) public String getVisitorHash(){return visitorHash;} public void setVisitorHash(String v){visitorHash=v;}
    /** Id aktor terautentikasi bila pemberi feedback sedang login; {@code null} untuk pengunjung anonim/publik. */
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    /** Waktu feedback ini diberikan. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
