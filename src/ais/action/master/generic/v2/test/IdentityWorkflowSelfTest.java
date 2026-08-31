package ais.action.master.generic.v2.test;
import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.*;import ais.database.model.*;
/**
 * Harness uji manual (dijalankan langsung via {@code main}) yang memverifikasi PENGAMANAN
 * "fail-closed" pada adapter kerangka CRUD generik {@code generic/v2} untuk entitas identitas/biodata
 * sensitif ({@link Dosen}, {@link BiodataDosen}, {@link BiodataMahasiswa}, {@link BiodataPegawai}).
 * Untuk masing-masing, memastikan tiga hal lewat {@link #check}: (1) meski
 * {@link GenericCrudDefinition} diawali dengan create/update/delete/import SEMUA diaktifkan secara
 * eksplisit, memanggil {@code adapter.configure(d)} harus MEMATIKAN kembali seluruhnya — entitas ini
 * wajib hanya bisa diubah lewat alur kerja (workflow) khususnya, bukan mutasi CRUD generik biasa;
 * (2) adapter mendeklarasikan tepat satu properti kunci alami (natural key); (3) kelas entitas
 * terdaftar sebagai "reviewed" pada {@link GenericCrudReviewedAdapterFactory}. Melempar
 * {@link IllegalStateException} pada pelanggaran pertama yang ditemukan.
 */
public final class IdentityWorkflowSelfTest{private IdentityWorkflowSelfTest(){}
	/** Menjalankan pemeriksaan fail-closed untuk keempat adapter identitas/biodata secara berurutan. */
	public static void main(String[]a)throws Exception{check(new LecturerIdentityWorkflowGenericCrudAdapter(),Dosen.class);check(new LecturerBiodataWorkflowGenericCrudAdapter(),BiodataDosen.class);check(new StudentBiodataWorkflowGenericCrudAdapter(),BiodataMahasiswa.class);check(new EmployeeBiodataWorkflowGenericCrudAdapter(),BiodataPegawai.class);System.out.println("IdentityWorkflowSelfTest OK");System.exit(0);}
	/** Memverifikasi satu {@code adapter}/{@code entityClass}: mutasi generik harus fail-closed setelah {@code configure}, natural key tunggal, dan entitas terdaftar reviewed. Melempar {@link IllegalStateException} berlabel kelas entitas bila salah satu gagal. */
	private static void check(GenericCrudAutoEntityAdapter x,Class c)throws Exception{GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(c);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);if(d.isCreateEnabled()||d.isUpdateEnabled()||d.isDeleteEnabled()||d.isImportEnabled())throw new IllegalStateException(c+" generic mutation must fail closed");if(x.getNaturalKeyProperties().size()!=1)throw new IllegalStateException(c+" natural key");if(!GenericCrudReviewedAdapterFactory.isReviewed(c))throw new IllegalStateException(c+" reviewed adapter");}}
