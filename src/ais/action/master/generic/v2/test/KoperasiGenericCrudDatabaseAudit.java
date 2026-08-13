package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudDefinitionRegistry;
import ais.action.master.generic.v2.adapter.AnggaranKasKoperasiGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.ModalPenyertaanKoperasiGenericCrudAdapter;

public final class KoperasiGenericCrudDatabaseAudit {
    private KoperasiGenericCrudDatabaseAudit() { }
    public static void main(String[] args) {
        GenericCrudDefinition budget = GenericCrudDefinitionRegistry.tryAutoRegister("koperasi",
                "anggaran_kas_koperasi", new String[] { "Koperasi", "AnggaranKasKoperasi" },
                "ais.action.master.koperasi", "AnggaranKasKoperasiAction",
                new String[] { "onAdd", "onSave", "initCriteria", "onSearchDefault" });
        GenericCrudDefinition capital = GenericCrudDefinitionRegistry.tryAutoRegister("koperasi",
                "modal_penyertaan_koperasi", new String[] { "Koperasi", "ModalPenyertaanKoperasi" },
                "ais.action.master.koperasi", "ModalPenyertaanKoperasiAction",
                new String[] { "onAdd", "onSave", "initCriteria", "onSearchDefault" });
        check(budget != null && budget.getAdapter() instanceof AnggaranKasKoperasiGenericCrudAdapter,
                "binding anggaran kas");
        check(capital != null && capital.getAdapter() instanceof ModalPenyertaanKoperasiGenericCrudAdapter,
                "binding modal penyertaan");
        check(budget.isCreateEnabled() && budget.isUpdateEnabled(), "CRUD anggaran kas");
        check(capital.isCreateEnabled() && capital.isUpdateEnabled(), "CRUD modal penyertaan");
        System.out.println("KoperasiGenericCrudDatabaseAudit OK budgetFields=" + budget.getFields().size()
                + " capitalFields=" + capital.getFields().size());
        System.exit(0);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
