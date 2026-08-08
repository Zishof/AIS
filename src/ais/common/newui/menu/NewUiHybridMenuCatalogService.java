package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Pemilih katalog hierarchy untuk branch yang telah diotorisasi. */
public final class NewUiHybridMenuCatalogService {

    public static final String SORT_ORDER = "order";
    public static final String SORT_AZ = "az";

    private NewUiHybridMenuCatalogService() { }

    public static List<NewUiHybridMenuNode> forGroup(NewUiHybridMenuSnapshot snapshot, Long groupId,
            boolean includeDescendants, String sort) {
        List<NewUiHybridMenuNode> result = new ArrayList<NewUiHybridMenuNode>();
        if (snapshot == null || groupId == null) return result;
        NewUiHybridMenuNode group = snapshot.findVisible(groupId);
        if (group == null || !group.isBranch()) return result;
        result.addAll(includeDescendants ? group.getDescendantLeaves() : group.getDirectLeaves());
        if (SORT_AZ.equalsIgnoreCase(sort)) Collections.sort(result, LABEL_ORDER);
        else Collections.sort(result, NewUiHybridMenuTreeBuilder.NODE_ORDER);
        return result;
    }

    /**
     * Mengembalikan seluruh child dan sub-child visible dalam urutan hierarchy.
     * Branch ikut dikembalikan supaya pengguna dapat membuka subkelompok dari
     * katalog, sedangkan leaf tetap membuka halaman yang telah melewati guard.
     */
    public static List<NewUiHybridMenuNode> allDescendants(NewUiHybridMenuSnapshot snapshot,
            Long groupId, String sort) {
        List<NewUiHybridMenuNode> result = new ArrayList<NewUiHybridMenuNode>();
        if (snapshot == null || groupId == null) return result;
        NewUiHybridMenuNode group = snapshot.findVisible(groupId);
        if (group == null || !group.isBranch()) return result;
        collectChildren(group, result);
        if (SORT_AZ.equalsIgnoreCase(sort)) Collections.sort(result, LABEL_ORDER);
        return result;
    }

    private static void collectChildren(NewUiHybridMenuNode parent, List<NewUiHybridMenuNode> result) {
        List<NewUiHybridMenuNode> children = new ArrayList<NewUiHybridMenuNode>();
        children.addAll(parent.getBranchChildren());
        children.addAll(parent.getDirectLeaves());
        Collections.sort(children, NewUiHybridMenuTreeBuilder.NODE_ORDER);
        for (int i = 0; i < children.size(); i++) {
            NewUiHybridMenuNode child = children.get(i);
            result.add(child);
            if (child.isBranch()) collectChildren(child, result);
        }
    }

    private static final Comparator<NewUiHybridMenuNode> LABEL_ORDER = new Comparator<NewUiHybridMenuNode>() {
        public int compare(NewUiHybridMenuNode a, NewUiHybridMenuNode b) {
            String x = a == null || a.getLabel() == null ? "" : a.getLabel();
            String y = b == null || b.getLabel() == null ? "" : b.getLabel();
            int c = x.compareToIgnoreCase(y);
            return c != 0 ? c : NewUiHybridMenuTreeBuilder.NODE_ORDER.compare(a, b);
        }
    };
}
