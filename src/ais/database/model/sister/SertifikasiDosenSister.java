package ais.database.model.sister;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas hasil sinkronisasi SISTER untuk data <b>sertifikasi_dosen (per SDM/dosen)</b>.
 * Kolom {@code kode} = id item di SISTER (dipakai kunci upsert); {@code keterangan} menyimpan JSON
 * mentah lengkap agar tidak ada data yang hilang; kolom bernama lain memetakan field penting agar
 * mudah di-query. Kolom dibuat otomatis via hbm2ddl (skema public). @Audited: WAJIB ALTER tabel
 * __audit di InitIndex.java.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "sertifikasi_dosen_sister")
public class SertifikasiDosenSister extends GeneralValueObject {

	private static final long serialVersionUID = 1L;
	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
	private String kode;
	private String keterangan;
	private Boolean aktif;
	private String idSdm;
	private String jenisSertifikasi;
	private String bidangStudi;
	private String tahunSertifikasi;
	private String skSertifikasi;
	private String nomorRegistrasi;

	public SertifikasiDosenSister() {
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) { return; }
		this.olehId = olehId;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) { return; }
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

	@Column(name = "kode")
	public String getKode() {
		return kode == null || kode.isEmpty() ? null : kode.trim();
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "keterangan", columnDefinition = "text")
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@Column(name = "aktif")
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "id_sdm")
	public String getIdSdm() {
		return idSdm;
	}

	public void setIdSdm(String idSdm) {
		this.idSdm = idSdm;
	}

	@Column(name = "jenis_sertifikasi")
	public String getJenisSertifikasi() {
		return jenisSertifikasi;
	}

	public void setJenisSertifikasi(String jenisSertifikasi) {
		this.jenisSertifikasi = jenisSertifikasi;
	}

	@Column(name = "bidang_studi")
	public String getBidangStudi() {
		return bidangStudi;
	}

	public void setBidangStudi(String bidangStudi) {
		this.bidangStudi = bidangStudi;
	}

	@Column(name = "tahun_sertifikasi")
	public String getTahunSertifikasi() {
		return tahunSertifikasi;
	}

	public void setTahunSertifikasi(String tahunSertifikasi) {
		this.tahunSertifikasi = tahunSertifikasi;
	}

	@Column(name = "sk_sertifikasi", columnDefinition = "text")
	public String getSkSertifikasi() {
		return skSertifikasi;
	}

	public void setSkSertifikasi(String skSertifikasi) {
		this.skSertifikasi = skSertifikasi;
	}

	@Column(name = "nomor_registrasi")
	public String getNomorRegistrasi() {
		return nomorRegistrasi;
	}

	public void setNomorRegistrasi(String nomorRegistrasi) {
		this.nomorRegistrasi = nomorRegistrasi;
	}

	@Override
	public String toString() {
		return id + "-" + kode;
	}
}
