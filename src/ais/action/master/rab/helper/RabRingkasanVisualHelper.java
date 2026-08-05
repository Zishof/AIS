package ais.action.master.rab.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;

import ais.action.master.rab.util.WorkspaceTreeModel;
import ais.common.Common;
import ais.database.model.rab.Workspace;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.DashboardUiKit.Stat;

/**
 * Pembangun <b>visual ringkasan Anggaran vs Realisasi (RAB)</b> berbasis HTML/CSS murni
 * (memakai {@link DashboardUiKit}) - TANPA JFreeChart - yang dapat dipakai ulang oleh halaman
 * Realisasi/Anggaran mana pun.
 *
 * <p>
 * Tujuannya menyajikan gambaran cepat: berapa total pagu (anggaran) yang direncanakan, berapa yang
 * sudah terserap (realisasi), berapa sisanya, dan bagaimana sebarannya per kelompok besar (item
 * anggaran tingkat atas) - sehingga pengguna tidak perlu menelusuri seluruh pohon hanya untuk
 * mengetahui kondisi penyerapan anggaran.
 * </p>
 *
 * <h3>Sumber data</h3>
 * Helper ini TIDAK melakukan kueri sendiri. Ia membaca dari {@link WorkspaceTreeModel} yang sudah
 * dibangun oleh halaman pemanggil (model yang sama dengan yang dipasang ke pohon), lalu menelusuri
 * anak-anak akar lewat {@code getRoot()}, {@code getChildCount(root)}, dan {@code getChild(root, i)}.
 * Untuk tiap kelompok tingkat atas dibaca {@code Workspace.getHargaTotal()} sebagai pagu/anggaran dan
 * {@code Workspace.getRealisasiProses()} sebagai realisasi - yaitu getter yang SAMA dengan yang dipakai
 * oleh perender pohon - sehingga angka pada visual selalu konsisten dengan angka yang tampil di pohon.
 * Karena tidak ada {@code openSession()}/{@code currentNativeSession()} yang dibuka di sini, tidak ada
 * session yang perlu ditutup; model yang memakai session melakukannya secara internal.
 *
 * <h3>Isi visual</h3>
 * <ul>
 * <li><b>Spanduk pengantar</b> + <b>kartu metrik</b>: Total Anggaran, Total Realisasi, Sisa Anggaran,
 * Tingkat Serapan (%), dan Jumlah Kelompok.</li>
 * <li><b>Batang ganda (groupedBar)</b> Anggaran vs Realisasi per kelompok - membandingkan pagu dan
 * penyerapan secara berdampingan.</li>
 * <li><b>Donat (donut)</b> komposisi anggaran - porsi pagu tiap kelompok terhadap total.</li>
 * <li><b>Garis progres (progressLines)</b> tingkat serapan tiap kelompok (realisasi / pagu).</li>
 * <li><b>Daftar batang (barList)</b> realisasi terbesar - kelompok dengan penyerapan tertinggi.</li>
 * <li><b>Sorotan (insight)</b> - serapan, sisa, kelompok dengan anggaran terbesar, serta serapan
 * tertinggi dan terendah.</li>
 * </ul>
 * Bila kelompok tingkat atas lebih dari {@link #MAKS_KELOMPOK}, kelebihannya digabung menjadi satu
 * entri "Lainnya" agar grafik tetap mudah dibaca; angka total tetap mencakup seluruh kelompok.
 *
 * <h3>Tampilan &amp; ketahanan</h3>
 * Seluruh keluaran adalah HTML/CSS responsif dari {@link DashboardUiKit} (grid yang otomatis turun
 * baris di layar sempit), sehingga rapi baik di desktop maupun ponsel dan tidak bergantung pustaka
 * grafik eksternal. Method {@link #render(Component, WorkspaceTreeModel)} membersihkan isi kontainer
 * lebih dulu (aman dipanggil berulang saat pencarian di-refresh) dan dibungkus {@code try/catch}
 * sehingga kegagalan membangun visual tidak akan menggagalkan render pohon utama.
 *
 * <h3>Catatan teknis</h3>
 * Kompatibel Java 1.7 dan ZK 5.5: tanpa lambda/stream, memakai {@code Comparator} eksplisit dan
 * perulangan biasa. Penelusuran {@code getChild} untuk sedikit kelompok tingkat atas berbiaya ringan;
 * cocok dipanggil saat halaman dimuat/di-refresh.
 */
