package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate yang memetakan tabel {@code public.social_program_extension}
 * pada modul donasi/zakat/dana sosial. Merupakan perluasan satu-ke-satu
 * ({@code @OneToOne}, unik per {@code program_id}) dari {@link ProgramDonatur}
 * (program penggalangan dana inti) yang menambahkan data khusus tampilan
 * publik/halaman kampanye galang dana: slug URL ({@code slug}, unik per
 * {@code tenantKey}), ringkasan &amp; cerita panjang ({@code shortDescription},
 * {@code longStory}), gambar sampul ({@code coverUrl}), status publikasi
 * ({@code publicStatus}, default {@code "DRAFT"}), lokasi sasaran
 * ({@code targetLocation}), keterbukaan informasi legal
 * ({@code legalDisclosure}), target nominal &amp; nominal donasi minimum
 * ({@code targetAmount}, {@code minimumDonation}), target jumlah penerima
 * manfaat ({@code targetBeneficiaries}), serta flag tampilan
 * ({@code featured} - ditonjolkan, {@code restricted} - dana terikat/tidak
 * bebas dialokasikan, {@code allowAnonymous} - izinkan donasi anonim, default
 * {@code true}) dan waktu publikasi ({@code publishedAt}).
 *
 * <p>
 * Berelasi many-to-one wajib ke {@link JenisDanaSosial} (jenis dana sosial,
 * mis. zakat/infak/sedekah/wakaf) lewat {@code fundType}. Mewarisi kolom
 * teknis multi-tenant ({@code tenantKey},
 * {@code status}, jejak audit {@code createdAt}/{@code updatedAt}/
 * {@code createdBy}/{@code updatedBy}) dari {@link SocialRecord}, dan diaudit
 * lewat Hibernate Envers ({@code @Audited}).
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="social_program_extension",uniqueConstraints={@UniqueConstraint(columnNames={"program_id"}),@UniqueConstraint(columnNames={"tenant_key","slug"})})
public class SocialProgramExtension extends SocialRecord { private static final long serialVersionUID=1L; private ProgramDonatur program; private JenisDanaSosial fundType; private String slug,shortDescription,longStory,coverUrl,publicStatus,targetLocation,legalDisclosure; private BigDecimal targetAmount,minimumDonation; private Integer targetBeneficiaries; private Boolean featured,restricted,allowAnonymous; private Date publishedAt;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id",nullable=false) public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 @Column(name="slug",nullable=false,length=180) public String getSlug(){return slug;} public void setSlug(String v){slug=trim(v);}
 @Column(name="short_description",length=500) public String getShortDescription(){return shortDescription;} public void setShortDescription(String v){shortDescription=trim(v);}
 @Column(name="long_story",columnDefinition="TEXT") public String getLongStory(){return longStory;} public void setLongStory(String v){longStory=v;}
 @Column(name="cover_url",length=1000) public String getCoverUrl(){return coverUrl;} public void setCoverUrl(String v){coverUrl=trim(v);}
 @Column(name="public_status",nullable=false,length=40) public String getPublicStatus(){return publicStatus==null?"DRAFT":publicStatus;} public void setPublicStatus(String v){publicStatus=trim(v);}
 @Column(name="target_location",length=255) public String getTargetLocation(){return targetLocation;} public void setTargetLocation(String v){targetLocation=trim(v);}
 @Column(name="legal_disclosure",columnDefinition="TEXT") public String getLegalDisclosure(){return legalDisclosure;} public void setLegalDisclosure(String v){legalDisclosure=v;}
 @Column(name="target_amount",precision=19,scale=2) public BigDecimal getTargetAmount(){return targetAmount;} public void setTargetAmount(BigDecimal v){targetAmount=v;}
 @Column(name="minimum_donation",precision=19,scale=2) public BigDecimal getMinimumDonation(){return minimumDonation;} public void setMinimumDonation(BigDecimal v){minimumDonation=v;}
 @Column(name="target_beneficiaries") public Integer getTargetBeneficiaries(){return targetBeneficiaries;} public void setTargetBeneficiaries(Integer v){targetBeneficiaries=v;}
 @Column(name="featured") public Boolean getFeatured(){return Boolean.TRUE.equals(featured);} public void setFeatured(Boolean v){featured=v;}
 @Column(name="restricted") public Boolean getRestricted(){return Boolean.TRUE.equals(restricted);} public void setRestricted(Boolean v){restricted=v;}
 @Column(name="allow_anonymous") public Boolean getAllowAnonymous(){return !Boolean.FALSE.equals(allowAnonymous);} public void setAllowAnonymous(Boolean v){allowAnonymous=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="published_at") public Date getPublishedAt(){return publishedAt;} public void setPublishedAt(Date v){publishedAt=v;}
}
