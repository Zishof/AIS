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

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "public", name = "repo_item_relation")
public class RepoItemRelation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, itemId, relatedItemId;
    private String relationType, actorId;
    private Integer sortOrder;
    private Date createdAt;
    private Boolean aktif;
    @Id @GeneratedValue(strategy = IDENTITY) @Column(name="id", insertable=false, nullable=false)
    public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="related_item_id",nullable=false) public Long getRelatedItemId(){return relatedItemId;} public void setRelatedItemId(Long v){relatedItemId=v;}
    @Column(name="relation_type",nullable=false,length=60) public String getRelationType(){return relationType;} public void setRelationType(String v){relationType=v;}
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    @Column(name="sort_order",nullable=false) public Integer getSortOrder(){return sortOrder==null?Integer.valueOf(0):sortOrder;} public void setSortOrder(Integer v){sortOrder=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Column(name="aktif",nullable=false) public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
}
