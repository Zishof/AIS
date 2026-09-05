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
 /** Alamat email penerima undangan; kanal pengiriman token undangan (mis. lewat {@code TemplateEmailJurnal}). */
 @Column(name="email",nullable=false,length=320) public String getEmail(){return email;} public void setEmail(String v){email=v;}
 /** Peran yang diundangkan kepada penerima (mis. "EDITOR", "REVIEWER", "AUTHOR"). */
 @Column(name="role_key",nullable=false,length=80) public String getRoleKey(){return roleKey;} public void setRoleKey(String v){roleKey=v;}
 /** Jenis cakupan peran yang diundangkan (mis. cakupan satu jurnal, satu bagian/rubrik, atau cakupan lain sesuai konvensi pemanggil). */
 @Column(name="scope_type",nullable=false,length=40) public String getScopeType(){return scopeType;} public void setScopeType(String v){scopeType=v;}
 /** Kunci cakupan spesifik sesuai {@link #getScopeType()} (mis. id jurnal/bagian dalam bentuk teks); opsional, tergantung konvensi cakupan yang dipakai. */
 @Column(name="scope_key",length=255) public String getScopeKey(){return scopeKey;} public void setScopeKey(String v){scopeKey=v;}
 /**
  * Hash (bukan nilai mentah) dari token verifikasi sekali pakai yang dikirim ke penerima lewat
  * kanal lain (mis. tautan di email ke {@link #getEmail()}). Menyimpan hash alih-alih token
  * mentah mencegah kebocoran token bila baris tabel ini terekspos (mis. lewat dump/backup/report),
  * karena pemanggil harus membandingkan hash dari token yang diberikan penerima, bukan
  * membaca token langsung dari baris ini.
  */
 @Column(name="token_hash",nullable=false,length=128) public String getTokenHash(){return tokenHash;} public void setTokenHash(String v){tokenHash=v;}
 /** Status siklus hidup undangan (mis. "PENDING", "ACCEPTED", "DECLINED", "REVOKED", "EXPIRED"). */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** Id pengguna AIS yang terhubung ke undangan ini setelah diterima, bila penerima berhasil menautkan/membuat akun; kosong sebelum diterima. */
 @Column(name="invited_user_id",length=255) public String getInvitedUserId(){return invitedUserId;} public void setInvitedUserId(String v){invitedUserId=v;}
 /** Waktu kedaluwarsa token undangan; setelah waktu ini token tidak lagi berlaku meski belum direspons. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="expires_at",nullable=false) public Date getExpiresAt(){return expiresAt;} public void setExpiresAt(Date v){expiresAt=v;}
 /** Waktu penerima menerima undangan; kosong bila belum direspons atau direspons dengan penolakan/pencabutan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="accepted_at") public Date getAcceptedAt(){return acceptedAt;} public void setAcceptedAt(Date v){acceptedAt=v;}
 /** Waktu penerima menolak undangan; kosong bila belum direspons atau direspons dengan penerimaan/pencabutan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="declined_at") public Date getDeclinedAt(){return declinedAt;} public void setDeclinedAt(Date v){declinedAt=v;}
 /** Waktu undangan dicabut oleh pengundang sebelum sempat direspons penerima; kosong bila undangan masih berjalan atau sudah direspons. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="revoked_at") public Date getRevokedAt(){return revokedAt;} public void setRevokedAt(Date v){revokedAt=v;}
}
