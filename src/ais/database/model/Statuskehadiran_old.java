package ais.database.model;

// Generated Dec 12, 2009 3:35:45 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.envers.Audited;

/**
 * Entity <b>model kehadiran mahasiswa VERSI LAMA</b> (tabel {@code public.statuskehadiran} — CATATAN:
 * nama tabel TIDAK berakhiran {@code _old}, hanya nama class Java-nya yang diberi akhiran itu untuk
 * membedakan dari model kehadiran baru).
 *
 * <h3>Status: LEGACY/DORMAN untuk penulisan, tapi tabelnya masih AKTIF DIBACA untuk migrasi satu-arah</h3>
 * <p>Diverifikasi lewat penelusuran seluruh pemakai: TIDAK ADA lagi kode yang menulis baris baru ke
 * entity ini (tidak ditemukan pemanggil {@code save}/{@code persist} atas class ini di luar dirinya
 * sendiri). Satu-satunya pemakai aktif adalah
 * {@code ais.action.master.helper.util.JamPerkuliahanSyncrhonizerProcessor#processMigrasiAbsensi()},
 * yang men-scan baris SISA (dari sebelum migrasi ke model baru) dan memindahkan datanya ke
 * {@link Pertemuan#populate} pada model kehadiran BARU (kolom {@code absensi} pada {@link Pertemuan}),
 * untuk pertemuan yang belum punya data absensi di model baru. Proses migrasi ini idempoten dan
 * hanya menyalin data yang belum ada di sisi baru — TIDAK menghapus baris lama setelah disalin.
 * {@code PengajuanIzinTidakMasukPerkuliahan} (lihat Javadoc-nya) juga mencatat bahwa entity ini masih
 * punya FK ke sana, sebagai jejak relasi peninggalan model lama.</p>
 *
 * <p>Kesimpulan verifikasi (sesuai instruksi tugas — JANGAN asumsikan dari nama {@code _old} saja):
 * entity ini BUKAN kode mati yang aman dihapus begitu saja selama proses migrasi batch di atas masih
 * dijadwalkan berjalan (dipanggil dari {@code run()}/{@code doProcess()} milik
 * {@code JamPerkuliahanSyncrhonizerProcessor}, lihat Javadoc kelas tersebut) — tabelnya berperan
 * sebagai SUMBER migrasi satu-arah menuju model kehadiran baru, bukan model kehadiran yang sedang
 * dipakai untuk mencatat kehadiran baru.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "statuskehadiran")

public class Statuskehadiran_old extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 4925896879277752808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return {@link #getKeterangan()}, dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return keterangan;
	}

	private PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan;
	private Statusabsensi statusabsensi;
	private String keterangan;
	private Mahasiswa mahasiswa;
	private BiodataCalonMahasiswa biodataCalonMahasiswa;
	private Dosen dosen;
	private Pertemuan pertemuan;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public Statuskehadiran_old() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return status kehadiran baris ini. Bila {@link #getPengajuanIzinTidakMasukPerkuliahan()}
	 *         terisi DAN pengajuan izin tersebut sudah disetujui ({@code getDiizinkan()}), status
	 *         SELALU ditimpa dengan status izin dari pengajuan tersebut (getter-mutasi) — baris hasil
	 *         pengajuan izin selalu mengikuti keputusan izin terbaru, bukan nilai tersimpan awal.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "statusabsensi", nullable = true)
	public Statusabsensi getStatusabsensi() {
		if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
			statusabsensi = pengajuanIzinTidakMasukPerkuliahan.getStatusabsensi();
		}
		return this.statusabsensi;
	}

	/**
	 * @param statusabsensi status kehadiran baris ini.
	 */
	public void setStatusabsensi(Statusabsensi statusabsensi) {
		this.statusabsensi = statusabsensi;
	}

	/**
	 * @return keterangan kehadiran baris ini. Sama seperti {@link #getStatusabsensi()}, bila
	 *         pengajuan izin terkait sudah disetujui, keterangan SELALU ditimpa dengan keterangan
	 *         dari pengajuan izin tersebut (getter-mutasi).
	 */
	@Column(name = "keterangan")
	public String getKeterangan() {
		if (pengajuanIzinTidakMasukPerkuliahan != null && pengajuanIzinTidakMasukPerkuliahan.getDiizinkan()) {
			keterangan = pengajuanIzinTidakMasukPerkuliahan.getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * @param keterangan keterangan kehadiran baris ini.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @param mahasiswa mahasiswa yang dicatat kehadirannya pada baris ini.
	 */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/**
	 * @return mahasiswa yang dicatat kehadirannya pada baris ini, atau {@code null} bila baris ini
	 *         menyangkut dosen/calon mahasiswa (lihat {@link #getDosen()}/
	 *         {@link #getBiodataCalonMahasiswa()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "mahasiswa", nullable = true)
	public Mahasiswa getMahasiswa() {
		return mahasiswa;
	}

	/**
	 * @param pertemuan pertemuan kuliah tempat kehadiran ini dicatat.
	 */
	public void setPertemuan(Pertemuan pertemuan) {
		this.pertemuan = pertemuan;
	}

	/**
	 * @return pertemuan kuliah tempat kehadiran ini dicatat — dipakai sebagai sumber saat migrasi ke
	 *         model kehadiran baru lewat {@code JamPerkuliahanSyncrhonizerProcessor#processMigrasiAbsensi()}
	 *         (lihat Javadoc kelas).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertemuan", nullable = true)
	public Pertemuan getPertemuan() {
		return pertemuan;
	}

	/**
	 * @return dosen yang dicatat kehadirannya pada baris ini, atau {@code null} bila baris ini
	 *         menyangkut mahasiswa/calon mahasiswa.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "dosen", nullable = true)
	public Dosen getDosen() {
		return dosen;
	}

	/**
	 * @param dosen dosen yang dicatat kehadirannya pada baris ini.
	 */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/**
	 * @return calon mahasiswa yang dicatat kehadirannya pada baris ini (mis. kehadiran ujian PMB),
	 *         atau {@code null} bila tidak relevan.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "biodata_calon_mahasiswa", nullable = true)
	public BiodataCalonMahasiswa getBiodataCalonMahasiswa() {
		return biodataCalonMahasiswa;
	}

	/**
	 * @param biodataCalonMahasiswa calon mahasiswa yang dicatat kehadirannya pada baris ini.
	 */
	public void setBiodataCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		this.biodataCalonMahasiswa = biodataCalonMahasiswa;
	}

	/**
	 * @return pengajuan izin tidak masuk kuliah yang terkait baris ini, bila kehadiran ini merupakan
	 *         hasil pengajuan izin (lihat efek getter-mutasi pada {@link #getStatusabsensi()}/
	 *         {@link #getKeterangan()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_izin_tidak_masuk_perkuliahan", nullable = true)
	public PengajuanIzinTidakMasukPerkuliahan getPengajuanIzinTidakMasukPerkuliahan() {
		return pengajuanIzinTidakMasukPerkuliahan;
	}

	/**
	 * @param pengajuanIzinTidakMasukPerkuliahan pengajuan izin tidak masuk kuliah terkait baris ini.
	 */
	public void setPengajuanIzinTidakMasukPerkuliahan(
			PengajuanIzinTidakMasukPerkuliahan pengajuanIzinTidakMasukPerkuliahan) {
		this.pengajuanIzinTidakMasukPerkuliahan = pengajuanIzinTidakMasukPerkuliahan;
	}

}
