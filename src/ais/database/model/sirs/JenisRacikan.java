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
 * Master <b>jenis racikan</b>: klasifikasi bentuk sediaan obat racikan yang dipakai apotik, mis.
 * puyer, kapsul, sirup racikan, salep campur, atau tetes. Entitas ini murni katalog rujukan —
 * hanya {@link #getNama()} dan {@link #getKeterangan()} — dan dirujuk oleh
 * {@link Racikan#getJenisRacikan()}.
 *
 * <h3>Peran dalam alur peracikan</h3>
 * Jenis racikan bersifat <b>deskriptif, bukan menentukan harga</b>. Tidak ada kolom tarif, biaya
 * jasa racik, maupun faktor pengali apa pun di sini; harga sebuah {@link Racikan} sepenuhnya
 * dihitung dari harga jual komponen-komponennya ({@link RacikanDetail} &rarr;
 * {@link ItemMedis} &rarr; {@link HargaJualItem}). Jenis racikan karena itu hanya berfungsi
 * sebagai penggolong untuk tampilan, penyaringan, dan pelaporan — misalnya pada
 * {@code ais.action.master.sirs.RacikanAction} dan pemilih racikan
 * ({@code AmbilDataRacikanBanbox}, {@code AmbilDataRacikanBanyak}). Bila di kemudian hari
 * dibutuhkan biaya jasa peracikan yang berbeda per bentuk sediaan, biaya itu <b>tidak</b> dapat
 * ditempelkan di entitas ini tanpa perubahan skema; jalur yang tersedia sekarang adalah lewat
 * {@link JenisBiaya}/{@link Biaya} pada baris transaksi.
 *
 * <h3>Ketiadaan penjaga integritas</h3>
 * <ul>
 * <li>{@link #getNama()} wajib diisi di tingkat kolom ({@code nullable = false}) tetapi
 * <b>tidak berindeks unik</b>, dan tidak ada penjaga tabrakan nama di lapisan model. Dua baris
 * jenis racikan bernama sama dapat tersimpan, dan karena {@link #toString()} memakai nama sebagai
 * label tampil, keduanya akan tampak identik di daftar pilihan — operator tidak dapat membedakan
 * mana yang dipilih.</li>
 * <li>Tidak ada penanda aktif/nonaktif. Jenis racikan yang sudah tidak dipakai lagi tidak dapat
 * disembunyikan dari daftar pilihan tanpa dihapus, dan penghapusannya sendiri tidak dijaga
 * terhadap baris {@link Racikan} yang masih merujuknya (kolom {@code jenis_racikan} pada
 * {@code sirs.racikan} bersifat {@code nullable}).</li>
 * <li>Tidak ada kolom kode; identifikasi hanya lewat nama dan id.</li>
 * </ul>
 *
 * <h3>Pola arsitektur</h3>
 * <ul>
 * <li><b>Field audit bayangan</b> — {@link #getOleh()}, {@link #getOlehId()},
 * {@link #getTanggal_dirubah()}, dan {@link #onUpdate()} adalah infrastruktur audit
 * ({@code AuditTimestampInterceptor} + Hibernate Envers lewat {@link Audited}), keharusan teknis
 * dan bukan data domain.</li>
 * <li><b>Tanpa sumbu tenant</b> — sesuai keterbatasan seluruh modul {@code sirs}, katalog ini
 * bersifat global lintas unit.</li>
 * </ul>
 *
 * @see Racikan formula racikan yang digolongkan oleh jenis ini
 * @see RacikanDetail komponen penyusun racikan
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "jenis_racikan")
public class JenisRacikan extends GeneralValueObject {

	/**
	 * Versi serialisasi Java, seragam untuk seluruh entitas modul {@code sirs} karena berasal dari
	 * templat pembangkit yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama tabel {@code sirs.jenis_racikan}, dibangkitkan basis data (IDENTITY). */
	private Long id;

	/** Identitas pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String olehId;

	/**
	 * Mengembalikan identitas pengguna yang terakhir mengubah baris ini.
	 *
	 * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi identitas pengguna pengubah terakhir; nilai kosong/spasi diabaikan agar jejak audit
	 * tidak terhapus oleh form yang mengirim isian kosong.
	 *
	 * @param olehId id pengguna pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Nama pengguna yang terakhir mengubah baris ini — field audit bayangan. */
	private String oleh;

	/**
	 * Representasi teks jenis racikan untuk komponen ZK, memakai field {@link #nama} langsung.
	 * Karena nama tidak dijaga keunikannya, dua baris berbeda dapat menghasilkan label identik.
	 *
	 * @return nama jenis racikan; dapat {@code null} bila belum diisi
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Mengisi nama pengguna pengubah terakhir; nilai kosong/spasi diabaikan.
	 *
	 * @param oleh nama pengguna pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna pengubah terakhir, atau {@code null}
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang mendelegasikan pencatatan audit ke
	 * {@code AuditTimestampInterceptor.ubah(this)} setiap kali baris ini diperbarui.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Mengisi cap waktu perubahan terakhir; normalnya diisi otomatis oleh interceptor audit.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengembalikan cap waktu perubahan terakhir baris ini.
	 *
	 * @return cap waktu perubahan terakhir (diinisialisasi ke waktu pembuatan objek)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Nama bentuk sediaan racikan (puyer, kapsul, sirup, dan sebagainya); wajib diisi, tidak unik. */
	private String nama;

	/** Keterangan bebas atas jenis racikan. */
	private String keterangan;

	/** Konstruktor kosong yang diwajibkan JPA/Hibernate. */
	public JenisRacikan() {
	}

	/**
	 * Mengembalikan kunci utama jenis racikan.
	 *
	 * @return id baris, atau {@code null} bila belum pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya untuk kerangka kerja persistensi atau saat menyalin
	 * entitas menjadi baris baru.
	 *
	 * @param id kunci utama baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan nama bentuk sediaan racikan.
	 *
	 * @return nama jenis racikan (kolom wajib, maksimal 50 karakter)
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Mengisi nama bentuk sediaan racikan. Tidak ada pemeriksaan tabrakan nama di lapisan model
	 * maupun indeks unik pada kolomnya, sehingga duplikasi mungkin terjadi dan menghasilkan
	 * pilihan yang tampak identik di layar.
	 *
	 * @param nama nama jenis racikan
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas atas jenis racikan.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Mengisi keterangan bebas atas jenis racikan.
	 *
	 * @param keterangan catatan bebas
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
