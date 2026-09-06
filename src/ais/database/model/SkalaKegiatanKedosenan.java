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
 * Entity Hibernate untuk tabel {@code public.skala_kegiatan_kedosenan} &mdash; master
 * <b>tingkat/skala sebuah kegiatan Tridharma</b> (mis. "Jurusan", "Fakultas", "Nasional",
 * "Internasional"), pasangan dari {@link JabatanKegiatanKedosenan} di dasar rantai modul
 * "Kegiatan Dosen" (lihat javadoc {@link KelompokKegiatanKedosenan} untuk gambaran hierarki
 * penuh: {@link JenisKelompokKegiatanKedosenan} &rarr; {@link KelompokKegiatanKedosenan}
 * &rarr; {@code DetailKelompokKegiatanKedosenan} &rarr; {@link JabatanKegiatanKedosenan} /
 * kelas ini).
 *
 * <p><b>Perhatikan penamaan kolom lain yang memakai nama tabel ini sebagai FK.</b>
 * {@code KelompokKegiatanKedosenan#getJenisKelompokKegiatanKedosenan()} dipetakan ke kolom
 * {@code skala_kegiatan_kedosenan} meski secara semantik menunjuk
 * {@link JenisKelompokKegiatanKedosenan}, bukan kelas ini &mdash; sisa salin-tempel lama;
 * lihat javadoc method tersebut untuk rinciannya. Kelas ini sendiri tidak terpengaruh oleh
 * kuirk itu.
 *
 * <p>Direlasikan secara many-to-many dari sisi pemilik
 * {@code DetailKelompokKegiatanKedosenan#getSkalaKegiatanKedosenans()}, berpasangan dengan
 * {@link JabatanKegiatanKedosenan} sebagai dua sumbu matriks pilihan (tingkat x peran) pada
 * layar yang sama.
 *
 * <p>Dikelola lewat CRUD master data sederhana. Diturunkan dari {@link GeneralValueObject};
 * {@code id}, {@code oleh}, {@code olehId}, dan {@link #tanggal_dirubah} dideklarasikan
 * ulang di sini karena kelas induk adalah POJO abstrak biasa (bukan
 * {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis, bukan duplikasi
 * keliru.
 *
 * <p>Seperti master lain di modul ini, {@link #getAktif()} dan {@link #getNomorUrut()}
 * bersifat satu arah: nilai {@code null} pada kolom dibaca sebagai {@code true}/{@code 1}
 * oleh getter, tetapi setter tidak menormalkan {@code null} menjadi nilai default itu.
 * Berbeda dari {@link JabatanKegiatanKedosenan}, kolom {@link #getNama()} di sini
 * <b>tidak</b> diberi constraint unik pada level Hibernate.
 *
 * @see KelompokKegiatanKedosenan
 * @see JenisKelompokKegiatanKedosenan
 * @see JabatanKegiatanKedosenan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "skala_kegiatan_kedosenan")

public class SkalaKegiatanKedosenan extends GeneralValueObject {

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
	private Boolean aktif;
	private Integer nomorUrut;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public SkalaKegiatanKedosenan() {
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

	/** @return nama tingkat/skala kegiatan (wajib diisi), sudah di-trim. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama tingkat/skala kegiatan. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan untuk tingkat ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk tingkat ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila tingkat ini aktif dan boleh dipilih; {@code true} juga bila
	 *     kolom masih {@code null} (belum pernah diisi) &mdash; lihat catatan kelas soal
	 *     bendera satu arah.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif baru; tidak dinormalisasi, boleh {@code null}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return nomor urut untuk pengurutan tampilan/kombinasi matriks tingkat x peran pada
	 *     layar pemilihan; {@code 1} bila kolom masih {@code null} (belum pernah diisi).
	 */
	public Integer getNomorUrut() {
		return nomorUrut == null ? 1 : nomorUrut;
	}

	/** @param nomorUrut nomor urut baru; tidak dinormalisasi, boleh {@code null}. */
	public void setNomorUrut(Integer nomorUrut) {
		this.nomorUrut = nomorUrut;
	}

}
