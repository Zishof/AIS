package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Kontrak satu-ke-satu seluruh toolbarbutton pada index.zul existing. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiIndexToolbarParityService {
    private static final Map ITEMS;
    static {
        Map values = new LinkedHashMap();
        utility(values, "usersOnline", "onLihatOnline", "Pengguna online", "nuiShellOnlineDialog");
        utility(values, "eMenuButton", "onPopup", "Menu utama", "nuiMenuToggle");
        module(values, "eMedicButton", "onEmedic", "emedic");
        module(values, "eLearningButton", "onPengumumanPerkuliahan", "elearning");
        module(values, "ePrestasiButton", "onKegiatanDanPrestasi", "prestasi");
        module(values, "ePustakaButton", "onPustaka", "pustaka");
        module(values, "eWorkflowButton", "onWorkflow", "workflow");
        module(values, "eRepositoryButton", "onRepository", "repository");
        module(values, "eAntarJemputButton", "onAntarJemput", "antar_jemput");
        module(values, "eSpmiButton", "onSpmi", "spmi");
        module(values, "eKantinButton", "onKantin", "toko");
        module(values, "eKoperasiButton", "onKoperasi", "koperasi");
        module(values, "eAkademikButton", "onDashboard", "akademik");
        module(values, "eAdministrasiButton", "onAdministrasi", "administrasi");
        module(values, "ePengadaanButton", "onPengadaan", "pengadaan");
        module(values, "ePembayaranButton", "onPembayaran", "pembayaran");
        module(values, "eKeuanganButton", "onKeuangan", "keuangan");
        module(values, "eAkuntingButton", "onAkunting", "akuntansi");
        module(values, "eKepegawaianButton", "onKepegawaian", "kepegawaian");
        module(values, "eGajiButton", "onGaji", "gaji");
        module(values, "eKinerjaButton", "onKinerja", "kinerja");
        module(values, "ePresensiButton", "onPresensi", "presensi");
        module(values, "eKalenderAkademikButton", "onInformasiKalenderAkademik", "kalender_akademik");
        module(values, "eInfoKegiatanButton", "onInformasiKegiatan", "info_kegiatan");
        module(values, "eFeederButton", "onSinkronisasiFeeder", "feeder");
        module(values, "eSisterButton", "onSinkronisasiSister", "sister");
        utility(values, "menuService", "onOpenMenu", "Menu responsif", "nuiMenuToggle");
        utility(values, "customerService", "onCustomerService", "Layanan pengguna", "nuiShellCustomerDialog");
        utility(values, "back_to_top", "Clients.scrollIntoView", "Kembali ke atas", "nuiBackToTop");
        ITEMS = Collections.unmodifiableMap(values);
    }

    private NewUiIndexToolbarParityService() { }

    public static List<Item> items() {
        return Collections.unmodifiableList(new ArrayList<Item>(ITEMS.values()));
    }

    public static Item find(String legacyId) {
        return legacyId == null ? null : (Item) ITEMS.get(legacyId);
    }

    public static boolean isComplete() {
        if (ITEMS.size() != 29) return false;
        List<String> modules = NewUiModuleFunctionService.moduleKeys();
        for (java.util.Iterator iterator = ITEMS.values().iterator(); iterator.hasNext();) {
            Item item = (Item) iterator.next();
            if (item.moduleKey != null && !modules.contains(item.moduleKey)) return false;
        }
        return true;
    }

    private static void module(Map values, String id, String handler, String moduleKey) {
        values.put(id, new Item(id, handler, moduleKey, moduleKey, true));
    }

    private static void utility(Map values, String id, String handler, String label, String marker) {
        values.put(id, new Item(id, handler, null, marker, false));
    }

    public static final class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String legacyId, legacyHandler, moduleKey, nativeMarker;
        private final boolean module;
        Item(String legacyId, String legacyHandler, String moduleKey, String nativeMarker, boolean module) {
            this.legacyId = legacyId; this.legacyHandler = legacyHandler;
            this.moduleKey = moduleKey; this.nativeMarker = nativeMarker; this.module = module;
        }
        public String getLegacyId() { return legacyId; }
        public String getLegacyHandler() { return legacyHandler; }
        public String getModuleKey() { return moduleKey; }
        public String getNativeMarker() { return nativeMarker; }
        public boolean isModule() { return module; }
    }
}
