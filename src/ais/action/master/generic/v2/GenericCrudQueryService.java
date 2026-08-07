package ais.action.master.generic.v2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudQueryService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();

    public GenericCrudPage list(GenericCrudRequestContext context, int page, int pageSize,
            String search, List filters, GenericCrudSort sort) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        GenericCrudDefinition definition = context.getDefinition();
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        page = page < 1 ? 1 : page;
        pageSize = normalizePageSize(pageSize, definition);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Criteria count = session.createCriteria(definition.getEntityClass());
            definition.getAdapter().applyDefaultFilters(count, context);
            scope.applyCount(count, context);
            applySearch(count, definition, search);
            applyFilters(count, definition, metadata, filters);
            Number total = (Number) count.setProjection(Projections.rowCount()).uniqueResult();

            Criteria query = session.createCriteria(definition.getEntityClass());
            definition.getAdapter().applyDefaultFilters(query, context);
            scope.applyRead(query, context);
            applySearch(query, definition, search);
            applyFilters(query, definition, metadata, filters);
            GenericCrudSort safeSort = validateSort(definition, sort);
            query.addOrder(safeSort.isAscending() ? Order.asc(safeSort.getProperty()) : Order.desc(safeSort.getProperty()));
            if (!definition.getIdentifierProperty().equals(safeSort.getProperty())) { query.addOrder(Order.asc(definition.getIdentifierProperty())); }
            query.setFirstResult((page - 1) * pageSize).setMaxResults(pageSize);
            List objects = query.list();
            List rows = new ArrayList();
            for (int i = 0; i < objects.size(); i++) { rows.add(toRow(objects.get(i), definition, metadata)); }
            GenericCrudPage result = new GenericCrudPage();
            result.setRows(rows);
            result.setTotal(total == null ? 0L : total.longValue());
            result.setPage(page);
            result.setPageSize(pageSize);
            return result;
        } finally { close(session); }
    }

    public Map get(GenericCrudRequestContext context, Serializable id) throws Exception {
        privilege.require(context, GenericCrudOperation.READ);
        GenericCrudDefinition definition = context.getDefinition();
        ClassMetadata metadata = GenericCrudRuntimeMetadataVerifier.verify(definition);
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            Object object = session.get(definition.getEntityClass(), id);
            if (!(object instanceof GeneralValueObject)) { throw new GenericCrudException(404, "ROW_NOT_FOUND", "Data tidak ditemukan."); }
            scope.validateObject((GeneralValueObject) object, context);
            return toRow(object, definition, metadata);
        } finally { close(session); }
    }

    private int normalizePageSize(int size, GenericCrudDefinition definition) {
        int value = size < 1 ? definition.getDefaultPageSize() : size;
        return Math.min(value, definition.getMaxPageSize());
    }

    private GenericCrudSort validateSort(GenericCrudDefinition definition, GenericCrudSort sort) throws GenericCrudException {
        if (sort != null) {
            GenericCrudFieldDefinition field = definition.getField(sort.getProperty());
            if (field == null || !field.isSortable() || !field.isReadable() || field.isSensitive()) {
                throw new GenericCrudException(400, "SORT_NOT_ALLOWED", "Kolom tidak diizinkan untuk sorting.");
            }
            return sort;
        }
        return new GenericCrudSort(definition.getDefaultSortProperty(), definition.isDefaultSortAscending());
    }

    private void applySearch(Criteria criteria, GenericCrudDefinition definition, String search) {
        if (search == null || search.trim().length() == 0) { return; }
        Disjunction or = Restrictions.disjunction();
        Iterator fields = definition.getFields().iterator();
        while (fields.hasNext()) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.next();
            if (field.isSearchable() && String.class.getName().equals(field.getJavaType())) {
                or.add(Restrictions.ilike(field.getProperty(), "%" + search.trim() + "%"));
            }
        }
        criteria.add(or);
    }

    private void applyFilters(Criteria criteria, GenericCrudDefinition definition, ClassMetadata metadata, List filters) throws Exception {
        if (filters == null) { return; }
        for (int i = 0; i < filters.size(); i++) {
            GenericCrudFilter filter = (GenericCrudFilter) filters.get(i);
            GenericCrudFieldDefinition field = definition.getField(filter.getProperty());
            if (field == null || !field.isSearchable() || !field.isReadable() || field.isSensitive()) {
                throw new GenericCrudException(400, "FIELD_NOT_ALLOWED", "Field filter tidak diizinkan.");
            }
            Type type = metadata.getPropertyType(filter.getProperty());
            Object value = GenericCrudFilter.IN.equals(filter.getOperator()) ? null
                    : GenericCrudValueConverter.convert(filter.getValue(), type.getReturnedClass());
            Criterion criterion;
            if (GenericCrudFilter.CONTAINS.equals(filter.getOperator()) && type.getReturnedClass() == String.class) criterion = Restrictions.ilike(filter.getProperty(), "%" + value + "%");
            else if (GenericCrudFilter.STARTS_WITH.equals(filter.getOperator()) && type.getReturnedClass() == String.class) criterion = Restrictions.ilike(filter.getProperty(), value + "%");
            else if (GenericCrudFilter.NE.equals(filter.getOperator())) criterion = Restrictions.ne(filter.getProperty(), value);
            else if (GenericCrudFilter.GT.equals(filter.getOperator())) criterion = Restrictions.gt(filter.getProperty(), value);
            else if (GenericCrudFilter.GTE.equals(filter.getOperator())) criterion = Restrictions.ge(filter.getProperty(), value);
            else if (GenericCrudFilter.LT.equals(filter.getOperator())) criterion = Restrictions.lt(filter.getProperty(), value);
            else if (GenericCrudFilter.LTE.equals(filter.getOperator())) criterion = Restrictions.le(filter.getProperty(), value);
            else if (GenericCrudFilter.IS_NULL.equals(filter.getOperator())) criterion = Restrictions.isNull(filter.getProperty());
            else if (GenericCrudFilter.IS_NOT_NULL.equals(filter.getOperator())) criterion = Restrictions.isNotNull(filter.getProperty());
            else if (GenericCrudFilter.IN.equals(filter.getOperator())) {
                String[] parts = String.valueOf(filter.getValue()).split(",");
                Object[] values = new Object[parts.length];
                for (int p = 0; p < parts.length; p++) values[p] = GenericCrudValueConverter.convert(parts[p], type.getReturnedClass());
                criterion = Restrictions.in(filter.getProperty(), values);
            } else if (GenericCrudFilter.EQ.equals(filter.getOperator())) criterion = Restrictions.eq(filter.getProperty(), value);
            else throw new GenericCrudException(400, "FILTER_OPERATOR_INVALID", "Operator filter tidak diizinkan.");
            criteria.add(criterion);
        }
    }

    private Map toRow(Object object, GenericCrudDefinition definition, ClassMetadata metadata) {
        Map row = new LinkedHashMap();
        row.put(definition.getIdentifierProperty(), metadata.getIdentifier(object, EntityMode.POJO));
        Iterator fields = definition.getFields().iterator();
        while (fields.hasNext()) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.next();
            if (field.isSensitive() || !field.isReadable() || definition.getIdentifierProperty().equals(field.getProperty())) { continue; }
            Object value = metadata.getPropertyValue(object, field.getProperty(), EntityMode.POJO);
            if (value instanceof GeneralValueObject) { value = String.valueOf(value); }
            row.put(field.getProperty(), value);
        }
        return row;
    }
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
