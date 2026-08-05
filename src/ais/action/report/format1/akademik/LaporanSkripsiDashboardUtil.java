package ais.action.report.format1.akademik;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper ringan untuk dashboard laporan skripsi/sidang/yudisium.
 * Dibuat dengan HTML + CSS agar tidak membutuhkan JFreeChart.
 * Kompatibel Java 1.7.
 */
class LaporanSkripsiDashboardUtil {

    private static final int TABLE_LIMIT = 80;

    private LaporanSkripsiDashboardUtil() {
    }

    static String renderRekapSidang(List maps, String filterInfo) {
        DashboardSummary summary = hitungSummarySidang(maps);
        StringBuilder html = start("Dashboard Rekapitulasi Sidang",
                "Ringkasan ini membantu melihat jumlah peserta, status sidang, dosen yang terlibat, dan sebaran nilai dari data yang sedang difilter.",
                filterInfo);
        html.append(cards(new String[][] {
                { "Total Baris", formatInt(summary.total), "Jumlah data sesuai filter." },
                { "Sudah Sidang", formatInt(summary.sudah), "Peserta yang telah selesai sidang." },
                { "Belum Sidang", formatInt(summary.belum), "Peserta yang belum selesai sidang." },
                { "Dosen Terlibat", formatInt(summary.dosenUnik), "Dosen pembimbing atau penguji pada data ini." },
                { "Rata-rata Nilai", formatDouble(summary.avgNilai), "Nilai akhir rata-rata dari data yang tersedia." }
        }));

        html.append(section("Status Sidang",
                "Perbandingan peserta yang sudah dan belum sidang untuk membantu memantau penyelesaian tugas akhir.",
                twoColumn(donut(summary.sudah, summary.belum, "Sudah", "Belum"),
                        bars(countBy(maps, "pembimbing"), "Peran dosen dalam laporan", 10))));

        html.append(section("Sebaran Nilai dan Program Studi",
                "Nilai huruf dan program studi terlihat berdampingan agar data yang menonjol cepat ditemukan.",
                twoColumn(bars(countBy(maps, "nilaihuruf"), "Nilai huruf", 8),
                        bars(countBy(maps, "jur"), "Program studi", 10))));

        html.append(section("Daftar Rekap Sidang",
                "Data utama ditampilkan singkat agar mudah diperiksa sebelum mencetak laporan resmi.",
                table(maps, new String[] { "pembimbing", "dosen", "nim", "nama_mhs", "jur", "judul", "status_sidang",
                        "nilaihuruf", "tanggal_sidang" },
                        new String[] { "Peran", "Dosen", "NIM", "Mahasiswa", "Prodi", "Judul", "Sidang", "Huruf",
                                "Tanggal" })));
        return finish(html);
    }

    static String renderSidang(List maps, String filterInfo) {
        DashboardSummary summary = hitungSummarySidang(maps);
        StringBuilder html = start("Dashboard Laporan Sidang",
                "Daftar ini memudahkan pengecekan peserta sidang, susunan dosen, status sidang, dan hasil nilai sebelum laporan dicetak.",
                filterInfo);
        html.append(cards(new String[][] {
                { "Total Peserta", formatInt(summary.total), "Mahasiswa pada filter saat ini." },
                { "Sudah Sidang", formatInt(summary.sudah), "Sudah memiliki status sidang selesai." },
                { "Belum Sidang", formatInt(summary.belum), "Masih perlu dipantau jadwal atau hasilnya." },
                { "Rata-rata IPK", formatDouble(summary.avgIpk), "IPK rata-rata dari data yang tersedia." },
                { "Rata-rata Nilai", formatDouble(summary.avgNilai), "Nilai sidang rata-rata dari peserta." }
        }));

        html.append(section("Kondisi Peserta",
                "Status dan sebaran prodi membantu melihat kelompok mahasiswa yang membutuhkan tindak lanjut.",
                twoColumn(donut(summary.sudah, summary.belum, "Sudah", "Belum"),
                        bars(countBy(maps, "jur"), "Peserta per program studi", 10))));

        html.append(section("Nilai Sidang",
                "Sebaran nilai memperlihatkan hasil akhir peserta dalam bentuk yang mudah dibandingkan.",
                twoColumn(bars(countBy(maps, "nilaihuruf"), "Nilai huruf", 10),
                        bars(countBy(maps, "status_aktif"), "Status mahasiswa", 10))));

        html.append(section("Daftar Peserta Sidang",
                "Susunan dosen dan hasil sidang ditampilkan ringkas untuk pemeriksaan cepat.",
                table(maps, new String[] { "nim", "nama_mhs", "jur", "judul", "dosen1", "dosen2", "dosen3",
                        "status_sidang", "totalnilai", "nilaihuruf" },
                        new String[] { "NIM", "Mahasiswa", "Prodi", "Judul", "Pembimbing I", "Pembimbing II",
                                "Penguji I", "Sidang", "Nilai", "Huruf" })));
        return finish(html);
    }

