package ais.action.servlet.api;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * <h3>Pembaca dokumen terlindungi: satu halaman, sebagai GAMBAR ber-watermark.</h3>
 *
 * <p>Dipakai fitur Pustaka dan Repository yang berstatus <b>baca-saja</b> karena
 * terikat hak cipta. Klien tidak pernah menerima berkas PDF utuh — hanya
 * gambar satu halaman yang diminta, sehingga tidak ada berkas yang bisa
 * disimpan lalu disebarluaskan apa adanya.</p>
 *
 * <p><b>Watermark identitas pembaca</b> (nama + nomor induk + waktu akses)
 * digambar MELEKAT pada piksel halaman, bukan sebagai lapisan terpisah di
 * klien. Dengan begitu tangkapan layar maupun foto layar tetap membawa
 * identitas peminta, sehingga kebocoran dapat ditelusuri.</p>
 *
 * <p><b>Batas kejujuran:</b> apa pun yang bisa dibaca di layar pada dasarnya
 * sudah sampai ke mata pengguna; watermark menghalangi penyebaran anonim dan
 * penyalinan kasual, BUKAN menjadikan penyalinan mustahil. Karena itu berkas
 * sengaja tidak pernah ditulis ke folder publik (pola lama menulis hasil
 * render ke {@code /tmp} webapp sehingga gambarnya dapat diambil siapa pun
 * lewat URL tanpa token).</p>
 */
public final class PembacaTerlindungiUtil {

	private PembacaTerlindungiUtil() {
	}

	/** Resolusi render. Cukup terbaca di layar, tidak cukup nyaman untuk cetak. */
	private static final int DPI = 110;

	/** Batas aman jumlah halaman yang boleh diminta sekali panggil. */
	public static final int MAKS_HALAMAN_SEKALI_MINTA = 1;

	/**
	 * Render satu halaman PDF menjadi JPEG ber-watermark, dikembalikan sebagai
	 * Base64 supaya dapat ditempelkan langsung pada respons JSON API.
	 *
	 * @param berkasPdf berkas sumber di server (tidak pernah dikirim apa adanya)
	 * @param halaman   nomor halaman mulai 1
	 * @param identitas teks identitas pembaca, mis. "Ahmad Habibi · 2510110004"
	 * @return Base64 JPEG, atau null bila halaman di luar jangkauan
	 */
	public static String halamanSebagaiBase64(File berkasPdf, int halaman, String identitas)
			throws Exception {
		PDDocument dokumen = null;
		try {
			dokumen = PDDocument.load(berkasPdf);
			@SuppressWarnings("rawtypes")
			List halamanSemua = dokumen.getDocumentCatalog().getAllPages();
			if (halaman < 1 || halaman > halamanSemua.size()) {
				return null;
			}
			PDPage pd = (PDPage) halamanSemua.get(halaman - 1);
			BufferedImage gambar = pd.convertToImage(BufferedImage.TYPE_INT_RGB, DPI);
			gambarWatermark(gambar, identitas);

			ByteArrayOutputStream keluaran = new ByteArrayOutputStream();
			ImageIO.write(gambar, "jpg", keluaran);
			return new String(Base64.encodeBase64(keluaran.toByteArray()), "UTF-8");
		} finally {
			if (dokumen != null) {
				try {
					dokumen.close();
				} catch (Exception abaikan) {
					// Dokumen sudah tertutup/rusak; tidak ada lagi yang bisa dilakukan.
				}
			}
		}
	}

	/** Jumlah halaman dokumen; dipakai klien untuk navigasi. */
	public static int jumlahHalaman(File berkasPdf) throws Exception {
		PDDocument dokumen = null;
		try {
			dokumen = PDDocument.load(berkasPdf);
			return dokumen.getNumberOfPages();
		} finally {
			if (dokumen != null) {
				try {
					dokumen.close();
				} catch (Exception abaikan) {
					// Lihat catatan di atas.
				}
			}
		}
	}

	/**
	 * Tulis identitas pembaca secara diagonal berulang menutupi halaman.
	 *
	 * <p>Sengaja BERULANG dan menyilang: satu tanda di pojok mudah dipotong,
	 * sedangkan pola menyilang membuat pemotongan merusak isi yang ingin
	 * disalin. Transparansi dijaga cukup rendah agar teks halaman tetap
	 * nyaman dibaca — perlindungan tidak boleh mengorbankan pembaca yang sah.</p>
	 */
	private static void gambarWatermark(BufferedImage gambar, String identitas) {
		if (identitas == null || identitas.trim().isEmpty()) {
			identitas = "Dokumen terlindungi";
		}
		String stempel = identitas + "  ·  "
				+ new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());

		Graphics2D g = gambar.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			int lebar = gambar.getWidth();
			int tinggi = gambar.getHeight();
			int ukuranFont = Math.max(12, lebar / 45);
			g.setFont(new Font("SansSerif", Font.BOLD, ukuranFont));
			g.setColor(new Color(120, 120, 120));
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));

			AffineTransform asli = g.getTransform();
			g.rotate(Math.toRadians(-30), lebar / 2.0, tinggi / 2.0);

			int lebarTeks = g.getFontMetrics().stringWidth(stempel);
			int jarakX = lebarTeks + ukuranFont * 4;
			int jarakY = ukuranFont * 7;

			// Mulai dari luar bidang supaya rotasi tidak menyisakan sudut kosong.
			for (int y = -tinggi; y < tinggi * 2; y += jarakY) {
				for (int x = -lebar; x < lebar * 2; x += jarakX) {
					g.drawString(stempel, x, y);
				}
			}
			g.setTransform(asli);

			// Satu baris tegas di kaki halaman: mudah terbaca saat difoto,
			// menegaskan bahwa salinan ini terikat pada satu pembaca.
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
			g.setColor(new Color(40, 40, 40));
			g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(10, ukuranFont * 2 / 3)));
			String kaki = "Hanya untuk dibaca oleh " + stempel
					+ "  —  dilarang menggandakan atau menyebarluaskan";
			g.drawString(kaki, ukuranFont, tinggi - ukuranFont);
		} finally {
			g.dispose();
		}
	}
}
