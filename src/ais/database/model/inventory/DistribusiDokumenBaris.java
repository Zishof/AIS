package ais.database.model.inventory;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/** Baris barang pada dokumen distribusi. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "inventory_distribution", name = "distribution_document_line",
	uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_no" }))
public class DistribusiDokumenBaris implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long documentId;
	private Integer lineNo;
	private Long itemId;
	private String itemCode;
	private String itemName;
	private BigDecimal qty = BigDecimal.ZERO;
	private String uom;
	private String notes;
	private Long sourceProductId;
	private Long destinationProductId;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "document_id", nullable = false)
	public Long getDocumentId() { return documentId; }
	public void setDocumentId(Long documentId) { this.documentId = documentId; }

	@Column(name = "line_no", nullable = false)
	public Integer getLineNo() { return lineNo; }
	public void setLineNo(Integer lineNo) { this.lineNo = lineNo; }

	@Column(name = "item_id")
	public Long getItemId() { return itemId; }
	public void setItemId(Long itemId) { this.itemId = itemId; }

	@Column(name = "item_code", length = 100)
	public String getItemCode() { return itemCode; }
	public void setItemCode(String itemCode) { this.itemCode = itemCode; }

	@Column(name = "item_name", nullable = false, length = 255)
	public String getItemName() { return itemName; }
	public void setItemName(String itemName) { this.itemName = itemName; }

	@Column(name = "qty", nullable = false, precision = 24, scale = 6)
	public BigDecimal getQty() { return qty; }
	public void setQty(BigDecimal qty) { this.qty = qty; }

	@Column(name = "uom", length = 50)
	public String getUom() { return uom; }
	public void setUom(String uom) { this.uom = uom; }

	@Column(name = "notes", columnDefinition = "text")
	public String getNotes() { return notes; }
	public void setNotes(String notes) { this.notes = notes; }

	@Column(name = "source_product_id")
	public Long getSourceProductId() { return sourceProductId; }
	public void setSourceProductId(Long sourceProductId) { this.sourceProductId = sourceProductId; }

	@Column(name = "destination_product_id")
	public Long getDestinationProductId() { return destinationProductId; }
	public void setDestinationProductId(Long destinationProductId) { this.destinationProductId = destinationProductId; }
}
