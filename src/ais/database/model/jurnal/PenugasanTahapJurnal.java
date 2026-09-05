package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.penugasan_tahap_jurnal} —
 * penugasan seorang pengguna ({@link #getUserId()}) pada satu tahap alur editorial jurnal
 * (mis. reviewer, editor bagian, copyeditor) untuk satu item/naskah ({@link #getItemId()}),
 * lengkap dengan peran ({@link #getRoleKey()}), tahap ({@link #getStageKey()}), dan rentang
 * waktu penugasan aktif ({@link #getStartsAt()}/{@link #getEndsAt()}).
 *
 * <p>
 * Merepresentasikan pola penugasan bergaya OJS (Open Journal Systems): satu naskah dapat
 * memiliki banyak baris penugasan seiring berjalannya tahap review/editorial.
 * {@link #getSectionKey()} mengacu ke bagian/rubrik jurnal bila penugasan spesifik ke
 * bagian tertentu. {@link #getProvenanceJson()} menyimpan jejak asal-usul data penugasan
 * (mis. hasil impor dari OJS) dalam format JSON. Tidak ada relasi Hibernate terpetakan ke
 * entitas item/naskah; tautan memakai id mentah.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="penugasan_tahap_jurnal")
public class PenugasanTahapJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long itemId; private String userId,roleKey,stageKey,sectionKey,status,provenanceJson; private Date startsAt,endsAt;
 /**
  * Id item/naskah jurnal yang menjadi objek penugasan ini; kolom FK biasa, opsional (mis.
  * penugasan level-jurnal seperti editor pengelola yang tidak terikat satu naskah tertentu
  * dapat mengosongkan kolom ini, bergantung konvensi pemanggil).
  */
 @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 /** Identitas pengguna yang ditugaskan pada tahap editorial ini. */
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 /** Peran pengguna dalam penugasan ini (mis. "REVIEWER", "EDITOR", "COPYEDITOR"). */
 @Column(name="role_key",nullable=false,length=80) public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
 /**
  * Tahap alur editorial tempat penugasan ini berlaku (mis. "REVIEW", "COPYEDIT"). Berbeda dari
  * {@link PenugasanReviewerJurnal} yang khusus memodelkan penugasan reviewer per ronde review
  * dengan siklus undang/terima/selesai, entity ini mencakup penugasan pada TAHAP ALUR mana pun
  * (bukan hanya review) tanpa siklus undangan formal — cocok untuk pola OJS
  * {@code stage_assignments}/{@code user_group}.
  */
 @Column(name="stage_key",nullable=false,length=80) public String getStageKey(){return stageKey;} public void setStageKey(String v){stageKey=v;}
 /** Bagian/rubrik jurnal terkait penugasan (mis. penugasan editor bagian tertentu), bila relevan; kosong bila penugasan berlaku lintas bagian. */
 @Column(name="section_key",length=120) public String getSectionKey(){return sectionKey;} public void setSectionKey(String v){sectionKey=v;}
 /** Status penugasan (mis. "AKTIF", "SELESAI", "DIBATALKAN"). */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /**
  * Jejak asal-usul data penugasan (mis. metadata hasil impor dari OJS lewat
  * {@code OjsDomainTransformService}, termasuk id baris sumber dan tabel OJS asal) dalam format
  * JSON, untuk ketertelusuran data hasil migrasi.
  */
 @Column(name="provenance_json",columnDefinition="text") public String getProvenanceJson(){return provenanceJson;} public void setProvenanceJson(String v){provenanceJson=v;}
 /** Waktu mulai berlakunya penugasan pada tahap ini. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="starts_at",nullable=false) public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;}
 /** Waktu berakhirnya penugasan, bila sudah selesai/dicabut; kosong selama penugasan masih berjalan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="ends_at") public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;}
}
