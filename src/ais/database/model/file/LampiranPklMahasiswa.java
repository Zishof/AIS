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
 * Entity <b>lampiran berkas PKL (Praktik Kerja Lapangan) mahasiswa</b> &mdash; satu baris tabel
 * {@code public.lampiran_pkl_mahasiswa} mewakili satu berkas yang diunggah mahasiswa untuk
 * memenuhi satu butir persyaratan PKL (mis. surat pengantar instansi, laporan akhir PKL, surat
 * keterangan selesai magang, daftar hadir), sebagaimana didefinisikan katalog
 * {@link ais.database.model.pkl.PersyaratanPkl}.
 *
 * <h2>Bukan relasi JPA &mdash; sekadar Long yang menunjuk baris jawaban</h2>
 * <p><b>Penting, karena nama kolom menyesatkan (pola yang berulang di seluruh klaster lampiran
 * akademik ini &mdash; bandingkan {@link LampiranBeasiswaMahasiswa}, {@link LampiranKknMahasiswa}):
 * </b> kolom {@link #getPersyaratanPkl()} <b>bukan</b> {@code @ManyToOne} ke
 * {@link ais.database.model.pkl.PersyaratanPkl} (katalog butir persyaratan program PKL), melainkan
 * {@code Long} polos yang menyimpan id baris {@code ais.database.model.pkl.MahasiswaPklPersyaratan}
 * &mdash; baris "jawaban" milik satu mahasiswa untuk satu butir persyaratan (lihat pemakaian
 * {@code mahasiswaPklPersyaratan.setPersyaratanPkl(...)} di
 * {@code ais.action.master.pkl.PklUntukMahasiswaAction}/{@code KelompokPklAction}). Tidak ada
 * {@code @JoinColumn}/constraint FK yang divalidasi Hibernate di sini; integritas rujukan
 * sepenuhnya bergantung pada disiplin kode pemanggil, bukan pada skema.</p>
 *
 * <h2>Kerangka umum keluarga FileFotoLain</h2>
 * <p>Kelas ini subclass tipis dari {@link FileFotoLain}: seluruh mekanisme unggah/unduh,
 * penyimpanan blob-atau-Google-Drive, dan pembuatan tautan aman ({@code /al?d=...}, lihat
 * {@link FileFotoLain#createLinkUri()}) diwariskan apa adanya. Yang benar-benar spesifik di sini
 * hanya: nama tabel/kolom FK ({@code persyaratan_pkl}), konstanta {@link #DEFAULT_JENIS}, dan
 * pendaftaran kelas ini di peta relasi statis {@code FileFotoLain.RELASI_MAP} (field relasi =
 * {@code "persyaratanPkl"}) supaya mesin generik {@code FileFotoLain.ambil()}/
 * {@code createFileFotoLain()} tahu properti mana yang harus diisi lewat reflection tanpa
 * percabangan if-else per kelas.</p>
 *
 * <h2>Diaudit Envers</h2>
 * <p>{@code @Audited}: setiap INSERT/UPDATE/DELETE baris ini direkam Hibernate Envers ke tabel
 * bayangan {@code lampiran_pkl_mahasiswa_AUD}, kecuali kolom {@link #getFoto()} yang ditandai
 * {@code @NotAudited} (isi berkas biner sengaja tidak digandakan ke riwayat audit &mdash; hanya
 * metadata perubahan yang dilacak).</p>
 *
 * <h2>Celah akses yang diwarisi (bukan sesuatu yang ditambahkan kelas ini)</h2>
 * <p>Berkas entity ini diunduh lewat servlet {@code /al} ({@code AmbilLampiran.process()}), yang
 * TIDAK memverifikasi sesi login maupun kepemilikan baris apa pun &mdash; ia hanya mendekripsi
 * token berisi {@code ref}/{@code jenis}/{@code clazz}/{@code usingId} lalu memanggil
 * {@link FileFotoLain#ambil}. Ini instance dari pola IDOR generik yang sudah tercatat di
 * {@code task_b82b25d2} (celah ada pada mekanisme bersama {@code FileFotoLain}/servlet
 * {@code /al}, dipakai seluruh ~50 subclass {@code FileFoto}/{@code FileFotoLain}, bukan sesuatu
 * yang spesifik ditambahkan {@link LampiranPklMahasiswa}) &mdash; lihat javadoc {@link FileFotoLain}
 * untuk detail mekanismenya.</p>
 *
 * @see FileFotoLain
 * @see ais.database.model.pkl.PersyaratanPkl
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "lampiran_pkl_mahasiswa")
public class LampiranPklMahasiswa extends FileFotoLain {
	private String lokasiSimpan;

	/**
	 * Lokasi simpan mentah cadangan yang diwarisi dari kolom bertipe sama pada {@link FileFoto}.
	 * Pada subclass ini field induk itu DIBAYANGI (di-shadow) oleh field lokal bernama sama, jadi
	 * getter/setter di kelas ini hanya membaca/menulis salinan lokal &mdash; bukan bug baru, pola
	 * ini berulang di seluruh keluarga {@code FileFotoLain}/{@code FileFoto} yang di-generate
	 * hbm2java. Tidak dipetakan {@code @Column}, sehingga nilainya tidak pernah persisten ke basis
	 * data lewat Hibernate; hanya berguna sebagai penampung sementara dalam satu siklus hidup
	 * objek di memori (mis. dipakai proses yang menulis lalu langsung membaca kembali sebelum
	 * entity di-detach).
	 *
	 * @return path lokasi simpan sementara, atau {@code null} bila belum pernah diisi
	 */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/**
	 * Menyetel lokasi simpan sementara di memori. Lihat catatan pada {@link #getLokasiSimpan()}
	 * mengenai field yang di-shadow dan tidak dipetakan ke kolom database.
	 *
	 * @param lokasiSimpan path lokasi simpan yang akan disimpan sementara
	 */
	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	/**
	 * Nilai default kolom {@code jenis} untuk seluruh baris lampiran PKL yang dibuat lewat jalur
	 * generik {@code FileFotoLain}/{@code AmbilDataLampiranFileLain}, dipakai sebagai penanda
	 * kategori berkas saat tidak ada nilai {@code jenis} lain yang lebih spesifik diberikan
	 * pemanggil. Lihat juga {@link #getJenis()} yang SELALU mengembalikan konstanta ini,
	 * mengabaikan nilai kolom {@code jenis} yang sesungguhnya tersimpan di baris (lihat catatan
	 * di sana).
	 */
	public static String DEFAULT_JENIS = "lampiran pkl";

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} lintas deploy;
	 * nilainya sengaja tidak diubah kecuali struktur field yang memengaruhi serialisasi berubah.
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * Identitas (biasanya userid/NIM) pihak yang terakhir membuat/mengubah baris ini, dipakai
	 * untuk jejak audit ringan yang tampil di UI (terpisah dari mekanisme Envers penuh).
	 *
	 * @return id pihak pengunggah/pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pihak pengunggah/pengubah. Nilai kosong atau hanya spasi SENGAJA
	 * diabaikan (method langsung {@code return} tanpa mengubah field) supaya proses simpan-ulang
	 * yang tidak membawa nilai baru tidak menimpa identitas asli yang sudah tercatat dengan
	 * string kosong.
	 *
	 * @param olehId id pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyetel nama tampilan pihak pengunggah/pengubah. Sama seperti {@link #setOlehId(String)},
	 * nilai kosong/{@code null} diabaikan agar tidak menimpa nilai asli dengan string kosong pada
	 * proses simpan-ulang yang tidak membawa nilai baru.
	 *
	 * @param oleh nama pihak pengunggah/pengubah; nilai {@code null}/kosong diabaikan
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
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
	 * Callback lifecycle JPA {@code @PreUpdate}: dijalankan Hibernate otomatis tepat sebelum
	 * statement UPDATE dikirim, memperbarui {@link #tanggal_dirubah} lewat
	 * {@code AuditTimestampInterceptor.ubah(this)} supaya kolom stempel waktu selalu mencerminkan
	 * saat perubahan terakhir tanpa perlu diisi manual oleh setiap pemanggil {@code save/update}.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Menyetel stempel waktu perubahan terakhir secara manual. Nilai defaultnya diisi otomatis
	 * saat objek dibuat ({@code WaktuUtil.getDate()}) dan diperbarui lagi oleh {@link #onUpdate()}
	 * pada setiap UPDATE; setter ini dipakai bila pemanggil perlu menimpa nilai tersebut secara
	 * eksplisit (mis. saat memuat data lama hasil migrasi).
	 *
	 * @param tanggal_dirubah stempel waktu perubahan yang baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu perubahan terakhir baris ini, dipetakan ke kolom timestamp tanpa nama kolom
	 * eksplisit (memakai nama properti {@code tanggal_dirubah} apa adanya, sesuai
	 * {@code MyNamingStrategy} yang tidak mengonversi ke snake_case).
	 *
	 * @return tanggal &amp; waktu perubahan terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi string ringkas baris ini untuk keperluan log/debug: nama berkas apa adanya
	 * (lihat {@link #getNama()}), TANPA memanggil getter (mengakses field {@code nama} langsung),
	 * sehingga bisa mengembalikan {@code null} mentah bila belum pernah diisi atau logika
	 * {@code copyDari} pada getter belum pernah dipicu.
	 *
	 * @return nama berkas apa adanya, bisa {@code null}
	 */
	public String toString() {
		return nama;
	}

	private String nama;
	private String keterangan;
	private Long persyaratanPkl;
	private Blob foto;
	private LampiranPklMahasiswa copyDari;

	private String jenis;
	private String link;

	/**
	 * Nilai kolom {@code jenis} apa adanya (tanpa dipaksa ke {@link #DEFAULT_JENIS} seperti
	 * {@link #getJenis()}). Dipakai lewat kontrak {@link FileFoto#ambilJenis()} oleh kode generik
	 * (mis. {@code FileFoto.berkasCadanganUnik}) yang perlu tahu jenis SEBENARNYA yang tersimpan
	 * di baris, bukan nilai yang selalu sama untuk seluruh baris kelas ini.
	 *
	 * @return nilai field {@code jenis} apa adanya, bisa {@code null}
	 */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/** Konstruktor kosong wajib bagi entity JPA/Hibernate; field diisi lewat setter/reflection. */
	public LampiranPklMahasiswa() {
	}

	/**
	 * Primary key baris ini. {@code insertable = false} karena nilainya SELALU berasal dari
	 * {@code GenerationType.IDENTITY} (kolom serial di PostgreSQL) &mdash; Hibernate tidak pernah
	 * menyertakannya pada statement INSERT, hanya membaca nilai yang dikembalikan database.
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
	 * Menyetel id baris. Pada praktiknya jarang dipanggil manual karena id dibangkitkan basis data
	 * (lihat {@link #getId()}); tersedia untuk kebutuhan Hibernate internal dan skenario memuat
	 * ulang entity dari representasi lain (mis. JSON cache di {@code FileFotoLain.ambil()}).
	 *
	 * @param id id baris yang akan disetel
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Nama berkas asli (termasuk ekstensi) sebagaimana diunggah mahasiswa, dipakai untuk
	 * penamaan berkas fisik di disk ({@code FileFoto.getPathfile()}), judul unduhan, dan deteksi
	 * jenis (gambar/dokumen/dst).
	 *
	 * <p><b>Fallback {@code copyDari}:</b> bila baris ini adalah "salinan" dari baris lain (field
	 * {@link #copyDari} terisi &mdash; dipakai skenario nilai lampiran yang diduplikasi antar
	 * entitas tanpa menggandakan blob-nya, lihat {@code FileFoto.getPathfile()} yang menelusuri
	 * rantai ini), nama yang dikembalikan SELALU nama milik baris sumber, bukan field lokal
	 * (yang bahkan ikut ditimpa sebagai efek samping pemanggilan getter ini).</p>
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
	 * Menyetel nama berkas asli. Lihat {@link #getNama()} untuk perilaku fallback {@code copyDari}
	 * yang dapat menimpa nilai ini saat dibaca kembali.
	 *
	 * @param nama nama berkas (termasuk ekstensi)
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Keterangan bebas terkait berkas ini, biasanya label butir persyaratan PKL yang dipenuhi
	 * (mis. "Surat Pengantar dari Instansi"), ditampilkan sebagai label tombol unduh/hapus di UI
	 * generik {@code FileFotoLain.createDownloadUpload()}.
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
	 * Menyetel isi berkas biner (Large Object PostgreSQL). Lihat catatan penting soal transaksi
	 * non-autocommit pada {@code FileFotoLain.createFileFotoLain()}: kolom ini WAJIB ditulis di
	 * dalam transaksi yang tidak dilepas prematur oleh connection pool.
	 *
	 * @param foto isi berkas biner
	 */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * Isi berkas biner baris ini, dengan dua aturan prioritas: (1) bila kolom {@code gdrive}
	 * terisi, berkas disimpan di Google Drive bukan di database &mdash; method ini SENGAJA
	 * mengembalikan {@code null} supaya pemanggil tidak salah mengira ada blob lokal; (2) bila
	 * baris ini "salinan" ({@link #copyDari} != null), blob milik baris SUMBER yang dikembalikan,
	 * bukan field lokal, untuk menghindari duplikasi penyimpanan blob yang sama berkali-kali.
	 * Ditandai {@code @NotAudited} karena isi biner sengaja tidak digandakan ke tabel bayangan
	 * Envers &mdash; hanya metadata perubahan yang perlu dilacak, bukan seluruh isi berkas
	 * pada tiap revisi.
	 *
	 * @return blob isi berkas, atau {@code null} bila disimpan di Google Drive
	 */
	@NotAudited
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/**
	 * Id baris {@code MahasiswaPklPersyaratan} (jawaban satu mahasiswa atas satu butir
	 * persyaratan PKL) yang dipenuhi lampiran ini. Lihat catatan penting di javadoc kelas: field
	 * ini {@code Long} polos, BUKAN {@code @ManyToOne} ke entity, sehingga tidak ada validasi FK
	 * oleh Hibernate.
	 *
	 * @return id baris jawaban persyaratan PKL yang dirujuk, bisa {@code null}
	 */
	@Column(name = "persyaratan_pkl", nullable = true)
	public Long getPersyaratanPkl() {
		return persyaratanPkl;
	}

	/**
	 * Menyetel rujukan ke baris jawaban persyaratan PKL. Lihat {@link #getPersyaratanPkl()}.
	 *
	 * @param persyaratanPkl id baris {@code MahasiswaPklPersyaratan} yang dirujuk
	 */
	public void setPersyaratanPkl(Long persyaratanPkl) {
		this.persyaratanPkl = persyaratanPkl;
	}

	/**
	 * Baris sumber bila lampiran ini adalah "salinan" &mdash; berbagi blob/nama/gdrive fisik milik
	 * baris lain alih-alih menggandakannya (lihat penjelasan di {@link #getFoto()},
	 * {@link #getNama()}, {@link #getGdrive()}). {@code @NotFound(IGNORE)}: bila baris sumber
	 * sudah terhapus, Hibernate mengembalikan {@code null} alih-alih melempar exception, sehingga
	 * baris "salinan" yang sumbernya hilang tidak menyebabkan seluruh query gagal.
	 *
	 * @return baris sumber salinan, atau {@code null} bila bukan salinan / sumbernya sudah tak ada
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public LampiranPklMahasiswa getCopyDari() {
		return copyDari;
	}

	/**
	 * Menandai baris ini sebagai salinan dari baris {@code copyDari} yang diberikan.
	 *
	 * @param copyDari baris sumber yang blob/metadatanya akan dibagikan
	 */
	public void setCopyDari(LampiranPklMahasiswa copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;

	/**
	 * Id berkas Google Drive tempat isi berkas sesungguhnya disimpan (alternatif penyimpanan
	 * selain blob PostgreSQL). Bila {@link #copyDari} terisi, nilai diambil dari baris sumber.
	 * Bila field lokal kosong, dicoba dibaca lewat {@link ais.database.model.GeneralValueObject#retreive}
	 * (cache/parameter tambahan generik) sebagai fallback sebelum akhirnya mengembalikan field
	 * lokal apa adanya (yang mungkin tetap kosong).
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
	 * Menyetel id berkas Google Drive. Nilai tidak kosong juga disimpan lewat
	 * {@link ais.database.model.GeneralValueObject#put} (mekanisme parameter tambahan generik)
	 * selain ke field lokal, agar konsisten dengan jalur baca fallback di {@link #getGdrive()}.
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
	 * Username akun Google yang dipakai mengunggah ke Drive (untuk ditampilkan sebagai info
	 * kepemilikan berkas di UI). Diwarisi dari {@link #copyDari} bila baris ini salinan.
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
	 * Implementasi kontrak {@link FileFoto#ambilLink()}: mengembalikan tautan eksternal berkas
	 * ini bila lampiran berupa link (bukan berkas unggahan biasa). Lihat {@link #getLink()}.
	 *
	 * @return tautan eksternal berkas, string kosong bila tidak ada
	 */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilRef()}: mengembalikan nilai
	 * {@link #getPersyaratanPkl()} sebagai "acuan" generik yang dipakai mesin
	 * {@code FileFotoLain.ambil()}/{@code hapusAtauUpdate()} untuk mencari/menghapus baris lewat
	 * reflection tanpa tahu nama field aslinya.
	 *
	 * @return id baris jawaban persyaratan PKL yang dirujuk (sama dengan {@link #getPersyaratanPkl()})
	 */
	@Override
	public Long ambilRef() {
		return persyaratanPkl;
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#ambilClazz()}: mengembalikan class runtime baris
	 * ini (bisa berupa proxy Hibernate), dipakai kode generik untuk membangun query/URL yang
	 * type-safe tanpa hardcode nama kelas.
	 *
	 * @return {@code this.getClass()}
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		return this.getClass();
	}

	/**
	 * Implementasi kontrak {@link FileFotoLain#getJenis()}. <b>Catatan perilaku tak lazim:</b>
	 * method ini TIDAK sekadar membaca kolom {@code jenis} &mdash; ia lebih dulu MENIMPA field
	 * {@code jenis} dengan {@link #DEFAULT_JENIS} setiap kali dipanggil, lalu mengembalikan nilai
	 * yang baru saja ditimpa itu. Efeknya, nilai {@code jenis} yang sesungguhnya tersimpan di
	 * baris (jika pernah berbeda) tidak pernah terlihat lewat getter ini; gunakan
	 * {@link #ambilJenis()} bila memerlukan nilai kolom apa adanya.
	 *
	 * @return selalu {@link #DEFAULT_JENIS}
	 */
	@Override
	public String getJenis() {
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	/**
	 * Menyetel field {@code jenis} secara langsung. Perhatikan bahwa {@link #getJenis()} akan
	 * menimpa nilai ini kembali ke {@link #DEFAULT_JENIS} pada pemanggilan berikutnya; setter ini
	 * efektif hanya bila dibaca lewat {@link #ambilJenis()} atau sebelum {@link #getJenis()}
	 * pernah dipanggil.
	 *
	 * @param jenis nilai jenis yang akan disetel
	 */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * Tautan eksternal (mis. URL Google Photos/Drive berbagi) bila lampiran ini berupa link
	 * alih-alih berkas unggahan biasa. Dipakai {@code getNama()} pada beberapa subclass sejenis
	 * ({@link LampiranLainMahasiswa}, dst) untuk menandai nama tampilan "Berupa link file".
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
	 * sekali lewat {@link FileFotoLain#createLinkUri()} lalu di-cache pada field transient
	 * {@link #url} untuk pemanggilan berikutnya dalam siklus hidup objek yang sama. <b>Perhatikan:</b>
	 * berbeda dari subclass sejenis ({@link LampiranBeasiswaMahasiswa}, {@link LampiranKknMahasiswa})
	 * yang menambahkan guard {@code url == null && getId() != null} sebelum memanggil
	 * {@code createLinkUri()}, method ini pada kelas PKL TIDAK memiliki guard tersebut &mdash;
	 * setiap pemanggilan getter SELALU membangun ulang URI, termasuk untuk baris yang belum
	 * pernah disimpan ({@code getId() == null}), yang berpotensi memicu pembuatan tautan/berkas
	 * cadangan untuk id yang belum valid. Exception dicatat ke {@code ErrorAuditUtil} dan
	 * ditelan supaya kegagalan satu baris tidak menghentikan render grid lampiran.
	 *
	 * @return URL unduhan/tampilan berkas, atau {@code null} bila gagal membangun tautan
	 */
	@Transient
	public String getUrl() {
		try {
			url = createLinkUri();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/LampiranPklMahasiswa.java:240");
		}
		return url;
	}

	/**
	 * Menyetel URL secara langsung, memotong jalur pembentukan otomatis di {@link #getUrl()} untuk
	 * pemanggilan berikutnya (karena {@code getUrl()} pada kelas ini tidak memiliki guard
	 * {@code url == null}, nilai yang disetel di sini tetap akan DITIMPA pada pemanggilan
	 * {@link #getUrl()} berikutnya).
	 *
	 * @param url URL yang akan disetel
	 */
	public void setUrl(String url) {
		this.url = url;
	}
}
