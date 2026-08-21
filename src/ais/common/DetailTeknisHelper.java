package ais.common;

import java.io.File;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Textbox;

import ais.database.model.Konfigurasi;

/**
 * {@code DetailTeknisHelper} — panel <b>"Detail Informasi Teknis"</b> yang dipakai bersama oleh
 * SEMUA pesan kegagalan laporan di aplikasi ini.
 *
 * <h3>Masalah yang diselesaikan</h3>
 * <p>Pesan kegagalan laporan sudah ramah bagi pengguna ("Laporan belum dapat ditampilkan …"),
 * tetapi <b>membuang seluruh informasi teknisnya</b>. Akibatnya pengguna hanya bisa mengirim
 * tangkapan layar berisi kalimat umum, sedangkan pengembang tetap tidak tahu exception apa yang
 * sebenarnya terjadi. Satu-satunya jalur teknis yang tersedia sebelumnya adalah tombol
 * "Download detail error", dan tombol itu hanya muncul bila berkas detail sempat ditulis ke disk
 * — pada banyak kegagalan berkas tersebut tidak ada, sehingga tidak ada jejak sama sekali.</p>
 *
 * <p>Helper ini menambahkan satu tombol <b>"Lihat Detail Error"</b> di bawah pesan ramah
 * tersebut. Saat ditekan, panel terbuka dan menampilkan rincian exception di dalam kotak teks
 * yang dapat diseleksi, lengkap dengan tombol <b>Salin</b> agar pengguna tinggal menempelkannya
 * ke pesan/e-mail untuk pengembang.</p>
 *
 * <h3>Kenapa komponen ZK, bukan JavaScript</h3>
 * <p>Isi HTML yang disisipkan ZK lewat {@code innerHTML} <b>tidak pernah menjalankan</b> tag
 * {@code <script>}. Karena itu buka-tutup panel dikerjakan di sisi server dengan
 * {@code setVisible()} pada komponen ZK biasa, sama seperti komponen aplikasi lainnya. Hanya
 * penyalinan ke papan klip yang memerlukan JavaScript, dan itu dikirim lewat
 * {@link Clients#evalJavaScript(String)} pada saat tombol ditekan.</p>
 *
 * <h3>Penyalinan yang tetap bekerja di HTTP</h3>
 * <p>{@code navigator.clipboard} hanya tersedia pada <i>secure context</i> (HTTPS/localhost).
 * Sebagian instalasi berjalan di HTTP biasa, sehingga tombol Salin selalu menyediakan jalur
 * cadangan {@code document.execCommand('copy')}. Di luar itu, kotak teksnya sendiri dapat
 * diseleksi manual (Ctrl+A, Ctrl+C) sehingga pengguna tidak pernah kehilangan jalan.</p>
 *
 * <h3>Keamanan</h3>
 * <p>Teks teknis disaring lebih dulu oleh {@link #sensor(String)} untuk menghapus kata sandi,
 * token, dan kredensial di dalam URL koneksi sebelum ditampilkan atau disalin — pesan exception
 * basis data kadang membawa URL JDBC berikut sandinya. Seluruh badan kelas ini gagal-aman:
 * kegagalan apa pun di sini tidak boleh menghalangi tampilnya pesan utama.</p>
 *
 * <h3>Konfigurasi</h3>
 * <ul>
 *   <li>{@code report_error_tombol_detail_teknis} (default aktif) — tampilkan tombolnya atau tidak.</li>
 *   <li>{@code report_error_detail_tampilkan_stacktrace} (default aktif) — sertakan stack trace
 *       penuh atau cukup ringkasan tipe/pesan exception. Kunci ini sudah dipakai
 *       {@code Report.buildReportErrorDetailText}, jadi satu setelan berlaku untuk berkas detail
 *       maupun panel ini.</li>
 * </ul>
 *
 * <p>Kompatibilitas: Java 1.6 (tanpa lambda, diamond, try-with-resources, atau Stream).</p>
 */
