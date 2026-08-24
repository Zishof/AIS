package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="transaksi_donasi",uniqueConstraints={@UniqueConstraint(columnNames={"transaction_number"}),@UniqueConstraint(columnNames={"tenant_key","idempotency_key"})})
public class TransaksiDonasi extends SocialRecord { private static final long serialVersionUID=1L; private String transactionNumber,donorNameSnapshot,donorContactSnapshot,publicName,currency,sourceChannel,idempotencyKey,requestId,prayer,receiptStatus,accountingStatus; private SocialDonorIdentity donorIdentity; private JenisDanaSosial fundType; private PerhitunganZakat calculation; private SosialChannel sosialChannel; private BigDecimal grossDonationAmount,platformContribution,gatewayFee,totalPayable; private Boolean anonymous,publicPrayer; private Date expiresAt,paidAt;
 @Column(name="transaction_number",nullable=false,length=80) public String getTransactionNumber(){return transactionNumber;} public void setTransactionNumber(String v){transactionNumber=trim(v);}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="donor_identity_id") public SocialDonorIdentity getDonorIdentity(){return donorIdentity;} public void setDonorIdentity(SocialDonorIdentity v){donorIdentity=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="calculation_id") public PerhitunganZakat getCalculation(){return calculation;} public void setCalculation(PerhitunganZakat v){calculation=v;}
 @Column(name="donor_name_snapshot",nullable=false,length=255) public String getDonorNameSnapshot(){return donorNameSnapshot;} public void setDonorNameSnapshot(String v){donorNameSnapshot=trim(v);}
 @Column(name="donor_contact_snapshot",length=320) public String getDonorContactSnapshot(){return donorContactSnapshot;} public void setDonorContactSnapshot(String v){donorContactSnapshot=trim(v);}
 @Column(name="public_name",length=255) public String getPublicName(){return publicName;} public void setPublicName(String v){publicName=trim(v);}
 @Column(name="currency",nullable=false,length=3) public String getCurrency(){return currency==null?"IDR":currency;} public void setCurrency(String v){currency=trim(v);}
 @Column(name="source_channel",length=40) public String getSourceChannel(){return sourceChannel;} public void setSourceChannel(String v){sourceChannel=trim(v);}
 @Column(name="idempotency_key",nullable=false,length=120) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=trim(v);}
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 @Column(name="prayer",length=1000) public String getPrayer(){return prayer;} public void setPrayer(String v){prayer=v;}
 @Column(name="receipt_status",length=40) public String getReceiptStatus(){return receiptStatus;} public void setReceiptStatus(String v){receiptStatus=trim(v);}
 @Column(name="accounting_status",length=40) public String getAccountingStatus(){return accountingStatus;} public void setAccountingStatus(String v){accountingStatus=trim(v);}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sosial_channel_id",nullable=false) public SosialChannel getSosialChannel(){return sosialChannel;} public void setSosialChannel(SosialChannel v){sosialChannel=v;}
 @Column(name="gross_donation_amount",nullable=false,precision=19,scale=2) public BigDecimal getGrossDonationAmount(){return grossDonationAmount;} public void setGrossDonationAmount(BigDecimal v){grossDonationAmount=v;}
 @Column(name="platform_contribution",nullable=false,precision=19,scale=2) public BigDecimal getPlatformContribution(){return platformContribution==null?BigDecimal.ZERO:platformContribution;} public void setPlatformContribution(BigDecimal v){platformContribution=v;}
 @Column(name="gateway_fee",nullable=false,precision=19,scale=2) public BigDecimal getGatewayFee(){return gatewayFee==null?BigDecimal.ZERO:gatewayFee;} public void setGatewayFee(BigDecimal v){gatewayFee=v;}
 @Column(name="total_payable",nullable=false,precision=19,scale=2) public BigDecimal getTotalPayable(){return totalPayable;} public void setTotalPayable(BigDecimal v){totalPayable=v;}
 @Column(name="anonymous") public Boolean getAnonymous(){return Boolean.TRUE.equals(anonymous);} public void setAnonymous(Boolean v){anonymous=v;}
 @Column(name="public_prayer") public Boolean getPublicPrayer(){return Boolean.TRUE.equals(publicPrayer);} public void setPublicPrayer(Boolean v){publicPrayer=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expires_at") public Date getExpiresAt(){return expiresAt;} public void setExpiresAt(Date v){expiresAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="paid_at") public Date getPaidAt(){return paidAt;} public void setPaidAt(Date v){paidAt=v;}
}
