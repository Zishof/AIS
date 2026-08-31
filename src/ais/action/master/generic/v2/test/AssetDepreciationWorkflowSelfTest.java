package ais.action.master.generic.v2.test;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.AssetDepreciationWorkflowGenericCrudAdapter;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.database.model.asset.AssetDetail;
/**
 * Harness uji manual (bukan JUnit) untuk {@link AssetDepreciationWorkflowGenericCrudAdapter},
 * adaptor generic-CRUD yang membungkus workflow penyusutan aset ({@link AssetDetail}).
 *
 * <p>
 * Memverifikasi bahwa {@code configure} memaksa {@link GenericCrudDefinition} menutup
 * create/update/delete/import generik (workflow penyusutan aset wajib fail-closed lewat jalur
 * native, bukan CRUD generik), bahwa kunci alami hanya terdiri dari satu properti (barcode aset),
 * dan bahwa {@link AssetDetail} terdaftar sebagai "reviewed" di
 * {@link GenericCrudReviewedAdapterFactory}.
 * </p>
 */
public final class AssetDepreciationWorkflowSelfTest{private AssetDepreciationWorkflowSelfTest(){}
	/** Menjalankan seluruh skenario uji konfigurasi fail-closed, kunci alami, dan status reviewed. */
	public static void main(String[]a){AssetDepreciationWorkflowGenericCrudAdapter x=new AssetDepreciationWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(AssetDetail.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native workflow must fail closed");check(x.getNaturalKeyProperties().size()==1,"barcode key");check(GenericCrudReviewedAdapterFactory.isReviewed(AssetDetail.class),"reviewed factory");System.out.println("AssetDepreciationWorkflowSelfTest OK");System.exit(0);}/** Melempar {@link IllegalStateException} berisi {@code m} bila {@code v} bernilai {@code false}. */
private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
