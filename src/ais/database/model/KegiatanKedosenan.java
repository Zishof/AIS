package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Model data untuk satu KEGIATAN kedosenan yang diajukan seorang dosen sebagai bukti kinerja di
 * luar mengajar langsung (mis. seminar, workshop, organisasi profesi) -- konsepnya PARALEL dengan
 * {@code KegiatanKemahasiswaan} untuk mahasiswa, dan TIDAK berelasi FK dengan {@code Kegiatan}/
 * {@code DetailKegiatan} (mekanisme billing/tagihan kegiatan berbayar yang sudah didokumentasikan
 * terpisah) -- kesamaan nama semata kebetulan penamaan domain, bukan hubungan data. Setiap baris
 * melewati alur persetujuan sederhana lewat {@link #getStatus()} (bukan lewat SOP), diklasifikasi
 * oleh kelompok/detail-kelompok, jabatan, dan skala kegiatan, serta opsional melampirkan
 * {@link Sertifikat} sebagai bukti keikutsertaan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code KelompokKegiatanKedosenan kelompokKegiatanKedosenan},
 * {@code String status}, {@code Dosen diajukanOleh}, {@code Sertifikat sertifikat}; pemetaan persistence: tabel
 * {@code public.kegiatan_kedosenan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getKode()}, {@code getStatus()}, {@code getTahun()}); mutasi
 * data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setStatus()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Catatan getter yang menulis field ({@code getKode()}, {@code getTahun()}, {@code getTahunAkademik()},
 * {@code getJenisSemester()}):</b> masing-masing mengisi field-nya sendiri secara LAZY pada pemanggilan
 * pertama (kode dari id diformat 5 digit; tahun/tahunAkademik/jenisSemester dari kalender berjalan) -- pola
 * berulang di puluhan entity AIS ({@code ais-getter-mutasi-field-anti-pattern-sistemik}), bukan cacat unik
 * kelas ini.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "kegiatan_kedosenan")

public class KegiatanKedosenan extends GeneralValueObject {

	/** Status awal: pengajuan belum ditinjau. Default {@link #getStatus()} bila kolom kosong. */
	public static final String BELUM_DIPROSES = "Belum diproses";
	/** Status: pengajuan sedang ditinjau/diproses. */
	public static final String SEDANG_DIPROSES = "Sedang diproses";
	/** Status: pengajuan disetujui. */
	public static final String DISETUJUI = "Disetujui";
	/** Status: pengajuan ditolak. */
	public static final String DITOLAK = "Ditolak";

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

	/** Kode ringkas kegiatan; lihat {@link #getKode()} untuk perilaku default. */
	private String kode;

	/** Nama kegiatan (Indonesia). */
	private String nama;
	/** Nama kegiatan versi Inggris. */
	private String namaEn;
	/** Keterangan bebas. */
	private String keterangan;
	/** Tempat penyelenggaraan kegiatan. */
	private String tempat;

	/** Kelompok kegiatan kedosenan (klasifikasi utama, wajib). */
	private KelompokKegiatanKedosenan kelompokKegiatanKedosenan;
	/** Sub-klasifikasi di bawah {@link #kelompokKegiatanKedosenan} (wajib). */
	private DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan;

	/** Jabatan/peran dosen dalam kegiatan (mis. peserta/pemateri/panitia). */
	private JabatanKegiatanKedosenan jabatanKegiatanKedosenan;
	/** Skala kegiatan (mis. lokal/nasional/internasional). */
	private SkalaKegiatanKedosenan skalaKegiatanKedosenan;

	/** Tanggal mulai kegiatan. */
	private Date mulai;
	/** Tanggal akhir kegiatan. */
	private Date sampai;

	/** Status alur persetujuan; lihat konstanta {@link #BELUM_DIPROSES}, {@link #SEDANG_DIPROSES}, dst. */
	private String status;
	/** Tautan referensi/bukti kegiatan (URL eksternal). */
	private String url;
	/** Dosen pengaju kegiatan ini. */
	private Dosen diajukanOleh;
	/** Jurusan/program studi terkait kegiatan (opsional). */
	private Jurusan jurusan;
	/** Fakultas terkait kegiatan (opsional). */
	private Fakultas fakultas;
	/** Tahun akademik kegiatan; lihat {@link #getTahunAkademik()} untuk perilaku default. */
	private String tahunAkademik;
	/** Jenis semester (Ganjil/Genap); lihat {@link #getJenisSemester()} untuk perilaku default. */
	private String jenisSemester;
	/** Tahun (angka) kegiatan; lihat {@link #getTahun()} untuk perilaku default. */
	private Integer tahun;
	/** Sertifikat bukti keikutsertaan (opsional). */
	private Sertifikat sertifikat;

