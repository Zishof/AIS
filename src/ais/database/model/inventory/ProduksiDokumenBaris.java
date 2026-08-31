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

/**
 * Entitas Hibernate untuk tabel {@code koperasi.production_document_line}, merepresentasikan
 * satu baris rincian pada dokumen produksi ({@link ProduksiDokumen}, dirujuk lewat id polos
 * {@link #getDocumentId()}) pada modul manufaktur/produksi koperasi. Kombinasi
 * ({@code document_id}, {@code line_no}) bersifat unik, memberi urutan tetap
 * ({@link #getLineNo()}) pada baris-baris dalam satu dokumen.
 * <p>
 * Satu baris mewakili satu pergerakan barang (bahan baku yang dikonsumsi maupun hasil produksi
 * yang dihasilkan — dibedakan lewat {@link #getLineType()}) untuk satu item ({@link #getItemId()}
 * /{@link #getItemCode()}/{@link #getItemName()}) sejumlah {@link #getQty()} dengan satuan
 * {@link #getUom()}, opsional terikat pada nomor lot tertentu ({@link #getLotNo()}, untuk
 * pelacakan genealogi lot — lihat {@link ProduksiGenealogiLot}) dan biaya per unit
 * ({@link #getUnitCost()}). Flag {@link #getStockAffecting()} menandai apakah baris ini
 * benar-benar memengaruhi saldo stok gudang saat diproses, atau hanya bersifat informatif.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "production_document_line", uniqueConstraints = @UniqueConstraint(columnNames = { "document_id", "line_no" }))
public class ProduksiDokumenBaris implements Serializable {
	private static final long serialVersionUID = 1L;
	private Long id; private Long documentId; private Integer lineNo; private String lineType;
	private Long itemId; private String itemCode; private String itemName; private BigDecimal qty = BigDecimal.ZERO;
	private String uom; private String lotNo; private BigDecimal unitCost = BigDecimal.ZERO;
	private Boolean stockAffecting = Boolean.FALSE; private String notes;
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", unique = true, nullable = false) public Long getId(){return id;} public void setId(Long v){id=v;}
	@Column(name="document_id",nullable=false) public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
	@Column(name="line_no",nullable=false) public Integer getLineNo(){return lineNo;} public void setLineNo(Integer v){lineNo=v;}
	/** Jenis baris (mis. konsumsi bahan baku vs hasil produksi keluar). */
	@Column(name="line_type",nullable=false,length=30) public String getLineType(){return lineType;} public void setLineType(String v){lineType=v;}
	@Column(name="item_id") public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
	@Column(name="item_code",length=100) public String getItemCode(){return itemCode;} public void setItemCode(String v){itemCode=v;}
	@Column(name="item_name",nullable=false,length=255) public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;}
	@Column(name="qty",nullable=false,precision=19,scale=4) public BigDecimal getQty(){return qty;} public void setQty(BigDecimal v){qty=v;}
	@Column(name="uom",length=30) public String getUom(){return uom;} public void setUom(String v){uom=v;}
	@Column(name="lot_no",length=120) public String getLotNo(){return lotNo;} public void setLotNo(String v){lotNo=v;}
	@Column(name="unit_cost",precision=19,scale=4) public BigDecimal getUnitCost(){return unitCost;} public void setUnitCost(BigDecimal v){unitCost=v;}
	/** Menandai apakah baris ini memengaruhi saldo stok gudang saat dokumen diproses, atau hanya informatif. */
	@Column(name="stock_affecting",nullable=false) public Boolean getStockAffecting(){return stockAffecting;} public void setStockAffecting(Boolean v){stockAffecting=v;}
	@Column(name="notes",columnDefinition="text") public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
