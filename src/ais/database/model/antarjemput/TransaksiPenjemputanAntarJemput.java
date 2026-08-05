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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "transaksi_penjemputan_antar_jemput")
public class TransaksiPenjemputanAntarJemput extends GeneralValueObject {

	private static final long serialVersionUID = 2463821577548439816L;

	public static final String MENUNGGU = "MENUNGGU";
	public static final String DIPANGGIL = "DIPANGGIL";
	public static final String SELESAI = "SELESAI";
	public static final String DITOLAK = "DITOLAK";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private String kode;
	private String nama;
	private String keterangan;
	private Date waktuScan;
	private String tipeScan;
	private String nomorScan;
	private String pintuGerbang;
	private String nomorAntrian;
	private String status;

	private JadwalAntarJemput jadwalAntarJemput;
	private KartuPenjemputAntarJemput kartuPenjemputAntarJemput;
	private Pegawai satpam;

	public TransaksiPenjemputanAntarJemput() {
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
		return nama == null ? getNomorAntrian() : nama.trim();
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

	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktuScan() {
		return waktuScan == null ? ais.ui.util.WaktuUtil.getDate() : waktuScan;
	}

	public void setWaktuScan(Date waktuScan) {
		this.waktuScan = waktuScan;
	}

	@Column(name = "tipe_scan", length = 30)
	public String getTipeScan() {
		return tipeScan == null ? "KARTU" : tipeScan;
	}

	public void setTipeScan(String tipeScan) {
		this.tipeScan = tipeScan;
	}

	@Column(name = "nomor_scan", length = 255)
	public String getNomorScan() {
		return nomorScan;
	}

	public void setNomorScan(String nomorScan) {
		this.nomorScan = nomorScan;
	}

	@Column(name = "pintu_gerbang", length = 100)
	public String getPintuGerbang() {
		return pintuGerbang;
	}

	public void setPintuGerbang(String pintuGerbang) {
		this.pintuGerbang = pintuGerbang;
	}

	@Column(name = "nomor_antrian", length = 50)
	public String getNomorAntrian() {
		return nomorAntrian;
	}

	public void setNomorAntrian(String nomorAntrian) {
		this.nomorAntrian = nomorAntrian;
	}

	@Column(name = "status", length = 30)
	public String getStatus() {
		return status == null ? MENUNGGU : status;
	}

	public void setStatus(String status) {
		this.status = status;
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
	@JoinColumn(name = "kartu_penjemput_antar_jemput")
	public KartuPenjemputAntarJemput getKartuPenjemputAntarJemput() {
		kartuPenjemputAntarJemput = check(kartuPenjemputAntarJemput);
		return kartuPenjemputAntarJemput;
	}

	public void setKartuPenjemputAntarJemput(KartuPenjemputAntarJemput kartuPenjemputAntarJemput) {
		this.kartuPenjemputAntarJemput = kartuPenjemputAntarJemput;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satpam")
	public Pegawai getSatpam() {
		satpam = check(satpam);
		return satpam;
	}

	public void setSatpam(Pegawai satpam) {
		this.satpam = satpam;
	}
}
