package ais.database.model.jurnal;
import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.template_email_jurnal} —
 * satu versi template email otomatis modul jurnal (mis. notifikasi status naskah, undangan
 * review), diidentifikasi lewat kunci template ({@link #getTemplateKey()}) dan bahasa
 * ({@link #getLocale()}), dengan riwayat versi tersimpan lewat {@link #getVersionNumber()}.
 *
 * <p>
 * {@link #getSubjectTemplate()} dan {@link #getBodyTemplate()} berisi teks template mentah
 * (kemungkinan berisi placeholder variabel yang disubstitusi saat pengiriman).
 * {@link #getVariablePolicyJson()} mendefinisikan variabel apa saja yang boleh/wajib dipakai
 * dalam template ini, dalam format JSON (kebijakan validasi konten template).
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="template_email_jurnal")
public class TemplateEmailJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String templateKey,locale,subjectTemplate,bodyTemplate,variablePolicyJson; private Integer versionNumber;
 /** Kunci identifikasi template (mis. "NASKAH_DITERIMA", "UNDANGAN_REVIEW"). */
 @Column(name="template_key",nullable=false,length=160) public String getTemplateKey(){return templateKey;} public void setTemplateKey(String v){templateKey=v;}
 /** Kode bahasa/lokal template (mis. "id", "en"). */
 @Column(name="locale",nullable=false,length=20) public String getLocale(){return locale;} public void setLocale(String v){locale=v;}
 /** Template subjek email, dapat berisi placeholder variabel. */
 @Column(name="subject_template",nullable=false,columnDefinition="text") public String getSubjectTemplate(){return subjectTemplate;} public void setSubjectTemplate(String v){subjectTemplate=v;}
 /** Template isi/badan email, dapat berisi placeholder variabel. */
 @Column(name="body_template",nullable=false,columnDefinition="text") public String getBodyTemplate(){return bodyTemplate;} public void setBodyTemplate(String v){bodyTemplate=v;}
 /** Kebijakan variabel yang diizinkan/wajib pada template ini, dalam format JSON. */
 @Column(name="variable_policy_json",nullable=false,columnDefinition="text") public String getVariablePolicyJson(){return variablePolicyJson;} public void setVariablePolicyJson(String v){variablePolicyJson=v;}
 /** Nomor versi template ini (bertambah setiap kali template direvisi). */
 @Column(name="version_number",nullable=false) public Integer getVersionNumber(){return versionNumber;} public void setVersionNumber(Integer v){versionNumber=v;}
}
