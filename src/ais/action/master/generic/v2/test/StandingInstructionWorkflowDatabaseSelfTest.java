package ais.action.master.generic.v2.test;import java.util.HashSet;import ais.common.newui.akunting.NewUiStandingInstructionService;import ais.common.newui.akunting.NewUiStandingInstructionService.Snapshot;import ais.database.hibernate.HibernateUtil;import ais.database.model.rab.SatuanKerja;/**
 * Harness uji manual (dijalankan lewat {@code main}, memerlukan koneksi database nyata — bukan
 * JUnit/mock) untuk memastikan {@link NewUiStandingInstructionService#load} dapat memuat snapshot
 * daftar instruksi standing tanpa melempar galat dan mengembalikan total baris yang valid
 * (bukan negatif). Menonaktifkan validasi Bean Validation JPA/Hibernate lewat system property
 * sebelum memanggil service, karena harness dijalankan di luar konteks container yang biasanya
 * menyediakan validator. Kelas final tanpa instance (konstruktor privat kosong).
 */
public final class StandingInstructionWorkflowDatabaseSelfTest{private StandingInstructionWorkflowDatabaseSelfTest(){}
	/**
	 * Menjalankan satu pemuatan snapshot instruksi standing (tanpa filter, halaman pertama 20 baris)
	 * dan memvalidasi hasilnya, lalu keluar dengan kode 0 bila berhasil.
	 *
	 * @param a argumen baris perintah, tidak dipakai
	 * @throws Exception diteruskan apa adanya dari kegagalan akses database/service
	 */
	public static void main(String[]a)throws Exception{System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");Snapshot s=new NewUiStandingInstructionService().load(null,null,null,null,false,false,false,0,20,new HashSet<SatuanKerja>());if(s.total<0)throw new IllegalStateException("total");System.out.println("StandingInstructionWorkflowDatabaseSelfTest OK total="+s.total);System.exit(0);}}
