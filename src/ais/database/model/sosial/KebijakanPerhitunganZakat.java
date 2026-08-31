package ais.database.model.sosial;

import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.kebijakan_perhitungan_zakat}, merepresentasikan
 * satu versi kebijakan/aturan perhitungan zakat untuk satu {@link #getJenisZakat()} (mis. zakat
 * maal, zakat fitrah, zakat penghasilan) pada modul sosial. Baris ini adalah data konfigurasi
 * yang menjadi acuan (referensi) ketika sebuah {@link PerhitunganZakat} dibuat — bukan hasil
 * perhitungan itu sendiri.
 * <p>
 * Parameter perhitungan mencakup ambang nisab ({@link #getNisabBasis()},
 * {@link #getNisabQuantity()}, {@link #getNisabUnit()}), tarif/kadar zakat
 * ({@link #getRate()}), harga acuan komoditas nisab pada waktu tertentu
 * ({@link #getReferencePrice()}/{@link #getReferencePriceAt()}), lama kepemilikan harta yang
 * disyaratkan ({@link #getHaulMonths()}), serta nilai zakat fitrah dalam bentuk uang maupun
 * beras ({@link #getFitrahCash()}/{@link #getFitrahKg()}). Formula aktual yang dipakai untuk
 * menghitung dirujuk lewat kunci {@link #getFormulaKey()} (diimplementasikan di lapisan
 * servis/aksi, bukan di entitas ini), dengan pembulatan hasil diatur lewat
 * {@link #getRoundingMode()}/{@link #getResultScale()}.
 * <p>
 * Kebijakan berlaku untuk suatu periode ({@link #getEffectiveFrom()}&ndash;
 * {@link #getEffectiveUntil()}) dan dapat berbeda per wilayah ({@link #getRegion()}); setiap
 * revisi kebijakan dicatat sebagai versi baru ({@link #getVersion()}) lengkap dengan sumber
 * rujukan ({@link #getSourceReference()}) dan siapa/kapan kebijakan disetujui
 * ({@link #getApprovedBy()}/{@link #getApprovedAt()}).
 * <p>
 * Mewarisi kolom multi-tenant {@code tenant_key} dari {@link SocialRecord}. Perubahan
 * (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate Envers).
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="kebijakan_perhitungan_zakat")
public class KebijakanPerhitunganZakat extends SocialRecord {
 private static final long serialVersionUID=1L; private JenisZakat jenisZakat; private String region,version,sourceReference,nisabBasis,nisabUnit,formulaKey,roundingMode,approvedBy; private Date effectiveFrom,effectiveUntil,referencePriceAt,approvedAt; private BigDecimal nisabQuantity,rate,referencePrice,fitrahCash,fitrahKg; private Integer haulMonths,resultScale;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="jenis_zakat_id",nullable=false) public JenisZakat getJenisZakat(){return jenisZakat;} public void setJenisZakat(JenisZakat v){jenisZakat=v;}
 @Column(name="region",length=120) public String getRegion(){return region;} public void setRegion(String v){region=trim(v);}
 @Column(name="version",nullable=false,length=40) public String getVersion(){return version;} public void setVersion(String v){version=trim(v);}
 @Column(name="source_reference",columnDefinition="TEXT") public String getSourceReference(){return sourceReference;} public void setSourceReference(String v){sourceReference=v;}
 @Column(name="nisab_basis",length=60) public String getNisabBasis(){return nisabBasis;} public void setNisabBasis(String v){nisabBasis=trim(v);}
 @Column(name="nisab_unit",length=40) public String getNisabUnit(){return nisabUnit;} public void setNisabUnit(String v){nisabUnit=trim(v);}
 @Column(name="formula_key",nullable=false,length=60) public String getFormulaKey(){return formulaKey;} public void setFormulaKey(String v){formulaKey=trim(v);}
 @Column(name="rounding_mode",length=40) public String getRoundingMode(){return roundingMode;} public void setRoundingMode(String v){roundingMode=trim(v);}
 @Column(name="approved_by",length=255) public String getApprovedBy(){return approvedBy;} public void setApprovedBy(String v){approvedBy=trim(v);}
 @Temporal(TemporalType.DATE) @Column(name="effective_from",nullable=false) public Date getEffectiveFrom(){return effectiveFrom;} public void setEffectiveFrom(Date v){effectiveFrom=v;}
 @Temporal(TemporalType.DATE) @Column(name="effective_until") public Date getEffectiveUntil(){return effectiveUntil;} public void setEffectiveUntil(Date v){effectiveUntil=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="reference_price_at") public Date getReferencePriceAt(){return referencePriceAt;} public void setReferencePriceAt(Date v){referencePriceAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="approved_at") public Date getApprovedAt(){return approvedAt;} public void setApprovedAt(Date v){approvedAt=v;}
 @Column(name="nisab_quantity",precision=19,scale=6) public BigDecimal getNisabQuantity(){return nisabQuantity;} public void setNisabQuantity(BigDecimal v){nisabQuantity=v;}
 @Column(name="rate",precision=19,scale=8) public BigDecimal getRate(){return rate;} public void setRate(BigDecimal v){rate=v;}
 @Column(name="reference_price",precision=19,scale=2) public BigDecimal getReferencePrice(){return referencePrice;} public void setReferencePrice(BigDecimal v){referencePrice=v;}
 @Column(name="fitrah_cash",precision=19,scale=2) public BigDecimal getFitrahCash(){return fitrahCash;} public void setFitrahCash(BigDecimal v){fitrahCash=v;}
 @Column(name="fitrah_kg",precision=19,scale=4) public BigDecimal getFitrahKg(){return fitrahKg;} public void setFitrahKg(BigDecimal v){fitrahKg=v;}
 @Column(name="haul_months") public Integer getHaulMonths(){return haulMonths;} public void setHaulMonths(Integer v){haulMonths=v;}
 @Column(name="result_scale") public Integer getResultScale(){return resultScale==null?2:resultScale;} public void setResultScale(Integer v){resultScale=v;}
}
