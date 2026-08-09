package ais.common.newui.menu.test;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.menu.NewUiHybridMenuCatalogService;
import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteGuard;
import ais.common.newui.menu.NewUiHybridMenuRouteRegistry;
import ais.common.newui.menu.NewUiHybridMenuSnapshot;
import ais.common.newui.menu.NewUiHybridMenuTreeBuilder;
import ais.common.newui.menu.NewUiPermission;
import ais.common.newui.menu.NewUiMahasiswaMenu;

/** Test harness tanpa JUnit untuk branch/leaf/RBAC/orphan/cycle Hybrid V2. */
public final class NewUiHybridMenuTreeBuilderSelfTest {

    private NewUiHybridMenuTreeBuilderSelfTest() { }

    private static NewUiHybridMenuNode node(long id, long root, long child, int order,
            boolean read, boolean route) {
        NewUiHybridMenuNode value = new NewUiHybridMenuNode();
        value.setMenuId(Long.valueOf(id)); value.setRoot(Long.valueOf(root)); value.setChild(Long.valueOf(child));
        value.setNomorUrut(Integer.valueOf(order)); value.setLabel("M" + id); value.setAssigned(true);
        value.setActiveInScope(true); value.setPermission(new NewUiPermission(read, false, false, false, false, false));
        value.setRouteStatus(route ? NewUiHybridMenuRouteRegistry.NEW_UI : NewUiHybridMenuRouteRegistry.NOT_MAPPED);
        value.setResolvedUrl(route ? "/new?menuId=" + id : null);
        return value;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        List<NewUiHybridMenuNode> source = new ArrayList<NewUiHybridMenuNode>();
        source.add(node(1, 0, 10, 1, false, false));
        source.add(node(2, 10, 20, 1, true, true));
        source.add(node(3, 10, 30, 2, false, true));
        source.add(node(4, 0, 40, 2, true, true));
        source.add(node(5, 999, 50, 3, true, true));
        source.add(node(6, 10, 60, 3, false, false));
        source.add(node(7, 60, 70, 1, true, true));

        NewUiHybridMenuTreeBuilder.Result built = NewUiHybridMenuTreeBuilder.build(source);
        NewUiHybridMenuSnapshot snapshot = new NewUiHybridMenuSnapshot("u", "r", "pt", built);
        NewUiHybridMenuNode branch = snapshot.findVisible(Long.valueOf(1L));
        check(branch != null && branch.isBranch(), "parent with visible leaf must be branch");
        check(branch.isStructuralOnly(), "READ=0 parent must be structural-only");
        check(branch.getDirectLeaves().size() == 1, "forbidden child leaked or readable leaf missing");
        check(branch.getBranchChildren().size() == 1, "visible sub-group missing");
        check(branch.getDescendantLeaves().size() == 2, "sub-child leaf missing");
        check(snapshot.findVisible(Long.valueOf(3L)) == null, "READ=0 leaf leaked");
        check(snapshot.getSidebarBranches().size() == 2, "root/orphan leaves were not grouped virtually");
        check(NewUiHybridMenuCatalogService.forGroup(snapshot, Long.valueOf(1L), false, "order").size() == 1,
                "direct catalog incorrect");
        List<NewUiHybridMenuNode> hierarchy = NewUiHybridMenuCatalogService.allDescendants(
                snapshot, Long.valueOf(1L), NewUiHybridMenuCatalogService.SORT_ORDER);
        check(hierarchy.size() == 3, "catalog must contain every child and sub-child");
        check(hierarchy.get(0).getMenuId().longValue() == 2L
                && hierarchy.get(1).getMenuId().longValue() == 6L
                && hierarchy.get(2).getMenuId().longValue() == 7L,
                "catalog hierarchy order incorrect");
        check(NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(
                NewUiHybridMenuRouteGuard.evaluateMenu(snapshot, Long.valueOf(3L))), "direct READ=0 not forbidden");
        check(NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(
                NewUiHybridMenuRouteGuard.evaluateMenu(snapshot, Long.valueOf(99999L))), "unassigned menu not forbidden");
        check(built.getDiagnostics().getOrphanCount() > 0, "orphan not diagnosed");
        check(!built.getDiagnostics().hasCriticalWarnings(),
                "recoverable orphan/ambiguous hierarchy must not be logged as critical");

        List<NewUiHybridMenuNode> mahasiswaSource = new ArrayList<NewUiHybridMenuNode>();
        mahasiswaSource.add(node(100, 0, 10, 1, true, true));
        NewUiHybridMenuNode mahasiswa = node(6, 10, 0, 1, true, true);
        mahasiswa.setLabel("Mahasiswa"); mahasiswa.setNewUiModule("root"); mahasiswa.setNewUiPage("mahasiswa");
        mahasiswaSource.add(mahasiswa);
        NewUiHybridMenuSnapshot mahasiswaSnapshot = new NewUiHybridMenuSnapshot("u", "r", "pt",
                NewUiHybridMenuTreeBuilder.build(mahasiswaSource));
        NewUiHybridMenuNode mahasiswaBranch = mahasiswaSnapshot.findVisible(Long.valueOf(6L));
        check(mahasiswaBranch != null && mahasiswaBranch.isBranch(), "Mahasiswa must become a branch");
        check(mahasiswaBranch.getDirectLeaves().size() == 2, "Master/Statistik direct submenu missing");
        check(mahasiswaBranch.getBranchChildren().size() == 3, "Pendukung/Prestasi/Data submenu missing");
        check(!mahasiswaBranch.getBranchChildren().get(0).isDirectLeavesInSidebar(),
                "detail tab Pendukung must stay in content catalog, not sidebar");
        check(mahasiswaBranch.getLeafCount() == 27, "Mahasiswa nested leaf count incorrect: " + mahasiswaBranch.getLeafCount());
        check(mahasiswaSnapshot.findAssigned(Long.valueOf(-6101L)) != null,
                "virtual Mahasiswa leaf must remain guarded and assigned");
        check(NewUiMahasiswaMenu.MENU_MAHASISWA.equals(
                NewUiMahasiswaMenu.authorizationMenuId(Long.valueOf(-6001L))),
                "virtual Mahasiswa leaf must inherit real menu authorization");
        check(Long.valueOf(99L).equals(NewUiMahasiswaMenu.authorizationMenuId(Long.valueOf(99L))),
                "non-Mahasiswa menu authorization must remain unchanged");

        List<NewUiHybridMenuNode> cycle = new ArrayList<NewUiHybridMenuNode>();
        cycle.add(node(10, 0, 1, 1, false, false)); cycle.add(node(11, 1, 2, 1, false, false));
        cycle.add(node(12, 2, 1, 1, true, true));
        built = NewUiHybridMenuTreeBuilder.build(cycle);
        check(built.getDiagnostics().getCycleCount() > 0, "cycle not diagnosed");
        check(built.getDiagnostics().hasCriticalWarnings(), "cycle must remain a critical warning");
        System.out.println("PASS Hybrid Menu V2 tree/catalog/guard self-test");
    }
}