public class RabRingkasanVisualHelper {

	/** Jumlah maksimum kelompok yang ditampilkan terpisah pada grafik; sisanya digabung "Lainnya". */
	private static final int MAKS_KELOMPOK = 8;

	private RabRingkasanVisualHelper() {
		// utilitas statis - tidak untuk di-instansiasi
	}

	/** Wadah ringan satu kelompok anggaran tingkat atas. */
	private static final class Kelompok {
		final String label;
		final double anggaran;
		final double realisasi;

		Kelompok(String label, double anggaran, double realisasi) {
			this.label = label;
			this.anggaran = anggaran;
			this.realisasi = realisasi;
		}
	}

	/**
	 * Bangun dan tempelkan visual ringkasan ke {@code container} berdasarkan {@code model} yang sudah jadi.
	 * Aman dipanggil berulang (kontainer dibersihkan dahulu) dan tidak melempar exception ke pemanggil.
	 *
	 * @param container komponen induk tempat visual ditempel (mis. sebuah {@code Div})
	 * @param model     model pohon RAB yang sudah dibangun oleh halaman; boleh {@code null}
	 */
	public static void render(Component container, WorkspaceTreeModel model) {
		if (container == null) {
			return;
		}
		try {
			Common.clear(container);
			if (model == null) {
				container.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
						"Pilih Tahun Anggaran, Satuan Kerja, dan Sumber Dana untuk menampilkan ringkasan.")));
				return;
			}

			Object root = model.getRoot();
			int n = root == null ? 0 : model.getChildCount(root);

			List<Kelompok> items = new ArrayList<Kelompok>();
			double totalAnggaran = 0.0;
			double totalRealisasi = 0.0;
			for (int i = 0; i < n; i++) {
				Workspace w = null;
				try {
					w = (Workspace) model.getChild(root, i);
				} catch (Exception exChild) {
					w = null;
				}
				if (w == null) {
					continue;
				}
				double ang = w.getHargaTotal() == null ? 0.0 : w.getHargaTotal().doubleValue();
				double real = 0.0;
				try {
					Double r = w.getRealisasiProses();
					real = r == null ? 0.0 : r.doubleValue();
				} catch (Exception exReal) {
					real = 0.0;
				}
				items.add(new Kelompok(w.toString(), ang, real));
				totalAnggaran += ang;
				totalRealisasi += real;
			}

			double sisa = totalAnggaran - totalRealisasi;
			int serapPct = DashboardUiKit.pct(totalRealisasi, totalAnggaran);

			// Urutkan menurun berdasarkan anggaran agar kelompok terpenting tampil dahulu.
			Collections.sort(items, new Comparator<Kelompok>() {
				public int compare(Kelompok a, Kelompok b) {
					return Double.compare(b.anggaran, a.anggaran);
				}
			});

			// Ambil sebagian besar + gabung sisanya jadi "Lainnya" supaya grafik tetap terbaca.
			LinkedHashMap<String, Double> angTop = new LinkedHashMap<String, Double>();
			LinkedHashMap<String, Double> realTop = new LinkedHashMap<String, Double>();
			double angLain = 0.0;
			double realLain = 0.0;
			for (int i = 0; i < items.size(); i++) {
				Kelompok it = items.get(i);
				if (i < MAKS_KELOMPOK) {
					String key = DashboardUiKit.shorten(it.label, 42);
					if (key == null || key.trim().length() == 0) {
						key = "(tanpa nama)";
					}
					while (angTop.containsKey(key)) {
						key = key + " ";
					}
					angTop.put(key, it.anggaran);
					realTop.put(key, it.realisasi);
				} else {
					angLain += it.anggaran;
					realLain += it.realisasi;
				}
			}
			if (angLain > 0 || realLain > 0) {
				angTop.put("Lainnya", angLain);
				realTop.put("Lainnya", realLain);
			}

			LinkedHashMap<String, Integer> serapPerKel = new LinkedHashMap<String, Integer>();
			for (Map.Entry<String, Double> e : angTop.entrySet()) {
				double a = e.getValue() == null ? 0.0 : e.getValue().doubleValue();
				Double rv = realTop.get(e.getKey());
				double r = rv == null ? 0.0 : rv.doubleValue();
				serapPerKel.put(e.getKey(), DashboardUiKit.pct(r, a));
			}

			StringBuilder h = new StringBuilder();
			h.append(DashboardUiKit.introBanner("Ringkasan Anggaran vs Realisasi",
					"Membandingkan pagu anggaran dengan penyerapan (realisasi) per kelompok besar. "
							+ "Angka mengikuti yang tampil pada pohon anggaran di bawah."));

			List<Stat> stats = new ArrayList<Stat>();
			stats.add(new Stat("Total Anggaran", DashboardUiKit.money(totalAnggaran), "Pagu seluruh item",
					DashboardUiKit.PRIMARY));
			stats.add(new Stat("Total Realisasi", DashboardUiKit.money(totalRealisasi), "Anggaran yang sudah terpakai",
					DashboardUiKit.GOOD));
			stats.add(new Stat("Sisa Anggaran", DashboardUiKit.money(sisa), "Belum terpakai",
					sisa < 0 ? DashboardUiKit.BAD : DashboardUiKit.WARN));
			stats.add(new Stat("Tingkat Serapan", serapPct + "%", "Realisasi dibanding pagu",
					serapPct >= 50 ? DashboardUiKit.GOOD : DashboardUiKit.ACCENT));
			stats.add(new Stat("Jumlah Kelompok", String.valueOf(items.size()), "Item anggaran tingkat atas",
					DashboardUiKit.ACCENT));
			h.append(DashboardUiKit.cards(stats));

			h.append(DashboardUiKit.openGrid(320));
			h.append(DashboardUiKit.groupedBar("Anggaran vs Realisasi per Kelompok",
					"Batang biru = pagu anggaran, hijau = realisasi.", angTop, realTop, "Anggaran", "Realisasi",
					DashboardUiKit.PRIMARY, DashboardUiKit.GOOD, true));
			h.append(DashboardUiKit.donut("Komposisi Anggaran", "Porsi pagu tiap kelompok terhadap total.", angTop, true,
					"Belum ada anggaran."));
			h.append(DashboardUiKit.closeGrid());

			h.append(DashboardUiKit.openGrid(320));
			h.append(DashboardUiKit.progressLines("Tingkat Serapan per Kelompok",
					"Persentase realisasi terhadap pagu masing-masing kelompok.", serapPerKel));
			h.append(DashboardUiKit.barList("Realisasi Terbesar", "Kelompok dengan penyerapan terbesar.", realTop,
					DashboardUiKit.GOOD, null, true, "Belum ada realisasi."));
			h.append(DashboardUiKit.closeGrid());

			LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
			info.put("Tingkat Serapan", serapPct + "%");
			info.put("Sisa Anggaran", DashboardUiKit.money(sisa));
			if (!items.isEmpty()) {
				info.put("Anggaran Terbesar", DashboardUiKit.shorten(items.get(0).label, 36));
				Kelompok tinggi = null;
				Kelompok rendah = null;
				for (int i = 0; i < items.size(); i++) {
					Kelompok it = items.get(i);
					if (it.anggaran <= 0) {
						continue;
					}
					double rasio = it.realisasi / it.anggaran;
					if (tinggi == null || rasio > (tinggi.realisasi / tinggi.anggaran)) {
						tinggi = it;
					}
					if (rendah == null || rasio < (rendah.realisasi / rendah.anggaran)) {
						rendah = it;
					}
				}
				if (tinggi != null) {
					info.put("Serapan Tertinggi", DashboardUiKit.shorten(tinggi.label, 28) + " ("
							+ DashboardUiKit.pct(tinggi.realisasi, tinggi.anggaran) + "%)");
				}
				if (rendah != null) {
					info.put("Serapan Terendah", DashboardUiKit.shorten(rendah.label, 28) + " ("
							+ DashboardUiKit.pct(rendah.realisasi, rendah.anggaran) + "%)");
				}
			}
			h.append(DashboardUiKit.insight("Sorotan", "Beberapa angka penting dari anggaran dan realisasi.", info));

			container.appendChild(DashboardUiKit.html(h.toString()));
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}
}
