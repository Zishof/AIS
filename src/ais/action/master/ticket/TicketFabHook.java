package ais.action.master.ticket;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.UiLifeCycle;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;

import ais.common.Common;
import ais.ui.util.MyWindow;

/**
 * Hook global penambah tombol <b>"Ajukan Tiket"</b> mengambang di sudut kanan-bawah, HANYA pada
 * halaman <b>shell utama</b> (index/dekstop) sehingga tombol tampil persisten di seluruh sesi
 * tanpa bergantung pada tab yang sedang dibuka.
 *
 * <p><b>Anti-bentrok:</b> tombol Bantuan mengambang ({@code kb-fab-global}) berada di
 * {@code bottom:78px}; tombol tiket ini ditempatkan di {@code bottom:138px} (di atasnya) sehingga
 * tidak saling menimpa. Lihat {@code ais.action.master.helper.BantuanGlobalHook}.</p>
 *
 * <p><b>Keamanan:</b> seluruh proses dibungkus try/catch — kegagalan apa pun tidak boleh
 * mengganggu render. Tombol hanya muncul bila modul Ticketing aktif
 * ({@link TicketKonfigurasiAction#modulAktif()}). Klik → membuka modul Ticketing sebagai modal.
 * Nonaktif permanen: hapus listener di {@code WEB-INF/zk.xml}. Java 1.7.</p>
 */
public class TicketFabHook implements UiLifeCycle {

	/** Key halaman shell (nama berkas zul tanpa ekstensi) yang menerima tombol. */
	private static final Set<String> SHELL_KEYS;
	static {
		Set<String> s = new HashSet<String>();
		s.add("index");
		s.add("index2");
		s.add("indexl");
		s.add("dekstop");
		SHELL_KEYS = Collections.unmodifiableSet(s);
	}

	private static final String SCLASS = "kb-fab-tiket";

	@Override
	public void afterPageAttached(Page page, Desktop desktop) {
		try {
			if (!TicketKonfigurasiAction.modulAktif() || !TicketKonfigurasiAction.fabAktif()) {
				return;
			}
			String key = keyDariPage(page);
			if (key == null || !SHELL_KEYS.contains(key)) {
				return;
			}
			if (sudahAda(page)) {
				return;
			}
			buatFab().setPage(page);
		} catch (Throwable ignore) {
			ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) TicketFabHook.afterPageAttached");
		}
	}

	private static boolean sudahAda(Page page) {
		try {
			for (Object o : page.getRoots()) {
				if (o instanceof Div && SCLASS.equals(((Div) o).getSclass())) {
					return true;
				}
			}
		} catch (Throwable ignore) {
			ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) TicketFabHook.sudahAda");
		}
		return false;
	}

	/** Bangun tombol tiket mengambang (belum dipasang ke page). */
	private static Div buatFab() {
		final Div fab = new Div();
		fab.setSclass(SCLASS);
		// !important pada style INLINE mengalahkan aturan tema shell yang memaksa div melar
		// (width/height:100% !important, position:static !important) sehingga FAB tetap pill kecil.
		fab.setStyle("position:fixed !important;right:16px !important;bottom:138px !important;"
				+ "left:auto !important;top:auto !important;width:max-content !important;min-width:0 !important;"
				+ "height:auto !important;max-width:240px;z-index:99990 !important;cursor:pointer;box-sizing:border-box;"
				+ "background:#059669 !important;color:#ffffff !important;border-radius:22px !important;"
				+ "padding:9px 15px !important;margin:0 !important;box-shadow:0 4px 14px rgba(5,150,105,.35);"
				+ "font-size:13px !important;font-weight:600;line-height:1 !important;"
				+ "display:inline-flex !important;align-items:center;gap:6px;white-space:nowrap;"
				+ "font-family:'Segoe UI',Arial,sans-serif;");
		fab.setTooltiptext("Ajukan tiket / laporkan kendala sistem");
		new Html("<span style='font-size:15px;line-height:1;'>&#127915;</span><span>Ajukan Tiket</span>")
				.setParent(fab);
		fab.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				bukaModul(fab);
			}
		});
		return fab;
	}

	/** Membuka modul Ticketing sebagai jendela modal. */
	private static void bukaModul(Component acuan) {
		try {
			MyWindow window = new MyWindow("Ticketing Management", "normal", true);
			window.setWidth("92%");
			window.setHeight("92%");
			window.setContentStyle("overflow:auto;");
			window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
			TicketingAction.display(window, Common.getCurrentUser());
			window.setVisible(true);
			window.onModal();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private static String keyDariPage(Page page) {
		if (page == null) {
			return null;
		}
		String p;
		try {
			p = page.getRequestPath();
		} catch (Throwable t) {
			return null;
		}
		if (p == null || p.trim().length() == 0) {
			return null;
		}
		p = p.trim();
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

	@Override
	public void afterComponentAttached(Component comp, Page page) {
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
