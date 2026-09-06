package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * Entitas Hibernate untuk tabel {@code radpostauth} pada basis data server RADIUS terpisah,
 * mengikuti skema BAKU FreeRADIUS: log HASIL setiap percobaan otentikasi (berhasil atau gagal),
 * dibuat oleh modul {@code postauth} FreeRADIUS setelah proses cek {@link Radcheck}/
 * {@link Radgroupcheck} selesai. Dipakai untuk audit/troubleshooting login Wi-Fi (mis. melacak
 * percobaan login gagal berulang). Dibaca/ditulis langsung oleh proses FreeRADIUS di luar aplikasi
 * Java ini.
 *
 * <p>
 * <b>Keamanan — WASPADAI kolom {@link #getPass()}</b>: sesuai konvensi standar modul
 * {@code postauth} FreeRADIUS, kolom ini biasa diisi dengan password yang DIKIRIM PENGGUNA saat
 * percobaan otentikasi (baik yang berhasil maupun gagal) — untuk PAP ini berarti password
 * PLAINTEXT tersimpan permanen di log, termasuk untuk percobaan yang GAGAL (mis. salah ketik),
 * yang tetap membocorkan password mendekati asli. Ini memperbesar permukaan risiko yang sudah
 * dicatat pada {@link Radcheck} dan {@link ais.action.master.helper.util.RadiusProcessor}: siapa
 * pun dengan akses baca ke {@code radpostauth} berpotensi mengumpulkan password pengguna dari
 * riwayat log, bukan hanya dari baris cek aktif. Mitigasi standar FreeRADIUS adalah menonaktifkan
 * logging {@code User-Password}/{@code Password} di modul {@code postauth} (via
 * {@code radiusd.conf}), atau membatasi akses baca ke tabel ini secermat mungkin.
 * </p>
 *
 * <p>{@link #getReply()} mencatat hasil (mis. {@code "Access-Accept"}/{@code "Access-Reject"}),
 * bukan atribut balasan lengkap seperti {@link Radreply}. Kelas ini tidak extends
 * {@link ais.database.model.GeneralValueObject} dan tidak memakai {@code @Audited}/Envers atau
 * kolom audit {@code oleh}/{@code tanggal_dirubah}, karena mengikuti skema standar FreeRADIUS apa
 * adanya (dihasilkan otomatis oleh hbm2java).</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(name = "radpostauth", schema = "public")
public class Radpostauth implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -4590644091453347848L;
	/** ID baris (primary key numerik). */
	private long id;
	/** Nama pengguna yang mencoba otentikasi. */
	private String username;
	/** Password yang dikirim pengguna saat percobaan ini — lihat catatan keamanan pada javadoc kelas (berpotensi plaintext, tercatat walau percobaan gagal). */
	private String pass;
	/** Hasil percobaan otentikasi, mis. {@code "Access-Accept"} atau {@code "Access-Reject"}. */
	private String reply;
	/** ID nomor yang dipanggil (identitas access point/SSID) saat percobaan ini. */
	private String calledstationid;
	/** ID nomor pemanggil (umumnya MAC address perangkat pengguna) saat percobaan ini. */
	private String callingstationid;
	/** Waktu percobaan otentikasi dilakukan. */
	private Date authdate;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radpostauth() {
	}

	/** Konstruktor ringkas untuk membuat baris {@code radpostauth} baru dengan identitas dan waktu percobaan saja (tanpa detail password/reply). */
	public Radpostauth(long id, String username, Date authdate) {
		this.id = id;
		this.username = username;
		this.authdate = authdate;
	}

	/** Konstruktor lengkap untuk membuat baris {@code radpostauth} baru dengan seluruh kolom. */
	public Radpostauth(long id, String username, String pass, String reply,
			String calledstationid, String callingstationid, Date authdate) {
		this.id = id;
		this.username = username;
		this.pass = pass;
		this.reply = reply;
		this.calledstationid = calledstationid;
		this.callingstationid = callingstationid;
		this.authdate = authdate;
	}

	/** @return ID baris (primary key). */
	@Id
	@Column(name = "id", unique = true, nullable = false)
	public long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(long id) {
		this.id = id;
	}

	/** @return nama pengguna yang mencoba otentikasi. */
	@Column(name = "username", nullable = false, length = 253)
	public String getUsername() {
		return this.username;
	}

	/** @param username nama pengguna yang akan diset. */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return password yang dikirim pengguna saat percobaan ini — lihat catatan keamanan pada
	 *         javadoc kelas (berpotensi plaintext, tercatat walau percobaan gagal).
	 */
	@Column(name = "pass", length = 128)
	public String getPass() {
		return this.pass;
	}

	/** @param pass password percobaan yang akan diset. */
	public void setPass(String pass) {
		this.pass = pass;
	}

	/** @return hasil percobaan otentikasi (mis. Access-Accept/Access-Reject). */
	@Column(name = "reply", length = 32)
	public String getReply() {
		return this.reply;
	}

	/** @param reply hasil percobaan yang akan diset. */
	public void setReply(String reply) {
		this.reply = reply;
	}

	/** @return ID nomor yang dipanggil saat percobaan ini. */
	@Column(name = "calledstationid", length = 50)
	public String getCalledstationid() {
		return this.calledstationid;
	}

	/** @param calledstationid ID nomor yang dipanggil, akan diset. */
	public void setCalledstationid(String calledstationid) {
		this.calledstationid = calledstationid;
	}

	/** @return ID nomor pemanggil (umumnya MAC address perangkat pengguna) saat percobaan ini. */
	@Column(name = "callingstationid", length = 50)
	public String getCallingstationid() {
		return this.callingstationid;
	}

	/** @param callingstationid ID nomor pemanggil yang akan diset. */
	public void setCallingstationid(String callingstationid) {
		this.callingstationid = callingstationid;
	}

	/** @return waktu percobaan otentikasi dilakukan. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "authdate", nullable = false, length = 35)
	public Date getAuthdate() {
		return this.authdate;
	}

	/** @param authdate waktu percobaan yang akan diset. */
	public void setAuthdate(Date authdate) {
		this.authdate = authdate;
	}

}
