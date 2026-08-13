package ais.action.master.generic.v2.adapter;
import java.util.ArrayList;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudFieldDefinition;import ais.database.model.rab.PenggunaanAnggaran;
/** Penggunaan anggaran dibentuk dari transaksi sumber; mutasi generic harus fail-closed. */
@SuppressWarnings("rawtypes") public final class PenggunaanAnggaranWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
 public PenggunaanAnggaranWorkflowGenericCrudAdapter(){super(PenggunaanAnggaran.class,false,null,true);}public void configure(GenericCrudDefinition d){d.setDisplayName("Penggunaan Anggaran");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("waktu");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}public List getNaturalKeyProperties(){List x=new ArrayList();x.add("ref");return x;}
}
