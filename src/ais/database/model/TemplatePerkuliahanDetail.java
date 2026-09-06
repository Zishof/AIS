package ais.database.model;

// Generated Dec 12, 2009 7:42:38 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.envers.Audited;

import ais.common.Common;

/**
 * Model data untuk satu BARIS DETAIL jadwal dalam {@link TemplatePerkuliahan}: satu kelas
 * konkret dari satu {@link Matakuliah} pada hari/jam/ruang tertentu, dengan hingga dua dosen
 * pengampu ({@link #getDosen1()}/{@link #getDosen2()}). Mendukung kelas PARALEL (rombongan
 * belajar ganda untuk mata kuliah yang sama, lihat {@link #getMerupakan_paralel()}/{@link
 * #getPerkuliahan_paralel()}) serta tiga varian "tanpa" untuk kelas yang sengaja belum lengkap
 * jadwalnya ({@link #getMerupakan_tanpa_jadwal_perkuliahan()}, {@link #getMerupakan_tanpa_dosen()},
 * {@link #getMerupakan_tanpa_ruangan()}). Waktu mulai/selesai disimpan GANDA: sebagai teks
 * {@code "HH.mm"} ({@link #getWaktuMulai()}/{@link #getWaktuSelesai()}) DAN sebagai angka
 * desimal turunan ({@link #getWaktuMulaiD()}/{@link #getWaktuSelesaiD()}) untuk keperluan
 * pengurutan/perhitungan numerik. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Matakuliah matakuliah}, {@code Dosen dosen1}, {@code
 * Dosen dosen2}, {@code Ruang ruang}, {@code String waktuMulai}, {@code String waktuSelesai}, {@code
 * TemplatePerkuliahan templatePerkuliahan}; pemetaan persistence: tabel
 * {@code public.template_perkuliahan_detail}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()},
 * {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getMatakuliah()}, {@code getWaktuMulaiD()}, {@code
 * getRuang()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()},
 * {@code setTanggal_dirubah()}, {@code setMatakuliah()}). Bagian lain dari kontrak tetap mengikuti kelas induk
 * atau interface yang disebut di atas.</p>
 * <p><b>Konstruktor {@link #TemplatePerkuliahanDetail(Long)}:</b> membangun instance HANYA dengan id
 * terisi (tanpa memuat sisanya dari database) -- dipakai sebagai referensi ringan (mis. sebagai nilai
 * {@link #getPerkuliahan_paralel()}) tanpa perlu query penuh, mengandalkan proxy/lazy-load Hibernate saat
 * field lainnya benar-benar diakses.</p>
 * <p><b>Getter yang menulis field ({@code getWaktuMulaiD()}, {@code getWaktuSelesaiD()}):</b> keduanya
 * MENGURAI ULANG {@link #waktuMulai}/{@link #waktuSelesai} (teks) menjadi {@code Double} setiap pemanggilan
 * dan MENIMPA field angkanya -- kegagalan parse (mis. format bukan angka) ditangkap dan ditampilkan lewat
 * {@code Common.tampilErrorJikaAdmin} sehingga nilai lama tetap dipertahankan; pola berulang di puluhan
 * entity AIS ({@code ais-getter-mutasi-field-anti-pattern-sistemik}).</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see TemplatePerkuliahan induk template yang menaungi baris detail ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "template_perkuliahan_detail")

public class TemplatePerkuliahanDetail extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = -6970840500825359503L;

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

	/** @return representasi ringkas untuk debug/log: {@link #getMatakuliah()} yang dikonversi ke {@code String}. */
	public String toString() {
		return matakuliah + "";
	}

	/** Mata kuliah yang dijadwalkan pada baris detail ini (wajib). */
	private Matakuliah matakuliah;
	/** Dosen pengampu pertama. */
	private Dosen dosen1;
	/** Dosen pengampu kedua (kelas team-teaching), boleh {@code null}. */
	private Dosen dosen2;

	/** Jurusan/program studi terkait kelas ini (opsional). */
	private Jurusan jurusan;
	/** Ruang tempat kelas berlangsung. */
	private Ruang ruang;
	/** Semester kelas ini ditawarkan. */
	private Integer semester;
	/** Kapasitas maksimum peserta kelas. */
	private Integer kapasitasKelas;

	/** Nama program (teks bebas), pelengkap {@link #getJurusan()}. */
	private String program;

	/** Jam mulai (teks {@code "HH.mm"}); default {@code "00.00"}. */
	private String waktuMulai = "00.00";
	/** Jam selesai (teks {@code "HH.mm"}); default {@code "00.00"}. */
	private String waktuSelesai = "00.00";
	/** Hari kelas berlangsung; default {@code "Senin"}. */
	private String hari = "Senin";
	/** Kode kelas/rombongan belajar (mis. "A", "B"); default {@code "A"}. */
	private String kelas = "A";

	/** Sesi waktu (mis. "PAGI"/"SIANG"/"MALAM"); default {@code "PAGI"}. */
	private String waktu = "PAGI";

	/** Versi angka dari {@link #waktuMulai}; lihat {@link #getWaktuMulaiD()} untuk cara pengisiannya. */
	private Double waktuMulaiD = null;
	/** Versi angka dari {@link #waktuSelesai}; lihat {@link #getWaktuSelesaiD()} untuk cara pengisiannya. */
	private Double waktuSelesaiD = null;

	/** Kurikulum yang menaungi mata kuliah pada kelas ini (opsional). */
	private Kurikulum kurikulum;
	/** Template perkuliahan induk yang menaungi baris detail ini. */
	private TemplatePerkuliahan templatePerkuliahan;

	/** Warna tampilan kelas ini pada jadwal (kode warna teks bebas). */
	private String warna;

	/** Menandai baris ini adalah salah satu dari rombongan belajar PARALEL. */
	private Boolean merupakan_paralel;
	/** Referensi ke baris detail pasangan paralel (bila {@link #merupakan_paralel} {@code true}). */
	private TemplatePerkuliahanDetail perkuliahan_paralel;
	/** Menandai kelas ini SENGAJA belum punya jadwal (hari/jam) definitif; default {@code false}. */
	private Boolean merupakan_tanpa_jadwal_perkuliahan = false;
	/** Menandai kelas ini SENGAJA belum punya dosen pengampu; default {@code false}. */
	private Boolean merupakan_tanpa_dosen = false;
	/** Menandai kelas ini SENGAJA belum punya ruang; default {@code false}. */
	private Boolean merupakan_tanpa_ruangan = false;
	/** Slot jam perkuliahan baku (opsional, alternatif dari waktu mulai/selesai bebas). */
	private JamPerkuliahan jamPerkuliahan;

	/** Konstruktor kosong, dipakai Hibernate. */
	public TemplatePerkuliahanDetail() {

	}

	/**
	 * Membangun instance HANYA dengan id terisi, tanpa memuat field lain dari database --
	 * dipakai sebagai referensi ringan yang mengandalkan proxy/lazy-load Hibernate.
	 *
	 * @param id id baris detail yang direferensikan.
	 */
	public TemplatePerkuliahanDetail(Long id) {
		this.id = id;
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

	/** @return mata kuliah yang dijadwalkan (wajib). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "matakuliah", nullable = false)
	public Matakuliah getMatakuliah() {
		return this.matakuliah;
	}

	/** @param matakuliah mata kuliah baru. */
	public void setMatakuliah(Matakuliah matakuliah) {
		this.matakuliah = matakuliah;
	}

	/** @return dosen pengampu pertama, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen1", nullable = true)
	public Dosen getDosen1() {
		return this.dosen1;
	}

	/** @param dosen1 dosen pengampu pertama yang baru. */
	public void setDosen1(Dosen dosen1) {
		this.dosen1 = dosen1;
	}

	/** @return dosen pengampu kedua (team-teaching), boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen2", nullable = true)
	public Dosen getDosen2() {
		return this.dosen2;
	}

	/** @param dosen2 dosen pengampu kedua yang baru. */
	public void setDosen2(Dosen dosen2) {
		this.dosen2 = dosen2;
	}

	/** @return ruang tempat kelas berlangsung, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang")
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/** @param ruang ruang baru. */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/** @return semester kelas ditawarkan, boleh {@code null}. */
	@Column(name = "semester")
	public Integer getSemester() {
		return this.semester;
	}

	/** @param semester semester baru. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/** @param jurusan jurusan/program studi terkait yang baru. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return jurusan/program studi terkait, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/** @param waktuMulai jam mulai (teks {@code "HH.mm"}) baru; disimpan {@code null} bila kosong/spasi saja. */
	public void setWaktuMulai(String waktuMulai) {
		this.waktuMulai = waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/** @return jam mulai (teks {@code "HH.mm"}), {@code null} bila kosong. */
	@Column(name = "waktu_mulai", length = 20)
	public String getWaktuMulai() {
		return waktuMulai == null || waktuMulai.trim().equals("") ? null : waktuMulai.trim();
	}

	/** @param waktuSelesai jam selesai (teks {@code "HH.mm"}) baru; disimpan {@code null} bila kosong/spasi saja. */
	public void setWaktuSelesai(String waktuSelesai) {
		this.waktuSelesai = waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/** @return jam selesai (teks {@code "HH.mm"}), {@code null} bila kosong. */
	@Column(name = "waktu_selesai", length = 20)
	public String getWaktuSelesai() {
		return waktuSelesai == null || waktuSelesai.trim().equals("") ? null : waktuSelesai.trim();
	}

	/** @param hari hari kelas berlangsung yang baru. */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/** @return hari kelas berlangsung; default {@code "Senin"}. */
	@Column(name = "hari", length = 20)
	public String getHari() {
		return hari;
	}

	/** @param kelas kode kelas/rombongan belajar yang baru. */
	public void setKelas(String kelas) {
		this.kelas = kelas;
	}

	/** @return kode kelas/rombongan belajar; default {@code "A"}. */
	@Column(name = "kelas", length = 20)
	public String getKelas() {
		return kelas;
	}

	/** @param waktu sesi waktu (PAGI/SIANG/MALAM) yang baru. */
	public void setWaktu(String waktu) {
		this.waktu = waktu;
	}

	/** @return sesi waktu (PAGI/SIANG/MALAM); default {@code "PAGI"}. */
	@Column(name = "waktu", length = 20)
	public String getWaktu() {
		return waktu;
	}

	/** @param program nama program (teks bebas) yang baru. */
	public void setProgram(String program) {
		this.program = program;
	}

	/** @return nama program (teks bebas), boleh {@code null}. */
	@Column(name = "program", length = 20)
	public String getProgram() {
		return program;
	}

	/** @param waktuSelesaiD versi angka jam selesai baru; bisa tertimpa lagi oleh {@link #getWaktuSelesaiD()}. */
	public void setWaktuSelesaiD(Double waktuSelesaiD) {
		this.waktuSelesaiD = waktuSelesaiD;
	}

	/**
	 * @return versi angka {@link #waktuSelesai}. Bila {@link #waktuSelesai} terisi, DIURAI ULANG
	 *         lewat {@code Double.parseDouble} setiap pemanggilan dan MENIMPA field ini; kegagalan
	 *         parse ditangkap dan ditampilkan lewat {@code Common.tampilErrorJikaAdmin}, nilai lama
	 *         dipertahankan.
	 */
	@Column(name = "waktu_selesai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuSelesaiD() {
		if (waktuSelesai != null && !waktuSelesai.trim().equals("")) {
			try {
				waktuSelesaiD = Double.parseDouble(waktuSelesai.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return waktuSelesaiD;
	}

	/** @param waktuMulaiD versi angka jam mulai baru; bisa tertimpa lagi oleh {@link #getWaktuMulaiD()}. */
	public void setWaktuMulaiD(Double waktuMulaiD) {
		this.waktuMulaiD = waktuMulaiD;
	}

	/**
	 * @return versi angka {@link #waktuMulai}. Bila {@link #waktuMulai} terisi, DIURAI ULANG lewat
	 *         {@code Double.parseDouble} setiap pemanggilan dan MENIMPA field ini; kegagalan parse
	 *         ditangkap dan ditampilkan lewat {@code Common.tampilErrorJikaAdmin}, nilai lama
	 *         dipertahankan.
	 */
	@Column(name = "waktu_mulai_d", length = 15, precision = 15, scale = 2)
	public Double getWaktuMulaiD() {
		if (waktuMulai != null && !waktuMulai.trim().equals("")) {
			try {
				waktuMulaiD = Double.parseDouble(waktuMulai.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}
		}
		return waktuMulaiD;
	}

	/** @param kurikulum kurikulum terkait yang baru. */
	public void setKurikulum(Kurikulum kurikulum) {
		this.kurikulum = kurikulum;
	}

	/** @return kurikulum terkait, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kurikulum", nullable = true)
	public Kurikulum getKurikulum() {
		return kurikulum;
	}

	/** @param warna kode warna tampilan yang baru. */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/** @return kode warna tampilan pada jadwal, boleh {@code null}. */
	@Column(name = "warna", nullable = true, length = 20)
	public String getWarna() {
		return warna;
	}

	/** @param merupakan_paralel penanda kelas paralel yang baru. */
	public void setMerupakan_paralel(Boolean merupakan_paralel) {
		this.merupakan_paralel = merupakan_paralel;
	}

	/** @return {@code true} bila baris ini adalah salah satu rombongan belajar paralel. */
	public Boolean getMerupakan_paralel() {
		return merupakan_paralel;
	}

	/** @param perkuliahan_paralel referensi baris detail pasangan paralel yang baru. */
	public void setPerkuliahan_paralel(TemplatePerkuliahanDetail perkuliahan_paralel) {
		this.perkuliahan_paralel = perkuliahan_paralel;
	}

	/** @return referensi baris detail pasangan paralel, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "perkuliahan_paralel", nullable = true)
	public TemplatePerkuliahanDetail getPerkuliahan_paralel() {
		return perkuliahan_paralel;
	}

	/** @param merupakan_tanpa_jadwal_perkuliahan penanda tanpa-jadwal yang baru. */
	public void setMerupakan_tanpa_jadwal_perkuliahan(Boolean merupakan_tanpa_jadwal_perkuliahan) {
		this.merupakan_tanpa_jadwal_perkuliahan = merupakan_tanpa_jadwal_perkuliahan;
	}

	/** @return {@code true} bila kelas ini sengaja belum punya jadwal (hari/jam) definitif. */
	public Boolean getMerupakan_tanpa_jadwal_perkuliahan() {
		return merupakan_tanpa_jadwal_perkuliahan;
	}

	/** @param merupakan_tanpa_dosen penanda tanpa-dosen yang baru. */
	public void setMerupakan_tanpa_dosen(Boolean merupakan_tanpa_dosen) {
		this.merupakan_tanpa_dosen = merupakan_tanpa_dosen;
	}

	/** @return {@code true} bila kelas ini sengaja belum punya dosen pengampu. */
	public Boolean getMerupakan_tanpa_dosen() {
		return merupakan_tanpa_dosen;
	}

	/** @param merupakan_tanpa_ruangan penanda tanpa-ruangan yang baru. */
	public void setMerupakan_tanpa_ruangan(Boolean merupakan_tanpa_ruangan) {
		this.merupakan_tanpa_ruangan = merupakan_tanpa_ruangan;
	}

	/** @return {@code true} bila kelas ini sengaja belum punya ruang. */
	public Boolean getMerupakan_tanpa_ruangan() {
		return merupakan_tanpa_ruangan;
	}

	/** @return template perkuliahan induk yang menaungi baris detail ini, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "template_perkuliahan", nullable = true)
	public TemplatePerkuliahan getTemplatePerkuliahan() {
		return templatePerkuliahan;
	}

	/** @param templatePerkuliahan template perkuliahan induk yang baru. */
	public void setTemplatePerkuliahan(TemplatePerkuliahan templatePerkuliahan) {
		this.templatePerkuliahan = templatePerkuliahan;
	}

	/** @return kapasitas maksimum peserta kelas, boleh {@code null}. */
	public Integer getKapasitasKelas() {
		return kapasitasKelas;
	}

	/** @param kapasitasKelas kapasitas maksimum peserta kelas yang baru. */
	public void setKapasitasKelas(Integer kapasitasKelas) {
		this.kapasitasKelas = kapasitasKelas;
	}

	/** @return slot jam perkuliahan baku, boleh {@code null}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jam_perkuliahan", nullable = true)
	public JamPerkuliahan getJamPerkuliahan() {
		return jamPerkuliahan;
	}

	/** @param jamPerkuliahan slot jam perkuliahan baku yang baru. */
	public void setJamPerkuliahan(JamPerkuliahan jamPerkuliahan) {
		this.jamPerkuliahan = jamPerkuliahan;
	}

}
