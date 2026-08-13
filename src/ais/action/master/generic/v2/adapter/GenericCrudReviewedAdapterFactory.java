package ais.action.master.generic.v2.adapter;

import ais.database.model.koperasi.AnggaranKasKoperasi;
import ais.database.model.koperasi.ModalPenyertaanKoperasi;
import ais.database.model.PenumumanWebsite;
import ais.database.model.PenugasanDosenMengajar;
import ais.database.model.koperasi.TransaksiKoperasiDetail;
import ais.database.model.kursus.SertifikatKursus;
import ais.database.model.KehadiranPegawaiBulanan;
import ais.database.model.Dashboard;
import ais.database.model.Pegawai;
import ais.database.model.employ.Pensiun;
import ais.database.model.NilaiKegiatanKemahasiswaan;
import ais.database.model.sekolah.NilaiKegiatanKesiswaan;
import ais.database.model.koperasi.PembagianShu;
import ais.database.model.inventory.PengajuanPembelianGudang;
import ais.database.model.sekolah.InterviewCalonSiswa;
import ais.database.model.inventory.Pembelian;
import ais.database.model.akunting.StandingInstruction;
import ais.database.model.akunting.Transitori;
import ais.database.model.sekolah.Tagihan;
import ais.database.model.rab.Kalender;
import ais.database.model.BiodataCalonMahasiswaPunyaVerifikasiBerkas;
import ais.database.model.rab.PenggunaanAnggaran;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.lkp.RealisasiKerjaPegawai;
import ais.database.model.TunggakanMahasiswa;
import ais.database.model.asset.AssetDetail;
import ais.database.model.CicilanPembayaran;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.Perkuliahan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Kegiatan;
import ais.database.model.KegiatanTemporary;
import ais.database.model.PembayaranMahasiswa;
import ais.database.model.BiodataDosen;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.BiodataPegawai;
import ais.database.model.Dosen;

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
                || Dashboard.class.equals(entityClass)
                || Pegawai.class.equals(entityClass)
                || Pensiun.class.equals(entityClass)
                || NilaiKegiatanKemahasiswaan.class.equals(entityClass)
                || NilaiKegiatanKesiswaan.class.equals(entityClass)
                || PembagianShu.class.equals(entityClass)
                || PengajuanPembelianGudang.class.equals(entityClass)
                || InterviewCalonSiswa.class.equals(entityClass)
                || Pembelian.class.equals(entityClass)
                || StandingInstruction.class.equals(entityClass)
                || Transitori.class.equals(entityClass)
                || Tagihan.class.equals(entityClass)
                || Kalender.class.equals(entityClass)
                || BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.equals(entityClass)
                || PenggunaanAnggaran.class.equals(entityClass)
                || KomponenDataProdukKursus.class.equals(entityClass)
                || RealisasiKerjaPegawai.class.equals(entityClass)
                || TunggakanMahasiswa.class.equals(entityClass)
                || AssetDetail.class.equals(entityClass)
                || CicilanPembayaran.class.equals(entityClass)
                || GrupTransaksi.class.equals(entityClass)
                || DaftarPengajuanTransfer.class.equals(entityClass)
                || Perkuliahan.class.equals(entityClass)
                || Detailperkuliahan.class.equals(entityClass)
                || Pertemuan.class.equals(entityClass)
                || KrsMahasiswa.class.equals(entityClass)
                || Kegiatan.class.equals(entityClass)
                || KegiatanTemporary.class.equals(entityClass)
                || PembayaranMahasiswa.class.equals(entityClass)
                || BiodataDosen.class.equals(entityClass)
                || BiodataMahasiswa.class.equals(entityClass)
                || BiodataPegawai.class.equals(entityClass)
                || Dosen.class.equals(entityClass);
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
        if (Pegawai.class.equals(entityClass)) {
            String actionName = sourceActionClass == null ? "" : sourceActionClass.getName();
            if ("ais.action.master.employ.PensiunAction".equals(actionName)
                    || "ais.action.master.employ.PegawaiPensiunAction".equals(actionName))
                return new PegawaiPensiunGenericCrudAdapter();
            // PegawaiAction mempunyai workspace biodata yang jauh lebih luas;
            // route umum tetap fail-closed dan tidak boleh mewarisi filter pensiun.
            return new GenericCrudAutoEntityAdapter(entityClass, softDelete, sourceActionClass, metadataLifecycle);
        }
        if (Pensiun.class.equals(entityClass)) return new PensiunRecordGenericCrudAdapter();
        if (NilaiKegiatanKemahasiswaan.class.equals(entityClass)) return new NilaiKegiatanKemahasiswaanGenericCrudAdapter();
        if (NilaiKegiatanKesiswaan.class.equals(entityClass)) return new NilaiKegiatanKesiswaanGenericCrudAdapter();
        if (PembagianShu.class.equals(entityClass)) return new PembagianShuWorkflowGenericCrudAdapter();
        if (PengajuanPembelianGudang.class.equals(entityClass)) return new PengajuanPembelianGudangWorkflowGenericCrudAdapter();
        if (InterviewCalonSiswa.class.equals(entityClass)) return new InterviewCalonSiswaWorkflowGenericCrudAdapter();
        if (Pembelian.class.equals(entityClass)) return new PembelianWorkflowGenericCrudAdapter();
        if (StandingInstruction.class.equals(entityClass)) return new StandingInstructionWorkflowGenericCrudAdapter();
        if (Transitori.class.equals(entityClass)) return new TransitoriWorkflowGenericCrudAdapter();
        if (Tagihan.class.equals(entityClass)) return new TagihanWorkflowGenericCrudAdapter();
        if (Kalender.class.equals(entityClass)) return new KalenderHariLiburGenericCrudAdapter();
        if (BiodataCalonMahasiswaPunyaVerifikasiBerkas.class.equals(entityClass)) return new CandidateDocumentVerificationWorkflowGenericCrudAdapter();
        if (PenggunaanAnggaran.class.equals(entityClass)) return new PenggunaanAnggaranWorkflowGenericCrudAdapter();
        if (KomponenDataProdukKursus.class.equals(entityClass)) return new CourseComponentWorkflowGenericCrudAdapter();
        if (RealisasiKerjaPegawai.class.equals(entityClass)) return new WorkRealizationWorkflowGenericCrudAdapter();
        if (TunggakanMahasiswa.class.equals(entityClass)) return new StudentArrearsWorkflowGenericCrudAdapter();
        if (AssetDetail.class.equals(entityClass)) return new AssetDepreciationWorkflowGenericCrudAdapter();
        if (CicilanPembayaran.class.equals(entityClass)) return new InstallmentPaymentWorkflowGenericCrudAdapter();
        if (GrupTransaksi.class.equals(entityClass)) return new JournalWorkflowGenericCrudAdapter();
        if (DaftarPengajuanTransfer.class.equals(entityClass)) return new TransferRequestWorkflowGenericCrudAdapter();
        if (Perkuliahan.class.equals(entityClass)) return new PerkuliahanWorkflowGenericCrudAdapter();
        if (Detailperkuliahan.class.equals(entityClass)) return new DetailPerkuliahanWorkflowGenericCrudAdapter();
        if (Pertemuan.class.equals(entityClass)) return new PertemuanWorkflowGenericCrudAdapter();
        if (KrsMahasiswa.class.equals(entityClass)) return new KrsMahasiswaWorkflowGenericCrudAdapter();
        if (Kegiatan.class.equals(entityClass)) return new BillingChargeWorkflowGenericCrudAdapter();
        if (KegiatanTemporary.class.equals(entityClass)) return new BillingCartWorkflowGenericCrudAdapter();
        if (PembayaranMahasiswa.class.equals(entityClass)) return new StudentPaymentWorkflowGenericCrudAdapter();
        if (BiodataDosen.class.equals(entityClass)) return new LecturerBiodataWorkflowGenericCrudAdapter();
        if (BiodataMahasiswa.class.equals(entityClass)) return new StudentBiodataWorkflowGenericCrudAdapter();
        if (BiodataPegawai.class.equals(entityClass)) return new EmployeeBiodataWorkflowGenericCrudAdapter();
        if (Dosen.class.equals(entityClass)) return new LecturerIdentityWorkflowGenericCrudAdapter();
        return new GenericCrudAutoEntityAdapter(entityClass, softDelete, sourceActionClass, metadataLifecycle);
    }
}
