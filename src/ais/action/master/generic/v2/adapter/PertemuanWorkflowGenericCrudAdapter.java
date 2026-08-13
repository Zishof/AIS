package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.Pertemuan;
/** Meeting mutations require timetable, attendance, materials, exams, discussions and online-class orchestration. */
@SuppressWarnings({"rawtypes","unchecked"})public final class PertemuanWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public PertemuanWorkflowGenericCrudAdapter(){super(Pertemuan.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Pertemuan Pembelajaran","tanggal");}public List getNaturalKeyProperties(){return Arrays.asList("perkuliahan","pertemuanKe");}}
