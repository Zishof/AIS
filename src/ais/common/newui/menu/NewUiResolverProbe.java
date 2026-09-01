package ais.common.newui.menu;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Alat bantu pemetaan: cari adaptor native sebuah URL menu lewat resolver yang
 * sebenarnya dipakai aplikasi.
 *
 * <p>Pemetaan dengan mencocokkan nama berkas gagal untuk sebagian menu, karena
 * nama halaman adaptor tidak selalu sama dengan nama berkas {@code .zul}-nya.
 * Menanyakannya kepada resolver menghilangkan tebakan itu. Dipisahkan sebagai
 * kelas tersendiri di dalam paket yang sama karena {@code resolveFromPaths}
 * memang tidak publik — dan tidak perlu dijadikan publik hanya untuk ini.</p>
 *
 * <p>Daftar jalur dibangun dengan menelusuri folder, bukan lewat
 * {@code ServletContext}, supaya dapat dijalankan di luar container.</p>
 */
public final class NewUiResolverProbe {

    private NewUiResolverProbe() { }

    private static void kumpulkan(File akar, String awalan, Set<String> keluar) {
        File[] isi = akar.listFiles();
        if (isi == null) return;
        for (int i = 0; i < isi.length; i++) {
            String jalur = awalan + isi[i].getName();
            if (isi[i].isDirectory()) kumpulkan(isi[i], jalur + "/", keluar);
            else keluar.add(jalur);
        }
    }

    /** Kelas composer yang disebut atribut apply/use di dalam berkas .zul. */
    private static String composerDariZul(String route) {
        String jalur = route;
        int tanya = jalur.indexOf('?');
        if (tanya >= 0) jalur = jalur.substring(0, tanya);
        if (!jalur.endsWith(".zul")) return "";
        if (jalur.startsWith("/pages/")) jalur = jalur.substring("/pages/".length());
        File berkas = new File(ZUL_DIR, jalur);
        if (!berkas.isFile()) return "";
        try {
            byte[] isi = java.nio.file.Files.readAllBytes(berkas.toPath());
            String teks = new String(isi, "UTF-8");
            String pola = "(?:apply|use)\\s*=\\s*[\"']([^\"']+)[\"']";
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile(pola, java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(teks);
            return m.find() ? m.group(1) : "";
        } catch (Exception gagal) {
            return "";
        }
    }

    private static File ZUL_DIR;

    public static void main(String[] args) {
        ZUL_DIR = new File(args[0], "../z/x/y/pages");
        File akar = new File(args[0]);
        Set<String> jalur = new HashSet<String>();
        kumpulkan(akar, "/WEB-INF/new/", jalur);
        System.out.println("jalur terkumpul: " + jalur.size());
        for (int i = 1; i < args.length; i++) {
            NewUiNativeJspResolver.Result hasil =
                    NewUiNativeJspResolver.resolveFromPaths(args[i], true, jalur);
            String lewat = "langsung";
            if (hasil == null) {
                // Resolver sungguhan punya cadangan: bila nama berkas .zul tidak
                // cocok, ia membaca atribut apply/use di dalam .zul itu lalu
                // mencoba lagi memakai nama kelas composer-nya. Tanpa langkah ini
                // pemetaan tampak gagal padahal tidak.
                String composer = composerDariZul(args[i]);
                if (composer.length() > 0) {
                    hasil = NewUiNativeJspResolver.resolveFromPaths(composer, true, jalur);
                    lewat = "composer " + composer;
                }
            }
            System.out.println(String.format("%-58s %-34s %s", args[i],
                    hasil == null ? "TIDAK TERPETAKAN"
                            : hasil.getModule() + " / " + hasil.getPage(), lewat));
        }
        System.exit(0);
    }
}
