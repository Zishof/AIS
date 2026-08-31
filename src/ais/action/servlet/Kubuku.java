package ais.action.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sisdes.Penduduk;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;

/**
 * Servlet endpoint SSO (Single Sign-On) untuk integrasi pihak ketiga "Kubuku" (layanan
 * perpustakaan digital): memvalidasi kredensial {@code app_id}/{@code app_key} milik Kubuku,
 * lalu mengautentikasi pengguna akhir ({@code user id}/{@code password}) terhadap database lokal
 * dan mengembalikan profil ringkas dalam JSON agar Kubuku dapat login-kan pengguna tanpa
 * password terpisah.
 *
 * <p>
 * Hanya method {@link #doPost} yang diterima; {@link #doGet} selalu menolak dengan HTTP 405.
 * Pencarian pengguna berjenjang dan berhenti pada kecocokan pertama: {@link Mahasiswa} →
 * {@link Siswa} → {@link Tbmuser} (dosen/pegawai/admin, diperkaya dari relasi Dosen/Pegawai bila
 * ada) → {@link Penduduk}, masing-masing dicocokkan berdasarkan identitas (NIM/nomor induk/user
 * id/kode) dan password yang sudah dienkripsi dengan skema enkripsi sistem
 * ({@code Common#desEncrypter}) — bukan hash satu-arah, karena nilai terenkripsi dibandingkan
 * langsung terhadap kolom password tersimpan.
 * </p>
 *
 * <p>
 * <b>Catatan keamanan (dilaporkan, tidak diperbaiki sesuai instruksi tugas):</b> kredensial
 * integrasi ({@code app_id}/{@code app_key}) memiliki nilai default fallback tertanam di kode —
 * {@code "app_sso_kubuku"} dan {@code "iniKunciSSOkubuku"} — pada pemanggilan
 * {@code Common.getKonfigurasi(key, default)}. Bila konfigurasi database belum diisi ulang,
 * nilai default ini berlaku sebagai kredensial otorisasi endpoint SSO dan sudah ter-commit ke
 * source control; perlu ditinjau/dirotasi oleh pemilik integrasi.
 * </p>
 */
