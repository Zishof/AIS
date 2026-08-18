package ais.action.master.payroll.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.zk.ui.Component;

import ais.common.Common;
import ais.database.model.Statusabsensi;
import ais.database.model.StatuskehadiranKaryawanHarian;
import ais.ui.util.MyGrid;

/**
 * Class khusus untuk dasbor tambahan HRD/Pimpinan.
 *
 * Syarat kecil di DashboardKehadiranExpert:
 * - Ubah inner class berikut dari private static menjadi static / public static:
 *   DashboardData, RingkasanPegawaiHolder, RingkasanSatkerHolder.
 * - Field di holder boleh tetap default/package-private selama package class ini sama.
 *
 * Cara panggil dari DashboardKehadiranExpert.renderDashboards():
 *
 * new DashboardKehadiranTambahan(portal.pcBottom, data).renderAll();
 */
public class DashboardKehadiranTambahan {

    private final Component pcBottom;
    private final DashboardKehadiranExpert.DashboardData data;

    public DashboardKehadiranTambahan(Component pcBottom,
            DashboardKehadiranExpert.DashboardData data) {
        this.pcBottom = pcBottom;
        this.data = data;
    }

    /**
     * Urutan 10 dasbor tambahan.
     * Silakan pindahkan urutan pemanggilan method jika ingin mengubah susunan tampilan.
     */
    public void renderAll() throws Exception {
        progress("Merender Skor Kedisiplinan Pegawai...", 97);
        renderSkorKedisiplinanPegawai();
        progress("Merender Rasio Kehadiran Per Pegawai...", 97);
        renderRasioKehadiranPerPegawai();
        progress("Merender Konsistensi Kehadiran Pegawai...", 97);
        renderKonsistensiKehadiranPegawai();
        progress("Merender Ranking Ketidakhadiran Pegawai...", 98);
        renderRankingKetidakhadiranPegawai();
        progress("Merender Efektivitas Jam Kerja Pegawai...", 98);
        renderEfektivitasJamKerjaPegawai();
        progress("Merender Pegawai Sering Masuk Hari Libur...", 98);
        renderPegawaiSeringMasukHariLibur();
        progress("Merender Validasi Anomali Presensi...", 99);
        renderValidasiAnomaliPresensi();
        progress("Merender Rekap Pengajuan Pegawai...", 99);
        renderRekapPengajuanPegawai();
        progress("Merender Heatmap Keterlambatan dan Beban Kerja...", 99);
        renderHeatmapKeterlambatanHari();
        progress("Merender Pegawai Risiko Disiplin Menurun...", 99);
        renderPegawaiRisikoDisiplinMenurun();
        progress("Merender Ringkasan Tindak Lanjut HRD...", 99);
        renderRingkasanTindakLanjutHrd();
    }

