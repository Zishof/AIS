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

/**
 * Mesin kalkulasi dan penyimpanan nilai akhir mahasiswa untuk satu mata kuliah (satu
 * {@link Perkuliahan}), dipakai oleh fitur "Web Grading" — antarmuka non-ZKoss (kemungkinan REST/
 * servlet, mengingat nama "Web") bagi dosen untuk memicu perhitungan ulang nilai akhir. Kelas ini
 * murni berisi logika database dan matematika (secara eksplisit dipisahkan dari komponen UI ZKoss
 * sesuai komentar penulis aslinya), mendukung DUA rezim kurikulum yang berjalan berdampingan di
 * AIS:
 * <ul>
 * <li><b>OBE (Outcome-Based Education)</b> — bobot komponen nilai (ujian, tugas per pertemuan,
 * tugas kelompok) disimpan sebagai JSON per {@link FormatNilai} (kunci {@code
 * formatNilaiId}/{@code formatNilaiId + "_bobot"}) pada masing-masing entitas komponen, sehingga
 * satu komponen penilaian dapat berkontribusi ke banyak Sub-CPMK (Capaian Pembelajaran Mata
 * Kuliah) sekaligus dengan bobot berbeda-beda per Sub-CPMK. Deteksi rezim OBE dilakukan per
 * {@link Perkuliahan} lewat {@code kurikulum.apakahObe(tahunAjaran, ganjilGenap)}.</li>
 * <li><b>Non-OBE (konvensional)</b> — setiap komponen penilaian terikat pada SATU
 * {@link FormatNilai} lewat referensi langsung (bukan JSON multi-kunci), dan bobotnya diambil dari
 * properti {@code prosentase} milik komponen itu sendiri.</li>
 * </ul>
 * <p>
 * Kedua rezim ini ditangani oleh satu mesin kalkulasi yang sama ({@link
 * #kalkulasiDanSimpanNilai(Perkuliahan, FormatNilai, Tbmuser)}), dengan percabangan {@code isObe}
 * di setiap titik pengumpulan/kalkulasi bobot dan skor.
 * </p>
 *
 * <h2>Alur kalkulasi nilai</h2>
 * <ol>
 * <li>Perkuliahan paralel digabung (bila perkuliahan ini "merupakan paralel" dari perkuliahan
 * induk) sehingga komponen penilaian dari seluruh kelas paralel ikut dihitung sebagai satu
 * kesatuan.</li>
 * <li>Seluruh komponen penilaian yang cocok dengan {@link FormatNilai}/Sub-CPMK target
 * dikumpulkan: ujian ({@link PertemuanPunyaUjian}), tugas per pertemuan ({@link Pertemuan}/
 * {@link TugasPertemuan}), dan tugas kelompok ({@link TugasKelompok}).</li>
 * <li>Total persentase bobot ({@code totalPersen}) dijumlahkan dari seluruh komponen yang
 * terkumpul, sebagai penyebut normalisasi nilai akhir.</li>
 * <li>Untuk setiap mahasiswa peserta ({@link Detailperkuliahan}): bobot komponen yang mahasiswa
 * tersebut punya dispensasi tidak ikut (ditandai dalam string {@code mhsYgTidakIkut} berformat
 * {@code ",id,id,..."} pada tiap komponen) dikeluarkan dari penyebut ({@code totalPersenTidakIkut}),
 * lalu nilai tiap komponen yang diikuti dijumlahkan secara proporsional:
 * {@code nilaiSemua += (skor * bobotKomponen) / (totalPersen - totalPersenTidakIkut)}. Untuk OBE,
 * skor ujian dinormalisasi dulu ke skala 100 dari pasangan nilai/nilai-maksimum yang tersimpan
 * dalam JSON {@code nilaiObe}.</li>
 * <li>Nilai akhir disimpan ke {@link Detailperkuliahan} lewat {@code populateDetailNilai}, lalu
 * total nilai dan huruf mutu final maupun "sementara" (nilai berjalan sebelum semua komponen
 * lengkap) dihitung lewat {@code hitungTotalNilai}/{@code hitungTotalNilaiSementara} dan dipetakan
 * ke huruf mutu lewat {@link Common#getNilaiHuruf}.</li>
 * <li>Setiap 50 mahasiswa, session di-{@code flush()} dan {@code clear()} untuk mencegah
 * penumpukan objek ter-cache Hibernate (mencegah OutOfMemory pada perkuliahan dengan peserta
 * sangat banyak) — pola ini menunjukkan kelas ini dirancang untuk beroperasi pada volume data
 * signifikan.</li>
 * </ol>
 *
 * <p>
 * Setiap pemanggilan membuka {@link Session} Hibernate baru dan terpisah dari session HTTP request
 * yang sedang berjalan, lalu menarik ulang ("re-attach") entitas {@link Perkuliahan}/
 * {@link FormatNilai} yang diterima dari pemanggil ke session baru tersebut — pola ini sengaja
 * dipakai untuk mencegah {@code LazyInitializationException} saat mengakses koleksi anak
 * (pertemuan, tugas, dsb.) milik entitas yang mungkin diterima dari session yang sudah tertutup.
 * Perkuliahan yang sudah dikunci ({@code kul.getDikunci() != null}) menolak kalkulasi dengan
 * melempar {@link Exception}.
 * </p>
 */
