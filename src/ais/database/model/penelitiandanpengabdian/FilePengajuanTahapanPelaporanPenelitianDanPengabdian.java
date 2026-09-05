package ais.database.model.penelitiandanpengabdian;

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



import ais.database.model.GeneralValueObject;

/**
 * Model entitas <b>lampiran berkas</b> untuk satu pengajuan laporan tahap
 * ({@link PengajuanTahapanPelaporanPenelitianDanPengabdian}): mencatat metadata satu berkas yang
 * diunggah (nama asli, mime type, path fisik di server, tanggal unggah) dan relasi langsungnya ke
 * baris pengajuan laporan tahap yang bersangkutan.
 *
 * <p>
 * <b>Bukan bagian dari paket lampiran generik {@code ais.database.model.file}
 * ({@link ais.database.model.file.LampiranLain} dkk.)</b> — kelas ini adalah tabel lampiran
 * <b>khusus modul</b> dengan relasi {@code @ManyToOne} langsung ke
 * {@link PengajuanTahapanPelaporanPenelitianDanPengabdian} (kolom FK
 * {@code pengajuan_tahapan_pelaporan_penelitian_dan_pengabdian}, {@code NOT NULL}), dipetakan ke
 * tabel {@code file_pengajuan_tahapan} (nama tabel lebih pendek daripada nama kelasnya). Satu baris
 * pengajuan laporan tahap dapat memiliki beberapa baris berkas dari waktu ke waktu (setiap unggahan
 * baru menambah baris, bukan menimpa); baris <b>terbaru</b> (id terbesar) yang dipakai untuk
 * menampilkan tombol unduh pada layar daftar pengajuan (lihat
 * {@code PengajuanTahapanPelaporanPenelitianDanPengabdianHelper}).
 * </p>
 *
 * <p>
 * Diisi lewat konstruksi manual (bukan lewat komponen upload generik {@code LampiranLain}) di
 * {@code PengajuanTahapanPelaporanPenelitianDanPengabdianHelper.displayWindowPengajuan}, dijalankan
 * asinkron lewat timer setelah baris pengajuan induk tersimpan agar transaksi utama tetap cepat.
 * </p>
 *
 * @see PengajuanTahapanPelaporanPenelitianDanPengabdian
 * @see FilePengajuanPenelitianDanPengabdian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "penelitiandanpengabdian", name = "file_pengajuan_tahapan")



public class FilePengajuanTahapanPelaporanPenelitianDanPengabdian extends GeneralValueObject {

	/**
	 * Versi kelas untuk kebutuhan serialisasi ({@link java.io.Serializable}). Nilai ini identik
	 * dengan {@code serialVersionUID} pada beberapa entitas lain di paket ini — sisa pola
	 * salin-tempel hbm2java, tidak bermakna khusus.
	 */
	private static final long serialVersionUID = 2463812577548439808L;
	/** Primary key baris lampiran berkas, auto-increment ({@code IDENTITY}) pada kolom {@code id}. */
	private Long id;
	/** Field audit legacy: nama pengguna yang melakukan perubahan terakhir (bebas format, isi manual). Lihat {@link #getOleh()}. */
	private String oleh;
	/** Field audit legacy: id/username pengguna yang melakukan perubahan terakhir. Lihat {@link #getOlehId()}. */
	private String olehId;

	/** @return id/username pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan (tidak menimpa nilai yang sudah tersimpan).
	 *
	 * @param olehId id/username pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna pencatat perubahan terakhir. Nilai {@code null} atau string kosong/spasi
	 * diabaikan, dengan alasan yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila {@code null} atau kosong setelah di-trim
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pengguna yang tercatat melakukan perubahan terakhir pada baris ini (field audit legacy, tidak dipetakan sebagai kolom entitas). */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence sesaat sebelum
	 * setiap {@code UPDATE} baris ini dieksekusi, mendelegasikan ke
	 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} untuk memperbarui
	 * {@link #tanggal_dirubah} ke waktu saat ini. Tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengatur cap waktu perubahan terakhir secara manual. Dalam alur normal field ini diperbarui
	 * otomatis lewat {@link #onUpdate()}.
	 *
	 * @param tanggal_dirubah cap waktu perubahan terakhir yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return cap waktu perubahan terakhir baris ini; diinisialisasi ke waktu pembuatan objek dan diperbarui otomatis oleh {@link #onUpdate()} pada setiap {@code UPDATE}. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return representasi teks ringkas baris ini untuk keperluan log/debug: gabungan nama berkas dan mime type dipisah {@code "_"}. */
	public String toString() {
		return fileName + "_" + mimeType;
	}

	/** Path absolut berkas fisik di penyimpanan server. Lihat {@link #getPath()}. */
	private String path;
	/** Mime type berkas hasil deteksi otomatis saat unggah (mis. {@code application/pdf}). Lihat {@link #getMimeType()}. */
	private String mimeType;
	/** Nama asli berkas yang diunggah pengaju. Lihat {@link #getNama()}. */
	private String fileName;
	/** Keterangan untuk baris lampiran ini. Tidak diisi lewat alur unggah normal di helper terkait (tidak ada field form untuk ini), tetap tersedia untuk kebutuhan lain (mis. impor data). Lihat {@link #getKeterangan()}. */
	private String keterangan;
	/** Pengajuan laporan tahap yang memiliki berkas lampiran ini (FK wajib). */
	private PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian;
	/** Waktu unggah berkas ini, default waktu pembuatan objek. Lihat {@link #getUploadDate()}. */
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();

	/** Konstruktor default (wajib untuk entitas Hibernate/JPA); seluruh field diisi lewat setter. */
	public FilePengajuanTahapanPelaporanPenelitianDanPengabdian() {
	}

	/** @return primary key baris lampiran berkas ini, atau {@code null} bila belum tersimpan. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengatur id baris ini secara manual. Karena kolom {@code id} dipetakan
	 * {@code insertable = false} (nilai dihasilkan basis data lewat {@code IDENTITY}), pengaturan
	 * manual di sini hanya berguna untuk menandai objek yang mewakili baris yang sudah ada.
	 *
	 * @param id primary key yang ingin diasosiasikan ke objek ini
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return keterangan untuk baris lampiran ini, apa adanya (boleh {@code null}; kolom dibatasi 1000 karakter). */
	@Column(name = "keterangan", length = 1000)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan baru untuk baris lampiran ini; boleh {@code null}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param mimeType mime type berkas (mis. {@code application/pdf}), biasanya hasil {@code Files.probeContentType(...)} saat unggah. */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/** @return mime type berkas ini, apa adanya (kolom dibatasi 255 karakter). Dipakai antara lain untuk header content-type saat unduhan lewat {@code Filedownload.save}. */
	@Column(name = "mime_type", length = 255)
	public String getMimeType() {
		return mimeType;
	}

	/** @param fileName nama asli berkas yang diunggah. Perhatikan nama method setter ("Nama") tidak selaras dengan nama field ({@code fileName}) maupun kolom ({@code file_name}) — sisa penamaan hbm2java. */
	public void setNama(String fileName) {
		this.fileName = fileName;
	}

	/** @return nama asli berkas yang diunggah, apa adanya (kolom dibatasi 255 karakter). Lihat catatan penamaan pada {@link #setNama(String)}. */
	@Column(name = "file_name", length = 255)
	public String getNama() {
		return fileName;
	}

	/** @return waktu unggah berkas ini (kolom {@code NOT NULL} di basis data); default waktu pembuatan objek bila tidak diatur ulang lewat {@link #setUploadDate(Date)}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	/** @param uploadDate waktu unggah baru untuk berkas ini. */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/** @return path absolut berkas fisik ini di penyimpanan server, apa adanya (boleh {@code null}; kolom dibatasi 1000 karakter). Dipakai langsung untuk membuka {@code java.io.File} saat unduhan, tanpa validasi ulang bahwa path masih berada di direktori media yang diharapkan. */
	@Column(name = "path", length = 1000)
	public String getPath() {
		return path;
	}

	/** @param path path absolut baru berkas fisik ini di penyimpanan server. */
	public void setPath(String path) {
		this.path = path;
	}

	/** @return pengajuan laporan tahap yang memiliki berkas lampiran ini (FK wajib, tidak pernah {@code null} pada baris tersimpan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengajuan_tahapan_pelaporan_penelitian_dan_pengabdian", nullable = false)
	public PengajuanTahapanPelaporanPenelitianDanPengabdian getPengajuanTahapanPelaporanPenelitianDanPengabdian() {
		return pengajuanTahapanPelaporanPenelitianDanPengabdian;
	}

	/** @param pengajuanTahapanPelaporanPenelitianDanPengabdian pengajuan laporan tahap yang memiliki berkas lampiran ini. */
	public void setPengajuanTahapanPelaporanPenelitianDanPengabdian(
			PengajuanTahapanPelaporanPenelitianDanPengabdian pengajuanTahapanPelaporanPenelitianDanPengabdian) {
		this.pengajuanTahapanPelaporanPenelitianDanPengabdian = pengajuanTahapanPelaporanPenelitianDanPengabdian;
	}

}
