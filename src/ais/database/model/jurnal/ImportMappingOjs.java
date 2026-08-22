package ais.database.model.jurnal;
import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="import_mapping_ojs")
public class ImportMappingOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private Long sourceId,jobId,targetId; private String sourceTable,sourcePk,sourceField,targetType,targetField,decision,rawPayload,sourceChecksum;
 @Column(name="source_id",nullable=false) public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
 @Column(name="job_id") public Long getJobId(){return jobId;} public void setJobId(Long v){jobId=v;}
 @Column(name="source_table",nullable=false,length=160) public String getSourceTable(){return sourceTable;} public void setSourceTable(String v){sourceTable=v;}
 @Column(name="source_pk",nullable=false,length=500) public String getSourcePk(){return sourcePk;} public void setSourcePk(String v){sourcePk=v;}
 @Column(name="source_field",length=160) public String getSourceField(){return sourceField;} public void setSourceField(String v){sourceField=v;}
 @Column(name="target_type",length=255) public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=v;}
 @Column(name="target_id") public Long getTargetId(){return targetId;} public void setTargetId(Long v){targetId=v;}
 @Column(name="target_field",length=160) public String getTargetField(){return targetField;} public void setTargetField(String v){targetField=v;}
 @Column(name="decision",nullable=false,length=40) public String getDecision(){return decision;} public void setDecision(String v){decision=v;}
 @Column(name="raw_payload",columnDefinition="text") public String getRawPayload(){return rawPayload;} public void setRawPayload(String v){rawPayload=v;}
 @Column(name="source_checksum",length=64) public String getSourceChecksum(){return sourceChecksum;} public void setSourceChecksum(String v){sourceChecksum=v;}
}
