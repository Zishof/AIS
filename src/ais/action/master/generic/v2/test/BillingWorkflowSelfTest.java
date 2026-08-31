package ais.action.master.generic.v2.test;
import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.*;import ais.database.model.*;
/**
 * Harness uji manual (dijalankan lewat {@code main}, bukan JUnit) yang memverifikasi tiga adapter
 * generic-CRUD pada alur penagihan/pembayaran — {@link BillingChargeWorkflowGenericCrudAdapter}
 * ({@link Kegiatan}), {@link BillingCartWorkflowGenericCrudAdapter} ({@link KegiatanTemporary}), dan
 * {@link StudentPaymentWorkflowGenericCrudAdapter} ({@link PembayaranMahasiswa}) — masing-masing
 * memaksa mode "fail closed" (create/update/delete/import selalu nonaktif walau diminta aktif),
 * memiliki jumlah kolom kunci alami yang sesuai, dan terdaftar sebagai adapter "reviewed". Kelas
 * final tanpa instance (konstruktor privat kosong).
 */
public final class BillingWorkflowSelfTest{private BillingWorkflowSelfTest(){}
	/**
	 * Menjalankan pengecekan {@link #check} untuk ketiga adapter alur penagihan, lalu keluar dengan
	 * kode 0 bila semua lolos atau melempar {@link IllegalStateException} bila salah satu gagal.
	 *
	 * @param a argumen baris perintah, tidak dipakai
	 * @throws Exception diteruskan apa adanya dari kegagalan konfigurasi adapter
	 */
	public static void main(String[]a)throws Exception{check(new BillingChargeWorkflowGenericCrudAdapter(),Kegiatan.class,1);check(new BillingCartWorkflowGenericCrudAdapter(),KegiatanTemporary.class,4);check(new StudentPaymentWorkflowGenericCrudAdapter(),PembayaranMahasiswa.class,1);System.out.println("BillingWorkflowSelfTest OK");System.exit(0);}
	/**
	 * Mengonfigurasi satu adapter dengan definisi CRUD generik yang meminta semua mutasi + impor
	 * aktif, lalu memastikan adapter tetap memaksanya nonaktif ("fail closed"), jumlah kolom kunci
	 * alami cocok dengan {@code keys}, dan entitas terdaftar sebagai "reviewed".
	 *
	 * @param x    adapter yang diuji
	 * @param c    kelas entitas yang dikonfigurasikan ke adapter
	 * @param keys jumlah kolom kunci alami yang diharapkan
	 * @throws Exception dilempar sebagai {@link IllegalStateException} bila salah satu ekspektasi gagal
	 */
	private static void check(GenericCrudAutoEntityAdapter x,Class c,int keys)throws Exception{GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(c);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);if(d.isCreateEnabled()||d.isUpdateEnabled()||d.isDeleteEnabled()||d.isImportEnabled())throw new IllegalStateException(c+" generic mutation must fail closed");if(x.getNaturalKeyProperties().size()!=keys)throw new IllegalStateException(c+" natural key");if(!GenericCrudReviewedAdapterFactory.isReviewed(c))throw new IllegalStateException(c+" reviewed adapter");}}
