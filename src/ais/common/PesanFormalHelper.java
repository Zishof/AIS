package ais.common;

import ais.ui.util.MyMessageboxConfig;

/**
 * {@code PesanFormalHelper} — utilitas <b>bersama (reuse)</b> di seluruh aplikasi untuk menyusun
 * &amp; menampilkan SEMUA jenis pesan (sukses, peringatan, error/kesalahan) memakai bahasa yang
 * <b>sangat formal, sopan, dan mudah dipahami</b> oleh semua pengguna — bukan hanya administrator
 * atau teknisi. Ini adalah versi UMUM (tidak terikat modul Neo Feeder) dari pola yang sebelumnya
 * dipakai khusus untuk pesan koneksi Feeder ({@code NeoFeederPesanFormalHelper}), agar SELURUH
 * alert di aplikasi — di modul manapun — bisa memakai standar yang sama.
 *
 * <h3>Standar isi pesan (WAJIB, sesuai kebijakan aplikasi)</h3>
 * Setiap pesan kesalahan yang dihasilkan kelas ini SELALU memuat 4 bagian:
 * <ol>
 *   <li><b>Sapaan formal</b> — pembuka sopan ("Yang terhormat Bapak/Ibu Pengguna,").</li>
 *   <li><b>Penyebab</b> — penjelasan serinci &amp; sedetail mungkin tentang apa yang terjadi
 *       (disuplai pemanggil, karena hanya pemanggil yang tahu konteks bisnisnya).</li>
 *   <li><b>Tindak Lanjut</b> — langkah-langkah konkret &amp; berurutan yang dapat dicoba pengguna
 *       sendiri untuk mengatasi masalah tersebut (disuplai pemanggil).</li>
 *   <li><b>Eskalasi</b> — anjuran BAKU (sama di semua pesan) menghubungi Administrator Sistem atau
 *       melaporkan ke Pengembang Sistem, WAJIB melampirkan tangkapan layar (screenshot) pada saat
 *       kesalahan terjadi, apabila langkah-langkah di atas belum menyelesaikan masalah.</li>
 * </ol>
 *
 * <h3>Multi-bahasa</h3>
 * Seluruh bagian TETAP/berulang (sapaan, label "Penyebab:"/"Tindak Lanjut:", teks eskalasi) SELALU
 * diterjemahkan satu-per-satu via {@link Common#getBahasaConfig(String)} — mekanisme kamus bahasa
 * baku aplikasi ini (dipakai juga oleh {@link MyMessageboxConfig}) — sehingga potongan teks yang
 * SAMA di setiap pemanggilan cukup diterjemahkan/di-cache SEKALI di kamus, bukan sebagai kalimat
 * gabungan yang unik tiap kali (yang akan membanjiri kamus &amp; menyulitkan cache). Isi dinamis
 * (penyebab spesifik, langkah spesifik, nilai data) disisipkan apa adanya oleh pemanggil dan tetap
 * ikut diterjemahkan best-effort oleh {@link MyMessageboxConfig#show} (yang menerjemahkan pesan
 * akhir secara keseluruhan sebagai lapisan kedua).
 *
 * <h3>Dipakai di titik pusat, bukan ditulis ulang di tiap layar</h3>
 * Cukup panggil {@link #tampilkanSukses(String, String)}, {@link #tampilkanGagal(String, String,
 * String[])}, atau {@link #tampilkanGagalException(String, Throwable, String[])} — SATU baris,
 * tanpa menulis ulang teks sapaan/label/eskalasi di tiap layar. Method {@code pesanX(...)} (tanpa
 * awalan {@code tampilkan}) hanya MENYUSUN string (tidak menampilkan apa pun) — berguna bila pesan
 * perlu disimpan dahulu ke {@code Label}/progress-bar sebelum ditampilkan belakangan.
 *
 * <h3>Kompatibilitas</h3>
 * Java 1.7 (tanpa lambda, tanpa try-with-resources multi-catch).
 *
 * @author Tim AIS
 */
public final class PesanFormalHelper {

    private PesanFormalHelper() {
    }

    // =========================================================
    // Label/teks TETAP (diterjemahkan satu-per-satu via kamus bahasa)
    // =========================================================

