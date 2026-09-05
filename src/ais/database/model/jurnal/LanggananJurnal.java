package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.langganan_jurnal}, merepresentasikan
 * satu langganan akses berbayar terhadap koleksi/kumpulan konten jurnal ilmiah
 * ({@link #getCollectionId()} — id koleksi, dirujuk sebagai kolom FK biasa, bukan relasi
 * {@code @ManyToOne} eksplisit) pada sistem jurnal OJS-style di AIS. Langganan dapat dimiliki
 * oleh individu ({@link #getUserId()}) atau oleh institusi ({@link #getInstitutionId()} beserta
 * {@link #getInstitutionType()}, mis. perguruan tinggi/perpustakaan berlangganan untuk seluruh
 * anggotanya) — kedua jenis kepemilikan bersifat opsional/salah satu.
 * <p>
 * {@link #getPolicyKey()} merujuk kebijakan/paket langganan yang berlaku, dengan
 * {@link #getPolicySnapshotJson()} menyimpan salinan (snapshot) aturan kebijakan tersebut dalam
 * JSON pada saat langganan dibuat — sehingga perubahan kebijakan di kemudian hari tidak mengubah
 * hak akses langganan yang sudah berjalan. Masa berlaku langganan ditentukan oleh
 * {@link #getStartsAt()}/{@link #getEndsAt()}, dan {@link #getPaymentId()} menghubungkan langganan
 * ke transaksi pembayaran terkait (id pembayaran, kolom FK biasa), dengan
 * {@link #getExternalReference()} untuk referensi tambahan ke sistem eksternal (mis. payment
 * gateway atau agregator langganan pihak ketiga).
 * <p>
 * Kolom teknis bersama (id, tenant, audit, versi optimistic-locking) diwariskan dari
 * {@link JurnalEntityBase}.
 */
@Entity @Table(schema="penelitiandanpengabdian",name="langganan_jurnal")
public class LanggananJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long collectionId,institutionId,paymentId; private String policyKey,policySnapshotJson,userId,institutionType,status,externalReference; private Date startsAt,endsAt;
 /** Id koleksi repository ({@code RepoCollection} bertipe {@code JOURNAL}) yang menjadi objek langganan ini; kolom FK biasa, bukan relasi Hibernate. */
 @Column(name="collection_id",nullable=false) public Long getCollectionId(){return collectionId;} public void setCollectionId(Long v){collectionId=v;}
 /** Kunci kebijakan/paket langganan yang dipilih saat aktivasi (mis. "INSTITUTION_ANNUAL"), dirujuk ke daftar policy pada {@code RepoCollection.accessPolicyJson}. */
 @Column(name="policy_key",nullable=false,length=120) public String getPolicyKey(){return policyKey;} public void setPolicyKey(String v){policyKey=v;}
 /** Salinan (snapshot) JSON aturan kebijakan langganan pada saat dibuat, agar perubahan kebijakan koleksi setelahnya tidak mengubah hak akses langganan yang sudah berjalan. */
 @Column(name="policy_snapshot_json",nullable=false,columnDefinition="text") public String getPolicySnapshotJson(){return policySnapshotJson;} public void setPolicySnapshotJson(String v){policySnapshotJson=v;}
 /** Identitas pengguna pemilik langganan bila langganan bertipe individu; kosong bila langganan bertipe institusi (lihat {@link #getInstitutionId()}). */
 @Column(name="user_id",length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 /** Jenis institusi pemilik langganan (mis. perguruan tinggi/perpustakaan) bila langganan bertipe institusi; kosong bila langganan bertipe individu. */
 @Column(name="institution_type",length=80) public String getInstitutionType(){return institutionType;} public void setInstitutionType(String v){institutionType=v;}
 /** Id institusi pemilik langganan bila langganan bertipe institusi; kolom FK longgar, opsional. */
 @Column(name="institution_id") public Long getInstitutionId(){return institutionId;} public void setInstitutionId(Long v){institutionId=v;}
 /** Waktu mulai berlakunya masa langganan. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="starts_at",nullable=false) public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;}
 /** Waktu berakhirnya masa langganan; akses dievaluasi hanya selama waktu kini berada di antara {@link #getStartsAt()} dan nilai ini. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="ends_at",nullable=false) public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;}
 /** Status siklus hidup langganan (mis. "ACTIVE", "PENDING_PAYMENT"). */
 @Column(name="status",nullable=false,length=40) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** Id transaksi pembayaran yang melunasi langganan ini, bila kebijakan mensyaratkan pembayaran; kosong bila kebijakan gratis atau pembayaran belum dilakukan. */
 @Column(name="payment_id") public Long getPaymentId(){return paymentId;} public void setPaymentId(Long v){paymentId=v;}
 /** Referensi tambahan ke sistem eksternal terkait langganan ini (mis. id transaksi payment gateway atau agregator langganan pihak ketiga). */
 @Column(name="external_reference",length=255) public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
}
