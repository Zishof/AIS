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
	private String titikJemput;
	private String titikTurun;
	private String catatanKesehatan;
	private String statusLangganan;
	private Boolean aktif;

	private JadwalAntarJemput jadwalAntarJemput;
	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private Guru guru;
	private Dosen dosen;
	private Pegawai pegawai;
	private KelasSiswa kelasSiswa;

	public PesertaJadwalAntarJemput() {
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

	public Integer getNomorUrut() {
		return nomorUrut == null ? 0 : nomorUrut;
	}

	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

	@Column(name = "titik_jemput")
	public String getTitikJemput() {
		return titikJemput;
	}

	public void setTitikJemput(String titikJemput) {
		this.titikJemput = titikJemput;
	}

	@Column(name = "titik_turun")
	public String getTitikTurun() {
		return titikTurun;
	}

	public void setTitikTurun(String titikTurun) {
		this.titikTurun = titikTurun;
	}

	@Column(name = "catatan_kesehatan")
	public String getCatatanKesehatan() {
		return catatanKesehatan;
	}

	public void setCatatanKesehatan(String catatanKesehatan) {
		this.catatanKesehatan = catatanKesehatan;
	}

	@Column(name = "status_langganan", length = 30)
	public String getStatusLangganan() {
		return statusLangganan == null ? "AKTIF" : statusLangganan;
	}

	public void setStatusLangganan(String statusLangganan) {
		this.statusLangganan = statusLangganan;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jadwal_antar_jemput")
	public JadwalAntarJemput getJadwalAntarJemput() {
		jadwalAntarJemput = check(jadwalAntarJemput);
		return jadwalAntarJemput;
	}

	public void setJadwalAntarJemput(JadwalAntarJemput jadwalAntarJemput) {
		this.jadwalAntarJemput = jadwalAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		dosen = check(dosen);
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		pegawai = check(pegawai);
		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		if (kelasSiswa == null && getSiswa() != null) {
			kelasSiswa = getSiswa().getKelas();
		}
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}
}
