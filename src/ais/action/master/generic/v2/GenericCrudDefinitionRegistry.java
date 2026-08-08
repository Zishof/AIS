package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.adapter.AgamaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudFormOverrideProvider;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudAdapter;
import ais.database.model.Agama;
import ais.database.model.Jenjang;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;

/**
 * Registry allow-list. Scanner menghasilkan kandidat disabled; hanya entity yang
 * direview dan ditulis eksplisit di sini yang dapat dieksekusi.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudDefinitionRegistry {
    private static final Map DEFINITIONS = new LinkedHashMap();
    static {
        register(buildAgama());
        register(buildMahasiswa());
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

    /**
     * Bridge untuk JSP scaffold hasil generator. Kandidat berasal dari atribut
     * JSP server-side, bukan parameter HTTP. Definisi hasil auto tetap dibatasi
     * Super Admin oleh scope adapter dan class sensitif ditolak factory.
     */
    public static synchronized GenericCrudDefinition tryAutoRegister(String module, String page,
            String[] serverCandidates) {
        if (module == null || page == null || serverCandidates == null || serverCandidates.length == 0) return null;
        GenericCrudDefinition existing = (GenericCrudDefinition) DEFINITIONS.get(routeKey(module, page));
        if (existing != null) return existing.isEnabled() ? existing : null;
        try {
            GenericCrudDefinition generated = GenericCrudAutoDefinitionFactory.build(module, page, serverCandidates);
            if (generated == null) return null;
            register(generated);
            return generated;
        } catch (Exception rejected) {
            try { ais.common.Common.tampilErrorJikaAdmin(rejected); } catch (Exception ignored) { }
            return null;
        }
    }

    public static synchronized GenericCrudDefinition tryAdministrativeRegister(String module, String page,
            String mappedEntityKey) {
        if (module == null || page == null || mappedEntityKey == null) return null;
        String definitionKey = "admin:" + mappedEntityKey;
        GenericCrudDefinition existing = (GenericCrudDefinition) DEFINITIONS.get(definitionKey);
        if (existing != null) return existing;
        try {
            GenericCrudDefinition generated = GenericCrudAutoDefinitionFactory.buildAdministrative(module, page, mappedEntityKey);
            if (generated == null) return null;
            register(generated);
            return generated;
        } catch (Exception rejected) {
            try { ais.common.Common.tampilErrorJikaAdmin(rejected); } catch (Exception ignored) { }
            return null;
        }
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

    private static GenericCrudDefinition buildMahasiswa() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(Mahasiswa.class);
        d.setModuleKey("root");
        d.setPageKey("mahasiswa");
        d.setDisplayName("Mahasiswa");
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(false);
        d.setImportDeleteEnabled(false);
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
        d.setFormMode(GenericCrudFormOverrideProvider.MODE_GENERIC_DRAWER);
        MahasiswaGenericCrudAdapter adapter = new MahasiswaGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);

        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(field("nim", "NIM", String.class, "text", true, true, true, true, true, true, 20));
        d.addField(field("nama", "Nama", String.class, "text", true, true, true, true, true, true, 30));
        d.addField(relationField("jurusan", "Program Studi", Jurusan.class, true, true, true, true, 40));
        d.addField(relationField("jenjang", "Jenjang", Jenjang.class, true, false, false, false, 50));
        d.addField(field("tahunangkatan", "Tahun Angkatan", Integer.class, "number", true, true, true, false, true, false, 60));
        d.addField(field("program", "Program", String.class, "text", true, true, true, false, true, true, 70));
        d.addField(choiceField("semesterMulai", "Semester Mulai", new String[]{"Ganjil", "Genap"}, true, true, true, 80));
        d.addField(choiceField("kelamin", "Jenis Kelamin", new String[]{"Laki-laki", "Perempuan"}, true, true, true, 90));
        d.addField(field("tanggalMasuk", "Tanggal Masuk", java.util.Date.class, "date", true, true, true, false, true, false, 100));
        d.addField(field("tempatlahir", "Tempat Lahir", String.class, "text", false, true, true, false, true, true, 110));
        d.addField(field("tanggallahir", "Tanggal Lahir", java.util.Date.class, "date", false, true, true, false, true, false, 120));
        d.addField(field("email", "Email", String.class, "text", false, true, true, false, true, true, 130));
        d.addField(field("telp", "Telepon", String.class, "text", false, true, true, false, true, true, 140));
        d.addField(field("alamat", "Alamat", String.class, "textarea", false, true, true, false, false, true, 150));
        d.addField(relationField("agama", "Agama", Agama.class, false, true, true, false, 160));
        d.addField(relationField("statusAwalMahasiswa", "Status Awal", StatusAwalMahasiswa.class, false, true, true, false, 170));
        d.addField(choiceField("warganegara", "Kewarganegaraan", new String[]{"WNI", "WNA"}, false, true, true, 180));
        d.addField(field("waktuKuliah", "Waktu Kuliah", String.class, "text", false, true, true, false, true, true, 190));
        d.addField(field("keterangan", "Keterangan", String.class, "textarea", false, true, true, false, false, true, 200));
        d.addField(field("aktif", "Aktif", Boolean.class, "checkbox", true, true, true, false, true, false, 210));
        return d;
    }

    private static GenericCrudFieldDefinition relationField(String key, String label, Class type,
            boolean table, boolean create, boolean update, boolean required, int position) {
        GenericCrudFieldDefinition f = field(key, label, type, "relation", table, create, update,
                required, false, false, position);
        f.setRelationEntityKey(type.getName());
        f.setRelationDisplayProperty("nama");
        f.setRelationSearchProperties("kode,nama,nim");
        return f;
    }

    private static GenericCrudFieldDefinition choiceField(String key, String label, String[] values,
            boolean table, boolean create, boolean update, int position) {
        GenericCrudFieldDefinition f = field(key, label, String.class, "select", table, create, update,
                false, true, false, position);
        f.setEnumValues(values);
        return f;
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
