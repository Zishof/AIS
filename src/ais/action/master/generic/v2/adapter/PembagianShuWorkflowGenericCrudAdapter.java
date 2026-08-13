package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.koperasi.Koperasi;
import ais.database.model.koperasi.PembagianShu;

/**
 * Model kepala SHU hanya dibaca oleh Generic CRUD. Mutasi dilakukan workflow
 * NewUiPembagianShuService agar perhitungan seluruh ShuAnggota selalu atomik.
 */
@SuppressWarnings({ "rawtypes" })
public final class PembagianShuWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public PembagianShuWorkflowGenericCrudAdapter() { super(PembagianShu.class, false, null, true); }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Pembagian SHU");
        definition.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
        definition.setCreateEnabled(false); definition.setUpdateEnabled(false);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("tahun"); definition.setDefaultSortAscending(false);
        definition.setDefaultPageSize(25);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable(false);
        }
    }

    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyCooperativeScope(criteria, context == null ? null : context.getUser());
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyCooperativeScope(criteria, context == null ? null : context.getUser());
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (!(object instanceof PembagianShu))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Pembagian SHU tidak valid.");
        Koperasi cooperative = cooperative(context == null ? null : context.getUser());
        if (ais.common.Common.getApakahAdmin() || cooperative == null) return;
        if (!cooperative.equals(((PembagianShu) object).getKoperasi()))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Pembagian SHU bukan milik koperasi aktif.");
    }
    public List getNaturalKeyProperties() { List values = new ArrayList(); values.add("tahun"); values.add("koperasi"); return values; }

    private void applyCooperativeScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin()) return;
        Koperasi value = cooperative(user);
        if (value == null) criteria.add(Restrictions.sqlRestriction("1=0"));
        else criteria.add(Restrictions.eq("koperasi", value));
    }
    private Koperasi cooperative(Tbmuser user) {
        try {
            if (user != null && user.getAnggotaKoperasi() != null) return user.getAnggotaKoperasi().getKoperasi();
        } catch (Exception ignored) { }
        try { return ais.common.Common.getCurrentKoperasi(); }
        catch (Exception ignored) { return null; }
    }
}
