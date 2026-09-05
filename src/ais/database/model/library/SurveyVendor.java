package ais.database.model.library;

// Header Survey Pemilihan Penilaian Vendor (angket multi-pengguna, pra-pembelian).
// Staf pengadaan setup survey + vendor + kriteria(bobot configurable) + pengguna(penilai);
// tiap pengguna menilai independen; hasil diagregat + staf menentukan pemenang (audit auto vs terpilih).

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
 * Entitas header <b>Survey Pemilihan Penilaian Vendor</b> (tabel {@code library.survey_vendor}) —
 * mekanisme survei evaluasi vendor <i>multi-pengguna</i> untuk pengadaan perpustakaan, dipakai
 * pra-pembelian sebagai alternatif/pelengkap {@link SeleksiVendor} (yang penilaiannya tunggal oleh
 * satu penilai). Staf pengadaan menyiapkan satu survei berisi vendor yang dinilai
 * ({@link SurveyVendorVendor}), kriteria penilaian beserta bobotnya
 * ({@link SurveyVendorKriteria}), dan pengguna yang diberi peran penilai/pengamat
 * ({@link SurveyVendorPengguna}); tiap pengguna penilai kemudian mengisi penilaiannya sendiri
 * secara independen ({@link SurveyVendorPenilaian} sebagai header per-pengguna,
 * {@link SurveyVendorPenilaianDetail} sebagai sel skor per kriteria&times;vendor), dan staf
 * pengadaan akhirnya menentukan {@link #getVendorTerpilih()} — yang tidak harus sama dengan
 * pemenang skor otomatis, asalkan alasannya dicatat pada {@link #getAlasanUtama()}.
 *
 * <h2>Rantai relasi lengkap</h2>
 * <p>{@code SurveyVendor} (header) &rarr; {@link SurveyVendorKriteria} (kriteria+bobot,
 * {@code survey_vendor}) &rarr; {@link SurveyVendorVendor} (vendor yang dinilai,
 * {@code survey_vendor} + {@code penyedia}) &rarr; {@link SurveyVendorPengguna} (pengguna yang
 * di-assign, {@code survey_vendor} + {@code pengguna}) &rarr; {@link SurveyVendorPenilaian}
 * (header penilaian satu pengguna, {@code survey_vendor} + {@code pengguna}) &rarr;
 * {@link SurveyVendorPenilaianDetail} (sel skor, {@code penilaian} + {@code kriteria} +
 * {@code vendor}). Matriks skor satu penilaian pengguna berukuran (jumlah kriteria)&times;(jumlah
 * vendor) baris {@code SurveyVendorPenilaianDetail}.</p>
 *
 * <h2>Status: kolom teks bebas set, tidak diturunkan dari sumber independen</h2>
 * <p>Berbeda dari {@link SeleksiVendor} yang statusnya ditegakkan lewat disposisi
 * {@code DisposisiSop} (lihat javadoc kelasnya), {@link #getStatus()} di sini murni kolom teks
 * ({@link #DRAFT}/{@link #AKTIF}/{@link #SELESAI}) yang nilainya sepenuhnya bergantung pada
 * pemanggilan {@link #setStatus(String)} — tidak ada logika di kelas ini yang menurunkan atau
 * mengoreksinya dari keadaan lain. Penegakan alur (survei harus {@link #AKTIF} sebelum menerima
 * penilaian, hanya staf yang boleh mengubah status) sepenuhnya berada di {@code SurveyVendorAction}
 * (mis. layar penilaian menolak input bila {@code !AKTIF.equals(sv.getStatus())}), bukan pada
 * entitas ini. Ini adalah instance tambahan dari pola "status murni deskriptif" yang berulang di
 * modul lain — di sini pun tidak ada penjamin bahwa status yang tersimpan konsisten dengan progres
 * penilaian sesungguhnya bila seseorang menulis lewat jalur lain (CRUD generik, API) yang melewati
 * {@code SurveyVendorAction}.</p>
 *
 * <h2>Gerbang kualifikasi opsional (P1)</h2>
 * <p>{@link #getPakaiQualification()} mengaktifkan gerbang lulus/gagal per vendor sebelum
 * penilaian skor — vendor yang gagal kualifikasi ditandai lewat
 * {@link SurveyVendorVendor#getLulusQualification()} (bawaan {@code true}/lulus). Gerbang ini
 * bersifat opsional per survei dan, seperti status, penegakannya ada di lapisan action, bukan
 * pada validasi entitas.</p>
 *
 * <h2>Audit trail pemenang: otomatis vs terpilih</h2>
 * <p>{@link #getVendorTerpilih()} dan {@link #getAlasanUtama()} (P4) secara eksplisit memisahkan
 * pemenang berdasar skor agregat (dihitung di {@code SurveyVendorAction}, tidak disimpan di
 * entitas manapun) dari keputusan akhir staf pengadaan. Bila keduanya berbeda, hanya
 * {@link #getAlasanUtama()} yang mencatat alasannya — tidak ada kolom yang menyimpan skor
 * otomatis itu sendiri untuk dibandingkan ulang di kemudian hari; nilai historisnya hanya dapat
 * direkonstruksi dengan menjumlahkan ulang seluruh {@link SurveyVendorPenilaianDetail} yang
 * relevan.</p>
 *
 * @see SurveyVendorKriteria
 * @see SurveyVendorVendor
 * @see SurveyVendorPengguna
 * @see SurveyVendorPenilaian
 * @see SurveyVendorPenilaianDetail
 * @see SeleksiVendor
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "library", name = "survey_vendor")
public class SurveyVendor extends GeneralValueObject {

	/** Penanda versi serialisasi Java. */
	private static final long serialVersionUID = 7720145511001000001L;

	/** Status awal: survei masih disiapkan, belum menerima penilaian — bawaan {@link #getStatus()}. */
	public static final String DRAFT = "Draft";
	/** Status survei sedang berjalan dan menerima penilaian dari pengguna yang di-assign. */
	public static final String AKTIF = "Aktif";
	/** Status survei sudah ditutup/selesai dinilai. */
	public static final String SELESAI = "Selesai";

	/** Rekomendasi akhir: vendor terpilih direkomendasikan untuk dipakai. */
	public static final String REKOM_DIREKOMENDASIKAN = "Direkomendasikan";
	/** Rekomendasi akhir: hasil survei perlu ditinjau ulang sebelum diputuskan. */
	public static final String REKOM_PERTIMBANGAN_ULANG = "Perlu pertimbangan ulang";
	/** Rekomendasi akhir: tidak ada vendor yang layak direkomendasikan. */
	public static final String REKOM_TIDAK = "Tidak direkomendasikan";

	/** Kunci utama basis data, dibangkitkan {@code IDENTITY}; {@code null} selama baris belum tersimpan. */
	private Long id;
	/** Nama pengguna terakhir yang mengubah baris ini; diisi {@code AuditTimestampInterceptor}, bukan oleh form. */
	private String oleh;
	/** Id pengguna terakhir yang mengubah baris ini; pasangan teknis dari {@link #oleh}. */
	private String olehId;

	/**
	 * Mengembalikan id pengguna yang terakhir mengubah header survei ini.
	 *
	 * @return id pengguna terakhir, atau {@code null} bila belum pernah diubah lewat jalur yang
	 *         memasang interceptor audit
	 */
	public String getOlehId() { return olehId; }
	/**
	 * Menyetel id pengguna yang terakhir mengubah header survei ini.
	 *
	 * <p><b>Setter defensif:</b> masukan {@code null}/kosong diabaikan diam-diam.</p>
	 *
	 * @param olehId id pengguna; {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) { if (olehId == null || olehId.trim().isEmpty()) { return; } this.olehId = olehId; }
	/**
	 * Menyetel nama pengguna yang terakhir mengubah header survei ini.
	 *
	 * @param oleh nama pengguna; {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) { if (oleh == null || oleh.trim().isEmpty()) { return; } this.oleh = oleh; }
	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah header survei ini.
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
	 * Mengembalikan stempel waktu perubahan terakhir header survei ini.
	 *
	 * @return stempel waktu perubahan terakhir; tidak pernah {@code null} untuk objek baru di memori
	 */
	@Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah() { return tanggal_dirubah; }

	/**
	 * Representasi teks singkat: judul survei.
	 *
	 * @return judul survei; dapat {@code null} bila belum diisi
	 */
	public String toString() { return judul; }

	/** Kode survei (bebas isi). */
	private String kode;
	/** Judul survei. */
	private String judul;
	/** Jenis barang/jasa yang menjadi objek survei. */
	private String jenisBarangJasa;
	/** Tanggal survei dibuka. */
	private Date tanggal;
	/** Catatan bebas tentang survei. */
	private String keterangan;
	/** Status alur survei (salah satu {@link #DRAFT}/{@link #AKTIF}/{@link #SELESAI}); murni kolom teks, lihat javadoc kelas. */
	private String status;
	/** Staf yang membuat/menyiapkan survei ini. */
	private Tbmuser dibuatOleh;
	/** Waktu pembuatan survei. */
	private Date tanggalPembuatan;
	/** Penanda pengaktifan gerbang kualifikasi lulus/gagal (P1) sebelum vendor dinilai skornya; opsional. */
	private Boolean pakaiQualification;   // P1: gerbang lulus/gagal sebelum scoring (opsional)

	// Ringkasan perbandingan (final oleh staf pengadaan)
	/** Nama/ringkasan vendor pembanding pertama, diisi final oleh staf pengadaan. */
	private String vendorPembanding1;
	/** Nama/ringkasan vendor pembanding kedua. */
	private String vendorPembanding2;
	/** Nama/ringkasan vendor pembanding ketiga. */
	private String vendorPembanding3;
	/** Alasan vendor tertentu dipilih di antara para pembanding. */
	private String alasanDipilih;

	// Rekomendasi & audit trail (P4): pemenang otomatis (skor) vs vendor terpilih staf + alasan
	/** Rekomendasi akhir; salah satu konstanta {@code REKOM_*}. */
	private String rekomendasi;
	/** Vendor yang secara final dipilih staf pengadaan; boleh berbeda dari pemenang skor otomatis — lihat javadoc kelas. */
	private SurveyVendorVendor vendorTerpilih;
	/** Alasan utama vendor terpilih, terutama penting bila berbeda dari pemenang skor otomatis. */
	private String alasanUtama;

	// Penilai akhir (Lampiran 1.2)
	/** Nama penilai yang menandatangani Lampiran 1.2. */
	private String namaPenilai;
	/** Jabatan penilai Lampiran 1.2. */
	private String jabatanPenilai;
	/** Tanggal Lampiran 1.2 ditandatangani. */
	private Date tanggalPenilaian;

	/** Konstruktor tanpa argumen yang diwajibkan JPA/Hibernate. */
	public SurveyVendor() {}

	/**
	 * Mengembalikan kunci utama header survei ini.
	 *
	 * @return id survei, atau {@code null} bila baris belum pernah disimpan
	 */
	@Id @GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() { return this.id; }
	/**
	 * Menyetel kunci utama header survei ini. Hanya untuk kebutuhan Hibernate/penyalinan objek.
	 *
	 * @param id kunci utama
	 */
	public void setId(Long id) { this.id = id; }

	/** @return kode survei; boleh {@code null} */
	@Column(name = "kode", length = 100) public String getKode() { return kode; }
	/** @param kode kode survei; boleh {@code null} */
	public void setKode(String kode) { this.kode = kode; }

	/** @return judul survei; boleh {@code null} */
	@Column(name = "judul", length = 255) public String getJudul() { return judul; }
	/** @param judul judul survei */
	public void setJudul(String judul) { this.judul = judul; }

	/** @return jenis barang/jasa yang menjadi objek survei; boleh {@code null} */
	@Column(name = "jenis_barang_jasa") public String getJenisBarangJasa() { return jenisBarangJasa; }
	/** @param v jenis barang/jasa objek survei */
	public void setJenisBarangJasa(String v) { this.jenisBarangJasa = v; }

	/** @return tanggal survei dibuka; boleh {@code null} */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal") public Date getTanggal() { return tanggal; }
	/** @param tanggal tanggal survei dibuka; boleh {@code null} */
	public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

	/** @return catatan bebas tentang survei; boleh {@code null} */
	@Column(name = "keterangan") public String getKeterangan() { return keterangan; }
	/** @param keterangan catatan bebas; boleh {@code null} */
	public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

	/**
	 * Mengembalikan status alur survei, dengan bawaan {@link #DRAFT} bila belum diisi.
	 *
	 * <p><b>Perhatikan:</b> berbeda dari {@link SeleksiVendor#getStatus()}, method ini
	 * <b>tidak</b> menurunkan atau mengoreksi nilainya dari sumber independen apa pun — ia hanya
	 * membaca bidang mentah {@link #status} dan menormalkan {@code null} menjadi {@link #DRAFT}.
	 * Penegakan bahwa status benar-benar mencerminkan progres survei sepenuhnya bergantung pada
	 * disiplin {@code SurveyVendorAction} memanggil {@link #setStatus(String)} pada titik yang
	 * tepat; lihat javadoc kelas.</p>
	 *
	 * @return salah satu {@link #DRAFT}, {@link #AKTIF}, atau {@link #SELESAI}; tidak pernah
	 *         {@code null}
	 */
	@Column(name = "status", length = 30) public String getStatus() { return status == null ? DRAFT : status; }
	/**
	 * Menyetel status alur survei secara langsung, tanpa validasi transisi apa pun di sini.
	 *
	 * @param status status baru; sebaiknya salah satu {@link #DRAFT}/{@link #AKTIF}/{@link #SELESAI}
	 */
	public void setStatus(String status) { this.status = status; }

	/**
	 * Mengembalikan staf pengadaan yang membuat/menyiapkan survei ini, setelah proksi malasnya
	 * diselesaikan {@code check(...)}.
	 *
	 * @return pembuat survei, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() { dibuatOleh = check(dibuatOleh); return dibuatOleh; }
	/**
	 * Menyetel staf pengadaan yang membuat survei ini.
	 *
	 * @param dibuatOleh pembuat survei; boleh {@code null}
	 */
	public void setDibuatOleh(Tbmuser dibuatOleh) { this.dibuatOleh = dibuatOleh; }

	/** @return waktu pembuatan survei; boleh {@code null} */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_pembuatan") public Date getTanggalPembuatan() { return tanggalPembuatan; }
	/** @param v waktu pembuatan survei; boleh {@code null} */
	public void setTanggalPembuatan(Date v) { this.tanggalPembuatan = v; }

	/**
	 * Mengembalikan penanda pengaktifan gerbang kualifikasi (P1), dengan {@code null} dinormalkan
	 * menjadi {@code false}.
	 *
	 * @return {@code true} bila survei ini memakai gerbang lulus/gagal kualifikasi sebelum
	 *         penilaian skor; {@code false} bila tidak atau belum diisi
	 */
	@Column(name = "pakai_qualification") public Boolean getPakaiQualification() { return pakaiQualification != null && pakaiQualification; }
	/**
	 * Menyetel penanda pengaktifan gerbang kualifikasi (P1).
	 *
	 * @param v {@code true} untuk mengaktifkan gerbang kualifikasi; boleh {@code null}
	 */
	public void setPakaiQualification(Boolean v) { this.pakaiQualification = v; }

	/** @return nama/ringkasan vendor pembanding pertama; boleh {@code null} */
	@Column(name = "vendor_pembanding1") public String getVendorPembanding1() { return vendorPembanding1; }
	/** @param v nama/ringkasan vendor pembanding pertama */
	public void setVendorPembanding1(String v) { this.vendorPembanding1 = v; }
	/** @return nama/ringkasan vendor pembanding kedua; boleh {@code null} */
	@Column(name = "vendor_pembanding2") public String getVendorPembanding2() { return vendorPembanding2; }
	/** @param v nama/ringkasan vendor pembanding kedua */
	public void setVendorPembanding2(String v) { this.vendorPembanding2 = v; }
	/** @return nama/ringkasan vendor pembanding ketiga; boleh {@code null} */
	@Column(name = "vendor_pembanding3") public String getVendorPembanding3() { return vendorPembanding3; }
	/** @param v nama/ringkasan vendor pembanding ketiga */
	public void setVendorPembanding3(String v) { this.vendorPembanding3 = v; }
	/** @return alasan vendor tertentu dipilih di antara para pembanding; boleh {@code null} */
	@Column(name = "alasan_dipilih") public String getAlasanDipilih() { return alasanDipilih; }
	/** @param v alasan vendor tertentu dipilih */
	public void setAlasanDipilih(String v) { this.alasanDipilih = v; }

	/** @return rekomendasi akhir; salah satu konstanta {@code REKOM_*}; boleh {@code null} */
	@Column(name = "rekomendasi", length = 60) public String getRekomendasi() { return rekomendasi; }
	/** @param v rekomendasi akhir; sebaiknya salah satu konstanta {@code REKOM_*} */
	public void setRekomendasi(String v) { this.rekomendasi = v; }

	/**
	 * Mengembalikan vendor yang secara final dipilih staf pengadaan, setelah proksi malasnya
	 * diselesaikan {@code check(...)}.
	 *
	 * <p>Nilai ini adalah keputusan manusia, bukan turunan otomatis dari skor agregat — lihat
	 * bagian "Audit trail pemenang" pada javadoc kelas. Bila berbeda dari pemenang skor tertinggi,
	 * {@link #getAlasanUtama()} mestinya mencatat alasannya.</p>
	 *
	 * @return vendor terpilih final, atau {@code null} bila belum diputuskan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "vendor_terpilih", nullable = true)
	public SurveyVendorVendor getVendorTerpilih() { vendorTerpilih = check(vendorTerpilih); return vendorTerpilih; }
	/**
	 * Menyetel vendor yang secara final dipilih staf pengadaan.
	 *
	 * @param v vendor terpilih; boleh {@code null}
	 */
	public void setVendorTerpilih(SurveyVendorVendor v) { this.vendorTerpilih = v; }

	/** @return alasan utama vendor terpilih; boleh {@code null} */
	@Column(name = "alasan_utama") public String getAlasanUtama() { return alasanUtama; }
	/** @param v alasan utama vendor terpilih */
	public void setAlasanUtama(String v) { this.alasanUtama = v; }

	/** @return nama penilai Lampiran 1.2; boleh {@code null} */
	@Column(name = "nama_penilai") public String getNamaPenilai() { return namaPenilai; }
	/** @param v nama penilai Lampiran 1.2 */
	public void setNamaPenilai(String v) { this.namaPenilai = v; }
	/** @return jabatan penilai Lampiran 1.2; boleh {@code null} */
	@Column(name = "jabatan_penilai") public String getJabatanPenilai() { return jabatanPenilai; }
	/** @param v jabatan penilai Lampiran 1.2 */
	public void setJabatanPenilai(String v) { this.jabatanPenilai = v; }
	/** @return tanggal Lampiran 1.2 ditandatangani; boleh {@code null} */
	@Temporal(TemporalType.TIMESTAMP) @Column(name = "tanggal_penilaian") public Date getTanggalPenilaian() { return tanggalPenilaian; }
	/** @param v tanggal Lampiran 1.2 ditandatangani */
	public void setTanggalPenilaian(Date v) { this.tanggalPenilaian = v; }
}
