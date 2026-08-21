package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Relasi terstruktur antara karya dan authority penulis/kontributor. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_item_contributor",
        uniqueConstraints=@UniqueConstraint(columnNames={"item_id","authority_id","contributor_role"}))
public class RepoItemContributor implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId,authorityId; private String contributorRole,displayName; private Integer sequenceNumber;
    private Boolean corresponding,aktif; private Date createdAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="authority_id",nullable=false) public Long getAuthorityId(){return authorityId;} public void setAuthorityId(Long v){authorityId=v;}
    @Column(name="contributor_role",nullable=false,length=60) public String getContributorRole(){return contributorRole;} public void setContributorRole(String v){contributorRole=v;}
    @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
    @Column(name="sequence_number") public Integer getSequenceNumber(){return sequenceNumber;} public void setSequenceNumber(Integer v){sequenceNumber=v;}
    @Column(name="corresponding") public Boolean getCorresponding(){return corresponding==null?Boolean.FALSE:corresponding;} public void setCorresponding(Boolean v){corresponding=v;}
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
