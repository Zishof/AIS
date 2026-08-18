package ais.action.servlet;

import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.koperasi.helper.LaporanKantinUtil;
import ais.action.master.koperasi.helper.LaporanKantinUtil.Hasil;
import ais.action.master.koperasi.helper.LaporanKantinUtil.Kolom;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * Servlet penyaji PDF untuk modul "Laporan-Laporan e-Kantin".
 *
 * Berbeda dari versi cetak-peramban (window.print) yang nomor halamannya bergantung pada
 * pengaturan skala cetak, servlet ini membuat PDF di SISI PELADEN dengan iText sehingga
 * penomoran halaman ("Halaman X / Y"), kop berulang, dan tata letak konsisten apa pun
 * peramban/skala. Data + aturan keamanan (lingkup toko pedagang) diambil dari
 * {@link LaporanKantinUtil} yang sama dengan endpoint JSON.
 *
 * URL: {ROOT}/LaporanKantinPdf?r=<idLaporan>&tokoId=&tglMulai=&tglSampai=&qProduk=&qPelanggan=
 *
 * Sesi Hibernate: memakai HibernateUtil.currentSession() (di dalam util) lalu DITUTUP via
 * HibernateUtil.closeSession() di finally, karena servlet ini bukan konteks ZK dan tidak ada
 * filter open-session-in-view (mencegah kebocoran koneksi pada thread pool Tomcat).
 *
 * Java 1.7 kompatibel; iText 5.0.3 (com.itextpdf).
 */
