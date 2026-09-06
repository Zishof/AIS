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

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;

/**
 * Model data untuk rekap kehadiran SEORANG pegawai (bukan khusus dosen) dalam SATU bulan
 * tertentu (agregat bulanan): jumlah hari masuk, alpa, sakit, izin, cuti, keterlambatan, pulang
 * cepat, lembur, serta status aktif pegawai pada bulan tersebut. Tipe ini membawa state yang
 * dipertukarkan oleh lapisan persistence, service, dan UI; makna bisnis utamanya ditentukan oleh
 * field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Long id}, {@code String oleh}, {@code
 * String olehId}, {@code Date tanggal_dirubah}, {@code Integer bulan}, {@code Integer tahun}, {@code Pegawai
 * pegawai}, {@code Integer masuk}, {@code Integer alpa}, {@code Integer sakit}, {@code Integer izin}, {@code
 * Integer cuti}, {@code Double lembur}; pemetaan persistence: tabel {@code public.kehadiran_pegawai_bulanan};
 * pembacaan/pencarian ({@code getOlehId()}, {@code getId()}, {@code getOleh()}, {@code getTanggal_dirubah()},
 * {@code getBulan()}, {@code getPegawai()}, {@code getMasuk()}); mutasi data ({@code setOlehId()}, {@code
 * setId()}, {@code setOleh()}, {@code onUpdate()}, {@code setTanggal_dirubah()}, {@code setBulan()}). Bagian
 * lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Catatan relasi:</b> berbeda dari {@link KehadiranDosenBulanan#getDosen()} yang berupa {@code Long}
 * mentah, {@link #getPegawai()} di sini adalah relasi {@code @ManyToOne} penuh ke {@link Pegawai}, dengan
 * {@code @NotFound(IGNORE)} sehingga baris {@code pegawai} yang sudah terhapus tidak membuat query gagal
 * (relasinya sekadar bernilai {@code null}).</p>
 * <p><b>Efek samping:</b> accessor dan mutator hanya membaca atau mengubah state entity/value object di memori.
 * Persistence, transaksi, otorisasi, dan pemuatan relasi lazy tetap menjadi tanggung jawab DAO/service dengan
 * session aktif; jangan menaruh query duplikat pada model.</p>
 *
 * @see GeneralValueObject
 * @see KehadiranDosenBulanan versi rekap kehadiran bulanan khusus dosen
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kehadiran_pegawai_bulanan")
public class KehadiranPegawaiBulanan extends GeneralValueObject {

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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
	 * sama seperti {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
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

	/** Bulan rekap (1-12). */
	private Integer bulan;
	/** Tahun rekap. */
	private Integer tahun;
	/** Jumlah hari pegawai berstatus aktif dalam bulan ini. */
	private Integer aktif;
	/** Jumlah hari masuk. */
	private Integer masuk;
	/** Jumlah hari tidak hadir (total, termasuk hari libur). */
	private Integer tidakHadir;
	/** Jumlah hari tidak hadir TANPA menghitung hari libur/holiday. */
	private Integer tidakHadirTanpaHoliday;
	/** Jumlah hari alpa (tanpa keterangan). */
	private Integer alpa;
	/** Jumlah hari sakit. */
	private Integer sakit;
	/** Jumlah hari izin. */
	private Integer izin;
	/** Jumlah hari belum ada catatan presensi. */
	private Integer belum;
	/** Jumlah hari cuti. */
	private Integer cuti;
	/** Jumlah hari tepat waktu (tidak terlambat). */
	private Integer tepatWaktu;
	/** Jumlah hari pulang cepat. */
	private Integer pulangcepat;
	/** Jumlah hari terlambat masuk. */
	private Integer terlambat;
	/** Total jam lembur dalam bulan ini. */
	private Double lembur;
	/** Jumlah hari masuk pada hari libur. */
	private Integer masukDihariLibur;

	/** Pegawai pemilik rekap kehadiran ini. */
	private Pegawai pegawai;
	/** Nama tampilan baris rekap. */
	private String nama;
	/** Keterangan bebas. */
	private String keterangan;

	/** Konstruktor kosong, dipakai Hibernate. */
	public KehadiranPegawaiBulanan() {
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

	/**
	 * @return pegawai pemilik rekap; di-resolve lewat {@link GeneralValueObject#check(Object)}, dan
	 *         berkat {@code @NotFound(IGNORE)} baris pegawai yang sudah terhapus di database
	 *         menghasilkan {@code null} alih-alih melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai pegawai pemilik rekap yang baru. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/** @return jumlah hari masuk, {@code 0} bila belum diisi (bukan {@code null}). */
	@Column(name = "masuk", nullable = false)
	public Integer getMasuk() {
		return masuk == null ? 0 : masuk;
	}

	/** @param masuk jumlah hari masuk baru. */
	public void setMasuk(Integer masuk) {
		this.masuk = masuk;
	}

	/** @return jumlah hari alpa, {@code 0} bila belum diisi. */
	@Column(name = "alpa", nullable = false)
	public Integer getAlpa() {
		return alpa == null ? 0 : alpa;
	}

	/** @param alpa jumlah hari alpa baru. */
	public void setAlpa(Integer alpa) {
		this.alpa = alpa;
	}

	/** @return jumlah hari sakit, {@code 0} bila belum diisi. */
	@Column(name = "sakit", nullable = false)
	public Integer getSakit() {
		return sakit == null ? 0 : sakit;
	}

	/** @param sakit jumlah hari sakit baru. */
	public void setSakit(Integer sakit) {
		this.sakit = sakit;
	}

	/** @return jumlah hari izin, {@code 0} bila belum diisi. */
	@Column(name = "izin", nullable = false)
	public Integer getIzin() {
		return izin == null ? 0 : izin;
	}

	/** @param izin jumlah hari izin baru. */
	public void setIzin(Integer izin) {
		this.izin = izin;
	}

	/** @return jumlah hari belum ada catatan presensi, {@code 0} bila belum diisi. */
	@Column(name = "belum", nullable = false)
	public Integer getBelum() {
		return belum == null ? 0 : belum;
	}

	/** @param belum jumlah hari belum ada catatan baru. */
	public void setBelum(Integer belum) {
		this.belum = belum;
	}

	/** @return jumlah hari cuti, {@code 0} bila belum diisi. */
	@Column(name = "cuti", nullable = false)
	public Integer getCuti() {
		return cuti == null ? 0 : cuti;
	}

	/** @param cuti jumlah hari cuti baru. */
	public void setCuti(Integer cuti) {
		this.cuti = cuti;
	}

	/** @return total jam lembur, {@code 0} bila belum diisi. */
	@Column(name = "lembur", nullable = false)
	public Double getLembur() {
		return lembur == null ? 0 : lembur;
	}

	/** @param lembur total jam lembur baru. */
	public void setLembur(Double lembur) {
		this.lembur = lembur;
	}

	/** @return jumlah hari tepat waktu, {@code 0} bila belum diisi. */
	public Integer getTepatWaktu() {
		return tepatWaktu == null ? 0 : tepatWaktu;
	}

	/** @param tepatWaktu jumlah hari tepat waktu baru. */
	public void setTepatWaktu(Integer tepatWaktu) {
		this.tepatWaktu = tepatWaktu;
	}

	/** @return jumlah hari pulang cepat, {@code 0} bila belum diisi. */
	public Integer getPulangcepat() {
		return pulangcepat == null ? 0 : pulangcepat;
	}

	/** @param pulangcepat jumlah hari pulang cepat baru. */
	public void setPulangcepat(Integer pulangcepat) {
		this.pulangcepat = pulangcepat;
	}

	/** @return jumlah hari terlambat, {@code 0} bila belum diisi. */
	public Integer getTerlambat() {
		return terlambat == null ? 0 : terlambat;
	}

	/** @param terlambat jumlah hari terlambat baru. */
	public void setTerlambat(Integer terlambat) {
		this.terlambat = terlambat;
	}

	/** @return jumlah hari pegawai berstatus aktif, {@code 0} bila belum diisi. */
	public Integer getAktif() {
		return aktif == null ? 0 : aktif;
	}

	/** @param aktif jumlah hari aktif baru. */
	public void setAktif(Integer aktif) {
		this.aktif = aktif;
	}

	/** @return jumlah hari masuk pada hari libur, {@code 0} bila belum diisi. */
	public Integer getMasukDihariLibur() {
		return masukDihariLibur == null ? 0 : masukDihariLibur;
	}

	/** @param masukDihariLibur jumlah hari masuk pada hari libur baru. */
	public void setMasukDihariLibur(Integer masukDihariLibur) {
		this.masukDihariLibur = masukDihariLibur;
	}

	/** @return jumlah hari tidak hadir (total), {@code 0} bila belum diisi. */
	public Integer getTidakHadir() {
		return tidakHadir == null ? 0 : tidakHadir;
	}

	/** @param tidakHadir jumlah hari tidak hadir baru. */
	public void setTidakHadir(Integer tidakHadir) {
		this.tidakHadir = tidakHadir;
	}

	/** @return jumlah hari tidak hadir TANPA menghitung hari libur, {@code 0} bila belum diisi. */
	public Integer getTidakHadirTanpaHoliday() {
		return tidakHadirTanpaHoliday == null ? 0 : tidakHadirTanpaHoliday;
	}

	/** @param tidakHadirTanpaHoliday jumlah hari tidak hadir (tanpa holiday) baru. */
	public void setTidakHadirTanpaHoliday(Integer tidakHadirTanpaHoliday) {
		this.tidakHadirTanpaHoliday = tidakHadirTanpaHoliday;
	}

}
