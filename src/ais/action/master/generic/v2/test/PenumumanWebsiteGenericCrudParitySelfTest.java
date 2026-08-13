package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudDefinition;
import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.PenumumanWebsiteGenericCrudAdapter;
import ais.database.model.PenumumanWebsite;

public final class PenumumanWebsiteGenericCrudParitySelfTest {
    private PenumumanWebsiteGenericCrudParitySelfTest() { }

    public static void main(String[] args) throws Exception {
        check(GenericCrudReviewedAdapterFactory.isReviewed(PenumumanWebsite.class), "review registry");
        check(GenericCrudReviewedAdapterFactory.create(PenumumanWebsite.class, true, null, false)
                instanceof PenumumanWebsiteGenericCrudAdapter, "review adapter");
        PenumumanWebsiteGenericCrudAdapter adapter = new PenumumanWebsiteGenericCrudAdapter();
        GenericCrudDefinition definition = new GenericCrudDefinition();
        definition.setDeleteEnabled(true); definition.setImportEnabled(true);
        adapter.configure(definition);
        check("tanggal".equals(definition.getDefaultSortProperty()), "sort tanggal");
        check(!definition.isDefaultSortAscending(), "sort terbaru");
        check(definition.getMaxPageSize() == 200, "limit Action");
        check(!definition.isDeleteEnabled() && !definition.isImportEnabled(), "capability Action");
        try {
            adapter.beforeSave(null, new PenumumanWebsite(), null);
            throw new IllegalStateException("judul kosong diterima");
        } catch (GenericCrudException expected) {
            check("TITLE_REQUIRED".equals(expected.getCode()), "validasi judul");
        }
        check(adapter.getNaturalKeyProperties().isEmpty(), "tanpa deduplikasi buatan");
        System.out.println("PenumumanWebsiteGenericCrudParitySelfTest OK");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
