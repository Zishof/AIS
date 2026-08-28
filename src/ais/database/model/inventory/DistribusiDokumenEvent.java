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

/** Jejak perubahan status dokumen distribusi. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "inventory_distribution", name = "distribution_document_event")
public class DistribusiDokumenEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; private Long documentId; private String fromStatus; private String toStatus;
	private String notes; private String actorId; private Date eventAt = new Date();
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	@Column(name = "document_id", nullable = false) public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { documentId = value; }
	@Column(name = "from_status", length = 30) public String getFromStatus() { return fromStatus; } public void setFromStatus(String value) { fromStatus = value; }
	@Column(name = "to_status", nullable = false, length = 30) public String getToStatus() { return toStatus; } public void setToStatus(String value) { toStatus = value; }
	@Column(name = "notes", columnDefinition = "text") public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
	@Column(name = "actor_id", length = 100) public String getActorId() { return actorId; } public void setActorId(String value) { actorId = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "event_at", nullable = false)
	public Date getEventAt() { return eventAt; } public void setEventAt(Date value) { eventAt = value; }
}
