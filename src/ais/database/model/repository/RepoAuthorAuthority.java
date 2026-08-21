package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Authority record untuk menyatukan variasi nama penulis dan identifier eksternal. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_author_authority",
        uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","normalized_name"}))
public class RepoAuthorAuthority implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id; private String tenantKey,canonicalName,normalizedName,nameVariants,orcid,nidn,nip,nim,
            affiliation,rorId,institutionalEmail,topics; private Boolean verified,aktif; private Date createdAt,updatedAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    @Column(name="canonical_name",nullable=false,length=255) public String getCanonicalName(){return canonicalName==null?"":canonicalName;} public void setCanonicalName(String v){canonicalName=v;}
    @Column(name="normalized_name",nullable=false,length=255) public String getNormalizedName(){return normalizedName==null?"":normalizedName;} public void setNormalizedName(String v){normalizedName=v;}
    @Column(name="name_variants",columnDefinition="TEXT") public String getNameVariants(){return nameVariants==null?"":nameVariants;} public void setNameVariants(String v){nameVariants=v;}
    @Column(name="orcid",length=40) public String getOrcid(){return orcid==null?"":orcid;} public void setOrcid(String v){orcid=v;}
    @Column(name="nidn",length=80) public String getNidn(){return nidn==null?"":nidn;} public void setNidn(String v){nidn=v;}
    @Column(name="nip",length=100) public String getNip(){return nip==null?"":nip;} public void setNip(String v){nip=v;}
    @Column(name="nim",length=100) public String getNim(){return nim==null?"":nim;} public void setNim(String v){nim=v;}
    @Column(name="affiliation",length=500) public String getAffiliation(){return affiliation==null?"":affiliation;} public void setAffiliation(String v){affiliation=v;}
    @Column(name="ror_id",length=100) public String getRorId(){return rorId==null?"":rorId;} public void setRorId(String v){rorId=v;}
    @Column(name="institutional_email",length=255) public String getInstitutionalEmail(){return institutionalEmail==null?"":institutionalEmail;} public void setInstitutionalEmail(String v){institutionalEmail=v;}
    @Column(name="topics",columnDefinition="TEXT") public String getTopics(){return topics==null?"":topics;} public void setTopics(String v){topics=v;}
    @Column(name="verified") public Boolean getVerified(){return verified==null?Boolean.FALSE:verified;} public void setVerified(Boolean v){verified=v;}
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="updated_at",nullable=false) public Date getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Date v){updatedAt=v;}
}
