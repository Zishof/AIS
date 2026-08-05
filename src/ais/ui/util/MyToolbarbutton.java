package ais.ui.util;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.zul.Label;

/**
 * Pengganti seragam untuk MyButtonAwesome / MyButtonBorderAwesome
 * (keduanya sudah dihapus): semua tombol kini satu bentuk lewat
 * MyToolbarbuttonConfig sehingga gaya pill tema berlaku merata.
 *
 * API kompatibel dengan tombol lama:
 * - konstruktor (kelasIkonAwesome, label) dan (kelasIkonAwesome, label,
 *   tanpaBahasa): kelas ikon FontAwesome dipetakan ke aset gambar
 *   internal yang sudah ada; ikon tanpa padanan tampil sebagai tombol
 *   teks biasa (tetap seragam).
 * - getLabelC(): mengembalikan Label proxy yang setValue/setStyle-nya
 *   diteruskan ke tombol ini, supaya pemanggil lama tetap jalan.
 *
 * Kompatibel Java 1.7, ZK 5 dan ZK CE 9/10.
 */
public class MyToolbarbutton extends MyToolbarbuttonConfig {

	private static final long serialVersionUID = 1L;

	/**
	 * Pemetaan kelas FontAwesome lama -> aset SVG di /img/svg/. Mencakup
	 * SEMUA token fa-* yang dipakai bersama MyToolbarbutton di seluruh
	 * codebase (hasil pemindaian 2026-06-13); tiga ikon yang tidak punya
	 * padanan (award, hdd, plus-circle) dibuatkan SVG baru mengikuti pola
	 * Bootstrap Icons 16x16 fill=currentColor yang sama dengan ikon lain.
	 */
	private static final Map<String, String> PETA_IKON = new HashMap<String, String>();
	static {
		PETA_IKON.put("fa-address-book", "/img/svg/address-book-thin.svg");
		PETA_IKON.put("fa-ban", "/img/svg/close-circle-line.svg");
		PETA_IKON.put("fa-bars", "/img/svg/list-task.svg");
		PETA_IKON.put("fa-bell", "/img/svg/bell.svg");
		PETA_IKON.put("fa-bell-o", "/img/svg/bell.svg");
		PETA_IKON.put("fa-book", "/img/svg/book.svg");
		PETA_IKON.put("fa-calendar", "/img/svg/calendar3.svg");
		PETA_IKON.put("fa-calendar-check-o", "/img/svg/calendar-check.svg");
		PETA_IKON.put("fa-calendar-o", "/img/svg/calendar2.svg");
		PETA_IKON.put("fa-camera", "/img/svg/camera.svg");
		PETA_IKON.put("fa-certificate", "/img/svg/award.svg");
		PETA_IKON.put("fa-check", "/img/svg/check2.svg");
		PETA_IKON.put("fa-check-circle", "/img/svg/check2-circle.svg");
		PETA_IKON.put("fa-check-circle-o", "/img/svg/check2-circle.svg");
		PETA_IKON.put("fa-check-square", "/img/svg/check-square.svg");
		PETA_IKON.put("fa-check-square-o", "/img/svg/check-square.svg");
		PETA_IKON.put("fa-cloud-download", "/img/svg/download.svg");
		PETA_IKON.put("fa-cog", "/img/svg/gear.svg");
		PETA_IKON.put("fa-comments", "/img/svg/comment-2-text-line.svg");
		PETA_IKON.put("fa-credit-card-alt", "/img/svg/payments.svg");
		PETA_IKON.put("fa-desktop", "/img/svg/desktop-light.svg");
		PETA_IKON.put("fa-download", "/img/svg/download.svg");
		PETA_IKON.put("fa-eye", "/img/svg/eye.svg");
		PETA_IKON.put("fa-file-audio-o", "/img/svg/file-audio-thin.svg");
		PETA_IKON.put("fa-files-o", "/img/svg/copy.svg");
		PETA_IKON.put("fa-file-text-o", "/img/svg/file-earmark-text.svg");
		PETA_IKON.put("fa-file-video-o", "/img/svg/camera-video.svg");
		PETA_IKON.put("fa-floppy-o", "/img/svg/save-2-fill.svg");
		PETA_IKON.put("fa-graduation-cap", "/img/svg/graduation-cap-light.svg");
		PETA_IKON.put("fa-handshake-o", "/img/svg/person-check.svg");
		PETA_IKON.put("fa-hdd-o", "/img/svg/hdd.svg");
		PETA_IKON.put("fa-history", "/img/svg/clock-history.svg");
		PETA_IKON.put("fa-line-chart", "/img/svg/chart-line.svg");
		PETA_IKON.put("fa-plus", "/img/svg/add.svg");
		PETA_IKON.put("fa-plus-square", "/img/svg/plus-circle.svg");
		PETA_IKON.put("fa-save", "/img/svg/save-2-fill.svg");
		PETA_IKON.put("fa-sliders", "/img/svg/filter-square.svg");
		PETA_IKON.put("fa-star", "/img/svg/trophy.svg");
		PETA_IKON.put("fa-times", "/img/svg/deny.svg");
		PETA_IKON.put("fa-times-circle", "/img/svg/close-circle-line.svg");
		PETA_IKON.put("fa-id-card", "/img/svg/user-rectangle-light.svg");
		PETA_IKON.put("fa-info-circle", "/img/svg/information-circle-outline.svg");
		PETA_IKON.put("fa-link", "/img/svg/attachment-2.svg");
		PETA_IKON.put("fa-list-alt", "/img/svg/card-checklist.svg");
		PETA_IKON.put("fa-money", "/img/svg/money-bills.svg");
		PETA_IKON.put("fa-paperclip", "/img/svg/attachment-2.svg");
		PETA_IKON.put("fa-pencil", "/img/svg/pencil-square.svg");
		PETA_IKON.put("fa-pencil-square", "/img/svg/pencil-square.svg");
		PETA_IKON.put("fa-pencil-square-o", "/img/svg/pencil-square.svg");
		PETA_IKON.put("fa-plus-circle", "/img/svg/plus-circle.svg");
		PETA_IKON.put("fa-print", "/img/svg/printer.svg");
		PETA_IKON.put("fa-qrcode", "/img/svg/qrcode-scan.svg");
		PETA_IKON.put("fa-refresh", "/img/svg/refresh.svg");
		PETA_IKON.put("fa-search", "/img/svg/search.svg");
		PETA_IKON.put("fa-sign-in", "/img/svg/box-arrow-in-right.svg");
		PETA_IKON.put("fa-sign-out", "/img/svg/logout-line.svg");
		PETA_IKON.put("fa-sitemap", "/img/svg/account_tree.svg");
		PETA_IKON.put("fa-spinner", "/img/svg/process.svg");
		PETA_IKON.put("fa-table", "/img/svg/table.svg");
		PETA_IKON.put("fa-tasks", "/img/svg/list-check.svg");
		PETA_IKON.put("fa-th-list", "/img/svg/table-list.svg");
		PETA_IKON.put("fa-trash", "/img/svg/trash.svg");
		PETA_IKON.put("fa-upload", "/img/svg/upload.svg");
		PETA_IKON.put("fa-user", "/img/svg/user-circle-thin.svg");
		PETA_IKON.put("fa-user-circle", "/img/svg/user-circle-thin.svg");
		PETA_IKON.put("fa-users", "/img/svg/users.svg");
		PETA_IKON.put("fa-user-secret", "/img/svg/user-tie.svg");
		PETA_IKON.put("fa-volume-up", "/img/svg/sound-on.svg");
		PETA_IKON.put("fa-whatsapp", "/img/svg/whats.svg");
		PETA_IKON.put("fa-wpforms", "/img/svg/form-one.svg");
	}

