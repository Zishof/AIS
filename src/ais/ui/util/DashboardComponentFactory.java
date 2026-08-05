package ais.ui.util;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import ais.ui.util.MyPortalchildren;
import ais.ui.util.MyPortallayout;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;

import ais.common.Common;

/** Factory kecil agar pembuatan panel, portal, grid, dan close session dashboard seragam. */
public final class DashboardComponentFactory {
    private DashboardComponentFactory() {}

    public static String responsiveWidth() {
        return Common.isMobile() ? "100%" : "50%";
    }

    public static MyPortalchildren addPortalChildren(MyPortallayout layout, String width) {
        MyPortalchildren pc = new MyPortalchildren();
        pc.setWidth(width == null ? responsiveWidth() : width);
        pc.setStyle("padding:6px;");
        pc.setParent(layout);
        return pc;
    }

    public static Panel createPanel(Component parent, String title, String description) {
        Panel panel = new Panel();
        panel.setTitle(title);
        panel.setBorder("normal");
        panel.setCollapsible(true);
        panel.setStyle("margin-bottom:12px;");
        panel.setParent(parent);
        Panelchildren children = new Panelchildren();
        children.setStyle("padding:10px;background:#f8fafc;");
        children.setParent(panel);
        children.appendChild(new org.zkoss.zul.Html(DashboardModernHtmlUtil.description(description)));
        return panel;
    }

    public static Panelchildren body(Panel panel) {
        if (panel != null && panel.getChildren() != null && panel.getChildren().size() > 0
                && panel.getChildren().get(0) instanceof Panelchildren) {
            return (Panelchildren) panel.getChildren().get(0);
        }
        return null;
    }

    public static Grid createPagingGrid(Component parent, int pageSize) {
        Grid grid = new Grid();
        grid.setMold("paging");
        grid.setPageSize(pageSize <= 0 ? 10 : pageSize);
        grid.setSclass("dgrid fgrid");
        grid.setWidth("100%");
        grid.setParent(parent);
        return grid;
    }

    public static void closeOpenedSession(Session session) {
        DashboardModernHtmlUtil.closeOpenedSession(session);
    }
}
