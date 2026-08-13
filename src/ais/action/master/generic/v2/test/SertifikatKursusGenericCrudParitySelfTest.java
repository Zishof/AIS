package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.SertifikatKursusGenericCrudAdapter;
import ais.database.model.kursus.SertifikatKursus;

public final class SertifikatKursusGenericCrudParitySelfTest {
    private SertifikatKursusGenericCrudParitySelfTest() { }
    public static void main(String[] args) throws Exception {
        check(GenericCrudReviewedAdapterFactory.isReviewed(SertifikatKursus.class), "review registry");
        SertifikatKursusGenericCrudAdapter adapter = new SertifikatKursusGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition(); add(definition, "status");
        add(definition, "nomorSertifikat"); add(definition, "nilaiAkhir"); adapter.configure(definition);
        check(!definition.isCreateEnabled() && definition.isUpdateEnabled() && !definition.isDeleteEnabled(), "capability");
        check(definition.getField("status").isUpdateable(), "status editable");
        check(!definition.getField("nomorSertifikat").isUpdateable() && !definition.getField("nilaiAkhir").isUpdateable(), "issued data immutable");
        check(definition.getField("status").getEnumValues().length == 2, "status choices");
        List actions = adapter.getActions(definition, null); Map action = (Map) actions.get(0);
        check("public_verification".equals(action.get("actionKey")) && "SINGLE".equals(action.get("selectionMode")), "verification action");
        SertifikatKursus invalid = new SertifikatKursus(); invalid.setStatus("Tidak Valid");
        try { adapter.beforeSave(null, invalid, null); throw new IllegalStateException("status invalid diterima"); }
        catch (GenericCrudException expected) { check("CERTIFICATE_STATUS_INVALID".equals(expected.getCode()), "status validation"); }
        System.out.println("SertifikatKursusGenericCrudParitySelfTest OK");
    }
    private static void add(GenericCrudDefinition definition, String name) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(name, name, String.class.getName());
        field.setCreateable(true); field.setUpdateable(true); definition.addField(field);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
