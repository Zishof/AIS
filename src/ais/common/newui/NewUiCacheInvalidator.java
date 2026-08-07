package ais.common.newui;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpSession;

import ais.common.CommonPrivilages;

/**
 * Invalidasi cache hak akses/menu untuk New UI dan cache existing terkait.
 *
 * <p>Strategi kompatibel single-JVM: sebuah <b>global menu-permission version</b>
 * (counter) yang di-bump setiap kali role/privilege berubah. Setiap session
 * menyimpan versi terakhir yang dipakai; bila berbeda, tree menu New UI dibangun
 * ulang pada request berikutnya. Ini menghindari keharusan restart Tomcat setelah
 * perubahan privilege.</p>
 *
 * <p><b>Keterbatasan:</b> counter berada di JVM. Pada deployment multi-node,
 * invalidasi TIDAK otomatis merambat antar node — jangan diklaim sebaliknya.
 * Bila multi-node dibutuhkan, gunakan version-stamp berbasis database (belum
 * diimplementasikan di sini).</p>
 *
 * <p>Kompatibel Java 1.6 (AtomicLong tersedia sejak 1.5).</p>
 */
public final class NewUiCacheInvalidator {

    /** Nama attribute session untuk tree menu New UI hasil cache. */
    public static final String SESSION_TREE = "nui_menu_tree";
    /** Nama attribute session untuk versi menu-permission yang terakhir dipakai session. */
    public static final String SESSION_TREE_VERSION = "nui_menu_tree_version";
    /** Nama attribute session existing untuk menu legacy. */
    public static final String SESSION_CURRENT_MENUS = "current_menus";
    /** Nama attribute session existing untuk menu aktif legacy. */
    public static final String SESSION_CURRENT_MENU = "current_menu";

    private static final AtomicLong GLOBAL_VERSION = new AtomicLong(1L);

    private NewUiCacheInvalidator() {
    }

    /** Versi global menu-permission saat ini. */
    public static long getGlobalVersion() {
        return GLOBAL_VERSION.get();
    }

    /** Naikkan versi global → seluruh session akan reload tree pada request berikutnya. */
    public static long invalidateAllMenuVersions() {
        return GLOBAL_VERSION.incrementAndGet();
    }

    /**
     * Bersihkan cache privilege existing ({@link CommonPrivilages#rolePrivilagesUtama})
     * untuk sebuah role, lalu bump versi global.
     */
    public static void invalidateRole(String roleId) {
        if (roleId != null && roleId.length() > 0) {
            try {
                Map<String, ?> map = CommonPrivilages.rolePrivilagesUtama;
                if (map != null) {
                    String prefix = roleId + "_";
                    synchronized (map) {
                        Iterator<String> it = map.keySet().iterator();
                        while (it.hasNext()) {
                            String key = it.next();
                            if (key != null && key.startsWith(prefix)) {
                                it.remove();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "NewUiCacheInvalidator.invalidateRole");
            }
        }
        invalidateAllMenuVersions();
    }

    /** Bump versi global untuk perubahan yang menyangkut seorang user (mis. ganti role). */
    public static void invalidateUser(String userId) {
        invalidateAllMenuVersions();
    }

    /**
     * Bersihkan cache menu pada satu session: attribute New UI + attribute legacy
     * <code>current_menus</code>/<code>current_menu</code>.
     */
    public static void invalidateSession(HttpSession session) {
        if (session == null) {
            return;
        }
        try {
            session.removeAttribute(SESSION_TREE);
            session.removeAttribute(SESSION_TREE_VERSION);
            session.removeAttribute(SESSION_CURRENT_MENUS);
            session.removeAttribute(SESSION_CURRENT_MENU);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiCacheInvalidator.invalidateSession");
        }
    }
}
