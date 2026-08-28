package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.DaftarUlangCalonMahasiswaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.PembayaranCalonSiswaGenericCrudAdapter;
import ais.database.model.Kegiatan;
import ais.database.model.sekolah.PembayaranSiswa;

/**
 * Verifikasi definisi manual read-only untuk menu calon (pembayaran calon
 * siswa 8755593 dan daftar ulang calon mahasiswa 1003000017): terkunci
 * fail-closed, memakai adapter berfilter calon, dan entityKey berprefiks
 * "calon:" supaya tidak menimpa definisi auto entity yang sama.
 */
@SuppressWarnings("rawtypes")
public final class CalonReadOnlyDefinitionSelfTest {
    private CalonReadOnlyDefinitionSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition pembayaran = find("sekolah", "pembayaran_calon_siswa");
        check(pembayaran != null, "Definisi pembayaran calon siswa belum terdaftar.");
        check(pembayaran.getEntityClass() == PembayaranSiswa.class, "Entity pembayaran calon siswa salah.");
        check(("calon:" + PembayaranSiswa.class.getName()).equals(pembayaran.getEntityKey()),
                "EntityKey pembayaran calon siswa harus berprefiks calon: agar tidak menimpa definisi auto.");
        check(GenericCrudDefinition.READ_ONLY.equals(pembayaran.getLifecycleStatus())
                && !pembayaran.isCreateEnabled() && !pembayaran.isUpdateEnabled()
                && !pembayaran.isDeleteEnabled() && !pembayaran.isImportEnabled(),
                "Pembayaran calon siswa wajib read-only fail-closed.");
        check(pembayaran.getAdapter() instanceof PembayaranCalonSiswaGenericCrudAdapter,
                "Adapter filter calonSiswa belum terpasang.");
        check(pembayaran.getField("calonSiswa") != null && pembayaran.getField("nominal") != null
                && pembayaran.getField("tanggalBayar") != null,
                "Kolom inti pembayaran calon siswa belum lengkap.");
        checkAllFieldsLocked(pembayaran, "pembayaran calon siswa");

        GenericCrudDefinition daftarUlang = find("root", "daftar_ulang_calon_mahasiswa");
        check(daftarUlang != null, "Definisi daftar ulang calon mahasiswa belum terdaftar.");
        check(daftarUlang.getEntityClass() == Kegiatan.class, "Entity daftar ulang calon salah.");
        check(("calon:" + Kegiatan.class.getName()).equals(daftarUlang.getEntityKey()),
                "EntityKey daftar ulang calon harus berprefiks calon:.");
        check(GenericCrudDefinition.READ_ONLY.equals(daftarUlang.getLifecycleStatus())
                && !daftarUlang.isCreateEnabled() && !daftarUlang.isUpdateEnabled()
                && !daftarUlang.isDeleteEnabled() && !daftarUlang.isImportEnabled(),
                "Daftar ulang calon wajib read-only fail-closed.");
        check(daftarUlang.getAdapter() instanceof DaftarUlangCalonMahasiswaGenericCrudAdapter,
                "Adapter filter calonMahasiswa belum terpasang.");
        check(daftarUlang.getField("calonMahasiswa") != null
                && daftarUlang.getField("amountTerhutang") != null,
                "Kolom inti daftar ulang calon belum lengkap.");
        checkAllFieldsLocked(daftarUlang, "daftar ulang calon");

        System.out.println("PASS Calon read-only definition self-test");
    }

    private static GenericCrudDefinition find(String module, String page) {
        Iterator definitions = GenericCrudDefinitionRegistry.listDefinitions().iterator();
        while (definitions.hasNext()) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.next();
            if (module.equals(candidate.getModuleKey()) && page.equals(candidate.getPageKey())) return candidate;
        }
        return null;
    }

    private static void checkAllFieldsLocked(GenericCrudDefinition definition, String label) {
        Iterator fields = definition.getFields().iterator();
        while (fields.hasNext()) {
            ais.action.master.generic.v2.GenericCrudFieldDefinition field =
                    (ais.action.master.generic.v2.GenericCrudFieldDefinition) fields.next();
            check(!field.isCreateable() && !field.isUpdateable(),
                    "Field " + field.getProperty() + " pada " + label + " tidak boleh mutable.");
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
