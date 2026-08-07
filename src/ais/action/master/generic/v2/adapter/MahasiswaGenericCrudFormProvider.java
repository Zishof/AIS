package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/** Metadata parity complex form Mahasiswa. Mutasi tetap disabled sampai adapter domain direview. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MahasiswaGenericCrudFormProvider implements GenericCrudFormOverrideProvider {
    public String getMode(GenericCrudRequestContext context) { return MODE_FULL_PAGE_TABS; }

    public GenericCrudFormDefinition getDefinition(GenericCrudRequestContext context, Object entity, boolean createMode) {
        GenericCrudFormDefinition definition = new GenericCrudFormDefinition();
        definition.setFormKey("mahasiswa-full-tabs-v1");
        definition.setMode(MODE_FULL_PAGE_TABS);
        definition.setTitle("Mahasiswa");
        definition.setSaveStrategy("LEGACY_ACTION_BRIDGE");
        List tabs = new ArrayList();
        tabs.add(tab("identitas", "Identitas", false));
        tabs.add(tab("akademik", "Akademik", true));
        tabs.add(tab("alamat", "Alamat & Kontak", true));
        tabs.add(tab("orang_tua", "Orang Tua/Wali", true));
        tabs.add(tab("dokumen", "Dokumen & Foto", true));
        tabs.add(tab("riwayat", "Riwayat", true));
        definition.setTabs(tabs);
        return definition;
    }

    private Map tab(String key, String label, boolean persisted) {
        Map tab = new LinkedHashMap();
        tab.put("key", key);
        tab.put("label", label);
        tab.put("lazyLoad", Boolean.valueOf(persisted));
        tab.put("requiresPersistedEntity", Boolean.valueOf(persisted));
        return tab;
    }

    public Map loadTab(String key, GenericCrudRequestContext context, Object entity) { return new LinkedHashMap(); }
    public Map validateTab(String key, Map values, GenericCrudRequestContext context, Object entity) { return new LinkedHashMap(); }
    public GenericCrudResult saveTab(String key, Map values, GenericCrudRequestContext context, Object entity, Object token) {
        return GenericCrudResult.error("PARITY_REVIEW_REQUIRED", "Form Mahasiswa masih mode parity/read-only.");
    }
    public List getFormActions(GenericCrudRequestContext context, Object entity) { return new ArrayList(); }
}
