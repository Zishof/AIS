package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Penilaian ringkas untuk tata kelola konten bantuan; tidak menyimpan alamat IP mentah. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false)
@Table(schema="public",name="repo_help_feedback")
public class RepoHelpFeedback implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id;private String tenantKey,contentKey,comment,visitorHash,actorId;private Boolean helpful;private Date createdAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    @Column(name="content_key",nullable=false,length=120) public String getContentKey(){return contentKey;} public void setContentKey(String v){contentKey=v;}
    @Column(name="helpful",nullable=false) public Boolean getHelpful(){return helpful;} public void setHelpful(Boolean v){helpful=v;}
    @Column(name="comment",length=1000) public String getComment(){return comment;} public void setComment(String v){comment=v;}
    @Column(name="visitor_hash",length=64) public String getVisitorHash(){return visitorHash;} public void setVisitorHash(String v){visitorHash=v;}
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
