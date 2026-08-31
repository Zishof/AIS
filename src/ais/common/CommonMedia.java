package ais.common;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Image;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoGambarItem;
import ais.database.model.file.FotoGambarKopSurat;
import ais.database.model.file.FotoGambarProduk;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.FotoGambarTandaTanganPejabat;
import ais.database.model.file.MediaParameter;

/**
 * Kelas utilitas statis untuk pengelolaan berkas media (foto/gambar profil pengguna, foto item
 * inventori, foto produk, kop surat, tanda tangan pejabat, lampiran surat masuk/keluar, dsb.) di
 * AIS: menentukan direktori penyimpanan media di server, mengubah ukuran (resize) gambar dengan
 * kualitas render halus, membangun URL "tidak langsung" (indirect URL) yang mengarah ke servlet
 * pengambil media (mis. {@code /AmbilMedia}, {@code /AmbilMediaItem}, {@code /AmbilFile}) untuk
 * ditampilkan di halaman, serta menebak tipe MIME sebuah berkas berdasarkan ekstensinya.
 *
 * <h2>Dua generasi API dalam satu kelas</h2>
 * <p>
 * Kelas ini memuat campuran dua generasi implementasi: sebagian besar method terkait "foto
 * pengguna langsung" (mis. {@link #loadFotoPenggunaLangsung}, {@link #getUrlFotoPengguna(Tbmuser,
 * Integer, Integer)}, {@link #preview(FileFoto)}, {@link #tampilkanGambar(String, String, String)})
 * kini hanya berupa <b>wrapper tipis</b> yang langsung mendelegasikan seluruh pekerjaan ke kelas
 * {@code ais.common.ProfileImageUtil} — dipertahankan di sini semata untuk kompatibilitas mundur
 * agar kode pemanggil lama yang masih memakai {@code CommonMedia.xxx(...)} tidak perlu diubah.
 * Sebaliknya, method-method terkait direktori media ({@link #getMediaDirectory()}), resize gambar
 * mentah ({@link #resize}, {@link #resizeImage}), pembangunan URL media terenkripsi
 * ({@link #getMedia(MediaParameter)} dan seluruh method {@code getUrlFotoXxx}), serta deteksi MIME
 * ({@link #getMime(File)}) adalah implementasi asli kelas ini sendiri.
 * </p>
 *
 * <h2>Direktori media &amp; inisialisasi malas</h2>
 * <p>
 * {@link #getMediaDirectory()} menentukan folder fisik tempat berkas media tersimpan di server
 * (pola {@code <REAL_PATH>/f<prefix>}, dengan {@code prefix} umumnya berupa context path aplikasi
 * web) dan membuatnya bila belum ada. Hasilnya di-cache pada field statis {@link #mediaDic}/
 * {@link #MEDIA_DIR} agar penentuan direktori (yang bergantung pada request HTTP aktif lewat
 * {@code ExecutionsCtrl}/{@code RequestContext}) hanya dilakukan sekali per siklus hidup aplikasi.
 * {@link #initConfig()} adalah inisialisasi terpisah yang membaca/menulis berkas properti eksternal
 * {@code /opt/.g/.h/portlet.properties} berisi {@code ais_host} (URL dasar aplikasi), dipakai
 * konteks portlet/integrasi eksternal; bila berkas belum ada, dibuat otomatis dengan nilai default
 * {@code http://localhost:8080/ais/}.
 * </p>
 *
 * <h2>URL media tidak langsung &amp; enkripsi referensi</h2>
 * <p>
 * {@link #getMedia(MediaParameter)} dan seluruh method {@code getUrlFotoXxx} (item, produk, kop
 * surat, pejabat, surat masuk/keluar) tidak pernah mengekspos id/nama kelas entitas media secara
 * langsung di URL. Sebagai gantinya, detail referensi (id entitas, nama properti berkas, nama
 * properti media, nama kelas Java entitas, dsb.) dikemas ke JSON lalu <b>dienkripsi</b> lewat
 * {@code Common.desEncrypter.get().encrypt(...)} sebelum disisipkan sebagai parameter query
 * {@code d} pada URL servlet {@code /AmbilMedia} — pola ini menyulitkan penebakan/enumerasi id
 * entitas media secara langsung dari URL (mirip <i>obscured reference</i>), namun perlu dicatat
 * bahwa skema enkripsi yang dipakai ({@code desEncrypter}) adalah DES simetris yang sama dipakai
 * untuk enkripsi password di beberapa kelas lain AIS (lihat catatan keamanan pada
 * {@link UserDetailsServiceImpl}) — keamanan mekanisme ini bergantung penuh pada kerahasiaan kunci
 * DES bersama, dan enkripsi referensi ini sendiri <b>bukan pengganti pemeriksaan otorisasi</b> di
 * servlet penerima ({@code AmbilMedia} dkk.) untuk memastikan pengguna yang meminta memang berhak
 * melihat media tersebut.
 * </p>
 *
 * <h2>Catatan pemeriksaan keamanan upload/path (sesuai permintaan audit)</h2>
 * <p>
 * Kelas ini <b>tidak berisi logika penerimaan berkas unggahan</b> (tidak ada method yang menerima
 * {@code multipart/form-data} atau stream unggahan langsung) — pemrosesan unggahan sesungguhnya
 * didelegasikan ke kelas lain (mis. {@code ProfileImageUtil} atau action ZK terkait) yang berada di
 * luar cakupan berkas ini. Method {@link #resize(File, int, int)} memang menerima sembarang
 * {@link File} untuk diubah ukurannya, tetapi memvalidasi lebih dulu bahwa berkas tersebut benar
 * gambar lewat {@code Common.isImage(originalFile)} (pemeriksaan berbasis konten, bukan sekadar
 * ekstensi nama berkas) sebelum diproses, dan nama berkas keluaran dibangun dari
 * {@code originalFile.getName()} — yang secara semantik {@link File#getName()} hanya mengembalikan
 * komponen nama akhir tanpa pemisah direktori, sehingga tidak membuka celah <i>path traversal</i>
 * lewat segmen {@code "../"} pada langkah ini. Tidak ditemukan validasi ekstensi berkas yang hilang
 * atau celah path traversal yang jelas di dalam berkas ini sendiri pada saat audit.
 * </p>
 */