    static String renderJudisium(List maps, String filterInfo) {
        DashboardSummary summary = hitungSummaryJudisium(maps);
        StringBuilder html = start("Dashboard Rekapitulasi Yudisium",
                "Ringkasan ini membantu melihat peserta lulus, IPK, nilai akhir, predikat, dan gelombang yudisium dari data terpilih.",
                filterInfo);
        html.append(cards(new String[][] {
                { "Total Lulus", formatInt(summary.total), "Mahasiswa lulus sesuai filter." },
                { "Rata-rata IPK", formatDouble(summary.avgIpk), "IPK rata-rata peserta yudisium." },
                { "Rata-rata Nilai", formatDouble(summary.avgNilai), "Nilai akhir rata-rata peserta." },
                { "Predikat", formatInt(countBy(maps, "judisium").size()), "Jumlah predikat berbeda pada data ini." },
                { "Gelombang", formatInt(countBy(maps, "gelombang").size()), "Jumlah gelombang yang berisi peserta." }
        }));

        html.append(section("Predikat dan Gelombang",
                "Predikat kelulusan dan gelombang terlihat jelas untuk memudahkan rekap akhir akademik.",
                twoColumn(bars(countBy(maps, "judisium"), "Predikat yudisium", 10),
                        bars(countBy(maps, "gelombang"), "Peserta per gelombang", 10))));

        html.append(section("Rentang Nilai",
                "Rentang nilai akhir membantu melihat sebaran hasil kelulusan secara cepat.",
                twoColumn(bars(countRange(maps, "nilai"), "Rentang nilai", 10),
                        bars(countRange(maps, "ipk"), "Rentang IPK", 10))));

        html.append(section("Daftar Peserta Yudisium",
                "Data peserta ditampilkan ringkas untuk validasi sebelum mencetak berita acara atau rekap resmi.",
                table(maps, new String[] { "gelombang", "nim", "nama", "sksk", "ipk", "nilai", "huruf", "judisium",
                        "tanggal_lulus" },
                        new String[] { "Gelombang", "NIM", "Mahasiswa", "SKS", "IPK", "Nilai", "Huruf", "Predikat",
                                "Tanggal Lulus" })));
        return finish(html);
    }

    static String empty(String title, String filterInfo) {
        StringBuilder html = start(title,
                "Belum ada data yang cocok dengan pilihan filter. Silakan ubah filter lalu tampilkan kembali.", filterInfo);
        html.append("<div class='empty'>Tidak ada data yang dapat ditampilkan.</div>");
        return finish(html);
    }

