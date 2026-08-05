package ais.database.hibernate;

/**
 * Backward-compatible wrapper.
 *
 * Konfigurasi lama memakai GlobalAuditInterceptor. Logic utama dipusatkan di
 * AuditTimestampInterceptor agar tidak ada dua implementasi berbeda yang saling bertabrakan.
 */
public class GlobalAuditInterceptor extends AuditTimestampInterceptor {
    private static final long serialVersionUID = 1L;
}
