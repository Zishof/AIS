package ais.action.master.generic.v2.test;

import ais.action.master.AgamaAction;
import ais.action.master.AlatTransportasiMahasiswaAction;
import ais.action.master.KurikulumAction;
import ais.action.master.SkripsiAction;
import ais.action.master.BuktiPembayaranAction;
import ais.action.master.payroll.ItemGajiPegawaiAction;
import ais.action.master.rab.ChecklistLaporanDetailAction;
import ais.action.master.rab.ChecklistLaporanDetailDefaultAction;
import ais.action.master.sirs.BookingRegistrasiAction;
import ais.action.master.sirs.CetakKartuPasienAction;
import ais.action.master.sirs.DiagnosaPenyakitAction;
import ais.action.master.sirs.PembayaranAction;
import ais.action.master.sirs.TransaksiReturAction;
import ais.action.master.koperasi.PembelianAnggotaKoperasiAction;
import ais.action.master.asset.PenyediaAssetAction;
import ais.action.master.generic.v2.adapter.GenericCrudExistingActionInvoker;
import ais.action.master.recruitment.CalonPegawaiAction;
import ais.common.HeadlessActionContext;
import ais.common.HeadlessBusinessRuleException;
import ais.database.model.Agama;
import ais.database.model.AlatTransportasiMahasiswa;
import ais.database.model.Kurikulum;
import ais.database.model.Skripsi;
import ais.database.model.BuktiPembayaran;
import ais.database.model.payroll.ItemGajiPegawai;
import ais.database.model.rab.ChecklistLaporanDetail;
import ais.database.model.rab.ChecklistLaporanDetailDefault;
import ais.database.model.sirs.BookingRegistrasi;
import ais.database.model.sirs.CetakKartuPasien;
import ais.database.model.sirs.DiagnosaPenyakit;
import ais.database.model.sirs.Pembayaran;
import ais.database.model.sirs.TransaksiRetur;
import ais.database.model.koperasi.PembelianAnggotaKoperasi;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.recruitment.CalonPegawai;
import ais.ui.util.MyMessageboxConfig;

/** Uji bahwa validasi Action existing dapat dijalankan tanpa compose halaman ZUL. */
public final class HeadlessExistingActionSelfTest {
    private HeadlessExistingActionSelfTest() { }

