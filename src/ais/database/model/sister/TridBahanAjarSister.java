package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.hibernate.envers.Audited;
import ais.database.model.GeneralValueObject;

/**
 * Entitas hasil sinkronisasi SISTER untuk endpoint <b>trid/bahan_ajar</b>. Kolom bernama = field SISTER; {@code kode}=id item (kunci upsert); {@code keterangan}=JSON mentah. @Audited (tabel __audit auto hbm2ddl).
 *
 * <p>Memetakan tabel fisik {@code sister_trid_bahan_ajar}. Bagian dari klaster {@code Trid*Sister} (data TRIDHARMA —
 * tiga pilar tugas dosen: pendidikan/pengajaran, penelitian, pengabdian — beserta unsur penunjangnya) yang
 * disinkronkan dari <b>SISTER Kemdikbud</b> (Sistem Informasi Sumber Daya Terintegrasi) melalui
 * {@link ais.common.DataSisterApi}. Kelas ini terdaftar pada {@link SisterEntitasRegistry} dengan kunci
 * {@code "bahan_ajar"} sehingga proses sinkron/upsert generik di {@code DataSisterApi} dapat memetakan payload
 * JSON endpoint SISTER ke tabel fisik ini berdasarkan nama tabel simpan.</p>
 *
 * <p>Kelas-kelas {@code Trid*Sister} berbagi bentuk identik: field administratif yang sama (lihat javadoc tiap
 * field di bawah) ditambah beberapa field spesifik-domain yang namanya mengikuti field JSON dari SISTER apa
 * adanya (tanpa transformasi/normalisasi selain trim pada {@code kode}). Kelas ini dipakai sebagai <b>rujukan</b>
 * untuk pola tersebut pada klaster bagian 2 (aktivitas pengajaran/publikasi/tunjangan); kelas saudara memakai
 * javadoc yang lebih ringkas dengan {@code @see} balik ke sini untuk anggota administratif yang identik.</p>
 *
 * @see ais.database.model.sister.SisterEntitasRegistry
 * @see ais.common.DataSisterApi
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_trid_bahan_ajar")
public class TridBahanAjarSister extends GeneralValueObject {
	/** Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deploy. */
	private static final long serialVersionUID = 1L;
	/** Primary key auto-increment tabel fisik; lihat {@link #getId()}. */
	private Long id;
	/** Nama/label pengguna aplikasi yang memicu operasi simpan terakhir (diisi sisi klien, bukan dari payload SISTER). */
	private String oleh;
	/** Id pengguna aplikasi yang memicu operasi simpan terakhir (diisi sisi klien, bukan dari payload SISTER). */
	private String olehId;
	/** Timestamp perubahan terakhir; diinisialisasi ke waktu construction lalu diperbarui otomatis oleh {@link #onUpdate()} pada tiap {@code UPDATE}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Id item pada sisi SISTER (bukan {@link #id} lokal) — kunci alami yang dipakai proses upsert di {@code DataSisterApi} untuk mencocokkan baris lama vs baru per halaman sinkron. */
	private String kode;
	/** Payload JSON mentah dari endpoint SISTER untuk item ini, disimpan apa adanya sebagai cadangan/audit, terpisah dari kolom-kolom bernama yang sudah diekstrak di bawah. */
	private String keterangan;
	/** Penanda status aktif/nonaktif item hasil sinkron; lihat {@link #getAktif()} untuk perilaku default saat null. */
	private Boolean aktif;
	/** Id SDM (dosen) pemilik item ini pada sisi SISTER; dipakai sebagai kunci korelasi ke identitas dosen, disimpan sebagai teks (bukan FK terkelola Hibernate). */
	private String idSdm;
	/** Judul bahan ajar sebagaimana dilaporkan ke SISTER. */
	private String judul;
	/** Nomor ISBN bahan ajar (teks bebas, tidak divalidasi format). */
	private String isbn;
	/** Nama jenis/kategori bahan ajar (mis. buku ajar, modul, diktat) sesuai referensi SISTER. */
	private String namaJenis;
	/** Nama penerbit bahan ajar. */
	private String namaPenerbit;
	/** Tanggal terbit bahan ajar, disimpan sebagai teks mentah dari SISTER (bukan tipe tanggal asli). */
	private String tanggalTerbit;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public TridBahanAjarSister() {}
	/** @return {@link #id}. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id nilai {@link #id} baru (umumnya diisi ORM, jarang dipanggil manual). */
	public void setId(Long id) { this.id = id; }
	/** @return {@link #olehId}. */
	public String getOlehId() { return olehId; }
	/** Mengisi {@link #olehId}; mengabaikan (no-op) nilai null/kosong agar nilai lama yang sudah tersimpan tidak tertimpa kosong secara tidak sengaja. @param olehId id pengguna pemicu simpan. */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/** @return {@link #oleh}. */
	public String getOleh() { return oleh; }
	/** Mengisi {@link #oleh}; mengabaikan (no-op) nilai null/kosong, alasan sama seperti {@link #setOlehId(String)}. @param oleh nama pengguna pemicu simpan. */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** Hook {@code @PreUpdate}: mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui {@link #tanggal_dirubah} otomatis setiap kali baris di-UPDATE. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/** @return {@link #tanggal_dirubah}. */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param t nilai {@link #tanggal_dirubah} baru. */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/** @return {@link #kode} setelah di-trim, atau {@code null} bila kosong/blank — menormalkan agar pemanggil tidak perlu mengecek string kosong terpisah dari null. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode nilai {@link #kode} baru (id item SISTER); tidak divalidasi/di-trim di setter, hanya di getter. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return {@link #keterangan}. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k nilai {@link #keterangan} baru (JSON mentah). */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return {@link #aktif}, atau {@code true} bila belum diisi (default optimistik: item baru dianggap aktif sampai dinyatakan sebaliknya oleh proses sinkron). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a nilai {@link #aktif} baru; {@code null} diperbolehkan (akan dibaca sebagai aktif oleh getter). */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return {@link #idSdm}. */
	@Column(name = "id_sdm", columnDefinition = "text") public String getIdSdm() { return idSdm; }
	/** @param v nilai {@link #idSdm} baru. */
	public void setIdSdm(String v) { this.idSdm = v; }
	/** @return {@link #judul}. */
	@Column(name = "judul", columnDefinition = "text") public String getJudul() { return judul; }
	/** @param v nilai {@link #judul} baru. */
	public void setJudul(String v) { this.judul = v; }
	/** @return {@link #isbn}. */
	@Column(name = "isbn", columnDefinition = "text") public String getIsbn() { return isbn; }
	/** @param v nilai {@link #isbn} baru. */
	public void setIsbn(String v) { this.isbn = v; }
	/** @return {@link #namaJenis}. */
	@Column(name = "nama_jenis", columnDefinition = "text") public String getNamaJenis() { return namaJenis; }
	/** @param v nilai {@link #namaJenis} baru. */
	public void setNamaJenis(String v) { this.namaJenis = v; }
	/** @return {@link #namaPenerbit}. */
	@Column(name = "nama_penerbit", columnDefinition = "text") public String getNamaPenerbit() { return namaPenerbit; }
	/** @param v nilai {@link #namaPenerbit} baru. */
	public void setNamaPenerbit(String v) { this.namaPenerbit = v; }
	/** @return {@link #tanggalTerbit}. */
	@Column(name = "tanggal_terbit", columnDefinition = "text") public String getTanggalTerbit() { return tanggalTerbit; }
	/** @param v nilai {@link #tanggalTerbit} baru. */
	public void setTanggalTerbit(String v) { this.tanggalTerbit = v; }
	/** @return representasi ringkas {@code "<id>-<kode>"}, dipakai untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
