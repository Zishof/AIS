package ais.action.master.akunting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.SimpleListModel;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.ReimbursementPegawai;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;

/** Dashboard operasional reimbursement untuk finance dan administrator. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ReimbursementDashboardAction extends GenericAutowireComposer {
    private static final long serialVersionUID = 1L;
    private MyDatebox start;
    private MyDatebox end;
    private Combobox status;
    private Label kpiTotal;
    private Label kpiNominal;
    private Label kpiMenunggu;
    private Label kpiLunas;
    private Label kpiSla;
    private Label lastRefresh;
    private Grid gridStatus;
    private Grid gridKategori;
    private Grid gridAging;
    private Grid gridBulanan;
    private Grid gridAntrean;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        if (!isFinance()) {
            // Jangan blokir dengan popup (mengganggu pegawai yang hanya ingin tab "Reimbursement" utk
            // mengajukan). Cukup sembunyikan tab Dashboard, lalu arahkan ke tab "Reimbursement".
            sembunyikanTabDanPilihReimbursement(comp);
            comp.setVisible(false);
            return;
        }
        initFilter();
        gridStatus.setRowRenderer(new MetricRenderer());
        gridKategori.setRowRenderer(new MetricRenderer());
        gridAging.setRowRenderer(new MetricRenderer());
        gridBulanan.setRowRenderer(new MetricRenderer());
        gridAntrean.setRowRenderer(new QueueRenderer());
        load();
    }

    private void initFilter() {
        Calendar cal = ais.ui.util.WaktuUtil.getCalendar();
        end.setValue(cal.getTime());
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        start.setValue(cal.getTime());
        String[] statuses = { "Semua", ReimbursementPegawai.DIAJUKAN, ReimbursementPegawai.REVISI,
                ReimbursementPegawai.DITOLAK, ReimbursementPegawai.DISETUJUI, ReimbursementPegawai.LUNAS };
        for (int i = 0; i < statuses.length; i++) {
            Comboitem item = status.appendItem(statuses[i]);
            item.setValue(i == 0 ? "" : statuses[i]);
        }
        status.setSelectedIndex(0);
    }

    public void onRefresh(Event event) { load(); }

    private void load() {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            Criteria criteria = session.createCriteria(ReimbursementPegawai.class);
            applyFilter(criteria);
            List data = criteria.addOrder(Order.desc("tanggalPengajuan")).setMaxResults(50000).list();
            for (int i = 0; i < data.size(); i++) {
                ReimbursementPegawai item = (ReimbursementPegawai) data.get(i);
                Hibernate.initialize(item.getPegawai());
                Hibernate.initialize(item.getAtasan());
            }
            calculate(data);
            lastRefresh.setValue("Diperbarui " + Common.dateFormat3.get().format(ais.ui.util.WaktuUtil.getDate()));
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            kpiTotal.setValue("Data gagal dimuat");
        } finally { HibernateUtil.closeSessionQuietly(session); }
    }

    private void applyFilter(Criteria criteria) {
        if (start.getValue() != null) criteria.add(Restrictions.ge("tanggalPengajuan", startOfDay(start.getValue())));
        if (end.getValue() != null) criteria.add(Restrictions.lt("tanggalPengajuan", nextDay(end.getValue())));
        if (status.getSelectedItem() != null) {
            String value = (String) status.getSelectedItem().getValue();
            if (value != null && !value.isEmpty()) criteria.add(Restrictions.eq("status", value));
        }
    }

    private void calculate(List data) {
        Map<String, Metric> byStatus = new LinkedHashMap<String, Metric>();
        String[] order = { ReimbursementPegawai.DIAJUKAN, ReimbursementPegawai.REVISI,
                ReimbursementPegawai.DITOLAK, ReimbursementPegawai.DISETUJUI, ReimbursementPegawai.LUNAS };
        for (int i = 0; i < order.length; i++) byStatus.put(order[i], new Metric(order[i]));
        Map<String, Metric> byCategory = new TreeMap<String, Metric>();
        Map<String, Metric> byMonth = new TreeMap<String, Metric>();
        Map<String, Metric> aging = new LinkedHashMap<String, Metric>();
        aging.put("0–3 hari", new Metric("0–3 hari"));
        aging.put("4–7 hari", new Metric("4–7 hari"));
        aging.put("8–14 hari", new Metric("8–14 hari"));
        aging.put("> 14 hari", new Metric("> 14 hari"));

        double total = 0, paid = 0, cycleDays = 0;
        int waiting = 0, paidCount = 0, slaBreach = 0;
        List queue = new ArrayList();
        Date now = ais.ui.util.WaktuUtil.getDate();
        Calendar month = ais.ui.util.WaktuUtil.getCalendar();
        for (int i = 0; i < data.size(); i++) {
            ReimbursementPegawai d = (ReimbursementPegawai) data.get(i);
            double amount = d.getNominal();
            total += amount;
            Metric s = byStatus.get(d.getStatus());
            if (s == null) { s = new Metric(d.getStatus()); byStatus.put(d.getStatus(), s); }
            s.add(amount);
            String category = empty(d.getKategori()) ? "Tanpa kategori" : d.getKategori();
            Metric c = byCategory.get(category);
            if (c == null) { c = new Metric(category); byCategory.put(category, c); }
            c.add(amount);
            month.setTime(d.getTanggalPengajuan());
            String monthKey = month.get(Calendar.YEAR) + "-" + two(month.get(Calendar.MONTH) + 1);
            Metric m = byMonth.get(monthKey);
            if (m == null) { m = new Metric(monthKey); byMonth.put(monthKey, m); }
            m.add(amount);

            if (ReimbursementPegawai.LUNAS.equals(d.getStatus())) {
                paid += amount; paidCount++;
                if (d.getTanggalPembayaran() != null) cycleDays += days(d.getTanggalPengajuan(), d.getTanggalPembayaran());
            } else if (ReimbursementPegawai.DIAJUKAN.equals(d.getStatus())
                    || ReimbursementPegawai.DISETUJUI.equals(d.getStatus())) {
                waiting++;
                int age = days(d.getTanggalPengajuan(), now);
                String bucket = age <= 3 ? "0–3 hari" : age <= 7 ? "4–7 hari" : age <= 14 ? "8–14 hari" : "> 14 hari";
                aging.get(bucket).add(amount);
                if (age > 7) slaBreach++;
                if (queue.size() < 20) queue.add(new QueueItem(d, age));
            }
        }
        kpiTotal.setValue(String.valueOf(data.size()));
        kpiNominal.setValue("Rp " + Common.numberFormat.get().format(total));
        kpiMenunggu.setValue(waiting + " dokumen");
        kpiLunas.setValue("Rp " + Common.numberFormat.get().format(paid));
        kpiSla.setValue(slaBreach + " lewat 7 hari" + (paidCount > 0 ? " • rata-rata "
                + Common.numberFormat.get().format(cycleDays / paidCount) + " hari" : ""));
        gridStatus.setModel(new SimpleListModel(toList(byStatus, data.size(), total)));
        gridKategori.setModel(new SimpleListModel(toList(byCategory, data.size(), total)));
        gridAging.setModel(new SimpleListModel(toList(aging, waiting, sum(aging))));
        gridBulanan.setModel(new SimpleListModel(toList(byMonth, data.size(), total)));
        gridAntrean.setModel(new SimpleListModel(queue));
    }

    private List toList(Map<String, Metric> map, int totalCount, double totalAmount) {
        List list = new ArrayList();
        for (Metric metric : map.values()) {
            metric.percentage = totalAmount <= 0 ? 0 : metric.amount * 100.0 / totalAmount;
            list.add(metric);
        }
        return list;
    }

    private double sum(Map<String, Metric> map) {
        double result = 0;
        for (Metric metric : map.values()) result += metric.amount;
        return result;
    }

    /** Non-Keuangan: sembunyikan tab Dashboard (composer ini), lalu pilih tab "Reimbursement" agar pegawai
     *  langsung bisa mengajukan tanpa popup akses. */
    private void sembunyikanTabDanPilihReimbursement(Component comp) {
        try {
            Component c = comp;
            while (c != null && !(c instanceof org.zkoss.zul.Tabpanel)) {
                c = c.getParent();
            }
            if (c == null) return;
            org.zkoss.zul.Tabpanel tp = (org.zkoss.zul.Tabpanel) c;
            if (tp.getLinkedTab() != null) tp.getLinkedTab().setVisible(false);
            org.zkoss.zul.Tabbox tb = tp.getTabbox();
            if (tb != null && tb.getTabs() != null) {
                java.util.List<?> tabs = tb.getTabs().getChildren();
                if (tabs.size() > 1 && tabs.get(1) instanceof org.zkoss.zul.Tab) {
                    ((org.zkoss.zul.Tab) tabs.get(1)).setSelected(true);
                }
            }
        } catch (Exception e) { /* kosmetik, abaikan */ }
    }

    private boolean isFinance() {
        Tbmuser user = Common.getCurrentUser();
        if (user == null) return false;
        Set roles = user.ambilRolesId();
        if (roles != null) {
            for (Object role : roles) {
                String id = String.valueOf(role);
                if (Tbmrole.ADMINISTRATOR.equalsIgnoreCase(id) || Tbmrole.KEUANGAN.equalsIgnoreCase(id)) return true;
            }
        }
        // Hormati setelan per-role "Tampilkan Dashboard Keuangan" (TbmroleAction -> Tbmrole.getKeuangan()),
        // sehingga akses bisa di-SETUP admin tanpa harus role ber-id "keu".
        for (Tbmrole tr : new Tbmrole[] { user.getUserRole(), user.getUserRole2(), user.getUserRole3(),
                user.getUserRole4(), user.getUserRole5() }) {
            if (tr != null && Boolean.TRUE.equals(tr.getKeuangan())) return true;
        }
        return false;
    }

    private Date startOfDay(Date date) { Calendar c = ais.ui.util.WaktuUtil.getCalendar(); c.setTime(date); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTime(); }
    private Date nextDay(Date date) { Calendar c = ais.ui.util.WaktuUtil.getCalendar(); c.setTime(startOfDay(date)); c.add(Calendar.DAY_OF_MONTH,1); return c.getTime(); }
    private int days(Date from, Date to) { return from == null || to == null ? 0 : (int) Math.max(0, (to.getTime() - from.getTime()) / 86400000L); }
    private String two(int value) { return value < 10 ? "0" + value : String.valueOf(value); }
    private boolean empty(String value) { return value == null || value.trim().isEmpty(); }

    private static class Metric {
        String label; int count; double amount; double percentage;
        Metric(String label) { this.label = label; }
        void add(double value) { count++; amount += value; }
    }
    private static class QueueItem {
        ReimbursementPegawai data; int age;
        QueueItem(ReimbursementPegawai data, int age) { this.data = data; this.age = age; }
    }
    private class MetricRenderer implements RowRenderer {
        public void render(Row row, Object value) {
            Metric m = (Metric) value;
            new Label(m.label).setParent(row);
            new Label(String.valueOf(m.count)).setParent(row);
            new Label("Rp " + Common.numberFormat.get().format(m.amount)).setParent(row);
            new Label(Common.numberFormat.get().format(m.percentage) + "%").setParent(row);
        }
    }
    private class QueueRenderer implements RowRenderer {
        public void render(Row row, Object value) {
            QueueItem q = (QueueItem) value;
            ReimbursementPegawai d = q.data;
            new Label(d.getKode()).setParent(row);
            new Label(d.getPegawai() == null ? "-" : d.getPegawai().getNama()).setParent(row);
            new Label(d.getStatus()).setParent(row);
            new Label(String.valueOf(q.age) + " hari").setParent(row);
            new Label("Rp " + Common.numberFormat.get().format(d.getNominal())).setParent(row);
            new Label(d.getAtasan() == null ? "-" : d.getAtasan().getNama()).setParent(row);
        }
    }
}
