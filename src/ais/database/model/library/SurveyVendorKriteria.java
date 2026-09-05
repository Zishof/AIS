package ais.database.model.library;

// Kriteria penilaian survey (configurable per survey) + bobot. P2/P3 saran AI: bobot & pertanyaan
// tidak di-hard-code, disimpan di DB sehingga admin bisa mengubah tanpa ubah source.

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
 * Entitas <b>kriteria penilaian</b> (tabel {@code library.survey_vendor_kriteria}) untuk satu
 * {@link SurveyVendor} — satu baris mewakili satu kriteria (mis. "Harga", "Kualitas Layanan")
 * beserta bobotnya, ditautkan lewat {@link #getSurveyVendor()} (banyak-ke-satu).
 *
 * <p>Sesuai komentar berkas ini: bobot dan pertanyaan kriteria sengaja <b>tidak di-hard-code</b>
 * ke dalam kode Java — semuanya disimpan di basis data sehingga admin dapat menambah, mengubah,
 * atau menonaktifkan kriteria tanpa mengubah kode sumber. Setiap sel skor pada
 * {@link SurveyVendorPenilaianDetail} menunjuk satu baris kriteria lewat
 * {@link SurveyVendorPenilaianDetail#getKriteria()}, sehingga menghapus atau mengubah bobot
 * kriteria yang sudah dipakai penilaian akan mengubah perhitungan agregat penilaian yang sudah
 * ada tanpa jejak versi/riwayat perubahan.</p>
 *
 * @see SurveyVendor
 * @see SurveyVendorPenilaianDetail
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor_kriteria")
public class SurveyVendorKriteria extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000003L;

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah baris kriteria ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah baris kriteria ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah baris kriteria ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris kriteria ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir baris kriteria ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Representasi teks singkat: nama kriteria.
	 *
	 * @return nama kriteria; dapat {@code null} bila belum diisi
	 */
	public String toString() { return nama; }

	/** Header survei tempat kriteria ini berlaku. */
	private SurveyVendor surveyVendor;
	/** Nomor urut tampilan kriteria pada form penilaian. */
	private Integer urutan;
	/** Nama kriteria (mis. "Harga", "Kualitas Layanan"). */
	private String nama;
	/** Teks pertanyaan/panduan penilaian untuk kriteria ini. */
	private String pertanyaan;
	/** Bobot persen kriteria; nilainya configurable, tidak di-hard-code. */
	private Double bobot;      // persen
	/** Penanda kriteria masih dipakai/ditampilkan; bawaan {@code true}. */
	private Boolean aktif;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendorKriteria() {}

	/**
	 * Mengembalikan kunci utama baris kriteria ini.
	 *
	 * @return id kriteria, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama baris kriteria ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/**
	 * Mengembalikan header survei tempat kriteria ini berlaku.
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
	 * Menyetel header survei tempat kriteria ini berlaku.
	 *
	 * @param v header survei; boleh {@code null}
	 */
	public void setSurveyVendor(SurveyVendor v) { this.surveyVendor = v; }

	/**
	 * Mengembalikan nomor urut tampilan kriteria, dengan bawaan 0 bila belum diisi.
	 *
	 * @return nomor urut; tidak pernah {@code null}
	 */
	@Column(name = "urutan") public Integer getUrutan() { return urutan == null ? 0 : urutan; }
	/**
	 * Menyetel nomor urut tampilan kriteria.
	 *
	 * @param urutan nomor urut; boleh {@code null} untuk kembali ke bawaan 0
	 */
	public void setUrutan(Integer urutan) { this.urutan = urutan; }

	/** @return nama kriteria; boleh {@code null} */
	@Column(name = "nama", length = 255) public String getNama() { return nama; }
	/** @param nama nama kriteria */
	public void setNama(String nama) { this.nama = nama; }

	/** @return teks pertanyaan/panduan penilaian kriteria ini; boleh {@code null} */
	@Column(name = "pertanyaan") public String getPertanyaan() { return pertanyaan; }
	/** @param pertanyaan teks pertanyaan/panduan penilaian */
	public void setPertanyaan(String pertanyaan) { this.pertanyaan = pertanyaan; }

	/**
	 * Mengembalikan bobot persen kriteria ini, dengan bawaan 0.0 bila belum diisi.
	 *
	 * <p>Tidak ada validasi di kelas ini bahwa jumlah bobot seluruh kriteria pada satu survei
	 * berjumlah 100 — konsistensinya (bila memang disyaratkan) menjadi tanggung jawab
	 * {@code SurveyVendorAction}.</p>
	 *
	 * @return bobot persen; tidak pernah {@code null}
	 */
	@Column(name = "bobot") public Double getBobot() { return bobot == null ? 0.0 : bobot; }
	/**
	 * Menyetel bobot persen kriteria ini.
	 *
	 * @param bobot bobot persen; boleh {@code null} untuk kembali ke bawaan 0.0
	 */
	public void setBobot(Double bobot) { this.bobot = bobot; }

	/**
	 * Mengembalikan penanda kriteria masih dipakai/ditampilkan, dengan bawaan {@code true} bila
	 * belum diisi.
	 *
	 * @return {@code true} bila kriteria aktif/ditampilkan; {@code false} bila dinonaktifkan
	 */
	@Column(name = "aktif") public Boolean getAktif() { return aktif == null || aktif; }
	/**
	 * Menyetel penanda kriteria masih dipakai/ditampilkan.
	 *
	 * @param aktif penanda aktif; boleh {@code null} untuk kembali ke bawaan {@code true}
	 */
	public void setAktif(Boolean aktif) { this.aktif = aktif; }
}
