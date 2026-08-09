package ais.action.master.generic.v2.adapter;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.common.Common;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusKeluar;

/**
 * Aturan bisnis headless yang dipakai bersama oleh MahasiswaAction (ZK) dan
 * MahasiswaGenericCrudAdapter (New UI). Kelas ini sengaja tidak mengimpor
 * komponen ZK agar aturan yang sama dapat dijalankan dari request HTTP biasa.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MahasiswaExistingBusinessRules {
    private MahasiswaExistingBusinessRules() { }

    public static List validate(Session session, Mahasiswa mahasiswa) {
        List errors = new ArrayList();
        if (mahasiswa == null) {
            errors.add("_global:Data mahasiswa tidak tersedia.");
            return errors;
        }
        required(errors, "nim", mahasiswa.getNim(), "NIM wajib diisi.");
        required(errors, "nama", mahasiswa.getNama(), "Nama mahasiswa wajib diisi.");
        required(errors, "program", mahasiswa.getProgram(), "Program wajib dipilih.");
        required(errors, "warganegara", mahasiswa.getWarganegara(), "Kewarganegaraan wajib dipilih.");
        if (mahasiswa.getJurusan() == null) errors.add("jurusan:Program studi wajib dipilih.");
        required(errors, "semesterMulai", mahasiswa.getSemesterMulai(), "Semester mulai wajib dipilih.");
        if (mahasiswa.getTahunangkatan() == null) errors.add("tahunangkatan:Tahun angkatan wajib diisi.");
        if (mahasiswa.getTanggalMasuk() == null) errors.add("tanggalMasuk:Tanggal masuk wajib diisi.");

        StatusKeluar statusKeluar = mahasiswa.getStatusKeluar();
        if (statusKeluar != null && mahasiswa.getSemesterLulus() == null) {
            errors.add("semesterLulus:Semester lulus/keluar/DO wajib diisi ketika status keluar dipilih.");
        }
        if (Boolean.TRUE.equals(mahasiswa.getMerupakanPindahan())) {
            required(errors, "pindahanDariKampus", mahasiswa.getPindahanDariKampus(),
                    "Kampus asal mahasiswa pindahan wajib diisi.");
            required(errors, "namaProdiPindah", mahasiswa.getNamaProdiPindah(),
                    "Program studi asal mahasiswa pindahan wajib diisi.");
            required(errors, "nimPindahan", mahasiswa.getNimPindahan(),
                    "NIM asal mahasiswa pindahan wajib diisi.");
            if (mahasiswa.getSksYangDiakui() == null) {
                errors.add("sksYangDiakui:Jumlah SKS yang diakui wajib diisi.");
            }
            if (mahasiswa.getTanggalPindah() == null) {
                errors.add("tanggalPindah:Tanggal pindah wajib diisi.");
            }
        }
        if (Boolean.TRUE.equals(mahasiswa.getMerupakanAlihProdi()) && mahasiswa.getTanggalPindahProdi() == null) {
            errors.add("tanggalPindahProdi:Tanggal alih program studi wajib diisi.");
        }
        validateMinimumGraduationSemester(errors, mahasiswa, statusKeluar);
        validateDuplicateNim(session, errors, mahasiswa);
        return errors;
    }

    public static void applyPersistenceDefaults(Mahasiswa mahasiswa) throws Exception {
        if (mahasiswa == null) return;
        if (mahasiswa.getNim() != null) mahasiswa.setNim(mahasiswa.getNim().trim());
        if (mahasiswa.getNama() != null) mahasiswa.setNama(mahasiswa.getNama().trim());
        if (blank(mahasiswa.getPass())) {
            mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
            mahasiswa.setIs_encripted(Boolean.TRUE);
        }
    }

    private static void validateDuplicateNim(Session session, List errors, Mahasiswa mahasiswa) {
        if (session == null || blank(mahasiswa.getNim())) return;
        Criteria duplicate = session.createCriteria(Mahasiswa.class)
                .add(Restrictions.eq("nim", mahasiswa.getNim().trim()).ignoreCase());
        if (mahasiswa.getId() != null) duplicate.add(Restrictions.ne("id", mahasiswa.getId()));
        duplicate.setMaxResults(1);
        if (!duplicate.list().isEmpty()) errors.add("nim:NIM sudah digunakan mahasiswa lain.");
    }

    private static void validateMinimumGraduationSemester(List errors, Mahasiswa mahasiswa,
            StatusKeluar statusKeluar) {
        if (statusKeluar == null || statusKeluar.getId() == null || !Long.valueOf(1L).equals(statusKeluar.getId())
                || mahasiswa.getSemesterLulus() == null) return;
        Jurusan jurusan = mahasiswa.getJurusan();
        if (jurusan == null || jurusan.getJenjang() == null
                || Boolean.TRUE.equals(mahasiswa.getMerupakanPindahan())
                || Boolean.TRUE.equals(mahasiswa.getMerupakanAlihProdi())) return;
        Integer minimum = jurusan.getJenjang().getJumlahSemesterLulus();
        if (minimum != null && mahasiswa.getSemesterLulus().intValue() < minimum.intValue()) {
            errors.add("semesterLulus:Semester lulus minimal untuk jenjang ini adalah " + minimum + ".");
        }
    }

    private static void required(List errors, String field, Object value, String message) {
        if (blank(value)) errors.add(field + ":" + message);
    }

    private static boolean blank(Object value) {
        return value == null || String.valueOf(value).trim().length() == 0;
    }
}
