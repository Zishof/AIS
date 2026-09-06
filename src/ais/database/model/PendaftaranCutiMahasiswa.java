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

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Model data untuk satu PENDAFTARAN CUTI mahasiswa (cuti akademik/penangguhan studi), berbeda
 * TOTAL dari {@link ais.database.model.payroll.CutiDanIzin} (cuti/izin PEGAWAI) -- keduanya
 * entity independen di modul berbeda (akademik vs kepegawaian), berbeda tabel, tidak berelasi FK
 * sama sekali, dan kebetulan hanya berbagi konsep umum "cuti". Kelas ini {@code extends}
 * {@link ais.database.model.sop.DataSop} sehingga pendaftaran cuti mengalir lewat mesin disposisi
 * SOP untuk persetujuannya, dan semester cuti dihitung otomatis dari angkatan mahasiswa. Tipe ini
 * membawa state yang dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis
 * utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki
 * {@link ais.database.model.sop.DataSop} (dan transitif {@link GeneralValueObject}). Kelas ini hanya boleh
 * memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang berlaku bagi seluruh keluarga
 * harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Mahasiswa mahasiswa}, {@code String tahunAkademik},
 * {@code String ganjilGenap}, {@code Boolean persetujuan}, {@code DisposisiSop disposisiSop}, {@code Boolean
 * aktif}; pemetaan persistence: tabel {@code public.pendaftaran_cuti_mahasiswa}; pembacaan/pencarian ({@code
 * getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()}, {@code getMahasiswa()},
 * {@code getSemester()}, {@code getDisposisiSop()}); mutasi data ({@code setOlehId()}, {@code setId()}, {@code
 * setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setMahasiswa()}). Bagian lain dari
 * kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Setter {@link #setDisposisiSop(DisposisiSop)} bersifat WRITE-ONCE</b> -- pola yang sama persis dengan
 * {@link CatatanAdministrasi#setDisposisiSop(DisposisiSop)}: argumen {@code null}/tanpa id diabaikan, dan
 * sekali {@link #disposisiSop} terisi, tidak bisa diganti lagi lewat setter ini.</p>
 * <p><b>Catatan getter yang menulis field ({@code getSemester()}, {@code getAktif()}):</b> {@code getSemester()}
 * menghitung ulang dari {@link Common#getSemester} berdasar tahun angkatan mahasiswa setiap kali dipanggil bila
 * prasyaratnya lengkap; {@code getAktif()} bisa MENGUNCI dirinya ke {@code false} permanen begitu disposisi
 * menandakan tidak aktif/ditolak -- pola yang sama seperti {@link CatatanAdministrasi#getAktif()}, keduanya
 * instance dari pola sistemik {@code ais-getter-mutasi-field-anti-pattern-sistemik}.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see ais.database.model.sop.DataSop
 * @see ais.database.model.payroll.CutiDanIzin entity cuti/izin PEGAWAI, independen total dari kelas ini
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "pendaftaran_cuti_mahasiswa")
public class PendaftaranCutiMahasiswa extends DataSop {

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

	/** @return representasi ringkas untuk debug/log: isi {@link #getKeterangan()} apa adanya. */
	public String toString() {
		return keterangan;
	}

	/** Kode ringkas pendaftaran; lihat {@link #getKode()}. */
	private String kode;
	/** Mahasiswa pengaju cuti. */
	private Mahasiswa mahasiswa;
	/** Semester cuti (dihitung); lihat {@link #getSemester()} untuk perilaku turunan dari angkatan. */
	private Integer semester;
	/** Tahap pendaftaran (bila proses cuti berjenjang), boleh {@code null}. */
	private Integer tahap;
	/** Alasan/keterangan pengajuan cuti. */
	private String keterangan;
	/** Tahun akademik pengajuan; lihat {@link #getTahunAkademik()} untuk perilaku default. */
	private String tahunAkademik;
	/** Jenis semester (Ganjil/Genap) pengajuan; lihat {@link #getGanjilGenap()} untuk perilaku default. */
	private String ganjilGenap;
	/** Status persetujuan; lihat {@link #getPersetujuan()} untuk perilaku turunan dari disposisi. */
	private Boolean persetujuan;
	/** Menandai pengajuan cuti untuk semester pendek. */
	private Boolean semesterPendek;
	/** Tanggal pengajuan; default saat object dibuat, lihat {@link #getTanggal()} untuk perilaku lanjutan. */
	private Date tanggal = ais.ui.util.WaktuUtil.getDate();
	/** Baris disposisi SOP tempat pengajuan ini mengalir; lihat {@link #setDisposisiSop(DisposisiSop)} soal write-once. */
	private DisposisiSop disposisiSop;
	/** Menandai pengajuan masih aktif/berlaku; lihat {@link #getAktif()} untuk perilaku turunan dari disposisi. */
	private Boolean aktif;

	/** Konstruktor kosong, dipakai Hibernate. */
	public PendaftaranCutiMahasiswa() {
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

	/** @return alasan/keterangan pengajuan cuti, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan alasan/keterangan pengajuan cuti yang baru. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param mahasiswa mahasiswa pengaju cuti yang baru. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return mahasiswa pengaju cuti, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param semester semester cuti baru; bisa tertimpa lagi oleh {@link #getSemester()}. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * @return semester cuti. Bila {@link #mahasiswa}, {@link #ganjilGenap}, dan
	 *         {@link #tahunAkademik} semuanya terisi, field ini DIHITUNG ULANG setiap pemanggilan
	 *         lewat {@link Common#getSemester} berdasarkan tahun angkatan mahasiswa, dan MENIMPA
	 *         nilai lama.
	 */
	public Integer getSemester() {
		if (mahasiswa != null && ganjilGenap != null && tahunAkademik != null) {
			final Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String ta = tahunAkademik;
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			semester = Common.getSemester(tahunAngkatanMhs, ganjilGenap, mahasiswa.getPindahKeKampusIniMasukSemester(),
					tahun, mahasiswa.getSemesterMulai());
		}

		return semester;
	}

	/** @return tahun akademik pengajuan; default {@link Common#getCurrentTahunAkademik()} bila belum diisi. */
	@Column(name = "tahun_akademik", nullable = true)
	public String getTahunAkademik() {
		return tahunAkademik == null ? Common.getCurrentTahunAkademik() : tahunAkademik;
	}

	/** @param tahunAkademik tahun akademik baru. */
	public void setTahunAkademik(String tahunAkademik) {
		this.tahunAkademik = tahunAkademik;
	}

	/** @return jenis semester (Ganjil/Genap); default ditentukan dari kalender berjalan bila belum diisi. */
	@Column(name = "ganjil_genap", nullable = true)
	public String getGanjilGenap() {
		return ganjilGenap == null ? (Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP)
				: ganjilGenap;
	}

	/** @param ganjilGenap jenis semester baru. */
	public void setGanjilGenap(String ganjilGenap) {
		this.ganjilGenap = ganjilGenap;
	}

	/**
	 * @return status persetujuan. Default {@code true} bila belum pernah diisi; kemudian
	 *         DITENTUKAN ULANG dari {@link #getDisposisiSop()}: {@code true} bila disposisi
	 *         menyetujui (ada {@code disposisiSetuju} dengan pengaju), {@code false} bila
	 *         disposisi ada tapi belum/tidak disetujui.
	 */
	public Boolean getPersetujuan() {
		if (persetujuan == null) {
			persetujuan = true;
		}

		if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
				&& getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
			persetujuan = true;
		}

		if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
				|| getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
			persetujuan = false;
		}

		return persetujuan;
	}

	/** @param persetujuan status persetujuan baru; bisa tertimpa lagi oleh {@link #getPersetujuan()}. */
	public void setPersetujuan(Boolean persetujuan) {
		this.persetujuan = persetujuan;
	}

	/** @return tanggal pengajuan; bila {@code null}, DIISI SEKALI dari {@link #getTanggal_dirubah()}. */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		if (tanggal == null) {
			tanggal = getTanggal_dirubah();
		}
		return tanggal;
	}

	/** @param tanggal tanggal pengajuan yang baru. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return tahap pendaftaran, boleh {@code null}. */
	public Integer getTahap() {
		return tahap;
	}

	/** @param tahap tahap pendaftaran baru. */
	public void setTahap(Integer tahap) {
		this.tahap = tahap;
	}

	/** @return {@code true} bila pengajuan untuk semester pendek; default {@code false}. */
	public Boolean getSemesterPendek() {
		return semesterPendek == null ? false : semesterPendek;
	}

	/** @param semesterPendek penanda semester pendek yang baru. */
	public void setSemesterPendek(Boolean semesterPendek) {
		this.semesterPendek = semesterPendek;
	}

	/** @return baris disposisi SOP tempat pengajuan ini mengalir, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP -- WRITE-ONCE, pola sama seperti
	 * {@link CatatanAdministrasi#setDisposisiSop(DisposisiSop)}: argumen {@code null}/tanpa id
	 * diabaikan diam-diam, dan sekali {@link #disposisiSop} terisi tidak bisa diganti lagi.
	 *
	 * @param disposisiSop disposisi SOP baru; efektif hanya pada pengisian PERTAMA kali.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * @return {@code true} (default) bila pengajuan masih aktif/berlaku. Field {@link #aktif}
	 *         bisa DITIMPA PERMANEN menjadi {@code false} bila {@link #getDisposisiSop()}
	 *         menandakan tidak aktif atau alurnya sampai pada titik penolakan -- pola sama
	 *         seperti {@link CatatanAdministrasi#getAktif()}.
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/** @param aktif penanda aktif/berlaku yang baru; bisa tertimpa lagi oleh {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return kode ringkas pendaftaran, boleh {@code null}. */
	public String getKode() {
		return kode;
	}

	/** @param kode kode ringkas baru. */
	public void setKode(String kode) {
		this.kode = kode;
	}
}
