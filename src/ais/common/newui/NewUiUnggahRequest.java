package ais.common.newui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Pembungkus permintaan {@code multipart/form-data} untuk jalur native.
 *
 * <h3>Mengapa diurai sendiri, bukan dengan {@code request.getPart()}</h3>
 * <p>{@code getPart()} dan {@code @MultipartConfig} adalah Servlet 3.0.
 * {@code web.xml} aplikasi ini menyatakan {@code version="2.5"}, sehingga
 * container menjalankan semantik 2.5 dan kedua fasilitas itu tidak tersedia —
 * bahkan {@code getParameter()} tidak akan melihat field apa pun dari badan
 * multipart. Menaikkan versi deskriptor menyentuh perilaku seluruh aplikasi
 * untuk satu fitur; commons-fileupload sudah ada di classpath dan menyelesaikan
 * hal yang sama tanpa menyentuh apa pun di luar jalur ini.</p>
 *
 * <h3>Mengapa dibungkus, bukan dibaca di tempat</h3>
 * <p>Badan permintaan hanya dapat dibaca sekali. Setelah diurai, seluruh
 * pemanggil di hilir — servlet gerbang, penjaga, index.jsp, controller — masih
 * memanggil {@code getParameter()} seperti biasa. Membungkusnya membuat mereka
 * tidak perlu tahu bahwa permintaan ini datang sebagai multipart; tanpa itu,
 * setiap lapisan harus ditulis dua kali.</p>
 *
 * <h3>Batas yang disengaja</h3>
 * <ul>
 *   <li><b>Satu berkas per permintaan.</b> Import Feeder tetap wajib .xlsx;
 *       controller lampiran boleh menerima dokumen lain setelah jenis berkas
 *       berbahaya ditolak dan batas ukuran diverifikasi kembali.</li>
 *   <li><b>Batas ukuran tegas.</b> Tanpa batas, satu permintaan dapat mengisi
 *       disk server. Berkas yang melampauinya ditolak sebagai galat, bukan
 *       dipotong diam-diam.</li>
 *   <li><b>Berkas sementara tidak dihapus di sini.</b> Pekerjaan yang
 *       memprosesnya berjalan melampaui umur permintaan; pemiliknyalah yang
 *       menghapus. Lihat pemanggil.</li>
 * </ul>
 */
public final class NewUiUnggahRequest extends HttpServletRequestWrapper {

    /**
     * Batas ukuran berkas unggahan.
     *
     * <p>Enam puluh empat megabita: berkas templat PDDIKTI terbesar yang pernah
     * ditemui jauh di bawahnya, dan angka ini masih memberi ruang bagi lembaga
     * dengan jumlah mahasiswa besar tanpa membuat satu permintaan sanggup
     * menghabiskan disk.</p>
     */
    public static final long BATAS_UKURAN = 64L * 1024L * 1024L;

    /** Ambang penyimpanan ke disk; di bawah ini bagian ditahan di memori. */
    private static final int AMBANG_MEMORI = 256 * 1024;

    private final Map<String, String[]> field;
    private final File berkas;
    private final String namaBerkas;
    private final String mimeType;

    private NewUiUnggahRequest(HttpServletRequest asli, Map<String, String[]> field,
            File berkas, String namaBerkas, String mimeType) {
        super(asli);
        this.field = field;
        this.berkas = berkas;
        this.namaBerkas = namaBerkas;
        this.mimeType = mimeType;
    }

    /** Apakah permintaan ini membawa badan multipart. */
    public static boolean multipart(HttpServletRequest request) {
        return request != null && "POST".equalsIgnoreCase(request.getMethod())
                && ServletFileUpload.isMultipartContent(request);
    }

