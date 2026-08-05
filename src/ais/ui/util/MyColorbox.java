package ais.ui.util;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 * Pengganti CE untuk org.zkoss.zkex.zul.Colorbox (PE-only) berbasis
 * Bandbox: kotak teks menampilkan kode warna #rrggbb dengan latar
 * mengikuti warna terpilih, dan bandpopup berisi palet warna siap
 * pakai + isian hex manual.
 *
 * API kompatibel dengan Colorbox asli: getColor()/setColor(), event
 * onChange terkirim saat warna dipilih. Kompatibel Java 1.7 dan
 * ZK 5 CE maupun ZK 9/10 CE.
 */
public class MyColorbox extends Bandbox {

	private static final long serialVersionUID = 1L;

	/* Palet: tiap baris satu keluarga warna, dari terang ke gelap. */
	private static final String[][] PALET = {
			{ "#ffffff", "#f1f5f9", "#cbd5e1", "#94a3b8", "#64748b", "#334155", "#0f172a", "#000000" },
			{ "#fee2e2", "#fca5a5", "#ef4444", "#dc2626", "#b91c1c", "#991b1b", "#7f1d1d", "#450a0a" },
			{ "#ffedd5", "#fdba74", "#f97316", "#ea580c", "#c2410c", "#9a3412", "#7c2d12", "#431407" },
			{ "#fef9c3", "#fde047", "#eab308", "#ca8a04", "#a16207", "#854d0e", "#713f12", "#422006" },
			{ "#dcfce7", "#86efac", "#22c55e", "#16a34a", "#15803d", "#166534", "#14532d", "#052e16" },
			{ "#ccfbf1", "#5eead4", "#14b8a6", "#0d9488", "#0f766e", "#115e59", "#134e4a", "#042f2e" },
			{ "#dbeafe", "#93c5fd", "#3b82f6", "#2563eb", "#1d4ed8", "#1e40af", "#1e3a8a", "#172554" },
			{ "#ede9fe", "#c4b5fd", "#8b5cf6", "#7c3aed", "#6d28d9", "#5b21b6", "#4c1d95", "#2e1065" },
			{ "#fce7f3", "#f9a8d4", "#ec4899", "#db2777", "#be185d", "#9d174d", "#831843", "#500724" } };

	private Textbox isianHex;
	private Label pratinjau;

	public MyColorbox() {
		this("#000000");
	}

	public MyColorbox(String color) {
		setReadonly(true);
		setWidth("110px");
		setSclass("ais-ce-colorbox-band");
		buatPopupWarna();
		setColor(color);
	}

	/** API kompat Colorbox: warna #rrggbb. */
	public String getColor() {
		String value = getValue();
		return normalisasi(value);
	}

	public void setColor(String color) {
		String warna = normalisasi(color);
		setValue(warna);
		terapkanWarnaKeKotak(warna);
		if (isianHex != null) {
			isianHex.setValue(warna);
		}
		if (pratinjau != null) {
			pratinjau.setStyle(gayaPratinjau(warna));
		}
	}

	private static String normalisasi(String color) {
		String warna = color == null ? "" : color.trim();
		if (warna.isEmpty()) {
			return "#000000";
		}
		if (!warna.startsWith("#")) {
			warna = "#" + warna;
		}
		return warna.toLowerCase();
	}

	/** Latar kotak bandbox mengikuti warna terpilih agar langsung terlihat. */
	private void terapkanWarnaKeKotak(String warna) {
		setStyle("background-color:" + warna + ";color:" + warnaKontras(warna)
				+ ";font-weight:bold;text-align:center;border-radius:8px;");
	}

	/** Teks putih untuk warna gelap, hitam untuk warna terang. */
	private static String warnaKontras(String hex) {
		try {
			int r = Integer.parseInt(hex.substring(1, 3), 16);
			int g = Integer.parseInt(hex.substring(3, 5), 16);
			int b = Integer.parseInt(hex.substring(5, 7), 16);
			return (r * 299 + g * 587 + b * 114) / 1000 >= 140 ? "#0f172a" : "#ffffff";
		} catch (Exception e) {
			return "#ffffff";
		}
	}

	private String gayaPratinjau(String warna) {
		return "display:inline-block;width:46px;height:24px;border-radius:6px;border:1px solid #cbd5e1;"
				+ "background-color:" + warna + ";";
	}

	private void buatPopupWarna() {
		Bandpopup popup = new Bandpopup();
		popup.setParent(this);
		popup.setStyle("padding:10px;width:236px;box-sizing:border-box;");

		Vbox isi = new Vbox();
		isi.setParent(popup);
		isi.setSpacing("6px");

		/* Grid palet: tiap swatch div kecil yang bisa diklik. */
		for (int baris = 0; baris < PALET.length; baris++) {
			Hbox kotakBaris = new Hbox();
			kotakBaris.setParent(isi);
			kotakBaris.setSpacing("2px");
			for (int kolom = 0; kolom < PALET[baris].length; kolom++) {
				final String warna = PALET[baris][kolom];
				Div swatch = new Div();
				swatch.setParent(kotakBaris);
				swatch.setStyle("width:22px;height:22px;border-radius:5px;cursor:pointer;"
						+ "border:1px solid rgba(15,23,42,.18);background-color:" + warna + ";");
				swatch.setTooltiptext(warna);
				swatch.addEventListener(Events.ON_CLICK, new EventListener() {
					public void onEvent(Event event) throws Exception {
						pilihWarna(warna);
					}
				});
			}
		}

		/* Isian hex manual + pratinjau + tombol pakai. */
		Hbox barisHex = new Hbox();
		barisHex.setParent(isi);
		barisHex.setSpacing("6px");
		barisHex.setAlign("center");

		pratinjau = new Label("");
		pratinjau.setParent(barisHex);
		pratinjau.setStyle(gayaPratinjau("#000000"));

		isianHex = new Textbox("#000000");
		isianHex.setParent(barisHex);
		isianHex.setCols(8);
		isianHex.setTooltiptext("Kode warna, contoh: #1d4ed8");
		isianHex.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				pilihWarna(isianHex.getValue());
			}
		});

		MyToolbarbuttonConfig pakai = new MyToolbarbuttonConfig("Pakai");
		pakai.setParent(barisHex);
		pakai.addEventListener(Events.ON_CLICK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				pilihWarna(isianHex.getValue());
			}
		});
	}

	private void pilihWarna(String warna) {
		setColor(warna);
		try {
			close();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/ui/util/MyColorbox.java:169");
		}
		/* Colorbox asli memberi tahu pemakainya lewat onChange. */
		Events.postEvent(Events.ON_CHANGE, this, getColor());
	}
}
