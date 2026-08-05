package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.hibernate.SQLQuery;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.ui.util.DashboardUiKit;

/**
 * Monitor Pemakaian Diskon &amp; Cashback — konversi dari {@code modul/kantin/diskon/monitor.jsp} ke ZK.
 *
 * <p>Memantau berapa banyak diskon &amp; cashback yang diberikan, berapa yang sudah dicairkan, dan
 * berapa saldo cashback yang masih mengendap; lengkap dengan tren, produk &amp; member penerima
 * promo terbesar, serta dampak tiap aturan promo. Semua grafik HTML/CSS ({@link DashboardUiKit}).</p>
 *
 * <p><b>Pembatasan toko:</b> pedagang yang tidak boleh melihat toko lain hanya melihat data tokonya.</p>
 */
public class MonitorDiskonKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;
    private Div content;
    private Combobox cboPeriode;
    private Combobox cboToko;
    private Combobox cboJenis;
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

        DashboardUiKit.attachIntro(comp, "Monitor Promo & Cashback",
                "Pantau pengeluaran promo: berapa diskon & cashback yang diberikan, yang sudah dicairkan, "
                        + "saldo cashback yang masih mengendap, serta produk, member, dan aturan promo paling berdampak.");

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
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/MonitorDiskonKantinAction.java:74");
        }
        // 2026-07-26: saat dasbor ini dimuat sbg tab "Monitor Promo" dari Dasbor Kantin (lewat
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
            } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/MonitorDiskonKantinAction.java:tokoIdParam");
            }
        }
    }

    // ======================== Filter ========================

    private void buildFilter() {
        host.getChildren().clear();

        Div card = new Div();
        card.setSclass("ais-crud-filter-card");
        card.setParent(host);
        Div body = new Div();
        body.setSclass("ais-crud-filter-body");
        body.setParent(card);
        body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Pilih periode, toko, dan jenis anggota untuk menyaring angka promo di bawah.")));

        Div ctr = new Div();
        ctr.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        ctr.setParent(body);

        EventListener reload = new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                build();
            }
        };

        cboPeriode = new Combobox();
        cboPeriode.setWidth("100%");
        cboPeriode.setReadonly(true);
        addPeriode("Hari Ini", "today");
        addPeriode("Minggu Ini", "week");
        addPeriode("Bulan Ini", "month");
        addPeriode("Semester Ini (6 Bln)", "semester");
        addPeriode("Tahun Ini", "year");
        addPeriode("3 Tahun Terakhir", "3years");
        cboPeriode.setSelectedIndex(2); // default Bulan Ini
        cboPeriode.addEventListener("onSelect", reload);
        ctr.appendChild(kolom("Periode", cboPeriode, "1 1 160px"));

        if (scopeToko == null) {
            cboToko = new Combobox();
            cboToko.setWidth("100%");
            cboToko.setReadonly(true);
            Common.insertComboDanSemua(cboToko, "nama", Toko.class, Restrictions.eq("aktif", true));
            cboToko.addEventListener("onSelect", reload);
            ctr.appendChild(kolom("Toko / Kios", cboToko, "1 1 160px"));
        }

        cboJenis = new Combobox();
        cboJenis.setWidth("100%");
        cboJenis.setReadonly(true);
        Common.insertComboDanSemua(cboJenis, "nama", ais.database.model.koperasi.JenisAnggotaKoperasi.class,
                Restrictions.eq("aktif", true));
        cboJenis.addEventListener("onSelect", reload);
        ctr.appendChild(kolom("Jenis Anggota", cboJenis, "1 1 160px"));

        content = new Div();
        content.setSclass("ais-crud-data-card");
        content.setParent(host);
    }

    private void addPeriode(String label, String code) {
        org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem(label);
        ci.setValue(code);
        ci.setParent(cboPeriode);
    }

    // ======================== Build dashboard ========================

    private void build() {
        if (content == null) {
            return;
        }
        content.getChildren().clear();
        StringBuilder sb = new StringBuilder();

        final Long tokoId = tokoFilter();
        final Long jenisId = jenisFilter();
        final String condTokoPak = tokoId == null ? "" : " AND a.toko = " + tokoId + " ";
        final String condTokoDet = tokoId == null ? "" : " AND det.toko = " + tokoId + " ";
        // Filter jenis anggota lewat sub-join ke anggota_koperasi.
        final String joinJenisPak = jenisId == null ? ""
                : " INNER JOIN koperasi.anggota_koperasi c ON a.anggota_koperasi = c.id ";
        final String condJenisPak = jenisId == null ? "" : " AND c.jenis_anggota_koperasi = " + jenisId + " ";
        final String joinJenisDet = jenisId == null ? ""
                : " INNER JOIN koperasi.pembelian_anggota_koperasi pak ON det.pembelian_anggota_koperasi = pak.id "
                        + " INNER JOIN koperasi.anggota_koperasi c ON pak.anggota_koperasi = c.id ";
        final String condJenisDet = jenisId == null ? "" : " AND c.jenis_anggota_koperasi = " + jenisId + " ";

        // ---- KPI ----
        double diskon = scalar("SELECT COALESCE(SUM(a.total_diskon),0) FROM koperasi.pembelian_anggota_koperasi a "
                + joinJenisPak + " WHERE 1=1 " + condTokoPak + timeCond("a.tanggal_pembayaran") + condJenisPak);
        double cashbackIn = scalar("SELECT COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a "
                + joinJenisPak + " WHERE 1=1 " + condTokoPak + timeCond("a.tanggal_pembayaran") + condJenisPak);
        double cair = scalar("SELECT COALESCE(SUM(a.nominal_cair),0) FROM koperasi.pencairan_diskon a "
                + joinJenisPak + " WHERE a.status IN ('BERHASIL','PENDING') " + condTokoPak
                + timeCond("a.waktu_pencairan") + condJenisPak);
        double mengendap = scalar("SELECT (COALESCE((SELECT SUM(pa.totalcashback) "
                + "FROM koperasi.pembelian_anggota_koperasi pa "
                + (jenisId == null ? "" : "INNER JOIN koperasi.anggota_koperasi c ON pa.anggota_koperasi = c.id ")
                + "WHERE 1=1 " + condJenisPak.replace("c.", "c.") + "),0) - COALESCE((SELECT SUM(d.nominal_cair) "
                + "FROM koperasi.pencairan_diskon d "
                + (jenisId == null ? "" : "INNER JOIN koperasi.anggota_koperasi c ON d.anggota_koperasi = c.id ")
                + "WHERE d.status IN ('BERHASIL','PENDING') " + condJenisPak + "),0))");

        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Diskon Diberikan", "Rp " + DashboardUiKit.money(diskon),
                "potongan langsung di struk", DashboardUiKit.BAD));
        kartu.add(new DashboardUiKit.Stat("Cashback Diberikan", "Rp " + DashboardUiKit.money(cashbackIn),
                "masuk saldo member", DashboardUiKit.GOOD));
        kartu.add(new DashboardUiKit.Stat("Cashback Dicairkan", "Rp " + DashboardUiKit.money(cair),
                "sudah ditarik member", DashboardUiKit.ACCENT));
        kartu.add(new DashboardUiKit.Stat("Saldo Cashback Mengendap", "Rp " + DashboardUiKit.money(mengendap),
                "belum dicairkan (akumulatif)", DashboardUiKit.PRIMARY));
        sb.append(DashboardUiKit.descChip("Ringkasan biaya promo pada periode terpilih."));
        sb.append(DashboardUiKit.cards(kartu));

        // ---- Tren diskon vs cashback ----
        String fmt = isBulanan() ? "YYYY-MM" : "YYYY-MM-DD";
        LinkedHashMap<String, Double> trDiskon = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, Double> trCashback = new LinkedHashMap<String, Double>();
        for (Object[] r : rows("SELECT TO_CHAR(a.tanggal_pembayaran,'" + fmt + "') t, COALESCE(SUM(a.total_diskon),0), "
                + "COALESCE(SUM(a.totalcashback),0) FROM koperasi.pembelian_anggota_koperasi a " + joinJenisPak
                + " WHERE (a.total_diskon > 0 OR a.totalcashback > 0) " + condTokoPak
                + timeCond("a.tanggal_pembayaran") + condJenisPak
                + " GROUP BY TO_CHAR(a.tanggal_pembayaran,'" + fmt + "') ORDER BY t ASC")) {
            trDiskon.put(str(r[0]), num(r[1]));
            trCashback.put(str(r[0]), num(r[2]));
        }
        sb.append(DashboardUiKit.groupedBar("Tren Pemberian Promo", "Perbandingan diskon dan cashback dari waktu ke waktu.",
                trDiskon, trCashback, "Diskon", "Cashback", DashboardUiKit.BAD, DashboardUiKit.GOOD, true));

        // ---- Top produk promo (donut) ----
        LinkedHashMap<String, Double> topProduk = new LinkedHashMap<String, Double>();
        for (Object[] r : rows("SELECT p.nama, COALESCE(SUM(det.diskon),0)+COALESCE(SUM(det.cashback),0) tot "
                + "FROM koperasi.pembelian det INNER JOIN koperasi.produk p ON det.produk = p.id " + joinJenisDet
                + " WHERE (det.diskon > 0 OR det.cashback > 0) " + condTokoDet + timeCond("det.waktu") + condJenisDet
                + " GROUP BY p.nama ORDER BY tot DESC LIMIT 5")) {
            topProduk.put(str(r[0]), num(r[1]));
        }
        sb.append(DashboardUiKit.donut("Top 5 Produk Promo", "Produk dengan total diskon + cashback terbesar.",
                topProduk, true, "Belum ada produk yang kena promo."));

        // ---- Top member penerima (barList) ----
        LinkedHashMap<String, Double> topMember = new LinkedHashMap<String, Double>();
        for (Object[] r : rows("SELECT c.nama, COALESCE(SUM(a.totalcashback),0) cb "
                + "FROM koperasi.pembelian_anggota_koperasi a INNER JOIN koperasi.anggota_koperasi c "
                + "ON a.anggota_koperasi = c.id WHERE (a.total_diskon > 0 OR a.totalcashback > 0) " + condTokoPak
                + timeCond("a.tanggal_pembayaran") + (jenisId == null ? "" : " AND c.jenis_anggota_koperasi = " + jenisId)
                + " GROUP BY c.nama ORDER BY cb DESC LIMIT 8")) {
            topMember.put(str(r[0]), num(r[1]));
        }
        sb.append(DashboardUiKit.barList("Member Penerima Cashback Terbesar",
                "Member yang paling banyak mendapat cashback.", topMember, DashboardUiKit.GOOD, "", true,
                "Belum ada member yang menerima cashback."));

        // ---- Dampak aturan diskon (tabel) ----
        sb.append(tabelAturan(condTokoDet, joinJenisDet, condJenisDet));

        // ---- Data Saldo Member Reguler Special (MRS) ----
        sb.append(tabelSaldoMrs());

        content.appendChild(DashboardUiKit.html(sb.toString()));
    }

    /**
     * Tabel saldo Member Reguler Special (MRS): Total Saldo Awal (dari top-up), Total Terpakai
     * (belanja/expenses), dan Sisa Saldo (= awal − terpakai). Member MRS dikenali dari jenis
     * anggota yang namanya memuat "reguler ... spe(sial/cial)" atau kode identitas berawalan "MRS".
     */
    private String tabelSaldoMrs() {
        StringBuilder html = new StringBuilder();
        html.append("<div style='background:#fff;border:1px solid ").append(DashboardUiKit.LINE)
                .append(";border-radius:14px;padding:14px;margin-top:14px;'>");
        html.append("<div style='font-weight:800;color:#0f172a;margin-bottom:2px;'>Data Saldo Member Reguler Special (MRS)</div>");
        html.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>")
                .append("Untuk tiap member Reguler Special: total saldo dari top-up, yang sudah dipakai belanja, dan sisa saldonya.</div>");
        html.append("<div style='overflow-x:auto;'><table style='width:100%;min-width:520px;border-collapse:collapse;font-size:12.5px;'>");
        html.append("<thead><tr style='text-align:left;color:#475569;border-bottom:2px solid ").append(DashboardUiKit.LINE)
                .append(";'><th style='padding:6px;'>Nama Member</th>")
                .append("<th style='padding:6px;text-align:right;'>Total Saldo Awal (Top-Up)</th>")
                .append("<th style='padding:6px;text-align:right;'>Total Terpakai (Belanja)</th>")
                .append("<th style='padding:6px;text-align:right;'>Sisa Saldo</th></tr></thead><tbody>");
        List<Object[]> data = rows("SELECT ak.kode_identitas, ak.nama, "
                + "COALESCE((SELECT SUM(dp.nominal) FROM public.deposit dp "
                + "WHERE dp.anggota_koperasi = ak.id),0) AS saldo_awal, "
                + "COALESCE((SELECT SUM(pb.total) FROM koperasi.pembelian pb "
                + "INNER JOIN koperasi.cara_pembayaran_koperasi cpk ON pb.cara_pembayaran_koperasi = cpk.id "
                + "WHERE pb.anggota_koperasi = ak.id AND cpk.manual = false),0) AS terpakai "
                + "FROM koperasi.anggota_koperasi ak "
                + "INNER JOIN koperasi.jenis_anggota_koperasi jak ON ak.jenis_anggota_koperasi = jak.id "
                + "WHERE (ak.aktif = true OR ak.aktif IS NULL) "
                + "AND (LOWER(jak.nama) LIKE '%reguler%spe%' OR ak.kode_identitas ILIKE 'MRS%') "
                + "ORDER BY saldo_awal DESC LIMIT 300");
        if (data.isEmpty()) {
            html.append("<tr><td colspan='4' style='padding:12px;color:#64748b;'>Belum ada member Reguler Special (MRS) atau belum ada top-up.</td></tr>");
        } else {
            double tA = 0, tT = 0, tS = 0;
            for (Object[] r : data) {
                String kode = str(r[0]);
                String nm = str(r[1]);
                String nama = (kode.isEmpty() ? "" : kode + " — ") + nm;
                double awal = num(r[2]);
                double terp = num(r[3]);
                double sisa = awal - terp;
                tA += awal;
                tT += terp;
                tS += sisa;
                html.append("<tr style='border-bottom:1px solid ").append(DashboardUiKit.LINE).append(";'>")
                        .append("<td style='padding:6px;font-weight:600;'>").append(DashboardUiKit.esc(nama)).append("</td>")
                        .append("<td style='padding:6px;text-align:right;color:#2563eb;font-weight:700;'>Rp ")
                        .append(DashboardUiKit.money(awal)).append("</td>")
                        .append("<td style='padding:6px;text-align:right;color:#dc2626;'>Rp ")
                        .append(DashboardUiKit.money(terp)).append("</td>")
                        .append("<td style='padding:6px;text-align:right;font-weight:800;color:#16a34a;'>Rp ")
                        .append(DashboardUiKit.money(sisa)).append("</td></tr>");
            }
            html.append("<tr style='border-top:2px solid ").append(DashboardUiKit.LINE).append(";font-weight:800;'>")
                    .append("<td style='padding:6px;'>TOTAL</td>")
                    .append("<td style='padding:6px;text-align:right;'>Rp ").append(DashboardUiKit.money(tA)).append("</td>")
                    .append("<td style='padding:6px;text-align:right;'>Rp ").append(DashboardUiKit.money(tT)).append("</td>")
                    .append("<td style='padding:6px;text-align:right;color:#16a34a;'>Rp ").append(DashboardUiKit.money(tS))
                    .append("</td></tr>");
        }
        html.append("</tbody></table></div></div>");
        return html.toString();
    }

    private String tabelAturan(String condTokoDet, String joinJenisDet, String condJenisDet) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='background:#fff;border:1px solid ").append(DashboardUiKit.LINE)
                .append(";border-radius:14px;padding:14px;margin-top:14px;'>");
        html.append("<div style='font-weight:800;color:#0f172a;margin-bottom:2px;'>Dampak Aturan Promo</div>");
        html.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>")
                .append("Seberapa sering tiap aturan promo dipakai dan total biayanya.</div>");
        html.append("<table style='width:100%;border-collapse:collapse;font-size:12.5px;'>");
        html.append("<thead><tr style='text-align:left;color:#475569;border-bottom:2px solid ")
                .append(DashboardUiKit.LINE).append(";'>")
                .append("<th style='padding:6px;'>Aturan</th><th style='padding:6px;'>Tipe</th>")
                .append("<th style='padding:6px;text-align:center;'>Dipakai</th>")
                .append("<th style='padding:6px;text-align:right;'>Total Biaya</th></tr></thead><tbody>");
        List<Object[]> data = rows("SELECT ad.nama_aturan, ad.potongan_langsung, COUNT(det.id), "
                + "COALESCE(SUM(det.diskon + det.cashback),0) FROM koperasi.pembelian det "
                + "INNER JOIN koperasi.aturan_diskon ad ON det.aturan_diskon = ad.id " + joinJenisDet
                + " WHERE 1=1 " + condTokoDet + timeCond("det.waktu") + condJenisDet
                + " GROUP BY ad.id, ad.nama_aturan, ad.potongan_langsung ORDER BY 4 DESC LIMIT 50");
        if (data.isEmpty()) {
            html.append("<tr><td colspan='4' style='padding:12px;color:#64748b;'>Belum ada promo yang terpakai pada periode ini.</td></tr>");
        } else {
            for (Object[] r : data) {
                boolean potong = bool(r[1]);
                html.append("<tr style='border-bottom:1px solid ").append(DashboardUiKit.LINE).append(";'>");
                html.append("<td style='padding:6px;font-weight:600;'>").append(DashboardUiKit.esc(str(r[0]))).append("</td>");
                html.append("<td style='padding:6px;'><span style='padding:1px 8px;border-radius:10px;font-size:11px;font-weight:700;background:")
                        .append(potong ? "#fee2e2;color:#dc2626;" : "#dcfce7;color:#16a34a;").append("'>")
                        .append(potong ? "Potong Struk" : "Cashback").append("</span></td>");
                html.append("<td style='padding:6px;text-align:center;font-weight:700;'>").append((long) num(r[2])).append(" x</td>");
                html.append("<td style='padding:6px;text-align:right;font-weight:700;color:#0f172a;'>Rp ")
                        .append(DashboardUiKit.money(num(r[3]))).append("</td></tr>");
            }
        }
        html.append("</tbody></table></div>");
        return html.toString();
    }

    // ======================== Filter helpers ========================

    private String periodeCode() {
        if (cboPeriode != null && cboPeriode.getSelectedItem() != null
                && cboPeriode.getSelectedItem().getValue() != null) {
            return cboPeriode.getSelectedItem().getValue().toString();
        }
        return "month";
    }

    private boolean isBulanan() {
        String p = periodeCode();
        return "year".equals(p) || "semester".equals(p) || "3years".equals(p);
    }

    private String timeCond(String col) {
        String p = periodeCode();
        if ("today".equals(p)) {
            return " AND " + col + " >= current_date ";
        }
        if ("week".equals(p)) {
            return " AND " + col + " >= current_date - interval '7 days' ";
        }
        if ("month".equals(p)) {
            return " AND date_trunc('month'," + col + ") = date_trunc('month',current_date) ";
        }
        if ("semester".equals(p)) {
            return " AND " + col + " >= current_date - interval '6 months' ";
        }
        if ("year".equals(p)) {
            return " AND date_trunc('year'," + col + ") = date_trunc('year',current_date) ";
        }
        if ("3years".equals(p)) {
            return " AND " + col + " >= current_date - interval '3 years' ";
        }
        return "";
    }

    private Long tokoFilter() {
        if (scopeToko != null) {
            return scopeToko.getId();
        }
        if (cboToko != null && cboToko.getSelectedItem() != null
                && cboToko.getSelectedItem().getValue() instanceof Toko) {
            return ((Toko) cboToko.getSelectedItem().getValue()).getId();
        }
        return null;
    }

    private Long jenisFilter() {
        if (cboJenis != null && cboJenis.getSelectedItem() != null
                && cboJenis.getSelectedItem().getValue() instanceof ais.database.model.koperasi.JenisAnggotaKoperasi) {
            return ((ais.database.model.koperasi.JenisAnggotaKoperasi) cboJenis.getSelectedItem().getValue()).getId();
        }
        return null;
    }

    // ======================== Util ========================

    private Div kolom(String label, Component field, String flex) {
        Div d = new Div();
        d.setStyle("display:flex;flex-direction:column;flex:" + flex + ";");
        org.zkoss.zul.Label l = new org.zkoss.zul.Label(label);
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;display:block;margin-bottom:3px;");
        d.appendChild(l);
        d.appendChild(field);
        return d;
    }

    private double scalar(String sql) {
        try {
            Object o = HibernateUtil.currentSession().createSQLQuery(sql).uniqueResult();
            return o == null ? 0.0 : ((Number) o).doubleValue();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            return 0.0;
        }
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

    private static boolean bool(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        String s = o.toString().trim();
        return s.equals("t") || s.equalsIgnoreCase("true") || s.equals("1");
    }
}
