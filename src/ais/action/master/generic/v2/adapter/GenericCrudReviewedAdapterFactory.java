package ais.action.master.generic.v2.adapter;

import ais.database.model.koperasi.AnggaranKasKoperasi;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;
import ais.database.model.PenumumanWebsite;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.kursus.SertifikatKursus;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Dashboard;

/** Memilih adapter hasil review untuk model yang mempunyai rule Action khusus. */
public final class GenericCrudReviewedAdapterFactory {
    private GenericCrudReviewedAdapterFactory() { }

    public static boolean isReviewed(Class entityClass) {
        return AnggaranKasKoperasi.class.equals(entityClass)
                || ModalPenyertaanKoperasi.class.equals(entityClass)
                || PenumumanWebsite.class.equals(entityClass)
                || PenugasanDosenMengajar.class.equals(entityClass)
                || TransaksiKoperasiDetail.class.equals(entityClass)
                || SertifikatKursus.class.equals(entityClass)
                || KehadiranPegawaiBulanan.class.equals(entityClass)
                || Dashboard.class.equals(entityClass);
    }

    public static GenericCrudAutoEntityAdapter create(Class entityClass, boolean softDelete,
            Class sourceActionClass, boolean metadataLifecycle) {
        if (AnggaranKasKoperasi.class.equals(entityClass)) return new AnggaranKasKoperasiGenericCrudAdapter();
        if (ModalPenyertaanKoperasi.class.equals(entityClass)) return new ModalPenyertaanKoperasiGenericCrudAdapter();
        if (PenumumanWebsite.class.equals(entityClass)) return new PenumumanWebsiteGenericCrudAdapter();
        if (PenugasanDosenMengajar.class.equals(entityClass)) return new PenugasanDosenMengajarGenericCrudAdapter();
        if (TransaksiKoperasiDetail.class.equals(entityClass)) return new TransaksiKoperasiDetailGenericCrudAdapter();
        if (SertifikatKursus.class.equals(entityClass)) return new SertifikatKursusGenericCrudAdapter();
        if (KehadiranPegawaiBulanan.class.equals(entityClass)) return new KehadiranPegawaiBulananGenericCrudAdapter();
        if (Dashboard.class.equals(entityClass)) return new DashboardCatalogGenericCrudAdapter();
        return new GenericCrudAutoEntityAdapter(entityClass, softDelete, sourceActionClass, metadataLifecycle);
    }
}
