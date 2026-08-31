package ais.common.newui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pembentuk tree Menu yang deterministik dan aman terhadap duplicate, orphan,
 * serta cycle. Relasi existing: root=0 adalah akar dan child.root=parent.child.
 * Kelas ini sengaja tidak bergantung Hibernate agar dapat diuji sebagai unit murni.
 */
public final class NewUiMenuTreeBuilder {

    public static final int MAX_DEPTH = 64;

    /**
     * Pembawa data/helper lokal milik {@link NewUiMenuTreeBuilder} untuk result. Tipe ini mengelompokkan nilai
     * antara agar perhitungan atau rendering tidak memakai array/map tanpa kontrak yang jelas.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link NewUiMenuTreeBuilder}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code List roots}, {@code int
     * duplicateCount}, {@code int orphanCount}, {@code int cycleCount}; operasi lokal: {@code getRoots()}, {@code
     * getDuplicateCount()}, {@code getOrphanCount()}, {@code getCycleCount}(). Aturan bisnis bersama tetap berada
     * pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiMenuTreeBuilder
     */
    public static final class Result {
        private final List<NewUiMenuNode> roots;
        private final int duplicateCount;
        private final int orphanCount;
        private final int cycleCount;

        Result(List<NewUiMenuNode> roots, int duplicateCount, int orphanCount, int cycleCount) {
            this.roots = roots;
            this.duplicateCount = duplicateCount;
            this.orphanCount = orphanCount;
            this.cycleCount = cycleCount;
        }

        public List<NewUiMenuNode> getRoots() { return roots; }
        public int getDuplicateCount() { return duplicateCount; }
        public int getOrphanCount() { return orphanCount; }
        public int getCycleCount() { return cycleCount; }
    }

    private NewUiMenuTreeBuilder() {
    }

    public static Result buildAndPrune(List<NewUiMenuNode> source) {
        List<NewUiMenuNode> flat = source == null
                ? new ArrayList<NewUiMenuNode>() : new ArrayList<NewUiMenuNode>(source);
        Collections.sort(flat, NODE_ORDER);

        Map<Long, NewUiMenuNode> unique = new HashMap<Long, NewUiMenuNode>();
        List<NewUiMenuNode> accepted = new ArrayList<NewUiMenuNode>();
        int duplicates = 0;
        for (int i = 0; i < flat.size(); i++) {
            NewUiMenuNode node = flat.get(i);
            if (node == null || node.getMenuId() == null || unique.containsKey(node.getMenuId())) {
                duplicates++;
                continue;
            }
            node.setChildren(new ArrayList<NewUiMenuNode>());
            unique.put(node.getMenuId(), node);
            accepted.add(node);
        }

        Map<Long, List<NewUiMenuNode>> byRoot = new HashMap<Long, List<NewUiMenuNode>>();
        for (int i = 0; i < accepted.size(); i++) {
            NewUiMenuNode node = accepted.get(i);
            Long root = value(node.getRoot());
            List<NewUiMenuNode> bucket = byRoot.get(root);
            if (bucket == null) {
                bucket = new ArrayList<NewUiMenuNode>();
                byRoot.put(root, bucket);
            }
            bucket.add(node);
        }

        List<NewUiMenuNode> roots = byRoot.get(Long.valueOf(0L));
        if (roots == null) roots = new ArrayList<NewUiMenuNode>();
        Set<Long> attached = new HashSet<Long>();
        int[] cycles = new int[] { 0 };
        for (int i = 0; i < roots.size(); i++) {
            attach(roots.get(i), byRoot, attached, new HashSet<Long>(), 0, cycles);
        }

        int orphans = accepted.size() - attached.size();
        return new Result(prune(roots), duplicates, orphans, cycles[0]);
    }

    private static void attach(NewUiMenuNode node, Map<Long, List<NewUiMenuNode>> byRoot,
            Set<Long> attached, Set<Long> ancestry, int depth, int[] cycles) {
        if (node == null || node.getMenuId() == null) return;
        if (depth > MAX_DEPTH || ancestry.contains(node.getMenuId())) {
            cycles[0]++;
            return;
        }
        attached.add(node.getMenuId());
        Set<Long> nextAncestry = new HashSet<Long>(ancestry);
        nextAncestry.add(node.getMenuId());
        List<NewUiMenuNode> candidates = byRoot.get(value(node.getChild()));
        List<NewUiMenuNode> children = new ArrayList<NewUiMenuNode>();
        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                NewUiMenuNode child = candidates.get(i);
                if (child == null || child.getMenuId() == null) continue;
                if (nextAncestry.contains(child.getMenuId()) || depth + 1 > MAX_DEPTH) {
                    cycles[0]++;
                    continue;
                }
                // Data dengan dua parent ambigu ditolak agar node tidak muncul ganda.
                if (attached.contains(child.getMenuId())) continue;
                attach(child, byRoot, attached, nextAncestry, depth + 1, cycles);
                children.add(child);
            }
        }
        node.setChildren(children);
    }

    /** Post-order pruning: self READ atau minimal satu descendant READ. */
    private static List<NewUiMenuNode> prune(List<NewUiMenuNode> nodes) {
        List<NewUiMenuNode> kept = new ArrayList<NewUiMenuNode>();
        if (nodes == null) return kept;
        for (int i = 0; i < nodes.size(); i++) {
            NewUiMenuNode node = nodes.get(i);
            List<NewUiMenuNode> children = prune(node.getChildren());
            node.setChildren(children);
            int readableDescendants = 0;
            for (int j = 0; j < children.size(); j++) {
                NewUiMenuNode child = children.get(j);
                readableDescendants += (child.isReadable() ? 1 : 0) + child.getReadableDescendantCount();
            }
            node.setReadableDescendantCount(readableDescendants);
            if (node.isReadable() || readableDescendants > 0) {
                node.setGroup(!node.isClickable() && !children.isEmpty());
                kept.add(node);
            }
        }
        return kept;
    }

    public static final Comparator<NewUiMenuNode> NODE_ORDER = new Comparator<NewUiMenuNode>() {
        public int compare(NewUiMenuNode a, NewUiMenuNode b) {
            int c = compareInteger(a.getNomorUrut(), b.getNomorUrut());
            if (c != 0) return c;
            c = compareLong(a.getRoot(), b.getRoot());
            if (c != 0) return c;
            c = compareLong(a.getChild(), b.getChild());
            if (c != 0) return c;
            return compareLong(a.getMenuId(), b.getMenuId());
        }
    };

    private static Long value(Long v) { return v == null ? Long.valueOf(0L) : v; }

    private static int compareInteger(Integer a, Integer b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.intValue() < b.intValue() ? -1 : (a.intValue() > b.intValue() ? 1 : 0);
    }

    private static int compareLong(Long a, Long b) {
        long x = a == null ? 0L : a.longValue();
        long y = b == null ? 0L : b.longValue();
        return x < y ? -1 : (x > y ? 1 : 0);
    }
}
