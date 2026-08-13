package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.Dosen;
/** Lecturer persistence is an aggregate workflow: identity, account, photo, homebase, history, access, and Feeder synchronization. */
@SuppressWarnings({"rawtypes","unchecked"})public final class LecturerIdentityWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public LecturerIdentityWorkflowGenericCrudAdapter(){super(Dosen.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Dosen","nama");d.setDefaultSortAscending(true);}public List getNaturalKeyProperties(){return Arrays.asList("code");}}
