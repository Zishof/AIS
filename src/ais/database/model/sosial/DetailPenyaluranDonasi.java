package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate: satu baris rincian penyaluran dana pada modul sosial/donasi AIS — dipetakan
 * ke tabel {@code public.detail_penyaluran_donasi}. Merupakan pecahan/detail dari satu
 * {@link PenyaluranDonasi} (event/batch penyaluran) untuk kombinasi {@link #fundType} (jenis dana,
 * mis. Zakat/Infaq/Sedekah), {@link #beneficiaryCategory} (kategori penerima manfaat), dan
 * {@link #sourceAllocation} (alokasi dana sumber yang dipakai) tertentu — memungkinkan satu event
 * penyaluran mencakup banyak kombinasi jenis-dana/kategori-penerima dalam baris terpisah, masing-
 * masing dengan nominal ({@link #amount}), jumlah penerima manfaat, lokasi, bukti penyaluran
 * ({@link #evidenceJson}), referensi akuntansi, dan status persetujuannya sendiri. {@link #program}
 * (program donatur) opsional, dipakai bila penyaluran ini terikat pada program donasi tertentu.
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="detail_penyaluran_donasi")
public class DetailPenyaluranDonasi extends SocialRecord {
 private static final long serialVersionUID=1L;
 /** Event/batch penyaluran donasi induk dari detail ini (wajib). */
 private PenyaluranDonasi distribution;
 /** Jenis dana sosial yang disalurkan pada detail ini (mis. Zakat/Infaq/Sedekah, wajib). */
 private JenisDanaSosial fundType;
 /** Program donatur terkait, bila penyaluran ini terikat pada program donasi tertentu (opsional). */
 private ProgramDonatur program;
 /** Kategori penerima manfaat penyaluran ini (wajib). */
 private KategoriPenerimaManfaat beneficiaryCategory;
 /** Alokasi donasi sumber yang dipakai untuk mendanai penyaluran ini (wajib). */
 private AlokasiDonasi sourceAllocation;
 /** Nominal dana yang disalurkan pada detail ini. */
 private BigDecimal amount;
 private Date distributionDate;
 private Integer beneficiaryCount;
 /** Lokasi penyaluran; deskripsi bebas; bukti penyaluran sebagai JSON mentah (mis. daftar foto/dokumen, tidak divalidasi skema di entitas); referensi jurnal akuntansi terkait; dan status persetujuan detail penyaluran ini (bebas teks). */
 private String location,description,evidenceJson,accountingReference,approvalStatus;
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
