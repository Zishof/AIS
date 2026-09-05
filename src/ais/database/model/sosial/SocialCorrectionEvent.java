package ais.database.model.sosial;
import java.math.BigDecimal; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.social_correction_event} — catatan permintaan
 * dan riwayat koreksi (correction event) atas data transaksional modul sosial (mis. koreksi
 * nominal transaksi donasi, koreksi alokasi dana, pembatalan bukti setor), bersifat
 * append-only ({@code dynamicUpdate=false} — baris ini tidak dimaksudkan untuk diubah setelah
 * dibuat, hanya disisipkan).
 *
 * <p>
 * {@link #getTargetType()}/{@link #getTargetReference()} menunjuk secara generik ke entitas
 * yang dikoreksi (referensi mentah, bukan relasi Hibernate terpetakan, karena target dapat
 * berupa tabel apa saja di modul sosial). {@link #getPriorState()}/{@link #getResultingState()}
 * merekam snapshot data sebelum dan sesudah koreksi (mis. dalam format JSON) untuk keperluan
 * audit. {@link #getRequestId()} unik per tenant menjamin idempotensi permintaan koreksi.
 * {@link #getApprovalStatus()} melacak alur persetujuan koreksi bila memerlukan otorisasi.
 * Diaudit penuh oleh Hibernate Envers.
 * </p>
 *
 * <p>
 * <b>Investigasi pemakaian:</b> AKTIF, bukan dorman — {@code
 * ais.action.master.sosial.helper.SocialCorrectionService} mengimplementasikan alur
 * maker-checker dua langkah lengkap di atas entitas ini: {@code request(...)} membuat baris
 * berstatus {@code REQUESTED} (memvalidasi tipe REFUND/REVERSAL, nominal, alasan minimal 10
 * karakter, dan idempotency key request-nya sendiri), lalu {@code approveAndPost(...)}
 * memverifikasi total refund/reversal tidak melebihi dana transaksi, mengurangi saldo alokasi
 * ({@link AlokasiDonasi}) yang belum disalurkan, dan menandai baris {@code POSTED}. <b>Catatan
 * keamanan positif:</b> {@code approveAndPost} SECARA EKSPLISIT menolak persetujuan oleh
 * pembuat permintaan yang sama ({@code actorId} pemroses harus berbeda dari {@link
 * #getCreatedBy()}) — mencegah self-approval, kontras dengan sejumlah modul legacy AIS lain di
 * mana celah self-approval pada alur persetujuan diketahui belum ditambal. Ini menguatkan
 * kesimpulan bahwa layer {@code Social*} adalah desain baru dengan standar keamanan lebih ketat
 * daripada rata-rata kode legacy di aplikasi ini, bukan sekadar penamaan Inggris di atas pola
 * lama.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=false) @Table(schema="public",name="social_correction_event",uniqueConstraints=@UniqueConstraint(columnNames={"tenant_key","request_id"}))
public class SocialCorrectionEvent extends SocialRecord { private static final long serialVersionUID=1L; private String targetType,targetReference,correctionType,reason,priorState,resultingState,approvalStatus,actor,requestId; private BigDecimal amount;
 /** Jenis/tipe entitas yang dikoreksi (mis. "TRANSAKSI_DONASI", "ALOKASI_DONASI"). */
 @Column(name="target_type",nullable=false,length=60) public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=trim(v);}
 /** Referensi id entitas target koreksi (mentah, sesuai jenis pada {@link #getTargetType()}). */
 @Column(name="target_reference",nullable=false,length=120) public String getTargetReference(){return targetReference;} public void setTargetReference(String v){targetReference=trim(v);}
 /** Jenis koreksi yang dilakukan (mis. "REVISI_NOMINAL", "PEMBATALAN"). */
 @Column(name="correction_type",nullable=false,length=60) public String getCorrectionType(){return correctionType;} public void setCorrectionType(String v){correctionType=trim(v);}
 /** Alasan/penjelasan koreksi. */
 @Column(name="reason",nullable=false,columnDefinition="TEXT") public String getReason(){return reason;} public void setReason(String v){reason=v;}
 /** Snapshot kondisi data target sebelum koreksi diterapkan (untuk jejak audit). */
 @Column(name="prior_state",columnDefinition="TEXT") public String getPriorState(){return priorState;} public void setPriorState(String v){priorState=v;}
 /** Snapshot kondisi data target setelah koreksi diterapkan (untuk jejak audit). */
 @Column(name="resulting_state",columnDefinition="TEXT") public String getResultingState(){return resultingState;} public void setResultingState(String v){resultingState=v;}
 /** Status persetujuan permintaan koreksi (mis. "MENUNGGU", "DISETUJUI", "DITOLAK"). */
 @Column(name="approval_status",length=40) public String getApprovalStatus(){return approvalStatus;} public void setApprovalStatus(String v){approvalStatus=trim(v);}
 /** Identitas pengguna yang mengajukan/melakukan koreksi. */
 @Column(name="actor",length=255) public String getActor(){return actor;} public void setActor(String v){actor=trim(v);}
 /** Id permintaan koreksi, unik per tenant, untuk menjamin idempotensi. */
 @Column(name="request_id",length=120) public String getRequestId(){return requestId;} public void setRequestId(String v){requestId=trim(v);}
 /** Nilai nominal terkait koreksi (mis. selisih nominal), bila relevan. */
 @Column(name="amount",precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}
