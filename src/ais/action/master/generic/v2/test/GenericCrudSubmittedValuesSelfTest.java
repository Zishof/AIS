package ais.action.master.generic.v2.test;

import java.util.LinkedHashMap;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.GenericCrudSubmittedValues;

/** Regresi FIELD_NOT_ALLOWED dan payload identifier relasi. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class GenericCrudSubmittedValuesSelfTest {
    private GenericCrudSubmittedValuesSelfTest() { }

    public static void main(String[] args) {
        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setIdentifierProperty("id");
        definition.addField(field("id", true, false));
        definition.addField(field("nama", true, true));
        definition.addField(field("jurusan", true, true));
        definition.addField(field("tanggal_dirubah", false, false));

        Map request = new LinkedHashMap();
        request.put("action", new String[] { "update" });
        request.put("id", new String[] { "14073" });
        request.put("nama", new String[] { "Mahasiswa Uji" });
        request.put("jurusan", new String[] { "32" });
        request.put("tanggal_dirubah", new String[] { "2026-08-11" });

        Map update = GenericCrudSubmittedValues.fromParameters(definition, request, false);
        check(!update.containsKey("id"), "ID target bocor sebagai field UPDATE");
        check("Mahasiswa Uji".equals(update.get("nama")), "Field mutable hilang");
        check("32".equals(update.get("jurusan")), "Identifier relasi berubah");
        check(!update.containsKey("tanggal_dirubah"), "Field read-only bocor");
        check(!update.containsKey("action"), "Parameter kontrol bocor");

        Map create = GenericCrudSubmittedValues.fromParameters(definition, request, true);
        check("14073".equals(create.get("id")), "Assigned ID CREATE harus dipertahankan");
        System.out.println("PASS Generic CRUD submitted-value allow-list self-test");
    }

    private static GenericCrudFieldDefinition field(String property, boolean create, boolean update) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(property, property, String.class.getName());
        field.setCreateable(create);
        field.setUpdateable(update);
        return field;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
