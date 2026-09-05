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
 /** Mengembalikan event/batch penyaluran induk, apa adanya (tanpa {@code check()} lazy-resolve — berbeda dari pola getter relasi entitas legacy {@link ProgramDonatur}/{@link PenyaluranDonasi} di paket ini). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="distribution_id",nullable=false) public PenyaluranDonasi getDistribution(){return distribution;}
 /** Menyetel event/batch penyaluran induk. Tanpa validasi; wajib diisi sebelum baris disimpan ({@code nullable=false}). */
 public void setDistribution(PenyaluranDonasi v){distribution=v;}
 /** Mengembalikan jenis dana sosial (mis. Zakat/Infaq/Sedekah) detail ini. Dipakai {@link ais.action.master.sosial.helper.SocialDistributionService#post} untuk memvalidasi kecocokan dengan jenis dana {@link #sourceAllocation} dan kompatibilitas dana {@code restricted} terhadap {@link #beneficiaryCategory}. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;}
 /** Menyetel jenis dana sosial. Tanpa validasi; wajib diisi. */
 public void setFundType(JenisDanaSosial v){fundType=v;}
 /** Mengembalikan program donatur legacy terkait, bila detail ini terikat pada satu {@link ProgramDonatur} tertentu. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id") public ProgramDonatur getProgram(){return program;}
 /** Menyetel program donatur terkait. Opsional ({@code program_id} tanpa {@code nullable=false}); tanpa validasi. */
 public void setProgram(ProgramDonatur v){program=v;}
 /** Mengembalikan kategori penerima manfaat detail ini. Dipakai bersama {@link #fundType} untuk memvalidasi kompatibilitas dana {@code restricted} pada {@link ais.action.master.sosial.helper.SocialDistributionService#post}. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="beneficiary_category_id",nullable=false) public KategoriPenerimaManfaat getBeneficiaryCategory(){return beneficiaryCategory;}
 /** Menyetel kategori penerima manfaat. Tanpa validasi; wajib diisi. */
 public void setBeneficiaryCategory(KategoriPenerimaManfaat v){beneficiaryCategory=v;}
 /** Mengembalikan alokasi donasi sumber yang mendanai penyaluran ini — saldo alokasi inilah yang dikurangi/divalidasi (jumlah baris {@code POSTED} tidak melebihi {@link ais.database.model.sosial.AlokasiDonasi#getAmount()}) oleh {@link ais.action.master.sosial.helper.SocialDistributionService#post}. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_allocation_id",nullable=false) public AlokasiDonasi getSourceAllocation(){return sourceAllocation;}
 /** Menyetel alokasi donasi sumber. Tanpa validasi; wajib diisi. */
 public void setSourceAllocation(AlokasiDonasi v){sourceAllocation=v;}
 /** Mengembalikan nominal dana yang disalurkan pada detail ini, apa adanya. Divalidasi harus positif ({@code signum()>0}) dan tidak melebihi sisa saldo alokasi sumber saat diposting — lihat {@link ais.action.master.sosial.helper.SocialDistributionService#post}; entitas ini sendiri tidak menegakkan validasi tersebut. */
 @Column(name="amount",nullable=false,precision=19,scale=2) public BigDecimal getAmount(){return amount;}
 /** Menyetel nominal dana yang disalurkan. Tanpa validasi pada level entitas. */
 public void setAmount(BigDecimal v){amount=v;}
 /** Mengembalikan tanggal penyaluran, apa adanya. Bila belum diisi saat diposting, {@link ais.action.master.sosial.helper.SocialDistributionService#post} mengisinya otomatis dengan waktu posting. */
 @Temporal(TemporalType.DATE) @Column(name="distribution_date",nullable=false) public Date getDistributionDate(){return distributionDate;}
 /** Menyetel tanggal penyaluran. Tanpa validasi. */
 public void setDistributionDate(Date v){distributionDate=v;}
 /** Mengembalikan jumlah penerima manfaat pada detail ini, apa adanya; boleh {@code null} (kolom tidak {@code nullable=false}). */
 @Column(name="beneficiary_count") public Integer getBeneficiaryCount(){return beneficiaryCount;}
 /** Menyetel jumlah penerima manfaat. Tanpa validasi. */
 public void setBeneficiaryCount(Integer v){beneficiaryCount=v;}
 /** Mengembalikan lokasi penyaluran, apa adanya. */
 @Column(name="location",length=255) public String getLocation(){return location;}
 /** Menyetel lokasi penyaluran, dipangkas spasi awal/akhir lewat {@link SocialRecord#trim(String)}. */
 public void setLocation(String v){location=trim(v);}
 /** Mengembalikan deskripsi bebas penyaluran ini, apa adanya. */
 @Column(name="description",columnDefinition="TEXT") public String getDescription(){return description;}
 /** Menyetel deskripsi bebas. Tanpa validasi/trim pada setter (berbeda dari {@link #getLocation()}/{@link #setLocation(String)} yang dipangkas). */
 public void setDescription(String v){description=v;}
 /** Mengembalikan bukti penyaluran sebagai teks JSON mentah (mis. daftar url foto/dokumen); tidak divalidasi skema di sini. */
 @Column(name="evidence_json",columnDefinition="TEXT") public String getEvidenceJson(){return evidenceJson;}
 /** Menyetel bukti penyaluran (teks JSON mentah). Tanpa validasi skema pada setter. */
 public void setEvidenceJson(String v){evidenceJson=v;}
 /** Mengembalikan referensi jurnal akuntansi terkait detail ini, apa adanya; teks bebas, tidak divalidasi/dihubungkan lewat foreign key. */
 @Column(name="accounting_reference",length=255) public String getAccountingReference(){return accountingReference;}
 /** Menyetel referensi jurnal akuntansi, dipangkas spasi awal/akhir. */
 public void setAccountingReference(String v){accountingReference=trim(v);}
 /**
  * Mengembalikan status persetujuan detail penyaluran ini, apa adanya — bebas teks (bukan enum
  * tervalidasi pada level entitas); nilai yang diketahui dipakai kode pemanggil antara lain
  * {@code "POSTED"} (lihat {@link ais.action.master.sosial.helper.SocialDistributionService#post},
  * yang juga memvalidasi transisi status lewat {@code SocialStateMachine.requireDistribution}
  * sebelum mengizinkan perubahan ke {@code POSTED}). <b>Verifikasi eksplisit gerbang
  * persetujuan:</b> kolom ini sendiri hanyalah data; gerbang yang sesungguhnya menegakkan
  * transisi status yang sah dan privilese {@code FINANCE} berada di layanan
  * {@code SocialDistributionService} tersebut (server-side, bukan sekadar UI) — lihat javadoc
  * method {@code post(SocialRequestContext, Long)} untuk alur validasi lengkapnya.
  */
 @Column(name="approval_status",length=40) public String getApprovalStatus(){return approvalStatus;}
 /** Menyetel status persetujuan, dipangkas spasi awal/akhir. Tanpa validasi nilai pada level entitas — validasi transisi status yang sah ditegakkan di layer service, bukan di sini. */
 public void setApprovalStatus(String v){approvalStatus=trim(v);}
}
