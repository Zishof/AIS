package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Snapshot immutable-by-convention yang dikirim ke renderer; tidak menyimpan entity Hibernate. */
public class NewUiHybridMenuSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String roleId;
    private final String scope;
    private final List<NewUiHybridMenuNode> sidebarBranches;
    private final List<NewUiHybridMenuNode> searchableNodes = new ArrayList<NewUiHybridMenuNode>();
    private final List<NewUiHybridMenuNode> authorizedLeaves = new ArrayList<NewUiHybridMenuNode>();
    private final Map<Long, NewUiHybridMenuNode> visibleById = new HashMap<Long, NewUiHybridMenuNode>();
    private final Map<Long, NewUiHybridMenuNode> assignedById = new HashMap<Long, NewUiHybridMenuNode>();
    private final NewUiHybridMenuDiagnostics diagnostics;

    public NewUiHybridMenuSnapshot(String userId, String roleId, String scope,
            NewUiHybridMenuTreeBuilder.Result result) {
        this.userId = userId; this.roleId = roleId; this.scope = scope;
        this.sidebarBranches = result == null ? new ArrayList<NewUiHybridMenuNode>() : result.getSidebarBranches();
        this.diagnostics = result == null ? new NewUiHybridMenuDiagnostics() : result.getDiagnostics();
        if (result != null) {
            List<NewUiHybridMenuNode> all = result.getAllAssigned();
            for (int i = 0; i < all.size(); i++) assignedById.put(all.get(i).getMenuId(), all.get(i));
        }
        for (int i = 0; i < sidebarBranches.size(); i++) indexVisible(sidebarBranches.get(i));
    }

    public static NewUiHybridMenuSnapshot empty() {
        return new NewUiHybridMenuSnapshot(null, null, null, null);
    }

    private void indexVisible(NewUiHybridMenuNode node) {
        if (node == null || node.getMenuId() == null || !node.isVisible()) return;
        visibleById.put(node.getMenuId(), node);
        if (!node.isVirtual()) searchableNodes.add(node);
        List<NewUiHybridMenuNode> direct = node.getDirectLeaves();
        for (int i = 0; i < direct.size(); i++) {
            NewUiHybridMenuNode leaf = direct.get(i);
            visibleById.put(leaf.getMenuId(), leaf); searchableNodes.add(leaf); authorizedLeaves.add(leaf);
        }
        List<NewUiHybridMenuNode> branches = node.getBranchChildren();
        for (int i = 0; i < branches.size(); i++) indexVisible(branches.get(i));
    }

    public String getUserId() { return userId; }
    public String getRoleId() { return roleId; }
    public String getScope() { return scope; }
    public List<NewUiHybridMenuNode> getSidebarBranches() { return sidebarBranches; }
    public List<NewUiHybridMenuNode> getSearchableNodes() { return searchableNodes; }
    public List<NewUiHybridMenuNode> getAuthorizedLeaves() { return authorizedLeaves; }
    public NewUiHybridMenuDiagnostics getDiagnostics() { return diagnostics; }
    public NewUiHybridMenuNode findVisible(Long id) { return id == null ? null : visibleById.get(id); }
    public NewUiHybridMenuNode findAssigned(Long id) { return id == null ? null : assignedById.get(id); }

    public List<NewUiHybridMenuNode> breadcrumb(Long id) {
        List<NewUiHybridMenuNode> path = new ArrayList<NewUiHybridMenuNode>();
        if (findPath(sidebarBranches, id, path)) return path;
        path.clear(); return path;
    }

    private boolean findPath(List<NewUiHybridMenuNode> branches, Long id, List<NewUiHybridMenuNode> path) {
        if (branches == null || id == null) return false;
        for (int i = 0; i < branches.size(); i++) {
            NewUiHybridMenuNode branch = branches.get(i); path.add(branch);
            if (id.equals(branch.getMenuId())) return true;
            List<NewUiHybridMenuNode> direct = branch.getDirectLeaves();
            for (int j = 0; j < direct.size(); j++) {
                if (id.equals(direct.get(j).getMenuId())) { path.add(direct.get(j)); return true; }
            }
            if (findPath(branch.getBranchChildren(), id, path)) return true;
            path.remove(path.size() - 1);
        }
        return false;
    }
}