public class WebGradingHelper {

    /**
     * Titik masuk sinkronisasi nilai untuk kurikulum OBE: menerima payload JSON
     * {@code formatNilaisData} berisi daftar id {@link FormatNilai} (Sub-CPMK) yang perlu dihitung
     * ulang, lalu untuk setiap {@link FormatNilai} milik {@code perkuliahan} yang (a) memiliki
     * {@code statusPertemuan} dan (b) id-nya muncul sebagai kunci pada JSON tersebut, memicu
     * kalkulasi lewat {@link #kalkulasiDanSimpanNilai(Perkuliahan, FormatNilai, Tbmuser)} secara
     * berurutan satu per satu Sub-CPMK.
     *
     * @param perkuliahan      perkuliahan (mata kuliah pada satu kelas) yang nilainya disinkronkan
     * @param formatNilaisData JSON yang kuncinya adalah id {@link FormatNilai} yang diminta untuk
     *                         dihitung ulang (nilai pada tiap kunci tidak dipakai, hanya keberadaan
     *                         kunci yang diperiksa lewat {@code isNull})
     * @param tbmuser          pengguna yang memicu sinkronisasi, diteruskan untuk pencatatan
     *                         audit/histori perubahan nilai
     * @throws Exception diteruskan dari {@link #kalkulasiDanSimpanNilai}, termasuk bila perkuliahan
     *                    sudah terkunci untuk penilaian
     */
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

    /**
     * Titik masuk sinkronisasi nilai untuk kurikulum non-OBE: berbeda dari {@link
     * #sinkronObe(Perkuliahan, String, Tbmuser)}, method ini menerima langsung SATU objek
     * {@link FormatNilai} target (bukan JSON berisi banyak id) karena pada kurikulum non-OBE setiap
     * permintaan sinkronisasi selalu untuk satu kolom nilai tunggal. Hanya meneruskan pemanggilan ke
     * {@link #kalkulasiDanSimpanNilai(Perkuliahan, FormatNilai, Tbmuser)} tanpa logika tambahan.
     *
     * @param perkuliahan perkuliahan (mata kuliah pada satu kelas) yang nilainya disinkronkan
     * @param fn          format nilai/kolom nilai target yang akan dihitung ulang
     * @param tbmuser     pengguna yang memicu sinkronisasi, diteruskan untuk audit
     * @throws Exception diteruskan dari {@link #kalkulasiDanSimpanNilai}
     */
    public static void sinkronNonObe(Perkuliahan perkuliahan, FormatNilai fn, Tbmuser tbmuser) throws Exception {
        kalkulasiDanSimpanNilai(perkuliahan, fn, tbmuser);
    }

    /**
     * Mesin kalkulasi inti — satu-satunya tempat yang benar-benar menghitung dan menyimpan nilai
     * akhir mahasiswa untuk satu {@link FormatNilai}/Sub-CPMK pada satu {@link Perkuliahan} (beserta
     * kelas paralelnya). Dipanggil oleh {@link #sinkronObe} maupun {@link #sinkronNonObe}; murni
     * logika database dan matematika tanpa dependensi komponen UI ZKoss. Lihat javadoc kelas
     * {@link WebGradingHelper} untuk uraian lengkap alur kalkulasi (pengumpulan komponen penilaian,
     * normalisasi bobot, dispensasi tidak-ikut, penyimpanan nilai akhir dan sementara, serta
     * batching flush Hibernate).
     *
     * @param kulParam perkuliahan target (akan ditarik ulang/"re-attach" ke session baru di dalam
     *                 method ini agar koleksi anaknya dapat diakses tanpa
     *                 {@code LazyInitializationException})
     * @param fnParam  format nilai/Sub-CPMK target yang dihitung ulang (juga ditarik ulang ke
     *                 session baru)
     * @param tbmuser  pengguna yang memicu kalkulasi, diteruskan ke {@code populateDetailNilai}
     *                 untuk pencatatan audit/histori
     * @throws Exception bila {@code kulParam}/{@code fnParam} tidak valid setelah re-attach, bila
     *                    perkuliahan sudah terkunci untuk penilaian ({@code kul.getDikunci() !=
     *                    null}), atau bila terjadi kegagalan database di tengah proses (transaksi
     *                    akan di-rollback lebih dulu sebelum exception diteruskan)
     */
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
                                        .add(Restrictions.or(Restrictions.isNotNull("keyhasil"), Restrictions.isNotNull("nilaiObe")))
                                        .setMaxResults(1).setProjection(Projections.property("nilaiObe"))
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
