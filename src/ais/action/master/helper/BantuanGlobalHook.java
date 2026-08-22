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
 * {@code MyToolbarbuttonConfig}, yang menggantinya dengan tombol "?" MILIK TAB itu
 * sendiri (lihat {@link #keyDariKomponen(Component)}), sehingga seluruh
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
			/* FIX 21-08-2026 -- BANTUAN SALAH HALAMAN.
			 *
			 * Dahulu FAB dipasang ke PAGE dan ditandai ATTR_PAGE_DONE, sehingga hanya ADA SATU
			 * tombol untuk seluruh halaman ZK. Padahal tab Home/Mahasiswa/Beasiswa berbagi satu
			 * Page yang sama: tab yang dibuka PERTAMA merebut slot itu, dan key panduannya
			 * terkunci di situ. Akibatnya membuka tab lain tetap menampilkan panduan tab pertama.
			 *
			 * Kini FAB dipasang ke WADAH TAB (induk dari include), sehingga tiap tab punya
			 * tombolnya sendiri dengan key-nya sendiri, dan ikut tersembunyi ketika tabnya
			 * tidak aktif. Bila wadahnya hanya boleh beranak tunggal (mis. region Borderlayout),
			 * pemasangan dilewati -- lebih baik tanpa tombol daripada menampilkan panduan yang
			 * keliru untuk halaman lain. */
			Component induk = comp.getParent();
			if (induk == null || sudahAdaFab(induk)) {
				return;
			}
			try {
				buatFab(key, true).setParent(induk);
			} catch (Throwable takBolehBeranakBanyak) {
				ais.common.ErrorAuditUtil.record(takBolehBeranakBanyak,
						"BantuanGlobalHook: wadah tab menolak FAB, key=" + key);
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
			"cursor:pointer !important;padding:11px 14px;font-size:13px;font-weight:600;color:#0f172a;"
			+ "display:flex;align-items:center;white-space:nowrap;pointer-events:auto !important;";

	private static final String GAYA_WRAPPER_FAB =
			"position:fixed !important;right:22px !important;bottom:78px !important;"
			+ "left:auto !important;top:auto !important;z-index:2147483000 !important;"
			+ "width:48px !important;height:48px !important;min-width:48px !important;"
			+ "min-height:48px !important;max-width:48px !important;max-height:48px !important;"
			+ "margin:0 !important;padding:0 !important;overflow:visible !important;"
			+ "display:block !important;box-sizing:border-box !important;"
			+ "font-family:'Segoe UI',Arial,sans-serif;pointer-events:auto !important;";

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
	 * bertindih. Dengan satu tombol bulat 48px pada sudut kanan-bawah, sudut layar
	 * hanya terpakai satu slot dan tumpang tindih itu hilang.</p>
	 *
	 * <p>Panel menunya komponen ZK biasa yang disembunyikan/ditampilkan lewat
	 * {@code setVisible}, BUKAN JavaScript: isi yang disisipkan ZK lewat innerHTML tidak
	 * pernah menjalankan {@code <script>}, jadi peralihan tampil-sembunyi dikerjakan di
	 * sisi server seperti komponen ZK lainnya.</p>
	 */
	private static Div buatFab(final String key, boolean sertakanBantuan) {
		return buatFab(key, sertakanBantuan, null);
	}

	/**
	 * Varian yang membiarkan pemanggil menentukan sendiri aksi item "Bantuan Halaman Ini".
	 *
	 * <p>Dipakai tombol Bantuan <i>inline</i> milik ZUL. Tombol itu sudah membawa key panduan
	 * yang benar di dalam {@code onClick}-nya, jadi lebih tepat memicu ulang aksi aslinya
	 * daripada menebak key dari nama berkas -- key ZUL dan key panduan tidak selalu sama.</p>
	 */
	public static Div buatFab(final String key, boolean sertakanBantuan,
			final EventListener aksiBantuanHalaman) {
		final Div wrapper = new Div();
		wrapper.setSclass("kb-fab-global");
		wrapper.setStyle(GAYA_WRAPPER_FAB);

		// Panel menu, tersembunyi sampai tombol ditekan. Diposisikan absolut TERHADAP
		// wrapper sehingga tidak menambah tinggi slot saat tertutup.
		final Div panel = new Div();
		panel.setVisible(false);
		panel.setStyle("position:absolute;right:0;bottom:60px;min-width:224px;background:#ffffff;"
				+ "border:1px solid #dbe3ec;border-radius:12px;overflow:hidden;"
				+ "box-shadow:0 14px 38px rgba(15,23,42,.20);"
				+ "z-index:2147483001 !important;pointer-events:auto !important;");
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
							if (aksiBantuanHalaman != null) {
								aksiBantuanHalaman.onEvent(event);
							} else {
								BantuanHelper.tampilkanDariResource(pemicu[0], key, "Bantuan");
							}
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
		tombol.setStyle("width:48px !important;height:48px !important;border-radius:50%;"
				+ "cursor:pointer !important;background:#1d4ed8;"
				+ "color:#ffffff;font-size:20px;font-weight:700;line-height:48px;text-align:center;"
				+ "box-shadow:0 6px 18px rgba(29,78,216,.38);"
				+ "position:relative !important;z-index:2147483002 !important;"
				+ "pointer-events:auto !important;box-sizing:border-box !important;");
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
	/**
	 * Klaim satu-satunya slot tombol Bantuan mengambang pada halaman ini.
	 *
	 * @return {@code true} bila pemanggil berhak memasang; {@code false} bila sudah terisi.
	 */
	/** Key panduan (nama berkas zul tanpa path/ekstensi) untuk halaman tsb, atau {@code null}. */
	/**
	 * Key panduan untuk KOMPONEN tertentu — ditelusuri dari include tab terdekat.
	 *
	 * <p>Dipakai tombol Bantuan inline. Menurunkan key dari {@code page.getRequestPath()}
	 * TIDAK bisa dipakai di aplikasi ini karena banyak layar dimuat sebagai tab ke dalam
	 * satu Page yang sama, sehingga request path-nya selalu halaman induk. Yang benar adalah
	 * nama berkas ZUL milik include tab tempat komponen itu berada.</p>
	 */
	public static String keyDariKomponen(Component comp) {
		try {
			Component kini = comp;
			while (kini != null) {
				if (kini instanceof MyInclude) {
					String k = keyDariSrc(((MyInclude) kini).getSrc());
					if (k != null && k.trim().length() > 0) {
						return k;
					}
				}
				kini = kini.getParent();
			}
			return comp == null ? null : keyDariPage(comp.getPage());
		} catch (Throwable t) {
			return null;
		}
	}

	/** Wadah tab (induk dari include terdekat) tempat komponen ini berada, atau {@code null}. */
	public static Component wadahTab(Component comp) {
		try {
			Component kini = comp;
			while (kini != null) {
				if (kini instanceof MyInclude) {
					return kini.getParent();
				}
				kini = kini.getParent();
			}
		} catch (Throwable t) {
			/* abaikan */
		}
		return null;
	}

	/**
	 * Lepas tombol Bantuan mengambang yang menempel LANGSUNG pada wadah ini.
	 *
	 * <p>Dipakai tombol Bantuan inline: hook global memasang FAB pada wadah tab sebelum isi
	 * tab selesai dibangun, sehingga ia belum tahu bahwa tab tersebut punya tombol Bantuan
	 * sendiri. Tombol inline-lah yang membawa key panduan paling tepat, jadi FAB bawaan hook
	 * dilepas lebih dulu agar tidak ada dua tombol pada satu tab.</p>
	 */
	/**
	 * Lepas tombol Bantuan mengambang yang menempel langsung pada HALAMAN.
	 *
	 * <p>Padanan {@link #lepasFabDari(Component)} untuk layar yang dibuka sebagai halaman
	 * penuh (bukan tab). Pada jalur itu {@code afterPageAttached} memasang FAB ke Page, dan
	 * penelusuran wadah tab tidak akan menemukannya.</p>
	 */
	public static void lepasFabDariHalaman(Page page) {
		try {
			if (page == null) {
				return;
			}
			java.util.List<Object> salinan = new java.util.ArrayList<Object>(page.getRoots());
			for (int i = 0; i < salinan.size(); i++) {
				Object o = salinan.get(i);
				if (o instanceof Div && "kb-fab-global".equals(((Div) o).getSclass())) {
					((Div) o).detach();
				}
			}
			page.removeAttribute(ATTR_PAGE_DONE);
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "BantuanGlobalHook.lepasFabDariHalaman");
		}
	}

	public static void lepasFabDari(Component wadah) {
		try {
			if (wadah == null) {
				return;
			}
			java.util.List<Object> salinan = new java.util.ArrayList<Object>(wadah.getChildren());
			for (int i = 0; i < salinan.size(); i++) {
				Object o = salinan.get(i);
				if (o instanceof Div && "kb-fab-global".equals(((Div) o).getSclass())) {
					((Div) o).detach();
				}
			}
		} catch (Throwable t) {
			ais.common.ErrorAuditUtil.record(t, "BantuanGlobalHook.lepasFabDari");
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
