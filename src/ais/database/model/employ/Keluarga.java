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

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.payroll.AsuransiPegawai;

@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "employ", name = "keluarga")
public class Keluarga extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1129196121609467759L;

	public static final String ISTRI = "ISTRI";
	public static final String SUAMI = "SUAMI";
	public static final String ANAK = "ANAK";
	public static final String MERTUA = "MERTUA";
	public static final String ORANG_TUA = "ORANG_TUA";
	public static final String SAUDARA = "SAUDARA";
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
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

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	private String hubungan;
	private String nama;
	private String tempatLahir;
	private Date tanggalLahir;
	private Date tanggalNikah;
	private String jenisKelamin;
	private String alamat;
	private String pekerjaan;
	private String keteranganTambahan;
	private Boolean status = false;
	private Boolean menikah = false;
	private Pendidikan pendidikan;
	private String jurusanPendidikan;

	private AsuransiPegawai asuransiPegawai1;
	private String nomorAsuransiPegawai1;
	private Double premiAsuransi1;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "asuransi_pegawai1")
	public AsuransiPegawai getAsuransiPegawai1() {
		asuransiPegawai1 = check(asuransiPegawai1);
		return asuransiPegawai1;
	}

	public void setAsuransiPegawai1(AsuransiPegawai asuransiPegawai1) {
		this.asuransiPegawai1 = asuransiPegawai1;
	}

	@Column(name = "menikah")
	public Boolean getMenikah() {
		return menikah;
	}

	public void setMenikah(Boolean menikah) {
		this.menikah = menikah;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pendidikan")
	public Pendidikan getPendidikan() {
		pendidikan = check(pendidikan);
		return pendidikan;
	}

	public void setPendidikan(Pendidikan pendidikan) {
		this.pendidikan = pendidikan;
	}

	@Column(name = "jurusan_pendidikan")
	public String getJurusanPendidikan() {
		return jurusanPendidikan;
	}

	public void setJurusanPendidikan(String jurusanPendidikan) {
		this.jurusanPendidikan = jurusanPendidikan;
	}

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pegawai", nullable = false)
	public Pegawai getPegawai() {
		pegawai = check(pegawai);

		try {
			if (pegawai == null) {
				pegawai = Common.getCurrentUser().getPegawai();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/employ/Keluarga.java:174");

		}

		return pegawai;
	}

	public void setPegawai(Pegawai pegawai) {
		this.pegawai = pegawai;
	}

	@Column(name = "hubungan", nullable = false)
	public String getHubungan() {
		return hubungan;
	}

	public void setHubungan(String hubungan) {
		this.hubungan = hubungan;
	}

	@Column(name = "nama", nullable = false)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "tempat_lahir", nullable = false)
	public String getTempatLahir() {
		return tempatLahir;
	}

	public void setTempatLahir(String tempatLahir) {
		this.tempatLahir = tempatLahir;
	}

	@Column(name = "tanggal_lahir", nullable = false)
	public Date getTanggalLahir() {
		return tanggalLahir;
	}

	public void setTanggalLahir(Date tanggalLahir) {
		this.tanggalLahir = tanggalLahir;
	}

	@Column(name = "jenis_kelamin", nullable = false)
	public String getJenisKelamin() {
		return jenisKelamin;
	}

	public void setJenisKelamin(String jenisKelamin) {
		this.jenisKelamin = jenisKelamin;
	}

	@Column(name = "alamat", nullable = false)
	public String getAlamat() {
		return alamat == null || alamat.trim().isEmpty() ? (getPegawai() == null ? "" : getPegawai().getAlamat())
				: alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	@Column(name = "pekerjaan", nullable = true)
	public String getPekerjaan() {
		return pekerjaan;
	}

	public void setPekerjaan(String pekerjaan) {
		this.pekerjaan = pekerjaan;
	}

	@Column(name = "keterangan_tambahan", nullable = false)
	public String getKeteranganTambahan() {
		return keteranganTambahan;
	}

	public void setKeteranganTambahan(String keteranganTambahan) {
		this.keteranganTambahan = keteranganTambahan;
	}

	public Boolean getStatus() {
		if (status == null) {
			status = false;
		}
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}

	@Temporal(TemporalType.DATE)
	public Date getTanggalNikah() {
		return tanggalNikah;
	}

	public void setTanggalNikah(Date tanggalNikah) {
		this.tanggalNikah = tanggalNikah;
	}

	public String getNomorAsuransiPegawai1() {
		return nomorAsuransiPegawai1;
	}

	public void setNomorAsuransiPegawai1(String nomorAsuransiPegawai1) {
		this.nomorAsuransiPegawai1 = nomorAsuransiPegawai1;
	}

	public Double getPremiAsuransi1() {
		if (getAsuransiPegawai1() != null && (premiAsuransi1 == null || premiAsuransi1.intValue() == 0)) {
			premiAsuransi1 = getAsuransiPegawai1().getTarif();
		}
		return premiAsuransi1 == null ? 0.0 : premiAsuransi1;
	}

	public void setPremiAsuransi1(Double premiAsuransi1) {
		this.premiAsuransi1 = premiAsuransi1;
	}

}
