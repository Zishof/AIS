package ais.database.model.jurnal;
import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="import_checkpoint_ojs")
public class ImportCheckpointOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long jobId,processedCount,acceptedCount,failedCount; private Integer batchNumber; private String sourceTable,cursorValue,status;
 @Column(name="job_id",nullable=false) public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
 @Column(name="source_table",nullable=false,length=160) public String getSourceTable(){return sourceTable;} public void setSourceTable(String v){sourceTable=v;}
 @Column(name="cursor_value",length=500) public String getCursorValue(){return cursorValue;} public void setCursorValue(String v){cursorValue=v;}
 @Column(name="batch_number",nullable=false) public Integer getBatchNumber(){return batchNumber;} public void setBatchNumber(Integer v){batchNumber=v;}
 @Column(name="processed_count",nullable=false) public Long getProcessedCount(){return processedCount;} public void setProcessedCount(Long v){processedCount=v;}
 @Column(name="accepted_count",nullable=false) public Long getAcceptedCount(){return acceptedCount;} public void setAcceptedCount(Long v){acceptedCount=v;}
 @Column(name="failed_count",nullable=false) public Long getFailedCount(){return failedCount;} public void setFailedCount(Long v){failedCount=v;}
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
