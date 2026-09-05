package ais.common.test;

import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.EbisnisLaporanAkses;
import ais.common.EbisnisMenuKatalog;
import ais.common.HeadlessActionContext;
import ais.action.master.koperasi.helper.LaporanKatalogData;

/** Self-test tanpa database untuk kontrak RBAC kategori/laporan eBisnis. */
public final class EbisnisLaporanAksesSelfTest {

	private EbisnisLaporanAksesSelfTest() {
	}

	private static void check(boolean kondisi, String pesan) {
		if (!kondisi) {
			throw new AssertionError(pesan);
		}
	}

	private static JSONObject item(String id) throws Exception {
		JSONObject o = new JSONObject();
		o.put("id", id);
		o.put("judul", id);
		return o;
	}

	private static JSONArray katalog() throws Exception {
		JSONArray arr = new JSONArray();
		JSONObject a = new JSONObject();
		a.put("katId", "penjualan");
		a.put("kat", "Penjualan");
		JSONArray ai = new JSONArray();
		ai.put(item("faktur"));
		ai.put(item("duplikat"));
		a.put("items", ai);
		arr.put(a);

		JSONObject b = new JSONObject();
		b.put("katId", "keuangan");
		b.put("kat", "Keuangan");
		JSONArray bi = new JSONArray();
		bi.put(item("arus_kas"));
		bi.put(item("duplikat"));
		b.put("items", bi);
		arr.put(b);
		return arr;
	}

	public static void main(String[] args) throws Exception {
		JSONArray katalog = katalog();

		JSONObject lama = EbisnisMenuKatalog.urai(null);
		check(EbisnisLaporanAkses.semua(lama), "role lama wajib kompatibel: semua laporan");
		check(EbisnisLaporanAkses.saringKatalog(katalog, lama).length() == 2,
				"katalog role lama tidak boleh terpotong");

		JSONObject cfg = EbisnisLaporanAkses.defaultKonfigurasi();
		cfg.put(EbisnisLaporanAkses.KUNCI_SEMUA, false);
		JSONObject kategori = cfg.getJSONObject(EbisnisLaporanAkses.KUNCI_KATEGORI);
		JSONObject laporan = cfg.getJSONObject(EbisnisLaporanAkses.KUNCI_LAPORAN);
		kategori.put("penjualan", true);
		kategori.put("keuangan", false);
		laporan.put(EbisnisLaporanAkses.kunciLaporan("penjualan", "faktur"), true);
		laporan.put(EbisnisLaporanAkses.kunciLaporan("penjualan", "duplikat"), false);
		JSONObject terbatas = EbisnisMenuKatalog.defaultObj();
		terbatas.put(EbisnisLaporanAkses.KUNCI, cfg);

		JSONArray tersaring = EbisnisLaporanAkses.saringKatalog(katalog, terbatas);
		check(tersaring.length() == 1, "kategori tanpa laporan berizin wajib disembunyikan");
		check(tersaring.getJSONObject(0).getJSONArray("items").length() == 1,
				"hanya laporan yang dicentang boleh dikirim");
		check(EbisnisLaporanAkses.bolehMenjalankan(katalog, terbatas, "faktur"),
				"laporan berizin ditolak");
		check(!EbisnisLaporanAkses.bolehMenjalankan(katalog, terbatas, "arus_kas"),
				"kategori nonaktif masih dapat dijalankan");
		check(!EbisnisLaporanAkses.bolehMenjalankan(katalog, terbatas, "tidak_ada"),
				"id laporan tak dikenal wajib fail-closed");

		kategori.put("keuangan", true);
		laporan.put(EbisnisLaporanAkses.kunciLaporan("keuangan", "duplikat"), true);
		check(EbisnisLaporanAkses.bolehMenjalankan(katalog, terbatas, "duplikat"),
				"id duplikat yang diizinkan pada kategori lain harus sah");

		JSONObject hasilUrai = EbisnisMenuKatalog.urai(terbatas.toString());
		check(!EbisnisLaporanAkses.semua(hasilUrai), "urai membuang mode pembatasan");
		check(EbisnisLaporanAkses.bolehMenjalankan(katalog, hasilUrai, "faktur"),
				"urai membuang pilihan laporan");

		hasilUrai.put("supervisor", true);
		check(EbisnisLaporanAkses.bolehMenjalankan(katalog, hasilUrai, "tidak_ada"),
				"supervisor harus melewati pembatasan rinci");

		HeadlessActionContext.enter();
		try {
			JSONArray nyata = LaporanKatalogData.katalog();
			check(nyata.length() >= 30, "katalog nyata kehilangan kategori");
			java.util.Set<String> kategoriIds = new java.util.HashSet<String>();
			int jumlahLaporan = 0;
			for (int i = 0; i < nyata.length(); i++) {
				JSONObject kat = nyata.getJSONObject(i);
				String katId = kat.optString("katId", "");
				check(katId.length() > 0, "kategori nyata tanpa katId");
				check(kategoriIds.add(katId), "katId katalog nyata duplikat: " + katId);
				JSONArray items = kat.optJSONArray("items");
				jumlahLaporan += items == null ? 0 : items.length();
			}
			check(jumlahLaporan >= 150, "katalog nyata kehilangan laporan");
		} finally {
			HeadlessActionContext.exit();
		}

		System.out.println("EbisnisLaporanAksesSelfTest OK: default-all, filter, deny, duplicate, supervisor, katalog nyata");
	}
}
