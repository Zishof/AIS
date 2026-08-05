<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.HasilUjianMahasiswa"%>
<%@page import="ais.database.model.PertemuanPunyaUjian"%>
<%@page import="ais.database.model.sekolah.CalonSiswa"%>
<%@page import="ais.database.model.sekolah.GelombangPendaftaranPsb"%>
<%@page import="ais.database.model.sekolah.JadwalUjianPSB"%>
<%@page import="ais.database.model.sekolah.RuangGelombangPendaftaranPsbPSB"%>
<%@page import="ais.common.Common"%>
<%@page import="ais.common.ConstantValues"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="java.util.*"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");

    String action  = request.getParameter("action");
    String casisId = request.getParameter("id");

    if (casisId == null || casisId.trim().length() == 0) {
        out.print(new JSONObject().put("status","error")
            .put("message", Common.getBahasaConfig("Data pendaftaran tidak dikenali. Silakan masuk kembali.")));
        return;
    }

    Session hibSession = null;
    try {
        hibSession = HibernateUtil.openSession();

        CalonSiswa casis = (CalonSiswa) hibSession.get(CalonSiswa.class, Long.parseLong(casisId.trim()));
        if (casis == null) {
            out.print(new JSONObject().put("status","error")
                .put("message", Common.getBahasaConfig("Data calon siswa tidak ditemukan di peladen.")));
            return;
        }
        GelombangPendaftaranPsb gelombang = casis.getGelombangPendaftaranPsb();
        if (gelombang == null) {
            out.print(new JSONObject().put("status","error")
                .put("message", Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data Anda.")));
            return;
        }

        if ("fetch_exams".equals(action)) {

            String hariIniStr = Common.databaseDateFormat.get().format(WaktuUtil.getDate());

            // 1. Ambil alokasi ruang ujian
            RuangGelombangPendaftaranPsbPSB ruangRec =
                (RuangGelombangPendaftaranPsbPSB) hibSession
                    .createCriteria(RuangGelombangPendaftaranPsbPSB.class)
                    .add(Restrictions.eq("calonSiswa", casis))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1)
                    .uniqueResult();

            if (ruangRec == null || ruangRec.getRuangPSB() == null
                    || ruangRec.getRuangPSB().getUjianPSB() == null) {
                out.print(new JSONObject().put("status","no_schedule")
                    .put("message", Common.getBahasaConfig(
                        "Anda belum mendapat alokasi ruang ujian. Mohon hubungi panitia PPDB.")));
                return;
            }

            // 2. Ambil JadwalUjianPSB untuk ujian yang dialokasikan
            List jadwalList = ConstantValues.simpleList(
                hibSession.createCriteria(JadwalUjianPSB.class)
                    .add(Restrictions.eq("ujianPSB", ruangRec.getRuangPSB().getUjianPSB()))
                    .add(Restrictions.or(
                        Restrictions.eq("gelombangPendaftaranPsb", gelombang),
                        Restrictions.isNull("gelombangPendaftaranPsb")))
                    .addOrder(Order.asc("waktuMulai")),
                JadwalUjianPSB.class);

            if (jadwalList.isEmpty()) {
                out.print(new JSONObject().put("status","no_schedule")
                    .put("message", Common.getBahasaConfig(
                        "Jadwal ujian belum tersedia untuk gelombang Anda. Mohon tunggu pengumuman panitia.")));
                return;
            }

            // 3. Ambil PertemuanPunyaUjian dari jadwal-jadwal tersebut
            List ppuList = ConstantValues.simpleList(
                hibSession.createCriteria(PertemuanPunyaUjian.class)
                    .addOrder(Order.asc("nama"))
                    .add(Restrictions.or(
                        Restrictions.or(
                            Restrictions.eq("tidakDitampilkanJikaWaktuSudahTerlewat", false),
                            Restrictions.isNull("tidakDitampilkanJikaWaktuSudahTerlewat")),
                        Restrictions.sqlRestriction(
                            "date('" + hariIniStr + "') between date(mulai_ujian) and date(sampai_ujian)")))
                    .createAlias("pertemuan", "p")
                    .add(Restrictions.in("p.jadwalUjianPSB", jadwalList)),
                PertemuanPunyaUjian.class);

            JSONArray examsArray = new JSONArray();
            double nilaiTotal  = 0.0;
            double jumlahUjian = 0.0;
            boolean adaBelumUjian = false;

            for (int i = 0; i < ppuList.size(); i++) {
                PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) ppuList.get(i);

                // Ambil hasil ujian calon siswa ini
                HasilUjianMahasiswa hasil = (HasilUjianMahasiswa) hibSession
                    .createCriteria(HasilUjianMahasiswa.class)
                    .add(Restrictions.eq("pertemuanPunyaUjian", ppu))
                    .add(Restrictions.eq("calonSiswa", casis))
                    .setMaxResults(1)
                    .uniqueResult();

                int jmlBolehIkut   = ppu.getJumlahBolehIkut();
                int jmlPernahIkut  = hasil == null ? 0 : hasil.getJumlahIkut();
                Double maxNilai    = hasil == null ? null : hasil.getNilai();

                java.util.Date mulaiUjian  = ppu.getMulaiUjian();
                java.util.Date sampaiUjian = ppu.getSampaiUjian();

                JSONObject exam = new JSONObject();
                exam.put("id",           ppu.getId());
                exam.put("nama",         ppu.getNama() != null ? ppu.getNama() : "-");
                exam.put("tglMulai",     mulaiUjian  != null ? WaktuUtil.formatTanggalLengkap(mulaiUjian)  : "-");
                exam.put("tglSelesai",   sampaiUjian != null ? WaktuUtil.formatTanggalLengkap(sampaiUjian) : "-");
                exam.put("tanggalMulai", mulaiUjian  != null ? mulaiUjian.getTime()  : JSONObject.NULL);
                exam.put("tanggalSelesai", sampaiUjian != null ? sampaiUjian.getTime() : JSONObject.NULL);
                exam.put("maxPercobaan", jmlBolehIkut);
                exam.put("jumlahIkut",   jmlPernahIkut);

                if (maxNilai == null) {
                    exam.put("status", "BELUM");
                    adaBelumUjian = true;
                } else {
                    exam.put("status", "SUDAH");
                    if (ppu.getLihatNilaiSetelahUjian()) {
                        exam.put("nilai", maxNilai.doubleValue());
                        nilaiTotal  += maxNilai.doubleValue();
                        jumlahUjian += 1.0;
                    }
                }
                examsArray.put(exam);
            }

            // Grade deskriptif
            double rataRata = (jumlahUjian > 0) ? (nilaiTotal / jumlahUjian) : 0.0;
            String grade = Common.getBahasaConfig("CUKUP");
            if      (rataRata >= 90.0) grade = Common.getBahasaConfig("UNGGUL");
            else if (rataRata >= 70.0) grade = Common.getBahasaConfig("BAIK SEKALI");
            else if (rataRata >= 50.0) grade = Common.getBahasaConfig("BAIK");

            String defLulus = Common.getBahasaConfig(
                "Selamat, Anda dinyatakan telah selesai mengikuti proses evaluasi online. Silakan ikuti tahapan selanjutnya.");
            String defNilaiLabel = Common.getBahasaConfig("Kualifikasi kelulusan Anda adalah : ");

            JSONObject resp = new JSONObject();
            resp.put("status",        "success");
            resp.put("exams",         examsArray);
            resp.put("rataRata",      rataRata);
            resp.put("grade",         grade);
            resp.put("allFinished",   (!adaBelumUjian && jumlahUjian > 0));
            resp.put("infoLulus",     Common.getKonfigurasi("info_lulus_ujian_psb", defLulus).getNilai());
            resp.put("infoNilaiLabel",Common.getKonfigurasi("info_nilai_lulus_ujian_psb", defNilaiLabel).getNilai());
            out.print(resp.toString());

        } else {
            out.print(new JSONObject().put("status","error")
                .put("message", Common.getBahasaConfig("Aksi tidak dikenali: ") + action));
        }

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp:176");
        try {
            out.print(new JSONObject().put("status","error")
                .put("message", Common.getBahasaConfig("Terjadi kesalahan pada peladen: ") + e.getMessage()));
        } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp:180");}
    } finally {
        if (hibSession != null) {
            try { hibSession.clear();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp:183");}
            try { hibSession.disconnect(); } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp:184");}
            try { hibSession.close();      } catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/ppdb/_ikut_ujian_online_service.jsp:185");}
        }
    }
%>
