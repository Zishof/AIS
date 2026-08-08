package ais.common.newui.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Hierarchy fungsi Mahasiswa yang sebelumnya berupa tab/flyout ZKOSS. */
public final class NewUiMahasiswaMenu {
    public static final Long MENU_MAHASISWA = Long.valueOf(6L);
    public static final Long MASTER = Long.valueOf(-6001L);
    public static final Long PENDUKUNG = Long.valueOf(-6100L);
    public static final Long PRESTASI = Long.valueOf(-6200L);
    public static final Long DATA = Long.valueOf(-6300L);
    public static final Long STATISTIK = Long.valueOf(-6401L);

    private static final Map<Long, String[]> ITEMS = new LinkedHashMap<Long, String[]>();
    static {
        item(MASTER, "Master Mahasiswa", "Mahasiswa", "master");
        item(PENDUKUNG, "Pendukung", "Pendukung", "group");
        item(Long.valueOf(-6101L), "Kelas", "Pendukung", "onManajemenKelas");
        item(Long.valueOf(-6102L), "Perkuliahan", "Pendukung", "onPerkuliahanMahasiswa");
        item(Long.valueOf(-6103L), "Asrama", "Pendukung", "onManajemenAsrama");
        item(Long.valueOf(-6104L), "Kelompok", "Pendukung", "onManajemenKelompok");
        item(Long.valueOf(-6105L), "Status", "Pendukung", "onManajemenKelompokStatus");
        item(Long.valueOf(-6106L), "Program", "Pendukung", "onManajemenProgram");
        item(Long.valueOf(-6107L), "Status Keluar", "Pendukung", "onManajemenKelompokStatusKeluar");
        item(Long.valueOf(-6108L), "Dosen PA", "Pendukung", "onManajemenDosenPA");
        item(Long.valueOf(-6109L), "Form Tambahan", "Pendukung", "onFormTambahan");
        item(Long.valueOf(-6110L), "Kartu Mahasiswa", "Pendukung", "onKartuMahasiswa");
        item(Long.valueOf(-6111L), "Operator Seluler", "Pendukung", "onOperatorSeluler");
        item(Long.valueOf(-6112L), "Alat Transport", "Pendukung", "onAlatTransport");
        item(Long.valueOf(-6113L), "Jenis Tinggal", "Pendukung", "onJenisTinggal");
        item(Long.valueOf(-6114L), "Pekerjaan Orang Tua", "Pendukung", "onPekerjaanOrtu");
        item(Long.valueOf(-6115L), "Penghasilan Orang Tua", "Pendukung", "onPenghasilanOrtu");
        item(Long.valueOf(-6116L), "Pendidikan Orang Tua", "Pendukung", "onPendidikanOrtu");
        item(PRESTASI, "Prestasi", "Prestasi", "group");
        item(Long.valueOf(-6201L), "KHS", "Prestasi", "onTampilKHS");
        item(Long.valueOf(-6202L), "Transkrip Akademik", "Prestasi", "onTampilTranskripAkademik");
        item(Long.valueOf(-6203L), "Prestasi Belajar", "Prestasi", "onTampilPrestasi");
        item(Long.valueOf(-6204L), "Kegiatan Kemahasiswaan", "Prestasi", "onKegiatanKemahasiswaan");
        item(DATA, "Data", "Data", "group");
        item(Long.valueOf(-6301L), "Rekap Mahasiswa", "Data", "onRekapJumlahMahasiswa");
        item(Long.valueOf(-6302L), "Formulir Mahasiswa", "Data", "onKegiatanMahasiswa");
        item(Long.valueOf(-6303L), "Surat Mahasiswa", "Data", "onSuratMahasiswa");
        item(Long.valueOf(-6304L), "Album Mahasiswa", "Data", "onAlbumMahasiswa");
        item(Long.valueOf(-6305L), "Ekspor Data", "Data", "onDataMahasiswa");
        item(STATISTIK, "Statistik", "Statistik", "onStatistik");
    }

    private NewUiMahasiswaMenu() { }

    private static void item(Long id, String label, String group, String action) {
        ITEMS.put(id, new String[]{label, group, action});
    }

    public static String[] selection(Long id) { return id == null ? null : ITEMS.get(id); }

    /** Virtual child tetap memakai assignment/privilege menu Mahasiswa asli. */
    public static Long authorizationMenuId(Long id) {
        return id != null && ITEMS.containsKey(id) ? MENU_MAHASISWA : id;
    }

    public static void expand(List<NewUiHybridMenuNode> roots, List<NewUiHybridMenuNode> assigned) {
        if (roots == null) return;
        NewUiHybridMenuNode mahasiswa = detachLeafAndPromote(roots);
        if (mahasiswa == null || mahasiswa.getPermission() == null || !mahasiswa.getPermission().isCanRead()) return;
        mahasiswa.setKind(NewUiHybridMenuNode.BRANCH);
        mahasiswa.setClickable(true); mahasiswa.setStructuralOnly(false);
        mahasiswa.setDirectLeavesInSidebar(true);

        List<NewUiHybridMenuNode> direct = new ArrayList<NewUiHybridMenuNode>();
        direct.add(leaf(MASTER, mahasiswa, 1));
        direct.add(leaf(STATISTIK, mahasiswa, 5));
        List<NewUiHybridMenuNode> groups = new ArrayList<NewUiHybridMenuNode>();
        groups.add(group(PENDUKUNG, mahasiswa, 2, -6101L, -6116L));
        groups.add(group(PRESTASI, mahasiswa, 3, -6201L, -6204L));
        groups.add(group(DATA, mahasiswa, 4, -6301L, -6305L));
        mahasiswa.setDirectLeaves(direct); mahasiswa.setBranchChildren(groups);
        rebuild(mahasiswa);
        if (assigned != null) collect(mahasiswa, assigned, false);
        rebuildAll(roots);
    }

