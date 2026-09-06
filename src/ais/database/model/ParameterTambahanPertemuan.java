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

/**
 * Entity Hibernate/JPA untuk tabel {@code public.parameter_tambahan_pertemuan} — baris
 * <b>definisi</b> satu {@link ParameterTambahan} (field kustom/dinamis) untuk konteks
 * "Pertemuan" dan modul-modul terkaitnya: menentukan urutan tampil ({@link #getNomorUrut()},
 * diturunkan dari {@link ParameterTambahan} induk), posisi kolom ({@link #getKolomKe()}), dan
 * SEBELAS flag boolean independen yang masing-masing menyalakan/mematikan kemunculan field ini
 * pada satu form/modul: {@link #getPerkuliahan()}, {@link #getJadwalUjianPMB()}, {@link
 * #getMahasiswaRequestTugasAkhir()}, {@link #getKelompokKkn()}, {@link #getKelompokPkl()},
 * {@link #getSkripsi()}, {@link #getKrsMahasiswa()}, {@link #getJadwalUjianPSB()}, {@link
 * #getJadwalPelajaran()}, {@link #getPertemuanPunyaGrupPertemuan()}, {@link
 * #getFormulirKegiatan()}.
 *
 * <p><b>Ini adalah sisi DEFINISI, bukan sisi NILAI.</b> Entity ini hanya menyatakan "field
 * kustom mana yang aktif di form mana"; nilai aktual yang diisi pengguna untuk satu {@code
 * Pertemuan} tertentu disimpan lewat mekanisme {@code ParameterTambahanAstract} pada entity
 * konsumen ({@link Pertemuan}, method {@code populateParameterTambahan(List)}), BUKAN pada
 * baris entity ini. Pola kunci/tipe atribut ZK yang salah pasang antar modul pada mekanisme
 * itu (mis. baris ZK menyimpan objek dengan kunci/tipe milik modul LAIN, membuat data kembali
 * kosong tiap simpan) sudah pernah ditemukan dan diperbaiki pada satu entity konsumen ({@code
 * payroll.PengajuanTransaksiPegawai}, r84074), dengan 6 kandidat lain ditandai belum diperiksa
 * ({@code employ.CutiDanIzin}, {@code CatatanAdministrasi}, {@code CatatanMahasiswa}, {@code
 * CatatanPegawai}, {@code IsiAngketParameterUmum}, {@code KegiatanSiswa}). {@link Pertemuan}
 * SENDIRI tidak termasuk daftar itu; verifikasi langsung menunjukkan {@code
 * Pertemuan.populateParameterTambahan} membaca atribut ZK {@code
 * "kelompokParameterTambahanPertemuan"} bertipe {@link KelompokParameterTambahanPertemuan} —
 * cocok persis dengan kunci/tipe yang ditulis {@code ParameterTambahanPertemuanListener} milik
 * modul yang SAMA, sehingga {@link Pertemuan} tampak konsisten/aman terhadap pola bug ini
 * (bukan bagian dari daftar 6 kandidat yang sudah tercatat sebelumnya, melainkan verifikasi
 * independen pada sesi dokumentasi ini).</p>
 *
 * @see KelompokParameterTambahanPertemuan
 * @see ParameterTambahan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "parameter_tambahan_pertemuan")
public class ParameterTambahanPertemuan extends GeneralValueObject {

	/**
	 * ID versi serialisasi Java untuk kompatibilitas antar build (bukan kolom database).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key baris {@code parameter_tambahan_pertemuan}, kolom {@code id} (identity, auto-generate). */
	private Long id;
	/** Nama/username aktor yang membuat/terakhir mengubah baris ini (field audit longgar, bukan FK). */
	private String oleh;
	/** ID aktor yang membuat/terakhir mengubah baris ini (pasangan {@link #oleh}, bukan FK). */
	private String olehId;

	/**
	 * @return ID aktor ({@link #olehId}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID aktor audit. Setter ini <b>fail-closed diam-diam</b>: nilai {@code null} atau
	 * string kosong/berspasi diabaikan sepenuhnya (nilai lama tetap dipertahankan), tanpa
	 * exception maupun log.
	 *
	 * @param olehId ID aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama aktor audit. Sama seperti {@link #setOlehId(String)}: nilai {@code null} atau
	 * kosong/berspasi diabaikan diam-diam, nilai lama dipertahankan.
	 *
	 * @param oleh nama aktor baru; nilai kosong/{@code null} tidak berefek
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama aktor ({@link #oleh}) yang tercatat membuat/mengubah baris ini; boleh {@code null}.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum {@code
	 * UPDATE} dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
	 * untuk memperbarui jejak audit "terakhir diubah" milik entity ini.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu "terakhir diubah" secara manual. Field ini juga diinisialisasi ke
	 * waktu sekarang saat instance dibuat, dan ditulis ulang otomatis oleh {@link #onUpdate()}
	 * setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return stempel waktu terakhir baris ini diubah (kolom timestamp), diisi otomatis oleh
	 *         {@link #onUpdate()} pada setiap {@code UPDATE}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Definisi field kustom/dinamis yang direpresentasikan baris ini. */
	private ParameterTambahan parameterTambahan;
	/** Kelompok tampilan tempat baris definisi ini berada. */
	private KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan;

	/** Urutan tampil; diturunkan dari {@link #parameterTambahan} bila terisi, lihat {@link #getNomorUrut()}. */
	private Integer nomorUrut;
	/** Posisi kolom tampilan field ini; default 1 bila kosong. */
	private Integer kolomKe;
	/** Flag: field ini aktif pada modul Perkuliahan. */
	private Boolean perkuliahan;
	/** Flag: field ini aktif pada modul Jadwal Ujian PMB. */
	private Boolean jadwalUjianPMB;
	/** Flag: field ini aktif pada modul Request Tugas Akhir Mahasiswa. */
	private Boolean mahasiswaRequestTugasAkhir;
	/** Flag: field ini aktif pada modul Kelompok KKN. */
	private Boolean kelompokKkn;
	/** Flag: field ini aktif pada modul Kelompok PKL. */
	private Boolean kelompokPkl;
	/** Flag: field ini aktif pada modul Skripsi. */
	private Boolean skripsi;
	/** Flag: field ini aktif pada modul KRS Mahasiswa. */
	private Boolean krsMahasiswa;
	/** Flag: field ini aktif pada modul Jadwal Ujian PSB. */
	private Boolean jadwalUjianPSB;
	/** Flag: field ini aktif pada modul Jadwal Pelajaran. */
	private Boolean jadwalPelajaran;
	/** Flag: field ini aktif pada modul Pertemuan yang punya Grup Pertemuan. */
	private Boolean pertemuanPunyaGrupPertemuan;
	/** Flag: field ini aktif pada modul Formulir Kegiatan. */
	private Boolean formulirKegiatan;

	/**
	 * Urutan tampil baris definisi ini.
	 *
	 * <p><b>Getter yang menulis balik (diturunkan dari relasi):</b> bila {@link
	 * #getParameterTambahan()} tidak {@code null}, field {@link #nomorUrut} DITIMPA dengan
	 * {@code parameterTambahan.getNomorUrut()} setiap kali getter ini dipanggil — nilai yang
	 * pernah diset manual lewat {@link #setNomorUrut(Integer)} tertimpa selama relasi itu
	 * terisi (yang secara skema wajib, kolomnya {@code nullable = false}).</p>
	 *
	 * @return nomor urut efektif; {@code 1} bila hasil akhirnya {@code null}.
	 */
	public Integer getNomorUrut() {
		parameterTambahan = getParameterTambahan();
		if (parameterTambahan != null) {
			nomorUrut = parameterTambahan.getNomorUrut();
		}
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/**
	 * @param nomorUrut nomor urut baru untuk field lokal (bisa tetap ditimpa oleh nomor urut
	 *                  {@link #parameterTambahan} saat dibaca via {@link #getNomorUrut()}).
	 */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/**
	 * Konstruktor kosong yang dibutuhkan Hibernate untuk instansiasi entity via refleksi.
	 */
	public ParameterTambahanPertemuan() {
	}

	/**
	 * @return primary key baris {@code parameter_tambahan_pertemuan}; {@code null} sebelum
	 *         baris di-{@code INSERT}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id primary key; biasanya tidak perlu diset manual karena kolomnya {@code
	 *           insertable = false} (identity, dibangkitkan database).
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return definisi field kustom/dinamis yang direpresentasikan baris ini (proxy lazy
	 *         diresolusi via {@code check()}); kolomnya {@code nullable = false} sehingga
	 *         secara skema selalu terisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "parameter_tambahan", nullable = false)
	public ParameterTambahan getParameterTambahan() {
		parameterTambahan = check(parameterTambahan);
		return parameterTambahan;
	}

	/**
	 * @param parameterTambahan definisi field kustom baru.
	 */
	public void setParameterTambahan(ParameterTambahan parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * @return kelompok tampilan tempat baris definisi ini berada (proxy lazy diresolusi via
	 *         {@code check()}); kolomnya {@code nullable = false} sehingga secara skema selalu
	 *         terisi.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelompok_parameter_tambahan_pertemuan", nullable = false)
	public KelompokParameterTambahanPertemuan getKelompokParameterTambahanPertemuan() {
		kelompokParameterTambahanPertemuan = check(kelompokParameterTambahanPertemuan);
		return kelompokParameterTambahanPertemuan;
	}

	/**
	 * @param kelompokParameterTambahanPertemuan kelompok tampilan baru.
	 */
	public void setKelompokParameterTambahanPertemuan(
			KelompokParameterTambahanPertemuan kelompokParameterTambahanPertemuan) {
		this.kelompokParameterTambahanPertemuan = kelompokParameterTambahanPertemuan;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Perkuliahan; default {@code false}
	 *         bila belum diisi.
	 */
	public Boolean getPerkuliahan() {
		return perkuliahan == null ? false : perkuliahan;
	}

	/**
	 * @param perkuliahan nilai flag baru.
	 */
	public void setPerkuliahan(Boolean perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Jadwal Ujian PMB; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getJadwalUjianPMB() {
		return jadwalUjianPMB == null ? false : jadwalUjianPMB;
	}

	/**
	 * @param jadwalUjianPMB nilai flag baru.
	 */
	public void setJadwalUjianPMB(Boolean jadwalUjianPMB) {
		this.jadwalUjianPMB = jadwalUjianPMB;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Request Tugas Akhir Mahasiswa;
	 *         default {@code false} bila belum diisi.
	 */
	public Boolean getMahasiswaRequestTugasAkhir() {
		return mahasiswaRequestTugasAkhir == null ? false : mahasiswaRequestTugasAkhir;
	}

	/**
	 * @param mahasiswaRequestTugasAkhir nilai flag baru.
	 */
	public void setMahasiswaRequestTugasAkhir(Boolean mahasiswaRequestTugasAkhir) {
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Kelompok KKN; default {@code false}
	 *         bila belum diisi.
	 */
	public Boolean getKelompokKkn() {
		return kelompokKkn == null ? false : kelompokKkn;
	}

	/**
	 * @param kelompokKkn nilai flag baru.
	 */
	public void setKelompokKkn(Boolean kelompokKkn) {
		this.kelompokKkn = kelompokKkn;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Kelompok PKL; default {@code false}
	 *         bila belum diisi.
	 */
	public Boolean getKelompokPkl() {
		return kelompokPkl == null ? false : kelompokPkl;
	}

	/**
	 * @param kelompokPkl nilai flag baru.
	 */
	public void setKelompokPkl(Boolean kelompokPkl) {
		this.kelompokPkl = kelompokPkl;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Skripsi; default {@code false} bila
	 *         belum diisi.
	 */
	public Boolean getSkripsi() {
		return skripsi == null ? false : skripsi;
	}

	/**
	 * @param skripsi nilai flag baru.
	 */
	public void setSkripsi(Boolean skripsi) {
		this.skripsi = skripsi;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul KRS Mahasiswa; default {@code false}
	 *         bila belum diisi.
	 */
	public Boolean getKrsMahasiswa() {
		return krsMahasiswa == null ? false : krsMahasiswa;
	}

	/**
	 * @param krsMahasiswa nilai flag baru.
	 */
	public void setKrsMahasiswa(Boolean krsMahasiswa) {
		this.krsMahasiswa = krsMahasiswa;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Jadwal Ujian PSB; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getJadwalUjianPSB() {
		return jadwalUjianPSB == null ? false : jadwalUjianPSB;
	}

	/**
	 * @param jadwalUjianPSB nilai flag baru.
	 */
	public void setJadwalUjianPSB(Boolean jadwalUjianPSB) {
		this.jadwalUjianPSB = jadwalUjianPSB;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Jadwal Pelajaran; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getJadwalPelajaran() {
		return jadwalPelajaran == null ? false : jadwalPelajaran;
	}

	/**
	 * @param jadwalPelajaran nilai flag baru.
	 */
	public void setJadwalPelajaran(Boolean jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Pertemuan yang punya Grup Pertemuan;
	 *         default {@code false} bila belum diisi.
	 */
	public Boolean getPertemuanPunyaGrupPertemuan() {
		return pertemuanPunyaGrupPertemuan == null ? false : pertemuanPunyaGrupPertemuan;
	}

	/**
	 * @param pertemuanPunyaGrupPertemuan nilai flag baru.
	 */
	public void setPertemuanPunyaGrupPertemuan(Boolean pertemuanPunyaGrupPertemuan) {
		this.pertemuanPunyaGrupPertemuan = pertemuanPunyaGrupPertemuan;
	}

	/**
	 * @return {@code true} bila field ini aktif pada modul Formulir Kegiatan; default {@code
	 *         false} bila belum diisi.
	 */
	public Boolean getFormulirKegiatan() {
		return formulirKegiatan == null ? false : formulirKegiatan;
	}

	/**
	 * @param formulirKegiatan nilai flag baru.
	 */
	public void setFormulirKegiatan(Boolean formulirKegiatan) {
		this.formulirKegiatan = formulirKegiatan;
	}

	/**
	 * @return posisi kolom tampilan field ini; {@code 1} bila belum diisi.
	 */
	public Integer getKolomKe() {
		return kolomKe == null ? 1 : kolomKe;
	}

	/**
	 * @param kolomKe posisi kolom baru.
	 */
	public void setKolomKe(Integer kolomKe) {
		this.kolomKe = kolomKe;
	}

}
