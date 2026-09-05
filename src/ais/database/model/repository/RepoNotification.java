package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.repo_notification} —
 * notifikasi dalam-aplikasi (in-app) untuk pengguna repositori institusional;
 * pengiriman surel eksternal tetap berupa adapter opsional di luar entitas
 * ini (baris ini hanya merepresentasikan status "sudah dibuat"/"sudah
 * dibaca" di dalam aplikasi, bukan status pengiriman surel).
 *
 * <p>
 * Sumber paling umum baris notifikasi ini adalah pencocokan berkala
 * {@link RepoUserPreference} (saved search/alert langganan) terhadap item
 * baru — lihat {@link #getPreferenceId()} — meski notifikasi juga bisa
 * dibuat langsung oleh alur kerja submission/review (mis. pemberitahuan ke
 * reviewer/depositor).
 * </p>
 */
@Entity @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_notification")
public class RepoNotification implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId,preferenceId; private String recipientId,recipientRole,type,message; private Date readAt,createdAt;
    /** Id baris notifikasi ini (identity, auto-generated). */
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    /** Id {@link RepoItem} yang menjadi subjek notifikasi ini (mis. item baru yang cocok dengan saved search, atau item yang berubah status). */
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    /** Id {@link RepoUserPreference} (saved search/alert) yang memicu notifikasi ini, bila notifikasi berasal dari pencocokan langganan; {@code null} bila dipicu sumber lain (mis. alur kerja). */
    @Column(name="preference_id") public Long getPreferenceId(){return preferenceId;} public void setPreferenceId(Long v){preferenceId=v;}
    /** Id pengguna penerima notifikasi. */
    @Column(name="recipient_id",length=255) public String getRecipientId(){return recipientId;} public void setRecipientId(String v){recipientId=v;}
    /** Peran penerima pada saat notifikasi dibuat (mis. {@code "DEPOSITOR"}, {@code "REVIEWER"}) — field audit shadow untuk konteks tampilan, bukan sumber kebenaran hak akses saat ini. */
    @Column(name="recipient_role",length=60) public String getRecipientRole(){return recipientRole;} public void setRecipientRole(String v){recipientRole=v;}
    /** Jenis notifikasi, mis. {@code "NEW_MATCH"} (item baru cocok saved search), {@code "STATUS_CHANGE"}, dst. */
    @Column(name="type",nullable=false,length=40) public String getType(){return type;} public void setType(String v){type=v;}
    /** Isi pesan notifikasi yang ditampilkan ke pengguna. */
    @Column(name="message",nullable=false,length=1000) public String getMessage(){return message;} public void setMessage(String v){message=v;}
    /** Waktu penerima membaca/membuka notifikasi ini; {@code null} selama masih belum dibaca (dasar hitung notifikasi belum-dibaca). */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="read_at") public Date getReadAt(){return readAt;} public void setReadAt(Date v){readAt=v;}
    /** Waktu notifikasi ini dibuat. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
