package ais.database.model.file;

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
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Foto profil {@code AnggotaKoperasi} MANDIRI — member POS yang tidak
 * ditautkan ke siswa/mahasiswa/pengguna (31-08, laporan galat
 * {@code anggota_foto_upload}). Pola persis {@link FotoSiswa}: satu tabel
 * foto profil per jenis subjek, dibaca/ditulis lewat mesin
 * {@code ProfileImageUtil}/{@code FileFotoLain} yang sama sehingga tidak ada
 * silo media khusus aplikasi desktop. Member yang PUNYA tautan sivitas tetap
 * memakai tabel foto entitas tautannya (perilaku lama tidak berubah).
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "foto_anggota_koperasi")
public class FotoAnggotaKoperasi extends FileFotoLain {

	private static final long serialVersionUID = 1L;

	/** Nilai default {@link #getJenis()} untuk seluruh baris kelas ini: {@code "foto anggota koperasi"}. */
	public static String DEFAULT_JENIS = "foto anggota koperasi";

	/** Primary key baris (identity, di-generate database). */
	private Long id;
	/** Nama pengguna/pelaku yang membuat atau terakhir mengubah baris ini (audit jejak, boleh kosong). */
	private String oleh;
	/** Id pengguna (oleh) yang membuat/mengubah baris ini (audit jejak, boleh kosong). */
	private String olehId;
	/** Nama berkas foto, juga dipakai sebagai kunci tampilan/unduhan. */
	private String nama;
	/** Keterangan/deskripsi bebas untuk baris ini. */
	private String keterangan;
	/** Id {@code koperasi.anggota_koperasi} pemilik foto; lihat {@link #getAnggotaKoperasi()}. */
	private Long anggotaKoperasi;
	/** Isi biner foto (PostgreSQL Large Object), kosong bila disimpan di {@link #gdrive}. */
	private Blob foto;
	/** Baris sumber untuk pola salin-tanpa-duplikasi; lihat {@link #getCopyDari()}. */
	private FotoAnggotaKoperasi copyDari;
	/** Tautan eksternal (link) opsional untuk foto ini; lihat {@link #getLink()}. */
	private String link;
	/** Nilai "jenis" baris ini, selalu direset ke {@link #DEFAULT_JENIS} oleh {@link #getJenis()}. */
	private String jenis;
	/** Path/lokasi penyimpanan berkas fisik di disk untuk baris ini (override lokal, lihat {@link FileFoto#getLokasiSimpan()}). */
	private String lokasiSimpan;
	/** Id berkas pada Google Drive bila foto disimpan eksternal (bukan sebagai blob database). */
	private String gdrive;
	/** Nama akun/username Google Drive yang menyimpan berkas (informasional). */
	private String gdriveUsername;

	/** Konstruktor default (dibutuhkan Hibernate untuk instansiasi via refleksi). */
	public FotoAnggotaKoperasi() {
	}

	/**
	 * Callback JPA {@code @PreUpdate}: dipanggil otomatis oleh Hibernate tepat sebelum UPDATE
	 * dieksekusi, mendelegasikan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}
	 * untuk memperbarui stempel waktu perubahan secara konsisten di seluruh entitas.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/** Stempel waktu perubahan terakhir baris ini, diinisialisasi saat objek dibuat. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Mengatur stempel waktu perubahan terakhir baris ini. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return stempel waktu perubahan terakhir baris ini (kolom timestamp, diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return {@link #nama} berkas sebagai representasi string baris ini. */
	public String toString() {
		return nama;
	}

