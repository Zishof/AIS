package ais.action.master.helper;

import java.io.File;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.UiLifeCycle;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.database.model.Konfigurasi;
import ais.ui.util.MyInclude;

/**
 * Hook global penambah tombol "Bantuan" mengambang (floating) pada halaman konten
 * yang TIDAK memiliki tombol Bantuan bawaan.
 *
 * <p><b>Mengapa perlu.</b> Sebagian besar halaman menampilkan bantuan lewat tombol
 * inline di ZUL ({@code BantuanHelper.tampilkanDariResource}). Namun sejumlah halaman
 * formulir/laporan/konfigurasi tidak memiliki toolbar sehingga tak dapat disisipi
 * tombol inline. Hook ini menambahkan satu tombol mengambang di sudut kanan-bawah
 * untuk halaman-halaman tersebut, dikenali dari nama berkas ZUL (key).</p>
 *
 * <p><b>Satu tombol, tiga pilihan.</b> Tombol mengambang berbentuk bulat "?" di sudut
 * kanan-bawah membuka menu berisi Bantuan Halaman Ini, Tanya Jawab, dan Semua Panduan.
 * Sejak 20-08-2026 menu ini SELALU lengkap: dahulu dua pilihan pertama hanya muncul untuk
 * daftar key tertentu, karena halaman lain sudah punya tombol Bantuan inline sendiri dan
 * dikhawatirkan tombolnya dobel. Kini tombol inline itu justru disembunyikan oleh
 * {@code MyToolbarbuttonConfig} (lihat {@link #adaFabDiHalaman(Page)}), sehingga seluruh
 * halaman ZK memakai satu pola bantuan yang sama dan layar utama tidak penuh tombol.
 * Pengaman lama tetap berlaku: tombol hanya muncul bila berkas
 * {@code WEB-INF/bantuan/<key>.html} ada, jadi pilihan yang ditawarkan pasti ada isinya.</p>
 *
 * <p><b>Keamanan.</b> Seluruh proses dibungkus try/catch agar kegagalan apa pun TIDAK
 * pernah mengganggu render halaman. Dapat dimatikan administrator lewat konfigurasi
 * tanpa perlu deploy ulang; untuk menonaktifkan permanen, hapus pendaftaran listener
 * di {@code WEB-INF/zk.xml}.</p>
 *
 * <p><b>Dua konfigurasi, dua peran.</b>
 * {@code bantuan_tombol_tampil} (default aktif) adalah sakelar INDUK ON/OFF: dimatikan
 * berarti tidak ada tombol Bantuan sama sekali di seluruh halaman — ZK maupun JSP.
 * {@code bantuan_tombol_global} (default aktif) hanya memilih GAYA tombol Bantuan
 * inline: mengambang di pojok kanan-bawah (aktif) atau tetap menyatu di toolbar
 * (tidak aktif); tombolnya sendiri tetap ada. Lihat {@link #tombolBantuanAktif()}
 * dan {@link #gayaMengambangAktif()}.</p>
 */
public class BantuanGlobalHook implements UiLifeCycle {


	// Cache flag konfigurasi (refresh tiap 60 dtk) agar tidak membebani tiap page-attach.
	private static volatile long lastCheck = 0L;
	private static volatile boolean enabled = true;

	// Cache sakelar induk, dipisah dari cache gaya di atas agar keduanya independen.
	private static volatile long lastCheckTampil = 0L;
	private static volatile boolean tampil = true;

	/** Nama konfigurasi sakelar induk ON/OFF seluruh tombol Bantuan. */
	public static final String KONFIG_TAMPIL = "bantuan_tombol_tampil";

	/**
	 * <b>Sakelar induk</b>: apakah tombol Bantuan ditampilkan sama sekali.
	 * Konfigurasi {@code bantuan_tombol_tampil} (default AKTIF).
	 *
	 * <p>Bernilai TIDAK_AKTIF berarti tidak ada tombol Bantuan di mana pun —
	 * tombol mengambang ZK (hook ini), tombol Bantuan inline pada toolbar
	 * ({@code MyToolbarbuttonConfig}, {@code GenericCrudAction}), maupun tombol
	 * mengambang halaman JSP ({@code WEB-INF/baru/include/bantuan_button.jsp}).
	 * Isi panduannya sendiri tetap dapat diakses langsung lewat URL {@code /bantuan?key=...};
	 * yang dimatikan hanyalah tombol pemicunya.</p>
	 *
	 * <p>Berbeda dari {@link #gayaMengambangAktif()} yang hanya memilih <i>gaya</i>
	 * (mengambang atau tetap inline di toolbar) dan tidak pernah menyembunyikan tombol.</p>
	 */
	public static boolean tombolBantuanAktif() {
		long now = System.currentTimeMillis();
		if (now - lastCheckTampil > 60000L) {
			try {
				tampil = Common.bolehKonfigurasi(KONFIG_TAMPIL, Konfigurasi.AKTIF);
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanGlobalHook.java:tombolBantuanAktif");
				// pertahankan nilai sebelumnya
			}
			lastCheckTampil = now;
		}
		return tampil;
	}

