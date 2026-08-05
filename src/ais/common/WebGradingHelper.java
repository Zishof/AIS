package ais.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.FormatNilai;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.NamaTugasKelompokPunyaMahasiswa;
import ais.database.model.NilaiHuruf;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaUjian;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.file.TugasFileContent;

public class WebGradingHelper {

    // 1. Fungsi Sinkronisasi Utama untuk Kurikulum OBE
    public static void sinkronObe(Perkuliahan perkuliahan, String formatNilaisData, Tbmuser tbmuser) throws Exception {
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            List<FormatNilai> formatNilais = Common.getFormatNilais(session, perkuliahan);
            JSONObject jsonObject = new JSONObject(formatNilaisData);
            
            for (FormatNilai nilai : formatNilais) {
                if (nilai.getStatusPertemuan() != null && !jsonObject.isNull(nilai.getId().toString())) {
                    // Lakukan kalkulasi per Sub-CPMK secara berurutan
                    kalkulasiDanSimpanNilai(perkuliahan, nilai, tbmuser);
                }
            }
        } finally {
            if (session != null && session.isOpen()) {
                try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/WebGradingHelper.java:47");}
            }
        }
    }

    // 2. Fungsi Sinkronisasi Utama untuk Kurikulum Non-OBE
    public static void sinkronNonObe(Perkuliahan perkuliahan, FormatNilai fn, Tbmuser tbmuser) throws Exception {
        kalkulasiDanSimpanNilai(perkuliahan, fn, tbmuser);
    }

    // 3. Mesin Kalkulasi Inti (Murni Logika Database & Matematika, Tanpa UI ZKoss)
    @SuppressWarnings("unchecked")
    private static void kalkulasiDanSimpanNilai(Perkuliahan kulParam, FormatNilai fnParam, Tbmuser tbmuser) throws Exception {
        if (fnParam == null || kulParam == null) return;

        Session session = null;
        Transaction tx = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();

            // PENTING: Menarik ulang entitas (Re-attach) ke dalam Session yang baru dibuka
            // Ini untuk mencegah "LazyInitializationException" saat memanggil child collections
            Perkuliahan kul = (Perkuliahan) session.get(Perkuliahan.class, kulParam.getId());
            FormatNilai fn = (FormatNilai) session.get(FormatNilai.class, fnParam.getId());

            if (kul.getDikunci() != null) throw new Exception("Penilaian untuk perkuliahan ini telah terkunci.");

            Perkuliahan kuliyah = kul.getMerupakan_paralel() && kul.getPerkuliahan_paralel() != null ? kul.getPerkuliahan_paralel() : kul;
            List<Perkuliahan> perkuliahans = kuliyah.ambilParalelPerkuliahan();
            if (!perkuliahans.contains(kuliyah)) perkuliahans.add(kuliyah);

            List<FormatNilai> masterFormatNilais = kuliyah.ambilFormatNilai(session);
            Collection<Long> detailperkuliahans = kuliyah.ambilDetailperkuliahan();

            // A. Mengumpulkan Seluruh Komponen Penilaian (Ujian, Tugas, Kelompok)
            List<Pertemuan> pertemuanTugas1 = new ArrayList<Pertemuan>();
            for (Perkuliahan p : perkuliahans) pertemuanTugas1.addAll(p.ambilPertemuanList());

            Double totalPersen = 0.0;
            List<TugasKelompok> tugasKelompoks = new ArrayList<TugasKelompok>();
            List<PertemuanPunyaUjian> pertemuanPunyaUjians = new ArrayList<PertemuanPunyaUjian>();

            for (Perkuliahan perkuliahan : perkuliahans) {
                boolean isObe = perkuliahan.getKurikulum() != null && perkuliahan.getKurikulum().apakahObe(perkuliahan.getTahunAjaran(), perkuliahan.getGanjilGenap());

                // Ambil Tugas Kelompok
                List<TugasKelompok> tkLocal = session.createCriteria(TugasKelompok.class)
                        .add(Restrictions.eq("perkuliahan", perkuliahan)).list();
                for (TugasKelompok tk : tkLocal) {
                    if (isObe) {
                        JSONObject jo = new JSONObject(tk.getFormatNilais());
                        if (!jo.isNull(fn.getId().toString())) tugasKelompoks.add(tk);
                    } else if (tk.getFormatNilai() != null && tk.getFormatNilai().getStatusPertemuan() != null && fn.getStatusPertemuan() != null && tk.getFormatNilai().getStatusPertemuan().getId().equals(fn.getStatusPertemuan().getId())) {
                        tugasKelompoks.add(tk);
                    }
                }

                // Ambil Ujian
                for (Pertemuan pertemuan : pertemuanTugas1) {
                    TreeMap<Long, PertemuanPunyaUjian> ppus = pertemuan.ambilPertemuanPunyaUjianTotal(tbmuser);
                    for (PertemuanPunyaUjian ppu : ppus.values()) {
                        if (isObe) {
                            JSONObject jo = new JSONObject(ppu.getFormatNilais());
                            if (!jo.isNull(fn.getId().toString())) pertemuanPunyaUjians.add(ppu);
                        } else if (ppu.getFormatNilai() != null && fn.getStatusPertemuan() != null && ppu.getFormatNilai().getStatusPertemuan() != null && ppu.getFormatNilai().getStatusPertemuan().getId().equals(fn.getStatusPertemuan().getId())) {
                            pertemuanPunyaUjians.add(ppu);
                        }
                    }
                }
            }

            // Hitung Total Persen Penilaian
            for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
                boolean isObe = ppu.getPertemuan().getPerkuliahan().getKurikulum() != null && ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), ppu.getPertemuan().getPerkuliahan().getGanjilGenap());
                if (isObe) {
                    JSONObject jo = new JSONObject(ppu.getFormatNilais());
                    totalPersen += jo.isNull(fn.getId().toString() + "_bobot") ? 100.0 : jo.getDouble(fn.getId().toString() + "_bobot");
                } else {
                    totalPersen += ppu.getProsentase();
                }
            }

            for (Pertemuan p : pertemuanTugas1) {
                if (!p.getJudultugas().trim().isEmpty()) {
                    boolean isObe = p.getPerkuliahan().getKurikulum() != null && p.getPerkuliahan().getKurikulum().apakahObe(p.getPerkuliahan().getTahunAjaran(), p.getPerkuliahan().getGanjilGenap());
                    if (isObe) {
                        JSONObject jo = new JSONObject(p.getFormatNilais());
                        if (!jo.isNull(fn.getId().toString())) totalPersen += jo.getDouble(fn.getId().toString());
                    } else if (fn.getStatusPertemuan() != null && p.getFormatNilai() != null && p.getFormatNilai().getStatusPertemuan() != null && p.getFormatNilai().getStatusPertemuan().getId().equals(fn.getStatusPertemuan().getId())) {
                        totalPersen += p.getProsentase();
                    }
                }
                for (TugasPertemuan tp : p.ambilTugasPertemuanTotal().values()) {
                    if (!tp.getJudultugas().trim().isEmpty()) {
                        boolean isObe = p.getPerkuliahan().getKurikulum() != null && p.getPerkuliahan().getKurikulum().apakahObe(p.getPerkuliahan().getTahunAjaran(), p.getPerkuliahan().getGanjilGenap());
                        if (isObe) {
                            JSONObject jo = new JSONObject(tp.getFormatNilais());
                            if (!jo.isNull(fn.getId().toString())) totalPersen += jo.getDouble(fn.getId().toString());
                        } else if (fn.getStatusPertemuan() != null && tp.getFormatNilai() != null && tp.getFormatNilai().getStatusPertemuan() != null && tp.getFormatNilai().getStatusPertemuan().getId().equals(fn.getStatusPertemuan().getId())) {
                            totalPersen += tp.getProsentase();
                        }
                    }
                }
            }

            for (TugasKelompok tk : tugasKelompoks) {
                boolean isObe = tk.getPerkuliahan().getKurikulum() != null && tk.getPerkuliahan().getKurikulum().apakahObe(tk.getPerkuliahan().getTahunAjaran(), tk.getPerkuliahan().getGanjilGenap());
                if (isObe) {
                    JSONObject jo = new JSONObject(tk.getFormatNilais());
                    if (!jo.isNull(fn.getId().toString())) totalPersen += jo.getDouble(fn.getId().toString());
                } else {
                    totalPersen += tk.getProsentase();
                }
            }

            // B. Iterasi Per Mahasiswa untuk Menghitung & Menyimpan Nilai Akhir
            int count = 0;
            for (Long dpId : detailperkuliahans) {
                Detailperkuliahan dp = (Detailperkuliahan) session.get(Detailperkuliahan.class, dpId);
                if (dp == null) continue;

                Double totalPersenTidakIkut = 0.0;
                Long mhsId = dp.getMahasiswa().getId();

                // Pengecekan Dispensasi Tidak Ikut
                for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
                    if (ppu.getMhsYgTidakIkut().contains("," + mhsId + ",")) totalPersenTidakIkut += ppu.getProsentase(); 
                }
                
                Double nilaiSemua = 0.0;

                // Kalkulasi Nilai Ujian
                for (PertemuanPunyaUjian ppu : pertemuanPunyaUjians) {
                    if (!ppu.getMhsYgTidakIkut().contains("," + mhsId + ",")) {
                        boolean isObe = ppu.getPertemuan().getPerkuliahan().getKurikulum() != null && ppu.getPertemuan().getPerkuliahan().getKurikulum().apakahObe(ppu.getPertemuan().getPerkuliahan().getTahunAjaran(), ppu.getPertemuan().getPerkuliahan().getGanjilGenap());
                        if (isObe) {
                            JSONObject joPpu = new JSONObject(ppu.getFormatNilais());
                            if (!joPpu.isNull(fn.getId().toString())) {
                                Double bobot = joPpu.isNull(fn.getId().toString() + "_bobot") ? 100.0 : joPpu.getDouble(fn.getId().toString() + "_bobot");
                                String nilaiObeStr = (String) session.createCriteria(HasilUjianMahasiswa.class)
                                        .add(Restrictions.isNotNull("keyhasil")).setMaxResults(1).setProjection(Projections.property("nilaiObe"))
                                        .add(Restrictions.eq("pertemuanPunyaUjian", ppu)).add(Restrictions.eq("mahasiswa", dp.getMahasiswa())).uniqueResult();
                                
                                if (nilaiObeStr != null && !nilaiObeStr.trim().isEmpty()) {
                                    JSONObject joHasil = new JSONObject(nilaiObeStr);
                                    Double nilaiSkor = joHasil.isNull(fn.getId().toString()) ? 0.0 : joHasil.getDouble(fn.getId().toString());
                                    Double nilaiMax = joHasil.isNull(fn.getId().toString() + "_max") ? 0.0 : joHasil.getDouble(fn.getId().toString() + "_max");
                                    Double nilaiDidapat = nilaiMax.equals(0.0) ? 0.0 : (nilaiSkor * 100.0) / nilaiMax;
                                    if ((totalPersen - totalPersenTidakIkut) > 0) {
                                        nilaiSemua += (nilaiDidapat * bobot) / (totalPersen - totalPersenTidakIkut);
                                    }
                                }
                            }
                        } else {
                            Number nUjian = (Number) session.createCriteria(HasilUjianMahasiswa.class)
                                    .add(Restrictions.isNotNull("keyhasil")).setMaxResults(1).setProjection(Projections.property("nilai"))
                                    .add(Restrictions.eq("pertemuanPunyaUjian", ppu)).add(Restrictions.eq("mahasiswa", dp.getMahasiswa())).uniqueResult();
                            if (nUjian != null) {
                                if ((totalPersen - totalPersenTidakIkut) > 0) {
                                    nilaiSemua += (nUjian.doubleValue() * ppu.getProsentase()) / (totalPersen - totalPersenTidakIkut);
                                }
                            }
                        }
                    }
                }

                // Kalkulasi Nilai Tugas Pertemuan
                for (Pertemuan p : pertemuanTugas1) {
                    if (!p.getJudultugas().trim().isEmpty() && !p.getMhsYgTidakIkut().contains("," + mhsId + ",")) {
                         TugasFileContent tfc = p.ambilTugasFileContent(dp.getMahasiswa());
                         if(tfc != null) {
                             Double bobot = p.getProsentase();
                             if ((totalPersen - totalPersenTidakIkut) > 0) {
                                 nilaiSemua += (tfc.getNilai() * bobot) / (totalPersen - totalPersenTidakIkut);
                             }
                         }
                    }
                }

                // Kalkulasi Nilai Tugas Kelompok
                for (TugasKelompok tk : tugasKelompoks) {
                    if (!tk.getMhsYgTidakIkut().contains("," + mhsId + ",")) {
                        Number nTugas = (Number) session.createCriteria(NamaTugasKelompokPunyaMahasiswa.class)
                                .setMaxResults(1).setProjection(Projections.property("nilai"))
                                .createAlias("namaTugasKelompok", "namaTugasKelompok")
                                .add(Restrictions.eq("namaTugasKelompok.tugasKelompok", tk))
                                .add(Restrictions.eq("mahasiswa", dp.getMahasiswa())).uniqueResult();
                        if (nTugas != null) {
                            if ((totalPersen - totalPersenTidakIkut) > 0) {
                                nilaiSemua += (nTugas.doubleValue() * tk.getProsentase()) / (totalPersen - totalPersenTidakIkut);
                            }
                        }
                    }
                }

                // C. Proses Penyimpanan (Update Tabel Detailperkuliahan)
                dp.populateDetailNilai(fn, null, nilaiSemua, true, tbmuser);
                Matakuliah matakuliah = dp.getPerkuliahan() != null ? dp.getPerkuliahan().getMatakuliah() : dp.getMatakuliahKonversi();

                Double total = dp.hitungTotalNilai(true, masterFormatNilais);
                NilaiHuruf nilaiHuruf = Common.getNilaiHuruf(total, dp.getMahasiswa().getTahunangkatan(), dp.getMahasiswa().getJurusan(), dp.getMahasiswa().getJurusan().getFakultas(), dp.getTahunAkademik(), dp.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, matakuliah == null ? "" : matakuliah.getKode(), matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

                dp.setTotalIP(nilaiHuruf == null ? 0.0 : nilaiHuruf.getNilaiDiIPK());
                dp.setTotalNilai(total);
                dp.setNilaiHuruf(nilaiHuruf == null ? "" : nilaiHuruf.getNilaiHuruf());
                dp.setLulus(nilaiHuruf == null ? null : nilaiHuruf.getLulus());

                // Hitung Sementara
                Double totalSementara = dp.hitungTotalNilaiSementara(true, masterFormatNilais);
                NilaiHuruf nilaiHurufSementara = Common.getNilaiHuruf(totalSementara, dp.getMahasiswa().getTahunangkatan(), dp.getMahasiswa().getJurusan(), dp.getMahasiswa().getJurusan().getFakultas(), dp.getTahunAkademik(), dp.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL, matakuliah == null ? "" : matakuliah.getKode(), matakuliah == null ? null : matakuliah.getJenisNilaiHuruf());

                dp.setTotalNilaiSementara(totalSementara);
                dp.setNilaiHurufSementara(nilaiHurufSementara == null ? "" : nilaiHurufSementara.getNilaiHuruf());
                dp.setTotalIPSementara(nilaiHurufSementara == null ? 0.0 : nilaiHurufSementara.getNilaiDiIPK());

                session.update(dp);

                // Batching Flush untuk Mencegah Memory Leak (Out of Memory)
                count++;
                if (count % 50 == 0) {
                    session.flush();
                    session.clear();
                }
            }

            tx.commit();

            

        } catch (Exception e) {
            // Perbaikan Utama: Memastikan tx.isActive() sebelum melakukan rollback
            // agar mencegah error "Transaction not successfully started"
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e; 
        } finally {
            if (session != null && session.isOpen()) {
                try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/WebGradingHelper.java:287");}
            }
        }
    }
}