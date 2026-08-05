package ais.action.master.sekolah;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.PerguruanTinggi;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.ui.util.MyCombobox;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyHtml;

/**
 * Rekap Multi-Tahun Jalur Masuk PSB (Calon Siswa).
 *
 * <p>Menampilkan tabel rekap 4 tahun terakhir per Gelombang Pendaftaran PSB,
 * dengan kolom: Peminat, Diterima, Bayar Pendaftaran, Daftar Ulang.
 * Filter opsional: Paket PSB dan Penjurusan Sekolah.</p>
 */
public class RekapJalurMasukMultiTahunPsb extends DashboardSiswaBase {

    private Combobox cboPaket;
    private Combobox cboPenjurusan;

    private static final class RowData {
        int peminat;
        int diterima;
        int bayarPendaftaran;
        int daftarUlang;
    }

    public RekapJalurMasukMultiTahunPsb(PerguruanTinggi pt) {
        super(pt);
    }

    @Override
    protected void buildExtraFilter(Div filterBar) {
        Label sep = new Label("|");
        sep.setStyle("color:#bbb;");
        sep.setParent(filterBar);

        Label lblPaket = new Label(ais.common.Common.getBahasaConfig("Paket:"));
        lblPaket.setStyle("font-size:13px;font-weight:600;white-space:nowrap;");
        lblPaket.setParent(filterBar);
        cboPaket = buildComboPaket();
        cboPaket.setParent(filterBar);

        Label lblPenjurusan = new Label(ais.common.Common.getBahasaConfig("Penjurusan:"));
        lblPenjurusan.setStyle("font-size:13px;font-weight:600;white-space:nowrap;");
        lblPenjurusan.setParent(filterBar);
        cboPenjurusan = buildComboPenjurusan();
        cboPenjurusan.setParent(filterBar);
    }

    @SuppressWarnings("unchecked")
    private MyCombobox buildComboPaket() {
        MyCombobox cbo = new MyCombobox();
        cbo.setWidth("140px");
        cbo.setReadonly(true);
        MyComboitemConfig all = new MyComboitemConfig();
        all.setLabel(ais.common.Common.getBahasaConfig("Semua Paket"));
        all.setValue(null);
        all.setParent(cbo);
        cbo.setSelectedItem(all);
        try {
            List<PaketPsb> list = HibernateUtil.currentSession()
                    .createQuery("FROM PaketPsb ORDER BY nama ASC").list();
            for (PaketPsb p : list) {
                MyComboitemConfig ci = new MyComboitemConfig();
                ci.setLabel(p.getNama());
                ci.setValue(p.getId());
                ci.setParent(cbo);
            }
        } catch (Exception e) {
            logErr("RekapJalurMasukMultiTahunPsb.buildComboPaket", e);
        }
        return cbo;
    }

    @SuppressWarnings("unchecked")
    private MyCombobox buildComboPenjurusan() {
        MyCombobox cbo = new MyCombobox();
        cbo.setWidth("150px");
        cbo.setReadonly(true);
        MyComboitemConfig all = new MyComboitemConfig();
        all.setLabel(ais.common.Common.getBahasaConfig("Semua Penjurusan"));
        all.setValue(null);
        all.setParent(cbo);
        cbo.setSelectedItem(all);
        try {
            List<PenjurusanSekolah> list = HibernateUtil.currentSession()
                    .createQuery("FROM PenjurusanSekolah ORDER BY nama ASC").list();
            for (PenjurusanSekolah p : list) {
                MyComboitemConfig ci = new MyComboitemConfig();
                ci.setLabel(p.getNama());
                ci.setValue(p.getId());
                ci.setParent(cbo);
            }
        } catch (Exception e) {
            logErr("RekapJalurMasukMultiTahunPsb.buildComboPenjurusan", e);
        }
        return cbo;
    }

    private Object getSelectedPaketId() {
        if (cboPaket == null || cboPaket.getSelectedItem() == null) { return null; }
        return cboPaket.getSelectedItem().getValue();
    }

