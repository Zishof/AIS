package ais.database.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.apache.commons.lang.StringUtils;
import org.hibernate.envers.Audited;

import ais.common.Common;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "badanhukum")

public class BadanHukum extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5232831172545879880L;
	private Long id;
	private String oleh;
	private String olehId;

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
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
		return nama;
	}

	private String kode;
	private String nama;
	private String alamat1;
	private String alamat2;
	private String kota;
	private String kodePos;
	private String telepon;
	private String faksimil;
	private Date tanggalAkta;
	private String namaAkta;
	private Date tanggalPengesahan;
	private String nomorPengesahan;
	private Date tanggalAwalPendirian;
	private String email;
	private String alamatWebsite;
	private String logo;

	public BadanHukum() {

	}

	public BadanHukum(Long id, String kode, String nama, String alamat1, String alamat2, String kota, String kodePos,
			String telepon, String faksimil, Date tanggalAkta, String namaAkta, Date tanggalPengesahan,
			String nomorPengesahan, Date tanggalAwalPendirian, String email, String alamatWebsite, String logo) {
		super();
		this.id = id;
		this.kode = kode;
		this.nama = nama;
		this.alamat1 = alamat1;
		this.alamat2 = alamat2;
		this.kota = kota;
		this.kodePos = kodePos;
		this.telepon = telepon;
		this.faksimil = faksimil;
		this.tanggalAkta = tanggalAkta;
		this.namaAkta = namaAkta;
		this.tanggalPengesahan = tanggalPengesahan;
		this.nomorPengesahan = nomorPengesahan;
		this.tanggalAwalPendirian = tanggalAwalPendirian;
		this.email = email;
		this.alamatWebsite = alamatWebsite;
		this.logo = logo;
	}

	@Id
	@Column(name = "id", insertable = false, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "kode", length = 150)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "nama", length = 150)
	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	@Column(name = "alamat1")
	public String getAlamat1() {
		return alamat1;
	}

	public void setAlamat1(String alamat1) {
		this.alamat1 = alamat1;
	}

	@Column(name = "alamat2")
	public String getAlamat2() {
		return alamat2;
	}

	public void setAlamat2(String alamat2) {
		this.alamat2 = alamat2;
	}

	@Column(name = "kota", length = 150)
	public String getKota() {
		return kota;
	}

	public void setKota(String kota) {
		this.kota = kota;
	}

	@Column(name = "kodepos", length = 150)
	public String getKodePos() {
		return kodePos;
	}

	public void setKodePos(String kodePos) {
		this.kodePos = kodePos;
	}

	@Column(name = "telepon", length = 100)
	public String getTelepon() {
		return telepon;
	}

	public void setTelepon(String telepon) {
		this.telepon = telepon;
	}

	@Column(name = "faksimil", length = 100)
	public String getFaksimil() {
		return faksimil;
	}

	public void setFaksimil(String faksimil) {
		this.faksimil = faksimil;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalakta", length = 0)
	public Date getTanggalAkta() {
		return tanggalAkta;
	}

	public void setTanggalAkta(Date tanggalAkta) {
		this.tanggalAkta = tanggalAkta;
	}

	@Column(name = "namaakta", length = 150)
	public String getNamaAkta() {
		return namaAkta;
	}

	public void setNamaAkta(String namaAkta) {
		this.namaAkta = namaAkta;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalpengesahan", length = 0)
	public Date getTanggalPengesahan() {
		return tanggalPengesahan;
	}

	public void setTanggalPengesahan(Date tanggalPengesahan) {
		this.tanggalPengesahan = tanggalPengesahan;
	}

	@Column(name = "nomorpengesahan", length = 100)
	public String getNomorPengesahan() {
		return nomorPengesahan;
	}

	public void setNomorPengesahan(String nomorPengesahan) {
		this.nomorPengesahan = nomorPengesahan;
	}

	@Temporal(TemporalType.DATE)
	@Column(name = "tanggalawalpendirian", length = 0)
	public Date getTanggalAwalPendirian() {
		return tanggalAwalPendirian;
	}

	public void setTanggalAwalPendirian(Date tanggalAwalPendirian) {
		this.tanggalAwalPendirian = tanggalAwalPendirian;
	}

	@Column(name = "email", length = 255)
	public String getEmail() {
		if (email != null && email.contains(",,")) {
			for (int i = 0; i < 5; i++) {
				email = email.replaceAll(",,", ",");
			}
		}
		if (email == null) {
			email = "";
		}
		if (email.trim().equals(",")) {
			email = "";
		}
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void appendEmail(String email) {
		if (this.email != null && email != null && !email.trim().isEmpty() && StringUtils.contains(this.email, email)) {
			return;
		}
		if (email != null && !email.trim().isEmpty() && Common.isValidEmailAddress(email) && !email.startsWith("@")) {
			this.email = this.email == null || this.email.trim().isEmpty() ? email : this.email + "," + email;
		}
	}

	@Column(name = "alamatwebsite", length = 100)
	public String getAlamatWebsite() {
		return alamatWebsite;
	}

	public void setAlamatWebsite(String alamatWebsite) {
		this.alamatWebsite = alamatWebsite;
	}

	@Column(name = "logo", length = 100)
	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

}
