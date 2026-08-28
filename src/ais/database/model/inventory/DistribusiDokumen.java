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
@Table(schema = "inventory_distribution", name = "distribution_document",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = { "toko_id", "document_type", "document_no" }),
		@UniqueConstraint(columnNames = { "toko_id", "client_mutation_id" })
	})
public class DistribusiDokumen implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long tokoId;
	private String documentType;
	private String documentNo;
	private String status = "DRAFT";
	private String referenceNo;
	private String originName;
	private String destinationName;
	private Long originTokoId;
	private Long destinationTokoId;
	private String carrierName;
	private String trackingNo;
	private String receiverName;
	private String proofUrl;
	private String freightInvoiceNo;
	private BigDecimal freightAmount;
	private Date freightInvoiceDate;
	private Date plannedAt;
	private Date actualAt;
	private String notes;
	private String clientMutationId;
	private String createdBy;
	private Date createdAt = new Date();
	private String updatedBy;
	private Date updatedAt = new Date();
	private Long version = Long.valueOf(0L);

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "toko_id", nullable = false)
	public Long getTokoId() { return tokoId; }
	public void setTokoId(Long tokoId) { this.tokoId = tokoId; }

	@Column(name = "document_type", nullable = false, length = 50)
	public String getDocumentType() { return documentType; }
	public void setDocumentType(String documentType) { this.documentType = documentType; }

	@Column(name = "document_no", nullable = false, length = 80)
	public String getDocumentNo() { return documentNo; }
	public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }

	@Column(name = "status", nullable = false, length = 30)
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	@Column(name = "reference_no", length = 120)
	public String getReferenceNo() { return referenceNo; }
	public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }

	@Column(name = "origin_name", length = 180)
	public String getOriginName() { return originName; }
	public void setOriginName(String originName) { this.originName = originName; }

	@Column(name = "destination_name", length = 180)
	public String getDestinationName() { return destinationName; }
	public void setDestinationName(String destinationName) { this.destinationName = destinationName; }

	@Column(name = "origin_toko_id")
	public Long getOriginTokoId() { return originTokoId; }
	public void setOriginTokoId(Long originTokoId) { this.originTokoId = originTokoId; }

	@Column(name = "destination_toko_id")
	public Long getDestinationTokoId() { return destinationTokoId; }
	public void setDestinationTokoId(Long destinationTokoId) { this.destinationTokoId = destinationTokoId; }

	@Column(name = "carrier_name", length = 180)
	public String getCarrierName() { return carrierName; }
	public void setCarrierName(String carrierName) { this.carrierName = carrierName; }

	@Column(name = "tracking_no", length = 120)
	public String getTrackingNo() { return trackingNo; }
	public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }

	@Column(name = "receiver_name", length = 180)
	public String getReceiverName() { return receiverName; }
	public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

	@Column(name = "proof_url", length = 1000)
	public String getProofUrl() { return proofUrl; }
	public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }

	@Column(name = "freight_invoice_no", length = 120)
	public String getFreightInvoiceNo() { return freightInvoiceNo; }
	public void setFreightInvoiceNo(String freightInvoiceNo) { this.freightInvoiceNo = freightInvoiceNo; }

	@Column(name = "freight_amount", precision = 19, scale = 2)
	public BigDecimal getFreightAmount() { return freightAmount; }
	public void setFreightAmount(BigDecimal freightAmount) { this.freightAmount = freightAmount; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "freight_invoice_date")
	public Date getFreightInvoiceDate() { return freightInvoiceDate; }
	public void setFreightInvoiceDate(Date freightInvoiceDate) { this.freightInvoiceDate = freightInvoiceDate; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "planned_at")
	public Date getPlannedAt() { return plannedAt; }
	public void setPlannedAt(Date plannedAt) { this.plannedAt = plannedAt; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "actual_at")
	public Date getActualAt() { return actualAt; }
	public void setActualAt(Date actualAt) { this.actualAt = actualAt; }

	@Column(name = "notes", columnDefinition = "text")
	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }

	@Column(name = "client_mutation_id", length = 100)
	public String getClientMutationId() { return clientMutationId; }
	public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }

	@Column(name = "created_by", length = 100)
	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

	@Column(name = "updated_by", length = 100)
	public String getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

	@Column(name = "version", nullable = false)
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
}
