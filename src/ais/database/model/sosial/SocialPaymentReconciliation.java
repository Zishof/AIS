package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate yang memetakan tabel {@code public.social_payment_reconciliation}
 * pada modul donasi/zakat/dana sosial. Merepresentasikan satu baris hasil
 * rekonsiliasi pembayaran donasi ({@link PembayaranDonasi}) terhadap laporan
 * penyelesaian (settlement) dari gerbang pembayaran ({@code gateway}) —
 * dipakai untuk mencocokkan nominal yang seharusnya diterima
 * ({@code expectedAmount}) dengan nominal yang benar-benar diterima
 * ({@code receivedAmount}) setelah dipotong biaya gerbang ({@code fee}),
 * beserta selisihnya ({@code difference}). {@code settlementReference} adalah
 * nomor referensi penyelesaian dari gateway, unik per kombinasi
 * ({@code tenantKey}, {@code gateway}, {@code settlementReference}).
 *
 * <p>
 * Bila ditemukan ketidaksesuaian, jenisnya dicatat di {@code exceptionType}
 * dan catatan peninjauan di {@code notes}, {@code reviewedBy}, {@code
 * reviewedAt} — merepresentasikan alur kerja tim keuangan/bendahara
 * meninjau dan menutup selisih rekonsiliasi. Mewarisi kolom teknis
 * multi-tenant &amp; jejak audit dari {@link SocialRecord}, dan diaudit lewat
 * Hibernate Envers ({@code @Audited}).
 * </p>
 *
 * <p>
 * <b>Investigasi pemakaian:</b> entitas ini AKTIF dipakai produksi, bukan
 * dorman — {@code ais.action.master.sosial.helper.SocialCallbackService}
 * (pemroses webhook gateway SmartLink) menulis satu baris di sini setiap
 * kali callback pembayaran menunjukkan anomali ({@code
 * DUPLICATE_CALLBACK_VARIANT}, {@code CURRENCY_MISMATCH}, {@code
 * AMOUNT_MISMATCH}) — lihat {@code SocialCallbackService#exception}. Kelas
 * ini bagian dari layer bolt-on modern (paket {@code ais.action.master
 * .sosial.helper.Social*}) yang berdiri sendiri, terpisah dari alur
 * rekonsiliasi/akuntansi legacy — jembatannya ke data legacy hanya lewat
 * relasi {@link #getPayment()} ke {@link PembayaranDonasi} (baris legacy
 * yang direkonsiliasi), bukan lewat tabel rekonsiliasi legacy manapun.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="social_payment_reconciliation",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","gateway","settlement_reference"}))
public class SocialPaymentReconciliation extends SocialRecord { private static final long serialVersionUID=1L; private PembayaranDonasi payment; private Date settlementDate,reviewedAt; private String gateway,settlementReference,exceptionType,notes,reviewedBy; private BigDecimal expectedAmount,receivedAmount,fee,difference;
 /** Baris pembayaran donasi legacy ({@link PembayaranDonasi}) yang direkonsiliasi oleh baris ini (opsional — dapat kosong untuk anomali yang belum terkait order tertentu). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="payment_id") public PembayaranDonasi getPayment(){return payment;} public void setPayment(PembayaranDonasi v){payment=v;}
 /** Tanggal laporan penyelesaian (settlement) dari gateway yang menjadi dasar rekonsiliasi ini. */
 @Temporal(TemporalType.DATE) @Column(name="settlement_date") public Date getSettlementDate(){return settlementDate;} public void setSettlementDate(Date v){settlementDate=v;}
 /** Waktu baris ini ditinjau/ditutup oleh {@link #getReviewedBy()}. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="reviewed_at") public Date getReviewedAt(){return reviewedAt;} public void setReviewedAt(Date v){reviewedAt=v;}
 /** Kode gateway pembayaran terkait (mis. {@code "SMARTLINK"}). */
 @Column(name="gateway",length=60) public String getGateway(){return gateway;} public void setGateway(String v){gateway=trim(v);}
 /** Nomor referensi penyelesaian dari gateway; unik per kombinasi tenant+gateway. */
 @Column(name="settlement_reference",length=255) public String getSettlementReference(){return settlementReference;} public void setSettlementReference(String v){settlementReference=trim(v);}
 /** Jenis ketidaksesuaian yang ditemukan (mis. {@code "DUPLICATE_CALLBACK_VARIANT"}, {@code "CURRENCY_MISMATCH"}, {@code "AMOUNT_MISMATCH"} — lihat {@code SocialCallbackService}). */
 @Column(name="exception_type",length=80) public String getExceptionType(){return exceptionType;} public void setExceptionType(String v){exceptionType=trim(v);}
 /** Catatan bebas tim keuangan/bendahara saat meninjau/menutup selisih rekonsiliasi ini. */
 @Column(name="notes",columnDefinition="TEXT") public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
 /** Identitas peninjau yang menutup/menyelesaikan baris rekonsiliasi ini. */
 @Column(name="reviewed_by",length=255) public String getReviewedBy(){return reviewedBy;} public void setReviewedBy(String v){reviewedBy=trim(v);}
 /** Nominal yang seharusnya diterima menurut catatan internal. */
 @Column(name="expected_amount",precision=19,scale=2) public BigDecimal getExpectedAmount(){return expectedAmount;} public void setExpectedAmount(BigDecimal v){expectedAmount=v;}
 /** Nominal yang benar-benar diterima menurut laporan gateway. */
 @Column(name="received_amount",precision=19,scale=2) public BigDecimal getReceivedAmount(){return receivedAmount;} public void setReceivedAmount(BigDecimal v){receivedAmount=v;}
 /** Biaya gateway yang dipotong dari nominal yang diterima. */
 @Column(name="fee",precision=19,scale=2) public BigDecimal getFee(){return fee;} public void setFee(BigDecimal v){fee=v;}
 /** Selisih antara nominal yang diterima dan nominal yang diharapkan ({@link #getReceivedAmount()} dikurangi {@link #getExpectedAmount()}). */
 @Column(name="difference",precision=19,scale=2) public BigDecimal getDifference(){return difference;} public void setDifference(BigDecimal v){difference=v;}
}