public class CommonMedia {

	/** Lebar default ikon/thumbnail kecil (piksel), dipakai {@link #resize(File)}. */
	public static final int IMG_WIDTH = 20;
	/** Tinggi default ikon/thumbnail kecil (piksel), dipakai {@link #resize(File)}. */
	public static final int IMG_HEIGHT = 20;

	/** Lebar default thumbnail ukuran sedang (piksel), dipakai method {@code loadFotoPenggunaLangsung}. */
	public static final int IMG_WIDTH_MEDIUM = 90;
	/** Tinggi default thumbnail ukuran sedang (piksel), dipakai method {@code loadFotoPenggunaLangsung}. */
	public static final int IMG_HEIGHT_MEDIUM = 100;

	/** Cache internal direktori media fisik di server; diisi sekali oleh {@link #getMediaDirectory(String)}. */
	private static File mediaDic = null;
	/** Cache publik direktori media fisik di server (hasil akhir {@link #getMediaDirectory()}), dipakai ulang oleh pemanggilan berikutnya tanpa perlu resolusi request ulang. */
	public static File MEDIA_DIR = null;
	/** Prefix (umumnya context path aplikasi web) yang dipakai membentuk path direktori media {@code <REAL_PATH>/f<prefix>}. */
	public static String prefix = "";
	/** Path absolut direktori media fisik yang terakhir dihitung, diisi bersamaan dengan {@link #mediaDic}. */
	public static String path = null;

	/** Penanda apakah {@link #initConfig()} sudah pernah berhasil dijalankan pada siklus hidup JVM ini. */
	private static volatile Boolean hasInitConfig = false;

	// Map untuk efisiensi mime-type lookup O(1)
	/** Peta ekstensi berkas (huruf kecil, tanpa titik) ke tipe MIME, dipakai {@link #getMime(File)} sebagai jalur cepat sebelum jatuh ke {@link URLConnection#guessContentTypeFromName(String)}. */
	private static final Map<String, String> MIME_MAP = new HashMap<String, String>();
	static {
		MIME_MAP.put("pdf", "application/pdf");
		MIME_MAP.put("png", "image/png");
		MIME_MAP.put("gif", "image/gif");
		MIME_MAP.put("jpeg", "image/jpeg");
		MIME_MAP.put("jpg", "image/jpeg");
		MIME_MAP.put("ppt", "application/vnd.ms-powerpoint");
		MIME_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		MIME_MAP.put("doc", "application/msword");
		MIME_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		MIME_MAP.put("xls", "application/vnd.ms-excel");
		MIME_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		MIME_MAP.put("html", "text/html");
		MIME_MAP.put("htm", "text/html");
		MIME_MAP.put("xml", "text/xml");
	}

	/** Varian ringkas {@link #resize(File, int, int)} memakai ukuran ikon kecil default ({@link #IMG_WIDTH} x {@link #IMG_HEIGHT}). */
	public static File resize(File originalFile) {
		return resize(originalFile, IMG_WIDTH, IMG_HEIGHT);
	}

	/** Varian {@link #resize(File, int, int)} yang menerima {@code width}/{@code height} sebagai {@code double}, dibulatkan ke bawah (cast) menjadi {@code int} sebelum diteruskan. */
	public static File resize(File originalFile, double width, double height) {
		return resize(originalFile, (int) width, (int) height);
	}

