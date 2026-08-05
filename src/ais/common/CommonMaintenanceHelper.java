package ais.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.JenisKegiatan;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Program;
import ais.database.model.StatusPertemuan;
import ais.database.model.library.HariLiburPerpustakaan;

/**
 * Helper proses maintenance/cache kecil yang sebelumnya berada langsung di Common.
 * Dibuat terpisah agar Common tetap menjadi facade dan pengelolaan session lebih aman.
 */
public class CommonMaintenanceHelper {

    private static final Logger log = Logger.getLogger(CommonMaintenanceHelper.class);

    private CommonMaintenanceHelper() {
    }

    public static Pertemuan ambilPertemuan(StatusPertemuan statusPertemuan, Perkuliahan perkuliahan) {
        if (statusPertemuan == null || perkuliahan == null) {
            return null;
        }

        Pertemuan pertemuan = null;
        try {
            List<Pertemuan> utsPertemuans = perkuliahan.ambilPertemuanList();
            if (utsPertemuans != null) {
                for (Pertemuan p : utsPertemuans) {
                    if (p != null && p.getStatusPertemuan() != null && statusPertemuan.getId() != null
                            && p.getStatusPertemuan().getId() != null
                            && statusPertemuan.getId().equals(p.getStatusPertemuan().getId())) {
                        pertemuan = p;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }

        if (pertemuan != null) {
            return pertemuan;
        }

        pertemuan = new Pertemuan();
        pertemuan.setBukuRujukan1("");
        pertemuan.setBukuRujukan2("");
        pertemuan.setDosenTamu("");
        pertemuan.setPerkuliahan(perkuliahan);
        pertemuan.setPertemuanKe(0);
        pertemuan.setRuang(perkuliahan.getRuang());
        pertemuan.setStatusPertemuan(statusPertemuan);
        pertemuan.setTanggal(new Date());
        pertemuan.setTopik(statusPertemuan.getNama());
        pertemuan.setWaktuMulai("00.00");
        pertemuan.setWaktuSelesai("00.00");

        Session session = null;
        Transaction tx = null;
        boolean mulaiTransaksi = false;
        try {
            session = HibernateUtil.currentNativeSession();
            tx = session.getTransaction();
            if (tx == null || !tx.isActive()) {
                tx = session.beginTransaction();
                mulaiTransaksi = true;
            }
            session.save(pertemuan);
            if (mulaiTransaksi && tx != null && tx.isActive()) {
                tx.commit();
            }
            try {
                perkuliahan.reInitPertemuan(session);
            } catch (Exception e) {
                Common.tampilErrorJikaAdmin(e);
            }
        } catch (Exception e) {
            try {
                if (mulaiTransaksi && tx != null && tx.isActive()) {
                    tx.rollback();
                }
            } catch (Exception rollbackError) {
                Common.tampilErrorJikaAdmin(rollbackError);
            }
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
        return pertemuan;
    }

    public static void reInitProgram() {
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            @SuppressWarnings("unchecked")
            List<Program> myPrograms = session.createCriteria(Program.class)
                    .add(Restrictions.or(Restrictions.eq("aktif", Boolean.TRUE), Restrictions.isNull("aktif"))).list();
            Map<String, Program> dataBaru = new HashMap<String, Program>();
            if (myPrograms != null) {
                for (Program program : myPrograms) {
                    if (program != null && program.getNama() != null) {
                        dataBaru.put(program.getNama(), program);
                    }
                }
            }
            Common.programs.clear();
            Common.programs.putAll(dataBaru);
            ConstantValues.initJumlahTahapan();
        } catch (Exception e) {
            log.warn("Gagal reInitProgram", e);
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    public static void reInitHariLibur() {
        Session session = null;
        try {
            session = HibernateUtil.currentNativeSession();
            @SuppressWarnings("unchecked")
            List<HariLiburPerpustakaan> myHariLiburPerpustakaans = ConstantValues
                    .simpleList(session.createCriteria(HariLiburPerpustakaan.class), HariLiburPerpustakaan.class);
            Map<Date, HariLiburPerpustakaan> dataBaru = new HashMap<Date, HariLiburPerpustakaan>();
            if (myHariLiburPerpustakaans != null) {
                for (HariLiburPerpustakaan hariLiburPerpustakaan : myHariLiburPerpustakaans) {
                    if (hariLiburPerpustakaan != null && hariLiburPerpustakaan.getTanggal() != null) {
                        dataBaru.put(hariLiburPerpustakaan.getTanggal(), hariLiburPerpustakaan);
                    }
                }
            }
            Common.hariLiburPerpustakaans.clear();
            Common.hariLiburPerpustakaans.putAll(dataBaru);
        } catch (Exception e) {
            log.warn("Gagal reInitHariLibur", e);
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    @SuppressWarnings("rawtypes")
    public static void initSequence(Class clazz, String squenceName) {
        if (clazz == null || squenceName == null || squenceName.trim().length() == 0) {
            return;
        }
        String sequence = squenceName.trim();
        if (!sequence.matches("[A-Za-z0-9_\\.]+")) {
            throw new IllegalArgumentException("Nama sequence tidak valid: " + sequence);
        }

        Session session = null;
        Transaction tx = null;
        boolean mulaiTransaksi = false;
        try {
            session = HibernateUtil.currentNativeSession();
            ClassMetadata metadata = HibernateUtil.getClassMetadata(clazz);
            Number s = (Number) session.createCriteria(clazz)
                    .setProjection(Projections.max(metadata.getIdentifierPropertyName())).uniqueResult();
            if (s == null) {
                s = Integer.valueOf(1);
            }
            Integer number = Integer.valueOf(s.intValue() + 1);
            String sql = "ALTER SEQUENCE tms_jua." + sequence + " RESTART WITH " + number;
            tx = session.getTransaction();
            if (tx == null || !tx.isActive()) {
                tx = session.beginTransaction();
                mulaiTransaksi = true;
            }
            session.createSQLQuery(sql).executeUpdate();
            if (mulaiTransaksi && tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (RuntimeException e) {
            rollback(tx, mulaiTransaksi);
            Common.tampilErrorJikaAdmin(e);
            throw e;
        } catch (Exception e) {
            rollback(tx, mulaiTransaksi);
            Common.tampilErrorJikaAdmin(e);
        } finally {
            Common.closeNativeSessionQuietly(session);
        }
    }

    private static void rollback(Transaction tx, boolean mulaiTransaksi) {
        try {
            if (mulaiTransaksi && tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
        }
    }
}
