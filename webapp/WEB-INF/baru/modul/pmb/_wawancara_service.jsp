<%--
/**
 * =====================================================================
 * _wawancara_service.jsp — JSON Service Endpoint Fitur Wawancara PMB
 * =====================================================================
 *
 * DESKRIPSI UMUM
 * ==============
 * File ini merupakan endpoint JSON (RESTful-style service via JSP) yang
 * melayani seluruh permintaan data dan mutasi terkait fitur Wawancara
 * (Interview) dalam proses Penerimaan Mahasiswa Baru (PMB).
 *
 * File ini berfungsi sebagai jembatan antara halaman antarmuka pengguna
 * (_wawancara.jsp) dengan lapisan data (Hibernate ORM + PostgreSQL). Semua
 * respons yang dikembalikan berformat JSON sehingga konsumsinya fleksibel
 * dari berbagai jenis klien: halaman JSP (fetch API), aplikasi mobile, dan
 * layanan pihak ketiga jika diperlukan.
 *
 * ALUR KERJA SISTEM
 * =================
 * Ketika calon mahasiswa membuka menu Wawancara di Portal PMB:
 *   1. Halaman _wawancara.jsp melakukan fetch ke endpoint ini dengan
 *      action=get_data untuk memuat informasi jadwal.
 *   2. Service ini melakukan serangkaian gate check keamanan:
 *        a. Validasi ID pendaftaran (harus numerik, tidak kosong).
 *        b. Verifikasi existensi BiodataCalonMahasiswa di basis data.
 *        c. Jika gelombang mewajibkan pembayaran, pastikan status lunas.
 *        d. Cek berkas wajib via VerifikasiPMBHelper: jika ada berkas
 *           yang wajibUploadSebelumInterview=true namun belum diunggah,
 *           kembalikan status "berkas_kurang" beserta pesan instruksi rinci.
 *   3. Bila semua gate check lulus, cari rekaman InterviewPunyaCalonMahasiswa
 *      yang aktif hari ini (berdasarkan kolom mulai dan sampai).
 *   4. Data yang dikembalikan mencakup: identitas pewawancara, jadwal waktu,
 *      platform video konferensi beserta tautannya, info dari gelombang,
 *      dan status kesiapan (siap) peserta.
 *   5. Setelah peserta mengkonfirmasi kesiapan, halaman memanggil
 *      action=submit_siap untuk merekam pernyataan tersebut. Service memperbarui
 *      kolom siap=true dan catatan, lalu mengembalikan tautan WhatsApp otomatis
 *      ke pewawancara (jika nomor HP tersedia di data Pegawai).
 *
 * AKSI YANG DIDUKUNG (Parameter: action)
 * ========================================
 *   get_data    — Memuat data jadwal wawancara hari ini untuk calon yang login.
 *                 Hasil berisi: nama sesi, nama & foto pewawancara, jadwal waktu
 *                 (mulai/sampai sebagai string format dan timestamp milisecond),
 *                 platform video konferensi, tautan video, info wawancara dari
 *                 GelombangPendaftaran, status siap, dan catatan.
 *
 *   submit_siap — Merekam pernyataan bahwa peserta siap untuk wawancara.
 *                 Bersifat IDEMPOTEN: jika peserta sudah menandai siap sebelumnya,
 *                 method mengembalikan sukses tanpa operasi duplikat. Jika pertama
 *                 kali, update siap=true ke basis data, commit, lalu generate link
 *                 WhatsApp ke nomor pewawancara jika ada. Juga menerapkan gate check
 *                 berkas sebagai perlindungan ganda (double guard).
 *
 * STATUS RESPONS JSON
 * ====================
 *   success      — Operasi berhasil, data tersedia di field lainnya.
 *   no_schedule  — Tidak ada jadwal wawancara aktif hari ini untuk calon ini.
 *   berkas_kurang — Terdapat berkas wajib yang belum diunggah; field "message"
 *                   berisi petunjuk langkah-langkah terperinci.
 *   forbidden    — Belum memenuhi syarat pembayaran.
 *   error        — Kesalahan teknis; field "message" berisi deskripsi singkat.
 *
 * PLATFORM VIDEO KONFERENSI
 * ==========================
 * Field onlineMenggunakan menunjukkan platform yang digunakan:
 *   0 = TIDAK_AKTIF (tatap muka langsung)
 *   1 = JITSI      → videoIconKey: "jitsi"
 *   2 = GOOGLE_MEET → videoIconKey: "gmeet"
 *   3 = ZOOM        → videoIconKey: "zoom"
 *   4 = BBB (BigBlueButton) → videoIconKey: "bbb"
 *   5 = SKYPE       → videoIconKey: "skype"
 *   6 = WHATSAPP    → videoIconKey: "wa"
 *   7 = LAIN        → videoIconKey: "other"
 *
 * MANAJEMEN SESI HIBERNATE
 * =========================
 * File ini menggunakan HibernateUtil.openSession() (bukan currentSession())
 * karena konteks eksekusi adalah thread servlet JSP yang tidak terikat pada
 * sesi ZK. Session yang dibuka secara eksplisit ini WAJIB ditutup di blok
 * finally (clear, disconnect, close) untuk mencegah kebocoran koneksi ke pool
 * C3P0 dan menghindari kondisi "deathmarch" pool yang dapat menyebabkan
 * seluruh aplikasi tidak responsif.
 *
 * KOMPATIBILITAS
 * ==============
 * Java 1.7: tidak menggunakan lambda, try-with-resources, Stream API,
 * diamond operator <>, method references (::), atau fitur Java 8+.
 * PostgreSQL 9.3+: kriteria SQL menggunakan fungsi date() yang kompatibel.
 *
 * @author  Tim Pengembang AIS
 * @version 2026-07-16
 * @see     ais.action.master.pmb.VerifikasiPMBHelper#ambilPesanGagalSebelumInterview
 * @see     ais.database.model.InterviewPunyaCalonMahasiswa
 * @see     ais.database.model.InterviewCalonMahasiswa
 * @see     ais.database.model.BiodataCalonMahasiswa
 */
