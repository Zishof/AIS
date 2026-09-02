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

    private static final String SUMBER = "C:/opt/AIS/ais/src/main/src";

    private static final String WEBAPP = "C:/opt/AIS/ais/src/main/webapp";

    public static void main(String[] args) throws Exception {
        Set<String> paths = new HashSet<String>();
        kumpulkan(new File(WEBAPP + "/WEB-INF/new"), "/WEB-INF/new", paths);

        // Nama sederhana seluruh kelas ber-@Entity. Pabrik definisi menerjemahkan
        // nama entitas yang dideklarasikan scaffold menjadi metadata Hibernate;
        // bila namanya bukan entitas terpetakan, build() gagal, tryAutoRegister
        // mengembalikan null, dan dispatcher jatuh ke stub SCAFFOLD. Layar itu
        // berongga sungguhan -- dan itu dapat diketahui tanpa basis data.
        Set<String> entitas = new HashSet<String>();
        kumpulkanEntitas(new File(SUMBER), entitas);

        // Ada DUA dispatcher, dan membedakannya menentukan arti hasil sapuan:
        //
        //   _shared/services/dispatcher.jsp
        //       dispatcher scaffold. Mencoba Generic CRUD lebih dulu, dan bila
        //       pabrik menolak barulah mengembalikan stub SCAFFOLD.
        //   _shared/generic-crud/services/dispatcher.jsp
        //       service Generic CRUD tulisan tangan yang menyetel
        //       genericCrudEntityKey sendiri. Ini halaman yang sudah jadi,
        //       bukan scaffold.
        //
        // Menyamakan keduanya -- cukup dengan mencari "dispatcher.jsp" --
        // membuat halaman Generic CRUD tulisan tangan terhitung sebagai
        // scaffold, dan karena ia tidak mendeklarasikan nuiServiceEntities ia
        // lalu terhitung pula sebagai "pasti berongga". Itulah yang membuat
        // Agama dan Mahasiswa sempat dilaporkan berongga padahal keduanya
        // dilayani dengan baik.
        Set<String> scaffold = new HashSet<String>();
        for (String p : paths) {
            if (!p.endsWith("_service.jsp")) continue;
            if (isiMemuat(WEBAPP + p, "_shared/generic-crud/services/dispatcher.jsp")) continue;
            if (isiMemuat(WEBAPP + p, "_shared/services/dispatcher.jsp")) scaffold.add(p);
        }

        int total = 0, tanpaAdaptor = 0, tidakTersedia = 0, masihScaffold = 0, siap = 0;
        int berentitas = 0, berongga = 0, takTerpetakan = 0;
        int perluKontrakKhusus = 0;
        Map<String, Integer> perTipeLayanan = new TreeMap<String, Integer>();
        Map<String, List<String>> perModul = new TreeMap<String, List<String>>();
        for (int i = 0; i < ais.common.MenuSnapshotData.DATA.length; i++) {
            String[] k = ais.common.MenuSnapshotData.DATA[i].split("[|]", -1);
            if (k.length < 5) continue;
            String id = k[0], label = k[3], url = k[4];
            if (url == null || url.trim().length() == 0) continue;   // cabang, bukan daun
            total++;
            NewUiUnavailableRouteRegistry.Entry unavailable =
                    NewUiUnavailableRouteRegistry.find(url);
            if (unavailable != null) {
                tidakTersedia++;
                catat(perModul, "(fungsi tidak tersedia)", id + "\t" + label + "\t"
                        + unavailable.getCode() + "\t" + url);
                continue;
            }
            NewUiNativeJspResolver.Result r = null;
            // Lapisan pertama resolve(): keputusan pemetaan eksplisit.
            ais.common.newui.menu.NewUiRuteEksplisitRegistry.Tujuan eksplisit =
                    ais.common.newui.menu.NewUiRuteEksplisitRegistry.cari(url);
            if (eksplisit != null) {
                String t = "/WEB-INF/new/" + eksplisit.getModule()
                        + "/services/" + eksplisit.getPage() + "_service.jsp";
                if (paths.contains(t)) {
                    r = new NewUiNativeJspResolver.Result(
                            t, eksplisit.getModule(), eksplisit.getPage());
                }
            }
            if (r == null) r = NewUiNativeJspResolver.resolveFromPaths(url, true, paths);
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
                String tipeLayanan = tipeLayanan(WEBAPP + r.getTarget());
                tambah(perTipeLayanan, tipeLayanan);
                // Dispatcher mencoba Generic CRUD lebih dulu; ia jatuh ke stub
                // SCAFFOLD hanya bila tryAutoRegister mengembalikan null.
                // Entitas yang dideklarasikan scaffold adalah masukan utama
                // pemanggilan itu, jadi scaffold tanpa entitas dapat dipastikan
                // berongga tanpa basis data. Yang punya entitas belum tentu
                // terlayani -- pabrik masih dapat menolaknya -- tetapi itu tidak
                // dapat diputuskan luring, dan ditandai demikian.
                String[] deklarasi = entitasDideklarasikan(WEBAPP + r.getTarget());
                boolean adaEntitas = deklarasi.length > 0;
                boolean terpetakan = false;
                for (int d = 0; d < deklarasi.length; d++) {
                    if (entitas.contains(deklarasi[d])) { terpetakan = true; break; }
                }
                String tanda;
                if (!adaEntitas) { berongga++; tanda = "[RONGGA] "; }
                else if (!terpetakan) { takTerpetakan++; tanda = "[ENTITAS-TAK-DIKENAL] "; }
                else {
                    berentitas++;
                    if (perluKontrakKhusus(tipeLayanan)) {
                        perluKontrakKhusus++;
                        tanda = "[review:" + tipeLayanan + "] ";
                    } else {
                        tanda = "[crud?:" + tipeLayanan + "] ";
                    }
                }
                catat(perModul, tanda + r.getModule(),
                        id + "\t" + label + "\t" + r.getPage());
            } else {
                siap++;
            }
        }

        System.out.println("menu daun berurl : " + total);
        System.out.println("sudah native     : " + siap);
        System.out.println("masih scaffold   : " + masihScaffold);
        System.out.println("tanpa adaptor    : " + tanpaAdaptor);
        System.out.println("fungsi tak tersedia (eksplisit) : " + tidakTersedia);
        System.out.println("  scaffold + entitas (mungkin Generic CRUD) : " + berentitas);
        System.out.println("  scaffold TANPA entitas (pasti berongga)   : " + berongga);
        System.out.println("  scaffold dgn entitas TAK TERPETAKAN       : " + takTerpetakan);
        System.out.println("  tipe semantik yang perlu kontrak khusus   : " + perluKontrakKhusus);
        System.out.println("  distribusi tipe layanan                   : " + perTipeLayanan);
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

    private static void tambah(Map<String, Integer> hitungan, String kunci) {
        Integer lama = hitungan.get(kunci);
        hitungan.put(kunci, Integer.valueOf(lama == null ? 1 : lama.intValue() + 1));
    }

    /**
     * Tipe yang secara semantik bukan tabel data induk biasa.
     *
     * <p>Ini penanda audit, bukan keputusan routing. Mempunyai entity Hibernate
     * tidak membuktikan bahwa dashboard, laporan, integrasi, atau workflow
     * boleh dirender sebagai tabel entity mentah.</p>
     */
    public static boolean perluKontrakKhusus(String tipe) {
        return "dashboard".equals(tipe) || "report".equals(tipe)
                || "integration".equals(tipe) || "workflow".equals(tipe);
    }

    /** Ambil nilai literal atribut {@code nuiServiceType} dari sumber JSP. */
    public static String tipeLayananDariIsi(String sumber) {
        if (sumber == null) return "tidak_diketahui";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "nuiServiceType\\\"\\s*,\\s*\\\"([^\\\"]+)\\\"").matcher(sumber);
        return m.find() ? m.group(1).trim().toLowerCase() : "tidak_diketahui";
    }

    private static String tipeLayanan(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            StringBuilder sumber = new StringBuilder();
            String baris;
            while ((baris = r.readLine()) != null) sumber.append(baris).append((char) 10);
            return tipeLayananDariIsi(sumber.toString());
        } catch (Exception e) {
            return "tidak_diketahui";
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
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

    /** Nama sederhana entitas yang dideklarasikan scaffold; kosong bila tidak ada. */
    private static String[] entitasDideklarasikan(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
            String baris;
            while ((baris = r.readLine()) != null) {
                int i = baris.indexOf("nuiServiceEntities");
                if (i < 0) continue;
                // Harus benar-benar pemanggilan setAttribute, bukan sekadar
                // penyebutan nama atribut. Komentar di kepala berkas yang
                // menjelaskan atribut ini akan cocok lebih dulu dan membuat
                // pembacaan berhenti pada baris yang tidak punya daftar entitas
                // -- persis yang terjadi ketika catatan penyuntingan manual
                // ditambahkan di atas berkas.
                if (baris.indexOf("setAttribute") < 0) continue;
                // Bentuk barisnya:
                //   setAttribute("nuiServiceEntities", new String[]{"Foo","Bar"});
                // Mulai membaca dari '{', bukan dari akhir nama atribut:
                // i + 18 justru mendarat pada tanda kutip penutup nama atribut
                // itu sendiri, sehingga pembacaan naif selalu menemukan kutip
                // dan setiap scaffold tampak mendeklarasikan entitas.
                int kurung = baris.indexOf(123, i);
                int akhir = kurung < 0 ? -1 : baris.indexOf(125, kurung);
                if (kurung < 0 || akhir < 0) return new String[0];
                List<String> nama = new ArrayList<String>();
                int dari = kurung + 1;
                while (dari < akhir) {
                    int buka = baris.indexOf(34, dari);
                    if (buka < 0 || buka > akhir) break;
                    int tutup = baris.indexOf(34, buka + 1);
                    if (tutup < 0 || tutup > akhir) break;
                    String v = baris.substring(buka + 1, tutup).trim();
                    if (v.length() > 0) nama.add(v);
                    dari = tutup + 1;
                }
                return nama.toArray(new String[nama.size()]);
            }
            return new String[0];
        } catch (Exception e) {
            return new String[0];
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
    }

    /** Nama sederhana tiap kelas sumber yang membawa anotasi @Entity. */
    private static void kumpulkanEntitas(File dir, Set<String> out) {
        File[] anak = dir.listFiles();
        if (anak == null) return;
        for (int i = 0; i < anak.length; i++) {
            if (anak[i].isDirectory()) { kumpulkanEntitas(anak[i], out); continue; }
            String nama = anak[i].getName();
            if (!nama.endsWith(".java")) continue;
            if (isiMemuat(anak[i].getPath(), "@Entity")) {
                out.add(nama.substring(0, nama.length() - 5));
            }
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
