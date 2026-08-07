package ais.action.master.generic.v2;

import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;

import ais.action.master.generic.v2.adapter.GenericCrudAutoEntityAdapter;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;

/** Membuat definisi dari metadata Hibernate; tidak pernah menerima nama class dari parameter HTTP. */
@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
public final class GenericCrudAutoDefinitionFactory {
    private static final String[] BLOCKED_CLASS_TOKENS = new String[] {
        "password", "credential", "token", "secret", "oauth", "session", "login", "captcha",
        "user", "role", "privilege", "permission", "hakakses", "menu",
        "file", "lampiran", "dokumen", "blob", "audit", "revision", "log", "history",
        "queue", "job", "notification", "notifikasi", "webhook", "bank", "rekening", "payment"
    };
    private static final String[] BLOCKED_FIELD_TOKENS = new String[] {
        "password", "passwd", "token", "secret", "credential", "salt", "hash", "privatekey",
        "apikey", "accesskey", "refresh", "binary", "content", "isi_file", "path", "filename"
    };
    private static final String[] INTERNAL_FIELDS = new String[] {
        "oleh", "olehid", "tanggal_dirubah", "created", "createdat", "updated", "updatedat",
        "deleted", "deletedat", "version", "copydari", "initdata", "dikunci"
    };

    private GenericCrudAutoDefinitionFactory() { }

    public static GenericCrudDefinition build(String module, String page, String[] serverCandidates) throws Exception {
        Class entityClass = selectMappedClass(module, page, serverCandidates);
        return buildForClass(module, page, entityClass, false);
    }

    /** Admin model browser: key hanya boleh cocok persis dengan mapped GVO Hibernate. */
    public static GenericCrudDefinition buildAdministrative(String module, String page, String mappedEntityKey) throws Exception {
        Class entityClass = findMappedClass(mappedEntityKey);
        return buildForClass(module, page, entityClass, true);
    }

