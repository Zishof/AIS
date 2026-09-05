package ais.database.model.recruitment;
// Generated 10 Okt 18 12:46:07 by Hibernate Tools 5.2.3.Final

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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
 * Entity JPA/Hibernate untuk tabel {@code public.kelompok_pendaftaran_pegawai}: kelompok/kategori
 * pendaftaran di dalam satu {@link GelombangPendaftaranPegawai} pada modul rekrutmen calon
 * pegawai. Dipakai untuk membagi kuota dan (berpotensi) ambang skor kelulusan menjadi beberapa
 * kelompok terpisah dalam satu gelombang yang sama, tiap kelompok punya nama, deskripsi, kuota
 * kursi, dan status aktif sendiri.
 *
 * <p><b>Field {@link #getSkorSampai()}</b> adalah ambang skor (batas atas skor kelompok ini), tapi
 * penelusuran kode di {@code ais.action.master.recruitment} menemukan hanya getter/setter yang
 * dipakai (mis. dari layar CRUD), <b>tidak ada jalur kode yang benar-benar membaca field ini untuk
 * mengelompokkan atau memvalidasi skor pelamar</b> pada saat dokumentasi ini ditulis — field ini
 * tampak seperti fitur yang direncanakan (kelompok berbasis rentang skor) namun belum (atau tidak
 * lagi) diwujudkan di lapisan action/service. Ini bukan celah keamanan (tidak ada input klien yang
 * dipercaya untuk menentukan kelulusan lewat field ini), melainkan field dorman yang perlu
 * diperlakukan hati-hati: jangan berasumsi field ini otomatis menegakkan aturan kelompok skor apa
 * pun.</p>
 *
 * <p><b>Relasi:</b> {@link #getGelombangPendaftaran()} adalah {@code @ManyToOne} wajib ({@code
 * nullable = false}) dengan {@code fetch = LAZY} ke {@link GelombangPendaftaranPegawai}, memakai
 * resolusi proxy lewat {@link GeneralValueObject#check(Object)}.</p>
 *
 * <p>Diaudit oleh Hibernate Envers ({@code @Audited}); setiap INSERT/UPDATE/DELETE tercatat ke
 * tabel revisi historis terpisah.</p>
 *
 * @see GelombangPendaftaranPegawai
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(name = "kelompok_pendaftaran_pegawai", schema = "public")
public class KelompokPendaftaranPegawai extends GeneralValueObject {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deployment.
	 */
	private static final long serialVersionUID = 5909958736690383653L;
	/**
	 * Primary key baris ini pada tabel {@code kelompok_pendaftaran_pegawai}, dihasilkan otomatis
	 * oleh database ({@code IDENTITY}). Lihat {@link #getId()}.
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
	 * Gelombang pendaftaran induk yang menaungi kelompok ini. Lihat {@link
	 * #getGelombangPendaftaran()}.
	 */
	private GelombangPendaftaranPegawai gelombangPendaftaran;
	/**
	 * Deskripsi bebas kelompok ini. Lihat {@link #getDeskripsi()}.
	 */
	private String deskripsi;
	/**
	 * Kuota kursi/kursi yang tersedia untuk kelompok ini. Lihat {@link #getKuota()} — default 30.
	 */
	private Integer kuota;
	/**
	 * Ambang skor (batas atas) kelompok ini; lihat catatan Javadoc kelas tentang status dorman
	 * field ini. Lihat {@link #getSkorSampai()} — default 0.0.
	 */
	private Double skorSampai;
	/**
	 * Nama kelompok, ditampilkan ke admin/peserta. Lihat {@link #getNama()}.
	 */
	private String nama;
	/**
	 * Penanda apakah kelompok ini aktif. Lihat {@link #getAktif()} — default {@code true}.
	 */
	private Boolean aktif;

	/**
	 * Konstruktor kosong yang disyaratkan Hibernate/JPA untuk instansiasi lewat refleksi. Field
	 * lain (nama, gelombang induk, kuota) harus diisi terpisah lewat setter.
	 */
	public KelompokPendaftaranPegawai() {
	}

	/**
	 * Mengambil primary key baris ini.
	 *
	 * @return ID kelompok, atau {@code null} untuk instance transient.
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
	 * Mengambil gelombang pendaftaran induk kelompok ini, dengan resolusi proxy lazy lewat {@link
	 * GeneralValueObject#check(Object)}. Relasi {@code @ManyToOne} dengan {@code fetch = LAZY} dan
	 * kolom FK {@code gelombang_pendaftaran_id} wajib ({@code nullable = false}).
	 *
	 * @return {@link GelombangPendaftaranPegawai} induk setelah resolusi proxy; secara skema tidak
	 * boleh {@code null} pada baris yang sudah dipersist.
	 * @see GeneralValueObject#check(Object)
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "gelombang_pendaftaran_id", nullable = false)
	public GelombangPendaftaranPegawai getGelombangPendaftaran() {
		gelombangPendaftaran = check(gelombangPendaftaran);
		return gelombangPendaftaran;
	}

	/**
	 * Mengeset {@link #gelombangPendaftaran}.
	 *
	 * @param gelombangPendaftaran nilai baru untuk {@link #gelombangPendaftaran}.
	 */
	public void setGelombangPendaftaran(GelombangPendaftaranPegawai gelombangPendaftaran) {
		this.gelombangPendaftaran = gelombangPendaftaran;
	}

	/**
	 * Mengambil deskripsi bebas kelompok ini.
	 *
	 * @return teks deskripsi, atau {@code null} bila belum diisi.
	 */
	@Column(name = "deskripsi")
	public String getDeskripsi() {
		return this.deskripsi;
	}

	/**
	 * Mengeset {@link #deskripsi}.
	 *
	 * @param deskripsi nilai baru untuk {@link #deskripsi}.
	 */
	public void setDeskripsi(String deskripsi) {
		this.deskripsi = deskripsi;
	}

	/**
	 * Mengambil kuota kursi kelompok ini.
	 *
	 * @return kuota kursi; default 30 bila field {@link #kuota} belum pernah diset. Tipe kembalian
	 * primitif {@code int} (bukan {@link Integer}) berarti pemanggilan pada instance yang baru
	 * dikonstruksi lewat refleksi Hibernate tetap aman (tidak ada unboxing {@code null}) karena
	 * pengecekan {@code null} terhadap field {@link Integer} dilakukan sebelum unboxing.
	 */
	@Column(name = "kuota", nullable = false)
	public int getKuota() {
		return this.kuota == null ? 30 : kuota;
	}

	/**
	 * Mengeset {@link #kuota}.
	 *
	 * @param kuota nilai baru untuk {@link #kuota}.
	 */
	public void setKuota(int kuota) {
		this.kuota = kuota;
	}

	/**
	 * Mengambil nama kelompok pendaftaran ini.
	 *
	 * @return nama kelompok; kolom wajib diisi ({@code nullable = false}) pada database, tapi
	 * tidak ada validasi null di level Java sebelum persist.
	 */
	@Column(name = "nama", nullable = false)
	public String getNama() {
		return this.nama;
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
	 * Mengambil status aktif kelompok ini. Tidak dipetakan dengan {@code @Column} eksplisit (kolom
	 * disimpulkan dari nama getter oleh Hibernate secara implisit).
	 *
	 * @return {@code true} bila field {@link #aktif} belum pernah diset — kelompok baru dianggap
	 * aktif secara default — atau nilai eksplisit yang tersimpan.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengeset {@link #aktif}.
	 *
	 * @param aktif nilai baru untuk {@link #aktif}.
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengambil ambang skor (batas atas) kelompok ini. Lihat catatan pada Javadoc kelas: field ini
	 * saat ini tidak dipakai oleh jalur kode manapun di {@code ais.action.master.recruitment} untuk
	 * benar-benar mengelompokkan atau memvalidasi skor pelamar — anggap sebagai data konfigurasi
	 * pasif, bukan aturan yang ditegakkan otomatis.
	 *
	 * @return ambang skor; default {@code 0.0} bila field {@link #skorSampai} belum pernah diset.
	 */
	public Double getSkorSampai() {
		return skorSampai == null ? 0.0 : skorSampai;
	}

	/**
	 * Mengeset {@link #skorSampai}.
	 *
	 * @param skorSampai nilai baru untuk {@link #skorSampai}.
	 */
	public void setSkorSampai(Double skorSampai) {
		this.skorSampai = skorSampai;
	}

}
