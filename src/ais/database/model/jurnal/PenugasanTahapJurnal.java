package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="penugasan_tahap_jurnal")
public class PenugasanTahapJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long itemId; private String userId,roleKey,stageKey,sectionKey,status,provenanceJson; private Date startsAt,endsAt;
 @Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 @Column(name="role_key",nullable=false,length=80) public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
 @Column(name="stage_key",nullable=false,length=80) public String getStageKey(){return stageKey;} public void setStageKey(String v){stageKey=v;}
 @Column(name="section_key",length=120) public String getSectionKey(){return sectionKey;} public void setSectionKey(String v){sectionKey=v;}
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 @Column(name="provenance_json",columnDefinition="text") public String getProvenanceJson(){return provenanceJson;} public void setProvenanceJson(String v){provenanceJson=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="starts_at",nullable=false) public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="ends_at") public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;}
}
