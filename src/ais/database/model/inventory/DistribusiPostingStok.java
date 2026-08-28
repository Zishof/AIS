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

/** Penanda idempoten posting stok untuk satu baris distribusi. */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "inventory_distribution", name = "distribution_stock_posting",
	uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_id", "direction" }))
public class DistribusiPostingStok implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long documentId;
	private Long lineId;
	private String direction;
	private Long legacyMutationId;
	private Long sourceTokoId;
	private Long destinationTokoId;
	private Long sourceProductId;
	private Long destinationProductId;
	private BigDecimal qty = BigDecimal.ZERO;
	private String createdBy;
	private Date createdAt = new Date();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	@Column(name = "document_id", nullable = false)
	public Long getDocumentId() { return documentId; }
	public void setDocumentId(Long documentId) { this.documentId = documentId; }

	@Column(name = "line_id", nullable = false)
	public Long getLineId() { return lineId; }
	public void setLineId(Long lineId) { this.lineId = lineId; }

	@Column(name = "direction", nullable = false, length = 10)
	public String getDirection() { return direction; }
	public void setDirection(String direction) { this.direction = direction; }

	@Column(name = "legacy_mutation_id", nullable = false)
	public Long getLegacyMutationId() { return legacyMutationId; }
	public void setLegacyMutationId(Long legacyMutationId) { this.legacyMutationId = legacyMutationId; }

	@Column(name = "source_toko_id", nullable = false)
	public Long getSourceTokoId() { return sourceTokoId; }
	public void setSourceTokoId(Long sourceTokoId) { this.sourceTokoId = sourceTokoId; }

	@Column(name = "destination_toko_id", nullable = false)
	public Long getDestinationTokoId() { return destinationTokoId; }
	public void setDestinationTokoId(Long destinationTokoId) { this.destinationTokoId = destinationTokoId; }

	@Column(name = "source_product_id", nullable = false)
	public Long getSourceProductId() { return sourceProductId; }
	public void setSourceProductId(Long sourceProductId) { this.sourceProductId = sourceProductId; }

	@Column(name = "destination_product_id", nullable = false)
	public Long getDestinationProductId() { return destinationProductId; }
	public void setDestinationProductId(Long destinationProductId) { this.destinationProductId = destinationProductId; }

	@Column(name = "qty", nullable = false, precision = 24, scale = 6)
	public BigDecimal getQty() { return qty; }
	public void setQty(BigDecimal qty) { this.qty = qty; }

	@Column(name = "created_by", length = 100)
	public String getCreatedBy() { return createdBy; }
	public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_at", nullable = false)
	public Date getCreatedAt() { return createdAt; }
	public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
