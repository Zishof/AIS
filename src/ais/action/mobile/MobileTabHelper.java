package ais.action.mobile;

import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import ais.common.Common;

/**
 * <h1>MobileTabHelper — Pola "daftar bagian &rarr; buka konten layar penuh &rarr; kembali"</h1>
 *
 * <p>
 * Helper tampilan ini menyediakan <b>satu pola navigasi mobile yang dapat dipakai-ulang</b> untuk
 * halaman desktop yang aslinya memakai banyak <i>tab</i> (mis. e-Learning dengan tab Agenda,
 * Linimasa, Tugas/Ujian/Materi, Kalender, Dasbor, dst.). Di layar ponsel, menampilkan banyak tab
 * sekaligus terasa sempit dan rawan rusak. Karena itu pola yang dipakai adalah:
 * </p>
 * <ol>
 *   <li>Saat menu dibuka, tampilkan dulu <b>daftar bagian</b> (seperti daftar menu) — ringkas dan
 *       mudah dipindai.</li>
 *   <li>Saat sebuah bagian diketuk, tampilkan <b>konten bagian itu secara layar penuh</b> dengan
 *       tinggi pasti (agar komponen ber-{@code height:100%} dari logika lama tetap terisi).</li>
 *   <li>Sediakan tombol <b>Kembali</b> untuk balik ke daftar bagian.</li>
 * </ol>
 *
 * <h2>Kenapa pola ini penting &amp; dapat dipakai-ulang?</h2>
 * <p>
 * Banyak halaman lama berbagi struktur yang sama: sekumpulan tab, masing-masing memuat kontennya
 * sendiri (sering secara malas/lazy saat tab dipilih). Dengan {@code MobileTabHelper}, halaman
 * mobile cukup menyuplai <b>daftar bagian</b> berupa pasangan {@code (judul, ikon, pemuat-konten)};
 * seluruh mekanika tampilan (daftar, drill-down, tombol kembali, area konten ber-tinggi pasti)
 * ditangani di sini. Logika data tetap memakai fungsi lama melalui {@link Item.Loader}, sehingga
 * <b>tidak ada duplikasi logika</b> dan perawatan tampilan terpusat di satu tempat. Pola yang sama
 * bisa diterapkan ke menu mobile lain yang berbasis tab.
 * </p>
 *
 * <h2>Pemuatan malas (lazy) &amp; hemat memori</h2>
 * <p>
 * Konten sebuah bagian baru dimuat ketika bagian itu pertama kali dibuka, lalu dibersihkan saat
 * pengguna kembali — sehingga hanya satu bagian berat yang aktif pada satu waktu. Ini menjaga
 * pemakaian memori tetap kecil meski halaman aslinya kaya komponen.
 * </p>
 *
 * <h2>Ketahanan</h2>
 * <p>
 * Kegagalan memuat satu bagian ditangkap dan diganti pesan ramah; tidak merusak bagian lain.
 * Toggle tampil/sembunyi memakai {@code setVisible} sisi-server (tanpa skrip klien) sehingga
 * sederhana dan kompatibel ZK 5.x.
 * </p>
 *
 * <h2>Kontrak CSS</h2>
 * <p>
 * Memakai kelas {@code ais-m-drill-*} yang didefinisikan di {@code ais_mobile.css}:
 * {@code ais-m-drill-row} (baris bagian), {@code ais-m-drill-content} (tampilan konten),
 * {@code ais-m-drill-bar} (bilah kembali), {@code ais-m-drill-area} (area konten ber-tinggi pasti).
 * </p>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Java 1.7 (tanpa lambda) dan ZK 5.x.</p>
 *
 * @author e-Campus UI Team
 * @see MobileUiHelper
 */
public final class MobileTabHelper {

	private MobileTabHelper() {
	}

	/**
	 * <h3>Satu bagian (tab) untuk pola daftar &rarr; drill-down</h3>
	 *
	 * <p>Menyimpan judul, ikon, dan pemuat konten ({@link Loader}). Pemuat dipanggil saat bagian
	 * dibuka, dengan wadah area konten sebagai argumen.</p>
	 */
	public static final class Item {

