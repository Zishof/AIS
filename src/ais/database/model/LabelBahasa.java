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
 * Entity <b>kamus terjemahan label UI</b> (tabel {@code public.label_bahasa}) — satu baris memetakan
 * satu {@link #getNama()} (kunci label, unik) ke terjemahannya dalam beberapa bahasa
 * ({@link #getIndonesia()}, {@link #getEnglish()}, {@link #getArab()}, {@link #getMandarin()}).
 * Dipakai untuk mendukung tampilan multi-bahasa pada UI (mis. portal publik/PMB) tanpa
 * menggandakan halaman per bahasa.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "label_bahasa")

public class LabelBahasa extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return id akun yang membuat/mengubah baris ini.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan id pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param olehId id akun pembuat/pengubah.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pembuat/pengubah. Nilai kosong/null diabaikan (tidak menimpa nilai lama) —
	 * write-guard satu-arah.
	 *
	 * @param oleh nama akun pembuat/pengubah.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama akun yang membuat/mengubah baris ini.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook Envers/JPA: memperbarui timestamp audit shadow {@link #tanggal_dirubah} setiap kali baris
	 * ini di-update.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah waktu perubahan terakhir (audit shadow field).
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return waktu perubahan terakhir baris ini, diisi otomatis oleh {@link #onUpdate()}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas "{id}-{nama}", dipakai untuk keperluan log/debug.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String indonesia;
	private String english;
	private String arab;
	private String keterangan;

	/**
	 * Konstruktor kosong (dipakai Hibernate untuk instansiasi via reflection).
	 */
	public LabelBahasa() {
	}

	/**
	 * @return id unik baris (surrogate key, auto-increment).
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * @param id id unik baris.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kunci label (unik), di-trim saat dibaca. Dipakai kode pemanggil untuk mencari baris
	 *         terjemahan yang sesuai.
	 */
	@Column(name = "nama", nullable = false, unique = true, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * @param nama kunci label (unik).
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * @return terjemahan label dalam Bahasa Indonesia.
	 */
	@Column(columnDefinition = "text")
	public String getIndonesia() {
		return indonesia;
	}

	/**
	 * @param indonesia terjemahan label dalam Bahasa Indonesia.
	 */
	public void setIndonesia(String indonesia) {
		this.indonesia = indonesia;
	}

	/**
	 * @return terjemahan label dalam Bahasa Inggris.
	 */
	@Column(columnDefinition = "text")
	public String getEnglish() {
		return english;
	}

	/**
	 * @param english terjemahan label dalam Bahasa Inggris.
	 */
	public void setEnglish(String english) {
		this.english = english;
	}

	/**
	 * @return keterangan/catatan bebas tentang baris label ini.
	 */
	public String getKeterangan() {
		return keterangan;
	}

	/**
	 * @param keterangan keterangan/catatan bebas.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return terjemahan label dalam Bahasa Arab; bila belum diisi, jatuh balik ke
	 *         {@link #getIndonesia()} (agar tampilan tidak kosong sebelum penerjemah mengisinya).
	 */
	@Column(columnDefinition = "text")
	public String getArab() {
		return arab == null || arab.isEmpty() ? indonesia : arab;
	}

	/**
	 * @param arab terjemahan label dalam Bahasa Arab.
	 */
	public void setArab(String arab) {
		this.arab = arab;
	}

	private String mandarin;

	/**
	 * @return terjemahan label dalam Bahasa Mandarin.
	 */
	@Column(columnDefinition = "text")
	public String getMandarin() {
		return mandarin;
	}

	/**
	 * @param mandarin terjemahan label dalam Bahasa Mandarin.
	 */
	public void setMandarin(String mandarin) {
		this.mandarin = mandarin;
	}

}
