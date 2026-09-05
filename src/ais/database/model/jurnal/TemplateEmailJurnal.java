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
 *
 * <p>
 * Digunakan secara aktif oleh {@code ais.action.master.jurnal.JurnalEmailService}: setiap
 * penyimpanan template baru ({@code save(...)}) menaikkan {@link #getVersionNumber()} (query
 * {@code max(versionNumber)} per kombinasi jurnal+kunci+lokal, lalu +1) sehingga baris versi
 * lama TETAP disimpan sebagai riwayat (tidak diedit di tempat) — pengiriman email
 * ({@code send(...)}) selalu mengambil baris {@code aktif=true} dengan
 * {@code versionNumber} tertinggi. {@link #getVariablePolicyJson()} lalu dipakai sebagai
 * ALLOWLIST saat substitusi variabel ke {@link #getSubjectTemplate()}/{@link #getBodyTemplate()}
 * — variabel di luar daftar ini ditolak, mencegah injeksi konten template arbitrer oleh
 * pemanggil. Kunci template divalidasi terhadap katalog tetap
 * {@code JurnalEmailTemplateCatalog} (bukan string bebas), dan penyediaan template awal
 * dilakukan lewat {@code seedDefaults(...)}.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="template_email_jurnal")
public class TemplateEmailJurnal extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String templateKey,locale,subjectTemplate,bodyTemplate,variablePolicyJson; private Integer versionNumber;
 /**
  * Kunci identifikasi template (mis. "NASKAH_DITERIMA", "UNDANGAN_REVIEW"), divalidasi
  * terhadap katalog tetap {@code JurnalEmailTemplateCatalog} sebelum disimpan — bukan
  * string bebas dari pemanggil. Setter tidak melakukan validasi ulang; validasi dilakukan
  * oleh service pemanggil sebelum {@code setTemplateKey} dipanggil.
  */
 @Column(name="template_key",nullable=false,length=160) public String getTemplateKey(){return templateKey;} public void setTemplateKey(String v){templateKey=v;}
 /** Kode bahasa/lokal template (mis. "id", "en"), dinormalisasi oleh service pemanggil sebelum disimpan. */
 @Column(name="locale",nullable=false,length=20) public String getLocale(){return locale;} public void setLocale(String v){locale=v;}
 /** Template subjek email, dapat berisi placeholder variabel yang divalidasi terhadap {@link #getVariablePolicyJson()} saat pengiriman. */
 @Column(name="subject_template",nullable=false,columnDefinition="text") public String getSubjectTemplate(){return subjectTemplate;} public void setSubjectTemplate(String v){subjectTemplate=v;}
 /** Template isi/badan email, dapat berisi placeholder variabel yang divalidasi terhadap {@link #getVariablePolicyJson()} saat pengiriman. */
 @Column(name="body_template",nullable=false,columnDefinition="text") public String getBodyTemplate(){return bodyTemplate;} public void setBodyTemplate(String v){bodyTemplate=v;}
 /**
  * Kebijakan variabel yang diizinkan/wajib pada template ini, dalam format JSON
  * (mis. {@code {"schemaVersion":1,"allowed":[...]}}). Berfungsi sebagai ALLOWLIST saat
  * substitusi variabel ke subjek/isi email — mencegah variabel di luar daftar disuntikkan
  * ke dalam email terkirim.
  */
 @Column(name="variable_policy_json",nullable=false,columnDefinition="text") public String getVariablePolicyJson(){return variablePolicyJson;} public void setVariablePolicyJson(String v){variablePolicyJson=v;}
 /**
  * Nomor versi template ini (bertambah setiap kali template direvisi, dihitung dari
  * {@code max(versionNumber)+1} per kombinasi jurnal+kunci+lokal). Baris versi lama tidak
  * dihapus/ditimpa — hanya baris dengan {@code aktif=true} dan versi tertinggi yang dipakai
  * saat pengiriman, sehingga riwayat revisi template tetap tersedia untuk audit.
  */
 @Column(name="version_number",nullable=false) public Integer getVersionNumber(){return versionNumber;} public void setVersionNumber(Integer v){versionNumber=v;}
}
