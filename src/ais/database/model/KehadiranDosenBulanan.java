package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.ui.util.WaktuUtil;

/**
 * Model data untuk rekap kehadiran mengajar SEORANG dosen dalam SATU bulan tertentu (agregat
 * bulanan, bukan catatan presensi per hari). Menyimpan jumlah kelas/mata kuliah yang diampu,
 * jumlah pertemuan masuk, beban SKS (termasuk versi "pecahan" untuk pembagian proporsional),
 * serta rentang tanggal periode rekap. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Integer bulan}, {@code Integer tahun}, {@code Long dosen},
 * {@code Integer sks}, {@code Date tanggalMulai}, {@code Date tanggalSampai}; pemetaan persistence: tabel
 * {@code public.kehadiran_dosen_bulanan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getBulan()}, {@code getDosen()}, {@code getSkspecahan()});
 * mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setBulan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Catatan relasi:</b> {@link #getDosen()} berupa {@code Long} MENTAH (id dosen), bukan {@code @ManyToOne}
 * ke {@link Dosen} -- berbeda dari kebanyakan entity sejenis di modul akademik yang memetakan relasi penuh;
 * pemanggil harus me-resolve id ini sendiri lewat DAO {@code Dosen} bila butuh objeknya.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see KehadiranPegawaiBulanan versi rekap kehadiran bulanan untuk pegawai non-dosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kehadiran_dosen_bulanan")
public class KehadiranDosenBulanan extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
	 */
	private String oleh;
	/** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
	private String olehId;

	/**
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
	 * @see GeneralValueObject#getOlehId()
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
	public String toString() {
		return id + "-" + nama;
	}

	/** Bulan rekap (1-12). */
	private Integer bulan;
	/** Tahun rekap. */
	private Integer tahun;

	/** Id dosen pemilik rekap ini (relasi MENTAH, lihat catatan kelas). */
	private Long dosen;
	/** Nama tampilan baris rekap. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;

	/** SKS "pecahan" (proporsional, bisa desimal); lihat {@link #getSkspecahan()}. */
	private Double skspecahan;
	/** SKS pecahan versi per-mata-kuliah. */
	private Double skspecahanmk;
	/** Total SKS bulan ini (bulat). */
	private Integer sks;
	/** Jumlah hari mengajar dalam bulan ini. */
	private Integer hr;
	/** Jumlah kelas yang diampu dalam bulan ini. */
	private Integer jmlKelas;
	/** Jumlah mata kuliah yang diampu dalam bulan ini. */
	private Integer jmlMk;
	/** Jumlah pertemuan dengan kehadiran "masuk" tercatat. */
	private Integer masuk;
	/** Total SKS terakumulasi (berbeda konteks dari {@link #sks} bulan berjalan). */
	private Integer sksTotal;

	/** Tanggal mulai periode rekap. */
	private Date tanggalMulai;
	/** Tanggal akhir periode rekap. */
	private Date tanggalSampai;

	/** Konstruktor kosong, dipakai Hibernate. */
	public KehadiranDosenBulanan() {
	}

	/** @return primary key entity, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama tampilan baris rekap, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan bebas yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return bulan rekap (1-12). */
	@Column(name = "bulan", nullable = false)
	public Integer getBulan() {
		return bulan;
	}

	/** @param bulan bulan rekap baru. */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/** @return tahun rekap. */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	/** @param tahun tahun rekap baru. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/** @return id dosen pemilik rekap (relasi MENTAH, bukan {@code @ManyToOne} -- lihat catatan kelas). */
	@Column(name = "dosen", nullable = false)
	public Long getDosen() {
		return dosen;
	}

	/** @param dosen id dosen pemilik rekap yang baru. */
	public void setDosen(Long dosen) {
		this.dosen = dosen;
	}

	/** @return total SKS bulan ini, {@code 0} bila belum diisi (bukan {@code null}). */
	public Integer getSks() {
		return sks == null ? 0 : sks;
	}

	/** @param sks total SKS baru. */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

	/** @return jumlah hari mengajar, {@code 0} bila belum diisi. */
	public Integer getHr() {
		return hr == null ? 0 : hr;
	}

	/** @param hr jumlah hari mengajar baru. */
	public void setHr(Integer hr) {
		this.hr = hr;
	}

	/** @return total SKS terakumulasi, {@code 0} bila belum diisi. */
	public Integer getSksTotal() {
		return sksTotal == null ? 0 : sksTotal;
	}

	/** @param sksTotal total SKS terakumulasi baru. */
	public void setSksTotal(Integer sksTotal) {
		this.sksTotal = sksTotal;
	}

	/** @return tanggal mulai periode rekap; hari ini bila belum pernah diisi (bukan {@code null}). */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai == null ? WaktuUtil.getDate() : tanggalMulai;
	}

	/** @param tanggalMulai tanggal mulai periode rekap yang baru. */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/** @return tanggal akhir periode rekap; hari ini bila belum pernah diisi (bukan {@code null}). */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSampai() {
		return tanggalSampai == null ? WaktuUtil.getDate() : tanggalSampai;
	}

	/** @param tanggalSampai tanggal akhir periode rekap yang baru. */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/** @return SKS pecahan; bila belum diisi, dipakai {@link #getSks()} yang dikonversi ke {@code double}. */
	public Double getSkspecahan() {
		return skspecahan == null ? getSks().doubleValue() : skspecahan;
	}

	/** @param skspecahan SKS pecahan baru. */
	public void setSkspecahan(Double skspecahan) {
		this.skspecahan = skspecahan;
	}

	/** @return jumlah pertemuan masuk, {@code 0} bila belum diisi. */
	public Integer getMasuk() {
		return masuk == null ? 0 : masuk;
	}

	/** @param masuk jumlah pertemuan masuk baru. */
	public void setMasuk(Integer masuk) {
		this.masuk = masuk;
	}

	/** @return jumlah kelas yang diampu, {@code 0} bila belum diisi. */
	public Integer getJmlKelas() {
		return jmlKelas == null ? 0 : jmlKelas;
	}

	/** @param jmlKelas jumlah kelas baru. */
	public void setJmlKelas(Integer jmlKelas) {
		this.jmlKelas = jmlKelas;
	}

	/** @return jumlah mata kuliah yang diampu, {@code 0} bila belum diisi. */
	public Integer getJmlMk() {
		return jmlMk == null ? 0 : jmlMk;
	}

	/** @param jmlMk jumlah mata kuliah baru. */
	public void setJmlMk(Integer jmlMk) {
		this.jmlMk = jmlMk;
	}

	/** @return SKS pecahan per-mata-kuliah, {@code 0.0} bila belum diisi. */
	public Double getSkspecahanmk() {
		return skspecahanmk == null ? 0.0 : skspecahanmk;
	}

	/** @param skspecahanmk SKS pecahan per-mata-kuliah baru. */
	public void setSkspecahanmk(Double skspecahanmk) {
		this.skspecahanmk = skspecahanmk;
	}

}
