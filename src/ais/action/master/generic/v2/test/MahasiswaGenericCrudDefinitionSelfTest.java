package ais.action.master.generic.v2.test;

import java.util.Iterator;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudRelationScopeAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudFormDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudFormOverrideProvider;
import ais.action.master.generic.v2.adapter.MahasiswaGenericCrudFormProvider;
import ais.common.newui.NewUiPermission;
import ais.common.newui.menu.NewUiHybridMenuNode;
import ais.common.newui.menu.NewUiHybridMenuRouteGuard;

/** Uji konfigurasi tanpa koneksi database untuk build legacy. */
@SuppressWarnings("rawtypes")
public final class MahasiswaGenericCrudDefinitionSelfTest {
    private MahasiswaGenericCrudDefinitionSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition mahasiswa = null;
        Iterator definitions = GenericCrudDefinitionRegistry.listDefinitions().iterator();
        while (definitions.hasNext()) {
            GenericCrudDefinition candidate = (GenericCrudDefinition) definitions.next();
            if ("ais.database.model.Mahasiswa".equals(candidate.getEntityKey())) mahasiswa = candidate;
        }
        check(mahasiswa != null, "Definisi Mahasiswa tidak ditemukan");
        check(mahasiswa.isEnabled() && mahasiswa.isFullCrud(), "CRUD Mahasiswa belum aktif");
        check(mahasiswa.isCreateEnabled() && mahasiswa.isUpdateEnabled() && mahasiswa.isDeleteEnabled(),
                "Operasi CUD Mahasiswa belum lengkap");
        check(mahasiswa.getAdapter() instanceof MahasiswaGenericCrudAdapter, "Adapter Mahasiswa tidak eksplisit");
        check(mahasiswa.getScopeAdapter() == mahasiswa.getAdapter(), "Scope Mahasiswa tidak terpasang");
        check(mahasiswa.getAdapter() instanceof GenericCrudRelationScopeAdapter,
                "Lookup relasi Mahasiswa belum mempunyai scope");
        check(GenericCrudFormOverrideProvider.MODE_FULL_PAGE_TABS.equals(mahasiswa.getFormMode()),
                "Form Mahasiswa belum memakai kontrak tab parity");
        check(mahasiswa.getFormOverrideProvider() instanceof MahasiswaGenericCrudFormProvider,
                "Provider parity Mahasiswa belum terpasang");
        check(mahasiswa.getField("nim").isRequired() && mahasiswa.getField("nama").isRequired()
                && mahasiswa.getField("jurusan").isRequired(), "Field wajib Mahasiswa tidak lengkap");
        check(mahasiswa.getField("pass") == null && mahasiswa.getField("token") == null,
                "Field sensitif terekspos");
        for (Iterator fields = mahasiswa.getFields().iterator(); fields.hasNext();) {
            GenericCrudFieldDefinition field = (GenericCrudFieldDefinition) fields.next();
            check(!field.isSensitive(), "Field sensitif masuk allow-list: " + field.getProperty());
        }
        assertParityContract(mahasiswa);
        assertActionGuards();
        System.out.println("PASS Mahasiswa Generic CRUD definition self-test");
    }

    private static void assertParityContract(GenericCrudDefinition mahasiswa) {
        try {
            GenericCrudFormDefinition form = mahasiswa.getFormOverrideProvider().getDefinition(null, null, true);
            check(form.getTabs().size() == 10, "Tab form Mahasiswa tidak lengkap");
            check(form.getSections().size() == 5, "Kelompok modul Mahasiswa tidak lengkap");
            check(form.getActions().size() >= 40, "Fungsi Mahasiswa belum tercatat lengkap");
            int nativeCount = 0; int bridgeCount = 0;
            for (Iterator values = form.getActions().iterator(); values.hasNext();) {
                java.util.Map value = (java.util.Map) values.next();
                String status = String.valueOf(value.get("implementationStatus"));
                if ("NEW_UI_NATIVE".equals(status)) nativeCount++;
                if ("NEW_UI_NATIVE_PANEL".equals(status)) {
                    bridgeCount++;
                    check(value.get("nativePanelKey") != null, "Panel New UI tidak mempunyai key");
                    check(value.get("legacyRoute") == null, "Panel New UI tidak boleh mempunyai route tampilan lain");
                    check(value.get("sourceHandler") != null, "Panel New UI tidak mempunyai service handler");
                }
            }
            check(nativeCount >= 3, "Fungsi native New UI tidak tercatat");
            check(bridgeCount >= 37, "Fungsi Mahasiswa belum tersedia sebagai panel native");
        } catch (Exception e) {
            throw new IllegalStateException("Kontrak parity Mahasiswa gagal dibaca", e);
        }
    }

    private static void assertActionGuards() {
        NewUiHybridMenuNode full = new NewUiHybridMenuNode();
        full.setPermission(new NewUiPermission(true, true, true, true, false, false));
        String[] readActions = new String[]{"meta", "list", "get", "relation_lookup",
                "revisions", "preference_load", "preference_save", "export_xlsx"};
        for (int i = 0; i < readActions.length; i++) {
            check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, full, readActions[i]),
                    "Action READ diblokir: " + readActions[i]);
        }
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, full, "create"), "CREATE diblokir");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, full, "update"), "UPDATE diblokir");
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, full, "delete"), "DELETE diblokir");

        NewUiHybridMenuNode readOnly = new NewUiHybridMenuNode();
        readOnly.setPermission(new NewUiPermission(true, false, false, false, false, false));
        check(NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "list"), "READ role ditolak");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "create"), "CREATE bocor");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "update"), "UPDATE bocor");
        check(!NewUiHybridMenuRouteGuard.isActionAuthorized(null, readOnly, "delete"), "DELETE bocor");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
