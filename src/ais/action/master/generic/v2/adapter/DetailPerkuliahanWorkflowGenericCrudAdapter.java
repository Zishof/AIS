package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.Detailperkuliahan;
/** Enrollment/grade mutations require KRS, verification, payment, prerequisite and Feeder guards. */
@SuppressWarnings({"rawtypes","unchecked"})public final class DetailPerkuliahanWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public DetailPerkuliahanWorkflowGenericCrudAdapter(){super(Detailperkuliahan.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Peserta & Nilai Perkuliahan","id");}public List getNaturalKeyProperties(){return Arrays.asList("mahasiswa","perkuliahan");}}
