package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.TransitoriWorkflowGenericCrudAdapter;import ais.database.model.akunting.Transitori;/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) untuk
 * {@link TransitoriWorkflowGenericCrudAdapter} — adapter alur kerja generic-CRUD v2 untuk entitas
 * {@link Transitori} (akunting). Memverifikasi tiga hal: (1) adapter memaksa
 * {@link GenericCrudDefinition} menjadi native-only, yaitu men-nonaktifkan create/update/delete/import
 * generik walaupun awalnya diaktifkan secara eksplisit — karena entitas ini hanya boleh diubah lewat
 * alur transitori native, bukan CRUD generik; (2) kunci alami ({@code getNaturalKeyProperties()})
 * memuat {@code daftarPengajuanTransfer} dan {@code kode}; (3) entitas {@link Transitori} terdaftar
 * sebagai "reviewed" di {@link GenericCrudReviewedAdapterFactory}. Method {@code check} melempar
 * {@link IllegalStateException} pada kegagalan assert sehingga proses berhenti dengan exit code
 * bukan nol; sukses ditandai cetak "TransitoriWorkflowSelfTest OK" dan {@code System.exit(0)}.
 */
@SuppressWarnings("rawtypes")public final class TransitoriWorkflowSelfTest{private TransitoriWorkflowSelfTest(){}public static void main(String[]a){TransitoriWorkflowGenericCrudAdapter x=new TransitoriWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(Transitori.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.contains("daftarPengajuanTransfer")&&k.contains("kode"),"keys");check(GenericCrudReviewedAdapterFactory.isReviewed(Transitori.class),"reviewed");System.out.println("TransitoriWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
