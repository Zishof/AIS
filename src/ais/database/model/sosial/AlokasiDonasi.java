package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="alokasi_donasi")
public class AlokasiDonasi extends SocialRecord { private static final long serialVersionUID=1L; private TransaksiDonasi transaction; private ProgramDonatur program; private JenisDanaSosial fundType; private BigDecimal amount; private String restriction; private Date postedAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="transaction_id",nullable=false) public TransaksiDonasi getTransaction(){return transaction;} public void setTransaction(TransaksiDonasi v){transaction=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id") public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="fund_type_id",nullable=false) public JenisDanaSosial getFundType(){return fundType;} public void setFundType(JenisDanaSosial v){fundType=v;}
 @Column(name="amount",nullable=false,precision=19,scale=2) public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 @Column(name="restriction",length=120) public String getRestriction(){return restriction;} public void setRestriction(String v){restriction=trim(v);}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="posted_at") public Date getPostedAt(){return postedAt;} public void setPostedAt(Date v){postedAt=v;}
}
