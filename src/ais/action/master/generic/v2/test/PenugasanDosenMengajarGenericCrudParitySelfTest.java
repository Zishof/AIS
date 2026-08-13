package ais.action.master.generic.v2.test;

import java.util.List;
import java.util.Map;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudFieldDefinition;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.PenugasanDosenMengajarGenericCrudAdapter;
import ais.database.model.PenugasanDosenMengajar;

public final class PenugasanDosenMengajarGenericCrudParitySelfTest {
    private PenugasanDosenMengajarGenericCrudParitySelfTest() { }
    public static void main(String[] args) throws Exception {
        check(GenericCrudReviewedAdapterFactory.isReviewed(PenugasanDosenMengajar.class), "review registry");
        PenugasanDosenMengajarGenericCrudAdapter adapter = new PenugasanDosenMengajarGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition();
        add(definition, "kode"); add(definition, "tanggalSuratTugas"); add(definition, "tmtSuratTugas");
        add(definition, "keterangan"); add(definition, "dosen"); add(definition, "tahunAkademik");
        adapter.configure(definition);
        check(!definition.isCreateEnabled() && definition.isUpdateEnabled() && !definition.isDeleteEnabled(), "capability");
        check(definition.getField("kode").isUpdateable() && definition.getField("keterangan").isUpdateable(), "inline edit");
        check(!definition.getField("dosen").isUpdateable() && !definition.getField("tahunAkademik").isUpdateable(), "field immutable");
        List actions = adapter.getActions(definition, null); Map action = (Map) actions.get(0);
        check("generate_from_schedule".equals(action.get("actionKey")), "generate action");
        check("NONE".equals(action.get("selectionMode")), "tanpa selection");
        check(adapter.getNaturalKeyProperties().size() == 5, "natural key");
        System.out.println("PenugasanDosenMengajarGenericCrudParitySelfTest OK");
    }
    private static void add(GenericCrudDefinition definition, String name) {
        GenericCrudFieldDefinition field = new GenericCrudFieldDefinition(name, name, String.class.getName());
        field.setCreateable(true); field.setUpdateable(true); definition.addField(field);
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
}
