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

/** Dokumen induk produksi yang skemanya dikelola Hibernate. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "production_document",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = { "toko_id", "document_type", "document_no" }),
		@UniqueConstraint(columnNames = { "toko_id", "client_mutation_id" })
	})
public class ProduksiDokumen implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long tokoId;
	private String documentType;
	private String documentNo;
	private String status = "DRAFT";
	private String referenceNo;
	private Long bomId;
	private BigDecimal plannedQty = BigDecimal.ZERO;
	private BigDecimal actualQty = BigDecimal.ZERO;
	private String uom;
	private BigDecimal materialCost = BigDecimal.ZERO;
	private BigDecimal laborCost = BigDecimal.ZERO;
	private BigDecimal overheadCost = BigDecimal.ZERO;
	private BigDecimal totalCost = BigDecimal.ZERO;
	private BigDecimal unitCost = BigDecimal.ZERO;
	private Date plannedAt;
	private Date actualAt;
	private String notes;
	private String clientMutationId;
	private String createdBy;
	private Date createdAt = new Date();
	private String updatedBy;
	private Date updatedAt = new Date();
	private Long version = Long.valueOf(0L);

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
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
	@Column(name = "bom_id")
	public Long getBomId() { return bomId; }
	public void setBomId(Long bomId) { this.bomId = bomId; }
	@Column(name = "planned_qty", precision = 19, scale = 4)
	public BigDecimal getPlannedQty() { return plannedQty; }
	public void setPlannedQty(BigDecimal plannedQty) { this.plannedQty = plannedQty; }
	@Column(name = "actual_qty", precision = 19, scale = 4)
	public BigDecimal getActualQty() { return actualQty; }
	public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
	@Column(name = "uom", length = 30)
	public String getUom() { return uom; }
	public void setUom(String uom) { this.uom = uom; }
	@Column(name = "material_cost", precision = 19, scale = 2)
	public BigDecimal getMaterialCost() { return materialCost; }
	public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }
	@Column(name = "labor_cost", precision = 19, scale = 2)
	public BigDecimal getLaborCost() { return laborCost; }
	public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
	@Column(name = "overhead_cost", precision = 19, scale = 2)
	public BigDecimal getOverheadCost() { return overheadCost; }
	public void setOverheadCost(BigDecimal overheadCost) { this.overheadCost = overheadCost; }
	@Column(name = "total_cost", precision = 19, scale = 2)
	public BigDecimal getTotalCost() { return totalCost; }
	public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
	@Column(name = "unit_cost", precision = 19, scale = 4)
	public BigDecimal getUnitCost() { return unitCost; }
	public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "planned_at")
	public Date getPlannedAt() { return plannedAt; }
	public void setPlannedAt(Date plannedAt) { this.plannedAt = plannedAt; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "actual_at")
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
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
	@Column(name = "updated_by", length = 100)
	public String getUpdatedBy() { return updatedBy; }
	public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "updated_at", nullable = false)
	public Date getUpdatedAt() { return updatedAt; }
	public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
	@Column(name = "version", nullable = false)
	public Long getVersion() { return version; }
	public void setVersion(Long version) { this.version = version; }
}