    private static String t(String s) {
        try {
            return Common.getBahasaConfig(s);
        } catch (Throwable e) {
            return s;
        }
    }

    private static String sapaan() {
        return t("Yang terhormat Bapak/Ibu Pengguna,");
    }

    private static String labelPenyebab() {
        return t("Penyebab:");
    }

    private static String labelTindakLanjut() {
        return t("Tindak Lanjut yang dapat Bapak/Ibu coba:");
    }

    /** Baris eskalasi BAKU — dipakai di SEMUA pesan gagal di seluruh aplikasi agar konsisten. */
    public static String eskalasi() {
        return t("Apabila Bapak/Ibu kurang memahami pesan kesalahan ini atau langkah-langkah di atas "
                + "belum berhasil mengatasi masalah, kami mohon agar segera menghubungi Administrator "
                + "Sistem di lingkungan Bapak/Ibu, atau melaporkan kejadian ini kepada Pengembang "
                + "Sistem. Untuk mempercepat proses penanganan, mohon WAJIB melampirkan tangkapan "
                + "layar (screenshot) pada saat kesalahan ini terjadi beserta waktu kejadiannya.");
    }

    // =========================================================
    // SUKSES
    // =========================================================

    /**
     * Menyusun pesan SUKSES formal.
     *
     * @param aktivitas nama aktivitas yang berhasil, mis. {@code "penyimpanan data Perkuliahan"};
     *                  boleh {@code null}/kosong
     * @param detail    rincian tambahan yang relevan (boleh {@code null}/kosong)
     * @return pesan siap tampil
     */
    public static String pesanSukses(String aktivitas, String detail) {
        StringBuilder sb = new StringBuilder();
        sb.append(sapaan()).append("\n\n");
        sb.append(t("Dengan ini kami sampaikan bahwa proses"));
        sb.append(" ").append(kosongKe(aktivitas, t("yang Bapak/Ibu jalankan")));
        sb.append(" ").append(t("telah BERHASIL diselesaikan dengan baik."));
        if (detail != null && detail.trim().length() > 0) {
            sb.append("\n\n").append(t("Rincian:")).append(" ").append(detail.trim());
        }
        sb.append("\n\n").append(t("Terima kasih atas kesabaran Bapak/Ibu."));
        return sb.toString();
    }

    /** Menampilkan pesan sukses (lihat {@link #pesanSukses(String, String)}) via {@link MyMessageboxConfig}. */
    public static void tampilkanSukses(String aktivitas, String detail) {
        tampilkanAman(pesanSukses(aktivitas, detail), "Informasi", MyMessageboxConfig.INFORMATION);
    }

    // =========================================================
    // GAGAL — UMUM (dipakai untuk SEMUA jenis error/kesalahan di aplikasi)
    // =========================================================

    /**
     * Menyusun pesan GAGAL/KESALAHAN formal — inti dari kelas ini, dipakai untuk SEMUA jenis
     * error di aplikasi (bukan hanya koneksi Feeder).
     *
     * @param aktivitas     nama aktivitas yang gagal, mis. {@code "penyimpanan data Perkuliahan"};
     *                      boleh {@code null}/kosong
     * @param penyebab      penjelasan SERINCI &amp; SEDETAIL mungkin tentang apa yang menyebabkan
     *                      kesalahan ini (WAJIB diisi pemanggil dengan penjelasan konkret, bukan
     *                      sekadar "terjadi kesalahan"); boleh {@code null}/kosong sebagai fallback
     *                      generik
     * @param langkahSolusi daftar langkah tindak lanjut berurutan yang dapat dicoba pengguna
     *                      (masing-masing SATU kalimat imperatif); boleh {@code null}/kosong
     *                      sebagai fallback generik
     * @return pesan siap tampil
     */
    public static String pesanGagal(String aktivitas, String penyebab, String[] langkahSolusi) {
        StringBuilder sb = new StringBuilder();
        sb.append(sapaan()).append("\n\n");
        sb.append(t("Mohon maaf, telah terjadi kesalahan pada saat memproses"));
        sb.append(" ").append(kosongKe(aktivitas, t("permintaan Bapak/Ibu"))).append(".\n\n");

        sb.append(labelPenyebab()).append(" ");
        if (penyebab != null && penyebab.trim().length() > 0) {
            sb.append(penyebab.trim());
        } else {
            sb.append(t("Sistem tidak dapat menentukan penyebab pasti secara otomatis. Kendala ini "
                    + "kemungkinan bersifat sementara."));
        }

        sb.append("\n\n").append(labelTindakLanjut()).append("\n");
        if (langkahSolusi != null && langkahSolusi.length > 0) {
            for (int i = 0; i < langkahSolusi.length; i++) {
                if (langkahSolusi[i] == null || langkahSolusi[i].trim().length() == 0) {
                    continue;
                }
                sb.append("  ").append(i + 1).append(". ").append(langkahSolusi[i].trim()).append("\n");
            }
        } else {
            sb.append("  1. ").append(t("Silakan ulangi proses ini beberapa saat lagi, kemungkinan "
                    + "kendala bersifat sementara.")).append("\n");
            sb.append("  2. ").append(t("Periksa kembali data/pengaturan yang berkaitan dengan proses "
                    + "ini sebelum mencoba kembali.")).append("\n");
        }

        sb.append("\n").append(eskalasi());
        return sb.toString();
    }

