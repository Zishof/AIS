package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code radusergroup} pada basis data server RADIUS terpisah,
 * mengikuti skema BAKU FreeRADIUS: memetakan satu {@code username} ke satu {@code groupname},
 * dengan {@code priority} menentukan urutan evaluasi bila satu user tergabung di beberapa grup
 * (angka lebih kecil dievaluasi lebih dulu oleh {@code radiusd}). Baris di sini menjadi dasar bagi
 * {@code radiusd} untuk menggabungkan aturan {@link Radgroupcheck}/{@link Radgroupreply} milik
 * grup ke dalam evaluasi otentikasi/otorisasi user yang bersangkutan. Dibaca langsung oleh proses
 * FreeRADIUS di luar aplikasi Java ini.
 *
 * <p>
 * <b>Catatan pemetaan Hibernate</b>: {@code @Id} pada kelas ini adalah {@link #getGroupname()},
 * BUKAN {@code username} — sesuai definisi hbm2java asli dari skema tabel (walau secara konsep
 * bisnis FreeRADIUS, kombinasi {@code username}+{@code groupname} yang idealnya unik per baris;
 * primary key tunggal pada {@code groupname} berarti Hibernate hanya dapat memuat SATU baris per
 * {@code groupname} lewat {@code session.get()}, meski tabel fisik FreeRADIUS mengizinkan banyak
 * baris per grup dengan {@code username} berbeda — perhatikan ini bila kelas ini dipakai lewat
 * {@code get}/{@code load} alih-alih query eksplisit).
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
@Table(name = "radusergroup", schema = "public")
public class Radusergroup implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4512928435659956374L;
	/** Nama grup RADIUS (primary key pada pemetaan Hibernate ini — lihat catatan pada javadoc kelas). */
	private String groupname;
	/** Nama pengguna yang menjadi anggota grup ini. */
	private String username;
	/** Prioritas evaluasi bila user tergabung di beberapa grup (angka lebih kecil dievaluasi lebih dulu). */
	private int priority;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radusergroup() {
	}

	/** Konstruktor lengkap untuk membuat baris {@code radusergroup} baru dengan seluruh kolom. */
	public Radusergroup(String groupname, String username, int priority) {
		this.groupname = groupname;
		this.username = username;
		this.priority = priority;
	}

	/** @return nama grup RADIUS (primary key). */
	@Id
	@Column(name = "groupname", unique = true, nullable = false, length = 64)
	public String getGroupname() {
		return this.groupname;
	}

	/** @param groupname nama grup RADIUS yang akan diset. */
	public void setGroupname(String groupname) {
		this.groupname = groupname;
	}

	/** @return nama pengguna anggota grup ini. */
	@Column(name = "username", nullable = false, length = 64)
	public String getUsername() {
		return this.username;
	}

	/** @param username nama pengguna yang akan diset. */
	public void setUsername(String username) {
		this.username = username;
	}

	/** @return prioritas evaluasi grup untuk user ini. */
	@Column(name = "priority", nullable = false)
	public int getPriority() {
		return this.priority;
	}

	/** @param priority prioritas evaluasi yang akan diset. */
	public void setPriority(int priority) {
		this.priority = priority;
	}

}
