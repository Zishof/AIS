package ais.action.master.pmb.nim;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Jenjang;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;

/**
 * Generator NIM Politeknik Bhakti Kencana (10 digit).
 *
 * Format:
 *   Digit 1-2  : 2 angka terakhir Kode Perguruan Tinggi  (mis. 045078 → 78)
 *   Digit 3-4  : 2 angka terakhir Tahun Masuk             (mis. 2026 → 26)
 *   Digit 5    : Periode masuk  — Ganjil=1, Genap=2
 *   Digit 6    : Jenjang        — Diploma/D3=1, Sarjana Terapan/D4=2
 *   Digit 7    : Kode Program Studi — diambil dari field Kode Jurusan/Prodi.
 *                Pastikan field Kode pada data Jurusan diisi "1", "2", "3" dst.
 *                (Farmasi=1, Fisioterapi=2, MIK=3, dst.)
 *   Digit 8-10 : Nomor urut mahasiswa per prodi-semester-tahun (3 digit)
 *
 * Contoh: 7826111001
 *   78  = kode PT 045078
 *   26  = tahun 2026
 *   1   = semester Ganjil
 *   1   = D3/Diploma
 *   1   = Farmasi (kode prodi diisi "1" di data Jurusan)
 *   001 = nomor urut ke-1
 *
 * Saat Regenerasi NIM:
 *   Jika mahasiswa sudah memiliki NIM, nomor urut (3 digit terakhir) dipertahankan
 *   selama tidak ada konflik dengan mahasiswa lain. Jika konflik, sistem otomatis
 *   menggunakan nomor urut berikutnya.
 */
public class PoltekBhaktiKencanaNimGenerator implements NimGenerator {

    /** Fallback 2 digit akhir kode PT bila PerguruanTinggiUtil tidak tersedia. */
    private static final String FALLBACK_KODE_PT = "78";

    @Override
    public String generateNim(BiodataCalonMahasiswa calonMahasiswa) {
        return generateNim(calonMahasiswa, new ArrayList<String>());
    }

