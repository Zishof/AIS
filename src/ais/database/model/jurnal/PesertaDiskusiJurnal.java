package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.peserta_diskusi_jurnal} —
 * keanggotaan seorang pengguna ({@link #getUserId()}) dalam satu thread diskusi editorial
 * jurnal ({@link #getDiskusiId()}, mis. diskusi review/editorial bergaya OJS), lengkap dengan
 * peran keikutsertaannya ({@link #getParticipantRole()}) dan rentang waktu partisipasi
 * ({@link #getJoinedAt()}/{@link #getLeftAt()}).
 *
 * <p>
 * Tidak ada relasi Hibernate terpetakan ke entitas diskusi induk; tautan memakai id mentah
 * {@link #getDiskusiId()}.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="peserta_diskusi_jurnal")
public class PesertaDiskusiJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long diskusiId; private String userId,participantRole; private Date joinedAt,leftAt;
 /** Id thread diskusi editorial yang diikuti. */
 @Column(name="diskusi_id",nullable=false) public Long getDiskusiId(){return diskusiId;} public void setDiskusiId(Long v){diskusiId=v;}
 /** Identitas pengguna peserta diskusi. */
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 /** Peran peserta dalam diskusi ini (mis. "PENULIS", "REVIEWER", "EDITOR"). */
 @Column(name="participant_role",nullable=false,length=60) public String getParticipantRole(){return participantRole;} public void setParticipantRole(String v){participantRole=v;}
 /** Waktu peserta bergabung ke diskusi. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="joined_at",nullable=false) public Date getJoinedAt(){return joinedAt;} public void setJoinedAt(Date v){joinedAt=v;}
 /** Waktu peserta keluar/berhenti mengikuti diskusi, bila sudah tidak aktif. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="left_at") public Date getLeftAt(){return leftAt;} public void setLeftAt(Date v){leftAt=v;}
}