    private static StringBuilder start(String title, String description, String filterInfo) {
        StringBuilder html = new StringBuilder();
        html.append("<style>");
        html.append(".skripsiDash{font-family:Inter,Segoe UI,Arial,sans-serif;background:#f8fafc;padding:18px;color:#0f172a;line-height:1.45}");
        html.append(".skripsiDash *{box-sizing:border-box}.skripsiHero{background:linear-gradient(135deg, rgba(0,0,0,.35), rgba(0,0,0,0) 55%), linear-gradient(135deg, var(--ais-theme-primary,#1d4ed8) 0%, var(--ais-theme-primary,#1d4ed8) 45%, var(--ais-theme-accent,#06b6d4) 100%);color:white;border-radius:18px;padding:22px 24px;margin-bottom:16px;box-shadow:0 14px 30px rgba(15,23,42,.18)}");
        html.append(".skripsiHero h2{margin:0 0 8px;font-size:24px}.skripsiHero p{margin:0;max-width:960px;color:#dbeafe}.filter{margin-top:14px;display:inline-block;background:rgba(255,255,255,.14);border:1px solid rgba(255,255,255,.25);border-radius:999px;padding:7px 12px;font-size:12px;color:#eff6ff}");
        html.append(".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:12px;margin-bottom:16px}.card{background:white;border:1px solid #e2e8f0;border-radius:16px;padding:15px;box-shadow:0 8px 22px rgba(15,23,42,.06)}.card .label{font-size:12px;color:#64748b}.card .value{font-size:25px;font-weight:800;margin:5px 0;color:#0f172a}.card .note{font-size:11px;color:#64748b}");
        html.append(".section{background:white;border:1px solid #e2e8f0;border-radius:18px;padding:16px;margin-bottom:16px;box-shadow:0 8px 22px rgba(15,23,42,.05)}.section h3{font-size:17px;margin:0 0 4px}.section .desc{font-size:12px;color:#64748b;margin-bottom:14px}.two{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:16px}");
        html.append(".chartTitle{font-weight:700;margin-bottom:10px;color:#1e293b}.barRow{display:grid;grid-template-columns:minmax(95px,190px) 1fr 48px;gap:8px;align-items:center;margin:8px 0;font-size:12px}.barLabel{white-space:nowrap;overflow:hidden;text-overflow:ellipsis;color:#334155}.barBg{height:11px;background:#e2e8f0;border-radius:999px;overflow:hidden}.barFill{height:11px;background:linear-gradient(90deg,#2563eb,#22c55e);border-radius:999px}.barVal{text-align:right;color:#475569}");
        html.append(".donutWrap{display:flex;align-items:center;gap:18px;min-height:180px}.donut{width:150px;height:150px;border-radius:50%;background:conic-gradient(#2563eb var(--p),#e2e8f0 0);display:flex;align-items:center;justify-content:center}.donutCenter{width:92px;height:92px;border-radius:50%;background:white;display:flex;align-items:center;justify-content:center;font-weight:800;font-size:22px;color:#0f172a;box-shadow:inset 0 0 0 1px #e2e8f0}.legend{font-size:13px;color:#334155}.legend b{display:inline-block;min-width:84px}.dot{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:6px;background:#2563eb}.dot.gray{background:#cbd5e1}");
        html.append(".tableWrap{overflow:auto;border-radius:14px;border:1px solid #e2e8f0}.data{width:100%;border-collapse:collapse;font-size:12px}.data th{background:#f1f5f9;text-align:left;padding:10px;color:#334155;position:sticky;top:0}.data td{padding:9px 10px;border-top:1px solid #e2e8f0;vertical-align:top}.data tr:nth-child(even) td{background:#fbfdff}.muted{color:#64748b;font-size:12px;margin-top:8px}.empty{background:white;border:1px dashed #cbd5e1;border-radius:18px;padding:34px;text-align:center;color:#64748b}");
        html.append("</style><div class='skripsiDash'>");
        html.append("<div class='skripsiHero'><h2>").append(esc(title)).append("</h2><p>").append(esc(description)).append("</p>");
        if (filterInfo != null && filterInfo.trim().length() > 0) {
            html.append("<div class='filter'>").append(esc(filterInfo)).append("</div>");
        }
        html.append("</div>");
        return html;
    }

    private static String finish(StringBuilder html) {
        html.append("</div>");
        return html.toString();
    }

