package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.PenilaianSiswaGenericCrudAdapter;
import ais.database.model.sekolah.KelasSiswa;

/** Verifikasi binding New UI Penilaian Siswa ke entity KelasSiswa yang benar. */
@SuppressWarnings("rawtypes")
public final class PenilaianSiswaGenericCrudDefinitionSelfTest {
    private PenilaianSiswaGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition found = null;
        Iterator definitions = GenericCrudDefinitionRegistry.listDefinitions().iterator();
        while (definitions.hasNext()) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.next();
            if ("sekolah".equals(candidate.getModuleKey())
                    && "penilaian_siswa".equals(candidate.getPageKey())) found = candidate;
        }
        check(found != null, "Definisi Penilaian Siswa belum terdaftar.");
        check(found.getEntityClass() == KelasSiswa.class,
                "Penilaian Siswa masih salah mengarah ke entity selain KelasSiswa.");
        check(found.getAdapter() instanceof PenilaianSiswaGenericCrudAdapter,
                "Override adapter Penilaian Siswa belum aktif.");
        check(found.isCreateEnabled() && found.isUpdateEnabled() && found.isDeleteEnabled(),
                "CRUD Kelas Siswa belum lengkap.");
        check(found.getField("nama").isRequired() && found.getField("yayasan").isRequired()
                && found.getField("sekolah").isRequired(),
                "Validasi wajib dari PenilaianSiswaAction belum dipertahankan.");
        System.out.println("PASS Penilaian Siswa native CRUD definition self-test");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
