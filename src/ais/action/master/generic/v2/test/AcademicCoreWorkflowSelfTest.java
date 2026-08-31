package ais.action.master.generic.v2.test;
import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.*;import ais.database.model.*;
/**
 * Harness uji manual untuk empat adapter CRUD generik inti akademik: {@code Perkuliahan},
 * {@code Detailperkuliahan}, {@code Pertemuan}, dan {@code KrsMahasiswa}. Untuk masing-masing
 * entitas, harness ini SENGAJA membuat {@link GenericCrudDefinition} dengan create/update/delete/
 * import diaktifkan penuh terlebih dahulu, lalu memanggil {@code configure} milik adapter dan
 * memastikan seluruh flag mutasi tersebut berbalik menjadi nonaktif ("fail closed") — memverifikasi
 * bahwa adapter data akademik inti ini memaksa jalur generik menjadi read-only walau diminta
 * sebaliknya. Juga memeriksa jumlah properti kunci alami tiap adapter dan bahwa kelas entitasnya
 * terdaftar sebagai "reviewed" di {@link GenericCrudReviewedAdapterFactory}. Kegagalan pemeriksaan
 * melempar {@link IllegalStateException}; sukses mencetak "AcademicCoreWorkflowSelfTest OK" dan
 * keluar dengan {@code System.exit(0)}.
 */
public final class AcademicCoreWorkflowSelfTest{private AcademicCoreWorkflowSelfTest(){}
	/** Menjalankan pemeriksaan fail-closed untuk keempat adapter akademik inti; lihat javadoc kelas. */
	public static void main(String[]a)throws Exception{check(new PerkuliahanWorkflowGenericCrudAdapter(),Perkuliahan.class,4);check(new DetailPerkuliahanWorkflowGenericCrudAdapter(),Detailperkuliahan.class,2);check(new PertemuanWorkflowGenericCrudAdapter(),Pertemuan.class,2);check(new KrsMahasiswaWorkflowGenericCrudAdapter(),KrsMahasiswa.class,1);System.out.println("AcademicCoreWorkflowSelfTest OK");System.exit(0);}
	/** Memvalidasi satu adapter: mutasi generik harus fail-closed, jumlah kunci alami harus cocok, dan entitas harus terdaftar "reviewed". Melempar {@link IllegalStateException} bila salah satu gagal. */
	private static void check(GenericCrudAutoEntityAdapter x,Class c,int keys)throws Exception{GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(c);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);if(d.isCreateEnabled()||d.isUpdateEnabled()||d.isDeleteEnabled()||d.isImportEnabled())throw new IllegalStateException(c+" generic mutation must fail closed");if(x.getNaturalKeyProperties().size()!=keys)throw new IllegalStateException(c+" natural key");if(!GenericCrudReviewedAdapterFactory.isReviewed(c))throw new IllegalStateException(c+" reviewed adapter");}}
