package ais.action.mobile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Column;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

/**
 * Helper tampilan eLearning mobile.
 *
 * <p>Helper ini berisi potongan UI kecil yang dapat dipakai ulang oleh
 * halaman eLearning mobile tanpa bergantung pada layout desktop. Tujuannya
 * menjaga controller tetap ringkas: controller memilih data dan alur event,
 * sedangkan helper membuat kartu ringkasan, tabel ZK, grafik HTML/CSS, tombol
 * aksi, dan daftar kegiatan. Semua komponen yang dibuat adalah komponen ZK
 * standar versi 5.x seperti {@link Div}, {@link Grid}, {@link Rows},
 * {@link Row}, {@link Label}, dan {@link Html}. Tidak ada dependensi ke
 * JFreeChart, canvas server-side, atau library modern yang berisiko tidak
 * tersedia di instalasi lama. Grafik ringkas dibuat dari elemen HTML biasa
 * dengan class CSS yang ditambahkan ke {@code ais_mobile.css}; hasilnya lebih
 * ringan di ponsel dan tetap bisa dibaca saat halaman dibuka di desktop.</p>
 */
public final class ElearningMobileUiHelper {

	private ElearningMobileUiHelper() {
	}

	public static Div shellSection(String title, String description) {
		Div section = div("ais-el-section");
		section.appendChild(label(title, "ais-el-section-title"));
		section.appendChild(label(description, "ais-el-section-desc"));
		return section;
	}

	public static Div metricCard(String value, String label, String hint, String tone) {
		Div card = div("ais-el-metric " + safe(tone));
		card.appendChild(label(value, "ais-el-metric-value"));
		card.appendChild(label(label, "ais-el-metric-label"));
		card.appendChild(label(hint, "ais-el-metric-hint"));
		return card;
	}

	public static Div actionCard(String icon, String title, String description, EventListener listener) {
		Div card = div("ais-el-action");
		card.appendChild(MobileUiHelper.image(icon, "ais-el-action-icon"));
		Div body = div("ais-el-action-body");
		body.appendChild(label(title, "ais-el-action-title"));
		body.appendChild(label(description, "ais-el-action-desc"));
		card.appendChild(body);
		card.appendChild(label(">", "ais-el-action-go"));
		if (listener != null) {
			card.addEventListener("onClick", listener);
		}
		return card;
	}

	public static Div progressCard(String title, String description, int percent) {
		if (percent < 0) {
			percent = 0;
		}
		if (percent > 100) {
			percent = 100;
		}
		Div card = div("ais-el-card");
		Hbox head = new Hbox();
		head.setSclass("ais-el-progress-head");
		head.appendChild(label(title, "ais-el-card-title"));
		head.appendChild(label(String.valueOf(percent) + "%", "ais-el-progress-number"));
		card.appendChild(head);
		card.appendChild(label(description, "ais-el-card-desc"));
		Div track = div("ais-el-progress-track");
		Div fill = div("ais-el-progress-fill");
		fill.setStyle("width:" + percent + "%");
		track.appendChild(fill);
		card.appendChild(track);
		return card;
	}

	public static Div trendCard(String title, String description, int[] values) {
		Div card = div("ais-el-card");
		card.appendChild(label(title, "ais-el-card-title"));
		card.appendChild(label(description, "ais-el-card-desc"));
		Div bars = div("ais-el-trend");
		if (values != null) {
			for (int i = 0; i < values.length; i++) {
				int value = values[i];
				if (value < 8) {
					value = 8;
				}
				if (value > 100) {
					value = 100;
				}
				Div bar = div("ais-el-trend-bar");
				bar.setStyle("height:" + value + "%");
				bars.appendChild(bar);
			}
		}
		card.appendChild(bars);
		return card;
	}

	public static Div radarCard(String title, String description, int materi, int tugas, int diskusi, int ujian) {
		Div card = div("ais-el-card");
		card.appendChild(label(title, "ais-el-card-title"));
		card.appendChild(label(description, "ais-el-card-desc"));
		int a = clamp(materi);
		int b = clamp(tugas);
		int c = clamp(diskusi);
		int d = clamp(ujian);
		Div radar = div("ais-el-radar");
		radar.setStyle("background:conic-gradient(#16794a 0 " + a + "%,#2f7fc1 " + a + "% " + (a + b)
				+ "%,#f5b942 " + (a + b) + "% " + (a + b + c) + "%,#d95454 " + (a + b + c) + "% "
				+ (a + b + c + d) + "%,#e8edea " + (a + b + c + d) + "% 100%)");
		radar.appendChild(label("Belajar", "ais-el-radar-core"));
		card.appendChild(radar);
		card.appendChild(label("Materi, tugas, diskusi, dan ujian dibandingkan dalam satu ringkasan.", "ais-el-card-desc"));
		return card;
	}

	public static Grid learningGrid() {
		Grid grid = new Grid();
		grid.setSclass("ais-el-grid");
		grid.setWidth("100%");
		Columns columns = new Columns();
		columns.appendChild(column("Kegiatan", "35%"));
		columns.appendChild(column("Status", "20%"));
		columns.appendChild(column("Waktu", "20%"));
		columns.appendChild(column("Catatan", "25%"));
		grid.appendChild(columns);
		Rows rows = new Rows();
		grid.appendChild(rows);
		appendRow(rows, "Materi Basis Data", "Dibaca", "Hari ini", "Lanjutkan dari modul normalisasi.");
		appendRow(rows, "Tugas Pemrograman Web", "Perlu dikumpulkan", "3 hari lagi", "Periksa instruksi sebelum unggah.");
		appendRow(rows, "Diskusi Sistem Informasi", "Aktif", "Minggu ini", "Ada balasan baru dari kelas.");
		appendRow(rows, "Ujian Online", "Terjadwal", "Pekan depan", "Persiapkan koneksi dan perangkat.");
		return grid;
	}

	public static Button primaryButton(String label, EventListener listener) {
		Button button = new Button(label);
		button.setSclass("ais-el-button-primary");
		if (listener != null) {
			button.addEventListener("onClick", listener);
		}
		return button;
	}

	public static Toolbarbutton linkButton(String label, String icon, EventListener listener) {
		Toolbarbutton button = new Toolbarbutton(label);
		button.setImage(icon);
		button.setSclass("ais-el-link-button");
		if (listener != null) {
			button.addEventListener("onClick", listener);
		}
		return button;
	}

	public static Html html(String content) {
		return new Html(content == null ? "" : content);
	}

	public static Div div(String sclass) {
		Div div = new Div();
		div.setSclass(sclass);
		return div;
	}

	public static Label label(String text, String sclass) {
		Label label = new Label(text == null ? "" : text);
		label.setSclass(sclass);
		return label;
	}

	public static void clear(Component component) {
		if (component != null) {
			component.getChildren().clear();
		}
	}

	private static Column column(String label, String width) {
		Column column = new Column(label);
		column.setWidth(width);
		return column;
	}

	private static void appendRow(Rows rows, String kegiatan, String status, String waktu, String catatan) {
		Row row = new Row();
		row.appendChild(label(kegiatan, "ais-el-cell-title"));
		row.appendChild(label(status, "ais-el-cell-status"));
		row.appendChild(label(waktu, "ais-el-cell"));
		row.appendChild(label(catatan, "ais-el-cell"));
		rows.appendChild(row);
	}

	private static int clamp(int value) {
		if (value < 1) {
			return 1;
		}
		if (value > 70) {
			return 70;
		}
		return value;
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}
}
