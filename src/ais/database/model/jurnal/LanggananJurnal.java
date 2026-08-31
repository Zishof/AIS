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
 @Column(name="collection_id",nullable=false) public Long getCollectionId(){return collectionId;} public void setCollectionId(Long v){collectionId=v;}
 @Column(name="policy_key",nullable=false,length=120) public String getPolicyKey(){return policyKey;} public void setPolicyKey(String v){policyKey=v;}
 @Column(name="policy_snapshot_json",nullable=false,columnDefinition="text") public String getPolicySnapshotJson(){return policySnapshotJson;} public void setPolicySnapshotJson(String v){policySnapshotJson=v;}
 @Column(name="user_id",length=255) public String getUserId(){return userId;} public void setUserId(String v){userId=v;}
 @Column(name="institution_type",length=80) public String getInstitutionType(){return institutionType;} public void setInstitutionType(String v){institutionType=v;}
 @Column(name="institution_id") public Long getInstitutionId(){return institutionId;} public void setInstitutionId(Long v){institutionId=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="starts_at",nullable=false) public Date getStartsAt(){return startsAt;} public void setStartsAt(Date v){startsAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="ends_at",nullable=false) public Date getEndsAt(){return endsAt;} public void setEndsAt(Date v){endsAt=v;}
 @Column(name="status",nullable=false,length=40) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 @Column(name="payment_id") public Long getPaymentId(){return paymentId;} public void setPaymentId(Long v){paymentId=v;}
 @Column(name="external_reference",length=255) public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
}
