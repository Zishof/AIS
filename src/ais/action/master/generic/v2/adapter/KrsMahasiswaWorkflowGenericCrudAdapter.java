package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.KrsMahasiswa;
/** KRS mutations require semester status, quota, prerequisite, finance and approval checks. */
@SuppressWarnings({"rawtypes","unchecked"})public final class KrsMahasiswaWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public KrsMahasiswaWorkflowGenericCrudAdapter(){super(KrsMahasiswa.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"KRS Mahasiswa","semester");}public List getNaturalKeyProperties(){return Arrays.asList("kodeUnik");}}
