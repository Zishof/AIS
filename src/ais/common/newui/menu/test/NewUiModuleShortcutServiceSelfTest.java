package ais.common.newui.menu.test;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteRegistry;
import ais.common.newui.menu.NewUiHybridMenuSnapshot;
import ais.common.newui.menu.NewUiHybridMenuTreeBuilder;
import ais.common.newui.menu.NewUiModuleShortcut;
import ais.common.newui.menu.NewUiModuleDashboardService;
import ais.common.newui.menu.NewUiModuleShortcutService;
import ais.common.newui.menu.NewUiPermission;
import ais.database.model.Tbmrole;

/** Self-test tanpa JUnit untuk pemetaan shortcut legacy ke snapshot RBAC. */
public final class NewUiModuleShortcutServiceSelfTest {

    private NewUiModuleShortcutServiceSelfTest() { }

    private static NewUiHybridMenuNode node(long id, long root, long child,
            String label, boolean read) {
        NewUiHybridMenuNode value = new NewUiHybridMenuNode();
        value.setMenuId(Long.valueOf(id)); value.setRoot(Long.valueOf(root));
        value.setChild(Long.valueOf(child)); value.setNomorUrut(Integer.valueOf((int)id));
        value.setLabel(label); value.setAssigned(true); value.setActiveInScope(true);
        value.setPermission(new NewUiPermission(read, false, false, false, false, false));
        value.setRouteStatus(read ? NewUiHybridMenuRouteRegistry.NEW_UI : NewUiHybridMenuRouteRegistry.NOT_MAPPED);
        return value;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        List<NewUiHybridMenuNode> source = new ArrayList<NewUiHybridMenuNode>();
        source.add(node(1, 0, 10, "Sistem Informasi Akademik", false));
        source.add(node(2, 10, 20, "Perkuliahan", false));
        source.add(node(3, 20, 30, "Kelas Perkuliahan", true));
        source.add(node(4, 0, 40, "Perpustakaan", false));
        source.add(node(5, 40, 50, "Katalog Perpustakaan", true));
        source.add(node(6, 0, 60, "Kalender Akademik", false));
        source.add(node(7, 60, 70, "Agenda Akademik", true));
        source.add(node(8, 0, 80, "Info Kegiatan", true));
        source.add(node(9, 0, 90, "Tata Kelola Surat", false));
        source.add(node(10, 90, 100, "Surat Masuk", true));
        NewUiHybridMenuSnapshot snapshot = new NewUiHybridMenuSnapshot("u", "r", "PT",
                NewUiHybridMenuTreeBuilder.build(source));

        Tbmrole role = new Tbmrole();
        role.setDashboard(Boolean.TRUE); role.setElearning(Boolean.TRUE);
        role.setPustaka(Boolean.FALSE); role.setKalenderAkademik(Boolean.TRUE);
        role.setInfoKegiatan(Boolean.TRUE); role.setAdministrasi(Boolean.TRUE);
        List<NewUiModuleShortcut> cards = NewUiModuleShortcutService.build(role, snapshot);
        check(cards.size() == 5, "seluruh flag aktif yang punya target READ harus menjadi kartu");
        check("e-Learning".equals(cards.get(0).getLabel()),
                "e-Learning harus dapat memakai branch Perkuliahan sebagai fallback");
        check("Akademik".equals(cards.get(1).getLabel()), "urutan shortcut legacy harus dipertahankan");
        check("Administrasi".equals(cards.get(2).getLabel()), "Administrasi belum terpetakan");
        check("administrasi".equals(cards.get(2).getKey()), "key Administrasi harus sama dengan registry dashboard");
        check("Kalender Akademik".equals(cards.get(3).getLabel()), "Kalender Akademik belum terpetakan");
        check("Info Kegiatan".equals(cards.get(4).getLabel()), "Info Kegiatan belum terpetakan");
        for (int i = 0; i < cards.size(); i++) {
            check(cards.get(i).getTarget() != null && cards.get(i).getTarget().isVisible(),
                    "target harus berasal dari snapshot visible");
            check(NewUiModuleDashboardService.supports(cards.get(i).getKey()),
                    "setiap shortcut harus mempunyai dashboard native: " + cards.get(i).getKey());
            check(!"Pustaka".equals(cards.get(i).getLabel()), "flag Pustaka mati tidak boleh bocor");
        }
        String[] dashboardKeys = new String[] {"emedic", "elearning", "prestasi", "pustaka",
                "workflow", "repository", "antar_jemput", "spmi", "toko", "koperasi",
                "akademik", "administrasi", "pengadaan", "pembayaran", "keuangan",
                "akuntansi", "kepegawaian", "gaji", "kinerja", "presensi",
                "kalender_akademik", "info_kegiatan", "feeder", "sister"};
        for (int i = 0; i < dashboardKeys.length; i++) {
            check(NewUiModuleDashboardService.supports(dashboardKeys[i]),
                    "dashboard toolbar index.zul belum native: " + dashboardKeys[i]);
        }
        System.out.println("NewUiModuleShortcutServiceSelfTest OK");
    }
}
