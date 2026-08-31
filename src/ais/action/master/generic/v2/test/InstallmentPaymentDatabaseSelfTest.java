package ais.action.master.generic.v2.test;import java.util.Calendar;import ais.common.newui.finance.NewUiInstallmentPaymentService;import ais.common.newui.finance.NewUiInstallmentPaymentService.Filter;import ais.common.newui.finance.NewUiInstallmentPaymentService.Snapshot;/**
 * Harness uji manual (dijalankan lewat {@code main}) untuk memverifikasi
 * {@link NewUiInstallmentPaymentService} terhadap database nyata: memuat snapshot pembayaran
 * cicilan 3 minggu terakhir (halaman pertama, 10 baris) lewat {@link Filter}, lalu memvalidasi
 * bahwa hasil {@link Snapshot} punya {@code total} non-negatif, jumlah baris tidak melebihi
 * ukuran halaman, dan daftar opsi filter ({@code s.options()}) tersedia.
 */
public final class InstallmentPaymentDatabaseSelfTest{private InstallmentPaymentDatabaseSelfTest(){}
	/** Menjalankan skenario pemuatan snapshot cicilan dan memvalidasi konsistensi paging/opsi; keluar dengan kode 0 bila berhasil. */
	public static void main(String[]a){NewUiInstallmentPaymentService s=new NewUiInstallmentPaymentService();Filter f=new Filter();Calendar c=Calendar.getInstance();f.end=c.getTime();c.add(Calendar.WEEK_OF_MONTH,-3);f.start=c.getTime();f.page=0;f.size=10;Snapshot x=s.load(f);if(x.total<0||x.rows.size()>10)throw new IllegalStateException("paging invalid");if(s.options()==null)throw new IllegalStateException("options missing");System.out.println("InstallmentPaymentDatabaseSelfTest OK total="+x.total+" amount="+x.amount);System.exit(0);}}
