package ais.database.model.sosial;
import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="social_prayer_message")
public class SocialPrayerMessage extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private String message,displayName,moderationStatus,moderatedBy; private Boolean anonymous; private Date publicAt; private Integer reportsCount;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false,unique=true) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 @Column(name="message",nullable=false,length=1000) public String getMessage(){return message;} public void setMessage(String v){message=v;}
 @Column(name="display_name",length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=trim(v);}
 @Column(name="moderation_status",nullable=false,length=40) public String getModerationStatus(){return moderationStatus==null?"PENDING":moderationStatus;} public void setModerationStatus(String v){moderationStatus=trim(v);}
 @Column(name="moderated_by",length=255) public String getModeratedBy(){return moderatedBy;} public void setModeratedBy(String v){moderatedBy=trim(v);}
 @Column(name="anonymous") public Boolean getAnonymous(){return Boolean.TRUE.equals(anonymous);} public void setAnonymous(Boolean v){anonymous=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="public_at") public Date getPublicAt(){return publicAt;} public void setPublicAt(Date v){publicAt=v;}
 @Column(name="reports_count") public Integer getReportsCount(){return reportsCount==null?0:reportsCount;} public void setReportsCount(Integer v){reportsCount=v;}
}
