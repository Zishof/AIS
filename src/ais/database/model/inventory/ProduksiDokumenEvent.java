package ais.database.model.inventory;
import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable; import java.util.Date;
import javax.persistence.Column; import javax.persistence.Entity; import javax.persistence.GeneratedValue; import javax.persistence.Id; import javax.persistence.Table; import javax.persistence.Temporal; import javax.persistence.TemporalType;
/**
 * Entitas Hibernate untuk tabel {@code koperasi.production_document_event} — baris log
 * peristiwa perpindahan status pada satu dokumen produksi (mis. dokumen produksi/cetak di
 * modul koperasi), dicatat setiap kali status dokumen berubah dari {@link #getFromStatus()}
 * ke {@link #getToStatus()}.
 *
 * <p>
 * Entitas ini bersifat append-only (riwayat/audit trail): tidak ada relasi terpetakan
 * (annotated) ke entitas dokumen induknya, hanya disimpan sebagai id mentah lewat
 * {@link #getDocumentId()}, sehingga join ke dokumen asal harus dilakukan manual oleh
 * pemanggil.
 * </p>
 */
@Entity @Table(schema="koperasi",name="production_document_event")
public class ProduksiDokumenEvent implements Serializable {
	private static final long serialVersionUID=1L; private Long id; private Long documentId; private String fromStatus; private String toStatus; private String notes; private String actorId; private Date eventAt=new Date();
	@Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",unique=true,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
	/** Id dokumen produksi yang mengalami perubahan status (referensi mentah, tanpa relasi terpetakan). */
	@Column(name="document_id",nullable=false) public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
	/** Status dokumen sebelum perubahan; {@code null} untuk peristiwa pertama (dokumen baru dibuat). */
	@Column(name="from_status",length=30) public String getFromStatus(){return fromStatus;} public void setFromStatus(String v){fromStatus=v;}
	/** Status dokumen setelah perubahan (status hasil dari peristiwa ini). */
	@Column(name="to_status",nullable=false,length=30) public String getToStatus(){return toStatus;} public void setToStatus(String v){toStatus=v;}
	/** Catatan bebas terkait peristiwa perubahan status (mis. alasan penolakan/revisi). */
	@Column(name="notes",columnDefinition="text") public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
	/** Identitas pelaku (user/aktor) yang memicu perubahan status ini. */
	@Column(name="actor_id",length=100) public String getActorId(){return actorId;} public void setActorId(String v){actorId=v;}
	/** Waktu terjadinya perubahan status; default saat objek dibuat adalah waktu konstruksi. */
	@Temporal(TemporalType.TIMESTAMP) @Column(name="event_at",nullable=false) public Date getEventAt(){return eventAt;} public void setEventAt(Date v){eventAt=v;}
}