    private static NewUiHybridMenuNode detachLeafAndPromote(List<NewUiHybridMenuNode> branches) {
        for (int i = 0; i < branches.size(); i++) {
            NewUiHybridMenuNode parent = branches.get(i);
            List<NewUiHybridMenuNode> leaves = parent.getDirectLeaves();
            for (int j = 0; j < leaves.size(); j++) {
                NewUiHybridMenuNode leaf = leaves.get(j);
                if (MENU_MAHASISWA.equals(leaf.getMenuId())
                        && "mahasiswa".equalsIgnoreCase(leaf.getLabel() == null ? "" : leaf.getLabel().trim())) {
                    leaves.remove(j); parent.getBranchChildren().add(leaf);
                    Collections.sort(parent.getBranchChildren(), NewUiHybridMenuTreeBuilder.NODE_ORDER);
                    return leaf;
                }
            }
            NewUiHybridMenuNode found = detachLeafAndPromote(parent.getBranchChildren());
            if (found != null) return found;
        }
        return null;
    }

    private static NewUiHybridMenuNode group(Long id, NewUiHybridMenuNode parent, int order, long first, long last) {
        NewUiHybridMenuNode node = base(id, parent, order);
        node.setKind(NewUiHybridMenuNode.BRANCH); node.setStructuralOnly(true);
        node.setClickable(false); node.setDirectLeavesInSidebar(false);
        List<NewUiHybridMenuNode> leaves = new ArrayList<NewUiHybridMenuNode>();
        long step = first <= last ? 1L : -1L; int position = 1;
        for (long value = first; ; value += step) {
            leaves.add(leaf(Long.valueOf(value), node, position++));
            if (value == last) break;
        }
        node.setDirectLeaves(leaves); rebuild(node); return node;
    }

    private static NewUiHybridMenuNode leaf(Long id, NewUiHybridMenuNode parent, int order) {
        NewUiHybridMenuNode node = base(id, parent, order);
        node.setKind(NewUiHybridMenuNode.LEAF); node.setClickable(true);
        node.setStructuralOnly(false); node.setLeafCount(1);
        List<NewUiHybridMenuNode> self = new ArrayList<NewUiHybridMenuNode>(); self.add(node);
        node.setDescendantLeaves(self); return node;
    }

    private static NewUiHybridMenuNode base(Long id, NewUiHybridMenuNode parent, int order) {
        String[] value = ITEMS.get(id); NewUiHybridMenuNode node = new NewUiHybridMenuNode();
        node.setMenuId(id); node.setLabel(value == null ? "Menu Mahasiswa" : value[0]);
        node.setIcon("fa-solid fa-angle-right"); node.setNomorUrut(Integer.valueOf(order));
        node.setRoot(parent.getChild()); node.setChild(id); node.setParentMenuId(parent.getMenuId());
        node.setParentPath(parent.getFullPath()); node.setFullPath(parent.getFullPath() + " / " + node.getLabel());
        node.setAssigned(true); node.setActiveInScope(true); node.setVisible(true); node.setVirtual(true);
        node.setPermission(parent.getPermission()); node.setRouteStatus(NewUiHybridMenuRouteRegistry.NEW_UI);
        node.setResolvedUrl("/WEB-INF/new/root/uiux/mahasiswa.jsp");
        node.setNewUiModule("root"); node.setNewUiPage("mahasiswa"); return node;
    }

    private static void rebuild(NewUiHybridMenuNode node) {
        List<NewUiHybridMenuNode> all = new ArrayList<NewUiHybridMenuNode>();
        all.addAll(node.getDirectLeaves());
        for (int i = 0; i < node.getBranchChildren().size(); i++) all.addAll(node.getBranchChildren().get(i).getDescendantLeaves());
        node.setDescendantLeaves(all); node.setLeafCount(all.size());
    }

    private static void rebuildAll(List<NewUiHybridMenuNode> branches) {
        for (int i = 0; i < branches.size(); i++) { rebuildAll(branches.get(i).getBranchChildren()); rebuild(branches.get(i)); }
    }

    private static void collect(NewUiHybridMenuNode node, List<NewUiHybridMenuNode> assigned, boolean includeNode) {
        if (includeNode) assigned.add(node);
        for (int i = 0; i < node.getDirectLeaves().size(); i++) assigned.add(node.getDirectLeaves().get(i));
        for (int i = 0; i < node.getBranchChildren().size(); i++) collect(node.getBranchChildren().get(i), assigned, true);
    }
}
