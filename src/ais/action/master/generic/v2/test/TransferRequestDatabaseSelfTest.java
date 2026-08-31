package ais.action.master.generic.v2.test;
import ais.common.newui.akunting.NewUiTransferWorkflowService;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Filter;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Options;
import ais.common.newui.akunting.NewUiTransferWorkflowService.ProcessDraft;
import ais.common.newui.akunting.NewUiTransferWorkflowService.ProcessSnapshot;
import ais.common.newui.akunting.NewUiTransferWorkflowService.Snapshot;
/**
 * Harness uji manual berbasis DATABASE SUNGGUHAN (dijalankan langsung via {@code main}, bukan dari
 * test runner/mocking) untuk {@link NewUiTransferWorkflowService} (alur permintaan transfer dana
 * modul akunting, antarmuka baru). Memverifikasi: paging dasar ({@link Filter} kosong), filter
 * status gabungan (waiting/submitted/transitory/transferred), filter jenis gabungan
 * (advance/accountability/procurement/termin/tax), paging daftar proses ({@code processes}), opsi
 * metode transfer yang tersedia ({@code options}), dan bahwa membuat proses dengan judul
 * kosong/spasi ditolak dengan {@link IllegalArgumentException}. Mencetak ringkasan hasil ke stdout
 * dan keluar dengan kode 0 bila seluruh pemeriksaan lolos; melempar {@link IllegalStateException}
 * lewat {@link #check(boolean, String)} pada pemeriksaan pertama yang gagal.
 */
public final class TransferRequestDatabaseSelfTest{private TransferRequestDatabaseSelfTest(){}
	/** Menjalankan seluruh skenario uji terhadap {@link NewUiTransferWorkflowService} secara berurutan menggunakan koneksi database sungguhan. */
	public static void main(String[]a){NewUiTransferWorkflowService s=new NewUiTransferWorkflowService();Filter f=new Filter();f.page=0;f.size=10;Snapshot d=s.load(f);check(d.total>=0&&d.rows.size()<=10,"paging");Filter st=new Filter();st.waiting=true;st.submitted=true;st.transitory=true;st.transferred=true;st.page=0;st.size=10;s.load(st);Filter kind=new Filter();kind.advance=true;kind.accountability=true;kind.procurement=true;kind.termin=true;kind.tax=true;kind.page=0;kind.size=10;s.load(kind);ProcessSnapshot p=s.processes(f);check(p.total>=0&&p.rows.size()<=10,"process paging");Options o=s.options();ProcessDraft bad=new ProcessDraft();bad.title=" ";boolean rejected=false;try{s.createProcess(bad,null);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid process rejected");System.out.println("TransferRequestDatabaseSelfTest OK requests="+d.total+" processes="+p.total+" methods="+o.methods.size());System.exit(0);}
	/** Menegaskan {@code v} bernilai {@code true}; melempar {@link IllegalStateException} berisi {@code m} bila tidak. */
	private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
