<%--
/**
 * ======================================================================
 * _wawancara_service.jsp — JSON Service Endpoint Fitur Wawancara PSB/PPDB
 * ======================================================================
 *
 * DESKRIPSI UMUM
 * ==============
 * Endpoint JSON yang melayani seluruh permintaan data dan mutasi terkait
 * fitur Wawancara (Interview) dalam proses Penerimaan Siswa Baru (PSB/PPDB).
 *
 * File ini berfungsi sebagai jembatan antara halaman antarmuka pengguna
 * (_wawancara.jsp) dengan lapisan data (Hibernate ORM + PostgreSQL). Semua
 * respons berformat JSON agar konsumsinya fleksibel via fetch API.
 *
 * AKSI YANG DIDUKUNG (Parameter: action)
 * =======================================
 *   get_data    — Memuat data jadwal wawancara hari ini untuk calon yang login.
 *                 Hasil berisi: nama sesi, nama & foto pewawancara, jadwal waktu
 *                 (mulai/sampai sebagai string format dan timestamp ms), platform
 *                 video konferensi, tautan video, info wawancara dari gelombang,
 *                 status siap, dan catatan.
 *
 *   submit_siap — Merekam pernyataan bahwa peserta siap untuk wawancara.
 *                 Bersifat IDEMPOTEN: jika peserta sudah menandai siap sebelumnya,
 *                 kembalikan sukses tanpa operasi duplikat. Jika pertama kali,
 *                 update siap=true, commit, lalu generate link WA ke pewawancara.
 *
 * STATUS RESPONS JSON
 * ====================
 *   success      — Operasi berhasil.
 *   no_schedule  — Tidak ada jadwal wawancara aktif hari ini.
 *   berkas_kurang — Ada berkas wajib-interview yang belum diunggah.
 *   error        — Kesalahan teknis; field "message" berisi deskripsi.
 *
 * MANAJEMEN SESI HIBERNATE
 * =========================
 * File ini menggunakan HibernateUtil.openSession() (bukan currentSession())
 * karena konteks eksekusi adalah thread servlet JSP. Session WAJIB ditutup
 * di blok finally (clear, disconnect, close).
 *
 * KOMPATIBILITAS
 * ==============
 * Java 1.7: tidak ada lambda, try-with-resources, Stream API, diamond <>.
 * PostgreSQL 9.3+: filter tanggal via date('YYYYMMDD').
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     ais.database.model.sekolah.InterviewCalonSiswa
 * @see     ais.database.model.sekolah.InterviewPunyaCalonSiswa
 * @see     ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa#ambilPesanGagalSebelumInterview
 */