	/** @return {@link #id primary key} baris ini. */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** Mengatur {@link #id primary key} baris ini. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return {@link #olehId id pelaku} audit jejak baris ini. */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengatur {@link #olehId id pelaku} audit jejak. Nilai {@code null}/kosong-blank
	 * SENGAJA diabaikan (guard di badan method) agar id pelaku yang sudah tercatat tidak
	 * tertimpa oleh pemanggil yang lupa mengisi/menyertakan nilai kosong.
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** @return {@link #oleh nama pelaku} audit jejak baris ini. */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Mengatur {@link #oleh nama pelaku} audit jejak. Nilai {@code null}/kosong-blank
	 * SENGAJA diabaikan (guard di badan method), simetris dengan {@link #setOlehId(String)}.
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * @return {@link #nama nama berkas}, di-trim. Bila baris ini merupakan salinan
	 *         ({@link #copyDari} tidak {@code null}), nilai disegarkan lebih dulu dari sumber
	 *         salinan agar tampilan selalu mengikuti data terbaru sumber.
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/** Mengatur {@link #nama nama berkas}. */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return {@link #keterangan}. Getter ini TIDAK menyegarkan nilai dari {@link #copyDari}. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** Mengatur {@link #keterangan}. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return isi biner foto, atau {@code null} bila baris ini disimpan di Google Drive
	 *         ({@link #getGdrive()} terisi -- isinya tidak ada di basis data). Bila bukan
	 *         salinan, foto diambil dari kolom {@link #foto} baris ini sendiri; bila merupakan
	 *         salinan ({@link #copyDari} tidak {@code null}), foto didelegasikan ke
	 *         {@code copyDari.foto} supaya blob tidak diduplikasi. Ditandai {@link NotAudited}
	 *         karena isi biner tidak praktis disalin ke tabel riwayat Envers.
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/** Mengatur isi biner {@link #foto foto}. */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/** Id {@code koperasi.anggota_koperasi} pemilik foto. */
	@Column(name = "anggota_koperasi", nullable = true)
	public Long getAnggotaKoperasi() {
		return anggotaKoperasi;
	}

	/** Mengatur {@link #anggotaKoperasi id anggota koperasi} pemilik foto ini. */
	public void setAnggotaKoperasi(Long anggotaKoperasi) {
		this.anggotaKoperasi = anggotaKoperasi;
	}

	/**
	 * @return baris {@link FotoAnggotaKoperasi} sumber bila baris ini adalah salinan (copy)
	 *         yang berbagi berkas fisik/blob dengan baris lain -- lihat mekanisme umum di
	 *         {@code FileFoto#berkasMilikBarisIni(java.io.File)}. Relasi {@code @ManyToOne}
	 *         dengan {@code cascade PERSIST/MERGE} dan {@code @NotFound(IGNORE)} sehingga baris
	 *         sumber yang sudah terhapus tidak melemparkan exception, cukup dianggap tidak ada.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public FotoAnggotaKoperasi getCopyDari() {
		return copyDari;
	}

	/** Mengatur {@link #copyDari baris sumber salinan}. */
	public void setCopyDari(FotoAnggotaKoperasi copyDari) {
		this.copyDari = copyDari;
	}

	/** @return {@link #lokasiSimpan lokasi penyimpanan} berkas fisik baris ini. */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/** Mengatur {@link #lokasiSimpan lokasi penyimpanan} berkas fisik baris ini. */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/**
	 * @return {@link #gdrive id Google Drive}. Bila baris ini salinan, nilai disegarkan dari
	 *         {@link #copyDari} lebih dulu. Bila field lokal kosong, dicoba diambil ulang lewat
	 *         {@code retreive("gdrive")} (mekanisme pemulihan nilai dari {@code
	 *         GeneralValueObject}) sebelum jatuh kembali ke nilai field asli.
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Mengatur {@link #gdrive id Google Drive}. Nilai yang tidak kosong juga disimpan lewat
	 * {@code put(gdrive, "gdrive")} (mekanisme penyimpanan nilai tambahan {@code
	 * GeneralValueObject}), selain diisikan ke field lokal.
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * @return {@link #gdriveUsername}, disegarkan dari {@link #copyDari} lebih dulu bila baris
	 *         ini merupakan salinan.
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/** Mengatur {@link #gdriveUsername}. */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/**
	 * {@inheritDoc}
	 * @return {@link #jenis} -- berbeda dari {@link #getJenis()} (yang selalu mereset ke
	 *         {@link #DEFAULT_JENIS}), method kontrak {@link FileFotoLain#ambilJenis()} ini
	 *         hanya membaca nilai field apa adanya tanpa reset.
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * {@inheritDoc}
	 * @return {@code this.getClass()} -- kelas entitas ini sendiri (bukan kelas induk terkait
	 *         seperti pada {@link FotoGambarProduk#ambilClazz()}), karena baris kelas ini
	 *         berdiri sendiri (foto anggota koperasi mandiri) tanpa tabel induk sivitas yang
	 *         perlu dirujuk balik.
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		return this.getClass();
	}

	/**
	 * {@inheritDoc}
	 * @return selalu {@link #DEFAULT_JENIS} -- getter ini SENGAJA mereset field {@link #jenis}
	 *         ke {@link #DEFAULT_JENIS} setiap dipanggil (bukan sekadar membaca nilai yang
	 *         tersimpan), sehingga baris kelas ini tidak pernah punya "jenis" lain meskipun
	 *         {@link #setJenis(String)} pernah dipanggil dengan nilai berbeda.
	 */
	@Override
	@Column(name = "jenis", length = 30)
	public String getJenis() {
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	/**
	 * Mengatur {@link #jenis}. Lihat catatan pada {@link #getJenis()}: nilai yang diset di sini
	 * akan ditimpa kembali ke {@link #DEFAULT_JENIS} pada pemanggilan {@link #getJenis()} berikutnya.
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/** @return {@link #link}, atau string kosong bila belum diisi (tidak pernah {@code null}), di-trim. */
	public String getLink() {
		return link == null ? "" : link.trim();
	}

	/** Mengatur {@link #link}. */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * {@inheritDoc}
	 * <p>Mengembalikan {@link #getAnggotaKoperasi()} sebagai referensi entitas induk.</p>
	 */
	@Override
	public Long ambilRef() {
		return anggotaKoperasi;
	}

	/**
	 * {@inheritDoc}
	 * <p>Mengembalikan {@link #getLink()} -- kelas ini (subclass {@link FileFotoLain}) mendukung
	 * representasi tautan eksternal untuk foto anggota koperasi.</p>
	 */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/** Tautan servlet lampiran yang di-cache sekali per instance; lihat {@link #getUrl()}. */
	private String url;

	/**
	 * @return {@link #url}, dibangun sekali (di-cache pada field {@link #url}) lewat
	 *         {@link FileFotoLain#createLinkUri()} saat pertama dipanggil dan baris ini sudah
	 *         punya {@link #getId()}. Ditandai {@code @Transient} sehingga TIDAK dipetakan ke
	 *         kolom database -- murni nilai turunan untuk kebutuhan tampilan. Kegagalan
	 *         membangun tautan (exception apa pun dari {@code createLinkUri()}) dicatat ke
	 *         {@link ais.common.ErrorAuditUtil} dan method ini tetap mengembalikan {@link #url}
	 *         apa adanya (bisa {@code null}) alih-alih melempar exception ke pemanggil.
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/database/model/file/FotoAnggotaKoperasi.java:getUrl");
		}
		return url;
	}

	/** Mengatur {@link #url} (override manual, melewati mekanisme cache-otomatis di {@link #getUrl()}). */
	public void setUrl(String url) {
		this.url = url;
	}
}
