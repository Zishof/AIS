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

	public JadwalAntarJemput() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "kode", length = 50)
	public String getKode() {
		return kode == null ? null : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (nama == null && getRuteAntarJemput() != null) {
			nama = getRuteAntarJemput().getNama();
		}
		return nama == null ? null : nama.trim();
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "keterangan")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggal() {
		return tanggal;
	}

	public void setTanggal(Date tanggal) {
		this.tanggal = tanggal;
	}

	@Temporal(TemporalType.TIME)
	public Date getJamMulai() {
		return jamMulai;
	}

	public void setJamMulai(Date jamMulai) {
		this.jamMulai = jamMulai;
	}

	@Temporal(TemporalType.TIME)
	public Date getJamSelesai() {
		return jamSelesai;
	}

	public void setJamSelesai(Date jamSelesai) {
		this.jamSelesai = jamSelesai;
	}

	@Column(name = "hari", length = 20)
	public String getHari() {
		return hari;
	}

	public void setHari(String hari) {
		this.hari = hari;
	}

	@Column(name = "tahun_ajaran", length = 9)
	public String getTahunAjaran() {
		return tahunAjaran == null ? Common.getCurrentTahunAkademik() : tahunAjaran;
	}

	public void setTahunAjaran(String tahunAjaran) {
		this.tahunAjaran = tahunAjaran;
	}

	public Integer getSemester() {
		return semester == null ? (Common.isNowSemensterGanjil() ? 1 : 2) : semester;
	}

	public void setSemester(Integer semester) {
		this.semester = semester;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? DRAFT : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "rute_antar_jemput")
	public RuteAntarJemput getRuteAntarJemput() {
		ruteAntarJemput = check(ruteAntarJemput);
		return ruteAntarJemput;
	}

	public void setRuteAntarJemput(RuteAntarJemput ruteAntarJemput) {
		this.ruteAntarJemput = ruteAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kendaraan_antar_jemput")
	public KendaraanAntarJemput getKendaraanAntarJemput() {
		kendaraanAntarJemput = check(kendaraanAntarJemput);
		return kendaraanAntarJemput;
	}

	public void setKendaraanAntarJemput(KendaraanAntarJemput kendaraanAntarJemput) {
		this.kendaraanAntarJemput = kendaraanAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "sopir")
	public Pegawai getSopir() {
		if (sopir == null && getKendaraanAntarJemput() != null) {
			sopir = getKendaraanAntarJemput().getSopir();
		}
		sopir = check(sopir);
		return sopir;
	}

	public void setSopir(Pegawai sopir) {
		this.sopir = sopir;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek1")
	public Pegawai getKenek1() {
		kenek1 = check(kenek1);
		return kenek1;
	}

	public void setKenek1(Pegawai kenek1) {
		this.kenek1 = kenek1;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek2")
	public Pegawai getKenek2() {
		kenek2 = check(kenek2);
		return kenek2;
	}

	public void setKenek2(Pegawai kenek2) {
		this.kenek2 = kenek2;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kenek3")
	public Pegawai getKenek3() {
		kenek3 = check(kenek3);
		return kenek3;
	}

	public void setKenek3(Pegawai kenek3) {
		this.kenek3 = kenek3;
	}
}
