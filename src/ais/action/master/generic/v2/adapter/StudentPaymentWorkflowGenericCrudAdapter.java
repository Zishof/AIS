package ais.action.master.generic.v2.adapter;
import java.util.Arrays;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.database.model.PembayaranMahasiswa;
/** Payment rows share the kegiatan table and are written only after validation, accounting, scholarship, and gateway checks. */
@SuppressWarnings({"rawtypes","unchecked"})public final class StudentPaymentWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{public StudentPaymentWorkflowGenericCrudAdapter(){super(PembayaranMahasiswa.class,false,null,true);}public void configure(GenericCrudDefinition d){PerkuliahanWorkflowGenericCrudAdapter.lock(d,"Pembayaran Mahasiswa","tanggal");}public List getNaturalKeyProperties(){return Arrays.asList("kodeunik");}}
