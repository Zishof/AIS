package ais.action.servlet.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.model.BlokirMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;

/**
 * Guard terpusat untuk validasi akses API sebelum action dijalankan.
 * Saat rule baru diperlukan, cukup tambahkan di class ini agar tidak menyebar di Api.java.
 */
public final class ApiAccessGuard {

    private static final String KEY_BOLEH_MASUK_API = "apakah_boleh_masuk_api_aktif";

    private ApiAccessGuard() {
    }

    public static JSONObject check(HttpServletRequest request, JSONObject json) {
        if (!isCekBolehMasukApiAktif()) {
            return null;
        }
        return checkMahasiswaBlocker(request, json);
    }

    private static boolean isCekBolehMasukApiAktif() {
        try {
            Konfigurasi konfigurasi = Common.getKonfigurasi(KEY_BOLEH_MASUK_API, Konfigurasi.AKTIF);
            return konfigurasi != null && Konfigurasi.AKTIF.equalsIgnoreCase(ApiHelperSupport.trimToEmpty(konfigurasi.getNilai()));
        } catch (Exception e) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private static JSONObject checkMahasiswaBlocker(HttpServletRequest request, JSONObject json) {
        try {
            Tbmuser tbmuser = ApiUtil.currentUser(json, request);
            if (tbmuser == null || tbmuser.getMahasiswa() == null || tbmuser.getMahasiswa().getId() == null) {
                return null;
            }

            Mahasiswa mahasiswa = tbmuser.getMahasiswa();
            List<String> warning = new ArrayList<String>();

            Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
            String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
            int currentSmt = Common.getSemester(tahunAngkatanMhs, semesterMulai,
                    mahasiswa.getPindahKeKampusIniMasukSemester(), mahasiswa.getSemesterMulai());
            JenisKegiatan.apakahBoleh(mahasiswa, currentSmt, warning);

            appendBlokirMahasiswaWarning(mahasiswa, warning);
            return buildWarningResponse(warning);
        } catch (Exception e) {
            return null;
        }
    }

    private static void appendBlokirMahasiswaWarning(Mahasiswa mahasiswa, List<String> warning) {
        if (mahasiswa == null || mahasiswa.getId() == null || warning == null) {
            return;
        }
        try {
            Map<Long, BlokirMahasiswa> blokirMap = ConstantValues.ambilBerdasarClass(BlokirMahasiswa.class);
            if (blokirMap == null || blokirMap.isEmpty()) {
                return;
            }
            for (Object o : blokirMap.values()) {
                if (!(o instanceof BlokirMahasiswa)) {
                    continue;
                }
                BlokirMahasiswa blokir = (BlokirMahasiswa) o;
                if (blokir != null && Boolean.TRUE.equals(blokir.getAktif()) && Boolean.TRUE.equals(blokir.getLogin())
                        && blokir.getMahasiswa() != null && mahasiswa.getId().equals(blokir.getMahasiswa().getId())
                        && ApiHelperSupport.hasText(blokir.getKeterangan())) {
                    warning.add(blokir.getKeterangan());
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/api/ApiAccessGuard.java:91");
        }
    }

    private static JSONObject buildWarningResponse(List<String> warning) {
        if (warning == null || warning.isEmpty()) {
            return null;
        }
        StringBuilder description = new StringBuilder();
        for (String item : warning) {
            if (!ApiHelperSupport.hasText(item)) {
                continue;
            }
            if (description.length() > 0) {
                description.append("\n\n\n");
            }
            description.append(item);
        }
        return description.length() == 0 ? null : ApiHelperSupport.status("91", description.toString());
    }
}
