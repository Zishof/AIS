package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.database.model.employ.Pensiun;

/**
 * Tabel employ.pensiun bukan entity yang dikelola PensiunAction; cegah mutasi
 * generik sampai workflow pengajuan khusus mempunyai lifecycle native sendiri.
 */
@SuppressWarnings({ "rawtypes" })
public final class PensiunRecordGenericCrudAdapter extends GenericCrudAutoEntityAdapter {
    public PensiunRecordGenericCrudAdapter() { super(Pensiun.class, false, null, true); }

    public void configure(GenericCrudDefinition definition) {
        definition.setDisplayName("Rekam Pengajuan Pensiun");
        definition.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);
        definition.setCreateEnabled(false); definition.setUpdateEnabled(false);
        definition.setDeleteEnabled(false); definition.setImportEnabled(false);
        definition.setDefaultSortProperty("tanggal_dirubah"); definition.setDefaultSortAscending(false);
        List fields = definition.getFields();
        for (int i = 0; i < fields.size(); i++) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.get(i);
            field.setCreateable(false); field.setUpdateable(false);
        }
    }

    public List getNaturalKeyProperties() { return new ArrayList(); }
}
