package ais.action.master.generic.v2.test;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.InstallmentPaymentWorkflowGenericCrudAdapter;import ais.database.model.CicilanPembayaran;/**
 * Harness uji manual untuk adapter {@link InstallmentPaymentWorkflowGenericCrudAdapter} pada
 * framework CRUD generik: memverifikasi entitas {@link CicilanPembayaran} gagal-tertutup terhadap
 * mutasi generik (create/update/delete/import), jumlah kolom kunci alami sesuai ekspektasi (4),
 * dan adapter terdaftar sebagai adapter yang sudah ditinjau lewat
 * {@link GenericCrudReviewedAdapterFactory#isReviewed(Class)}.
 */
public final class InstallmentPaymentWorkflowSelfTest{private InstallmentPaymentWorkflowSelfTest(){}
	/** Menjalankan seluruh pemeriksaan; melempar {@link IllegalStateException} bila ada yang gagal. */
	public static void main(String[]a){InstallmentPaymentWorkflowGenericCrudAdapter x=new InstallmentPaymentWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(CicilanPembayaran.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"fail closed");check(x.getNaturalKeyProperties().size()==4,"natural key");check(GenericCrudReviewedAdapterFactory.isReviewed(CicilanPembayaran.class),"reviewed");System.out.println("InstallmentPaymentWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