	/** Menandai baris ini boleh dipilih pada form terkait; lihat {@link #getBolehDipilih()}. */
	private Boolean bolehDipilih;

	/** Konstruktor kosong, dipakai Hibernate. */
	public KegiatanKedosenan() {
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

	/** @return nama kegiatan, sudah di-{@code trim}; {@code null} bila belum diisi. Wajib unik pada tabel. */
	@Column(name = "nama", nullable = false, length = 255, unique = true)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama kegiatan baru. */
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

	/** @return kelompok kegiatan (klasifikasi utama, wajib); dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_kegiatan_kedosenan", nullable = false)
	public KelompokKegiatanKedosenan getKelompokKegiatanKedosenan() {
		kelompokKegiatanKedosenan = check(kelompokKegiatanKedosenan);
		return kelompokKegiatanKedosenan;
	}

	/** @param kelompokKegiatanKedosenan kelompok kegiatan baru. */
	public void setKelompokKegiatanKedosenan(KelompokKegiatanKedosenan kelompokKegiatanKedosenan) {
		this.kelompokKegiatanKedosenan = kelompokKegiatanKedosenan;
	}

	/** @return tanggal mulai kegiatan, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai;
	}

	/** @param mulai tanggal mulai kegiatan yang baru. */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/** @return tanggal akhir kegiatan, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai;
	}

	/** @param sampai tanggal akhir kegiatan yang baru. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/**
	 * @return kode ringkas kegiatan. Bila belum pernah diisi dan {@link #getId()} sudah ada,
	 *         DIBANGKITKAN SEKALI dari id yang diformat 5 digit berpadding nol (mis. id 42 menjadi
	 *         {@code "00042"}) dan hasilnya DISIMPAN ke field -- baris tanpa id tersimpan (belum
	 *         pernah di-persist) selalu mengembalikan {@code null}.
	 */
	public String getKode() {
		if (id != null && (kode == null || kode.trim().isEmpty())) {
			String k = "0000000000" + id;
			kode = k.substring(k.length() - 5);
		}
		return kode;
	}

	/** @param kode kode ringkas baru; akan tertimpa lagi bila kosong dan id sudah ada. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return sub-klasifikasi di bawah kelompok kegiatan (wajib); dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "detail_kelompok_kegiatan_kedosenan", nullable = false)
	public DetailKelompokKegiatanKedosenan getDetailKelompokKegiatanKedosenan() {
		detailKelompokKegiatanKedosenan = check(detailKelompokKegiatanKedosenan);
		return detailKelompokKegiatanKedosenan;
	}

	/** @param detailKelompokKegiatanKedosenan sub-klasifikasi baru. */
	public void setDetailKelompokKegiatanKedosenan(DetailKelompokKegiatanKedosenan detailKelompokKegiatanKedosenan) {
		this.detailKelompokKegiatanKedosenan = detailKelompokKegiatanKedosenan;
	}

	/** @return status alur persetujuan; default {@link #BELUM_DIPROSES} bila kolom kosong. */
	public String getStatus() {
		return status == null ? BELUM_DIPROSES : status;
	}

	/** @param status status alur persetujuan yang baru. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return dosen pengaju kegiatan, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "diajukan_oleh", nullable = true)
	public Dosen getDiajukanOleh() {
		diajukanOleh = check(diajukanOleh);
		return diajukanOleh;
	}

	/** @param diajukanOleh dosen pengaju yang baru. */
	public void setDiajukanOleh(Dosen diajukanOleh) {
		this.diajukanOleh = diajukanOleh;
	}

	/** @return jurusan/program studi terkait, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/** @param jurusan jurusan/program studi baru. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return fakultas terkait, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas", nullable = true)
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		return fakultas;
	}

	/** @param fakultas fakultas baru. */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

	/** @return tempat penyelenggaraan kegiatan, boleh {@code null}. */
	public String getTempat() {
		return tempat;
	}

	/** @param tempat tempat penyelenggaraan baru. */
	public void setTempat(String tempat) {
		this.tempat = tempat;
	}

	/** @return tautan referensi/bukti kegiatan, boleh {@code null}. */
	@Column(columnDefinition = "text")
	public String getUrl() {
		return url;
	}

	/** @param url tautan referensi/bukti kegiatan yang baru. */
	public void setUrl(String url) {
		this.url = url;
	}

	/** @return jabatan/peran dosen dalam kegiatan, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jabatan_kegiatan_kedosenan", nullable = true)
	public JabatanKegiatanKedosenan getJabatanKegiatanKedosenan() {
		jabatanKegiatanKedosenan = check(jabatanKegiatanKedosenan);
		return jabatanKegiatanKedosenan;
	}

	/** @param jabatanKegiatanKedosenan jabatan/peran baru. */
	public void setJabatanKegiatanKedosenan(JabatanKegiatanKedosenan jabatanKegiatanKedosenan) {
		this.jabatanKegiatanKedosenan = jabatanKegiatanKedosenan;
	}

	/** @return skala kegiatan, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "skala_kegiatan_kedosenan", nullable = true)
	public SkalaKegiatanKedosenan getSkalaKegiatanKedosenan() {
		skalaKegiatanKedosenan = check(skalaKegiatanKedosenan);
		return skalaKegiatanKedosenan;
	}

	/** @param skalaKegiatanKedosenan skala kegiatan baru. */
	public void setSkalaKegiatanKedosenan(SkalaKegiatanKedosenan skalaKegiatanKedosenan) {
		this.skalaKegiatanKedosenan = skalaKegiatanKedosenan;
	}

	/**
	 * @return tahun (angka) kegiatan. Bila {@link #tahunAkademik} terisi, diparse ulang dari
	 *         potongan sebelum {@code "/"} dan MENIMPA field {@link #tahun} setiap pemanggilan
	 *         (kegagalan parse dicatat lewat {@code ErrorAuditUtil} dan diabaikan, nilai lama
	 *         dipertahankan).
	 */
	public Integer getTahun() {
		if (tahunAkademik != null) {
			try {
				tahun = Integer.parseInt(tahunAkademik.split("/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/KegiatanKedosenan.java:278");

			}
		}
		return tahun;
	}

	/** @param tahun tahun (angka) baru; akan tertimpa lagi bila {@link #tahunAkademik} terisi. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * @return tahun akademik; bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#getCurrentTahunAkademik()} lalu disimpan ke field.
	 */
	public String getTahunAkademik() {
		if (tahunAkademik == null) {
			tahunAkademik = Common.getCurrentTahunAkademik();
		}
		return tahunAkademik;
	}

	/** @param tahunAkademik tahun akademik baru. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/**
	 * @return jenis semester (Ganjil/Genap); bila belum pernah diisi, DITENTUKAN SEKALI dari
	 *         {@link Common#isNowSemensterGanjil()} lalu disimpan ke field.
	 */
	public String getJenisSemester() {
		if (jenisSemester == null) {
			jenisSemester = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
		}
		return jenisSemester;
	}

	/** @param jenisSemester jenis semester baru. */
	public void setJenisSemester(String jenisSemester) {
		this.jenisSemester = jenisSemester;
	}

	/** @return sertifikat bukti keikutsertaan, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sertifikat", nullable = true)
	public Sertifikat getSertifikat() {
		sertifikat = check(sertifikat);
		return sertifikat;
	}

	/** @param sertifikat sertifikat bukti keikutsertaan yang baru. */
	public void setSertifikat(Sertifikat sertifikat) {
		this.sertifikat = sertifikat;
	}

	/** @return {@code true} (default) bila baris ini boleh dipilih pada form terkait. */
	public Boolean getBolehDipilih() {
		return bolehDipilih == null ? true : bolehDipilih;
	}

	/** @param bolehDipilih penanda boleh-dipilih yang baru. */
	public void setBolehDipilih(Boolean bolehDipilih) {
		this.bolehDipilih = bolehDipilih;
	}

	/** @return nama kegiatan versi Inggris, boleh {@code null}. */
	@Column(name = "namaen", columnDefinition = "text")
	public String getNamaEn() {
		return namaEn;
	}

	/** @param namaEn nama kegiatan versi Inggris yang baru. */
	public void setNamaEn(String namaEn) {
		this.namaEn = namaEn;
	}
}
