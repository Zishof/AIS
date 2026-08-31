package ais.action.master.generic.v2.test;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.JournalWorkflowGenericCrudAdapter;
import ais.database.model.akunting.GrupTransaksi;
/**
 * Harness uji manual untuk {@link JournalWorkflowGenericCrudAdapter} (adapter CRUD generik entitas
 * jurnal akunting {@link GrupTransaksi}). Sama seperti pola self-test adapter generik lain di
 * paket ini: membuat definisi dengan seluruh mutasi (create/update/delete/import) diaktifkan
 * lebih dulu, memanggil {@code configure}, lalu memastikan semuanya berbalik nonaktif — jurnal
 * akunting tidak boleh diubah lewat jalur CRUD generik karena harus melalui alur posting/approval
 * khusus. Juga memverifikasi kunci alami tunggal ({@code "kode"}) dan status "reviewed" entitas di
 * {@link GenericCrudReviewedAdapterFactory}. Kegagalan melempar {@link IllegalStateException};
 * sukses mencetak "JournalWorkflowSelfTest OK".
 */
public final class JournalWorkflowSelfTest{private JournalWorkflowSelfTest(){}
	/** Menjalankan pemeriksaan fail-closed dan kunci alami untuk adapter jurnal; lihat javadoc kelas. */
	public static void main(String[]a){JournalWorkflowGenericCrudAdapter x=new JournalWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(GrupTransaksi.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"generic mutation must fail closed");check(x.getNaturalKeyProperties().size()==1&&"kode".equals(x.getNaturalKeyProperties().get(0)),"journal natural key");check(GenericCrudReviewedAdapterFactory.isReviewed(GrupTransaksi.class),"reviewed journal adapter");System.out.println("JournalWorkflowSelfTest OK");System.exit(0);}
	/** Melempar {@link IllegalStateException} berpesan {@code m} bila {@code v} bernilai {@code false}. */
	private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
