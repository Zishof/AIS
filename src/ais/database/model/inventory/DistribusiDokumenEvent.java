package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Jejak perubahan status (event log) satu {@link DistribusiDokumen} -- anak ketiga klaster
 * header-baris-event, mencatat satu baris per transisi status yang idealnya menyertai setiap
 * perubahan {@link DistribusiDokumen#getStatus()}. Lihat javadoc {@link DistribusiDokumen} untuk
 * konteks arsitektur klaster secara penuh, termasuk catatan bahwa klaster ini saat ini dormant
 * (tidak ada kode aplikasi yang benar-benar menulis baris event ini).
 *
 * <p><b>Ledger append-only tanpa penegakan urutan.</b> Kelas ini murni baris catatan pasif -- tidak
 * ada validasi di model bahwa {@link #getFromStatus()} baris baru benar-benar sama dengan {@link
 * #getToStatus()} baris event sebelumnya untuk dokumen yang sama (rantai transisi konsisten), atau
 * bahwa {@link #getToStatus()} baris terbaru benar-benar cocok dengan {@link
 * DistribusiDokumen#getStatus()} header saat ini. Menjaga konsistensi itu, bila diperlukan, adalah
 * tanggung jawab kode pemanggil yang menulis kedua baris (header dan event) dalam satu transaksi --
 * pola yang sama seperti {@code PenandaJurnalPenyesuaian} atau ledger append-only lain di codebase
 * ini, tapi TANPA constraint unik atau mekanisme guard apa pun di level tabel ini yang menegakkannya.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "distribution_document_event")
public class DistribusiDokumenEvent implements Serializable {

	private static final long serialVersionUID = 1L;

	/** Primary key baris event ini. Digenerasi database ({@code IDENTITY}); {@code null} sebelum baris pertama kali disimpan. */
	private Long id;
	/** Id {@link DistribusiDokumen} yang statusnya berubah -- kolom {@code Long} polos tanpa relasi {@code @ManyToOne}. */
	private Long documentId;
	/** Status sebelum transisi ini, opsional (mis. {@code null} untuk event pertama saat dokumen baru dibuat). */
	private String fromStatus;
	/** Status sesudah transisi ini, wajib diisi. */
	private String toStatus;
	/** Catatan bebas teks untuk transisi ini (mis. alasan perubahan status), opsional. */
	private String notes;
	/** Userid/nama pelaku transisi status ini, opsional, tidak ber-FK. */
	private String actorId;
	/** Waktu transisi ini terjadi/dicatat, diinisialisasi ke waktu konstruksi objek Java. */
	private Date eventAt = new Date();

	/**
	 * Primary key baris event ini. Digenerasi database via strategi {@code IDENTITY}; {@code null}
	 * pada objek yang belum pernah di-{@code save}.
	 * @return id baris, atau {@code null} jika belum tersimpan.
	 */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }

	/**
	 * Id {@link DistribusiDokumen} yang mengalami transisi status ini, wajib diisi. Kolom {@code
	 * Long} polos tanpa relasi {@code @ManyToOne} -- memuat header dokumen terkait memerlukan query
	 * terpisah oleh kode pemanggil.
	 * @return id dokumen terkait.
	 */
	@Column(name = "document_id", nullable = false) public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { documentId = value; }

	/**
	 * Status dokumen SEBELUM transisi ini, opsional -- bisa {@code null} untuk merepresentasikan
	 * event pertama pada dokumen yang baru dibuat (tidak ada status "sebelumnya"). Tidak divalidasi
	 * di model ini terhadap {@link #getToStatus()} event sebelumnya untuk dokumen yang sama.
	 * @return status sebelum transisi, atau {@code null} bila ini event pertama/tidak diisi.
	 */
	@Column(name = "from_status", length = 30) public String getFromStatus() { return fromStatus; } public void setFromStatus(String value) { fromStatus = value; }

	/**
	 * Status dokumen SESUDAH transisi ini, wajib diisi. Idealnya sama dengan {@link
	 * DistribusiDokumen#getStatus()} header pada saat baris ini ditulis, tapi tidak ada mekanisme di
	 * model ini yang menegakkan kecocokan tersebut.
	 * @return status sesudah transisi.
	 */
	@Column(name = "to_status", nullable = false, length = 30) public String getToStatus() { return toStatus; } public void setToStatus(String value) { toStatus = value; }

	/** @return catatan bebas teks transisi ini, atau {@code null} bila tidak diisi. */
	@Column(name = "notes", columnDefinition = "text") public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }

	/** @return userid/nama pelaku transisi status ini, atau {@code null} bila tidak diisi. */
	@Column(name = "actor_id", length = 100) public String getActorId() { return actorId; } public void setActorId(String value) { actorId = value; }

	/**
	 * Waktu transisi status ini terjadi/dicatat, wajib diisi. Diinisialisasi ke waktu konstruksi
	 * objek Java (bukan waktu commit transaksi) -- nilai default bisa ditimpa manual oleh setter
	 * sebelum baris disimpan.
	 * @return waktu transisi.
	 */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "event_at", nullable = false)
	public Date getEventAt() { return eventAt; } public void setEventAt(Date value) { eventAt = value; }
}
