package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code radgroupreply} pada basis data server RADIUS terpisah,
 * mengikuti skema BAKU FreeRADIUS: analog dengan {@link Radreply}, tetapi berlaku untuk seluruh
 * anggota satu GRUP RADIUS ({@code groupname}, lihat {@link Radusergroup} untuk pemetaan
 * user-ke-grup) — parameter otorisasi jaringan (bukan kredensial) yang dikirim balik ke Network
 * Access Server untuk semua anggota grup sekaligus, mis. profil VLAN/bandwidth seragam per grup.
 * Dibaca langsung oleh proses FreeRADIUS di luar aplikasi Java ini.
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
@Table(name = "radgroupreply", schema = "public")
public class Radgroupreply implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 2197468735508249371L;
	/** ID baris (primary key numerik). */
	private int id;
	/** Nama grup RADIUS (lihat {@link Radusergroup}) yang atribut balasan ini berlaku untuknya. */
	private String groupname;
	/** Nama atribut RADIUS yang dikirim balik ke NAS untuk seluruh anggota grup. */
	private String attribute;
	/** Operator RADIUS untuk atribut balasan ini, mis. {@code ":="}. */
	private String op;
	/** Nilai atribut balasan yang dikirim ke NAS (parameter otorisasi jaringan, bukan kredensial). */
	private String value;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radgroupreply() {
	}

	/** Konstruktor lengkap untuk membuat baris {@code radgroupreply} baru dengan seluruh kolom. */
	public Radgroupreply(int id, String groupname, String attribute, String op,
			String value) {
		this.id = id;
		this.groupname = groupname;
		this.attribute = attribute;
		this.op = op;
		this.value = value;
	}

	/** @return ID baris (primary key). */
	@Id
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(int id) {
		this.id = id;
	}

	/** @return nama grup RADIUS yang atribut balasan ini berlaku untuknya. */
	@Column(name = "groupname", nullable = false, length = 64)
	public String getGroupname() {
		return this.groupname;
	}

	/** @param groupname nama grup RADIUS yang akan diset. */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	/** @return nama atribut RADIUS yang dikirim balik ke NAS. */
	@Column(name = "attribute", nullable = false, length = 64)
	public String getAttribute() {
		return this.attribute;
	}

	/** @param attribute nama atribut RADIUS yang akan diset. */
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	/** @return operator RADIUS untuk atribut balasan ini. */
	@Column(name = "op", nullable = false, length = 2)
	public String getOp() {
		return this.op;
	}

	/** @param op operator RADIUS yang akan diset. */
	public void setOp(String op) {
		this.op = op;
	}

	/** @return nilai atribut balasan (parameter otorisasi jaringan). */
	@Column(name = "value", nullable = false, length = 253)
	public String getValue() {
		return this.value;
	}

	/** @param value nilai atribut balasan yang akan diset. */
	public void setValue(String value) {
		this.value = value;
	}

}