    public static void main(String[] args) throws Exception {
        boolean rejected = false;
        try {
            new AgamaAction().executeHeadlessSave(new Agama());
        } catch (HeadlessBusinessRuleException expected) {
            rejected = expected.getMessage() != null && expected.getMessage().trim().length() > 0;
        }
        check(rejected, "Validasi wajib isi AgamaAction tidak dijalankan secara headless.");
        rejected = false;
        try {
            AlatTransportasiMahasiswa legacyTarget = new AlatTransportasiMahasiswa();
            legacyTarget.setFeeder(Long.valueOf(0L));
            GenericCrudExistingActionInvoker.execute(AlatTransportasiMahasiswaAction.class,
                    legacyTarget);
        } catch (HeadlessBusinessRuleException expected) {
            rejected = expected.getMessage() != null && expected.getMessage().trim().length() > 0;
        }
        check(rejected, "Validasi Action legacy DataInitDefault tidak dijalankan secara headless.");
        rejected = false;
        try {
            GenericCrudExistingActionInvoker.execute(LegacyNamedWindowAction.class, new Agama());
        } catch (HeadlessBusinessRuleException expected) {
            rejected = expected.getMessage() != null && expected.getMessage().indexOf("Nama wajib") >= 0;
        }
        check(rejected, "Window legacy bernama non-addWindow tidak berhasil diinjeksi.");
        check(GenericCrudExistingActionInvoker.supports(LegacyComponentHostAction.class, Agama.class),
                "Kontrak Action bertipe Component tidak dikenali.");
        rejected = false;
        try {
            Agama componentTarget = new Agama();
            componentTarget.setKode("");
            GenericCrudExistingActionInvoker.execute(LegacyComponentHostAction.class, componentTarget);
        } catch (HeadlessBusinessRuleException expected) {
            rejected = expected.getMessage() != null && expected.getMessage().trim().length() > 0;
        }
        check(rejected, "Container legacy bertipe Component tidak berhasil diinjeksi.");
        check(GenericCrudExistingActionInvoker.supportsCreate(LegacyComponentHostAction.class, Agama.class),
                "CREATE Action bertipe Component belum dikenali sebagai lifecycle native.");
        LegacyVoidSaveAction.saved = false;
        GenericCrudExistingActionInvoker.execute(LegacyVoidSaveAction.class, new Agama());
        check(LegacyVoidSaveAction.saved, "Action legacy void onSave(Event) tidak dieksekusi.");
        LegacyTabpanelAction.saved = false;
        GenericCrudExistingActionInvoker.execute(LegacyTabpanelAction.class, new Agama());
        check(LegacyTabpanelAction.saved, "Root Tabpanel legacy tidak dibangun secara headless.");
        check(GenericCrudExistingActionInvoker.supportsCreate(PenyediaAssetAction.class, PenyediaAsset.class),
                "Lifecycle native PenyediaAssetAction belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(CalonPegawaiAction.class, CalonPegawai.class),
                "Lifecycle native CalonPegawaiAction belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(KurikulumAction.class, Kurikulum.class),
                "Lifecycle init(Kurikulum, copy=false) belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(SkripsiAction.class, Skripsi.class),
                "Lifecycle init(Skripsi, tampilkanSimpan=true) belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(ChecklistLaporanDetailAction.class,
                ChecklistLaporanDetail.class), "Lifecycle callback Checklist Laporan belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(ChecklistLaporanDetailDefaultAction.class,
                ChecklistLaporanDetailDefault.class), "Lifecycle callback Checklist Default belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(ItemGajiPegawaiAction.class,
                ItemGajiPegawai.class), "Lifecycle callback Item Gaji Pegawai belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(BookingRegistrasiAction.class,
                BookingRegistrasi.class), "Tabpanel form Booking Registrasi belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(CetakKartuPasienAction.class,
                CetakKartuPasien.class), "Tabpanel form Cetak Kartu Pasien belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(DiagnosaPenyakitAction.class,
                DiagnosaPenyakit.class), "Tabpanel form Diagnosa Penyakit belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(PembayaranAction.class,
                Pembayaran.class), "Tabpanel form Pembayaran SIRS belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(PembelianAnggotaKoperasiAction.class,
                PembelianAnggotaKoperasi.class), "Tabpanel form Pembelian Anggota belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(TransaksiReturAction.class,
                TransaksiRetur.class), "Tabpanel form Transaksi Retur belum terhubung.");
        check(GenericCrudExistingActionInvoker.supportsCreate(BuktiPembayaranAction.class,
                BuktiPembayaran.class), "Lifecycle kontekstual Bukti Pembayaran belum terhubung.");
        check(!HeadlessActionContext.isActive(), "Konteks headless bocor setelah Action selesai.");
        System.out.println("PASS existing Action headless validation self-test");
        // Hibernate/c3p0 aplikasi mempertahankan worker non-daemon pada eksekusi CLI.
        System.exit(0);
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }

    /** Fixture kecil untuk membuktikan Action lama tidak bergantung nama field addWindow. */
    public static final class LegacyNamedWindowAction {
        private org.zkoss.zul.Window dialog;
        private org.zkoss.zul.Textbox nama;

        private void init(Agama agama) {
            nama = new org.zkoss.zul.Textbox(agama.getNama());
            nama.setParent(dialog);
        }

        public boolean onSave(org.zkoss.zk.ui.event.Event event) throws Exception {
            if (nama.getValue() == null || nama.getValue().trim().length() == 0) {
                MyMessageboxConfig.show("Nama wajib diisi.");
                return false;
            }
            return true;
        }
    }

    /** Fixture untuk Action lama yang mendeklarasikan addWindow sebagai Component. */
    public static final class LegacyComponentHostAction {
        private org.zkoss.zk.ui.Component addWindow;
        private org.zkoss.zul.Textbox kode;

        public void init(Agama agama) {
            kode = new org.zkoss.zul.Textbox(agama.getNama());
            kode.setParent(addWindow);
        }

        public boolean onSave(org.zkoss.zk.ui.event.Event event) throws Exception {
            if (kode.getValue() == null || kode.getValue().trim().length() == 0) {
                MyMessageboxConfig.show("Kode wajib diisi.");
                return false;
            }
            return true;
        }
    }

    /** Fixture pola legacy yang menyatakan sukses lewat return normal, bukan boolean. */
    public static final class LegacyVoidSaveAction {
        static boolean saved;
        private org.zkoss.zul.Window dialog;

        public void init(Agama agama) {
            org.zkoss.zul.Label marker = new org.zkoss.zul.Label("siap");
            marker.setParent(dialog);
        }

        public void onSave(org.zkoss.zk.ui.event.Event event) {
            saved = true;
        }
    }

    /** Fixture root form tambahData yang lazim pada CRUD SIRS/koperasi. */
    public static final class LegacyTabpanelAction {
        static boolean saved;
        private org.zkoss.zul.Tabpanel tambahData;

        public void init(Agama agama) {
            org.zkoss.zul.Div form = new org.zkoss.zul.Div();
            form.setParent(tambahData);
            tambahData.getLinkedTab().setSelected(true);
        }

        public boolean onSave(org.zkoss.zk.ui.event.Event event) {
            saved = tambahData.getLinkedTab().isSelected();
            return saved;
        }
    }
}
