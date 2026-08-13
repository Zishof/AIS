package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.KehadiranPegawaiBulananGenericCrudAdapter;
import ais.database.model.KehadiranPegawaiBulanan;

public final class KehadiranPegawaiBulananGenericCrudParitySelfTest {
    private KehadiranPegawaiBulananGenericCrudParitySelfTest() { }
    public static void main(String[] args) {
        check(GenericCrudReviewedAdapterFactory.isReviewed(KehadiranPegawaiBulanan.class), "review registry");
        KehadiranPegawaiBulananGenericCrudAdapter adapter = new KehadiranPegawaiBulananGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition();
        add(definition, "tahun"); add(definition, "bulan"); add(definition, "masuk"); add(definition, "lembur");
        adapter.configure(definition);
        check(GenericCrudDefinition.READ_ONLY.equals(definition.getLifecycleStatus()), "read only");
        check(!definition.isCreateEnabled() && !definition.isUpdateEnabled() && !definition.isDeleteEnabled(), "tanpa mutasi");
        check(!definition.getField("masuk").isUpdateable() && !definition.getField("lembur").isUpdateable(), "metrics immutable");
        check("tahun".equals(definition.getDefaultSortProperty()) && !definition.isDefaultSortAscending(), "sort terbaru");
        System.out.println("KehadiranPegawaiBulananGenericCrudParitySelfTest OK");
    }
    private static void add(GenericCrudDefinition definition, String name) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(name, name, String.class.getName());
        field.setCreateable(true); field.setUpdateable(true); definition.addField(field);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
