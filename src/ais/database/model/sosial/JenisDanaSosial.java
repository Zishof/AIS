package ais.database.model.sosial;

import javax.persistence.*;
import org.hibernate.envers.Audited;

@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="jenis_dana_sosial",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class JenisDanaSosial extends SocialRecord {
    private static final long serialVersionUID=1L; private String kode,nama,receiptType,accountingCode; private Boolean restricted,calculationRequired,publicActive; private SosialChannel sosialChannel;
    @Column(name="kode",nullable=false,length=40) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
    @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
    @Column(name="restricted") public Boolean getRestricted(){return Boolean.TRUE.equals(restricted);} public void setRestricted(Boolean v){restricted=v;}
    @Column(name="calculation_required") public Boolean getCalculationRequired(){return Boolean.TRUE.equals(calculationRequired);} public void setCalculationRequired(Boolean v){calculationRequired=v;}
    @Column(name="public_active") public Boolean getPublicActive(){return !Boolean.FALSE.equals(publicActive);} public void setPublicActive(Boolean v){publicActive=v;}
    @Column(name="receipt_type",length=60) public String getReceiptType(){return receiptType;} public void setReceiptType(String v){receiptType=trim(v);}
    @Column(name="accounting_code",length=120) public String getAccountingCode(){return accountingCode;} public void setAccountingCode(String v){accountingCode=trim(v);}
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sosial_channel_id",nullable=false) public SosialChannel getSosialChannel(){return sosialChannel;} public void setSosialChannel(SosialChannel v){sosialChannel=v;}
}
