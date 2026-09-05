package ais.database.model.recruitment;

// Generated Apr 5, 2010 1:13:29 AM by Hibernate Tools 3.2.4.CR1

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



import ais.database.model.Gedung;
import ais.database.model.GeneralValueObject;

/**
 * Entity JPA/Hibernate untuk tabel {@code public.ruang_pegawai}: master ruang ujian yang dipakai
 * untuk pelaksanaan ujian seleksi pada modul rekrutmen calon pegawai. Satu baris mewakili satu
 * ruangan fisik (kode, nama, kapasitas, gedung) yang bisa dikaitkan ke satu {@link
 * GelombangPendaftaranPegawai} dan/atau satu {@link UjianPegawai} tertentu sebagai lokasi
 * pelaksanaan ujian.
 *
 * <p><b>Relasi:</b></p>
 * <ul>
 * <li>{@link #getGedung()} — {@code @ManyToOne} opsional ke {@link Gedung} (gedung fisik tempat
 * ruangan ini berada).</li>
 * <li>{@link #getGelombangPendaftaranPegawai()} — {@code @ManyToOne} opsional ke {@link
 * GelombangPendaftaranPegawai}, gelombang yang memakai ruangan ini.</li>
 * <li>{@link #getUjianPegawai()} — {@code @ManyToOne} opsional ke {@link UjianPegawai}, ujian
 * spesifik yang memakai ruangan ini.</li>
 * <li>Entity anak {@link RuangGelombangPendaftaranPegawaiPegawai} mereferensikan ruangan ini
 * (sisi pemilik FK, tidak dideklarasikan di kelas ini) untuk mencatat penempatan tiap calon
 * pegawai ke ruangan.</li>
 * </ul>
 *
 * <p><b>Field {@link #getPenuh()}</b> adalah penanda "ruangan penuh" berupa {@link Integer} yang
 * dipakai selayaknya boolean (0/1) — bukan {@code Boolean} — dan diperbarui secara manual oleh
 * lapisan action ({@code RuangPegawaiAction}) berdasarkan perbandingan jumlah pengisi terhadap
 * {@link #getKapasitasRuangan()}, bukan dihitung otomatis oleh trigger database maupun getter di
 * kelas ini. Ini pola "flag shadow" yang harus dijaga konsistensinya secara eksplisit oleh
 * pemanggil: menambah/mengurangi penempatan {@link RuangGelombangPendaftaranPegawaiPegawai} tanpa
 * memanggil ulang logika yang memperbarui {@link #getPenuh()} akan membuat flag ini basi
 * (stale) relatif terhadap jumlah penempatan sesungguhnya.</p>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE tercatat ke
 * tabel revisi historis terpisah.</p>
 *
 * @see GelombangPendaftaranPegawai
 * @see UjianPegawai
 * @see RuangGelombangPendaftaranPegawaiPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "ruang_pegawai")



public class RuangPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment.
	 */
	private static final long serialVersionUID = -7550466125892447098L;
	/**
	 * Primary key baris ini pada tabel {@code ruang_pegawai}, dihasilkan otomatis oleh database
	 * ({@code IDENTITY}). Lihat {@link #getId()}.
	 */
	private Long id;
	/**
	 * Nama tampilan pengguna yang terakhir membuat/mengubah baris ini. Lihat {@link #getOleh()}/
	 * {@link #setOleh(String)}.
	 */
	private String oleh;
	/**
	 * ID pengguna yang terakhir membuat/mengubah baris ini, pasangan dari {@link #oleh}. Lihat
	 * {@link #getOlehId()}/{@link #setOlehId(String)}.
	 */
	private String olehId;

	/**
	 * Mengambil ID pengguna audit terakhir.
	 *
	 * @return ID pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengeset ID pengguna audit. Menolak (no-op) nilai {@code null}/kosong-whitespace — pola
	 * audit-shadow-field yang berulang di seluruh entity AIS agar jejak "olehId" tidak pernah
	 * tertimpa kosong.
	 *
	 * @param olehId ID pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengeset nama pengguna audit. Guard yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null}/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna audit terakhir.
	 *
	 * @return nama pengguna, atau {@code null} bila belum pernah diset.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: mendelegasikan pembaruan timestamp audit ke {@link
	 * ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} tepat sebelum UPDATE
	 * dijalankan. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengeset timestamp perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah timestamp baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp terakhir baris ini diubah.
	 *
	 * @return timestamp perubahan terakhir; tidak pernah {@code null}.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string entity ini untuk keperluan tampilan/log (mis. pada dropdown pemilihan
	 * ruang di UI ZK) — hanya nama ruangan.
	 *
	 * @return nilai {@link #nama} apa adanya (bisa {@code null} bila belum diisi).
	 */
	public String toString() {
		return nama;
	}

	/**
	 * Kode identifikasi ruangan (mis. kode singkat internal). Lihat {@link #getKodeRuangan()}.
	 */
	private String kodeRuangan;
	/**
	 * Nama ruangan, ditampilkan ke admin/peserta. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Gedung fisik tempat ruangan ini berada. Lihat {@link #getGedung()}.
	 */
	private Gedung gedung;
	/**
	 * Kapasitas maksimum peserta ruangan ini. Lihat {@link #getKapasitasRuangan()} — default 30.
	 */
	private Integer kapasitasRuangan;
	/**
	 * Gelombang pendaftaran yang memakai ruangan ini. Lihat {@link
	 * #getGelombangPendaftaranPegawai()}.
	 */
	private GelombangPendaftaranPegawai gelombangPendaftaranPegawai;
	/**
	 * Penanda "ruangan penuh" (0/1 disimpan sebagai {@link Integer}, bukan {@link Boolean}),
	 * diperbarui manual oleh lapisan action berdasarkan perbandingan jumlah pengisi terhadap
	 * {@link #getKapasitasRuangan()}. Lihat catatan pola "flag shadow" pada Javadoc kelas dan
	 * {@link #getPenuh()} — default 0.
	 */
	private Integer penuh;

	/**
	 * Ujian spesifik yang memakai ruangan ini. Lihat {@link #getUjianPegawai()}.
	 */
	private UjianPegawai ujianPegawai;

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi lewat refleksi. Field
	 * lain (nama, kode ruangan, kapasitas, gedung) harus diisi terpisah lewat setter.
	 */
	public RuangPegawai() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID ruangan, atau {@code null} untuk instance transient.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengeset {@link #id}.
	 *
	 * @param id nilai baru untuk {@link #id}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil kode identifikasi ruangan.
	 *
	 * @return kode ruangan; kolom wajib diisi ({@code nullable = false}) pada database, tapi
	 * tidak ada validasi null di level Java sebelum persist.
	 */
	@Column(name = "kode_ruangan", nullable = false, length = 50)
	public String getKodeRuangan() {
		return this.kodeRuangan;
	}

	/**
	 * Mengeset {@link #kodeRuangan}.
	 *
	 * @param kodeRuangan nilai baru untuk {@link #kodeRuangan}.
	 */
	public void setKodeRuangan(String kodeRuangan) {
		this.kodeRuangan = kodeRuangan;
	}

	/**
	 * Mengambil nama ruangan, dengan whitespace di kedua ujung dipangkas ({@link
	 * String#trim()}).
	 *
	 * @return nama ruangan yang sudah di-trim, atau {@code null} bila field belum diisi.
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengeset {@link #nama}.
	 *
	 * @param nama nilai baru untuk {@link #nama}.
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengambil gedung fisik tempat ruangan ini berada. Relasi {@code @ManyToOne} dengan {@code
	 * FetchMode.SELECT} (query terpisah saat diakses); kolom FK opsional ({@code nullable =
	 * true}). Tidak memakai resolusi proxy lewat {@link GeneralValueObject#check(Object)} —
	 * proxy Hibernate dikembalikan apa adanya.
	 *
	 * @return {@link Gedung}, atau {@code null} bila ruangan tidak terikat gedung manapun.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gedung", nullable = true)
	public Gedung getGedung() {
		return this.gedung;
	}

	/**
	 * Mengeset {@link #gedung}.
	 *
	 * @param gedung nilai baru untuk {@link #gedung}.
	 */
	public void setGedung(Gedung gedung) {
		this.gedung = gedung;
	}

	/**
	 * Mengeset {@link #kapasitasRuangan}.
	 *
	 * @param kapasitasRuangan nilai baru untuk {@link #kapasitasRuangan}.
	 */
	public void setKapasitasRuangan(Integer kapasitasRuangan) {
		this.kapasitasRuangan = kapasitasRuangan;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) kapasitas maksimum peserta ruangan ini. <b>Efek
	 * samping:</b> menulis {@code 30} ke field bila masih {@code null} — bukan getter murni.
	 *
	 * @return kapasitas ruangan; default {@code 30} bila belum pernah diset.
	 */
	@Column(name = "kapasitas_ruangan", length = 10, nullable = false)
	public Integer getKapasitasRuangan() {
		if (kapasitasRuangan == null) {
			kapasitasRuangan = 30;
		}
		return kapasitasRuangan;
	}

	/**
	 * Mengeset {@link #gelombangPendaftaranPegawai}.
	 *
	 * @param gelombangPendaftaranPegawai nilai baru untuk {@link #gelombangPendaftaranPegawai}.
	 */
	public void setGelombangPendaftaranPegawai(GelombangPendaftaranPegawai gelombangPendaftaranPegawai) {
		this.gelombangPendaftaranPegawai = gelombangPendaftaranPegawai;
	}

	/**
	 * Mengambil gelombang pendaftaran yang memakai ruangan ini. Relasi {@code @ManyToOne} dengan
	 * {@code FetchMode.SELECT}; kolom FK tidak menyatakan {@code nullable} eksplisit (default JPA
	 * {@code true}, opsional).
	 *
	 * @return {@link GelombangPendaftaranPegawai}, atau {@code null} bila ruangan tidak terikat
	 * gelombang manapun.
	 */
	@ManyToOne(cascade = { CascadeType.MERGE, CascadeType.PERSIST })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "gelombang_pendaftaran_pegawai")
	public GelombangPendaftaranPegawai getGelombangPendaftaranPegawai() {
		return gelombangPendaftaranPegawai;
	}

	/**
	 * Mengeset {@link #penuh}.
	 *
	 * @param penuh nilai baru untuk {@link #penuh}.
	 */
	public void setPenuh(Integer penuh) {
		this.penuh = penuh;
	}

	/**
	 * Mengambil (dan bila perlu menormalkan) penanda "ruangan penuh". <b>Efek samping:</b>
	 * menulis {@code 0} ke field bila masih {@code null}. Nilai ini murni cerminan status yang
	 * ditulis eksplisit oleh lapisan action (mis. {@code RuangPegawaiAction}) berdasarkan
	 * perbandingan jumlah pengisi terhadap {@link #getKapasitasRuangan()} — getter ini sendiri
	 * tidak menghitung ulang kepenuhan ruangan, jadi nilainya bisa basi bila penempatan berubah di
	 * luar jalur yang memperbarui flag ini. Lihat catatan pola "flag shadow" pada Javadoc kelas.
	 *
	 * @return {@code 0} (belum penuh) sebagai default bila field belum pernah diset, {@code 1}
	 * bila lapisan pemanggil sebelumnya menandai ruangan sebagai penuh, atau nilai lain yang
	 * mungkin diset manual (tidak divalidasi ke {0, 1} secara ketat di level ini).
	 */
	@Column(name = "penuh")
	public Integer getPenuh() {
		if (penuh == null) {
			penuh = 0;
		}
		return penuh;
	}


	/**
	 * Mengambil ujian spesifik yang memakai ruangan ini. Relasi {@code @ManyToOne} opsional
	 * ({@code nullable = true}) dengan {@code FetchMode.SELECT}.
	 *
	 * @return {@link UjianPegawai}, atau {@code null} bila ruangan tidak terikat ujian tertentu
	 * (mis. ruangan umum yang dipakai lintas ujian dalam satu gelombang).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "ujian_pegawai", nullable = true)
	public UjianPegawai getUjianPegawai() {
		return ujianPegawai;
	}

	/**
	 * Mengeset {@link #ujianPegawai}.
	 *
	 * @param ujianPegawai nilai baru untuk {@link #ujianPegawai}.
	 */
	public void setUjianPegawai(UjianPegawai ujianPegawai) {
		this.ujianPegawai = ujianPegawai;
	}

}
