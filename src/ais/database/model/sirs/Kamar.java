package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Ruang;

/**
 * Entity JPA/Hibernate untuk satu kamar rawat inap pada modul SIRS (Sistem Informasi Rumah
 * Sakit), dipetakan ke tabel {@code sirs.kamar}. Sebuah kamar berada di dalam satu {@link Ruang}
 * dan memiliki satu {@link KelasPerawatan} sebagai kelas defaultnya; di dalam kamar inilah
 * satu atau lebih {@link TempatTidur} (bed) ditempatkan.
 *
 * <p><b>Relasi kelas perawatan ganda:</b> {@link KelasPerawatan} muncul di DUA level yang
 * terpisah — {@link Kamar#getKelasPerawatan()} (kelas default kamar) dan
 * {@link TempatTidur#getKelasPerawatan()} (kelas per-bed, lihat javadoc kelas itu). Tidak ada
 * trigger atau constraint basis data yang memaksa keduanya konsisten; UI pemilihan bed
 * ({@code PindahTempatTidurRawatInapAction}) menyinkronkan combobox kelas mengikuti kamar yang
 * dipilih sebagai kenyamanan pengguna, tetapi data itu sendiri bisa saja berbeda bila diubah
 * lewat jalur lain (mis. layar CRUD {@link Kamar} dan CRUD {@link TempatTidur} diedit terpisah
 * tanpa saling menyinkronkan).</p>
 *
 * <p>Class ini adalah entity hbm2java standar: getter relasi memakai {@code check(...)} warisan
 * {@link GeneralValueObject} untuk resolusi proxy Hibernate lazy yang aman lintas session. Field
 * audit {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS
 * (diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola
 * audit aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama
 * sekali (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}); entity ini
 * juga tidak mengecualikan diri dari pola tersebut.</p>
 *
 * @see Ruang ruang perawatan yang menaungi kamar ini
 * @see KelasPerawatan kelas perawatan default kamar ini
 * @see TempatTidur tempat tidur (bed) yang menunjuk balik ke kamar ini lewat {@code kamar}
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "kamar")
public class Kamar extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.kamar}. Lihat {@link #getId()}. */
	private Long id;
	/** Identifier pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
	private String olehId;

	/**
	 * Mengembalikan identifier pengguna yang terakhir mengubah baris ini.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan identifier pengguna yang terakhir mengubah baris ini. Nilai kosong/blank
	 * sengaja DIABAIKAN (bukan di-set menjadi kosong) agar jejak audit sebelumnya tidak
	 * tertimpa oleh pemanggilan yang tidak membawa identitas pengguna.
	 *
	 * @param olehId ID pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
	private String oleh;

	/**
	 * Representasi string dari kamar ini, dipakai komponen ZK (combobox/label) yang memanggil
	 * {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} kamar apa adanya (tanpa null-check eksplisit — akan mengembalikan
	 *         {@code null} bila {@link #nama} belum diisi)
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Menetapkan nama pengguna yang terakhir mengubah baris ini. Nilai kosong/blank sengaja
	 * DIABAIKAN, simetris dengan {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang mendelegasikan ke
	 * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah}
	 * setiap kali baris ini di-{@code UPDATE}. Pola shadow-audit-field standar AIS.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan cap waktu perubahan terakhir secara manual. Dalam alur normal nilai ini
	 * dimutakhirkan otomatis oleh {@link #onUpdate()}; setter ini dipakai bila pemanggil perlu
	 * memaksa nilai tertentu (mis. saat memuat data hasil migrasi).
	 *
	 * @param tanggal_dirubah cap waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal/jam perubahan terakhir; default konstruksi objek adalah waktu objek
	 *         dibuat di memori, sebelum baris pernah tersimpan
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama/label kamar, mis. "Kamar 101". Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas tentang kamar ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Ruang perawatan yang menaungi kamar ini. Lihat {@link #getRuang()}. */
	private Ruang ruang;
	/** Kelas perawatan default kamar ini. Lihat {@link #getKelasPerawatan()}. */
	private KelasPerawatan kelasPerawatan;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public Kamar() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID kamar, atau {@code null} untuk instance yang belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan primary key baris ini. Kolom bertanda {@code insertable = false} pada
	 * pemetaan — nilai sesungguhnya berasal dari {@code IDENTITY} basis data saat
	 * {@code INSERT}, sehingga setter ini biasanya hanya relevan untuk memuat ulang entity yang
	 * sudah memiliki ID.
	 *
	 * @param id ID baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama/label kamar ini.
	 *
	 * @return nama kamar, mis. "Kamar 101"
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama/label kamar ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
	 *
	 * @param nama nama baru, maksimal 50 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang kamar ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang kamar ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan ruang perawatan yang menaungi kamar ini.
	 *
	 * @param ruang ruang baru, boleh {@code null} di level Java meski kolom basis data
	 *              ({@code nullable = false} pada {@link #getRuang()}) mensyaratkan wajib diisi
	 */
	public void setRuang(Ruang ruang) {
		this.ruang = ruang;
	}

	/**
	 * Mengembalikan ruang perawatan yang menaungi kamar ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject} agar aman dipanggil meski entity ini
	 * sudah lepas dari session Hibernate yang memuatnya. Kolom wajib diisi di basis data
	 * ({@code nullable = false}).
	 *
	 * @return ruang perawatan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "ruang", nullable = false)
	public Ruang getRuang() {
		ruang = check(ruang);
		return ruang;
	}

	/**
	 * Menetapkan kelas perawatan default kamar ini. Lihat catatan pada javadoc kelas mengenai
	 * relasi kelas perawatan ganda antara {@link Kamar} dan {@link TempatTidur}.
	 *
	 * @param kelasPerawatan kelas perawatan baru
	 */
	public void setKelasPerawatan(KelasPerawatan kelasPerawatan) {
		this.kelasPerawatan = kelasPerawatan;
	}

	/**
	 * Mengembalikan kelas perawatan default kamar ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject}. Kolom wajib diisi di basis data
	 * ({@code nullable = false}).
	 *
	 * @return kelas perawatan
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kelas_perawatan", nullable = false)
	public KelasPerawatan getKelasPerawatan() {
		kelasPerawatan = check(kelasPerawatan);
		return kelasPerawatan;
	}

}
