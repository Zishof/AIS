package ais.action.servlet;

import static nl.captcha.Captcha.NAME;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ais.common.Common;
import ais.common.ConstantValues;
import nl.captcha.Captcha;
import nl.captcha.Captcha.Builder;
import nl.captcha.backgrounds.BackgroundProducer;
import nl.captcha.backgrounds.GradiatedBackgroundProducer;
import nl.captcha.gimpy.FishEyeGimpyRenderer;
import nl.captcha.gimpy.GimpyRenderer;
import nl.captcha.noise.NoiseProducer;
import nl.captcha.noise.StraightLineNoiseProducer;
import nl.captcha.servlet.CaptchaServletUtil;
import nl.captcha.text.producer.TextProducer;

/**
 * Servlet penghasil gambar CAPTCHA lokal (simplecaptcha) untuk formulir login, dipetakan
 * sebagai {@code <img src="...">} pada halaman login lewat {@code web.xml}. Kelas ini HANYA
 * membangkitkan tantangan (gambar + jawaban) dan menyimpannya di HTTP session; validasi
 * jawaban yang dikirim pengguna dilakukan di TEMPAT LAIN, yaitu
 * {@code ais.common.SecurityFilter} pada jalur pemrosesan login.
 *
 * <p><b>Alur validasi (di luar kelas ini, untuk konteks):</b> {@code SecurityFilter}
 * membaca kembali {@link Captcha} yang disimpan servlet ini dari session lewat
 * {@link Captcha#NAME}, membandingkannya dengan parameter {@code answer} lewat
 * {@link Captcha#isCorrect(String)}, dan hanya melanjutkan proses login bila cocok — TAPI
 * pengecekan itu dibungkus try/catch yang, bila melempar exception apa pun, hanya mencatat ke
 * audit dan MELANJUTKAN proses login tanpa {@code return} (fail-open), alih-alih menolak
 * permintaan. Lihat javadoc method captcha pada {@code SecurityFilter} untuk detail; dalam
 * kondisi normal {@link Captcha#isCorrect(String)} tidak melempar exception untuk jawaban
 * {@code null}/kosong (implementasinya berupa {@code String.equals} yang null-safe pada sisi
 * kanan), sehingga celah fail-open ini hanya teraktivasi oleh kondisi tak terduga lain (mis.
 * exception dari {@code getSession()} atau perubahan behavior pustaka di masa depan) — namun
 * tetap merupakan cacat desain penanganan galat yang seharusnya fail-closed untuk gerbang
 * keamanan seperti ini. Seluruh mekanisme captcha lokal ini hanya aktif bila
 * {@code ConstantValues.aktifkanCaptchaLokal} bernilai true; bila false, tidak ada captcha
 * sama sekali pada jalur login (ini pengaturan yang disengaja, bukan bug).</p>
 *
 * @see HttpServlet
 */
public class CaptchaServlet extends HttpServlet {

	/**
	 * Versi serialisasi tetap untuk kompatibilitas {@link java.io.Serializable} servlet ini.
	 * Nilai literal ini (bukan {@code 1L} seperti servlet lain di paket ini) dipertahankan
	 * apa adanya dari implementasi asli.
	 */
	private static final long serialVersionUID = 40913456229L;

	/**
	 * Membangkitkan satu gambar CAPTCHA baru sesuai konfigurasi ukuran/noise/gimpy renderer/
	 * background/text producer dari {@link ConstantValues} (masing-masing dimuat secara
	 * reflektif lewat {@link Class#forName} bila nama kelasnya diisi di konfigurasi), lalu
	 * menyimpan objek {@link Captcha} yang dihasilkan ke HTTP session di bawah key
	 * {@link Captcha#NAME} agar dapat dicocokkan kembali oleh {@code SecurityFilter} saat
	 * formulir login disubmit, dan menuliskan gambarnya ke respons.
	 *
	 * <p>Bila konfigurasi kustom gagal dibangun (mis. nama kelas di {@link ConstantValues}
	 * salah/tidak ditemukan, atau constructor default tidak ada), exception ditangkap dan
	 * captcha fallback dengan parameter baku (300x70, {@link GradiatedBackgroundProducer},
	 * {@link StraightLineNoiseProducer}, {@link FishEyeGimpyRenderer}) dibangkitkan sebagai
	 * gantinya sehingga halaman login tetap dapat menampilkan captcha; kegagalan tetap dicatat
	 * lewat {@link Common#tampilErrorJikaAdmin(Exception)}. Setiap pemanggilan method ini
	 * SELALU membangkitkan captcha baru (menimpa yang lama di session bila ada) — komentar
	 * Javadoc asli method ini menyebut perilaku "pakai ulang bila sudah ada di session", namun
	 * implementasi aktual tidak pernah memeriksa keberadaan captcha lama sebelum membuat yang
	 * baru.</p>
	 *
	 * @param req  request HTTP masuk; dipakai untuk memperoleh/membuat {@link HttpSession}
	 *             tempat captcha disimpan
	 * @param resp response HTTP keluar; badannya diisi gambar captcha lewat
	 *             {@link CaptchaServletUtil#writeImage(HttpServletResponse, java.awt.image.BufferedImage)}
	 * @throws ServletException tidak pernah dilempar keluar pada praktiknya karena kegagalan
	 *                          konfigurasi ditangani lewat fallback; dipertahankan karena tanda
	 *                          tangan {@link HttpServlet#doGet}
	 * @throws IOException      bila penulisan gambar ke respons gagal
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		HttpSession session = req.getSession();

		Captcha captcha;
		try {
			Builder builder = new Captcha.Builder(Integer.parseInt(ConstantValues.aktifkanCaptchaLokalLebar),
					Integer.parseInt(ConstantValues.aktifkanCaptchaLokalTinggi));

			if (!ConstantValues.aktifkanCaptchaLokalNoice.trim().isEmpty()) {
				builder.addNoise((NoiseProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalNoice).newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalRender.trim().isEmpty()) {
				builder.gimp((GimpyRenderer) Class.forName(ConstantValues.aktifkanCaptchaLokalRender).newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalBackground.trim().isEmpty()) {
				builder.addBackground((BackgroundProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalBackground)
						.newInstance());
			}

			if (!ConstantValues.aktifkanCaptchaLokalText.trim().isEmpty()) {
				builder.addText((TextProducer) Class.forName(ConstantValues.aktifkanCaptchaLokalText).newInstance());
			}

			builder.addBorder();

			captcha = builder.build();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			captcha = new Captcha.Builder(300, 70).addText().addBackground(new GradiatedBackgroundProducer())
					.addNoise(new StraightLineNoiseProducer()).gimp(new FishEyeGimpyRenderer()).addBorder().build();
			Common.tampilErrorJikaAdmin(e); 
		} // Required.
		session.setAttribute(NAME, captcha);
		CaptchaServletUtil.writeImage(resp, captcha.getImage());
	}
}
