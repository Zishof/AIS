package ais.action.master.generic.v2;

import ais.common.CommonPrivilages;

/** RBAC fail-closed: session login, menu binding, dan privilege operasi wajib cocok. */
public class GenericCrudPrivilegeGuard {
    public void require(GenericCrudRequestContext context, String operation) throws GenericCrudException {
        if (context == null || context.getUser() == null) {
            throw new GenericCrudException(401, "AUTH_REQUIRED", "Sesi login tidak tersedia atau sudah berakhir.");
        }
        boolean allowed = fromContext(context, operation);
        if (!allowed) { throw new GenericCrudException(403, "PRIVILEGE_DENIED", "Hak akses operasi tidak diberikan untuk role aktif."); }
    }

    private boolean fromContext(GenericCrudRequestContext context, String operation) {
        if (GenericCrudOperation.READ.equals(operation) || GenericCrudOperation.EXPORT.equals(operation)
                || GenericCrudOperation.AUDIT.equals(operation)) { return context.isCanRead(); }
        if (GenericCrudOperation.CREATE.equals(operation) || GenericCrudOperation.IMPORT.equals(operation)) { return context.isCanCreate(); }
        if (GenericCrudOperation.UPDATE.equals(operation) || GenericCrudOperation.RESTORE.equals(operation)) { return context.isCanUpdate(); }
        if (GenericCrudOperation.DELETE.equals(operation)) { return context.isCanDelete(); }
        if (GenericCrudOperation.APPROVE.equals(operation)) { return context.isCanApprove(); }
        if (GenericCrudOperation.REJECT.equals(operation)) { return context.isCanReject(); }
        return false;
    }
}
