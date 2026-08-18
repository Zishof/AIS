package ais.action.master.generic.v2.adapter;
import java.util.Arrays;
import java.util.List;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.database.model.akunting.GrupTransaksi;
/** Journal mutations must pass balance, closing and posting guards in NewUiJournalService. */
@SuppressWarnings({"rawtypes","unchecked"})
public final class JournalWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
    public JournalWorkflowGenericCrudAdapter(){super(GrupTransaksi.class,false,null,true);}
    @Override public void configure(GenericCrudDefinition d){d.setDisplayName("Grup Transaksi / Jurnal Umum");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("tanggalTransaksi");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}
    @Override public List getNaturalKeyProperties(){return Arrays.asList("kode");}
}
