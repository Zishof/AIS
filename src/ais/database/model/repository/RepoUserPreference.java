package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Saved search and bookmark owned by one authenticated repository user. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_user_preference",
       uniqueConstraints=@UniqueConstraint(columnNames={"user_id","preference_type","item_id","query_value"}))
public class RepoUserPreference implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId; private String userId,preferenceType,label,queryValue; private Date createdAt; private Boolean aktif;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    @Column(name="preference_type",nullable=false,length=30) public String getPreferenceType(){return preferenceType;} public void setPreferenceType(String v){preferenceType=v;}
    @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
    @Column(name="query_value",length=2000) public String getQueryValue(){return queryValue;} public void setQueryValue(String v){queryValue=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
}
