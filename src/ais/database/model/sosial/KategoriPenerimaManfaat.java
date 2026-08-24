package ais.database.model.sosial;
import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="kategori_penerima_manfaat",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","kode"}))
public class KategoriPenerimaManfaat extends SocialRecord { private static final long serialVersionUID=1L; private String kode,nama,legalBasis,compatibleFundCodes; private Boolean publicVisible;
 @Column(name="kode",nullable=false,length=60) public String getKode(){return kode;} public void setKode(String v){kode=trim(v);}
 @Column(name="nama",nullable=false,length=255) public String getNama(){return nama;} public void setNama(String v){nama=trim(v);}
 @Column(name="legal_basis",columnDefinition="TEXT") public String getLegalBasis(){return legalBasis;} public void setLegalBasis(String v){legalBasis=v;}
 @Column(name="compatible_fund_codes",length=1000) public String getCompatibleFundCodes(){return compatibleFundCodes;} public void setCompatibleFundCodes(String v){compatibleFundCodes=trim(v);}
 @Column(name="public_visible") public Boolean getPublicVisible(){return !Boolean.FALSE.equals(publicVisible);} public void setPublicVisible(Boolean v){publicVisible=v;}
}
