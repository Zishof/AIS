package ais.database.model.file;

// Generated May 15, 2010 10:07:50 AM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.util.Date;
import java.util.TreeMap;

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

import org.apache.commons.io.FileUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.zkoss.zul.Label;

import ais.common.Common;
import ais.common.MateriDanKomentarHelper;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;

/**
 * Berkas/lampiran satu {@link Pertemuan} (materi kuliah/pengajaran) -- tabel
 * {@code pertemuan_file_content}, subclass {@link FileFotoLain}. Berbeda dari kebanyakan
 * subclass {@link FileFotoLain} lain, kelas ini TIDAK punya kolom acuan pemilik terpisah:
 * {@code RELASI_MAP} pada {@link FileFotoLain} mendaftarkan kelas ini dengan nama field
 * {@code "id"}, sehingga {@link #ambilRef()} sengaja mengembalikan primary key baris ini
 * sendiri ({@link #getId()}), BUKAN {@link #getPertemuan()} -- konsisten dengan {@code
 * TugasFileContent}, {@code AudioPertemuan}, dan {@code VideoPertemuan} yang didaftarkan pada
 * golongan yang sama. Akibatnya penyaringan berbasis {@code jenis} pada {@link
 * FileFotoLain#ambil} dimatikan untuk golongan ini, dan penghapusan non-{@code usingId} tidak
 * melakukan apa pun (lihat Javadoc {@code RELASI_MAP} dan {@code SOFT_DELETE_ID} pada
 * {@link FileFotoLain}).
 *
 * <p><b>Materi berupa tautan, bukan berkas.</b> Selain berkas biner ({@link #getFoto()}), satu
 * baris juga bisa berupa {@link #getLink()} eksternal (mis. Google Drive, OneDrive, YouTube).
 * {@link #getKeterangan()} secara lazy mengambil judul halaman tautan tersebut lewat request
 * HTTP singkat (timeout 3 detik) hanya ketika keterangan belum diisi -- lihat catatan
 * keamanan/keandalan pada Javadoc method itu.</p>
 *
 * <p><b>Google Drive sebagai sumber alternatif.</b> {@link #getGdrive()}/{@link
 * #getGdriveUsername()} TIDAK dipetakan sebagai kolom JPA; nilainya disimpan lewat cache berkas
 * per-instance {@link ais.database.model.GeneralValueObject#put(String, String) put}/{@link
 * ais.database.model.GeneralValueObject#retreive(String) retreive} milik {@code
 * GeneralValueObject} induk. Selama {@link #getGdrive()} terisi, {@link #getFoto()} sengaja
 * mengembalikan {@code null} sebagai pertanda berkas asli harus diambil dari Google Drive.</p>
 *
 * <p><b>Baris "copy".</b> {@link #getCopyDari()} adalah asosiasi opsional ke baris
 * {@code PertemuanFileContent} lain; ketika terisi, {@link #getNama()}, {@link #getFoto()},
 * {@link #getLink()}, {@link #getType()}, {@link #getFileMimeType()}, dan {@link
 * #getGoogleBook()} membaca nilainya dari baris sumber tersebut -- pola berbagi satu berkas
 * fisik/tautan di antara banyak baris tanpa menduplikasi blob, sama seperti subclass {@link
 * FileFoto} lain.</p>
 *
 * @see Pertemuan
 * @see FileFotoLain
 * @see TugasFileContent
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "pertemuan_file_content")
public class PertemuanFileContent extends FileFotoLain {
	/**
	 * Path/lokasi penyimpanan lokal baris ini. Field ini MENIMPA (shadow) field privat sejenis di
	 * {@link FileFoto}: tidak diberi anotasi JPA ({@code @Column}), jadi bukan kolom ter-mapping --
	 * getter/setter di sini hanya menyediakan state in-memory milik baris.
	 */
	private String lokasiSimpan;

	/** @return {@link #lokasiSimpan}, path penyimpanan lokal baris ini (bukan kolom database). */
	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	/** @param lokasiSimpan path penyimpanan lokal baru untuk baris ini. */
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

	/** @return id pengguna (String) yang mengunggah/mengubah baris ini, atau {@code null}. */
	public String getOlehId() {
		return olehId;
	}

	/** Menetapkan id pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/** Menetapkan nama pengunggah; nilai {@code null} atau kosong-setelah-trim diabaikan (field lama tidak ditimpa). */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
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
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir baris ini; lihat {@link #onUpdate()}. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** @return gabungan {@link #nama} dan {@link #fileMimeType} dipisah garis bawah; bisa memuat literal {@code "null"} bila salah satu belum diisi. */
	public String toString() {
		return nama + "_" + fileMimeType;
	}

	private Long pertemuan;
	private Long grupPertemuan;
	private Long kurikulumPunyaMatakuliah;
	private Long kurikulumPunyaMatakuliahDetail;
	private Blob foto;
	private String nama;
	private String keterangan;
	private String googleBook;
	private String lokasiFisik;
	/** Nilai default {@link #getJenis()}/{@link #jenis}: penanda jenis lampiran untuk baris materi pertemuan. */
	public static String DEFAULT_JENIS = "pertemuan file";

	/**
	 * Mengekstrak paket SCORM (berkas {@code .zip}) ke direktori kerja tetap di bawah
	 * {@code Common.REAL_PATH} bila direktori itu belum ada, memakai nama berkas (tanpa
	 * ekstensi {@code .zip}, spasi diganti {@code _}) sebagai penanda. Sumber zip diambil dari
	 * {@link #getLokasiFisik()} bila berkasnya ada di disk, atau lewat {@link
	 * FileFoto#ambilFileDariCacheAtauDisk() ambilFile()} sebagai fallback. Kegagalan (I/O,
	 * ekstraksi) dicatat lewat {@code ErrorAuditUtil} dan tidak dilempar ke pemanggil.
	 *
	 * @param pertemuanFileContent baris materi yang lokasi fisiknya akan diperiksa/diekstrak;
	 *                             method ini tidak melakukan apa pun bila {@code null} atau
	 *                             {@link #getLokasiFisik()}-nya kosong.
	 */
	public static void chekScrom(PertemuanFileContent pertemuanFileContent) {
		String fisik = pertemuanFileContent == null ? null : pertemuanFileContent.getLokasiFisik();
		if (fisik != null) {
			System.out.println("Lokasi Fisik -> " + fisik);
			try {
				String lokasi = "/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/f/s/2/s/s/e/e/w/scorm/"
						+ pertemuanFileContent.getNama().replace(".zip", "").replaceAll(" ", "_");

				String path1 = Common.REAL_PATH + lokasi;
				File dir = new File(path1);
				System.out.println("path1 -> " + path1 + ", ada -> " + dir.exists());
				if (!dir.exists()) {
					try {
						FileUtils.deleteDirectory(dir);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/PertemuanFileContent.java:138");

					}
					dir.mkdirs();

					File fileZip = new File(pertemuanFileContent.getLokasiFisik());

					if (!fileZip.exists()) {
						fileZip = pertemuanFileContent.ambilFile();
					}

					Path source = Paths.get(fileZip.getAbsolutePath());
					Path target = Paths.get(dir.getAbsolutePath());

					Common.unzipFolder(source, target);
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/PertemuanFileContent.java:155");
			}
		}
	}

	/**
	 * Pembungkus tipis yang meneruskan seluruh pekerjaan ke {@link
	 * MateriDanKomentarHelper#ambilKomentar}: mengumpulkan komentar atas materi sekumpulan
	 * {@link Pertemuan}, disaring berdasarkan peran penulis komentar (dosen/mahasiswa/guru/
	 * siswa/admin) dan kata kunci pencarian.
	 *
	 * @param pertemuans    peta label tampilan &rarr; id {@link Pertemuan} yang komentarnya
	 *                      dikumpulkan.
	 * @param refresh       bila {@code true}, memaksa pengambilan ulang tanpa memakai cache.
	 * @param dosenBol      sertakan komentar dari dosen.
	 * @param mahasiswaBol  sertakan komentar dari mahasiswa.
	 * @param guruBol       sertakan komentar dari guru.
	 * @param siswaBol      sertakan komentar dari siswa.
	 * @param adminBol      sertakan komentar dari admin.
	 * @param cari          kata kunci pencarian, atau kosong/{@code null} untuk semua komentar.
	 * @param label         komponen ZK yang diperbarui helper untuk menampilkan progres/hasil.
	 * @return peta hasil dari {@link MateriDanKomentarHelper#ambilKomentar}.
	 */
	public static TreeMap<String, Object[]> ambilKomentar(TreeMap<String, Long> pertemuans, boolean refresh,
			boolean dosenBol, boolean mahasiswaBol, boolean guruBol, boolean siswaBol, boolean adminBol, String cari,
			Label label) {
		return MateriDanKomentarHelper.ambilKomentar(pertemuans, refresh, dosenBol, mahasiswaBol, guruBol, siswaBol,
				adminBol, cari, label);
	}

	/**
	 * Pintasan {@link #ambilMateri(TreeMap, boolean, Label, boolean, Tbmuser)} dengan
	 * {@code urutBerdasarkanNama = false} (urutan default, biasanya berdasarkan waktu unggah).
	 *
	 * @param pertemuans peta label tampilan &rarr; id {@link Pertemuan} yang materinya dikumpulkan.
	 * @param refresh    bila {@code true}, memaksa pengambilan ulang tanpa memakai cache.
	 * @param label      komponen ZK yang diperbarui helper untuk menampilkan progres/hasil.
	 * @param tbmuser    pengguna yang mengakses, dipakai helper untuk penyaringan otorisasi.
	 * @return peta hasil dari {@link MateriDanKomentarHelper#ambilMateri}.
	 */
	public static TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			Tbmuser tbmuser) {
		return MateriDanKomentarHelper.ambilMateri(pertemuans, refresh, label, false, tbmuser);
	}

	/**
	 * Pembungkus tipis yang meneruskan seluruh pekerjaan ke {@link
	 * MateriDanKomentarHelper#ambilMateri}: mengumpulkan materi ({@code PertemuanFileContent})
	 * milik sekumpulan {@link Pertemuan}.
	 *
	 * @param pertemuans          peta label tampilan &rarr; id {@link Pertemuan} yang materinya
	 *                            dikumpulkan.
	 * @param refresh             bila {@code true}, memaksa pengambilan ulang tanpa memakai
	 *                            cache.
	 * @param label               komponen ZK yang diperbarui helper untuk menampilkan
	 *                            progres/hasil.
	 * @param urutBerdasarkanNama bila {@code true}, hasil diurutkan menurut nama materi alih-alih
	 *                            urutan default.
	 * @param tbmuser             pengguna yang mengakses, dipakai helper untuk penyaringan
	 *                            otorisasi.
	 * @return peta hasil dari {@link MateriDanKomentarHelper#ambilMateri}.
	 */
	public static TreeMap<String, Object[]> ambilMateri(TreeMap<String, Long> pertemuans, boolean refresh, Label label,
			boolean urutBerdasarkanNama, Tbmuser tbmuser) {
		return MateriDanKomentarHelper.ambilMateri(pertemuans, refresh, label, urutBerdasarkanNama, tbmuser);
	}

	/** @return {@link #jenis} apa adanya (bisa {@code null} bila {@link #getJenis()} belum pernah dipanggil sebelumnya). */
	@Override
	public String ambilJenis() {
		return jenis;
	}

	/**
	 * Mengembalikan keterangan materi ini, mengisi lazy dari judul halaman {@link #getLink()}
	 * bila keterangan belum diisi (kosong, atau masih literal {@code "link"}) dan tautannya
	 * berawalan {@code http}.
	 *
	 * <h4>Jalur cepat untuk cloud drive</h4>
	 * <p>Tautan Google Drive/OneDrive langsung diberi label tetap ({@code "Google Drive"}/
	 * {@code "OneDrive"}) TANPA request jaringan -- host tersebut lazim menolak permintaan bot
	 * (403/404) dan judul halamannya tidak dibutuhkan untuk menyimpan materi.</p>
	 *
	 * <h4>Request HTTP dari dalam getter entity</h4>
	 * <p>Untuk tautan lain, method ini MEMBUKA KONEKSI JARINGAN ({@code URL.openConnection()},
	 * timeout koneksi & baca masing-masing 3 detik) langsung dari getter entity -- efek samping
	 * yang tidak lazim untuk getter, tetapi sengaja dibiarkan karena metadata tautan ini
	 * bersifat opsional: kegagalan apa pun (host mati, timeout, bot-block) ditangkap dan
	 * digantikan label fallback {@code "Link materi"}, tautannya sendiri tetap tersimpan dan
	 * dapat dibuka. Bila content-type respons berupa teks/XML/HTML, judul {@code <title>}
	 * halaman diambil lewat Jsoup; jika tidak (mis. PDF/berkas biner), nama berkas diambil dari
	 * potongan terakhir path URL lewat {@link #namaFileDariLink(String)}.</p>
	 *
	 * <p>Hasil yang berhasil diambil DITULIS KEMBALI ke field {@link #keterangan} sebagai efek
	 * samping (bukan murni fungsi baca), sehingga pemanggilan berikutnya tidak mengulang request
	 * jaringan yang sama.</p>
	 *
	 * @return keterangan (di-trim), atau string kosong bila tidak pernah berhasil diisi.
	 */
	public String getKeterangan() {
		java.io.InputStream input = null;
		try {
			if ((keterangan == null || keterangan.trim().isEmpty() || keterangan.trim().equalsIgnoreCase("link"))
					&& link != null && link.toLowerCase().startsWith("http")) {
				String linkKecil = link.toLowerCase();
				// URL berbagi cloud memang lazim menolak request server (403/404) dan judulnya
				// tidak dibutuhkan untuk menyimpan materi. Jangan lakukan I/O jaringan dari getter
				// entity yang juga dipanggil Hibernate/serialisasi JSON.
				if (linkKecil.indexOf("drive.google.com/") >= 0) {
					keterangan = "Google Drive";
					return keterangan;
				}
				if (linkKecil.indexOf("onedrive.live.com/") >= 0 || linkKecil.indexOf("1drv.ms/") >= 0) {
					keterangan = "OneDrive";
					return keterangan;
				}
				java.net.URLConnection conn = new URL(link).openConnection();
				conn.setConnectTimeout(3000);
				conn.setReadTimeout(3000);
				String contentType = conn.getContentType();
				if (contentType == null || contentType.toLowerCase().startsWith("text/")
						|| contentType.toLowerCase().indexOf("xml") >= 0
						|| contentType.toLowerCase().indexOf("html") >= 0) {
					input = conn.getInputStream();
					Document doc = Jsoup.parse(input, "UTF-8", link);
					keterangan = doc.select("title").text();
				} else {
					keterangan = namaFileDariLink(link);
				}
			}
		} catch (Exception e) {
			keterangan = "Link materi";
			// Metadata tautan bersifat opsional. Tautan tetap disimpan/dapat dibuka walau
			// host menolak bot, sedang offline, atau melewati batas waktu.
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception ignored) {
				}
			}
		}
		return keterangan == null ? "" : keterangan.trim();
	}

	/**
	 * Mengambil nama berkas dari potongan terakhir path sebuah URL (setelah {@code /} terakhir),
	 * di-decode sebagai URL-encoding UTF-8. Dipakai {@link #getKeterangan()} sebagai fallback
	 * saat content-type respons bukan teks/HTML.
	 *
	 * @param link URL sumber.
	 * @return nama berkas hasil decode, atau {@code "Link materi"} bila path kosong/tidak dapat
	 *         di-parse.
	 */
	private String namaFileDariLink(String link) {
		try {
			String path = new URL(link).getPath();
			if (path != null && path.lastIndexOf('/') >= 0) {
				String nama = path.substring(path.lastIndexOf('/') + 1);
				if (nama != null && nama.trim().length() > 0) {
					return java.net.URLDecoder.decode(nama, "UTF-8");
				}
			}
		} catch (Exception e) {
		}
		return "Link materi";
	}

	/** @param keterangan keterangan materi ini; lihat {@link #getKeterangan()} untuk pengisian lazy-nya. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	private String fileMimeType;
	private Date uploadDate = ais.ui.util.WaktuUtil.getDate();
	private String link;
	private String type;

	private PertemuanFileContent copyDari;

	/** Konstruktor default (dipakai Hibernate). */
	public PertemuanFileContent() {
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
	 * Menetapkan tipe konten baris ini. Bila {@link #copyDari} terisi, parameter yang diterima
	 * DIABAIKAN dan digantikan nilai dari baris sumber sebelum ditulis ke field -- berbeda dari
	 * kebanyakan setter subclass {@link FileFoto} lain yang menyalin field {@code copyDari} pada
	 * GETTER, bukan pada setter.
	 *
	 * @param type tipe konten baru; efektif hanya dipakai bila {@link #copyDari} kosong.
	 */
	public void setType(String type) {
		if (copyDari != null) {
			type = copyDari.type;
		}
		this.type = type;
	}

	/** @return tipe konten baris ini, atau string kosong (bukan {@code null}) bila belum diisi. */
	public String getType() {
		return type == null ? "" : type;
	}

	/**
	 * @return tautan materi eksternal (di-trim), atau {@code null} bila kosong. Bila
	 *         {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari tautan baris sumber.
	 *         Bila {@link #getLokasiFisik()} terisi (materi memiliki berkas fisik lokal) DAN
	 *         tautannya mengandung spasi, spasi tersebut diganti {@code _} sebagai efek samping
	 *         getter -- penyesuaian agar tautan tetap valid dipakai sebagai referensi berkas.
	 */
	@Column(columnDefinition = "text", nullable = true)
	public String getLink() {
		if (copyDari != null) {
			link = copyDari.link;
		}

		if (lokasiFisik != null && !lokasiFisik.trim().isEmpty()) {
			if (link != null && !link.trim().isEmpty() && link.trim().contains(" ")) {
				link = link.trim().replaceAll(" ", "_");
			}
		}

		return link == null || link.trim().isEmpty() ? null : link.trim();
	}

	/** @param link tautan materi eksternal. */
	public void setLink(String link) {
		this.link = link;
	}

	/**
	 * Memuat entity {@link Pertemuan} pemilik baris ini lewat cache/lookup generik {@link
	 * GeneralValueObject#ambilData(Class, String)} berdasarkan {@link #getPertemuan()}.
	 *
	 * @return {@link Pertemuan} pemilik baris ini, atau {@code null} bila {@link #getPertemuan()}
	 *         kosong atau baris pertemuannya sudah tidak ada.
	 */
	public Pertemuan ambilPertemuan() {
		return getPertemuan() == null ? null
				: (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, getPertemuan().toString());
	}

	/**
	 * @return id {@link Pertemuan} pemilik baris ini. CATATAN: berbeda dari {@link #ambilRef()}
	 *         (yang mengembalikan {@link #getId()} sendiri sesuai golongan {@code RELASI_MAP}
	 *         {@code "id"} pada {@link FileFotoLain}), kolom ini TETAP ada dan tetap dipakai
	 *         {@link #ambilPertemuan()} untuk memuat entity induk -- hanya tidak dipakai sebagai
	 *         kunci pencarian lampiran generik {@link FileFotoLain#ambil}.
	 */
	@Column(name = "pertemuan")
	public Long getPertemuan() {
		return this.pertemuan;
	}

	/** @param pertemuan id {@link Pertemuan} pemilik baris ini. */
	public void setPertemuan(Long pertemuan) {
		this.pertemuan = pertemuan;
	}

	/** @param fileMimeType mime-type berkas ini. */
	public void setFileMimeType(String fileMimeType) {
		this.fileMimeType = fileMimeType;
	}

	/**
	 * @return mime-type berkas ini, atau {@code null} bila belum diisi. Bila {@link #copyDari}
	 *         terisi, nilainya disegarkan lebih dulu dari mime-type baris sumber.
	 */
	@Column(name = "file_mime_tipe", length = 255)
	public String getFileMimeType() {
		if (copyDari != null) {
			fileMimeType = copyDari.fileMimeType;
		}
		return fileMimeType;
	}

	/** @param uploadDate waktu unggah berkas ini. */
	public void setUploadDate(Date uploadDate) {
		this.uploadDate = uploadDate;
	}

	/** @return waktu unggah berkas ini, diinisialisasi ke waktu sekarang saat objek dibuat. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_upload", nullable = false, length = 0)
	public Date getUploadDate() {
		return uploadDate;
	}

	/** @param realFile nama berkas asli (disimpan ke kolom {@code real_file}). */
	public void setNama(String realFile) {
		this.nama = realFile;
	}

	/**
	 * @return nama berkas (di-trim), atau string kosong (bukan {@code null}) bila belum diisi.
	 *         Bila {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari nama baris
	 *         sumber. Dipetakan ke kolom {@code real_file}.
	 */
	@Column(name = "real_file", length = 255)
	public String getNama() {
		if (copyDari != null) {
			nama = copyDari.nama;
		}
		return nama == null ? "" : nama.trim();
	}

	/** @param foto isi biner berkas ini. */
	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	/**
	 * @return {@code null} bila {@link #getGdrive()} terisi (berkas asli ada di Google Drive, bukan
	 *         di kolom ini); jika tidak, blob milik {@link #copyDari} bila terisi, atau blob baris
	 *         ini sendiri. Dipetakan ke kolom {@code filecontent} dan tidak diaudit
	 *         ({@code @NotAudited}) karena isi biner tidak perlu dilacak riwayatnya oleh Envers.
	 */
	@NotAudited
	@Column(name = "filecontent")
	public Blob getFoto() {
		return gdrive != null && !gdrive.trim().isEmpty() ? null : (copyDari == null ? foto : copyDari.foto);
	}

	/** @return id kurikulum-punya-matakuliah terkait materi ini, atau {@code null}. */
	public Long getKurikulumPunyaMatakuliah() {
		return kurikulumPunyaMatakuliah;
	}

	/** @param kurikulumPunyaMatakuliah id kurikulum-punya-matakuliah terkait materi ini. */
	public void setKurikulumPunyaMatakuliah(Long kurikulumPunyaMatakuliah) {
		this.kurikulumPunyaMatakuliah = kurikulumPunyaMatakuliah;
	}

	/** @return id detail kurikulum-punya-matakuliah terkait materi ini, atau {@code null}. */
	public Long getKurikulumPunyaMatakuliahDetail() {
		return kurikulumPunyaMatakuliahDetail;
	}

	/** @param kurikulumPunyaMatakuliahDetail id detail kurikulum-punya-matakuliah terkait materi ini. */
	public void setKurikulumPunyaMatakuliahDetail(Long kurikulumPunyaMatakuliahDetail) {
		this.kurikulumPunyaMatakuliahDetail = kurikulumPunyaMatakuliahDetail;
	}

	/** @return id grup pertemuan terkait (mis. kelas paralel), atau {@code null}. */
	public Long getGrupPertemuan() {
		return grupPertemuan;
	}

	/** @param grupPertemuan id grup pertemuan terkait. */
	public void setGrupPertemuan(Long grupPertemuan) {
		this.grupPertemuan = grupPertemuan;
	}

	/**
	 * @return baris {@code PertemuanFileContent} sumber bila baris ini adalah "copy" yang
	 *         berbagi berkas fisik/tautan dengan baris lain; {@code null} bila baris ini berdiri
	 *         sendiri. {@code NotFoundAction.IGNORE} membuat asosiasi yang menunjuk baris yang
	 *         sudah terhapus diperlakukan sebagai {@code null}, bukan melempar exception.
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinColumn(name = "copy_dari", nullable = true)
	public PertemuanFileContent getCopyDari() {
		return copyDari;
	}

	/** @param copyDari baris sumber untuk berbagi berkas fisik/tautan (lihat {@link #getCopyDari()}). */
	public void setCopyDari(PertemuanFileContent copyDari) {
		this.copyDari = copyDari;
	}

	private String gdrive;
	private String gdriveUsername;
	private String jenis;
	private String syaratAkses;

	/**
	 * @return URL Google Drive tempat berkas sesungguhnya disimpan, atau {@code null} bila berkas
	 *         disimpan sebagai blob biasa. Bukan kolom JPA -- nilainya dibaca dari cache berkas
	 *         per-instance {@link ais.database.model.GeneralValueObject#retreive(String)
	 *         retreive("gdrive")} milik induk, dengan penyegaran lebih dulu dari {@link #copyDari}
	 *         bila terisi.
	 */
	public String getGdrive() {
		if (copyDari != null) {
			gdrive = copyDari.gdrive;
		}
		String s = gdrive == null || gdrive.trim().isEmpty() ? retreive("gdrive") : gdrive;
		return s != null && !s.trim().isEmpty() ? s : gdrive;
	}

	/**
	 * Menetapkan URL Google Drive berkas ini. Nilai tidak kosong ditulis ke cache berkas
	 * per-instance lewat {@link ais.database.model.GeneralValueObject#put(String, String)
	 * put(gdrive, "gdrive")} milik induk -- BUKAN ke kolom database.
	 *
	 * @param gdrive URL Google Drive baru; {@code null}/kosong tidak ditulis ke cache (hanya
	 *               mengubah field in-memory).
	 */
	public void setGdrive(String gdrive) {
		if (gdrive != null && !gdrive.trim().isEmpty()) {
			put(gdrive, "gdrive");
		}
		this.gdrive = gdrive;
	}

	/**
	 * @return nama pengguna akun Google Drive terkait, disegarkan lebih dulu dari {@link
	 *         #copyDari} bila terisi. Bukan kolom JPA -- murni field in-memory.
	 */
	public String getGdriveUsername() {
		if (copyDari != null) {
			gdriveUsername = copyDari.gdriveUsername;
		}
		return gdriveUsername;
	}

	/** @param gdriveUsername nama pengguna akun Google Drive terkait. */
	public void setGdriveUsername(String gdriveUsername) {
		this.gdriveUsername = gdriveUsername;
	}

	/** @return {@link #getLink()}, tautan materi eksternal ini. */
	@Override
	public String ambilLink() {
		return getLink();
	}

	/**
	 * @return metadata Google Book terkait materi ini (kolom {@code text}, JSON/teks bebas), atau
	 *         string kosong bila belum diisi. Bila {@link #copyDari} terisi, nilainya disegarkan
	 *         lebih dulu dari baris sumber.
	 */
	@Column(name = "google_book", columnDefinition = "text")
	public String getGoogleBook() {
		if (copyDari != null) {
			googleBook = copyDari.googleBook;
		}
		return googleBook == null ? "" : googleBook.trim();
	}

	/** @param googleBook metadata Google Book terkait materi ini. */
	public void setGoogleBook(String googleBook) {
		this.googleBook = googleBook;
	}

	/**
	 * @return {@link #getId()}, primary key baris ini sendiri -- BUKAN {@link #getPertemuan()}.
	 *         Sengaja demikian: {@code RELASI_MAP} pada {@link FileFotoLain} mendaftarkan kelas
	 *         ini dengan nama field {@code "id"} (golongan ketiga: entity tanpa kolom acuan
	 *         pemilik terpisah, {@code ref} dicocokkan langsung ke primary key), konsisten dengan
	 *         {@code TugasFileContent}/{@code AudioPertemuan}/{@code VideoPertemuan}. Lihat
	 *         Javadoc {@code RELASI_MAP} pada {@link FileFotoLain} untuk akibat golongan ini
	 *         terhadap penyaringan {@code jenis} dan perilaku penghapusan.
	 */
	@Override
	public Long ambilRef() {
		// TODO Auto-generated method stub
		return id;
	}

	/** @return kelas runtime baris ini (selalu {@code PertemuanFileContent.class} kecuali lewat proxy Hibernate). */
	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClazz() {
		// TODO Auto-generated method stub
		return getClass();
	}

	/**
	 * @return selalu {@link #DEFAULT_JENIS} ({@code "pertemuan file"}). Getter ini MENULISKAN
	 *         balik nilai tersebut ke field {@link #jenis} sebagai efek samping setiap
	 *         dipanggil, menimpa apa pun yang mungkin sebelumnya disetel lewat {@link
	 *         #setJenis(String)} -- konsisten dengan golongan {@code RELASI_MAP} {@code "id"}
	 *         pada {@link FileFotoLain} yang mematikan penyaringan berbasis {@code jenis} untuk
	 *         kelas ini, sehingga nilai {@code jenis} yang berbeda-beda tidak relevan.
	 */
	@Override
	public String getJenis() {
		// TODO Auto-generated method stub
		jenis = DEFAULT_JENIS;
		return jenis;
	}

	/** @param jenis nilai jenis lampiran; akan ditimpa {@link #DEFAULT_JENIS} pada pemanggilan {@link #getJenis()} berikutnya. */
	public void setJenis(String jenis) {
		this.jenis = jenis;
	}

	/**
	 * @return kebijakan syarat akses materi ini sebagai teks JSON (kolom {@code text}); bila
	 *         kolom kosong/belum diisi, mengembalikan objek JSON kosong ({@code "{}"}) alih-alih
	 *         {@code null} -- pemanggil yang mem-parse hasilnya sebagai JSON tidak perlu menjaga
	 *         kasus {@code null} secara terpisah.
	 */
	@Column(columnDefinition = "text")
	public String getSyaratAkses() {
		return syaratAkses == null || syaratAkses.trim().isEmpty() ? new JSONObject().toString() : syaratAkses;
	}

	/** @param syaratAkses kebijakan syarat akses materi ini sebagai teks JSON. */
	public void setSyaratAkses(String syaratAkses) {
		this.syaratAkses = syaratAkses;
	}

	private String url;

	/**
	 * @return URL akses berkas ini, dihitung malas (lazy) sekali lewat {@code createLinkUri()}
	 *         milik {@link FileFotoLain} lalu di-cache pada field {@link #url}. Kolom
	 *         {@code @Transient} -- tidak pernah dipersist, dihitung ulang setiap objek baru
	 *         dimuat. Kegagalan penghitungan dicatat lewat {@code ErrorAuditUtil} dan
	 *         menghasilkan {@code null} alih-alih exception ke pemanggil.
	 */
	@Transient
	public String getUrl() {
		try {
			if (url == null && getId() != null) {
				url = createLinkUri();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/PertemuanFileContent.java:435");
		}
		return url;
	}

	/** @param url URL akses berkas ini; menimpa cache lazy pada {@link #getUrl()}. */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return lokasi fisik berkas ini di disk (di-trim), atau {@code null} bila kosong. Bila
	 *         {@link #copyDari} terisi, nilainya disegarkan lebih dulu dari lokasi fisik baris
	 *         sumber. Dipakai {@link #chekScrom(PertemuanFileContent)} sebagai sumber utama zip
	 *         SCORM, dan oleh {@link #getLink()} sebagai penanda bahwa tautan perlu disanitasi
	 *         (spasi diganti {@code _}).
	 */
	@Column(columnDefinition = "text")
	public String getLokasiFisik() {
		if (copyDari != null) {
			lokasiFisik = copyDari.lokasiFisik;
		}
		return lokasiFisik == null || lokasiFisik.trim().isEmpty() ? null : lokasiFisik.trim();
	}

	/** @param lokasiFisik lokasi fisik berkas ini di disk. */
	public void setLokasiFisik(String lokasiFisik) {
		this.lokasiFisik = lokasiFisik;
	}
}