	/**
	 * Apakah gaya "Bantuan mengambang" aktif. Dipakai bersama oleh hook ini (untuk
	 * halaman tanpa toolbar) dan oleh {@code MyToolbarbuttonConfig}/{@code GenericCrudAction}
	 * (untuk memindahkan tombol Bantuan inline dari toolbar ke pojok kanan-bawah).
	 * Konfigurasi {@code bantuan_tombol_global} (default AKTIF).
	 *
	 * <p>Tunduk pada sakelar induk {@link #tombolBantuanAktif()}: bila tombol Bantuan
	 * dimatikan seluruhnya, tidak ada gaya mengambang yang perlu dipasang.</p>
	 */
	public static boolean gayaMengambangAktif() {
		return tombolBantuanAktif() && aktif();
	}

	private static boolean aktif() {
		long now = System.currentTimeMillis();
		if (now - lastCheck > 60000L) {
			try {
				enabled = Common.bolehKonfigurasi("bantuan_tombol_global", Konfigurasi.AKTIF);
			} catch (Throwable t) { ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanGlobalHook.java:118");
				// pertahankan nilai sebelumnya
			}
			lastCheck = now;
		}
		return enabled;
	}

	/** Penanda pada komponen include agar tombol tidak disisipkan dua kali. */
	private static final String ATTR_DONE = "_kbFabInjected";

	/** Penanda pada Page agar tombol tidak disisipkan dua kali per halaman. */
	private static final String ATTR_PAGE_DONE = "_kbFabPageInjected";

	/**
	 * Halaman yang dibuka sebagai <b>Page tersendiri</b> (full-page / defer-include).
	 * Untuk mayoritas halaman yang dibuka via tab, ZK memakai include mode "instant"
	 * (default "auto" + .zul) sehingga TIDAK membuat Page baru dan method ini tidak
	 * terpicu — jalur itu ditangani {@link #afterComponentAttached}.
	 */
	@Override
	public void afterPageAttached(Page page, Desktop desktop) {
		try {
			if (!gayaMengambangAktif()) {
				return;
			}
			final String key = keyDariPage(page);
			if (key == null) {
				return;
			}
			if (!fileBantuanAda(desktop, key)) {
				return;
			}
			buatFab(key, true).setPage(page);
			page.setAttribute(ATTR_PAGE_DONE, Boolean.TRUE);
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanGlobalHook.java:149");
			// JANGAN pernah mengganggu render halaman
		}
	}

