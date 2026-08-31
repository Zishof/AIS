package ais.action.master.generic.v2.test;import java.util.List;import ais.action.master.generic.v2.GenericCrudDefinition;import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;import ais.action.master.generic.v2.adapter.PenggunaanAnggaranWorkflowGenericCrudAdapter;import ais.database.model.rab.PenggunaanAnggaran;/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) untuk
 * {@link PenggunaanAnggaranWorkflowGenericCrudAdapter} — adapter alur kerja generic-CRUD v2 untuk
 * entitas {@link PenggunaanAnggaran} (RAB). Memverifikasi: (1) adapter memaksa
 * {@link GenericCrudDefinition} menjadi native-only (create/update/delete/import generik
 * dinonaktifkan walau semula diaktifkan eksplisit) karena penggunaan anggaran hanya boleh diubah
 * lewat alur RAB native; (2) kunci alami tunggal adalah {@code ref}; (3) entitas terdaftar sebagai
 * "reviewed" di {@link GenericCrudReviewedAdapterFactory}. Kegagalan assert melempar
 * {@link IllegalStateException}; sukses dicetak "PenggunaanAnggaranWorkflowSelfTest OK" lalu
 * {@code System.exit(0)}.
 */
@SuppressWarnings("rawtypes")public final class PenggunaanAnggaranWorkflowSelfTest{private PenggunaanAnggaranWorkflowSelfTest(){}public static void main(String[]a){PenggunaanAnggaranWorkflowGenericCrudAdapter x=new PenggunaanAnggaranWorkflowGenericCrudAdapter();GenericCrudDefinition d=new GenericCrudDefinition();d.setEntityClass(PenggunaanAnggaran.class);d.setCreateEnabled(true);d.setUpdateEnabled(true);d.setDeleteEnabled(true);d.setImportEnabled(true);x.configure(d);check(!d.isCreateEnabled()&&!d.isUpdateEnabled()&&!d.isDeleteEnabled()&&!d.isImportEnabled(),"native-only");List k=x.getNaturalKeyProperties();check(k.size()==1&&"ref".equals(k.get(0)),"ref key");check(GenericCrudReviewedAdapterFactory.isReviewed(PenggunaanAnggaran.class),"reviewed");System.out.println("PenggunaanAnggaranWorkflowSelfTest OK");System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
