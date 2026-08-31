package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.undangan_peran_jurnal},
 * merepresentasikan satu undangan bagi seseorang (via {@link #getEmail()}) untuk mengemban
 * suatu peran ({@link #getRoleKey()}, mis. editor/reviewer/penulis) pada modul manajemen
 * jurnal (bagian dari domain penelitian dan pengabdian). Cakupan peran yang diundangkan
 * ditentukan oleh pasangan {@link #getScopeType()}/{@link #getScopeKey()} (mis. cakupan satu
 * jurnal tertentu lewat {@code jurnalPenelitianId} yang diwarisi dari
 * {@link JurnalEntityBase}, atau cakupan lain sesuai nilai {@code scopeType}).
 * <p>
 * Undangan diverifikasi lewat token sekali pakai yang hanya disimpan dalam bentuk hash
 * ({@link #getTokenHash()}, bukan token mentah — token asli dikirim ke penerima lewat kanal
 * lain, mis. email) dan memiliki masa berlaku ({@link #getExpiresAt()}). Siklus hidup undangan
 * dilacak lewat {@link #getStatus()} beserta stempel waktu tindakan penerima:
 * {@link #getAcceptedAt()} (diterima, dengan {@link #getInvitedUserId()} diisi bila penerima
 * lalu terhubung ke akun pengguna AIS), {@link #getDeclinedAt()} (ditolak), atau
 * {@link #getRevokedAt()} (dicabut oleh pengundang sebelum direspons).
 */
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