public final class DetailTeknisHelper {

	private DetailTeknisHelper() {
	}

	/** Konfigurasi: tampilkan tombol "Lihat Detail Error" atau tidak. */
	public static final String KONFIG_TOMBOL = "report_error_tombol_detail_teknis";

	/** Konfigurasi: sertakan stack trace penuh pada detail teknis (dipakai bersama Report). */
	public static final String KONFIG_STACKTRACE = "report_error_detail_tampilkan_stacktrace";

	private static final String GAYA_TOMBOL =
			"padding:6px 12px;border-radius:999px;border:1px solid #fb923c;background:#ffffff;"
			+ "color:#9a3412;font-size:12px;font-weight:bold;cursor:pointer;";

	private static String t(String s) {
		try {
			return Common.getBahasaConfig(s);
		} catch (Throwable e) {
			return s;
		}
	}

	/** Apakah tombol detail teknis ditampilkan. Gagal-aman: default tampil. */
	public static boolean tombolAktif() {
		try {
			return Common.bolehKonfigurasi(KONFIG_TOMBOL, Konfigurasi.AKTIF);
		} catch (Throwable t) {
			return true;
		}
	}

	private static boolean stackTraceAktif() {
		try {
			return Common.bolehKonfigurasi(KONFIG_STACKTRACE, Konfigurasi.AKTIF);
		} catch (Throwable t) {
			return true;
		}
	}

	/**
	 * Kode rujukan singkat untuk satu kejadian kesalahan. Dicetak pada pesan yang dilihat
	 * pengguna DAN pada teks teknis, sehingga laporan pengguna ("kode LAP-XXXX") dapat
	 * dicocokkan dengan catatan di sisi pengembang.
	 */
	public static String kodeRujukan() {
		try {
			return "LAP-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
		} catch (Throwable t) {
			return "LAP-000000";
		}
	}

	// =========================================================
	// Penyusunan teks teknis
	// =========================================================

	/**
	 * Susun teks teknis yang siap dibaca/disalin pengembang.
	 *
	 * @param konteks    nama proses/laporan yang gagal (boleh {@code null})
	 * @param error      exception yang tertangkap (boleh {@code null})
	 * @param detailFile berkas detail error yang sudah ditulis {@code Report} bila ada; isinya
	 *                   jauh lebih kaya (nama template, format, parameter, analisis awal)
	 *                   sehingga diutamakan (boleh {@code null})
	 * @param kode       kode rujukan dari {@link #kodeRujukan()} (boleh {@code null})
	 */
	public static String teksTeknis(String konteks, Throwable error, File detailFile, String kode) {
		StringBuilder sb = new StringBuilder(4096);
		try {
			sb.append("DETAIL INFORMASI TEKNIS\n");
			sb.append("============================================================\n");
			if (kode != null && kode.trim().length() > 0) {
				sb.append("Kode rujukan  : ").append(kode.trim()).append("\n");
			}
			sb.append("Waktu         : ").append(waktuSekarang()).append("\n");
			if (konteks != null && konteks.trim().length() > 0) {
				sb.append("Proses        : ").append(konteks.trim()).append("\n");
			}
			String halaman = halamanSekarang();
			if (halaman != null) {
				sb.append("Halaman       : ").append(halaman).append("\n");
			}
			sb.append("\n");

			String dariBerkas = isiBerkas(detailFile);
			if (dariBerkas != null && dariBerkas.trim().length() > 0) {
				// Berkas detail Report sudah memuat ringkasan, analisis awal, parameter,
				// dan stack trace. Tidak perlu diulang dari objek exception.
				sb.append(dariBerkas);
			} else {
				sb.append(ringkasanException(error));
			}
		} catch (Throwable t) {
			try {
				ErrorAuditUtil.record(t, "DetailTeknisHelper.teksTeknis");
			} catch (Throwable ignore) {
				// tidak boleh mengganggu tampilnya pesan utama
			}
			sb.append("\n(Sebagian detail teknis gagal disusun: ").append(String.valueOf(t)).append(")");
		}
		return sensor(sb.toString());
	}

