package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
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

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

/**
 * Model data untuk satu penugasan mengajar dosen pada mata kuliah/kelas tertentu di suatu
 * tahun akademik dan semester (surat tugas mengajar). Tipe ini membawa state yang dipertukarkan
 * oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta
 * relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Jurusan jurusan}, {@code Date tanggalSuratTugas}, {@code
 * Date tmtSuratTugas}, {@code Dosen dosen}, {@code Integer sks}; pemetaan persistence: tabel
 * {@code public.penugasan_dosen_mengajar}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getNama()}, {@code getJurusan()}, {@code getTahun()}); mutasi
 * data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setJurusan()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Catatan getter yang menulis field ({@code getNama()}, {@code getTahun()}):</b> keduanya membaca field
 * lain lalu MENIMPA field-nya sendiri sebagai efek samping sebelum mengembalikan nilai -- pola berulang di
 * puluhan entity AIS ({@code ais-getter-mutasi-field-anti-pattern-sistemik}), bukan cacat unik kelas ini.</p>
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
@Table(schema = "public", name = "penugasan_dosen_mengajar")
public class PenugasanDosenMengajar extends GeneralValueObject {

	/** Versi serialisasi Java untuk kompatibilitas {@code Serializable}. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key entity (kolom {@code id}, identity/auto-increment). */
	private Long id;
	/**
	 * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
	 * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini, bukan
	 * milik induk, sehingga Hibernate memetakan kolomnya langsung pada subclass ini.
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
	 * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam agar
	 * jejak audit yang sudah terisi tidak tertimpa kosong oleh jalur simpan tanpa info pengguna
	 * (mis. proses batch). Perilaku sama seperti {@link GeneralValueObject#setOlehId(String)}.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Validasi sama seperti {@link #setOlehId(String)}:
	 * nilai {@code null}/kosong diabaikan diam-diam.
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

	/** Nama tampilan penugasan; SELALU ditimpa ulang oleh {@link #getNama()} dari tahun akademik + semester. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;
	/** Jurusan/program studi tempat penugasan berlaku (opsional). */
	private Jurusan jurusan;
	/** Kode ringkas penugasan (bebas, bukan primary key). */
	private String kode;
	/** Tanggal surat tugas diterbitkan. */
	private Date tanggalSuratTugas;
	/** Tanggal mulai berlaku (TMT) surat tugas. */
	private Date tmtSuratTugas;
	/** Nama program studi/jenjang dalam bentuk teks bebas (pelengkap {@link #getJurusan()}). */
	private String program;
	/** Tahun akademik penugasan, format {@code "YYYY/YYYY"}. */
	private String tahunAkademik;
	/** Tahun (angka) hasil parse dari {@link #tahunAkademik}; lihat {@link #getTahun()}. */
	private Integer tahun;
	/** Semester penugasan (teks bebas, mis. "Ganjil"/"Genap"). */
	private String semester;
	/** Dosen yang mendapat penugasan mengajar ini. */
	private Dosen dosen;
	/** Beban SKS penugasan. */
	private Integer sks;

	/** Kode integrasi dari sistem feeder (PDDIKTI/Neo Feeder), bila baris ini disinkronkan dari sana. */
	private String feeder;

	/** Konstruktor kosong, dipakai Hibernate. */
	public PenugasanDosenMengajar() {
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

	/**
	 * @return nama tampilan; SEBELUM dikembalikan, field ini ditimpa ulang dengan
	 *         {@code getTahunAkademik() + "-" + getSemester()} -- nilai apa pun yang disetel lewat
	 *         {@link #setNama(String)} akan selalu tertimpa oleh gabungan ini pada pembacaan berikutnya.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		nama = getTahunAkademik() + "-" + getSemester();
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan; akan tertimpa lagi pada pemanggilan {@link #getNama()} berikutnya. */
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

	/** @return jurusan/program studi terkait penugasan; dimuat lazy lewat sesi Hibernate aktif. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jurusan", nullable = true)
	public Jurusan getJurusan() {
		return jurusan;
	}

	/** @param jurusan jurusan/program studi baru. */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/** @return kode ringkas, string kosong (bukan {@code null}) bila belum diisi. */
	public String getKode() {
		return kode == null ? "" : kode.trim();
	}

	/** @param kode kode ringkas yang baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return tanggal surat tugas diterbitkan. */
	@Temporal(TemporalType.DATE)
	public Date getTanggalSuratTugas() {
		return tanggalSuratTugas;
	}

	/** @param tanggalSuratTugas tanggal surat tugas yang baru. */
	public void setTanggalSuratTugas(Date tanggalSuratTugas) {
		this.tanggalSuratTugas = tanggalSuratTugas;
	}

	/** @return tanggal mulai berlaku (TMT) surat tugas. */
	@Temporal(TemporalType.DATE)
	public Date getTmtSuratTugas() {
		return tmtSuratTugas;
	}

	/** @param tmtSuratTugas TMT surat tugas yang baru. */
	public void setTmtSuratTugas(Date tmtSuratTugas) {
		this.tmtSuratTugas = tmtSuratTugas;
	}

	/** @return nama program/jenjang dalam bentuk teks bebas. */
	public String getProgram() {
		return program;
	}

	/** @param program nama program/jenjang baru. */
	public void setProgram(String program) {
		this.program = program;
	}

	/** @return tahun akademik format {@code "YYYY/YYYY"}, bisa {@code null} bila belum diisi. */
	public String getTahunAkademik() {
		return tahunAkademik;
	}

	/** @param tahunAkademik tahun akademik baru. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/** @return semester penugasan (teks bebas). */
	public String getSemester() {
		return semester;
	}

	/** @param semester semester baru. */
	public void setSemester(String semester) {
		this.semester = semester;
	}

	/** @return dosen yang mendapat penugasan; dimuat lazy lewat sesi Hibernate aktif. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		return dosen;
	}

	/** @param dosen dosen baru yang mendapat penugasan. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return tahun (angka) penugasan. Bila {@link #tahunAkademik} terisi, diparse ulang dari
	 *         potongan sebelum {@code "/"} dan MENIMPA field {@link #tahun} setiap pemanggilan
	 *         (kegagalan parse dicatat lewat {@code ErrorAuditUtil} dan diabaikan); bila hasilnya
	 *         masih {@code null}, dipakai tahun kalender berjalan sebagai cadangan.
	 */
	public Integer getTahun() {
		if (getTahunAkademik() != null) {
			try {
				tahun = Integer.parseInt(StringUtils.split(getTahunAkademik(), "/")[0]);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/PenugasanDosenMengajar.java:201");

			}
		}
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/** @param tahun tahun (angka) baru; akan tertimpa lagi bila {@link #tahunAkademik} terisi. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/** @return kode integrasi feeder, {@code null} bila kosong/belum diisi (bukan string kosong). */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/** @param feeder kode integrasi feeder yang baru. */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/** @return beban SKS penugasan, boleh {@code null}. */
	public Integer getSks() {
		return sks;
	}

	/** @param sks beban SKS baru. */
	public void setSks(Integer sks) {
		this.sks = sks;
	}

}
