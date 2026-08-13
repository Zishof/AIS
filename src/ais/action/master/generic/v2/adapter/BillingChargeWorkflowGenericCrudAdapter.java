package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.Kegiatan;
/** Tagihan is generated/recalculated by KegiatanAction and PembayaranUtil; generic writes would corrupt installments and gateway reconciliation. */
@SuppressWarnings({"rawtypes","unchecked"})public final class BillingChargeWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public BillingChargeWorkflowGenericCrudAdapter(){super(Kegiatan.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Tagihan Mahasiswa","tanggal");}public List getNaturalKeyProperties(){return Arrays.asList("kodeunik");}}
