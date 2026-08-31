package ais.action.master.generic.v2.test;import ais.common.newui.akunting.NewUiTransitoriService;import ais.common.newui.akunting.NewUiTransitoriService.Snapshot;
/**
 * Harness uji manual (bukan JUnit) yang memerlukan koneksi database aktif: memanggil
 * {@link NewUiTransitoriService#load} (daftar akun/pos transitori akunting) dengan filter kosong
 * dan memverifikasi bahwa jumlah total ({@link Snapshot#total}) yang dikembalikan tidak negatif.
 * Menonaktifkan validasi Bean Validation/DDL Hibernate lewat properti sistem sebelum memanggil
 * layanan agar harness dapat dijalankan mandiri di luar siklus hidup aplikasi web penuh.
 */
public final class TransitoriWorkflowDatabaseSelfTest{private TransitoriWorkflowDatabaseSelfTest(){}
	/** Menjalankan pemuatan snapshot Transitori dan memverifikasi total tidak negatif. */
	public static void main(String[]a)throws Exception{System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");Snapshot s=new NewUiTransitoriService().load(null,null,null,true,false,false,false,0,20);if(s.total<0)throw new IllegalStateException("total");System.out.println("TransitoriWorkflowDatabaseSelfTest OK total="+s.total);System.exit(0);}}
