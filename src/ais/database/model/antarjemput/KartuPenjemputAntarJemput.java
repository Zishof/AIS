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
import ais.database.model.sekolah.Siswa;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "kartu_penjemput_antar_jemput")
public class KartuPenjemputAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439815L;

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private String namaPenjemput;
	private String hubungan;
	private String nomorIdentitas;
	private String nomorHp;
	private String nomorKartu;
	private String barcode;
	private Date berlakuMulai;
	private Date berlakuSampai;
	private Boolean aktif;

	private Siswa siswa;
	private Mahasiswa mahasiswa;
	private Guru guru;
	private Dosen dosen;
	private Pegawai pegawai;

	public KartuPenjemputAntarJemput() {
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
		return kode == null ? getNomorKartu() : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", length = 255)
	public String getNama() {
		return nama == null ? getNamaPenjemput() : nama.trim();
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

	@Column(name = "nama_penjemput", nullable = false, length = 255)
	public String getNamaPenjemput() {
		return namaPenjemput == null ? null : namaPenjemput.trim();
	}

	public void setNamaPenjemput(String namaPenjemput) {
		this.namaPenjemput = namaPenjemput;
	}

	@Column(name = "hubungan", length = 80)
	public String getHubungan() {
		return hubungan;
	}

	public void setHubungan(String hubungan) {
		this.hubungan = hubungan;
	}

	@Column(name = "nomor_identitas", length = 80)
	public String getNomorIdentitas() {
		return nomorIdentitas;
	}

	public void setNomorIdentitas(String nomorIdentitas) {
		this.nomorIdentitas = nomorIdentitas;
	}

	@Column(name = "nomor_hp", length = 40)
	public String getNomorHp() {
		return nomorHp;
	}

	public void setNomorHp(String nomorHp) {
		this.nomorHp = nomorHp;
	}

	@Column(name = "nomor_kartu", nullable = false, length = 100)
	public String getNomorKartu() {
		return nomorKartu == null ? null : nomorKartu.trim();
	}

	public void setNomorKartu(String nomorKartu) {
		this.nomorKartu = nomorKartu;
	}

	@Column(name = "barcode", length = 255)
	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	@Temporal(TemporalType.DATE)
	public Date getBerlakuMulai() {
		return berlakuMulai;
	}

	public void setBerlakuMulai(Date berlakuMulai) {
		this.berlakuMulai = berlakuMulai;
	}

	@Temporal(TemporalType.DATE)
	public Date getBerlakuSampai() {
		return berlakuSampai;
	}

	public void setBerlakuSampai(Date berlakuSampai) {
		this.berlakuSampai = berlakuSampai;
	}

	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
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
}
