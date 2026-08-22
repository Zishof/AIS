package ais.database.model.jurnal;
import javax.persistence.*;
@Entity @Table(schema="penelitiandanpengabdian",name="template_email_jurnal")
public class TemplateEmailJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String templateKey,locale,subjectTemplate,bodyTemplate,variablePolicyJson; private Integer versionNumber;
 @Column(name="template_key",nullable=false,length=160) public String getTemplateKey(){return templateKey;} public void setTemplateKey(String v){templateKey=v;}
 @Column(name="locale",nullable=false,length=20) public String getLocale(){return locale;} public void setLocale(String v){locale=v;}
 @Column(name="subject_template",nullable=false,columnDefinition="text") public String getSubjectTemplate(){return subjectTemplate;} public void setSubjectTemplate(String v){subjectTemplate=v;}
 @Column(name="body_template",nullable=false,columnDefinition="text") public String getBodyTemplate(){return bodyTemplate;} public void setBodyTemplate(String v){bodyTemplate=v;}
 @Column(name="variable_policy_json",nullable=false,columnDefinition="text") public String getVariablePolicyJson(){return variablePolicyJson;} public void setVariablePolicyJson(String v){variablePolicyJson=v;}
 @Column(name="version_number",nullable=false) public Integer getVersionNumber(){return versionNumber;} public void setVersionNumber(Integer v){versionNumber=v;}
}
