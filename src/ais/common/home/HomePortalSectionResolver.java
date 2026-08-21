package ais.common.home;

import ais.common.Common;
import ais.database.model.Konfigurasi;

public class HomePortalSectionResolver {
    public String value(String key, String fallback) {
        try {
            Konfigurasi c = Common.getKonfigurasi(key, fallback);
            return c != null && c.getNilai() != null ? c.getNilai().trim() : fallback;
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "HomePortalSectionResolver.value " + key);
            return fallback;
        }
    }

    public boolean enabled(String key, boolean fallback) {
        String value = value(key, fallback ? Konfigurasi.AKTIF : Konfigurasi.TIDAK_AKTIF);
        return Konfigurasi.AKTIF.equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
    }
}