--%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.*"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.action.master.pmb.VerifikasiPMBHelper"%>
<%@page import="java.util.*"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    // ── Header respons: JSON, no-cache ─────────────────────────────────────
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    String action = request.getParameter("action");
    String camaId = request.getParameter("id");

    // ── Validasi parameter wajib ───────────────────────────────────────────
    if (camaId == null || camaId.trim().length() == 0) {
        out.print(new JSONObject()
            .put("status", "error")
            .put("message", Common.getBahasaConfig("Data pendaftaran tidak dikenali. Silakan login kembali.")));
        return;
    }

    long camaIdLong;
    try {
        camaIdLong = Long.parseLong(camaId.trim());
    } catch (NumberFormatException nfe) {
        out.print(new JSONObject()
            .put("status", "error")
            .put("message", Common.getBahasaConfig("ID pendaftaran tidak valid.")));
        return;
    }

    // ── Format pembantu (thread-local-safe via new instance per request) ───
    SimpleDateFormat timeFmt   = new SimpleDateFormat("HH:mm");
    SimpleDateFormat tanggalFmt = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));

    Session hibSession = null;
    try {
        hibSession = HibernateUtil.openSession();

        // ── Muat BiodataCalonMahasiswa dengan sesi yang kita kelola ────────
        BiodataCalonMahasiswa cama = (BiodataCalonMahasiswa) hibSession.get(
                BiodataCalonMahasiswa.class, camaIdLong);

        if (cama == null) {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Data pendaftaran tidak ditemukan di sistem.")));
            return;
        }

        GelombangPendaftaran gelombang = cama.getGelombangPendaftaran();
        if (gelombang == null) {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data Anda.")));
            return;
        }

        // ── String tanggal server untuk SQL date filter ────────────────────
        String hariIniStr = Common.databaseDateFormat.get().format(WaktuUtil.getDate());

        // ==================================================================
        // ACTION: get_data — Memuat data jadwal wawancara aktif hari ini
        // ==================================================================
        if ("get_data".equals(action)) {

            // Gate 1: Pembayaran
            if (Boolean.TRUE.equals(gelombang.getHarusBayarSebelumBisaLogin())) {
                double persen = (cama.getPembayaranRegistrasi() == null)
                    ? 0.0 : cama.getPembayaranRegistrasi().getPersentaseLunas();
                if (persen < 0.01) {
                    out.print(new JSONObject()
                        .put("status", "forbidden")
                        .put("message", Common.getBahasaConfig(
                            "Anda harus menyelesaikan pembayaran registrasi terlebih dahulu sebelum dapat mengikuti sesi wawancara.")));
                    return;
                }
            }

            // Gate 2: Berkas wajib upload sebelum interview
            String pesanBerkas = VerifikasiPMBHelper.ambilPesanGagalSebelumInterview(cama);
            if (pesanBerkas != null) {
                out.print(new JSONObject()
                    .put("status", "berkas_kurang")
                    .put("message", pesanBerkas));
                return;
            }

            // Cari rekaman interview hari ini untuk calon ini
            InterviewPunyaCalonMahasiswa rec = (InterviewPunyaCalonMahasiswa) hibSession
                .createCriteria(InterviewPunyaCalonMahasiswa.class)
                .add(Restrictions.eq("biodataCalonMahasiswa", cama))
                .add(Restrictions.sqlRestriction(
                    "date('" + hariIniStr + "') between date(mulai) and date(sampai)"))
                .addOrder(Order.desc("id"))
                .setMaxResults(1)
                .uniqueResult();

            if (rec == null || rec.getInterviewCalonMahasiswa() == null) {
                out.print(new JSONObject()
                    .put("status", "no_schedule")
                    .put("message", Common.getBahasaConfig(
                        "Jadwal wawancara Anda belum tersedia hari ini. Mohon tunggu arahan dari panitia.")));
                return;
            }

            InterviewCalonMahasiswa icm = rec.getInterviewCalonMahasiswa();
            Pegawai pegawai             = icm.getPegawai();

            // ── Jadwal waktu (fallback ke level sesi jika per-orang kosong) ─
            Date mulaiDate  = rec.getMulai()  != null ? rec.getMulai()  : icm.getMulai();
            Date sampaiDate = rec.getSampai() != null ? rec.getSampai() : icm.getSampai();

            String mulaiStr   = mulaiDate  != null ? timeFmt.format(mulaiDate)    : "-";
            String sampaiStr  = sampaiDate != null ? timeFmt.format(sampaiDate)   : "-";
            String tanggalStr = mulaiDate  != null ? tanggalFmt.format(mulaiDate) : "-";
            long mulaiMs      = mulaiDate  != null ? mulaiDate.getTime()          : 0L;
            long sampaiMs     = sampaiDate != null ? sampaiDate.getTime()         : 0L;

            // ── Foto pewawancara ────────────────────────────────────────────
            String fotoUrl = "";
            if (pegawai != null) {
                try {
                    fotoUrl = CommonMedia.getUrlFotoPengguna(new Tbmuser(pegawai));
                } catch (Exception ex) {
                    fotoUrl = "";
                }
            }

            // ── Platform video konferensi ───────────────────────────────────
            int onlineMode   = icm.getOnlineMenggunakan();
            String videoPlatform = "";
            String videoIconKey  = "";
            String videoLink     = "";

            if (InterviewCalonMahasiswa.JITSI.equals(onlineMode)) {
                videoPlatform = "Jitsi Meet";
                videoIconKey  = "jitsi";
                try { videoLink = icm.generateJitsiLink(); } catch (Exception ex) { videoLink = ""; }

            } else if (InterviewCalonMahasiswa.GOOGLE_MEET.equals(onlineMode)) {
                videoPlatform = "Google Meet";
                videoIconKey  = "gmeet";
                videoLink     = icm.getLainLink() != null ? icm.getLainLink() : "";

            } else if (InterviewCalonMahasiswa.ZOOM.equals(onlineMode)) {
                videoPlatform = "Zoom";
                videoIconKey  = "zoom";
                videoLink     = icm.getZoomLink() != null ? icm.getZoomLink() : "";

            } else if (InterviewCalonMahasiswa.BBB.equals(onlineMode)) {
                videoPlatform = "BigBlueButton";
                videoIconKey  = "bbb";
                videoLink     = icm.getBbbLink() != null ? icm.getBbbLink() : "";

            } else if (InterviewCalonMahasiswa.SKYPE.equals(onlineMode)) {
                videoPlatform = "Skype";
                videoIconKey  = "skype";
                videoLink     = icm.getSkypeLink() != null ? icm.getSkypeLink() : "";

            } else if (InterviewCalonMahasiswa.WA.equals(onlineMode)) {
                videoPlatform = "WhatsApp";
                videoIconKey  = "wa";
                videoLink     = icm.getWaLink() != null ? icm.getWaLink() : "";

            } else if (InterviewCalonMahasiswa.LAIN.equals(onlineMode)) {
                videoPlatform = Common.getBahasaConfig("Video Konferensi");
                videoIconKey  = "other";
                videoLink     = icm.getLainLink() != null ? icm.getLainLink() : "";
            }

            // ── Info wawancara dari konfigurasi gelombang ──────────────────
            String infoInterview = gelombang.getInfoSaatInterview();
            if (infoInterview == null) { infoInterview = ""; }

            // ── Rakit JSON respons ─────────────────────────────────────────
            JSONObject resp = new JSONObject();
            resp.put("status",            "success");
            resp.put("interviewId",       rec.getId());
            resp.put("interviewerName",   icm.getNama() != null ? icm.getNama() : "-");
            resp.put("pegawaiName",       pegawai != null && pegawai.getNama() != null ? pegawai.getNama() : "-");
            resp.put("pegawaiPhoto",      fotoUrl);
            resp.put("mulaiStr",          mulaiStr);
            resp.put("sampaiStr",         sampaiStr);
            resp.put("tanggalStr",        tanggalStr);
            resp.put("mulaiMs",           mulaiMs);
            resp.put("sampaiMs",          sampaiMs);
            resp.put("isSiap",            Boolean.TRUE.equals(rec.getSiap()));
            resp.put("catatan",           rec.getKeterangan() != null ? rec.getKeterangan() : "");
            resp.put("infoInterview",     infoInterview);
            resp.put("onlineMenggunakan", onlineMode);
            resp.put("videoPlatform",     videoPlatform);
            resp.put("videoIconKey",      videoIconKey);
            resp.put("videoLink",         videoLink);
            out.print(resp.toString());

        // ==================================================================
        // ACTION: submit_siap — Rekam pernyataan kesiapan peserta
        // ==================================================================
        } else if ("submit_siap".equals(action)) {

            String catatan = request.getParameter("catatan");
            if (catatan == null) { catatan = ""; }

            // Gate: berkas wajib (double guard)
            String pesanBerkasSiap = VerifikasiPMBHelper.ambilPesanGagalSebelumInterview(cama);
            if (pesanBerkasSiap != null) {
                out.print(new JSONObject()
                    .put("status", "berkas_kurang")
                    .put("message", pesanBerkasSiap));
                return;
            }

            InterviewPunyaCalonMahasiswa rec = (InterviewPunyaCalonMahasiswa) hibSession
                .createCriteria(InterviewPunyaCalonMahasiswa.class)
                .add(Restrictions.eq("biodataCalonMahasiswa", cama))
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

            // Idempotency: jika sudah siap, kembalikan sukses tanpa duplikasi
            if (Boolean.TRUE.equals(rec.getSiap())) {
                out.print(new JSONObject()
                    .put("status", "success")
                    .put("waLink", "")
                    .put("message", Common.getBahasaConfig(
                        "Anda sudah menyatakan siap wawancara sebelumnya. Tidak ada perubahan yang dilakukan.")));
                return;
            }

            // Update siap=true + catatan
            Transaction tx = hibSession.beginTransaction();
            rec.setSiap(true);
            if (catatan.trim().length() > 0) {
                rec.setKeterangan(catatan.trim());
            }
            hibSession.update(rec);
            tx.commit();

            // ── Bangun link WhatsApp ke pewawancara (jika ada nomor HP) ───
            String waLink = "";
            try {
                if (rec.getInterviewCalonMahasiswa() != null
                        && rec.getInterviewCalonMahasiswa().getPegawai() != null) {
                    String hp = rec.getInterviewCalonMahasiswa().getPegawai().ambilNoHp();
                    if (hp != null && hp.trim().length() > 0) {
                        String pesanWa = Common.getBahasaConfig("Halo Bapak/Ibu, saya calon mahasiswa atas nama ")
                            + cama.getNama()
                            + Common.getBahasaConfig(" dengan Nomor Registrasi ")
                            + cama.getNoRegistrasi()
                            + Common.getBahasaConfig(" menyatakan telah SIAP untuk mengikuti sesi wawancara sekarang.");
                        waLink = "https://api.whatsapp.com/send?phone="
                            + hp.trim()
                            + "&text=" + URLEncoder.encode(pesanWa, "UTF-8");
                    }
                }
            } catch (Exception exWa) {
                // WA link tidak kritis — lanjutkan tanpa error
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
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_wawancara_service.jsp:398");
        try {
            out.print(new JSONObject()
                .put("status", "error")
                .put("message", Common.getBahasaConfig("Terjadi kesalahan internal peladen: ") + e.getMessage()));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_wawancara_service.jsp:403");}
    } finally {
        // ── WAJIB tutup sesi di blok finally untuk cegah kebocoran pool ───
        if (hibSession != null) {
            try { hibSession.clear();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_wawancara_service.jsp:407");}
            try { hibSession.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_wawancara_service.jsp:408");}
            try { hibSession.close();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_wawancara_service.jsp:409");}
        }
    }
%>
