package ais.action.master.library.modern;

import javax.servlet.http.HttpServletRequest;

import ais.common.Common;
import ais.database.model.Tbmuser;

/** Central role guard for the modern library adapters. */
public final class LibraryPermissionGuard {
    private LibraryPermissionGuard() { }

    public static boolean isStaff(HttpServletRequest request) {
        Tbmuser user=Common.getCurrentUser(request);if(user==null)return false;
        if(Common.getApakahAdmin())return true;
        try{String role=user.hakAkses()==null||user.hakAkses().getRoleId()==null?"":user.hakAkses().getRoleId().trim().toLowerCase();return role.contains("pustaka")||role.contains("library")||role.contains("librarian");}catch(Exception ignored){return false;}
    }

    public static boolean isAdministrator(HttpServletRequest request) {
        return Common.getCurrentUser(request)!=null&&Common.getApakahAdmin();
    }
}