    /** Menampilkan pesan gagal (lihat {@link #pesanGagal}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagal(String aktivitas, String penyebab, String[] langkahSolusi) {
        tampilkanAman(pesanGagal(aktivitas, penyebab, langkahSolusi), "Terjadi Kesalahan",
                MyMessageboxConfig.EXCLAMATION);
    }

    /**
     * Varian {@link #pesanGagal} yang menerima {@link Throwable} langsung (mis. dalam blok
     * {@code catch}) — memakai {@code exception.getMessage()} sebagai rincian teknis tambahan pada
     * bagian Penyebab, digabung dengan penjelasan bisnis dari pemanggil.
     *
     * @param aktivitas       nama aktivitas yang gagal
     * @param penjelasanBisnis penjelasan penyebab dari sudut pandang bisnis/konteks pemanggil
     *                        (boleh {@code null}/kosong)
     * @param exception       exception yang tertangkap (boleh {@code null})
     * @param langkahSolusi   daftar langkah tindak lanjut (boleh {@code null})
     * @return pesan siap tampil
     */
    public static String pesanGagalException(String aktivitas, String penjelasanBisnis, Throwable exception,
            String[] langkahSolusi) {
        String penyebab;
        if (penjelasanBisnis != null && penjelasanBisnis.trim().length() > 0) {
            penyebab = penjelasanBisnis.trim();
        } else {
            penyebab = t("Proses belum dapat diselesaikan. Data tidak diubah agar tetap aman dan konsisten.");
        }
        return pesanGagal(aktivitas, penyebab, langkahSolusi);
    }

    /** Menampilkan pesan gagal-karena-exception (lihat {@link #pesanGagalException}) via {@link MyMessageboxConfig}. */
    public static void tampilkanGagalException(String aktivitas, Throwable exception, String[] langkahSolusi) {
        tampilkanGagalDenganDetail(aktivitas, null, exception, langkahSolusi);
    }

    /** Sama seperti {@link #tampilkanGagalException(String, Throwable, String[])} dengan tambahan penjelasan bisnis. */
    public static void tampilkanGagalException(String aktivitas, String penjelasanBisnis, Throwable exception,
            String[] langkahSolusi) {
        tampilkanGagalDenganDetail(aktivitas, penjelasanBisnis, exception, langkahSolusi);
    }

    /**
     * Menampilkan kendala laporan langsung di area laporan. Ini dipakai agar layar PDF/preview
     * tidak tertutup popup browser yang panjang dan sulit dibaca end user.
     */
    public static void tampilkanGagalLaporan(org.zkoss.zk.ui.Component parent, String aktivitas,
            String penjelasanBisnis, Throwable exception, String[] langkahSolusi) {
        String kode = DetailTeknisHelper.kodeRujukan();
        String pesan = pesanGagalLaporanRingkas(aktivitas, penjelasanBisnis, langkahSolusi);
        if (parent != null) {
            try {
                tempelPesanLaporan(parent, "Laporan belum siap ditampilkan", pesan, aktivitas, exception, kode);
                return;
            } catch (Throwable t) {
                ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tampilkanGagalLaporan-tempel");
            }
        }
        // Tanpa komponen induk, panel ZK tidak dapat dipasang. Pakai dialog web bersama yang
        // sudah punya Detail Error + Copy Error; toast polos hanya jalur terakhir, karena toast
        // sama sekali tidak membawa informasi teknis.
        if (tampilkanDialogWebLaporan(aktivitas, pesan, exception, langkahSolusi, kode)) {
            return;
        }
        tampilkanToastRingkas("Laporan belum siap ditampilkan", pesan + " Kode rujukan: " + kode + ".");
    }

