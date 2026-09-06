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

    /**
     * Versi serialisasi bawaan {@link HttpServlet}; tidak dipakai secara fungsional karena
     * instance servlet tidak pernah diserialisasi oleh kontainer pada penyebaran AIS.
     */
    private static final long serialVersionUID = 1L;
    /** Locale Indonesia dipakai memformat angka dan tanggal pada seluruh isi laporan. */
    private static final Locale ID = new Locale("id", "ID");

    /**
     * Menangani permintaan HTTP POST dengan perilaku identik {@link #doGet}.
     *
     * @param req  permintaan masuk berisi parameter filter laporan (lihat Javadoc kelas untuk
     *             daftar lengkap parameter URL)
     * @param resp balasan yang akan diisi berkas PDF laporan e-Kantin
     * @throws java.io.IOException bila penulisan balasan gagal
     * @see #doGet(HttpServletRequest, HttpServletResponse)
     */
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
        doGet(req, resp);
    }

    /**
     * Titik masuk servlet: membangun data+lingkup laporan lewat {@link LaporanKantinUtil#build},
     * menuliskan header respons PDF, lalu mendelegasikan perenderan ke {@link #generate}.
     *
     * <p>Sesi Hibernate yang dibuka {@code LaporanKantinUtil.build} (lewat
     * {@code HibernateUtil.currentSession()}) SELALU ditutup di blok {@code finally} lewat
     * {@code HibernateUtil.closeSession()} &mdash; lihat Javadoc kelas untuk alasannya (servlet
     * murni, bukan konteks ZK, tidak ada filter open-session-in-view).</p>
     *
     * <p>Kegagalan apa pun (termasuk kegagalan {@code LaporanKantinUtil.build} atau
     * {@link #generate}) ditelan: dicatat lewat {@code ErrorAuditUtil.record} dan
     * {@code printStackTrace}, tanpa balasan error terstruktur ke klien &mdash; permintaan yang
     * gagal menghasilkan berkas PDF kosong/tidak lengkap pada koneksi yang sudah terlanjur
     * dibuka {@code resp.getOutputStream()}.</p>
     *
     * @param req  permintaan masuk; parameter yang dipakai (lihat {@link LaporanKantinUtil#build}
     *             dan {@link #generate}) meliputi {@code r} (id laporan), {@code tokoId},
     *             {@code tglMulai}, {@code tglSampai}, {@code qProduk}, {@code qPelanggan}
     * @param resp balasan; diisi header {@code Content-Type: application/pdf} dan
     *             {@code Content-Disposition: inline} lalu bita PDF ditulis langsung ke
     *             {@code resp.getOutputStream()}
     * @throws java.io.IOException bila penulisan balasan gagal
     */
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

    /**
     * Membangun tabel utama laporan (header kolom, baris detail, subtotal per grup bila
     * {@code H.grup >= 0}, dan grand total di baris terakhir bila {@code H.grandTotal}).
     *
     * <p>Lebar kolom ditentukan otomatis dari {@code Kolom.tipe}: kolom teks lebih lebar
     * ({@code 2.6f}), kolom tanggal sedang ({@code 2.0f}), kolom lain (termasuk angka)
     * paling sempit ({@code 1.7f}).</p>
     *
     * @param H hasil {@code LaporanKantinUtil.build(...)} yang menyediakan kolom dan baris data
     * @return tabel iText siap ditambahkan ke {@link Document}
     * @throws Exception bila pembangunan sel/tabel iText gagal
     */
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

    /**
     * Menambahkan satu baris detail ke {@code table}: mem-format tiap sel sesuai tipe kolom
     * ({@code num} dijajarkan kanan dan diformat lewat {@link #fmtNum}, {@code tgl} dijajarkan
     * tengah dan diformat {@code dd-MM-yyyy}, selain itu teks apa adanya rata kiri), lalu
     * mengakumulasi nilai kolom {@code num} ke {@code sub} (subtotal grup berjalan, boleh
     * {@code null} bila tanpa pengelompokan) dan {@code grand} (grand total keseluruhan).
     *
     * <p>Kolom penanda grup ({@code i == gi}) sengaja dikosongkan di baris detail karena
     * nilainya sudah ditampilkan pada baris header grup oleh {@link #buildTable}.</p>
     *
     * @param table    tabel iText yang sedang dibangun; sel baru ditambahkan ke sini
     * @param H        hasil laporan, dipakai untuk mengetahui definisi kolom ({@code H.kolom})
     * @param row      satu baris data mentah dari {@code H.baris}
     * @param gi       indeks kolom pengelompokan, atau {@code -1} bila laporan tidak berkelompok
     * @param sub      akumulator subtotal grup berjalan (diperbarui in-place); {@code null} bila
     *                 tanpa pengelompokan
     * @param grand    akumulator grand total (diperbarui in-place)
     * @param bf       font isi sel
     * @param amt      pemformat angka desimal (dua digit di belakang koma)
     * @param intf     pemformat bilangan bulat (tanpa desimal)
     * @param sdf      pemformat tanggal {@code dd-MM-yyyy}
     * @param lineBody warna garis pemisah bawah tiap sel
     */
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

    /**
     * Menambahkan satu baris subtotal grup ke {@code table}: kolom pengelompokan ({@code i ==
     * gi}) diisi label {@code "Subtotal " + key}, kolom {@code num} diisi jumlah {@code sub}
     * yang sudah diakumulasi {@link #addDetail} selama grup berjalan, kolom lain dikosongkan.
     *
     * @param table  tabel iText yang sedang dibangun; sel baru ditambahkan ke sini
     * @param H      hasil laporan, dipakai untuk mengetahui definisi kolom ({@code H.kolom}) dan
     *               indeks kolom pengelompokan ({@code H.grup})
     * @param sub    akumulator subtotal grup yang baru saja selesai (dibaca, tidak diubah)
     * @param key    nilai kunci grup yang ditutup, ditampilkan pada label subtotal
     * @param gf     font tebal untuk baris subtotal
     * @param amt    pemformat angka desimal (dua digit di belakang koma)
     * @param intf   pemformat bilangan bulat (tanpa desimal)
     * @param subLine warna garis pembatas atas/bawah baris subtotal
     */
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

    /**
     * Menentukan perataan horizontal sel header berdasarkan tipe kolom: {@code num} rata kanan,
     * {@code tgl} rata tengah, tipe lain (termasuk teks) rata kiri.
     *
     * @param tipe tipe kolom ({@code Kolom.tipe}): {@code "num"}, {@code "tgl"}, atau lainnya
     * @return salah satu konstanta perataan {@link Element} ({@code ALIGN_RIGHT}/
     *         {@code ALIGN_CENTER}/{@code ALIGN_LEFT})
     */
    private static int alignOf(String tipe) {
        if ("num".equals(tipe)) return Element.ALIGN_RIGHT;
        if ("tgl".equals(tipe)) return Element.ALIGN_CENTER;
        return Element.ALIGN_LEFT;
    }

    /**
     * Menebak apakah sebuah label kolom angka merupakan cacahan (hitungan unit, bukan nilai
     * uang) dari awalan labelnya, tanpa peka besar/kecil huruf.
     *
     * @param label label kolom, mis. {@code "Jumlah Item"} atau {@code "Jml Transaksi"}
     * @return {@code true} bila label berawalan {@code "jml"} atau {@code "jumlah"}; {@code false}
     *         bila {@code label} bernilai {@code null} atau tidak cocok pola tersebut
     */
    private static boolean isCount(String label) {
        if (label == null) return false;
        String l = label.toLowerCase();
        return l.startsWith("jml") || l.startsWith("jumlah");
    }

    /**
     * Memformat sebuah nilai angka untuk ditampilkan pada tabel: cacahan ({@link #isCount}
     * bernilai {@code true} untuk {@code label}) diformat sebagai bilangan bulat lewat
     * {@code intf}, nilai lain (mis. nominal uang) diformat dua digit desimal lewat
     * {@code amt}; nilai negatif dibungkus tanda kurung (notasi akuntansi) alih-alih tanda
     * minus.
     *
     * @param d    nilai yang diformat
     * @param label label kolom, dipakai untuk memilih pemformat lewat {@link #isCount}
     * @param amt  pemformat angka desimal (dua digit di belakang koma)
     * @param intf pemformat bilangan bulat (tanpa desimal)
     * @return representasi teks nilai {@code d}, dibungkus tanda kurung bila negatif
     */
    private static String fmtNum(double d, String label, NumberFormat amt, NumberFormat intf) {
        String s = (isCount(label) ? intf : amt).format(Math.abs(d));
        return d < 0 ? ("(" + s + ")") : s;
    }

    /**
     * Menentukan teks nama toko yang ditampilkan pada kop laporan ({@code KopFooter}).
     *
     * <p>Urutan resolusi: (1) bila pengguna login ({@code Common.getCurrentUser(req)}) adalah
     * pedagang dengan toko terpasang, nama toko pedagang itu dipakai (mengikat kop pada
     * identitas login, bukan input request); (2) bila tidak, dan parameter {@code tokoId} pada
     * request terisi, toko dicari lewat id tersebut dan namanya dipakai bila ditemukan; (3) bila
     * keduanya tidak menghasilkan toko, teks bawaan {@code "Semua Toko"} dipakai.</p>
     *
     * <p>Method ini HANYA menentukan teks tampilan pada kop halaman; scoping data laporan yang
     * sesungguhnya (baris mana yang boleh muncul di tabel) ditentukan terpisah oleh
     * {@link LaporanKantinUtil#build}, bukan oleh method ini.</p>
     *
     * @param req permintaan asal, dipakai membaca pengguna login dan parameter {@code tokoId}
     * @return nama toko untuk ditampilkan, atau {@code "Semua Toko"} bila tidak dapat ditentukan
     */
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

    /**
     * Memformat rentang tanggal filter laporan menjadi teks siap tampil pada kop halaman.
     *
     * <p>Kedua parameter diharapkan berformat {@code yyyy-MM-dd} (format input filter web) dan
     * ditampilkan ulang sebagai {@code dd-MM-yyyy}. Kombinasi nilai kosong/terisi menentukan
     * bentuk teks: keduanya kosong &rarr; {@code "Semua Periode"}; keduanya terisi &rarr;
     * {@code "<a> s/d <b>"}; hanya {@code a} &rarr; {@code "Mulai <a>"}; hanya {@code b} &rarr;
     * {@code "s/d <b>"}. Kegagalan parsing tanggal apa pun (format tak dikenal) jatuh kembali
     * ke {@code "Semua Periode"} alih-alih melempar galat ke pemanggil.</p>
     *
     * @param a tanggal mulai filter ({@code yyyy-MM-dd}), atau {@code null}/kosong bila tidak
     *          difilter
     * @param b tanggal akhir filter ({@code yyyy-MM-dd}), atau {@code null}/kosong bila tidak
     *          difilter
     * @return teks periode siap tampil pada kop laporan
     */
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

    /**
     * Tipe implementasi bersarang {@link KopFooter} milik {@link LaporanKantinPdf}. Kelas ini memberi nama pada
     * state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link LaporanKantinPdf}.
     * Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan dan diuji.</p> Tipe ini
     * merupakan detail implementasi privat; pemanggil luar harus memakai API kelas induk.
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String instansi}, {@code String
     * judul}, {@code String toko}, {@code String periode}, {@code String dicetak}, {@code String logoPath}, {@code
     * PdfTemplate total}, {@code BaseFont base}; operasi lokal: {@code onOpenDocument()}, {@code logo()}, {@code
     * onStartPage()}, {@code onEndPage()}, {@code onCloseDocument}(). Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see LaporanKantinPdf
     */
    private static final class KopFooter extends PdfPageEventHelper {
        /** Nama instansi/koperasi ditampilkan di baris pertama kop, huruf besar semua. */
        private final String instansi;
        /** Judul laporan ({@code Hasil.judul}), ditampilkan di baris kedua kop. */
        private final String judul;
        /** Teks nama toko (hasil {@link #resolveTokoTxt}), ditampilkan pada baris ketiga kop. */
        private final String toko;
        /** Teks rentang periode filter (hasil {@link #periode}), ditampilkan pada baris ketiga kop. */
        private final String periode;
        /** Teks tanggal+jam cetak, ditampilkan rata kiri pada footer tiap halaman. */
        private final String dicetak;
        /** Path berkas logo instansi, atau {@code null} bila tidak ada logo untuk ditampilkan. */
        private final String logoPath;
        /** Templat iText tempat total jumlah halaman dituliskan belakangan di {@link #onCloseDocument}. */
        private PdfTemplate total;
        /** Font dasar dipakai menghitung lebar teks nomor halaman pada footer. */
        private BaseFont base;
        /** Gambar logo yang sudah dimuat dari {@link #logoPath}, atau {@code null} bila gagal/tidak ada. */
        private Image logo;
        /** Penanda agar pemuatan {@link #logo} dari {@link #logoPath} hanya dicoba sekali per dokumen. */
        private boolean logoTried = false;

        /**
         * Membangun event helper kop+footer dengan seluruh teks statis yang akan dicetak
         * berulang pada tiap halaman laporan.
         *
         * @param instansi nama instansi/koperasi untuk baris pertama kop
         * @param judul    judul laporan untuk baris kedua kop
         * @param toko     teks nama toko untuk baris ketiga kop
         * @param periode  teks rentang periode filter untuk baris ketiga kop
         * @param dicetak  teks tanggal+jam cetak untuk footer
         * @param logoPath path berkas logo instansi, atau {@code null} bila tidak ada
         */
        KopFooter(String instansi, String judul, String toko, String periode, String dicetak, String logoPath) {
            this.instansi = instansi; this.judul = judul; this.toko = toko;
            this.periode = periode; this.dicetak = dicetak; this.logoPath = logoPath;
        }

        /**
         * Dipanggil iText sekali saat dokumen dibuka: menyiapkan {@link #base} (font pengukur
         * lebar teks) dan {@link #total} (templat kosong berukuran 50x14pt yang akan diisi
         * jumlah halaman final di {@link #onCloseDocument}, karena jumlah itu baru diketahui
         * setelah seluruh halaman selesai dirender).
         *
         * <p>Kegagalan pembuatan {@link #base} ditelan; footer tetap dicetak tanpa nomor
         * halaman total bila ini terjadi.</p>
         *
         * @param writer   penulis PDF aktif
         * @param document dokumen yang baru dibuka
         */
        public void onOpenDocument(PdfWriter writer, Document document) {
            try { base = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/servlet/LaporanKantinPdf.java:323"); }
            total = writer.getDirectContent().createTemplate(50, 14);
        }

        /**
         * Memuat lazily gambar logo dari {@link #logoPath}, hanya mencoba sekali per dokumen
         * (ditandai {@link #logoTried}) agar kegagalan pemuatan tidak diulang pada tiap halaman.
         *
         * @return gambar logo, atau {@code null} bila {@link #logoPath} kosong/{@code null} atau
         *         pemuatannya gagal
         */
        private Image logo() {
            if (!logoTried) {
                logoTried = true;
                try { if (logoPath != null && logoPath.trim().length() > 0) logo = Image.getInstance(logoPath); }
                catch (Exception e) { logo = null; }
            }
            return logo;
        }

        /**
         * Dipanggil iText di awal tiap halaman: menggambar logo (bila ada, dilebarkan
         * proporsional maksimum 150pt dengan tinggi tetap 34pt) diikuti tiga baris teks kop
         * (instansi, judul, toko+periode) dan sebuah garis pemisah tebal di bawahnya.
         *
         * <p>Seluruh operasi digambar langsung ke {@code PdfContentByte} dokumen; kegagalan
         * apa pun (termasuk kegagalan menggambar logo) ditelan agar satu halaman yang
         * bermasalah tidak menggagalkan seluruh dokumen.</p>
         *
         * @param writer   penulis PDF aktif
         * @param document dokumen yang sedang memulai halaman baru
         */
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

        /**
         * Dipanggil iText di akhir tiap halaman: menggambar garis pemisah tipis, teks
         * {@link #dicetak} rata kiri, teks {@code "Halaman X / "} diikuti templat
         * {@link #total} (diisi belakangan di {@link #onCloseDocument}) rata kanan.
         *
         * <p>Kegagalan apa pun ditelan agar satu halaman yang bermasalah tidak menggagalkan
         * seluruh dokumen; footer halaman itu bisa jadi tidak lengkap bila ini terjadi.</p>
         *
         * @param writer   penulis PDF aktif
         * @param document dokumen yang halamannya baru selesai
         */
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

        /**
         * Dipanggil iText sekali saat dokumen ditutup, setelah jumlah halaman final diketahui:
         * menuliskan jumlah halaman itu (nomor halaman terakhir dikurangi satu, karena
         * {@code writer.getPageNumber()} pada titik ini sudah menghitung halaman "berikutnya"
         * yang tidak pernah dibuka) ke {@link #total}, templat yang sudah ditempatkan pada
         * footer tiap halaman oleh {@link #onEndPage}.
         *
         * <p>Kegagalan apa pun ditelan; nomor total halaman pada footer bisa jadi tidak
         * tercetak bila ini terjadi, tanpa menggagalkan penutupan dokumen.</p>
         *
         * @param writer   penulis PDF aktif
         * @param document dokumen yang baru ditutup
         */
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
