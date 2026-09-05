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
 *
 * <p>
 * <b>Catatan keamanan (terverifikasi):</b> {@link #getConnectionReference()} HANYA menyimpan
 * kunci simbolik pendek (divalidasi regex {@code [A-Z][A-Z0-9_]{1,39}} oleh
 * {@code ais.action.master.jurnal.importer.OjsConnectionRegistry}), BUKAN JDBC URL/username/
 * password mentah. Kredensial koneksi basis data OJS sumber yang sesungguhnya (JDBC URL, user,
 * password) diselesaikan {@code OjsConnectionRegistry.resolve(...)} semata-mata dari environment
 * variable deployment (pola nama {@code AIS_JURNAL_OJS_<REF>_JDBC_URL/_USER/_PASSWORD}) —
 * per Javadoc kelas tersebut secara eksplisit: "Resolves named OJS source connections from
 * deployment secrets, never HTTP payloads or database rows." Dengan demikian tabel ini (dan
 * baris entitas ini) TIDAK PERNAH menyimpan kredensial mentah dan tidak menjadi kandidat
 * kerentanan penyimpanan-kredensial-plaintext seperti pola yang sudah tercatat di modul lain.
 * Field ini juga hanya dapat diisi/dibaca lewat operasi bergerbang
 * {@code auth.requireWorkflow(actor,"manageImport")} + {@code requireJournalScope(...)} pada
 * {@code OjsImportExecutionService}/{@code JurnalAdminApi} — bukan endpoint publik.
 * </p>
 */
@Entity @Table(schema="penelitiandanpengabdian",name="import_sumber_ojs")
public class ImportSumberOjs extends JurnalEntityBase {
 private static final long serialVersionUID=1L; private String sourceKey,displayName,ojsVersion,dialect,schemaSignature,connectionReference,status;
 /** Kunci unik sumber impor (kode identifikasi internal), unik per kombinasi tenant+sumber (lihat query pencarian di {@code OjsImportExecutionService.registerSource}). */
 @Column(name="source_key",nullable=false,length=120) public String getSourceKey(){return sourceKey;} public void setSourceKey(String v){sourceKey=v;}
 /** Nama tampilan sumber (untuk ditampilkan di UI admin impor), bebas diisi operator — tidak dipakai untuk logika/keamanan apa pun. */
 @Column(name="display_name",nullable=false,length=255) public String getDisplayName(){return displayName;} public void setDisplayName(String v){displayName=v;}
 /** Versi perangkat lunak OJS pada instalasi sumber, dideteksi otomatis lewat pre-flight inspeksi skema (bukan diisi manual oleh operator). */
 @Column(name="ojs_version",nullable=false,length=40) public String getOjsVersion(){return ojsVersion;} public void setOjsVersion(String v){ojsVersion=v;}
 /** Dialek basis data sumber (mis. "MYSQL", "POSTGRESQL"), dideteksi otomatis lewat pre-flight inspeksi, dipakai memilih dialek query yang sesuai saat membaca tabel sumber. */
 @Column(name="dialect",nullable=false,length=40) public String getDialect(){return dialect;} public void setDialect(String v){dialect=v;}
 /** Tanda tangan/hash skema tabel sumber, dipakai mendeteksi perubahan struktur sebelum impor dijalankan ulang (mis. kolom baru/hilang pada instalasi OJS sumber). */
 @Column(name="schema_signature",nullable=false,length=128) public String getSchemaSignature(){return schemaSignature;} public void setSchemaSignature(String v){schemaSignature=v;}
 /**
  * Referensi (bukan kredensial langsung) ke konfigurasi koneksi menuju basis data sumber —
  * kunci simbolik pendek yang divalidasi dan diselesaikan menjadi JDBC URL/user/password oleh
  * {@code OjsConnectionRegistry} SEMATA-MATA dari environment variable deployment, tidak
  * pernah dari payload HTTP maupun baris database. Lihat catatan keamanan pada Javadoc kelas.
  */
 @Column(name="connection_reference",nullable=false,length=255) public String getConnectionReference(){return connectionReference;} public void setConnectionReference(String v){connectionReference=v;}
 /** Status keaktifan/ketersediaan sumber ini untuk dipakai dalam pekerjaan impor baru (mis. "READY", "READY_WITH_GAPS" bila pre-flight menemukan tabel/field OJS yang tidak lengkap). */
 @Column(name="status",nullable=false,length=30) public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
