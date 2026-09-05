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
import javax.persistence.Transient;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * Entity <b>lampiran berkas KKN (Kuliah Kerja Nyata) mahasiswa</b> &mdash; satu baris tabel
 * {@code public.lampiran_kkn_mahasiswa} mewakili satu berkas yang diunggah mahasiswa untuk
 * memenuhi satu butir persyaratan KKN (mis. surat izin orang tua, surat keterangan sehat, laporan
 * akhir KKN) sebagaimana didefinisikan katalog {@code ais.database.model.kkn.PersyaratanKkn}.
 *
 * <h2>Bukan relasi JPA &mdash; sekadar Long yang menunjuk baris jawaban</h2>
 * <p><b>Penting, karena nama kolom menyesatkan (pola yang berulang di seluruh klaster lampiran
 * akademik ini &mdash; bandingkan {@link LampiranPklMahasiswa}, {@link LampiranBeasiswaMahasiswa}):
 * </b> kolom {@link #getPersyaratanKkn()} <b>bukan</b> {@code @ManyToOne} ke
 * {@code ais.database.model.kkn.PersyaratanKkn} (katalog butir persyaratan program KKN), melainkan
 * {@code Long} polos yang menyimpan id baris {@code ais.database.model.kkn.MahasiswaKknPersyaratan}
 * &mdash; baris "jawaban" milik satu mahasiswa untuk satu butir persyaratan (lihat pemakaian
 * {@code mahasiswaKknPersyaratan.setPersyaratanKkn(...)} di
 * {@code ais.action.master.kkn.KknUntukMahasiswaAction}/{@code KelompokKknAction}). Tidak ada
 * {@code @JoinColumn}/constraint FK yang divalidasi Hibernate di sini; integritas rujukan
 * sepenuhnya bergantung pada disiplin kode pemanggil, bukan pada skema.</p>
 *
 * <h2>Kerangka umum keluarga FileFotoLain</h2>
 * <p>Kelas ini subclass tipis dari {@link FileFotoLain}: seluruh mekanisme unggah/unduh,
 * penyimpanan blob-atau-Google-Drive, dan pembuatan tautan aman ({@code /al?d=...}, lihat
 * {@link FileFotoLain#createLinkUri()}) diwariskan apa adanya. Yang benar-benar spesifik di sini
 * hanya: nama tabel/kolom FK ({@code persyaratan_kkn}), konstanta {@link #DEFAULT_JENIS}, dan
 * pendaftaran kelas ini di peta relasi statis {@code FileFotoLain.RELASI_MAP} (field relasi =
 * {@code "persyaratanKkn"}).</p>
 *
 * <h2>Diaudit Envers</h2>
 * <p>{@code @Audited}: setiap INSERT/UPDATE/DELETE baris ini direkam Hibernate Envers ke tabel
 * bayangan {@code lampiran_kkn_mahasiswa_AUD}, kecuali kolom {@link #getFoto()} yang ditandai
 * {@code @NotAudited} (isi berkas biner sengaja tidak digandakan ke riwayat audit).</p>
 *
 * <h2>Celah akses yang diwarisi (bukan sesuatu yang ditambahkan kelas ini)</h2>
 * <p>Berkas entity ini diunduh lewat servlet {@code /al} ({@code AmbilLampiran.process()}), yang
 * TIDAK memverifikasi sesi login maupun kepemilikan baris apa pun. Ini instance dari pola IDOR
 * generik yang sudah tercatat di {@code task_b82b25d2} (celah pada mekanisme bersama
 * {@link FileFotoLain}/servlet {@code /al}, dipakai seluruh ~50 subclass sejenis, bukan sesuatu
 * yang spesifik ditambahkan {@link LampiranKknMahasiswa}).</p>
 *
 * @see FileFotoLain
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "lampiran_kkn_mahasiswa")
public class LampiranKknMahasiswa extends FileFotoLain {
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
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Identitas (biasanya userid/NIM) pihak yang terakhir membuat/mengubah baris ini.
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
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pihak pengunggah/pengubah; nilai kosong/{@code null} diabaikan
	 * (lihat {@link #setOlehId(String)}).
	 *
	 * @param oleh nama pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampilan pihak yang terakhir membuat/mengubah baris ini.
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

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
	 * Representasi string ringkas baris ini untuk log/debug: nama berkas apa adanya (field
	 * {@code nama} langsung, tanpa fallback {@code copyDari} milik getter).
	 *
	 * @return nama berkas apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private Long persyaratanKkn;
	private Blob foto;
	private LampiranKknMahasiswa copyDari;

	private String jenis;
	private String link;

	/**
	 * Nilai kolom {@code jenis} apa adanya (tanpa dipaksa ke {@link #DEFAULT_JENIS} seperti
	 * {@link #getJenis()}), sesuai kontrak {@link FileFoto#ambilJenis()}.
	 *
	 * @return nilai field {@code jenis} apa adanya, bisa {@code null}
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * Nilai default kolom {@code jenis} untuk seluruh baris lampiran KKN yang dibuat lewat jalur
	 * generik {@code FileFotoLain}/{@code AmbilDataLampiranFileLain}. Lihat {@link #getJenis()}
	 * yang selalu mengembalikan konstanta ini, mengabaikan nilai kolom sesungguhnya.
	 */
	public static String DEFAULT_JENIS = "lampiran kkn";

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public LampiranKknMahasiswa() {
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
	 * Nama berkas asli (termasuk ekstensi) sebagaimana diunggah mahasiswa.
	 *
	 * <p><b>Fallback {@code copyDari}:</b> bila baris ini "salinan" dari baris lain, nama yang
	 * dikembalikan SELALU nama milik baris sumber (lihat catatan sama pada
	 * {@link LampiranPklMahasiswa#getNama()}).</p>
	 *
	 * @return nama berkas (di-trim), atau {@code null} bila field {@code nama} maupun
	 *         {@code copyDari.nama} sama-sama kosong
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Menyetel nama berkas asli.
	 *
	 * @param nama nama berkas (termasuk ekstensi)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas terkait berkas ini, biasanya label butir persyaratan KKN yang dipenuhi.
	 *
	 * @return keterangan berkas, bisa {@code null}
	 */
	@Column(name = "keterangan", nullable = true)
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
	 * Menyetel isi berkas biner (Large Object PostgreSQL).
	 *
	 * @param foto isi berkas biner
	 */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * Isi berkas biner baris ini: {@code null} bila disimpan di Google Drive
	 * ({@link #getGdrive()} terisi), atau blob milik baris sumber bila baris ini "salinan".
	 * Ditandai {@code @NotAudited} agar isi biner tidak digandakan ke tabel bayangan Envers.
	 *
	 * @return blob isi berkas, atau {@code null} bila disimpan di Google Drive
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/**
	 * Id baris {@code MahasiswaKknPersyaratan} (jawaban satu mahasiswa atas satu butir
	 * persyaratan KKN) yang dipenuhi lampiran ini. Lihat catatan penting di javadoc kelas: field
	 * ini {@code Long} polos, BUKAN {@code @ManyToOne} ke entity.
	 *
	 * @return id baris jawaban persyaratan KKN yang dirujuk, bisa {@code null}
	 */
	@Column(name = "persyaratan_kkn", nullable = true)
	public Long getPersyaratanKkn() {
		return persyaratanKkn;
	}

	/**
	 * Menyetel rujukan ke baris jawaban persyaratan KKN. Lihat {@link #getPersyaratanKkn()}.
	 *
	 * @param persyaratanKkn id baris {@code MahasiswaKknPersyaratan} yang dirujuk
	 */
	public void setPersyaratanKkn(Long persyaratanKkn) {
		this.persyaratanKkn = persyaratanKkn;
	}

	/**
	 * Baris sumber bila lampiran ini "salinan" &mdash; berbagi blob/nama/gdrive fisik milik baris
	 * lain. {@code @NotFound(IGNORE)}: bila baris sumber sudah terhapus, dikembalikan {@code null}
	 * alih-alih melempar exception.
	 *
	 * @return baris sumber salinan, atau {@code null} bila bukan salinan / sumbernya sudah tak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public LampiranKknMahasiswa getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang blob/metadatanya akan dibagikan
	 */
	public void setCopyDari(LampiranKknMahasiswa copyDari) {
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
	 * Implementasi kontrak {@link FileFoto#ambilLink()}: tautan eksternal berkas ini bila
	 * lampiran berupa link. Lihat {@link #getLink()}.
	 *
	 * @return tautan eksternal berkas, string kosong bila tidak ada
	 */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilRef()}: mengembalikan {@link #getPersyaratanKkn()}
	 * sebagai "acuan" generik dipakai mesin {@code FileFotoLain.ambil()}/{@code hapusAtauUpdate()}.
	 *
	 * @return id baris jawaban persyaratan KKN yang dirujuk
	 */
	@Override
	public Long ambilRef() {
		return persyaratanKkn;
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilClazz()}: class runtime baris ini.
	 *
	 * @return {@code this.getClass()}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		return this.getClass();
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#getJenis()}. Sama seperti
	 * {@link LampiranPklMahasiswa#getJenis()}: SELALU menimpa field {@code jenis} dengan
	 * {@link #DEFAULT_JENIS} sebelum mengembalikannya. Gunakan {@link #ambilJenis()} untuk nilai
	 * kolom apa adanya.
	 *
	 * @return selalu {@link #DEFAULT_JENIS}
	 */
	@Override
	public String getJenis() {
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	/**
	 * Menyetel field {@code jenis} secara langsung; akan ditimpa kembali ke
	 * {@link #DEFAULT_JENIS} pada pemanggilan {@link #getJenis()} berikutnya.
	 *
	 * @param jenis nilai jenis yang akan disetel
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Tautan eksternal (mis. URL berbagi) bila lampiran berupa link, bukan berkas unggahan.
	 *
	 * @return tautan (di-trim), string kosong bila tidak ada (tidak pernah {@code null})
	 */
	public String getLink() {
		return link == null ? "" : link.trim();
	}

	/**
	 * Menyetel tautan eksternal berkas.
	 *
	 * @param link tautan eksternal yang akan disimpan
	 */
	public void setLink(String link) {
		this.link = link;
	}

	private String url;

	/**
	 * URL siap pakai untuk mengunduh/menampilkan berkas ini lewat servlet {@code /al}, dibangun
	 * sekali lewat {@link FileFotoLain#createLinkUri()} lalu di-cache di field {@link #url}.
	 * Memiliki guard {@code url == null && getId() != null} (sama seperti
	 * {@link LampiranBeasiswaMahasiswa#getUrl()}) sehingga tautan hanya dibangun sekali dan hanya
	 * untuk baris yang sudah tersimpan. Exception dicatat ke {@code ErrorAuditUtil} dan ditelan
	 * supaya kegagalan satu baris tidak menghentikan render grid lampiran.
	 *
	 * @return URL unduhan/tampilan berkas, atau {@code null} bila {@code getId() == null} atau
	 *         gagal membangun tautan
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/LampiranKknMahasiswa.java:250");
		}
		return url;
	}

	/**
	 * Menyetel URL secara langsung; nilai ini akan DIPERTAHANKAN oleh {@link #getUrl()} pada
	 * pemanggilan berikutnya (tidak ditimpa) karena guard {@code url == null} pada getter.
	 *
	 * @param url URL yang akan disetel
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