    private static String cards(String[][] data) {
        StringBuilder html = new StringBuilder("<div class='cards'>");
        for (int i = 0; i < data.length; i++) {
            html.append("<div class='card'><div class='label'>").append(esc(data[i][0])).append("</div><div class='value'>")
                    .append(esc(data[i][1])).append("</div><div class='note'>").append(esc(data[i][2])).append("</div></div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String section(String title, String desc, String content) {
        return "<div class='section'><h3>" + esc(title) + "</h3><div class='desc'>" + esc(desc) + "</div>" + content
                + "</div>";
    }

    private static String twoColumn(String left, String right) {
        return "<div class='two'><div>" + left + "</div><div>" + right + "</div></div>";
    }

    private static String donut(int a, int b, String labelA, String labelB) {
        int total = a + b;
        int percent = total <= 0 ? 0 : Math.round((a * 100.0f) / total);
        StringBuilder html = new StringBuilder();
        html.append("<div class='chartTitle'>Status ringkas</div><div class='donutWrap'>");
        html.append("<div class='donut' style='--p:").append(percent).append("%'><div class='donutCenter'>")
                .append(percent).append("%</div></div>");
        html.append("<div class='legend'><div><span class='dot'></span><b>").append(esc(labelA)).append("</b> ")
                .append(formatInt(a)).append("</div><div><span class='dot gray'></span><b>").append(esc(labelB))
                .append("</b> ").append(formatInt(b)).append("</div><div class='muted'>Persentase utama dihitung dari total data.</div></div>");
        html.append("</div>");
        return html.toString();
    }

    private static String bars(Map counts, String title, int maxRows) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='chartTitle'>").append(esc(title)).append("</div>");
        if (counts == null || counts.isEmpty()) {
            html.append("<div class='muted'>Belum ada data.</div>");
            return html.toString();
        }
        List entries = sortedEntries(counts);
        int max = 1;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry e = (Map.Entry) entries.get(i);
            Number n = (Number) e.getValue();
            if (n.intValue() > max) {
                max = n.intValue();
            }
        }
        int limit = Math.min(maxRows, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry e = (Map.Entry) entries.get(i);
            int v = ((Number) e.getValue()).intValue();
            int pct = Math.max(2, Math.round((v * 100.0f) / max));
            html.append("<div class='barRow'><div class='barLabel' title='").append(esc(String.valueOf(e.getKey())))
                    .append("'>").append(esc(String.valueOf(e.getKey()))).append("</div><div class='barBg'><div class='barFill' style='width:")
                    .append(pct).append("%'></div></div><div class='barVal'>").append(formatInt(v)).append("</div></div>");
        }
        if (entries.size() > limit) {
            html.append("<div class='muted'>Menampilkan ").append(limit).append(" dari ").append(entries.size())
                    .append(" kelompok.</div>");
        }
        return html.toString();
    }

    private static String table(List maps, String[] keys, String[] headers) {
        StringBuilder html = new StringBuilder();
        html.append("<div class='tableWrap'><table class='data'><thead><tr>");
        for (int i = 0; i < headers.length; i++) {
            html.append("<th>").append(esc(headers[i])).append("</th>");
        }
        html.append("</tr></thead><tbody>");
        int size = maps == null ? 0 : maps.size();
        int limit = Math.min(TABLE_LIMIT, size);
        for (int i = 0; i < limit; i++) {
            Map m = (Map) maps.get(i);
            html.append("<tr>");
            for (int k = 0; k < keys.length; k++) {
                html.append("<td>").append(esc(formatValue(m.get(keys[k])))).append("</td>");
            }
            html.append("</tr>");
        }
        if (limit == 0) {
            html.append("<tr><td colspan='").append(headers.length).append("'>Belum ada data.</td></tr>");
        }
        html.append("</tbody></table></div>");
        if (size > limit) {
            html.append("<div class='muted'>Menampilkan ").append(limit).append(" baris pertama dari ").append(size)
                    .append(" baris. Gunakan cetak PDF untuk melihat keseluruhan format resmi.</div>");
        }
        return html.toString();
    }

    private static DashboardSummary hitungSummarySidang(List maps) {
        DashboardSummary s = new DashboardSummary();
        if (maps == null) {
            return s;
        }
        Map dosen = new HashMap();
        double nilai = 0.0;
        int nilaiCount = 0;
        double ipk = 0.0;
        int ipkCount = 0;
        for (int i = 0; i < maps.size(); i++) {
            Map m = (Map) maps.get(i);
            s.total++;
            String status = safe(m.get("status_sidang"));
            if ("sudah".equalsIgnoreCase(status)) {
                s.sudah++;
            } else {
                s.belum++;
            }
            addUnique(dosen, m.get("dosen"));
            addUnique(dosen, m.get("dosen1"));
            addUnique(dosen, m.get("dosen2"));
            addUnique(dosen, m.get("dosen3"));
            addUnique(dosen, m.get("dosen4"));
            addUnique(dosen, m.get("dosen5"));
            Double n = toDouble(m.get("totalnilai"));
            if (n == null) {
                n = toDouble(m.get("nilai"));
            }
            if (n != null) {
                nilai += n.doubleValue();
                nilaiCount++;
            }
            Double p = toDouble(m.get("ipk"));
            if (p != null) {
                ipk += p.doubleValue();
                ipkCount++;
            }
        }
        s.dosenUnik = dosen.size();
        s.avgNilai = nilaiCount == 0 ? 0.0 : nilai / nilaiCount;
        s.avgIpk = ipkCount == 0 ? 0.0 : ipk / ipkCount;
        return s;
    }

