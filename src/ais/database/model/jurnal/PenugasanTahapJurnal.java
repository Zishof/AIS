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
 /** Id item/naskah jurnal yang menjadi objek penugasan ini. */
 @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 /** Identitas pengguna yang ditugaskan. */
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 /** Peran pengguna dalam penugasan ini (mis. "REVIEWER", "EDITOR"). */
 @Column(name="role_key",nullable=false,length=80) public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
 /** Tahap alur editorial tempat penugasan ini berlaku (mis. "REVIEW", "COPYEDIT"). */
 @Column(name="stage_key",nullable=false,length=80) public String getStageKey(){return stageKey;} public void setStageKey(String v){stageKey=v;}
 /** Bagian/rubrik jurnal terkait penugasan, bila relevan. */
 @Column(name="section_key",length=120) public String getSectionKey(){return sectionKey;} public void setSectionKey(String v){sectionKey=v;}
 /** Status penugasan (mis. "AKTIF", "SELESAI", "DIBATALKAN"). */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** Jejak asal-usul data penugasan (mis. metadata hasil impor) dalam format JSON. */
 @Column(name="provenance_json",columnDefinition="text") public String getProvenanceJson(){return provenanceJson;} public void setProvenanceJson(String v){provenanceJson=v;}
 /** Waktu mulai berlakunya penugasan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="starts_at",nullable=false) public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;}
 /** Waktu berakhirnya penugasan, bila sudah selesai. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="ends_at") public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;}
}
