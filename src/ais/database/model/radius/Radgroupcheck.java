package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code radgroupcheck} pada basis data server RADIUS terpisah,
 * mengikuti skema BAKU FreeRADIUS: analog dengan {@link Radcheck}, tetapi berlaku untuk seluruh
 * anggota satu GRUP RADIUS ({@code groupname}, lihat {@link Radusergroup} untuk pemetaan
 * user-ke-grup) alih-alih satu user tunggal — dipakai untuk aturan cek otentikasi yang seragam
 * bagi sekelompok pengguna sekaligus (mis. seluruh akun dengan profil jaringan yang sama). Dibaca
 * langsung oleh proses FreeRADIUS di luar aplikasi Java ini.
 *
 * <p>
 * <b>Keamanan</b>: seperti {@link Radcheck}, bila atribut yang disimpan adalah kredensial (mis.
 * {@code "Password"}), kolom {@link #getValue()} akan berisi kredensial PLAINTEXT yang berlaku
 * untuk seluruh anggota grup — lihat catatan keamanan lengkap pada
 * {@link ais.action.master.helper.util.RadiusProcessor} dan {@link Radcheck}. Pemakaian aktual di
 * AIS saat ini hanya menulis ke {@link Radcheck} per-user (lihat
 * {@link ais.action.master.helper.util.RadiusProcessor#doProcess()}); tabel ini bagian dari skema
 * standar FreeRADIUS yang tersedia namun belum tentu diisi otomatis oleh kode AIS.
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
@Table(name = "radgroupcheck", schema = "public")
public class Radgroupcheck implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -4500860838992788274L;
	/** ID baris (primary key numerik). */
	private int id;
	/** Nama grup RADIUS (lihat {@link Radusergroup}) yang aturan cek ini berlaku untuknya. */
	private String groupname;
	/** Nama atribut RADIUS yang dicek, mis. {@code "Password"} (lihat catatan keamanan pada javadoc kelas). */
	private String attribute;
	/** Operator perbandingan RADIUS, mis. {@code "=="}. */
	private String op;
	/** Nilai yang harus dipenuhi/dicocokkan untuk seluruh anggota grup ini — untuk atribut kredensial, lihat catatan keamanan pada javadoc kelas. */
	private String value;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radgroupcheck() {
	}

	/** Konstruktor lengkap untuk membuat baris {@code radgroupcheck} baru dengan seluruh kolom. */
	public Radgroupcheck(int id, String groupname, String attribute, String op,
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

	/** @return nama grup RADIUS yang aturan cek ini berlaku untuknya. */
	@Column(name = "groupname", nullable = false, length = 64)
	public String getGroupname() {
		return this.groupname;
	}

	/** @param groupname nama grup RADIUS yang akan diset. */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	/** @return nama atribut RADIUS yang dicek. */
	@Column(name = "attribute", nullable = false, length = 64)
	public String getAttribute() {
		return this.attribute;
	}

	/** @param attribute nama atribut RADIUS yang akan diset. */
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	/** @return operator perbandingan RADIUS. */
	@Column(name = "op", nullable = false, length = 2)
	public String getOp() {
		return this.op;
	}

	/** @param op operator perbandingan RADIUS yang akan diset. */
	public void setOp(String op) {
		this.op = op;
	}

	/**
	 * @return nilai atribut yang dicek untuk seluruh anggota grup — untuk atribut kredensial,
	 *         nilai ini tersimpan sebagai plaintext (lihat catatan keamanan pada javadoc kelas).
	 */
	@Column(name = "value", nullable = false, length = 253)
	public String getValue() {
		return this.value;
	}

	/** @param value nilai atribut yang akan diset. */
	public void setValue(String value) {
		this.value = value;
	}

}
