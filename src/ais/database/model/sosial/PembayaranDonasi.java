package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="pembayaran_donasi",uniqueConstraints={@UniqueConstraint(columnNames={"gateway_id","gateway_order_id"})})
public class PembayaranDonasi extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private String gatewayId,gatewayOrderId,gatewayReference,paymentStatus,callbackTransactionId,callbackFingerprint,callbackPayloadRedacted,currency,paymentUrl,settlementBatch,reconciliationStatus,failureReason,requestId; private BigDecimal requestAmount,fee,total; private Date issuedAt,expiryAt,paidAt,lastInquiryAt; private Integer retryCount;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 @Column(name="gateway_id",nullable=false,length=60) public String getGatewayId(){return gatewayId;} public void setGatewayId(String v){gatewayId=trim(v);}
 @Column(name="gateway_order_id",nullable=false,length=120) public String getGatewayOrderId(){return gatewayOrderId;} public void setGatewayOrderId(String v){gatewayOrderId=trim(v);}
 @Column(name="gateway_reference",length=255) public String getGatewayReference(){return gatewayReference;} public void setGatewayReference(String v){gatewayReference=trim(v);}
 @Column(name="payment_status",nullable=false,length=40) public String getPaymentStatus(){return paymentStatus==null?"CREATED":paymentStatus;} public void setPaymentStatus(String v){paymentStatus=trim(v);}
 @Column(name="callback_transaction_id",length=255) public String getCallbackTransactionId(){return callbackTransactionId;} public void setCallbackTransactionId(String v){callbackTransactionId=trim(v);}
 @Column(name="callback_fingerprint",length=128,unique=true) public String getCallbackFingerprint(){return callbackFingerprint;} public void setCallbackFingerprint(String v){callbackFingerprint=trim(v);}
 @Column(name="callback_payload_redacted",columnDefinition="TEXT") public String getCallbackPayloadRedacted(){return callbackPayloadRedacted;} public void setCallbackPayloadRedacted(String v){callbackPayloadRedacted=v;}
 @Column(name="currency",nullable=false,length=3) public String getCurrency(){return currency==null?"IDR":currency;} public void setCurrency(String v){currency=trim(v);}
 @Column(name="payment_url",length=1500) public String getPaymentUrl(){return paymentUrl;} public void setPaymentUrl(String v){paymentUrl=trim(v);}
 @Column(name="settlement_batch",length=120) public String getSettlementBatch(){return settlementBatch;} public void setSettlementBatch(String v){settlementBatch=trim(v);}
 @Column(name="reconciliation_status",length=40) public String getReconciliationStatus(){return reconciliationStatus;} public void setReconciliationStatus(String v){reconciliationStatus=trim(v);}
 @Column(name="failure_reason",length=1000) public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 @Column(name="request_amount",nullable=false,precision=19,scale=2) public BigDecimal getRequestAmount(){return requestAmount;} public void setRequestAmount(BigDecimal v){requestAmount=v;}
 @Column(name="fee",nullable=false,precision=19,scale=2) public BigDecimal getFee(){return fee==null?BigDecimal.ZERO:fee;} public void setFee(BigDecimal v){fee=v;}
 @Column(name="total",nullable=false,precision=19,scale=2) public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="issued_at") public Date getIssuedAt(){return issuedAt;} public void setIssuedAt(Date v){issuedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expiry_at") public Date getExpiryAt(){return expiryAt;} public void setExpiryAt(Date v){expiryAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="paid_at") public Date getPaidAt(){return paidAt;} public void setPaidAt(Date v){paidAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="last_inquiry_at") public Date getLastInquiryAt(){return lastInquiryAt;} public void setLastInquiryAt(Date v){lastInquiryAt=v;}
 @Column(name="retry_count") public Integer getRetryCount(){return retryCount==null?0:retryCount;} public void setRetryCount(Integer v){retryCount=v;}
}
