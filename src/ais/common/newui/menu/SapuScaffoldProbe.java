package ais.common.newui.menu;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;

/**
 * Perkakas luring: memutar ulang aturan {@link NewUiNativeJspResolver} terhadap
 * seluruh menu pada {@code MenuSnapshotData} untuk mendata mana yang masih
 * jatuh ke scaffold.
 *
 * <p>Sengaja tidak menyapu server. Sapuan HTTP hanya melihat menu yang dimiliki
 * satu akun, dan server demo menjalankan build lama — dua-duanya menghasilkan
 * hitungan yang salah dengan cara yang tidak kelihatan. Resolver adalah fungsi
 * murni atas (route, himpunan path JSP), jadi keduanya dapat disediakan dari
 * berkas dan hasilnya lengkap serta dapat diulang.</p>
 */
public final class SapuScaffoldProbe {

    private static final char BACKSLASH = (char) 92;
    private static final java.util.regex.Pattern COMPOSER =
            java.util.regex.Pattern.compile("(?:apply|use)\\s*=\\s*[\"']([^\"']+)[\"']",
                    java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final String WEBAPP = "C:/opt/AIS/ais/src/main/webapp";

    public static void main(String[] args) throws Exception {
        Set<String> paths = new HashSet<String>();
        kumpulkan(new File(WEBAPP + "/WEB-INF/new"), "/WEB-INF/new", paths);

        // Service JSP yang masih hasil pembangkit scaffold.
        Set<String> scaffold = new HashSet<String>();
        for (String p : paths) {
            if (!p.endsWith("_service.jsp")) continue;
            if (isiMemuat(WEBAPP + p, "dispatcher.jsp")) scaffold.add(p);
        }

        int total = 0, tanpaAdaptor = 0, masihScaffold = 0, siap = 0;
        int berentitas = 0, berongga = 0;
        Map<String, List<String>> perModul = new TreeMap<String, List<String>>();
        for (int i = 0; i < ais.common.MenuSnapshotData.DATA.length; i++) {
            String[] k = ais.common.MenuSnapshotData.DATA[i].split("[|]", -1);
            if (k.length < 5) continue;
            String id = k[0], label = k[3], url = k[4];
            if (url == null || url.trim().length() == 0) continue;   // cabang, bukan daun
            total++;
            NewUiNativeJspResolver.Result r =
                    NewUiNativeJspResolver.resolveFromPaths(url, true, paths);
            if (r == null) {
                // resolve() yang sebenarnya tidak berhenti di nama URL. Bila
                // gagal ia membaca ZUL-nya, mengambil kelas composer dari
                // apply=, lalu mencoba lagi dengan nama kelas itu -- dan JSP
                // memang dinamai menurut kelas aksi, bukan menurut ZUL
                // (mis. jenis_penghapusan_barang.zul -> PenghapusanMasterAsset).
                // Tanpa langkah ini ratusan menu dilaporkan tak teresolusi
                // padahal server melayaninya dengan baik.
                String composer = composerRoute(url);
                if (composer.length() > 0) {
                    r = NewUiNativeJspResolver.resolveFromPaths(composer, true, paths);
                }
            }
            if (r == null) {
                // Lapisan ketiga resolve(): menu laporan yang kolom url-nya
                // berisi nama laporan, dipetakan ke kelas *Window-nya.
                String window = NewUiLaporanAliasRegistry.windowUntuk(url);
                if (window != null) {
                    r = NewUiNativeJspResolver.resolveFromPaths(window, true, paths);
                }
            }
            if (r == null) {
                tanpaAdaptor++;
                catat(perModul, "(tanpa adaptor)", id + "\t" + label + "\t" + url);
                continue;
            }
            if (scaffold.contains(r.getTarget())) {
                masihScaffold++;
                // Dispatcher mencoba Generic CRUD lebih dulu; ia jatuh ke stub
                // SCAFFOLD hanya bila tryAutoRegister mengembalikan null.
                // Entitas yang dideklarasikan scaffold adalah masukan utama
                // pemanggilan itu, jadi scaffold tanpa entitas dapat dipastikan
                // berongga tanpa basis data. Yang punya entitas belum tentu
                // terlayani -- pabrik masih dapat menolaknya -- tetapi itu tidak
                // dapat diputuskan luring, dan ditandai demikian.
                boolean adaEntitas = punyaEntitas(WEBAPP + r.getTarget());
                if (adaEntitas) berentitas++; else berongga++;
                catat(perModul, (adaEntitas ? "[crud?] " : "[RONGGA] ") + r.getModule(),
                        id + "\t" + label + "\t" + r.getPage());
            } else {
                siap++;
            }
        }

        System.out.println("menu daun berurl : " + total);
        System.out.println("sudah native     : " + siap);
        System.out.println("masih scaffold   : " + masihScaffold);
        System.out.println("tanpa adaptor    : " + tanpaAdaptor);
        System.out.println("  scaffold + entitas (mungkin Generic CRUD) : " + berentitas);
        System.out.println("  scaffold TANPA entitas (pasti berongga)   : " + berongga);
        System.out.println();
        for (Map.Entry<String, List<String>> e : perModul.entrySet()) {
            System.out.println("=== " + e.getKey() + " (" + e.getValue().size() + ") ===");
            for (String s : e.getValue()) System.out.println("   " + s);
        }
    }

    private static void catat(Map<String, List<String>> m, String kunci, String baris) {
        List<String> l = m.get(kunci);
        if (l == null) { l = new ArrayList<String>(); m.put(kunci, l); }
        l.add(baris);
    }

    private static void kumpulkan(File dir, String prefix, Set<String> out) {
        File[] anak = dir.listFiles();
        if (anak == null) return;
        for (int i = 0; i < anak.length; i++) {
            String p = prefix + "/" + anak[i].getName();
            if (anak[i].isDirectory()) kumpulkan(anak[i], p, out);
            else if (p.endsWith(".jsp")) out.add(p);
        }
    }

    /** Apakah scaffold mendeklarasikan nuiServiceEntities yang tidak kosong. */
    private static boolean punyaEntitas(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String baris;
            while ((baris = r.readLine()) != null) {
                int i = baris.indexOf("nuiServiceEntities");
                if (i < 0) continue;
                return baris.indexOf(34, i + 18) >= 0;
            }
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
    }

    /**
     * Tiruan luring {@code NewUiNativeJspResolver.composerRoute}: ambil nama
     * kelas composer dari atribut {@code apply=} pada ZUL, buang akhiran
     * "Action".
     */
    private static String composerRoute(String existingRoute) {
        if (existingRoute == null) return "";
        String route = existingRoute.trim().replace(BACKSLASH, '/');
        int q = route.indexOf('?');
        if (q >= 0) route = route.substring(0, q);
        if (!route.startsWith("/") || route.indexOf("..") >= 0
                || !route.toLowerCase().endsWith(".zul")) return "";
        BufferedReader reader = null;
        try {
            File f = new File(WEBAPP + "/WEB-INF/z/x/y" + route);
            if (!f.isFile()) return "";
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
            StringBuilder sumber = new StringBuilder();
            String baris; int n = 0;
            while ((baris = reader.readLine()) != null && n++ < 160) sumber.append(baris).append((char) 10);
            java.util.regex.Matcher m = COMPOSER.matcher(sumber.toString());
            while (m.find()) {
                String kelas = m.group(1);
                if (kelas == null || kelas.indexOf(".action.") < 0) continue;
                int dot = kelas.lastIndexOf('.');
                String simple = dot < 0 ? kelas : kelas.substring(dot + 1);
                if (simple.endsWith("Action") && simple.length() > 6) {
                    simple = simple.substring(0, simple.length() - 6);
                }
                return simple;
            }
            return "";
        } catch (Exception e) {
            return "";
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) { }
        }
    }

    private static boolean isiMemuat(String path, String cari) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String baris;
            while ((baris = r.readLine()) != null) if (baris.indexOf(cari) >= 0) return true;
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
    }
}
