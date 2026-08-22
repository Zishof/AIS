package ais.database.model.jurnal;
import java.util.Date; import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="import_job_ojs")
public class ImportJobOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long sourceId; private Boolean dryRun; private String status,idempotencyKey,reportJson,errorSummary; private Date startedAt,finishedAt;
 @Column(name="source_id",nullable=false) public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
 @Column(name="dry_run",nullable=false) public Boolean getDryRun(){return dryRun;} public void setDryRun(Boolean v){dryRun=v;}
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
 @Column(name="idempotency_key",nullable=false,length=160) public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
 @Column(name="report_json",columnDefinition="text") public String getReportJson(){return reportJson;} public void setReportJson(String v){reportJson=v;}
 @Column(name="error_summary",columnDefinition="text") public String getErrorSummary(){return errorSummary;} public void setErrorSummary(String v){errorSummary=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="started_at") public Date getStartedAt(){return startedAt;} public void setStartedAt(Date v){startedAt=v;}
 @Temporal(TemporalType.TIMESTAMP) @Column(name="finished_at") public Date getFinishedAt(){return finishedAt;} public void setFinishedAt(Date v){finishedAt=v;}
}
