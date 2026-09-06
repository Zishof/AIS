package ais.common.test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PertemuanPunyaUjian;

/** Offline model checks and source contracts; no production database access. */
public final class UjianDibukaUlangRegressionSelfTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String source(String file) throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)), Charset.forName("UTF-8"));
    }

    public static void main(String[] args) throws Exception {
        PertemuanPunyaUjian ujian = new PertemuanPunyaUjian();
        ujian.setId(Long.valueOf(123));
        Mahasiswa mahasiswa = new Mahasiswa();
        mahasiswa.setId(Long.valueOf(456));
        ujian.setMulaiUjian(new Date(15000));
        ujian.setSampaiUjian(new Date(16000));
        String key = HasilUjianMahasiswa.genKey(ujian, mahasiswa, null, null, null);
        ujian.setMulaiUjian(new Date(22000));
        ujian.setSampaiUjian(new Date(23000));
        check(key.equals(HasilUjianMahasiswa.genKey(ujian, mahasiswa, null, null, null)),
                "Reopening must preserve the participant result key");
        mahasiswa.setId(Long.valueOf(457));
        check(!key.equals(HasilUjianMahasiswa.genKey(ujian, mahasiswa, null, null, null)),
                "Different students must not share results");
        check(new PertemuanPunyaUjian().ambilHasilUjianMahasiswaTelahIkut(false).isEmpty(),
                "Unsaved exam must not query the database");

        String helper = source("src/ais/action/master/helper/HasilUjianMahasiswaHelper.java");
        check(helper.contains("int jumlahPeserta = hasilMap.size();"),
                "OBE count must use participants, not distinct scores");
        check(!helper.contains("int jumlahPeserta = nilaiSet.size();"), "Old count must be removed");
        check(helper.contains("Skor kuis: ") && helper.contains("Nilai kuis: ")
                && helper.contains("Rincian nilai OBE belum tersedia"), "Raw results need explicit OBE fallback");
        String model = source("src/ais/database/model/PertemuanPunyaUjian.java");
        int start = model.indexOf("public List<Long> ambilHasilUjianMahasiswaTelahIkut(boolean refresh)");
        int end = model.indexOf("\n    /**", start);
        String method = model.substring(start, end);
        check(method.contains("Restrictions.eq(\"pertemuanPunyaUjian\", this)")
                && method.contains("Restrictions.isNotNull(\"mulaiPada\")"), "Read started results for this exam");
        check(!method.contains("getMulaiUjian") && !method.contains("getSampaiUjian")
                && !method.contains("ambilLokasiHasilUjianMahasiswa"), "No latest-window or location-cache filter");
        check(method.contains("finally") && method.contains("HibernateUtil.closeSessionQuietly(session)"),
                "Owned session must be closed");
        System.out.println("PASS UjianDibukaUlangRegressionSelfTest (model and source contracts)");
    }
}
