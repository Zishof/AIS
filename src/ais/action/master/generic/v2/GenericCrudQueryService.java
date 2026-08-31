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

/**
 * Layanan baca (query) baku framework CRUD generik {@link ais.action.master.generic.v2}:
 * menyediakan pencarian berpaginasi dengan pencarian teks bebas, filter kolom, dan pengurutan
 * ({@link #list}), serta pengambilan satu baris ({@link #get}). Semua akses field/filter/sort
 * divalidasi terhadap {@link GenericCrudFieldDefinition} entitas — kolom yang tidak
 * {@code searchable}/{@code sortable}/{@code readable}, atau ditandai
 * {@link GenericCrudFieldDefinition#isSensitive()}, ditolak dengan {@link GenericCrudException}
 * — sehingga pemanggil tidak dapat memaksa membaca atau mengurutkan berdasarkan kolom yang tidak
 * diizinkan lewat manipulasi parameter request.
 *
 * <p>
 * Setiap query menghormati izin ({@link GenericCrudOperation#READ}, lewat {@link #privilege}) dan
 * scope data pengguna (lewat {@link #scope}, diterapkan berbeda untuk hitung total vs
 * pengambilan baris). Baris hasil diubah ke {@link Map} generik lewat {@link #toRow}: relasi ke
 * entitas lain diringkas menjadi id + label tampilan ({@code __label}), bukan objek penuh,
 * sehingga aman diserialisasi ke klien tanpa risiko lazy-loading di luar sesi.
 * </p>
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudQueryService {
    private final GenericCrudPrivilegeGuard privilege = new GenericCrudPrivilegeGuard();
    private final GenericCrudScopeGuard scope = new GenericCrudScopeGuard();

    /**
     * Mengambil satu halaman data entitas sesuai {@code context}, dengan pencarian teks bebas
     * ({@code search}, dicocokkan {@code ilike} pada field bertipe {@code String} yang
     * {@code searchable}), daftar {@code filters} kolom, dan {@code sort}. Total baris dihitung
     * terpisah dari pengambilan baris agar scope hitung dan scope baca dapat diterapkan secara
     * independen. Pengurutan sekunder berdasarkan id ditambahkan otomatis bila kolom urut utama
     * bukan id, demi hasil yang stabil antar-halaman.
     *
     * @param context  konteks permintaan, sumber izin akses, scope, dan definisi entitas
     * @param page     halaman yang diminta (dipaksa minimal 1)
     * @param pageSize ukuran halaman (dinormalisasi ke default/maksimum definisi entitas)
     * @param search   kata kunci pencarian bebas, boleh {@code null}/kosong
     * @param filters  daftar {@link GenericCrudFilter} kolom, boleh {@code null}
     * @param sort     pengurutan yang diminta; bila {@code null}, dipakai default definisi entitas
     * @return halaman berisi baris data (bentuk {@link Map}) beserta total jumlahnya
     * @throws GenericCrudException bila kolom sort/filter tidak diizinkan atau operator filter
     *                               tidak dikenal
     */
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

    /**
     * Mengambil satu baris data berdasarkan {@code id}, diubah ke bentuk {@link Map} siap
     * tampil.
     *
     * @param context konteks permintaan, sumber izin akses, scope, dan definisi entitas
     * @param id      id baris data yang diminta
     * @return baris data dalam bentuk {@link Map} (properti ke nilai, relasi diringkas id+label)
     * @throws GenericCrudException bila data tidak ditemukan atau berada di luar scope pengguna
     */
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

    /** Menormalkan ukuran halaman: memakai default definisi bila {@code size} kurang dari 1, dibatasi maksimum definisi. */
    private int normalizePageSize(int size, GenericCrudDefinition definition) {
        int value = size < 1 ? definition.getDefaultPageSize() : size;
        return Math.min(value, definition.getMaxPageSize());
    }

    /** Memvalidasi {@code sort} terhadap definisi field (harus sortable, readable, tidak sensitive); mengembalikan sort default entitas bila {@code sort} {@code null}. */
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

    /** Menambahkan kondisi {@code OR ilike '%search%'} ke {@code criteria} pada seluruh field {@code String} yang searchable; tidak melakukan apa pun bila {@code search} kosong. */
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

    /**
     * Menerapkan {@code filters} ke {@code criteria}, satu {@link Criterion} per filter sesuai
     * operatornya ({@code CONTAINS}/{@code STARTS_WITH} untuk String, {@code NE}/{@code GT}/
     * {@code GTE}/{@code LT}/{@code LTE}/{@code EQ}, {@code IS_NULL}/{@code IS_NOT_NULL},
     * {@code IN} dengan nilai dipisah koma). Setiap kolom filter divalidasi harus searchable,
     * readable, dan tidak sensitive sebelum diterapkan.
     *
     * @throws GenericCrudException bila kolom filter tidak diizinkan atau operatornya tidak dikenal
     */
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

    /**
     * Mengubah satu entitas {@code object} menjadi {@link Map} baris siap tampil: field
     * sensitif/tidak-readable/id dilewati (id ditambahkan terpisah di awal), dan field bertipe
     * relasi entitas ({@link GeneralValueObject}) diringkas menjadi pasangan id +
     * {@code <properti>__label} (label tampilan) alih-alih objek penuh.
     */
    private Map toRow(Object object, GenericCrudDefinition definition, ClassMetadata metadata) {
        Map row = new LinkedHashMap();
        row.put(definition.getIdentifierProperty(), metadata.getIdentifier(object, EntityMode.POJO));
        Iterator fields = definition.getFields().iterator();
        while (fields.hasNext()) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.next();
            if (field.isSensitive() || !field.isReadable() || definition.getIdentifierProperty().equals(field.getProperty())) { continue; }
            Object value = metadata.getPropertyValue(object, field.getProperty(), EntityMode.POJO);
            if (value instanceof GeneralValueObject && field.getRelationEntityKey() != null) {
                ClassMetadata relationMetadata = HibernateUtil.getSessionFactory().getClassMetadata(value.getClass());
                if (relationMetadata == null) relationMetadata = HibernateUtil.getSessionFactory().getClassMetadata(field.getRelationEntityKey());
                Object relationId = relationMetadata == null ? null : relationMetadata.getIdentifier(value, EntityMode.POJO);
                row.put(field.getProperty(), relationId);
                row.put(field.getProperty() + "__label", relationLabel(value, relationMetadata,
                        field.getRelationDisplayProperty(), relationId));
            } else {
                if (value instanceof GeneralValueObject) value = String.valueOf(value);
                row.put(field.getProperty(), value);
            }
        }
        return row;
    }
    /** Mengambil label tampilan entitas relasi {@code value} dari properti {@code display}, jatuh kembali ke {@code id} bila label kosong/tidak dapat dibaca. */
    private String relationLabel(Object value, ClassMetadata metadata, String display, Object id) {
        if (metadata != null && display != null) {
            try {
                Object label = metadata.getIdentifierPropertyName().equals(display) ? id
                        : metadata.getPropertyValue(value, display, EntityMode.POJO);
                if (label != null && String.valueOf(label).trim().length() > 0) return String.valueOf(label);
            } catch (Exception ignored) { }
        }
        return id == null ? "" : String.valueOf(id);
    }
    /** Menutup {@code session} dengan aman bila masih terbuka, mengabaikan galat penutupan. */
    private void close(Session session) { try { if (session != null && session.isOpen()) session.close(); } catch (Exception ignored) { } }
}
