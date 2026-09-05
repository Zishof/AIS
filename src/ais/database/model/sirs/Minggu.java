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
import ais.database.model.Tbmuser;

/**
 * Entitas BARIS-KERJA (scratch/staging) per pengguna yang menyimpan
 * pemecahan satu bulan-tahun tertentu menjadi rentang-rentang minggu,
 * pada schema {@code sirs} (tabel {@code minggu}) — MESKI hidup di
 * paket {@code sirs}, kelas ini dipakai oleh utilitas laporan UMUM
 * {@code ais.action.report.helper.CommonReport} (bukan spesifik modul
 * rumah sakit), pola "entitas nyasar ke paket yang salah" yang serupa
 * dengan temuan berulang di paket model AIS lain.
 *
 * <p>
 * Diverifikasi dari {@code CommonReport.inputMinggu(Tbmuser, Integer,
 * Integer)}: untuk kombinasi {@link #getTbmuser()}/{@link #getBulan()}/
 * {@link #getTahun()} tertentu, SELURUH baris {@code minggu} milik
 * pengguna tersebut DIHAPUS TOTAL (SQL {@code delete from sirs.minggu
 * where tbmuser = :userId}) lalu DIBUAT ULANG hingga 10 baris baru
 * (masing-masing merepresentasikan satu minggu, lewat
 * {@link #getTanggalMulai()}) setiap kali laporan mingguan dibangkitkan.
 * Baris-baris ini kemudian dibaca kembali untuk mengisi parameter
 * JasperReport bernama {@code mg1}..{@code mg10} pada template laporan.
 * Ini BUKAN master data historis — ia adalah tabel kerja yang sengaja
 * ditimpa berulang, sehingga anotasi {@code @Audited} pada kelas ini
 * hanya akan mencatat riwayat hapus/buat-ulang yang sering terjadi,
 * bukan riwayat perubahan bisnis yang bermakna.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "sirs", name = "minggu")
public class Minggu extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String olehId;

	/**
	 * Mengambil ID pengguna (username/oleh-id) yang terakhir mengubah baris
	 * ini. Field audit shadow yang diisi lewat {@link #setOlehId(String)}.
	 *
	 * @return ID pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan ID pengguna yang mengubah baris ini. Nilai kosong/blank
	 * SENGAJA diabaikan (early return) agar field audit ini tidak pernah
	 * ditimpa jadi kosong — pola field audit shadow yang merupakan
	 * KEHARUSAN TEKNIS berulang di paket model AIS, bukan bug.
	 *
	 * @param olehId ID pengguna; nilai {@code null} atau string
	 *               kosong/whitespace-saja tidak akan mengubah nilai
	 *               tersimpan.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	private String oleh;

	/**
	 * Representasi ringkas baris minggu ini untuk keperluan tampilan/log.
	 *
	 * @return teks keterangan baris minggu ini.
	 */
	public String toString() {
		return keterangan;
	}

	/**
	 * Menetapkan nama pengguna yang mengubah baris ini. Nilai kosong/blank
	 * diabaikan agar field audit ini tidak pernah ditimpa kosong.
	 *
	 * @param oleh nama pengguna; nilai {@code null} atau
	 *             kosong/whitespace-saja tidak akan mengubah nilai
	 *             tersimpan.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Mengambil nama pengguna yang terakhir mengubah baris ini.
	 *
	 * @return nama pengguna terakhir yang mengubah, atau {@code null} jika
	 *         belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate} yang memperbarui {@link #tanggal_dirubah}
	 * otomatis sebelum baris ini di-UPDATE, lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = new Date();

	/**
	 * Menetapkan timestamp perubahan terakhir baris ini secara manual.
	 *
	 * @param tanggal_dirubah timestamp perubahan baru.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Mengambil timestamp perubahan terakhir baris ini, diperbarui
	 * otomatis oleh {@link #onUpdate()} setiap kali baris di-UPDATE.
	 *
	 * @return timestamp perubahan terakhir.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	private String keterangan;
	private Date tanggalMulai;
	private Date tanggalSampai;
	private Tbmuser tbmuser;
	private Integer bulan;
	private Integer tahun;

	/**
	 * Konstruktor default (wajib untuk entitas JPA/Hibernate).
	 */
	public Minggu() {
	}

	/**
	 * Primary key baris minggu, auto-increment (IDENTITY) dan diisi
	 * database.
	 *
	 * @return ID unik baris minggu ini.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan ID baris minggu.
	 *
	 * @param id ID baris minggu.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengambil keterangan bebas baris minggu ini.
	 *
	 * @return teks keterangan, atau {@code null} jika belum diisi.
	 */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menetapkan keterangan bebas baris minggu ini.
	 *
	 * @param keterangan teks keterangan.
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menetapkan tanggal awal minggu ini.
	 *
	 * @param tanggalMulai tanggal awal minggu.
	 */
	public void setTanggalMulai(Date tanggalMulai) {
		this.tanggalMulai = tanggalMulai;
	}

	/**
	 * Mengambil tanggal awal minggu ini — nilai inilah yang dibaca
	 * {@code CommonReport} untuk mengisi parameter laporan {@code mg1}..
	 * {@code mg10} (lihat javadoc kelas).
	 *
	 * @return tanggal awal minggu, atau {@code null} jika belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_mulai", nullable = true)
	public Date getTanggalMulai() {
		return tanggalMulai;
	}

	/**
	 * Menetapkan tanggal akhir minggu ini.
	 *
	 * @param tanggalSampai tanggal akhir minggu.
	 */
	public void setTanggalSampai(Date tanggalSampai) {
		this.tanggalSampai = tanggalSampai;
	}

	/**
	 * Mengambil tanggal akhir minggu ini.
	 *
	 * @return tanggal akhir minggu, atau {@code null} jika belum diisi.
	 */
	@Temporal(TemporalType.DATE)
	@Column(name = "tanggal_sampai", nullable = true)
	public Date getTanggalSampai() {
		return tanggalSampai;
	}

	/**
	 * Menetapkan pengguna pemilik baris minggu-kerja ini.
	 *
	 * @param tbmuser pengguna pemilik.
	 */
	public void setTbmuser(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Mengambil pengguna pemilik baris minggu-kerja ini — WAJIB diisi
	 * ({@code nullable = false}); satu pengguna hanya punya SATU set
	 * baris minggu aktif pada satu waktu, karena baris lama dihapus
	 * total setiap kali {@code CommonReport.inputMinggu(...)} dipanggil
	 * ulang untuk pengguna yang sama (lihat javadoc kelas). Proxy lazy
	 * diresolusi lewat {@link #check(Object)}.
	 *
	 * @return pengguna pemilik baris minggu-kerja ini.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "tbmuser", nullable = false)
	public Tbmuser getTbmuser() {
		tbmuser = check(tbmuser);
		return tbmuser;
	}

	/**
	 * Menetapkan bulan (1&ndash;12) yang menjadi acuan pemecahan minggu
	 * ini.
	 *
	 * @param bulan nomor bulan (1&ndash;12).
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Mengambil bulan (1&ndash;12) yang menjadi acuan pemecahan minggu
	 * ini.
	 *
	 * @return nomor bulan (1&ndash;12), atau {@code null} jika belum
	 *         diisi.
	 */
	public Integer getBulan() {
		return bulan;
	}

	/**
	 * Menetapkan tahun yang menjadi acuan pemecahan minggu ini.
	 *
	 * @param tahun tahun (mis. 2026).
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Mengambil tahun yang menjadi acuan pemecahan minggu ini.
	 *
	 * @return tahun, atau {@code null} jika belum diisi.
	 */
	public Integer getTahun() {
		return tahun;
	}

}
