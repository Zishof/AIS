package ais.action.master.koperasi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.SQLQuery;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.ui.util.DashboardUiKit;

/**
 * Notifikasi Member — konversi dari {@code modul/kantin/member/notifikasi}.
 *
 * <p>Daftar notifikasi milik pengguna yang sedang login (tabel {@code public.notifikasi},
 * dicocokkan lewat {@code nama = userId}). Menampilkan ringkasan (total &amp; belum dibaca) dan
 * tabel waktu/isi/status/status-baca.</p>
 */
public class NotifikasiMemberKantinAction extends GenericAutowireComposer {

    private static final long serialVersionUID = 1L;

    private Div host;
    private String userId;

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
            userId = u == null ? null : u.getUserId();
        } catch (Exception ignore) { ais.common.ErrorAuditUtil.record(ignore, "auto-audit(empty-catch) src/ais/action/master/koperasi/NotifikasiMemberKantinAction.java:50");
        }

        DashboardUiKit.attachIntro(comp, "Notifikasi Saya",
                "Pemberitahuan untuk akun Anda: konfirmasi transaksi, top-up, dan informasi lainnya.");

        if (host == null) {
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='padding:24px;text-align:center;color:#64748b;'>Halaman ini khusus untuk pengguna yang login.</div>"));
            return;
        }
        build();
    }

    public void onRefresh(Event event) throws Exception {
        build();
    }

    private void build() {
        host.getChildren().clear();
        String uid = userId.replace("'", "''");
        List<Object[]> data = rows("SELECT TO_CHAR(n.waktu,'dd-MM-yyyy HH24:MI'), COALESCE(n.keterangan,''), "
                + "COALESCE(n.hasil,''), n.buka FROM public.notifikasi n WHERE n.nama = '" + uid + "' "
                + "ORDER BY n.waktu DESC LIMIT 200");

        int belum = 0;
        for (Object[] r : data) {
            if (!bool(r[3])) {
                belum++;
            }
        }
        List<DashboardUiKit.Stat> kartu = new ArrayList<DashboardUiKit.Stat>();
        kartu.add(new DashboardUiKit.Stat("Total Notifikasi", DashboardUiKit.money(data.size()),
                "pesan", DashboardUiKit.PRIMARY));
        kartu.add(new DashboardUiKit.Stat("Belum Dibaca", DashboardUiKit.money(belum),
                "perlu diperhatikan", belum > 0 ? DashboardUiKit.WARN : DashboardUiKit.GOOD));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.descChip(
                "Daftar pemberitahuan untuk akun Anda, terbaru di atas.")));
        host.appendChild(DashboardUiKit.html(DashboardUiKit.cards(kartu)));

        if (data.isEmpty()) {
            host.appendChild(DashboardUiKit.html(
                    "<div style='font-size:12px;color:#64748b;padding:14px;'>Belum ada notifikasi.</div>"));
            return;
        }

        Grid grid = new Grid();
        grid.setSclass("dgrid");
        grid.setWidth("100%");
        grid.setStyle("border:0;background:transparent;");
        if (data.size() > 25) {
            grid.setMold("paging");
            grid.setPageSize(25);
        }
        Columns columns = new Columns();
        columns.setParent(grid);
        columns.setSizable(true);
        addCol(columns, "No.", "48px");
        addCol(columns, "Waktu", "150px");
        addCol(columns, "Isi Notifikasi", null);
        addCol(columns, "Status", "110px");
        addCol(columns, "Dibaca", "80px");

        Rows rows = new Rows();
        rows.setParent(grid);
        int no = 1;
        for (Object[] r : data) {
            boolean dibaca = bool(r[3]);
            Row row = new Row();
            row.setValign("top");
            row.setParent(rows);
            new Label(String.valueOf(no++)).setParent(row);
            new Label(str(r[0])).setParent(row);
            Label isi = new Label(str(r[1]));
            if (!dibaca) {
                isi.setStyle("font-weight:700;");
            }
            isi.setParent(row);
            String hasil = str(r[2]);
            Label st = new Label(hasil.isEmpty() ? "-" : hasil);
            st.setStyle("font-weight:700;color:" + warnaStatus(hasil) + ";");
            st.setParent(row);
            Label db = new Label(dibaca ? "Ya" : "Belum");
            db.setStyle("color:" + (dibaca ? "#16a34a" : "#f97316") + ";font-weight:700;");
            db.setParent(row);
        }
        grid.setParent(host);
    }

    private static String warnaStatus(String hasil) {
        String h = hasil == null ? "" : hasil.toUpperCase();
        if (h.contains("BERHASIL") || h.contains("SUKSES") || h.contains("OK")) {
            return "#16a34a";
        }
        if (h.contains("GAGAL") || h.contains("ERROR")) {
            return "#dc2626";
        }
        return "#64748b";
    }

    // ======================== Util ========================

    private void addCol(Columns columns, String label, String width) {
        Column c = new Column();
        c.setLabel(label);
        if (width != null) {
            c.setWidth(width);
        }
        c.setParent(columns);
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

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }
}
