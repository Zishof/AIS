package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.BiodataDosen;
/** Biodata dosen is one part of the guarded lecturer workspace and is not a standalone CRUD aggregate. */
@SuppressWarnings({"rawtypes","unchecked"})public final class LecturerBiodataWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public LecturerBiodataWorkflowGenericCrudAdapter(){super(BiodataDosen.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Biodata Dosen","id");d.setDefaultSortAscending(true);}public List getNaturalKeyProperties(){return Arrays.asList("dosen");}}
