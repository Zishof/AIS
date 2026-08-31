package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.TagihanWorkflowGenericCrudAdapter;import ais.database.model.sekolah.Tagihan;/**
 * Harness uji manual (dijalankan langsung via {@code main}) yang memverifikasi PENGAMANAN
 * "fail-closed" pada {@link TagihanWorkflowGenericCrudAdapter} untuk entitas {@link Tagihan}
 * (modul sekolah) pada kerangka CRUD generik {@code generic/v2} — pola sama seperti
 * {@code IdentityWorkflowSelfTest}. Memastikan: (1) meski {@link GenericCrudDefinition} diawali
 * dengan create/update/delete/import semua diaktifkan, {@code adapter.configure(d)} harus
 * mematikan seluruhnya (Tagihan hanya boleh dimutasi lewat alur native/khusus, bukan CRUD generik);
 * (2) kunci alami (natural key) mencakup {@code kodeUnik}, {@code tahunbulan}, dan {@code bayarKe};
 * (3) {@link Tagihan} terdaftar "reviewed" pada {@link GenericCrudReviewedAdapterFactory}. Melempar
 * {@link IllegalStateException} pada pemeriksaan pertama yang gagal.
 */
@SuppressWarnings("rawtypes")public final class TagihanWorkflowSelfTest{private TagihanWorkflowSelfTest(){}
	/** Menjalankan seluruh pemeriksaan fail-closed untuk adapter {@link Tagihan}. */
	public static void main(String[]a){TagihanWorkflowGenericCrudAdapter x=new TagihanWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(Tagihan.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.contains("kodeUnik")&&k.contains("tahunbulan")&&k.contains("bayarKe"),"keys");check(GenericCrudReviewedAdapterFactory.isReviewed(Tagihan.class),"reviewed");System.out.println("TagihanWorkflowSelfTest OK");System.exit(0);}
	/** Menegaskan {@code v} bernilai {@code true}; melempar {@link IllegalStateException} berisi {@code m} bila tidak. */
	private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
