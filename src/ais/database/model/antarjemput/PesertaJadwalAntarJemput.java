package ais.database.model.antarjemput;

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

import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;

/**
 * Entitas Hibernate untuk tabel {@code public.peserta_jadwal_antar_jemput}, merepresentasikan
 * satu orang peserta (siswa/mahasiswa/guru/dosen/pegawai) yang terdaftar berlangganan pada suatu
 * {@link JadwalAntarJemput} (jadwal rute layanan antar-jemput/shuttle sekolah atau perguruan
 * tinggi). Modul "antarjemput" mengelola operasional kendaraan antar-jemput siswa/pegawai.
 * <p>
 * Peserta selalu terhubung ke tepat satu jadwal lewat {@link #getJadwalAntarJemput()}, sedangkan
 * relasi ke siapa orangnya bersifat polimorfik: hanya salah satu dari {@link #getSiswa()},
 * {@link #getMahasiswa()}, {@link #getGuru()}, {@link #getDosen()}, atau {@link #getPegawai()}
 * yang terisi tergantung jenis peserta (siswa untuk modul sekolah, mahasiswa untuk modul
 * perguruan tinggi, dst). {@link #getNama()} mengambil nama dari field lokal atau, bila kosong,
 * dari salah satu relasi tersebut sesuai urutan prioritas siswa &gt; mahasiswa &gt; guru &gt;
 * dosen &gt; pegawai. {@link #getKelasSiswa()} adalah cache kelas siswa yang otomatis diisi dari
 * {@link #getSiswa()} bila peserta adalah siswa.
 * <p>
 * Perubahan tercatat historisnya lewat {@link Audited} (Hibernate Envers).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "peserta_jadwal_antar_jemput")
public class PesertaJadwalAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439814L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private Integer nomorUrut;
	/** Lokasi/alamat titik penjemputan peserta. */
	private String titikJemput;
	/** Lokasi/alamat titik penurunan peserta. */
	private String titikTurun;
	/** Catatan kondisi kesehatan peserta yang relevan bagi petugas antar-jemput. */
	private String catatanKesehatan;
	/** Status langganan layanan antar-jemput peserta ini, mis. "AKTIF"/nonaktif; default "AKTIF". */
	private String statusLangganan;
	private Boolean aktif;

	/** Jadwal/rute antar-jemput yang diikuti peserta ini. */
	private JadwalAntarJemput jadwalAntarJemput;
	/** Terisi bila peserta adalah siswa (modul sekolah). */
	private Siswa siswa;
	/** Terisi bila peserta adalah mahasiswa (modul perguruan tinggi). */
	private Mahasiswa mahasiswa;
	/** Terisi bila peserta adalah guru (modul sekolah). */
	private Guru guru;
	/** Terisi bila peserta adalah dosen (modul perguruan tinggi). */
	private Dosen dosen;
	/** Terisi bila peserta adalah pegawai/staf. */
	private Pegawai pegawai;
	/** Cache kelas siswa, otomatis diisi dari {@link #siswa} bila kosong. */
	private KelasSiswa kelasSiswa;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public PesertaJadwalAntarJemput() {
	}

	/** @return ID unik baris peserta (primary key, auto-increment via {@code IDENTITY}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id lihat {@link #getId()}. Normalnya tidak perlu diisi manual — dihasilkan DB saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return ID pengguna (username) yang terakhir mengubah baris ini. Field audit shadow — lihat {@link #getOleh()}. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Setter {@link #getOlehId()}. Nilai kosong/blank diabaikan (no-op) agar jejak audit lama
	 * tidak tertimpa saat proses simpan tidak membawa identitas pengguna — pola baku di semua
	 * entitas modul antarjemput.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return nama pengguna yang terakhir mengubah baris ini (field audit shadow, diisi via {@link #onUpdate()}). */
	public String getOleh() {
		return oleh;
	}

	/** Setter {@link #getOleh()}. Nilai kosong/blank diabaikan (no-op), sama seperti {@link #setOlehId(String)}. */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum baris ini
	 * di-UPDATE, memperbarui {@link #tanggal_dirubah} (dan field audit terkait) lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return timestamp terakhir baris ini diubah; diisi otomatis saat objek dibuat dan diperbarui via {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah lihat {@link #getTanggal_dirubah()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return kode singkat peserta ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama peserta, di-trim bila diisi manual pada field {@link #nama}. Bila kosong,
	 *         jatuh berurutan ke nama {@link #getSiswa()}, lalu {@link #getMahasiswa()}, lalu
	 *         {@link #getGuru()}, lalu {@link #getDosen()}, dan terakhir {@link #getPegawai()} —
	 *         urutan prioritas ini mencerminkan hanya SATU dari kelima relasi tersebut yang
	 *         diharapkan terisi per baris (lihat javadoc kelas). Hasil fallback TIDAK di-cache ke
	 *         field {@link #nama} (berbeda dari pola fallback di entitas antarjemput lain seperti
	 *         {@link JadwalAntarJemput#getNama()}) — setiap pemanggilan mengevaluasi ulang relasi.
	 */
	@Column(name = "nama", length = 255)
	public String getNama() {
		if (nama != null) {
			return nama.trim();
		}
		if (getSiswa() != null) {
			return getSiswa().getNama();
		}
		if (getMahasiswa() != null) {
			return getMahasiswa().getNama();
		}
		if (getGuru() != null) {
			return getGuru().getNama();
		}
		if (getDosen() != null) {
			return getDosen().getNama();
		}
		return getPegawai() == null ? null : getPegawai().getNama();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk peserta ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return nomor urut peserta ini pada jadwalnya (dipakai untuk mengurutkan pemanggilan peserta per rute); default {@code 0} bila belum di-set. */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	/** @param nomorUrut lihat {@link #getNomorUrut()}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	/** @return lokasi/alamat titik penjemputan peserta ini. */
	@Column(name = "titik_jemput")
	public String getTitikJemput() {
		return titikJemput;
	}

	/** @param titikJemput lihat {@link #getTitikJemput()}. */
	public void setTitikJemput(String titikJemput) {
		this.titikJemput = titikJemput;
	}

	/** @return lokasi/alamat titik penurunan peserta ini. */
	@Column(name = "titik_turun")
	public String getTitikTurun() {
		return titikTurun;
	}

	/** @param titikTurun lihat {@link #getTitikTurun()}. */
	public void setTitikTurun(String titikTurun) {
		this.titikTurun = titikTurun;
	}

	/**
	 * @return catatan kondisi kesehatan peserta yang relevan bagi petugas antar-jemput (mis.
	 *         alergi, kondisi medis yang perlu diwaspadai kru selama perjalanan). Data sensitif —
	 *         entitas ini tidak menerapkan pembatasan akses sendiri; pembatasan siapa yang boleh
	 *         membaca field ini (petugas/kru vs. pihak lain) sepenuhnya bergantung pada layar/aksi
	 *         pemanggil (lihat catatan kepemilikan pada javadoc kelas
	 *         {@link ais.database.model.antarjemput.LogNotifikasiAntarJemput}).
	 */
	@Column(name = "catatan_kesehatan")
	public String getCatatanKesehatan() {
		return catatanKesehatan;
	}

	/** @param catatanKesehatan lihat {@link #getCatatanKesehatan()}. */
	public void setCatatanKesehatan(String catatanKesehatan) {
		this.catatanKesehatan = catatanKesehatan;
	}

	/** @return status langganan layanan antar-jemput peserta ini; default {@code "AKTIF"} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "status_langganan", length = 30)
	public String getStatusLangganan() {
		return statusLangganan == null ? "AKTIF" : statusLangganan;
	}

	/** @param statusLangganan lihat {@link #getStatusLangganan()}. */
	public void setStatusLangganan(String statusLangganan) {
		this.statusLangganan = statusLangganan;
	}

	/** @return {@code true} bila peserta aktif; default {@code true} bila belum di-set (tidak di-cache ke field). */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return jadwal/rute antar-jemput yang diikuti peserta ini (relasi lazy); dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_antar_jemput")
	public JadwalAntarJemput getJadwalAntarJemput() {
		jadwalAntarJemput = check(jadwalAntarJemput);
		return jadwalAntarJemput;
	}

	/** @param jadwalAntarJemput lihat {@link #getJadwalAntarJemput()}. */
	public void setJadwalAntarJemput(JadwalAntarJemput jadwalAntarJemput) {
		this.jadwalAntarJemput = jadwalAntarJemput;
	}

	/** @return siswa yang diwakili peserta ini, bila jenis pesertanya siswa (modul sekolah); {@code null} bila peserta jenis lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	/** @param siswa lihat {@link #getSiswa()}. */
	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	/** @return mahasiswa yang diwakili peserta ini, bila jenis pesertanya mahasiswa (modul perguruan tinggi); {@code null} bila peserta jenis lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	/** @param mahasiswa lihat {@link #getMahasiswa()}. */
	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	/** @return guru yang diwakili peserta ini, bila jenis pesertanya guru (modul sekolah); {@code null} bila peserta jenis lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	/** @param guru lihat {@link #getGuru()}. */
	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	/** @return dosen yang diwakili peserta ini, bila jenis pesertanya dosen (modul perguruan tinggi); {@code null} bila peserta jenis lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	/** @param dosen lihat {@link #getDosen()}. */
	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	/** @return pegawai/staf yang diwakili peserta ini, bila jenis pesertanya pegawai; {@code null} bila peserta jenis lain. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	/** @param pegawai lihat {@link #getPegawai()}. */
	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	/**
	 * @return kelas siswa peserta ini. Bila belum di-set langsung dan peserta ini adalah siswa
	 *         ({@link #getSiswa()} tidak null), diisi otomatis dari kelas siswa tersebut dan
	 *         hasilnya ikut di-cache ke field {@link #kelasSiswa} in-memory (pola fallback yang
	 *         sama seperti {@link JadwalAntarJemput#getNama()}). Dilewatkan {@code check()} agar
	 *         proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		if (kelasSiswa == null && getSiswa() != null) {
			kelasSiswa = getSiswa().getKelas();
		}
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	/** @param kelasSiswa lihat {@link #getKelasSiswa()}. */
	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}
}
