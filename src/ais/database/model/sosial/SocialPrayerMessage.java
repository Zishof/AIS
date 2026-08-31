package ais.database.model.sosial;
import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.social_prayer_message} — pesan/doa yang
 * disertakan donatur pada satu transaksi donasi ({@link #getTransaction()}, relasi
 * satu-ke-satu), lazimnya ditampilkan publik di halaman kampanye/program setelah lolos
 * moderasi.
 *
 * <p>
 * {@link #getAnonymous()} memungkinkan donatur menyembunyikan identitas aslinya, dengan
 * {@link #getDisplayName()} sebagai nama tampilan alternatif. Pesan wajib melalui alur
 * moderasi ({@link #getModerationStatus()}, default {@code "PENDING"} bila belum diset)
 * sebelum tayang ({@link #getPublicAt()}); {@link #getReportsCount()} melacak jumlah laporan
 * konten tidak pantas dari pengguna lain untuk membantu keputusan moderasi. Diaudit penuh
 * oleh Hibernate Envers.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="social_prayer_message")
public class SocialPrayerMessage extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private String message,displayName,moderationStatus,moderatedBy; private Boolean anonymous; private Date publicAt; private Integer reportsCount;
 /** Transaksi donasi yang menyertakan pesan/doa ini (relasi satu-ke-satu). */
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false,unique=true) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 /** Isi pesan/doa dari donatur. */
 @Column(name="message",nullable=false,length=1000) public String getMessage(){return message;} public void setMessage(String v){message=v;}
 /** Nama tampilan donatur untuk pesan ini (dapat berbeda dari nama asli, terutama bila anonim). */
 @Column(name="display_name",length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=trim(v);}
 /** Status moderasi konten pesan (default {@code "PENDING"} bila belum diset). */
 @Column(name="moderation_status",nullable=false,length=40) public String getModerationStatus(){return moderationStatus==null?"PENDING":moderationStatus;} public void setModerationStatus(String v){moderationStatus=trim(v);}
 /** Identitas moderator yang memutuskan status moderasi pesan ini. */
 @Column(name="moderated_by",length=255) public String getModeratedBy(){return moderatedBy;} public void setModeratedBy(String v){moderatedBy=trim(v);}
 /** Menandai apakah donatur memilih tampil anonim (identitas asli disembunyikan dari publik). */
 @Column(name="anonymous") public Boolean getAnonymous(){return Boolean.TRUE.equals(anonymous);} public void setAnonymous(Boolean v){anonymous=v;}
 /** Waktu pesan ini ditayangkan/dipublikasikan ke publik. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="public_at") public Date getPublicAt(){return publicAt;} public void setPublicAt(Date v){publicAt=v;}
 /** Jumlah laporan konten tidak pantas yang diterima pesan ini (default 0 bila belum diset). */
 @Column(name="reports_count") public Integer getReportsCount(){return reportsCount==null?0:reportsCount;} public void setReportsCount(Integer v){reportsCount=v;}
}
