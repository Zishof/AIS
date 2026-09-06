package ais.database.model;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

/**
 * Entity Hibernate untuk tabel {@code public.user_devices} &mdash; baris pasangan
 * <b>kode pairing perangkat &harr; konfigurasi institusi</b> untuk alur bootstrap aplikasi
 * mobile <i>sebelum</i> pengguna login (mengambil branding: nama/logo/banner perguruan
 * tinggi, dan tautan konfigurasi server). Satu-satunya pemakai di repo ini adalah
 * {@code ais.action.servlet.api.ApiUtil}, lewat dua endpoint publik yang didaftarkan di
 * {@code ApiRouteRegistry.registerAuthAndGeneral(...)} (grup rute yang memang dirancang
 * dapat diakses sebelum autentikasi):
 * <ul>
 *   <li>{@code ApiUtil.code(...)} (rute {@code "code"}): mencari baris berdasarkan
 *   {@link #getUsername()}; bila belum ada, membuat baris baru dengan {@link #getImei()}
 *   diisi kode acak 18 karakter ({@code Common.getGeneratedBarCode(18)}). Lalu
 *   <b>menimpa</b> {@link #getUsername()} dan {@link #getPassword()} sesuai isi request,
 *   dan menyimpan atribut branding tambahan (nama/logo/banner PT, dsb.) lewat
 *   {@code put(...)}/{@code retreive(...)} warisan {@link GeneralValueObject}.</li>
 *   <li>{@code ApiUtil.ambilCode(...)} (rute {@code "ambilCode"}): mencari baris
 *   berdasarkan {@link #getImei()} (parameter request bernama {@code "code"}), lalu
 *   mengembalikan {@link #getUsername()}, {@link #getPassword()}, dan seluruh atribut
 *   branding yang tersimpan.</li>
 * </ul>
 *
 * <p><b>Nama field menyesatkan &mdash; bukan kredensial maupun IMEI perangkat sungguhan.</b>
 * Berdasarkan pemakaian di atas: {@link #getImei()} sebenarnya adalah <i>kode pairing acak</i>
 * yang dibangkitkan server (bukan IMEI perangkat yang dibaca dari device), dan
 * {@link #getPassword()} sebenarnya menyimpan sebuah <i>tautan/link</i> konfigurasi
 * (parameter request bernama {@code "link"}), bukan kata sandi. Nama kolom ini berisiko
 * menyesatkan pemelihara di masa depan untuk memperlakukannya sebagai kredensial nyata
 * (mis. mengira perlu di-hash) padahal isinya adalah data konfigurasi non-rahasia.
 *
 * <p><b>Catatan keamanan.</b> {@code ApiUtil.code(...)} <b>tidak memverifikasi kepemilikan</b>
 * {@link #getUsername()} sebelum menimpa {@link #getPassword()} (tautan) baris yang sudah
 * ada: siapa pun yang mengetahui/menebak sebuah {@code username} dapat memanggil rute
 * {@code "code"} dan mengganti tautan konfigurasi yang tersimpan untuknya, sehingga
 * perangkat lain yang kelak memanggil {@code "ambilCode"} dengan kode pairing (imei) milik
 * baris tersebut akan menerima tautan yang sudah diganti. Karena rute ini memang sengaja
 * pre-auth (dipakai sebelum login) dan isinya bukan kredensial rahasia, dampaknya terbatas
 * pada potensi pengalihan konfigurasi/branding klien, bukan pembocoran data pribadi atau
 * pengambilalihan akun secara langsung.
 *
 * <p>Diakses lewat sesi Hibernate native (bukan DAO generik). Diturunkan dari
 * {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "user_devices")
public class UserDevices extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Nama pelaku (audit shadow, lihat {@link GeneralValueObject}) yang membuat/mengubah baris ini. */
	private String oleh;
	/** Id pelaku (audit shadow) yang membuat/mengubah baris ini. */
	private String olehId;

	/** @return id pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pelaku. Nilai kosong/blank diabaikan (fail-safe agar audit shadow tidak
	 * tertimpa string kosong secara tidak sengaja) &mdash; bukan validasi keamanan.
	 *
	 * @param olehId id pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pelaku. Nilai kosong/blank diabaikan, sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pelaku; diabaikan jika {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pelaku terakhir yang mengubah baris ini, atau {@code null} bila belum pernah diisi. */
	public String getOleh() {
		return oleh;
	}

	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengubah stempel waktu perubahan terakhir secara manual. Nilai default sudah
	 * di-set ke waktu saat ini pada deklarasi field dan di-refresh otomatis oleh
	 * {@link #onUpdate()} pada setiap update; setter ini jarang perlu dipanggil langsung.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas berupa {@code id-} (tanpa field lain), dipakai untuk debugging/log. */
	public String toString() {
		return id + "-";
	}

	private String imei;
	private String username;
	private String password;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public UserDevices() {
	}

	/** @return id baris (primary key, auto-generated identity di database). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id id baris; kolom tidak insertable sehingga nilai ini diabaikan saat insert. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode pairing baris ini. Meski nama kolomnya "imei", isinya adalah <b>kode
	 *     acak yang dibangkitkan server</b> ({@code Common.getGeneratedBarCode(18)}), bukan
	 *     IMEI perangkat sungguhan &mdash; lihat catatan kelas.
	 */
	public String getImei() {
		return imei;
	}

	/** @param imei kode pairing baris ini. */
	public void setImei(String imei) {
		this.imei = imei;
	}

	/**
	 * @return username yang dipasangkan dengan kode pairing ini. Nilai ini adalah teks
	 *     bebas dari parameter request {@code "username"}, <b>tidak diverifikasi</b>
	 *     terhadap akun {@link Tbmuser} nyata maupun kepemilikannya &mdash; lihat catatan
	 *     keamanan pada javadoc kelas.
	 */
	public String getUsername() {
		return username;
	}

	/** @param username username yang dipasangkan dengan kode pairing ini. */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return tautan/link konfigurasi yang tersimpan untuk baris ini. Meski nama kolomnya
	 *     "password", isinya <b>bukan kata sandi</b> &mdash; nilainya diisi dari parameter
	 *     request {@code "link"} pada {@code ApiUtil.code(...)} dan dibaca kembali sebagai
	 *     {@code "link"} pada {@code ApiUtil.ambilCode(...)}; lihat catatan kelas.
	 */
	public String getPassword() {
		return password;
	}

	/** @param password tautan/link konfigurasi yang dipasangkan dengan kode pairing ini. */
	public void setPassword(String password) {
		this.password = password;
	}

}
