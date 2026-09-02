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
                tanpaAdaptor++;
                catat(perModul, "(tanpa adaptor)", id + "\t" + label + "\t" + url);
                continue;
            }
            if (scaffold.contains(r.getTarget())) {
                masihScaffold++;
                catat(perModul, r.getModule(), id + "\t" + label + "\t" + r.getPage());
            } else {
                siap++;
            }
        }

        System.out.println("menu daun berurl : " + total);
        System.out.println("sudah native     : " + siap);
        System.out.println("masih scaffold   : " + masihScaffold);
        System.out.println("tanpa adaptor    : " + tanpaAdaptor);
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
