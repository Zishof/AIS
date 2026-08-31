package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.WorkRealizationWorkflowGenericCrudAdapter;import ais.database.model.lkp.RealisasiKerjaPegawai;/**
 * Harness uji manual (dijalankan lewat {@code main}) untuk memverifikasi kontrak
 * {@link WorkRealizationWorkflowGenericCrudAdapter}: memastikan adapter ini memaksa entitas
 * {@link RealisasiKerjaPegawai} menjadi <b>native-only</b> (mematikan paksa
 * create/update/delete/import walau di-set aktif secara eksplisit pada
 * {@link GenericCrudDefinition} sebelum {@code configure} dipanggil), memiliki tepat 3 kolom
 * kunci alami ({@code getNaturalKeyProperties}), dan entitasnya sudah terdaftar sebagai
 * "reviewed" di {@link GenericCrudReviewedAdapterFactory}.
 */
@SuppressWarnings("rawtypes")public final class WorkRealizationWorkflowSelfTest{private WorkRealizationWorkflowSelfTest(){}
	/** Menjalankan validasi kontrak adapter realisasi kerja pegawai; keluar dengan kode 0 bila seluruh pemeriksaan lolos. */
	public static void main(String[]a){WorkRealizationWorkflowGenericCrudAdapter x=new WorkRealizationWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(RealisasiKerjaPegawai.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.size()==3,"natural keys");check(GenericCrudReviewedAdapterFactory.isReviewed(RealisasiKerjaPegawai.class),"reviewed");System.out.println("WorkRealizationWorkflowSelfTest OK");System.exit(0);}/** Melempar {@link IllegalStateException} berisi pesan {@code m} bila {@code v} bernilai false. */
private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
