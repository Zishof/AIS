package ais.action.master.repository.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import ais.action.master.repository.RepositoryFileService;

/**
 * Harness uji manual untuk lapisan keamanan unggah/akses berkas di
 * {@link RepositoryFileService}. Memanggil sejumlah method privat kelas tersebut lewat refleksi
 * (dibuat {@code setAccessible(true)} karena memang tidak dimaksudkan sebagai API publik) untuk
 * memverifikasi lima pagar keamanan: (1) validasi tanda tangan biner ({@code validSignature}) —
 * berkas dengan magic bytes PDF diterima untuk ekstensi {@code pdf} tapi ditolak bila diklaim
 * sebagai {@code png}, mencegah upload berkas berbahaya yang menyamar lewat ekstensi palsu; (2)
 * penetralan nama berkas ({@code safeFileName}) — separator path seperti {@code ../} diubah
 * menjadi karakter aman sehingga tidak bisa dipakai untuk path traversal, dan nama kosong/blank
 * ditolak dengan {@link IllegalArgumentException}; (3) normalisasi level akses
 * ({@code normalizeAccess}) — nilai valid diteruskan apa adanya, nilai asing/tidak dikenal
 * di-<i>fail-closed</i> menjadi {@code RESTRICTED} (bukan default terbuka); (4) resolusi MIME
 * type ({@code normalizedMime}) — tipe MIME ditentukan secara otoritatif dari ekstensi berkas di
 * sisi server, BUKAN dipercaya begitu saja dari deklarasi klien (diuji dengan mengirim header
 * MIME yang disusupi percobaan injeksi header {@code \r\nX-Test: injected}, yang harus diabaikan
 * sepenuhnya). Melempar {@link IllegalStateException} dengan pesan Indonesia yang menjelaskan
 * pagar mana yang jebol bila salah satu pemeriksaan gagal.
 */
public final class RepositoryFileSecuritySelfTest {
    private RepositoryFileSecuritySelfTest(){}
    /** Menjalankan seluruh pemeriksaan keamanan berkas (signature, nama aman, level akses, MIME otoritatif); lihat javadoc kelas untuk rincian tiap pagar. */
    public static void main(String[] args)throws Exception{Method signature=RepositoryFileService.class.getDeclaredMethod("validSignature",String.class,byte[].class,int.class);signature.setAccessible(true);byte[] pdf=new byte[]{0x25,0x50,0x44,0x46,0x2d,0x31};check(Boolean.TRUE.equals(signature.invoke(null,"pdf",pdf,pdf.length)),"Signature PDF valid ditolak.");check(Boolean.FALSE.equals(signature.invoke(null,"png",pdf,pdf.length)),"Ekstensi palsu diterima.");Method name=RepositoryFileService.class.getDeclaredMethod("safeFileName",String.class);name.setAccessible(true);check(".._rahasia.pdf".equals(name.invoke(null,"../rahasia.pdf")),"Separator path tidak dinetralisasi.");try{name.invoke(null," ");throw new IllegalStateException("Nama kosong diterima.");}catch(InvocationTargetException expected){check(expected.getCause() instanceof IllegalArgumentException,"Exception nama kosong salah.");}Method access=RepositoryFileService.class.getDeclaredMethod("normalizeAccess",String.class);access.setAccessible(true);check("OPEN_ACCESS".equals(access.invoke(null,"OPEN_ACCESS")),"Open access valid ditolak.");check("RESTRICTED".equals(access.invoke(null,"UNKNOWN")),"Akses asing tidak fail-closed.");Method mime=RepositoryFileService.class.getDeclaredMethod("normalizedMime",String.class,String.class);mime.setAccessible(true);check("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(mime.invoke(null,"docx","text/html\r\nX-Test: injected")),"MIME masih mempercayai deklarasi klien.");System.out.println("RepositoryFileSecuritySelfTest OK signatures filename access authoritative-mime fail-closed");}
    /** Melempar {@link IllegalStateException} berisi {@code message} bila {@code value} bernilai {@code false}; dipakai sebagai assert ringan sepanjang harness ini. */
    private static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
}
