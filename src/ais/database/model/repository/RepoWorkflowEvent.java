package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_workflow_event} —
 * audit trail append-only (immutable, {@code dynamicUpdate = false}) untuk
 * setiap transisi status submission/review pada modul repositori
 * institusional (mirip DSpace — lihat {@link RepoItem#getWorkflowStatus()}).
 *
 * <p>
 * <b>PENTING (hasil verifikasi):</b> alur kerja repositori ({@code SUBMIT},
 * {@code CLAIM}, {@code RETURN}, {@code REJECT}, {@code APPROVE},
 * {@code PUBLISH}, {@code WITHDRAW}, {@code RESTORE}) diimplementasikan
 * sebagai <b>state machine independen</b> di
 * {@code ais.action.master.repository.RepositoryWorkflowService} — TIDAK
 * memakai mesin alur kerja generik {@code AlurSop}/{@code DisposisiSop}.
 * Gerbang transisi di sana bersifat <b>server-side</b> yang sah, bukan
 * hanya UI-only: setiap perubahan status divalidasi lewat
 * {@code requireTransition(from,to)} (tabel transisi eksplisit),
 * {@code requireReviewer(actor)}/{@code requireDepositor(actor)} (peran
 * aktor) dan {@code verifyVersion(item, expectedVersion)} (optimistic
 * locking) sebelum baris kejadian ini ditulis — sehingga bug bypass-persetujuan
 * via mesin {@code AlurSop}/{@code DisposisiSop} generik (lihat investigasi
 * batch 116) TIDAK berlaku untuk modul repositori ini.
 * </p>
 *
 * <p>
 * Satu baris merepresentasikan satu langkah transisi
 * ({@link #getFromStatus()} &rarr; {@link #getToStatus()}) beserta aktor
 * pelaku ({@link #getActorId()}), komentar/alasan opsional
 * ({@link #getCommentText()}), dan penanda ronde review
 * ({@link #getRoundNumber()}) untuk item bersiklus revisi berulang.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = false)
@Table(schema = "public", name = "repo_workflow_event")
public class RepoWorkflowEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long itemId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private String commentText;
    private String actorId;
    private String actorName;
    private String requestId;
    private Date createdAt;
    private Integer roundNumber;

    /** Id baris kejadian alur kerja ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, nullable = false, unique = true)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    /** Id {@link RepoItem} yang mengalami transisi status ini. */
    @Column(name = "item_id", nullable = false)
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    /** Status workflow sebelum transisi (mis. {@code "IN_REVIEW"}); {@code null} untuk kejadian yang tidak berpindah status, mis. {@code COMMENT}/{@code AUTOSAVE}. */
    @Column(name = "from_status", length = 40)
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    /** Status workflow sesudah transisi (mis. {@code "APPROVED"}, {@code "PUBLISHED"}) — wajib diisi untuk setiap baris kejadian. */
    @Column(name = "to_status", nullable = false, length = 40)
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    /** Kode aksi yang memicu kejadian ini, mis. {@code "SUBMIT"}, {@code "CLAIM"}, {@code "REJECT"}, {@code "APPROVE"}, {@code "PUBLISH"}, {@code "WITHDRAW"}, {@code "RESTORE"}, {@code "COMMENT"}, {@code "AUTOSAVE"} — lihat {@code RepositoryWorkflowService}. */
    @Column(name = "action", nullable = false, length = 40)
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    /** Komentar/alasan yang menyertai aksi (mis. alasan penolakan/penarikan, permintaan revisi) — wajib diisi non-kosong untuk aksi tertentu seperti REJECT/RETURN/WITHDRAW di layer service. */
    @Column(name = "comment_text", columnDefinition = "TEXT")
    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }
    /** Id pengguna yang melakukan aksi ini — wajib diisi, dasar audit "siapa mengubah status apa". */
    @Column(name = "actor_id", nullable = false, length = 255)
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    /** Nama tampilan aktor pada saat kejadian terjadi (field audit shadow — disalin agar riwayat tetap terbaca meski nama akun berubah/dihapus kemudian). */
    @Column(name = "actor_name", length = 500)
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    /** Id korelasi permintaan (request id) dari pemanggil, untuk menelusuri satu aksi yang sama lintas log/audit lain. */
    @Column(name = "request_id", length = 100)
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    /** Waktu persis kejadian transisi ini tercatat. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    /** Nomor ronde review saat kejadian terjadi, untuk item yang mengalami siklus revisi berulang (submit &rarr; kembalikan &rarr; ajukan ulang); {@code null} bila tidak dilacak. */
    @Column(name="round_number") public Integer getRoundNumber(){return roundNumber;}
    public void setRoundNumber(Integer v){roundNumber=v;}
}
