package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/** Lookup paged untuk field many-to-one yang didefinisikan server-side. */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudRelationLookupService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();

    public GenericCrudResult lookup(GenericCrudRequestContext context, String property,
            String search, int page, int pageSize) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        GenericCrudFieldDefinition field = context.getDefinition().getField(property);
        if (field == null || field.getRelationEntityKey() == null
                || (!field.isCreateable() && !field.isUpdateable())) {
            throw new GenericCrudException(400, "RELATION_FIELD_NOT_ALLOWED", "Field relasi tidak diizinkan.");
        }
        Class relationClass = findMappedRelation(field.getRelationEntityKey());
        if (relationClass == null) {
            throw new GenericCrudException(400, "RELATION_NOT_MAPPED", "Model relasi tidak terdaftar pada Hibernate.");
        }
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(relationClass);
        page = page < 1 ? 1 : page;
        pageSize = Math.max(1, Math.min(pageSize, 50));
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Criteria count = session.createCriteria(relationClass);
            applySearch(count, metadata, search);
            Number total = (Number) count.setProjection(Projections.rowCount()).uniqueResult();

            Criteria rows = session.createCriteria(relationClass);
            applySearch(rows, metadata, search);
            String display = safeDisplayProperty(metadata, field.getRelationDisplayProperty());
            if (!metadata.getIdentifierPropertyName().equals(display)) rows.addOrder(Order.asc(display));
            rows.addOrder(Order.asc(metadata.getIdentifierPropertyName()));
            rows.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
            List objects = rows.list();
            List options = new ArrayList();
            for (int i = 0; i < objects.size(); i++) {
                Object object = objects.get(i);
                Object id = metadata.getIdentifier(object, EntityMode.POJO);
                Map option = new LinkedHashMap();
                option.put("id", id);
                option.put("label", label(metadata, object, display, id));
                options.add(option);
            }
            Map data = new LinkedHashMap();
            long totalRows = total == null ? 0L : total.longValue();
            data.put("items", options);
            data.put("total", Long.valueOf(totalRows));
            data.put("page", Integer.valueOf(page));
            data.put("pageSize", Integer.valueOf(pageSize));
            data.put("hasMore", Boolean.valueOf((long) page * pageSize < totalRows));
            return GenericCrudResult.ok("Pilihan relasi berhasil dimuat.", data);
        } finally {
            try { if (session.isOpen()) session.close(); } catch (Exception ignored) { }
        }
    }

    private Class findMappedRelation(String key) {
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        java.util.Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            try {
                Class mapped = metadata.getMappedClass(EntityMode.POJO);
                if (mapped != null && key.equals(mapped.getName())
                        && GeneralValueObject.class.isAssignableFrom(mapped)) return mapped;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private void applySearch(Criteria criteria, ClassMetadata metadata, String search) {
        if (search == null || search.trim().length() == 0) return;
        String value = "%" + search.trim() + "%";
        Disjunction any = Restrictions.disjunction();
        int added = 0;
        String[] candidates = new String[] { "kode", "nama", "nim" };
        for (int i = 0; i < candidates.length; i++) {
            try {
                if (metadata.getPropertyType(candidates[i]).getReturnedClass() == String.class) {
                    any.add(Restrictions.ilike(candidates[i], value)); added++;
                }
            } catch (Exception ignored) { }
        }
        if (added > 0) criteria.add(any);
    }

    private String safeDisplayProperty(ClassMetadata metadata, String preferred) {
        try { if (preferred != null) { metadata.getPropertyType(preferred); return preferred; } }
        catch (Exception ignored) { }
        return metadata.getIdentifierPropertyName();
    }

    private String label(ClassMetadata metadata, Object object, String display, Object id) {
        try {
            Object value = metadata.getIdentifierPropertyName().equals(display) ? id
                    : metadata.getPropertyValue(object, display, EntityMode.POJO);
            if (value != null && String.valueOf(value).trim().length() > 0) return String.valueOf(value);
        } catch (Exception ignored) { }
        return String.valueOf(id);
    }
}