    private void progress(String pesan, int persen) {
        try {
            if (data != null && data.progressHandler != null) {
                data.progressHandler.update(pesan, persen);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:73");
        }
    }

    /**
     * 1. Skor kedisiplinan formal per pegawai.
     */
    public void renderSkorKedisiplinanPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Skor Kedisiplinan Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Double.compare(hitungSkorDisiplin(b), hitungSkorDisiplin(a));
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Skor", "Grade", "Alpha", "Terlambat",
                "Pulang Cepat", "Tidak Absen Pulang", "Rekomendasi");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            double skor = hitungSkorDisiplin(h);
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            appendBadge(r, Common.numberFormat.get().format(skor), warnaSkor(skor));
            r.appendChild(new org.zkoss.zul.Label(gradeSkor(skor)));
            appendStyledNumber(r, h.alpa, "#dc3545");
            appendStyledNumber(r, h.terlambat, "#fd7e14");
            appendStyledNumber(r, h.pulangcepat, "#0d6efd");
            appendStyledNumber(r, h.tidakAbsenPulang, "#dc3545");
            r.appendChild(new org.zkoss.zul.Label(rekomendasiSkor(skor, h)));
        }
    }

    /**
     * 2. Rasio kehadiran per pegawai dalam persentase.
     */
    public void renderRasioKehadiranPerPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Rasio Kehadiran Per Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Double.compare(hitungRasioHadir(b), hitungRasioHadir(a));
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Hari Aktif", "Hadir", "Tidak Hadir",
                "Alpha", "Rasio Hadir", "Rasio Alpha", "Status");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            double rasioHadir = hitungRasioHadir(h);
            double rasioAlpha = h.aktif == 0 ? 0.0 : (h.alpa * 100.0 / h.aktif);
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.aktif)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.masuk)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tidakHadirTanpaHoliday)));
            appendStyledNumber(r, h.alpa, "#dc3545");
            appendBadge(r, Common.numberFormat.get().format(rasioHadir) + " %", warnaPersen(rasioHadir));
            appendStyledText(r, Common.numberFormat.get().format(rasioAlpha) + " %", rasioAlpha > 0.0, "#dc3545");
            r.appendChild(new org.zkoss.zul.Label(statusRasioHadir(rasioHadir)));
        }
    }

    /**
     * 3. Konsistensi tepat waktu.
     */
    public void renderKonsistensiKehadiranPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Konsistensi Kehadiran Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Double.compare(hitungKonsistensi(b), hitungKonsistensi(a));
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Hari Aktif", "Tepat Waktu", "Tepat Waktu Banget",
                "Terlambat", "Alpha", "Konsistensi", "Kategori");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            double konsistensi = hitungKonsistensi(h);
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.aktif)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tepatWaktu)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.tepatWaktuBanget)));
            appendStyledNumber(r, h.terlambat, "#fd7e14");
            appendStyledNumber(r, h.alpa, "#dc3545");
            appendBadge(r, Common.numberFormat.get().format(konsistensi) + " %", warnaPersen(konsistensi));
            r.appendChild(new org.zkoss.zul.Label(kategoriKonsistensi(konsistensi)));
        }
    }

    /**
     * 4. Ranking ketidakhadiran.
     */
    public void renderRankingKetidakhadiranPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Ranking Ketidakhadiran Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                long totalA = totalTidakHadir(a);
                long totalB = totalTidakHadir(b);
                return Long.compare(totalB, totalA);
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Peringkat", "Nama Pegawai", "Satuan Kerja", "Alpha", "Sakit", "Izin",
                "Cuti Memotong", "Cuti Tidak Memotong", "Total Tidak Hadir", "Status");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        int no = 1;
        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            long total = totalTidakHadir(h);
            if (total <= 0) continue;
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label("#" + no));
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            appendStyledNumber(r, h.alpa, "#dc3545");
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.sakit)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.izin)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_memotong)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_tidak_memotong)));
            appendBadge(r, total + " Hari", total >= 5 ? "#dc3545" : "#fd7e14");
            r.appendChild(new org.zkoss.zul.Label(statusKetidakhadiran(h, total)));
            no++;
        }
    }

    /**
     * 5. Efektivitas jam kerja pegawai.
     */
    public void renderEfektivitasJamKerjaPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Efektivitas Jam Kerja Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Double.compare(hitungJamEfektif(b), hitungJamEfektif(a));
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Σ Jam Kerja", "Σ Jam Telat",
                "Σ Jam Pulang Cepat", "Σ Jam Lembur", "Jam Efektif", "Status");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            double jamEfektif = hitungJamEfektif(h);
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            r.appendChild(new org.zkoss.zul.Label(formatJam(h.jamMasuk)));
            appendStyledText(r, formatJam(h.terlambatJam), h.terlambatJam > 0.0, "#fd7e14");
            appendStyledText(r, formatJam(h.cepatKeluar), h.cepatKeluar > 0.0, "#fd7e14");
            appendStyledText(r, formatJam(h.lemburMasuk), h.lemburMasuk > 0.0, "#6f42c1");
            appendBadge(r, formatJam(jamEfektif), jamEfektif < 0.0 ? "#dc3545" : "#198754");
            r.appendChild(new org.zkoss.zul.Label(statusEfektivitas(h, jamEfektif)));
        }
    }

    /**
     * 6. Pegawai yang sering masuk hari libur.
     */
    public void renderPegawaiSeringMasukHariLibur() {
        org.zkoss.zul.Panelchildren pch = createPanel("Pegawai Sering Masuk Hari Libur", pcBottom, null);

        Map<String, MasukLiburHolder> map = buildMasukLiburMap();
        List<MasukLiburHolder> list = new java.util.ArrayList<MasukLiburHolder>(map.values());
        java.util.Collections.sort(list, new java.util.Comparator<MasukLiburHolder>() {
            public int compare(MasukLiburHolder a, MasukLiburHolder b) {
                int c = Long.compare(b.jumlahHari, a.jumlahHari);
                if (c != 0) return c;
                return Double.compare(b.totalJamLembur, a.totalJamLembur);
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Jumlah Hari Libur Masuk", "Σ Jam Kerja",
                "Σ Jam Lembur Valid", "Status");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (MasukLiburHolder h : list) {
            if (h.jumlahHari <= 0) continue;
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            appendBadge(r, h.jumlahHari + " Hari", h.jumlahHari >= 3 ? "#dc3545" : "#0d6efd");
            r.appendChild(new org.zkoss.zul.Label(formatJam(h.totalJamKerja)));
            r.appendChild(new org.zkoss.zul.Label(formatJam(h.totalJamLembur)));
            r.appendChild(new org.zkoss.zul.Label(h.jumlahHari >= 3 ? "Perlu Evaluasi Beban Kerja" : "Monitor"));
        }
    }

    /**
     * 7. Validasi anomali presensi harian.
     */
    public void renderValidasiAnomaliPresensi() {
        org.zkoss.zul.Panelchildren pch = createPanel("Validasi Anomali Presensi", pcBottom, null);

        MyGrid grid = createGrid(pch, 15, "100%");
        addColumns(grid, "Tanggal", "Nama Pegawai", "Satuan Kerja", "Jenis Anomali", "Keterangan", "Prioritas");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (StatuskehadiranKaryawanHarian skh : data.listKehadiranFiltered) {
            List<AnomaliInfo> anomalis = cekAnomali(skh);
            for (AnomaliInfo anomali : anomalis) {
                org.zkoss.zul.Row r = new org.zkoss.zul.Row();
                r.setParent(rows);
                r.appendChild(new org.zkoss.zul.Label(formatTanggal(skh.getTanggal())));
                r.appendChild(new org.zkoss.zul.Label(getNamaPegawai(skh)));
                r.appendChild(new org.zkoss.zul.Label(getNamaSatker(skh)));
                r.appendChild(new org.zkoss.zul.Label(anomali.jenis));
                r.appendChild(new org.zkoss.zul.Label(anomali.keterangan));
                appendBadge(r, anomali.prioritas, warnaPrioritas(anomali.prioritas));
            }
        }
    }

    /**
     * 8. Rekap pengajuan pegawai.
     */
    public void renderRekapPengajuanPegawai() {
        org.zkoss.zul.Panelchildren pch = createPanel("Rekap Pengajuan Pegawai", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Integer.compare(b.jumlahPengajuan, a.jumlahPengajuan);
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Nama Pegawai", "Satuan Kerja", "Σ Pengajuan", "Sakit", "Izin", "Cuti Memotong",
                "Cuti Tidak Memotong", "Cuti Bisa Diambil", "Status");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            if (h.jumlahPengajuan <= 0 && h.sakit <= 0 && h.izin <= 0 && h.cuti_memotong <= 0 && h.cuti_tidak_memotong <= 0) continue;
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            appendStyledNumber(r, h.jumlahPengajuan, "#0dcaf0");
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.sakit)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.izin)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_memotong)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.cuti_tidak_memotong)));
            r.appendChild(new org.zkoss.zul.Label(h.jumlahCutiYangBisaDiambil + " Hari"));
            r.appendChild(new org.zkoss.zul.Label(statusPengajuan(h)));
        }
    }

    /**
     * 9. Heatmap sederhana per hari: terlambat, alpha, pulang cepat, lembur.
     */
    public void renderHeatmapKeterlambatanHari() {
        org.zkoss.zul.Panelchildren pch = createPanel("Heatmap Keterlambatan dan Beban Kerja per Hari", pcBottom, null);

        Map<Integer, HariStatHolder> map = buildHariStatMap();
        MyGrid grid = createGrid(pch, 7, "100%");
        addColumns(grid, "Hari", "Total Log", "Terlambat", "Alpha", "Pulang Cepat", "Tidak Absen Pulang",
                "Σ Jam Lembur", "Catatan");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        for (int hari = Calendar.SUNDAY; hari <= Calendar.SATURDAY; hari++) {
            HariStatHolder h = map.get(Integer.valueOf(hari));
            if (h == null) h = new HariStatHolder(hari);
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            r.appendChild(new org.zkoss.zul.Label(namaHari(hari)));
            r.appendChild(new org.zkoss.zul.Label(String.valueOf(h.totalLog)));
            appendHeatCell(r, h.totalTerlambat, maxTerlambat(map));
            appendHeatCell(r, h.totalAlpha, maxAlpha(map));
            appendHeatCell(r, h.totalPulangCepat, maxPulangCepat(map));
            appendHeatCell(r, h.totalTidakAbsenPulang, maxTidakAbsenPulang(map));
            r.appendChild(new org.zkoss.zul.Label(formatJam(h.totalJamLembur)));
            r.appendChild(new org.zkoss.zul.Label(catatanHari(h)));
        }
    }

    /**
     * 10. Pegawai berisiko disiplin menurun.
     * Catatan: tanpa data periode sebelumnya, indikator ini memakai skor risiko dari periode berjalan.
     */
    public void renderPegawaiRisikoDisiplinMenurun() {
        org.zkoss.zul.Panelchildren pch = createPanel("Pegawai Risiko Disiplin Menurun", pcBottom, null);

        List<DashboardKehadiranExpert.RingkasanPegawaiHolder> list =
                new java.util.ArrayList<DashboardKehadiranExpert.RingkasanPegawaiHolder>(data.mapRingkasan.values());
        java.util.Collections.sort(list, new java.util.Comparator<DashboardKehadiranExpert.RingkasanPegawaiHolder>() {
            public int compare(DashboardKehadiranExpert.RingkasanPegawaiHolder a,
                    DashboardKehadiranExpert.RingkasanPegawaiHolder b) {
                return Double.compare(hitungRiskScore(b), hitungRiskScore(a));
            }
        });

        MyGrid grid = createGrid(pch, 10, "100%");
        addColumns(grid, "Prioritas", "Nama Pegawai", "Satuan Kerja", "Risk Score", "Alpha", "Terlambat",
                "Pulang Cepat", "Tidak Absen Pulang", "Saran HRD");

        org.zkoss.zul.Rows rows = new org.zkoss.zul.Rows();
        rows.setParent(grid);

        int no = 1;
        for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : list) {
            double risk = hitungRiskScore(h);
            if (risk <= 0.0) continue;
            org.zkoss.zul.Row r = new org.zkoss.zul.Row();
            r.setParent(rows);
            appendBadge(r, "Prioritas #" + no, risk >= 10.0 ? "#dc3545" : "#fd7e14");
            r.appendChild(new org.zkoss.zul.Label(h.namaPegawai));
            r.appendChild(new org.zkoss.zul.Label(h.namaSatker));
            r.appendChild(new org.zkoss.zul.Label(Common.numberFormat.get().format(risk)));
            appendStyledNumber(r, h.alpa, "#dc3545");
            appendStyledNumber(r, h.terlambat, "#fd7e14");
            appendStyledNumber(r, h.pulangcepat, "#0d6efd");
            appendStyledNumber(r, h.tidakAbsenPulang, "#dc3545");
            r.appendChild(new org.zkoss.zul.Label(saranRisiko(h, risk)));
            no++;
        }
    }

    // ---------------------------------------------------------------------
    // Helper UI
    // ---------------------------------------------------------------------

    private org.zkoss.zul.Panelchildren createPanel(String title, Component parent, String style) {
        org.zkoss.zul.Panel pnl = new ais.ui.util.MyPanelConfig();
        pnl.setTitle(title);
        pnl.setBorder("none");
        pnl.setCollapsible(false);
        pnl.setClosable(false);
        pnl.setMaximizable(false);
        pnl.setMinimizable(false);
        pnl.setStyle("margin-bottom:14px; border:1px solid #e5e7eb; border-radius:18px; overflow:hidden;"
                + "background:#ffffff; box-shadow:0 14px 28px rgba(15,23,42,.07);");
        pnl.setParent(parent);

        org.zkoss.zul.Panelchildren pch = new org.zkoss.zul.Panelchildren();
        pch.setStyle(style == null ? "padding:14px; background:#fff;" : style);
        pch.setParent(pnl);
        appendDashboardDescription(pch, title);
        return pch;
    }

    private void appendDashboardDescription(Component parent, String title) {
        String desc = getDashboardDescription(title);
        if (desc == null || desc.trim().length() == 0) {
            return;
        }
        org.zkoss.zul.Html html = new org.zkoss.zul.Html("<div style=\"margin:0 0 12px 0; padding:10px 12px; "
                + "border-radius:14px; background:#f8fafc; border:1px solid #e2e8f0; color:#475569; "
                + "font-size:11.5px; line-height:1.6;\"><b style=\"color:#0f172a;\"></b> "
                + escapeHtml(desc) + "</div>");
        html.setParent(parent);
    }

    private String getDashboardDescription(String title) {
        if (title == null) {
            return "Menyajikan analisis lanjutan kehadiran agar HRD dan pimpinan dapat membaca pola disiplin, risiko, beban kerja, dan kebutuhan tindak lanjut secara lebih terarah.";
        }
        String t = title.toLowerCase(java.util.Locale.ENGLISH);
        if (t.indexOf("skor") >= 0) return "Menggabungkan alpha, keterlambatan, pulang cepat, dan tidak absen pulang menjadi skor kedisiplinan yang mudah dibandingkan. Skor ini membantu menentukan pegawai yang layak diapresiasi dan pegawai yang perlu dibina.";
        if (t.indexOf("rasio") >= 0) return "Memperlihatkan perbandingan hari hadir terhadap hari aktif sehingga tingkat kehadiran setiap pegawai dapat dibaca dengan cepat. Rasio rendah menjadi tanda awal perlunya klarifikasi atau perhatian dari atasan.";
        if (t.indexOf("konsistensi") >= 0) return "Menilai keteraturan hadir tepat waktu, bukan hanya jumlah hari hadir. Tampilan ini membantu melihat pegawai yang disiplin secara stabil dan pegawai yang kehadirannya masih berubah-ubah.";
        if (t.indexOf("ranking ketidakhadiran") >= 0) return "Mengurutkan pegawai dengan total ketidakhadiran tertinggi, termasuk alpha, sakit, izin, dan cuti. Urutan ini memudahkan HRD memilih data yang harus dicek lebih dulu agar masalah tidak berulang.";
        if (t.indexOf("efektivitas") >= 0) return "Membandingkan jam kerja, keterlambatan, pulang cepat, dan lembur untuk membaca kecukupan jam kerja efektif. Data ini membantu melihat apakah jam kerja pegawai sudah seimbang dengan beban tugasnya.";
        if (t.indexOf("hari libur") >= 0) return "Menampilkan pegawai yang sering tetap bekerja pada hari libur agar beban kerja tambahan, hak lembur, dan kebutuhan pengaturan jadwal dapat dipantau. Informasi ini juga membantu mencegah kelelahan kerja.";
        if (t.indexOf("anomali") >= 0) return "Menandai data presensi yang tampak tidak wajar, seperti hadir tanpa jam masuk, jam kerja sangat panjang, atau status yang perlu diperiksa ulang. Daftar ini membantu petugas memvalidasi data sebelum laporan dipakai.";
        if (t.indexOf("pengajuan") >= 0) return "Merangkum pengajuan pegawai seperti izin, cuti, tugas, atau kebutuhan administrasi lain agar hubungan antara pengajuan dan kehadiran dapat dilihat dengan mudah.";
        if (t.indexOf("heatmap") >= 0) return "Menunjukkan hari dalam pekan yang paling sering terjadi terlambat, alpha, pulang cepat, atau lembur. Pola ini membantu menentukan hari rawan yang perlu pengawasan atau penyesuaian jadwal.";
        if (t.indexOf("risiko") >= 0) return "Mencari pegawai yang berpotensi mengalami penurunan disiplin berdasarkan gabungan indikator kehadiran. Hasil ini sebaiknya digunakan sebagai bahan pembinaan awal, bukan langsung sebagai kesimpulan akhir.";
        if (t.indexOf("tindak lanjut") >= 0) return "Mengubah data kehadiran menjadi daftar aksi yang mudah dipakai, seperti pegawai yang perlu dikonfirmasi, unit yang perlu dievaluasi, dan pola masalah yang perlu dicegah berulang.";
        if (t.indexOf("radar") >= 0 || t.indexOf("kesehatan") >= 0) return "Merangkum kesehatan kehadiran dalam bentuk indikator ringkas agar pimpinan dapat melihat keseimbangan antara hadir, tepat waktu, alpha, lembur, dan kelengkapan absen.";
        return "Menyajikan analisis tambahan kehadiran dalam bentuk ringkas agar pengguna lebih cepat memahami kondisi, penyebab, dan tindak lanjut yang perlu dilakukan.";
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private MyGrid createGrid(Component parent, int pageSize, String width) {
        MyGrid grid = new MyGrid();
        grid.setMold("paging");
        grid.setPageSize(pageSize);
        grid.setSclass("dgrid fgrid table-striped");
        grid.setWidth(width == null ? "100%" : width);
        grid.setParent(parent);
        return grid;
    }

    private void addColumns(MyGrid grid, String... titles) {
        org.zkoss.zul.Columns cols = new org.zkoss.zul.Columns();
        cols.setParent(grid);
        for (String title : titles) {
            new ais.ui.util.MyColumnConfig(title).setParent(cols);
        }
    }

    private void appendStyledNumber(org.zkoss.zul.Row r, long value, String color) {
        appendStyledText(r, String.valueOf(value), value > 0, color);
    }

    private void appendStyledText(org.zkoss.zul.Row r, String value, boolean highlight, String color) {
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(value);
        if (highlight) {
            lbl.setStyle("color:" + color + "; font-weight:bold;");
        }
        r.appendChild(lbl);
    }

    private void appendBadge(org.zkoss.zul.Row r, String value, String color) {
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(value);
        lbl.setStyle("font-weight:600; padding:4px 8px; border-radius:4px; background-color:" + color + "20; color:" + color + ";");
        r.appendChild(lbl);
    }

    private void appendHeatCell(org.zkoss.zul.Row r, long value, long max) {
        org.zkoss.zul.Label lbl = new org.zkoss.zul.Label(String.valueOf(value));
        if (value > 0) {
            String color = value >= max && max > 0 ? "#dc3545" : "#fd7e14";
            lbl.setStyle("font-weight:bold; padding:4px 8px; border-radius:4px; background-color:" + color + "20; color:" + color + ";");
        }
        r.appendChild(lbl);
    }

    // ---------------------------------------------------------------------
    // Helper kalkulasi ringkasan pegawai
    // ---------------------------------------------------------------------

    private double hitungSkorDisiplin(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        double score = 100.0;
        score -= h.alpa * 10.0;
        score -= h.terlambat * 3.0;
        score -= h.tidakAbsenPulang * 4.0;
        score -= h.pulangcepat * 3.0;
        score += h.tepatWaktuBanget * 1.0;
        if (score < 0.0) score = 0.0;
        if (score > 100.0) score = 100.0;
        return score;
    }

    private String gradeSkor(double skor) {
        if (skor >= 90.0) return "A";
        if (skor >= 80.0) return "B";
        if (skor >= 70.0) return "C";
        if (skor >= 60.0) return "D";
        return "E";
    }

    private String warnaSkor(double skor) {
        if (skor >= 90.0) return "#198754";
        if (skor >= 75.0) return "#0d6efd";
        if (skor >= 60.0) return "#fd7e14";
        return "#dc3545";
    }

    private String rekomendasiSkor(double skor, DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        if (skor < 60.0 || h.alpa >= 3) return "Pembinaan HRD / Surat Peringatan";
        if (skor < 75.0 || h.terlambat >= 5) return "Monitoring dan Teguran";
        if (skor >= 90.0) return "Apresiasi Kedisiplinan";
        return "Normal";
    }

    private double hitungRasioHadir(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        return h.aktif == 0 ? 0.0 : (h.masuk * 100.0 / h.aktif);
    }

    private String warnaPersen(double persen) {
        if (persen >= 90.0) return "#198754";
        if (persen >= 75.0) return "#0d6efd";
        if (persen >= 60.0) return "#fd7e14";
        return "#dc3545";
    }

    private String statusRasioHadir(double rasioHadir) {
        if (rasioHadir >= 95.0) return "Sangat Baik";
        if (rasioHadir >= 85.0) return "Baik";
        if (rasioHadir >= 70.0) return "Cukup";
        return "Perlu Evaluasi";
    }

    private double hitungKonsistensi(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        return h.aktif == 0 ? 0.0 : ((h.tepatWaktu + h.tepatWaktuBanget) * 100.0 / h.aktif);
    }

    private String kategoriKonsistensi(double konsistensi) {
        if (konsistensi >= 90.0) return "Sangat Konsisten";
        if (konsistensi >= 75.0) return "Konsisten";
        if (konsistensi >= 60.0) return "Cukup";
        return "Perlu Pembinaan";
    }

    private long totalTidakHadir(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        return h.alpa + h.sakit + h.izin + h.cuti_memotong + h.cuti_tidak_memotong;
    }

    private String statusKetidakhadiran(DashboardKehadiranExpert.RingkasanPegawaiHolder h, long total) {
        if (h.alpa >= 3) return "Kritis - Alpha Tinggi";
        if (total >= 5) return "Perlu Monitoring";
        return "Normal";
    }

    private double hitungJamEfektif(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        return h.jamMasuk - h.terlambatJam - h.cepatKeluar;
    }

    private String statusEfektivitas(DashboardKehadiranExpert.RingkasanPegawaiHolder h, double jamEfektif) {
        if (h.lemburMasuk > 20.0) return "Overwork / Risiko Burnout";
        if (jamEfektif < 0.0) return "Kurang Efektif";
        if (h.terlambatJam > 5.0 || h.cepatKeluar > 5.0) return "Perlu Evaluasi Jam Kerja";
        return "Produktif";
    }

    private double hitungRiskScore(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        return (h.alpa * 4.0) + (h.terlambat * 1.5) + (h.tidakAbsenPulang * 2.0) + (h.pulangcepat * 1.5)
                + Math.max(0.0, h.terlambatJam) + Math.max(0.0, h.cepatKeluar);
    }

    private String saranRisiko(DashboardKehadiranExpert.RingkasanPegawaiHolder h, double risk) {
        if (h.alpa >= 3 || risk >= 15.0) return "Panggilan HRD dan evaluasi disiplin";
        if (h.terlambat >= 5 || h.tidakAbsenPulang >= 3) return "Monitoring mingguan";
        return "Teguran ringan / reminder presensi";
    }

    private String statusPengajuan(DashboardKehadiranExpert.RingkasanPegawaiHolder h) {
        if (h.cuti_memotong > h.jumlahCutiYangBisaDiambil) return "Cuti Melebihi Kuota";
        if (h.jumlahPengajuan >= 5) return "Frekuensi Pengajuan Tinggi";
        if (h.jumlahPengajuan > 0) return "Ada Pengajuan";
        return "Normal";
    }

    // ---------------------------------------------------------------------
    // Helper dari log harian
    // ---------------------------------------------------------------------

    private boolean isHariLibur(StatuskehadiranKaryawanHarian skh) {
        if (skh == null) return false;
        String key = buildLogHarianKey(skh);
        if (data != null && data.hariLiburLogKeys != null && data.hariLiburLogKeys.contains(key)) {
            return true;
        }
        boolean holiday = skh.getTanggal() != null && Common.isHoliday(skh.getTanggal());
        try {
            if (skh.getLiburNasional() != null) {
                holiday = true;
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:675");
        }
        if (isHariLiburDitentukanOlehShift(skh)) {
            return isKhususBuatHariLibur(skh);
        }
        return holiday;
    }

    private boolean isHariLiburDitentukanOlehShift(StatuskehadiranKaryawanHarian skh) {
        try {
            return skh != null
                    && skh.getDetailJenisShiftPegawai() != null
                    && skh.getDetailJenisShiftPegawai().getJenisShiftPegawai() != null
                    && Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getJenisShiftPegawai()
                            .getHariLiburDitentukan());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:690");
        }
        return false;
    }

    private boolean isKhususBuatHariLibur(StatuskehadiranKaryawanHarian skh) {
        try {
            return skh != null && skh.getDetailJenisShiftPegawai() != null
                    && Boolean.TRUE.equals(skh.getDetailJenisShiftPegawai().getKhususBuatHariLibur());
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:699");
        }
        return false;
    }

    private String buildLogHarianKey(StatuskehadiranKaryawanHarian skh) {
        String pegawaiKey = skh != null && skh.getPegawai() != null && skh.getPegawai().getId() != null
                ? String.valueOf(skh.getPegawai().getId())
                : "pegawai-null";
        String tanggalKey = skh != null && skh.getTanggal() != null ? Common.dateFormat83.get().format(skh.getTanggal())
                : "tanggal-null";
        return tanggalKey + "_" + pegawaiKey;
    }

    private Map<String, MasukLiburHolder> buildMasukLiburMap() {
        Map<String, MasukLiburHolder> map = new LinkedHashMap<String, MasukLiburHolder>();
        for (StatuskehadiranKaryawanHarian skh : data.listKehadiranFiltered) {
            if (skh == null || skh.getTanggal() == null || skh.getPegawai() == null) continue;
            boolean holiday = isHariLibur(skh);
            if (!holiday || skh.ambilMasukjam() == null) continue;

            String key = String.valueOf(skh.getPegawai().getId());
            MasukLiburHolder h = map.get(key);
            if (h == null) {
                h = new MasukLiburHolder();
                h.namaPegawai = getNamaPegawai(skh);
                h.namaSatker = getNamaSatker(skh);
                map.put(key, h);
            }
            h.jumlahHari++;
            h.totalJamKerja += skh.getJumlahJamMasuk() != null ? skh.getJumlahJamMasuk() : 0.0;
            h.totalJamLembur += getLemburValid(skh);
        }
        for (MasukLiburHolder h : map.values()) {
            h.totalJamLembur = batasiJamLembur(h.totalJamLembur);
        }
        return map;
    }

    private Map<Integer, HariStatHolder> buildHariStatMap() {
        Map<Integer, HariStatHolder> map = new java.util.TreeMap<Integer, HariStatHolder>();
        for (StatuskehadiranKaryawanHarian skh : data.listKehadiranFiltered) {
            if (skh == null || skh.getTanggal() == null) continue;
            Calendar cal = Calendar.getInstance();
            cal.setTime(skh.getTanggal());
            Integer hari = Integer.valueOf(cal.get(Calendar.DAY_OF_WEEK));
            HariStatHolder h = map.get(hari);
            if (h == null) {
                h = new HariStatHolder(hari.intValue());
                map.put(hari, h);
            }
            h.totalLog++;
            Statusabsensi status = skh.getStatusabsensi();
            boolean holiday = isHariLibur(skh);
            if (Boolean.TRUE.equals(skh.getDatangTerlambat())) h.totalTerlambat++;
            if (status != null && status.getId().equals(2L) && !holiday) h.totalAlpha++;
            if (Boolean.TRUE.equals(skh.getPulangCepat())) h.totalPulangCepat++;
            if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null) h.totalTidakAbsenPulang++;
            h.totalJamLembur += getLemburValid(skh);
        }
        for (HariStatHolder h : map.values()) {
            h.totalJamLembur = batasiJamLembur(h.totalJamLembur);
        }
        return map;
    }

    private List<AnomaliInfo> cekAnomali(StatuskehadiranKaryawanHarian skh) {
        List<AnomaliInfo> list = new java.util.ArrayList<AnomaliInfo>();
        if (skh == null) return list;

        Statusabsensi status = skh.getStatusabsensi();
        boolean statusMasuk = status != null && status.getId().equals(ais.common.ConstantValues.MASUK.getId());
        boolean holiday = isHariLibur(skh);
        double lembur = getLemburValid(skh);
        double jamKerja = skh.getJumlahJamMasuk() != null ? skh.getJumlahJamMasuk() : 0.0;

        if (skh.ambilMasukjam() != null && skh.ambilPulangjam() == null) {
            list.add(new AnomaliInfo("Tidak Absen Pulang", "Ada jam masuk tetapi jam pulang kosong", "Tinggi"));
        }
        if (statusMasuk && skh.ambilMasukjam() == null) {
            list.add(new AnomaliInfo("Status Masuk Tanpa Jam Masuk", "Status hadir tetapi jam masuk kosong", "Tinggi"));
        }
        if (holiday && skh.ambilMasukjam() != null) {
            list.add(new AnomaliInfo("Masuk Hari Libur", "Pegawai tercatat masuk pada hari libur", "Sedang"));
        }
        if (lembur > 8.0) {
            list.add(new AnomaliInfo("Lembur Sangat Tinggi", "Jam lembur lebih dari 8 jam dalam satu hari", "Tinggi"));
        }
        if (statusMasuk && jamKerja > 0.0 && jamKerja < 2.0) {
            list.add(new AnomaliInfo("Jam Kerja Sangat Rendah", "Total jam kerja kurang dari 2 jam", "Sedang"));
        }
        if (Boolean.TRUE.equals(skh.getPulangCepat())) {
            list.add(new AnomaliInfo("Pulang Cepat", "Pegawai pulang lebih cepat dari jadwal", "Rendah"));
        }
        return list;
    }

    private double getLemburValid(StatuskehadiranKaryawanHarian skh) {
        try {
            if (skh != null && skh.getPegawai() != null && skh.getPegawai().getTipePegawai() != null
                    && Boolean.TRUE.equals(skh.getPegawai().getTipePegawai().getMasukLembur())) {
                double jamLembur = skh.getJumlahLemburMasuk() != null ? skh.getJumlahLemburMasuk() : 0.0;
                return batasiJamLembur(jamLembur);
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:803");
        }
        return 0.0;
    }

    private double batasiJamLembur(double jamLembur) {
        if (jamLembur < 0.0) {
            return 0.0;
        }
        int bebanLemburPegawaiMax = data != null ? data.bebanLemburPegawaiMax : -1;
        if (bebanLemburPegawaiMax != -1 && jamLembur > bebanLemburPegawaiMax) {
            return bebanLemburPegawaiMax;
        }
        return jamLembur;
    }

    private String getNamaPegawai(StatuskehadiranKaryawanHarian skh) {
        return skh != null && skh.getPegawai() != null && skh.getPegawai().getNama() != null
                ? skh.getPegawai().getNama() : "Anonim";
    }

    private String getNamaSatker(StatuskehadiranKaryawanHarian skh) {
        return skh != null && skh.getPegawai() != null && skh.getPegawai().getSatuanKerja() != null
                && skh.getPegawai().getSatuanKerja().getNama() != null
                ? skh.getPegawai().getSatuanKerja().getNama() : "-";
    }

    private String formatTanggal(Date tanggal) {
        return tanggal == null ? "-" : Common.dateFormat4.get().format(tanggal);
    }

    private String formatJam(double value) {
        return Common.numberFormat.get().format(value) + " Jam";
    }

    private String warnaPrioritas(String prioritas) {
        if ("Tinggi".equalsIgnoreCase(prioritas)) return "#dc3545";
        if ("Sedang".equalsIgnoreCase(prioritas)) return "#fd7e14";
        return "#0d6efd";
    }

    private String namaHari(int hari) {
        try {
            if (Common.haris != null && Common.haris.length >= hari) return Common.haris[hari - 1];
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/DashboardKehadiranTambahan.java:847");
        }
        if (hari == Calendar.SUNDAY) return "Minggu";
        if (hari == Calendar.MONDAY) return "Senin";
        if (hari == Calendar.TUESDAY) return "Selasa";
        if (hari == Calendar.WEDNESDAY) return "Rabu";
        if (hari == Calendar.THURSDAY) return "Kamis";
        if (hari == Calendar.FRIDAY) return "Jumat";
        if (hari == Calendar.SATURDAY) return "Sabtu";
        return "-";
    }

    private String catatanHari(HariStatHolder h) {
        if (h.totalAlpha > 0 && h.totalTerlambat > 0) return "Rawan telat dan alpha";
        if (h.totalTerlambat > 0) return "Rawan keterlambatan";
        if (h.totalAlpha > 0) return "Rawan alpha";
        if (h.totalJamLembur > 0.0) return "Ada beban lembur";
        return "Normal";
    }

    private long maxTerlambat(Map<Integer, HariStatHolder> map) {
        long max = 0;
        for (HariStatHolder h : map.values()) if (h.totalTerlambat > max) max = h.totalTerlambat;
        return max;
    }

    private long maxAlpha(Map<Integer, HariStatHolder> map) {
        long max = 0;
        for (HariStatHolder h : map.values()) if (h.totalAlpha > max) max = h.totalAlpha;
        return max;
    }

    private long maxPulangCepat(Map<Integer, HariStatHolder> map) {
        long max = 0;
        for (HariStatHolder h : map.values()) if (h.totalPulangCepat > max) max = h.totalPulangCepat;
        return max;
    }

    private long maxTidakAbsenPulang(Map<Integer, HariStatHolder> map) {
        long max = 0;
        for (HariStatHolder h : map.values()) if (h.totalTidakAbsenPulang > max) max = h.totalTidakAbsenPulang;
        return max;
    }

	private void renderRingkasanTindakLanjutHrd() {
		org.zkoss.zul.Panelchildren pch = createPanel("Ringkasan Tindak Lanjut HRD", pcBottom, null);
		long totalPegawai = data == null || data.mapRingkasan == null ? 0L : data.mapRingkasan.size();
		long perluPembinaan = 0L;
		long perluValidasiAbsen = 0L;
		long perluAturBeban = 0L;
		long layakApresiasi = 0L;

		if (data != null && data.mapRingkasan != null) {
			for (DashboardKehadiranExpert.RingkasanPegawaiHolder h : data.mapRingkasan.values()) {
				if (h == null) {
					continue;
				}
				double skor = hitungSkorDisiplin(h);
				if (h.alpa > 0 || h.terlambat >= 3 || h.pulangcepat >= 3) {
					perluPembinaan++;
				}
				if (h.tidakAbsenPulang > 0) {
					perluValidasiAbsen++;
				}
				if (h.lemburMasuk > 20.0 || h.jamMasuk > 240.0) {
					perluAturBeban++;
				}
				if (skor >= 90.0 && h.alpa == 0 && h.tidakAbsenPulang == 0) {
					layakApresiasi++;
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<div style='font-size:12px; line-height:1.65; color:#475569; margin-bottom:12px;'>")
				.append("Ringkasan ini mengubah data presensi menjadi arahan kerja yang mudah dipahami. HRD dapat melihat jumlah pegawai yang perlu pembinaan, validasi absen, pengaturan beban kerja, atau apresiasi kedisiplinan.")
				.append("</div>");
		sb.append("<div style='display:grid; grid-template-columns:repeat(auto-fit,minmax(210px,1fr)); gap:12px;'>");
		sb.append(buildActionCardHtml("Total Pegawai Dianalisis", String.valueOf(totalPegawai),
				"Jumlah pegawai yang masuk ke perhitungan sesuai periode dan filter yang dipilih.", "#eff6ff", "#1d4ed8"));
		sb.append(buildActionCardHtml("Perlu Pembinaan", String.valueOf(perluPembinaan),
				"Pegawai dengan alpha, sering terlambat, atau pulang cepat yang sebaiknya dikonfirmasi lebih dulu.", "#fee2e2", "#991b1b"));
		sb.append(buildActionCardHtml("Perlu Validasi Absen", String.valueOf(perluValidasiAbsen),
				"Pegawai dengan catatan tidak absen pulang yang perlu dicek agar laporan tidak salah baca.", "#fff7ed", "#9a3412"));
		sb.append(buildActionCardHtml("Perlu Atur Beban Kerja", String.valueOf(perluAturBeban),
				"Pegawai dengan jam kerja atau lembur tinggi yang perlu dipantau agar beban kerja tetap sehat.", "#f5f3ff", "#6d28d9"));
		sb.append(buildActionCardHtml("Layak Apresiasi", String.valueOf(layakApresiasi),
				"Pegawai dengan kedisiplinan sangat baik yang dapat dipertimbangkan untuk apresiasi atau contoh praktik baik.", "#ecfdf5", "#166534"));
		sb.append("</div>");
		pch.appendChild(new org.zkoss.zul.Html(sb.toString()));
	}

	private String buildActionCardHtml(String title, String value, String description, String background, String color) {
		return "<div style='padding:14px; border-radius:16px; background:" + background
				+ "; border:1px solid rgba(15,23,42,.08); min-height:128px;'>"
				+ "<div style='font-size:11px; font-weight:800; letter-spacing:.04em; text-transform:uppercase; color:"
				+ color + ";'>" + escapeHtml(title) + "</div>"
				+ "<div style='font-size:28px; font-weight:900; color:" + color + "; margin-top:8px;'>"
				+ escapeHtml(value) + "</div>"
				+ "<div style='font-size:11px; line-height:1.5; color:" + color
				+ "; opacity:.86; margin-top:6px;'>" + escapeHtml(description) + "</div></div>";
	}


    // ---------------------------------------------------------------------
    // Holder lokal khusus class tambahan
    // ---------------------------------------------------------------------

    private static class MasukLiburHolder {
        String namaPegawai;
        String namaSatker;
        long jumlahHari = 0L;
        double totalJamKerja = 0.0;
        double totalJamLembur = 0.0;
    }

    private static class HariStatHolder {
        @SuppressWarnings("unused")
		int hari;
        long totalLog = 0L;
        long totalTerlambat = 0L;
        long totalAlpha = 0L;
        long totalPulangCepat = 0L;
        long totalTidakAbsenPulang = 0L;
        double totalJamLembur = 0.0;

        HariStatHolder(int hari) {
            this.hari = hari;
        }
    }

    private static class AnomaliInfo {
        String jenis;
        String keterangan;
        String prioritas;

        AnomaliInfo(String jenis, String keterangan, String prioritas) {
            this.jenis = jenis;
            this.keterangan = keterangan;
            this.prioritas = prioritas;
        }
    }
}