		/** Antarmuka pemuat konten satu bagian; implementasinya memakai-ulang logika lama. */
		public interface Loader {
			/**
			 * Memuat konten bagian ke dalam wadah area.
			 *
			 * @param content wadah area konten (sudah ber-tinggi pasti).
			 * @throws Exception bila pemuatan gagal.
			 */
			void load(Component content) throws Exception;
		}

		private final String label;
		private final String icon;
		private final Loader loader;

		/**
		 * @param label  judul bagian.
		 * @param icon   URL ikon bagian (boleh null).
		 * @param loader pemuat konten bagian.
		 */
		public Item(String label, String icon, Loader loader) {
			this.label = label;
			this.icon = icon;
			this.loader = loader;
		}
	}

	/**
	 * Membangun pola "daftar bagian &rarr; konten &rarr; kembali" ke dalam {@code host}.
	 *
	 * @param host       komponen wadah (mis. akar halaman mobile).
	 * @param items      daftar bagian.
	 * @param tinggiArea nilai CSS tinggi area konten (mis. {@code "calc(100dvh - 175px)"}); bila
	 *                   null dipakai nilai bawaan yang mengisi layar.
	 */
	public static void build(Component host, List<Item> items, String tinggiArea) {
		if (host == null || items == null) {
			return;
		}

		final Div list = MobileUiHelper.div("ais-m-drill-list");
		final Div contentView = MobileUiHelper.div("ais-m-drill-content");
		contentView.setVisible(false);

		/* Bilah kembali + judul bagian aktif. */
		Div bar = MobileUiHelper.div("ais-m-drill-bar");
		Div back = MobileUiHelper.div("ais-m-drill-back");
		back.appendChild(MobileUiHelper.image("/img/svg/arrow-left.svg", null));
		back.appendChild(MobileUiHelper.label("Kembali", null));
		back.addEventListener("onClick", new EventListener() {
			public void onEvent(Event e) {
				contentView.setVisible(false);
				list.setVisible(true);
			}
		});
		bar.appendChild(back);
		final Label judul = MobileUiHelper.label("", "ais-m-drill-title");
		bar.appendChild(judul);
		contentView.appendChild(bar);

		final Div area = MobileUiHelper.div("ais-m-drill-area");
		String tinggi = (tinggiArea == null || tinggiArea.trim().length() == 0)
				? "height:calc(100vh - 175px);height:calc(100dvh - 175px);min-height:52vh;"
				: ("height:" + tinggiArea + ";min-height:52vh;");
		area.setStyle(tinggi);
		contentView.appendChild(area);

		host.appendChild(list);
		host.appendChild(contentView);

		for (int i = 0; i < items.size(); i++) {
			final Item item = items.get(i);
			Div row = MobileUiHelper.div("ais-m-drill-row");
			Div ic = MobileUiHelper.div("ais-m-drill-ic");
			ic.appendChild(MobileUiHelper.image(item.icon == null ? "/img/svg/list-task.svg" : item.icon, null));
			row.appendChild(ic);
			row.appendChild(MobileUiHelper.label(item.label, "ais-m-drill-lbl"));
			row.appendChild(MobileUiHelper.image("/img/svg/chevron-right.svg", "ais-m-drill-go"));
			row.addEventListener("onClick", new EventListener() {
				public void onEvent(Event e) {
					bukaBagian(item, judul, area, list, contentView);
				}
			});
			list.appendChild(row);
		}
	}

	/**
	 * Membuka satu bagian: menampilkan tampilan konten, mengisi judul, lalu memuat konten via
	 * {@link Item.Loader}. Kegagalan ditangani agar tidak merusak tampilan.
	 */
	private static void bukaBagian(Item item, Label judul, Div area, Div list, Div contentView) {
		try {
			judul.setValue(item.label == null ? "" : item.label);
			list.setVisible(false);
			contentView.setVisible(true);
			Common.clear(area);
			if (item.loader != null) {
				item.loader.load(area);
			}
		} catch (Exception ex) {
			Common.tampilErrorJikaAdmin(ex);
			try {
				area.appendChild(MobileUiHelper.empty("Belum ada data atau bagian ini gagal dimuat.",
						"/img/svg/folder2.svg"));
			} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/mobile/MobileTabHelper.java:186");
			}
		}
	}
}
