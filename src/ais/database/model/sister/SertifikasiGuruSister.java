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
 * Entitas hasil sinkronisasi SISTER untuk data <b>sertifikasi guru (paralel dosen; diisi modul guru bila tersedia)</b>.
 * Kolom {@code kode} = id item di SISTER (dipakai kunci upsert); {@code keterangan} menyimpan JSON
 * mentah lengkap agar tidak ada data yang hilang; kolom bernama lain memetakan field penting agar
 * mudah di-query. Kolom dibuat otomatis via hbm2ddl (skema public). @Audited: WAJIB ALTER tabel
 * __audit di InitIndex.java.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sertifikasi_guru_sister")
public class SertifikasiGuruSister extends GeneralValueObject {

	/** Versi serialisasi. */
	private static final long serialVersionUID = 1L;
	/** ID baris lokal (surrogate key, auto increment; bukan id SISTER). */
	private Long id;
	/** Nama pengguna aplikasi yang melakukan perubahan terakhir (field audit shadow, diisi ulang oleh {@code onUpdate}). */
	private String oleh;
	/** ID pengguna aplikasi yang melakukan perubahan terakhir (field audit shadow). */
	private String olehId;
	/** Timestamp perubahan terakhir baris ini (field audit shadow); default saat objek dibuat, diperbarui via {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/** Kode/id item sertifikasi guru pada SISTER — kunci upsert sinkronisasi (bukan PK lokal). */
	private String kode;
	/** Salinan JSON mentah respons SISTER untuk item ini (cadangan lengkap agar tidak ada data yang hilang). */
	private String keterangan;
	/** Penanda baris aktif; {@code null} diperlakukan sebagai aktif, lihat {@link #getAktif()}. */
	private Boolean aktif;
	/**
	 * id guru pemilik sertifikasi ini. CATATAN: kolom ini TIDAK PERNAH DIISI oleh mesin sinkronisasi
	 * ({@link ais.common.DataSisterApi}) — kelas ini di-map di {@code hibernate.cfg.xml} (tabel dibuat)
	 * tetapi tidak ada satu pun jalur kode yang memanggil konstruktor/setter-nya; entitas dorman,
	 * disiapkan untuk modul guru yang belum diaktifkan (lihat javadoc kelas).
	 */
	private String idGuru;
	/** Jenis sertifikasi guru (mis. sertifikasi pendidik). */
	private String jenisSertifikasi;
	/** Bidang studi sertifikasi guru bersangkutan. */
	private String bidangStudi;
	/** Tahun sertifikasi guru bersangkutan diperoleh. */
	private String tahunSertifikasi;
	/** Nomor registrasi sertifikasi guru. */
	private String nomorRegistrasi;

	/** Konstruktor kosong (dibutuhkan Hibernate). */
	public SertifikasiGuruSister() {
	}

	/** @return ID baris lokal (surrogate key). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris lokal baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna aplikasi yang terakhir mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/** Menyetel ID pengguna pengubah; abai (no-op) bila kosong/blank agar nilai lama tak tertimpa kosong. @param olehId ID pengguna baru. */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) { return; }
		this.olehId = olehId;
	}

	/** @return nama pengguna aplikasi yang terakhir mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/** Menyetel nama pengguna pengubah; abai (no-op) bila kosong/blank agar nilai lama tak tertimpa kosong. @param oleh nama pengguna baru. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) { return; }
		this.oleh = oleh;
	}

	/** Callback JPA {@code @PreUpdate}: mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} untuk memperbarui {@code oleh}/{@code olehId}/{@code tanggal_dirubah}. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah timestamp perubahan baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode/id item SISTER, di-trim; {@code null} bila kosong. */
	@Column(name = "kode")
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	/** @param kode kode/id item SISTER baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return salinan JSON mentah respons SISTER untuk item ini. */
	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan JSON mentah baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return status aktif; {@code true} bila belum pernah diisi ({@code null}). */
	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baru. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return id guru pemilik sertifikasi ini; lihat catatan dorman pada field {@link #idGuru}. */
	@Column(name = "id_guru")
	public String getIdGuru() {
		return idGuru;
	}

	/** @param idGuru id guru baru. */
	public void setIdGuru(String idGuru) {
		this.idGuru = idGuru;
	}

	/** @return jenis sertifikasi guru. */
	@Column(name = "jenis_sertifikasi")
	public String getJenisSertifikasi() {
		return jenisSertifikasi;
	}

	/** @param jenisSertifikasi jenis sertifikasi baru. */
	public void setJenisSertifikasi(String jenisSertifikasi) {
		this.jenisSertifikasi = jenisSertifikasi;
	}

	/** @return bidang studi sertifikasi guru. */
	@Column(name = "bidang_studi")
	public String getBidangStudi() {
		return bidangStudi;
	}

	/** @param bidangStudi bidang studi baru. */
	public void setBidangStudi(String bidangStudi) {
		this.bidangStudi = bidangStudi;
	}

	/** @return tahun sertifikasi guru diperoleh. */
	@Column(name = "tahun_sertifikasi")
	public String getTahunSertifikasi() {
		return tahunSertifikasi;
	}

	/** @param tahunSertifikasi tahun sertifikasi baru. */
	public void setTahunSertifikasi(String tahunSertifikasi) {
		this.tahunSertifikasi = tahunSertifikasi;
	}

	/** @return nomor registrasi sertifikasi guru. */
	@Column(name = "nomor_registrasi")
	public String getNomorRegistrasi() {
		return nomorRegistrasi;
	}

	/** @param nomorRegistrasi nomor registrasi baru. */
	public void setNomorRegistrasi(String nomorRegistrasi) {
		this.nomorRegistrasi = nomorRegistrasi;
	}

	/** @return representasi ringkas "id-kode" untuk log/debug. */
	@Override
	public String toString() {
		return id + "-" + kode;
	}
}
