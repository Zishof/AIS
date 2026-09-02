package ais.common.newui.menu;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sapuan KETERJANGKAUAN KLIEN: memetakan setiap menu daun ke JSP layanannya,
 * lalu ke controller yang dipanggil JSP itu, lalu ke bentuk amplop JSON yang
 * dikeluarkan controller tersebut.
 *
 * <p>Alasannya satu kegagalan yang terus berulang: kontrak server dapat
 * lengkap, teruji, dan hijau, namun tidak dapat dipakai klien mana pun. Tiga
 * kali hal itu terjadi (enam laporan tanpa penanda, {@code tahunAjaran} yang
 * hilang pada dua alur KRS), dan setiap kali baru ketahuan ketika halaman
 * konsumennya dibangun. Sapuan scaffold menjawab "apakah server melayaninya";
 * sapuan ini menjawab pertanyaan yang berbeda dan justru lebih menentukan:
 * "apakah ada klien yang dapat memakannya".</p>
 *
 * <p>Pembeda yang menentukan adalah amplopnya. {@code NativeApplicationApi}
 * pada aplikasi Flutter MELEMPAR ADAPTER_NOT_IMPLEMENTED bila jawaban memuat
 * {@code ok} tanpa {@code success} — amplop demikian hanya dapat dibaca
 * halaman khusus yang memang menulis kontraknya. Jadi menu yang jatuh ke
 * renderer CRUD generik WAJIB dilayani controller beramplop
 * {@code {success, data}}; bila tidak, menunya pasti gagal di klien meskipun
 * seluruh uji server hijau.</p>
 *
 * <p>Keluarannya TSV agar dapat disilangkan dengan tabel perutean Flutter:
 * {@code id, label, url, jsp, controller, amplop}.</p>
 */
public final class SapuKlienProbe {

    private static final String WEBAPP = "C:/opt/AIS/ais/src/main/webapp";
    private static final String SUMBER = "C:/opt/AIS/ais/src/main/src";

    /** Controller dipanggil dari JSP sebagai {@code NamaController.handle(...)}. */
    private static final Pattern PEMANGGILAN =
            Pattern.compile("\\b(NewUi[A-Za-z0-9_]*Controller)\\s*\\.");

    private SapuKlienProbe() { }

    public static void main(String[] args) throws Exception {
        Set<String> paths = new HashSet<String>();
        SapuScaffoldProbe.kumpulkan(new File(WEBAPP + "/WEB-INF/new"), "/WEB-INF/new", paths);

        Map<String, String> amplopControllerCache = new TreeMap<String, String>();

        System.out.println("id\tlabel\turl\tjsp\tcontroller\tamplop");
        for (int i = 0; i < ais.common.MenuSnapshotData.DATA.length; i++) {
            String[] k = ais.common.MenuSnapshotData.DATA[i].split("[|]", -1);
            if (k.length < 5) continue;
            String id = k[0], label = k[3], url = k[4];
            if (url == null || url.trim().length() == 0) continue;   // cabang, bukan daun

            NewUiUnavailableRouteRegistry.Entry unavailable =
                    NewUiUnavailableRouteRegistry.find(url);
            if (unavailable != null) {
                baris(id, label, url, "(fungsi tidak tersedia)", unavailable.getCode(), "-");
                continue;
            }

            NewUiNativeJspResolver.Result r = resolusi(url, paths);
            if (r == null) {
                baris(id, label, url, "(tanpa adaptor)", "-", "-");
                continue;
            }

            String jsp = r.getTarget();
            String controller = controllerDari(WEBAPP + jsp);
            String amplop;
            if (controller.length() == 0) {
                // JSP tanpa pemanggilan controller berarti dispatcher: entah
                // scaffold, entah Generic CRUD tulisan tangan. Keduanya bicara
                // amplop {success, data}, jadi renderer generik memahaminya.
                amplop = dispatcher(WEBAPP + jsp) ? "generik(dispatcher)" : "?";
            } else {
                String cached = amplopControllerCache.get(controller);
                if (cached == null) {
                    cached = amplopDari(controller);
                    amplopControllerCache.put(controller, cached);
                }
                amplop = cached;
            }
            baris(id, label, url, jsp, controller.length() == 0 ? "-" : controller, amplop);
        }
    }

