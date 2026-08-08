package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Pembentuk hierarchy hybrid post-order dari menu yang sudah assigned dan in-scope. */
public final class NewUiHybridMenuTreeBuilder {

    public static final int MAX_DEPTH = 50;
    public static final Long VIRTUAL_GROUP_ID = Long.valueOf(-1L);

    public static final class Result {
        private final List<NewUiHybridMenuNode> sidebarBranches;
        private final List<NewUiHybridMenuNode> allAssigned;
        private final NewUiHybridMenuDiagnostics diagnostics;

        Result(List<NewUiHybridMenuNode> branches, List<NewUiHybridMenuNode> all,
                NewUiHybridMenuDiagnostics diagnostics) {
            this.sidebarBranches = branches; this.allAssigned = all; this.diagnostics = diagnostics;
        }
        public List<NewUiHybridMenuNode> getSidebarBranches() { return sidebarBranches; }
        public List<NewUiHybridMenuNode> getAllAssigned() { return allAssigned; }
        public NewUiHybridMenuDiagnostics getDiagnostics() { return diagnostics; }
    }

    private static final class Classification {
        private final NewUiHybridMenuNode node;
        private final boolean visible;
        Classification(NewUiHybridMenuNode node, boolean visible) { this.node = node; this.visible = visible; }
    }

    private NewUiHybridMenuTreeBuilder() { }

