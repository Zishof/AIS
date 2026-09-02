package ais.action.master.helper.test;

import ais.action.master.helper.HistoryStatusMahasiswaUtil;
import ais.action.ws.util.ConstantUtil;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;

/**
 * Uji regresi ringan untuk semantik penanda kegiatan syarat aktif. Test ini tidak
 * memerlukan database dan dapat dijalankan langsung dengan Java 8.
 */
public final class HistoryStatusMahasiswaPaymentRuleSelfTest {

    private HistoryStatusMahasiswaPaymentRuleSelfTest() {
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static Kegiatan kegiatan(String nama, Boolean syaratAktif) {
        JenisKegiatan jenis = new JenisKegiatan();
        jenis.setNamaKegiatan(nama);
        jenis.setDigunakanSyaratKeaktifan(syaratAktif);
        Kegiatan kegiatan = new Kegiatan();
        kegiatan.setJenisKegiatan(jenis);
        return kegiatan;
    }

    public static void main(String[] args) {
        check(!HistoryStatusMahasiswaUtil.kegiatanSyaratAktifBerlaku(
                kegiatan("Kegiatan legacy biasa", null), Integer.valueOf(3)),
                "NULL pada kegiatan biasa tidak boleh dianggap sebagai syarat aktif");
        check(!HistoryStatusMahasiswaUtil.kegiatanSyaratAktifBerlaku(
                kegiatan("Kegiatan non-syarat", Boolean.FALSE), Integer.valueOf(3)),
                "Flag false tidak boleh dianggap sebagai syarat aktif");
        check(HistoryStatusMahasiswaUtil.kegiatanSyaratAktifBerlaku(
                kegiatan("UKT eksplisit", Boolean.TRUE), Integer.valueOf(3)),
                "Flag true harus dianggap sebagai syarat aktif");
        check(HistoryStatusMahasiswaUtil.kegiatanSyaratAktifBerlaku(
                kegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA, null), Integer.valueOf(3)),
                "Daftar ulang mahasiswa lama tetap memakai default domain syarat aktif");
        System.out.println("PASS payment-based student status rule self-test");
    }
}
