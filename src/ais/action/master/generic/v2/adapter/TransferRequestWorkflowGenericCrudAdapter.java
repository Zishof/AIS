package ais.action.master.generic.v2.adapter;
import java.util.Arrays;
import java.util.List;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.database.model.akunting.DaftarPengajuanTransfer;
/** DPT is generated from approved source documents and mutated only by its guarded workflow. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class TransferRequestWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
    public TransferRequestWorkflowGenericCrudAdapter(){super(DaftarPengajuanTransfer.class,false,null,true);}
    @Override public void configure(GenericCrudDefinition d){d.setDisplayName("Daftar Pengajuan Transfer");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("waktu");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
    @Override public List getNaturalKeyProperties(){return Arrays.asList("kodeUnik");}
}