    public static List listAdministrativeModels() {
        List result = new ArrayList();
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            Class mapped;
            try { mapped = metadata.getMappedClass(EntityMode.POJO); } catch (Exception invalid) { continue; }
            if (mapped == null || !GeneralValueObject.class.isAssignableFrom(mapped)
                    || Modifier.isAbstract(mapped.getModifiers()) || isBlockedClass(mapped)) continue;
            Map row = new LinkedHashMap();
            row.put("entityKey", mapped.getName()); row.put("displayName", humanize(mapped.getSimpleName()));
            row.put("packageName", mapped.getPackage().getName()); row.put("tableName", metadata.getEntityName());
            result.add(row);
        }
        Collections.sort(result, new Comparator() {
            public int compare(Object one, Object two) {
                return String.valueOf(((Map) one).get("displayName")).compareToIgnoreCase(String.valueOf(((Map) two).get("displayName")));
            }
        });
        return result;
    }

    private static GenericCrudDefinition buildForClass(String module, String page, Class entityClass,
            boolean administrative) throws Exception {
        if (entityClass == null || isBlockedClass(entityClass)) return null;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        if (metadata == null || metadata.getIdentifierPropertyName() == null) return null;

        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setEntityClass(entityClass);
        if (administrative) definition.setEntityKey("admin:" + entityClass.getName());
        definition.setModuleKey(module);
        definition.setPageKey(page);
        definition.setDisplayName(humanize(entityClass.getSimpleName()));
        definition.setIdentifierProperty(metadata.getIdentifierPropertyName());
        definition.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        definition.setEnabled(true);
        definition.setCreateEnabled(true);
        definition.setUpdateEnabled(true);
        definition.setImportEnabled(false);
        definition.setExportPdfEnabled(true);
        definition.setExportDocxEnabled(true);
        definition.setExportPptxEnabled(true);
        definition.setAuditEnabled(false);
        definition.setRowAuditEnabled(false);
        definition.setRestoreEnabled(false);
        definition.setAdminDeleteEnabled(false);
        definition.setAutoGenerated(true);
        definition.setAdministrativeAutoCrud(administrative);

        boolean softDelete = hasBooleanProperty(metadata, "aktif");
        definition.setDeleteEnabled(softDelete);
        GenericCrudAutoEntityAdapter adapter = new GenericCrudAutoEntityAdapter(entityClass, softDelete);
        definition.setAdapter(adapter);
        definition.setScopeAdapter(adapter);

        GenericCrudFieldDefinition identifier = field(metadata.getIdentifierPropertyName(), "ID",
                metadata.getIdentifierType().getReturnedClass(), false, false, 0);
        identifier.setTableVisible(true);
        identifier.setSortable(true);
        definition.addField(identifier);

        String[] names = metadata.getPropertyNames();
        Type[] types = metadata.getPropertyTypes();
        int tableCount = 1;
        for (int i = 0; i < names.length; i++) {
            String name = names[i]; Type type = types[i]; Class returned = type.getReturnedClass();
            if (type.isCollectionType() || returned == byte[].class || java.sql.Blob.class.isAssignableFrom(returned)) continue;
            boolean sensitive = isBlockedField(name);
            boolean internal = isInternalField(name);
            boolean supported = isSupportedScalar(returned);
            if (!supported) continue;
            GenericCrudFieldDefinition field = field(name, humanize(name), returned,
                    supported && !sensitive && !internal, supported && !sensitive && !internal, i + 1);
            field.setSensitive(sensitive);
            field.setReadable(!sensitive);
            field.setExportable(!sensitive);
            field.setTableVisible(!sensitive && tableCount++ < 12);
            field.setSortable(!sensitive && supported);
            field.setSearchable(!sensitive && returned == String.class);
            field.setQuickFilter(field.isSearchable() && i < 8);
            definition.addField(field);
        }
        String defaultSort = chooseSort(metadata);
        definition.setDefaultSortProperty(defaultSort);
        definition.setVersionProperty(findVersionProperty(metadata));
        return definition;
    }

    private static Class findMappedClass(String entityKey) {
        if (entityKey == null || entityKey.length() == 0 || entityKey.indexOf("ais.database.model.") != 0) return null;
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            try {
                Class mapped = metadata.getMappedClass(EntityMode.POJO);
                if (mapped != null && entityKey.equals(mapped.getName())
                        && GeneralValueObject.class.isAssignableFrom(mapped)
                        && !Modifier.isAbstract(mapped.getModifiers())) return mapped;
            } catch (Exception ignored) { }
        }
        return null;
    }

    private static Class selectMappedClass(String module, String page, String[] candidates) {
        if (candidates == null || candidates.length == 0) return null;
        Map all = HibernateUtil.getSessionFactory().getAllClassMetadata();
        Class best = null; int bestScore = -1;
        Iterator values = all.values().iterator();
        while (values.hasNext()) {
            ClassMetadata metadata = (ClassMetadata) values.next();
            Class mapped;
            try { mapped = metadata.getMappedClass(EntityMode.POJO); } catch (Exception invalid) { continue; }
            if (mapped == null || !GeneralValueObject.class.isAssignableFrom(mapped) || Modifier.isAbstract(mapped.getModifiers())) continue;
            for (int i = 0; i < candidates.length; i++) {
                if (!mapped.getSimpleName().equals(candidates[i])) continue;
                int score = 10;
                String normalizedClass = normalize(mapped.getSimpleName());
                String normalizedPage = normalize(page);
                if (normalizedClass.equals(normalizedPage)) score += 100;
                else if (normalizedPage.indexOf(normalizedClass) >= 0 || normalizedClass.indexOf(normalizedPage) >= 0) score += 40;
                if (mapped.getPackage().getName().toLowerCase().indexOf("." + String.valueOf(module).toLowerCase()) >= 0) score += 20;
                score -= i;
                if (score > bestScore) { best = mapped; bestScore = score; }
            }
        }
        return best;
    }

    private static GenericCrudFieldDefinition field(String property, String label, Class type,
            boolean createable, boolean updateable, int position) {
        GenericCrudFieldDefinition result = new GenericCrudFieldDefinition(property, label, type.getName());
        result.setCreateable(createable); result.setUpdateable(updateable); result.setPosition(position);
        result.setEditorType(editor(type)); return result;
    }
    private static String editor(Class type) {
        if (type == Boolean.class || type == Boolean.TYPE) return "checkbox";
        if (Number.class.isAssignableFrom(type) || type.isPrimitive()) return "number";
        if (Date.class.isAssignableFrom(type)) return "date";
        return "text";
    }
    private static boolean isSupportedScalar(Class type) {
        return type == String.class || type == Boolean.class || type == Boolean.TYPE
                || Number.class.isAssignableFrom(type) || type == Integer.TYPE || type == Long.TYPE
                || type == Short.TYPE || type == Double.TYPE || type == Float.TYPE
                || Date.class.isAssignableFrom(type);
    }
    private static boolean hasBooleanProperty(ClassMetadata metadata, String name) {
        try { Class type = metadata.getPropertyType(name).getReturnedClass(); return type == Boolean.class || type == Boolean.TYPE; }
        catch (Exception ignored) { return false; }
    }
    private static String chooseSort(ClassMetadata metadata) {
        try { if (metadata.getPropertyType("nama").getReturnedClass() == String.class) return "nama"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("kode").getReturnedClass() == String.class) return "kode"; } catch (Exception ignored) { }
        return metadata.getIdentifierPropertyName();
    }
    private static String findVersionProperty(ClassMetadata metadata) {
        try { int index = metadata.getVersionProperty(); return index < 0 ? null : metadata.getPropertyNames()[index]; }
        catch (Exception ignored) { return null; }
    }
    private static boolean isBlockedClass(Class type) { return containsToken(type.getName(), BLOCKED_CLASS_TOKENS); }
    private static boolean isBlockedField(String value) { return containsToken(value, BLOCKED_FIELD_TOKENS); }
    private static boolean isInternalField(String value) { return containsToken(normalize(value), INTERNAL_FIELDS); }
    private static boolean containsToken(String value, String[] tokens) {
        String normalized = normalize(value);
        for (int i = 0; i < tokens.length; i++) if (normalized.indexOf(normalize(tokens[i])) >= 0) return true;
        return false;
    }
    private static String normalize(String value) { return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(); }
    private static String humanize(String value) {
        if (value == null) return "Data";
        String spaced = value.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        return spaced.length() == 0 ? "Data" : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
