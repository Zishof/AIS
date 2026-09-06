package ais.action.master.generic.v2.test;

import java.util.Date;
import java.util.List;

import ais.action.master.generic.v2.adapter.MahasiswaExistingBusinessRules;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;

/** Uji aturan domain bersama tanpa ZK desktop dan tanpa perubahan database. */
@SuppressWarnings("rawtypes")
public final class MahasiswaExistingBusinessRulesSelfTest {
    private MahasiswaExistingBusinessRulesSelfTest() { }

    public static void main(String[] args) {
        Mahasiswa kosong = new Mahasiswa();
        List errors = MahasiswaExistingBusinessRules.validate(null, kosong);
        check(has(errors, "nim:"), "NIM kosong tidak ditolak");
        check(has(errors, "nama:"), "Nama kosong tidak ditolak");
        check(has(errors, "jurusan:"), "Program studi kosong tidak ditolak");

        Mahasiswa valid = minimumValid(new Mahasiswa());
        check(MahasiswaExistingBusinessRules.validate(null, valid).isEmpty(),
                "Data minimum valid ditolak");

        valid = minimumValid(new Mahasiswa() {
            private static final long serialVersionUID = 1L;
            public Boolean getMerupakanPindahan() { return Boolean.TRUE; }
            public Integer getSksYangDiakui() { return null; }
            public Date getTanggalPindah() { return null; }
        });
        errors = MahasiswaExistingBusinessRules.validate(null, valid);
        check(errors.isEmpty(), "Informasi pindahan kosong harus boleh disimpan: " + errors);
        valid.setPindahanDariKampus("Kampus Asal");
        valid.setNamaProdiPindah("Farmasi");
        valid.setNimPindahan("ASAL-001");
        errors = MahasiswaExistingBusinessRules.validate(null, valid);
        check(errors.isEmpty(), "Informasi pindahan sebagian harus boleh disimpan: " + errors);
        check("ASAL-001".equals(valid.getNimPindahan()), "Informasi pindahan tidak boleh dihapus");

        valid = minimumValid(new Mahasiswa() {
            private static final long serialVersionUID = 1L;
            public Boolean getMerupakanAlihProdi() { return Boolean.TRUE; }
            public Date getTanggalPindahProdi() { return null; }
        });
        errors = MahasiswaExistingBusinessRules.validate(null, valid);
        check(has(errors, "tanggalPindahProdi:"), "Tanggal alih prodi tidak diwajibkan: " + errors
                + " flag=" + valid.getMerupakanAlihProdi() + " tanggal=" + valid.getTanggalPindahProdi());
        System.out.println("PASS Mahasiswa existing business rules self-test");
    }

    private static Mahasiswa minimumValid(Mahasiswa value) {
        value.setNim("TEST-001"); value.setNama("Mahasiswa Uji");
        value.setProgram("Reguler"); value.setWarganegara("WNI");
        value.setJurusan(new Jurusan()); value.setSemesterMulai("Ganjil");
        value.setTahunangkatan(Integer.valueOf(2026)); value.setTanggalMasuk(new Date());
        value.setMerupakanPindahan(Boolean.FALSE); value.setMerupakanAlihProdi(Boolean.FALSE);
        return value;
    }

    private static boolean has(List errors, String prefix) {
        for (int i = 0; i < errors.size(); i++) if (String.valueOf(errors.get(i)).startsWith(prefix)) return true;
        return false;
    }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
