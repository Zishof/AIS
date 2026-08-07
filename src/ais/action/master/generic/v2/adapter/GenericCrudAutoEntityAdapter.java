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
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;

/**
 * Adapter generik untuk model scalar yang sudah terdaftar pada Hibernate.
 * Auto-adapter sengaja hanya dapat dipakai Super Admin; entity dengan business
 * rule khusus tetap harus menggantinya dengan adapter eksplisit.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudAutoEntityAdapter extends AbstractGenericCrudEntityAdapter<GeneralValueObject>
        implements GenericCrudScopeAdapter, GenericCrudSessionValueAdapter {
    private final Class entityClass;
    private final boolean softDelete;

    public GenericCrudAutoEntityAdapter(Class entityClass, boolean softDelete) {
        this.entityClass = entityClass;
        this.softDelete = softDelete;
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        requireSuperAdmin();
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
        applyContextDefaults(target, values, context);
        applyWithSession(session, target, values);
    }

    public void applyUpdateValues(Session session, GeneralValueObject target, Map values,
            GenericCrudRequestContext context) throws Exception {
        applyWithSession(session, target, values);
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
        requireSuperAdmin();
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

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) throws Exception { requireSuperAdmin(); }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) throws Exception { requireSuperAdmin(); }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception { requireSuperAdmin(); }

    private void requireSuperAdmin() throws GenericCrudException {
        if (!Common.getApakahAdmin()) {
            throw new GenericCrudException(403, "AUTO_CRUD_SUPER_ADMIN_REQUIRED",
                    "Auto-CRUD seluruh model hanya tersedia untuk Super Admin. Gunakan adapter scope eksplisit untuk role lain.");
        }
    }
}