    /**
     * Tampilkan kendala laporan lewat dialog web {@code tampilkanPesanGagalFormal}
     * (pesan-formal.js), yang sudah menyediakan tombol Detail Error dan Copy Error.
     *
     * @return {@code true} bila perintah berhasil dikirim ke klien
     */
    private static boolean tampilkanDialogWebLaporan(String aktivitas, String pesan, Throwable exception,
            String[] langkahSolusi, String kode) {
        try {
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("judul", "Laporan belum siap ditampilkan");
            data.put("message", pesan);
            org.json.JSONArray solusi = new org.json.JSONArray();
            if (langkahSolusi != null) {
                for (int i = 0; i < langkahSolusi.length; i++) {
                    if (langkahSolusi[i] != null && langkahSolusi[i].trim().length() > 0) {
                        solusi.put(langkahSolusi[i].trim());
                    }
                }
            }
            data.put("solusi", solusi);
            String teknis = DetailTeknisHelper.teksTeknis(aktivitas, exception, null, kode);
            data.put("teknis", teknis);
            data.put("referensi", kode);
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "if(typeof tampilkanPesanGagalFormal==='function'){tampilkanPesanGagalFormal("
                    + org.json.JSONObject.quote(kosongKe(aktivitas, "pembuatan laporan")) + ","
                    + data.toString() + ");}else{"
                    + jsDialogDetail("Laporan belum siap ditampilkan",
                            pesan + " Kode rujukan: " + kode + ".", aktivitas, teknis, kode)
                    + "}");
            return true;
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tampilkanDialogWebLaporan");
            return false;
        }
    }

    /**
     * Pada layar ZK, gunakan dialog web bersama yang menyediakan Detail Error dan Copy Error.
     * Bila halaman lama belum memuat pesan-formal.js, tetap jatuh aman ke Messagebox biasa.
     */
    private static void tampilkanGagalDenganDetail(String aktivitas, String penjelasanBisnis,
            Throwable exception, String[] langkahSolusi) {
        try {
            if (adalahKonteksLaporan(aktivitas, penjelasanBisnis)) {
                tampilkanGagalLaporan(null, aktivitas, penjelasanBisnis, exception, langkahSolusi);
                return;
            }
            org.json.JSONObject data = new org.json.JSONObject();
            data.put("judul", "Ada kendala");
            data.put("message", (penjelasanBisnis == null || penjelasanBisnis.trim().length() == 0)
                    ? "Proses belum dapat diselesaikan. Data tidak diubah agar tetap aman dan konsisten."
                    : penjelasanBisnis.trim());
            org.json.JSONArray solusi = new org.json.JSONArray();
            if (langkahSolusi != null) {
                for (int i = 0; i < langkahSolusi.length; i++) {
                    if (langkahSolusi[i] != null && langkahSolusi[i].trim().length() > 0) solusi.put(langkahSolusi[i].trim());
                }
            }
            data.put("solusi", solusi);
            String kode = "ZK-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
            String teknis = DetailTeknisHelper.teksTeknis(aktivitas, exception, null, kode);
            data.put("teknis", teknis);
            data.put("referensi", kode);
            org.zkoss.zk.ui.util.Clients.evalJavaScript(
                    "if(typeof tampilkanPesanGagalFormal==='function'){tampilkanPesanGagalFormal("
                    + org.json.JSONObject.quote(kosongKe(aktivitas, "proses aplikasi")) + ","
                    + data.toString() + ");}else{"
                    + jsDialogDetail("Ada kendala", pesanGagalRingkas(penjelasanBisnis), aktivitas, teknis, kode)
                    + "}");
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tampilkanGagalDenganDetail");
            tampilkanToastRingkas("Ada kendala", pesanGagalRingkas(penjelasanBisnis));
        }
    }

    private static String detailTeknis(Throwable error) {
        if (error == null) return "Tidak ada detail exception.";
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    // =========================================================
    // Util internal
    // =========================================================

    private static String kosongKe(String s, String pengganti) {
        return (s == null || s.trim().length() == 0) ? pengganti : s.trim();
    }

    private static boolean adalahKonteksLaporan(String aktivitas, String penjelasanBisnis) {
        String teks = (kosongKe(aktivitas, "") + " " + kosongKe(penjelasanBisnis, "")).toLowerCase();
        return teks.indexOf("laporan") >= 0 || teks.indexOf("report") >= 0 || teks.indexOf("pdf") >= 0
                || teks.indexOf("jasper") >= 0 || teks.indexOf("cetak") >= 0;
    }

    private static String pesanGagalRingkas(String penjelasanBisnis) {
        if (penjelasanBisnis != null && penjelasanBisnis.trim().length() > 0) {
            return penjelasanBisnis.trim();
        }
        return "Proses belum dapat diselesaikan. Silakan coba lagi. Jika masih terjadi, hubungi admin sistem.";
    }

    private static String pesanGagalLaporanRingkas(String aktivitas, String penjelasanBisnis,
            String[] langkahSolusi) {
        StringBuilder sb = new StringBuilder();
        sb.append("Data laporan belum bisa dimuat.");
        if (penjelasanBisnis != null && penjelasanBisnis.trim().length() > 0) {
            sb.append(" ").append(sederhanakanPesanLaporan(penjelasanBisnis.trim()));
        } else {
            sb.append(" Biasanya karena filter/periode belum sesuai, data sumber belum lengkap, atau sistem sedang sibuk.");
        }
        sb.append(" Cek filter/periode dan kelengkapan data, lalu klik Tampilkan atau Cetak lagi.");
        if (langkahSolusi != null && langkahSolusi.length > 0) {
            String tambahan = langkahRingkas(langkahSolusi);
            if (tambahan.length() > 0) {
                sb.append(" ").append(tambahan);
            }
        }
        sb.append(" Jika tetap gagal, klik Lihat Detail Error lalu copy error tersebut untuk dikirim ke admin.");
        return sb.toString();
    }

    private static String sederhanakanPesanLaporan(String pesan) {
        String teks = pesan;
        teks = teks.replace("Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena", "Kemungkinan ada");
        teks = teks.replace("Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh", "Kemungkinan ada");
        teks = teks.replace("Bapak/Ibu", "Anda");
        teks = teks.replace("data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", "data yang belum lengkap, filter yang belum sesuai, atau sistem sedang sibuk.");
        return teks;
    }

    private static String langkahRingkas(String[] langkahSolusi) {
        StringBuilder sb = new StringBuilder();
        int nomor = 1;
        for (int i = 0; i < langkahSolusi.length && nomor <= 1; i++) {
            if (langkahSolusi[i] == null || langkahSolusi[i].trim().length() == 0) {
                continue;
            }
            if (nomor == 1) {
                sb.append("Langkah cepat: ");
            } else {
                sb.append(" ");
            }
            sb.append(langkahSolusi[i].trim().replace("Bapak/Ibu", "Anda"));
            nomor++;
        }
        return sb.toString();
    }

    private static void tempelPesanLaporan(org.zkoss.zk.ui.Component parent, String judul, String pesan,
            String konteks, Throwable exception, String kode) {
        java.util.List anak = new java.util.ArrayList(parent.getChildren());
        for (int i = 0; i < anak.size(); i++) {
            Object obj = anak.get(i);
            if (obj instanceof org.zkoss.zk.ui.Component) {
                org.zkoss.zk.ui.Component child = (org.zkoss.zk.ui.Component) obj;
                if (Boolean.TRUE.equals(child.getAttribute("aisInlineReportMessage"))) {
                    child.detach();
                }
            }
        }

        org.zkoss.zul.Div box = new org.zkoss.zul.Div();
        box.setAttribute("aisInlineReportMessage", Boolean.TRUE);
        box.setSclass("ais-inline-report-message");
        box.setStyle("margin:8px 10px;padding:10px 12px;border:1px solid #fdba74;"
                + "border-left:4px solid #f97316;border-radius:8px;background:#fffaf0;color:#7c2d12;"
                + "font-size:12px;line-height:1.45;box-shadow:0 1px 3px rgba(15,23,42,.10);");
        box.appendChild(new org.zkoss.zul.Html("<div style='display:flex;gap:10px;align-items:flex-start;'>"
                + "<div style='width:22px;height:22px;border-radius:50%;background:#ffedd5;color:#c2410c;"
                + "text-align:center;font-weight:700;line-height:22px;flex:0 0 22px;'>!</div>"
                + "<div style='min-width:0;'><div style='font-weight:700;margin-bottom:3px;'>"
                + html(judul) + "</div><div style='color:#854d0e;'>" + html(pesan) + "</div>"
                + "<div style='color:#854d0e;margin-top:6px;'>Kode rujukan: <b>" + html(kode)
                + "</b></div></div></div>"));

        // Tombol "Detail Informasi Teknis" + panel berisi exception yang sebenarnya, dapat
        // disalin pengguna untuk dikirim ke pengembang. Sebelumnya parameter exception diterima
        // tetapi TIDAK PERNAH dipakai, sehingga pesan ramah ini membuang seluruh jejak teknis.
        DetailTeknisHelper.pasangPanel(box, konteks, exception, null, kode);

        if (parent.getFirstChild() != null) {
            parent.insertBefore(box, parent.getFirstChild());
        } else {
            box.setParent(parent);
        }
        try {
            org.zkoss.zk.ui.util.Clients.scrollIntoView(box);
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tempelPesanLaporan-scroll");
        }
    }

    private static void tampilkanToastRingkas(String judul, String pesan) {
        try {
            org.zkoss.zk.ui.util.Clients.evalJavaScript(jsToast(judul, pesan));
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "PesanFormalHelper.tampilkanToastRingkas");
            tampilkanAman(judul + "\n\n" + pesan, "Informasi", MyMessageboxConfig.EXCLAMATION);
        }
    }

    private static String jsToast(String judul, String pesan) {
        return "(function(t,m){try{var esc=function(s){return String(s).replace(/&/g,'&amp;')"
                + ".replace(/</g,'&lt;').replace(/>/g,'&gt;');};"
                + "var d=document.createElement('div');"
                + "d.style.cssText='position:fixed;z-index:2147483647;left:50%;top:18px;"
                + "transform:translateX(-50%);max-width:720px;background:#fff7ed;color:#7c2d12;"
                + "border:1px solid #fdba74;border-radius:10px;padding:12px 16px;"
                + "box-shadow:0 12px 30px rgba(15,23,42,.22);font:13px/1.45 Arial,sans-serif;';"
                + "d.innerHTML='<b>'+esc(t)+'</b><br/>'+esc(m);document.body.appendChild(d);"
                + "setTimeout(function(){try{if(d.parentNode)d.parentNode.removeChild(d);}catch(e){}},9000);"
                + "}catch(e){}})(" + org.json.JSONObject.quote(kosongKe(judul, "Informasi")) + ","
                + org.json.JSONObject.quote(kosongKe(pesan, "Proses belum dapat diselesaikan.")) + ");";
    }

    private static String html(String s) {
        if (s == null) {
            return "";
        }
        String r = s;
        r = r.replace("&", "&amp;");
        r = r.replace("<", "&lt;");
        r = r.replace(">", "&gt;");
        r = r.replace("\"", "&quot;");
        r = r.replace("'", "&#39;");
        r = r.replace("\n", "<br/>");
        return r;
    }

    /** Tampilkan via {@link MyMessageboxConfig}, gagal-aman (tidak pernah melempar ke pemanggil). */
    private static void tampilkanAman(String pesan, String judul, String ikon) {
        try {
            MyMessageboxConfig.show(pesan, judul, MyMessageboxConfig.OK, ikon);
        } catch (Throwable t) {
            ais.common.ErrorAuditUtil.record(t, "auto-audit(empty-catch) src/ais/common/PesanFormalHelper.java:tampilkanAman");
        }
    }
}
