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

	/** Slideshow memenuhi seluruh layar pelanggan. Nilai bawaan {@link #getModeTampilan()} bila kosong. */
	public static final String MODE_FULLSCREEN = "FULLSCREEN";
	/** Slideshow hanya mengisi separuh layar pelanggan (sisanya dipakai tampilan transaksi berjalan). */
	public static final String MODE_SETENGAH = "SETENGAH";

	/** Transisi memudar antar slide. Nilai bawaan {@link #getAnimasi()} bila kosong. */
	public static final String ANIMASI_FADE = "FADE";
	/** Transisi geser antar slide. */
	public static final String ANIMASI_SLIDE = "SLIDE";
	/** Transisi perbesar/perkecil antar slide. */
	public static final String ANIMASI_ZOOM = "ZOOM";
	/** Transisi "Ken Burns" (perbesar + pan perlahan) antar slide. */
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

	/** @return id baris (identity, dibuat DB). Satu baris per toko (lihat {@link #getTokoId()}). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	/** @param id id baris; biasanya tidak diset manual, dibuat DB saat {@code save}. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return nama pengguna (audit shadow) yang terakhir menyimpan konfigurasi ini. */
	@Column(name = "oleh", nullable = true)
	public String getOleh() {
		return oleh;
	}

	/**
	 * @param oleh nama pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *             dipertahankan) agar kolom audit tidak pernah dikosongkan oleh caller yang lupa
	 *             mengisinya -- pola field audit shadow yang berulang di modul koperasi.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return id pengguna (audit shadow) yang terakhir menyimpan konfigurasi ini. */
	@Column(name = "oleh_id", nullable = true)
	public String getOlehId() {
		return olehId;
	}

	/**
	 * @param olehId id pengguna audit; nilai kosong/{@code null} diabaikan (nilai lama
	 *               dipertahankan), sama seperti {@link #setOleh(String)}.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: mendelegasikan pencatatan {@link #tanggal_dirubah} (dan field
	 * audit sejenis) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}, pola
	 * standar yang dipakai entity lain di paket ini. Dipanggil otomatis oleh provider JPA setiap
	 * {@code UPDATE}, tidak untuk dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** @return waktu baris terakhir diubah; diperbarui otomatis lewat {@link #onUpdate()}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_dirubah", nullable = true)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @param tanggal_dirubah waktu perubahan terakhir (biasanya tidak diset manual). */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return id {@link ais.database.model.inventory.Toko toko} pemilik konfigurasi ini. Kolom
	 *         {@code unique = true} menegakkan satu baris konfigurasi per toko di level DB.
	 */
	@Column(name = "toko_id", nullable = false, unique = true)
	public Long getTokoId() {
		return tokoId;
	}

	/** @param tokoId id toko pemilik konfigurasi; wajib diisi (unik per toko). */
	public void setTokoId(Long tokoId) {
		this.tokoId = tokoId;
	}

	/**
	 * @return apakah screensaver layar pelanggan dinyalakan untuk toko ini. Fallback ke
	 *         {@code false} bila kolom {@code null} -- konsisten dengan konvensi "opt-in default
	 *         OFF" untuk fitur baru di AIS: toko yang belum pernah menyentuh menu Konfigurasi tidak
	 *         tiba-tiba mendapat screensaver aktif hanya karena baris konfigurasinya sudah dibuat
	 *         (mis. oleh proses seed) dengan gambar slide yang kebetulan sudah ter-upload.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? false : aktif;
	}

	/** @param aktif nyala/matikan screensaver untuk toko ini. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return mode tampilan slideshow: {@link #MODE_FULLSCREEN} atau {@link #MODE_SETENGAH}.
	 *         Fallback ke {@link #MODE_FULLSCREEN} bila kolom kosong/{@code null}.
	 */
	@Column(name = "mode_tampilan", nullable = true, length = 20)
	public String getModeTampilan() {
		return modeTampilan == null || modeTampilan.trim().isEmpty() ? MODE_FULLSCREEN : modeTampilan;
	}

	/** @param modeTampilan mode tampilan baru; gunakan {@link #MODE_FULLSCREEN}/{@link #MODE_SETENGAH}. */
	public void setModeTampilan(String modeTampilan) {
		this.modeTampilan = modeTampilan;
	}

	/**
	 * @return jenis transisi antar slide: {@link #ANIMASI_FADE}, {@link #ANIMASI_SLIDE},
	 *         {@link #ANIMASI_ZOOM}, atau {@link #ANIMASI_KEN_BURNS}. Fallback ke
	 *         {@link #ANIMASI_FADE} bila kolom kosong/{@code null}.
	 */
	@Column(name = "animasi", nullable = true, length = 20)
	public String getAnimasi() {
		return animasi == null || animasi.trim().isEmpty() ? ANIMASI_FADE : animasi;
	}

	/** @param animasi jenis transisi baru; gunakan salah satu konstanta {@code ANIMASI_*}. */
	public void setAnimasi(String animasi) {
		this.animasi = animasi;
	}

	/**
	 * @return lama tampil tiap slide dalam detik. Fallback ke {@code 6} bila kolom {@code null}
	 *         atau kurang dari 1 (nilai negatif/nol tidak masuk akal untuk durasi slide dan bisa
	 *         membuat slideshow berputar terlalu cepat/tak terbaca di layar pelanggan).
	 */
	@Column(name = "durasi_detik", nullable = true)
	public Integer getDurasiDetik() {
		return durasiDetik == null || durasiDetik < 1 ? 6 : durasiDetik;
	}

	/** @param durasiDetik lama tampil tiap slide dalam detik. */
	public void setDurasiDetik(Integer durasiDetik) {
		this.durasiDetik = durasiDetik;
	}

	/**
	 * @return jeda tanpa transaksi (idle) sebelum screensaver mulai tampil, dalam detik. Fallback
	 *         ke {@code 30} bila kolom {@code null} atau kurang dari 1, dengan alasan yang sama
	 *         dengan {@link #getDurasiDetik()}.
	 */
	@Column(name = "idle_detik", nullable = true)
	public Integer getIdleDetik() {
		return idleDetik == null || idleDetik < 1 ? 30 : idleDetik;
	}

	/** @param idleDetik jeda idle sebelum screensaver mulai tampil, dalam detik. */
	public void setIdleDetik(Integer idleDetik) {
		this.idleDetik = idleDetik;
	}
}
