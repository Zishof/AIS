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
@Table(schema = "inventory_distribution", name = "distribution_document_line", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_no" }))
public class DistribusiDokumenBaris implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; private Long documentId; private Integer lineNo; private Long itemId;
	private String itemCode; private String itemName; private BigDecimal qty = BigDecimal.ZERO;
	private String uom; private String notes; private Long sourceProductId; private Long destinationProductId;
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; } public void setId(Long value) { id = value; }
	@Column(name = "document_id", nullable = false)
	public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { documentId = value; }
	@Column(name = "line_no", nullable = false)
	public Integer getLineNo() { return lineNo; } public void setLineNo(Integer value) { lineNo = value; }
	@Column(name = "item_id") public Long getItemId() { return itemId; } public void setItemId(Long value) { itemId = value; }
	@Column(name = "item_code", length = 100) public String getItemCode() { return itemCode; } public void setItemCode(String value) { itemCode = value; }
	@Column(name = "item_name", nullable = false, length = 255) public String getItemName() { return itemName; } public void setItemName(String value) { itemName = value; }
	@Column(name = "qty", nullable = false, precision = 24, scale = 6) public BigDecimal getQty() { return qty; } public void setQty(BigDecimal value) { qty = value; }
	@Column(name = "uom", length = 50) public String getUom() { return uom; } public void setUom(String value) { uom = value; }
	@Column(name = "notes", columnDefinition = "text") public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
	@Column(name = "source_product_id") public Long getSourceProductId() { return sourceProductId; } public void setSourceProductId(Long value) { sourceProductId = value; }
	@Column(name = "destination_product_id") public Long getDestinationProductId() { return destinationProductId; } public void setDestinationProductId(Long value) { destinationProductId = value; }
}
