package ais.database.model.sosial;
import java.math.BigDecimal; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false) @Table(schema="public",name="social_correction_event")
public class SocialCorrectionEvent extends SocialRecord { private static final long serialVersionUID=1L; private String targetType,targetReference,correctionType,reason,priorState,resultingState,approvalStatus,actor,requestId; private BigDecimal amount;
 @Column(name="target_type",nullable=false,length=60) public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=trim(v);}
 @Column(name="target_reference",nullable=false,length=120) public String getTargetReference(){return targetReference;} public void setTargetReference(String v){targetReference=trim(v);}
 @Column(name="correction_type",nullable=false,length=60) public String getCorrectionType(){return correctionType;} public void setCorrectionType(String v){correctionType=trim(v);}
 @Column(name="reason",nullable=false,columnDefinition="TEXT") public String getReason(){return reason;} public void setReason(String v){reason=v;}
 @Column(name="prior_state",columnDefinition="TEXT") public String getPriorState(){return priorState;} public void setPriorState(String v){priorState=v;}
 @Column(name="resulting_state",columnDefinition="TEXT") public String getResultingState(){return resultingState;} public void setResultingState(String v){resultingState=v;}
 @Column(name="approval_status",length=40) public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=trim(v);}
 @Column(name="actor",length=255) public String getActor(){return actor;} public void setActor(String v){actor=trim(v);}
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 @Column(name="amount",precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}
