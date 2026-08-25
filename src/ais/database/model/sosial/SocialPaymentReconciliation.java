package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="social_payment_reconciliation",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","gateway","settlement_reference"}))
public class SocialPaymentReconciliation extends SocialRecord { private static final long serialVersionUID=1L; private PembayaranDonasi payment; private Date settlementDate,reviewedAt; private String gateway,settlementReference,exceptionType,notes,reviewedBy; private BigDecimal expectedAmount,receivedAmount,fee,difference;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") public PembayaranDonasi getPayment(){return payment;} public void setPayment(PembayaranDonasi v){payment=v;}
 @Temporal(TemporalType.DATE) @Column(name="settlement_date") public Date getSettlementDate(){return settlementDate;} public void setSettlementDate(Date v){settlementDate=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="reviewed_at") public Date getReviewedAt(){return reviewedAt;} public void setReviewedAt(Date v){reviewedAt=v;}
 @Column(name="gateway",length=60) public String getGateway(){return gateway;} public void setGateway(String v){gateway=trim(v);}
 @Column(name="settlement_reference",length=255) public String getSettlementReference(){return settlementReference;} public void setSettlementReference(String v){settlementReference=trim(v);}
 @Column(name="exception_type",length=80) public String getExceptionType(){return exceptionType;} public void setExceptionType(String v){exceptionType=trim(v);}
 @Column(name="notes",columnDefinition="TEXT") public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
 @Column(name="reviewed_by",length=255) public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=trim(v);}
 @Column(name="expected_amount",precision=19,scale=2) public BigDecimal getExpectedAmount(){return expectedAmount;} public void setExpectedAmount(BigDecimal v){expectedAmount=v;}
 @Column(name="received_amount",precision=19,scale=2) public BigDecimal getReceivedAmount(){return receivedAmount;} public void setReceivedAmount(BigDecimal v){receivedAmount=v;}
 @Column(name="fee",precision=19,scale=2) public BigDecimal getFee(){return fee;} public void setFee(BigDecimal v){fee=v;}
 @Column(name="difference",precision=19,scale=2) public BigDecimal getDifference(){return difference;} public void setDifference(BigDecimal v){difference=v;}
}