    public static Result build(List<NewUiHybridMenuNode> source) {
        NewUiHybridMenuDiagnostics diagnostics = new NewUiHybridMenuDiagnostics();
        List<NewUiHybridMenuNode> sorted = source == null
                ? new ArrayList<NewUiHybridMenuNode>() : new ArrayList<NewUiHybridMenuNode>(source);
        Collections.sort(sorted, NODE_ORDER);

        Map<Long, NewUiHybridMenuNode> unique = new HashMap<Long, NewUiHybridMenuNode>();
        List<NewUiHybridMenuNode> accepted = new ArrayList<NewUiHybridMenuNode>();
        Map<Long, Integer> parentGroupCount = new HashMap<Long, Integer>();
        for (int i = 0; i < sorted.size(); i++) {
            NewUiHybridMenuNode node = sorted.get(i);
            if (node == null || node.getMenuId() == null || unique.containsKey(node.getMenuId())) {
                diagnostics.duplicate(node == null ? null : node.getMenuId());
                continue;
            }
            unique.put(node.getMenuId(), node);
            accepted.add(node);
            Long group = value(node.getChild());
            if (group.longValue() != 0L) {
                Integer count = parentGroupCount.get(group);
                parentGroupCount.put(group, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }
        for (Map.Entry<Long, Integer> entry : parentGroupCount.entrySet()) {
            if (entry.getValue().intValue() > 1) diagnostics.duplicateChildGroup(entry.getKey());
        }

        Map<Long, List<NewUiHybridMenuNode>> byRoot = new HashMap<Long, List<NewUiHybridMenuNode>>();
        for (int i = 0; i < accepted.size(); i++) {
            NewUiHybridMenuNode node = accepted.get(i);
            Long root = value(node.getRoot());
            List<NewUiHybridMenuNode> bucket = byRoot.get(root);
            if (bucket == null) { bucket = new ArrayList<NewUiHybridMenuNode>(); byRoot.put(root, bucket); }
            bucket.add(node);
        }

        List<NewUiHybridMenuNode> branches = new ArrayList<NewUiHybridMenuNode>();
        List<NewUiHybridMenuNode> virtualBranches = new ArrayList<NewUiHybridMenuNode>();
        List<NewUiHybridMenuNode> virtualLeaves = new ArrayList<NewUiHybridMenuNode>();
        Set<Long> processed = new HashSet<Long>();
        List<NewUiHybridMenuNode> roots = byRoot.get(Long.valueOf(0L));
        if (roots != null) {
            for (int i = 0; i < roots.size(); i++) {
                Classification result = classify(roots.get(i), byRoot, processed,
                        new HashSet<Long>(), "", null, 0, diagnostics);
                if (!result.visible) continue;
                if (result.node.isBranch()) branches.add(result.node); else virtualLeaves.add(result.node);
            }
        }

        // Assignment dengan parent tidak assigned/ambigu dipertahankan di grup virtual.
        for (int i = 0; i < accepted.size(); i++) {
            NewUiHybridMenuNode node = accepted.get(i);
            if (processed.contains(node.getMenuId())) continue;
            diagnostics.orphan(node.getMenuId(), node.getRoot());
            Classification result = classify(node, byRoot, processed, new HashSet<Long>(),
                    "Menu Lainnya", VIRTUAL_GROUP_ID, 0, diagnostics);
            if (!result.visible) continue;
            if (result.node.isBranch()) virtualBranches.add(result.node); else virtualLeaves.add(result.node);
        }

        if (!virtualBranches.isEmpty() || !virtualLeaves.isEmpty()) {
            NewUiHybridMenuNode virtual = virtualGroup(virtualBranches, virtualLeaves);
            branches.add(virtual);
        }
        Collections.sort(branches, NODE_ORDER);
        return new Result(branches, accepted, diagnostics);
    }

    private static Classification classify(NewUiHybridMenuNode node,
            Map<Long, List<NewUiHybridMenuNode>> byRoot, Set<Long> processed, Set<Long> visiting,
            String parentPath, Long parentId, int depth, NewUiHybridMenuDiagnostics diagnostics) {
        if (node == null || node.getMenuId() == null) return new Classification(node, false);
        if (depth > MAX_DEPTH) { diagnostics.depthLimit(node.getMenuId()); return new Classification(node, false); }
        if (visiting.contains(node.getMenuId())) { diagnostics.cycle(node.getMenuId()); return new Classification(node, false); }
        if (processed.contains(node.getMenuId())) return new Classification(node, false);

        visiting.add(node.getMenuId());
        node.setParentMenuId(parentId);
        node.setParentPath(parentPath);
        node.setFullPath(join(parentPath, node.getLabel()));

        List<NewUiHybridMenuNode> visibleBranches = new ArrayList<NewUiHybridMenuNode>();
        List<NewUiHybridMenuNode> visibleLeaves = new ArrayList<NewUiHybridMenuNode>();
        List<NewUiHybridMenuNode> candidates = byRoot.get(value(node.getChild()));
        if (candidates != null && value(node.getChild()).longValue() != 0L) {
            for (int i = 0; i < candidates.size(); i++) {
                NewUiHybridMenuNode child = candidates.get(i);
                Classification result = classify(child, byRoot, processed, visiting,
                        node.getFullPath(), node.getMenuId(), depth + 1, diagnostics);
                if (!result.visible) continue;
                if (result.node.isBranch()) visibleBranches.add(result.node); else visibleLeaves.add(result.node);
            }
        }
        visiting.remove(node.getMenuId());
        processed.add(node.getMenuId());

        boolean hasVisibleChild = !visibleBranches.isEmpty() || !visibleLeaves.isEmpty();
        boolean selfReadable = node.getPermission() != null && node.getPermission().isCanRead();
        boolean selfClickable = selfReadable && node.hasValidRoute();
        if (hasVisibleChild) {
            node.setKind(NewUiHybridMenuNode.BRANCH);
            node.setVisible(node.isAssigned() && node.isActiveInScope());
            node.setClickable(selfClickable);
            node.setStructuralOnly(!selfClickable);
            node.setBranchChildren(visibleBranches);
            node.setDirectLeaves(visibleLeaves);
            List<NewUiHybridMenuNode> descendants = new ArrayList<NewUiHybridMenuNode>();
            descendants.addAll(visibleLeaves);
            for (int i = 0; i < visibleBranches.size(); i++) descendants.addAll(visibleBranches.get(i).getDescendantLeaves());
            node.setDescendantLeaves(descendants);
            node.setLeafCount(descendants.size());
        } else {
            node.setKind(NewUiHybridMenuNode.LEAF);
            node.setVisible(node.isAssigned() && node.isActiveInScope() && selfClickable);
            node.setClickable(node.isVisible());
            node.setStructuralOnly(false);
            node.setBranchChildren(new ArrayList<NewUiHybridMenuNode>());
            node.setDirectLeaves(new ArrayList<NewUiHybridMenuNode>());
            List<NewUiHybridMenuNode> self = new ArrayList<NewUiHybridMenuNode>();
            if (node.isVisible()) self.add(node);
            node.setDescendantLeaves(self);
            node.setLeafCount(node.isVisible() ? 1 : 0);
        }
        return new Classification(node, node.isVisible());
    }

    private static NewUiHybridMenuNode virtualGroup(List<NewUiHybridMenuNode> branches,
            List<NewUiHybridMenuNode> leaves) {
        NewUiHybridMenuNode node = new NewUiHybridMenuNode();
        node.setMenuId(VIRTUAL_GROUP_ID); node.setLabel("Menu Lainnya");
        node.setNomorUrut(Integer.valueOf(Integer.MAX_VALUE)); node.setRoot(Long.valueOf(0L));
        node.setChild(Long.valueOf(-1L)); node.setAssigned(true); node.setActiveInScope(true);
        node.setVirtual(true); node.setKind(NewUiHybridMenuNode.BRANCH); node.setVisible(true);
        node.setClickable(false); node.setStructuralOnly(true); node.setFullPath("Menu Lainnya");
        for (int i = 0; i < branches.size(); i++) rebase(branches.get(i), node.getFullPath(), VIRTUAL_GROUP_ID);
        for (int i = 0; i < leaves.size(); i++) rebase(leaves.get(i), node.getFullPath(), VIRTUAL_GROUP_ID);
        node.setBranchChildren(branches); node.setDirectLeaves(leaves);
        List<NewUiHybridMenuNode> descendants = new ArrayList<NewUiHybridMenuNode>();
        descendants.addAll(leaves);
        for (int i = 0; i < branches.size(); i++) descendants.addAll(branches.get(i).getDescendantLeaves());
        node.setDescendantLeaves(descendants); node.setLeafCount(descendants.size());
        return node;
    }

    private static void rebase(NewUiHybridMenuNode node, String parentPath, Long parentId) {
        if (node == null) return;
        node.setParentMenuId(parentId); node.setParentPath(parentPath);
        node.setFullPath(join(parentPath, node.getLabel()));
        List<NewUiHybridMenuNode> leaves = node.getDirectLeaves();
        for (int i = 0; i < leaves.size(); i++) rebase(leaves.get(i), node.getFullPath(), node.getMenuId());
        List<NewUiHybridMenuNode> branches = node.getBranchChildren();
        for (int i = 0; i < branches.size(); i++) rebase(branches.get(i), node.getFullPath(), node.getMenuId());
    }

    public static final Comparator<NewUiHybridMenuNode> NODE_ORDER = new Comparator<NewUiHybridMenuNode>() {
        public int compare(NewUiHybridMenuNode a, NewUiHybridMenuNode b) {
            int c = compareInteger(a.getNomorUrut(), b.getNomorUrut());
            if (c != 0) return c;
            c = compareLong(a.getRoot(), b.getRoot()); if (c != 0) return c;
            c = compareLong(a.getChild(), b.getChild()); if (c != 0) return c;
            return compareLong(a.getMenuId(), b.getMenuId());
        }
    };

    private static String join(String parent, String label) {
        String p = parent == null ? "" : parent.trim(); String l = label == null ? "Menu" : label.trim();
        return p.length() == 0 ? l : p + " / " + l;
    }
    private static Long value(Long value) { return value == null ? Long.valueOf(0L) : value; }
    private static int compareInteger(Integer a, Integer b) {
        int x = a == null ? Integer.MAX_VALUE : a.intValue(); int y = b == null ? Integer.MAX_VALUE : b.intValue();
        return x < y ? -1 : (x > y ? 1 : 0);
    }
    private static int compareLong(Long a, Long b) {
        long x = a == null ? 0L : a.longValue(); long y = b == null ? 0L : b.longValue();
        return x < y ? -1 : (x > y ? 1 : 0);
    }
}
