package ais.database.model.file;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.FlushMode;
import org.hibernate.jdbc.Work;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;

public abstract class FileFoto extends GeneralValueObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4894128831407104192L;

	private Blob foto;
	private String lokasiSimpan;

	/*
	 * Guard khusus pembacaan Blob PostgreSQL.
	 * Jangan menjalankan proses audit/lampiran lagi ketika file sedang diekstrak,
	 * karena AuditListener dapat memanggil ambilFile() dan memicu siklus flush -> audit
	 * -> baca Blob -> flush kembali sampai StackOverflowError.
	 */
	private static final ThreadLocal<Integer> BLOB_EXTRACTION_LEVEL = new ThreadLocal<Integer>();

	public abstract Long getId();

	public abstract void setId(Long id);

	public abstract Long ambilRef();

	public abstract String getNama();

	public abstract String getKeterangan();

	public abstract String ambilLink();

	public abstract String ambilJenis();

	public abstract String getOlehId();

	public abstract void setNama(String nama);

	public static boolean merupakanGambar(String nama) {
		return nama != null && !nama.trim().isEmpty()
				&& (nama.toLowerCase().endsWith(".jpg") || nama.toLowerCase().endsWith(".png")
						|| nama.toLowerCase().endsWith(".jpeg") || nama.toLowerCase().endsWith(".gif")
						|| nama.toLowerCase().endsWith(".tif") || nama.toLowerCase().endsWith(".bmp"));
	}

	public boolean merupakanGambar() {
		String nama = getNama();
		return FileFoto.merupakanGambar(nama);
	}

	public static boolean merupakanVideo(String nama) {
		return nama != null && !nama.trim().isEmpty() && nama.toLowerCase().endsWith(".mp4");
		// return nama != null && !nama.trim().isEmpty()
		// && (nama.toLowerCase().endsWith(".mov") ||
		// nama.toLowerCase().endsWith(".mp4")
		// || nama.toLowerCase().endsWith(".3gp"));
	}

	public boolean merupakanVideo() {
		String nama = getNama();
		return FileFoto.merupakanVideo(nama);
	}

	public static boolean merupakanDokumen(String nama) {
		return nama != null && !nama.trim().isEmpty()
				&& (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")
						|| nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")
						|| nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("odt")
						|| nama.toLowerCase().endsWith("pptx"));
	}

	public boolean merupakanDokumen() {
		String nama = getNama();
		return FileFoto.merupakanDokumen(nama);
	}

	public static boolean preview(String nama) {
		if (nama != null) {
			if (nama.toLowerCase().endsWith("png") || nama.toLowerCase().endsWith("jpg")
					|| nama.toLowerCase().endsWith("jpeg") || nama.toLowerCase().endsWith("gif")
					|| nama.toLowerCase().endsWith("svg") || nama.toLowerCase().endsWith("tiff")) {
				return true;
			} else if (nama.toLowerCase().endsWith("pdf")) {
				return true;
			} else if (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")
					|| nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")
					|| nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("odt")
					|| nama.toLowerCase().endsWith("pptx")) {
				return true;
			} else if (nama.toLowerCase().endsWith("txt")) {
				return true;
			} else if (nama.toLowerCase().endsWith("zip") || nama.toLowerCase().endsWith("rar")
					|| nama.toLowerCase().endsWith("gz") || nama.toLowerCase().endsWith("7z")
					|| nama.toLowerCase().endsWith("tar")) {
				return false;
			} else if (nama.toLowerCase().endsWith("mp3") || nama.toLowerCase().endsWith("wav")
					|| nama.toLowerCase().endsWith("ogg")) {
				return false;
			} else if (nama.toLowerCase().endsWith("avi") || nama.toLowerCase().endsWith("mp4")
					|| nama.toLowerCase().endsWith("3gp")) {
				return false;
			}
		}

		return false;
	}

	public static String icon(String nama) {
		String downloadIcon = "/img/attachment-icon.png";

		if (nama != null) {
			if (nama.toLowerCase().endsWith("png") || nama.toLowerCase().endsWith("jpg")
					|| nama.toLowerCase().endsWith("jpeg") || nama.toLowerCase().endsWith("gif")
					|| nama.toLowerCase().endsWith("svg") || nama.toLowerCase().endsWith("tiff")) {
				downloadIcon = "/img/Image-JPEG-icon.png";
			} else if (nama.toLowerCase().endsWith("pdf")) {
				downloadIcon = "/img/Adobe-PDF-Document-icon.png";
			} else if (nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")) {
				downloadIcon = "/img/Excel-icon.png";
			} else if (nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("pptx")) {
				downloadIcon = "/img/PowerPoint-icon.png";
			} else if (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")) {
				downloadIcon = "/img/Word-icon.png";
			} else if (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")
					|| nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")
					|| nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("odt")
					|| nama.toLowerCase().endsWith("pptx")) {
				downloadIcon = "/img/Document-Write-icon.png";
			} else if (nama.toLowerCase().endsWith("xml")) {
				downloadIcon = "/img/Other-xml-icon.png";
			} else if (nama.toLowerCase().endsWith("txt")) {
				downloadIcon = "/img/text-icon.png";
			} else if (nama.toLowerCase().endsWith("html")) {
				downloadIcon = "/img/Other-html-5-icon.png";
			} else if (nama.toLowerCase().endsWith("java")) {
				downloadIcon = "/img/Java-icon.png";
			} else if (nama.toLowerCase().endsWith("jrxml")) {
				downloadIcon = "/img/File-Adobe-Dreamweaver-XML-01-icon.png";
			} else if (nama.toLowerCase().endsWith("rar")) {
				downloadIcon = "/img/WinRAR-icon.png";
			} else if (nama.toLowerCase().endsWith("zip") || nama.toLowerCase().endsWith("rar")
					|| nama.toLowerCase().endsWith("gz") || nama.toLowerCase().endsWith("7z")
					|| nama.toLowerCase().endsWith("tar")) {
				downloadIcon = "/img/compress-icon.png";
			} else if (nama.toLowerCase().endsWith("mp3") || nama.toLowerCase().endsWith("wav")
					|| nama.toLowerCase().endsWith("ogg")) {
				downloadIcon = "/img/music-icon.png";
			} else if (nama.toLowerCase().endsWith("avi") || nama.toLowerCase().endsWith("mp4")
					|| nama.toLowerCase().endsWith("3gp")) {
				downloadIcon = "/img/video-camera-icon.png";
			} else if (nama.contains("yout")) {
				downloadIcon = "/img/YouTube-icon.png";
			} else if (nama.contains("facebook")) {
				downloadIcon = "/img/social-facebook-box-blue-icon.png";
			} else if (nama.contains("drive.google")) {
				downloadIcon = "/img/Google-Drive-icon.png";
			} else if (nama.contains("calendar.google")) {
				downloadIcon = "/img/Google-Calendar-icon.png";
			} else if (nama.contains("classroom.google")) {
				downloadIcon = "/img/googleclassroom.png";
			} else if (nama.contains("dropbox")) {
				downloadIcon = "/img/dropbox-icon.png";
			} else if (nama.contains("google.form")) {
				downloadIcon = "/img/google.form.png";
			} else if (nama.contains("instagram")) {
				downloadIcon = "/img/Active-Instagram-3-icon.png";
			} else if (nama.contains("twitter")) {
				downloadIcon = "/img/Twitter-icon.png";
			}

		}

		return downloadIcon;
	}

	public static String iconAwesome(String nama) {
		String downloadIcon = "fa-file";

		if (nama != null) {

			nama = nama.trim();
			if (nama.equalsIgnoreCase("Biodata Mahasiswa") || nama.equalsIgnoreCase("Mahasiswa")
					|| nama.equalsIgnoreCase("Siswa")) {
				downloadIcon = "fa-user-graduate";
			} else if (nama.equalsIgnoreCase("Pengaturan Pengguna") || nama.equalsIgnoreCase("Dosen")
					|| nama.equalsIgnoreCase("Pegawai") || nama.equalsIgnoreCase("PA")
					|| nama.equalsIgnoreCase("Dosen PA") || nama.toLowerCase().contains("biodata")) {
				downloadIcon = "fa-user";
			} else if (nama.equalsIgnoreCase("Grup Pengguna")) {
				downloadIcon = "fa-list-alt";
			} else if (nama.equalsIgnoreCase("Pengumuman") || nama.equalsIgnoreCase("Info")) {
				downloadIcon = "fa-info";
			} else if (nama.equalsIgnoreCase("Linimasa")) {
				downloadIcon = "fa-list-alt";
			} else if (nama.equalsIgnoreCase("Form Mahasiswa")) {
				downloadIcon = "fa-edit";
			} else if (nama.toLowerCase().contains("kartu")) {
				downloadIcon = "fa-credit-card";
			} else if (nama.toLowerCase().contains("seluler")) {
				downloadIcon = "fa-mobile";
			} else if (nama.equalsIgnoreCase("Dashboard")) {
				downloadIcon = "fa-dashboard";
			} else if (nama.equalsIgnoreCase("Data") || nama.equalsIgnoreCase("khs")) {
				downloadIcon = "fa-file-text-o";
			} else if (nama.equalsIgnoreCase("Pendukung")) {
				downloadIcon = "fa-file-text";
			} else if (nama.equalsIgnoreCase("Info Kalender Akademik") || nama.equalsIgnoreCase("Hari")) {
				downloadIcon = "fa-calendar-o";
			} else if (nama.equalsIgnoreCase("Minggu") || nama.equalsIgnoreCase("Bulan")) {
				downloadIcon = "fa-calendar";
			} else if (nama.equalsIgnoreCase("Info Kegiatan") || nama.toLowerCase().contains("informasi")) {
				downloadIcon = "fa-info-circle";
			} else if (nama.equalsIgnoreCase("Home") || nama.equalsIgnoreCase("Asrama")
					|| nama.toLowerCase().contains("beranda")) {
				downloadIcon = "fa-home";
			} else if (nama.equalsIgnoreCase("Prestasi")) {
				downloadIcon = "fa-trophy";
			} else if (nama.equalsIgnoreCase("e-Learning") || nama.equalsIgnoreCase("krs")) {
				downloadIcon = "fa-clipboard";
			} else if (nama.equalsIgnoreCase("Pengguna")) {
				downloadIcon = "fa-male";
			} else if (nama.equalsIgnoreCase("Ubah Password")) {
				downloadIcon = "fa-key";
			} else if (nama.equalsIgnoreCase("Reset Password User")) {
				downloadIcon = "fa-check";
			} else if (nama.equalsIgnoreCase("Konfigurasi & Utility")) {
				downloadIcon = "fa-gears";
			} else if (nama.equalsIgnoreCase("Pendataan")) {
				downloadIcon = "fa-th-list";
			} else if (nama.equalsIgnoreCase("KKN")) {
				downloadIcon = "fa-th-large";
			} else if (nama.equalsIgnoreCase("Bimbingan")) {
				downloadIcon = "fa-pencil-square-o";
			} else if (nama.equalsIgnoreCase("Sidang") || nama.toLowerCase().contains("ganti")) {
				downloadIcon = "fa-pencil-square";
			} else if (nama.equalsIgnoreCase("Penjadwalan")) {
				downloadIcon = "fa-calendar";
			} else if (nama.equalsIgnoreCase("Perkuliahan") || nama.equalsIgnoreCase("PKL")
					|| nama.equalsIgnoreCase("Tgs.Kel")) {
				downloadIcon = "fa-tasks";
			} else if (nama.equalsIgnoreCase("Dosen Pembimbing")) {
				downloadIcon = "fa-list-alt";
			} else if (nama.equalsIgnoreCase("PMB")) {
				downloadIcon = "fa-group";
			} else if (nama.equalsIgnoreCase("Wisuda")) {
				downloadIcon = "fa-trophy";
			} else if (nama.equalsIgnoreCase("Keuangan Mahasiswa")) {
				downloadIcon = "fa-money";
			} else if (nama.equalsIgnoreCase("Pengaturan Biaya") || nama.equalsIgnoreCase("Setup")) {
				downloadIcon = "fa-gears";
			} else if (nama.toLowerCase().contains("pembayaran")) {
				downloadIcon = "fa-keyboard-o";
			} else if (nama.toLowerCase().contains("billing")) {
				downloadIcon = "fa-money-bill";
			} else if (nama.toLowerCase().contains("monitor")) {
				downloadIcon = "fa-desktop";
			} else if (nama.toLowerCase().contains("pustaka") || nama.equalsIgnoreCase("Ref.")) {
				downloadIcon = "fa-book";
			} else if (nama.toLowerCase().contains("feeder") || nama.toLowerCase().contains("singkron")
					|| nama.toLowerCase().contains("syn") || nama.toLowerCase().contains("hitung")) {
				downloadIcon = "fa-refresh";
			} else if (nama.toLowerCase().contains("transaksi")) {
				downloadIcon = "fa-shopping-cart";
			} else if (nama.toLowerCase().contains("laporan")) {
				downloadIcon = "fa-print";
			} else if (nama.toLowerCase().contains("akunting")) {
				downloadIcon = "fa-building";
			} else if (nama.toLowerCase().contains("simpan")) {
				downloadIcon = "fa-save";
			} else if (nama.toLowerCase().contains("proyek")) {
				downloadIcon = "fa-tasks";
			} else if (nama.toLowerCase().contains("grafik") || nama.toLowerCase().contains("statistik")) {
				downloadIcon = "fa-bar-chart-o";
			} else if (nama.equalsIgnoreCase("keluar") || nama.toLowerCase().contains("logout")) {
				downloadIcon = "fa-power-off";
			} else if (nama.toLowerCase().contains("peminjaman")) {
				downloadIcon = "fa-user-edit";
			} else if (nama.toLowerCase().contains("pengadaan")) {
				downloadIcon = "fa-money-check";
			} else if (nama.toLowerCase().contains("cari")) {
				downloadIcon = "fa-search";
			} else if (nama.toLowerCase().contains("refresh")) {
				downloadIcon = "fa-refresh";
			} else if (nama.toLowerCase().contains("hapus")) {
				downloadIcon = "fa-trash-o";
			} else if (nama.toLowerCase().contains("download")) {
				downloadIcon = "fa-download";
			} else if (nama.toLowerCase().contains("upload")) {
				downloadIcon = "fa-upload";
			} else if (nama.toLowerCase().contains("buka kunci")) {
				downloadIcon = "fa-unlock";
			} else if (nama.toLowerCase().contains("kunci")) {
				downloadIcon = "fa-lock";
			} else if (nama.toLowerCase().contains("history")) {
				downloadIcon = "fa-archive";
			} else if (nama.toLowerCase().contains("waktu")) {
				downloadIcon = "fa-clock-o";
			} else if (nama.toLowerCase().contains("kecuali")) {
				downloadIcon = "fa-unlock-o";
			} else if (nama.toLowerCase().contains("tambah")) {
				downloadIcon = "fa-plus-square";
			} else if (nama.toLowerCase().contains("password")) {
				downloadIcon = "fa-key";
			} else if (nama.toLowerCase().contains("lampiran")) {
				downloadIcon = "fa-paperclip";
			} else if (nama.toLowerCase().contains("copy")) {
				downloadIcon = "fa-copy";
			} else if (nama.toLowerCase().contains("komen")) {
				downloadIcon = "fa-comments";
			} else if (nama.toLowerCase().contains("lihat")) {
				downloadIcon = "fa-eye";
			} else if (nama.toLowerCase().contains("tagihan")) {
				downloadIcon = "fa-file-invoice";
			} else if (nama.toLowerCase().contains("krs")) {
				downloadIcon = "fa-ballot-check";
			} else if (nama.toLowerCase().contains("nilai")) {
				downloadIcon = "fa-file-check";
			}

			else if (nama.toLowerCase().endsWith("png") || nama.toLowerCase().endsWith("jpg")
					|| nama.toLowerCase().endsWith("jpeg") || nama.toLowerCase().endsWith("gif")
					|| nama.toLowerCase().endsWith("svg") || nama.toLowerCase().endsWith("tiff")) {
				downloadIcon = "fa-file-image";
			} else if (nama.toLowerCase().endsWith("pdf")) {
				downloadIcon = "fa-file-pdf";
			} else if (nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")) {
				downloadIcon = "fa-file-excel";
			} else if (nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("pptx")) {
				downloadIcon = "fa-file-powerpoint";
			} else if (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")) {
				downloadIcon = "fa-file-word";
			} else if (nama.toLowerCase().endsWith("doc") || nama.toLowerCase().endsWith("docx")
					|| nama.toLowerCase().endsWith("xlsx") || nama.toLowerCase().endsWith("xlsx")
					|| nama.toLowerCase().endsWith("ppt") || nama.toLowerCase().endsWith("odt")
					|| nama.toLowerCase().endsWith("pptx")) {
				downloadIcon = "fa-file-word";
			} else if (nama.toLowerCase().endsWith("xml")) {
				downloadIcon = "fa-file-code";
			} else if (nama.toLowerCase().endsWith("txt")) {
				downloadIcon = "fa-file-text";
			} else if (nama.toLowerCase().endsWith("html")) {
				downloadIcon = "fa-html5";
			} else if (nama.toLowerCase().endsWith("java")) {
				downloadIcon = "fa-coffee";
			} else if (nama.toLowerCase().endsWith("jrxml")) {
				downloadIcon = "fa-file-code";
			} else if (nama.toLowerCase().endsWith("rar")) {
				downloadIcon = "fa-file-archive";
			} else if (nama.toLowerCase().endsWith("zip") || nama.toLowerCase().endsWith("rar")
					|| nama.toLowerCase().endsWith("gz") || nama.toLowerCase().endsWith("7z")
					|| nama.toLowerCase().endsWith("tar")) {
				downloadIcon = "fa-file-archive";
			} else if (nama.toLowerCase().endsWith("mp3") || nama.toLowerCase().endsWith("wav")
					|| nama.toLowerCase().endsWith("ogg")) {
				downloadIcon = "fa-file-audio";
			} else if (nama.toLowerCase().endsWith("avi") || nama.toLowerCase().endsWith("mp4")
					|| nama.toLowerCase().endsWith("3gp")) {
				downloadIcon = "fa-file-video";
			} else if (nama.contains("yout")) {
				downloadIcon = "fa-youtube";
			} else if (nama.contains("facebook")) {
				downloadIcon = "fa-facebookfficial";
			} else if (nama.contains("drive.google")) {
				downloadIcon = "fa-hdd";
			} else if (nama.contains("calendar.google")) {
				downloadIcon = "fa-calendar";
			} else if (nama.contains("classroom.google")) {
				downloadIcon = "fa-user-circle";
			} else if (nama.contains("dropbox")) {
				downloadIcon = "fa-dropbox";
			} else if (nama.contains("google.form")) {
				downloadIcon = "fa-check-square";
			} else if (nama.contains("instagram")) {
				downloadIcon = "fa-instagram";
			} else if (nama.contains("twitter")) {
				downloadIcon = "fa-twitter";
			}

		}

		return downloadIcon;
	}

	public String iconDonwload() {
		return FileFoto.icon(getNama());
	}

	public boolean bisaPreview() {
		return FileFoto.preview(getNama())
				|| (this instanceof PertemuanFileContent ? (((PertemuanFileContent) this).getLokasiFisik()) != null
						: false)
				|| (ambilLink() != null && ambilLink().toLowerCase().trim().contains("dropbox"));
	}

	public Blob getFoto() {
		return foto;
	}

	public void setFoto(Blob foto) {
		this.foto = foto;
	}

	public String getLokasiSimpan() {
		return lokasiSimpan;
	}

	public void setLokasiSimpan(String lokasiSimpan) {
		this.lokasiSimpan = lokasiSimpan;
	}

	public static File fileAdaDiFolder(String jenis, Long ref) {
		if (Common.folder != null && Common.folder.exists()) {
			File fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".jpg");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".jpeg");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".png");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".svg");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".bmp");
			if (fileBaru.exists()) {
				return fileBaru;
			}

			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".JPG");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".JPEG");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".PNG");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".SVG");
			if (fileBaru.exists()) {
				return fileBaru;
			}
			fileBaru = new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + ".BMP");
			if (fileBaru.exists()) {
				return fileBaru;
			}
		}

		return null;
	}

	public static boolean isBlobExtractionRunning() {
		Integer level = BLOB_EXTRACTION_LEVEL.get();
		return level != null && level.intValue() > 0;
	}

	private static void enterBlobExtraction() {
		Integer level = BLOB_EXTRACTION_LEVEL.get();
		BLOB_EXTRACTION_LEVEL.set(Integer.valueOf(level == null ? 1 : level.intValue() + 1));
	}

	private static void exitBlobExtraction() {
		Integer level = BLOB_EXTRACTION_LEVEL.get();
		if (level == null || level.intValue() <= 1) {
			BLOB_EXTRACTION_LEVEL.remove();
		} else {
			BLOB_EXTRACTION_LEVEL.set(Integer.valueOf(level.intValue() - 1));
		}
	}

	/**
	 * Nama berkas pada folder cadangan datar ({@link Common#folder}) yang UNIK per baris.
	 *
	 * <p><b>Kenapa ada.</b> Sebelumnya berkas cadangan dinamai {@code <jenis>_<ref>.<ext>} saja.
	 * Kunci itu TIDAK memuat identitas baris: {@code ref} adalah angka mentah yang menunjuk ke
	 * tabel BERBEDA-BEDA tergantung fiturnya (ref lampiran soal = BankSoal.id, ref foto absen =
	 * id entitas absensi, dst), dan seluruh fitur berbagi SATU folder datar. Begitu dua baris
	 * berbeda menghasilkan nama yang sama, {@code getPathfile()}/{@code ambilFileDariCacheAtauDisk()}
	 * mengembalikan berkas milik baris LAIN sebagai berkas resmi -- inilah yang membuat gambar
	 * soal ujian bisa tampil sebagai foto absensi. Dengan menyertakan nama kelas + PK baris,
	 * tabrakan nama menjadi mustahil.</p>
	 *
	 * <p>Berkas cadangan lama (berpola lawas) sengaja TIDAK dibaca lagi; bila belum ada berkas
	 * kanonik, isinya dibangun ulang dari blob -- lebih lambat sekali di awal, tetapi tidak
	 * pernah menyajikan gambar milik orang lain.</p>
	 */
	/**
	 * Nama kelas entitas untuk penamaan folder berkas. Memakai {@code getClass()} langsung TIDAK
	 * aman karena Hibernate bisa menyodorkan proxy ({@code LampiranLain_$$_javassist_42}), yang
	 * akan membuat folder berbeda untuk baris yang sama. {@code Hibernate.getClass} membuka
	 * proxy tersebut.
	 */
	protected String namaKelasBerkas() {
		try {
			return org.hibernate.Hibernate.getClass(this).getSimpleName();
		} catch (Exception e) {
			return getClass().getSimpleName();
		}
	}

	/**
	 * Segmen folder penyimpanan berkas milik baris ini: <code>&lt;NamaKelas&gt;/&lt;id&gt;</code>.
	 *
	 * <p><b>Kenapa dipisah per kelas.</b> Sebelumnya semua berkas ditaruh di
	 * <code>&lt;media&gt;/&lt;id&gt;/</code> — hanya berdasarkan PK, TANPA penanda entitas.
	 * Padahal ada ±50 subclass {@code FileFoto}/{@code FileFotoLain} yang masing-masing punya
	 * urutan id sendiri, sehingga {@code FotoPegawai#268928} dan {@code LampiranLain#268928}
	 * berbagi satu folder yang sama. Selama nama berkasnya kebetulan sama, isinya saling
	 * menimpa dan berkas bisa TERTUKAR antar fitur (mis. gambar soal ujian tampil sebagai foto
	 * absensi). Dengan menyisipkan nama kelas, tiap entitas punya ruang sendiri dan tabrakan
	 * menjadi mustahil.</p>
	 */
	public String segmenFolderBerkas() {
		return namaKelasBerkas() + "/" + getId();
	}

	/** Folder penyimpanan berkas milik baris ini (dibuat bila belum ada). */
	protected File direktoriBerkas() throws Exception {
		File dir = new File(CommonMedia.getMediaDirectory(), segmenFolderBerkas());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	/**
	 * Berkas kanonik baris ini pada tata letak BARU (<code>&lt;media&gt;/&lt;Kelas&gt;/&lt;id&gt;/&lt;nama&gt;</code>).
	 *
	 * <p>Kompatibel mundur: bila berkas belum ada di lokasi baru tetapi MASIH ada di lokasi lama
	 * (<code>&lt;media&gt;/&lt;id&gt;/&lt;nama&gt;</code>), berkas lama DIPINDAHKAN ke lokasi baru
	 * sekali jalan. Jadi instalasi lama tidak perlu migrasi massal — berkas berpindah sendiri saat
	 * pertama diakses, dan bila pemindahan gagal isinya tetap dibangun ulang dari blob.</p>
	 */
	protected File berkasKanonik(String namaFile) throws Exception {
		File baru = new File(direktoriBerkas(), namaFile);
		if (baru.exists() && baru.length() > 0L) {
			return baru;
		}
		try {
			File lama = new File(CommonMedia.getMediaDirectory(), getId() + "/" + namaFile);
			if (lama.exists() && lama.length() > 0L && !lama.getAbsolutePath().equals(baru.getAbsolutePath())) {
				if (!lama.renameTo(baru)) {
					FileUtils.copyFile(lama, baru);
					lama.delete();
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "migrasi-berkas-folder-kelas src/ais/database/model/file/FileFoto.java");
		}
		return baru;
	}

	private File berkasCadanganUnik(String extension) {
		if (Common.folder == null || !Common.folder.exists() || getId() == null) {
			return null;
		}
		String jenis = sanitizeFileName(ambilJenis() == null ? "tanpa_jenis" : ambilJenis());
		String ref = String.valueOf(ambilRef());
		return new File(Common.folder.getAbsolutePath() + "/" + jenis + "_" + ref + "_"
				+ namaKelasBerkas() + "_" + getId() + "." + extension);
	}

	/**
	 * Memastikan sebuah berkas memang MILIK baris ini sebelum disajikan. Nama berkas kanonik
	 * selalu berawalan {@code <id>_} (lihat {@code getNama()}), sehingga berkas yang tidak
	 * memenuhi pola itu -- mis. sisa cache lawas milik baris lain, atau nilai kolom
	 * {@code pathfile} yang basi setelah migrasi/penomoran ulang -- ditolak dan dibangun ulang
	 * dari blob. Berkas laporan (jrxml/jasper) dikecualikan karena memang dinamai apa adanya.
	 */
	private boolean berkasMilikBarisIni(File file) {
		try {
			if (file == null || getId() == null) {
				return true;
			}
			String namaBerkas = file.getName();
			String low = namaBerkas.toLowerCase();
			if (low.endsWith(".jrxml") || low.endsWith(".jasper")) {
				return true;
			}
			if (cocokDenganIdentitasBaris(namaBerkas, this)) {
				return true;
			}
			/*
			 * Baris "copy" (getCopyDari() != null) SENGAJA berbagi berkas fisik milik baris SUMBER
			 * (lihat getPathfile(), yang mengembalikan path sumber langsung tanpa menyalin -- dan
			 * merekursi lewat copyDari.getPathfile() sendiri) supaya blob tidak diduplikasi -- mis.
			 * beberapa entri branding menunjuk berantai ke satu logo yang sama (A copy-dari B,
			 * B copy-dari C, dst). Berkas semacam itu SAH meski namanya mengikuti id baris SUMBER
			 * di ujung rantai, BUKAN cuma satu hop -- tanpa menelusuri SELURUH rantai, guard di atas
			 * keliru menganggap baris copy generasi ke-2+ sebagai "salah baris" dan membuang berkas
			 * yang sebenarnya benar. `dikunjungi` mencegah rantai melingkar tak berujung.
			 */
			java.util.Set<Long> dikunjungi = new java.util.HashSet<Long>();
			dikunjungi.add(getId());
			FileFoto sumber = null;
			try {
				sumber = getCopyDari();
			} catch (Exception eCopyDari) {
				sumber = null;
			}
			while (sumber != null && sumber.getId() != null && !dikunjungi.contains(sumber.getId())) {
				if (cocokDenganIdentitasBaris(namaBerkas, sumber)) {
					return true;
				}
				dikunjungi.add(sumber.getId());
				try {
					sumber = sumber.getCopyDari();
				} catch (Exception eCopyDariLanjut) {
					sumber = null;
				}
			}
			return false;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "berkasMilikBarisIni src/ais/database/model/file/FileFoto.java");
			return true;
		}
	}

	/** Sel satu titik pengecekan identitas dipakai {@link #berkasMilikBarisIni(File)} baik utk baris ini maupun tiap baris di rantai {@code copyDari}. */
	private static boolean cocokDenganIdentitasBaris(String namaBerkas, FileFoto baris) {
		if (baris == null || baris.getId() == null) {
			return false;
		}
		String namaBaris = baris.sanitizeFileName(baris.getNama());
		if (namaBaris != null && (namaBerkas.equalsIgnoreCase(namaBaris)
				|| namaBerkas.equalsIgnoreCase("thumbnail_" + namaBaris))) {
			return true;
		}
		String kelasBaris = baris.namaKelasBerkas();
		return namaBerkas.startsWith(baris.getId() + "_")
				|| namaBerkas.startsWith("thumbnail_" + baris.getId() + "_")
				|| namaBerkas.contains("_" + kelasBaris + "_" + baris.getId() + ".");
	}

	public File ambilFileDariCacheAtauDisk() {
		try {
			Long id = getId();
			String namaFile = sanitizeFileName(getNama());
			if (id != null && namaFile != null && namaFile.trim().length() > 0) {
				File fileDiMedia = berkasKanonik(namaFile);
				if (fileDiMedia.exists() && fileDiMedia.length() > 0L) {
					return fileDiMedia;
				}

				if (namaFile.toLowerCase().endsWith("jrxml") || namaFile.toLowerCase().endsWith("jasper")) {
					File fileReport = new File(Common.ambilREAL_PATH_REPORT(), namaFile);
					if (fileReport.exists() && fileReport.length() > 0L) {
						return fileReport;
					}
				}
			}

			if (Common.folder != null && Common.folder.exists() && this instanceof LampiranLain) {
				try {
					String nama = getNama();
					String[] ex = StringUtils.split(nama, ".");
					if (ex != null && ex.length > 0) {
						String extension = ex[ex.length - 1];
						File fileBaru = berkasCadanganUnik(extension);
						if (fileBaru != null && fileBaru.exists() && fileBaru.length() > 0L) {
							return fileBaru;
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:533");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:536");
		}
		return null;
	}

	public String getPathfile() {
		String pathfile = null;

		try {
			FileFoto copyDari = null;
			try {
				copyDari = getCopyDari();
			} catch (Exception e) {
				copyDari = null;
			}

			if (copyDari != null) {
				try {
					String copyPath = copyDari.getPathfile();
					if (copyPath != null && copyPath.trim().length() > 0
							&& java.nio.file.Files.exists(java.nio.file.Paths.get(copyPath))) {
						return copyPath;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:559");
				}
			}

			Long id = getId();
			if (id == null) {
				return null;
			}

			String namaFile = sanitizeFileName(getNama());
			if (namaFile == null || namaFile.trim().length() == 0) {
				namaFile = "lampiran_" + id;
			}

			String dir = CommonMedia.getMediaDirectory().getAbsolutePath();
			String fileDiMedia = berkasKanonik(namaFile).getAbsolutePath();
			if (namaFile.toLowerCase().endsWith("jrxml") || namaFile.toLowerCase().endsWith("jasper")) {
				dir = Common.ambilREAL_PATH_REPORT();
				fileDiMedia = dir + "/" + namaFile;
			}

			File f = new File(fileDiMedia);
			File parent = f.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			boolean exist = f.exists() && f.length() > 0L;
			String extension = null;
			File fileBaru = null;

			if (!exist) {
				if (Common.folder != null && Common.folder.exists() && this instanceof LampiranLain) {
					try {
						String[] ex = StringUtils.split(getNama(), ".");
						if (ex != null && ex.length > 0) {
							extension = ex[ex.length - 1];
							dir = Common.folder.getAbsolutePath();
							fileBaru = berkasCadanganUnik(extension);
							if (namaFile.toLowerCase().endsWith("jrxml")
									|| namaFile.toLowerCase().endsWith("jasper")) {
								dir = Common.ambilREAL_PATH_REPORT();
								fileBaru = new File(dir + "/" + getNama());
							}

							if (fileBaru != null && fileBaru.exists() && fileBaru.length() > 10L) {
								return fileBaru.getAbsolutePath();
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:608");
					}
				}

				writeBlobToFileSafely(f, copyDari);
			}

			if (f.exists() && f.length() > 0L) {
				pathfile = f.getAbsolutePath();
			}

			if (pathfile != null && fileBaru != null && fileBaru.getParentFile() != null
					&& !fileBaru.getParentFile().exists()) {
				fileBaru.getParentFile().mkdirs();
			}
			if (pathfile != null && fileBaru != null && !f.getAbsolutePath().equalsIgnoreCase(fileBaru.getAbsolutePath())) {
				try {
					FileUtils.copyFile(f, fileBaru);
					if (extension != null && (extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("png")
							|| extension.equalsIgnoreCase("gif") || extension.equalsIgnoreCase("bmp"))) {
						File jpgFile = berkasCadanganUnik("jpg");
						if (jpgFile != null) {
							FileUtils.copyFile(f, jpgFile);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:632");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:635");
		}

		return pathfile;
	}

	private String sanitizeFileName(String namaFile) {
		if (namaFile == null) {
			return "";
		}
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, " ", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "%", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "#", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "[", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "]", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "(", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, ")", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "`", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "'", "_");
		namaFile = org.apache.commons.lang3.StringUtils.replace(namaFile, "\"", "_");
		return namaFile;
	}

	private void writeBlobToFileSafely(File target, FileFoto copyDari) {
		if (target == null || target.getParentFile() == null) {
			return;
		}
		try {
			if (!target.getParentFile().exists()) {
				target.getParentFile().mkdirs();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:666");
		}

		FileFoto sumber = copyDari == null ? this : copyDari;
		enterBlobExtraction();
		try {
			/*
			 * PostgreSQL large object/Blob harus dibaca dalam transaction aktif, tetapi
			 * transaction baca ini tidak boleh di-commit karena commit akan memicu flush dan
			 * AuditListener lagi pada Hibernate 3.6. Gunakan rollback read-only setelah stream
			 * selesai dibaca.
			 */
			if (writeBlobFromManagedEntity(target, sumber)) {
				return;
			}

			/*
			 * Fallback untuk kondisi khusus, misalnya Blob baru dibuat di memory dan belum
			 * bisa dibaca ulang dari database.
			 */
			writeBlobFromCurrentObject(target, sumber);
		} finally {
			exitBlobExtraction();
		}
	}

	private boolean writeBlobFromManagedEntity(File target, FileFoto source) {
		Session session = null;
		org.hibernate.Transaction transaction = null;
		java.io.InputStream in = null;
		FileOutputStream out = null;
		File tempFile = null;
		try {
			if (target == null || source == null || source.getId() == null) {
				return false;
			}

			session = openBlobReadSession(source);
			try {
				session.setFlushMode(FlushMode.MANUAL);
				session.setDefaultReadOnly(true);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:707");
			}
			transaction = session.beginTransaction();

			Object loaded = session.get(source.getClass(), source.getId());
			if (!(loaded instanceof FileFoto)) {
				rollbackQuietly(transaction);
				transaction = null;
				return false;
			}
			try {
				session.setReadOnly(loaded, true);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:719");
			}

			FileFoto managed = (FileFoto) loaded;
			Blob blob = managed.getFoto();
			if (blob == null) {
				rollbackQuietly(transaction);
				transaction = null;
				return false;
			}

			in = blob.getBinaryStream();
			if (in == null) {
				rollbackQuietly(transaction);
				transaction = null;
				return false;
			}

			tempFile = createTempFileNearTarget(target);
			out = new FileOutputStream(tempFile);
			IOUtils.copy(in, out);
			closeQuietly(out);
			out = null;
			closeQuietly(in);
			in = null;

			rollbackQuietly(transaction);
			transaction = null;

			replaceFile(tempFile, target);
			return target.exists() && target.length() > 0L;
		} catch (Exception e) {
			rollbackQuietly(transaction);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFoto.java:752");
		} finally {
			closeQuietly(in);
			closeQuietly(out);
			deleteTempIfExists(tempFile);
			closeSessionQuietly(session);
		}
		return false;
	}

	/**
	 * Membuka session khusus untuk membaca BLOB dari entity yang sedang diproses.
	 *
	 * Normalnya file/BLOB dibaca melalui SessionFactory streaming agar beban download/upload
	 * tidak mengganggu session utama. Namun beberapa entity lama sudah terlanjur hanya
	 * didaftarkan di hibernate.cfg.xml utama. Untuk entity tersebut jangan dipaksa masuk
	 * hibernate.streaming.cfg.xml, karena dapat mengubah pemisahan konfigurasi yang sudah
	 * berjalan.
	 *
	 * Saat ini LampiranPengumumanAkademis dipertahankan memakai hibernate.cfg.xml utama
	 * agar tidak memicu MappingException: Unknown entity pada SessionFactory streaming.
	 */
	private Session openBlobReadSession(FileFoto source) {
		if (useMainHibernateConfigForBlob(source)) {
			return HibernateUtil.getSessionFactory().openSession();
		}
		return StreamingHibernateUtil.getInstance().openSession();
	}

	private boolean useMainHibernateConfigForBlob(FileFoto source) {
		if (source == null) {
			return false;
		}
		try {
			Class clazz = source.getClass();
			while (clazz != null) {
				String className = clazz.getName();
				if ("ais.database.model.file.LampiranPengumumanAkademis".equals(className)) {
					return true;
				}
				clazz = clazz.getSuperclass();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:794");
		}
		return false;
	}

	private void writeBlobFromCurrentObject(File target, FileFoto source) {
		if (target == null || source == null) {
			return;
		}
		/*
		 * Entity PERSISTEN (punya id): BLOB PostgreSQL (large object) WAJIB dibaca dalam
		 * transaksi terdedikasi. Membaca source.getFoto() "telanjang" pada koneksi ambient
		 * yang sedang autocommit (mis. session streaming DataProcessor yang belum
		 * beginTransaction) memicu "invalid large-object descriptor" dan meng-ABORT
		 * transaksi koneksi tersebut. Karena koneksi itu dipakai bersama lewat pool, proses
		 * lain (mis. AppStartupListener.updateSqlStreaming) lalu gagal dengan
		 * "current transaction is aborted". Maka baca BLOB persisten secara TERISOLASI di
		 * SessionFactory yang memetakan entity tsb (utama/streaming), lalu rollback read-only.
		 */
		if (source.getId() != null) {
			writeBlobFromDedicatedTransaction(target, source);
			return;
		}
		// BLOB in-memory (belum tersimpan / id null): aman dibaca langsung tanpa transaksi.
		FileOutputStream out = null;
		java.io.InputStream in = null;
		File tempFile = null;
		try {
			Blob blob = source.getFoto();
			if (blob == null) {
				return;
			}
			in = blob.getBinaryStream();
			if (in == null) {
				return;
			}
			tempFile = createTempFileNearTarget(target);
			out = new FileOutputStream(tempFile);
			IOUtils.copy(in, out);
			closeQuietly(out);
			out = null;
			closeQuietly(in);
			in = null;
			replaceFile(tempFile, target);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFoto.java:839");
		} finally {
			closeQuietly(in);
			closeQuietly(out);
			deleteTempIfExists(tempFile);
		}
	}

	/**
	 * Pembacaan BLOB persisten yang AMAN: buka session terdedikasi dari SessionFactory
	 * yang MEMETAKAN entity source (utama atau streaming, via openBlobReadSession), mulai
	 * transaksi (read-only, flush manual), re-get entity berdasarkan id, salin stream BLOB
	 * ke berkas, lalu rollback &amp; tutup session. Dengan begitu pembacaan PostgreSQL large
	 * object tidak pernah meng-abort koneksi yang dipakai proses lain, dan entity streaming
	 * (mis. LampiranLain) tidak memicu MappingException: Unknown entity. Sesi ditutup di
	 * finally (disconnect/close).
	 */
	private void writeBlobFromDedicatedTransaction(File target, FileFoto source) {
		Session session = null;
		org.hibernate.Transaction transaction = null;
		java.io.InputStream in = null;
		FileOutputStream out = null;
		File tempFile = null;
		try {
			if (target == null || source == null || source.getId() == null) {
				return;
			}
			// Buka session dari SessionFactory yang BENAR-BENAR memetakan entity source.
			// Entity seperti LampiranLain hanya terdaftar di hibernate.streaming.cfg.xml,
			// BUKAN di hibernate.cfg.xml utama; memaksa pakai factory utama -> session.get
			// melempar MappingException: Unknown entity. openBlobReadSession() memilih
			// streaming/utama sesuai entity (sama dengan jalur writeBlobFromManagedEntity).
			session = openBlobReadSession(source);
			try {
				session.setFlushMode(FlushMode.MANUAL);
				session.setDefaultReadOnly(true);
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:875");
			}
			transaction = session.beginTransaction();

			Object loaded = session.get(source.getClass(), source.getId());
			if (!(loaded instanceof FileFoto)) {
				rollbackQuietly(transaction);
				transaction = null;
				return;
			}
			Blob blob = ((FileFoto) loaded).getFoto();
			if (blob == null) {
				rollbackQuietly(transaction);
				transaction = null;
				return;
			}
			in = blob.getBinaryStream();
			if (in == null) {
				rollbackQuietly(transaction);
				transaction = null;
				return;
			}

			tempFile = createTempFileNearTarget(target);
			out = new FileOutputStream(tempFile);
			IOUtils.copy(in, out);
			closeQuietly(out);
			out = null;
			closeQuietly(in);
			in = null;

			rollbackQuietly(transaction);
			transaction = null;

			replaceFile(tempFile, target);
		} catch (Exception e) {
			rollbackQuietly(transaction);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFoto.java:912");
		} finally {
			closeQuietly(in);
			closeQuietly(out);
			deleteTempIfExists(tempFile);
			closeSessionQuietly(session);
		}
	}

	private File createTempFileNearTarget(File target) throws Exception {
		File parent = target == null ? null : target.getParentFile();
		ais.common.CommonFileUtil.ensureWritableFile(target, 20L * 1024L * 1024L);
		if (parent == null || !parent.isDirectory()) {
			throw new java.io.IOException("Direktori tujuan foto tidak tersedia: " + target);
		}
		String suffix = ".tmp_" + System.currentTimeMillis() + "_" + Math.abs(Common.randLong());
		// FIX FileNotFoundException "File name too long": nama berkas asli (dari nama upload yang
		// sangat panjang, mis. deskripsi lampiran berjudul panjang) digabung dgn suffix di atas bisa
		// melebihi batas filesystem (umumnya 255 byte). Potong nama dasar bila perlu, tetap
		// pertahankan ekstensi supaya logic lain yang bergantung padanya tidak terpengaruh.
		String namaAsli = target.getName();
		int batasAmanNamaDasar = 200;
		if (namaAsli.length() > batasAmanNamaDasar) {
			int dot = namaAsli.lastIndexOf('.');
			String ekstensi = dot >= 0 ? namaAsli.substring(dot) : "";
			int sisaUntukNama = Math.max(1, batasAmanNamaDasar - ekstensi.length());
			namaAsli = namaAsli.substring(0, Math.min(sisaUntukNama, namaAsli.length())) + ekstensi;
		}
		return new File(parent, namaAsli + suffix);
	}

	private void replaceFile(File tempFile, File target) throws Exception {
		if (tempFile == null || target == null || !tempFile.exists() || tempFile.length() <= 0L) {
			return;
		}
		File parent = target.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		if (target.exists() && !target.delete()) {
			FileUtils.copyFile(tempFile, target);
		} else if (!tempFile.renameTo(target)) {
			FileUtils.copyFile(tempFile, target);
		}
	}

	private void commitQuietly(org.hibernate.Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.commit();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:949");
		}
	}

	private void rollbackQuietly(org.hibernate.Transaction transaction) {
		try {
			if (transaction != null && transaction.isActive()) {
				transaction.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:958");
		}
	}

	private void closeSessionQuietly(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:968");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:972");
		}
		try {
			session.close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:976");
		}
	}

	private void closeQuietly(java.io.Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
			/* Driver PostgreSQL dapat melaporkan descriptor sudah invalid ketika stream
			 * large-object ditutup setelah EOF/rollback. Bila penyalinan sudah selesai,
			 * ini hanya kondisi cleanup; transaksi dan session tetap ditutup di finally. */
			String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
			if (message.indexOf("invalid large-object descriptor") < 0) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:986");
			}
		}
	}

	private void deleteTempIfExists(File tempFile) {
		try {
			if (tempFile != null && tempFile.exists() && tempFile.getName().indexOf(".tmp_") >= 0) {
				tempFile.delete();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:995");
		}
	}

	public File ambilFile() {
		String path = getPathfile();
		File file = path == null || path.trim().length() == 0 ? null : new File(path);
		/*
		 * PENJAGA SALAH-BERKAS. path bisa berasal dari kolom pathfile yang basi atau dari
		 * cache datar lawas yang dinamai <jenis>_<ref> tanpa identitas baris, sehingga bisa
		 * menunjuk berkas MILIK BARIS LAIN (gejala: gambar soal ujian tampil sebagai foto
		 * absensi). Bila nama berkasnya tidak cocok dengan baris ini, path diabaikan dan
		 * berkas dibangun ulang dari blob melalui jalur kanonik <media>/<id>/<nama>.
		 */
		if (file != null && !berkasMilikBarisIni(file)) {
			ais.common.ErrorAuditUtil.record(
					new IllegalStateException("Berkas tidak cocok dgn baris " + getClass().getSimpleName() + "#"
							+ getId() + " (nama=" + getNama() + "), diabaikan: " + file.getAbsolutePath()),
					"berkas-salah-baris src/ais/database/model/file/FileFoto.java:ambilFile");
			file = null;
		}
		try {
			if (file == null) {
				Long id = getId();
				String nama = sanitizeFileName(getNama());
				if (id != null && nama != null && nama.trim().length() > 0) {
					file = berkasKanonik(nama);
				}
			}

			if (file == null) {
				return new File(Common.REAL_PATH + "/img/logo.png");
			}

			File parent = file.getParentFile();
			if (parent != null && !parent.exists()) {
				parent.mkdirs();
			}

			if (!file.exists() || file.length() <= 0L) {
				if (getGdrive() != null && getGdrive().trim().length() > 0) {
					FileUtils.copyURLToFile(new URL(downloadGDriveUrl()), file);
				} else {
					writeBlobToFileSafely(file, getCopyDari());
				}
			}
		} catch (Exception e1) {
			// File lampiran lama/Google Drive kadang sudah tidak tersedia. Untuk kebutuhan
			// tanda tangan/report, gunakan placeholder di bawah tanpa membanjiri ErrorAudit.
		}

		// Berkas fisik & backup blob keduanya tidak tersedia (mis. terhapus manual/migrasi
		// server) -> jangan kembalikan File yang menunjuk ke path tak ada, sebab pemanggil
		// (mis. AmbilLampiran/AmbilFile/AmbilMedia) bisa langsung membuka FileInputStream
		// dan melempar FileNotFoundException mentah. Pakai placeholder yang sama seperti
		// kasus file==null di atas, supaya kontrak "ambilFile() selalu File yang benar-benar
		// ada" tetap terjaga untuk seluruh caller lain di codebase.
		if (file != null && (!file.exists() || file.length() <= 0L)) {
			File placeholder = new File(Common.REAL_PATH + "/img/logo.png");
			if (placeholder.exists()) {
				return placeholder;
			}
		}
		return file;
	}

	public abstract FileFoto getCopyDari();

	// public abstract void setCopyDari(FileFoto copyDari);

	public abstract String getGdrive();

	public abstract void setGdrive(String gdrive);

	public abstract String getGdriveUsername();

	public abstract void setGdriveUsername(String gdriveUsername);

	public void tampilGDrive(Component parent) throws Exception {
		if (parent == null) {
			Common.displayWindow(false, "https://drive.google.com/open?id=" + getGdrive(), true, "95%", "95%", true,
					this);
		} else {
			Html html = new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/" + getGdrive()
					+ "/preview\" " + Common.getStyleContent() + "></iframe>");
			html.setAttribute("lampiran_tambahan", true);
			html.setParent(parent);

			if (parent instanceof HtmlBasedComponent) {
				((HtmlBasedComponent) parent).setStyle("min-height: 325px;");
			}
		}
	}

	// public void hapus(Session session) {
	// session.getTransaction().begin();
	// session.delete(this);
	// session.getTransaction().commit();
	// }

	public static void hapusTotal(final String oid, Session session) {
		session.doWork(new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {

				Statement stmt = null;

				try {
					stmt = connection.createStatement();
					String unlink = "select lo_unlink(" + oid + ");";
					ResultSet rs = stmt.executeQuery(unlink);
					while (rs.next()) {
						int hasil = rs.getInt(1);
						System.out.println("Unlink file -> " + unlink + " hasil " + hasil);
					}

				} catch (SQLException e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFoto.java:1083");
				} finally {
					if (stmt != null) {
						stmt.close();
					}
				}

			}
		});

	}

	public String forwardGDriveUrl() {
		if (getGdrive() != null) {
			return "https://drive.google.com/file/d/" + getGdrive() + "/view";
		}
		return "";
	}

	public static String thumbnailFromExport(String exportUrl) {

		if (StringUtils.contains(exportUrl, "https://drive.google.com/uc?export=view&id=")) {
			return "https://drive.google.com/thumbnail?id=" + org.apache.commons.lang3.StringUtils.replace(exportUrl,
					"https://drive.google.com/uc?export=view&id=", "");
		} else {
			return exportUrl;
		}
	}

	public String thumbnailGDriveUrl() {
		if (getGdrive() != null) {
			return "https://drive.google.com/thumbnail?id=" + getGdrive();
		}
		return "";
	}

	public String exportGDriveUrl() {
		if (getGdrive() != null) {
			String url = "https://drive.usercontent.google.com/download?id=" + getGdrive() + "&export=view";
			return url;
//			return "https://drive.google.com/uc?export=view&id=" + getGdrive();
		}
		return "";
	}

	public String downloadGDriveUrl() {
		if (getGdrive() != null) {
			String url = "https://drive.usercontent.google.com/download?id=" + getGdrive() + "&export=view";
			return url;
//			return "https://drive.google.com/uc?download=view&id=" + getGdrive();
		}
		return "";
	}

	public String dropboxLinkRaw() {
		try {
			if (ambilLink() != null && ambilLink().toLowerCase().contains("dropbox")) {
				return org.apache.commons.lang3.StringUtils.replace(
						org.apache.commons.lang3.StringUtils.replace(ambilLink(), "dl=0", "raw=1"), "dl=1", "raw=1");
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFoto.java:1143");
			// TODO: handle exception
		}
		return "";
	}

}
