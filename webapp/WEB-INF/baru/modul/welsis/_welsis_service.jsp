<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONArray"%>
<%@page import="java.util.Date"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.Statusabsensi"%>
<%@page import="ais.database.model.sekolah.Sekolah"%>
<%@page import="ais.database.model.sekolah.Siswa"%>
<%@page import="ais.database.model.sekolah.AbsenPiket"%>
<%@page import="ais.database.model.sekolah.AbsenPiketDetail"%>
<%@page import="ais.database.model.sekolah.KunjunganSiswa"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
try {
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    JSONObject jsonRes = new JSONObject();
    String aksi = request.getParameter("action");
    
    Session dbSession = null;
    Transaction tx = null;

    try {
        dbSession = HibernateUtil.getSessionFactory().openSession();
        tx = dbSession.beginTransaction();

        Date tanggalSekarang = ais.ui.util.WaktuUtil.getDate();

        // =============================================================
        // LOGIKA 1: ABSENSI SCAN SISWA (TANPA PARAMETER SEKOLAH)
        // =============================================================
        if ("scan".equals(aksi)) {
            String kodeId = request.getParameter("kode");
            
            if (kodeId == null || kodeId.trim().isEmpty()) {
                jsonRes.put("status", "error");
                jsonRes.put("message", Common.getBahasaConfig("Kode siswa tidak boleh kosong."));
            } else {
                // Cari data siswa berdasarkan ID Finger, NIS, ATAU NISN
                Siswa profilSiswa = (Siswa) dbSession.createCriteria(Siswa.class)
                    .add(Restrictions.isNotNull("namaSiswa"))
                    .add(Restrictions.ne("namaSiswa",""))
                    .add(Restrictions.or(
                        Restrictions.eq("idfinger", kodeId.trim()),
                        Restrictions.or(
                            Restrictions.eq("nomorInduk", kodeId.trim()),
                            Restrictions.eq("nomorIndukNasional", kodeId.trim())
                        )
                    )).setMaxResults(1).uniqueResult();

                if (profilSiswa == null) {
                    jsonRes.put("status", "error");
                    jsonRes.put("message", Common.getBahasaConfig("Identitas tidak dikenali di sistem."));
                } else if (profilSiswa.getSekolah() == null) {
                    jsonRes.put("status", "error");
                    jsonRes.put("message", Common.getBahasaConfig("Siswa ini belum terdaftar pada sekolah manapun."));
                } else {
                    Sekolah sekolahAktif = profilSiswa.getSekolah();

                    // Cari record absensi pertama hari ini (untuk jam datang)
                    KunjunganSiswa kunjunganPertama = (KunjunganSiswa) dbSession.createCriteria(KunjunganSiswa.class)
                        .add(Restrictions.eq("siswa", profilSiswa))
                        .add(Restrictions.eq("sekolah", sekolahAktif))
                        .add(Restrictions.eq("tgl", tanggalSekarang))
                        .addOrder(Order.desc("id"))
                        .setMaxResults(1).uniqueResult();

                    // Cek apakah jam ini dia sudah absen (cegah spam absen di jam yang sama)
                    KunjunganSiswa absenSekarang = (KunjunganSiswa) dbSession.createCriteria(KunjunganSiswa.class)
                        .add(Restrictions.eq("siswa", profilSiswa))
                        .add(Restrictions.eq("sekolah", sekolahAktif))
                        .add(Restrictions.eq("tgl", tanggalSekarang))
                        .add(Restrictions.eq("jam", Calendar.getInstance().get(Calendar.HOUR_OF_DAY)))
                        .setMaxResults(1).uniqueResult();

                    if (absenSekarang == null) {
                        absenSekarang = new KunjunganSiswa();
                        absenSekarang.setSekolah(sekolahAktif);
                        absenSekarang.setSiswa(profilSiswa);
                        absenSekarang.setTanggal(tanggalSekarang);
                        
                        dbSession.save(absenSekarang);
                        
                        // Menangani Absen Piket & Status
                        String jamAwalMasuk = Common.timeFormat.get().format(absenSekarang.getTanggal());
                        if (kunjunganPertama != null) {
                            jamAwalMasuk = Common.timeFormat.get().format(kunjunganPertama.getTanggal());
                        }

                        if (profilSiswa.getKelas() != null) {
                            // Cari Absen Piket Kelas
                            AbsenPiket absenPiket = (AbsenPiket) dbSession.createCriteria(AbsenPiket.class)
                                .add(Restrictions.eq("kelas", profilSiswa.getKelas()))
                                .add(Restrictions.eq("sekolah", sekolahAktif))
                                .add(Restrictions.sqlRestriction("date(this_.tanggal)=date('" + Common.databaseDateFormat.get().format(absenSekarang.getTanggal()) + "')"))
                                .setMaxResults(1).uniqueResult();

                            if (absenPiket == null) {
                                absenPiket = new AbsenPiket();
                                absenPiket.setKelas(profilSiswa.getKelas());
                                absenPiket.setGuru(profilSiswa.getKelas().getGuruPembina());
                                absenPiket.setSekolah(sekolahAktif);
                                absenPiket.setTanggal(absenSekarang.getTanggal());
                                dbSession.save(absenPiket);
                            }

                            Statusabsensi stAbsen = ConstantValues.MASUK;
                            AbsenPiketDetail piketDetail = AbsenPiketDetail.ambil(null, profilSiswa, absenPiket, absenPiket.getKelas().getAbsensi(), dbSession);
                            
                            piketDetail.populate(
                                profilSiswa.getId() + "_" + absenPiket.getId(), 
                                stAbsen,
                                "Absen siswa pada " + Common.dateFormat.get().format(absenSekarang.getTanggal()), 
                                jamAwalMasuk,
                                Common.timeFormat.get().format(absenSekarang.getTanggal()), 
                                "AbsenPiket"
                            );
                            
                            dbSession.update(piketDetail);
                        }

                        jsonRes.put("status", "success");
                        jsonRes.put("message", Common.getBahasaConfig("Absensi Berhasil, selamat belajar ") + profilSiswa.getNamaSiswa() + "!");
                    } else {
                        jsonRes.put("status", "success");
                        jsonRes.put("message", Common.getBahasaConfig("Halo ") + profilSiswa.getNamaSiswa() + Common.getBahasaConfig(", absen Anda sudah tercatat pada jam ini."));
                    }
                }
            }
        } 
        
        // =============================================================
        // LOGIKA 2: AMBIL DAFTAR RIWAYAT (PAGING GLOBAL SISWA)
        // =============================================================
        else if ("list".equals(aksi)) {
            int limit = 10;
            int pageIdx = 0;
            try { pageIdx = Integer.parseInt(request.getParameter("page")); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/welsis/_welsis_service.jsp:151");}
            
            Long totalData = (Long) dbSession.createCriteria(KunjunganSiswa.class)
                .setProjection(Projections.rowCount())
                .uniqueResult();
            
            List<KunjunganSiswa> listKunjungan = dbSession.createCriteria(KunjunganSiswa.class)
                .addOrder(Order.desc("id"))
                .setFirstResult(pageIdx * limit)
                .setMaxResults(limit)
                .list();
            
            JSONArray dataArray = new JSONArray();
            SimpleDateFormat sdfWaktu = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat sdfTanggal = new SimpleDateFormat("dd/MM/yyyy");
            
            for (KunjunganSiswa k : listKunjungan) {
                JSONObject obj = new JSONObject();
                
                if (k.getSiswa() != null) {
                    obj.put("nama", k.getSiswa().getNamaSiswa());
                    obj.put("identitas", k.getSiswa().getNomorInduk());
                    obj.put("kelas", k.getSiswa().getKelas() != null ? k.getSiswa().getKelas().getNama() : "-");
                    obj.put("status", "Siswa");
                } else {
                    obj.put("nama", k.getNama());
                    obj.put("identitas", "-");
                    obj.put("kelas", "-");
                    obj.put("status", "Bukan Siswa");
                }
                
                obj.put("waktu", sdfWaktu.format(k.getTanggal()));
                obj.put("tanggal", sdfTanggal.format(k.getTanggal()));
                dataArray.put(obj);
            }
            
            jsonRes.put("status", "success");
            jsonRes.put("data", dataArray);
            jsonRes.put("total", totalData);
            jsonRes.put("limit", limit);
        } else {
            jsonRes.put("status", "error");
            jsonRes.put("message", "Aksi tidak dikenali.");
        }

        tx.commit();

    } catch (Exception ex) {
        if (tx != null && tx.isActive()) {
            tx.rollback();
        }
        jsonRes.put("status", "error");
        jsonRes.put("message", "Error: " + ex.getMessage());
    } finally {
        if (dbSession != null) {
            dbSession.clear();
            dbSession.disconnect();
            dbSession.close();
        }
    }

    out.print(jsonRes.toString());
    out.flush();
} catch (Exception e) {
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/welsis/_welsis_service.jsp:215");
}
%>