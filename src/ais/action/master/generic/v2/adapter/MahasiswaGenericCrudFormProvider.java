package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;

/**
 * Kontrak parity Mahasiswa. Fungsi yang belum native New UI selalu ditampilkan
 * sebagai bridge eksplisit ke modul ZKOSS asli; tidak boleh hilang diam-diam.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MahasiswaGenericCrudFormProvider implements GenericCrudFormOverrideProvider {
    public static final String SOURCE_ACTION = "ais.action.master.MahasiswaAction";
    public static final String SOURCE_ZUL = "/WEB-INF/z/x/y/pages/master/mahasiswa.zul";
    public static final String LEGACY_ROUTE = "/pages/master/mahasiswa.zul";

    public String getMode(GenericCrudRequestContext context) { return MODE_FULL_PAGE_TABS; }

    public GenericCrudFormDefinition getDefinition(GenericCrudRequestContext context, Object entity,
            boolean createMode) {
        GenericCrudFormDefinition definition = new GenericCrudFormDefinition();
        definition.setFormKey("mahasiswa-zkoss-parity-v2");
        definition.setMode(MODE_FULL_PAGE_TABS);
        definition.setTitle("Mahasiswa");
        definition.setSaveStrategy("ROOT_FIRST");
        definition.setTabs(formTabs());
        definition.setSections(moduleSections());
        definition.setActions(actions(context));
        return definition;
    }

    private List formTabs() {
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
        return tabs;
    }

    private List moduleSections() {
        List sections = new ArrayList();
        sections.add(section("mahasiswa", "Mahasiswa"));
        sections.add(section("pendukung", "Pendukung"));
        sections.add(section("prestasi", "Prestasi"));
        sections.add(section("data", "Data"));
        sections.add(section("statistik", "Statistik"));
        return sections;
    }

    private List actions(GenericCrudRequestContext context) {
        List actions = new ArrayList();
        nativeAdd(actions, context, "list_native", "Daftar, pencarian, filter, dan paging",
                "Mahasiswa", "READ", "onSearchDefault,loadData");
        nativeAdd(actions, context, "create_native", "Tambah mahasiswa", "Mahasiswa", "CREATE", "onAdd,onSave");
        nativeAdd(actions, context, "update_native", "Ubah mahasiswa", "Mahasiswa", "UPDATE", "onAdd,onSave");
        nativeAdd(actions, context, "deactivate_native", "Nonaktifkan mahasiswa", "Mahasiswa", "DELETE", "delete");
        nativeAdd(actions, context, "export_native", "Ekspor XLSX/PDF/DOCX/PPTX",
                "Data", "READ", "GenericCrudExportService,GenericCrudDocumentExportService");
        nativeAdd(actions, context, "audit_native", "Riwayat perubahan per mahasiswa",
                "Data", "READ", "GenericCrudAuditService");

        bridge(actions, context, "upload_photo_massal", "Upload Foto (NIM/ZIP)", "Mahasiswa", "UPDATE", "onUploadFotoMassal");
        bridge(actions, context, "download_photo_massal", "Download Foto Massal", "Mahasiswa", "READ", "onDownloadFotoMassal");
        bridge(actions, context, "help", "Bantuan Mahasiswa", "Mahasiswa", "READ", "BantuanHelper.tampilkanDariResource");

        bridge(actions, context, "kelas", "Kelas", "Pendukung", "READ", "onManajemenKelas");
        bridge(actions, context, "perkuliahan", "Perkuliahan", "Pendukung", "READ", "onPerkuliahanMahasiswa");
        bridge(actions, context, "asrama", "Asrama", "Pendukung", "READ", "onManajemenAsrama");
        bridge(actions, context, "kelompok", "Kelompok", "Pendukung", "READ", "onManajemenKelompok");
        bridge(actions, context, "status", "Status", "Pendukung", "READ", "onManajemenKelompokStatus");
        bridge(actions, context, "program", "Program", "Pendukung", "READ", "onManajemenProgram");
        bridge(actions, context, "status_keluar", "Status Keluar", "Pendukung", "READ", "onManajemenKelompokStatusKeluar");
        bridge(actions, context, "dosen_pa", "Dosen PA", "Pendukung", "READ", "onManajemenDosenPA");
        bridge(actions, context, "form_tambahan", "Form Tambahan", "Pendukung", "READ", "onFormTambahan");
        bridge(actions, context, "kartu", "Kartu Mahasiswa", "Pendukung", "READ", "onKartuMahasiswa");
        bridge(actions, context, "operator_seluler", "Operator Seluler", "Pendukung", "READ", "onOperatorSeluler");
        bridge(actions, context, "alat_transport", "Alat Transport", "Pendukung", "READ", "onAlatTransport");
        bridge(actions, context, "jenis_tinggal", "Jenis Tinggal", "Pendukung", "READ", "onJenisTinggal");
        bridge(actions, context, "pekerjaan_ortu", "Pekerjaan Orang Tua", "Pendukung", "READ", "onPekerjaanOrtu");
        bridge(actions, context, "penghasilan_ortu", "Penghasilan Orang Tua", "Pendukung", "READ", "onPenghasilanOrtu");
        bridge(actions, context, "pendidikan_ortu", "Pendidikan Orang Tua", "Pendukung", "READ", "onPendidikanOrtu");

        bridge(actions, context, "khs", "KHS", "Prestasi", "READ", "onTampilKHS");
        bridge(actions, context, "transkrip", "Transkrip Akademik", "Prestasi", "READ", "onTampilTranskripAkademik");
        bridge(actions, context, "prestasi_belajar", "Prestasi Belajar", "Prestasi", "READ", "onTampilPrestasi");
        bridge(actions, context, "kegiatan_kemahasiswaan", "Kegiatan Kemahasiswaan", "Prestasi", "READ", "onKegiatanKemahasiswaan");

        bridge(actions, context, "rekap", "Rekap Mahasiswa", "Data", "READ", "onRekapJumlahMahasiswa");
        bridge(actions, context, "formulir", "Formulir Mahasiswa", "Data", "READ", "onKegiatanMahasiswa");
        bridge(actions, context, "surat", "Surat Mahasiswa", "Data", "READ", "onSuratMahasiswa");
        bridge(actions, context, "album", "Album Mahasiswa", "Data", "READ", "onAlbumMahasiswa");
        bridge(actions, context, "export_legacy", "Ekspor Data Lengkap", "Data", "READ", "onDataMahasiswa");
        bridge(actions, context, "statistik", "Statistik", "Statistik", "READ", "onStatistik");

        bridge(actions, context, "download_lampiran", "Download Lampiran", "Operasi Lanjutan", "READ", "onDownloadLampiran");
        bridge(actions, context, "upload_password", "Upload Password", "Operasi Lanjutan", "UPDATE", "onUploadPassword");
        bridge(actions, context, "download_password", "Download Password", "Operasi Lanjutan", "READ", "onDownloadPassword");
        bridge(actions, context, "upload_ukt", "Upload UKT", "Operasi Lanjutan", "UPDATE", "onUploadUKT");
        bridge(actions, context, "upload_status", "Upload Status", "Operasi Lanjutan", "UPDATE", "onUploadStatus");
        bridge(actions, context, "upload_rfid", "Upload RFID", "Operasi Lanjutan", "UPDATE", "onUploadRfid");
        bridge(actions, context, "download_rfid", "Download RFID", "Operasi Lanjutan", "READ", "onDownloadRfid");
        bridge(actions, context, "sync_status", "Sinkronisasi Status", "Operasi Lanjutan", "UPDATE", "onSynchronizeStatus");
        bridge(actions, context, "import_data", "Import Data Mahasiswa", "Operasi Lanjutan", "CREATE", "onImport,uploadDataMahasiswa");
        bridge(actions, context, "feeder", "Kirim/Sinkronisasi Feeder", "Operasi Lanjutan", "UPDATE", "exportKeFeeder,ambilPerkuliahanDariFeeder");
        bridge(actions, context, "ojs", "Export ke OJS", "Operasi Lanjutan", "UPDATE", "updateUser");
        return actions;
    }

    private Map tab(String key, String label, boolean lazy, boolean persisted, boolean saveBeforeEnter) {
        Map tab = new LinkedHashMap();
        tab.put("key", key);
        tab.put("label", label);
        tab.put("lazyLoad", Boolean.valueOf(lazy));
        tab.put("requiresPersistedEntity", Boolean.valueOf(persisted));
        tab.put("saveBeforeEnter", Boolean.valueOf(saveBeforeEnter));
        tab.put("sourceAction", SOURCE_ACTION);
        if ("pindahan".equals(key)) tab.put("visibleWhen", "merupakanPindahan == true");
        if ("alih_prodi".equals(key)) tab.put("visibleWhen", "merupakanAlihProdi == true");
        return tab;
    }

    private Map section(String key, String label) {
        Map value = new LinkedHashMap();
        value.put("key", key);
        value.put("label", label);
        return value;
    }

    private void nativeAdd(List actions, GenericCrudRequestContext context, String key, String label,
            String group, String privilege, String sourceHandler) {
        if (!allowed(context, privilege)) return;
        Map value = action(key, label, group, privilege, sourceHandler);
        value.put("implementationStatus", "NEW_UI_NATIVE");
        value.put("enabled", Boolean.TRUE);
        actions.add(value);
    }

    private void bridge(List actions, GenericCrudRequestContext context, String key, String label,
            String group, String privilege, String sourceHandler) {
        if (!allowed(context, privilege)) return;
        Map value = action(key, label, group, privilege, sourceHandler);
        value.put("implementationStatus", "SAFE_LEGACY_BRIDGE");
        value.put("legacyRoute", contextPath(context) + LEGACY_ROUTE);
        value.put("enabled", Boolean.TRUE);
        actions.add(value);
    }

    private boolean allowed(GenericCrudRequestContext context, String privilege) {
        if (context == null) return true;
        if ("READ".equals(privilege)) return context.isCanRead();
        if ("CREATE".equals(privilege)) return context.isCanCreate();
        if ("UPDATE".equals(privilege)) return context.isCanUpdate();
        if ("DELETE".equals(privilege)) return context.isCanDelete();
        if ("APPROVE".equals(privilege)) return context.isCanApprove();
        if ("REJECT".equals(privilege)) return context.isCanReject();
        return false;
    }

    private Map action(String key, String label, String group, String privilege, String sourceHandler) {
        Map value = new LinkedHashMap();
        value.put("actionKey", key);
        value.put("label", label);
        value.put("group", group);
        value.put("requiredPrivilege", privilege);
        value.put("selectionMode", "NONE");
        value.put("sourceAction", SOURCE_ACTION);
        value.put("sourceZul", SOURCE_ZUL);
        value.put("sourceHandler", sourceHandler);
        return value;
    }

    private String contextPath(GenericCrudRequestContext context) {
        if (context == null || context.getRequest() == null) return "";
        String value = context.getRequest().getContextPath();
        return value == null ? "" : value;
    }

    public Map loadTab(String key, GenericCrudRequestContext context, Object entity) {
        Map result = new LinkedHashMap();
        result.put("implementationStatus", "SAFE_LEGACY_BRIDGE");
        result.put("legacyRoute", contextPath(context) + LEGACY_ROUTE);
        return result;
    }

    public Map validateTab(String key, Map values, GenericCrudRequestContext context, Object entity) {
        return new LinkedHashMap();
    }

    public GenericCrudResult saveTab(String key, Map values, GenericCrudRequestContext context,
            Object entity, Object token) {
        return GenericCrudResult.error("LEGACY_BRIDGE_REQUIRED",
                "Tab ini masih memakai business flow MahasiswaAction dan dibuka melalui bridge ZKOSS aman.");
    }

    public List getFormActions(GenericCrudRequestContext context, Object entity) {
        return actions(context);
    }
}