public class Kubuku extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** Konstruktor baku servlet, tanpa inisialisasi tambahan. */
    public Kubuku() {
        super();
    }

    /**
     * Menangani permintaan SSO dari Kubuku: memvalidasi {@code app_id}/{@code app_key}, lalu
     * mengautentikasi pengguna berjenjang (Mahasiswa/Siswa/Tbmuser/Penduduk) berdasarkan
     * identitas dan password terenkripsi, dan menulis hasil sebagai JSON (status 200 beserta
     * profil ringkas bila berhasil; 500 bila app_id/app_key tidak valid atau terjadi galat
     * internal; 404 bila pengguna tidak ditemukan/tidak aktif).
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // PERBAIKAN: Menggabungkan charset ke dalam setContentType untuk kompatibilitas Servlet API lama
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject jsonResponse = new JSONObject();
        
        try {
            // 1. Mengambil parameter dari HTTP POST 
            String appId = request.getParameter("app_id");
            String appKey = request.getParameter("app_key");
            
            // Dokumen Kubuku menuliskan "user id" (menggunakan spasi) 
            // Kita juga melakukan antisipasi fallback jika dikirim sebagai "user_id"
            String userId = request.getParameter("user id"); 
            if (userId == null || userId.trim().isEmpty()) {
                userId = request.getParameter("user_id");
            }
            String password = request.getParameter("password");
            String username = userId;
            
            // Mengambil IP Pengguna 
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty() || "unknown".equalsIgnoreCase(clientIp)) {
                clientIp = request.getRemoteAddr();
            }

            // 2. Validasi APP ID & APP KEY 
            Konfigurasi konfAppId = Common.getKonfigurasi("kubuku_app_id", "app_sso_kubuku");
            Konfigurasi konfAppKey = Common.getKonfigurasi("kubuku_app_key", "iniKunciSSOkubuku");
            
            if (appId == null || !appId.equals(konfAppId.getNilai()) || 
                appKey == null || !appKey.equals(konfAppKey.getNilai())) {
                
                jsonResponse.put("status", 500);
                jsonResponse.put("message", "App ID atau App Key tidak valid/Authorization Rejected");
                jsonResponse.put("ip", clientIp);
                out.print(jsonResponse.toString());
                return;
            }

            // 3. Autentikasi Pengguna ke Database
            Session session = null;
            try {
                session = HibernateUtil.getSessionFactory().openSession();
                // Enkripsi password menggunakan format sistem
                String mypassword = Common.desEncrypter.get().encrypt(password);
                
                // Pengecekan berjenjang: Mahasiswa -> Siswa -> Tbmuser (Dosen/Pegawai/Admin) -> Penduduk
                Mahasiswa mahasiswa = (Mahasiswa) ConstantValues.simpleObject(session.createCriteria(Mahasiswa.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.eq("nim", username)).add(Restrictions.eq("pass", mypassword)).setMaxResults(1),
                        Mahasiswa.class);
                
                Siswa siswa = null;
                Tbmuser tbmuser = null;
                Penduduk penduduk = null;
                
                if (mahasiswa == null) {
                    siswa = (Siswa) ConstantValues.simpleObject(session.createCriteria(Siswa.class)
                            .add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", ""))
                            .add(Restrictions.isNotNull("sekolah")).add(Restrictions.eq("nomorInduk", username))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("pass", mypassword)).setMaxResults(1), Siswa.class);
                }

                if (mahasiswa == null && siswa == null) {
                    tbmuser = (Tbmuser) ConstantValues.simpleObject(session.createCriteria(Tbmuser.class)
                                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                                            .add(Restrictions.eq("userId", username))
                                            .add(Restrictions.eq("userPassword", mypassword)).setMaxResults(1),
                                    Tbmuser.class);
                }

                if (mahasiswa == null && siswa == null && tbmuser == null) {
                    penduduk = (Penduduk) ConstantValues.simpleObject(session.createCriteria(Penduduk.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(Restrictions.eq("kode", username)).add(Restrictions.eq("pass", mypassword))
                            .setMaxResults(1), Penduduk.class);
                }

                // 4. Penentuan Response Berdasarkan Hasil Pencarian
                if (mahasiswa == null && siswa == null && tbmuser == null && penduduk == null) {
                    // Response 404: User Not Register (Atau statusnya tidak aktif)
                    jsonResponse.put("status", 404);
                    jsonResponse.put("message", "User Not register / Inactive");
                    jsonResponse.put("ip", clientIp);
                } else {
                    // Response 200: Login Berhasil 
                    jsonResponse.put("status", 200);
                    jsonResponse.put("message", "Login Success");
                    jsonResponse.put("ip", clientIp);
                    
                    // Menyiapkan variabel default data balasan 
                    String nama = "-";
                    String email = "";
                    String jenisKelamin = "L"; 
                    String prodi = "-";

                    // Mengekstrak detail mendalam sesuai entitas yang berhasil login
                    if (mahasiswa != null) {
                        nama = mahasiswa.getNama();
                        email = mahasiswa.getEmail();
                        jenisKelamin = mahasiswa.getKelamin() != null ? mahasiswa.getKelamin() : "L";
                        if (mahasiswa.getJurusan() != null) {
                            prodi = mahasiswa.getJurusan().getNama();
                        }
                    } else if (siswa != null) {
                        nama = siswa.getNama();
                        email = siswa.getAlamatEmail();
                        jenisKelamin = siswa.getJenisKelamin() != null ? siswa.getJenisKelamin() : "L";
                    } else if (tbmuser != null) {
                        // Tbmuser bisa jadi tertaut ke Dosen atau Pegawai
                        if (tbmuser.getDosen() != null) {
                            nama = tbmuser.getDosen().getNama();
                            email = tbmuser.getDosen().getEmail();
                            jenisKelamin = tbmuser.getDosen().getKelamin() != null ? tbmuser.getDosen().getKelamin() : "L";
                        } else if (tbmuser.getPegawai() != null) {
                            nama = tbmuser.getPegawai().getNama();
                            email = tbmuser.getPegawai().getEmail();
                            jenisKelamin = tbmuser.getPegawai().getKelamin() != null ? tbmuser.getPegawai().getKelamin() : "L";
                        } else {
                            nama = tbmuser.getUserNama() != null ? tbmuser.getUserNama() : tbmuser.getUserId();
                            email = tbmuser.getEmail();
                        }
                    } else if (penduduk != null) {
                        try {
                            nama = penduduk.getNama();
                        } catch (Exception e) {
                            nama = username;
                        }
                    }
                    
                    // Memasukkan data identitas ke dalam JSON 
                    jsonResponse.put("nama", nama != null ? nama : "-");
                    jsonResponse.put("email", email != null ? email : "");
                    jsonResponse.put("prodi", prodi != null ? prodi : "-");
                    
                    // Kita masukkan keduanya untuk memastikan tidak ada error parsing di server Kubuku.
                    jsonResponse.put("jenis kelamin", jenisKelamin); 
                    jsonResponse.put("jenis_kelamin", jenisKelamin);
                }
            } finally {
                if (session != null) {
                    try { session.clear(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Kubuku.java:177");}
                    try { session.disconnect(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Kubuku.java:178");}
                    try { session.close(); } catch (Exception e2) { ais.common.ErrorAuditUtil.record(e2, "auto-audit(empty-catch) src/ais/action/servlet/Kubuku.java:179");}
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/servlet/Kubuku.java:183");
            try {
                // Tangani Error sistem internal (Misal DB mati)
                jsonResponse.put("status", 500);
                jsonResponse.put("message", "Internal Server Error");
                jsonResponse.put("ip", request.getRemoteAddr());
            } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/servlet/Kubuku.java:189");}
        }

        out.print(jsonResponse.toString());
        out.flush();
    }

    /** Selalu menolak permintaan GET dengan HTTP 405 — endpoint SSO ini hanya menerima POST. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        response.getWriter().print("{\"status\": 500, \"message\": \"Method HTTP GET tidak diizinkan. Silakan gunakan HTTP POST.\"}");
    }
}