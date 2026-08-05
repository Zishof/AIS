package ais.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianOlehDosen;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.CommonVO;
import ais.database.model.Dosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.GrupKuosionerUmumDetail;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.OrangTua;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

public class ChecklistPenilaianHelper {

    @SuppressWarnings("unchecked")
    public static Boolean checkStatusChecklist(Dosen dosen, String ganjilGenap, String tahunAjaran) {
        if (dosen == null || dosen.getId() == null) {
            return false;
        }

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();

            // OPTIMASI: Query lebih rapi dan aman
            String sql = "SELECT b.id FROM perkuliahan b " +
                         "WHERE b.tahun_ajaran = :tahunAjaran " +
                         "AND b.ganjil_genap = :ganjilGenap " +
                         "AND :dosenId IN (b.dosen1, b.dosen2, b.dosen3, b.dosen4, b.dosen5, " +
                         "b.dosen6, b.dosen7, b.dosen8, b.dosen9, b.dosen10)";

            List<Number> perkuliahanIds = session.createSQLQuery(sql)
                    .setParameter("tahunAjaran", tahunAjaran)
                    .setParameter("ganjilGenap", ganjilGenap)
                    .setParameter("dosenId", dosen.getId())
                    .list();

            if (perkuliahanIds == null || perkuliahanIds.isEmpty()) {
                return false;
            }

            // Membentuk string keys (data_checklist) di memori
            List<String> dataperkuliahan = new ArrayList<String>(perkuliahanIds.size());
            String prefixDosen = dosen.getId() + "-";
            for (Number perkId : perkuliahanIds) {
                if (perkId != null) {
                    dataperkuliahan.add(prefixDosen + perkId.longValue());
                }
            }

            List<Long> dataPenilaian = session.createCriteria(ChecklistPenilaianDosen.class)
                    .createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")
                    .createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen")
                    .add(Restrictions.eq("angketPenilaianDosen.untukDosen", true))
                    .add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
                    .add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
                            Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
                    .setProjection(Projections.groupProperty("id")).list();

            if (dataPenilaian == null || dataPenilaian.isEmpty()) {
                return false;
            }

            List<String> dataList = ChecklistBaruPenilaianOlehDosen.count(dosen, tahunAjaran, ganjilGenap);
            
            // OPTIMASI MEMORI: List diubah ke HashSet agar pengecekan .contains() berkecepatan O(1)
            Set<String> dataSet = new HashSet<String>();
            if (dataList != null) {
                dataSet.addAll(dataList);
            }

            for (Long idnilai : dataPenilaian) {
                for (String s : dataperkuliahan) {
                    String key = idnilai + "-" + s;
                    if (!dataSet.contains(key)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ChecklistPenilaianHelper.java:102");
            return false;
        } finally {
            if (session != null && session.isOpen()) {
                try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianHelper.java:106");}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getJadwalChecklistUmum(Tbmuser tbmuser) {
        Session session = null;
        List<Object[]> datas = new ArrayList<Object[]>();
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String sqlTambahan = buildSqlTambahanUmum(tbmuser, null, null);

            StringBuilder sql = new StringBuilder();
            sql.append("select a.tahunakademik,a.semester from jadwal_checklist_penilaian_umum a ")
               .append(" inner join grup_checklist_penilaian_umum b on (a.grup_penilaian_umum=b.id) ")
               .append(" left join checklist_penilaian_umum c on (b.id=c.grup_checklist_penilaian_umum)  ")
               .append(" left join parameter_tambahan_angket_umum d on (b.id=d.grup_checklist_penilaian_umum)   ")
               .append(" where a.grup_kuesioner_umum is null and a.tahunakademik is not null and a.semester is not null and b.aktif and (c.aktif or d.id is not null) and sampai >= date('")
               .append(Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())).append("') and mulai <= date('")
               .append(Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())).append("') and ")
               .append(sqlTambahan)
               .append(" group by a.tahunakademik,a.semester ")
               .append(" order by a.tahunakademik,a.semester");

            datas = session.createSQLQuery(sql.toString()).list();
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ChecklistPenilaianHelper.java:133");
        } finally {
            if (session != null && session.isOpen()) {
                try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianHelper.java:136");}
            }
        }
        return datas;
    }

    @SuppressWarnings("unchecked")
    public static List<Object[]> getJadwalChecklistUmumGrup(Tbmuser tbmuser) {
        Session session = null;
        List<Object[]> datas = new ArrayList<Object[]>();
        if (tbmuser == null || tbmuser.getUserId() == null) {
            return datas;
        }
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            StringBuilder sql = new StringBuilder();
            sql.append("select a.tahunakademik, a.semester ")
               .append("from jadwal_checklist_penilaian_umum a ")
               .append("inner join grup_checklist_penilaian_umum b on (a.grup_penilaian_umum=b.id) ")
               .append("left join checklist_penilaian_umum c on (b.id=c.grup_checklist_penilaian_umum) ")
               .append("left join parameter_tambahan_angket_umum d on (b.id=d.grup_checklist_penilaian_umum) ")
               .append("left join parameter_tambahan e on (e.id=d.parameter_tambahan) ")
               .append("left join grup_checklist_penilaian_dosen gd on (gd.id=a.grup_checklist_penilaian_dosen) ")
               .append("left join sekolah.grup_checklist_penilaian_guru gg on (gg.id=a.grup_checklist_penilaian_guru) ")
               .append("inner join grup_kuosioner_umum_detail f on (f.grup_kuesioner_umum=a.grup_kuesioner_umum) ")
               .append("where a.tahunakademik is not null and a.semester is not null ")
               .append("and (b.aktif=true or b.aktif is null) ")
               .append("and (f.aktif=true or f.aktif is null) ")
               .append("and f.tbmuser=:user ")
               .append("and (a.sampai is null or a.sampai >= date(:hariIni)) ")
               .append("and (a.mulai is null or a.mulai <= date(:hariIni)) ")
               .append("and ( ")
               .append("     (c.id is not null and (c.aktif=true or c.aktif is null)) ")
               .append("  or (d.id is not null and (e.wajibdiisi=true or e.wajibdiisi is null)) ")
               .append("  or (gd.id is not null and (gd.aktif=true or gd.aktif is null)) ")
               .append("  or (gg.id is not null and (gg.aktif=true or gg.aktif is null)) ")
               .append(") ")
               .append("group by a.tahunakademik, a.semester ")
               .append("order by a.tahunakademik, a.semester");

            datas = session.createSQLQuery(sql.toString())
                    .setParameter("user", tbmuser.getUserId())
                    .setParameter("hariIni", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()))
                    .list();
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeOpenedSession(session);
        }
        return datas;
    }

    @SuppressWarnings("unchecked")
    public static Object[] getJumlahStatusChecklistUmumGrup(String tahunAkademik, String semester, Tbmuser tbmuser, boolean refresh) {
        Session session = null;

        boolean adaChecklist = false;
        boolean adaParameterTambahan = false;
        boolean adaAngketDosenDariJadwal = false;
        boolean adaAngketGuruDariJadwal = false;

        Set<Long> checklistPenilaianUmumTerjadwal = new HashSet<Long>();
        Set<String> checklistPenilaianUmumTerjadwalData = new HashSet<String>();
        Set<Long> parameterTambahanAngketUmumTerjadwal = new HashSet<Long>();
        Set<Long> parameterTambahanAngketUmumTerjadwalGrup = new HashSet<Long>();
        Set<Long> parameterTambahanAngketUmumTerjadwalGrupKuosioner = new HashSet<Long>();
        Set<String> checklistPenilaianUmumTerjadwalDipilih = new HashSet<String>();
        Set<Long> checklistParameterTerjadwalDipilih = new HashSet<Long>();
        Set<Long> checklistParameterTerjadwalDipilihUserTsb = new HashSet<Long>();
        Set<Long> grupChecklistPenilaianDosenTerjadwal = new HashSet<Long>();
        Set<Long> grupChecklistPenilaianGuruTerjadwal = new HashSet<Long>();

        if (tbmuser == null || tbmuser.getUserId() == null) {
            return buildReturnStatusChecklistUmumGrup(adaChecklist, adaParameterTambahan,
                    checklistPenilaianUmumTerjadwalData, checklistPenilaianUmumTerjadwalDipilih,
                    parameterTambahanAngketUmumTerjadwal, checklistParameterTerjadwalDipilih,
                    checklistParameterTerjadwalDipilihUserTsb, checklistPenilaianUmumTerjadwal,
                    adaAngketDosenDariJadwal, adaAngketGuruDariJadwal,
                    grupChecklistPenilaianDosenTerjadwal, grupChecklistPenilaianGuruTerjadwal);
        }

        try {
            session = HibernateUtil.getSessionFactory().openSession();

            StringBuilder sql = new StringBuilder();
            sql.append("select c.id as checklist_penilaian_umum, ")
               .append("d.parameter_tambahan as parameter_tambahan_angket_umum, ")
               .append("a.grup_penilaian_umum, a.grup_kuesioner_umum, ")
               .append("a.grup_checklist_penilaian_dosen, a.grup_checklist_penilaian_guru ")
               .append("from jadwal_checklist_penilaian_umum a ")
               .append("inner join grup_checklist_penilaian_umum b on (a.grup_penilaian_umum=b.id) ")
               .append("left join checklist_penilaian_umum c on (b.id=c.grup_checklist_penilaian_umum) ")
               .append("left join parameter_tambahan_angket_umum d on (b.id=d.grup_checklist_penilaian_umum) ")
               .append("left join parameter_tambahan e on (e.id=d.parameter_tambahan) ")
               .append("left join grup_checklist_penilaian_dosen gd on (gd.id=a.grup_checklist_penilaian_dosen) ")
               .append("left join sekolah.grup_checklist_penilaian_guru gg on (gg.id=a.grup_checklist_penilaian_guru) ")
               .append("inner join grup_kuosioner_umum_detail f on (f.grup_kuesioner_umum=a.grup_kuesioner_umum) ")
               .append("where (e.wajibdiisi=true or e.wajibdiisi is null) ")
               .append("and f.tbmuser=:user ")
               .append("and (f.aktif=true or f.aktif is null) ")
               .append("and (b.aktif=true or b.aktif is null) ")
               .append("and (a.sampai is null or a.sampai >= date(:hariIni)) ")
               .append("and (a.mulai is null or a.mulai <= date(:hariIni)) ");

            if (tahunAkademik != null && tahunAkademik.trim().length() > 0) {
                sql.append("and a.tahunakademik=:tahunAkademik ");
            }
            if (semester != null && semester.trim().length() > 0) {
                sql.append("and a.semester=:semester ");
            }
            sql.append("and ( ")
               .append("     (c.id is not null and (c.aktif=true or c.aktif is null)) ")
               .append("  or (d.id is not null) ")
               .append("  or (gd.id is not null and (gd.aktif=true or gd.aktif is null)) ")
               .append("  or (gg.id is not null and (gg.aktif=true or gg.aktif is null)) ")
               .append(") ");

            org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString());
            query.setParameter("user", tbmuser.getUserId());
            query.setParameter("hariIni", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
            if (tahunAkademik != null && tahunAkademik.trim().length() > 0) {
                query.setParameter("tahunAkademik", tahunAkademik.trim());
            }
            if (semester != null && semester.trim().length() > 0) {
                query.setParameter("semester", semester.trim());
            }

            List<Object[]> datas = query.list();

            for (Object[] obj : datas) {
                Long checklistId = getLong(obj, 0);
                Long parameterId = getLong(obj, 1);
                Long grupUmumId = getLong(obj, 2);
                Long grupKuesionerId = getLong(obj, 3);
                Long grupDosenId = getLong(obj, 4);
                Long grupGuruId = getLong(obj, 5);

                if (checklistId != null) {
                    checklistPenilaianUmumTerjadwal.add(checklistId);
                }
                if (parameterId != null) {
                    parameterTambahanAngketUmumTerjadwal.add(parameterId);
                }
                if (grupUmumId != null) {
                    parameterTambahanAngketUmumTerjadwalGrup.add(grupUmumId);
                }
                if (grupKuesionerId != null) {
                    parameterTambahanAngketUmumTerjadwalGrupKuosioner.add(grupKuesionerId);
                }
                if (grupDosenId != null) {
                    grupChecklistPenilaianDosenTerjadwal.add(grupDosenId);
                }
                if (grupGuruId != null) {
                    grupChecklistPenilaianGuruTerjadwal.add(grupGuruId);
                }
            }

            adaAngketDosenDariJadwal = !grupChecklistPenilaianDosenTerjadwal.isEmpty();
            adaAngketGuruDariJadwal = !grupChecklistPenilaianGuruTerjadwal.isEmpty();

            if (!checklistPenilaianUmumTerjadwal.isEmpty() && !parameterTambahanAngketUmumTerjadwalGrupKuosioner.isEmpty()) {
                Tbmuser tbmuserCurrent = Common.getCurrentUser();
                Tbmuser penilai = tbmuserCurrent == null ? tbmuser : tbmuserCurrent;

                if (tbmuserCurrent != null && !tbmuser.getUserId().equalsIgnoreCase(tbmuserCurrent.getUserId())) {
                    for (Long id : checklistPenilaianUmumTerjadwal) {
                        checklistPenilaianUmumTerjadwalData.add(id + "_" + tbmuser.getUserId());
                    }

                    Collection<ChecklistHasilPenilaianUmum> hasil = penilai.ambilChecklistHasilPenilaianUmum(session, null,
                            tbmuser.getUserId(), refresh);
                    for (ChecklistHasilPenilaianUmum row : hasil) {
                        if (isChecklistHasilUmumSesuaiJadwal(row, tahunAkademik, semester)) {
                            Long id = getChecklistPenilaianUmumId(row);
                            // PENTING: hanya hitung id pertanyaan yang termasuk jadwal aktif saat ini,
                            // agar jumlah terisi tidak pernah melebihi total pertanyaan yang tersedia.
                            if (id != null && checklistPenilaianUmumTerjadwal.contains(id)) {
                                checklistParameterTerjadwalDipilihUserTsb.add(id);
                            }
                        }
                    }
                    adaChecklist = !checklistParameterTerjadwalDipilihUserTsb.containsAll(checklistPenilaianUmumTerjadwal);
                    hasil = null;
                } else {
                    Criteria userCriteria = session.createCriteria(GrupKuosionerUmumDetail.class)
                            .add(Restrictions.in("grupKuesionerUmum.id", parameterTambahanAngketUmumTerjadwalGrupKuosioner))
                            .setProjection(Projections.groupProperty("tbmuser.userId"))
                            .add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
                    if (tbmuserCurrent != null) {
                        userCriteria.add(Restrictions.ne("tbmuser", tbmuserCurrent));
                    }

                    List<String> tbmusers = userCriteria.list();
                    for (String userid : tbmusers) {
                        if (userid == null || userid.trim().length() == 0) {
                            continue;
                        }

                        for (Long id : checklistPenilaianUmumTerjadwal) {
                            checklistPenilaianUmumTerjadwalData.add(id + "_" + userid);
                        }

                        Collection<ChecklistHasilPenilaianUmum> hasil = penilai.ambilChecklistHasilPenilaianUmum(session, null,
                                userid, refresh);

                        Set<Long> dipilihUserIni = new HashSet<Long>();
                        for (ChecklistHasilPenilaianUmum row : hasil) {
                            if (isChecklistHasilUmumSesuaiJadwal(row, tahunAkademik, semester)) {
                                Long id = getChecklistPenilaianUmumId(row);
                                // PENTING: hanya hitung id pertanyaan yang termasuk jadwal aktif saat ini,
                                // agar jumlah terisi tidak pernah melebihi total pertanyaan yang tersedia.
                                if (id != null && checklistPenilaianUmumTerjadwal.contains(id)) {
                                    dipilihUserIni.add(id);
                                    checklistPenilaianUmumTerjadwalDipilih.add(id + "_" + userid);
                                    if (tbmuser.getUserId().equalsIgnoreCase(userid)) {
                                        checklistParameterTerjadwalDipilihUserTsb.add(id);
                                    }
                                }
                            }
                        }

                        adaChecklist = !dipilihUserIni.containsAll(checklistPenilaianUmumTerjadwal);
                        hasil = null;
                        if (adaChecklist) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeOpenedSession(session);
        }

        return buildReturnStatusChecklistUmumGrup(adaChecklist, adaParameterTambahan,
                checklistPenilaianUmumTerjadwalData, checklistPenilaianUmumTerjadwalDipilih,
                parameterTambahanAngketUmumTerjadwal, checklistParameterTerjadwalDipilih,
                checklistParameterTerjadwalDipilihUserTsb, checklistPenilaianUmumTerjadwal,
                adaAngketDosenDariJadwal, adaAngketGuruDariJadwal,
                grupChecklistPenilaianDosenTerjadwal, grupChecklistPenilaianGuruTerjadwal);
    }

    @SuppressWarnings("unchecked")
    public static Object[] getJumlahStatusChecklistUmum(String tahunAkademik, String semester, Tbmuser tbmuser, boolean refresh) {
        Session session = null;
        
        boolean adaChecklist = false;
        boolean adaParameterTambahan = false;
        Set<Long> checklistPenilaianUmumTerjadwal = new HashSet<Long>();
        Set<Long> parameterTambahanAngketUmumTerjadwal = new HashSet<Long>();
        Set<Long> parameterTambahanAngketUmumTerjadwalGrup = new HashSet<Long>();
        Set<Long> checklistPenilaianUmumTerjadwalDipilih = new HashSet<Long>();
        Set<Long> checklistParameterTerjadwalDipilih = new HashSet<Long>();

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String sqlTambahan = buildSqlTambahanUmum(tbmuser, tahunAkademik, semester);

            StringBuilder sql = new StringBuilder();
            sql.append("select c.id as checklist_penilaian_umum, d.parameter_tambahan as parameter_tambahan_angket_umum, a.grup_penilaian_umum from jadwal_checklist_penilaian_umum a ")
               .append(" inner join grup_checklist_penilaian_umum b on (a.grup_penilaian_umum=b.id) ")
               .append(" left join checklist_penilaian_umum c on (b.id=c.grup_checklist_penilaian_umum)  ")
               .append(" left join parameter_tambahan_angket_umum d on (b.id=d.grup_checklist_penilaian_umum)   ")
               .append(" left join parameter_tambahan e on (e.id=d.parameter_tambahan) where (e.wajibdiisi=true or e.wajibdiisi is null) ");
               
            if (tahunAkademik != null) sql.append(" and a.tahunakademik='").append(tahunAkademik).append("' ");
            if (semester != null) sql.append(" and a.semester='").append(semester).append("' ");
            
            sql.append(" and b.aktif and (c.aktif or d.id is not null) and a.grup_kuesioner_umum is null and sampai >= date('")
               .append(Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())).append("') and mulai <= date('")
               .append(Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())).append("') and ").append(sqlTambahan);

            List<Object[]> datas = session.createSQLQuery(sql.toString()).list();

            for (Object[] obj : datas) {
                if (obj[0] != null) checklistPenilaianUmumTerjadwal.add(((Number) obj[0]).longValue());
                if (obj[1] != null) parameterTambahanAngketUmumTerjadwal.add(((Number) obj[1]).longValue());
                if (obj[2] != null) parameterTambahanAngketUmumTerjadwalGrup.add(((Number) obj[2]).longValue());
            }

            if (!checklistPenilaianUmumTerjadwal.isEmpty()) {
                Collection<ChecklistHasilPenilaianUmum> checklistHasilPenilaianUmums = new ArrayList<ChecklistHasilPenilaianUmum>();
                
                Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
                Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
                
                if (mahasiswa != null) {
                    checklistHasilPenilaianUmums = mahasiswa.ambilChecklistHasilPenilaianUmum(session, null, null, refresh);
                } else if (dosen != null) {
                    checklistHasilPenilaianUmums = dosen.ambilChecklistHasilPenilaianUmum(session, null, null, refresh);
                } else if (tbmuser != null) {
                    checklistHasilPenilaianUmums = tbmuser.ambilChecklistHasilPenilaianUmum(session, null, null, refresh);
                }

                for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : checklistHasilPenilaianUmums) {
                    if (checklistHasilPenilaianUmum.getPertemuanId() == null
                            && (tahunAkademik == null || checklistHasilPenilaianUmum.getTahunAkademik().equals(tahunAkademik))
                            && (semester == null || checklistHasilPenilaianUmum.getSemesterStr().equals(semester))) {
                        // PENTING: hanya hitung id pertanyaan yang MEMANG termasuk jadwal aktif saat ini
                        // (checklistPenilaianUmumTerjadwal). Tanpa filter ini, jawaban lama/riwayat dari
                        // pertanyaan di luar jadwal aktif ikut terhitung sehingga "jumlah terisi" bisa
                        // melebihi "total pertanyaan tersedia" (mis. tampil "54 dari 10").
                        Long idPertanyaan = checklistHasilPenilaianUmum.getChecklistPenilaianUmum() == null ? null
                                : checklistHasilPenilaianUmum.getChecklistPenilaianUmum().getId();
                        if (idPertanyaan != null && checklistPenilaianUmumTerjadwal.contains(idPertanyaan)) {
                            checklistPenilaianUmumTerjadwalDipilih.add(idPertanyaan);
                        }
                    }
                }
                adaChecklist = !checklistPenilaianUmumTerjadwalDipilih.containsAll(checklistPenilaianUmumTerjadwal);
                checklistHasilPenilaianUmums = null; // Bantu GC
            }

            if (!parameterTambahanAngketUmumTerjadwal.isEmpty() && !parameterTambahanAngketUmumTerjadwalGrup.isEmpty()) {
                Criteria criteria = session.createCriteria(IsiAngketParameterUmum.class)
                        .createAlias("jadwalChecklistPenilaianUmum", "jadwalChecklistPenilaianUmum")
                        .add(Restrictions.in("jadwalChecklistPenilaianUmum.grupChecklistPenilaianUmum.id", parameterTambahanAngketUmumTerjadwalGrup));

                Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
                Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();

                if (mahasiswa != null) {
                    criteria.add(Restrictions.eq("mahasiswa", mahasiswa));
                } else if (dosen != null) {
                    criteria.add(Restrictions.eq("dosen", dosen));
                } else if (tbmuser != null) {
                    criteria.add(Restrictions.eq("tbmuser", tbmuser));
                }

                List<IsiAngketParameterUmum> angketParameterUmums = criteria.list();
                if (angketParameterUmums.isEmpty()) {
                    adaParameterTambahan = true;
                } else {
                    for (Long id : parameterTambahanAngketUmumTerjadwal) {
                        if (id != null) {
                            for (IsiAngketParameterUmum angketParameterUmum : angketParameterUmums) {
                                List<CommonVO> hasil = angketParameterUmum.ambilDataParameterTambahan();
                                if (hasil.isEmpty()) {
                                    adaParameterTambahan = true;
                                } else {
                                    for (CommonVO vo : hasil) {
                                        boolean invalidName = (vo.getName1() == null || vo.getName1().trim().isEmpty());
                                        if (vo.getId() != null && vo.getId().equals(id.toString()) && invalidName) {
                                            adaParameterTambahan = true;
                                        } else if (vo.getId() != null && vo.getId().equals("-1") && invalidName) {
                                            adaParameterTambahan = true;
                                        }

                                        if (vo.getId() != null && vo.getId().equals(id.toString()) && !invalidName) {
                                            checklistParameterTerjadwalDipilih.add(id);
                                        }
                                    }
                                }
                                hasil = null; // Bantu GC
                            }
                        }
                    }
                }
                angketParameterUmums = null; // Bantu GC
            }
        } catch (Exception e) {
            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ChecklistPenilaianHelper.java:486");
        } finally {
            if (session != null && session.isOpen()) {
                try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianHelper.java:489");}
            }
        }

        return new Object[] { adaChecklist, adaParameterTambahan, checklistPenilaianUmumTerjadwal,
                checklistPenilaianUmumTerjadwalDipilih, parameterTambahanAngketUmumTerjadwal,
                checklistParameterTerjadwalDipilih };
    }
    @SuppressWarnings("unchecked")
    public static Boolean checkStatusChecklistUmum(String tahunAkademik, String semester, Tbmuser tbmuser) {
        Object[] data = getJumlahStatusChecklistUmum(tahunAkademik, semester, tbmuser, false);
        Boolean adaChecklistAda = data != null && data.length > 0 && data[0] instanceof Boolean ? (Boolean) data[0]
                : Boolean.FALSE;

        Set<Long> checklistPenilaianUmumTerjadwal = data != null && data.length > 2 && data[2] instanceof Set
                ? (Set<Long>) data[2] : new HashSet<Long>();
        Set<Long> checklistPenilaianUmumTerjadwalDipilih = data != null && data.length > 3 && data[3] instanceof Set
                ? (Set<Long>) data[3] : new HashSet<Long>();

        return checklistPenilaianUmumTerjadwal.size() > checklistPenilaianUmumTerjadwalDipilih.size()
                || Boolean.TRUE.equals(adaChecklistAda);
    }

    @SuppressWarnings("unchecked")
    public static Boolean checkStatusChecklistUmumGrup(String tahunAkademik, String semester, Tbmuser tbmuser) {
        Object[] data = getJumlahStatusChecklistUmumGrup(tahunAkademik, semester, tbmuser, false);

        // PENTING: data[0] (adaChecklist) SUDAH dihitung dengan benar di
        // getJumlahStatusChecklistUmumGrup (mengecek per-target apakah tbmuser ybs sudah
        // mengisi SEMUA pertanyaan terjadwal untuk target tsb). Sebelumnya method ini
        // mengabaikan data[0] dan malah membandingkan data[2] vs data[3] yang bertipe
        // Set<String> dan diisi di CABANG if/else yang SALING EKSKLUSIF (salah satunya
        // selalu kosong) -- akibatnya gate ini nyaris tidak pernah bekerja dengan benar.
        // Sekarang dipakai data[7] (total pertanyaan terjadwal, Set<Long>) dan data[6]
        // (jumlah yang sudah diisi tbmuser ybs, Set<Long>) yang memang bertipe benar,
        // ditambah data[0] sebagai jaring pengaman.
        Boolean adaChecklistAda = data != null && data.length > 0 && data[0] instanceof Boolean ? (Boolean) data[0]
                : Boolean.FALSE;

        Set<Long> checklistPenilaianUmumTerjadwal = data != null && data.length > 7 && data[7] instanceof Set
                ? (Set<Long>) data[7] : new HashSet<Long>();
        Set<Long> checklistParameterTerjadwalDipilihUserTsb = data != null && data.length > 6 && data[6] instanceof Set
                ? (Set<Long>) data[6] : new HashSet<Long>();

        return checklistPenilaianUmumTerjadwal.size() > checklistParameterTerjadwalDipilihUserTsb.size()
                || Boolean.TRUE.equals(adaChecklistAda);
    }

    public static Boolean adaJadwalAngketDosenDariJadwalUmum(String tahunAkademik, String semester, Tbmuser tbmuser) {
        if (tbmuser == null || tbmuser.getMahasiswa() == null || tbmuser.getMahasiswa().getId() == null) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(!ambilGrupAngketDosenGuruDariJadwalUmum(tahunAkademik, semester, tbmuser, true).isEmpty());
    }

    public static Boolean adaJadwalAngketGuruDariJadwalUmum(String tahunAkademik, String semester, Tbmuser tbmuser) {
        if (tbmuser == null || tbmuser.getSiswa() == null || tbmuser.getSiswa().getId() == null) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(!ambilGrupAngketDosenGuruDariJadwalUmum(tahunAkademik, semester, tbmuser, false).isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> ambilGrupAngketDosenGuruDariJadwalUmum(String tahunAkademik, String semester,
            Tbmuser tbmuser, boolean dosen) {
        Set<Long> result = new HashSet<Long>();
        if (tbmuser == null || tbmuser.getUserId() == null) {
            return result;
        }

        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            String sqlTambahan = buildSqlTambahanUmum(tbmuser, tahunAkademik, semester);
            String column = dosen ? "a.grup_checklist_penilaian_dosen" : "a.grup_checklist_penilaian_guru";
            String joinGroup = dosen
                    ? "left join grup_checklist_penilaian_dosen gkh on (gkh.id=a.grup_checklist_penilaian_dosen) "
                    : "left join sekolah.grup_checklist_penilaian_guru gkh on (gkh.id=a.grup_checklist_penilaian_guru) ";

            StringBuilder sql = new StringBuilder();
            sql.append("select distinct ").append(column).append(" ")
               .append("from jadwal_checklist_penilaian_umum a ")
               .append("inner join grup_checklist_penilaian_umum b on (a.grup_penilaian_umum=b.id) ")
               .append(joinGroup)
               .append("left join grup_kuosioner_umum_detail f on (f.grup_kuesioner_umum=a.grup_kuesioner_umum) ")
               .append("where ").append(column).append(" is not null ")
               .append("and (gkh.aktif=true or gkh.aktif is null) ")
               .append("and (b.aktif=true or b.aktif is null) ")
               .append("and (a.sampai is null or a.sampai >= date(:hariIni)) ")
               .append("and (a.mulai is null or a.mulai <= date(:hariIni)) ");

            if (tahunAkademik != null && tahunAkademik.trim().length() > 0) {
                sql.append("and a.tahunakademik=:tahunAkademik ");
            }
            if (semester != null && semester.trim().length() > 0) {
                sql.append("and a.semester=:semester ");
            }

            sql.append("and ( ")
               .append("(a.grup_kuesioner_umum is not null and f.tbmuser=:user and (f.aktif=true or f.aktif is null)) ")
               .append("or (a.grup_kuesioner_umum is null and ").append(sqlTambahan).append(") ")
               .append(") ");

            org.hibernate.SQLQuery query = session.createSQLQuery(sql.toString());
            query.setParameter("hariIni", Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()));
            query.setParameter("user", tbmuser.getUserId());
            if (tahunAkademik != null && tahunAkademik.trim().length() > 0) {
                query.setParameter("tahunAkademik", tahunAkademik.trim());
            }
            if (semester != null && semester.trim().length() > 0) {
                query.setParameter("semester", semester.trim());
            }

            List<Object> rows = query.list();
            for (Object row : rows) {
                Long id = getLong(row);
                if (id != null) {
                    result.add(id);
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        } finally {
            closeOpenedSession(session);
        }
        return result;
    }

    private static Object[] buildReturnStatusChecklistUmumGrup(boolean adaChecklist, boolean adaParameterTambahan,
            Set<String> checklistPenilaianUmumTerjadwalData, Set<String> checklistPenilaianUmumTerjadwalDipilih,
            Set<Long> parameterTambahanAngketUmumTerjadwal, Set<Long> checklistParameterTerjadwalDipilih,
            Set<Long> checklistParameterTerjadwalDipilihUserTsb, Set<Long> checklistPenilaianUmumTerjadwal,
            boolean adaAngketDosenDariJadwal, boolean adaAngketGuruDariJadwal,
            Set<Long> grupChecklistPenilaianDosenTerjadwal, Set<Long> grupChecklistPenilaianGuruTerjadwal) {
        return new Object[] { Boolean.valueOf(adaChecklist), Boolean.valueOf(adaParameterTambahan),
                checklistPenilaianUmumTerjadwalData, checklistPenilaianUmumTerjadwalDipilih,
                parameterTambahanAngketUmumTerjadwal, checklistParameterTerjadwalDipilih,
                checklistParameterTerjadwalDipilihUserTsb, checklistPenilaianUmumTerjadwal,
                Boolean.valueOf(adaAngketDosenDariJadwal), Boolean.valueOf(adaAngketGuruDariJadwal),
                grupChecklistPenilaianDosenTerjadwal, grupChecklistPenilaianGuruTerjadwal };
    }

    private static boolean isChecklistHasilUmumSesuaiJadwal(ChecklistHasilPenilaianUmum row, String tahunAkademik,
            String semester) {
        if (row == null || row.getPertemuanId() != null) {
            return false;
        }
        String ta = row.getTahunAkademik();
        String smt = row.getSemesterStr();
        return (tahunAkademik == null || tahunAkademik.equals(ta)) && (semester == null || semester.equals(smt));
    }

    private static Long getChecklistPenilaianUmumId(ChecklistHasilPenilaianUmum row) {
        try {
            return row == null || row.getChecklistPenilaianUmum() == null ? null
                    : row.getChecklistPenilaianUmum().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getLong(Object[] row, int index) {
        if (row == null || row.length <= index) {
            return null;
        }
        return getLong(row[index]);
    }

    private static Long getLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return Long.valueOf(((Number) value).longValue());
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static void closeOpenedSession(Session session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ChecklistPenilaianHelper.java:666");
        }
    }


    // ===========================================================================================
    // PRIVATE HELPER METHODS (Optimasi untuk DRY & Memory Usage)
    // ===========================================================================================

    private static String buildSqlTambahanUmum(Tbmuser tbmuser, String tahunAkademik, String semester) {
        Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
        Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
        Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
        Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
        OrangTua orangTua = tbmuser == null ? null : tbmuser.getOrangTua();

        StatusMahasiswa statusMahasiswa = getStatusMahasiswaAman(mahasiswa);
        StringBuilder sqlTambahan = new StringBuilder(" false ");

        if (isUserUmum(tbmuser)) {
            sqlTambahan = new StringBuilder(" b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM)).append("' ");
        } else if (mahasiswa != null && mahasiswa.getId() != null && statusMahasiswa != null
                && statusMahasiswa.getNama() != null
                && statusMahasiswa.getNama().toLowerCase().trim().contains("lulus")) {
            sqlTambahan = new StringBuilder(" (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)).append("' ");
            appendLongFilter(sqlTambahan, "b.status_mahasiswa", statusMahasiswa.getId());
            appendAngkatanFilter(sqlTambahan, mahasiswa.getTahunangkatan());
            appendLongFilter(sqlTambahan, "b.fakultas", getFakultasId(mahasiswa));
            appendLongFilter(sqlTambahan, "b.jurusan", getJurusanId(mahasiswa));
            appendLongFilter(sqlTambahan, "b.jenjang", getJenjangId(mahasiswa));
            appendStringFilter(sqlTambahan, "b.program", mahasiswa.getProgram());
            sqlTambahan.append(" ) ");
        } else if (mahasiswa != null && mahasiswa.getId() != null) {
            sqlTambahan = new StringBuilder(" ( (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)).append("' ");
            appendLongFilter(sqlTambahan, "b.status_mahasiswa", statusMahasiswa == null ? null : statusMahasiswa.getId());
            appendAngkatanFilter(sqlTambahan, mahasiswa.getTahunangkatan());
            appendLongFilter(sqlTambahan, "b.fakultas", getFakultasId(mahasiswa));
            appendLongFilter(sqlTambahan, "b.jurusan", getJurusanId(mahasiswa));
            sqlTambahan.append(" ) or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("' ) ");
            appendLongFilter(sqlTambahan, "b.jenjang", getJenjangId(mahasiswa));
            appendStringFilter(sqlTambahan, "b.program", mahasiswa.getProgram());
        } else if (dosen != null && dosen.getId() != null) {
            sqlTambahan = new StringBuilder(" (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_DOSEN))
                    .append("' or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("') ");
        } else if (siswa != null && siswa.getId() != null) {
            sqlTambahan = new StringBuilder(" ( (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_SISWA)).append("' ");
            appendAngkatanFilter(sqlTambahan, siswa.getTahunMasuk());
            appendLongFilter(sqlTambahan, "b.yayasan", getYayasanId(siswa));
            appendLongFilter(sqlTambahan, "b.sekolah", getSekolahId(siswa));
            sqlTambahan.append(" ) or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("' ) ");
        } else if (guru != null && guru.getId() != null) {
            sqlTambahan = new StringBuilder(" (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_GURU))
                    .append("' or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("') ");
        } else if (orangTua != null && orangTua.getId() != null) {
            sqlTambahan = new StringBuilder(" (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA))
                    .append("' or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("') ");
        } else if (tbmuser != null && tbmuser.getUserId() != null) {
            sqlTambahan = new StringBuilder(" (b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_ADMIN))
                    .append("' or b.diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM))
                    .append("') ");
        }

        if (mahasiswa != null && mahasiswa.getId() != null && isMahasiswaAsisten(mahasiswa, tahunAkademik, semester)) {
            sqlTambahan = new StringBuilder("( b.diperuntukkan='")
                    .append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_ASISTEN)).append("' or ")
                    .append(sqlTambahan.toString()).append(" ) ");
        }

        return sqlTambahan.toString();
    }

    private static boolean isUserUmum(Tbmuser tbmuser) {
        return tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
                && ConstantValues.tbmroleUmum != null && ConstantValues.tbmroleUmum.getRoleId() != null
                && ConstantValues.tbmroleUmum.getRoleId().equals(tbmuser.hakAkses().getRoleId());
    }

    private static StatusMahasiswa getStatusMahasiswaAman(Mahasiswa mahasiswa) {
        try {
            return mahasiswa == null ? null
                    : ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isMahasiswaAsisten(Mahasiswa mahasiswa, String tahunAkademik, String semester) {
        try {
            Map<Long, MahasiswaJadiAsisten> map = ConstantValues.ambilBerdasarClass(MahasiswaJadiAsisten.class);
            if (map == null || map.isEmpty()) {
                return false;
            }
            for (MahasiswaJadiAsisten mahasiswaJadiAsisten : map.values()) {
                if (mahasiswaJadiAsisten == null || !Boolean.TRUE.equals(mahasiswaJadiAsisten.getAktif())
                        || mahasiswaJadiAsisten.getPerkuliahan() == null
                        || mahasiswaJadiAsisten.getMahasiswa() == null
                        || mahasiswaJadiAsisten.getMahasiswa().getId() == null
                        || !mahasiswaJadiAsisten.getMahasiswa().getId().equals(mahasiswa.getId())) {
                    continue;
                }

                if (tahunAkademik == null && semester == null) {
                    return true;
                }

                if (safeEquals(mahasiswaJadiAsisten.getPerkuliahan().getTahunAjaran(), tahunAkademik)
                        && safeEquals(mahasiswaJadiAsisten.getPerkuliahan().getGanjilGenap(), semester)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
        return false;
    }

    private static boolean safeEquals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void appendLongFilter(StringBuilder sb, String column, Long value) {
        if (value == null) {
            sb.append(" and ").append(column).append(" is null ");
        } else {
            sb.append(" and (").append(column).append("=").append(value).append(" or ").append(column).append(" is null) ");
        }
    }

    private static void appendStringFilter(StringBuilder sb, String column, String value) {
        if (value == null || value.trim().isEmpty()) {
            sb.append(" and ").append(column).append(" is null ");
        } else {
            sb.append(" and (").append(column).append("='").append(escapeSql(value.trim())).append("' or ")
                    .append(column).append(" is null) ");
        }
    }

    private static void appendAngkatanFilter(StringBuilder sb, Integer tahunAngkatan) {
        if (tahunAngkatan == null) {
            sb.append(" and b.mulai_angkatan is null and b.sampai_angkatan is null ");
        } else {
            sb.append(" and (b.mulai_angkatan<=").append(tahunAngkatan).append(" or b.mulai_angkatan is null) ")
                    .append(" and (b.sampai_angkatan>=").append(tahunAngkatan).append(" or b.sampai_angkatan is null) ");
        }
    }

    private static String escapeSql(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    private static Long getFakultasId(Mahasiswa mahasiswa) {
        try {
            return mahasiswa == null || mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
                    ? null : mahasiswa.getJurusan().getFakultas().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getJurusanId(Mahasiswa mahasiswa) {
        try {
            return mahasiswa == null || mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getJenjangId(Mahasiswa mahasiswa) {
        try {
            return mahasiswa == null || mahasiswa.getJenjang() == null ? null : mahasiswa.getJenjang().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getYayasanId(Siswa siswa) {
        try {
            return siswa == null || siswa.getSekolah() == null || siswa.getSekolah().getYayasan() == null ? null
                    : siswa.getSekolah().getYayasan().getId();
        } catch (Exception e) {
            return null;
        }
    }

    private static Long getSekolahId(Siswa siswa) {
        try {
            return siswa == null || siswa.getSekolah() == null ? null : siswa.getSekolah().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
