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
 * Entity JPA/Hibernate untuk data master instalasi rumah sakit pada modul SIRS (Sistem Informasi
 * Rumah Sakit), dipetakan ke tabel {@code sirs.instalasi}. "Instalasi" di sini adalah klasifikasi
 * ADMINISTRATIF LUAS jenis unit pelayanan tempat suatu tindakan/diagnosa dibuat — mis. IGD/Instalasi
 * Gawat Darurat, Instalasi Rawat Jalan, Instalasi Rawat Inap — bukan sinonim dari {@link Poly}
 * (poliklinik spesifik seperti "Poli Anak"/"Poli Bedah") maupun {@link Bagian} (unit kerja
 * organisasi yang terhubung ke struktur akun akunting).
 *
 * <h2>Perbedaan dari {@link Poly} dan {@link Bagian} (verifikasi silang referensi FK)</h2>
 * <p>
 * Ketiga entity ini bernama mirip ("instalasi", "poli", "bagian" — sama-sama istilah unit
 * organisasi rumah sakit) tetapi ternyata TIDAK tumpang tindih, dikonfirmasi lewat penelusuran
 * seluruh pemakai field {@code private Instalasi ...}/{@code private Poly ...}/
 * {@code private Bagian ...} di basis kode:
 * </p>
 * <ul>
 *   <li><b>{@link Instalasi}</b> — hanya direferensikan SATU kali di seluruh basis kode, oleh
 *   {@link DiagnosaPenyakit#getInstalasi()} ("Instalasi (UGD/rawat jalan/rawat inap) tempat
 *   diagnosa dibuat", sudah didokumentasikan pada batch 100). Data master ini datar (hanya
 *   {@link #nama} + {@link #keterangan}, tanpa hierarki maupun kode unik) dan tampaknya dipakai
 *   untuk klasifikasi statistik/pelaporan jenis layanan, BUKAN untuk penjadwalan atau billing.</li>
 *   <li><b>{@link Poly}</b> — dipakai luas untuk alur klinis/penjadwalan: {@code Pendaftaran},
 *   {@code BookingRegistrasi}, {@code JadwalDokter}, {@code DiagnosaPenyakit}, serta tiga action
 *   pendaftaran ({@code PendaftaranRawatUgdAction}, {@code PendaftaranRawatJalanAction},
 *   {@code PendaftaranRawatInapAction}). Memiliki {@code kode} unik, hierarki sendiri lewat
 *   {@code polyDari} (poli induk/anak), serta {@code jenis} dan {@code pejabat} — mewakili
 *   poliklinik/departemen KLINIS spesifik tempat pasien didaftarkan dan dokter dijadwalkan.</li>
 *   <li><b>{@link Bagian}</b> — dipakai untuk struktur organisasi yang terhubung ke akunting:
 *   {@code Pegawai} (pegawai bekerja di satu bagian), {@code TransaksiMedis}, {@code Pembayaran},
 *   {@code TransaksiRetur}, {@code Lokasi} (aset). Memiliki {@code kode} unik serta relasi
 *   {@code Devisi} dan {@code Akun} — mewakili unit kerja untuk keperluan HR/biaya/akunting,
 *   BUKAN unit klinis tempat pasien dilayani.</li>
 * </ul>
 * <p>Kesimpulan: nama-nama ini TIDAK menyesatkan pada kasus ini — ketiganya benar-benar mewakili
 * tiga sumbu klasifikasi organisasi rumah sakit yang berbeda (klasifikasi layanan luas vs
 * poliklinik spesifik vs unit kerja akunting), dipakai oleh himpunan entity yang nyaris tidak
 * beririsan.</p>
 *
 * <p>Class ini adalah entity hbm2java standar paling sederhana di klaster ini: hanya
 * {@link #nama} dan {@link #keterangan} selain field generik warisan {@link GeneralValueObject},
 * tanpa relasi {@code @ManyToOne}/{@code @OneToMany} apa pun. Field audit
 * {@link #oleh}/{@link #olehId}/{@link #tanggal_dirubah} adalah shadow field standar AIS (diisi
 * {@code AuditTimestampInterceptor} lewat {@link #onUpdate()}) — KEHARUSAN TEKNIS pola audit
 * aplikasi, bukan bug. Modul {@code sirs} tidak memiliki sumbu tenant/satuan-kerja sama sekali
 * (dikonfirmasi berulang kali pada audit sebelumnya, {@code task_90bbdd51}).</p>
 *
 * @see DiagnosaPenyakit satu-satunya entity yang mereferensikan {@link Instalasi} lewat {@code instalasi}
 * @see Poly poliklinik spesifik, sumbu klasifikasi berbeda dari instalasi
 * @see Bagian unit kerja organisasi/akunting, sumbu klasifikasi berbeda dari instalasi
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "instalasi")
public class Instalasi extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas antar build. Nilai ini disalin dari template
	 * hbm2java standar AIS (identik di banyak entity sekelas) dan tidak perlu diubah kecuali
	 * struktur field berubah secara tidak kompatibel.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Primary key tabel {@code sirs.instalasi}. Lihat {@link #getId()}. */
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
	 * Representasi string dari instalasi ini, dipakai komponen ZK (combobox/label) yang
	 * memanggil {@code toString()} secara implisit.
	 *
	 * @return {@link #nama} instalasi apa adanya (tanpa null-check eksplisit — akan
	 *         mengembalikan {@code null} bila {@link #nama} belum diisi)
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

	/** Nama instalasi, mis. "Instalasi Gawat Darurat", "Instalasi Rawat Jalan". Lihat {@link #getNama()}. */
	private String nama;
	/** Keterangan bebas tentang instalasi ini. Lihat {@link #getKeterangan()}. */
	private String keterangan;

	/** Konstruktor default kosong, dibutuhkan Hibernate untuk instansiasi lewat refleksi. */
	public Instalasi() {
	}

	/**
	 * Mengembalikan primary key baris ini.
	 *
	 * @return ID instalasi, atau {@code null} untuk instance yang belum tersimpan
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
	 * Mengembalikan nama instalasi ini.
	 *
	 * @return nama instalasi, mis. "Instalasi Gawat Darurat"
	 */
	@Column(name = "nama", nullable = false, length = 50)
	public String getNama() {
		return this.nama;
	}

	/**
	 * Menetapkan nama instalasi ini. Kolom wajib diisi di lapisan basis data
	 * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
	 *
	 * @param nama nama baru, maksimal 50 karakter di kolom basis data
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan bebas tentang instalasi ini.
	 *
	 * @return keterangan, atau {@code null} bila tidak diisi
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas tentang instalasi ini.
	 *
	 * @param keterangan keterangan baru, boleh {@code null}
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

}
