package ais.common.newui;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;

/**
 * Layanan pergantian role aktif untuk pengguna multi-role New UI.
 *
 * <p>Mengikuti mekanisme existing: role aktif disimpan pada map statik
 * {@link Tbmuser#getUserRoleYgDipakai} (kunci userId) yang dibaca
 * {@link Tbmuser#hakAkses()}. Switch = menetapkan entri map itu ke role yang
 * <b>benar-benar dimiliki</b> user ({@link Tbmuser#ambilRoles()}) dan <b>aktif</b>,
 * lalu menginvalidasi cache menu/privilege. <b>Privilege TIDAK di-union.</b></p>
 *
 * <p><b>Keterbatasan bawaan:</b> {@code getUserRoleYgDipakai} bersifat statik per
 * userId, sehingga role aktif berlaku lintas-session untuk userId yang sama dan
 * memengaruhi UI legacy maupun New UI (keduanya memakai {@code hakAkses()}).</p>
 *
 * <p>Kompatibel Java 1.6.</p>
 */
public final class NewUiRoleSwitcherService {

    private NewUiRoleSwitcherService() {
    }

    /** roleId aktif saat ini, atau null. */
    public static String getActiveRoleId(HttpServletRequest request) {
        Tbmuser user = currentUser(request);
        if (user == null) {
            return null;
        }
        try {
            Tbmrole r = user.hakAkses();
            return r == null ? null : r.getRoleId();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.getActiveRoleId");
            return null;
        }
    }

    /**
     * Opsi role untuk UI switcher. Tiap baris: [roleId, roleName, aktif(1/0), sedangDipakai(1/0)].
     * Aman terhadap lazy/null (dibungkus try-catch per role).
     */
    public static List<String[]> getRoleOptions(HttpServletRequest request) {
        List<String[]> options = new ArrayList<String[]>();
        Tbmuser user = currentUser(request);
        if (user == null) {
            return options;
        }
        String activeId = getActiveRoleId(request);
        List<Tbmrole> roles = null;
        try {
            roles = user.ambilRoles();
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.ambilRoles");
            return options;
        }
        if (roles == null) {
            return options;
        }
        for (int i = 0; i < roles.size(); i++) {
            Tbmrole r = roles.get(i);
            if (r == null) {
                continue;
            }
            try {
                String roleId = r.getRoleId();
                if (roleId == null) {
                    continue;
                }
                String roleName = r.getRoleName() != null ? r.getRoleName() : roleId;
                String aktif = Boolean.FALSE.equals(r.getAktif()) ? "0" : "1";
                String current = roleId.equals(activeId) ? "1" : "0";
                options.add(new String[] { roleId, roleName, aktif, current });
            } catch (Exception e) {
                ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.getRoleOptions.row");
            }
        }
        return options;
    }

    /** true bila user memiliki lebih dari satu role (menentukan tampil/tidaknya dropdown). */
    public static boolean hasMultipleRoles(HttpServletRequest request) {
        List<String[]> options = getRoleOptions(request);
        return options != null && options.size() > 1;
    }

    /**
     * Ganti role aktif. Fail-closed: menolak roleId yang bukan milik user atau role nonaktif.
     * @return true bila berhasil.
     */
    public static boolean switchRole(HttpServletRequest request, String roleId) {
        if (roleId == null || roleId.length() == 0) {
            return false;
        }
        Tbmuser user = currentUser(request);
        if (user == null) {
            return false;
        }
        String userId = user.getUserId();
        if (userId == null) {
            return false;
        }

        // target harus benar-benar dimiliki user (bukan sekadar ada di DB)
        Tbmrole target = null;
        try {
            List<Tbmrole> roles = user.ambilRoles();
            if (roles != null) {
                for (int i = 0; i < roles.size(); i++) {
                    Tbmrole r = roles.get(i);
                    if (r != null && roleId.equals(r.getRoleId())) {
                        target = r;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.switchRole.lookup");
            return false;
        }
        if (target == null) {
            return false; // bukan milik user → tolak (cegah privilege escalation)
        }
        if (Boolean.FALSE.equals(target.getAktif())) {
            return false; // role nonaktif → tolak
        }

        String previousId = getActiveRoleId(request);

        // tetapkan role aktif (kompatibel mekanisme existing)
        try {
            Tbmuser.getUserRoleYgDipakai.put(userId, target);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.switchRole.set");
            return false;
        }

        // invalidasi cache: privilege role terkait + menu session + tree New UI
        try {
            if (previousId != null) {
                NewUiCacheInvalidator.invalidateRole(previousId);
            }
            NewUiCacheInvalidator.invalidateRole(roleId);
            NewUiCacheInvalidator.invalidateSession(request.getSession(false));
            NewUiCacheInvalidator.invalidateUser(userId);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.switchRole.invalidate");
        }

        return true;
    }

    private static Tbmuser currentUser(HttpServletRequest request) {
        try {
            return Common.getCurrentUser(request);
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "NewUiRoleSwitcherService.currentUser");
            return null;
        }
    }
}
