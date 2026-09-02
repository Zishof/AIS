package ais.common.newui.koperasi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ais.action.master.koperasi.helper.SimpanPinjamReportService;

/** Self-test struktur kontrak Laporan Simpan Pinjam tanpa database. */
public final class NewUiLaporanSimpanPinjamSelfTest {

    private NewUiLaporanSimpanPinjamSelfTest() { }

    public static void main(String[] args) {
        List<SimpanPinjamReportService.Bagian> katalog = SimpanPinjamReportService.katalog();
        check(katalog.size() == 9, "harus ada delapan buku dan satu laporan bunga");
        Set<String> kunci = new HashSet<String>();
        for (SimpanPinjamReportService.Bagian bagian : katalog) {
            check(bagian.kunci != null && bagian.kunci.length() > 0, "kunci wajib diisi");
            check(kunci.add(bagian.kunci), "kunci tidak boleh ganda: " + bagian.kunci);
            check(bagian.header.length == bagian.jenisKolom.length,
                    "jumlah header dan tipe kolom harus sama: " + bagian.kunci);
            check(bagian.header.length > 0, "laporan wajib mempunyai kolom: " + bagian.kunci);
            check(SimpanPinjamReportService.dikenal(bagian.kunci), "katalog harus dikenali");
        }
        check(kunci.contains(SimpanPinjamReportService.BUNGA_SIMPANAN), "laporan bunga wajib tersedia");
        check(!SimpanPinjamReportService.dikenal("../../template"), "kunci arbitrer wajib ditolak");
        System.out.println("NewUiLaporanSimpanPinjamSelfTest OK (9 bagian)");
    }

    private static void check(boolean benar, String pesan) {
        if (!benar) throw new IllegalStateException(pesan);
    }
}
