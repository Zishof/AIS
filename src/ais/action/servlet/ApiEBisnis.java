package ais.action.servlet;

/**
 * <b>ApiEBisnis</b> -- endpoint bermerek {@code /Api_eBisnis}, titik masuk API resmi untuk lini
 * produk "eBisnis" (POS/Koperasi) di bawah payung "Platform Core" (lihat dokumen analisis gap
 * Flutter, §2 v2.2 dan §6 Fase 0/1). Klien Flutter baru ({@code apps/ebisnis} di
 * {@code C:\opt\CodeBaseDesktopDanMobile}) memanggil URL ini, BUKAN {@code /PosApi} langsung --
 * supaya penamaan endpoint sudah konsisten dengan rencana pemisahan modul per lini produk
 * ({@code /Api_eSchooleCampus}, {@code /Api_eMedikeKlinikeFarmasi}, {@code /Api_Umum}, dst., yang
 * masing-masing akan jadi servlet terpisah saat lini produknya mulai dibangun).
 *
 * <p>SENGAJA hanya {@code extends PosApi} tanpa override apa pun -- konsisten dengan prinsip
 * "JANGAN ubah server, pilot murni klien baru memanggil API yang sudah ada" (rekomendasi Fase 1
 * dokumen analisis). Seluruh kontrak JSON (aksi, payload, balasan) IDENTIK dengan {@code /PosApi};
 * kelas ini murni alias bermerek supaya klien Flutter tidak perlu tahu nama servlet lama. Begitu
 * fitur eBisnis genuinely menyimpang dari POS existing (mis. billing Smartlink, multi-tenant), aksi
 * BARU ditambahkan di sini (bukan di {@link PosApi}) supaya jalur lama (Desktop Electron/Android
 * Capacitor yang masih dipakai selama masa transisi, lihat kebijakan §1 dokumen analisis) tidak
 * ikut terekspos ke aksi yang belum matang.</p>
 */
public class ApiEBisnis extends PosApi {
	private static final long serialVersionUID = 1L;

	/**
	 * Seed idempoten role varian "Inventory &amp; Sales" ({@code pemilik_sales_inventory},
	 * {@code sales_keliling}) saat servlet dimuat -- aman dipanggil berulang (lookup by roleId),
	 * dan kegagalannya TIDAK menjatuhkan servlet (dicoba ulang di load berikutnya).
	 */
	@Override
	public void init() throws javax.servlet.ServletException {
		super.init();
		try {
			ais.action.servlet.api.SalesInventoryHelper.pastikanSeedRole();
			ais.action.servlet.api.SalesInventoryHelper.pastikanSeedAkunUatLokal();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApiEBisnis.init: seed role inventory_sales");
		}
		try {
			ais.action.servlet.api.ApotikEmedikSeedHelper.pastikanSeed();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApiEBisnis.init: seed menu apotik/emedik");
		}
		try {
			ais.action.servlet.api.SalesInventoryTripHelper.pastikanSeedKategoriBiaya();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "ApiEBisnis.init: seed kategori biaya sales");
		}
	}

	/**
	 * Titik masuk seluruh aksi baru lini eBisnis (prefix {@code si_} utk varian Inventory &amp;
	 * Sales) -- lihat JavaDoc {@code PosApi.prosesAksiTambahan}: dipanggil setelah autentikasi
	 * token, TEPAT sebelum fallback "Aksi tidak dikenal". Endpoint {@code /PosApi} lama tidak
	 * pernah melihat aksi ini (hook default di PosApi selalu {@code false}).
	 */
	@Override
	protected boolean prosesAksiTambahan(String action, ais.database.model.Tbmuser tbmuser,
			org.json.JSONObject payload, org.json.JSONObject hasil,
			javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
			throws Exception {
		if (ais.action.servlet.api.SalesInventoryApiDispatcher.dispatch(action, tbmuser, payload, hasil,
				request, response)) {
			return true;
		}
		// Varian "POS Apotik" (prefix apotik_) -- gerbang menu fail-closed sudah lewat
		// bolehAksesActionKantin; dispatcher berikut murni rute + normalisasi.
		return ais.action.servlet.api.ApotikApiDispatcher.dispatch(action, tbmuser, payload, hasil);
	}
}
