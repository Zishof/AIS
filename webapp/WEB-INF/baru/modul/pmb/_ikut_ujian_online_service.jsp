<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.MatchMode"%>
<%@page import="org.hibernate.criterion.Order"%>
<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.criterion.Projections"%>
<%@page import="org.hibernate.Session"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.common.*"%>
<%@page import="ais.ui.util.WaktuUtil"%>
<%@page import="ais.action.master.pmb.VerifikasiPMBHelper"%>
<%@page import="java.util.*"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
    response.setHeader("Pragma", "no-cache");
    JSONObject responseJson = new JSONObject();
    String action = request.getParameter("action");
    String camaId = request.getParameter("id");
    if (camaId == null || camaId.trim().length() == 0) {
        out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Data pendaftaran tidak dikenali. Silakan login kembali.")));
        return;
    }
    
    Session hibSession = null;
    try {
        hibSession = HibernateUtil.openSession();
        BiodataCalonMahasiswa cama = (BiodataCalonMahasiswa) GeneralValueObject.ambilData(BiodataCalonMahasiswa.class, camaId, true);

        if (cama == null) {
            out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Data Calon Mahasiswa tidak ditemukan di peladen.")));
            return;
        }
        GelombangPendaftaran gelombangPendaftaran = cama.getGelombangPendaftaran();
        if (gelombangPendaftaran == null) {
            out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Gelombang pendaftaran belum terhubung dengan data Anda.")));
            return;
        }

        if ("fetch_exams".equals(action)) {
            // =====================================================================
            // LOGIKA PENGAMBILAN DATA SESUAI TampilanUjianCalonMahasiswa.java
            // =====================================================================

            // Gate: berkas wajib upload sebelum ujian
            String pesanGagalUjian = VerifikasiPMBHelper.ambilPesanGagalSebelumUjian(cama);
            if (pesanGagalUjian != null) {
                out.print(new JSONObject().put("status", "berkas_kurang").put("message", pesanGagalUjian));
                return;
            }

            // 1. Ambil ID Ruangan
            Long idRuang = (Long) hibSession.createCriteria(RuangPaketPMB.class)
                    .add(Restrictions.eq("biodataCalonMahasiswa", cama))
                    .setProjection(Projections.property("ruangPMB.id"))
                    .addOrder(Order.desc("id"))
                    .setMaxResults(1)
                    .uniqueResult();
            
            if (idRuang == null) {
                idRuang = -1L;
            }

            // 2. Ambil JadwalUjianPMB
            List<JadwalUjianPMB> jadwal = ConstantValues.simpleList(
                    hibSession.createCriteria(JadwalUjianPMB.class)
                            .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                            .add(Restrictions.or(
                                    Restrictions.or(Restrictions.isNull("berlakuUntukSemuaRuangan"), Restrictions.eq("berlakuUntukSemuaRuangan", true)),
                                    Restrictions.ilike("ruanganYgIkut", "," + idRuang.toString() + ",", MatchMode.ANYWHERE)))
                            .createAlias("ujianPMB", "ujianPMB")
                            .add(Restrictions.eq("ujianPMB.gelombangPendaftaran", gelombangPendaftaran))
                            .add(cama.getPaket() == null ? Restrictions.isNull("paket") : Restrictions.or(Restrictions.isNull("paket"), Restrictions.eq("paket", cama.getPaket()))),
                    JadwalUjianPMB.class);

            TreeMap<String, Long> pertemuansa = new TreeMap<String, Long>();
            for (JadwalUjianPMB jadwalUjianPMB : jadwal) {
                for (Pertemuan pertemuan : jadwalUjianPMB.ambilPertemuanList()) {
                    String keyPert = Common.dateFormat8.get().format(pertemuan.getTanggal());
                    keyPert += ("_" + (pertemuan.getWaktuMulai() == null && pertemuan.getWaktuSelesai() == null
                            ? "00.00-00.00"
                            : (pertemuan.getWaktuMulai() == null ? "00.00" : pertemuan.getWaktuMulai()) + "-"
                                    + (pertemuan.getWaktuSelesai() == null ? "00.00" : pertemuan.getWaktuSelesai())));
                    pertemuansa.put(keyPert + "_" + pertemuan.getId(), pertemuan.getId());
                }
            }

            // 3. Ambil PertemuanPunyaUjian — hanya dari pertemuan yang sesuai ruang mahasiswa
            //    (pertemuansa sudah difilter per-ruang di step 2; tanpa filter ini semua sesi
            //    dalam gelombang yang sama akan tampil ke semua mahasiswa)
            List<PertemuanPunyaUjian> ppuList = pertemuansa.isEmpty() ? new java.util.ArrayList<PertemuanPunyaUjian>()
                    : ConstantValues.simpleList(
                    hibSession.createCriteria(PertemuanPunyaUjian.class)
                            .addOrder(Order.asc("nama"))
                            .add(Restrictions.or(
                                    Restrictions.or(
                                            Restrictions.eq("tidakDitampilkanJikaWaktuSudahTerlewat", false),
                                            Restrictions.isNull("tidakDitampilkanJikaWaktuSudahTerlewat")),
                                    Restrictions.sqlRestriction("date('"
                                            + Common.databaseDateFormat.get().format(WaktuUtil.getDate())
                                            + "') between date(mulai_ujian) and date(sampai_ujian)")))
                            .createAlias("pertemuan", "pertemuan")
                            .add(Restrictions.in("pertemuan.id", pertemuansa.values()))
                            .createAlias("pertemuan.jadwalUjianPMB", "jadwalUjianPMB")
                            .add(cama.getPaket() == null ? Restrictions.isNull("jadwalUjianPMB.paket") : Restrictions.or(Restrictions.isNull("jadwalUjianPMB.paket"),
                                    Restrictions.eq("jadwalUjianPMB.paket", cama.getPaket())))
                            .createAlias("jadwalUjianPMB.ujianPMB", "ujianPMB")
                            .add(Restrictions.eq("ujianPMB.gelombangPendaftaran", gelombangPendaftaran)),
                    PertemuanPunyaUjian.class);

            // =====================================================================
            // PREPARASI DATA UNTUK JSON DAN PERHITUNGAN SKOR
            // =====================================================================
            JSONArray examsArray = new JSONArray();
            double nilaiTotal = 0.0;
            double jumlahUjian = 0.0;
            boolean adaBelumUjian = false;

            if (!ppuList.isEmpty()) {
                for (PertemuanPunyaUjian ppu : ppuList) {
                    JSONObject exam = new JSONObject();
                    exam.put("id", ppu.getId());
                    exam.put("nama", ppu.getNama());
                    
                    HasilUjianMahasiswa hasilUjianMahasiswa = HasilUjianMahasiswa.ambilByKey(
                    		ppu, null,
                    		cama, null, null);
                    
                    Date mulaiUjian = ppu.getMulaiUjian();
                    Date sampaiUjian = ppu.getSampaiUjian();
                    int jmlDitampilkan = ppu.getJmlDitampilkan();
                    int jmlBolehIkut = ppu.getJumlahBolehIkut();
                    int jmlPernahIkut = hasilUjianMahasiswa == null ? 0 : hasilUjianMahasiswa.getJumlahIkut();
                    Double maxNilai = hasilUjianMahasiswa == null ? null : hasilUjianMahasiswa.getNilai();
                    
                    // MEMASUKKAN INFORMASI JADWAL DAN KUOTA KE JSON UNTUK DIBACA OLEH FRONT-END
                    exam.put("tglMulai", mulaiUjian != null ? WaktuUtil.formatTanggalLengkap(mulaiUjian) : "-");
                    exam.put("tanggalMulai", mulaiUjian != null ? mulaiUjian.getTime() : null); // Timestamp untuk logika JavaScript
                    exam.put("tglSelesai", sampaiUjian != null ? WaktuUtil.formatTanggalLengkap(sampaiUjian) : "-");
                    exam.put("tanggalSelesai", sampaiUjian != null ? sampaiUjian.getTime() : null); // Timestamp untuk logika JavaScript
                    exam.put("maxPercobaan", jmlBolehIkut);
                    exam.put("jumlahIkut", jmlPernahIkut);

                    if (maxNilai == null) {
                        exam.put("status", "BELUM");
                        adaBelumUjian = true;
                    } else {
                        exam.put("status", "SUDAH");
                        // Hanya kirim nilai ke front-end jika admin mengizinkan peserta melihat nilai.
                        // Default lihatNilaiSetelahUjian = false, sehingga nilai TIDAK tampil secara default.
                        if (ppu.getLihatNilaiSetelahUjian()) {
                            exam.put("nilai", maxNilai.doubleValue());
                            nilaiTotal += maxNilai.doubleValue();
                            jumlahUjian += 1.0;
                        }
                    }
                    examsArray.put(exam);
                }
            }

            // 4. Logika Kelulusan & Grade Deskriptif
            String grade = Common.getBahasaConfig("CUKUP");
            double rataRata = (jumlahUjian > 0) ? (nilaiTotal / jumlahUjian) : 0.0;
            
            if (rataRata >= 90.0) grade = Common.getBahasaConfig("UNGGUL");
            else if (rataRata >= 70.0) grade = Common.getBahasaConfig("BAIK SEKALI");
            else if (rataRata >= 50.0) grade = Common.getBahasaConfig("BAIK");

            boolean autoAccepted = false;
            
            if (!adaBelumUjian && nilaiTotal > 0.01) {
                if (Boolean.TRUE.equals(gelombangPendaftaran.getUjianOnlineOtomatisDiterima())) {
                    Double nilaiMinimalOtomatis = gelombangPendaftaran.getNilaiMinimalUjianOnlineOtomatisDiterima();
                    if (nilaiMinimalOtomatis != null && nilaiMinimalOtomatis.doubleValue() <= rataRata && cama.getProdiLulus() == null) {
                        Transaction tx = hibSession.beginTransaction();
                        cama.setProdiLulus(cama.getProdi1());
                        cama.setTanggalDiterima(WaktuUtil.getDate());
                        cama.setStatusLulus(BiodataCalonMahasiswa.LULUS);
                        cama.setKeterangan(Common.getBahasaConfig("Dinyatakan lulus / diterima otomatis karena ikut ujian online"));
                        cama.setGelombangPendaftaranDiterima(gelombangPendaftaran);
                        cama.setJenisSeleksiDipilih(cama.getJenisSeleksi());
                        Common.refreshSaveOrUpdate(hibSession, cama);
                        tx.commit();
                        autoAccepted = true;
                        
                        // Eksekusi generate Keterangan Lulus
                        final BiodataCalonMahasiswa finalCama = cama;
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    ais.action.report.CommonReportHelper.onCetakSuratKeteranganLulus(finalCama, true);
                                } catch (Exception e) { e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online_service.jsp:193"); }
                            }
                        }).start();
                    }
                }
            }

            responseJson.put("status", "success");
            responseJson.put("exams", examsArray);
            // Angka rata-rata tidak akan ditampilkan di front-end sesuai permintaan, tapi kita tetap bisa melempar ke JSON
            responseJson.put("rataRata", rataRata);
            responseJson.put("grade", grade);
            responseJson.put("allFinished", (!adaBelumUjian && jumlahUjian > 0));
            responseJson.put("autoAccepted", autoAccepted);
            
            String defLulus = Common.getBahasaConfig("Selamat, Saudara dinyatakan telah selesai mengikuti proses evaluasi online. Silakan ikuti tahapan selanjutnya.");
            String defNilai = Common.getBahasaConfig("Kualifikasi kelulusan Anda adalah : ");
            
            responseJson.put("infoLulus", Common.getKonfigurasi("info_lulus_ujian_pmb", defLulus).getNilai());
            responseJson.put("infoNilaiLabel", Common.getKonfigurasi("info_nilia_lulus_ujian_pmb", defNilai).getNilai());
        }

        out.print(responseJson.toString());

    } catch (Exception e) {
        e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online_service.jsp:218");
        out.print(new JSONObject().put("status", "error").put("message", Common.getBahasaConfig("Terjadi kesalahan pada peladen: ") + e.getMessage()));
    } finally {
        if (hibSession != null) {
            try { hibSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online_service.jsp:222");}
            try { hibSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online_service.jsp:223");}
            try { hibSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) webapp/WEB-INF/baru/modul/pmb/_ikut_ujian_online_service.jsp:224");}
        }
    }
%>