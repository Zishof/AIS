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

/**
 * Entity JPA/Hibernate untuk data master gudang pada modul SIRS (Sistem Informasi Rumah Sakit),
 * dipetakan ke tabel {@code sirs.gudang}. Mewakili lokasi fisik penyimpanan (mis. gudang farmasi,
 * gudang logistik umum) yang menjadi titik referensi transaksi persediaan/inventaris di modul
 * lain (pengadaan, farmasi) — entity ini sendiri hanya menyimpan identitas dan hierarki gudang,
 * tidak memuat logika stok/transaksi apa pun.
 *
 * <h2>Hierarki gudang</h2>
 * <p>
 * Relasi self-referencing {@link #getGudangInduk()} memungkinkan satu gudang menjadi "anak" dari
 * gudang lain (mis. gudang cabang di bawah gudang pusat). Tidak ditemukan proteksi terhadap
 * siklus (mis. gudang A menjadi induk gudang B yang menjadi induk gudang A) pada level entity
 * ini — validasi semacam itu, bila ada, berada di lapisan action/UI yang memakainya, bukan di
 * kelas ini.</p>
 *
 * <p>Class ini adalah entity hbm2java standar: getter relasi memakai {@code check(...)} warisan
 * {@link GeneralValueObject} untuk resolusi proxy Hibernate lazy yang aman lintas session. Field
 * audit {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS
 * (diisi {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola
 * audit aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama
 * sekali (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see Bagian unit kerja organisasi/akunting — sumbu klasifikasi organisasi yang berbeda dari gudang
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "gudang")
public class Gudang extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.gudang}. Lihat {@link #getId()}. */
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
	 * Representasi string dari gudang ini, dipakai komponen ZK (combobox/label) yang memanggil
	 * {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} gudang apa adanya (tanpa null-check eksplisit — akan mengembalikan
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

	/** Kode unik gudang. Lihat {@link #getKode()}. */
	private String kode;
	/** Nama gudang. Lihat {@link #getNama()}. */
	private String nama;
	/** Alamat fisik gudang. Lihat {@link #getAlamat()}. */
	private String alamat;
	/** Keterangan bebas tentang gudang ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Gudang induk dalam hierarki gudang. Lihat {@link #getGudangInduk()}. */
	private Gudang gudangInduk;
	/** Flag aktif/nonaktif gudang, default {@code true} bila kosong. Lihat {@link #getAktif()}. */
	private Boolean aktif;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public Gudang() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID gudang, atau {@code null} untuk instance yang belum tersimpan
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
	 * Mengembalikan nama gudang ini.
	 *
	 * @return nama gudang
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama gudang ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
	 *
	 * @param nama nama baru, maksimal 50 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang gudang ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang gudang ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode unik gudang ini.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode unik gudang ini. Kolom wajib diisi dan bertanda {@code unique} di basis
	 * data — dua gudang tidak boleh berbagi kode yang sama, meski getter/setter ini sendiri
	 * tidak melakukan pengecekan duplikasi (constraint ditegakkan basis data saat simpan).
	 *
	 * @return kode gudang
	 */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan alamat fisik gudang ini.
	 *
	 * @param alamat alamat baru, boleh {@code null}
	 */
	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	/**
	 * Mengembalikan alamat fisik gudang ini. Field ini TIDAK dianotasi {@code @Column}
	 * eksplisit — dipetakan Hibernate secara implisit berdasarkan konvensi nama getter.
	 *
	 * @return alamat gudang, atau {@code null} bila tidak diisi
	 */
	public String getAlamat() {
		return alamat;
	}

	/**
	 * Menetapkan gudang induk dalam hierarki gudang. Tidak ada proteksi siklus pada level
	 * entity ini — lihat catatan pada javadoc kelas.
	 *
	 * @param gudangInduk gudang induk baru, boleh {@code null} untuk gudang tingkat teratas
	 */
	public void setGudangInduk(Gudang gudangInduk) {
		this.gudangInduk = gudangInduk;
	}

	/**
	 * Mengembalikan gudang induk dalam hierarki gudang, melewati resolusi proxy lazy
	 * {@code check(...)} milik {@link GeneralValueObject} agar aman dipanggil meski entity ini
	 * sudah lepas dari session Hibernate yang memuatnya.
	 *
	 * @return gudang induk, atau {@code null} bila gudang ini berada di tingkat teratas hierarki
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "gudang_induk", nullable = true)
	public Gudang getGudangInduk() {
		gudangInduk = check(gudangInduk);
		return gudangInduk;
	}

	/**
	 * Mengembalikan flag aktif/nonaktif gudang ini, di-default {@code true} bila kolom
	 * {@code null} (gudang dianggap aktif secara default). Field ini TIDAK dianotasi
	 * {@code @Column} eksplisit — dipetakan Hibernate secara implisit berdasarkan konvensi nama
	 * getter.
	 *
	 * @return {@code true} bila gudang aktif atau belum pernah diisi, {@code false} bila
	 *         eksplisit dinonaktifkan
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Menetapkan flag aktif/nonaktif gudang ini.
	 *
	 * @param aktif nilai baru, boleh {@code null} (akan diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()})
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
