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

import org.hibernate.envers.Audited;

/**
 * Pengaturan tampilan slideshow screensaver Layar Pelanggan, satu baris per toko
 * (menu Konfigurasi tab baru). Bukan tabel file/blob (tidak ada kolom gambar di sini
 * -- gambar sendiri ada di {@link ais.database.model.file.LayarPelangganSlide},
 * DB streaming terpisah) jadi entity ini aman di DB utama biasa.
 *
 * <p>{@code aktif=false} = screensaver dimatikan sama sekali walau ada gambar
 * ter-upload (default OFF -- {@code getAktif()} fallback ke false, konsisten dgn
 * gerbang opt-in default OFF utk fitur baru sesuai preferensi user).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "layar_pelanggan_screensaver_config")
public class LayarPelangganScreensaverConfig {

	public static final String MODE_FULLSCREEN = "FULLSCREEN";
	public static final String MODE_SETENGAH = "SETENGAH";

	public static final String ANIMASI_FADE = "FADE";
	public static final String ANIMASI_SLIDE = "SLIDE";
	public static final String ANIMASI_ZOOM = "ZOOM";
	public static final String ANIMASI_KEN_BURNS = "KEN_BURNS";

	private Long id;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	private Long tokoId;
	private Boolean aktif;
	private String modeTampilan;
	private String animasi;
	private Integer durasiDetik;
	private Integer idleDetik;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	@Column(name = "oleh_id", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	@Column(name = "toko_id", nullable = false, unique = true)
	public Long getTokoId() {
		return tokoId;
	}

	public void setTokoId(Long tokoId) {
		this.tokoId = tokoId;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? false : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	@Column(name = "mode_tampilan", nullable = true, length = 20)
	public String getModeTampilan() {
		return modeTampilan == null || modeTampilan.trim().isEmpty() ? MODE_FULLSCREEN : modeTampilan;
	}

	public void setModeTampilan(String modeTampilan) {
		this.modeTampilan = modeTampilan;
	}

	@Column(name = "animasi", nullable = true, length = 20)
	public String getAnimasi() {
		return animasi == null || animasi.trim().isEmpty() ? ANIMASI_FADE : animasi;
	}

	public void setAnimasi(String animasi) {
		this.animasi = animasi;
	}

	@Column(name = "durasi_detik", nullable = true)
	public Integer getDurasiDetik() {
		return durasiDetik == null || durasiDetik < 1 ? 6 : durasiDetik;
	}

	public void setDurasiDetik(Integer durasiDetik) {
		this.durasiDetik = durasiDetik;
	}

	@Column(name = "idle_detik", nullable = true)
	public Integer getIdleDetik() {
		return idleDetik == null || idleDetik < 1 ? 30 : idleDetik;
	}

	public void setIdleDetik(Integer idleDetik) {
		this.idleDetik = idleDetik;
	}
}
