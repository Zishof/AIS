package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.pembayaran_donasi}, merepresentasikan satu upaya
 * pembayaran (payment attempt) lewat payment gateway pihak ketiga untuk melunasi satu
 * {@link #getTransaction() transaksi donasi} ({@link TransaksiDonasi}) pada modul dana
 * sosial/donasi/zakat. Satu transaksi donasi dapat memiliki beberapa baris pembayaran (mis. bila
 * percobaan pertama kedaluwarsa/gagal dan donatur mencoba ulang), sehingga baris ini berperan
 * sebagai log per-percobaan pembayaran, bukan status donasi itu sendiri.
 * <p>
 * Kombinasi {@code gateway_id} + {@code gateway_order_id} dijaga unik lewat
 * {@code @UniqueConstraint} pada tabel, mengidentifikasi transaksi ini secara unik di sisi
 * payment gateway. Field {@code callback*} ({@link #getCallbackTransactionId()},
 * {@link #getCallbackFingerprint()}, {@link #getCallbackPayloadRedacted()}) menyimpan jejak
 * notifikasi callback/webhook dari gateway — {@code callbackPayloadRedacted} secara eksplisit
 * dimaksudkan menyimpan payload yang sudah disensor (redacted) agar data sensitif callback tidak
 * tersimpan mentah, dan {@code callbackFingerprint} (unique) dipakai untuk deteksi/pencegahan
 * pemrosesan callback duplikat. Field {@code settlementBatch}/{@code reconciliationStatus}
 * mendukung proses rekonsiliasi dana dengan laporan settlement gateway.
 * <p>
 * Relasi {@code @ManyToOne} (lazy): {@link #getTransaction()} (transaksi donasi induk) dan
 * {@link #getSosialChannel()} (kanal/program donasi yang menerima dana). Perubahan tercatat
 * historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="pembayaran_donasi",uniqueConstraints={@UniqueConstraint(columnNames={"gateway_id","gateway_order_id"})})
public class PembayaranDonasi extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private SosialChannel sosialChannel; private String gatewayId,gatewayOrderId,gatewayReference,paymentStatus,callbackTransactionId,callbackFingerprint,callbackPayloadRedacted,currency,paymentUrl,settlementBatch,reconciliationStatus,failureReason,requestId; private BigDecimal requestAmount,fee,total; private Date issuedAt,expiryAt,paidAt,lastInquiryAt; private Integer retryCount;
 /** Transaksi donasi induk yang dilunasi lewat percobaan pembayaran ini. Satu transaksi dapat memiliki beberapa baris pembayaran (retry). */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 /** Pengenal payment gateway pihak ketiga yang memproses pembayaran ini (mis. "MIDTRANS", "XENDIT"). */
 @Column(name="gateway_id",nullable=false,length=60) public String getGatewayId(){return gatewayId;} public void setGatewayId(String v){gatewayId=trim(v);}
 /** Nomor order/transaksi di sisi payment gateway, unik bersama {@link #getGatewayId()} (lihat {@code @UniqueConstraint} pada kelas). */
 @Column(name="gateway_order_id",nullable=false,length=120) public String getGatewayOrderId(){return gatewayOrderId;} public void setGatewayOrderId(String v){gatewayOrderId=trim(v);}
 /** Referensi tambahan dari gateway (mis. nomor VA/invoice) untuk pelacakan pembayaran ini. */
 @Column(name="gateway_reference",length=255) public String getGatewayReference(){return gatewayReference;} public void setGatewayReference(String v){gatewayReference=trim(v);}
 /** Status percobaan pembayaran ini (mis. "CREATED", "PENDING", "SUCCESS", "EXPIRED", "FAILED"). Default {@code "CREATED"} bila belum diset. */
 @Column(name="payment_status",nullable=false,length=40) public String getPaymentStatus(){return paymentStatus==null?"CREATED":paymentStatus;} public void setPaymentStatus(String v){paymentStatus=trim(v);}
 /** Pengenal transaksi pada notifikasi callback/webhook terakhir yang diterima dari gateway. */
 @Column(name="callback_transaction_id",length=255) public String getCallbackTransactionId(){return callbackTransactionId;} public void setCallbackTransactionId(String v){callbackTransactionId=trim(v);}
 /** Sidik jari (fingerprint) unik callback terakhir, dipakai untuk deteksi/pencegahan pemrosesan callback duplikat dari gateway. */
 @Column(name="callback_fingerprint",length=128,unique=true) public String getCallbackFingerprint(){return callbackFingerprint;} public void setCallbackFingerprint(String v){callbackFingerprint=trim(v);}
 /** Payload notifikasi callback/webhook dari gateway yang sudah disensor (redacted) &mdash; sengaja tidak menyimpan payload mentah agar data sensitif callback tidak tersimpan apa adanya. */
 @Column(name="callback_payload_redacted",columnDefinition="TEXT") public String getCallbackPayloadRedacted(){return callbackPayloadRedacted;} public void setCallbackPayloadRedacted(String v){callbackPayloadRedacted=v;}
 /** Kode mata uang percobaan pembayaran ini (ISO 4217). Default {@code "IDR"} bila belum diset. */
 @Column(name="currency",nullable=false,length=3) public String getCurrency(){return currency==null?"IDR":currency;} public void setCurrency(String v){currency=trim(v);}
 /** Tautan (URL) halaman pembayaran yang diberikan gateway untuk diselesaikan donatur. */
 @Column(name="payment_url",length=1500) public String getPaymentUrl(){return paymentUrl;} public void setPaymentUrl(String v){paymentUrl=trim(v);}
 /** Pengenal batch settlement gateway tempat pembayaran ini direkonsiliasi dengan laporan dana masuk. */
 @Column(name="settlement_batch",length=120) public String getSettlementBatch(){return settlementBatch;} public void setSettlementBatch(String v){settlementBatch=trim(v);}
 /** Status rekonsiliasi pembayaran ini terhadap laporan settlement gateway (mis. "BELUM_COCOK", "COCOK"). */
 @Column(name="reconciliation_status",length=40) public String getReconciliationStatus(){return reconciliationStatus;} public void setReconciliationStatus(String v){reconciliationStatus=trim(v);}
 /** Alasan kegagalan pembayaran, bila {@link #getPaymentStatus()} menandakan gagal/kedaluwarsa. */
 @Column(name="failure_reason",length=1000) public String getFailureReason(){return failureReason;} public void setFailureReason(String v){failureReason=v;}
 /** Pengenal permintaan (request id) dari sisi pemanggil, untuk penelusuran permintaan pembuatan pembayaran ini. */
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 /** Kanal/program donasi yang menerima dana dari percobaan pembayaran ini. */
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sosial_channel_id",nullable=false) public SosialChannel getSosialChannel(){return sosialChannel;} public void setSosialChannel(SosialChannel v){sosialChannel=v;}
 /** Nominal yang diminta untuk dibayar pada percobaan pembayaran ini (umumnya menyalin {@link TransaksiDonasi#getTotalPayable()} transaksi induk). */
 @Column(name="request_amount",nullable=false,precision=19,scale=2) public BigDecimal getRequestAmount(){return requestAmount;} public void setRequestAmount(BigDecimal v){requestAmount=v;}
 /** Biaya yang dipotong/dibebankan gateway untuk percobaan pembayaran ini. Default {@link BigDecimal#ZERO} bila belum diset. */
 @Column(name="fee",nullable=false,precision=19,scale=2) public BigDecimal getFee(){return fee==null?BigDecimal.ZERO:fee;} public void setFee(BigDecimal v){fee=v;}
 /** Total nominal aktual pada percobaan pembayaran ini (mis. jumlah yang diterima setelah biaya). */
 @Column(name="total",nullable=false,precision=19,scale=2) public BigDecimal getTotal(){return total;} public void setTotal(BigDecimal v){total=v;}
 /** Waktu percobaan pembayaran ini diterbitkan/dibuat di sisi gateway. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="issued_at") public Date getIssuedAt(){return issuedAt;} public void setIssuedAt(Date v){issuedAt=v;}
 /** Batas waktu percobaan pembayaran ini berlaku sebelum dianggap kedaluwarsa. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expiry_at") public Date getExpiryAt(){return expiryAt;} public void setExpiryAt(Date v){expiryAt=v;}
 /** Waktu pembayaran ini dikonfirmasi lunas oleh gateway. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="paid_at") public Date getPaidAt(){return paidAt;} public void setPaidAt(Date v){paidAt=v;}
 /** Waktu terakhir status pembayaran ini ditanyakan ulang (inquiry) secara aktif ke gateway, mis. saat callback tidak kunjung diterima. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="last_inquiry_at") public Date getLastInquiryAt(){return lastInquiryAt;} public void setLastInquiryAt(Date v){lastInquiryAt=v;}
 /** Jumlah percobaan inquiry/pengecekan ulang status yang sudah dilakukan ke gateway untuk pembayaran ini. Default 0 bila belum diset. */
 @Column(name="retry_count") public Integer getRetryCount(){return retryCount==null?0:retryCount;} public void setRetryCount(Integer v){retryCount=v;}
}
