package ais.action.master.sapto.util;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zss.model.Worksheet;
import org.zkoss.zss.ui.Spreadsheet;
import org.zkoss.zss.ui.impl.Utils;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listfoot;
import org.zkoss.zul.Listfooter;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Timer;
import org.zkoss.zul.event.PagingEvent;

import ais.common.Common;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MySpreadsheet;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.ZkCompat;

/**
 * Core SAPTO display utility — loads the background-thread result and renders
 * three tabs: Tabel (default), Grafik, and Excel.
 */
public class SaptoUtil {

    public static final String MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static void displayWorksheet(final Label label, final String sheetCode,
                                        final org.zkoss.zk.ui.Component center, final int col) {
        displayWorksheet(label, sheetCode, center, col, null);
    }

    @SuppressWarnings("rawtypes")
    public static void displayWorksheet(final Label label, final String sheetCode,
                                        final org.zkoss.zk.ui.Component center, final int col,
                                        final EventListener onCellClick) {

        final boolean hasClickDetail = onCellClick != null;
        final SaptoGridConfig.Config cfg = SaptoGridConfig.getConfig(sheetCode);

        final Timer timer = new Timer(1000);
        timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
        timer.setRepeats(true);
        Clients.showBusy(label.getValue());

        timer.addEventListener("onTimer", new EventListener() {

            @SuppressWarnings("unchecked")
            @Override
            public void onEvent(Event arg0) throws Exception {
                Clients.showBusy(label.getValue());
                if (!label.getValue().isEmpty()) return;

                timer.stop();
                timer.detach();
                Common.clear(center);
                Clients.clearBusy();

                final List<List> datas = (List<List>) label.getAttribute("datas");

                // ── Outer layout: North = description, Center = tabs ──────
                Borderlayout outerBl = new MyBorderlayout();
                outerBl.setParent(center);

                North descNorth = new North();
                descNorth.setParent(outerBl);
                ZkCompat.setFlex(descNorth, true);
                buildDescriptionPanel(descNorth, cfg.description);

                Center tabCenter = new Center();
                tabCenter.setParent(outerBl);
                ZkCompat.setFlex(tabCenter, true);

                // ── Tabbox ───────────────────────────────────────────────
                Tabbox tabbox = new Tabbox();
                tabbox.setHeight("100%");
                tabbox.setWidth("100%");
                tabbox.setParent(tabCenter);

                Tabs tabs = new Tabs();
                tabs.setParent(tabbox);
                Tabpanels panels = new Tabpanels();
                panels.setParent(tabbox);

                // Tab 1: Tabel
                Tab tabTable = new Tab("  Tabel Data  ");
                tabTable.setParent(tabs);
                Tabpanel panelTable = new ais.ui.util.MyTabpanel();
                panelTable.setStyle("overflow:auto;padding:4px");
                panelTable.setParent(panels);

                // Tab 2: Grafik
                Tab tabChart = new Tab("  Grafik & Analisis  ");
                tabChart.setParent(tabs);
                Tabpanel panelChart = new ais.ui.util.MyTabpanel();
                panelChart.setStyle("overflow:auto;padding:4px");
                panelChart.setParent(panels);

                // Tab 3: Excel
                Tab tabExcel = new Tab("  Tampilan Excel  ");
                tabExcel.setParent(tabs);
                Tabpanel panelExcel = new ais.ui.util.MyTabpanel();
                panelExcel.setStyle("padding:0");
                panelExcel.setParent(panels);

                // ── Tab 1: Listbox table ─────────────────────────────────
                buildTableTab(panelTable, datas, cfg, hasClickDetail);

                // ── Tab 2: Chart panel ───────────────────────────────────
                buildChartTab(panelChart, datas, cfg, sheetCode);

                // ── Tab 3: Excel spreadsheet ─────────────────────────────
                final Spreadsheet excelku = new MySpreadsheet();
                center.setAttribute("excelku", excelku);

                Borderlayout excelBl = new MyBorderlayout();
                excelBl.setParent(panelExcel);

                Center excelCenter = new Center();
                ZkCompat.setFlex(excelCenter, true);
                excelCenter.setParent(excelBl);
                excelku.setParent(excelCenter);

                South excelSouth = new South();
                excelBl.appendChild(excelSouth);

                if (hasClickDetail) {
                    excelSouth.appendChild(new MyLabelBolder("Klik angka/data pada tabel Excel untuk melihat rincian"));
                } else {
                    final MyToolbarbuttonConfig btnDl = new MyToolbarbuttonConfig(
                        "Download File Excel", "/img/excel.png");
                    EventListener dlListener = new EventListener() {
                        @Override
                        public void onEvent(Event event) throws Exception {
                            try {
                                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                                excelku.getBook().write(bout);
                                bout.close();
                                Filedownload.save(bout.toByteArray(), MIME_XLSX, sheetCode + ".xlsx");
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:165");
                                // ignore write errors
                            }
                        }
                    };
                    btnDl.addEventListener("onClick", dlListener);
                    if (label != null) label.setAttribute("downloadExcel", dlListener);
                    btnDl.setParent(excelSouth);
                }

                boolean templateAda = false;
                try {
                    String realPath = org.zkoss.zk.ui.Executions.getCurrent()
                        .getDesktop().getWebApp()
                        .getRealPath("/WEB-INF/sapto/" + sheetCode + ".xlsx");
                    if (realPath != null && new java.io.File(realPath).exists()) {
                        excelku.setSrc("../../WEB-INF/sapto/" + sheetCode + ".xlsx");
                        templateAda = true;
                    }
                } catch (Exception exSrc) {
                    // file template tidak ada / gagal set src
                    templateAda = false;
                }

                if (!templateAda) {
                    // KE-2: template WEB-INF/sapto/<kode>.xlsx TIDAK ADA -> Spreadsheet zss tak memuat Book,
                    // sehingga setMaxcolumns()/getSelectedSheet() melempar UiException
                    // "resource for ../../WEB-INF/sapto/<kode>.xlsx not found". Degradasi anggun: lepas
                    // spreadsheet & tampilkan info; tab "Tabel Data" dan "Grafik & Analisis" tetap berfungsi.
                    try { excelku.detach(); } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:194"); }
                    Html infoKosong = new Html();
                    infoKosong.setContent("<div style='color:#9CA3AF;padding:24px;text-align:center'>"
                        + "Template Excel <b>" + sheetCode + ".xlsx</b> tidak tersedia di server "
                        + "(folder WEB-INF/sapto). Silakan gunakan tab <b>Tabel Data</b> atau "
                        + "<b>Grafik &amp; Analisis</b>.</div>");
                    infoKosong.setParent(excelCenter);
                } else {
                excelku.setStyle("border:1px solid #8AA3C1");
                excelku.setHeight("100%");
                excelku.setWidth("100%");
                excelku.setMaxcolumns(col);

                int rowCount = datas == null ? 70 : datas.size() + 1;
                excelku.setMaxrows(rowCount);

                Worksheet sheet = excelku.getSelectedSheet();

                if (onCellClick != null) excelku.addEventListener("onCellClick", onCellClick);
                if (label.getAttribute("onCellClick") != null)
                    excelku.addEventListener("onCellClick", (EventListener) label.getAttribute("onCellClick"));

                // Populate cells
                int cols = 0;
                if (datas != null) {
                    for (int rowIdx = 0; rowIdx < datas.size(); rowIdx++) {
                        List sub = datas.get(rowIdx);
                        for (int colIdx = 0; colIdx < sub.size(); colIdx++) {
                            try {
                                Object d = sub.get(colIdx);
                                if (d == null || d.toString().trim().isEmpty()) continue;
                                if (d instanceof Integer) {
                                    Utils.setCellValue(sheet, rowIdx, colIdx, (Integer) d);
                                } else if (d instanceof Double) {
                                    Utils.setCellValue(sheet, rowIdx, colIdx, (Double) d);
                                } else if (d instanceof Date) {
                                    Utils.setCellValue(sheet, rowIdx, colIdx,
                                        Common.dateFormat2.get().format(d));
                                } else {
                                    Utils.setCellValue(sheet, rowIdx, colIdx, d.toString().trim());
                                }
                                if (cols < colIdx) cols = colIdx;
                            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:236"); /* skip bad cells */ }
                        }
                    }
                }
                for (int i = 0; i < cols; i++) {
                    try { sheet.autoSizeColumn(i); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:241"); /* ignore */ }
                }

                // FIX beban widget zss Spreadsheet: SEMUA 3 tab (Tabel Data/Grafik/Tampilan
                // Excel) dibangun eager saat load, jadi widget berat ini tetap dikirim ke
                // browser walau bukan tab default. Ganti dgn Grid ringan berpaginasi via
                // PratinjauXlsxHelper (pola B sama spt 57 dashboard lain) -- Book zss tetap
                // hidup (widget disembunyikan bukan detach) shg tombol "Download File Excel"
                // di excelSouth (getBook().write) tetap menghasilkan file utuh.
                ais.ui.util.PratinjauXlsxHelper.gantiSpreadsheetDenganGrid(excelku);

                // PERBAIKAN "Tampilan Excel kadang kosong di tengah":
                // Spreadsheet zss dibangun saat tab ini masih TERSEMBUNYI (tab default =
                // "Tabel Data"), sehingga sebagian sel belum ter-render → tampak kosong.
                // Render ulang (invalidate) tiap kali tab "Tampilan Excel" dipilih agar
                // seluruh sel tampil utuh saat benar-benar terlihat.
                tabExcel.addEventListener("onSelect", new EventListener() {
                    @Override
                    public void onEvent(Event ev) throws Exception {
                        try { excelku.invalidate(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:252"); /* abaikan */ }
                    }
                });
                } // tutup else (templateAda) — blok bergantung Book zss
            }
        });
        timer.start();
    }

    // -----------------------------------------------------------------------
    // Tab builders
    // -----------------------------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void buildTableTab(Tabpanel panel, List<List> datas,
                                      SaptoGridConfig.Config cfg, boolean hasClickHint) {
        if (datas == null) {
            Html empty = new Html();
            empty.setContent("<p style='color:#9CA3AF;padding:24px;text-align:center'>Tidak ada data.</p>");
            empty.setParent(panel);
            return;
        }

        // Header & baris awal: untuk laporan DINAMIS diturunkan dari DATA (baris ke-1 =
        // nama kolom hasil query, isi mulai baris ke-2), bukan placeholder statik
        // "Kolom 1..5". Untuk laporan terdaftar tetap memakai konfigurasi.
        String[] headers = cfg.headers;
        int dataStart = cfg.dataStartRow;
        if (cfg.dynamic) {
            List hr = datas.size() > 1 ? datas.get(1) : null;
            if (hr != null && !hr.isEmpty()) {
                headers = new String[hr.size()];
                for (int i = 0; i < hr.size(); i++) {
                    headers[i] = hr.get(i) == null ? "" : hr.get(i).toString().trim();
                }
            }
            dataStart = 2;
        }

        Listbox lb = new Listbox();
        lb.setWidth("100%");
        lb.setSclass("ais-sapto-grid");
        lb.setMold("paging");   // tampilkan bertahap (ada navigasi halaman)
        lb.setPageSize(25);     // 25 baris per halaman
        lb.setParent(panel);

        Listhead head = new Listhead();
        head.setSizable(true);
        head.setParent(lb);
        for (int hi = 0; hi < headers.length; hi++) {
            Listheader lh = new Listheader(headers[hi]);
            if (hi == 0) {
                // kolom pertama biasanya teks panjang — beri ruang lebih
                lh.setStyle("white-space:normal;word-break:break-word;min-width:160px");
                lh.setWidth("200px");
            } else {
                lh.setStyle("white-space:normal;word-break:break-word;min-width:80px;text-align:center");
                lh.setWidth("100px");
            }
            lh.setParent(head);
        }

        // Akumulator footer: jumlah (SUM) tiap kolom angka, dihitung lintas SELURUH
        // halaman (bukan hanya halaman yang sedang tampil).
        double[] colSum = new double[headers.length];
        int[] colNumCount = new int[headers.length];
        int[] colNonEmpty = new int[headers.length];

        int dataRows = 0;
        for (int i = dataStart; i < datas.size(); i++) {
            List row = datas.get(i);
            if (row == null || row.isEmpty()) continue;
            boolean hasData = false;
            for (Object o : row) {
                if (o != null && !o.toString().trim().isEmpty()) { hasData = true; break; }
            }
            if (!hasData) continue;

            Listitem item = new Listitem();
            item.setParent(lb);
            for (int ci = 0; ci < headers.length; ci++) {
                String val = (ci < row.size() && row.get(ci) != null)
                    ? row.get(ci).toString().trim() : "";
                Listcell lc = new Listcell(val);
                Double angka = parseAngka(val);   // ID-aware ("6.270.000" → 6270000)
                if (angka != null) {
                    lc.setStyle("text-align:right;white-space:nowrap");
                } else {
                    lc.setStyle("white-space:normal;word-break:break-word");
                }
                lc.setParent(item);

                // akumulasi untuk footer SUM
                if (!val.isEmpty()) colNonEmpty[ci]++;
                if (angka != null) {
                    colNumCount[ci]++;
                    colSum[ci] += angka.doubleValue();
                }
            }
            dataRows++;
        }

        // ── Footer: total (SUM) tiap kolom yang berisi angka ────────────────
        // Kolom dianggap "angka yang dijumlahkan" bila mayoritas isinya numerik
        // DAN bukan kolom identitas (No./NIS/NIM/Kode/Tahun/dll — menjumlahkannya
        // tidak bermakna). Total dihitung dari seluruh data, bukan per halaman.
        if (dataRows > 0) {
            boolean adaKolomAngka = false;
            for (int ci = 1; ci < headers.length; ci++) {
                if (kolomBisaDijumlah(headers[ci], colNumCount[ci], colNonEmpty[ci])) {
                    adaKolomAngka = true;
                    break;
                }
            }
            if (adaKolomAngka) {
                String gaya = "font-weight:800;color:#13294b;background:#dbe6f5;"
                    + "border-top:2px solid #1f3a63";
                Listfoot foot = new Listfoot();
                foot.setParent(lb);
                for (int ci = 0; ci < headers.length; ci++) {
                    Listfooter lf;
                    if (ci == 0) {
                        lf = new Listfooter("TOTAL");
                        lf.setStyle(gaya + ";text-align:left;white-space:nowrap");
                    } else if (kolomBisaDijumlah(headers[ci], colNumCount[ci], colNonEmpty[ci])) {
                        lf = new Listfooter(fmtNum(colSum[ci]));
                        lf.setStyle(gaya + ";text-align:right;white-space:nowrap");
                    } else {
                        lf = new Listfooter("");
                        lf.setStyle(gaya);
                    }
                    lf.setParent(foot);
                }
            }
        }

        // ── Paging GANDA (atas + bawah), gaya "os" ──────────────────────────
        // Pager bawah = milik Listbox sendiri (mold "paging") → ubah gayanya jadi "os".
        // Pager atas = Paging eksternal gaya "os" yang DISINKRONKAN dengan listbox,
        // sehingga klik di atas atau di bawah sama-sama berpindah halaman.
        if (dataRows > 0) {
            try {
                Paging bawah = lb.getPagingChild();   // pager internal listbox
                if (bawah != null) {
                    bawah.setMold("os");
                    bawah.setDetailed(true);
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sapto/util/SaptoUtil.java:399"); /* abaikan bila belum tersedia */ }

            final Listbox flb = lb;
            final Paging atas = new Paging();
            atas.setMold("os");
            atas.setDetailed(true);
            atas.setPageSize(25);
            atas.setTotalSize(dataRows);
            atas.setStyle("margin:0 0 6px 0");
            panel.insertBefore(atas, lb);   // letakkan DI ATAS tabel

            // klik pager ATAS → pindahkan halaman listbox (pager bawah ikut otomatis)
            atas.addEventListener("onPaging", new EventListener() {
                @Override
                public void onEvent(Event ev) throws Exception {
                    int pg = ((PagingEvent) ev).getActivePage();
                    if (flb.getActivePage() != pg) flb.setActivePage(pg);
                }
            });
            // perubahan halaman listbox (klik pager BAWAH) → samakan tampilan pager atas
            flb.addEventListener("onPaging", new EventListener() {
                @Override
                public void onEvent(Event ev) throws Exception {
                    int pg = flb.getActivePage();
                    if (atas.getActivePage() != pg) atas.setActivePage(pg);
                }
            });
        }

        if (dataRows == 0) {
            Html noData = new Html();
            noData.setContent("<p style='color:#9CA3AF;padding:24px;text-align:center'>Belum ada data yang dapat ditampilkan.</p>");
            noData.setParent(panel);
            lb.detach();
        } else if (hasClickHint) {
            Html hint = new Html();
            hint.setContent("<p style='font-size:11px;color:#6B7280;padding:6px 8px;background:#F9FAFB;" +
                "border-top:1px solid #E5E7EB;margin:0'>" +
                "Klik angka pada tab Excel untuk melihat data secara rinci.</p>");
            hint.setParent(panel);
        }
    }

    private static void buildChartTab(Tabpanel panel, List<List> datas,
                                      SaptoGridConfig.Config cfg, String sheetCode) {
        if (cfg.dynamic) {
            buildDynamicChartTab(panel, datas);
            return;
        }
        StringBuilder html = new StringBuilder();
        html.append("<div style='padding:8px'>");

        // Main chart
        if (!"none".equals(cfg.chartType)) {
            String chartHtml = SaptoChartBuilder.build(cfg.chartType, datas, cfg.dataStartRow,
                cfg.headers, null);
            html.append(chartHtml);
        }

        // Recommendation panel
        html.append(buildRecommendation(sheetCode));
        html.append("</div>");

        Html chartComp = new Html();
        chartComp.setContent(html.toString());
        chartComp.setParent(panel);
    }

    private static void buildDescriptionPanel(North north, String description) {
        Html desc = new Html();
        desc.setContent("<div style='background:#EFF6FF;border-bottom:1px solid #BFDBFE;" +
            "padding:8px 16px;font-size:12px;color:#1E40AF;line-height:1.5'>" +
            "<b style='color:#1D4ED8'>ℹ </b>" + htmlEsc(description) + "</div>");
        desc.setParent(north);
    }

    // -----------------------------------------------------------------------
    // Per-sheet recommendations
    // -----------------------------------------------------------------------

    private static String buildRecommendation(String sheetCode) {
        String heading;
        String[] tips;

        if (sheetCode.startsWith("A-3.1") || sheetCode.startsWith("A-3.2")) {
            heading = "Analisis Rekomendasi Mahasiswa & Lulusan";
            tips = new String[]{
                "Bandingkan rasio penerimaan vs. pendaftar untuk melihat daya saing program studi.",
                "Perhatikan tren IPK rata-rata dan masa studi — idealnya IPK naik dan masa studi mendekati normal.",
                "Lonjakan jumlah mahasiswa aktif perlu diimbangi dengan penambahan dosen dan prasarana.",
                "Lakukan tracer study secara rutin untuk memantau penyerapan lulusan di dunia kerja."
            };
        } else if (sheetCode.startsWith("A-4")) {
            heading = "Analisis Rekomendasi Sumber Daya Dosen";
            tips = new String[]{
                "Tingkatkan proporsi dosen S3 untuk memenuhi standar BAN-PT.",
                "Pastikan beban SKS setiap dosen tidak melebihi 12 SKS per semester.",
                "Dorong dosen aktif dalam publikasi dan penelitian untuk meningkatkan kualifikasi jabatan fungsional.",
                "Dosen dengan jabatan Tenaga Pengajar harus segera memproses sertifikasi Asisten Ahli."
            };
        } else if (sheetCode.startsWith("A-5")) {
            heading = "Analisis Rekomendasi Kurikulum & Pembelajaran";
            tips = new String[]{
                "Evaluasi kurikulum secara berkala (minimal 4 tahun sekali) untuk menyesuaikan dengan kebutuhan industri.",
                "Rasio SKS wajib vs. pilihan yang ideal adalah 70:30.",
                "Pastikan setiap dosen PA melakukan minimal 4 kali pertemuan bimbingan per semester.",
                "Monitor lama penyelesaian tugas akhir — target ≤ 6 bulan setelah seminar proposal."
            };
        } else if (sheetCode.startsWith("A-6")) {
            heading = "Analisis Rekomendasi Keuangan & Sarana";
            tips = new String[]{
                "Anggaran penelitian idealnya minimal 5% dari total anggaran institusi.",
                "Periksa kondisi prasarana secara berkala dan prioritaskan perawatan yang 'Tidak Terawat'.",
                "Diversifikasi sumber dana untuk mengurangi ketergantungan pada satu sumber.",
                "Koleksi perpustakaan perlu diperbarui minimal 10% per tahun."
            };
        } else if (sheetCode.startsWith("A-7")) {
            heading = "Analisis Rekomendasi Penelitian & Kerjasama";
            tips = new String[]{
                "Target publikasi internasional terindeks minimal 1 artikel per dosen per 2 tahun.",
                "Perbanyak kerjasama dengan industri untuk sumber dana penelitian eksternal.",
                "Dorong dosen mengurus HaKI atas karya penelitiannya.",
                "Kerjasama luar negeri membuka peluang pertukaran mahasiswa dan dosen."
            };
        } else if (sheetCode.startsWith("DKPS-2")) {
            heading = "Analisis Kerjasama Program Studi";
            tips = new String[]{
                "Target minimal: 1 MoU aktif lokal, 3 nasional, dan 1 internasional per tahun.",
                "Pastikan setiap kerjasama memiliki output nyata (PKL, guest lecture, penelitian bersama).",
                "Perpanjang MoU yang akan habis masa berlakunya minimal 3 bulan sebelum jatuh tempo.",
                "Kerjasama internasional membuka peluang akreditasi internasional (ASIIN, AUN-QA)."
            };
        } else if (sheetCode.startsWith("DKPS-3")) {
            heading = "Analisis Kemahasiswaan Program Studi";
            tips = new String[]{
                "Tingkatkan daya tarik PS melalui promosi aktif: open house, media sosial, dan testimoni alumni.",
                "Lakukan tracer study minimal setahun sekali untuk memantau kualitas lulusan secara berkelanjutan.",
                "Berikan layanan bimbingan konseling dan pengembangan karir yang terstruktur.",
                "Prestasi mahasiswa di tingkat nasional/internasional meningkatkan reputasi dan akreditasi PS."
            };
        } else if (sheetCode.startsWith("DKPS-4")) {
            heading = "Analisis Sumber Daya Manusia DTPS";
            tips = new String[]{
                "Proporsi DTPS berpendidikan S3 idealnya ≥50% untuk memenuhi standar BAN-PT.",
                "Pastikan beban SKS DTPS tidak melebihi 12 SKS/semester termasuk penelitian dan pengabdian.",
                "Rekognisi dosen (penghargaan, narasumber, reviewer) meningkatkan skor SDM akreditasi.",
                "Buat program pengembangan dosen yang terencana: studi lanjut, pelatihan, sertifikasi kompetensi."
            };
        } else if (sheetCode.startsWith("DKPS-5")) {
            heading = "Analisis Keuangan & Sarana Prasarana";
            tips = new String[]{
                "Anggaran riset idealnya minimal 10% dari total penggunaan dana PS.",
                "Lakukan inventarisasi dan peremajaan sarana secara berkala — perbarui minimal 5% per tahun.",
                "Fasilitas aksesibilitas difabel wajib ada sebagai syarat akreditasi BAN-PT terbaru.",
                "Koleksi perpustakaan harus diperbarui; pastikan akses jurnal internasional (Scopus, WoS) tersedia."
            };
        } else if (sheetCode.startsWith("DKPS-6")) {
            heading = "Analisis Pendidikan & Lulusan";
            tips = new String[]{
                "IPK rata-rata ≥3.20 dan masa studi ≤4,5 tahun (S1) menjadi acuan standar akreditasi Unggul.",
                "Lakukan review kurikulum berbasis OBE (Outcome-Based Education) setiap 4 tahun.",
                "Waktu tunggu kerja lulusan <3 bulan dan kesesuaian bidang >70% menjadi indikator keunggulan.",
                "Kepuasan pengguna lulusan harus disurvei secara reguler dan hasilnya digunakan untuk perbaikan PS."
            };
        } else if (sheetCode.startsWith("DKPS-7")) {
            heading = "Analisis Penelitian DTPS";
            tips = new String[]{
                "Target: setiap DTPS memiliki minimal 1 penelitian aktif per tahun dan 1 publikasi Sinta per 2 tahun.",
                "Publikasi di jurnal Scopus/WoS Q1-Q2 memberikan poin akreditasi tertinggi.",
                "Dorong kolaborasi penelitian lintas PS dan lintas institusi untuk meningkatkan kualitas output.",
                "Perolehan HaKI dan paten menunjukkan penelitian yang menghasilkan produk bernilai guna."
            };
        } else if (sheetCode.startsWith("DKPS-8")) {
            heading = "Analisis Pengabdian kepada Masyarakat (PkM)";
            tips = new String[]{
                "Target: setiap DTPS melaksanakan minimal 1 PkM per tahun dengan output terukur.",
                "PkM yang melibatkan mahasiswa dan menghasilkan luaran (HaKI, model pemberdayaan) mendapat nilai lebih.",
                "Manfaatkan dana hibah Kemdikbud (PKM, PKMDS) untuk mendanai PkM berkualitas.",
                "Dokumentasikan luaran PkM secara lengkap — foto, laporan, publikasi pengabdian."
            };
        } else {
            return "";
        }

        return SaptoChartBuilder.buildRecommendation(heading, tips);
    }

    /**
     * Grafik & Analisis OTOMATIS untuk laporan dinamis ("data_umum"): deteksi kolom
     * angka & kolom kategori dari data, tampilkan kartu ringkasan + grafik batang
     * total(angka) per kategori. Mengganti tab Grafik yang sebelumnya KOSONG.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void buildDynamicChartTab(Tabpanel panel, List<List> datas) {
        StringBuilder html = new StringBuilder("<div style='padding:8px'>");
        Html comp = new Html();

        if (datas == null || datas.size() < 3) {
            html.append("<p style='color:#9CA3AF;text-align:center;padding:32px 0;font-size:13px'>")
                .append("Belum ada data untuk dianalisis. Klik <b>Tampilkan</b> dahulu.</p></div>");
            comp.setContent(html.toString());
            comp.setParent(panel);
            return;
        }

        List header = datas.get(1);
        int nCols = header.size();
        java.util.List<List> body = new java.util.ArrayList<List>();
        for (int i = 2; i < datas.size(); i++) {
            List r = datas.get(i);
            if (r == null || r.isEmpty()) continue;
            boolean ada = false;
            for (Object o : r) { if (o != null && !o.toString().trim().isEmpty()) { ada = true; break; } }
            if (ada) body.add(r);
        }
        int totalBaris = body.size();

        // Deteksi kolom (lewati kolom 0 = nomor urut): kolom ANGKA (≥60% nilai numerik,
        // pilih yang totalnya terbesar) & kolom KATEGORI (teks dgn nilai unik paling sedikit).
        int valueCol = -1; double valueColSum = -1;
        int groupCol = -1; int groupColDistinct = Integer.MAX_VALUE;
        for (int c = 1; c < nCols; c++) {
            int numericCount = 0; double sum = 0;
            java.util.Set<String> distinct = new java.util.HashSet<String>();
            for (int i = 0; i < body.size(); i++) {
                List r = body.get(i);
                String v = (c < r.size() && r.get(c) != null) ? r.get(c).toString().trim() : "";
                if (v.isEmpty()) continue;
                Double num = parseAngka(v);
                if (num != null) { numericCount++; sum += num.doubleValue(); }
                distinct.add(v);
            }
            boolean numeric = totalBaris > 0 && numericCount >= (int) Math.ceil(totalBaris * 0.6);
            if (numeric) {
                if (sum > valueColSum) { valueColSum = sum; valueCol = c; }
            } else {
                int d = distinct.size();
                if (d > 1 && d < groupColDistinct) { groupColDistinct = d; groupCol = c; }
            }
        }

        String valName = (valueCol >= 0 && valueCol < header.size() && header.get(valueCol) != null)
            ? header.get(valueCol).toString().trim() : "Nilai";

        // Kartu ringkasan
        java.util.List<String[]> cards = new java.util.ArrayList<String[]>();
        cards.add(new String[]{ "Jumlah Baris", fmtNum(totalBaris), "Total entri data" });
        if (valueCol >= 0) {
            cards.add(new String[]{ "Total " + valName, fmtNum(valueColSum), "Penjumlahan kolom " + valName });
        }
        if (groupCol >= 0) {
            String gName = header.get(groupCol) == null ? "Kategori" : header.get(groupCol).toString().trim();
            cards.add(new String[]{ "Kategori (" + gName + ")", fmtNum(groupColDistinct), "Banyak nilai unik" });
        }
        html.append(SaptoChartBuilder.buildSummaryCards(cards.toArray(new String[0][])));

        // Grafik batang: total(angka) per kategori (15 teratas)
        if (valueCol >= 0 && groupCol >= 0) {
            java.util.LinkedHashMap<String, Double> agg = new java.util.LinkedHashMap<String, Double>();
            for (int i = 0; i < body.size(); i++) {
                List r = body.get(i);
                String g = (groupCol < r.size() && r.get(groupCol) != null) ? r.get(groupCol).toString().trim() : "";
                if (g.isEmpty()) g = "(kosong)";
                Double v = (valueCol < r.size() && r.get(valueCol) != null) ? parseAngka(r.get(valueCol).toString().trim()) : null;
                double val = v == null ? 0 : v.doubleValue();
                Double cur = agg.get(g);
                agg.put(g, (cur == null ? 0 : cur.doubleValue()) + val);
            }
            java.util.List<java.util.Map.Entry<String, Double>> entries =
                new java.util.ArrayList<java.util.Map.Entry<String, Double>>(agg.entrySet());
            java.util.Collections.sort(entries, new java.util.Comparator<java.util.Map.Entry<String, Double>>() {
                public int compare(java.util.Map.Entry<String, Double> a, java.util.Map.Entry<String, Double> b) {
                    return Double.compare(b.getValue().doubleValue(), a.getValue().doubleValue());
                }
            });
            java.util.List<List> chartRows = new java.util.ArrayList<List>();
            int limit = Math.min(entries.size(), 15);
            for (int i = 0; i < limit; i++) {
                java.util.ArrayList row = new java.util.ArrayList();
                row.add(entries.get(i).getKey());
                row.add(entries.get(i).getValue());
                chartRows.add(row);
            }
            String gName = (groupCol < header.size() && header.get(groupCol) != null)
                ? header.get(groupCol).toString().trim() : "Kategori";
            String title = "Total " + valName + " per " + gName + (entries.size() > limit ? " (15 teratas)" : "");
            html.append(SaptoChartBuilder.build("bar", chartRows, 0, new String[]{ gName, valName }, title));
        } else {
            html.append("<div style='background:#FFFBEB;border-left:4px solid #F59E0B;border-radius:0 8px 8px 0;")
                .append("padding:12px 16px;margin:8px 4px;font-size:12px;color:#92400E'>")
                .append("Grafik otomatis memerlukan minimal satu kolom <b>angka</b> dan satu kolom <b>kategori</b>. ")
                .append("Data ini berupa rincian — silakan lihat tab <b>Tabel Data</b> atau <b>Tampilan Excel</b>.")
                .append("</div>");
        }

        html.append("</div>");
        comp.setContent(html.toString());
        comp.setParent(panel);
    }

    /** Parse angka format Indonesia ("6.270.000" → 6270000; "3,5" → 3.5). Null bila bukan angka. */
    private static Double parseAngka(String s) {
        if (s == null) return null;
        String t = s.trim().replace("Rp", "").replace("rp", "").replace(" ", "").trim();
        if (t.isEmpty()) return null;
        t = t.replace(".", "").replace(",", ".");
        if (t.equals("-") || t.equals(".")) return null;
        try { return Double.valueOf(Double.parseDouble(t)); } catch (Exception e) { return null; }
    }

    private static String fmtNum(double v) {
        try { return Common.numberFormat.get().format(v); } catch (Exception e) { return String.valueOf((long) v); }
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        try { Double.parseDouble(s.replace(",", "")); return true; } catch (Exception e) { return false; }
    }

    /**
     * Kolom layak dijumlahkan di footer bila MAYORITAS isinya numerik DAN header-nya
     * bukan kolom identitas — menjumlahkan No./NIS/NIM/Kode/Tahun dsb. tidak bermakna.
     */
    private static boolean kolomBisaDijumlah(String header, int numCount, int nonEmpty) {
        if (numCount <= 0) return false;
        if (numCount * 2 < nonEmpty) return false;   // mayoritas isi harus numerik
        return !isKolomIdentitas(header);
    }

    /** True bila header kolom adalah nomor identitas (bukan nilai yang patut dijumlah). */
    private static boolean isKolomIdentitas(String header) {
        if (header == null) return false;
        String[] tokens = header.toLowerCase().split("[^a-z0-9]+");
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i];
            if (t.length() == 0) continue;
            if (t.equals("no") || t.equals("nis") || t.equals("nim") || t.equals("nisn")
                || t.equals("nip") || t.equals("npm") || t.equals("npwp") || t.equals("nik")
                || t.equals("ktp") || t.equals("kode") || t.equals("id") || t.equals("telp")
                || t.equals("telepon") || t.equals("tlp") || t.equals("hp") || t.equals("wa")
                || t.equals("rekening") || t.equals("rek") || t.equals("va")
                || t.equals("tahun") || t.equals("thn") || t.equals("angkatan")
                || t.equals("semester") || t.equals("smt") || t.equals("urut")) {
                return true;
            }
        }
        return false;
    }

    private static String htmlEsc(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
