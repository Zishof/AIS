package ais.database.model.jurnal;
import javax.persistence.*;
/**
 * Entitas Hibernate untuk tabel {@code penelitiandanpengabdian.import_sumber_ojs} — profil
 * satu instalasi/sumber OJS (Open Journal Systems) eksternal yang terdaftar sebagai sumber
 * impor data jurnal ke AIS. Satu baris di sini dapat menjadi asal banyak {@link ImportJobOjs}
 * (tautan lewat id mentah {@code sourceId}, bukan relasi Hibernate terpetakan).
 *
 * <p>
 * Menyimpan metadata teknis yang diperlukan untuk memvalidasi kompatibilitas sebelum impor:
 * versi OJS ({@link #getOjsVersion()}), dialek basis data ({@link #getDialect()}), dan tanda
 * tangan skema sumber ({@link #getSchemaSignature()}) untuk mendeteksi perubahan struktur
 * tabel OJS. {@link #getConnectionReference()} menunjuk ke konfigurasi koneksi (bukan kredensial
 * itu sendiri, dilihat dari nama kolomnya).
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_sumber_ojs")
public class ImportSumberOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String sourceKey,displayName,ojsVersion,dialect,schemaSignature,connectionReference,status;
 /** Kunci unik sumber impor (kode identifikasi internal). */
 @Column(name="source_key",nullable=false,length=120) public String getSourceKey(){return sourceKey;} public void setSourceKey(String v){sourceKey=v;}
 /** Nama tampilan sumber (untuk ditampilkan di UI). */
 @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
 /** Versi perangkat lunak OJS pada instalasi sumber. */
 @Column(name="ojs_version",nullable=false,length=40) public String getOjsVersion(){return ojsVersion;} public void setOjsVersion(String v){ojsVersion=v;}
 /** Dialek basis data sumber (mis. "MYSQL", "POSTGRESQL"). */
 @Column(name="dialect",nullable=false,length=40) public String getDialect(){return dialect;} public void setDialect(String v){dialect=v;}
 /** Tanda tangan/hash skema tabel sumber, dipakai mendeteksi perubahan struktur sebelum impor. */
 @Column(name="schema_signature",nullable=false,length=128) public String getSchemaSignature(){return schemaSignature;} public void setSchemaSignature(String v){schemaSignature=v;}
 /** Referensi ke konfigurasi koneksi menuju basis data sumber (bukan kredensial langsung). */
 @Column(name="connection_reference",nullable=false,length=255) public String getConnectionReference(){return connectionReference;} public void setConnectionReference(String v){connectionReference=v;}
 /** Status keaktifan/ketersediaan sumber ini untuk dipakai dalam pekerjaan impor baru. */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
