package ais.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pertemuan;
import ais.ui.util.MyMessageboxConfig;

/**
 * Validasi akademik yang sebelumnya berada langsung di Common.
 * Dipisahkan agar Common tetap ringan dan proses validasi lebih mudah dirawat.
 */
public class CommonAcademicValidationHelper {

    private CommonAcademicValidationHelper() {
    }

    @SuppressWarnings({ "unchecked", "static-access" })
    public static Set<Long> checkStatusAbsensi(final Mahasiswa mahasiswa, Integer semester, Integer semesterPendek,
            final String ujian) {
        Set<Long> longsHasilTidak = new HashSet<Long>();
        if (mahasiswa == null || ujian == null || ujian.trim().length() == 0) {
            return longsHasilTidak;
        }

        int maxAlpa = ambilBatasAbsensi(mahasiswa, semester, ujian,
                "batas_maksimal_jumlah_tidak_masuk_kuliah_karena_alpa_untuk_mengikuti_", 34);
        int maxSakit = ambilBatasAbsensi(mahasiswa, semester, ujian,
                "batas_maksimal_jumlah_tidak_masuk_kuliah_karena_sakit_untuk_mengikuti_", 34);
        int maxIzin = ambilBatasAbsensi(mahasiswa, semester, ujian,
                "batas_maksimal_jumlah_tidak_masuk_kuliah_karena_izin_untuk_mengikuti_", 34);
        int maxSemua = ambilBatasAbsensi(mahasiswa, semester, ujian,
                "batas_maksimal_jumlah_semua_tidak_masuk_kuliah_untuk_mengikuti_", 34);
        int maxPersen = ambilBatasAbsensi(mahasiswa, semester, ujian,
                "batas_maksimal_persen_tidak_masuk_kuliah_untuk_mengikuti_", 0);

        List<Long> detailperkuliahans = Common.getDetailperkuliahans(mahasiswa, semester, null, semesterPendek, false,
                false, false);
        if (detailperkuliahans == null || detailperkuliahans.isEmpty()) {
            return longsHasilTidak;
        }

        Map<Long, Detailperkuliahan> detailMap = new HashMap<Long, Detailperkuliahan>();
        List<Long> idPerkuliahan = new ArrayList<Long>();
        for (Long detailperkuliahanid : detailperkuliahans) {
            if (detailperkuliahanid == null) {
                continue;
            }
            Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
                    .ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
            if (detailperkuliahan != null) {
                detailMap.put(detailperkuliahanid, detailperkuliahan);
                if (detailperkuliahan.getPerkuliahan() != null && detailperkuliahan.getPerkuliahan().getId() != null) {
                    idPerkuliahan.add(detailperkuliahan.getPerkuliahan().getId());
                }
            }
        }

        Map<Long, List<String>> dataAbsensi = ambilDataAbsensi(idPerkuliahan);
        StringBuilder warning = new StringBuilder();

        for (Long detailperkuliahanid : detailperkuliahans) {
            Detailperkuliahan detailperkuliahan = detailMap.get(detailperkuliahanid);
            if (detailperkuliahan == null || detailperkuliahan.getPerkuliahan() == null
                    || detailperkuliahan.getMahasiswa() == null || detailperkuliahan.getMahasiswa().getId() == null) {
                continue;
            }

            Map<String, Integer> statuses = detailperkuliahan.getPerkuliahan().hitungStatus(
                    dataAbsensi.get(detailperkuliahan.getPerkuliahan().getId()), detailperkuliahan.getMahasiswa().getId());
            if (statuses == null) {
                statuses = new HashMap<String, Integer>();
            }

            int semua = 0;
            int qtyAlpa = ambilJumlah(statuses, "A");
            semua += qtyAlpa;
            if (qtyAlpa >= maxAlpa) {
                tambahWarning(warning, detailperkuliahan, "Status Kehadiran A (Alpa) = " + qtyAlpa);
                longsHasilTidak.add(detailperkuliahanid);
            }

            int qtySakit = ambilJumlah(statuses, "S");
            semua += qtySakit;
            if (qtySakit >= maxSakit) {
                tambahWarning(warning, detailperkuliahan, "Status Kehadiran S (Sakit) = " + qtySakit);
                longsHasilTidak.add(detailperkuliahanid);
            }

            int qtyIzin = ambilJumlah(statuses, "I");
            semua += qtyIzin;
            if (qtyIzin >= maxIzin) {
                tambahWarning(warning, detailperkuliahan, "Status Kehadiran I (Izin) = " + qtyIzin);
                longsHasilTidak.add(detailperkuliahanid);
            }

            if (semua >= maxSemua) {
                tambahWarning(warning, detailperkuliahan, "tidak masuk kuliah = " + semua);
                longsHasilTidak.add(detailperkuliahanid);
            }

            int jumlahMaksimalPertemuan = detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan() == null ? 0
                    : detailperkuliahan.getPerkuliahan().getJumlahMaksimalPertemuan().intValue();
            if (jumlahMaksimalPertemuan > 0) {
                double persen = (semua * 100.0) / jumlahMaksimalPertemuan;
                if (persen > maxPersen) {
                    tambahWarning(warning, detailperkuliahan,
                            "Tidak hadir kuliah = " + Common.numberFormat.get().format(persen) + "%");
                    longsHasilTidak.add(detailperkuliahanid);
                }
            }
        }

        if (warning.length() > 0) {
            tampilkanWarningAbsensi(mahasiswa, ujian, warning.toString());
        }
        return longsHasilTidak;
    }

