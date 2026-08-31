package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.penugasan_reviewer_jurnal},
 * merepresentasikan satu penugasan reviewer (mitra bestari) untuk me-review satu naskah/artikel
 * pada sistem jurnal ilmiah OJS-style di AIS. Naskah yang direview dirujuk lewat
 * {@link #getItemId()} (id item/submission artikel — tidak dipetakan sebagai relasi
 * {@code @ManyToOne} eksplisit, hanya kolom FK biasa) dan reviewer dirujuk lewat
 * {@link #getReviewerId()} (id user reviewer, juga berupa kolom teks/FK longgar, bukan relasi
 * objek). Satu naskah dapat melalui beberapa ronde review, dilacak lewat {@link #getRoundNumber()}.
 * <p>
 * {@link #getAnonymityMode()} menentukan skema keanoniman proses review (mis. single/double-blind
 * antara penulis dan reviewer). Siklus hidup penugasan dilacak lewat rangkaian timestamp
 * {@link #getInvitedAt()} (diundang) → {@link #getAcceptedAt()}/{@link #getDeclinedAt()} (respons
 * menerima/menolak, dengan tenggat {@link #getResponseDueAt()}) → {@link #getCompletedAt()}
 * (review selesai, dengan tenggat {@link #getReviewDueAt()}) atau {@link #getCancelledAt()}
 * (dibatalkan), dengan {@link #getStatus()} sebagai penanda status kini. Hasil review tersimpan
 * sebagai {@link #getRecommendation()} (rekomendasi keputusan) dan {@link #getResponseJson()}
 * (isi lengkap formulir review dalam JSON, mengikuti skema {@link #getFormVersionKey()}), dengan
 * {@link #getResponseChecksum()} untuk verifikasi integritas data respons dan
 * {@link #getConflictJson()} untuk mencatat deklarasi konflik kepentingan reviewer.
 * <p>
 * Kolom teknis bersama (id, tenant, audit, versi optimistic-locking) diwariskan dari
 * {@link JurnalEntityBase}.
 */
@Entity @Table(schema="penelitiandanpengabdian",name="penugasan_reviewer_jurnal")
public class PenugasanReviewerJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long itemId; private Integer roundNumber; private String reviewerId,status,anonymityMode,recommendation,formVersionKey,responseJson,responseChecksum,conflictJson; private Date invitedAt,responseDueAt,reviewDueAt,acceptedAt,declinedAt,completedAt,cancelledAt;
 @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 @Column(name="reviewer_id",nullable=false,length=255) public String getReviewerId(){return reviewerId;} public void setReviewerId(String v){reviewerId=v;}
 @Column(name="round_number",nullable=false) public Integer getRoundNumber(){return roundNumber;} public void setRoundNumber(Integer v){roundNumber=v;}
 @Column(name="status",nullable=false,length=40) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 @Column(name="anonymity_mode",nullable=false,length=30) public String getAnonymityMode(){return anonymityMode;} public void setAnonymityMode(String v){anonymityMode=v;}
 @Column(name="recommendation",length=80) public String getRecommendation(){return recommendation;} public void setRecommendation(String v){recommendation=v;}
 @Column(name="form_version_key",length=120) public String getFormVersionKey(){return formVersionKey;} public void setFormVersionKey(String v){formVersionKey=v;}
 @Column(name="response_json",columnDefinition="text") public String getResponseJson(){return responseJson;} public void setResponseJson(String v){responseJson=v;}
 @Column(name="response_checksum",length=64) public String getResponseChecksum(){return responseChecksum;} public void setResponseChecksum(String v){responseChecksum=v;}
 @Column(name="conflict_json",columnDefinition="text") public String getConflictJson(){return conflictJson;} public void setConflictJson(String v){conflictJson=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="invited_at") public Date getInvitedAt(){return invitedAt;} public void setInvitedAt(Date v){invitedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="response_due_at") public Date getResponseDueAt(){return responseDueAt;} public void setResponseDueAt(Date v){responseDueAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="review_due_at") public Date getReviewDueAt(){return reviewDueAt;} public void setReviewDueAt(Date v){reviewDueAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="accepted_at") public Date getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(Date v){acceptedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="declined_at") public Date getDeclinedAt(){return declinedAt;} public void setDeclinedAt(Date v){declinedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="completed_at") public Date getCompletedAt(){return completedAt;} public void setCompletedAt(Date v){completedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="cancelled_at") public Date getCancelledAt(){return cancelledAt;} public void setCancelledAt(Date v){cancelledAt=v;}
}