	/**
	 * Mengembalikan versi ukuran-ubah (resized) dari {@code originalFile} berukuran
	 * {@code width}x{@code height} piksel, dengan cache berbasis nama berkas: bila versi resize
	 * dengan dimensi yang sama sudah pernah dibuat sebelumnya (berkas
	 * {@code <height>px_<width>px__<namaAsli>} sudah ada di {@link #getMediaDirectory()}), berkas
	 * yang sudah ada langsung dikembalikan tanpa memproses ulang gambar. Bila {@code originalFile}
	 * bukan gambar valid (menurut {@code Common.isImage(...)}), atau berkas cache yang ditemukan
	 * ternyata bukan gambar valid, dikembalikan ikon administrator default sebagai fallback. Proses
	 * resize sesungguhnya didelegasikan ke {@link #resizeImage(File, int, int, File)}.
	 *
	 * @param originalFile berkas gambar sumber
	 * @param width        lebar target hasil resize (piksel)
	 * @param height       tinggi target hasil resize (piksel)
	 * @return berkas hasil resize (baru dibuat atau dari cache), ikon default bila sumber bukan
	 *         gambar valid, atau {@code null} bila terjadi exception yang tertangkap secara internal
	 */
	public static File resize(File originalFile, int width, int height) {
		try {
			if (!Common.isImage(originalFile)) {
				return new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
			}

			File filekecil = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + height + "px_" + width
					+ "px__" + originalFile.getName().replaceAll(" ", "_"));

			if (filekecil.exists()) {
				if (!Common.isImage(filekecil)) {
					return new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
				}
				return filekecil;
			}

			resizeImage(originalFile, width, height, filekecil);
			return filekecil;
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonMedia.java:99");
		}
		return null;
	}

	/**
	 * Melakukan operasi ubah-ukuran (resize) gambar sesungguhnya: membaca {@code originalFile} lewat
	 * {@link ImageIO#read(File)}, menskalakannya ke {@code width}x{@code height} piksel memakai
	 * {@link java.awt.Image#getScaledInstance}, lalu merender ulang hasilnya ke
	 * {@link BufferedImage} baru dengan pengaturan kualitas render halus (interpolasi bilinear,
	 * antialiasing, dan rendering kualitas tinggi diaktifkan lewat {@link RenderingHints}) sebelum
	 * ditulis ke {@code filekecil}. Format keluaran (ekstensi) mengikuti ekstensi nama
	 * {@code originalFile}; bila nama berkas tidak memiliki ekstensi yang jelas, dipakai {@code jpg}
	 * sebagai default. Seluruh exception (termasuk kegagalan baca/tulis gambar) ditelan secara diam-
	 * diam — method ini tidak melempar exception maupun mengembalikan nilai; pemanggil mengetahui
	 * kegagalan hanya lewat ketiadaan berkas {@code filekecil} setelahnya.
	 *
	 * @param originalFile berkas gambar sumber; bila {@code null} atau gagal dibaca sebagai gambar,
	 *                     method berhenti tanpa efek
	 * @param width        lebar target (piksel); harus lebih besar dari 0
	 * @param height       tinggi target (piksel); harus lebih besar dari 0
	 * @param filekecil    berkas tujuan penulisan hasil resize
	 */
	public static void resizeImage(File originalFile, int width, int height, File filekecil) {
		try {
			if (originalFile == null || width <= 0 || height <= 0) {
				return;
			}
			BufferedImage image = ImageIO.read(originalFile);
			if (image == null)
				return;

			if (image.getWidth() <= 0 || image.getHeight() <= 0) {
				return;
			}

			java.awt.Image originalImage = image.getScaledInstance(width, height, java.awt.Image.SCALE_DEFAULT);

			int type = ((image.getType() == 0) ? BufferedImage.TYPE_INT_ARGB : image.getType());
			BufferedImage resizedImage = new BufferedImage(width, height, type);

			Graphics2D g2d = resizedImage.createGraphics();
			g2d.drawImage(originalImage, 0, 0, width, height, null);
			g2d.dispose();
			g2d.setComposite(AlphaComposite.Src);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Perbaikan Potensi Error: Mencegah ArrayIndexOutOfBounds jika tidak ada titik
			// di nama file
			String extension = "jpg"; // default
			String fileName = originalFile.getName();
			int dotIndex = fileName.lastIndexOf('.');
			if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
				extension = fileName.substring(dotIndex + 1);
			}

			if (filekecil != null) {
				ImageIO.write(resizedImage, extension, filekecil);
			}
		} catch (Exception e) {
			// Abaikan atau log sesuai kebutuhan
		}
	}

	/**
	 * Mengubah sembarang implementasi {@link java.awt.Image} menjadi {@link BufferedImage} beralpha
	 * ({@code TYPE_INT_ARGB}). Bila {@code img} sudah berupa {@link BufferedImage}, dikembalikan apa
	 * adanya tanpa penyalinan (short-circuit); selain itu, gambar digambar ulang ke
	 * {@link BufferedImage} baru dengan dimensi yang sama.
	 *
	 * @param img gambar sumber, dari sumber apa pun yang mengimplementasikan {@link java.awt.Image}
	 * @return representasi {@link BufferedImage} dari {@code img}
	 */
	public static BufferedImage toBufferedImage(java.awt.Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		return bimage;
	}

	/**
	 * Menentukan (dan membuat bila perlu) direktori fisik penyimpanan media untuk request/konteks
	 * saat ini. Bila {@link #MEDIA_DIR} sudah pernah dihitung (cache statis), langsung
	 * dikembalikan. Selain itu, bila {@link #mediaDic} belum pernah diisi, method mencoba menentukan
	 * context path dari request HTTP aktif (lewat {@code ExecutionsCtrl.getCurrent()}, fallback ke
	 * {@link RequestContext#get()} bila tidak ada konteks eksekusi ZK), lalu meneruskannya ke
	 * {@link #getMediaDirectory(String)} untuk membangun path direktori sesungguhnya.
	 *
	 * @return direktori fisik penyimpanan media, sudah dipastikan ada (dibuat bila belum ada)
	 * @throws Exception diteruskan dari resolusi request/pembuatan direktori
	 */
	public static File getMediaDirectory() throws Exception {
		if (MEDIA_DIR != null) {
			return MEDIA_DIR;
		}

		if (mediaDic == null) {
			HttpServletRequest request = null;
			try {
				if (ExecutionsCtrl.getCurrent() != null) {
					request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMedia.java:162");
				// TODO: handle exception
			}

			if (request == null) {
				request = RequestContext.get();
			}

			MEDIA_DIR = getMediaDirectory(request == null ? Common.ROOT : request.getContextPath());
		} else {
			MEDIA_DIR = getMediaDirectory("");
		}

		return MEDIA_DIR;
	}

	/**
	 * Membangun path direktori media {@code <Common.REAL_PATH>/f<prefixParam>} dan menyimpannya ke
	 * cache {@link #mediaDic}/{@link #path}/{@link #prefix} bila belum pernah diisi sebelumnya
	 * (hanya berlaku sekali — pemanggilan berikutnya dengan {@code prefixParam} berbeda tidak akan
	 * mengubah cache yang sudah ada). Direktori dibuat lewat {@link File#mkdirs()} bila belum ada di
	 * disk, terlepas dari apakah cache baru saja diisi atau sudah ada sebelumnya.
	 *
	 * @param prefixParam prefix path (umumnya context path aplikasi web) yang menentukan folder
	 *                     media; diabaikan bila cache {@link #mediaDic} sudah pernah diisi atau bila
	 *                     {@code Common.REAL_PATH}/{@code prefixParam} kosong
	 * @return direktori media yang sudah dipastikan ada di disk (dapat berupa {@code null} bila
	 *         cache belum pernah berhasil diisi sama sekali)
	 * @throws Exception dideklarasikan untuk konsistensi dengan overload lain; tidak dilempar secara
	 *                    langsung oleh implementasi saat ini
	 */
	public static File getMediaDirectory(String prefixParam) throws Exception {
		if (mediaDic == null && !Common.REAL_PATH.isEmpty() && !prefixParam.isEmpty()) {
			File file = new File(Common.REAL_PATH);
			path = file.getAbsolutePath() + "/f" + prefixParam;
			mediaDic = new File(path);
			CommonMedia.prefix = prefixParam;
		}
		if (mediaDic != null && !mediaDic.exists()) {
			mediaDic.mkdirs();
		}
		return mediaDic;
	}

	/**
	 * Memastikan {@code file} tersedia sebagai representasi berkas fisik dari {@code foto}: bila
	 * {@code file} sudah ada di disk, dikembalikan apa adanya; bila belum, isi berkas foto disalin
	 * (lewat {@code Common.copy}) dari sumber data {@code foto.ambilFile()} ke lokasi {@code file}.
	 *
	 * @param foto entitas {@link FileFoto} sumber data berkas
	 * @param file  lokasi berkas tujuan/cache; dipakai sebagai target salinan bila belum ada
	 * @return {@code file} yang sudah dipastikan berisi data foto
	 * @throws Exception diteruskan dari operasi salin berkas
	 */
	public static File getFileFotoDenganFile(FileFoto foto, File file) throws Exception {
		if (file != null && file.exists()) {
			return file;
		} else {
			Common.copy(foto.ambilFile(), file);
			return file;
		}
	}

	/** Membungkus hasil {@link #getFileFotoLangsungOld(FileFoto, Integer, Integer, boolean)} (dipaksa sebagai gambar) menjadi {@link AImage} ZK siap tampil. */
	public static AImage getFotoLangsungOld(FileFoto foto, Integer height, Integer width) throws Exception {
		return new AImage(getFileFotoLangsungOld(foto, height, width, true));
	}

	/** Varian ringkas {@link #loadFotoPenggunaLangsung(Tbmuser, Image, Integer, Integer)} tanpa komponen {@link Image} existing, memakai dimensi sedang default ({@link #IMG_HEIGHT_MEDIUM}/{@link #IMG_WIDTH_MEDIUM}). */
	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser) throws Exception {
		return loadFotoPenggunaLangsung(tbmuser, null, IMG_HEIGHT_MEDIUM, IMG_WIDTH_MEDIUM);
	}

	/** Varian {@link #loadFotoPenggunaLangsung(Tbmuser, Image, Integer, Integer)} dengan dimensi sedang default, memakai komponen {@link Image} {@code foto} yang sudah ada. */
	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser, Image foto) throws Exception {
		return loadFotoPenggunaLangsung(tbmuser, foto, IMG_HEIGHT_MEDIUM, IMG_WIDTH_MEDIUM);
	}

	/** Alias {@link #loadFileFotoLangsung(Tbmuser)} dengan nama yang menegaskan nilai kembaliannya berupa path berkas. */
	public static String loadPathFileFotoLangsung(Tbmuser tbmuser) throws Exception {
		return loadFileFotoLangsung(tbmuser);
	}

	/** Varian ringkas {@link #loadFileFotoLangsung(Tbmuser, Integer, Integer, Boolean)} tanpa batasan dimensi, dipaksa sebagai gambar ({@code berupaGambar=true}). */
	public static String loadFileFotoLangsung(Tbmuser tbmuser) throws Exception {
		return loadFileFotoLangsung(tbmuser, null, null, true);
	}

	/** Varian {@link #getUrlFotoPengguna(Tbmuser, Integer, Integer)} yang tidak melempar exception ke pemanggil — kegagalan dicatat lewat {@code ErrorAuditUtil} dan dikembalikan string kosong. */
	public static String getUrlFotoPengguna(Tbmuser tbmuser)  {
		try {
			return getUrlFotoPengguna(tbmuser, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonMedia.java:225");
		}
		return "";
	}

	/** Varian {@link #getUrlFotoPengguna(Tbmuser, Integer, Integer)} dengan dimensi thumbnail kecil tetap (152x114 piksel). */
	public static String getUrlFotoPenggunaKecil(Tbmuser tbmuser) throws Exception {
		return getUrlFotoPengguna(tbmuser, 152, 114);
	}

	/** Varian {@link #getUrlFotoPengguna(Tbmuser, Integer, Integer)} yang menerima parameter {@code request} untuk kompatibilitas pemanggil lama; parameter tersebut tidak dipakai (diabaikan). */
	public static String getUrlFotoPengguna(HttpServletRequest request, Tbmuser tbmuser, Integer height, Integer width)
			throws Exception {
		return getUrlFotoPengguna(tbmuser, height, width);
	}

	/** Varian {@link #tampilkanGambarKecil(GeneralValueObject, String, String)} dengan ukuran 62px dan perataan tengah. */
	public static A tampilkanGambarKecil(GeneralValueObject object) throws Exception {
		return tampilkanGambarKecil(object, "62px", "center");
	}

	/** Varian {@link #tampilkanGambar(String, String, String)} dengan ukuran 72px dan perataan tengah. */
	public static A tampilkanGambar(String url) throws Exception {
		return tampilkanGambar(url, "72px", "center");
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.tampilkanGambar}: membangun komponen ZK {@link A} (tautan/thumbnail) untuk menampilkan gambar dari {@code url} dengan {@code ukuran} dan perataan {@code align} tertentu. */
	public static A tampilkanGambar(String url, String ukuran, String align) throws Exception {
		return ProfileImageUtil.tampilkanGambar(url, ukuran, align);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.getUrlFotoPengguna}: membangun URL foto profil {@code tbmuser} dengan batasan dimensi {@code height}/{@code width} opsional. */
	public static String getUrlFotoPengguna(Tbmuser tbmuser, Integer height, Integer width) throws Exception {
		return ProfileImageUtil.getUrlFotoPengguna(tbmuser, height, width);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.tampilkanGambarKecil}: membangun komponen ZK {@link A} thumbnail kecil untuk foto/gambar milik {@code object} dengan {@code ukuran} dan perataan {@code align} tertentu. */
	public static A tampilkanGambarKecil(GeneralValueObject object, String ukuran, String align) throws Exception {
		return ProfileImageUtil.tampilkanGambarKecil(object, ukuran, align);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.preview}: mengembalikan URL pratinjau untuk {@code fileFoto}. */
	public static String preview(FileFoto fileFoto) throws Exception {
		return ProfileImageUtil.preview(fileFoto);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.preview}: menampilkan pratinjau {@code fileFoto} langsung ke komponen ZK {@code parent}. */
	public static void preview(FileFoto fileFoto, Component parent) throws Exception {
		ProfileImageUtil.preview(fileFoto, parent);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.loadFileFotoLangsung}: mengembalikan path berkas foto {@code tbmuser} dengan batasan dimensi opsional dan penanda apakah harus berupa gambar. */
	public static String loadFileFotoLangsung(Tbmuser tbmuser, Integer height, Integer width, Boolean berupaGambar)
			throws Exception {
		return ProfileImageUtil.loadFileFotoLangsung(tbmuser, height, width, berupaGambar);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.loadFotoPenggunaLangsung}: memuat foto {@code tbmuser} ke komponen {@link Image} {@code foto} yang sudah ada (atau membuat baru bila {@code null}) dengan dimensi tertentu. */
	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser, Image foto, Integer height, Integer width)
			throws Exception {
		return ProfileImageUtil.loadFotoPenggunaLangsung(tbmuser, foto, height, width);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.getFileFotoLangsungOld}: mengembalikan berkas foto {@code foto} tanpa batasan dimensi, dengan penanda apakah harus berupa gambar. */
	public static File getFileFotoLangsungOld(FileFoto foto, boolean berupaGambar) throws Exception {
		return ProfileImageUtil.getFileFotoLangsungOld(foto, berupaGambar);
	}

	/** Wrapper tipis ke {@code ProfileImageUtil.getFileFotoLangsungOld}: mengembalikan berkas foto {@code foto} dengan batasan dimensi {@code height}/{@code width} dan penanda apakah harus berupa gambar. */
	public static File getFileFotoLangsungOld(FileFoto foto, Integer height, Integer width, boolean berupaGambar)
			throws Exception {
		return ProfileImageUtil.getFileFotoLangsungOld(foto, height, width, berupaGambar);
	}

	/**
	 * Membangun URL media terenkripsi untuk foto suatu {@link FotoGambarItem} (foto item inventori)
	 * lewat {@link #getMedia(MediaParameter)}.
	 *
	 * @param fotoId id spesifik foto (bila item punya beberapa foto), boleh {@code null}
	 * @param item   id entitas item terkait
	 * @param height batasan tinggi tampilan, boleh {@code null}
	 * @param width  batasan lebar tampilan, boleh {@code null}
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke foto item
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoItem(Long fotoId, Long item, Integer height, Integer width) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(item.toString(), "nama", "foto", FotoGambarItem.class,
				"item", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Membangun URL media terenkripsi untuk foto suatu {@link FotoGambarProduk} (foto produk) lewat
	 * {@link #getMedia(MediaParameter)}.
	 *
	 * @param fotoId id spesifik foto, boleh {@code null}
	 * @param produk id entitas produk terkait
	 * @param height batasan tinggi tampilan, boleh {@code null}
	 * @param width  batasan lebar tampilan, boleh {@code null}
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke foto produk
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoProduk(Long fotoId, Long produk, Integer height, Integer width) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(produk.toString(), "nama", "foto", FotoGambarProduk.class,
				"produk", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Membangun URL media terenkripsi untuk gambar suatu {@link FotoGambarKopSurat} (kop surat)
	 * lewat {@link #getMedia(MediaParameter)}.
	 *
	 * @param fotoId   id spesifik foto, boleh {@code null}
	 * @param kopSurat id entitas kop surat terkait
	 * @param height   batasan tinggi tampilan, boleh {@code null}
	 * @param width    batasan lebar tampilan, boleh {@code null}
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke gambar kop surat
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoKopSurat(Long fotoId, Long kopSurat, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(kopSurat.toString(), "nama", "foto",
				FotoGambarKopSurat.class, "kopSurat", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Membangun URL media terenkripsi untuk gambar tanda tangan suatu {@link
	 * FotoGambarTandaTanganPejabat} (tanda tangan pejabat, mis. dipakai pada cetakan surat resmi)
	 * lewat {@link #getMedia(MediaParameter)}, tanpa batasan dimensi tampilan.
	 *
	 * @param fotoId  id spesifik foto, boleh {@code null}
	 * @param pejabat id entitas pejabat terkait
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke gambar tanda tangan
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoPejabat(Long fotoId, Long pejabat) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(pejabat.toString(), "nama", "foto",
				FotoGambarTandaTanganPejabat.class, "pejabat", null, null);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Implementasi kanonik pembangunan URL media tidak langsung: mengemas seluruh detail referensi
	 * dari {@code mediaParameter} (id entitas, nama properti berkas/media, nama kelas Java entitas,
	 * nama properti relasi, flag {@code usingId}, batasan dimensi opsional, penanda foto utama,
	 * dan id foto spesifik opsional) menjadi satu objek JSON, mengenkripsinya lewat
	 * {@code Common.desEncrypter.get().encrypt(...)}, lalu menyisipkannya sebagai parameter query
	 * {@code d} (URL-encoded) pada URL servlet {@code /AmbilMedia} di host aplikasi saat ini. Lihat
	 * catatan keamanan mengenai skema enkripsi ini pada Javadoc kelas.
	 *
	 * @param mediaParameter kumpulan parameter yang mendeskripsikan media yang akan diambil (entitas,
	 *                       properti, dimensi, dsb.)
	 * @return URL lengkap ke servlet {@code /AmbilMedia} siap dipakai sebagai {@code src} gambar atau
	 *         tautan unduhan
	 * @throws Exception diteruskan dari kegagalan enkripsi atau {@link URLEncoder#encode}
	 */
	public static String getMedia(MediaParameter mediaParameter) throws Exception {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("id", mediaParameter.getId());
		jsonObject.put("name", mediaParameter.getFilePropertyName());
		jsonObject.put("foto", mediaParameter.getMediaPropertyName());
		jsonObject.put("clazz", mediaParameter.getClazz().getName());
		jsonObject.put("property", mediaParameter.getProperty() + "");
		jsonObject.put("usingId", mediaParameter.getUsingId() + "");

		if (mediaParameter.getWidth() != null)
			jsonObject.put("width", mediaParameter.getWidth() + "");
		if (mediaParameter.getHeight() != null)
			jsonObject.put("height", mediaParameter.getHeight() + "");
		if (mediaParameter.getFotoUtama())
			jsonObject.put("fotoUtama", "true");
		if (mediaParameter.getFotoId() != null)
			jsonObject.put("foto_id", mediaParameter.getFotoId() + "");

		String encript = Common.desEncrypter.get().encrypt(jsonObject.toString());
		return Common.getRequestHostWithProtocol() + "/AmbilMedia?d=" + URLEncoder.encode(encript, "UTF-8");
	}

	/**
	 * Memuat konfigurasi {@code ais_host} (URL dasar aplikasi) dari berkas properti eksternal
	 * {@code /opt/.g/.h/portlet.properties}, dipakai pada konteks integrasi portlet/eksternal di
	 * luar siklus permintaan HTTP normal (di mana URL host tidak dapat diturunkan dari request
	 * aktif). Bila berkas belum ada, dibuat otomatis dengan nilai default
	 * {@code ais_host=http://localhost:8080/ais/}. Nilai yang dibaca disalin ke
	 * {@link System#getProperties()} dengan kunci yang sama agar dapat diakses kode lain lewat
	 * {@link System#getProperty(String)}. Proses ini hanya benar-benar dijalankan sekali per siklus
	 * hidup JVM, dijaga oleh flag {@link #hasInitConfig}; stream berkas selalu ditutup di blok
	 * {@code finally} untuk mencegah kebocoran deskriptor berkas. Kegagalan (mis. berkas tidak
	 * dapat ditulis/dibaca) ditampilkan ke admin lewat {@link Common#tampilErrorJikaAdmin(Exception)}
	 * tanpa dilempar keluar.
	 */
	public static void initConfig() {
		if (!hasInitConfig) {
			File configFile = new File("/opt/.g/.h/portlet.properties");
			FileWriter fw = null;
			FileInputStream fis = null;

			try {
				if (!configFile.exists()) {
					configFile.getParentFile().mkdirs();
					configFile.createNewFile();

					Properties properties = new Properties();
					properties.put("ais_host", "http://localhost:8080/ais/");

					fw = new FileWriter(configFile);
					properties.store(fw, "");
				}

				Properties properties = new Properties();
				fis = new FileInputStream(configFile);
				properties.load(fis);

				String rootUrl = properties.getProperty("ais_host");
				if (rootUrl != null) {
					System.getProperties().put("ais_host", rootUrl);
				}

				hasInitConfig = true;
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				// Perbaikan Stream tidak tertutup
				if (fw != null) {
					try {
						fw.close();
					} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMedia.java:372");
					}
				}
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonMedia.java:378");
					}
				}
			}
		}
	}

	/**
	 * Membangun URL langsung (tanpa enkripsi referensi, berbeda dari {@link #getMedia}) ke servlet
	 * {@code /AmbilMediaItem} untuk menampilkan gambar item berdasarkan id-nya secara langsung pada
	 * parameter query {@code id}.
	 *
	 * @param item   id item yang gambarnya diambil (disisipkan apa adanya di URL, tidak dienkripsi)
	 * @param height batasan tinggi tampilan
	 * @param width  batasan lebar tampilan
	 * @param inc    diterima untuk kompatibilitas signature, tidak dipakai dalam pembangunan URL
	 * @return URL servlet {@code /AmbilMediaItem} siap pakai
	 * @throws Exception dideklarasikan untuk konsistensi antar-method sejenis; tidak dilempar oleh
	 *                    implementasi saat ini
	 */
	public static String getMediaItem(Long item, Integer height, Integer width, Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilMediaItem?id=" + item + "&height=" + height + "&width="
				+ width + "&img=.jpg";
	}

	/** Varian {@link #getMediaItem(Long, Integer, Integer, Boolean)} untuk gambar produk, mengarah ke servlet {@code /AmbilMediaProduk} dengan id produk disisipkan langsung (tidak dienkripsi). */
	public static String getMediaProduk(Long produk, Integer height, Integer width, Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilMediaProduk?id=" + produk + "&height=" + height + "&width="
				+ width + "&img=.jpg";
	}

	/**
	 * Membangun URL unduhan berkas umum ke servlet {@code /AmbilFile}. Berbeda dari
	 * {@link #getMediaItem}/{@link #getMediaProduk} yang menyisipkan id apa adanya, method ini
	 * mengenkripsi id (lewat {@code Common.desEncrypter.get().encrypt(...)}, pola sama seperti
	 * {@link #getMedia(MediaParameter)}) sebelum di-URL-encode dan disisipkan sebagai parameter
	 * {@code id}, disertai nama kelas entitas ({@code clazz}) sebagai parameter terpisah yang
	 * di-URL-encode tanpa dienkripsi.
	 *
	 * @param id    id entitas berkas yang akan diunduh
	 * @param clazz nama lengkap kelas Java entitas tempat berkas tersimpan, dikirim sebagai teks
	 *              polos (ter-URL-encode, tidak dienkripsi)
	 * @return URL servlet {@code /AmbilFile} siap pakai
	 * @throws Exception diteruskan dari kegagalan enkripsi atau {@link URLEncoder#encode}
	 */
	public static String getFile(Long id, String clazz) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilFile?id="
				+ URLEncoder.encode(Common.desEncrypter.get().encrypt(id.toString()), "UTF-8") + "&clazz="
				+ URLEncoder.encode(clazz, "UTF-8");
	}

	/**
	 * Membangun URL langsung ke servlet {@code /AmbilImageItemPerHalaman} untuk menampilkan gambar
	 * item yang terkait dengan satu "halaman" tertentu (mis. galeri gambar bertingkat per konteks
	 * data) dan opsional id data spesifik.
	 *
	 * @param item    id item
	 * @param idData  id data spesifik dalam konteks halaman tersebut, boleh {@code null} (parameter
	 *                {@code idData} dihilangkan dari URL bila {@code null})
	 * @param halaman nama/kode halaman, di-URL-encode sebelum disisipkan
	 * @param height  batasan tinggi tampilan
	 * @param width   batasan lebar tampilan
	 * @param inc     diterima untuk kompatibilitas signature, tidak dipakai
	 * @return URL servlet {@code /AmbilImageItemPerHalaman} siap pakai
	 * @throws Exception diteruskan dari {@link URLEncoder#encode}
	 */
	public static String getImageItemPerHalaman(Long item, Long idData, String halaman, Integer height, Integer width,
			Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilImageItemPerHalaman?id=" + item + "&halaman="
				+ URLEncoder.encode(halaman, "UTF-8") + (idData == null ? "" : "&idData=" + idData) + "&height="
				+ height + "&width=" + width + "&img=.jpg";
	}

	/**
	 * Membangun URL langsung ke servlet {@code /AmbilGaleriFotoImage} untuk menampilkan satu gambar
	 * dalam galeri foto item, dengan opsional id data spesifik.
	 *
	 * @param item   id item pemilik galeri
	 * @param idData id data/foto spesifik dalam galeri, boleh {@code null}
	 * @param height batasan tinggi tampilan
	 * @param width  batasan lebar tampilan
	 * @param inc    diterima untuk kompatibilitas signature, tidak dipakai
	 * @return URL servlet {@code /AmbilGaleriFotoImage} siap pakai
	 * @throws Exception dideklarasikan untuk konsistensi antar-method sejenis; tidak dilempar oleh
	 *                    implementasi saat ini
	 */
	public static String getGaleriFotoImage(Long item, Long idData, Integer height, Integer width, Boolean inc)
			throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilGaleriFotoImage?id=" + item
				+ (idData == null ? "" : "&idData=" + idData) + "&height=" + height + "&width=" + width + "&img=.jpg";
	}

	/** Membangun URL langsung ke servlet {@code /AmbilLampiranItem} untuk mengunduh lampiran item berdasarkan id-nya. */
	public static String getLampiranItem(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranItem?id=" + id;
	}

	/** Membangun URL langsung ke servlet {@code /AmbilLampiranInformasiPerpustakaan} untuk mengunduh lampiran informasi perpustakaan berdasarkan id-nya. */
	public static String getLampiranInformasiPerpustakaan(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranInformasiPerpustakaan?id=" + id;
	}

	/** Membangun URL langsung ke servlet {@code /AmbilLampiranInformasiRab} untuk mengunduh lampiran informasi RAB (Rencana Anggaran Biaya) berdasarkan id-nya. */
	public static String getLampiranInformasiRab(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranInformasiRab?id=" + id;
	}

	/**
	 * Membangun URL media terenkripsi untuk gambar suatu {@link FotoGambarSuratMasuk} (lampiran
	 * surat masuk) lewat {@link #getMedia(MediaParameter)}.
	 *
	 * @param fotoId     id spesifik foto, boleh {@code null}
	 * @param suratMasuk id entitas surat masuk terkait
	 * @param height     batasan tinggi tampilan, boleh {@code null}
	 * @param width      batasan lebar tampilan, boleh {@code null}
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke gambar surat masuk
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoSuratMasuk(Long fotoId, Long suratMasuk, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(suratMasuk.toString(), "nama", "foto",
				FotoGambarSuratMasuk.class, "suratMasuk", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Membangun URL media terenkripsi untuk gambar suatu {@link FotoGambarSuratKeluar} (lampiran
	 * surat keluar) lewat {@link #getMedia(MediaParameter)}.
	 *
	 * @param fotoId      id spesifik foto, boleh {@code null}
	 * @param suratKeluar id entitas surat keluar terkait
	 * @param height      batasan tinggi tampilan, boleh {@code null}
	 * @param width       batasan lebar tampilan, boleh {@code null}
	 * @return URL servlet {@code /AmbilMedia} berisi referensi terenkripsi ke gambar surat keluar
	 * @throws Exception diteruskan dari {@link #getMedia(MediaParameter)}
	 */
	public static String getUrlFotoSuratKeluar(Long fotoId, Long suratKeluar, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(suratKeluar.toString(), "nama", "foto",
				FotoGambarSuratKeluar.class, "suratKeluar", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	/**
	 * Menebak tipe MIME sebuah berkas berdasarkan ekstensi nama berkasnya (case-insensitive).
	 * Mencari lebih dulu di peta cepat {@link #MIME_MAP} untuk ekstensi umum (dokumen kantor,
	 * gambar, PDF, HTML/XML); bila ekstensi tidak dikenal di peta tersebut, jatuh ke
	 * {@link URLConnection#guessContentTypeFromName(String)} bawaan JDK sebagai penebak cadangan.
	 * Bila keduanya tidak berhasil menentukan tipe, dikembalikan {@code application/octet-stream}
	 * sebagai fallback generik.
	 *
	 * @param file berkas yang tipe MIME-nya akan ditebak; boleh {@code null}
	 * @return tipe MIME yang ditebak, atau {@code application/octet-stream} bila {@code file}
	 *         {@code null}/tidak memiliki ekstensi yang dikenali/tidak dapat ditebak
	 */
	public static String getMime(File file) {
		if (file == null)
			return "application/octet-stream";

		String fileName = file.getName().toLowerCase();
		int dotIndex = fileName.lastIndexOf('.');

		if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
			String extension = fileName.substring(dotIndex + 1);
			String mime = MIME_MAP.get(extension);

			if (mime != null) {
				return mime;
			}
		}

		String guess = URLConnection.guessContentTypeFromName(file.getName());
		return guess != null ? guess : "application/octet-stream";
	}
}
