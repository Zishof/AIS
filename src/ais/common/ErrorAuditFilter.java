package ais.common;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Filter global untuk menangkap error request Servlet/JSP/ZKoss.
 * Daftarkan di web.xml dengan url-pattern /* dan dispatcher REQUEST/FORWARD/INCLUDE/ERROR.
 */
public class ErrorAuditFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable throwable) {
            if (isSessionAlreadyInvalidatedNoise(throwable) || ErrorAuditUtil.isClientAbortIgnored(throwable)
                    || isZssSpreadsheetRenderNoise(throwable) || isTabExactlyOneSelectedNoise(throwable)
                    || isXmlValueDisconnectedNoise(throwable) || isZkComponentNotFoundNoise(throwable)) {
                return;
            }

            HttpServletRequest httpRequest = request instanceof HttpServletRequest ? (HttpServletRequest) request : null;
            String traceId = UUID.randomUUID().toString();
            ErrorAuditUtil.ErrorAuditResult audit = ErrorAuditUtil.recordVisibleFailure(throwable,
                    "Error tertangkap ErrorAuditFilter", httpRequest, traceId);
            if (httpRequest != null) {
                httpRequest.setAttribute("ais.error.trace", traceId);
                httpRequest.setAttribute("ais.error.content", audit == null ? null : audit.getContent());
                httpRequest.setAttribute("ais.error.log_id", audit == null ? null : audit.getErrorLogId());
                httpRequest.setAttribute("ais.error.throwable", throwable);
            }

            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            }
            if (throwable instanceof ServletException) {
                throw (ServletException) throwable;
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            }
            if (throwable instanceof Error) {
                throw (Error) throwable;
            }
            throw new ServletException(throwable);
        }
    }

    private boolean isZkComponentNotFoundNoise(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 20) {
            String name = t.getClass().getName();
            if ("org.zkoss.zk.ui.ComponentNotFoundException".equals(name)) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    /**
     * Noise BENIGN dari zss Spreadsheet: exception yang dilempar oleh event ASINKRON onSizeChange
     * (updateColWidth / updateRowHeight -> position helper) SETELAH sheet selesai dirender, saat
     * mengakses state POI yang sudah tidak konsisten (Book di-impor ulang/diganti). Bisa muncul
     * sebagai XmlValueDisconnectedException (CTRow/kolom yatim) ATAU IndexOutOfBoundsException
     * (kolom/baris di luar batas). Sheet SUDAH tampil ke user; penyesuaian lebar/tinggi susulan yang
     * gagal ini tidak memengaruhi data maupun tampilan yang sudah ada. Method zss terkait bersifat
     * private (tak bisa di-override) & stack tak punya frame ais.* (event async), sehingga penekanan
     * aman dilakukan di sini — SAMA seperti noise lain.
     *
     * Dibatasi KETAT ke JALUR RENDER size-change zss (InnerDataListener / onSizeChange /
     * updateColWidth / updateRowHeight / *PositionHelper*) agar TIDAK menelan error asli aplikasi
     * (mis. IndexOutOfBoundsException dari kode lain yang tidak lewat Spreadsheet).
     */
    private boolean isZssSpreadsheetRenderNoise(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 20) {
            if (adaFrameRenderSpreadsheetZss(t)) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    private boolean adaFrameRenderSpreadsheetZss(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        StackTraceElement[] frames = throwable.getStackTrace();
        if (frames == null) {
            return false;
        }
        for (int i = 0; i < frames.length; i++) {
            String cls = frames[i].getClassName();
            if (cls == null || !cls.startsWith("org.zkoss.zss.ui.Spreadsheet")) {
                continue;
            }
            // InnerDataListener = pembungkus event size-change asinkron zss.
            if (cls.indexOf("InnerDataListener") >= 0) {
                return true;
            }
            String mtd = frames[i].getMethodName();
            if ("onSizeChange".equals(mtd) || "updateColWidth".equals(mtd)
                    || "updateRowHeight".equals(mtd) || "getPositionHelpers".equals(mtd)
                    || "getColumnPositionHelper".equals(mtd) || "getRowPositionHelper".equals(mtd)
                    || "myGetColumnPositionHelper".equals(mtd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Noise BENIGN dari ZK Tabbox: {@code org.zkoss.zk.ui.UiException: Exactly one selected tab is
     * required: []}. Terjadi karena RACE tampilan murni sisi klien — klien mengirim onSelect untuk
     * sebuah tab, tetapi di server tabs sudah KOSONG (semua tab ditutup / tabbox dibangun ulang),
     * lalu klik/seleksi lama menyusul. Stack MURNI ZK ({@code org.zkoss.zul.Tab.service}) tanpa frame
     * ais.* sehingga tidak ada logika aplikasi yang bisa memperbaikinya di titik lempar; selain itu
     * bisa berasal dari Tab polos pihak-ketiga yang tak bisa kita subclass. Tidak ada dampak data
     * maupun tampilan (tab yang benar tetap terpilih setelah rebuild) → tekan di sini, konsisten
     * dengan pola noise lain. Dibatasi KETAT ke UiException berpesan persis agar tak menelan error lain.
     */
    private boolean isTabExactlyOneSelectedNoise(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 20) {
            String className = t.getClass() == null ? "" : t.getClass().getName();
            String message = t.getMessage();
            if (className.endsWith("UiException") && message != null
                    && message.indexOf("Exactly one selected tab is required") >= 0) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    private boolean isSessionAlreadyInvalidatedNoise(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 20) {
            String message = t.getMessage();
            String className = t.getClass() == null ? "" : t.getClass().getName();
            if (("java.lang.IllegalStateException".equals(className) || className.endsWith("IllegalStateException"))
                    && message != null) {
                String lower = message.toLowerCase();
                if (lower.indexOf("session already invalidated") >= 0
                        || lower.indexOf("response has been committed") >= 0
                        || lower.indexOf("cannot call sendredirect") >= 0) {
                    return true;
                }
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    /**
     * Noise BENIGN: XmlValueDisconnectedException dari XMLBeans/POI saat Spreadsheet ZSS
     * mengakses row/cell yang sudah di-garbage-collect. JVM fast-throw dapat mengosongkan
     * stack trace sehingga isZssSpreadsheetRenderNoise tidak dapat mendeteksi frame ZSS;
     * periksa juga nama kelas secara langsung untuk menutup celah tersebut.
     */
    private boolean isXmlValueDisconnectedNoise(Throwable throwable) {
        Throwable t = throwable;
        int guard = 0;
        while (t != null && guard < 20) {
            String className = t.getClass() == null ? "" : t.getClass().getName();
            if ("org.apache.xmlbeans.impl.values.XmlValueDisconnectedException".equals(className)) {
                return true;
            }
            t = t.getCause();
            guard++;
        }
        return false;
    }

    public void destroy() {
    }
}
