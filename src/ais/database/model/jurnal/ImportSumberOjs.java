package ais.database.model.jurnal;
import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="import_sumber_ojs")
public class ImportSumberOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String sourceKey,displayName,ojsVersion,dialect,schemaSignature,connectionReference,status;
 @Column(name="source_key",nullable=false,length=120) public String getSourceKey(){return sourceKey;} public void setSourceKey(String v){sourceKey=v;}
 @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
 @Column(name="ojs_version",nullable=false,length=40) public String getOjsVersion(){return ojsVersion;} public void setOjsVersion(String v){ojsVersion=v;}
 @Column(name="dialect",nullable=false,length=40) public String getDialect(){return dialect;} public void setDialect(String v){dialect=v;}
 @Column(name="schema_signature",nullable=false,length=128) public String getSchemaSignature(){return schemaSignature;} public void setSchemaSignature(String v){schemaSignature=v;}
 @Column(name="connection_reference",nullable=false,length=255) public String getConnectionReference(){return connectionReference;} public void setConnectionReference(String v){connectionReference=v;}
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
