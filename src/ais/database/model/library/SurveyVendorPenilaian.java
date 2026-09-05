package ais.database.model.library;

// Penilaian satu pengguna atas satu survey (header). Tiap penilai punya 1 baris ini; detail skor
// per-kriteria per-vendor ada di SurveyVendorPenilaianDetail. Mendukung status revisi (Ralat/Perbaikan).

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
import ais.database.model.Tbmuser;

/**
 * Entitas header <b>penilaian per-pengguna</b> (tabel {@code library.survey_vendor_penilaian}) —
 * satu baris mewakili penilaian yang diisi satu {@link Tbmuser} atas satu {@link SurveyVendor}.
 * Detail skor per kriteria&times;vendor tersimpan terpisah pada
 * {@link SurveyVendorPenilaianDetail} (relasi satu-ke-banyak lewat
 * {@link SurveyVendorPenilaianDetail#getPenilaian()}).
 *
 * <h2>Mendukung revisi (Ralat/Perbaikan)</h2>
 * <p>Selain {@link #BELUM} (belum dinilai) dan {@link #SELESAI} (penilaian tersimpan lengkap),
 * status {@link #REVISI} menandai penilaian yang perlu diperbaiki ulang oleh penilainya — pada
 * layar (lihat {@code SurveyVendorAction}) tombol yang ditampilkan untuk penilaian yang sudah
 * {@link #SELESAI} berlabel "Ralat/Revisi", memungkinkan pengguna membuka kembali penilaiannya
 * yang sudah tersimpan. Kelas ini sendiri tidak memvalidasi transisi status (mis. tidak mencegah
 * lompat dari {@link #BELUM} langsung ke {@link #SELESAI} tanpa detail skor terisi) — validasi
 * kelengkapan ada di lapisan action yang mengisi {@link SurveyVendorPenilaianDetail} sebelum
 * menyetel status ini.</p>
 *
 * <h2>Relasi ganda ke {@link SurveyVendor} dan {@link Tbmuser}</h2>
 * <p>Kombinasi {@link #getSurveyVendor()} + {@link #getPengguna()} secara logis menjadi kunci unik
 * satu penilaian (satu pengguna seharusnya hanya punya satu baris penilaian per survei), namun
 * <b>tidak ada batasan {@code unique} pada tingkat basis data maupun validasi di kelas ini</b> yang
 * menegakkan hal tersebut — pencegahan duplikasi (bila ada) sepenuhnya bergantung pada
 * {@code SurveyVendorAction} memeriksa keberadaan baris sebelum membuat yang baru.</p>
 *
 * @see SurveyVendor
 * @see SurveyVendorPenilaianDetail
 * @see SurveyVendorPengguna
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_penilaian")
public class SurveyVendorPenilaian extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000005L;

	/** Status awal: pengguna belum mengisi penilaian — bawaan {@link #getStatus()}. */
	public static final String BELUM = "Belum dinilai";
	/** Status setelah pengguna menyimpan seluruh skor penilaiannya. */
	public static final String SELESAI = "Selesai";
	/** Status penilaian yang dibuka kembali untuk diperbaiki (Ralat/Perbaikan) — lihat javadoc kelas. */
	public static final String REVISI = "Revisi";

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris penilaian ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah baris penilaian ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris penilaian ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris penilaian ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris penilaian ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Representasi teks singkat berformat {@code "<nama pengguna> - <status>"}.
	 *
	 * @return gabungan nama pengguna (atau string kosong bila belum ditautkan) dan status
	 */
	public String toString() { return (pengguna == null ? "" : pengguna.getUserNama()) + " - " + getStatus(); }

	/** Header survei yang dinilai. */
	private SurveyVendor surveyVendor;
	/** Pengguna yang mengisi penilaian ini. */
	private Tbmuser pengguna;
	/** Status penilaian (salah satu {@link #BELUM}/{@link #SELESAI}/{@link #REVISI}). */
	private String status;
	/** Tanggal penilaian ini disimpan/diselesaikan. */
	private Date tanggalPenilaian;
	/** Catatan bebas dari penilai. */
	private String catatan;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendorPenilaian() {}

	/**
	 * Mengembalikan kunci utama baris penilaian ini.
	 *
	 * @return id, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama baris penilaian ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Mengembalikan header survei yang dinilai.
	 *
	 * <p>Tidak memanggil {@code check(...)} karena relasi ini tidak dinyatakan {@code LAZY} pada
	 * anotasi (bawaan JPA untuk {@code @ManyToOne} adalah <i>eager</i>).</p>
	 *
	 * @return header survei; boleh {@code null} bila belum ditautkan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "survey_vendor", nullable = true)
	public SurveyVendor getSurveyVendor() { return surveyVendor; }
	/**
	 * Menyetel header survei yang dinilai.
	 *
	 * @param v header survei; boleh {@code null}
	 */
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	/**
	 * Mengembalikan pengguna yang mengisi penilaian ini, setelah proksi malasnya diselesaikan
	 * {@code check(...)}.
	 *
	 * @return pengguna penilai; boleh {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pengguna", nullable = true)
	public Tbmuser getPengguna() { pengguna = check(pengguna); return pengguna; }
	/**
	 * Menyetel pengguna yang mengisi penilaian ini.
	 *
	 * @param pengguna pengguna penilai; boleh {@code null}
	 */
	public void setPengguna(Tbmuser pengguna) { this.pengguna = pengguna; }

	/**
	 * Mengembalikan status penilaian ini, dengan bawaan {@link #BELUM} bila belum diisi. Method
	 * ini murni membaca bidang mentah — tidak ada logika yang menurunkan atau mengoreksinya dari
	 * keberadaan detail skor (bandingkan dengan {@link SeleksiVendor#getStatus()} yang ditegakkan
	 * dari disposisi SOP).
	 *
	 * @return salah satu {@link #BELUM}, {@link #SELESAI}, atau {@link #REVISI}; tidak pernah
	 *         {@code null}
	 */
	@Column(name = "status", length = 30) public String getStatus() { return status == null ? BELUM : status; }
	/**
	 * Menyetel status penilaian ini secara langsung, tanpa validasi transisi apa pun di sini.
	 *
	 * @param status status baru; sebaiknya salah satu {@link #BELUM}/{@link #SELESAI}/{@link #REVISI}
	 */
	public void setStatus(String status) { this.status = status; }

	/** @return tanggal penilaian ini disimpan/diselesaikan; boleh {@code null} */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_penilaian") public Date getTanggalPenilaian() { return tanggalPenilaian; }
	/** @param v tanggal penilaian disimpan/diselesaikan; boleh {@code null} */
	public void setTanggalPenilaian(Date v) { this.tanggalPenilaian = v; }

	/** @return catatan bebas dari penilai; boleh {@code null} */
	@Column(name = "catatan") public String getCatatan() { return catatan; }
	/** @param catatan catatan bebas dari penilai; boleh {@code null} */
	public void setCatatan(String catatan) { this.catatan = catatan; }
}
