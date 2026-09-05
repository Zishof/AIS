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
 * Entitas hasil sinkronisasi SISTER untuk endpoint referensi <b>{@code referensi/sdm}</b> — mirror data
 * <b>SDM (Sumber Daya Manusia) INDIVIDUAL</b> (dosen/tenaga kependidikan terdaftar di SISTER), BUKAN kode
 * referensi/lookup murni seperti 32+3 kelas {@code Ref*Sister} lain di paket ini. Sesuai catatan verifikasi
 * tugas ini: kelas ini (bersama {@link RefMahasiswaPddiktiSister}) memang berpola berbeda — satu baris per
 * orang, dengan kolom identitas personal (NIDN/NIP/NUPTK) alih-alih hanya kode+nama kategori.
 * <p>
 * <b>Peran kunci sebagai SEED sinkronisasi lanjutan.</b> Berbeda dari kelas {@code Ref*Sister} lain yang
 * murni tabel tampilan, {@link #kode} (=id_sdm SISTER) di kelas ini dipakai ULANG secara aktif oleh
 * {@code ais.common.DataSisterApi} sebagai parameter query {@code id_sdm} untuk seluruh "ORKESTRASI 2: DATA
 * DOSEN / SDM & TRIDHARMA" — yaitu endpoint {@code data_pribadi/*} (profil, kependudukan, keluarga, alamat,
 * kepegawaian, dst — lihat {@code DpXxxSister}), endpoint Tridharma (pengajaran, penelitian, publikasi, dst —
 * lihat {@code TridXxxSister}), dan endpoint BKD (Beban Kerja Dosen — lihat {@code BkdXxxSister}). Karena itu
 * sinkronisasi {@code referensi/sdm} WAJIB berjalan dan berhasil LEBIH DULU sebelum data pribadi/Tridharma
 * per-dosen bisa ditarik — {@code DataSisterApi.ambilDaftarIdSdm()} membaca distinct {@link #kode} dari tabel
 * ini via Hibernate Criteria; daftar kosong berarti data SDM belum tersinkron sama sekali.
 * <p>
 * {@code DataSisterApi.ambilDaftarDosen()} juga mencocokkan {@link #nidn} baris ini dengan entitas
 * {@code Dosen} e-Campus lokal (by NIDN) untuk melengkapi prodi/fakultas pada dialog pemilihan dosen di UI
 * (SISTER sendiri tidak menyertakan prodi/fakultas pada daftar SDM) — sinkronisasi Tridharma dapat dijalankan
 * bertahap per-dosen/per-batch lewat dialog tersebut, bukan wajib sekaligus semua dosen.
 * <p>
 * Struktur kolom inti ({@link #id}, {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} shadow audit,
 * {@link #kode}, {@link #keterangan}, {@link #aktif}) serta mekanisme upsert refleksi generik dan default
 * aktif=true — sama seperti kelas pola {@link RefAgamaSister}; lihat javadoc di sana untuk penjelasan lengkap
 * mesin sinkronisasi. TUJUH kolom di kelas ini murni spesifik profil SDM: {@link #namaSdm}, {@link #nidn},
 * {@link #nip}, {@link #nuptk}, {@link #namaStatusAktif}, {@link #namaStatusPegawai}, {@link #jenisSdm}.
 * Dipetakan di {@link SisterEntitasRegistry} pada key {@code "referensi/sdm"}. {@code @Audited} (tabel
 * bayangan {@code sister_ref_sdm_AUD} otomatis via hbm2ddl).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sister_ref_sdm")
public class RefSdmSister extends GeneralValueObject {
	private static final long serialVersionUID = 1L;
	/** PK lokal (identity DB), BUKAN kode SISTER — lihat {@link #kode}. */
	private Long id;
	/** Nama aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOleh}). */
	private String oleh;
	/** ID aktor yang terakhir mengubah baris (field audit shadow, diisi via {@link #setOlehId}). */
	private String olehId;
	/** Timestamp perubahan terakhir; diinisialisasi saat objek dibuat, dimutakhirkan oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id_sdm SISTER (field JSON {@code "id"}) — kunci upsert sinkronisasi SEKALIGUS seed parameter {@code id_sdm} untuk seluruh sinkronisasi data pribadi/Tridharma/BKD per-dosen di {@code DataSisterApi}. */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk baris SDM ini; dipakai ulang oleh {@code DataSisterApi.ambilDaftarDosen()} untuk mengekstrak nama/nidn/jenis_sdm pada dialog pemilihan dosen. */
	private String keterangan;
	/** Flag aktif dari SISTER; {@code null} diperlakukan aktif oleh {@link #getAktif()}. */
	private Boolean aktif;
	/** Nama SDM (field JSON {@code "nama_sdm"}) — kolom spesifik kelas ini. */
	private String namaSdm;
	/** NIDN (Nomor Induk Dosen Nasional), dipakai {@code DataSisterApi.ambilDaftarDosen()} untuk mencocokkan ke entitas {@code Dosen} lokal demi melengkapi prodi/fakultas (field JSON {@code "nidn"}). */
	private String nidn;
	/** NIP (Nomor Induk Pegawai) SDM (field JSON {@code "nip"}) — kolom spesifik kelas ini. */
	private String nip;
	/** NUPTK (Nomor Unik Pendidik dan Tenaga Kependidikan) SDM (field JSON {@code "nuptk"}) — kolom spesifik kelas ini. */
	private String nuptk;
	/** Nama status aktif SDM pada SISTER (field JSON {@code "nama_status_aktif"}) — kolom spesifik kelas ini. */
	private String namaStatusAktif;
	/** Nama status pegawai SDM pada SISTER (field JSON {@code "nama_status_pegawai"}) — kolom spesifik kelas ini. */
	private String namaStatusPegawai;
	/** Jenis SDM (field JSON {@code "jenis_sdm"}, mis. dosen/tenaga kependidikan) — kolom spesifik kelas ini. */
	private String jenisSdm;

	/** Constructor default (kontrak JPA/Hibernate — instansiasi via refleksi). */
	public RefSdmSister() {}
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
	/** @return kode/id_sdm SISTER, di-trim; string kosong dinormalisasi menjadi {@code null}. */
	@Column(name = "kode") public String getKode() { return kode==null||kode.isEmpty()?null:kode.trim(); }
	/** @param kode kode/id_sdm SISTER baru (kunci upsert + seed sinkronisasi lanjutan); TIDAK di-trim di setter, hanya di getter. */
	public void setKode(String kode) { this.kode = kode; }
	/** @return JSON mentah baris SDM ini. */
	@Column(name = "keterangan", columnDefinition = "text") public String getKeterangan() { return keterangan; }
	/** @param k JSON mentah baru (biasanya {@code JSONObject.toString()} dari respons SISTER). */
	public void setKeterangan(String k) { this.keterangan = k; }
	/** @return status aktif; {@code null} tersimpan diperlakukan sebagai {@code true} (default aktif). */
	@Column(name = "aktif") public Boolean getAktif() { return aktif==null?true:aktif; }
	/** @param a status aktif baru dari SISTER. */
	public void setAktif(Boolean a) { this.aktif = a; }
	/** @return nama SDM. */
	@Column(name = "nama_sdm", columnDefinition = "text") public String getNamaSdm() { return namaSdm; }
	/** @param v nama SDM baru. */
	public void setNamaSdm(String v) { this.namaSdm = v; }
	/** @return NIDN (Nomor Induk Dosen Nasional). */
	@Column(name = "nidn", columnDefinition = "text") public String getNidn() { return nidn; }
	/** @param v NIDN baru. */
	public void setNidn(String v) { this.nidn = v; }
	/** @return NIP (Nomor Induk Pegawai). */
	@Column(name = "nip", columnDefinition = "text") public String getNip() { return nip; }
	/** @param v NIP baru. */
	public void setNip(String v) { this.nip = v; }
	/** @return NUPTK (Nomor Unik Pendidik dan Tenaga Kependidikan). */
	@Column(name = "nuptk", columnDefinition = "text") public String getNuptk() { return nuptk; }
	/** @param v NUPTK baru. */
	public void setNuptk(String v) { this.nuptk = v; }
	/** @return nama status aktif SDM pada SISTER. */
	@Column(name = "nama_status_aktif", columnDefinition = "text") public String getNamaStatusAktif() { return namaStatusAktif; }
	/** @param v nama status aktif baru. */
	public void setNamaStatusAktif(String v) { this.namaStatusAktif = v; }
	/** @return nama status pegawai SDM pada SISTER. */
	@Column(name = "nama_status_pegawai", columnDefinition = "text") public String getNamaStatusPegawai() { return namaStatusPegawai; }
	/** @param v nama status pegawai baru. */
	public void setNamaStatusPegawai(String v) { this.namaStatusPegawai = v; }
	/** @return jenis SDM (mis. dosen/tenaga kependidikan). */
	@Column(name = "jenis_sdm", columnDefinition = "text") public String getJenisSdm() { return jenisSdm; }
	/** @param v jenis SDM baru. */
	public void setJenisSdm(String v) { this.jenisSdm = v; }
	/** @return representasi ringkas {@code id-kode} untuk log/debug. */
	@Override public String toString() { return id + "-" + kode; }
}
