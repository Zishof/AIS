package ais.common.newui.menu;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Projections;

import ais.action.master.generic.v2.adapter.GenericCrudInstitutionScope;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;

/**
 * Membaca entity yang benar-benar direferensikan Action existing dari constant
 * pool class, lalu menghitungnya dengan scope institusi New UI yang sama.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiActionDashboardMetricService {
    private static final Pattern MODEL = Pattern.compile("ais/database/model/[A-Za-z0-9_$/]+");
    private static final Pattern ACTION = Pattern.compile("ais/action/[A-Za-z0-9_$/]+");
    private static final int MAX_METRICS = 24;

    private NewUiActionDashboardMetricService() { }

    public static boolean hasNativeAdapter(String sourceClass) {
        return sourceClass != null && (sourceClass.endsWith("PendaftaranOverviewDashboardAction")
                || sourceClass.endsWith("PendapatanDashboardAction")
                || sourceClass.endsWith("DiagnosaTerbanyakDashboardAction")
                || sourceClass.endsWith("KadaluarsaFarmasiDashboardAction"));
    }

    public static List<Metric> load(String sourceClass, Tbmuser user) {
        List<String> names = entityClassNames(sourceClass);
        List<Metric> result = new ArrayList<Metric>();
        Session session = null;
        try { session = HibernateUtil.currentSession(); } catch (Exception ignored) { }
        for (int i = 0; i < names.size() && result.size() < MAX_METRICS; i++) {
            String name = names.get(i); long count = 0L; boolean available = false;
            try {
                Class entity = Class.forName(name);
                if (HibernateUtil.getSessionFactory().getClassMetadata(entity) == null) continue;
                Criteria criteria = session.createCriteria(entity).setProjection(Projections.rowCount());
                GenericCrudInstitutionScope.apply(criteria, entity, user);
                Object value = criteria.uniqueResult();
                count = value instanceof Number ? ((Number) value).longValue() : 0L;
                available = true;
            } catch (Exception error) {
                try { ais.common.ErrorAuditUtil.record(error,
                        "NewUiActionDashboardMetricService." + sourceClass + "." + name); }
                catch (Exception ignored) { }
            }
            result.add(new Metric(label(name), name, count, available));
        }
        if (result.isEmpty()) loadSqlAdapter(sourceClass, session, result);
        return Collections.unmodifiableList(result);
    }

    /** Adapter query identik dengan builder dashboard SIRS existing. */
    private static void loadSqlAdapter(String sourceClass, Session session, List<Metric> result) {
        if (session == null || sourceClass == null) return;
        int year = Calendar.getInstance().get(Calendar.YEAR);
        try {
            if (sourceClass.endsWith("PendaftaranOverviewDashboardAction")) {
                SQLQuery query = session.createSQLQuery("select coalesce(nullif(trim(jenis),''),'(Lainnya)'), count(*) "
                        + "from sirs.pendaftaran where extract(year from tanggalpendaftaran)=:tahun group by 1 order by 2 desc");
                query.setInteger("tahun", year); List rows = query.list(); long total = 0L;
                for (int i = 0; rows != null && i < rows.size(); i++) total += number(((Object[]) rows.get(i))[1]).longValue();
                result.add(new Metric("Total Pendaftaran " + year, "sirs.pendaftaran", total, true));
                addGrouped(result, rows, "Pendaftaran ", "sirs.pendaftaran");
            } else if (sourceClass.endsWith("PendapatanDashboardAction")) {
                SQLQuery query = session.createSQLQuery("select count(*), coalesce(sum(total_biaya),0), "
                        + "coalesce(sum(bayar_tunai),0), coalesce(sum(bayar_non_tunai),0) from sirs.pembayaran "
                        + "where extract(year from tanggal_pembayaran)=:tahun");
                query.setInteger("tahun", year); Object row = query.uniqueResult();
                if (row instanceof Object[]) {
                    Object[] values = (Object[]) row;
                    add(result, "Transaksi " + year, values, 0, "sirs.pembayaran");
                    add(result, "Total Pendapatan " + year, values, 1, "sirs.pembayaran.total_biaya");
                    add(result, "Tunai " + year, values, 2, "sirs.pembayaran.bayar_tunai");
                    add(result, "Non Tunai " + year, values, 3, "sirs.pembayaran.bayar_non_tunai");
                }
            } else if (sourceClass.endsWith("DiagnosaTerbanyakDashboardAction")) {
                SQLQuery total = session.createSQLQuery("select count(*) from sirs.diagnosa_penyakit "
                        + "where diagnosa_akhir1 is not null and extract(year from tanggal)=:tahun");
                total.setInteger("tahun", year); result.add(metric("Total Diagnosis " + year,
                        "sirs.diagnosa_penyakit", total.uniqueResult()));
                SQLQuery top = session.createSQLQuery("select coalesce(nullif(trim(i.nama_indonesia),''),i.kode,'(Tanpa Nama)'), count(*) "
                        + "from sirs.diagnosa_penyakit d join sirs.icd i on d.diagnosa_akhir1=i.id "
                        + "where extract(year from d.tanggal)=:tahun group by 1 order by 2 desc limit 10");
                top.setInteger("tahun", year); addGrouped(result, top.list(), "", "sirs.diagnosa_penyakit");
            } else if (sourceClass.endsWith("KadaluarsaFarmasiDashboardAction")) {
                SQLQuery query = session.createSQLQuery("select case when tanggal_kadaluarsa < now() then 0 "
                        + "when tanggal_kadaluarsa <= now()+interval '30 day' then 1 "
                        + "when tanggal_kadaluarsa <= now()+interval '90 day' then 2 else 3 end, count(*) "
                        + "from sirs.kadaluarsa where tanggal_kadaluarsa is not null group by 1 order by 1");
                List rows = query.list(); String[] labels = { "Sudah Kedaluwarsa", "≤ 30 Hari", "31–90 Hari", "> 90 Hari" };
                long[] counts = new long[labels.length];
                for (int i = 0; rows != null && i < rows.size(); i++) {
                    Object[] row = (Object[]) rows.get(i); int index = number(row[0]).intValue();
                    if (index >= 0 && index < labels.length) counts[index] = number(row[1]).longValue();
                }
                for (int i = 0; i < labels.length; i++) result.add(new Metric(labels[i], "sirs.kadaluarsa", counts[i], true));
            }
        } catch (Exception error) {
            try { ais.common.ErrorAuditUtil.record(error, "NewUiActionDashboardMetricService.sql." + sourceClass); }
            catch (Exception ignored) { }
        }
    }

    private static void addGrouped(List<Metric> result, List rows, String prefix, String source) {
        for (int i = 0; rows != null && i < rows.size() && result.size() < MAX_METRICS; i++) {
            Object[] row = (Object[]) rows.get(i);
            result.add(metric(prefix + String.valueOf(row[0]), source, row[1]));
        }
    }
    private static void add(List<Metric> result, String label, Object[] values, int index, String source) {
        if (index < values.length) result.add(metric(label, source, values[index]));
    }
    private static Metric metric(String label, String source, Object value) {
        return new Metric(label, source, number(value).longValue(), true);
    }
    private static Number number(Object value) { return value instanceof Number ? (Number) value : Long.valueOf(0L); }

    /** Deterministik dan tanpa database; dipakai audit parity source Action. */
    public static List<String> entityClassNames(String sourceClass) {
        Set<String> names = new LinkedHashSet<String>();
        scanClass(sourceClass, names, new LinkedHashSet<String>());
        return Collections.unmodifiableList(new ArrayList<String>(names));
    }

    private static void scanClass(String sourceClass, Set<String> names, Set<String> visited) {
        if (sourceClass == null || sourceClass.trim().length() == 0 || !visited.add(sourceClass)) return;
        InputStream input = null; DataInputStream data = null;
        Set<String> innerClasses = new LinkedHashSet<String>();
        try {
            String resource = "/" + sourceClass.replace('.', '/') + ".class";
            input = NewUiActionDashboardMetricService.class.getResourceAsStream(resource);
            if (input == null) return;
            data = new DataInputStream(input);
            if (data.readInt() != 0xCAFEBABE) return;
            data.readUnsignedShort(); data.readUnsignedShort();
            int count = data.readUnsignedShort();
            for (int i = 1; i < count; i++) {
                int tag = data.readUnsignedByte();
                if (tag == 1) collect(data.readUTF(), sourceClass, names, innerClasses);
                else if (tag == 3 || tag == 4) data.readInt();
                else if (tag == 5 || tag == 6) { data.readLong(); i++; }
                else if (tag == 7 || tag == 8 || tag == 16 || tag == 19 || tag == 20) data.readUnsignedShort();
                else if (tag == 9 || tag == 10 || tag == 11 || tag == 12 || tag == 17 || tag == 18) {
                    data.readUnsignedShort(); data.readUnsignedShort();
                } else if (tag == 15) { data.readUnsignedByte(); data.readUnsignedShort(); }
                else break;
            }
        } catch (Exception error) {
            try { ais.common.ErrorAuditUtil.record(error,
                    "NewUiActionDashboardMetricService.bytecode." + sourceClass); }
            catch (Exception ignored) { }
        } finally {
            try { if (data != null) data.close(); else if (input != null) input.close(); }
            catch (Exception ignored) { }
        }
        for (String inner : innerClasses) scanClass(inner, names, visited);
    }

    private static void collect(String value, String sourceClass, Set<String> names,
            Set<String> innerClasses) {
        Matcher matcher = MODEL.matcher(value == null ? "" : value);
        while (matcher.find()) {
            String name = matcher.group().replace('/', '.');
            if (name.indexOf(".package-info") < 0) names.add(name);
        }
        String internal = sourceClass.replace('.', '/');
        Pattern innerPattern = Pattern.compile(Pattern.quote(internal) + "\\$[A-Za-z0-9_$]+");
        Matcher inner = innerPattern.matcher(value == null ? "" : value);
        while (inner.find()) innerClasses.add(inner.group().replace('/', '.'));
        Matcher dependency = ACTION.matcher(value == null ? "" : value);
        while (dependency.find()) {
            String name = dependency.group().replace('/', '.');
            String simple = name.substring(name.lastIndexOf('.') + 1);
            if (!name.equals(sourceClass) && (simple.indexOf("Dashboard") >= 0
                    || simple.indexOf("Dasboard") >= 0 || simple.indexOf("Dasbor") >= 0
                    || simple.indexOf("Builder") >= 0)) {
                innerClasses.add(name);
            }
        }
    }

    private static String label(String className) {
        String simple = className.substring(className.lastIndexOf('.') + 1);
        return simple.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
    }

    public static final class Metric implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label, entityClass; private final long value; private final boolean available;
        Metric(String label, String entityClass, long value, boolean available) {
            this.label = label; this.entityClass = entityClass; this.value = value; this.available = available;
        }
        public String getLabel() { return label; }
        public String getEntityClass() { return entityClass; }
        public long getValue() { return value; }
        public boolean isAvailable() { return available; }
    }
}
