package ais.common.newui.menu;

public final class NewUiNativeJspExplicitRouteSelfTest {
    public static void main(String[] args) {
        check("/WEB-INF/new/root/uiux/mahasiswa.jsp", "/pages/master/mahasiswa.zul", false);
        check("/WEB-INF/new/root/services/mahasiswa_service.jsp", "/pages/master/mahasiswa.zul?x=1", true);
        check("/WEB-INF/new/root/uiux/pegawai.jsp", "/pages/master/pegawai.zul", false);
        check("/WEB-INF/new/root/uiux/jenis_pembayaran.jsp", "/pages/master/jenis_pembayaran.zul", false);
        check("/WEB-INF/new/alumni/uiux/mahasiswa.jsp", "/pages/master/alumni/mahasiswa.zul", false);
        check("/WEB-INF/new/repository/uiux/repository.jsp", "/pages/master/repository.zul?tab=item", false);
        check("/WEB-INF/new/rab/uiux/satuan_kerja.jsp", "/pages/master/rab/satuan_kerja.zul", false);
        check("/WEB-INF/new/library/uiux/penyedia.jsp", "/pages/master/inventory/penyedia.zul", false);
        check("/WEB-INF/new/root/maintenance/uiux/menu.jsp", "/pages/main/menu.zul", false);
        check("/WEB-INF/new/root/pmb/uiux/paket.jsp", "/pages/master/paket.zul", false);
        check("/WEB-INF/new/root/report/uiux/format1/payroll/laporan_absensi_pegawai.jsp", "ais.action.report.format1.payroll.LaporanAbsensiPegawai", false);
        check("/WEB-INF/new/helper/uiux/generate_undangan_wisuda.jsp", "cetakUndanganWisuda", false);
        check("/WEB-INF/new/root/report/uiux/lkp/laporan_realisasi_lkp_detail.jsp", "ais.action.report.lkp.LaporanRealisasiLkpDetailWindow", false);
        check("/WEB-INF/new/root/report/uiux/bkd/laporan_lembar_hasil_verifikasi_bkd.jsp", "ais.action.report.bkd.LaporanLembarHasilVerifikasiBkdWindow", false);
        check("/WEB-INF/new/root/report/uiux/format1/akademik/laporan_rekap_angket_dosen_per_dosen.jsp", "rekapAngketDosenPerDosen", false);
        check("/WEB-INF/new/root/report/uiux/helper/pdf/laporan_rekapitulasi_pmdk.jsp", "rekapDataPmdk", false);
        if(NewUiNativeJspResolver.explicitFromRoute("/pages/other/mahasiswa.zul",false)!=null)throw new IllegalStateException("Route yang tidak terdaftar tidak boleh ditebak eksplisit.");
        System.out.println("NewUiNativeJspExplicitRouteSelfTest OK");
    }
    private static void check(String expected,String route,boolean service){NewUiNativeJspResolver.Result result=NewUiNativeJspResolver.explicitFromRoute(route,service);if(result==null||!expected.equals(result.getTarget()))throw new IllegalStateException(route+" -> "+(result==null?"null":result.getTarget()));}
}
