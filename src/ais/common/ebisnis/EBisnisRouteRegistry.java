package ais.common.ebisnis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry tunggal untuk namespace web eBisnis di bawah {@code /ebisnis/*}.
 *
 * <p>Registry ini sengaja tidak membaca nama class/JSP dari request. Semua
 * tujuan forward berasal dari daftar statis agar path tidak dapat dipakai
 * untuk membuka JSP di bawah {@code WEB-INF} secara arbitrer.</p>
 */
public final class EBisnisRouteRegistry {

	public static final String KIND_FORWARD = "FORWARD";
	public static final String KIND_REDIRECT = "REDIRECT";
	public static final String KIND_API = "API";
	public static final String KIND_BARU = "BARU";
	public static final String KIND_ASSET = "ASSET";

	public static final class Route {
		private final String kind;
		private final String target;
		private final boolean aisLoginRequired;
		private final Map<String, String> parameters;

		private Route(String kind, String target, boolean aisLoginRequired,
				Map<String, String> parameters) {
			this.kind = kind;
			this.target = target;
			this.aisLoginRequired = aisLoginRequired;
			this.parameters = parameters == null ? Collections.<String, String>emptyMap()
					: Collections.unmodifiableMap(parameters);
		}

		public String getKind() { return kind; }
		public String getTarget() { return target; }
		public boolean isAisLoginRequired() { return aisLoginRequired; }
		public Map<String, String> getParameters() { return parameters; }
	}

	private static final Map<String, Route> EXACT;
	private static final Map<String, Route> INVENTORY;

	static {
		Map<String, Route> exact = new LinkedHashMap<String, Route>();
		exact.put("/", forward("/WEB-INF/baru/ebisnis.jsp", false));
		exact.put("/auth/daftar", forward("/pendaftaran", false));
		exact.put("/auth/login", forwardWith("/EbisnisPublic", false, "aksi", "login"));
		exact.put("/auth/logout", forwardWith("/EbisnisPublic", false, "aksi", "logout"));
		exact.put("/auth/session", forward("/EbisnisPublic", false));
		exact.put("/daftar", forward("/pendaftaran", false));
		exact.put("/masuk", forwardWith("/EbisnisPublic", false, "aksi", "login"));
		exact.put("/keluar", forwardWith("/EbisnisPublic", false, "aksi", "logout"));
		exact.put("/dashboard", forward("/EbisnisPublic", false));
		exact.put("/api/v1", route(KIND_API, "/Api_eBisnis", false, null));
		exact.put("/api/v1/", route(KIND_API, "/Api_eBisnis", false, null));
		exact.put("/app", forward("/main", true));
		exact.put("/app/dashboard", forward("/main", true));
		exact.put("/app/inventory", forward("/WEB-INF/baru/modul/inventory/index.jsp", true));
		exact.put("/app/pos", baru("kantin", "ringkasan"));
		exact.put("/app/apotik", directModule("/WEB-INF/baru/modul/apotik/index.jsp"));
		exact.put("/app/emedik", directModule("/WEB-INF/baru/modul/emedik/index.jsp"));
		exact.put("/inventory", forward("/WEB-INF/baru/modul/inventory/index.jsp", true));
		exact.put("/pos", baru("kantin", null));
		exact.put("/apotik", directModule("/WEB-INF/baru/modul/apotik/index.jsp"));
		exact.put("/emedik", directModule("/WEB-INF/baru/modul/emedik/index.jsp"));
		exact.put("/ekoperasi", baru("kantin", null));
		exact.put("/pesantren", forward("/main", true));
		exact.put("/eschool", forward("/main", true));
		exact.put("/ecampus", forward("/main", true));
		exact.put("/belanja", forward("/kantin", false));
		exact.put("/platform", forward("/new", true));
		exact.put("/harga", redirect("/#section-harga"));
		exact.put("/tentang", redirect("/#section-about"));
		exact.put("/kontak", redirect("/#section-daftar"));
		exact.put("/syarat", redirect("/#section-daftar"));
		exact.put("/privasi", redirect("/#section-daftar"));
		exact.put("/presentasi", forward("/presentasi", false));
		exact.put("/proposal", forward("/proposal", false));
		exact.put("/pks", forward("/pks", false));
		exact.put("/penawaran", forward("/penawaran", false));
		exact.put("/dokumen/presentasi", forward("/presentasi", false));
		exact.put("/dokumen/proposal", forward("/proposal", false));
		exact.put("/dokumen/pks", forward("/pks", false));
		exact.put("/dokumen/penawaran", forward("/penawaran", false));
		EXACT = Collections.unmodifiableMap(exact);

		String[][] screens = new String[][] {
			{ "01", "data_supplier" }, { "02", "daftar_supplier" },
			{ "03", "detail_supplier_aktif" }, { "04", "data_customer" },
			{ "05", "daftar_customer" }, { "06", "detail_customer_aktif" },
			{ "07", "data_sales" }, { "08", "data_stok_barang" },
			{ "09", "laporan_opname" }, { "10", "cetak_laporan_opname" },
			{ "11", "harga_beli_jual" }, { "12", "cetak_harga_beli_jual" },
			{ "13", "cetak_harga_jual" }, { "14", "ekspor_harga_stok" },
			{ "15", "cetak_daftar_stok" }, { "16", "hasil_cetak_stok" },
			{ "17", "menu_master_harga" }, { "18", "harga_beli_supplier" },
			{ "19", "harga_jual_customer" }, { "20", "pembelian_supplier" },
			{ "21", "hutang_pembelian" }, { "22", "data_hutang_supplier" },
			{ "23", "hutang_dengan_lunas" }, { "24", "pembayaran_hutang" },
			{ "25", "riwayat_pembayaran_hutang" }, { "26", "cetak_pembayaran_hutang" },
			{ "27", "analisis_hutang" }, { "28", "cetak_faktur_pembelian" },
			{ "29", "laporan_pembelian_periode" }, { "30", "penjualan_sales" },
			{ "31", "piutang_penjualan" }, { "32", "data_piutang_customer" },
			{ "33", "piutang_dengan_lunas" }, { "34", "pembayaran_piutang" },
			{ "35", "riwayat_pembayaran_piutang" }, { "36", "cetak_pembayaran_piutang" },
			{ "37", "analisis_piutang_customer" }, { "38", "analisis_piutang_sales" },
			{ "39", "surat_perintah_sales" }, { "40", "nota_sales" },
			{ "41", "laporan_piutang" }, { "42", "cetak_laporan_piutang" },
			{ "43", "kas_jurnal" }, { "44", "data_perkiraan" },
			{ "45", "parameter_laba_rugi" }, { "46", "cetak_laba_rugi_kotor" },
			{ "47", "laporan_laba_rugi" }, { "48", "cetak_laporan_laba_rugi" }
		};
		Map<String, Route> inventory = new LinkedHashMap<String, Route>();
		for (int i = 0; i < screens.length; i++) {
			Route screen = forward("/WEB-INF/baru/modul/inventory/" + screens[i][1] + ".jsp", true);
			inventory.put(screens[i][0], screen);
			inventory.put(screens[i][1], screen);
		}
		INVENTORY = Collections.unmodifiableMap(inventory);
	}

	private EBisnisRouteRegistry() { }

	public static Route resolve(String normalizedPath) {
		Route exact = EXACT.get(normalizedPath);
		if (exact != null) return exact;

		if (normalizedPath.startsWith("/inventory/")) {
			String key = normalizedPath.substring("/inventory/".length());
			if (key.startsWith("screen/")) key = key.substring("screen/".length());
			return INVENTORY.get(key);
		}

		/* API Flutter lama tetap memakai JSON action pada body. Path tambahan di
		 * namespace baru sengaja diteruskan ke dispatcher yang sama agar rollout
		 * URL tidak memecah kontrak command/idempotency yang sudah dipakai klien. */
		if (normalizedPath.startsWith("/api/v1/")) {
			return route(KIND_API, "/Api_eBisnis", false, null);
		}

		Route module = resolveBaruModule(normalizedPath, "/apotik/", "apotik");
		if (module != null) return module;
		module = resolveBaruModule(normalizedPath, "/emedik/", "emedik");
		if (module != null) return module;
		module = resolveBaruModule(normalizedPath, "/pos/", "kantin");
		if (module != null) return module;
		module = resolveBaruModule(normalizedPath, "/ekoperasi/", "kantin");
		if (module != null) return module;

		if (normalizedPath.startsWith("/assets/")) {
			String relative = normalizedPath.substring("/assets/".length());
			if (isSafeAsset(relative)) return route(KIND_ASSET, "/" + relative, false, null);
		}
		return null;
	}

	public static Map<String, Route> inventoryRoutes() { return INVENTORY; }

	private static Route resolveBaruModule(String path, String prefix, String module) {
		if (!path.startsWith(prefix)) return null;
		String page = path.substring(prefix.length());
		if (!isSafeSlug(page)) return null;
		return baru(module, page);
	}

	private static boolean isSafeSlug(String value) {
		return value != null && value.matches("[a-z0-9][a-z0-9_-]{0,63}");
	}

	private static boolean isSafeAsset(String value) {
		if (value == null || value.length() == 0 || value.indexOf("..") >= 0
				|| value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0) return false;
		String lower = value.toLowerCase(Locale.ENGLISH);
		return lower.startsWith("css/") || lower.startsWith("js/")
				|| lower.startsWith("img/") || lower.startsWith("fonts/");
	}

	private static Route directModule(String target) {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("__requestAttribute:posDirectPage", "true");
		return route(KIND_FORWARD, target, true, parameters);
	}

	private static Route baru(String module, String page) {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put("p", module);
		if (page != null) parameters.put("s", page);
		return route(KIND_BARU, "/baru", true, parameters);
	}

	private static Route forward(String target, boolean auth) {
		return route(KIND_FORWARD, target, auth, null);
	}

	private static Route forwardWith(String target, boolean auth, String name, String value) {
		Map<String, String> parameters = new LinkedHashMap<String, String>();
		parameters.put(name, value);
		return route(KIND_FORWARD, target, auth, parameters);
	}

	private static Route redirect(String target) {
		return route(KIND_REDIRECT, target, false, null);
	}

	private static Route route(String kind, String target, boolean auth, Map<String, String> parameters) {
		return new Route(kind, target, auth, parameters);
	}
}
