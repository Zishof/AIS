package ais.ui.util;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Div;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;

import ais.common.Common;
import ais.database.model.Tbmuser;

/**
 * <h2>Pemilih Bahasa (ZK) — DROPDOWN BENDERA (Indonesia / English / Arab / Mandarin)</h2>
 *
 * <p>Tombol berupa GAMBAR bendera bahasa AKTIF (+panah kecil). Saat diklik membuka {@link Menupopup}
 * berisi daftar bendera untuk dipilih. Memilih → {@link Common#initBahasaParameter(String)} (set bahasa
 * aktif + simpan permanen bila login) lalu reload halaman.</p>
 *
 * <p>Bendera aktif memakai {@link Image} (render andal di ZK5, termasuk SVG). Path desktop-relatif
 * "/..." otomatis diberi context-path oleh ZK — JANGAN prepend Common.ROOT.</p>
 */
public class BahasaSwitchHelper {

	private static final String BASIS_FLAG = "/component/assets/media/flags/";

	private BahasaSwitchHelper() {
	}

	/** Dropdown bendera: tombol = bendera aktif; klik → Menupopup daftar bendera. */
	public static Component buatComboBahasa() {
		String kodeAktif = kodeSekarang();

		// Popup daftar bendera.
		final Menupopup pop = new Menupopup();
		tambahItem(pop, "id", "indonesia", "Indonesia");
		tambahItem(pop, "en", "united-kingdom", "English");
		tambahItem(pop, "ar", "saudi-arabia", "العربية");
		tambahItem(pop, "zh", "china", "中文");

		Div wrap = new Div();
		wrap.setSclass("ais-pilih-bahasa-combo");
		// position:relative;top:-3px → angkat sedikit agar sejajar dengan lonceng Info & profil di header.
		wrap.setStyle("display:inline-block;vertical-align:middle;cursor:pointer;white-space:nowrap;"
				+ "position:relative;top:-3px;");
		pop.setParent(wrap);

		final Image img = new Image();
		try {
			img.setSrc(BASIS_FLAG + flagFor(kodeAktif) + ".svg");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/BahasaSwitchHelper.java:56");
		}
		try {
			img.setTooltiptext(tooltipFor(kodeAktif));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/BahasaSwitchHelper.java:60");
		}
		img.setStyle("width:19px;height:13px;border-radius:2px;box-shadow:0 0 0 1px rgba(255,255,255,.45);"
				+ "vertical-align:middle;cursor:pointer;");
		img.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pop.open(img, "after_start");
			}
		});
		img.setParent(wrap);

		Label caret = new Label("▾");
		caret.setStyle("font-size:8px;color:#cbd5e1;vertical-align:middle;cursor:pointer;margin-left:1px;");
		caret.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				pop.open(img, "after_start");
			}
		});
		caret.setParent(wrap);

		return wrap;
	}

	private static void tambahItem(Menupopup pop, final String kode, String namaFlag, String tooltip) {
		Menuitem it = new Menuitem();
		try {
			it.setImage(BASIS_FLAG + namaFlag + ".svg");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/BahasaSwitchHelper.java:89");
		}
		it.setLabel(tooltip); // bendera + nama bahasa di daftar pilihan (biar jelas)
		try {
			it.setTooltiptext(tooltip);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/BahasaSwitchHelper.java:94");
		}
		it.addEventListener(Events.ON_CLICK, new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				// Set bahasa aktif di sesi + persist, lalu MUAT ULANG halaman kini. sendRedirect(null) me-reload
				// URL desktop saat ini apa adanya (context-path aman) — TIDAK menyusun URL manual yang bisa
				// kehilangan "/ecampus" dan malah keluar ke halaman luar.
				Common.initBahasaParameter(kode);
				Executions.sendRedirect(null);
			}
		});
		it.setParent(pop);
	}

	private static String flagFor(String kode) {
		if ("en".equals(kode)) {
			return "united-kingdom";
		}
		if ("ar".equals(kode)) {
			return "saudi-arabia";
		}
		if ("zh".equals(kode)) {
			return "china";
		}
		return "indonesia";
	}

	private static String tooltipFor(String kode) {
		if ("en".equals(kode)) {
			return "English";
		}
		if ("ar".equals(kode)) {
			return "العربية";
		}
		if ("zh".equals(kode)) {
			return "中文";
		}
		return "Indonesia";
	}

	private static String kodeSekarang() {
		// Baca current_lang LANGSUNG dari sesi — sumber yang SAMA dengan yang dipakai getBahasaConfig untuk
		// menerjemahkan menu. Dengan begitu bendera SELALU cocok dengan bahasa menu, TANPA bergantung pada
		// Common.kodeBahasaAktif() (yang punya fallback DB & bisa tertinggal versi lama saat deploy parsial).
		String lang = null;
		try {
			Object o = Sessions.getCurrent(true).getAttribute("current_lang");
			lang = o == null ? null : o.toString();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/BahasaSwitchHelper.java:143");
		}
		if (lang == null || lang.trim().length() == 0) {
			// Sesi kosong → pakai logika terpusat (sesi+DB) sebagai cadangan.
			try {
				return Common.kodeBahasaAktif();
			} catch (Exception e) {
				return "id";
			}
		}
		if (Tbmuser.ENGLISH.equalsIgnoreCase(lang)) {
			return "en";
		}
		if (Tbmuser.ARAB.equalsIgnoreCase(lang)) {
			return "ar";
		}
		if (Tbmuser.MANDARIN.equalsIgnoreCase(lang)) {
			return "zh";
		}
		return "id";
	}
}
