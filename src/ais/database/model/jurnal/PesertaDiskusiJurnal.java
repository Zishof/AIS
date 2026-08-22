package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="peserta_diskusi_jurnal")
public class PesertaDiskusiJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long diskusiId; private String userId,participantRole; private Date joinedAt,leftAt;
 @Column(name="diskusi_id",nullable=false) public Long getDiskusiId(){return diskusiId;} public void setDiskusiId(Long v){diskusiId=v;}
 @Column(name="user_id",nullable=false,length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 @Column(name="participant_role",nullable=false,length=60) public String getParticipantRole(){return participantRole;} public void setParticipantRole(String v){participantRole=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="joined_at",nullable=false) public Date getJoinedAt(){return joinedAt;} public void setJoinedAt(Date v){joinedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="left_at") public Date getLeftAt(){return leftAt;} public void setLeftAt(Date v){leftAt=v;}
}