    private Object getSelectedPenjurusanId() {
        if (cboPenjurusan == null || cboPenjurusan.getSelectedItem() == null) { return null; }
        return cboPenjurusan.getSelectedItem().getValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doRefreshSiswa(Session session, int tahunMasuk) {
        List<Integer>              years    = computeYears(tahunMasuk);
        Object                     paketId  = getSelectedPaketId();
        Object                     penjId   = getSelectedPenjurusanId();

        List<GelombangPendaftaranPsb> allGel = session.createQuery(
                "FROM GelombangPendaftaranPsb ORDER BY nama ASC").list();

        Map<Object, Map<Integer, RowData>> data = new LinkedHashMap<Object, Map<Integer, RowData>>();
        for (GelombangPendaftaranPsb g : allGel) {
            data.put(g.getId(), new LinkedHashMap<Integer, RowData>());
        }

        loadMetric(session, years, paketId, penjId, data, "peminat",          null);
        loadMetric(session, years, paketId, penjId, data, "diterima",
                "AND cs.telahDiterima = true");
        loadMetric(session, years, paketId, penjId, data, "bayarPendaftaran",
                "AND cs.riwayatPembayaranPendaftaran IS NOT NULL "
                + "AND cs.riwayatPembayaranPendaftaran <> ''");
        loadMetric(session, years, paketId, penjId, data, "daftarUlang",
                "AND cs.riwayatPembayaranDaftarUlang IS NOT NULL "
                + "AND cs.riwayatPembayaranDaftarUlang <> ''");

        new MyHtml(buildTableHtml(allGel, years, data)).setParent(contentHolder);
    }

    @SuppressWarnings("unchecked")
    private void loadMetric(Session session, List<Integer> years,
                            Object paketId, Object penjId,
                            Map<Object, Map<Integer, RowData>> data,
                            String metric, String extraWhere) {
        try {
            StringBuilder hql = new StringBuilder(
                    "SELECT cs.gelombangPendaftaranPsb.id, cs.tahunMasuk, COUNT(cs) "
                    + "FROM CalonSiswa cs "
                    + "WHERE cs.gelombangPendaftaranPsb IS NOT NULL "
                    + "AND cs.tahunMasuk IN (:years)");
            if (paketId  != null) { hql.append(" AND cs.paketPsb.id = :paketId"); }
            if (penjId   != null) { hql.append(" AND cs.penjurusanSekolah.id = :penjId"); }
            if (extraWhere != null) { hql.append(" ").append(extraWhere); }
            hql.append(" GROUP BY cs.gelombangPendaftaranPsb.id, cs.tahunMasuk");

            Query q = session.createQuery(hql.toString());
            q.setParameterList("years", years);
            if (paketId  != null) { q.setParameter("paketId", paketId); }
            if (penjId   != null) { q.setParameter("penjId",  penjId); }

            for (Object[] row : (List<Object[]>) q.list()) {
                Object gelId = row[0];
                int    yr    = row[1] instanceof Integer ? (Integer) row[1] : 0;
                int    cnt   = ((Number) row[2]).intValue();
                if (!data.containsKey(gelId)) { continue; }
                Map<Integer, RowData> byYear = data.get(gelId);
                if (!byYear.containsKey(yr)) { byYear.put(yr, new RowData()); }
                RowData rd = byYear.get(yr);
                if      ("peminat".equals(metric))          { rd.peminat = cnt; }
                else if ("diterima".equals(metric))         { rd.diterima = cnt; }
                else if ("bayarPendaftaran".equals(metric)) { rd.bayarPendaftaran = cnt; }
                else if ("daftarUlang".equals(metric))      { rd.daftarUlang = cnt; }
            }
        } catch (Exception e) {
            logErr("RekapJalurMasukMultiTahunPsb.loadMetric[" + metric + "]", e);
        }
    }

    private List<Integer> computeYears(int tahunMasuk) {
        List<Integer> years = new ArrayList<Integer>();
        for (int i = 3; i >= 0; i--) { years.add(Integer.valueOf(tahunMasuk - i)); }
        return years;
    }

    private String buildTableHtml(List<GelombangPendaftaranPsb> allGel,
                                  List<Integer> years,
                                  Map<Object, Map<Integer, RowData>> data) {
        int numYears = years.size();
        int colSpan  = 1 + numYears * 4;

        StringBuilder sb = new StringBuilder(4096);
        sb.append("<div style='overflow-x:auto;'>");
        sb.append("<table class='z-table' "
                + "style='border-collapse:collapse;width:100%;font-size:12px;'>");

        // ── Header baris 1: grup tahun
        sb.append("<thead><tr>")
          .append("<th rowspan='2' style='").append(thStyle("left")).append("'>Gelombang</th>");
        for (Integer y : years) {
            sb.append("<th colspan='4' style='").append(thStyle("center")).append("'>")
              .append(y).append("</th>");
        }
        sb.append("</tr>");

        // ── Header baris 2: sub-kolom
        sb.append("<tr>");
        for (int i = 0; i < numYears; i++) {
            sb.append("<th style='").append(thStyle("center")).append("' title='Peminat'>PMT</th>");
            sb.append("<th style='").append(thStyle("center")).append("' title='Diterima'>DTR</th>");
            sb.append("<th style='").append(thStyle("center"))
              .append("' title='Bayar Pendaftaran'>BPD</th>");
            sb.append("<th style='").append(thStyle("center"))
              .append("' title='Daftar Ulang'>DU</th>");
        }
        sb.append("</tr></thead>");

        // ── Akumulator total
        int[] totPmt = new int[numYears];
        int[] totDtr = new int[numYears];
        int[] totBpd = new int[numYears];
        int[] totDu  = new int[numYears];

        // ── Baris data
        sb.append("<tbody>");
        boolean hasData = false;
        for (GelombangPendaftaranPsb gel : allGel) {
            Map<Integer, RowData> byYear = data.get(gel.getId());
            boolean adaData = false;
            for (Integer y : years) {
                RowData rd = byYear.get(y);
                if (rd != null && rd.peminat > 0) { adaData = true; break; }
            }
            if (!adaData) { continue; }
            hasData = true;

            sb.append("<tr><td style='").append(tdStyle("left")).append("'>")
              .append(escHtml(gel.getNama())).append("</td>");
            for (int i = 0; i < numYears; i++) {
                RowData rd = byYear.get(years.get(i));
                int pmt = rd != null ? rd.peminat : 0;
                int dtr = rd != null ? rd.diterima : 0;
                int bpd = rd != null ? rd.bayarPendaftaran : 0;
                int du  = rd != null ? rd.daftarUlang : 0;
                totPmt[i] += pmt; totDtr[i] += dtr;
                totBpd[i] += bpd; totDu[i]  += du;
                sb.append("<td style='").append(tdStyle("right")).append("'>").append(dash(pmt)).append("</td>");
                sb.append("<td style='").append(tdStyle("right")).append("'>").append(dash(dtr)).append("</td>");
                sb.append("<td style='").append(tdStyle("right")).append("'>").append(dash(bpd)).append("</td>");
                sb.append("<td style='").append(tdStyle("right")).append("'>").append(dash(du)).append("</td>");
            }
            sb.append("</tr>");
        }

        if (!hasData) {
            sb.append("<tr><td colspan='").append(colSpan)
              .append("' style='text-align:center;padding:20px;color:#888;'>")
              .append("Tidak ada data untuk filter yang dipilih.</td></tr>");
        }
        sb.append("</tbody>");

        // ── Footer total
        sb.append("<tfoot><tr><td style='").append(thStyle("left")).append("'>JUMLAH</td>");
        for (int i = 0; i < numYears; i++) {
            sb.append("<td style='").append(thStyle("right")).append("'>").append(fmtAngka(totPmt[i])).append("</td>");
            sb.append("<td style='").append(thStyle("right")).append("'>").append(fmtAngka(totDtr[i])).append("</td>");
            sb.append("<td style='").append(thStyle("right")).append("'>").append(fmtAngka(totBpd[i])).append("</td>");
            sb.append("<td style='").append(thStyle("right")).append("'>").append(fmtAngka(totDu[i])).append("</td>");
        }
        sb.append("</tr></tfoot>");

        sb.append("</table>");
        sb.append("<div style='margin-top:8px;font-size:11px;color:#666;'>");
        sb.append("PMT = Peminat &nbsp;&nbsp; DTR = Diterima &nbsp;&nbsp; "
                + "BPD = Bayar Pendaftaran &nbsp;&nbsp; DU = Daftar Ulang");
        sb.append("</div></div>");
        return sb.toString();
    }

    private String thStyle(String align) {
        return "background:#4a5568;color:#fff;padding:6px 8px;text-align:" + align
             + ";border:1px solid #2d3748;white-space:nowrap;font-weight:600;";
    }

    private String tdStyle(String align) {
        return "padding:5px 8px;border:1px solid #e2e8f0;text-align:" + align + ";";
    }

    private static String dash(int n) {
        return n == 0 ? "-" : String.valueOf(n);
    }
}
