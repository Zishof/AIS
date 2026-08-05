package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;

/**
 * <h3>DashboardCardHelper — Perkakas Tampilan Dashboard yang Dapat Dipakai Ulang</h3>
 *
 * <p><b>Untuk apa kelas ini:</b> Menyediakan potongan tampilan (UI) standar yang SERAGAM untuk
 * semua dashboard/monitor di aplikasi, sehingga setiap dashboard baru tidak perlu menulis ulang gaya
 * kartu angka, bingkai panel, maupun tata letak yang otomatis rapi di layar HP maupun komputer. Tujuan
 * utamanya adalah <b>reuse</b> (pakai ulang) agar tampilan konsisten dan mudah dirawat: bila suatu saat
 * gaya kartu atau panel ingin diseragamkan/diperbarui, cukup diubah di satu tempat (kelas ini) dan
 * seluruh dashboard ikut berubah.</p>
 *
 * <p><b>Isi utama (tiga fungsi):</b></p>
 * <ol>
 *   <li>{@link #panel(Component, String, String)} — membuat satu "kotak" (Panel ZK) ber-judul yang
 *       bisa dilipat, sekaligus menampilkan kalimat penjelasan sederhana di bagian atas isinya, lalu
 *       mengembalikan area isi ({@link Panelchildren}) tempat pemanggil menaruh grafik/tabel.</li>
 *   <li>{@link #kartu(String, String, String, String)} — menghasilkan HTML satu "kartu angka" (KPI)
 *       berisi judul kecil, angka besar berwarna, dan keterangan singkat.</li>
 *   <li>{@link #barisKartu(String...)} dan {@link #barisChart(String...)} — membungkus beberapa kartu
 *       atau beberapa grafik dalam satu baris yang otomatis <i>turun ke bawah</i> (wrap) saat layar
 *       sempit, sehingga nyaman dibaca di HP maupun desktop tanpa kode tambahan.</li>
 * </ol>
 *
 * <p><b>Prinsip desain (best practice):</b> memakai CSS flexbox dengan {@code flex-wrap} agar
 * responsif tanpa media-query rumit; warna aksen lembut dan bayangan tipis agar "enak dipandang";
 * penjelasan panel memakai bahasa sangat sederhana (bukan istilah teknis) supaya pengguna yang sama
 * sekali awam teknologi tetap paham fungsi tiap bagian. Semua grafik yang ditaruh di dalamnya
 * disarankan memakai {@link HtmlChartHelper} (murni HTML/CSS/SVG, tanpa JFreeChart).</p>
 *
 * <p><b>Keamanan teks:</b> seluruh teks judul/nilai/keterangan di-<i>escape</i> agar karakter khusus
 * ({@code < > & " '}) tidak merusak struktur HTML. Kelas ini {@code final} dan hanya berisi metode
 * statik (tanpa state), jadi aman dipakai bersama antar-thread.</p>
 */
public final class DashboardCardHelper {

	private DashboardCardHelper() {
	}

	/**
	 * Membuat satu "kotak" panel ber-judul (dapat dilipat) beserta kalimat penjelasan sederhana di
	 * bagian atas, lalu mengembalikan area isi tempat pemanggil menaruh konten (grafik/tabel/kartu).
	 *
	 * @param induk               komponen induk tempat panel dipasang (mis. Vbox body dashboard).
	 * @param judul               judul panel yang tampil di bar atas.
	 * @param penjelasanSederhana 1–2 kalimat bahasa awam tentang guna panel; bila null/kosong tidak
	 *                            ditampilkan.
	 * @return {@link Panelchildren} area isi panel; pemanggil tinggal {@code setParent}-kan
	 *         komponennya (mis. {@code new MyHtml(...)} atau grid) ke sini.
	 */
	public static Panelchildren panel(Component induk, String judul, String penjelasanSederhana) {
		Panel panel = new Panel();
		panel.setTitle(judul == null ? "" : judul);
		panel.setBorder("normal");
		panel.setCollapsible(true);
		panel.setStyle("background:#ffffff;border-radius:14px;overflow:hidden;margin-bottom:12px;"
				+ "box-shadow:0 1px 6px rgba(15,23,42,0.06);");
		panel.setParent(induk);

		Panelchildren isi = new Panelchildren();
		isi.setStyle("padding:14px;background:#f8fafc;");
		isi.setParent(panel);

		if (penjelasanSederhana != null && penjelasanSederhana.trim().length() > 0) {
			Html ket = new Html("<div style=\"font-size:12.5px;color:#334155;line-height:1.55;margin-bottom:12px;"
					+ "background:#eff6ff;border-left:4px solid #3b82f6;border-radius:8px;padding:9px 13px;\">"
					+ esc(penjelasanSederhana) + "</div>");
			ket.setParent(isi);
		}
		return isi;
	}

