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

/** Test harness tanpa JUnit untuk branch/leaf/RBAC/orphan/cycle Hybrid V2. */
public final class NewUiHybridMenuTreeBuilderSelfTest {

    private NewUiHybridMenuTreeBuilderSelfTest() { }

    private static NewUiHybridMenuNode node(long id, long root, long child, int order,
            boolean read, boolean route) {
        NewUiHybridMenuNode value = new NewUiHybridMenuNode();
        value.setMenuId(Long.valueOf(id)); value.setRoot(Long.valueOf(root)); value.setChild(Long.valueOf(child));
        value.setNomorUrut(Integer.valueOf(order)); value.setLabel("M" + id); value.setAssigned(true);
        value.setActiveInScope(true); value.setPermission(new NewUiPermission(read, false, false, false, false, false));
        value.setRouteStatus(route ? NewUiHybridMenuRouteRegistry.LEGACY_EMBED : NewUiHybridMenuRouteRegistry.NOT_MAPPED);
        value.setResolvedUrl(route ? "/baru?menu=" + id : null);
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

        NewUiHybridMenuTreeBuilder.Result built = NewUiHybridMenuTreeBuilder.build(source);
        NewUiHybridMenuSnapshot snapshot = new NewUiHybridMenuSnapshot("u", "r", "pt", built);
        NewUiHybridMenuNode branch = snapshot.findVisible(Long.valueOf(1L));
        check(branch != null && branch.isBranch(), "parent with visible leaf must be branch");
        check(branch.isStructuralOnly(), "READ=0 parent must be structural-only");
        check(branch.getDirectLeaves().size() == 1, "forbidden child leaked or readable leaf missing");
        check(snapshot.findVisible(Long.valueOf(3L)) == null, "READ=0 leaf leaked");
        check(snapshot.getSidebarBranches().size() == 2, "root/orphan leaves were not grouped virtually");
        check(NewUiHybridMenuCatalogService.forGroup(snapshot, Long.valueOf(1L), false, "order").size() == 1,
                "direct catalog incorrect");
        check(NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(
                NewUiHybridMenuRouteGuard.evaluateMenu(snapshot, Long.valueOf(3L))), "direct READ=0 not forbidden");
        check(NewUiHybridMenuRouteRegistry.FORBIDDEN.equals(
                NewUiHybridMenuRouteGuard.evaluateMenu(snapshot, Long.valueOf(99999L))), "unassigned menu not forbidden");
        check(built.getDiagnostics().getOrphanCount() > 0, "orphan not diagnosed");

        List<NewUiHybridMenuNode> cycle = new ArrayList<NewUiHybridMenuNode>();
        cycle.add(node(10, 0, 1, 1, false, false)); cycle.add(node(11, 1, 2, 1, false, false));
        cycle.add(node(12, 2, 1, 1, true, true));
        built = NewUiHybridMenuTreeBuilder.build(cycle);
        check(built.getDiagnostics().getCycleCount() > 0, "cycle not diagnosed");
        System.out.println("PASS Hybrid Menu V2 tree/catalog/guard self-test");
    }
}
