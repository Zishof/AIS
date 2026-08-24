package ais.database.model.sosial;
import java.math.BigDecimal; import java.util.Date; import javax.persistence.*; import org.hibernate.envers.Audited;
@Entity @Audited @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true) @Table(schema="public",name="perkembangan_program_sosial")
public class PerkembanganProgramSosial extends SocialRecord { private static final long serialVersionUID=1L; private ProgramDonatur program; private String title,content,mediaJson,milestone,publishStatus,author; private Date updateDate; private BigDecimal amountUsed; private Integer beneficiaryCount;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="program_id",nullable=false) public ProgramDonatur getProgram(){return program;} public void setProgram(ProgramDonatur v){program=v;}
 @Column(name="title",nullable=false,length=255) public String getTitle(){return title;} public void setTitle(String v){title=trim(v);}
 @Column(name="content",columnDefinition="TEXT",nullable=false) public String getContent(){return content;} public void setContent(String v){content=v;}
 @Column(name="media_json",columnDefinition="TEXT") public String getMediaJson(){return mediaJson;} public void setMediaJson(String v){mediaJson=v;}
 @Column(name="milestone",length=120) public String getMilestone(){return milestone;} public void setMilestone(String v){milestone=trim(v);}
 @Column(name="publish_status",length=40) public String getPublishStatus(){return publishStatus;} public void setPublishStatus(String v){publishStatus=trim(v);}
 @Column(name="author",length=255) public String getAuthor(){return author;} public void setAuthor(String v){author=trim(v);}
 @Temporal(TemporalType.DATE) @Column(name="update_date") public Date getUpdateDate(){return updateDate;} public void setUpdateDate(Date v){updateDate=v;}
 @Column(name="amount_used",precision=19,scale=2) public BigDecimal getAmountUsed(){return amountUsed;} public void setAmountUsed(BigDecimal v){amountUsed=v;}
 @Column(name="beneficiary_count") public Integer getBeneficiaryCount(){return beneficiaryCount;} public void setBeneficiaryCount(Integer v){beneficiaryCount=v;}
}
