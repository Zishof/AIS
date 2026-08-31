package ais.action.master.generic.v2;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.model.Menu;
import ais.database.model.Tbmuser;

/**
 * Konteks satu permintaan HTTP ke framework CRUD generik {@link ais.action.master.generic.v2}:
 * membawa pengguna yang sedang login, menu aktif, definisi entitas ({@link
 * GenericCrudDefinition}) yang sedang diakses, dan hasil resolusi hak akses
 * (baca/buat/ubah/hapus/setujui/tolak) untuk kombinasi pengguna+menu+entitas tersebut.
 *
 * <p>
 * Dibuat sekali per request lewat {@link #from(HttpServletRequest, GenericCrudDefinition)}.
 * Resolusi hak akses berlapis: awalnya diisi dari privilese menu ZK klasik ({@link
 * ais.common.CommonPrivilages}), lalu ditimpa (bila ada aturan rute eksplisit) oleh
 * {@link GenericCrudRoutePrivilegeResolver#resolve}, dan akhirnya — khusus mode
 * {@code administrativeAutoCrud} untuk admin sistem — ditimpa lagi menjadi izin penuh
 * baca/buat/ubah (hapus mengikuti flag {@code definition.isDeleteEnabled()}, setujui/tolak
 * dimatikan). Bidang {@link #request} ditandai {@code transient} karena
 * {@link HttpServletRequest} tidak dapat diserialisasi.
 * </p>
 */
public class GenericCrudRequestContext implements Serializable {
    private static final long serialVersionUID = 1L;
    private transient HttpServletRequest request;
    private GenericCrudDefinition definition;
    private Tbmuser user;
    private Menu menu;
    private boolean canRead;
    private boolean canCreate;
    private boolean canUpdate;
    private boolean canDelete;
    private boolean canApprove;
    private boolean canReject;
    private String requestId;

    /**
     * Membangun konteks permintaan dari {@code request} dan {@code definition} entitas yang
     * diakses: mengambil pengguna yang login, menu aktif dari sesi, dan menghitung hak akses
     * berlapis (privilese menu klasik, lalu aturan rute eksplisit, lalu mode admin otomatis).
     *
     * @param request    permintaan HTTP saat ini, boleh {@code null} (menghasilkan konteks
     *                   kosong tanpa pengguna/hak akses)
     * @param definition definisi entitas CRUD generik yang sedang diakses
     * @return konteks permintaan siap pakai untuk operasi CRUD generik selanjutnya
     */
    public static GenericCrudRequestContext from(HttpServletRequest request, GenericCrudDefinition definition) {
        GenericCrudRequestContext context = new GenericCrudRequestContext();
        context.request = request;
        context.definition = definition;
        context.requestId = request == null ? null : first(request.getHeader("X-Request-ID"), request.getParameter("requestId"));
        if (request != null) {
            try { context.user = Common.getCurrentUser(request); } catch (Exception ignored) { context.user = null; }
            Object currentMenu = request.getSession(false) == null ? null : request.getSession(false).getAttribute("current_menu");
            if (currentMenu instanceof Menu) { context.menu = (Menu) currentMenu; }
            if (context.menu != null) {
                context.canRead = check(context.menu, CommonPrivilages.READ, context.user);
                context.canCreate = check(context.menu, CommonPrivilages.CREATE, context.user);
                context.canUpdate = check(context.menu, CommonPrivilages.UPDATE, context.user);
                context.canDelete = check(context.menu, CommonPrivilages.DELETE, context.user);
                context.canApprove = check(context.menu, CommonPrivilages.APPROVE, context.user);
                context.canReject = check(context.menu, CommonPrivilages.REJECT, context.user);
            }
            boolean[] routePermission = GenericCrudRoutePrivilegeResolver.resolve(context.user,
                    definition.getModuleKey(), definition.getPageKey(), menuId(request));
            if (routePermission != null) {
                context.canRead = routePermission[0];
                context.canCreate = routePermission[1];
                context.canUpdate = routePermission[2];
                context.canDelete = routePermission[3];
                context.canApprove = routePermission[4];
                context.canReject = routePermission[5];
            }
            if (definition.isAdministrativeAutoCrud() && Common.getApakahAdmin()) {
                context.canRead = true;
                context.canCreate = true;
                context.canUpdate = true;
                context.canDelete = definition.isDeleteEnabled();
                context.canApprove = false;
                context.canReject = false;
            }
        }
        return context;
    }

    /** Mengembalikan {@code one} bila tidak kosong, selain itu {@code two}. */
    private static String first(String one, String two) { return one != null && one.length() > 0 ? one : two; }
    /** Membaca parameter {@code menuId} (atau {@code menu} sebagai fallback) dari {@code request} dan mengonversinya ke {@link Long}, {@code null} bila tidak ada/tidak valid. */
    private static Long menuId(HttpServletRequest request) {
        if (request == null) return null;
        String value = request.getParameter("menuId");
        if (value == null || value.trim().length() == 0) value = request.getParameter("menu");
        try { return value == null ? null : Long.valueOf(value); }
        catch (Exception invalid) { return null; }
    }
    /**
     * Mengecek privilese {@code code} pada {@code menu} untuk {@code user}, memakai overload
     * {@code checkPrevilages(Menu, Integer, Tbmuser)} lewat refleksi bila tersedia (build baru),
     * atau jatuh kembali ke {@code checkPrevilages(Menu, Integer)} (build lama yang belum punya
     * overload ber-user) bila method tersebut tidak ditemukan.
     */
    private static boolean check(Menu menu, Integer code, Tbmuser user) {
        try {
            java.lang.reflect.Method exact = CommonPrivilages.class.getMethod("checkPrevilages",
                    new Class[] { Menu.class, Integer.class, Tbmuser.class });
            Object result = exact.invoke(null, new Object[] { menu, code, user });
            return Boolean.TRUE.equals(result);
        } catch (NoSuchMethodException oldBuild) {
            return CommonPrivilages.checkPrevilages(menu, code);
        } catch (Exception denied) { return false; }
    }
    public HttpServletRequest getRequest() { return request; }
    public GenericCrudDefinition getDefinition() { return definition; }
    public Tbmuser getUser() { return user; }
    public Menu getMenu() { return menu; }
    public boolean isCanRead() { return canRead; }
    public boolean isCanCreate() { return canCreate; }
    public boolean isCanUpdate() { return canUpdate; }
    public boolean isCanDelete() { return canDelete; }
    public boolean isCanApprove() { return canApprove; }
    public boolean isCanReject() { return canReject; }
    public String getRequestId() { return requestId; }
}
