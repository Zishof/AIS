package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.KalenderHariLiburGenericCrudAdapter;import ais.database.model.rab.Kalender;/**
 * Harness uji manual (dijalankan langsung via {@code main}) untuk PARITAS — bukan fail-closed —
 * pada {@link KalenderHariLiburGenericCrudAdapter} untuk entitas {@link Kalender} (hari libur, modul
 * RAB) pada kerangka CRUD generik {@code generic/v2}. Berbeda dari harness identitas/Tagihan yang
 * memverifikasi mutasi generik DIMATIKAN, tes ini memverifikasi CRUD generik justru TETAP AKTIF
 * (create/update/delete semua {@code true} setelah {@code adapter.configure(d)}) karena hari libur
 * adalah data referensi aman untuk dikelola lewat CRUD generik biasa. Juga memeriksa: kolom urut
 * default {@code "tanggal"}, kunci alami tunggal {@code tanggal} (satu baris per tanggal), dan
 * {@link Kalender} terdaftar "reviewed" pada {@link GenericCrudReviewedAdapterFactory}. Melempar
 * {@link IllegalStateException} pada pemeriksaan pertama yang gagal.
 */
@SuppressWarnings("rawtypes")public final class KalenderHariLiburGenericCrudParitySelfTest{private KalenderHariLiburGenericCrudParitySelfTest(){}
	/** Menjalankan seluruh pemeriksaan paritas CRUD generik untuk adapter {@link Kalender}. */
	public static void main(String[]a)throws Exception{KalenderHariLiburGenericCrudAdapter x=new KalenderHariLiburGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(Kalender.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);x.configure(d);check("tanggal".equals(d.getDefaultSortProperty()),"sort");check(d.isCreateEnabled()&&d.isUpdateEnabled()&&d.isDeleteEnabled(),"crud");List k=x.getNaturalKeyProperties();check(k.size()==1&&k.contains("tanggal"),"unique date");check(GenericCrudReviewedAdapterFactory.isReviewed(Kalender.class),"reviewed");System.out.println("KalenderHariLiburGenericCrudParitySelfTest OK");System.exit(0);}
	/** Menegaskan {@code v} bernilai {@code true}; melempar {@link IllegalStateException} berisi {@code m} bila tidak. */
	private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