    @Override
    public String generateNim(BiodataCalonMahasiswa calonMahasiswa,
            List<String> jumlahPengecualian) {

        String nim = "-";

        if (calonMahasiswa.getProdiLulus() == null) {
            return nim;
        }

        Session session = HibernateUtil.currentNativeSession();
        try {

            // Digit 1-2: 2 angka terakhir kode PT (mis. "045078" → "78")
            String digitPt = FALLBACK_KODE_PT;
            try {
                PerguruanTinggi pt = PerguruanTinggiUtil.getPerguruanTinggi();
                if (pt != null) {
                    String kodePt = pt.getKodePerguruanTinggi();
                    if (kodePt != null && kodePt.trim().length() >= 2) {
                        kodePt = kodePt.trim();
                        digitPt = kodePt.substring(kodePt.length() - 2);
                    }
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/pmb/nim/PoltekBhaktiKencanaNimGenerator.java:78");
                // gunakan FALLBACK_KODE_PT
            }

            // Digit 3-4: 2 angka terakhir tahun masuk
            Integer tahun = calonMahasiswa.getTahun();
            String digitTahun = tahun.toString().substring(2);

            // Digit 5: periode masuk — Ganjil=1, Genap=2
            String digitSemester = Perkuliahan.GANJIL.equals(calonMahasiswa.getSemesterMulai()) ? "1" : "2";

            // Digit 6: jenjang — D3/Diploma=1, D4/Sarjana Terapan=2
            String digitJenjang = resolveDigitJenjang(calonMahasiswa);

            // Digit 7: kode prodi — baca dari field Kode pada entitas Jurusan.
            String digitProdi = calonMahasiswa.getProdiLulus().getKode();
            if (digitProdi == null) digitProdi = "0";
            digitProdi = digitProdi.trim();

            // Prefix 7 digit (format selain nomor urut)
            String prefix7 = digitPt + digitTahun + digitSemester + digitJenjang + digitProdi;

            // ----------------------------------------------------------------
            // REGENERASI: pertahankan nomor urut lama jika tidak konflik.
            // Hanya berlaku pada panggilan pertama (bukan rekursi deduplication).
            // ----------------------------------------------------------------
            String nimLama = calonMahasiswa.getNim();
            if (jumlahPengecualian.isEmpty()
                    && nimLama != null && !nimLama.trim().isEmpty()) {
                nimLama = nimLama.trim();
                if (nimLama.length() >= 3) {
                    String urutLama = nimLama.substring(nimLama.length() - 3);
                    if (urutLama.matches("\\d{3}")) {
                        String nimCandidate = prefix7 + urutLama;
                        if (nimCandidate.equals(nimLama)) {
                            // Format & urut tidak berubah — kembalikan langsung
                            return nimCandidate;
                        }
                        // Cek apakah urut ini sudah dipakai mahasiswa lain
                        Integer konflik = ((Number) session.createCriteria(Mahasiswa.class)
                                .add(Restrictions.eq("nim", nimCandidate))
                                .setProjection(Projections.count("nim"))
                                .uniqueResult()).intValue();
                        if (konflik == 0) {
                            System.out.println("PoltekBhaktiKencana NIM regenerasi — urut dipertahankan:");
                            System.out.println("  NIM lama  = " + nimLama);
                            System.out.println("  NIM baru  = " + nimCandidate + "  (urut " + urutLama + " dipertahankan)");
                            return nimCandidate;
                        }
                        System.out.println("PoltekBhaktiKencana NIM regenerasi — urut " + urutLama
                                + " konflik, fallback ke urut berikutnya.");
                    }
                }
            }

            // ----------------------------------------------------------------
            // Digit 8-10: nomor urut berikutnya berdasarkan jumlah mahasiswa
            // di prodi + semester + tahun yang sama.
            // ----------------------------------------------------------------
            Long jumlah = ((Number) session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.or(
                            Restrictions.isNull("aktif"),
                            Restrictions.eq("aktif", true)))
                    .setProjection(Projections.rowCount())
                    .add(Restrictions.eq("tahunangkatan", tahun))
                    .add(Restrictions.eq("semesterMulai", calonMahasiswa.getSemesterMulai()))
                    .add(Restrictions.eq("jurusan", calonMahasiswa.getProdiLulus()))
                    .setMaxResults(1)
                    .uniqueResult()).longValue();

            jumlah += jumlahPengecualian.size();
            String digitUrut = "000000000000" + (jumlah + 1);
            digitUrut = digitUrut.substring(digitUrut.length() - 3);

            System.out.println("PoltekBhaktiKencana NIM generation:");
            System.out.println("  digit 1-2  (kode PT suffix)  = " + digitPt);
            System.out.println("  digit 3-4  (tahun masuk)     = " + digitTahun);
            System.out.println("  digit 5    (semester)         = " + digitSemester);
            System.out.println("  digit 6    (jenjang)          = " + digitJenjang);
            System.out.println("  digit 7    (kode prodi)       = " + digitProdi
                    + "  [prodi: " + calonMahasiswa.getProdiLulus().getKode()
                    + "-" + calonMahasiswa.getProdiLulus().getNama() + "]");
            System.out.println("  digit 8-10 (nomor urut)       = " + digitUrut);

            nim = prefix7 + digitUrut;

            Integer count = ((Number) session.createCriteria(Mahasiswa.class)
                    .add(Restrictions.eq("nim", nim))
                    .setProjection(Projections.count("nim"))
                    .uniqueResult()).intValue();

            if (!count.equals(0)) {
                jumlahPengecualian.add(nim);
                return generateNim(calonMahasiswa, jumlahPengecualian);
            }

            return nim;

        } finally {
            HibernateUtil.closeSession();
        }
    }

    /**
     * Menentukan digit jenjang:
     *  - Jika kode Jenjang sudah "1" atau "2", dipakai langsung.
     *  - Jika tidak, diturunkan dari nama Jenjang: D3/Diploma=1, D4/Sarjana Terapan=2.
     */
    private String resolveDigitJenjang(BiodataCalonMahasiswa calonMahasiswa) {
        try {
            Jenjang jenjang = calonMahasiswa.getProdiLulus().getJenjang();
            if (jenjang == null) return "1";

            String kode = jenjang.getKode().trim();
            if ("1".equals(kode) || "2".equals(kode)) {
                return kode;
            }

            // derivasi dari nama jenjang
            String nama = jenjang.getNama() == null ? "" : jenjang.getNama().trim().toUpperCase();
            if (nama.startsWith("D4") || nama.contains("SARJANA TERAPAN") || nama.contains("DIPLOMA 4")) {
                return "2";
            }
            if (nama.startsWith("D3") || nama.contains("DIPLOMA 3")) {
                return "1";
            }

            // kode apa adanya sebagai fallback
            return kode;
        } catch (Exception e) {
            return "1";
        }
    }
}
