package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter native untuk Action existing yang hanya mengumpulkan beberapa ZUL
 * dalam tab. New UI tidak memuat ZUL tersebut; setiap tab diselesaikan kembali
 * ke menu New UI yang sudah lolos RBAC.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiActionDashboardNavigationService {
    private static final Map DEFINITIONS;

    static {
        Map values = new LinkedHashMap();
        add(values, "ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin",
                e("Kegiatan Dosen", "kegiatan dosen", "kegiatan kedosenan"),
                e("Organisasi Dosen", "organisasi dosen"),
                e("Prestasi Dosen", "prestasi dosen"),
                e("Karya / Penghargaan Dosen", "penghargaan dosen", "karya dosen"));
        add(values, "ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin",
                e("Kegiatan Siswa", "kegiatan siswa", "kegiatan kesiswaan"),
                e("Organisasi Siswa", "organisasi siswa"),
                e("Prestasi Siswa", "prestasi siswa"),
                e("Karya / Penghargaan Siswa", "penghargaan siswa", "karya siswa"));
        add(values, "ais.action.master.koperasi.DashboardKoperasiAction",
                e("Simpan Pinjam", "dashboard simpan pinjam", "simpan pinjam"),
                e("Laporan Simpan Pinjam", "laporan simpan pinjam"),
                e("Pembagian SHU", "pembagian shu"),
                e("Laporan Keuangan", "laporan keuangan koperasi"),
                e("Anggaran Kas", "anggaran kas koperasi"),
                e("Modal Penyertaan", "modal penyertaan koperasi"),
                e("ARO Deposito", "deposito aro koperasi", "aro deposito"),
                e("Kantin / Toko", "dashboard kantin", "kantin koperasi"));
        DEFINITIONS = Collections.unmodifiableMap(values);
    }

    private NewUiActionDashboardNavigationService() { }

    public static boolean hasDefinition(String sourceClass) {
        return sourceClass != null && DEFINITIONS.containsKey(sourceClass);
    }

    public static List<Entry> build(String sourceClass, List<NewUiHybridMenuNode> authorizedNodes) {
        List specs = (List) DEFINITIONS.get(sourceClass);
        List<Entry> result = new ArrayList<Entry>();
        if (specs == null) return Collections.unmodifiableList(result);
        for (int i = 0; i < specs.size(); i++) {
            Spec spec = (Spec) specs.get(i);
            NewUiHybridMenuNode target = NewUiModuleShortcutService.findBest(
                    authorizedNodes, spec.aliases, null);
            result.add(new Entry(spec.label, target));
        }
        return Collections.unmodifiableList(result);
    }

    public static List<String> definitionLabels(String sourceClass) {
        List specs = (List) DEFINITIONS.get(sourceClass);
        List<String> result = new ArrayList<String>();
        if (specs != null) for (int i = 0; i < specs.size(); i++) {
            result.add(((Spec) specs.get(i)).label);
        }
        return Collections.unmodifiableList(result);
    }

    private static Spec e(String label, String... aliases) { return new Spec(label, aliases); }
    private static void add(Map values, String sourceClass, Spec... specs) {
        List list = new ArrayList();
        for (int i = 0; i < specs.length; i++) list.add(specs[i]);
        values.put(sourceClass, Collections.unmodifiableList(list));
    }

    private static final class Spec {
        final String label; final String[] aliases;
        Spec(String label, String[] aliases) { this.label = label; this.aliases = aliases; }
    }

    public static final class Entry implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label; private final NewUiHybridMenuNode target;
        Entry(String label, NewUiHybridMenuNode target) { this.label = label; this.target = target; }
        public String getLabel() { return label; }
        public NewUiHybridMenuNode getTarget() { return target; }
        public boolean isAvailable() { return target != null && target.getMenuId() != null; }
    }
}
