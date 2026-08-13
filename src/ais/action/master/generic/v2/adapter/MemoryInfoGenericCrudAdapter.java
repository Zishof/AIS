package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.action.master.generic.v2.GenericCrudOperation;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.MemoryInfo;

/** Parity MemoryInfoAction: monitoring read-only, laporan, dan hapus semua. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MemoryInfoGenericCrudAdapter extends AbstractGenericCrudEntityAdapter<MemoryInfo>
        implements GenericCrudScopeAdapter, GenericCrudCustomActionProvider, GenericCrudDashboardProvider {
    public MemoryInfo createNew(GenericCrudRequestContext context) { return new MemoryInfo(); }
    public boolean canDelete(MemoryInfo target, GenericCrudRequestContext context, List reasons) { return false; }
    public List getNaturalKeyProperties() { return new ArrayList(); }

    public List getActions(GenericCrudDefinition definition, GenericCrudRequestContext context) {
        List result = new ArrayList(); Map action = new LinkedHashMap();
        action.put("actionKey", "clear_all"); action.put("label", "Hapus Semua Info Memori");
        action.put("requiredPrivilege", GenericCrudOperation.DELETE); action.put("selectionMode", "NONE");
        action.put("enabled", Boolean.valueOf(context != null && context.isCanDelete())); action.put("dangerous", Boolean.TRUE);
        action.put("confirmation", "Seluruh histori info memori akan dihapus. Lanjutkan?");
        result.add(action); return result;
    }

    public GenericCrudResult execute(String actionKey, List selectedIds, Map parameters,
            GenericCrudRequestContext context) throws Exception {
        if (!"clear_all".equals(actionKey)) return GenericCrudResult.error("ACTION_NOT_ALLOWED", "Aksi tidak dikenal.");
        Session session = HibernateUtil.currentNativeSession(); Transaction tx = null;
        try {
            tx = session.beginTransaction(); int affected = session.createSQLQuery("delete from memory_info").executeUpdate();
            tx.commit(); Map data = new LinkedHashMap(); data.put("affected", Integer.valueOf(affected));
            return GenericCrudResult.ok("Seluruh info memori berhasil dihapus.", data);
        } catch (Exception failure) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignored) { }
            throw failure;
        } finally { HibernateUtil.closeSession(); }
    }

    public Map getDashboard(GenericCrudRequestContext context) throws Exception {
        Map dashboard = new LinkedHashMap(); dashboard.put("title", "Dasbor Pemakaian Memori");
        dashboard.put("description", "Ringkasan kondisi memori server dan tren beban terbaru.");
        List kpis = new ArrayList(), trend = new ArrayList(), recent = new ArrayList();
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            List samples = session.createCriteria(MemoryInfo.class).addOrder(Order.desc("id")).setMaxResults(120).list();
            if (!samples.isEmpty()) {
                MemoryInfo current = (MemoryInfo) samples.get(0); long max = mb(current.getMaxMemory());
                long allocated = mb(current.getAllocatedMemory()), free = mb(current.getTotalFreeMemory());
                long used = Math.max(0L, allocated - free); long percent = allocated > 0 ? Math.round(used * 100.0 / allocated) : 0L;
                kpis.add(kpi("Batas Maksimum", max, "MB")); kpis.add(kpi("Dialokasikan", allocated, "MB"));
                kpis.add(kpi("Sedang Dipakai", used, "MB")); kpis.add(kpi("Pemakaian", percent, "%"));
            }
            for (int i = samples.size() - 1; i >= 0; i--) {
                MemoryInfo sample = (MemoryInfo) samples.get(i); Map point = new LinkedHashMap();
                point.put("label", String.valueOf(sample.getId()));
                point.put("value", Long.valueOf(Math.max(0L, mb(sample.getAllocatedMemory()) - mb(sample.getTotalFreeMemory())))); trend.add(point);
            }
            for (int i = 0; i < samples.size() && i < 20; i++) {
                MemoryInfo sample = (MemoryInfo) samples.get(i); Map row = new LinkedHashMap();
                row.put("id", sample.getId()); row.put("timestamp", sample.getTanggal_dirubah());
                row.put("maxMb", Long.valueOf(mb(sample.getMaxMemory()))); row.put("allocatedMb", Long.valueOf(mb(sample.getAllocatedMemory())));
                row.put("freeMb", Long.valueOf(mb(sample.getTotalFreeMemory()))); recent.add(row);
            }
        } finally { try { session.close(); } catch (Exception ignored) { } }
        dashboard.put("kpis", kpis); dashboard.put("trend", trend); dashboard.put("recent", recent); return dashboard;
    }
    private Map kpi(String label, long value, String unit) { Map result = new LinkedHashMap(); result.put("label", label); result.put("value", Long.valueOf(value)); result.put("unit", unit); return result; }
    private long mb(Long value) { return value == null ? 0L : value.longValue() / 1024L / 1024L; }
    public void applyReadScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void applyCountScope(Criteria criteria, GenericCrudRequestContext context) { }
    public void validateObjectScope(GeneralValueObject object, GenericCrudRequestContext context) { }
}
