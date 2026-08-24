package ais.action.master.jurnal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;

import ais.common.JurnalAksesKatalog;
import ais.database.model.Menu;
import ais.database.model.Tbmrole;

/**
 * Menjaga sumber izin jurnal JSON dan relasi menu fisik role tetap konsisten.
 * Hanya rentang menu jurnal yang dikelola; menu modul lain tidak pernah disentuh.
 */
public final class JurnalRoleMenuSynchronizer {
    private JurnalRoleMenuSynchronizer() {}

    public static void synchronize(Session session, Tbmrole role, String accessJson) {
        if (session == null || role == null) throw new IllegalArgumentException("Session dan role wajib tersedia.");
        Set<Menu> available = new HashSet<Menu>();
        boolean any = false;
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            if (!JurnalAksesKatalog.bolehMenu(accessJson, entry.kunci)) continue;
            any = true;
            available.add(required(session, JurnalMenuReconciler.CHILD_ID_BASE + entry.child, entry.label));
        }
        if (any) available.add(required(session, JurnalMenuReconciler.PARENT_ID, "Jurnal"));
        synchronize(role, available);
    }

    /** Visible for deterministic tests without a database. */
    public static void synchronize(Tbmrole role, Set<Menu> selectedJournalMenus) {
        if (role == null) throw new IllegalArgumentException("Role wajib tersedia.");
        Set<Menu> merged = new HashSet<Menu>();
        if (role.getMenus() != null) {
            for (Menu menu : role.getMenus()) if (!managed(menu)) merged.add(menu);
        }
        if (selectedJournalMenus != null) merged.addAll(selectedJournalMenus);
        role.setMenus(merged);
    }

    private static Menu required(Session session, long id, String label) {
        Menu menu = (Menu) session.get(Menu.class, Long.valueOf(id));
        if (menu == null || !Boolean.TRUE.equals(menu.getAktif()))
            throw new IllegalStateException("Menu jurnal belum tersedia/aktif: " + label + " (" + id + ").");
        return menu;
    }

    private static boolean managed(Menu menu) {
        if (menu == null || menu.getId() == null) return false;
        long id = menu.getId().longValue();
        return id >= JurnalMenuReconciler.PARENT_ID && id <= JurnalMenuReconciler.PARENT_ID + 28L;
    }
}