public class LaporanKantinPdf extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Locale ID = new Locale("id", "ID");

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
        doGet(req, resp);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
        try {
            Hasil H = LaporanKantinUtil.build(req);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=\"laporan-kantin.pdf\"");
            generate(req, H, resp.getOutputStream());
        } catch (Exception ex) {
            ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/servlet/LaporanKantinPdf.java:120");
        } finally {
            try { HibernateUtil.closeSession(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:122"); }
        }
    }

    /**
     * Inti pembuatan PDF (kop+tabel+subtotal+grand total via iText), DIEKSTRAK dari {@link #doGet}
     * supaya bisa dipakai ULANG oleh pemanggil non-servlet lain -- konkretnya {@code PosApi} (aksi
     * {@code laporan_pdf}, dipanggil aplikasi Desktop Electron lewat token, BUKAN cookie session)
     * yang butuh bytes PDF langsung (dibungkus base64 di JSON), bukan ditulis ke
     * {@code HttpServletResponse} secara langsung. {@link #doGet} tetap perilaku IDENTIK dgn
     * sebelumnya -- method ini murni pemindahan badan kode, bukan perubahan logika.
     *
     * @param req dipakai HANYA utk resolusi nama instansi/logo/scope toko (lihat
     *            {@link #resolveTokoTxt}) -- TIDAK dipakai utk query data (data sudah final di
     *            {@code H}, dibangun pemanggil lewat {@code LaporanKantinUtil.build}).
     * @param H   hasil {@code LaporanKantinUtil.build(...)} yang SUDAH dihitung pemanggil.
     * @param os  tujuan penulisan byte PDF -- TIDAK ditutup oleh method ini (pemanggil yang
     *            membuka/menutup stream, konsisten dgn {@code doGet} yg memakai
     *            {@code resp.getOutputStream()} apa adanya).
     */
    public void generate(HttpServletRequest req, Hasil H, OutputStream os) throws Exception {
        String instansi = "Laporan e-Kantin / Koperasi";
        try {
            Object pt = PerguruanTinggiUtil.getPerguruanTinggi(req);
            if (pt != null) {
                Object n = pt.getClass().getMethod("getNama").invoke(pt);
                if (n != null && n.toString().trim().length() > 0) instansi = n.toString().trim();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:77"); }
        // getPerguruanTinggiMedia(req, jenis) -- BUKAN getMedia(jenis) (versi ambient tanpa request,
        // yg bergantung ExecutionsCtrl/RequestContext dari konteks ZK/FilterJSP -- TIDAK ada di jalur
        // ini krn dipanggil dari servlet murni PosApi, bukan JSP/ZK. getMedia() sendiri sudah aman
        // (fallback logo.png bawaan bila null), tapi lewat req eksplisit di sini logo INSTANSI yang
        // sesungguhnya bisa ditemukan -- sama pola dgn laporan_laporan.jsp).
        String logoPath = null;
        try { logoPath = PerguruanTinggiUtil.getPerguruanTinggiMedia(req, "logo_perguruanTinggi_"); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:79"); }

        String tokoTxt = resolveTokoTxt(req);
        String periodeTxt = periode(req.getParameter("tglMulai"), req.getParameter("tglSampai"));
        String dicetak = "Dicetak: " + new SimpleDateFormat("dd-MM-yyyy HH:mm", ID).format(new java.util.Date());

        boolean landscape = H.kolom.size() > 6;
        Document doc = new Document(landscape ? PageSize.A4.rotate() : PageSize.A4, 30, 30, 96, 42);
        try {
            PdfWriter writer = PdfWriter.getInstance(doc, os);
            writer.setPageEvent(new KopFooter(instansi, H.judul, tokoTxt, periodeTxt, dicetak, logoPath));
            doc.open();

            if (!"00".equals(H.status)) {
                doc.add(new Paragraph("soon".equals(H.status)
                        ? "Laporan sedang disiapkan."
                        : ("Tidak dapat menampilkan laporan. " + H.message),
                        FontFactory.getFont(FontFactory.HELVETICA, 11)));
            } else if (H.baris.isEmpty()) {
                doc.add(new Paragraph("Tidak ada data untuk filter yang dipilih.",
                        FontFactory.getFont(FontFactory.HELVETICA, 11)));
            } else {
                if (H.catatan != null && H.catatan.length() > 0) {
                    Paragraph c = new Paragraph(H.catatan,
                            FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY));
                    c.setSpacingAfter(4f);
                    doc.add(c);
                }
                doc.add(buildTable(H));
            }
            doc.close();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/LaporanKantinPdf.java:115");
        } finally {
            try { os.flush(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:117"); }
        }
    }

    // ====================== Tabel ======================

    private PdfPTable buildTable(Hasil H) throws Exception {
        int n = H.kolom.size();
        PdfPTable table = new PdfPTable(n);
        table.setWidthPercentage(100);
        float[] wdt = new float[n];
        for (int i = 0; i < n; i++) {
            String t = H.kolom.get(i).tipe;
            wdt[i] = "text".equals(t) ? 2.6f : ("tgl".equals(t) ? 2.0f : 1.7f);
        }
        table.setWidths(wdt);
        table.setHeaderRows(1);

        Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, BaseColor.BLACK);
        for (int i = 0; i < n; i++) {
            Kolom k = H.kolom.get(i);
            PdfPCell c = new PdfPCell(new Phrase(k.label, hf));
            c.setHorizontalAlignment(alignOf(k.tipe));
            c.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
            c.setBorderWidthTop(1.2f); c.setBorderWidthBottom(1.2f);
            c.setBorderColor(BaseColor.BLACK);
            c.setPadding(4f);
            table.addCell(c);
        }

        NumberFormat amt = NumberFormat.getNumberInstance(ID); amt.setMinimumFractionDigits(2); amt.setMaximumFractionDigits(2);
        NumberFormat intf = NumberFormat.getNumberInstance(ID); intf.setMaximumFractionDigits(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", ID);
        Font bf = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, BaseColor.BLACK);
        Font gf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, BaseColor.BLACK);

        int gi = H.grup;
        String[] tipe = H.tipe;
        double[] grand = new double[n];
        boolean hasTotal = false;
        for (int i = 0; i < n; i++) if ("num".equals(tipe[i])) hasTotal = true;

        BaseColor lineBody = new BaseColor(229, 231, 235);
        BaseColor grpBg = new BaseColor(238, 243, 251);
        BaseColor subLine = new BaseColor(203, 213, 225);

        if (gi >= 0) {
            String curKey = null; boolean started = false; double[] sub = new double[n];
            for (int b = 0; b < H.baris.size(); b++) {
                Object[] row = H.baris.get(b);
                String key = row[gi] == null ? "" : row[gi].toString();
                if (!started || !key.equals(curKey)) {
                    if (started) addSubtotal(table, H, sub, curKey, gf, amt, intf, subLine);
                    curKey = key; started = true; sub = new double[n];
                    PdfPCell g = new PdfPCell(new Phrase(key, gf));
                    g.setColspan(n); g.setBackgroundColor(grpBg);
                    g.setBorder(Rectangle.TOP | Rectangle.BOTTOM); g.setBorderColor(subLine); g.setBorderWidth(0.6f);
                    g.setPadding(4f);
                    table.addCell(g);
                }
                addDetail(table, H, row, gi, sub, grand, bf, amt, intf, sdf, lineBody);
            }
            if (started) addSubtotal(table, H, sub, curKey, gf, amt, intf, subLine);
        } else {
            for (int b = 0; b < H.baris.size(); b++) {
                addDetail(table, H, H.baris.get(b), -1, null, grand, bf, amt, intf, sdf, lineBody);
            }
        }

        if (hasTotal && H.grandTotal) {
            for (int i = 0; i < n; i++) {
                Kolom k = H.kolom.get(i);
                String txt = (i == 0) ? "GRAND TOTAL" : ("num".equals(k.tipe) ? fmtNum(grand[i], k.label, amt, intf) : "");
                PdfPCell c = new PdfPCell(new Phrase(txt, gf));
                c.setHorizontalAlignment("num".equals(k.tipe) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                c.setBorder(Rectangle.TOP); c.setBorderWidthTop(1.5f); c.setBorderColor(BaseColor.BLACK);
                c.setPaddingTop(4f); c.setPaddingBottom(4f); c.setPaddingLeft(6f); c.setPaddingRight(6f);
                table.addCell(c);
            }
        }
        return table;
    }

    private void addDetail(PdfPTable table, Hasil H, Object[] row, int gi, double[] sub, double[] grand,
                           Font bf, NumberFormat amt, NumberFormat intf, SimpleDateFormat sdf, BaseColor lineBody) {
        int n = H.kolom.size();
        for (int i = 0; i < n; i++) {
            Kolom k = H.kolom.get(i);
            String t = k.tipe;
            Object v = i < row.length ? row[i] : null;
            String txt; int al;
            if (gi >= 0 && i == gi) {
                txt = ""; al = Element.ALIGN_LEFT;
            } else if ("num".equals(t)) {
                if (v == null) {
                    txt = ""; al = Element.ALIGN_RIGHT;
                } else {
                    double d = (v instanceof Number) ? ((Number) v).doubleValue() : 0d;
                    if (sub != null) sub[i] += d;
                    grand[i] += d;
                    txt = fmtNum(d, k.label, amt, intf); al = Element.ALIGN_RIGHT;
                }
            } else if ("tgl".equals(t)) {
                txt = (v instanceof java.util.Date) ? sdf.format((java.util.Date) v) : ""; al = Element.ALIGN_CENTER;
            } else {
                txt = (v == null) ? "" : v.toString(); al = Element.ALIGN_LEFT;
            }
            PdfPCell c = new PdfPCell(new Phrase(txt, bf));
            c.setHorizontalAlignment(al);
            c.setBorder(Rectangle.BOTTOM); c.setBorderWidthBottom(0.5f); c.setBorderColor(lineBody);
            c.setPaddingTop(3f); c.setPaddingBottom(3f); c.setPaddingLeft(6f); c.setPaddingRight(6f);
            table.addCell(c);
        }
    }

    private void addSubtotal(PdfPTable table, Hasil H, double[] sub, String key,
                             Font gf, NumberFormat amt, NumberFormat intf, BaseColor subLine) {
        int n = H.kolom.size(); int gi = H.grup;
        for (int i = 0; i < n; i++) {
            Kolom k = H.kolom.get(i);
            String txt; int al = Element.ALIGN_LEFT;
            if (i == gi) { txt = "Subtotal " + key; }
            else if ("num".equals(k.tipe)) { txt = fmtNum(sub[i], k.label, amt, intf); al = Element.ALIGN_RIGHT; }
            else { txt = ""; }
            PdfPCell c = new PdfPCell(new Phrase(txt, gf));
            c.setHorizontalAlignment(al);
            c.setBorder(Rectangle.TOP | Rectangle.BOTTOM); c.setBorderWidth(0.6f); c.setBorderColor(subLine);
            c.setPaddingTop(3f); c.setPaddingBottom(3f); c.setPaddingLeft(6f); c.setPaddingRight(6f);
            table.addCell(c);
        }
    }

    private static int alignOf(String tipe) {
        if ("num".equals(tipe)) return Element.ALIGN_RIGHT;
        if ("tgl".equals(tipe)) return Element.ALIGN_CENTER;
        return Element.ALIGN_LEFT;
    }

    private static boolean isCount(String label) {
        if (label == null) return false;
        String l = label.toLowerCase();
        return l.startsWith("jml") || l.startsWith("jumlah");
    }

    private static String fmtNum(double d, String label, NumberFormat amt, NumberFormat intf) {
        String s = (isCount(label) ? intf : amt).format(Math.abs(d));
        return d < 0 ? ("(" + s + ")") : s;
    }

    private String resolveTokoTxt(HttpServletRequest req) {
        try {
            Tbmuser u = Common.getCurrentUser(req);
            if (u != null && u.getPedagang() != null && u.getPedagang().getToko() != null) {
                String n = u.getPedagang().getToko().getNama();
                return n == null ? "-" : n;
            }
            String tp = req.getParameter("tokoId");
            if (tp != null && tp.trim().length() > 0) {
                org.hibernate.Session _s = null;
                try {
                    _s = HibernateUtil.getSessionFactory().openSession();
                    Object o = _s.get(Toko.class, Long.valueOf(tp.trim()));
                    if (o instanceof Toko) { String n = ((Toko) o).getNama(); return n == null ? ("Toko #" + tp) : n; }
                } finally {
                    try { if (_s != null) _s.clear(); } catch (Exception _e) { ais.common.ErrorAuditUtil.record(_e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:286");}
                    try { if (_s != null) _s.disconnect(); } catch (Exception _e) { ais.common.ErrorAuditUtil.record(_e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:287");}
                    try { if (_s != null) _s.close(); } catch (Exception _e) { ais.common.ErrorAuditUtil.record(_e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:288");}
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:291"); }
        return "Semua Toko";
    }

    private static String periode(String a, String b) {
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat out = new SimpleDateFormat("dd-MM-yyyy");
        boolean ha = a != null && a.trim().length() > 0;
        boolean hb = b != null && b.trim().length() > 0;
        if (!ha && !hb) return "Semua Periode";
        try {
            if (ha && hb) return out.format(in.parse(a.trim())) + " s/d " + out.format(in.parse(b.trim()));
            if (ha) return "Mulai " + out.format(in.parse(a.trim()));
            return "s/d " + out.format(in.parse(b.trim()));
        } catch (Exception e) { return "Semua Periode"; }
    }

    // ====================== Kop (tiap halaman) + Footer (nomor halaman) ======================

    private static final class KopFooter extends PdfPageEventHelper {
        private final String instansi, judul, toko, periode, dicetak, logoPath;
        private PdfTemplate total;
        private BaseFont base;
        private Image logo;
        private boolean logoTried = false;

        KopFooter(String instansi, String judul, String toko, String periode, String dicetak, String logoPath) {
            this.instansi = instansi; this.judul = judul; this.toko = toko;
            this.periode = periode; this.dicetak = dicetak; this.logoPath = logoPath;
        }

        public void onOpenDocument(PdfWriter writer, Document document) {
            try { base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:323"); }
            total = writer.getDirectContent().createTemplate(50, 14);
        }

        private Image logo() {
            if (!logoTried) {
                logoTried = true;
                try { if (logoPath != null && logoPath.trim().length() > 0) logo = Image.getInstance(logoPath); }
                catch (Exception e) { logo = null; }
            }
            return logo;
        }

        public void onStartPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                float top = document.top();
                float cx = (document.left() + document.right()) / 2f;

                Image lg = logo();
                if (lg != null) {
                    try {
                        float h = 34f;
                        float natH = lg.getHeight() <= 0 ? h : lg.getHeight();
                        float w = lg.getWidth() * (h / natH);
                        if (w > 150f) w = 150f;
                        lg.scaleAbsolute(w, h);
                        lg.setAbsolutePosition(cx - w / 2f, top + 42f);
                        cb.addImage(lg);
                    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:352"); }
                }
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(instansi.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)), cx, top + 30f, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(judul, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)), cx, top + 16f, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(toko + "  |  Periode: " + periode, FontFactory.getFont(FontFactory.HELVETICA, 8.5f, BaseColor.DARK_GRAY)),
                        cx, top + 4f, 0);
                cb.setLineWidth(1.4f);
                cb.setColorStroke(BaseColor.BLACK);
                cb.moveTo(document.left(), top + 1f);
                cb.lineTo(document.right(), top + 1f);
                cb.stroke();
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:366"); }
        }

        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte cb = writer.getDirectContent();
                float y = document.bottom() - 20f;
                cb.setLineWidth(0.5f);
                cb.setColorStroke(new BaseColor(209, 213, 219));
                cb.moveTo(document.left(), y + 10f);
                cb.lineTo(document.right(), y + 10f);
                cb.stroke();
                cb.setColorStroke(BaseColor.BLACK);

                Font ff = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(dicetak, ff), document.left(), y, 0);
                String txt = "Halaman " + writer.getPageNumber() + " / ";
                float tw = (base != null) ? base.getWidthPoint(txt, 8) : 40f;
                ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(txt, ff), document.right() - tw - 16f, y, 0);
                if (total != null) cb.addTemplate(total, document.right() - 15f, y);
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:386"); }
        }

        public void onCloseDocument(PdfWriter writer, Document document) {
            try {
                if (total != null && base != null) {
                    total.beginText();
                    total.setFontAndSize(base, 8);
                    total.setColorFill(BaseColor.GRAY);
                    total.setTextMatrix(0, 2);
                    total.showText(String.valueOf(writer.getPageNumber() - 1));
                    total.endText();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:399"); }
        }
    }
}
