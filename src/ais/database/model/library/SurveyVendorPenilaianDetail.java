package ais.database.model.library;

// Sel skor: nilai (1..5) satu pengguna untuk satu kriteria pada satu vendor. Matriks penilaian =
// (jumlah kriteria) x (jumlah vendor) baris per penilaian pengguna.

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.GeneralValueObject;

/**
 * Entitas <b>sel skor</b> (tabel {@code library.survey_vendor_penilaian_detail}) — level paling
 * granular pada rantai relasi survei vendor: satu baris menyimpan satu nilai (umumnya 1..5, tidak
 * ditegakkan skema maupun validasi di kelas ini) yang diberikan satu pengguna
 * ({@link #getPenilaian()}) untuk satu pasangan kriteria&times;vendor
 * ({@link #getKriteria()}/{@link #getVendor()}).
 *
 * <p>Matriks skor lengkap satu penilaian pengguna berukuran (jumlah baris
 * {@link SurveyVendorKriteria} aktif pada surveinya) &times; (jumlah baris
 * {@link SurveyVendorVendor} pada surveinya), sehingga menyelesaikan satu penilaian berarti
 * menyimpan sejumlah baris kelas ini sebanyak hasil kali kedua dimensi tersebut.
 * {@code SurveyVendorAction} yang membangun dan membaca matriks ini (lihat kode di sekitar
 * pembuatan {@code cell[ki][vi]} dan penyimpanan {@code SurveyVendorPenilaianDetail} per sel).
 * Tidak ada batasan {@code unique} gabungan {@code (penilaian, kriteria, vendor)} pada tingkat
 * skema maupun validasi di kelas ini — mencegah sel duplikat sepenuhnya bergantung pada disiplin
 * action yang menyimpannya sekali per sel.</p>
 *
 * @see SurveyVendorPenilaian
 * @see SurveyVendorKriteria
 * @see SurveyVendorVendor
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_penilaian_detail")
public class SurveyVendorPenilaianDetail extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000006L;

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah sel skor ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah sel skor ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah sel skor ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah sel skor ini.
	 *
	 * @return nama pengguna terakhir, atau {@code null} bila belum pernah terisi
	 */
	public String getOleh() { return oleh; }

	/**
	 * Kait JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} tepat sebelum baris diperbarui. Method sengaja
	 * {@code protected} dan tidak boleh dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	/**
	 * Menyetel stempel waktu perubahan terakhir. Umumnya dipanggil {@code AuditTimestampInterceptor}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) { this.tanggal_dirubah = tanggal_dirubah; }
	/**
	 * Mengembalikan stempel waktu perubahan terakhir sel skor ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/** Header penilaian (satu pengguna, satu survei) tempat sel skor ini berada. */
	private SurveyVendorPenilaian penilaian;
	/** Kriteria yang dinilai pada sel ini. */
	private SurveyVendorKriteria kriteria;
	/** Vendor yang dinilai pada sel ini. */
	private SurveyVendorVendor vendor;
	/** Nilai/skor yang diberikan (umumnya 1..5; tidak ditegakkan skema atau validasi). */
	private Integer nilai;
	/** Catatan bebas untuk sel skor ini. */
	private String ket;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendorPenilaianDetail() {}

	/**
	 * Mengembalikan kunci utama sel skor ini.
	 *
	 * @return id, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama sel skor ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Mengembalikan header penilaian tempat sel skor ini berada.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return header penilaian; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "penilaian", nullable = true)
	public SurveyVendorPenilaian getPenilaian() { return penilaian; }
	/**
	 * Menyetel header penilaian tempat sel skor ini berada.
	 *
	 * @param v header penilaian; boleh {@code null}
	 */
	public void setPenilaian(SurveyVendorPenilaian v) { this.penilaian = v; }

	/**
	 * Mengembalikan kriteria yang dinilai pada sel ini.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return kriteria; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kriteria", nullable = true)
	public SurveyVendorKriteria getKriteria() { return kriteria; }
	/**
	 * Menyetel kriteria yang dinilai pada sel ini.
	 *
	 * @param v kriteria; boleh {@code null}
	 */
	public void setKriteria(SurveyVendorKriteria v) { this.kriteria = v; }

	/**
	 * Mengembalikan vendor yang dinilai pada sel ini.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return vendor; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "vendor", nullable = true)
	public SurveyVendorVendor getVendor() { return vendor; }
	/**
	 * Menyetel vendor yang dinilai pada sel ini.
	 *
	 * @param v vendor; boleh {@code null}
	 */
	public void setVendor(SurveyVendorVendor v) { this.vendor = v; }

	/**
	 * Mengembalikan nilai/skor sel ini. Konvensi antarmuka umumnya 1..5, namun kolomnya
	 * tidak dibatasi rentang tersebut baik oleh skema maupun oleh getter/setter ini.
	 *
	 * @return nilai/skor; boleh {@code null} bila belum dinilai
	 */
	@Column(name = "nilai") public Integer getNilai() { return nilai; }
	/**
	 * Menyetel nilai/skor sel ini.
	 *
	 * @param nilai nilai/skor; boleh {@code null}
	 */
	public void setNilai(Integer nilai) { this.nilai = nilai; }

	/**
	 * Mengembalikan catatan bebas untuk sel skor ini.
	 *
	 * @return catatan; boleh {@code null}
	 */
	@Column(name = "ket") public String getKet() { return ket; }
	/**
	 * Menyetel catatan bebas untuk sel skor ini.
	 *
	 * @param ket catatan; boleh {@code null}
	 */
	public void setKet(String ket) { this.ket = ket; }
}
