package ais.common.newui.menu.test;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteRegistry;
import ais.common.newui.menu.NewUiHybridMenuSnapshot;
import ais.common.newui.menu.NewUiHybridMenuTreeBuilder;
import ais.common.newui.menu.NewUiModuleShortcut;
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
        NewUiHybridMenuSnapshot snapshot = new NewUiHybridMenuSnapshot("u", "r", "PT",
                NewUiHybridMenuTreeBuilder.build(source));

        Tbmrole role = new Tbmrole();
        role.setDashboard(Boolean.TRUE); role.setElearning(Boolean.TRUE);
        role.setPustaka(Boolean.FALSE);
        List<NewUiModuleShortcut> cards = NewUiModuleShortcutService.build(role, snapshot);
        check(cards.size() == 2, "hanya flag aktif yang boleh menjadi kartu");
        check("e-Learning".equals(cards.get(0).getLabel()),
                "e-Learning harus dapat memakai branch Perkuliahan sebagai fallback");
        check("Akademik".equals(cards.get(1).getLabel()), "urutan shortcut legacy harus dipertahankan");
        for (int i = 0; i < cards.size(); i++) {
            check(cards.get(i).getTarget() != null && cards.get(i).getTarget().isVisible(),
                    "target harus berasal dari snapshot visible");
            check(!"Pustaka".equals(cards.get(i).getLabel()), "flag Pustaka mati tidak boleh bocor");
        }
        System.out.println("NewUiModuleShortcutServiceSelfTest OK");
    }
}
