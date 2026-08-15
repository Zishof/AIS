package ais.action.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Penyaji isi panduan (Bantuan) untuk halaman JSP.
 *
 * <p>Berkas panduan tersimpan di {@code WEB-INF/bantuan/<key>.html} yang tidak dapat
 * diakses langsung dari peramban. Servlet ini membaca berkas tersebut berdasarkan
 * parameter {@code key}, membungkusnya dalam halaman bergaya lengkap dengan tombol
 * Cetak, lalu mengirimkannya. Dipakai oleh tombol "Bantuan" mengambang pada halaman
 * JSP (lihat {@code WEB-INF/baru/include/bantuan_button.jsp}).</p>
 *
 * <p><b>Keamanan.</b> Nama berkas divalidasi ketat ({@code [a-z0-9_-]+}) sehingga tidak
 * mungkin menembus direktori lain (path traversal). Servlet hanya menyajikan berkas
 * panduan dan tidak mengungkap data lain.</p>
 */
public class Bantuan extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html; charset=UTF-8");

		String key = request.getParameter("key");
		key = key == null ? "" : key.trim().toLowerCase();
		boolean modeTanyaJawab = "qa".equalsIgnoreCase(request.getParameter("mode"));

		PrintWriter out = response.getWriter();

		if (!key.matches("[a-z0-9_\\-]+")) {
			out.write(bungkus("Bantuan",
					"<h2 style='color:#1d4ed8;'>Permintaan Tidak Valid</h2>"
							+ "<p>Permintaan panduan tidak dikenali.</p>"));
			return;
		}

		String isi = null;

		// 1. Utamakan isi termodifikasi dari tabel Bantuan.
		try {
			isi = ais.action.master.helper.BantuanHelper.ambilDariTabel(key);
		} catch (Throwable t) {
			isi = null;
		}

		// 2. Bila belum pernah dimodifikasi, ambil berkas HTML bawaan.
		if (isi == null || isi.trim().length() == 0) {
			try {
				String path = getServletContext().getRealPath("/WEB-INF/bantuan/" + key + ".html");
				if (path != null) {
					File f = new File(path);
					if (f.isFile()) {
						isi = baca(f);
					}
				}
			} catch (Exception e) {
				isi = null;
			}
		}

		if (isi == null || isi.trim().length() == 0) {
			isi = "<h2 style='color:#1d4ed8;'>Panduan Belum Tersedia</h2>"
					+ "<p>Panduan untuk halaman ini belum tersedia. Silakan hubungi administrator "
					+ "bila Anda memerlukan penjelasan lebih lanjut.</p>";
		}

		if (modeTanyaJawab) {
			String qaUmum = bacaResource("_qa_umum");
			if (qaUmum == null) {
				qaUmum = "";
			}
			isi = "<div class='kb-qa-tools'><input id='kbQaCari' type='search' "
					+ "placeholder='Cari pertanyaan: simpan, stok, kas, transaksi, jaringan…' "
					+ "oninput='kbCariQa(this.value)'><span id='kbQaJumlah'></span></div>"
					+ "<details class='kb-qa-item' open><summary>Apa fungsi dan petunjuk khusus halaman ini?</summary>"
					+ "<div>" + isi + "</div></details>" + qaUmum;
		}

		out.write(bungkus(modeTanyaJawab ? "Tanya Jawab" : "Bantuan", isi, modeTanyaJawab));
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private static String baca(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				bos.write(buf, 0, n);
			}
			return new String(bos.toByteArray(), "UTF-8");
		} finally {
			try {
				in.close();
			} catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/servlet/Bantuan.java:98");
			}
		}
	}

	private String bacaResource(String key) {
		try {
			String path = getServletContext().getRealPath("/WEB-INF/bantuan/" + key + ".html");
			if (path != null) {
				File f = new File(path);
				if (f.isFile()) {
					return baca(f);
				}
			}
		} catch (Exception ignore) {
			ais.common.ErrorAuditUtil.record(ignore,
					"auto-audit(empty-catch) src/ais/action/servlet/Bantuan.java:bacaResource");
		}
		return null;
	}

	private static String bungkus(String judul, String isi) {
		return bungkus(judul, isi, false);
	}

	private static String bungkus(String judul, String isi, boolean modeTanyaJawab) {
		return "<!doctype html><html lang='id'><head><meta charset='UTF-8'>"
				+ "<meta name='viewport' content='width=device-width,initial-scale=1'>"
				+ "<title>" + judul + "</title><style>"
				+ "body{margin:0;font-family:'Segoe UI',Arial,sans-serif;color:#0f172a;background:#fff;}"
				+ ".kb-bar{position:sticky;top:0;background:#eef2f7;border-bottom:1px solid #d7dee8;"
				+ "padding:8px 14px;text-align:right;}"
				+ ".kb-print{cursor:pointer;border:1px solid #cbd5e1;border-radius:8px;background:#fff;"
				+ "color:#1d4ed8;font-weight:600;padding:6px 12px;font-size:13px;}"
				+ ".kb-wrap{max-width:900px;margin:0 auto;padding:16px 22px;font-size:13px;line-height:1.7;}"
				+ ".kb-wrap h2{color:#1d4ed8;} .kb-wrap h3{color:#0f172a;}"
				+ ".kb-qa-tools{position:sticky;top:47px;z-index:2;background:#fff;padding:8px 0 12px;display:flex;gap:12px;align-items:center;}"
				+ ".kb-qa-tools input{flex:1;border:1px solid #cbd5e1;border-radius:9px;padding:10px 12px;font:inherit;}"
				+ ".kb-qa-tools span{color:#15803d;font-weight:600;white-space:nowrap;}"
				+ "details{border:1px solid #dbe5df;border-radius:10px;padding:0 14px;margin:0 0 10px;background:#fff;}"
				+ "summary{cursor:pointer;color:#166534;font-weight:700;padding:12px 0;} details>div{padding:0 0 12px;}"
				+ "@media print{.kb-bar{display:none;}}"
				+ "</style></head><body>"
				+ "<div class='kb-bar'><button class='kb-print' onclick='window.print()'>"
				+ "&#128424; Cetak</button></div>"
				+ "<div class='kb-wrap'>" + isi + "</div>"
				+ (modeTanyaJawab ? "<script>function kbCariQa(q){q=(q||'').toLowerCase().trim();var a=document.querySelectorAll('details');var n=0;a.forEach(function(x){var ok=!q||x.textContent.toLowerCase().indexOf(q)>=0;x.style.display=ok?'block':'none';if(ok)n++;});document.getElementById('kbQaJumlah').textContent=n+' pertanyaan';}kbCariQa('');</script>" : "")
				+ "</body></html>";
	}
}
