package ais.action.master.repository.test;

import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryWorkflowService;

/**
 * Harness uji manual (bukan JUnit) untuk dua aturan internal {@link RepositoryWorkflowService}
 * (repository dokumen/karya ilmiah): batas ukuran halaman paginasi dan daftar putih (allow-list)
 * status workflow/review. Dijalankan lewat {@code main}.
 *
 * <p>
 * Karena method yang diuji ({@code pageSize}, {@code workflowStatus}, {@code reviewStatus})
 * bersifat {@code private} pada kelas layanan, harness ini mengaksesnya lewat refleksi
 * ({@link java.lang.reflect.Method#setAccessible(boolean)}). Yang diverifikasi: ukuran halaman
 * negatif jatuh ke default (20), di bawah minimum dipaksa ke 5, di atas maksimum dipaksa ke 100;
 * status workflow/review dinormalisasi ke huruf besar untuk nilai yang dikenal dan dibuang
 * (dikembalikan string kosong) untuk nilai asing atau percobaan injeksi (mis. {@code "DROP
 * TABLE"}) — sehingga nilai status yang tersimpan ke database selalu berasal dari daftar putih,
 * bukan input pengguna mentah.
 * </p>
 */
public final class RepositoryWorkspacePaginationSelfTest {
    private RepositoryWorkspacePaginationSelfTest(){}
    /** Menjalankan seluruh skenario uji batas ukuran halaman dan allow-list status workflow/review. */
    public static void main(String[] args)throws Exception{Method size=RepositoryWorkflowService.class.getDeclaredMethod("pageSize",int.class);size.setAccessible(true);check(((Integer)size.invoke(null,-1)).intValue()==20,"Fallback page size salah.");check(((Integer)size.invoke(null,1)).intValue()==5,"Minimum page size salah.");check(((Integer)size.invoke(null,999)).intValue()==100,"Maximum page size salah.");Method workflow=RepositoryWorkflowService.class.getDeclaredMethod("workflowStatus",String.class);workflow.setAccessible(true);check("DRAFT".equals(workflow.invoke(null,"draft")),"Normalisasi status gagal.");check("".equals(workflow.invoke(null,"DROP TABLE")),"Status asing tidak ditolak.");Method review=RepositoryWorkflowService.class.getDeclaredMethod("reviewStatus",String.class);review.setAccessible(true);check("IN_REVIEW".equals(review.invoke(null,"IN_REVIEW")),"Status review valid ditolak.");check("".equals(review.invoke(null,"PUBLISHED")),"Status non-antrean diterima.");System.out.println("RepositoryWorkspacePaginationSelfTest OK bounds and status allow-list");}
    /** Melempar {@link IllegalStateException} berisi {@code message} bila {@code value} bernilai {@code false}. */
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
