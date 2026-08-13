package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

import ais.database.hibernate.HibernateUtil;
import ais.action.master.generic.v2.adapter.GenericCrudRowSanitizer;
import ais.database.model.GeneralValueObject;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GenericCrudAuditService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();

    public GenericCrudPage listRowRevisions(GenericCrudRequestContext context, Serializable id, int page, int pageSize) throws Exception {
        privilege.require(context, GenericCrudOperation.AUDIT);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isAuditEnabled() || !definition.isRowAuditEnabled()) {
            throw new GenericCrudException(403, "ROW_AUDIT_DISABLED", "Audit per data belum diaktifkan.");
        }
        page = page < 1 ? 1 : page;
        pageSize = Math.min(pageSize < 1 ? 10 : pageSize, 100);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Object active = session.get(definition.getEntityClass(), id);
            if (!(active instanceof ais.database.model.GeneralValueObject)) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data aktif tidak ditemukan atau berada di luar scope.");
            scope.validateObject((ais.database.model.GeneralValueObject) active, context);
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery countQuery = reader.createQuery().forRevisionsOfEntity(definition.getEntityClass(), false, true)
                    .add(AuditEntity.id().eq(id)).addProjection(AuditEntity.revisionNumber().count());
            Number total = (Number) countQuery.getSingleResult();
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(definition.getEntityClass(), false, true)
                    .add(AuditEntity.id().eq(id)).addOrder(AuditEntity.revisionNumber().desc())
                    .setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
            List all = query.getResultList();
            List rows = new ArrayList();
            for (int i = 0; i < all.size(); i++) { rows.add(toRevisionRow((Object[]) all.get(i))); }
            GenericCrudPage result = new GenericCrudPage();
            result.setRows(rows);
            result.setTotal(total == null ? 0L : total.longValue());
            result.setPage(page);
            result.setPageSize(pageSize);
            return result;
        } finally { close(session); }
    }

    public Map compare(GenericCrudRequestContext context, Serializable id, Number left, Number right) throws Exception {
        privilege.require(context, GenericCrudOperation.AUDIT);
        if (!context.getDefinition().isRowAuditEnabled()) { throw new GenericCrudException(403, "ROW_AUDIT_DISABLED", "Audit per data belum diaktifkan."); }
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Object active = session.get(context.getDefinition().getEntityClass(), id);
            if (!(active instanceof ais.database.model.GeneralValueObject)) throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data aktif tidak ditemukan atau berada di luar scope.");
            scope.validateObject((ais.database.model.GeneralValueObject) active, context);
            AuditReader reader = AuditReaderFactory.get(session);
            Object one = reader.find(context.getDefinition().getEntityClass(), id, left);
            Object two = reader.find(context.getDefinition().getEntityClass(), id, right);
            if (one == null || two == null) { throw new GenericCrudException(404, "REVISION_NOT_FOUND", "Revisi tidak ditemukan."); }
            Map result = new LinkedHashMap();
            List fields = context.getDefinition().getFields();
            java.beans.PropertyDescriptor[] descriptors = java.beans.Introspector.getBeanInfo(context.getDefinition().getEntityClass()).getPropertyDescriptors();
            for (int i = 0; i < fields.size(); i++) {
                GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
                if (field.isSensitive()) { continue; }
                java.beans.PropertyDescriptor descriptor = find(descriptors, field.getProperty());
                if (descriptor == null || descriptor.getReadMethod() == null) { continue; }
                Object oldValue = descriptor.getReadMethod().invoke(one, new Object[0]);
                Object newValue = descriptor.getReadMethod().invoke(two, new Object[0]);
                if (oldValue == null ? newValue != null : !oldValue.equals(newValue)) {
                    Map change = new LinkedHashMap();
                    GenericCrudRowSanitizer sanitizer = context.getDefinition().getAdapter()
                            instanceof GenericCrudRowSanitizer
                            ? (GenericCrudRowSanitizer) context.getDefinition().getAdapter() : null;
                    boolean sensitive = sanitizer != null
                            && ((one instanceof GeneralValueObject
                                    && sanitizer.isSensitiveProperty((GeneralValueObject) one,
                                            field.getProperty(), context))
                                || (two instanceof GeneralValueObject
                                    && sanitizer.isSensitiveProperty((GeneralValueObject) two,
                                            field.getProperty(), context)));
                    change.put("left", sensitive ? "********" : oldValue);
                    change.put("right", sensitive ? "********" : newValue);
                    result.put(field.getProperty(), change);
                }
            }
            return result;
        } finally { close(session); }
    }

    public GenericCrudPage listGlobalRevisions(GenericCrudRequestContext context, int page, int pageSize) throws Exception {
        privilege.require(context, GenericCrudOperation.AUDIT);
        GenericCrudDefinition definition = context.getDefinition();
        if (!definition.isGlobalAuditEnabled() || definition.getRestorePolicy() == null
                || !definition.getRestorePolicy().canViewGlobalAudit(context)) {
            throw new GenericCrudException(403, "GLOBAL_AUDIT_DISABLED", "Audit global belum diaktifkan atau ditolak policy.");
        }
        page = page < 1 ? 1 : page; pageSize = Math.min(pageSize < 1 ? 10 : pageSize, 100);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            AuditReader reader = AuditReaderFactory.get(session);
            AuditQuery query = reader.createQuery().forRevisionsOfEntity(definition.getEntityClass(), false, true)
                    .addOrder(AuditEntity.revisionNumber().desc()).setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
            List tuples = query.getResultList(); List rows = new ArrayList();
            org.hibernate.metadata.ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(definition.getEntityClass());
            for (int i = 0; i < tuples.size(); i++) {
                Object[] tuple = (Object[]) tuples.get(i); Object audited = tuple[0];
                Serializable id = metadata.getIdentifier(audited, org.hibernate.EntityMode.POJO);
                Object active = session.get(definition.getEntityClass(), id);
                if (!(active instanceof ais.database.model.GeneralValueObject)) continue;
                try { scope.validateObject((ais.database.model.GeneralValueObject) active, context); }
                catch (Exception outsideScope) { continue; }
                Map row = toRevisionRow(tuple); row.put("id", id); rows.add(row);
            }
            GenericCrudPage result = new GenericCrudPage(); result.setRows(rows); result.setTotal(rows.size()); result.setPage(page); result.setPageSize(pageSize); return result;
        } finally { close(session); }
    }

    private Map toRevisionRow(Object[] tuple) {
        Map row = new LinkedHashMap();
        Object revision = tuple.length > 1 ? tuple[1] : null;
        if (revision instanceof DefaultRevisionEntity) {
            DefaultRevisionEntity entity = (DefaultRevisionEntity) revision;
            row.put("revision", Integer.valueOf(entity.getId()));
            row.put("timestamp", Long.valueOf(entity.getTimestamp()));
        } else { row.put("revision", String.valueOf(revision)); }
        row.put("type", tuple.length > 2 ? String.valueOf(tuple[2]) : "");
        return row;
    }

    private java.beans.PropertyDescriptor find(java.beans.PropertyDescriptor[] descriptors, String name) {
        for (int i = 0; i < descriptors.length; i++) if (name.equals(descriptors[i].getName())) return descriptors[i];
        return null;
    }
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
