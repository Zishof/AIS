package ais.action.master.generic.v2;

import java.io.Serializable;

/** Restore hanya melalui adapter + policy eksplisit; tidak ada reflection mass-write generik. */
public class GenericCrudRestoreService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();

    public GenericCrudResult restoreField(GenericCrudRequestContext context, Serializable id, Number revision,
            String property, Object token, String reason) throws Exception {
        privilege.require(context, GenericCrudOperation.RESTORE);
        requireReason(reason);
        GenericCrudFieldDefinition field = context.getDefinition().getField(property);
        if (field == null || !field.isRestoreable()) throw new GenericCrudException(403, "FIELD_RESTORE_DISABLED", "Field tidak diizinkan untuk restore.");
        if (!ready(context) || !context.getDefinition().getRestorePolicy().canRestoreField(context, id, property, revision)) {
            throw new GenericCrudException(403, "RESTORE_POLICY_DENIED", "Policy menolak restore field.");
        }
        return context.getDefinition().getAuditRevisionAdapter().restoreField(context, id, revision, property, token, reason);
    }

    public GenericCrudResult restoreRevision(GenericCrudRequestContext context, Serializable id, Number revision,
            boolean deep, Object token, String reason) throws Exception {
        privilege.require(context, GenericCrudOperation.RESTORE);
        requireReason(reason);
        if (!ready(context) || !context.getDefinition().getRestorePolicy().canRestoreRevision(context, id, revision, deep)) {
            throw new GenericCrudException(403, "RESTORE_POLICY_DENIED", "Policy menolak restore revisi.");
        }
        if (deep && !context.getDefinition().getAuditRevisionAdapter().supportsDeepRestore()) {
            throw new GenericCrudException(403, "DEEP_RESTORE_DISABLED", "Deep restore belum dikonfigurasi.");
        }
        return context.getDefinition().getAuditRevisionAdapter().restoreRevision(context, id, revision, deep, token, reason);
    }

    private boolean ready(GenericCrudRequestContext context) {
        return context.getDefinition().isRestoreEnabled() && context.getDefinition().getRestorePolicy() != null
                && context.getDefinition().getAuditRevisionAdapter() != null;
    }
    private void requireReason(String reason) throws GenericCrudException {
        if (reason == null || reason.trim().length() < 5) throw new GenericCrudException(400, "REASON_REQUIRED", "Alasan restore minimal 5 karakter.");
    }
}
