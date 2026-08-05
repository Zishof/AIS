package ais.action.master.akreditasi;

import ais.common.Common;
import ais.ui.util.MyWindow;

/**
 * Dasbor utama Akreditasi LAMDIK S1 — menampilkan semua instrumen penilaian
 * DKPS 2.0 (Deskripsi Kinerja Program Studi) dalam 7 kelompok kriteria.
 */
public class LaporanAkreditasiLamdikS1 extends MyWindow {

    private static final long serialVersionUID = 1L;

    public LaporanAkreditasiLamdikS1() {
        super();
        try { init(); } catch (Exception e) { Common.tampilErrorJikaAdmin(e); }
    }

    public LaporanAkreditasiLamdikS1(String title, String border, boolean closable) throws Exception {
        super(title, border, closable);
        init();
    }

    private void init() {
        ais.ui.util.MyButtonTabbox outerBtn = ais.ui.util.MyButtonTabbox.buat(
                Common.tampilanScrollTabbox(this), "100%", new int[] { 0 });

        outerBtn.tambahTabLazy(0, "Kriteria 2: Kerjasama", "/img/svg/handshake.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK2(panel); }
        });
        outerBtn.tambahTabLazy(1, "Kriteria 3: Mahasiswa", "/img/svg/user-graduate.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK3(panel); }
        });
        outerBtn.tambahTabLazy(2, "Kriteria 4: SDM", "/img/svg/user-group.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK4(panel); }
        });
        outerBtn.tambahTabLazy(3, "Kriteria 5: Keuangan & Sarana", "/img/svg/money-bills.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK5(panel); }
        });
        outerBtn.tambahTabLazy(4, "Kriteria 6: Pendidikan", "/img/svg/chalkboard-teacher-light.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK6(panel); }
        });
        outerBtn.tambahTabLazy(5, "Kriteria 7: Penelitian", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK7(panel); }
        });
        outerBtn.tambahTabLazy(6, "Kriteria 8: PkM", "/img/svg/user-follow-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { buildK8(panel); }
        });
    }

    private void buildK2(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "2.1-1 Kerjasama Pendidikan", "/img/svg/book.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_2_1_1_KerjasamaPendidikan()); }
        });
        btn.tambahTabLazy(1, "2.1-2 Kerjasama Penelitian", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_2_1_2_KerjasamaPenelitian()); }
        });
        btn.tambahTabLazy(2, "2.1-3 Kerjasama PkM", "/img/svg/user-follow-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_2_1_3_KerjasamaPkm()); }
        });
    }

    private void buildK3(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "3.1 Seleksi Mhs Baru", "/img/svg/user-pen.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_1_SeleksiMahasiswa()); }
        });
        btn.tambahTabLazy(1, "3.2 Prestasi Mahasiswa", "/img/svg/award.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_2_PrestasiMahasiswa()); }
        });
        btn.tambahTabLazy(2, "3.3-1 HKI Paten", "/img/svg/check-square.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_3_1_HkiPaten()); }
        });
        btn.tambahTabLazy(3, "3.3-2 HKI Hak Cipta", "/img/svg/check-circled-outline.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_3_2_HkiHakCipta()); }
        });
        btn.tambahTabLazy(4, "3.3-3 Buku ISBN", "/img/svg/book.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_3_3_BukuIsbn()); }
        });
        btn.tambahTabLazy(5, "3.3-4 Publikasi Jurnal Mhs", "/img/svg/journal-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_3_4_PublikasiJurnalMhs()); }
        });
        btn.tambahTabLazy(6, "3.4 Kepuasan Mahasiswa", "/img/svg/person-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_3_4_KepuasanMahasiswa()); }
        });
    }

    private void buildK4(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "4.1 Profil DTPS", "/img/svg/user-circle-thin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_1_ProfilDtps()); }
        });
        btn.tambahTabLazy(1, "4.2 Beban Kerja DTPS", "/img/svg/user-list-thin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_2_BebanKerjaDtps()); }
        });
        btn.tambahTabLazy(2, "4.3 Rekognisi DTPS", "/img/svg/award.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_3_RekognisiDtps()); }
        });
        btn.tambahTabLazy(3, "4.4 Pengembangan DTPS", "/img/svg/user-pen.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_4_PengembanganDtps()); }
        });
        btn.tambahTabLazy(4, "4.5 Tenaga Kependidikan", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_5_TenagaKependidikan()); }
        });
        btn.tambahTabLazy(5, "4.6 Pengembangan Tendik", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_4_6_PengembanganTendik()); }
        });
    }

    private void buildK5(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "5.1 Penggunaan Dana", "/img/svg/money-bills.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_5_1_PenggunaanDana()); }
        });
        btn.tambahTabLazy(1, "5.2 Sarana Laboratorium", "/img/svg/folder2-open.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_5_2_SaranaLaboratorium()); }
        });
        btn.tambahTabLazy(2, "5.3 Prasarana Pendidikan", "/img/svg/folder2.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_5_3_PrasaranaPendidikan()); }
        });
        btn.tambahTabLazy(3, "5.4 TIK", "/img/svg/dashboard-chart.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_5_4_Tik()); }
        });
    }

    private void buildK6(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "6.1 Kurikulum", "/img/svg/book.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_1_StrukturKurikulum()); }
        });
        btn.tambahTabLazy(1, "6.2 Integrasi Riset/PkM", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_2_IntegrasiRisetPkm()); }
        });
        btn.tambahTabLazy(2, "6.3 Magang Kependidikan", "/img/svg/user-business.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_3_MagangKependidikan()); }
        });
        btn.tambahTabLazy(3, "6.4 Kegiatan Luar Kelas", "/img/svg/users.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_4_KegiatanLuarKelas()); }
        });
        btn.tambahTabLazy(4, "6.5 Bimbing TA", "/img/svg/chalkboard-user.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_5_PembimbinganTA()); }
        });
        btn.tambahTabLazy(5, "6.6 IPK Lulusan", "/img/svg/graduate-cap.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_6_IpkLulusan()); }
        });
        btn.tambahTabLazy(6, "6.7 Masa Studi", "/img/svg/calendar2.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_7_MasaStudi()); }
        });
        btn.tambahTabLazy(7, "6.8 Lulusan Bekerja", "/img/svg/user-tie.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_8_LulusanBekerja()); }
        });
        btn.tambahTabLazy(8, "6.9 Waktu Tunggu", "/img/svg/chart-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_9_WaktuTungguLulusan()); }
        });
        btn.tambahTabLazy(9, "6.10 Kesesuaian Bidang", "/img/svg/check-square.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_10_KesesuaianBidangKerja()); }
        });
        btn.tambahTabLazy(10, "6.11 Kepuasan Pengguna", "/img/svg/person-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_6_11_KepuasanPengguna()); }
        });
    }

    private void buildK7(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "7.1 Penelitian DTPS", "/img/svg/journal-bookmark.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_7_1_PenelitianDtps()); }
        });
        btn.tambahTabLazy(1, "7.2 Mhs dalam Riset", "/img/svg/user-graduate.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_7_2_KeterlibatanMhsRiset()); }
        });
        btn.tambahTabLazy(2, "7.3 Publikasi Ilmiah", "/img/svg/journal-check.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_7_3_PublikasiIlmiah()); }
        });
        btn.tambahTabLazy(3, "7.4 Publikasi Jurnal Tier", "/img/svg/books-thin.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_7_4_PublikasiJurnalTier()); }
        });
        btn.tambahTabLazy(4, "7.5 Sitasi Karya Ilmiah", "/img/svg/check2-all.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_7_5_SitasiKaryaIlmiah()); }
        });
    }

    private void buildK8(org.zkoss.zul.Div container) {
        ais.ui.util.MyButtonTabbox btn = ais.ui.util.MyButtonTabbox.buat(container, "100%", new int[] { 0 });
        btn.tambahTabLazy(0, "8.1 PkM DTPS", "/img/svg/user-follow-line.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_8_1_PkmDtps()); }
        });
        btn.tambahTabLazy(1, "8.2 Mhs dalam PkM", "/img/svg/user-group.svg", new ais.ui.util.MyButtonTabbox.PemuatTab() {
            @Override public void muat(org.zkoss.zul.Div panel) throws Exception { panel.appendChild(new LaporanDkps_8_2_KeterlibatanMhsPkm()); }
        });
    }
}
