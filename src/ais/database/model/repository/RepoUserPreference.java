package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** Saved search and bookmark owned by one authenticated repository user. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_user_preference")
public class RepoUserPreference implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId,lastNotifiedItemId; private String tenantKey,userId,preferenceType,label,queryValue,lastError; private Date createdAt,lastCheckedAt,lastMatchedAt; private Boolean aktif; private Integer failureCount;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
    @Column(name="tenant_key",nullable=false,length=120) public String getTenantKey(){return tenantKey;} public void setTenantKey(String v){tenantKey=v;}
    @Column(name="preference_type",nullable=false,length=30) public String getPreferenceType(){return preferenceType;} public void setPreferenceType(String v){preferenceType=v;}
    @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="label",length=255) public String getLabel(){return label;} public void setLabel(String v){label=v;}
    @Column(name="query_value",length=2000) public String getQueryValue(){return queryValue;} public void setQueryValue(String v){queryValue=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_checked_at") public Date getLastCheckedAt(){return lastCheckedAt;} public void setLastCheckedAt(Date v){lastCheckedAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="last_matched_at") public Date getLastMatchedAt(){return lastMatchedAt;} public void setLastMatchedAt(Date v){lastMatchedAt=v;}
    @Column(name="last_notified_item_id") public Long getLastNotifiedItemId(){return lastNotifiedItemId;} public void setLastNotifiedItemId(Long v){lastNotifiedItemId=v;}
    @Column(name="failure_count") public Integer getFailureCount(){return failureCount==null?Integer.valueOf(0):failureCount;} public void setFailureCount(Integer v){failureCount=v;}
    @Column(name="last_error",length=1000) public String getLastError(){return lastError;} public void setLastError(String v){lastError=v;}
    @Column(name="aktif") public Boolean getAktif(){return aktif==null?Boolean.TRUE:aktif;} public void setAktif(Boolean v){aktif=v;}
}
