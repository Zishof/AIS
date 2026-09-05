package ais.database.model.sirs;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk data master poliklinik pada modul SIRS (Sistem Informasi Rumah
 * Sakit), dipetakan ke tabel {@code sirs.poly}. "Poly" (poliklinik) di sini adalah unit KLINIS
 * spesifik tempat pasien didaftarkan dan dokter dijadwalkan (mis. "Poli Anak", "Poli Bedah",
 * "Poli Gigi") — berbeda dari {@link Instalasi} (klasifikasi administratif luas IGD/rawat
 * jalan/rawat inap) maupun {@link Bagian} (unit kerja organisasi/akunting). Lihat javadoc
 * {@link Instalasi} untuk perbandingan lengkap ketiga sumbu klasifikasi ini berikut bukti
 * referensi FK yang membedakannya.
 *
 * <h2>Hierarki poli/subpoli</h2>
 * <p>
 * Entity ini memiliki relasi self-referencing {@link #getPolyDari()} yang memungkinkan satu poli
 * menjadi "anak" dari poli lain (mis. sub-spesialisasi di bawah poli induk). Pola pemakaiannya
 * yang berulang di banyak entity konsumen ({@code Pendaftaran}, {@code DiagnosaPenyakit},
 * {@code BookingRegistrasi}, serta action {@code PendaftaranRawatUgdAction},
 * {@code PendaftaranRawatJalanAction}, {@code PendaftaranRawatInapAction},
 * {@code BookingRegistrasiAction}) adalah pasangan field {@code poly}/{@code subpoly} — bukan
 * lewat {@link #getPolyDari()} pada instance yang sama, melainkan dua referensi TERPISAH ke dua
 * instance {@link Poly} yang berbeda (poli utama dan sub-poli yang dipilih pengguna pada
 * pendaftaran). {@link #getPolyDari()} sendiri lebih berperan sebagai metadata struktural data
 * master (poli mana anak dari poli mana), sementara pasangan {@code poly}/{@code subpoly} pada
 * entity konsumen adalah pilihan aktual yang dibuat per-transaksi.</p>
 *
 * <p>Class ini adalah entity hbm2java standar: relasi {@link #getPolyDari()} memakai
 * {@code FetchMode.SELECT} eksplisit (bukan default {@code JOIN}), sehingga poli induk dimuat
 * lewat query {@code SELECT} terpisah saat diakses. Field audit
 * {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS (diisi
 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola audit
 * aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama sekali
 * (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see Instalasi klasifikasi administratif luas, sumbu berbeda dari poliklinik
 * @see Bagian unit kerja organisasi/akunting, sumbu berbeda dari poliklinik
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "poly")
public class Poly extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.poly}. Lihat {@link #getId()}. */
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
	 * Representasi string dari poli ini, dipakai komponen ZK (combobox/label) yang memanggil
	 * {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} poli apa adanya (tanpa null-check eksplisit — akan mengembalikan
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

	/** Nama poliklinik, mis. "Poli Anak", "Poli Bedah". Lihat {@link #getNama()}. */
	private String nama;
	/** Kode unik poliklinik. Lihat {@link #getKode()}. */
	private String kode;
	/** Keterangan bebas tentang poliklinik ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Nama pejabat/penanggung jawab poliklinik ini. Lihat {@link #getPejabat()}. */
	private String pejabat;
	/** Jenis/kategori poliklinik (nilai bebas, lihat catatan {@link #getJenis()}). */
	private String jenis;
	/** Poliklinik induk dalam hierarki poli/subpoli data master. Lihat {@link #getPolyDari()}. */
	private Poly polyDari;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public Poly() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID poli, atau {@code null} untuk instance yang belum tersimpan
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
	 * Mengembalikan nama poliklinik ini.
	 *
	 * @return nama poli, mis. "Poli Anak", atau {@code null} bila belum diisi (kolom
	 *         {@code nullable = true} — berbeda dari kebanyakan entity {@code sirs} lain yang
	 *         mewajibkan {@code nama})
	 */
	@Column(name = "nama", nullable = true, length = 255)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama poliklinik ini.
	 *
	 * @param nama nama baru, boleh {@code null}, maksimal 255 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang poliklinik ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang poliklinik ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan kode unik poliklinik ini.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan kode unik poliklinik ini. Kolom wajib diisi dan bertanda {@code unique} di
	 * basis data — dua poli tidak boleh berbagi kode yang sama, meski getter/setter ini sendiri
	 * tidak melakukan pengecekan duplikasi (constraint ditegakkan basis data saat simpan).
	 *
	 * @return kode poli
	 */
	@Column(name = "kode", nullable = false, unique = true, length = 255)
	public String getKode() {
		return kode;
	}

	/**
	 * Menetapkan nama pejabat/penanggung jawab poliklinik ini.
	 *
	 * @param pejabat nama pejabat baru, boleh {@code null}
	 */
	public void setPejabat(String pejabat) {
		this.pejabat = pejabat;
	}

	/**
	 * Mengembalikan nama pejabat/penanggung jawab poliklinik ini. Field ini TIDAK dianotasi
	 * {@code @Column} eksplisit — dipetakan Hibernate secara implisit berdasarkan konvensi nama
	 * getter (perilaku default anotasi entity JPA di kelas ini).
	 *
	 * @return nama pejabat, atau {@code null} bila tidak diisi
	 */
	public String getPejabat() {
		return pejabat;
	}

	/**
	 * Menetapkan poliklinik induk dalam hierarki data master poli/subpoli. Lihat catatan pada
	 * javadoc kelas: field ini berbeda dari pasangan {@code poly}/{@code subpoly} yang dipakai
	 * entity konsumen (mis. {@code Pendaftaran}) untuk memilih poli utama dan sub-poli per
	 * transaksi.
	 *
	 * @param polyDari poli induk baru, boleh {@code null} untuk poli tingkat teratas
	 */
	public void setPolyDari(Poly polyDari) {
		this.polyDari = polyDari;
	}

	/**
	 * Mengembalikan poliklinik induk dalam hierarki data master poli/subpoli. Relasi memakai
	 * {@code FetchMode.SELECT} eksplisit sehingga dimuat lewat query terpisah, bukan
	 * {@code JOIN} pada query utama.
	 *
	 * @return poli induk, atau {@code null} bila poli ini berada di tingkat teratas hierarki
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "poly_dari", nullable = true)
	public Poly getPolyDari() {
		return polyDari;
	}

	/**
	 * Mengembalikan jenis/kategori poliklinik ini. Field ini TIDAK dianotasi {@code @Column}
	 * eksplisit — dipetakan Hibernate secara implisit berdasarkan konvensi nama getter. Nilai
	 * yang tersimpan adalah string bebas; tidak ditemukan enum Java atau konstanta yang
	 * mendefinisikan nilai valid untuk field ini di dalam kelas ini sendiri.
	 *
	 * @return jenis poli, atau {@code null} bila tidak diisi
	 */
	public String getJenis() {
		return jenis;
	}

	/**
	 * Menetapkan jenis/kategori poliklinik ini.
	 *
	 * @param jenis jenis baru, boleh {@code null}
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

}
