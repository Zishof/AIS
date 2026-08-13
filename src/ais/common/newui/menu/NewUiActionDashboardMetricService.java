package ais.common.newui.menu;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Criteria;
import org.hibernate.Session;
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
        return Collections.unmodifiableList(result);
    }

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
                    || simple.indexOf("Dasboard") >= 0 || simple.indexOf("Builder") >= 0)) {
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
