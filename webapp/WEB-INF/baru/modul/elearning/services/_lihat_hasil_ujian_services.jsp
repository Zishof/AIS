<%@page import="org.hibernate.Session"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.criterion.*"%>
<%@page import="ais.database.hibernate.HibernateUtil"%>
<%@page import="ais.database.model.*"%>
<%@page import="ais.database.model.sekolah.*"%>
<%@page import="ais.common.*"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="java.util.*"%>
<%@page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%
JSONObject resp = new JSONObject();
Session mySession = null;
Transaction tx = null;

try {
    String ppuParam = request.getParameter("ppu");
    String action = request.getParameter("action");
    
    mySession = HibernateUtil.openSession();
    tx = mySession.beginTransaction(); 
    
    // =========================================================================
    // BLOK AKSI: SIMPAN NILAI MANUAL (ESAI / NON-PG)
    // Termasuk Kalkulasi JSON untuk Mode OBE
    // =========================================================================
    if ("save_manual".equals(action)) {
        Long idHasil = Long.parseLong(request.getParameter("idHasil"));
        boolean inputIsObe = "true".equals(request.getParameter("isObe"));
        HasilUjianMahasiswa hum = (HasilUjianMahasiswa) mySession.get(HasilUjianMahasiswa.class, idHasil);
        
        if (hum != null) {
            if (inputIsObe) {
                // Pemrosesan Data Manual Berformat JSON untuk OBE
                String obeDataStr = request.getParameter("obeData");
                JSONObject newObe = new JSONObject(obeDataStr);
                JSONObject humObe = new JSONObject(hum.getNilaiObe() != null && hum.getNilaiObe().trim().startsWith("{") ? hum.getNilaiObe() : "{}");
                
                PertemuanPunyaUjian currentPpu = hum.getPertemuanPunyaUjian();
                JSONObject ppuMapping = new JSONObject(currentPpu.getFormatNilais() != null ? currentPpu.getFormatNilais() : "{}");
                
                double totalBobot = 0;
                double totalSkorBerbobot = 0;
                
                Iterator<String> keys = newObe.keys();
                while(keys.hasNext()) {
                    String fnIdStr = keys.next();
                    double score = newObe.getDouble(fnIdStr);
                    
                    // Simpan nilai yang didapat per CPMK dan atur nilai maksimum default (100)
                    humObe.put(fnIdStr, score);
                    humObe.put(fnIdStr + "_max", 100.0); 
                    
                    // Hitung akumulasi nilai total berdasar bobot
                    double bobot = ppuMapping.isNull(fnIdStr + "_bobot") ? 100.0 : ppuMapping.getDouble(fnIdStr + "_bobot");
                    totalBobot += bobot;
                    totalSkorBerbobot += (score * bobot);
                }
                
                hum.setNilaiObe(humObe.toString());
                if(totalBobot > 0) {
                    hum.setNilai(totalSkorBerbobot / totalBobot);
                }
            } else {
                // Pemrosesan Nilai Manual Reguler (Non-OBE)
                Double nilai = Double.parseDouble(request.getParameter("nilai"));
                hum.setNilai(nilai);
            }
            mySession.update(hum);
        }
        tx.commit(); tx = null;
        resp.put("status", "success");
        out.print(resp.toString()); out.flush(); return;
    }

    // =========================================================================
    // BLOK AKSI: HITUNG ULANG NILAI ESAI
    // =========================================================================
    if ("recount_essay".equals(action)) {
        Long idHasil = Long.parseLong(request.getParameter("idHasil"));
        HasilUjianMahasiswa hum = (HasilUjianMahasiswa) mySession.get(HasilUjianMahasiswa.class, idHasil);
        if (hum != null) {
            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(hum.getPertemuanPunyaUjian().getJmlDitampilkan(), null, true);
            
            Double sumNilai = 0.0;
            for(Long idSoal : ujianPunyaSoals) {
                 UjianPunyaSoal ups = (UjianPunyaSoal) GeneralValueObject.ambilData(UjianPunyaSoal.class, idSoal.toString());
                 if(ups != null) {
                      BankSoal bs = ups.getBankSoal();
                      HasilUjianMahasiswaDetail humd = (HasilUjianMahasiswaDetail) mySession.createCriteria(HasilUjianMahasiswaDetail.class)
                          .add(Restrictions.eq("hasilUjianMahasiswa", hum))
                          .add(Restrictions.eq("ujianPunyaSoal", ups)).setMaxResults(1).uniqueResult();

                      if(humd != null && humd.getNilai() != null && bs.getSkor() != null && bs.getSkor() > 0) {
                          sumNilai += (humd.getNilai() * 100.0) / bs.getSkor();
                      }
                 }
            }
            Double newNilai = 0.0;
            if(ujianPunyaSoals.size() > 0) {
                newNilai = sumNilai / ujianPunyaSoals.size();
                hum.setNilai(newNilai);
                mySession.update(hum);
            }
            tx.commit(); tx = null;
            resp.put("status", "success");
            resp.put("new_nilai", Common.numberFormat.get().format(newNilai));
        } else {
            resp.put("status", "error");
            resp.put("message", "Data Hasil Ujian tidak ditemukan.");
        }
        out.print(resp.toString()); out.flush(); return;
    }

    // =========================================================================
    // BLOK MUAT DATA DAFTAR PESERTA UJIAN (DEFAULT VIEW)
    // =========================================================================
    String keyword = request.getParameter("keyword");
    boolean doRefresh = "true".equalsIgnoreCase(request.getParameter("refresh"));
    String namaVal = keyword != null ? keyword.trim() : "";
    
    PertemuanPunyaUjian ppu = (PertemuanPunyaUjian) mySession.get(PertemuanPunyaUjian.class, Long.parseLong(ppuParam.trim()));
    if (ppu == null) throw new Exception("Data ujian tidak valid.");

    boolean isPG = ppu.getUjian() != null && BankSoal.PILIHAN_GANDA.equals(ppu.getUjian().getJenis());
    
    // Cek Status OBE dan Ambil Format Nilai
    boolean isObe = false;
    List<FormatNilai> formatNilais = new ArrayList<FormatNilai>();
    if (ppu.getPertemuan() != null && ppu.getPertemuan().getPerkuliahan() != null) {
        if (ppu.getPertemuan().getPerkuliahan().getKurikulum() != null && 
            ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(
                ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), 
                ppu.getPertemuan().getPerkuliahan().getGanjilGenap())) {
            isObe = true;
            formatNilais = Common.getFormatNilais(mySession, ppu.getPertemuan().getPerkuliahan());
        }
    }
    
    JSONObject ppuMapping = new JSONObject(ppu.getFormatNilais() != null ? ppu.getFormatNilais() : "{}");

    // --- [LOGIKA PENGAMBILAN DATA PESERTA] ---
    List<VOMahasiswa> mahasiswasTemorary = new ArrayList<VOMahasiswa>();
    int jumlahPeserta = 0;

    if (ppu.getPertemuan() != null && ppu.getPertemuan().getJadwalUjianPMB() != null) {
        if (ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB() != null
                && ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null
                && ppu.getPertemuan().getJadwalUjianPMB().getPesertaUjianHarusTelahUjian()) {
            
            mahasiswasTemorary = ConstantValues.simpleList(
                    mySession.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("mulaiPada"))
                            .add(Restrictions.isNotNull("keyhasil"))
                            .setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
                            .add(Restrictions.isNotNull("biodataCalonMahasiswa"))
                            .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
                            .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                                    : Restrictions.or(
                                            Restrictions.ilike("biodataCalonMahasiswa.nama", namaVal, MatchMode.ANYWHERE),
                                            Restrictions.or(
                                                    Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", namaVal, MatchMode.ANYWHERE),
                                                    Restrictions.ilike("biodataCalonMahasiswa.noUjian", namaVal, MatchMode.ANYWHERE))))
                            .add(Restrictions.eq("pertemuanPunyaUjian", ppu)),
                    BiodataCalonMahasiswa.class, false);
        }
        else if (ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB() != null
                && ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null
                && !ppu.getPertemuan().getJadwalUjianPMB().getRuanganYgIkut().isEmpty()) {

            mahasiswasTemorary = ConstantValues.simpleList(mySession.createCriteria(RuangPaketPMB.class)
                    .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
                    .add(ppu.getPertemuan().getJadwalUjianPMB().getPesertaUjianHarusPunyaNomorUjian()
                                    ? Restrictions.and(Restrictions.ne("biodataCalonMahasiswa.noUjian", ""),
                                            Restrictions.isNotNull("biodataCalonMahasiswa.noUjian"))
                                    : Restrictions.sqlRestriction("true"))
                    .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                            : Restrictions.or(
                                    Restrictions.ilike("biodataCalonMahasiswa.nama", namaVal, MatchMode.ANYWHERE),
                                    Restrictions.or(
                                            Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", namaVal, MatchMode.ANYWHERE),
                                            Restrictions.ilike("biodataCalonMahasiswa.noUjian", namaVal, MatchMode.ANYWHERE))))
                    .setProjection(Projections.property("biodataCalonMahasiswa.id"))
                    .add(Restrictions.sqlRestriction("ruang_pmb in (-1"
                            + ppu.getPertemuan().getJadwalUjianPMB().getRuanganYgIkut() + "-1)")),
                    BiodataCalonMahasiswa.class, false);
        }
        else if (ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB() != null
                && ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
            
            mahasiswasTemorary = ConstantValues.simpleList(
                    mySession.createCriteria(BiodataCalonMahasiswa.class)
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
                            .add(ppu.getPertemuan().getJadwalUjianPMB().getPesertaUjianHarusPunyaNomorUjian()
                                            ? Restrictions.and(Restrictions.ne("noUjian", ""), Restrictions.isNotNull("noUjian"))
                                            : Restrictions.sqlRestriction("true"))
                            .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                                    : Restrictions.or(
                                            Restrictions.ilike("nama", namaVal, MatchMode.ANYWHERE),
                                            Restrictions.or(
                                                    Restrictions.ilike("noRegistrasi", namaVal, MatchMode.ANYWHERE),
                                                    Restrictions.ilike("noUjian", namaVal, MatchMode.ANYWHERE))))
                            .add(ppu.getPertemuan().getJadwalUjianPMB().getPaket() == null
                                    ? Restrictions.sqlRestriction("true")
                                    : Restrictions.eq("paket", ppu.getPertemuan().getJadwalUjianPMB().getPaket()))
                            .add(Restrictions.eq("gelombangPendaftaran", ppu.getPertemuan().getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran()))
                            .addOrder(Order.asc("noRegistrasi")),
                    BiodataCalonMahasiswa.class);

            jumlahPeserta = mahasiswasTemorary.size();
        } else {
            mahasiswasTemorary = ConstantValues.simpleList(
                    mySession.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
                            .setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
                            .add(Restrictions.isNotNull("biodataCalonMahasiswa"))
                            .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
                            .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                                    : Restrictions.or(
                                            Restrictions.ilike("biodataCalonMahasiswa.nama", namaVal, MatchMode.ANYWHERE),
                                            Restrictions.or(
                                                    Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", namaVal, MatchMode.ANYWHERE),
                                                    Restrictions.ilike("biodataCalonMahasiswa.noUjian", namaVal, MatchMode.ANYWHERE))))
                            .add(Restrictions.eq("pertemuanPunyaUjian", ppu)),
                    BiodataCalonMahasiswa.class, false);
        }
    } else if (ppu.getPertemuan() != null) {
        mahasiswasTemorary = new ArrayList<VOMahasiswa>();
        List<Mahasiswa> temp = ppu.getPertemuan().ambilMahasiswa();
        List<Mahasiswa> mahasiswas = new ArrayList<Mahasiswa>();
        for (Mahasiswa maha : temp) {
            if (namaVal.isEmpty()
                    || (!namaVal.isEmpty() && ((maha.getNama() != null
                            && maha.getNama().toLowerCase().contains(namaVal.toLowerCase()))
                            || (maha.getNim() != null && maha.getNim().toLowerCase().contains(namaVal.toLowerCase()))))) {
                Long id = maha.getId();
                if (!ppu.getMhsYgTidakIkut().contains("," + id + ",")) {
                    mahasiswas.add(maha);
                }
            }
        }
        mahasiswasTemorary.addAll(mahasiswas);
        jumlahPeserta = mahasiswasTemorary.size();
    } else {
        mahasiswasTemorary = ConstantValues.simpleList(
                mySession.createCriteria(HasilUjianMahasiswa.class).add(Restrictions.isNotNull("keyhasil"))
                        .setProjection(Projections.groupProperty("biodataCalonMahasiswa.id"))
                        .add(Restrictions.isNotNull("biodataCalonMahasiswa"))
                        .createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
                        .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                                : Restrictions.or(
                                        Restrictions.ilike("biodataCalonMahasiswa.nama", namaVal, MatchMode.ANYWHERE),
                                        Restrictions.or(
                                                Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", namaVal, MatchMode.ANYWHERE),
                                                Restrictions.ilike("biodataCalonMahasiswa.noUjian", namaVal, MatchMode.ANYWHERE))))
                        .add(Restrictions.eq("pertemuanPunyaUjian", ppu)),
                BiodataCalonMahasiswa.class, false);

        List<VOMahasiswa> mhs = ConstantValues.simpleList(
                        mySession.createCriteria(HasilUjianMahasiswa.class)
                                .add(Restrictions.isNotNull("keyhasil"))
                                .setProjection(Projections.groupProperty("mahasiswa.id"))
                                .add(Restrictions.isNotNull("mahasiswa"))
                                .createAlias("mahasiswa", "mahasiswa")
                                .add(namaVal.isEmpty() ? Restrictions.sqlRestriction("true")
                                        : Restrictions.or(
                                                Restrictions.ilike("mahasiswa.nama", namaVal, MatchMode.ANYWHERE),
                                                Restrictions.ilike("mahasiswa.nim", namaVal, MatchMode.ANYWHERE)))
                                .add(Restrictions.eq("pertemuanPunyaUjian", ppu)),
                        Mahasiswa.class, false);

        mahasiswasTemorary.addAll(mhs);
        jumlahPeserta = mahasiswasTemorary.size();
    }

    if (jumlahPeserta == 0) jumlahPeserta = mahasiswasTemorary.size();

    // --- [MENGURAI DATA HASIL PER MAHASISWA] ---
    boolean reloadNama = doRefresh && namaVal.isEmpty();
    if (reloadNama) {
        ppu.bersihkanLokasiHasilUjianMahasiswa();
        ppu.tulisLokasiHasilUjianMahasiswa(new JSONObject().toString());
    }

    int jatahSoalPerAnak = ppu.getJmlDitampilkan() != null ? ppu.getJmlDitampilkan() : 0;
    int globalTotalSoalBeredar = jumlahPeserta * jatahSoalPerAnak;
    int globalSoalTerjawab = 0;
    int pesertaYgIkutUjian = 0;

    JSONArray arrPeserta = new JSONArray();

    for (VOMahasiswa voMahasiswa : mahasiswasTemorary) {
        if (voMahasiswa != null) {
            Mahasiswa voMhs = (voMahasiswa instanceof Mahasiswa) ? (Mahasiswa) voMahasiswa : null;
            BiodataCalonMahasiswa voCal = (voMahasiswa instanceof BiodataCalonMahasiswa) ? (BiodataCalonMahasiswa) voMahasiswa : null;

            HasilUjianMahasiswa hum = HasilUjianMahasiswa.ambilByKey(ppu, voMhs, voCal, null, null);
            if (reloadNama) ppu.populateHasilUjianMahasiswa(hum, true);

            ais.ui.util.MyArrayList<Long> ujianPunyaSoals = hum.ambilUjianPunyaSoals(ppu.getJmlDitampilkan(), null, true);
            Set<Long> terjwbSet = hum.ambilBankSoalIdTerjawab(ppu.getJmlDitampilkan(), ujianPunyaSoals, doRefresh);
            
            int terjawab = (terjwbSet != null) ? terjwbSet.size() : 0;
            globalSoalTerjawab += terjawab;
            if (terjawab > 0) pesertaYgIkutUjian++;

            JSONObject jp = new JSONObject();
            jp.put("idHasil", hum.getId());
            jp.put("nama", voMahasiswa.getNama() != null ? voMahasiswa.getNama() : "Tanpa Nama");
            
            if (voMhs != null) {
                jp.put("nim", voMhs.getNim() != null ? voMhs.getNim() : "-");
                jp.put("foto", CommonMedia.getUrlFotoPengguna(new Tbmuser(voMhs)));
            } else if (voCal != null) {
                jp.put("nim", voCal.getNoRegistrasi() != null ? voCal.getNoRegistrasi() : "-");
                jp.put("foto", Common.ROOT + "/images/default_user.png");
            }
            
            jp.put("waktuMulai", hum.getMulaiPada() != null ? Common.timeFormat.get().format(hum.getMulaiPada()) : "-");
            jp.put("waktuSelesai", hum.getSelesaiPada() != null ? Common.timeFormat.get().format(hum.getSelesaiPada()) : "-");
            
            int totalSoal = (hum.getJumlahSoal() != null && hum.getJumlahSoal() > 0) ? hum.getJumlahSoal().intValue() : jatahSoalPerAnak;
            double persenSelesai = totalSoal > 0 ? (100.0 * terjawab) / totalSoal : 0;
            
            jp.put("soalTerjawab", terjawab);
            jp.put("totalSoal", totalSoal);
            jp.put("persenSelesai", Math.min(100, Math.round(persenSelesai)));
            
            jp.put("isPG", isPG);
            jp.put("isObe", isObe);
            jp.put("jawabanBenar", hum.getJawabanBenar() != null ? hum.getJawabanBenar() : 0);
            jp.put("maxBenar", hum.getJawabanBenarMax() != null ? hum.getJawabanBenarMax() : totalSoal);
            jp.put("nilaiAkhir", hum.getNilai() != null ? Common.numberFormat.get().format(hum.getNilai()) : "0");
            
            // Susun array skor per Sub-CPMK agar mudah di-render Frontend
            if (isObe) {
                JSONArray obeScores = new JSONArray();
                JSONObject humObe = new JSONObject(hum.getNilaiObe() != null && hum.getNilaiObe().trim().startsWith("{") ? hum.getNilaiObe() : "{}");
                
                for(FormatNilai fn : formatNilais) {
                    if (fn.getStatusPertemuan() != null && !ppuMapping.isNull(fn.getId().toString())) {
                        Double nilaiDapat = humObe.isNull(fn.getId().toString()) ? 0.0 : humObe.getDouble(fn.getId().toString());
                        Double nilaiMax = humObe.isNull(fn.getId().toString() + "_max") ? 0.0 : humObe.getDouble(fn.getId().toString() + "_max");
                        
                        JSONObject os = new JSONObject();
                        os.put("id", fn.getId());
                        os.put("nama", fn.getNama());
                        os.put("nilaiDapat", nilaiDapat);
                        os.put("nilaiStr", nilaiMax.equals(0.0) ? "" : Common.numberFormat.get().format((nilaiDapat * 100.0) / nilaiMax));
                        obeScores.put(os);
                    }
                }
                jp.put("obeScores", obeScores);
            }
            
            arrPeserta.put(jp);
        }
    }
    
    JSONObject stat = new JSONObject();
    stat.put("jmlPeserta", jumlahPeserta);
    stat.put("jmlIkut", pesertaYgIkutUjian);
    stat.put("jmlBelumIkut", Math.max(0, jumlahPeserta - pesertaYgIkutUjian));
    stat.put("persenIkut", jumlahPeserta > 0 ? (100.0 * pesertaYgIkutUjian) / jumlahPeserta : 0);
    stat.put("persenBelumIkut", jumlahPeserta > 0 ? (100.0 * (jumlahPeserta - pesertaYgIkutUjian)) / jumlahPeserta : 0);
    
    int globalSoalKosong = Math.max(0, globalTotalSoalBeredar - globalSoalTerjawab);
    stat.put("totalSoalBeredar", globalTotalSoalBeredar);
    stat.put("soalTerjawab", globalSoalTerjawab);
    stat.put("soalKosong", globalSoalKosong);
    stat.put("persenTerjawab", globalTotalSoalBeredar > 0 ? (100.0 * globalSoalTerjawab) / globalTotalSoalBeredar : 0);
    stat.put("persenKosong", globalTotalSoalBeredar > 0 ? (100.0 * globalSoalKosong) / globalTotalSoalBeredar : 0);

    resp.put("status", "success");
    resp.put("peserta", arrPeserta);
    resp.put("statistik", stat);
    
    tx.commit(); tx = null;

} catch (Exception e) {
    if (tx != null) tx.rollback();
    e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit webapp/WEB-INF/baru/modul/elearning/services/_lihat_hasil_ujian_services.jsp:382");
    resp.put("status", "error");
    resp.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
} finally {
    ais.common.ElearningSessionUtil.closeQuietly(mySession);
}

out.print(resp.toString());
out.flush();
%>