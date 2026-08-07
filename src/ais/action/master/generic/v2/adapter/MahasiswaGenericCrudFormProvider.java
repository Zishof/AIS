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
        definition.setSaveStrategy("ROOT_FIRST");
        List tabs = new ArrayList();
        tabs.add(tab("data_mahasiswa", "Data Mahasiswa", false, false, false));
        tabs.add(tab("pindahan", "Keterangan Mahasiswa Pindahan", false, false, false));
        tabs.add(tab("alih_prodi", "Keterangan Mahasiswa Alih Prodi", false, false, false));
        tabs.add(tab("biodata", "Biodata Lengkap", true, true, true));
        tabs.add(tab("beasiswa", "Beasiswa", true, true, true));
        tabs.add(tab("cuti", "Cuti", true, true, true));
        tabs.add(tab("kelulusan", "Informasi Kelulusan", false, true, false));
        tabs.add(tab("alumni", "Informasi Alumni", true, true, true));
        tabs.add(tab("login_orang_tua", "Login Orang Tua", false, true, false));
        tabs.add(tab("audit", "Audit & Riwayat", true, true, false));
        definition.setTabs(tabs);
        List actions = new ArrayList();
        actions.add(action("download_password", "Download Password", "READ", "MULTIPLE"));
        actions.add(action("upload_password", "Upload Password", "UPDATE", "NONE"));
        actions.add(action("upload_rfid", "Upload RFID", "UPDATE", "NONE"));
        actions.add(action("download_rfid", "Download RFID", "READ", "ALL_FILTERED"));
        actions.add(action("sync_status", "Sinkronisasi Status", "UPDATE", "MULTIPLE"));
        actions.add(action("feeder_send_or_sync", "Sinkronisasi Feeder", "UPDATE", "MULTIPLE"));
        actions.add(action("download_photo_massal", "Download Foto Massal", "READ", "ALL_FILTERED"));
        actions.add(action("upload_photo_massal", "Upload Foto Massal", "UPDATE", "NONE"));
        actions.add(action("kartu_mahasiswa", "Kartu Mahasiswa", "READ", "SINGLE"));
        actions.add(action("lihat_khs", "KHS", "READ", "SINGLE"));
        actions.add(action("lihat_transkrip", "Transkrip Akademik", "READ", "SINGLE"));
        definition.setActions(actions);
        return definition;
    }

    private Map tab(String key, String label, boolean lazy, boolean persisted, boolean saveBeforeEnter) {
        Map tab = new LinkedHashMap();
        tab.put("key", key);
        tab.put("label", label);
        tab.put("lazyLoad", Boolean.valueOf(lazy));
        tab.put("requiresPersistedEntity", Boolean.valueOf(persisted));
        tab.put("saveBeforeEnter", Boolean.valueOf(saveBeforeEnter));
        if ("pindahan".equals(key)) tab.put("visibleWhen", "merupakanPindahan == true");
        if ("alih_prodi".equals(key)) tab.put("visibleWhen", "merupakanAlihProdi == true");
        return tab;
    }

    private Map action(String key, String label, String privilege, String selection) {
        Map value = new LinkedHashMap(); value.put("actionKey", key); value.put("label", label);
        value.put("requiredPrivilege", privilege); value.put("selectionMode", selection); value.put("enabled", Boolean.FALSE); return value;
    }

    public Map loadTab(String key, GenericCrudRequestContext context, Object entity) { return new LinkedHashMap(); }
    public Map validateTab(String key, Map values, GenericCrudRequestContext context, Object entity) { return new LinkedHashMap(); }
    public GenericCrudResult saveTab(String key, Map values, GenericCrudRequestContext context, Object entity, Object token) {
        return GenericCrudResult.error("PARITY_REVIEW_REQUIRED", "Form Mahasiswa masih mode parity/read-only.");
    }
    public List getFormActions(GenericCrudRequestContext context, Object entity) { return new ArrayList(); }
}
