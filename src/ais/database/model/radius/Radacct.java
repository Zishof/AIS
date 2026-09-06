package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entitas Hibernate untuk tabel {@code radacct} pada basis data server RADIUS terpisah, mengikuti
 * skema BAKU FreeRADIUS: log akunting per SESI koneksi jaringan (mis. satu sesi Wi-Fi kampus dari
 * konek sampai putus). Setiap baris dibuat oleh {@code radiusd} saat menerima paket
 * {@code Accounting-Start} dan diperbarui saat {@code Accounting-Stop}, mencatat durasi sesi
 * ({@link #getAcctsessiontime()}), volume data naik/turun ({@link #getAcctinputoctets()}/
 * {@link #getAcctoutputoctets()}), serta identitas perangkat/koneksi (MAC address pemanggil,
 * alamat IP yang diberikan). Entitas ini besar (banyak kolom) karena mencerminkan atribut RADIUS
 * standar Accounting-Request apa adanya. Dibaca/ditulis langsung oleh proses FreeRADIUS di luar
 * aplikasi Java ini; sisi AIS memakainya (bila dipakai) terutama untuk pelaporan/monitoring
 * pemakaian jaringan, bukan untuk keputusan otentikasi.
 *
 * <p>
 * <b>Privasi</b>: {@link #getCallingstationid()} (MAC address perangkat pengguna) dan
 * {@link #getFramedipaddress()}/{@link #getNasipaddress()} (alamat IP) tergolong data yang dapat
 * dipakai untuk melacak aktivitas jaringan seseorang; perlakukan sebagai data yang perlu dibatasi
 * aksesnya seperti log jaringan pada umumnya.
 * </p>
 *
 * <p>Kelas ini tidak extends {@link ais.database.model.GeneralValueObject} dan tidak memakai
 * {@code @Audited}/Envers atau kolom audit {@code oleh}/{@code tanggal_dirubah}, karena mengikuti
 * skema standar FreeRADIUS apa adanya (dihasilkan otomatis oleh hbm2java).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(name = "radacct", schema = "public")
public class Radacct implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3417248046424872372L;
	/** ID baris (primary key numerik). */
	private long radacctid;
	/** ID sesi unik yang diberikan NAS (access point), dipakai untuk mengaitkan paket start/stop dari sesi yang sama. */
	private String acctsessionid;
	/** ID sesi unik lintas-NAS yang dihitung {@code radiusd} sendiri (kombinasi NAS+port+session), menjamin keunikan meski beberapa NAS memakai {@code acctsessionid} yang sama. */
	private String acctuniqueid;
	/** Nama pengguna yang memiliki sesi ini. */
	private String username;
	/** Nama grup RADIUS (lihat {@link Radusergroup}) milik pengguna saat sesi ini berlangsung. */
	private String groupname;
	/** Nama realm (domain otentikasi) bila dipakai, mis. untuk setup RADIUS multi-domain. */
	private String realm;
	/** Alamat IP Network Access Server (access point/switch) yang menangani sesi ini. */
	private Serializable nasipaddress;
	/** ID port fisik pada NAS yang dipakai untuk sesi ini. */
	private String nasportid;
	/** Tipe port NAS (mis. nirkabel/kabel) yang dipakai untuk sesi ini. */
	private String nasporttype;
	/** Waktu mulai sesi (saat NAS mengirim paket Accounting-Start). */
	private Date acctstarttime;
	/** Waktu selesai sesi (saat NAS mengirim paket Accounting-Stop), kosong bila sesi masih berlangsung. */
	private Date acctstoptime;
	/** Durasi sesi dalam detik. */
	private Long acctsessiontime;
	/** Metode autentikasi yang dipakai untuk sesi ini (mis. RADIUS, sistem lokal NAS). */
	private String acctauthentic;
	/** Info koneksi saat sesi dimulai (mis. kecepatan link), sesuai atribut {@code Connect-Info} dari NAS. */
	private String connectinfoStart;
	/** Info koneksi saat sesi berakhir. */
	private String connectinfoStop;
	/** Jumlah byte data yang diterima dari pengguna (upload) selama sesi. */
	private Long acctinputoctets;
	/** Jumlah byte data yang dikirim ke pengguna (download) selama sesi. */
	private Long acctoutputoctets;
	/** ID nomor yang dipanggil (identitas access point/SSID sisi jaringan). */
	private String calledstationid;
	/** ID nomor pemanggil — pada Wi-Fi umumnya berisi alamat MAC perangkat pengguna (lihat catatan privasi pada javadoc kelas). */
	private String callingstationid;
	/** Alasan sesi diakhiri (mis. logout pengguna, timeout, sesi diputus admin). */
	private String acctterminatecause;
	/** Jenis layanan yang diberikan untuk sesi ini (atribut RADIUS {@code Service-Type}). */
	private String servicetype;
	/** Kunci sesi internal server Ascend (kompatibilitas perangkat NAS lawas jenis Ascend). */
	private String xascendsessionsvrkey;
	/** Protokol framing yang dipakai (mis. PPP), sesuai atribut RADIUS {@code Framed-Protocol}. */
	private String framedprotocol;
	/** Alamat IP yang diberikan (di-assign) ke pengguna untuk sesi ini. */
	private Serializable framedipaddress;
	/** Penundaan (detik) sebelum paket Accounting-Start dikirim NAS, dipakai untuk kompensasi waktu retransmisi. */
	private Integer acctstartdelay;
	/** Penundaan (detik) sebelum paket Accounting-Stop dikirim NAS. */
	private Integer acctstopdelay;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radacct() {
	}

	/** Konstruktor ringkas untuk membuat baris {@code radacct} baru dengan kolom identitas sesi minimal (tanpa detail akunting). */
	public Radacct(long radacctid, String acctsessionid, String acctuniqueid,
			Serializable nasipaddress) {
		this.radacctid = radacctid;
		this.acctsessionid = acctsessionid;
		this.acctuniqueid = acctuniqueid;
		this.nasipaddress = nasipaddress;
	}

	/** Konstruktor lengkap untuk membuat baris {@code radacct} baru dengan seluruh kolom akunting sesi. */
	public Radacct(long radacctid, String acctsessionid, String acctuniqueid,
			String username, String groupname, String realm,
			Serializable nasipaddress, String nasportid, String nasporttype,
			Date acctstarttime, Date acctstoptime, Long acctsessiontime,
			String acctauthentic, String connectinfoStart,
			String connectinfoStop, Long acctinputoctets,
			Long acctoutputoctets, String calledstationid,
			String callingstationid, String acctterminatecause,
			String servicetype, String xascendsessionsvrkey,
			String framedprotocol, Serializable framedipaddress,
			Integer acctstartdelay, Integer acctstopdelay) {
		this.radacctid = radacctid;
		this.acctsessionid = acctsessionid;
		this.acctuniqueid = acctuniqueid;
		this.username = username;
		this.groupname = groupname;
		this.realm = realm;
		this.nasipaddress = nasipaddress;
		this.nasportid = nasportid;
		this.nasporttype = nasporttype;
		this.acctstarttime = acctstarttime;
		this.acctstoptime = acctstoptime;
		this.acctsessiontime = acctsessiontime;
		this.acctauthentic = acctauthentic;
		this.connectinfoStart = connectinfoStart;
		this.connectinfoStop = connectinfoStop;
		this.acctinputoctets = acctinputoctets;
		this.acctoutputoctets = acctoutputoctets;
		this.calledstationid = calledstationid;
		this.callingstationid = callingstationid;
		this.acctterminatecause = acctterminatecause;
		this.servicetype = servicetype;
		this.xascendsessionsvrkey = xascendsessionsvrkey;
		this.framedprotocol = framedprotocol;
		this.framedipaddress = framedipaddress;
		this.acctstartdelay = acctstartdelay;
		this.acctstopdelay = acctstopdelay;
	}

	/** @return ID baris (primary key). */
	@Id
	@Column(name = "radacctid", unique = true, nullable = false)
	public long getRadacctid() {
		return this.radacctid;
	}

	/** @param radacctid ID baris (primary key) yang akan diset. */
	public void setRadacctid(long radacctid) {
		this.radacctid = radacctid;
	}

	/** @return ID sesi unik yang diberikan NAS. */
	@Column(name = "acctsessionid", nullable = false, length = 64)
	public String getAcctsessionid() {
		return this.acctsessionid;
	}

	/** @param acctsessionid ID sesi yang akan diset. */
	public void setAcctsessionid(String acctsessionid) {
		this.acctsessionid = acctsessionid;
	}

	/** @return ID sesi unik lintas-NAS yang dihitung {@code radiusd}. */
	@Column(name = "acctuniqueid", nullable = false, length = 32)
	public String getAcctuniqueid() {
		return this.acctuniqueid;
	}

	/** @param acctuniqueid ID sesi unik lintas-NAS yang akan diset. */
	public void setAcctuniqueid(String acctuniqueid) {
		this.acctuniqueid = acctuniqueid;
	}

	/** @return nama pengguna pemilik sesi ini. */
	@Column(name = "username", length = 253)
	public String getUsername() {
		return this.username;
	}

	/** @param username nama pengguna yang akan diset. */
	public void setUsername(String username) {
		this.username = username;
	}

	/** @return nama grup RADIUS milik pengguna saat sesi ini. */
	@Column(name = "groupname", length = 253)
	public String getGroupname() {
		return this.groupname;
	}

	/** @param groupname nama grup RADIUS yang akan diset. */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	/** @return nama realm (domain otentikasi) sesi ini, bila dipakai. */
	@Column(name = "realm", length = 64)
	public String getRealm() {
		return this.realm;
	}

	/** @param realm nama realm yang akan diset. */
	public void setRealm(String realm) {
		this.realm = realm;
	}

	/** @return alamat IP Network Access Server yang menangani sesi ini. */
	@Column(name = "nasipaddress", nullable = false)
	public Serializable getNasipaddress() {
		return this.nasipaddress;
	}

	/** @param nasipaddress alamat IP NAS yang akan diset. */
	public void setNasipaddress(Serializable nasipaddress) {
		this.nasipaddress = nasipaddress;
	}

	/** @return ID port fisik NAS yang dipakai untuk sesi ini. */
	@Column(name = "nasportid", length = 15)
	public String getNasportid() {
		return this.nasportid;
	}

	/** @param nasportid ID port NAS yang akan diset. */
	public void setNasportid(String nasportid) {
		this.nasportid = nasportid;
	}

	/** @return tipe port NAS yang dipakai untuk sesi ini. */
	@Column(name = "nasporttype", length = 32)
	public String getNasporttype() {
		return this.nasporttype;
	}

	/** @param nasporttype tipe port NAS yang akan diset. */
	public void setNasporttype(String nasporttype) {
		this.nasporttype = nasporttype;
	}

	/** @return waktu mulai sesi. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "acctstarttime", length = 35)
	public Date getAcctstarttime() {
		return this.acctstarttime;
	}

	/** @param acctstarttime waktu mulai sesi yang akan diset. */
	public void setAcctstarttime(Date acctstarttime) {
		this.acctstarttime = acctstarttime;
	}

	/** @return waktu selesai sesi, kosong bila sesi masih berlangsung. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "acctstoptime", length = 35)
	public Date getAcctstoptime() {
		return this.acctstoptime;
	}

	/** @param acctstoptime waktu selesai sesi yang akan diset. */
	public void setAcctstoptime(Date acctstoptime) {
		this.acctstoptime = acctstoptime;
	}

	/** @return durasi sesi dalam detik. */
	@Column(name = "acctsessiontime")
	public Long getAcctsessiontime() {
		return this.acctsessiontime;
	}

	/** @param acctsessiontime durasi sesi (detik) yang akan diset. */
	public void setAcctsessiontime(Long acctsessiontime) {
		this.acctsessiontime = acctsessiontime;
	}

	/** @return metode autentikasi yang dipakai untuk sesi ini. */
	@Column(name = "acctauthentic", length = 32)
	public String getAcctauthentic() {
		return this.acctauthentic;
	}

	/** @param acctauthentic metode autentikasi yang akan diset. */
	public void setAcctauthentic(String acctauthentic) {
		this.acctauthentic = acctauthentic;
	}

	/** @return info koneksi saat sesi dimulai. */
	@Column(name = "connectinfo_start", length = 50)
	public String getConnectinfoStart() {
		return this.connectinfoStart;
	}

	/** @param connectinfoStart info koneksi awal yang akan diset. */
	public void setConnectinfoStart(String connectinfoStart) {
		this.connectinfoStart = connectinfoStart;
	}

	/** @return info koneksi saat sesi berakhir. */
	@Column(name = "connectinfo_stop", length = 50)
	public String getConnectinfoStop() {
		return this.connectinfoStop;
	}

	/** @param connectinfoStop info koneksi akhir yang akan diset. */
	public void setConnectinfoStop(String connectinfoStop) {
		this.connectinfoStop = connectinfoStop;
	}

	/** @return jumlah byte data yang diterima dari pengguna (upload) selama sesi. */
	@Column(name = "acctinputoctets")
	public Long getAcctinputoctets() {
		return this.acctinputoctets;
	}

	/** @param acctinputoctets jumlah byte upload yang akan diset. */
	public void setAcctinputoctets(Long acctinputoctets) {
		this.acctinputoctets = acctinputoctets;
	}

	/** @return jumlah byte data yang dikirim ke pengguna (download) selama sesi. */
	@Column(name = "acctoutputoctets")
	public Long getAcctoutputoctets() {
		return this.acctoutputoctets;
	}

	/** @param acctoutputoctets jumlah byte download yang akan diset. */
	public void setAcctoutputoctets(Long acctoutputoctets) {
		this.acctoutputoctets = acctoutputoctets;
	}

	/** @return ID nomor yang dipanggil (identitas access point/SSID). */
	@Column(name = "calledstationid", length = 50)
	public String getCalledstationid() {
		return this.calledstationid;
	}

	/** @param calledstationid ID nomor yang dipanggil, akan diset. */
	public void setCalledstationid(String calledstationid) {
		this.calledstationid = calledstationid;
	}

	/** @return ID nomor pemanggil (umumnya MAC address perangkat pengguna — lihat catatan privasi pada javadoc kelas). */
	@Column(name = "callingstationid", length = 50)
	public String getCallingstationid() {
		return this.callingstationid;
	}

	/** @param callingstationid ID nomor pemanggil yang akan diset. */
	public void setCallingstationid(String callingstationid) {
		this.callingstationid = callingstationid;
	}

	/** @return alasan sesi diakhiri. */
	@Column(name = "acctterminatecause", length = 32)
	public String getAcctterminatecause() {
		return this.acctterminatecause;
	}

	/** @param acctterminatecause alasan sesi diakhiri, akan diset. */
	public void setAcctterminatecause(String acctterminatecause) {
		this.acctterminatecause = acctterminatecause;
	}

	/** @return jenis layanan yang diberikan untuk sesi ini. */
	@Column(name = "servicetype", length = 32)
	public String getServicetype() {
		return this.servicetype;
	}

	/** @param servicetype jenis layanan yang akan diset. */
	public void setServicetype(String servicetype) {
		this.servicetype = servicetype;
	}

	/** @return kunci sesi internal server Ascend (kompatibilitas NAS lawas). */
	@Column(name = "xascendsessionsvrkey", length = 10)
	public String getXascendsessionsvrkey() {
		return this.xascendsessionsvrkey;
	}

	/** @param xascendsessionsvrkey kunci sesi Ascend yang akan diset. */
	public void setXascendsessionsvrkey(String xascendsessionsvrkey) {
		this.xascendsessionsvrkey = xascendsessionsvrkey;
	}

	/** @return protokol framing yang dipakai (mis. PPP). */
	@Column(name = "framedprotocol", length = 32)
	public String getFramedprotocol() {
		return this.framedprotocol;
	}

	/** @param framedprotocol protokol framing yang akan diset. */
	public void setFramedprotocol(String framedprotocol) {
		this.framedprotocol = framedprotocol;
	}

	/** @return alamat IP yang di-assign ke pengguna untuk sesi ini. */
	@Column(name = "framedipaddress")
	public Serializable getFramedipaddress() {
		return this.framedipaddress;
	}

	/** @param framedipaddress alamat IP yang di-assign, akan diset. */
	public void setFramedipaddress(Serializable framedipaddress) {
		this.framedipaddress = framedipaddress;
	}

	/** @return penundaan (detik) sebelum paket Accounting-Start dikirim NAS. */
	@Column(name = "acctstartdelay")
	public Integer getAcctstartdelay() {
		return this.acctstartdelay;
	}

	/** @param acctstartdelay penundaan start yang akan diset. */
	public void setAcctstartdelay(Integer acctstartdelay) {
		this.acctstartdelay = acctstartdelay;
	}

	/** @return penundaan (detik) sebelum paket Accounting-Stop dikirim NAS. */
	@Column(name = "acctstopdelay")
	public Integer getAcctstopdelay() {
		return this.acctstopdelay;
	}

	/** @param acctstopdelay penundaan stop yang akan diset. */
	public void setAcctstopdelay(Integer acctstopdelay) {
		this.acctstopdelay = acctstopdelay;
	}

}
