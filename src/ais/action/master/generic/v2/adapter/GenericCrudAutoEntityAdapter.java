package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/**
 * Adapter generik untuk model scalar yang sudah terdaftar pada Hibernate.
 * Auto-adapter sengaja hanya dapat dipakai Super Admin; entity dengan business
 * rule khusus tetap harus menggantinya dengan adapter eksplisit.
 */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public class GenericCrudAutoEntityAdapter extends AbstractGenericCrudEntityAdapter<GeneralValueObject>
        implements GenericCrudScopeAdapter {
    private final Class entityClass;
    private final boolean softDelete;

    public GenericCrudAutoEntityAdapter(Class entityClass, boolean softDelete) {
        this.entityClass = entityClass;
        this.softDelete = softDelete;
    }

    public GeneralValueObject createNew(GenericCrudRequestContext context) throws Exception {
        requireSuperAdmin();
        Object value = entityClass.newInstance();
        if (!(value instanceof GeneralValueObject)) {
            throw new GenericCrudException(409, "AUTO_ENTITY_INVALID", "Model auto-CRUD bukan GeneralValueObject.");
        }
        return (GeneralValueObject) value;
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
