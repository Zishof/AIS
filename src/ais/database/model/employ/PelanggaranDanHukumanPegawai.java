package ais.database.model.employ;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate "template"/aturan pemetaan pelanggaran-ke-hukuman kepegawaian AIS — dipetakan
 * ke tabel {@code employ.pelanggaran_dan_hukuman_pegawai}. Satu baris merepresentasikan SATU
 * ATURAN yang memetakan sehimpunan {@link PelanggaranPegawai} ({@link #getPelanggaranPegawais()})
 * ke sehimpunan {@link HukumanPegawai} ({@link #getHukumanPegawais()}) — mis. aturan "Terlambat
 * &gt; 3x dalam sebulan" dipetakan ke hukuman "Surat Peringatan 1". Entitas ini adalah MASTER DATA
 * aturan, BUKAN catatan kejadian pelanggaran pegawai tertentu — untuk catatan kejadian aktual,
 * lihat {@link PendataanPelanggaranPegawai} (yang merujuk baris template ini lewat field
 * {@code pelanggaranDanHukumanPegawai} sekaligus menyimpan salinan himpunan
 * pelanggaran/hukumannya sendiri, yang bisa saja disesuaikan berbeda dari template). Lihat
 * "Rantai disiplin pegawai" pada Javadoc {@link HukumanPegawai} untuk gambaran alur lengkap.
 *
 * @see PelanggaranPegawai
 * @see HukumanPegawai
 * @see PendataanPelanggaranPegawai
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "pelanggaran_dan_hukuman_pegawai", schema = "employ")
public class PelanggaranDanHukumanPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}.
	 */
	private static final long serialVersionUID = -7490758846785025664L;
	/** Primary key baris ini, di-generate database (IDENTITY). */
	private Long id;
	/** Nama pengguna audit terakhir yang mengubah baris ini. */
	private String oleh;
	/** Id pengguna audit terakhir yang mengubah baris ini (pasangan {@link #oleh}). */
	private String olehId;

	/** @return {@link #olehId} — id pengguna audit terakhir yang mengubah baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Meng-set {@link #olehId}; dilewati (no-op) bila {@code olehId} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param olehId id pengguna yang melakukan perubahan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Meng-set {@link #oleh}; dilewati (no-op) bila {@code oleh} {@code null} atau kosong/hanya
	 * berisi spasi, sehingga nilai audit lama tidak pernah tertimpa nilai kosong.
	 *
	 * @param oleh nama pengguna yang melakukan perubahan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return {@link #oleh} — nama pengguna audit terakhir yang mengubah baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}, dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE; mendelegasikan pembaruan {@link #tanggal_dirubah} ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}. Tidak dipanggil manual
	 * dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah nilai timestamp audit baru; dipanggil manual maupun otomatis oleh {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return {@link #tanggal_dirubah} — timestamp terakhir baris ini diubah; nilai awal saat konstruksi objek adalah waktu sekarang. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Keterangan/deskripsi bebas untuk aturan pelanggaran-dan-hukuman ini, boleh {@code null}. */
	private String keterangan;
	/** Nama aturan (mis. "Terlambat Berulang Kali"). */
	private String nama;

	/** Menandai apakah aturan ini masih aktif/boleh dipakai; {@code null} diperlakukan sebagai aktif (lihat {@link #getAktif()}). */
	private Boolean aktif;

	/** Himpunan jenis hukuman yang dipetakan oleh aturan ini. */
	private Set<HukumanPegawai> hukumanPegawais = new HashSet<HukumanPegawai>();

	/**
	 * @return {@link #hukumanPegawais} — himpunan jenis hukuman yang dipetakan aturan ini, terurut
	 *         menurut {@code nama}, lewat join table {@code pelanggaran_dan_hukuman_pegawai_has_hukuman}.
	 */
	@ManyToMany(targetEntity = HukumanPegawai.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_dan_hukuman_pegawai_has_hukuman", schema = "employ", joinColumns = @JoinColumn(name = "pelanggaran_dan_hukuman_pegawai"), inverseJoinColumns = @JoinColumn(name = "hukuman_pegawai"))
	public Set<HukumanPegawai> getHukumanPegawais() {
		return hukumanPegawais;
	}

	/** @param hukumanPegawais himpunan jenis hukuman baru untuk aturan ini. */
	public void setHukumanPegawais(Set<HukumanPegawai> hukumanPegawais) {
		this.hukumanPegawais = hukumanPegawais;
	}

	/** Himpunan jenis pelanggaran yang dipetakan oleh aturan ini. */
	private Set<PelanggaranPegawai> pelanggaranPegawais = new HashSet<PelanggaranPegawai>();

	/**
	 * @return {@link #pelanggaranPegawais} — himpunan jenis pelanggaran yang dipetakan aturan ini,
	 *         terurut menurut {@code nama}, lewat join table
	 *         {@code pelanggaran_dan_hukuman_has_pelanggaran_pegawai}. Perhatikan nama join table
	 *         ini BERBEDA dari yang dipakai {@link #getHukumanPegawais()} (kata "pegawai" dan
	 *         "hukuman"/"pelanggaran" tertukar posisi) — sekadar variasi penamaan tabel fisik,
	 *         bukan indikasi baris/relasi yang salah; kolom join {@code pelanggaran_pegawai_dan_hukuman}
	 *         tetap merujuk {@link #id} baris ini dengan benar.
	 */
	@ManyToMany(targetEntity = PelanggaranPegawai.class, cascade = { CascadeType.MERGE })
	@OrderBy(value = "nama asc")
	@JoinTable(name = "pelanggaran_dan_hukuman_has_pelanggaran_pegawai", schema = "employ", joinColumns = @JoinColumn(name = "pelanggaran_pegawai_dan_hukuman"), inverseJoinColumns = @JoinColumn(name = "pelanggaran_pegawai"))
	public Set<PelanggaranPegawai> getPelanggaranPegawais() {
		return pelanggaranPegawais;
	}

	/** @param pelanggaranPegawais himpunan jenis pelanggaran baru untuk aturan ini. */
	public void setPelanggaranPegawais(Set<PelanggaranPegawai> pelanggaranPegawais) {
		this.pelanggaranPegawais = pelanggaranPegawais;
	}

	/** Konstruktor default (dibutuhkan Hibernate); tidak menginisialisasi field apa pun secara eksplisit selain default deklarasi field dan kedua himpunan kosong. */
	public PelanggaranDanHukumanPegawai() {
	}

	/**
	 * Konstruktor kenyamanan untuk membuat baris dengan {@link #id} dan {@link #nama} langsung
	 * terisi. Field lain (keterangan, status aktif, himpunan pelanggaran/hukuman) TIDAK ikut diisi
	 * oleh konstruktor ini.
	 *
	 * @param id   primary key yang akan di-set langsung (bukan menunggu generate database)
	 * @param nama nama aturan pelanggaran-dan-hukuman
	 */
	public PelanggaranDanHukumanPegawai(long id, String nama) {
		this.id = id;
		this.nama = nama;
	}

	/** @return {@link #id} — primary key baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baru; normalnya di-generate database, jarang di-set manual dari kode aplikasi. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #keterangan} — keterangan/deskripsi bebas aturan ini, boleh {@code null}. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi bebas baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return {@link #nama} — nama aturan ini (tidak di-trim, berbeda dengan beberapa entitas lain di paket ini). */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
	}

	/** @param nama nama aturan baru. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #aktif}; {@code true} bila belum pernah di-set ({@code null}) — default aktif. */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif baru untuk aturan ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
