package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * Model data untuk rekap keterlibatan SEORANG dosen dalam penyelenggaraan ujian (UTS/UAS) suatu
 * perkuliahan dalam SATU bulan tertentu (agregat bulanan): jumlah soal/tugas UTS-UAS, pemecahan
 * per jenis soal (pilihan ganda/esai), serta versi "dibagi jumlah dosen pengampu" untuk perkuliahan
 * yang diampu lebih dari satu dosen. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Long dosen}, {@code Long perkuliahan}, {@code Integer
 * uts}, {@code Integer uas}, {@code Integer jmlDosen}; pemetaan persistence: tabel
 * {@code public.rekap_ujian_dosen_bulanan}; pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code
 * getOleh()}, {@code getTanggal_dirubah()}, {@code getDosen()}, {@code getPerkuliahan()}, {@code getUts()});
 * mutasi data ({@code setOlehId()}, {@code setId()}, {@code setOleh()}, {@code onUpdate()}, {@code
 * setTanggal_dirubah()}, {@code setDosen()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Catatan relasi:</b> {@link #getDosen()} dan {@link #getPerkuliahan()} berupa {@code Long} MENTAH
 * (id), bukan {@code @ManyToOne} -- sama seperti pola pada {@link KehadiranDosenBulanan#getDosen()};
 * pemanggil harus me-resolve id ini sendiri lewat DAO masing-masing bila butuh objeknya.</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see KehadiranDosenBulanan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "rekap_ujian_dosen_bulanan")
public class RekapUjianDosenBulanan extends GeneralValueObject {

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

	/** Semester (teks bebas) rekap ini. */
	private String smt;
	/** Id perkuliahan terkait (relasi MENTAH, lihat catatan kelas). */
	private Long perkuliahan;
	/** Id dosen terkait (relasi MENTAH, lihat catatan kelas). */
	private Long dosen;
	/** Nama tampilan baris rekap. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;

	/** Bulan rekap (1-12). */
	private Integer bulan;
	/** Tahun rekap. */
	private Integer tahun;

	/** Jumlah soal/tugas UTS berbentuk tugas (non-ujian tertulis). */
	private Integer utsTugas;
	/** Jumlah soal/tugas UAS berbentuk tugas. */
	private Integer uasTugas;
	/** Jumlah dosen pengampu perkuliahan ini; dipakai pembagi versi "dibagi jumlah dosen". */
	private Integer jmlDosen;
	/** Jumlah soal UTS. */
	private Integer uts;
	/** Jumlah soal UAS. */
	private Integer uas;

	/** Jumlah soal UTS dibagi {@link #jmlDosen}. */
	private Double utsDibagiJmlDosen;
	/** Jumlah soal UAS dibagi {@link #jmlDosen}. */
	private Double uasDibagiJmlDosen;

	/** Jumlah soal UTS berbentuk pilihan ganda. */
	private Integer utsUjianPg;
	/** Jumlah soal UAS berbentuk pilihan ganda. */
	private Integer uasUjianPg;

	/** Jumlah soal UTS berbentuk esai. */
	private Integer utsUjianEssay;
	/** Jumlah soal UAS berbentuk esai. */
	private Integer uasUjianEssay;

	/** Jumlah soal UTS pilihan ganda dibagi {@link #jmlDosen}. */
	private Double utsUjianPgJmlDosen;
	/** Jumlah soal UAS pilihan ganda dibagi {@link #jmlDosen}. */
	private Double uasUjianPgJmlDosen;

	/** Jumlah soal UTS esai dibagi {@link #jmlDosen}. */
	private Double utsUjianEssayJmlDosen;
	/** Jumlah soal UAS esai dibagi {@link #jmlDosen}. */
	private Double uasUjianEssayJmlDosen;

	/** Tanggal mulai periode rekap. */
	private Date tanggalMulai;

	/** Konstruktor kosong, dipakai Hibernate. */
	public RekapUjianDosenBulanan() {
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

	/** @return nama tampilan baris rekap, sudah di-{@code trim}; {@code null} bila belum diisi. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tampilan baru. */
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

	/** @return id dosen terkait (relasi MENTAH -- lihat catatan kelas). */
	@Column(name = "dosen", nullable = false)
	public Long getDosen() {
		return dosen;
	}

	/** @param dosen id dosen terkait yang baru. */
	public void setDosen(Long dosen) {
		this.dosen = dosen;
	}

	/** @return jumlah soal UTS, {@code 0} bila belum diisi (bukan {@code null}). */
	@Column(name = "uts", nullable = false)
	public Integer getUts() {
		return uts == null ? 0 : uts;
	}

	/** @param uts jumlah soal UTS baru. */
	public void setUts(Integer uts) {
		this.uts = uts;
	}

	/** @return jumlah soal UAS, {@code 0} bila belum diisi. */
	@Column(name = "uas", nullable = false)
	public Integer getUas() {
		return uas == null ? 0 : uas;
	}

	/** @param uas jumlah soal UAS baru. */
	public void setUas(Integer uas) {
		this.uas = uas;
	}

	/** @return semester (teks bebas) rekap ini. */
	@Column(name = "smt", nullable = false)
	public String getSmt() {
		return smt;
	}

	/** @param smt semester baru. */
	public void setSmt(String smt) {
		this.smt = smt;
	}

	/** @return id perkuliahan terkait (relasi MENTAH -- lihat catatan kelas). */
	@Column(name = "perkuliahan", nullable = false)
	public Long getPerkuliahan() {
		return perkuliahan;
	}

	/** @param perkuliahan id perkuliahan terkait yang baru. */
	public void setPerkuliahan(Long perkuliahan) {
		this.perkuliahan = perkuliahan;
	}

	/** @return bulan rekap (1-12). */
	@Column(name = "bulan", nullable = false)
	public Integer getBulan() {
		return bulan;
	}

	/** @param bulan bulan rekap baru. */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/** @return tahun rekap. */
	@Column(name = "tahun", nullable = false)
	public Integer getTahun() {
		return tahun;
	}

	/** @param tahun tahun rekap baru. */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/** @return jumlah soal UTS pilihan ganda, boleh {@code null}. */
	public Integer getUtsUjianPg() {
		return utsUjianPg;
	}

	/** @param utsUjianPg jumlah soal UTS pilihan ganda baru. */
	public void setUtsUjianPg(Integer utsUjianPg) {
		this.utsUjianPg = utsUjianPg;
	}

	/** @return jumlah soal UAS pilihan ganda, boleh {@code null}. */
	public Integer getUasUjianPg() {
		return uasUjianPg;
	}

	/** @param uasUjianPg jumlah soal UAS pilihan ganda baru. */
	public void setUasUjianPg(Integer uasUjianPg) {
		this.uasUjianPg = uasUjianPg;
	}

	/** @return jumlah soal UTS esai, boleh {@code null}. */
	public Integer getUtsUjianEssay() {
		return utsUjianEssay;
	}

	/** @param utsUjianEssay jumlah soal UTS esai baru. */
	public void setUtsUjianEssay(Integer utsUjianEssay) {
		this.utsUjianEssay = utsUjianEssay;
	}

	/** @return jumlah soal UAS esai, boleh {@code null}. */
	public Integer getUasUjianEssay() {
		return uasUjianEssay;
	}

	/** @param uasUjianEssay jumlah soal UAS esai baru. */
	public void setUasUjianEssay(Integer uasUjianEssay) {
		this.uasUjianEssay = uasUjianEssay;
	}

	/** @return jumlah soal/tugas UTS berbentuk tugas, {@code 0} bila belum diisi. */
	public Integer getUtsTugas() {
		return utsTugas == null ? 0 : utsTugas;
	}

	/** @param utsTugas jumlah soal/tugas UTS berbentuk tugas baru. */
	public void setUtsTugas(Integer utsTugas) {
		this.utsTugas = utsTugas;
	}

	/** @return jumlah soal/tugas UAS berbentuk tugas, {@code 0} bila belum diisi. */
	public Integer getUasTugas() {
		return uasTugas == null ? 0 : uasTugas;
	}

	/** @param uasTugas jumlah soal/tugas UAS berbentuk tugas baru. */
	public void setUasTugas(Integer uasTugas) {
		this.uasTugas = uasTugas;
	}

	/** @return tanggal mulai periode rekap, boleh {@code null}. */
	@Temporal(TemporalType.DATE)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/** @param tanggalMulai tanggal mulai periode rekap yang baru. */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/** @return jumlah dosen pengampu; default {@code 1} bila belum diisi (bukan {@code null} atau {@code 0}). */
	public Integer getJmlDosen() {
		return jmlDosen == null ? 1 : jmlDosen;
	}

	/** @param jmlDosen jumlah dosen pengampu baru. */
	public void setJmlDosen(Integer jmlDosen) {
		this.jmlDosen = jmlDosen;
	}

	/** @return jumlah soal UTS dibagi jumlah dosen, boleh {@code null}. */
	public Double getUtsDibagiJmlDosen() {
		return utsDibagiJmlDosen;
	}

	/** @param utsDibagiJmlDosen nilai baru. */
	public void setUtsDibagiJmlDosen(Double utsDibagiJmlDosen) {
		this.utsDibagiJmlDosen = utsDibagiJmlDosen;
	}

	/** @return jumlah soal UAS dibagi jumlah dosen, boleh {@code null}. */
	public Double getUasDibagiJmlDosen() {
		return uasDibagiJmlDosen;
	}

	/** @param uasDibagiJmlDosen nilai baru. */
	public void setUasDibagiJmlDosen(Double uasDibagiJmlDosen) {
		this.uasDibagiJmlDosen = uasDibagiJmlDosen;
	}

	/** @return jumlah soal UTS pilihan ganda dibagi jumlah dosen, boleh {@code null}. */
	public Double getUtsUjianPgJmlDosen() {
		return utsUjianPgJmlDosen;
	}

	/** @param utsUjianPgJmlDosen nilai baru. */
	public void setUtsUjianPgJmlDosen(Double utsUjianPgJmlDosen) {
		this.utsUjianPgJmlDosen = utsUjianPgJmlDosen;
	}

	/** @return jumlah soal UAS pilihan ganda dibagi jumlah dosen, boleh {@code null}. */
	public Double getUasUjianPgJmlDosen() {
		return uasUjianPgJmlDosen;
	}

	/** @param uasUjianPgJmlDosen nilai baru. */
	public void setUasUjianPgJmlDosen(Double uasUjianPgJmlDosen) {
		this.uasUjianPgJmlDosen = uasUjianPgJmlDosen;
	}

	/** @return jumlah soal UTS esai dibagi jumlah dosen, boleh {@code null}. */
	public Double getUtsUjianEssayJmlDosen() {
		return utsUjianEssayJmlDosen;
	}

	/** @param utsUjianEssayJmlDosen nilai baru. */
	public void setUtsUjianEssayJmlDosen(Double utsUjianEssayJmlDosen) {
		this.utsUjianEssayJmlDosen = utsUjianEssayJmlDosen;
	}

	/** @return jumlah soal UAS esai dibagi jumlah dosen, boleh {@code null}. */
	public Double getUasUjianEssayJmlDosen() {
		return uasUjianEssayJmlDosen;
	}

	/** @param uasUjianEssayJmlDosen nilai baru. */
	public void setUasUjianEssayJmlDosen(Double uasUjianEssayJmlDosen) {
		this.uasUjianEssayJmlDosen = uasUjianEssayJmlDosen;
	}

}