	private static String ringkasanException(Throwable error) {
		if (error == null) {
			return "Tidak ada objek exception yang tercatat untuk kejadian ini.\n"
					+ "Kemungkinan kegagalan terdeteksi dari hasil proses, bukan dari exception.\n";
		}
		StringBuilder sb = new StringBuilder(2048);
		Throwable akar = akar(error);
		sb.append("Tipe error utama       : ").append(error.getClass().getName()).append("\n");
		sb.append("Pesan error utama      : ").append(String.valueOf(error.getMessage())).append("\n");
		if (akar != null && akar != error) {
			sb.append("Tipe penyebab terdalam : ").append(akar.getClass().getName()).append("\n");
			sb.append("Pesan penyebab terdalam: ").append(String.valueOf(akar.getMessage())).append("\n");
		}
		sb.append("\n");
		if (stackTraceAktif()) {
			sb.append("Stack trace:\n");
			sb.append("------------------------------------------------------------\n");
			sb.append(stackTrace(error));
		} else {
			sb.append("(Stack trace tidak ditampilkan karena konfigurasi ")
					.append(KONFIG_STACKTRACE).append(" tidak aktif.)\n");
		}
		return sb.toString();
	}

	private static Throwable akar(Throwable error) {
		Throwable hasil = error;
		int batas = 0;
		while (hasil != null && hasil.getCause() != null && hasil.getCause() != hasil && batas < 50) {
			hasil = hasil.getCause();
			batas++;
		}
		return hasil;
	}

	private static String stackTrace(Throwable error) {
		java.io.StringWriter sw = new java.io.StringWriter();
		java.io.PrintWriter pw = new java.io.PrintWriter(sw);
		try {
			error.printStackTrace(pw);
			pw.flush();
		} finally {
			pw.close();
		}
		return sw.toString();
	}

