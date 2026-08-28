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

/** Relasi lot material ke lot hasil produksi untuk traceability. */
@Entity
@Table(schema = "inventory_production", name = "production_lot_genealogy",
	uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "input_line_id", "output_line_id" }))
public class ProduksiGenealogiLot implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long documentId;
	private Long inputLineId;
	private Long outputLineId;
	private String inputLotNo;
	private String outputLotNo;
	private BigDecimal allocatedQty = BigDecimal.ZERO;
	private Date createdAt = new Date();

	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	@Column(name = "document_id", nullable = false)
	public Long getDocumentId() { return documentId; }
	public void setDocumentId(Long documentId) { this.documentId = documentId; }
	@Column(name = "input_line_id", nullable = false)
	public Long getInputLineId() { return inputLineId; }
	public void setInputLineId(Long inputLineId) { this.inputLineId = inputLineId; }
	@Column(name = "output_line_id", nullable = false)
	public Long getOutputLineId() { return outputLineId; }
	public void setOutputLineId(Long outputLineId) { this.outputLineId = outputLineId; }
	@Column(name = "input_lot_no", length = 120)
	public String getInputLotNo() { return inputLotNo; }
	public void setInputLotNo(String inputLotNo) { this.inputLotNo = inputLotNo; }
	@Column(name = "output_lot_no", length = 120)
	public String getOutputLotNo() { return outputLotNo; }
	public void setOutputLotNo(String outputLotNo) { this.outputLotNo = outputLotNo; }
	@Column(name = "allocated_qty", nullable = false, precision = 19, scale = 4)
	public BigDecimal getAllocatedQty() { return allocatedQty; }
	public void setAllocatedQty(BigDecimal allocatedQty) { this.allocatedQty = allocatedQty; }
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