	/**
	 * Jalur utama untuk halaman yang dibuka via tab. Setiap konten tab dimuat lewat
	 * {@link ais.ui.util.MyInclude} dalam mode "instant" (komponen digabung ke halaman
	 * induk, tanpa Page baru), sehingga {@link #afterPageAttached} tidak terpicu — namun
	 * {@code afterComponentAttached} tetap dipanggil saat komponen include menempel.
	 * Di sini tombol Bantuan mengambang disisipkan. Halaman yang punya tombol Bantuan inline
	 * tidak menjadi dobel karena tombol inline-nya disembunyikan oleh MyToolbarbuttonConfig.
	 */
	@Override
	public void afterComponentAttached(Component comp, Page page) {
		try {
			if (!(comp instanceof MyInclude)) {
				return;
			}
			if (!gayaMengambangAktif()) {
				return;
			}
			if (comp.getAttribute(ATTR_DONE) != null) {
				return;
			}
			final String key = keyDariSrc(((MyInclude) comp).getSrc());
			if (key == null) {
				return;
			}
			Desktop desktop = page != null ? page.getDesktop() : comp.getDesktop();
			if (desktop == null || !fileBantuanAda(desktop, key)) {
				return;
			}
			comp.setAttribute(ATTR_DONE, Boolean.TRUE);

			// KE-FIX (UiException "Only one child is allowed: <Center ...>"): comp.getParent()
			// sering adalah region Borderlayout (Center/North/South/East/West) yang HANYA
			// boleh punya satu anak, dan comp sendiri sudah jadi anak tunggalnya -- menambah
			// fab sbg anak kedua via fab.setParent(induk) melempar exception ini. FAB bersifat
			// overlay (position:fixed) sehingga tidak perlu nested di layout sama sekali;
			// pasang langsung ke Page bila tersedia (jalur normal), induk hanya sbg fallback.
			if (page != null) {
				if (page.getAttribute(ATTR_PAGE_DONE) != null) {
					return;
				}
				buatFab(key, true).setPage(page);
				page.setAttribute(ATTR_PAGE_DONE, Boolean.TRUE);
			} else {
				Component induk = comp.getParent();
				// Guard tombol ganda: jika wadah yang sama sudah punya FAB (mis. dua include
				// halaman-whitelist tampil bersamaan dalam satu container), jangan tambah lagi.
				if (induk != null && sudahAdaFab(induk)) {
					return;
				}
				if (induk != null) {
					buatFab(key, true).setParent(induk);
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanGlobalHook.java:196");
			// JANGAN pernah mengganggu render halaman
		}
	}

	/** Apakah wadah sudah memuat tombol Bantuan mengambang (langsung sebagai anak). */
	private static boolean sudahAdaFab(Component induk) {
		try {
			for (Object o : induk.getChildren()) {
				if (o instanceof Div && "kb-fab-global".equals(((Div) o).getSclass())) {
					return true;
				}
			}
		} catch (Throwable ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/helper/BantuanGlobalHook.java:209");
			// abaikan; anggap belum ada
		}
		return false;
	}

	/** Gaya satu baris menu di dalam panel kebab. */
	private static final String GAYA_ITEM =
			"cursor:pointer;padding:11px 14px;font-size:13px;font-weight:600;color:#0f172a;"
			+ "display:flex;align-items:center;white-space:nowrap;";

	/** Satu baris menu: ikon, label, dan aksinya. */
	private static Div itemMenu(Div panel, String ikon, String label, String tooltip, EventListener aksi) {
		Div item = new Div();
		item.setStyle(GAYA_ITEM);
		item.setTooltiptext(tooltip);
		new Html("<span style='font-size:15px;line-height:1;width:18px;text-align:center;'>" + ikon
				+ "</span><span style='margin-left:10px;'>" + label + "</span>").setParent(item);
		item.addEventListener("onClick", aksi);
		item.setParent(panel);
		return item;
	}

	/**
	 * Bangun tombol Bantuan mengambang berbentuk KEBAB (belum dipasang ke parent/page).
	 *
	 * <p><b>Kenapa kebab.</b> Sebelumnya tiga tombol pil ditumpuk vertikal mulai
	 * {@code bottom:78px}, sehingga kolomnya membentang sampai sekitar 199px. Tombol
	 * mengambang lain di aplikasi ini menempati sudut yang sama -- {@code TicketFabHook}
	 * ada di {@code bottom:138px}, tepat di tengah kolom itu -- sehingga keduanya
	 * bertindih. Dengan satu tombol bulat 48px pada {@code bottom:78px} (tepi atas 126px),
	 * sudut kanan-bawah hanya terpakai satu slot dan tumpang tindih itu hilang.</p>
	 *
	 * <p>Panel menunya komponen ZK biasa yang disembunyikan/ditampilkan lewat
	 * {@code setVisible}, BUKAN JavaScript: isi yang disisipkan ZK lewat innerHTML tidak
	 * pernah menjalankan {@code <script>}, jadi peralihan tampil-sembunyi dikerjakan di
	 * sisi server seperti komponen ZK lainnya.</p>
	 */
	private static Div buatFab(final String key, boolean sertakanBantuan) {
		final Div wrapper = new Div();
		wrapper.setSclass("kb-fab-global");
		wrapper.setStyle("position:fixed;right:16px;bottom:78px;z-index:99990;"
				+ "font-family:'Segoe UI',Arial,sans-serif;");

		// Panel menu, tersembunyi sampai tombol ditekan. Diposisikan absolut TERHADAP
		// wrapper sehingga tidak menambah tinggi slot saat tertutup.
		final Div panel = new Div();
		panel.setVisible(false);
		panel.setStyle("position:absolute;right:0;bottom:60px;min-width:224px;background:#ffffff;"
				+ "border:1px solid #dbe3ec;border-radius:12px;overflow:hidden;"
				+ "box-shadow:0 14px 38px rgba(15,23,42,.20);");
		panel.setParent(wrapper);

		new Html("<div style='padding:9px 14px 7px;font-size:11px;letter-spacing:.06em;"
				+ "text-transform:uppercase;color:#64748b;background:#f8fafc;"
				+ "border-bottom:1px solid #eef2f7;'>Bantuan</div>").setParent(panel);

		if (sertakanBantuan) {
			final Div[] pemicu = new Div[1];
			pemicu[0] = itemMenu(panel, "&#128214;", "Bantuan Halaman Ini",
					"Panduan modul yang sedang Anda buka", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							panel.setVisible(false);
							BantuanHelper.tampilkanDariResource(pemicu[0], key, "Bantuan");
						}
					});

			final Div[] pemicuQa = new Div[1];
			pemicuQa[0] = itemMenu(panel, "&#128172;", "Tanya Jawab",
					"Tanya jawab lengkap sesuai halaman ini", new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							panel.setVisible(false);
							BantuanHelper.tampilkanTanyaJawabDariResource(pemicuQa[0], key, "Tanya Jawab");
						}
					});
		}

		// Pusat Panduan selalu ikut: seluruh panduan menurut peran dan per modul,
		// tidak bergantung pada tersedianya panduan khusus halaman ini.
		final Div[] pemicuPusat = new Div[1];
		pemicuPusat[0] = itemMenu(panel, "&#128218;", "Semua Panduan",
				"Daftar seluruh panduan: menurut peran dan per modul", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						panel.setVisible(false);
						BantuanHelper.tampilkanDariResource(pemicuPusat[0], "panduan", "Pusat Panduan");
					}
				});

		final Div tombol = new Div();
		tombol.setStyle("width:48px;height:48px;border-radius:50%;cursor:pointer;background:#1d4ed8;"
				+ "color:#ffffff;font-size:20px;font-weight:700;line-height:48px;text-align:center;"
				+ "box-shadow:0 6px 18px rgba(29,78,216,.38);");
		tombol.setTooltiptext("Bantuan");
		new Html("?").setParent(tombol);
		tombol.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				panel.setVisible(!panel.isVisible());
			}
		});
		tombol.setParent(wrapper);

		return wrapper;
	}

	/** Turunkan key dari src include (mis. "/WEB-INF/z/x/y/mahasiswa.zul" -> "mahasiswa"). */
	/**
	 * Apakah halaman ini sudah dipasangi tombol Bantuan mengambang oleh hook global.
	 *
	 * <p>Dipakai {@code MyToolbarbuttonConfig}: bila sudah ada, tombol Bantuan inline milik
	 * ZUL cukup disembunyikan sehingga layar hanya menampilkan satu tombol "?" berisi menu
	 * Bantuan Halaman Ini / Tanya Jawab / Semua Panduan. Bila belum ada -- misalnya berkas
	 * panduan halaman tidak tersedia sehingga hook memilih tidak memasang apa pun -- tombol
	 * inline tetap dipertahankan agar bantuan tidak hilang sama sekali.</p>
	 */
	public static boolean adaFabDiHalaman(Page page) {
		try {
			return page != null && page.getAttribute(ATTR_PAGE_DONE) != null;
		} catch (Throwable t) {
			return false;
		}
	}

	private static String keyDariSrc(String src) {
		if (src == null || src.trim().length() == 0) {
			return null;
		}
		String p = src.trim();
		int q = p.indexOf('?');
		if (q >= 0) {
			p = p.substring(0, q);
		}
		int slash = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
		if (slash >= 0) {
			p = p.substring(slash + 1);
		}
		if (p.toLowerCase().endsWith(".zul")) {
			p = p.substring(0, p.length() - 4);
		}
		return p.toLowerCase();
	}

	/** Turunkan key (nama berkas zul tanpa path/ekstensi) dari request path halaman. */
	private static String keyDariPage(Page page) {
		if (page == null) {
			return null;
		}
		String p = null;
		try {
			p = page.getRequestPath();
		} catch (Throwable t) {
			p = null;
		}
		if (p == null || p.trim().length() == 0) {
			return null;
		}
		p = p.trim();
		int q = p.indexOf('?');
		if (q >= 0) {
			p = p.substring(0, q);
		}
		int h = p.indexOf('#');
		if (h >= 0) {
			p = p.substring(0, h);
		}
		int slash = p.lastIndexOf('/');
		if (slash >= 0) {
			p = p.substring(slash + 1);
		}
		int bslash = p.lastIndexOf('\\');
		if (bslash >= 0) {
			p = p.substring(bslash + 1);
		}
		if (p.toLowerCase().endsWith(".zul")) {
			p = p.substring(0, p.length() - 4);
		}
		return p.toLowerCase();
	}

	private static boolean fileBantuanAda(Desktop desktop, String key) {
		try {
			String path = desktop.getWebApp().getRealPath("/WEB-INF/bantuan/" + key + ".html");
			return path != null && new File(path).isFile();
		} catch (Throwable t) {
			return false;
		}
	}

	@Override
	public void afterComponentDetached(Component comp, Page prevpage) {
	}

	@Override
	public void afterComponentMoved(Component parent, Component child, Component prevparent) {
	}

	@Override
	public void afterPageDetached(Page page, Desktop prevdesktop) {
	}
}
