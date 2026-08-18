package ais.database.model.employ;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;



@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "kenaikan_gaji_berkala")



public class KenaikanGajiBerkala extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;private String olehId;public String getOlehId() {return olehId;}public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}this.olehId = olehId;}
	private String keterangan;
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	public String getOleh() {
		return oleh;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public String toString() {
		return keterangan;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	private Pegawai pegawai;
	private String noSK;
	private Date tanggalSk;
	private Integer masaKerjaTahun;
	private Integer masaKerjaBulan;
	private GajiPokok gajiPokokBaru;
	private Date tmt;
	private Date naikBerikutnya;
	private String status;
	private GajiPokok gaji;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@Column(name = "no_sk")
	public String getNoSK() {
		return noSK;
	}

	public void setNoSK(String noSK) {
		this.noSK = noSK;
	}

	@Column(name = "tanggal_sk")
	public Date getTanggalSK() {
		return tanggalSk;
	}

	public void setTanggalSK(Date tanggalSK) {
		this.tanggalSk = tanggalSK;
	}

	@Column(name = "masa_kerja_tahun", nullable = false)
	public Integer getMasaKerjaTahun() {
		return masaKerjaTahun;
	}

	public void setMasaKerjaTahun(Integer masaKerjaTahun) {
		this.masaKerjaTahun = masaKerjaTahun;
	}

	@Column(name = "masa_kerja_bulan", nullable = false)
	public Integer getMasaKerjaBulan() {
		return masaKerjaBulan;
	}

	public void setMasaKerjaBulan(Integer masaKerjaBulan) {
		this.masaKerjaBulan = masaKerjaBulan;
	}

	@Column(name = "status", nullable = false)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gaji_pokok_baru", nullable = false)
	public GajiPokok getGajiPokokBaru() {
		return gajiPokokBaru;
	}

	public void setGajiPokokBaru(GajiPokok gajiPokokBaru) {
		this.gajiPokokBaru = gajiPokokBaru;
	}

	@Column(name = "tmt", nullable = false)
	public Date getTmt() {
		return tmt;
	}

	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	@Column(name = "naik_berikutnya")
	public Date getNaikBerikutnya() {
		return naikBerikutnya;
	}

	public void setNaikBerikutnya(Date naikBerikutnya) {
		this.naikBerikutnya = naikBerikutnya;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gaji", nullable = false)
	public GajiPokok getGaji() {
		return gaji;
	}

	public void setGaji(GajiPokok gaji) {
		this.gaji = gaji;
	}

}
