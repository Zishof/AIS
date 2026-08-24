package ais.database.model.sosial;
import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="bukti_setor_sosial",uniqueConstraints={@UniqueConstraint(columnNames={"tenant_key","receipt_number"}),@UniqueConstraint(columnNames={"verification_token"})})
public class BuktiSetorSosial extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private String receiptNumber,receiptType,templateVersion,verificationToken,pdfPath,deliveryStatus,replacementReference; private Date generatedAt; private Boolean voided;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false,unique=true) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 @Column(name="receipt_number",nullable=false,length=120) public String getReceiptNumber(){return receiptNumber;} public void setReceiptNumber(String v){receiptNumber=trim(v);}
 @Column(name="receipt_type",length=60) public String getReceiptType(){return receiptType;} public void setReceiptType(String v){receiptType=trim(v);}
 @Column(name="template_version",length=40) public String getTemplateVersion(){return templateVersion;} public void setTemplateVersion(String v){templateVersion=trim(v);}
 @Column(name="verification_token",nullable=false,length=120) public String getVerificationToken(){return verificationToken;} public void setVerificationToken(String v){verificationToken=trim(v);}
 @Column(name="pdf_path",length=1000) public String getPdfPath(){return pdfPath;} public void setPdfPath(String v){pdfPath=trim(v);}
 @Column(name="delivery_status",length=40) public String getDeliveryStatus(){return deliveryStatus;} public void setDeliveryStatus(String v){deliveryStatus=trim(v);}
 @Column(name="replacement_reference",length=120) public String getReplacementReference(){return replacementReference;} public void setReplacementReference(String v){replacementReference=trim(v);}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="generated_at",nullable=false) public Date getGeneratedAt(){return generatedAt;} public void setGeneratedAt(Date v){generatedAt=v;}
 @Column(name="voided") public Boolean getVoided(){return Boolean.TRUE.equals(voided);} public void setVoided(Boolean v){voided=v;}
}
