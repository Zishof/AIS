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
        "apikey", "accesskey", "refresh", "binary", "content", "isi_file", "path", "filename",
        "filelocation"
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
                    || Modifier.isAbstract(mapped.getModifiers())) continue;
            Map row = new LinkedHashMap();
            row.put("entityKey", mapped.getName()); row.put("displayName", humanize(mapped.getSimpleName()));
            row.put("packageName", mapped.getPackage().getName()); row.put("tableName", metadata.getEntityName());
            row.put("restricted", Boolean.valueOf(isBlockedClass(mapped)));
            row.put("mode", isBlockedClass(mapped) ? GenericCrudDefinition.READ_ONLY : GenericCrudDefinition.FULL_CRUD);
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
        if (entityClass == null) return null;
        ClassMetadata metadata = HibernateUtil.getSessionFactory().getClassMetadata(entityClass);
        if (metadata == null || metadata.getIdentifierPropertyName() == null) return null;
        boolean restrictedClass = isBlockedClass(entityClass);
        boolean constructable = hasDefaultConstructor(entityClass);
        boolean assignedGenerator = isAssignedIdentifier(metadata);
        boolean assignedIdentifierSupported = isSupportedScalar(metadata.getIdentifierType().getReturnedClass());
        boolean autoCreatePossible = !restrictedClass && constructable
                && (!assignedGenerator || assignedIdentifierSupported);

        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setEntityClass(entityClass);
        if (administrative) definition.setEntityKey("admin:" + entityClass.getName());
        definition.setModuleKey(module);
        definition.setPageKey(page);
        definition.setDisplayName(humanize(entityClass.getSimpleName()));
        definition.setIdentifierProperty(metadata.getIdentifierPropertyName());
        definition.setLifecycleStatus(restrictedClass ? GenericCrudDefinition.READ_ONLY : GenericCrudDefinition.FULL_CRUD);
        definition.setEnabled(true);
        definition.setCreateEnabled(autoCreatePossible);
        definition.setUpdateEnabled(!restrictedClass);
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
        definition.setDeleteEnabled(!restrictedClass && softDelete);
        GenericCrudAutoEntityAdapter adapter = new GenericCrudAutoEntityAdapter(entityClass, softDelete);
        definition.setAdapter(adapter);
        definition.setScopeAdapter(adapter);

        boolean assignedIdentifier = assignedGenerator && assignedIdentifierSupported;
        GenericCrudFieldDefinition identifier = field(metadata.getIdentifierPropertyName(), "ID",
                metadata.getIdentifierType().getReturnedClass(), !restrictedClass && assignedIdentifier, false, 0);
        identifier.setRequired(!restrictedClass && assignedIdentifier);
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
            ClassMetadata relationMetadata = type.isAssociationType()
                    ? HibernateUtil.getSessionFactory().getClassMetadata(returned) : null;
            boolean relation = type.isAssociationType() && GeneralValueObject.class.isAssignableFrom(returned)
                    && relationMetadata != null;
            boolean supported = isSupportedScalar(returned) || returned.isEnum() || relation;
            if (!supported) {
                if (!isInternalField(name) && !isNullable(metadata, i)) autoCreatePossible = false;
                continue;
            }
            boolean mutableRelation = !relation || isSupportedIdentifier(relationMetadata);
            if (!mutableRelation && !isNullable(metadata, i)) autoCreatePossible = false;
            GenericCrudFieldDefinition field = field(name, humanize(name), returned,
                    !restrictedClass && !sensitive && !internal && mutableRelation,
                    !restrictedClass && !sensitive && !internal && mutableRelation, i + 1);
            field.setSensitive(sensitive);
            field.setReadable(!sensitive);
            field.setExportable(!sensitive);
            field.setTableVisible(!sensitive && tableCount++ < 12);
            field.setSortable(!sensitive && !relation);
            field.setSearchable(!sensitive && returned == String.class);
            field.setQuickFilter(field.isSearchable() && i < 8);
            field.setRequired(!restrictedClass && !sensitive && !internal && mutableRelation && !isNullable(metadata, i));
            if (relation) {
                field.setEditorType("relation");
                field.setRelationEntityKey(returned.getName());
                field.setRelationDisplayProperty(chooseDisplayProperty(relationMetadata));
                field.setRelationSearchProperties("kode,nama,nim");
            } else if (returned.isEnum()) {
                Object[] constants = returned.getEnumConstants();
                String[] values = new String[constants == null ? 0 : constants.length];
                for (int e = 0; e < values.length; e++) values[e] = String.valueOf(constants[e]);
                field.setEnumValues(values);
            }
            definition.addField(field);
        }
        String defaultSort = chooseSort(metadata);
        definition.setDefaultSortProperty(defaultSort);
        definition.setVersionProperty(findVersionProperty(metadata));
        definition.setCreateEnabled(autoCreatePossible);
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
        if (type == java.sql.Time.class) return "time";
        if (type == java.sql.Timestamp.class) return "datetime-local";
        if (Date.class.isAssignableFrom(type)) return "date";
        if (type.isEnum()) return "select";
        return "text";
    }
    private static boolean isSupportedScalar(Class type) {
        return type == String.class || type == Boolean.class || type == Boolean.TYPE
                || Number.class.isAssignableFrom(type) || type == Integer.TYPE || type == Long.TYPE
                || type == Short.TYPE || type == Byte.TYPE || type == Double.TYPE || type == Float.TYPE
                || type == Character.class || type == Character.TYPE || Date.class.isAssignableFrom(type)
                || java.util.UUID.class.isAssignableFrom(type);
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
    private static boolean hasDefaultConstructor(Class type) {
        try { type.getDeclaredConstructor(new Class[0]); return true; }
        catch (Exception missing) { return false; }
    }
    private static boolean isSupportedIdentifier(ClassMetadata metadata) {
        return metadata != null && isSupportedScalar(metadata.getIdentifierType().getReturnedClass());
    }
    private static boolean isAssignedIdentifier(ClassMetadata metadata) {
        try {
            Object factory = HibernateUtil.getSessionFactory();
            java.lang.reflect.Method getPersister = factory.getClass().getMethod("getEntityPersister", new Class[] { String.class });
            Object persister = getPersister.invoke(factory, new Object[] { metadata.getEntityName() });
            java.lang.reflect.Method getGenerator = persister.getClass().getMethod("getIdentifierGenerator", new Class[0]);
            Object generator = getGenerator.invoke(persister, new Object[0]);
            return generator != null && generator.getClass().getName().toLowerCase().indexOf("assigned") >= 0;
        } catch (Exception unavailable) { return false; }
    }
    private static boolean isNullable(ClassMetadata metadata, int propertyIndex) {
        try {
            Object factory = HibernateUtil.getSessionFactory();
            java.lang.reflect.Method getPersister = factory.getClass().getMethod("getEntityPersister", new Class[] { String.class });
            Object persister = getPersister.invoke(factory, new Object[] { metadata.getEntityName() });
            java.lang.reflect.Method getNullability = persister.getClass().getMethod("getPropertyNullability", new Class[0]);
            boolean[] nullable = (boolean[]) getNullability.invoke(persister, new Object[0]);
            return nullable == null || propertyIndex < 0 || propertyIndex >= nullable.length || nullable[propertyIndex];
        } catch (Exception unavailable) { return true; }
    }
    private static String chooseDisplayProperty(ClassMetadata metadata) {
        if (metadata == null) return null;
        try { if (metadata.getPropertyType("nama").getReturnedClass() == String.class) return "nama"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("kode").getReturnedClass() == String.class) return "kode"; } catch (Exception ignored) { }
        try { if (metadata.getPropertyType("nim").getReturnedClass() == String.class) return "nim"; } catch (Exception ignored) { }
        return metadata.getIdentifierPropertyName();
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
