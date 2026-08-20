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
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = false)
@Table(schema = "public", name = "repo_usage_event")
public class RepoUsageEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id, itemId, bitstreamId;
    private String eventType, visitorHash, actorId, userAgentClass;
    private Date occurredAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="bitstream_id") public Long getBitstreamId(){return bitstreamId;} public void setBitstreamId(Long v){bitstreamId=v;}
    @Column(name="event_type",nullable=false,length=20) public String getEventType(){return eventType;} public void setEventType(String v){eventType=v;}
    @Column(name="visitor_hash",length=64) public String getVisitorHash(){return visitorHash;} public void setVisitorHash(String v){visitorHash=v;}
    @Column(name="actor_id",length=255) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
    @Column(name="user_agent_class",length=40) public String getUserAgentClass(){return userAgentClass;} public void setUserAgentClass(String v){userAgentClass=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="occurred_at",nullable=false) public Date getOccurredAt(){return occurredAt;} public void setOccurredAt(Date v){occurredAt=v;}
}
