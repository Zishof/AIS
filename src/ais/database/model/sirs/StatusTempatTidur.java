package ais.database.model.sirs;

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

import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk data master status administratif tempat tidur pada modul SIRS
 * (Sistem Informasi Rumah Sakit), dipetakan ke tabel {@code sirs.status_tempat_tidur}. Baris
 * pada tabel ini adalah daftar nilai bebas yang dikelola sebagai data master (mis. "Tersedia",
 * "Sedang Dibersihkan", "Rusak/Perbaikan") — TIDAK ada enum Java yang membatasi nilai apa saja
 * yang boleh ada; siapa pun dengan hak akses CRUD dapat menambah status baru.
 *
 * <p><b>WASPADA — bukan indikator okupansi:</b> status di sini murni administratif/fisik dan
 * TERPISAH SEPENUHNYA dari flag okupansi {@link TempatTidur#getTerisi()}. Sebuah bed berstatus
 * "Tersedia" di sini bisa saja tetap {@code terisi = true} (sedang ditempati pasien), dan
 * sebaliknya. Baris kode di {@link TempatTidur#updateTerisi()} yang semula berniat
 * memutakhirkan status ini secara otomatis saat bed terisi
 * ({@code setStatusTempatTidur(ConstantValues.TIDAK_TERSEDIA)}) sengaja dinonaktifkan/dikomentari
 * — lihat javadoc method tersebut untuk rincian bug okupansi terkait ({@code task_d82932ef}).
 * Jangan menganggap nilai {@link TempatTidur#getStatusTempatTidur()} sebagai pengganti
 * {@link TempatTidur#getTerisi()} atau sebaliknya.</p>
 *
 * <p>Class ini adalah entity hbm2java standar tanpa relasi {@code @ManyToOne}/{@code @OneToMany}
 * apa pun — hanya {@link #nama} dan {@link #keterangan} selain field generik warisan
 * {@link GeneralValueObject}. Field audit {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah}
 * adalah shadow field standar AIS (diisi {@code AuditTimestampInterceptor} lewat
 * {@link #onUpdate()}) — KEHARUSAN TEKNIS pola audit aplikasi, bukan bug. Modul {@code sirs}
 * tidak memiliki sumbu tenant/satuan-kerja sama sekali (dikonfirmasi berulang kali pada audit
 * sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see TempatTidur tempat tidur yang menunjuk balik ke status ini lewat {@code statusTempatTidur}
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "status_tempat_tidur")
public class StatusTempatTidur extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.status_tempat_tidur}. Lihat {@link #getId()}. */
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
	 * Representasi string dari status tempat tidur ini, dipakai komponen ZK (combobox/label)
	 * yang memanggil {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} status apa adanya (tanpa null-check eksplisit — akan mengembalikan
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

	/** Nama status, mis. "Tersedia", "Sedang Dibersihkan", "Rusak/Perbaikan". Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas tentang status ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public StatusTempatTidur() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID status, atau {@code null} untuk instance yang belum tersimpan
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
	 * Mengembalikan nama status tempat tidur ini.
	 *
	 * @return nama status, mis. "Tersedia"
	 */
	@Column(name = "nama", nullable = false, length = 250)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama status tempat tidur ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun
	 * maupun pengecekan duplikasi.
	 *
	 * @param nama nama baru, maksimal 250 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang status ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang status ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