--%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.sekolah.InterviewCalonSiswa"%>
<%@page import="ais.database.model.sekolah.InterviewPunyaCalonSiswa"%>
<%@page import="ais.database.model.sekolah.VerifikasiKelengkapanCalonSiswa"%>
<%@page import="ais.database.model.Pegawai"%>
<%@page import="ais.database.model.Tbmuser"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.CommonMedia"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    String action  = request.getParameter("action");
    String casisId = request.getParameter("id");

    if (casisId == null || casisId.trim().length() == 0) {
        out.print(new JSONObject()
            .put("status", "error")
            .put("message", Common.getBahasaConfig("Data pendaftaran tidak dikenali. Silakan masuk kembali.")));
        return;
    }

    long casisIdLong;
    try {
        casisIdLong = Long.parseLong(casisId.trim());
    } catch (NumberFormatException nfe) {
        out.print(new JSONObject()
            .put("status", "error")
            .put("message", Common.getBahasaConfig("ID pendaftaran tidak valid.")));
        return;
    }

    SimpleDateFormat timeFmt    = new SimpleDateFormat("HH:mm");
    SimpleDateFormat tanggalFmt = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));

    Session hibSession = null;
    try {
        hibSession = HibernateUtil.openSession();

        CalonSiswa casis = (CalonSiswa) hibSession.get(CalonSiswa.class, casisIdLong);
        if (casis == null) {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Data pendaftaran tidak ditemukan di sistem.")));
            return;
        }

        GelombangPendaftaranPsb gelombang = casis.getGelombangPendaftaranPsb();
        if (gelombang == null) {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data Anda.")));
            return;
        }

        // Tanggal server untuk filter SQL (kompatibel PG 9.3)
        String hariIniStr = Common.databaseDateFormat.get().format(WaktuUtil.getDate());

        // ==================================================================
        // ACTION: get_data
        // ==================================================================
        if ("get_data".equals(action)) {

            // Gate: berkas wajib sebelum interview
            String pesanBerkas = VerifikasiKelengkapanCalonSiswa.ambilPesanGagalSebelumInterview(casis, hibSession);
            if (pesanBerkas != null) {
                out.print(new JSONObject()
                    .put("status", "berkas_kurang")
                    .put("message", pesanBerkas));
                return;
            }

            // Cari penugasan interview aktif hari ini untuk calon ini
            InterviewPunyaCalonSiswa rec = (InterviewPunyaCalonSiswa) hibSession
                .createCriteria(InterviewPunyaCalonSiswa.class)
                .add(Restrictions.eq("calonSiswa", casis))
                .add(Restrictions.sqlRestriction(
                    "date('" + hariIniStr + "') between date(mulai) and date(sampai)"))
                .addOrder(Order.desc("id"))
                .setMaxResults(1)
                .uniqueResult();

            if (rec == null || rec.getInterviewCalonSiswa() == null) {
                out.print(new JSONObject()
                    .put("status", "no_schedule")
                    .put("message", Common.getBahasaConfig(
                        "Jadwal wawancara Anda belum tersedia hari ini. Mohon tunggu arahan dari panitia.")));
                return;
            }

            InterviewCalonSiswa ics = rec.getInterviewCalonSiswa();
            Pegawai pegawai         = ics.getPegawai();

            // Waktu (per-peserta dulu, fallback ke sesi)
            Date mulaiDate  = rec.getMulai();
            Date sampaiDate = rec.getSampai();

            String mulaiStr   = mulaiDate  != null ? timeFmt.format(mulaiDate)    : "-";
            String sampaiStr  = sampaiDate != null ? timeFmt.format(sampaiDate)   : "-";
            String tanggalStr = mulaiDate  != null ? tanggalFmt.format(mulaiDate) : "-";
            long mulaiMs      = mulaiDate  != null ? mulaiDate.getTime()          : 0L;
            long sampaiMs     = sampaiDate != null ? sampaiDate.getTime()         : 0L;

            // Foto pewawancara
            String fotoUrl = "";
            if (pegawai != null) {
                try {
                    fotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai));
                } catch (Exception exFoto) {
                    fotoUrl = "";
                }
            }

            // Platform video konferensi
            int onlineMode      = ics.getOnlineMenggunakan();
            String videoPlatform = "";
            String videoIconKey  = "";
            String videoLink     = "";

            if (InterviewCalonSiswa.JITSI.equals(onlineMode)) {
                videoPlatform = "Jitsi Meet";
                videoIconKey  = "jitsi";
                try { videoLink = ics.generateJitsiLink(); } catch (Exception exJitsi) { videoLink = ""; }

            } else if (InterviewCalonSiswa.GOOGLE_MEET.equals(onlineMode)) {
                videoPlatform = "Google Meet";
                videoIconKey  = "gmeet";
                videoLink     = ics.getLainLink() != null ? ics.getLainLink() : "";

            } else if (InterviewCalonSiswa.ZOOM.equals(onlineMode)) {
                videoPlatform = "Zoom";
                videoIconKey  = "zoom";
                videoLink     = ics.getZoomLink() != null ? ics.getZoomLink() : "";

            } else if (InterviewCalonSiswa.BBB.equals(onlineMode)) {
                videoPlatform = "BigBlueButton";
                videoIconKey  = "bbb";
                videoLink     = ics.getBbbLink() != null ? ics.getBbbLink() : "";

            } else if (InterviewCalonSiswa.SKYPE.equals(onlineMode)) {
                videoPlatform = "Skype";
                videoIconKey  = "skype";
                videoLink     = ics.getSkypeLink() != null ? ics.getSkypeLink() : "";

            } else if (InterviewCalonSiswa.WA.equals(onlineMode)) {
                videoPlatform = "WhatsApp";
                videoIconKey  = "wa";
                videoLink     = ics.getWaLink() != null ? ics.getWaLink() : "";

            } else if (InterviewCalonSiswa.LAIN.equals(onlineMode)) {
                videoPlatform = Common.getBahasaConfig("Video Konferensi");
                videoIconKey  = "other";
                videoLink     = ics.getLainLink() != null ? ics.getLainLink() : "";
            }

            // Info dari gelombang
            String infoInterview = gelombang.getInfoSaatInterview();
            if (infoInterview == null) { infoInterview = ""; }

            JSONObject resp = new JSONObject();
            resp.put("status",             "success");
            resp.put("interviewId",        rec.getId());
            resp.put("interviewerName",    ics.getNama() != null ? ics.getNama() : "-");
            resp.put("pegawaiName",        pegawai != null && pegawai.getNama() != null ? pegawai.getNama() : "-");
            resp.put("pegawaiPhoto",       fotoUrl);
            resp.put("mulaiStr",           mulaiStr);
            resp.put("sampaiStr",          sampaiStr);
            resp.put("tanggalStr",         tanggalStr);
            resp.put("mulaiMs",            mulaiMs);
            resp.put("sampaiMs",           sampaiMs);
            resp.put("isSiap",             Boolean.TRUE.equals(rec.getSiap()));
            resp.put("catatan",            rec.getKeterangan() != null ? rec.getKeterangan() : "");
            resp.put("infoInterview",      infoInterview);
            resp.put("onlineMenggunakan",  onlineMode);
            resp.put("videoPlatform",      videoPlatform);
            resp.put("videoIconKey",       videoIconKey);
            resp.put("videoLink",          videoLink);
            out.print(resp.toString());

        // ==================================================================
        // ACTION: submit_siap
        // ==================================================================
        } else if ("submit_siap".equals(action)) {

            String catatan = request.getParameter("catatan");
            if (catatan == null) { catatan = ""; }

            // Gate ganda: berkas wajib
            String pesanBerkasSiap = VerifikasiKelengkapanCalonSiswa.ambilPesanGagalSebelumInterview(casis, hibSession);
            if (pesanBerkasSiap != null) {
                out.print(new JSONObject()
                    .put("status", "berkas_kurang")
                    .put("message", pesanBerkasSiap));
                return;
            }

            InterviewPunyaCalonSiswa rec = (InterviewPunyaCalonSiswa) hibSession
                .createCriteria(InterviewPunyaCalonSiswa.class)
                .add(Restrictions.eq("calonSiswa", casis))
                .add(Restrictions.sqlRestriction(
                    "date('" + hariIniStr + "') between date(mulai) and date(sampai)"))
                .addOrder(Order.desc("id"))
                .setMaxResults(1)
                .uniqueResult();

            if (rec == null) {
                out.print(new JSONObject()
                    .put("status", "error")
                    .put("message", Common.getBahasaConfig(
                        "Sesi wawancara aktif tidak ditemukan atau sudah berakhir. Hubungi panitia jika ini keliru.")));
                return;
            }

            // Idempoten: sudah siap sebelumnya
            if (Boolean.TRUE.equals(rec.getSiap())) {
                out.print(new JSONObject()
                    .put("status", "success")
                    .put("waLink", "")
                    .put("message", Common.getBahasaConfig(
                        "Anda sudah menyatakan siap wawancara sebelumnya. Tidak ada perubahan yang dilakukan.")));
                return;
            }

            // Simpan kesiapan
            Transaction tx = hibSession.beginTransaction();
            rec.setSiap(true);
            if (catatan.trim().length() > 0) {
                rec.setKeterangan(catatan.trim());
            }
            hibSession.update(rec);
            tx.commit();

            // Link WhatsApp ke pewawancara
            String waLink = "";
            try {
                if (rec.getInterviewCalonSiswa() != null
                        && rec.getInterviewCalonSiswa().getPegawai() != null) {
                    String hp = rec.getInterviewCalonSiswa().getPegawai().ambilNoHp();
                    if (hp != null && hp.trim().length() > 0) {
                        String pesanWa = Common.getBahasaConfig("Halo Bapak/Ibu, saya calon siswa atas nama ")
                            + casis.getNama()
                            + Common.getBahasaConfig(" dengan Nomor Registrasi ")
                            + casis.getNoRegistrasi()
                            + Common.getBahasaConfig(" menyatakan telah SIAP untuk mengikuti sesi wawancara sekarang.");
                        waLink = "https://api.whatsapp.com/send?phone="
                            + hp.trim()
                            + "&text=" + URLEncoder.encode(pesanWa, "UTF-8");
                    }
                }
            } catch (Exception exWa) {
                waLink = "";
            }

            out.print(new JSONObject()
                .put("status", "success")
                .put("waLink", waLink)
                .put("message", Common.getBahasaConfig(
                    "Pernyataan kesiapan Anda berhasil disimpan. Pewawancara akan segera dihubungi.")));

        // ==================================================================
        // Unknown action
        // ==================================================================
        } else {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Aksi tidak dikenali: ") + action));
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_wawancara_service.jsp:337");
        try {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Terjadi kesalahan internal peladen: ") + e.getMessage()));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_wawancara_service.jsp:342");}
    } finally {
        if (hibSession != null) {
            try { hibSession.clear();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_wawancara_service.jsp:345");}
            try { hibSession.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_wawancara_service.jsp:346");}
            try { hibSession.close();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_wawancara_service.jsp:347");}
        }
    }
%>
