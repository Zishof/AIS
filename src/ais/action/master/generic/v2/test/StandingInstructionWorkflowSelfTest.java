package ais.action.master.generic.v2.test;

import java.util.List;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.StandingInstructionWorkflowGenericCrudAdapter;
import ais.database.model.akunting.StandingInstruction;

/**
 * Harness regresi mandiri untuk {@link StandingInstructionWorkflowGenericCrudAdapter}. Pemeriksaan memastikan
 * konfigurasi workflow native menonaktifkan create/update/delete/import, kunci alami memuat
 * {@code kodeUnik} dan {@code kode}, serta {@link StandingInstruction} terdaftar sebagai entitas
 * {@code reviewed} pada {@link GenericCrudReviewedAdapterFactory}.
 *
 * <p><b>Batas tanggung jawab:</b> kelas ini hanya menjadi executable self-test, bukan bagian dari alur
 * produksi dan bukan tempat menyalin aturan domain. Perubahan perilaku umum harus dilakukan pada adapter atau
 * service yang diuji, kemudian dikunci dengan assertion di sini agar tidak muncul implementasi paralel.</p>
 *
 * <p>{@link #main(String[])} menjalankan seluruh pemeriksaan, mencetak pesan sukses, lalu mengakhiri JVM dengan
 * kode {@code 0}. Helper {@code check} melempar {@link IllegalStateException} bila suatu kontrak tidak terpenuhi,
 * sehingga proses berhenti dengan status gagal.</p>
 */
@SuppressWarnings("rawtypes")
public final class StandingInstructionWorkflowSelfTest{private StandingInstructionWorkflowSelfTest(){}
	/** Menjalankan seluruh pemeriksaan adapter standing instruction; lihat javadoc kelas. */
	public static void main(String[]a){StandingInstructionWorkflowGenericCrudAdapter x=new StandingInstructionWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(StandingInstruction.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native workflow");List k=x.getNaturalKeyProperties();check(k.contains("kodeUnik")&&k.contains("kode"),"keys");check(GenericCrudReviewedAdapterFactory.isReviewed(StandingInstruction.class),"reviewed");System.out.println("StandingInstructionWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
