package ais.action.master.generic.v2.test;
import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.PembelianWorkflowGenericCrudAdapter;import ais.common.newui.inventory.NewUiPembelianService.LineInput;import ais.database.model.inventory.Pembelian;
/**
 * Harness uji manual (bukan JUnit) untuk {@link PembelianWorkflowGenericCrudAdapter}, adaptor
 * generic-CRUD (paket {@link ais.action.master.generic.v2}) yang membungkus workflow Pembelian
 * (paket inventory) bawaan aplikasi.
 *
 * <p>
 * Memverifikasi bahwa {@link PembelianWorkflowGenericCrudAdapter#configure} secara paksa
 * mematikan create/update/delete/import generik pada {@link GenericCrudDefinition} (workflow
 * Pembelian tetap harus lewat jalur native, bukan CRUD generik langsung ke tabel), bahwa kunci
 * alami ({@code getNaturalKeyProperties()}) memuat {@code kode}, {@code produk}, dan
 * {@code toko}, bahwa entitas {@link Pembelian} terdaftar sebagai "reviewed" di {@link
 * GenericCrudReviewedAdapterFactory}, dan bahwa konstruktor {@code LineInput} menyimpan
 * id produk dan kuantitas dengan benar.
 * </p>
 */
@SuppressWarnings("rawtypes") public final class PembelianWorkflowSelfTest{private PembelianWorkflowSelfTest(){}
	/** Menjalankan seluruh skenario uji konfigurasi adaptor, kunci alami, status reviewed, dan {@code LineInput}. */
	public static void main(String[]a)throws Exception{PembelianWorkflowGenericCrudAdapter x=new PembelianWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(Pembelian.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.contains("kode")&&k.contains("produk")&&k.contains("toko"),"natural key");check(GenericCrudReviewedAdapterFactory.isReviewed(Pembelian.class),"reviewed");LineInput l=new LineInput(Long.valueOf(1),2);check(l.productId.longValue()==1&&l.quantity==2,"line");System.out.println("PembelianWorkflowSelfTest OK");System.exit(0);}/** Melempar {@link IllegalStateException} berisi {@code m} bila {@code v} bernilai {@code false}. */
private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
