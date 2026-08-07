package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.adapter.AgamaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudFormOverrideProvider;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudFormProvider;
import ais.database.model.Agama;
import ais.database.model.Mahasiswa;

/**
 * Registry allow-list. Scanner menghasilkan kandidat disabled; hanya entity yang
 * direview dan ditulis eksplisit di sini yang dapat dieksekusi.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudDefinitionRegistry {
    private static final Map DEFINITIONS = new LinkedHashMap();
    static {
        register(buildAgama());
        register(buildMahasiswaParity());
    }
    private GenericCrudDefinitionRegistry() { }

    public static GenericCrudDefinition resolve(String entityKey, String module, String page) throws GenericCrudException {
        GenericCrudDefinition definition = null;
        if (entityKey != null && entityKey.length() > 0) { definition = (GenericCrudDefinition) DEFINITIONS.get(entityKey); }
        if (definition == null && module != null && page != null) {
            definition = (GenericCrudDefinition) DEFINITIONS.get(routeKey(module, page));
        }
        if (definition == null) { throw new GenericCrudException(404, "ENTITY_NOT_REGISTERED", "Entity tidak terdaftar pada allow-list Generic CRUD."); }
        if (!same(module, definition.getModuleKey()) || !same(page, definition.getPageKey())) {
            throw new GenericCrudException(403, "BINDING_MISMATCH", "Binding module/page tidak sesuai konfigurasi entity.");
        }
        GenericCrudRuntimeMetadataVerifier.verify(definition);
        return definition;
    }

    public static List listDefinitions() {
        List result = new ArrayList();
        java.util.Iterator iterator = DEFINITIONS.values().iterator();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (value instanceof GenericCrudDefinition && !result.contains(value)) { result.add(value); }
        }
        return Collections.unmodifiableList(result);
    }

    private static void register(GenericCrudDefinition definition) {
        DEFINITIONS.put(definition.getEntityKey(), definition);
        DEFINITIONS.put(routeKey(definition.getModuleKey(), definition.getPageKey()), definition);
    }
    private static String routeKey(String module, String page) { return "route:" + module + "/" + page; }
    private static boolean same(String one, String two) { return one == null || one.length() == 0 || one.equals(two); }

    private static GenericCrudDefinition buildAgama() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(Agama.class);
        d.setModuleKey("root");
        d.setPageKey("agama");
        d.setDisplayName("Agama");
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(true);
        d.setImportDeleteEnabled(true);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setSavedViewEnabled(true);
        d.setAuditEnabled(true);
        d.setRowAuditEnabled(true);
        d.setGlobalAuditEnabled(false);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("nama");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        AgamaGenericCrudAdapter adapter = new AgamaGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(field("kode", "Kode", String.class, "text", true, true, true, false, true, true, 20));
        d.addField(field("nama", "Nama", String.class, "text", true, true, true, true, true, true, 30));
        d.addField(field("keterangan", "Keterangan", String.class, "textarea", true, true, true, false, true, true, 40));
        d.addField(field("aktif", "Aktif", Boolean.class, "checkbox", true, true, true, false, true, false, 50));
        d.addField(field("feeder", "Feeder", Long.class, "number", true, true, true, false, true, false, 60));
        return d;
    }

    private static GenericCrudDefinition buildMahasiswaParity() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(Mahasiswa.class);
        d.setModuleKey("root");
        d.setPageKey("mahasiswa");
        d.setDisplayName("Mahasiswa");
        d.setLifecycleStatus(GenericCrudDefinition.REVIEW_REQUIRED);
        d.setEnabled(false);
        d.setFormMode(GenericCrudFormOverrideProvider.MODE_FULL_PAGE_TABS);
        d.setFormOverrideProvider(new MahasiswaGenericCrudFormProvider());
        return d;
    }

    private static GenericCrudFieldDefinition field(String key, String label, Class type, String editor,
            boolean table, boolean create, boolean update, boolean required, boolean sortable,
            boolean searchable, int position) {
        GenericCrudFieldDefinition f = new GenericCrudFieldDefinition(key, label, type.getName());
        f.setEditorType(editor);
        f.setTableVisible(table);
        f.setQuickFilter(searchable);
        f.setCreateable(create);
        f.setUpdateable(update);
        f.setRequired(required);
        f.setSortable(sortable);
        f.setSearchable(searchable);
        f.setPosition(position);
        return f;
    }
}
