package ais.common;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.database.model.Menu;
import ais.database.model.Tbmrole;

/**
 * Menjaga agar capability menu pada {@code jurnal_akses_json} dan relasi fisik
 * {@code job_has_menu} tidak berbeda. Pemanggil wajib menjalankannya di transaksi
 * penyimpanan role yang sama sehingga JSON dan menu berhasil atau gagal bersama.
 */
public final class JurnalRoleMenuSynchronizer {
    // Satu-satunya sinkronizer role-menu jurnal; jangan menggandakan logika ini di package fitur.
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
            Menu menu = ensureCanonical(session, id.longValue(), role.getRoleId());
            merged.add(menu);
        }
        role.setMenus(merged);
    }

    public static boolean isManaged(long id) {
        return id >= PARENT_ID && id <= LAST_CHILD_ID;
    }

    /**
     * Instalasi lama tidak selalu sudah menjalankan migrasi menu jurnal. Saat
     * administrator memang memilih akses jurnal, buat/pulihkan baris canonical
     * dalam transaksi penyimpanan role yang sama. Collision tetap fail-closed.
     */
    private static Menu ensureCanonical(Session session, long id, String actorId) {
        long expectedRoot = id == PARENT_ID ? 46L : 4605L;
        long expectedChild = id == PARENT_ID ? 4605L : id - CHILD_ID_BASE;
        JurnalAksesKatalog.Entri entry = id == PARENT_ID ? null : entry(expectedChild);
        if (id != PARENT_ID && entry == null)
            throw new IllegalStateException("ID menu jurnal tidak dikenal: " + id);

        Menu menu = (Menu) session.get(Menu.class, Long.valueOf(id));
        if (menu == null) {
            Menu collision = (Menu) session.createCriteria(Menu.class)
                    .add(Restrictions.eq("root", Long.valueOf(expectedRoot)))
                    .add(Restrictions.eq("child", Long.valueOf(expectedChild)))
                    .add(Restrictions.ne("id", Long.valueOf(id)))
                    .setMaxResults(1).uniqueResult();
            if (collision != null)
                throw new IllegalStateException("Menu jurnal collision pada root/child "
                        + expectedRoot + "/" + expectedChild + ": " + collision.getId());
            menu = new Menu();
            menu.setId(Long.valueOf(id));
            session.save(menu);
        } else {
            validateCanonical(menu, id, expectedRoot, expectedChild);
        }

        menu.setRoot(Long.valueOf(expectedRoot));
        menu.setChild(Long.valueOf(expectedChild));
        menu.setLabel(id == PARENT_ID ? "Jurnal" : entry.label);
        menu.setUrl(id == PARENT_ID ? "/jurnal/admin" : "/jurnal/admin/" + entry.kunci);
        menu.setNomorUrut(Integer.valueOf(id == PARENT_ID ? 0 : order(entry)));
        menu.setAktif(Boolean.TRUE);
        menu.setTampilDiPt(Boolean.TRUE);
        menu.setTampilDiSekolah(Boolean.TRUE);
        menu.setBukaHalamanBaru(Boolean.FALSE);
        menu.setOleh("JurnalRoleMenuSynchronizer");
        menu.setOlehId(actorId);
        menu.setTanggal_dirubah(new Date());
        session.saveOrUpdate(menu);
        return menu;
    }

    private static void validateCanonical(Menu menu, long id, long expectedRoot, long expectedChild) {
        if (menu.getRoot() == null || menu.getChild() == null)
            throw new IllegalStateException("Menu jurnal canonical tidak lengkap: " + id);
        if (menu.getRoot().longValue() != expectedRoot || menu.getChild().longValue() != expectedChild)
            throw new IllegalStateException("Menu jurnal canonical mengalami collision: " + id);
    }

    private static JurnalAksesKatalog.Entri entry(long child) {
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR)
            if (entry.child == child) return entry;
        return null;
    }

    private static int order(JurnalAksesKatalog.Entri selected) {
        int order = 1;
        for (JurnalAksesKatalog.Entri entry : JurnalAksesKatalog.DAFTAR) {
            if (entry == selected) return order;
            order++;
        }
        return order;
    }
}