    private static DashboardSummary hitungSummaryJudisium(List maps) {
        DashboardSummary s = new DashboardSummary();
        if (maps == null) {
            return s;
        }
        double nilai = 0.0;
        int nilaiCount = 0;
        double ipk = 0.0;
        int ipkCount = 0;
        for (int i = 0; i < maps.size(); i++) {
            Map m = (Map) maps.get(i);
            s.total++;
            Double n = toDouble(m.get("nilai"));
            if (n != null) {
                nilai += n.doubleValue();
                nilaiCount++;
            }
            Double p = toDouble(m.get("ipk"));
            if (p != null) {
                ipk += p.doubleValue();
                ipkCount++;
            }
        }
        s.avgNilai = nilaiCount == 0 ? 0.0 : nilai / nilaiCount;
        s.avgIpk = ipkCount == 0 ? 0.0 : ipk / ipkCount;
        return s;
    }

    private static Map countBy(List maps, String key) {
        Map result = new LinkedHashMap();
        if (maps == null) {
            return result;
        }
        for (int i = 0; i < maps.size(); i++) {
            Map m = (Map) maps.get(i);
            String value = safe(m.get(key));
            if (value.length() == 0 || "null".equalsIgnoreCase(value)) {
                value = "Tidak diisi";
            }
            Integer count = (Integer) result.get(value);
            result.put(value, count == null ? new Integer(1) : new Integer(count.intValue() + 1));
        }
        return result;
    }

    private static Map countRange(List maps, String key) {
        Map result = new LinkedHashMap();
        result.put("0 - 1,99", new Integer(0));
        result.put("2,00 - 2,74", new Integer(0));
        result.put("2,75 - 3,24", new Integer(0));
        result.put("3,25 - 3,74", new Integer(0));
        result.put("3,75 - 4,00+", new Integer(0));
        result.put("Tidak ada nilai", new Integer(0));
        if (maps == null) {
            return result;
        }
        for (int i = 0; i < maps.size(); i++) {
            Map m = (Map) maps.get(i);
            Double v = toDouble(m.get(key));
            String label;
            if (v == null) {
                label = "Tidak ada nilai";
            } else if (v.doubleValue() < 2.0) {
                label = "0 - 1,99";
            } else if (v.doubleValue() < 2.75) {
                label = "2,00 - 2,74";
            } else if (v.doubleValue() < 3.25) {
                label = "2,75 - 3,24";
            } else if (v.doubleValue() < 3.75) {
                label = "3,25 - 3,74";
            } else {
                label = "3,75 - 4,00+";
            }
            Integer count = (Integer) result.get(label);
            result.put(label, new Integer(count == null ? 1 : count.intValue() + 1));
        }
        return result;
    }

    private static List sortedEntries(Map map) {
        List entries = new ArrayList(map.entrySet());
        Collections.sort(entries, new Comparator() {
            public int compare(Object o1, Object o2) {
                Map.Entry e1 = (Map.Entry) o1;
                Map.Entry e2 = (Map.Entry) o2;
                int v1 = ((Number) e1.getValue()).intValue();
                int v2 = ((Number) e2.getValue()).intValue();
                if (v1 == v2) {
                    return String.valueOf(e1.getKey()).compareTo(String.valueOf(e2.getKey()));
                }
                return v2 - v1;
            }
        });
        return entries;
    }

    private static void addUnique(Map map, Object value) {
        String s = safe(value);
        if (s.length() > 0 && !"null".equalsIgnoreCase(s)) {
            map.put(s, Boolean.TRUE);
        }
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date) {
            try {
                return new SimpleDateFormat("dd-MM-yyyy").format((Date) value);
            } catch (Exception e) {
                return value.toString();
            }
        }
        if (value instanceof Number) {
            return formatDouble(((Number) value).doubleValue());
        }
        return String.valueOf(value);
    }

    private static String formatInt(int value) {
        return new DecimalFormat("#,##0").format(value);
    }

    private static String formatDouble(double value) {
        return new DecimalFormat("#,##0.##").format(value);
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return new Double(((Number) value).doubleValue());
        }
        try {
            return new Double(String.valueOf(value).replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        String s = value;
        s = s.replace("&", "&amp;");
        s = s.replace("<", "&lt;");
        s = s.replace(">", "&gt;");
        s = s.replace("\"", "&quot;");
        s = s.replace("'", "&#39;");
        return s;
    }

    private static class DashboardSummary {
        int total;
        int sudah;
        int belum;
        int dosenUnik;
        double avgNilai;
        double avgIpk;
    }
}