    private static int ambilBatasAbsensi(Mahasiswa mahasiswa, Integer semester, String ujian, String prefix,
            int defaultValue) {
        try {
            return Integer.parseInt(Common
                    .getKonfigurasi(prefix + ujian.toLowerCase(), String.valueOf(defaultValue), semester,
                            mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
                            mahasiswa.getStatusAwalMahasiswa())
                    .getNilai().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, List<String>> ambilDataAbsensi(List<Long> idPerkuliahan) {
        Map<Long, List<String>> dataAbsensi = new HashMap<Long, List<String>>();
        if (idPerkuliahan == null || idPerkuliahan.isEmpty()) {
            return dataAbsensi;
        }
        Session session = HibernateUtil.currentSession();
        List<Pertemuan> statusPertemuan = session.createCriteria(Pertemuan.class)
                .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", Boolean.TRUE)))
                .add(Restrictions.in("perkuliahan.id", idPerkuliahan)).add(Restrictions.isNotNull("absensi"))
                .setReadOnly(true).list();
        if (statusPertemuan != null) {
            for (Pertemuan pertemuan : statusPertemuan) {
                if (pertemuan != null && pertemuan.getPerkuliahan() != null
                        && pertemuan.getPerkuliahan().getId() != null) {
                    Long id = pertemuan.getPerkuliahan().getId();
                    if (!dataAbsensi.containsKey(id)) {
                        dataAbsensi.put(id, new ArrayList<String>());
                    }
                    dataAbsensi.get(id).add(pertemuan.getAbsensi());
                }
            }
        }
        return dataAbsensi;
    }

    private static int ambilJumlah(Map<String, Integer> statuses, String key) {
        Integer nilai = statuses == null ? null : statuses.get(key);
        return nilai == null ? 0 : nilai.intValue();
    }

    private static void tambahWarning(StringBuilder warning, Detailperkuliahan detailperkuliahan, String pesan) {
        warning.append("\n\nPerkuliahan ").append(detailperkuliahan.getPerkuliahan()).append(" => ").append(pesan);
    }

    private static void tampilkanWarningAbsensi(final Mahasiswa mahasiswa, final String ujian, final String warning) {
        Common.createDefaultTimer(new EventListener() {
            @Override
            public void onEvent(Event arg0) throws Exception {
                String warningUtama = "Mahasiswa dengan nim \"" + mahasiswa.getNim()
                        + "\" belum bisa mencetak semua matakuliah di kartu ujian " + ujian
                        + ", karena alasan sbb :" + warning;
                try {
                    MyMessageboxConfig.show(warningUtama, "Peringatan", MyMessageboxConfig.OK,
                            MyMessageboxConfig.EXCLAMATION);
                } catch (Exception e) {
                    Common.tampilErrorJikaAdmin(e);
                }
            }
        });
    }
}
