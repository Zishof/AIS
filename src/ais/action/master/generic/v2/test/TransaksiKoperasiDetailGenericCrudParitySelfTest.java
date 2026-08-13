package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.TransaksiKoperasiDetailGenericCrudAdapter;
import ais.database.model.koperasi.TransaksiKoperasiDetail;

public final class TransaksiKoperasiDetailGenericCrudParitySelfTest {
    private TransaksiKoperasiDetailGenericCrudParitySelfTest() { }
    public static void main(String[] args) {
        check(GenericCrudReviewedAdapterFactory.isReviewed(TransaksiKoperasiDetail.class), "review registry");
        TransaksiKoperasiDetailGenericCrudAdapter adapter = new TransaksiKoperasiDetailGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition(); add(definition, "aktif");
        add(definition, "pokok"); add(definition, "margin"); add(definition, "sisa");
        adapter.configure(definition);
        check(!definition.isCreateEnabled() && definition.isUpdateEnabled() && !definition.isDeleteEnabled(), "capability");
        check(definition.getField("aktif").isUpdateable(), "checkbox aktif");
        check(!definition.getField("pokok").isUpdateable() && !definition.getField("margin").isUpdateable()
                && !definition.getField("sisa").isUpdateable(), "nominal immutable");
        check(!definition.isImportEnabled(), "import finansial ditutup");
        System.out.println("TransaksiKoperasiDetailGenericCrudParitySelfTest OK");
    }
    private static void add(GenericCrudDefinition definition, String name) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(name, name, String.class.getName());
        field.setCreateable(true); field.setUpdateable(true); definition.addField(field);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
