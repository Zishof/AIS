package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.database.model.GeneralValueObject;

/**
 * Token otentikasi perangkat kasir untuk halaman POS LOKAL (Electron, dimuat dari {@code file://} --
 * tidak bisa memakai cookie {@code HttpSession} seperti jalur web biasa, lihat JavaDoc
 * {@code ais.action.servlet.PosApi}). Satu baris di sini MENGGANTIKAN peran cookie session untuk
 * konteks itu: satu token = satu sesi login dari satu instalasi aplikasi desktop.
 *
 * <p><b>{@link #getTokenHash()} menyimpan HASH SHA-256 dari token mentah, BUKAN token mentah itu
 * sendiri</b> -- persis seperti kata sandi tidak pernah disimpan mentah. Kalau baris tabel ini bocor
 * (mis. dump database), token asli tetap tidak bisa dipakai penyerang karena hash satu arah. Token
 * mentah HANYA pernah ada di memori server sesaat saat diterbitkan (lihat
 * {@code ais.action.servlet.api.PosDeviceAuthApi#terbitkanToken}) dan di penyimpanan lokal
 * terenkripsi milik aplikasi Electron (lihat {@code safeStorage} di {@code main.js} shell desktop) --
 * tidak pernah ditulis ke database dalam bentuk mentah.</p>
 *
 * <p>Sengaja TIDAK memakai {@code @Audited} (Envers) -- riwayat perubahan baris token tidak berguna
 * diaudit (bukan data bisnis), dan menghindarinya mengurangi kebutuhan skema tabel {@code _AUD}
 * tambahan hanya demi tabel teknis ini.</p>
 *
 * <p><b>WAJIB extends {@link GeneralValueObject}</b> (bukan POJO polos) -- {@code AuditListener}
 * global proyek ini ({@code ais.database.hibernate.AuditListener#onPostInsert}) meng-cast SETIAP
 * entity yang baru disimpan ke {@code java.io.Serializable} tanpa terkecuali (terpicu di event
 * post-insert Hibernate inti, TIDAK terkait {@code @Audited}/Envers) -- {@link GeneralValueObject}
 * adalah satu-satunya superclass di proyek ini yang sudah {@code implements Serializable}. Entity
 * BARU yang tidak menurunkannya akan gagal disimpan dengan {@code ClassCastException} (bug nyata
 * yang pernah terjadi persis di kelas ini sebelum diperbaiki).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "pos_device_token")
public class PosDeviceToken extends GeneralValueObject {

	private static final long serialVersionUID = 1L;

	private Long id;
	private String tokenHash;
	private String userId;
	private String labelPerangkat;
	private Date dibuatPada = new Date();
	private Date kedaluwarsaPada;
	private Date terakhirDipakaiPada;

	/** {@link GeneralValueObject} mewajibkan override ini -- tak ada perilaku khusus yang dibutuhkan token, jadi dibiarkan kosong. */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
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

	/** Hash SHA-256 (heksadesimal, 64 karakter) dari token mentah -- lihat JavaDoc kelas. */
	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	/** {@link ais.database.model.Tbmuser#getUserId()} pemilik token ini. */
	@Column(name = "user_id", nullable = false, length = 50)
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	/** Label bebas (opsional) supaya admin bisa mengenali perangkat mana yg memegang token ini nanti. */
	@Column(name = "label_perangkat", nullable = true, length = 255)
	public String getLabelPerangkat() {
		return labelPerangkat;
	}

	public void setLabelPerangkat(String labelPerangkat) {
		this.labelPerangkat = labelPerangkat;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "dibuat_pada", nullable = false)
	public Date getDibuatPada() {
		return dibuatPada;
	}

	public void setDibuatPada(Date dibuatPada) {
		this.dibuatPada = dibuatPada;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "kedaluwarsa_pada", nullable = false)
	public Date getKedaluwarsaPada() {
		return kedaluwarsaPada;
	}

	public void setKedaluwarsaPada(Date kedaluwarsaPada) {
		this.kedaluwarsaPada = kedaluwarsaPada;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "terakhir_dipakai_pada", nullable = true)
	public Date getTerakhirDipakaiPada() {
		return terakhirDipakaiPada;
	}

	public void setTerakhirDipakaiPada(Date terakhirDipakaiPada) {
		this.terakhirDipakaiPada = terakhirDipakaiPada;
	}
}
