package ais.common.newui.menu;

import java.io.Serializable;

import ais.common.newui.NewUiRouteRegistry;

/** Resolver route New UI native dengan status eksplisit dan fail-closed. */
public final class NewUiHybridMenuRouteRegistry {

    public static final String NEW_UI = "NEW_UI";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_MAPPED = "NOT_MAPPED";
    public static final String NOT_FOUND = "NOT_FOUND";

    /**
     * Tipe implementasi bersarang {@link ResolvedRoute} milik {@link NewUiHybridMenuRouteRegistry}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiHybridMenuRouteRegistry}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String status}, {@code String
     * module}, {@code String page}, {@code String legacyUrl}; operasi lokal: {@code getStatus()}, {@code
     * getModule()}, {@code getPage()}, {@code getLegacyUrl()}, {@code isValid}(). Aturan bisnis bersama tetap
     * berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiHybridMenuRouteRegistry
     */
    public static final class ResolvedRoute implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String status;
        private final String module;
        private final String page;
        private final String legacyUrl;

        ResolvedRoute(String status, String module, String page, String legacyUrl) {
            this.status = status; this.module = module; this.page = page; this.legacyUrl = legacyUrl;
        }
        public String getStatus() { return status; }
        public String getModule() { return module; }
        public String getPage() { return page; }
        public String getLegacyUrl() { return legacyUrl; }
        public boolean isValid() { return NEW_UI.equals(status); }
    }

    private NewUiHybridMenuRouteRegistry() { }

    public static ResolvedRoute resolve(Long menuId, String existingUrl, boolean openNewWindow) {
        NewUiRouteRegistry.Route route = NewUiRouteRegistry.routeForMenuIdAndUrl(menuId, existingUrl);
        if (route != null) return new ResolvedRoute(NEW_UI, route.getModule(), route.getPage(), null);
        // Native-only: URL lama hanya boleh dipakai sebagai kunci pencarian registry.
        // Menu berizin yang belum mempunyai mapping eksplisit tetap membuka
        // resolver JSP JSON/native. URL lama tidak pernah dirender, di-embed,
        // atau dijadikan tujuan redirect ke ZKoss.
        if (NewUiRouteRegistry.isSafeLegacyUrl(existingUrl)) {
            return new ResolvedRoute(NEW_UI, "_shared", "native_menu", null);
        }
        return new ResolvedRoute(NOT_MAPPED, null, null, null);
    }
}
