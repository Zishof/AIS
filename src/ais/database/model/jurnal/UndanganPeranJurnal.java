package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="undangan_peran_jurnal")
public class UndanganPeranJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String email,roleKey,scopeType,scopeKey,tokenHash,status,invitedUserId; private Date expiresAt,acceptedAt,declinedAt,revokedAt;
 @Column(name="email",nullable=false,length=320) public String getEmail(){return email;} public void setEmail(String v){email=v;}
 @Column(name="role_key",nullable=false,length=80) public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
 @Column(name="scope_type",nullable=false,length=40) public String getScopeType(){return scopeType;} public void setScopeType(String v){scopeType=v;}
 @Column(name="scope_key",length=255) public String getScopeKey(){return scopeKey;} public void setScopeKey(String v){scopeKey=v;}
 @Column(name="token_hash",nullable=false,length=128) public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 @Column(name="invited_user_id",length=255) public String getInvitedUserId(){return invitedUserId;} public void setInvitedUserId(String v){invitedUserId=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expires_at",nullable=false) public Date getExpiresAt(){return expiresAt;} public void setExpiresAt(Date v){expiresAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="accepted_at") public Date getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(Date v){acceptedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="declined_at") public Date getDeclinedAt(){return declinedAt;} public void setDeclinedAt(Date v){declinedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="revoked_at") public Date getRevokedAt(){return revokedAt;} public void setRevokedAt(Date v){revokedAt=v;}
}
