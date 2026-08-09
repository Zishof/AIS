package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudAutoDefinitionFactory;

/** Uji kontrak lifecycle CRUD yang diturunkan dari metode Action existing. */
public final class GenericCrudActionLifecycleSelfTest {
    private GenericCrudActionLifecycleSelfTest() { }

    public static void main(String[] args) {
        String[] methods = new String[] { "onSearchDefault", "onAdd", "onSave", "onDeleteSelected" };
        check(GenericCrudAutoDefinitionFactory.supports(methods, new String[] { "onAdd", "onCreate" }),
                "CREATE dari Action tidak terdeteksi");
        check(GenericCrudAutoDefinitionFactory.supports(methods, new String[] { "onSave", "onUpdate" }),
                "UPDATE dari Action tidak terdeteksi");
        check(GenericCrudAutoDefinitionFactory.supports(methods, new String[] { "onDelete", "onRemove" }),
                "DELETE prefix dari Action tidak terdeteksi");
        check(!GenericCrudAutoDefinitionFactory.supports(new String[] { "onSearchDefault" },
                new String[] { "onDelete", "onRemove" }), "READ-only Action tidak boleh mendapat DELETE");
        check(GenericCrudAutoDefinitionFactory.supports(methods, new String[] { "onSave" }),
                "Lifecycle existing harus membutuhkan onSave eksplisit");
        System.out.println("PASS Generic CRUD Action lifecycle self-test");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
