package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.NewUiMenuNode;

/** DTO aman untuk satu branch/leaf pada snapshot hybrid. */
public class NewUiHybridMenuNode extends NewUiMenuNode {

    private static final long serialVersionUID = 1L;

    public static final String BRANCH = "BRANCH";
    public static final String LEAF = "LEAF";

    private String kind = LEAF;
    private boolean assigned;
    private boolean activeInScope;
    private boolean visible;
    private boolean clickable;
    private boolean structuralOnly;
    private boolean virtual;
    private Long parentMenuId;
    private String existingUrl;
    private String resolvedUrl;
    private String routeStatus = NewUiHybridMenuRouteRegistry.NOT_MAPPED;
    private String fullPath = "";
    private String parentPath = "";
    private String aliases = "";
    private String keywords = "";
    private int leafCount;
    private List<NewUiHybridMenuNode> branchChildren = new ArrayList<NewUiHybridMenuNode>();
    private List<NewUiHybridMenuNode> directLeaves = new ArrayList<NewUiHybridMenuNode>();
    private List<NewUiHybridMenuNode> descendantLeaves = new ArrayList<NewUiHybridMenuNode>();

    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind == null ? LEAF : kind; }
    public boolean isBranch() { return BRANCH.equals(kind); }
    public boolean isLeaf() { return LEAF.equals(kind); }
    public boolean isAssigned() { return assigned; }
    public void setAssigned(boolean assigned) { this.assigned = assigned; }
    public boolean isActiveInScope() { return activeInScope; }
    public void setActiveInScope(boolean value) { activeInScope = value; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isClickable() { return clickable; }
    public void setClickable(boolean clickable) { this.clickable = clickable; }
    public boolean isStructuralOnly() { return structuralOnly; }
    public void setStructuralOnly(boolean value) { structuralOnly = value; }
    public boolean isVirtual() { return virtual; }
    public void setVirtual(boolean virtual) { this.virtual = virtual; }
    public Long getParentMenuId() { return parentMenuId; }
    public void setParentMenuId(Long parentMenuId) { this.parentMenuId = parentMenuId; }
    public String getExistingUrl() { return existingUrl; }
    public void setExistingUrl(String existingUrl) { this.existingUrl = existingUrl; }
    public String getResolvedUrl() { return resolvedUrl; }
    public void setResolvedUrl(String resolvedUrl) { this.resolvedUrl = resolvedUrl; }
    public String getRouteStatus() { return routeStatus; }
    public void setRouteStatus(String routeStatus) { this.routeStatus = routeStatus == null ? NewUiHybridMenuRouteRegistry.NOT_MAPPED : routeStatus; }
    public String getFullPath() { return fullPath; }
    public void setFullPath(String fullPath) { this.fullPath = fullPath == null ? "" : fullPath; }
    public String getParentPath() { return parentPath; }
    public void setParentPath(String parentPath) { this.parentPath = parentPath == null ? "" : parentPath; }
    public String getAliases() { return aliases; }
    public void setAliases(String aliases) { this.aliases = aliases == null ? "" : aliases; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords == null ? "" : keywords; }
    public int getLeafCount() { return leafCount; }
    public void setLeafCount(int leafCount) { this.leafCount = leafCount; setReadableDescendantCount(leafCount); }
    public List<NewUiHybridMenuNode> getBranchChildren() { return branchChildren; }
    public void setBranchChildren(List<NewUiHybridMenuNode> value) {
        branchChildren = value == null ? new ArrayList<NewUiHybridMenuNode>() : value;
        List<NewUiMenuNode> compatible = new ArrayList<NewUiMenuNode>();
        compatible.addAll(branchChildren);
        super.setChildren(compatible);
    }
    public List<NewUiHybridMenuNode> getDirectLeaves() { return directLeaves; }
    public void setDirectLeaves(List<NewUiHybridMenuNode> value) { directLeaves = value == null ? new ArrayList<NewUiHybridMenuNode>() : value; }
    public List<NewUiHybridMenuNode> getDescendantLeaves() { return descendantLeaves; }
    public void setDescendantLeaves(List<NewUiHybridMenuNode> value) { descendantLeaves = value == null ? new ArrayList<NewUiHybridMenuNode>() : value; }

    public boolean hasValidRoute() {
        return NewUiHybridMenuRouteRegistry.NEW_UI.equals(routeStatus)
                || NewUiHybridMenuRouteRegistry.LEGACY_EMBED.equals(routeStatus)
                || NewUiHybridMenuRouteRegistry.LEGACY_REDIRECT.equals(routeStatus);
    }

    public String getSearchText() {
        return safe(getLabel()) + " " + fullPath + " " + aliases + " " + keywords;
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
