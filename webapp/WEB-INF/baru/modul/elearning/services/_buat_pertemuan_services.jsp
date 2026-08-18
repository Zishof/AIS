<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="org.hibernate.Session"%>
<%@ page import="org.hibernate.criterion.Restrictions"%>
<%@ page import="ais.database.hibernate.HibernateUtil"%>
<%@ page import="ais.common.Common"%>
<%@ page import="ais.common.ConstantValues"%>
<%@ page import="ais.database.model.*"%>

<%
    // Hanya menerima permintaan POST untuk alasan keamanan
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        return;
    }
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    
    String action = request.getParameter("action");
    Session sess = null;

    // Format tanggal baku masukan HTML5 (<input type="date">)
    SimpleDateFormat html5DateFormat = new SimpleDateFormat("yyyy-MM-dd");

    try {
        sess = HibernateUtil.openSession();
        sess.getTransaction().begin();

        // --------------------------------------------------------------------------------
        // AKSI 1: SINKRONISASI STATUS CACHE (Membersihkan Singgahan Java)
        // --------------------------------------------------------------------------------
        if ("sync_state".equals(action)) {
            Long idPerkuliahan = Long.parseLong(request.getParameter("idPerkuliahan"));
            Perkuliahan perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, idPerkuliahan);
            
            if (perkuliahan != null) {
                // Membersihkan singgahan (cache) jadwal di tingkat objek Java.
                // PERBAIKAN: Kita TIDAK LAGI memanggil reInitPertemuan atau ambilPertemuan di sini.
                // Memuat seluruh relasi file tugas/ujian di dalam blok transaksi aktif ini 
                // adalah penyebab utama Deadlock atau I/O Timeout (SQLState: 57P01 / 08006).
                perkuliahan.belum(); 
                
                out.print("{\"status\":\"ok\"}");
            } else {
                out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Data perkuliahan tidak ditemukan.") + "\"}");
            }
        }
        
        // --------------------------------------------------------------------------------
        // AKSI 2: TAMBAH SATU JADWAL PERTEMUAN
        // --------------------------------------------------------------------------------
        else if ("add_one".equals(action)) {
            Long idPerkuliahan = Long.parseLong(request.getParameter("idPerkuliahan"));
            Perkuliahan perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, idPerkuliahan);
            
            if (perkuliahan != null) {
                Pertemuan pert = new Pertemuan();
                pert.setPerkuliahan(perkuliahan);
                pert.setRuang(perkuliahan.getRuang());
                
                String tgl = request.getParameter("add_tanggal");
                if (tgl != null && !tgl.trim().isEmpty()) {
                    Date parsedDt = html5DateFormat.parse(tgl);
                    pert.setTanggal(parsedDt);
                    pert.setMulai(parsedDt);
                }
                
                pert.setWaktuMulai(request.getParameter("add_waktuMulai"));
                pert.setWaktuSelesai(request.getParameter("add_waktuSelesai"));
                pert.setTopik(request.getParameter("add_topik"));
                pert.setAktif(true);
                
                String st = request.getParameter("add_statusPertemuan");
                if (st != null && !st.trim().isEmpty()) {
                    StatusPertemuan sp = (StatusPertemuan) sess.get(StatusPertemuan.class, Long.parseLong(st));
                    pert.setStatusPertemuan(sp);
                }
                
                // Tentukan nomor urut pertemuan tertinggi saat ini
                Integer maxKe = (Integer) sess.createCriteria(Pertemuan.class)
                    .add(Restrictions.eq("perkuliahan", perkuliahan))
                    .setProjection(org.hibernate.criterion.Projections.max("pertemuanKe"))
                    .uniqueResult();
                pert.setPertemuanKe(maxKe != null ? maxKe + 1 : 1);
                
                sess.save(pert);
                perkuliahan.belum(); // Bersihkan cache agar pemanggilan selanjutnya memuat data baru
                
                out.print("{\"status\":\"ok\"}");
            } else {
                out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Data perkuliahan tidak ditemukan.") + "\"}");
            }
        }

        // --------------------------------------------------------------------------------
        // AKSI 3: PENGHASILAN JADWAL SECARA MASSAL (GENERATE MASSAL)
        // --------------------------------------------------------------------------------
        else if ("generate_massal".equals(action)) {
            Long idPerkuliahan = Long.parseLong(request.getParameter("idPerkuliahan"));
            Perkuliahan perkuliahan = (Perkuliahan) sess.get(Perkuliahan.class, idPerkuliahan);
            
            if (perkuliahan != null) {
                String tglMulaiStr = request.getParameter("tanggalMulai");
                if (tglMulaiStr != null && !tglMulaiStr.trim().isEmpty()) {
                    perkuliahan.setTanggalMulaiPerkuliahan(html5DateFormat.parse(tglMulaiStr));
                }

                perkuliahan.setWaktuMulai(request.getParameter("waktuMulai"));
                perkuliahan.setWaktuSelesai(request.getParameter("waktuSelesai"));
                perkuliahan.setJenis(request.getParameter("jenisPertemuan"));
                sess.update(perkuliahan);
                
                int maxPertemuan = perkuliahan.getJumlahMaksimalPertemuan() != null ? perkuliahan.getJumlahMaksimalPertemuan() : 16;
                String jenis = perkuliahan.getJenis() != null ? perkuliahan.getJenis() : "Mingguan";

                Calendar cal = Calendar.getInstance();
                if (perkuliahan.getTanggalMulaiPerkuliahan() != null) {
                    cal.setTime(perkuliahan.getTanggalMulaiPerkuliahan());
                }

                for (int i = 1; i <= maxPertemuan; i++) {
                    Date currDate = cal.getTime();
                    
                    // Verifikasi ketersediaan jadwal pada hari yang sama agar tidak berganda
                    Pertemuan cekPertemuan = (Pertemuan) sess.createCriteria(Pertemuan.class)
                        .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                        .add(Restrictions.eq("perkuliahan", perkuliahan))
                        .add(Restrictions.eq("tanggal", currDate))
                        .setMaxResults(1).uniqueResult();

                    if (cekPertemuan == null) {
                        Pertemuan pert = new Pertemuan();
                        pert.setPertemuanKe(i);
                        pert.setPerkuliahan(perkuliahan);
                        pert.setTanggal(currDate);
                        pert.setMulai(currDate);
                        pert.setWaktuMulai(perkuliahan.getWaktuMulai());
                        pert.setWaktuSelesai(perkuliahan.getWaktuSelesai());
                        pert.setRuang(perkuliahan.getRuang());
                        pert.setStatusPertemuan(ConstantValues.TATAP_MUKA);
                        pert.setAktif(true);
                        sess.save(pert);
                    }
                    
                    // Kalkulasi putaran waktu pertemuan selanjutnya sesuai jenis siklus
                    if ("Harian".equalsIgnoreCase(jenis)) cal.add(Calendar.DATE, 1);
                    else if ("2 Harian".equalsIgnoreCase(jenis) || "Tgl Ganjil".equalsIgnoreCase(jenis) || "Tgl Genap".equalsIgnoreCase(jenis)) cal.add(Calendar.DATE, 2);
                    else if ("3 Harian".equalsIgnoreCase(jenis)) cal.add(Calendar.DATE, 3);
                    else if ("Mingguan".equalsIgnoreCase(jenis)) cal.add(Calendar.DATE, 7);
                    else if ("2 Mingguan".equalsIgnoreCase(jenis)) cal.add(Calendar.DATE, 14);
                    else if ("Bulanan".equalsIgnoreCase(jenis)) cal.add(Calendar.MONTH, 1);
                    else cal.add(Calendar.DATE, 7); // Standar Mingguan
                }
                
                perkuliahan.belum(); // Bersihkan cache
                out.print("{\"status\":\"ok\"}");
            } else {
                out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Data perkuliahan tidak ditemukan.") + "\"}");
            }
        }
        
        // Finalisasi: Simpan seluruh perubahan ke dalam pangkalan data
        sess.getTransaction().commit();
        
    } catch(Exception e) {
        if (sess != null && sess.getTransaction() != null && sess.getTransaction().isActive()) {
            sess.getTransaction().rollback(); // Batalkan seluruh proses jika terjadi galat
        }
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_buat_pertemuan_services.jsp:171");
        String eMsg = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : e.getClass().getSimpleName();
        out.print("{\"status\":\"error\", \"msg\":\"" + Common.getBahasaConfig("Kesalahan internal peladen") + ": " + eMsg + "\"}");
    } finally {
        // PERBAIKAN: Menjamin koneksi pangkalan data ditutup dalam kondisi apapun
        ais.common.ElearningSessionUtil.closeQuietly(sess);
        HibernateUtil.closeSessionQuietly(sess);
    }
%>
