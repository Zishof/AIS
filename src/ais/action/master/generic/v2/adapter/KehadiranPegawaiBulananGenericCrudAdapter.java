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
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;

/** Laporan bulanan existing bersifat read/search/export, bukan sumber mutasi. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class KehadiranPegawaiBulananGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public KehadiranPegawaiBulananGenericCrudAdapter() {
        super(KehadiranPegawaiBulanan.class, false, null, true);
    }
    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Kehadiran Pegawai Bulanan");
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
    public void applyDefaultFilters(Criteria criteria, GenericCrudRequestContext context) {
        criteria.add(Restrictions.isNotNull("pegawai"));
    }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) {
        applyEmployeeScope(criteria, context == null ? null : context.getUser());
    }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) {
        applyEmployeeScope(criteria, context == null ? null : context.getUser());
    }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) throws Exception {
        authorize(context);
        if (!(object instanceof KehadiranPegawaiBulanan))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Ringkasan kehadiran tidak valid.");
        Tbmuser user = context.getUser(); Pegawai employee = user.getPegawai();
        if (ais.common.Common.getApakahAdmin() || employee == null) return;
        if (!employee.equals(((KehadiranPegawaiBulanan) object).getPegawai()))
            throw new GenericCrudException(403, "OBJECT_OUTSIDE_SCOPE", "Ringkasan bukan milik pegawai aktif.");
    }
    public List getNaturalKeyProperties() { return new ArrayList(); }
    private void applyEmployeeScope(Criteria criteria, Tbmuser user) {
        if (criteria == null) return;
        if (user == null) { criteria.add(Restrictions.sqlRestriction("1=0")); return; }
        if (ais.common.Common.getApakahAdmin()) return;
        Pegawai employee = user.getPegawai();
        if (employee != null && (user.hakAkses() == null || !Boolean.TRUE.equals(user.hakAkses().getMelihatDataPegawaiLain()))) {
            criteria.add(Restrictions.eq("pegawai", employee)); return;
        }
        if (user.getSatuanKerja() != null) {
            criteria.createAlias("pegawai", "scopePegawai")
                    .add(Restrictions.eq("scopePegawai.satuanKerja", user.getSatuanKerja()));
        }
    }
}
