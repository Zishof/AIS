package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.GenericCrudApprovalAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudAttachmentAdapter;
import ais.action.master.generic.v2.adapter.PegawaiHistoryGenericCrudAdapter;

public final class PegawaiHistoryGenericCrudDefinitionSelfTest {
    private static final String[] PAGES = new String[] {
        "riwayat_tanda_jasa_pegawai", "riwayat_pendidikan_pegawai", "riwayat_pelatihan_pegawai",
        "riwayat_organisasi_sekolah_pegawai", "riwayat_organisasi_kampus_pegawai",
        "riwayat_organisasi_lain_pegawai", "riwayat_keterangan_lain_pegawai",
        "riwayat_kerja_pegawai", "riwayat_keluar_negeri_pegawai", "riwayat_kartu_identitas_pegawai"
    };

    private PegawaiHistoryGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) {
        int found = 0;
        for (Iterator values = GenericCrudDefinitionRegistry.listDefinitions().iterator(); values.hasNext();) {
            GenericCrudDefinition definition = (GenericCrudDefinition) values.next();
            if (!contains(PAGES, definition.getPageKey())) continue;
            found++;
            check("employ".equals(definition.getModuleKey()), "Module riwayat salah: " + definition.getPageKey());
            check(definition.isFullCrud() && definition.isCreateEnabled() && definition.isUpdateEnabled()
                    && definition.isDeleteEnabled(), "Lifecycle belum lengkap: " + definition.getPageKey());
            check(definition.getAdapter() instanceof PegawaiHistoryGenericCrudAdapter,
                    "Adapter salah: " + definition.getPageKey());
            check(definition.getAdapter() instanceof GenericCrudApprovalAdapter,
                    "Approval belum aktif: " + definition.getPageKey());
            check(definition.isAttachmentEnabled() && definition.getAdapter() instanceof GenericCrudAttachmentAdapter,
                    "Lampiran belum aktif: " + definition.getPageKey());
            check(definition.isImportEnabled() && definition.isImportRequiresApprove()
                    && !definition.isImportDeleteEnabled(), "Aturan import approval salah: " + definition.getPageKey());
            check(definition.getField("pegawai") != null && definition.getField("status") != null,
                    "Field scope/status belum ada: " + definition.getPageKey());
            check(!definition.getField("status").isCreateable() && !definition.getField("status").isUpdateable(),
                    "Approval bisa dibypass: " + definition.getPageKey());
        }
        check(found == PAGES.length, "Definisi riwayat ditemukan " + found + " dari " + PAGES.length);
        System.out.println("PASS PegawaiHistoryGenericCrudDefinitionSelfTest " + found + " definitions");
    }

    private static boolean contains(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return true;
        return false;
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
