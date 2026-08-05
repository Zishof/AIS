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
@Table(schema = "public", name = "detail_penjemputan_antar_jemput")
public class DetailPenjemputanAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439817L;

	public static final String MENUNGGU_PANGGILAN = "MENUNGGU_PANGGILAN";
	public static final String SUDAH_DIPANGGIL = "SUDAH_DIPANGGIL";
	public static final String KELUAR_KELAS = "KELUAR_KELAS";
	public static final String DISERAHKAN = "DISERAHKAN";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private String teksPanggilan;
	private String perangkatTujuan;
	private String statusPanggilan;
	private Date waktuDipanggil;
	private Date waktuKeluarKelas;
	private Date waktuSerahTerima;

	private TransaksiPenjemputanAntarJemput transaksiPenjemputanAntarJemput;
	private PesertaJadwalAntarJemput pesertaJadwalAntarJemput;
	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private Guru guru;
	private Dosen dosen;
	private Pegawai pegawai;
	private KelasSiswa kelasSiswa;

	public DetailPenjemputanAntarJemput() {
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
		if (getPesertaJadwalAntarJemput() != null) {
			return getPesertaJadwalAntarJemput().getNama();
		}
		return null;
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

	@Column(name = "teks_panggilan")
	public String getTeksPanggilan() {
		if (teksPanggilan == null && getNama() != null) {
			teksPanggilan = "Penjemputan ananda " + getNama() + " sudah datang.";
		}
		return teksPanggilan;
	}

	public void setTeksPanggilan(String teksPanggilan) {
		this.teksPanggilan = teksPanggilan;
	}

	@Column(name = "perangkat_tujuan", length = 255)
	public String getPerangkatTujuan() {
		return perangkatTujuan;
	}

	public void setPerangkatTujuan(String perangkatTujuan) {
		this.perangkatTujuan = perangkatTujuan;
	}

	@Column(name = "status_panggilan", length = 40)
	public String getStatusPanggilan() {
		return statusPanggilan == null ? MENUNGGU_PANGGILAN : statusPanggilan;
	}

	public void setStatusPanggilan(String statusPanggilan) {
		this.statusPanggilan = statusPanggilan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuDipanggil() {
		return waktuDipanggil;
	}

	public void setWaktuDipanggil(Date waktuDipanggil) {
		this.waktuDipanggil = waktuDipanggil;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuKeluarKelas() {
		return waktuKeluarKelas;
	}

	public void setWaktuKeluarKelas(Date waktuKeluarKelas) {
		this.waktuKeluarKelas = waktuKeluarKelas;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuSerahTerima() {
		return waktuSerahTerima;
	}

	public void setWaktuSerahTerima(Date waktuSerahTerima) {
		this.waktuSerahTerima = waktuSerahTerima;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_penjemputan_antar_jemput")
	public TransaksiPenjemputanAntarJemput getTransaksiPenjemputanAntarJemput() {
		transaksiPenjemputanAntarJemput = check(transaksiPenjemputanAntarJemput);
		return transaksiPenjemputanAntarJemput;
	}

	public void setTransaksiPenjemputanAntarJemput(
			TransaksiPenjemputanAntarJemput transaksiPenjemputanAntarJemput) {
		this.transaksiPenjemputanAntarJemput = transaksiPenjemputanAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "peserta_jadwal_antar_jemput")
	public PesertaJadwalAntarJemput getPesertaJadwalAntarJemput() {
		pesertaJadwalAntarJemput = check(pesertaJadwalAntarJemput);
		return pesertaJadwalAntarJemput;
	}

	public void setPesertaJadwalAntarJemput(PesertaJadwalAntarJemput pesertaJadwalAntarJemput) {
		this.pesertaJadwalAntarJemput = pesertaJadwalAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "siswa")
	public Siswa getSiswa() {
		if (siswa == null && getPesertaJadwalAntarJemput() != null) {
			siswa = getPesertaJadwalAntarJemput().getSiswa();
		}
		siswa = check(siswa);
		return siswa;
	}

	public void setSiswa(Siswa siswa) {
		this.siswa = siswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "mahasiswa")
	public Mahasiswa getMahasiswa() {
		if (mahasiswa == null && getPesertaJadwalAntarJemput() != null) {
			mahasiswa = getPesertaJadwalAntarJemput().getMahasiswa();
		}
		mahasiswa = check(mahasiswa);
		return mahasiswa;
	}

	public void setMahasiswa(Mahasiswa mahasiswa) {
		this.mahasiswa = mahasiswa;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "guru")
	public Guru getGuru() {
		if (guru == null && getPesertaJadwalAntarJemput() != null) {
			guru = getPesertaJadwalAntarJemput().getGuru();
		}
		guru = check(guru);
		return guru;
	}

	public void setGuru(Guru guru) {
		this.guru = guru;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dosen")
	public Dosen getDosen() {
		if (dosen == null && getPesertaJadwalAntarJemput() != null) {
			dosen = getPesertaJadwalAntarJemput().getDosen();
		}
		dosen = check(dosen);
		return dosen;
	}

	public void setDosen(Dosen dosen) {
		this.dosen = dosen;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai")
	public Pegawai getPegawai() {
		if (pegawai == null && getPesertaJadwalAntarJemput() != null) {
			pegawai = getPesertaJadwalAntarJemput().getPegawai();
		}
		pegawai = check(pegawai);
		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_siswa")
	public KelasSiswa getKelasSiswa() {
		if (kelasSiswa == null && getPesertaJadwalAntarJemput() != null) {
			kelasSiswa = getPesertaJadwalAntarJemput().getKelasSiswa();
		}
		kelasSiswa = check(kelasSiswa);
		return kelasSiswa;
	}

	public void setKelasSiswa(KelasSiswa kelasSiswa) {
		this.kelasSiswa = kelasSiswa;
	}
}
