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
@Table(schema = "employ", name = "mutasi_pindah")

public class MutasiPindah extends GeneralValueObject {

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

	public static final String DISETUJUI = "DISETUJUI";
	public static final String BELUM_DIPROSES = "BELUM DIPROSES";

	private Pegawai pegawai;
	private String noSuratUsul;
	private Date tanggalSuratUsul;
	private JenisPensiun jenisPensiun;
	private Golongan golonganTerakhir;
	private Date tanggalPensiun;
	private Date tmt;
	private String status;
	private UnitKerja unitKerjaAwal;
	private UnitKerja unitKerjaTujuan;
	private JabatanStruktural jabatanStrukturalAwal;
	private JabatanStruktural jabatanStrukturalAkhir;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/MutasiPindah.java:123");

		}

		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@Column(name = "no_surat_usul")
	public String getNoSuratUsul() {
		return noSuratUsul;
	}

	public void setNoSuratUsul(String noSuratUsul) {
		this.noSuratUsul = noSuratUsul;
	}

	@Column(name = "tanggal_surat_usul")
	public Date getTanggalSuratUsul() {
		return tanggalSuratUsul;
	}

	public void setTanggalSuratUsul(Date tanggalSuratUsul) {
		this.tanggalSuratUsul = tanggalSuratUsul;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jenisPensiun", nullable = true)
	public JenisPensiun getJenisPensiun() {
		return jenisPensiun;
	}

	public void setJenisPensiun(JenisPensiun jenisPensiun) {
		this.jenisPensiun = jenisPensiun;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "golongan_terakhir", nullable = true)
	public Golongan getGolonganTerakhir() {
		return golonganTerakhir;
	}

	public void setGolonganTerakhir(Golongan golonganTerakhir) {
		this.golonganTerakhir = golonganTerakhir;
	}

	@Column(name = "tanggal_pesiun")
	public Date getTanggalPensiun() {
		return tanggalPensiun;
	}

	public void setTanggalPensiun(Date tanggalPensiun) {
		this.tanggalPensiun = tanggalPensiun;
	}

	@Column(name = "status")
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "unit_kerja_awal")
	public UnitKerja getUnitKerjaAwal() {
		return unitKerjaAwal;
	}

	public void setUnitKerjaAwal(UnitKerja unitKerjaAwal) {
		this.unitKerjaAwal = unitKerjaAwal;
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "unit_kerja_tujuan")
	public UnitKerja getUnitKerjaTujuan() {
		return unitKerjaTujuan;
	}

	public void setUnitKerjaTujuan(UnitKerja unitKerjaTujuan) {
		this.unitKerjaTujuan = unitKerjaTujuan;
	}

	@Column(name = "tmt")
	public Date getTmt() {
		return tmt;
	}

	public void setTmt(Date tmt) {
		this.tmt = tmt;
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural_awal")
	public JabatanStruktural getJabatanStrukturalAwal() {
		return jabatanStrukturalAwal;
	}

	public void setJabatanStrukturalAwal(JabatanStruktural jabatanStrukturalAwal) {
		this.jabatanStrukturalAwal = jabatanStrukturalAwal;
	}

	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "jabatan_struktural_akhir")
	public JabatanStruktural getJabatanStrukturalAkhir() {
		return jabatanStrukturalAkhir;
	}

	public void setJabatanStrukturalAkhir(JabatanStruktural jabatanStrukturalAkhir) {
		this.jabatanStrukturalAkhir = jabatanStrukturalAkhir;
	}

}