	private Label labelProxy;

	public MyToolbarbutton() {
		super();
	}

	public MyToolbarbutton(String kelasIkonAwesome, String label) {
		this(kelasIkonAwesome, label, false);
	}

	public MyToolbarbutton(String kelasIkonAwesome, String label, boolean tanpaBahasa) {
		super();
		String gambar = petakanIkon(kelasIkonAwesome);
		if (gambar != null) {
			setImage(gambar);
		}
		if (label != null && !label.isEmpty()) {
			setLabel(label);
		} else if (gambar == null && kelasIkonAwesome != null && !kelasIkonAwesome.trim().isEmpty()) {
			/* ikon-only tanpa padanan gambar: jangan biarkan tombol kosong */
			setTooltiptext(kelasIkonAwesome);
		}
	}

	/** Ambil padanan gambar dari token fa-* pertama yang dikenal. */
	private static String petakanIkon(String kelasIkon) {
		if (kelasIkon == null) {
			return null;
		}
		String[] token = kelasIkon.trim().split("\\s+");
		for (int i = 0; i < token.length; i++) {
			String t = token[i].trim();
			if (t.isEmpty() || "fa".equals(t) || "fa-regular".equals(t) || "fa-solid".equals(t)
					|| "fa-big".equals(t)) {
				continue;
			}
			String gambar = PETA_IKON.get(t);
			if (gambar != null) {
				return gambar;
			}
		}
		return null;
	}

	/**
	 * Kompat MyButtonAwesome.getLabelC(): Label proxy yang meneruskan
	 * setValue/setStyle ke tombol ini.
	 */
	public Label getLabelC() {
		if (labelProxy == null) {
			final MyToolbarbutton tombol = this;
			labelProxy = new Label() {
				private static final long serialVersionUID = 1L;

				public void setValue(String value) {
					super.setValue(value);
					tombol.setLabel(value == null ? "" : value);
				}

				public String getValue() {
					String nilai = tombol.getLabel();
					return nilai == null ? "" : nilai;
				}

				public void setStyle(String style) {
					super.setStyle(style);
					if (style != null && !style.trim().isEmpty()) {
						String lama = tombol.getStyle();
						if (lama == null || lama.indexOf(style) < 0) {
							tombol.setStyle(lama == null || lama.trim().isEmpty() ? style : lama + ";" + style);
						}
					}
				}
			};
			labelProxy.setValue(getLabel());
		}
		return labelProxy;
	}
}
