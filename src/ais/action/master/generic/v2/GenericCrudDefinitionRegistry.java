package ais.action.master.generic.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.adapter.AgamaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.BadanHukumGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudFormOverrideProvider;
import ais.action.master.generic.v2.adapter.JenjangProgramStudiGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.KeluargaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudFormProvider;
import ais.action.master.generic.v2.adapter.PenilaianSiswaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.PegawaiHistoryGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.ParameterUmumGenericCrudAdapter;
import ais.database.model.Agama;
import ais.database.model.BadanHukum;
import ais.database.model.Jenjang;
import ais.database.model.JenjangProgramStudi;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.ParameterUmum;
import ais.database.model.Ruang;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.KurikulumSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.RiwayatTandaJasaPegawai;
import ais.database.model.employ.RiwayatPendidikanPegawai;
import ais.database.model.employ.RiwayatPelatihanPegawai;
import ais.database.model.employ.RiwayatOrganisasiSekolahPegawai;
import ais.database.model.employ.RiwayatOrganisasiKampusPegawai;
import ais.database.model.employ.RiwayatOrganisasiLainPegawai;
import ais.database.model.employ.RiwayatKeteranganLainPegawai;
import ais.database.model.employ.RiwayatKerjaPegawai;
import ais.database.model.employ.RiwayatKeluarNegeriPegawai;
import ais.database.model.employ.RiwayatKartuIdentitasPegawai;
import ais.database.model.payroll.AsuransiPegawai;

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
        register(buildJenjangProgramStudi());
        register(buildPenilaianSiswa());
        register(buildBadanHukum());
        register(buildKeluarga());
        register(buildParameterUmum());
        register(buildEmployeeHistory(RiwayatTandaJasaPegawai.class, "riwayat_tanda_jasa_pegawai", "Riwayat Tanda Jasa Pegawai"));
        register(buildEmployeeHistory(RiwayatPendidikanPegawai.class, "riwayat_pendidikan_pegawai", "Riwayat Pendidikan Pegawai"));
        register(buildEmployeeHistory(RiwayatPelatihanPegawai.class, "riwayat_pelatihan_pegawai", "Riwayat Pelatihan Pegawai"));
        register(buildEmployeeHistory(RiwayatOrganisasiSekolahPegawai.class, "riwayat_organisasi_sekolah_pegawai", "Riwayat Organisasi Sekolah Pegawai"));
        register(buildEmployeeHistory(RiwayatOrganisasiKampusPegawai.class, "riwayat_organisasi_kampus_pegawai", "Riwayat Organisasi Kampus Pegawai"));
        register(buildEmployeeHistory(RiwayatOrganisasiLainPegawai.class, "riwayat_organisasi_lain_pegawai", "Riwayat Organisasi Lain Pegawai"));
        register(buildEmployeeHistory(RiwayatKeteranganLainPegawai.class, "riwayat_keterangan_lain_pegawai", "Riwayat Keterangan Lain Pegawai"));
        register(buildEmployeeHistory(RiwayatKerjaPegawai.class, "riwayat_kerja_pegawai", "Riwayat Kerja Pegawai"));
        register(buildEmployeeHistory(RiwayatKeluarNegeriPegawai.class, "riwayat_keluar_negeri_pegawai", "Riwayat Keluar Negeri Pegawai"));
        register(buildEmployeeHistory(RiwayatKartuIdentitasPegawai.class, "riwayat_kartu_identitas_pegawai", "Riwayat Kartu Identitas Pegawai"));
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
        try {
            GenericCrudAutoDefinitionFactory.appendMissingMappedFields(definition);
        } catch (Exception metadataFailure) {
            throw new GenericCrudException(500, "FIELD_METADATA_FAILED",
                    "Metadata field entity tidak dapat dimuat.", metadataFailure);
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
     * JSP server-side, bukan parameter HTTP. Operasi mutasi diturunkan dari
     * metode Action existing; RBAC dan scope institusi ditegakkan adapter.
     */
    public static synchronized GenericCrudDefinition tryAutoRegister(String module, String page,
            String[] serverCandidates) {
        return tryAutoRegister(module, page, serverCandidates, null, null, null);
    }

    public static synchronized GenericCrudDefinition tryAutoRegister(String module, String page,
            String[] serverCandidates, String sourceAction, String[] sourceMethods) {
        return tryAutoRegister(module, page, serverCandidates, null, sourceAction, sourceMethods);
    }

    public static synchronized GenericCrudDefinition tryAutoRegister(String module, String page,
            String[] serverCandidates, String sourcePackage, String sourceAction, String[] sourceMethods) {
        if (module == null || page == null || serverCandidates == null || serverCandidates.length == 0) return null;
        GenericCrudDefinition existing = (GenericCrudDefinition) DEFINITIONS.get(routeKey(module, page));
        if (existing != null) return existing.isEnabled() ? existing : null;
        try {
            GenericCrudDefinition generated = GenericCrudAutoDefinitionFactory.build(module, page,
                    serverCandidates, sourcePackage, sourceAction, sourceMethods);
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
        d.setSourceActionClassName("ais.action.master.AgamaAction");
        d.setExistingActionLifecycleBound(true);
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
        d.setSourceActionClassName("ais.action.master.MahasiswaAction");
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
        d.setPhotoEnabled(true);
        d.setAuditEnabled(true);
        d.setRowAuditEnabled(true);
        d.setGlobalAuditEnabled(false);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("nama");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        d.setFormMode(GenericCrudFormOverrideProvider.MODE_FULL_PAGE_TABS);
        MahasiswaGenericCrudAdapter adapter = new MahasiswaGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.setFormOverrideProvider(new MahasiswaGenericCrudFormProvider());

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

    private static GenericCrudDefinition buildJenjangProgramStudi() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(JenjangProgramStudi.class);
        d.setModuleKey("root");
        d.setPageKey("jenjang_program_studi");
        d.setDisplayName("Jenjang Program Studi");
        d.setSourceActionClassName("ais.action.master.JenjangProgramStudiAction");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(false);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setAuditEnabled(false);
        d.setRowAuditEnabled(false);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("id");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        JenjangProgramStudiGenericCrudAdapter adapter = new JenjangProgramStudiGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(relationField("jurusan", "Jurusan / Program Studi", Jurusan.class,
                true, true, true, true, 20));
        d.addField(relationField("jenjang", "Jenjang", Jenjang.class,
                true, true, true, false, 30));
        d.addField(field("email", "Email", String.class, "text", true, true, true,
                false, true, true, 40));
        d.addField(field("telpPS", "Telepon Program Studi", String.class, "text", true, true, true,
                false, true, true, 50));
        d.addField(field("homepagePS", "Homepage Program Studi", String.class, "text", false, true, true,
                false, true, true, 60));
        return d;
    }

    private static GenericCrudDefinition buildPenilaianSiswa() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(KelasSiswa.class);
        d.setModuleKey("sekolah");
        d.setPageKey("penilaian_siswa");
        d.setDisplayName("Penilaian Siswa / Kelas");
        d.setSourceActionClassName("ais.action.master.sekolah.PenilaianSiswaAction");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(false);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setAuditEnabled(false);
        d.setRowAuditEnabled(false);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("nama");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        PenilaianSiswaGenericCrudAdapter adapter = new PenilaianSiswaGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(field("nama", "Nama Kelas", String.class, "text", true, true, true, true, true, true, 20));
        d.addField(relationField("yayasan", "Yayasan", Yayasan.class, true, true, true, true, 30));
        d.addField(relationField("sekolah", "Sekolah", Sekolah.class, true, true, true, true, 40));
        d.addField(relationField("ruang", "Ruang", Ruang.class, true, true, true, false, 50));
        d.addField(field("tingkat", "Tingkat", Integer.class, "number", true, true, true, true, true, false, 60));
        d.addField(field("tahunAjaran", "Tahun Ajaran", String.class, "text", true, true, true, true, true, true, 70));
        d.addField(relationField("kurikulumSekolah", "Kurikulum Sekolah", KurikulumSekolah.class,
                true, true, true, false, 80));
        d.addField(field("keterangan", "Keterangan", String.class, "textarea", true, true, true,
                false, true, true, 90));
        d.addField(field("aktif", "Aktif", Boolean.class, "checkbox", true, true, true,
                false, true, false, 100));
        return d;
    }

    private static GenericCrudDefinition buildBadanHukum() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(BadanHukum.class);
        d.setModuleKey("root");
        d.setPageKey("badan_hukum");
        d.setDisplayName("Badan Hukum");
        d.setSourceActionClassName("ais.action.master.BadanHukumAction");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(false);
        d.setImportEnabled(false);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setAuditEnabled(true);
        d.setRowAuditEnabled(true);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("id");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(10);
        BadanHukumGenericCrudAdapter adapter = new BadanHukumGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(field("kode", "Kode", String.class, "text", true, true, true, true, true, true, 20));
        d.addField(field("nama", "Nama", String.class, "text", true, true, true, false, true, true, 30));
        d.addField(field("alamat1", "Alamat 1", String.class, "textarea", false, true, true, false, false, true, 40));
        d.addField(field("alamat2", "Alamat 2", String.class, "textarea", false, true, true, false, false, true, 50));
        d.addField(field("kota", "Kota", String.class, "text", true, true, true, false, true, true, 60));
        d.addField(field("kodePos", "Kode Pos", String.class, "text", false, true, true, false, true, true, 70));
        d.addField(field("telepon", "Telepon", String.class, "text", false, true, true, false, true, true, 80));
        d.addField(field("faksimil", "Faksimil", String.class, "text", false, true, true, false, true, true, 90));
        d.addField(field("tanggalAkta", "Tanggal Akta", java.util.Date.class, "date", false, true, true, false, true, false, 100));
        d.addField(field("namaAkta", "Nama Akta", String.class, "text", false, true, true, false, true, true, 110));
        d.addField(field("tanggalPengesahan", "Tanggal Pengesahan", java.util.Date.class, "date", false, true, true, false, true, false, 120));
        d.addField(field("nomorPengesahan", "Nomor Pengesahan", String.class, "text", false, true, true, false, true, true, 130));
        d.addField(field("tanggalAwalPendirian", "Tanggal Awal Pendirian", java.util.Date.class, "date", false, true, true, false, true, false, 140));
        d.addField(field("email", "Email", String.class, "text", false, true, true, false, true, true, 150));
        d.addField(field("alamatWebsite", "Website", String.class, "text", false, true, true, false, true, true, 160));
        d.addField(field("logo", "Logo", String.class, "text", false, true, true, false, true, true, 170));
        return d;
    }

    private static GenericCrudDefinition buildKeluarga() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(Keluarga.class);
        d.setModuleKey("employ");
        d.setPageKey("keluarga");
        d.setDisplayName("Keluarga Pegawai");
        d.setSourceActionClassName("ais.action.master.employ.KeluargaAction");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(true);
        d.setImportRequiresApprove(true);
        d.setAttachmentEnabled(true);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setAuditEnabled(true);
        d.setRowAuditEnabled(true);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("hubungan");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        KeluargaGenericCrudAdapter adapter = new KeluargaGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(relationField("pegawai", "Pegawai", ais.database.model.Pegawai.class,
                true, true, false, false, 20));
        d.addField(choiceField("hubungan", "Hubungan", new String[] { Keluarga.SUAMI, Keluarga.ISTRI,
                Keluarga.ANAK, Keluarga.MERTUA, Keluarga.ORANG_TUA, Keluarga.SAUDARA }, true, true, true, 30));
        d.addField(field("tanggalNikah", "Tanggal Hubungan (Nikah)", java.util.Date.class, "date",
                false, true, true, false, true, false, 40));
        d.addField(field("nama", "Nama", String.class, "text", true, true, true, true, true, true, 50));
        d.addField(field("tempatLahir", "Tempat Lahir", String.class, "text", false, true, true, true, true, true, 60));
        d.addField(field("tanggalLahir", "Tanggal Lahir", java.util.Date.class, "date", true, true, true, true, true, false, 70));
        d.addField(choiceField("jenisKelamin", "Jenis Kelamin", new String[] { "Laki-laki", "Perempuan" }, true, true, true, 80));
        d.addField(field("alamat", "Alamat", String.class, "textarea", false, true, true, true, false, true, 90));
        d.addField(field("pekerjaan", "Pekerjaan", String.class, "text", true, true, true, true, true, true, 100));
        d.addField(relationField("asuransiPegawai1", "Asuransi", AsuransiPegawai.class,
                false, true, true, false, 110));
        d.addField(field("nomorAsuransiPegawai1", "Nomor Asuransi", String.class, "text",
                false, true, true, false, true, true, 120));
        d.addField(field("premiAsuransi1", "Premi Asuransi", Double.class, "number",
                false, true, true, false, true, false, 130));
        d.addField(field("keterangan", "Keterangan", String.class, "textarea",
                false, true, true, false, false, true, 140));
        d.addField(field("keteranganTambahan", "Keterangan Tambahan", String.class, "textarea",
                false, true, true, true, false, true, 150));
        d.addField(field("status", "Status Persetujuan", Boolean.class, "checkbox",
                true, false, false, false, true, false, 160));
        return d;
    }

    private static GenericCrudDefinition buildEmployeeHistory(Class entityClass, String page, String label) {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(entityClass);
        d.setModuleKey("employ");
        d.setPageKey(page);
        d.setDisplayName(label);
        d.setSourceActionClassName("ais.action.master.employ." + entityClass.getSimpleName() + "Action");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(true);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(true);
        d.setImportEnabled(true);
        d.setImportRequiresApprove(true);
        d.setAttachmentEnabled(true);
        d.setExportPdfEnabled(true);
        d.setExportDocxEnabled(true);
        d.setExportPptxEnabled(true);
        d.setAuditEnabled(true);
        d.setRowAuditEnabled(true);
        d.setRestoreEnabled(false);
        d.setAdminDeleteEnabled(false);
        d.setDefaultSortProperty("id");
        d.setDefaultPageSize(10);
        d.setMaxPageSize(100);
        PegawaiHistoryGenericCrudAdapter adapter = new PegawaiHistoryGenericCrudAdapter(entityClass);
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(relationField("pegawai", "Pegawai", ais.database.model.Pegawai.class,
                true, true, false, false, 20));
        d.addField(field("status", "Status Persetujuan", Boolean.class, "checkbox",
                true, false, false, false, true, false, 900));
        return d;
    }

    private static GenericCrudDefinition buildParameterUmum() {
        GenericCrudDefinition d = new GenericCrudDefinition();
        d.setEntityClass(ParameterUmum.class);
        d.setModuleKey("root");
        d.setPageKey("parameter_umum");
        d.setDisplayName("Parameter Umum");
        d.setSourceActionClassName("ais.action.master.ParameterUmumAction");
        d.setExistingActionLifecycleBound(false);
        d.setLifecycleStatus(GenericCrudDefinition.FULL_CRUD);
        d.setEnabled(true);
        d.setCreateEnabled(false);
        d.setUpdateEnabled(true);
        d.setDeleteEnabled(false);
        d.setImportEnabled(false);
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
        d.setDefaultPageSize(25);
        d.setMaxPageSize(100);
        ParameterUmumGenericCrudAdapter adapter = new ParameterUmumGenericCrudAdapter();
        d.setAdapter(adapter);
        d.setScopeAdapter(adapter);
        d.addField(field("id", "ID", Long.class, "number", true, false, false, false, true, false, 10));
        d.addField(field("nama", "Nama Parameter", String.class, "text", true, false, false, true, true, true, 20));
        d.addField(field("nilai", "Nilai", String.class, "textarea", true, false, true, false, false, false, 30));
        d.addField(field("keterangan", "Keterangan", String.class, "textarea", true, false, true, false, false, true, 40));
        d.addField(field("info1", "Info 1", String.class, "textarea", false, false, true, false, false, false, 50));
        d.addField(field("info2", "Info 2", String.class, "textarea", false, false, true, false, false, false, 60));
        d.addField(field("info3", "Info 3", String.class, "textarea", false, false, true, false, false, false, 70));
        d.addField(field("info4", "Info 4", String.class, "textarea", false, false, true, false, false, false, 80));
        d.addField(field("info5", "Info 5", String.class, "textarea", false, false, true, false, false, false, 90));
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
