package ais.common;

import org.hibernate.Session;

/**
 * Helper penutup session khusus modul e-Learning.
 * currentSession() tidak perlu ditutup manual. Helper ini dipakai untuk session
 * yang dibuat melalui openSession() atau currentNativeSession().
 */
public final class ElearningSessionUtil {
    private ElearningSessionUtil() {
    }

    public static void closeQuietly(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                try { session.clear(); } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ElearningSessionUtil.java:20"); }
                try { session.disconnect(); } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ElearningSessionUtil.java:21"); }
                try { session.close(); } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ElearningSessionUtil.java:22"); }
            }
        } catch (Throwable ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/common/ElearningSessionUtil.java:24");
        }
    }
}
