package ais.database.model.sosial;
import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
/**
 * Entitas Hibernate untuk tabel {@code public.bukti_setor_sosial} — bukti setor/kuitansi
 * resmi (mis. PDF) yang diterbitkan untuk satu transaksi donasi ({@link #getTransaction()}),
 * relasi satu-ke-satu ({@code @OneToOne}, unik per transaksi).
 *
 * <p>
 * Nomor kuitansi ({@link #getReceiptNumber()}) unik per tenant (lihat
 * {@code @UniqueConstraint} pada {@code tenant_key + receipt_number}), dan
 * {@link #getVerificationToken()} unik global — dipakai untuk verifikasi keaslian bukti setor
 * secara publik (mis. lewat tautan/QR code tanpa perlu login). Berkas hasil cetak disimpan di
 * luar basis data dan dirujuk lewat {@link #getPdfPath()}. Bila bukti setor perlu diganti
 * (mis. koreksi data), bukti lama ditandai {@link #getVoided()} dan
 * {@link #getReplacementReference()} menunjuk ke bukti penggantinya. Diaudit penuh oleh
 * Hibernate Envers.
 * </p>
 */
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="bukti_setor_sosial",uniqueConstraints={@UniqueConstraint(columnNames={"tenant_key","receipt_number"}),@UniqueConstraint(columnNames={"verification_token"})})
public class BuktiSetorSosial extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private String receiptNumber,receiptType,templateVersion,verificationToken,pdfPath,deliveryStatus,replacementReference; private Date generatedAt; private Boolean voided;
 /** Transaksi donasi yang menjadi dasar penerbitan bukti setor ini (relasi satu-ke-satu). */
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false,unique=true) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 /** Nomor kuitansi/bukti setor, unik per tenant. */
 @Column(name="receipt_number",nullable=false,length=120) public String getReceiptNumber(){return receiptNumber;} public void setReceiptNumber(String v){receiptNumber=trim(v);}
 /** Jenis/kategori bukti setor (mis. kuitansi zakat vs sedekah), bila dibedakan. */
 @Column(name="receipt_type",length=60) public String getReceiptType(){return receiptType;} public void setReceiptType(String v){receiptType=trim(v);}
 /** Versi template cetak yang dipakai untuk menghasilkan dokumen ini. */
 @Column(name="template_version",length=40) public String getTemplateVersion(){return templateVersion;} public void setTemplateVersion(String v){templateVersion=trim(v);}
 /** Token unik untuk verifikasi keaslian bukti setor secara publik (mis. lewat QR code). */
 @Column(name="verification_token",nullable=false,length=120) public String getVerificationToken(){return verificationToken;} public void setVerificationToken(String v){verificationToken=trim(v);}
 /** Lokasi/path berkas PDF hasil cetak bukti setor. */
 @Column(name="pdf_path",length=1000) public String getPdfPath(){return pdfPath;} public void setPdfPath(String v){pdfPath=trim(v);}
 /** Status pengiriman bukti setor ke donatur (mis. "TERKIRIM", "GAGAL"). */
 @Column(name="delivery_status",length=40) public String getDeliveryStatus(){return deliveryStatus;} public void setDeliveryStatus(String v){deliveryStatus=trim(v);}
 /** Referensi ke bukti setor pengganti, bila bukti ini sudah dibatalkan/digantikan. */
 @Column(name="replacement_reference",length=120) public String getReplacementReference(){return replacementReference;} public void setReplacementReference(String v){replacementReference=trim(v);}
 /** Waktu bukti setor ini diterbitkan/dicetak. */
 @Temporal(TemporalType.TIMESTAMP) @Column(name="generated_at",nullable=false) public Date getGeneratedAt(){return generatedAt;} public void setGeneratedAt(Date v){generatedAt=v;}
 /** Menandai apakah bukti setor ini sudah dibatalkan (voided), mis. karena digantikan versi baru. */
 @Column(name="voided") public Boolean getVoided(){return Boolean.TRUE.equals(voided);} public void setVoided(Boolean v){voided=v;}
}
