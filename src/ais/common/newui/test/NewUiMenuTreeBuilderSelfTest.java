package ais.common.newui.test;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.NewUiMenuNode;
import ais.common.newui.NewUiMenuTreeBuilder;
import ais.common.newui.NewUiPermission;
import ais.common.newui.NewUiRouteRegistry;
import ais.database.model.Menu;

/** Test harness tanpa JUnit agar dapat dijalankan pada build legacy. */
public final class NewUiMenuTreeBuilderSelfTest {

    private NewUiMenuTreeBuilderSelfTest() {
    }

    private static NewUiMenuNode node(long id, long root, long child, int order, boolean read) {
        NewUiMenuNode value = new NewUiMenuNode();
        value.setMenuId(Long.valueOf(id));
        value.setRoot(Long.valueOf(root));
        value.setChild(Long.valueOf(child));
        value.setNomorUrut(Integer.valueOf(order));
        value.setLabel("M" + id);
        value.setPermission(new NewUiPermission(read, false, false, false, false, false));
        return value;
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    public static void main(String[] args) {
        List<NewUiMenuNode> source = new ArrayList<NewUiMenuNode>();
        source.add(node(1, 0, 10, 2, false));
        source.add(node(2, 10, 20, 1, true));
        source.add(node(3, 0, 30, 1, false));
        NewUiMenuTreeBuilder.Result result = NewUiMenuTreeBuilder.buildAndPrune(source);
        assertTrue(result.getRoots().size() == 1, "unreadable root was not pruned");
        assertTrue(result.getRoots().get(0).getMenuId().longValue() == 1L, "display-only ancestor missing");
        assertTrue(result.getRoots().get(0).getChildren().size() == 1, "readable descendant missing");

        source.add(node(2, 0, 99, 0, true));
        source.add(node(4, 999, 1000, 0, true));
        result = NewUiMenuTreeBuilder.buildAndPrune(source);
        assertTrue(result.getDuplicateCount() == 1, "duplicate not detected");
        assertTrue(result.getOrphanCount() == 1, "orphan not detected");

        List<NewUiMenuNode> cycle = new ArrayList<NewUiMenuNode>();
        cycle.add(node(10, 0, 1, 0, false));
        cycle.add(node(11, 1, 2, 0, false));
        cycle.add(node(12, 2, 1, 0, true));
        result = NewUiMenuTreeBuilder.buildAndPrune(cycle);
        assertTrue(result.getCycleCount() > 0, "cycle not detected");

        Menu mapped = new Menu(Long.valueOf(2));
        mapped.setUrl("/pages/maintenance/job/list.zul");
        String[] route = NewUiRouteRegistry.routeForMenu(mapped);
        assertTrue(route != null && "root/maintenance".equals(route[0]) && "tbmrole".equals(route[1]),
                "explicit route mapping failed");

        Menu mahasiswa = new Menu(Long.valueOf(6));
        mahasiswa.setUrl("/pages/master/mahasiswa.zul");
        route = NewUiRouteRegistry.routeForMenu(mahasiswa);
        assertTrue(route != null && "root".equals(route[0]) && "mahasiswa".equals(route[1]),
                "Mahasiswa must resolve to WEB-INF/new/root/uiux/mahasiswa.jsp");
        assertTrue(!NewUiRouteRegistry.isSafeLegacyUrl("https://evil.example/x"), "external fallback accepted");
        assertTrue(!NewUiRouteRegistry.isSafeLegacyUrl("/pages/../secret.zul"), "traversal fallback accepted");
        System.out.println("PASS New UI RBAC tree/route self-test");
    }
}
