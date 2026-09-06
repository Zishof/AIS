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
 * Entity Hibernate untuk tabel {@code public.jabatan_fungsional_dosen} &mdash; master
 * <b>jabatan fungsional akademik dosen</b> internal (mis. "Asisten Ahli", "Lektor",
 * "Lektor Kepala", "Guru Besar"). Direferensikan dari
 * {@code Dosen#getJabatanFungsionalDosen()} (kolom {@code jabatan_fungsional_dosen}).
 *
 * <p><b>Bukan pengganti kodifikasi EPSBED/PDDikti.</b> {@code Dosen} juga memegang relasi
 * terpisah ke {@code ais.database.model.epsbed.EpsbedJabatanAkademik}
 * ({@code Dosen#getJabatanAkademik()}, kolom {@code epsbed_jabatan_akademik}) untuk
 * keperluan pelaporan resmi; kedua relasi itu independen dan dipakai bersama pada layar
 * biodata maupun berkas pelaporan &mdash; lihat javadoc kelas {@code Dosen} untuk
 * perbandingannya. Field {@link #getFeeder()} pada kelas ini adalah jembatan lain: kode
 * pemetaan jabatan fungsional master internal ke kodifikasi Feeder PDDikti, terpisah dari
 * relasi objek ke {@code EpsbedJabatanAkademik}.
 *
 * <p>Dikelola lewat CRUD master data sederhana. Diturunkan dari {@link GeneralValueObject};
 * {@code id}, {@code oleh}, {@code olehId}, dan {@link #tanggal_dirubah} dideklarasikan
 * ulang di sini karena kelas induk adalah POJO abstrak biasa (bukan
 * {@code @Entity}/{@code @MappedSuperclass}) &mdash; keharusan teknis, bukan duplikasi
 * keliru. Seperti master lain di paket ini, {@link #getAktif()} bersifat satu arah: nilai
 * {@code null} pada kolom dibaca sebagai {@code true} oleh getter, tetapi setter tidak
 * menormalkan {@code null} menjadi {@code true}.
 *
 * @see Dosen
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jabatan_fungsional_dosen")

public class JabatanFungsionalDosen extends GeneralValueObject {

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

	private String feeder;

	/** Konstruktor kosong yang diperlukan Hibernate. */
	public JabatanFungsionalDosen() {
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

	/** @return nama jabatan fungsional dosen (wajib diisi), sudah di-trim. */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama jabatan fungsional dosen. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan tambahan untuk jabatan ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan tambahan untuk jabatan ini. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return kode pemetaan jabatan ini ke kodifikasi Feeder PDDikti, sudah di-trim;
	 *     {@code null} bila belum diisi atau hanya berisi spasi.
	 */
	public String getFeeder() {
		return feeder == null || feeder.trim().isEmpty() ? null : feeder.trim();
	}

	/** @param feeder kode pemetaan jabatan ini ke kodifikasi Feeder PDDikti. */
	public void setFeeder(String feeder) {
		this.feeder = feeder;
	}

	/**
	 * @return {@code true} bila jabatan ini aktif dan boleh dipilih; {@code true} juga bila
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

}
