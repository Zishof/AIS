package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudRequestContext;
import ais.action.master.generic.v2.GenericCrudResult;
import ais.common.newui.menu.NewUiNativeSubrouteRegistry;
import ais.common.Common;
import ais.database.model.Konfigurasi;

/**
 * Kontrak parity Mahasiswa native New UI. Seluruh tab dan action tetap
 * terinventarisasi tanpa iframe, redirect, atau ketergantungan tampilan lain.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class MahasiswaGenericCrudFormProvider implements GenericCrudFormOverrideProvider {
    public static final String SOURCE_ACTION = "ais.action.master.MahasiswaAction";

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

        nativePanel(actions, context, "upload_photo_massal", "Upload Foto (NIM/ZIP)", "Mahasiswa", "UPDATE", "onUploadFotoMassal");
        nativePanel(actions, context, "download_photo_massal", "Download Foto Massal", "Mahasiswa", "READ", "onDownloadFotoMassal");
        nativePanel(actions, context, "help", "Bantuan Mahasiswa", "Mahasiswa", "READ", "BantuanHelper.tampilkanDariResource");

        nativePanel(actions, context, "kelas", "Kelas", "Pendukung", "READ", "onManajemenKelas");
        nativePanel(actions, context, "perkuliahan", "Perkuliahan", "Pendukung", "READ", "onPerkuliahanMahasiswa");
        nativePanel(actions, context, "asrama", "Asrama", "Pendukung", "READ", "onManajemenAsrama");
        nativePanel(actions, context, "kelompok", "Kelompok", "Pendukung", "READ", "onManajemenKelompok");
        nativePanel(actions, context, "status", "Status", "Pendukung", "READ", "onManajemenKelompokStatus");
        nativePanel(actions, context, "program", "Program", "Pendukung", "READ", "onManajemenProgram");
        nativePanel(actions, context, "status_keluar", "Status Keluar", "Pendukung", "READ", "onManajemenKelompokStatusKeluar");
        nativePanel(actions, context, "dosen_pa", "Dosen PA", "Pendukung", "READ", "onManajemenDosenPA");
        nativePanel(actions, context, "form_tambahan", "Form Tambahan", "Pendukung", "READ", "onFormTambahan");
        nativePanel(actions, context, "kartu", "Kartu Mahasiswa", "Pendukung", "READ", "onKartuMahasiswa");
        nativePanel(actions, context, "operator_seluler", "Operator Seluler", "Pendukung", "READ", "onOperatorSeluler");
        nativePanel(actions, context, "alat_transport", "Alat Transport", "Pendukung", "READ", "onAlatTransport");
        nativePanel(actions, context, "jenis_tinggal", "Jenis Tinggal", "Pendukung", "READ", "onJenisTinggal");
        nativePanel(actions, context, "pekerjaan_ortu", "Pekerjaan Orang Tua", "Pendukung", "READ", "onPekerjaanOrtu");
        nativePanel(actions, context, "penghasilan_ortu", "Penghasilan Orang Tua", "Pendukung", "READ", "onPenghasilanOrtu");
        nativePanel(actions, context, "pendidikan_ortu", "Pendidikan Orang Tua", "Pendukung", "READ", "onPendidikanOrtu");

        nativePanel(actions, context, "khs", "KHS", "Prestasi", "READ", "onTampilKHS");
        nativePanel(actions, context, "transkrip", "Transkrip Akademik", "Prestasi", "READ", "onTampilTranskripAkademik");
        nativePanel(actions, context, "prestasi_belajar", "Prestasi Belajar", "Prestasi", "READ", "onTampilPrestasi");
        nativePanel(actions, context, "kegiatan_kemahasiswaan", "Kegiatan Kemahasiswaan", "Prestasi", "READ", "onKegiatanKemahasiswaan");

        nativePanel(actions, context, "rekap", "Rekap Mahasiswa", "Data", "READ", "onRekapJumlahMahasiswa");
        nativePanel(actions, context, "formulir", "Formulir Mahasiswa", "Data", "READ", "onKegiatanMahasiswa");
        nativePanel(actions, context, "surat", "Surat Mahasiswa", "Data", "READ", "onSuratMahasiswa");
        nativePanel(actions, context, "album", "Album Mahasiswa", "Data", "READ", "onAlbumMahasiswa");
        nativePanel(actions, context, "export_data_lengkap", "Ekspor Data Lengkap", "Data", "READ", "onDataMahasiswa");
        nativePanel(actions, context, "statistik", "Statistik", "Statistik", "READ", "onStatistik");

        nativePanel(actions, context, "download_lampiran", "Download Lampiran", "Operasi Lanjutan", "READ", "onDownloadLampiran");
        nativePanel(actions, context, "upload_password", "Upload Password", "Operasi Lanjutan", "UPDATE", "onUploadPassword");
        nativePanel(actions, context, "download_password", "Download Password", "Operasi Lanjutan", "READ", "onDownloadPassword");
        nativePanel(actions, context, "upload_ukt", "Upload UKT", "Operasi Lanjutan", "UPDATE", "onUploadUKT");
        nativePanel(actions, context, "upload_status", "Upload Status", "Operasi Lanjutan", "UPDATE", "onUploadStatus");
        nativePanel(actions, context, "upload_rfid", "Upload RFID", "Operasi Lanjutan", "UPDATE", "onUploadRfid");
        nativePanel(actions, context, "download_rfid", "Download RFID", "Operasi Lanjutan", "READ", "onDownloadRfid");
        nativePanel(actions, context, "sync_status", "Sinkronisasi Status", "Operasi Lanjutan", "UPDATE", "onSynchronizeStatus");
        nativePanel(actions, context, "import_data", "Import Data Mahasiswa", "Operasi Lanjutan", "CREATE", "onImport,uploadDataMahasiswa");
        nativePanel(actions, context, "feeder", "Kirim/Sinkronisasi Feeder", "Operasi Lanjutan", "UPDATE", "exportKeFeeder,ambilPerkuliahanDariFeeder");
        nativePanel(actions, context, "ojs", "Export ke OJS", "Operasi Lanjutan", "UPDATE", "updateUser");
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

    private void nativePanel(List actions, GenericCrudRequestContext context, String key, String label,
            String group, String privilege, String sourceHandler) {
        if (!allowed(context, privilege)) return;
        if (context != null && "upload_photo_massal".equals(key) && !context.isCanDelete()) return;
        if (context != null && "download_lampiran".equals(key) && !(context.isCanCreate() && context.isCanUpdate())) return;
        if (context != null && "ojs".equals(key)
                && !Common.bolehKonfigurasi("terhubung_ke_ojs", Konfigurasi.TIDAK_AKTIF)) return;
        if (context != null && "feeder".equals(key)
                && !(Common.getApakahAdminBolehAksesFeeder()
                && Common.bolehKonfigurasi("aktifkan_terhubung_langsung_ke_feeder"))) return;
        Map value = action(key, label, group, privilege, sourceHandler);
        value.put("nativePanelKey", key);
        if (NewUiNativeSubrouteRegistry.supportsMahasiswa(key)) {
            value.put("implementationStatus", "NEW_UI_NATIVE_ROUTE");
            value.put("nativeSubroute", key);
            value.put("enabled", Boolean.TRUE);
        } else {
            value.put("implementationStatus", "MIGRATION_REQUIRED");
            value.put("enabled", Boolean.FALSE);
        }
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
        value.put("sourceHandler", sourceHandler);
        return value;
    }

    public Map loadTab(String key, GenericCrudRequestContext context, Object entity) {
        Map result = new LinkedHashMap();
        result.put("implementationStatus", "NEW_UI_NATIVE_PANEL");
        result.put("tabKey", key);
        return result;
    }

    public Map validateTab(String key, Map values, GenericCrudRequestContext context, Object entity) {
        return new LinkedHashMap();
    }

    public GenericCrudResult saveTab(String key, Map values, GenericCrudRequestContext context,
            Object entity, Object token) {
        return GenericCrudResult.error("NATIVE_TAB_SAVE_NOT_REGISTERED",
                "Penyimpanan tab ini harus melalui service New UI yang terdaftar.");
    }

    public List getFormActions(GenericCrudRequestContext context, Object entity) {
        return actions(context);
    }
}
