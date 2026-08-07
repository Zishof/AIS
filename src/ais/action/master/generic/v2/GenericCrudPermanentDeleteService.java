package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/** Menghapus row bisnis aktif saja; tidak pernah menghapus tabel audit/Envers. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudPermanentDeleteService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();

    public Map preflight(GenericCrudRequestContext context, Serializable id) throws Exception {
        privilege.require(context, GenericCrudOperation.DELETE);
        requirePolicy(context);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Object object = session.get(context.getDefinition().getEntityClass(), id);
            if (!(object instanceof GeneralValueObject)) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data aktif tidak ditemukan.");
            scope.validateObject((GeneralValueObject) object, context);
            Map result = context.getDefinition().getPermanentDeletePolicy().buildPreflight(context, id, object);
            result.put("typedConfirmation", context.getDefinition().getPermanentDeletePolicy().getTypedConfirmation(context, id, object));
            result.put("reasonRequired", Boolean.valueOf(context.getDefinition().getPermanentDeletePolicy().isReasonRequired()));
            return result;
        } finally { close(session); }
    }

    public GenericCrudResult deleteActiveRow(GenericCrudRequestContext context, Serializable id,
            String typedConfirmation, String reason) throws Exception {
        privilege.require(context, GenericCrudOperation.DELETE);
        requirePolicy(context);
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Object object = session.get(context.getDefinition().getEntityClass(), id);
            if (!(object instanceof GeneralValueObject)) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data aktif tidak ditemukan.");
            scope.validateObject((GeneralValueObject) object, context);
            String expected = context.getDefinition().getPermanentDeletePolicy().getTypedConfirmation(context, id, object);
            if (expected == null || !expected.equals(typedConfirmation)) throw new GenericCrudException(400, "CONFIRMATION_MISMATCH", "Teks konfirmasi tidak cocok.");
            if (context.getDefinition().getPermanentDeletePolicy().isReasonRequired() && (reason == null || reason.trim().length() < 5)) {
                throw new GenericCrudException(400, "REASON_REQUIRED", "Alasan penghapusan wajib diisi.");
            }
            if (!context.getDefinition().getPermanentDeletePolicy().canDeleteActiveRow(context, id, object)) {
                throw new GenericCrudException(409, "DELETE_PREFLIGHT_BLOCKED", "Relasi atau kebijakan domain memblokir penghapusan.");
            }
            Map snapshot = new LinkedHashMap();
            snapshot.put("id", id);
            snapshot.put("display", String.valueOf(object));
            context.getDefinition().getPermanentDeletePolicy().beforeDelete(context, id, object);
            session.delete(object);
            session.flush();
            context.getDefinition().getPermanentDeletePolicy().afterDelete(context, id, snapshot);
            tx.commit();
            return GenericCrudResult.ok("Row aktif berhasil dihapus; histori audit dipertahankan.", snapshot);
        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            if (e instanceof GenericCrudException) throw e;
            throw new GenericCrudException(500, "PERMANENT_DELETE_FAILED", "Penghapusan dibatalkan.", e);
        } finally { close(session); }
    }

    private void requirePolicy(GenericCrudRequestContext context) throws GenericCrudException {
        if (!Common.getApakahAdmin()) throw new GenericCrudException(403, "SUPER_ADMIN_REQUIRED", "Operasi hanya untuk Super Admin.");
        if (!context.getDefinition().isAdminDeleteEnabled() || context.getDefinition().getPermanentDeletePolicy() == null
                || !context.getDefinition().getPermanentDeletePolicy().isEnabled()) {
            throw new GenericCrudException(403, "ADMIN_DELETE_DISABLED", "Permanent delete belum diaktifkan untuk entity ini.");
        }
    }
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
