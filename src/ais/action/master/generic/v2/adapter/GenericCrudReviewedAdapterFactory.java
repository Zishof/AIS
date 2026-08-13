package ais.action.master.generic.v2.adapter;

import ais.database.model.koperasi.AnggaranKasKoperasi;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;

/** Memilih adapter hasil review untuk model yang mempunyai rule Action khusus. */
public final class GenericCrudReviewedAdapterFactory {
    private GenericCrudReviewedAdapterFactory() { }

    public static GenericCrudAutoEntityAdapter create(Class entityClass, boolean softDelete,
            Class sourceActionClass, boolean metadataLifecycle) {
        if (AnggaranKasKoperasi.class.equals(entityClass)) return new AnggaranKasKoperasiGenericCrudAdapter();
        if (ModalPenyertaanKoperasi.class.equals(entityClass)) return new ModalPenyertaanKoperasiGenericCrudAdapter();
        return new GenericCrudAutoEntityAdapter(entityClass, softDelete, sourceActionClass, metadataLifecycle);
    }
}
