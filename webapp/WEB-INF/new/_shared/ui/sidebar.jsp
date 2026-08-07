<%-- Sidebar RBAC New UI: dirender dari NewUiMenuAccessService (menu boleh-akses role aktif).
     Dipakai index.jsp hanya bila flag konfigurasi nui_rbac_sidebar aktif.
     Menerima request attribute: nui_current_module (String). --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.JspWriter" %>
<%@ page import="ais.common.newui.NewUiMenuAccessService" %>
<%@ page import="ais.common.newui.NewUiMenuNode" %>
<%!
    private String h(Object v) {
        if (v == null) return "";
        return String.valueOf(v).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String linkTarget(NewUiMenuNode node, String newUrl) throws IOException {
        if (node.isMappedToNewUi()) {
            return newUrl + "?frame=1&module=" + URLEncoder.encode(node.getNewUiModule(), "UTF-8")
                    + "&page=" + URLEncoder.encode(node.getNewUiPage() == null ? "index" : node.getNewUiPage(), "UTF-8");
        }
        return node.getLegacyUrl();
    }

    private String shellUrl(NewUiMenuNode node, String newUrl) throws IOException {
        if (node.isMappedToNewUi()) {
            return newUrl + "?module=" + URLEncoder.encode(node.getNewUiModule(), "UTF-8")
                    + "&page=" + URLEncoder.encode(node.getNewUiPage() == null ? "index" : node.getNewUiPage(), "UTF-8");
        }
        return node.getLegacyUrl();
    }

    // Tambahkan tautan untuk node yang readable + punya target, lalu telusuri anaknya (flatten ke link).
    private void appendLinks(JspWriter out, NewUiMenuNode node, String newUrl, String currentModule)
            throws IOException {
        if (node == null) return;
        boolean hasTarget = (node.getLegacyUrl() != null && node.getLegacyUrl().length() > 0) || node.isMappedToNewUi();
        if (node.isReadable() && hasTarget) {
            boolean active = node.getNewUiModule() != null && node.getNewUiModule().equals(currentModule);
            out.print("<a class=\"" + (active ? "active" : "") + "\" target=\"nuiMainFrame\" data-shell-url=\""
                    + h(shellUrl(node, newUrl)) + "\" href=\"" + h(linkTarget(node, newUrl)) + "\">"
                    + "<span>◇</span><span>" + h(node.getLabel()) + "</span></a>\n");
        }
        List<NewUiMenuNode> children = node.getChildren();
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                appendLinks(out, children.get(i), newUrl, currentModule);
            }
        }
    }
%>
<%
    String ctx = request.getContextPath();
    String newUrl = ctx + "/new";
    String currentModule = (String) request.getAttribute("nui_current_module");
    List<NewUiMenuNode> tree = NewUiMenuAccessService.getAccessibleTree(request);
    if (tree == null || tree.isEmpty()) {
%>
        <div class="nui-nav-group"><div class="nui-nav-label">Menu</div>
            <a class="disabled" href="#" onclick="return false;"><span>&#9671;</span><span>Tidak ada menu untuk peran aktif</span></a>
        </div>
<%
    } else {
        for (int i = 0; i < tree.size(); i++) {
            NewUiMenuNode root = tree.get(i);
            boolean rootHasTarget = (root.getLegacyUrl() != null && root.getLegacyUrl().length() > 0) || root.isMappedToNewUi();
            if (root.hasChildren()) {
                out.print("<div class=\"nui-nav-group\"><div class=\"nui-nav-label\">" + h(root.getLabel()) + "</div>\n");
                if (root.isReadable() && rootHasTarget) {
                    appendLinks(out, root, newUrl, currentModule); // termasuk root sendiri
                } else {
                    List<NewUiMenuNode> kids = root.getChildren();
                    for (int j = 0; j < kids.size(); j++) {
                        appendLinks(out, kids.get(j), newUrl, currentModule);
                    }
                }
                out.print("</div>\n");
            } else if (root.isReadable() && rootHasTarget) {
                out.print("<div class=\"nui-nav-group\">\n");
                appendLinks(out, root, newUrl, currentModule);
                out.print("</div>\n");
            }
        }
    }
%>