	private static String isiBerkas(File berkas) {
		if (berkas == null || !berkas.exists() || !berkas.isFile()) {
			return null;
		}
		java.io.InputStreamReader reader = null;
		try {
			// Batasi agar berkas detail yang sangat besar tidak membekukan layar.
			long maksimal = 200000L;
			reader = new java.io.InputStreamReader(new java.io.FileInputStream(berkas), "UTF-8");
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[8192];
			int dibaca;
			while ((dibaca = reader.read(buffer)) > 0 && sb.length() < maksimal) {
				sb.append(buffer, 0, dibaca);
			}
			if (sb.length() >= maksimal) {
				sb.append("\n\n(Dipotong. Rincian selengkapnya ada pada berkas ")
						.append(berkas.getName()).append(" di sisi server.)");
			}
			return sb.toString();
		} catch (Throwable t) {
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Throwable ignore) {
					// berkas hanya dibaca; kegagalan penutupan tidak mengubah hasil
				}
			}
		}
	}

	private static String waktuSekarang() {
		try {
			return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
		} catch (Throwable t) {
			return "-";
		}
	}

	private static String halamanSekarang() {
		try {
			org.zkoss.zk.ui.Execution exec = org.zkoss.zk.ui.Executions.getCurrent();
			if (exec == null) {
				return null;
			}
			String path = exec.getDesktop() == null ? null : exec.getDesktop().getRequestPath();
			return (path == null || path.trim().length() == 0) ? null : path.trim();
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * Hapus kredensial dari teks teknis sebelum ditampilkan atau disalin.
	 *
	 * <p>Pesan exception basis data/HTTP kerap membawa URL koneksi lengkap dengan sandinya —
	 * teks ini akan disalin pengguna dan dikirim lewat kanal yang tidak terkendali, jadi
	 * penyensoran dilakukan di sini, bukan diserahkan ke kebijaksanaan pengguna.</p>
	 */
	static String sensor(String teks) {
		if (teks == null || teks.length() == 0) {
			return teks;
		}
		String hasil = teks;
		try {
			// Authorization: Bearer <token> -- nilainya mengandung spasi, jadi disikat sampai
			// akhir baris. HARUS didahulukan sebelum aturan kata-kunci di bawah, yang hanya
			// akan memakan kata "Bearer" dan justru meninggalkan tokennya.
			hasil = hasil.replaceAll("(?i)(authorization)(\\s*[=:]\\s*).*", "$1$2***disensor***");
			// Token bertipe Bearer yang berdiri sendiri tanpa header Authorization.
			hasil = hasil.replaceAll("(?i)\\bbearer\\s+[A-Za-z0-9._~+/=\\-]{8,}", "Bearer ***disensor***");
			// password=rahasia / pwd: rahasia / token=abc123 / client_secret="abc"
			// Kutip pembuka ditangkap terpisah ($3): tanpa itu, nilai yang diapit tanda kutip
			// justru LOLOS -- kutip termasuk karakter penghenti, sehingga pola tidak cocok
			// sama sekali dan rahasianya tetap tampil.
			hasil = hasil.replaceAll(
					"(?i)(password|passwd|pwd|secret|token|api[_-]?key)"
							+ "(\\s*[=:]\\s*)([\"']?)[^\\s,;&'\"<>\\)\\]}]+",
					"$1$2$3***disensor***");
			// skema://pengguna:sandi@host
			hasil = hasil.replaceAll("(?i)([a-z0-9+.-]+://[^/:@\\s]+):([^@\\s/]+)@", "$1:***disensor***@");
		} catch (Throwable t) {
			// Bila regex gagal, lebih baik kembalikan teks tanpa perubahan daripada
			// kehilangan seluruh detail teknis.
			return teks;
		}
		return hasil;
	}

	// =========================================================
	// Panel ZK
	// =========================================================

	/**
	 * Pasang tombol "Lihat Detail Error" beserta panelnya ke {@code induk}.
	 *
	 * @return {@link Hbox} berisi tombol-tombol, agar pemanggil dapat menambahkan tombol lain
	 *         (mis. "Download detail error") ke baris yang sama; {@code null} bila tombol tidak
	 *         dipasang (konfigurasi mati, induk kosong, atau terjadi kegagalan)
	 */
	public static Hbox pasangPanel(Component induk, String konteks, Throwable error, File detailFile,
			String kode) {
		if (induk == null) {
			return null;
		}
		try {
			if (!tombolAktif()) {
				return null;
			}
			final String teks = teksTeknis(konteks, error, detailFile, kode);

			Hbox baris = new Hbox();
			baris.setSpacing("8px");
			baris.setStyle("margin-top:12px;");
			baris.setParent(induk);

			final Div panel = new Div();
			panel.setVisible(false);
			panel.setStyle("margin-top:10px;");

			final Button tombol = new Button(t("Lihat Detail Error"));
			tombol.setStyle(GAYA_TOMBOL);
			tombol.setTooltiptext(t("Tampilkan rincian teknis kesalahan ini untuk dikirim ke admin"));
			tombol.setParent(baris);
			tombol.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					boolean buka = !panel.isVisible();
					panel.setVisible(buka);
					tombol.setLabel(buka ? t("Sembunyikan Detail Error")
							: t("Lihat Detail Error"));
				}
			});

			// Isi panel
			new Html("<div style='font-size:11px;color:#9a3412;margin-bottom:6px;'>"
					+ escape(t("Salin seluruh isi kotak di bawah ini, lalu kirimkan kepada Administrator "
							+ "atau Pengembang Sistem. Kotak ini juga dapat diseleksi manual "
							+ "(Ctrl+A lalu Ctrl+C)."))
					+ "</div>").setParent(panel);

			Textbox kotak = new Textbox();
			kotak.setMultiline(true);
			kotak.setRows(12);
			kotak.setReadonly(true);
			kotak.setWidth("100%");
			kotak.setValue(teks);
			kotak.setStyle("font-family:Consolas,'Courier New',monospace;font-size:11px;"
					+ "line-height:1.45;color:#1f2937;background:#ffffff;border:1px solid #fdba74;");
			kotak.setParent(panel);

			Hbox barisSalin = new Hbox();
			barisSalin.setSpacing("8px");
			barisSalin.setStyle("margin-top:8px;");
			barisSalin.setParent(panel);

			Button salin = new Button(t("Copy Error untuk Admin"));
			salin.setStyle("padding:6px 12px;border-radius:999px;border:1px solid #fb923c;"
					+ "background:#ea580c;color:#ffffff;font-size:12px;font-weight:bold;cursor:pointer;");
			salin.setTooltiptext(t("Salin detail teknis ke papan klip untuk dikirim ke admin"));
			salin.setParent(barisSalin);
			salin.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					try {
						Clients.evalJavaScript(jsSalin(teks));
					} catch (Throwable t) {
						ErrorAuditUtil.record(t, "DetailTeknisHelper.salin");
					}
				}
			});

			panel.setParent(induk);
			return baris;
		} catch (Throwable t) {
			try {
				ErrorAuditUtil.record(t, "DetailTeknisHelper.pasangPanel");
			} catch (Throwable ignore) {
				// pesan utama tetap harus tampil walau panel gagal dipasang
			}
			return null;
		}
	}

	/**
	 * JavaScript penyalin. {@code navigator.clipboard} dicoba lebih dulu, lalu jatuh ke
	 * {@code execCommand('copy')} yang tetap bekerja pada halaman HTTP biasa.
	 */
	private static String jsSalin(String teks) {
		return "(function(t){"
				+ "var kabar=function(m){try{var d=document.createElement('div');"
				+ "d.style.cssText='position:fixed;z-index:2147483647;left:50%;top:18px;"
				+ "transform:translateX(-50%);background:#065f46;color:#ecfdf5;border-radius:8px;"
				+ "padding:9px 14px;font:13px/1.4 Arial,sans-serif;"
				+ "box-shadow:0 10px 26px rgba(15,23,42,.25);';d.textContent=m;"
				+ "document.body.appendChild(d);setTimeout(function(){try{"
				+ "if(d.parentNode)d.parentNode.removeChild(d);}catch(e){}},3500);}catch(e){}};"
				+ "var cadangan=function(){try{var ta=document.createElement('textarea');ta.value=t;"
				+ "ta.style.cssText='position:fixed;top:-1000px;left:-1000px;';"
				+ "document.body.appendChild(ta);ta.focus();ta.select();"
				+ "var ok=document.execCommand('copy');document.body.removeChild(ta);"
				+ "kabar(ok?'Detail teknis sudah disalin.':"
				+ "'Penyalinan otomatis gagal. Silakan seleksi manual pada kotak detail (Ctrl+A lalu Ctrl+C).');"
				+ "}catch(e){kabar('Penyalinan otomatis gagal. Silakan seleksi manual pada kotak detail "
				+ "(Ctrl+A lalu Ctrl+C).');}};"
				+ "try{if(navigator.clipboard&&navigator.clipboard.writeText){"
				+ "navigator.clipboard.writeText(t).then(function(){kabar('Detail teknis sudah disalin.');},"
				+ "function(){cadangan();});}else{cadangan();}}catch(e){cadangan();}"
				+ "})(" + org.json.JSONObject.quote(teks == null ? "" : teks) + ");";
	}

	private static String escape(String s) {
		if (s == null) {
			return "";
		}
		String r = s;
		r = r.replace("&", "&amp;");
		r = r.replace("<", "&lt;");
		r = r.replace(">", "&gt;");
		r = r.replace("\"", "&quot;");
		r = r.replace("'", "&#39;");
		return r;
	}
}
