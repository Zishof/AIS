package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.StudentArrearsWorkflowGenericCrudAdapter;import ais.database.model.TunggakanMahasiswa;/**
 * Harness uji manual untuk adapter {@link StudentArrearsWorkflowGenericCrudAdapter} pada
 * framework CRUD generik: memverifikasi entitas {@link TunggakanMahasiswa} tertutup rapat dari
 * mutasi generik (create/update/delete/import harus gagal-tertutup meski diminta aktif), jumlah
 * kolom kunci alami sesuai ekspektasi (4), dan adapter terdaftar sebagai adapter yang sudah
 * ditinjau lewat {@link GenericCrudReviewedAdapterFactory#isReviewed(Class)}.
 */
@SuppressWarnings("rawtypes")public final class StudentArrearsWorkflowSelfTest{private StudentArrearsWorkflowSelfTest(){}
	/** Menjalankan seluruh pemeriksaan; melempar {@link IllegalStateException} bila ada yang gagal. */
	public static void main(String[]a){StudentArrearsWorkflowGenericCrudAdapter x=new StudentArrearsWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(TunggakanMahasiswa.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.size()==4,"keys");check(GenericCrudReviewedAdapterFactory.isReviewed(TunggakanMahasiswa.class),"reviewed");System.out.println("StudentArrearsWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