    /**
     * Urai permintaan multipart menjadi pembungkus yang berperilaku seperti
     * permintaan form biasa.
     *
     * @throws IllegalArgumentException bila berkasnya melampaui batas, tidak
     *         sesuai konteks aksi, berbahaya, atau tidak ada sama sekali
     */
    public static NewUiUnggahRequest urai(HttpServletRequest request) throws Exception {
        DiskFileItemFactory pabrik = new DiskFileItemFactory();
        pabrik.setSizeThreshold(AMBANG_MEMORI);
        ServletFileUpload pengurai = new ServletFileUpload(pabrik);
        pengurai.setFileSizeMax(BATAS_UKURAN);
        pengurai.setSizeMax(BATAS_UKURAN + (1024L * 1024L));
        pengurai.setHeaderEncoding("UTF-8");

        Map<String, List<String>> kumpul = new HashMap<String, List<String>>();
        File berkas = null;
        String namaBerkas = "";
        String mimeType = "application/octet-stream";

        List<?> bagian = pengurai.parseRequest(request);
        for (int i = 0; i < bagian.size(); i++) {
            FileItem item = (FileItem) bagian.get(i);
            if (item.isFormField()) {
                List<String> nilai = kumpul.get(item.getFieldName());
                if (nilai == null) {
                    nilai = new ArrayList<String>();
                    kumpul.put(item.getFieldName(), nilai);
                }
                nilai.add(item.getString("UTF-8"));
                continue;
            }
            if (item.getName() == null || item.getName().trim().length() == 0) continue;
            if (berkas != null) {
                hapusSementara(berkas);
                throw new IllegalArgumentException("Kirim satu berkas saja dalam satu permintaan.");
            }
            namaBerkas = namaAman(item.getName());
            mimeType = item.getContentType() == null || item.getContentType().trim().length() == 0
                    ? "application/octet-stream" : item.getContentType().trim();
            berkas = File.createTempFile("nui_unggah_", ".tmp");
            item.write(berkas);
            item.delete();
        }

        Map<String, String[]> field = new HashMap<String, String[]>();
        for (Map.Entry<String, List<String>> e : kumpul.entrySet()) {
            field.put(e.getKey(), e.getValue().toArray(new String[e.getValue().size()]));
        }
        if (berkas == null) {
            throw new IllegalArgumentException("Berkas unggahan wajib dipilih.");
        }
        String[] action = field.get("action");
        String aksi = action == null || action.length == 0 ? "" : action[0];
        if ("import_mulai".equalsIgnoreCase(aksi)
                && !namaBerkas.toLowerCase().endsWith(".xlsx")) {
            hapusSementara(berkas);
            throw new IllegalArgumentException("Berkas impor harus berformat .xlsx.");
        }
        if (ekstensiBerbahaya(namaBerkas)) {
            hapusSementara(berkas);
            throw new IllegalArgumentException(
                    "Jenis berkas ini tidak diizinkan. Kompres berkas menjadi .zip atau .rar terlebih dahulu.");
        }
        return new NewUiUnggahRequest(request, field, berkas, namaBerkas, mimeType);
    }

    /** Sama dengan penjaga upload ZK: berkas yang dapat dieksekusi browser/server ditolak. */
    private static boolean ekstensiBerbahaya(String nama) {
        String v = nama == null ? "" : nama.toLowerCase();
        return v.endsWith(".jsp") || v.endsWith(".jspx") || v.endsWith(".zul")
                || v.endsWith(".html") || v.endsWith(".htm") || v.endsWith(".exe")
                || v.endsWith(".sh") || v.endsWith(".php");
    }

    private static void hapusSementara(File file) {
        try { if (file != null && file.exists()) file.delete(); }
        catch (Exception ignored) { }
    }

    /**
     * Nama berkas tanpa jalur.
     *
     * <p>Sebagian klien mengirim nama lengkap beserta direktorinya. Nama itu
     * hanya dipakai untuk ditampilkan dan dicatat — berkasnya sendiri selalu
     * ditulis ke nama sementara yang dibuat server — tetapi jalur dari klien
     * tetap dibuang di sini supaya tidak ada pemakai berikutnya yang tergoda
     * memperlakukannya sebagai lokasi.</p>
     */
    private static String namaAman(String nama) {
        String v = nama.replace('\\', '/');
        int garis = v.lastIndexOf('/');
        if (garis >= 0) v = v.substring(garis + 1);
        return v.trim();
    }

    /** Berkas unggahan, atau null bila permintaan tidak membawanya. */
    public File getBerkas() {
        return berkas;
    }

    /** Nama berkas sebagaimana disebut klien; hanya untuk ditampilkan. */
    public String getNamaBerkas() {
        return namaBerkas;
    }

    /** MIME yang dilaporkan klien; controller tetap wajib melakukan validasi konteksnya. */
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getParameter(String nama) {
        String[] v = field.get(nama);
        if (v != null && v.length > 0) return v[0];
        // Parameter query string tetap terbaca: gerbang native menambahkan
        // service/menuId lewat query saat meneruskan permintaan.
        return super.getParameter(nama);
    }

    @Override
    public String[] getParameterValues(String nama) {
        String[] v = field.get(nama);
        return v != null ? v : super.getParameterValues(nama);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        List<String> nama = new ArrayList<String>(field.keySet());
        Enumeration<?> bawaan = super.getParameterNames();
        while (bawaan.hasMoreElements()) {
            String n = String.valueOf(bawaan.nextElement());
            if (!nama.contains(n)) nama.add(n);
        }
        return Collections.enumeration(nama);
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map getParameterMap() {
        Map<String, String[]> gabung = new HashMap<String, String[]>();
        Map<?, ?> bawaan = super.getParameterMap();
        for (Map.Entry<?, ?> e : bawaan.entrySet()) {
            gabung.put(String.valueOf(e.getKey()), (String[]) e.getValue());
        }
        gabung.putAll(field);
        return gabung;
    }

    /**
     * Tipe isi dilaporkan sebagai form biasa.
     *
     * <p>Badannya sudah diurai dan tidak dapat dibaca lagi. Membiarkan tipe
     * aslinya membuat lapisan di hilir mengira masih ada multipart yang bisa
     * diurai, lalu mendapat badan kosong.</p>
     */
    @Override
    public String getContentType() {
        return "application/x-www-form-urlencoded; charset=UTF-8";
    }

    @Override
    public javax.servlet.ServletInputStream getInputStream() throws IOException {
        throw new IOException("Badan permintaan multipart sudah diurai.");
    }
}