    /** Tiga lapisan resolve() yang sama dengan sapuan scaffold. */
    private static NewUiNativeJspResolver.Result resolusi(String url, Set<String> paths) {
        NewUiRuteEksplisitRegistry.Tujuan eksplisit = NewUiRuteEksplisitRegistry.cari(url);
        if (eksplisit != null) {
            String t = "/WEB-INF/new/" + eksplisit.getModule()
                    + "/services/" + eksplisit.getPage() + "_service.jsp";
            if (paths.contains(t)) {
                return new NewUiNativeJspResolver.Result(t, eksplisit.getModule(),
                        eksplisit.getPage());
            }
        }
        NewUiNativeJspResolver.Result r = NewUiNativeJspResolver.resolveFromPaths(url, true, paths);
        if (r == null) {
            String composer = SapuScaffoldProbe.composerRoute(url);
            if (composer.length() > 0) {
                r = NewUiNativeJspResolver.resolveFromPaths(composer, true, paths);
            }
        }
        if (r == null) {
            String window = NewUiLaporanAliasRegistry.windowUntuk(url);
            if (window != null) {
                r = NewUiNativeJspResolver.resolveFromPaths(window, true, paths);
            }
        }
        return r;
    }

    private static String controllerDari(String jspPath) {
        String isi = baca(jspPath);
        Matcher m = PEMANGGILAN.matcher(isi);
        Set<String> nama = new java.util.LinkedHashSet<String>();
        while (m.find()) nama.add(m.group(1));
        StringBuilder sb = new StringBuilder();
        for (String n : nama) {
            if (sb.length() > 0) sb.append('+');
            sb.append(n);
        }
        return sb.toString();
    }

    private static boolean dispatcher(String jspPath) {
        return SapuScaffoldProbe.isiMemuat(jspPath, "dispatcher.jsp");
    }

    /**
     * Amplop yang dipakai controller. Yang menentukan adalah adanya
     * {@code put("success", true)} pada jalur sukses; tanpa itu klien generik
     * melempar dan hanya halaman khusus yang dapat membacanya.
     */
    private static String amplopDari(String namaGabungan) {
        String[] nama = namaGabungan.split("\\+");
        boolean adaGenerik = false, adaFlat = false;
        for (int i = 0; i < nama.length; i++) {
            File f = cariSumber(new File(SUMBER), nama[i] + ".java");
            if (f == null) continue;
            String isi = baca(f.getAbsolutePath());
            // Amplop generik dapat dibangun dengan dua cara, dan menghitung
            // hanya cara pertama membuat sapuan ini berbohong: controller yang
            // memakai GenericCrudResult atau meneruskan ke Generic CRUD juga
            // mengeluarkan {success, data}, tetapi tidak pernah menulis
            // put("success", true) sendiri. Pengumuman Akademis dan Pengaturan
            // Bahasa sempat terhitung ok-flat karena itu, padahal keduanya
            // memang sudah terbaca renderer generik.
            if (isi.indexOf("put(\"success\", true)") >= 0
                    || isi.indexOf("GenericCrudResult") >= 0
                    || isi.indexOf("delegateGeneric") >= 0
                    || isi.indexOf("GenericCrudHttpController") >= 0) {
                adaGenerik = true;
            } else {
                adaFlat = true;
            }
        }
        if (adaGenerik && adaFlat) return "campuran";
        if (adaGenerik) return "generik";
        if (adaFlat) return "ok-flat";
        return "?";
    }

    private static File cariSumber(File dir, String namaBerkas) {
        File[] anak = dir.listFiles();
        if (anak == null) return null;
        for (int i = 0; i < anak.length; i++) {
            if (anak[i].isDirectory()) {
                File hit = cariSumber(anak[i], namaBerkas);
                if (hit != null) return hit;
            } else if (anak[i].getName().equals(namaBerkas)) {
                return anak[i];
            }
        }
        return null;
    }

    private static String baca(String path) {
        java.io.InputStream in = null;
        try {
            in = new java.io.FileInputStream(path);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new String(bos.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) { }
        }
    }

    private static void baris(String id, String label, String url, String jsp,
            String controller, String amplop) {
        System.out.println(id + "\t" + label + "\t" + url + "\t" + jsp + "\t"
                + controller + "\t" + amplop);
    }
}
