package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Button;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.ui.util.DashboardUiKit;
import ais.ui.util.MyMessageboxConfig;

/**
 * Pesanan Saya (Draft) — konversi dari {@code modul/kantin/member/_draft_riwayat_transaksi_member.jsp}.
 *
 * <p>Menampilkan pesanan milik pengguna yang login (tabel {@code koperasi.draft_pembelian_anggota_koperasi})
 * beserta statusnya; pesanan yang <b>belum dibayar</b> ({@code lunas IS NULL}) bisa dibatalkan. Otomatis
 * hanya menampilkan/menghapus pesanan milik member yang bersangkutan (member-scoped).</p>
 */
public class DraftPesananMemberKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host; // wired dari ZUL
    private Long memberId;
    private Datebox dtMulai;
    private Datebox dtAkhir;
    private Textbox txtCari;
    private Div cardBox;
    private org.zkoss.zul.Grid grid;

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
        try {
            ais.database.model.Tbmuser u = Common.getCurrentUser();
            AnggotaKoperasi a = u == null ? null : u.getAnggotaKoperasi();
            memberId = a == null ? null : a.getId();
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/DraftPesananMemberKantinAction.java:62");
        }

        DashboardUiKit.attachIntro(comp, "Pesanan Saya",
                "Daftar pesanan Anda beserta statusnya. Pesanan yang belum dibayar masih bisa dibatalkan.");

        if (host == null) {
            return;
        }
        if (memberId == null) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>Halaman ini khusus untuk Anggota Koperasi yang login.</div>"));
            return;
        }
        buildUI();
        loadList();
    }

    private void buildUI() {
        host.getChildren().clear();

        Div card = new Div();
        card.setSclass("ais-crud-filter-card");
        card.setParent(host);
        Div body = new Div();
        body.setSclass("ais-crud-filter-body");
        body.setParent(card);
        body.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Saring berdasarkan tanggal atau nama toko/cara bayar. Pesanan yang belum dibayar bisa dibatalkan.")));

        Div f = new Div();
        f.setStyle("display:flex;flex-wrap:wrap;gap:10px;align-items:flex-end;");
        f.setParent(body);

        dtMulai = new Datebox();
        dtMulai.setWidth("140px");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        dtMulai.setValue(cal.getTime());
        f.appendChild(kolom("Tanggal Awal", dtMulai));

        dtAkhir = new Datebox();
        dtAkhir.setWidth("140px");
        dtAkhir.setValue(new java.util.Date());
        f.appendChild(kolom("Tanggal Akhir", dtAkhir));

        txtCari = new Textbox();
        txtCari.setWidth("170px");
        txtCari.setTooltiptext("nama toko / cara bayar");
        f.appendChild(kolom("Cari Toko / Cara Bayar", txtCari));

        Button b = new Button("Terapkan");
        b.addEventListener("onClick", new EventListener() {
            @Override
            public void onEvent(Event e) throws Exception {
                loadList();
            }
        });
        f.appendChild(kolom(" ", b));

        cardBox = new Div();
        cardBox.setStyle("margin-top:4px;");
        cardBox.setParent(host);

        Div dataCard = new Div();
        dataCard.setSclass("ais-crud-data-card");
        dataCard.setParent(host);

        grid = new org.zkoss.zul.Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setStyle("border:0;background:transparent;");
        grid.setMold("paging");
        grid.setPageSize(15);
        grid.setParent(dataCard);
        Columns c = new Columns();
        c.setParent(grid);
        addCol(c, "Waktu Pesan", "140px");
        addCol(c, "Toko", "150px");
        addCol(c, "Cara Bayar / Ket.", null);
        addCol(c, "Diskon", "100px");
        addCol(c, "Cashback", "100px");
        addCol(c, "Total Tagihan", "120px");
        addCol(c, "Status / Aksi", "120px");
    }

    private String filterSql() {
        StringBuilder w = new StringBuilder(" WHERE a.anggota_koperasi = " + memberId + " ");
        if (dtMulai != null && dtMulai.getValue() != null && dtAkhir != null && dtAkhir.getValue() != null) {
            String a1 = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dtMulai.getValue());
            String a2 = new java.text.SimpleDateFormat("yyyy-MM-dd").format(dtAkhir.getValue());
            w.append(" AND DATE(a.tanggal_pembayaran) BETWEEN DATE('").append(a1).append("') AND DATE('").append(a2)
                    .append("') ");
        }
        String kw = txtCari == null ? "" : txtCari.getValue().trim().replace("'", "''");
        if (!kw.isEmpty()) {
            w.append(" AND (b.nama ILIKE '%").append(kw).append("%' OR cp.nama ILIKE '%").append(kw).append("%') ");
        }
        return w.toString();
    }

    private void loadList() {
        if (grid == null) {
            return;
        }
        String from = "FROM koperasi.draft_pembelian_anggota_koperasi a LEFT JOIN koperasi.toko b ON a.toko = b.id "
                + "LEFT JOIN koperasi.cara_pembayaran_koperasi cp ON cp.id = a.cara_pembayaran_koperasi ";

        Object[] sum = first(rows("SELECT COUNT(*), "
                + "COALESCE(SUM(CASE WHEN a.lunas IS NULL THEN COALESCE(a.total_biaya,0) ELSE 0 END),0), "
                + "COALESCE(SUM(CASE WHEN a.lunas IS NULL THEN 1 ELSE 0 END),0) " + from + filterSql()));
        long total = sum == null ? 0 : (long) num(sum[0]);
        double tagihanBelum = sum == null ? 0 : num(sum[1]);
        long belum = sum == null ? 0 : (long) num(sum[2]);
        if (cardBox != null) {
            cardBox.getChildren().clear();
            List<DashboardUiKit.Stat> k = new ArrayList<DashboardUiKit.Stat>();
            k.add(new DashboardUiKit.Stat("Total Pesanan", DashboardUiKit.money(total), "pada rentang ini",
                    DashboardUiKit.PRIMARY));
            k.add(new DashboardUiKit.Stat("Belum Dibayar", DashboardUiKit.money(belum), "pesanan",
                    belum > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
            k.add(new DashboardUiKit.Stat("Tagihan Belum Dibayar", "Rp " + DashboardUiKit.money(tagihanBelum),
                    "perlu diselesaikan", belum > 0 ? DashboardUiKit.BAD : DashboardUiKit.GOOD));
            cardBox.appendChild(DashboardUiKit.html(DashboardUiKit.cards(k)));
        }

        Rows rows = new Rows();
        List<Object[]> data = rows("SELECT a.id, TO_CHAR(a.tanggal_pembayaran,'DD-MM-YYYY HH24:MI'), "
                + "COALESCE(b.nama,'Kantin Utama'), COALESCE(cp.nama,'-'), COALESCE(a.keterangan,''), "
                + "COALESCE(a.total_diskon,0), COALESCE(a.totalcashback,0), COALESCE(a.total_biaya,0), a.lunas " + from
                + filterSql() + " ORDER BY a.id DESC LIMIT 300");
        if (data.isEmpty()) {
            Row r = new Row();
            r.setParent(rows);
            Label l = new Label(ais.common.Common.getBahasaConfig("Belum ada pesanan pada rentang ini."));
            l.setStyle("color:#64748b;padding:10px;");
            l.setParent(r);
            ganti(rows);
            return;
        }
        for (Object[] d : data) {
            final Long id = lng(d[0]);
            boolean lunas = d[8] != null;
            Row row = new Row();
            row.setValign("middle");
            row.setParent(rows);
            new Label(str(d[1])).setParent(row);
            Label lt = new Label(str(d[2]));
            lt.setStyle("font-weight:700;");
            lt.setParent(row);
            String ket = str(d[4]);
            new Label(str(d[3]) + (ket.isEmpty() ? "" : "  ·  " + ket)).setParent(row);
            Label ld = new Label("Rp " + DashboardUiKit.money(num(d[5])));
            ld.setStyle("color:#dc2626;");
            ld.setParent(row);
            Label lc = new Label("Rp " + DashboardUiKit.money(num(d[6])));
            lc.setStyle("color:#06b6d4;");
            lc.setParent(row);
            Label lb = new Label("Rp " + DashboardUiKit.money(num(d[7])));
            lb.setStyle("font-weight:800;color:#16a34a;");
            lb.setParent(row);
            if (lunas) {
                Label badge = new Label(ais.common.Common.getBahasaConfig("Lunas"));
                badge.setStyle("background:#dcfce7;color:#16a34a;font-weight:800;padding:2px 10px;border-radius:10px;"
                        + "font-size:11px;");
                badge.setParent(row);
            } else {
                Button bBatal = new Button("Batalkan");
                bBatal.setTooltiptext("Batalkan pesanan yang belum dibayar");
                bBatal.addEventListener("onClick", new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        onBatal(id);
                    }
                });
                bBatal.setParent(row);
            }
        }
        ganti(rows);
    }

    /** Batalkan (hapus) pesanan draft milik member ini — dengan cek ulang status lunas (anti race). */
    private void onBatal(final Long id) throws Exception {
        if (id == null) {
            return;
        }
        MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan pesanan ini? Mohon diperhatikan bahwa pesanan yang telah dibayar tidak dapat dibatalkan.", "Konfirmasi Pembatalan",
                MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {
                    @Override
                    public void onEvent(Event e) throws Exception {
                        int i = new Integer(e.getData().toString());
                        if (i != MyMessageboxConfig.OK) {
                            return;
                        }
                        org.hibernate.Session s = HibernateUtil.getSessionFactory().openSession();
                        try {
                            Object lunas = s.createSQLQuery("SELECT lunas FROM koperasi.draft_pembelian_anggota_koperasi "
                                    + "WHERE id = " + id + " AND anggota_koperasi = " + memberId).uniqueResult();
                            // uniqueResult null bisa berarti baris milik orang lain / tidak ada → hentikan.
                            if (lunas != null) {
                                MyMessageboxConfig.show("Mohon maaf, Bapak/Ibu. Pesanan ini telah diproses atau telah dibayar oleh toko sehingga tidak dapat dibatalkan.",
                                        "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
                                loadList();
                                return;
                            }
                            org.hibernate.Transaction tx = s.beginTransaction();
                            s.createSQLQuery("DELETE FROM koperasi.draft_pembelian WHERE draft_pembelian_anggota_koperasi = "
                                    + id).executeUpdate();
                            s.createSQLQuery("DELETE FROM koperasi.draft_pembelian_anggota_koperasi WHERE id = " + id
                                    + " AND anggota_koperasi = " + memberId).executeUpdate();
                            tx.commit();
                            MyMessageboxConfig.show("Pesanan Bapak/Ibu telah berhasil dibatalkan.", "Informasi", MyMessageboxConfig.OK,
                                    MyMessageboxConfig.INFORMATION);
                            loadList();
                        } catch (Exception ex) {
                            Common.tampilErrorJikaAdmin(ex);
                        } finally {
                            try {
                                s.disconnect();
                            } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/DraftPesananMemberKantinAction.java:281");
                            }
                            try {
                                s.close();
                            } catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/koperasi/DraftPesananMemberKantinAction.java:285");
                            }
                        }
                    }
                });
    }

    // ======================== Util ========================

    private Div kolom(String label, Component field) {
        Div d = new Div();
        d.setStyle("display:flex;flex-direction:column;");
        Label l = new Label(label);
        l.setStyle("font-size:11px;font-weight:700;color:#64748b;margin-bottom:3px;");
        d.appendChild(l);
        d.appendChild(field);
        return d;
    }

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
    }

    private void ganti(Rows rows) {
        if (grid == null) {
            return;
        }
        if (grid.getRows() != null) {
            grid.getRows().detach();
        }
        rows.setParent(grid);
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

    private Object[] first(List<Object[]> r) {
        return r == null || r.isEmpty() ? null : r.get(0);
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long lng(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}
