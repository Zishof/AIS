package ais.common.newui;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ais.common.newui.menu.NewUiHybridMenuAccessService;
import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuSnapshot;

/** Facade kompatibilitas V1; implementasi produksi berada pada package hybrid. */
public final class NewUiMenuAccessService {

    private NewUiMenuAccessService() { }

    public static NewUiHybridMenuSnapshot getSnapshot(HttpServletRequest request) {
        return NewUiHybridMenuAccessService.getSnapshot(request);
    }

    public static List<NewUiMenuNode> getAccessibleTree(HttpServletRequest request) {
        List<NewUiMenuNode> result = new ArrayList<NewUiMenuNode>();
        result.addAll(getSnapshot(request).getSidebarBranches());
        return result;
    }

    public static NewUiMenuNode findById(HttpServletRequest request, Long menuId) {
        return getSnapshot(request).findVisible(menuId);
    }

    public static NewUiMenuNode findByRoute(HttpServletRequest request, String module, String page) {
        return NewUiHybridMenuAccessService.findByRoute(getSnapshot(request), module, page);
    }

    public static List<NewUiMenuNode> breadcrumb(HttpServletRequest request, Long menuId) {
        List<NewUiMenuNode> result = new ArrayList<NewUiMenuNode>();
        result.addAll(getSnapshot(request).breadcrumb(menuId));
        return result;
    }

    public static NewUiHybridMenuNode findAssigned(HttpServletRequest request, Long menuId) {
        return getSnapshot(request).findAssigned(menuId);
    }
}
