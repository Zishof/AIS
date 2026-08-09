package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;

import ais.common.CommonPrivilages;
import ais.action.master.generic.v2.adapter.GenericCrudSessionValueAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudMutationService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();
    private final GenericCrudValidationService validation = new GenericCrudValidationService();

    public GenericCrudResult create(GenericCrudRequestContext context, Map submitted) throws Exception {
        privilege.require(context, GenericCrudOperation.CREATE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isCreateEnabled()) { deny("CREATE_DISABLED", "Pembuatan data belum diaktifkan."); }
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        Map values = allowedValues(definition, submitted, true);
        List errors = validation.validateRequired(definition, values, true);
        definition.getAdapter().validateCreate(values, context, errors);
        if (!errors.isEmpty()) { return validationError(errors); }
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = definition.getAdapter().createNew(context);
            if (definition.getAdapter() instanceof GenericCrudSessionValueAdapter) {
                ((GenericCrudSessionValueAdapter) definition.getAdapter()).applyCreateValues(session, target, values, context);
            } else {
                definition.getAdapter().applyCreateValues(target, values, context);
            }
            scope.validateObject(target, context);
            definition.getAdapter().beforeSave(session, target, context);
            session.save(target);
            session.flush();
            definition.getAdapter().afterSave(session, target, context);
            Serializable id = metadata.getIdentifier(target, EntityMode.POJO);
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.CREATE, target, "Generic CRUD V2");
            return GenericCrudResult.ok("Data berhasil disimpan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { close(session); }
    }

    public GenericCrudResult update(GenericCrudRequestContext context, Serializable id, Map submitted, Object optimisticToken) throws Exception {
        privilege.require(context, GenericCrudOperation.UPDATE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isUpdateEnabled()) { deny("UPDATE_DISABLED", "Perubahan data belum diaktifkan."); }
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        Map values = allowedValues(definition, submitted, false);
        List errors = validation.validateRequired(definition, values, false);
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = (GeneralValueObject) session.get(definition.getEntityClass(), id);
            if (target == null) { throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data tidak ditemukan."); }
            scope.validateObject(target, context);
            verifyOptimisticToken(definition, metadata, target, optimisticToken);
            definition.getAdapter().validateUpdate(target, values, context, errors);
            if (!errors.isEmpty()) { rollback(tx); return validationError(errors); }
            if (definition.getAdapter() instanceof GenericCrudSessionValueAdapter) {
                ((GenericCrudSessionValueAdapter) definition.getAdapter()).applyUpdateValues(session, target, values, context);
            } else {
                definition.getAdapter().applyUpdateValues(target, values, context);
            }
            // Cegah perubahan relasi tenant/pemilik untuk memindahkan record ke luar scope aktif.
            scope.validateObject(target, context);
            definition.getAdapter().beforeSave(session, target, context);
            session.saveOrUpdate(target);
            session.flush();
            definition.getAdapter().afterSave(session, target, context);
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.UPDATE, target, "Generic CRUD V2");
            return GenericCrudResult.ok("Perubahan berhasil disimpan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { close(session); }
    }

    public GenericCrudResult softDelete(GenericCrudRequestContext context, Serializable id) throws Exception {
        privilege.require(context, GenericCrudOperation.DELETE);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isFullCrud() || !definition.isDeleteEnabled()) { deny("DELETE_DISABLED", "Penghapusan belum diaktifkan."); }
        GenericCrudRuntimeMetadataVerifier.verify(definition);
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            GeneralValueObject target = (GeneralValueObject) session.get(definition.getEntityClass(), id);
            if (target == null) { throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data tidak ditemukan."); }
            scope.validateObject(target, context);
            List reasons = new ArrayList();
            if (!definition.getAdapter().canDelete(target, context, reasons)) {
                throw new GenericCrudException(409, "DELETE_BLOCKED", reasons.isEmpty() ? "Data tidak dapat dinonaktifkan." : String.valueOf(reasons.get(0)));
            }
            definition.getAdapter().delete(session, target, context);
            session.flush();
            tx.commit();
            CommonPrivilages.saveActivity(getClass(), CommonPrivilages.DELETE, target, "Soft delete Generic CRUD V2");
            return GenericCrudResult.ok("Data berhasil dinonaktifkan.", idData(id));
        } catch (Exception e) {
            rollback(tx);
            throw mapMutationError(e);
        } finally { close(session); }
    }

    private Map allowedValues(GenericCrudDefinition definition, Map submitted, boolean create) throws GenericCrudException {
        Map values = new LinkedHashMap();
        Iterator iterator = submitted.keySet().iterator();
        while (iterator.hasNext()) {
            String key = String.valueOf(iterator.next());
            GenericCrudFieldDefinition field = definition.getField(key);
            if (field == null || (create ? !field.isCreateable() : !field.isUpdateable())) {
                throw new GenericCrudException(400, "FIELD_NOT_ALLOWED", "Field " + key + " tidak diizinkan untuk operasi ini.");
            }
            values.put(key, submitted.get(key));
        }
        return values;
    }

    private void verifyOptimisticToken(GenericCrudDefinition definition, ClassMetadata metadata,
            Object target, Object token) throws GenericCrudException {
        if (definition.getVersionProperty() == null) { return; }
        if (token == null) { throw new GenericCrudException(409, "OPTIMISTIC_TOKEN_REQUIRED", "Token versi wajib disertakan."); }
        Object current = metadata.getPropertyValue(target, definition.getVersionProperty(), EntityMode.POJO);
        if (current == null || !String.valueOf(current).equals(String.valueOf(token))) {
            throw new GenericCrudException(409, "OPTIMISTIC_CONFLICT", "Data telah berubah. Muat ulang sebelum menyimpan.");
        }
    }

    private GenericCrudResult validationError(List errors) {
        GenericCrudResult result = GenericCrudResult.error("VALIDATION_FAILED", "Periksa kembali data yang diisi.");
        Map fieldErrors = new LinkedHashMap();
        for (int i = 0; i < errors.size(); i++) {
            String text = String.valueOf(errors.get(i));
            int separator = text.indexOf(':');
            fieldErrors.put(separator < 0 ? "_global" : text.substring(0, separator), separator < 0 ? text : text.substring(separator + 1));
        }
        result.setFieldErrors(fieldErrors);
        return result;
    }

    private Map idData(Serializable id) { Map result = new LinkedHashMap(); result.put("id", id); return result; }
    private void deny(String code, String message) throws GenericCrudException { throw new GenericCrudException(403, code, message); }
    private void rollback(Transaction tx) { try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { } }
    private GenericCrudException mapMutationError(Exception error) {
        if (error instanceof GenericCrudException) { return (GenericCrudException) error; }
        if (error instanceof IllegalArgumentException) { return new GenericCrudException(409, "BUSINESS_RULE", error.getMessage()); }
        return new GenericCrudException(500, "MUTATION_FAILED", "Operasi gagal dan transaksi dibatalkan.", error);
    }
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
