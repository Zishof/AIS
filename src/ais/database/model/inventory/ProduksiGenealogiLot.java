package ais.database.model.inventory;
import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable; import java.math.BigDecimal; import java.util.Date;
import javax.persistence.Column; import javax.persistence.Entity; import javax.persistence.GeneratedValue; import javax.persistence.Id; import javax.persistence.Table; import javax.persistence.Temporal; import javax.persistence.TemporalType; import javax.persistence.UniqueConstraint;
@Entity @Table(schema="inventory_production",name="production_lot_genealogy",uniqueConstraints=@UniqueConstraint(columnNames={"document_id","input_line_id","output_line_id"}))
public class ProduksiGenealogiLot implements Serializable {
	private static final long serialVersionUID=1L; private Long id; private Long documentId; private Long inputLineId; private Long outputLineId; private String inputLotNo; private String outputLotNo; private BigDecimal allocatedQty=BigDecimal.ZERO; private Date createdAt=new Date();
	@Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",unique=true,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
	@Column(name="document_id",nullable=false) public Long getDocumentId(){return documentId;} public void setDocumentId(Long v){documentId=v;}
	@Column(name="input_line_id",nullable=false) public Long getInputLineId(){return inputLineId;} public void setInputLineId(Long v){inputLineId=v;}
	@Column(name="output_line_id",nullable=false) public Long getOutputLineId(){return outputLineId;} public void setOutputLineId(Long v){outputLineId=v;}
	@Column(name="input_lot_no",length=120) public String getInputLotNo(){return inputLotNo;} public void setInputLotNo(String v){inputLotNo=v;}
	@Column(name="output_lot_no",length=120) public String getOutputLotNo(){return outputLotNo;} public void setOutputLotNo(String v){outputLotNo=v;}
	@Column(name="allocated_qty",nullable=false,precision=19,scale=4) public BigDecimal getAllocatedQty(){return allocatedQty;} public void setAllocatedQty(BigDecimal v){allocatedQty=v;}
	@Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
