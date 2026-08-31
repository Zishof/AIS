package ais.action.master.generic.v2.test;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.TransferRequestWorkflowGenericCrudAdapter;
import ais.database.model.akunting.DaftarPengajuanTransfer;
/**
 * Harness uji manual untuk {@link TransferRequestWorkflowGenericCrudAdapter} (adapter CRUD generik
 * entitas pengajuan transfer dana antar unit kerja, {@link DaftarPengajuanTransfer}). Mengikuti
 * pola self-test adapter generik lain: memaksa seluruh flag mutasi aktif lebih dulu, memanggil
 * {@code configure}, lalu memverifikasi semuanya berbalik nonaktif (fail-closed) — pengajuan
 * transfer harus melalui alur approval khusus, bukan CRUD generik langsung. Juga memeriksa kunci
 * alami ({@code "kodeUnik"}) dan status "reviewed" entitas di
 * {@link GenericCrudReviewedAdapterFactory}. Kegagalan melempar {@link IllegalStateException};
 * sukses mencetak "TransferRequestWorkflowSelfTest OK".
 */
public final class TransferRequestWorkflowSelfTest{private TransferRequestWorkflowSelfTest(){}
	/** Menjalankan pemeriksaan fail-closed dan kunci alami untuk adapter pengajuan transfer; lihat javadoc kelas. */
	public static void main(String[]a){TransferRequestWorkflowGenericCrudAdapter x=new TransferRequestWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(DaftarPengajuanTransfer.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"generic mutations must fail closed");check("kodeUnik".equals(x.getNaturalKeyProperties().get(0)),"natural key");check(GenericCrudReviewedAdapterFactory.isReviewed(DaftarPengajuanTransfer.class),"reviewed adapter");System.out.println("TransferRequestWorkflowSelfTest OK");System.exit(0);}
	/** Melempar {@link IllegalStateException} berpesan {@code m} bila {@code v} bernilai {@code false}. */
	private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
