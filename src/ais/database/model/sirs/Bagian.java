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
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.Devisi;

/**
 * Entity JPA/Hibernate untuk data master bagian (unit kerja) pada modul SIRS (Sistem Informasi
 * Rumah Sakit), dipetakan ke tabel {@code sirs.bagian}. "Bagian" di sini adalah unit kerja
 * ORGANISASI/AKUNTING — terhubung ke {@link Devisi} (divisi induk) dan {@link Akun} (akun
 * akunting) — dipakai untuk keperluan kepegawaian dan pembiayaan (mis. pegawai bekerja di satu
 * bagian, transaksi medis/pembayaran/retur dikaitkan ke satu bagian, aset berlokasi di satu
 * bagian). Ini BUKAN unit klinis tempat pasien dilayani (itu peran {@link Poly}) maupun
 * klasifikasi administratif luas IGD/rawat jalan/rawat inap (itu peran {@link Instalasi}). Lihat
 * javadoc {@link Instalasi} untuk perbandingan lengkap ketiga sumbu klasifikasi ini berikut bukti
 * referensi FK yang membedakannya (Bagian dipakai {@code Pegawai}, {@code TransaksiMedis},
 * {@code Pembayaran}, {@code TransaksiRetur}, {@code Lokasi} — semuanya konteks HR/biaya/aset,
 * bukan alur klinis).
 *
 * <p>Class ini adalah entity hbm2java standar: getter relasi memakai {@code check(...)} warisan
 * {@link GeneralValueObject} untuk resolusi proxy Hibernate lazy yang aman lintas session. Field
 * audit {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS
 * (diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola
 * audit aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama
 * sekali (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see Devisi divisi induk bagian ini
 * @see Akun akun akunting terkait bagian ini
 * @see Instalasi klasifikasi administratif luas, sumbu berbeda dari bagian
 * @see Poly poliklinik klinis, sumbu berbeda dari bagian
 * @see ais.action.master.sirs.BagianAction layar CRUD data master bagian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "bagian")
public class Bagian extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.bagian}. Lihat {@link #getId()}. */
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
	 * Representasi string dari bagian ini, dipakai komponen ZK (combobox/label) yang memanggil
	 * {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} bagian apa adanya (tanpa null-check eksplisit — akan mengembalikan
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

	/** Nama bagian/unit kerja. Lihat {@link #getNama()}. */
	private String nama;
	/** Kode unik bagian. Lihat {@link #getKode()}. */
	private String kode;
	/** Keterangan bebas tentang bagian ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Divisi induk bagian ini. Lihat {@link #getDevisi()}. */
	private Devisi devisi;
	/** Akun akunting terkait bagian ini. Lihat {@link #getAkun()}. */
	private Akun akun;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public Bagian() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID bagian, atau {@code null} untuk instance yang belum tersimpan
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
	 * Mengembalikan nama bagian ini.
	 *
	 * @return nama bagian
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama bagian ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
	 *
	 * @param nama nama baru, maksimal 50 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang bagian ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang bagian ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode unik bagian ini.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode unik bagian ini. Kolom wajib diisi dan bertanda {@code unique} di basis
	 * data — dua bagian tidak boleh berbagi kode yang sama, meski getter/setter ini sendiri
	 * tidak melakukan pengecekan duplikasi (constraint ditegakkan basis data saat simpan; layar
	 * {@code BagianAction} juga melakukan pengecekan kode duplikat sendiri sebelum simpan lewat
	 * {@code checkKodeBagian()}).
	 *
	 * @return kode bagian
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan divisi induk bagian ini.
	 *
	 * @param devisi divisi baru, boleh {@code null}
	 */
	public void setDevisi(Devisi devisi) {
		this.devisi = devisi;
	}

	/**
	 * Mengembalikan divisi induk bagian ini, melewati resolusi proxy lazy {@code check(...)}
	 * milik {@link GeneralValueObject} agar aman dipanggil meski entity ini sudah lepas dari
	 * session Hibernate yang memuatnya.
	 *
	 * @return divisi induk, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "devisi", nullable = true)
	public Devisi getDevisi() {
		devisi = check(devisi);
		return devisi;
	}

	/**
	 * Menetapkan akun akunting terkait bagian ini.
	 *
	 * @param akun akun baru, boleh {@code null}
	 */
	public void setAkun(Akun akun) {
		this.akun = akun;
	}

	/**
	 * Mengembalikan akun akunting terkait bagian ini, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject}.
	 *
	 * @return akun akunting, atau {@code null} bila belum terpasang
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "akun_id", nullable = true)
	public Akun getAkun() {
		akun = check(akun);
		return akun;
	}

}
