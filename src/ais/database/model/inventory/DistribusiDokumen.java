package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

/** Dokumen induk distribusi/pengiriman yang skemanya dikelola Hibernate. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "distribution_document", uniqueConstraints = {
	@UniqueConstraint(columnNames = { "toko_id", "document_type", "document_no" }),
	@UniqueConstraint(columnNames = { "toko_id", "client_mutation_id" }) })
public class DistribusiDokumen implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; private Long tokoId; private String documentType; private String documentNo;
	private String status = "DRAFT"; private String referenceNo; private String originName;
	private String destinationName; private Long originTokoId; private Long destinationTokoId;
	private String carrierName; private String trackingNo; private String receiverName; private String proofUrl;
	private String freightInvoiceNo; private BigDecimal freightAmount; private Date freightInvoiceDate;
	private Date plannedAt; private Date actualAt;
	private String notes; private String clientMutationId; private String createdBy;
	private Date createdAt = new Date(); private String updatedBy; private Date updatedAt = new Date();
	private Long version = Long.valueOf(0L);

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long id) { this.id = id; }
	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; } public void setTokoId(Long tokoId) { this.tokoId = tokoId; }
	@Column(name = "document_type", nullable = false, length = 50)
	public String getDocumentType() { return documentType; } public void setDocumentType(String value) { documentType = value; }
	@Column(name = "document_no", nullable = false, length = 80)
	public String getDocumentNo() { return documentNo; } public void setDocumentNo(String value) { documentNo = value; }
	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; } public void setStatus(String value) { status = value; }
	@Column(name = "reference_no", length = 120)
	public String getReferenceNo() { return referenceNo; } public void setReferenceNo(String value) { referenceNo = value; }
	@Column(name = "origin_name", length = 180)
	public String getOriginName() { return originName; } public void setOriginName(String value) { originName = value; }
	@Column(name = "destination_name", length = 180)
	public String getDestinationName() { return destinationName; } public void setDestinationName(String value) { destinationName = value; }
	@Column(name = "origin_toko_id")
	public Long getOriginTokoId() { return originTokoId; } public void setOriginTokoId(Long value) { originTokoId = value; }
	@Column(name = "destination_toko_id")
	public Long getDestinationTokoId() { return destinationTokoId; } public void setDestinationTokoId(Long value) { destinationTokoId = value; }
	@Column(name = "carrier_name", length = 180)
	public String getCarrierName() { return carrierName; } public void setCarrierName(String value) { carrierName = value; }
	@Column(name = "tracking_no", length = 120)
	public String getTrackingNo() { return trackingNo; } public void setTrackingNo(String value) { trackingNo = value; }
	@Column(name = "receiver_name", length = 180)
	public String getReceiverName() { return receiverName; } public void setReceiverName(String value) { receiverName = value; }
	@Column(name = "proof_url", length = 1000)
	public String getProofUrl() { return proofUrl; } public void setProofUrl(String value) { proofUrl = value; }
	@Column(name = "freight_invoice_no", length = 120)
	public String getFreightInvoiceNo() { return freightInvoiceNo; } public void setFreightInvoiceNo(String value) { freightInvoiceNo = value; }
	@Column(name = "freight_amount", precision = 19, scale = 2)
	public BigDecimal getFreightAmount() { return freightAmount; } public void setFreightAmount(BigDecimal value) { freightAmount = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "freight_invoice_date")
	public Date getFreightInvoiceDate() { return freightInvoiceDate; } public void setFreightInvoiceDate(Date value) { freightInvoiceDate = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "planned_at")
	public Date getPlannedAt() { return plannedAt; } public void setPlannedAt(Date value) { plannedAt = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "actual_at")
	public Date getActualAt() { return actualAt; } public void setActualAt(Date value) { actualAt = value; }
	@Column(name = "notes", columnDefinition = "text")
	public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
	@Column(name = "client_mutation_id", length = 100)
	public String getClientMutationId() { return clientMutationId; } public void setClientMutationId(String value) { clientMutationId = value; }
	@Column(name = "created_by", length = 100)
	public String getCreatedBy() { return createdBy; } public void setCreatedBy(String value) { createdBy = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; } public void setCreatedAt(Date value) { createdAt = value; }
	@Column(name = "updated_by", length = 100)
	public String getUpdatedBy() { return updatedBy; } public void setUpdatedBy(String value) { updatedBy = value; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Date value) { updatedAt = value; }
	@Column(name = "version", nullable = false)
	public Long getVersion() { return version; } public void setVersion(Long value) { version = value; }
}
