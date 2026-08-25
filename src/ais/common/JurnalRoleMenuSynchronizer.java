package ais.common;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Session;

import ais.database.model.Menu;
import ais.database.model.Tbmrole;

/**
 * Menjaga agar capability menu pada {@code jurnal_akses_json} dan relasi fisik
 * {@code job_has_menu} tidak berbeda. Pemanggil wajib menjalankannya di transaksi
 * penyimpanan role yang sama sehingga JSON dan menu berhasil atau gagal bersama.
 */
public final class JurnalRoleMenuSynchronizer {
    public static final long PARENT_ID = 2000460500L;
    public static final long CHILD_ID_BASE = 2000000000L;
    public static final long LAST_CHILD_ID = 2000460528L;

    private JurnalRoleMenuSynchronizer() {}

    public static Set<Long> desiredMenuIds(String rawAccessJson) {
        JurnalAksesKatalog.validasiUntukSimpan(rawAccessJson);
        LinkedHashSet<Long> ids = new LinkedHashSet<Long>();
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            if (JurnalAksesKatalog.bolehMenu(rawAccessJson, entry.kunci))
                ids.add(Long.valueOf(CHILD_ID_BASE + entry.child));
        }
        if (!ids.isEmpty()) {
            LinkedHashSet<Long> withParent = new LinkedHashSet<Long>();
            withParent.add(Long.valueOf(PARENT_ID));
            withParent.addAll(ids);
            return withParent;
        }
        return ids;
    }

    public static void synchronize(Session session, Tbmrole role, String rawAccessJson) {
        if (session == null || role == null || role.getRoleId() == null)
            throw new IllegalArgumentException("Session dan role wajib tersedia untuk sinkronisasi menu jurnal.");
        Set<Long> desired = desiredMenuIds(rawAccessJson);
        LinkedHashSet<Menu> merged = new LinkedHashSet<Menu>();
        if (role.getMenus() != null) {
            for (Menu menu : role.getMenus()) {
                if (menu != null && menu.getId() != null && !isManaged(menu.getId().longValue()))
                    merged.add(menu);
            }
        }
        for (Long id : desired) {
            Menu menu = (Menu) session.get(Menu.class, id);
            validateCanonical(menu, id.longValue());
            merged.add(menu);
        }
        role.setMenus(merged);
    }

    public static boolean isManaged(long id) {
        return id >= PARENT_ID && id <= LAST_CHILD_ID;
    }

    private static void validateCanonical(Menu menu, long id) {
        if (menu == null || menu.getRoot() == null || menu.getChild() == null || !Boolean.TRUE.equals(menu.getAktif()))
            throw new IllegalStateException("Menu jurnal canonical belum tersedia/aktif: " + id);
        long expectedRoot = id == PARENT_ID ? 46L : 4605L;
        long expectedChild = id == PARENT_ID ? 4605L : id - CHILD_ID_BASE;
        if (menu.getRoot().longValue() != expectedRoot || menu.getChild().longValue() != expectedChild)
            throw new IllegalStateException("Menu jurnal canonical mengalami collision: " + id);
    }
}
