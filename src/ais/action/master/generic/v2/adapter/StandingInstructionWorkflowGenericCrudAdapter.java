package ais.action.master.generic.v2.adapter;import java.util.ArrayList;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.GenericCrudFieldDefinition;import ais.database.model.akunting.StandingInstruction;/**
 * Adapter CRUD generik (paket {@code generic/v2}, framework action-layer generik) untuk entitas
 * {@link StandingInstruction} (instruksi pembayaran berulang/standing instruction akunting).
 * Entitas ini bersifat murni <b>read-only</b>: seluruh field data dibentuk/diubah lewat proses
 * workflow lain di luar layar CRUD generik (kemungkinan job terjadwal atau proses akunting
 * tersendiri), sehingga layar generik hanya dipakai untuk menampilkan/menelusuri data, bukan
 * mengedit — {@code configure} secara eksplisit mematikan create/update/delete/import dan
 * menandai {@link GenericCrudDefinition#READ_ONLY}, serta mengunci semua field agar tidak bisa
 * dibuat/diubah lewat form generik.
 */
@SuppressWarnings("rawtypes")public final class StandingInstructionWorkflowGenericCrudAdapter extends GenericCrudAutoEntityAdapter{
	/** Mendaftarkan {@link StandingInstruction} ke superclass sebagai entitas non-lazy dengan dukungan impor dinonaktifkan lewat konstruktor. */
	public StandingInstructionWorkflowGenericCrudAdapter(){super(StandingInstruction.class,false,null,true);}
	/** Mengonfigurasi definisi CRUD generik untuk {@link StandingInstruction} menjadi read-only penuh: menonaktifkan create/update/delete/import, mengunci setiap field, dan mengatur urutan default berdasarkan id menurun. */
	public void configure(GenericCrudDefinition d){d.setDisplayName("Standing Instruction");d.setLifecycleStatus(GenericCrudDefinition.READ_ONLY);d.setCreateEnabled(false);d.setUpdateEnabled(false);d.setDeleteEnabled(false);d.setImportEnabled(false);d.setDefaultSortProperty("id");d.setDefaultSortAscending(false);for(Object o:d.getFields()){GenericCrudFieldDefinition f=(GenericCrudFieldDefinition)o;f.setCreateable(false);f.setUpdateable(false);}}/** Kunci alami entitas: kombinasi {@code kodeUnik} dan {@code kode}, dipakai untuk pencocokan/impor data. */
public List getNaturalKeyProperties(){List v=new ArrayList();v.add("kodeUnik");v.add("kode");return v;}}
