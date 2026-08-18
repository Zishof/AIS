package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;

/**
 * Ramalan / Forecast Penjualan — konversi &amp; peningkatan dari {@code modul/kantin/_forecast.jsp}.
 *
 * <p>Menampilkan tren jumlah transaksi &amp; omzet (harian/mingguan/bulanan) lalu memproyeksikan
 * beberapa periode ke depan memakai garis tren sederhana (regresi linear). Tujuannya membantu
 * memperkirakan keramaian/penjualan agar stok &amp; tenaga bisa disiapkan. Grafik HTML/CSS
 * ({@link DashboardUiKit}), tanpa JFreeChart.</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang hanya melihat penjualan tokonya sendiri.</p>
 */
public class ForecastPenjualanKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;
    private Div content;
    private Combobox cboPeriode;
    private Toko scopeToko;

    @Override
    public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
            Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
        Common.doCheckSecurity();
        return super.doBeforeCompose(page, parent, compInfo);
    }

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        Common.initLaguage();
        resolveScopeToko();

        DashboardUiKit.attachIntro(comp, "Ramalan Penjualan",
                "Lihat naik-turunnya transaksi dan omzet, lalu perkiraan beberapa periode ke depan "
                        + "supaya stok dan tenaga kerja bisa disiapkan lebih awal.");

        if (host == null) {
            return;
        }
        buildFilter();
        build();
    }

    private void resolveScopeToko() {
        try {
            Toko ct = Common.getCurrentToko();
            if (ct != null && ct.getId() != null) {
                Boolean b = ct.getBolehMelihatTokolain();
                if (b == null || !b.booleanValue()) {
                    scopeToko = ct;
                }
            }
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/ForecastPenjualanKantinAction.java:73");
        }
        // 2026-07-26: saat dasbor ini dimuat sbg tab "Ramalan Penjualan" dari Dasbor Kantin (lewat
        // MyInclude), warisi pilihan "Cari Toko" admin di sana lewat parameter URL "tokoId" -- HANYA
        // kalau pengguna belum terkunci sendiri ke satu toko di atas (scopeToko masih null), supaya
        // parameter dari URL tak pernah bisa membajak kunci keamanan pedagang.
        if (scopeToko == null) {
            try {
                String tp = org.zkoss.zk.ui.Executions.getCurrent().getParameter("tokoId");
                if (tp != null && tp.trim().length() > 0) {
                    Long tid = Long.valueOf(tp.trim());
                    org.hibernate.Session session = HibernateUtil.currentSession();
                    scopeToko = (Toko) session.get(Toko.class, tid);
                }
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/ForecastPenjualanKantinAction.java:tokoIdParam");
            }
        }
    }

    private void buildFilter() {
        host.getChildren().clear();

        Div card = new Div();
        card.setSclass("ais-crud-filter-card");
        card.setParent(host);
        Div body = new Div();
        body.setSclass("ais-crud-filter-body");
        body.setParent(card);
        body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Pilih satuan waktu. Garis putus-putus pada proyeksi adalah perkiraan, bukan kepastian.")));

        Div ctr = new Div();
        ctr.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        ctr.setParent(body);

        cboPeriode = new Combobox();
        cboPeriode.setWidth("100%");
        cboPeriode.setReadonly(true);
        addItem("Harian (14 hari terakhir)", "harian");
        addItem("Mingguan (8 minggu terakhir)", "mingguan");
        addItem("Bulanan (12 bulan terakhir)", "bulanan");
        cboPeriode.setSelectedIndex(0);
        cboPeriode.addEventListener("onSelect", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                build();
            }
        });
        Div k = new Div();
        k.setStyle("display:flex;flex-direction:column;flex:1 1 220px;");
        org.zkoss.zul.Label l = new org.zkoss.zul.Label(ais.common.Common.getBahasaConfig("Satuan Waktu"));
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;margin-bottom:3px;");
        k.appendChild(l);
        k.appendChild(cboPeriode);
        ctr.appendChild(k);

        content = new Div();
        content.setSclass("ais-crud-data-card");
        content.setParent(host);
    }

    private void addItem(String label, String code) {
        Comboitem ci = new Comboitem(label);
        ci.setValue(code);
        ci.setParent(cboPeriode);
    }

    private void build() {
        if (content == null) {
            return;
        }
        content.getChildren().clear();
        StringBuilder sb = new StringBuilder();

        String p = periodeCode();
        String satuan = "harian".equals(p) ? "hari" : "mingguan".equals(p) ? "minggu" : "bulan";
        String condToko = scopeToko == null ? "" : " AND toko = " + scopeToko.getId() + " ";

        String trunc;
        String label;
        String range;
        if ("mingguan".equals(p)) {
            trunc = "DATE_TRUNC('week', waktu)";
            label = "TO_CHAR(DATE_TRUNC('week', waktu),'DD Mon')";
            range = "CURRENT_DATE - INTERVAL '8 weeks'";
        } else if ("bulanan".equals(p)) {
            trunc = "DATE_TRUNC('month', waktu)";
            label = "TO_CHAR(DATE_TRUNC('month', waktu),'Mon YYYY')";
            range = "CURRENT_DATE - INTERVAL '12 months'";
        } else {
            trunc = "DATE(waktu)";
            label = "TO_CHAR(DATE(waktu),'DD Mon')";
            range = "CURRENT_DATE - INTERVAL '14 days'";
        }

        List<String> labels = new ArrayList<String>();
        List<Double> counts = new ArrayList<Double>();
        List<Double> omzet = new ArrayList<Double>();
        for (Object[] r : rows("SELECT " + label + " l, "
                + "COUNT(DISTINCT COALESCE(pembelian_anggota_koperasi, id)) c, COALESCE(SUM(total),0) o "
                + "FROM koperasi.pembelian WHERE waktu >= " + range + condToko
                + " GROUP BY " + trunc + " ORDER BY " + trunc + " ASC")) {
            labels.add(str(r[0]));
            counts.add(num(r[1]));
            omzet.add(num(r[2]));
        }

        if (counts.isEmpty()) {
            content.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>Belum ada data penjualan untuk diramalkan.</div>"));
            return;
        }

        double total = 0;
        for (Double d : counts) {
            total += d;
        }
        double rata = total / counts.size();

        // Regresi linear sederhana pada jumlah transaksi (x = indeks periode).
        double[] mb = regresi(counts);
        double slope = mb[0];
        double intercept = mb[1];
        int n = counts.size();
        double prediksiBerikut = Math.max(0, slope * n + intercept);

        // Arah tren: bandingkan prediksi vs rata-rata historis.
        boolean naik = slope >= 0;
        double persen = rata > 0 ? (slope / rata) * 100.0 : 0;

        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Total Transaksi", DashboardUiKit.money(total),
                counts.size() + " " + satuan + " terakhir", DashboardUiKit.PRIMARY));
        kartu.add(new DashboardUiKit.Stat("Rata-rata / " + satuan, DashboardUiKit.money(Math.round(rata)),
                "transaksi", DashboardUiKit.ACCENT));
        kartu.add(new DashboardUiKit.Stat("Perkiraan " + satuan + " berikutnya",
                "± " + DashboardUiKit.money(Math.round(prediksiBerikut)), "transaksi (proyeksi)",
                DashboardUiKit.GOOD));
        kartu.add(new DashboardUiKit.Stat("Arah Tren", (naik ? "Naik " : "Turun ") + DashboardUiKit.money(Math.abs(Math.round(persen))) + "%",
                "per " + satuan, naik ? DashboardUiKit.GOOD : DashboardUiKit.WARN));
        sb.append(DashboardUiKit.descChip("Ringkasan tren dan perkiraan jumlah transaksi."));
        sb.append(DashboardUiKit.cards(kartu));

        // Tren historis (jumlah transaksi + omzet).
        sb.append(DashboardUiKit.sparkline("Tren Jumlah Transaksi",
                "Banyaknya transaksi tiap " + satuan + " (" + labels.size() + " periode terakhir).", counts,
                DashboardUiKit.PRIMARY, "Belum ada data."));
        sb.append(DashboardUiKit.sparkline("Tren Omzet",
                "Total nilai penjualan tiap " + satuan + ".", omzet, DashboardUiKit.GOOD, "Belum ada data."));

        // Proyeksi 3 periode ke depan (garis tren).
        LinkedHashMap<String, Double> proyeksi = new LinkedHashMap<String, Double>();
        for (int i = 1; i <= 3; i++) {
            double v = Math.max(0, slope * (n - 1 + i) + intercept);
            proyeksi.put(satuan + " ke-" + i + " mendatang", (double) Math.round(v));
        }
        sb.append(DashboardUiKit.barList("Proyeksi Transaksi ke Depan",
                "Perkiraan jumlah transaksi untuk 3 " + satuan + " berikutnya berdasarkan garis tren.",
                proyeksi, DashboardUiKit.ACCENT, " trx", false, "Belum bisa diproyeksikan."));

        content.appendChild(DashboardUiKit.html(sb.toString()));
    }

    /** Regresi linear least-squares; mengembalikan {slope, intercept} untuk y pada x = 0..n-1. */
    private static double[] regresi(List<Double> y) {
        int n = y.size();
        if (n < 2) {
            double only = n == 1 ? y.get(0).doubleValue() : 0;
            return new double[] { 0, only };
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double xi = i;
            double yi = y.get(i).doubleValue();
            sumX += xi;
            sumY += yi;
            sumXY += xi * yi;
            sumX2 += xi * xi;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) {
            return new double[] { 0, sumY / n };
        }
        double slope = (n * sumXY - sumX * sumY) / denom;
        double intercept = (sumY - slope * sumX) / n;
        return new double[] { slope, intercept };
    }

    private String periodeCode() {
        if (cboPeriode != null && cboPeriode.getSelectedItem() != null
                && cboPeriode.getSelectedItem().getValue() != null) {
            return cboPeriode.getSelectedItem().getValue().toString();
        }
        return "harian";
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql) {
        try {
            SQLQuery q = HibernateUtil.currentSession().createSQLQuery(sql);
            return q.list();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return new ArrayList<Object[]>();
        }
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
