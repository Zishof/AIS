package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code radreply} pada basis data server RADIUS terpisah,
 * mengikuti skema BAKU <a href="https://wiki.freeradius.org/config/Sql">FreeRADIUS</a>: berbeda
 * dari {@link Radcheck} (atribut yang harus DICOCOKKAN agar otentikasi berhasil), baris di tabel
 * ini adalah atribut yang DIKIRIM KEMBALI oleh {@code radiusd} ke Network Access Server (access
 * point Wi-Fi) setelah otentikasi berhasil (mis. VLAN yang harus dipakai, batas bandwidth, alamat
 * IP yang di-assign) — dibaca langsung oleh proses FreeRADIUS di luar aplikasi Java ini.
 *
 * <p>Tidak seperti {@link Radcheck}, kolom {@link #getValue()} di sini pada umumnya BUKAN
 * password/kredensial — nilainya berupa parameter otorisasi jaringan. Kelas ini tidak
 * extends {@link ais.database.model.GeneralValueObject} dan tidak memakai {@code @Audited}/Envers
 * atau kolom audit {@code oleh}/{@code tanggal_dirubah}, karena mengikuti skema standar FreeRADIUS
 * apa adanya (dihasilkan otomatis oleh hbm2java).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(name = "radreply", schema = "public")
public class Radreply implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 608532494583874573L;
	/** ID baris (primary key numerik). */
	private int id;
	/** Nama pengguna (identitas login) yang menerima atribut balasan ini setelah otentikasi berhasil. */
	private String username;
	/** Nama atribut RADIUS yang dikirim balik ke NAS, mis. atribut VLAN/IP/batas bandwidth. */
	private String attribute;
	/** Operator RADIUS untuk atribut balasan ini, mis. {@code ":="} (assign) atau {@code "=="}. */
	private String op;
	/** Nilai atribut balasan yang dikirim ke NAS (parameter otorisasi jaringan, bukan kredensial). */
	private String value;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radreply() {
	}

	/** Konstruktor lengkap untuk membuat baris {@code radreply} baru dengan seluruh kolom. */
	public Radreply(int id, String username, String attribute, String op,
			String value) {
		this.id = id;
		this.username = username;
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

	/** @return nama pengguna penerima atribut balasan ini. */
	@Column(name = "username", nullable = false, length = 64)
	public String getUsername() {
		return this.username;
	}

	/** @param username nama pengguna yang akan diset. */
	public void setUsername(String username) {
		this.username = username;
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
