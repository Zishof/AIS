package ais.database.model.radius;

// Generated Oct 20, 2011 1:32:56 PM by Hibernate Tools 3.2.1.GA

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entitas Hibernate untuk tabel {@code radcheck} pada basis data server RADIUS terpisah
 * ({@link ais.database.hibernate.RadiusHibernateUtil}), mengikuti skema BAKU
 * <a href="https://wiki.freeradius.org/config/Sql">FreeRADIUS</a>: satu baris menyatakan satu
 * pasangan atribut-cek (mis. {@code attribute="Password"}, {@code op="=="},
 * {@code value="rahasia123"}) yang harus dipenuhi agar percobaan autentikasi ({@code username})
 * dianggap berhasil oleh {@code radiusd}. Baris di tabel ini dibaca langsung oleh proses
 * FreeRADIUS (di luar aplikasi Java ini) saat memvalidasi permintaan otentikasi Wi-Fi kampus;
 * entitas ini juga dipetakan di sisi AIS agar dapat ditulis/disinkron dari sini (lihat
 * {@link ais.action.master.helper.util.RadiusProcessor}).
 *
 * <p>
 * <b>Keamanan — kolom {@link #getValue()} menyimpan PASSWORD PLAINTEXT.</b> Untuk atribut
 * {@code "Password"} (konvensi PAP FreeRADIUS yang dipakai di AIS, lihat
 * {@link ais.action.master.helper.util.RadiusProcessor#doProcess()}), kolom {@code value} berisi
 * password pengguna dalam bentuk teks polos (hasil dekripsi dari password AIS yang tersimpan
 * terenkripsi), BUKAN hash — ini konvensi umum {@code Cleartext-Password} pada skema FreeRADIUS
 * standar, namun berarti siapa pun yang punya akses baca ke basis data RADIUS (atau ke entitas ini
 * lewat kode Java) dapat membaca password pengguna secara langsung. Lihat catatan keamanan lengkap
 * pada {@link ais.action.master.helper.util.RadiusProcessor} (penulis/pemakai utama kelas ini)
 * untuk detail mitigasi dan kenapa migrasi ke CHAP/MS-CHAP tidak menghilangkan risiko ini.
 * </p>
 *
 * <p>Berbeda dari kebanyakan entitas {@code ais.database.model} lain, kelas ini TIDAK meng-extend
 * {@link ais.database.model.GeneralValueObject} dan TIDAK memakai {@code @Audited}/Envers atau
 * kolom audit {@code oleh}/{@code tanggal_dirubah} — karena skema tabelnya mengikuti standar
 * FreeRADIUS apa adanya (dihasilkan otomatis oleh hbm2java dari skema tersebut), bukan konvensi
 * internal AIS.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(name = "radcheck", schema = "public")
public class Radcheck implements java.io.Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7458800787983769668L;
	/** ID baris (primary key numerik, auto-increment). */
	private int id;
	/** Nama pengguna (identitas login) yang dicek RADIUS, mis. NIM/username AIS atau {@code kodeUniq} berawalan {@code _ortu_} untuk akun orang tua. */
	private String username;
	/** Nama atribut RADIUS yang dicek, mis. {@code "Password"} (lihat catatan keamanan pada javadoc kelas). */
	private String attribute;
	/** Operator perbandingan RADIUS, mis. {@code "=="} (kecocokan persis, dipakai AIS untuk PAP). */
	private String op;
	/** Nilai yang harus dipenuhi/dicocokkan untuk atribut ini — untuk atribut {@code "Password"} berisi PASSWORD PLAINTEXT, lihat javadoc kelas. */
	private String value;

	/** Kunci unik buatan AIS (bukan standar FreeRADIUS) untuk mengaitkan baris ini balik ke entitas sumber (mis. {@code Tbmuser}/{@code Mahasiswa}) dan mencegah duplikasi saat sinkronisasi ulang. */
	private String kodeUniq;

	/** Representasi ringkas untuk log/debug: {@code username -- value} (perhatian: ini dapat membocorkan password plaintext ke log bila dipanggil tanpa hati-hati). */
	public String toString() {
		return username + " -- " + value;
	}

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public Radcheck() {
	}

	/** Konstruktor lengkap untuk membuat baris {@code radcheck} baru dengan seluruh kolom inti (tanpa {@code kodeUniq}). */
	public Radcheck(int id, String username, String attribute, String op,
			String value) {
		this.id = id;
		this.username = username;
		this.attribute = attribute;
		this.op = op;
		this.value = value;
	}

	/** @return ID baris (primary key). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public int getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(int id) {
		this.id = id;
	}

	/** @return nama pengguna (identitas login) yang dicek RADIUS. */
	@Column(name = "username", nullable = false, length = 64)
	public String getUsername() {
		return this.username;
	}

	/** @param username nama pengguna (identitas login) yang akan diset. */
	public void setUsername(String username) {
		this.username = username;
	}

	/** @return nama atribut RADIUS yang dicek (mis. {@code "Password"}). */
	@Column(name = "attribute", nullable = false, length = 64)
	public String getAttribute() {
		return this.attribute;
	}

	/** @param attribute nama atribut RADIUS yang akan diset. */
	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	/** @return operator perbandingan RADIUS (mis. {@code "=="}). */
	@Column(name = "op", nullable = false, length = 2)
	public String getOp() {
		return this.op;
	}

	/** @param op operator perbandingan RADIUS yang akan diset. */
	public void setOp(String op) {
		this.op = op;
	}

	/**
	 * @return nilai atribut yang dicek RADIUS — untuk atribut {@code "Password"}, ini adalah
	 *         PASSWORD PENGGUNA DALAM BENTUK PLAINTEXT (lihat catatan keamanan pada javadoc kelas).
	 */
	@Column(name = "value", nullable = false, length = 253)
	public String getValue() {
		return this.value;
	}

	/**
	 * @param value nilai atribut yang akan diset — untuk atribut {@code "Password"}, nilai ini
	 *              disimpan APA ADANYA sebagai plaintext (tidak di-hash oleh kelas ini).
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/** @return kunci unik buatan AIS yang mengaitkan baris ini ke entitas sumber (bukan kolom standar FreeRADIUS). */
	@Column(name = "kode_uniq", unique = true, nullable = false, length = 253)
	public String getKodeUniq() {
		return kodeUniq;
	}

	/** @param kodeUniq kunci unik buatan AIS yang akan diset. */
	public void setKodeUniq(String kodeUniq) {
		this.kodeUniq = kodeUniq;
	}

}
