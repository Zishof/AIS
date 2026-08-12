package ais.action.master.generic.v2.adapter;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudValueConverter;
import ais.common.Common;
import ais.common.HeadlessBusinessRuleException;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Adapter generik untuk model scalar yang sudah terdaftar pada Hibernate.
 * Adapter mengikuti privilege menu dan otomatis membatasi relasi institusi atau
 * pemilik yang tersedia pada pengguna aktif. Business rule khusus tetap dapat
 * menggantinya dengan adapter eksplisit.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudAutoEntityAdapter extends AbstractGenericCrudEntityAdapter<GeneralValueObject>
        implements GenericCrudScopeAdapter, GenericCrudSessionValueAdapter {
    private final Class entityClass;
    private final boolean softDelete;
    private final Class sourceActionClass;
    private final boolean metadataLifecycle;

    public GenericCrudAutoEntityAdapter(Class entityClass, boolean softDelete) {
        this(entityClass, softDelete, null, false);
    }

    public GenericCrudAutoEntityAdapter(Class entityClass, boolean softDelete, Class sourceActionClass) {
        this(entityClass, softDelete, sourceActionClass, false);
    }

    public GenericCrudAutoEntityAdapter(Class entityClass, boolean softDelete, Class sourceActionClass,
            boolean metadataLifecycle) {
        this.entityClass = entityClass;
        this.softDelete = softDelete;
        this.sourceActionClass = sourceActionClass;
        this.metadataLifecycle = metadataLifecycle;
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        authorize(context);
        Constructor constructor = entityClass.getDeclaredConstructor(new Class[0]);
        if (!constructor.isAccessible()) constructor.setAccessible(true);
        Object value = constructor.newInstance(new Object[0]);
        if (!(value instanceof GeneralValueObject)) {
            throw new GenericCrudException(409, "AUTO_ENTITY_INVALID", "Model auto-CRUD bukan GeneralValueObject.");
        }
        return (GeneralValueObject) value;
    }

    public void applyCreateValues(Session session, GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception {
        authorize(context);
        applyContextDefaults(target, values, context);
        applyWithSession(session, target, values);
    }

    public void applyUpdateValues(Session session, GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception {
        authorize(context);
        applyWithSession(session, target, values);
    }

    /**
     * Sumber kebenaran mutasi adalah Action existing. Entity yang sudah diisi
     * dari request New UI diteruskan ke form builder dan onSave milik Action,
     * sehingga validasi, duplicate check, default, dan efek simpan tidak disalin.
     */
    public void beforeSave(Session session, GeneralValueObject target,
            GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (sourceActionClass == null) {
            if (metadataLifecycle) return;
            throw new GenericCrudException(501, "EXISTING_ACTION_NOT_BOUND",
                    "Operasi perubahan belum diaktifkan karena lifecycle Action existing belum terhubung.");
        }
        try {
            GenericCrudExistingActionInvoker.execute(sourceActionClass, target);
        } catch (HeadlessBusinessRuleException rejected) {
            throw new GenericCrudException(400, "EXISTING_ACTION_VALIDATION", rejected.getMessage(), rejected);
        } catch (GenericCrudException known) {
            throw known;
        } catch (Exception failure) {
            throw new GenericCrudException(500, "EXISTING_ACTION_FAILED",
                    "Lifecycle Action existing gagal dijalankan dan transaksi dibatalkan.", failure);
        }
    }

    private void applyWithSession(Session session, GeneralValueObject target, Map values) throws Exception {
        ClassMetadata ownerMetadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        Map scalarValues = new LinkedHashMap();
        Iterator entries = values.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            String property = String.valueOf(entry.getKey());
            if (property.equals(ownerMetadata.getIdentifierPropertyName())) {
                Object convertedId = GenericCrudValueConverter.convert(entry.getValue(),
                        ownerMetadata.getIdentifierType().getReturnedClass());
                if (!(convertedId instanceof java.io.Serializable)) {
                    throw new GenericCrudException(400, "INVALID_ID", "Identifier model tidak serializable.");
                }
                ownerMetadata.setIdentifier(target, (java.io.Serializable) convertedId, EntityMode.POJO);
                continue;
            }
            Type type = ownerMetadata.getPropertyType(property);
            Class returned = type.getReturnedClass();
            if (!type.isAssociationType() || !GeneralValueObject.class.isAssignableFrom(returned)) {
                scalarValues.put(property, entry.getValue());
                continue;
            }
            Object raw = entry.getValue();
            Object relation = null;
            if (raw != null && String.valueOf(raw).trim().length() > 0) {
                ClassMetadata relationMetadata = HibernateUtil.getSessionFactory().getClassMetadata(returned);
                if (relationMetadata == null) {
                    throw new GenericCrudException(400, "RELATION_NOT_MAPPED", "Relasi " + property + " tidak terdaftar pada Hibernate.");
                }
                Object convertedId = GenericCrudValueConverter.convert(raw,
                        relationMetadata.getIdentifierType().getReturnedClass());
                relation = session.get(returned, (java.io.Serializable) convertedId);
                if (relation == null) {
                    throw new GenericCrudException(400, "RELATION_NOT_FOUND", "Pilihan " + property + " tidak ditemukan.");
                }
            }
            ownerMetadata.setPropertyValue(target, property, relation, EntityMode.POJO);
        }
        super.applyCreateValues(target, scalarValues, null);
    }

    /**
     * Mengisi kolom audit internal yang sengaja tidak ditampilkan pada form generik.
     * Tanpa ini model inventori, aset, perpustakaan, dan SIRS akan gagal pada
     * constraint {@code dibuat_oleh} walaupun request berasal dari user sah.
     */
    private void applyContextDefaults(GeneralValueObject target, Map values,
            GenericCrudRequestContext context) {
        if (context == null || context.getUser() == null) return;
        Tbmuser user = context.getUser();
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        setDefault(metadata, target, values, "dibuatOleh", user);
        setDefault(metadata, target, values, "diubahOleh", user);
        setDefault(metadata, target, values, "validatorUser", user);
        setDefault(metadata, target, values, "oleh", user.getUserId());
        setDefault(metadata, target, values, "olehId", user.getUserId());
        setDefault(metadata, target, values, "ditetapkanOleh", user.getUserId());
        Map scopes = scopeBindings(context);
        Iterator scoped = scopes.entrySet().iterator();
        while (scoped.hasNext()) {
            Map.Entry entry = (Map.Entry) scoped.next();
            setDefault(metadata, target, values, String.valueOf(entry.getKey()), entry.getValue());
        }
    }

    private void setDefault(ClassMetadata metadata, GeneralValueObject target, Map values,
            String property, Object value) {
        if (value == null || values.containsKey(property)) return;
        try {
            Class expected = metadata.getPropertyType(property).getReturnedClass();
            if (!expected.isAssignableFrom(value.getClass())) return;
            Object current = metadata.getPropertyValue(target, property, EntityMode.POJO);
            if (current == null || current instanceof String && String.valueOf(current).trim().length() == 0) {
                metadata.setPropertyValue(target, property, value, EntityMode.POJO);
            }
        } catch (Exception missingOrDerivedProperty) {
            // Model tidak memiliki property audit tersebut atau getternya merupakan nilai turunan.
        }
    }

    public boolean canDelete(GeneralValueObject target, GenericCrudRequestContext context, List reasons) {
        if (!softDelete) {
            reasons.add("Model tidak mempunyai field aktif untuk soft delete.");
            return false;
        }
        return true;
    }

    public void delete(Session session, GeneralValueObject target, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        metadata.setPropertyValue(target, "aktif", Boolean.FALSE, EntityMode.POJO);
        session.saveOrUpdate(target);
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        addStringProperty(metadata, result, "kode");
        addStringProperty(metadata, result, "nama");
        return result;
    }

    private void addStringProperty(ClassMetadata metadata, List result, String property) {
        try {
            Type type = metadata.getPropertyType(property);
            if (type != null && type.getReturnedClass() == String.class) result.add(property);
        } catch (Exception ignored) { }
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        authorize(context); applyScope(criteria, context);
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception {
        authorize(context); applyScope(criteria, context);
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (object == null || Common.getApakahAdmin()) return;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        Iterator entries = scopeBindings(context).entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            Object actual = metadata.getPropertyValue(object, String.valueOf(entry.getKey()), EntityMode.POJO);
            if (!sameEntity(actual, entry.getValue())) {
                throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE",
                        "Data berada di luar institusi atau kepemilikan role aktif.");
            }
        }
    }

    private void applyScope(Criteria criteria, GenericCrudRequestContext context) {
        GenericCrudInstitutionScope.apply(criteria, entityClass,
                context == null ? null : context.getUser());
    }

    private Map scopeBindings(GenericCrudRequestContext context) {
        return GenericCrudInstitutionScope.bindings(entityClass,
                context == null ? null : context.getUser());
    }

    private boolean sameEntity(Object one, Object two) {
        if (one == two) return true;
        if (one == null || two == null) return false;
        try {
            ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(one.getClass());
            if (metadata != null) {
                Object oneId = metadata.getIdentifier(one, EntityMode.POJO);
                Object twoId = metadata.getIdentifier(two, EntityMode.POJO);
                return oneId != null && oneId.equals(twoId);
            }
        } catch (Exception ignored) { }
        return one.equals(two);
    }

    protected void authorize(GenericCrudRequestContext context) throws GenericCrudException {
        if (context == null || context.getUser() == null || !context.isCanRead()) {
            throw new GenericCrudException(403, "AUTO_CRUD_READ_FORBIDDEN",
                    "Role aktif tidak memiliki hak READ untuk menu Generic CRUD ini.");
        }
    }
}
