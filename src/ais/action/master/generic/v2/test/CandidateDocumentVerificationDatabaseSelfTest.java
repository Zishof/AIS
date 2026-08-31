package ais.action.master.generic.v2.test;import ais.common.newui.pmb.NewUiCandidateDocumentVerificationService;import ais.common.newui.pmb.NewUiCandidateDocumentVerificationService.Filter;import ais.common.newui.pmb.NewUiCandidateDocumentVerificationService.Snapshot;/**
 * Harness uji manual (bukan JUnit, dijalankan lewat {@code main}) untuk
 * {@link NewUiCandidateDocumentVerificationService} — layanan verifikasi dokumen calon mahasiswa
 * (PMB) pada antarmuka baru. Memuat satu halaman data ({@code Filter.size=10}) lalu memverifikasi:
 * (1) hasil paging valid ({@code total>=0}, jumlah baris tidak melebihi ukuran halaman); (2) opsi
 * filter ({@code programs}/{@code waves}/{@code selections}) tidak null; (3) bila ada baris data,
 * baris pertama memiliki {@code id} dan {@code document} terisi. Kegagalan assert melempar
 * {@link IllegalStateException}; sukses mencetak jumlah total dan baris yang dimuat lalu
 * {@code System.exit(0)}.
 */
public final class CandidateDocumentVerificationDatabaseSelfTest{private CandidateDocumentVerificationDatabaseSelfTest(){}public static void main(String[]a){Filter f=new Filter();f.size=10;Snapshot s=new NewUiCandidateDocumentVerificationService().load(f);check(s.total>=0&&s.rows.size()<=10,"paging");check(s.programs!=null&&s.waves!=null&&s.selections!=null,"options");if(!s.rows.isEmpty()){check(s.rows.get(0).id!=null,"id");check(s.rows.get(0).document!=null,"document");}System.out.println("CandidateDocumentVerificationDatabaseSelfTest OK total="+s.total+" rows="+s.rows.size());System.exit(0);}private static void check(boolean v,String m){if(!v)throw new IllegalStateException(m);}}
