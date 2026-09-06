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
 * Entity Hibernate untuk tabel {@code public.jenis_kelompok_kegiatan_kedosenan} &mdash;
 * <b>payung terluar</b> pada rantai master modul "Kegiatan Dosen" (aktivitas Tridharma:
 * pendidikan, penelitian, pengabdian, penunjang). Satu baris di sini mewakili satu jenis
 * kelompok kegiatan tingkat teratas (mis. "Kelompok Utama", "Kelompok Penunjang").
 *
 * <p>Rantai master modul ini berjenjang empat tingkat, lihat javadoc
 * {@link KelompokKegiatanKedosenan} untuk gambaran lengkap:</p>
 *
 * <pre>
 *   JenisKelompokKegiatanKedosenan            (kelas ini &mdash; payung terluar)
 *        &darr;
 *   {@link KelompokKegiatanKedosenan}         (aspek kegiatan)
 *        &darr;
 *   DetailKelompokKegiatanKedosenan           (butir rincian di bawah aspek)
 *        &darr;
 *   {@link JabatanKegiatanKedosenan} / {@link SkalaKegiatanKedosenan}
 * </pre>
 *
 * <p>Direferensikan dari {@link KelompokKegiatanKedosenan#getJenisKelompokKegiatanKedosenan()}.
 * <b>Perhatikan</b>: relasi tersebut, meskipun secara semantik menunjuk ke kelas ini, dipetakan
 * lewat kolom bernama {@code skala_kegiatan_kedosenan} (sisa salin-tempel pemetaan
 * {@link SkalaKegiatanKedosenan}) &mdash; lihat javadoc method itu untuk rincian kuirk-nya.
 *
 * <p>Dikelola lewat CRUD master data sederhana di
 * {@code ais.action.master.JenisKelompokKegiatanKedosenanAction}. Diturunkan dari
 * {@link GeneralValueObject}; {@code id}, {@code oleh}, {@code olehId}, dan
 * {@link #tanggal_dirubah} dideklarasikan ulang di sini karena kelas induk adalah POJO
 * abstrak biasa (bukan {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis,
 * bukan duplikasi keliru, sama seperti kelas-kelas lain di modul ini.
 *
 * @see KelompokKegiatanKedosenan
 * @see JabatanKegiatanKedosenan
 * @see SkalaKegiatanKedosenan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_kelompok_kegiatan_kedosenan")

public class JenisKelompokKegiatanKedosenan extends GeneralValueObject {

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

	/** @return representasi ringkas berupa {@code id-nama}, dipakai untuk debugging/log. */
	public String toString() {
		return id + "-" + nama;
	}

	private String nama;
	private String keterangan;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public JenisKelompokKegiatanKedosenan() {
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

	/** @return nama jenis kelompok kegiatan kedosenan (wajib diisi), sudah di-trim. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jenis kelompok kegiatan kedosenan. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan untuk jenis kelompok ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk jenis kelompok ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
