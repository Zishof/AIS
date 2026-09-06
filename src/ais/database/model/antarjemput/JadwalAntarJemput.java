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

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

/**
 * Entitas Hibernate untuk satu jadwal layanan antar-jemput siswa — dipetakan ke tabel
 * {@code public.jadwal_antar_jemput} (modul {@code antarjemput}). Menggabungkan sebuah
 * {@link RuteAntarJemput} (rute) dengan {@link KendaraanAntarJemput} (kendaraan) dan kru
 * (sopir + hingga 3 kenek/pendamping) pada tanggal/jam/hari tertentu, dengan status siklus hidup
 * {@link #DRAFT} → {@link #AKTIF} → {@link #SELESAI} (atau {@link #BATAL}).
 *
 * <h2>Fallback nilai default</h2>
 * <p>
 * Beberapa getter mengisi nilai secara otomatis bila belum di-set eksplisit: {@link #getNama()}
 * memakai nama {@link #ruteAntarJemput} sebagai fallback; {@link #getSopir()} memakai sopir dari
 * {@link #kendaraanAntarJemput} sebagai fallback bila sopir jadwal ini belum ditentukan sendiri;
 * {@link #getTahunAjaran()} dan {@link #getSemester()} memakai tahun akademik/semester berjalan
 * ({@code Common.getCurrentTahunAkademik()}/{@code Common.isNowSemensterGanjil()}) sebagai
 * default; {@link #getStatus()} default {@link #DRAFT}; {@link #getAktif()} default {@code true}.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "jadwal_antar_jemput")
public class JadwalAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439813L;

	/** Jadwal baru dibuat, belum diaktifkan/dijalankan. */
	public static final String DRAFT = "DRAFT";
	/** Jadwal aktif/sedang berjalan. */
	public static final String AKTIF = "AKTIF";
	/** Jadwal sudah selesai dijalankan. */
	public static final String SELESAI = "SELESAI";
	/** Jadwal dibatalkan. */
	public static final String BATAL = "BATAL";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	/** Nama jadwal; bila belum diisi, fallback ke nama {@link #ruteAntarJemput} — lihat {@link #getNama()}. */
	private String nama;
	private String keterangan;
	/** Tanggal spesifik jadwal ini berlaku (untuk jadwal sekali jalan/non-berulang). */
	private Date tanggal;
	private Date jamMulai;
	private Date jamSelesai;
	/** Hari berulang jadwal ini berlaku (mis. untuk jadwal rutin mingguan), bebas format teks. */
	private String hari;
	/** Tahun ajaran; default tahun akademik berjalan bila belum di-set — lihat {@link #getTahunAjaran()}. */
	private String tahunAjaran;
	/** Semester (1=ganjil, 2=genap); default semester berjalan bila belum di-set — lihat {@link #getSemester()}. */
	private Integer semester;
	/** Status siklus hidup jadwal — salah satu {@link #DRAFT}/{@link #AKTIF}/{@link #SELESAI}/{@link #BATAL}, default {@link #DRAFT}. */
	private String status;
	private Boolean aktif;

	private RuteAntarJemput ruteAntarJemput;
	private KendaraanAntarJemput kendaraanAntarJemput;
	/** Sopir jadwal ini; bila belum di-set, fallback ke sopir {@link #kendaraanAntarJemput} — lihat {@link #getSopir()}. */
	private Pegawai sopir;
	private Pegawai kenek1;
	private Pegawai kenek2;
	private Pegawai kenek3;

	/** Konstruktor default (dibutuhkan Hibernate). */
	public JadwalAntarJemput() {
	}

	/** @return ID unik baris jadwal (primary key, auto-increment via {@code IDENTITY}). */
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

	/** @return kode singkat jadwal ini, di-trim; {@code null} bila belum diisi. */
	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	/** @param kode lihat {@link #getKode()}. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama jadwal, di-trim. Bila belum diisi manual, fallback ke nama
	 *         {@link #getRuteAntarJemput()} (rute yang dipakai jadwal ini); hasil fallback itu
	 *         ikut di-cache ke field {@link #nama} in-memory (bukan murni derived getter — nilai
	 *         hasil fallback bisa ikut tersimpan ke DB bila objek ini kemudian di-flush/di-save
	 *         ulang oleh Hibernate).
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null && getRuteAntarJemput() != null) {
			nama = getRuteAntarJemput().getNama();
		}
		return nama == null ? null : nama.trim();
	}

	/** @param nama lihat {@link #getNama()}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/catatan bebas untuk jadwal ini. */
	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	/** @param keterangan lihat {@link #getKeterangan()}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @return tanggal spesifik berlakunya jadwal ini (dipakai untuk jadwal sekali-jalan/non-berulang; untuk jadwal rutin lihat {@link #getHari()}). */
	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	/** @param tanggal lihat {@link #getTanggal()}. */
	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	/** @return jam mulai/keberangkatan jadwal ini (hanya komponen waktu yang dipersist — lihat {@code @Temporal(TIME)}). */
	@Temporal(TemporalType.TIME)
	public Date getJamMulai() {
		return jamMulai;
	}

	/** @param jamMulai lihat {@link #getJamMulai()}. */
	public void setJamMulai(Date jamMulai) {
		this.jamMulai = jamMulai;
	}

	/** @return jam selesai/estimasi tiba jadwal ini (hanya komponen waktu yang dipersist). */
	@Temporal(TemporalType.TIME)
	public Date getJamSelesai() {
		return jamSelesai;
	}

	/** @param jamSelesai lihat {@link #getJamSelesai()}. */
	public void setJamSelesai(Date jamSelesai) {
		this.jamSelesai = jamSelesai;
	}

	/** @return hari berulang jadwal ini berlaku (mis. untuk jadwal rutin mingguan); format teks bebas, tidak divalidasi terhadap daftar nama hari tertentu. */
	@Column(name = "hari", length = 20)
	public String getHari() {
		return hari;
	}

	/** @param hari lihat {@link #getHari()}. */
	public void setHari(String hari) {
		this.hari = hari;
	}

	/** @return tahun ajaran jadwal ini; bila belum di-set, fallback (tanpa di-cache ke field) ke tahun akademik berjalan dari {@code Common.getCurrentTahunAkademik()}. */
	@Column(name = "tahun_ajaran", length = 9)
	public String getTahunAjaran() {
		return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	/** @param tahunAjaran lihat {@link #getTahunAjaran()}. */
	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	/** @return semester jadwal ini (1=ganjil, 2=genap); bila belum di-set, fallback (tanpa di-cache ke field) ke semester berjalan dari {@code Common.isNowSemensterGanjil()}. */
	public Integer getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? 1 : 2) : semester;
	}

	/** @param semester lihat {@link #getSemester()}. */
	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	/** @return status siklus hidup jadwal; default {@link #DRAFT} bila belum di-set (tidak di-cache ke field). */
	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? DRAFT : status;
	}

	/** @param status lihat {@link #getStatus()}; nilai valid: {@link #DRAFT}, {@link #AKTIF}, {@link #SELESAI}, {@link #BATAL}. Tidak divalidasi terhadap konstanta ini oleh setter — pemanggil bertanggung jawab menjaga konsistensi. */
	public void setStatus(String status) {
		this.status = status;
	}

	/** @return {@code true} bila jadwal aktif; default {@code true} bila belum di-set (tidak di-cache ke field). */
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/** @param aktif lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/** @return rute yang dipakai jadwal ini (relasi lazy); dilewatkan {@code check()} agar proxy Hibernate yang sudah dihapus/tidak valid tidak ikut terekspos ke pemanggil. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "rute_antar_jemput")
	public RuteAntarJemput getRuteAntarJemput() {
		ruteAntarJemput = check(ruteAntarJemput);
		return ruteAntarJemput;
	}

	/** @param ruteAntarJemput lihat {@link #getRuteAntarJemput()}. */
	public void setRuteAntarJemput(RuteAntarJemput ruteAntarJemput) {
		this.ruteAntarJemput = ruteAntarJemput;
	}

	/** @return kendaraan yang dipakai jadwal ini (relasi lazy); juga sumber fallback sopir bila sopir jadwal ini belum di-set — lihat {@link #getSopir()}. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kendaraan_antar_jemput")
	public KendaraanAntarJemput getKendaraanAntarJemput() {
		kendaraanAntarJemput = check(kendaraanAntarJemput);
		return kendaraanAntarJemput;
	}

	/** @param kendaraanAntarJemput lihat {@link #getKendaraanAntarJemput()}. */
	public void setKendaraanAntarJemput(KendaraanAntarJemput kendaraanAntarJemput) {
		this.kendaraanAntarJemput = kendaraanAntarJemput;
	}

	/**
	 * @return sopir yang bertugas pada jadwal ini. Bila belum di-set eksplisit pada jadwal ini,
	 *         fallback ke sopir default {@link #getKendaraanAntarJemput()} (dan hasil fallback itu
	 *         ikut di-cache ke field {@link #sopir} in-memory — pola yang sama seperti
	 *         {@link #getNama()}). Dilewatkan {@code check()} agar proxy Hibernate yang sudah
	 *         dihapus/tidak valid tidak ikut terekspos ke pemanggil.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sopir")
	public Pegawai getSopir() {
		if (sopir == null && getKendaraanAntarJemput() != null) {
			sopir = getKendaraanAntarJemput().getSopir();
		}
		sopir = check(sopir);
		return sopir;
	}

	/** @param sopir lihat {@link #getSopir()}. */
	public void setSopir(Pegawai sopir) {
		this.sopir = sopir;
	}

	/** @return kenek/pendamping pertama pada jadwal ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek1")
	public Pegawai getKenek1() {
		kenek1 = check(kenek1);
		return kenek1;
	}

	/** @param kenek1 lihat {@link #getKenek1()}. */
	public void setKenek1(Pegawai kenek1) {
		this.kenek1 = kenek1;
	}

	/** @return kenek/pendamping kedua pada jadwal ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek2")
	public Pegawai getKenek2() {
		kenek2 = check(kenek2);
		return kenek2;
	}

	/** @param kenek2 lihat {@link #getKenek2()}. */
	public void setKenek2(Pegawai kenek2) {
		this.kenek2 = kenek2;
	}

	/** @return kenek/pendamping ketiga pada jadwal ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek3")
	public Pegawai getKenek3() {
		kenek3 = check(kenek3);
		return kenek3;
	}

	/** @param kenek3 lihat {@link #getKenek3()}. */
	public void setKenek3(Pegawai kenek3) {
		this.kenek3 = kenek3;
	}
}
