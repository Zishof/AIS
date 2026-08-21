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

public class CommonMedia {

	public static final int IMG_WIDTH = 20;
	public static final int IMG_HEIGHT = 20;

	public static final int IMG_WIDTH_MEDIUM = 90;
	public static final int IMG_HEIGHT_MEDIUM = 100;

	private static File mediaDic = null;
	public static File MEDIA_DIR = null;
	public static String prefix = "";
	public static String path = null;

	private static volatile Boolean hasInitConfig = false;

	// Map untuk efisiensi mime-type lookup O(1)
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

	public static File resize(File originalFile) {
		return resize(originalFile, IMG_WIDTH, IMG_HEIGHT);
	}

	public static File resize(File originalFile, double width, double height) {
		return resize(originalFile, (int) width, (int) height);
	}

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

	public static File getFileFotoDenganFile(FileFoto foto, File file) throws Exception {
		if (file != null && file.exists()) {
			return file;
		} else {
			Common.copy(foto.ambilFile(), file);
			return file;
		}
	}

	public static AImage getFotoLangsungOld(FileFoto foto, Integer height, Integer width) throws Exception {
		return new AImage(getFileFotoLangsungOld(foto, height, width, true));
	}

	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser) throws Exception {
		return loadFotoPenggunaLangsung(tbmuser, null, IMG_HEIGHT_MEDIUM, IMG_WIDTH_MEDIUM);
	}

	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser, Image foto) throws Exception {
		return loadFotoPenggunaLangsung(tbmuser, foto, IMG_HEIGHT_MEDIUM, IMG_WIDTH_MEDIUM);
	}

	public static String loadPathFileFotoLangsung(Tbmuser tbmuser) throws Exception {
		return loadFileFotoLangsung(tbmuser);
	}

	public static String loadFileFotoLangsung(Tbmuser tbmuser) throws Exception {
		return loadFileFotoLangsung(tbmuser, null, null, true);
	}

	public static String getUrlFotoPengguna(Tbmuser tbmuser)  {
		try {
			return getUrlFotoPengguna(tbmuser, null, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonMedia.java:225");
		}
		return "";
	}

	public static String getUrlFotoPenggunaKecil(Tbmuser tbmuser) throws Exception {
		return getUrlFotoPengguna(tbmuser, 152, 114);
	}

	public static String getUrlFotoPengguna(HttpServletRequest request, Tbmuser tbmuser, Integer height, Integer width)
			throws Exception {
		return getUrlFotoPengguna(tbmuser, height, width);
	}

	public static A tampilkanGambarKecil(GeneralValueObject object) throws Exception {
		return tampilkanGambarKecil(object, "62px", "center");
	}

	public static A tampilkanGambar(String url) throws Exception {
		return tampilkanGambar(url, "72px", "center");
	}

	public static A tampilkanGambar(String url, String ukuran, String align) throws Exception {
		return ProfileImageUtil.tampilkanGambar(url, ukuran, align);
	}

	public static String getUrlFotoPengguna(Tbmuser tbmuser, Integer height, Integer width) throws Exception {
		return ProfileImageUtil.getUrlFotoPengguna(tbmuser, height, width);
	}

	public static A tampilkanGambarKecil(GeneralValueObject object, String ukuran, String align) throws Exception {
		return ProfileImageUtil.tampilkanGambarKecil(object, ukuran, align);
	}

	public static String preview(FileFoto fileFoto) throws Exception {
		return ProfileImageUtil.preview(fileFoto);
	}

	public static void preview(FileFoto fileFoto, Component parent) throws Exception {
		ProfileImageUtil.preview(fileFoto, parent);
	}

	public static String loadFileFotoLangsung(Tbmuser tbmuser, Integer height, Integer width, Boolean berupaGambar)
			throws Exception {
		return ProfileImageUtil.loadFileFotoLangsung(tbmuser, height, width, berupaGambar);
	}

	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser, Image foto, Integer height, Integer width)
			throws Exception {
		return ProfileImageUtil.loadFotoPenggunaLangsung(tbmuser, foto, height, width);
	}

	public static File getFileFotoLangsungOld(FileFoto foto, boolean berupaGambar) throws Exception {
		return ProfileImageUtil.getFileFotoLangsungOld(foto, berupaGambar);
	}

	public static File getFileFotoLangsungOld(FileFoto foto, Integer height, Integer width, boolean berupaGambar)
			throws Exception {
		return ProfileImageUtil.getFileFotoLangsungOld(foto, height, width, berupaGambar);
	}

	public static String getUrlFotoItem(Long fotoId, Long item, Integer height, Integer width) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(item.toString(), "nama", "foto", FotoGambarItem.class,
				"item", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	public static String getUrlFotoProduk(Long fotoId, Long produk, Integer height, Integer width) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(produk.toString(), "nama", "foto", FotoGambarProduk.class,
				"produk", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	public static String getUrlFotoKopSurat(Long fotoId, Long kopSurat, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(kopSurat.toString(), "nama", "foto",
				FotoGambarKopSurat.class, "kopSurat", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	public static String getUrlFotoPejabat(Long fotoId, Long pejabat) throws Exception {
		MediaParameter mediaParameter = new MediaParameter(pejabat.toString(), "nama", "foto",
				FotoGambarTandaTanganPejabat.class, "pejabat", null, null);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

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

	public static String getMediaItem(Long item, Integer height, Integer width, Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilMediaItem?id=" + item + "&height=" + height + "&width="
				+ width + "&img=.jpg";
	}

	public static String getMediaProduk(Long produk, Integer height, Integer width, Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilMediaProduk?id=" + produk + "&height=" + height + "&width="
				+ width + "&img=.jpg";
	}

	public static String getFile(Long id, String clazz) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilFile?id="
				+ URLEncoder.encode(Common.desEncrypter.get().encrypt(id.toString()), "UTF-8") + "&clazz="
				+ URLEncoder.encode(clazz, "UTF-8");
	}

	public static String getImageItemPerHalaman(Long item, Long idData, String halaman, Integer height, Integer width,
			Boolean inc) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilImageItemPerHalaman?id=" + item + "&halaman="
				+ URLEncoder.encode(halaman, "UTF-8") + (idData == null ? "" : "&idData=" + idData) + "&height="
				+ height + "&width=" + width + "&img=.jpg";
	}

	public static String getGaleriFotoImage(Long item, Long idData, Integer height, Integer width, Boolean inc)
			throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilGaleriFotoImage?id=" + item
				+ (idData == null ? "" : "&idData=" + idData) + "&height=" + height + "&width=" + width + "&img=.jpg";
	}

	public static String getLampiranItem(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranItem?id=" + id;
	}

	public static String getLampiranInformasiPerpustakaan(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranInformasiPerpustakaan?id=" + id;
	}

	public static String getLampiranInformasiRab(Long id) throws Exception {
		return Common.getRequestHostWithProtocol() + "/AmbilLampiranInformasiRab?id=" + id;
	}

	public static String getUrlFotoSuratMasuk(Long fotoId, Long suratMasuk, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(suratMasuk.toString(), "nama", "foto",
				FotoGambarSuratMasuk.class, "suratMasuk", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

	public static String getUrlFotoSuratKeluar(Long fotoId, Long suratKeluar, Integer height, Integer width)
			throws Exception {
		MediaParameter mediaParameter = new MediaParameter(suratKeluar.toString(), "nama", "foto",
				FotoGambarSuratKeluar.class, "suratKeluar", height, width);
		mediaParameter.setFotoId(fotoId);
		return getMedia(mediaParameter);
	}

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
