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
 * <p>
 * <b>Evaluasi keamanan/integritas proses (dicek saat dokumentasi ditulis) — anonimitas
 * reviewer/penulis:</b> {@link #getAnonymityMode()} divalidasi sebagai salah satu dari
 * {@code DOUBLE_ANONYMOUS}/{@code SINGLE_ANONYMOUS}/{@code OPEN} oleh
 * {@code ais.action.master.jurnal.JurnalWorkflowService#inviteReviewer}, namun nilai ini HANYA
 * disimpan — tidak ada kode di {@code JurnalWorkflowService} maupun
 * {@code ais.action.master.jurnal.JurnalReportService} yang membaca kembali
 * {@code anonymityMode} untuk menyembunyikan {@link #getReviewerId()} dari penulis naskah atau
 * menyembunyikan identitas penulis dari reviewer. Secara khusus,
 * {@code JurnalReportService} (laporan "REVIEWS") mengekspor {@code reviewer_id} apa adanya ke
 * siapa pun yang lolos {@code auth.requireRead(actor,"laporan")} +
 * {@code auth.requireJournalScope(...)} untuk jurnal terkait, tanpa mempertimbangkan mode
 * anonimitas penugasan. Ini MEMPERKUAT pola yang sudah tercatat pada
 * {@code ais.database.model.Diskusi} (kebijakan anonimitas disimpan tapi tidak pernah
 * ditegakkan) — bukan temuan baru yang independen; lihat catatan proyek terkait
 * {@code Diskusi.java}. Redaksi identitas berdasarkan {@code anonymityMode}, bila diperlukan,
 * harus diimplementasikan di lapisan presentasi/API yang menyusun data untuk ditampilkan ke
 * penulis atau reviewer, bukan diasumsikan sudah terjadi di sini.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="penugasan_reviewer_jurnal")
public class PenugasanReviewerJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long itemId; private Integer roundNumber; private String reviewerId,status,anonymityMode,recommendation,formVersionKey,responseJson,responseChecksum,conflictJson; private Date invitedAt,responseDueAt,reviewDueAt,acceptedAt,declinedAt,completedAt,cancelledAt;
 /** Id item/naskah (submission) repository yang menjadi objek review ini; kolom FK biasa, bukan relasi Hibernate. */
 @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 /**
  * Identitas pengguna reviewer yang ditugaskan (kolom teks/FK longgar, bukan relasi objek).
  * Lihat catatan anonimitas pada Javadoc kelas: nilai ini disimpan apa adanya terlepas dari
  * {@link #getAnonymityMode()}, sehingga siapa pun yang punya akses baca ke baris/laporan ini
  * dapat melihat identitas reviewer secara langsung.
  */
 @Column(name="reviewer_id",nullable=false,length=255) public String getReviewerId(){return reviewerId;} public void setReviewerId(String v){reviewerId=v;}
 /** Nomor ronde review (mulai dari 1); satu naskah dapat melalui beberapa ronde review berurutan, masing-masing baris penugasan terpisah. */
 @Column(name="round_number",nullable=false) public Integer getRoundNumber(){return roundNumber;} public void setRoundNumber(Integer v){roundNumber=v;}
 /** Status siklus hidup penugasan (mis. "INVITED", "ACCEPTED", "DECLINED", "COMPLETED", "CANCELLED"). */
 @Column(name="status",nullable=false,length=40) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /**
  * Skema keanoniman review: {@code DOUBLE_ANONYMOUS} (penulis dan reviewer sama-sama tidak
  * tahu identitas satu sama lain), {@code SINGLE_ANONYMOUS} (reviewer anonim bagi penulis,
  * tidak sebaliknya), atau {@code OPEN} (identitas terbuka). <b>Nilai ini murni deklaratif</b> —
  * lihat catatan keamanan pada Javadoc kelas mengenai tidak adanya penegakan otomatis.
  */
 @Column(name="anonymity_mode",nullable=false,length=30) public String getAnonymityMode(){return anonymityMode;} public void setAnonymityMode(String v){anonymityMode=v;}
 /** Rekomendasi keputusan hasil review (mis. "ACCEPT", "MINOR_REVISION", "MAJOR_REVISION", "REJECT"); diisi saat review diselesaikan. */
 @Column(name="recommendation",length=80) public String getRecommendation(){return recommendation;} public void setRecommendation(String v){recommendation=v;}
 /** Kunci versi skema formulir review yang dipakai, agar {@link #getResponseJson()} dapat ditafsirkan sesuai struktur formulir pada versi tersebut. */
 @Column(name="form_version_key",length=120) public String getFormVersionKey(){return formVersionKey;} public void setFormVersionKey(String v){formVersionKey=v;}
 /** Isi lengkap formulir review yang diserahkan reviewer, dalam format JSON, mengikuti skema {@link #getFormVersionKey()}. */
 @Column(name="response_json",columnDefinition="text") public String getResponseJson(){return responseJson;} public void setResponseJson(String v){responseJson=v;}
 /** Checksum (SHA-256) dari {@link #getResponseJson()} pada saat diserahkan, untuk verifikasi bahwa isi respons tidak berubah setelah disimpan. */
 @Column(name="response_checksum",length=64) public String getResponseChecksum(){return responseChecksum;} public void setResponseChecksum(String v){responseChecksum=v;}
 /** Deklarasi konflik kepentingan reviewer terhadap naskah ini, dalam format JSON, bila reviewer mengisi deklarasi tersebut. */
 @Column(name="conflict_json",columnDefinition="text") public String getConflictJson(){return conflictJson;} public void setConflictJson(String v){conflictJson=v;}
 /** Waktu reviewer diundang untuk menugaskan review ini. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="invited_at") public Date getInvitedAt(){return invitedAt;} public void setInvitedAt(Date v){invitedAt=v;}
 /** Tenggat waktu bagi reviewer untuk merespons (menerima/menolak) undangan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="response_due_at") public Date getResponseDueAt(){return responseDueAt;} public void setResponseDueAt(Date v){responseDueAt=v;}
 /** Tenggat waktu bagi reviewer untuk menyelesaikan review, dihitung sejak undangan diterima. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="review_due_at") public Date getReviewDueAt(){return reviewDueAt;} public void setReviewDueAt(Date v){reviewDueAt=v;}
 /** Waktu reviewer menerima undangan review; kosong bila belum direspons atau ditolak/dibatalkan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="accepted_at") public Date getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(Date v){acceptedAt=v;}
 /** Waktu reviewer menolak undangan review; kosong bila belum direspons atau diterima/dibatalkan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="declined_at") public Date getDeclinedAt(){return declinedAt;} public void setDeclinedAt(Date v){declinedAt=v;}
 /** Waktu reviewer menyelesaikan dan menyerahkan hasil review; kosong bila review belum selesai. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="completed_at") public Date getCompletedAt(){return completedAt;} public void setCompletedAt(Date v){completedAt=v;}
 /** Waktu penugasan dibatalkan (mis. oleh editor, bukan oleh reviewer sendiri); kosong bila penugasan masih berjalan atau sudah selesai normal. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="cancelled_at") public Date getCancelledAt(){return cancelledAt;} public void setCancelledAt(Date v){cancelledAt=v;}
}
