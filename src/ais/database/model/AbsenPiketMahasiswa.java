package ais.database.model;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

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
import ais.ui.util.WaktuUtil;

/**
 * Entity <b>jadwal piket presensi mahasiswa</b> (tabel {@code public.absen_piket_mahasiswa}) — mirip
 * konsep {@code AbsenPiket} pada modul sekolah, tapi untuk perguruan tinggi: satu baris mendefinisikan
 * periode ({@link #getSemester()}/{@link #getTahunAjaran()}) dan cakupan (jurusan/fakultas atau
 * seluruh mahasiswa) di mana {@link #getPegawai()} bertugas piket mencatat presensi mahasiswa.
 * Detail siapa saja yang tercatat hadir disimpan terpisah (lihat kelas terkait di paket
 * {@code ais.database.model.sekolah}: {@link ais.database.model.sekolah.AbsenPiket},
 * {@link ais.database.model.sekolah.AbsenPiketDetail}, {@link ais.database.model.sekolah.AbsenPiketPeserta}
 * — meski nama classnya menyiratkan modul sekolah, dipakai bersama oleh
 * {@code AbsenPiketMahasiswaAction} untuk piket mahasiswa juga).
 *
 * <h3>Catatan keamanan — verifikasi IDOR (BUKAN kasus sama dengan task_493423ef butir b44)</h3>
 * <p>task_493423ef butir b44 mencatat IDOR terautentikasi penuh pada
 * {@code ElearningApiUtil.simpanAbsenPiket} (servlet API e-learning) untuk presensi PIKET SISWA
 * (sekolah) — token login siapa pun bisa mengubah status kehadiran siswa mana pun. Berdasarkan
 * penelusuran seluruh pemakai class ini, {@code AbsenPiketMahasiswa} TIDAK memiliki jalur servlet/API
 * setara: satu-satunya pemakai adalah {@code ais.action.master.AbsenPiketMahasiswaAction} (aksi ZK
 * admin biasa, tunduk pada kontrol akses menu standar) dan
 * {@code ais.action.master.sekolah.helper.DetailAbsenPiketMahasiswaHelper}. Karena tidak ada endpoint
 * API publik yang menulis ke entity ini, pola IDOR b44 tidak berlaku di sini — bukan karena sudah
 * ditambal, tapi karena permukaan seranganny (fitur piket via API) memang tidak ada untuk mahasiswa.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(name = "absen_piket_mahasiswa", schema = "public")
public class AbsenPiketMahasiswa extends GeneralValueObject {

	/** Penanda semester genap, dipakai pada label/filter UI. */
	public static final String GENAP = "Genap";
	/** Penanda semester ganjil, dipakai pada label/filter UI. */
	public static final String GANJIL = "Ganjil";

	/**
	 *
	 */
	private static final long serialVersionUID = 7154228487700348608L;
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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

	private Date tanggal;
	private Pegawai pegawai;

	private Integer semester;
	private String tahunAjaran;
	private String keterangan;

	private PerguruanTinggi perguruanTinggi;
	private Boolean semuaBolehAbsen;

	private Jurusan jurusan;
	private Fakultas fakultas;

	/**
	 * @return representasi ringkas "{id}_{pegawai}_{semester}_{tahunAjaran}", dipakai untuk
	 *         keperluan log/debug.
	 */
	public String toString() {
		return getId() + "_" + getPegawai() + "_" + getSemester() + "_" + getTahunAjaran();
	}

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public AbsenPiketMahasiswa() {
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
	 * @return semester jadwal piket ini (1 ganjil, 2 genap); default dihitung dari kalender akademik
	 *         berjalan ({@link Common#isNowSemensterGanjil()}) bila belum diisi — getter-mutasi.
	 */
	@Column(name = "semester", nullable = false)
	public Integer getSemester() {
		if (semester == null) {
			semester = Common.isNowSemensterGanjil() ? 1 : 2;
		}
		return this.semester;
	}

	/**
	 * @param semester semester jadwal piket ini.
	 */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/**
	 * @return tahun ajaran jadwal piket ini; default tahun akademik berjalan
	 *         ({@link Common#getCurrentTahunAkademik()}) bila belum diisi — getter-mutasi.
	 */
	@Column(name = "tahun_ajaran", nullable = false, length = 9)
	public String getTahunAjaran() {

		if (tahunAjaran == null) {
			tahunAjaran = Common.getCurrentTahunAkademik();
		}
		return this.tahunAjaran;
	}

	/**
	 * @param tahunAjaran tahun ajaran jadwal piket ini.
	 */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/**
	 * @return keterangan/catatan bebas tentang jadwal piket ini.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return tanggal jadwal piket ini dibuat/berlaku; default waktu saat ini bila belum diisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal() {
		return tanggal == null ? WaktuUtil.getDate() : tanggal;
	}

	/**
	 * @param tanggal tanggal jadwal piket ini dibuat/berlaku.
	 */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/**
	 * @return perguruan tinggi pemilik jadwal piket ini; bila belum diisi, jatuh balik ke perguruan
	 *         tinggi tunggal aplikasi ({@code PerguruanTinggiUtil#getPerguruanTinggi()}), dengan
	 *         kegagalan lookup diserap diam-diam — getter-mutasi dengan fallback fail-safe.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "perguruan_tinggi")
	public PerguruanTinggi getPerguruanTinggi() {
		perguruanTinggi = check(perguruanTinggi);
		try {
			if (perguruanTinggi == null) {
				perguruanTinggi = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggi();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/AbsenPiketMahasiswa.java:164");
		}
		return perguruanTinggi;
	}

	/**
	 * @param perguruanTinggi perguruan tinggi pemilik jadwal piket ini.
	 */
	public void setPerguruanTinggi(PerguruanTinggi perguruanTinggi) {
		this.perguruanTinggi = perguruanTinggi;
	}

	/**
	 * @return {@code true} bila jadwal piket ini berlaku untuk SEMUA mahasiswa (lintas
	 *         jurusan/fakultas), mengabaikan {@link #getJurusan()}/{@link #getFakultas()}; default
	 *         {@code false}.
	 */
	public Boolean getSemuaBolehAbsen() {
		return semuaBolehAbsen == null ? false : semuaBolehAbsen;
	}

	/**
	 * @param semuaBolehAbsen status berlaku untuk semua mahasiswa atau tidak.
	 */
	public void setSemuaBolehAbsen(Boolean semuaBolehAbsen) {
		this.semuaBolehAbsen = semuaBolehAbsen;
	}

	/**
	 * @return pegawai yang bertugas piket pada jadwal ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/**
	 * @param pegawai pegawai yang bertugas piket pada jadwal ini.
	 */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * @return jurusan yang dicakup jadwal piket ini, atau {@code null} bila berlaku lintas jurusan
	 *         (lihat juga {@link #getSemuaBolehAbsen()}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jurusan")
	public Jurusan getJurusan() {
		jurusan = check(jurusan);
		return jurusan;
	}

	/**
	 * @param jurusan jurusan yang dicakup jadwal piket ini.
	 */
	public void setJurusan(Jurusan jurusan) {
		this.jurusan = jurusan;
	}

	/**
	 * @return fakultas yang dicakup jadwal piket ini — bila {@link #getJurusan()} terisi, fakultas
	 *         selalu DITURUNKAN dari jurusan itu ({@code jurusan.getFakultas()}), menimpa nilai kolom
	 *         {@code fakultas} tersimpan (getter-mutasi, konsisten dengan pola turunan fakultas dari
	 *         jurusan di entity sejenis seperti {@link TextBerjalan}).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "fakultas")
	public Fakultas getFakultas() {
		fakultas = check(fakultas);
		jurusan = getJurusan();
		if (jurusan != null) {
			fakultas = jurusan.getFakultas();
		}
		return fakultas;
	}

	/**
	 * @param fakultas fakultas yang dicakup jadwal piket ini (diabaikan saat dibaca kembali bila
	 *                 {@link #getJurusan()} terisi — lihat {@link #getFakultas()}).
	 */
	public void setFakultas(Fakultas fakultas) {
		this.fakultas = fakultas;
	}

}
