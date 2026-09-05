package ais.database.model.file;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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

import ais.database.model.penelitiandanpengabdian.PengumumanPenelitian;

/**
 * Entity <b>lampiran berkas pengumuman penelitian</b> &mdash; satu baris tabel
 * {@code public.lampiran_pengumuman_penelitian} mewakili satu berkas yang dilekatkan pada satu
 * baris {@link ais.database.model.penelitiandanpengabdian.PengumumanPenelitian} (mis. poster
 * pengumuman, PDF panduan pengajuan proposal, formulir yang harus diisi dosen/mahasiswa).
 *
 * <h2>Konten institusional, bukan dokumen pribadi</h2>
 * <p>Berbeda dari klaster lampiran mahasiswa (file 1-3 paket ini) yang membawa dokumen pribadi
 * per-orang, berkas di kelas ini adalah materi pengumuman yang SATU baris dibaca banyak orang
 * (seluruh dosen/mahasiswa yang berhak melihat pengumuman penelitian). Tidak ada gagasan
 * "kepemilikan per-pengguna" yang relevan di sini; siapa saja yang berhak melihat pengumumannya
 * berhak pula melihat lampirannya.</p>
 *
 * <h2>Relasi FK nyata, bukan subclass FileFotoLain</h2>
 * <p>Sama seperti {@link LampiranPenelitianDanPengabdian}, kelas ini memakai {@code @ManyToOne}
 * sungguhan ke {@link ais.database.model.penelitiandanpengabdian.PengumumanPenelitian} lewat
 * {@link #getPengumumanPenelitian()} ({@code nullable = false}, divalidasi FK oleh basis data),
 * dan turun langsung dari {@link FileFoto} (bukan {@link FileFotoLain}) sehingga TIDAK melewati
 * servlet generik {@code /al} maupun peta relasi statis {@code FileFotoLain.RELASI_MAP}.
 * {@link #ambilJenis()} dan {@link #ambilLink()} SELALU mengembalikan {@code null}.</p>
 *
 * <h2>Diaudit Envers: TIDAK</h2>
 * <p>Kelas ini TIDAK memiliki anotasi {@code @Audited} &mdash; perubahan pada baris lampiran
 * pengumuman penelitian tidak direkam Hibernate Envers.</p>
 *
 * @see FileFoto
 * @see ais.database.model.penelitiandanpengabdian.PengumumanPenelitian
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Table(schema = "public", name = "lampiran_pengumuman_penelitian")

public class LampiranPengumumanPenelitian extends FileFoto {
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
	private static final long serialVersionUID = 2463812577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Identitas (biasanya userid) pihak yang terakhir mengunggah/mengubah baris ini.
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
	 * @return {@code fileName + "_" + mimeType}
	 */
	public String toString() {
		return fileName + "_" + mimeType;
	}

	private Blob foto;
	private String mimeType;
	private String fileName;
	private String keterangan;
	private PengumumanPenelitian pengumumanPenelitian;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();
	private LampiranPengumumanPenelitian copyDari;

	/**
	 * Implementasi kontrak {@link FileFoto#ambilJenis()}. Kelas ini tidak memiliki konsep
	 * "jenis lampiran" berjamak, sehingga SELALU mengembalikan {@code null}.
	 *
	 * @return selalu {@code null}
	 */
	@Override
	public String ambilJenis() {
		return null;
	}

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public LampiranPengumumanPenelitian() {
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
	 * Keterangan bebas terkait berkas ini.
	 *
	 * @return keterangan berkas, bisa {@code null}
	 */
	@Column(name = "keterangan", length = 1000)
	public String getKeterangan() {
		return this.keterangan;
	}

	/**
	 * Menyetel keterangan berkas.
	 *
	 * @param keterangan keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menyetel isi berkas biner (Large Object PostgreSQL), dipetakan ke kolom {@code file_content}.
	 *
	 * @param foto isi berkas biner
	 */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * Isi berkas biner baris ini: {@code null} bila disimpan di Google Drive
	 * ({@link #getGdrive()} terisi), atau blob milik baris sumber bila baris ini "salinan".
	 * Tidak ditandai {@code @NotAudited} karena kelas ini memang tidak {@code @Audited}.
	 *
	 * @return blob isi berkas, atau {@code null} bila disimpan di Google Drive
	 */
	@Column(name = "file_content")
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/**
	 * Menyetel tipe MIME berkas.
	 *
	 * @param mimeType tipe MIME (mis. {@code application/pdf})
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Tipe MIME berkas, dengan fallback ke baris {@link #copyDari} bila baris ini "salinan"
	 * (berbeda dari {@link LampiranPenelitianDanPengabdian#getMimeType()} yang tidak memiliki
	 * fallback ini).
	 *
	 * @return tipe MIME berkas, bisa {@code null}
	 */
	@Column(name = "mime_type", length = 255)
	public String getMimeType() {
		if (copyDari != null) {
			mimeType = copyDari.mimeType;
		}
		return mimeType;
	}

	/**
	 * Menyetel nama berkas asli (parameter bernama {@code fileName} mengisi field {@code fileName},
	 * kontrak {@link FileFoto#setNama(String)}).
	 *
	 * @param fileName nama berkas (termasuk ekstensi)
	 */
	public void setNama(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Nama berkas asli, dengan fallback ke baris {@link #copyDari} bila baris ini "salinan".
	 *
	 * @return nama berkas, bisa {@code null}
	 */
	@Column(name = "file_name", length = 255)
	public String getNama() {
		if (copyDari != null) {
			fileName = copyDari.fileName;
		}
		return fileName;
	}

	/**
	 * Menyetel baris {@link ais.database.model.penelitiandanpengabdian.PengumumanPenelitian}
	 * pemilik lampiran ini.
	 *
	 * @param pengumumanPenelitian baris pengumuman penelitian pemilik lampiran
	 */
	public void setPengumumanPenelitian(PengumumanPenelitian pengumumanPenelitian) {
		this.pengumumanPenelitian = pengumumanPenelitian;
	}

	/**
	 * Baris {@link ais.database.model.penelitiandanpengabdian.PengumumanPenelitian} pemilik
	 * lampiran ini. Relasi {@code @ManyToOne} sungguhan dengan {@code @JoinColumn(nullable = false)}
	 * &mdash; divalidasi FK oleh basis data.
	 *
	 * @return baris pengumuman penelitian pemilik lampiran, tidak pernah {@code null} pada baris
	 *         yang tersimpan dengan benar
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pengumuman_penelitian", nullable = false)
	public PengumumanPenelitian getPengumumanPenelitian() {
		return pengumumanPenelitian;
	}

	/**
	 * Stempel waktu unggah berkas (berbeda dari {@link #getTanggal_dirubah()} yang mencatat
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
	 * Menyetel stempel waktu unggah berkas.
	 *
	 * @param uploadDate tanggal &amp; waktu unggah yang baru
	 */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
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
	public LampiranPengumumanPenelitian getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang blob/metadatanya akan dibagikan
	 */
	public void setCopyDari(LampiranPengumumanPenelitian copyDari) {
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
	 * Implementasi kontrak {@link FileFoto#ambilRef()}: mengembalikan id baris
	 * {@link ais.database.model.penelitiandanpengabdian.PengumumanPenelitian} pemilik lampiran
	 * ini, atau {@code null} bila relasi belum/tidak ter-set.
	 *
	 * @return id {@link #getPengumumanPenelitian()}, atau {@code null}
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return getPengumumanPenelitian() == null ? null : getPengumumanPenelitian().getId();
	}
}
