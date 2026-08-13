package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.BadanHukum;

/** Lifecycle native untuk form singleton BadanHukumAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class BadanHukumGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<BadanHukum>
        implements GenericCrudScopeAdapter {

    public BadanHukum createNew(GenericCrudRequestContext context) {
        BadanHukum value = new BadanHukum();
        value.setId(Long.valueOf(1L));
        return value;
    }

    public void validateCreate(Map values, GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    public void validateUpdate(BadanHukum current, Map values,
            GenericCrudRequestContext context, List errors) {
        validate(values, errors);
    }

    private void validate(Map values, List errors) {
        Object kode = values.get("kode");
        if (kode == null || String.valueOf(kode).trim().length() == 0) {
            errors.add("kode:Kode wajib diisi");
        }
    }

    public void beforeSave(Session session, BadanHukum target,
            GenericCrudRequestContext context) throws Exception {
        if (target.getKode() == null || target.getKode().trim().length() == 0) {
            throw new GenericCrudException(400, "BADAN_HUKUM_KODE_REQUIRED", "Kode wajib diisi.");
        }
        if (target.getId() == null) target.setId(Long.valueOf(1L));
        Object existing = session.createCriteria(BadanHukum.class).setMaxResults(1).uniqueResult();
        if (existing instanceof BadanHukum
                && !((BadanHukum) existing).getId().equals(target.getId())) {
            throw new GenericCrudException(409, "BADAN_HUKUM_SINGLETON",
                    "Data badan hukum hanya boleh mempunyai satu record.");
        }
    }

    public boolean canDelete(BadanHukum target, GenericCrudRequestContext context, List reasons) {
        reasons.add("Data badan hukum adalah konfigurasi singleton dan tidak boleh dihapus.");
        return false;
    }

    public List getNaturalKeyProperties() {
        List result = new ArrayList();
        result.add("kode");
        return result;
    }

    /** Badan hukum merupakan konfigurasi institusi global. */
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(ais.database.model.GeneralValueObject object,
            GenericCrudRequestContext context) { }
}
