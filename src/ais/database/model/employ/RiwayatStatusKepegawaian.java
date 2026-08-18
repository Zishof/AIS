package ais.database.model.employ;

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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "employ", name = "riwayat_status_kepegawaian")

public class RiwayatStatusKepegawaian extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String keterangan;
	public static final String CPNS = "CPNS";
	public static final String PNS = "PNS";

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

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	private String statusKepegawaian;
	private String jenisPegawai;
	private String noSK;
	private Date tanggalSK;
	private Date tmt;
	private Golongan golongan;
	private JabatanStruktural jabatanStruktural;
	private JabatanFungsional jabatanFungsional;
	private String tahunAnggaran;

	private Date tanggalSKBKN;
	private String sKPejabat;
	private String sKPejabatNIP;
	private Date tmtCoba;
	private String noUjiSehat;
	private Date tanggalUjiSehat;
	private String noSTTPL;
	private Date tanggalSTTPL;
	private String tugas;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/RiwayatStatusKepegawaian.java:131");

		}

		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@Column(name = "status_kepegawaian", nullable = false)
	public String getStatusKepegawaian() {
		return statusKepegawaian;
	}

	public void setStatusKepegawaian(String statusKepegawaian) {
		this.statusKepegawaian = statusKepegawaian;
	}

	@Column(name = "jenis_pegawai", nullable = true)
	public String getJenisPegawai() {
		return jenisPegawai;
	}

	public void setJenisPegawai(String jenisPegawai) {
		this.jenisPegawai = jenisPegawai;
	}

	@Column(name = "no_SK", nullable = false)
	public String getNoSK() {
		return noSK;
	}

	public void setNoSK(String noSK) {
		this.noSK = noSK;
	}

	@Column(name = "tgl_SK", nullable = false)
	public Date getTanggalSK() {
		return tanggalSK;
	}

	public void setTanggalSK(Date tanggalSK) {
		this.tanggalSK = tanggalSK;
	}

	@Column(name = "tmt", nullable = false)
	public Date getTmt() {
		return tmt;
	}

	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "golongan", nullable = false)
	public Golongan getGolongan() {
		return golongan;
	}

	public void setGolongan(Golongan golongan) {
		this.golongan = golongan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural")
	public JabatanStruktural getJabatanStruktural() {
		return jabatanStruktural;
	}

	public void setJabatanStruktural(JabatanStruktural jabatanStruktural) {
		this.jabatanStruktural = jabatanStruktural;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_fungsional")
	public JabatanFungsional getJabatanFungsional() {
		return jabatanFungsional;
	}

	public void setJabatanFungsional(JabatanFungsional jabatanFungsional) {
		this.jabatanFungsional = jabatanFungsional;
	}

	@Column(name = "tahun_anggaran")
	public String getTahunAnggaran() {
		return tahunAnggaran;
	}

	public void setTahunAnggaran(String tahunAnggaran) {
		this.tahunAnggaran = tahunAnggaran;
	}

	@Column(name = "tanggal_sk_bkn")
	public Date getTanggalSKBKN() {
		return tanggalSKBKN;
	}

	public void setTanggalSKBKN(Date tanggalSKBKN) {
		this.tanggalSKBKN = tanggalSKBKN;
	}

	@Column(name = "sk_pejabat")
	public String getsKPejabat() {
		return sKPejabat;
	}

	public void setsKPejabat(String sKPejabat) {
		this.sKPejabat = sKPejabat;
	}

	@Column(name = "sk_pejabat_nip")
	public String getsKPejabatNIP() {
		return sKPejabatNIP;
	}

	public void setsKPejabatNIP(String sKPejabatNIP) {
		this.sKPejabatNIP = sKPejabatNIP;
	}

	@Column(name = "tmt_coba")
	public Date getTmtCoba() {
		return tmtCoba;
	}

	public void setTmtCoba(Date tmtCoba) {
		this.tmtCoba = tmtCoba;
	}

	@Column(name = "no_uji_sehat")
	public String getNoUjiSehat() {
		return noUjiSehat;
	}

	public void setNoUjiSehat(String noUjiSehat) {
		this.noUjiSehat = noUjiSehat;
	}

	@Column(name = "tanggal_uji_sehat")
	public Date getTanggalUjiSehat() {
		return tanggalUjiSehat;
	}

	public void setTanggalUjiSehat(Date tanggalUjiSehat) {
		this.tanggalUjiSehat = tanggalUjiSehat;
	}

	@Column(name = "no_sttpl")
	public String getNoSTTPL() {
		return noSTTPL;
	}

	public void setNoSTTPL(String noSTTPL) {
		this.noSTTPL = noSTTPL;
	}

	@Column(name = "tanggal_sttpl")
	public Date getTanggalSTTPL() {
		return tanggalSTTPL;
	}

	public void setTanggalSTTPL(Date tanggalSTTPL) {
		this.tanggalSTTPL = tanggalSTTPL;
	}

	@Column(name = "tugas")
	public String getTugas() {
		return tugas;
	}

	public void setTugas(String tugas) {
		this.tugas = tugas;
	}

}
