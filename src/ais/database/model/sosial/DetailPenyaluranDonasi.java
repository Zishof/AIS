package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="detail_penyaluran_donasi")
public class DetailPenyaluranDonasi extends SocialRecord { private static final long serialVersionUID=1L; private PenyaluranDonasi distribution; private JenisDanaSosial fundType; private ProgramDonatur program; private KategoriPenerimaManfaat beneficiaryCategory; private AlokasiDonasi sourceAllocation; private BigDecimal amount; private Date distributionDate; private Integer beneficiaryCount; private String location,description,evidenceJson,accountingReference,approvalStatus;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="distribution_id",nullable=false) public PenyaluranDonasi getDistribution(){return distribution;} public void setDistribution(PenyaluranDonasi v){distribution=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id") public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="beneficiary_category_id",nullable=false) public KategoriPenerimaManfaat getBeneficiaryCategory(){return beneficiaryCategory;} public void setBeneficiaryCategory(KategoriPenerimaManfaat v){beneficiaryCategory=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_allocation_id",nullable=false) public AlokasiDonasi getSourceAllocation(){return sourceAllocation;} public void setSourceAllocation(AlokasiDonasi v){sourceAllocation=v;}
 @Column(name="amount",nullable=false,precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 @Temporal(TemporalType.DATE) @Column(name="distribution_date",nullable=false) public Date getDistributionDate(){return distributionDate;} public void setDistributionDate(Date v){distributionDate=v;}
 @Column(name="beneficiary_count") public Integer getBeneficiaryCount(){return beneficiaryCount;} public void setBeneficiaryCount(Integer v){beneficiaryCount=v;}
 @Column(name="location",length=255) public String getLocation(){return location;} public void setLocation(String v){location=trim(v);}
 @Column(name="description",columnDefinition="TEXT") public String getDescription(){return description;} public void setDescription(String v){description=v;}
 @Column(name="evidence_json",columnDefinition="TEXT") public String getEvidenceJson(){return evidenceJson;} public void setEvidenceJson(String v){evidenceJson=v;}
 @Column(name="accounting_reference",length=255) public String getAccountingReference(){return accountingReference;} public void setAccountingReference(String v){accountingReference=trim(v);}
 @Column(name="approval_status",length=40) public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=trim(v);}
}
