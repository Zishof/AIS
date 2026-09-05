package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import ais.database.model.GeneralValueObject;

/**
 * Riwayat/log satu laporan (report) yang pernah digenerate -- tabel {@code report_history}.
 * Berbeda dari kelas-kelas lain di paket ini, kelas ini bukan subclass {@link FileFoto}/{@link
 * FileFotoLain} -- ia mewarisi langsung dari {@link GeneralValueObject} dan tidak menyimpan blob
 * isi laporan yang sesungguhnya: {@link ais.action.report.Report#saveReportHistory} (satu-satunya
 * pemanggil {@code new ReportHistory()} yang teridentifikasi) SELALU memanggil {@link
 * #setFile(java.sql.Blob) setFile(null)}, sehingga kolom {@link #getFile()} pada praktiknya
 * selalu kosong -- baris ini murni catatan METADATA (nama berkas yang digenerate, format
 * laporan, dan barcode validasi), BUKAN penyimpanan berkas.
 *
 * <p><b>Fungsi barcode validasi.</b> {@link #getBarcode()} adalah kode yang disisipkan ke dalam
 * laporan (mis. dicetak sebagai barcode fisik di PDF) saat digenerate, lalu dipakai
 * {@link ais.action.report.helper.pdf.GenerateValidasiLaporanWindow} untuk memverifikasi
 * keaslian sebuah laporan cetak: kode discan/dimasukkan ulang, dicocokkan lewat query
 * {@code Restrictions.eq("barcode", ...)}, dan keberadaannya di tabel ini dianggap bukti bahwa
 * laporan tersebut memang pernah digenerate sistem.</p>
 *
 * <p><b>Nama berkas sebagai metadata, bukan penunjuk lokasi aman.</b> {@link #getNama()}
 * menyimpan nama berkas fisik hasil generate (lihat {@code myFile.getName()} pada pemanggil).
 * Karena berkas fisiknya sendiri disimpan di lokasi sementara di luar kendali kelas ini (lihat
 * catatan berkas laporan sementara pada {@code Report}), nama ini semata metadata riwayat --
 * bukan API untuk mengambil ulang berkas yang aman terhadap laporan yang sudah tidak ada di
 * disk.</p>
 *
 * @see ais.action.report.Report
 * @see ais.action.report.helper.pdf.GenerateValidasiLaporanWindow
 * @see GeneralValueObject
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "public", name = "report_history")
public class ReportHistory extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 1463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate}: menandai timestamp perubahan lewat
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap baris ini
	 * di-{@code UPDATE}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #nama} apa adanya; bisa {@code null} bila belum diisi. */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private Blob file;
	private String barcode;

	/** Konstruktor default (dipakai Hibernate). */
	public ReportHistory() {
	}

	/** @return primary key baris ini; kolom identity, tidak pernah di-{@code INSERT} manual. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return nama berkas laporan yang digenerate (di-trim), atau {@code null} bila belum diisi
	 *         -- metadata riwayat, bukan penunjuk lokasi berkas yang terjamin masih ada (lihat
	 *         Javadoc kelas).
	 */
	@Column(name = "nama", nullable = false, length = 150)
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama berkas laporan yang digenerate. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan baris ini (dipakai sebagai format laporan, mis. "pdf"/"xls"), atau {@code null}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan baris ini (format laporan). */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @param file isi biner laporan. CATATAN: pemanggil yang teridentifikasi ({@link
	 *             ais.action.report.Report#saveReportHistory}) selalu memanggil method ini
	 *             dengan {@code null} -- lihat Javadoc kelas untuk penjelasan bahwa baris ini
	 *             pada praktiknya murni metadata, bukan penyimpanan berkas.
	 */
	public void setFile(Blob file) {
		this.file = file;
	}

	/**
	 * @return isi biner laporan; pada praktiknya SELALU {@code null} karena satu-satunya
	 *         pemanggil {@link #setFile(Blob)} yang teridentifikasi selalu mengirim {@code null}
	 *         (lihat Javadoc kelas). Dipetakan ke kolom {@code _file} (diberi awalan garis bawah
	 *         karena {@code file} kemungkinan kata tercadang/konflik di database).
	 */
	@Column(name = "_file", nullable = true)
	public Blob getFile() {
		return file;
	}

	/** @param barcode kode validasi yang disisipkan ke laporan saat digenerate. */
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	/**
	 * @return kode validasi yang disisipkan ke laporan saat digenerate, dipakai {@link
	 *         ais.action.report.helper.pdf.GenerateValidasiLaporanWindow} untuk memverifikasi
	 *         keaslian laporan cetak lewat pencarian persis ({@code Restrictions.eq}). Kolom
	 *         wajib diisi ({@code nullable = false}).
	 */
	@Column(name = "barcode", length = 50, nullable = false)
	public String getBarcode() {
		return barcode;
	}

}
