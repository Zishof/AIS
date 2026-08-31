package ais.action.master.generic.v2.test;import ais.common.newui.finance.NewUiStudentArrearsService;import ais.common.newui.finance.NewUiStudentArrearsService.Filter;import ais.common.newui.finance.NewUiStudentArrearsService.Snapshot;/**
 * Harness uji manual (dijalankan lewat {@code main}) untuk memverifikasi
 * {@link NewUiStudentArrearsService} terhadap database nyata: memuat snapshot tunggakan
 * mahasiswa halaman pertama (10 baris), memvalidasi konsistensi paging dan total utang
 * non-negatif, lalu bila ada baris hasil, memuat rincian ({@code details}) mahasiswa pertama dan
 * memastikan daftar tagihan ({@code charges}) dan pembayaran ({@code payments}) tidak null.
 */
public final class StudentArrearsDatabaseSelfTest{private StudentArrearsDatabaseSelfTest(){}
	/** Menjalankan skenario pemuatan snapshot dan rincian tunggakan mahasiswa; keluar dengan kode 0 bila berhasil. */
	public static void main(String[]a){System.setProperty("javax.persistence.validation.mode","none");System.setProperty("hibernate.validator.apply_to_ddl","false");Filter f=new Filter();f.page=0;f.size=10;Snapshot s=new NewUiStudentArrearsService().load(f);check(s.total>=0&&s.rows.size()<=10,"rows");check(s.debt>=0,"debt");if(!s.rows.isEmpty()){NewUiStudentArrearsService.Details d=new NewUiStudentArrearsService().details(s.rows.get(0).id);check(d.charges!=null&&d.payments!=null,"details");}System.out.println("StudentArrearsDatabaseSelfTest OK total="+s.total+" debt="+s.debt);System.exit(0);}/** Melempar {@link IllegalStateException} berisi pesan {@code m} bila {@code v} bernilai false. */
private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