	/**
	 * Menghasilkan HTML satu "kartu angka" (KPI) — judul kecil di atas, angka besar berwarna di
	 * tengah, dan keterangan singkat di bawah. Kartu sudah mengandung {@code flex} sehingga langsung
	 * rapi ketika dibungkus {@link #barisKartu(String...)}.
	 *
	 * @param judul      label singkat kartu (mis. "Total Sisa").
	 * @param nilai      angka/teks utama (mis. "Rp 12.500.000").
	 * @param keterangan penjelasan singkat di bawah angka (mis. "dana yang masih tersedia").
	 * @param warna      warna aksen (hex, mis. {@code #15803d}); null → biru bawaan.
	 * @return potongan HTML kartu (dipakai bersama {@link #barisKartu(String...)}).
	 */
	public static String kartu(String judul, String nilai, String keterangan, String warna) {
		String c = warna == null || warna.trim().length() == 0 ? "#1d4ed8" : warna;
		return "<div style=\"background:#ffffff;border:1px solid #e2e8f0;border-top:4px solid " + c + ";"
				+ "border-radius:14px;padding:14px 16px;box-shadow:0 2px 10px rgba(15,23,42,0.06);"
				+ "flex:1 1 170px;min-width:158px;box-sizing:border-box;\">"
				+ "<div style=\"font-size:11px;color:#64748b;font-weight:700;text-transform:uppercase;letter-spacing:.3px;\">"
				+ esc(judul) + "</div>"
				+ "<div style=\"font-size:22px;font-weight:800;color:" + c + ";margin-top:6px;line-height:1.15;\">"
				+ esc(nilai) + "</div>"
				+ "<div style=\"font-size:11.5px;color:#64748b;margin-top:4px;line-height:1.35;\">"
				+ esc(keterangan) + "</div></div>";
	}

	/**
	 * Membungkus beberapa {@link #kartu(String, String, String, String)} dalam satu baris yang
	 * otomatis turun ke bawah saat layar sempit (responsif HP/desktop).
	 *
	 * @param kartuHtml daftar HTML kartu; nilai null/kosong diabaikan.
	 * @return potongan HTML baris kartu, siap dipasang ke {@code new MyHtml(...)}.
	 */
	public static String barisKartu(String... kartuHtml) {
		StringBuilder sb = new StringBuilder(256);
		sb.append("<div style=\"display:flex;gap:12px;flex-wrap:wrap;align-items:stretch;\">");
		if (kartuHtml != null) {
			for (String k : kartuHtml) {
				if (k != null && k.trim().length() > 0) {
					sb.append(k);
				}
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * Membungkus beberapa grafik (mis. keluaran {@link HtmlChartHelper}) berdampingan dalam satu baris
	 * yang otomatis turun ke bawah saat layar sempit. Tiap grafik diberi lebar minimal agar tetap
	 * terbaca; di HP grafik akan tampil bertumpuk satu per satu.
	 *
	 * @param chartHtml daftar HTML grafik; nilai null/kosong diabaikan.
	 * @return potongan HTML baris grafik, siap dipasang ke {@code new MyHtml(...)}.
	 */
	public static String barisChart(String... chartHtml) {
		StringBuilder sb = new StringBuilder(512);
		sb.append("<div style=\"display:flex;gap:12px;flex-wrap:wrap;align-items:stretch;\">");
		if (chartHtml != null) {
			for (String ch : chartHtml) {
				if (ch != null && ch.trim().length() > 0) {
					sb.append("<div style=\"flex:1 1 340px;min-width:300px;box-sizing:border-box;\">").append(ch)
							.append("</div>");
				}
			}
		}
		sb.append("</div>");
		return sb.toString();
	}

	/** Escape karakter khusus HTML agar teks pengguna tidak merusak struktur halaman. */
	private static String esc(String s) {
		if (s == null) {
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
				.replace("'", "&#39;");
	}
}
