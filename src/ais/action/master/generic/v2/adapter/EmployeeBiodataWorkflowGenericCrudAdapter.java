package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.BiodataPegawai;
/** Biodata pegawai belongs to the employee/account/photo aggregate and remains mutation-guarded. */
@SuppressWarnings({"rawtypes","unchecked"})public final class EmployeeBiodataWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public EmployeeBiodataWorkflowGenericCrudAdapter(){super(BiodataPegawai.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Biodata Pegawai","id");d.setDefaultSortAscending(true);}public List getNaturalKeyProperties(){return Arrays.asList("pegawai");}}
