package ais.database.model.asset;

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
 * <h2>JenisLokasi — Master "Jenis / Kategori Lokasi" (klasifikasi tempat fisik).</h2>
 *
 * <p>
 * Entity ini adalah <b>data master sederhana</b> yang berfungsi menggolongkan setiap
 * {@link Lokasi} ke dalam sebuah <i>tipe</i>. Contoh nilai yang lazim dipakai pada modul
 * e-Kantin / pergudangan: <b>Gudang</b>, <b>Outlet</b>, <b>Kasir</b>, <b>Entry Data</b>,
 * <b>Dapur/Produksi</b>, <b>Etalase</b>, dan seterusnya. Dengan memisahkan "jenis" ke tabel
 * tersendiri, pengguna (admin) dapat menambah/ubah/menghapus kategori tanpa perlu mengubah kode
 * program, dan setiap laporan maupun dasbor dapat mengelompokkan angka <b>per jenis lokasi</b>
 * secara konsisten (mis. total stok di semua lokasi berjenis "Gudang" versus "Outlet").
 * </p>
 *
 * <h3>Alasan desain (kenapa dipisah dari {@link Lokasi})</h3>
 * <p>
 * Semula sebuah lokasi hanya memuat nama dan alamat. Kebutuhan pergudangan menuntut pembedaan
 * peran tempat: sebuah "Gudang" diperlakukan berbeda dari sebuah "Outlet" (titik jual) ketika
 * menghitung persediaan, mutasi, dan nilai barang. Alih-alih menanam nilai tipe sebagai teks bebas
 * (yang rawan salah ketik: "gudang", "Gudang ", "GUDANG"), tipe diangkat menjadi entity referensi.
 * Manfaatnya: (1) konsistensi data — semua lokasi merujuk baris jenis yang sama; (2) fleksibilitas —
 * kategori baru cukup ditambah lewat menu, langsung tampil di seluruh filter/laporan; (3) atribut
 * presentasi ({@link #getWarna() warna} &amp; {@link #getIkon() ikon}) tersimpan sekali di sini lalu
 * dipakai ulang oleh banyak tampilan (badge berwarna pada tabel, kartu ringkas, hingga grafik),
 * mendukung UI/UX modern yang seragam dan mudah dirawat.
 * </p>
 *
 * <h3>Relasi</h3>
 * <p>
 * {@link Lokasi#getJenisLokasi()} menunjuk ke satu baris {@code JenisLokasi} (relasi
 * banyak-ke-satu). Relasi bersifat opsional ({@code nullable}) agar data lama yang belum
 * dikategorikan tetap valid dan tidak memutus kompatibilitas. Kolom {@link #getAktif() aktif}
 * memungkinkan menon-aktifkan kategori tanpa menghapusnya (soft-disable), sehingga referensi
 * historis pada lokasi lama tetap utuh sementara kategori tersebut tidak lagi muncul sebagai pilihan
 * pada form input baru.
 * </p>
 *
 * <h3>Pemetaan basis data &amp; audit</h3>
 * <p>
 * Dipetakan ke tabel <code>asset.jenis_lokasi</code> — satu skema dengan {@link Lokasi}
 * (<code>asset.lokasi</code>) sehingga mudah ditelusuri bersama. Primary key
 * <code>id</code> memakai strategi {@link javax.persistence.GenerationType#IDENTITY} (auto-increment
 * database) dan ditandai {@code insertable = false} agar nilainya sepenuhnya dikelola oleh basis data.
 * Karena diberi anotasi Hibernate Envers {@link org.hibernate.envers.Audited @Audited}, setiap
 * perubahan terekam pada tabel revisi audit — berguna untuk penelusuran siapa mengubah master ini.
 * Kolom jejak {@code oleh}/{@code olehId} diisi identitas pengguna terakhir, dan
 * {@code tanggal_dirubah} diperbarui otomatis melalui {@code @PreUpdate}
 * ({@code AuditTimestampInterceptor.ubah}). Setter {@link #setOleh(String)} dan
 * {@link #setOlehId(String)} sengaja mengabaikan nilai kosong/null agar jejak terakhir tidak
 * tertimpa nilai hampa saat objek disimpan dari alur yang tidak menyertakan identitas.
 * </p>
 *
 * <h3>Efisiensi memori &amp; catatan pemakaian</h3>
 * <p>
 * Objek ini sengaja ramping (hanya kolom skalar) sehingga hemat memori bila dimuat dalam jumlah
 * banyak; ia juga cocok di-cache di sisi aplikasi karena jarang berubah. Untuk menampilkan daftar
 * pilihan, muat sekali lalu gunakan ulang — hindari query berulang di dalam perulangan. Semua akses
 * data mengikuti aturan sesi framework: bila diambil lewat {@code currentSession()} jangan ditutup
 * manual; bila lewat {@code openSession()}/{@code currentNativeSession()} tutup di blok
 * {@code finally}. Kelas ini <i>tidak</i> menyimpan status sesi apa pun sehingga aman dipakai lintas
 * lapisan (servlet, ZK, maupun util laporan). Kompatibel dengan Java 1.7.
 * </p>
 *
 * @author AIS e-Kantin (modul pergudangan)
 * @see Lokasi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "asset", name = "jenis_lokasi")
public class JenisLokasi extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable}; tidak perlu
	 * diubah kecuali bentuk field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 7645120933115540021L;

	/** Primary key auto-generated (IDENTITY) tabel {@code asset.jenis_lokasi}. */
	private Long id;
	/** Nama pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String oleh;
	/** Id pengguna yang terakhir mengubah baris ini; diisi otomatis, lihat {@link #onUpdate()}. */
	private String olehId;

	/** Nama jenis lokasi (mis. "Gudang", "Outlet"); wajib diisi. */
	private String nama;
	/** Keterangan bebas, opsional. */
	private String keterangan;
	/** Status aktif; {@code null} diperlakukan sebagai aktif, lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Warna badge (hex, mis. {@code #16a34a}) untuk penanda visual tipe di tabel/kartu/grafik. */
	private String warna;
	/** Kelas ikon (mis. FontAwesome {@code fas fa-warehouse}) sebagai penanda visual tipe. */
	private String ikon;
	/** Urutan tampil pada daftar/pilihan (kecil = di atas). */
	private Integer urutan;

	/** Konstruktor default tanpa argumen, dipakai Hibernate untuk instansiasi via refleksi. */
	public JenisLokasi() {
	}

	/**
	 * @return identitas pembuat/pengubah terakhir (nama), untuk jejak audit ringan.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan identitas pengubah. Nilai kosong/null diabaikan agar jejak lama tidak tertimpa hampa.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/** @return id pengguna yang terakhir mengubah baris ini (audit), untuk jejak audit ringan. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id pengubah. Nilai kosong/null diabaikan (lihat {@link #setOleh(String)}).
	 *
	 * @param olehId id pengguna; diabaikan bila null/kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Hook siklus hidup JPA yang dipanggil Hibernate tepat sebelum setiap {@code UPDATE}.
	 * Mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * yang mengisi {@link #tanggal_dirubah}, {@link #oleh}, dan {@link #olehId} dengan waktu serta
	 * identitas pengguna aktif. Dipicu otomatis oleh Hibernate, tidak dipanggil manual.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Waktu perubahan terakhir baris ini; diperbarui otomatis oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi waktu perubahan terakhir. Field diinisialisasi ke waktu saat objek dibuat, lalu
	 * ditimpa ulang oleh {@link #onUpdate()} setiap kali baris di-{@code UPDATE}.
	 *
	 * @param tanggal_dirubah waktu perubahan.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini; tidak pernah {@code null}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi ringkas {@code id-nama} untuk log/combobox. */
	public String toString() {
		return id + "-" + nama;
	}

	/** @return primary key baris ini, atau {@code null} untuk instance baru yang belum disimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi primary key. Kolom database bersifat {@code insertable = false} (IDENTITY,
	 * auto-generate oleh database), sehingga pengisian manual tidak berpengaruh pada
	 * {@code INSERT}.
	 *
	 * @param id primary key.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama jenis lokasi (mis. "Gudang", "Outlet"), sudah di-{@code trim}; wajib diisi.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama jenis lokasi. Tidak melakukan trim di sisi setter — trimming terjadi hanya
	 * saat dibaca lewat {@link #getNama()}.
	 *
	 * @param nama nama jenis lokasi.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan bebas jenis lokasi ini, boleh {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas.
	 *
	 * @param keterangan teks keterangan, boleh {@code null}.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Status aktif. Bernilai {@code true} bila belum pernah diset (default aman) sehingga kategori
	 * lama otomatis dianggap aktif tanpa migrasi data.
	 *
	 * @return {@code true} bila jenis ini masih boleh dipilih pada input baru.
	 */
	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	/**
	 * Mengisi status aktif.
	 *
	 * @param aktif {@code true}/{@code false}; {@code null} diperlakukan sebagai aktif oleh
	 *              {@link #getAktif()}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * @return warna badge (hex) untuk penanda visual; {@code null} bila belum diatur (pemakai boleh
	 *         memakai warna default).
	 */
	@Column(name = "warna", nullable = true, length = 32)
	public String getWarna() {
		return warna == null || warna.trim().isEmpty() ? null : warna.trim();
	}

	/**
	 * Mengisi warna badge.
	 *
	 * @param warna kode warna hex, boleh {@code null}.
	 */
	public void setWarna(String warna) {
		this.warna = warna;
	}

	/**
	 * @return kelas ikon (mis. {@code fas fa-warehouse}); {@code null} bila belum diatur.
	 */
	@Column(name = "ikon", nullable = true, length = 64)
	public String getIkon() {
		return ikon == null || ikon.trim().isEmpty() ? null : ikon.trim();
	}

	/**
	 * Mengisi kelas ikon.
	 *
	 * @param ikon kelas ikon, boleh {@code null}.
	 */
	public void setIkon(String ikon) {
		this.ikon = ikon;
	}

	/**
	 * @return urutan tampil (kecil lebih dulu); default {@code 0} bila belum diatur.
	 */
	@Column(name = "urutan", nullable = true)
	public Integer getUrutan() {
		return urutan == null ? Integer.valueOf(0) : urutan;
	}

	/**
	 * Mengisi urutan tampil.
	 *
	 * @param urutan urutan tampil, boleh {@code null} (diperlakukan sebagai {@code 0}).
	 */
	public void setUrutan(Integer urutan) {
		this.urutan = urutan;
	}
}
