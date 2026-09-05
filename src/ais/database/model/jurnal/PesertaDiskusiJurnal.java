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
 * <p>
 * <b>Relasi dengan {@code ais.database.model.Diskusi} (dikonfirmasi saat dokumentasi
 * ditulis):</b> entity ini BUKAN duplikat/pengganti {@code Diskusi}/{@code DiskusiKomentar} —
 * ia adalah tabel keanggotaan TAMBAHAN yang dipasangkan dengan mekanisme diskusi/komentar yang
 * SUDAH ADA sebelumnya di {@code ais.database.model}. {@code ais.action.master.jurnal.JurnalDiscussionService}
 * memakai {@code Diskusi} sebagai thread dan {@code DiskusiKomentar} sebagai isi komentar apa
 * adanya (tidak dimodelkan ulang di paket {@code jurnal}), sementara {@code PesertaDiskusiJurnal}
 * hanya menambahkan daftar siapa saja peserta thread tersebut beserta perannya — dibutuhkan
 * karena {@code Diskusi} lama tidak punya tabel keanggotaan eksplisit bergaya OJS
 * ({@code query_participants}).
 * </p>
 * <p>
 * <b>Evaluasi keamanan (dicek saat dokumentasi ditulis) — kebocoran identitas peserta:</b>
 * {@code Diskusi} memiliki kolom {@code anonymityMode} (lihat
 * {@code JurnalDiscussionService#create}, divalidasi sebagai {@code DOUBLE_ANONYMOUS}/
 * {@code SINGLE_ANONYMOUS}/{@code OPEN}) dan {@code visibility} ({@code INTERNAL}/
 * {@code REVIEWERS}/{@code AUTHOR_EDITOR}/{@code ALL_PARTICIPANTS}), tetapi
 * {@code JurnalDiscussionService} TIDAK PERNAH membaca kembali nilai {@code anonymityMode}
 * saat menyimpan komentar ({@code DiskusiKomentar.olehId} selalu diisi identitas asli pengirim)
 * maupun saat menambah peserta (baris ini selalu berisi {@link #getUserId()} identitas asli).
 * Ini MEMPERKUAT pola yang sudah tercatat sebelumnya pada {@code Diskusi.java} (kebijakan
 * anonimitas disimpan tapi tidak pernah ditegakkan) — bukan temuan independen baru. Gerbang
 * akses yang ADA di {@code JurnalDiscussionService#comment} (memeriksa keanggotaan lewat baris
 * ini, atau izin {@code prosesReview}) mencegah PIHAK LUAR ikut berkomentar, tapi tidak
 * mencegah PESERTA YANG SUDAH TERDAFTAR saling mengetahui identitas satu sama lain sesuai
 * {@code visibility}/{@code anonymityMode} — penegakan visibilitas berbutir-halus semacam itu
 * harus dilakukan di lapisan yang menyusun tampilan daftar peserta/komentar, bukan diasumsikan
 * terjadi di sini.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="peserta_diskusi_jurnal")
public class PesertaDiskusiJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long diskusiId; private String userId,participantRole; private Date joinedAt,leftAt;
 /** Id thread diskusi editorial ({@code ais.database.model.Diskusi}, entity lama yang digunakan ulang) yang diikuti; kolom FK longgar, bukan relasi Hibernate. */
 @Column(name="diskusi_id",nullable=false) public Long getDiskusiId(){return diskusiId;} public void setDiskusiId(Long v){diskusiId=v;}
 /**
  * Identitas pengguna peserta diskusi. Nilai ini selalu identitas asli (tidak pernah
  * disamarkan/di-hash) terlepas dari mode anonimitas thread diskusi induknya — lihat catatan
  * keamanan pada Javadoc kelas.
  */
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 /** Peran peserta dalam diskusi ini (mis. "AUTHOR", "EDITOR", "SECTION_EDITOR", "REVIEWER", "COPYEDITOR", "PRODUCTION", "PROOFREADER"). */
 @Column(name="participant_role",nullable=false,length=60) public String getParticipantRole(){return participantRole;} public void setParticipantRole(String v){participantRole=v;}
 /**
  * Waktu peserta bergabung ke diskusi. Bila peserta yang sebelumnya keluar ({@link #getLeftAt()}
  * terisi) diundang kembali, {@code JurnalDiscussionService#addParticipant} menggunakan ulang
  * baris yang sama (mengosongkan {@code leftAt} dan memperbarui peran) alih-alih membuat baris
  * baru — {@link #getJoinedAt()} pada kasus ini TIDAK diperbarui ke waktu bergabung-kembali,
  * tetap menyimpan waktu bergabung pertama kali.
  */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="joined_at",nullable=false) public Date getJoinedAt(){return joinedAt;} public void setJoinedAt(Date v){joinedAt=v;}
 /** Waktu peserta keluar/berhenti mengikuti diskusi, bila sudah tidak aktif; kosong bila peserta masih aktif mengikuti thread. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="left_at") public Date getLeftAt(){return leftAt;} public void setLeftAt(Date v){leftAt=v;}
}
