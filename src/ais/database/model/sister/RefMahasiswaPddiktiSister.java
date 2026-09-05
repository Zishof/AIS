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
 * Entitas hasil sinkronisasi SISTER untuk endpoint referensi <b>{@code referensi/mahasiswa_pddikti}</b> —
 * mirror data <b>MAHASISWA INDIVIDUAL</b> dari PDDIKTI (Pangkalan Data Pendidikan Tinggi Kemdikbudristek),
 * BUKAN kode referensi/lookup murni seperti 32+3 kelas {@code Ref*Sister} lain di paket ini. Sesuai catatan
 * verifikasi tugas ini: kelas ini (bersama {@link RefSdmSister}) memang berpola berbeda — menyimpan satu
 * baris per orang (di sini per mahasiswa), bukan satu baris per kode kategori.
 * <p>
 * Ditarik per program studi: {@code DataSisterApi.daftarEndpoint(PerguruanTinggi, List)} melakukan iterasi
 * atas seluruh {@link ais.database.model.akademik.Jurusan} aktif milik perguruan tinggi berjalan dan menyusun
 * URL {@code referensi/mahasiswa_pddikti?id_perguruan_tinggi=<feeder_pt>&id_program_studi=<feeder_jurusan>}
 * untuk tiap prodi — SATU array JSON per prodi, semuanya diupsert ke tabel yang SAMA ini (tak dipisah per
 * prodi). Karena itu {@link #kode} (id mahasiswa PDDIKTI) adalah kunci upsert LINTAS PRODI: baris tak
 * otomatis terhapus bila mahasiswa pindah/lulus dari satu prodi kecuali disinkron ulang.
 * <p>
 * Struktur kolom inti ({@link #id}, {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} shadow audit,
 * {@link #kode}, {@link #keterangan}, {@link #aktif}) serta mekanisme upsert refleksi generik
 * ({@code DataSisterApi}) dan default aktif=true — sama seperti kelas pola {@link RefAgamaSister}; lihat
 * javadoc di sana untuk penjelasan lengkap mesin sinkronisasi. TIGA kolom di kelas ini murni spesifik
 * mahasiswa (bukan field umum "nama" referensi): {@link #idMahasiswa}, {@link #namaMahasiswa}, {@link #nim}.
 * Dipetakan di {@link SisterEntitasRegistry} pada key {@code "referensi/mahasiswa_pddikti"}.
 * {@code @Audited} (tabel bayangan {@code sister_ref_mahasiswa_pddikti_AUD} otomatis via hbm2ddl).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_mahasiswa_pddikti")
public class RefMahasiswaPddiktiSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	/** PK lokal (identity DB), BUKAN kode SISTER — lihat {@link #kode}. */
	private Long id;
	/** Nama aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOleh}). */
	private String oleh;
	/** ID aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOlehId}). */
	private String olehId;
	/** Timestamp perubahan terakhir; diinisialisasi saat objek dibuat, dimutakhirkan oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id item SISTER (field JSON {@code "id"}) — kunci upsert sinkronisasi, LINTAS prodi (lihat javadoc kelas). */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk baris mahasiswa ini. */
	private String keterangan;
	/** Flag aktif dari SISTER; {@code null} diperlakukan aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** ID mahasiswa pada PDDIKTI (field JSON {@code "id_mahasiswa"}) — kolom spesifik kelas ini. */
	private String idMahasiswa;
	/** Nama mahasiswa (field JSON {@code "nama_mahasiswa"}) — kolom spesifik kelas ini. */
	private String namaMahasiswa;
	/** NIM (Nomor Induk Mahasiswa) pada PDDIKTI (field JSON {@code "nim"}) — kolom spesifik kelas ini. */
	private String nim;

	/** Constructor default (kontrak JPA/Hibernate — instansiasi via refleksi). */
	public RefMahasiswaPddiktiSister() {}
	/** @return PK lokal (identity DB), bukan kode SISTER. */
	@Id @GeneratedValue(strategy = IDENTITY) @Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/** @param id PK lokal; normalnya diisi Hibernate saat insert, bukan diset manual. */
	public void setId(Long id) { this.id = id; }
	/** @return id aktor pengubah terakhir. */
	public String getOlehId() { return olehId; }
	/**
	 * Menetapkan id aktor pengubah. Mengabaikan diam-diam nilai null/kosong (nilai lama dipertahankan) —
	 * pola guard field audit shadow yang berulang di seluruh klaster entitas SISTER, BUKAN bug.
	 * @param olehId id aktor baru; diabaikan bila null/blank.
	 */
	public void setOlehId(String olehId) { if (olehId==null||olehId.trim().isEmpty()) return; this.olehId = olehId; }
	/** @return nama aktor pengubah terakhir. */
	public String getOleh() { return oleh; }
	/**
	 * Menetapkan nama aktor pengubah. Mengabaikan diam-diam nilai null/kosong, sama seperti {@link #setOlehId}.
	 * @param oleh nama aktor baru; diabaikan bila null/blank.
	 */
	public void setOleh(String oleh) { if (oleh==null||oleh.trim().isEmpty()) return; this.oleh = oleh; }
	/** Callback {@code @PreUpdate}: mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah} otomatis pada setiap UPDATE. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }
	/** @return timestamp perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }
	/** @param t timestamp baru (biasanya diisi otomatis via {@link #onUpdate()}, bukan manual). */
	public void setTanggal_dirubah(Date t) { this.tanggal_dirubah = t; }
	/** @return kode SISTER, di-trim; string kosong dinormalisasi menjadi {@code null}. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode kode/id item SISTER baru (kunci upsert); TIDAK di-trim di setter, hanya di getter. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return JSON mentah baris ini. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k JSON mentah baru (biasanya {@code JSONObject.toString()} dari respons SISTER). */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return status aktif; {@code null} tersimpan diperlakukan sebagai {@code true} (default aktif). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a status aktif baru dari SISTER. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return id mahasiswa pada PDDIKTI. */
	@Column(name = "id_mahasiswa", columnDefinition = "text") public String getIdMahasiswa() { return idMahasiswa; }
	/** @param v id mahasiswa baru. */
	public void setIdMahasiswa(String v) { this.idMahasiswa = v; }
	/** @return nama mahasiswa. */
	@Column(name = "nama_mahasiswa", columnDefinition = "text") public String getNamaMahasiswa() { return namaMahasiswa; }
	/** @param v nama mahasiswa baru. */
	public void setNamaMahasiswa(String v) { this.namaMahasiswa = v; }
	/** @return NIM (Nomor Induk Mahasiswa) pada PDDIKTI. */
	@Column(name = "nim", columnDefinition = "text") public String getNim() { return nim; }
	/** @param v NIM baru. */
	public void setNim(String v) { this.nim = v; }
	/** @return representasi ringkas {@code id-kode} untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
