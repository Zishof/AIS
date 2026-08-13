package ais.action.master.generic.v2.test;

import ais.action.master.generic.v2.GenericCrudException;
import ais.action.master.generic.v2.adapter.AnggaranKasKoperasiGenericCrudAdapter;
import ais.action.master.generic.v2.adapter.GenericCrudReviewedAdapterFactory;
import ais.action.master.generic.v2.adapter.ModalPenyertaanKoperasiGenericCrudAdapter;
import ais.database.model.koperasi.AnggaranKasKoperasi;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;

public final class KoperasiGenericCrudParitySelfTest {
    private KoperasiGenericCrudParitySelfTest() { }
    public static void main(String[] args) throws Exception {
        check(GenericCrudReviewedAdapterFactory.create(AnggaranKasKoperasi.class, true, null, true)
                instanceof AnggaranKasKoperasiGenericCrudAdapter, "adapter anggaran kas");
        check(GenericCrudReviewedAdapterFactory.create(ModalPenyertaanKoperasi.class, true, null, true)
                instanceof ModalPenyertaanKoperasiGenericCrudAdapter, "adapter modal penyertaan");
        AnggaranKasKoperasi budget = new AnggaranKasKoperasi(); budget.setTahun(Integer.valueOf(1800));
        expect(new AnggaranKasKoperasiGenericCrudAdapter(), budget, "INVALID_BUDGET_YEAR");
        ModalPenyertaanKoperasi capital = new ModalPenyertaanKoperasi(); capital.setNominal(Double.valueOf(0D));
        expect(new ModalPenyertaanKoperasiGenericCrudAdapter(), capital, "INVESTOR_NAME_REQUIRED");
        System.out.println("KoperasiGenericCrudParitySelfTest OK");
    }
    private static void expect(Object adapter, Object value, String code) throws Exception {
        try {
            if (adapter instanceof AnggaranKasKoperasiGenericCrudAdapter)
                ((AnggaranKasKoperasiGenericCrudAdapter) adapter).beforeSave(null, (AnggaranKasKoperasi) value, null);
            else ((ModalPenyertaanKoperasiGenericCrudAdapter) adapter).beforeSave(null,
                    (ModalPenyertaanKoperasi) value, null);
            throw new IllegalStateException("validasi " + code + " tidak dijalankan");
        } catch (GenericCrudException expected) { check(code.equals(expected.getCode()), code); }
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
