package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.rab.Tugas;
/** RAB tasks form a recursive revision tree; copies and revision numbers are produced by RabUtil as one workflow. */
@SuppressWarnings({"rawtypes","unchecked"})public final class RabTaskRevisionWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public RabTaskRevisionWorkflowGenericCrudAdapter(){super(Tugas.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Tugas RAB","revisi");}public List getNaturalKeyProperties(){return Arrays.asList("proyek","revisi","parent","nama");}}
