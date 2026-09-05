package ais.database.model.file;

// Generated May 15, 2010 10:07:50 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.sql.Blob;
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
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity <b>berkas mentah unggahan impor massal biodata calon mahasiswa</b> &mdash; satu baris
 * tabel {@code public.upload_biodata_calon_mahasiswa_file_content} menyimpan ISI BERKAS (blob)
 * dari satu file impor (mis. Excel/CSV hasil ekspor SPAN-PTKIN &mdash; sistem seleksi nasional
 * perguruan tinggi keagamaan Islam negeri, lihat pemakai
 * {@code ais.action.master.UploadBiodataCalonMahasiswaSPANPTKINAction}) yang diunggah admin PMB
 * untuk MEMBUAT BANYAK baris {@link ais.database.model.UploadBiodataCalonMahasiswa} (dan pada
 * gilirannya banyak baris {@code BiodataCalonMahasiswa}) sekaligus lewat proses parsing/impor
 * batch.
 *
 * <h2>Berbeda total dari klaster lampiran per-mahasiswa (file 1-9 paket ini)</h2>
 * <p>Seluruh kelas lain di paket ini (PKL/Beasiswa/KKN/lampiran-lain mahasiswa &amp; calon
 * mahasiswa) menyimpan SATU dokumen milik SATU mahasiswa/calon mahasiswa untuk keperluan
 * administratif perorangan. Kelas ini sebaliknya menyimpan SATU berkas impor massal (spreadsheet)
 * yang menjadi SUMBER DATA bagi banyak baris {@code BiodataCalonMahasiswa} sekaligus &mdash;
 * perannya sebagai "lampiran file konten unggahan batch", sejenis (dari sisi peran arsitektural)
 * dengan {@code TugasFileContent}/{@code PertemuanFileContent} yang juga terdaftar di
 * {@code FileFotoLain.RELASI_MAP} dengan field relasi {@code "id"}, BUKAN dengan klaster lampiran
 * per-orang. Berbeda dari kedua kelas itu, kelas ini bahkan tidak terdaftar di
 * {@code FileFotoLain.RELASI_MAP} sama sekali (turun langsung dari {@link FileFoto}, bukan
 * {@link FileFotoLain}) karena tidak pernah diakses lewat mesin generik
 * {@code FileFotoLain.ambil()} &mdash; hanya diquery langsung lewat {@link #getRef()} oleh
 * {@code UploadBiodataCalonMahasiswaSPANPTKINAction} (lihat {@link #getRef()}).</p>
 *
 * <h2>Rujukan {@link #getRef()}: Long polos ke baris log impor, bukan ke calon mahasiswa</h2>
 * <p><b>Penting, karena mudah disalahpahami dari nama kelasnya:</b> kolom {@link #getRef()}
 * menunjuk ke id baris {@link ais.database.model.UploadBiodataCalonMahasiswa} (satu baris = satu
 * SESI/JOB impor batch, membawa kolom seperti {@code keterangan}/{@code peringatan} hasil
 * parsing), <b>BUKAN</b> ke id {@code BiodataCalonMahasiswa} perorangan manapun. Satu baris kelas
 * ini melahirkan BANYAK baris {@code BiodataCalonMahasiswa} lewat proses parsing, bukan
 * sebaliknya. Tidak ada {@code @JoinColumn}/constraint FK di sini; rujukan sepenuhnya konvensi
 * kode pemanggil (lihat {@code Restrictions.eq("ref", uploadBiodataCalonMahasiswa.getId())} di
 * {@code UploadBiodataCalonMahasiswaSPANPTKINAction}).</p>
 *
 * <h2>Bukan subclass FileFotoLain</h2>
 * <p>Turun langsung dari {@link FileFoto}: {@link #ambilJenis()} dan {@link #ambilLink()} SELALU
 * mengembalikan {@code null}, dan berkas kelas ini diunduh/diproses ulang lewat pemanggilan
 * langsung {@link FileFoto#ambilFile()}/{@link Filedownload#save} di Action pemanggil, BUKAN
 * lewat servlet generik {@code /al}.</p>
 *
 * <h2>Diaudit Envers</h2>
 * <p>{@code @Audited}: setiap INSERT/UPDATE/DELETE direkam ke tabel bayangan
 * {@code upload_biodata_calon_mahasiswa_file_content_AUD}, kecuali {@link #getFoto()} yang
 * {@code @NotAudited}.</p>
 *
 * @see FileFoto
 * @see ais.database.model.UploadBiodataCalonMahasiswa
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "upload_biodata_calon_mahasiswa_file_content")

public class UploadBiodataCalonMahasiswaFileContent extends FileFoto {
	private String lokasiSimpan;

	/**
	 * Lokasi simpan mentah cadangan; lihat catatan pada {@link LampiranPklMahasiswa#getLokasiSimpan()}
	 * mengenai field yang di-shadow dari {@link FileFoto} dan tidak dipetakan ke kolom database.
	 *
	 * @return path lokasi simpan sementara, atau {@code null} bila belum pernah diisi
	 */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/**
	 * Menyetel lokasi simpan sementara di memori (tidak persisten ke basis data).
	 *
	 * @param lokasiSimpan path lokasi simpan yang akan disimpan sementara
	 */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 8396956558947881938L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Identitas (biasanya userid admin PMB) pihak yang terakhir mengunggah/mengubah baris ini.
	 *
	 * @return id pihak pengunggah/pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pihak pengunggah/pengubah. Nilai kosong/{@code null} SENGAJA diabaikan
	 * agar simpan-ulang yang tidak membawa nilai baru tidak menimpa identitas asli.
	 *
	 * @param olehId id pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pihak pengunggah/pengubah; nilai kosong/{@code null} diabaikan
	 * (lihat {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * Nama tampilan pihak yang terakhir mengunggah/mengubah baris ini.
	 *
	 * @return nama pengunggah/pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Callback lifecycle JPA {@code @PreUpdate}, memperbarui {@link #tanggal_dirubah} otomatis
	 * sebelum UPDATE lewat {@code AuditTimestampInterceptor.ubah(this)}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini.
	 *
	 * @return tanggal &amp; waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string ringkas baris ini untuk log/debug: gabungan nama berkas dan tipe MIME.
	 *
	 * @return {@code nama + "_" + fileMimeType}
	 */
	public String toString() {
		return nama + "_" + fileMimeType;
	}

	private Long ref;
	private Blob foto;
	private String nama;
	private String keterangan;
	private String fileMimeType;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();
	private UploadBiodataCalonMahasiswaFileContent copyDari;

	/**
	 * Implementasi kontrak {@link FileFoto#ambilJenis()}. Kelas ini tidak memiliki konsep
	 * "jenis lampiran" (satu baris {@code UploadBiodataCalonMahasiswa} hanya punya satu berkas
	 * impor terkait), sehingga SELALU mengembalikan {@code null}.
	 *
	 * @return selalu {@code null}
	 */
	@Override
	public String ambilJenis() {
		return null;
	}

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public UploadBiodataCalonMahasiswaFileContent() {
	}

	/**
	 * Primary key baris ini. {@code insertable = false} karena nilainya SELALU berasal dari
	 * {@code GenerationType.IDENTITY} (kolom serial PostgreSQL).
	 *
	 * @return id baris, {@code null} sebelum baris pernah disimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel id baris; jarang dipanggil manual karena id dibangkitkan basis data.
	 *
	 * @param id id baris yang akan disetel
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Id baris {@link ais.database.model.UploadBiodataCalonMahasiswa} (satu sesi/job impor batch)
	 * yang berkas mentahnya disimpan baris ini. Lihat catatan penting di javadoc kelas: field ini
	 * {@code Long} polos TANPA {@code @JoinColumn}/constraint FK, dan menunjuk ke baris LOG IMPOR,
	 * <b>BUKAN</b> ke {@code BiodataCalonMahasiswa} perorangan manapun.
	 *
	 * @return id baris {@code UploadBiodataCalonMahasiswa} pemilik berkas impor ini, bisa
	 *         {@code null}
	 */
	@Column(name = "ref")
	public Long getRef() {
		return this.ref;
	}

	/**
	 * Menyetel rujukan ke baris log impor. Lihat {@link #getRef()}.
	 *
	 * @param ref id baris {@code UploadBiodataCalonMahasiswa} yang dirujuk
	 */
	public void setRef(Long ref) {
		this.ref = ref;
	}

	/**
	 * Menyetel tipe MIME berkas impor (mis. {@code application/vnd.ms-excel}).
	 *
	 * @param fileMimeType tipe MIME berkas
	 */
	public void setFileMimeType(String fileMimeType) {
		this.fileMimeType = fileMimeType;
	}

	/**
	 * Tipe MIME berkas impor, dengan fallback ke baris {@link #copyDari} bila baris ini "salinan".
	 * Dipakai pemanggil (mis. {@code UploadBiodataCalonMahasiswaSPANPTKINAction}) sebagai
	 * parameter kedua {@code Filedownload.save(file, mimeType)} saat admin mengunduh ulang berkas
	 * impor yang pernah diunggah.
	 *
	 * @return tipe MIME berkas impor, bisa {@code null}
	 */
	@Column(name = "file_mime_tipe", length = 255)
	public String getFileMimeType() {
		if (copyDari != null) {
			fileMimeType = copyDari.fileMimeType;
		}
		return fileMimeType;
	}

	/**
	 * Menyetel stempel waktu unggah berkas impor.
	 *
	 * @param uploadDate tanggal &amp; waktu unggah yang baru
	 */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/**
	 * Stempel waktu unggah berkas impor (berbeda dari {@link #getTanggal_dirubah()} yang mencatat
	 * perubahan terakhir).
	 *
	 * @return tanggal &amp; waktu unggah
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	/**
	 * Menyetel nama berkas impor asli (parameter bernama {@code realFile} mengisi field
	 * {@code nama}, dipetakan ke kolom {@code real_file}, kontrak {@link FileFoto#setNama(String)}).
	 *
	 * @param realFile nama berkas impor asli (termasuk ekstensi)
	 */
	public void setNama(String realFile) {
		this.nama = realFile;
	}

	/**
	 * Nama berkas impor asli, dengan fallback ke baris {@link #copyDari} bila baris ini "salinan".
	 *
	 * @return nama berkas impor, bisa {@code null}
	 */
	@Column(name = "real_file", length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return nama;
	}

	/**
	 * Menyetel isi berkas biner (Large Object PostgreSQL), dipetakan ke kolom {@code filecontent}.
	 *
	 * @param foto isi berkas biner (spreadsheet impor)
	 */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * Isi berkas biner baris ini (spreadsheet impor mentah): {@code null} bila disimpan di Google
	 * Drive ({@link #getGdrive()} terisi), atau blob milik baris sumber bila baris ini "salinan".
	 * Ditandai {@code @NotAudited} agar isi biner (berpotensi besar) tidak digandakan ke tabel
	 * bayangan Envers pada tiap revisi.
	 *
	 * @return blob isi berkas impor, atau {@code null} bila disimpan di Google Drive
	 */
	@NotAudited
	@Column(name = "filecontent")
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/**
	 * Baris sumber bila lampiran ini "salinan" &mdash; berbagi blob/nama/mimeType/gdrive fisik
	 * milik baris lain. {@code @NotFound(IGNORE)}: bila baris sumber sudah terhapus, dikembalikan
	 * {@code null} alih-alih melempar exception.
	 *
	 * @return baris sumber salinan, atau {@code null} bila bukan salinan / sumbernya sudah tak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public UploadBiodataCalonMahasiswaFileContent getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang blob/metadatanya akan dibagikan
	 */
	public void setCopyDari(UploadBiodataCalonMahasiswaFileContent copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

	/**
	 * Id berkas Google Drive tempat isi berkas sesungguhnya disimpan. Lihat penjelasan lengkap
	 * pada {@link LampiranPklMahasiswa#getGdrive()} mengenai fallback {@code copyDari} dan
	 * {@code retreive("gdrive")}.
	 *
	 * @return id berkas Google Drive, atau {@code null}/kosong bila berkas disimpan sebagai blob
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menyetel id berkas Google Drive; nilai tidak kosong juga disimpan lewat {@code put()} agar
	 * konsisten dengan jalur baca fallback di {@link #getGdrive()}.
	 *
	 * @param gdrive id berkas Google Drive; nilai kosong tidak dipropagasi ke {@code put()}
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * Username akun Google yang dipakai mengunggah ke Drive. Diwarisi dari {@link #copyDari} bila
	 * baris ini salinan.
	 *
	 * @return username akun Google pengunggah, bisa {@code null}
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/**
	 * Menyetel username akun Google Drive pengunggah.
	 *
	 * @param gdriveUsername username akun Google
	 */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/**
	 * Implementasi kontrak {@link FileFoto#ambilLink()}. Kelas ini tidak punya kolom {@code link},
	 * sehingga SELALU mengembalikan {@code null}.
	 *
	 * @return selalu {@code null}
	 */
	@Override
	public String ambilLink() {
		return null;
	}

	/**
	 * Implementasi kontrak {@link FileFoto#ambilRef()}: mengembalikan {@link #getRef()} apa
	 * adanya. Lihat catatan penting di javadoc kelas dan {@link #getRef()}: nilai ini menunjuk ke
	 * baris log impor {@code UploadBiodataCalonMahasiswa}, bukan ke calon mahasiswa perorangan.
	 *
	 * @return id baris {@code UploadBiodataCalonMahasiswa} yang dirujuk (sama dengan
	 *         {@link #getRef()})
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return ref;
	}

	/**
	 * Keterangan bebas terkait berkas impor ini. <b>Catatan:</b> berbeda dari pola umum kelas
	 * sejenis di paket ini (mis. {@link LampiranPklMahasiswa#getKeterangan()}) yang mengembalikan
	 * {@code null} apa adanya, getter ini mengembalikan string kosong sebagai pengganti
	 * {@code null} &mdash; pemanggil yang langsung memakai nilai baliknya (mis. konkatenasi
	 * string) tidak perlu menjaga terhadap {@code null} untuk field ini secara khusus.
	 *
	 * @return keterangan berkas, string kosong bila belum pernah diisi (tidak pernah {@code null})
	 */
	public String getKeterangan() {
		return keterangan == null ? "" : keterangan;
	}

	/**
	 * Menyetel keterangan berkas impor.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}
}
